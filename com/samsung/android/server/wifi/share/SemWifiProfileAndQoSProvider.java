package com.samsung.android.server.wifi.share;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.ISharedPasswordCallback;
import com.samsung.android.server.wifi.WifiGuiderFeatureController;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsProvider;
import com.samsung.android.server.wifi.share.McfDataUtil;
import com.samsung.android.server.wifi.share.SemWifiQoSCacheManager;
import com.sec.android.app.C0852CscFeatureTagCommon;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemWifiProfileAndQoSProvider {
    private static final long MAX_PASSWORD_REQ_TIME = 3600000;
    private static final long PERIOD_QOS_DATA_UPDATE_TIME = 3000000;
    private static final String SYSTEM_DB_NEARBY_SCANNING = "nearby_scanning_enabled";
    private static final String TAG = "WifiProfileShare";
    private static final String VERSION = "1.1";
    private static final boolean mDebugProcessMessage = false;
    /* access modifiers changed from: private */
    public final Adapter mAdapter;
    /* access modifiers changed from: private */
    public final Object mCallbackLock = new Object();
    /* access modifiers changed from: private */
    public final CasterMode mCasterMode;
    /* access modifiers changed from: private */
    public final ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final WifiGuiderFeatureController mFeatureController;
    private boolean[] mFlagForUseMcfService = new boolean[2];
    private KeyguardManager mKeyguardManager;
    /* access modifiers changed from: private */
    public final McfController mMcfProvider;
    /* access modifiers changed from: private */
    public Set<IMcfServiceState> mMcfServiceCallbacks = new HashSet();
    private IMcfServiceState mMcfServiceListener = new IMcfServiceState() {
        public void onServiceConnected() {
            synchronized (SemWifiProfileAndQoSProvider.this.mCallbackLock) {
                for (IMcfServiceState callback : SemWifiProfileAndQoSProvider.this.mMcfServiceCallbacks) {
                    if (callback != null) {
                        callback.onServiceConnected();
                    }
                }
            }
        }

        public void onFailedToBindService() {
            synchronized (SemWifiProfileAndQoSProvider.this.mCallbackLock) {
                Iterator<IMcfServiceState> iterator = SemWifiProfileAndQoSProvider.this.mMcfServiceCallbacks.iterator();
                while (iterator.hasNext()) {
                    IMcfServiceState callback = iterator.next();
                    if (callback != null) {
                        callback.onFailedToBindService();
                    }
                    iterator.remove();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final McfDataUtil mMcfUtil;
    /* access modifiers changed from: private */
    public boolean mQosOnlyOpenNetwork;
    /* access modifiers changed from: private */
    public final SubscriberMode mSubscribeMode;
    /* access modifiers changed from: private */
    public int[] mTestQoSData;

    public interface Adapter {
        WifiInfo getConnectionInfo();

        int[] getCurrentNetworkScore();

        List<WifiConfiguration> getPrivilegedConfiguredNetworks();

        List<ScanResult> getScanResults();

        WifiConfiguration getSpecificNetwork(int i);

        WifiConfiguration getWifiApConfiguration();

        int getWifiApState();

        boolean isWifiEnabled();

        boolean isWpa3SaeSupported();

        void startScan();
    }

    public SemWifiProfileAndQoSProvider(Context context, Adapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mFeatureController = WifiGuiderFeatureController.getInstance(this.mContext);
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mMcfProvider = new McfController(this.mContext);
        this.mMcfUtil = new McfDataUtil();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mCasterMode = new CasterMode(thread.getLooper());
        this.mSubscribeMode = new SubscriberMode(thread.getLooper());
        if (SemCscFeature.getInstance().getBoolean(C0852CscFeatureTagCommon.TAG_CSCFEATURE_COMMON_SUPPORTCOMCASTWIFI)) {
            setDisableNetworkRecommendationIfNotSet();
        }
    }

    /* access modifiers changed from: private */
    public boolean isWifiNetworkActivated() {
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == 1;
    }

    public void checkAndStart() {
        Log.d(TAG, "checkAndStart");
        this.mCasterMode.start();
        this.mSubscribeMode.start();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(SemWifiProfileAndQoSProvider.TAG, "unlock the screen");
                SemWifiProfileAndQoSProvider.this.mCasterMode.setEnableCaster(true);
                SemWifiProfileAndQoSProvider.this.mSubscribeMode.setEnableSubscriber(true);
            }
        }, new IntentFilter("android.intent.action.USER_PRESENT"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(SemWifiProfileAndQoSProvider.TAG, "screen off");
                SemWifiProfileAndQoSProvider.this.mCasterMode.screenOff();
                SemWifiProfileAndQoSProvider.this.mCasterMode.clearUserConfirmHistory();
                SemWifiProfileAndQoSProvider.this.mSubscribeMode.setEnableSubscriber(false);
                SemWifiProfileAndQoSProvider.this.mMcfServiceCallbacks.clear();
            }
        }, new IntentFilter("android.intent.action.SCREEN_OFF"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(SemWifiProfileAndQoSProvider.TAG, "screen on");
                if (!SemWifiProfileAndQoSProvider.this.isKeyguardLocked()) {
                    SemWifiProfileAndQoSProvider.this.mCasterMode.setEnableCaster(true);
                    SemWifiProfileAndQoSProvider.this.mSubscribeMode.setEnableSubscriber(true);
                }
            }
        }, new IntentFilter("android.intent.action.SCREEN_ON"));
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sem_wifi_network_rating_scorer_enabled"), false, new ContentObserver(this.mCasterMode.getHandler()) {
            public void onChange(boolean selfChange) {
                boolean enable = SemWifiProfileAndQoSProvider.this.isEnableNetworkRecommendation();
                Log.i(SemWifiProfileAndQoSProvider.TAG, "network score policy was changed to " + enable);
                SemWifiProfileAndQoSProvider.this.mCasterMode.sendMessage(7, enable);
                SemWifiProfileAndQoSProvider.this.mSubscribeMode.sendMessage(12, enable);
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SYSTEM_DB_NEARBY_SCANNING), false, new ContentObserver(this.mCasterMode.getHandler()) {
            public void onChange(boolean selfChange) {
                boolean enable = SemWifiProfileAndQoSProvider.this.isEnabledNearByScanningSettings();
                SemWifiProfileAndQoSProvider.this.mCasterMode.sendMessage(7, enable);
                if (!enable) {
                    SemWifiProfileAndQoSProvider.this.mSubscribeMode.asyncCancelToRequestPassword();
                }
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("emergency_mode"), false, new ContentObserver(this.mCasterMode.getHandler()) {
            public void onChange(boolean selfChange) {
                boolean isEnabled = SemWifiProfileAndQoSProvider.this.getUltraPowerSaveEnabledFromProvider();
                SemWifiProfileAndQoSProvider.this.mCasterMode.setEnableCaster(isEnabled);
                SemWifiProfileAndQoSProvider.this.mSubscribeMode.setEnableSubscriber(isEnabled);
            }
        });
        this.mSubscribeMode.setEnableSubscriber(true, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
    }

    public String getVersion() {
        return VERSION;
    }

    public void setUserConfirm(boolean isAccept, String userData) {
        this.mCasterMode.asyncSetUserConfirm(isAccept, userData);
    }

    public void requestPassword(boolean enabled, String bssid, ISharedPasswordCallback callback) {
        if (enabled) {
            this.mSubscribeMode.asyncRequestPassword(bssid, callback);
        } else {
            this.mSubscribeMode.asyncCancelToRequestPassword();
        }
    }

    public void requestPasswordPost(boolean enable) {
        this.mSubscribeMode.asyncRequestPasswordPost(enable);
    }

    public Map<String, Map<String, Integer>> getQoSScores(List<String> bssids) {
        return this.mSubscribeMode.syncGetQoSScores(bssids);
    }

    public void updateQoSData(boolean changedNetworkType, boolean changedQoSData) {
        Log.i(TAG, "network QoS data was changed, updateQoSData networkType: " + changedNetworkType + ", qosData: " + changedQoSData);
        this.mCasterMode.requestToUpdateShareData(true);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiProfileShare:");
        pw.println(dump());
        pw.println(SemWifiQoSCacheManager.printCachedScores(this.mSubscribeMode.syncGetQoSScores((List<String>) null), new SemWifiQoSCacheManager.ConfigKeyProvider() {
            public final String getConfigKey(String str) {
                return SemWifiProfileAndQoSProvider.this.lambda$dump$0$SemWifiProfileAndQoSProvider(str);
            }
        }));
        this.mCasterMode.dump(fd, pw, args);
        this.mSubscribeMode.dump(fd, pw, args);
        pw.println("");
    }

    public /* synthetic */ String lambda$dump$0$SemWifiProfileAndQoSProvider(String bssid) {
        List<String> configKeys = this.mMcfUtil.getConfigKeys(bssid);
        if (configKeys.size() > 0) {
            return configKeys.get(0);
        }
        return "unknown";
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Version: 1.1");
        sb.append("\n - QoS Policy: ");
        sb.append(this.mQosOnlyOpenNetwork ? "open network only" : "both open and secured network");
        sb.append("\nCaster Info:");
        sb.append("\n - Current State: ");
        sb.append(this.mCasterMode.getCurrentState().getName());
        int[] scores = this.mAdapter.getCurrentNetworkScore();
        if (scores != null) {
            sb.append("\n - Network Scores: ");
            for (int value : scores) {
                sb.append(value);
                sb.append(" ");
            }
        }
        sb.append("\n - Sharing QoS Data: ");
        sb.append(this.mCasterMode.getLastSharingQoSInfo());
        if (this.mCasterMode.mLastSharedStaConfigKey != null) {
            sb.append("\n - Sharing STA Password for ");
            sb.append(this.mCasterMode.mLastSharedStaConfigKey);
        }
        if (this.mCasterMode.mMobileWipsDetectedBssids != null) {
            sb.append("\n - Mobile WIPS detection bssids:");
            for (String bssid : this.mCasterMode.mMobileWipsDetectedBssids) {
                sb.append(" ");
                sb.append(bssid);
            }
        }
        if (this.mCasterMode.mLastSharedApConfigKey != null) {
            sb.append("\n - Sharing AP Password for ");
            sb.append(this.mCasterMode.mLastSharedApConfigKey);
        }
        sb.append("\nSubscriber Info:");
        sb.append("\n - Current State: ");
        sb.append(this.mSubscribeMode.getCurrentState().getName());
        if (this.mTestQoSData != null) {
            sb.append("\nTestQoSData: ");
            sb.append("[");
            for (int mTestQoSDatum : this.mTestQoSData) {
                sb.append(mTestQoSDatum);
                sb.append(" ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public void test(Bundle testSettings) {
        boolean[] policy;
        if (testSettings != null) {
            if (testSettings.getBoolean("clearScores")) {
                this.mSubscribeMode.asyncClearCachedNetworkScores();
            }
            if (testSettings.getBoolean("clearConfirmHistory")) {
                this.mCasterMode.clearUserConfirmHistory();
            }
            if (testSettings.getBoolean("resetTestScores")) {
                this.mTestQoSData = null;
                this.mCasterMode.requestToUpdateShareData(true);
            } else {
                int[] testNetworkScores = testSettings.getIntArray("networkScores");
                if (testNetworkScores != null && testNetworkScores.length == 4) {
                    this.mTestQoSData = testNetworkScores;
                    this.mCasterMode.requestToUpdateShareData(true);
                }
            }
            if (testSettings.getBoolean("updateScanPolicy") && (policy = testSettings.getBooleanArray("scanPolicies")) != null && policy.length == 3) {
                this.mMcfProvider.setScanMode(policy[0], policy[1], policy[2]);
            }
            if (testSettings.containsKey("qosForOpenNetwork")) {
                this.mQosOnlyOpenNetwork = testSettings.getBoolean("qosForOpenNetwork", false);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean getUltraPowerSaveEnabledFromProvider() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "emergency_mode", 0) == 1;
    }

    private void setDisableNetworkRecommendationIfNotSet() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "sem_wifi_network_rating_scorer_enabled", -1) == -1) {
            Log.i(TAG, "force disable network score provider setting");
            Settings.Global.putInt(this.mContext.getContentResolver(), "sem_wifi_network_rating_scorer_enabled", 0);
        }
    }

    /* access modifiers changed from: private */
    public boolean isEnableNetworkRecommendation() {
        int enableInt = Settings.Global.getInt(this.mContext.getContentResolver(), "sem_wifi_network_rating_scorer_enabled", 1);
        StringBuilder sb = new StringBuilder();
        sb.append("network score provider settings ");
        sb.append(enableInt == 1 ? "enabled" : "disabled");
        Log.d(TAG, sb.toString());
        if (enableInt == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isEnabledNearByScanningSettings() {
        int enableInt = Settings.System.getInt(this.mContext.getContentResolver(), SYSTEM_DB_NEARBY_SCANNING, 0);
        StringBuilder sb = new StringBuilder();
        sb.append("nearby scanning settings ");
        sb.append(enableInt == 1 ? "run" : enableInt == 2 ? "pause" : "stop");
        Log.d(TAG, sb.toString());
        if (enableInt == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isKeyguardLocked() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        }
        return this.mKeyguardManager.isKeyguardLocked();
    }

    /* access modifiers changed from: private */
    public void setMcfServiceForCaster(boolean enable) {
        this.mFlagForUseMcfService[0] = enable;
    }

    /* access modifiers changed from: private */
    public void setMcfServiceForSubscriber(boolean enable) {
        this.mFlagForUseMcfService[1] = enable;
    }

    /* access modifiers changed from: private */
    public void checkAndBindMcfService(IMcfServiceState callback) {
        synchronized (this.mCallbackLock) {
            int size = this.mMcfServiceCallbacks.size();
            if (this.mMcfProvider.isServiceBound()) {
                Log.d(TAG, "already bound mcf service, callback size: " + size);
                this.mMcfServiceCallbacks.add(callback);
                callback.onServiceConnected();
            } else if (size > 0) {
                Log.d(TAG, "already tried to bind mcf service, callback size: " + size);
                this.mMcfServiceCallbacks.add(callback);
            } else {
                Log.d(TAG, "try to bind mcf service");
                if (this.mMcfProvider.bindMcfService(this.mMcfServiceListener)) {
                    this.mMcfServiceCallbacks.add(callback);
                } else {
                    Log.e(TAG, "failed to bind service");
                    callback.onFailedToBindService();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkAndUnbindMcfService(IMcfServiceState callback) {
        this.mMcfServiceCallbacks.remove(callback);
        boolean[] zArr = this.mFlagForUseMcfService;
        if (zArr[0] || zArr[1]) {
            StringBuilder sb = new StringBuilder();
            sb.append("mcf service is used for ");
            String str = "";
            sb.append(this.mFlagForUseMcfService[0] ? "caster " : str);
            if (this.mFlagForUseMcfService[1]) {
                str = "subscriber ";
            }
            sb.append(str);
            Log.d(TAG, sb.toString());
            return;
        }
        this.mMcfServiceCallbacks.clear();
        this.mMcfProvider.unbindMcfService();
        Log.d(TAG, "unbind mcf service");
    }

    private class CasterMode extends StateMachine implements IMcfServiceState {
        static final int CMD_CASTER_SHARE_DATA_CHANGED = 7;
        static final int CMD_CHECK_AND_START_SHARE = 1;
        static final int CMD_CLEAR_HISTORY = 11;
        static final int CMD_DISMISS_DIALOG = 5;
        static final int CMD_SCREEN_OFF = 13;
        static final int CMD_SHOW_PWD_CONFIRM_DIALOG = 3;
        static final int CMD_STOP_PASSWORD_SHARE = 12;
        static final int CMD_STOP_SHARE = 2;
        static final int CMD_UNBIND_MCF_SERVICE = 10;
        static final int CMD_UPDATE_PWD_BSSID_LIST = 9;
        static final int CMD_UPDATE_QOS_DATA_POLL = 8;
        static final int CMD_USER_CONFIRM = 4;
        static final int EVENT_MCF_SERVICE_CONNECTED = 20;
        static final int EVENT_PASSWORD_REQUESTED = 21;
        static final int EVENT_PASSWORD_SESSION_CLOSED = 22;
        private static final String TAG = "WifiProfileShare.Caster";
        private static final String URI_CONTENT_MOBILE_WIPS = "content://com.samsung.server.wifi.mobilewips.client/detection";
        /* access modifiers changed from: private */
        public final State mActiveState = new ActiveState();
        /* access modifiers changed from: private */
        public ICasterCallback mCallback = new ICasterCallback() {
            public void onPasswordRequested(McfDataUtil.McfData data, String userData) {
                Log.v(CasterMode.TAG, "onPasswordRequested sta: " + CasterMode.this.mLastSharedStaConfigKey + " request: " + data.getConfigKey());
                if (CasterMode.this.mLastSharedStaConfigKey != null && CasterMode.this.mLastSharedStaConfigKey.equals(data.getConfigKey())) {
                    CasterMode.this.sendMessage(21, 0, 0, userData);
                } else if (CasterMode.this.mLastSharedApConfigKey == null || !CasterMode.this.mLastSharedApConfigKey.equals(data.getConfigKey())) {
                    Log.e(CasterMode.TAG, "onPasswordRequested - not exit config for " + data.getConfigKey());
                } else {
                    CasterMode.this.sendMessage(21, 1, 0, userData);
                }
            }

            public void onSessionClosed(String userData) {
                CasterMode.this.sendMessage(22, userData);
            }
        };
        /* access modifiers changed from: private */
        public final State mDefaultState = new DefaultState();
        private boolean mEnablePasswordShare = true;
        final SparseArray<String> mGetWhatToString = MessageUtils.findMessageNames(new Class[]{CasterMode.class});
        /* access modifiers changed from: private */
        public String mLastSharedApConfigKey;
        /* access modifiers changed from: private */
        public String mLastSharedStaConfigKey;
        private McfDataUtil.McfData mLastUpdatedQoSData;
        /* access modifiers changed from: private */
        public Set<String> mMobileWipsDetectedBssids;
        /* access modifiers changed from: private */
        public Map<String, Boolean[]> mNotifiedUserData = new HashMap();
        private int mRetryCount;
        /* access modifiers changed from: private */
        public Pair<McfDataUtil.McfData, List<McfDataUtil.McfData>> mShareData;
        /* access modifiers changed from: private */
        public boolean mWifiNetworkConnected;

        CasterMode(Looper looper) {
            super(TAG, looper);
            addState(this.mDefaultState);
            addState(this.mActiveState, this.mDefaultState);
            setInitialState(this.mDefaultState);
            setLogRecSize(32);
            setLogOnlyTransitions(false);
        }

        public void start() {
            SemWifiProfileAndQoSProvider.super.start();
            SemWifiProfileAndQoSProvider.this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        if (CasterMode.this.mWifiNetworkConnected) {
                            Log.d(CasterMode.TAG, "Wi-Fi network state changed - disconnected");
                            CasterMode.this.requestToUpdateShareData(false);
                        }
                        boolean unused = CasterMode.this.mWifiNetworkConnected = false;
                        return;
                    }
                    boolean unused2 = CasterMode.this.mWifiNetworkConnected = true;
                    CasterMode.this.requestToUpdateShareData(true);
                    Log.d(CasterMode.TAG, "Wi-Fi network state changed - connected");
                }
            }, new IntentFilter("android.net.wifi.STATE_CHANGE"));
            SemWifiProfileAndQoSProvider.this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    int apState = intent.getIntExtra("wifi_state", 11);
                    if (apState == 13) {
                        CasterMode.this.requestToUpdateShareData(true);
                        Log.d(CasterMode.TAG, "mobile hotspot activated");
                    } else if (apState == 11) {
                        CasterMode.this.requestToUpdateShareData(false);
                        Log.d(CasterMode.TAG, "mobile hotspot deactivated");
                    }
                }
            }, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
            SemWifiProfileAndQoSProvider.this.mContext.getContentResolver().registerContentObserver(Uri.parse(URI_CONTENT_MOBILE_WIPS), true, new ContentObserver(getHandler()) {
                public void onChange(boolean selfChange) {
                    if (CasterMode.this.updateMwipsBssids()) {
                        Log.v(CasterMode.TAG, "mwips block bssid list is changed");
                        CasterMode.this.requestToUpdateShareData(true);
                    }
                }
            });
        }

        /* access modifiers changed from: package-private */
        public void setEnableCaster(boolean enable) {
            removeMessages(1);
            if (enable) {
                sendMessageDelayed(1, 1000);
            } else {
                sendMessage(2);
            }
        }

        /* access modifiers changed from: package-private */
        public void screenOff() {
            removeMessages(13);
            sendMessage(13);
        }

        /* access modifiers changed from: package-private */
        public void asyncSetUserConfirm(boolean isAccept, String userData) {
            StringBuilder sb = new StringBuilder();
            sb.append("user response to ");
            sb.append(isAccept ? "accept" : "reject");
            sb.append(" share for ");
            sb.append(userData);
            Log.i(TAG, sb.toString());
            sendMessage(4, isAccept, 0, userData);
        }

        /* access modifiers changed from: private */
        public boolean updateMwipsBssids() {
            if (this.mMobileWipsDetectedBssids == null) {
                this.mMobileWipsDetectedBssids = new HashSet();
            }
            Cursor cursor = null;
            boolean flagUpdate = false;
            try {
                Log.v(TAG, "update mobile wips bssids");
                Cursor cursor2 = SemWifiProfileAndQoSProvider.this.mContext.getContentResolver().query(Uri.parse(URI_CONTENT_MOBILE_WIPS), new String[]{MobileWipsProvider.MAC_ADDR, MobileWipsProvider.SSID_NAME, MobileWipsProvider.TIME_STAMP, MobileWipsProvider.SEEN_TIME, MobileWipsProvider.ATTACK_TYPE}, (String) null, (String[]) null, (String) null);
                if (cursor2 != null) {
                    cursor2.moveToFirst();
                    while (!cursor2.isAfterLast()) {
                        String bssid = cursor2.getString(0);
                        Log.v(TAG, " - add " + bssid + " " + cursor2.getString(1) + " attackType:" + cursor2.getString(4));
                        if (!this.mMobileWipsDetectedBssids.contains(bssid)) {
                            this.mMobileWipsDetectedBssids.add(bssid);
                            flagUpdate = true;
                        }
                        cursor2.moveToNext();
                    }
                } else {
                    Log.v(TAG, " - empty, cursor is null");
                }
                if (cursor2 != null) {
                    try {
                        cursor2.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "error to close cursor");
                    }
                }
            } catch (SQLException e2) {
                Log.e(TAG, "error to get wips list");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (SQLException e3) {
                        Log.e(TAG, "error to close cursor");
                    }
                }
                throw th;
            }
            return flagUpdate;
        }

        private String getPassword(int networkId) {
            List<WifiConfiguration> configs = SemWifiProfileAndQoSProvider.this.mAdapter.getPrivilegedConfiguredNetworks();
            if (configs == null) {
                return null;
            }
            for (WifiConfiguration config : configs) {
                if (config.networkId == networkId) {
                    if (!config.allowedKeyManagement.get(0) || config.wepKeys == null || config.wepKeys[0] == null) {
                        return config.preSharedKey;
                    }
                    return config.wepKeys[0];
                }
            }
            return null;
        }

        private Pair<String, WifiConfiguration> getCurrentConfig() {
            int networkId;
            WifiInfo info = SemWifiProfileAndQoSProvider.this.mAdapter.getConnectionInfo();
            if (info == null || (networkId = info.getNetworkId()) == -1) {
                return null;
            }
            return Pair.create(info.getBSSID(), SemWifiProfileAndQoSProvider.this.mAdapter.getSpecificNetwork(networkId));
        }

        private boolean isOpenAp(WifiConfiguration config) {
            if (!config.allowedKeyManagement.get(9)) {
                if (!config.allowedKeyManagement.get(0)) {
                    return false;
                }
                if (config.wepKeys == null || config.wepKeys[0] == null) {
                    return true;
                }
                return false;
            }
            return true;
        }

        private boolean isSecuredAp(WifiConfiguration config) {
            if (config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(4) || config.allowedKeyManagement.get(6) || config.allowedKeyManagement.get(22) || config.allowedKeyManagement.get(8)) {
                return true;
            }
            return (!config.allowedKeyManagement.get(0) || config.wepKeys == null || config.wepKeys[0] == null) ? false : true;
        }

        private boolean isAvailableToShareQoS(WifiConfiguration config) {
            return config != null && (isOpenAp(config) || (!SemWifiProfileAndQoSProvider.this.mQosOnlyOpenNetwork && isSecuredAp(config))) && !config.isPasspoint();
        }

        private boolean isAvailableToShareConfig(WifiConfiguration config) {
            return config != null && isSecuredAp(config) && !config.semIsVendorSpecificSsid && !config.semSamsungSpecificFlags.get(4) && !config.isEphemeral() && !config.isPasspoint();
        }

        /* access modifiers changed from: private */
        public void stopShare() {
            Log.d(TAG, "stop caster");
            SemWifiProfileAndQoSProvider.this.mMcfProvider.stopAllCasterMode();
            this.mLastUpdatedQoSData = null;
        }

        /* access modifiers changed from: private */
        public void requestToUpdateShareData(boolean enable) {
            sendMessage(7, enable);
        }

        /* access modifiers changed from: private */
        public void setPasswordShare(boolean enable) {
            this.mEnablePasswordShare = enable;
            if (enable) {
                requestToUpdateShareData(true);
            } else {
                sendMessage(12);
            }
        }

        /* access modifiers changed from: private */
        public void updateQoSData(McfDataUtil.McfData newQoSInfo) {
            Log.i(TAG, "qos data was updated , before:" + this.mLastUpdatedQoSData + " after:" + newQoSInfo);
            this.mLastUpdatedQoSData = newQoSInfo;
            SemWifiProfileAndQoSProvider.this.mMcfProvider.checkAndUpdateQoSData(newQoSInfo);
        }

        /* access modifiers changed from: private */
        public void updateQoSDataPoll(boolean forceUpdate) {
            McfDataUtil.McfData newQoSInfo;
            Pair<String, WifiConfiguration> network = getCurrentConfig();
            if (network == null) {
                Log.e(TAG, "failed to start caster, network was disconnected");
                updateQoSData((McfDataUtil.McfData) null);
            } else if (!isAvailableToShareQoS((WifiConfiguration) network.second) || (newQoSInfo = getSharedQosInfo((String) network.first, (WifiConfiguration) network.second)) == null) {
            } else {
                if (forceUpdate || !newQoSInfo.equals(this.mLastUpdatedQoSData)) {
                    updateQoSData(newQoSInfo);
                }
            }
        }

        private SemWifiQoSCacheManager.NetworkType getNetworkType(WifiConfiguration config, int prevNetworkTypeInt) {
            Network network;
            if (SemWifiProfileAndQoSProvider.this.mTestQoSData != null) {
                int nt = SemWifiProfileAndQoSProvider.this.mTestQoSData[0];
                SemWifiQoSCacheManager.NetworkType testNetworkType = SemWifiQoSCacheManager.NetworkType.from(nt);
                Log.v(TAG, "getNetworkType for test, input:" + nt + " result:" + testNetworkType.name());
                return testNetworkType;
            } else if (prevNetworkTypeInt == SemWifiQoSCacheManager.NetworkType.NO_INTERNET.ordinal()) {
                return SemWifiQoSCacheManager.NetworkType.NO_INTERNET;
            } else {
                if (config != null && config.isCaptivePortal) {
                    Log.v(TAG, "captive portal configuration was set");
                    return SemWifiQoSCacheManager.NetworkType.CAPTIVE_PORTAL;
                } else if (!SemWifiProfileAndQoSProvider.this.isWifiNetworkActivated() || (network = SemWifiProfileAndQoSProvider.this.mConnectivityManager.getActiveNetwork()) == null || !SemWifiProfileAndQoSProvider.this.mConnectivityManager.getNetworkCapabilities(network).hasCapability(17)) {
                    return SemWifiQoSCacheManager.NetworkType.NORMAL;
                } else {
                    Log.v(TAG, "captive portal capability was set");
                    return SemWifiQoSCacheManager.NetworkType.CAPTIVE_PORTAL;
                }
            }
        }

        private int[] getCurrentNetworkScore() {
            if (SemWifiProfileAndQoSProvider.this.mTestQoSData == null || SemWifiProfileAndQoSProvider.this.mTestQoSData.length != 4) {
                return SemWifiProfileAndQoSProvider.this.mAdapter.getCurrentNetworkScore();
            }
            Log.v(TAG, "Using test network type: " + SemWifiProfileAndQoSProvider.this.mTestQoSData[0] + ", score [" + SemWifiProfileAndQoSProvider.this.mTestQoSData[1] + "," + SemWifiProfileAndQoSProvider.this.mTestQoSData[2] + "," + SemWifiProfileAndQoSProvider.this.mTestQoSData[3] + "]");
            return SemWifiProfileAndQoSProvider.this.mTestQoSData;
        }

        private McfDataUtil.McfData getSharedQosInfo(String bssid, WifiConfiguration config) {
            int[] currentNetworkScore = getCurrentNetworkScore();
            if (currentNetworkScore == null) {
                Log.v(TAG, "current network score is empty");
            } else {
                Log.i(TAG, "current network type: " + currentNetworkScore[0] + ", score: [" + currentNetworkScore[1] + ", " + currentNetworkScore[2] + ", " + currentNetworkScore[3] + "]");
            }
            int[] qosData = SemWifiQoSCacheManager.getQoSSpeedInt(currentNetworkScore);
            if (qosData != null) {
                if (this.mMobileWipsDetectedBssids == null) {
                    updateMwipsBssids();
                }
                if (this.mMobileWipsDetectedBssids.contains(bssid)) {
                    Log.v(TAG, "connected network is mwips, skip to share qos");
                    qosData[0] = SemWifiQoSCacheManager.NetworkType.SUSPICIOUS.ordinal();
                } else {
                    qosData[0] = getNetworkType(config, qosData[0]).ordinal();
                }
                McfDataUtil.McfData data = SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataForQos(bssid, qosData);
                SemWifiProfileAndQoSProvider.this.mSubscribeMode.mCache.addOrUpdateScore(bssid, data.getQoSData());
                return data;
            }
            Log.e(TAG, "failed to generate qos array, need to check network score data");
            return null;
        }

        /* access modifiers changed from: private */
        public boolean isIptimeSecuredAp(String configKey) {
            return "\"iptime\"WPA_PSK".equals(configKey) || "\"iptime5G\"WPA_PSK".equals(configKey);
        }

        private List<McfDataUtil.McfData> getSharedPasswordInfo(WifiConfiguration apConfig) {
            return SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataListForSharingPasswordForBssid(apConfig.BSSID, apConfig.configKey(), apConfig.preSharedKey);
        }

        private List<McfDataUtil.McfData> getSharedPasswordInfo(String bssid, WifiConfiguration config) {
            String preSharedKey = getPassword(config.networkId);
            String configKey = config.configKey();
            if (isIptimeSecuredAp(configKey)) {
                return SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataListForSharingPasswordForBssid(bssid, configKey, preSharedKey);
            }
            List<McfDataUtil.McfData> pwdInfo = SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataListForSharingPassword(config.configKey(), preSharedKey);
            if (pwdInfo.size() == 0) {
                return SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataListForSharingPasswordForBssid(bssid, configKey, preSharedKey);
            }
            return pwdInfo;
        }

        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v1, resolved type: java.lang.Object} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v2, resolved type: java.lang.String} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v2, resolved type: java.lang.Object} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v2, resolved type: android.net.wifi.WifiConfiguration} */
        /* access modifiers changed from: private */
        /* JADX WARNING: Multi-variable type inference failed */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public android.util.Pair<com.samsung.android.server.wifi.share.McfDataUtil.McfData, java.util.List<com.samsung.android.server.wifi.share.McfDataUtil.McfData>> checkAndGetShareData() {
            /*
                r10 = this;
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                boolean r0 = r0.isKeyguardLocked()
                r1 = 0
                java.lang.String r2 = "WifiProfileShare.Caster"
                if (r0 == 0) goto L_0x0011
                java.lang.String r0 = "checkAndGetShareData - device locked"
                android.util.Log.v(r2, r0)
                return r1
            L_0x0011:
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                boolean r0 = r0.getUltraPowerSaveEnabledFromProvider()
                if (r0 == 0) goto L_0x001f
                java.lang.String r0 = "checkAndGetShareData - emergency mode"
                android.util.Log.v(r2, r0)
                return r1
            L_0x001f:
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                boolean r0 = r0.isEnabledNearByScanningSettings()
                if (r0 != 0) goto L_0x002d
                java.lang.String r0 = "checkAndGetShareData - nearby scanning"
                android.util.Log.v(r2, r0)
                return r1
            L_0x002d:
                r0 = 0
                r3 = 0
                r4 = 0
                android.util.Pair r5 = r10.getCurrentConfig()
                if (r5 == 0) goto L_0x0070
                java.lang.Object r6 = r5.first
                r4 = r6
                java.lang.String r4 = (java.lang.String) r4
                java.lang.Object r6 = r5.second
                r3 = r6
                android.net.wifi.WifiConfiguration r3 = (android.net.wifi.WifiConfiguration) r3
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r6 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                com.samsung.android.server.wifi.WifiGuiderFeatureController r6 = r6.mFeatureController
                boolean r6 = r6.isSupportQosProvider()
                if (r6 != 0) goto L_0x0052
                java.lang.String r6 = "checkAndGetShareData - feature unsupported (qos share)"
                android.util.Log.v(r2, r6)
                goto L_0x0070
            L_0x0052:
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r6 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                boolean r6 = r6.isEnableNetworkRecommendation()
                if (r6 != 0) goto L_0x0060
                java.lang.String r6 = "checkAndGetShareData - not exist network rating provider"
                android.util.Log.v(r2, r6)
                goto L_0x0070
            L_0x0060:
                boolean r6 = r10.isAvailableToShareQoS(r3)
                if (r6 != 0) goto L_0x006c
                java.lang.String r6 = "checkAndGetShareData(qos) - unavailable for this config"
                android.util.Log.v(r2, r6)
                goto L_0x0070
            L_0x006c:
                com.samsung.android.server.wifi.share.McfDataUtil$McfData r0 = r10.getSharedQosInfo(r4, r3)
            L_0x0070:
                java.util.ArrayList r6 = new java.util.ArrayList
                r6.<init>()
                r10.mLastSharedStaConfigKey = r1
                r10.mLastSharedApConfigKey = r1
                boolean r7 = r10.mEnablePasswordShare
                if (r7 != 0) goto L_0x0084
                java.lang.String r7 = "checkAndGetShareData(pwd) - subscriber activated"
                android.util.Log.v(r2, r7)
                goto L_0x00ff
            L_0x0084:
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r7 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                com.samsung.android.server.wifi.WifiGuiderFeatureController r7 = r7.mFeatureController
                boolean r7 = r7.isSupportWifiProfileShare()
                if (r7 != 0) goto L_0x0096
                java.lang.String r7 = "checkAndGetShareData(pwd) - feature unsupported"
                android.util.Log.v(r2, r7)
                goto L_0x00ff
            L_0x0096:
                if (r3 != 0) goto L_0x009e
                java.lang.String r7 = "checkAndGetShareData(pwd sta) - config is null"
                android.util.Log.v(r2, r7)
                goto L_0x00b7
            L_0x009e:
                boolean r7 = r10.isAvailableToShareConfig(r3)
                if (r7 != 0) goto L_0x00aa
                java.lang.String r7 = "checkAndGetShareData(pwd sta) - unavailable for this config"
                android.util.Log.v(r2, r7)
                goto L_0x00b7
            L_0x00aa:
                java.lang.String r7 = r3.configKey()
                r10.mLastSharedStaConfigKey = r7
                java.util.List r7 = r10.getSharedPasswordInfo(r4, r3)
                r6.addAll(r7)
            L_0x00b7:
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r7 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$Adapter r7 = r7.mAdapter
                int r7 = r7.getWifiApState()
                r8 = 13
                if (r7 != r8) goto L_0x00ff
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r7 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$Adapter r7 = r7.mAdapter
                android.net.wifi.WifiConfiguration r7 = r7.getWifiApConfiguration()
                if (r7 != 0) goto L_0x00d7
                java.lang.String r8 = "checkAndGetShareData(pwd mhs) - config is null"
                android.util.Log.e(r2, r8)
                goto L_0x00ff
            L_0x00d7:
                boolean r8 = r10.isSecuredAp(r7)
                if (r8 != 0) goto L_0x00f2
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "checkAndGetShareData(pwd mhs) - unavailable for this config "
                r8.append(r9)
                r8.append(r7)
                java.lang.String r8 = r8.toString()
                android.util.Log.e(r2, r8)
                goto L_0x00ff
            L_0x00f2:
                java.lang.String r8 = r7.configKey()
                r10.mLastSharedApConfigKey = r8
                java.util.List r8 = r10.getSharedPasswordInfo(r7)
                r6.addAll(r8)
            L_0x00ff:
                if (r0 != 0) goto L_0x010d
                int r7 = r6.size()
                if (r7 != 0) goto L_0x010d
                java.lang.String r7 = "checkAndGetShareData - there is no data"
                android.util.Log.v(r2, r7)
                return r1
            L_0x010d:
                android.util.Pair r1 = android.util.Pair.create(r0, r6)
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.checkAndGetShareData():android.util.Pair");
        }

        /* access modifiers changed from: private */
        public McfDataUtil.McfData getLastSharingQoSInfo() {
            return this.mLastUpdatedQoSData;
        }

        /* access modifiers changed from: private */
        public boolean startShare(McfDataUtil.McfData qosInfo, List<McfDataUtil.McfData> pwdInfo) {
            this.mLastUpdatedQoSData = qosInfo;
            if (qosInfo == null && pwdInfo == null) {
                return false;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("start caster for ");
            if (qosInfo != null) {
                sb.append("qos (");
                sb.append(qosInfo);
                sb.append(") ");
            }
            for (McfDataUtil.McfData pwdItem : pwdInfo) {
                sb.append("pwd (");
                sb.append(pwdItem);
                sb.append(") ");
            }
            sb.append("size ");
            sb.append(pwdInfo.size());
            Log.i(TAG, sb.toString());
            return SemWifiProfileAndQoSProvider.this.mMcfProvider.startCaster(qosInfo, pwdInfo, this.mCallback);
        }

        /* access modifiers changed from: private */
        public void startActivityForUserConfirm(boolean show, boolean isHotspot, String userData) {
            StringBuilder sb = new StringBuilder();
            sb.append(show ? "show" : "dismiss");
            sb.append(" confirm dialog for ");
            sb.append(userData);
            Log.i(TAG, sb.toString());
            if (show) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.SharePasswordDialog");
                intent.putExtra("userData", userData);
                intent.putExtra("isHotspot", isHotspot);
                try {
                    SemWifiProfileAndQoSProvider.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Intent intent2 = new Intent("com.samsung.android.net.wifi.DISMISS_REQ_PASSWORD_DIALOG");
                if (userData != null) {
                    intent2.putExtra("userData", userData);
                }
                intent2.setPackage("com.android.settings");
                try {
                    SemWifiProfileAndQoSProvider.this.mContext.sendBroadcastAsUser(intent2, UserHandle.CURRENT);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        public void onServiceConnected() {
            sendMessage(20);
            this.mRetryCount = 0;
        }

        public void onFailedToBindService() {
            int i = this.mRetryCount;
            this.mRetryCount = i + 1;
            if (i < 5) {
                sendMessageDelayed(1, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
            }
        }

        /* access modifiers changed from: protected */
        public String getLogRecString(Message msg) {
            return " " + msg.arg1;
        }

        /* access modifiers changed from: protected */
        public String getWhatToString(int what) {
            String s = this.mGetWhatToString.get(what);
            if (s != null) {
                return s;
            }
            return SemWifiProfileAndQoSProvider.super.getWhatToString(what);
        }

        /* access modifiers changed from: package-private */
        public void clearUserConfirmHistory() {
            sendMessage(11);
        }

        /* access modifiers changed from: private */
        public void clearUserConfirmHistoryInternal() {
            for (String userData : this.mNotifiedUserData.keySet()) {
                Boolean[] value = this.mNotifiedUserData.get(userData);
                if (value == null || value[0].booleanValue()) {
                    Log.v(TAG, "already closed confirm dialog, user data " + userData);
                } else {
                    SemWifiProfileAndQoSProvider.this.mMcfProvider.setUserConfirm(false, userData);
                    Log.d(TAG, "auto reject profile sharing for " + userData);
                }
            }
            Log.v(TAG, "clear all confirm history");
            this.mNotifiedUserData.clear();
            SemWifiProfileAndQoSProvider.this.mMcfProvider.clearUserRequestPasswordHistory();
        }

        private class DefaultState extends State {
            private DefaultState() {
            }

            public void enter() {
                CasterMode.super.enter();
                Pair unused = CasterMode.this.mShareData = null;
            }

            public boolean processMessage(Message msg) {
                int i = msg.what;
                if (i != 1) {
                    if (i != 5) {
                        if (i != 7) {
                            if (i == 20) {
                                Log.d(CasterMode.TAG, "mcf service connected, retry " + msg.arg1);
                                if (CasterMode.this.mShareData != null) {
                                    CasterMode casterMode = CasterMode.this;
                                    if (casterMode.startShare((McfDataUtil.McfData) casterMode.mShareData.first, (List) CasterMode.this.mShareData.second)) {
                                        CasterMode casterMode2 = CasterMode.this;
                                        casterMode2.transitionTo(casterMode2.mActiveState);
                                    } else if (msg.arg1 > 3) {
                                        Log.e(CasterMode.TAG, "failed to start caster mode");
                                    } else {
                                        CasterMode.this.sendMessageDelayed(20, msg.arg1 + 1, 0, 3000);
                                    }
                                }
                            } else if (i != 22) {
                                if (i == 10) {
                                    SemWifiProfileAndQoSProvider.this.setMcfServiceForCaster(false);
                                    SemWifiProfileAndQoSProvider.this.checkAndUnbindMcfService(CasterMode.this);
                                } else if (i == 11) {
                                    CasterMode.this.clearUserConfirmHistoryInternal();
                                }
                            }
                        } else if (msg.arg1 == 1) {
                            CasterMode.this.sendMessage(1);
                        }
                    }
                    String userData = (String) msg.obj;
                    boolean isHotspot = false;
                    if (userData != null && CasterMode.this.mNotifiedUserData.containsKey(userData)) {
                        Boolean[] value = (Boolean[]) CasterMode.this.mNotifiedUserData.get(userData);
                        if (value != null) {
                            value[0] = true;
                            isHotspot = value[1].booleanValue();
                            CasterMode.this.mNotifiedUserData.put(userData, value);
                        } else {
                            Log.v(CasterMode.TAG, "dismiss confirm dialog, user data " + userData);
                        }
                    }
                    CasterMode.this.startActivityForUserConfirm(false, isHotspot, userData);
                } else {
                    CasterMode casterMode3 = CasterMode.this;
                    Pair unused = casterMode3.mShareData = casterMode3.checkAndGetShareData();
                    if (CasterMode.this.mShareData != null) {
                        CasterMode.this.removeMessages(10);
                        SemWifiProfileAndQoSProvider.this.checkAndBindMcfService(CasterMode.this);
                    }
                }
                return true;
            }
        }

        private class ActiveState extends State {
            private int mQoSPollCounter;

            private ActiveState() {
            }

            private void registerBssidWatcherIfNecessary() {
                if (CasterMode.this.mLastSharedStaConfigKey != null) {
                    CasterMode casterMode = CasterMode.this;
                    if (!casterMode.isIptimeSecuredAp(casterMode.mLastSharedStaConfigKey)) {
                        SemWifiProfileAndQoSProvider.this.mMcfUtil.registerListener(CasterMode.this.mLastSharedStaConfigKey, 
                        /*  JADX ERROR: Method code generation error
                            jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0027: INVOKE  
                              (wrap: com.samsung.android.server.wifi.share.McfDataUtil : 0x0018: INVOKE  (r0v9 com.samsung.android.server.wifi.share.McfDataUtil) = 
                              (wrap: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider : 0x0016: IGET  (r0v8 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider) = 
                              (wrap: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode : 0x0014: IGET  (r0v7 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode) = 
                              (r3v0 'this' com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState A[THIS])
                             com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.ActiveState.this$1 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode)
                             com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this$0 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider)
                             com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.access$2400(com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider):com.samsung.android.server.wifi.share.McfDataUtil type: STATIC)
                              (wrap: java.lang.String : 0x001e: INVOKE  (r1v2 java.lang.String) = 
                              (wrap: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode : 0x001c: IGET  (r1v1 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode) = 
                              (r3v0 'this' com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState A[THIS])
                             com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.ActiveState.this$1 com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode)
                             com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.access$900(com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode):java.lang.String type: STATIC)
                              (wrap: com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU : 0x0024: CONSTRUCTOR  (r2v0 com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU) = 
                              (r3v0 'this' com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState A[THIS])
                             call: com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU.<init>(com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState):void type: CONSTRUCTOR)
                             com.samsung.android.server.wifi.share.McfDataUtil.registerListener(java.lang.String, com.samsung.android.server.wifi.share.McfDataUtil$OnBssidChangeListener):void type: VIRTUAL in method: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.ActiveState.registerBssidWatcherIfNecessary():void, dex: classes.dex
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:221)
                            	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                            	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:142)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:62)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                            	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:142)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:62)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:211)
                            	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:204)
                            	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:318)
                            	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:271)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:240)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:112)
                            	at jadx.core.codegen.ClassGen.addInnerClass(ClassGen.java:249)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:238)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:112)
                            	at jadx.core.codegen.ClassGen.addInnerClass(ClassGen.java:249)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:238)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:112)
                            	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:78)
                            	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:44)
                            	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:33)
                            	at jadx.core.codegen.CodeGen.generate(CodeGen.java:21)
                            	at jadx.core.ProcessClass.generateCode(ProcessClass.java:61)
                            	at jadx.core.dex.nodes.ClassNode.decompile(ClassNode.java:273)
                            Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0024: CONSTRUCTOR  (r2v0 com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU) = 
                              (r3v0 'this' com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState A[THIS])
                             call: com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU.<init>(com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode$ActiveState):void type: CONSTRUCTOR in method: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.ActiveState.registerBssidWatcherIfNecessary():void, dex: classes.dex
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                            	at jadx.core.codegen.InsnGen.addWrappedArg(InsnGen.java:123)
                            	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:107)
                            	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:787)
                            	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:728)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:368)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:250)
                            	... 69 more
                            Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Expected class to be processed at this point, class: com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU, state: NOT_LOADED
                            	at jadx.core.dex.nodes.ClassNode.ensureProcessed(ClassNode.java:260)
                            	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:606)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:364)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                            	... 75 more
                            */
                        /*
                            this = this;
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this
                            java.lang.String r0 = r0.mLastSharedStaConfigKey
                            if (r0 == 0) goto L_0x002b
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this
                            java.lang.String r1 = r0.mLastSharedStaConfigKey
                            boolean r0 = r0.isIptimeSecuredAp(r1)
                            if (r0 != 0) goto L_0x002b
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                            com.samsung.android.server.wifi.share.McfDataUtil r0 = r0.mMcfUtil
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode r1 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this
                            java.lang.String r1 = r1.mLastSharedStaConfigKey
                            com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU r2 = new com.samsung.android.server.wifi.share.-$$Lambda$SemWifiProfileAndQoSProvider$CasterMode$ActiveState$E_ajUbJigajl29TajA0uOji19BU
                            r2.<init>(r3)
                            r0.registerListener(r1, r2)
                            return
                        L_0x002b:
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider$CasterMode r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.this
                            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.this
                            com.samsung.android.server.wifi.share.McfDataUtil r0 = r0.mMcfUtil
                            r0.unregisterListener()
                            return
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider.CasterMode.ActiveState.registerBssidWatcherIfNecessary():void");
                    }

                    /* renamed from: lambda$registerBssidWatcherIfNecessary$0$SemWifiProfileAndQoSProvider$CasterMode$ActiveState */
                    public /* synthetic */ void mo8067x8fc1a32d() {
                        CasterMode.this.sendMessage(9);
                    }

                    public void enter() {
                        CasterMode.super.enter();
                        this.mQoSPollCounter = 0;
                        setQoSUpdatePoll(true);
                        CasterMode.this.removeMessages(10);
                        SemWifiProfileAndQoSProvider.this.setMcfServiceForCaster(true);
                        registerBssidWatcherIfNecessary();
                    }

                    public void exit() {
                        CasterMode.super.exit();
                        setQoSUpdatePoll(false);
                        CasterMode.this.stopShare();
                        CasterMode.this.sendMessage(10);
                        Pair unused = CasterMode.this.mShareData = null;
                        SemWifiProfileAndQoSProvider.this.mMcfUtil.unregisterListener();
                    }

                    private void setQoSUpdatePoll(boolean enable) {
                        CasterMode.this.removeMessages(8);
                        if (enable) {
                            if (this.mQoSPollCounter == 0) {
                                CasterMode.this.sendMessageDelayed(8, 8000);
                            } else {
                                CasterMode.this.sendMessageDelayed(8, 1, 0, SemWifiProfileAndQoSProvider.PERIOD_QOS_DATA_UPDATE_TIME);
                            }
                            this.mQoSPollCounter++;
                        }
                    }

                    private Pair<String, Boolean> getNextUserData() {
                        for (String userDataKey : CasterMode.this.mNotifiedUserData.keySet()) {
                            Boolean[] value = (Boolean[]) CasterMode.this.mNotifiedUserData.get(userDataKey);
                            if (value != null && !value[0].booleanValue()) {
                                return Pair.create(userDataKey, value[1]);
                            }
                        }
                        return null;
                    }

                    private boolean isFirstRequest() {
                        int count = 0;
                        for (Boolean[] value : CasterMode.this.mNotifiedUserData.values()) {
                            if (!value[0].booleanValue()) {
                                count++;
                            }
                        }
                        return count == 1;
                    }

                    private boolean isNeverRequested(String userData) {
                        return userData != null && !CasterMode.this.mNotifiedUserData.containsKey(userData);
                    }

                    private boolean addToRequestList(boolean isHotspot, String userData) {
                        if (isNeverRequested(userData)) {
                            CasterMode.this.mNotifiedUserData.put(userData, new Boolean[]{false, Boolean.valueOf(isHotspot)});
                            return true;
                        }
                        Log.v(CasterMode.TAG, "already requested " + userData);
                        return false;
                    }

                    private void setUserConfirm(String userData, boolean isAccept) {
                        if (userData == null || !CasterMode.this.mNotifiedUserData.containsKey(userData)) {
                            Log.e(CasterMode.TAG, "user " + userData + " never requested password");
                            return;
                        }
                        Boolean[] value = (Boolean[]) CasterMode.this.mNotifiedUserData.get(userData);
                        if (value != null) {
                            value[0] = true;
                            CasterMode.this.mNotifiedUserData.put(userData, value);
                            SemWifiProfileAndQoSProvider.this.mMcfProvider.setUserConfirm(isAccept, userData);
                            return;
                        }
                        Log.e(CasterMode.TAG, "value is null, user " + userData);
                    }

                    public boolean processMessage(Message msg) {
                        int i = msg.what;
                        if (i != 1) {
                            if (i != 2) {
                                boolean isHotspot = false;
                                if (i == 3) {
                                    if (msg.arg1 == 1) {
                                        isHotspot = true;
                                    }
                                    CasterMode.this.startActivityForUserConfirm(true, isHotspot, (String) msg.obj);
                                } else if (i != 4) {
                                    if (i != 5) {
                                        if (i != 7) {
                                            if (i == 8) {
                                                CasterMode casterMode = CasterMode.this;
                                                if (msg.arg1 == 1) {
                                                    isHotspot = true;
                                                }
                                                casterMode.updateQoSDataPoll(isHotspot);
                                                setQoSUpdatePoll(true);
                                            } else if (i == 9) {
                                                List<McfDataUtil.McfData> pwdInfoList = (List) CasterMode.this.mShareData.second;
                                                if (pwdInfoList != null && pwdInfoList.size() > 0) {
                                                    SemWifiProfileAndQoSProvider.this.mMcfProvider.checkAndUpdatePasswordData(SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataListForSharingPassword(pwdInfoList.get(0).getConfigKey(), pwdInfoList.get(0).getPassword()), CasterMode.this.mCallback);
                                                }
                                            } else if (i != 21) {
                                                if (i != 22) {
                                                    switch (i) {
                                                        case 11:
                                                            break;
                                                        case 12:
                                                            SemWifiProfileAndQoSProvider.this.mMcfProvider.checkAndUpdatePasswordData((List<McfDataUtil.McfData>) null, CasterMode.this.mCallback);
                                                            SemWifiProfileAndQoSProvider.this.mMcfUtil.unregisterListener();
                                                            break;
                                                        case 13:
                                                            CasterMode.this.stopShare();
                                                            break;
                                                    }
                                                }
                                            } else {
                                                boolean isHotspot2 = msg.arg1 == 1;
                                                String userData = (String) msg.obj;
                                                if (!isNeverRequested(userData)) {
                                                    Log.d(CasterMode.TAG, "already requested password from user " + userData + ", replying reject message");
                                                    setUserConfirm(userData, false);
                                                } else if (addToRequestList(isHotspot2, userData) && isFirstRequest()) {
                                                    CasterMode.this.sendMessage(3, msg.arg1, msg.arg2, msg.obj);
                                                }
                                            }
                                        }
                                    }
                                    return false;
                                } else {
                                    String userData2 = (String) msg.obj;
                                    if (msg.arg1 == 1) {
                                        isHotspot = true;
                                    }
                                    setUserConfirm(userData2, isHotspot);
                                    Pair<String, Boolean> nextUserData = getNextUserData();
                                    if (nextUserData != null) {
                                        CasterMode.this.sendMessageDelayed(3, ((Boolean) nextUserData.second).booleanValue() ? 1 : 0, 0, nextUserData.first, 500);
                                    }
                                }
                            } else {
                                CasterMode.this.sendMessage(5);
                                CasterMode casterMode2 = CasterMode.this;
                                casterMode2.transitionTo(casterMode2.mDefaultState);
                            }
                            return true;
                        }
                        CasterMode casterMode3 = CasterMode.this;
                        Pair unused = casterMode3.mShareData = casterMode3.checkAndGetShareData();
                        if (CasterMode.this.mShareData == null) {
                            CasterMode casterMode4 = CasterMode.this;
                            casterMode4.transitionTo(casterMode4.mDefaultState);
                        } else {
                            CasterMode casterMode5 = CasterMode.this;
                            casterMode5.updateQoSData((McfDataUtil.McfData) casterMode5.mShareData.first);
                            SemWifiProfileAndQoSProvider.this.mMcfProvider.checkAndUpdatePasswordData((List) CasterMode.this.mShareData.second, CasterMode.this.mCallback);
                            registerBssidWatcherIfNecessary();
                        }
                        return true;
                    }
                }
            }

            private class SubscriberMode extends StateMachine implements IMcfServiceState {
                static final int CMD_CLEAR_CACHED_SCORES = 14;
                static final int CMD_INTEGRATE_SCAN_RESULTS = 15;
                static final int CMD_QOS_POLL = 3;
                static final int CMD_REQ_PASSWORD = 5;
                static final int CMD_REQ_PASSWORD_TIMEOUT = 10;
                static final int CMD_REQ_QOS = 4;
                static final int CMD_REQ_QOS_TIMEOUT = 8;
                static final int CMD_SCORE_RECOMMENDATION_SETTING_CHANGED = 12;
                static final int CMD_START = 1;
                static final int CMD_STOP = 2;
                static final int CMD_STOP_REQ_PASSWORD = 6;
                static final int CMD_UNBIND_MCF_SERVICE = 11;
                static final int CMD_WIFI_STATE_CHANGED = 9;
                static final int EVENT_MCF_FOUND_DEVICE = 23;
                static final int EVENT_MCF_RESP_PASSWORD = 20;
                static final int EVENT_MCF_RESP_SCORE = 21;
                static final int EVENT_MCF_SERVICE_CONNECTED = 22;
                private static final String TAG = "WifiProfileShare.Sub";
                /* access modifiers changed from: private */
                public final SemWifiQoSCacheManager mCache = new SemWifiQoSCacheManager();
                /* access modifiers changed from: private */
                public final State mDefaultState = new DefaultState();
                /* access modifiers changed from: private */
                public final State mDeviceIdleState = new DeviceIdleState();
                final SparseArray<String> mGetWhatToString = MessageUtils.findMessageNames(new Class[]{SubscriberMode.class});
                /* access modifiers changed from: private */
                public ISharedPasswordCallback mLastPasswordCallback;
                /* access modifiers changed from: private */
                public String mLastRequestedBssid;
                /* access modifiers changed from: private */
                public String mLastRequestedConfigKey;
                /* access modifiers changed from: private */
                public final State mPasswordReqState = new PasswordRequestedState();
                /* access modifiers changed from: private */
                public final State mQoSReqState = new QoSRequestedState();
                private int mRetryCount;
                /* access modifiers changed from: private */
                public ISubscriberCallback mSubscriberCallback = new ISubscriberCallback() {
                    public void onQoSDataDelivered(McfDataUtil.McfData qosData) {
                        SubscriberMode subscriberMode = SubscriberMode.this;
                        subscriberMode.sendMessage(21, new NetworkScore(qosData.getPartOfBssid(), qosData.getQoSData()));
                    }

                    public void onPasswordDelivered(McfDataUtil.McfData data) {
                        if (SubscriberMode.this.mLastRequestedConfigKey == null || !SubscriberMode.this.mLastRequestedConfigKey.equals(data.getConfigKey())) {
                            Log.e(SubscriberMode.TAG, "wrong password of configKey is delivered " + data.getConfigKey());
                            return;
                        }
                        SubscriberMode.this.sendMessage(20, 1, 0, data.getPassword());
                    }

                    public void onFailedPasswordDelivery() {
                        SubscriberMode.this.sendMessage(20, 0);
                    }

                    public void onFoundDevicesForPassword(boolean isPossibleToRequest) {
                        SubscriberMode.this.sendMessage(23, isPossibleToRequest);
                    }
                };

                SubscriberMode(Looper looper) {
                    super(TAG, looper);
                    addState(this.mDefaultState);
                    addState(this.mDeviceIdleState, this.mDefaultState);
                    addState(this.mQoSReqState, this.mDeviceIdleState);
                    addState(this.mPasswordReqState, this.mDeviceIdleState);
                    setInitialState(this.mDefaultState);
                    setLogRecSize(32);
                    setLogOnlyTransitions(false);
                }

                public void start() {
                    SemWifiProfileAndQoSProvider.super.start();
                    SemWifiProfileAndQoSProvider.this.mContext.registerReceiver(new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            int wifiState = intent.getIntExtra("wifi_state", 4);
                            if (wifiState == 3) {
                                SubscriberMode.this.sendMessage(9, 1);
                            } else if (wifiState == 1 || wifiState == 4) {
                                SubscriberMode.this.sendMessage(9, 0);
                            }
                        }
                    }, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
                    SemWifiProfileAndQoSProvider.this.mContext.registerReceiver(new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            SubscriberMode.this.sendMessage(15);
                        }
                    }, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
                }

                /* access modifiers changed from: package-private */
                public void setEnableSubscriber(boolean enable) {
                    setEnableSubscriber(enable, 0);
                }

                /* access modifiers changed from: package-private */
                public void setEnableSubscriber(boolean enable, long delayMs) {
                    if (enable) {
                        sendMessageDelayed(1, delayMs);
                    } else {
                        sendMessage(2);
                    }
                }

                /* access modifiers changed from: package-private */
                public void asyncClearCachedNetworkScores() {
                    sendMessage(14);
                }

                /* access modifiers changed from: package-private */
                public void asyncRequestPassword(String bssid, ISharedPasswordCallback callback) {
                    if (callback != null) {
                        Log.i(TAG, "user try to connect network for " + bssid);
                        this.mLastPasswordCallback = callback;
                        sendMessage(5, bssid);
                        return;
                    }
                    Log.e(TAG, "failed to request password, callback is null");
                }

                /* access modifiers changed from: package-private */
                public void asyncRequestPasswordPost(boolean enable) {
                    if (enable) {
                        Log.i(TAG, "user want to request to share network");
                    } else {
                        Log.i(TAG, "stop to request to share network");
                    }
                    sendMessage(5, enable ? 1 : 2, 0);
                }

                /* access modifiers changed from: package-private */
                public void asyncCancelToRequestPassword() {
                    Log.i(TAG, "cancel to request password");
                    this.mLastPasswordCallback = null;
                    sendMessage(6);
                }

                /* access modifiers changed from: package-private */
                public Map<String, Map<String, Integer>> syncGetQoSScores(List<String> bssids) {
                    if (bssids != null) {
                        sendMessage(4);
                    }
                    return this.mCache.getScores(bssids);
                }

                /* access modifiers changed from: private */
                public void callbackToClient(boolean isFoundDevice) {
                    if (this.mLastPasswordCallback != null) {
                        try {
                            Log.i(TAG, "found and connected device for password");
                            this.mLastPasswordCallback.onAvailable(isFoundDevice);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                /* access modifiers changed from: private */
                public void callbackToClient(boolean isAccept, String bssid, String password) {
                    if (this.mLastPasswordCallback != null) {
                        try {
                            Log.i(TAG, "response from other device. accept:" + isAccept);
                            if (isAccept) {
                                this.mLastPasswordCallback.onAccept(bssid, password);
                            } else {
                                this.mLastPasswordCallback.onRejected(bssid);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                /* access modifiers changed from: private */
                public boolean checkConditions() {
                    if (SemWifiProfileAndQoSProvider.this.isKeyguardLocked()) {
                        Log.v(TAG, "device is locked");
                        return false;
                    } else if (SemWifiProfileAndQoSProvider.this.getUltraPowerSaveEnabledFromProvider()) {
                        Log.v(TAG, "emergency mode enabled");
                        return false;
                    } else if (SemWifiProfileAndQoSProvider.this.mFeatureController.isSupportSamsungNetworkScore() || SemWifiProfileAndQoSProvider.this.mFeatureController.isSupportWifiProfileRequest()) {
                        return SemWifiProfileAndQoSProvider.this.mAdapter.isWifiEnabled();
                    } else {
                        Log.v(TAG, "feature is disabled");
                        return false;
                    }
                }

                public void onServiceConnected() {
                    sendMessage(22);
                    this.mRetryCount = 0;
                }

                public void onFailedToBindService() {
                    int i = this.mRetryCount;
                    this.mRetryCount = i + 1;
                    if (i < 5) {
                        sendMessageDelayed(1, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
                    }
                }

                /* access modifiers changed from: protected */
                public String getLogRecString(Message msg) {
                    return " " + msg.arg1;
                }

                /* access modifiers changed from: protected */
                public String getWhatToString(int what) {
                    String s = this.mGetWhatToString.get(what);
                    if (s != null) {
                        return s;
                    }
                    return SemWifiProfileAndQoSProvider.super.getWhatToString(what);
                }

                private class DefaultState extends State {
                    private DefaultState() {
                    }

                    public boolean processMessage(Message msg) {
                        int i = msg.what;
                        if (i != 1) {
                            if (i == 5) {
                                SubscriberMode.this.deferMessage(msg);
                            } else if (i != 9) {
                                if (i == 11) {
                                    SemWifiProfileAndQoSProvider.this.setMcfServiceForSubscriber(false);
                                    SemWifiProfileAndQoSProvider.this.checkAndUnbindMcfService(SubscriberMode.this);
                                } else if (i == 14) {
                                    SubscriberMode.this.mCache.removeAll();
                                } else if (i == 15) {
                                    SemWifiProfileAndQoSProvider.this.mMcfUtil.updateScanResults(SemWifiProfileAndQoSProvider.this.mAdapter.getScanResults());
                                }
                            } else if (msg.arg1 == 1) {
                                SubscriberMode.this.sendMessage(1);
                            }
                        } else if (SubscriberMode.this.checkConditions()) {
                            SubscriberMode subscriberMode = SubscriberMode.this;
                            subscriberMode.transitionTo(subscriberMode.mDeviceIdleState);
                        }
                        return true;
                    }
                }

                private class DeviceIdleState extends State {
                    private DeviceIdleState() {
                    }

                    public void enter() {
                        SubscriberMode.super.enter();
                    }

                    public void exit() {
                        SubscriberMode.super.exit();
                    }

                    public boolean processMessage(Message msg) {
                        int i = msg.what;
                        if (i != 9) {
                            if (i != 12) {
                                if (!(i == 20 || i == 23)) {
                                    switch (i) {
                                        case 1:
                                        case 6:
                                            break;
                                        case 2:
                                            SubscriberMode subscriberMode = SubscriberMode.this;
                                            subscriberMode.transitionTo(subscriberMode.mDefaultState);
                                            break;
                                        case 3:
                                        case 4:
                                            if (SemWifiProfileAndQoSProvider.this.mFeatureController.isSupportSamsungNetworkScore()) {
                                                if (SemWifiProfileAndQoSProvider.this.isEnableNetworkRecommendation()) {
                                                    if (SemWifiProfileAndQoSProvider.this.mAdapter.isWifiEnabled()) {
                                                        SubscriberMode subscriberMode2 = SubscriberMode.this;
                                                        subscriberMode2.transitionTo(subscriberMode2.mQoSReqState);
                                                        break;
                                                    } else {
                                                        Log.v(SubscriberMode.TAG, "qos request ignored, Wi-Fi disabled");
                                                        break;
                                                    }
                                                } else {
                                                    Log.v(SubscriberMode.TAG, "qos request ignored, feature unsupported");
                                                    break;
                                                }
                                            } else {
                                                Log.v(SubscriberMode.TAG, "qos request ignored, feature unsupported");
                                                break;
                                            }
                                        case 5:
                                            if (msg.arg1 != 2) {
                                                if (!SemWifiProfileAndQoSProvider.this.mFeatureController.isSupportWifiProfileRequest()) {
                                                    Log.v(SubscriberMode.TAG, "device unsupported profile request");
                                                    break;
                                                } else {
                                                    String unused = SubscriberMode.this.mLastRequestedBssid = (String) msg.obj;
                                                    if (SubscriberMode.this.mLastRequestedBssid == null) {
                                                        Log.e(SubscriberMode.TAG, "request to share failed, target bssid is null");
                                                        break;
                                                    } else {
                                                        SubscriberMode subscriberMode3 = SubscriberMode.this;
                                                        subscriberMode3.transitionTo(subscriberMode3.mPasswordReqState);
                                                        break;
                                                    }
                                                }
                                            }
                                            break;
                                        default:
                                            return false;
                                    }
                                }
                            } else if (msg.arg1 == 1) {
                                SubscriberMode.this.sendMessage(4);
                            } else {
                                SubscriberMode.this.removeMessages(3);
                            }
                        } else if (msg.arg1 == 1) {
                            SubscriberMode.this.sendMessage(4);
                        } else {
                            SubscriberMode.this.sendMessage(2);
                        }
                        return true;
                    }
                }

                private class QoSRequestedState extends State {
                    private boolean mIsStarted;
                    private long mLastScanRequestedTime;

                    private QoSRequestedState() {
                        this.mLastScanRequestedTime = 0;
                    }

                    private void resetTimeout() {
                        SubscriberMode.this.removeMessages(8);
                        SubscriberMode.this.sendMessageDelayed(8, 30000);
                    }

                    public void enter() {
                        SubscriberMode.super.enter();
                        SemWifiProfileAndQoSProvider.this.checkAndBindMcfService(SubscriberMode.this);
                        SemWifiProfileAndQoSProvider.this.setMcfServiceForSubscriber(true);
                        resetTimeout();
                        SubscriberMode.this.removeMessages(11);
                    }

                    public void exit() {
                        SubscriberMode.super.exit();
                        stopSubscribeForQoS();
                        SubscriberMode.this.removeMessages(8);
                        SubscriberMode.this.sendMessage(11);
                    }

                    private void stopSubscribeForQoS() {
                        Log.i(SubscriberMode.TAG, "stop subscriber mode");
                        this.mIsStarted = false;
                        SemWifiProfileAndQoSProvider.this.mMcfProvider.stopSubscriberModeForQoS();
                        SubscriberMode.this.mCache.removeOldItems();
                    }

                    private boolean startSubscribeForQoS() {
                        if (this.mIsStarted) {
                            Log.v(SubscriberMode.TAG, "already started subscriber mode: qos");
                            return true;
                        }
                        Log.i(SubscriberMode.TAG, "start subscriber mode: qos");
                        this.mIsStarted = SemWifiProfileAndQoSProvider.this.mMcfProvider.startSubscriberForQoS(SubscriberMode.this.mSubscriberCallback);
                        return this.mIsStarted;
                    }

                    private boolean isOpenNetwork(List<String> configKeys) {
                        String configKey;
                        if (configKeys.size() <= 0 || (configKey = configKeys.get(0)) == null) {
                            return false;
                        }
                        if (configKey.endsWith(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE) || configKey.endsWith("OWE")) {
                            return true;
                        }
                        return false;
                    }

                    public boolean processMessage(Message msg) {
                        int i = msg.what;
                        if (i != 3) {
                            if (i == 4) {
                                resetTimeout();
                            } else if (i == 8) {
                                SubscriberMode subscriberMode = SubscriberMode.this;
                                subscriberMode.transitionTo(subscriberMode.mDeviceIdleState);
                            } else if (i != 11) {
                                if (i != 12) {
                                    if (i == 21) {
                                        NetworkScore scoreData = (NetworkScore) msg.obj;
                                        if (scoreData.bssid == null) {
                                            scoreData.bssid = SemWifiProfileAndQoSProvider.this.mMcfUtil.findBssidFromScanResult(scoreData.pBssid);
                                        }
                                        if (scoreData.bssid != null) {
                                            List<String> configKeys = SemWifiProfileAndQoSProvider.this.mMcfUtil.getConfigKeys(scoreData.bssid);
                                            if (!SemWifiProfileAndQoSProvider.this.mQosOnlyOpenNetwork || isOpenNetwork(configKeys)) {
                                                for (int i2 = 0; i2 < configKeys.size(); i2++) {
                                                    Log.i(SubscriberMode.TAG, "received qos data " + configKeys.get(i2) + " (" + scoreData + ")");
                                                }
                                                SubscriberMode.this.mCache.addOrUpdateScore(scoreData.bssid, scoreData.speedData);
                                            }
                                        } else if (msg.arg1 == 0) {
                                            long now = SystemClock.elapsedRealtime();
                                            if (now - this.mLastScanRequestedTime > 15000) {
                                                this.mLastScanRequestedTime = now;
                                                Log.e(SubscriberMode.TAG, "bssid not matched, start scan");
                                                SemWifiProfileAndQoSProvider.this.mAdapter.startScan();
                                            }
                                            SubscriberMode.this.sendMessageDelayed(msg.what, 1, 0, new NetworkScore(scoreData), 10000);
                                        }
                                    } else if (i != 22) {
                                        return false;
                                    } else {
                                        Log.d(SubscriberMode.TAG, "mcf service connected retry " + msg.arg1);
                                        if (!startSubscribeForQoS()) {
                                            if (msg.arg1 > 3) {
                                                SubscriberMode.this.removeMessages(8);
                                                SubscriberMode.this.sendMessage(8);
                                                Log.e(SubscriberMode.TAG, "failed to start subscriber mode for qos");
                                            } else {
                                                SubscriberMode.this.sendMessageDelayed(22, msg.arg1 + 1, 0, 3000);
                                            }
                                        }
                                    }
                                } else if (msg.arg1 == 0) {
                                    SubscriberMode subscriberMode2 = SubscriberMode.this;
                                    subscriberMode2.transitionTo(subscriberMode2.mDeviceIdleState);
                                }
                            }
                        }
                        return true;
                    }
                }

                private class PasswordRequestedState extends State {
                    private boolean mIsStarted;

                    private PasswordRequestedState() {
                    }

                    private void resetTimer() {
                        SubscriberMode.this.removeMessages(10);
                        SubscriberMode.this.sendMessageDelayed(10, 3600000);
                    }

                    public void enter() {
                        SubscriberMode.super.enter();
                        SemWifiProfileAndQoSProvider.this.checkAndBindMcfService(SubscriberMode.this);
                        SemWifiProfileAndQoSProvider.this.setMcfServiceForSubscriber(true);
                        resetTimer();
                        SubscriberMode.this.removeMessages(11);
                        SemWifiProfileAndQoSProvider.this.mCasterMode.setPasswordShare(false);
                    }

                    public void exit() {
                        SubscriberMode.super.exit();
                        stopSubscribeForPassword();
                        SubscriberMode.this.removeMessages(10);
                        SubscriberMode.this.sendMessageDelayed(11, IWCEventManager.autoDisconnectThreshold);
                        SemWifiProfileAndQoSProvider.this.mCasterMode.setPasswordShare(true);
                    }

                    private void stopSubscribeForPassword() {
                        Log.i(SubscriberMode.TAG, "stop subscriber mode");
                        ISharedPasswordCallback unused = SubscriberMode.this.mLastPasswordCallback = null;
                        String unused2 = SubscriberMode.this.mLastRequestedBssid = null;
                        String unused3 = SubscriberMode.this.mLastRequestedConfigKey = null;
                        this.mIsStarted = false;
                        SemWifiProfileAndQoSProvider.this.mMcfProvider.stopSubscriberModeForPassword();
                    }

                    private boolean startSubscribeForPassword() {
                        if (this.mIsStarted) {
                            Log.d(SubscriberMode.TAG, "already started subscriber mode: password");
                            return true;
                        }
                        SubscriberMode subscriberMode = SubscriberMode.this;
                        String unused = subscriberMode.mLastRequestedConfigKey = SemWifiProfileAndQoSProvider.this.mMcfUtil.getConfigKeyForPassword(SubscriberMode.this.mLastRequestedBssid, SemWifiProfileAndQoSProvider.this.mAdapter.isWpa3SaeSupported());
                        McfDataUtil.McfData pwdReqInfo = SemWifiProfileAndQoSProvider.this.mMcfUtil.getMcfDataForRequestingPassword(SubscriberMode.this.mLastRequestedBssid, SubscriberMode.this.mLastRequestedConfigKey);
                        if (pwdReqInfo == null) {
                            Log.e(SubscriberMode.TAG, "can not start subscriber mode for password, McfData is null");
                            return false;
                        }
                        Log.i(SubscriberMode.TAG, "start subscriber mode: password " + pwdReqInfo);
                        this.mIsStarted = SemWifiProfileAndQoSProvider.this.mMcfProvider.startSubscriberForPassword(pwdReqInfo, SubscriberMode.this.mSubscriberCallback);
                        return this.mIsStarted;
                    }

                    public boolean processMessage(Message msg) {
                        int i = msg.what;
                        if (!(i == 3 || i == 4)) {
                            boolean z = false;
                            if (i != 5) {
                                if (i == 6) {
                                    SubscriberMode subscriberMode = SubscriberMode.this;
                                    subscriberMode.transitionTo(subscriberMode.mDeviceIdleState);
                                } else if (i != 8) {
                                    if (i == 20) {
                                        SubscriberMode subscriberMode2 = SubscriberMode.this;
                                        if (msg.arg1 == 1) {
                                            z = true;
                                        }
                                        subscriberMode2.callbackToClient(z, SubscriberMode.this.mLastRequestedBssid, (String) msg.obj);
                                        SubscriberMode subscriberMode3 = SubscriberMode.this;
                                        subscriberMode3.transitionTo(subscriberMode3.mDeviceIdleState);
                                    } else if (i == 10) {
                                        Log.e(SubscriberMode.TAG, "device requesting password for 1 hour");
                                        resetTimer();
                                    } else if (i != 11) {
                                        if (i == 22) {
                                            Log.d(SubscriberMode.TAG, "mcf service connected retry " + msg.arg1);
                                            if (!startSubscribeForPassword()) {
                                                if (msg.arg1 > 2) {
                                                    Log.e(SubscriberMode.TAG, "failed to request password to device");
                                                    SubscriberMode.this.sendMessage(6);
                                                } else {
                                                    SubscriberMode.this.sendMessageDelayed(22, msg.arg1 + 1, 0, 2000);
                                                }
                                            }
                                        } else if (i != 23) {
                                            return false;
                                        } else {
                                            SubscriberMode subscriberMode4 = SubscriberMode.this;
                                            if (msg.arg1 == 1) {
                                                z = true;
                                            }
                                            subscriberMode4.callbackToClient(z);
                                        }
                                    }
                                }
                            } else if (msg.arg1 == 1) {
                                SemWifiProfileAndQoSProvider.this.mMcfProvider.startSubscriberForPassword();
                            } else if (msg.arg1 == 2) {
                                SemWifiProfileAndQoSProvider.this.mMcfProvider.stopPostAdvertise();
                            } else {
                                Log.e(SubscriberMode.TAG, "cancel previous password request");
                                this.mIsStarted = false;
                                SemWifiProfileAndQoSProvider.this.mMcfProvider.stopSubscriberModeForPassword();
                                resetTimer();
                                SubscriberMode.this.sendMessage(22);
                            }
                        }
                        return true;
                    }
                }

                private class NetworkScore {
                    String bssid;
                    String pBssid;
                    int[] speedData;

                    private NetworkScore(String pBssid2, int[] speedData2) {
                        this.pBssid = pBssid2;
                        this.bssid = SemWifiProfileAndQoSProvider.this.mMcfUtil.findBssidFromScanResult(pBssid2);
                        this.speedData = speedData2;
                    }

                    private NetworkScore(NetworkScore score) {
                        this.bssid = score.bssid;
                        this.pBssid = score.pBssid;
                        this.speedData = score.speedData;
                    }

                    public String toString() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("bssid:");
                        sb.append(this.bssid);
                        sb.append(", pbssid:");
                        sb.append(this.pBssid);
                        sb.append(", speed:[");
                        for (int speed : this.speedData) {
                            sb.append(speed);
                            sb.append(" ");
                        }
                        sb.append("]");
                        return sb.toString();
                    }
                }
            }
        }
