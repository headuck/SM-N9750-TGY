package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.hardware.SystemSensorManager;
import android.net.IpMemoryStore;
import android.net.NetworkCapabilities;
import android.net.NetworkScoreManager;
import android.net.wifi.IWifiScanner;
import android.net.wifi.IWificond;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkScoreCache;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.PowerProfile;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.NetworkRequestStoreData;
import com.android.server.wifi.NetworkSuggestionStoreData;
import com.android.server.wifi.ScanOnlyModeManager;
import com.android.server.wifi.aware.WifiAwareMetrics;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.hotspot2.PasspointObjectFactory;
import com.android.server.wifi.p2p.SupplicantP2pIfaceHal;
import com.android.server.wifi.p2p.WifiP2pMetrics;
import com.android.server.wifi.p2p.WifiP2pMonitor;
import com.android.server.wifi.p2p.WifiP2pNative;
import com.android.server.wifi.rtt.RttMetrics;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.samsung.android.server.wifi.WifiB2BConfigurationPolicy;
import com.samsung.android.server.wifi.WifiLowLatency;
import com.samsung.android.server.wifi.WifiPickerController;
import com.samsung.android.server.wifi.dqa.SemWifiIssueDetector;
import com.samsung.android.server.wifi.share.SemWifiProfileAndQoSProvider;
import com.samsung.android.server.wifi.softap.SemSoftapConfig;
import com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver;
import com.samsung.android.server.wifi.softap.SemWifiApChipInfo;
import com.samsung.android.server.wifi.softap.SemWifiApClientInfo;
import com.samsung.android.server.wifi.softap.SemWifiApMonitor;
import com.samsung.android.server.wifi.softap.SemWifiApPowerSaveImpl;
import com.samsung.android.server.wifi.softap.SemWifiApTimeOutImpl;
import com.samsung.android.server.wifi.softap.SemWifiApTrafficPoller;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartBleScanner;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DGattClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartD2DMHS;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattClient;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartGattServer;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartMHS;
import java.util.Random;

public class WifiInjector {
    private static final String BOOT_DEFAULT_WIFI_COUNTRY_CODE = "ro.boot.wificountrycode";
    private static final String WIFICOND_SERVICE_NAME = "wificond";
    static WifiInjector sWifiInjector = null;
    private final ActiveModeWarden mActiveModeWarden;
    private final BackupManagerProxy mBackupManagerProxy = new BackupManagerProxy();
    private final IBatteryStats mBatteryStats;
    private final BuildProperties mBuildProperties = new SystemBuildProperties();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final CarrierNetworkEvaluator mCarrierNetworkEvaluator;
    private CarrierNetworkNotifier mCarrierNetworkNotifier;
    private final CellularLinkLayerStatsCollector mCellularLinkLayerStatsCollector;
    /* access modifiers changed from: private */
    public final ClientModeImpl mClientModeImpl;
    private final Clock mClock = new Clock();
    private final LocalLog mConnectivityLocalLog;
    private final Context mContext;
    private final WifiCountryCode mCountryCode;
    private final DppManager mDppManager;
    private final DppMetrics mDppMetrics;
    private final FrameworkFacade mFrameworkFacade = new FrameworkFacade();
    private HalDeviceManager mHalDeviceManager;
    private final HostapdHal mHostapdHal;
    private final IpMemoryStore mIpMemoryStore;
    private final SemWifiIssueDetector mIssueDetector;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private final LinkProbeManager mLinkProbeManager;
    private final WifiLockManager mLockManager;
    private final NetworkScoreManager mNetworkScoreManager;
    private final NetworkSuggestionEvaluator mNetworkSuggestionEvaluator;
    private final INetworkManagementService mNwManagementService;
    private OpenNetworkNotifier mOpenNetworkNotifier;
    private final PasspointManager mPasspointManager;
    private final PasspointNetworkEvaluator mPasspointNetworkEvaluator;
    private final HandlerThread mPasspointProvisionerHandlerThread;
    private final WifiPickerController mPickerController;
    private final PropertyService mPropertyService = new SystemPropertyService();
    private HandlerThread mRttHandlerThread;
    private final SarManager mSarManager;
    private final SavedNetworkEvaluator mSavedNetworkEvaluator;
    private final ScanRequestProxy mScanRequestProxy;
    private final ScoredNetworkEvaluator mScoredNetworkEvaluator;
    private final ScoringParams mScoringParams;
    private final SelfRecovery mSelfRecovery;
    private SemWifiProfileAndQoSProvider mSemQoSProfileShareProvider;
    private final SemSoftapConfig mSemSoftapConfig;
    private final SemWifiApBroadcastReceiver mSemWifiApBroadcastReceiver;
    private final SemWifiApChipInfo mSemWifiApChipInfo;
    private final SemWifiApClientInfo mSemWifiApClientInfo;
    private final SemWifiApMonitor mSemWifiApMonitor;
    private final SemWifiApPowerSaveImpl mSemWifiApPowerSaveImpl;
    private SemWifiApSmartBleScanner mSemWifiApSmartBleScanner;
    private SemWifiApSmartClient mSemWifiApSmartClient;
    private SemWifiApSmartD2DClient mSemWifiApSmartD2DClient;
    private SemWifiApSmartD2DGattClient mSemWifiApSmartD2DGattClient;
    private SemWifiApSmartD2DMHS mSemWifiApSmartD2DMHS;
    private SemWifiApSmartGattClient mSemWifiApSmartGattClient;
    private SemWifiApSmartGattServer mSemWifiApSmartGattServer;
    private SemWifiApSmartMHS mSemWifiApSmartMHS;
    private final SemWifiApTimeOutImpl mSemWifiApTimeOutImpl;
    private SemWifiApTrafficPoller mSemWifiApTrafficPoller;
    private final WifiSettingsStore mSettingsStore;
    private final SIMAccessor mSimAccessor;
    private final SupplicantP2pIfaceHal mSupplicantP2pIfaceHal;
    private final SupplicantStaIfaceHal mSupplicantStaIfaceHal;
    private final WakeupController mWakeupController;
    private WifiApConfigStore mWifiApConfigStore;
    private HandlerThread mWifiAwareHandlerThread;
    private final WifiB2BConfigurationPolicy mWifiB2bConfigPolicy;
    private final WifiBackupRestore mWifiBackupRestore;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiConfigStore mWifiConfigStore;
    private final WifiConnectivityHelper mWifiConnectivityHelper;
    private final WifiController mWifiController;
    private final HandlerThread mWifiCoreHandlerThread;
    private final WifiDataStall mWifiDataStall;
    private final BaseWifiDiagnostics mWifiDiagnostics;
    private final HandlerThread mWifiDiagnosticsHandlerThread;
    private final WifiGeofenceManager mWifiGeofenceManager;
    private final WifiKeyStore mWifiKeyStore;
    private WifiLastResortWatchdog mWifiLastResortWatchdog;
    private final WifiLowLatency mWifiLowLatency;
    private final WifiMetrics mWifiMetrics;
    private final WifiMonitor mWifiMonitor;
    private final WifiMulticastLockManager mWifiMulticastLockManager;
    private final WifiNative mWifiNative;
    private final WifiNetworkScoreCache mWifiNetworkScoreCache;
    private final WifiNetworkSelector mWifiNetworkSelector;
    private final WifiNetworkSuggestionsManager mWifiNetworkSuggestionsManager;
    private WifiNotificationController mWifiNotificationController;
    private final WifiP2pMetrics mWifiP2pMetrics;
    private final WifiP2pMonitor mWifiP2pMonitor;
    private final WifiP2pNative mWifiP2pNative;
    private final HandlerThread mWifiP2pServiceHandlerThread;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private WifiScanner mWifiScanner;
    private final WifiScoreCard mWifiScoreCard;
    private final HandlerThread mWifiServiceHandlerThread;
    private final WifiStateTracker mWifiStateTracker;
    private final WifiTrafficPoller mWifiTrafficPoller;
    private final WifiVendorHal mWifiVendorHal;
    private final WificondControl mWificondControl;

    public WifiInjector(Context context) {
        Context context2 = context;
        if (context2 == null) {
            throw new IllegalStateException("WifiInjector should not be initialized with a null Context.");
        } else if (sWifiInjector == null) {
            sWifiInjector = this;
            this.mContext = context2;
            this.mWifiB2bConfigPolicy = WifiB2BConfigurationPolicy.getInstance(this.mContext);
            this.mPickerController = new WifiPickerController(this.mContext, this.mFrameworkFacade);
            this.mWifiScoreCard = new WifiScoreCard(this.mClock, Settings.Secure.getString(this.mContext.getContentResolver(), "android_id"));
            this.mSettingsStore = new WifiSettingsStore(this.mContext);
            this.mWifiPermissionsWrapper = new WifiPermissionsWrapper(this.mContext);
            this.mNetworkScoreManager = (NetworkScoreManager) this.mContext.getSystemService(NetworkScoreManager.class);
            this.mWifiNetworkScoreCache = new WifiNetworkScoreCache(this.mContext);
            this.mNetworkScoreManager.registerNetworkScoreCache(1, this.mWifiNetworkScoreCache, 0);
            WifiPermissionsWrapper wifiPermissionsWrapper = this.mWifiPermissionsWrapper;
            Context context3 = this.mContext;
            this.mWifiPermissionsUtil = new WifiPermissionsUtil(wifiPermissionsWrapper, context3, UserManager.get(context3), this);
            this.mWifiBackupRestore = new WifiBackupRestore(this.mWifiPermissionsUtil);
            this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mFrameworkFacade.getService("batterystats"));
            this.mWifiStateTracker = new WifiStateTracker(this.mBatteryStats);
            this.mWifiServiceHandlerThread = new HandlerThread("WifiService");
            this.mWifiServiceHandlerThread.start();
            this.mWifiCoreHandlerThread = new HandlerThread("ClientModeImpl");
            this.mWifiCoreHandlerThread.start();
            this.mWifiP2pServiceHandlerThread = new HandlerThread("WifiP2pService");
            this.mWifiP2pServiceHandlerThread.start();
            this.mPasspointProvisionerHandlerThread = new HandlerThread("PasspointProvisionerHandlerThread");
            this.mPasspointProvisionerHandlerThread.start();
            this.mWifiDiagnosticsHandlerThread = new HandlerThread("WifiDiagnostics");
            this.mWifiDiagnosticsHandlerThread.start();
            Looper clientModeImplLooper = this.mWifiCoreHandlerThread.getLooper();
            this.mCarrierNetworkConfig = new CarrierNetworkConfig(this.mContext, clientModeImplLooper, this.mFrameworkFacade);
            WifiAwareMetrics awareMetrics = new WifiAwareMetrics(this.mClock);
            RttMetrics rttMetrics = new RttMetrics(this.mClock);
            this.mWifiP2pMetrics = new WifiP2pMetrics(this.mClock);
            this.mDppMetrics = new DppMetrics();
            this.mCellularLinkLayerStatsCollector = new CellularLinkLayerStatsCollector(this.mContext);
            this.mWifiMetrics = new WifiMetrics(this.mContext, this.mFrameworkFacade, this.mClock, clientModeImplLooper, awareMetrics, rttMetrics, new WifiPowerMetrics(), this.mWifiP2pMetrics, this.mDppMetrics, this.mCellularLinkLayerStatsCollector);
            this.mWifiMonitor = new WifiMonitor(this);
            this.mHalDeviceManager = new HalDeviceManager(this.mClock);
            this.mWifiVendorHal = new WifiVendorHal(this.mHalDeviceManager, this.mWifiCoreHandlerThread.getLooper());
            this.mSupplicantStaIfaceHal = new SupplicantStaIfaceHal(this.mContext, this.mWifiMonitor, this.mPropertyService, clientModeImplLooper);
            this.mSemWifiApMonitor = new SemWifiApMonitor(this);
            this.mSemWifiApClientInfo = new SemWifiApClientInfo(this.mContext, clientModeImplLooper);
            this.mSemWifiApTimeOutImpl = new SemWifiApTimeOutImpl(this.mContext, this.mFrameworkFacade);
            this.mSemWifiApPowerSaveImpl = new SemWifiApPowerSaveImpl(this.mContext);
            this.mSemSoftapConfig = new SemSoftapConfig(this.mContext);
            this.mHostapdHal = new HostapdHal(this.mContext, clientModeImplLooper);
            this.mSemWifiApBroadcastReceiver = new SemWifiApBroadcastReceiver(this.mContext);
            this.mSemWifiApBroadcastReceiver.startTracking();
            this.mWificondControl = new WificondControl(this, this.mWifiMonitor, this.mCarrierNetworkConfig, (AlarmManager) this.mContext.getSystemService("alarm"), clientModeImplLooper, this.mClock);
            this.mNwManagementService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
            this.mWifiNative = new WifiNative(this.mWifiVendorHal, this.mSupplicantStaIfaceHal, this.mHostapdHal, this.mWificondControl, this.mWifiMonitor, this.mNwManagementService, this.mPropertyService, this.mWifiMetrics, new Handler(this.mWifiCoreHandlerThread.getLooper()), new Random());
            this.mWifiP2pMonitor = new WifiP2pMonitor(this);
            this.mSupplicantP2pIfaceHal = new SupplicantP2pIfaceHal(this.mWifiP2pMonitor);
            this.mWifiP2pNative = new WifiP2pNative(this.mWifiVendorHal, this.mSupplicantP2pIfaceHal, this.mHalDeviceManager, this.mPropertyService);
            this.mWifiTrafficPoller = new WifiTrafficPoller(this.mContext, clientModeImplLooper);
            this.mCountryCode = new WifiCountryCode(this.mWifiNative, this.mContext, SystemProperties.get(BOOT_DEFAULT_WIFI_COUNTRY_CODE), this.mContext.getResources().getBoolean(17891610));
            this.mWifiKeyStore = new WifiKeyStore(this.mKeyStore);
            Context context4 = this.mContext;
            this.mWifiConfigStore = new WifiConfigStore(context4, clientModeImplLooper, this.mClock, this.mWifiMetrics, WifiConfigStore.createSharedFile(UserManager.get(context4)));
            SubscriptionManager subscriptionManager = (SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class);
            Context context5 = this.mContext;
            WifiConfigManager wifiConfigManager = r0;
            Looper clientModeImplLooper2 = clientModeImplLooper;
            WifiConfigManager wifiConfigManager2 = new WifiConfigManager(context5, this.mClock, UserManager.get(context5), makeTelephonyManager(), this.mWifiKeyStore, this.mWifiConfigStore, this.mWifiPermissionsUtil, this.mWifiPermissionsWrapper, this, new NetworkListSharedStoreData(this.mContext), new NetworkListUserStoreData(this.mContext), new DeletedEphemeralSsidsStoreData(this.mClock), new RandomizedMacStoreData(), this.mFrameworkFacade, this.mWifiCoreHandlerThread.getLooper());
            this.mWifiConfigManager = wifiConfigManager;
            this.mWifiMetrics.setWifiConfigManager(this.mWifiConfigManager);
            this.mWifiConnectivityHelper = new WifiConnectivityHelper(this.mWifiNative);
            this.mConnectivityLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 256 : 1024);
            Looper clientModeImplLooper3 = clientModeImplLooper2;
            this.mScoringParams = new ScoringParams(this.mContext, this.mFrameworkFacade, new Handler(clientModeImplLooper3));
            this.mWifiMetrics.setScoringParams(this.mScoringParams);
            this.mWifiNetworkSelector = new WifiNetworkSelector(this.mContext, this.mWifiScoreCard, this.mScoringParams, this.mWifiConfigManager, this.mClock, this.mConnectivityLocalLog, this.mWifiMetrics, this.mWifiNative);
            CompatibilityScorer compatibilityScorer = new CompatibilityScorer(this.mScoringParams);
            this.mWifiNetworkSelector.registerCandidateScorer(compatibilityScorer);
            ScoreCardBasedScorer scoreCardBasedScorer = new ScoreCardBasedScorer(this.mScoringParams);
            this.mWifiNetworkSelector.registerCandidateScorer(scoreCardBasedScorer);
            BubbleFunScorer bubbleFunScorer = new BubbleFunScorer(this.mScoringParams);
            this.mWifiNetworkSelector.registerCandidateScorer(bubbleFunScorer);
            this.mWifiMetrics.setWifiNetworkSelector(this.mWifiNetworkSelector);
            this.mSavedNetworkEvaluator = new SavedNetworkEvaluator(this.mContext, this.mScoringParams, this.mWifiConfigManager, this.mClock, this.mConnectivityLocalLog, this.mWifiConnectivityHelper, subscriptionManager, this.mWifiMetrics);
            this.mWifiNetworkSuggestionsManager = new WifiNetworkSuggestionsManager(this.mContext, new Handler(this.mWifiCoreHandlerThread.getLooper()), this, this.mWifiPermissionsUtil, this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiMetrics, this.mWifiKeyStore);
            this.mNetworkSuggestionEvaluator = new NetworkSuggestionEvaluator(this.mWifiNetworkSuggestionsManager, this.mWifiConfigManager, this.mConnectivityLocalLog);
            this.mScoredNetworkEvaluator = new ScoredNetworkEvaluator(context, clientModeImplLooper3, this.mFrameworkFacade, this.mNetworkScoreManager, this.mWifiConfigManager, this.mConnectivityLocalLog, this.mWifiNetworkScoreCache, this.mWifiPermissionsUtil);
            this.mCarrierNetworkEvaluator = new CarrierNetworkEvaluator(this.mContext, this.mWifiConfigManager, this.mCarrierNetworkConfig, this.mConnectivityLocalLog, this, this.mWifiMetrics);
            this.mSimAccessor = new SIMAccessor(this.mContext);
            Context context6 = this.mContext;
            Handler handler = new Handler(this.mWifiCoreHandlerThread.getLooper());
            WifiNative wifiNative = this.mWifiNative;
            WifiKeyStore wifiKeyStore = this.mWifiKeyStore;
            Clock clock = this.mClock;
            SIMAccessor sIMAccessor = this.mSimAccessor;
            PasspointObjectFactory passpointObjectFactory = new PasspointObjectFactory();
            WifiConfigManager wifiConfigManager3 = this.mWifiConfigManager;
            WifiConfigStore wifiConfigStore = this.mWifiConfigStore;
            WifiMetrics wifiMetrics = this.mWifiMetrics;
            TelephonyManager makeTelephonyManager = makeTelephonyManager();
            Looper clientModeImplLooper4 = this.mConnectivityLocalLog;
            BubbleFunScorer bubbleFunScorer2 = bubbleFunScorer;
            ScoreCardBasedScorer scoreCardBasedScorer2 = scoreCardBasedScorer;
            TelephonyManager telephonyManager = makeTelephonyManager;
            CompatibilityScorer compatibilityScorer2 = compatibilityScorer;
            PasspointManager passpointManager = r0;
            Looper clientModeImplLooper5 = clientModeImplLooper2;
            PasspointManager passpointManager2 = new PasspointManager(context6, this, handler, wifiNative, wifiKeyStore, clock, sIMAccessor, passpointObjectFactory, wifiConfigManager3, wifiConfigStore, wifiMetrics, telephonyManager, subscriptionManager, clientModeImplLooper4);
            this.mPasspointManager = passpointManager;
            this.mPasspointNetworkEvaluator = new PasspointNetworkEvaluator(this.mContext, this.mPasspointManager, this.mWifiConfigManager, this.mConnectivityLocalLog, this.mCarrierNetworkConfig, this, subscriptionManager);
            this.mWifiGeofenceManager = new WifiGeofenceManager(this.mContext, clientModeImplLooper5, this.mWifiConfigManager);
            this.mWifiMetrics.setPasspointManager(this.mPasspointManager);
            Context context7 = this.mContext;
            this.mScanRequestProxy = new ScanRequestProxy(context7, (AppOpsManager) context7.getSystemService("appops"), (ActivityManager) this.mContext.getSystemService("activity"), this, this.mWifiConfigManager, this.mWifiPermissionsUtil, this.mWifiMetrics, this.mClock, this.mFrameworkFacade, new Handler(clientModeImplLooper5));
            this.mSarManager = new SarManager(this.mContext, makeTelephonyManager(), clientModeImplLooper5, this.mWifiNative, new SystemSensorManager(this.mContext, clientModeImplLooper5), this.mWifiMetrics);
            this.mWifiDiagnostics = new WifiDiagnostics(this.mContext, this, this.mWifiNative, this.mBuildProperties, new LastMileLogger(this), this.mClock, this.mWifiDiagnosticsHandlerThread.getLooper());
            this.mWifiDataStall = new WifiDataStall(this.mContext, this.mFrameworkFacade, this.mWifiMetrics);
            this.mWifiMetrics.setWifiDataStall(this.mWifiDataStall);
            this.mLinkProbeManager = new LinkProbeManager(this.mClock, this.mWifiNative, this.mWifiMetrics, this.mFrameworkFacade, this.mWifiCoreHandlerThread.getLooper(), this.mContext);
            this.mIssueDetector = new SemWifiIssueDetector(this.mContext, clientModeImplLooper5, new SemWifiIssueDetector.WifiIssueDetectorAdapter() {
                public void sendBigData(Bundle bigdataParams) {
                    if (WifiInjector.this.mClientModeImpl != null) {
                        WifiInjector.this.mClientModeImpl.callSECApi(WifiInjector.this.mClientModeImpl.obtainMessage(77, 0, 0, bigdataParams));
                    }
                }
            });
            Context context8 = this.mContext;
            Looper looper = clientModeImplLooper5;
            this.mClientModeImpl = new ClientModeImpl(context8, this.mFrameworkFacade, looper, UserManager.get(context8), this, this.mBackupManagerProxy, this.mCountryCode, this.mWifiNative, new WrongPasswordNotifier(this.mContext, this.mFrameworkFacade), this.mSarManager, this.mWifiTrafficPoller, this.mLinkProbeManager);
            Context context9 = this.mContext;
            this.mActiveModeWarden = new ActiveModeWarden(this, context9, looper, this.mWifiNative, new DefaultModeManager(context9, clientModeImplLooper5), this.mBatteryStats);
            Context context10 = this.mContext;
            WifiConfigManager wifiConfigManager4 = this.mWifiConfigManager;
            Context context11 = context10;
            WifiConfigManager wifiConfigManager5 = wifiConfigManager4;
            this.mWakeupController = new WakeupController(this.mContext, this.mWifiCoreHandlerThread.getLooper(), new WakeupLock(this.mWifiConfigManager, this.mWifiMetrics.getWakeupMetrics(), this.mClock), new WakeupEvaluator(this.mScoringParams), new WakeupOnboarding(context11, wifiConfigManager5, this.mWifiCoreHandlerThread.getLooper(), this.mFrameworkFacade, new WakeupNotificationFactory(this.mContext, this.mFrameworkFacade)), this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiNetworkSuggestionsManager, this.mWifiMetrics.getWakeupMetrics(), this, this.mFrameworkFacade, this.mClock);
            this.mLockManager = new WifiLockManager(this.mContext, BatteryStatsService.getService(), this.mClientModeImpl, this.mFrameworkFacade, new Handler(clientModeImplLooper5), this.mWifiNative, this.mClock, this.mWifiMetrics);
            this.mWifiController = new WifiController(this.mContext, this.mClientModeImpl, clientModeImplLooper5, this.mSettingsStore, this.mWifiServiceHandlerThread.getLooper(), this.mFrameworkFacade, this.mActiveModeWarden, this.mWifiPermissionsUtil);
            this.mSelfRecovery = new SelfRecovery(this.mWifiController, this.mClock);
            this.mWifiMulticastLockManager = new WifiMulticastLockManager(this.mClientModeImpl.getMcastLockManagerFilterController(), BatteryStatsService.getService());
            this.mDppManager = new DppManager(this.mWifiCoreHandlerThread.getLooper(), this.mWifiNative, this.mWifiConfigManager, this.mContext, this.mDppMetrics);
            this.mIpMemoryStore = IpMemoryStore.getMemoryStore(this.mContext);
            this.mSemWifiApChipInfo = new SemWifiApChipInfo(this.mContext);
            this.mWifiLowLatency = new WifiLowLatency(this.mContext, this.mWifiNative);
            this.mWifiNetworkSelector.registerNetworkEvaluator(this.mSavedNetworkEvaluator);
            this.mWifiNetworkSelector.registerNetworkEvaluator(this.mNetworkSuggestionEvaluator);
            this.mWifiNetworkSelector.registerNetworkEvaluator(this.mPasspointNetworkEvaluator);
            this.mWifiNetworkSelector.registerNetworkEvaluator(this.mCarrierNetworkEvaluator);
            this.mWifiNetworkSelector.registerNetworkEvaluator(this.mScoredNetworkEvaluator);
            this.mClientModeImpl.start();
        } else {
            throw new IllegalStateException("WifiInjector was already created, use getInstance instead.");
        }
    }

    public static WifiInjector getInstance() {
        WifiInjector wifiInjector = sWifiInjector;
        if (wifiInjector != null) {
            return wifiInjector;
        }
        throw new IllegalStateException("Attempted to retrieve a WifiInjector instance before constructor was called.");
    }

    public void enableVerboseLogging(int verbose) {
        this.mWifiLastResortWatchdog.enableVerboseLogging(verbose);
        this.mWifiBackupRestore.enableVerboseLogging(verbose);
        this.mHalDeviceManager.enableVerboseLogging(verbose);
        this.mScanRequestProxy.enableVerboseLogging(verbose);
        this.mWakeupController.enableVerboseLogging(verbose);
        this.mCarrierNetworkConfig.enableVerboseLogging(verbose);
        this.mWifiNetworkSuggestionsManager.enableVerboseLogging(verbose);
        LogcatLog.enableVerboseLogging(verbose);
        this.mDppManager.enableVerboseLogging(verbose);
        this.mWifiLowLatency.enableVerboseLogging(verbose);
    }

    public UserManager getUserManager() {
        return UserManager.get(this.mContext);
    }

    public WifiMetrics getWifiMetrics() {
        return this.mWifiMetrics;
    }

    public WifiP2pMetrics getWifiP2pMetrics() {
        return this.mWifiP2pMetrics;
    }

    public SupplicantStaIfaceHal getSupplicantStaIfaceHal() {
        return this.mSupplicantStaIfaceHal;
    }

    public BackupManagerProxy getBackupManagerProxy() {
        return this.mBackupManagerProxy;
    }

    public FrameworkFacade getFrameworkFacade() {
        return this.mFrameworkFacade;
    }

    public HandlerThread getWifiServiceHandlerThread() {
        return this.mWifiServiceHandlerThread;
    }

    public HandlerThread getWifiP2pServiceHandlerThread() {
        return this.mWifiP2pServiceHandlerThread;
    }

    public HandlerThread getPasspointProvisionerHandlerThread() {
        return this.mPasspointProvisionerHandlerThread;
    }

    public HandlerThread getWifiCoreHandlerThread() {
        return this.mWifiCoreHandlerThread;
    }

    public WifiTrafficPoller getWifiTrafficPoller() {
        return this.mWifiTrafficPoller;
    }

    public WifiCountryCode getWifiCountryCode() {
        return this.mCountryCode;
    }

    public WifiApConfigStore getWifiApConfigStore() {
        if (this.mWifiApConfigStore == null) {
            this.mWifiApConfigStore = new WifiApConfigStore(this.mContext, this.mWifiCoreHandlerThread.getLooper(), this.mBackupManagerProxy, this.mFrameworkFacade);
        }
        return this.mWifiApConfigStore;
    }

    public SarManager getSarManager() {
        return this.mSarManager;
    }

    public ClientModeImpl getClientModeImpl() {
        return this.mClientModeImpl;
    }

    public Handler getClientModeImplHandler() {
        return this.mClientModeImpl.getHandler();
    }

    public ActiveModeWarden getActiveModeWarden() {
        return this.mActiveModeWarden;
    }

    public WifiSettingsStore getWifiSettingsStore() {
        return this.mSettingsStore;
    }

    public WifiLockManager getWifiLockManager() {
        return this.mLockManager;
    }

    public WifiController getWifiController() {
        return this.mWifiController;
    }

    public WifiLastResortWatchdog getWifiLastResortWatchdog() {
        return this.mWifiLastResortWatchdog;
    }

    public Clock getClock() {
        return this.mClock;
    }

    public PropertyService getPropertyService() {
        return this.mPropertyService;
    }

    public BuildProperties getBuildProperties() {
        return this.mBuildProperties;
    }

    public KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    public WifiBackupRestore getWifiBackupRestore() {
        return this.mWifiBackupRestore;
    }

    public WifiMulticastLockManager getWifiMulticastLockManager() {
        return this.mWifiMulticastLockManager;
    }

    public WifiConfigManager getWifiConfigManager() {
        return this.mWifiConfigManager;
    }

    public PasspointManager getPasspointManager() {
        return this.mPasspointManager;
    }

    public CarrierNetworkConfig getCarrierNetworkConfig() {
        return this.mCarrierNetworkConfig;
    }

    public WakeupController getWakeupController() {
        return this.mWakeupController;
    }

    public ScoringParams getScoringParams() {
        return this.mScoringParams;
    }

    public WifiScoreCard getWifiScoreCard() {
        return this.mWifiScoreCard;
    }

    public TelephonyManager makeTelephonyManager() {
        return (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public WifiStateTracker getWifiStateTracker() {
        return this.mWifiStateTracker;
    }

    public DppManager getDppManager() {
        return this.mDppManager;
    }

    public SemWifiApMonitor getWifiApMonitor() {
        return this.mSemWifiApMonitor;
    }

    public SemWifiApClientInfo getSemWifiApClientInfo() {
        return this.mSemWifiApClientInfo;
    }

    public SemWifiApTimeOutImpl getSemWifiApTimeOutImpl() {
        return this.mSemWifiApTimeOutImpl;
    }

    public SemWifiApPowerSaveImpl getSemWifiApPowerSaveImpl() {
        return this.mSemWifiApPowerSaveImpl;
    }

    public SemSoftapConfig getSemSoftapConfig() {
        return this.mSemSoftapConfig;
    }

    public SemWifiApBroadcastReceiver getSemWifiApBroadcastReceiver() {
        return this.mSemWifiApBroadcastReceiver;
    }

    public IWificond makeWificond() {
        return IWificond.Stub.asInterface(ServiceManager.getService(WIFICOND_SERVICE_NAME));
    }

    public SoftApManager makeSoftApManager(WifiManager.SoftApCallback callback, SoftApModeConfiguration config) {
        return new SoftApManager(this.mContext, this.mWifiCoreHandlerThread.getLooper(), this.mFrameworkFacade, this.mWifiNative, this.mCountryCode.getCountryCode(), callback, this.mWifiApConfigStore, config, this.mWifiMetrics, this.mSarManager);
    }

    public ScanOnlyModeManager makeScanOnlyModeManager(ScanOnlyModeManager.Listener listener) {
        return new ScanOnlyModeManager(this.mContext, this.mWifiCoreHandlerThread.getLooper(), this.mWifiNative, listener, this.mWifiMetrics, this.mWakeupController, this.mCountryCode, this.mSarManager);
    }

    public ClientModeManager makeClientModeManager(ClientModeManager.Listener listener) {
        return new ClientModeManager(this.mContext, this.mWifiCoreHandlerThread.getLooper(), this.mWifiNative, listener, this.mWifiMetrics, this.mClientModeImpl);
    }

    public WifiLog makeLog(String tag) {
        return new LogcatLog(tag);
    }

    public BaseWifiDiagnostics getWifiDiagnostics() {
        return this.mWifiDiagnostics;
    }

    public synchronized WifiScanner getWifiScanner() {
        IWifiScanner service;
        if (this.mWifiScanner == null && (service = IWifiScanner.Stub.asInterface(ServiceManager.getService("wifiscanner"))) != null) {
            try {
                this.mWifiScanner = new WifiScanner(this.mContext, service, this.mWifiCoreHandlerThread.getLooper());
            } catch (IllegalStateException e) {
            }
        }
        return this.mWifiScanner;
    }

    public WifiConnectivityManager makeWifiConnectivityManager(ClientModeImpl clientModeImpl) {
        ClientModeImpl clientModeImpl2 = clientModeImpl;
        Context context = this.mContext;
        Looper looper = this.mWifiCoreHandlerThread.getLooper();
        FrameworkFacade frameworkFacade = this.mFrameworkFacade;
        this.mOpenNetworkNotifier = new OpenNetworkNotifier(context, looper, frameworkFacade, this.mClock, this.mWifiMetrics, this.mWifiConfigManager, this.mWifiConfigStore, clientModeImpl, new ConnectToNetworkNotificationBuilder(this.mContext, frameworkFacade));
        Context context2 = this.mContext;
        Looper looper2 = this.mWifiCoreHandlerThread.getLooper();
        FrameworkFacade frameworkFacade2 = this.mFrameworkFacade;
        this.mWifiNotificationController = new WifiNotificationController(context2, looper2, frameworkFacade2, this.mClock, this.mWifiMetrics, this.mWifiConfigManager, this.mWifiConfigStore, clientModeImpl, new ConnectToNetworkNotificationBuilder(this.mContext, frameworkFacade2));
        Context context3 = this.mContext;
        Looper looper3 = this.mWifiCoreHandlerThread.getLooper();
        FrameworkFacade frameworkFacade3 = this.mFrameworkFacade;
        this.mCarrierNetworkNotifier = new CarrierNetworkNotifier(context3, looper3, frameworkFacade3, this.mClock, this.mWifiMetrics, this.mWifiConfigManager, this.mWifiConfigStore, clientModeImpl, new ConnectToNetworkNotificationBuilder(this.mContext, frameworkFacade3));
        this.mWifiLastResortWatchdog = new WifiLastResortWatchdog(this, this.mClock, this.mWifiMetrics, clientModeImpl, clientModeImpl.getHandler().getLooper());
        return new WifiConnectivityManager(this.mContext, getScoringParams(), clientModeImpl2, this, this.mWifiConfigManager, clientModeImpl.getWifiInfo(), this.mWifiNetworkSelector, this.mWifiConnectivityHelper, this.mWifiLastResortWatchdog, this.mOpenNetworkNotifier, this.mWifiNotificationController, this.mCarrierNetworkNotifier, this.mCarrierNetworkConfig, this.mWifiMetrics, this.mWifiCoreHandlerThread.getLooper(), this.mClock, this.mConnectivityLocalLog, this.mPasspointManager, this.mWifiGeofenceManager);
    }

    public WifiNetworkFactory makeWifiNetworkFactory(NetworkCapabilities nc, WifiConnectivityManager wifiConnectivityManager) {
        Looper looper = this.mWifiCoreHandlerThread.getLooper();
        Context context = this.mContext;
        return new WifiNetworkFactory(looper, context, nc, (ActivityManager) context.getSystemService("activity"), (AlarmManager) this.mContext.getSystemService("alarm"), (AppOpsManager) this.mContext.getSystemService("appops"), this.mClock, this, wifiConnectivityManager, this.mWifiConfigManager, this.mWifiConfigStore, this.mWifiPermissionsUtil, this.mWifiMetrics);
    }

    public NetworkRequestStoreData makeNetworkRequestStoreData(NetworkRequestStoreData.DataSource dataSource) {
        return new NetworkRequestStoreData(dataSource);
    }

    public UntrustedWifiNetworkFactory makeUntrustedWifiNetworkFactory(NetworkCapabilities nc, WifiConnectivityManager wifiConnectivityManager) {
        return new UntrustedWifiNetworkFactory(this.mWifiCoreHandlerThread.getLooper(), this.mContext, nc, wifiConnectivityManager);
    }

    public NetworkSuggestionStoreData makeNetworkSuggestionStoreData(NetworkSuggestionStoreData.DataSource dataSource) {
        return new NetworkSuggestionStoreData(dataSource);
    }

    public WifiPermissionsUtil getWifiPermissionsUtil() {
        return this.mWifiPermissionsUtil;
    }

    public WifiPermissionsWrapper getWifiPermissionsWrapper() {
        return this.mWifiPermissionsWrapper;
    }

    public HandlerThread getWifiAwareHandlerThread() {
        if (this.mWifiAwareHandlerThread == null) {
            this.mWifiAwareHandlerThread = new HandlerThread("wifiAwareService");
            this.mWifiAwareHandlerThread.start();
        }
        return this.mWifiAwareHandlerThread;
    }

    public HandlerThread getRttHandlerThread() {
        if (this.mRttHandlerThread == null) {
            this.mRttHandlerThread = new HandlerThread("wifiRttService");
            this.mRttHandlerThread.start();
        }
        return this.mRttHandlerThread;
    }

    public HalDeviceManager getHalDeviceManager() {
        return this.mHalDeviceManager;
    }

    public WifiNative getWifiNative() {
        return this.mWifiNative;
    }

    public WifiMonitor getWifiMonitor() {
        return this.mWifiMonitor;
    }

    public WifiP2pNative getWifiP2pNative() {
        return this.mWifiP2pNative;
    }

    public WifiP2pMonitor getWifiP2pMonitor() {
        return this.mWifiP2pMonitor;
    }

    public SelfRecovery getSelfRecovery() {
        return this.mSelfRecovery;
    }

    public PowerProfile getPowerProfile() {
        return new PowerProfile(this.mContext, false);
    }

    public ScanRequestProxy getScanRequestProxy() {
        return this.mScanRequestProxy;
    }

    public Runtime getJavaRuntime() {
        return Runtime.getRuntime();
    }

    public ActivityManagerService getActivityManagerService() {
        return ActivityManager.getService();
    }

    public WifiDataStall getWifiDataStall() {
        return this.mWifiDataStall;
    }

    public WifiNetworkSuggestionsManager getWifiNetworkSuggestionsManager() {
        return this.mWifiNetworkSuggestionsManager;
    }

    public SemWifiApChipInfo getSemWifiApChipInfo() {
        return this.mSemWifiApChipInfo;
    }

    public SemWifiApSmartGattServer getSemWifiApSmartGattServer() {
        return this.mSemWifiApSmartGattServer;
    }

    public SemWifiApSmartMHS getSemWifiApSmartMHS() {
        return this.mSemWifiApSmartMHS;
    }

    public SemWifiApSmartClient getSemWifiApSmartClient() {
        return this.mSemWifiApSmartClient;
    }

    public SemWifiApSmartGattClient getSemWifiApSmartGattClient() {
        return this.mSemWifiApSmartGattClient;
    }

    public SemWifiApSmartBleScanner getSemWifiApSmartBleScanner() {
        return this.mSemWifiApSmartBleScanner;
    }

    public void setSemWifiApSmartGattServer(SemWifiApSmartGattServer obj) {
        this.mSemWifiApSmartGattServer = obj;
    }

    public void setSemWifiApSmartMHS(SemWifiApSmartMHS obj) {
        this.mSemWifiApSmartMHS = obj;
    }

    public void setSemWifiApSmartClient(SemWifiApSmartClient obj) {
        this.mSemWifiApSmartClient = obj;
    }

    public void setSemWifiApSmartGattClient(SemWifiApSmartGattClient obj) {
        this.mSemWifiApSmartGattClient = obj;
    }

    public void setSemWifiApSmartBleScanner(SemWifiApSmartBleScanner obj) {
        this.mSemWifiApSmartBleScanner = obj;
    }

    public void setSemWifiApSmartD2DMHS(SemWifiApSmartD2DMHS obj) {
        this.mSemWifiApSmartD2DMHS = obj;
    }

    public void setSemWifiApSmartD2DClient(SemWifiApSmartD2DClient obj) {
        this.mSemWifiApSmartD2DClient = obj;
    }

    public SemWifiApSmartD2DMHS getSemWifiApSmartD2DMHS() {
        return this.mSemWifiApSmartD2DMHS;
    }

    public SemWifiApSmartD2DClient getSemWifiApSmartD2DClient() {
        return this.mSemWifiApSmartD2DClient;
    }

    public void setSemWifiApSmartD2DGattClient(SemWifiApSmartD2DGattClient obj) {
        this.mSemWifiApSmartD2DGattClient = obj;
    }

    public void setSemWifiApTrafficPoller(SemWifiApTrafficPoller obj) {
        this.mSemWifiApTrafficPoller = obj;
    }

    public SemWifiApSmartD2DGattClient getSemWifiApSmartD2DGattClient() {
        return this.mSemWifiApSmartD2DGattClient;
    }

    public SemWifiApTrafficPoller getSemWifiApTrafficPoller() {
        return this.mSemWifiApTrafficPoller;
    }

    public HostapdHal getHostapdHAL() {
        return this.mHostapdHal;
    }

    public IpMemoryStore getIpMemoryStore() {
        return this.mIpMemoryStore;
    }

    public SemWifiIssueDetector getIssueDetector() {
        return this.mIssueDetector;
    }

    public WifiGeofenceManager getWifiGeofenceManager() {
        return this.mWifiGeofenceManager;
    }

    public WifiPickerController getPickerController() {
        return this.mPickerController;
    }

    public WifiB2BConfigurationPolicy getWifiB2bConfigPolicy() {
        return this.mWifiB2bConfigPolicy;
    }

    public WifiLowLatency getWifiLowLatency() {
        return this.mWifiLowLatency;
    }

    public SemWifiProfileAndQoSProvider getQoSProfileShareProvider(SemWifiProfileAndQoSProvider.Adapter adapter) {
        if (this.mSemQoSProfileShareProvider == null) {
            this.mSemQoSProfileShareProvider = new SemWifiProfileAndQoSProvider(this.mContext, adapter);
        }
        return this.mSemQoSProfileShareProvider;
    }
}
