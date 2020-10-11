package com.android.server.wifi;

import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.WorkSource;
import android.sec.enterprise.WifiPolicyCache;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.util.ScanResultUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.server.wifi.SemSarManager;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.dqa.ReportUtil;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiConnectivityManager {
    @VisibleForTesting
    public static final int BSSID_BLACKLIST_EXPIRE_TIME_MS = 300000;
    @VisibleForTesting
    public static final int BSSID_BLACKLIST_THRESHOLD = 3;
    private static final int CHANNEL_LIST_AGE_MS = 3600000;
    private static final int CONNECTED_PNO_SCAN_INTERVAL_MS = 160000;
    private static final boolean CSC_SUPPORT_ASSOCIATED_NETWORK_SELECTION = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTASSOCIATEDNETWORKSELECTION, false);
    private static final boolean ENABLE_DISCONNECTED_PNO_SCAN = false;
    private static final int LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS = 80000;
    private static final int LOW_RSSI_NETWORK_RETRY_START_DELAY_MS = 20000;
    public static final int MAX_CONNECTION_ATTEMPTS_RATE = 6;
    public static final int MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS = 240000;
    @VisibleForTesting
    public static final int MAX_PERIODIC_SCAN_INTERVAL_MS = 128000;
    public static final int MAX_PERIODIC_SCAN_INTERVAL_MS_FOR_GEOFENCE = 1024000;
    static final int MAX_PERIODIC_SCAN_NON_WAKEUP_TIMER = 0;
    public static final String MAX_PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Max Periodic Scan Timer";
    static final int MAX_PERIODIC_SCAN_WAKEUP_TIMER = 1;
    private static final int MAX_SCAN_FAIL_COUNT = 10;
    @VisibleForTesting
    public static final int MAX_SCAN_RESTART_ALLOWED = 5;
    public static final String MAX_SLEEP_PERIODIC_SCAN_TIMER_AUX_TAG = "WifiConnectivityManager Schedule Max Sleep Periodic Scan Timer Aux";
    public static final String MAX_SLEEP_PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Max Sleep Periodic Scan Timer";
    @VisibleForTesting
    static final int MOVING_PNO_SCAN_INTERVAL_MS = 20000;
    @VisibleForTesting
    public static final int PERIODIC_SCAN_INTERVAL_MS = 8000;
    public static final String PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Periodic Scan Timer";
    @VisibleForTesting
    public static final int REASON_CODE_AP_UNABLE_TO_HANDLE_NEW_STA = 17;
    public static final int REASON_CODE_BSSID_PRUNED_ASSOC_DISALLOW = 65539;
    public static final int REASON_CODE_BSSID_PRUNED_ASSOC_RETRY_DELAY = 65537;
    public static final int REASON_CODE_BSSID_PRUNED_BASE = 65536;
    public static final int REASON_CODE_BSSID_PRUNED_RSSI_ASSOC_REJ = 65538;
    public static final int REASON_CODE_BSSID_PRUNED_UNSPECIFIED = 65536;
    private static final long RESET_TIME_STAMP = Long.MIN_VALUE;
    public static final String RESTART_CONNECTIVITY_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Scan";
    private static final int RESTART_SCAN_DELAY_MS = 2000;
    public static final String RESTART_SINGLE_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Single Scan";
    private static final boolean SCAN_IMMEDIATELY = true;
    private static final boolean SCAN_ON_SCHEDULE = false;
    @VisibleForTesting
    static final int STATIONARY_PNO_SCAN_INTERVAL_MS = 60000;
    private static final String TAG = "WifiConnectivityManager";
    public static final int VALID_WAKEUP_MAX_PERIODIC_SCAN_MS = 3600000;
    private static final String VendorNotificationStyle = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGWIFINOTIFICATIONSTYLE);
    private static final int WATCHDOG_INTERVAL_MS = 1200000;
    public static final String WATCHDOG_TIMER_TAG = "WifiConnectivityManager Schedule Watchdog Timer";
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    public static final int WIFI_STATE_TRANSITIONING = 3;
    public static final int WIFI_STATE_UNKNOWN = 0;
    private final AlarmManager mAlarmManager;
    private final AllSingleScanListener mAllSingleScanListener;
    private int mBand5GHzBonus;
    private boolean mBluetoothConnected;
    private Map<String, BssidBlacklistStatus> mBssidBlacklist;
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final CarrierNetworkNotifier mCarrierNetworkNotifier;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final LinkedList<Long> mConnectionAttemptTimeStamps;
    /* access modifiers changed from: private */
    public final WifiConnectivityHelper mConnectivityHelper;
    private final Context mContext;
    private int mCurrentConnectionBonus;
    /* access modifiers changed from: private */
    public boolean mDbg;
    private boolean mEnableAutoJoinWhenAssociated;
    private final Handler mEventHandler;
    private long mFirstMaxPeriodicScanTimeStamp;
    private int mFullScanMaxRxRate;
    private int mFullScanMaxTxRate;
    private String mLastConnectionAttemptBssid;
    private long mLastPeriodicSingleScanTimeStamp;
    private final LocalLog mLocalLog;
    private final AlarmManager.OnAlarmListener mMaxPeriodicScanTimerListener;
    private boolean mMaxPeriodicScanTimerSet;
    private final AlarmManager.OnAlarmListener mMaxSleepPeriodicScanTimerAuxListener;
    private boolean mMaxSleepPeriodicScanTimerAuxSet;
    private final AlarmManager.OnAlarmListener mMaxSleepPeriodicScanTimerListener;
    private boolean mMaxSleepPeriodicScanTimerSet;
    private final WifiNetworkSelector mNetworkSelector;
    private boolean mOneShotScan;
    private final OpenNetworkNotifier mOpenNetworkNotifier;
    private PasspointManager mPasspointManager;
    private final AlarmManager.OnAlarmListener mPeriodicScanTimerListener;
    private boolean mPeriodicScanTimerSet;
    private int mPeriodicSingleScanInterval;
    private int mPnoScanIntervalMs;
    private final PnoScanListener mPnoScanListener;
    /* access modifiers changed from: private */
    public boolean mPnoScanStarted;
    private final AlarmManager.OnAlarmListener mRestartScanListener;
    private int mRssiScoreOffset;
    private int mRssiScoreSlope;
    private boolean mRunning;
    private int mSameNetworkBonus;
    /* access modifiers changed from: private */
    public int mScanRestartCount;
    private WifiScanner mScanner;
    private final ScoringParams mScoringParams;
    /* access modifiers changed from: private */
    public boolean mScreenOn;
    private int mSecureBonus;
    /* access modifiers changed from: private */
    public int mSingleScanFailCount;
    /* access modifiers changed from: private */
    public int mSingleScanRestartCount;
    private boolean mSpecificNetworkRequestInProgress;
    /* access modifiers changed from: private */
    public final ClientModeImpl mStateMachine;
    private int mTotalConnectivityAttemptsRateLimited;
    private boolean mTrustedConnectionAllowed;
    private boolean mUntrustedConnectionAllowed;
    /* access modifiers changed from: private */
    public boolean mUseSingleRadioChainScanResults;
    /* access modifiers changed from: private */
    public boolean mWaitForFullBandScanResults;
    private final AlarmManager.OnAlarmListener mWatchdogListener;
    /* access modifiers changed from: private */
    public boolean mWifiConnectivityManagerEnabled;
    /* access modifiers changed from: private */
    public boolean mWifiEnabled;
    private WifiGeofenceManager mWifiGeofenceManager;
    private final WifiInfo mWifiInfo;
    private final WifiInjector mWifiInjector;
    private final WifiLastResortWatchdog mWifiLastResortWatchdog;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;
    private WifiNotificationController mWifiNotificationController;
    private int mWifiState;

    static /* synthetic */ int access$1608(WifiConnectivityManager x0) {
        int i = x0.mSingleScanFailCount;
        x0.mSingleScanFailCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1708(WifiConnectivityManager x0) {
        int i = x0.mSingleScanRestartCount;
        x0.mSingleScanRestartCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1908(WifiConnectivityManager x0) {
        int i = x0.mScanRestartCount;
        x0.mScanRestartCount = i + 1;
        return i;
    }

    private static class BssidBlacklistStatus {
        public int blacklistedExpireTimeMs;
        public long blacklistedTimeStamp;
        public int counter;
        public boolean isBlacklisted;
        public int reasonCode;

        private BssidBlacklistStatus() {
            this.blacklistedTimeStamp = WifiConnectivityManager.RESET_TIME_STAMP;
        }
    }

    /* access modifiers changed from: private */
    public void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private class RestartSingleScanListener implements AlarmManager.OnAlarmListener {
        private final boolean mIsFullBandScan;

        RestartSingleScanListener(boolean isFullBandScan) {
            this.mIsFullBandScan = isFullBandScan;
        }

        public void onAlarm() {
            WifiConnectivityManager.this.startSingleScan(this.mIsFullBandScan, ClientModeImpl.WIFI_WORK_SOURCE);
        }
    }

    /* access modifiers changed from: private */
    public boolean handleScanResults(List<ScanDetail> scanDetails, String listenerName) {
        refreshBssidBlacklist();
        if (this.mStateMachine.isSupplicantTransientState()) {
            localLog(listenerName + " onResults: No network selection because supplicantTransientState is " + this.mStateMachine.isSupplicantTransientState());
            return false;
        } else if (this.mWifiState == 1 && !this.mEnableAutoJoinWhenAssociated) {
            localLog(listenerName + " onResults: No network selection because Wifi is connected. And auto roaming is disabled");
            return false;
        } else if (!WifiPolicyCache.getInstance(this.mContext).getAutomaticConnectionToWifi()) {
            localLog(listenerName + " onResults: No network selection because getAutomaticConnectionToWifi() is false");
            return false;
        } else {
            localLog(listenerName + " onResults: start network selection");
            WifiConfiguration candidate = this.mNetworkSelector.selectNetwork(scanDetails, buildBssidBlacklist(), this.mWifiInfo, this.mStateMachine.isConnected(), this.mStateMachine.isDisconnected(), this.mUntrustedConnectionAllowed, this.mBluetoothConnected);
            this.mWifiLastResortWatchdog.updateAvailableNetworks(this.mNetworkSelector.getConnectableScanDetails());
            this.mWifiMetrics.countScanResults(scanDetails);
            if (candidate != null) {
                localLog(listenerName + ":  WNS candidate-" + candidate.SSID);
                connectToNetwork(candidate);
                this.mWifiInjector.getPickerController().setEnable(false);
                return true;
            }
            if (this.mWifiState == 2 && this.mWifiEnabled) {
                this.mWifiNotificationController.handleScanResults(this.mNetworkSelector.getFilteredScanDetailsForOpenUnsavedNetworks());
                this.mWifiInjector.getPickerController().showPickerDialogIfNecessary();
            }
            clearBssidBlacklistExceptBssidPruned();
            return false;
        }
    }

    private class AllSingleScanListener implements WifiScanner.ScanListener {
        private int mNumScanResultsIgnoredDueToSingleRadioChain;
        private List<ScanDetail> mScanDetails;

        private AllSingleScanListener() {
            this.mScanDetails = new ArrayList();
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        public void onSuccess() {
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            wifiConnectivityManager.localLog("registerScanListener onFailure: reason: " + reason + " description: " + description);
        }

        public void onPeriodChanged(int periodInMs) {
        }

        public void onResults(WifiScanner.ScanData[] results) {
            if (!WifiConnectivityManager.this.mWifiEnabled || !WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                clearScanDetails();
                boolean unused = WifiConnectivityManager.this.mWaitForFullBandScanResults = false;
            } else if (WifiConnectivityManager.this.mStateMachine.getConcurrentEnabled()) {
                WifiConnectivityManager.this.localLog("AllSingleScanListener do not handleScanResults() since concurrent is enabled.");
                clearScanDetails();
            } else {
                boolean isFullBandScanResults = results[0].getBandScanned() == 7 || results[0].getBandScanned() == 3;
                if (WifiConnectivityManager.this.mWaitForFullBandScanResults) {
                    if (!isFullBandScanResults) {
                        WifiConnectivityManager.this.localLog("AllSingleScanListener waiting for full band scan results.");
                        clearScanDetails();
                        return;
                    }
                    boolean unused2 = WifiConnectivityManager.this.mWaitForFullBandScanResults = false;
                }
                if (results.length > 0) {
                    WifiConnectivityManager.this.mWifiMetrics.incrementAvailableNetworksHistograms(this.mScanDetails, isFullBandScanResults);
                    WifiConnectivityManager.this.mStateMachine.sendMessage(ClientModeImpl.CMD_SCAN_RESULT_AVAILABLE);
                }
                if (this.mNumScanResultsIgnoredDueToSingleRadioChain > 0) {
                    Log.i(WifiConnectivityManager.TAG, "Number of scan results ignored due to single radio chain scan: " + this.mNumScanResultsIgnoredDueToSingleRadioChain);
                }
                boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "AllSingleScanListener");
                clearScanDetails();
                if (!WifiConnectivityManager.this.mPnoScanStarted) {
                    return;
                }
                if (wasConnectAttempted) {
                    WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoBad();
                } else {
                    WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoGood();
                }
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
            if (WifiConnectivityManager.this.mWifiEnabled && WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                if (WifiConnectivityManager.this.mDbg) {
                    WifiConnectivityManager.this.localLog("AllSingleScanListener onFullResult: " + fullScanResult.SSID + " capabilities " + fullScanResult.capabilities);
                }
                if (WifiConnectivityManager.this.mUseSingleRadioChainScanResults || fullScanResult.radioChainInfos == null || fullScanResult.radioChainInfos.length != 1) {
                    this.mScanDetails.add(ScanResultUtil.toScanDetail(fullScanResult));
                } else {
                    this.mNumScanResultsIgnoredDueToSingleRadioChain++;
                }
            }
        }
    }

    private class SingleScanListener implements WifiScanner.ScanListener {
        private final boolean mIsFullBandScan;

        SingleScanListener(boolean isFullBandScan) {
            this.mIsFullBandScan = isFullBandScan;
        }

        public void onSuccess() {
            int unused = WifiConnectivityManager.this.mSingleScanFailCount = 0;
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            wifiConnectivityManager.localLog("SingleScanListener onFailure: reason: " + reason + " description: " + description);
            if (WifiConnectivityManager.this.mStateMachine.getConcurrentEnabled()) {
                WifiConnectivityManager.this.localLog("Do not schedule delayed single scan since concurrent is enabled");
                return;
            }
            if (WifiConnectivityManager.access$1708(WifiConnectivityManager.this) < 5) {
                WifiConnectivityManager.this.scheduleDelayedSingleScan(this.mIsFullBandScan);
            } else {
                int unused = WifiConnectivityManager.this.mSingleScanRestartCount = 0;
                WifiConnectivityManager.this.localLog("Failed to successfully start single scan for 5 times");
            }
            if (WifiConnectivityManager.access$1608(WifiConnectivityManager.this) > 10) {
                int unused2 = WifiConnectivityManager.this.mSingleScanFailCount = 0;
                WifiConnectivityManager.this.mStateMachine.report(ReportIdKey.ID_SCAN_FAIL, ReportUtil.getReportDataForScanFail(5, reason));
            }
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            wifiConnectivityManager.localLog("SingleScanListener onPeriodChanged: actual scan period " + periodInMs + "ms");
        }

        public void onResults(WifiScanner.ScanData[] results) {
            int unused = WifiConnectivityManager.this.mSingleScanRestartCount = 0;
        }

        public void onFullResult(ScanResult fullScanResult) {
        }
    }

    private class PnoScanListener implements WifiScanner.PnoScanListener {
        private int mLowRssiNetworkRetryDelay;
        private List<ScanDetail> mScanDetails;

        private PnoScanListener() {
            this.mScanDetails = new ArrayList();
            this.mLowRssiNetworkRetryDelay = 20000;
        }

        public void clearScanDetails() {
            this.mScanDetails.clear();
        }

        public void resetLowRssiNetworkRetryDelay() {
            this.mLowRssiNetworkRetryDelay = 20000;
        }

        @VisibleForTesting
        public int getLowRssiNetworkRetryDelay() {
            return this.mLowRssiNetworkRetryDelay;
        }

        public void onSuccess() {
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            wifiConnectivityManager.localLog("PnoScanListener onFailure: reason: " + reason + " description: " + description);
            if (WifiConnectivityManager.access$1908(WifiConnectivityManager.this) < 5) {
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(WifiConnectivityManager.RESTART_SCAN_DELAY_MS);
                return;
            }
            int unused = WifiConnectivityManager.this.mScanRestartCount = 0;
            WifiConnectivityManager.this.localLog("Failed to successfully start PNO scan for 5 times");
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
            wifiConnectivityManager.localLog("PnoScanListener onPeriodChanged: actual scan period " + periodInMs + "ms");
        }

        public void onResults(WifiScanner.ScanData[] results) {
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPnoNetworkFound(ScanResult[] results) {
            for (ScanResult result : results) {
                if (result.informationElements == null) {
                    WifiConnectivityManager.this.localLog("Skipping scan result with null information elements");
                } else {
                    this.mScanDetails.add(ScanResultUtil.toScanDetail(result));
                }
            }
            boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "PnoScanListener");
            clearScanDetails();
            int unused = WifiConnectivityManager.this.mScanRestartCount = 0;
            if (!wasConnectAttempted) {
                if (this.mLowRssiNetworkRetryDelay > WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS) {
                    this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS;
                }
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(this.mLowRssiNetworkRetryDelay);
                this.mLowRssiNetworkRetryDelay *= 2;
                return;
            }
            resetLowRssiNetworkRetryDelay();
        }
    }

    private class OnSavedNetworkUpdateListener implements WifiConfigManager.OnSavedNetworkUpdateListener {
        private OnSavedNetworkUpdateListener() {
        }

        public void onSavedNetworkAdded(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkEnabled(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkRemoved(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkUpdated(int networkId) {
            WifiConnectivityManager.this.mStateMachine.updateCapabilities();
            updatePnoScan();
        }

        public void onSavedNetworkTemporarilyDisabled(int networkId, int disableReason) {
            if (disableReason != 6) {
                WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(networkId);
            }
        }

        public void onSavedNetworkPermanentlyDisabled(int networkId, int disableReason) {
            if (disableReason != 10 && disableReason != 15) {
                WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(networkId);
                updatePnoScan();
            }
        }

        private void updatePnoScan() {
            if (!WifiConnectivityManager.this.mScreenOn) {
                WifiConnectivityManager.this.localLog("Saved networks updated");
            }
        }
    }

    WifiConnectivityManager(Context context, ScoringParams scoringParams, ClientModeImpl stateMachine, WifiInjector injector, WifiConfigManager configManager, WifiInfo wifiInfo, WifiNetworkSelector networkSelector, WifiConnectivityHelper connectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, CarrierNetworkNotifier carrierNetworkNotifier, CarrierNetworkConfig carrierNetworkConfig, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog) {
        Context context2 = context;
        this.mFirstMaxPeriodicScanTimeStamp = RESET_TIME_STAMP;
        this.mDbg = false;
        this.mWifiEnabled = false;
        this.mWifiConnectivityManagerEnabled = false;
        this.mRunning = false;
        this.mScreenOn = false;
        this.mWifiState = 0;
        this.mUntrustedConnectionAllowed = false;
        this.mTrustedConnectionAllowed = false;
        this.mSpecificNetworkRequestInProgress = false;
        this.mScanRestartCount = 0;
        this.mSingleScanRestartCount = 0;
        this.mSingleScanFailCount = 0;
        this.mTotalConnectivityAttemptsRateLimited = 0;
        this.mLastConnectionAttemptBssid = null;
        this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
        this.mPnoScanStarted = false;
        this.mPeriodicScanTimerSet = false;
        this.mMaxPeriodicScanTimerSet = false;
        this.mMaxSleepPeriodicScanTimerSet = false;
        this.mMaxSleepPeriodicScanTimerAuxSet = false;
        this.mOneShotScan = false;
        this.mBluetoothConnected = false;
        this.mWaitForFullBandScanResults = false;
        this.mUseSingleRadioChainScanResults = false;
        this.mBssidBlacklist = new HashMap();
        this.mRestartScanListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.startConnectivityScan(true);
            }
        };
        this.mWatchdogListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.watchdogHandler();
            }
        };
        this.mPeriodicScanTimerListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.periodicScanTimerHandler();
            }
        };
        this.mMaxPeriodicScanTimerListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.localLog("mMaxPeriodicScanTimerListener, onAlarm");
                WifiConnectivityManager.this.maxPeriodicScanTimerHandler();
            }
        };
        this.mMaxSleepPeriodicScanTimerListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.localLog("mMaxSleepPeriodicScanTimerListener, onAlarm");
                WifiConnectivityManager.this.maxPeriodicScanTimerHandler();
            }
        };
        this.mMaxSleepPeriodicScanTimerAuxListener = new AlarmManager.OnAlarmListener() {
            public void onAlarm() {
                WifiConnectivityManager.this.localLog("mMaxSleepPeriodicScanTimerAuxListener, onAlarm");
            }
        };
        this.mAllSingleScanListener = new AllSingleScanListener();
        this.mPnoScanListener = new PnoScanListener();
        this.mContext = context2;
        this.mStateMachine = stateMachine;
        this.mWifiInjector = injector;
        this.mConfigManager = configManager;
        this.mWifiInfo = wifiInfo;
        this.mNetworkSelector = networkSelector;
        this.mConnectivityHelper = connectivityHelper;
        this.mLocalLog = localLog;
        this.mWifiLastResortWatchdog = wifiLastResortWatchdog;
        this.mOpenNetworkNotifier = openNetworkNotifier;
        this.mCarrierNetworkNotifier = carrierNetworkNotifier;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        this.mWifiMetrics = wifiMetrics;
        this.mAlarmManager = (AlarmManager) context2.getSystemService("alarm");
        this.mEventHandler = new Handler(looper);
        this.mClock = clock;
        this.mScoringParams = scoringParams;
        this.mConnectionAttemptTimeStamps = new LinkedList<>();
        this.mBand5GHzBonus = context.getResources().getInteger(17694972);
        this.mCurrentConnectionBonus = context.getResources().getInteger(17694989);
        this.mSameNetworkBonus = context.getResources().getInteger(17694979);
        this.mSecureBonus = context.getResources().getInteger(17694980);
        this.mRssiScoreOffset = context.getResources().getInteger(17694977);
        this.mRssiScoreSlope = context.getResources().getInteger(17694978);
        this.mEnableAutoJoinWhenAssociated = CSC_SUPPORT_ASSOCIATED_NETWORK_SELECTION;
        this.mUseSingleRadioChainScanResults = context.getResources().getBoolean(17891605);
        this.mFullScanMaxTxRate = context.getResources().getInteger(17694992);
        this.mFullScanMaxRxRate = context.getResources().getInteger(17694991);
        this.mPnoScanIntervalMs = 20000;
        localLog("PNO settings: min5GHzRssi " + this.mScoringParams.getEntryRssi(5000) + " min24GHzRssi " + this.mScoringParams.getEntryRssi(ScoringParams.BAND2) + " currentConnectionBonus " + this.mCurrentConnectionBonus + " sameNetworkBonus " + this.mSameNetworkBonus + " secureNetworkBonus " + this.mSecureBonus + " initialScoreMax " + initialScoreMax());
        StringBuilder sb = new StringBuilder();
        sb.append("Passpoint is: ");
        sb.append(1 != 0 ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mConfigManager.setOnSavedNetworkUpdateListener(new OnSavedNetworkUpdateListener());
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    WifiConnectivityManager(Context context, ScoringParams scoringParams, ClientModeImpl stateMachine, WifiInjector injector, WifiConfigManager configManager, WifiInfo wifiInfo, WifiNetworkSelector networkSelector, WifiConnectivityHelper connectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, WifiNotificationController wifiNotificationController, CarrierNetworkNotifier carrierNetworkNotifier, CarrierNetworkConfig carrierNetworkConfig, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog, PasspointManager passpointManager, WifiGeofenceManager geofenceManager) {
        this(context, scoringParams, stateMachine, injector, configManager, wifiInfo, networkSelector, connectivityHelper, wifiLastResortWatchdog, openNetworkNotifier, carrierNetworkNotifier, carrierNetworkConfig, wifiMetrics, looper, clock, localLog);
        this.mWifiNotificationController = wifiNotificationController;
        this.mPasspointManager = passpointManager;
        this.mWifiGeofenceManager = geofenceManager;
    }

    private int initialScoreMax() {
        return this.mRssiScoreSlope * (Math.max(this.mScoringParams.getGoodRssi(ScoringParams.BAND2), this.mScoringParams.getGoodRssi(5000)) + this.mRssiScoreOffset);
    }

    private boolean shouldSkipConnectionAttempt(Long timeMillis) {
        Iterator<Long> attemptIter = this.mConnectionAttemptTimeStamps.iterator();
        while (attemptIter.hasNext() && timeMillis.longValue() - attemptIter.next().longValue() > 240000) {
            attemptIter.remove();
        }
        return this.mConnectionAttemptTimeStamps.size() >= 6;
    }

    private void noteConnectionAttempt(Long timeMillis) {
        this.mConnectionAttemptTimeStamps.addLast(timeMillis);
    }

    private void clearConnectionAttemptTimeStamps() {
        this.mConnectionAttemptTimeStamps.clear();
    }

    private void connectToNetwork(WifiConfiguration candidate) {
        String currentAssociationId;
        ScanResult scanResultCandidate = candidate.getNetworkSelectionStatus().getCandidate();
        if (scanResultCandidate == null) {
            localLog("connectToNetwork: bad candidate - " + candidate + " scanResult: " + scanResultCandidate);
            return;
        }
        String targetBssid = scanResultCandidate.BSSID;
        String targetAssociationId = candidate.SSID + " : " + targetBssid;
        if (targetBssid != null && ((targetBssid.equals(this.mLastConnectionAttemptBssid) || targetBssid.equals(this.mWifiInfo.getBSSID())) && SupplicantState.isConnecting(this.mWifiInfo.getSupplicantState()))) {
            localLog("connectToNetwork: Either already connected or is connecting to " + targetAssociationId);
        } else if (candidate.BSSID == null || candidate.BSSID.equals("any") || candidate.BSSID.equals(targetBssid)) {
            long elapsedTimeMillis = this.mClock.getElapsedSinceBootMillis();
            if (this.mScreenOn || !shouldSkipConnectionAttempt(Long.valueOf(elapsedTimeMillis))) {
                noteConnectionAttempt(Long.valueOf(elapsedTimeMillis));
                this.mLastConnectionAttemptBssid = targetBssid;
                WifiConfiguration currentConnectedNetwork = this.mConfigManager.getConfiguredNetwork(this.mWifiInfo.getNetworkId());
                if (currentConnectedNetwork == null) {
                    currentAssociationId = "Disconnected";
                } else {
                    currentAssociationId = this.mWifiInfo.getSSID() + " : " + this.mWifiInfo.getBSSID();
                }
                if (currentConnectedNetwork == null || currentConnectedNetwork.networkId != candidate.networkId) {
                    localLog("connectToNetwork: Connect to " + targetAssociationId + " from " + currentAssociationId);
                    this.mStateMachine.startConnectToNetwork(candidate.networkId, 1010, targetBssid);
                } else if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                    localLog("connectToNetwork: Roaming candidate - " + targetAssociationId + ". The actual roaming target is up to the firmware.");
                } else {
                    localLog("connectToNetwork: Roaming to " + targetAssociationId + " from " + currentAssociationId);
                    this.mStateMachine.startRoamToNetwork(candidate.networkId, scanResultCandidate);
                }
            } else {
                localLog("connectToNetwork: Too many connection attempts. Skipping this attempt!");
                this.mTotalConnectivityAttemptsRateLimited++;
            }
        } else {
            localLog("connecToNetwork: target BSSID " + targetBssid + " does not match the config specified BSSID " + candidate.BSSID + ". Drop it!");
        }
    }

    private int getScanBand() {
        return getScanBand(true);
    }

    private int getScanBand(boolean isFullBandScan) {
        if (isFullBandScan) {
            return 7;
        }
        return 0;
    }

    private boolean setScanChannels(WifiScanner.ScanSettings settings) {
        WifiConfiguration config = this.mStateMachine.getCurrentWifiConfiguration();
        if (config == null) {
            return false;
        }
        Set<Integer> freqs = this.mConfigManager.fetchChannelSetForNetworkForPartialScan(config.networkId, 3600000, this.mWifiInfo.getFrequency());
        if (freqs == null || freqs.size() == 0) {
            localLog("No scan channels for " + config.configKey() + ". Perform full band scan");
            return false;
        }
        int index = 0;
        settings.channels = new WifiScanner.ChannelSpec[freqs.size()];
        for (Integer freq : freqs) {
            settings.channels[index] = new WifiScanner.ChannelSpec(freq.intValue());
            index++;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void watchdogHandler() {
        if (this.mWifiState == 2) {
            localLog("start a single scan from watchdogHandler");
            scheduleWatchdogTimer();
            startSingleScan(true, ClientModeImpl.WIFI_WORK_SOURCE);
        }
    }

    private void startPeriodicSingleScan() {
        long currentTimeStamp = this.mClock.getElapsedSinceBootMillis();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("startPeriodicSingleScan mPeriodicSingleScanInterval: " + this.mPeriodicSingleScanInterval);
        if (this.mWifiGeofenceManager.isSupported()) {
            this.mWifiGeofenceManager.setScanInterval(this.mPeriodicSingleScanInterval / 1000, getMaxPeriodicScanInterval() / 1000);
            sbuf.append(", isInRange: " + this.mWifiGeofenceManager.isInRange());
        }
        localLog(sbuf.toString());
        long j = this.mLastPeriodicSingleScanTimeStamp;
        if (j != RESET_TIME_STAMP) {
            long msSinceLastScan = currentTimeStamp - j;
            if (msSinceLastScan < 8000) {
                localLog("Last periodic single scan started " + msSinceLastScan + "ms ago, defer this new scan request.");
                schedulePeriodicScanTimer(8000 - ((int) msSinceLastScan));
                return;
            }
        }
        boolean isScanNeeded = true;
        boolean isFullBandScan = true;
        boolean isTrafficOverThreshold = this.mWifiInfo.txSuccessRate > ((double) this.mFullScanMaxTxRate) || this.mWifiInfo.rxSuccessRate > ((double) this.mFullScanMaxRxRate);
        if (this.mWifiState == 1 && isTrafficOverThreshold) {
            if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                localLog("No partial scan because firmware roaming is supported.");
                isScanNeeded = false;
            } else {
                localLog("No full band scan due to ongoing traffic");
                isFullBandScan = false;
            }
        }
        if (isScanNeeded) {
            this.mLastPeriodicSingleScanTimeStamp = currentTimeStamp;
            startSingleScan(isFullBandScan, ClientModeImpl.WIFI_WORK_SOURCE);
            schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
            this.mPeriodicSingleScanInterval *= 2;
            int maxPeriodicScanInterval = getMaxPeriodicScanInterval();
            if (this.mPeriodicSingleScanInterval > maxPeriodicScanInterval) {
                this.mPeriodicSingleScanInterval = maxPeriodicScanInterval;
                return;
            }
            return;
        }
        schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
    }

    /* access modifiers changed from: private */
    public void maxPeriodicScanTimerHandler() {
        localLog("maxPeriodicScanTimerHandler");
        if (!this.mScreenOn && this.mWifiState == 2) {
            Log.i(TAG, "start a single scan from maxPeriodicScanTimerHandler");
            if (SemSarManager.isRfTestMode()) {
                scheduleMaxPeriodicScanTimer();
            } else if (!shouldMaxPeriodicScanOnSleep()) {
                scheduleMaxSleepPeriodicScanTimer();
                scheduleMaxSleepPeriodicScanTimerAux();
            } else if (this.mLastPeriodicSingleScanTimeStamp == RESET_TIME_STAMP) {
                localLog("mLastPeriodicSingleScanTimeStamp is not set");
            } else if (this.mClock.getElapsedSinceBootMillis() - this.mFirstMaxPeriodicScanTimeStamp < 3600000) {
                scheduleMaxPeriodicScanTimer();
            } else {
                localLog("since first max periodic scan timer is triggered before 1 hour, change to max periodic scanner");
                resetFirstMaxPeriodicSingleScanTimeStamp();
                localLog("paranoid! forcely set mImsCallEstablished as false");
                this.mStateMachine.setImsCallEstablished(false);
            }
            startSingleScan(true, ClientModeImpl.WIFI_WORK_SOURCE);
        }
    }

    private void resetLastPeriodicSingleScanTimeStamp() {
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    }

    /* access modifiers changed from: private */
    public void periodicScanTimerHandler() {
        localLog("periodicScanTimerHandler");
        if (this.mScreenOn) {
            startPeriodicSingleScan();
        }
    }

    /* access modifiers changed from: private */
    public void startSingleScan(boolean isFullBandScan, WorkSource workSource) {
        if (this.mScanner == null) {
            localLog("mScanner is null. Skipping this SingleScan!");
        } else if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            if (this.mStateMachine.getConcurrentEnabled()) {
                localLog("Concurrent is enabled. Skipping this SingleScan!");
                return;
            }
            this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
            WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
            if (!isFullBandScan && !setScanChannels(settings)) {
                isFullBandScan = true;
            }
            settings.type = 2;
            settings.band = getScanBand(isFullBandScan);
            settings.reportEvents = 3;
            settings.numBssidsPerScan = 0;
            List<WifiScanner.ScanSettings.HiddenNetwork> hiddenNetworkList = this.mConfigManager.retrieveHiddenNetworkList();
            settings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) hiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[hiddenNetworkList.size()]);
            settings.semPackageName = TAG;
            this.mScanner.startScan(settings, new SingleScanListener(isFullBandScan), workSource);
            this.mWifiMetrics.incrementConnectivityOneshotScanCount();
        }
    }

    private void startPeriodicScan(boolean scanImmediately) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        if (this.mWifiState != 1 || this.mEnableAutoJoinWhenAssociated) {
            if (scanImmediately) {
                resetLastPeriodicSingleScanTimeStamp();
            }
            this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
            startPeriodicSingleScan();
        }
    }

    private static int deviceMobilityStateToPnoScanIntervalMs(int state) {
        if (state == 0 || state == 1 || state == 2) {
            return 20000;
        }
        if (state != 3) {
            return -1;
        }
        return 60000;
    }

    public void setDeviceMobilityState(int newState) {
        int newPnoScanIntervalMs = deviceMobilityStateToPnoScanIntervalMs(newState);
        if (newPnoScanIntervalMs < 0) {
            Log.e(TAG, "Invalid device mobility state: " + newState);
        } else if (newPnoScanIntervalMs != this.mPnoScanIntervalMs) {
            this.mPnoScanIntervalMs = newPnoScanIntervalMs;
            Log.d(TAG, "PNO Scan Interval changed to " + this.mPnoScanIntervalMs + " ms.");
            if (this.mPnoScanStarted) {
                Log.d(TAG, "Restarting PNO Scan with new scan interval");
                stopPnoScan();
                this.mWifiMetrics.enterDeviceMobilityState(newState);
                startDisconnectedPnoScan();
                return;
            }
            this.mWifiMetrics.enterDeviceMobilityState(newState);
        } else if (this.mPnoScanStarted) {
            this.mWifiMetrics.logPnoScanStop();
            this.mWifiMetrics.enterDeviceMobilityState(newState);
            this.mWifiMetrics.logPnoScanStart();
        } else {
            this.mWifiMetrics.enterDeviceMobilityState(newState);
        }
    }

    private void startDisconnectedPnoScan() {
    }

    private void stopPnoScan() {
        if (this.mPnoScanStarted) {
            this.mScanner.stopPnoScan(this.mPnoScanListener);
            this.mPnoScanStarted = false;
            this.mWifiMetrics.logPnoScanStop();
        }
    }

    private void scheduleWatchdogTimer() {
        localLog("scheduleWatchdogTimer");
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 1200000, WATCHDOG_TIMER_TAG, this.mWatchdogListener, this.mEventHandler);
    }

    private void schedulePeriodicScanTimer(int intervalMs) {
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + ((long) intervalMs), PERIODIC_SCAN_TIMER_TAG, this.mPeriodicScanTimerListener, this.mEventHandler);
        this.mPeriodicScanTimerSet = true;
    }

    private void cancelPeriodicScanTimer() {
        if (this.mPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mPeriodicScanTimerListener);
            this.mPeriodicScanTimerSet = false;
        }
    }

    private void scheduleMaxPeriodicScanTimer() {
        this.mAlarmManager.setExact(2, this.mClock.getElapsedSinceBootMillis() + ((long) getMaxPeriodicScanInterval()), MAX_PERIODIC_SCAN_TIMER_TAG, this.mMaxPeriodicScanTimerListener, this.mEventHandler);
        this.mMaxPeriodicScanTimerSet = true;
    }

    private void cancelMaxPeriodicScanTimer() {
        if (this.mMaxPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mMaxPeriodicScanTimerListener);
            this.mMaxPeriodicScanTimerSet = false;
        }
    }

    private void scheduleMaxSleepPeriodicScanTimer() {
        this.mAlarmManager.setExact(3, this.mClock.getElapsedSinceBootMillis() + ((long) getMaxPeriodicScanInterval()), MAX_SLEEP_PERIODIC_SCAN_TIMER_TAG, this.mMaxSleepPeriodicScanTimerListener, this.mEventHandler);
        this.mMaxSleepPeriodicScanTimerSet = true;
    }

    private void cancelMaxSleepPeriodicScanTimer() {
        if (this.mMaxSleepPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mMaxSleepPeriodicScanTimerListener);
            this.mMaxSleepPeriodicScanTimerSet = false;
        }
    }

    private void scheduleMaxSleepPeriodicScanTimerAux() {
        this.mAlarmManager.setExact(3, (this.mClock.getElapsedSinceBootMillis() + ((long) getMaxPeriodicScanInterval())) - 5, MAX_SLEEP_PERIODIC_SCAN_TIMER_AUX_TAG, this.mMaxSleepPeriodicScanTimerAuxListener, this.mEventHandler);
        this.mMaxSleepPeriodicScanTimerAuxSet = true;
    }

    private void cancelMaxSleepPeriodicScanTimerAux() {
        if (this.mMaxSleepPeriodicScanTimerAuxSet) {
            this.mAlarmManager.cancel(this.mMaxSleepPeriodicScanTimerAuxListener);
            this.mMaxSleepPeriodicScanTimerAuxSet = false;
        }
    }

    /* access modifiers changed from: private */
    public void scheduleDelayedSingleScan(boolean isFullBandScan) {
        localLog("scheduleDelayedSingleScan");
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + 2000, RESTART_SINGLE_SCAN_TIMER_TAG, new RestartSingleScanListener(isFullBandScan), this.mEventHandler);
    }

    /* access modifiers changed from: private */
    public void scheduleDelayedConnectivityScan(int msFromNow) {
        localLog("scheduleDelayedConnectivityScan");
        this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + ((long) msFromNow), RESTART_CONNECTIVITY_SCAN_TIMER_TAG, this.mRestartScanListener, this.mEventHandler);
    }

    /* access modifiers changed from: private */
    public void startConnectivityScan(boolean scanImmediately) {
        localLog("startConnectivityScan: screenOn=" + this.mScreenOn + " wifiState=" + stateToString(this.mWifiState) + " scanImmediately=" + scanImmediately + " wifiEnabled=" + this.mWifiEnabled + " wifiConnectivityManagerEnabled=" + this.mWifiConnectivityManagerEnabled + " oneShotScan=" + this.mOneShotScan);
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            stopConnectivityScan();
            int i = this.mWifiState;
            if (i != 1 && i != 2) {
                return;
            }
            if (this.mScreenOn) {
                startPeriodicScan(scanImmediately);
            } else if (this.mWifiState == 2) {
                if (this.mOneShotScan) {
                    this.mOneShotScan = false;
                    localLog("start a single scan from oneShotScan");
                    startSingleScan(true, ClientModeImpl.WIFI_WORK_SOURCE);
                }
                if (this.mWifiGeofenceManager.isSupported()) {
                    this.mWifiGeofenceManager.setScanInterval(this.mPeriodicSingleScanInterval / 1000, getMaxPeriodicScanInterval() / 1000);
                }
                if (shouldMaxPeriodicScanOnSleep()) {
                    setFirstMaxPeriodicSingleScanTimeStamp();
                    scheduleMaxPeriodicScanTimer();
                    return;
                }
                scheduleMaxSleepPeriodicScanTimer();
                scheduleMaxSleepPeriodicScanTimerAux();
            }
        }
    }

    private void stopConnectivityScan() {
        cancelPeriodicScanTimer();
        cancelMaxPeriodicScanTimer();
        cancelMaxSleepPeriodicScanTimer();
        cancelMaxSleepPeriodicScanTimerAux();
        stopPnoScan();
        this.mScanRestartCount = 0;
    }

    public void handleScreenStateChanged(boolean screenOn) {
        localLog("handleScreenStateChanged: screenOn=" + screenOn);
        this.mScreenOn = screenOn;
        this.mWifiNotificationController.handleScreenStateChanged(screenOn);
        startConnectivityScan(false);
    }

    private static String stateToString(int state) {
        if (state == 1) {
            return "connected";
        }
        if (state == 2) {
            return "disconnected";
        }
        if (state != 3) {
            return "unknown";
        }
        return "transitioning";
    }

    public void handleConnectionStateChanged(int state) {
        localLog("handleConnectionStateChanged: state=" + stateToString(state));
        this.mWifiState = state;
        if (this.mWifiState == 1) {
            this.mWifiNotificationController.clearPendingNotification(true);
        }
        if (this.mWifiState == 2) {
            this.mLastConnectionAttemptBssid = null;
            startConnectivityScan(true);
            return;
        }
        startConnectivityScan(false);
    }

    public void handleConnectionAttemptEnded(int failureCode) {
        if (failureCode == 1) {
            if (this.mWifiInfo.getWifiSsid() != null) {
                String wifiSsid = this.mWifiInfo.getWifiSsid().toString();
            }
            this.mWifiNotificationController.clearPendingNotification(true);
        }
    }

    private void checkStateAndEnable() {
        enable(!this.mSpecificNetworkRequestInProgress && (this.mUntrustedConnectionAllowed || this.mTrustedConnectionAllowed));
        startConnectivityScan(true);
    }

    public void setTrustedConnectionAllowed(boolean allowed) {
        localLog("setTrustedConnectionAllowed: allowed=" + allowed);
        if (this.mTrustedConnectionAllowed != allowed) {
            this.mTrustedConnectionAllowed = allowed;
            checkStateAndEnable();
        }
    }

    public void resetPeriodicScanTime() {
        localLog("resetPeriodicScanTime");
        startConnectivityScan(true);
    }

    public void setUntrustedConnectionAllowed(boolean allowed) {
        localLog("setUntrustedConnectionAllowed: allowed=" + allowed);
        if (this.mUntrustedConnectionAllowed != allowed) {
            this.mUntrustedConnectionAllowed = allowed;
            checkStateAndEnable();
        }
    }

    public void setSpecificNetworkRequestInProgress(boolean inProgress) {
        localLog("setsetSpecificNetworkRequestInProgress : inProgress=" + inProgress);
        if (this.mSpecificNetworkRequestInProgress != inProgress) {
            this.mSpecificNetworkRequestInProgress = inProgress;
            checkStateAndEnable();
        }
    }

    public void setUserConnectChoice(int netId) {
        localLog("setUserConnectChoice: netId=" + netId);
        this.mNetworkSelector.setUserConnectChoice(netId);
    }

    public void prepareForForcedConnection(int netId) {
        localLog("prepareForForcedConnection: netId=" + netId);
        clearConnectionAttemptTimeStamps();
        clearBssidBlacklist();
    }

    public void forceConnectivityScan(WorkSource workSource) {
        localLog("forceConnectivityScan in request of " + workSource);
        this.mWaitForFullBandScanResults = true;
        startSingleScan(true, workSource);
    }

    private boolean updateBssidBlacklist(String bssid, boolean enable, int reasonCode, int blacklistedExpireTimeMs) {
        if (enable) {
            if (this.mBssidBlacklist.remove(bssid) != null) {
                return true;
            }
            return false;
        } else if (this.mWifiLastResortWatchdog.shouldIgnoreBssidUpdate(bssid)) {
            localLog("Ignore update Bssid Blacklist since Watchdog trigger is activated");
            return false;
        } else {
            BssidBlacklistStatus status = this.mBssidBlacklist.get(bssid);
            if (status == null) {
                status = new BssidBlacklistStatus();
                status.reasonCode = reasonCode;
                status.blacklistedExpireTimeMs = blacklistedExpireTimeMs;
                this.mBssidBlacklist.put(bssid, status);
            }
            status.blacklistedTimeStamp = this.mClock.getElapsedSinceBootMillis();
            status.counter++;
            if (status.isBlacklisted || (status.counter < 3 && reasonCode != 17 && (65536 > reasonCode || reasonCode > 65539))) {
                return false;
            }
            status.isBlacklisted = true;
            return true;
        }
    }

    public boolean trackBssid(String bssid, boolean enable, int reasonCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("trackBssid: ");
        sb.append(enable ? "enable " : "disable ");
        sb.append(bssid);
        sb.append(" reason code ");
        sb.append(reasonCode);
        localLog(sb.toString());
        if (bssid == null || !updateBssidBlacklist(bssid, enable, reasonCode, BSSID_BLACKLIST_EXPIRE_TIME_MS)) {
            return false;
        }
        updateFirmwareRoamingConfiguration();
        if (!enable) {
            startConnectivityScan(true);
        }
        return true;
    }

    public boolean trackBssid(String bssid, boolean enable, int reasonCode, int timeRemaining) {
        StringBuilder sb = new StringBuilder();
        sb.append("trackBssid: ");
        sb.append(enable ? "enable " : "disable ");
        sb.append(bssid);
        sb.append(" reason code ");
        sb.append(String.format("0x%08X", new Object[]{Integer.valueOf(reasonCode)}));
        sb.append(" time remaining ");
        sb.append(timeRemaining);
        localLog(sb.toString());
        if (bssid == null) {
            return false;
        }
        if (!updateBssidBlacklist(bssid, enable, reasonCode, timeRemaining == Integer.MIN_VALUE ? BSSID_BLACKLIST_EXPIRE_TIME_MS : timeRemaining)) {
            return false;
        }
        updateFirmwareRoamingConfiguration();
        if (!enable) {
            startConnectivityScan(true);
        }
        return true;
    }

    @VisibleForTesting
    public boolean isBssidDisabled(String bssid) {
        BssidBlacklistStatus status = this.mBssidBlacklist.get(bssid);
        if (status == null) {
            return false;
        }
        return status.isBlacklisted;
    }

    private HashSet<String> buildBssidBlacklist() {
        HashSet<String> blacklistedBssids = new HashSet<>();
        for (String bssid : this.mBssidBlacklist.keySet()) {
            if (isBssidDisabled(bssid)) {
                blacklistedBssids.add(bssid);
            }
        }
        return blacklistedBssids;
    }

    private void updateFirmwareRoamingConfiguration() {
        if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
            int maxBlacklistSize = this.mConnectivityHelper.getMaxNumBlacklistBssid();
            if (maxBlacklistSize < 0) {
                Log.wtf(TAG, "Invalid max BSSID blacklist size:  " + maxBlacklistSize);
            } else if (maxBlacklistSize == 0) {
                Log.d(TAG, "Skip setting firmware roaming configuration since max BSSID blacklist size is zero");
            } else {
                ArrayList<String> blacklistedBssids = new ArrayList<>(buildBssidBlacklist());
                int blacklistSize = blacklistedBssids.size();
                if (blacklistSize > maxBlacklistSize) {
                    Log.wtf(TAG, "Attempt to write " + blacklistSize + " blacklisted BSSIDs, max size is " + maxBlacklistSize);
                    blacklistedBssids = new ArrayList<>(blacklistedBssids.subList(0, maxBlacklistSize));
                    localLog("Trim down BSSID blacklist size from " + blacklistSize + " to " + blacklistedBssids.size());
                }
                if (!this.mConnectivityHelper.setFirmwareRoamingConfiguration(blacklistedBssids, new ArrayList())) {
                    localLog("Failed to set firmware roaming configuration.");
                }
            }
        }
    }

    private void refreshBssidBlacklist() {
        if (!this.mBssidBlacklist.isEmpty()) {
            boolean updated = false;
            Iterator<BssidBlacklistStatus> iter = this.mBssidBlacklist.values().iterator();
            Long currentTimeStamp = Long.valueOf(this.mClock.getElapsedSinceBootMillis());
            while (iter.hasNext()) {
                BssidBlacklistStatus status = iter.next();
                if (status.isBlacklisted && currentTimeStamp.longValue() - status.blacklistedTimeStamp >= ((long) status.blacklistedExpireTimeMs)) {
                    iter.remove();
                    updated = true;
                }
            }
            if (updated) {
                updateFirmwareRoamingConfiguration();
            }
        }
    }

    private void retrieveWifiScanner() {
        if (this.mScanner == null) {
            this.mScanner = this.mWifiInjector.getWifiScanner();
            Preconditions.checkNotNull(this.mScanner);
            this.mScanner.registerScanListener(this.mAllSingleScanListener);
        }
    }

    private void clearBssidBlacklist() {
        this.mBssidBlacklist.clear();
        updateFirmwareRoamingConfiguration();
    }

    private void clearBssidBlacklistExceptBssidPruned() {
        Iterator<BssidBlacklistStatus> iter = this.mBssidBlacklist.values().iterator();
        while (iter.hasNext()) {
            BssidBlacklistStatus status = iter.next();
            if (!status.isBlacklisted || status.reasonCode < 65536) {
                iter.remove();
            }
        }
        updateFirmwareRoamingConfiguration();
    }

    private void start() {
        if (!this.mRunning) {
            retrieveWifiScanner();
            this.mConnectivityHelper.getFirmwareRoamingInfo();
            clearBssidBlacklist();
            this.mRunning = true;
        }
    }

    private void stop() {
        if (this.mRunning) {
            this.mRunning = false;
            stopConnectivityScan();
            clearBssidBlacklist();
            resetLastPeriodicSingleScanTimeStamp();
            this.mWifiNotificationController.clearPendingNotification(true);
            this.mLastConnectionAttemptBssid = null;
            this.mWaitForFullBandScanResults = false;
        }
    }

    private void updateRunningState() {
        if (!this.mWifiEnabled || !this.mWifiConnectivityManagerEnabled) {
            localLog("Stopping WifiConnectivityManager");
            stop();
            return;
        }
        localLog("Starting up WifiConnectivityManager");
        start();
    }

    public void setWifiEnabled(boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set WiFi ");
        sb.append(enable ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mOneShotScan = !this.mWifiEnabled && enable && !this.mScreenOn;
        clearConnectionAttemptTimeStamps();
        this.mWifiEnabled = enable;
        updateRunningState();
    }

    public void enable(boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set WiFiConnectivityManager ");
        sb.append(enable ? "enabled" : "disabled");
        localLog(sb.toString());
        this.mWifiConnectivityManagerEnabled = enable;
        updateRunningState();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public int getLowRssiNetworkRetryDelay() {
        return this.mPnoScanListener.getLowRssiNetworkRetryDelay();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public long getLastPeriodicSingleScanTimeStamp() {
        return this.mLastPeriodicSingleScanTimeStamp;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiConnectivityManager");
        pw.println("WifiConnectivityManager - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiConnectivityManager - Log End ----");
        this.mWifiNotificationController.dump(fd, pw, args);
        this.mCarrierNetworkConfig.dump(fd, pw, args);
        pw.println("mBluetoothConnected " + this.mBluetoothConnected);
    }

    private int getMaxPeriodicScanInterval() {
        if (!this.mWifiGeofenceManager.isSupported() || this.mWifiGeofenceManager.isInRange()) {
            return MAX_PERIODIC_SCAN_INTERVAL_MS;
        }
        return MAX_PERIODIC_SCAN_INTERVAL_MS_FOR_GEOFENCE;
    }

    private void setFirstMaxPeriodicSingleScanTimeStamp() {
        this.mFirstMaxPeriodicScanTimeStamp = this.mClock.getElapsedSinceBootMillis();
    }

    private void resetFirstMaxPeriodicSingleScanTimeStamp() {
        this.mFirstMaxPeriodicScanTimeStamp = RESET_TIME_STAMP;
    }

    private boolean shouldMaxPeriodicScanOnSleep() {
        if (SemSarManager.isRfTestMode() || this.mStateMachine.isImsCallEstablished()) {
            return true;
        }
        return false;
    }

    public void changeMaxPeriodicScanMode(int mode) {
        if (!this.mScreenOn && this.mWifiState == 2 && this.mConfigManager.getSavedNetworks(1010).size() > 0) {
            localLog("change MaxPeriodScan mode = " + mode);
            stopConnectivityScan();
            if (mode != 0) {
                if (mode == 1) {
                    setFirstMaxPeriodicSingleScanTimeStamp();
                    scheduleMaxPeriodicScanTimer();
                }
            } else if (!shouldMaxPeriodicScanOnSleep()) {
                scheduleMaxSleepPeriodicScanTimer();
                scheduleMaxSleepPeriodicScanTimerAux();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int getPeriodicSingleScanInterval() {
        return this.mPeriodicSingleScanInterval;
    }

    /* access modifiers changed from: package-private */
    public void setBluetoothConnected(boolean connected) {
        this.mBluetoothConnected = connected;
    }
}
