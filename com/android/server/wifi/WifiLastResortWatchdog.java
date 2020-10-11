package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiLastResortWatchdog {
    public static final String BSSID_ANY = "any";
    public static final String BUGREPORT_TITLE = "Wifi watchdog triggered";
    public static final int FAILURE_CODE_ASSOCIATION = 1;
    public static final int FAILURE_CODE_AUTHENTICATION = 2;
    public static final int FAILURE_CODE_DHCP = 3;
    public static final int FAILURE_THRESHOLD = 7;
    @VisibleForTesting
    public static final long LAST_TRIGGER_TIMEOUT_MILLIS = 7200000;
    public static final int MAX_BSSID_AGE = 10;
    public static final double PROB_TAKE_BUGREPORT_DEFAULT = 1.0d;
    private static final String TAG = "WifiLastResortWatchdog";
    private double mBugReportProbability = 1.0d;
    private ClientModeImpl mClientModeImpl;
    private Looper mClientModeImplLooper;
    private Clock mClock;
    private final LocalLog mLocalLog = new LocalLog(100);
    private Map<String, AvailableNetworkFailureCount> mRecentAvailableNetworks = new HashMap();
    private Map<String, Pair<AvailableNetworkFailureCount, Integer>> mSsidFailureCount = new HashMap();
    private String mSsidLastTrigger = null;
    private long mTimeLastTrigger = 0;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mWatchdogAllowedToTrigger = true;
    private boolean mWatchdogFixedWifi = true;
    private WifiInjector mWifiInjector;
    private boolean mWifiIsConnected = false;
    private WifiMetrics mWifiMetrics;

    WifiLastResortWatchdog(WifiInjector wifiInjector, Clock clock, WifiMetrics wifiMetrics, ClientModeImpl clientModeImpl, Looper clientModeImplLooper) {
        this.mWifiInjector = wifiInjector;
        this.mClock = clock;
        this.mWifiMetrics = wifiMetrics;
        this.mClientModeImpl = clientModeImpl;
        this.mClientModeImplLooper = clientModeImplLooper;
    }

    public void updateAvailableNetworks(List<Pair<ScanDetail, WifiConfiguration>> availableNetworks) {
        Pair<AvailableNetworkFailureCount, Integer> ssidFailsAndApCount;
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "updateAvailableNetworks: size = " + availableNetworks.size());
        }
        if (availableNetworks != null) {
            for (Pair<ScanDetail, WifiConfiguration> pair : availableNetworks) {
                ScanDetail scanDetail = (ScanDetail) pair.first;
                WifiConfiguration config = (WifiConfiguration) pair.second;
                ScanResult scanResult = scanDetail.getScanResult();
                if (scanResult != null) {
                    String bssid = scanResult.BSSID;
                    String ssid = "\"" + scanDetail.getSSID() + "\"";
                    if (this.mVerboseLoggingEnabled) {
                        Log.v(TAG, " " + bssid + ": " + scanDetail.getSSID());
                    }
                    AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
                    if (availableNetworkFailureCount == null) {
                        availableNetworkFailureCount = new AvailableNetworkFailureCount(config);
                        availableNetworkFailureCount.ssid = ssid;
                        Pair<AvailableNetworkFailureCount, Integer> ssidFailsAndApCount2 = this.mSsidFailureCount.get(ssid);
                        if (ssidFailsAndApCount2 == null) {
                            ssidFailsAndApCount = Pair.create(new AvailableNetworkFailureCount(config), 1);
                            if (this.mTimeLastTrigger == 0 || this.mClock.getElapsedSinceBootMillis() - this.mTimeLastTrigger >= LAST_TRIGGER_TIMEOUT_MILLIS) {
                                localLog("updateAvailableNetworks: setWatchdogTriggerEnabled to true");
                                setWatchdogTriggerEnabled(true);
                            }
                        } else {
                            ssidFailsAndApCount = Pair.create((AvailableNetworkFailureCount) ssidFailsAndApCount2.first, Integer.valueOf(((Integer) ssidFailsAndApCount2.second).intValue() + 1));
                        }
                        this.mSsidFailureCount.put(ssid, ssidFailsAndApCount);
                    }
                    if (config != null) {
                        availableNetworkFailureCount.config = config;
                    }
                    availableNetworkFailureCount.age = -1;
                    this.mRecentAvailableNetworks.put(bssid, availableNetworkFailureCount);
                }
            }
        }
        Iterator<Map.Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AvailableNetworkFailureCount> entry = it.next();
            if (entry.getValue().age < 9) {
                entry.getValue().age++;
            } else {
                String ssid2 = entry.getValue().ssid;
                Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid2);
                if (ssidFails != null) {
                    Integer apCount = Integer.valueOf(((Integer) ssidFails.second).intValue() - 1);
                    if (apCount.intValue() > 0) {
                        this.mSsidFailureCount.put(ssid2, Pair.create((AvailableNetworkFailureCount) ssidFails.first, apCount));
                    } else {
                        this.mSsidFailureCount.remove(ssid2);
                    }
                } else {
                    Log.d(TAG, "updateAvailableNetworks: SSID to AP count mismatch for " + ssid2);
                }
                it.remove();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, toString());
        }
    }

    public boolean noteConnectionFailureAndTriggerIfNeeded(String ssid, String bssid, int reason) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "noteConnectionFailureAndTriggerIfNeeded: [" + ssid + ", " + bssid + ", " + reason + "]");
        }
        updateFailureCountForNetwork(ssid, bssid, reason);
        if (!this.mWatchdogAllowedToTrigger) {
            this.mWifiMetrics.incrementWatchdogTotalConnectionFailureCountAfterTrigger();
            this.mWatchdogFixedWifi = false;
        }
        boolean isRestartNeeded = checkTriggerCondition();
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "isRestartNeeded = " + isRestartNeeded);
        }
        if (isRestartNeeded) {
            localLog("noteConnectionFailureAndTriggerIfNeeded: setWatchdogTriggerEnabled to false");
            setWatchdogTriggerEnabled(false);
            this.mWatchdogFixedWifi = true;
            loge("Watchdog triggering recovery");
            this.mSsidLastTrigger = ssid;
            this.mTimeLastTrigger = this.mClock.getElapsedSinceBootMillis();
            localLog(toString());
            this.mWifiInjector.getSelfRecovery().trigger(0);
            incrementWifiMetricsTriggerCounts();
            clearAllFailureCounts();
        }
        return isRestartNeeded;
    }

    public void connectedStateTransition(boolean isEntering) {
        logv("connectedStateTransition: isEntering = " + isEntering);
        this.mWifiIsConnected = isEntering;
        if (isEntering) {
            if (!this.mWatchdogAllowedToTrigger && this.mWatchdogFixedWifi && checkIfAtleastOneNetworkHasEverConnected() && checkIfConnectedBackToSameSsid()) {
                takeBugReportWithCurrentProbability("Wifi fixed after restart");
                this.mWifiMetrics.incrementNumLastResortWatchdogSuccesses();
                this.mWifiMetrics.setWatchdogSuccessTimeDurationMs(this.mClock.getElapsedSinceBootMillis() - this.mTimeLastTrigger);
            }
            clearAllFailureCounts();
            localLog("connectedStateTransition: setWatchdogTriggerEnabled to true");
            setWatchdogTriggerEnabled(true);
        }
    }

    private boolean checkIfConnectedBackToSameSsid() {
        if (TextUtils.equals(this.mSsidLastTrigger, this.mClientModeImpl.getWifiInfo().getSSID())) {
            return true;
        }
        localLog("checkIfConnectedBackToSameSsid: different SSID be connected");
        return false;
    }

    private void takeBugReportWithCurrentProbability(String bugDetail) {
        if (this.mBugReportProbability > Math.random()) {
            new Handler(this.mClientModeImplLooper).post(new Runnable(bugDetail) {
                private final /* synthetic */ String f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiLastResortWatchdog.this.mo3387x3d2d063b(this.f$1);
                }
            });
        }
    }

    /* renamed from: lambda$takeBugReportWithCurrentProbability$0$WifiLastResortWatchdog */
    public /* synthetic */ void mo3387x3d2d063b(String bugDetail) {
        this.mClientModeImpl.takeBugReport(BUGREPORT_TITLE, bugDetail);
    }

    private void updateFailureCountForNetwork(String ssid, String bssid, int reason) {
        logv("updateFailureCountForNetwork: [" + ssid + ", " + bssid + ", " + reason + "]");
        if ("any".equals(bssid)) {
            incrementSsidFailureCount(ssid, reason);
        } else {
            incrementBssidFailureCount(ssid, bssid, reason);
        }
    }

    private void incrementSsidFailureCount(String ssid, int reason) {
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            Log.d(TAG, "updateFailureCountForNetwork: No networks for ssid = " + ssid);
            return;
        }
        ((AvailableNetworkFailureCount) ssidFails.first).incrementFailureCount(reason);
    }

    private void incrementBssidFailureCount(String ssid, String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            Log.d(TAG, "updateFailureCountForNetwork: Unable to find Network [" + ssid + ", " + bssid + "]");
        } else if (!availableNetworkFailureCount.ssid.equals(ssid)) {
            Log.d(TAG, "updateFailureCountForNetwork: Failed connection attempt has wrong ssid. Failed [" + ssid + ", " + bssid + "], buffered [" + availableNetworkFailureCount.ssid + ", " + bssid + "]");
        } else {
            if (availableNetworkFailureCount.config == null && this.mVerboseLoggingEnabled) {
                Log.v(TAG, "updateFailureCountForNetwork: network has no config [" + ssid + ", " + bssid + "]");
            }
            availableNetworkFailureCount.incrementFailureCount(reason);
            incrementSsidFailureCount(ssid, reason);
        }
    }

    public boolean shouldIgnoreBssidUpdate(String bssid) {
        return this.mWatchdogAllowedToTrigger && isBssidOnlyApOfSsid(bssid) && isSingleSsidRecorded() && checkIfAtleastOneNetworkHasEverConnected();
    }

    public boolean shouldIgnoreSsidUpdate() {
        return this.mWatchdogAllowedToTrigger && isSingleSsidRecorded() && checkIfAtleastOneNetworkHasEverConnected();
    }

    private boolean isBssidOnlyApOfSsid(String bssid) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            return false;
        }
        String ssid = availableNetworkFailureCount.ssid;
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            Log.d(TAG, "isOnlyBssidAvailable: Could not find SSID count for " + ssid);
            return false;
        } else if (((Integer) ssidFails.second).intValue() != 1) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isSingleSsidRecorded() {
        return this.mSsidFailureCount.size() == 1;
    }

    private boolean checkTriggerCondition() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "checkTriggerCondition.");
        }
        if (this.mWifiIsConnected || !this.mWatchdogAllowedToTrigger) {
            return false;
        }
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            if (!isOverFailureThreshold(entry.getKey())) {
                return false;
            }
        }
        boolean atleastOneNetworkHasEverConnected = checkIfAtleastOneNetworkHasEverConnected();
        logv("checkTriggerCondition: return = " + atleastOneNetworkHasEverConnected);
        return checkIfAtleastOneNetworkHasEverConnected();
    }

    private boolean checkIfAtleastOneNetworkHasEverConnected() {
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            if (entry.getValue().config != null && entry.getValue().config.getNetworkSelectionStatus().getHasEverConnected()) {
                return true;
            }
        }
        return false;
    }

    private void incrementWifiMetricsTriggerCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "incrementWifiMetricsTriggerCounts.");
        }
        this.mWifiMetrics.incrementNumLastResortWatchdogTriggers();
        this.mWifiMetrics.addCountToNumLastResortWatchdogAvailableNetworksTotal(this.mSsidFailureCount.size());
        int badAuth = 0;
        int badAssoc = 0;
        int badDhcp = 0;
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry : this.mSsidFailureCount.entrySet()) {
            int badSum = ((AvailableNetworkFailureCount) entry.getValue().first).associationRejection + ((AvailableNetworkFailureCount) entry.getValue().first).authenticationFailure + ((AvailableNetworkFailureCount) entry.getValue().first).dhcpFailure;
            if (badSum >= 7) {
                int i = 1;
                badAssoc += ((AvailableNetworkFailureCount) entry.getValue().first).associationRejection >= badSum / 2 ? 1 : 0;
                badAuth += ((AvailableNetworkFailureCount) entry.getValue().first).authenticationFailure >= badSum / 2 ? 1 : 0;
                if (((AvailableNetworkFailureCount) entry.getValue().first).dhcpFailure < badSum / 2) {
                    i = 0;
                }
                badDhcp += i;
            }
        }
        if (badAuth > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(badAuth);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAuthentication();
        }
        if (badAssoc > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAssociationNetworksTotal(badAssoc);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAssociation();
        }
        if (badDhcp > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadDhcpNetworksTotal(badDhcp);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadDhcp();
        }
    }

    public void clearAllFailureCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "clearAllFailureCounts.");
        }
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            entry.getValue().resetCounts();
        }
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            ((AvailableNetworkFailureCount) entry2.getValue().first).resetCounts();
        }
    }

    /* access modifiers changed from: package-private */
    public Map<String, AvailableNetworkFailureCount> getRecentAvailableNetworks() {
        return this.mRecentAvailableNetworks;
    }

    private void setWatchdogTriggerEnabled(boolean enable) {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "setWatchdogTriggerEnabled: enable = " + enable);
        }
        this.mWatchdogAllowedToTrigger = enable;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mWatchdogAllowedToTrigger: ");
        sb.append(this.mWatchdogAllowedToTrigger);
        sb.append("\nmWifiIsConnected: ");
        sb.append(this.mWifiIsConnected);
        sb.append("\nmRecentAvailableNetworks: ");
        sb.append(this.mRecentAvailableNetworks.size());
        for (Map.Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            sb.append("\n ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(", Age: ");
            sb.append(entry.getValue().age);
        }
        sb.append("\nmSsidFailureCount:");
        for (Map.Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            sb.append("\n");
            sb.append(entry2.getKey());
            sb.append(": ");
            sb.append((Integer) entry2.getValue().second);
            sb.append(",");
            sb.append(((AvailableNetworkFailureCount) entry2.getValue().first).toString());
        }
        return sb.toString();
    }

    public boolean isOverFailureThreshold(String bssid) {
        if (getFailureCount(bssid, 1) + getFailureCount(bssid, 2) + getFailureCount(bssid, 3) >= 7) {
            return true;
        }
        return false;
    }

    public int getFailureCount(String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            return 0;
        }
        String ssid = availableNetworkFailureCount.ssid;
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            Log.d(TAG, "getFailureCount: Could not find SSID count for " + ssid);
            return 0;
        }
        AvailableNetworkFailureCount failCount = (AvailableNetworkFailureCount) ssidFails.first;
        if (reason == 1) {
            return failCount.associationRejection;
        }
        if (reason == 2) {
            return failCount.authenticationFailure;
        }
        if (reason != 3) {
            return 0;
        }
        return failCount.dhcpFailure;
    }

    /* access modifiers changed from: protected */
    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setBugReportProbability(double newProbability) {
        this.mBugReportProbability = newProbability;
    }

    public static class AvailableNetworkFailureCount {
        public int age = 0;
        public int associationRejection = 0;
        public int authenticationFailure = 0;
        public WifiConfiguration config;
        public int dhcpFailure = 0;
        public String ssid = "";

        AvailableNetworkFailureCount(WifiConfiguration configParam) {
            this.config = configParam;
        }

        public void incrementFailureCount(int reason) {
            if (reason == 1) {
                this.associationRejection++;
            } else if (reason == 2) {
                this.authenticationFailure++;
            } else if (reason == 3) {
                this.dhcpFailure++;
            }
        }

        /* access modifiers changed from: package-private */
        public void resetCounts() {
            this.associationRejection = 0;
            this.authenticationFailure = 0;
            this.dhcpFailure = 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.ssid);
            sb.append(" HasEverConnected: ");
            WifiConfiguration wifiConfiguration = this.config;
            sb.append(wifiConfiguration != null ? Boolean.valueOf(wifiConfiguration.getNetworkSelectionStatus().getHasEverConnected()) : "null_config");
            sb.append(", Failures: {Assoc: ");
            sb.append(this.associationRejection);
            sb.append(", Auth: ");
            sb.append(this.authenticationFailure);
            sb.append(", Dhcp: ");
            sb.append(this.dhcpFailure);
            sb.append("}");
            return sb.toString();
        }
    }

    private void localLog(String s) {
        this.mLocalLog.log(s);
    }

    private void logv(String s) {
        this.mLocalLog.log(s);
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, s);
        }
    }

    private void loge(String s) {
        this.mLocalLog.log(s);
        Log.e(TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiLastResortWatchdog");
        pw.println("WifiLastResortWatchdog - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiLastResortWatchdog - Log End ----");
    }
}
