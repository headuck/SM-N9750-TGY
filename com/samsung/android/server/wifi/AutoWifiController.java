package com.samsung.android.server.wifi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.net.wifi.aware.WifiAwareManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiGeofenceManager;
import com.android.server.wifi.hotspot2.SystemInfo;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.sec.android.app.C0852CscFeatureTagCommon;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoWifiController extends StateMachine {
    public static final String AUTO_WIFI_PACKAGE_NAME = "samsung.wifi.autowifi";
    static final int CMD_ALWAYS_ALLOW_SCANNING_OPTION_CHANGED = 3;
    static final int CMD_AUTO_WIFI_MODE_OFF = 8;
    static final int CMD_AUTO_WIFI_MODE_ON = 7;
    static final int CMD_CHECK_FAVORITE_AP = 17;
    static final int CMD_CONFIGURED_NETWORKS_CHANGED = 18;
    static final int CMD_ENTER_NO_SERVICE_AREA = 15;
    static final int CMD_GEOFENCE_EXIT_TIMER = 13;
    static final int CMD_GEOFENCE_STATE_CHANGED = 5;
    static final int CMD_LOCATION_MODE_OFF = 10;
    static final int CMD_LOCATION_MODE_ON = 9;
    static final int CMD_NETWORK_STATE_CHANGED = 6;
    static final int CMD_PERIODIC_SCAN_POLL = 11;
    static final int CMD_RESET_AUTOWIFI_SCORE = 16;
    static final int CMD_SCREEN_STATE_CHANGED = 12;
    static final int CMD_SIM_ABSENT = 14;
    static final int CMD_UPDATE_SCORE = 19;
    static final int CMD_WIFI_OFF_TRIGGERED = 2;
    static final int CMD_WIFI_ON_TRIGGERED = 1;
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final int FAVORITE_AP_SCORE = 98;
    private static final long GEOFENCE_STATE_CHANGED_DELAY = 10000;
    private static final long GEOFENCE_STATE_CHANGED_DELAY_LONG = 60000;
    public static final int GEOFENCE_STATE_ENTER = 1;
    public static final int GEOFENCE_STATE_EXIT = 0;
    public static final int GEOFENCE_STATE_UNKNOWN = 2;
    private static final long MAX_DURATION_TIME = 300000;
    private static final int MAX_SCAN_RECEIVER_COUNT = 3;
    private static final long PERIODIC_SCAN_INTERVAL = 128000;
    static final int RECEIVED_SCAN_RESULT = 4;
    private static final String SAMSUNG_PACKAGE_NAME = "com.samsung.android.";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "AutoWifiController";
    private static final long UNINTENDED_AP_CONNECTION_TIME = 120000;
    private static final String VERSION = "v3.2.18";
    private static final int[] mApMaskCheckVsie = {660652};
    private static final int[] mIgnorableApMASK = {2861248};
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private static final boolean mSupportWpa3Sae = (true ^ "0".equals("2"));
    ActivityManager mActivityManager;
    final AutoWifiAdapter mAdapter;
    AllSingleScanListener mAllSingleScanListener;
    /* access modifiers changed from: private */
    public boolean mAlwaysWifiOnUser = true;
    final AutoWifiNotificationController mAutoWifiNotificationController;
    WifiAwareManager mAwareManager;
    ConnectivityManager mConnectivityManager;
    final Context mContext;
    private State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public boolean mDoNotUpdateAlwaysWifiOnUserFlag = false;
    /* access modifiers changed from: private */
    public boolean mDoNotUpdateGeofenceExitDelay = false;
    /* access modifiers changed from: private */
    public boolean mEnableSmartSwitch = false;
    /* access modifiers changed from: private */
    public ArrayList<String> mExceptNetworkKeys = new ArrayList<>();
    private State mFavoriteGeofenceEnterState = new FavoriteGeofenceEnterState();
    private State mGeofenceExitState = new GeofenceExitState();
    /* access modifiers changed from: private */
    public State mInitialState = new InitialState();
    /* access modifiers changed from: private */
    public boolean mIsP2pNetworkConnected = false;
    /* access modifiers changed from: private */
    public long mLastConnectedDuration = 0;
    private String mLastConnectedFavoriteApKey = null;
    /* access modifiers changed from: private */
    public int mLastConnectedNetworkId = -1;
    /* access modifiers changed from: private */
    public int mLastGeofenceState = 2;
    /* access modifiers changed from: private */
    public int mLastReceivedNetworkState = 0;
    private int mLastReportedCellCount = 0;
    private int mLastReportedNetworkIdForCellCount = -1;
    /* access modifiers changed from: private */
    public boolean mLastScreenState = true;
    /* access modifiers changed from: private */
    public long mLastUpdatedGeofenceExitTime = 0;
    AutoWifiListener mListener;
    private boolean mLogMessages = false;
    /* access modifiers changed from: private */
    public boolean mManualGeofenceControl = false;
    private int mMaxBssidWhiteListSize;
    private int mMaxCellIdCount;
    /* access modifiers changed from: private */
    public long mMinDurationToTransitGeofenceExitState = 10000;
    /* access modifiers changed from: private */
    public long mMinDurationToTransitWifiOffState = MAX_DURATION_TIME;
    /* access modifiers changed from: private */
    public long mMinScanResultCountToTransitWifiOffState = 3;
    /* access modifiers changed from: private */
    public long mNextScanInterval = 0;
    ArrayList<ContentObserver> mObservers = new ArrayList<>();
    /* access modifiers changed from: private */
    public long mPeriodicScanMaxInterval = PERIODIC_SCAN_INTERVAL;
    ArrayList<BroadcastReceiver> mReceivers = new ArrayList<>();
    /* access modifiers changed from: private */
    public State mScanModeState = new ScanModeState();
    final ScanResultMatcher mScanResultMatcher;
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public State mUserTriggeredWifiOffState = new UserTriggeredWifiOffState();
    /* access modifiers changed from: private */
    public State mUserTriggeredWifiOnState = new UserTriggeredWifiOnState();
    /* access modifiers changed from: private */
    public State mWifiConnectedState = new WifiConnectedState();
    private WifiGeofenceManager.WifiGeofenceStateListener mWifiGeofenceListener = new WifiGeofenceManager.WifiGeofenceStateListener() {
        public void onGeofenceStateChanged(int state, List<String> configKeys) {
            int geofenceState = AutoWifiController.this.getGeofenceInternalState(state);
            int internalState = geofenceState;
            if (AutoWifiController.this.mManualGeofenceControl) {
                Log.d(AutoWifiController.TAG, "Geofence manual mode:ON, geofence configKeys:" + configKeys);
                return;
            }
            if (geofenceState == 1) {
                AutoWifiController.this.mScanResultMatcher.updateGeofenceEnterNetwork(configKeys);
                if (!AutoWifiController.this.mScanResultMatcher.hasFavoriteApInGeofenceArea()) {
                    internalState = 0;
                }
            }
            AutoWifiController.this.setGeofenceState(internalState, geofenceState);
        }
    };
    /* access modifiers changed from: private */
    public State mWifiOffState = new WifiOffState();
    /* access modifiers changed from: private */
    public State mWifiOffWithScanModeState = new WifiOffWithScanModeState();
    /* access modifiers changed from: private */
    public State mWifiOnState = new WifiOnState();

    public interface AutoWifiAdapter {
        int addOrUpdateNetwork(WifiConfiguration wifiConfiguration);

        int getCellCount(String str);

        List<WifiConfiguration> getConfiguredNetworks();

        int getCurrentGeofenceState();

        List<String> getGeofenceEnterKeys();

        NetworkInfo getNetworkInfo();

        List<ScanResult> getScanResults();

        WifiConfiguration getSpecificNetwork(int i);

        int getWifiApEnabledState();

        int getWifiEnabledState();

        WifiInfo getWifiInfo();

        int getWifiSavedState();

        WifiScanner getWifiScanner();

        boolean isUnstableAp(String str);

        boolean isWifiSharingEnabled();

        boolean isWifiToggleEnabled();

        void notifyScanModeChanged();

        void obtainScanAlwaysAvailablePolicy(boolean z);

        void requestGeofenceEnabled(boolean z, String str);

        void setScanAlwaysAvailable(boolean z);

        boolean setWifiEnabled(String str, boolean z);

        void setWifiSavedState(int i);

        boolean startScan(String str);
    }

    public interface AutoWifiListener {
        void onAutoWifiStateChanged(boolean z);
    }

    enum WifiState {
        WIFI_STATE_ON,
        WIFI_STATE_SCANMODE,
        WIFI_STATE_OFF
    }

    public AutoWifiController(Context context, Looper looper, AutoWifiAdapter adapter) {
        super(TAG, looper);
        this.mContext = context;
        this.mAdapter = adapter;
        ArrayList<String> arrayList = this.mExceptNetworkKeys;
        arrayList.add("\"ollehWiFi \"" + WifiConfiguration.KeyMgmt.strings[0]);
        ArrayList<String> arrayList2 = this.mExceptNetworkKeys;
        arrayList2.add("\"olleh GiGA WiFi \"" + WifiConfiguration.KeyMgmt.strings[0]);
        ArrayList<String> arrayList3 = this.mExceptNetworkKeys;
        arrayList3.add("\"KT GiGA WiFi \"" + WifiConfiguration.KeyMgmt.strings[0]);
        ArrayList<String> arrayList4 = this.mExceptNetworkKeys;
        arrayList4.add("\"KT WiFi \"" + WifiConfiguration.KeyMgmt.strings[0]);
        ArrayList<String> arrayList5 = this.mExceptNetworkKeys;
        arrayList5.add("\"T wifi zone\"" + WifiConfiguration.KeyMgmt.strings[0]);
        this.mScanResultMatcher = new ScanResultMatcher(context);
        this.mAutoWifiNotificationController = new AutoWifiNotificationController(this.mContext, this.mAdapter, this.mExceptNetworkKeys);
        this.mLogMessages = DBG;
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mFavoriteGeofenceEnterState, this.mDefaultState);
        addState(this.mScanModeState, this.mFavoriteGeofenceEnterState);
        addState(this.mWifiOnState, this.mFavoriteGeofenceEnterState);
        addState(this.mWifiConnectedState, this.mWifiOnState);
        addState(this.mUserTriggeredWifiOffState, this.mFavoriteGeofenceEnterState);
        addState(this.mGeofenceExitState, this.mDefaultState);
        addState(this.mWifiOffState, this.mGeofenceExitState);
        addState(this.mWifiOffWithScanModeState, this.mGeofenceExitState);
        addState(this.mUserTriggeredWifiOnState, this.mGeofenceExitState);
        setInitialState(this.mInitialState);
        setLogRecSize(DBG ? 500 : 100);
        setLogOnlyTransitions(false);
        WifiGuiderFeatureController featureController = WifiGuiderFeatureController.getInstance(this.mContext);
        this.mMaxCellIdCount = featureController.getAutoWifiCellCount();
        this.mMaxBssidWhiteListSize = featureController.getAutoWifiBssidCount();
        this.mMinDurationToTransitWifiOffState = (long) (featureController.getAutoWifiTurnOffDurationSeconds() * 1000);
        this.mMinScanResultCountToTransitWifiOffState = (long) featureController.getAutoWifiTurnOffScanCount();
        if (SemCscFeature.getInstance().getBoolean(C0852CscFeatureTagCommon.TAG_CSCFEATURE_COMMON_SUPPORTCOMCASTWIFI) || SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_ENABLEAUTOWIFI)) {
            setupDefaultSetting();
        }
        registerReceiver();
        registerForAutoWifiModeChange();
        registerForSmartNetworkSwitchModeChange();
        registerForScanModeChange();
        registerForUltraPowerSaving();
        registerForLocation();
    }

    private void registerReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                int currentState = 0;
                if (networkInfo != null) {
                    if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                        int unused = AutoWifiController.this.mLastConnectedNetworkId = AutoWifiController.this.mAdapter.getWifiInfo().getNetworkId();
                        currentState = 1;
                    } else {
                        currentState = 0;
                    }
                }
                if (AutoWifiController.this.mLastReceivedNetworkState != currentState) {
                    int unused2 = AutoWifiController.this.mLastReceivedNetworkState = currentState;
                    AutoWifiController autoWifiController = AutoWifiController.this;
                    autoWifiController.sendMessage(autoWifiController.obtainMessage(6, currentState));
                }
            }
        };
        this.mContext.registerReceiver(receiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        this.mReceivers.add(receiver);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
            BroadcastReceiver receiver2 = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    boolean unused = AutoWifiController.this.mIsP2pNetworkConnected = ((NetworkInfo) intent.getParcelableExtra("networkInfo")).isConnected();
                    StringBuilder sb = new StringBuilder();
                    sb.append("p2p ");
                    sb.append(AutoWifiController.this.mIsP2pNetworkConnected ? "connected" : "disconnected");
                    Log.d(AutoWifiController.TAG, sb.toString());
                }
            };
            this.mContext.registerReceiver(receiver2, new IntentFilter("android.net.wifi.p2p.CONNECTION_STATE_CHANGE"));
            this.mReceivers.add(receiver2);
        }
        BroadcastReceiver receiver3 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (AutoWifiController.DBG) {
                    Log.d(AutoWifiController.TAG, "onReceive CONFIGURED_NETWORKS_CHANGED_ACTION");
                }
                AutoWifiController.this.removeMessages(18);
                AutoWifiController.this.sendMessageDelayed(18, 3000);
            }
        };
        this.mContext.registerReceiver(receiver3, new IntentFilter("android.net.wifi.CONFIGURED_NETWORKS_CHANGE"));
        this.mReceivers.add(receiver3);
        BroadcastReceiver receiver4 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean connectedViaWifi;
                if (AutoWifiController.this.mConnectivityManager == null) {
                    AutoWifiController autoWifiController = AutoWifiController.this;
                    autoWifiController.mConnectivityManager = (ConnectivityManager) autoWifiController.mContext.getSystemService("connectivity");
                }
                if (AutoWifiController.this.mConnectivityManager != null) {
                    NetworkInfo networkInfo = AutoWifiController.this.mConnectivityManager.getActiveNetworkInfo();
                    int i = 1;
                    if (networkInfo == null || networkInfo.getType() != 1) {
                        connectedViaWifi = false;
                    } else {
                        connectedViaWifi = true;
                    }
                    AutoWifiController autoWifiController2 = AutoWifiController.this;
                    if (!connectedViaWifi) {
                        i = 0;
                    }
                    autoWifiController2.sendMessage(autoWifiController2.obtainMessage(19, i));
                }
            }
        };
        this.mContext.registerReceiver(receiver4, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        this.mReceivers.add(receiver4);
        BroadcastReceiver receiver5 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                AutoWifiController.this.setScreenState(true);
            }
        };
        this.mContext.registerReceiver(receiver5, new IntentFilter("android.intent.action.SCREEN_ON"));
        this.mReceivers.add(receiver5);
        BroadcastReceiver receiver6 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                AutoWifiController.this.setScreenState(false);
            }
        };
        this.mContext.registerReceiver(receiver6, new IntentFilter("android.intent.action.SCREEN_OFF"));
        this.mReceivers.add(receiver6);
    }

    private void registerForScanModeChange() {
        ContentObserver contentObserver = new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange) {
                AutoWifiController autoWifiController = AutoWifiController.this;
                autoWifiController.sendMessage(autoWifiController.obtainMessage(3, autoWifiController.getAlawaysAllowScanModeFromProvider() ? 1 : 0));
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_scan_always_enabled"), false, contentObserver);
        this.mObservers.add(contentObserver);
    }

    private void registerForAutoWifiModeChange() {
        ContentObserver contentObserver = new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange) {
                if (AutoWifiController.this.getAutoWifiEnabledFromProvider()) {
                    AutoWifiController.this.sendMessage(7);
                } else {
                    AutoWifiController.this.sendMessage(8);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sem_auto_wifi_control_enabled"), false, contentObserver);
        this.mObservers.add(contentObserver);
    }

    private void registerForSmartNetworkSwitchModeChange() {
        ContentObserver contentObserver = new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange) {
                AutoWifiController autoWifiController = AutoWifiController.this;
                boolean unused = autoWifiController.mEnableSmartSwitch = autoWifiController.getSamrtSwitchModeFromProvider();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_poor_network_test_enabled"), false, contentObserver);
        this.mObservers.add(contentObserver);
    }

    private void registerForUltraPowerSaving() {
        ContentObserver contentObserver = new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange) {
                if (AutoWifiController.this.getUltraPowerSaveEnabledFromProvider()) {
                    AutoWifiController.this.sendMessage(8);
                } else {
                    AutoWifiController.this.sendMessage(7);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("emergency_mode"), false, contentObserver);
        this.mObservers.add(contentObserver);
    }

    private void registerForLocation() {
        ContentObserver contentObserver = new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.compareTo(Settings.Secure.getUriFor("location_providers_allowed")) != 0) {
                    return;
                }
                if (AutoWifiController.this.isLocationEnabled()) {
                    AutoWifiController.this.sendMessage(9);
                } else {
                    AutoWifiController.this.sendMessage(10);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), true, contentObserver);
        this.mObservers.add(contentObserver);
    }

    public void checkAndStart() {
        start();
        boolean z = true;
        if (getGeofenceInternalState(this.mAdapter.getCurrentGeofenceState()) != 1) {
            z = false;
        }
        forceUpdateGeofenceState(z, (String) null);
        Log.d(TAG, "checkAndStart - geofence state : " + this.mLastGeofenceState);
        this.mAlwaysWifiOnUser = getAlwaysWifiOnUserFromDb();
        Log.d(TAG, "checkAndStart - last user state:" + this.mAlwaysWifiOnUser);
        if (getAutoWifiEnabledFromProvider()) {
            sendMessage(7);
            Log.i(TAG, "Auto Wi-Fi initialized");
            return;
        }
        Log.i(TAG, "Auto Wi-Fi initialized with disabled");
    }

    public void registerScanListener() {
        WifiScanner scanner;
        if (this.mAllSingleScanListener == null && (scanner = this.mAdapter.getWifiScanner()) != null) {
            this.mAllSingleScanListener = new AllSingleScanListener();
            scanner.registerScanListener(this.mAllSingleScanListener);
        }
    }

    public void doQuit() {
        Iterator<BroadcastReceiver> it = this.mReceivers.iterator();
        while (it.hasNext()) {
            this.mContext.unregisterReceiver(it.next());
        }
        Iterator<ContentObserver> it2 = this.mObservers.iterator();
        while (it2.hasNext()) {
            this.mContext.getContentResolver().unregisterContentObserver(it2.next());
        }
        quit();
    }

    public void setListener(AutoWifiListener listener) {
        this.mListener = listener;
    }

    /* renamed from: com.samsung.android.server.wifi.AutoWifiController$13 */
    static /* synthetic */ class C070713 {

        /* renamed from: $SwitchMap$com$samsung$android$server$wifi$AutoWifiController$WifiState */
        static final /* synthetic */ int[] f50xcc994fa8 = new int[WifiState.values().length];

        static {
            try {
                f50xcc994fa8[WifiState.WIFI_STATE_OFF.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f50xcc994fa8[WifiState.WIFI_STATE_SCANMODE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                f50xcc994fa8[WifiState.WIFI_STATE_ON.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean setWifiControllerState(WifiState state) {
        int i = C070713.f50xcc994fa8[state.ordinal()];
        if (i == 1 || i == 2) {
            if (state == WifiState.WIFI_STATE_OFF) {
                setScanAlwaysAvailable(false);
            } else {
                setScanAlwaysAvailable(true);
            }
            if (this.mAdapter.isWifiToggleEnabled()) {
                return this.mAdapter.setWifiEnabled(AUTO_WIFI_PACKAGE_NAME, false);
            }
            this.mAdapter.notifyScanModeChanged();
            return true;
        } else if (i != 3 || this.mAdapter.isWifiToggleEnabled()) {
            return true;
        } else {
            if (!isWifiApEnabeldEnablingState() || this.mAdapter.isWifiSharingEnabled()) {
                return this.mAdapter.setWifiEnabled(AUTO_WIFI_PACKAGE_NAME, true);
            }
            Log.i(TAG, "can't enable Wi-Fi state because hotspot is enabled");
            if (this.mAdapter.getWifiSavedState() != 1) {
                Log.i(TAG, "save Wi-Fi state to enable");
                this.mAdapter.setWifiSavedState(1);
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void updateScoreForCurrentNetwork(boolean connectedViaWifi) {
        if (connectedViaWifi) {
            WifiInfo wifiInfo = this.mAdapter.getWifiInfo();
            if (wifiInfo != null) {
                int networkId = wifiInfo.getNetworkId();
                if (DBG) {
                    Log.i(TAG, "Connected network id:" + networkId);
                }
                WifiConfiguration config = this.mAdapter.getSpecificNetwork(networkId);
                if (config == null) {
                    Log.e(TAG, "Connected network but Wi-Fi configuration is null");
                } else if (isTargetAp(config)) {
                    for (int mask : mIgnorableApMASK) {
                        if ((wifiInfo.getIpAddress() & 16777215) == mask) {
                            Log.d(TAG, "This is Android Hotspot");
                            return;
                        }
                    }
                    String networkKey = config.configKey();
                    if (OpBrandingLoader.Vendor.KTT != mOpBranding || wifiInfo.getFrequency() < 5000 || !this.mScanResultMatcher.isKTHomeAP(wifiInfo.getBSSID())) {
                        if (mOpBranding.getCountry() == 1 && config.bssidWhitelist != null) {
                            int bssidSize = config.bssidWhitelist.size();
                            if (DBG) {
                                Log.i(TAG, "bssid size " + bssidSize + "/" + this.mMaxBssidWhiteListSize);
                            }
                            if (bssidSize > this.mMaxBssidWhiteListSize) {
                                Log.d(TAG, "Current network bssid size:" + bssidSize);
                                this.mAutoWifiNotificationController.setApInManyAreas(config, networkKey);
                                return;
                            }
                        }
                        this.mLastReportedNetworkIdForCellCount = networkId;
                        this.mLastReportedCellCount = this.mAdapter.getCellCount(networkKey);
                        if (DBG) {
                            Log.i(TAG, "collectCellCount: " + this.mLastReportedCellCount + "/" + this.mMaxCellIdCount);
                        }
                        int i = this.mLastReportedCellCount;
                        int i2 = this.mMaxCellIdCount;
                        if (i > i2) {
                            Log.d(TAG, "Current network maybe hotspot as EGG, cell count:" + this.mLastReportedCellCount);
                            this.mAutoWifiNotificationController.setApInManyAreas(config, networkKey);
                            return;
                        }
                        int apWithNumerousCellCount = (int) (((float) i2) * 0.6f);
                        if (i > apWithNumerousCellCount) {
                            Log.d(TAG, "Current network might be hotspot as EGG, cell count:" + this.mLastReportedCellCount + "/" + apWithNumerousCellCount);
                            this.mAutoWifiNotificationController.setPolicyForApWithNumerousCellId();
                        }
                        this.mAutoWifiNotificationController.setNetworkState(true, false, config, networkKey);
                        return;
                    }
                    if (DBG) {
                        Log.d(TAG, "This is KT Home AP");
                    }
                    if (config.semAutoWifiScore != 101) {
                        this.mAutoWifiNotificationController.setKTHomeApToFavorite(config, networkKey);
                    }
                }
            } else {
                Log.e(TAG, "Connected network but Wi-Fi info is null");
            }
        } else {
            this.mAutoWifiNotificationController.setNetworkState(false, false, (WifiConfiguration) null, (String) null);
        }
    }

    private boolean isWifiApEnabeldEnablingState() {
        int wifiState = this.mAdapter.getWifiApEnabledState();
        if (wifiState == 13 || wifiState == 12) {
            return true;
        }
        return false;
    }

    public void setWifiEnabledTriggered(boolean enable, String packageName, boolean triggeredByUser) {
        NetworkInfo networkInfo;
        if (AUTO_WIFI_PACKAGE_NAME.equals(packageName)) {
            Log.i(TAG, "setWifiEnabled called by Auto Wi-Fi Controller");
        } else if (enable) {
            sendMessage(1);
        } else {
            if (triggeredByUser && (networkInfo = this.mAdapter.getNetworkInfo()) != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                this.mAutoWifiNotificationController.setNetworkState(false, true, (WifiConfiguration) null, (String) null);
            }
            if (packageName == null || !packageName.startsWith(SAMSUNG_PACKAGE_NAME) || !packageName.contains("gear")) {
                sendMessage(2);
            } else {
                sendMessage(obtainMessage(2, 1, 0));
            }
        }
    }

    public void factoryReset() {
        this.mAutoWifiNotificationController.factoryReset();
    }

    public void forgetNetwork(int networkId) {
        WifiConfiguration config = this.mAdapter.getSpecificNetwork(networkId);
        if (config != null) {
            String networkKey = config.configKey();
            this.mScanResultMatcher.removeNetwork(networkKey);
            this.mAutoWifiNotificationController.forgetNetwork(networkKey, config);
        }
    }

    /* access modifiers changed from: private */
    public void setGeofenceState(int state, int commonState) {
        Log.i(TAG, "set Geofence state : " + getGeofenceStateString(state));
        this.mLastGeofenceState = state;
        sendMessage(obtainMessage(5, state, commonState));
    }

    public void setSimState(boolean inserted) {
        if (!inserted) {
            sendMessage(14);
        } else {
            sendMessage(7);
        }
    }

    public void setPhoneServiceAvailable(boolean enable) {
        if (enable) {
            sendMessage(7);
        } else {
            sendMessage(15);
        }
    }

    public void setUltraPowerSaveMode(boolean enable) {
        if (enable) {
            sendMessage(8);
        } else {
            sendMessage(7);
        }
    }

    public void setLocationState(boolean enable) {
        if (enable) {
            sendMessage(9);
        } else {
            sendMessage(10);
        }
    }

    public void setAirplainMode(boolean enable) {
        if (enable) {
            sendMessage(8);
        } else {
            sendMessage(7);
        }
    }

    public void setScreenState(boolean on) {
        this.mLastScreenState = on;
        sendMessage(obtainMessage(12, on, 0));
    }

    public WifiGeofenceManager.WifiGeofenceStateListener getGeofenceListener() {
        return this.mWifiGeofenceListener;
    }

    public void setConfigForTest(Bundle configs) {
        Bundle bundle = configs;
        int manualMode = bundle.getInt("manual_mode", -1);
        if (DBG) {
            Log.d(TAG, "manual_mode:" + manualMode);
        }
        if (manualMode >= 0) {
            this.mManualGeofenceControl = manualMode == 1;
        }
        int geofenceState = bundle.getInt("geofence_state", -1);
        if (DBG) {
            Log.d(TAG, "geofence_state:" + geofenceState);
        }
        if (geofenceState >= 0) {
            this.mScanResultMatcher.copyFavoriteNetworksToGeofenceEnterNetworkList();
            setGeofenceState(geofenceState, geofenceState);
        }
        int locationState = bundle.getInt("location_state", -1);
        if (DBG) {
            Log.d(TAG, "location_state:" + locationState);
        }
        if (locationState >= 0) {
            setLocationState(locationState != 0);
        }
        int airplainMode = bundle.getInt("airplain_mode", -1);
        if (DBG) {
            Log.d(TAG, "airplain_mode:" + airplainMode);
        }
        if (airplainMode >= 0) {
            setAirplainMode(airplainMode != 0);
        }
        int ultrapowersaveMode = bundle.getInt("ultrapowersave_mode", -1);
        if (DBG) {
            Log.d(TAG, "ultrapowersave_mode:" + ultrapowersaveMode);
        }
        if (ultrapowersaveMode >= 0) {
            setUltraPowerSaveMode(ultrapowersaveMode != 0);
        }
        long duration = bundle.getLong("max_duration", -1);
        if (DBG) {
            Log.d(TAG, "max_duration:" + duration);
        }
        if (duration > 0) {
            this.mMinDurationToTransitWifiOffState = duration;
        }
        long duration2 = bundle.getLong("max_geofence_exit_duration", -1);
        if (DBG) {
            Log.d(TAG, "max_geofence_exit_duration:" + duration2);
        }
        if (duration2 > 0) {
            this.mDoNotUpdateGeofenceExitDelay = true;
            this.mMinDurationToTransitGeofenceExitState = duration2;
        }
        int i = manualMode;
        long scanInterval = bundle.getLong("scan_interval", -1);
        if (DBG) {
            Log.d(TAG, "scan_interval:" + scanInterval);
        }
        if (scanInterval >= PERIODIC_SCAN_INTERVAL) {
            this.mPeriodicScanMaxInterval = scanInterval;
        }
        int scanCount = bundle.getInt("scan_counter", -1);
        if (DBG) {
            Log.d(TAG, "scan_counter:" + scanCount);
        }
        if (scanCount > 0) {
            this.mMinScanResultCountToTransitWifiOffState = (long) scanCount;
        }
        int maxCellCount = bundle.getInt("cell_counter", -1);
        if (DBG) {
            Log.d(TAG, "cell_counter:" + maxCellCount);
        }
        if (maxCellCount > 0) {
            this.mMaxCellIdCount = maxCellCount;
        }
        int maxBssidCount = bundle.getInt("bssid_counter", -1);
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            long j = scanInterval;
            sb.append("bssid_counter:");
            sb.append(maxBssidCount);
            Log.d(TAG, sb.toString());
        }
        if (maxBssidCount > 0) {
            this.mMaxBssidWhiteListSize = maxBssidCount;
        }
        int initScore = bundle.getInt("init_score", -1);
        if (initScore >= 0) {
            Log.d(TAG, "init_score : " + initScore);
            sendMessage(obtainMessage(16, initScore, 0));
        }
        int i2 = initScore;
        int i3 = geofenceState;
        long durScore1 = bundle.getLong("dur_score_1", -1);
        if (DBG) {
            Log.d(TAG, "duration score 1:" + durScore1);
        }
        int i4 = locationState;
        int i5 = airplainMode;
        int i6 = ultrapowersaveMode;
        long durScore2 = bundle.getLong("dur_score_2", -1);
        if (DBG) {
            Log.d(TAG, "duration score 2:" + durScore2);
        }
        int i7 = maxBssidCount;
        int i8 = maxCellCount;
        long durMinus = bundle.getLong("dur_minus", -1);
        if (DBG) {
            Log.d(TAG, "duration minus:" + durMinus);
        }
        long durRecovery = bundle.getLong("dur_recovery", -1);
        if (DBG) {
            Log.d(TAG, "duration recovery:" + durRecovery);
        }
        if (durScore1 + durScore2 + durMinus + durRecovery > 0) {
            this.mAutoWifiNotificationController.setConfigForTest(durScore1, durScore2, durMinus, durRecovery);
        }
    }

    /* access modifiers changed from: private */
    public boolean checkDeviceStatusInIdle() {
        if (!getSimState() || getAirplainModeEnabledFromProvider() || getUltraPowerSaveEnabledFromProvider()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void setAutoWifiScoreForAll(int initScore) {
        List<WifiConfiguration> savedConfigs = this.mAdapter.getConfiguredNetworks();
        if (savedConfigs != null) {
            Log.d(TAG, "savedConfigs size:" + savedConfigs.size());
            for (WifiConfiguration config : savedConfigs) {
                Log.d(TAG, "setAutoWifiScoreForAll : update " + config.configKey(true) + " prev:" + config.semAutoWifiScore + " new:" + initScore);
                config.semAutoWifiScore = initScore;
                this.mAdapter.addOrUpdateNetwork(config);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setAlwaysWifiOnUserToDb(boolean isAlwaysOnUser) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "sem_auto_wifi_last_user_state", isAlwaysOnUser);
    }

    private boolean getAlwaysWifiOnUserFromDb() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_last_user_state", 1) == 1;
    }

    /* access modifiers changed from: private */
    public boolean getAlawaysAllowScanModeFromProvider() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
    }

    private void setupDefaultSetting() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_control_enabled", -1) == -1) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "sem_auto_wifi_control_enabled", 1);
        }
    }

    /* access modifiers changed from: private */
    public boolean getAutoWifiEnabledFromProvider() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_control_enabled", 0) == 1;
    }

    /* access modifiers changed from: private */
    public boolean getSamrtSwitchModeFromProvider() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 0) == 1;
    }

    /* access modifiers changed from: private */
    public boolean getAirplainModeEnabledFromProvider() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean getSimState() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return TelephonyUtil.isSimCardReady(this.mTelephonyManager);
    }

    /* access modifiers changed from: private */
    public boolean getUltraPowerSaveEnabledFromProvider() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "emergency_mode", 0) == 1;
    }

    /* access modifiers changed from: private */
    public boolean isLocationEnabled() {
        LocationManager locManager = (LocationManager) this.mContext.getSystemService("location");
        if (locManager.isProviderEnabled("network") || locManager.isProviderEnabled("gps")) {
            return true;
        }
        return false;
    }

    public boolean isAutoWifiEnabled() {
        if (!getAutoWifiEnabledFromProvider() || !getSimState() || getAirplainModeEnabledFromProvider() || getUltraPowerSaveEnabledFromProvider()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isActivePeerConnection() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
            if (this.mAwareManager == null) {
                this.mAwareManager = (WifiAwareManager) this.mContext.getSystemService("wifiaware");
            }
            WifiAwareManager wifiAwareManager = this.mAwareManager;
            if (wifiAwareManager != null) {
                int ndpCount = wifiAwareManager.getCountNdp(false);
                Log.i(TAG, "Wi-Fi aware connection count:" + ndpCount);
                if (ndpCount > 0) {
                    return true;
                }
            }
        }
        if (this.mIsP2pNetworkConnected == 0) {
            return false;
        }
        Log.i(TAG, "p2p was activated");
        return true;
    }

    /* access modifiers changed from: private */
    public boolean setWifiStateScanMode() {
        if (setWifiControllerState(WifiState.WIFI_STATE_SCANMODE)) {
            return true;
        }
        Log.e(TAG, "Can't change Wi-Fi state (policy), return state");
        return false;
    }

    /* access modifiers changed from: private */
    public void setWifiStateDisableOrScanMode() {
        if (getAlawaysAllowScanModeFromProvider()) {
            setWifiControllerState(WifiState.WIFI_STATE_SCANMODE);
        } else {
            setWifiControllerState(WifiState.WIFI_STATE_OFF);
        }
    }

    /* access modifiers changed from: private */
    public void transitWifiOffState(boolean updateWifiState) {
        if (getAlawaysAllowScanModeFromProvider() && isLocationEnabled()) {
            if (updateWifiState) {
                setWifiControllerState(WifiState.WIFI_STATE_SCANMODE);
            }
            transitionTo(this.mWifiOffWithScanModeState);
        } else if (!updateWifiState) {
            transitionTo(this.mWifiOffState);
        } else if (setWifiControllerState(WifiState.WIFI_STATE_OFF)) {
            transitionTo(this.mWifiOffState);
        } else {
            Log.e(TAG, "Can't change Wi-Fi state");
        }
    }

    /* access modifiers changed from: private */
    public void obtainScanAlwaysAvailablePolicy(boolean enable) {
        this.mAdapter.obtainScanAlwaysAvailablePolicy(enable);
    }

    /* access modifiers changed from: private */
    public void setScanAlwaysAvailable(boolean enable) {
        this.mAdapter.setScanAlwaysAvailable(enable);
    }

    /* access modifiers changed from: private */
    public int getGeofenceInternalState(int geofenceState) {
        if (geofenceState == 1) {
            return 1;
        }
        if (geofenceState != 2) {
            return 2;
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public void forceUpdateGeofenceState(boolean enterState, String additionalKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("forceUpdateGeofenceState ");
        sb.append(enterState ? "enter" : "exit");
        Log.d(TAG, sb.toString());
        int newGeofenceState = 0;
        if (enterState) {
            newGeofenceState = 1;
            List<String> configKeys = this.mAdapter.getGeofenceEnterKeys();
            if (additionalKey != null && !configKeys.contains(additionalKey)) {
                configKeys.add(additionalKey);
            }
            if (this.mManualGeofenceControl) {
                Log.d(TAG, "Geofence manual mode:ON, geofence configKeys:" + configKeys);
            } else {
                this.mScanResultMatcher.updateGeofenceEnterNetwork(configKeys);
                if (!this.mScanResultMatcher.hasFavoriteApInGeofenceArea()) {
                    newGeofenceState = 0;
                }
            }
        }
        setGeofenceState(newGeofenceState, newGeofenceState);
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(Message message, State state) {
        if (this.mLogMessages) {
            Log.i(TAG, " " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }
    }

    private String printTime() {
        return " rt=" + SystemClock.uptimeMillis() + "/" + SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: protected */
    public String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(smToString(msg));
        sb.append(" ");
        sb.append(printTime());
        int i = msg.what;
        if (i == 5) {
            sb.append(" geofence ");
            sb.append(getGeofenceStateString(msg.arg1));
            sb.append(" (");
            sb.append(getGeofenceStateString(msg.arg1));
            sb.append(")");
        } else if (i == 6) {
            sb.append(" Wi-Fi ");
            sb.append(msg.arg1 == 1 ? "connected" : "disconnected");
        } else if (i != 12) {
            sb.append(" ");
            sb.append(Integer.toString(msg.arg1));
            sb.append(" ");
            sb.append(Integer.toString(msg.arg2));
            sb.append(" g:");
            sb.append(getGeofenceStateString(this.mLastGeofenceState));
            sb.append(" u:");
            sb.append(this.mAlwaysWifiOnUser);
        } else {
            sb.append(" screen ");
            sb.append(msg.arg1 == 1 ? "on" : "off");
        }
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public String smToString(Message message) {
        return smToString(message.what);
    }

    /* access modifiers changed from: package-private */
    public String smToString(int what) {
        switch (what) {
            case 1:
                return "CMD_WIFI_ON_TRIGGERED";
            case 2:
                return "CMD_WIFI_OFF_TRIGGERED";
            case 3:
                return "CMD_ALWAYS_ALLOW_SCANNING_OPTION_CHANGED";
            case 4:
                return "RECEIVED_SCAN_RESULT";
            case 5:
                return "CMD_GEOFENCE_STATE_CHANGED";
            case 6:
                return "CMD_NETWORK_STATE_CHANGED";
            case 7:
                return "CMD_AUTO_WIFI_MODE_ON";
            case 8:
                return "CMD_AUTO_WIFI_MODE_OFF";
            case 9:
                return "CMD_LOCATION_MODE_ON";
            case 10:
                return "CMD_LOCATION_MODE_OFF";
            case 11:
                return "CMD_PERIODIC_SCAN_POLL";
            case 12:
                return "CMD_SCREEN_STATE_CHANGED";
            case 13:
                return "CMD_GEOFENCE_EXIT_TIMER";
            case 14:
                return "CMD_SIM_ABSENT";
            case 15:
                return "CMD_ENTER_NO_SERVICE_AREA";
            case 16:
                return "CMD_RESET_AUTOWIFI_SCORE";
            case 17:
                return "CMD_CHECK_FAVORITE_AP";
            case 18:
                return "CMD_CONFIGURED_NETWORKS_CHANGED";
            case 19:
                return "CMD_UPDATE_SCORE";
            default:
                return "what:" + Integer.toString(what);
        }
    }

    private String getGeofenceStateString(int geofenceState) {
        String s = String.valueOf(geofenceState);
        if (geofenceState == 0) {
            return s + "Exit";
        } else if (geofenceState == 1) {
            return s + "Enter";
        } else if (geofenceState != 2) {
            return s;
        } else {
            return s + SystemInfo.UNKNOWN_INFO;
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            boolean z = false;
            switch (msg.what) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 17:
                    break;
                case 8:
                    if (AutoWifiController.this.mAdapter.getWifiEnabledState() == 1) {
                        AutoWifiController.this.setWifiStateDisableOrScanMode();
                    }
                    AutoWifiController autoWifiController = AutoWifiController.this;
                    autoWifiController.transitionTo(autoWifiController.mInitialState);
                    break;
                case 14:
                case 15:
                    if (AutoWifiController.this.getAutoWifiEnabledFromProvider() && !AutoWifiController.this.mAlwaysWifiOnUser && AutoWifiController.this.checkDeviceStatusInIdle()) {
                        Log.i(AutoWifiController.TAG, "Can't check geofence state, Force turn on the Wi-Fi");
                        boolean unused = AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_ON);
                    }
                    AutoWifiController autoWifiController2 = AutoWifiController.this;
                    autoWifiController2.transitionTo(autoWifiController2.mInitialState);
                    break;
                case 16:
                    AutoWifiController.this.setAutoWifiScoreForAll(msg.arg1);
                    break;
                case 18:
                    if (AutoWifiController.this.getAutoWifiEnabledFromProvider()) {
                        AutoWifiController.this.mScanResultMatcher.updateFavoriteNetworkKey(AutoWifiController.this.mAdapter.getConfiguredNetworks());
                        break;
                    }
                    break;
                case 19:
                    AutoWifiController autoWifiController3 = AutoWifiController.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    autoWifiController3.updateScoreForCurrentNetwork(z);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            AutoWifiController.this.obtainScanAlwaysAvailablePolicy(false);
            AutoWifiController.this.mAutoWifiNotificationController.setEnable(false);
            if (AutoWifiController.this.mListener != null) {
                AutoWifiController.this.mListener.onAutoWifiStateChanged(false);
            }
            int wifiState = AutoWifiController.this.mAdapter.getWifiEnabledState();
            if (wifiState == 1 || wifiState == 0) {
                Log.i(AutoWifiController.TAG, "Auto Wi-Fi Disabled, deinitialize geofence");
                AutoWifiController.this.mAdapter.requestGeofenceEnabled(false, AutoWifiController.AUTO_WIFI_PACKAGE_NAME);
            }
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 5) {
                if (i != 7) {
                    if (i != 8 && i != 14 && i != 15) {
                        return false;
                    }
                    Log.d(AutoWifiController.TAG, "Auto Wi-Fi disabled");
                } else if (AutoWifiController.this.getAutoWifiEnabledFromProvider()) {
                    boolean unused = AutoWifiController.this.mDoNotUpdateAlwaysWifiOnUserFlag = true;
                    if (!AutoWifiController.this.checkDeviceStatusInIdle()) {
                        Log.i(AutoWifiController.TAG, "can't enable Auto Wi-Fi");
                    } else {
                        int wifiState = AutoWifiController.this.mAdapter.getWifiEnabledState();
                        if (AutoWifiController.this.mLastGeofenceState == 1) {
                            if (wifiState == 3 || wifiState == 2 || AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                                NetworkInfo networkInfo = AutoWifiController.this.mAdapter.getNetworkInfo();
                                if (networkInfo == null || networkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
                                    AutoWifiController autoWifiController = AutoWifiController.this;
                                    autoWifiController.transitionTo(autoWifiController.mWifiOnState);
                                } else {
                                    AutoWifiController autoWifiController2 = AutoWifiController.this;
                                    autoWifiController2.transitionTo(autoWifiController2.mWifiConnectedState);
                                }
                            } else {
                                AutoWifiController.this.mScanResultMatcher.setUpdateFlag();
                                if (AutoWifiController.this.mScanResultMatcher.getFavoriteNetworkCount() <= 0) {
                                    AutoWifiController.this.setWifiStateDisableOrScanMode();
                                    AutoWifiController autoWifiController3 = AutoWifiController.this;
                                    autoWifiController3.transitionTo(autoWifiController3.mUserTriggeredWifiOffState);
                                } else if (AutoWifiController.this.setWifiStateScanMode()) {
                                    AutoWifiController autoWifiController4 = AutoWifiController.this;
                                    autoWifiController4.transitionTo(autoWifiController4.mScanModeState);
                                } else {
                                    AutoWifiController autoWifiController5 = AutoWifiController.this;
                                    autoWifiController5.transitionTo(autoWifiController5.mUserTriggeredWifiOffState);
                                }
                            }
                        } else if (wifiState == 3 || wifiState == 2 || AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                            AutoWifiController autoWifiController6 = AutoWifiController.this;
                            autoWifiController6.transitionTo(autoWifiController6.mUserTriggeredWifiOnState);
                        } else {
                            AutoWifiController.this.transitWifiOffState(true);
                        }
                    }
                } else {
                    Log.i(AutoWifiController.TAG, "Auto Wi-Fi option : off");
                }
            } else if (msg.arg2 == 0) {
                AutoWifiController.this.mAutoWifiNotificationController.setGeofenceStateExit();
            }
            return true;
        }

        public void exit() {
            AutoWifiController.this.obtainScanAlwaysAvailablePolicy(true);
            AutoWifiController.this.mAutoWifiNotificationController.setEnable(true);
            if (AutoWifiController.this.mListener != null) {
                AutoWifiController.this.mListener.onAutoWifiStateChanged(true);
            }
            Log.i(AutoWifiController.TAG, "Auto Wi-Fi Enabled, initialize geofence");
            AutoWifiController.this.mAdapter.requestGeofenceEnabled(true, AutoWifiController.AUTO_WIFI_PACKAGE_NAME);
            AutoWifiController autoWifiController = AutoWifiController.this;
            boolean unused = autoWifiController.mEnableSmartSwitch = autoWifiController.getSamrtSwitchModeFromProvider();
        }
    }

    class FavoriteGeofenceEnterState extends State {
        FavoriteGeofenceEnterState() {
        }

        public void enter() {
            long unused = AutoWifiController.this.mLastUpdatedGeofenceExitTime = 0;
            if (AutoWifiController.this.mDoNotUpdateAlwaysWifiOnUserFlag) {
                if (AutoWifiController.DBG) {
                    Log.d(AutoWifiController.TAG, "do not update alaways Wi-Fi on user flag");
                }
                boolean unused2 = AutoWifiController.this.mDoNotUpdateAlwaysWifiOnUserFlag = false;
            } else if (AutoWifiController.this.mLastGeofenceState == 1) {
                if (AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                    boolean unused3 = AutoWifiController.this.mAlwaysWifiOnUser = true;
                } else {
                    boolean unused4 = AutoWifiController.this.mAlwaysWifiOnUser = false;
                }
                AutoWifiController autoWifiController = AutoWifiController.this;
                autoWifiController.setAlwaysWifiOnUserToDb(autoWifiController.mAlwaysWifiOnUser);
            }
            AutoWifiController.this.registerScanListener();
            Log.d(AutoWifiController.TAG, "Geofence Enter with AlwaysWifiOnUser:" + AutoWifiController.this.mAlwaysWifiOnUser);
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 1) {
                if (i != 2) {
                    if (i != 5) {
                        if (i != 6) {
                            if (i != 12) {
                                if (i != 13) {
                                    return false;
                                }
                                if (AutoWifiController.this.mLastUpdatedGeofenceExitTime != 0 && SystemClock.elapsedRealtime() - AutoWifiController.this.mLastUpdatedGeofenceExitTime >= AutoWifiController.this.mMinDurationToTransitGeofenceExitState) {
                                    Log.i(AutoWifiController.TAG, "geofence exit, AlwaysWifiOnUser:" + AutoWifiController.this.mAlwaysWifiOnUser + ", lastConnectedNetworkId:" + AutoWifiController.this.mLastConnectedNetworkId);
                                    AutoWifiController.this.mAutoWifiNotificationController.setGeofenceStateExit();
                                    if (AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                                        if (!AutoWifiController.this.mAlwaysWifiOnUser && !AutoWifiController.this.isActivePeerConnection() && AutoWifiController.this.mScanResultMatcher.getFavoriteNetworkCount() > 0) {
                                            AutoWifiController autoWifiController = AutoWifiController.this;
                                            if (autoWifiController.isFavoriteAp(autoWifiController.mLastConnectedNetworkId) || AutoWifiController.this.mLastConnectedDuration < AutoWifiController.UNINTENDED_AP_CONNECTION_TIME) {
                                                Log.i(AutoWifiController.TAG, "Wi-Fi turnning off because last connected network is favorite AP");
                                                AutoWifiController.this.transitWifiOffState(true);
                                            }
                                        }
                                        AutoWifiController autoWifiController2 = AutoWifiController.this;
                                        autoWifiController2.transitionTo(autoWifiController2.mUserTriggeredWifiOnState);
                                    } else {
                                        AutoWifiController.this.transitWifiOffState(true);
                                    }
                                }
                            } else if (AutoWifiController.this.mLastGeofenceState == 0) {
                                AutoWifiController.this.sendMessage(obtainGeofenceExitTimerMessage());
                            }
                        } else if (msg.arg1 == 1) {
                            AutoWifiController autoWifiController3 = AutoWifiController.this;
                            autoWifiController3.transitionTo(autoWifiController3.mWifiConnectedState);
                        }
                    } else if (msg.arg1 == 0) {
                        if (AutoWifiController.this.mLastUpdatedGeofenceExitTime == 0) {
                            long unused = AutoWifiController.this.mLastUpdatedGeofenceExitTime = SystemClock.elapsedRealtime();
                        } else if (SystemClock.elapsedRealtime() - AutoWifiController.this.mLastUpdatedGeofenceExitTime > AutoWifiController.this.mMinDurationToTransitGeofenceExitState) {
                            AutoWifiController.this.removeMessages(13);
                            AutoWifiController.this.sendMessage(obtainGeofenceExitTimerMessage());
                        }
                        Log.i(AutoWifiController.TAG, "set geofence exit timer " + AutoWifiController.this.mLastUpdatedGeofenceExitTime);
                        AutoWifiController.this.sendMessageDelayed(obtainGeofenceExitTimerMessage(), AutoWifiController.this.mMinDurationToTransitGeofenceExitState);
                    } else {
                        long unused2 = AutoWifiController.this.mLastUpdatedGeofenceExitTime = 0;
                        AutoWifiController.this.removeMessages(13);
                    }
                } else if (AutoWifiController.this.mLastGeofenceState == 0) {
                    AutoWifiController.this.transitWifiOffState(false);
                } else {
                    if (!AutoWifiController.this.mDoNotUpdateGeofenceExitDelay) {
                        if (AutoWifiController.this.getCurrentState() == AutoWifiController.this.mWifiConnectedState) {
                            long unused3 = AutoWifiController.this.mMinDurationToTransitGeofenceExitState = 60000;
                        } else {
                            long unused4 = AutoWifiController.this.mMinDurationToTransitGeofenceExitState = 10000;
                        }
                    }
                    if (msg.arg1 == 1) {
                        Log.d(AutoWifiController.TAG, "Wi-Fi state changed by white packages");
                        if (AutoWifiController.this.setWifiStateScanMode()) {
                            AutoWifiController autoWifiController4 = AutoWifiController.this;
                            autoWifiController4.transitionTo(autoWifiController4.mScanModeState);
                        }
                    } else {
                        AutoWifiController autoWifiController5 = AutoWifiController.this;
                        autoWifiController5.transitionTo(autoWifiController5.mUserTriggeredWifiOffState);
                    }
                }
            } else if (AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                int unused5 = AutoWifiController.this.mLastConnectedNetworkId = -1;
                long unused6 = AutoWifiController.this.mLastConnectedDuration = 0;
                AutoWifiController autoWifiController6 = AutoWifiController.this;
                autoWifiController6.transitionTo(autoWifiController6.mWifiOnState);
            }
            return true;
        }

        private Message obtainGeofenceExitTimerMessage() {
            return AutoWifiController.this.obtainMessage(13, (int) (AutoWifiController.this.mLastUpdatedGeofenceExitTime / 1000), ((int) (AutoWifiController.this.mMinDurationToTransitGeofenceExitState / 1000)) + 50);
        }
    }

    class ScanModeState extends State {
        private int mScanCounter = 0;
        private final int[] mScanIntervalArr = {8, 16, 32, 64, 128};

        ScanModeState() {
        }

        public void enter() {
            this.mScanCounter = 0;
            AutoWifiController.this.mScanResultMatcher.setUpdateFlag();
            if (AutoWifiController.this.mLastScreenState && AutoWifiController.this.mLastGeofenceState == 1) {
                startScanPoll();
            }
            boolean unused = AutoWifiController.this.mAlwaysWifiOnUser = false;
            AutoWifiController autoWifiController = AutoWifiController.this;
            autoWifiController.setAlwaysWifiOnUserToDb(autoWifiController.mAlwaysWifiOnUser);
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 2) {
                if (i != 4) {
                    if (i == 5) {
                        if (msg.arg1 == 1) {
                            this.mScanCounter = 0;
                            startScanPoll();
                        } else {
                            stopScanPoll();
                        }
                        return false;
                    } else if (i != 11) {
                        if (i != 12) {
                            return false;
                        }
                        if (msg.arg1 == 1) {
                            this.mScanCounter = 0;
                            startScanPoll();
                            AutoWifiController.this.mAdapter.startScan(AutoWifiController.AUTO_WIFI_PACKAGE_NAME);
                        } else {
                            stopScanPoll();
                        }
                        return false;
                    } else if (AutoWifiController.this.mScanResultMatcher.getFavoriteNetworkCount() > 0) {
                        if (AutoWifiController.this.mScanResultMatcher.getGoefenceEnteredNetworkCount() > 0) {
                            AutoWifiController.this.mAdapter.startScan(AutoWifiController.AUTO_WIFI_PACKAGE_NAME);
                        } else {
                            Log.i(AutoWifiController.TAG, "not exist geofence entered Wi-Fi networks. skip scan");
                        }
                        this.mScanCounter++;
                        startScanPoll();
                    } else {
                        Log.i(AutoWifiController.TAG, "Skip scan, There is no saved favorite Wi-Fi networks. Transition to Wi-Fi off");
                        AutoWifiController.this.setWifiStateDisableOrScanMode();
                        AutoWifiController autoWifiController = AutoWifiController.this;
                        autoWifiController.transitionTo(autoWifiController.mUserTriggeredWifiOffState);
                    }
                } else if (AutoWifiController.this.mScanResultMatcher.isFoundConnectableApInScanResult()) {
                    if (AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_ON)) {
                        AutoWifiController autoWifiController2 = AutoWifiController.this;
                        autoWifiController2.transitionTo(autoWifiController2.mWifiOnState);
                    } else {
                        Log.e(AutoWifiController.TAG, "Can't change Wi-Fi state");
                    }
                }
            }
            return true;
        }

        private void stopScanPoll() {
            this.mScanCounter = 0;
            AutoWifiController.this.removeMessages(11);
        }

        private void startScanPoll() {
            AutoWifiController.this.removeMessages(11);
            long unused = AutoWifiController.this.mNextScanInterval = getNextScanInterval(this.mScanCounter);
            AutoWifiController autoWifiController = AutoWifiController.this;
            autoWifiController.sendMessageDelayed(11, autoWifiController.mNextScanInterval);
        }

        private long getNextScanInterval(int count) {
            if (count >= 0) {
                int[] iArr = this.mScanIntervalArr;
                if (count <= iArr.length - 1) {
                    return (long) (iArr[count] * 1000);
                }
            }
            return AutoWifiController.this.mPeriodicScanMaxInterval;
        }

        public void exit() {
            stopScanPoll();
        }
    }

    class WifiOnState extends State {
        public int mScanReceiverCount = 0;
        private long mStateEnterTime = 0;

        WifiOnState() {
        }

        public void enter() {
            resetWifiOffTransitionScanCounter();
            resetWifiOffTransitionTime();
            NetworkInfo networkInfo = AutoWifiController.this.mAdapter.getNetworkInfo();
            if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                AutoWifiController autoWifiController = AutoWifiController.this;
                autoWifiController.transitionTo(autoWifiController.mWifiConnectedState);
            }
            AutoWifiController autoWifiController2 = AutoWifiController.this;
            autoWifiController2.setScanAlwaysAvailable(autoWifiController2.getAlawaysAllowScanModeFromProvider());
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 1) {
                boolean z = false;
                if (i == 12) {
                    if (msg.arg1 == 1) {
                        resetWifiOffTransitionScanCounter();
                    }
                    return false;
                } else if (i == 3) {
                    AutoWifiController autoWifiController = AutoWifiController.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    autoWifiController.setScanAlwaysAvailable(z);
                } else if (i != 4) {
                    if (i != 5 || msg.arg1 != 0 || !AutoWifiController.SETTINGS_PACKAGE_NAME.equals(AutoWifiController.this.getForegroundPackageName()) || !AutoWifiController.this.mLastScreenState) {
                        return false;
                    }
                    if (AutoWifiController.DBG) {
                        Log.i(AutoWifiController.TAG, "Ignore to change Wi-Fi state off, because settings activity is showing");
                    }
                } else if (AutoWifiController.this.getCurrentState() == AutoWifiController.this.mWifiConnectedState || AutoWifiController.this.mScanResultMatcher.isFoundConnectableApInScanResult()) {
                    resetWifiOffTransitionScanCounter();
                } else {
                    Log.i(AutoWifiController.TAG, "received scan result but not found connectable ap, AlwaysWifiOnUser:" + AutoWifiController.this.mAlwaysWifiOnUser);
                    if (!AutoWifiController.this.mAlwaysWifiOnUser && AutoWifiController.this.mScanResultMatcher.getFavoriteNetworkCount() > 0) {
                        increaseScoreAndTranstion();
                    }
                }
            }
            return true;
        }

        private void resetWifiOffTransitionScanCounter() {
            this.mScanReceiverCount = 0;
        }

        public void resetWifiOffTransitionTime() {
            this.mStateEnterTime = SystemClock.elapsedRealtime();
        }

        private void increaseScoreAndTranstion() {
            if (AutoWifiController.this.mLastConnectedNetworkId == -1) {
                Log.d(AutoWifiController.TAG, "never connect any network after Wi-Fi state changed");
            } else if (AutoWifiController.this.isActivePeerConnection()) {
                Log.d(AutoWifiController.TAG, "peer connection is activated, do not change Wi-Fi state");
            } else {
                long minDuration = AutoWifiController.this.mMinDurationToTransitWifiOffState;
                if (AutoWifiController.this.mEnableSmartSwitch && minDuration >= AutoWifiController.MAX_DURATION_TIME) {
                    minDuration /= 2;
                }
                int i = this.mScanReceiverCount + 1;
                this.mScanReceiverCount = i;
                if (((long) i) > AutoWifiController.this.mMinScanResultCountToTransitWifiOffState) {
                    AutoWifiController autoWifiController = AutoWifiController.this;
                    if (!autoWifiController.isFavoriteAp(autoWifiController.mLastConnectedNetworkId) && AutoWifiController.this.mLastConnectedDuration >= AutoWifiController.UNINTENDED_AP_CONNECTION_TIME) {
                        if (AutoWifiController.DBG) {
                            Log.i(AutoWifiController.TAG, "last connected network is not favorite AP, reset scan counter");
                        }
                        this.mScanReceiverCount = 0;
                    } else if (AutoWifiController.SETTINGS_PACKAGE_NAME.equals(AutoWifiController.this.getForegroundPackageName())) {
                        if (AutoWifiController.DBG) {
                            Log.i(AutoWifiController.TAG, "settings activity is showing, reset scan counter");
                        }
                        this.mScanReceiverCount = 0;
                    } else if (SystemClock.elapsedRealtime() - this.mStateEnterTime > minDuration && AutoWifiController.this.setWifiStateScanMode()) {
                        AutoWifiController autoWifiController2 = AutoWifiController.this;
                        autoWifiController2.transitionTo(autoWifiController2.mScanModeState);
                    }
                }
            }
        }
    }

    class WifiConnectedState extends State {
        private boolean mGeofenceInitialized = false;
        private long mTimeAtConnectStart = 0;

        WifiConnectedState() {
        }

        public void enter() {
            long unused = AutoWifiController.this.mLastUpdatedGeofenceExitTime = 0;
            this.mTimeAtConnectStart = SystemClock.elapsedRealtime();
            if (!this.mGeofenceInitialized) {
                checkGeofenceManagerInitialized();
            }
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 5) {
                if (i == 6) {
                    if (msg.arg1 == 0) {
                        AutoWifiController.this.mWifiOnState.resetWifiOffTransitionTime();
                        AutoWifiController autoWifiController = AutoWifiController.this;
                        autoWifiController.transitionTo(autoWifiController.mWifiOnState);
                    }
                    return true;
                } else if (i != 13) {
                    return false;
                }
            }
            String str = "initialized";
            if (AutoWifiController.this.mLastGeofenceState == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("geofence state is wrong, geofence ");
                if (!this.mGeofenceInitialized) {
                    str = "not initialized";
                }
                sb.append(str);
                Log.e(AutoWifiController.TAG, sb.toString());
                return true;
            }
            if (!this.mGeofenceInitialized) {
                checkGeofenceManagerInitialized();
                StringBuilder sb2 = new StringBuilder();
                sb2.append("geofence ");
                if (!this.mGeofenceInitialized) {
                    str = "not initialized";
                }
                sb2.append(str);
                Log.i(AutoWifiController.TAG, sb2.toString());
            }
            return false;
        }

        public void exit() {
            long unused = AutoWifiController.this.mLastConnectedDuration = SystemClock.elapsedRealtime() - this.mTimeAtConnectStart;
        }

        private void checkGeofenceManagerInitialized() {
            List<String> configKeys = AutoWifiController.this.mAdapter.getGeofenceEnterKeys();
            if (configKeys == null || configKeys.isEmpty()) {
                this.mGeofenceInitialized = false;
            } else {
                this.mGeofenceInitialized = true;
            }
        }
    }

    class UserTriggeredWifiOffState extends State {
        UserTriggeredWifiOffState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i == 2) {
                return true;
            }
            if (i == 3) {
                AutoWifiController.this.setWifiStateDisableOrScanMode();
                return true;
            } else if (i != 4) {
                return false;
            } else {
                if (AutoWifiController.this.mLastGeofenceState != 0) {
                    return true;
                }
                AutoWifiController.this.sendMessage(13);
                return true;
            }
        }
    }

    class GeofenceExitState extends State {
        GeofenceExitState() {
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 1) {
                if (i != 5) {
                    return false;
                }
                if (msg.arg1 == 1) {
                    int wifiState = AutoWifiController.this.mAdapter.getWifiEnabledState();
                    if (wifiState == 3 || wifiState == 2 || AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                        NetworkInfo networkInfo = AutoWifiController.this.mAdapter.getNetworkInfo();
                        if (networkInfo == null || networkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED) {
                            AutoWifiController autoWifiController = AutoWifiController.this;
                            autoWifiController.transitionTo(autoWifiController.mWifiOnState);
                        } else {
                            AutoWifiController autoWifiController2 = AutoWifiController.this;
                            autoWifiController2.transitionTo(autoWifiController2.mWifiConnectedState);
                        }
                    } else if (AutoWifiController.this.setWifiStateScanMode()) {
                        AutoWifiController autoWifiController3 = AutoWifiController.this;
                        autoWifiController3.transitionTo(autoWifiController3.mScanModeState);
                    }
                } else if (msg.arg2 == 0) {
                    AutoWifiController.this.mAutoWifiNotificationController.setGeofenceStateExit();
                }
            } else if (AutoWifiController.this.mAdapter.isWifiToggleEnabled()) {
                AutoWifiController autoWifiController4 = AutoWifiController.this;
                autoWifiController4.transitionTo(autoWifiController4.mUserTriggeredWifiOnState);
            }
            return true;
        }
    }

    class WifiOffState extends State {
        WifiOffState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i != 2) {
                if (i != 3) {
                    if (i != 9) {
                        return false;
                    }
                    if (AutoWifiController.this.getAlawaysAllowScanModeFromProvider()) {
                        boolean unused = AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_SCANMODE);
                        AutoWifiController autoWifiController = AutoWifiController.this;
                        autoWifiController.transitionTo(autoWifiController.mWifiOffWithScanModeState);
                    }
                } else if (msg.arg1 == 1 && AutoWifiController.this.isLocationEnabled()) {
                    boolean unused2 = AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_SCANMODE);
                    AutoWifiController autoWifiController2 = AutoWifiController.this;
                    autoWifiController2.transitionTo(autoWifiController2.mWifiOffWithScanModeState);
                }
            }
            return true;
        }

        private boolean allowWifiStateChange() {
            if (!AutoWifiController.this.getAirplainModeEnabledFromProvider() && !AutoWifiController.this.getUltraPowerSaveEnabledFromProvider()) {
                return true;
            }
            return false;
        }
    }

    class WifiOffWithScanModeState extends State {
        WifiOffWithScanModeState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            if (i == 2) {
                return true;
            }
            if (i != 3) {
                if (i == 4) {
                    return true;
                }
                if (i != 10) {
                    return false;
                }
                boolean unused = AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_OFF);
                AutoWifiController autoWifiController = AutoWifiController.this;
                autoWifiController.transitionTo(autoWifiController.mWifiOffState);
                return true;
            } else if (msg.arg1 != 0) {
                return true;
            } else {
                boolean unused2 = AutoWifiController.this.setWifiControllerState(WifiState.WIFI_STATE_OFF);
                AutoWifiController autoWifiController2 = AutoWifiController.this;
                autoWifiController2.transitionTo(autoWifiController2.mWifiOffState);
                return true;
            }
        }
    }

    class UserTriggeredWifiOnState extends State {
        UserTriggeredWifiOnState() {
        }

        public void enter() {
            AutoWifiController autoWifiController = AutoWifiController.this;
            autoWifiController.setScanAlwaysAvailable(autoWifiController.getAlawaysAllowScanModeFromProvider());
        }

        public boolean processMessage(Message msg) {
            WifiConfiguration config;
            AutoWifiController.this.logStateAndMessage(msg, this);
            int i = msg.what;
            boolean z = false;
            if (i == 2) {
                AutoWifiController.this.transitWifiOffState(false);
            } else if (i == 3) {
                AutoWifiController autoWifiController = AutoWifiController.this;
                if (msg.arg1 == 1) {
                    z = true;
                }
                autoWifiController.setScanAlwaysAvailable(z);
            } else if (i != 5) {
                if (i != 6) {
                    if (i != 17) {
                        return false;
                    }
                    if (!(AutoWifiController.this.mLastConnectedNetworkId == -1 || (config = AutoWifiController.this.mAdapter.getSpecificNetwork(AutoWifiController.this.mLastConnectedNetworkId)) == null || !AutoWifiController.this.isFavoriteAp(config))) {
                        Log.i(AutoWifiController.TAG, "las network was favorite AP. re-enter geofence state");
                        AutoWifiController.this.forceUpdateGeofenceState(true, config.configKey());
                        AutoWifiController autoWifiController2 = AutoWifiController.this;
                        autoWifiController2.transitionTo(autoWifiController2.mWifiOnState);
                    }
                } else if (msg.arg1 == 1) {
                    Log.d(AutoWifiController.TAG, "wifi Connect event on UserTriggeredWifiOnState");
                    AutoWifiController autoWifiController3 = AutoWifiController.this;
                    autoWifiController3.sendMessageDelayed(autoWifiController3.obtainMessage(17, 0, 0), 15000);
                }
            } else if (msg.arg1 == 1) {
                AutoWifiController autoWifiController4 = AutoWifiController.this;
                autoWifiController4.transitionTo(autoWifiController4.mWifiOnState);
            } else if (msg.arg2 == 0) {
                AutoWifiController.this.mAutoWifiNotificationController.setGeofenceStateExit();
            }
            return true;
        }
    }

    private class ScanResultMatcher {
        private Context mContext;
        private ArrayList<String> mFavoriteNetworkKeys = new ArrayList<>();
        private ArrayList<String> mGeofenceEnterNetworkKeys = new ArrayList<>();
        private Object mLockFavoriteNetworkKeys = new Object();
        private boolean mNeedToUpdate = true;

        ScanResultMatcher(Context context) {
            this.mContext = context;
        }

        /* access modifiers changed from: package-private */
        public String dump() {
            StringBuffer sb = new StringBuffer();
            sb.append("GeofenceEnterNetworkKeys: ");
            sb.append(this.mGeofenceEnterNetworkKeys);
            sb.append("\n");
            sb.append("FavoriteNetworkKeys: ");
            sb.append(this.mFavoriteNetworkKeys);
            sb.append("\n");
            return sb.toString();
        }

        /* access modifiers changed from: package-private */
        public void setUpdateFlag() {
            this.mNeedToUpdate = true;
        }

        /* access modifiers changed from: package-private */
        public int getGoefenceEnteredNetworkCount() {
            return this.mGeofenceEnterNetworkKeys.size();
        }

        /* access modifiers changed from: package-private */
        public int getFavoriteNetworkCount() {
            if (this.mNeedToUpdate) {
                List<WifiConfiguration> configs = AutoWifiController.this.mAdapter.getConfiguredNetworks();
                if (configs == null || configs.size() <= 0) {
                    int backupSize = getAutoWifiFavoriteApCountFromDb();
                    Log.i(AutoWifiController.TAG, "config size is zero, return db value " + backupSize);
                    return backupSize;
                }
                updateFavoriteNetworkKey(configs);
                this.mNeedToUpdate = false;
            }
            return this.mFavoriteNetworkKeys.size();
        }

        /* access modifiers changed from: package-private */
        public int updateFavoriteNetworkKey(List<WifiConfiguration> configs) {
            int size = 0;
            if (configs != null) {
                synchronized (this.mLockFavoriteNetworkKeys) {
                    this.mFavoriteNetworkKeys.clear();
                    for (WifiConfiguration config : configs) {
                        if (AutoWifiController.isTargetAp(config)) {
                            if (AutoWifiController.this.isFavoriteAp(config)) {
                                this.mFavoriteNetworkKeys.add(config.configKey());
                            }
                        }
                    }
                }
                size = this.mFavoriteNetworkKeys.size();
                setAutoWifiFavoriteApCountToDb(size);
            }
            Log.i(AutoWifiController.TAG, "updateFavoriteNetworkKey count: " + size);
            return size;
        }

        private int getAutoWifiFavoriteApCountFromDb() {
            int value = Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_favorite_ap_count", 0);
            if (AutoWifiController.DBG) {
                Log.d(AutoWifiController.TAG, "getAutoWifiFavoriteApCountFromDb " + value);
            }
            return value;
        }

        private void setAutoWifiFavoriteApCountToDb(int count) {
            if (AutoWifiController.DBG) {
                Log.d(AutoWifiController.TAG, "setAutoWifiFavoriteApCountToDb " + count);
            }
            Settings.Global.putInt(this.mContext.getContentResolver(), "sem_auto_wifi_favorite_ap_count", count);
        }

        /* access modifiers changed from: package-private */
        public boolean isFoundConnectableApInScanResult() {
            List<ScanResult> scanResults = AutoWifiController.this.mAdapter.getScanResults();
            if (scanResults != null && getFavoriteNetworkCount() > 0) {
                for (ScanResult scanItem : scanResults) {
                    if (scanItem != null) {
                        String bssid = scanItem.BSSID;
                        if (bssid == null) {
                            Log.d(AutoWifiController.TAG, "unknown bssid - ssid:" + scanItem.SSID);
                        } else {
                            for (String key : AutoWifiController.getConfigKeyFrom(scanItem)) {
                                if (key != null && this.mFavoriteNetworkKeys.contains(key)) {
                                    if (AutoWifiController.this.mExceptNetworkKeys.contains(key)) {
                                        if (AutoWifiController.DBG) {
                                            Log.d(AutoWifiController.TAG, "except ap - key:" + key);
                                        }
                                    } else if (!this.mGeofenceEnterNetworkKeys.contains(key)) {
                                        Log.i(AutoWifiController.TAG, "not in range of favorite ap area - key:" + key);
                                    } else if (AutoWifiController.this.mAdapter.isUnstableAp(bssid)) {
                                        Log.i(AutoWifiController.TAG, "unstable ap - key:" + key + ", bssid:" + bssid);
                                    } else {
                                        Log.i(AutoWifiController.TAG, "Found saved network in scan result - key:" + key + ", bssid:" + bssid);
                                        return true;
                                    }
                                }
                            }
                            continue;
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Not found saved Wi-Fi network in scan result ");
            sb.append(scanResults == null ? "null" : String.valueOf(scanResults.size()));
            Log.i(AutoWifiController.TAG, sb.toString());
            return false;
        }

        /* access modifiers changed from: private */
        public boolean isKTHomeAP(String bssid) {
            List<ScanResult> scanResults = AutoWifiController.this.mAdapter.getScanResults();
            if (scanResults != null) {
                for (ScanResult scanItem : scanResults) {
                    if (scanItem != null && scanItem.BSSID != null && bssid.equals(scanItem.BSSID) && scanItem.capabilities.contains("[KTH]")) {
                        Log.d(AutoWifiController.TAG, "This AP is KT Home AP.");
                        return true;
                    }
                }
                return false;
            }
            Log.d(AutoWifiController.TAG, "scanResults is null");
            return false;
        }

        /* access modifiers changed from: package-private */
        public void updateGeofenceEnterNetwork(List<String> configKeys) {
            synchronized (this.mLockFavoriteNetworkKeys) {
                this.mGeofenceEnterNetworkKeys.clear();
                for (String key : configKeys) {
                    if (this.mFavoriteNetworkKeys.contains(key)) {
                        this.mGeofenceEnterNetworkKeys.add(key);
                    }
                }
            }
        }

        /* access modifiers changed from: package-private */
        public boolean hasFavoriteApInGeofenceArea() {
            if (this.mGeofenceEnterNetworkKeys.isEmpty()) {
                return false;
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public void removeNetwork(String networkKey) {
            if (this.mGeofenceEnterNetworkKeys.contains(networkKey)) {
                this.mGeofenceEnterNetworkKeys.remove(networkKey);
            }
        }

        /* access modifiers changed from: package-private */
        public void copyFavoriteNetworksToGeofenceEnterNetworkList() {
            synchronized (this.mLockFavoriteNetworkKeys) {
                this.mGeofenceEnterNetworkKeys.clear();
                Iterator<String> it = this.mFavoriteNetworkKeys.iterator();
                while (it.hasNext()) {
                    this.mGeofenceEnterNetworkKeys.add(it.next());
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isFavoriteAp(int networkId) {
        Log.d(TAG, "isFavoriteAp - networkId : " + networkId);
        if (networkId != -1) {
            return isFavoriteAp(this.mAdapter.getSpecificNetwork(networkId));
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isFavoriteAp(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        Log.d(TAG, "isFavoriteAp " + config.configKey() + ":" + config.semAutoWifiScore);
        if (config.semAutoWifiScore > 98) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public static boolean isTargetAp(WifiConfiguration config) {
        if (config.semIsVendorSpecificSsid) {
            if (DBG) {
                Log.d(TAG, "it's vendor ap netId:" + config.networkId);
            }
            return false;
        } else if (config.isPasspoint()) {
            if (DBG) {
                Log.d(TAG, "it's passpoint ap netId:" + config.networkId);
            }
            return false;
        } else if (config.semSamsungSpecificFlags.get(1)) {
            if (DBG) {
                Log.d(TAG, "it's Samsung Mobile Hotspot netId:" + config.networkId);
            }
            return false;
        } else if (config.semAutoReconnect != 0) {
            return true;
        } else {
            if (DBG) {
                Log.d(TAG, "it's not allowing auto reconnect ap netId:" + config.networkId);
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public static String[] getConfigKeyFrom(ScanResult result) {
        String ssid = "\"" + result.SSID + "\"";
        if (result.capabilities.contains("WAPI-PSK")) {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[22]};
        } else if (result.capabilities.contains("WAPI-CERT")) {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[23]};
        } else if (result.capabilities.contains("WEP")) {
            return new String[]{ssid + "WEP"};
        } else if (mSupportWpa3Sae && result.capabilities.contains("SAE")) {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[8]};
        } else if (result.capabilities.contains("PSK")) {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[1]};
        } else if (result.capabilities.contains("EAP")) {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[2]};
        } else {
            return new String[]{ssid + WifiConfiguration.KeyMgmt.strings[0]};
        }
    }

    private static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    /* access modifiers changed from: private */
    public String getForegroundPackageName() {
        if (this.mActivityManager == null) {
            this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        }
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager == null) {
            return null;
        }
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            return new String(tasks.get(0).topActivity.getPackageName());
        }
        return null;
    }

    public String getDebugString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FW Version:");
        sb.append(VERSION);
        sb.append("\n");
        sb.append("CurrentState:");
        sb.append(getCurrentState().getName());
        sb.append("\n");
        sb.append("LastGeofenceState:");
        sb.append(getGeofenceStateString(this.mLastGeofenceState));
        sb.append("\n");
        sb.append("Geofence Control Mode:");
        sb.append(this.mManualGeofenceControl ? "manual" : "auto");
        sb.append(" mode\n");
        sb.append("Wi-Fi OFF Transtion Min.ScanResult Count:");
        sb.append(this.mWifiOnState.mScanReceiverCount);
        sb.append("/");
        sb.append(this.mMinScanResultCountToTransitWifiOffState);
        sb.append("\n");
        sb.append("Wi-Fi OFF Transtion Min.Time:");
        sb.append(this.mMinDurationToTransitWifiOffState / 1000);
        sb.append(" seconds\n");
        if (this.mEnableSmartSwitch && this.mMinDurationToTransitWifiOffState >= MAX_DURATION_TIME) {
            sb.append(" - SNS is enabled, apply half of transition Min.Time\n");
        }
        sb.append("Max Scan Interval:");
        sb.append(this.mNextScanInterval / 1000);
        sb.append("/");
        sb.append(this.mPeriodicScanMaxInterval / 1000);
        sb.append(" seconds\n");
        sb.append("Max cell count:");
        sb.append(this.mMaxCellIdCount);
        sb.append("\n");
        sb.append("Cell count of ");
        sb.append("networkId:");
        sb.append(this.mLastReportedNetworkIdForCellCount);
        sb.append(" is ");
        sb.append(this.mLastReportedCellCount);
        sb.append("\n");
        sb.append("Max bssid count:");
        sb.append(this.mMaxBssidWhiteListSize);
        sb.append("\n");
        sb.append("Geofence Exit Transition Min.Time:");
        sb.append(this.mMinDurationToTransitGeofenceExitState / 1000);
        sb.append(" seconds\n");
        sb.append("Goefnece Exit Received at:");
        if (this.mLastUpdatedGeofenceExitTime != 0) {
            sb.append((SystemClock.elapsedRealtime() - this.mLastUpdatedGeofenceExitTime) / 1000);
        } else {
            sb.append("0");
        }
        sb.append(" seconds before\n");
        sb.append("Last connected network id:");
        sb.append(this.mLastConnectedNetworkId);
        sb.append(", duration:");
        sb.append(this.mLastConnectedDuration / 1000);
        sb.append(" seconds\n");
        sb.append("Always On Wi-Fi User: ");
        sb.append(this.mAlwaysWifiOnUser);
        sb.append("\n");
        sb.append(this.mScanResultMatcher.dump());
        sb.append(this.mAutoWifiNotificationController.dump());
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        AutoWifiController.super.dump(fd, pw, args);
        pw.println(getDebugString());
    }

    private class AllSingleScanListener implements WifiScanner.ScanListener {
        private AllSingleScanListener() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason, String description) {
        }

        public void onPeriodChanged(int periodInMs) {
        }

        public void onResults(WifiScanner.ScanData[] results) {
            AutoWifiController.this.sendMessage(4);
        }

        public void onFullResult(ScanResult fullScanResult) {
        }
    }
}
