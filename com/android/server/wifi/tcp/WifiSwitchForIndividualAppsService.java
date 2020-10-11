package com.android.server.wifi.tcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.IIpConnectivityMetrics;
import android.net.INetd;
import android.net.INetdEventCallback;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.net.BaseNetdEventCallback;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.WifiConnectivityMonitor;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.tcp.WifiApInfo;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.sns.SnsBigDataManager;
import com.samsung.android.server.wifi.sns.SnsBigDataTCPE;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WifiSwitchForIndividualAppsService extends Handler {
    private static final int BASE = 135668;
    /* access modifiers changed from: private */
    public static boolean DBG = Debug.semIsProductDev();
    private static final boolean DNS_ROUTE_CHANGE_ENABLED = true;
    public static final int SWITCH_TO_MOBILE_DATA_DISABLED = 135679;
    public static final int SWITCH_TO_MOBILE_DATA_ENABLED = 135678;
    public static final int SWITCH_TO_MOBILE_DATA_QAI = 135680;
    private static final String TAG = "WifiSwitchForIndividualAppsService";
    private static final boolean TCP_MONITOR_FEATURE_ENABLED = true;
    private static Object mTCPEdatalock = new Object();
    private final int BEACON_AVERAGE_RATE_TRHESHOLD;
    private final int BEACON_COUNT_RANGE;
    private final int BEACON_SAMPLE_THRESHOLD;
    private final int DETECTION_MODE_ALL_APP_EXCEPT_SYSTEM;
    private final int DETECTION_MODE_CHAT_ONLY;
    private final int DETECTION_MODE_FILTERED_APP;
    private final String EXTRA_SHOW_SWITCH_FOR_INDIVIDUAL_APPS;
    private final int INVALID_UID;
    private final String KEY_TCP_MONITOR_COMMAND;
    private final int LATEST_TX_RX_LENGTH;
    private final int MOBILE_DATA_ENABLE_CHECK_DELAY;
    private final int MOBILE_DATA_ENABLE_CHECK_MAX;
    private final int RECEIVE_QUEUE_COLUMN = 1;
    private final String RESULT_BLOCKING_DNS = "BD";
    private final String RESULT_BLOCKING_RECEIEVE_QUEUE_SYN = "BQ";
    private final String RESULT_BLOCKING_RETRANS = "BR";
    private final String RESULT_BLOCKING_RETRANS_CONT = "BR_cont";
    private final String RESULT_BLOCKING_SYN = "BS";
    private final String RESULT_NORMAL = "NORMAL";
    private final int TCP_COLUMN_RETRANSMISSION = 7;
    private final int TCP_COLUMN_STATUS = 4;
    private final int TCP_COLUMN_TX_RX_QUEUE = 5;
    private final int TCP_COLUMN_UID = 8;
    private final int TCP_DETECTED_HISTORY_SIZE;
    private final int TCP_HISTORY_MAX_COUNT;
    private final int TCP_MONITOR_CHECK_MOBILE_DATA_ENABLED = 135701;
    private final long TCP_MONITOR_DETECT_INTERVAL;
    private final int TCP_MONITOR_FOREGROUND_ACTIVITY_DETECT = 135671;
    private final int TCP_MONITOR_FOREGROUND_ACTIVITY_START = 135669;
    private final int TCP_MONITOR_FOREGROUND_ACTIVITY_STOP = 135670;
    private final int TCP_MONITOR_QC_REQUEST = 135689;
    private final int TCP_MONITOR_QC_RESULT_TIMEOUT = 135691;
    private final int TCP_MONITOR_QC_RESULT_UPDATED = 135690;
    private final int TCP_MONITOR_RESET_TCP_TIMEOUT_VALUE = 135702;
    private final int TCP_MONITOR_RUN_SHELL_COMMAND_AGAIN = 135710;
    private final int TCP_MONITOR_SHELL_COMMAND_RESULT = 135709;
    private final int TCP_MONITOR_START_FIRST_DETECTION = 4;
    private final int TCP_MONITOR_START_SCREEN_ON = 2;
    private final int TCP_MONITOR_START_SETTINGS_CHANGED = 3;
    private final int TCP_MONITOR_START_WIFI_CONNECT = 1;
    private final int TCP_MONITOR_STOP_FIRST_DETECTION = 4;
    private final int TCP_MONITOR_STOP_SCREEN_OFF = 2;
    private final int TCP_MONITOR_STOP_SETTINGS_CHANGED = 3;
    private final int TCP_MONITOR_STOP_WIFI_DISCONNECT = 1;
    private final int TCP_MONITOR_SWITCH_INDIVIDUAL_APP_LIST_CHANGED = 135694;
    private final int TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_MOBILE_DATA = 135692;
    private final int TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_WIFI = 135693;
    private final int TCP_MONITOR_TURN_OFF_MOBILE_NETWORK = 135700;
    private final int TCP_MONITOR_TURN_ON_MOBILE_NETWORK = 135699;
    private final boolean TCP_MONITOR_UID_BLOCK_NETD_MODE;
    private final int TCP_MONITOR_VOIP_STATE_CHANGED = 135719;
    private final String TCP_STATUS_ESTABLISHED = "01";
    private final String TCP_STATUS_LAST_ACK = "09";
    private final String TCP_STATUS_SYN_SENT = "02";
    private final int THRESHOLD_ACCUMULATED_SUM_TX_RX = 2000;
    private final int THRESHOLD_CHAT_ESTABLISH = 5;
    private final int THRESHOLD_CHAT_SUM_TX_RX = 30;
    private final int THRESHOLD_DETECTION_IGNORED_AGGRESSIVE = 5;
    private final int THRESHOLD_DETECTION_IGNORED_NORMAL = 3;
    private final int THRESHOLD_DNS_BLOCK_COUNTER = 3;
    private final int THRESHOLD_GENERAL_ESTABLISH = 10;
    private final int THRESHOLD_GENERAL_SUM_TX_RX = 100;
    private final int THRESHOLD_LINK_SPEED_24G = 30;
    private final int THRESHOLD_LINK_SPEED_5G = 40;
    private final double THRESHOLD_LOSS = 0.1d;
    private final int THRESHOLD_RECEIVE_QUEUE_COUNTER = 2;
    private final int THRESHOLD_RETRANSMISSION_COUNTER = 5;
    private final int THRESHOLD_RETRANSMISSION_SOCKET_RATIO = 25;
    private final int THRESHOLD_RSSI_24GHz = -55;
    private final int THRESHOLD_RSSI_5GHz = -60;
    private final int THRESHOLD_SYN_BLOCK_AGGRESSIVE_MODE_DETECTION_COUNT = 5;
    private final int THRESHOLD_SYN_BLOCK_COUNT_DETECTED_APP = 2;
    private final int THRESHOLD_SYN_BLOCK_COUNT_HIGH = 5;
    private final int THRESHOLD_SYN_BLOCK_COUNT_LOW = 3;
    private final int THRESHOLD_SYN_SOCKET_RATIO = 50;
    private final int TIMEOUT_QC_REQEUST;
    private final int TIME_DELAY_VOIP_STATE_FINISHED;
    private final int TIME_RESET_TCP_TIMEOUT;
    private boolean isDnsCallbackRegistered;
    /* access modifiers changed from: private */
    public WifiApInfo mApInfo;
    private int mBeaconRateCount;
    private BroadcastReceiver mBroadcastReceiver;
    private ClientModeImpl mClientModeImpl;
    private ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public Context mContext;
    private int mCurrentQai;
    private HashMap<Integer, UID_STATUS> mCurrentUidBlockedList;
    private final Object mCurrentUidBlockedListLock;
    /* access modifiers changed from: private */
    public int mDetectionMode;
    private int mDnsBlockCounter;
    /* access modifiers changed from: private */
    public boolean mDnsDetected;
    private ArrayList<String> mDnsList;
    private ArrayList<String> mDumpHandlerMsg;
    private IIpConnectivityMetrics mIpConnectivityMetrics;
    private boolean mIs5GHz;
    /* access modifiers changed from: private */
    public boolean mIsForegroundActivityDetectionStarted;
    private boolean mIsInterfaceNameNotAvailable;
    private boolean mIsMobileDataEnabledByTcpMonitor;
    private boolean mIsQaiSwitchableMode;
    /* access modifiers changed from: private */
    public boolean mIsScreenOn;
    private boolean mIsVoipOngoing;
    /* access modifiers changed from: private */
    public boolean mIsWaitForQCResult;
    /* access modifiers changed from: private */
    public boolean mIsWifiConnected;
    /* access modifiers changed from: private */
    public boolean mIsWifiValidState;
    /* access modifiers changed from: private */
    public WifiTcpMonitorInfo mLastAutoSwitchNotifiedTcpMonitorInfo;
    private WifiTcpMonitorInfo mLastDetectedTcpMonitorInfo;
    /* access modifiers changed from: private */
    public WifiTcpMonitorInfo mLastSuggestionNotifiedTcpMonitorInfo;
    /* access modifiers changed from: private */
    public WifiTcpMonitorInfo mLastSwitchNotifiedTcpMonitorInfo;
    /* access modifiers changed from: private */
    public long mLastTcpMonitorTime;
    private ArrayList<Long> mLatestTxRx;
    private int mLinkRssiThreshold;
    private int mLinkSpeed;
    private int mLinkSpeedThreshold;
    private double mLoss;
    private ConnectivityManager.NetworkCallback mMobileNetworkCallback;
    private NetdCommandHandler mNetdCommandHandler;
    private final INetdEventCallback mNetdEventCallback;
    /* access modifiers changed from: private */
    public WifiTransportLayerNotificationManager mNotifier;
    private PackageManager mPackageManager;
    private String mPackageName;
    private boolean mPoorNetworkDetectionEnabled;
    private int mPrevRunningBeaconCount;
    private int mRSSI;
    private ArrayList<Integer> mRunningBeaconCountList;
    private int mSkipUid;
    private boolean mSkipUidNotified;
    private SnsBigDataManager mSnsBigDataManager;
    private SnsBigDataTCPE mSnsBigDataTCPE;
    /* access modifiers changed from: private */
    public String mSsid;
    /* access modifiers changed from: private */
    public boolean mSwitchForIndividualAppsEnabled;
    /* access modifiers changed from: private */
    public boolean mSwitchForIndividualAppsEverDetected;
    private int mSynBlockCountSum;
    private ArrayList<WifiTcpMonitorInfo> mTcpMonitorDetectedHistory;
    private ArrayList<WifiTcpMonitorInfo> mTcpMonitorInfoHistory;
    private TelephonyManager mTelephonyManager;
    private int mThresholdDetectionIgnored;
    private int mThresholdLatestSumTxRx;
    private int mThresholdNormalEstablish;
    /* access modifiers changed from: private */
    public int mUid;
    private long mUidAccumulatedTxRx;
    private long mUidChangedTime;
    private int mUidChatAppRetransmissionCounter;
    private ArrayList<Integer> mUidListWaitingForQcResult;
    private long mUidPrevRx;
    private int mUidPrevSynSize;
    private long mUidPrevTx;
    private int mUidReceieveQueueCounter;
    private int mUidRetransmissionCounter;
    private long mUidRxDiff;
    private ArrayList<Integer> mUidSynBlockCounter;
    private int mUidSynBlockNoEstablishCounter;
    private long mUidTxDiff;
    private long mUidTxRxOnResume;
    /* access modifiers changed from: private */
    public long mWifiConnectedTime;
    private WifiConnectivityMonitor mWifiConnectivityMonitor;
    /* access modifiers changed from: private */
    public Network mWifiNetwork;
    private ConnectivityManager.NetworkCallback mWifiNetworkCallback;
    private WifiPackageInfo mWifiPackageInfo;
    /* access modifiers changed from: private */
    public WifiTransportLayerMonitor mWifiTransportLayerMonitor;

    private enum UID_STATUS {
        NONE,
        DETECTED,
        ACTIVATED,
        REMOVED
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public WifiSwitchForIndividualAppsService(Looper looper, ClientModeImpl cmi, WifiTransportLayerMonitor wtm, WifiConnectivityMonitor wcm, Context context) {
        super(looper);
        Context context2 = context;
        boolean z = false;
        this.DETECTION_MODE_CHAT_ONLY = 0;
        this.DETECTION_MODE_FILTERED_APP = 1;
        this.DETECTION_MODE_ALL_APP_EXCEPT_SYSTEM = 2;
        this.mSkipUid = 0;
        this.mSkipUidNotified = false;
        this.mUid = -1;
        this.mPackageName = null;
        this.mRSSI = -200;
        this.mLinkSpeed = 0;
        this.mLinkSpeedThreshold = 20;
        this.mLinkRssiThreshold = -55;
        this.mLoss = 0.0d;
        this.mIs5GHz = false;
        this.mSsid = "";
        this.mUidPrevRx = 0;
        this.mUidPrevTx = 0;
        this.mUidTxDiff = 0;
        this.mUidRxDiff = 0;
        this.mUidTxRxOnResume = 0;
        this.mUidAccumulatedTxRx = 0;
        this.mPoorNetworkDetectionEnabled = false;
        this.mSwitchForIndividualAppsEnabled = false;
        this.mIsForegroundActivityDetectionStarted = false;
        this.mSwitchForIndividualAppsEverDetected = false;
        this.mIsQaiSwitchableMode = true;
        this.mCurrentQai = -1;
        this.mDetectionMode = 0;
        this.mIsScreenOn = true;
        this.mIsWifiConnected = false;
        this.mIsWifiValidState = false;
        this.mIsWaitForQCResult = false;
        this.mIsInterfaceNameNotAvailable = false;
        this.mIsMobileDataEnabledByTcpMonitor = false;
        this.mUidPrevSynSize = 0;
        this.mUidRetransmissionCounter = 0;
        this.mUidSynBlockNoEstablishCounter = 0;
        this.mSynBlockCountSum = 0;
        this.mUidChatAppRetransmissionCounter = 0;
        this.mUidReceieveQueueCounter = 0;
        this.mDnsBlockCounter = 0;
        this.mWifiConnectedTime = 0;
        this.mUidSynBlockCounter = new ArrayList<>();
        this.INVALID_UID = -1;
        this.TIMEOUT_QC_REQEUST = IWCEventManager.wifiOFFPending_MS;
        this.TIME_RESET_TCP_TIMEOUT = 15000;
        this.MOBILE_DATA_ENABLE_CHECK_MAX = 3;
        this.MOBILE_DATA_ENABLE_CHECK_DELAY = IWCEventManager.wifiOFFPending_MS;
        this.TCP_MONITOR_DETECT_INTERVAL = 1000;
        this.KEY_TCP_MONITOR_COMMAND = "TCPMONITOR";
        this.EXTRA_SHOW_SWITCH_FOR_INDIVIDUAL_APPS = "show_individual_apps";
        this.TCP_MONITOR_UID_BLOCK_NETD_MODE = true;
        this.mWifiNetworkCallback = null;
        this.mMobileNetworkCallback = null;
        this.LATEST_TX_RX_LENGTH = 3;
        this.mLatestTxRx = new ArrayList<>();
        this.mThresholdNormalEstablish = 10;
        this.mThresholdLatestSumTxRx = 100;
        this.mThresholdDetectionIgnored = 5;
        this.mCurrentUidBlockedListLock = new Object();
        this.mCurrentUidBlockedList = new HashMap<>();
        this.mUidListWaitingForQcResult = new ArrayList<>();
        this.mPrevRunningBeaconCount = 0;
        this.mBeaconRateCount = 0;
        this.BEACON_COUNT_RANGE = 40;
        this.BEACON_SAMPLE_THRESHOLD = 3;
        this.BEACON_AVERAGE_RATE_TRHESHOLD = 5;
        this.mRunningBeaconCountList = new ArrayList<>();
        this.mDnsList = new ArrayList<>();
        this.mIsVoipOngoing = false;
        this.TIME_DELAY_VOIP_STATE_FINISHED = IWCEventManager.wifiOFFPending_MS;
        this.mUidChangedTime = 0;
        this.mDumpHandlerMsg = new ArrayList<>();
        this.mTcpMonitorInfoHistory = new ArrayList<>();
        this.mTcpMonitorDetectedHistory = new ArrayList<>();
        this.mLastDetectedTcpMonitorInfo = null;
        this.mLastSuggestionNotifiedTcpMonitorInfo = null;
        this.mLastSwitchNotifiedTcpMonitorInfo = null;
        this.mLastAutoSwitchNotifiedTcpMonitorInfo = null;
        this.mLastTcpMonitorTime = 0;
        this.TCP_HISTORY_MAX_COUNT = ReportIdKey.ID_DHCP_FAIL;
        this.TCP_DETECTED_HISTORY_SIZE = 10;
        this.mDnsDetected = false;
        this.mNetdEventCallback = new BaseNetdEventCallback() {
            public void onDnsEvent(int netId, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
                String callingApp = WifiSwitchForIndividualAppsService.this.getPackageManager().getNameForUid(uid);
                if (WifiSwitchForIndividualAppsService.this.mUid == uid && WifiSwitchForIndividualAppsService.this.mIsForegroundActivityDetectionStarted) {
                    if (WifiSwitchForIndividualAppsService.DBG) {
                        Log.d(WifiSwitchForIndividualAppsService.TAG, "onDnsEvent - (Current UID)" + callingApp + "(" + uid + ")");
                    }
                    boolean unused = WifiSwitchForIndividualAppsService.this.mDnsDetected = true;
                }
            }
        };
        this.isDnsCallbackRegistered = false;
        this.mContext = context2;
        this.mWifiTransportLayerMonitor = wtm;
        this.mWifiConnectivityMonitor = wcm;
        this.mClientModeImpl = cmi;
        this.mNotifier = new WifiTransportLayerNotificationManager(context2);
        HandlerThread netdCommandHandlerThread = new HandlerThread("NetdCommandHandler");
        netdCommandHandlerThread.start();
        this.mNetdCommandHandler = new NetdCommandHandler(netdCommandHandlerThread.getLooper());
        this.mSwitchForIndividualAppsEverDetected = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_ever_detected", 0) == 1 ? true : z;
        if (DBG) {
            Log.d(TAG, "mSwitchForIndividualAppsEverDetected: " + this.mSwitchForIndividualAppsEverDetected);
        }
        setupBroadcastReceiver();
        registerWifiNetworkCallbacks();
        registerForSwitchForIndividualAppsChange();
        setAudioPlaybackCallback();
        this.mSnsBigDataManager = new SnsBigDataManager(this.mContext);
        this.mSnsBigDataTCPE = (SnsBigDataTCPE) this.mSnsBigDataManager.getBigDataFeature("TCPE");
    }

    private void reportNetworkConnectivity() {
        if (this.mIsWifiValidState && this.mWifiNetwork != null) {
            getConnectivityManager().reportNetworkConnectivity(this.mWifiNetwork, true);
            getConnectivityManager().reportNetworkConnectivity(this.mWifiNetwork, false);
            this.mIsWaitForQCResult = true;
        }
    }

    private void registerWifiNetworkCallbacks() {
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(1);
        this.mWifiNetworkCallback = new ConnectivityManager.NetworkCallback() {
            public void onAvailable(Network network) {
                if (WifiSwitchForIndividualAppsService.DBG) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "mWifiNetworkCallback onAvailable - " + network.toString());
                }
                boolean unused = WifiSwitchForIndividualAppsService.this.mIsWifiConnected = true;
                long unused2 = WifiSwitchForIndividualAppsService.this.mWifiConnectedTime = SystemClock.elapsedRealtime();
                WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService = WifiSwitchForIndividualAppsService.this;
                String unused3 = wifiSwitchForIndividualAppsService.mSsid = wifiSwitchForIndividualAppsService.getSsid();
                WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService2 = WifiSwitchForIndividualAppsService.this;
                WifiApInfo unused4 = wifiSwitchForIndividualAppsService2.mApInfo = wifiSwitchForIndividualAppsService2.mWifiTransportLayerMonitor.setSsid(WifiSwitchForIndividualAppsService.this.mSsid);
                if (WifiSwitchForIndividualAppsService.this.mApInfo != null) {
                    WifiSwitchForIndividualAppsService.this.mApInfo.setAccumulatedConnectionCount(WifiSwitchForIndividualAppsService.this.mApInfo.getAccumulatedConnectionCount() + 1);
                    WifiSwitchForIndividualAppsService.this.mWifiTransportLayerMonitor.updateWifiApInfo(WifiSwitchForIndividualAppsService.this.mApInfo);
                }
                Network unused5 = WifiSwitchForIndividualAppsService.this.mWifiNetwork = network;
                super.onAvailable(network);
            }

            public void onLost(Network network) {
                if (WifiSwitchForIndividualAppsService.DBG) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "mWifiNetworkCallback onLost - " + network.toString());
                }
                boolean unused = WifiSwitchForIndividualAppsService.this.mIsWifiConnected = false;
                int duration = ((int) (SystemClock.elapsedRealtime() - WifiSwitchForIndividualAppsService.this.mWifiConnectedTime)) / 60000;
                if (WifiSwitchForIndividualAppsService.this.mApInfo != null) {
                    WifiSwitchForIndividualAppsService.this.mApInfo.setAccumulatedConnectionTime(WifiSwitchForIndividualAppsService.this.mApInfo.getAccumulatedConnectionTime() + duration);
                    WifiSwitchForIndividualAppsService.this.mWifiTransportLayerMonitor.updateWifiApInfo(WifiSwitchForIndividualAppsService.this.mApInfo);
                }
                boolean unused2 = WifiSwitchForIndividualAppsService.this.mIsWifiValidState = false;
                Network unused3 = WifiSwitchForIndividualAppsService.this.mWifiNetwork = null;
                WifiSwitchForIndividualAppsService.this.stopTCPMonitoring(1);
                super.onLost(network);
            }

            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (WifiSwitchForIndividualAppsService.this.mIsWifiConnected) {
                    if (WifiSwitchForIndividualAppsService.DBG) {
                        Log.i(WifiSwitchForIndividualAppsService.TAG, "mWifiNetworkCallback onCapabilitiesChanged - " + network.toString() + networkCapabilities.toString());
                    }
                    int reason = 1;
                    if (networkCapabilities.hasCapability(16)) {
                        if (WifiSwitchForIndividualAppsService.DBG) {
                            Log.d(WifiSwitchForIndividualAppsService.TAG, "mWifiNetworkCallback onCapabilitiesChanged - NET_CAPABILITY_VALIDATED");
                        }
                        boolean unused = WifiSwitchForIndividualAppsService.this.mIsWifiValidState = true;
                        WifiSwitchForIndividualAppsService.this.updateDnsInfo();
                        if (WifiSwitchForIndividualAppsService.this.isSwitchForIndividualAppsEnabled()) {
                            if (!WifiSwitchForIndividualAppsService.this.mSwitchForIndividualAppsEverDetected) {
                                reason = 4;
                            }
                            WifiSwitchForIndividualAppsService.this.startTCPMonitoring(reason);
                        }
                    } else {
                        boolean unused2 = WifiSwitchForIndividualAppsService.this.mIsWifiValidState = false;
                        if (WifiSwitchForIndividualAppsService.this.mIsForegroundActivityDetectionStarted) {
                            WifiSwitchForIndividualAppsService.this.stopTCPMonitoring(1);
                        }
                    }
                    if (WifiSwitchForIndividualAppsService.this.mIsWaitForQCResult) {
                        boolean unused3 = WifiSwitchForIndividualAppsService.this.mIsWaitForQCResult = false;
                        WifiSwitchForIndividualAppsService.this.sendEmptyMessage(135690);
                    }
                }
                super.onCapabilitiesChanged(network, networkCapabilities);
            }
        };
        getConnectivityManager().registerNetworkCallback(req.build(), this.mWifiNetworkCallback);
    }

    private void turnOnMobileData() {
        Log.d(TAG, "turnOnMobileData");
        NetworkRequest request = new NetworkRequest.Builder().addTransportType(0).addCapability(12).build();
        this.mMobileNetworkCallback = new NetworkRequestCallback();
        getConnectivityManager().requestNetwork(request, this.mMobileNetworkCallback);
        this.mIsMobileDataEnabledByTcpMonitor = true;
    }

    private void turnOffMobileData() {
        Log.d(TAG, "turnOffMobileData");
        try {
            if (this.mMobileNetworkCallback != null) {
                getConnectivityManager().unregisterNetworkCallback(this.mMobileNetworkCallback);
                this.mMobileNetworkCallback = null;
                this.mIsMobileDataEnabledByTcpMonitor = false;
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "turnOffMobileData - IllegalArgumentException " + e);
        } catch (Exception e2) {
            Log.e(TAG, "turnOffMobileData - Exception " + e2);
            e2.printStackTrace();
        }
    }

    private class NetworkRequestCallback extends ConnectivityManager.NetworkCallback {
        private NetworkRequestCallback() {
        }

        public void onAvailable(Network network) {
            Log.d(WifiSwitchForIndividualAppsService.TAG, "mMobileNetworkCallback onAvailable - " + network.toString());
        }

        public void onLost(Network network) {
            Log.d(WifiSwitchForIndividualAppsService.TAG, "mMobileNetworkCallback onLost - =" + network.toString());
        }
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager;
    }

    /* access modifiers changed from: private */
    public boolean isSwitchForIndividualAppsEnabled() {
        return this.mPoorNetworkDetectionEnabled && this.mIsWifiValidState && this.mIsScreenOn && (this.mSwitchForIndividualAppsEnabled || !this.mSwitchForIndividualAppsEverDetected) && this.mIsQaiSwitchableMode;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:2:0x0005, code lost:
        r0 = r3.mCurrentQai;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isAggressiveMode() {
        /*
            r3 = this;
            boolean r0 = r3.mPoorNetworkDetectionEnabled
            r1 = 1
            if (r0 == 0) goto L_0x000d
            int r0 = r3.mCurrentQai
            r2 = -1
            if (r0 == r2) goto L_0x000c
            if (r0 != r1) goto L_0x000d
        L_0x000c:
            goto L_0x000e
        L_0x000d:
            r1 = 0
        L_0x000e:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.isAggressiveMode():boolean");
    }

    private void registerForSwitchForIndividualAppsChange() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_switch_for_individual_apps_enabled"), false, new ContentObserver(this) {
            public void onChange(boolean selfChange) {
                WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService = WifiSwitchForIndividualAppsService.this;
                boolean z = false;
                if (Settings.Global.getInt(wifiSwitchForIndividualAppsService.mContext.getContentResolver(), "wifi_switch_for_individual_apps_enabled", 0) == 1) {
                    z = true;
                }
                boolean unused = wifiSwitchForIndividualAppsService.mSwitchForIndividualAppsEnabled = z;
                Log.d(WifiSwitchForIndividualAppsService.TAG, "SwitchForIndividualApps changed - " + WifiSwitchForIndividualAppsService.this.mSwitchForIndividualAppsEnabled);
                if (!WifiSwitchForIndividualAppsService.this.mSwitchForIndividualAppsEverDetected) {
                    boolean unused2 = WifiSwitchForIndividualAppsService.this.mSwitchForIndividualAppsEverDetected = true;
                    Settings.Global.putInt(WifiSwitchForIndividualAppsService.this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_ever_detected", 1);
                }
                if (WifiSwitchForIndividualAppsService.this.isSwitchForIndividualAppsEnabled()) {
                    WifiSwitchForIndividualAppsService.this.startTCPMonitoring(3);
                } else {
                    WifiSwitchForIndividualAppsService.this.stopTCPMonitoring(3);
                }
            }
        });
        boolean z = true;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_enabled", 0) != 1) {
            z = false;
        }
        this.mSwitchForIndividualAppsEnabled = z;
        Log.d(TAG, "SwitchForIndividualApps - " + this.mSwitchForIndividualAppsEnabled);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_switch_for_individual_apps_detection_mode"), false, new ContentObserver(this) {
            public void onChange(boolean selfChange) {
                WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService = WifiSwitchForIndividualAppsService.this;
                int unused = wifiSwitchForIndividualAppsService.mDetectionMode = Settings.Global.getInt(wifiSwitchForIndividualAppsService.mContext.getContentResolver(), "wifi_switch_for_individual_apps_detection_mode", 0);
                Log.d(WifiSwitchForIndividualAppsService.TAG, "mDetectionMode changed - " + WifiSwitchForIndividualAppsService.this.mDetectionMode);
            }
        });
        this.mDetectionMode = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_detection_mode", 0);
        if (DBG) {
            Log.d(TAG, "mDetectionMode: " + this.mDetectionMode);
        }
    }

    private void setupBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("com.samsung.android.net.wifi.TCP_MONITOR_ACTION_USE_MOBILE_DATA")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "WIFI_TCP_MONITOR_ACTION_USE_MOBILE_DATA recieved");
                    if (WifiSwitchForIndividualAppsService.this.mLastSwitchNotifiedTcpMonitorInfo != null) {
                        WifiSwitchForIndividualAppsService.this.mLastSwitchNotifiedTcpMonitorInfo.actionResult = 5;
                        WifiSwitchForIndividualAppsService.this.mLastSwitchNotifiedTcpMonitorInfo.actionDuration = ((int) (SystemClock.elapsedRealtime() - WifiSwitchForIndividualAppsService.this.mLastTcpMonitorTime)) / 1000;
                        WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService = WifiSwitchForIndividualAppsService.this;
                        wifiSwitchForIndividualAppsService.sendBigDataFeatureForTCPE(wifiSwitchForIndividualAppsService.mLastSwitchNotifiedTcpMonitorInfo, false);
                    }
                    WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService2 = WifiSwitchForIndividualAppsService.this;
                    wifiSwitchForIndividualAppsService2.sendMessage(wifiSwitchForIndividualAppsService2.obtainMessage(135692, intent.getIntExtra("uid", 0), 0, intent.getStringExtra("packageName")));
                    WifiSwitchForIndividualAppsService.this.mNotifier.removeNotification(2);
                    WifiSwitchForIndividualAppsService.this.mWifiTransportLayerMonitor.resetSwitchForIndivdiaulAppsDetectionCount(intent.getStringExtra("packageName"));
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_ACTION_SETTINGS")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "WIFI_TCP_MONITOR_ACTION_SETTINGS recieved");
                    if (WifiSwitchForIndividualAppsService.this.mLastAutoSwitchNotifiedTcpMonitorInfo != null) {
                        WifiSwitchForIndividualAppsService.this.mLastAutoSwitchNotifiedTcpMonitorInfo.actionResult = 4;
                        WifiSwitchForIndividualAppsService.this.mLastAutoSwitchNotifiedTcpMonitorInfo.actionDuration = ((int) (SystemClock.elapsedRealtime() - WifiSwitchForIndividualAppsService.this.mLastTcpMonitorTime)) / 1000;
                        WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService3 = WifiSwitchForIndividualAppsService.this;
                        wifiSwitchForIndividualAppsService3.sendBigDataFeatureForTCPE(wifiSwitchForIndividualAppsService3.mLastAutoSwitchNotifiedTcpMonitorInfo, false);
                    }
                    WifiSwitchForIndividualAppsService.this.mNotifier.removeNotification(1);
                    WifiSwitchForIndividualAppsService.this.mNotifier.removeNotification(3);
                    WifiSwitchForIndividualAppsService.this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                    try {
                        Intent intentSettings = new Intent();
                        intentSettings.setClassName("com.android.settings", "com.android.settings.Settings$SwitchForIndividualAppsSettingsActivity");
                        intentSettings.setFlags(276824064);
                        intentSettings.putExtra("show_individual_apps", true);
                        intentSettings.putExtra("UID", intent.getIntExtra("UID", 0));
                        WifiSwitchForIndividualAppsService.this.mContext.startActivity(intentSettings);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_DELETE_NOTIFICATION")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "WIFI_TCP_MONITOR_DELETE_NOTIFICATION recieved");
                    int reason = intent.getIntExtra("reason", 0);
                    WifiTcpMonitorInfo info = null;
                    if (reason == 2 && WifiSwitchForIndividualAppsService.this.mLastSwitchNotifiedTcpMonitorInfo != null) {
                        info = WifiSwitchForIndividualAppsService.this.mLastSwitchNotifiedTcpMonitorInfo;
                    } else if (reason == 3 && WifiSwitchForIndividualAppsService.this.mLastAutoSwitchNotifiedTcpMonitorInfo != null) {
                        info = WifiSwitchForIndividualAppsService.this.mLastAutoSwitchNotifiedTcpMonitorInfo;
                    } else if (reason == 1 && WifiSwitchForIndividualAppsService.this.mLastSuggestionNotifiedTcpMonitorInfo != null) {
                        info = WifiSwitchForIndividualAppsService.this.mLastSuggestionNotifiedTcpMonitorInfo;
                    }
                    if (info != null) {
                        info.actionResult = reason;
                        info.actionDuration = ((int) (SystemClock.elapsedRealtime() - WifiSwitchForIndividualAppsService.this.mLastTcpMonitorTime)) / 1000;
                        WifiSwitchForIndividualAppsService.this.sendBigDataFeatureForTCPE(info, false);
                    }
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_SWITCHABLE_APP_LIST_CHANGED")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "WIFI_TCP_MONITOR_SWITCHABLE_APP_LIST_CHANGED recieved");
                    WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService4 = WifiSwitchForIndividualAppsService.this;
                    wifiSwitchForIndividualAppsService4.sendMessage(wifiSwitchForIndividualAppsService4.obtainMessage(135694, (ArrayList) intent.getSerializableExtra("UID_LIST")));
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "ACTION_SCREEN_ON recieved");
                    boolean unused = WifiSwitchForIndividualAppsService.this.mIsScreenOn = true;
                    if (WifiSwitchForIndividualAppsService.this.isSwitchForIndividualAppsEnabled()) {
                        WifiSwitchForIndividualAppsService.this.startTCPMonitoring(2);
                    }
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "ACTION_SCREEN_OFF recieved");
                    boolean unused2 = WifiSwitchForIndividualAppsService.this.mIsScreenOn = false;
                    if (WifiSwitchForIndividualAppsService.this.mIsForegroundActivityDetectionStarted) {
                        WifiSwitchForIndividualAppsService.this.stopTCPMonitoring(2);
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.samsung.android.net.wifi.TCP_MONITOR_ACTION_USE_MOBILE_DATA");
        intentFilter.addAction("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_ACTION_SETTINGS");
        intentFilter.addAction("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_DELETE_NOTIFICATION");
        intentFilter.addAction("com.samsung.android.net.wifi.WIFI_TCP_MONITOR_SWITCHABLE_APP_LIST_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    private boolean isVoipOngoing() {
        return this.mIsVoipOngoing;
    }

    /* access modifiers changed from: private */
    public void setVoipOngoing(boolean isOngoing) {
        if (isOngoing) {
            removeMessages(135719);
            sendMessage(obtainMessage(135719, 1, 0));
        } else if (!hasMessages(135719)) {
            sendMessageDelayed(obtainMessage(135719, 0, 0), 3000);
        }
    }

    private void setAudioPlaybackCallback() {
        if (DBG) {
            Log.d(TAG, "setAudioPlaybackCallback");
        }
        ((AudioManager) this.mContext.getSystemService("audio")).registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                if (configs != null) {
                    boolean isOngoing = false;
                    Iterator<AudioPlaybackConfiguration> it = configs.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        AudioPlaybackConfiguration config = it.next();
                        if (config.getAudioAttributes().getUsage() == 2 && config.isActive()) {
                            isOngoing = true;
                            break;
                        }
                    }
                    if (WifiSwitchForIndividualAppsService.DBG) {
                        Log.d(WifiSwitchForIndividualAppsService.TAG, "onPlaybackConfigChanged - isOngoing " + isOngoing);
                    }
                    WifiSwitchForIndividualAppsService.this.setVoipOngoing(isOngoing);
                }
                super.onPlaybackConfigChanged(configs);
            }
        }, this);
    }

    public void handleMessage(Message msg) {
        if (!(msg.what == 135671 || msg.what == 135719)) {
            writeHandlerMsg(msg);
        }
        int i = msg.what;
        boolean z = false;
        if (i != 135719) {
            switch (i) {
                case 135669:
                    if (DBG) {
                        Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_START - ");
                    }
                    this.mIsForegroundActivityDetectionStarted = true;
                    registerDnsCallback();
                    if (msg.arg1 == 2) {
                        if (DBG) {
                            Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_START - update start time");
                        }
                        this.mUidChangedTime = SystemClock.elapsedRealtime();
                    }
                    removeMessages(135671);
                    sendEmptyMessageDelayed(135671, 1000);
                    break;
                case 135670:
                    if (DBG) {
                        Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_STOP - " + msg.arg1);
                    }
                    this.mIsForegroundActivityDetectionStarted = false;
                    if (msg.arg1 == 1 || msg.arg1 == 3 || msg.arg1 == 4) {
                        clearCurrentUidBlockedList();
                    }
                    unregisterDnsCallback();
                    removeMessages(135671);
                    if (this.mUid != this.mSkipUid) {
                        if (DBG) {
                            Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_STOP - update running time");
                        }
                        updateSwitchedPackageInfo(this.mSsid, this.mPackageName);
                        break;
                    }
                    break;
                case 135671:
                    if (!this.mIsForegroundActivityDetectionStarted) {
                        if (DBG) {
                            Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_DETECT - STOP");
                            break;
                        }
                    } else {
                        WifiPackageInfo info = this.mWifiTransportLayerMonitor.getCurrentPackageInfo();
                        if (info == null) {
                            if (DBG) {
                                Log.w(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_DETECT - NULL");
                            }
                        } else if (this.mSkipUid == info.getUid()) {
                            if (DBG && !this.mSkipUidNotified) {
                                Log.d(TAG, "TCP_MONITOR_FOREGROUND_ACTIVITY_DETECT - SKIP");
                            }
                            this.mSkipUidNotified = true;
                        } else {
                            this.mSkipUidNotified = false;
                            if (this.mUid != info.getUid()) {
                                if (this.mUid != this.mSkipUid) {
                                    updateSwitchedPackageInfo(this.mSsid, this.mPackageName);
                                }
                                this.mWifiPackageInfo = info;
                                this.mUid = info.getUid();
                                this.mPackageName = info.getPackageName();
                                resetUidBaseHistory(info.isChatApp());
                            }
                            updateLinkStatistics();
                            if (!isPackageException(this.mWifiPackageInfo) && !isSkipNetworkCondition(this.mUid, this.mPackageName)) {
                                runForegroundUidBlockingCheck();
                            }
                        }
                        sendEmptyMessageDelayed(135671, 1000);
                        break;
                    }
                    break;
                default:
                    switch (i) {
                        case SWITCH_TO_MOBILE_DATA_ENABLED /*135678*/:
                            if (DBG) {
                                Log.d(TAG, "SWITCH_TO_MOBILE_DATA_ENABLED");
                            }
                            this.mPoorNetworkDetectionEnabled = true;
                            if (isSwitchForIndividualAppsEnabled()) {
                                startTCPMonitoring(3);
                                break;
                            }
                            break;
                        case SWITCH_TO_MOBILE_DATA_DISABLED /*135679*/:
                            if (DBG) {
                                Log.d(TAG, "SWITCH_TO_MOBILE_DATA_DISABLED");
                            }
                            this.mPoorNetworkDetectionEnabled = false;
                            stopTCPMonitoring(3);
                            break;
                        case SWITCH_TO_MOBILE_DATA_QAI /*135680*/:
                            if (DBG) {
                                Log.d(TAG, "SWITCH_TO_MOBILE_DATA_QAI " + msg.arg1);
                            }
                            if (msg.arg1 == -1 || msg.arg1 == 1 || msg.arg1 == 2) {
                                z = true;
                            }
                            this.mIsQaiSwitchableMode = z;
                            this.mCurrentQai = msg.arg1;
                            break;
                        default:
                            switch (i) {
                                case 135689:
                                    if (DBG) {
                                        Log.d(TAG, "TCP_MONITOR_QC_REQUEST");
                                    }
                                    reportNetworkConnectivity();
                                    sendEmptyMessageDelayed(135691, 3000);
                                    break;
                                case 135690:
                                    if (DBG) {
                                        Log.d(TAG, "TCP_MONITOR_QC_RESULT_UPDATED");
                                    }
                                    removeMessages(135691);
                                    updateQcResult(this.mIsWifiValidState);
                                    break;
                                case 135691:
                                    if (DBG != 0) {
                                        Log.d(TAG, "TCP_MONITOR_QC_RESULT_TIMEOUT");
                                    }
                                    if (this.mIsWaitForQCResult) {
                                        this.mIsWaitForQCResult = false;
                                    }
                                    updateQcResult(this.mIsWifiValidState);
                                    break;
                                case 135692:
                                    if (DBG != 0) {
                                        Log.d(TAG, "TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_MOBILE_DATA");
                                    }
                                    int uidAdd = msg.arg1;
                                    addCurrentUidBlockedList(uidAdd);
                                    updateCurrentUidBlockedListStatus(uidAdd, UID_STATUS.ACTIVATED);
                                    useMobileDataForBlockedApp(uidAdd);
                                    this.mLastAutoSwitchNotifiedTcpMonitorInfo = this.mLastDetectedTcpMonitorInfo;
                                    this.mNotifier.showNotification(3, uidAdd, (String) msg.obj);
                                    this.mWifiTransportLayerMonitor.enableSwitchEnabledAppInfo(uidAdd);
                                    break;
                                case 135693:
                                    if (DBG) {
                                        Log.d(TAG, "TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_WIFI");
                                    }
                                    int uidRemove = msg.arg1;
                                    updateCurrentUidBlockedListStatus(uidRemove, UID_STATUS.REMOVED);
                                    useDefaultNetworkForBlockedApp(uidRemove);
                                    this.mNotifier.removeNotification(3);
                                    break;
                                case 135694:
                                    if (DBG) {
                                        Log.d(TAG, "TCP_MONITOR_SWITCH_INDIVIDUAL_APP_LIST_CHANGED");
                                    }
                                    ArrayList<Integer> list = (ArrayList) msg.obj;
                                    if (list != null) {
                                        this.mWifiTransportLayerMonitor.updateSwitchEnabledAppList(list);
                                        if (!this.mCurrentUidBlockedList.isEmpty()) {
                                            try {
                                                ArrayList<Integer> uidList = new ArrayList<>();
                                                uidList.addAll(this.mCurrentUidBlockedList.keySet());
                                                Iterator<Integer> it = uidList.iterator();
                                                while (it.hasNext()) {
                                                    int uid = it.next().intValue();
                                                    if (!list.contains(Integer.valueOf(uid))) {
                                                        sendMessage(obtainMessage(135693, uid, 0));
                                                    } else if (getCurrentUidBlockedListStatus(uid) == UID_STATUS.REMOVED) {
                                                        removeCurrentUidblockedList(uid);
                                                    }
                                                }
                                                break;
                                            } catch (Exception e) {
                                                if (DBG) {
                                                    Log.d(TAG, "TCP_MONITOR_SWITCH_INDIVIDUAL_APP_LIST_CHANGED - Exception: " + e);
                                                }
                                                e.printStackTrace();
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case 135699:
                                            if (DBG) {
                                                Log.d(TAG, "TCP_MONITOR_TURN_ON_MOBILE_NETWORK");
                                            }
                                            turnOnMobileData();
                                            break;
                                        case 135700:
                                            if (DBG != 0) {
                                                Log.d(TAG, "TCP_MONITOR_TURN_OFF_MOBILE_NETWORK");
                                            }
                                            turnOffMobileData();
                                            break;
                                        case 135701:
                                            if (DBG) {
                                                Log.d(TAG, "TCP_CHECK_MOBILE_DATA_ENABLED: " + msg.arg1 + ", " + msg.arg2);
                                            }
                                            int packageUid = msg.arg1;
                                            String packageName = (String) msg.obj;
                                            if (!isMobileDataConnected()) {
                                                int attemps = msg.arg2;
                                                if (attemps < 3) {
                                                    sendMessageDelayed(obtainMessage(135701, packageUid, attemps + 1, packageName), 3000);
                                                    break;
                                                }
                                            } else {
                                                this.mIsInterfaceNameNotAvailable = false;
                                                changeRouteToMobileNetwork(packageUid);
                                                break;
                                            }
                                            break;
                                        case 135702:
                                            if (DBG) {
                                                Log.d(TAG, "TCP_RESET_TCP_TIMEOUT_VALUE");
                                            }
                                            resetTcpTimeOut();
                                            break;
                                    }
                            }
                    }
            }
        } else {
            if (this.mIsVoipOngoing != (msg.arg1 == 1)) {
                if (DBG) {
                    Log.d(TAG, "TCP_MONITOR_VOIP_STATE_CHANGED " + msg.arg1);
                }
                writeHandlerMsg(msg);
                if (msg.arg1 == 1) {
                    z = true;
                }
                this.mIsVoipOngoing = z;
            }
        }
        super.handleMessage(msg);
    }

    /* access modifiers changed from: private */
    public void startTCPMonitoring(int reason) {
        if (!this.mIsForegroundActivityDetectionStarted) {
            if (DBG) {
                Log.d(TAG, "startTCPMonitoring - " + reason);
            }
            sendMessage(obtainMessage(135669, reason, 0));
        } else if (DBG) {
            Log.d(TAG, "startTCPMonitoring : already started");
        }
    }

    /* access modifiers changed from: private */
    public void stopTCPMonitoring(int reason) {
        if (this.mIsForegroundActivityDetectionStarted) {
            if (DBG) {
                Log.d(TAG, "stopTCPMonitoring - " + reason);
            }
            sendMessage(obtainMessage(135670, reason, 0));
            if (reason == 1 || reason == 3) {
                useDefaultNetworkForAllApp();
            }
        } else if ((reason == 1 || reason == 3) && this.mIsMobileDataEnabledByTcpMonitor) {
            if (DBG) {
                Log.d(TAG, "stopTCPMonitoring while pause state");
            }
            useDefaultNetworkForAllApp();
        } else {
            Log.d(TAG, "stopTCPMonitoring : already stopped");
        }
    }

    /* access modifiers changed from: private */
    public PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    private void updateSwitchedPackageInfo(String ssid, String packageName) {
        if (this.mApInfo != null) {
            int runningTime = (int) ((SystemClock.elapsedRealtime() - this.mUidChangedTime) / 60000);
            if (DBG) {
                Log.d(TAG, "updateDetectedPackageInfo - " + runningTime);
            }
            if (runningTime <= 0) {
                return;
            }
            if (this.mApInfo.getDetectedPackageList().get(packageName) != null) {
                this.mApInfo.updateNormalOperationTime(packageName, runningTime);
                this.mWifiTransportLayerMonitor.updateWifiApInfo(this.mApInfo);
                return;
            }
            if (DBG) {
                Log.d(TAG, "updateDetectedPackageInfo - create new DetectedPackage info");
            }
            this.mApInfo.addDetectedPakcageInfo(this.mPackageName, runningTime);
            this.mWifiTransportLayerMonitor.updateWifiApInfo(this.mApInfo);
        }
    }

    private void resetUidBaseHistory(boolean isChatApp) {
        this.mUidPrevRx = TrafficStats.getUidRxPackets(this.mUid);
        this.mUidPrevTx = TrafficStats.getUidTxPackets(this.mUid);
        this.mUidTxRxOnResume = this.mUidPrevRx + this.mUidPrevTx;
        this.mUidAccumulatedTxRx = 0;
        this.mUidRetransmissionCounter = 0;
        this.mUidChatAppRetransmissionCounter = 0;
        this.mUidSynBlockNoEstablishCounter = 0;
        this.mUidReceieveQueueCounter = 0;
        this.mDnsBlockCounter = 0;
        this.mUidSynBlockCounter.clear();
        this.mUidPrevSynSize = 0;
        if (isChatApp) {
            this.mThresholdLatestSumTxRx = 30;
            this.mThresholdNormalEstablish = 5;
        } else {
            this.mThresholdLatestSumTxRx = 100;
            this.mThresholdNormalEstablish = 10;
        }
        this.mUidChangedTime = SystemClock.elapsedRealtime();
        resetDnsDetected();
    }

    private void runForegroundUidBlockingCheck() {
        WifiTcpMonitorInfo info = new WifiTcpMonitorInfo(this.mUid, this.mPackageName);
        info.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date()).toString();
        info.result = "NORMAL";
        updateRxTx(info.uid);
        if (loadTcpInfo(info)) {
            addTcpMonitorInfoList(info);
            if (!isSkipTcpMonitorInfo(info) && isBlockingApp(info)) {
                this.mLastDetectedTcpMonitorInfo = info;
                sendBigDataFeatureForTCPE(info, true);
                updateTcpDetectedHistory();
                this.mWifiTransportLayerMonitor.addWifiPackageDetectedCount(info.uid);
                WifiApInfo wifiApInfo = this.mApInfo;
                if (wifiApInfo != null) {
                    wifiApInfo.addSwitchForIndivdiaulAppsDetectionCount(this.mPackageName);
                    this.mWifiTransportLayerMonitor.updateWifiApInfo(this.mApInfo);
                }
                if (uidBlockedAppDetected(this.mUid, this.mPackageName)) {
                    addCurrentUidBlockedList(this.mUid);
                }
                this.mSkipUid = this.mUid;
            }
            this.mUidPrevSynSize = info.syn;
            if (DBG || !"NORMAL".equals(info.result)) {
                Log.d(TAG, info.toString());
            }
        } else if (DBG) {
            Log.d(TAG, "Failed to load TCP data");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:125:0x02c9 A[Catch:{ all -> 0x0314 }] */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x02e3 A[SYNTHETIC, Splitter:B:129:0x02e3] */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x02eb A[Catch:{ IOException -> 0x02e7 }] */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x02f0 A[Catch:{ IOException -> 0x02e7 }] */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x02f5 A[Catch:{ IOException -> 0x02e7 }] */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x031a A[SYNTHETIC, Splitter:B:147:0x031a] */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x0322 A[Catch:{ IOException -> 0x031e }] */
    /* JADX WARNING: Removed duplicated region for block: B:154:0x0327 A[Catch:{ IOException -> 0x031e }] */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x032c A[Catch:{ IOException -> 0x031e }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean loadTcpInfo(com.android.server.wifi.tcp.WifiTcpMonitorInfo r32) {
        /*
            r31 = this;
            r1 = r31
            r2 = r32
            java.lang.String r3 = "Exception: "
            java.lang.String r4 = "WifiSwitchForIndividualAppsService"
            r5 = 0
            r6 = 0
            r7 = 0
            r8 = 0
            java.lang.String r9 = ""
            r10 = 0
            java.io.FileReader r0 = new java.io.FileReader     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            java.lang.String r11 = "/proc/net/tcp"
            r0.<init>(r11)     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r5 = r0
            java.io.FileReader r0 = new java.io.FileReader     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            java.lang.String r11 = "/proc/net/tcp6"
            r0.<init>(r11)     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r6 = r0
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r0.<init>(r5)     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r7 = r0
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r0.<init>(r6)     // Catch:{ Exception -> 0x02c0, all -> 0x02b7 }
            r8 = r0
            java.lang.String r0 = ""
            r11 = 0
        L_0x002e:
            java.lang.String r12 = r7.readLine()     // Catch:{ Exception -> 0x02af, all -> 0x02a5 }
            r0 = r12
            java.lang.String r13 = ":"
            java.lang.String r15 = "09"
            r16 = 7
            java.lang.String r14 = "02"
            r18 = r9
            java.lang.String r9 = " +"
            r19 = r10
            java.lang.String r10 = "\n"
            r20 = 0
            r22 = r4
            java.lang.String r4 = "01"
            r24 = 8
            r25 = 4
            r26 = r3
            r3 = 1
            if (r12 == 0) goto L_0x0147
            java.lang.StringBuilder r12 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r12.<init>()     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r12.append(r0)     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r12.append(r10)     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            java.lang.String r10 = r12.toString()     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            int r11 = r11 + 1
            if (r11 <= r3) goto L_0x012d
            java.lang.String[] r9 = r10.split(r9)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = r9[r25]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r12 = r12.equals(r4)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r12 == 0) goto L_0x008a
            int r12 = r2.uid     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String r12 = java.lang.Integer.toString(r12)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r3 = r9[r24]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r12.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x008a
            int r3 = r2.establishIPv4     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 1
            int r3 = r3 + r12
            r2.establishIPv4 = r3     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            int r3 = r2.establishAll     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            int r3 = r3 + r12
            r2.establishAll = r3     // Catch:{ Exception -> 0x0125, all -> 0x011b }
        L_0x008a:
            r3 = r9[r25]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equals(r14)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00a6
            int r3 = r2.uid     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String r3 = java.lang.Integer.toString(r3)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = r9[r24]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equalsIgnoreCase(r12)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00a6
            int r3 = r2.syn     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 1
            int r3 = r3 + r12
            r2.syn = r3     // Catch:{ Exception -> 0x0125, all -> 0x011b }
        L_0x00a6:
            r3 = r9[r25]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equals(r4)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00ce
            r3 = r9[r16]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 16
            long r27 = java.lang.Long.parseLong(r3, r12)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            int r3 = (r27 > r20 ? 1 : (r27 == r20 ? 0 : -1))
            if (r3 <= 0) goto L_0x00ce
            int r3 = r2.uid     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String r3 = java.lang.Integer.toString(r3)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = r9[r24]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equalsIgnoreCase(r12)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00ce
            int r3 = r2.retransmission     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 1
            int r3 = r3 + r12
            r2.retransmission = r3     // Catch:{ Exception -> 0x0125, all -> 0x011b }
        L_0x00ce:
            r3 = r9[r25]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equals(r15)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00ea
            int r3 = r2.uid     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String r3 = java.lang.Integer.toString(r3)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = r9[r24]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equalsIgnoreCase(r12)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x00ea
            int r3 = r2.laskAck     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 1
            int r3 = r3 + r12
            r2.laskAck = r3     // Catch:{ Exception -> 0x0125, all -> 0x011b }
        L_0x00ea:
            r3 = r9[r25]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equals(r4)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x011a
            int r3 = r2.uid     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String r3 = java.lang.Integer.toString(r3)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r4 = r9[r24]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            boolean r3 = r3.equalsIgnoreCase(r4)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            if (r3 == 0) goto L_0x011a
            r3 = 5
            r3 = r9[r3]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            java.lang.String[] r3 = r3.split(r13)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r4 = 1
            r12 = r3[r4]     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r4 = 16
            long r12 = java.lang.Long.parseLong(r12, r4)     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            int r4 = (r12 > r20 ? 1 : (r12 == r20 ? 0 : -1))
            if (r4 <= 0) goto L_0x011a
            int r4 = r2.receivingQueue     // Catch:{ Exception -> 0x0125, all -> 0x011b }
            r12 = 1
            int r4 = r4 + r12
            r2.receivingQueue = r4     // Catch:{ Exception -> 0x0125, all -> 0x011b }
        L_0x011a:
            goto L_0x012d
        L_0x011b:
            r0 = move-exception
            r3 = r0
            r18 = r10
            r11 = r22
            r10 = r26
            goto L_0x0318
        L_0x0125:
            r0 = move-exception
            r9 = r10
            r11 = r22
            r10 = r26
            goto L_0x02c5
        L_0x012d:
            r9 = r10
            r10 = r19
            r4 = r22
            r3 = r26
            goto L_0x002e
        L_0x0136:
            r0 = move-exception
            r3 = r0
            r11 = r22
            r10 = r26
            goto L_0x0318
        L_0x013e:
            r0 = move-exception
            r9 = r18
            r11 = r22
            r10 = r26
            goto L_0x02c5
        L_0x0147:
            r3 = 0
        L_0x0148:
            java.lang.String r11 = r8.readLine()     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r0 = r11
            if (r11 == 0) goto L_0x0254
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r11.<init>()     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r11.append(r0)     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            r11.append(r10)     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            java.lang.String r11 = r11.toString()     // Catch:{ Exception -> 0x013e, all -> 0x0136 }
            int r3 = r3 + 1
            r12 = 1
            if (r3 <= r12) goto L_0x0244
            java.lang.String[] r12 = r11.split(r9)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r27 = r0
            r0 = r12[r25]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equals(r4)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x018d
            int r0 = r2.uid     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r18 = r3
            r3 = r12[r24]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x018f
            int r0 = r2.establishIPv6     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = 1
            int r0 = r0 + r3
            r2.establishIPv6 = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            int r0 = r2.establishAll     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            int r0 = r0 + r3
            r2.establishAll = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            goto L_0x018f
        L_0x018d:
            r18 = r3
        L_0x018f:
            r0 = r12[r25]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equals(r14)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01ab
            int r0 = r2.uid     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = r12[r24]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01ab
            int r0 = r2.syn     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = 1
            int r0 = r0 + r3
            r2.syn = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
        L_0x01ab:
            r0 = r12[r25]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equals(r4)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01d3
            r0 = r12[r16]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = 16
            long r28 = java.lang.Long.parseLong(r0, r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            int r0 = (r28 > r20 ? 1 : (r28 == r20 ? 0 : -1))
            if (r0 <= 0) goto L_0x01d3
            int r0 = r2.uid     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = r12[r24]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01d3
            int r0 = r2.retransmission     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = 1
            int r0 = r0 + r3
            r2.retransmission = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
        L_0x01d3:
            r0 = r12[r25]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equals(r15)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01ef
            int r0 = r2.uid     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = r12[r24]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x01ef
            int r0 = r2.laskAck     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = 1
            int r0 = r0 + r3
            r2.laskAck = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
        L_0x01ef:
            r0 = r12[r25]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equals(r4)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x022d
            int r0 = r2.uid     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String r0 = java.lang.Integer.toString(r0)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r3 = r12[r24]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            boolean r0 = r0.equalsIgnoreCase(r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            if (r0 == 0) goto L_0x0228
            r0 = 5
            r3 = r12[r0]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            java.lang.String[] r3 = r3.split(r13)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r17 = 1
            r0 = r3[r17]     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r17 = r3
            r3 = 16
            long r29 = java.lang.Long.parseLong(r0, r3)     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            int r0 = (r29 > r20 ? 1 : (r29 == r20 ? 0 : -1))
            if (r0 <= 0) goto L_0x0225
            int r0 = r2.receivingQueue     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            r23 = 1
            int r0 = r0 + 1
            r2.receivingQueue = r0     // Catch:{ Exception -> 0x023c, all -> 0x0232 }
            goto L_0x0231
        L_0x0225:
            r23 = 1
            goto L_0x0231
        L_0x0228:
            r3 = 16
            r23 = 1
            goto L_0x0231
        L_0x022d:
            r3 = 16
            r23 = 1
        L_0x0231:
            goto L_0x024c
        L_0x0232:
            r0 = move-exception
            r3 = r0
            r18 = r11
            r11 = r22
            r10 = r26
            goto L_0x0318
        L_0x023c:
            r0 = move-exception
            r9 = r11
            r11 = r22
            r10 = r26
            goto L_0x02c5
        L_0x0244:
            r27 = r0
            r18 = r3
            r23 = r12
            r3 = 16
        L_0x024c:
            r3 = r18
            r0 = r27
            r18 = r11
            goto L_0x0148
        L_0x0254:
            r27 = r0
            long r9 = r1.mUidTxDiff     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r2.f33tx = r9     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            long r9 = r1.mUidRxDiff     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r2.f32rx = r9     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            double r9 = r1.mLoss     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r2.loss = r9     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            int r0 = r1.mRSSI     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r2.rssi = r0     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            int r0 = r1.mLinkSpeed     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r2.linkSpeed = r0     // Catch:{ Exception -> 0x029d, all -> 0x0295 }
            r4 = 1
            r5.close()     // Catch:{ IOException -> 0x0279 }
            r6.close()     // Catch:{ IOException -> 0x0279 }
            r7.close()     // Catch:{ IOException -> 0x0279 }
            r8.close()     // Catch:{ IOException -> 0x0279 }
            goto L_0x0312
        L_0x0279:
            r0 = move-exception
            r18 = 0
            boolean r3 = DBG
            if (r3 == 0) goto L_0x0311
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r10 = r26
            r3.append(r10)
            r3.append(r0)
            java.lang.String r3 = r3.toString()
            r11 = r22
            goto L_0x030e
        L_0x0295:
            r0 = move-exception
            r11 = r22
            r10 = r26
            r3 = r0
            goto L_0x0318
        L_0x029d:
            r0 = move-exception
            r11 = r22
            r10 = r26
            r9 = r18
            goto L_0x02c5
        L_0x02a5:
            r0 = move-exception
            r11 = r4
            r18 = r9
            r19 = r10
            r10 = r3
            r3 = r0
            goto L_0x0318
        L_0x02af:
            r0 = move-exception
            r11 = r4
            r18 = r9
            r19 = r10
            r10 = r3
            goto L_0x02c5
        L_0x02b7:
            r0 = move-exception
            r11 = r4
            r19 = r10
            r10 = r3
            r3 = r0
            r18 = r9
            goto L_0x0318
        L_0x02c0:
            r0 = move-exception
            r11 = r4
            r19 = r10
            r10 = r3
        L_0x02c5:
            boolean r3 = DBG     // Catch:{ all -> 0x0314 }
            if (r3 == 0) goto L_0x02db
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0314 }
            r3.<init>()     // Catch:{ all -> 0x0314 }
            r3.append(r10)     // Catch:{ all -> 0x0314 }
            r3.append(r0)     // Catch:{ all -> 0x0314 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0314 }
            android.util.Log.d(r11, r3)     // Catch:{ all -> 0x0314 }
        L_0x02db:
            r0.printStackTrace()     // Catch:{ all -> 0x0314 }
            r18 = 0
            r4 = 0
            if (r5 == 0) goto L_0x02e9
            r5.close()     // Catch:{ IOException -> 0x02e7 }
            goto L_0x02e9
        L_0x02e7:
            r0 = move-exception
            goto L_0x02f9
        L_0x02e9:
            if (r6 == 0) goto L_0x02ee
            r6.close()     // Catch:{ IOException -> 0x02e7 }
        L_0x02ee:
            if (r7 == 0) goto L_0x02f3
            r7.close()     // Catch:{ IOException -> 0x02e7 }
        L_0x02f3:
            if (r8 == 0) goto L_0x0312
            r8.close()     // Catch:{ IOException -> 0x02e7 }
            goto L_0x0312
        L_0x02f9:
            r18 = 0
            boolean r3 = DBG
            if (r3 == 0) goto L_0x0311
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r10)
            r3.append(r0)
            java.lang.String r3 = r3.toString()
        L_0x030e:
            android.util.Log.d(r11, r3)
        L_0x0311:
            goto L_0x0313
        L_0x0312:
        L_0x0313:
            return r4
        L_0x0314:
            r0 = move-exception
            r3 = r0
            r18 = r9
        L_0x0318:
            if (r5 == 0) goto L_0x0320
            r5.close()     // Catch:{ IOException -> 0x031e }
            goto L_0x0320
        L_0x031e:
            r0 = move-exception
            goto L_0x0330
        L_0x0320:
            if (r6 == 0) goto L_0x0325
            r6.close()     // Catch:{ IOException -> 0x031e }
        L_0x0325:
            if (r7 == 0) goto L_0x032a
            r7.close()     // Catch:{ IOException -> 0x031e }
        L_0x032a:
            if (r8 == 0) goto L_0x0349
            r8.close()     // Catch:{ IOException -> 0x031e }
            goto L_0x0349
        L_0x0330:
            r18 = 0
            boolean r4 = DBG
            if (r4 == 0) goto L_0x034a
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r10)
            r4.append(r0)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r11, r4)
            goto L_0x034a
        L_0x0349:
        L_0x034a:
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.loadTcpInfo(com.android.server.wifi.tcp.WifiTcpMonitorInfo):boolean");
    }

    private boolean isBlockingApp(WifiTcpMonitorInfo info) {
        int i;
        this.mSynBlockCountSum = 0;
        if (this.mWifiPackageInfo.isChatApp() && info.retransmission > 0) {
            int i2 = this.mUidChatAppRetransmissionCounter;
            if (i2 > 5) {
                info.result = "BR";
                return true;
            }
            this.mUidChatAppRetransmissionCounter = i2 + 1;
            info.result = "BR_cont";
            info.chatRetrans = this.mUidChatAppRetransmissionCounter;
        }
        if (info.retransmission <= 0 || (info.retransmission * 100) / info.establishAll < 25) {
            this.mUidRetransmissionCounter = 0;
        } else {
            this.mUidRetransmissionCounter++;
            info.retransCount = this.mUidRetransmissionCounter;
        }
        if (this.mUidRetransmissionCounter > 5) {
            info.result = "BR";
            return true;
        }
        if (this.mUidSynBlockCounter.size() == 10) {
            this.mUidSynBlockCounter.remove(0);
            this.mUidSynBlockCounter.trimToSize();
        }
        if (info.establishAll < this.mUidPrevSynSize && info.syn < this.mUidPrevSynSize) {
            this.mUidSynBlockCounter.add(1);
        } else if (info.syn > 0 && info.establishAll == 0) {
            this.mUidSynBlockCounter.add(1);
        } else if (info.establishAll > 0) {
            boolean isSynBlock = true;
            if (info.establishIPv4 > 0 && (info.syn * 100) / info.establishIPv4 < 50) {
                isSynBlock = false;
            } else if (info.establishIPv6 > 0 && (info.syn * 100) / info.establishIPv6 < 50) {
                isSynBlock = false;
            }
            this.mUidSynBlockCounter.add(Integer.valueOf(isSynBlock ? 1 : 0));
        }
        this.mSynBlockCountSum = 0;
        Iterator<Integer> it = this.mUidSynBlockCounter.iterator();
        while (it.hasNext()) {
            this.mSynBlockCountSum += it.next().intValue();
        }
        int maxSynCount = 5;
        if (this.mWifiTransportLayerMonitor.isSwitchEnabledApp(info.uid) && this.mApInfo.getDetectedPackageList() != null && this.mApInfo.getDetectedPackageList().containsKey(info.packageName) && this.mApInfo.getDetectedPackageList().get(info.packageName).getDetectedCount() > 5) {
            if (DBG) {
                Log.d(TAG, "isBlockingApp - SBC aggressive");
            }
            maxSynCount = 2;
        } else if (info.establishAll < 5) {
            maxSynCount = 3;
        }
        int i3 = this.mSynBlockCountSum;
        info.synBlockCount = i3;
        info.maxSynCount = maxSynCount;
        if (i3 >= maxSynCount) {
            info.result = "BS";
            return true;
        }
        if (i3 <= 0 || info.establishAll != 0) {
            this.mUidSynBlockNoEstablishCounter = 0;
        } else {
            this.mUidSynBlockNoEstablishCounter++;
            info.synBlockNoEstablish = this.mUidSynBlockNoEstablishCounter;
        }
        if (this.mUidSynBlockNoEstablishCounter >= maxSynCount) {
            info.result = "BS";
            return true;
        }
        if (info.receivingQueue <= 0 || info.syn <= 0) {
            this.mUidReceieveQueueCounter = 0;
        } else {
            this.mUidReceieveQueueCounter++;
            int i4 = this.mUidReceieveQueueCounter;
            info.receivingQueueCount = i4;
            if (i4 > 2) {
                info.result = "BQ";
                return true;
            }
        }
        if (this.mWifiPackageInfo.isChatApp()) {
            if (isDnsDetected()) {
                resetDnsDetected();
                if (info.establishAll == 0) {
                    this.mDnsBlockCounter++;
                    int i5 = this.mDnsBlockCounter;
                    info.dnsBlockCount = i5;
                    if (i5 > 3) {
                        info.result = "BD";
                        return true;
                    }
                }
            } else {
                if (info.establishAll != 0 || (i = this.mDnsBlockCounter) <= 0) {
                    this.mDnsBlockCounter = 0;
                } else {
                    this.mDnsBlockCounter = i + 1;
                }
                int i6 = this.mDnsBlockCounter;
                info.dnsBlockCount = i6;
                if (i6 > 3) {
                    info.result = "BD";
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public String getSsid() {
        WifiInfo wifiInfo = syncGetCurrentWifiInfo();
        if (wifiInfo != null) {
            return wifiInfo.getSSID();
        }
        return null;
    }

    private void updateLinkStatistics() {
        int i;
        WifiInfo wifiInfo = syncGetCurrentWifiInfo();
        if (wifiInfo != null) {
            this.mLinkSpeed = wifiInfo.getLinkSpeed();
            this.mRSSI = wifiInfo.getRssi();
            this.mIs5GHz = wifiInfo.is5GHz();
            this.mLoss = this.mWifiConnectivityMonitor.getCurrentLoss();
            if (this.mIs5GHz) {
                this.mLinkSpeedThreshold = 40;
                this.mLinkRssiThreshold = -60;
            } else {
                this.mLinkSpeedThreshold = 30;
                this.mLinkRssiThreshold = -55;
            }
            if (isAggressiveMode()) {
                i = 5;
            } else {
                i = 3;
            }
            this.mThresholdDetectionIgnored = i;
        }
    }

    private WifiInfo syncGetCurrentWifiInfo() {
        WifiInfo wifiInfo = this.mClientModeImpl.getWifiInfo();
        if (wifiInfo != null) {
            return new WifiInfo(wifiInfo);
        }
        return new WifiInfo();
    }

    private void updateRxTx(int uid) {
        long txCurr = TrafficStats.getUidTxPackets(uid);
        long rxCurr = TrafficStats.getUidRxPackets(uid);
        this.mUidTxDiff = txCurr - this.mUidPrevTx;
        this.mUidRxDiff = rxCurr - this.mUidPrevRx;
        this.mUidPrevTx = txCurr;
        this.mUidPrevRx = rxCurr;
        this.mUidAccumulatedTxRx = (txCurr + rxCurr) - this.mUidTxRxOnResume;
    }

    private boolean isPackageException(WifiPackageInfo info) {
        int i;
        int result = 0;
        int uid = info.getUid();
        WifiApInfo.DetectedPackageInfo packageInfo = this.mApInfo.getDetectedPackageList().get(info.getPackageName());
        if (uid == -1 || uid == 1000 || info.isSystemApp()) {
            if (DBG) {
                Log.d(TAG, "isPackageException - system app");
            }
            result = 1;
        } else if (!info.isSwitchable() && ((i = this.mDetectionMode) == 0 || i == 1)) {
            result = 2;
        } else if (isCurrentUidBlockedList(uid)) {
            if (DBG) {
                Log.d(TAG, "isPackageException - already blocked " + info.getPackageName());
            }
            result = 3;
        } else if (this.mUidAccumulatedTxRx > 2000) {
            if (DBG) {
                Log.d(TAG, "isPackageException - txrx:" + this.mUidAccumulatedTxRx);
            }
            result = 4;
        } else if (!this.mWifiTransportLayerMonitor.isSwitchEnabledApp(info.getUid()) && packageInfo != null && packageInfo.getDetectedCount() >= this.mThresholdDetectionIgnored) {
            if (DBG) {
                Log.d(TAG, "isPackageException - detection ignored time:" + packageInfo.getDetectedCount());
            }
            result = 5;
        } else if (!info.hasInternetPermission()) {
            if (DBG) {
                Log.d(TAG, "isPackageException - no internet permission " + info.getPackageName());
            }
            result = 6;
        }
        if (result != 0) {
            Log.d(TAG, "isPackageException - result:" + result + ", " + info.getPackageName());
            this.mSkipUid = uid;
            return true;
        }
        this.mSkipUid = 0;
        return false;
    }

    private boolean isSkipTcpMonitorInfo(WifiTcpMonitorInfo info) {
        int result = 0;
        long latestTxRx = 0;
        ArrayList<Long> arrayList = this.mLatestTxRx;
        if (arrayList != null) {
            if (arrayList.size() >= 3) {
                this.mLatestTxRx.remove(0);
            }
            this.mLatestTxRx.add(Long.valueOf(info.f33tx + info.f32rx));
            Iterator<Long> it = this.mLatestTxRx.iterator();
            while (it.hasNext()) {
                latestTxRx += it.next().longValue();
            }
        }
        if (info.establishAll > this.mThresholdNormalEstablish) {
            if (DBG) {
                Log.d(TAG, "isSkipTcpMonitorInfo - e:" + info.establishAll);
            }
            result = 1;
        } else if (latestTxRx > ((long) this.mThresholdLatestSumTxRx)) {
            if (DBG) {
                Log.d(TAG, "isSkipTcpMonitorInfo - txrx:" + latestTxRx);
            }
            result = 2;
        }
        if (result == 0) {
            return false;
        }
        Log.d(TAG, "isSkipTcpMonitorInfo - result:" + result);
        return true;
    }

    private boolean isSkipNetworkCondition(int uid, String packageName) {
        int result = 0;
        if (this.mLinkSpeed < this.mLinkSpeedThreshold) {
            if (DBG) {
                Log.d(TAG, "isSkipNetworkCondition - linkspeed :" + this.mLinkSpeed);
            }
            result = 1;
        } else if (this.mRSSI < this.mLinkRssiThreshold) {
            if (DBG) {
                Log.d(TAG, "isSkipNetworkCondition - rssi :" + this.mRSSI);
            }
            result = 2;
        } else if (this.mLoss > 0.1d) {
            if (DBG) {
                Log.d(TAG, "isSkipNetworkCondition - loss :" + this.mLoss);
            }
            result = 3;
        } else if (isBeaconRatePoor()) {
            if (DBG) {
                Log.d(TAG, "isSkipNetworkCondition - beacon");
            }
            result = 4;
        } else if (isVoipOngoing()) {
            if (DBG) {
                Log.d(TAG, "isSkipNetworkCondition - voip ongoing");
            }
            result = 5;
        }
        if (result == 0) {
            return false;
        }
        Log.d(TAG, "isSkipNetworkCondition - result:" + result);
        return true;
    }

    private boolean isBeaconRatePoor() {
        boolean result;
        int currentRunningBeaconCount = this.mClientModeImpl.getBeaconCount();
        int i = this.mPrevRunningBeaconCount;
        if (i == 0) {
            this.mPrevRunningBeaconCount = currentRunningBeaconCount;
            if (DBG) {
                Log.d(TAG, "isBeaconRatePoor - skip first beacon count");
            }
            return false;
        }
        int beaconDiff = currentRunningBeaconCount - i;
        int averageNumberOfBeacon = 0;
        if (beaconDiff < 0) {
            this.mBeaconRateCount = 0;
            if (this.mRunningBeaconCountList.size() > 0) {
                this.mRunningBeaconCountList.clear();
                this.mRunningBeaconCountList.trimToSize();
            }
            result = true;
        } else if (beaconDiff == 0) {
            int i2 = this.mBeaconRateCount + 1;
            this.mBeaconRateCount = i2;
            if (i2 > 3) {
                result = true;
            } else {
                result = false;
            }
        } else if (beaconDiff < 40) {
            this.mBeaconRateCount = 0;
            if (this.mRunningBeaconCountList.size() == 3) {
                this.mRunningBeaconCountList.remove(0);
                this.mRunningBeaconCountList.trimToSize();
            }
            this.mRunningBeaconCountList.add(Integer.valueOf(beaconDiff));
            if (this.mRunningBeaconCountList.size() > 0) {
                Iterator<Integer> it = this.mRunningBeaconCountList.iterator();
                while (it.hasNext()) {
                    averageNumberOfBeacon += it.next().intValue();
                }
                averageNumberOfBeacon /= this.mRunningBeaconCountList.size();
                if (averageNumberOfBeacon > 5) {
                    result = false;
                } else {
                    result = true;
                }
            } else {
                result = true;
            }
        } else {
            result = true;
        }
        if (DBG && result) {
            Log.d(TAG, "isBeaconRatePoor: " + result + ", " + this.mPrevRunningBeaconCount + ", " + currentRunningBeaconCount + ", " + beaconDiff + ", " + averageNumberOfBeacon);
        }
        this.mPrevRunningBeaconCount = currentRunningBeaconCount;
        return result;
    }

    private boolean isNotifyEnabledApp(WifiPackageInfo info) {
        if (info == null) {
            Log.d(TAG, "isNotifyEnabledApp - null WifiPackageInfo");
            return false;
        }
        int i = this.mDetectionMode;
        if (i != 0) {
            if (i != 1) {
                if (i == 2 && !info.isSystemApp()) {
                    return true;
                }
            } else if (info.isSwitchable()) {
                return true;
            }
        } else if (info.isChatApp()) {
            return true;
        }
        return false;
    }

    private boolean uidBlockedAppDetected(int uid, String packageName) {
        if (isCurrentUidBlockedList(uid)) {
            Log.d(TAG, "uidBlockedAppDetected: already detected " + uid);
            return false;
        }
        WifiApInfo wifiApInfo = this.mApInfo;
        if (wifiApInfo == null || !wifiApInfo.isNormalRunningTimePrevention(packageName)) {
            WifiApInfo wifiApInfo2 = this.mApInfo;
            if (wifiApInfo2 != null) {
                wifiApInfo2.updateNormalOperationTime(packageName, 0);
                this.mWifiTransportLayerMonitor.updateWifiApInfo(this.mApInfo);
            }
            if (this.mSwitchForIndividualAppsEverDetected || !isNotifyEnabledApp(this.mWifiTransportLayerMonitor.getWifiPackageInfo(uid))) {
                if (!this.mIsWaitForQCResult) {
                    if (DBG) {
                        Log.d(TAG, "uidBlockedAppDetected: qc trigger " + uid);
                    }
                    this.mUidListWaitingForQcResult.add(Integer.valueOf(uid));
                    sendEmptyMessage(135689);
                } else {
                    if (DBG) {
                        Log.d(TAG, "uidBlockedAppDetected: add " + uid);
                    }
                    this.mUidListWaitingForQcResult.add(Integer.valueOf(uid));
                }
                if (isNotifyEnabledApp(this.mWifiTransportLayerMonitor.getWifiPackageInfo(uid))) {
                    return true;
                }
                if (DBG) {
                    Log.d(TAG, "uidBlockedAppDetected: do not notify to user ");
                }
                return false;
            }
            Log.d(TAG, "uidBlockedAppDetected: first detected");
            this.mSwitchForIndividualAppsEverDetected = true;
            Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_ever_detected", 1);
            this.mLastSuggestionNotifiedTcpMonitorInfo = this.mLastDetectedTcpMonitorInfo;
            this.mNotifier.showNotification(1, uid, packageName);
            if (this.mIsForegroundActivityDetectionStarted) {
                stopTCPMonitoring(4);
            }
            return false;
        }
        Log.d(TAG, "uidBlockedAppDetected: Normal running time prevention - " + packageName);
        return false;
    }

    private void updateQcResult(boolean qcPass) {
        ArrayList<Integer> arrayList = this.mUidListWaitingForQcResult;
        if (arrayList != null && !arrayList.isEmpty()) {
            if (qcPass) {
                Iterator<Integer> it = this.mUidListWaitingForQcResult.iterator();
                while (it.hasNext()) {
                    int uid = it.next().intValue();
                    if (!isNotifyEnabledApp(this.mWifiTransportLayerMonitor.getWifiPackageInfo(uid))) {
                        if (DBG) {
                            Log.d(TAG, "updateQcResult - skip switching " + uid);
                        }
                    } else if (!this.mWifiTransportLayerMonitor.isSwitchEnabledApp(uid)) {
                        if (DBG) {
                            Log.d(TAG, "updateQcResult - show first notification " + uid);
                        }
                        this.mLastSwitchNotifiedTcpMonitorInfo = this.mLastDetectedTcpMonitorInfo;
                        this.mNotifier.showNotification(2, uid, this.mWifiTransportLayerMonitor.getPackageName(uid));
                    } else {
                        if (DBG) {
                            Log.d(TAG, "updateQcResult - switch " + uid);
                        }
                        sendMessage(obtainMessage(135692, uid, 0, this.mWifiTransportLayerMonitor.getPackageName(uid)));
                    }
                }
            }
            this.mUidListWaitingForQcResult.clear();
        }
    }

    private void addCurrentUidBlockedList(int uid) {
        Log.d(TAG, "addCurrentUidBlockedList: " + uid);
        if (this.mCurrentUidBlockedList != null) {
            synchronized (this.mCurrentUidBlockedListLock) {
                this.mCurrentUidBlockedList.remove(Integer.valueOf(uid));
                this.mCurrentUidBlockedList.put(Integer.valueOf(uid), UID_STATUS.DETECTED);
            }
        }
    }

    private void removeCurrentUidblockedList(int uid) {
        Log.d(TAG, "removeCurrentUidblockedList: " + uid);
        if (this.mCurrentUidBlockedList != null) {
            synchronized (this.mCurrentUidBlockedListLock) {
                this.mCurrentUidBlockedList.remove(Integer.valueOf(uid));
            }
        }
    }

    private void clearCurrentUidBlockedList() {
        Log.d(TAG, "clearCurrentUidBlockedList:");
        if (this.mCurrentUidBlockedList != null) {
            synchronized (this.mCurrentUidBlockedListLock) {
                this.mCurrentUidBlockedList.clear();
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0047, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateCurrentUidBlockedListStatus(int r4, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS r5) {
        /*
            r3 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "updateCurrentUidBlockedListStatus: "
            r0.append(r1)
            r0.append(r4)
            java.lang.String r1 = ", "
            r0.append(r1)
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "WifiSwitchForIndividualAppsService"
            android.util.Log.d(r1, r0)
            java.lang.Object r0 = r3.mCurrentUidBlockedListLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0049 }
            if (r1 == 0) goto L_0x0046
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0049 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0049 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0049 }
            if (r1 == 0) goto L_0x0046
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0049 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0049 }
            r1.remove(r2)     // Catch:{ all -> 0x0049 }
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0049 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0049 }
            r1.put(r2, r5)     // Catch:{ all -> 0x0049 }
            r1 = 1
            monitor-exit(r0)     // Catch:{ all -> 0x0049 }
            return r1
        L_0x0046:
            monitor-exit(r0)     // Catch:{ all -> 0x0049 }
            r0 = 0
            return r0
        L_0x0049:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0049 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.updateCurrentUidBlockedListStatus(int, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS):boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0024, code lost:
        return com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS.NONE;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS getCurrentUidBlockedListStatus(int r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mCurrentUidBlockedListLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0025 }
            if (r1 == 0) goto L_0x0021
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0025 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0025 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0025 }
            if (r1 == 0) goto L_0x0021
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0025 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0025 }
            java.lang.Object r1 = r1.get(r2)     // Catch:{ all -> 0x0025 }
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS r1 = (com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS) r1     // Catch:{ all -> 0x0025 }
            monitor-exit(r0)     // Catch:{ all -> 0x0025 }
            return r1
        L_0x0021:
            monitor-exit(r0)     // Catch:{ all -> 0x0025 }
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS r0 = com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS.NONE
            return r0
        L_0x0025:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0025 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.getCurrentUidBlockedListStatus(int):com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0017, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isCurrentUidBlockedList(int r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mCurrentUidBlockedListLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0019 }
            if (r1 == 0) goto L_0x0016
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r3.mCurrentUidBlockedList     // Catch:{ all -> 0x0019 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0019 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0019 }
            if (r1 == 0) goto L_0x0016
            r1 = 1
            monitor-exit(r0)     // Catch:{ all -> 0x0019 }
            return r1
        L_0x0016:
            monitor-exit(r0)     // Catch:{ all -> 0x0019 }
            r0 = 0
            return r0
        L_0x0019:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0019 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.isCurrentUidBlockedList(int):boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x002e, code lost:
        android.util.Log.d(TAG, "hasActivatedCurrentUidBlockedList: no items");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0036, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasActivatedCurrentUidBlockedList() {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mCurrentUidBlockedListLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r4.mCurrentUidBlockedList     // Catch:{ all -> 0x0037 }
            if (r1 == 0) goto L_0x002d
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r4.mCurrentUidBlockedList     // Catch:{ all -> 0x0037 }
            boolean r1 = r1.isEmpty()     // Catch:{ all -> 0x0037 }
            if (r1 != 0) goto L_0x002d
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS> r1 = r4.mCurrentUidBlockedList     // Catch:{ all -> 0x0037 }
            java.util.Collection r1 = r1.values()     // Catch:{ all -> 0x0037 }
            java.util.Iterator r1 = r1.iterator()     // Catch:{ all -> 0x0037 }
        L_0x0019:
            boolean r2 = r1.hasNext()     // Catch:{ all -> 0x0037 }
            if (r2 == 0) goto L_0x002d
            java.lang.Object r2 = r1.next()     // Catch:{ all -> 0x0037 }
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS r2 = (com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS) r2     // Catch:{ all -> 0x0037 }
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService$UID_STATUS r3 = com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.UID_STATUS.ACTIVATED     // Catch:{ all -> 0x0037 }
            if (r2 != r3) goto L_0x002c
            r1 = 1
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            return r1
        L_0x002c:
            goto L_0x0019
        L_0x002d:
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            java.lang.String r0 = "WifiSwitchForIndividualAppsService"
            java.lang.String r1 = "hasActivatedCurrentUidBlockedList: no items"
            android.util.Log.d(r0, r1)
            r0 = 0
            return r0
        L_0x0037:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService.hasActivatedCurrentUidBlockedList():boolean");
    }

    /* access modifiers changed from: private */
    public void updateDnsInfo() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager != null) {
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            this.mDnsList.clear();
            int dns1 = dhcpInfo.dns1;
            int dns2 = dhcpInfo.dns2;
            if (dns1 > 0) {
                this.mDnsList.add(intToIp(dns1));
            }
            if (dns2 > 0) {
                this.mDnsList.add(intToIp(dns2));
            }
        }
    }

    private String intToIp(int i) {
        return (i & 255) + "." + ((i >> 8) & 255) + "." + ((i >> 16) & 255) + "." + ((i >> 24) & 255);
    }

    private void useMobileDataForBlockedApp(int uid) {
        if (!this.mIsMobileDataEnabledByTcpMonitor) {
            sendMessage(obtainMessage(135699));
        }
        if (isMobileDataConnected()) {
            this.mIsInterfaceNameNotAvailable = false;
            changeRouteToMobileNetwork(uid);
            return;
        }
        sendMessageDelayed(obtainMessage(135701, uid, 1), 3000);
    }

    private void useDefaultNetworkForBlockedApp(int uid) {
        changeRouteToDefaultNetwork(uid);
        if (!hasActivatedCurrentUidBlockedList()) {
            sendMessage(obtainMessage(135700));
            this.mNotifier.clearNotificationAll();
        }
    }

    private void useDefaultNetworkForAllApp() {
        clearCurrentUidBlockedList();
        removeMessages(135701);
        changeAllRouteToDefaultNetwork();
        sendMessage(obtainMessage(135700));
        this.mNotifier.clearNotificationAll();
    }

    private class NetdCommandHandler extends Handler {
        private static final int MSG_RUN_SHELL_COMMAND = 1;
        private static final int MSG_RUN_SHELL_COMMAND_AGAIN = 2;
        private final String NETD_SERVICE_NAME = "netd";
        private final String SUCCESS = "SUCCESS";
        /* access modifiers changed from: private */
        public INetd mNetdService;

        public NetdCommandHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                String command = (String) msg.obj;
                if (WifiSwitchForIndividualAppsService.DBG) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "NetdCommandHandler MSG_RUN_SHELL_COMMAND");
                }
                runNetdShellCommand(command, false);
            } else if (i == 2) {
                String commandAgain = (String) msg.obj;
                if (WifiSwitchForIndividualAppsService.DBG) {
                    Log.d(WifiSwitchForIndividualAppsService.TAG, "NetdCommandHandler MSG_RUN_SHELL_COMMAND_AGAIN");
                }
                runNetdShellCommand(commandAgain, true);
            }
            super.handleMessage(msg);
        }

        private void connectNativeNetdService() {
            boolean nativeServiceAvailable = false;
            try {
                this.mNetdService = INetd.Stub.asInterface(ServiceManager.getService("netd"));
                if (this.mNetdService != null) {
                    nativeServiceAvailable = this.mNetdService.isAlive();
                    if (!nativeServiceAvailable && WifiSwitchForIndividualAppsService.DBG) {
                        Log.e(WifiSwitchForIndividualAppsService.TAG, "connectNativeNetdService: connection failed");
                    }
                }
            } catch (RemoteException e) {
                this.mNetdService = null;
            }
        }

        private void runNetdShellCommand(final String command, final boolean isRetry) {
            if (this.mNetdService == null) {
                connectNativeNetdService();
            }
            if (this.mNetdService != null) {
                new Thread() {
                    public void run() {
                        Log.d(WifiSwitchForIndividualAppsService.TAG, "runNetdShellCommand(cmd):" + command);
                        String param = "";
                        int index = 0;
                        while (index < command.length()) {
                            try {
                                param = param + ((char) (command.charAt(index) ^ "TCPMONITOR".charAt(index % "TCPMONITOR".length())));
                                index++;
                            } catch (RemoteException e) {
                                Log.d(WifiSwitchForIndividualAppsService.TAG, "runNetdShellCommand - RemoteException");
                                e.printStackTrace();
                                return;
                            } catch (Exception e2) {
                                Log.d(WifiSwitchForIndividualAppsService.TAG, "runNetdShellCommand - " + e2);
                                e2.printStackTrace();
                                return;
                            }
                        }
                        if (WifiSwitchForIndividualAppsService.DBG) {
                            Log.d(WifiSwitchForIndividualAppsService.TAG, "runNetdShellCommand(encode):" + param);
                        }
                        String result = NetdCommandHandler.this.mNetdService.runTcpMonitorShellCommand(command, param);
                        if (WifiSwitchForIndividualAppsService.DBG) {
                            Log.d(WifiSwitchForIndividualAppsService.TAG, "runNetdShellCommand(result):" + result);
                        }
                        if (result.equals(param + " - ")) {
                            WifiSwitchForIndividualAppsService.this.runShellCommandResult("SUCCESS");
                            return;
                        }
                        if (!isRetry) {
                            WifiSwitchForIndividualAppsService.this.runDelayedNetdShellCommandAgain(command);
                        }
                        WifiSwitchForIndividualAppsService.this.runShellCommandResult(result);
                    }
                }.start();
            } else {
                Log.d(WifiSwitchForIndividualAppsService.TAG, "Netd Service is null");
            }
        }
    }

    /* access modifiers changed from: private */
    public void runDelayedNetdShellCommandAgain(String command) {
        sendMessageDelayed(obtainMessage(135710, command), 3000);
    }

    /* access modifiers changed from: private */
    public void runShellCommandResult(String result) {
        sendMessage(obtainMessage(135709, result));
    }

    private void changeRouteToMobileNetwork(int uid) {
        Log.d(TAG, "changeRouteToMobileNetwork: " + uid);
        String mobileIfaceName = getMobileInterfaceName();
        if (mobileIfaceName != null) {
            String command = "ip -4 rule add from all uidrange " + uid + "-" + uid + " table " + mobileIfaceName + " pref 2";
            if (DBG) {
                Log.d(TAG, "changeRouteToMobileNetwork - command: " + command);
            }
            NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
            if (netdCommandHandler != null) {
                netdCommandHandler.sendMessage(obtainMessage(1, command));
            }
            modifyTcpTimeOut();
            removeMessages(135702);
            sendMessageDelayed(obtainMessage(135702), 15000);
            changeDnsRouteUid(uid, "wlan0");
        } else if (!this.mIsInterfaceNameNotAvailable) {
            if (DBG) {
                Log.d(TAG, "changeRouteToMobileNetwork - check interface name again");
            }
            sendMessageDelayed(obtainMessage(135701, uid, 1), 3000);
            this.mIsInterfaceNameNotAvailable = true;
        } else {
            if (DBG) {
                Log.d(TAG, "changeRouteToMobileNetwork - failed to get interface name");
            }
            this.mIsInterfaceNameNotAvailable = false;
        }
    }

    private void changeRouteToDefaultNetwork(int uid) {
        String command = "ip -4 rule del from all uidrange " + uid + "-" + uid + " pref 2";
        if (DBG) {
            Log.d(TAG, "changeRouteToDefaultNetwork - command: " + command);
        }
        NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
        if (netdCommandHandler != null) {
            netdCommandHandler.sendMessage(obtainMessage(1, command));
        }
        removeDnsRouteUid(uid, "wlan0");
    }

    private void changeAllRouteToDefaultNetwork() {
        if (DBG) {
            Log.d(TAG, "changeAllRouteToDefaultNetwork - command: " + " while ip rule del pref 2 2>/dev/null; do true; done");
        }
        NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
        if (netdCommandHandler != null) {
            netdCommandHandler.sendMessage(obtainMessage(1, " while ip rule del pref 2 2>/dev/null; do true; done"));
        }
        removeAllDnsRoutes();
    }

    private void modifyTcpTimeOut() {
        if (DBG) {
            Log.d(TAG, "modifyTcpTimeOut - command: " + "echo 3 > /proc/sys/net/ipv4/tcp_retries2");
        }
        NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
        if (netdCommandHandler != null) {
            netdCommandHandler.sendMessage(obtainMessage(1, "echo 3 > /proc/sys/net/ipv4/tcp_retries2"));
        }
    }

    private void resetTcpTimeOut() {
        if (DBG) {
            Log.d(TAG, "resetTcpTimeOut - command: " + "echo 15 > /proc/sys/net/ipv4/tcp_retries2");
        }
        NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
        if (netdCommandHandler != null) {
            netdCommandHandler.sendMessage(obtainMessage(1, "echo 15 > /proc/sys/net/ipv4/tcp_retries2"));
        }
    }

    private void changeDnsRouteUid(int uid, String interfaceName) {
        ArrayList<String> arrayList = this.mDnsList;
        if (arrayList != null && !arrayList.isEmpty()) {
            Iterator<String> it = this.mDnsList.iterator();
            while (it.hasNext()) {
                String ip = it.next();
                Log.d(TAG, "changeDnsRouteUid: " + uid + " - " + ip);
                String command = "ip -4 rule add from all uidrange " + uid + "-" + uid + " to " + ip + " table " + interfaceName + " pref 1";
                if (DBG) {
                    Log.d(TAG, "changeDnsRouteUid - command: " + command);
                }
                NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
                if (netdCommandHandler != null) {
                    netdCommandHandler.sendMessage(obtainMessage(1, command));
                }
            }
        }
    }

    private void removeDnsRouteUid(int uid, String interfaceName) {
        ArrayList<String> arrayList = this.mDnsList;
        if (arrayList != null && !arrayList.isEmpty()) {
            Iterator<String> it = this.mDnsList.iterator();
            while (it.hasNext()) {
                String command = "ip -4 rule del from all uidrange " + uid + "-" + uid + " to " + it.next() + " table " + interfaceName + " pref 1";
                if (DBG) {
                    Log.d(TAG, "removeDnsRouteUid - command: " + command);
                }
                NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
                if (netdCommandHandler != null) {
                    netdCommandHandler.sendMessage(obtainMessage(1, command));
                }
            }
        }
    }

    private void removeAllDnsRoutes() {
        if (DBG) {
            Log.d(TAG, "RemoveAllDnsRoutes - command: " + " while ip rule del pref 1 2>/dev/null; do true; done");
        }
        NetdCommandHandler netdCommandHandler = this.mNetdCommandHandler;
        if (netdCommandHandler != null) {
            netdCommandHandler.sendMessage(obtainMessage(1, " while ip rule del pref 1 2>/dev/null; do true; done"));
        }
    }

    private String getMobileInterfaceName() {
        String mobileIfaceName = null;
        for (Network network : getConnectivityManager().getAllNetworks()) {
            NetworkCapabilities nc = getConnectivityManager().getNetworkCapabilities(network);
            if (nc.hasTransport(0) && nc.hasCapability(12)) {
                LinkProperties lp = getConnectivityManager().getLinkProperties(network);
                Iterator<LinkAddress> it = lp.getLinkAddresses().iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (it.next().isIpv4()) {
                            mobileIfaceName = lp.getInterfaceName();
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (mobileIfaceName == null && lp.toString().contains("Stacked")) {
                    mobileIfaceName = lp.toString().split("Stacked")[1].split(" ")[3];
                }
            }
        }
        if (DBG) {
            Log.d(TAG, "getMobileInterfaceName - " + mobileIfaceName);
        }
        return mobileIfaceName;
    }

    private boolean isMobileDataConnected() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (this.mTelephonyManager.getDataState() == 2) {
            if (!DBG) {
                return true;
            }
            Log.d(TAG, "isMobileDataConnected: true");
            return true;
        } else if (!DBG) {
            return false;
        } else {
            Log.d(TAG, "isMobileDataConnected: false");
            return false;
        }
    }

    public void factoryResetForTcpMonitoring() {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_enabled", 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_ever_detected", 0);
        removeFile(WifiTransportLayerFileManager.FILE_TCP_MONITOR_AP_INFO);
        removeFile(WifiTransportLayerFileManager.FILE_TCP_MONITOR_PACKAGE_INFO);
        removeFile(WifiTransportLayerFileManager.FILE_TCP_SWITCHABLE_UID_INFO);
    }

    private void removeFile(String path) {
        File file = new File(path);
        try {
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.w(TAG, "removeFile - Exception - " + e);
            e.printStackTrace();
        }
    }

    private void updateTcpDetectedHistory() {
        ArrayList<WifiTcpMonitorInfo> arrayList = this.mTcpMonitorInfoHistory;
        if (arrayList != null && !arrayList.isEmpty()) {
            int endIndex = this.mTcpMonitorInfoHistory.size();
            int startIndex = endIndex - 10;
            if (startIndex < 0) {
                startIndex = 0;
            }
            this.mTcpMonitorDetectedHistory.addAll(this.mTcpMonitorInfoHistory.subList(startIndex, endIndex));
            while (this.mTcpMonitorDetectedHistory.size() > 300) {
                this.mTcpMonitorDetectedHistory.remove(0);
            }
        }
    }

    private void addTcpMonitorInfoList(WifiTcpMonitorInfo info) {
        this.mLastTcpMonitorTime = SystemClock.elapsedRealtime();
        synchronized (mTCPEdatalock) {
            if (this.mTcpMonitorInfoHistory != null) {
                if (this.mTcpMonitorInfoHistory.size() >= 300) {
                    this.mTcpMonitorInfoHistory.remove(0);
                    this.mTcpMonitorInfoHistory.trimToSize();
                }
                this.mTcpMonitorInfoHistory.add(info);
            }
        }
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("[isSwitchForIndividualAppsEnabled] " + isSwitchForIndividualAppsEnabled() + "\n");
        sb.append("[mSwitchForIndividualAppsEnabled] " + this.mSwitchForIndividualAppsEnabled + "\n");
        sb.append("[mSwitchForIndividualAppsEverDetected] " + this.mSwitchForIndividualAppsEverDetected + "\n");
        sb.append("[mPoorNetworkDetectionEnabled] " + this.mPoorNetworkDetectionEnabled + "\n");
        sb.append("[mCurrentQai] " + this.mCurrentQai + "\n");
        sb.append("[mDetectionMode] " + this.mDetectionMode + "\n");
        sb.append("[mIsWifiValidState] " + this.mIsWifiValidState + "\n");
        sb.append("[mIsScreenOn] " + this.mIsScreenOn + "\n");
        sb.append("\n[CURRENT UID LIST]\n");
        HashMap<Integer, UID_STATUS> hashMap = this.mCurrentUidBlockedList;
        if (hashMap == null || hashMap.isEmpty()) {
            sb.append("EMPTY\n");
        } else {
            int index = 1;
            try {
                HashMap<Integer, UID_STATUS> blockedList = (HashMap) this.mCurrentUidBlockedList.clone();
                for (Integer intValue : blockedList.keySet()) {
                    int uid = intValue.intValue();
                    sb.append("[" + index + "] " + uid + ":" + this.mWifiTransportLayerMonitor.getPackageName(uid) + ":" + blockedList.get(Integer.valueOf(uid)) + "\n");
                    index++;
                }
            } catch (Exception e) {
                Log.e(TAG, "dump - Exception " + e);
                e.printStackTrace();
            }
        }
        sb.append("\n[LATEST HISTORY]\n");
        ArrayList<WifiTcpMonitorInfo> arrayList = this.mTcpMonitorInfoHistory;
        if (arrayList == null || arrayList.isEmpty()) {
            sb.append("EMPTY\n");
        } else {
            int index2 = 1;
            Iterator<WifiTcpMonitorInfo> it = this.mTcpMonitorInfoHistory.iterator();
            while (it.hasNext()) {
                sb.append("[" + index2 + "] " + it.next().toString() + "\n");
                index2++;
            }
        }
        sb.append("\n\n[DETECTED HISTORY]\n");
        ArrayList<WifiTcpMonitorInfo> arrayList2 = this.mTcpMonitorDetectedHistory;
        if (arrayList2 == null || arrayList2.isEmpty()) {
            sb.append("EMPTY\n");
        } else {
            int index3 = 1;
            Iterator<WifiTcpMonitorInfo> it2 = this.mTcpMonitorDetectedHistory.iterator();
            while (it2.hasNext()) {
                sb.append("[" + index3 + "] " + it2.next().toString() + "\n");
                index3++;
            }
        }
        sb.append("\n\n[HANDLER MESSAGES]\n");
        ArrayList<String> arrayList3 = this.mDumpHandlerMsg;
        if (arrayList3 == null || arrayList3.isEmpty()) {
            sb.append("EMPTY\n");
        } else {
            int index4 = 1;
            Iterator<String> it3 = this.mDumpHandlerMsg.iterator();
            while (it3.hasNext()) {
                sb.append("[" + index4 + "] " + it3.next() + "\n");
                index4++;
            }
        }
        WifiTransportLayerMonitor wifiTransportLayerMonitor = this.mWifiTransportLayerMonitor;
        if (wifiTransportLayerMonitor != null) {
            sb.append(wifiTransportLayerMonitor.dump());
        }
        return sb.toString();
    }

    private String writeHandlerMsg(Message msg) {
        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date()).toString());
        int i = msg.what;
        if (i != 135719) {
            switch (i) {
                case 135669:
                    sb.append("TCP_MONITOR_FOREGROUND_ACTIVITY_START");
                    break;
                case 135670:
                    sb.append("TCP_MONITOR_FOREGROUND_ACTIVITY_STOP");
                    break;
                case 135671:
                    sb.append("TCP_MONITOR_FOREGROUND_ACTIVITY_DETECT");
                    break;
                default:
                    switch (i) {
                        case SWITCH_TO_MOBILE_DATA_ENABLED /*135678*/:
                            sb.append("SWITCH_TO_MOBILE_DATA_ENABLED");
                            break;
                        case SWITCH_TO_MOBILE_DATA_DISABLED /*135679*/:
                            sb.append("SWITCH_TO_MOBILE_DATA_DISABLED");
                            break;
                        case SWITCH_TO_MOBILE_DATA_QAI /*135680*/:
                            sb.append("SWITCH_TO_MOBILE_DATA_QAI");
                            break;
                        default:
                            switch (i) {
                                case 135689:
                                    sb.append("TCP_MONITOR_QC_REQUEST");
                                    break;
                                case 135690:
                                    sb.append("TCP_MONITOR_QC_RESULT_UPDATED");
                                    break;
                                case 135691:
                                    sb.append("TCP_MONITOR_QC_RESULT_TIMEOUT");
                                    break;
                                case 135692:
                                    sb.append("TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_MOBILE_DATA");
                                    break;
                                case 135693:
                                    sb.append("TCP_MONITOR_SWITCH_INDIVIDUAL_APP_TO_WIFI");
                                    break;
                                case 135694:
                                    sb.append("TCP_MONITOR_SWITCH_INDIVIDUAL_APP_LIST_CHANGED");
                                    break;
                                default:
                                    switch (i) {
                                        case 135699:
                                            sb.append("TCP_MONITOR_TURN_ON_MOBILE_NETWORK");
                                            break;
                                        case 135700:
                                            sb.append("TCP_MONITOR_TURN_OFF_MOBILE_NETWORK");
                                            break;
                                        case 135701:
                                            sb.append("TCP_MONITOR_CHECK_MOBILE_DATA_ENABLED");
                                            break;
                                        case 135702:
                                            sb.append("TCP_MONITOR_RESET_TCP_TIMEOUT_VALUE");
                                            break;
                                        default:
                                            switch (i) {
                                                case 135709:
                                                    sb.append("TCP_MONITOR_SHELL_COMMAND_RESULT");
                                                    break;
                                                case 135710:
                                                    sb.append("TCP_MONITOR_RUN_SHELL_COMMAND_AGAIN");
                                                    break;
                                                default:
                                                    sb.append(msg.what);
                                                    sb.append(" ");
                                                    break;
                                            }
                                    }
                            }
                    }
            }
        } else {
            sb.append("TCP_MONITOR_VOIP_STATE_CHANGED");
        }
        sb.append(" ");
        sb.append(msg.arg1);
        sb.append(" ");
        sb.append(msg.arg2);
        sb.append(" ");
        sb.append(msg.obj);
        this.mDumpHandlerMsg.add(sb.toString());
        while (this.mDumpHandlerMsg.size() > 300) {
            this.mDumpHandlerMsg.remove(0);
        }
        return sb.toString();
    }

    private void resetDnsDetected() {
        this.mDnsDetected = false;
    }

    private boolean isDnsDetected() {
        return this.mDnsDetected;
    }

    private void registerDnsCallback() {
        IIpConnectivityMetrics iIpConnectivityMetrics;
        if (this.mIpConnectivityMetrics == null) {
            this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
        }
        if (!this.isDnsCallbackRegistered && (iIpConnectivityMetrics = this.mIpConnectivityMetrics) != null) {
            try {
                if (iIpConnectivityMetrics.addNetdEventCallback(4, this.mNetdEventCallback)) {
                    if (DBG) {
                        Log.d(TAG, "registerDnsCallback - added");
                    }
                    this.isDnsCallbackRegistered = true;
                }
            } catch (RemoteException e) {
                Log.d(TAG, "registerDnsCallback - RemoteException:" + e);
                e.printStackTrace();
            }
        }
    }

    private void unregisterDnsCallback() {
        IIpConnectivityMetrics iIpConnectivityMetrics;
        if (this.mIpConnectivityMetrics == null) {
            this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
        }
        if (this.isDnsCallbackRegistered && (iIpConnectivityMetrics = this.mIpConnectivityMetrics) != null) {
            try {
                if (iIpConnectivityMetrics.removeNetdEventCallback(4)) {
                    if (DBG) {
                        Log.d(TAG, "unregisterDnsCallback - removed");
                    }
                    this.isDnsCallbackRegistered = false;
                }
            } catch (RemoteException e) {
                Log.d(TAG, "unregisterDnsCallback - RemoteException:" + e);
                e.printStackTrace();
            }
        }
    }

    private void resetBigDataFeatureForTCPE() {
        this.mSnsBigDataTCPE.initialize();
        this.mSnsBigDataManager.clearFeature("TCPE");
    }

    /* access modifiers changed from: private */
    public void sendBigDataFeatureForTCPE(WifiTcpMonitorInfo info, boolean reset) {
        int frequency;
        long now = SystemClock.elapsedRealtime();
        WifiInfo wifiInfo = syncGetCurrentWifiInfo();
        if (wifiInfo == null || !wifiInfo.is5GHz()) {
            frequency = 2;
        } else {
            frequency = 5;
        }
        if (DBG) {
            Log.d(TAG, "sendBigDataFeatureForTCPE = " + frequency + ", " + info.toString());
        }
        SnsBigDataTCPE snsBigDataTCPE = this.mSnsBigDataTCPE;
        snsBigDataTCPE.mTcpTime = now;
        snsBigDataTCPE.mTcpApFrequency = frequency;
        snsBigDataTCPE.mTcpQcResults = info.actionResult;
        this.mSnsBigDataTCPE.mTcpAlgorithmResult = info.result;
        this.mSnsBigDataTCPE.mTcpPackageName = info.packageName;
        this.mSnsBigDataTCPE.mTcpEstablished = info.establishAll;
        this.mSnsBigDataTCPE.mTcpSyn = info.syn;
        this.mSnsBigDataTCPE.mTcpRetransmission = info.retransmission;
        this.mSnsBigDataTCPE.mTcpLastAck = info.laskAck;
        this.mSnsBigDataTCPE.mTcpRx = info.f32rx;
        this.mSnsBigDataTCPE.mTcpTx = info.f33tx;
        this.mSnsBigDataTCPE.mTcpLoss = info.loss;
        this.mSnsBigDataTCPE.mTcpRssi = info.rssi;
        this.mSnsBigDataTCPE.mTcpLinkSpeed = info.linkSpeed;
        WifiPackageInfo packageInfo = this.mWifiTransportLayerMonitor.getWifiPackageInfo(info.uid);
        this.mSnsBigDataTCPE.mTcpCategory = packageInfo.getCategory();
        this.mSnsBigDataTCPE.mTcpPackageDetectedCount = packageInfo.getDetectedCount();
        this.mSnsBigDataTCPE.mTcpAutoSwitchEnabled = this.mWifiTransportLayerMonitor.isSwitchEnabledApp(info.uid) ? 1 : 0;
        this.mSnsBigDataTCPE.mTcpApPackageDetectedCount = this.mApInfo.getPackageDetectedCount(info.packageName);
        this.mSnsBigDataTCPE.mTcpApDetectedCount = this.mApInfo.getSwitchForIndivdiaulAppsDetectionCount();
        this.mSnsBigDataTCPE.mTcpApConnectionCount = this.mApInfo.getAccumulatedConnectionCount();
        this.mSnsBigDataTCPE.mTcpApConnectionTime = this.mApInfo.getAccumulatedConnectionTime();
        if (this.mSnsBigDataManager.addOrUpdateFeatureAllValue("TCPE")) {
            this.mSnsBigDataManager.insertLog("TCPE");
        }
        if (reset) {
            resetBigDataFeatureForTCPE();
        }
    }
}
