package com.android.server.wifi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.ScanOnlyModeManager;
import com.android.server.wifi.WifiController;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiController extends StateMachine {
    private static final int BASE = 155648;
    static final int CMD_AIRPLANE_TOGGLED = 155657;
    static final int CMD_AP_START_FAILURE = 155661;
    static final int CMD_AP_STOPPED = 155663;
    static final int CMD_DEFERRED_RECOVERY_RESTART_WIFI = 155672;
    static final int CMD_DEFERRED_TOGGLE = 155659;
    static final int CMD_EMERGENCY_CALL_STATE_CHANGED = 155662;
    static final int CMD_EMERGENCY_MODE_CHANGED = 155649;
    static final int CMD_RECOVERY_DISABLE_WIFI = 155667;
    static final int CMD_RECOVERY_RESTART_WIFI = 155665;
    private static final int CMD_RECOVERY_RESTART_WIFI_CONTINUE = 155666;
    public static final int CMD_RESET_AP = 155670;
    static final int CMD_SCANNING_STOPPED = 155669;
    static final int CMD_SCAN_ALWAYS_MODE_CHANGED = 155655;
    static final int CMD_SET_AP = 155658;
    public static final int CMD_SET_AP_BOOSTER_FLAG = 155671;
    static final int CMD_SHUTDOWN = 155673;
    static final int CMD_STA_START_FAILURE = 155664;
    static final int CMD_STA_STOPPED = 155668;
    static final int CMD_WIFI_TOGGLED = 155656;
    /* access modifiers changed from: private */
    public static final String CSC_CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDINGFORMOBILEAP, "ALL");
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final long DEFAULT_REENABLE_DELAY_MS = 500;
    private static final long DEFER_MARGIN_MS = 5;
    /* access modifiers changed from: private */
    public static final boolean ENBLE_WLAN_CONFIG_ANALYTICS;
    private static final int MAX_RECOVERY_TIMEOUT_DELAY_MS = 4000;
    private static final String TAG = "WifiController";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    /* access modifiers changed from: private */
    public final ActiveModeWarden mActiveModeWarden;
    private ClientModeManager.Listener mClientModeCallback = new ClientModeCallback();
    /* access modifiers changed from: private */
    public final ClientModeImpl mClientModeImpl;
    /* access modifiers changed from: private */
    public final Looper mClientModeImplLooper;
    private ConnectivityManager mCm;
    /* access modifiers changed from: private */
    public Context mContext;
    private DefaultState mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public boolean mDeferredSoftApEnable = false;
    /* access modifiers changed from: private */
    public EcmState mEcmState = new EcmState();
    /* access modifiers changed from: private */
    public final FrameworkFacade mFacade;
    private boolean mFirstUserSignOnSeen = false;
    /* access modifiers changed from: private */
    public boolean mIsShutdown = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiSharingModeEnabled = false;
    NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
    /* access modifiers changed from: private */
    public long mReEnableDelayMillis;
    /* access modifiers changed from: private */
    public int mRecoveryDelayMillis;
    /* access modifiers changed from: private */
    public boolean mResetSoftAp = false;
    private ScanOnlyModeManager.Listener mScanOnlyModeCallback = new ScanOnlyCallback();
    /* access modifiers changed from: private */
    public final WifiSettingsStore mSettingsStore;
    /* access modifiers changed from: private */
    public SoftApModeConfiguration mSoftApWifiConfig = null;
    /* access modifiers changed from: private */
    public StaDisabledState mStaDisabledState = new StaDisabledState();
    /* access modifiers changed from: private */
    public StaDisabledWithScanState mStaDisabledWithScanState = new StaDisabledWithScanState();
    /* access modifiers changed from: private */
    public StaEnabledState mStaEnabledState = new StaEnabledState();
    /* access modifiers changed from: private */
    public final WifiInjector mWifiInjector;
    private final WifiPermissionsUtil mWifiPermissionsUtil;

    public interface P2pDisableListener {
        void onDisable();
    }

    static {
        boolean z = true;
        if (Integer.parseInt("1") != 1) {
            z = false;
        }
        ENBLE_WLAN_CONFIG_ANALYTICS = z;
    }

    WifiController(Context context, ClientModeImpl clientModeImpl, Looper clientModeImplLooper, WifiSettingsStore wss, Looper wifiServiceLooper, FrameworkFacade f, ActiveModeWarden amw, WifiPermissionsUtil wifiPermissionsUtil) {
        super(TAG, wifiServiceLooper);
        this.mFacade = f;
        this.mContext = context;
        this.mClientModeImpl = clientModeImpl;
        this.mClientModeImplLooper = clientModeImplLooper;
        this.mActiveModeWarden = amw;
        this.mSettingsStore = wss;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mWifiInjector = WifiInjector.getInstance();
        addState(this.mDefaultState);
        addState(this.mStaDisabledState, this.mDefaultState);
        addState(this.mStaEnabledState, this.mDefaultState);
        addState(this.mStaDisabledWithScanState, this.mDefaultState);
        addState(this.mEcmState, this.mDefaultState);
        setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? 100 : 500);
        setLogOnlyTransitions(false);
        this.mActiveModeWarden.registerScanOnlyCallback(this.mScanOnlyModeCallback);
        this.mActiveModeWarden.registerClientModeCallback(this.mClientModeCallback);
        readWifiReEnableDelay();
        readWifiRecoveryDelay();
        readWifiSharingMode();
    }

    public void start() {
        boolean isAirplaneModeOn = this.mSettingsStore.isAirplaneModeOn();
        boolean isWifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        boolean isScanningAlwaysAvailable = this.mSettingsStore.isScanAlwaysAvailable();
        boolean isLocationModeActive = this.mWifiPermissionsUtil.isLocationModeEnabled();
        log("isAirplaneModeOn = " + isAirplaneModeOn + ", isWifiEnabled = " + isWifiEnabled + ", isScanningAvailable = " + isScanningAlwaysAvailable + ", isLocationModeActive = " + isLocationModeActive);
        if (checkScanOnlyModeAvailable()) {
            setInitialState(this.mStaDisabledWithScanState);
        } else {
            setInitialState(this.mStaDisabledState);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    if (state == 14) {
                        Log.e(WifiController.TAG, "SoftAP start failed");
                        WifiController.this.sendMessage(WifiController.CMD_AP_START_FAILURE);
                    } else if (state == 11) {
                        WifiController.this.sendMessage(WifiController.CMD_AP_STOPPED);
                    }
                } else if (action.equals("android.location.MODE_CHANGED")) {
                    WifiController.this.sendMessage(WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED);
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int wifistate = intent.getIntExtra("wifi_state", 4);
                    Log.i(WifiController.TAG, "wifistate :" + wifistate);
                    if (wifistate == 1 && WifiController.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() && WifiController.this.mSoftApWifiConfig != null && WifiController.this.mDeferredSoftApEnable) {
                        WifiController.this.log("STA_STOPPED,  enable softap");
                        boolean unused = WifiController.this.mDeferredSoftApEnable = false;
                        WifiController wifiController = WifiController.this;
                        wifiController.sendMessage(WifiController.CMD_SET_AP, 1, 1, wifiController.mSoftApWifiConfig);
                    }
                }
            }
        }, new IntentFilter(filter));
        WifiController.super.start();
        registerForWifiSharingModeChange();
    }

    /* access modifiers changed from: private */
    public boolean checkScanOnlyModeAvailable() {
        if (this.mSettingsStore.isManagedByAutoWifi() || this.mWifiPermissionsUtil.isLocationModeEnabled()) {
            return this.mSettingsStore.isScanAlwaysAvailable();
        }
        return false;
    }

    private void readWifiSharingMode() {
        boolean z = false;
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing", 0) == 1) {
            z = true;
        }
        this.mIsWifiSharingModeEnabled = z;
    }

    private void registerForWifiSharingModeChange() {
        this.mFacade.registerContentObserver(this.mContext, Settings.Secure.getUriFor("wifi_ap_wifi_sharing"), true, new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                boolean wifiSharingCurrentValue = WifiController.this.mIsWifiSharingModeEnabled;
                WifiController wifiController = WifiController.this;
                boolean z = false;
                if (Settings.Secure.getInt(wifiController.mContext.getContentResolver(), "wifi_ap_wifi_sharing", 0) == 1) {
                    z = true;
                }
                boolean unused = wifiController.mIsWifiSharingModeEnabled = z;
                if (WifiController.DBG && wifiSharingCurrentValue != WifiController.this.mIsWifiSharingModeEnabled) {
                    WifiController.this.logd("Wifi Sharing Provider changed " + wifiSharingCurrentValue + "->" + WifiController.this.mIsWifiSharingModeEnabled);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(Message message, State state) {
        if (DBG) {
            Log.d(TAG, " " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }
    }

    /* access modifiers changed from: protected */
    public String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(smToString(msg));
        int i = msg.what;
        if (i == CMD_SET_AP || i == CMD_AP_STOPPED || i == 155670) {
            sb.append(" ");
            sb.append(Integer.toString(msg.arg1));
            sb.append(" ");
            sb.append(Integer.toString(msg.arg2));
            sb.append(" WifiApState=");
            sb.append(this.mActiveModeWarden.getSoftApState());
        } else {
            sb.append(" ");
            sb.append(Integer.toString(msg.arg1));
            sb.append(" ");
            sb.append(Integer.toString(msg.arg2));
        }
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public String smToString(Message message) {
        return smToString(message.what);
    }

    /* access modifiers changed from: package-private */
    public String smToString(int what) {
        if (what == CMD_AP_STOPPED) {
            return "CMD_AP_STOPPED";
        }
        if (what == 155670) {
            return "CMD_RESET_AP";
        }
        switch (what) {
            case CMD_WIFI_TOGGLED /*155656*/:
                return "CMD_WIFI_TOGGLED";
            case CMD_AIRPLANE_TOGGLED /*155657*/:
                return "CMD_AIRPLANE_TOGGLED";
            case CMD_SET_AP /*155658*/:
                return "CMD_SET_AP";
            default:
                return "what:" + Integer.toString(what);
        }
    }

    private class ScanOnlyCallback implements ScanOnlyModeManager.Listener {
        private ScanOnlyCallback() {
        }

        public void onStateChanged(int state) {
            if (state == 4) {
                Log.d(WifiController.TAG, "ScanOnlyMode unexpected failure: state unknown");
            } else if (state == 1) {
                Log.d(WifiController.TAG, "ScanOnlyMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_SCANNING_STOPPED);
            } else if (state == 3) {
                Log.d(WifiController.TAG, "scan mode active");
            } else {
                Log.d(WifiController.TAG, "unexpected state update: " + state);
            }
        }
    }

    private class ClientModeCallback implements ClientModeManager.Listener {
        private ClientModeCallback() {
        }

        public void onStateChanged(int state) {
            if (state == 4) {
                WifiController.this.logd("ClientMode unexpected failure: state unknown");
                WifiController.this.sendMessage(WifiController.CMD_STA_START_FAILURE);
            } else if (state == 1) {
                WifiController.this.logd("ClientMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_STA_STOPPED);
            } else if (state == 3) {
                WifiController.this.logd("client mode active");
            } else {
                WifiController wifiController = WifiController.this;
                wifiController.logd("unexpected state update: " + state);
            }
        }
    }

    private void readWifiReEnableDelay() {
        this.mReEnableDelayMillis = this.mFacade.getLongSetting(this.mContext, "wifi_reenable_delay", DEFAULT_REENABLE_DELAY_MS);
    }

    private void readWifiRecoveryDelay() {
        this.mRecoveryDelayMillis = this.mContext.getResources().getInteger(17694997);
        if (this.mRecoveryDelayMillis > MAX_RECOVERY_TIMEOUT_DELAY_MS) {
            this.mRecoveryDelayMillis = MAX_RECOVERY_TIMEOUT_DELAY_MS;
            Log.w(TAG, "Overriding timeout delay with maximum limit value");
        }
    }

    private class P2pDisableCallback implements P2pDisableListener {
        private P2pDisableCallback() {
        }

        public void onDisable() {
            WifiController.this.log("success shutdown P2P and enable softap now");
            if (WifiController.this.mSoftApWifiConfig != null) {
                WifiController.this.mActiveModeWarden.enterSoftAPMode(WifiController.this.mSoftApWifiConfig);
            }
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message msg) {
            WifiController.this.logStateAndMessage(msg, this);
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED /*155649*/:
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED /*155662*/:
                    if (msg.arg1 == 1) {
                        WifiController wifiController = WifiController.this;
                        wifiController.transitionTo(wifiController.mEcmState);
                        break;
                    }
                    break;
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                case WifiController.CMD_AP_START_FAILURE /*155661*/:
                case WifiController.CMD_STA_START_FAILURE /*155664*/:
                case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE /*155666*/:
                case WifiController.CMD_STA_STOPPED /*155668*/:
                case WifiController.CMD_SCANNING_STOPPED /*155669*/:
                case WifiController.CMD_DEFERRED_RECOVERY_RESTART_WIFI /*155672*/:
                    break;
                case WifiController.CMD_AIRPLANE_TOGGLED /*155657*/:
                    Log.d(WifiController.TAG, "CMD_AIRPLANE_TOGGLED ,isMobileApOn():" + WifiController.this.isMobileApOn());
                    if (!WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        Log.d(WifiController.TAG, "Airplane mode disabled, determine next state");
                        if (!"VZW".equals(WifiController.CSC_CONFIG_OP_BRANDING) || WifiController.this.mSettingsStore.getPersistedWifiApState() != 1) {
                            if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                if (WifiController.this.checkScanOnlyModeAvailable()) {
                                    WifiController wifiController2 = WifiController.this;
                                    wifiController2.transitionTo(wifiController2.mStaDisabledWithScanState);
                                    break;
                                }
                            } else {
                                WifiController wifiController3 = WifiController.this;
                                wifiController3.transitionTo(wifiController3.mStaEnabledState);
                                break;
                            }
                        } else {
                            WifiController.this.mSettingsStore.persistWifiApState(0);
                            WifiController wifiController4 = WifiController.this;
                            SoftApModeConfiguration unused = wifiController4.mSoftApWifiConfig = new SoftApModeConfiguration(1, wifiController4.mWifiInjector.getWifiApConfigStore().getApConfiguration());
                            if (WifiController.this.mSoftApWifiConfig != null) {
                                WifiController.this.mActiveModeWarden.enterSoftAPMode(WifiController.this.mSoftApWifiConfig);
                            } else {
                                Log.e(WifiController.TAG, "mSoftApWifiConfig null");
                            }
                            try {
                                Thread.sleep(600);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (WifiController.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() && WifiController.this.mIsWifiSharingModeEnabled) {
                                if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                                        WifiController wifiController5 = WifiController.this;
                                        wifiController5.transitionTo(wifiController5.mStaDisabledWithScanState);
                                        break;
                                    }
                                } else {
                                    WifiController wifiController6 = WifiController.this;
                                    wifiController6.transitionTo(wifiController6.mStaEnabledState);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (WifiController.this.mSettingsStore.getWifiSavedState() == 1) {
                            WifiController.this.mSettingsStore.setWifiSavedState(0);
                        }
                        Log.d(WifiController.TAG, "Airplane mode toggled, shutdown all modes");
                        WifiController.this.mActiveModeWarden.shutdownWifi();
                        if (WifiController.this.isMobileApOn() && "VZW".equals(WifiController.CSC_CONFIG_OP_BRANDING)) {
                            WifiController.this.mSettingsStore.persistWifiApState(1);
                        }
                        if (WifiController.ENBLE_WLAN_CONFIG_ANALYTICS && WifiController.this.mClientModeImpl.isConnected()) {
                            WifiController.this.mClientModeImpl.setIsWifiOffByAirplane(true);
                        }
                        WifiController wifiController7 = WifiController.this;
                        wifiController7.transitionTo(wifiController7.mStaDisabledState);
                        break;
                    }
                    break;
                case WifiController.CMD_SET_AP /*155658*/:
                    int mWifiApState = WifiController.this.mActiveModeWarden.getSoftApState();
                    WifiController wifiController8 = WifiController.this;
                    wifiController8.logd("DefaultState CMD_SET_AP " + msg.arg1 + " " + msg.arg2 + " " + WifiController.this.mSettingsStore.getWifiSavedState() + " " + mWifiApState);
                    if (!WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        if (WifiController.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() && msg.arg1 == 1 && (mWifiApState == 11 || mWifiApState == 14)) {
                            WifiController.this.logForWifiSharing();
                            if (WifiController.this.mIsWifiSharingModeEnabled) {
                                if (WifiController.this.isP2pEnabled()) {
                                    WifiController.this.log("P2pEnabled, shutdown P2P and then enable softap");
                                    SoftApModeConfiguration unused2 = WifiController.this.mSoftApWifiConfig = (SoftApModeConfiguration) msg.obj;
                                    WifiController.this.mClientModeImpl.disableP2p(new P2pDisableCallback());
                                    break;
                                }
                            } else if (WifiController.this.getCurrentState() == WifiController.this.mStaEnabledState) {
                                if (WifiController.this.checkScanOnlyModeAvailable()) {
                                    WifiController wifiController9 = WifiController.this;
                                    wifiController9.transitionTo(wifiController9.mStaDisabledWithScanState);
                                } else {
                                    WifiController wifiController10 = WifiController.this;
                                    wifiController10.transitionTo(wifiController10.mStaDisabledState);
                                }
                                SoftApModeConfiguration unused3 = WifiController.this.mSoftApWifiConfig = (SoftApModeConfiguration) msg.obj;
                                boolean unused4 = WifiController.this.mDeferredSoftApEnable = true;
                                break;
                            }
                        }
                        if (msg.arg1 != 1 || (mWifiApState != 11 && mWifiApState != 14)) {
                            if (msg.arg1 == 0 && (WifiController.this.isMobileApOn() || mWifiApState == 14)) {
                                WifiController.this.mActiveModeWarden.stopSoftAPMode(msg.arg2);
                                break;
                            } else {
                                Log.e(WifiController.TAG, "Ignored as it is in already same state,mWifiApState:" + mWifiApState + ",msg.arg1:" + msg.arg1 + ",msg.arg2:" + msg.arg2);
                                break;
                            }
                        } else {
                            SoftApModeConfiguration mSoftApWifiConfig = (SoftApModeConfiguration) msg.obj;
                            if (mSoftApWifiConfig.getWifiConfiguration() == null) {
                                Log.d(WifiController.TAG, " creating mSoftApWifiConfig ,getWifiConfiguration is null");
                                mSoftApWifiConfig = new SoftApModeConfiguration(mSoftApWifiConfig.getTargetMode(), WifiController.this.mWifiInjector.getWifiApConfigStore().getApConfiguration());
                            }
                            WifiController.this.mActiveModeWarden.enterSoftAPMode(mSoftApWifiConfig);
                            break;
                        }
                    } else {
                        WifiController.this.log("drop softap requests when in airplane mode");
                        break;
                    }
                    break;
                case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                    WifiController.this.log("DEFERRED_TOGGLE ignored due to state change");
                    break;
                case WifiController.CMD_AP_STOPPED /*155663*/:
                    WifiController wifiController11 = WifiController.this;
                    wifiController11.log("SoftAp mode disabled, determine next state" + WifiController.this.mIsWifiSharingModeEnabled + ",mResetSoftAp " + WifiController.this.mResetSoftAp + ",mSettingsStore.getWifiSavedState():" + WifiController.this.mSettingsStore.getWifiSavedState() + ",mSettingsStore.isWifiToggleEnabled:" + WifiController.this.mSettingsStore.isWifiToggleEnabled());
                    WifiController.this.logForWifiSharing();
                    if (!WifiController.this.mResetSoftAp) {
                        if (!WifiController.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() || !WifiController.this.mIsWifiSharingModeEnabled) {
                            if (WifiController.this.mSettingsStore.getWifiSavedState() != 1) {
                                if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                                        WifiController wifiController12 = WifiController.this;
                                        wifiController12.transitionTo(wifiController12.mStaDisabledWithScanState);
                                        break;
                                    }
                                } else {
                                    WifiController wifiController13 = WifiController.this;
                                    wifiController13.transitionTo(wifiController13.mStaEnabledState);
                                    break;
                                }
                            } else if ("TMB".equals(WifiController.CSC_CONFIG_OP_BRANDING) || "MTR".equals(WifiController.CSC_CONFIG_OP_BRANDING) || "TMK".equals(WifiController.CSC_CONFIG_OP_BRANDING) || "VZW".equals(WifiController.CSC_CONFIG_OP_BRANDING)) {
                                if (WifiController.this.isUsbTethered()) {
                                    WifiController.this.mSettingsStore.setWifiSavedState(0);
                                    break;
                                } else {
                                    WifiController.this.mSettingsStore.handleWifiToggled(true);
                                    WifiController.this.mSettingsStore.setWifiSavedState(0);
                                    WifiController wifiController14 = WifiController.this;
                                    wifiController14.transitionTo(wifiController14.mStaEnabledState);
                                    break;
                                }
                            } else {
                                WifiController.this.mSettingsStore.handleWifiToggled(true);
                                WifiController.this.mSettingsStore.setWifiSavedState(0);
                                WifiController wifiController15 = WifiController.this;
                                wifiController15.transitionTo(wifiController15.mStaEnabledState);
                                break;
                            }
                        } else {
                            WifiController wifiController16 = WifiController.this;
                            wifiController16.log("getCurrentState() = " + WifiController.this.getCurrentState().getName());
                            if (WifiController.this.getCurrentState() == WifiController.this.mStaDisabledState) {
                                if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                                        WifiController wifiController17 = WifiController.this;
                                        wifiController17.transitionTo(wifiController17.mStaDisabledWithScanState);
                                        break;
                                    }
                                } else {
                                    WifiController wifiController18 = WifiController.this;
                                    wifiController18.transitionTo(wifiController18.mStaEnabledState);
                                    break;
                                }
                            }
                        }
                    } else {
                        WifiController.this.log("Adding 100milli sec");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                        boolean unused5 = WifiController.this.mResetSoftAp = false;
                        if (WifiController.this.mSoftApWifiConfig != null) {
                            WifiController.this.mActiveModeWarden.enterSoftAPMode(WifiController.this.mSoftApWifiConfig);
                            break;
                        }
                    }
                    break;
                case WifiController.CMD_RECOVERY_RESTART_WIFI /*155665*/:
                    WifiController wifiController19 = WifiController.this;
                    wifiController19.deferMessage(wifiController19.obtainMessage(WifiController.CMD_DEFERRED_RECOVERY_RESTART_WIFI));
                    WifiController.this.mActiveModeWarden.shutdownWifi();
                    WifiController wifiController20 = WifiController.this;
                    wifiController20.transitionTo(wifiController20.mStaDisabledState);
                    break;
                case WifiController.CMD_RECOVERY_DISABLE_WIFI /*155667*/:
                    WifiController.this.log("Recovery has been throttled, disable wifi");
                    WifiController.this.mActiveModeWarden.shutdownWifi();
                    WifiController wifiController21 = WifiController.this;
                    wifiController21.transitionTo(wifiController21.mStaDisabledState);
                    break;
                case WifiController.CMD_RESET_AP /*155670*/:
                    WifiController.this.logForWifiSharing();
                    if (WifiController.this.isMobileApOn()) {
                        boolean unused6 = WifiController.this.mResetSoftAp = true;
                        SoftApModeConfiguration unused7 = WifiController.this.mSoftApWifiConfig = (SoftApModeConfiguration) msg.obj;
                        WifiController.this.mActiveModeWarden.stopSoftAPMode(1);
                        break;
                    }
                    break;
                case WifiController.CMD_SHUTDOWN /*155673*/:
                    WifiController wifiController22 = WifiController.this;
                    wifiController22.transitionTo(wifiController22.mStaDisabledState);
                    boolean unused8 = WifiController.this.mIsShutdown = true;
                    break;
                default:
                    throw new RuntimeException("WifiController.handleMessage " + msg.what);
            }
            return true;
        }
    }

    class StaDisabledState extends State {
        private int mDeferredEnableSerialNumber = 0;
        private long mDisabledTimestamp;
        private boolean mHaveDeferredEnable = false;

        StaDisabledState() {
        }

        public void enter() {
            WifiController.this.mActiveModeWarden.disableWifi();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
        }

        public boolean processMessage(Message msg) {
            if (WifiController.this.mIsShutdown) {
                return true;
            }
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController wifiController = WifiController.this;
                        wifiController.transitionTo(wifiController.mStaDisabledWithScanState);
                        break;
                    }
                    break;
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!WifiController.this.checkScanOnlyModeAvailable()) {
                            if (WifiController.this.mClientModeImpl.syncGetWifiState() == 3) {
                                WifiController.this.loge("illegal state found both WifiController and WifiStateMachine");
                                WifiController wifiController2 = WifiController.this;
                                wifiController2.transitionTo(wifiController2.mStaDisabledState);
                                break;
                            }
                        } else if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController wifiController3 = WifiController.this;
                            wifiController3.transitionTo(wifiController3.mStaDisabledWithScanState);
                            break;
                        }
                    } else if (!doDeferEnable(msg)) {
                        WifiController wifiController4 = WifiController.this;
                        wifiController4.transitionTo(wifiController4.mStaEnabledState);
                        break;
                    } else {
                        if (this.mHaveDeferredEnable) {
                            this.mDeferredEnableSerialNumber++;
                        }
                        this.mHaveDeferredEnable = !this.mHaveDeferredEnable;
                        break;
                    }
                    break;
                case WifiController.CMD_SET_AP /*155658*/:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        WifiController.this.log("drop softap requests when in airplane mode");
                        break;
                    } else {
                        if (msg.arg1 == 1) {
                            WifiController.this.mSettingsStore.setWifiSavedState(msg.arg2);
                        }
                        return false;
                    }
                case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                    if (msg.arg1 == this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                        break;
                    } else {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                        break;
                    }
                case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE /*155666*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController wifiController5 = WifiController.this;
                            wifiController5.transitionTo(wifiController5.mStaDisabledWithScanState);
                            break;
                        }
                    } else {
                        WifiController wifiController6 = WifiController.this;
                        wifiController6.transitionTo(wifiController6.mStaEnabledState);
                        break;
                    }
                    break;
                case WifiController.CMD_DEFERRED_RECOVERY_RESTART_WIFI /*155672*/:
                    WifiController wifiController7 = WifiController.this;
                    wifiController7.sendMessageDelayed(WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE, (long) wifiController7.mRecoveryDelayMillis);
                    break;
                case WifiController.CMD_SHUTDOWN /*155673*/:
                    boolean unused = WifiController.this.mIsShutdown = true;
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController wifiController = WifiController.this;
            wifiController.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController wifiController2 = WifiController.this;
            wifiController2.sendMessageDelayed(deferredMsg, (wifiController2.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class StaEnabledState extends State {
        StaEnabledState() {
        }

        public void enter() {
            WifiController.this.log("StaEnabledState.enter()");
            WifiController.this.mActiveModeWarden.enterClientMode();
        }

        public boolean processMessage(Message msg) {
            String bugTitle;
            String bugDetail;
            WifiController.this.logStateAndMessage(msg, this);
            switch (msg.what) {
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController wifiController = WifiController.this;
                            wifiController.transitionTo(wifiController.mStaDisabledState);
                            break;
                        } else {
                            WifiController wifiController2 = WifiController.this;
                            wifiController2.transitionTo(wifiController2.mStaDisabledWithScanState);
                            break;
                        }
                    }
                    break;
                case WifiController.CMD_AIRPLANE_TOGGLED /*155657*/:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        return false;
                    }
                    Log.d(WifiController.TAG, "airplane mode toggled - and airplane mode is off.  return handled mIsWifiSharingModeEnabled :" + WifiController.this.mIsWifiSharingModeEnabled);
                    if ("VZW".equals(WifiController.CSC_CONFIG_OP_BRANDING)) {
                        if (!WifiController.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() || !WifiController.this.mIsWifiSharingModeEnabled) {
                            if (WifiController.this.mSettingsStore.getPersistedWifiApState() == 1) {
                                WifiController.this.mSettingsStore.persistWifiApState(0);
                            }
                        } else if (WifiController.this.mSettingsStore.getPersistedWifiApState() == 1) {
                            WifiController.this.mSettingsStore.persistWifiApState(0);
                            if (!WifiController.this.isUsbTethered()) {
                                WifiController wifiController3 = WifiController.this;
                                SoftApModeConfiguration unused = wifiController3.mSoftApWifiConfig = new SoftApModeConfiguration(1, wifiController3.mWifiInjector.getWifiApConfigStore().getApConfiguration());
                                if (WifiController.this.mSoftApWifiConfig != null) {
                                    WifiController.this.mActiveModeWarden.enterSoftAPMode(WifiController.this.mSoftApWifiConfig);
                                } else {
                                    Log.e(WifiController.TAG, "mSoftApWifiConfig null");
                                }
                            }
                        }
                    }
                    return true;
                case WifiController.CMD_SET_AP /*155658*/:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(1);
                    }
                    return false;
                case WifiController.CMD_AP_START_FAILURE /*155661*/:
                case WifiController.CMD_AP_STOPPED /*155663*/:
                    WifiController.this.logd("sta enabled CMD_AP_STOPPED mResetSoftAp : " + WifiController.this.mResetSoftAp);
                    if (WifiController.this.mResetSoftAp) {
                        return false;
                    }
                    break;
                case WifiController.CMD_STA_START_FAILURE /*155664*/:
                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController wifiController4 = WifiController.this;
                        wifiController4.transitionTo(wifiController4.mStaDisabledWithScanState);
                        break;
                    } else {
                        WifiController wifiController5 = WifiController.this;
                        wifiController5.transitionTo(wifiController5.mStaDisabledState);
                        break;
                    }
                case WifiController.CMD_RECOVERY_RESTART_WIFI /*155665*/:
                    if (msg.arg1 >= SelfRecovery.REASON_STRINGS.length || msg.arg1 < 0) {
                        bugDetail = "";
                        bugTitle = "Wi-Fi BugReport";
                    } else {
                        bugDetail = SelfRecovery.REASON_STRINGS[msg.arg1];
                        bugTitle = "Wi-Fi BugReport: " + bugDetail;
                    }
                    if (msg.arg1 != 0) {
                        new Handler(WifiController.this.mClientModeImplLooper).post(new Runnable(bugTitle, bugDetail) {
                            private final /* synthetic */ String f$1;
                            private final /* synthetic */ String f$2;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                            }

                            public final void run() {
                                WifiController.StaEnabledState.this.lambda$processMessage$0$WifiController$StaEnabledState(this.f$1, this.f$2);
                            }
                        });
                    }
                    return false;
                case WifiController.CMD_STA_STOPPED /*155668*/:
                    WifiController wifiController6 = WifiController.this;
                    wifiController6.transitionTo(wifiController6.mStaDisabledState);
                    break;
                default:
                    return false;
            }
            return true;
        }

        public /* synthetic */ void lambda$processMessage$0$WifiController$StaEnabledState(String bugTitle, String bugDetail) {
            WifiController.this.mClientModeImpl.takeBugReport(bugTitle, bugDetail);
        }
    }

    class StaDisabledWithScanState extends State {
        private int mDeferredEnableSerialNumber = 0;
        private long mDisabledTimestamp;
        private boolean mHaveDeferredEnable = false;

        StaDisabledWithScanState() {
        }

        public void enter() {
            WifiController.this.mActiveModeWarden.enterScanOnlyMode();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
        }

        public boolean processMessage(Message msg) {
            WifiController.this.logStateAndMessage(msg, this);
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                    if (!WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.log("StaDisabledWithScanState: scan no longer available");
                        WifiController wifiController = WifiController.this;
                        wifiController.transitionTo(wifiController.mStaDisabledState);
                        break;
                    }
                    break;
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.mClientModeImpl.syncGetWifiState() == 3) {
                            WifiController.this.loge("illegal state found both WifiController and WifiStateMachine");
                            WifiController wifiController2 = WifiController.this;
                            wifiController2.transitionTo(wifiController2.mStaDisabledWithScanState);
                            break;
                        }
                    } else if (!doDeferEnable(msg)) {
                        WifiController wifiController3 = WifiController.this;
                        wifiController3.transitionTo(wifiController3.mStaEnabledState);
                        break;
                    } else {
                        if (this.mHaveDeferredEnable) {
                            this.mDeferredEnableSerialNumber++;
                        }
                        this.mHaveDeferredEnable = !this.mHaveDeferredEnable;
                        break;
                    }
                    break;
                case WifiController.CMD_SET_AP /*155658*/:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(msg.arg2);
                    }
                    return false;
                case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                    if (msg.arg1 == this.mDeferredEnableSerialNumber) {
                        WifiController.this.logd("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                        break;
                    } else {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                        break;
                    }
                case WifiController.CMD_AP_START_FAILURE /*155661*/:
                case WifiController.CMD_AP_STOPPED /*155663*/:
                    return false;
                case WifiController.CMD_SCANNING_STOPPED /*155669*/:
                    WifiController.this.log("WifiController: SCANNING_STOPPED when in scan mode -> StaDisabled");
                    WifiController wifiController4 = WifiController.this;
                    wifiController4.transitionTo(wifiController4.mStaDisabledState);
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController wifiController = WifiController.this;
            wifiController.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController wifiController2 = WifiController.this;
            wifiController2.sendMessageDelayed(deferredMsg, (wifiController2.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    private State getNextWifiState() {
        if (this.mSettingsStore.getWifiSavedState() == 1) {
            return this.mStaEnabledState;
        }
        if (checkScanOnlyModeAvailable()) {
            return this.mStaDisabledWithScanState;
        }
        return this.mStaDisabledState;
    }

    class EcmState extends State {
        private int mEcmEntryCount;

        EcmState() {
        }

        public void enter() {
            WifiController.this.mActiveModeWarden.stopSoftAPMode(-1);
            boolean configWiFiDisableInECBM = WifiController.this.mFacade.getConfigWiFiDisableInECBM(WifiController.this.mContext);
            WifiController wifiController = WifiController.this;
            wifiController.log("WifiController msg getConfigWiFiDisableInECBM " + configWiFiDisableInECBM);
            if (1 != 0) {
                WifiController.this.mActiveModeWarden.shutdownWifi();
            }
            this.mEcmEntryCount = 1;
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED /*155649*/:
                    if (msg.arg1 == 1) {
                        this.mEcmEntryCount++;
                    } else if (msg.arg1 == 0) {
                        decrementCountAndReturnToAppropriateState();
                    }
                    return true;
                case WifiController.CMD_SET_AP /*155658*/:
                    return true;
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED /*155662*/:
                    if (msg.arg1 == 1) {
                        this.mEcmEntryCount++;
                    } else if (msg.arg1 == 0) {
                        decrementCountAndReturnToAppropriateState();
                    }
                    return true;
                case WifiController.CMD_AP_STOPPED /*155663*/:
                case WifiController.CMD_STA_STOPPED /*155668*/:
                case WifiController.CMD_SCANNING_STOPPED /*155669*/:
                    return true;
                case WifiController.CMD_RECOVERY_RESTART_WIFI /*155665*/:
                case WifiController.CMD_RECOVERY_DISABLE_WIFI /*155667*/:
                    return true;
                default:
                    return false;
            }
        }

        private void decrementCountAndReturnToAppropriateState() {
            boolean exitEcm = false;
            int i = this.mEcmEntryCount;
            if (i == 0) {
                WifiController.this.loge("mEcmEntryCount is 0; exiting Ecm");
                exitEcm = true;
            } else {
                int i2 = i - 1;
                this.mEcmEntryCount = i2;
                if (i2 == 0) {
                    exitEcm = true;
                }
            }
            if (!exitEcm) {
                return;
            }
            if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                WifiController wifiController = WifiController.this;
                wifiController.transitionTo(wifiController.mStaEnabledState);
            } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                WifiController wifiController2 = WifiController.this;
                wifiController2.transitionTo(wifiController2.mStaDisabledWithScanState);
            } else {
                WifiController wifiController3 = WifiController.this;
                wifiController3.transitionTo(wifiController3.mStaDisabledState);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isMobileApOn() {
        int state = this.mActiveModeWarden.getSoftApState();
        if (state == 13 || state == 12) {
            return true;
        }
        return false;
    }

    private boolean isWifiOn() {
        int wifiState = this.mClientModeImpl.syncGetWifiState();
        if (wifiState == 3 || wifiState == 2) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isP2pEnabled() {
        try {
            WifiP2pManager mWifiP2pManager = (WifiP2pManager) this.mContext.getSystemService("wifip2p");
            if (mWifiP2pManager == null) {
                return false;
            }
            return mWifiP2pManager.isWifiP2pEnabled();
        } catch (NullPointerException e) {
            Log.d(TAG, "isP2pEnabled - NullPointerException");
            return false;
        }
    }

    private void checkAndGetConnectivityManager() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    public boolean isUsbTethered() {
        checkAndGetConnectivityManager();
        String[] tethered = this.mCm.getTetheredIfaces();
        String[] usbRegexs = this.mCm.getTetherableUsbRegexs();
        for (String tether : tethered) {
            if ("ncm0".equals(tether)) {
                Log.d(TAG, "enabled tetheredIface : ncm0");
                return false;
            }
            for (String regex : usbRegexs) {
                if (tether.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        WifiController.super.dump(fd, pw, args);
        pw.println("Wi-Fi Sharing dump ----- start -----\n");
        pw.println("isWifiSharing enabled ? " + this.mIsWifiSharingModeEnabled);
        pw.println("IsWifiToggled :" + this.mSettingsStore.isWifiToggleEnabled());
        pw.println("Wifi Saved State :" + this.mSettingsStore.getWifiSavedState());
        pw.println("WifiApState " + this.mActiveModeWarden.getSoftApState());
        pw.println("getPersisted WifiAp State  :" + this.mSettingsStore.getPersistedWifiApState());
        pw.println("Wi-Fi Sharing dump ----- end -----\n");
    }

    public void logForWifiSharing() {
        logd("isWifiSharing enabled ? " + this.mIsWifiSharingModeEnabled);
        logd("Current state : " + getCurrentState().getName());
        logd("Is Wifi Toggled :" + this.mSettingsStore.isWifiToggleEnabled());
        logd("Wifi Saved State :" + this.mSettingsStore.getWifiSavedState());
        logd("getPersisted WifiAp State  :" + this.mSettingsStore.getPersistedWifiApState());
    }
}
