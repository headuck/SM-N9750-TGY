package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.net.MacAddress;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.LinkProbeManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.util.TimedQuotaManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LinkProbeManager {
    @VisibleForTesting
    static final long DELAY_AFTER_TX_SUCCESS_MS = 6000;
    @VisibleForTesting
    static final long DELAY_BETWEEN_PROBES_MS = 6000;
    @VisibleForTesting
    static final int[] EXPERIMENT_DELAYS_MS = {IWCEventManager.wifiOFFPending_MS, WifiNetworkSelector.MINIMUM_NETWORK_SELECTION_INTERVAL_MS, 9000, 12000, 15000};
    @VisibleForTesting
    static final int[] EXPERIMENT_LINK_SPEEDS = {10, 15, 20};
    @VisibleForTesting
    static final int[] EXPERIMENT_RSSIS = {-65, RSSI_THRESHOLD, -75};
    @VisibleForTesting
    static final int LINK_SPEED_THRESHOLD_MBPS = 15;
    @VisibleForTesting
    static final long MAX_PROBE_COUNT_IN_PERIOD = 192;
    @VisibleForTesting
    static final long PERIOD_MILLIS = Duration.ofDays(1).toMillis();
    @VisibleForTesting
    static final int RSSI_THRESHOLD = -70;
    @VisibleForTesting
    static final long SCREEN_ON_DELAY_MS = 6000;
    private static final String TAG = "WifiLinkProbeManager";
    private static final int WIFI_LINK_PROBING_ENABLED_DEFAULT = 1;
    private final Clock mClock;
    private final Context mContext;
    private List<Experiment> mExperiments = new ArrayList();
    private final FrameworkFacade mFrameworkFacade;
    private long mLastLinkProbeTimestampMs;
    private long mLastScreenOnTimestampMs;
    private long mLastTxSuccessCount;
    private long mLastTxSuccessIncreaseTimestampMs;
    private boolean mLinkProbingEnabled = false;
    private boolean mLinkProbingSupported;
    private final TimedQuotaManager mTimedQuotaManager;
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;

    public LinkProbeManager(Clock clock, WifiNative wifiNative, WifiMetrics wifiMetrics, FrameworkFacade frameworkFacade, Looper looper, Context context) {
        this.mClock = clock;
        this.mWifiNative = wifiNative;
        this.mWifiMetrics = wifiMetrics;
        this.mFrameworkFacade = frameworkFacade;
        this.mContext = context;
        this.mLinkProbingSupported = this.mContext.getResources().getBoolean(17891606);
        this.mTimedQuotaManager = new TimedQuotaManager(clock, MAX_PROBE_COUNT_IN_PERIOD, PERIOD_MILLIS);
        if (this.mLinkProbingSupported) {
            this.mFrameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_link_probing_enabled"), false, new ContentObserver(new Handler(looper)) {
                public void onChange(boolean selfChange) {
                    LinkProbeManager.this.updateLinkProbeSetting();
                }
            });
            updateLinkProbeSetting();
            resetOnNewConnection();
            resetOnScreenTurnedOn();
        }
        initExperiments();
    }

    /* access modifiers changed from: private */
    public void updateLinkProbeSetting() {
        boolean z = true;
        if (this.mFrameworkFacade.getIntegerSetting(this.mContext, "wifi_link_probing_enabled", 1) != 1) {
            z = false;
        }
        this.mLinkProbingEnabled = z;
    }

    public void enableVerboseLogging(boolean enable) {
        this.mVerboseLoggingEnabled = enable;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of LinkProbeManager");
        pw.println("LinkProbeManager - link probing supported by device: " + this.mLinkProbingSupported);
        pw.println("LinkProbeManager - link probing feature flag enabled: " + this.mLinkProbingEnabled);
        pw.println("LinkProbeManager - mLastLinkProbeTimestampMs: " + this.mLastLinkProbeTimestampMs);
        pw.println("LinkProbeManager - mLastTxSuccessIncreaseTimestampMs: " + this.mLastTxSuccessIncreaseTimestampMs);
        pw.println("LinkProbeManager - mLastTxSuccessCount: " + this.mLastTxSuccessCount);
        pw.println("LinkProbeManager - mLastScreenOnTimestampMs: " + this.mLastScreenOnTimestampMs);
        pw.println("LinkProbeManager - mTimedQuotaManager: " + this.mTimedQuotaManager);
    }

    public void resetOnNewConnection() {
        this.mExperiments.forEach($$Lambda$X1lFDUueUo45PAoqhGr4T3sqGcQ.INSTANCE);
        if (this.mLinkProbingSupported) {
            long now = this.mClock.getElapsedSinceBootMillis();
            this.mLastLinkProbeTimestampMs = now;
            this.mLastTxSuccessIncreaseTimestampMs = now;
            this.mLastTxSuccessCount = PERIOD_MILLIS;
        }
    }

    public void resetOnScreenTurnedOn() {
        this.mExperiments.forEach($$Lambda$wnTZM417PCBfxzRuRKe4M8L3Dow.INSTANCE);
        if (this.mLinkProbingSupported) {
            this.mLastScreenOnTimestampMs = this.mClock.getElapsedSinceBootMillis();
        }
    }

    public void updateConnectionStats(WifiInfo wifiInfo, String interfaceName) {
        WifiInfo wifiInfo2 = wifiInfo;
        this.mExperiments.forEach(new Consumer(wifiInfo2) {
            private final /* synthetic */ WifiInfo f$0;

            {
                this.f$0 = r1;
            }

            public final void accept(Object obj) {
                ((LinkProbeManager.Experiment) obj).updateConnectionStats(this.f$0);
            }
        });
        if (this.mLinkProbingSupported) {
            long now = this.mClock.getElapsedSinceBootMillis();
            if (this.mLastTxSuccessCount < wifiInfo2.txSuccess) {
                this.mLastTxSuccessIncreaseTimestampMs = now;
            }
            this.mLastTxSuccessCount = wifiInfo2.txSuccess;
            long timeSinceLastLinkProbeMs = now - this.mLastLinkProbeTimestampMs;
            if (timeSinceLastLinkProbeMs >= 6000) {
                long timeSinceLastTxSuccessIncreaseMs = now - this.mLastTxSuccessIncreaseTimestampMs;
                if (timeSinceLastTxSuccessIncreaseMs >= 6000 && now - this.mLastScreenOnTimestampMs >= 6000) {
                    int rssi = wifiInfo.getRssi();
                    int linkSpeed = wifiInfo.getLinkSpeed();
                    if ((rssi == -127 || rssi <= RSSI_THRESHOLD || linkSpeed <= 15) && this.mTimedQuotaManager.requestQuota()) {
                        if (this.mLinkProbingEnabled) {
                            if (this.mVerboseLoggingEnabled) {
                                Log.d(TAG, String.format("link probing triggered with conditions: timeSinceLastLinkProbeMs=%d timeSinceLastTxSuccessIncreaseMs=%d rssi=%d linkSpeed=%s", new Object[]{Long.valueOf(timeSinceLastLinkProbeMs), Long.valueOf(timeSinceLastTxSuccessIncreaseMs), Integer.valueOf(rssi), Integer.valueOf(linkSpeed)}));
                            }
                            WifiNative wifiNative = this.mWifiNative;
                            C04012 r7 = r0;
                            long j = now;
                            final long j2 = timeSinceLastTxSuccessIncreaseMs;
                            int linkSpeed2 = linkSpeed;
                            final int linkSpeed3 = rssi;
                            int i = rssi;
                            final int rssi2 = linkSpeed2;
                            C04012 r0 = new WifiNative.SendMgmtFrameCallback() {
                                public void onAck(int elapsedTimeMs) {
                                    if (LinkProbeManager.this.mVerboseLoggingEnabled) {
                                        Log.d(LinkProbeManager.TAG, "link probing success, elapsedTimeMs=" + elapsedTimeMs);
                                    }
                                    LinkProbeManager.this.mWifiMetrics.logLinkProbeSuccess(j2, linkSpeed3, rssi2, elapsedTimeMs);
                                }

                                public void onFailure(int reason) {
                                    if (LinkProbeManager.this.mVerboseLoggingEnabled) {
                                        Log.d(LinkProbeManager.TAG, "link probing failure, reason=" + reason);
                                    }
                                    LinkProbeManager.this.mWifiMetrics.logLinkProbeFailure(j2, linkSpeed3, rssi2, reason);
                                }
                            };
                            wifiNative.probeLink(interfaceName, MacAddress.fromString(wifiInfo.getBSSID()), r7, -1);
                        } else {
                            String str = interfaceName;
                            int i2 = linkSpeed;
                            int i3 = rssi;
                            long j3 = now;
                        }
                        this.mLastLinkProbeTimestampMs = this.mClock.getElapsedSinceBootMillis();
                    }
                }
            }
        }
    }

    private void initExperiments() {
        for (int delay : EXPERIMENT_DELAYS_MS) {
            int[] iArr = EXPERIMENT_RSSIS;
            int length = iArr.length;
            int i = 0;
            while (i < length) {
                int rssiThreshold = iArr[i];
                int[] iArr2 = EXPERIMENT_LINK_SPEEDS;
                int length2 = iArr2.length;
                int i2 = 0;
                while (i2 < length2) {
                    this.mExperiments.add(new Experiment(this.mClock, this.mWifiMetrics, delay, delay, delay, rssiThreshold, iArr2[i2]));
                    i2++;
                    length2 = length2;
                    iArr2 = iArr2;
                    i = i;
                }
                i++;
            }
        }
    }

    private static class Experiment {
        private final Clock mClock;
        private final int mDelayBetweenProbesMs;
        private final String mExperimentId = getExperimentId();
        private long mLastLinkProbeTimestampMs;
        private long mLastScreenOnTimestampMs;
        private long mLastTxSuccessCount;
        private long mLastTxSuccessIncreaseTimestampMs;
        private final int mLinkSpeedThreshold;
        private final int mNoTxDelayMs;
        private final int mRssiThreshold;
        private final int mScreenOnDelayMs;
        private final WifiMetrics mWifiMetrics;

        Experiment(Clock clock, WifiMetrics wifiMetrics, int screenOnDelayMs, int noTxDelayMs, int delayBetweenProbesMs, int rssiThreshold, int linkSpeedThreshold) {
            this.mClock = clock;
            this.mWifiMetrics = wifiMetrics;
            this.mScreenOnDelayMs = screenOnDelayMs;
            this.mNoTxDelayMs = noTxDelayMs;
            this.mDelayBetweenProbesMs = delayBetweenProbesMs;
            this.mRssiThreshold = rssiThreshold;
            this.mLinkSpeedThreshold = linkSpeedThreshold;
            resetOnNewConnection();
            resetOnScreenTurnedOn();
        }

        private String getExperimentId() {
            return "[screenOnDelay=" + this.mScreenOnDelayMs + ',' + "noTxDelay=" + this.mNoTxDelayMs + ',' + "delayBetweenProbes=" + this.mDelayBetweenProbesMs + ',' + "rssiThreshold=" + this.mRssiThreshold + ',' + "linkSpeedThreshold=" + this.mLinkSpeedThreshold + ']';
        }

        /* access modifiers changed from: package-private */
        public void resetOnNewConnection() {
            long now = this.mClock.getElapsedSinceBootMillis();
            this.mLastLinkProbeTimestampMs = now;
            this.mLastTxSuccessIncreaseTimestampMs = now;
            this.mLastTxSuccessCount = LinkProbeManager.PERIOD_MILLIS;
        }

        /* access modifiers changed from: package-private */
        public void resetOnScreenTurnedOn() {
            this.mLastScreenOnTimestampMs = this.mClock.getElapsedSinceBootMillis();
        }

        /* access modifiers changed from: package-private */
        public void updateConnectionStats(WifiInfo wifiInfo) {
            long now = this.mClock.getElapsedSinceBootMillis();
            if (this.mLastTxSuccessCount < wifiInfo.txSuccess) {
                this.mLastTxSuccessIncreaseTimestampMs = now;
            }
            this.mLastTxSuccessCount = wifiInfo.txSuccess;
            if (now - this.mLastLinkProbeTimestampMs >= ((long) this.mDelayBetweenProbesMs) && now - this.mLastTxSuccessIncreaseTimestampMs >= ((long) this.mNoTxDelayMs) && now - this.mLastScreenOnTimestampMs >= 6000) {
                int rssi = wifiInfo.getRssi();
                int linkSpeed = wifiInfo.getLinkSpeed();
                if (rssi == -127 || rssi <= this.mRssiThreshold || linkSpeed <= this.mLinkSpeedThreshold) {
                    this.mWifiMetrics.incrementLinkProbeExperimentProbeCount(this.mExperimentId);
                    this.mLastLinkProbeTimestampMs = this.mClock.getElapsedSinceBootMillis();
                }
            }
        }
    }
}
