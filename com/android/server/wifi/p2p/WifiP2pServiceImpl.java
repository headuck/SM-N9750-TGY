package com.android.server.wifi.p2p;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.ip.IIpClient;
import android.net.ip.IpClientCallbacks;
import android.net.ip.IpClientUtil;
import android.net.shared.ProvisioningConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pConfigList;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pStaticIpConfig;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SemHqmManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.p2p.common.GUIUtil;
import com.android.server.wifi.p2p.common.Hash;
import com.android.server.wifi.p2p.common.Util;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.WifiAsyncChannel;
import com.android.server.wifi.util.WifiHandler;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.samsung.android.desktopmode.SemDesktopModeManager;
import com.samsung.android.desktopmode.SemDesktopModeState;
import com.samsung.android.feature.SemFloatingFeature;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import libcore.util.HexEncoding;

public class WifiP2pServiceImpl extends IWifiP2pManager.Stub {
    private static final String ACTION_CHECK_SIOP_LEVEL = "com.samsung.intent.action.CHECK_SIOP_LEVEL";
    private static final String ACTION_P2P_LO_TIMER_EXPIRED = "com.samsung.android.net.wifi.p2p.LO_TIMER_EXPIRED";
    private static final String ACTION_P2P_STOPFIND_TIMER_EXPIRED = "android.net.wifi.p2p.STOPFIND_TIMER_EXPIRED";
    private static final String ACTION_SMARTSWITCH_TRANSFER = "com.samsung.android.intent.SMARTSWITCH_TRANSFER";
    private static final int ADVANCED_OPP_MAX_SCAN_RETRY = 8;
    private static final String ANONYMIZED_DEVICE_ADDRESS = "02:00:00:00:00:00";
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static final int CMD_BOOT_COMPLETED = 143415;
    public static final int CMD_SEC_LOGGING = 143420;
    private static final int CONNECTION_TIMED_OUT = 30;
    private static final int CONTACT_CRC_LENGTH = 4;
    private static final int CONTACT_HASH_LENGTH = 6;
    private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
    private static final int DEFAULT_POLL_TRAFFIC_INTERVAL_MSECS = 3000;
    private static final String DEFAULT_STATIC_IP = "192.168.49.10";
    /* access modifiers changed from: private */
    public static final String[] DHCP_RANGE = {"192.168.49.100", "192.168.49.199"};
    public static final int DISABLED = 0;
    public static final int DISABLE_P2P = 143377;
    public static final int DISABLE_P2P_RSP = 143395;
    public static final int DISABLE_P2P_TIMED_OUT = 143366;
    private static final int DISABLE_P2P_WAIT_TIME_MS = 5000;
    public static final int DISCONNECT_WIFI_REQUEST = 143372;
    public static final int DISCONNECT_WIFI_RESPONSE = 143373;
    private static final int DISCOVER_TIMEOUT_S = 120;
    private static final int DROP_WIFI_USER_ACCEPT = 143364;
    private static final int DROP_WIFI_USER_REJECT = 143365;
    private static final String EMPTY_DEVICE_ADDRESS = "00:00:00:00:00:00";
    public static final int ENABLED = 1;
    public static final int ENABLE_P2P = 143376;
    public static final String ENABLE_SURVEY_MODE = SemFloatingFeature.getInstance().getString("SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE");
    public static final boolean ENABLE_UNIFIED_HQM_SERVER = true;
    /* access modifiers changed from: private */
    public static final Boolean FORM_GROUP = false;
    public static final int GROUP_CREATING_TIMED_OUT = 143361;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 120000;
    private static final int GROUP_IDLE_TIME_S = 10;
    private static final int IDX_PHONE = 256;
    private static final int IDX_TABLET = 512;
    private static final int INVITATION_PROCEDURE_TIMED_OUT = 143411;
    private static final int INVITATION_WAIT_TIME_MS = 30000;
    private static final int IPC_DHCP_RESULTS = 143392;
    private static final int IPC_POST_DHCP_ACTION = 143391;
    private static final int IPC_PRE_DHCP_ACTION = 143390;
    private static final int IPC_PROVISIONING_FAILURE = 143394;
    private static final int IPC_PROVISIONING_SUCCESS = 143393;
    private static final Boolean JOIN_GROUP = true;
    private static final int MAX_DEVICE_NAME_LENGTH = 32;
    private static final int MODE_FORCE_GC = 4;
    private static final int MODE_PERSISTENT = 8;
    private static final int MODE_RETRY_COUNT = 3;
    private static final String NETWORKTYPE = "WIFI_P2P";
    private static final int NFC_REQUEST_TIMED_OUT = 143410;
    private static final Boolean NO_RELOAD = false;
    private static final int P2P_ADVOPP_DELAYED_DISCOVER_PEER = 143461;
    private static final int P2P_ADVOPP_DISCOVER_PEER = 143460;
    private static final int P2P_ADVOPP_LISTEN_TIMEOUT = 143462;
    public static final int P2P_CONNECTION_CHANGED = 143371;
    public static final int P2P_ENABLE_PENDING = 143370;
    private static final int P2P_EXPIRATION_TIME = 5;
    private static final int P2P_GROUP_STARTED_TIMED_OUT = 143412;
    /* access modifiers changed from: private */
    public static String P2P_HW_MAC_ADDRESS = EMPTY_DEVICE_ADDRESS;
    private static final int P2P_INVITATION_WAKELOCK_DURATION = 30000;
    private static final int P2P_LISTEN_EXPIRATION_TIME = 2;
    private static final int P2P_LISTEN_OFFLOADING_CHAN_NUM = 99999;
    private static final int P2P_LISTEN_OFFLOADING_COUNT = 4;
    private static final int P2P_LISTEN_OFFLOADING_FIND_TIMEOUT = 1;
    private static final int P2P_LISTEN_OFFLOADING_INTERVAL = 31000;
    private static final int P2P_TRAFFIC_POLL = 143480;
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    public static final int PEER_CONNECTION_USER_CONFIRM = 143367;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    /* access modifiers changed from: private */
    public static String RANDOM_MAC_ADDRESS = EMPTY_DEVICE_ADDRESS;
    /* access modifiers changed from: private */
    public static final String[] RECEIVER_PERMISSIONS_FOR_BROADCAST = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE"};
    /* access modifiers changed from: private */
    public static final Boolean RELOAD = true;
    public static final int REMOVE_CLIENT_INFO = 143378;
    private static final String SERVER_ADDRESS = "192.168.49.1";
    public static final int SET_COUNTRY_CODE = 143379;
    public static final int SET_MIRACAST_MODE = 143374;
    private static final String SIDESYNC_ACTION_SINK_CONNECTED = "com.sec.android.sidesync.sink.SIDESYNC_CONNECTED";
    private static final String SIDESYNC_ACTION_SINK_DESTROYED = "com.sec.android.sidesync.sink.SERVICE_DESTROY";
    private static final String SIDESYNC_ACTION_SOURCE_CONNECTED = "com.sec.android.sidesync.source.SIDESYNC_CONNECTED";
    private static final String SIDESYNC_ACTION_SOURCE_DESTROYED = "com.sec.android.sidesync.source.SERVICE_DESTROY";
    private static final String SSRM_NOTIFICATION_PERMISSION = "com.samsung.android.permission.SSRM_NOTIFICATION_PERMISSION";
    private static final String TAG = "WifiP2pService";
    private static final int TIME_ELAPSED_AFTER_CONNECTED = 143413;
    private static final String WIFI_DIRECT_SETTINGS_PKGNAME = "com.android.settings";
    public static final int WIFI_ENABLE_PROCEED = 143380;
    /* access modifiers changed from: private */
    public static String WIFI_HW_MAC_ADDRESS = EMPTY_DEVICE_ADDRESS;
    private static final String WIFI_P2P_NOTIFICATION_CHANNEL = "wifi_p2p_notification_channel";
    /* access modifiers changed from: private */
    public static String chkWfdStatus = "disconnected";
    /* access modifiers changed from: private */
    public static byte[] hash2_byte = {0, 0};
    /* access modifiers changed from: private */
    public static byte[] hash_byte = {0, 0, 0};
    private static int intentValue = 0;
    private static final int mAdvancedOppDelayedDiscoverTime = 10000;
    private static final int mAdvancedOppListenTimeout = 20;
    private static final int mAdvancedOppScanIntervalTime = 3000;
    private static int mDurationForNoa = 0;
    private static long mStartTimeForNoa = 0;
    /* access modifiers changed from: private */
    public static boolean mVerboseLoggingEnabled = true;
    private static long mWorkingTimeForNoa = 0;
    /* access modifiers changed from: private */
    public static boolean mWpsSkip;
    private static int numofclients = 0;
    /* access modifiers changed from: private */
    public static int sDisableP2pTimeoutIndex = 0;
    /* access modifiers changed from: private */
    public static int sGroupCreatingTimeoutIndex = 0;
    /* access modifiers changed from: private */
    public static int siopLevel = -3;
    private final String APP_ID = "android.net.wifi.p2p";
    /* access modifiers changed from: private */
    public int idxIcon = 256;
    /* access modifiers changed from: private */
    public InputMethodManager imm;
    /* access modifiers changed from: private */
    public boolean isNightMode;
    /* access modifiers changed from: private */
    public ActivityManager mActivityMgr;
    /* access modifiers changed from: private */
    public boolean mAdvancedOppInProgress = false;
    /* access modifiers changed from: private */
    public boolean mAdvancedOppReceiver = false;
    /* access modifiers changed from: private */
    public boolean mAdvancedOppRemoveGroupAndJoin = false;
    /* access modifiers changed from: private */
    public boolean mAdvancedOppRemoveGroupAndListen = false;
    /* access modifiers changed from: private */
    public int mAdvancedOppScanRetryCount = 0;
    /* access modifiers changed from: private */
    public boolean mAdvancedOppSender = false;
    /* access modifiers changed from: private */
    public AlarmManager mAlarmManager;
    /* access modifiers changed from: private */
    public boolean mAutonomousGroup;
    public P2pBigDataLog mBigData;
    /* access modifiers changed from: private */
    public boolean mBleLatency = false;
    /* access modifiers changed from: private */
    public Map<IBinder, Messenger> mClientChannelList = new HashMap();
    private ClientHandler mClientHandler;
    /* access modifiers changed from: private */
    public HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap<>();
    /* access modifiers changed from: private */
    public ConnectivityManager mCm;
    /* access modifiers changed from: private */
    public WifiP2pConnectReqInfo mConnReqInfo = new WifiP2pConnectReqInfo();
    /* access modifiers changed from: private */
    public HashMap<String, WifiP2pConnectReqInfo> mConnReqInfoList = new HashMap<>();
    /* access modifiers changed from: private */
    public int mConnectedDevicesCnt;
    /* access modifiers changed from: private */
    public Notification mConnectedNotification;
    /* access modifiers changed from: private */
    public HashMap<String, WifiP2pConnectedPeriodInfo> mConnectedPeriodInfoList = new HashMap<>();
    /* access modifiers changed from: private */
    public String mConnectedPkgName = null;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public int mCountWifiAntenna = 1;
    /* access modifiers changed from: private */
    public final Map<IBinder, DeathHandlerData> mDeathDataByBinder = new HashMap();
    /* access modifiers changed from: private */
    public boolean mDelayedDiscoverPeers = false;
    /* access modifiers changed from: private */
    public int mDelayedDiscoverPeersArg;
    /* access modifiers changed from: private */
    public int mDelayedDiscoverPeersCmd;
    /* access modifiers changed from: private */
    public String mDeviceNameInSettings = null;
    /* access modifiers changed from: private */
    public DhcpResults mDhcpResults;
    /* access modifiers changed from: private */
    public PowerManager.WakeLock mDialogWakeLock;
    /* access modifiers changed from: private */
    public boolean mDiscoveryBlocked = false;
    /* access modifiers changed from: private */
    public boolean mDiscoveryPostponed = false;
    /* access modifiers changed from: private */
    public boolean mDiscoveryStarted = false;
    /* access modifiers changed from: private */
    public Messenger mForegroundAppMessenger;
    /* access modifiers changed from: private */
    public String mForegroundAppPkgName;
    /* access modifiers changed from: private */
    public FrameworkFacade mFrameworkFacade;
    private String mInterface;
    /* access modifiers changed from: private */
    public AlertDialog mInvitationDialog = null;
    /* access modifiers changed from: private */
    public TextView mInvitationMsg;
    /* access modifiers changed from: private */
    public IIpClient mIpClient;
    /* access modifiers changed from: private */
    public int mIpClientStartIndex = 0;
    /* access modifiers changed from: private */
    public boolean mIsBootComplete = false;
    /* access modifiers changed from: private */
    public boolean mJoinExistingGroup;
    /* access modifiers changed from: private */
    public int mLOCount = 0;
    /* access modifiers changed from: private */
    public PendingIntent mLOTimerIntent;
    /* access modifiers changed from: private */
    public int mLapseTime;
    /* access modifiers changed from: private */
    public String mLastSetCountryCode;
    /* access modifiers changed from: private */
    public boolean mListenOffloading = false;
    private LocationManager mLocationManager;
    private Object mLock = new Object();
    /* access modifiers changed from: private */
    public int mMaxClientCnt = 0;
    /* access modifiers changed from: private */
    public AlertDialog mMaximumConnectionDialog = null;
    /* access modifiers changed from: private */
    public HashMap<String, Integer> mModeChange = new HashMap<>();
    /* access modifiers changed from: private */
    public NetworkInfo mNetworkInfo;
    private Notification mNotification;
    INetworkManagementService mNwService;
    /* access modifiers changed from: private */
    public P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    /* access modifiers changed from: private */
    public boolean mPersistentGroup = false;
    /* access modifiers changed from: private */
    public volatile int mPollTrafficIntervalMsecs = IWCEventManager.wifiOFFPending_MS;
    private PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mReceivedEnableP2p = false;
    /* access modifiers changed from: private */
    public String mReinvokePersistent = null;
    /* access modifiers changed from: private */
    public int mReinvokePersistentNetId = -1;
    /* access modifiers changed from: private */
    public AsyncChannel mReplyChannel = new WifiAsyncChannel(TAG);
    /* access modifiers changed from: private */
    public SemHqmManager mSemHqmManager;
    /* access modifiers changed from: private */
    public String mServiceDiscReqId;
    /* access modifiers changed from: private */
    public byte mServiceTransactionId = 0;
    /* access modifiers changed from: private */
    public boolean mSetupInterfaceIsRunnging = false;
    private Notification mSoundNotification;
    private WifiP2pDevice mTempDevice = new WifiP2pDevice();
    /* access modifiers changed from: private */
    public boolean mTemporarilyDisconnectedWifi = false;
    /* access modifiers changed from: private */
    public WifiP2pDevice mThisDevice = new WifiP2pDevice();
    /* access modifiers changed from: private */
    public long mTimeForGopsReceiver = 0;
    /* access modifiers changed from: private */
    public PendingIntent mTimerIntent;
    /* access modifiers changed from: private */
    public int mTrafficPollToken = 0;
    /* access modifiers changed from: private */
    public Context mUiContext;
    /* access modifiers changed from: private */
    public Context mUiContextDay;
    /* access modifiers changed from: private */
    public Context mUiContextNight;
    /* access modifiers changed from: private */
    public boolean mValidFreqConflict = false;
    private PowerManager.WakeLock mWakeLock;
    /* access modifiers changed from: private */
    public boolean mWfdConnected = false;
    /* access modifiers changed from: private */
    public boolean mWfdDialog = false;
    /* access modifiers changed from: private */
    public WifiManager.WifiLock mWiFiLock = null;
    /* access modifiers changed from: private */
    public int mWifiApState = 11;
    /* access modifiers changed from: private */
    public WifiAwareManager mWifiAwareManager = null;
    /* access modifiers changed from: private */
    public AsyncChannel mWifiChannel;
    /* access modifiers changed from: private */
    public WifiInjector mWifiInjector;
    /* access modifiers changed from: private */
    public WifiP2pMetrics mWifiP2pMetrics;
    /* access modifiers changed from: private */
    public WifiPermissionsUtil mWifiPermissionsUtil;
    /* access modifiers changed from: private */
    public int mWifiState = 1;
    /* access modifiers changed from: private */
    public CountDownTimer mWpsTimer;
    EditText pin = null;
    /* access modifiers changed from: private */
    public EditText pinConn;
    AlertDialog t_dialog = null;
    /* access modifiers changed from: private */
    public boolean userRejected = false;

    static /* synthetic */ int access$1608(WifiP2pServiceImpl x0) {
        int i = x0.mLOCount;
        x0.mLOCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$16204() {
        int i = sGroupCreatingTimeoutIndex + 1;
        sGroupCreatingTimeoutIndex = i;
        return i;
    }

    static /* synthetic */ int access$18508(WifiP2pServiceImpl x0) {
        int i = x0.mAdvancedOppScanRetryCount;
        x0.mAdvancedOppScanRetryCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$19608(WifiP2pServiceImpl x0) {
        int i = x0.mTrafficPollToken;
        x0.mTrafficPollToken = i + 1;
        return i;
    }

    static /* synthetic */ int access$24410(WifiP2pServiceImpl x0) {
        int i = x0.mLapseTime;
        x0.mLapseTime = i - 1;
        return i;
    }

    static /* synthetic */ byte access$25404(WifiP2pServiceImpl x0) {
        byte b = (byte) (x0.mServiceTransactionId + 1);
        x0.mServiceTransactionId = b;
        return b;
    }

    static /* synthetic */ int access$8604() {
        int i = sDisableP2pTimeoutIndex + 1;
        sDisableP2pTimeoutIndex = i;
        return i;
    }

    public enum P2pStatus {
        SUCCESS,
        INFORMATION_IS_CURRENTLY_UNAVAILABLE,
        INCOMPATIBLE_PARAMETERS,
        LIMIT_REACHED,
        INVALID_PARAMETER,
        UNABLE_TO_ACCOMMODATE_REQUEST,
        PREVIOUS_PROTOCOL_ERROR,
        NO_COMMON_CHANNEL,
        UNKNOWN_P2P_GROUP,
        BOTH_GO_INTENT_15,
        INCOMPATIBLE_PROVISIONING_METHOD,
        REJECTED_BY_USER,
        UNKNOWN;

        public static P2pStatus valueOf(int error) {
            switch (error) {
                case 0:
                    return SUCCESS;
                case 1:
                    return INFORMATION_IS_CURRENTLY_UNAVAILABLE;
                case 2:
                    return INCOMPATIBLE_PARAMETERS;
                case 3:
                    return LIMIT_REACHED;
                case 4:
                    return INVALID_PARAMETER;
                case 5:
                    return UNABLE_TO_ACCOMMODATE_REQUEST;
                case 6:
                    return PREVIOUS_PROTOCOL_ERROR;
                case 7:
                    return NO_COMMON_CHANNEL;
                case 8:
                    return UNKNOWN_P2P_GROUP;
                case 9:
                    return BOTH_GO_INTENT_15;
                case 10:
                    return INCOMPATIBLE_PROVISIONING_METHOD;
                case 11:
                    return REJECTED_BY_USER;
                default:
                    return UNKNOWN;
            }
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 139265:
                case 139268:
                case 139271:
                case 139274:
                case 139277:
                case 139280:
                case 139283:
                case 139285:
                case 139287:
                case 139292:
                case 139295:
                case 139298:
                case 139301:
                case 139304:
                case 139307:
                case 139310:
                case 139315:
                case 139318:
                case 139321:
                case 139323:
                case 139326:
                case 139329:
                case 139332:
                case 139335:
                case 139346:
                case 139349:
                case 139351:
                case 139354:
                case 139356:
                case 139358:
                case 139360:
                case 139361:
                case 139365:
                case 139368:
                case 139371:
                case 139372:
                case 139374:
                case 139375:
                case 139376:
                case 139377:
                case 139378:
                case 139380:
                case 139405:
                case 139406:
                case 139407:
                case 139408:
                case 139412:
                case 139414:
                case 139415:
                case 139419:
                case 139420:
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(Message.obtain(msg));
                    return;
                default:
                    Slog.d(WifiP2pServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + msg);
                    return;
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog log) {
        this.mClientHandler.setWifiLog(log);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setWifiLogForReplyChannel(WifiLog log) {
        this.mReplyChannel.setWifiLog(log);
    }

    private class DeathHandlerData {
        IBinder.DeathRecipient mDeathRecipient;
        Messenger mMessenger;

        DeathHandlerData(IBinder.DeathRecipient dr, Messenger m) {
            this.mDeathRecipient = dr;
            this.mMessenger = m;
        }

        public String toString() {
            return "deathRecipient=" + this.mDeathRecipient + ", messenger=" + this.mMessenger;
        }
    }

    public WifiP2pServiceImpl(Context context, WifiInjector wifiInjector) {
        Context context2 = context;
        this.mContext = new ContextThemeWrapper(context2, context.getResources().getIdentifier("@android:style/Theme.DeviceDefault.Light", (String) null, (String) null));
        this.mWifiInjector = wifiInjector;
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mFrameworkFacade = this.mWifiInjector.getFrameworkFacade();
        this.mWifiP2pMetrics = this.mWifiInjector.getWifiP2pMetrics();
        Context uiContext = ActivityThread.currentActivityThread().getSystemUiContext();
        this.mUiContextDay = new ContextThemeWrapper(uiContext, 16974123);
        this.mUiContextNight = new ContextThemeWrapper(uiContext, 16974120);
        this.mUiContext = this.mUiContextDay;
        this.mInterface = "p2p0";
        this.mActivityMgr = (ActivityManager) context2.getSystemService("activity");
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, TAG);
        this.mDialogWakeLock = this.mPowerManager.newWakeLock(268435482, TAG);
        this.mDialogWakeLock.setReferenceCounted(false);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
            this.mWifiAwareManager = (WifiAwareManager) this.mContext.getSystemService("wifiaware");
        }
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mTimerIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_P2P_STOPFIND_TIMER_EXPIRED, (Uri) null), 0);
        this.mLOTimerIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_P2P_LO_TIMER_EXPIRED, (Uri) null), 0);
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(17040004);
        HandlerThread wifiP2pThread = this.mWifiInjector.getWifiP2pServiceHandlerThread();
        this.mClientHandler = new ClientHandler(TAG, wifiP2pThread.getLooper());
        this.mP2pStateMachine = new P2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        this.mP2pStateMachine.start();
        this.mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");
        this.mBigData = new P2pBigDataLog();
        if (this.mPowerManager.isScreenOn()) {
            setProp("lcdon");
        } else {
            setProp("lcdoff");
        }
        this.mCountWifiAntenna = 2;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction(ACTION_P2P_STOPFIND_TIMER_EXPIRED);
        filter.addAction(ACTION_P2P_LO_TIMER_EXPIRED);
        filter.addAction("com.samsung.android.knox.intent.action.RESTRICTION_DISABLE_WFD_INTERNAL");
        this.mContext.registerReceiver(new WifiStateReceiver(), filter);
        IntentFilter gopsFilter = new IntentFilter();
        gopsFilter.addAction(SIDESYNC_ACTION_SOURCE_CONNECTED);
        gopsFilter.addAction(SIDESYNC_ACTION_SINK_CONNECTED);
        gopsFilter.addAction(SIDESYNC_ACTION_SOURCE_DESTROYED);
        gopsFilter.addAction(SIDESYNC_ACTION_SINK_DESTROYED);
        gopsFilter.addAction("android.intent.action.SCREEN_ON");
        gopsFilter.addAction("android.intent.action.SCREEN_OFF");
        gopsFilter.addAction(ACTION_SMARTSWITCH_TRANSFER);
        gopsFilter.addAction(ACTION_CHECK_SIOP_LEVEL);
        gopsFilter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        this.mContext.registerReceiver(new GopsReceiver(), gopsFilter);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                WifiP2pServiceImpl.this.sendSetDeviceName();
            }
        }, new IntentFilter("com.android.settings.DEVICE_NAME_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("started", false)) {
                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                        Log.d(WifiP2pServiceImpl.TAG, "OnReceive - com.android.settings.wifi.p2p.SETTINGS_STRATED");
                    }
                    WifiP2pServiceImpl.this.sendSetDeviceName();
                    if (Build.VERSION.SDK_INT < 29) {
                        WifiP2pServiceImpl.this.mP2pStateMachine.setPhoneNumberIntoProbeResp();
                    }
                }
            }
        }, new IntentFilter("com.android.settings.wifi.p2p.SETTINGS_STRATED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String simState = intent.getStringExtra("ss");
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    Log.d(WifiP2pServiceImpl.TAG, "OnReceive - android.intent.action.SIM_STATE_CHANGED, state : " + simState);
                }
                if ("LOADED".equals(simState) && Build.VERSION.SDK_INT < 29) {
                    WifiP2pServiceImpl.this.mP2pStateMachine.setPhoneNumberIntoProbeResp();
                }
            }
        }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    Log.d(WifiP2pServiceImpl.TAG, "OnReceive - ACTION_BOOT_COMPLETED");
                }
                WifiP2pServiceImpl.this.sendSetDeviceName();
                boolean unused = WifiP2pServiceImpl.this.mIsBootComplete = true;
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.CMD_BOOT_COMPLETED);
            }
        }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    Log.d(WifiP2pServiceImpl.TAG, "OnReceive - ACTION_CONFIGURATION_CHANGED");
                }
                boolean unused = WifiP2pServiceImpl.this.isNightMode = (context.getResources().getConfiguration().uiMode & 48) == 32;
                if (WifiP2pServiceImpl.this.isNightMode && WifiP2pServiceImpl.this.mUiContext == WifiP2pServiceImpl.this.mUiContextDay) {
                    WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                    Context unused2 = wifiP2pServiceImpl.mUiContext = wifiP2pServiceImpl.mUiContextNight;
                    if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                        WifiP2pServiceImpl.this.dialogRejectForThemeChanging();
                    }
                } else if (!WifiP2pServiceImpl.this.isNightMode && WifiP2pServiceImpl.this.mUiContext == WifiP2pServiceImpl.this.mUiContextNight) {
                    WifiP2pServiceImpl wifiP2pServiceImpl2 = WifiP2pServiceImpl.this;
                    Context unused3 = wifiP2pServiceImpl2.mUiContext = wifiP2pServiceImpl2.mUiContextDay;
                    if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                        WifiP2pServiceImpl.this.dialogRejectForThemeChanging();
                    }
                }
            }
        }, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    public void connectivityServiceReady() {
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    public void handleUserSwitch(int userId) {
        Log.d(TAG, "Removing p2p group when user switching");
        this.mP2pStateMachine.sendMessage(139280);
    }

    public void checkDeviceNameInSettings() {
        this.mDeviceNameInSettings = Settings.System.getString(this.mContext.getContentResolver(), "device_name");
        if (TextUtils.isEmpty(this.mDeviceNameInSettings)) {
            this.mDeviceNameInSettings = Settings.Global.getString(this.mContext.getContentResolver(), "device_name");
        }
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "checkDeviceNameInSettings: " + this.mDeviceNameInSettings);
        }
    }

    public void sendSetDeviceName() {
        checkDeviceNameInSettings();
        if (!TextUtils.isEmpty(this.mDeviceNameInSettings) && !this.mDeviceNameInSettings.equals(this.mP2pStateMachine.getPersistedDeviceName())) {
            if (mVerboseLoggingEnabled) {
                Log.d(TAG, "sendSetDeviceName: this will be set only in P2pEnabledState");
            }
            WifiP2pDevice wifiP2pDevice = this.mTempDevice;
            wifiP2pDevice.deviceName = this.mDeviceNameInSettings;
            this.mP2pStateMachine.sendMessage(139315, 0, 0, wifiP2pDevice);
        }
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        private WifiStateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                int unused = WifiP2pServiceImpl.this.mWifiState = intent.getIntExtra("wifi_state", 1);
            } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                int unused2 = WifiP2pServiceImpl.this.mWifiApState = intent.getIntExtra("wifi_state", 11);
            } else if (action.equals("com.samsung.android.knox.intent.action.RESTRICTION_DISABLE_WFD_INTERNAL")) {
                if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(139280);
                }
            } else if (action.equals(WifiP2pServiceImpl.ACTION_P2P_STOPFIND_TIMER_EXPIRED)) {
                Log.d(WifiP2pServiceImpl.TAG, "ACTION_P2P_STOPFIND_TIMER_EXPIRED");
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(139268);
            } else if (action.equals(WifiP2pServiceImpl.ACTION_P2P_LO_TIMER_EXPIRED)) {
                Log.d(WifiP2pServiceImpl.TAG, "ACTION_P2P_LO_TIMER_EXPIRED");
                if (WifiP2pServiceImpl.this.mLOCount < 4) {
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(139265, WifiP2pServiceImpl.P2P_LISTEN_OFFLOADING_CHAN_NUM);
                    return;
                }
                Log.d(WifiP2pServiceImpl.TAG, " Reset listen offloading count to 0! LO ended!");
                int unused3 = WifiP2pServiceImpl.this.mLOCount = 0;
                boolean unused4 = WifiP2pServiceImpl.this.mListenOffloading = false;
            }
        }
    }

    private class GopsReceiver extends BroadcastReceiver {
        private GopsReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean chkSmswTransfer;
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                long unused = WifiP2pServiceImpl.this.mTimeForGopsReceiver = System.currentTimeMillis();
                Log.d(WifiP2pServiceImpl.TAG, "GopsReceiver : received : " + action);
                if (WifiP2pServiceImpl.SIDESYNC_ACTION_SOURCE_CONNECTED.equals(action)) {
                    WifiP2pServiceImpl.this.setProp("sscon");
                } else if (WifiP2pServiceImpl.SIDESYNC_ACTION_SOURCE_DESTROYED.equals(action)) {
                    WifiP2pServiceImpl.this.setProp("ssdis");
                } else if (WifiP2pServiceImpl.SIDESYNC_ACTION_SINK_CONNECTED.equals(action)) {
                    WifiP2pServiceImpl.this.setProp("sicon");
                } else if (WifiP2pServiceImpl.SIDESYNC_ACTION_SINK_DESTROYED.equals(action)) {
                    WifiP2pServiceImpl.this.setProp("sidis");
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    WifiP2pServiceImpl.this.setProp("lcdon");
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    WifiP2pServiceImpl.this.setProp("lcdoff");
                } else if (WifiP2pServiceImpl.ACTION_SMARTSWITCH_TRANSFER.equals(action)) {
                    try {
                        chkSmswTransfer = intent.getBooleanExtra("smartswitch_transfer", false);
                    } catch (Exception e) {
                        chkSmswTransfer = false;
                        Log.e(WifiP2pServiceImpl.TAG, "smartswitch_transfer is not set. because exception is occured.");
                    }
                    Log.i(WifiP2pServiceImpl.TAG, "smartswitch_transfer = " + chkSmswTransfer);
                    if (chkSmswTransfer) {
                        WifiP2pServiceImpl.this.setProp("smswon");
                    } else {
                        WifiP2pServiceImpl.this.setProp("smswoff");
                    }
                } else if (WifiP2pServiceImpl.ACTION_CHECK_SIOP_LEVEL.equals(action)) {
                    try {
                        int unused2 = WifiP2pServiceImpl.siopLevel = intent.getIntExtra("siop_level_broadcast", -3);
                    } catch (Exception e2) {
                        int unused3 = WifiP2pServiceImpl.siopLevel = -3;
                        Log.e(WifiP2pServiceImpl.TAG, "siop_level was set to the default value. because exception is occured.");
                    }
                    Log.i(WifiP2pServiceImpl.TAG, "siop_level = " + WifiP2pServiceImpl.siopLevel);
                    WifiP2pServiceImpl.this.setProp("siopLevCha");
                } else if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action)) {
                    try {
                        int wfdStatus = intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS").getActiveDisplayState();
                        if (wfdStatus == 2) {
                            String unused4 = WifiP2pServiceImpl.chkWfdStatus = "connected";
                        } else if (wfdStatus == 1) {
                            String unused5 = WifiP2pServiceImpl.chkWfdStatus = "connecting";
                        } else {
                            String unused6 = WifiP2pServiceImpl.chkWfdStatus = "disconnected";
                        }
                    } catch (Exception e3) {
                        String unused7 = WifiP2pServiceImpl.chkWfdStatus = "disconnected";
                        Log.e(WifiP2pServiceImpl.TAG, "chkWfdStatus was set to the default value. because exception is occured.");
                    }
                    Log.i(WifiP2pServiceImpl.TAG, "chkWfdStatus = " + WifiP2pServiceImpl.chkWfdStatus);
                    WifiP2pServiceImpl.this.setProp("wfdSta");
                }
                Log.d(WifiP2pServiceImpl.TAG, "GopsReceiver : received : " + action + " time : " + (System.currentTimeMillis() - WifiP2pServiceImpl.this.mTimeForGopsReceiver) + "ms");
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setProp(java.lang.String r12) {
        /*
            r11 = this;
            r0 = -1
            int r1 = r12.hashCode()     // Catch:{ Exception -> 0x01c8 }
            r2 = 8
            r3 = 4
            r4 = 2
            java.lang.String r5 = "lcdon"
            java.lang.String r6 = "lcdoff"
            r7 = 1
            r8 = 0
            switch(r1) {
                case -2080821647: goto L_0x00a7;
                case -1482467299: goto L_0x009c;
                case -1108501374: goto L_0x0094;
                case -898407267: goto L_0x008a;
                case -790836629: goto L_0x007f;
                case 102789228: goto L_0x0077;
                case 109431660: goto L_0x006d;
                case 109432440: goto L_0x0063;
                case 109729570: goto L_0x0059;
                case 109730350: goto L_0x004f;
                case 657006763: goto L_0x0044;
                case 1138883286: goto L_0x0038;
                case 1272544337: goto L_0x002c;
                case 1272545117: goto L_0x0020;
                case 1807858777: goto L_0x0014;
                default: goto L_0x0012;
            }
        L_0x0012:
            goto L_0x00b0
        L_0x0014:
            java.lang.String r1 = "closeInvitationDialog"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 9
            goto L_0x00b0
        L_0x0020:
            java.lang.String r1 = "apstadis"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 11
            goto L_0x00b0
        L_0x002c:
            java.lang.String r1 = "apstacon"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 10
            goto L_0x00b0
        L_0x0038:
            java.lang.String r1 = "siopLevCha"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 12
            goto L_0x00b0
        L_0x0044:
            java.lang.String r1 = "openInvitationDialog"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = r2
            goto L_0x00b0
        L_0x004f:
            java.lang.String r1 = "ssdis"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = r7
            goto L_0x00b0
        L_0x0059:
            java.lang.String r1 = "sscon"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = r8
            goto L_0x00b0
        L_0x0063:
            java.lang.String r1 = "sidis"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 3
            goto L_0x00b0
        L_0x006d:
            java.lang.String r1 = "sicon"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = r4
            goto L_0x00b0
        L_0x0077:
            boolean r1 = r12.equals(r5)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = r3
            goto L_0x00b0
        L_0x007f:
            java.lang.String r1 = "wfdSta"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 13
            goto L_0x00b0
        L_0x008a:
            java.lang.String r1 = "smswon"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 6
            goto L_0x00b0
        L_0x0094:
            boolean r1 = r12.equals(r6)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 5
            goto L_0x00b0
        L_0x009c:
            java.lang.String r1 = "groupexit"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 14
            goto L_0x00b0
        L_0x00a7:
            java.lang.String r1 = "smswoff"
            boolean r1 = r12.equals(r1)     // Catch:{ Exception -> 0x01c8 }
            if (r1 == 0) goto L_0x0012
            r0 = 7
        L_0x00b0:
            java.lang.String r1 = "wlan.p2p.wfdsta"
            java.lang.String r9 = "wlan.p2p.numclient"
            java.lang.String r10 = "wlan.p2p.chkintent"
            switch(r0) {
                case 0: goto L_0x018c;
                case 1: goto L_0x017c;
                case 2: goto L_0x016d;
                case 3: goto L_0x015d;
                case 4: goto L_0x014e;
                case 5: goto L_0x013d;
                case 6: goto L_0x012c;
                case 7: goto L_0x011b;
                case 8: goto L_0x010a;
                case 9: goto L_0x00f9;
                case 10: goto L_0x00e5;
                case 11: goto L_0x00d1;
                case 12: goto L_0x00c4;
                case 13: goto L_0x00bd;
                default: goto L_0x00b9;
            }
        L_0x00b9:
            numofclients = r8     // Catch:{ Exception -> 0x01c8 }
            goto L_0x019b
        L_0x00bd:
            java.lang.String r0 = chkWfdStatus     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r1, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x00c4:
            java.lang.String r0 = "wlan.p2p.temp"
            int r1 = siopLevel     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r1 = java.lang.Integer.toString(r1)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r0, r1)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x00d1:
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            if (r0 <= 0) goto L_0x00da
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            int r0 = r0 - r7
            numofclients = r0     // Catch:{ Exception -> 0x01c8 }
        L_0x00da:
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r9, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x00e5:
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            if (r0 < 0) goto L_0x00ee
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            int r0 = r0 + r7
            numofclients = r0     // Catch:{ Exception -> 0x01c8 }
        L_0x00ee:
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r9, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x00f9:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 & -33
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x010a:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 | 32
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x011b:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 & -17
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x012c:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 | 16
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x013d:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 & -9
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x014e:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 | r2
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x015d:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 & -5
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x016d:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 | r3
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x017c:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 & -3
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x018c:
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            r0 = r0 | r4
            intentValue = r0     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x019b:
            int r0 = numofclients     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r9, r0)     // Catch:{ Exception -> 0x01c8 }
            intentValue = r8     // Catch:{ Exception -> 0x01c8 }
            int r0 = intentValue     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r10, r0)     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = "disconnected"
            chkWfdStatus = r0     // Catch:{ Exception -> 0x01c8 }
            java.lang.String r0 = chkWfdStatus     // Catch:{ Exception -> 0x01c8 }
            android.os.SystemProperties.set(r1, r0)     // Catch:{ Exception -> 0x01c8 }
            android.os.PowerManager r0 = r11.mPowerManager     // Catch:{ Exception -> 0x01c8 }
            boolean r0 = r0.isScreenOn()     // Catch:{ Exception -> 0x01c8 }
            if (r0 == 0) goto L_0x01c4
            r11.setProp(r5)     // Catch:{ Exception -> 0x01c8 }
            goto L_0x01c7
        L_0x01c4:
            r11.setProp(r6)     // Catch:{ Exception -> 0x01c8 }
        L_0x01c7:
            goto L_0x01d0
        L_0x01c8:
            r0 = move-exception
            java.lang.String r1 = "WifiP2pService"
            java.lang.String r2 = "setprop for GOPS is failed."
            android.util.Log.e(r1, r2)
        L_0x01d0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.setProp(java.lang.String):void");
    }

    /* access modifiers changed from: private */
    public long checkTimeNoa(int noa_num, int noa_dur) {
        if (noa_num != 0) {
            if (noa_num == 1) {
                if (mStartTimeForNoa != 0) {
                    mWorkingTimeForNoa += ((System.currentTimeMillis() - mStartTimeForNoa) * ((long) mDurationForNoa)) / 100;
                }
                mStartTimeForNoa = System.currentTimeMillis();
                mDurationForNoa = noa_dur;
                return 0;
            } else if (!(noa_num == 2 || noa_num == 3 || noa_num == 4 || noa_num == 5)) {
                long result = mWorkingTimeForNoa;
                Log.d(TAG, "mWorkingTimeForNoa: " + mWorkingTimeForNoa + " result: " + result);
                mWorkingTimeForNoa = 0;
                return result;
            }
        }
        if (mStartTimeForNoa == 0) {
            return 0;
        }
        mWorkingTimeForNoa += ((System.currentTimeMillis() - mStartTimeForNoa) * ((long) mDurationForNoa)) / 100;
        mStartTimeForNoa = 0;
        mDurationForNoa = 0;
        return 0;
    }

    public boolean isInactiveState() {
        enforceAccessPermission();
        enforceChangePermission();
        return this.mP2pStateMachine.mIsInactiveState;
    }

    public int getWifiP2pState() {
        enforceAccessPermission();
        enforceChangePermission();
        return this.mP2pStateMachine.mP2pState;
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    private int checkConnectivityInternalPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL");
    }

    private int checkLocationHardwarePermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE");
    }

    private void enforceConnectivityInternalOrLocationHardwarePermission() {
        if (checkConnectivityInternalPermission() != 0 && checkLocationHardwarePermission() != 0) {
            enforceConnectivityInternalPermission();
        }
    }

    /* access modifiers changed from: private */
    public void stopIpClient() {
        this.mIpClientStartIndex++;
        IIpClient iIpClient = this.mIpClient;
        if (iIpClient != null) {
            try {
                iIpClient.stop();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
            this.mIpClient = null;
        }
        this.mDhcpResults = null;
    }

    /* access modifiers changed from: private */
    public void startIpClient(String ifname, Handler smHandler) {
        stopIpClient();
        this.mIpClientStartIndex++;
        IpClientUtil.makeIpClient(this.mContext, ifname, new IpClientCallbacksImpl(this.mIpClientStartIndex, smHandler));
    }

    private class IpClientCallbacksImpl extends IpClientCallbacks {
        private final Handler mHandler;
        private final int mStartIndex;

        private IpClientCallbacksImpl(int startIndex, Handler handler) {
            this.mStartIndex = startIndex;
            this.mHandler = handler;
        }

        public void onIpClientCreated(IIpClient ipClient) {
            this.mHandler.post(new Runnable(ipClient) {
                private final /* synthetic */ IIpClient f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WifiP2pServiceImpl.IpClientCallbacksImpl.this.mo5682x36e41d72(this.f$1);
                }
            });
        }

        /* renamed from: lambda$onIpClientCreated$0$WifiP2pServiceImpl$IpClientCallbacksImpl */
        public /* synthetic */ void mo5682x36e41d72(IIpClient ipClient) {
            if (WifiP2pServiceImpl.this.mIpClientStartIndex == this.mStartIndex) {
                IIpClient unused = WifiP2pServiceImpl.this.mIpClient = ipClient;
                try {
                    WifiP2pServiceImpl.this.mIpClient.startProvisioning(new ProvisioningConfiguration.Builder().withoutIpReachabilityMonitor().withPreDhcpAction(30000).withProvisioningTimeoutMs(36000).build().toStableParcelable());
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
        }

        public void onPreDhcpAction() {
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION);
        }

        public void onPostDhcpAction() {
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_POST_DHCP_ACTION);
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_DHCP_RESULTS, dhcpResults);
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS);
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE);
        }
    }

    public Messenger getMessenger(IBinder binder) {
        Messenger messenger;
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            messenger = new Messenger(this.mClientHandler);
            if (mVerboseLoggingEnabled) {
                Log.d(TAG, "getMessenger: uid=" + getCallingUid() + ", binder=" + binder + ", messenger=" + messenger);
            }
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient(binder) {
                private final /* synthetic */ IBinder f$1;

                {
                    this.f$1 = r2;
                }

                public final void binderDied() {
                    WifiP2pServiceImpl.this.lambda$getMessenger$0$WifiP2pServiceImpl(this.f$1);
                }
            };
            try {
                binder.linkToDeath(dr, 0);
                this.mDeathDataByBinder.put(binder, new DeathHandlerData(dr, messenger));
            } catch (RemoteException e) {
                Log.e(TAG, "Error on linkToDeath: e=" + e);
            }
            this.mP2pStateMachine.sendMessage(ENABLE_P2P);
        }
        return messenger;
    }

    public /* synthetic */ void lambda$getMessenger$0$WifiP2pServiceImpl(IBinder binder) {
        if (mVerboseLoggingEnabled) {
            Log.d(TAG, "binderDied: binder=" + binder);
        }
        close(binder);
    }

    public Messenger getP2pStateMachineMessenger() {
        enforceConnectivityInternalOrLocationHardwarePermission();
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mP2pStateMachine.getHandler());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0078, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close(android.os.IBinder r7) {
        /*
            r6 = this;
            r6.enforceAccessPermission()
            r6.enforceChangePermission()
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            java.util.Map<android.os.IBinder, com.android.server.wifi.p2p.WifiP2pServiceImpl$DeathHandlerData> r1 = r6.mDeathDataByBinder     // Catch:{ all -> 0x0079 }
            java.lang.Object r1 = r1.get(r7)     // Catch:{ all -> 0x0079 }
            com.android.server.wifi.p2p.WifiP2pServiceImpl$DeathHandlerData r1 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.DeathHandlerData) r1     // Catch:{ all -> 0x0079 }
            if (r1 != 0) goto L_0x001c
            java.lang.String r2 = "WifiP2pService"
            java.lang.String r3 = "close(): no death recipient for binder"
            android.util.Log.w(r2, r3)     // Catch:{ all -> 0x0079 }
            monitor-exit(r0)     // Catch:{ all -> 0x0079 }
            return
        L_0x001c:
            com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = r6.mP2pStateMachine     // Catch:{ all -> 0x0079 }
            r3 = 143378(0x23012, float:2.00915E-40)
            r4 = 0
            r2.sendMessage(r3, r4, r4, r7)     // Catch:{ all -> 0x0079 }
            android.os.IBinder$DeathRecipient r2 = r1.mDeathRecipient     // Catch:{ all -> 0x0079 }
            r7.unlinkToDeath(r2, r4)     // Catch:{ all -> 0x0079 }
            java.util.Map<android.os.IBinder, com.android.server.wifi.p2p.WifiP2pServiceImpl$DeathHandlerData> r2 = r6.mDeathDataByBinder     // Catch:{ all -> 0x0079 }
            r2.remove(r7)     // Catch:{ all -> 0x0079 }
            android.os.Messenger r2 = r1.mMessenger     // Catch:{ all -> 0x0079 }
            if (r2 == 0) goto L_0x0077
            java.util.Map<android.os.IBinder, com.android.server.wifi.p2p.WifiP2pServiceImpl$DeathHandlerData> r2 = r6.mDeathDataByBinder     // Catch:{ all -> 0x0079 }
            boolean r2 = r2.isEmpty()     // Catch:{ all -> 0x0079 }
            if (r2 == 0) goto L_0x0077
            android.os.Messenger r2 = r1.mMessenger     // Catch:{ RemoteException -> 0x0058 }
            com.android.server.wifi.p2p.WifiP2pServiceImpl$ClientHandler r3 = r6.mClientHandler     // Catch:{ RemoteException -> 0x0058 }
            r4 = 139268(0x22004, float:1.95156E-40)
            android.os.Message r3 = r3.obtainMessage(r4)     // Catch:{ RemoteException -> 0x0058 }
            r2.send(r3)     // Catch:{ RemoteException -> 0x0058 }
            android.os.Messenger r2 = r1.mMessenger     // Catch:{ RemoteException -> 0x0058 }
            com.android.server.wifi.p2p.WifiP2pServiceImpl$ClientHandler r3 = r6.mClientHandler     // Catch:{ RemoteException -> 0x0058 }
            r4 = 139280(0x22010, float:1.95173E-40)
            android.os.Message r3 = r3.obtainMessage(r4)     // Catch:{ RemoteException -> 0x0058 }
            r2.send(r3)     // Catch:{ RemoteException -> 0x0058 }
            goto L_0x006f
        L_0x0058:
            r2 = move-exception
            java.lang.String r3 = "WifiP2pService"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0079 }
            r4.<init>()     // Catch:{ all -> 0x0079 }
            java.lang.String r5 = "close: Failed sending clean-up commands: e="
            r4.append(r5)     // Catch:{ all -> 0x0079 }
            r4.append(r2)     // Catch:{ all -> 0x0079 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0079 }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x0079 }
        L_0x006f:
            com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = r6.mP2pStateMachine     // Catch:{ all -> 0x0079 }
            r3 = 143377(0x23011, float:2.00914E-40)
            r2.sendMessage(r3)     // Catch:{ all -> 0x0079 }
        L_0x0077:
            monitor-exit(r0)     // Catch:{ all -> 0x0079 }
            return
        L_0x0079:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0079 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.close(android.os.IBinder):void");
    }

    public void setMiracastMode(int mode) {
        enforceConnectivityInternalPermission();
        checkConfigureWifiDisplayPermission();
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, mode);
    }

    public void checkConfigureWifiDisplayPermission() {
        if (!getWfdPermission(Binder.getCallingUid())) {
            throw new SecurityException("Wifi Display Permission denied for uid = " + Binder.getCallingUid());
        }
    }

    /* access modifiers changed from: private */
    public boolean getWfdPermission(int uid) {
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        return this.mWifiInjector.getWifiPermissionsWrapper().getUidPermission("android.permission.CONFIGURE_WIFI_DISPLAY", uid) != -1;
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiP2pService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        this.mP2pStateMachine.dump(fd, pw, args);
        pw.println("mAutonomousGroup " + this.mAutonomousGroup);
        pw.println("mJoinExistingGroup " + this.mJoinExistingGroup);
        pw.println("mDiscoveryStarted " + this.mDiscoveryStarted);
        pw.println("mNetworkInfo " + this.mNetworkInfo);
        pw.println("mTemporarilyDisconnectedWifi " + this.mTemporarilyDisconnectedWifi);
        pw.println("mServiceDiscReqId " + this.mServiceDiscReqId);
        pw.println("mDeathDataByBinder " + this.mDeathDataByBinder);
        pw.println("mClientInfoList " + this.mClientInfoList.size());
        pw.println();
        IIpClient ipClient = this.mIpClient;
        if (ipClient != null) {
            pw.println("mIpClient:");
            IpClientUtil.dumpIpClient(ipClient, fd, pw, args);
        }
    }

    private class P2pBigDataLog {
        private static final String KEY_CNM = "WCVN";
        private static final String KEY_CONN_PERIOD = "CONP";
        private static final String KEY_CONN_RECEIVED = "CSOR";
        private static final String KEY_DISCONNECT_REASON = "DISR";
        private static final String KEY_DRV_VER = "WDRV";
        private static final String KEY_FREQ = "FREQ";
        private static final String KEY_FW_VER = "WFWV";
        private static final String KEY_GROUP_FORMATION_RESULT = "GRFR";
        private static final String KEY_GROUP_NEGO = "GRNE";
        private static final String KEY_IS_GO = "ISGO";
        private static final String KEY_NOA_PERIOD = "NOAP";
        private static final String KEY_NUM_CLIENT = "NOCL";
        private static final String KEY_PEER_DEVICE_TYPE = "DEVT";
        private static final String KEY_PEER_GO_INTENT = "PINT";
        private static final String KEY_PEER_MANUFACTURER = "MANU";
        private static final String KEY_PERSISTENT = "PSTC";
        private static final String KEY_PKG_NAME = "PKGN";
        private static final String PATH_OF_WIFIVER_INFO = "/data/misc/conn/.wifiver.info";
        private static final String TAG = "P2pBigDataLog";
        private static final String WIFI_VER_PREFIX_BRCM = "HD_ver";
        private static final String WIFI_VER_PREFIX_MAVL = "received";
        private static final String WIFI_VER_PREFIX_QCA = "FW:";
        private static final String WIFI_VER_PREFIX_QCOM = "CNSS";
        private static final String WIFI_VER_PREFIX_SPRTRM = "is 0x";
        private final String APP_ID = "android.net.wifi.p2p";
        public String mChipsetName;
        public String mConnReceived;
        public String mConnectionPeriod;
        public String mDisconnectReason;
        public String mDriverVer;
        public String mFirmwareVer;
        public String mFreq;
        public String mGroupNego;
        public String mIsGroupOwner;
        public String mNoaPeriod;
        public String mNumClient;
        public String mPeerDevType;
        public String mPeerGOIntent;
        public String mPeerManufacturer;
        public String mPersistent;
        public String mPkgName;
        public String mResult;

        public P2pBigDataLog() {
        }

        public void initialize() {
            this.mIsGroupOwner = "";
            this.mNumClient = "";
            this.mFreq = "";
            this.mConnectionPeriod = "";
            this.mNoaPeriod = "";
            this.mPeerManufacturer = "";
            this.mPeerDevType = "";
            this.mDisconnectReason = "";
            this.mPkgName = "";
            this.mConnReceived = "";
            this.mPersistent = "";
            this.mGroupNego = "";
            this.mPeerGOIntent = "";
            this.mResult = "";
        }

        public String getJsonFormat(String feature) {
            if (feature == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if ("WDCL".equals(feature)) {
                sb.append(convertToQuotedString(KEY_FW_VER) + ":");
                sb.append(convertToQuotedString(this.mFirmwareVer) + ",");
                sb.append(convertToQuotedString(KEY_DRV_VER) + ":");
                sb.append(convertToQuotedString(this.mDriverVer) + ",");
                sb.append(convertToQuotedString(KEY_CNM) + ":");
                sb.append(convertToQuotedString(this.mChipsetName) + ",");
                sb.append(convertToQuotedString(KEY_IS_GO) + ":");
                sb.append(convertToQuotedString(this.mIsGroupOwner) + ",");
                sb.append(convertToQuotedString(KEY_NUM_CLIENT) + ":");
                sb.append(convertToQuotedString(this.mNumClient) + ",");
                sb.append(convertToQuotedString(KEY_FREQ) + ":");
                sb.append(convertToQuotedString(this.mFreq) + ",");
                sb.append(convertToQuotedString(KEY_PKG_NAME) + ":");
                sb.append(convertToQuotedString(this.mPkgName) + ",");
                sb.append(convertToQuotedString(KEY_CONN_PERIOD) + ":");
                sb.append(convertToQuotedString(this.mConnectionPeriod) + ",");
                sb.append(convertToQuotedString(KEY_NOA_PERIOD) + ":");
                sb.append(convertToQuotedString(this.mNoaPeriod) + ",");
                sb.append(convertToQuotedString(KEY_PEER_MANUFACTURER) + ":");
                sb.append(convertToQuotedString(this.mPeerManufacturer) + ",");
                sb.append(convertToQuotedString(KEY_PEER_DEVICE_TYPE) + ":");
                sb.append(convertToQuotedString(this.mPeerDevType) + ",");
                sb.append(convertToQuotedString(KEY_DISCONNECT_REASON) + ":");
                sb.append(convertToQuotedString(this.mDisconnectReason));
            } else if ("WDGF".equals(feature)) {
                sb.append(convertToQuotedString(KEY_FW_VER) + ":");
                sb.append(convertToQuotedString(this.mFirmwareVer) + ",");
                sb.append(convertToQuotedString(KEY_DRV_VER) + ":");
                sb.append(convertToQuotedString(this.mDriverVer) + ",");
                sb.append(convertToQuotedString(KEY_CNM) + ":");
                sb.append(convertToQuotedString(this.mChipsetName) + ",");
                sb.append(convertToQuotedString(KEY_PKG_NAME) + ":");
                sb.append(convertToQuotedString(this.mPkgName) + ",");
                sb.append(convertToQuotedString(KEY_CONN_RECEIVED) + ":");
                sb.append(convertToQuotedString(this.mConnReceived) + ",");
                sb.append(convertToQuotedString(KEY_PERSISTENT) + ":");
                sb.append(convertToQuotedString(this.mPersistent) + ",");
                sb.append(convertToQuotedString(KEY_GROUP_NEGO) + ":");
                sb.append(convertToQuotedString(this.mGroupNego) + ",");
                sb.append(convertToQuotedString(KEY_PEER_MANUFACTURER) + ":");
                sb.append(convertToQuotedString(this.mPeerManufacturer) + ",");
                sb.append(convertToQuotedString(KEY_PEER_DEVICE_TYPE) + ":");
                sb.append(convertToQuotedString(this.mPeerDevType) + ",");
                sb.append(convertToQuotedString(KEY_PEER_GO_INTENT) + ":");
                sb.append(convertToQuotedString(this.mPeerGOIntent) + ",");
                sb.append(convertToQuotedString(KEY_GROUP_FORMATION_RESULT) + ":");
                sb.append(convertToQuotedString(this.mResult));
            }
            return sb.toString();
        }

        public boolean parseData(String feature, String data) {
            if (feature == null || data == null) {
                return false;
            }
            String[] array = data.split("\\s+");
            if ("WDCL".equals(feature)) {
                if (array.length != 9) {
                    Log.d(TAG, "Wrong parseData for WDCL, length : " + array.length);
                    return false;
                }
                int index = 0 + 1;
                this.mIsGroupOwner = array[0];
                int index2 = index + 1;
                this.mNumClient = array[index];
                int index3 = index2 + 1;
                this.mFreq = array[index2];
                int index4 = index3 + 1;
                this.mPkgName = array[index3];
                int index5 = index4 + 1;
                this.mConnectionPeriod = array[index4];
                int index6 = index5 + 1;
                this.mNoaPeriod = array[index5];
                int index7 = index6 + 1;
                this.mPeerManufacturer = array[index6];
                int index8 = index7 + 1;
                this.mPeerDevType = array[index7];
                int i = index8 + 1;
                this.mDisconnectReason = array[index8];
                return true;
            } else if (!"WDGF".equals(feature)) {
                return false;
            } else {
                if (array.length != 8) {
                    Log.d(TAG, "Wrong parseData for WDGF, length : " + array.length);
                    return false;
                }
                int index9 = 0 + 1;
                this.mPkgName = array[0];
                int index10 = index9 + 1;
                this.mConnReceived = array[index9];
                int index11 = index10 + 1;
                this.mPersistent = array[index10];
                int index12 = index11 + 1;
                this.mGroupNego = array[index11];
                int index13 = index12 + 1;
                this.mPeerManufacturer = array[index12];
                int index14 = index13 + 1;
                this.mPeerDevType = array[index13];
                int index15 = index14 + 1;
                this.mPeerGOIntent = array[index14];
                int i2 = index15 + 1;
                this.mResult = array[index15];
                return true;
            }
        }

        /*  JADX ERROR: NullPointerException in pass: CodeShrinkVisitor
            java.lang.NullPointerException
            	at jadx.core.dex.instructions.args.InsnArg.wrapInstruction(InsnArg.java:118)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.inline(CodeShrinkVisitor.java:146)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkBlock(CodeShrinkVisitor.java:71)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkMethod(CodeShrinkVisitor.java:43)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.visit(CodeShrinkVisitor.java:35)
            */
        private java.lang.String getFirmwareVer() {
            /*
                r14 = this;
                java.lang.String r0 = "driver version is "
                java.lang.String r1 = "FW"
                java.lang.String r2 = "version"
                java.lang.String r3 = "CNSS"
                java.lang.String r4 = "File Close error"
                r5 = 0
                r6 = 0
                r7 = 0
                r8 = 0
                r9 = 0
                r10 = 0
                java.io.FileReader r11 = new java.io.FileReader     // Catch:{ IOException -> 0x0162 }
                java.lang.String r12 = "/data/misc/conn/.wifiver.info"
                r11.<init>(r12)     // Catch:{ IOException -> 0x0162 }
                r5 = r11
                java.io.BufferedReader r11 = new java.io.BufferedReader     // Catch:{ IOException -> 0x0162 }
                r11.<init>(r5)     // Catch:{ IOException -> 0x0162 }
                r6 = r11
                java.lang.String r11 = r6.readLine()     // Catch:{ IOException -> 0x0162 }
                r7 = r11
                if (r7 == 0) goto L_0x0152
                java.lang.String r11 = "HD_ver"
                boolean r11 = r7.contains(r11)     // Catch:{ IOException -> 0x0162 }
                r12 = -1
                java.lang.String r13 = "NG"
                if (r11 == 0) goto L_0x0070
                java.lang.String r0 = r6.readLine()     // Catch:{ IOException -> 0x0162 }
                r7 = r0
                if (r7 == 0) goto L_0x0061
                int r0 = r7.indexOf(r2)     // Catch:{ IOException -> 0x0162 }
                r9 = r0
                if (r9 == r12) goto L_0x005d
                int r0 = r2.length()     // Catch:{ IOException -> 0x0162 }
                int r0 = r0 + r9
                int r9 = r0 + 1
                java.lang.String r0 = " "
                int r0 = r7.indexOf(r0, r9)     // Catch:{ IOException -> 0x0162 }
                r10 = r0
                java.lang.String r0 = r7.substring(r9, r10)     // Catch:{ IOException -> 0x0162 }
                r6.close()     // Catch:{ IOException -> 0x005b }
                r5.close()     // Catch:{ IOException -> 0x005b }
                return r0
            L_0x005b:
                r1 = move-exception
                return r4
            L_0x005d:
                r0 = r13
                r8 = r0
                goto L_0x0131
            L_0x0061:
                r8 = r13
                java.lang.String r0 = "file was damaged, it need check !"
                r6.close()     // Catch:{ IOException -> 0x006e }
                r5.close()     // Catch:{ IOException -> 0x006e }
                return r0
            L_0x006e:
                r0 = move-exception
                return r4
            L_0x0070:
                boolean r2 = r7.contains(r3)     // Catch:{ IOException -> 0x0162 }
                if (r2 == 0) goto L_0x0099
                int r0 = r7.indexOf(r3)     // Catch:{ IOException -> 0x0162 }
                r9 = r0
                if (r9 == r12) goto L_0x0095
                java.lang.String r0 = "CNSS-PR-"
                int r0 = r0.length()     // Catch:{ IOException -> 0x0162 }
                int r9 = r9 + r0
                java.lang.String r0 = r7.substring(r9)     // Catch:{ IOException -> 0x0162 }
                r6.close()     // Catch:{ IOException -> 0x0093 }
                r5.close()     // Catch:{ IOException -> 0x0093 }
                return r0
            L_0x0093:
                r1 = move-exception
                return r4
            L_0x0095:
                r0 = r13
                r8 = r0
                goto L_0x0131
            L_0x0099:
                java.lang.String r2 = "FW:"
                boolean r2 = r7.contains(r2)     // Catch:{ IOException -> 0x0162 }
                if (r2 == 0) goto L_0x00cc
                int r0 = r7.indexOf(r1)     // Catch:{ IOException -> 0x0162 }
                r9 = r0
                if (r9 == r12) goto L_0x00c8
                int r0 = r1.length()     // Catch:{ IOException -> 0x0162 }
                int r0 = r0 + r9
                int r9 = r0 + 1
                java.lang.String r0 = "HW"
                int r0 = r7.indexOf(r0)     // Catch:{ IOException -> 0x0162 }
                int r10 = r0 + -2
                java.lang.String r0 = r7.substring(r9, r10)     // Catch:{ IOException -> 0x0162 }
                r6.close()     // Catch:{ IOException -> 0x00c6 }
                r5.close()     // Catch:{ IOException -> 0x00c6 }
                return r0
            L_0x00c6:
                r1 = move-exception
                return r4
            L_0x00c8:
                r0 = r13
                r8 = r0
                goto L_0x0131
            L_0x00cc:
                java.lang.String r1 = "received"
                boolean r1 = r7.contains(r1)     // Catch:{ IOException -> 0x0162 }
                r2 = 0
                if (r1 == 0) goto L_0x00ff
                java.lang.String r0 = ".p"
                int r0 = r7.indexOf(r0)     // Catch:{ IOException -> 0x0162 }
                int r9 = r0 + 1
                java.lang.String r0 = r7.substring(r9)     // Catch:{ IOException -> 0x0162 }
                r7 = r0
                if (r9 == r12) goto L_0x00fc
                java.lang.String r0 = "-"
                int r0 = r7.indexOf(r0)     // Catch:{ IOException -> 0x0162 }
                r10 = r0
                java.lang.String r0 = r7.substring(r2, r10)     // Catch:{ IOException -> 0x0162 }
                r6.close()     // Catch:{ IOException -> 0x00fa }
                r5.close()     // Catch:{ IOException -> 0x00fa }
                return r0
            L_0x00fa:
                r1 = move-exception
                return r4
            L_0x00fc:
                r0 = r13
                r8 = r0
                goto L_0x0131
            L_0x00ff:
                java.lang.String r1 = "is 0x"
                boolean r1 = r7.contains(r1)     // Catch:{ IOException -> 0x0162 }
                if (r1 == 0) goto L_0x012f
                int r1 = r7.indexOf(r0)     // Catch:{ IOException -> 0x0162 }
                int r0 = r0.length()     // Catch:{ IOException -> 0x0162 }
                int r1 = r1 + r0
                int r9 = r1 + 1
                if (r9 == r12) goto L_0x012c
                java.lang.String r0 = "] ["
                int r0 = r7.indexOf(r0)     // Catch:{ IOException -> 0x0162 }
                r10 = r0
                java.lang.String r0 = r7.substring(r2, r10)     // Catch:{ IOException -> 0x0162 }
                r6.close()     // Catch:{ IOException -> 0x012a }
                r5.close()     // Catch:{ IOException -> 0x012a }
                return r0
            L_0x012a:
                r1 = move-exception
                return r4
            L_0x012c:
                r0 = r13
                r8 = r0
                goto L_0x0131
            L_0x012f:
                r0 = r13
                r8 = r0
            L_0x0131:
                boolean r0 = r13.equals(r8)     // Catch:{ IOException -> 0x0162 }
                if (r0 == 0) goto L_0x0144
                r6.close()     // Catch:{ IOException -> 0x0142 }
                r5.close()     // Catch:{ IOException -> 0x0142 }
                return r7
            L_0x0142:
                r0 = move-exception
                return r4
            L_0x0144:
                r6.close()     // Catch:{ IOException -> 0x0150 }
                r5.close()     // Catch:{ IOException -> 0x0150 }
                java.lang.String r0 = "error"
                return r0
            L_0x0150:
                r0 = move-exception
                return r4
            L_0x0152:
                java.lang.String r0 = "file is null .. !"
                r6.close()     // Catch:{ IOException -> 0x015e }
                r5.close()     // Catch:{ IOException -> 0x015e }
                return r0
            L_0x015e:
                r0 = move-exception
                return r4
            L_0x0160:
                r0 = move-exception
                goto L_0x0176
            L_0x0162:
                r0 = move-exception
                java.lang.String r1 = "/data/misc/conn/.wifiver.info doesn't exist or there are something wrong on handling it"
                if (r6 == 0) goto L_0x016d
                r6.close()     // Catch:{ IOException -> 0x016b }
                goto L_0x016d
            L_0x016b:
                r1 = move-exception
                goto L_0x0173
            L_0x016d:
                if (r5 == 0) goto L_0x0174
                r5.close()     // Catch:{ IOException -> 0x016b }
                goto L_0x0174
            L_0x0173:
                return r4
            L_0x0174:
                return r1
            L_0x0176:
                if (r6 == 0) goto L_0x017e
                r6.close()     // Catch:{ IOException -> 0x017c }
                goto L_0x017e
            L_0x017c:
                r0 = move-exception
                goto L_0x0184
            L_0x017e:
                if (r5 == 0) goto L_0x0185
                r5.close()     // Catch:{ IOException -> 0x017c }
                goto L_0x0185
            L_0x0184:
                return r4
            L_0x0185:
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pBigDataLog.getFirmwareVer():java.lang.String");
        }

        /*  JADX ERROR: NullPointerException in pass: CodeShrinkVisitor
            java.lang.NullPointerException
            	at jadx.core.dex.instructions.args.InsnArg.wrapInstruction(InsnArg.java:118)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.inline(CodeShrinkVisitor.java:146)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkBlock(CodeShrinkVisitor.java:71)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.shrinkMethod(CodeShrinkVisitor.java:43)
            	at jadx.core.dex.visitors.shrink.CodeShrinkVisitor.visit(CodeShrinkVisitor.java:35)
            */
        private java.lang.String getDriverVer() {
            /*
                r16 = this;
                java.lang.String r0 = "cp version is "
                java.lang.String r1 = "-GPL"
                java.lang.String r2 = "SW"
                java.lang.String r3 = "v"
                java.lang.String r4 = "HD_ver:"
                java.lang.String r5 = "File Close error"
                r6 = 0
                r7 = 0
                r8 = 0
                r9 = 0
                r10 = 0
                r11 = 0
                r12 = 0
                java.io.FileReader r13 = new java.io.FileReader     // Catch:{ IOException -> 0x0151 }
                java.lang.String r14 = "/data/misc/conn/.wifiver.info"
                r13.<init>(r14)     // Catch:{ IOException -> 0x0151 }
                r6 = r13
                java.io.BufferedReader r13 = new java.io.BufferedReader     // Catch:{ IOException -> 0x0151 }
                r13.<init>(r6)     // Catch:{ IOException -> 0x0151 }
                r7 = r13
                java.lang.String r13 = r7.readLine()     // Catch:{ IOException -> 0x0151 }
                r8 = r13
                if (r8 == 0) goto L_0x0141
                java.lang.String r13 = "HD_ver"
                boolean r13 = r8.contains(r13)     // Catch:{ IOException -> 0x0151 }
                r14 = -1
                java.lang.String r15 = "NG"
                if (r13 == 0) goto L_0x005f
                int r0 = r8.indexOf(r4)     // Catch:{ IOException -> 0x0151 }
                r10 = r0
                if (r10 == r14) goto L_0x005b
                int r0 = r4.length()     // Catch:{ IOException -> 0x0151 }
                int r0 = r0 + r10
                int r10 = r0 + 1
                java.lang.String r0 = " "
                int r0 = r8.indexOf(r0, r10)     // Catch:{ IOException -> 0x0151 }
                r11 = r0
                java.lang.String r0 = r8.substring(r10, r11)     // Catch:{ IOException -> 0x0151 }
                r1 = r0
                r7.close()     // Catch:{ IOException -> 0x0059 }
                r6.close()     // Catch:{ IOException -> 0x0059 }
                return r1
            L_0x0059:
                r0 = move-exception
                return r5
            L_0x005b:
                r0 = r15
                r9 = r0
                goto L_0x0120
            L_0x005f:
                java.lang.String r4 = "CNSS"
                boolean r4 = r8.contains(r4)     // Catch:{ IOException -> 0x0151 }
                if (r4 == 0) goto L_0x0090
                int r0 = r8.indexOf(r3)     // Catch:{ IOException -> 0x0151 }
                r10 = r0
                if (r10 == r14) goto L_0x008c
                int r0 = r3.length()     // Catch:{ IOException -> 0x0151 }
                int r10 = r10 + r0
                java.lang.String r0 = " CNSS"
                int r0 = r8.indexOf(r0)     // Catch:{ IOException -> 0x0151 }
                r11 = r0
                java.lang.String r0 = r8.substring(r10, r11)     // Catch:{ IOException -> 0x0151 }
                r1 = r0
                r7.close()     // Catch:{ IOException -> 0x008a }
                r6.close()     // Catch:{ IOException -> 0x008a }
                return r1
            L_0x008a:
                r0 = move-exception
                return r5
            L_0x008c:
                r0 = r15
                r9 = r0
                goto L_0x0120
            L_0x0090:
                java.lang.String r3 = "FW:"
                boolean r3 = r8.contains(r3)     // Catch:{ IOException -> 0x0151 }
                if (r3 == 0) goto L_0x00c3
                int r0 = r8.indexOf(r2)     // Catch:{ IOException -> 0x0151 }
                r10 = r0
                if (r10 == r14) goto L_0x00c0
                int r0 = r2.length()     // Catch:{ IOException -> 0x0151 }
                int r0 = r0 + r10
                int r10 = r0 + 1
                java.lang.String r0 = "FW"
                int r0 = r8.indexOf(r0)     // Catch:{ IOException -> 0x0151 }
                int r11 = r0 + -2
                java.lang.String r0 = r8.substring(r10, r11)     // Catch:{ IOException -> 0x0151 }
                r1 = r0
                r7.close()     // Catch:{ IOException -> 0x00be }
                r6.close()     // Catch:{ IOException -> 0x00be }
                return r1
            L_0x00be:
                r0 = move-exception
                return r5
            L_0x00c0:
                r0 = r15
                r9 = r0
                goto L_0x0120
            L_0x00c3:
                java.lang.String r2 = "received"
                boolean r2 = r8.contains(r2)     // Catch:{ IOException -> 0x0151 }
                if (r2 == 0) goto L_0x00ed
                int r0 = r8.indexOf(r1)     // Catch:{ IOException -> 0x0151 }
                int r10 = r0 + -4
                if (r10 == r14) goto L_0x00ea
                int r0 = r8.indexOf(r1)     // Catch:{ IOException -> 0x0151 }
                r11 = r0
                java.lang.String r0 = r8.substring(r10, r11)     // Catch:{ IOException -> 0x0151 }
                r1 = r0
                r7.close()     // Catch:{ IOException -> 0x00e8 }
                r6.close()     // Catch:{ IOException -> 0x00e8 }
                return r1
            L_0x00e8:
                r0 = move-exception
                return r5
            L_0x00ea:
                r0 = r15
                r9 = r0
                goto L_0x0120
            L_0x00ed:
                java.lang.String r1 = "is 0x"
                boolean r1 = r8.contains(r1)     // Catch:{ IOException -> 0x0151 }
                if (r1 == 0) goto L_0x011e
                int r1 = r8.indexOf(r0)     // Catch:{ IOException -> 0x0151 }
                int r0 = r0.length()     // Catch:{ IOException -> 0x0151 }
                int r10 = r1 + r0
                if (r10 == r14) goto L_0x011b
                java.lang.String r0 = "date"
                int r0 = r8.indexOf(r0)     // Catch:{ IOException -> 0x0151 }
                int r11 = r0 + -2
                java.lang.String r0 = r8.substring(r10, r11)     // Catch:{ IOException -> 0x0151 }
                r1 = r0
                r7.close()     // Catch:{ IOException -> 0x0119 }
                r6.close()     // Catch:{ IOException -> 0x0119 }
                return r1
            L_0x0119:
                r0 = move-exception
                return r5
            L_0x011b:
                r0 = r15
                r9 = r0
                goto L_0x0120
            L_0x011e:
                r0 = r15
                r9 = r0
            L_0x0120:
                boolean r0 = r15.equals(r9)     // Catch:{ IOException -> 0x0151 }
                if (r0 == 0) goto L_0x0133
                r7.close()     // Catch:{ IOException -> 0x0131 }
                r6.close()     // Catch:{ IOException -> 0x0131 }
                return r8
            L_0x0131:
                r0 = move-exception
                return r5
            L_0x0133:
                r7.close()     // Catch:{ IOException -> 0x013f }
                r6.close()     // Catch:{ IOException -> 0x013f }
                java.lang.String r0 = "error"
                return r0
            L_0x013f:
                r0 = move-exception
                return r5
            L_0x0141:
                java.lang.String r0 = "file is null .. !"
                r7.close()     // Catch:{ IOException -> 0x014d }
                r6.close()     // Catch:{ IOException -> 0x014d }
                return r0
            L_0x014d:
                r0 = move-exception
                return r5
            L_0x014f:
                r0 = move-exception
                goto L_0x0166
            L_0x0151:
                r0 = move-exception
                r1 = r0
                java.lang.String r0 = "/data/misc/conn/.wifiver.info doesn't exist or there are something wrong on handling it"
                if (r7 == 0) goto L_0x015d
                r7.close()     // Catch:{ IOException -> 0x015b }
                goto L_0x015d
            L_0x015b:
                r0 = move-exception
                goto L_0x0163
            L_0x015d:
                if (r6 == 0) goto L_0x0164
                r6.close()     // Catch:{ IOException -> 0x015b }
                goto L_0x0164
            L_0x0163:
                return r5
            L_0x0164:
                return r0
            L_0x0166:
                if (r7 == 0) goto L_0x016e
                r7.close()     // Catch:{ IOException -> 0x016c }
                goto L_0x016e
            L_0x016c:
                r0 = move-exception
                goto L_0x0174
            L_0x016e:
                if (r6 == 0) goto L_0x0175
                r6.close()     // Catch:{ IOException -> 0x016c }
                goto L_0x0175
            L_0x0174:
                return r5
            L_0x0175:
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pBigDataLog.getDriverVer():java.lang.String");
        }

        private String getChipsetName() {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(PATH_OF_WIFIVER_INFO);
                br = new BufferedReader(fr);
                String verString = br.readLine();
                if (verString == null) {
                    try {
                        br.close();
                        fr.close();
                        return "file is null .. !";
                    } catch (IOException e) {
                        return "File Close error";
                    }
                } else if (verString.contains(WIFI_VER_PREFIX_BRCM)) {
                    try {
                        br.close();
                        fr.close();
                        return "1";
                    } catch (IOException e2) {
                        return "File Close error";
                    }
                } else if (verString.contains(WIFI_VER_PREFIX_QCOM)) {
                    try {
                        br.close();
                        fr.close();
                        return "2";
                    } catch (IOException e3) {
                        return "File Close error";
                    }
                } else if (verString.contains(WIFI_VER_PREFIX_QCA)) {
                    try {
                        br.close();
                        fr.close();
                        return "3";
                    } catch (IOException e4) {
                        return "File Close error";
                    }
                } else if (verString.contains(WIFI_VER_PREFIX_MAVL)) {
                    try {
                        br.close();
                        fr.close();
                        return "4";
                    } catch (IOException e5) {
                        return "File Close error";
                    }
                } else if (verString.contains(WIFI_VER_PREFIX_SPRTRM)) {
                    try {
                        br.close();
                        fr.close();
                        return "5";
                    } catch (IOException e6) {
                        return "File Close error";
                    }
                } else if ("NG".equals("NG")) {
                    String str = "Unknown String format..Full string is " + verString;
                    try {
                        br.close();
                        fr.close();
                        return str;
                    } catch (IOException e7) {
                        return "File Close error";
                    }
                } else {
                    try {
                        br.close();
                        fr.close();
                        return "error";
                    } catch (IOException e8) {
                        return "File Close error";
                    }
                }
            } catch (IOException e9) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e10) {
                        return "File Close error";
                    }
                }
                if (fr != null) {
                    fr.close();
                }
                return "/data/misc/conn/.wifiver.info doesn't exist or there are something wrong on handling it";
            }
        }

        private String convertToQuotedString(String string) {
            return "\"" + string + "\"";
        }

        private String removeDoubleQuotes(String string) {
            if (string == null) {
                return null;
            }
            int length = string.length();
            if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
                return string.substring(1, length - 1);
            }
            return string;
        }
    }

    private class P2pStateMachine extends StateMachine {
        private final int P2P_GO_OPER_FREQ = -999;
        private String filterMaskRTCP = "0xffff000000000000000000ff00000000000000000000000000000000000000ff";
        private String filterMaskRTSPDst = "0xffff000000000000000000ff000000000000000000000000ffff";
        private String filterMaskRTSPSrc = "0xffff000000000000000000ff00000000000000000000ffff";
        private String filterMaskSSDP = "0xffff000000000000000000ff000000000000000000000000ffff";
        private String filterOffset = "12";
        private String filterRTCP = "0x08000000000000000000001100000000000000000000000000000000000000c9";
        private String filterRTSPDst = "0x0800000000000000000000060000000000000000000000001c44";
        private String filterRTSPSrc = "0x080000000000000000000006000000000000000000001c44";
        private String filterSSDP = "0x080000000000000000000011000000000000000000000000076c";
        /* access modifiers changed from: private */
        public String mConnectedDevAddr = null;
        /* access modifiers changed from: private */
        public String mConnectedDevIntfAddr = null;
        private DefaultState mDefaultState = new DefaultState();
        /* access modifiers changed from: private */
        public FrequencyConflictState mFrequencyConflictState = new FrequencyConflictState();
        /* access modifiers changed from: private */
        public WifiP2pGroup mGroup;
        private WifiP2pGroup mGroupBackup;
        /* access modifiers changed from: private */
        public GroupCreatedState mGroupCreatedState = new GroupCreatedState();
        /* access modifiers changed from: private */
        public GroupCreatingState mGroupCreatingState = new GroupCreatingState();
        /* access modifiers changed from: private */
        public GroupNegotiationState mGroupNegotiationState = new GroupNegotiationState();
        /* access modifiers changed from: private */
        public final WifiP2pGroupList mGroups = new WifiP2pGroupList((WifiP2pGroupList) null, new WifiP2pGroupList.GroupDeleteListener() {
            public void onDeleteGroup(int netId) {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                    p2pStateMachine.logd("called onDeleteGroup() netId=" + netId);
                }
                P2pStateMachine.this.mWifiNative.removeP2pNetwork(netId);
                P2pStateMachine.this.mWifiNative.saveConfig();
                P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
            }
        });
        private List<String> mHistoricalDumpLogs = new ArrayList();
        /* access modifiers changed from: private */
        public InactiveState mInactiveState = new InactiveState();
        /* access modifiers changed from: private */
        public String mInterfaceName;
        /* access modifiers changed from: private */
        public boolean mIsGotoJoinState = false;
        private boolean mIsHalInterfaceAvailable = false;
        /* access modifiers changed from: private */
        public boolean mIsInactiveState = false;
        /* access modifiers changed from: private */
        public boolean mIsWifiEnabled = false;
        /* access modifiers changed from: private */
        public NfcProvisionState mNfcProvisionState = new NfcProvisionState();
        /* access modifiers changed from: private */
        public OngoingGroupRemovalState mOngoingGroupRemovalState = new OngoingGroupRemovalState();
        /* access modifiers changed from: private */
        public P2pDisabledState mP2pDisabledState = new P2pDisabledState();
        /* access modifiers changed from: private */
        public P2pDisablingState mP2pDisablingState = new P2pDisablingState();
        private P2pEnabledState mP2pEnabledState = new P2pEnabledState();
        /* access modifiers changed from: private */
        public P2pEnablingState mP2pEnablingState = new P2pEnablingState();
        private P2pNotSupportedState mP2pNotSupportedState = new P2pNotSupportedState();
        /* access modifiers changed from: private */
        public int mP2pState = 1;
        /* access modifiers changed from: private */
        public WifiP2pStaticIpConfig mP2pStaticIpConfig;
        /* access modifiers changed from: private */
        public final WifiP2pDeviceList mPeers = new WifiP2pDeviceList();
        /* access modifiers changed from: private */
        public final WifiP2pDeviceList mPeersLostDuringConnection = new WifiP2pDeviceList();
        /* access modifiers changed from: private */
        public ProvisionDiscoveryState mProvisionDiscoveryState = new ProvisionDiscoveryState();
        /* access modifiers changed from: private */
        public boolean mRequestNfcCalled = false;
        private WifiP2pGroup mSavedP2pGroup;
        /* access modifiers changed from: private */
        public WifiP2pConfig mSavedPeerConfig = new WifiP2pConfig();
        /* access modifiers changed from: private */
        public WifiP2pDevice mSavedProvDiscDevice;
        /* access modifiers changed from: private */
        public int mSelectP2pConfigIndex;
        /* access modifiers changed from: private */
        public WifiP2pConfigList mSelectP2pConfigList;
        /* access modifiers changed from: private */
        public String mSelectedP2pGroupAddress;
        /* access modifiers changed from: private */
        public UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState = new UserAuthorizingInviteRequestState();
        /* access modifiers changed from: private */
        public UserAuthorizingJoinState mUserAuthorizingJoinState = new UserAuthorizingJoinState();
        /* access modifiers changed from: private */
        public UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState = new UserAuthorizingNegotiationRequestState();
        private WaitForUserActionState mWaitForUserActionState = new WaitForUserActionState();
        /* access modifiers changed from: private */
        public WaitForWifiDisableState mWaitForWifiDisableState = new WaitForWifiDisableState();
        /* access modifiers changed from: private */
        public String mWifiInterface;
        /* access modifiers changed from: private */
        public WifiNative mWifiLegacyNative = WifiInjector.getInstance().getWifiNative();
        /* access modifiers changed from: private */
        public WifiP2pMonitor mWifiMonitor = WifiP2pServiceImpl.this.mWifiInjector.getWifiP2pMonitor();
        /* access modifiers changed from: private */
        public WifiP2pNative mWifiNative = WifiP2pServiceImpl.this.mWifiInjector.getWifiP2pNative();
        /* access modifiers changed from: private */
        public final WifiP2pInfo mWifiP2pInfo = new WifiP2pInfo();

        static /* synthetic */ int access$6808(P2pStateMachine x0) {
            int i = x0.mSelectP2pConfigIndex;
            x0.mSelectP2pConfigIndex = i + 1;
            return i;
        }

        P2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(name, looper);
            addState(this.mDefaultState);
            addState(this.mP2pNotSupportedState, this.mDefaultState);
            addState(this.mP2pDisablingState, this.mDefaultState);
            addState(this.mP2pDisabledState, this.mDefaultState);
            addState(this.mWaitForUserActionState, this.mP2pDisabledState);
            addState(this.mWaitForWifiDisableState, this.mP2pDisabledState);
            addState(this.mP2pEnablingState, this.mDefaultState);
            addState(this.mP2pEnabledState, this.mDefaultState);
            addState(this.mInactiveState, this.mP2pEnabledState);
            addState(this.mNfcProvisionState, this.mInactiveState);
            addState(this.mGroupCreatingState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingInviteRequestState, this.mGroupCreatingState);
            addState(this.mUserAuthorizingNegotiationRequestState, this.mGroupCreatingState);
            addState(this.mProvisionDiscoveryState, this.mGroupCreatingState);
            addState(this.mGroupNegotiationState, this.mGroupCreatingState);
            addState(this.mFrequencyConflictState, this.mGroupCreatingState);
            addState(this.mGroupCreatedState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingJoinState, this.mGroupCreatedState);
            addState(this.mOngoingGroupRemovalState, this.mGroupCreatedState);
            if (p2pSupported) {
                setInitialState(this.mP2pDisabledState);
            } else {
                setInitialState(this.mP2pNotSupportedState);
            }
            setLogRecSize(50);
            setLogOnlyTransitions(true);
            if (p2pSupported) {
                WifiP2pServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver(WifiP2pServiceImpl.this) {
                    public void onReceive(Context context, Intent intent) {
                        int wifistate = intent.getIntExtra("wifi_state", 4);
                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                            p2pStateMachine.logd("WIFI_STATE_CHANGED_ACTION wifistate : " + wifistate);
                        }
                        if (wifistate == 3) {
                            boolean unused = P2pStateMachine.this.mIsWifiEnabled = true;
                            P2pStateMachine.this.checkAndReEnableP2p();
                        } else if (wifistate == 0) {
                            boolean unused2 = P2pStateMachine.this.mIsWifiEnabled = false;
                            P2pStateMachine.this.sendMessage(139274);
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                        }
                    }
                }, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
                WifiP2pServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver(WifiP2pServiceImpl.this) {
                    public void onReceive(Context context, Intent intent) {
                        if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.isLocationModeEnabled()) {
                            P2pStateMachine.this.sendMessage(139268);
                        }
                    }
                }, new IntentFilter("android.location.MODE_CHANGED"));
                this.mWifiNative.registerInterfaceAvailableListener(new HalDeviceManager.InterfaceAvailableForRequestListener() {
                    public final void onAvailabilityChanged(boolean z) {
                        WifiP2pServiceImpl.P2pStateMachine.this.lambda$new$0$WifiP2pServiceImpl$P2pStateMachine(z);
                    }
                }, getHandler());
                WifiP2pServiceImpl.this.mFrameworkFacade.registerContentObserver(WifiP2pServiceImpl.this.mContext, Settings.Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(new Handler(looper), WifiP2pServiceImpl.this) {
                    public void onChange(boolean selfChange) {
                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                        p2pStateMachine.enableVerboseLogging(WifiP2pServiceImpl.this.mFrameworkFacade.getIntegerSetting(WifiP2pServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0));
                    }
                });
            }
        }

        public /* synthetic */ void lambda$new$0$WifiP2pServiceImpl$P2pStateMachine(boolean isAvailable) {
            this.mIsHalInterfaceAvailable = isAvailable;
            if (isAvailable) {
                checkAndReEnableP2p();
            }
        }

        /* access modifiers changed from: private */
        public void enableVerboseLogging(int verbose) {
            boolean unused = WifiP2pServiceImpl.mVerboseLoggingEnabled = verbose > 0;
            this.mWifiNative.enableVerboseLogging(verbose);
            this.mWifiMonitor.enableVerboseLogging(verbose);
        }

        public void registerForWifiMonitorEvents() {
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.AP_STA_CONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.AP_STA_DISCONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_LOST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_FIND_STOPPED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_STARTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147457, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147458, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_NO_COMMON_CHANNEL, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147527, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GOPS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_WPS_SKIP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_P2P_SCONNECT_PROBE_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PERSISTENT_PSK_FAIL_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_BIGDATA_DISCONNECT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_BIGDATA_CONNECTION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_BIGDATA_GROUP_OWNER_INTENT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147499, getHandler());
            this.mWifiMonitor.startMonitoring(this.mInterfaceName);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                String serviceData;
                String extra;
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    if (message.what == 143375) {
                        P2pStateMachine.this.logd(getName() + message.what);
                    } else {
                        P2pStateMachine.this.logd(getName() + message.toString());
                    }
                }
                WifiP2pConfigList wifiP2pConfigList = null;
                int i = 2;
                switch (message.what) {
                    case 69632:
                        if (message.arg1 != 0) {
                            P2pStateMachine.this.loge("Full connection failure, error = " + message.arg1);
                            AsyncChannel unused = WifiP2pServiceImpl.this.mWifiChannel = null;
                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                            p2pStateMachine.transitionTo(p2pStateMachine.mP2pDisabledState);
                            break;
                        } else {
                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                P2pStateMachine.this.logd("Full connection with ClientModeImpl established");
                            }
                            AsyncChannel unused2 = WifiP2pServiceImpl.this.mWifiChannel = (AsyncChannel) message.obj;
                            break;
                        }
                    case 69633:
                        new WifiAsyncChannel(WifiP2pServiceImpl.TAG).connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        break;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            P2pStateMachine.this.loge("Client connection lost with reason: " + message.arg1);
                        }
                        AsyncChannel unused3 = WifiP2pServiceImpl.this.mWifiChannel = null;
                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                        p2pStateMachine2.transitionTo(p2pStateMachine2.mP2pDisabledState);
                        break;
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 2);
                        WifiP2pServiceImpl.this.auditLog(false, "Connecting to device using Wi-Fi Direct (P2P) succeeded");
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 2);
                        WifiP2pServiceImpl.this.auditLog(false, "Connecting to device using Wi-Fi Direct (P2P) cancelled");
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 2);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 2);
                        break;
                    case 139283:
                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                        p2pStateMachine3.replyToMessage(message, 139284, (Object) p2pStateMachine3.getPeers(p2pStateMachine3.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid));
                        break;
                    case 139285:
                        P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                        p2pStateMachine4.replyToMessage(message, 139286, (Object) new WifiP2pInfo(p2pStateMachine4.mWifiP2pInfo));
                        break;
                    case 139287:
                        if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid, false)) {
                            P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                            p2pStateMachine5.replyToMessage(message, 139288, (Object) p2pStateMachine5.maybeEraseOwnDeviceAddress(p2pStateMachine5.mGroup, message.sendingUid));
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139288, (Object) null);
                            break;
                        }
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 2);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 2);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 2);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 2);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 2);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 2);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 2);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139319, 2);
                        break;
                    case 139321:
                        P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                        p2pStateMachine6.replyToMessage(message, 139322, (Object) new WifiP2pGroupList(p2pStateMachine6.maybeEraseOwnDeviceAddress(p2pStateMachine6.mGroups, message.sendingUid), (WifiP2pGroupList.GroupDeleteListener) null));
                        break;
                    case 139323:
                        if (WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 2);
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
                        break;
                    case 139329:
                    case 139332:
                    case 139335:
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                    case WifiP2pServiceImpl.SET_COUNTRY_CODE /*143379*/:
                    case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION /*143390*/:
                    case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION /*143391*/:
                    case WifiP2pServiceImpl.IPC_DHCP_RESULTS /*143392*/:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS /*143393*/:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE /*143394*/:
                    case 147457:
                    case 147458:
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT:
                        break;
                    case 139339:
                    case 139340:
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) null);
                        break;
                    case 139342:
                    case 139343:
                        P2pStateMachine.this.replyToMessage(message, 139345, 2);
                        break;
                    case 139346:
                        if (!P2pStateMachine.this.factoryReset(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139347, 0);
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139348);
                            break;
                        }
                    case 139349:
                        if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkStackPermission(message.sendingUid)) {
                            P2pStateMachine.this.loge("Permission violation - no NETWORK_STACK permission, uid = " + message.sendingUid);
                            P2pStateMachine.this.replyToMessage(message, 139350, (Object) null);
                            break;
                        } else {
                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                            p2pStateMachine7.replyToMessage(message, 139350, (Object) p2pStateMachine7.mSavedPeerConfig);
                            break;
                        }
                    case 139351:
                        if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkStackPermission(message.sendingUid)) {
                            P2pStateMachine.this.loge("Permission violation - no NETWORK_STACK permission, uid = " + message.sendingUid);
                            P2pStateMachine.this.replyToMessage(message, 139352);
                            break;
                        } else {
                            WifiP2pConfig peerConfig = (WifiP2pConfig) message.obj;
                            if (!P2pStateMachine.this.isConfigInvalid(peerConfig)) {
                                P2pStateMachine.this.logd("setSavedPeerConfig to " + peerConfig);
                                WifiP2pConfig unused4 = P2pStateMachine.this.mSavedPeerConfig = peerConfig;
                                P2pStateMachine.this.replyToMessage(message, 139353);
                                break;
                            } else {
                                P2pStateMachine.this.loge("Dropping set mSavedPeerConfig requeset" + peerConfig);
                                P2pStateMachine.this.replyToMessage(message, 139352);
                                break;
                            }
                        }
                    case 139354:
                        P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                        if (!p2pStateMachine8.mIsWifiEnabled || !P2pStateMachine.this.isHalInterfaceAvailable() || !WifiP2pServiceImpl.this.mWifiPermissionsUtil.isLocationModeEnabled()) {
                            i = 1;
                        }
                        p2pStateMachine8.replyToMessage(message, 139355, i);
                        break;
                    case 139356:
                        P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                        if (!WifiP2pServiceImpl.this.mDiscoveryStarted) {
                            i = 1;
                        }
                        p2pStateMachine9.replyToMessage(message, 139357, i);
                        break;
                    case 139358:
                        P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                        p2pStateMachine10.replyToMessage(message, 139359, (Object) WifiP2pServiceImpl.this.mNetworkInfo);
                        break;
                    case 139360:
                        if (message.obj instanceof Bundle) {
                            Bundle bundle = (Bundle) message.obj;
                            String pkgName = bundle.getString("android.net.wifi.p2p.CALLING_PACKAGE");
                            IBinder binder = bundle.getBinder("android.net.wifi.p2p.CALLING_BINDER");
                            try {
                                WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkPackage(message.sendingUid, pkgName);
                                if (!(binder == null || message.replyTo == null)) {
                                    WifiP2pServiceImpl.this.mClientChannelList.put(binder, message.replyTo);
                                    String unused5 = P2pStateMachine.this.getClientInfo(message.replyTo, true).mPackageName = pkgName;
                                    break;
                                }
                            } catch (SecurityException se) {
                                P2pStateMachine.this.loge("Unable to update calling package, " + se);
                                break;
                            }
                        }
                        break;
                    case 139361:
                        if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid, false)) {
                            P2pStateMachine p2pStateMachine11 = P2pStateMachine.this;
                            p2pStateMachine11.replyToMessage(message, 139362, (Object) p2pStateMachine11.maybeEraseOwnDeviceAddress(WifiP2pServiceImpl.this.mThisDevice, message.sendingUid));
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139362, (Object) null);
                            break;
                        }
                    case 139378:
                        P2pStateMachine p2pStateMachine12 = P2pStateMachine.this;
                        if (p2pStateMachine12.mSelectP2pConfigList != null) {
                            wifiP2pConfigList = new WifiP2pConfigList(P2pStateMachine.this.mSelectP2pConfigList);
                        }
                        p2pStateMachine12.replyToMessage(message, 139379, (Object) wifiP2pConfigList);
                        break;
                    case 139380:
                        if (!P2pStateMachine.this.setDialogListenerApp(message.replyTo, message.getData().getString("appPkgName"), message.getData().getBoolean("dialogResetFlag"))) {
                            P2pStateMachine.this.replyToMessage(message, 139381, 4);
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139382);
                            break;
                        }
                    case 139408:
                        P2pStateMachine.this.replyToMessage(message, 139409, 2);
                        break;
                    case 139412:
                        if (message.obj != null) {
                            P2pStateMachine.this.addHistoricalDumpLog(((Bundle) message.obj).getString("extra_log"));
                            break;
                        }
                        break;
                    case 139419:
                        if (!P2pStateMachine.this.mWifiNative.p2pSet("screen_sharing", message.arg1 == 1 ? "1" : "0")) {
                            P2pStateMachine.this.loge("Failed to set screen sharing");
                            break;
                        }
                        break;
                    case 139420:
                        int serviceId = message.arg1;
                        String serviceData2 = message.getData().getString("SDATA");
                        if (serviceData2 != null && serviceData2.length() > 0) {
                            serviceData = serviceId + " " + serviceData2;
                        } else if (serviceId <= 0 || serviceId >= 256) {
                            P2pStateMachine.this.loge("Failed to set service_data (invalid id)");
                            break;
                        } else {
                            serviceData = Integer.toString(serviceId);
                        }
                        if (!P2pStateMachine.this.mWifiNative.p2pSet("service_data", serviceData)) {
                            P2pStateMachine.this.loge("Failed to set service_data");
                            break;
                        }
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                        boolean unused6 = WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        boolean unused7 = WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            if (message.obj != null) {
                                try {
                                    ((StateMachine) message.obj).sendMessage(message.arg2);
                                    break;
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                    break;
                                }
                            } else {
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                break;
                            }
                        }
                        break;
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) enabling succeeded", 1);
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                        if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISABLE_P2P_RSP);
                        } else {
                            P2pStateMachine.this.loge("Unexpected disable request when WifiChannel is null");
                        }
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                        break;
                    case WifiP2pServiceImpl.CMD_BOOT_COMPLETED /*143415*/:
                        P2pStateMachine.this.checkAndSetConnectivityInstance();
                        if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress == null || WifiP2pServiceImpl.this.mThisDevice.deviceAddress.isEmpty()) {
                            if (!WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                                if (P2pStateMachine.this.makeP2pHwMac()) {
                                    WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS;
                                    P2pStateMachine.this.sendThisDeviceChangedBroadcast();
                                    break;
                                }
                            } else {
                                String unused8 = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS = WifiP2pServiceImpl.toHexString(WifiP2pServiceImpl.this.createRandomMac());
                                WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS;
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    Log.i(WifiP2pServiceImpl.TAG, "CMD_BOOT_COMPLETED. MAC will be changed: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                                }
                                P2pStateMachine.this.sendThisDeviceChangedBroadcast();
                                break;
                            }
                        }
                        break;
                    case WifiP2pServiceImpl.CMD_SEC_LOGGING /*143420*/:
                        if (WifiP2pServiceImpl.this.mIsBootComplete) {
                            Bundle args = (Bundle) message.obj;
                            WifiP2pServiceImpl.this.mBigData.initialize();
                            if (args != null) {
                                if (!args.getBoolean("bigdata", false)) {
                                    P2pStateMachine.this.insertLog(args.getString("feature", (String) null), args.getString("extra", (String) null), args.getLong("value", -1));
                                    break;
                                } else {
                                    String feature = args.getString("feature", (String) null);
                                    if (WifiP2pServiceImpl.this.mBigData.parseData(feature, args.getString("data", (String) null)) && (extra = WifiP2pServiceImpl.this.mBigData.getJsonFormat(feature)) != null) {
                                        P2pStateMachine.this.insertLog(feature, extra, -1);
                                        break;
                                    }
                                }
                            } else {
                                P2pStateMachine.this.loge("CMD_SEC_LOGGING : args null!");
                                break;
                            }
                        }
                        break;
                    case WifiP2pServiceImpl.P2P_ADVOPP_LISTEN_TIMEOUT /*143462*/:
                        boolean unused9 = WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen = false;
                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_LISTEN_TIMEOUT);
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                        if (WifiP2pServiceImpl.this.userRejected) {
                            P2pStateMachine.this.sendP2pRequestChangedBroadcast(false);
                            boolean unused10 = WifiP2pServiceImpl.this.userRejected = false;
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                        if (message.obj != null) {
                            WifiP2pGroup unused11 = P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            break;
                        } else {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                            break;
                        }
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus status = (P2pStatus) message.obj;
                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                            P2pStateMachine.this.logd("P2P_INVITATION_RESULT_EVENT : " + status);
                        }
                        if (status == P2pStatus.NO_COMMON_CHANNEL) {
                            if (P2pStateMachine.this.mSelectP2pConfigList != null) {
                                if (P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex) != null) {
                                    P2pStateMachine p2pStateMachine13 = P2pStateMachine.this;
                                    String unused12 = p2pStateMachine13.mConnectedDevAddr = p2pStateMachine13.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex).deviceAddress;
                                    P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mConnectedDevAddr, 2);
                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                    P2pStateMachine.access$6808(P2pStateMachine.this);
                                    P2pStateMachine p2pStateMachine14 = P2pStateMachine.this;
                                    p2pStateMachine14.sendMessageDelayed(p2pStateMachine14.obtainMessage(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT, p2pStateMachine14.mSelectP2pConfigIndex, 0), 30000);
                                    P2pStateMachine.this.sendMessage(139271);
                                    break;
                                } else {
                                    WifiP2pConfigList unused13 = P2pStateMachine.this.mSelectP2pConfigList = null;
                                    int unused14 = P2pStateMachine.this.mSelectP2pConfigIndex = 0;
                                    P2pStateMachine.this.stopLegacyWifiScan(false);
                                    break;
                                }
                            } else {
                                P2pStateMachine p2pStateMachine15 = P2pStateMachine.this;
                                p2pStateMachine15.transitionTo(p2pStateMachine15.mFrequencyConflictState);
                                break;
                            }
                        }
                        break;
                    case WifiP2pMonitor.P2P_NO_COMMON_CHANNEL:
                        if (P2pStateMachine.this.mSavedPeerConfig == null) {
                            WifiP2pConfig unused15 = P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = (String) message.obj;
                            WifiP2pDevice wifiP2pDevice = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                            WifiP2pDevice dev = wifiP2pDevice;
                            if (wifiP2pDevice != null) {
                                if (dev.wpsPbcSupported()) {
                                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                } else if (dev.wpsKeypadSupported()) {
                                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                } else if (dev.wpsDisplaySupported()) {
                                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                }
                            }
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        }
                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                            P2pStateMachine.this.logd("P2P_NO_COMMON_CHANNEL : " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + " WPS = " + P2pStateMachine.this.mSavedPeerConfig.wps.setup);
                        }
                        P2pStateMachine p2pStateMachine16 = P2pStateMachine.this;
                        p2pStateMachine16.transitionTo(p2pStateMachine16.mFrequencyConflictState);
                        break;
                    case WifiP2pMonitor.P2P_BIGDATA_DISCONNECT_EVENT:
                    case WifiP2pMonitor.P2P_BIGDATA_CONNECTION_RESULT_EVENT:
                        String data = P2pStateMachine.this.buildLoggingData(message.what, (String) message.obj);
                        if (data != null) {
                            Bundle args2 = new Bundle();
                            args2.putBoolean("bigdata", true);
                            if (message.what == 147536) {
                                args2.putString("feature", "WDCL");
                            } else if (message.what == 147537) {
                                WifiP2pServiceImpl.this.mConnReqInfo.reset();
                                args2.putString("feature", "WDGF");
                            }
                            args2.putString("data", data);
                            P2pStateMachine p2pStateMachine17 = P2pStateMachine.this;
                            p2pStateMachine17.sendMessage(p2pStateMachine17.obtainMessage(WifiP2pServiceImpl.CMD_SEC_LOGGING, 0, 0, args2));
                            break;
                        }
                        break;
                    case WifiP2pMonitor.P2P_BIGDATA_GROUP_OWNER_INTENT_EVENT:
                        String unused16 = P2pStateMachine.this.buildLoggingData(message.what, (String) message.obj);
                        break;
                    default:
                        P2pStateMachine.this.loge("Unhandled message " + message);
                        return false;
                }
                return true;
            }
        }

        class P2pNotSupportedState extends State {
            P2pNotSupportedState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 1);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 1);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 1);
                        WifiP2pServiceImpl.this.auditLog(false, "Connecting to device using Wi-Fi Direct (P2P) failed");
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 1);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 1);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 1);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 1);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 1);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 1);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 1);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 1);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 1);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 1);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 1);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139319, 1);
                        break;
                    case 139323:
                        if (WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 1);
                            break;
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                            break;
                        }
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 1);
                        break;
                    case 139329:
                        P2pStateMachine.this.replyToMessage(message, 139330, 1);
                        break;
                    case 139332:
                        P2pStateMachine.this.replyToMessage(message, 139333, 1);
                        break;
                    case 139346:
                        P2pStateMachine.this.replyToMessage(message, 139347, 1);
                        break;
                    case 139380:
                        P2pStateMachine.this.replyToMessage(message, 139381, 1);
                        break;
                    case 139408:
                        P2pStateMachine.this.replyToMessage(message, 139409, 1);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class P2pDisablingState extends State {
            P2pDisablingState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    P2pStateMachine.this.logd(getName());
                }
                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                p2pStateMachine.sendMessageDelayed(p2pStateMachine.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.access$8604(), 0), RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                    p2pStateMachine.logd(getName() + message.toString());
                }
                switch (message.what) {
                    case 139365:
                    case 139368:
                        P2pStateMachine.this.deferMessage(message);
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                        break;
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT /*143366*/:
                        if (WifiP2pServiceImpl.sDisableP2pTimeoutIndex != message.arg1) {
                            WifiP2pServiceImpl.this.auditLog(false, "Wi-Fi Direct (P2P) disabling failed", 1);
                            break;
                        } else {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                            p2pStateMachine2.transitionTo(p2pStateMachine2.mP2pDisabledState);
                            WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                            break;
                        }
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                    case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                    case WifiP2pServiceImpl.REMOVE_CLIENT_INFO /*143378*/:
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case 147458:
                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                            P2pStateMachine.this.logd("p2p socket connection lost");
                        }
                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                        p2pStateMachine3.transitionTo(p2pStateMachine3.mP2pDisabledState);
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISABLE_P2P_RSP);
                    P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT);
                    return;
                }
                P2pStateMachine.this.loge("DISABLE_P2P_SUCCEEDED(): WifiChannel is null");
            }
        }

        class P2pDisabledState extends State {
            P2pDisabledState() {
            }

            public void enter() {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    P2pStateMachine.this.logd(getName());
                }
                if (WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                    if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress != null && !WifiP2pServiceImpl.this.mThisDevice.deviceAddress.isEmpty()) {
                        String unused = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS = WifiP2pServiceImpl.toHexString(WifiP2pServiceImpl.this.createRandomMac());
                        WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS;
                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                            Log.i(WifiP2pServiceImpl.TAG, "P2pDisabledState MAC will be changed: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                        }
                    }
                } else if ((WifiP2pServiceImpl.this.mThisDevice.deviceAddress == null || WifiP2pServiceImpl.this.mThisDevice.deviceAddress.isEmpty()) && P2pStateMachine.this.makeP2pHwMac()) {
                    WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS;
                    P2pStateMachine.this.sendThisDeviceChangedBroadcast();
                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                        Log.i(WifiP2pServiceImpl.TAG, "P2pDisabledState MAC will be changed: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                    }
                }
            }

            private void setupInterfaceFeatures(String interfaceName) {
                if (WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                    Log.i(WifiP2pServiceImpl.TAG, "Supported feature: P2P MAC randomization");
                    P2pStateMachine.this.mWifiNative.p2pSet("random_mac", WifiP2pServiceImpl.RANDOM_MAC_ADDRESS);
                    P2pStateMachine.this.mWifiNative.setMacRandomization(true);
                    return;
                }
                Log.i(WifiP2pServiceImpl.TAG, "Unsupported feature: P2P MAC randomization");
                P2pStateMachine.this.mWifiNative.setMacRandomization(false);
            }

            public boolean processMessage(Message message) {
                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                    P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                    p2pStateMachine.logd(getName() + message.toString());
                }
                switch (message.what) {
                    case 139265:
                    case 139371:
                        Context access$3200 = WifiP2pServiceImpl.this.mContext;
                        Context unused = WifiP2pServiceImpl.this.mContext;
                        WifiManager tWifiManager = (WifiManager) access$3200.getSystemService("wifi");
                        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && !P2pStateMachine.this.isForegroundApp(WifiP2pServiceImpl.WIFI_DIRECT_SETTINGS_PKGNAME) && tWifiManager != null && (tWifiManager.isWifiApEnabled() || tWifiManager.getWifiApState() == 12 || (!tWifiManager.isWifiApEnabled() && tWifiManager.getWifiApState() != 12 && tWifiManager.isWifiEnabled()))) {
                            int unused2 = WifiP2pServiceImpl.this.mDelayedDiscoverPeersCmd = message.what;
                            int unused3 = WifiP2pServiceImpl.this.mDelayedDiscoverPeersArg = message.arg1;
                            boolean unused4 = WifiP2pServiceImpl.this.mDelayedDiscoverPeers = true;
                            P2pStateMachine.this.sendMessage(139365);
                            break;
                        }
                    case 139365:
                        Bundle bundle = message.getData();
                        if (bundle != null) {
                            WifiP2pServiceImpl.this.allowForcingEnableP2pForApp(bundle.getString("appPkgName"));
                        }
                        Context access$32002 = WifiP2pServiceImpl.this.mContext;
                        Context unused5 = WifiP2pServiceImpl.this.mContext;
                        WifiManager tWifiManager2 = (WifiManager) access$32002.getSystemService("wifi");
                        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && tWifiManager2 != null && (tWifiManager2.isWifiApEnabled() || tWifiManager2.getWifiApState() == 12)) {
                            WifiP2pServiceImpl.this.checkAndShowP2pEnableDialog(0);
                        } else if (WifiP2pServiceImpl.this.mWifiAwareManager != null && WifiP2pServiceImpl.this.mWifiAwareManager.isEnabled() && !WifiP2pServiceImpl.this.mReceivedEnableP2p) {
                            WifiP2pServiceImpl.this.checkAndShowP2pEnableDialog(1);
                        } else if (!WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() || tWifiManager2 == null || tWifiManager2.isWifiApEnabled() || tWifiManager2.getWifiApState() == 12 || !tWifiManager2.isWifiEnabled()) {
                            P2pStateMachine.this.setLegacyWifiEnable(true);
                        } else {
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.ENABLE_P2P);
                        }
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                        break;
                    case 139368:
                        P2pStateMachine.this.replyToMessage(message, 139369);
                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                        break;
                    case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                        if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress == null || WifiP2pServiceImpl.this.mThisDevice.deviceAddress.isEmpty()) {
                            if (WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                                String unused6 = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS = WifiP2pServiceImpl.toHexString(WifiP2pServiceImpl.this.createRandomMac());
                                WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS;
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    Log.i(WifiP2pServiceImpl.TAG, "ENABLE_P2P MAC will be changed: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                                }
                            } else if (P2pStateMachine.this.makeP2pHwMac()) {
                                WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS;
                                P2pStateMachine.this.sendThisDeviceChangedBroadcast();
                            }
                        }
                        if (P2pStateMachine.this.mIsWifiEnabled) {
                            Context access$32003 = WifiP2pServiceImpl.this.mContext;
                            Context unused7 = WifiP2pServiceImpl.this.mContext;
                            WifiManager tWifiManager3 = (WifiManager) access$32003.getSystemService("wifi");
                            if (!WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() || tWifiManager3 == null || (!tWifiManager3.isWifiApEnabled() && tWifiManager3.getWifiApState() != 12)) {
                                if (WifiP2pServiceImpl.this.mWifiAwareManager == null || !WifiP2pServiceImpl.this.mWifiAwareManager.isEnabled() || WifiP2pServiceImpl.this.mReceivedEnableP2p) {
                                    boolean unused8 = WifiP2pServiceImpl.this.mSetupInterfaceIsRunnging = true;
                                    P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                    String unused9 = p2pStateMachine2.mInterfaceName = p2pStateMachine2.mWifiNative.setupInterface(
                                    /*  JADX ERROR: Method code generation error
                                        jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x01c0: INVOKE  
                                          (r3v38 'p2pStateMachine2' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                          (wrap: java.lang.String : 0x01bc: INVOKE  (r5v2 java.lang.String) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pNative : 0x01ad: INVOKE  (r5v1 com.android.server.wifi.p2p.WifiP2pNative) = 
                                          (r3v38 'p2pStateMachine2' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$2800(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine):com.android.server.wifi.p2p.WifiP2pNative type: STATIC)
                                          (wrap: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM : 0x01b3: CONSTRUCTOR  (r8v2 com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         call: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM.<init>(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState):void type: CONSTRUCTOR)
                                          (wrap: android.os.Handler : 0x01b8: INVOKE  (r9v1 android.os.Handler) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine : 0x01b6: IGET  (r9v0 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.this$1 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.getHandler():android.os.Handler type: VIRTUAL)
                                         com.android.server.wifi.p2p.WifiP2pNative.setupInterface(com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListener, android.os.Handler):java.lang.String type: VIRTUAL)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$9102(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine, java.lang.String):java.lang.String type: STATIC in method: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.processMessage(android.os.Message):boolean, dex: classes.dex
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
                                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                                        	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:142)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:62)
                                        	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                                        	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                                        	at jadx.core.codegen.RegionGen.makeSwitch(RegionGen.java:298)
                                        	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:64)
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
                                        Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0000: IPUT  
                                          (wrap: java.lang.String : 0x01bc: INVOKE  (r5v2 java.lang.String) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pNative : 0x01ad: INVOKE  (r5v1 com.android.server.wifi.p2p.WifiP2pNative) = 
                                          (r3v38 'p2pStateMachine2' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$2800(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine):com.android.server.wifi.p2p.WifiP2pNative type: STATIC)
                                          (wrap: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM : 0x01b3: CONSTRUCTOR  (r8v2 com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         call: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM.<init>(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState):void type: CONSTRUCTOR)
                                          (wrap: android.os.Handler : 0x01b8: INVOKE  (r9v1 android.os.Handler) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine : 0x01b6: IGET  (r9v0 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.this$1 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.getHandler():android.os.Handler type: VIRTUAL)
                                         com.android.server.wifi.p2p.WifiP2pNative.setupInterface(com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListener, android.os.Handler):java.lang.String type: VIRTUAL)
                                          (r3v38 'p2pStateMachine2' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.mInterfaceName java.lang.String in method: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.processMessage(android.os.Message):boolean, dex: classes.dex
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                                        	at jadx.core.codegen.InsnGen.inlineMethod(InsnGen.java:924)
                                        	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:684)
                                        	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:368)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:250)
                                        	... 79 more
                                        Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x01bc: INVOKE  (r5v2 java.lang.String) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pNative : 0x01ad: INVOKE  (r5v1 com.android.server.wifi.p2p.WifiP2pNative) = 
                                          (r3v38 'p2pStateMachine2' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$2800(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine):com.android.server.wifi.p2p.WifiP2pNative type: STATIC)
                                          (wrap: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM : 0x01b3: CONSTRUCTOR  (r8v2 com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         call: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM.<init>(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState):void type: CONSTRUCTOR)
                                          (wrap: android.os.Handler : 0x01b8: INVOKE  (r9v1 android.os.Handler) = 
                                          (wrap: com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine : 0x01b6: IGET  (r9v0 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.this$1 com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine)
                                         com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.getHandler():android.os.Handler type: VIRTUAL)
                                         com.android.server.wifi.p2p.WifiP2pNative.setupInterface(com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListener, android.os.Handler):java.lang.String type: VIRTUAL in method: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.processMessage(android.os.Message):boolean, dex: classes.dex
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                                        	at jadx.core.codegen.InsnGen.addWrappedArg(InsnGen.java:123)
                                        	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:107)
                                        	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:429)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                                        	... 83 more
                                        Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x01b3: CONSTRUCTOR  (r8v2 com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM) = 
                                          (r10v0 'this' com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState A[THIS])
                                         call: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM.<init>(com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState):void type: CONSTRUCTOR in method: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.processMessage(android.os.Message):boolean, dex: classes.dex
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                                        	at jadx.core.codegen.InsnGen.addWrappedArg(InsnGen.java:123)
                                        	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:107)
                                        	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:787)
                                        	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:728)
                                        	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:368)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                                        	... 87 more
                                        Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Expected class to be processed at this point, class: com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM, state: NOT_LOADED
                                        	at jadx.core.dex.nodes.ClassNode.ensureProcessed(ClassNode.java:260)
                                        	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:606)
                                        	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:364)
                                        	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                                        	... 93 more
                                        */
                                    /*
                                        this = this;
                                        java.lang.String r0 = "Unable to change interface settings: "
                                        boolean r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r1 == 0) goto L_0x0024
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r2 = new java.lang.StringBuilder
                                        r2.<init>()
                                        java.lang.String r3 = r10.getName()
                                        r2.append(r3)
                                        java.lang.String r3 = r11.toString()
                                        r2.append(r3)
                                        java.lang.String r2 = r2.toString()
                                        r1.logd(r2)
                                    L_0x0024:
                                        int r1 = r11.what
                                        java.lang.String r2 = "Wi-Fi Direct (P2P) disabling succeeded"
                                        java.lang.String r3 = "wifi"
                                        r4 = 0
                                        r5 = 12
                                        r6 = 1
                                        switch(r1) {
                                            case 139265: goto L_0x02f4;
                                            case 139365: goto L_0x0246;
                                            case 139368: goto L_0x0235;
                                            case 139371: goto L_0x02f4;
                                            case 143376: goto L_0x007f;
                                            case 143378: goto L_0x0032;
                                            default: goto L_0x0031;
                                        }
                                    L_0x0031:
                                        return r4
                                    L_0x0032:
                                        java.lang.Object r0 = r11.obj
                                        boolean r0 = r0 instanceof android.os.IBinder
                                        if (r0 != 0) goto L_0x0041
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r1 = "Invalid obj when REMOVE_CLIENT_INFO"
                                        r0.loge(r1)
                                        goto L_0x0363
                                    L_0x0041:
                                        java.lang.Object r0 = r11.obj
                                        android.os.IBinder r0 = (android.os.IBinder) r0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        java.util.Map r1 = r1.mClientChannelList
                                        java.lang.Object r1 = r1.remove(r0)
                                        android.os.Messenger r1 = (android.os.Messenger) r1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        java.util.HashMap r2 = r2.mClientInfoList
                                        java.lang.Object r2 = r2.remove(r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$ClientInfo r2 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.ClientInfo) r2
                                        if (r2 == 0) goto L_0x0363
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                        r4.<init>()
                                        java.lang.String r5 = "Remove client - "
                                        r4.append(r5)
                                        java.lang.String r5 = r2.mPackageName
                                        r4.append(r5)
                                        java.lang.String r4 = r4.toString()
                                        r3.logd(r4)
                                        goto L_0x0363
                                    L_0x007f:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r1.mThisDevice
                                        java.lang.String r1 = r1.deviceAddress
                                        java.lang.String r7 = "WifiP2pService"
                                        if (r1 == 0) goto L_0x009d
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r1.mThisDevice
                                        java.lang.String r1 = r1.deviceAddress
                                        boolean r1 = r1.isEmpty()
                                        if (r1 == 0) goto L_0x010f
                                    L_0x009d:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context r1 = r1.mContext
                                        android.content.res.Resources r1 = r1.getResources()
                                        r8 = 17891609(0x1110119, float:2.6633081E-38)
                                        boolean r1 = r1.getBoolean(r8)
                                        if (r1 == 0) goto L_0x00f4
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        byte[] r1 = r1.createRandomMac()
                                        java.lang.String r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.toHexString(r1)
                                        java.lang.String unused = com.android.server.wifi.p2p.WifiP2pServiceImpl.RANDOM_MAC_ADDRESS = r1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r1.mThisDevice
                                        java.lang.String r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.RANDOM_MAC_ADDRESS
                                        r1.deviceAddress = r8
                                        boolean r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r1 == 0) goto L_0x010f
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r8 = "ENABLE_P2P MAC will be changed: "
                                        r1.append(r8)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r8 = r8.mThisDevice
                                        java.lang.String r8 = r8.deviceAddress
                                        r1.append(r8)
                                        java.lang.String r1 = r1.toString()
                                        android.util.Log.i(r7, r1)
                                        goto L_0x010f
                                    L_0x00f4:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        boolean r1 = r1.makeP2pHwMac()
                                        if (r1 == 0) goto L_0x010f
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r1.mThisDevice
                                        java.lang.String r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS
                                        r1.deviceAddress = r8
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1.sendThisDeviceChangedBroadcast()
                                    L_0x010f:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        boolean r1 = r1.mIsWifiEnabled
                                        if (r1 != 0) goto L_0x0133
                                        java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                        r0.<init>()
                                        java.lang.String r1 = "Ignore P2P enable since wifi is "
                                        r0.append(r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        boolean r1 = r1.mIsWifiEnabled
                                        r0.append(r1)
                                        java.lang.String r0 = r0.toString()
                                        android.util.Log.e(r7, r0)
                                        goto L_0x0363
                                    L_0x0133:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context r1 = r1.mContext
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r8 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context unused = r8.mContext
                                        java.lang.Object r1 = r1.getSystemService(r3)
                                        android.net.wifi.WifiManager r1 = (android.net.wifi.WifiManager) r1
                                        com.android.server.wifi.WifiInjector r3 = com.android.server.wifi.WifiInjector.getInstance()
                                        com.samsung.android.server.wifi.softap.SemWifiApChipInfo r3 = r3.getSemWifiApChipInfo()
                                        boolean r3 = r3.supportWifiSharing()
                                        if (r3 == 0) goto L_0x0173
                                        if (r1 == 0) goto L_0x0173
                                        boolean r3 = r1.isWifiApEnabled()
                                        if (r3 != 0) goto L_0x0164
                                        int r3 = r1.getWifiApState()
                                        if (r3 != r5) goto L_0x0173
                                    L_0x0164:
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x022c
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r3 = "when mobilehotspot is on, p2p cannot be enabled. so do nothing."
                                        r0.logd(r3)
                                        goto L_0x022c
                                    L_0x0173:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.aware.WifiAwareManager r3 = r3.mWifiAwareManager
                                        if (r3 == 0) goto L_0x01a4
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.aware.WifiAwareManager r3 = r3.mWifiAwareManager
                                        boolean r3 = r3.isEnabled()
                                        if (r3 == 0) goto L_0x01a4
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean r3 = r3.mReceivedEnableP2p
                                        if (r3 != 0) goto L_0x01a4
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x022c
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r3 = "when Wi-Fi Aware is on, p2p cannot be enabled. so do nothing."
                                        r0.logd(r3)
                                        goto L_0x022c
                                    L_0x01a4:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r3.mSetupInterfaceIsRunnging = r6
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r5 = r3.mWifiNative
                                        com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM r8 = new com.android.server.wifi.p2p.-$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM
                                        r8.<init>(r10)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r9 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.os.Handler r9 = r9.getHandler()
                                        java.lang.String r5 = r5.setupInterface(r8, r9)
                                        java.lang.String unused = r3.mInterfaceName = r5
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r3.mSetupInterfaceIsRunnging = r4
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r3 = r3.mInterfaceName
                                        if (r3 != 0) goto L_0x01d9
                                        java.lang.String r0 = "Failed to setup interface for P2P"
                                        android.util.Log.e(r7, r0)
                                        goto L_0x0363
                                    L_0x01d9:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r3 = r3.mInterfaceName
                                        r10.setupInterfaceFeatures(r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        android.os.INetworkManagementService r3 = r3.mNwService     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        java.lang.String r4 = r4.mInterfaceName     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        r3.setInterfaceUp(r4)     // Catch:{ RemoteException -> 0x0208, IllegalStateException -> 0x01f2 }
                                        goto L_0x021d
                                    L_0x01f2:
                                        r3 = move-exception
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r5 = new java.lang.StringBuilder
                                        r5.<init>()
                                        r5.append(r0)
                                        r5.append(r3)
                                        java.lang.String r0 = r5.toString()
                                        r4.loge(r0)
                                        goto L_0x021e
                                    L_0x0208:
                                        r3 = move-exception
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r5 = new java.lang.StringBuilder
                                        r5.<init>()
                                        r5.append(r0)
                                        r5.append(r3)
                                        java.lang.String r0 = r5.toString()
                                        r4.loge(r0)
                                    L_0x021d:
                                    L_0x021e:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r0.registerForWifiMonitorEvents()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$P2pEnablingState r3 = r0.mP2pEnablingState
                                        r0.transitionTo(r3)
                                    L_0x022c:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r0.auditLog(r6, r2, r6)
                                        goto L_0x0363
                                    L_0x0235:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1 = 139369(0x22069, float:1.95298E-40)
                                        r0.replyToMessage(r11, r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r0.auditLog(r6, r2, r6)
                                        goto L_0x0363
                                    L_0x0246:
                                        android.os.Bundle r0 = r11.getData()
                                        if (r0 == 0) goto L_0x0259
                                        java.lang.String r1 = "appPkgName"
                                        java.lang.String r1 = r0.getString(r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r7.allowForcingEnableP2pForApp(r1)
                                    L_0x0259:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context r1 = r1.mContext
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context unused = r7.mContext
                                        java.lang.Object r1 = r1.getSystemService(r3)
                                        android.net.wifi.WifiManager r1 = (android.net.wifi.WifiManager) r1
                                        com.android.server.wifi.WifiInjector r3 = com.android.server.wifi.WifiInjector.getInstance()
                                        com.samsung.android.server.wifi.softap.SemWifiApChipInfo r3 = r3.getSemWifiApChipInfo()
                                        boolean r3 = r3.supportWifiSharing()
                                        if (r3 == 0) goto L_0x0292
                                        if (r1 == 0) goto L_0x0292
                                        boolean r3 = r1.isWifiApEnabled()
                                        if (r3 != 0) goto L_0x028a
                                        int r3 = r1.getWifiApState()
                                        if (r3 != r5) goto L_0x0292
                                    L_0x028a:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r3.checkAndShowP2pEnableDialog(r4)
                                        goto L_0x02ec
                                    L_0x0292:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.aware.WifiAwareManager r3 = r3.mWifiAwareManager
                                        if (r3 == 0) goto L_0x02bc
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.aware.WifiAwareManager r3 = r3.mWifiAwareManager
                                        boolean r3 = r3.isEnabled()
                                        if (r3 == 0) goto L_0x02bc
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean r3 = r3.mReceivedEnableP2p
                                        if (r3 != 0) goto L_0x02bc
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r3.checkAndShowP2pEnableDialog(r6)
                                        goto L_0x02ec
                                    L_0x02bc:
                                        com.android.server.wifi.WifiInjector r3 = com.android.server.wifi.WifiInjector.getInstance()
                                        com.samsung.android.server.wifi.softap.SemWifiApChipInfo r3 = r3.getSemWifiApChipInfo()
                                        boolean r3 = r3.supportWifiSharing()
                                        if (r3 == 0) goto L_0x02e7
                                        if (r1 == 0) goto L_0x02e7
                                        boolean r3 = r1.isWifiApEnabled()
                                        if (r3 != 0) goto L_0x02e7
                                        int r3 = r1.getWifiApState()
                                        if (r3 == r5) goto L_0x02e7
                                        boolean r3 = r1.isWifiEnabled()
                                        if (r3 == 0) goto L_0x02e7
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r4 = 143376(0x23010, float:2.00913E-40)
                                        r3.sendMessage(r4)
                                        goto L_0x02ec
                                    L_0x02e7:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r3.setLegacyWifiEnable(r6)
                                    L_0x02ec:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        r3.auditLog(r6, r2, r6)
                                        goto L_0x0363
                                    L_0x02f4:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context r0 = r0.mContext
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.content.Context unused = r1.mContext
                                        java.lang.Object r0 = r0.getSystemService(r3)
                                        android.net.wifi.WifiManager r0 = (android.net.wifi.WifiManager) r0
                                        com.android.server.wifi.WifiInjector r1 = com.android.server.wifi.WifiInjector.getInstance()
                                        com.samsung.android.server.wifi.softap.SemWifiApChipInfo r1 = r1.getSemWifiApChipInfo()
                                        boolean r1 = r1.supportWifiSharing()
                                        if (r1 == 0) goto L_0x0362
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r2 = "com.android.settings"
                                        boolean r1 = r1.isForegroundApp(r2)
                                        if (r1 != 0) goto L_0x0362
                                        if (r0 == 0) goto L_0x0362
                                        boolean r1 = r0.isWifiApEnabled()
                                        if (r1 != 0) goto L_0x0341
                                        int r1 = r0.getWifiApState()
                                        if (r1 == r5) goto L_0x0341
                                        boolean r1 = r0.isWifiApEnabled()
                                        if (r1 != 0) goto L_0x0362
                                        int r1 = r0.getWifiApState()
                                        if (r1 == r5) goto L_0x0362
                                        boolean r1 = r0.isWifiEnabled()
                                        if (r1 == 0) goto L_0x0362
                                    L_0x0341:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        int r2 = r11.what
                                        int unused = r1.mDelayedDiscoverPeersCmd = r2
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        int r2 = r11.arg1
                                        int unused = r1.mDelayedDiscoverPeersArg = r2
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r1.mDelayedDiscoverPeers = r6
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r2 = 139365(0x22065, float:1.95292E-40)
                                        r1.sendMessage(r2)
                                    L_0x0362:
                                    L_0x0363:
                                        return r6
                                    */
                                    throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.P2pDisabledState.processMessage(android.os.Message):boolean");
                                }

                                /* renamed from: lambda$processMessage$0$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState */
                                public /* synthetic */ void mo5743xa9b01d2c(String ifaceName) {
                                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                                }
                            }

                            class WaitForUserActionState extends State {
                                WaitForUserActionState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + "{ what=" + message.what + " }");
                                    }
                                    switch (message.what) {
                                        case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                                            Context access$3200 = WifiP2pServiceImpl.this.mContext;
                                            Context unused = WifiP2pServiceImpl.this.mContext;
                                            WifiManager tWifiManager = (WifiManager) access$3200.getSystemService("wifi");
                                            if (tWifiManager != null) {
                                                if (WifiP2pServiceImpl.this.mWifiApState == 13 || WifiP2pServiceImpl.this.mWifiApState == 12) {
                                                    tWifiManager.semSetWifiApEnabled((WifiConfiguration) null, false);
                                                } else {
                                                    tWifiManager.setWifiEnabled(false);
                                                }
                                            }
                                            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_ENABLE_PENDING);
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            p2pStateMachine2.transitionTo(p2pStateMachine2.mWaitForWifiDisableState);
                                            return true;
                                        case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                                            P2pStateMachine.this.logd("User rejected enabling p2p");
                                            P2pStateMachine.this.sendP2pStateChangedBroadcast(false);
                                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                            p2pStateMachine3.transitionTo(p2pStateMachine3.mP2pDisabledState);
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            }

                            class WaitForWifiDisableState extends State {
                                WaitForWifiDisableState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + "{ what=" + message.what + " }");
                                    }
                                    int i = message.what;
                                    if (i != 139365 && i != 139368) {
                                        return false;
                                    }
                                    P2pStateMachine.this.deferMessage(message);
                                    return true;
                                }
                            }

                            class P2pEnablingState extends State {
                                P2pEnablingState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + message.toString());
                                    }
                                    switch (message.what) {
                                        case 139365:
                                        case 139368:
                                            WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) enabling succeeded", 1);
                                            P2pStateMachine.this.deferMessage(message);
                                            break;
                                        case 147457:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("P2p socket connection successful");
                                            }
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            p2pStateMachine2.transitionTo(p2pStateMachine2.mInactiveState);
                                            break;
                                        case 147458:
                                            P2pStateMachine.this.loge("P2p socket connection failed");
                                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                            p2pStateMachine3.transitionTo(p2pStateMachine3.mP2pDisabledState);
                                            break;
                                        default:
                                            return false;
                                    }
                                    return true;
                                }
                            }

                            class P2pEnabledState extends State {
                                P2pEnabledState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    int unused = P2pStateMachine.this.mP2pState = 2;
                                    P2pStateMachine.this.sendP2pStateChangedBroadcast(true);
                                    WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                                    if (P2pStateMachine.this.isPendingFactoryReset()) {
                                        boolean unused2 = P2pStateMachine.this.factoryReset(1000);
                                    }
                                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                    P2pStateMachine.this.initializeP2pSettings();
                                    boolean unused3 = WifiP2pServiceImpl.this.mReceivedEnableP2p = false;
                                    if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && WifiP2pServiceImpl.this.mDelayedDiscoverPeers) {
                                        if (WifiP2pServiceImpl.this.mDelayedDiscoverPeersCmd == 139265 || WifiP2pServiceImpl.this.mDelayedDiscoverPeersCmd == 139371) {
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            p2pStateMachine.sendMessage(WifiP2pServiceImpl.this.mDelayedDiscoverPeersCmd, WifiP2pServiceImpl.this.mDelayedDiscoverPeersArg);
                                        }
                                        boolean unused4 = WifiP2pServiceImpl.this.mDelayedDiscoverPeers = false;
                                        int unused5 = WifiP2pServiceImpl.this.mDelayedDiscoverPeersCmd = -1;
                                        int unused6 = WifiP2pServiceImpl.this.mDelayedDiscoverPeersArg = -1;
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    int p2pFreq;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        if (message.what == 143375) {
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            p2pStateMachine.logd(getName() + message.what);
                                        } else {
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            p2pStateMachine2.logd(getName() + message.toString());
                                        }
                                    }
                                    switch (message.what) {
                                        case 139265:
                                            String pkgName = message.getData().getString("appPkgName");
                                            int channelNum = message.arg1;
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                p2pStateMachine3.logd(getName() + " package to call DISCOVER_PEERS (channel : " + channelNum + ") -> " + pkgName);
                                            }
                                            if (WifiP2pServiceImpl.this.mAdvancedOppSender || WifiP2pServiceImpl.this.mAdvancedOppReceiver || WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid, true)) {
                                                if (!"com.android.bluetooth".equals(pkgName)) {
                                                    P2pStateMachine.this.stopLegacyWifiScan(true);
                                                    if (!WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                                                        P2pStateMachine.this.clearSupplicantServiceRequest();
                                                        if (channelNum > 0 && channelNum != WifiP2pServiceImpl.P2P_LISTEN_OFFLOADING_CHAN_NUM) {
                                                            if (P2pStateMachine.this.mPeers.clear()) {
                                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                            }
                                                            P2pStateMachine.this.mWifiNative.p2pFlush();
                                                        }
                                                        if (channelNum == WifiP2pServiceImpl.P2P_LISTEN_OFFLOADING_CHAN_NUM) {
                                                            P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                            p2pStateMachine4.logd(getName() + " Start discovery for next listen offloading ");
                                                            P2pStateMachine.this.mWifiNative.p2pFind(1, channelNum);
                                                        } else {
                                                            P2pStateMachine.this.mWifiNative.p2pFind(0, channelNum);
                                                        }
                                                        P2pStateMachine.this.replyToMessage(message, 139267);
                                                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                                                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) enabling succeeded", 1);
                                                        break;
                                                    } else {
                                                        boolean unused = WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                                                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                                                        WifiP2pServiceImpl.this.auditLog(false, "Wi-Fi Direct (P2P) enabling failed", 1);
                                                        break;
                                                    }
                                                } else {
                                                    boolean unused2 = WifiP2pServiceImpl.this.mAdvancedOppReceiver = true;
                                                    boolean unused3 = WifiP2pServiceImpl.this.mAdvancedOppInProgress = true;
                                                    P2pStateMachine.this.sendMessage(139277);
                                                    break;
                                                }
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139266, 0);
                                                break;
                                            }
                                            break;
                                        case 139268:
                                            if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                                                boolean unused4 = WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                            }
                                            P2pStateMachine.this.stopLegacyWifiScan(false);
                                            if (!P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                                                P2pStateMachine.this.replyToMessage(message, 139269, 0);
                                                break;
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139270);
                                                break;
                                            }
                                        case 139292:
                                            if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid, false)) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                    p2pStateMachine5.logd(getName() + " add service");
                                                }
                                                if (!P2pStateMachine.this.addLocalService(message.replyTo, (WifiP2pServiceInfo) message.obj)) {
                                                    P2pStateMachine.this.replyToMessage(message, 139293);
                                                    break;
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message, 139294);
                                                    break;
                                                }
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139293);
                                                break;
                                            }
                                        case 139295:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                p2pStateMachine6.logd(getName() + " remove service");
                                            }
                                            P2pStateMachine.this.removeLocalService(message.replyTo, (WifiP2pServiceInfo) message.obj);
                                            P2pStateMachine.this.replyToMessage(message, 139297);
                                            break;
                                        case 139298:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                                p2pStateMachine7.logd(getName() + " clear service");
                                            }
                                            P2pStateMachine.this.clearLocalServices(message.replyTo);
                                            P2pStateMachine.this.replyToMessage(message, 139300);
                                            break;
                                        case 139301:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                                p2pStateMachine8.logd(getName() + " add service request");
                                            }
                                            if (P2pStateMachine.this.addServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj)) {
                                                P2pStateMachine.this.replyToMessage(message, 139303);
                                                break;
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139302);
                                                break;
                                            }
                                        case 139304:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                                p2pStateMachine9.logd(getName() + " remove service request");
                                            }
                                            P2pStateMachine.this.removeServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj);
                                            P2pStateMachine.this.replyToMessage(message, 139306);
                                            break;
                                        case 139307:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled != 0) {
                                                P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                                                p2pStateMachine10.logd(getName() + " clear service request");
                                            }
                                            P2pStateMachine.this.clearServiceRequests(message.replyTo);
                                            P2pStateMachine.this.replyToMessage(message, 139309);
                                            break;
                                        case 139310:
                                            if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message.sendingUid, message.replyTo), message.sendingUid, true)) {
                                                if (!WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine p2pStateMachine11 = P2pStateMachine.this;
                                                        p2pStateMachine11.logd(getName() + " discover services");
                                                    }
                                                    if (P2pStateMachine.this.updateSupplicantServiceRequest()) {
                                                        P2pStateMachine.this.stopLegacyWifiScan(true);
                                                        P2pStateMachine.this.mWifiNative.p2pFind(0, message.arg1 * 1000);
                                                        P2pStateMachine.this.replyToMessage(message, 139312);
                                                        break;
                                                    } else {
                                                        P2pStateMachine.this.replyToMessage(message, 139311, 3);
                                                        break;
                                                    }
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message, 139311, 2);
                                                    break;
                                                }
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139311, 0);
                                                break;
                                            }
                                        case 139315:
                                            WifiP2pDevice d = (WifiP2pDevice) message.obj;
                                            if (d != null && P2pStateMachine.this.setAndPersistDeviceName(d.deviceName)) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine p2pStateMachine12 = P2pStateMachine.this;
                                                    p2pStateMachine12.logd("set device name " + d.deviceName);
                                                }
                                                P2pStateMachine.this.replyToMessage(message, 139317);
                                                WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) changing device name  succeeded");
                                                break;
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139316, 0);
                                                WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) changing device name  failed");
                                                break;
                                            }
                                        case 139318:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine13 = P2pStateMachine.this;
                                                p2pStateMachine13.logd(getName() + " delete persistent group");
                                            }
                                            P2pStateMachine.this.mGroups.remove(message.arg1);
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.updatePersistentGroup(P2pStateMachine.this.mGroups);
                                            P2pStateMachine.this.replyToMessage(message, 139320);
                                            break;
                                        case 139323:
                                            WifiP2pWfdInfo d2 = (WifiP2pWfdInfo) message.obj;
                                            if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                                                P2pStateMachine.this.replyToMessage(message, 139324, 0);
                                            } else if (d2 == null || !P2pStateMachine.this.setWfdInfo(d2)) {
                                                P2pStateMachine.this.replyToMessage(message, 139324, 0);
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139325);
                                            }
                                            if (d2 == null || !d2.isWfdEnabled()) {
                                                if (WifiP2pServiceImpl.this.mWfdDialog && WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                                    boolean unused5 = WifiP2pServiceImpl.this.mWfdDialog = false;
                                                    WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                                    break;
                                                }
                                            } else {
                                                boolean unused6 = WifiP2pServiceImpl.this.mWfdDialog = false;
                                                break;
                                            }
                                            break;
                                        case 139329:
                                            if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(message.sendingUid)) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine p2pStateMachine14 = P2pStateMachine.this;
                                                    p2pStateMachine14.logd(getName() + " start listen mode");
                                                }
                                                P2pStateMachine.this.mWifiNative.p2pFlush();
                                                if (!P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                                                    P2pStateMachine.this.replyToMessage(message, 139330);
                                                    break;
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message, 139331);
                                                    break;
                                                }
                                            } else {
                                                P2pStateMachine p2pStateMachine15 = P2pStateMachine.this;
                                                p2pStateMachine15.loge("Permission violation - no NETWORK_SETTING permission, uid = " + message.sendingUid);
                                                P2pStateMachine.this.replyToMessage(message, 139330);
                                                break;
                                            }
                                        case 139332:
                                            if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(message.sendingUid)) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine p2pStateMachine16 = P2pStateMachine.this;
                                                    p2pStateMachine16.logd(getName() + " stop listen mode");
                                                }
                                                if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                                                    P2pStateMachine.this.replyToMessage(message, 139334);
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message, 139333);
                                                }
                                                P2pStateMachine.this.mWifiNative.p2pFlush();
                                                break;
                                            } else {
                                                P2pStateMachine p2pStateMachine17 = P2pStateMachine.this;
                                                p2pStateMachine17.loge("Permission violation - no NETWORK_SETTING permission, uid = " + message.sendingUid);
                                                P2pStateMachine.this.replyToMessage(message, 139333);
                                                break;
                                            }
                                        case 139335:
                                            Bundle p2pChannels = (Bundle) message.obj;
                                            int lc = p2pChannels.getInt("lc", 0);
                                            int oc = p2pChannels.getInt("oc", 0);
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine18 = P2pStateMachine.this;
                                                p2pStateMachine18.logd(getName() + " set listen and operating channel");
                                            }
                                            if (!P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                                                P2pStateMachine.this.replyToMessage(message, 139336);
                                                break;
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message, 139337);
                                                break;
                                            }
                                        case 139339:
                                            Bundle requestBundle = new Bundle();
                                            requestBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverRequest());
                                            P2pStateMachine.this.replyToMessage(message, 139341, (Object) requestBundle);
                                            break;
                                        case 139340:
                                            Bundle selectBundle = new Bundle();
                                            selectBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverSelect());
                                            P2pStateMachine.this.replyToMessage(message, 139341, (Object) selectBundle);
                                            break;
                                        case 139368:
                                            if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing()) {
                                                P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                                                break;
                                            } else {
                                                P2pStateMachine.this.setLegacyWifiEnable(false);
                                                WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                                                break;
                                            }
                                        case 139371:
                                            P2pStateMachine.this.replyToMessage(message, 139266, 0);
                                            break;
                                        case 139372:
                                            String pkgName2 = message.getData().getString("appPkgName");
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine19 = P2pStateMachine.this;
                                                p2pStateMachine19.logd(getName() + " package to call P2P_LISTEN -> " + pkgName2);
                                            }
                                            if (!"com.android.bluetooth".equals(pkgName2)) {
                                                if (!P2pStateMachine.this.mWifiNative.p2pListen(message.arg1)) {
                                                    P2pStateMachine.this.replyToMessage(message, 139330);
                                                    break;
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message, 139331);
                                                    break;
                                                }
                                            } else {
                                                boolean unused7 = WifiP2pServiceImpl.this.mAdvancedOppSender = true;
                                                boolean unused8 = WifiP2pServiceImpl.this.mAdvancedOppInProgress = true;
                                                P2pStateMachine.this.sendMessage(139277);
                                                break;
                                            }
                                        case 139375:
                                            int isTimerOn = message.arg1;
                                            if (isTimerOn != 1) {
                                                if (isTimerOn == 2) {
                                                    WifiP2pServiceImpl.this.mAlarmManager.cancel(WifiP2pServiceImpl.this.mTimerIntent);
                                                    break;
                                                }
                                            } else {
                                                WifiP2pServiceImpl.this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 120000, WifiP2pServiceImpl.this.mTimerIntent);
                                                break;
                                            }
                                            break;
                                        case 139405:
                                            P2pStateMachine.this.mWifiNative.p2pSet("PREQ", message.getData().getString("REQ_DATA"));
                                            break;
                                        case 139406:
                                            P2pStateMachine.this.mWifiNative.p2pSet("PRESP", message.getData().getString("RESP_DATA"));
                                            break;
                                        case 139407:
                                            if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                                                boolean unused9 = WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                            }
                                            if (WifiP2pServiceImpl.this.mWfdConnected || (WifiP2pServiceImpl.this.mThisDevice.wfdInfo != null && WifiP2pServiceImpl.this.mThisDevice.wfdInfo.isWfdEnabled())) {
                                                P2pStateMachine p2pStateMachine20 = P2pStateMachine.this;
                                                p2pStateMachine20.logd(getName() + " Do not call stopLegacyWifiScan(false) because WFD is connected");
                                            } else {
                                                P2pStateMachine.this.stopLegacyWifiScan(false);
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                                            break;
                                        case 139414:
                                            int listenOffloadingTimer = message.arg1;
                                            if (listenOffloadingTimer != 1) {
                                                P2pStateMachine p2pStateMachine21 = P2pStateMachine.this;
                                                p2pStateMachine21.logd(getName() + " SET_LISTEN_OFFLOADING_TIMER " + listenOffloadingTimer);
                                                WifiP2pServiceImpl.this.mAlarmManager.cancel(WifiP2pServiceImpl.this.mLOTimerIntent);
                                                boolean unused10 = WifiP2pServiceImpl.this.mListenOffloading = false;
                                                int unused11 = WifiP2pServiceImpl.this.mLOCount = 0;
                                                break;
                                            } else {
                                                P2pStateMachine p2pStateMachine22 = P2pStateMachine.this;
                                                p2pStateMachine22.logd(getName() + " SET_LISTEN_OFFLOADING_TIMER " + listenOffloadingTimer);
                                                WifiP2pServiceImpl.this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + 31000, WifiP2pServiceImpl.this.mLOTimerIntent);
                                                boolean unused12 = WifiP2pServiceImpl.this.mListenOffloading = true;
                                                WifiP2pServiceImpl.access$1608(WifiP2pServiceImpl.this);
                                                break;
                                            }
                                        case 139415:
                                            Bundle p2pLOParams = (Bundle) message.obj;
                                            int channel = p2pLOParams.getInt("channel", 0);
                                            int period = p2pLOParams.getInt("period", 0);
                                            int interval = p2pLOParams.getInt("interval", 0);
                                            int count = p2pLOParams.getInt(ReportIdKey.KEY_COUNT, 0);
                                            if (channel <= 0) {
                                                P2pStateMachine p2pStateMachine23 = P2pStateMachine.this;
                                                p2pStateMachine23.logd(getName() + " stopP2pListenOffloading ");
                                                P2pStateMachine.this.mWifiNative.stopP2pListenOffloading();
                                                break;
                                            } else {
                                                try {
                                                    Thread.sleep(600);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                P2pStateMachine p2pStateMachine24 = P2pStateMachine.this;
                                                p2pStateMachine24.logd(getName() + " startP2pListenOffloading " + channel + " " + period + " " + interval + " " + count);
                                                P2pStateMachine.this.mWifiNative.startP2pListenOffloading(channel, period, interval, count);
                                                break;
                                            }
                                        case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                                            P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine25 = P2pStateMachine.this;
                                                p2pStateMachine25.logd(getName() + "setMiracastMode : " + message.arg1);
                                            }
                                            if (message.arg1 == 0) {
                                                boolean unused13 = WifiP2pServiceImpl.this.mWfdConnected = false;
                                                if (WifiP2pServiceImpl.this.mBleLatency) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine p2pStateMachine26 = P2pStateMachine.this;
                                                        p2pStateMachine26.logd("setMiracastMode = " + message.arg1 + ", P2P Group Freq = None, set BLE scanning latency = " + message.arg1);
                                                    }
                                                    boolean unused14 = WifiP2pServiceImpl.this.mBleLatency = false;
                                                    P2pStateMachine.this.mWifiLegacyNative.setLatencyCritical(P2pStateMachine.this.mWifiInterface, message.arg1);
                                                    break;
                                                }
                                            } else {
                                                boolean unused15 = WifiP2pServiceImpl.this.mWfdConnected = true;
                                                P2pStateMachine.this.stopLegacyWifiScan(true);
                                                if (P2pStateMachine.this.mGroup != null && (p2pFreq = P2pStateMachine.this.mGroup.getFrequency()) < 3000 && !WifiP2pServiceImpl.this.mBleLatency) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine p2pStateMachine27 = P2pStateMachine.this;
                                                        p2pStateMachine27.logd("setMiracastMode = " + message.arg1 + ", P2P Group Freq = " + p2pFreq + " hz, set BLE scanning latency = " + message.arg1);
                                                    }
                                                    boolean unused16 = WifiP2pServiceImpl.this.mBleLatency = true;
                                                    P2pStateMachine.this.mWifiLegacyNative.setLatencyCritical(P2pStateMachine.this.mWifiInterface, message.arg1);
                                                    break;
                                                }
                                            }
                                            break;
                                        case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                                            boolean blocked = message.arg1 == 1;
                                            if (WifiP2pServiceImpl.this.mDiscoveryBlocked != blocked) {
                                                boolean unused17 = WifiP2pServiceImpl.this.mDiscoveryBlocked = blocked;
                                                if (blocked && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                                    P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                    boolean unused18 = WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                                                }
                                                if (!blocked && WifiP2pServiceImpl.this.mDiscoveryPostponed && !WifiP2pServiceImpl.this.mWfdConnected) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        Log.d(WifiP2pServiceImpl.TAG, "p2pFind() called by BLOCK_DISCOVERY disabled");
                                                    }
                                                    boolean unused19 = WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                                    P2pStateMachine.this.mWifiNative.p2pFind(120);
                                                }
                                                if (blocked) {
                                                    if (message.obj != null) {
                                                        try {
                                                            ((StateMachine) message.obj).sendMessage(message.arg2);
                                                            break;
                                                        } catch (Exception e2) {
                                                            P2pStateMachine p2pStateMachine28 = P2pStateMachine.this;
                                                            p2pStateMachine28.loge("unable to send BLOCK_DISCOVERY response: " + e2);
                                                            break;
                                                        }
                                                    } else {
                                                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                        break;
                                                    }
                                                }
                                            }
                                            break;
                                        case WifiP2pServiceImpl.ENABLE_P2P /*143376*/:
                                            break;
                                        case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                                            if (P2pStateMachine.this.mPeers.clear()) {
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                            }
                                            if (P2pStateMachine.this.mGroups.clear()) {
                                                P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                                            }
                                            P2pStateMachine.this.clearServicesForAllClients();
                                            P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                            P2pStateMachine.this.handleGroupCreationFailure();
                                            P2pStateMachine.this.mWifiMonitor.stopMonitoring(P2pStateMachine.this.mInterfaceName);
                                            P2pStateMachine.this.mWifiNative.teardownInterface();
                                            P2pStateMachine p2pStateMachine29 = P2pStateMachine.this;
                                            p2pStateMachine29.transitionTo(p2pStateMachine29.mP2pDisablingState);
                                            break;
                                        case WifiP2pServiceImpl.REMOVE_CLIENT_INFO /*143378*/:
                                            if (message.obj instanceof IBinder) {
                                                IBinder b = (IBinder) message.obj;
                                                P2pStateMachine p2pStateMachine30 = P2pStateMachine.this;
                                                p2pStateMachine30.clearClientInfo((Messenger) WifiP2pServiceImpl.this.mClientChannelList.get(b));
                                                WifiP2pServiceImpl.this.mClientChannelList.remove(b);
                                                break;
                                            }
                                            break;
                                        case WifiP2pServiceImpl.SET_COUNTRY_CODE /*143379*/:
                                            String countryCode = ((String) message.obj).toUpperCase(Locale.ROOT);
                                            if (Settings.Global.getInt(WifiP2pServiceImpl.this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
                                                if (WifiP2pServiceImpl.this.mLastSetCountryCode != null) {
                                                    boolean equals = countryCode.equals(WifiP2pServiceImpl.this.mLastSetCountryCode);
                                                    break;
                                                }
                                            } else {
                                                Log.e(WifiP2pServiceImpl.TAG, "Airplane mode : skipped SET_COUNTRY_CODE");
                                                break;
                                            }
                                            break;
                                        case 147458:
                                            P2pStateMachine.this.loge("Unexpected loss of p2p socket connection");
                                            P2pStateMachine p2pStateMachine31 = P2pStateMachine.this;
                                            p2pStateMachine31.transitionTo(p2pStateMachine31.mP2pDisabledState);
                                            break;
                                        case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                                            if (message.obj != null) {
                                                WifiP2pDevice device = (WifiP2pDevice) message.obj;
                                                if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress != null && !WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(device.deviceAddress)) {
                                                    if (Build.VERSION.SDK_INT < 29 && device.contactInfoHash != null) {
                                                        P2pStateMachine.this.convertDeviceNameNSetIconViaContact(device);
                                                    }
                                                    P2pStateMachine.this.mPeers.updateSupplicantDetails(device);
                                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                                    break;
                                                }
                                            } else {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                break;
                                            }
                                        case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                                            if (message.obj != null) {
                                                if (P2pStateMachine.this.mPeers.remove(((WifiP2pDevice) message.obj).deviceAddress) != null) {
                                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                                    break;
                                                }
                                            } else {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                break;
                                            }
                                            break;
                                        case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                                            if (WifiP2pServiceImpl.this.mListenOffloading) {
                                                if (!P2pStateMachine.this.mPeers.isEmpty()) {
                                                    try {
                                                        Thread.sleep(300);
                                                    } catch (InterruptedException e3) {
                                                        e3.printStackTrace();
                                                    }
                                                    P2pStateMachine p2pStateMachine32 = P2pStateMachine.this;
                                                    p2pStateMachine32.logd(getName() + " Start listen offloading!");
                                                    P2pStateMachine.this.mWifiNative.startP2pListenOffloading(1, 500, 5000, 6);
                                                }
                                                P2pStateMachine p2pStateMachine33 = P2pStateMachine.this;
                                                p2pStateMachine33.logd(getName() + " Set listen offloading timer!");
                                                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(139414, 1);
                                                boolean unused20 = WifiP2pServiceImpl.this.mListenOffloading = false;
                                            }
                                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                                            if (!WifiP2pServiceImpl.this.mWfdConnected) {
                                                P2pStateMachine.this.stopLegacyWifiScan(false);
                                            }
                                            WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) disabling succeeded", 1);
                                            break;
                                        case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine34 = P2pStateMachine.this;
                                                p2pStateMachine34.logd(getName() + " receive service response");
                                            }
                                            if (message.obj != null) {
                                                for (WifiP2pServiceResponse resp : (List) message.obj) {
                                                    resp.setSrcDevice(P2pStateMachine.this.mPeers.get(resp.getSrcDevice().deviceAddress));
                                                    P2pStateMachine.this.sendServiceResponse(resp);
                                                }
                                                break;
                                            } else {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                break;
                                            }
                                        case WifiP2pMonitor.P2P_PERSISTENT_PSK_FAIL_EVENT:
                                            String dataString = (String) message.obj;
                                            if (dataString != null) {
                                                try {
                                                    int networkId = Integer.parseInt(dataString.substring(dataString.indexOf("=") + 1));
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine p2pStateMachine35 = P2pStateMachine.this;
                                                        p2pStateMachine35.logd(getName() + " delete persistent group(PERSISTENT_PSK_FAIL), networkId : " + networkId);
                                                    }
                                                    P2pStateMachine.this.mGroups.remove(networkId);
                                                    break;
                                                } catch (NumberFormatException e4) {
                                                    break;
                                                }
                                            }
                                            break;
                                        case WifiP2pMonitor.P2P_P2P_SCONNECT_PROBE_REQ_EVENT:
                                            Intent req_intent = new Intent("android.net.wifi.p2p.SCONNECT_PROBE_REQ");
                                            req_intent.addFlags(67108864);
                                            req_intent.putExtra("probeReq", new String((String) message.obj));
                                            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(req_intent, UserHandle.ALL);
                                            break;
                                        default:
                                            return false;
                                    }
                                    return true;
                                }

                                public void exit() {
                                    int unused = P2pStateMachine.this.mP2pState = 1;
                                    P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                                    P2pStateMachine.this.sendP2pStateChangedBroadcast(false);
                                    WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, (String) null, (String) null);
                                    WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(false);
                                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                    boolean unused2 = WifiP2pServiceImpl.this.mDiscoveryStarted = false;
                                    boolean unused3 = WifiP2pServiceImpl.this.mWfdConnected = false;
                                    if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                        WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                        WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                        AlertDialog unused4 = WifiP2pServiceImpl.this.mInvitationDialog = null;
                                    }
                                    String unused5 = WifiP2pServiceImpl.this.mLastSetCountryCode = null;
                                    P2pStateMachine.this.mWifiNative.p2pSet("pre_auth", "0");
                                    WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) enabling succeeded", 1);
                                }
                            }

                            class InactiveState extends State {
                                InactiveState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    if (P2pStateMachine.this.mSavedPeerConfig != null && !WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin) {
                                        P2pStateMachine.this.mSavedPeerConfig.invalidate();
                                    }
                                    boolean unused = P2pStateMachine.this.mIsInactiveState = true;
                                    if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                        P2pStateMachine.this.sendMessage(139372, 20);
                                        P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.P2P_ADVOPP_LISTEN_TIMEOUT, 20000);
                                    }
                                    if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        WifiP2pServiceImpl.this.mConnReqInfo.set(p2pStateMachine.fetchCurrentDeviceDetails(p2pStateMachine.mSavedPeerConfig), "com.android.bluetooth.advopp", 0, 0, 1, P2pStateMachine.this.mWifiNative.p2pGetManufacturer(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mWifiNative.p2pGetDeviceType(P2pStateMachine.this.mSavedPeerConfig.deviceAddress));
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine.this.logd("AdvancedOpp - remove autonomous group and join the found group");
                                        }
                                        boolean unused2 = WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin = false;
                                        boolean unused3 = WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                        p2pStateMachine2.p2pConnectWithPinDisplay(p2pStateMachine2.mSavedPeerConfig);
                                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                        p2pStateMachine3.transitionTo(p2pStateMachine3.mGroupNegotiationState);
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    boolean ret;
                                    boolean result;
                                    Message message2 = message;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        if (message2.what == 143375) {
                                            P2pStateMachine.this.logd(getName() + message2.what);
                                        } else {
                                            P2pStateMachine.this.logd(getName() + message.toString());
                                        }
                                    }
                                    boolean z = false;
                                    switch (message2.what) {
                                        case 139268:
                                            if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                                                boolean unused = WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                            }
                                            if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                                                P2pStateMachine.this.mWifiNative.p2pFlush();
                                                String unused2 = WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                                                P2pStateMachine.this.replyToMessage(message2, 139270);
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message2, 139269, 0);
                                            }
                                            P2pStateMachine.this.stopLegacyWifiScan(false);
                                            return true;
                                        case 139271:
                                            if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                                P2pStateMachine.this.replyToMessage(message2, 139272);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " sending connect");
                                            }
                                            Bundle bundle = message.getData();
                                            WifiP2pConfig config = (WifiP2pConfig) bundle.getParcelable("wifiP2pConfig");
                                            String pkgName = bundle.getString("appPkgName");
                                            WifiP2pDevice peerDev = P2pStateMachine.this.fetchCurrentDeviceDetails(config);
                                            int persistent = 0;
                                            int join = (peerDev == null || !peerDev.isGroupOwner()) ? 0 : 1;
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " package to call connect -> " + pkgName);
                                            }
                                            if (WifiP2pServiceImpl.this.mAdvancedOppSender || WifiP2pServiceImpl.this.mAdvancedOppReceiver || WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message2.sendingUid, message2.replyTo), message2.sendingUid, false)) {
                                                boolean isConnectFailed = false;
                                                if (P2pStateMachine.this.isConfigValidAsGroup(config)) {
                                                    boolean unused3 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                    P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                    if (P2pStateMachine.this.mWifiNative.p2pGroupAdd(config, true)) {
                                                        WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(3, config);
                                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                                        p2pStateMachine.transitionTo(p2pStateMachine.mGroupNegotiationState);
                                                    } else {
                                                        P2pStateMachine.this.loge("Cannot join a group with config.");
                                                        isConnectFailed = true;
                                                        P2pStateMachine.this.replyToMessage(message2, 139272);
                                                        WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address  using Wi-Fi Direct (P2P) failed");
                                                    }
                                                } else if (P2pStateMachine.this.isConfigInvalid(config)) {
                                                    P2pStateMachine.this.loge("Dropping connect request " + config);
                                                    P2pStateMachine.this.replyToMessage(message2, 139272);
                                                    WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address  using Wi-Fi Direct (P2P) failed");
                                                    return true;
                                                } else {
                                                    P2pStateMachine.this.mPeers.updateGroupCapability(config.deviceAddress, P2pStateMachine.this.mWifiNative.getGroupCapability(config.deviceAddress));
                                                    if (P2pStateMachine.this.mWifiNative.p2pPeer(config.deviceAddress) == null) {
                                                        P2pStateMachine.this.loge("Dropping connect requeset : peer is flushed " + config);
                                                        P2pStateMachine.this.replyToMessage(message2, 139272);
                                                        WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address  using Wi-Fi Direct (P2P) failed");
                                                        return true;
                                                    }
                                                    boolean unused4 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                                    boolean unused5 = WifiP2pServiceImpl.this.mWfdDialog = true;
                                                    boolean unused6 = WifiP2pServiceImpl.this.mValidFreqConflict = true;
                                                    boolean isResp = P2pStateMachine.this.mSavedPeerConfig != null && config.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    WifiP2pConfig unused7 = P2pStateMachine.this.mSavedPeerConfig = config;
                                                    boolean unused8 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                    P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                    if (P2pStateMachine.this.reinvokePersistentGroup(config)) {
                                                        WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(1, config);
                                                        persistent = 1;
                                                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                                        p2pStateMachine2.transitionTo(p2pStateMachine2.mGroupNegotiationState);
                                                    } else if (isResp) {
                                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            P2pStateMachine.this.logd("prov disc is not needed!!");
                                                        }
                                                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                        p2pStateMachine3.p2pConnectWithPinDisplay(p2pStateMachine3.mSavedPeerConfig);
                                                        P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                        p2pStateMachine4.transitionTo(p2pStateMachine4.mGroupNegotiationState);
                                                    } else {
                                                        WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(0, config);
                                                        P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                        p2pStateMachine5.transitionTo(p2pStateMachine5.mProvisionDiscoveryState);
                                                    }
                                                }
                                                if (isConnectFailed) {
                                                    return true;
                                                }
                                                WifiP2pConfig unused9 = P2pStateMachine.this.mSavedPeerConfig = config;
                                                P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + " using Wi-Fi Direct (P2P) succeeded");
                                                WifiP2pServiceImpl.this.mConnReqInfo.set(peerDev, pkgName, 0, persistent, join, P2pStateMachine.this.mWifiNative.p2pGetManufacturer(config.deviceAddress), P2pStateMachine.this.mWifiNative.p2pGetDeviceType(config.deviceAddress));
                                                P2pStateMachine.this.replyToMessage(message2, 139273);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139272);
                                            WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address  using Wi-Fi Direct (P2P) failed");
                                            return true;
                                        case 139277:
                                            if (WifiP2pServiceImpl.this.mAdvancedOppSender || WifiP2pServiceImpl.this.mAdvancedOppReceiver || WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message2.sendingUid, message2.replyTo), message2.sendingUid, false)) {
                                                if (!WifiP2pServiceImpl.this.mAdvancedOppReceiver && !WifiP2pServiceImpl.this.mAdvancedOppSender) {
                                                    boolean unused10 = WifiP2pServiceImpl.this.mAutonomousGroup = true;
                                                }
                                                if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                                    P2pStateMachine.this.replyToMessage(message2, 139278, 0);
                                                    return true;
                                                }
                                                int netId = message2.arg1;
                                                WifiP2pConfig config2 = (WifiP2pConfig) message2.obj;
                                                if (config2 != null) {
                                                    if (P2pStateMachine.this.isConfigValidAsGroup(config2)) {
                                                        WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(3, config2);
                                                        ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(config2, false);
                                                    } else {
                                                        ret = false;
                                                    }
                                                } else if (netId != -2) {
                                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(2, (WifiP2pConfig) null);
                                                    ret = P2pStateMachine.this.setP2pGroupForSamsung();
                                                } else if (P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress) != -1) {
                                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(1, (WifiP2pConfig) null);
                                                    ret = P2pStateMachine.this.setP2pGroupForSamsung();
                                                } else {
                                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(2, (WifiP2pConfig) null);
                                                    ret = P2pStateMachine.this.setP2pGroupForSamsung();
                                                }
                                                if (ret) {
                                                    P2pStateMachine.this.replyToMessage(message2, 139279);
                                                    P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                    p2pStateMachine6.transitionTo(p2pStateMachine6.mGroupNegotiationState);
                                                    return true;
                                                }
                                                P2pStateMachine.this.replyToMessage(message2, 139278, 0);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139278, 0);
                                            return true;
                                        case 139329:
                                            if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(message2.sendingUid)) {
                                                P2pStateMachine.this.loge("Permission violation - no NETWORK_SETTING permission, uid = " + message2.sendingUid);
                                                P2pStateMachine.this.replyToMessage(message2, 139330);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " start listen mode");
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pFlush();
                                            if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139331);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139330);
                                            return true;
                                        case 139332:
                                            if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(message2.sendingUid)) {
                                                P2pStateMachine.this.loge("Permission violation - no NETWORK_SETTING permission, uid = " + message2.sendingUid);
                                                P2pStateMachine.this.replyToMessage(message2, 139333);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " stop listen mode");
                                            }
                                            if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139334);
                                            } else {
                                                P2pStateMachine.this.replyToMessage(message2, 139333);
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pFlush();
                                            return true;
                                        case 139335:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments(s)");
                                                return true;
                                            }
                                            Bundle p2pChannels = (Bundle) message2.obj;
                                            int lc = p2pChannels.getInt("lc", 0);
                                            int oc = p2pChannels.getInt("oc", 0);
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                                            }
                                            if (P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139337);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139336);
                                            return true;
                                        case 139342:
                                            String handoverSelect = null;
                                            if (message2.obj != null) {
                                                handoverSelect = ((Bundle) message2.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                                            }
                                            if (handoverSelect == null || !P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(handoverSelect)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139345);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139344);
                                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                            p2pStateMachine7.transitionTo(p2pStateMachine7.mGroupCreatingState);
                                            return true;
                                        case 139343:
                                            String handoverRequest = null;
                                            if (message2.obj != null) {
                                                handoverRequest = ((Bundle) message2.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                                            }
                                            if (handoverRequest == null || !P2pStateMachine.this.mWifiNative.responderReportNfcHandover(handoverRequest)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139345);
                                                return true;
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139344);
                                            P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                            p2pStateMachine8.transitionTo(p2pStateMachine8.mGroupCreatingState);
                                            return true;
                                        case 139371:
                                            int timeout = message2.arg1;
                                            P2pStateMachine.this.stopLegacyWifiScan(true);
                                            if (P2pStateMachine.this.mPeers.clear()) {
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pFlush();
                                            if (timeout == -999) {
                                                result = P2pStateMachine.this.mWifiNative.p2pFind(5, -999);
                                            } else {
                                                result = P2pStateMachine.this.mWifiNative.p2pFind(timeout);
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139267);
                                            P2pStateMachine.this.logd(getName() + " p2pFlushFind result : " + result);
                                            return true;
                                        case 139376:
                                            String pkgName2 = message.getData().getString("appPkgName");
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " package to call requestNfcConnect -> " + pkgName2 + " with arg : " + message2.arg1);
                                            }
                                            if (WifiP2pServiceImpl.this.mConnReqInfo.peerDev == null && WifiP2pServiceImpl.this.mConnReqInfo.pkgName == null) {
                                                String unused11 = WifiP2pServiceImpl.this.mConnReqInfo.pkgName = pkgName2;
                                            }
                                            if (message2.arg1 != 1) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                            p2pStateMachine9.transitionTo(p2pStateMachine9.mNfcProvisionState);
                                            return true;
                                        case 139377:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " sending connect");
                                            }
                                            if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                                P2pStateMachine.this.replyToMessage(message2, 139272);
                                                return true;
                                            }
                                            int i = message2.arg1;
                                            boolean unused12 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                            boolean unused13 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                            boolean unused14 = WifiP2pServiceImpl.this.mValidFreqConflict = true;
                                            if (((WifiP2pConfig) message2.obj).netId == 1) {
                                                P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139273);
                                            P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                                            p2pStateMachine10.transitionTo(p2pStateMachine10.mGroupNegotiationState);
                                            WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + " using Wi-Fi Direct (P2P) succeeded");
                                            return true;
                                        case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                                            WifiP2pConfig config3 = (WifiP2pConfig) message2.obj;
                                            if (P2pStateMachine.this.isConfigInvalid(config3)) {
                                                P2pStateMachine.this.loge("Dropping GO neg request " + config3);
                                                return true;
                                            }
                                            WifiP2pConfig unused15 = P2pStateMachine.this.mSavedPeerConfig = config3;
                                            boolean unused16 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                            boolean unused17 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(0, config3);
                                            WifiP2pDevice peerDev2 = P2pStateMachine.this.fetchCurrentDeviceDetails(config3);
                                            String manufacturer = P2pStateMachine.this.mWifiNative.p2pGetManufacturer(config3.deviceAddress);
                                            String type = P2pStateMachine.this.mWifiNative.p2pGetDeviceType(config3.deviceAddress);
                                            String pkgName3 = null;
                                            List<ActivityManager.RunningTaskInfo> tasks = WifiP2pServiceImpl.this.mActivityMgr.getRunningTasks(1);
                                            if (tasks.size() != 0) {
                                                pkgName3 = tasks.get(0).baseActivity.getPackageName();
                                            }
                                            if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                                pkgName3 = "com.android.bluetooth.advopp";
                                            }
                                            WifiP2pServiceImpl.this.mConnReqInfo.set(peerDev2, pkgName3, 1, 0, 0, manufacturer, type);
                                            P2pStateMachine p2pStateMachine11 = P2pStateMachine.this;
                                            if (p2pStateMachine11.sendConnectNoticeToApp(p2pStateMachine11.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mSavedPeerConfig)) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine12 = P2pStateMachine.this;
                                            p2pStateMachine12.transitionTo(p2pStateMachine12.mUserAuthorizingNegotiationRequestState);
                                            return true;
                                        case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                                                return true;
                                            }
                                            WifiP2pGroup unused18 = P2pStateMachine.this.mGroup = (WifiP2pGroup) message2.obj;
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " group started");
                                            }
                                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                                            }
                                            if (!P2pStateMachine.this.mGroup.mStaticIp.isEmpty()) {
                                                P2pStateMachine.this.logd("set staticIP from EAPOL-Key " + P2pStateMachine.this.mGroup.mStaticIp);
                                                P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(P2pStateMachine.this.mGroup.mStaticIp));
                                            } else if (P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp != 0 && !P2pStateMachine.this.mGroup.mGroupOwnerStaticIp.isEmpty()) {
                                                P2pStateMachine.this.mGroup.mGroupOwnerStaticIp = "";
                                            }
                                            if (P2pStateMachine.this.mGroup.getNetworkId() == -2 || WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                                boolean unused19 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                P2pStateMachine.this.deferMessage(message2);
                                                P2pStateMachine p2pStateMachine13 = P2pStateMachine.this;
                                                p2pStateMachine13.transitionTo(p2pStateMachine13.mGroupNegotiationState);
                                                return true;
                                            }
                                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                            return true;
                                        case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT:
                                            if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                                return true;
                                            }
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                                                return true;
                                            }
                                            WifiP2pGroup group = (WifiP2pGroup) message2.obj;
                                            WifiP2pDevice owner = group.getOwner();
                                            if (owner == null) {
                                                int id = group.getNetworkId();
                                                if (id < 0) {
                                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                                    return true;
                                                }
                                                String addr = P2pStateMachine.this.mGroups.getOwnerAddr(id);
                                                if (addr != null) {
                                                    group.setOwner(new WifiP2pDevice(addr));
                                                    owner = group.getOwner();
                                                } else {
                                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                                    return true;
                                                }
                                            }
                                            WifiP2pConfig config4 = new WifiP2pConfig();
                                            config4.deviceAddress = group.getOwner().deviceAddress;
                                            String unused20 = P2pStateMachine.this.mSelectedP2pGroupAddress = config4.deviceAddress;
                                            if (P2pStateMachine.this.isConfigInvalid(config4)) {
                                                P2pStateMachine.this.loge("Dropping invitation request " + config4);
                                                return true;
                                            }
                                            WifiP2pConfig unused21 = P2pStateMachine.this.mSavedPeerConfig = config4;
                                            if (owner != null) {
                                                WifiP2pDevice wifiP2pDevice = P2pStateMachine.this.mPeers.get(owner.deviceAddress);
                                                WifiP2pDevice owner2 = wifiP2pDevice;
                                                if (wifiP2pDevice != null) {
                                                    if (owner2.wpsPbcSupported()) {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                                    } else if (owner2.wpsKeypadSupported()) {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                                    } else if (owner2.wpsDisplaySupported()) {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                                    }
                                                }
                                            }
                                            boolean unused22 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                            boolean unused23 = WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.startConnectionEvent(0, config4);
                                            P2pStateMachine p2pStateMachine14 = P2pStateMachine.this;
                                            if (!p2pStateMachine14.sendConnectNoticeToApp(p2pStateMachine14.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mSavedPeerConfig)) {
                                                P2pStateMachine p2pStateMachine15 = P2pStateMachine.this;
                                                p2pStateMachine15.transitionTo(p2pStateMachine15.mUserAuthorizingInviteRequestState);
                                            }
                                            P2pStateMachine p2pStateMachine16 = P2pStateMachine.this;
                                            p2pStateMachine16.transitionTo(p2pStateMachine16.mUserAuthorizingInviteRequestState);
                                            return true;
                                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                return true;
                                            }
                                            WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message2.obj;
                                            WifiP2pDevice device = provDisc.device;
                                            P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = provDisc.device.candidateStaticIp;
                                            if (device == null) {
                                                P2pStateMachine.this.loge("Device entry is null");
                                                return true;
                                            }
                                            WifiP2pConfig unused24 = P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = provDisc.device.deviceAddress;
                                            if (message2.what == 147491) {
                                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("Keypad prov disc request");
                                                }
                                            } else if (message2.what == 147492) {
                                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                                P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("Display prov disc request");
                                                }
                                                P2pStateMachine.this.notifyInvitationReceived();
                                                P2pStateMachine p2pStateMachine17 = P2pStateMachine.this;
                                                if (!p2pStateMachine17.sendConnectNoticeToApp(p2pStateMachine17.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mSavedPeerConfig)) {
                                                    P2pStateMachine p2pStateMachine18 = P2pStateMachine.this;
                                                    p2pStateMachine18.transitionTo(p2pStateMachine18.mUserAuthorizingNegotiationRequestState);
                                                }
                                            } else {
                                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("PBC prov disc request");
                                                }
                                            }
                                            WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                            if (dev != null) {
                                                WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                                                if (dev.isGroupOwner() && provDisc.device.isGroupOwner()) {
                                                    z = true;
                                                }
                                                boolean unused25 = wifiP2pServiceImpl.mJoinExistingGroup = z;
                                            }
                                            if (!WifiP2pServiceImpl.this.mJoinExistingGroup) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine19 = P2pStateMachine.this;
                                            if (p2pStateMachine19.sendConnectNoticeToApp(p2pStateMachine19.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mSavedPeerConfig)) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine20 = P2pStateMachine.this;
                                            p2pStateMachine20.transitionTo(p2pStateMachine20.mUserAuthorizingInviteRequestState);
                                            return true;
                                        default:
                                            return false;
                                    }
                                }

                                public void exit() {
                                    boolean unused = P2pStateMachine.this.mIsInactiveState = false;
                                }
                            }

                            class GroupCreatingState extends State {
                                GroupCreatingState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                    p2pStateMachine.sendMessageDelayed(p2pStateMachine.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.access$16204(), 0), 120000);
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + message.toString());
                                    }
                                    switch (message.what) {
                                        case 139265:
                                            P2pStateMachine.this.replyToMessage(message, 139266, 2);
                                            return true;
                                        case 139268:
                                        case 139372:
                                            return true;
                                        case 139274:
                                            P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.endConnectionEvent(3);
                                            P2pStateMachine.this.stopLegacyWifiScan(false);
                                            if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                P2pStateMachine.this.mWifiNative.p2pReject(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
                                            }
                                            P2pStateMachine.this.handleGroupCreationFailure();
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            p2pStateMachine2.transitionTo(p2pStateMachine2.mInactiveState);
                                            P2pStateMachine.this.replyToMessage(message, 139276);
                                            return true;
                                        case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                                            if (WifiP2pServiceImpl.sGroupCreatingTimeoutIndex != message.arg1) {
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("Group negotiation timed out");
                                            }
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.endConnectionEvent(2);
                                            P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                            if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                String unused = p2pStateMachine3.mConnectedDevAddr = p2pStateMachine3.mSavedPeerConfig.deviceAddress;
                                            }
                                            P2pStateMachine.this.handleGroupCreationFailure();
                                            P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                            p2pStateMachine4.transitionTo(p2pStateMachine4.mInactiveState);
                                            return true;
                                        case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                                            if (message.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                return true;
                                            }
                                            WifiP2pDevice device = (WifiP2pDevice) message.obj;
                                            if (P2pStateMachine.this.mSavedPeerConfig == null || P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                    p2pStateMachine5.logd("Add device to lost list " + device);
                                                }
                                                P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                p2pStateMachine6.logd("mSavedPeerConfig " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + "device " + device.deviceAddress);
                                            }
                                            return false;
                                        case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                                            boolean unused2 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                            p2pStateMachine7.transitionTo(p2pStateMachine7.mGroupNegotiationState);
                                            return true;
                                        case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                                            if (!WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                return true;
                                            }
                                            P2pStateMachine.this.logd("skip resuming legacy wifi scan while group creating");
                                            return true;
                                        case 147527:
                                            WifiP2pProvDiscEvent userReject = (WifiP2pProvDiscEvent) message.obj;
                                            if (userReject != null && P2pStateMachine.this.mSavedPeerConfig != null && !userReject.device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                                WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                                WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                                AlertDialog unused3 = WifiP2pServiceImpl.this.mInvitationDialog = null;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("Peer cancelled connection establishment while creating group");
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                            P2pStateMachine.this.handleGroupCreationFailure();
                                            P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                            p2pStateMachine8.transitionTo(p2pStateMachine8.mInactiveState);
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            }

                            class UserAuthorizingNegotiationRequestState extends State {
                                UserAuthorizingNegotiationRequestState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine.this.logd("Accepted without notification since it's from advanced opp");
                                        }
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                                    } else if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0 || TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                        P2pStateMachine.this.notifyInvitationReceived();
                                        P2pStateMachine.this.soundNotification();
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName() + message.toString());
                                    }
                                    boolean join = false;
                                    switch (message.what) {
                                        case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                                            if (P2pStateMachine.this.mSavedPeerConfig == null) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("Abort group creation since mSavedPeerConfig NULL");
                                                }
                                                P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                                P2pStateMachine.this.handleGroupCreationFailure();
                                                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                                p2pStateMachine.transitionTo(p2pStateMachine.mInactiveState);
                                                break;
                                            } else {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("Accept connection request");
                                                }
                                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                                p2pStateMachine2.p2pConnectWithPinDisplay(p2pStateMachine2.mSavedPeerConfig);
                                                P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                                    P2pStateMachine.this.sendP2pRequestChangedBroadcast(true);
                                                }
                                                P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                p2pStateMachine3.transitionTo(p2pStateMachine3.mGroupNegotiationState);
                                                break;
                                            }
                                        case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                                            if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("User rejected negotiation " + P2pStateMachine.this.mSavedPeerConfig);
                                                }
                                                P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                WifiP2pDevice dev = p2pStateMachine4.fetchCurrentDeviceDetails(p2pStateMachine4.mSavedPeerConfig);
                                                if ((dev != null && dev.isGroupOwner()) || WifiP2pServiceImpl.this.mJoinExistingGroup) {
                                                    join = true;
                                                }
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("User rejected negotiation, join =  " + join);
                                                }
                                                boolean unused = WifiP2pServiceImpl.this.userRejected = true;
                                                if (join) {
                                                    P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                                    if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                                                        P2pStateMachine.this.mWifiNative.p2pReject(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                        P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
                                                    }
                                                    P2pStateMachine.this.mWifiNative.p2pFlush();
                                                    P2pStateMachine.this.mWifiNative.p2pFind();
                                                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                                } else {
                                                    P2pStateMachine.this.mWifiNative.p2pReject(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                    p2pStateMachine5.p2pConnectWithPinDisplay(p2pStateMachine5.mSavedPeerConfig);
                                                }
                                                WifiP2pConfig unused2 = P2pStateMachine.this.mSavedPeerConfig = null;
                                            } else {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("User rejected negotiation - mSavedPeerConfig NULL");
                                                }
                                                P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                                                P2pStateMachine.this.handleGroupCreationFailure();
                                            }
                                            P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                            p2pStateMachine6.transitionTo(p2pStateMachine6.mInactiveState);
                                            break;
                                        case WifiP2pServiceImpl.PEER_CONNECTION_USER_CONFIRM /*143367*/:
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                            P2pStateMachine.this.mWifiNative.p2pConnect(P2pStateMachine.this.mSavedPeerConfig, WifiP2pServiceImpl.FORM_GROUP.booleanValue());
                                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                            p2pStateMachine7.transitionTo(p2pStateMachine7.mGroupNegotiationState);
                                            break;
                                        default:
                                            return false;
                                    }
                                    return true;
                                }

                                public void exit() {
                                }
                            }

                            class UserAuthorizingInviteRequestState extends State {
                                UserAuthorizingInviteRequestState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine.this.logd("Accepted without notification since it's from advanced opp");
                                        }
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                                        return;
                                    }
                                    P2pStateMachine.this.notifyInvitationReceived();
                                    P2pStateMachine.this.soundNotification();
                                }

                                /* JADX WARNING: Code restructure failed: missing block: B:13:0x0074, code lost:
                                    if (com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$14700(r1, com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.access$7400(r1)) == false) goto L_0x0076;
                                 */
                                /* Code decompiled incorrectly, please refer to instructions dump. */
                                public boolean processMessage(android.os.Message r5) {
                                    /*
                                        r4 = this;
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x0022
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r2 = r4.getName()
                                        r1.append(r2)
                                        java.lang.String r2 = r5.toString()
                                        r1.append(r2)
                                        java.lang.String r1 = r1.toString()
                                        r0.logd(r1)
                                    L_0x0022:
                                        r0 = 1
                                        int r1 = r5.what
                                        switch(r1) {
                                            case 143362: goto L_0x0056;
                                            case 143363: goto L_0x002a;
                                            default: goto L_0x0028;
                                        }
                                    L_0x0028:
                                        r1 = 0
                                        return r1
                                    L_0x002a:
                                        boolean r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r1 == 0) goto L_0x004c
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r2 = new java.lang.StringBuilder
                                        r2.<init>()
                                        java.lang.String r3 = "User rejected invitation "
                                        r2.append(r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r3 = r3.mSavedPeerConfig
                                        r2.append(r3)
                                        java.lang.String r2 = r2.toString()
                                        r1.logd(r2)
                                    L_0x004c:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$InactiveState r2 = r1.mInactiveState
                                        r1.transitionTo(r2)
                                        goto L_0x00a0
                                    L_0x0056:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r1 = r1.mWifiNative
                                        r1.p2pStopFind()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r1 = r1.mSavedPeerConfig
                                        int r1 = r1.netId
                                        r2 = -1
                                        if (r1 == r2) goto L_0x0076
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r2 = r1.mSavedPeerConfig
                                        boolean r1 = r1.reinvokePersistentGroup(r2)
                                        if (r1 != 0) goto L_0x007f
                                    L_0x0076:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r2 = r1.mSavedPeerConfig
                                        r1.p2pConnectWithPinDisplay(r2)
                                    L_0x007f:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pDeviceList r1 = r1.mPeers
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r2 = r2.mSavedPeerConfig
                                        java.lang.String r2 = r2.deviceAddress
                                        r3 = 1
                                        r1.updateStatus(r2, r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1.sendPeersChangedBroadcast()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$GroupNegotiationState r2 = r1.mGroupNegotiationState
                                        r1.transitionTo(r2)
                                    L_0x00a0:
                                        return r0
                                    */
                                    throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.UserAuthorizingInviteRequestState.processMessage(android.os.Message):boolean");
                                }

                                public void exit() {
                                }
                            }

                            class ProvisionDiscoveryState extends State {
                                ProvisionDiscoveryState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.stopLegacyWifiScan(true);
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName() + message.toString());
                                    }
                                    int i = message.what;
                                    if (!(i == 139268 || i == 139372)) {
                                        boolean z = false;
                                        if (i != 147495) {
                                            switch (i) {
                                                case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                                                    WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                                                    if (provDisc.device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                                                        P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = provDisc.device.candidateStaticIp;
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            P2pStateMachine.this.logd("PBC prov disc request");
                                                        }
                                                        WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                        WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                                                        if (dev != null && dev.isGroupOwner() && provDisc.device.isGroupOwner()) {
                                                            z = true;
                                                        }
                                                        boolean unused = wifiP2pServiceImpl.mJoinExistingGroup = z;
                                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                                        p2pStateMachine.p2pConnectWithPinDisplay(p2pStateMachine.mSavedPeerConfig);
                                                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                        P2pStateMachine.this.sendPeersChangedBroadcast();
                                                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                                        p2pStateMachine2.transitionTo(p2pStateMachine2.mGroupNegotiationState);
                                                        break;
                                                    }
                                                    break;
                                                case WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT:
                                                    if (message.obj != null) {
                                                        WifiP2pDevice device = ((WifiP2pProvDiscEvent) message.obj).device;
                                                        if (device != null) {
                                                            P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = device.candidateStaticIp;
                                                        }
                                                        if ((device == null || device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                                P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                                            }
                                                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                            p2pStateMachine3.p2pConnectWithPinDisplay(p2pStateMachine3.mSavedPeerConfig);
                                                            P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                            p2pStateMachine4.transitionTo(p2pStateMachine4.mGroupNegotiationState);
                                                            break;
                                                        }
                                                    } else {
                                                        Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                                                        break;
                                                    }
                                                case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                                                    if (message.obj != null) {
                                                        WifiP2pDevice device2 = ((WifiP2pProvDiscEvent) message.obj).device;
                                                        if (device2 != null) {
                                                            P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = device2.candidateStaticIp;
                                                        }
                                                        if ((device2 == null || device2.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                                P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                                            }
                                                            if (TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                                                boolean unused2 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                                                P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                                if (!p2pStateMachine5.sendConnectNoticeToApp(p2pStateMachine5.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mSavedPeerConfig)) {
                                                                    P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                                    p2pStateMachine6.transitionTo(p2pStateMachine6.mUserAuthorizingNegotiationRequestState);
                                                                    break;
                                                                }
                                                            } else {
                                                                P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                                                p2pStateMachine7.p2pConnectWithPinDisplay(p2pStateMachine7.mSavedPeerConfig);
                                                                P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                                                p2pStateMachine8.transitionTo(p2pStateMachine8.mGroupNegotiationState);
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                        break;
                                                    }
                                                    break;
                                                case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                                                    if (message.obj != null) {
                                                        WifiP2pProvDiscEvent provDisc2 = (WifiP2pProvDiscEvent) message.obj;
                                                        WifiP2pDevice device3 = provDisc2.device;
                                                        if (device3 != null) {
                                                            P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = provDisc2.device.candidateStaticIp;
                                                            if (device3.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                                    P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                                                }
                                                                P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc2.pin;
                                                                P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                                                p2pStateMachine9.p2pConnectWithPinDisplay(p2pStateMachine9.mSavedPeerConfig);
                                                                P2pStateMachine.this.notifyInvitationSent(provDisc2.pin, device3.deviceAddress);
                                                                P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                                                                p2pStateMachine10.transitionTo(p2pStateMachine10.mGroupNegotiationState);
                                                                break;
                                                            }
                                                        } else {
                                                            Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                                                            break;
                                                        }
                                                    } else {
                                                        Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                        break;
                                                    }
                                                default:
                                                    return false;
                                            }
                                        } else {
                                            P2pStateMachine.this.loge("provision discovery failed");
                                            WifiP2pServiceImpl.this.mWifiP2pMetrics.endConnectionEvent(4);
                                            P2pStateMachine.this.handleGroupCreationFailure();
                                            P2pStateMachine p2pStateMachine11 = P2pStateMachine.this;
                                            p2pStateMachine11.transitionTo(p2pStateMachine11.mInactiveState);
                                            P2pStateMachine.this.stopLegacyWifiScan(false);
                                        }
                                    }
                                    return true;
                                }
                            }

                            class GroupNegotiationState extends State {
                                GroupNegotiationState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    P2pStateMachine.this.stopLegacyWifiScan(true);
                                    P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT);
                                    P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                    p2pStateMachine.sendMessageDelayed(p2pStateMachine.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.access$16204(), 0), 120000);
                                }

                                /* JADX WARNING: Can't fix incorrect switch cases order */
                                /* Code decompiled incorrectly, please refer to instructions dump. */
                                public boolean processMessage(android.os.Message r11) {
                                    /*
                                        r10 = this;
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x0022
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r2 = r10.getName()
                                        r1.append(r2)
                                        java.lang.String r2 = r11.toString()
                                        r1.append(r2)
                                        java.lang.String r1 = r1.toString()
                                        r0.logd(r1)
                                    L_0x0022:
                                        int r0 = r11.what
                                        r1 = 139268(0x22004, float:1.95156E-40)
                                        r2 = 1
                                        if (r0 == r1) goto L_0x0586
                                        r1 = 139372(0x2206c, float:1.95302E-40)
                                        if (r0 == r1) goto L_0x0586
                                        r1 = -2
                                        r3 = 0
                                        switch(r0) {
                                            case 147481: goto L_0x0546;
                                            case 147482: goto L_0x04ac;
                                            case 147483: goto L_0x055d;
                                            case 147484: goto L_0x0499;
                                            case 147485: goto L_0x01b3;
                                            case 147486: goto L_0x04f9;
                                            case 147487: goto L_0x00ed;
                                            case 147488: goto L_0x004a;
                                            default: goto L_0x0034;
                                        }
                                    L_0x0034:
                                        switch(r0) {
                                            case 147490: goto L_0x0038;
                                            case 147491: goto L_0x0038;
                                            case 147492: goto L_0x0038;
                                            case 147493: goto L_0x0586;
                                            default: goto L_0x0037;
                                        }
                                    L_0x0037:
                                        return r3
                                    L_0x0038:
                                        java.lang.Object r0 = r11.obj
                                        android.net.wifi.p2p.WifiP2pProvDiscEvent r0 = (android.net.wifi.p2p.WifiP2pProvDiscEvent) r0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pStaticIpConfig r1 = r1.mP2pStaticIpConfig
                                        android.net.wifi.p2p.WifiP2pDevice r3 = r0.device
                                        int r3 = r3.candidateStaticIp
                                        r1.candidateStaticIp = r3
                                        goto L_0x0587
                                    L_0x004a:
                                        java.lang.Object r0 = r11.obj
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r0 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus) r0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.SUCCESS
                                        if (r0 != r3) goto L_0x0054
                                        goto L_0x0587
                                    L_0x0054:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                        r4.<init>()
                                        java.lang.String r5 = "Invitation result "
                                        r4.append(r5)
                                        r4.append(r0)
                                        java.lang.String r4 = r4.toString()
                                        r3.loge(r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.UNKNOWN_P2P_GROUP
                                        if (r0 != r3) goto L_0x00ab
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r3 = r3.mSavedPeerConfig
                                        if (r3 == 0) goto L_0x0587
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r3 = r3.mSavedPeerConfig
                                        int r3 = r3.netId
                                        if (r3 < 0) goto L_0x0098
                                        boolean r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r4 == 0) goto L_0x008d
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r5 = "Remove unknown client from the list"
                                        r4.logd(r5)
                                    L_0x008d:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r5 = r4.mSavedPeerConfig
                                        java.lang.String r5 = r5.deviceAddress
                                        boolean unused = r4.removeClientFromList(r3, r5, r2)
                                    L_0x0098:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r4 = r4.mSavedPeerConfig
                                        r4.netId = r1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r4 = r1.mSavedPeerConfig
                                        r1.p2pConnectWithPinDisplay(r4)
                                        goto L_0x0587
                                    L_0x00ab:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE
                                        if (r0 != r3) goto L_0x00c2
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r3 = r3.mSavedPeerConfig
                                        r3.netId = r1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r3 = r1.mSavedPeerConfig
                                        r1.p2pConnectWithPinDisplay(r3)
                                        goto L_0x0587
                                    L_0x00c2:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL
                                        if (r0 != r1) goto L_0x00d1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$FrequencyConflictState r3 = r1.mFrequencyConflictState
                                        r1.transitionTo(r3)
                                        goto L_0x0587
                                    L_0x00d1:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        com.android.server.wifi.p2p.WifiP2pMetrics r1 = r1.mWifiP2pMetrics
                                        r3 = 5
                                        r1.endConnectionEvent(r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1.handleGroupCreationFailure()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$InactiveState r3 = r1.mInactiveState
                                        r1.transitionTo(r3)
                                        goto L_0x0587
                                    L_0x00ed:
                                        java.lang.Object r0 = r11.obj
                                        android.net.wifi.p2p.WifiP2pGroup r0 = (android.net.wifi.p2p.WifiP2pGroup) r0
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r0.getOwner()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r4 = r4.mSavedPeerConfig
                                        if (r4 != 0) goto L_0x00fe
                                        return r3
                                    L_0x00fe:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pDeviceList r4 = r4.mPeers
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r5 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r5 = r5.mSavedPeerConfig
                                        java.lang.String r5 = r5.deviceAddress
                                        android.net.wifi.p2p.WifiP2pDevice r4 = r4.get(r5)
                                        if (r4 != 0) goto L_0x0120
                                        android.net.wifi.p2p.WifiP2pDevice r5 = new android.net.wifi.p2p.WifiP2pDevice
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r6 = r6.mSavedPeerConfig
                                        java.lang.String r6 = r6.deviceAddress
                                        r5.<init>(r6)
                                        r4 = r5
                                    L_0x0120:
                                        if (r1 != 0) goto L_0x014f
                                        int r5 = r0.getNetworkId()
                                        java.lang.String r6 = "Ignored invitation from null owner"
                                        if (r5 >= 0) goto L_0x0130
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r2.loge(r6)
                                        return r3
                                    L_0x0130:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroupList r7 = r7.mGroups
                                        java.lang.String r7 = r7.getOwnerAddr(r5)
                                        if (r7 == 0) goto L_0x0149
                                        android.net.wifi.p2p.WifiP2pDevice r6 = new android.net.wifi.p2p.WifiP2pDevice
                                        r6.<init>(r7)
                                        r0.setOwner(r6)
                                        android.net.wifi.p2p.WifiP2pDevice r1 = r0.getOwner()
                                        goto L_0x014f
                                    L_0x0149:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r2.loge(r6)
                                        return r3
                                    L_0x014f:
                                        boolean r5 = r0.contains(r4)
                                        if (r5 != 0) goto L_0x015d
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r2 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r5 = "Ignored invitation during GO Negociation with other device."
                                        r2.loge(r5)
                                        return r3
                                    L_0x015d:
                                        android.net.wifi.p2p.WifiP2pConfig r5 = new android.net.wifi.p2p.WifiP2pConfig
                                        r5.<init>()
                                        android.net.wifi.p2p.WifiP2pDevice r6 = r0.getOwner()
                                        java.lang.String r6 = r6.deviceAddress
                                        r5.deviceAddress = r6
                                        java.lang.String r6 = r4.deviceAddress
                                        r5.fw_dev = r6
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.String r7 = r5.deviceAddress
                                        java.lang.String unused = r6.mSelectedP2pGroupAddress = r7
                                        java.lang.String r6 = r5.deviceAddress
                                        boolean r6 = android.text.TextUtils.isEmpty(r6)
                                        if (r6 == 0) goto L_0x0195
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                        r6.<init>()
                                        java.lang.String r7 = "Dropping invitation request "
                                        r6.append(r7)
                                        r6.append(r5)
                                        java.lang.String r6 = r6.toString()
                                        r3.loge(r6)
                                        goto L_0x0587
                                    L_0x0195:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r6.mAutonomousGroup = r3
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r3.mJoinExistingGroup = r2
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig unused = r3.mSavedPeerConfig = r5
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r6 = r3.mSavedPeerConfig
                                        r3.p2pConnectWithPinDisplay(r6)
                                        goto L_0x0587
                                    L_0x01b3:
                                        java.lang.Object r0 = r11.obj
                                        if (r0 != 0) goto L_0x01c0
                                        java.lang.String r0 = "WifiP2pService"
                                        java.lang.String r1 = "Illegal argument(s)"
                                        android.util.Log.e(r0, r1)
                                        goto L_0x0587
                                    L_0x01c0:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.Object r4 = r11.obj
                                        android.net.wifi.p2p.WifiP2pGroup r4 = (android.net.wifi.p2p.WifiP2pGroup) r4
                                        android.net.wifi.p2p.WifiP2pGroup unused = r0.mGroup = r4
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x01e9
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                        r4.<init>()
                                        java.lang.String r5 = r10.getName()
                                        r4.append(r5)
                                        java.lang.String r5 = " group started"
                                        r4.append(r5)
                                        java.lang.String r4 = r4.toString()
                                        r0.logd(r4)
                                    L_0x01e9:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        boolean r0 = r0.isGroupOwner()
                                        if (r0 == 0) goto L_0x021f
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.getOwner()
                                        java.lang.String r0 = r0.deviceAddress
                                        java.lang.String r4 = "00:00:00:00:00:00"
                                        boolean r0 = r4.equals(r0)
                                        if (r0 == 0) goto L_0x021f
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.getOwner()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r4 = r4.mThisDevice
                                        java.lang.String r4 = r4.deviceAddress
                                        r0.deviceAddress = r4
                                    L_0x021f:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r0.mPersistentGroup = r3
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        int r0 = r0.getNetworkId()
                                        if (r0 != r1) goto L_0x026f
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.Boolean r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.RELOAD
                                        boolean r1 = r1.booleanValue()
                                        r0.updatePersistentNetworks(r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.getOwner()
                                        java.lang.String r0 = r0.deviceAddress
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r1 = r1.mGroup
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroupList r4 = r4.mGroups
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r5 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r5 = r5.mGroup
                                        java.lang.String r5 = r5.getNetworkName()
                                        int r4 = r4.getNetworkId(r0, r5)
                                        r1.setNetworkId(r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r1.mPersistentGroup = r2
                                    L_0x026f:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        java.lang.String r0 = r0.mStaticIp
                                        boolean r0 = r0.isEmpty()
                                        if (r0 != 0) goto L_0x02b6
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r4 = "set staticIP from EAPOL-Key "
                                        r1.append(r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r4 = r4.mGroup
                                        java.lang.String r4 = r4.mStaticIp
                                        r1.append(r4)
                                        java.lang.String r1 = r1.toString()
                                        r0.logd(r1)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pStaticIpConfig r0 = r0.mP2pStaticIpConfig
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r1 = r1.mGroup
                                        java.lang.String r1 = r1.mStaticIp
                                        java.net.InetAddress r1 = android.net.NetworkUtils.numericToInetAddress(r1)
                                        java.net.Inet4Address r1 = (java.net.Inet4Address) r1
                                        int r1 = android.net.NetworkUtils.inetAddressToInt(r1)
                                        r0.candidateStaticIp = r1
                                        goto L_0x02d8
                                    L_0x02b6:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pStaticIpConfig r0 = r0.mP2pStaticIpConfig
                                        int r0 = r0.candidateStaticIp
                                        if (r0 == 0) goto L_0x02d8
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        java.lang.String r0 = r0.mGroupOwnerStaticIp
                                        boolean r0 = r0.isEmpty()
                                        if (r0 != 0) goto L_0x02d8
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        java.lang.String r1 = ""
                                        r0.mGroupOwnerStaticIp = r1
                                    L_0x02d8:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        boolean r0 = r0.isGroupOwner()
                                        r1 = 0
                                        r4 = 10
                                        if (r0 == 0) goto L_0x0332
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean r0 = r0.mAutonomousGroup
                                        if (r0 != 0) goto L_0x0304
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r0 = r0.mWifiNative
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r3 = r3.mGroup
                                        java.lang.String r3 = r3.getInterface()
                                        r0.setP2pGroupIdle(r3, r4)
                                    L_0x0304:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.mThisDevice
                                        if (r0 == 0) goto L_0x0323
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.getOwner()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        android.net.wifi.p2p.WifiP2pDevice r3 = r3.mThisDevice
                                        r0.update(r3)
                                    L_0x0323:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r3 = r0.mGroup
                                        java.lang.String r3 = r3.getInterface()
                                        r0.startDhcpServer(r3)
                                        goto L_0x0489
                                    L_0x0332:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pStaticIpConfig r0 = r0.mP2pStaticIpConfig
                                        int r0 = r0.candidateStaticIp
                                        java.lang.String r5 = "Unknown group owner "
                                        if (r0 == 0) goto L_0x0417
                                        r0 = 0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r3 = r3.mGroup
                                        java.lang.String r3 = r3.getInterface()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this     // Catch:{ Exception -> 0x03af }
                                        android.os.INetworkManagementService r6 = r6.mNwService     // Catch:{ Exception -> 0x03af }
                                        android.net.InterfaceConfiguration r6 = r6.getInterfaceConfig(r3)     // Catch:{ Exception -> 0x03af }
                                        r0 = r6
                                        if (r0 == 0) goto L_0x03ae
                                        android.net.LinkAddress r6 = new android.net.LinkAddress     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ Exception -> 0x03af }
                                        android.net.wifi.p2p.WifiP2pStaticIpConfig r7 = r7.mP2pStaticIpConfig     // Catch:{ Exception -> 0x03af }
                                        int r7 = r7.candidateStaticIp     // Catch:{ Exception -> 0x03af }
                                        java.net.InetAddress r7 = android.net.NetworkUtils.intToInetAddress(r7)     // Catch:{ Exception -> 0x03af }
                                        r8 = 24
                                        r6.<init>(r7, r8)     // Catch:{ Exception -> 0x03af }
                                        r0.setLinkAddress(r6)     // Catch:{ Exception -> 0x03af }
                                        r0.setInterfaceUp()     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this     // Catch:{ Exception -> 0x03af }
                                        android.os.INetworkManagementService r6 = r6.mNwService     // Catch:{ Exception -> 0x03af }
                                        r6.setInterfaceConfig(r3, r0)     // Catch:{ Exception -> 0x03af }
                                        java.util.ArrayList r6 = new java.util.ArrayList     // Catch:{ Exception -> 0x03af }
                                        r6.<init>()     // Catch:{ Exception -> 0x03af }
                                        android.net.RouteInfo r7 = new android.net.RouteInfo     // Catch:{ Exception -> 0x03af }
                                        android.net.LinkAddress r8 = r0.getLinkAddress()     // Catch:{ Exception -> 0x03af }
                                        r7.<init>(r8, r1, r3)     // Catch:{ Exception -> 0x03af }
                                        r6.add(r7)     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ Exception -> 0x03af }
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this     // Catch:{ Exception -> 0x03af }
                                        android.os.INetworkManagementService r7 = r7.mNwService     // Catch:{ Exception -> 0x03af }
                                        r7.addInterfaceToLocalNetwork(r3, r6)     // Catch:{ Exception -> 0x03af }
                                        boolean r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled     // Catch:{ Exception -> 0x03af }
                                        if (r7 == 0) goto L_0x03ae
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this     // Catch:{ Exception -> 0x03af }
                                        java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x03af }
                                        r8.<init>()     // Catch:{ Exception -> 0x03af }
                                        java.lang.String r9 = "Static IP configuration succeeded "
                                        r8.append(r9)     // Catch:{ Exception -> 0x03af }
                                        r8.append(r3)     // Catch:{ Exception -> 0x03af }
                                        java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x03af }
                                        r7.logd(r8)     // Catch:{ Exception -> 0x03af }
                                    L_0x03ae:
                                        goto L_0x03ce
                                    L_0x03af:
                                        r6 = move-exception
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r8 = new java.lang.StringBuilder
                                        r8.<init>()
                                        java.lang.String r9 = "Error configuring interface "
                                        r8.append(r9)
                                        r8.append(r3)
                                        java.lang.String r9 = ", :"
                                        r8.append(r9)
                                        r8.append(r6)
                                        java.lang.String r8 = r8.toString()
                                        r7.loge(r8)
                                    L_0x03ce:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r6 = r6.mWifiNative
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r7 = r7.mGroup
                                        java.lang.String r7 = r7.getInterface()
                                        r6.setP2pGroupIdle(r7, r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r4 = r4.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r4 = r4.getOwner()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pDeviceList r6 = r6.mPeers
                                        java.lang.String r7 = r4.deviceAddress
                                        android.net.wifi.p2p.WifiP2pDevice r6 = r6.get(r7)
                                        if (r6 == 0) goto L_0x0402
                                        r4.updateSupplicantDetails(r6)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r5 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r5.sendPeersChangedBroadcast()
                                        goto L_0x0416
                                    L_0x0402:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r7 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r8 = new java.lang.StringBuilder
                                        r8.<init>()
                                        r8.append(r5)
                                        r8.append(r4)
                                        java.lang.String r5 = r8.toString()
                                        r7.logw(r5)
                                    L_0x0416:
                                        goto L_0x0489
                                    L_0x0417:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r0 = r0.mWifiNative
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r6 = r6.mGroup
                                        java.lang.String r6 = r6.getInterface()
                                        r0.setP2pGroupIdle(r6, r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pNative r0 = r0.mWifiNative
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r4 = r4.mGroup
                                        java.lang.String r4 = r4.getInterface()
                                        r0.setP2pPowerSave(r4, r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r3 = r3.mGroup
                                        java.lang.String r3 = r3.getInterface()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.os.Handler r4 = r4.getHandler()
                                        r0.startIpClient(r3, r4)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pGroup r0 = r0.mGroup
                                        android.net.wifi.p2p.WifiP2pDevice r0 = r0.getOwner()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pDeviceList r3 = r3.mPeers
                                        java.lang.String r4 = r0.deviceAddress
                                        android.net.wifi.p2p.WifiP2pDevice r3 = r3.get(r4)
                                        if (r3 == 0) goto L_0x0475
                                        r0.updateSupplicantDetails(r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r4.sendPeersChangedBroadcast()
                                        goto L_0x0489
                                    L_0x0475:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                        r6.<init>()
                                        r6.append(r5)
                                        r6.append(r0)
                                        java.lang.String r5 = r6.toString()
                                        r4.logw(r5)
                                    L_0x0489:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig unused = r0.mSavedPeerConfig = r1
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$GroupCreatedState r1 = r0.mGroupCreatedState
                                        r0.transitionTo(r1)
                                        goto L_0x0587
                                    L_0x0499:
                                        java.lang.Object r0 = r11.obj
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r0 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus) r0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL
                                        if (r0 != r1) goto L_0x0587
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$FrequencyConflictState r3 = r1.mFrequencyConflictState
                                        r1.transitionTo(r3)
                                        goto L_0x0587
                                    L_0x04ac:
                                        java.lang.Object r0 = r11.obj
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r0 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus) r0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL
                                        if (r0 != r1) goto L_0x04d0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean r1 = r1.mValidFreqConflict
                                        if (r1 == 0) goto L_0x04d0
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        boolean unused = r1.mValidFreqConflict = r3
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$FrequencyConflictState r3 = r1.mFrequencyConflictState
                                        r1.transitionTo(r3)
                                        goto L_0x0587
                                    L_0x04d0:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStatus r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStatus.REJECTED_BY_USER
                                        if (r0 != r1) goto L_0x04f9
                                        boolean r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r1 == 0) goto L_0x04f4
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                        r4.<init>()
                                        java.lang.String r5 = r10.getName()
                                        r4.append(r5)
                                        java.lang.String r5 = " rejected by peer"
                                        r4.append(r5)
                                        java.lang.String r4 = r4.toString()
                                        r1.logd(r4)
                                    L_0x04f4:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1.sendP2pRequestChangedBroadcast(r3)
                                    L_0x04f9:
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x0519
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r4 = r10.getName()
                                        r1.append(r4)
                                        java.lang.String r4 = " go failure"
                                        r1.append(r4)
                                        java.lang.String r1 = r1.toString()
                                        r0.logd(r1)
                                    L_0x0519:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r0 = r0.mSavedPeerConfig
                                        if (r0 == 0) goto L_0x052c
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r1 = r0.mSavedPeerConfig
                                        java.lang.String r1 = r1.deviceAddress
                                        java.lang.String unused = r0.mConnectedDevAddr = r1
                                    L_0x052c:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        com.android.server.wifi.p2p.WifiP2pMetrics r0 = r0.mWifiP2pMetrics
                                        r0.endConnectionEvent(r3)
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r0.handleGroupCreationFailure()
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine$InactiveState r1 = r0.mInactiveState
                                        r0.transitionTo(r1)
                                        goto L_0x0587
                                    L_0x0546:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r0 = r0.mSavedPeerConfig
                                        if (r0 == 0) goto L_0x055d
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r1 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        android.net.wifi.p2p.WifiP2pConfig r1 = r1.mSavedPeerConfig
                                        java.lang.String r1 = r1.deviceAddress
                                        r0.connectRetryCount(r1, r2)
                                    L_0x055d:
                                        boolean r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.mVerboseLoggingEnabled
                                        if (r0 == 0) goto L_0x057d
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        java.lang.StringBuilder r1 = new java.lang.StringBuilder
                                        r1.<init>()
                                        java.lang.String r3 = r10.getName()
                                        r1.append(r3)
                                        java.lang.String r3 = " go success"
                                        r1.append(r3)
                                        java.lang.String r1 = r1.toString()
                                        r0.logd(r1)
                                    L_0x057d:
                                        com.android.server.wifi.p2p.WifiP2pServiceImpl$P2pStateMachine r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.this
                                        r1 = 143361(0x23001, float:2.00892E-40)
                                        r0.removeMessages(r1)
                                        goto L_0x0587
                                    L_0x0586:
                                    L_0x0587:
                                        return r2
                                    */
                                    throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.GroupNegotiationState.processMessage(android.os.Message):boolean");
                                }

                                public void exit() {
                                    if (P2pStateMachine.this.mGroup != null) {
                                        if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                            return;
                                        }
                                        if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo != null && WifiP2pServiceImpl.this.mThisDevice.wfdInfo.isWfdEnabled()) {
                                            return;
                                        }
                                    }
                                    if (!WifiP2pServiceImpl.this.mAdvancedOppInProgress) {
                                        P2pStateMachine.this.stopLegacyWifiScan(false);
                                    }
                                }
                            }

                            class FrequencyConflictState extends State {
                                private AlertDialog mFrequencyConflictDialog;

                                FrequencyConflictState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    notifyFrequencyConflict();
                                }

                                private void notifyFrequencyConflict() {
                                    P2pStateMachine.this.logd("Notify frequency conflict");
                                    Resources r = Resources.getSystem();
                                    if (!WifiP2pServiceImpl.this.mCm.getNetworkInfo(1).isConnected()) {
                                        P2pStateMachine.this.showNoCommonChannelsDialog();
                                        if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 2);
                                            WifiP2pConfig unused = P2pStateMachine.this.mSavedPeerConfig = null;
                                        }
                                        P2pStateMachine.this.handleGroupCreationFailure();
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.transitionTo(p2pStateMachine.mInactiveState);
                                        return;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext);
                                    P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                    AlertDialog dialog = builder.setMessage(r.getString(17042725, new Object[]{p2pStateMachine2.getDeviceName(p2pStateMachine2.mSavedPeerConfig.deviceAddress)})).setPositiveButton(r.getString(17040166), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                                        }
                                    }).setNegativeButton(r.getString(17040084), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                                        }
                                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        public void onCancel(DialogInterface arg0) {
                                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                                        }
                                    }).create();
                                    dialog.setCanceledOnTouchOutside(false);
                                    dialog.getWindow().setType(2003);
                                    WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
                                    attrs.privateFlags = 16;
                                    dialog.getWindow().setAttributes(attrs);
                                    dialog.show();
                                    this.mFrequencyConflictDialog = dialog;
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + message.toString());
                                    }
                                    int i = message.what;
                                    if (i != 143373) {
                                        switch (i) {
                                            case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT /*143364*/:
                                                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                                                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                                                } else {
                                                    P2pStateMachine.this.loge("DROP_WIFI_USER_ACCEPT message received when WifiChannel is null");
                                                }
                                                boolean unused = WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                                                break;
                                            case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT /*143365*/:
                                                if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                    P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 2);
                                                    WifiP2pConfig unused2 = P2pStateMachine.this.mSavedPeerConfig = null;
                                                }
                                                WifiP2pServiceImpl.this.mWifiP2pMetrics.endConnectionEvent(6);
                                                P2pStateMachine.this.handleGroupCreationFailure();
                                                P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                                p2pStateMachine2.transitionTo(p2pStateMachine2.mInactiveState);
                                                break;
                                            default:
                                                switch (i) {
                                                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                                                    case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                                                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                        p2pStateMachine3.loge(getName() + "group sucess during freq conflict!");
                                                        break;
                                                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                                                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                                                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                                                        break;
                                                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                                                        P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                        p2pStateMachine4.loge(getName() + "group started after freq conflict, handle anyway");
                                                        P2pStateMachine.this.deferMessage(message);
                                                        P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                        p2pStateMachine5.transitionTo(p2pStateMachine5.mGroupNegotiationState);
                                                        break;
                                                    default:
                                                        return false;
                                                }
                                        }
                                    } else {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                            p2pStateMachine6.logd(getName() + "Wifi disconnected, retry p2p");
                                        }
                                        WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                        if (dev == null || dev.status != 1) {
                                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                            p2pStateMachine7.transitionTo(p2pStateMachine7.mInactiveState);
                                            P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                            p2pStateMachine8.sendMessage(139271, p2pStateMachine8.mSavedPeerConfig);
                                        } else {
                                            P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                            p2pStateMachine9.p2pConnectWithPinDisplay(p2pStateMachine9.mSavedPeerConfig);
                                            P2pStateMachine.this.sendPeersChangedBroadcast();
                                            P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                                            p2pStateMachine10.transitionTo(p2pStateMachine10.mGroupNegotiationState);
                                        }
                                    }
                                    return true;
                                }

                                public void exit() {
                                    AlertDialog alertDialog = this.mFrequencyConflictDialog;
                                    if (alertDialog != null) {
                                        alertDialog.dismiss();
                                    }
                                }
                            }

                            class GroupCreatedState extends State {
                                GroupCreatedState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    WifiManager tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
                                    if (tWifiManager != null && WifiP2pServiceImpl.this.mWiFiLock == null) {
                                        WifiManager.WifiLock unused = WifiP2pServiceImpl.this.mWiFiLock = tWifiManager.createWifiLock(WifiP2pServiceImpl.TAG);
                                    }
                                    if (!P2pStateMachine.this.mIsGotoJoinState) {
                                        WifiP2pServiceImpl.this.mWiFiLock.acquire();
                                    }
                                    if (P2pStateMachine.this.mIsGotoJoinState) {
                                        boolean unused2 = P2pStateMachine.this.mIsGotoJoinState = false;
                                    } else if (WifiP2pServiceImpl.this.mAdvancedOppReceiver) {
                                        P2pStateMachine.this.sendMessage(139265);
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.sendMessageDelayed(p2pStateMachine.obtainMessage(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER, WifiP2pServiceImpl.access$18508(WifiP2pServiceImpl.this)), 3000);
                                    } else if (WifiP2pServiceImpl.this.mAdvancedOppSender) {
                                        P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER, 10000);
                                    } else {
                                        if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                            P2pStateMachine.this.mSavedPeerConfig.invalidate();
                                        }
                                        if (!P2pStateMachine.this.mGroup.isGroupOwner() || WifiP2pServiceImpl.this.mAutonomousGroup) {
                                            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, (String) null, (String) null);
                                            P2pStateMachine.this.updateThisDevice(0);
                                            int unused3 = P2pStateMachine.this.mP2pState = 3;
                                        }
                                        if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS));
                                            if (!WifiP2pServiceImpl.this.mPersistentGroup) {
                                                P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT, 300000);
                                            } else {
                                                boolean unused4 = WifiP2pServiceImpl.this.mPersistentGroup = false;
                                            }
                                        } else if (P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp != 0) {
                                            if (P2pStateMachine.this.mGroup.mGroupOwnerStaticIp.isEmpty()) {
                                                P2pStateMachine.this.mGroup.mGroupOwnerStaticIp = NetworkUtils.intToInetAddress((P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp | 16777216) & 33554431).getHostAddress();
                                            }
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            String unused5 = p2pStateMachine2.mConnectedDevAddr = p2pStateMachine2.mGroup.getOwner().deviceAddress;
                                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                            String unused6 = p2pStateMachine3.mConnectedDevIntfAddr = p2pStateMachine3.mGroup.getOwner().interfaceAddress;
                                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mGroup.getOwner().deviceAddress, 0);
                                            WifiP2pServiceImpl.this.connectRetryCount(P2pStateMachine.this.mConnectedDevAddr, 0);
                                            P2pStateMachine.this.sendPeersChangedBroadcast();
                                            P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                            p2pStateMachine4.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(p2pStateMachine4.mGroup.mGroupOwnerStaticIp));
                                            P2pStateMachine.this.mP2pStaticIpConfig.isStaticIp = true;
                                            int unused7 = WifiP2pServiceImpl.this.mConnectedDevicesCnt = 1;
                                            P2pStateMachine.this.showNotification();
                                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                                        }
                                        if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                        }
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT);
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT);
                                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                                        int unused8 = WifiP2pServiceImpl.this.mMaxClientCnt = 0;
                                        P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.TIME_ELAPSED_AFTER_CONNECTED, 300000);
                                    }
                                    boolean unused9 = WifiP2pServiceImpl.mWpsSkip = false;
                                    P2pStateMachine.this.addP2pPktLogFilter();
                                    boolean unused10 = WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen = false;
                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.endConnectionEvent(1);
                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.startGroupEvent(P2pStateMachine.this.mGroup);
                                    P2pStateMachine.this.addP2pPktLogFilter();
                                    WifiP2pServiceImpl.access$19608(WifiP2pServiceImpl.this);
                                    P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                    p2pStateMachine5.sendMessage(WifiP2pServiceImpl.P2P_TRAFFIC_POLL, WifiP2pServiceImpl.this.mTrafficPollToken, 0);
                                }

                                public boolean processMessage(Message message) {
                                    String manufacturer;
                                    boolean ret;
                                    int i;
                                    int netId;
                                    String pkgName;
                                    Message message2 = message;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName() + message.toString());
                                    }
                                    boolean z = false;
                                    switch (message2.what) {
                                        case 139268:
                                        case 139372:
                                            return true;
                                        case 139271:
                                            if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                                P2pStateMachine.this.replyToMessage(message2, 139272);
                                                return true;
                                            }
                                            if (P2pStateMachine.this.mGroup.isGroupOwner() && P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                                P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT);
                                                P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT, 300000);
                                            }
                                            if (!WifiP2pServiceImpl.this.mAdvancedOppSender && !WifiP2pServiceImpl.this.mAdvancedOppReceiver && !WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(P2pStateMachine.this.getCallingPkgName(message2.sendingUid, message2.replyTo), message2.sendingUid, false)) {
                                                P2pStateMachine.this.replyToMessage(message2, 139272);
                                                return true;
                                            } else if (P2pStateMachine.this.mSelectP2pConfigList != null) {
                                                WifiP2pConfig config = P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex);
                                                if (config != null) {
                                                    P2pStateMachine.this.logd("Inviting device : " + config.deviceAddress);
                                                    if (P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config.deviceAddress)) {
                                                        P2pStateMachine.this.mPeers.updateStatus(config.deviceAddress, 1);
                                                        P2pStateMachine.this.sendPeersChangedBroadcast();
                                                        String unused = P2pStateMachine.this.mSelectedP2pGroupAddress = config.deviceAddress;
                                                        int unused2 = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                                    }
                                                }
                                                if (config != null) {
                                                    return true;
                                                }
                                                P2pStateMachine.this.stopLegacyWifiScan(false);
                                                return true;
                                            } else {
                                                Bundle bundle = message.getData();
                                                WifiP2pConfig config2 = (WifiP2pConfig) bundle.getParcelable("wifiP2pConfig");
                                                String pkgName2 = bundle.getString("appPkgName");
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd(getName() + " package to call connect -> " + pkgName2);
                                                }
                                                if (P2pStateMachine.this.isConfigInvalid(config2)) {
                                                    P2pStateMachine.this.loge("Dropping connect requeset " + config2);
                                                    P2pStateMachine.this.replyToMessage(message2, 139272);
                                                    return true;
                                                }
                                                WifiP2pDevice peerDev = P2pStateMachine.this.fetchCurrentDeviceDetails(config2);
                                                if (!"com.android.bluetooth".equals(pkgName2) || !WifiP2pServiceImpl.this.mAdvancedOppReceiver) {
                                                    manufacturer = null;
                                                } else {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine.this.logd("Connection establishment for Advanced OPP");
                                                    }
                                                    P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER);
                                                    WifiP2pConfig unused3 = P2pStateMachine.this.mSavedPeerConfig = config2;
                                                    if (peerDev == null || !peerDev.isGroupOwner()) {
                                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            P2pStateMachine.this.logd("Inviting normal peer");
                                                        }
                                                        WifiP2pServiceImpl.this.mConnReqInfo.set(peerDev, "com.android.bluetooth.advopp", 0, 0, 1, P2pStateMachine.this.mWifiNative.p2pGetManufacturer(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), P2pStateMachine.this.mWifiNative.p2pGetDeviceType(P2pStateMachine.this.mSavedPeerConfig.deviceAddress));
                                                        manufacturer = 1;
                                                    } else {
                                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            P2pStateMachine.this.logd("Peer is group owner operating @ freq : " + P2pStateMachine.this.mWifiNative.p2pGetListenFreq(peerDev.deviceAddress));
                                                        }
                                                        boolean unused4 = WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin = true;
                                                        P2pStateMachine.this.sendMessage(139280);
                                                        return true;
                                                    }
                                                }
                                                if (!(manufacturer == null && config2.deviceAddress == null) && (P2pStateMachine.this.mSavedProvDiscDevice == null || !P2pStateMachine.this.mSavedProvDiscDevice.deviceAddress.equals(config2.deviceAddress))) {
                                                    P2pStateMachine.this.logd("Inviting device : " + config2.deviceAddress);
                                                    WifiP2pConfig unused5 = P2pStateMachine.this.mSavedPeerConfig = config2;
                                                    if (P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config2.deviceAddress)) {
                                                        P2pStateMachine.this.mPeers.updateStatus(config2.deviceAddress, 1);
                                                        P2pStateMachine.this.sendPeersChangedBroadcast();
                                                        String unused6 = P2pStateMachine.this.mSelectedP2pGroupAddress = config2.deviceAddress;
                                                        P2pStateMachine.this.replyToMessage(message2, 139273);
                                                        WifiP2pServiceImpl.this.auditLog(true, "Connecting to device address " + config2.deviceAddress + " using Wi-Fi Direct (P2P) succeeded");
                                                        return true;
                                                    }
                                                    P2pStateMachine.this.replyToMessage(message2, 139272, 0);
                                                    WifiP2pServiceImpl.this.auditLog(false, "Connecting to device address " + config2.deviceAddress + " using Wi-Fi Direct (P2P) failed");
                                                    return true;
                                                }
                                                if (config2.wps.setup == 0) {
                                                    P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), (String) null);
                                                } else if (config2.wps.pin == null) {
                                                    String pin = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface(), (String) null);
                                                    try {
                                                        Integer.parseInt(pin);
                                                        if (!P2pStateMachine.this.sendShowPinReqToFrontApp(pin)) {
                                                            P2pStateMachine.this.notifyInvitationSent(pin, config2.deviceAddress != null ? config2.deviceAddress : "any");
                                                        }
                                                    } catch (NumberFormatException e) {
                                                    }
                                                } else {
                                                    P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), config2.wps.pin);
                                                }
                                                if (config2.deviceAddress != null) {
                                                    P2pStateMachine.this.mPeers.updateStatus(config2.deviceAddress, 1);
                                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                                }
                                                P2pStateMachine.this.replyToMessage(message2, 139273);
                                                return true;
                                            }
                                        case 139274:
                                            if (!P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                return false;
                                            }
                                            P2pStateMachine.this.logd("We will do CANCEL_CONNECT if GO has no client");
                                            if (P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                                P2pStateMachine.this.replyToMessage(message2, 139276);
                                                break;
                                            } else {
                                                return false;
                                            }
                                        case 139280:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " remove group");
                                            }
                                            if (P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                                                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                                p2pStateMachine.transitionTo(p2pStateMachine.mOngoingGroupRemovalState);
                                                P2pStateMachine.this.replyToMessage(message2, 139282);
                                                return true;
                                            }
                                            P2pStateMachine.this.handleGroupRemoved();
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            p2pStateMachine2.transitionTo(p2pStateMachine2.mInactiveState);
                                            P2pStateMachine.this.replyToMessage(message2, 139281, 0);
                                            return true;
                                        case 139326:
                                            WpsInfo wps = (WpsInfo) message2.obj;
                                            if (wps == null) {
                                                P2pStateMachine.this.replyToMessage(message2, 139327);
                                                return true;
                                            }
                                            if (wps.setup == 0) {
                                                ret = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), (String) null);
                                            } else if (wps.pin == null) {
                                                String pin2 = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface(), (String) null);
                                                try {
                                                    Integer.parseInt(pin2);
                                                    P2pStateMachine.this.notifyInvitationSent(pin2, "any");
                                                    ret = true;
                                                } catch (NumberFormatException e2) {
                                                    ret = false;
                                                }
                                            } else {
                                                ret = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wps.pin);
                                            }
                                            P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                            if (ret) {
                                                i = 139328;
                                            } else {
                                                i = 139327;
                                            }
                                            p2pStateMachine3.replyToMessage(message2, i);
                                            return true;
                                        case 139371:
                                            int timeout = message2.arg1;
                                            if (P2pStateMachine.this.mPeers.clear()) {
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                            }
                                            P2pStateMachine.this.mWifiNative.p2pFlush();
                                            P2pStateMachine.this.mPeers.updateSupplicantDetails(P2pStateMachine.this.mGroup.getOwner());
                                            P2pStateMachine.this.stopLegacyWifiPeriodicScan(true);
                                            if (timeout == -999) {
                                                P2pStateMachine.this.mWifiNative.p2pFind(5, -999);
                                            } else {
                                                P2pStateMachine.this.mWifiNative.p2pFind(timeout);
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139267);
                                            return true;
                                        case 139374:
                                            WifiP2pConfigList unused7 = P2pStateMachine.this.mSelectP2pConfigList = (WifiP2pConfigList) message2.obj;
                                            int unused8 = P2pStateMachine.this.mSelectP2pConfigIndex = 0;
                                            for (WifiP2pConfig cc : P2pStateMachine.this.mSelectP2pConfigList.getConfigList()) {
                                                P2pStateMachine.this.loge("device :" + cc.deviceAddress);
                                            }
                                            P2pStateMachine.this.replyToMessage(message2, 139273);
                                            if (P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex) == null) {
                                                WifiP2pConfigList unused9 = P2pStateMachine.this.mSelectP2pConfigList = null;
                                                int unused10 = P2pStateMachine.this.mSelectP2pConfigIndex = 0;
                                                P2pStateMachine.this.stopLegacyWifiScan(false);
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                            p2pStateMachine4.sendMessageDelayed(p2pStateMachine4.obtainMessage(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT, p2pStateMachine4.mSelectP2pConfigIndex, 0), 30000);
                                            P2pStateMachine.this.sendMessage(139271);
                                            P2pStateMachine.this.stopLegacyWifiScan(true);
                                            return true;
                                        case 139376:
                                            P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                            if (message2.arg1 == 1) {
                                                z = true;
                                            }
                                            boolean unused11 = p2pStateMachine5.mRequestNfcCalled = z;
                                            if (!P2pStateMachine.this.mRequestNfcCalled) {
                                                return true;
                                            }
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT);
                                            P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT, 30000);
                                            return true;
                                        case 139408:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " remove client");
                                            }
                                            WifiP2pConfig p2p_config = (WifiP2pConfig) message2.obj;
                                            WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(p2p_config.deviceAddress);
                                            if (dev == null || TextUtils.isEmpty(dev.interfaceAddress) || !P2pStateMachine.this.mWifiNative.p2pRemoveClient(dev.interfaceAddress, true)) {
                                                if (P2pStateMachine.this.mWifiNative.p2pRemoveClient(p2p_config.deviceAddress, true)) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine.this.logd(getName() + " remove client using dev_address");
                                                    }
                                                    P2pStateMachine.this.replyToMessage(message2, 139410);
                                                    return true;
                                                }
                                                if (WifiP2pServiceImpl.this.mConnectedDevicesCnt == 1) {
                                                    P2pStateMachine.this.handleGroupRemoved();
                                                    P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                    p2pStateMachine6.transitionTo(p2pStateMachine6.mInactiveState);
                                                }
                                                P2pStateMachine.this.replyToMessage(message2, 139409, 0);
                                                return true;
                                            } else if (!WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                return true;
                                            } else {
                                                P2pStateMachine.this.logd(getName() + " remove client using interface_address");
                                                return true;
                                            }
                                        case WifiP2pServiceImpl.DISABLE_P2P /*143377*/:
                                            P2pStateMachine.this.sendMessage(139280);
                                            P2pStateMachine.this.deferMessage(message2);
                                            return true;
                                        case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION /*143390*/:
                                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), false);
                                            try {
                                                WifiP2pServiceImpl.this.mIpClient.completedPreDhcpAction();
                                                return true;
                                            } catch (RemoteException e3) {
                                                e3.rethrowFromSystemServer();
                                                return true;
                                            }
                                        case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION /*143391*/:
                                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                                            return true;
                                        case WifiP2pServiceImpl.IPC_DHCP_RESULTS /*143392*/:
                                            DhcpResults unused12 = WifiP2pServiceImpl.this.mDhcpResults = (DhcpResults) message2.obj;
                                            return true;
                                        case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS /*143393*/:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("mDhcpResults: " + WifiP2pServiceImpl.this.mDhcpResults);
                                            }
                                            if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                                                P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                                String unused13 = p2pStateMachine7.mConnectedDevAddr = p2pStateMachine7.mGroup.getOwner().deviceAddress;
                                                P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                                String unused14 = p2pStateMachine8.mConnectedDevIntfAddr = p2pStateMachine8.mGroup.getOwner().interfaceAddress;
                                                P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mConnectedDevAddr, 0);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                int unused15 = WifiP2pServiceImpl.this.mConnectedDevicesCnt = 1;
                                                P2pStateMachine.this.showNotification();
                                                P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                                p2pStateMachine9.setWifiP2pInfoOnGroupFormation(WifiP2pServiceImpl.this.mDhcpResults.serverAddress);
                                                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                                try {
                                                    String ifname = P2pStateMachine.this.mGroup.getInterface();
                                                    if (WifiP2pServiceImpl.this.mDhcpResults == null) {
                                                        return true;
                                                    }
                                                    WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(ifname, WifiP2pServiceImpl.this.mDhcpResults.getRoutes(ifname));
                                                    return true;
                                                } catch (IllegalStateException ie) {
                                                    P2pStateMachine.this.loge("Failed to add iface to local network " + ie);
                                                    return true;
                                                } catch (Exception e4) {
                                                    P2pStateMachine.this.loge("Failed to add iface to local network " + e4);
                                                    return true;
                                                }
                                            } else {
                                                P2pStateMachine.this.loge("DHCP failed");
                                                P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                                return true;
                                            }
                                        case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE /*143394*/:
                                            P2pStateMachine.this.loge("IP provisioning failed");
                                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                            return true;
                                        case WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT /*143410*/:
                                            P2pStateMachine.this.logd("Nfc join wait time expired");
                                            boolean unused16 = P2pStateMachine.this.mRequestNfcCalled = false;
                                            return true;
                                        case WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT /*143411*/:
                                            int index = message2.arg1;
                                            if (P2pStateMachine.this.mSelectP2pConfigList == null || index != P2pStateMachine.this.mSelectP2pConfigIndex) {
                                                return true;
                                            }
                                            if (P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex) == null) {
                                                WifiP2pConfigList unused17 = P2pStateMachine.this.mSelectP2pConfigList = null;
                                                int unused18 = P2pStateMachine.this.mSelectP2pConfigIndex = 0;
                                                P2pStateMachine.this.stopLegacyWifiScan(false);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("Invitation timed out in Multi-connecting : " + P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex).deviceAddress);
                                            }
                                            P2pStateMachine p2pStateMachine10 = P2pStateMachine.this;
                                            String unused19 = p2pStateMachine10.mConnectedDevAddr = p2pStateMachine10.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex).deviceAddress;
                                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mConnectedDevAddr, 2);
                                            P2pStateMachine.this.sendPeersChangedBroadcast();
                                            P2pStateMachine.access$6808(P2pStateMachine.this);
                                            P2pStateMachine p2pStateMachine11 = P2pStateMachine.this;
                                            p2pStateMachine11.sendMessageDelayed(p2pStateMachine11.obtainMessage(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT, p2pStateMachine11.mSelectP2pConfigIndex, 0), 30000);
                                            P2pStateMachine.this.sendMessage(139271);
                                            return true;
                                        case WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT /*143412*/:
                                            break;
                                        case WifiP2pServiceImpl.TIME_ELAPSED_AFTER_CONNECTED /*143413*/:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("mConnectedPkgName : " + WifiP2pServiceImpl.this.mConnectedPkgName);
                                            }
                                            if (!WifiP2pServiceImpl.WIFI_DIRECT_SETTINGS_PKGNAME.equals(WifiP2pServiceImpl.this.mConnectedPkgName)) {
                                                return true;
                                            }
                                            P2pStateMachine.this.showP2pConnectedNotification();
                                            return true;
                                        case WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER /*143460*/:
                                            if (WifiP2pServiceImpl.this.mAdvancedOppScanRetryCount < 8) {
                                                P2pStateMachine.this.sendMessage(139265);
                                                P2pStateMachine p2pStateMachine12 = P2pStateMachine.this;
                                                p2pStateMachine12.sendMessageDelayed(p2pStateMachine12.obtainMessage(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER, WifiP2pServiceImpl.access$18508(WifiP2pServiceImpl.this)), 3000);
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("p2p discovery failure : remove autonomous group");
                                            }
                                            P2pStateMachine.this.sendMessage(139280);
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER);
                                            int unused20 = WifiP2pServiceImpl.this.mAdvancedOppScanRetryCount = 0;
                                            return true;
                                        case WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER /*143461*/:
                                            if (!WifiP2pServiceImpl.this.mAdvancedOppSender) {
                                                return true;
                                            }
                                            boolean unused21 = WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen = true;
                                            P2pStateMachine.this.sendMessage(139280);
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER);
                                            return true;
                                        case WifiP2pServiceImpl.P2P_TRAFFIC_POLL /*143480*/:
                                            if (message2.arg1 != WifiP2pServiceImpl.this.mTrafficPollToken) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine13 = P2pStateMachine.this;
                                            p2pStateMachine13.sendMessageDelayed(p2pStateMachine13.obtainMessage(WifiP2pServiceImpl.P2P_TRAFFIC_POLL, WifiP2pServiceImpl.this.mTrafficPollToken, 0), (long) WifiP2pServiceImpl.this.mPollTrafficIntervalMsecs);
                                            WifiP2pServiceImpl.this.mWifiInjector.getWifiTrafficPoller().notifyOnDataActivity(TrafficStats.getTxBytes(P2pStateMachine.this.mGroup.getInterface()), TrafficStats.getRxBytes(P2pStateMachine.this.mGroup.getInterface()));
                                            return true;
                                        case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                return false;
                                            }
                                            WifiP2pDevice device = (WifiP2pDevice) message2.obj;
                                            if (P2pStateMachine.this.mGroup.contains(device) || (P2pStateMachine.this.mSavedPeerConfig != null && P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress))) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd("Add device to lost list " + device);
                                                }
                                                P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                                                return true;
                                            }
                                            WifiP2pDevice device2 = P2pStateMachine.this.mPeers.remove(device.deviceAddress);
                                            if (!WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                return true;
                                            }
                                            P2pStateMachine.this.logd("device lost in connected state " + device2);
                                            return true;
                                        case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                                            WifiP2pConfig config3 = (WifiP2pConfig) message2.obj;
                                            if (P2pStateMachine.this.isConfigInvalid(config3)) {
                                                P2pStateMachine.this.loge("Dropping GO neg request " + config3);
                                                return true;
                                            }
                                            WifiP2pConfig unused22 = P2pStateMachine.this.mSavedPeerConfig = config3;
                                            boolean unused23 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                            boolean unused24 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                            if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                return false;
                                            }
                                            WifiP2pDevice peer = P2pStateMachine.this.mPeers.get(config3.deviceAddress);
                                            if (peer == null || peer.supportFwInvite != 1) {
                                                return true;
                                            }
                                            P2pStateMachine p2pStateMachine14 = P2pStateMachine.this;
                                            if (p2pStateMachine14.sendConnectNoticeToApp(peer, p2pStateMachine14.mSavedPeerConfig)) {
                                                return true;
                                            }
                                            P2pStateMachine.this.logd("GC is receiving Connect Req");
                                            boolean unused25 = P2pStateMachine.this.mIsGotoJoinState = true;
                                            P2pStateMachine p2pStateMachine15 = P2pStateMachine.this;
                                            p2pStateMachine15.transitionTo(p2pStateMachine15.mUserAuthorizingJoinState);
                                            return true;
                                        case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                                            P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                                            return true;
                                        case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " group removed");
                                            }
                                            P2pStateMachine.this.handleGroupRemoved();
                                            if (!WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin) {
                                                P2pStateMachine.this.mPeers.clear();
                                            }
                                            P2pStateMachine p2pStateMachine16 = P2pStateMachine.this;
                                            p2pStateMachine16.transitionTo(p2pStateMachine16.mInactiveState);
                                            return true;
                                        case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT:
                                            P2pStatus status = (P2pStatus) message2.obj;
                                            if (status == P2pStatus.SUCCESS) {
                                                return true;
                                            }
                                            P2pStateMachine.this.loge("Invitation result " + status);
                                            if (status != P2pStatus.UNKNOWN_P2P_GROUP || (netId = P2pStateMachine.this.mGroup.getNetworkId()) < 0) {
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("Remove unknown client from the list");
                                            }
                                            P2pStateMachine p2pStateMachine17 = P2pStateMachine.this;
                                            boolean unused26 = p2pStateMachine17.removeClientFromList(netId, p2pStateMachine17.mSavedPeerConfig.deviceAddress, false);
                                            P2pStateMachine p2pStateMachine18 = P2pStateMachine.this;
                                            p2pStateMachine18.sendMessage(139271, p2pStateMachine18.mSavedPeerConfig);
                                            return true;
                                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                                            if (!P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                P2pStateMachine.this.logd("I'm GC. Ignore connection request.");
                                                return true;
                                            } else if (WifiP2pServiceImpl.this.mConnectedDevicesCnt >= WifiP2pManager.MAX_CLIENT_SUPPORT) {
                                                P2pStateMachine.this.logd("Connection limited - mConnectedDevicesCnt : " + WifiP2pServiceImpl.this.mConnectedDevicesCnt);
                                                P2pStateMachine.this.showConnectionLimitDialog();
                                                return true;
                                            } else {
                                                WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message2.obj;
                                                if (P2pStateMachine.this.mSavedPeerConfig == null || P2pStateMachine.this.mSavedPeerConfig.deviceAddress == null || !P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(provDisc.device.deviceAddress)) {
                                                    WifiP2pConfig unused27 = P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                                                    if (!(provDisc == null || provDisc.device == null)) {
                                                        WifiP2pDevice unused28 = P2pStateMachine.this.mSavedProvDiscDevice = provDisc.device;
                                                        if (provDisc.device != null) {
                                                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = provDisc.device.deviceAddress;
                                                            P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = provDisc.device.candidateStaticIp;
                                                        }
                                                    }
                                                    if (message2.what == 147491) {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                                    } else if (message2.what == 147492) {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                                                    } else {
                                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                                    }
                                                    P2pStateMachine p2pStateMachine19 = P2pStateMachine.this;
                                                    WifiP2pDevice peerDev2 = p2pStateMachine19.fetchCurrentDeviceDetails(p2pStateMachine19.mSavedPeerConfig);
                                                    String manufacturer2 = P2pStateMachine.this.mWifiNative.p2pGetManufacturer(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    String type = P2pStateMachine.this.mWifiNative.p2pGetDeviceType(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    String pkgName3 = null;
                                                    List<ActivityManager.RunningTaskInfo> tasks = WifiP2pServiceImpl.this.mActivityMgr.getRunningTasks(1);
                                                    if (tasks.size() != 0) {
                                                        pkgName3 = tasks.get(0).baseActivity.getPackageName();
                                                    }
                                                    if (WifiP2pServiceImpl.this.mAdvancedOppSender || WifiP2pServiceImpl.this.mAdvancedOppReceiver) {
                                                        pkgName = "com.android.bluetooth.advopp";
                                                    } else {
                                                        pkgName = pkgName3;
                                                    }
                                                    List<ActivityManager.RunningTaskInfo> list = tasks;
                                                    WifiP2pServiceImpl.this.mConnReqInfo.set(peerDev2, pkgName, 1, 0, 1, manufacturer2, type);
                                                    if (provDisc.fw_peer != null && P2pStateMachine.this.mGroup.contains(P2pStateMachine.this.mPeers.get(provDisc.fw_peer))) {
                                                        P2pStateMachine.this.notifyInvitationReceivedForceAccept();
                                                        P2pStateMachine.this.logd("accept fw_peer");
                                                        return true;
                                                    } else if ((P2pStateMachine.this.mSelectedP2pGroupAddress == null || !P2pStateMachine.this.mSelectedP2pGroupAddress.equals(provDisc.device.deviceAddress)) && (!P2pStateMachine.this.mRequestNfcCalled || message2.what != 147489)) {
                                                        P2pStateMachine p2pStateMachine20 = P2pStateMachine.this;
                                                        if (p2pStateMachine20.sendConnectNoticeToApp(p2pStateMachine20.mSavedProvDiscDevice, P2pStateMachine.this.mSavedPeerConfig)) {
                                                            return true;
                                                        }
                                                        P2pStateMachine.this.logd("Go to UserAuthorizingJoinState");
                                                        boolean unused29 = P2pStateMachine.this.mIsGotoJoinState = true;
                                                        if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                            P2pStateMachine p2pStateMachine21 = P2pStateMachine.this;
                                                            p2pStateMachine21.transitionTo(p2pStateMachine21.mUserAuthorizingJoinState);
                                                            return true;
                                                        } else if (!WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            return true;
                                                        } else {
                                                            P2pStateMachine.this.logd("Ignore provision discovery for GC");
                                                            return true;
                                                        }
                                                    } else {
                                                        if (!WifiP2pServiceImpl.mWpsSkip || P2pStateMachine.this.mRequestNfcCalled) {
                                                            P2pStateMachine.this.notifyInvitationReceivedForceAccept();
                                                        }
                                                        boolean unused30 = P2pStateMachine.this.mRequestNfcCalled = false;
                                                        boolean unused31 = WifiP2pServiceImpl.mWpsSkip = false;
                                                        return true;
                                                    }
                                                } else {
                                                    P2pStateMachine.this.logd("Ignore duplicated pd request");
                                                    return true;
                                                }
                                            }
                                        case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                                            if (WifiP2pServiceImpl.this.mWfdConnected) {
                                                return true;
                                            }
                                            P2pStateMachine.this.stopLegacyWifiPeriodicScan(false);
                                            return true;
                                        case WifiP2pMonitor.AP_STA_DISCONNECTED_EVENT:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                return true;
                                            }
                                            WifiP2pDevice device3 = (WifiP2pDevice) message2.obj;
                                            String deviceAddress = device3.deviceAddress;
                                            if (deviceAddress != null) {
                                                String unused32 = P2pStateMachine.this.mConnectedDevAddr = deviceAddress;
                                                P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 3);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                if (P2pStateMachine.this.mGroup.removeClient(deviceAddress)) {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine.this.logd("Removed client " + deviceAddress);
                                                    }
                                                    if (!(P2pStateMachine.this.mSavedPeerConfig == null || P2pStateMachine.this.mSavedPeerConfig.deviceAddress == null || !P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(deviceAddress))) {
                                                        P2pStateMachine.this.logd("mSavedPeerConfig need to be cleared");
                                                        WifiP2pConfig unused33 = P2pStateMachine.this.mSavedPeerConfig = null;
                                                    }
                                                    if ((!WifiP2pServiceImpl.this.mAutonomousGroup || WifiP2pServiceImpl.this.mAdvancedOppReceiver || WifiP2pServiceImpl.this.mAdvancedOppSender) && P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                                        P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                                    } else {
                                                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                                    }
                                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.updateGroupEvent(P2pStateMachine.this.mGroup);
                                                } else {
                                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                        P2pStateMachine.this.logd("Failed to remove client " + deviceAddress);
                                                    }
                                                    for (WifiP2pDevice c : P2pStateMachine.this.mGroup.getClientList()) {
                                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                            P2pStateMachine.this.logd("client " + c.deviceAddress);
                                                        }
                                                    }
                                                }
                                                int unused34 = WifiP2pServiceImpl.this.mConnectedDevicesCnt = P2pStateMachine.this.mGroup.getClientList().size();
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd(getName() + " ap sta disconnected");
                                                }
                                                WifiP2pServiceImpl.this.setProp("apstadis");
                                                long unused35 = WifiP2pServiceImpl.this.checkTimeNoa(5, 0);
                                                return true;
                                            }
                                            P2pStateMachine.this.loge("Disconnect on unknown device: " + device3);
                                            return true;
                                        case WifiP2pMonitor.AP_STA_CONNECTED_EVENT:
                                            if (message2.obj == null) {
                                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                                return true;
                                            }
                                            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, (String) null, (String) null);
                                            P2pStateMachine.this.updateThisDevice(0);
                                            int unused36 = P2pStateMachine.this.mP2pState = 3;
                                            String unused37 = P2pStateMachine.this.mSelectedP2pGroupAddress = null;
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT);
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT);
                                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER);
                                            boolean unused38 = WifiP2pServiceImpl.this.mAdvancedOppReceiver = false;
                                            boolean unused39 = WifiP2pServiceImpl.this.mAdvancedOppSender = false;
                                            WifiP2pDevice device4 = (WifiP2pDevice) message2.obj;
                                            String deviceAddress2 = device4.deviceAddress;
                                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                                            if (deviceAddress2 != null) {
                                                WifiP2pServiceImpl.this.connectRetryCount(deviceAddress2, 0);
                                                int unused40 = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                                String unused41 = P2pStateMachine.this.mConnectedDevAddr = deviceAddress2;
                                                String unused42 = P2pStateMachine.this.mConnectedDevIntfAddr = device4.interfaceAddress;
                                                if (P2pStateMachine.this.mSavedProvDiscDevice != null && deviceAddress2.equals(P2pStateMachine.this.mSavedProvDiscDevice.deviceAddress)) {
                                                    WifiP2pDevice unused43 = P2pStateMachine.this.mSavedProvDiscDevice = null;
                                                }
                                                if (P2pStateMachine.this.mPeers.get(deviceAddress2) != null) {
                                                    P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(deviceAddress2));
                                                } else {
                                                    P2pStateMachine.this.mGroup.addClient(deviceAddress2);
                                                }
                                                P2pStateMachine.this.mPeers.updateStatus(deviceAddress2, 0);
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.logd(getName() + " ap sta connected");
                                                }
                                                WifiP2pServiceImpl.this.setProp("apstacon");
                                                int unused44 = WifiP2pServiceImpl.this.mConnectedDevicesCnt = P2pStateMachine.this.mGroup.getClientList().size();
                                                if (WifiP2pServiceImpl.this.mMaxClientCnt < WifiP2pServiceImpl.this.mConnectedDevicesCnt) {
                                                    int unused45 = WifiP2pServiceImpl.this.mMaxClientCnt = WifiP2pServiceImpl.this.mConnectedDevicesCnt;
                                                }
                                                P2pStateMachine.this.showNotification();
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                WifiP2pServiceImpl.this.mWifiP2pMetrics.updateGroupEvent(P2pStateMachine.this.mGroup);
                                                if (P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp != 0) {
                                                    WifiP2pStaticIpConfig access$15500 = P2pStateMachine.this.mP2pStaticIpConfig;
                                                    int mNextIpAddr = access$15500.mThisDeviceStaticIp + 16777216;
                                                    access$15500.mThisDeviceStaticIp = mNextIpAddr;
                                                    P2pStateMachine.this.mWifiNative.p2pSet("static_ip", Formatter.formatIpAddress(mNextIpAddr));
                                                }
                                                if (P2pStateMachine.this.mSelectP2pConfigList != null) {
                                                    P2pStateMachine.access$6808(P2pStateMachine.this);
                                                    if (P2pStateMachine.this.mSelectP2pConfigList.getConfigIndex(P2pStateMachine.this.mSelectP2pConfigIndex) == null) {
                                                        WifiP2pConfigList unused46 = P2pStateMachine.this.mSelectP2pConfigList = null;
                                                        int unused47 = P2pStateMachine.this.mSelectP2pConfigIndex = 0;
                                                        P2pStateMachine.this.stopLegacyWifiScan(false);
                                                    } else {
                                                        P2pStateMachine p2pStateMachine22 = P2pStateMachine.this;
                                                        p2pStateMachine22.sendMessageDelayed(p2pStateMachine22.obtainMessage(WifiP2pServiceImpl.INVITATION_PROCEDURE_TIMED_OUT, p2pStateMachine22.mSelectP2pConfigIndex, 0), 30000);
                                                        P2pStateMachine.this.sendMessage(139271);
                                                    }
                                                } else if ((WifiP2pServiceImpl.this.mThisDevice.wfdInfo == null || !WifiP2pServiceImpl.this.mThisDevice.wfdInfo.isWfdEnabled()) && !WifiP2pServiceImpl.this.mAdvancedOppInProgress) {
                                                    P2pStateMachine.this.stopLegacyWifiScan(false);
                                                }
                                            } else {
                                                P2pStateMachine.this.loge("Connect on null device address, ignore");
                                            }
                                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                            return true;
                                        case 147499:
                                            if (((String) message2.obj) == null) {
                                                return false;
                                            }
                                            if (WifiP2pServiceImpl.this.mReinvokePersistentNetId < 0) {
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(" remove client from the list");
                                            }
                                            P2pStateMachine p2pStateMachine23 = P2pStateMachine.this;
                                            boolean unused48 = p2pStateMachine23.removeClientFromList(WifiP2pServiceImpl.this.mReinvokePersistentNetId, WifiP2pServiceImpl.this.mReinvokePersistent, true);
                                            int unused49 = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                            return true;
                                        case WifiP2pMonitor.P2P_GOPS_EVENT:
                                            String ps_method = (String) message2.obj;
                                            if (ps_method == null || P2pStateMachine.this.mGroup == null || !P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                return true;
                                            }
                                            try {
                                                int noa_dur = Integer.parseInt(ps_method.substring(ps_method.indexOf("noa_dur=") + 8, ps_method.indexOf("rx/s") - 2));
                                                int noa_num = Integer.parseInt(ps_method.substring(ps_method.indexOf("NOA") + 3, ps_method.indexOf("NOA") + 4));
                                                if (noa_num == 0) {
                                                    P2pStateMachine.this.mWifiNative.setP2pNoa(P2pStateMachine.this.mGroup.getInterface(), false, noa_dur);
                                                } else if (noa_num == 1) {
                                                    P2pStateMachine.this.mWifiNative.setP2pNoa(P2pStateMachine.this.mGroup.getInterface(), true, noa_dur);
                                                } else if (noa_num == 2) {
                                                    P2pStateMachine.this.mWifiNative.setP2pIncBw(P2pStateMachine.this.mGroup.getInterface(), false, noa_dur);
                                                } else if (noa_num != 3) {
                                                    P2pStateMachine.this.mWifiNative.setP2pNoa(P2pStateMachine.this.mGroup.getInterface(), false, noa_dur);
                                                    P2pStateMachine.this.mWifiNative.setP2pIncBw(P2pStateMachine.this.mGroup.getInterface(), false, noa_dur);
                                                } else {
                                                    P2pStateMachine.this.mWifiNative.setP2pIncBw(P2pStateMachine.this.mGroup.getInterface(), true, noa_dur);
                                                }
                                                long unused50 = WifiP2pServiceImpl.this.checkTimeNoa(noa_num, noa_dur);
                                                return true;
                                            } catch (Exception e5) {
                                                P2pStateMachine.this.loge("parsing failed in GOPS because NumberFormatException or StringIndexOutOfBoundsException, so skip");
                                                return true;
                                            }
                                        case WifiP2pMonitor.P2P_WPS_SKIP_EVENT:
                                            boolean unused51 = WifiP2pServiceImpl.mWpsSkip = true;
                                            return true;
                                        case 147527:
                                            WifiP2pProvDiscEvent userReject = (WifiP2pProvDiscEvent) message2.obj;
                                            if (P2pStateMachine.this.mSavedPeerConfig != null && (userReject == null || !userReject.device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress))) {
                                                return true;
                                            }
                                            if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                                WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                                WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                                AlertDialog unused52 = WifiP2pServiceImpl.this.mInvitationDialog = null;
                                            }
                                            WifiP2pConfig unused53 = P2pStateMachine.this.mSavedPeerConfig = null;
                                            return true;
                                        default:
                                            return false;
                                    }
                                    P2pStateMachine.this.logd("P2P_GROUP_STARTED_TIMED_OUT");
                                    P2pStateMachine.this.stopLegacyWifiScan(false);
                                    if (!P2pStateMachine.this.mGroup.isGroupOwner() || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                        return true;
                                    }
                                    P2pStateMachine.this.sendMessage(139280);
                                    return true;
                                }

                                public void exit() {
                                    if (!P2pStateMachine.this.mIsGotoJoinState) {
                                        Slog.d(WifiP2pServiceImpl.TAG, "=========== Exit GroupCreatedState");
                                        WifiP2pDevice unused = P2pStateMachine.this.mSavedProvDiscDevice = null;
                                        if (WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin || WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen) {
                                            Log.i(WifiP2pServiceImpl.TAG, "Exit GroupCreatedState. advOPP: " + WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin + " : " + WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndListen);
                                        } else if (WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                                            String unused2 = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS = WifiP2pServiceImpl.toHexString(WifiP2pServiceImpl.this.createRandomMac());
                                            P2pStateMachine.this.mWifiNative.p2pSet("random_mac", WifiP2pServiceImpl.RANDOM_MAC_ADDRESS);
                                            if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress == null || !WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(WifiP2pServiceImpl.RANDOM_MAC_ADDRESS)) {
                                                WifiP2pServiceImpl.this.mThisDevice.deviceAddress = WifiP2pServiceImpl.RANDOM_MAC_ADDRESS;
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    Log.i(WifiP2pServiceImpl.TAG, "Exit GroupCreatedState. MAC is changed: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                                                }
                                            }
                                        }
                                        if (WifiP2pServiceImpl.this.mWiFiLock != null) {
                                            WifiP2pServiceImpl.this.mWiFiLock.release();
                                        }
                                        WifiP2pServiceImpl.this.mWifiP2pMetrics.endGroupEvent();
                                        P2pStateMachine.this.updateThisDevice(3);
                                        P2pStateMachine.this.resetWifiP2pInfo();
                                        P2pStateMachine.this.removeP2pPktLogFilter();
                                        WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, (String) null, (String) null);
                                        int unused3 = P2pStateMachine.this.mP2pState = 2;
                                        WifiP2pServiceImpl.this.setProp("groupexit");
                                        long unused4 = WifiP2pServiceImpl.this.checkTimeNoa(5, 0);
                                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                        P2pStateMachine.this.clearP2pConnectedNotification();
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.TIME_ELAPSED_AFTER_CONNECTED);
                                        P2pStateMachine.this.clearNotification();
                                        boolean unused5 = P2pStateMachine.this.mRequestNfcCalled = false;
                                        boolean unused6 = WifiP2pServiceImpl.this.mWfdConnected = false;
                                        String unused7 = WifiP2pServiceImpl.this.mConnectedPkgName = null;
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_GROUP_STARTED_TIMED_OUT);
                                        boolean unused8 = WifiP2pServiceImpl.this.mAdvancedOppReceiver = false;
                                        boolean unused9 = WifiP2pServiceImpl.this.mAdvancedOppSender = false;
                                        int unused10 = WifiP2pServiceImpl.this.mAdvancedOppScanRetryCount = 0;
                                        int unused11 = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER);
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER);
                                    }
                                }
                            }

                            class UserAuthorizingJoinState extends State {
                                UserAuthorizingJoinState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    if (WifiP2pServiceImpl.this.mAdvancedOppReceiver || WifiP2pServiceImpl.this.mAdvancedOppSender) {
                                        P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER);
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                                        return;
                                    }
                                    P2pStateMachine.this.notifyInvitationReceived();
                                    P2pStateMachine.this.soundNotification();
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + message.toString());
                                    }
                                    switch (message.what) {
                                        case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT /*143362*/:
                                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                                            if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                                if (!P2pStateMachine.this.mGroup.isGroupOwner()) {
                                                    if (P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                                                        P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                                        p2pStateMachine2.logd("Inviting device : " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                        P2pStateMachine.this.sendPeersChangedBroadcast();
                                                    } else {
                                                        P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                        p2pStateMachine3.logd("Failed inviting device : " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    }
                                                    WifiP2pConfig unused = P2pStateMachine.this.mSavedPeerConfig = null;
                                                } else if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                                    if (!TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                                                        P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                        p2pStateMachine4.logd("Allowed device : " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                    }
                                                    P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                                } else {
                                                    P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                                                }
                                            }
                                            P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                            p2pStateMachine5.transitionTo(p2pStateMachine5.mGroupCreatedState);
                                            break;
                                        case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT /*143363*/:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("User rejected incoming request");
                                            }
                                            WifiP2pConfig unused2 = P2pStateMachine.this.mSavedPeerConfig = null;
                                            P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                            p2pStateMachine6.transitionTo(p2pStateMachine6.mGroupCreatedState);
                                            break;
                                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                                        case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                                            break;
                                        case 147527:
                                            if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                                WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                                WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                                AlertDialog unused3 = WifiP2pServiceImpl.this.mInvitationDialog = null;
                                            }
                                            WifiP2pConfig unused4 = P2pStateMachine.this.mSavedPeerConfig = null;
                                            P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                            p2pStateMachine7.transitionTo(p2pStateMachine7.mGroupCreatedState);
                                            break;
                                        default:
                                            return false;
                                    }
                                    return true;
                                }

                                public void exit() {
                                }
                            }

                            class OngoingGroupRemovalState extends State {
                                OngoingGroupRemovalState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                }

                                public boolean processMessage(Message message) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                        p2pStateMachine.logd(getName() + message.toString());
                                    }
                                    int i = message.what;
                                    if (i == 139280) {
                                        P2pStateMachine.this.replyToMessage(message, 139282);
                                        return true;
                                    } else if (i != WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS) {
                                        return false;
                                    } else {
                                        return true;
                                    }
                                }
                            }

                            class NfcProvisionState extends State {
                                NfcProvisionState() {
                                }

                                public void enter() {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName());
                                    }
                                    P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT);
                                    P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT, 30000);
                                    P2pStateMachine.this.mWifiNative.p2pSet("pre_auth", "1");
                                }

                                public boolean processMessage(Message message) {
                                    String pkgName;
                                    Message message2 = message;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        P2pStateMachine.this.logd(getName() + "{ what=" + message2.what + " }");
                                    }
                                    switch (message2.what) {
                                        case 139372:
                                            String pkgName2 = message.getData().getString("appPkgName");
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd(getName() + " package to call P2P_LISTEN -> " + pkgName2);
                                            }
                                            if (!"com.android.bluetooth".equals(pkgName2)) {
                                                int timeout = message2.arg1;
                                                P2pStateMachine.this.stopLegacyWifiScan(true);
                                                if (!P2pStateMachine.this.mWifiNative.p2pListen(timeout)) {
                                                    P2pStateMachine.this.replyToMessage(message2, 139330);
                                                    break;
                                                } else {
                                                    P2pStateMachine.this.replyToMessage(message2, 139331);
                                                    break;
                                                }
                                            } else {
                                                P2pStateMachine.this.loge(getName() + " Unhandled message { what=" + message2.what + " }");
                                                return false;
                                            }
                                        case 139376:
                                            if (message2.arg1 != 1) {
                                                P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT);
                                                break;
                                            } else {
                                                P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT);
                                                P2pStateMachine.this.sendMessageDelayed(WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT, 30000);
                                                break;
                                            }
                                        case WifiP2pServiceImpl.NFC_REQUEST_TIMED_OUT /*143410*/:
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine.this.logd("Nfc wait time expired");
                                            }
                                            WifiP2pConfig unused = P2pStateMachine.this.mSavedPeerConfig = null;
                                            P2pStateMachine.this.mWifiNative.p2pSet("pre_auth", "0");
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            p2pStateMachine.transitionTo(p2pStateMachine.mInactiveState);
                                            break;
                                        case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                                            WifiP2pConfig unused2 = P2pStateMachine.this.mSavedPeerConfig = (WifiP2pConfig) message2.obj;
                                            P2pStateMachine p2pStateMachine2 = P2pStateMachine.this;
                                            if (!p2pStateMachine2.isConfigInvalid(p2pStateMachine2.mSavedPeerConfig)) {
                                                boolean unused3 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                boolean unused4 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                                P2pStateMachine p2pStateMachine3 = P2pStateMachine.this;
                                                p2pStateMachine3.p2pConnectWithPinDisplay(p2pStateMachine3.mSavedPeerConfig);
                                                P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                P2pStateMachine p2pStateMachine4 = P2pStateMachine.this;
                                                p2pStateMachine4.transitionTo(p2pStateMachine4.mGroupNegotiationState);
                                                break;
                                            } else {
                                                P2pStateMachine.this.loge("Dropping GO neg request " + P2pStateMachine.this.mSavedPeerConfig);
                                                break;
                                            }
                                        case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT:
                                            WifiP2pGroup group = (WifiP2pGroup) message2.obj;
                                            if (group.getOwner() == null) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                                    break;
                                                }
                                            } else {
                                                WifiP2pConfig unused5 = P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                                                P2pStateMachine.this.mSavedPeerConfig.deviceAddress = group.getOwner().deviceAddress;
                                                P2pStateMachine p2pStateMachine5 = P2pStateMachine.this;
                                                String unused6 = p2pStateMachine5.mSelectedP2pGroupAddress = p2pStateMachine5.mSavedPeerConfig.deviceAddress;
                                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                                P2pStateMachine p2pStateMachine6 = P2pStateMachine.this;
                                                if (!p2pStateMachine6.isConfigInvalid(p2pStateMachine6.mSavedPeerConfig)) {
                                                    boolean unused7 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                    boolean unused8 = WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                                    P2pStateMachine p2pStateMachine7 = P2pStateMachine.this;
                                                    p2pStateMachine7.p2pConnectWithPinDisplay(p2pStateMachine7.mSavedPeerConfig);
                                                    P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                                    P2pStateMachine p2pStateMachine8 = P2pStateMachine.this;
                                                    p2pStateMachine8.transitionTo(p2pStateMachine8.mGroupNegotiationState);
                                                    break;
                                                } else {
                                                    P2pStateMachine.this.loge("Dropping invitation request " + P2pStateMachine.this.mSavedPeerConfig);
                                                    break;
                                                }
                                            }
                                            break;
                                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                                            WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message2.obj;
                                            if (!(provDisc == null || provDisc.device == null)) {
                                                P2pStateMachine.this.mP2pStaticIpConfig.candidateStaticIp = provDisc.device.candidateStaticIp;
                                                boolean unused9 = WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                                boolean unused10 = WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                                P2pStateMachine.this.mPeers.updateStatus(provDisc.device.deviceAddress, 1);
                                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                                P2pStateMachine p2pStateMachine9 = P2pStateMachine.this;
                                                p2pStateMachine9.transitionTo(p2pStateMachine9.mGroupNegotiationState);
                                                WifiP2pDevice peerDev = provDisc.device;
                                                String manufacturer = P2pStateMachine.this.mWifiNative.p2pGetManufacturer(provDisc.device.deviceAddress);
                                                String type = P2pStateMachine.this.mWifiNative.p2pGetDeviceType(provDisc.device.deviceAddress);
                                                if (WifiP2pServiceImpl.this.mConnReqInfo.pkgName != null) {
                                                    pkgName = WifiP2pServiceImpl.this.mConnReqInfo.pkgName;
                                                } else {
                                                    pkgName = null;
                                                }
                                                WifiP2pServiceImpl.this.mConnReqInfo.set(peerDev, pkgName, 0, 0, 0, manufacturer, type);
                                                break;
                                            }
                                        default:
                                            return false;
                                    }
                                    return true;
                                }

                                public void exit() {
                                }
                            }

                            /* access modifiers changed from: private */
                            public void addHistoricalDumpLog(String log) {
                                if (this.mHistoricalDumpLogs.size() > 35) {
                                    this.mHistoricalDumpLogs.remove(0);
                                }
                                this.mHistoricalDumpLogs.add(log);
                            }

                            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                                WifiP2pServiceImpl.super.dump(fd, pw, args);
                                pw.println("mWifiP2pInfo " + this.mWifiP2pInfo);
                                pw.println("mGroup " + this.mGroup);
                                pw.println("mSavedPeerConfig " + this.mSavedPeerConfig);
                                pw.println("mGroups" + this.mGroups);
                                pw.println("mSavedP2pGroup " + this.mSavedP2pGroup);
                                pw.println("Wi-Fi Direct api call history:");
                                pw.println(this.mHistoricalDumpLogs.toString());
                                pw.println("Config Change:");
                                pw.println(WifiP2pServiceImpl.this.mModeChange.toString());
                                pw.println();
                            }

                            /* access modifiers changed from: private */
                            public void insertLog(String feature, String extra, long value) {
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    Log.d(WifiP2pServiceImpl.TAG, "insertLog (HQM : true, CONTEXT : " + WifiP2pServiceImpl.ENABLE_SURVEY_MODE + ") - feature : " + feature + ", extra : " + extra + ", value : " + value);
                                } else {
                                    String str = feature;
                                    String str2 = extra;
                                    long j = value;
                                }
                                if (WifiP2pServiceImpl.this.mSemHqmManager == null) {
                                    WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                                    SemHqmManager unused = wifiP2pServiceImpl.mSemHqmManager = (SemHqmManager) wifiP2pServiceImpl.mContext.getSystemService("HqmManagerService");
                                }
                                if (WifiP2pServiceImpl.this.mSemHqmManager != null) {
                                    WifiP2pServiceImpl.this.mSemHqmManager.sendHWParamToHQMwithAppId(0, BaseBigDataItem.COMPONENT_ID, feature, BaseBigDataItem.HIT_TYPE_ONCE_A_DAY, (String) null, (String) null, "", extra, "", "android.net.wifi.p2p");
                                } else {
                                    Log.e(WifiP2pServiceImpl.TAG, "error - mSemHqmManager is null");
                                }
                            }

                            /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r5v13, resolved type: java.lang.Object} */
                            /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v13, resolved type: com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo} */
                            /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v28, resolved type: java.lang.Object} */
                            /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v11, resolved type: com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectedPeriodInfo} */
                            /* access modifiers changed from: private */
                            /* JADX WARNING: Multi-variable type inference failed */
                            /* Code decompiled incorrectly, please refer to instructions dump. */
                            public java.lang.String buildLoggingData(int r26, java.lang.String r27) {
                                /*
                                    r25 = this;
                                    r1 = r25
                                    r2 = r26
                                    java.lang.String r3 = " "
                                    r4 = r27
                                    java.lang.String[] r5 = r4.split(r3)
                                    r0 = 0
                                    java.lang.String r6 = ""
                                    r7 = 0
                                    r8 = 0
                                    r9 = -1
                                    int r10 = r5.length
                                    java.lang.String r12 = "WifiP2pService"
                                    r13 = 3
                                    if (r10 != r13) goto L_0x041a
                                    r10 = 1
                                    r13 = r5[r10]
                                    java.lang.String r14 = "="
                                    if (r13 == 0) goto L_0x002e
                                    r13 = r5[r10]
                                    r15 = r5[r10]
                                    int r15 = r15.indexOf(r14)
                                    int r15 = r15 + r10
                                    java.lang.String r0 = r13.substring(r15)
                                    r13 = r0
                                    goto L_0x002f
                                L_0x002e:
                                    r13 = r0
                                L_0x002f:
                                    r0 = 2
                                    r15 = r5[r0]
                                    if (r15 == 0) goto L_0x0049
                                    r15 = r5[r0]     // Catch:{ NumberFormatException -> 0x0047 }
                                    r0 = r5[r0]     // Catch:{ NumberFormatException -> 0x0047 }
                                    int r0 = r0.indexOf(r14)     // Catch:{ NumberFormatException -> 0x0047 }
                                    int r0 = r0 + r10
                                    java.lang.String r0 = r15.substring(r0)     // Catch:{ NumberFormatException -> 0x0047 }
                                    int r0 = java.lang.Integer.parseInt(r0)     // Catch:{ NumberFormatException -> 0x0047 }
                                    r9 = r0
                                L_0x0046:
                                    goto L_0x0049
                                L_0x0047:
                                    r0 = move-exception
                                    goto L_0x0046
                                L_0x0049:
                                    r0 = 147536(0x24050, float:2.06742E-40)
                                    if (r2 != r0) goto L_0x02c3
                                    android.net.wifi.p2p.WifiP2pGroup r0 = r1.mGroup
                                    java.lang.String r10 = "1 "
                                    if (r0 != 0) goto L_0x00d1
                                    android.net.wifi.p2p.WifiP2pGroup r0 = r1.mGroupBackup
                                    if (r0 != 0) goto L_0x0070
                                    java.lang.String r0 = "mGroup and mGroupBackup NULL"
                                    android.util.Log.e(r12, r0)
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    r0.append(r6)
                                    java.lang.String r10 = "-1 -1 -1 "
                                    r0.append(r10)
                                    java.lang.String r0 = r0.toString()
                                    goto L_0x0131
                                L_0x0070:
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    r0.append(r6)
                                    android.net.wifi.p2p.WifiP2pGroup r14 = r1.mGroupBackup
                                    boolean r14 = r14.isGroupOwner()
                                    r0.append(r14)
                                    r0.append(r3)
                                    java.lang.String r0 = r0.toString()
                                    android.net.wifi.p2p.WifiP2pGroup r6 = r1.mGroupBackup
                                    boolean r6 = r6.isGroupOwner()
                                    if (r6 != 0) goto L_0x00a0
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    r6.append(r10)
                                    java.lang.String r0 = r6.toString()
                                    goto L_0x00b8
                                L_0x00a0:
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r10 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    int r10 = r10.mMaxClientCnt
                                    r6.append(r10)
                                    r6.append(r3)
                                    java.lang.String r0 = r6.toString()
                                L_0x00b8:
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    android.net.wifi.p2p.WifiP2pGroup r10 = r1.mGroupBackup
                                    int r10 = r10.getFrequency()
                                    r6.append(r10)
                                    r6.append(r3)
                                    java.lang.String r0 = r6.toString()
                                    goto L_0x0131
                                L_0x00d1:
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    r0.append(r6)
                                    android.net.wifi.p2p.WifiP2pGroup r14 = r1.mGroup
                                    boolean r14 = r14.isGroupOwner()
                                    r0.append(r14)
                                    r0.append(r3)
                                    java.lang.String r0 = r0.toString()
                                    android.net.wifi.p2p.WifiP2pGroup r6 = r1.mGroup
                                    boolean r6 = r6.isGroupOwner()
                                    if (r6 != 0) goto L_0x0101
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    r6.append(r10)
                                    java.lang.String r0 = r6.toString()
                                    goto L_0x0119
                                L_0x0101:
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r10 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    int r10 = r10.mMaxClientCnt
                                    r6.append(r10)
                                    r6.append(r3)
                                    java.lang.String r0 = r6.toString()
                                L_0x0119:
                                    java.lang.StringBuilder r6 = new java.lang.StringBuilder
                                    r6.<init>()
                                    r6.append(r0)
                                    android.net.wifi.p2p.WifiP2pGroup r10 = r1.mGroup
                                    int r10 = r10.getFrequency()
                                    r6.append(r10)
                                    r6.append(r3)
                                    java.lang.String r0 = r6.toString()
                                L_0x0131:
                                    r14 = 0
                                    r16 = -1
                                    long r18 = java.lang.System.currentTimeMillis()
                                    android.net.wifi.p2p.WifiP2pGroup r6 = r1.mGroup
                                    r20 = 1000(0x3e8, double:4.94E-321)
                                    r10 = 0
                                    if (r6 == 0) goto L_0x015f
                                    boolean r6 = r6.isGroupOwner()
                                    if (r6 == 0) goto L_0x015f
                                    android.net.wifi.p2p.WifiP2pGroup r6 = r1.mGroup
                                    java.util.Collection r6 = r6.getClientList()
                                    int r6 = r6.size()
                                    if (r6 != 0) goto L_0x015f
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    r11 = 10
                                    long r22 = r6.checkTimeNoa(r11, r10)
                                    long r16 = r22 / r20
                                    r10 = r16
                                    goto L_0x0161
                                L_0x015f:
                                    r10 = r16
                                L_0x0161:
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r6 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r6 = r6.mConnectedPeriodInfoList
                                    java.lang.Object r6 = r6.remove(r13)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectedPeriodInfo r6 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.WifiP2pConnectedPeriodInfo) r6
                                    if (r6 != 0) goto L_0x0183
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r4 = r4.mConnectedPeriodInfoList
                                    r17 = r5
                                    java.lang.String r5 = r1.convertDevAddress(r13)
                                    java.lang.Object r4 = r4.remove(r5)
                                    r6 = r4
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectedPeriodInfo r6 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.WifiP2pConnectedPeriodInfo) r6
                                    goto L_0x0185
                                L_0x0183:
                                    r17 = r5
                                L_0x0185:
                                    if (r6 == 0) goto L_0x01d0
                                    java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                    r4.<init>()
                                    r4.append(r0)
                                    java.lang.String r5 = r6.pkgName
                                    r4.append(r5)
                                    r4.append(r3)
                                    java.lang.String r0 = r4.toString()
                                    long r4 = r6.startTime
                                    r22 = 0
                                    int r4 = (r4 > r22 ? 1 : (r4 == r22 ? 0 : -1))
                                    if (r4 <= 0) goto L_0x01b7
                                    long r4 = r6.startTime
                                    int r4 = (r4 > r18 ? 1 : (r4 == r18 ? 0 : -1))
                                    if (r4 >= 0) goto L_0x01b7
                                    long r4 = r6.startTime
                                    long r4 = r18 - r4
                                    long r14 = r4 / r20
                                L_0x01b7:
                                    java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                    r4.<init>()
                                    r4.append(r0)
                                    r4.append(r14)
                                    r4.append(r3)
                                    r4.append(r10)
                                    r4.append(r3)
                                    java.lang.String r0 = r4.toString()
                                    goto L_0x01f5
                                L_0x01d0:
                                    java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                    r4.<init>()
                                    java.lang.String r5 = "No matching peer found with "
                                    r4.append(r5)
                                    r4.append(r13)
                                    java.lang.String r4 = r4.toString()
                                    android.util.Log.i(r12, r4)
                                    java.lang.StringBuilder r4 = new java.lang.StringBuilder
                                    r4.<init>()
                                    r4.append(r0)
                                    java.lang.String r5 = "null -1 -1 "
                                    r4.append(r5)
                                    java.lang.String r0 = r4.toString()
                                L_0x01f5:
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r4 = r4.mConnReqInfoList
                                    java.lang.Object r4 = r4.remove(r13)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r4 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.WifiP2pConnectReqInfo) r4
                                    if (r4 != 0) goto L_0x0214
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r5 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r5 = r5.mConnReqInfoList
                                    java.lang.String r12 = r1.convertDevAddress(r13)
                                    java.lang.Object r5 = r5.remove(r12)
                                    r4 = r5
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r4 = (com.android.server.wifi.p2p.WifiP2pServiceImpl.WifiP2pConnectReqInfo) r4
                                L_0x0214:
                                    if (r4 == 0) goto L_0x023a
                                    java.lang.StringBuilder r5 = new java.lang.StringBuilder
                                    r5.<init>()
                                    r5.append(r0)
                                    java.lang.String r12 = r4.getPeerManufacturer()
                                    r5.append(r12)
                                    r5.append(r3)
                                    int r12 = r4.getPeerDevType()
                                    r5.append(r12)
                                    r5.append(r3)
                                    java.lang.String r0 = r5.toString()
                                    r5 = r17
                                    goto L_0x02ac
                                L_0x023a:
                                    com.android.server.wifi.p2p.WifiP2pNative r5 = r1.mWifiNative
                                    java.lang.String r5 = r5.p2pGetManufacturer(r13)
                                    if (r5 != 0) goto L_0x024c
                                    com.android.server.wifi.p2p.WifiP2pNative r7 = r1.mWifiNative
                                    java.lang.String r12 = r1.convertDevAddress(r13)
                                    java.lang.String r5 = r7.p2pGetManufacturer(r12)
                                L_0x024c:
                                    com.android.server.wifi.p2p.WifiP2pNative r7 = r1.mWifiNative
                                    java.lang.String r7 = r7.p2pGetDeviceType(r13)
                                    if (r7 != 0) goto L_0x025e
                                    com.android.server.wifi.p2p.WifiP2pNative r8 = r1.mWifiNative
                                    java.lang.String r12 = r1.convertDevAddress(r13)
                                    java.lang.String r7 = r8.p2pGetDeviceType(r12)
                                L_0x025e:
                                    java.lang.StringBuilder r8 = new java.lang.StringBuilder
                                    r8.<init>()
                                    r8.append(r0)
                                    r8.append(r5)
                                    r8.append(r3)
                                    java.lang.String r0 = r8.toString()
                                    if (r7 != 0) goto L_0x0288
                                    java.lang.StringBuilder r3 = new java.lang.StringBuilder
                                    r3.<init>()
                                    r3.append(r0)
                                    java.lang.String r8 = "-1 "
                                    r3.append(r8)
                                    java.lang.String r0 = r3.toString()
                                    r8 = r7
                                    r7 = r5
                                    r5 = r17
                                    goto L_0x02ac
                                L_0x0288:
                                    java.lang.String r8 = "-"
                                    java.lang.String[] r8 = r7.split(r8)
                                    java.lang.StringBuilder r12 = new java.lang.StringBuilder
                                    r12.<init>()
                                    r12.append(r0)
                                    r20 = r0
                                    r16 = 0
                                    r0 = r8[r16]
                                    r12.append(r0)
                                    r12.append(r3)
                                    java.lang.String r0 = r12.toString()
                                    r24 = r7
                                    r7 = r5
                                    r5 = r8
                                    r8 = r24
                                L_0x02ac:
                                    java.lang.StringBuilder r3 = new java.lang.StringBuilder
                                    r3.<init>()
                                    r3.append(r0)
                                    r3.append(r9)
                                    java.lang.String r0 = r3.toString()
                                    r3 = 0
                                    r1.mGroupBackup = r3
                                    r6 = r0
                                    r17 = r5
                                    goto L_0x0412
                                L_0x02c3:
                                    r17 = r5
                                    r0 = 147537(0x24051, float:2.06743E-40)
                                    if (r2 != r0) goto L_0x03ab
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    if (r0 == 0) goto L_0x033a
                                    if (r13 == 0) goto L_0x033a
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r0 = r0.peerDev
                                    if (r0 == 0) goto L_0x033a
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r0 = r0.peerDev
                                    java.lang.String r0 = r0.deviceAddress
                                    boolean r0 = r13.equals(r0)
                                    if (r0 != 0) goto L_0x0308
                                    java.lang.String r0 = r1.convertDevAddress(r13)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r3 = r3.peerDev
                                    java.lang.String r3 = r3.deviceAddress
                                    boolean r0 = r0.equals(r3)
                                    if (r0 == 0) goto L_0x033a
                                L_0x0308:
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    r0.append(r6)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    java.lang.String r3 = r3.toString()
                                    r0.append(r3)
                                    r0.append(r9)
                                    java.lang.String r0 = r0.toString()
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = new com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r5 = r4.mConnReqInfo
                                    r3.<init>(r5)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r4 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r4 = r4.mConnReqInfoList
                                    r4.put(r13, r3)
                                    r6 = r0
                                    goto L_0x0354
                                L_0x033a:
                                    java.lang.String r0 = "Connection request information doesn't exist or match"
                                    android.util.Log.i(r12, r0)
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    r0.append(r6)
                                    java.lang.String r3 = "null 0 0 0 null -1 -1 "
                                    r0.append(r3)
                                    r0.append(r9)
                                    java.lang.String r0 = r0.toString()
                                    r6 = r0
                                L_0x0354:
                                    if (r9 != 0) goto L_0x0412
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectedPeriodInfo r0 = new com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectedPeriodInfo
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    r0.<init>()
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    if (r3 == 0) goto L_0x03a1
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r3 = r3.peerDev
                                    if (r3 == 0) goto L_0x03a1
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r3 = r3.peerDev
                                    android.net.wifi.p2p.WifiP2pDevice unused = r0.peerDev = r3
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    java.lang.String r3 = r3.pkgName
                                    java.lang.String unused = r0.pkgName = r3
                                    long r3 = java.lang.System.currentTimeMillis()
                                    long unused = r0.startTime = r3
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.util.HashMap r3 = r3.mConnectedPeriodInfoList
                                    android.net.wifi.p2p.WifiP2pDevice r4 = r0.peerDev
                                    java.lang.String r4 = r4.deviceAddress
                                    r3.put(r4, r0)
                                L_0x03a1:
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    java.lang.String r4 = r0.pkgName
                                    java.lang.String unused = r3.mConnectedPkgName = r4
                                    goto L_0x0412
                                L_0x03ab:
                                    r0 = 147538(0x24052, float:2.06745E-40)
                                    if (r2 != r0) goto L_0x0413
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    if (r0 == 0) goto L_0x0412
                                    if (r13 == 0) goto L_0x0412
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r0 = r0.peerDev
                                    if (r0 == 0) goto L_0x0412
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r0 = r0.peerDev
                                    java.lang.String r0 = r0.deviceAddress
                                    boolean r0 = r13.equals(r0)
                                    if (r0 != 0) goto L_0x03ee
                                    java.lang.String r0 = r1.convertDevAddress(r13)
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r3 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r3 = r3.mConnReqInfo
                                    android.net.wifi.p2p.WifiP2pDevice r3 = r3.peerDev
                                    java.lang.String r3 = r3.deviceAddress
                                    boolean r0 = r0.equals(r3)
                                    if (r0 == 0) goto L_0x0412
                                L_0x03ee:
                                    if (r9 < 0) goto L_0x03fe
                                    r0 = 15
                                    if (r9 > r0) goto L_0x03fe
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl r0 = com.android.server.wifi.p2p.WifiP2pServiceImpl.this
                                    com.android.server.wifi.p2p.WifiP2pServiceImpl$WifiP2pConnectReqInfo r0 = r0.mConnReqInfo
                                    r0.setPeerGOIntentValue(r9)
                                    goto L_0x0412
                                L_0x03fe:
                                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                                    r0.<init>()
                                    java.lang.String r3 = "Invalid intent value : "
                                    r0.append(r3)
                                    r0.append(r9)
                                    java.lang.String r0 = r0.toString()
                                    android.util.Log.i(r12, r0)
                                L_0x0412:
                                    return r6
                                L_0x0413:
                                    java.lang.String r0 = "Invalid event"
                                    android.util.Log.e(r12, r0)
                                    r3 = 0
                                    return r3
                                L_0x041a:
                                    r3 = 0
                                    java.lang.String r4 = "Invalid argument for p2p logging data"
                                    android.util.Log.e(r12, r4)
                                    return r3
                                */
                                throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.P2pStateMachine.buildLoggingData(int, java.lang.String):java.lang.String");
                            }

                            private String convertDevAddress(String addr) {
                                java.util.Formatter partialMacAddr = new java.util.Formatter();
                                String macAddrStr = "";
                                try {
                                    partialMacAddr.format("%02x", new Object[]{Integer.valueOf(Integer.parseInt(addr.substring(12, 14), 16) ^ 128)});
                                    macAddrStr = partialMacAddr.toString();
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                } catch (Throwable th) {
                                    partialMacAddr.close();
                                    throw th;
                                }
                                partialMacAddr.close();
                                return addr.substring(0, 12) + macAddrStr + addr.substring(14, addr.length());
                            }

                            /* access modifiers changed from: private */
                            public void checkAndSetConnectivityInstance() {
                                if (WifiP2pServiceImpl.this.mCm == null) {
                                    WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                                    ConnectivityManager unused = wifiP2pServiceImpl.mCm = (ConnectivityManager) wifiP2pServiceImpl.mContext.getSystemService("connectivity");
                                }
                            }

                            /* access modifiers changed from: private */
                            public void checkAndReEnableP2p() {
                                boolean isHalInterfaceAvailable = isHalInterfaceAvailable();
                                Log.d(WifiP2pServiceImpl.TAG, "Wifi enabled=" + this.mIsWifiEnabled + ", P2P Interface availability=" + isHalInterfaceAvailable + ", Number of clients=" + WifiP2pServiceImpl.this.mDeathDataByBinder.size());
                                if (this.mIsWifiEnabled && isHalInterfaceAvailable && !WifiP2pServiceImpl.this.mDeathDataByBinder.isEmpty()) {
                                    sendMessage(WifiP2pServiceImpl.ENABLE_P2P);
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean isHalInterfaceAvailable() {
                                if (this.mWifiNative.isHalInterfaceSupported()) {
                                    return this.mIsHalInterfaceAvailable;
                                }
                                return true;
                            }

                            private void checkAndSendP2pStateChangedBroadcast() {
                                boolean isHalInterfaceAvailable = isHalInterfaceAvailable();
                                Log.d(WifiP2pServiceImpl.TAG, "Wifi enabled=" + this.mIsWifiEnabled + ", P2P Interface availability=" + isHalInterfaceAvailable);
                                sendP2pStateChangedBroadcast(this.mIsWifiEnabled && isHalInterfaceAvailable);
                            }

                            /* access modifiers changed from: private */
                            public void sendP2pStateChangedBroadcast(boolean enabled) {
                                Intent intent = new Intent("android.net.wifi.p2p.STATE_CHANGED");
                                intent.addFlags(67108864);
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("sendP2pStateChangedBroadcast : " + enabled);
                                }
                                if (enabled) {
                                    WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) enabling succeeded", 1);
                                    intent.putExtra("wifi_p2p_state", 2);
                                } else {
                                    intent.putExtra("wifi_p2p_state", 1);
                                }
                                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                            }

                            /* access modifiers changed from: private */
                            public void sendP2pDiscoveryChangedBroadcast(boolean started) {
                                int i;
                                if (WifiP2pServiceImpl.this.mDiscoveryStarted != started) {
                                    boolean unused = WifiP2pServiceImpl.this.mDiscoveryStarted = started;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("discovery change broadcast " + started);
                                    }
                                    Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
                                    intent.addFlags(67108864);
                                    if (started) {
                                        i = 2;
                                    } else {
                                        i = 1;
                                    }
                                    intent.putExtra("discoveryState", i);
                                    WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void sendThisDeviceChangedBroadcast() {
                                Intent intent = new Intent("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
                                intent.addFlags(67108864);
                                intent.putExtra("wifiP2pDevice", eraseOwnDeviceAddress(WifiP2pServiceImpl.this.mThisDevice));
                                WifiP2pServiceImpl.this.mContext.sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, WifiP2pServiceImpl.RECEIVER_PERMISSIONS_FOR_BROADCAST);
                            }

                            /* access modifiers changed from: private */
                            public void sendPeersChangedBroadcast() {
                                Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
                                intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
                                intent.addFlags(67108864);
                                intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
                                intent.putExtra("connectedDevAddress", this.mConnectedDevAddr);
                                WifiP2pServiceImpl.this.mContext.sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, WifiP2pServiceImpl.RECEIVER_PERMISSIONS_FOR_BROADCAST);
                                if (this.mConnectedDevAddr != null) {
                                    this.mConnectedDevAddr = null;
                                }
                            }

                            /* access modifiers changed from: private */
                            public void sendP2pConnectionChangedBroadcast() {
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("sending p2p connection changed broadcast");
                                }
                                Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
                                intent.addFlags(603979776);
                                intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
                                intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
                                intent.putExtra("connectedDevAddress", this.mConnectedDevAddr);
                                intent.putExtra("connectedDevIntfAddress", this.mConnectedDevIntfAddr);
                                intent.putExtra("p2pGroupInfo", eraseOwnDeviceAddress(this.mGroup));
                                intent.putExtra("countWifiAntenna", WifiP2pServiceImpl.this.mCountWifiAntenna);
                                WifiP2pServiceImpl.this.mContext.sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, WifiP2pServiceImpl.RECEIVER_PERMISSIONS_FOR_BROADCAST);
                                intent.setPackage("com.samsung.android.allshare.service.fileshare");
                                intent.addFlags(268435456);
                                WifiP2pServiceImpl.this.mContext.sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, WifiP2pServiceImpl.RECEIVER_PERMISSIONS_FOR_BROADCAST);
                                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
                                } else {
                                    loge("sendP2pConnectionChangedBroadcast(): WifiChannel is null");
                                }
                                if (this.mConnectedDevAddr != null) {
                                    this.mConnectedDevAddr = null;
                                }
                                if (this.mConnectedDevIntfAddr != null) {
                                    this.mConnectedDevIntfAddr = null;
                                }
                            }

                            /* access modifiers changed from: private */
                            public void sendP2pRequestChangedBroadcast(boolean accepted) {
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("sending p2p request changed broadcast");
                                }
                                Intent intent = new Intent("android.net.wifi.p2p.REQUEST_STATE_CHANGE");
                                intent.addFlags(603979776);
                                intent.putExtra("requestState", accepted);
                                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                            }

                            /* access modifiers changed from: private */
                            public void sendP2pPersistentGroupsChangedBroadcast() {
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("sending p2p persistent groups changed broadcast");
                                }
                                Intent intent = new Intent("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
                                intent.addFlags(67108864);
                                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                            }

                            /* access modifiers changed from: private */
                            public void startDhcpServer(String intf) {
                                boolean foundP2pRange = false;
                                try {
                                    InterfaceConfiguration ifcg = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(intf);
                                    ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), 24));
                                    ifcg.setInterfaceUp();
                                    WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(intf, ifcg);
                                    String[] tetheringDhcpRanges = ((ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity")).getTetheredDhcpRanges();
                                    if (WifiP2pServiceImpl.this.mNwService.isTetheringStarted()) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            logd("Stop existing tethering and restart it");
                                        }
                                        WifiP2pServiceImpl.this.mNwService.stopTethering();
                                    }
                                    WifiP2pServiceImpl.this.mNwService.tetherInterface(intf);
                                    if (tetheringDhcpRanges.length <= 0 || tetheringDhcpRanges.length % 2 != 0) {
                                        WifiP2pServiceImpl.this.mNwService.startTethering(WifiP2pServiceImpl.DHCP_RANGE);
                                    } else {
                                        int i = 0;
                                        while (true) {
                                            if (i >= tetheringDhcpRanges.length) {
                                                break;
                                            } else if (tetheringDhcpRanges[i].contains("192.168.49")) {
                                                tetheringDhcpRanges[i] = "192.168.49.100";
                                                tetheringDhcpRanges[i + 1] = "192.168.49.199";
                                                WifiP2pServiceImpl.this.mNwService.startTethering(tetheringDhcpRanges);
                                                foundP2pRange = true;
                                                break;
                                            } else {
                                                i++;
                                            }
                                        }
                                        if (!foundP2pRange) {
                                            String[] tempDhcpRange = new String[(tetheringDhcpRanges.length + WifiP2pServiceImpl.DHCP_RANGE.length)];
                                            System.arraycopy(tetheringDhcpRanges, 0, tempDhcpRange, 0, tetheringDhcpRanges.length);
                                            System.arraycopy(WifiP2pServiceImpl.DHCP_RANGE, 0, tempDhcpRange, tetheringDhcpRanges.length, WifiP2pServiceImpl.DHCP_RANGE.length);
                                            WifiP2pServiceImpl.this.mNwService.startTethering(tempDhcpRange);
                                        }
                                    }
                                    logd("Started Dhcp server on " + intf);
                                } catch (Exception e) {
                                    loge("Error configuring interface " + intf + ", :" + e);
                                }
                            }

                            private void stopDhcpServer(String intf) {
                                String str;
                                str = "Stopped Dhcp server";
                                try {
                                    WifiP2pServiceImpl.this.mNwService.untetherInterface(intf);
                                    for (String temp : WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces()) {
                                        logd("List all interfaces " + temp);
                                        if (temp.compareTo(intf) != 0) {
                                            str = "Found other tethering interfaces, so keep tethering alive";
                                            logd(str);
                                            return;
                                        }
                                    }
                                    WifiP2pServiceImpl.this.mNwService.stopTethering();
                                    logd(str);
                                } catch (Exception e) {
                                    loge("Error stopping Dhcp server" + e);
                                } finally {
                                    logd(str);
                                }
                            }

                            private Context chooseDisplayContext() {
                                Context displayContext = null;
                                SemDesktopModeManager desktopModeManager = (SemDesktopModeManager) WifiP2pServiceImpl.this.mContext.getSystemService("desktopmode");
                                if (desktopModeManager != null) {
                                    SemDesktopModeState state = desktopModeManager.getDesktopModeState();
                                    logd("Desktop mode : " + state.enabled);
                                    if (state.enabled == 3 || state.enabled == 4) {
                                        logd("Dex Mode enabled");
                                        Display[] displays = ((DisplayManager) WifiP2pServiceImpl.this.mContext.getSystemService("display")).getDisplays("com.samsung.android.hardware.display.category.DESKTOP");
                                        if (displays != null && displays.length > 0) {
                                            displayContext = WifiP2pServiceImpl.this.mContext.createDisplayContext(displays[0]);
                                            if (WifiP2pServiceImpl.this.isNightMode) {
                                                displayContext.setTheme(16974120);
                                            } else {
                                                displayContext.setTheme(16974123);
                                            }
                                        }
                                    }
                                }
                                return displayContext;
                            }

                            private void notifyP2pEnableFailure() {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                AlertDialog dialog = new AlertDialog.Builder(displayContext).setTitle(r.getString(17042717)).setMessage(r.getString(17042724)).setPositiveButton(r.getString(17039370), (DialogInterface.OnClickListener) null).create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.getWindow().setType(2003);
                                WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
                                attrs.privateFlags = 16;
                                dialog.getWindow().setAttributes(attrs);
                                dialog.show();
                            }

                            /* access modifiers changed from: private */
                            public void showConnectionLimitDialog() {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                if (WifiP2pServiceImpl.this.mMaximumConnectionDialog == null || !WifiP2pServiceImpl.this.mMaximumConnectionDialog.isShowing()) {
                                    AlertDialog unused = WifiP2pServiceImpl.this.mMaximumConnectionDialog = new AlertDialog.Builder(displayContext).setTitle(r.getString(17042715)).setMessage(r.getString(17042716, new Object[]{Integer.valueOf(WifiP2pManager.MAX_CLIENT_SUPPORT)})).setPositiveButton(r.getString(17039370), (DialogInterface.OnClickListener) null).create();
                                    WifiP2pServiceImpl.this.mMaximumConnectionDialog.getWindow().setType(2003);
                                    WifiP2pServiceImpl.this.mMaximumConnectionDialog.show();
                                }
                            }

                            /* access modifiers changed from: private */
                            public void showNoCommonChannelsDialog() {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                AlertDialog dialog = new AlertDialog.Builder(displayContext).setTitle(r.getString(17042717)).setMessage(r.getString(17042729)).setPositiveButton(r.getString(17039370), (DialogInterface.OnClickListener) null).create();
                                dialog.getWindow().setType(2003);
                                dialog.show();
                            }

                            private void addRowToDialog(ViewGroup group, int stringId, String value) {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                View row = LayoutInflater.from(displayContext).inflate(17367467, group, false);
                                ((TextView) row.findViewById(16909212)).setText(r.getString(stringId));
                                ((TextView) row.findViewById(16909777)).setText(value);
                                if (WifiP2pServiceImpl.this.mUiContext == WifiP2pServiceImpl.this.mUiContextNight) {
                                    ((TextView) row.findViewById(16909777)).setTextColor(WifiP2pServiceImpl.this.mContext.getResources().getColor(17171302));
                                }
                                group.addView(row);
                            }

                            private void addMsgToDialog(ViewGroup group, int stringId, String value) {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                View row = LayoutInflater.from(displayContext).inflate(17367467, group, false);
                                int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                TextView unused2 = WifiP2pServiceImpl.this.mInvitationMsg = (TextView) row.findViewById(16909777);
                                WifiP2pServiceImpl.this.mInvitationMsg.setText(r.getString(stringId, new Object[]{Integer.valueOf(WifiP2pServiceImpl.this.mLapseTime), value}));
                                if (WifiP2pServiceImpl.this.mUiContext == WifiP2pServiceImpl.this.mUiContextNight) {
                                    WifiP2pServiceImpl.this.mInvitationMsg.setTextColor(WifiP2pServiceImpl.this.mContext.getResources().getColor(17171302));
                                }
                                ((TextView) row.findViewById(16909212)).setVisibility(8);
                                group.addView(row);
                            }

                            private void addPluralsMsgToDialog(ViewGroup group, int stringId, String value) {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                View row = LayoutInflater.from(displayContext).inflate(17367467, group, false);
                                int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                TextView unused2 = WifiP2pServiceImpl.this.mInvitationMsg = (TextView) row.findViewById(16909777);
                                WifiP2pServiceImpl.this.mInvitationMsg.setText(r.getQuantityString(stringId, WifiP2pServiceImpl.this.mLapseTime, new Object[]{value, Integer.valueOf(WifiP2pServiceImpl.this.mLapseTime)}));
                                if (WifiP2pServiceImpl.this.mUiContext == WifiP2pServiceImpl.this.mUiContextNight) {
                                    WifiP2pServiceImpl.this.mInvitationMsg.setTextColor(WifiP2pServiceImpl.this.mContext.getResources().getColor(17171302));
                                }
                                ((TextView) row.findViewById(16909212)).setVisibility(8);
                                group.addView(row);
                            }

                            /* access modifiers changed from: private */
                            public void notifyInvitationSent(String pin, String peerAddress) {
                                Resources r = Resources.getSystem();
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                View textEntryView = LayoutInflater.from(displayContext).inflate(17367466, (ViewGroup) null);
                                ViewGroup group = (ViewGroup) textEntryView.findViewById(16909067);
                                addRowToDialog(group, 17042732, getDeviceName(peerAddress));
                                addRowToDialog(group, 17042731, pin);
                                AlertDialog dialog = new AlertDialog.Builder(displayContext).setTitle(r.getString(17042727)).setView(textEntryView).setPositiveButton(r.getString(17039370), (DialogInterface.OnClickListener) null).create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.getWindow().setType(2003);
                                WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
                                attrs.privateFlags = 16;
                                dialog.getWindow().setAttributes(attrs);
                                dialog.show();
                            }

                            private void notifyP2pProvDiscShowPinRequest(String pin, String peerAddress) {
                                Resources r = Resources.getSystem();
                                View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(17367466, (ViewGroup) null);
                                ViewGroup group = (ViewGroup) textEntryView.findViewById(16909067);
                                addRowToDialog(group, 17042732, getDeviceName(peerAddress));
                                addRowToDialog(group, 17042731, pin);
                                AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(17042727)).setView(textEntryView).setPositiveButton(r.getString(17039481), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_CONFIRM);
                                    }
                                }).create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.getWindow().setType(2003);
                                WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
                                attrs.privateFlags = 16;
                                dialog.getWindow().setAttributes(attrs);
                                dialog.show();
                            }

                            /* access modifiers changed from: private */
                            public void notifyInvitationReceived() {
                                if (!WifiP2pServiceImpl.this.isAllowWifiDirectByEDM()) {
                                    sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                                    int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                    if (WifiP2pServiceImpl.this.mWpsTimer != null) {
                                        WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                    }
                                    WifiP2pServiceImpl.this.mDialogWakeLock.release();
                                    return;
                                }
                                if (WifiP2pServiceImpl.this.mWpsTimer != null) {
                                    WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                }
                                if (WifiP2pServiceImpl.this.mInvitationDialog != null && WifiP2pServiceImpl.this.mInvitationDialog.isShowing()) {
                                    WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                    AlertDialog unused2 = WifiP2pServiceImpl.this.mInvitationDialog = null;
                                }
                                Context access$700 = WifiP2pServiceImpl.this.mUiContext;
                                Context displayContext = chooseDisplayContext();
                                if (displayContext == null) {
                                    displayContext = WifiP2pServiceImpl.this.mUiContext;
                                }
                                Resources r = Resources.getSystem();
                                final WpsInfo wps = this.mSavedPeerConfig.wps;
                                View textEntryView = LayoutInflater.from(displayContext).inflate(17367466, (ViewGroup) null);
                                ViewGroup group = (ViewGroup) textEntryView.findViewById(16909067);
                                if (getDeviceName(this.mSavedPeerConfig.deviceAddress).equals(this.mSavedPeerConfig.deviceAddress)) {
                                    this.mWifiNative.p2pFind();
                                }
                                addPluralsMsgToDialog(group, 18153514, getDeviceName(this.mSavedPeerConfig.deviceAddress));
                                EditText unused3 = WifiP2pServiceImpl.this.pinConn = (EditText) textEntryView.findViewById(16909800);
                                final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                                    public boolean onDoubleTap(MotionEvent e) {
                                        return true;
                                    }

                                    public boolean onDoubleTapEvent(MotionEvent e) {
                                        return true;
                                    }

                                    public boolean onDown(MotionEvent e) {
                                        return true;
                                    }

                                    public void onLongPress(MotionEvent e) {
                                    }

                                    public void onShowPress(MotionEvent e) {
                                    }
                                });
                                WifiP2pServiceImpl.this.pinConn.setOnTouchListener(new View.OnTouchListener() {
                                    public boolean onTouch(View v, MotionEvent event) {
                                        return gestureDetector.onTouchEvent(event);
                                    }
                                });
                                WifiP2pServiceImpl.this.mDialogWakeLock.acquire(30000);
                                AlertDialog unused4 = WifiP2pServiceImpl.this.mInvitationDialog = new AlertDialog.Builder(displayContext).setTitle(r.getString(17042728)).setView(textEntryView).setPositiveButton(r.getString(17042713), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                            if (wps.setup == 2) {
                                                P2pStateMachine.this.mSavedPeerConfig.wps.pin = WifiP2pServiceImpl.this.pinConn.getText().toString();
                                            }
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                                p2pStateMachine.logd(P2pStateMachine.this.getName() + " accept invitation " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                                            }
                                        }
                                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) invitation accepted");
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                                        int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                        WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                        WifiP2pServiceImpl.this.mDialogWakeLock.release();
                                        InputMethodManager unused2 = WifiP2pServiceImpl.this.imm = (InputMethodManager) WifiP2pServiceImpl.this.mContext.getSystemService("input_method");
                                        if (WifiP2pServiceImpl.this.imm != null) {
                                            WifiP2pServiceImpl.this.imm.hideSoftInputFromWindow(WifiP2pServiceImpl.this.pinConn.getWindowToken(), 0);
                                        }
                                    }
                                }).setNegativeButton(r.getString(17042712), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            p2pStateMachine.logd(P2pStateMachine.this.getName() + " ignore connect");
                                        }
                                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) invitation denied");
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                                        int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                        WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                        WifiP2pServiceImpl.this.mDialogWakeLock.release();
                                        InputMethodManager unused2 = WifiP2pServiceImpl.this.imm = (InputMethodManager) WifiP2pServiceImpl.this.mContext.getSystemService("input_method");
                                        if (WifiP2pServiceImpl.this.imm != null) {
                                            WifiP2pServiceImpl.this.imm.hideSoftInputFromWindow(WifiP2pServiceImpl.this.pinConn.getWindowToken(), 0);
                                        }
                                    }
                                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    public void onCancel(DialogInterface arg0) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            p2pStateMachine.logd(P2pStateMachine.this.getName() + " ignore connect");
                                        }
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                                        WifiP2pServiceImpl.this.auditLog(true, "Wi-Fi Direct (P2P) invitation denied");
                                        int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                        WifiP2pServiceImpl.this.mWpsTimer.cancel();
                                        WifiP2pServiceImpl.this.mDialogWakeLock.release();
                                        InputMethodManager unused2 = WifiP2pServiceImpl.this.imm = (InputMethodManager) WifiP2pServiceImpl.this.mContext.getSystemService("input_method");
                                        if (WifiP2pServiceImpl.this.imm != null) {
                                            WifiP2pServiceImpl.this.imm.forceHideSoftInput();
                                        }
                                    }
                                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    public void onDismiss(DialogInterface dialog) {
                                        InputMethodManager unused = WifiP2pServiceImpl.this.imm = (InputMethodManager) WifiP2pServiceImpl.this.mContext.getSystemService("input_method");
                                        if (WifiP2pServiceImpl.this.imm != null) {
                                            WifiP2pServiceImpl.this.imm.hideSoftInputFromWindow(WifiP2pServiceImpl.this.pinConn.getWindowToken(), 0);
                                        }
                                        WifiP2pServiceImpl.this.setProp("closeInvitationDialog");
                                    }
                                }).create();
                                textEntryView.setFocusable(false);
                                int i = wps.setup;
                                if (i == 1) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("Shown pin section visible");
                                    }
                                    addRowToDialog(group, 17042731, wps.pin);
                                } else if (i == 2) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("Enter pin section visible");
                                    }
                                    textEntryView.findViewById(16908928).setVisibility(0);
                                    WifiP2pServiceImpl.this.pinConn.requestFocus();
                                }
                                if ((r.getConfiguration().uiMode & 5) == 5) {
                                    WifiP2pServiceImpl.this.mInvitationDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode != 164) {
                                                return false;
                                            }
                                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                                            dialog.dismiss();
                                            return true;
                                        }
                                    });
                                }
                                WifiP2pServiceImpl.this.mInvitationDialog.getWindow().setType(2008);
                                WifiP2pServiceImpl.this.mInvitationDialog.getWindow().setGravity(80);
                                WindowManager.LayoutParams attrs = WifiP2pServiceImpl.this.mInvitationDialog.getWindow().getAttributes();
                                attrs.privateFlags = 16;
                                WifiP2pServiceImpl.this.mInvitationDialog.getWindow().setAttributes(attrs);
                                if (WifiP2pServiceImpl.this.pinConn.hasFocus()) {
                                    WifiP2pServiceImpl.this.mInvitationDialog.getWindow().setSoftInputMode(5);
                                }
                                WifiP2pServiceImpl.this.mInvitationDialog.show();
                                if (wps.setup == 2) {
                                    WifiP2pServiceImpl.this.mInvitationDialog.getButton(-1).setEnabled(false);
                                    WifiP2pServiceImpl.this.pinConn.addTextChangedListener(new TextWatcher() {
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        }

                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                        }

                                        public void afterTextChanged(Editable s) {
                                            if (s.length() == 4 || s.length() == 8) {
                                                WifiP2pServiceImpl.this.mInvitationDialog.getButton(-1).setEnabled(true);
                                            } else {
                                                WifiP2pServiceImpl.this.mInvitationDialog.getButton(-1).setEnabled(false);
                                            }
                                        }
                                    });
                                }
                                CountDownTimer unused5 = WifiP2pServiceImpl.this.mWpsTimer = new CountDownTimer(30000, 1000) {
                                    public void onTick(long millisUntilFinished) {
                                        if (P2pStateMachine.this.mSavedPeerConfig != null) {
                                            WifiP2pServiceImpl.access$24410(WifiP2pServiceImpl.this);
                                            TextView access$24500 = WifiP2pServiceImpl.this.mInvitationMsg;
                                            Resources system = Resources.getSystem();
                                            int access$24400 = WifiP2pServiceImpl.this.mLapseTime;
                                            P2pStateMachine p2pStateMachine = P2pStateMachine.this;
                                            access$24500.setText(system.getQuantityString(18153514, access$24400, new Object[]{p2pStateMachine.getDeviceName(p2pStateMachine.mSavedPeerConfig.deviceAddress), Integer.valueOf(WifiP2pServiceImpl.this.mLapseTime)}));
                                        }
                                    }

                                    public void onFinish() {
                                        int unused = WifiP2pServiceImpl.this.mLapseTime = 30;
                                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                                        InputMethodManager unused2 = WifiP2pServiceImpl.this.imm = (InputMethodManager) WifiP2pServiceImpl.this.mContext.getSystemService("input_method");
                                        if (WifiP2pServiceImpl.this.imm != null) {
                                            WifiP2pServiceImpl.this.imm.hideSoftInputFromWindow(WifiP2pServiceImpl.this.pinConn.getWindowToken(), 0);
                                        }
                                        WifiP2pServiceImpl.this.mInvitationDialog.dismiss();
                                    }
                                }.start();
                                WifiP2pServiceImpl.this.setProp("openInvitationDialog");
                            }

                            /* access modifiers changed from: private */
                            public void notifyInvitationReceivedForceAccept() {
                                if (this.mSavedPeerConfig.wps.setup == 0) {
                                    this.mWifiNative.startWpsPbc(this.mGroup.getInterface(), (String) null);
                                } else {
                                    this.mWifiNative.startWpsPinKeypad(this.mGroup.getInterface(), this.mSavedPeerConfig.wps.pin);
                                }
                                this.mSavedPeerConfig = null;
                            }

                            /* access modifiers changed from: private */
                            public void updatePersistentNetworks(boolean reload) {
                                if (reload) {
                                    this.mGroups.clear();
                                }
                                if (this.mWifiNative.p2pListNetworks(this.mGroups) || reload) {
                                    for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                                        if (!(WifiP2pServiceImpl.this.mThisDevice == null || WifiP2pServiceImpl.this.mThisDevice.deviceAddress == null || group.getOwner() == null || !WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(group.getOwner().deviceAddress))) {
                                            group.setOwner(WifiP2pServiceImpl.this.mThisDevice);
                                        }
                                    }
                                    this.mWifiNative.saveConfig();
                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.updatePersistentGroup(this.mGroups);
                                    sendP2pPersistentGroupsChangedBroadcast();
                                }
                            }

                            private void removePersistentNetworks() {
                                this.mGroups.clear();
                                if (this.mWifiNative.p2pRemoveNetworks()) {
                                    this.mWifiNative.saveConfig();
                                    sendP2pPersistentGroupsChangedBroadcast();
                                }
                                this.mWifiNative.p2pListNetworks(this.mGroups);
                            }

                            /* access modifiers changed from: private */
                            public void stopLegacyWifiScan(boolean flag) {
                                WifiManager tWifiManager;
                                if (WifiP2pServiceImpl.this.mWifiState == 3 && (tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi")) != null) {
                                    Message msg = new Message();
                                    msg.what = 74;
                                    Bundle args = new Bundle();
                                    args.putBoolean("enable", flag);
                                    msg.obj = args;
                                    tWifiManager.callSECApi(msg);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void setLegacyWifiEnable(boolean flag) {
                                WifiManager tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
                                if (tWifiManager == null) {
                                    return;
                                }
                                if (flag) {
                                    Message msg = new Message();
                                    msg.what = 26;
                                    Bundle args = new Bundle();
                                    args.putBoolean("enable", flag);
                                    msg.obj = args;
                                    tWifiManager.callSECApi(msg);
                                    return;
                                }
                                tWifiManager.setWifiEnabled(flag);
                            }

                            /* access modifiers changed from: private */
                            public void stopLegacyWifiPeriodicScan(boolean flag) {
                                WifiManager tWifiManager;
                                if (WifiP2pServiceImpl.this.mWifiState == 3 && (tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi")) != null) {
                                    Message msg = new Message();
                                    msg.what = 18;
                                    Bundle args = new Bundle();
                                    args.putBoolean("stop", flag);
                                    msg.obj = args;
                                    tWifiManager.callSECApi(msg);
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean isConfigInvalid(WifiP2pConfig config) {
                                if (config == null || TextUtils.isEmpty(config.deviceAddress) || this.mPeers.get(config.deviceAddress) == null) {
                                    return true;
                                }
                                return false;
                            }

                            /* access modifiers changed from: private */
                            public boolean isConfigValidAsGroup(WifiP2pConfig config) {
                                if (config != null && !TextUtils.isEmpty(config.deviceAddress) && !TextUtils.isEmpty(config.networkName) && !TextUtils.isEmpty(config.passphrase)) {
                                    return true;
                                }
                                return false;
                            }

                            /* access modifiers changed from: private */
                            public WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig config) {
                                if (config == null) {
                                    return null;
                                }
                                this.mPeers.updateGroupCapability(config.deviceAddress, this.mWifiNative.getGroupCapability(config.deviceAddress));
                                return this.mPeers.get(config.deviceAddress);
                            }

                            private WifiP2pDevice eraseOwnDeviceAddress(WifiP2pDevice device) {
                                if (device == null) {
                                    return null;
                                }
                                WifiP2pDevice result = new WifiP2pDevice(device);
                                if (device.deviceAddress != null && WifiP2pServiceImpl.this.mThisDevice.deviceAddress != null && device.deviceAddress.length() > 0 && WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(device.deviceAddress)) {
                                    result.deviceAddress = WifiP2pServiceImpl.ANONYMIZED_DEVICE_ADDRESS;
                                }
                                return result;
                            }

                            private WifiP2pGroup eraseOwnDeviceAddress(WifiP2pGroup group) {
                                if (group == null) {
                                    return null;
                                }
                                WifiP2pGroup result = new WifiP2pGroup(group);
                                for (WifiP2pDevice originalDevice : group.getClientList()) {
                                    result.removeClient(originalDevice);
                                    result.addClient(eraseOwnDeviceAddress(originalDevice));
                                }
                                result.setOwner(eraseOwnDeviceAddress(group.getOwner()));
                                return result;
                            }

                            /* access modifiers changed from: private */
                            public WifiP2pDevice maybeEraseOwnDeviceAddress(WifiP2pDevice device, int uid) {
                                if (device == null) {
                                    return null;
                                }
                                if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkLocalMacAddressPermission(uid)) {
                                    return new WifiP2pDevice(device);
                                }
                                return eraseOwnDeviceAddress(device);
                            }

                            /* access modifiers changed from: private */
                            public WifiP2pGroup maybeEraseOwnDeviceAddress(WifiP2pGroup group, int uid) {
                                if (group == null) {
                                    return null;
                                }
                                if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkLocalMacAddressPermission(uid)) {
                                    return new WifiP2pGroup(group);
                                }
                                return eraseOwnDeviceAddress(group);
                            }

                            /* access modifiers changed from: private */
                            public WifiP2pGroupList maybeEraseOwnDeviceAddress(WifiP2pGroupList groupList, int uid) {
                                if (groupList == null) {
                                    return null;
                                }
                                WifiP2pGroupList result = new WifiP2pGroupList();
                                for (WifiP2pGroup group : groupList.getGroupList()) {
                                    result.add(maybeEraseOwnDeviceAddress(group, uid));
                                }
                                return result;
                            }

                            /* access modifiers changed from: private */
                            public void p2pConnectWithPinDisplay(WifiP2pConfig config) {
                                int modeValue;
                                WifiManager tWifiManager;
                                NetworkInfo netInfo;
                                if (config == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                    return;
                                }
                                WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
                                if (dev == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                                    return;
                                }
                                boolean join = dev.isGroupOwner() || WifiP2pServiceImpl.this.mJoinExistingGroup;
                                if (!join && ((config.groupOwnerIntent < 0 || config.groupOwnerIntent > 15) && (tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi")) != null && (netInfo = tWifiManager.getNetworkInfo()) != null && netInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED)) {
                                    int frequency = -1;
                                    if (tWifiManager.getConnectionInfo() != null) {
                                        frequency = tWifiManager.getConnectionInfo().getFrequency();
                                    }
                                    if (frequency >= 5170) {
                                        config.groupOwnerIntent = 8;
                                    } else {
                                        config.groupOwnerIntent = 7;
                                    }
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        Log.d(WifiP2pServiceImpl.TAG, "set groupOwnerIntent : " + config.groupOwnerIntent);
                                    }
                                }
                                if (!join && config.wps.setup != 5 && WifiP2pServiceImpl.this.mThisDevice.wfdInfo != null && WifiP2pServiceImpl.this.mThisDevice.wfdInfo.isWfdEnabled() && (WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() & 1) == 0) {
                                    int modeValue2 = 0;
                                    Integer mode = (Integer) WifiP2pServiceImpl.this.mModeChange.get(config.deviceAddress);
                                    if (mode != null) {
                                        modeValue2 = mode.intValue();
                                    }
                                    if (modeValue == 0) {
                                        if (WifiP2pServiceImpl.this.mConnReqInfo != null && config.groupOwnerIntent < WifiP2pServiceImpl.this.mConnReqInfo.getPeerGOIntentValue()) {
                                            modeValue |= 4;
                                        }
                                        if (config.netId == -2) {
                                            modeValue |= 8;
                                        }
                                    } else {
                                        if ((modeValue & 3) == 3) {
                                            if (modeValue >= 11) {
                                                modeValue = 0;
                                            } else {
                                                modeValue++;
                                            }
                                            logi("Connection Mode Change");
                                        }
                                        if ((modeValue & 4) > 0) {
                                            config.groupOwnerIntent = 0;
                                        }
                                        if ((modeValue & 8) > 0) {
                                            config.netId = -2;
                                        }
                                    }
                                    logd("Connection Mode : " + modeValue);
                                    WifiP2pServiceImpl.this.mModeChange.put(config.deviceAddress, Integer.valueOf(modeValue));
                                }
                                int unused = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                String pin = this.mWifiNative.p2pConnect(config, join);
                                try {
                                    Integer.parseInt(pin);
                                    notifyInvitationSent(pin, config.deviceAddress);
                                } catch (NumberFormatException e) {
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean reinvokePersistentGroup(WifiP2pConfig config) {
                                int netId;
                                if (config == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                    return false;
                                } else if (config.wps.setup == 2) {
                                    return false;
                                } else {
                                    WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
                                    if (dev == null) {
                                        Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                                        return false;
                                    }
                                    boolean join = dev.isGroupOwner();
                                    String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("target ssid is " + ssid + " join:" + join);
                                    }
                                    if (join && dev.isGroupLimit()) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            logd("target device reaches group limit.");
                                        }
                                        join = false;
                                    } else if (join && (netId = this.mGroups.getNetworkId(dev.deviceAddress, ssid)) >= 0) {
                                        if (!this.mWifiNative.p2pGroupAdd(netId)) {
                                            return false;
                                        }
                                        return true;
                                    }
                                    String modelNumber = this.mWifiNative.p2pGetModelName(dev.deviceAddress);
                                    if (modelNumber == null || !modelNumber.equals("TigaMini")) {
                                        if (dev.deviceName != null) {
                                            if (dev.deviceName.equals("IM10")) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    logd("target device is sony dongle(IM10)");
                                                }
                                                config.groupOwnerIntent = 15;
                                                config.netId = -1;
                                                return false;
                                            } else if (dev.deviceName.startsWith("SMARTBEAM_")) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    logd("target device is SMARTBEAM_");
                                                }
                                                config.groupOwnerIntent = 1;
                                            }
                                        }
                                        if (join || !dev.isDeviceLimit()) {
                                            if (!join && dev.isInvitationCapable()) {
                                                int netId2 = -2;
                                                if (config.netId < 0) {
                                                    netId2 = this.mGroups.getNetworkId(dev.deviceAddress);
                                                } else if (config.deviceAddress.equals(this.mGroups.getOwnerAddr(config.netId))) {
                                                    netId2 = config.netId;
                                                }
                                                if (netId2 < 0) {
                                                    if (config.netId == -1) {
                                                        return false;
                                                    }
                                                    netId2 = getNetworkIdFromClientList(dev.deviceAddress);
                                                }
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    logd("netId related with " + dev.deviceAddress + " = " + netId2);
                                                }
                                                if (netId2 >= 0) {
                                                    if (this.mWifiNative.p2pReinvoke(netId2, dev.deviceAddress)) {
                                                        config.netId = netId2;
                                                        int unused = WifiP2pServiceImpl.this.mReinvokePersistentNetId = netId2;
                                                        String unused2 = WifiP2pServiceImpl.this.mReinvokePersistent = dev.deviceAddress;
                                                        return true;
                                                    }
                                                    loge("p2pReinvoke() failed, update networks");
                                                    updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                                                    return false;
                                                }
                                            }
                                            return false;
                                        }
                                        loge("target device reaches the device limit.");
                                        return false;
                                    }
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("target device doesnot support persistent group");
                                    }
                                    config.groupOwnerIntent = 5;
                                    config.netId = -1;
                                    return false;
                                }
                            }

                            private int getNetworkIdFromClientList(String deviceAddress) {
                                if (deviceAddress == null) {
                                    return -1;
                                }
                                for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                                    int netId = group.getNetworkId();
                                    String[] p2pClientList = getClientList(netId);
                                    if (p2pClientList != null) {
                                        for (String client : p2pClientList) {
                                            if (deviceAddress.equalsIgnoreCase(client)) {
                                                return netId;
                                            }
                                        }
                                        continue;
                                    }
                                }
                                return -1;
                            }

                            private String[] getClientList(int netId) {
                                String p2pClients = this.mWifiNative.getP2pClientList(netId);
                                if (p2pClients == null) {
                                    return null;
                                }
                                return p2pClients.split(" ");
                            }

                            /* access modifiers changed from: private */
                            public boolean removeClientFromList(int netId, String addr, boolean isRemovable) {
                                StringBuilder modifiedClientList = new StringBuilder();
                                String[] currentClientList = getClientList(netId);
                                boolean isClientRemoved = false;
                                if (currentClientList != null) {
                                    boolean isClientRemoved2 = false;
                                    for (String client : currentClientList) {
                                        if (!client.equalsIgnoreCase(addr)) {
                                            modifiedClientList.append(" ");
                                            modifiedClientList.append(client);
                                        } else {
                                            isClientRemoved2 = true;
                                        }
                                    }
                                    isClientRemoved = isClientRemoved2;
                                }
                                if (modifiedClientList.length() == 0 && isRemovable) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("Remove unknown network");
                                    }
                                    this.mGroups.remove(netId);
                                    WifiP2pServiceImpl.this.mWifiP2pMetrics.updatePersistentGroup(this.mGroups);
                                    return true;
                                } else if (!isClientRemoved) {
                                    return false;
                                } else {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("Modified client list: " + modifiedClientList);
                                    }
                                    if (modifiedClientList.length() == 0) {
                                        modifiedClientList.append("\"\"");
                                    }
                                    this.mWifiNative.setP2pClientList(netId, modifiedClientList.toString());
                                    this.mWifiNative.saveConfig();
                                    return true;
                                }
                            }

                            /* access modifiers changed from: private */
                            public void setWifiP2pInfoOnGroupFormation(InetAddress serverInetAddress) {
                                WifiP2pInfo wifiP2pInfo = this.mWifiP2pInfo;
                                wifiP2pInfo.groupFormed = true;
                                wifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
                                this.mWifiP2pInfo.groupOwnerAddress = serverInetAddress;
                            }

                            /* access modifiers changed from: private */
                            public void resetWifiP2pInfo() {
                                WifiP2pInfo wifiP2pInfo = this.mWifiP2pInfo;
                                wifiP2pInfo.groupFormed = false;
                                wifiP2pInfo.isGroupOwner = false;
                                wifiP2pInfo.groupOwnerAddress = null;
                            }

                            /* access modifiers changed from: private */
                            public String getDeviceName(String deviceAddress) {
                                WifiP2pDevice d = this.mPeers.get(deviceAddress);
                                if (d != null) {
                                    return d.deviceName;
                                }
                                return deviceAddress;
                            }

                            /* access modifiers changed from: private */
                            public String getPersistedDeviceName() {
                                String deviceName = WifiP2pServiceImpl.this.mFrameworkFacade.getStringSetting(WifiP2pServiceImpl.this.mContext, "wifi_p2p_device_name");
                                if (deviceName != null) {
                                    return deviceName;
                                }
                                String id = WifiP2pServiceImpl.this.mFrameworkFacade.getSecureStringSetting(WifiP2pServiceImpl.this.mContext, "android_id");
                                return "Android_" + id.substring(0, 4);
                            }

                            /* access modifiers changed from: private */
                            public boolean setAndPersistDeviceName(String devName) {
                                if (devName == null) {
                                    return false;
                                }
                                String originalDevName = devName;
                                if (devName.getBytes().length > 32) {
                                    devName = cutString(devName, 32);
                                }
                                if (!this.mWifiNative.setDeviceName(devName)) {
                                    loge("Failed to set device name " + devName);
                                    return false;
                                }
                                WifiP2pServiceImpl.this.mThisDevice.deviceName = devName;
                                WifiP2pServiceImpl.this.mFrameworkFacade.setStringSetting(WifiP2pServiceImpl.this.mContext, "wifi_p2p_device_name", originalDevName);
                                sendThisDeviceChangedBroadcast();
                                return true;
                            }

                            public String cutString(String str, int cutByte) {
                                StringBuilder ret = new StringBuilder();
                                int i = 0;
                                while (i < str.length()) {
                                    int chCount = Character.charCount(str.codePointAt(i));
                                    int length = cutByte - str.substring(i, i + chCount).getBytes().length;
                                    cutByte = length;
                                    if (length < 0) {
                                        break;
                                    }
                                    ret.append(str.substring(i, i + chCount));
                                    i += chCount;
                                }
                                return ret.toString();
                            }

                            /* access modifiers changed from: private */
                            public boolean setWfdInfo(WifiP2pWfdInfo wfdInfo) {
                                boolean success;
                                if (!wfdInfo.isWfdEnabled()) {
                                    this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad ext_nfc_token nfc_interface");
                                    success = this.mWifiNative.setWfdEnable(false);
                                } else {
                                    if (wfdInfo.getDeviceType() != 0) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            logd("WFD sink device does not support PIN display method");
                                        }
                                        this.mWifiNative.setConfigMethods("virtual_push_button ext_nfc_token nfc_interface");
                                    } else {
                                        this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad ext_nfc_token nfc_interface");
                                    }
                                    success = this.mWifiNative.setWfdEnable(true) && this.mWifiNative.setWfdDeviceInfo(wfdInfo.getDeviceInfoHex());
                                }
                                if (!success) {
                                    loge("Failed to set wfd properties");
                                    return false;
                                }
                                WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wfdInfo;
                                sendThisDeviceChangedBroadcast();
                                return true;
                            }

                            /* access modifiers changed from: private */
                            public void convertDeviceNameNSetIconViaContact(WifiP2pDevice dev) {
                                String hashInfo = dev.contactInfoHash;
                                String hash = null;
                                String crc = null;
                                String phoneNumber = null;
                                if (hashInfo.length() >= 10) {
                                    hash = hashInfo.substring(0, 6);
                                    crc = hashInfo.substring(6, 10);
                                }
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("Peer contact info - hash : " + hash + ", crc : " + crc);
                                }
                                if (!(hash == null || crc == null)) {
                                    phoneNumber = Hash.retrieveDB(WifiP2pServiceImpl.this.mContext, hash, crc);
                                }
                                if (phoneNumber != null) {
                                    String contactName = Util.retrieveContact(WifiP2pServiceImpl.this.mContext, phoneNumber);
                                }
                                if (dev.contactImage == null) {
                                    dev.contactImage = GUIUtil.cropIcon(WifiP2pServiceImpl.this.mContext, 17106259, GUIUtil.getContactImage(WifiP2pServiceImpl.this.mContext, phoneNumber));
                                }
                            }

                            /* access modifiers changed from: private */
                            public void setPhoneNumberIntoProbeResp() {
                                String contactHash = null;
                                String contactCRC = null;
                                String phoneNumber = Util.getMyMobileNumber(WifiP2pServiceImpl.this.mContext);
                                if (phoneNumber == null) {
                                    loge("Can't set my contact info since my phone number is not provided by some reason");
                                    return;
                                }
                                String myNumber = Util.cutNumber(phoneNumber);
                                byte[] unused = WifiP2pServiceImpl.hash_byte = Hash.getSipHashByte(myNumber);
                                byte[] unused2 = WifiP2pServiceImpl.hash2_byte = Hash.getDataCheckByte(myNumber);
                                if (WifiP2pServiceImpl.hash_byte != null) {
                                    contactHash = Util.byteToString(WifiP2pServiceImpl.hash_byte);
                                }
                                if (WifiP2pServiceImpl.hash2_byte != null) {
                                    contactCRC = Util.byteToString(WifiP2pServiceImpl.hash2_byte);
                                }
                                if (contactHash == null || contactCRC == null) {
                                    loge("Can't set my contact info since the hash info is not created well");
                                    return;
                                }
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("My contact info - hash : " + contactHash + ", crc : " + contactCRC);
                                }
                                WifiP2pNative wifiP2pNative = this.mWifiNative;
                                wifiP2pNative.p2pSet("phone_number", contactHash + contactCRC);
                            }

                            /* access modifiers changed from: private */
                            public void initializeP2pSettings() {
                                this.mWifiInterface = this.mWifiLegacyNative.getClientInterfaceName();
                                WifiP2pServiceImpl.this.checkDeviceNameInSettings();
                                if (TextUtils.isEmpty(WifiP2pServiceImpl.this.mDeviceNameInSettings)) {
                                    String unused = WifiP2pServiceImpl.this.mDeviceNameInSettings = getPersistedDeviceName();
                                }
                                WifiP2pServiceImpl.this.mThisDevice.deviceName = WifiP2pServiceImpl.this.mDeviceNameInSettings;
                                if (WifiP2pServiceImpl.isTablet()) {
                                    int unused2 = WifiP2pServiceImpl.this.idxIcon = 512;
                                } else {
                                    int unused3 = WifiP2pServiceImpl.this.idxIcon = 256;
                                }
                                this.mWifiNative.p2pSet("discovery_icon", Integer.toString(WifiP2pServiceImpl.this.idxIcon));
                                if (WifiP2pServiceImpl.this.mThisDevice.deviceName != null && WifiP2pServiceImpl.this.mThisDevice.deviceName.getBytes().length > 32) {
                                    WifiP2pServiceImpl.this.mThisDevice.deviceName = cutString(WifiP2pServiceImpl.this.mThisDevice.deviceName, 32);
                                }
                                if (!this.mWifiNative.setDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName)) {
                                    loge("initializeP2pSettings - Failed to set device name");
                                } else {
                                    WifiP2pServiceImpl.this.mFrameworkFacade.setStringSetting(WifiP2pServiceImpl.this.mContext, "wifi_p2p_device_name", WifiP2pServiceImpl.this.mDeviceNameInSettings);
                                }
                                this.mWifiNative.setP2pDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
                                String detail = SystemProperties.get("ro.product.model", "");
                                WifiP2pNative wifiP2pNative = this.mWifiNative;
                                if (!wifiP2pNative.setP2pSsidPostfix("-" + detail)) {
                                    loge("Failed to set ssid postfix " + detail);
                                }
                                if (Build.VERSION.SDK_INT < 29 && WifiP2pServiceImpl.this.mIsBootComplete) {
                                    setPhoneNumberIntoProbeResp();
                                }
                                this.mWifiNative.p2pSet("samsung_discovery", "1");
                                this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
                                if (!this.mWifiNative.p2pSet("fw_invite", "1")) {
                                    loge("Failed to set fw_invite");
                                }
                                this.mP2pStaticIpConfig = new WifiP2pStaticIpConfig();
                                this.mWifiNative.p2pSet("static_ip", WifiP2pServiceImpl.DEFAULT_STATIC_IP);
                                this.mP2pStaticIpConfig.mThisDeviceStaticIp = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.DEFAULT_STATIC_IP));
                                String deviceAddr = this.mWifiNative.p2pGetDeviceAddress();
                                if (deviceAddr != null && !deviceAddr.isEmpty()) {
                                    WifiP2pServiceImpl.this.mThisDevice.deviceAddress = deviceAddr;
                                    if (!WifiP2pServiceImpl.this.mContext.getResources().getBoolean(17891609)) {
                                        String unused4 = WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS = deviceAddr;
                                    }
                                }
                                updateThisDevice(3);
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                                }
                                this.mWifiNative.p2pFlush();
                                this.mWifiNative.p2pServiceFlush();
                                byte unused5 = WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
                                String unused6 = WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                                String countryCode = Settings.Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_country_code");
                                if (countryCode != null && !countryCode.isEmpty()) {
                                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.SET_COUNTRY_CODE, countryCode);
                                }
                                updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                                enableVerboseLogging(WifiP2pServiceImpl.this.mFrameworkFacade.getIntegerSetting(WifiP2pServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0));
                            }

                            /* access modifiers changed from: private */
                            public void updateThisDevice(int status) {
                                WifiP2pServiceImpl.this.mThisDevice.status = status;
                                sendThisDeviceChangedBroadcast();
                            }

                            /* access modifiers changed from: private */
                            public void handleGroupCreationFailure() {
                                resetWifiP2pInfo();
                                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.FAILED, (String) null, (String) null);
                                sendP2pConnectionChangedBroadcast();
                                boolean peersChanged = this.mPeers.remove(this.mPeersLostDuringConnection);
                                WifiP2pConfig wifiP2pConfig = this.mSavedPeerConfig;
                                if (!(wifiP2pConfig == null || TextUtils.isEmpty(wifiP2pConfig.deviceAddress) || this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) == null)) {
                                    peersChanged = true;
                                }
                                if (peersChanged) {
                                    sendPeersChangedBroadcast();
                                }
                                this.mWifiNative.p2pFlush();
                                this.mPeersLostDuringConnection.clear();
                                String unused = WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                                if (this.mSavedPeerConfig != null) {
                                    this.mSavedPeerConfig = null;
                                }
                                int unused2 = WifiP2pServiceImpl.this.mReinvokePersistentNetId = -1;
                                this.mP2pStaticIpConfig.candidateStaticIp = 0;
                                boolean unused3 = WifiP2pServiceImpl.this.mValidFreqConflict = false;
                                boolean unused4 = WifiP2pServiceImpl.this.mAdvancedOppReceiver = false;
                                boolean unused5 = WifiP2pServiceImpl.this.mAdvancedOppSender = false;
                                boolean unused6 = WifiP2pServiceImpl.this.mAdvancedOppInProgress = false;
                                int unused7 = WifiP2pServiceImpl.this.mAdvancedOppScanRetryCount = 0;
                            }

                            /* access modifiers changed from: private */
                            public void handleGroupRemoved() {
                                if (this.mGroup.isGroupOwner()) {
                                    stopDhcpServer(this.mGroup.getInterface());
                                } else if (this.mP2pStaticIpConfig.isStaticIp) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("initialize P2pStaticIpConfig");
                                    }
                                    this.mP2pStaticIpConfig.isStaticIp = false;
                                } else {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("stop IpClient");
                                    }
                                    WifiP2pServiceImpl.this.stopIpClient();
                                    try {
                                        WifiP2pServiceImpl.this.mNwService.removeInterfaceFromLocalNetwork(this.mGroup.getInterface());
                                    } catch (RemoteException e) {
                                        loge("Failed to remove iface from local network " + e);
                                    }
                                }
                                try {
                                    WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses(this.mGroup.getInterface());
                                } catch (Exception e2) {
                                    loge("Failed to clear addresses " + e2);
                                }
                                this.mWifiNative.setP2pGroupIdle(this.mGroup.getInterface(), 0);
                                boolean peersChanged = false;
                                for (WifiP2pDevice d : this.mGroup.getClientList()) {
                                    if (this.mPeers.remove(d)) {
                                        peersChanged = true;
                                    }
                                }
                                if (this.mPeers.remove(this.mGroup.getOwner())) {
                                    peersChanged = true;
                                }
                                if (this.mPeers.remove(this.mPeersLostDuringConnection)) {
                                    peersChanged = true;
                                }
                                if (peersChanged) {
                                    sendPeersChangedBroadcast();
                                }
                                if (!this.mGroup.isGroupOwner()) {
                                    this.mGroupBackup = this.mGroup;
                                }
                                this.mGroup = null;
                                if (!WifiP2pServiceImpl.this.mAdvancedOppRemoveGroupAndJoin) {
                                    this.mWifiNative.p2pFlush();
                                }
                                int unused = WifiP2pServiceImpl.this.mConnectedDevicesCnt = 0;
                                int unused2 = WifiP2pServiceImpl.this.mMaxClientCnt = 0;
                                this.mSelectP2pConfigList = null;
                                this.mSelectP2pConfigIndex = 0;
                                this.mIsGotoJoinState = false;
                                this.mPeersLostDuringConnection.clear();
                                String unused3 = WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                                boolean unused4 = WifiP2pServiceImpl.this.mValidFreqConflict = false;
                                if (!WifiP2pServiceImpl.this.mAdvancedOppReceiver && !WifiP2pServiceImpl.this.mAdvancedOppSender) {
                                    boolean unused5 = WifiP2pServiceImpl.this.mAdvancedOppInProgress = false;
                                }
                                boolean unused6 = WifiP2pServiceImpl.this.mAdvancedOppReceiver = false;
                                boolean unused7 = WifiP2pServiceImpl.this.mAdvancedOppSender = false;
                                removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DISCOVER_PEER);
                                removeMessages(WifiP2pServiceImpl.P2P_ADVOPP_DELAYED_DISCOVER_PEER);
                                int unused8 = WifiP2pServiceImpl.this.mAdvancedOppScanRetryCount = 0;
                                if (!WifiP2pServiceImpl.this.mAdvancedOppInProgress) {
                                    stopLegacyWifiScan(false);
                                }
                                if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                                    if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                                    } else {
                                        loge("handleGroupRemoved(): WifiChannel is null");
                                    }
                                    boolean unused9 = WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
                                }
                                this.mWifiNative.p2pSet("static_ip", WifiP2pServiceImpl.DEFAULT_STATIC_IP);
                                WifiP2pStaticIpConfig wifiP2pStaticIpConfig = this.mP2pStaticIpConfig;
                                wifiP2pStaticIpConfig.candidateStaticIp = 0;
                                wifiP2pStaticIpConfig.mThisDeviceStaticIp = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.DEFAULT_STATIC_IP));
                            }

                            /* access modifiers changed from: private */
                            public void showP2pConnectedNotification() {
                                Resources r = Resources.getSystem();
                                NotificationManager notificationManager = (NotificationManager) WifiP2pServiceImpl.this.mContext.getSystemService("notification");
                                if (notificationManager != null) {
                                    NotificationChannel notificationChannel = new NotificationChannel(WifiP2pServiceImpl.WIFI_P2P_NOTIFICATION_CHANNEL, r.getString(17042717), 2);
                                    Intent intent = new Intent("com.samsung.settings.WIFI_DIRECT_SETTINGS");
                                    intent.setFlags(604012544);
                                    PendingIntent pi = PendingIntent.getActivity(WifiP2pServiceImpl.this.mContext, 0, intent, 0);
                                    CharSequence title = r.getString(17042719);
                                    CharSequence message = r.getText(17042718);
                                    if (WifiP2pServiceImpl.this.mConnectedNotification == null) {
                                        WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
                                        Notification unused = wifiP2pServiceImpl.mConnectedNotification = new Notification.BigTextStyle(new Notification.Builder(wifiP2pServiceImpl.mContext, WifiP2pServiceImpl.WIFI_P2P_NOTIFICATION_CHANNEL).setContentTitle(title).setContentText(message).setSmallIcon(17301642).setWhen(0)).bigText(message).build();
                                        WifiP2pServiceImpl.this.mConnectedNotification.when = 0;
                                        WifiP2pServiceImpl.this.mConnectedNotification.icon = 17301642;
                                        WifiP2pServiceImpl.this.mConnectedNotification.flags = 8;
                                        WifiP2pServiceImpl.this.mConnectedNotification.tickerText = title;
                                    }
                                    WifiP2pServiceImpl.this.mConnectedNotification.setLatestEventInfo(WifiP2pServiceImpl.this.mContext, title, message, pi);
                                    notificationManager.createNotificationChannel(notificationChannel);
                                    notificationManager.notify(WifiP2pServiceImpl.this.mConnectedNotification.icon, WifiP2pServiceImpl.this.mConnectedNotification);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void clearP2pConnectedNotification() {
                                NotificationManager notificationManager = (NotificationManager) WifiP2pServiceImpl.this.mContext.getSystemService("notification");
                                if (notificationManager != null && WifiP2pServiceImpl.this.mConnectedNotification != null) {
                                    notificationManager.cancel(WifiP2pServiceImpl.this.mConnectedNotification.icon);
                                    Notification unused = WifiP2pServiceImpl.this.mConnectedNotification = null;
                                }
                            }

                            /* access modifiers changed from: private */
                            public void showNotification() {
                                showStatusBarIcon();
                            }

                            /* access modifiers changed from: private */
                            public void clearNotification() {
                                clearStatusBarIcon();
                            }

                            private void showStatusBarIcon() {
                                ((StatusBarManager) WifiP2pServiceImpl.this.mContext.getSystemService("statusbar")).setIcon("wifi_p2p", 17304254, 0, WifiP2pServiceImpl.this.mContext.getResources().getString(17042717));
                            }

                            private void clearStatusBarIcon() {
                                ((StatusBarManager) WifiP2pServiceImpl.this.mContext.getSystemService("statusbar")).removeIcon("wifi_p2p");
                            }

                            /* access modifiers changed from: private */
                            public void soundNotification() {
                                try {
                                    Ringtone r = RingtoneManager.getRingtone(WifiP2pServiceImpl.this.mContext, RingtoneManager.getDefaultUri(2));
                                    r.setStreamType(5);
                                    r.play();
                                    if (((AudioManager) WifiP2pServiceImpl.this.mContext.getSystemService("audio")).getRingerMode() == 1) {
                                        ((Vibrator) WifiP2pServiceImpl.this.mContext.getSystemService("vibrator")).vibrate(1000);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            /* access modifiers changed from: private */
                            public void replyToMessage(Message msg, int what) {
                                if (msg.replyTo != null) {
                                    Message dstMsg = obtainMessage(msg);
                                    dstMsg.what = what;
                                    WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void replyToMessage(Message msg, int what, int arg1) {
                                if (msg.replyTo != null) {
                                    Message dstMsg = obtainMessage(msg);
                                    dstMsg.what = what;
                                    dstMsg.arg1 = arg1;
                                    WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void replyToMessage(Message msg, int what, Object obj) {
                                if (msg.replyTo != null) {
                                    Message dstMsg = obtainMessage(msg);
                                    dstMsg.what = what;
                                    dstMsg.obj = obj;
                                    WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
                                }
                            }

                            private Message obtainMessage(Message srcMsg) {
                                Message msg = Message.obtain();
                                msg.arg2 = srcMsg.arg2;
                                return msg;
                            }

                            /* access modifiers changed from: protected */
                            public void logd(String s) {
                                Slog.d(WifiP2pServiceImpl.TAG, s.replaceAll("([0-9a-fA-F]{2}:)([0-9a-fA-F]{2}:){3}([0-9a-fA-F]{2}:[0-9a-fA-F]{2})", "$1$3"));
                            }

                            /* access modifiers changed from: protected */
                            public void loge(String s) {
                                Slog.e(WifiP2pServiceImpl.TAG, s.replaceAll("([0-9a-fA-F]{2}:)([0-9a-fA-F]{2}:){3}([0-9a-fA-F]{2}:[0-9a-fA-F]{2})", "$1$3"));
                            }

                            /* access modifiers changed from: private */
                            public boolean updateSupplicantServiceRequest() {
                                clearSupplicantServiceRequest();
                                StringBuffer sb = new StringBuffer();
                                for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                                    for (int i = 0; i < c.mReqList.size(); i++) {
                                        WifiP2pServiceRequest req = (WifiP2pServiceRequest) c.mReqList.valueAt(i);
                                        if (req != null) {
                                            sb.append(req.getSupplicantQuery());
                                        }
                                    }
                                }
                                if (sb.length() == 0) {
                                    return false;
                                }
                                String unused = WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq(WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS, sb.toString());
                                if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                                    return false;
                                }
                                return true;
                            }

                            /* access modifiers changed from: private */
                            public void clearSupplicantServiceRequest() {
                                if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                                    this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
                                    String unused = WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean addServiceRequest(Messenger m, WifiP2pServiceRequest req) {
                                if (m == null || req == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                    return false;
                                }
                                clearClientDeadChannels();
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo == null) {
                                    return false;
                                }
                                WifiP2pServiceImpl.access$25404(WifiP2pServiceImpl.this);
                                if (WifiP2pServiceImpl.this.mServiceTransactionId == 0) {
                                    WifiP2pServiceImpl.access$25404(WifiP2pServiceImpl.this);
                                }
                                req.setTransactionId(WifiP2pServiceImpl.this.mServiceTransactionId);
                                clientInfo.mReqList.put(WifiP2pServiceImpl.this.mServiceTransactionId, req);
                                if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                                    return true;
                                }
                                return updateSupplicantServiceRequest();
                            }

                            /* access modifiers changed from: private */
                            public void removeServiceRequest(Messenger m, WifiP2pServiceRequest req) {
                                if (m == null || req == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                }
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo != null) {
                                    boolean removed = false;
                                    int i = 0;
                                    while (true) {
                                        if (i >= clientInfo.mReqList.size()) {
                                            break;
                                        } else if (req.equals(clientInfo.mReqList.valueAt(i))) {
                                            removed = true;
                                            clientInfo.mReqList.removeAt(i);
                                            break;
                                        } else {
                                            i++;
                                        }
                                    }
                                    if (removed && WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                                        updateSupplicantServiceRequest();
                                    }
                                }
                            }

                            /* access modifiers changed from: private */
                            public void clearServiceRequests(Messenger m) {
                                if (m == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                    return;
                                }
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo != null && clientInfo.mReqList.size() != 0) {
                                    clientInfo.mReqList.clear();
                                    if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                                        updateSupplicantServiceRequest();
                                    }
                                }
                            }

                            /* access modifiers changed from: private */
                            public void addP2pPktLogFilter() {
                                WifiNative wifiNative = this.mWifiLegacyNative;
                                String str = this.mWifiInterface;
                                if (wifiNative.setPktlogFilter(str, this.filterOffset + " " + this.filterMaskSSDP + " " + this.filterSSDP)) {
                                    Log.i(WifiP2pServiceImpl.TAG, "addP2pPktLogFilter = [" + this.mWifiInterface + "]");
                                    WifiNative wifiNative2 = this.mWifiLegacyNative;
                                    String str2 = this.mWifiInterface;
                                    wifiNative2.setPktlogFilter(str2, this.filterOffset + " " + this.filterMaskRTSPDst + " " + this.filterRTSPDst);
                                    WifiNative wifiNative3 = this.mWifiLegacyNative;
                                    String str3 = this.mWifiInterface;
                                    wifiNative3.setPktlogFilter(str3, this.filterOffset + " " + this.filterMaskRTSPSrc + " " + this.filterRTSPSrc);
                                    WifiNative wifiNative4 = this.mWifiLegacyNative;
                                    String str4 = this.mWifiInterface;
                                    wifiNative4.setPktlogFilter(str4, this.filterOffset + " " + this.filterMaskRTCP + " " + this.filterRTCP);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void removeP2pPktLogFilter() {
                                WifiNative wifiNative = this.mWifiLegacyNative;
                                String str = this.mWifiInterface;
                                if (wifiNative.removePktlogFilter(str, this.filterOffset + " " + this.filterMaskSSDP + " " + this.filterSSDP)) {
                                    Log.i(WifiP2pServiceImpl.TAG, "removeP2pPktLogFilter = [" + this.mWifiInterface + "]");
                                    WifiNative wifiNative2 = this.mWifiLegacyNative;
                                    String str2 = this.mWifiInterface;
                                    wifiNative2.removePktlogFilter(str2, this.filterOffset + " " + this.filterMaskRTSPDst + " " + this.filterRTSPDst);
                                    WifiNative wifiNative3 = this.mWifiLegacyNative;
                                    String str3 = this.mWifiInterface;
                                    wifiNative3.removePktlogFilter(str3, this.filterOffset + " " + this.filterMaskRTSPSrc + " " + this.filterRTSPSrc);
                                    WifiNative wifiNative4 = this.mWifiLegacyNative;
                                    String str4 = this.mWifiInterface;
                                    wifiNative4.removePktlogFilter(str4, this.filterOffset + " " + this.filterMaskRTCP + " " + this.filterRTCP);
                                }
                            }

                            private void saveFwDumpByP2P() {
                                Log.i(WifiP2pServiceImpl.TAG, "saveFwDumpByP2P mWifiInterface = [" + this.mWifiInterface + "]");
                                this.mWifiLegacyNative.saveFwDump(this.mWifiInterface);
                            }

                            /* access modifiers changed from: private */
                            public boolean addLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
                                if (m == null || servInfo == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                                    return false;
                                }
                                clearClientDeadChannels();
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo == null || !clientInfo.mServList.add(servInfo)) {
                                    return false;
                                }
                                if (this.mWifiNative.p2pServiceAdd(servInfo)) {
                                    return true;
                                }
                                clientInfo.mServList.remove(servInfo);
                                return false;
                            }

                            /* access modifiers changed from: private */
                            public void removeLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
                                if (m == null || servInfo == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                                    return;
                                }
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo != null) {
                                    this.mWifiNative.p2pServiceDel(servInfo);
                                    clientInfo.mServList.remove(servInfo);
                                }
                            }

                            /* access modifiers changed from: private */
                            public void clearLocalServices(Messenger m) {
                                if (m == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                    return;
                                }
                                ClientInfo clientInfo = getClientInfo(m, false);
                                if (clientInfo != null) {
                                    for (WifiP2pServiceInfo servInfo : clientInfo.mServList) {
                                        this.mWifiNative.p2pServiceDel(servInfo);
                                    }
                                    clientInfo.mServList.clear();
                                }
                            }

                            /* access modifiers changed from: private */
                            public void clearClientInfo(Messenger m) {
                                clearLocalServices(m);
                                clearServiceRequests(m);
                                ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.remove(m);
                                if (clientInfo != null) {
                                    logd("Client:" + clientInfo.mPackageName + " is removed");
                                }
                            }

                            /* access modifiers changed from: private */
                            public void sendServiceResponse(WifiP2pServiceResponse resp) {
                                if (resp == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "sendServiceResponse with null response");
                                    return;
                                }
                                for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                                    if (((WifiP2pServiceRequest) c.mReqList.get(resp.getTransactionId())) != null) {
                                        Message msg = Message.obtain();
                                        msg.what = 139314;
                                        msg.arg1 = 0;
                                        msg.arg2 = 0;
                                        msg.obj = resp;
                                        if (c.mMessenger != null) {
                                            try {
                                                c.mMessenger.send(msg);
                                            } catch (RemoteException e) {
                                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                    logd("detect dead channel");
                                                }
                                                clearClientInfo(c.mMessenger);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }

                            private void clearClientDeadChannels() {
                                ArrayList<Messenger> deadClients = new ArrayList<>();
                                for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                                    Message msg = Message.obtain();
                                    msg.what = 139313;
                                    msg.arg1 = 0;
                                    msg.arg2 = 0;
                                    msg.obj = null;
                                    if (c.mMessenger != null) {
                                        try {
                                            c.mMessenger.send(msg);
                                        } catch (RemoteException e) {
                                            if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                                logd("detect dead channel");
                                            }
                                            deadClients.add(c.mMessenger);
                                        }
                                    }
                                }
                                Iterator<Messenger> it = deadClients.iterator();
                                while (it.hasNext()) {
                                    clearClientInfo(it.next());
                                }
                            }

                            /* access modifiers changed from: private */
                            public ClientInfo getClientInfo(Messenger m, boolean createIfNotExist) {
                                ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(m);
                                if (clientInfo != null || !createIfNotExist) {
                                    return clientInfo;
                                }
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("add a new client");
                                }
                                ClientInfo clientInfo2 = new ClientInfo(m);
                                WifiP2pServiceImpl.this.mClientInfoList.put(m, clientInfo2);
                                return clientInfo2;
                            }

                            private void sendDetachedMsg(int reason) {
                                if (WifiP2pServiceImpl.this.mForegroundAppMessenger != null) {
                                    Message msg = Message.obtain();
                                    msg.what = 139381;
                                    msg.arg1 = reason;
                                    try {
                                        WifiP2pServiceImpl.this.mForegroundAppMessenger.send(msg);
                                    } catch (RemoteException e) {
                                    }
                                    Messenger unused = WifiP2pServiceImpl.this.mForegroundAppMessenger = null;
                                    String unused2 = WifiP2pServiceImpl.this.mForegroundAppPkgName = null;
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean sendShowPinReqToFrontApp(String pin) {
                                if (!isForegroundApp(WifiP2pServiceImpl.this.mForegroundAppPkgName)) {
                                    sendDetachedMsg(4);
                                    return false;
                                }
                                Message msg = Message.obtain();
                                msg.what = 139384;
                                Bundle bundle = new Bundle();
                                bundle.putString("wpsPin", pin);
                                msg.setData(bundle);
                                return sendDialogMsgToFrontApp(msg);
                            }

                            /* access modifiers changed from: private */
                            public boolean sendConnectNoticeToApp(WifiP2pDevice dev, WifiP2pConfig config) {
                                if (dev == null) {
                                    dev = new WifiP2pDevice(config.deviceAddress);
                                }
                                if (!isForegroundApp(WifiP2pServiceImpl.this.mForegroundAppPkgName)) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("application is NOT foreground");
                                    }
                                    sendDetachedMsg(4);
                                    return false;
                                }
                                Message msg = Message.obtain();
                                msg.what = 139383;
                                Bundle bundle = new Bundle();
                                bundle.putParcelable("wifiP2pDevice", dev);
                                bundle.putParcelable("wifiP2pConfig", config);
                                msg.setData(bundle);
                                return sendDialogMsgToFrontApp(msg);
                            }

                            private boolean sendDialogMsgToFrontApp(Message msg) {
                                try {
                                    WifiP2pServiceImpl.this.mForegroundAppMessenger.send(msg);
                                    return true;
                                } catch (RemoteException e) {
                                    Messenger unused = WifiP2pServiceImpl.this.mForegroundAppMessenger = null;
                                    String unused2 = WifiP2pServiceImpl.this.mForegroundAppPkgName = null;
                                    return false;
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean setDialogListenerApp(Messenger m, String appPkgName, boolean isReset) {
                                if (WifiP2pServiceImpl.this.mForegroundAppPkgName != null && !WifiP2pServiceImpl.this.mForegroundAppPkgName.equals(appPkgName)) {
                                    if (isForegroundApp(WifiP2pServiceImpl.this.mForegroundAppPkgName)) {
                                        if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                            logd("application is NOT foreground");
                                        }
                                        return false;
                                    }
                                    sendDetachedMsg(4);
                                }
                                if (isReset) {
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("reset dialog listener");
                                    }
                                    Messenger unused = WifiP2pServiceImpl.this.mForegroundAppMessenger = null;
                                    String unused2 = WifiP2pServiceImpl.this.mForegroundAppPkgName = null;
                                    return true;
                                } else if (!isForegroundApp(appPkgName)) {
                                    return false;
                                } else {
                                    Messenger unused3 = WifiP2pServiceImpl.this.mForegroundAppMessenger = m;
                                    String unused4 = WifiP2pServiceImpl.this.mForegroundAppPkgName = appPkgName;
                                    if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                        logd("set dialog listener. app=" + appPkgName);
                                    }
                                    return true;
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean isForegroundApp(String pkgName) {
                                if (pkgName == null) {
                                    return false;
                                }
                                List<ActivityManager.RunningTaskInfo> tasks = WifiP2pServiceImpl.this.mActivityMgr.getRunningTasks(1);
                                if (tasks.size() == 0) {
                                    return false;
                                }
                                return pkgName.equals(tasks.get(0).baseActivity.getPackageName());
                            }

                            /* access modifiers changed from: private */
                            public WifiP2pDeviceList getPeers(String pkgName, int uid) {
                                if (WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkCanAccessWifiDirect(pkgName, uid, true)) {
                                    return new WifiP2pDeviceList(this.mPeers);
                                }
                                return new WifiP2pDeviceList();
                            }

                            private void setPendingFactoryReset(boolean pending) {
                                WifiP2pServiceImpl.this.mFrameworkFacade.setIntegerSetting(WifiP2pServiceImpl.this.mContext, "wifi_p2p_pending_factory_reset", pending);
                            }

                            /* access modifiers changed from: private */
                            public boolean isPendingFactoryReset() {
                                if (WifiP2pServiceImpl.this.mFrameworkFacade.getIntegerSetting(WifiP2pServiceImpl.this.mContext, "wifi_p2p_pending_factory_reset", 0) != 0) {
                                    return true;
                                }
                                return false;
                            }

                            /* access modifiers changed from: private */
                            public boolean factoryReset(int uid) {
                                String pkgName = WifiP2pServiceImpl.this.mContext.getPackageManager().getNameForUid(uid);
                                UserManager userManager = WifiP2pServiceImpl.this.mWifiInjector.getUserManager();
                                if (!WifiP2pServiceImpl.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid) || userManager.hasUserRestriction("no_network_reset") || userManager.hasUserRestriction("no_config_wifi")) {
                                    return false;
                                }
                                Log.i(WifiP2pServiceImpl.TAG, "factoryReset uid=" + uid + " pkg=" + pkgName);
                                if (WifiP2pServiceImpl.this.mNetworkInfo.isAvailable()) {
                                    if (this.mWifiNative.p2pListNetworks(this.mGroups)) {
                                        for (WifiP2pGroup group : this.mGroups.getGroupList()) {
                                            this.mWifiNative.removeP2pNetwork(group.getNetworkId());
                                        }
                                    }
                                    updatePersistentNetworks(true);
                                    setPendingFactoryReset(false);
                                } else {
                                    setPendingFactoryReset(true);
                                }
                                return true;
                            }

                            /* access modifiers changed from: private */
                            public String getCallingPkgName(int uid, Messenger replyMessenger) {
                                ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(replyMessenger);
                                if (clientInfo != null) {
                                    return clientInfo.mPackageName;
                                }
                                if (uid == 1000) {
                                    return WifiP2pServiceImpl.this.mContext.getOpPackageName();
                                }
                                return null;
                            }

                            /* access modifiers changed from: private */
                            public void clearServicesForAllClients() {
                                for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                                    clearLocalServices(c.mMessenger);
                                    clearServiceRequests(c.mMessenger);
                                }
                            }

                            /* access modifiers changed from: private */
                            public boolean setP2pGroupForSamsung() {
                                NetworkInfo netInfo;
                                boolean ret = false;
                                WifiManager tWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
                                int frequency = 0;
                                if (!(tWifiManager == null || (netInfo = tWifiManager.getNetworkInfo()) == null || netInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED || tWifiManager.getConnectionInfo() == null)) {
                                    frequency = tWifiManager.getConnectionInfo().getFrequency();
                                }
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    logd("legacy AP frequency : " + frequency);
                                }
                                if (WifiP2pServiceImpl.this.mAdvancedOppSender || WifiP2pServiceImpl.this.mAdvancedOppReceiver) {
                                    if (frequency >= 5170) {
                                        ret = this.mWifiNative.p2pGroupAdd(false, frequency);
                                    } else {
                                        ret = this.mWifiNative.p2pGroupAdd(false, 5);
                                    }
                                } else if (frequency <= 2472) {
                                    ret = this.mWifiNative.p2pGroupAdd(false, frequency);
                                }
                                if (!ret) {
                                    ret = this.mWifiNative.p2pGroupAdd(false, 5);
                                }
                                if (!ret) {
                                    return this.mWifiNative.p2pGroupAdd(false, 2);
                                }
                                return ret;
                            }

                            private String getWifiHwMac() {
                                String macAddress;
                                if (WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(WifiP2pServiceImpl.WIFI_HW_MAC_ADDRESS) && (macAddress = this.mWifiLegacyNative.getVendorConnFileInfo(0)) != null && macAddress.length() >= 17) {
                                    String unused = WifiP2pServiceImpl.WIFI_HW_MAC_ADDRESS = macAddress.substring(0, 17);
                                }
                                if (WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    Log.i(WifiP2pServiceImpl.TAG, "getWifiHwMac: " + WifiP2pServiceImpl.WIFI_HW_MAC_ADDRESS);
                                }
                                return WifiP2pServiceImpl.WIFI_HW_MAC_ADDRESS;
                            }

                            /* access modifiers changed from: private */
                            public boolean makeP2pHwMac() {
                                if (WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS)) {
                                    byte[] bArr = new byte[6];
                                    byte[] addr = WifiP2pServiceImpl.macAddressToByteArray(getWifiHwMac());
                                    if (WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(WifiP2pServiceImpl.toHexString(addr))) {
                                        Log.i(WifiP2pServiceImpl.TAG, "makeP2pHwMac: wifihwmac is empty");
                                        return false;
                                    }
                                    addr[0] = (byte) (addr[0] | 2);
                                    String unused = WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS = WifiP2pServiceImpl.toHexString(addr);
                                }
                                if (!WifiP2pServiceImpl.mVerboseLoggingEnabled) {
                                    return true;
                                }
                                Log.i(WifiP2pServiceImpl.TAG, "makeP2pHwMac: " + WifiP2pServiceImpl.P2P_HW_MAC_ADDRESS);
                                return true;
                            }
                        }

                        private class ClientInfo {
                            /* access modifiers changed from: private */
                            public Messenger mMessenger;
                            /* access modifiers changed from: private */
                            public String mPackageName;
                            /* access modifiers changed from: private */
                            public SparseArray<WifiP2pServiceRequest> mReqList;
                            /* access modifiers changed from: private */
                            public List<WifiP2pServiceInfo> mServList;

                            private ClientInfo(Messenger m) {
                                this.mMessenger = m;
                                this.mPackageName = null;
                                this.mReqList = new SparseArray<>();
                                this.mServList = new ArrayList();
                            }
                        }

                        /* access modifiers changed from: private */
                        public void auditLog(boolean outcome, String msg) {
                            auditLog(outcome, msg, 5);
                        }

                        /* access modifiers changed from: private */
                        public void auditLog(boolean outcome, String msg, int group) {
                            try {
                                Uri uri = Uri.parse("content://com.sec.knox.provider/AuditLog");
                                ContentValues cv = new ContentValues();
                                cv.put("severity", 5);
                                cv.put("group", Integer.valueOf(group));
                                cv.put("outcome", Boolean.valueOf(outcome));
                                cv.put("uid", Integer.valueOf(Process.myPid()));
                                cv.put("component", "WifiP2pServiceImpl");
                                cv.put("message", msg);
                                this.mContext.getContentResolver().insert(uri, cv);
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, e.toString());
                            }
                        }

                        /* access modifiers changed from: private */
                        /* JADX WARNING: Code restructure failed: missing block: B:2:0x0004, code lost:
                            r0 = android.net.Uri.parse("content://com.sec.knox.provider/RestrictionPolicy4");
                         */
                        /* Code decompiled incorrectly, please refer to instructions dump. */
                        public boolean isAllowWifiDirectByEDM() {
                            /*
                                r7 = this;
                                android.content.Context r0 = r7.mContext
                                if (r0 == 0) goto L_0x0051
                                java.lang.String r0 = "content://com.sec.knox.provider/RestrictionPolicy4"
                                android.net.Uri r0 = android.net.Uri.parse(r0)
                                java.lang.String r1 = "true"
                                java.lang.String[] r5 = new java.lang.String[]{r1}
                                android.content.Context r1 = r7.mContext
                                android.content.ContentResolver r1 = r1.getContentResolver()
                                r3 = 0
                                r6 = 0
                                java.lang.String r4 = "isWifiDirectAllowed"
                                r2 = r0
                                android.database.Cursor r1 = r1.query(r2, r3, r4, r5, r6)
                                if (r1 == 0) goto L_0x0051
                                r1.moveToFirst()     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                                java.lang.String r2 = "isWifiDirectAllowed"
                                int r2 = r1.getColumnIndex(r2)     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                                java.lang.String r2 = r1.getString(r2)     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                                java.lang.String r3 = "false"
                                boolean r2 = r2.equals(r3)     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                                if (r2 == 0) goto L_0x004d
                                boolean r2 = mVerboseLoggingEnabled     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                                if (r2 == 0) goto L_0x0041
                                java.lang.String r2 = "WifiP2pService"
                                java.lang.String r3 = "isAllowWifiDirectByEDM() : wifi direct is not allowed."
                                android.util.Log.d(r2, r3)     // Catch:{ Exception -> 0x004c, all -> 0x0047 }
                            L_0x0041:
                                r2 = 0
                                r1.close()
                                return r2
                            L_0x0047:
                                r2 = move-exception
                                r1.close()
                                throw r2
                            L_0x004c:
                                r2 = move-exception
                            L_0x004d:
                                r1.close()
                            L_0x0051:
                                r0 = 1
                                return r0
                            */
                            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.p2p.WifiP2pServiceImpl.isAllowWifiDirectByEDM():boolean");
                        }

                        private void sendPingForArpUpdate(InetAddress address) {
                            if (address != null) {
                                try {
                                    if (address.isReachable(2000)) {
                                        Log.i(TAG, "sendPingForArpUpdate (SUCCESS)");
                                    } else {
                                        Log.i(TAG, "sendPingForArpUpdate (FAILED)");
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }

                        /* access modifiers changed from: private */
                        public static boolean isTablet() {
                            String deviceType = SystemProperties.get("ro.build.characteristics");
                            return deviceType != null && deviceType.contains("tablet");
                        }

                        /* access modifiers changed from: private */
                        public void connectRetryCount(String peerAddr, int count) {
                            Integer mode = this.mModeChange.get(peerAddr);
                            if (mode != null) {
                                int modeValue = mode.intValue();
                                if (count > 0 && (modeValue & 3) < 3) {
                                    modeValue++;
                                } else if (count != 0 || modeValue <= 0) {
                                    if (count < 0 && modeValue > 0) {
                                        modeValue--;
                                    }
                                } else if ((modeValue & 3) == 1) {
                                    this.mModeChange.remove(peerAddr);
                                    return;
                                } else {
                                    modeValue &= ~(modeValue & 3);
                                }
                                Log.d(TAG, "Connection Mode : " + modeValue);
                                this.mModeChange.put(peerAddr, Integer.valueOf(modeValue));
                            }
                        }

                        private class WifiP2pConnectReqInfo {
                            private int connectionReceived;
                            private int isJoin;
                            private int isPersistent;
                            /* access modifiers changed from: private */
                            public WifiP2pDevice peerDev;
                            private int peerDevType = -1;
                            private int peerGOIntentValue = -1;
                            private String peerManufacturer;
                            /* access modifiers changed from: private */
                            public String pkgName;

                            public WifiP2pConnectReqInfo() {
                            }

                            public WifiP2pConnectReqInfo(WifiP2pConnectReqInfo source) {
                                if (source != null) {
                                    this.peerDev = new WifiP2pDevice(source.peerDev);
                                    this.pkgName = source.pkgName;
                                    this.connectionReceived = source.connectionReceived;
                                    this.isPersistent = source.isPersistent;
                                    this.isJoin = source.isJoin;
                                    this.peerManufacturer = source.peerManufacturer;
                                    this.peerDevType = source.peerDevType;
                                    this.peerGOIntentValue = source.peerGOIntentValue;
                                }
                            }

                            public void set(WifiP2pDevice dev, String name, int conn, int p, int j, String ma, String typeStr) {
                                this.peerDev = dev;
                                if (name != null) {
                                    this.pkgName = name.replaceAll("\\s+", "");
                                }
                                this.connectionReceived = conn;
                                this.isPersistent = p;
                                this.isJoin = j;
                                if (ma != null) {
                                    this.peerManufacturer = ma.replaceAll("\\s+", "");
                                }
                                if (name == null || TextUtils.isEmpty(this.pkgName)) {
                                    this.pkgName = "unknown";
                                }
                                if (ma == null || TextUtils.isEmpty(this.peerManufacturer)) {
                                    this.peerManufacturer = "unknown";
                                }
                                if (typeStr != null) {
                                    String[] tokens = typeStr.split("-");
                                    if (tokens.length == 3) {
                                        try {
                                            this.peerDevType = Integer.parseInt(tokens[0]);
                                        } catch (NumberFormatException e) {
                                            Log.e(WifiP2pServiceImpl.TAG, "NumberFormatException while getting peerDevType");
                                        }
                                    }
                                }
                            }

                            public void reset() {
                                this.peerDev = null;
                                this.pkgName = null;
                                this.connectionReceived = 0;
                                this.isPersistent = 0;
                                this.isJoin = 0;
                                this.peerManufacturer = null;
                                this.peerDevType = -1;
                                this.peerGOIntentValue = -1;
                            }

                            public String getPeerManufacturer() {
                                return this.peerManufacturer;
                            }

                            public int getPeerDevType() {
                                return this.peerDevType;
                            }

                            public void setPeerGOIntentValue(int val) {
                                this.peerGOIntentValue = val;
                            }

                            public int getPeerGOIntentValue() {
                                return this.peerGOIntentValue;
                            }

                            public String toString() {
                                return this.pkgName + " " + this.connectionReceived + " " + this.isPersistent + " " + this.isJoin + " " + this.peerManufacturer + " " + this.peerDevType + " " + this.peerGOIntentValue + " ";
                            }
                        }

                        private class WifiP2pConnectedPeriodInfo {
                            /* access modifiers changed from: private */
                            public WifiP2pDevice peerDev = null;
                            /* access modifiers changed from: private */
                            public String pkgName = null;
                            /* access modifiers changed from: private */
                            public long startTime = 0;

                            public WifiP2pConnectedPeriodInfo() {
                            }
                        }

                        /* access modifiers changed from: private */
                        public void checkAndShowP2pEnableDialog(int req_type) {
                            if (req_type == 0) {
                                Log.d(TAG, "P2P is not allowed because MHS is enabled");
                                showWifiEnableWarning(2, 0);
                            } else if (req_type == 1) {
                                Log.d(TAG, "P2P is not allowed because NAN is enabled");
                                showWifiEnableWarning(2, 1);
                            }
                        }

                        private void showWifiEnableWarning(int extra_type, int req_type) {
                            Intent intent = new Intent();
                            intent.setClassName(WIFI_DIRECT_SETTINGS_PKGNAME, "com.samsung.android.settings.wifi.WifiWarning");
                            intent.putExtra("req_type", req_type);
                            intent.putExtra("extra_type", extra_type);
                            intent.setFlags(1015021568);
                            try {
                                this.mContext.startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception is occured in showWifiEnableWarning.");
                            }
                        }

                        /* access modifiers changed from: private */
                        public void dialogRejectForThemeChanging() {
                            auditLog(true, "Wi-Fi Direct (P2P) invitation denied");
                            this.mP2pStateMachine.sendMessage(PEER_CONNECTION_USER_REJECT);
                            this.mLapseTime = 30;
                            this.mWpsTimer.cancel();
                            this.mDialogWakeLock.release();
                            this.imm = (InputMethodManager) this.mContext.getSystemService("input_method");
                            InputMethodManager inputMethodManager = this.imm;
                            if (inputMethodManager != null) {
                                inputMethodManager.hideSoftInputFromWindow(this.pinConn.getWindowToken(), 0);
                            }
                            this.mInvitationDialog.dismiss();
                            this.mInvitationDialog = null;
                        }

                        /* access modifiers changed from: private */
                        public byte[] createRandomMac() {
                            byte[] addr = new byte[6];
                            new Random().nextBytes(addr);
                            addr[0] = (byte) (addr[0] & 254);
                            addr[0] = (byte) (addr[0] | 2);
                            return addr;
                        }

                        public static String toHexString(byte[] data) {
                            if (data == null) {
                                return "";
                            }
                            StringBuilder sb = new StringBuilder(data.length * 3);
                            boolean first = true;
                            for (byte b : data) {
                                if (first) {
                                    first = false;
                                } else {
                                    sb.append(':');
                                }
                                sb.append(String.format("%02x", new Object[]{Integer.valueOf(b & 255)}));
                            }
                            return sb.toString();
                        }

                        public static byte[] macAddressToByteArray(String macStr) {
                            if (TextUtils.isEmpty(macStr)) {
                                return null;
                            }
                            String cleanMac = macStr.replace(":", "");
                            if (cleanMac.length() == 12) {
                                return HexEncoding.decode(cleanMac.toCharArray(), false);
                            }
                            throw new IllegalArgumentException("invalid mac string length: " + cleanMac);
                        }

                        /* access modifiers changed from: private */
                        public void allowForcingEnableP2pForApp(String callPkgName) {
                            if (callPkgName == null || callPkgName.isEmpty() || (!callPkgName.equals("WifiWarning") && !callPkgName.equals("SmartView") && !callPkgName.equals("BluetoothCast"))) {
                                Log.i(TAG, "allowForcingEnableP2pForApp false");
                                this.mReceivedEnableP2p = false;
                                return;
                            }
                            Log.i(TAG, "allowForcingEnableP2pForApp true: called: " + callPkgName);
                            this.mReceivedEnableP2p = true;
                        }

                        public boolean isSetupInterfaceRunning() {
                            return this.mSetupInterfaceIsRunnging;
                        }
                    }
