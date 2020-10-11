package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.INetd;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.wifi.IDppCallback;
import android.net.wifi.INetworkRequestMatchCallback;
import android.net.wifi.IOnWifiUsabilityStatsListener;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.ITrafficStateCallback;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiSsid;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.sec.enterprise.EnterpriseDeviceManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.LocalLog;
import android.util.Log;
import android.util.MutableInt;
import android.util.Slog;
import android.util.SparseArray;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.PowerProfile;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.LocalOnlyHotspotRequestInfo;
import com.android.server.wifi.WifiConnectivityMonitor;
import com.android.server.wifi.WifiServiceImpl;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.PasspointProvider;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.util.ExternalCallbackTracker;
import com.android.server.wifi.util.GeneralUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiHandler;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.feature.SemFloatingFeature;
import com.samsung.android.net.wifi.ISemWifiApSmartCallback;
import com.samsung.android.net.wifi.ISharedPasswordCallback;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.net.wifi.SemWifiApBleScanResult;
import com.samsung.android.server.wifi.AutoWifiController;
import com.samsung.android.server.wifi.CscParser;
import com.samsung.android.server.wifi.HotspotMobileDataLimit;
import com.samsung.android.server.wifi.SemWifiFrameworkUxUtils;
import com.samsung.android.server.wifi.SwitchBoardService;
import com.samsung.android.server.wifi.WifiControlHistoryProvider;
import com.samsung.android.server.wifi.WifiDefaultApController;
import com.samsung.android.server.wifi.WifiDevicePolicyManager;
import com.samsung.android.server.wifi.WifiEnableWarningPolicy;
import com.samsung.android.server.wifi.WifiGuiderManagementService;
import com.samsung.android.server.wifi.WifiGuiderPackageMonitor;
import com.samsung.android.server.wifi.WifiMobileDeviceManager;
import com.samsung.android.server.wifi.WifiRoamingAssistant;
import com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.dqa.ReportUtil;
import com.samsung.android.server.wifi.hotspot2.PasspointDefaultProvider;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider;
import com.samsung.android.server.wifi.softap.DhcpPacket;
import com.samsung.android.server.wifi.softap.SemSoftapConfig;
import com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver;
import com.samsung.android.server.wifi.softap.SemWifiApClientInfo;
import com.samsung.android.server.wifi.softap.SemWifiApTrafficPoller;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartBleScanner;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DMHS;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartMHS;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import com.samsung.android.server.wifi.wlansniffer.WlanSnifferController;
import com.sec.android.app.CscFeatureTagCOMMON;
import com.sec.android.app.CscFeatureTagSetting;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.ksoap2.SoapEnvelope;

public class WifiServiceImpl extends BaseWifiService {
    private static final int BACKGROUND_IMPORTANCE_CUTOFF = 125;
    public static final String CONFIGOPBRANDINGFORMOBILEAP = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDINGFORMOBILEAP, "ALL");
    private static final boolean CSC_DISABLE_EMERGENCYCALLBACK_TRANSITION = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEEMERGENCYCALLBACKTRANSITION);
    private static final boolean CSC_SEND_DHCP_RELEASE = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SENDSIGNALDURINGPOWEROFF);
    private static final boolean DBG = true;
    private static final long DEFAULT_SCAN_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final String DESKTOP_SYSTEM_UI_PACKAGE_NAME = "com.samsung.desktopsystemui";
    private static final boolean MHSDBG = ("eng".equals(Build.TYPE) || Debug.semIsProductDev());
    private static final int MHS_SAR_BACKOFF_5G_DISABLED = 4;
    private static final int MHS_SAR_BACKOFF_5G_ENABLED = 3;
    private static final int NUM_SOFT_AP_CALLBACKS_WARN_LIMIT = 10;
    private static final int NUM_SOFT_AP_CALLBACKS_WTF_LIMIT = 20;
    private static final String P2P_PACKAGE_NAME = "com.android.server.wifi.p2p";
    private static final int RUN_WITH_SCISSORS_TIMEOUT_MILLIS = 4000;
    static final int SECURITY_EAP = 3;
    static final int SECURITY_NONE = 0;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_WEP = 1;
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final boolean SUPPORTMOBILEAPENHANCED = true;
    public static final boolean SUPPORTMOBILEAPENHANCED_D2D = "d2d".equals("d2d");
    public static final boolean SUPPORTMOBILEAPENHANCED_LITE = "d2d".equals("lite");
    public static final boolean SUPPORTMOBILEAPENHANCED_WIFI_ONLY_LITE = "d2d".equals("wifi_only_lite");
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "WifiService";
    private static final boolean VDBG = false;
    private static final boolean WIFI_STOP_SCAN_FOR_ETWS = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_STOPSCANFORETWS);
    private static Set<Integer> mAllFreqArray = new HashSet();
    private static SparseArray<Integer> mFreq2ChannelNum = new SparseArray<>();
    private static int[] mFreqArr = {2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472, 2484, 5170, 5180, 5190, 5200, 5210, 5220, 5230, 5240, 5260, 5280, 5300, 5320, 5500, 5520, 5540, 5560, 5580, 5600, 5620, 5640, 5660, 5680, 5700, 5745, 5765, 5785, 5805, 5825};
    private static final String mHotspotActionForSimStatus = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGHOTSPOTACTIONFORSIMSTATUS);
    public static final String mIndoorChannelCountry = "GH,GG,GR,GL,ZA,NL,NO,NF,NZ,NU,KR,DK,DE,LV,RO,LU,LY,LT,LI,MK,IM,MC,MA,ME,MV,MT,BH,BB,VA,VE,VN,BE,BA,BG,BR,SA,SM,PM,RS,SE,CH,ES,SK,SI,AE,AR,IS,IE,AL,EE,GB,IO,OM,AU,AT,UY,UA,IL,EG,IT,IN,JP,JE,GE,CN,GI,CZ,CL,CA,CC,CO,KW,CK,HR,CY,TH,TR,TK,PA,FO,PT,PL,FR,TF,PF,FJ,FI,PN,HM,HU,HK";
    private static List<String> mMHSChannelHistoryLogs = new ArrayList();
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private int isProvisionSuccessful = 0;
    final ActiveModeWarden mActiveModeWarden;
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    /* access modifiers changed from: private */
    public AsyncChannelExternalClientHandler mAsyncChannelExternalClientHandler;
    /* access modifiers changed from: private */
    public AutoWifiController mAutoWifiController;
    /* access modifiers changed from: private */
    public boolean mBlockScanFromOthers = false;
    private String mCSCRegion = "";
    /* access modifiers changed from: private */
    public boolean mChameleonEnabled = false;
    final ClientModeImpl mClientModeImpl;
    @VisibleForTesting
    AsyncChannel mClientModeImplChannel;
    ClientModeImplHandler mClientModeImplHandler;
    private final Clock mClock;
    /* access modifiers changed from: private */
    public final Context mContext;
    Map<String, String> mCountryChannel = new HashMap(5);
    Map<String, String> mCountryChannelList = new HashMap();
    /* access modifiers changed from: private */
    public final WifiCountryCode mCountryCode;
    private String mCountryIso = "";
    private WifiDefaultApController mDefaultApController;
    private final WifiDevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public int mDomRoamMaxUser = 10;
    private final DppManager mDppManager;
    private final FrameworkFacade mFacade;
    /* access modifiers changed from: private */
    public final FrameworkFacade mFrameworkFacade;
    /* access modifiers changed from: private */
    public int mGsmMaxUser = 1;
    private List<String> mHistoricalDumpLogs = new ArrayList();
    private HotspotMobileDataLimit mHotspotMobileDataLimit;
    private Messenger mIWCMessenger = null;
    /* access modifiers changed from: private */
    public IWCMonitor mIWCMonitor;
    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private final ConcurrentHashMap<String, Integer> mIfaceIpModes;
    boolean mInIdleMode;
    private final String mIndoorChannelFilePath = "/vendor/etc/wifi/indoorchannel.info";
    /* access modifiers changed from: private */
    public int mIntRoamMaxUser = 10;
    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private WifiConfiguration mLocalOnlyHotspotConfig = null;
    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private final HashMap<Integer, LocalOnlyHotspotRequestInfo> mLocalOnlyHotspotRequests;
    private WifiLog mLog;
    /* access modifiers changed from: private */
    public int mMaxUser = WifiApConfigStore.MAX_CLIENT;
    private MobileWipsFrameworkService mMobileWipsFrameworkService;
    private INetd mNetdService = null;
    private final PasspointDefaultProvider mPasspointDefaultProvider;
    private final PasspointManager mPasspointManager;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onDataConnectionStateChanged(int state, int networkType) {
            int maxClientNum;
            Slog.d(WifiServiceImpl.TAG, "onDataConnectionStateChanged: state -" + String.valueOf(state) + ", networkType - " + TelephonyManager.getNetworkTypeName(networkType));
            WifiServiceImpl.this.setMaxClientVzwBasedOnNetworkType(networkType);
            if ("SPRINT".equals(WifiServiceImpl.CONFIGOPBRANDINGFORMOBILEAP)) {
                int wifiApState = WifiServiceImpl.this.getWifiApEnabledState();
                if (wifiApState == 12 || wifiApState == 13) {
                    ContentResolver cr = WifiServiceImpl.this.mContext.getContentResolver();
                    int i = WifiApConfigStore.MAX_CLIENT;
                    if (1 == networkType || 2 == networkType || 16 == networkType || networkType == 0) {
                        maxClientNum = Settings.System.getInt(cr, "chameleon_gsmmaxuser", 1);
                    } else {
                        try {
                            maxClientNum = Settings.System.getInt(cr, "chameleon_maxuser");
                        } catch (Settings.SettingNotFoundException e) {
                            maxClientNum = WifiApConfigStore.MAX_CLIENT;
                        }
                    }
                    WifiConfiguration mWifiConfig = WifiServiceImpl.this.getWifiApConfiguration();
                    if (mWifiConfig != null) {
                        int maxClientNum2 = maxClientNum < mWifiConfig.maxclient ? maxClientNum : mWifiConfig.maxclient;
                        Log.i(WifiServiceImpl.TAG, "maxClientNum = " + maxClientNum2);
                        Message msg = new Message();
                        msg.what = 14;
                        Bundle b = new Bundle();
                        b.putInt("maxClient", maxClientNum2);
                        msg.obj = b;
                        WifiServiceImpl.this.callSECApi(msg);
                    }
                }
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:5:0x002c, code lost:
            if (r0 != 2) goto L_0x005d;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onServiceStateChanged(android.telephony.ServiceState r4) {
            /*
                r3 = this;
                super.onServiceStateChanged(r4)
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "onServiceStateChanged : "
                r0.append(r1)
                int r1 = r4.getState()
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                java.lang.String r1 = "WifiService"
                android.util.Slog.d(r1, r0)
                com.android.server.wifi.WifiServiceImpl r0 = com.android.server.wifi.WifiServiceImpl.this
                r0.checkAndSarBackOffFor5G(r4)
                int r0 = r4.getState()
                if (r0 == 0) goto L_0x0046
                r2 = 1
                if (r0 == r2) goto L_0x002f
                r2 = 2
                if (r0 == r2) goto L_0x0046
                goto L_0x005d
            L_0x002f:
                com.android.server.wifi.WifiServiceImpl r0 = com.android.server.wifi.WifiServiceImpl.this
                com.android.server.wifi.WifiSettingsStore r0 = r0.mSettingsStore
                boolean r0 = r0.isAirplaneModeOn()
                if (r0 != 0) goto L_0x005d
                java.lang.String r0 = "[FCC]STATE_OUT_OF_SERVICE & Airplane off, Enable setFccChannel()"
                android.util.Slog.d(r1, r0)
                com.android.server.wifi.WifiServiceImpl r0 = com.android.server.wifi.WifiServiceImpl.this
                com.android.server.wifi.ClientModeImpl r0 = r0.mClientModeImpl
                r0.syncSetFccChannel(r2)
                goto L_0x005d
            L_0x0046:
                com.android.server.wifi.WifiServiceImpl r0 = com.android.server.wifi.WifiServiceImpl.this
                com.android.server.wifi.WifiSettingsStore r0 = r0.mSettingsStore
                boolean r0 = r0.isAirplaneModeOn()
                if (r0 != 0) goto L_0x005d
                java.lang.String r0 = "[FCC]NOT IN STATE_OUT_OF_SERVICE & Airplane off, Disable setFccChannel()"
                android.util.Slog.d(r1, r0)
                com.android.server.wifi.WifiServiceImpl r0 = com.android.server.wifi.WifiServiceImpl.this
                com.android.server.wifi.ClientModeImpl r0 = r0.mClientModeImpl
                r1 = 0
                r0.syncSetFccChannel(r1)
            L_0x005d:
                int r0 = r4.getDataNetworkType()
                com.android.server.wifi.WifiServiceImpl r1 = com.android.server.wifi.WifiServiceImpl.this
                r1.setMaxClientVzwBasedOnNetworkType(r0)
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.C047514.onServiceStateChanged(android.telephony.ServiceState):void");
        }
    };
    private final PowerManager mPowerManager;
    PowerProfile mPowerProfile;
    private int mPrevNrState = 0;
    private int mQoSTestCounter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.USER_REMOVED")) {
                WifiServiceImpl.this.mClientModeImpl.removeUserConfigs(intent.getIntExtra("android.intent.extra.user_handle", 0));
            } else if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                WifiServiceImpl.this.mClientModeImpl.sendBluetoothAdapterStateChange(intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0));
            } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                WifiServiceImpl.this.mWifiController.sendMessage(155649, emergencyMode, 0);
                WifiServiceImpl.this.mClientModeImpl.report(9, ReportUtil.getReportDataForChangeState(emergencyMode));
            } else if (action.equals("android.intent.action.EMERGENCY_CALL_STATE_CHANGED")) {
                boolean inCall = intent.getBooleanExtra("phoneInEmergencyCall", false);
                WifiServiceImpl.this.mWifiController.sendMessage(155662, inCall, 0);
                WifiServiceImpl.this.mClientModeImpl.report(9, ReportUtil.getReportDataForChangeState(inCall));
            } else if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                WifiServiceImpl.this.handleIdleModeChanged();
            } else if (action.equals("android.provider.Telephony.SMS_CB_WIFI_RECEIVED")) {
                boolean unused = WifiServiceImpl.this.mBlockScanFromOthers = true;
                Log.e(WifiServiceImpl.TAG, "received broadcast ETWS, Scanning will be blocked");
            }
        }
    };
    /* access modifiers changed from: private */
    public final ConcurrentHashMap<Integer, ISemWifiApSmartCallback> mRegisteredSemWifiApSmartCallbacks;
    /* access modifiers changed from: private */
    public final ExternalCallbackTracker<ISoftApCallback> mRegisteredSoftApCallbacks;
    boolean mScanPending;
    final ScanRequestProxy mScanRequestProxy;
    /* access modifiers changed from: private */
    public SemWifiProfileAndQoSProvider mSemQoSProfileShareProvider;
    private SemWifiApBroadcastReceiver mSemWifiApBroadcastReceiver;
    private SemWifiApClientInfo mSemWifiApClientInfo;
    private SemWifiApSmartBleScanner mSemWifiApSmartBleScanner = null;
    private SemWifiApSmartClient mSemWifiApSmartClient = null;
    private SemWifiApSmartD2DClient mSemWifiApSmartD2DClient = null;
    private SemWifiApSmartD2DGattClient mSemWifiApSmartD2DGattClient = null;
    private SemWifiApSmartD2DMHS mSemWifiApSmartD2DMHS = null;
    private SemWifiApSmartGattClient mSemWifiApSmartGattClient = null;
    private SemWifiApSmartGattServer mSemWifiApSmartGattServer = null;
    private LocalLog mSemWifiApSmartLocalLog = null;
    private SemWifiApSmartMHS mSemWifiApSmartMHS = null;
    /* access modifiers changed from: private */
    public String mSemWifiApSmartMhsMac = null;
    /* access modifiers changed from: private */
    public int mSemWifiApSmartState = 0;
    /* access modifiers changed from: private */
    public SemWifiApSmartUtil mSemWifiApSmartUtil = null;
    private SemWifiApTrafficPoller mSemWifiApTrafficPoller = null;
    final WifiSettingsStore mSettingsStore;
    /* access modifiers changed from: private */
    public int mSoftApNumClients = 0;
    /* access modifiers changed from: private */
    public int mSoftApState = 11;
    private String mSsid = "";
    private int mSupportedFeaturesOfHal = -1;
    private SwitchBoardService mSwitchBoardService;
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public int mTetheredData = 0;
    private final UserManager mUserManager;
    private boolean mVerboseLoggingEnabled = false;
    private WifiApConfigStore mWifiApConfigStore;
    private int mWifiApState = 11;
    private final WifiBackupRestore mWifiBackupRestore;
    /* access modifiers changed from: private */
    public WifiConnectivityMonitor mWifiConnectivityMonitor;
    /* access modifiers changed from: private */
    public WifiController mWifiController;
    private WifiEnableWarningPolicy mWifiEnableWarningPolicy;
    private WifiGuiderManagementService mWifiGuiderManagementService;
    private WifiGuiderPackageMonitor mWifiGuiderPackageMonitor;
    /* access modifiers changed from: private */
    public final WifiInjector mWifiInjector;
    private final WifiLockManager mWifiLockManager;
    private final WifiMetrics mWifiMetrics;
    private final WifiMulticastLockManager mWifiMulticastLockManager;
    private WifiNative mWifiNative;
    /* access modifiers changed from: private */
    public final WifiNetworkSuggestionsManager mWifiNetworkSuggestionsManager;
    /* access modifiers changed from: private */
    public WifiPermissionsUtil mWifiPermissionsUtil;
    private WifiRoamingAssistant mWifiRoamingAssistant;
    private boolean mWifiSharingLitePopup = false;
    private WifiTrafficPoller mWifiTrafficPoller;
    private WlanSnifferController mWlanSnifferController;
    private int scanRequestCounter = 0;
    private boolean semIsShutdown = true;
    private boolean semIsTestMode = false;

    static {
        int i = 0;
        while (true) {
            int[] iArr = mFreqArr;
            if (i < iArr.length) {
                mAllFreqArray.add(Integer.valueOf(iArr[i]));
                i++;
            } else {
                mFreq2ChannelNum.append(2412, 1);
                mFreq2ChannelNum.append(2417, 2);
                mFreq2ChannelNum.append(2422, 3);
                mFreq2ChannelNum.append(2427, 4);
                mFreq2ChannelNum.append(2432, 5);
                mFreq2ChannelNum.append(2437, 6);
                mFreq2ChannelNum.append(2442, 7);
                mFreq2ChannelNum.append(2447, 8);
                mFreq2ChannelNum.append(2452, 9);
                mFreq2ChannelNum.append(2457, 10);
                mFreq2ChannelNum.append(2462, 11);
                mFreq2ChannelNum.append(2467, 12);
                mFreq2ChannelNum.append(2472, 13);
                mFreq2ChannelNum.append(2484, 14);
                mFreq2ChannelNum.append(5170, 34);
                mFreq2ChannelNum.append(5180, 36);
                mFreq2ChannelNum.append(5190, 38);
                mFreq2ChannelNum.append(5200, 40);
                mFreq2ChannelNum.append(5210, 42);
                mFreq2ChannelNum.append(5220, 44);
                mFreq2ChannelNum.append(5230, 46);
                mFreq2ChannelNum.append(5240, 48);
                mFreq2ChannelNum.append(5260, 52);
                mFreq2ChannelNum.append(5280, 56);
                mFreq2ChannelNum.append(5300, 60);
                mFreq2ChannelNum.append(5320, 64);
                mFreq2ChannelNum.append(5500, 100);
                mFreq2ChannelNum.append(5520, 104);
                mFreq2ChannelNum.append(5540, 108);
                mFreq2ChannelNum.append(5560, Integer.valueOf(ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE));
                mFreq2ChannelNum.append(5580, 116);
                mFreq2ChannelNum.append(5600, Integer.valueOf(SoapEnvelope.VER12));
                mFreq2ChannelNum.append(5620, 124);
                mFreq2ChannelNum.append(5640, 128);
                mFreq2ChannelNum.append(5660, 132);
                mFreq2ChannelNum.append(5680, 136);
                mFreq2ChannelNum.append(5700, 140);
                mFreq2ChannelNum.append(5720, 144);
                mFreq2ChannelNum.append(5745, 149);
                mFreq2ChannelNum.append(5765, 153);
                mFreq2ChannelNum.append(5785, 157);
                mFreq2ChannelNum.append(5805, 161);
                mFreq2ChannelNum.append(5825, 165);
                return;
            }
        }
    }

    public final class LocalOnlyRequestorCallback implements LocalOnlyHotspotRequestInfo.RequestingApplicationDeathCallback {
        public LocalOnlyRequestorCallback() {
        }

        public void onLocalOnlyHotspotRequestorDeath(LocalOnlyHotspotRequestInfo requestor) {
            WifiServiceImpl.this.unregisterCallingAppAndStopLocalOnlyHotspot(requestor);
        }
    }

    private class AsyncChannelExternalClientHandler extends WifiHandler {
        AsyncChannelExternalClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            super.handleMessage(msg);
            switch (message.what) {
                case 69633:
                    WifiServiceImpl.this.mFrameworkFacade.makeWifiAsyncChannel(WifiServiceImpl.TAG).connect(WifiServiceImpl.this.mContext, this, message.replyTo);
                    return;
                case 151553:
                    if (checkPrivilegedPermissionsAndReplyIfNotAuthorized(message, 151554)) {
                        WifiConfiguration config = (WifiConfiguration) message.obj;
                        int networkId = message.arg1;
                        String nameForUid = WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid);
                        Slog.d(WifiServiceImpl.TAG, "CONNECT  nid=" + Integer.toString(networkId) + " config=" + config + " uid=" + message.sendingUid + " name=" + nameForUid);
                        boolean hasPassword = false;
                        if (config != null) {
                            if (config.preSharedKey != null) {
                                hasPassword = config.preSharedKey.length() > 8;
                            } else if (!(config.wepKeys == null || config.wepKeys[0] == null)) {
                                hasPassword = config.wepKeys[0].length() >= 4;
                            }
                            WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        } else if (config != null || networkId == -1) {
                            Slog.e(WifiServiceImpl.TAG, "AsyncChannelExternalClientHandler.handleMessage ignoring invalid msg=" + message);
                            replyFailed(message, 151554, 8);
                        } else {
                            WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        }
                        if (config == null && WifiServiceImpl.this.mClientModeImplChannel != null) {
                            config = WifiServiceImpl.this.mClientModeImpl.syncGetSpecificNetwork(WifiServiceImpl.this.mClientModeImplChannel, networkId);
                        }
                        if (config != null) {
                            WifiServiceImpl.this.mClientModeImpl.report(103, ReportUtil.getReportDataForWifiManagerConnectApi(true, config.networkId, config.SSID, "connect", nameForUid, hasPassword));
                            return;
                        }
                        return;
                    }
                    return;
                case 151556:
                    if (checkPrivilegedPermissionsAndReplyIfNotAuthorized(message, 151557)) {
                        if (WifiServiceImpl.this.mAutoWifiController != null) {
                            WifiServiceImpl.this.mAutoWifiController.forgetNetwork(message.arg1);
                        }
                        WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        if (WifiServiceImpl.this.mWifiConnectivityMonitor != null) {
                            WifiServiceImpl.this.mWifiConnectivityMonitor.networkRemoved(message.arg1);
                        }
                        WifiServiceImpl.this.mClientModeImpl.report(102, ReportUtil.getReportDataForWifiManagerApi(message.arg1, "forget", WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid), "unknown"));
                        return;
                    }
                    return;
                case 151559:
                    if (checkPrivilegedPermissionsAndReplyIfNotAuthorized(message, 151560)) {
                        WifiConfiguration config2 = (WifiConfiguration) message.obj;
                        int networkId2 = message.arg1;
                        Slog.d(WifiServiceImpl.TAG, "SAVE nid=" + Integer.toString(networkId2) + " config=" + config2 + " uid=" + message.sendingUid + " name=" + WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid));
                        if (config2 != null) {
                            boolean hasPassword2 = false;
                            if (config2.preSharedKey != null) {
                                hasPassword2 = config2.preSharedKey.length() > 8;
                            } else if (!(config2.wepKeys == null || config2.wepKeys[0] == null)) {
                                hasPassword2 = config2.wepKeys[0].length() >= 4;
                            }
                            WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                            WifiServiceImpl.this.mClientModeImpl.report(105, ReportUtil.getReportDataForWifiManagerAddOrUpdateApi(config2.networkId, hasPassword2, "save", WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid), WifiServiceImpl.this.getPackageName(Binder.getCallingPid())));
                            return;
                        }
                        Slog.e(WifiServiceImpl.TAG, "AsyncChannelExternalClientHandler.handleMessage ignoring invalid msg=" + message);
                        replyFailed(message, 151560, 8);
                        return;
                    }
                    return;
                case 151569:
                    if (checkPrivilegedPermissionsAndReplyIfNotAuthorized(message, 151570)) {
                        WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        WifiServiceImpl.this.mClientModeImpl.report(101, ReportUtil.getReportDataForWifiManagerApi(message.arg1, "disable", WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid), "unknown"));
                        return;
                    }
                    return;
                case 151572:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151574)) {
                        WifiServiceImpl.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        return;
                    }
                    return;
                case 151752:
                    if (WifiServiceImpl.this.mIWCMonitor != null) {
                        WifiServiceImpl.this.mIWCMonitor.sendMessage(message.what, message.sendingUid, message.arg1, message.obj);
                        Log.e(WifiServiceImpl.TAG, "nid: " + message.arg1 + " uid: " + message.sendingUid);
                        return;
                    }
                    return;
                case 151753:
                    if (WifiServiceImpl.this.mIWCMonitor != null) {
                        WifiServiceImpl.this.mIWCMonitor.sendMessage(message.what, message.sendingUid, message.arg1, message.obj);
                        Log.e(WifiServiceImpl.TAG, "nid: " + message.arg1 + " uid: " + message.sendingUid);
                        return;
                    }
                    return;
                default:
                    Slog.d(WifiServiceImpl.TAG, "AsyncChannelExternalClientHandler.handleMessage ignoring msg=" + message);
                    return;
            }
        }

        private boolean checkChangePermissionAndReplyIfNotAuthorized(Message msg, int replyWhat) {
            if (WifiServiceImpl.this.mWifiPermissionsUtil.checkChangePermission(msg.sendingUid)) {
                return true;
            }
            Slog.e(WifiServiceImpl.TAG, "AsyncChannelExternalClientHandler.handleMessage ignoring unauthorized msg=" + msg);
            replyFailed(msg, replyWhat, 9);
            return false;
        }

        private boolean checkPrivilegedPermissionsAndReplyIfNotAuthorized(Message msg, int replyWhat) {
            if (WifiServiceImpl.this.isPrivileged(-1, msg.sendingUid)) {
                return true;
            }
            Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring unauthorized msg=" + msg);
            replyFailed(msg, replyWhat, 9);
            return false;
        }

        private void replyFailed(Message msg, int what, int why) {
            if (msg.replyTo != null) {
                Message reply = Message.obtain();
                reply.what = what;
                reply.arg1 = why;
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private class ClientModeImplHandler extends WifiHandler {
        private AsyncChannel mCmiChannel;

        ClientModeImplHandler(String tag, Looper looper, AsyncChannel asyncChannel) {
            super(tag, looper);
            this.mCmiChannel = asyncChannel;
            this.mCmiChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mClientModeImpl.getHandler());
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i != 69632) {
                if (i != 69636) {
                    Slog.d(WifiServiceImpl.TAG, "ClientModeImplHandler.handleMessage ignoring msg=" + msg);
                    return;
                }
                Slog.e(WifiServiceImpl.TAG, "ClientModeImpl channel lost, msg.arg1 =" + msg.arg1);
                WifiServiceImpl wifiServiceImpl = WifiServiceImpl.this;
                wifiServiceImpl.mClientModeImplChannel = null;
                this.mCmiChannel.connect(wifiServiceImpl.mContext, this, WifiServiceImpl.this.mClientModeImpl.getHandler());
            } else if (msg.arg1 == 0) {
                WifiServiceImpl.this.mClientModeImplChannel = this.mCmiChannel;
            } else {
                Slog.e(WifiServiceImpl.TAG, "ClientModeImpl connection failure, error=" + msg.arg1);
                WifiServiceImpl.this.mClientModeImplChannel = null;
            }
        }
    }

    public WifiServiceImpl(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        this.mContext = context;
        this.mWifiInjector = wifiInjector;
        this.mClock = wifiInjector.getClock();
        this.mFacade = this.mWifiInjector.getFrameworkFacade();
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mWifiTrafficPoller = this.mWifiInjector.getWifiTrafficPoller();
        this.mWifiTrafficPoller.setService(this);
        this.mUserManager = this.mWifiInjector.getUserManager();
        this.mCountryCode = this.mWifiInjector.getWifiCountryCode();
        this.mClientModeImpl = this.mWifiInjector.getClientModeImpl();
        this.mActiveModeWarden = this.mWifiInjector.getActiveModeWarden();
        this.mClientModeImpl.enableRssiPolling(true);
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mSettingsStore = this.mWifiInjector.getWifiSettingsStore();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mWifiLockManager = this.mWifiInjector.getWifiLockManager();
        this.mWifiMulticastLockManager = this.mWifiInjector.getWifiMulticastLockManager();
        HandlerThread wifiServiceHandlerThread = this.mWifiInjector.getWifiServiceHandlerThread();
        this.mAsyncChannelExternalClientHandler = new AsyncChannelExternalClientHandler(TAG, wifiServiceHandlerThread.getLooper());
        this.mClientModeImplHandler = new ClientModeImplHandler(TAG, wifiServiceHandlerThread.getLooper(), asyncChannel);
        this.mWifiController = this.mWifiInjector.getWifiController();
        this.mWifiBackupRestore = this.mWifiInjector.getWifiBackupRestore();
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mLog = this.mWifiInjector.makeLog(TAG);
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
        this.mIfaceIpModes = new ConcurrentHashMap<>();
        this.mLocalOnlyHotspotRequests = new HashMap<>();
        enableVerboseLoggingInternal(getVerboseLoggingLevel());
        this.mRegisteredSoftApCallbacks = new ExternalCallbackTracker<>(this.mClientModeImplHandler);
        this.mWifiInjector.getActiveModeWarden().registerSoftApCallback(new SoftApCallbackImpl());
        this.mPowerProfile = this.mWifiInjector.getPowerProfile();
        this.mWifiNative = this.mWifiInjector.getWifiNative();
        this.mWifiNetworkSuggestionsManager = this.mWifiInjector.getWifiNetworkSuggestionsManager();
        this.mDppManager = this.mWifiInjector.getDppManager();
        this.mDefaultApController = WifiDefaultApController.init(this.mContext, this, this.mClientModeImpl, this.mWifiInjector);
        this.mMobileWipsFrameworkService = MobileWipsFrameworkService.init(this.mContext);
        this.mPasspointManager = this.mWifiInjector.getPasspointManager();
        this.mPasspointDefaultProvider = new PasspointDefaultProvider(this.mContext, this);
        this.mWifiGuiderManagementService = new WifiGuiderManagementService(this.mContext, this.mWifiInjector);
        this.mWifiGuiderPackageMonitor = new WifiGuiderPackageMonitor();
        this.mWifiGuiderPackageMonitor.registerApi("android.bluetooth.BluetoothSocket.connect()", ReportIdKey.ID_DHCP_FAIL, 20);
        this.mWifiEnableWarningPolicy = new WifiEnableWarningPolicy(this.mContext);
        this.mWifiRoamingAssistant = WifiRoamingAssistant.init(this.mContext);
        this.mDevicePolicyManager = WifiDevicePolicyManager.init(this.mContext, this, this.mClientModeImpl);
        if (!FactoryTest.isFactoryBinary()) {
            boolean z = SUPPORTMOBILEAPENHANCED_WIFI_ONLY_LITE;
            this.mSemWifiApSmartLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 256 : 1024);
            this.mSemWifiApSmartUtil = new SemWifiApSmartUtil(this.mContext, this.mSemWifiApSmartLocalLog);
            this.mSemWifiApSmartGattServer = new SemWifiApSmartGattServer(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
            this.mWifiInjector.setSemWifiApSmartGattServer(this.mSemWifiApSmartGattServer);
            this.mSemWifiApSmartMHS = new SemWifiApSmartMHS(this.mContext, this.mSemWifiApSmartUtil, this.mDevicePolicyManager, this.mSemWifiApSmartLocalLog);
            this.mWifiInjector.setSemWifiApSmartMHS(this.mSemWifiApSmartMHS);
            this.mSemWifiApSmartClient = new SemWifiApSmartClient(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
            this.mWifiInjector.setSemWifiApSmartClient(this.mSemWifiApSmartClient);
            this.mSemWifiApSmartGattClient = new SemWifiApSmartGattClient(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
            this.mWifiInjector.setSemWifiApSmartGattClient(this.mSemWifiApSmartGattClient);
            this.mSemWifiApSmartGattClient.registerSemWifiApSmartCallback(new SemWifiApSmartCallbackImpl());
            this.mSemWifiApSmartBleScanner = new SemWifiApSmartBleScanner(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
            this.mWifiInjector.setSemWifiApSmartBleScanner(this.mSemWifiApSmartBleScanner);
            if (SUPPORTMOBILEAPENHANCED_D2D || SUPPORTMOBILEAPENHANCED_WIFI_ONLY_LITE) {
                this.mSemWifiApSmartD2DGattClient = new SemWifiApSmartD2DGattClient(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
                this.mSemWifiApSmartD2DMHS = new SemWifiApSmartD2DMHS(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
                this.mSemWifiApSmartD2DClient = new SemWifiApSmartD2DClient(this.mContext, this.mSemWifiApSmartUtil, this.mSemWifiApSmartLocalLog);
                this.mWifiInjector.setSemWifiApSmartD2DMHS(this.mSemWifiApSmartD2DMHS);
                this.mWifiInjector.setSemWifiApSmartD2DClient(this.mSemWifiApSmartD2DClient);
                this.mWifiInjector.setSemWifiApSmartD2DGattClient(this.mSemWifiApSmartD2DGattClient);
            }
        }
        this.mSemWifiApTrafficPoller = new SemWifiApTrafficPoller(this.mContext, this.mWifiNative);
        this.mWifiInjector.setSemWifiApTrafficPoller(this.mSemWifiApTrafficPoller);
        this.mRegisteredSemWifiApSmartCallbacks = new ConcurrentHashMap<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Slog.i(WifiServiceImpl.TAG, "android.intent.action.BOOT_COMPLETED");
                if ("SPRINT".equals(WifiServiceImpl.CONFIGOPBRANDINGFORMOBILEAP)) {
                    try {
                        int unused = WifiServiceImpl.this.mTetheredData = Settings.Secure.getInt(WifiServiceImpl.this.mContext.getContentResolver(), "chameleon_tethereddata");
                        Slog.d(WifiServiceImpl.TAG, "Boot_completed, mTetheredData = " + WifiServiceImpl.this.mTetheredData);
                    } catch (Settings.SettingNotFoundException e) {
                        Slog.d(WifiServiceImpl.TAG, "Settings.SettingNotFoundException for CHAMELEON_TETHEREDDATA");
                        int wifiApState = WifiServiceImpl.this.getWifiApEnabledState();
                        if (wifiApState == 12 && wifiApState == 13) {
                            WifiServiceImpl.this.stopSoftAp();
                            try {
                                Thread.sleep(600);
                            } catch (InterruptedException e2) {
                                e2.printStackTrace();
                            }
                            int unused2 = WifiServiceImpl.this.mTetheredData = SystemProperties.getInt("persist.sys.tether_data", -1);
                            Settings.Secure.putInt(WifiServiceImpl.this.mContext.getContentResolver(), "chameleon_tethereddata", WifiServiceImpl.this.mTetheredData);
                            return;
                        }
                        int unused3 = WifiServiceImpl.this.mTetheredData = SystemProperties.getInt("persist.sys.tether_data", -1);
                        Settings.Secure.putInt(WifiServiceImpl.this.mContext.getContentResolver(), "chameleon_tethereddata", WifiServiceImpl.this.mTetheredData);
                    }
                }
            }
        }, filter);
        if ("SPRINT".equals(CONFIGOPBRANDINGFORMOBILEAP)) {
            this.mTetheredData = 2;
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    boolean unused = WifiServiceImpl.this.mChameleonEnabled = true;
                    String mTempTetheredData = intent.getStringExtra("chameleon_wifi_tetheredData");
                    String mTempSSid = intent.getStringExtra("chameleon_wifi_ssid");
                    if (mTempSSid != null) {
                        Slog.d(WifiServiceImpl.TAG, "[onReceive] CHAMELEON Tethering.SSID : " + mTempSSid);
                        WifiConfiguration wifiConfig = WifiServiceImpl.this.getWifiApConfiguration();
                        wifiConfig.SSID = mTempSSid;
                        WifiServiceImpl.this.setWifiApConfiguration(wifiConfig, WifiServiceImpl.SETTINGS_PACKAGE_NAME);
                    }
                    if (mTempTetheredData != null) {
                        int unused2 = WifiServiceImpl.this.mTetheredData = Integer.parseInt(mTempTetheredData);
                        Slog.d(WifiServiceImpl.TAG, "[onReceive] CHAMELEON mTetheredData : " + WifiServiceImpl.this.mTetheredData);
                    }
                    if (0 != 0) {
                        int unused3 = WifiServiceImpl.this.mMaxUser = Integer.parseInt((String) null);
                        Slog.d(WifiServiceImpl.TAG, "[onReceive] mMaxUser : " + WifiServiceImpl.this.mMaxUser);
                    }
                    int unused4 = WifiServiceImpl.this.mGsmMaxUser = Integer.parseInt("1");
                    Slog.d(WifiServiceImpl.TAG, "[onReceive] mGsmMaxUser : " + WifiServiceImpl.this.mGsmMaxUser);
                    int unused5 = WifiServiceImpl.this.mDomRoamMaxUser = Integer.parseInt("8");
                    Slog.d(WifiServiceImpl.TAG, "[onReceive] mDomRoamMaxUser : " + WifiServiceImpl.this.mDomRoamMaxUser);
                    int unused6 = WifiServiceImpl.this.mIntRoamMaxUser = Integer.parseInt("8");
                    Slog.d(WifiServiceImpl.TAG, "[onReceive] mIntRoamMaxUser : " + WifiServiceImpl.this.mIntRoamMaxUser);
                    Slog.d(WifiServiceImpl.TAG, "[setValue] mTetheredData = " + WifiServiceImpl.this.mTetheredData + ", mMaxUser = " + WifiServiceImpl.this.mMaxUser + ", mGsmMaxUser = " + WifiServiceImpl.this.mGsmMaxUser + ", mDomRoamMaxUser = " + WifiServiceImpl.this.mDomRoamMaxUser + ", mIntRoamMaxUser = " + WifiServiceImpl.this.mIntRoamMaxUser);
                    ContentResolver cr = WifiServiceImpl.this.mContext.getContentResolver();
                    Settings.Secure.putInt(cr, "chameleon_tethereddata", WifiServiceImpl.this.mTetheredData);
                    Settings.System.putInt(cr, "chameleon_maxuser", WifiServiceImpl.this.mMaxUser);
                    Settings.System.putInt(cr, "chameleon_gsmmaxuser", WifiServiceImpl.this.mGsmMaxUser);
                    Settings.System.putInt(cr, "chameleon_domroammaxuser", WifiServiceImpl.this.mDomRoamMaxUser);
                    Settings.System.putInt(cr, "chameleon_introammaxuser", WifiServiceImpl.this.mIntRoamMaxUser);
                    Settings.System.putString(cr, "chameleon_ssid", mTempSSid);
                }
            }, new IntentFilter("com.samsung.sec.android.application.csc.chameleon_wifi"));
        }
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiServiceImpl.this.getWifiApEnabledState() == 13) {
                    WifiServiceImpl.this.stopSoftAp();
                }
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Slog.d(WifiServiceImpl.TAG, "[onReceive] action : " + intent.getAction());
                int bandwidth = intent.getIntExtra("bandwidth", 1);
                Slog.d(WifiServiceImpl.TAG, "[onReceive] BANDWIDTH : " + bandwidth);
                WifiServiceImpl.this.setWifiApRttFeatureBandwidth(bandwidth);
            }
        }, new IntentFilter("com.android.cts.verifier.wifiaware.action.BANDWIDTH"));
    }

    public void setWifiApRttFeatureBandwidth(int bandwidth) {
        Log.d(TAG, "setWifiApRttFeatureBandwidth request:" + bandwidth);
        WifiNative wifiNative = this.mWifiNative;
        if (wifiNative == null) {
            return;
        }
        if (wifiNative.setWifiApRttFeatureBandwidth(bandwidth)) {
            Log.d(TAG, "setWifiApRttFeature set successfully");
        } else {
            Log.e(TAG, "Failed to set/reset the feature");
        }
    }

    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog log) {
        this.mAsyncChannelExternalClientHandler.setWifiLog(log);
    }

    public static String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public void checkAndStartWifi() {
        Log.d(TAG, "checkAndStartWifi start");
        long startAt = System.currentTimeMillis();
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.d(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start wifi.");
            return;
        }
        boolean wifiEnabled = false;
        if (FactoryTest.isFactoryBinary()) {
            Slog.i(TAG, "It's factory binary, do not enable Wi-Fi");
            this.mSettingsStore.handleWifiToggled(false);
        } else {
            wifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("WifiService starting up with Wi-Fi ");
        sb.append(wifiEnabled ? "enabled" : "disabled");
        Slog.i(TAG, sb.toString());
        registerForScanModeChange();
        getTelephonyManager().listen(this.mPhoneStateListener, 65);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiServiceImpl.this.mSettingsStore.handleAirplaneModeToggled()) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155657);
                }
                boolean isAirplaneModeOn = WifiServiceImpl.this.mSettingsStore.isAirplaneModeOn();
                if (isAirplaneModeOn) {
                    Log.d(WifiServiceImpl.TAG, "resetting country code because Airplane mode is ON");
                    WifiServiceImpl.this.mCountryCode.airplaneModeEnabled();
                }
                if (WifiServiceImpl.this.mAutoWifiController != null) {
                    WifiServiceImpl.this.mAutoWifiController.setAirplainMode(isAirplaneModeOn);
                }
                WifiServiceImpl.this.mClientModeImpl.report(8, ReportUtil.getReportDataForChangeState(isAirplaneModeOn));
                WifiServiceImpl.this.mClientModeImpl.setFccChannel();
            }
        }, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("ss");
                if ("ABSENT".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "resetting networks because SIM was removed");
                    WifiServiceImpl.this.mClientModeImpl.resetSimAuthNetworks(false);
                    if (WifiServiceImpl.this.mAutoWifiController != null) {
                        WifiServiceImpl.this.mAutoWifiController.setSimState(TelephonyUtil.isSimCardReady(WifiServiceImpl.this.getTelephonyManager()));
                    }
                } else if ("IMSI".equals(state) || "READY".equals(state)) {
                    if (WifiServiceImpl.this.mAutoWifiController != null) {
                        WifiServiceImpl.this.mAutoWifiController.setSimState(true);
                    }
                } else if ("LOADED".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "resetting networks because SIM was loaded");
                    WifiServiceImpl.this.mClientModeImpl.resetSimAuthNetworks(true);
                }
            }
        }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                WifiServiceImpl.this.handleWifiApStateChange(intent.getIntExtra("wifi_state", 11), intent.getIntExtra("previous_wifi_state", 11), intent.getIntExtra("wifi_ap_error_code", -1), intent.getStringExtra("wifi_ap_interface_name"), intent.getIntExtra("wifi_ap_mode", -1));
            }
        }, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
        registerForBroadcasts();
        this.mInIdleMode = this.mPowerManager.isDeviceIdleMode();
        if (!this.mClientModeImpl.syncInitialize(this.mClientModeImplChannel)) {
            Log.wtf(TAG, "Failed to initialize ClientModeImpl");
        }
        this.mWifiController.start();
        bootUp();
        if (wifiEnabled) {
            WifiMobileDeviceManager.allowToStateChange(true);
            setWifiEnabled(this.mContext.getPackageName(), wifiEnabled);
            WifiMobileDeviceManager.allowToStateChange(false);
        }
        this.mWifiConnectivityMonitor = WifiConnectivityMonitor.makeWifiConnectivityMonitor(this.mContext, this.mClientModeImpl, this.mWifiInjector);
        WifiConnectivityMonitor wifiConnectivityMonitor = this.mWifiConnectivityMonitor;
        if (wifiConnectivityMonitor != null) {
            wifiConnectivityMonitor.setWifiEnabled(wifiEnabled, (String) null);
        }
        if (this.mWifiConnectivityMonitor != null) {
            this.mIWCMonitor = IWCMonitor.initIWCMonitor(this.mContext, this.mWifiInjector);
            this.mIWCMonitor.setWcmAsyncChannel(this.mWifiConnectivityMonitor.getHandler());
            this.mWifiConnectivityMonitor.setIWCMonitorAsyncChannel(this.mIWCMonitor.getHandler());
            this.mClientModeImpl.setIWCMonitorAsyncChannel(this.mIWCMonitor.getHandler());
            Log.d(TAG, "SEC_PRODUCT_FEATURE_WLAN_SUPPORT_INTELLIGENT_WIFI_CONNECTION is enabled!");
        } else {
            Log.d(TAG, "SEC_PRODUCT_FEATURE_WLAN_SUPPORT_INTELLIGENT_WIFI_CONNECTION is true, but mWifiConnectivityMonitor is null");
        }
        checkAndStartAutoWifi();
        checkAndStartMHS();
        long duration = System.currentTimeMillis() - startAt;
        Log.d(TAG, "checkAndStartWifi end at +" + duration + "ms");
        if (duration >= 3000) {
            this.mClientModeImpl.report(16, ReportUtil.getReportDataForInitDelay((int) (duration / 1000)));
        }
    }

    private void checkAndStartMHS() {
        CscParser mParser = new CscParser(CscParser.getCustomerPath());
        this.mCSCRegion = mParser.get("GeneralInfo.Region");
        Log.d(TAG, "mCSCRegion:" + this.mCSCRegion);
        this.mCountryIso = mParser.get("GeneralInfo.CountryISO");
        if (supportWifiSharingLite()) {
            IntentFilter wifiSharingFilter = new IntentFilter();
            wifiSharingFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction()) && intent.getIntExtra("wifi_state", 4) == 3 && WifiServiceImpl.this.isWifiSharingEnabled() && WifiServiceImpl.this.isMobileApOn()) {
                        WifiServiceImpl.this.mAsyncChannelExternalClientHandler.post(new Runnable() {
                            public final void run() {
                                WifiServiceImpl.C04848.this.lambda$onReceive$0$WifiServiceImpl$8();
                            }
                        });
                    }
                }

                public /* synthetic */ void lambda$onReceive$0$WifiServiceImpl$8() {
                    WifiServiceImpl.this.setIndoorChannelsToDriver(true);
                }
            }, wifiSharingFilter);
            mapIndoorCountryToChannel();
        }
        this.mWifiApConfigStore = this.mWifiInjector.getWifiApConfigStore();
        this.mSemWifiApBroadcastReceiver = WifiInjector.getInstance().getSemWifiApBroadcastReceiver();
        this.mSemWifiApBroadcastReceiver.startTracking();
        startSwitchBoardService();
        this.mSemWifiApTrafficPoller.handleBootCompleted();
    }

    private void startSwitchBoardService() {
        Log.e(TAG, "startSwitchBoardService");
        HandlerThread switchboardThread = new HandlerThread("SwitchBoardService");
        switchboardThread.start();
        this.mSwitchBoardService = SwitchBoardService.getInstance(this.mContext, switchboardThread.getLooper(), this.mClientModeImpl);
    }

    private int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(1)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            return 3;
        }
        if (config.wepKeys[0] != null) {
            return 1;
        }
        return 0;
    }

    private int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return 1;
        }
        if (result.capabilities.contains("PSK")) {
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            return 3;
        }
        return 0;
    }

    public void handleBootCompleted() {
        Log.d(TAG, "Handle boot completed");
        if (!FactoryTest.isFactoryBinary()) {
            this.mSemWifiApSmartUtil.handleBootCompleted();
            this.mSemWifiApSmartGattServer.handleBootCompleted();
            this.mSemWifiApSmartMHS.handleBootCompleted();
            this.mSemWifiApSmartClient.handleBootCompleted();
            this.mSemWifiApSmartGattClient.handleBootCompleted();
            this.mSemWifiApSmartBleScanner.handleBootCompleted();
            if (SUPPORTMOBILEAPENHANCED_D2D || SUPPORTMOBILEAPENHANCED_WIFI_ONLY_LITE) {
                this.mSemWifiApSmartD2DGattClient.handleBootCompleted();
                this.mSemWifiApSmartD2DMHS.handleBootCompleted();
                this.mSemWifiApSmartD2DClient.handleBootCompleted();
            }
        }
        this.mPasspointManager.initializeProvisioner(this.mWifiInjector.getPasspointProvisionerHandlerThread().getLooper());
        this.mClientModeImpl.handleBootCompleted();
        if (this.mSemQoSProfileShareProvider == null && this.mContext.getPackageManager().hasSystemFeature("com.samsung.feature.samsung_experience_mobile") && SemFloatingFeature.getInstance().getBoolean("SEC_FLOATING_FEATURE_MCF_SUPPORT_FRAMEWORK")) {
            this.mSemQoSProfileShareProvider = this.mWifiInjector.getQoSProfileShareProvider(new SemWifiProfileAndQoSProvider.Adapter() {
                public int[] getCurrentNetworkScore() {
                    int[] currentNetworkScores = WifiServiceImpl.this.mWifiConnectivityMonitor.getOpenNetworkQosScores();
                    if (currentNetworkScores == null || currentNetworkScores.length != 3) {
                        Log.d(WifiServiceImpl.TAG, "getCurrentNetworkScore - invalid score data");
                        return null;
                    }
                    int[] iArr = new int[4];
                    iArr[0] = WifiServiceImpl.this.mWifiConnectivityMonitor.getOpenNetworkQosNoInternetStatus() ? 2 : 0;
                    iArr[1] = currentNetworkScores[0];
                    iArr[2] = currentNetworkScores[1];
                    iArr[3] = currentNetworkScores[2];
                    return iArr;
                }

                public boolean isWpa3SaeSupported() {
                    return (WifiServiceImpl.this.getSupportedFeatures() & 134217728) != 0;
                }

                public boolean isWifiEnabled() {
                    return WifiServiceImpl.this.getWifiEnabledState() == 3;
                }

                public void startScan() {
                    WifiServiceImpl.this.startScan("android");
                }

                public List<ScanResult> getScanResults() {
                    return WifiServiceImpl.this.getScanResults("android");
                }

                public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
                    if (WifiServiceImpl.this.mClientModeImplChannel != null) {
                        return WifiServiceImpl.this.mClientModeImpl.syncGetPrivilegedConfiguredNetwork(WifiServiceImpl.this.mClientModeImplChannel);
                    }
                    return new ArrayList();
                }

                public WifiConfiguration getSpecificNetwork(int networkId) {
                    return WifiServiceImpl.this.getSpecificNetwork(networkId);
                }

                public WifiInfo getConnectionInfo() {
                    return WifiServiceImpl.this.getConnectionInfo("android");
                }

                public int getWifiApState() {
                    return WifiServiceImpl.this.getWifiApEnabledState();
                }

                public WifiConfiguration getWifiApConfiguration() {
                    if (!WifiServiceImpl.this.isMobileApOn()) {
                        return null;
                    }
                    WifiConfiguration apConfig = WifiServiceImpl.this.getWifiApConfiguration();
                    if (apConfig != null) {
                        String mhsMacAddress = WifiServiceImpl.this.mSemWifiApSmartUtil.getMHSMacFromInterface();
                        if (mhsMacAddress == null) {
                            return null;
                        }
                        apConfig = new WifiConfiguration(apConfig);
                        if (apConfig.SSID != null && !apConfig.SSID.startsWith("\"")) {
                            apConfig.SSID = "\"" + apConfig.SSID + "\"";
                        }
                        if (apConfig.allowedKeyManagement.get(4)) {
                            apConfig.allowedKeyManagement = new BitSet(64);
                            apConfig.allowedKeyManagement.set(1);
                        } else if (apConfig.allowedKeyManagement.get(25) || apConfig.allowedKeyManagement.get(26)) {
                            apConfig.allowedKeyManagement = new BitSet(64);
                            apConfig.allowedKeyManagement.set(8);
                        }
                        apConfig.BSSID = mhsMacAddress;
                    }
                    return apConfig;
                }
            });
            this.mSemQoSProfileShareProvider.checkAndStart();
            this.mWifiConnectivityMonitor.registerOpenNetworkQosCallback(new WifiConnectivityMonitor.OpenNetworkQosCallback() {
                public void onNoInternetStatusChange(boolean valid) {
                    WifiServiceImpl.this.mSemQoSProfileShareProvider.updateQoSData(true, false);
                }

                public void onQualityScoreChanged() {
                    WifiServiceImpl.this.mSemQoSProfileShareProvider.updateQoSData(false, true);
                }
            });
        }
    }

    public void handleUserSwitch(int userId) {
        Log.d(TAG, "Handle user switch " + userId);
        this.mClientModeImpl.handleUserSwitch(userId);
    }

    public void handleUserUnlock(int userId) {
        Log.d(TAG, "Handle user unlock " + userId);
        this.mClientModeImpl.handleUserUnlock(userId);
    }

    public void handleUserStop(int userId) {
        Log.d(TAG, "Handle user stop " + userId);
        this.mClientModeImpl.handleUserStop(userId);
    }

    public boolean startScan(String packageName) {
        return startScan(packageName, (Set<Integer>) null);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0034, code lost:
        if (WIFI_STOP_SCAN_FOR_ETWS == false) goto L_0x0044;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0038, code lost:
        if (r7.mBlockScanFromOthers == false) goto L_0x0044;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003a, code lost:
        android.util.Log.i(TAG, "ETWS: ignore scan");
        r7.mBlockScanFromOthers = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0043, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:?, code lost:
        r7.mWifiPermissionsUtil.enforceCanAccessScanResults(r8, r10);
        r0 = new com.android.server.wifi.util.GeneralUtil.Mutable<>();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0068, code lost:
        if (r7.mWifiInjector.getClientModeImplHandler().runWithScissors(new com.android.server.wifi.$$Lambda$WifiServiceImpl$uJhaCZrKivZlQmLD6fKbGekPXUY(r16, r0, r10, r17, r18), com.android.server.wifi.iwc.IWCEventManager.autoDisconnectThreshold) != false) goto L_0x0079;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x006a, code lost:
        android.util.Log.e(TAG, "Failed to post runnable to start scan");
        sendFailedScanBroadcast();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0078, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0081, code lost:
        if (((java.lang.Boolean) r0.value).booleanValue() != false) goto L_0x008f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0083, code lost:
        android.util.Log.e(TAG, "Failed to start scan");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x008a, code lost:
        android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x008e, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x008f, code lost:
        android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0093, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0094, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0096, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:?, code lost:
        android.util.Slog.e(TAG, "Permission violation - startScan not allowed for uid=" + r10 + ", packageName=" + r8 + ", reason=" + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00c1, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00c2, code lost:
        android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00c5, code lost:
        throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startScan(java.lang.String r17, java.util.Set<java.lang.Integer> r18) {
        /*
            r16 = this;
            r7 = r16
            r8 = r17
            int r0 = r16.enforceChangePermission(r17)
            r9 = 0
            if (r0 == 0) goto L_0x000c
            return r9
        L_0x000c:
            int r10 = android.os.Binder.getCallingUid()
            long r11 = android.os.Binder.clearCallingIdentity()
            com.android.server.wifi.WifiLog r0 = r7.mLog
            java.lang.String r1 = "startScan uid=%"
            com.android.server.wifi.WifiLog$LogMessage r0 = r0.info(r1)
            long r1 = (long) r10
            com.android.server.wifi.WifiLog$LogMessage r0 = r0.mo2069c((long) r1)
            r0.flush()
            monitor-enter(r16)
            boolean r0 = r7.mInIdleMode     // Catch:{ all -> 0x00c6 }
            r13 = 1
            if (r0 == 0) goto L_0x0031
            r16.sendFailedScanBroadcast()     // Catch:{ all -> 0x00c6 }
            r7.mScanPending = r13     // Catch:{ all -> 0x00c6 }
            monitor-exit(r16)     // Catch:{ all -> 0x00c6 }
            return r9
        L_0x0031:
            monitor-exit(r16)     // Catch:{ all -> 0x00c6 }
            boolean r0 = WIFI_STOP_SCAN_FOR_ETWS
            if (r0 == 0) goto L_0x0044
            boolean r0 = r7.mBlockScanFromOthers
            if (r0 == 0) goto L_0x0044
            java.lang.String r0 = "WifiService"
            java.lang.String r1 = "ETWS: ignore scan"
            android.util.Log.i(r0, r1)
            r7.mBlockScanFromOthers = r9
            return r9
        L_0x0044:
            com.android.server.wifi.util.WifiPermissionsUtil r0 = r7.mWifiPermissionsUtil     // Catch:{ SecurityException -> 0x0096 }
            r0.enforceCanAccessScanResults(r8, r10)     // Catch:{ SecurityException -> 0x0096 }
            com.android.server.wifi.util.GeneralUtil$Mutable r0 = new com.android.server.wifi.util.GeneralUtil$Mutable     // Catch:{ SecurityException -> 0x0096 }
            r0.<init>()     // Catch:{ SecurityException -> 0x0096 }
            com.android.server.wifi.WifiInjector r1 = r7.mWifiInjector     // Catch:{ SecurityException -> 0x0096 }
            android.os.Handler r14 = r1.getClientModeImplHandler()     // Catch:{ SecurityException -> 0x0096 }
            com.android.server.wifi.-$$Lambda$WifiServiceImpl$uJhaCZrKivZlQmLD6fKbGekPXUY r15 = new com.android.server.wifi.-$$Lambda$WifiServiceImpl$uJhaCZrKivZlQmLD6fKbGekPXUY     // Catch:{ SecurityException -> 0x0096 }
            r1 = r15
            r2 = r16
            r3 = r0
            r4 = r10
            r5 = r17
            r6 = r18
            r1.<init>(r3, r4, r5, r6)     // Catch:{ SecurityException -> 0x0096 }
            r1 = 4000(0xfa0, double:1.9763E-320)
            boolean r1 = r14.runWithScissors(r15, r1)     // Catch:{ SecurityException -> 0x0096 }
            if (r1 != 0) goto L_0x0079
            java.lang.String r2 = "WifiService"
            java.lang.String r3 = "Failed to post runnable to start scan"
            android.util.Log.e(r2, r3)     // Catch:{ SecurityException -> 0x0096 }
            r16.sendFailedScanBroadcast()     // Catch:{ SecurityException -> 0x0096 }
            android.os.Binder.restoreCallingIdentity(r11)
            return r9
        L_0x0079:
            E r2 = r0.value     // Catch:{ SecurityException -> 0x0096 }
            java.lang.Boolean r2 = (java.lang.Boolean) r2     // Catch:{ SecurityException -> 0x0096 }
            boolean r2 = r2.booleanValue()     // Catch:{ SecurityException -> 0x0096 }
            if (r2 != 0) goto L_0x008f
            java.lang.String r2 = "WifiService"
            java.lang.String r3 = "Failed to start scan"
            android.util.Log.e(r2, r3)     // Catch:{ SecurityException -> 0x0096 }
            android.os.Binder.restoreCallingIdentity(r11)
            return r9
        L_0x008f:
            android.os.Binder.restoreCallingIdentity(r11)
            return r13
        L_0x0094:
            r0 = move-exception
            goto L_0x00c2
        L_0x0096:
            r0 = move-exception
            java.lang.String r1 = "WifiService"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0094 }
            r2.<init>()     // Catch:{ all -> 0x0094 }
            java.lang.String r3 = "Permission violation - startScan not allowed for uid="
            r2.append(r3)     // Catch:{ all -> 0x0094 }
            r2.append(r10)     // Catch:{ all -> 0x0094 }
            java.lang.String r3 = ", packageName="
            r2.append(r3)     // Catch:{ all -> 0x0094 }
            r2.append(r8)     // Catch:{ all -> 0x0094 }
            java.lang.String r3 = ", reason="
            r2.append(r3)     // Catch:{ all -> 0x0094 }
            r2.append(r0)     // Catch:{ all -> 0x0094 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0094 }
            android.util.Slog.e(r1, r2)     // Catch:{ all -> 0x0094 }
            android.os.Binder.restoreCallingIdentity(r11)
            return r9
        L_0x00c2:
            android.os.Binder.restoreCallingIdentity(r11)
            throw r0
        L_0x00c6:
            r0 = move-exception
            monitor-exit(r16)     // Catch:{ all -> 0x00c6 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.startScan(java.lang.String, java.util.Set):boolean");
    }

    public /* synthetic */ void lambda$startScan$0$WifiServiceImpl(GeneralUtil.Mutable scanSuccess, int callingUid, String packageName, Set freqs) {
        scanSuccess.value = Boolean.valueOf(this.mScanRequestProxy.startScan(callingUid, packageName, freqs));
    }

    public void semStartPartialChannelScan(int[] frequencies, String packageName) {
        Log.i(TAG, "semStartPartialChannelScan uid :" + Binder.getCallingUid() + ", package : " + packageName);
        startScan(packageName, getPartialChannelScanSettings(frequencies));
    }

    private Set<Integer> getPartialChannelScanSettings(int[] freqs) {
        Set<Integer> mFreqs = new HashSet<>();
        for (int freq : freqs) {
            if (!mAllFreqArray.contains(Integer.valueOf(freq))) {
                Log.w(TAG, "getPartialChannelScanSettings: ignore freq = " + freq);
            } else {
                mFreqs.add(Integer.valueOf(freq));
            }
        }
        return mFreqs;
    }

    private void sendFailedScanBroadcast() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            if ("VZW".equals(SemCscFeature.getInstance().getString(CscFeatureTagCOMMON.TAG_CSCFEATURE_COMMON_CONFIGIMPLICITBROADCASTS))) {
                Intent cloneIntent = (Intent) intent.clone();
                cloneIntent.setPackage("com.verizon.mips.services");
                this.mContext.sendBroadcastAsUser(cloneIntent, UserHandle.ALL);
            }
            if ("WeChatWiFi".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSOCIALSVCINTEGRATIONN))) {
                Intent cloneIntent2 = (Intent) intent.clone();
                cloneIntent2.setPackage("com.samsung.android.wechatwifiservice");
                this.mContext.sendBroadcastAsUser(cloneIntent2, UserHandle.ALL);
            }
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken() {
        enforceConnectivityInternalPermission();
        if (!this.mVerboseLoggingEnabled) {
            return null;
        }
        this.mLog.info("getCurrentNetworkWpsNfcConfigurationToken uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        return null;
    }

    /* access modifiers changed from: package-private */
    public void handleIdleModeChanged() {
        boolean doScan = false;
        synchronized (this) {
            boolean idle = this.mPowerManager.isDeviceIdleMode();
            if (this.mInIdleMode != idle) {
                this.mInIdleMode = idle;
                if (!idle && this.mScanPending) {
                    this.mScanPending = false;
                    doScan = true;
                }
            }
        }
        if (doScan) {
            startScan(this.mContext.getOpPackageName());
        }
    }

    private boolean checkNetworkSettingsPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.NETWORK_SETTINGS", pid, uid) == 0;
    }

    private boolean checkNetworkSetupWizardPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.NETWORK_SETUP_WIZARD", pid, uid) == 0;
    }

    private boolean checkNetworkStackPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.NETWORK_STACK", pid, uid) == 0;
    }

    private boolean checkNetworkManagedProvisioningPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.NETWORK_MANAGED_PROVISIONING", pid, uid) == 0;
    }

    /* access modifiers changed from: private */
    public boolean isPrivileged(int pid, int uid) {
        return checkNetworkSettingsPermission(pid, uid) || checkNetworkSetupWizardPermission(pid, uid) || checkNetworkStackPermission(pid, uid) || checkNetworkManagedProvisioningPermission(pid, uid);
    }

    private boolean isSettingsOrSuw(int pid, int uid) {
        return checkNetworkSettingsPermission(pid, uid) || checkNetworkSetupWizardPermission(pid, uid);
    }

    private boolean isSystemOrPlatformSigned(String packageName) {
        long ident = Binder.clearCallingIdentity();
        boolean z = false;
        try {
            ApplicationInfo info = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (info.isSystemApp() || info.isUpdatedSystemApp() || info.isSignedWithPlatformKey()) {
                z = true;
            }
            return z;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isDeviceOrProfileOwner(int uid) {
        DevicePolicyManagerInternal dpmi = this.mWifiInjector.getWifiPermissionsWrapper().getDevicePolicyManagerInternal();
        if (dpmi == null) {
            return false;
        }
        if (dpmi.isActiveAdminWithPolicy(uid, -2) || dpmi.isActiveAdminWithPolicy(uid, -1)) {
            return true;
        }
        return false;
    }

    private void enforceNetworkSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceAccessNetworkPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
    }

    private boolean checkAccessSecuredPermission(int pid, int uid) {
        if (this.mContext.checkPermission("com.samsung.permission.SEM_ACCESS_WIFI_SECURED_INFO", pid, uid) == 0) {
            return true;
        }
        return false;
    }

    private void enforceProvideDiagnosticsPermission() {
        this.mContext.enforceCallingOrSelfPermission("com.samsung.permission.WIFI_DIAGNOSTICS_PROVIDER", TAG);
    }

    private void enforceFactoryTestPermission() {
        this.mContext.enforceCallingOrSelfPermission("com.samsung.permission.WIFI_FACTORY_TEST", TAG);
    }

    private int enforceChangePermission(String callingPackage) {
        if (checkNetworkSettingsPermission(Binder.getCallingPid(), Binder.getCallingUid())) {
            return 0;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
        return this.mAppOps.noteOp("android:change_wifi_state", Binder.getCallingUid(), callingPackage);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationHardwarePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", "LocationHardware");
    }

    private void enforceReadCredentialPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_WIFI_CREDENTIAL", TAG);
    }

    private void enforceWorkSourcePermission() {
        this.mContext.enforceCallingPermission("android.permission.UPDATE_DEVICE_STATS", TAG);
    }

    private void enforceMulticastChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_MULTICAST_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
    }

    private void enforceLocationPermission(String pkgName, int uid) {
        this.mWifiPermissionsUtil.enforceLocationPermission(pkgName, uid);
    }

    private boolean isTargetSdkLessThanQOrPrivileged(String packageName, int pid, int uid) {
        return this.mWifiPermissionsUtil.isTargetSdkLessThan(packageName, 29) || isPrivileged(pid, uid) || isDeviceOrProfileOwner(uid) || isSystemOrPlatformSigned(packageName) || this.mWifiPermissionsUtil.checkSystemAlertWindowPermission(uid, packageName);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:158:0x0266, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean setWifiEnabled(java.lang.String r13, boolean r14) {
        /*
            r12 = this;
            monitor-enter(r12)
            int r0 = r12.enforceChangePermission(r13)     // Catch:{ all -> 0x0271 }
            r1 = 0
            if (r0 == 0) goto L_0x000a
            monitor-exit(r12)
            return r1
        L_0x000a:
            int r0 = android.os.Binder.getCallingPid()     // Catch:{ all -> 0x0271 }
            int r2 = android.os.Binder.getCallingUid()     // Catch:{ all -> 0x0271 }
            boolean r0 = r12.isPrivileged(r0, r2)     // Catch:{ all -> 0x0271 }
            if (r0 != 0) goto L_0x004a
            com.android.server.wifi.util.WifiPermissionsUtil r2 = r12.mWifiPermissionsUtil     // Catch:{ all -> 0x0271 }
            r3 = 29
            boolean r2 = r2.isTargetSdkLessThan(r13, r3)     // Catch:{ all -> 0x0271 }
            if (r2 != 0) goto L_0x004a
            com.android.server.wifi.util.WifiPermissionsUtil r2 = r12.mWifiPermissionsUtil     // Catch:{ all -> 0x0271 }
            int r3 = android.os.Binder.getCallingUid()     // Catch:{ all -> 0x0271 }
            boolean r2 = r2.checkFactoryTestPermission(r3)     // Catch:{ all -> 0x0271 }
            if (r2 != 0) goto L_0x004a
            boolean r2 = r12.isSystemOrPlatformSigned(r13)     // Catch:{ all -> 0x0271 }
            if (r2 != 0) goto L_0x004a
            com.android.server.wifi.WifiLog r2 = r12.mLog     // Catch:{ all -> 0x0271 }
            java.lang.String r3 = "setWifiEnabled not allowed for uid=%"
            com.android.server.wifi.WifiLog$LogMessage r2 = r2.info(r3)     // Catch:{ all -> 0x0271 }
            int r3 = android.os.Binder.getCallingUid()     // Catch:{ all -> 0x0271 }
            long r3 = (long) r3     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiLog$LogMessage r2 = r2.mo2069c((long) r3)     // Catch:{ all -> 0x0271 }
            r2.flush()     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x004a:
            com.android.server.wifi.WifiSettingsStore r2 = r12.mSettingsStore     // Catch:{ all -> 0x0271 }
            boolean r2 = r2.isAirplaneModeOn()     // Catch:{ all -> 0x0271 }
            if (r2 == 0) goto L_0x0069
            if (r0 != 0) goto L_0x0069
            java.lang.String r2 = "com.android.bluetooth"
            boolean r2 = r2.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r2 != 0) goto L_0x0069
            com.android.server.wifi.WifiLog r2 = r12.mLog     // Catch:{ all -> 0x0271 }
            java.lang.String r3 = "setWifiEnabled in Airplane mode: only isPrivileged apps or bluetooth can toggle wifi"
            com.android.server.wifi.WifiLog$LogMessage r2 = r2.err(r3)     // Catch:{ all -> 0x0271 }
            r2.flush()     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x0069:
            int r2 = r12.mWifiApState     // Catch:{ all -> 0x0271 }
            r3 = 13
            r4 = 1
            if (r2 != r3) goto L_0x0072
            r2 = r4
            goto L_0x0073
        L_0x0072:
            r2 = r1
        L_0x0073:
            if (r2 == 0) goto L_0x0084
            if (r0 != 0) goto L_0x0084
            com.android.server.wifi.WifiLog r3 = r12.mLog     // Catch:{ all -> 0x0271 }
            java.lang.String r4 = "setWifiEnabled SoftAp enabled: only Settings can toggle wifi"
            com.android.server.wifi.WifiLog$LogMessage r3 = r3.err(r4)     // Catch:{ all -> 0x0271 }
            r3.flush()     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x0084:
            java.lang.String r3 = "yy/MM/dd kk:mm:ss "
            long r5 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0271 }
            java.lang.CharSequence r3 = android.text.format.DateFormat.format(r3, r5)     // Catch:{ all -> 0x0271 }
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x0271 }
            r5.<init>()     // Catch:{ all -> 0x0271 }
            r5.append(r3)     // Catch:{ all -> 0x0271 }
            java.lang.String r6 = " WifiManager.setWifiEnabled("
            r5.append(r6)     // Catch:{ all -> 0x0271 }
            r5.append(r14)     // Catch:{ all -> 0x0271 }
            java.lang.String r6 = ") : "
            r5.append(r6)     // Catch:{ all -> 0x0271 }
            r5.append(r13)     // Catch:{ all -> 0x0271 }
            java.lang.String r6 = "\n"
            r5.append(r6)     // Catch:{ all -> 0x0271 }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x0271 }
            r12.addHistoricalDumpLog(r5)     // Catch:{ all -> 0x0271 }
            android.content.Context r5 = r12.mContext     // Catch:{ all -> 0x0271 }
            com.samsung.android.server.wifi.WifiControlHistoryProvider.setControlHistory(r5, r13, r14)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiLog r5 = r12.mLog     // Catch:{ all -> 0x0271 }
            java.lang.String r6 = "setWifiEnabled package=% uid=% enable=%"
            com.android.server.wifi.WifiLog$LogMessage r5 = r5.info(r6)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiLog$LogMessage r5 = r5.mo2070c((java.lang.String) r13)     // Catch:{ all -> 0x0271 }
            int r6 = android.os.Binder.getCallingUid()     // Catch:{ all -> 0x0271 }
            long r6 = (long) r6     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiLog$LogMessage r5 = r5.mo2069c((long) r6)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiLog$LogMessage r5 = r5.mo2071c((boolean) r14)     // Catch:{ all -> 0x0271 }
            r5.flush()     // Catch:{ all -> 0x0271 }
            long r5 = android.os.Binder.clearCallingIdentity()     // Catch:{ all -> 0x0271 }
            android.content.Context r7 = r12.mContext     // Catch:{ all -> 0x026c }
            boolean r7 = com.samsung.android.server.wifi.WifiMobileDeviceManager.isAllowToUseWifi(r7, r14)     // Catch:{ all -> 0x026c }
            if (r7 != 0) goto L_0x00f0
            com.android.server.wifi.WifiLog r4 = r12.mLog     // Catch:{ all -> 0x026c }
            java.lang.String r7 = "setWifiEnabled disallow to use wifi: disabled by mdm"
            com.android.server.wifi.WifiLog$LogMessage r4 = r4.info(r7)     // Catch:{ all -> 0x026c }
            r4.flush()     // Catch:{ all -> 0x026c }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x00f0:
            if (r14 == 0) goto L_0x010b
            com.samsung.android.server.wifi.WifiDevicePolicyManager r7 = r12.mDevicePolicyManager     // Catch:{ all -> 0x026c }
            boolean r7 = r7.isAllowToUseWifi()     // Catch:{ all -> 0x026c }
            if (r7 != 0) goto L_0x010b
            com.android.server.wifi.WifiLog r4 = r12.mLog     // Catch:{ all -> 0x026c }
            java.lang.String r7 = "setWifiEnabled disallow to use wifi: disabled by dpm"
            com.android.server.wifi.WifiLog$LogMessage r4 = r4.info(r7)     // Catch:{ all -> 0x026c }
            r4.flush()     // Catch:{ all -> 0x026c }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x010b:
            if (r14 == 0) goto L_0x013a
            com.samsung.android.server.wifi.WifiEnableWarningPolicy r7 = r12.mWifiEnableWarningPolicy     // Catch:{ all -> 0x026c }
            boolean r7 = r7.isAllowWifiWarning()     // Catch:{ all -> 0x026c }
            if (r7 == 0) goto L_0x013a
            com.samsung.android.server.wifi.WifiEnableWarningPolicy r7 = r12.mWifiEnableWarningPolicy     // Catch:{ all -> 0x026c }
            int r8 = r12.getWifiApEnabledState()     // Catch:{ all -> 0x026c }
            int r9 = r12.getWifiEnabledState()     // Catch:{ all -> 0x026c }
            boolean r10 = r12.isWifiSharingEnabled()     // Catch:{ all -> 0x026c }
            boolean r7 = r7.needToShowWarningDialog(r8, r9, r10, r13)     // Catch:{ all -> 0x026c }
            if (r7 == 0) goto L_0x013a
            com.android.server.wifi.WifiLog r4 = r12.mLog     // Catch:{ all -> 0x026c }
            java.lang.String r7 = "setWifiEnabled on CSC_COMMON_CHINA_NAL_SECURITY_TYPE: disabled for China warning dialog"
            com.android.server.wifi.WifiLog$LogMessage r4 = r4.info(r7)     // Catch:{ all -> 0x026c }
            r4.flush()     // Catch:{ all -> 0x026c }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x013a:
            if (r14 != 0) goto L_0x014d
            boolean r7 = r12.supportWifiSharingLite()     // Catch:{ all -> 0x026c }
            if (r7 == 0) goto L_0x014d
            android.content.Context r7 = r12.mContext     // Catch:{ all -> 0x026c }
            android.content.ContentResolver r7 = r7.getContentResolver()     // Catch:{ all -> 0x026c }
            java.lang.String r8 = "wifi_sharing_lite_popup_status"
            android.provider.Settings.System.putInt(r7, r8, r1)     // Catch:{ all -> 0x026c }
        L_0x014d:
            if (r14 == 0) goto L_0x0166
            boolean r7 = r12.checkAndShowWifiSharingLiteDialog(r13)     // Catch:{ all -> 0x026c }
            if (r7 == 0) goto L_0x0166
            com.android.server.wifi.WifiLog r4 = r12.mLog     // Catch:{ all -> 0x026c }
            java.lang.String r7 = "setWifiEnabled in Wifi sharing: disabled for showing wifi sharing lite dialog"
            com.android.server.wifi.WifiLog$LogMessage r4 = r4.info(r7)     // Catch:{ all -> 0x026c }
            r4.flush()     // Catch:{ all -> 0x026c }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x0166:
            if (r14 == 0) goto L_0x017f
            boolean r7 = r12.checkAndShowFirmwareChangeDialog(r13)     // Catch:{ all -> 0x026c }
            if (r7 == 0) goto L_0x017f
            com.android.server.wifi.WifiLog r4 = r12.mLog     // Catch:{ all -> 0x026c }
            java.lang.String r7 = "setWifiEnabled on only enabling SoftAp: disabled for showing wifi enable warning dialog"
            com.android.server.wifi.WifiLog$LogMessage r4 = r4.info(r7)     // Catch:{ all -> 0x026c }
            r4.flush()     // Catch:{ all -> 0x026c }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r1
        L_0x017f:
            com.android.server.wifi.WifiSettingsStore r7 = r12.mSettingsStore     // Catch:{ all -> 0x026c }
            boolean r7 = r7.handleWifiToggled(r14)     // Catch:{ all -> 0x026c }
            if (r7 != 0) goto L_0x018d
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            monitor-exit(r12)
            return r4
        L_0x018d:
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiMetrics r7 = r12.mWifiMetrics     // Catch:{ all -> 0x0271 }
            r7.incrementNumWifiToggles(r0, r14)     // Catch:{ all -> 0x0271 }
            r7 = 0
            java.lang.String r8 = "com.android.systemui"
            boolean r8 = r8.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r8 != 0) goto L_0x01af
            java.lang.String r8 = "com.android.settings"
            boolean r8 = r8.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r8 != 0) goto L_0x01af
            java.lang.String r8 = "com.samsung.desktopsystemui"
            boolean r8 = r8.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r8 == 0) goto L_0x01b0
        L_0x01af:
            r7 = 1
        L_0x01b0:
            long r8 = android.os.Binder.clearCallingIdentity()     // Catch:{ all -> 0x0271 }
            r5 = r8
            r12.setWifiEnabledTriggered(r14, r13, r7)     // Catch:{ all -> 0x0267 }
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            if (r14 != 0) goto L_0x01c5
            boolean r8 = CSC_SEND_DHCP_RELEASE     // Catch:{ all -> 0x0271 }
            if (r8 == 0) goto L_0x01c5
            r12.sendDhcpRelease()     // Catch:{ all -> 0x0271 }
        L_0x01c5:
            if (r14 == 0) goto L_0x01eb
            int r8 = r12.getWifiEnabledState()     // Catch:{ all -> 0x0271 }
            if (r8 == 0) goto L_0x01d8
            if (r8 != r4) goto L_0x01d0
            goto L_0x01d8
        L_0x01d0:
            java.lang.String r9 = "WifiService"
            java.lang.String r10 = "skip forcinglyEnableAllNetworks due to already enabled wifi state"
            android.util.Slog.d(r9, r10)     // Catch:{ all -> 0x0271 }
            goto L_0x01eb
        L_0x01d8:
            com.android.internal.util.AsyncChannel r9 = r12.mClientModeImplChannel     // Catch:{ all -> 0x0271 }
            if (r9 == 0) goto L_0x01e4
            com.android.server.wifi.ClientModeImpl r9 = r12.mClientModeImpl     // Catch:{ all -> 0x0271 }
            com.android.internal.util.AsyncChannel r10 = r12.mClientModeImplChannel     // Catch:{ all -> 0x0271 }
            r9.forcinglyEnableAllNetworks(r10)     // Catch:{ all -> 0x0271 }
            goto L_0x01eb
        L_0x01e4:
            java.lang.String r9 = "WifiService"
            java.lang.String r10 = "mClientModeImplChannel is not initialized"
            android.util.Slog.e(r9, r10)     // Catch:{ all -> 0x0271 }
        L_0x01eb:
            com.android.server.wifi.WifiConnectivityMonitor r8 = r12.mWifiConnectivityMonitor     // Catch:{ all -> 0x0271 }
            if (r8 == 0) goto L_0x0202
            com.android.server.wifi.ClientModeImpl r8 = r12.mClientModeImpl     // Catch:{ all -> 0x0271 }
            android.net.wifi.WifiInfo r8 = r8.getWifiInfo()     // Catch:{ all -> 0x0271 }
            r9 = 0
            if (r8 == 0) goto L_0x01fd
            java.lang.String r10 = r8.getBSSID()     // Catch:{ all -> 0x0271 }
            r9 = r10
        L_0x01fd:
            com.android.server.wifi.WifiConnectivityMonitor r10 = r12.mWifiConnectivityMonitor     // Catch:{ all -> 0x0271 }
            r10.setWifiEnabled(r14, r9)     // Catch:{ all -> 0x0271 }
        L_0x0202:
            long r8 = android.os.Binder.clearCallingIdentity()     // Catch:{ all -> 0x0271 }
            r5 = r8
            if (r14 != 0) goto L_0x0218
            android.content.Context r8 = r12.mContext     // Catch:{ all -> 0x0213 }
            java.lang.String r8 = r8.getPackageName()     // Catch:{ all -> 0x0213 }
            r12.disconnect(r8)     // Catch:{ all -> 0x0213 }
            goto L_0x0218
        L_0x0213:
            r1 = move-exception
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            throw r1     // Catch:{ all -> 0x0271 }
        L_0x0218:
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.WifiController r8 = r12.mWifiController     // Catch:{ all -> 0x0271 }
            r9 = 155656(0x26008, float:2.1812E-40)
            r8.sendMessage(r9)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.IWCMonitor r8 = r12.mIWCMonitor     // Catch:{ all -> 0x0271 }
            if (r8 == 0) goto L_0x0248
            android.os.Bundle r8 = new android.os.Bundle     // Catch:{ all -> 0x0271 }
            r8.<init>()     // Catch:{ all -> 0x0271 }
            java.lang.String r10 = "package"
            r8.putString(r10, r13)     // Catch:{ all -> 0x0271 }
            java.lang.String r10 = "calling_uid"
            int r11 = android.os.Binder.getCallingUid()     // Catch:{ all -> 0x0271 }
            r8.putInt(r10, r11)     // Catch:{ all -> 0x0271 }
            com.android.server.wifi.IWCMonitor r10 = r12.mIWCMonitor     // Catch:{ all -> 0x0271 }
            if (r7 == 0) goto L_0x0241
            r11 = r4
            goto L_0x0242
        L_0x0241:
            r11 = r1
        L_0x0242:
            if (r14 == 0) goto L_0x0245
            r1 = r4
        L_0x0245:
            r10.sendMessage(r9, r11, r1, r8)     // Catch:{ all -> 0x0271 }
        L_0x0248:
            if (r14 == 0) goto L_0x0265
            if (r7 == 0) goto L_0x0265
            java.lang.String r1 = "com.android.systemui"
            boolean r1 = r1.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r1 != 0) goto L_0x025c
            java.lang.String r1 = "com.samsung.desktopsystemui"
            boolean r1 = r1.equals(r13)     // Catch:{ all -> 0x0271 }
            if (r1 == 0) goto L_0x0265
        L_0x025c:
            com.android.server.wifi.WifiInjector r1 = r12.mWifiInjector     // Catch:{ all -> 0x0271 }
            com.samsung.android.server.wifi.WifiPickerController r1 = r1.getPickerController()     // Catch:{ all -> 0x0271 }
            r1.setEnable(r4)     // Catch:{ all -> 0x0271 }
        L_0x0265:
            monitor-exit(r12)
            return r4
        L_0x0267:
            r1 = move-exception
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            throw r1     // Catch:{ all -> 0x0271 }
        L_0x026c:
            r1 = move-exception
            android.os.Binder.restoreCallingIdentity(r5)     // Catch:{ all -> 0x0271 }
            throw r1     // Catch:{ all -> 0x0271 }
        L_0x0271:
            r13 = move-exception
            monitor-exit(r12)
            throw r13
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.setWifiEnabled(java.lang.String, boolean):boolean");
    }

    public int getWifiEnabledState() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiEnabledState uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mClientModeImpl.syncGetWifiState();
    }

    public int getWifiApEnabledState() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiApEnabledState uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        ActiveModeWarden activeModeWarden = this.mActiveModeWarden;
        if (activeModeWarden != null) {
            return activeModeWarden.getSoftApState();
        }
        return this.mWifiApState;
    }

    private /* synthetic */ void lambda$getWifiApEnabledState$1(MutableInt apState) {
        apState.value = this.mWifiApState;
    }

    public void updateInterfaceIpState(String ifaceName, int mode) {
        enforceNetworkStackPermission();
        this.mLog.info("updateInterfaceIpState uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(ifaceName, mode) {
            private final /* synthetic */ String f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$updateInterfaceIpState$2$WifiServiceImpl(this.f$1, this.f$2);
            }
        });
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00b9, code lost:
        return;
     */
    /* renamed from: updateInterfaceIpStateInternal */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void lambda$updateInterfaceIpState$2$WifiServiceImpl(java.lang.String r8, int r9) {
        /*
            r7 = this;
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r0 = r7.mLocalOnlyHotspotRequests
            monitor-enter(r0)
            r1 = -1
            java.lang.Integer r2 = java.lang.Integer.valueOf(r1)     // Catch:{ all -> 0x00ba }
            if (r8 == 0) goto L_0x0017
            java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.Integer> r3 = r7.mIfaceIpModes     // Catch:{ all -> 0x00ba }
            java.lang.Integer r4 = java.lang.Integer.valueOf(r9)     // Catch:{ all -> 0x00ba }
            java.lang.Object r3 = r3.put(r8, r4)     // Catch:{ all -> 0x00ba }
            java.lang.Integer r3 = (java.lang.Integer) r3     // Catch:{ all -> 0x00ba }
            r2 = r3
        L_0x0017:
            java.lang.String r3 = "WifiService"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x00ba }
            r4.<init>()     // Catch:{ all -> 0x00ba }
            java.lang.String r5 = "updateInterfaceIpState: ifaceName="
            r4.append(r5)     // Catch:{ all -> 0x00ba }
            r4.append(r8)     // Catch:{ all -> 0x00ba }
            java.lang.String r5 = " mode="
            r4.append(r5)     // Catch:{ all -> 0x00ba }
            r4.append(r9)     // Catch:{ all -> 0x00ba }
            java.lang.String r5 = " previous mode= "
            r4.append(r5)     // Catch:{ all -> 0x00ba }
            r4.append(r2)     // Catch:{ all -> 0x00ba }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x00ba }
            android.util.Slog.d(r3, r4)     // Catch:{ all -> 0x00ba }
            if (r9 == r1) goto L_0x00af
            r3 = 0
            r4 = 2
            if (r9 == 0) goto L_0x0087
            r5 = 1
            if (r9 == r5) goto L_0x007c
            if (r9 == r4) goto L_0x0059
            com.android.server.wifi.WifiLog r1 = r7.mLog     // Catch:{ all -> 0x00ba }
            java.lang.String r3 = "updateInterfaceIpStateInternal: unknown mode %"
            com.android.server.wifi.WifiLog$LogMessage r1 = r1.warn(r3)     // Catch:{ all -> 0x00ba }
            long r3 = (long) r9     // Catch:{ all -> 0x00ba }
            com.android.server.wifi.WifiLog$LogMessage r1 = r1.mo2069c((long) r3)     // Catch:{ all -> 0x00ba }
            r1.flush()     // Catch:{ all -> 0x00ba }
            goto L_0x00b8
        L_0x0059:
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r4 = r7.mLocalOnlyHotspotRequests     // Catch:{ all -> 0x00ba }
            boolean r4 = r4.isEmpty()     // Catch:{ all -> 0x00ba }
            if (r4 == 0) goto L_0x0078
            int r4 = r7.getRvfMode()     // Catch:{ all -> 0x00ba }
            if (r4 != r5) goto L_0x0070
            java.lang.String r1 = "WifiService"
            java.lang.String r3 = " RVF mode on, do not turn off"
            android.util.Log.d(r1, r3)     // Catch:{ all -> 0x00ba }
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return
        L_0x0070:
            r7.stopSoftAp()     // Catch:{ all -> 0x00ba }
            r7.lambda$updateInterfaceIpState$2$WifiServiceImpl(r3, r1)     // Catch:{ all -> 0x00ba }
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return
        L_0x0078:
            r7.sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked()     // Catch:{ all -> 0x00ba }
            goto L_0x00b8
        L_0x007c:
            boolean r1 = r7.isConcurrentLohsAndTetheringSupported()     // Catch:{ all -> 0x00ba }
            if (r1 != 0) goto L_0x00b8
            r1 = 3
            r7.sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(r1)     // Catch:{ all -> 0x00ba }
            goto L_0x00b8
        L_0x0087:
            java.lang.String r5 = "WifiService"
            java.lang.String r6 = "IP mode config error - need to clean up"
            android.util.Slog.d(r5, r6)     // Catch:{ all -> 0x00ba }
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r5 = r7.mLocalOnlyHotspotRequests     // Catch:{ all -> 0x00ba }
            boolean r5 = r5.isEmpty()     // Catch:{ all -> 0x00ba }
            if (r5 == 0) goto L_0x00a1
            java.lang.String r4 = "WifiService"
            java.lang.String r5 = "no LOHS requests, stop softap"
            android.util.Slog.d(r4, r5)     // Catch:{ all -> 0x00ba }
            r7.stopSoftAp()     // Catch:{ all -> 0x00ba }
            goto L_0x00ab
        L_0x00a1:
            java.lang.String r5 = "WifiService"
            java.lang.String r6 = "we have LOHS requests, clean them up"
            android.util.Slog.d(r5, r6)     // Catch:{ all -> 0x00ba }
            r7.sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(r4)     // Catch:{ all -> 0x00ba }
        L_0x00ab:
            r7.lambda$updateInterfaceIpState$2$WifiServiceImpl(r3, r1)     // Catch:{ all -> 0x00ba }
            goto L_0x00b8
        L_0x00af:
            if (r8 != 0) goto L_0x00b8
            java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.Integer> r1 = r7.mIfaceIpModes     // Catch:{ all -> 0x00ba }
            r1.clear()     // Catch:{ all -> 0x00ba }
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return
        L_0x00b8:
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return
        L_0x00ba:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.lambda$updateInterfaceIpState$2$WifiServiceImpl(java.lang.String, int):void");
    }

    public boolean startSoftAp(WifiConfiguration wifiConfig) {
        PackageManager pm = this.mContext.getPackageManager();
        String clientPkgName = getPackageName(Binder.getCallingPid());
        int signature = pm.checkSignatures("android", clientPkgName);
        Log.d(TAG, "clientPkgName : " + clientPkgName + " " + signature);
        if (signature != 0) {
            Log.d(TAG, "check network stack for " + clientPkgName);
            enforceNetworkStackPermission();
        }
        this.mLog.info("startSoftAp uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.e(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start hotspot.");
            return false;
        }
        int wifiApState = getWifiApEnabledState();
        if (wifiApState == 13 || wifiApState == 12) {
            Log.w(TAG, " skip due to  " + wifiApState);
            return true;
        }
        if (wifiApState == 11) {
            this.mIfaceIpModes.clear();
        }
        this.mLog.info("startSoftAp uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (this.mIfaceIpModes.contains(1)) {
                this.mLog.err("Tethering is already active.").flush();
                return false;
            }
            if (!isConcurrentLohsAndTetheringSupported() && !this.mLocalOnlyHotspotRequests.isEmpty()) {
                stopSoftApInternal(2);
            }
            if (getRvfMode() == 1) {
                Log.d(TAG, "RVF mode on");
                boolean startSoftApInternal = startSoftApInternal(wifiConfig, 2);
                return startSoftApInternal;
            }
            boolean startSoftApInternal2 = startSoftApInternal(wifiConfig, 1);
            return startSoftApInternal2;
        }
    }

    private boolean startSoftApInternal(WifiConfiguration wifiConfig, int mode) {
        this.mLog.trace("startSoftApInternal uid=% mode=%").mo2069c((long) Binder.getCallingUid()).mo2069c((long) mode).flush();
        if (wifiConfig != null && !WifiApConfigStore.validateApWifiConfiguration(wifiConfig)) {
            Slog.e(TAG, "Invalid WifiConfiguration");
            return false;
        } else if (this.mClientModeImpl.syncGetWifiState() == 3 && !EnterpriseDeviceManager.getInstance().getWifiPolicy().isWifiStateChangeAllowed()) {
            Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
            intent.putExtra("wifi_state", 14);
            intent.putExtra("previous_wifi_state", 11);
            intent.putExtra("wifi_ap_error_code", 0);
            this.mContext.sendBroadcast(intent);
            intent.setPackage(SETTINGS_PACKAGE_NAME);
            this.mContext.sendBroadcast(intent);
            intent.setPackage("com.android.systemui");
            this.mContext.sendBroadcast(intent);
            return false;
        } else if (!this.mDevicePolicyManager.isAllowToUseHotspot() && mode != 2) {
            return false;
        } else {
            if (wifiConfig == null) {
                if (getWifiApConfiguration().allowedKeyManagement.get(0) && !this.mDevicePolicyManager.isOpenWifiApAllowed()) {
                    return false;
                }
                wifiConfig = getWifiApConfiguration();
            } else if (wifiConfig.allowedKeyManagement.get(0) && !this.mDevicePolicyManager.isOpenWifiApAllowed()) {
                return false;
            } else {
                if (wifiConfig.allowedKeyManagement.get(1)) {
                    if (wifiConfig.preSharedKey == null || wifiConfig.preSharedKey.length() < 8 || wifiConfig.preSharedKey.length() > 32) {
                        wifiConfig.allowedKeyManagement.set(0);
                    } else {
                        wifiConfig.allowedKeyManagement.set(4);
                    }
                    Log.e(TAG, " conf changed to wpa2/none from wpa");
                }
            }
            if (supportWifiSharingLite() && isWifiSharingEnabled()) {
                setIndoorChannelsToDriver(true);
            }
            SoftApModeConfiguration softApConfig = new SoftApModeConfiguration(mode, wifiConfig);
            if (this.mSettingsStore.getWifiSavedState() == 1) {
                this.mWifiController.sendMessage(155658, 1, 1, softApConfig);
            } else {
                this.mWifiController.sendMessage(155658, 1, 0, softApConfig);
            }
            return true;
        }
    }

    public boolean stopSoftAp() {
        PackageManager pm = this.mContext.getPackageManager();
        String clientPkgName = getPackageName(Binder.getCallingPid());
        int signature = pm.checkSignatures("android", clientPkgName);
        Log.d(TAG, "clientPkgName : " + clientPkgName + " " + signature);
        if (signature != 0) {
            Log.d(TAG, "check network stack for " + clientPkgName);
            enforceNetworkStackPermission();
        }
        this.mLog.info("stopSoftAp uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                this.mLog.trace("Call to stop Tethering while LOHS is active, Registered LOHS callers will be updated when softap stopped.").flush();
            }
            if (getRvfMode() == 1) {
                Log.d(TAG, "RVF mode on, turn off hotspot" + this.mLocalOnlyHotspotRequests.isEmpty());
                lambda$updateInterfaceIpState$2$WifiServiceImpl((String) null, -1);
                setRvfMode(0);
                boolean stopSoftApInternal = stopSoftApInternal(2);
                return stopSoftApInternal;
            } else if (this.mLocalOnlyHotspotRequests.isEmpty() || this.mIfaceIpModes.contains(1)) {
                boolean stopSoftApInternal2 = stopSoftApInternal(1);
                return stopSoftApInternal2;
            } else {
                Log.d(TAG, "mLocalOnlyHotspotRequests, turn off hotspot" + this.mLocalOnlyHotspotRequests.isEmpty());
                boolean stopSoftApInternal3 = stopSoftApInternal(2);
                return stopSoftApInternal3;
            }
        }
    }

    private boolean stopSoftApInternal(int mode) {
        this.mLog.trace("stopSoftApInternal uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.e(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start hotspot.");
            return false;
        }
        if (isWifiSharingEnabled() && this.isProvisionSuccessful == 1) {
            this.isProvisionSuccessful = 0;
        }
        if (supportWifiSharing() && supportWifiSharingLite()) {
            setIndoorChannelsToDriver(false);
        }
        this.mWifiController.sendMessage(155658, 0, mode);
        return true;
    }

    private final class SoftApCallbackImpl implements WifiManager.SoftApCallback {
        private SoftApCallbackImpl() {
        }

        public synchronized void onStateChanged(int state, int failureReason) {
            int unused = WifiServiceImpl.this.mSoftApState = state;
            Iterator<ISoftApCallback> iterator = WifiServiceImpl.this.mRegisteredSoftApCallbacks.getCallbacks().iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().onStateChanged(state, failureReason);
                } catch (RemoteException e) {
                    Log.e(WifiServiceImpl.TAG, "onStateChanged: remote exception -- " + e);
                    iterator.remove();
                }
            }
        }

        public synchronized void onNumClientsChanged(int numClients) {
            int unused = WifiServiceImpl.this.mSoftApNumClients = numClients;
            Iterator<ISoftApCallback> iterator = WifiServiceImpl.this.mRegisteredSoftApCallbacks.getCallbacks().iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().onNumClientsChanged(numClients);
                } catch (RemoteException e) {
                    Log.e(WifiServiceImpl.TAG, "onNumClientsChanged: remote exception -- " + e);
                    iterator.remove();
                }
            }
        }
    }

    public void registerSoftApCallback(IBinder binder, ISoftApCallback callback, int callbackIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("registerSoftApCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            this.mWifiInjector.getClientModeImplHandler().post(new Runnable(binder, callback, callbackIdentifier) {
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ ISoftApCallback f$2;
                private final /* synthetic */ int f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$registerSoftApCallback$3$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
                }
            });
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$registerSoftApCallback$3$WifiServiceImpl(IBinder binder, ISoftApCallback callback, int callbackIdentifier) {
        if (!this.mRegisteredSoftApCallbacks.add(binder, callback, callbackIdentifier)) {
            Log.e(TAG, "registerSoftApCallback: Failed to add callback");
            return;
        }
        try {
            callback.onStateChanged(this.mSoftApState, 0);
            callback.onNumClientsChanged(this.mSoftApNumClients);
        } catch (RemoteException e) {
            Log.e(TAG, "registerSoftApCallback: remote exception -- " + e);
        }
    }

    public void unregisterSoftApCallback(int callbackIdentifier) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterSoftApCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(callbackIdentifier) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$unregisterSoftApCallback$4$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$unregisterSoftApCallback$4$WifiServiceImpl(int callbackIdentifier) {
        this.mRegisteredSoftApCallbacks.remove(callbackIdentifier);
    }

    /* access modifiers changed from: private */
    public void handleWifiApStateChange(int currentState, int previousState, int errorCode, String ifaceName, int mode) {
        Slog.d(TAG, "handleWifiApStateChange: currentState=" + currentState + " previousState=" + previousState + " errorCode= " + errorCode + " ifaceName=" + ifaceName + " mode=" + mode);
        this.mWifiApState = currentState;
        if (currentState == 14) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                int errorToReport = 2;
                if (errorCode == 1) {
                    errorToReport = 1;
                }
                sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(errorToReport);
                lambda$updateInterfaceIpState$2$WifiServiceImpl((String) null, -1);
            }
        } else if (currentState == 10 || currentState == 11) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                if (this.mIfaceIpModes.getOrDefault(ifaceName, -1).intValue() == 2) {
                    sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked();
                } else if (!isConcurrentLohsAndTetheringSupported()) {
                    sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(2);
                }
                updateInterfaceIpState((String) null, -1);
            }
        } else if (currentState == 13 && isWifiSharingEnabled() && getWifiEnabledState() == 3) {
            if (supportWifiSharingLite()) {
                Log.i(TAG, "setting indoor channel info when wifi turns on");
                this.mAsyncChannelExternalClientHandler.post(new Runnable() {
                    public final void run() {
                        WifiServiceImpl.this.lambda$handleWifiApStateChange$5$WifiServiceImpl();
                    }
                });
            }
            if ("VZW".equals(CONFIGOPBRANDINGFORMOBILEAP) && getConnectionInfo("system").getNetworkId() != -1) {
                Toast.makeText(this.mContext, this.mContext.getResources().getString(17042610), 0).show();
            }
        }
    }

    public /* synthetic */ void lambda$handleWifiApStateChange$5$WifiServiceImpl() {
        setIndoorChannelsToDriver(true);
    }

    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private void sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(int arg1) {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotFailedMessage(arg1);
                requestor.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private void sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked() {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotStoppedMessage();
                requestor.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy({"mLocalOnlyHotspotRequests"})
    private void sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked() {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
            } catch (RemoteException e) {
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void registerLOHSForTest(int pid, LocalOnlyHotspotRequestInfo request) {
        this.mLocalOnlyHotspotRequests.put(Integer.valueOf(pid), request);
    }

    /* Debug info: failed to restart local var, previous not found, register: 11 */
    /* JADX INFO: finally extract failed */
    public int startLocalOnlyHotspot(Messenger messenger, IBinder binder, String packageName) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.e(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start hotspot.");
            return 2;
        } else if (enforceChangePermission(packageName) != 0) {
            return 2;
        } else {
            enforceLocationPermission(packageName, uid);
            long ident = Binder.clearCallingIdentity();
            try {
                if (this.mWifiPermissionsUtil.isLocationModeEnabled()) {
                    Binder.restoreCallingIdentity(ident);
                    if (this.mUserManager.hasUserRestriction("no_config_tethering")) {
                        return 4;
                    }
                    if (!this.mFrameworkFacade.isAppForeground(uid)) {
                        return 3;
                    }
                    this.mLog.info("startLocalOnlyHotspot uid=% pid=%").mo2069c((long) uid).mo2069c((long) pid).flush();
                    synchronized (this.mLocalOnlyHotspotRequests) {
                        int i = 1;
                        if (!isConcurrentLohsAndTetheringSupported() && this.mIfaceIpModes.contains(1)) {
                            this.mLog.info("Cannot start localOnlyHotspot when WiFi Tethering is active.").flush();
                            return 3;
                        } else if (this.mLocalOnlyHotspotRequests.get(Integer.valueOf(pid)) == null) {
                            LocalOnlyHotspotRequestInfo request = new LocalOnlyHotspotRequestInfo(binder, messenger, new LocalOnlyRequestorCallback());
                            if (this.mIfaceIpModes.contains(2)) {
                                try {
                                    this.mLog.trace("LOHS already up, trigger onStarted callback").flush();
                                    request.sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
                                } catch (RemoteException e) {
                                    return 2;
                                }
                            } else if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                                boolean is5Ghz = hasAutomotiveFeature(this.mContext) && this.mContext.getResources().getBoolean(17891607) && is5GhzSupported();
                                Context context = this.mContext;
                                if (!is5Ghz) {
                                    i = 0;
                                }
                                this.mLocalOnlyHotspotConfig = WifiApConfigStore.generateLocalOnlyHotspotConfig(context, i);
                                startSoftApInternal(this.mLocalOnlyHotspotConfig, 2);
                            }
                            this.mLocalOnlyHotspotRequests.put(Integer.valueOf(pid), request);
                            return 0;
                        } else {
                            this.mLog.trace("caller already has an active request").flush();
                            throw new IllegalStateException("Caller already has an active LocalOnlyHotspot request");
                        }
                    }
                } else {
                    throw new SecurityException("Location mode is not enabled.");
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    public void stopLocalOnlyHotspot() {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        this.mLog.info("stopLocalOnlyHotspot uid=% pid=%").mo2069c((long) uid).mo2069c((long) pid).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            LocalOnlyHotspotRequestInfo requestInfo = this.mLocalOnlyHotspotRequests.get(Integer.valueOf(pid));
            if (requestInfo != null) {
                requestInfo.unlinkDeathRecipient();
                unregisterCallingAppAndStopLocalOnlyHotspot(requestInfo);
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 4 */
    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0055, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterCallingAppAndStopLocalOnlyHotspot(com.android.server.wifi.LocalOnlyHotspotRequestInfo r5) {
        /*
            r4 = this;
            com.android.server.wifi.WifiLog r0 = r4.mLog
            java.lang.String r1 = "unregisterCallingAppAndStopLocalOnlyHotspot pid=%"
            com.android.server.wifi.WifiLog$LogMessage r0 = r0.trace(r1)
            int r1 = r5.getPid()
            long r1 = (long) r1
            com.android.server.wifi.WifiLog$LogMessage r0 = r0.mo2069c((long) r1)
            r0.flush()
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r0 = r4.mLocalOnlyHotspotRequests
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r1 = r4.mLocalOnlyHotspotRequests     // Catch:{ all -> 0x0056 }
            int r2 = r5.getPid()     // Catch:{ all -> 0x0056 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)     // Catch:{ all -> 0x0056 }
            java.lang.Object r1 = r1.remove(r2)     // Catch:{ all -> 0x0056 }
            if (r1 != 0) goto L_0x0034
            com.android.server.wifi.WifiLog r1 = r4.mLog     // Catch:{ all -> 0x0056 }
            java.lang.String r2 = "LocalOnlyHotspotRequestInfo not found to remove"
            com.android.server.wifi.WifiLog$LogMessage r1 = r1.trace(r2)     // Catch:{ all -> 0x0056 }
            r1.flush()     // Catch:{ all -> 0x0056 }
            monitor-exit(r0)     // Catch:{ all -> 0x0056 }
            return
        L_0x0034:
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.LocalOnlyHotspotRequestInfo> r1 = r4.mLocalOnlyHotspotRequests     // Catch:{ all -> 0x0056 }
            boolean r1 = r1.isEmpty()     // Catch:{ all -> 0x0056 }
            if (r1 == 0) goto L_0x0054
            r1 = 0
            r4.mLocalOnlyHotspotConfig = r1     // Catch:{ all -> 0x0056 }
            r2 = -1
            r4.lambda$updateInterfaceIpState$2$WifiServiceImpl(r1, r2)     // Catch:{ all -> 0x0056 }
            long r1 = android.os.Binder.clearCallingIdentity()     // Catch:{ all -> 0x0056 }
            r3 = 2
            r4.stopSoftApInternal(r3)     // Catch:{ all -> 0x004f }
            android.os.Binder.restoreCallingIdentity(r1)     // Catch:{ all -> 0x0056 }
            goto L_0x0054
        L_0x004f:
            r3 = move-exception
            android.os.Binder.restoreCallingIdentity(r1)     // Catch:{ all -> 0x0056 }
            throw r3     // Catch:{ all -> 0x0056 }
        L_0x0054:
            monitor-exit(r0)     // Catch:{ all -> 0x0056 }
            return
        L_0x0056:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0056 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.unregisterCallingAppAndStopLocalOnlyHotspot(com.android.server.wifi.LocalOnlyHotspotRequestInfo):void");
    }

    public void startWatchLocalOnlyHotspot(Messenger messenger, IBinder binder) {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public void stopWatchLocalOnlyHotspot() {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        if (this.mWifiPermissionsUtil.checkConfigOverridePermission(uid)) {
            this.mLog.info("getWifiApConfiguration uid=%").mo2069c((long) uid).flush();
            GeneralUtil.Mutable<WifiConfiguration> config = new GeneralUtil.Mutable<>();
            if (this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(config) {
                private final /* synthetic */ GeneralUtil.Mutable f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$getWifiApConfiguration$6$WifiServiceImpl(this.f$1);
                }
            }, IWCEventManager.autoDisconnectThreshold)) {
                return (WifiConfiguration) config.value;
            }
            Log.e(TAG, "Failed to post runnable to fetch ap config");
            return this.mWifiApConfigStore.getApConfiguration();
        }
        throw new SecurityException("App not allowed to read or update stored WiFi Ap config (uid = " + uid + ")");
    }

    public /* synthetic */ void lambda$getWifiApConfiguration$6$WifiServiceImpl(GeneralUtil.Mutable config) {
        config.value = this.mWifiApConfigStore.getApConfiguration();
    }

    public boolean setWifiApConfiguration(WifiConfiguration wifiConfig, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        int uid = Binder.getCallingUid();
        if (this.mWifiPermissionsUtil.checkConfigOverridePermission(uid)) {
            this.mLog.info("setWifiApConfiguration uid=%").mo2069c((long) uid).flush();
            if (wifiConfig == null) {
                return false;
            }
            if (WifiApConfigStore.validateApWifiConfiguration(wifiConfig)) {
                this.mClientModeImplHandler.post(new Runnable(wifiConfig) {
                    private final /* synthetic */ WifiConfiguration f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        WifiServiceImpl.this.lambda$setWifiApConfiguration$7$WifiServiceImpl(this.f$1);
                    }
                });
                return true;
            }
            Slog.e(TAG, "Invalid WifiConfiguration");
            return false;
        }
        throw new SecurityException("App not allowed to read or update stored WiFi AP config (uid = " + uid + ")");
    }

    public /* synthetic */ void lambda$setWifiApConfiguration$7$WifiServiceImpl(WifiConfiguration wifiConfig) {
        this.mWifiApConfigStore.setApConfiguration(wifiConfig);
    }

    public void notifyUserOfApBandConversion(String packageName) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("notifyUserOfApBandConversion uid=% packageName=%").mo2069c((long) Binder.getCallingUid()).mo2070c(packageName).flush();
        }
        this.mWifiApConfigStore.notifyUserOfApBandConversion(packageName);
    }

    public void setWifiApConfigurationToDefault() {
        enforceAccessPermission();
        this.mClientModeImplHandler.post(new Runnable() {
            public final void run() {
                WifiServiceImpl.this.lambda$setWifiApConfigurationToDefault$8$WifiServiceImpl();
            }
        });
    }

    public /* synthetic */ void lambda$setWifiApConfigurationToDefault$8$WifiServiceImpl() {
        this.mWifiApConfigStore.setWifiApConfigurationToDefault();
    }

    public String getWifiApInterfaceName() {
        enforceAccessPermission();
        try {
            return this.mWifiNative.getSoftApInterfaceName();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void bindNetdNativeService() {
        try {
            this.mNetdService = INetd.Stub.asInterface(ServiceManager.getService("netd"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind service netd, error=" + e.getMessage());
        }
        if (this.mNetdService == null) {
            Log.e(TAG, "Can't bind service netd");
        }
    }

    public synchronized String runIptablesRulesCommand(String command) {
        String result;
        result = null;
        enforceNetworkStackPermission();
        if (this.mNetdService == null) {
            bindNetdNativeService();
        }
        try {
            result = this.mNetdService.runVpnRulesCommand(4, command);
        } catch (Exception e) {
            Log.e(TAG, "Failed to run command: cmd=" + command + ", error=" + e.getMessage());
        }
        return result;
    }

    public String semGetStationInfo(String mac) {
        if (getWifiApEnabledState() == 13) {
            return getStationInfo(mac);
        }
        return null;
    }

    public int getWifiApMaxClient() {
        int chipNum;
        String str;
        enforceAccessPermission();
        int featureNum = WifiApConfigStore.MAX_CLIENT;
        if (!MHSDBG || (str = SystemProperties.get("mhs.maxclient")) == null || str.equals("")) {
            String ss = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_chip_maxclient");
            if (ss == null || ss.equals("na")) {
                return featureNum;
            }
            try {
                chipNum = Integer.parseInt(ss);
            } catch (Exception e) {
                Log.w(TAG, "exception : " + e);
                chipNum = featureNum;
            }
            int rInt = chipNum < featureNum ? chipNum : featureNum;
            int i = 10;
            if (rInt < 10) {
                i = rInt;
            }
            int rInt2 = i;
            if (!MHSDBG) {
                return rInt2;
            }
            Log.w(TAG, "featureNum:" + featureNum + " chipNum:" + chipNum + " rInt:" + rInt2);
            return rInt2;
        }
        Log.w(TAG, "changed max client " + Integer.parseInt(str));
        return Integer.parseInt(str);
    }

    public boolean supportWifiAp5G() {
        enforceAccessPermission();
        if (MHSDBG) {
            String str = SystemProperties.get("mhs.5g");
            if (str.equals("1")) {
                return true;
            }
            if (str.equals("0")) {
                return false;
            }
        }
        String ss = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_chip_support5g");
        if (ss != null && !ss.equals("na")) {
            return Boolean.parseBoolean(ss) && isRegionFor5G();
        }
        if (!SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTMOBILEAP5G, false)) {
            return false;
        }
        return true;
    }

    public boolean supportWifiAp5GBasedOnCountry() {
        enforceAccessPermission();
        if (MHSDBG) {
            String str = SystemProperties.get("mhs.5gcountry");
            if (str.equals("1")) {
                return true;
            }
            if (str.equals("0")) {
                return false;
            }
        }
        String ss = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_chip_support5g_baseon_country");
        String ss_5g = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_chip_support5g");
        if (ss == null || ss.equals("na")) {
            return false;
        }
        return Boolean.parseBoolean(ss_5g) && Boolean.parseBoolean(ss) && isRegionFor5GCountry();
    }

    public int semGetWifiApChannel() {
        enforceAccessPermission();
        if (getWifiApEnabledState() == 13) {
            return getWifiApChannel();
        }
        return this.mWifiApConfigStore.getApConfiguration().apChannel;
    }

    public boolean supportWifiSharingLite() {
        return this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharingLite();
    }

    public boolean setWifiSharingEnabled(boolean enable) {
        enforceAccessPermission();
        if (!supportWifiSharing()) {
            Log.i(TAG, "Failed: Does not support Wi-Fi Sharing.");
            return false;
        }
        Message msg = new Message();
        msg.what = 77;
        Bundle args = new Bundle();
        args.putString("feature", "MHWS");
        long ident = Binder.clearCallingIdentity();
        if (enable) {
            try {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing", 1);
                args.putString("extra", "ON");
                getWifiApEnabledState();
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing", 0);
            args.putString("extra", "OFF");
        }
        Log.i(TAG, "Wi-Fi Sharing mode : " + enable + " wifiApState: " + this.mWifiApState + " getWifiEnabledState  : " + getWifiEnabledState());
        msg.obj = args;
        callSECApi(msg);
        this.mContext.sendBroadcast(new Intent("com.samsung.intent.action.UPDATE_OPTIONS_MENU"));
        Binder.restoreCallingIdentity(ident);
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isMobileApOn() {
        int wifiApState = getWifiApEnabledState();
        if (wifiApState == 13 || wifiApState == 12) {
            Log.i(TAG, "Mobile Ap is in enabled state");
            return true;
        }
        Log.i(TAG, "Mobile AP is in disabled state");
        return false;
    }

    public boolean setProvisionSuccess(boolean set) {
        Log.i(TAG, "Provision variable set to " + set);
        if (set) {
            this.isProvisionSuccessful = 1;
        } else {
            this.isProvisionSuccessful = 2;
        }
        CharSequence dateTime = DateFormat.format("yy/MM/dd kk:mm:ss ", System.currentTimeMillis());
        SemSoftapConfig semSoftapConfig = this.mWifiInjector.getSemSoftapConfig();
        semSoftapConfig.addMHSDumpLog(dateTime + " isProvisionSuccessful (" + this.isProvisionSuccessful + ") : \n");
        return true;
    }

    public int getProvisionSuccess() {
        if (OpBrandingLoader.Vendor.VZW == mOpBranding && SystemProperties.get("Provisioning.disable").equals("1")) {
            return 1;
        }
        Log.i(TAG, "isProvisioning successful  " + this.isProvisionSuccessful);
        return this.isProvisionSuccessful;
    }

    public boolean isScanAlwaysAvailable() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isScanAlwaysAvailable uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    public boolean disconnect(String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("disconnect not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("disconnect uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.report(100, ReportUtil.getReportDataForWifiManagerApi(-1, "disconnect", this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()), getPackageName(Binder.getCallingPid())));
        this.mClientModeImpl.disconnectCommand(Binder.getCallingUid(), 2);
        return true;
    }

    public boolean reconnect(String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("reconnect not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("reconnect uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.reconnectCommand(new WorkSource(Binder.getCallingUid()));
        return true;
    }

    public boolean reassociate(String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("reassociate not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("reassociate uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.reassociateCommand();
        return true;
    }

    public long getSupportedFeatures() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getSupportedFeatures uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return getSupportedFeaturesInternal();
    }

    private String getSupportedFeaturesHumanReadable() {
        StringBuilder sb = new StringBuilder();
        long supportedFeatures = getSupportedFeatures();
        sb.append(supportedFeatures);
        sb.append(" ");
        if ((1 & supportedFeatures) != 0) {
            sb.append("[INFRA]");
        }
        if ((2 & supportedFeatures) != 0) {
            sb.append("[INFRA_5G]");
        }
        if ((4 & supportedFeatures) != 0) {
            sb.append("[PASSPOINT]");
        }
        if ((8 & supportedFeatures) != 0) {
            sb.append("[P2P]");
        }
        if ((16 & supportedFeatures) != 0) {
            sb.append("[AP]");
        }
        if ((32 & supportedFeatures) != 0) {
            sb.append("[SCANNER]");
        }
        if ((64 & supportedFeatures) != 0) {
            sb.append("[AWARE]");
        }
        if ((128 & supportedFeatures) != 0) {
            sb.append("[D2_RTT]");
        }
        if ((256 & supportedFeatures) != 0) {
            sb.append("[D2AP_RTT]");
        }
        if ((512 & supportedFeatures) != 0) {
            sb.append("[BATCH_SCAN]");
        }
        if ((1024 & supportedFeatures) != 0) {
            sb.append("[PNO]");
        }
        if ((2048 & supportedFeatures) != 0) {
            sb.append("[DUAL_STA]");
        }
        if ((4096 & supportedFeatures) != 0) {
            sb.append("[TDLS]");
        }
        if ((8192 & supportedFeatures) != 0) {
            sb.append("[TDLS_OFFCH]");
        }
        if ((16384 & supportedFeatures) != 0) {
            sb.append("[EPR]");
        }
        if ((32768 & supportedFeatures) != 0) {
            sb.append("[WIFI_SHARING]");
        }
        if ((65536 & supportedFeatures) != 0) {
            sb.append("[LINK_LAYER_STATS]");
        }
        if ((131072 & supportedFeatures) != 0) {
            sb.append("[LOG]");
        }
        if ((262144 & supportedFeatures) != 0) {
            sb.append("[PNO_HAL]");
        }
        if ((524288 & supportedFeatures) != 0) {
            sb.append("[RSSI_MON]");
        }
        if ((1048576 & supportedFeatures) != 0) {
            sb.append("[MKEEP_ALIVE]");
        }
        if ((2097152 & supportedFeatures) != 0) {
            sb.append("[NDO]");
        }
        if ((4194304 & supportedFeatures) != 0) {
            sb.append("[CTL_TP]");
        }
        if ((8388608 & supportedFeatures) != 0) {
            sb.append("[CTL_ROAM]");
        }
        if ((16777216 & supportedFeatures) != 0) {
            sb.append("[IE_WHITE]");
        }
        if ((33554432 & supportedFeatures) != 0) {
            sb.append("[RANDMAC_SCAN]");
        }
        if ((67108864 & supportedFeatures) != 0) {
            sb.append("[TX_POWER_LIMIT]");
        }
        if ((134217728 & supportedFeatures) != 0) {
            sb.append("[SAE]");
        }
        if ((268435456 & supportedFeatures) != 0) {
            sb.append("[SUITE_B]");
        }
        if ((536870912 & supportedFeatures) != 0) {
            sb.append("[OWE]");
        }
        if ((1073741824 & supportedFeatures) != 0) {
            sb.append("[LOW_LETENCY]");
        }
        if ((-2147483648L & supportedFeatures) != 0) {
            sb.append("[DPP]");
        }
        if ((4294967296L & supportedFeatures) != 0) {
            sb.append("[P2P_RANDOM_MAC]");
        }
        return sb.toString();
    }

    public void requestActivityInfo(ResultReceiver result) {
        Bundle bundle = new Bundle();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("requestActivityInfo uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        bundle.putParcelable("controller_activity", reportActivityInfo());
        result.send(0, bundle);
    }

    public WifiActivityEnergyInfo reportActivityInfo() {
        WifiActivityEnergyInfo energyInfo;
        long[] txTimePerLevel;
        double rxIdleCurrent;
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("reportActivityInfo uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        if ((getSupportedFeatures() & 65536) == 0) {
            return null;
        }
        WifiActivityEnergyInfo energyInfo2 = null;
        AsyncChannel asyncChannel = this.mClientModeImplChannel;
        if (asyncChannel != null && !this.semIsShutdown) {
            WifiLinkLayerStats stats = this.mClientModeImpl.syncGetLinkLayerStats(asyncChannel);
            if (stats != null) {
                double rxIdleCurrent2 = this.mPowerProfile.getAveragePower("wifi.controller.idle");
                double rxCurrent = this.mPowerProfile.getAveragePower("wifi.controller.rx");
                double txCurrent = this.mPowerProfile.getAveragePower("wifi.controller.tx");
                double voltage = this.mPowerProfile.getAveragePower("wifi.controller.voltage") / 1000.0d;
                long rxIdleTime = (long) ((stats.on_time - stats.tx_time) - stats.rx_time);
                if (stats.tx_time_per_level != null) {
                    long[] txTimePerLevel2 = new long[stats.tx_time_per_level.length];
                    int i = 0;
                    while (true) {
                        WifiActivityEnergyInfo energyInfo3 = energyInfo2;
                        if (i >= txTimePerLevel2.length) {
                            break;
                        }
                        txTimePerLevel2[i] = (long) stats.tx_time_per_level[i];
                        i++;
                        energyInfo2 = energyInfo3;
                    }
                    txTimePerLevel = txTimePerLevel2;
                } else {
                    txTimePerLevel = new long[0];
                }
                double txCurrent2 = txCurrent;
                long energyUsed = (long) (((((double) stats.tx_time) * txCurrent) + (((double) stats.rx_time) * rxCurrent) + (((double) rxIdleTime) * rxIdleCurrent2)) * voltage);
                if (rxIdleTime < 0 || stats.on_time < 0 || stats.tx_time < 0 || stats.rx_time < 0 || stats.on_time_scan < 0 || energyUsed < 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" rxIdleCur=" + rxIdleCurrent2);
                    sb.append(" rxCur=" + rxCurrent);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(" txCur=");
                    double d = rxIdleCurrent2;
                    rxIdleCurrent = txCurrent2;
                    sb2.append(rxIdleCurrent);
                    sb.append(sb2.toString());
                    sb.append(" voltage=" + voltage);
                    sb.append(" on_time=" + stats.on_time);
                    sb.append(" tx_time=" + stats.tx_time);
                    sb.append(" tx_time_per_level=" + Arrays.toString(txTimePerLevel));
                    sb.append(" rx_time=" + stats.rx_time);
                    sb.append(" rxIdleTime=" + rxIdleTime);
                    sb.append(" scan_time=" + stats.on_time_scan);
                    sb.append(" energy=" + energyUsed);
                    Log.d(TAG, " reportActivityInfo: " + sb.toString());
                } else {
                    double d2 = rxIdleCurrent2;
                    rxIdleCurrent = txCurrent2;
                }
                double d3 = rxIdleCurrent;
                double d4 = rxCurrent;
                double d5 = voltage;
                energyInfo = new WifiActivityEnergyInfo(this.mClock.getElapsedSinceBootMillis(), 3, (long) stats.tx_time, txTimePerLevel, (long) stats.rx_time, (long) stats.on_time_scan, rxIdleTime, energyUsed);
            } else {
                energyInfo = null;
            }
            if (energyInfo == null || !energyInfo.isValid()) {
                return null;
            }
            return energyInfo;
        } else if (this.semIsShutdown) {
            Slog.e(TAG, "Skip reportActivityInfo in Shutdown");
            return null;
        } else {
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return null;
        }
    }

    public ParceledListSlice<WifiConfiguration> getConfiguredNetworks(String packageName) {
        return semGetConfiguredNetworks(0, packageName);
    }

    public ParceledListSlice<WifiConfiguration> semGetConfiguredNetworks(int from, String packageName) {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        if (!(callingUid == 2000 || callingUid == 0)) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mWifiPermissionsUtil.enforceCanAccessScanResults(packageName, callingUid);
            } catch (SecurityException e) {
                Slog.e(TAG, "Permission violation - getConfiguredNetworks not allowed for uid=" + callingUid + ", packageName=" + packageName + ", reason=" + e);
                return new ParceledListSlice<>(new ArrayList());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        boolean isTargetSdkLessThanQOrPrivileged = isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), callingUid);
        boolean z = true;
        if (this.mWifiInjector.makeTelephonyManager().checkCarrierPrivilegesForPackageAnyPhone(packageName) != 1) {
            z = false;
        }
        boolean isCarrierApp = z;
        if (isTargetSdkLessThanQOrPrivileged || isCarrierApp) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getConfiguredNetworks uid=%").mo2069c((long) callingUid).flush();
            }
            int targetConfigUid = -1;
            if (isPrivileged(getCallingPid(), callingUid) || isDeviceOrProfileOwner(callingUid)) {
                targetConfigUid = 1010;
            } else if (isCarrierApp) {
                targetConfigUid = callingUid;
            }
            AsyncChannel asyncChannel = this.mClientModeImplChannel;
            if (asyncChannel != null) {
                List<WifiConfiguration> configs = this.mClientModeImpl.syncGetConfiguredNetworks(callingUid, asyncChannel, from, targetConfigUid);
                if (configs == null) {
                    return null;
                }
                if (isTargetSdkLessThanQOrPrivileged) {
                    return new ParceledListSlice<>(configs);
                }
                List<WifiConfiguration> creatorConfigs = new ArrayList<>();
                for (WifiConfiguration config : configs) {
                    if (config.creatorUid == callingUid) {
                        creatorConfigs.add(config);
                    }
                }
                return new ParceledListSlice<>(creatorConfigs);
            }
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return null;
        }
        this.mLog.info("getConfiguredNetworks not allowed for uid=%").mo2069c((long) callingUid).flush();
        return new ParceledListSlice<>(new ArrayList());
    }

    public WifiConfiguration getSpecificNetwork(int netID) {
        enforceAccessPermission();
        AsyncChannel asyncChannel = this.mClientModeImplChannel;
        if (asyncChannel != null) {
            return this.mClientModeImpl.syncGetSpecificNetwork(asyncChannel, netID);
        }
        Slog.e(TAG, "mClientModeImplChannel is not initialized");
        return null;
    }

    public ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks(String packageName) {
        enforceReadCredentialPermission();
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(packageName, callingUid);
            Binder.restoreCallingIdentity(ident);
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getPrivilegedConfiguredNetworks uid=%").mo2069c((long) callingUid).flush();
            }
            AsyncChannel asyncChannel = this.mClientModeImplChannel;
            if (asyncChannel != null) {
                List<WifiConfiguration> configs = this.mClientModeImpl.syncGetPrivilegedConfiguredNetwork(asyncChannel);
                if (configs != null) {
                    return new ParceledListSlice<>(configs);
                }
            } else {
                Slog.e(TAG, "mClientModeImplChannel is not initialized");
            }
            return null;
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getPrivilegedConfiguredNetworks not allowed for uid=" + callingUid + ", packageName=" + packageName + ", reason=" + e);
            Binder.restoreCallingIdentity(ident);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public Map<String, Map<Integer, List<ScanResult>>> getAllMatchingFqdnsForScanResults(List<ScanResult> scanResults) {
        if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getMatchingPasspointConfigurations uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            return this.mClientModeImpl.syncGetAllMatchingFqdnsForScanResults(scanResults, this.mClientModeImplChannel);
        }
        throw new SecurityException("WifiService: Permission denied");
    }

    public Map<OsuProvider, List<ScanResult>> getMatchingOsuProviders(List<ScanResult> scanResults) {
        if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getMatchingOsuProviders uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            return this.mClientModeImpl.syncGetMatchingOsuProviders(scanResults, this.mClientModeImplChannel);
        }
        throw new SecurityException("WifiService: Permission denied");
    }

    public Map<OsuProvider, PasspointConfiguration> getMatchingPasspointConfigsForOsuProviders(List<OsuProvider> osuProviders) {
        if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getMatchingPasspointConfigsForOsuProviders uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            if (osuProviders != null) {
                return this.mClientModeImpl.syncGetMatchingPasspointConfigsForOsuProviders(osuProviders, this.mClientModeImplChannel);
            }
            Log.e(TAG, "Attempt to retrieve Passpoint configuration with null osuProviders");
            return new HashMap();
        }
        throw new SecurityException("WifiService: Permission denied");
    }

    public List<WifiConfiguration> getWifiConfigsForPasspointProfiles(List<String> fqdnList) {
        if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getWifiConfigsForPasspointProfiles uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            if (fqdnList != null) {
                return this.mClientModeImpl.syncGetWifiConfigsForPasspointProfiles(fqdnList, this.mClientModeImplChannel);
            }
            Log.e(TAG, "Attempt to retrieve WifiConfiguration with null fqdn List");
            return new ArrayList();
        }
        throw new SecurityException("WifiService: Permission denied");
    }

    public int addOrUpdateNetwork(WifiConfiguration config, String packageName) {
        return semAddOrUpdateNetwork(0, config, packageName);
    }

    public int semAddOrUpdateNetwork(int from, WifiConfiguration config, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return -1;
        }
        if (!this.mWifiPermissionsUtil.checkFactoryTestPermission(Binder.getCallingUid()) && !isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("addOrUpdateNetwork not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return -1;
        }
        this.mLog.info("addOrUpdateNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (config == null) {
            Slog.e(TAG, "bad network configuration");
            return -1;
        }
        if (from == -1000) {
            if (Binder.getCallingUid() != 1000) {
                Slog.e(TAG, "Operation only allowed to system uid");
                return -1;
            }
            config.semSamsungSpecificFlags.set(4);
        }
        this.mWifiMetrics.incrementNumAddOrUpdateNetworkCalls();
        boolean z = true;
        if (config.isPasspoint()) {
            PasspointConfiguration passpointConfig = PasspointProvider.convertFromWifiConfig(config);
            if (passpointConfig.getCredential() == null) {
                Slog.e(TAG, "Missing credential for Passpoint profile");
                return -1;
            }
            X509Certificate[] x509Certificates = null;
            if (config.enterpriseConfig.getCaCertificate() != null) {
                x509Certificates = new X509Certificate[]{config.enterpriseConfig.getCaCertificate()};
            }
            passpointConfig.getCredential().setCaCertificates(x509Certificates);
            passpointConfig.getCredential().setClientCertificateChain(config.enterpriseConfig.getClientCertificateChain());
            passpointConfig.getCredential().setClientPrivateKey(config.enterpriseConfig.getClientPrivateKey());
            if (addOrUpdatePasspointConfiguration(passpointConfig, packageName)) {
                return 0;
            }
            Slog.e(TAG, "Failed to add Passpoint profile");
            return -1;
        }
        Slog.i("addOrUpdateNetwork", " uid = " + Integer.toString(Binder.getCallingUid()) + " SSID " + config.SSID + " nid=" + Integer.toString(config.networkId));
        if (config.networkId == -1) {
            config.creatorUid = Binder.getCallingUid();
        } else {
            config.lastUpdateUid = Binder.getCallingUid();
        }
        if (this.mClientModeImplChannel != null) {
            boolean hasPassword = false;
            if (config.preSharedKey != null) {
                if (config.preSharedKey.length() <= 8) {
                    z = false;
                }
                hasPassword = z;
            } else if (!(config.wepKeys == null || config.wepKeys[0] == null)) {
                if (config.wepKeys[0].length() < 4) {
                    z = false;
                }
                hasPassword = z;
            }
            int netId = this.mClientModeImpl.syncAddOrUpdateNetwork(this.mClientModeImplChannel, from, config);
            this.mClientModeImpl.report(105, ReportUtil.getReportDataForWifiManagerAddOrUpdateApi(netId, hasPassword, "addOrUpdateNetwork", this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()), getPackageName(Binder.getCallingPid())));
            return netId;
        }
        Slog.e(TAG, "mClientModeImplChannel is not initialized");
        return -1;
    }

    public static void verifyCert(X509Certificate caCert) throws GeneralSecurityException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath path = factory.generateCertPath(Arrays.asList(new X509Certificate[]{caCert}));
        KeyStore ks = KeyStore.getInstance("AndroidCAStore");
        ks.load((InputStream) null, (char[]) null);
        PKIXParameters params = new PKIXParameters(ks);
        params.setRevocationEnabled(false);
        validator.validate(path, params);
    }

    public boolean removeNetwork(int netId, String packageName) {
        return semRemoveNetwork(0, netId, packageName);
    }

    /* JADX INFO: finally extract failed */
    public boolean semRemoveNetwork(int from, int netId, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("removeNetwork not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("removeNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (from == -1000 && Binder.getCallingUid() != 1000) {
            Slog.e(TAG, "Operation only allowed to system uid");
            return false;
        } else if (this.mClientModeImplChannel != null) {
            this.mClientModeImpl.report(102, ReportUtil.getReportDataForWifiManagerApi(netId, "removeNetwork", this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()), getPackageName(Binder.getCallingPid())));
            if (this.mIWCMonitor != null) {
                int uid = Binder.getCallingUid();
                Bundle args = new Bundle();
                args.putString("package", packageName);
                this.mIWCMonitor.sendMessage(151752, uid, netId, args);
                Log.e(TAG, "uid: " + uid + " nid: " + netId + " packageName: " + packageName);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                if (this.mAutoWifiController != null) {
                    this.mAutoWifiController.forgetNetwork(netId);
                }
                Binder.restoreCallingIdentity(ident);
                return this.mClientModeImpl.syncRemoveNetwork(this.mClientModeImplChannel, from, netId);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return false;
        }
    }

    public boolean enableNetwork(int netId, boolean disableOthers, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("enableNetwork not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("enableNetwork uid=% disableOthers=%").mo2069c((long) Binder.getCallingUid()).mo2071c(disableOthers).flush();
        this.mWifiMetrics.incrementNumEnableNetworkCalls();
        if (this.mClientModeImplChannel != null) {
            if (this.mIWCMonitor != null) {
                int uid = Binder.getCallingUid();
                Bundle args = new Bundle();
                args.putString("package", packageName);
                this.mIWCMonitor.sendMessage(151753, uid, netId, args);
                Log.e(TAG, "uid: " + uid + " nid: " + netId + " packageName: " + packageName);
            }
            return this.mClientModeImpl.syncEnableNetwork(this.mClientModeImplChannel, netId, disableOthers);
        }
        Slog.e(TAG, "mClientModeImplChannel is not initialized");
        return false;
    }

    public boolean disableNetwork(int netId, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (!isTargetSdkLessThanQOrPrivileged(packageName, Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("disableNetwork not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return false;
        }
        this.mLog.info("disableNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mClientModeImplChannel != null) {
            this.mClientModeImpl.report(101, ReportUtil.getReportDataForWifiManagerApi(netId, "disableNetwork", this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()), getPackageName(Binder.getCallingPid())));
            return this.mClientModeImpl.syncDisableNetwork(this.mClientModeImplChannel, netId);
        }
        Slog.e(TAG, "mClientModeImplChannel is not initialized");
        return false;
    }

    public WifiInfo getConnectionInfo(String callingPackage) {
        WifiInfo result;
        boolean hideDefaultMacAddress;
        boolean hideBssidSsidAndNetworkId;
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getConnectionInfo uid=%").mo2069c((long) uid).flush();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            result = this.mClientModeImpl.syncRequestConnectionInfo();
            hideDefaultMacAddress = true;
            hideBssidSsidAndNetworkId = true;
            if (this.mWifiInjector.getWifiPermissionsWrapper().getLocalMacAddressPermission(uid) == 0) {
                hideDefaultMacAddress = false;
            } else {
                Log.e(TAG, uid + " has no permission about LOCAL_MAC_ADDRESS");
            }
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(callingPackage, uid);
            hideBssidSsidAndNetworkId = false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error checking receiver permission", e);
        } catch (SecurityException e2) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        if (hideDefaultMacAddress) {
            result.setMacAddress("02:00:00:00:00:00");
        }
        if (hideBssidSsidAndNetworkId) {
            result.setBSSID("02:00:00:00:00:00");
            result.setSSID(WifiSsid.createFromHex((String) null));
            result.setNetworkId(-1);
        }
        if (this.mVerboseLoggingEnabled && (hideBssidSsidAndNetworkId || hideDefaultMacAddress)) {
            WifiLog wifiLog = this.mLog;
            wifiLog.mo2095v("getConnectionInfo: hideBssidSsidAndNetworkId=" + hideBssidSsidAndNetworkId + ", hideDefaultMacAddress=" + hideDefaultMacAddress);
        }
        Binder.restoreCallingIdentity(ident);
        return result;
    }

    public NetworkInfo getNetworkInfo() {
        enforceAccessNetworkPermission();
        return this.mClientModeImpl.syncGetNetworkInfo();
    }

    public List<ScanResult> getScanResults(String callingPackage) {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getScanResults uid=%").mo2069c((long) uid).flush();
        }
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(callingPackage, uid);
            List<ScanResult> scanResults = new ArrayList<>();
            if (!this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(scanResults) {
                private final /* synthetic */ List f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$getScanResults$9$WifiServiceImpl(this.f$1);
                }
            }, IWCEventManager.autoDisconnectThreshold)) {
                Log.e(TAG, "Failed to post runnable to fetch scan results");
                return new ArrayList();
            }
            Binder.restoreCallingIdentity(ident);
            return scanResults;
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getScanResults not allowed for uid=" + uid + ", packageName=" + callingPackage + ", reason=" + e);
            return new ArrayList();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public /* synthetic */ void lambda$getScanResults$9$WifiServiceImpl(List scanResults) {
        scanResults.addAll(this.mScanRequestProxy.getScanResults());
    }

    public boolean addOrUpdatePasspointConfiguration(PasspointConfiguration config, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mLog.info("addorUpdatePasspointConfiguration uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        return this.mClientModeImpl.syncAddOrUpdatePasspointConfig(this.mClientModeImplChannel, config, Binder.getCallingUid(), packageName);
    }

    public boolean removePasspointConfiguration(String fqdn, String packageName) {
        int uid = Binder.getCallingUid();
        if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid) || this.mWifiPermissionsUtil.checkNetworkCarrierProvisioningPermission(uid)) {
            this.mLog.info("removePasspointConfiguration uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return this.mClientModeImpl.syncRemovePasspointConfig(this.mClientModeImplChannel, fqdn);
        } else if (this.mWifiPermissionsUtil.isTargetSdkLessThan(packageName, 29)) {
            return false;
        } else {
            throw new SecurityException("WifiService: Permission denied");
        }
    }

    public List<PasspointConfiguration> getPasspointConfigurations(String packageName) {
        int uid = Binder.getCallingUid();
        if (!"SamsungPasspoint".equals(packageName)) {
            this.mAppOps.checkPackage(uid, packageName);
        }
        if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid) || this.mWifiPermissionsUtil.checkDiagnosticsProviderPermission(uid) || this.mWifiPermissionsUtil.checkNetworkSetupWizardPermission(uid)) {
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("getPasspointConfigurations uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            return this.mClientModeImpl.syncGetPasspointConfigs(this.mClientModeImplChannel);
        } else if (this.mWifiPermissionsUtil.isTargetSdkLessThan(packageName, 29)) {
            return new ArrayList();
        } else {
            throw new SecurityException("WifiService: Permission denied");
        }
    }

    public void queryPasspointIcon(long bssid, String fileName) {
        enforceAccessPermission();
        this.mLog.info("queryPasspointIcon uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.syncQueryPasspointIcon(this.mClientModeImplChannel, bssid, fileName);
    }

    public int matchProviderWithCurrentNetwork(String fqdn) {
        this.mLog.info("matchProviderWithCurrentNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        return this.mClientModeImpl.matchProviderWithCurrentNetwork(this.mClientModeImplChannel, fqdn);
    }

    public void deauthenticateNetwork(long holdoff, boolean ess) {
        this.mLog.info("deauthenticateNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.deauthenticateNetwork(this.mClientModeImplChannel, holdoff, ess);
    }

    public void setCountryCode(String countryCode) {
        Slog.i(TAG, "WifiService trying to set country code to " + countryCode);
        if (this.semIsShutdown) {
            Slog.i(TAG, "Skip setCountryCode in Shutdown");
            return;
        }
        enforceConnectivityInternalPermission();
        this.mLog.info("setCountryCode uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        long token = Binder.clearCallingIdentity();
        this.mCountryCode.setCountryCode(countryCode);
        Binder.restoreCallingIdentity(token);
    }

    public String getCountryCode() {
        enforceConnectivityInternalPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCountryCode uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mCountryCode.getCountryCode();
    }

    public boolean isDualBandSupported() {
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isDualBandSupported uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mContext.getResources().getBoolean(17891597);
    }

    private int getMaxApInterfacesCount() {
        return this.mContext.getResources().getInteger(17695014);
    }

    private boolean isConcurrentLohsAndTetheringSupported() {
        return getMaxApInterfacesCount() >= 2;
    }

    public boolean needs5GHzToAnyApBandConversion() {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("needs5GHzToAnyApBandConversion uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mContext.getResources().getBoolean(17891596);
    }

    @Deprecated
    public DhcpInfo getDhcpInfo() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getDhcpInfo uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        DhcpResults dhcpResults = this.mClientModeImpl.syncGetDhcpResults();
        DhcpInfo info = new DhcpInfo();
        if (dhcpResults.ipAddress != null && (dhcpResults.ipAddress.getAddress() instanceof Inet4Address)) {
            info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.ipAddress.getAddress());
        }
        if (dhcpResults.gateway != null) {
            info.gateway = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.gateway);
        }
        int dnsFound = 0;
        Iterator it = dhcpResults.dnsServers.iterator();
        while (it.hasNext()) {
            InetAddress dns = (InetAddress) it.next();
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                }
                dnsFound++;
                if (dnsFound > 1) {
                    break;
                }
            }
        }
        Inet4Address serverAddress = dhcpResults.serverAddress;
        if (serverAddress != null) {
            info.serverAddress = NetworkUtils.inetAddressToInt(serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;
        return info;
    }

    class TdlsTaskParams {
        public boolean enable;
        public String remoteIpAddress;

        TdlsTaskParams() {
        }
    }

    class TdlsTask extends AsyncTask<TdlsTaskParams, Integer, Integer> {
        TdlsTask() {
        }

        /* access modifiers changed from: protected */
        public Integer doInBackground(TdlsTaskParams... params) {
            TdlsTaskParams param = params[0];
            String remoteIpAddress = param.remoteIpAddress.trim();
            boolean enable = param.enable;
            String macAddress = null;
            BufferedReader reader = null;
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader("/proc/net/arp"));
                String readLine = reader2.readLine();
                while (true) {
                    String readLine2 = reader2.readLine();
                    String line = readLine2;
                    if (readLine2 == null) {
                        break;
                    }
                    String[] tokens = line.split("[ ]+");
                    if (tokens.length >= 6) {
                        String ip = tokens[0];
                        String mac = tokens[3];
                        if (remoteIpAddress.equals(ip)) {
                            macAddress = mac;
                            break;
                        }
                    }
                }
                if (macAddress == null) {
                    Slog.w(WifiServiceImpl.TAG, "Did not find remoteAddress {" + remoteIpAddress + "} in /proc/net/arp");
                } else {
                    WifiServiceImpl.this.enableTdlsWithMacAddress(macAddress, enable);
                }
                try {
                    reader2.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
                Slog.e(WifiServiceImpl.TAG, "Could not open /proc/net/arp to lookup mac address");
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e3) {
                Slog.e(WifiServiceImpl.TAG, "Could not read /proc/net/arp to lookup mac address");
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
            return 0;
        }
    }

    public void enableTdls(String remoteAddress, boolean enable) {
        if (remoteAddress != null) {
            this.mLog.info("enableTdls uid=% enable=%").mo2069c((long) Binder.getCallingUid()).mo2071c(enable).flush();
            TdlsTaskParams params = new TdlsTaskParams();
            params.remoteIpAddress = remoteAddress;
            params.enable = enable;
            new TdlsTask().execute(new TdlsTaskParams[]{params});
            return;
        }
        throw new IllegalArgumentException("remoteAddress cannot be null");
    }

    public void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable) {
        this.mLog.info("enableTdlsWithMacAddress uid=% enable=%").mo2069c((long) Binder.getCallingUid()).mo2071c(enable).flush();
        if (remoteMacAddress != null) {
            this.mClientModeImpl.enableTdls(remoteMacAddress, enable);
            return;
        }
        throw new IllegalArgumentException("remoteMacAddress cannot be null");
    }

    public boolean setRoamTrigger(int roamTrigger) {
        enforceChangePermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncSetRoamTrigger(roamTrigger);
        }
        Log.d(TAG, "setRoamTrigger Invalid package");
        return false;
    }

    public int getRoamTrigger() {
        enforceAccessPermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncGetRoamTrigger();
        }
        Log.d(TAG, "getRoamTrigger Invalid package");
        return -1;
    }

    public boolean setRoamDelta(int roamDelta) {
        enforceChangePermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncSetRoamDelta(roamDelta);
        }
        Log.d(TAG, "setRoamDelta Invalid package");
        return false;
    }

    public int getRoamDelta() {
        enforceAccessPermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncGetRoamDelta();
        }
        Log.d(TAG, "getRoamDelta Invalid package");
        return -1;
    }

    public boolean setRoamScanPeriod(int roamScanPeriod) {
        enforceChangePermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncSetRoamScanPeriod(roamScanPeriod);
        }
        Log.d(TAG, "setRoamScanPeriod Invalid package");
        return false;
    }

    public int getRoamScanPeriod() {
        enforceAccessPermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncGetRoamScanPeriod();
        }
        Log.d(TAG, "getRoamScanPeriod Invalid package");
        return -1;
    }

    public boolean setRoamBand(int band) {
        enforceChangePermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncSetRoamBand(band);
        }
        Log.d(TAG, "setRoamBand Invalid package");
        return false;
    }

    public int getRoamBand() {
        enforceAccessPermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncGetRoamBand();
        }
        Log.d(TAG, "getRoamBand Invalid package");
        return -1;
    }

    public boolean setCountryRev(String countryRev) {
        enforceChangePermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncSetCountryRev(countryRev);
        }
        Log.d(TAG, "setCountryRev Invalid package");
        return false;
    }

    public String getCountryRev() {
        enforceAccessPermission();
        if (isFmcPackage()) {
            return this.mClientModeImpl.syncGetCountryRev();
        }
        Log.d(TAG, "getCountryRev Invalid package");
        return null;
    }

    public boolean semIsCarrierNetworkSaved() {
        enforceAccessPermission();
        int pid = Binder.getCallingPid();
        Log.d(TAG, "semIsCarrierNetworkSaved requested by pid=" + pid);
        return this.mWifiInjector.getWifiConfigManager().isCarrierNetworkSaved();
    }

    public Messenger getWifiServiceMessenger(String packageName) {
        enforceAccessPermission();
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("getWifiServiceMessenger uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return new Messenger(this.mAsyncChannelExternalClientHandler);
        }
        throw new SecurityException("Could not create wifi service messenger");
    }

    public void disableEphemeralNetwork(String SSID, String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
        if (!isPrivileged(Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mLog.info("disableEphemeralNetwork not allowed for uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            return;
        }
        this.mClientModeImpl.report(101, ReportUtil.getReportDataForWifiManagerApi(SSID, "disableEphemeralNetwork", this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()), getPackageName(Binder.getCallingPid())));
        this.mLog.info("disableEphemeralNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mClientModeImpl.disableEphemeralNetwork(SSID);
    }

    private void registerForScanModeChange() {
        this.mFrameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_scan_always_enabled"), false, new ContentObserver((Handler) null) {
            public void onChange(boolean selfChange) {
                WifiServiceImpl.this.mSettingsStore.handleWifiScanAlwaysAvailableToggled();
                WifiServiceImpl.this.mWifiController.sendMessage(155655);
            }
        });
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        if (!CSC_DISABLE_EMERGENCYCALLBACK_TRANSITION) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        }
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        if (WIFI_STOP_SCAN_FOR_ETWS) {
            intentFilter.addAction("android.provider.Telephony.SMS_CB_WIFI_RECEIVED");
        }
        if (this.mContext.getResources().getBoolean(17891613)) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
        }
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        intentFilter2.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                    int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                    Uri uri = intent.getData();
                    if (uid != -1 && uri != null) {
                        String pkgName = uri.getSchemeSpecificPart();
                        WifiServiceImpl.this.mClientModeImpl.removeAppConfigs(pkgName, uid);
                        WifiServiceImpl.this.mWifiInjector.getClientModeImplHandler().post(new Runnable(pkgName, uid) {
                            private final /* synthetic */ String f$1;
                            private final /* synthetic */ int f$2;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                            }

                            public final void run() {
                                WifiServiceImpl.C047413.this.lambda$onReceive$0$WifiServiceImpl$13(this.f$1, this.f$2);
                            }
                        });
                    }
                }
            }

            public /* synthetic */ void lambda$onReceive$0$WifiServiceImpl$13(String pkgName, int uid) {
                WifiServiceImpl.this.mScanRequestProxy.clearScanRequestTimestampsForApp(pkgName, uid);
                WifiServiceImpl.this.mWifiNetworkSuggestionsManager.removeApp(pkgName);
                WifiServiceImpl.this.mClientModeImpl.removeNetworkRequestUserApprovedAccessPointsForApp(pkgName);
                WifiServiceImpl.this.mWifiInjector.getPasspointManager().removePasspointProviderWithPackage(pkgName);
            }
        }, intentFilter2);
        WifiGuiderManagementService wifiGuiderManagementService = this.mWifiGuiderManagementService;
        if (wifiGuiderManagementService != null) {
            wifiGuiderManagementService.registerForBroadcastsForWifiGuider();
        }
    }

    /* JADX WARNING: type inference failed for: r1v1, types: [android.os.Binder] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onShellCommand(java.io.FileDescriptor r9, java.io.FileDescriptor r10, java.io.FileDescriptor r11, java.lang.String[] r12, android.os.ShellCallback r13, android.os.ResultReceiver r14) {
        /*
            r8 = this;
            com.android.server.wifi.WifiShellCommand r0 = new com.android.server.wifi.WifiShellCommand
            com.android.server.wifi.WifiInjector r1 = r8.mWifiInjector
            r0.<init>(r1)
            r1 = r8
            r2 = r9
            r3 = r10
            r4 = r11
            r5 = r12
            r6 = r13
            r7 = r14
            r0.exec(r1, r2, r3, r4, r5, r6, r7)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.onShellCommand(java.io.FileDescriptor, java.io.FileDescriptor, java.io.FileDescriptor, java.lang.String[], android.os.ShellCallback, android.os.ResultReceiver):void");
    }

    private void dumpMHS(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println();
        pw.println("MHS dump ----- start -----\n");
        pw.println("mCSCRegion:" + this.mCSCRegion + " mCountryIso:" + this.mCountryIso + " isRegionFor5GCountry:" + isRegionFor5GCountry() + " isRegionFor5G:" + isRegionFor5G());
        pw.println(this.mWifiInjector.getWifiApConfigStore().getDumpLogs());
        pw.println();
        pw.println(this.mWifiInjector.getSemSoftapConfig().getDumpLogs());
        pw.println();
        pw.println(this.mWifiInjector.getSemWifiApClientInfo().getDumpLogs());
        pw.println();
        pw.println("Provision Success:" + this.isProvisionSuccessful);
        pw.println();
        pw.println("isWifiSharingEnabled:" + isWifiSharingEnabled());
        pw.println("MHS Clients\n" + getWifiApStaList());
        pw.println();
        pw.println(this.mWifiInjector.getSemWifiApChipInfo().getDumpLogs());
        pw.println();
        pw.println(this.mWifiInjector.getHostapdHAL().getDumpLogs());
        pw.println();
        this.mWifiInjector.getSemWifiApTimeOutImpl().dump(fd, pw, args);
        pw.println();
        pw.println("--api");
        pw.println("5G:" + supportWifiAp5G());
        pw.println("5g_Country:" + supportWifiAp5GBasedOnCountry());
        pw.println("maxClient:" + getWifiApMaxClient());
        pw.println("wifisharing:" + supportWifiSharing());
        pw.println("wifisharinglite:" + supportWifiSharingLite());
        pw.println();
        String[] provisionApp = this.mContext.getResources().getStringArray(17236154);
        if (provisionApp != null) {
            pw.println("--provisioning apps length:" + provisionApp.length);
            if (provisionApp.length == 2) {
                pw.println(provisionApp[0] + "," + provisionApp[1]);
            }
        }
        pw.println("provision csc : " + SemCscFeature.getInstance().getString(CscFeatureTagSetting.TAG_CSCFEATURE_SETTING_CONFIGMOBILEHOTSPOTPROVISIONAPP));
        if (!FactoryTest.isFactoryBinary()) {
            pw.println();
            this.mSemWifiApSmartLocalLog.dump(fd, pw, args);
            pw.println();
            pw.println(this.mSemWifiApSmartUtil.getDumpLogs());
            pw.println();
            pw.println();
            pw.println(this.mSemWifiApSmartBleScanner.getDumpLogs());
            pw.println();
        }
        pw.println("MHS dump ----- end -----\n");
        pw.println();
        pw.println();
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        } else if (args != null && args.length > 0 && WifiMetrics.PROTO_DUMP_ARG.equals(args[0])) {
            this.mClientModeImpl.updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
        } else if (args != null && args.length > 0 && "ipclient".equals(args[0])) {
            String[] ipClientArgs = new String[(args.length - 1)];
            System.arraycopy(args, 1, ipClientArgs, 0, ipClientArgs.length);
            this.mClientModeImpl.dumpIpClient(fd, pw, ipClientArgs);
        } else if (args != null && args.length > 0 && WifiScoreReport.DUMP_ARG.equals(args[0])) {
            WifiScoreReport wifiScoreReport = this.mClientModeImpl.getWifiScoreReport();
            if (wifiScoreReport != null) {
                wifiScoreReport.dump(fd, pw, args);
            }
        } else if (args == null || args.length <= 0 || !WifiScoreCard.DUMP_ARG.equals(args[0])) {
            WifiNative wifiNative = this.mWifiNative;
            wifiNative.saveFwDump(wifiNative.getClientInterfaceName());
            this.mClientModeImpl.updateLinkLayerStatsRssiAndScoreReport();
            pw.println("Wi-Fi is " + this.mClientModeImpl.syncGetWifiStateByName());
            StringBuilder sb = new StringBuilder();
            sb.append("Verbose logging is ");
            sb.append(this.mVerboseLoggingEnabled ? "on" : "off");
            pw.println(sb.toString());
            pw.println("Stay-awake conditions: " + this.mFacade.getIntegerSetting(this.mContext, "stay_on_while_plugged_in", 0));
            pw.println("mInIdleMode " + this.mInIdleMode);
            pw.println("mScanPending " + this.mScanPending);
            if (WifiChipInfo.getInstance().isReady()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Wi-Fi vendor: ");
                WifiChipInfo.getInstance();
                sb2.append(WifiChipInfo.getChipsetNameHumanReadable());
                pw.println(sb2.toString());
            }
            pw.println("Supported feature: " + getSupportedFeaturesHumanReadable());
            pw.println("Wi-Fi api call history:");
            pw.println(this.mHistoricalDumpLogs.toString());
            pw.println("Wi-Fi control history from provider:");
            WifiControlHistoryProvider.dumpControlHistory(this.mContext, pw);
            this.mWifiController.dump(fd, pw, args);
            this.mSettingsStore.dump(fd, pw, args);
            this.mWifiTrafficPoller.dump(fd, pw, args);
            pw.println();
            pw.println("Locks held:");
            this.mWifiLockManager.dump(pw);
            pw.println();
            this.mWifiMulticastLockManager.dump(pw);
            pw.println();
            this.mActiveModeWarden.dump(fd, pw, args);
            pw.println();
            dumpMHS(fd, pw, args);
            pw.println();
            WifiConnectivityMonitor wifiConnectivityMonitor = this.mWifiConnectivityMonitor;
            if (wifiConnectivityMonitor != null) {
                wifiConnectivityMonitor.dump(fd, pw, args);
                pw.println();
            }
            this.mClientModeImpl.dump(fd, pw, args);
            pw.println();
            this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(pw) {
                private final /* synthetic */ PrintWriter f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$dump$11$WifiServiceImpl(this.f$1);
                }
            }, IWCEventManager.autoDisconnectThreshold);
            this.mClientModeImpl.updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
            pw.println();
            this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(fd, pw, args) {
                private final /* synthetic */ FileDescriptor f$1;
                private final /* synthetic */ PrintWriter f$2;
                private final /* synthetic */ String[] f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$dump$12$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
                }
            }, IWCEventManager.autoDisconnectThreshold);
            this.mWifiBackupRestore.dump(fd, pw, args);
            pw.println();
            pw.println("ScoringParams: settings put global wifi_score_params " + this.mWifiInjector.getScoringParams());
            pw.println();
            WifiScoreReport wifiScoreReport2 = this.mClientModeImpl.getWifiScoreReport();
            if (wifiScoreReport2 != null) {
                pw.println("WifiScoreReport:");
                wifiScoreReport2.dump(fd, pw, args);
            }
            pw.println();
            SarManager sarManager = this.mWifiInjector.getSarManager();
            if (sarManager != null) {
                sarManager.dump(fd, pw, args);
            }
            pw.println();
            this.mScanRequestProxy.dump(fd, pw, args);
            pw.println();
            this.mDefaultApController.dump(fd, pw, args);
            this.mPasspointDefaultProvider.dump(fd, pw, args);
            IWCMonitor iWCMonitor = this.mIWCMonitor;
            if (iWCMonitor != null) {
                iWCMonitor.dump(fd, pw, args);
            }
            AutoWifiController autoWifiController = this.mAutoWifiController;
            if (autoWifiController != null) {
                autoWifiController.dump(fd, pw, args);
            }
            pw.println("WifiGuiderPackageMonitor:");
            pw.println(this.mWifiGuiderPackageMonitor.dump());
            pw.println();
            SemWifiProfileAndQoSProvider semWifiProfileAndQoSProvider = this.mSemQoSProfileShareProvider;
            if (semWifiProfileAndQoSProvider != null) {
                semWifiProfileAndQoSProvider.dump(fd, pw, args);
            }
            this.mWifiInjector.getWifiLowLatency().dump(fd, pw, args);
        } else {
            this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(pw) {
                private final /* synthetic */ PrintWriter f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$dump$10$WifiServiceImpl(this.f$1);
                }
            }, IWCEventManager.autoDisconnectThreshold);
        }
    }

    public /* synthetic */ void lambda$dump$10$WifiServiceImpl(PrintWriter pw) {
        WifiScoreCard wifiScoreCard = this.mWifiInjector.getWifiScoreCard();
        if (wifiScoreCard != null) {
            pw.println(wifiScoreCard.getNetworkListBase64(true));
        }
    }

    public /* synthetic */ void lambda$dump$11$WifiServiceImpl(PrintWriter pw) {
        WifiScoreCard wifiScoreCard = this.mWifiInjector.getWifiScoreCard();
        if (wifiScoreCard != null) {
            pw.println("WifiScoreCard:");
            pw.println(wifiScoreCard.getNetworkListBase64(true));
        }
    }

    public /* synthetic */ void lambda$dump$12$WifiServiceImpl(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mWifiNetworkSuggestionsManager.dump(fd, pw, args);
        pw.println();
    }

    private void mapIndoorCountryToChannel() {
        File indoorChannelFile = new File("/vendor/etc/wifi/indoorchannel.info");
        Log.d(TAG, "mIndoorChannelFilePath:/vendor/etc/wifi/indoorchannel.info");
        Log.d(TAG, "Indoor channel filename:" + indoorChannelFile.getAbsolutePath() + "indoorChannelFile.exists() :" + indoorChannelFile.exists());
        BufferedReader br = null;
        if (indoorChannelFile.exists()) {
            try {
                Log.d(TAG, "Reading the file for indoor channel/vendor/etc/wifi/indoorchannel.info");
                BufferedReader br2 = new BufferedReader(new FileReader(indoorChannelFile));
                String line = br2.readLine();
                if (line != null && line.split(" ").length > 1) {
                    String fileVersion = line.split(" ")[1];
                }
                while (true) {
                    String readLine = br2.readLine();
                    String line2 = readLine;
                    if (readLine != null) {
                        this.mCountryChannelList.put(line2.substring(0, 2), line2.substring(3));
                    } else {
                        try {
                            br2.close();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            } catch (Exception e2) {
                Log.e(TAG, "Indoor channel file access fail:/vendor/etc/wifi/indoorchannel.inforead from hardcoded channels");
                initializeChannelInfo();
                e2.printStackTrace();
                if (br != null) {
                    br.close();
                }
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        } else {
            Log.e(TAG, "Indoor channel file does not exist:/vendor/etc/wifi/indoorchannel.info,read from hardcoded channels");
            initializeChannelInfo();
        }
    }

    public void initializeChannelInfo() {
        Log.d(TAG, "Initialize the indoor channel info");
        this.mCountryChannel.put("IN", "36 40 44 48 52 56 60 64 149 153 157 161");
        this.mCountryChannel.put("KR,BB,VE,VN,AR,UY,CL,CA,CO,PA", "36 40 44 48");
        this.mCountryChannel.put("BO", "52 56 60 64 149 153 157 161 165");
        this.mCountryChannel.put("QA", "149 153 157 161 165");
        this.mCountryChannel.put("GH,GG,GR,GL,ZA,NL,NO,NF,NZ,NU,DK,DE,LV,RO,LU,LY,LT,LI,MK,IM,MC,MA,ME,MV,MT,BH,VA,BE,BA,BG,BR,SA,SM,PM,RS,SE,CH,ES,SK,SI,AE,IS,IE,AL,EE,GB,IO,OM,AU,AT,UA,IL,EG,IT,JP,JE,GE,CN,GI,CZ,CC,CL,CA,CC,CO,KW,CK,HR,CY,TH,TR,TK,FO,PT,PL,FR,TF,PF,FJ,FI,PN,HM,HU,HK", "36 40 44 48 52 56 60 64");
        for (Map.Entry<String, String> entry : this.mCountryChannel.entrySet()) {
            String value = entry.getValue();
            Log.d(TAG, "Country = " + entry.getKey() + ", channels = " + value);
            String[] countryList = entry.getKey().split(",");
            for (String put : countryList) {
                this.mCountryChannelList.put(put, value);
            }
        }
    }

    public void setIndoorChannelsToDriver(boolean toBeSet) {
        String countryCode = this.mCountryCode.getCountryCode();
        if (countryCode == null || !this.mCountryChannelList.containsKey(countryCode)) {
            Log.e(TAG, "Country doesn't support indoor channel.");
            return;
        }
        String channelDetailsToSendToDriver = "";
        int channelLen = 0;
        if (toBeSet) {
            Log.d(TAG, "Setting indoor channel info in driver");
            channelDetailsToSendToDriver = this.mCountryChannelList.get(countryCode);
            if (this.mClientModeImpl.syncGetWifiState() != 3) {
                Log.e(TAG, "Wifi is off. So, not setting indoor channels to driver.");
                return;
            }
            channelLen = channelDetailsToSendToDriver.split(" ").length;
            Log.d(TAG, "Number of indoor channels = " + channelLen);
            Log.d(TAG, "Indoor channel details(<ch1> <ch2> ...) : " + channelDetailsToSendToDriver);
        }
        Log.d(TAG, "sending cmd SEC_COMMAND_ID_SET_INDOOR_CHANNELS to WiFiNative to set/reset indoor ch");
        if (this.mWifiNative != null) {
            String status = toBeSet ? "set" : "reset";
            if (this.mWifiNative.setIndoorChannels(channelLen, channelDetailsToSendToDriver)) {
                Log.d(TAG, "Indoor channels " + status + " successfully");
                return;
            }
            Log.e(TAG, "Error! Indoor channels not " + status);
        }
    }

    private boolean getIndoorStatus() {
        String countryCode = this.mCountryCode.getCountryCode();
        NetworkInfo info = getNetworkInfo();
        Log.d(TAG, "Network info details : " + info.getDetailedState());
        Log.d(TAG, "Device country code : " + countryCode);
        if (countryCode == null || !this.mCountryChannelList.containsKey(countryCode)) {
            Log.e(TAG, "Country doesn't support indoor channel.");
            return false;
        } else if (!isWifiConnected()) {
            Log.e(TAG, "Device is not connected to any WIFI network. Disconnected Flag:");
            return false;
        } else {
            String[] channelList = this.mCountryChannelList.get(countryCode).split(" ");
            WifiInfo currentNetwork = getConnectionInfo("system");
            Log.d(TAG, "Current network frequency : " + currentNetwork.getFrequency());
            int channelNum = mFreq2ChannelNum.get(currentNetwork.getFrequency()).intValue();
            Log.d(TAG, "Channel number :" + channelNum + " for frequency : " + currentNetwork.getFrequency());
            for (String equals : channelList) {
                if (equals.equals(Integer.toString(channelNum))) {
                    Log.d(TAG, "STA connected to indoor channel. Take the user consent for turning on MHS");
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isWifiConnected() {
        boolean result = false;
        ConnectivityManager connectivity = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivity != null) {
            result = connectivity.getNetworkInfo(1).isConnected();
        }
        if (!result) {
            Log.d(TAG, "isWifiConnected1 :" + result);
            WifiConfiguration config = getSpecificNetwork(this.mClientModeImpl.getWifiInfo().getNetworkId());
            if (config != null && config.isCaptivePortal && !config.isAuthenticated) {
                result = true;
            }
        }
        Log.d(TAG, "isWifiConnected :" + result);
        return result;
    }

    public boolean acquireWifiLock(IBinder binder, int lockMode, String tag, WorkSource ws) {
        this.mLog.info("acquireWifiLock uid=% lockMode=%").mo2069c((long) Binder.getCallingUid()).mo2069c((long) lockMode).flush();
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", (String) null);
        WorkSource updatedWs = (ws == null || ws.isEmpty()) ? new WorkSource(Binder.getCallingUid()) : ws;
        if (WifiLockManager.isValidLockMode(lockMode)) {
            GeneralUtil.Mutable<Boolean> lockSuccess = new GeneralUtil.Mutable<>();
            if (this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(lockSuccess, lockMode, tag, binder, updatedWs) {
                private final /* synthetic */ GeneralUtil.Mutable f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ String f$3;
                private final /* synthetic */ IBinder f$4;
                private final /* synthetic */ WorkSource f$5;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$acquireWifiLock$13$WifiServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
                }
            }, IWCEventManager.autoDisconnectThreshold)) {
                return ((Boolean) lockSuccess.value).booleanValue();
            }
            Log.e(TAG, "Failed to post runnable to acquireWifiLock");
            return false;
        }
        throw new IllegalArgumentException("lockMode =" + lockMode);
    }

    public /* synthetic */ void lambda$acquireWifiLock$13$WifiServiceImpl(GeneralUtil.Mutable lockSuccess, int lockMode, String tag, IBinder binder, WorkSource updatedWs) {
        lockSuccess.value = Boolean.valueOf(this.mWifiLockManager.acquireWifiLock(lockMode, tag, binder, updatedWs));
    }

    public void updateWifiLockWorkSource(IBinder binder, WorkSource ws) {
        this.mLog.info("updateWifiLockWorkSource uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", (String) null);
        if (!this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(binder, (ws == null || ws.isEmpty()) ? new WorkSource(Binder.getCallingUid()) : ws) {
            private final /* synthetic */ IBinder f$1;
            private final /* synthetic */ WorkSource f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$updateWifiLockWorkSource$14$WifiServiceImpl(this.f$1, this.f$2);
            }
        }, IWCEventManager.autoDisconnectThreshold)) {
            Log.e(TAG, "Failed to post runnable to updateWifiLockWorkSource");
        }
    }

    public /* synthetic */ void lambda$updateWifiLockWorkSource$14$WifiServiceImpl(IBinder binder, WorkSource updatedWs) {
        this.mWifiLockManager.updateWifiLockWorkSource(binder, updatedWs);
    }

    public boolean releaseWifiLock(IBinder binder) {
        this.mLog.info("releaseWifiLock uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", (String) null);
        GeneralUtil.Mutable<Boolean> lockSuccess = new GeneralUtil.Mutable<>();
        if (this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(lockSuccess, binder) {
            private final /* synthetic */ GeneralUtil.Mutable f$1;
            private final /* synthetic */ IBinder f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$releaseWifiLock$15$WifiServiceImpl(this.f$1, this.f$2);
            }
        }, IWCEventManager.autoDisconnectThreshold)) {
            return ((Boolean) lockSuccess.value).booleanValue();
        }
        Log.e(TAG, "Failed to post runnable to releaseWifiLock");
        return false;
    }

    public /* synthetic */ void lambda$releaseWifiLock$15$WifiServiceImpl(GeneralUtil.Mutable lockSuccess, IBinder binder) {
        lockSuccess.value = Boolean.valueOf(this.mWifiLockManager.releaseWifiLock(binder));
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();
        this.mLog.info("initializeMulticastFiltering uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.initializeFiltering();
    }

    public void acquireMulticastLock(IBinder binder, String tag) {
        enforceMulticastChangePermission();
        this.mLog.info("acquireMulticastLock uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.acquireLock(binder, tag);
    }

    public void releaseMulticastLock(String tag) {
        enforceMulticastChangePermission();
        this.mLog.info("releaseMulticastLock uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.releaseLock(tag);
    }

    public boolean isMulticastEnabled() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isMulticastEnabled uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mWifiMulticastLockManager.isMulticastEnabled();
    }

    public void enableVerboseLogging(int verbose) {
        enforceAccessPermission();
        enforceNetworkSettingsPermission();
        this.mLog.info("enableVerboseLogging uid=% verbose=%").mo2069c((long) Binder.getCallingUid()).mo2069c((long) verbose).flush();
        this.mFacade.setIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", verbose);
        enableVerboseLoggingInternal(verbose);
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLoggingInternal(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        this.mClientModeImpl.enableVerboseLogging(verbose);
        this.mWifiLockManager.enableVerboseLogging(verbose);
        this.mWifiMulticastLockManager.enableVerboseLogging(verbose);
        this.mWifiInjector.enableVerboseLogging(verbose);
    }

    public int getVerboseLoggingLevel() {
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getVerboseLoggingLevel uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0);
    }

    public void setMaxClientVzwBasedOnNetworkType(int networkType) {
        if ("VZW".equals(CONFIGOPBRANDINGFORMOBILEAP)) {
            int wifiApState = getWifiApEnabledState();
            if (wifiApState == 12 || wifiApState == 13) {
                int maxClientNum = 5;
                if (networkType == 13) {
                    maxClientNum = WifiApConfigStore.MAX_CLIENT;
                }
                Message msg = new Message();
                msg.what = 14;
                Bundle b = new Bundle();
                b.putInt("maxClient", maxClientNum);
                msg.obj = b;
                callSECApi(msg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkAndSarBackOffFor5G(ServiceState serviceState) {
        Log.d(TAG, "serviceState.getNrBearerStatus=" + serviceState.getNrBearerStatus() + " / mPrevNrState=" + this.mPrevNrState);
        if (serviceState.getNrBearerStatus() == 1) {
            if (this.mPrevNrState != 1) {
                if (getWifiApEnabledState() == 13) {
                    this.mWifiNative.setHotspotBackOff(6);
                } else if (getWifiEnabledState() == 3) {
                    this.mClientModeImpl.set5GSarBackOff(6);
                }
            }
            this.mPrevNrState = 1;
        } else if (serviceState.getNrBearerStatus() == 2) {
            if (this.mPrevNrState != 2) {
                if (getWifiApEnabledState() == 13) {
                    this.mWifiNative.setHotspotBackOff(4);
                } else if (getWifiEnabledState() == 3) {
                    this.mClientModeImpl.set5GSarBackOff(4);
                }
            }
            this.mPrevNrState = 2;
        } else {
            int i = this.mPrevNrState;
            if (i == 2) {
                if (getWifiApEnabledState() == 13) {
                    this.mWifiNative.setHotspotBackOff(3);
                } else if (getWifiEnabledState() == 3) {
                    this.mClientModeImpl.set5GSarBackOff(3);
                }
            } else if (i == 1) {
                if (getWifiApEnabledState() == 13) {
                    this.mWifiNative.setHotspotBackOff(5);
                } else if (getWifiEnabledState() == 3) {
                    this.mClientModeImpl.set5GSarBackOff(5);
                }
            }
            this.mPrevNrState = 0;
        }
    }

    public void factoryReset(String packageName) {
        enforceConnectivityInternalPermission();
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("factoryReset uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            if (!this.mUserManager.hasUserRestriction("no_network_reset")) {
                if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
                    stopSoftApInternal(-1);
                }
                if (!this.mUserManager.hasUserRestriction("no_config_wifi")) {
                    if (this.mClientModeImplChannel != null) {
                        List<WifiConfiguration> networks = this.mClientModeImpl.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mClientModeImplChannel, 1010);
                        if (networks != null) {
                            for (WifiConfiguration config : networks) {
                                removeNetwork(config.networkId, packageName);
                            }
                        }
                        List<PasspointConfiguration> configs = this.mClientModeImpl.syncGetPasspointConfigs(this.mClientModeImplChannel);
                        if (configs != null) {
                            for (PasspointConfiguration config2 : configs) {
                                if (!(config2.getHomeSp() == null || config2.getHomeSp().getProviderType() == 2)) {
                                    removePasspointConfiguration(config2.getHomeSp().getFqdn(), packageName);
                                }
                            }
                        }
                    }
                    this.mClientModeImpl.disconnectCommand(Binder.getCallingUid(), 23);
                    this.mWifiInjector.getClientModeImplHandler().post(new Runnable() {
                        public final void run() {
                            WifiServiceImpl.this.lambda$factoryReset$16$WifiServiceImpl();
                        }
                    });
                    notifyFactoryReset();
                    AutoWifiController autoWifiController = this.mAutoWifiController;
                    if (autoWifiController != null) {
                        autoWifiController.factoryReset();
                    }
                    WifiRoamingAssistant wifiRoamingAssistant = this.mWifiRoamingAssistant;
                    if (wifiRoamingAssistant != null) {
                        wifiRoamingAssistant.factoryReset();
                    }
                }
                this.mDefaultApController.factoryReset();
                this.mPasspointDefaultProvider.addDefaultProviderFromCredential();
                IWCMonitor iWCMonitor = this.mIWCMonitor;
                if (iWCMonitor != null) {
                    iWCMonitor.sendMessage(IWCMonitor.FACTORY_RESET_REQUIRED);
                }
                SemWifiApSmartGattClient semWifiApSmartGattClient = this.mSemWifiApSmartGattClient;
                if (semWifiApSmartGattClient != null) {
                    semWifiApSmartGattClient.factoryReset();
                }
                SemWifiApSmartGattServer semWifiApSmartGattServer = this.mSemWifiApSmartGattServer;
                if (semWifiApSmartGattServer != null) {
                    semWifiApSmartGattServer.factoryReset();
                }
            }
        }
    }

    public /* synthetic */ void lambda$factoryReset$16$WifiServiceImpl() {
        this.mWifiInjector.getWifiConfigManager().clearDeletedEphemeralNetworks();
        this.mClientModeImpl.clearNetworkRequestUserApprovedAccessPoints();
        this.mWifiNetworkSuggestionsManager.clear();
        this.mWifiInjector.getWifiScoreCard().clear();
    }

    static boolean logAndReturnFalse(String s) {
        Log.d(TAG, s);
        return false;
    }

    private void notifyFactoryReset() {
        Intent intent = new Intent("android.net.wifi.action.NETWORK_SETTINGS_RESET");
        intent.addFlags(16777216);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.NETWORK_CARRIER_PROVISIONING");
    }

    public Network getCurrentNetwork() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCurrentNetwork uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        return this.mClientModeImpl.getCurrentNetwork();
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        enforceConnectivityInternalPermission();
        this.mLog.info("enableWifiConnectivityManager uid=% enabled=%").mo2069c((long) Binder.getCallingUid()).mo2071c(enabled).flush();
        this.mClientModeImpl.enableWifiConnectivityManager(enabled);
    }

    public byte[] retrieveBackupData() {
        enforceNetworkSettingsPermission();
        this.mLog.info("retrieveBackupData uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mClientModeImplChannel == null) {
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return null;
        }
        Slog.d(TAG, "Retrieving backup data");
        byte[] backupData = this.mWifiBackupRestore.retrieveBackupDataFromConfigurations(this.mClientModeImpl.syncGetPrivilegedConfiguredNetwork(this.mClientModeImplChannel));
        Slog.d(TAG, "Retrieved backup data");
        return backupData;
    }

    private void restoreNetworks(List<WifiConfiguration> configurations) {
        if (configurations == null) {
            Slog.e(TAG, "Backup data parse failed");
            return;
        }
        for (WifiConfiguration configuration : configurations) {
            int networkId = this.mClientModeImpl.syncAddOrUpdateNetwork(this.mClientModeImplChannel, configuration);
            if (networkId == -1) {
                Slog.e(TAG, "Restore network failed: " + configuration.configKey());
            } else {
                this.mClientModeImpl.syncEnableNetwork(this.mClientModeImplChannel, networkId, false);
            }
        }
    }

    public void restoreBackupData(byte[] data) {
        enforceNetworkSettingsPermission();
        this.mLog.info("restoreBackupData uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mClientModeImplChannel == null) {
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromBackupData(data));
        Slog.d(TAG, "Restored backup data");
    }

    public void restoreSupplicantBackupData(byte[] supplicantData, byte[] ipConfigData) {
        enforceNetworkSettingsPermission();
        this.mLog.trace("restoreSupplicantBackupData uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        if (this.mClientModeImplChannel == null) {
            Slog.e(TAG, "mClientModeImplChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring supplicant backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromSupplicantBackupData(supplicantData, ipConfigData));
        Slog.d(TAG, "Restored supplicant backup data");
    }

    public void startSubscriptionProvisioning(OsuProvider provider, IProvisioningCallback callback) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        } else if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            int uid = Binder.getCallingUid();
            this.mLog.trace("startSubscriptionProvisioning uid=%").mo2069c((long) uid).flush();
            if (this.mClientModeImpl.syncStartSubscriptionProvisioning(uid, provider, callback, this.mClientModeImplChannel)) {
                this.mLog.trace("Subscription provisioning started with %").mo2070c(provider.toString()).flush();
            }
        } else {
            throw new SecurityException("WifiService: Permission denied");
        }
    }

    public void registerTrafficStateCallback(IBinder binder, ITrafficStateCallback callback, int callbackIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("registerTrafficStateCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            this.mWifiInjector.getClientModeImplHandler().post(new Runnable(binder, callback, callbackIdentifier) {
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ ITrafficStateCallback f$2;
                private final /* synthetic */ int f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$registerTrafficStateCallback$17$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
                }
            });
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$registerTrafficStateCallback$17$WifiServiceImpl(IBinder binder, ITrafficStateCallback callback, int callbackIdentifier) {
        this.mWifiTrafficPoller.addCallback(binder, callback, callbackIdentifier);
    }

    public void unregisterTrafficStateCallback(int callbackIdentifier) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterTrafficStateCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(callbackIdentifier) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$unregisterTrafficStateCallback$18$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$unregisterTrafficStateCallback$18$WifiServiceImpl(int callbackIdentifier) {
        this.mWifiTrafficPoller.removeCallback(callbackIdentifier);
    }

    private boolean is5GhzSupported() {
        return (getSupportedFeaturesInternal() & 2) == 2;
    }

    private long getSupportedFeaturesInternal() {
        AsyncChannel channel = this.mClientModeImplChannel;
        if (channel != null) {
            return this.mClientModeImpl.syncGetSupportedFeatures(channel);
        }
        Slog.e(TAG, "mClientModeImplChannel is not initialized");
        return 0;
    }

    private static boolean hasAutomotiveFeature(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
    }

    public void registerNetworkRequestMatchCallback(IBinder binder, INetworkRequestMatchCallback callback, int callbackIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("registerNetworkRequestMatchCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            this.mWifiInjector.getClientModeImplHandler().post(new Runnable(binder, callback, callbackIdentifier) {
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ INetworkRequestMatchCallback f$2;
                private final /* synthetic */ int f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$registerNetworkRequestMatchCallback$19$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
                }
            });
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$registerNetworkRequestMatchCallback$19$WifiServiceImpl(IBinder binder, INetworkRequestMatchCallback callback, int callbackIdentifier) {
        this.mClientModeImpl.addNetworkRequestMatchCallback(binder, callback, callbackIdentifier);
    }

    public void unregisterNetworkRequestMatchCallback(int callbackIdentifier) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterNetworkRequestMatchCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(callbackIdentifier) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$unregisterNetworkRequestMatchCallback$20$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$unregisterNetworkRequestMatchCallback$20$WifiServiceImpl(int callbackIdentifier) {
        this.mClientModeImpl.removeNetworkRequestMatchCallback(callbackIdentifier);
    }

    public int addNetworkSuggestions(List<WifiNetworkSuggestion> networkSuggestions, String callingPackageName) {
        if (enforceChangePermission(callingPackageName) != 0) {
            return 2;
        }
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("addNetworkSuggestions uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        int callingUid = Binder.getCallingUid();
        GeneralUtil.Mutable<Integer> success = new GeneralUtil.Mutable<>();
        if (!this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(success, networkSuggestions, callingUid, callingPackageName) {
            private final /* synthetic */ GeneralUtil.Mutable f$1;
            private final /* synthetic */ List f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ String f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$addNetworkSuggestions$21$WifiServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        }, IWCEventManager.autoDisconnectThreshold)) {
            Log.e(TAG, "Failed to post runnable to add network suggestions");
            return 1;
        }
        if (((Integer) success.value).intValue() != 0) {
            Log.e(TAG, "Failed to add network suggestions");
        }
        return ((Integer) success.value).intValue();
    }

    public /* synthetic */ void lambda$addNetworkSuggestions$21$WifiServiceImpl(GeneralUtil.Mutable success, List networkSuggestions, int callingUid, String callingPackageName) {
        success.value = Integer.valueOf(this.mWifiNetworkSuggestionsManager.add(networkSuggestions, callingUid, callingPackageName));
    }

    public int removeNetworkSuggestions(List<WifiNetworkSuggestion> networkSuggestions, String callingPackageName) {
        if (enforceChangePermission(callingPackageName) != 0) {
            return 2;
        }
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("removeNetworkSuggestions uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        int callingUid = Binder.getCallingUid();
        GeneralUtil.Mutable<Integer> success = new GeneralUtil.Mutable<>();
        if (!this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(success, networkSuggestions, callingUid, callingPackageName) {
            private final /* synthetic */ GeneralUtil.Mutable f$1;
            private final /* synthetic */ List f$2;
            private final /* synthetic */ int f$3;
            private final /* synthetic */ String f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$removeNetworkSuggestions$22$WifiServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        }, IWCEventManager.autoDisconnectThreshold)) {
            Log.e(TAG, "Failed to post runnable to remove network suggestions");
            return 1;
        }
        if (((Integer) success.value).intValue() != 0) {
            Log.e(TAG, "Failed to remove network suggestions");
        }
        return ((Integer) success.value).intValue();
    }

    public /* synthetic */ void lambda$removeNetworkSuggestions$22$WifiServiceImpl(GeneralUtil.Mutable success, List networkSuggestions, int callingUid, String callingPackageName) {
        success.value = Integer.valueOf(this.mWifiNetworkSuggestionsManager.remove(networkSuggestions, callingUid, callingPackageName));
    }

    public String[] getFactoryMacAddresses() {
        int uid = Binder.getCallingUid();
        if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid)) {
            List<String> result = new ArrayList<>();
            if (!this.mWifiInjector.getClientModeImplHandler().runWithScissors(new Runnable(result) {
                private final /* synthetic */ List f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$getFactoryMacAddresses$23$WifiServiceImpl(this.f$1);
                }
            }, IWCEventManager.autoDisconnectThreshold) || result.isEmpty()) {
                return null;
            }
            return (String[]) result.stream().toArray($$Lambda$WifiServiceImpl$EfgfTvi04qWi6e59wo2Ap33XY.INSTANCE);
        }
        throw new SecurityException("App not allowed to get Wi-Fi factory MAC address (uid = " + uid + ")");
    }

    public /* synthetic */ void lambda$getFactoryMacAddresses$23$WifiServiceImpl(List result) {
        String mac = this.mClientModeImpl.getFactoryMacAddress();
        if (mac != null) {
            result.add(mac);
        }
    }

    static /* synthetic */ String[] lambda$getFactoryMacAddresses$24(int x$0) {
        return new String[x$0];
    }

    public void setDeviceMobilityState(int state) {
        this.mContext.enforceCallingPermission("android.permission.WIFI_SET_DEVICE_MOBILITY_STATE", TAG);
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("setDeviceMobilityState uid=% state=%").mo2069c((long) Binder.getCallingUid()).mo2069c((long) state).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(state) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$setDeviceMobilityState$25$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$setDeviceMobilityState$25$WifiServiceImpl(int state) {
        this.mClientModeImpl.setDeviceMobilityState(state);
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void startDppAsConfiguratorInitiator(IBinder binder, String enrolleeUri, int selectedNetworkId, int netRole, IDppCallback callback) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (TextUtils.isEmpty(enrolleeUri)) {
            throw new IllegalArgumentException("Enrollee URI must not be null or empty");
        } else if (selectedNetworkId < 0) {
            throw new IllegalArgumentException("Selected network ID invalid");
        } else if (callback != null) {
            int uid = getMockableCallingUid();
            if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
                this.mDppManager.mHandler.post(new Runnable(uid, binder, enrolleeUri, selectedNetworkId, netRole, callback) {
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ IBinder f$2;
                    private final /* synthetic */ String f$3;
                    private final /* synthetic */ int f$4;
                    private final /* synthetic */ int f$5;
                    private final /* synthetic */ IDppCallback f$6;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                        this.f$4 = r5;
                        this.f$5 = r6;
                        this.f$6 = r7;
                    }

                    public final void run() {
                        WifiServiceImpl.this.lambda$startDppAsConfiguratorInitiator$26$WifiServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
                    }
                });
                return;
            }
            throw new SecurityException("WifiService: Permission denied");
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$startDppAsConfiguratorInitiator$26$WifiServiceImpl(int uid, IBinder binder, String enrolleeUri, int selectedNetworkId, int netRole, IDppCallback callback) {
        this.mDppManager.startDppAsConfiguratorInitiator(uid, binder, enrolleeUri, selectedNetworkId, netRole, callback);
    }

    public void startDppAsEnrolleeInitiator(IBinder binder, String configuratorUri, IDppCallback callback) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (TextUtils.isEmpty(configuratorUri)) {
            throw new IllegalArgumentException("Enrollee URI must not be null or empty");
        } else if (callback != null) {
            int uid = getMockableCallingUid();
            if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
                this.mDppManager.mHandler.post(new Runnable(uid, binder, configuratorUri, callback) {
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ IBinder f$2;
                    private final /* synthetic */ String f$3;
                    private final /* synthetic */ IDppCallback f$4;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                        this.f$4 = r5;
                    }

                    public final void run() {
                        WifiServiceImpl.this.lambda$startDppAsEnrolleeInitiator$27$WifiServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4);
                    }
                });
                return;
            }
            throw new SecurityException("WifiService: Permission denied");
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$startDppAsEnrolleeInitiator$27$WifiServiceImpl(int uid, IBinder binder, String configuratorUri, IDppCallback callback) {
        this.mDppManager.startDppAsEnrolleeInitiator(uid, binder, configuratorUri, callback);
    }

    public void stopDppSession() throws RemoteException {
        if (isSettingsOrSuw(Binder.getCallingPid(), Binder.getCallingUid())) {
            this.mDppManager.mHandler.post(new Runnable(getMockableCallingUid()) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$stopDppSession$28$WifiServiceImpl(this.f$1);
                }
            });
            return;
        }
        throw new SecurityException("WifiService: Permission denied");
    }

    public /* synthetic */ void lambda$stopDppSession$28$WifiServiceImpl(int uid) {
        this.mDppManager.stopDppSession(uid);
    }

    public void addOnWifiUsabilityStatsListener(IBinder binder, IOnWifiUsabilityStatsListener listener, int listenerIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (listener != null) {
            this.mContext.enforceCallingPermission("android.permission.WIFI_UPDATE_USABILITY_STATS_SCORE", TAG);
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("addOnWifiUsabilityStatsListener uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            this.mWifiInjector.getClientModeImplHandler().post(new Runnable(binder, listener, listenerIdentifier) {
                private final /* synthetic */ IBinder f$1;
                private final /* synthetic */ IOnWifiUsabilityStatsListener f$2;
                private final /* synthetic */ int f$3;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                }

                public final void run() {
                    WifiServiceImpl.this.lambda$addOnWifiUsabilityStatsListener$29$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
                }
            });
        } else {
            throw new IllegalArgumentException("Listener must not be null");
        }
    }

    public /* synthetic */ void lambda$addOnWifiUsabilityStatsListener$29$WifiServiceImpl(IBinder binder, IOnWifiUsabilityStatsListener listener, int listenerIdentifier) {
        this.mWifiMetrics.addOnWifiUsabilityListener(binder, listener, listenerIdentifier);
    }

    public void removeOnWifiUsabilityStatsListener(int listenerIdentifier) {
        this.mContext.enforceCallingPermission("android.permission.WIFI_UPDATE_USABILITY_STATS_SCORE", TAG);
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("removeOnWifiUsabilityStatsListener uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(listenerIdentifier) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$removeOnWifiUsabilityStatsListener$30$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$removeOnWifiUsabilityStatsListener$30$WifiServiceImpl(int listenerIdentifier) {
        this.mWifiMetrics.removeOnWifiUsabilityListener(listenerIdentifier);
    }

    public void updateWifiUsabilityScore(int seqNum, int score, int predictionHorizonSec) {
        this.mContext.enforceCallingPermission("android.permission.WIFI_UPDATE_USABILITY_STATS_SCORE", TAG);
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("updateWifiUsabilityScore uid=% seqNum=% score=% predictionHorizonSec=%").mo2069c((long) Binder.getCallingUid()).mo2069c((long) seqNum).mo2069c((long) score).mo2069c((long) predictionHorizonSec).flush();
        }
        this.mWifiInjector.getClientModeImplHandler().post(new Runnable(seqNum, score, predictionHorizonSec) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ int f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$updateWifiUsabilityScore$31$WifiServiceImpl(this.f$1, this.f$2, this.f$3);
            }
        });
    }

    public /* synthetic */ void lambda$updateWifiUsabilityScore$31$WifiServiceImpl(int seqNum, int score, int predictionHorizonSec) {
        this.mClientModeImpl.updateWifiUsabilityScore(seqNum, score, predictionHorizonSec);
    }

    /* JADX INFO: finally extract failed */
    /* JADX WARNING: Code restructure failed: missing block: B:206:0x043c, code lost:
        if (r11.mWifiInjector.getWifiB2bConfigPolicy().isPolicyApplied() == false) goto L_0x0444;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:207:0x043e, code lost:
        android.util.Log.e(TAG, "Error set NCHO API - EDM is applied.");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:208:0x0443, code lost:
        return -1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:209:0x0444, code lost:
        enforceChangePermission();
        r0 = r11.mClientModeImplChannel;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:210:0x0449, code lost:
        if (r0 == null) goto L_0x06b0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:211:0x044b, code lost:
        r1 = r11.mClientModeImpl.syncCallSECApi(r0, r12);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int callSECApi(android.os.Message r12) {
        /*
            r11 = this;
            java.lang.String r0 = "mac"
            r1 = -1
            if (r12 == 0) goto L_0x06b3
            int r2 = android.os.Binder.getCallingPid()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "callSECApi msg.what="
            r3.append(r4)
            int r4 = r12.what
            r3.append(r4)
            java.lang.String r4 = ", callingPid:"
            r3.append(r4)
            r3.append(r2)
            java.lang.String r3 = r3.toString()
            java.lang.String r4 = "WifiService"
            android.util.Log.d(r4, r3)
            r3 = 0
            int r5 = r12.what
            r6 = 3
            if (r5 == r6) goto L_0x0691
            r6 = 4
            if (r5 == r6) goto L_0x0653
            r0 = 5
            if (r5 == r0) goto L_0x0641
            r0 = 6
            r6 = 0
            r7 = 1
            if (r5 == r0) goto L_0x05fe
            r0 = 17
            if (r5 == r0) goto L_0x05f0
            r0 = 18
            if (r5 == r0) goto L_0x05e4
            r0 = 70
            if (r5 == r0) goto L_0x05b8
            r0 = 71
            if (r5 == r0) goto L_0x0566
            r0 = 73
            if (r5 == r0) goto L_0x0562
            r0 = 74
            java.lang.String r8 = "enable"
            if (r5 == r0) goto L_0x0539
            r0 = 81
            if (r5 == r0) goto L_0x052d
            r0 = 82
            if (r5 == r0) goto L_0x052d
            r0 = 87
            java.lang.String r9 = "packageName"
            if (r5 == r0) goto L_0x0501
            r0 = 88
            if (r5 == r0) goto L_0x04e4
            java.lang.String r0 = "apiName"
            java.lang.String r10 = "oauth_provider"
            switch(r5) {
                case 1: goto L_0x05ac;
                case 14: goto L_0x04b6;
                case 24: goto L_0x052d;
                case 26: goto L_0x0488;
                case 28: goto L_0x0484;
                case 46: goto L_0x0469;
                case 90: goto L_0x0454;
                case 100: goto L_0x0444;
                case 101: goto L_0x0432;
                case 102: goto L_0x0444;
                case 103: goto L_0x0432;
                case 104: goto L_0x0444;
                case 105: goto L_0x0432;
                case 106: goto L_0x0432;
                case 107: goto L_0x0432;
                case 130: goto L_0x0432;
                case 131: goto L_0x0432;
                case 132: goto L_0x0432;
                case 133: goto L_0x0432;
                case 134: goto L_0x0432;
                case 135: goto L_0x0432;
                case 136: goto L_0x0432;
                case 137: goto L_0x0432;
                case 150: goto L_0x0432;
                case 151: goto L_0x0432;
                case 161: goto L_0x0432;
                case 162: goto L_0x0432;
                case 163: goto L_0x0432;
                case 164: goto L_0x0432;
                case 165: goto L_0x0432;
                case 170: goto L_0x0432;
                case 171: goto L_0x0432;
                case 180: goto L_0x0426;
                case 181: goto L_0x0405;
                case 197: goto L_0x03e6;
                case 198: goto L_0x03db;
                case 201: goto L_0x03b5;
                case 222: goto L_0x039c;
                case 230: goto L_0x0388;
                case 242: goto L_0x05e4;
                case 267: goto L_0x052d;
                case 279: goto L_0x0357;
                case 280: goto L_0x052d;
                case 281: goto L_0x052d;
                case 282: goto L_0x034b;
                case 283: goto L_0x052d;
                case 290: goto L_0x0328;
                case 292: goto L_0x0319;
                case 293: goto L_0x02c9;
                case 294: goto L_0x0295;
                case 299: goto L_0x0289;
                case 301: goto L_0x0271;
                case 303: goto L_0x0263;
                case 304: goto L_0x0255;
                case 307: goto L_0x0240;
                case 314: goto L_0x0239;
                case 330: goto L_0x052d;
                case 401: goto L_0x021c;
                case 402: goto L_0x020f;
                case 404: goto L_0x01a5;
                case 405: goto L_0x017a;
                case 407: goto L_0x052d;
                case 408: goto L_0x016e;
                case 409: goto L_0x012a;
                case 500: goto L_0x011a;
                default: goto L_0x006d;
            }
        L_0x006d:
            switch(r5) {
                case 41: goto L_0x010c;
                case 42: goto L_0x00fe;
                case 43: goto L_0x00ef;
                case 44: goto L_0x00c9;
                default: goto L_0x0070;
            }
        L_0x0070:
            switch(r5) {
                case 77: goto L_0x052d;
                case 78: goto L_0x008e;
                case 79: goto L_0x052d;
                default: goto L_0x0073;
            }
        L_0x0073:
            switch(r5) {
                case 109: goto L_0x0432;
                case 110: goto L_0x0444;
                case 111: goto L_0x0432;
                default: goto L_0x0076;
            }
        L_0x0076:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r5 = "not implementated yet. command id:"
            r0.append(r5)
            int r5 = r12.what
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            android.util.Log.e(r4, r0)
            goto L_0x06b0
        L_0x008e:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = "extra_type"
            java.lang.String r0 = r3.getString(r0)
            java.lang.String r4 = "MHS"
            boolean r0 = r4.equals(r0)
            java.lang.String r4 = "extra_log"
            if (r0 == 0) goto L_0x00be
            com.android.server.wifi.WifiInjector r0 = r11.mWifiInjector
            com.samsung.android.server.wifi.softap.SemSoftapConfig r0 = r0.getSemSoftapConfig()
            java.lang.String r4 = r3.getString(r4)
            r0.addMHSDumpLog(r4)
            goto L_0x00c5
        L_0x00be:
            java.lang.String r0 = r3.getString(r4)
            r11.addHistoricalDumpLog(r0)
        L_0x00c5:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x00c9:
            r11.enforceNetworkSettingsPermission()
            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = r11.mSemQoSProfileShareProvider
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = r11.mSemQoSProfileShareProvider
            int r4 = r12.arg1
            if (r4 != r7) goto L_0x00e2
            r6 = r7
        L_0x00e2:
            java.lang.String r4 = "userData"
            java.lang.String r4 = r3.getString(r4)
            r0.setUserConfirm(r6, r4)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x00ef:
            r11.enforceNetworkSettingsPermission()
            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = r11.mSemQoSProfileShareProvider
            if (r0 == 0) goto L_0x06b0
            r4 = 0
            r0.requestPassword(r6, r4, r4)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x00fe:
            r11.enforceNetworkSettingsPermission()
            android.content.Context r0 = r11.mContext
            com.samsung.android.server.wifi.WifiGuiderFeatureController r0 = com.samsung.android.server.wifi.WifiGuiderFeatureController.getInstance(r0)
            boolean r0 = r0.isSupportWifiProfileRequest()
            return r0
        L_0x010c:
            r11.enforceNetworkSettingsPermission()
            android.content.Context r0 = r11.mContext
            com.samsung.android.server.wifi.WifiGuiderFeatureController r0 = com.samsung.android.server.wifi.WifiGuiderFeatureController.getInstance(r0)
            boolean r0 = r0.isSupportSamsungNetworkScore()
            return r0
        L_0x011a:
            r11.enforceChangePermission()
            com.android.internal.util.AsyncChannel r0 = r11.mClientModeImplChannel
            if (r0 == 0) goto L_0x06b0
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            int r0 = r0.callSECApi(r12)
            r1 = r0
            goto L_0x06b0
        L_0x012a:
            java.lang.String r0 = android.os.Build.TYPE
            java.lang.String r5 = "user"
            boolean r0 = r5.equals(r0)
            if (r0 != 0) goto L_0x0167
            boolean r0 = android.os.Debug.semIsProductDev()
            if (r0 != 0) goto L_0x013b
            goto L_0x0167
        L_0x013b:
            com.android.server.wifi.IWCMonitor r0 = r11.mIWCMonitor
            if (r0 != 0) goto L_0x0146
            java.lang.String r0 = "Not support in IWC non-support device"
            android.util.Log.d(r4, r0)
            goto L_0x06b0
        L_0x0146:
            int r0 = r12.arg1
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "sendMessage with action "
            r5.append(r6)
            r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.d(r4, r5)
            com.android.server.wifi.IWCMonitor r4 = r11.mIWCMonitor
            r5 = 552993(0x87021, float:7.74908E-40)
            r4.sendMessage(r5, r0)
            r1 = 0
            goto L_0x06b0
        L_0x0167:
            java.lang.String r0 = "SEC_COMMAND_ID_IWC_SET_FORCE_ACTION command does not support in commercial device"
            android.util.Log.d(r4, r0)
            goto L_0x06b0
        L_0x016e:
            r11.enforceChangePermission()
            int r0 = r12.arg1
            int r4 = r12.arg2
            r11.restoreUserPreference(r0, r4)
            goto L_0x06b0
        L_0x017a:
            r11.enforceProvideDiagnosticsPermission()
            java.lang.Object r4 = r12.obj
            if (r4 == 0) goto L_0x06b0
            java.lang.Object r4 = r12.obj
            boolean r4 = r4 instanceof android.os.Bundle
            if (r4 == 0) goto L_0x06b0
            java.lang.Object r4 = r12.obj
            r3 = r4
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = r3.getString(r0)
            java.lang.String r4 = "period"
            int r4 = r3.getInt(r4)
            java.lang.String r5 = "counter"
            int r5 = r3.getInt(r5)
            com.samsung.android.server.wifi.WifiGuiderPackageMonitor r6 = r11.mWifiGuiderPackageMonitor
            r6.registerApi(r0, r4, r5)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x01a5:
            java.lang.Object r5 = r12.obj
            if (r5 == 0) goto L_0x06b0
            java.lang.Object r5 = r12.obj
            boolean r5 = r5 instanceof android.os.Bundle
            if (r5 == 0) goto L_0x06b0
            java.lang.Object r5 = r12.obj
            r3 = r5
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r5 = "packageUid"
            int r5 = r3.getInt(r5)
            java.lang.String r6 = r3.getString(r9)
            java.lang.String r0 = r3.getString(r0)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "report call history packageName:"
            r7.append(r8)
            r7.append(r6)
            java.lang.String r8 = ", apiName:"
            r7.append(r8)
            r7.append(r0)
            java.lang.String r7 = r7.toString()
            android.util.Log.d(r4, r7)
            if (r0 == 0) goto L_0x020d
            if (r6 == 0) goto L_0x020d
            r4 = 1000(0x3e8, float:1.401E-42)
            if (r5 <= r4) goto L_0x020d
            long r7 = android.os.Binder.clearCallingIdentity()
            com.samsung.android.server.wifi.WifiGuiderPackageMonitor r4 = r11.mWifiGuiderPackageMonitor     // Catch:{ all -> 0x0208 }
            com.android.server.wifi.FrameworkFacade r9 = r11.mFrameworkFacade     // Catch:{ all -> 0x0208 }
            boolean r9 = r9.isAppForeground(r5)     // Catch:{ all -> 0x0208 }
            int r4 = r4.addApiLog(r6, r9, r0)     // Catch:{ all -> 0x0208 }
            r1 = r4
            if (r1 <= 0) goto L_0x0204
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl     // Catch:{ all -> 0x0208 }
            r9 = 500(0x1f4, float:7.0E-43)
            android.os.Bundle r10 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForCallingSpecificApiFrequently(r0, r6, r1)     // Catch:{ all -> 0x0208 }
            r4.report(r9, r10)     // Catch:{ all -> 0x0208 }
        L_0x0204:
            android.os.Binder.restoreCallingIdentity(r7)
            goto L_0x020d
        L_0x0208:
            r4 = move-exception
            android.os.Binder.restoreCallingIdentity(r7)
            throw r4
        L_0x020d:
            goto L_0x06b0
        L_0x020f:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            r11.startFromBeginning(r3)
            goto L_0x06b0
        L_0x021c:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = "json"
            java.lang.String r0 = r3.getString(r0)
            r11.setQtables(r0)
            goto L_0x06b0
        L_0x0239:
            boolean r0 = r11.getIndoorStatus()
            r1 = r0
            goto L_0x06b0
        L_0x0240:
            r11.enforceAccessPermission()
            com.android.server.wifi.WifiConnectivityMonitor r0 = r11.mWifiConnectivityMonitor
            if (r0 == 0) goto L_0x0251
            boolean r0 = r0.getValidState()
            if (r0 == 0) goto L_0x0251
            r0 = 1
            r1 = r0
            goto L_0x06b0
        L_0x0251:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0255:
            r11.enforceAccessPermission()
            com.android.server.wifi.WifiConnectivityMonitor r0 = r11.mWifiConnectivityMonitor
            if (r0 == 0) goto L_0x06b0
            int r0 = com.android.server.wifi.WifiConnectivityMonitor.getEverQualityTested()
            r1 = r0
            goto L_0x06b0
        L_0x0263:
            r11.enforceChangePermission()
            com.android.server.wifi.WifiConnectivityMonitor r0 = r11.mWifiConnectivityMonitor
            if (r0 == 0) goto L_0x06b0
            int r0 = r0.getCurrentStatusMode()
            r1 = r0
            goto L_0x06b0
        L_0x0271:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            com.android.internal.util.AsyncChannel r4 = r11.mClientModeImplChannel
            int r0 = r0.syncCallSECApi(r4, r12)
            if (r0 != r7) goto L_0x0285
            com.android.server.wifi.WifiConnectivityMonitor r0 = r11.mWifiConnectivityMonitor
            if (r0 == 0) goto L_0x0285
            r0.resetWatchdogSettings()
        L_0x0285:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0289:
            r11.enforceAccessPermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            int r0 = r0.getCurrentGeofenceState()
            r1 = r0
            goto L_0x06b0
        L_0x0295:
            r11.enforceProvideDiagnosticsPermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = r3.getString(r10)
            boolean r4 = android.text.TextUtils.isEmpty(r0)
            if (r4 != 0) goto L_0x02c7
            long r4 = android.os.Binder.clearCallingIdentity()
            java.lang.String r7 = "agree"
            boolean r6 = r3.getBoolean(r7, r6)     // Catch:{ all -> 0x02c2 }
            r11.updateOAuthAgreement(r0, r6)     // Catch:{ all -> 0x02c2 }
            android.os.Binder.restoreCallingIdentity(r4)
            goto L_0x02c7
        L_0x02c2:
            r6 = move-exception
            android.os.Binder.restoreCallingIdentity(r4)
            throw r6
        L_0x02c7:
            goto L_0x06b0
        L_0x02c9:
            r11.enforceAccessPermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = r3.getString(r10)
            boolean r4 = android.text.TextUtils.isEmpty(r0)
            if (r4 != 0) goto L_0x0317
            long r4 = android.os.Binder.clearCallingIdentity()
            java.lang.String r6 = r11.getOAuthAgreements()     // Catch:{ all -> 0x0312 }
            boolean r7 = android.text.TextUtils.isEmpty(r6)     // Catch:{ all -> 0x0312 }
            if (r7 != 0) goto L_0x030e
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x0312 }
            r7.<init>()     // Catch:{ all -> 0x0312 }
            java.lang.String r8 = "["
            r7.append(r8)     // Catch:{ all -> 0x0312 }
            r7.append(r0)     // Catch:{ all -> 0x0312 }
            java.lang.String r8 = "]"
            r7.append(r8)     // Catch:{ all -> 0x0312 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x0312 }
            boolean r7 = r6.contains(r7)     // Catch:{ all -> 0x0312 }
            r1 = r7
        L_0x030e:
            android.os.Binder.restoreCallingIdentity(r4)
            goto L_0x0317
        L_0x0312:
            r6 = move-exception
            android.os.Binder.restoreCallingIdentity(r4)
            throw r6
        L_0x0317:
            goto L_0x06b0
        L_0x0319:
            r11.enforceAccessPermission()
            r0 = 0
            com.samsung.android.server.wifi.AutoWifiController r1 = r11.mAutoWifiController
            if (r1 == 0) goto L_0x0325
            r0 = 1
            r1 = r0
            goto L_0x06b0
        L_0x0325:
            r1 = r0
            goto L_0x06b0
        L_0x0328:
            r11.enforceChangePermission()
            boolean r0 = android.os.Debug.semIsProductDev()
            if (r0 != 0) goto L_0x0335
            boolean r0 = r11.semIsTestMode
            if (r0 == 0) goto L_0x0347
        L_0x0335:
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x0347
            com.samsung.android.server.wifi.AutoWifiController r0 = r11.mAutoWifiController
            if (r0 == 0) goto L_0x0347
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.samsung.android.server.wifi.AutoWifiController r0 = r11.mAutoWifiController
            r0.setConfigForTest(r3)
        L_0x0347:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x034b:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            r0.sendCallSECApiAsync(r12, r2)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0357:
            r11.enforceProvideDiagnosticsPermission()
            int r0 = r12.arg1
            if (r0 < 0) goto L_0x0384
            java.lang.Object r0 = r12.obj
            android.os.PersistableBundle r0 = (android.os.PersistableBundle) r0
            android.content.Context r4 = r11.mContext
            java.lang.String r5 = "carrier_config"
            java.lang.Object r4 = r4.getSystemService(r5)
            android.telephony.CarrierConfigManager r4 = (android.telephony.CarrierConfigManager) r4
            long r5 = android.os.Binder.clearCallingIdentity()
            int r7 = r12.arg1     // Catch:{ all -> 0x037f }
            r4.overrideConfig(r7, r0)     // Catch:{ all -> 0x037f }
            android.os.Binder.restoreCallingIdentity(r5)
            int r7 = r12.arg1
            r4.notifyConfigChangedForSubId(r7)
            goto L_0x0384
        L_0x037f:
            r7 = move-exception
            android.os.Binder.restoreCallingIdentity(r5)
            throw r7
        L_0x0384:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0388:
            r11.enforceChangePermission()
            r11.enforceConnectivityInternalPermission()
            com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver r0 = r11.mSemWifiApBroadcastReceiver
            if (r0 == 0) goto L_0x0395
            r0.stopTracking()
        L_0x0395:
            r11.shutdown()
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x039c:
            r11.enforceChangePermission()
            long r4 = android.os.Binder.clearCallingIdentity()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl     // Catch:{ all -> 0x03b0 }
            int r0 = r0.callSECApi(r12)     // Catch:{ all -> 0x03b0 }
            android.os.Binder.restoreCallingIdentity(r4)
            r1 = r0
            goto L_0x06b0
        L_0x03b0:
            r0 = move-exception
            android.os.Binder.restoreCallingIdentity(r4)
            throw r0
        L_0x03b5:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = "keep_connection"
            boolean r0 = r3.getBoolean(r0)
            if (r0 == 0) goto L_0x03ca
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            r4.sendCallSECApiAsync(r12, r2)
        L_0x03ca:
            com.android.server.wifi.WifiConnectivityMonitor r4 = r11.mWifiConnectivityMonitor
            if (r4 == 0) goto L_0x03d1
            r4.setUserSelection(r0)
        L_0x03d1:
            com.android.server.wifi.IWCMonitor r4 = r11.mIWCMonitor
            if (r4 == 0) goto L_0x03d8
            r4.sendUserSelection(r0)
        L_0x03d8:
            r1 = 0
            goto L_0x06b0
        L_0x03db:
            r11.enforceChangePermission()
            boolean r0 = r11.isWifiSharingEnabled()
            r0 = r0 ^ r7
            r1 = r0
            goto L_0x06b0
        L_0x03e6:
            r11.enforceChangePermission()
            com.android.server.wifi.SoftApModeConfiguration r0 = new com.android.server.wifi.SoftApModeConfiguration
            java.lang.Object r4 = r12.obj
            android.net.wifi.WifiConfiguration r4 = (android.net.wifi.WifiConfiguration) r4
            r0.<init>(r7, r4)
            com.android.server.wifi.WifiController r4 = r11.mWifiController
            r5 = 155670(0x26016, float:2.1814E-40)
            int r6 = r12.arg1
            int r7 = r12.arg2
            android.os.Message r4 = r4.obtainMessage(r5, r6, r7, r0)
            r4.sendToTarget()
            r1 = 0
            goto L_0x06b0
        L_0x0405:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            java.lang.String r4 = "mode"
            int r4 = r3.getInt(r4)
            int r0 = r0.setRoamDhcpPolicy(r4)
            r1 = r0
            goto L_0x06b0
        L_0x0426:
            r11.enforceAccessPermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            int r0 = r0.getRoamDhcpPolicy()
            r1 = r0
            goto L_0x06b0
        L_0x0432:
            com.android.server.wifi.WifiInjector r0 = r11.mWifiInjector
            com.samsung.android.server.wifi.WifiB2BConfigurationPolicy r0 = r0.getWifiB2bConfigPolicy()
            boolean r0 = r0.isPolicyApplied()
            if (r0 == 0) goto L_0x0444
            java.lang.String r0 = "Error set NCHO API - EDM is applied."
            android.util.Log.e(r4, r0)
            return r1
        L_0x0444:
            r11.enforceChangePermission()
            com.android.internal.util.AsyncChannel r0 = r11.mClientModeImplChannel
            if (r0 == 0) goto L_0x06b0
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            int r0 = r4.syncCallSECApi(r0, r12)
            r1 = r0
            goto L_0x06b0
        L_0x0454:
            r11.enforceProvideDiagnosticsPermission()
            com.samsung.android.server.wifi.WifiGuiderManagementService r0 = r11.mWifiGuiderManagementService
            if (r0 == 0) goto L_0x0465
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.samsung.android.server.wifi.WifiGuiderManagementService r0 = r11.mWifiGuiderManagementService
            r0.updateConditions(r3)
        L_0x0465:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0469:
            r11.enforceAccessPermission()
            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = r11.mSemQoSProfileShareProvider
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider r0 = r11.mSemQoSProfileShareProvider
            r0.test(r3)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0484:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0488:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            if (r3 == 0) goto L_0x04b2
            boolean r0 = r3.getBoolean(r8, r6)
            java.lang.String r5 = "package"
            java.lang.String r6 = "com.android.server.wifi.p2p"
            java.lang.String r5 = r3.getString(r5, r6)
            if (r0 == 0) goto L_0x04ad
            java.lang.String r6 = "SEC_COMMAND_ID_SET_WIFI_ENABLED_WITH_P2P - WiFi Enabled with p2p -> Stop Scan(Anqp), Stop Assoc, Stop WPS?"
            android.util.Log.d(r4, r6)
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            r4.setConcurrentEnabled(r0)
            r11.setWifiEnabled(r5, r0)
        L_0x04ad:
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            r4.sendCallSECApiAsync(r12, r2)
        L_0x04b2:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x04b6:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj     // Catch:{ Exception -> 0x04de }
            android.os.Bundle r0 = (android.os.Bundle) r0     // Catch:{ Exception -> 0x04de }
            r3 = r0
            com.android.server.wifi.WifiNative r0 = r11.mWifiNative     // Catch:{ Exception -> 0x04de }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x04de }
            r4.<init>()     // Catch:{ Exception -> 0x04de }
            java.lang.String r5 = "SET_MAXCLIENT "
            r4.append(r5)     // Catch:{ Exception -> 0x04de }
            java.lang.String r5 = "maxClient"
            int r5 = r3.getInt(r5)     // Catch:{ Exception -> 0x04de }
            r4.append(r5)     // Catch:{ Exception -> 0x04de }
            java.lang.String r4 = r4.toString()     // Catch:{ Exception -> 0x04de }
            r0.sendHostapdCommand(r4)     // Catch:{ Exception -> 0x04de }
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x04de:
            r0 = move-exception
            r0.printStackTrace()
            goto L_0x06b0
        L_0x04e4:
            r11.enforceProvideDiagnosticsPermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            com.samsung.android.server.wifi.WifiGuiderManagementService r0 = r11.mWifiGuiderManagementService
            if (r0 == 0) goto L_0x06b0
            int r0 = r0.registerAction(r3)
            r1 = r0
            goto L_0x06b0
        L_0x0501:
            r11.enforceProvideDiagnosticsPermission()
            java.lang.Object r0 = r12.obj
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x06b0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.String r0 = r3.getString(r9)
            if (r0 != 0) goto L_0x051d
            java.lang.String r0 = r11.getPackageName(r2)
        L_0x051d:
            java.lang.String r4 = "className"
            java.lang.String r4 = r3.getString(r4)
            com.samsung.android.server.wifi.WifiGuiderManagementService r5 = r11.mWifiGuiderManagementService
            if (r5 == 0) goto L_0x052b
            int r1 = r5.registerService(r0, r4)
        L_0x052b:
            goto L_0x06b0
        L_0x052d:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            int r0 = r0.callSECApi(r12)
            r1 = r0
            goto L_0x06b0
        L_0x0539:
            r11.enforceChangePermission()
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            if (r3 == 0) goto L_0x055e
            boolean r0 = r3.getBoolean(r8, r6)
            if (r0 == 0) goto L_0x0554
            java.lang.String r5 = "SEC_COMMAND_ID_SET_WIFI_SCAN_WITH_P2P - WiFi scan with p2p -> Stop Scan(Anqp), Stop Assoc, Stop WPS?"
            android.util.Log.d(r4, r5)
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            r4.sendCallSECApiAsync(r12, r2)
            goto L_0x055e
        L_0x0554:
            java.lang.String r5 = "SEC_COMMAND_ID_SET_WIFI_SCAN_WITH_P2P - Start Scan(Anqp), Start Assoc"
            android.util.Log.d(r4, r5)
            com.android.server.wifi.ClientModeImpl r4 = r11.mClientModeImpl
            r4.sendCallSECApiAsync(r12, r2)
        L_0x055e:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0562:
            r0 = 1
            r1 = r0
            goto L_0x06b0
        L_0x0566:
            com.android.server.wifi.IWCMonitor r0 = r11.mIWCMonitor
            if (r0 == 0) goto L_0x05ac
            int r5 = r12.what
            int r6 = r12.sendingUid
            int r7 = r12.arg1
            java.lang.Object r8 = r12.obj
            r0.sendMessage(r5, r6, r7, r8)
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r5 = "uid: "
            r0.append(r5)
            int r5 = r12.sendingUid
            r0.append(r5)
            java.lang.String r5 = "nid: "
            r0.append(r5)
            java.lang.String r5 = "netId"
            int r5 = r3.getInt(r5)
            r0.append(r5)
            java.lang.String r5 = "enabled: "
            r0.append(r5)
            java.lang.String r5 = "autoReconnect"
            int r5 = r3.getInt(r5)
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            android.util.Log.e(r4, r0)
        L_0x05ac:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            r0.sendCallSECApiAsync(r12, r2)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x05b8:
            r11.enforceNetworkSettingsPermission()
            java.lang.Object r0 = r12.obj
            boolean r0 = r0 instanceof android.os.Bundle
            if (r0 == 0) goto L_0x05e0
            java.lang.Object r0 = r12.obj
            r3 = r0
            android.os.Bundle r3 = (android.os.Bundle) r3
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "state_change_warning."
            r0.append(r4)
            java.lang.String r4 = "applabel"
            java.lang.String r4 = r3.getString(r4)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            r11.setWifiEnabled(r0, r7)
        L_0x05e0:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x05e4:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            r0.sendCallSECApiAsync(r12, r2)
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x05f0:
            r11.enforceChangePermission()
            com.android.server.wifi.ClientModeImpl r0 = r11.mClientModeImpl
            com.android.internal.util.AsyncChannel r4 = r11.mClientModeImplChannel
            int r0 = r0.syncCallSECApi(r4, r12)
            r1 = r0
            goto L_0x06b0
        L_0x05fe:
            int r0 = android.os.Binder.getCallingPid()
            int r5 = android.os.Binder.getCallingUid()
            boolean r0 = r11.checkAccessSecuredPermission(r0, r5)
            if (r0 != 0) goto L_0x0613
            java.lang.String r0 = "permission Denial"
            android.util.Log.e(r4, r0)
            goto L_0x06b0
        L_0x0613:
            int r0 = r12.arg1
            if (r0 < 0) goto L_0x062d
            int r0 = r12.arg1
            if (r0 != r7) goto L_0x061d
            r0 = r7
            goto L_0x061e
        L_0x061d:
            r0 = r6
        L_0x061e:
            r11.semIsTestMode = r0
            boolean r0 = r11.semIsTestMode
            if (r0 == 0) goto L_0x062d
            com.android.server.wifi.WifiNative r0 = r11.mWifiNative
            java.lang.String r4 = r0.getClientInterfaceName()
            r0.disableRandomMac(r4)
        L_0x062d:
            int r0 = r12.arg2
            if (r0 == r7) goto L_0x0637
            com.samsung.android.server.wifi.WifiEnableWarningPolicy r0 = r11.mWifiEnableWarningPolicy
            r0.testConfig(r6)
            goto L_0x063d
        L_0x0637:
            com.samsung.android.server.wifi.WifiEnableWarningPolicy r0 = r11.mWifiEnableWarningPolicy
            r0.testConfig(r7)
        L_0x063d:
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x0641:
            r11.enforceAccessPermission()
            com.android.server.wifi.WifiNative r0 = r11.mWifiNative     // Catch:{ Exception -> 0x064e }
            java.lang.String r4 = "READ_WHITELIST"
            r0.sendHostapdCommand(r4)     // Catch:{ Exception -> 0x064e }
            r0 = 0
            r1 = r0
            goto L_0x06b0
        L_0x064e:
            r0 = move-exception
            r0.printStackTrace()
            goto L_0x06b0
        L_0x0653:
            r11.enforceChangePermission()
            java.lang.Object r4 = r12.obj     // Catch:{ Exception -> 0x068c }
            android.os.Bundle r4 = (android.os.Bundle) r4     // Catch:{ Exception -> 0x068c }
            r3 = r4
            if (r3 == 0) goto L_0x068b
            com.android.server.wifi.WifiInjector r4 = com.android.server.wifi.WifiInjector.getInstance()     // Catch:{ Exception -> 0x068c }
            com.samsung.android.server.wifi.softap.SemWifiApClientInfo r4 = r4.getSemWifiApClientInfo()     // Catch:{ Exception -> 0x068c }
            r11.mSemWifiApClientInfo = r4     // Catch:{ Exception -> 0x068c }
            com.samsung.android.server.wifi.softap.SemWifiApClientInfo r4 = r11.mSemWifiApClientInfo     // Catch:{ Exception -> 0x068c }
            java.lang.String r5 = r3.getString(r0)     // Catch:{ Exception -> 0x068c }
            r4.setAccessPointDisassocSta(r5)     // Catch:{ Exception -> 0x068c }
            com.android.server.wifi.WifiNative r4 = r11.mWifiNative     // Catch:{ Exception -> 0x068c }
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x068c }
            r5.<init>()     // Catch:{ Exception -> 0x068c }
            java.lang.String r6 = "DISASSOCIATE "
            r5.append(r6)     // Catch:{ Exception -> 0x068c }
            java.lang.String r0 = r3.getString(r0)     // Catch:{ Exception -> 0x068c }
            r5.append(r0)     // Catch:{ Exception -> 0x068c }
            java.lang.String r0 = r5.toString()     // Catch:{ Exception -> 0x068c }
            r4.sendHostapdCommand(r0)     // Catch:{ Exception -> 0x068c }
            r1 = 0
        L_0x068b:
            goto L_0x06b0
        L_0x068c:
            r0 = move-exception
            r0.printStackTrace()
            goto L_0x06b0
        L_0x0691:
            r11.enforceAccessPermission()
            com.android.server.wifi.WifiNative r0 = r11.mWifiNative     // Catch:{ Exception -> 0x06ab }
            java.lang.String r4 = "NUM_STA"
            java.lang.String r0 = r0.sendHostapdCommand(r4)     // Catch:{ Exception -> 0x06ab }
            r1 = 0
            if (r0 == 0) goto L_0x06aa
            boolean r4 = r0.isEmpty()     // Catch:{ Exception -> 0x06ab }
            if (r4 != 0) goto L_0x06aa
            int r4 = java.lang.Integer.parseInt(r0)     // Catch:{ Exception -> 0x06ab }
            r1 = r4
        L_0x06aa:
            goto L_0x06b0
        L_0x06ab:
            r0 = move-exception
            r0.printStackTrace()
        L_0x06b0:
            r12.recycle()
        L_0x06b3:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceImpl.callSECApi(android.os.Message):int");
    }

    public String callSECStringApi(Message msg) {
        String retValue = null;
        if (msg != null) {
            int callingPid = Binder.getCallingPid();
            Log.d(TAG, "callSECStringApi msg.what=" + msg.what + ", callingPid:" + callingPid);
            int i = msg.what;
            if (i != 45) {
                if (!(i == 108 || i == 160)) {
                    if (i == 270) {
                        enforceAccessPermission();
                        this.mClientModeImpl.initializeWifiChipInfo();
                        retValue = WifiChipInfo.getInstance().getFirmwareVer(true);
                    } else if (i == 291) {
                        enforceAccessPermission();
                        AutoWifiController autoWifiController = this.mAutoWifiController;
                        if (autoWifiController != null) {
                            retValue = autoWifiController.getDebugString();
                        }
                    } else if (!(i == 300 || i == 317)) {
                        if (i == 403) {
                            enforceAccessPermission();
                            retValue = getQtables();
                        } else if (i == 406) {
                            enforceProvideDiagnosticsPermission();
                            retValue = this.mWifiGuiderPackageMonitor.dump();
                        } else if (i != 85) {
                            if (i == 86) {
                                enforceAccessPermission();
                                WifiGuiderManagementService wifiGuiderManagementService = this.mWifiGuiderManagementService;
                                if (wifiGuiderManagementService != null) {
                                    retValue = wifiGuiderManagementService.getVersion();
                                }
                            } else if (i == 223) {
                                enforceAccessPermission();
                                if (this.mClientModeImplChannel == null) {
                                    Log.e(TAG, "ClientModeImplHandler is not initialized");
                                } else {
                                    long ident = Binder.clearCallingIdentity();
                                    try {
                                        retValue = this.mClientModeImpl.syncCallSECStringApi(this.mClientModeImplChannel, msg);
                                    } finally {
                                        Binder.restoreCallingIdentity(ident);
                                    }
                                }
                            } else if (i != 224) {
                                switch (i) {
                                    case 274:
                                    case 275:
                                    case 276:
                                    case 277:
                                        enforceFactoryTestPermission();
                                        AsyncChannel asyncChannel = this.mClientModeImplChannel;
                                        if (asyncChannel != null) {
                                            retValue = this.mClientModeImpl.syncCallSECStringApi(asyncChannel, msg);
                                            break;
                                        }
                                        break;
                                    case DhcpPacket.MIN_PACKET_LENGTH_L2 /*278*/:
                                        if (checkAccessSecuredPermission(Binder.getCallingPid(), Binder.getCallingUid())) {
                                            AsyncChannel asyncChannel2 = this.mClientModeImplChannel;
                                            if (asyncChannel2 != null) {
                                                retValue = this.mClientModeImpl.syncCallSECStringApi(asyncChannel2, msg);
                                                break;
                                            }
                                        } else {
                                            Log.e(TAG, "permission Denial");
                                            break;
                                        }
                                        break;
                                    default:
                                        Log.d(TAG, "not implement yet. command id:" + msg.what);
                                        break;
                                }
                            } else {
                                enforceAccessPermission();
                                retValue = this.mHistoricalDumpLogs.toString();
                            }
                        }
                    }
                }
                enforceAccessPermission();
                AsyncChannel asyncChannel3 = this.mClientModeImplChannel;
                if (asyncChannel3 != null) {
                    retValue = this.mClientModeImpl.syncCallSECStringApi(asyncChannel3, msg);
                }
            } else {
                enforceAccessPermission();
                SemWifiProfileAndQoSProvider semWifiProfileAndQoSProvider = this.mSemQoSProfileShareProvider;
                if (semWifiProfileAndQoSProvider != null) {
                    retValue = semWifiProfileAndQoSProvider.dump();
                }
            }
            msg.recycle();
        }
        return retValue;
    }

    private int getWifiApChannel() {
        try {
            return Integer.parseInt(this.mWifiNative.sendHostapdCommand("GET_CHANNEL"));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<String> callSECListStringApi(Message msg) {
        List<String> retValue = new ArrayList<>();
        if (msg != null) {
            int callingPid = Binder.getCallingPid();
            Log.d(TAG, "callSECListStringApi msg.what=" + msg.what + ", callingPid:" + callingPid);
            if (msg.what != 91) {
                Log.d(TAG, "not implement yet. command id:" + msg.what);
            } else {
                enforceProvideDiagnosticsPermission();
                WifiGuiderManagementService wifiGuiderManagementService = this.mWifiGuiderManagementService;
                if (wifiGuiderManagementService != null) {
                    for (String result : wifiGuiderManagementService.getCachedDiagnosisResults()) {
                        retValue.add(result);
                    }
                }
            }
            msg.recycle();
        }
        return retValue;
    }

    private String getOAuthAgreements() {
        return Settings.Global.getString(this.mContext.getContentResolver(), "sem_wifi_allowed_oauth_provider");
    }

    private void updateOAuthAgreement(String oAuthProvider, boolean agree) {
        String oAuthProviders = getOAuthAgreements();
        if (agree) {
            if (!TextUtils.isEmpty(oAuthProviders)) {
                if (!oAuthProviders.contains("[" + oAuthProvider + "]")) {
                    ContentResolver contentResolver = this.mContext.getContentResolver();
                    Settings.Global.putString(contentResolver, "sem_wifi_allowed_oauth_provider", oAuthProviders + "[" + oAuthProvider + "]");
                    return;
                }
                return;
            }
            ContentResolver contentResolver2 = this.mContext.getContentResolver();
            Settings.Global.putString(contentResolver2, "sem_wifi_allowed_oauth_provider", "[" + oAuthProvider + "]");
        } else if (!TextUtils.isEmpty(oAuthProviders)) {
            ContentResolver contentResolver3 = this.mContext.getContentResolver();
            Settings.Global.putString(contentResolver3, "sem_wifi_allowed_oauth_provider", oAuthProviders.replace("[" + oAuthProvider + "]", ""));
        }
    }

    /* access modifiers changed from: private */
    public String getPackageName(int pid) {
        List<ActivityManager.RunningAppProcessInfo> processInfo;
        if (pid < 0 || (processInfo = this.mActivityManager.getRunningAppProcesses()) == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo item : processInfo) {
            if (item.pid == pid) {
                return item.processName;
            }
        }
        return null;
    }

    private boolean isFmcPackage() {
        String packagename = getPackageName(Binder.getCallingPid());
        Log.i(TAG, "isFmcPackage packageName : " + packagename);
        if (packagename.equals("com.sec.wevoip.wes_v3") || packagename.equals("com.amc.ui") || packagename.equals("com.amc.util")) {
            return true;
        }
        return false;
    }

    private String getStationInfo(String mac) {
        try {
            WifiNative wifiNative = this.mWifiNative;
            return wifiNative.sendHostapdCommand("GET_STA_INFO " + mac);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkAndShowWifiSharingLiteDialog(String packageName) {
        int value = Settings.System.getInt(this.mContext.getContentResolver(), "wifi_sharing_lite_popup_status", 0);
        if (!supportWifiSharingLite() || !isWifiSharingEnabled() || !isMobileApOn() || value != 0) {
            return false;
        }
        Log.d(TAG, "WIFI sharing lite popup");
        SemWifiFrameworkUxUtils.showWarningDialog(this.mContext, 5, new String[]{packageName});
        return true;
    }

    private boolean checkAndShowFirmwareChangeDialog(String packageName) {
        if (isWifiSharingEnabled() || !isMobileApOn()) {
            return false;
        }
        Log.d(TAG, "Wifi is not allowed because MHS is enabled");
        SemWifiFrameworkUxUtils.showWarningDialog(this.mContext, 4, new String[]{packageName});
        return true;
    }

    public void setImsCallEstablished(boolean isEstablished) {
        enforceChangePermission();
        this.mClientModeImpl.setImsCallEstablished(isEstablished);
    }

    public void setAutoConnectCarrierApEnabled(boolean enabled) {
        enforceChangePermission();
        this.mClientModeImpl.setAutoConnectCarrierApEnabled(enabled);
    }

    public boolean setRvfMode(int value) {
        this.mWifiApConfigStore.setRvfMode(value);
        return value == getRvfMode();
    }

    public int getRvfMode() {
        return this.mWifiApConfigStore.getRvfMode();
    }

    public List<String> getWifiApStaListDetail() {
        try {
            return this.mWifiInjector.getSemWifiApClientInfo().getWifiApStaListDetail();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getWifiApStaList() {
        enforceChangePermission();
        try {
            return this.mWifiNative.sendHostapdCommand("GET_STA_LIST");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean supportWifiSharing() {
        boolean rbool = this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing();
        Log.d(TAG, "supportWifiSharing() " + rbool);
        return rbool;
    }

    public boolean isWifiSharingEnabled() {
        try {
            if (!supportWifiSharing()) {
                Log.i(TAG, "MOBILEAP_WIFI_CONCURRENCY feature is disabled");
                return false;
            } else if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing") == 1) {
                Log.i(TAG, "Wi-Fi Sharing has been enabled");
                return true;
            } else {
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_wifi_sharing") == 0) {
                    Log.i(TAG, "Wi-Fi Sharing has been disabled");
                    return false;
                }
                return false;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.i(TAG, "Wi-Fi Sharing settings has not been accessed");
        }
    }

    public boolean isRegionFor5G() {
        String str = this.mCSCRegion;
        if (str == null) {
            return false;
        }
        if (str.equals("NA") || this.mCSCRegion.equals("KOR")) {
            return true;
        }
        return false;
    }

    public boolean isRegionFor5GCountry() {
        int testapilevel;
        int first_api_level = SystemProperties.getInt("ro.product.first_api_level", -1);
        if (MHSDBG && (testapilevel = SystemProperties.getInt("mhs.first_api_level", -1)) != -1) {
            first_api_level = testapilevel;
        }
        if (first_api_level >= 28) {
            if ("NA".equals(this.mCSCRegion) || "KOR".equals(this.mCSCRegion) || "EUR".equals(this.mCSCRegion) || "CHN".equals(this.mCSCRegion)) {
                return true;
            }
            if (!"extend".equals("default")) {
                return false;
            }
            if ("CIS".equals(this.mCSCRegion) || "SEA".equals(this.mCSCRegion) || "SWA".equals(this.mCSCRegion) || "LA".equals(this.mCSCRegion) || "AE".equals(this.mCountryIso) || "SA".equals(this.mCountryIso) || "ZA".equals(this.mCountryIso)) {
                return true;
            }
            return false;
        } else if ("NA".equals(this.mCSCRegion) || "KOR".equals(this.mCSCRegion)) {
            return true;
        } else {
            for (String region : "default".split(",")) {
                if (region.equals(this.mCSCRegion)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void addMHSChannelHistoryLog(String log) {
        StringBuffer value = new StringBuffer();
        Log.i(TAG, log + " mhsch: " + mMHSChannelHistoryLogs.size());
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (mMHSChannelHistoryLogs.size() > 100) {
            mMHSChannelHistoryLogs.remove(0);
        }
        mMHSChannelHistoryLogs.add(value.toString());
    }

    public Messenger getIWCMessenger() {
        if (this.mIWCMessenger == null) {
            IWCMonitor iWCMonitor = this.mIWCMonitor;
            if (iWCMonitor != null) {
                this.mIWCMessenger = iWCMonitor.getIWCMessenger();
            } else {
                Log.d(TAG, "Could not get IWC Messenger");
            }
        }
        return this.mIWCMessenger;
    }

    private String getQtables() {
        IWCMonitor iWCMonitor = this.mIWCMonitor;
        if (iWCMonitor != null) {
            return iWCMonitor.getQtables();
        }
        return null;
    }

    private void setQtables(String qt) {
        IWCMonitor iWCMonitor = this.mIWCMonitor;
        if (iWCMonitor != null) {
            iWCMonitor.setQtables(qt, true);
        }
    }

    private void startFromBeginning(Bundle args) {
        Bundle b = args.deepCopy();
        IWCMonitor iWCMonitor = this.mIWCMonitor;
        if (iWCMonitor != null) {
            iWCMonitor.sendMessage(IWCMonitor.START_FROM_BEGINNING, b);
        }
    }

    private void restoreUserPreference(int type, int value) {
        IWCMonitor iWCMonitor = this.mIWCMonitor;
        if (iWCMonitor != null) {
            iWCMonitor.sendMessage(IWCMonitor.RESTORE_USER_PREFERENCE, type, value);
        }
    }

    private void bootUp() {
        this.semIsShutdown = false;
    }

    private void shutdown() {
        Log.d(TAG, "Shutdown is called");
        this.semIsShutdown = true;
        if (CSC_SEND_DHCP_RELEASE) {
            sendDhcpRelease();
        }
        this.mWifiController.sendMessage(155673);
        this.mClientModeImpl.setShutdown();
    }

    private void checkAndStartAutoWifi() {
        if (this.mClientModeImpl.isSupportedGeofence() && !this.mClientModeImpl.isWifiOnly()) {
            HandlerThread autoWifiThread = new HandlerThread("AutoWifi");
            autoWifiThread.start();
            this.mAutoWifiController = new AutoWifiController(this.mContext, autoWifiThread.getLooper(), new AutoWifiController.AutoWifiAdapter() {
                public boolean setWifiEnabled(String packageName, boolean enabled) {
                    return WifiServiceImpl.this.setWifiEnabled(packageName, enabled);
                }

                public void notifyScanModeChanged() {
                    WifiServiceImpl.this.mWifiController.sendMessage(155655);
                }

                public boolean isWifiSharingEnabled() {
                    return WifiServiceImpl.this.isWifiSharingEnabled();
                }

                public int getWifiApEnabledState() {
                    return WifiServiceImpl.this.getWifiApEnabledState();
                }

                public int getWifiEnabledState() {
                    return WifiServiceImpl.this.getWifiEnabledState();
                }

                public NetworkInfo getNetworkInfo() {
                    return WifiServiceImpl.this.getNetworkInfo();
                }

                public int addOrUpdateNetwork(WifiConfiguration config) {
                    if (WifiServiceImpl.this.mClientModeImplChannel == null) {
                        return -1;
                    }
                    return WifiServiceImpl.this.mClientModeImpl.syncAddOrUpdateNetwork(WifiServiceImpl.this.mClientModeImplChannel, config);
                }

                public boolean startScan(String packageName) {
                    return WifiServiceImpl.this.mScanRequestProxy.startScan(Binder.getCallingUid(), packageName);
                }

                public WifiInfo getWifiInfo() {
                    return WifiServiceImpl.this.mClientModeImpl.getWifiInfo();
                }

                public WifiConfiguration getSpecificNetwork(int networkId) {
                    if (WifiServiceImpl.this.mClientModeImplChannel != null) {
                        return WifiServiceImpl.this.mClientModeImpl.syncGetSpecificNetwork(WifiServiceImpl.this.mClientModeImplChannel, networkId);
                    }
                    return null;
                }

                public List<WifiConfiguration> getConfiguredNetworks() {
                    if (WifiServiceImpl.this.mClientModeImplChannel == null) {
                        return null;
                    }
                    return WifiServiceImpl.this.mClientModeImpl.syncGetConfiguredNetworks(1000, WifiServiceImpl.this.mClientModeImplChannel, 1010);
                }

                public List<ScanResult> getScanResults() {
                    List<ScanResult> scanResults = new ArrayList<>();
                    scanResults.addAll(WifiServiceImpl.this.mScanRequestProxy.getScanResults());
                    return scanResults;
                }

                public boolean isUnstableAp(String bssid) {
                    return WifiServiceImpl.this.mClientModeImpl.isUnstableAp(bssid);
                }

                public boolean isWifiToggleEnabled() {
                    return WifiServiceImpl.this.mSettingsStore.isWifiToggleEnabled();
                }

                public int getWifiSavedState() {
                    return WifiServiceImpl.this.mSettingsStore.getWifiSavedState();
                }

                public void setWifiSavedState(int state) {
                    WifiServiceImpl.this.mSettingsStore.setWifiSavedState(state);
                }

                public void setScanAlwaysAvailable(boolean enabled) {
                    WifiServiceImpl.this.mSettingsStore.setScanAlwaysAvailable(enabled);
                }

                public void obtainScanAlwaysAvailablePolicy(boolean enabled) {
                    WifiServiceImpl.this.mSettingsStore.obtainScanAlwaysAvailablePolicy(enabled);
                }

                public void requestGeofenceEnabled(boolean enabled, String packageName) {
                    WifiServiceImpl.this.mClientModeImpl.requestGeofenceState(enabled, packageName);
                }

                public int getCurrentGeofenceState() {
                    return WifiServiceImpl.this.mClientModeImpl.getCurrentGeofenceState();
                }

                public List<String> getGeofenceEnterKeys() {
                    return WifiServiceImpl.this.mClientModeImpl.getGeofenceEnterKeys();
                }

                public int getCellCount(String configKey) {
                    return WifiServiceImpl.this.mClientModeImpl.getGeofenceCellCount(configKey);
                }

                public WifiScanner getWifiScanner() {
                    return WifiServiceImpl.this.mWifiInjector.getWifiScanner();
                }
            });
            this.mAutoWifiController.checkAndStart();
            this.mClientModeImpl.setWifiGeofenceListener(this.mAutoWifiController.getGeofenceListener());
        }
    }

    /* access modifiers changed from: private */
    public TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    public void sendDhcpRelease() {
        Log.d(TAG, "sendMessage - ClientModeImpl.CMD_SEND_DHCP_RELEASE");
        this.mClientModeImpl.sendMessage(ClientModeImpl.CMD_SEND_DHCP_RELEASE);
    }

    private void addHistoricalDumpLog(String log) {
        if (this.mHistoricalDumpLogs.size() > 20) {
            this.mHistoricalDumpLogs.remove(0);
        }
        this.mHistoricalDumpLogs.add(log);
    }

    private void setWifiEnabledTriggered(boolean enable, String packageName, boolean triggeredByUser) {
        insertLogForWifiEnabled(enable, packageName);
        AutoWifiController autoWifiController = this.mAutoWifiController;
        if (autoWifiController != null) {
            autoWifiController.setWifiEnabledTriggered(enable, packageName, triggeredByUser);
        }
    }

    public void insertLogForWifiEnabled(boolean isEnabled, String packageName) {
        String str;
        NetworkInfo info;
        boolean isConnected = false;
        if (!isEnabled && (info = getNetworkInfo()) != null && info.isConnected()) {
            isConnected = true;
        }
        boolean z = false;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 0) == 1) {
            z = true;
        }
        boolean isSnsEnabled = z;
        StringBuilder sb = new StringBuilder();
        String str2 = "1";
        sb.append(isEnabled ? str2 : "0");
        sb.append(" ");
        sb.append(packageName == null ? "x" : packageName);
        sb.append(" ");
        if (isConnected) {
            str = str2;
        } else {
            str = "0";
        }
        sb.append(str);
        sb.append(" ");
        if (!isSnsEnabled) {
            str2 = "0";
        }
        sb.append(str2);
        String data = sb.toString();
        Message msg = Message.obtain();
        msg.what = 77;
        msg.obj = WifiBigDataLogManager.getBigDataBundle(WifiBigDataLogManager.FEATURE_ON_OFF, data);
        this.mClientModeImpl.callSECApi(msg);
    }

    private final class SemWifiApSmartCallbackImpl implements WifiManager.SemWifiApSmartCallback {
        private SemWifiApSmartCallbackImpl() {
        }

        public synchronized void onStateChanged(int state, String mhsMac) {
            int unused = WifiServiceImpl.this.mSemWifiApSmartState = state;
            String unused2 = WifiServiceImpl.this.mSemWifiApSmartMhsMac = mhsMac;
            Iterator<ISemWifiApSmartCallback> iterator = WifiServiceImpl.this.mRegisteredSemWifiApSmartCallbacks.values().iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().onStateChanged(state, mhsMac);
                } catch (RemoteException e) {
                    Log.e(WifiServiceImpl.TAG, "onStateChanged: remote exception -- " + e);
                    iterator.remove();
                }
            }
        }
    }

    public void registerSemWifiApSmartCallback(final IBinder binder, ISemWifiApSmartCallback callback, final int callbackIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("registerSemWifiApSmartCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
            }
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    public void binderDied() {
                        binder.unlinkToDeath(this, 0);
                        WifiServiceImpl.this.mAsyncChannelExternalClientHandler.post(new Runnable(callbackIdentifier) {
                            private final /* synthetic */ int f$1;

                            {
                                this.f$1 = r2;
                            }

                            public final void run() {
                                WifiServiceImpl.C047716.this.lambda$binderDied$0$WifiServiceImpl$16(this.f$1);
                            }
                        });
                    }

                    public /* synthetic */ void lambda$binderDied$0$WifiServiceImpl$16(int callbackIdentifier) {
                        WifiServiceImpl.this.mRegisteredSemWifiApSmartCallbacks.remove(Integer.valueOf(callbackIdentifier));
                    }
                }, 0);
                this.mAsyncChannelExternalClientHandler.post(new Runnable(callbackIdentifier, callback) {
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ ISemWifiApSmartCallback f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        WifiServiceImpl.this.lambda$registerSemWifiApSmartCallback$32$WifiServiceImpl(this.f$1, this.f$2);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "Error on linkToDeath - " + e);
            }
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public /* synthetic */ void lambda$registerSemWifiApSmartCallback$32$WifiServiceImpl(int callbackIdentifier, ISemWifiApSmartCallback callback) {
        this.mRegisteredSemWifiApSmartCallbacks.put(Integer.valueOf(callbackIdentifier), callback);
        if (this.mRegisteredSemWifiApSmartCallbacks.size() > 20) {
            Log.wtf(TAG, "Too many SemWifiApSmartCallback AP callbacks: " + this.mRegisteredSemWifiApSmartCallbacks.size());
        } else if (this.mRegisteredSemWifiApSmartCallbacks.size() > 10) {
            Log.w(TAG, "Too many SemWifiApSmartCallback AP callbacks: " + this.mRegisteredSemWifiApSmartCallbacks.size());
        }
        try {
            callback.onStateChanged(this.mSemWifiApSmartState, this.mSemWifiApSmartMhsMac);
        } catch (RemoteException e) {
            Log.e(TAG, "registerSemWifiApSmartCallback: remote exception -- " + e);
        }
    }

    public void unregisterSemWifiApSmartCallback(int callbackIdentifier) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterSemWifiApSmartCallback uid=%").mo2069c((long) Binder.getCallingUid()).flush();
        }
        this.mAsyncChannelExternalClientHandler.post(new Runnable(callbackIdentifier) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiServiceImpl.this.lambda$unregisterSemWifiApSmartCallback$33$WifiServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$unregisterSemWifiApSmartCallback$33$WifiServiceImpl(int callbackIdentifier) {
        this.mRegisteredSemWifiApSmartCallbacks.remove(Integer.valueOf(callbackIdentifier));
    }

    public List<SemWifiApBleScanResult> semGetWifiApBleScanDetail() {
        long ident = Binder.clearCallingIdentity();
        List<SemWifiApBleScanResult> res = null;
        try {
            if (this.mSemWifiApSmartClient != null) {
                res = this.mSemWifiApSmartClient.getWifiApBleScanResults();
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean semWifiApBleClientRole(boolean enable) {
        long ident = Binder.clearCallingIdentity();
        boolean res = false;
        try {
            if (this.mSemWifiApSmartClient != null) {
                res = this.mSemWifiApSmartClient.setWifiApSmartClient(enable);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean semWifiApBleMhsRole(boolean enable) {
        boolean res = false;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartMHS != null) {
                res = this.mSemWifiApSmartMHS.setWifiApSmartMHS(enable);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean connectToSmartMHS(String addr, int type, int mhidden, int mSecurity, String mhs_mac, String mUserName, int ver) {
        boolean res = false;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartGattClient != null) {
                res = this.mSemWifiApSmartGattClient.connectToSmartMHS(addr, type, mhidden, mSecurity, mhs_mac, mUserName, ver);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getSmartApConnectedStatus(String mhs_mac) {
        int res = -1;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartGattClient != null) {
                res = this.mSemWifiApSmartGattClient.getSmartApConnectedStatus(mhs_mac);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getSmartApConnectedStatusFromScanResult(String clientmac) {
        int res = -1;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartGattClient != null) {
                res = this.mSemWifiApSmartGattClient.getSmartApConnectedStatusFromScanResult(clientmac);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isWifiApWpa3Supported() {
        Binder.restoreCallingIdentity(Binder.clearCallingIdentity());
        return false;
    }

    public List<SemWifiApBleScanResult> semGetWifiApBleD2DScanDetail() {
        List<SemWifiApBleScanResult> res = null;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartD2DMHS != null) {
                res = this.mSemWifiApSmartD2DMHS.getWifiApBleD2DScanResults();
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean semWifiApBleD2DClientRole(boolean enable) {
        boolean res = true;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartD2DClient != null) {
                res = this.mSemWifiApSmartD2DClient.semWifiApBleD2DClientRole(enable);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean semWifiApBleD2DMhsRole(boolean enable) {
        boolean res = true;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartD2DMHS != null) {
                res = this.mSemWifiApSmartD2DMHS.semWifiApBleD2DMhsRole(enable);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean connectToSmartD2DClient(String bleaddr, String client_mac, ISemWifiApSmartCallback callback) {
        boolean res = true;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartD2DGattClient != null) {
                res = this.mSemWifiApSmartD2DGattClient.connectToSmartD2DClient(bleaddr, client_mac, callback);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getSmartD2DClientConnectedStatus(String mac) {
        int res = -1;
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSemWifiApSmartD2DGattClient != null) {
                res = this.mSemWifiApSmartD2DGattClient.getSmartD2DClientConnectedStatus(mac);
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public String semLoadMonitorModeFirmware(boolean enable) {
        Log.d(TAG, "semLoadMonitorModeFirmware : enable = " + enable);
        if (this.mWlanSnifferController == null) {
            this.mWlanSnifferController = new WlanSnifferController(this.mContext);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mWlanSnifferController.semLoadMonitorModeFirmware(enable);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public String semCheckMonitorMode() {
        Log.d(TAG, "semCheckMonitorMode");
        if (this.mWlanSnifferController == null) {
            this.mWlanSnifferController = new WlanSnifferController(this.mContext);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mWlanSnifferController.semCheckMonitorMode();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public String semStartMonitorMode(int ch, int bw) {
        Log.d(TAG, "semStartMonitorMode : ch = " + ch + " : bw = " + bw);
        if (this.mWlanSnifferController == null) {
            this.mWlanSnifferController = new WlanSnifferController(this.mContext);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mWlanSnifferController.semStartMonitorMode(ch, bw);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public String semStartAirlogs(boolean enable, boolean compressiveMode) {
        Log.i(TAG, "semStartAirlogs : enable = " + enable + " : compressiveMode = " + compressiveMode);
        if (this.mWlanSnifferController == null) {
            this.mWlanSnifferController = new WlanSnifferController(this.mContext);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mWlanSnifferController.semStartAirlogs(enable, compressiveMode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public String semStopMonitorMode() {
        Log.d(TAG, "semStopMonitorMode");
        if (this.mWlanSnifferController == null) {
            this.mWlanSnifferController = new WlanSnifferController(this.mContext);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mWlanSnifferController.semStopMonitorMode();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public Map<String, Map<String, Integer>> getQoSScores(List<String> bssids) {
        try {
            enforceNetworkSettingsPermission();
            if (this.mSemQoSProfileShareProvider != null) {
                return this.mSemQoSProfileShareProvider.getQoSScores(bssids);
            }
            return null;
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getQoSScores not allowed" + e);
            return null;
        }
    }

    public void requestPasswordToDevice(String bssid, ISharedPasswordCallback callback) {
        try {
            Log.i(TAG, "requestPasswordToDevice");
            enforceNetworkSettingsPermission();
            if (this.mSemQoSProfileShareProvider != null) {
                this.mSemQoSProfileShareProvider.requestPassword(true, bssid, callback);
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getQoSScores not allowed" + e);
        }
    }

    public void requestPasswordToDevicePost(boolean enable) {
        try {
            Log.i(TAG, "requestPasswordToDevicePost, enable : " + enable);
            enforceNetworkSettingsPermission();
            if (this.mSemQoSProfileShareProvider != null) {
                this.mSemQoSProfileShareProvider.requestPasswordPost(enable);
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getQoSScores not allowed" + e);
        }
    }

    public double[] getLatitudeLongitude(WifiConfiguration config) {
        String packageName = this.mContext.getOpPackageName();
        int uid = Binder.getCallingUid();
        try {
            enforceLocationPermission(packageName, uid);
            return this.mClientModeImpl.getLatitudeLongitude(config);
        } catch (SecurityException e) {
            Slog.e(TAG, "Permission violation - getLatitudeLongitude not allowed for uid=" + uid + ", packageName=" + packageName + ", reason=" + e);
            return null;
        }
    }

    public void sendQCResultToWCM(Message msg) {
        this.mWifiConnectivityMonitor.sendQCResultToWCM(msg);
    }

    public void setValidationCheckStart() {
        this.mWifiConnectivityMonitor.setValidationCheckStart();
    }

    public void sendValidationCheckModeResult(boolean valid) {
        this.mWifiConnectivityMonitor.sendValidationCheckModeResult(valid);
    }
}
