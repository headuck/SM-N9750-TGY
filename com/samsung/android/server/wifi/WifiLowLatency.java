package com.samsung.android.server.wifi;

import android.app.ActivityManager;
import android.app.usage.IUsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import com.android.server.wifi.util.ScanResultUtil;
import com.samsung.android.app.usage.IUsageStatsWatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class WifiLowLatency extends Handler {
    private static final int DUMPSYS_ENTRY_COUNT_LIMIT = 200;
    private static final String DUMP_ARG = "WifiLowLatency history:";
    private static final int FORCE_MODE_DEFAULT = 0;
    private static final int FORCE_MODE_DISABLE = 2;
    private static final int FORCE_MODE_ENABLE = 1;
    private static final int FORCE_MODE_QUERY = 3;
    private static final String LATENCYMODE_ENABLE = "Enable";
    private static final String LATENCYMODE_INTENT = "com.samsung.android.wifi.LATENCYMODE";
    private static final String LATENCYMODE_MAXWINDOW = "MaxWindow";
    private static final int MAX_BLE_WINDOW = 30;
    private static final int MODE_LIMIT_BLE_SCAN = 1;
    private static final int MODE_LIMIT_BLE_WIFI_SCAN = 2;
    private static final int MODE_LIMIT_NONE = 0;
    private static final int PKTS_LOG_INTERVAL = 3;
    private static final int RX_PKTS_CONT_MASK = 63;
    private static final int RX_PKTS_CONT_UNMASK = 15;
    private static final int RX_PKTS_THRESHOLD = 15;
    private static final int RX_PKTS_UP_THRESHOLD = 999;
    private static final int STATE_GAME_RUNNING = 1;
    private static final int STATE_IMS_CALLING = 16;
    private static final int STATE_NONE = 0;
    private static final int STATE_TRAFFIC_DETECTED = 4;
    private static final int STATE_VOIP_CALLING = 2;
    private static final int STATE_WHITELISTAPP_RUNNING = 8;
    private static final String TAG = "WifiLowLatency";
    private static final boolean isFeatureEnabled = false;
    /* access modifiers changed from: private */
    public static boolean mEanbledTrafficPoll = false;
    private static int mForceMode = 0;
    private static int mLowLatencyMode;
    private static int mLowLatencyState;
    private static boolean mReady = false;
    /* access modifiers changed from: private */
    public static int mTrafficPollCnt = 0;
    /* access modifiers changed from: private */
    public static int mTrafficPollToken = 0;
    /* access modifiers changed from: private */
    public static boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public ConnectedApInfo mApInfo;
    private final ClientModeImpl mClientModeImpl;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public LlHandler mLlHandler;
    private LinkedList<String> mLowLatencyHistory = new LinkedList<>();
    /* access modifiers changed from: private */
    public NetworkInfo mNetworkInfo;
    private PackageManager mPackageManager;
    private long mRxPackets;
    /* access modifiers changed from: private */
    public boolean mScreenOn = true;
    private int mTrafficCondition = 0;
    /* access modifiers changed from: private */
    public boolean mTrafficMode = false;
    private long mTxPackets;
    private long mUidRxPackets;
    private long mUidTxPackets;
    private IUsageStatsManager mUsageStatsManager;
    /* access modifiers changed from: private */
    public String mUsageStatsPackageName;
    /* access modifiers changed from: private */
    public int mUsageStatsUid;
    private final IUsageStatsWatcher.Stub mUsageStatsWatcher = new IUsageStatsWatcher.Stub() {
        public void noteResumeComponent(ComponentName resumeComponentName, Intent intent) {
            if (resumeComponentName == null) {
                try {
                    if (WifiLowLatency.mVerboseLoggingEnabled) {
                        Log.d(WifiLowLatency.TAG, "resumeComponentName is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String packageName = resumeComponentName.getPackageName();
                if (WifiLowLatency.mVerboseLoggingEnabled) {
                    Log.d(WifiLowLatency.TAG, "resume: " + packageName);
                }
                ActivityManager.StackInfo focusedStack = ActivityManager.getService().getFocusedStackInfo();
                if (focusedStack != null) {
                    String packageName2 = focusedStack.topActivity.getPackageName();
                    if (!WifiLowLatency.this.mUsageStatsPackageName.equals(packageName2)) {
                        int unused = WifiLowLatency.this.mUsageStatsUid = WifiLowLatency.this.getPackageManager().getApplicationInfo(packageName2, 128).uid;
                        String unused2 = WifiLowLatency.this.mUsageStatsPackageName = packageName2;
                        if (!WifiTransportLayerUtils.isLauchablePackage(WifiLowLatency.this.mContext, packageName2)) {
                            if (WifiLowLatency.mVerboseLoggingEnabled) {
                                Log.d(WifiLowLatency.TAG, "Not Launchable");
                            }
                            int unused3 = WifiLowLatency.this.mUsageStatsUid = 0;
                        }
                        if (WifiLowLatency.this.mWhitelistApp.containsKey(WifiLowLatency.this.mUsageStatsPackageName)) {
                            WifiLowLatency.this.mWhitelistApp.replace(WifiLowLatency.this.mUsageStatsPackageName, true);
                            if (WifiLowLatency.this.mNetworkInfo != null && !WifiLowLatency.this.getStateWhitelistAppRunning() && WifiLowLatency.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                                Message.obtain(WifiLowLatency.this.mLlHandler, 6, 1, 0).sendToTarget();
                            }
                        }
                        WifiLowLatency.this.evaluateTrafficPolling();
                    }
                }
            }
        }

        public void notePauseComponent(ComponentName pauseComponentName) {
            if (pauseComponentName == null) {
                try {
                    if (WifiLowLatency.mVerboseLoggingEnabled) {
                        Log.d(WifiLowLatency.TAG, "pauseComponentName is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String packageName = pauseComponentName.getPackageName();
                if (WifiLowLatency.mVerboseLoggingEnabled) {
                    Log.d(WifiLowLatency.TAG, "pause: " + packageName);
                }
                if (WifiLowLatency.this.mWhitelistApp.containsKey(packageName)) {
                    WifiLowLatency.this.mWhitelistApp.replace(packageName, false);
                    if (!WifiLowLatency.this.mWhitelistApp.containsValue(true) && WifiLowLatency.this.getStateWhitelistAppRunning()) {
                        Message.obtain(WifiLowLatency.this.mLlHandler, 6, 0, 0).sendToTarget();
                    }
                }
            }
        }

        public void noteStopComponent(ComponentName arg0) throws RemoteException {
        }
    };
    /* access modifiers changed from: private */
    public HashMap<String, Boolean> mWhitelistApp;
    private final WifiNative mWifiNative;

    static /* synthetic */ int access$308() {
        int i = mTrafficPollToken;
        mTrafficPollToken = i + 1;
        return i;
    }

    static /* synthetic */ int access$408() {
        int i = mTrafficPollCnt;
        mTrafficPollCnt = i + 1;
        return i;
    }

    public WifiLowLatency(Context context, WifiNative wifiNative) {
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mClientModeImpl = WifiInjector.getInstance().getClientModeImpl();
        IntentFilter bootFilter = new IntentFilter();
        bootFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                    WifiLowLatency.this.initialize();
                }
            }
        }, bootFilter);
        this.mWhitelistApp = new HashMap<>();
        this.mWhitelistApp.put("com.microsoft.xcloud.cta", false);
        this.mWhitelistApp.put("com.microsoft.xcloud", false);
    }

    private class LlHandler extends Handler {
        private static final int MSG_LOWLATENCY_FORCEMODE = 7;
        private static final int MSG_LOWLATENCY_GAME_RUNNING = 3;
        private static final int MSG_LOWLATENCY_IMS_CALLING = 8;
        private static final int MSG_LOWLATENCY_TP_DETECTED = 5;
        private static final int MSG_LOWLATENCY_VOIP_CALLING = 4;
        private static final int MSG_LOWLATENCY_WHITELISTAPP_RUNNING = 6;
        private static final int MSG_TRAFFIC_POLL = 2;
        private static final int MSG_TRAFFIC_POLL_ENABLE = 1;
        private static final int POLL_TRAFFIC_INTERVAL_MSEC = 1000;
        private final String TAG = "WifiLowLatency.LlHandler";

        public LlHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d("WifiLowLatency.LlHandler", "MSG_TRAFFIC_POLL_ENABLE " + msg.arg1);
                    boolean unused = WifiLowLatency.mEanbledTrafficPoll = msg.arg1 == 1;
                    WifiLowLatency.access$308();
                    int unused2 = WifiLowLatency.mTrafficPollCnt = 0;
                    if (WifiLowLatency.mEanbledTrafficPoll) {
                        WifiLowLatency.this.checkTraffic();
                        sendMessageDelayed(Message.obtain(this, 2, WifiLowLatency.mTrafficPollToken, 0), 1000);
                        return;
                    } else if (WifiLowLatency.this.mTrafficMode) {
                        sendMessage(Message.obtain(this, 5, 0, 0));
                        boolean unused3 = WifiLowLatency.this.mTrafficMode = false;
                        return;
                    } else {
                        return;
                    }
                case 2:
                    if (msg.arg1 == WifiLowLatency.mTrafficPollToken) {
                        WifiLowLatency.this.checkTraffic();
                        sendMessageDelayed(Message.obtain(this, 2, WifiLowLatency.mTrafficPollToken, 0), 1000);
                        WifiLowLatency.access$408();
                        return;
                    }
                    return;
                case 3:
                    WifiLowLatency.this.setStateLowLatency(1, msg.arg1);
                    return;
                case 4:
                    WifiLowLatency.this.setStateLowLatency(2, msg.arg1);
                    return;
                case 5:
                    WifiLowLatency.this.setStateLowLatency(4, msg.arg1);
                    return;
                case 6:
                    WifiLowLatency.this.setStateLowLatency(8, msg.arg1);
                    return;
                case 7:
                    WifiLowLatency.this.setStateLowLatency(0, 0);
                    return;
                case 8:
                    WifiLowLatency.this.setStateLowLatency(16, msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    private class ConnectedApInfo {
        public LinkedList<String> bssid;
        public boolean isEnterpriseNetwork;
        public int nBssid2G;
        public int nBssid5G;
        public int nRcl2G;
        public int nRcl5G;
        public String ssid;

        private ConnectedApInfo() {
        }

        /* access modifiers changed from: private */
        public void reset() {
            this.ssid = null;
            this.nBssid2G = 0;
            this.nBssid5G = 0;
            this.nRcl2G = 0;
            this.nRcl5G = 0;
            this.isEnterpriseNetwork = false;
            LinkedList<String> linkedList = this.bssid;
            if (linkedList != null) {
                linkedList.clear();
            }
        }
    }

    /* access modifiers changed from: private */
    public void initialize() {
        if (!mReady) {
            Log.d(TAG, "initialize");
            this.mApInfo = new ConnectedApInfo();
            this.mApInfo.reset();
            this.mApInfo.bssid = new LinkedList<>();
            HandlerThread lowLatencyTheread = new HandlerThread("WifiLowLatencyThread");
            lowLatencyTheread.start();
            this.mLlHandler = new LlHandler(lowLatencyTheread.getLooper());
            regAudioPlaybackCallback();
            try {
                this.mUsageStatsUid = -1;
                this.mUsageStatsPackageName = "default";
                this.mUsageStatsManager = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
                this.mUsageStatsManager.registerUsageStatsWatcher(this.mUsageStatsWatcher);
            } catch (Exception e) {
                Log.w(TAG, "Exception occured while register UsageStatWatcher " + e);
                e.printStackTrace();
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.STATE_CHANGE");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                        NetworkInfo unused = WifiLowLatency.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (WifiLowLatency.this.mNetworkInfo != null) {
                            if (WifiLowLatency.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                                WifiInfo mWifiInfo = ((WifiManager) WifiLowLatency.this.mContext.getSystemService("wifi")).getConnectionInfo();
                                WifiLowLatency.this.mApInfo.ssid = mWifiInfo.getSSID();
                                if (WifiLowLatency.this.mApInfo.bssid == null) {
                                    WifiLowLatency.this.mApInfo.bssid = new LinkedList<>();
                                }
                                WifiLowLatency.this.checkEnterpriseNetworkFromRcl();
                                WifiLowLatency.this.checkEnterpriseNetworkFromScanResults();
                            } else if (WifiLowLatency.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                                WifiLowLatency.this.mApInfo.reset();
                            }
                        }
                    } else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                        boolean unused2 = WifiLowLatency.this.mScreenOn = false;
                    } else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                        boolean unused3 = WifiLowLatency.this.mScreenOn = true;
                    }
                    WifiLowLatency.this.evaluateTrafficPolling();
                }
            }, filter);
            mReady = true;
        }
    }

    /* access modifiers changed from: private */
    public void checkTraffic() {
        long prevUidRxPackets = this.mUidRxPackets;
        this.mUidRxPackets = TrafficStats.getUidRxPackets(this.mUsageStatsUid);
        if (mTrafficPollCnt == 3) {
            mTrafficPollCnt = 0;
        }
        long received = this.mUidRxPackets - prevUidRxPackets;
        this.mTrafficCondition <<= 1;
        if (received > 15 && received < 999) {
            this.mTrafficCondition |= 1;
        }
        if (!this.mTrafficMode) {
            if ((this.mTrafficCondition & 63) == 63) {
                this.mTrafficMode = true;
                Message.obtain(this.mLlHandler, 5, 1, 0).sendToTarget();
            }
        } else if ((this.mTrafficCondition & 15) == 0) {
            this.mTrafficMode = false;
            Message.obtain(this.mLlHandler, 5, 0, 0).sendToTarget();
        }
    }

    /* access modifiers changed from: private */
    public void evaluateTrafficPolling() {
        Message msg;
        NetworkInfo networkInfo = this.mNetworkInfo;
        if (networkInfo != null) {
            if (networkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED || !this.mScreenOn || this.mUsageStatsUid <= 1000) {
                if (getStateVoipCalling() && this.mNetworkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
                    Message.obtain(this.mLlHandler, 4, 0, 0).sendToTarget();
                }
                if (getStateImsCalling() && this.mNetworkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
                    Message.obtain(this.mLlHandler, 8, 0, 0).sendToTarget();
                }
                msg = Message.obtain(this.mLlHandler, 1, 0, 0);
            } else {
                msg = Message.obtain(this.mLlHandler, 1, 1, 0);
            }
            msg.sendToTarget();
            if ((this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && mForceMode == 1) || mForceMode == 2) {
                Message.obtain(this.mLlHandler, 7, 0, 0).sendToTarget();
            }
        }
    }

    private void regAudioPlaybackCallback() {
        Log.d(TAG, "registerAudioPlaybackCallback");
        final AudioManager am = (AudioManager) this.mContext.getSystemService("audio");
        try {
            am.registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    boolean flagVoiceComm = false;
                    if (configs != null) {
                        int audioMode = am.getMode();
                        Iterator<AudioPlaybackConfiguration> it = configs.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            } else if (it.next().getAudioAttributes().getUsage() == 2) {
                                if (WifiLowLatency.mVerboseLoggingEnabled) {
                                    Log.d(WifiLowLatency.TAG, "VOICE_COMMUNICATION audioMode " + audioMode);
                                }
                                if (audioMode == 3) {
                                    flagVoiceComm = true;
                                    break;
                                }
                            }
                        }
                        if (flagVoiceComm) {
                            if (WifiLowLatency.this.mNetworkInfo != null && !WifiLowLatency.this.getStateVoipCalling() && WifiLowLatency.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                                Message.obtain(WifiLowLatency.this.mLlHandler, 4, 1, 0).sendToTarget();
                            }
                        } else if (WifiLowLatency.this.getStateVoipCalling()) {
                            Message.obtain(WifiLowLatency.this.mLlHandler, 4, 0, 0).sendToTarget();
                        }
                    }
                    super.onPlaybackConfigChanged(configs);
                }
            }, this);
        } catch (NullPointerException e) {
            Log.d(TAG, "catch NPE");
        }
    }

    /* access modifiers changed from: private */
    public PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    /* access modifiers changed from: private */
    public boolean getStateVoipCalling() {
        return (mLowLatencyState & 2) == 2;
    }

    private boolean getStateGameRunning() {
        return (mLowLatencyState & 1) == 1;
    }

    private boolean getStateTrafficDetected() {
        return (mLowLatencyState & 4) == 4;
    }

    /* access modifiers changed from: private */
    public boolean getStateWhitelistAppRunning() {
        return (mLowLatencyState & 8) == 8;
    }

    private boolean getStateImsCalling() {
        return (mLowLatencyState & 16) == 16;
    }

    /* access modifiers changed from: private */
    public void setStateLowLatency(int state, int enable) {
        int i;
        int prevState = mLowLatencyState;
        if (enable == 0) {
            mLowLatencyState &= ~state;
        } else {
            mLowLatencyState |= state;
        }
        Log.d(TAG, "enable:" + enable + " state:" + state + " prev:" + prevState + " new:" + mLowLatencyState + " force:" + mForceMode);
        if ((prevState != 0 && mLowLatencyState == 0 && mForceMode != 1) || (i = mForceMode) == 2) {
            mLowLatencyMode = 0;
            sendBroadcastLatencyMode(0, 0);
            Log.d(TAG, "Latency mode deactivated");
            logLatencyMode(System.currentTimeMillis(), mLowLatencyMode, mLowLatencyState);
        } else if ((prevState == 0 && mLowLatencyState != 0 && i != 2) || mForceMode == 1) {
            mLowLatencyMode = 1;
            sendBroadcastLatencyMode(1, 30);
            Log.d(TAG, "Latency mode activated");
            logLatencyMode(System.currentTimeMillis(), mLowLatencyMode, mLowLatencyState);
        }
    }

    private void sendBroadcastLatencyMode(int enable, int maxWin) {
        Intent intentLatencyMode = new Intent(LATENCYMODE_INTENT);
        intentLatencyMode.putExtra(LATENCYMODE_ENABLE, enable);
        intentLatencyMode.putExtra(LATENCYMODE_MAXWINDOW, maxWin);
        this.mContext.sendBroadcastAsUser(intentLatencyMode, UserHandle.ALL, "android.permission.ACCESS_WIFI_STATE");
    }

    public void setImsCallingState(boolean enable) {
        LlHandler llHandler;
        if (mReady && (llHandler = this.mLlHandler) != null) {
            if (enable) {
                NetworkInfo networkInfo = this.mNetworkInfo;
                if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    Message.obtain(this.mLlHandler, 8, 1, 0).sendToTarget();
                    return;
                }
                return;
            }
            Message.obtain(llHandler, 8, 0, 0).sendToTarget();
        }
    }

    public int setForceMode(int mode) {
        if (mode == 0) {
            mForceMode = 0;
            return 0;
        } else if (mode == 1) {
            mForceMode = 1;
            return 0;
        } else if (mode == 2) {
            mForceMode = 2;
            return 0;
        } else if (mode != 3) {
            return 0;
        } else {
            int modeResult = mForceMode;
            Log.d(TAG, "Forcemode Query:" + mForceMode);
            return modeResult;
        }
    }

    public boolean getLatencyMode() {
        return mLowLatencyState != 0;
    }

    public void enableVerboseLogging(int verbose) {
        mVerboseLoggingEnabled = verbose > 0;
    }

    /* access modifiers changed from: private */
    public void checkEnterpriseNetworkFromScanResults() {
        if (!this.mApInfo.isEnterpriseNetwork) {
            ArrayList<ScanDetail> mScanResults = this.mWifiNative.getScanResults("wlan0");
            for (int i = 0; i < mScanResults.size(); i++) {
                ScanResult result = mScanResults.get(i).getScanResult();
                if (("\"" + result.SSID + "\"").equals(this.mApInfo.ssid)) {
                    if (ScanResultUtil.isScanResultForEapNetwork(result)) {
                        if (mVerboseLoggingEnabled) {
                            Log.i(TAG, "Determined as enterprise network by security option");
                        }
                        this.mApInfo.isEnterpriseNetwork = true;
                        return;
                    }
                    boolean newBSSID = true;
                    int j = 0;
                    while (true) {
                        if (j >= this.mApInfo.bssid.size()) {
                            break;
                        } else if (this.mApInfo.bssid.get(j).equals(result.BSSID)) {
                            newBSSID = false;
                            break;
                        } else {
                            j++;
                        }
                    }
                    if (newBSSID) {
                        this.mApInfo.bssid.add(result.BSSID);
                        if (result.is5GHz()) {
                            this.mApInfo.nBssid5G++;
                        } else if (result.is24GHz()) {
                            this.mApInfo.nBssid2G++;
                        }
                    }
                    if (this.mApInfo.nBssid2G > 3 || this.mApInfo.nBssid5G > 3) {
                        if (mVerboseLoggingEnabled) {
                            Log.i(TAG, "Determined as enterprise network by scan results");
                        }
                        this.mApInfo.isEnterpriseNetwork = true;
                        return;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkEnterpriseNetworkFromRcl() {
        List<Integer> rcl;
        if (!this.mApInfo.isEnterpriseNetwork) {
            WifiRoamingAssistant mRoamingAssistant = WifiRoamingAssistant.getInstance();
            WifiConfiguration currConfig = this.mClientModeImpl.getCurrentWifiConfiguration();
            if (mRoamingAssistant != null && currConfig != null && (rcl = mRoamingAssistant.getNetworkFrequencyList(currConfig.configKey())) != null) {
                ConnectedApInfo connectedApInfo = this.mApInfo;
                connectedApInfo.nRcl2G = 0;
                connectedApInfo.nRcl5G = 0;
                for (int i = 0; i < rcl.size(); i++) {
                    int freq = rcl.get(i).intValue();
                    if (freq > 2400 && freq < 2500) {
                        this.mApInfo.nRcl2G++;
                    } else if (freq > 4900 && freq < 5900) {
                        this.mApInfo.nRcl5G++;
                    }
                }
                if (this.mApInfo.nRcl2G > 2 || this.mApInfo.nRcl5G > 2) {
                    this.mApInfo.isEnterpriseNetwork = true;
                    if (mVerboseLoggingEnabled) {
                        Log.i(TAG, "Determined as enterprise network by rcl");
                    }
                }
            }
        }
    }

    private void logLatencyMode(long now, int mode, int reason) {
        try {
            String timestamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date(now));
            String s = String.format(Locale.US, "%s,%d,%d", new Object[]{timestamp, Integer.valueOf(mode), Integer.valueOf(reason)});
            synchronized (this.mLowLatencyHistory) {
                this.mLowLatencyHistory.add(s);
                while (this.mLowLatencyHistory.size() > 200) {
                    this.mLowLatencyHistory.removeFirst();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "format problem", e);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        LinkedList<String> history;
        synchronized (this.mLowLatencyHistory) {
            history = new LinkedList<>(this.mLowLatencyHistory);
        }
        pw.println(DUMP_ARG);
        Iterator it = history.iterator();
        while (it.hasNext()) {
            pw.println((String) it.next());
        }
        history.clear();
        pw.println();
    }
}
