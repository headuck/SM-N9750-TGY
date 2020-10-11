package com.android.server.wifi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.IUsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.net.util.InterfaceParams;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.LocalLog;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.SystemInfo;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService;
import com.android.server.wifi.tcp.WifiTransportLayerMonitor;
import com.samsung.android.app.usage.IUsageStatsWatcher;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.SemWifiConstants;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.samsung.android.server.wifi.sns.SnsBigDataManager;
import com.samsung.android.server.wifi.sns.SnsBigDataSCNT;
import com.samsung.android.server.wifi.sns.SnsBigDataSSIV;
import com.samsung.android.server.wifi.sns.SnsBigDataSSVI;
import com.samsung.android.server.wifi.sns.SnsBigDataWFQC;
import com.samsung.android.service.reactive.ReactiveServiceManager;
import com.sec.android.app.C0852CscFeatureTagCommon;
import com.sec.android.app.CscFeatureTagRIL;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiConnectivityMonitor extends StateMachine {
    private static final int ACTIVITY_CHECK_POLL = 135221;
    private static final int ACTIVITY_CHECK_START = 135219;
    private static final int ACTIVITY_CHECK_STOP = 135220;
    private static final int ACTIVITY_RESTART_AGGRESSIVE_MODE = 135222;
    public static final int ANALYTICS_DISCONNECT_REASON_ARP_NO_RESPONSE = 3;
    public static final int ANALYTICS_DISCONNECT_REASON_DNS_DNS_REFUSED = 2;
    public static final int ANALYTICS_DISCONNECT_REASON_DNS_PRIVATE_IP = 1;
    public static final int ANALYTICS_DISCONNECT_REASON_RESERVED = 0;
    public static final long AUTO_NETWORK_SWITCH_TURNED_ON_SCAN_DEFER_DURATION = 12000;
    private static final int BASE = 135168;
    private static final String BIG_DATA_SNS_SCNT_INTENT = "com.samsung.android.server.wifi.SCNT";
    public static final int BRCM_BIGDATA_ECNT_NOINTERNET_ARP = 1;
    public static final int BRCM_BIGDATA_ECNT_NOINTERNET_GENERAL = 0;
    public static final int BRCM_BIGDATA_ECNT_NOINTERNET_TXBAD = 2;
    private static final int BSSID_STAT_CACHE_SIZE = 20;
    private static final int BSSID_STAT_EMPTY_COUNT = 3;
    private static final int BSSID_STAT_RANGE_HIGH_DBM = -45;
    private static final int BSSID_STAT_RANGE_LOW_DBM = -105;
    static final int CAPTIVE_PORTAL_DETECTED = 135471;
    public static final int CHECK_ALTERNATIVE_NETWORKS = 135288;
    private static final String CHN_PUBLIC_DNS_IP = "114.114.114.114";
    static final int CMD_CHANGE_WIFI_ICON_VISIBILITY = 135476;
    static final int CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL = 135481;
    public static final int CMD_CONFIG_SET_CAPTIVE_PORTAL = 135289;
    public static final int CMD_CONFIG_UPDATE_NETWORK_SELECTION = 135290;
    private static final int CMD_DELAY_NETSTATS_SESSION_INIT = 135225;
    static final int CMD_ELE_BY_GEO_DETECTED = 135284;
    static final int CMD_ELE_DETECTED = 135283;
    public static final int CMD_IWC_ACTIVITY_CHECK_POLL = 135376;
    public static final int CMD_IWC_CURRENT_QAI = 135368;
    public static final int CMD_IWC_ELE_DETECTED = 135374;
    public static final int CMD_IWC_QC_RESULT = 135372;
    private static final int CMD_IWC_QC_RESULT_TIMEOUT = 135375;
    public static final int CMD_IWC_REQUEST_INTERNET_CHECK = 135371;
    public static final int CMD_IWC_REQUEST_NETWORK_SWITCH_TO_MOBILE = 135369;
    public static final int CMD_IWC_REQUEST_NETWORK_SWITCH_TO_WIFI = 135370;
    public static final int CMD_IWC_RSSI_FETCH_RESULT = 135373;
    static final int CMD_LINK_GOOD_ENTERED = 135398;
    static final int CMD_LINK_POOR_ENTERED = 135399;
    static final int CMD_NETWORK_PROPERTIES_UPDATED = 135478;
    static final int CMD_PROVISIONING_FAIL = 135401;
    static final int CMD_QUALITY_CHECK_BY_SCORE = 135388;
    static final int CMD_REACHABILITY_LOST = 135400;
    static final int CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE = 135390;
    static final int CMD_RECOVERY_TO_HIGH_QUALITY_FROM_LOW_LINK = 135389;
    static final int CMD_ROAM_START_COMPLETE = 135477;
    private static final int CMD_RSSI_FETCH = 135188;
    static final int CMD_TRAFFIC_POLL = 135193;
    private static final int CMD_TRANSIT_ON_SWITCHABLE = 135300;
    private static final int CMD_TRANSIT_ON_VALID = 135299;
    static final int CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT = 135489;
    static final int CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT_TYPE = 135490;
    static final int CMD_UPDATE_CURRENT_BSSID_ON_QC_RESULT = 135491;
    static final int CMD_UPDATE_CURRENT_BSSID_ON_THROUGHPUT_UPDATE = 135488;
    private static final String CONFIG_SECURE_SVC_INTEGRATION = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSECURESVCINTEGRATION);
    private static final int CONNECTIVITY_VALIDATION_BLOCK = 135205;
    private static final int CONNECTIVITY_VALIDATION_RESULT = 135206;
    private static final String CSC_VENDOR_NOTI_STYLE = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGWIFINOTIFICATIONSTYLE);
    /* access modifiers changed from: private */
    public static boolean DBG = Debug.semIsProductDev();
    private static final int DNS_CHECK_RESULT_DISCONNECTED = 9;
    private static final int DNS_CHECK_RESULT_NO_INTERNET = 1;
    private static final int DNS_CHECK_RESULT_NULLPOINTEREXCEPTION = 8;
    private static final int DNS_CHECK_RESULT_PRIVATE_IP = 2;
    private static final int DNS_CHECK_RESULT_PROXY_IGNORE = 11;
    private static final int DNS_CHECK_RESULT_REMAINED = 10;
    private static final int DNS_CHECK_RESULT_SECURITYEXCEPTION = 7;
    private static final int DNS_CHECK_RESULT_SUCCESS = 0;
    private static final int DNS_CHECK_RESULT_TIMEOUT = 3;
    private static final int DNS_CHECK_RESULT_TIMEOUT_ICMP_OK = 4;
    private static final int DNS_CHECK_RESULT_TIMEOUT_RSSI_OK = 5;
    private static final int DNS_CHECK_RESULT_UNKNOWNHOSTEXCEPTION = 6;
    private static final int EVENT_BSSID_CHANGE = 135175;
    private static final int EVENT_CONNECTIVITY_ACTION_CHANGE = 135179;
    private static final int EVENT_DHCP_SESSION_COMPLETE = 135237;
    private static final int EVENT_DHCP_SESSION_STARTED = 135236;
    private static final int EVENT_INET_CONDITION_ACTION = 135245;
    private static final int EVENT_INET_CONDITION_CHANGE = 135180;
    private static final int EVENT_LINK_DETECTION_DISABLED = 135247;
    private static final int EVENT_MOBILE_CONNECTED = 135232;
    private static final int EVENT_NETWORK_PROPERTIES_CHANGED = 135235;
    private static final int EVENT_NETWORK_REMOVED = 135244;
    private static final int EVENT_NETWORK_STATE_CHANGE = 135170;
    private static final int EVENT_PARALLEL_CP_CHECK_RESULT = 135246;
    private static final int EVENT_ROAM_COMPLETE = 135242;
    private static final int EVENT_ROAM_STARTED = 135241;
    private static final int EVENT_ROAM_TIMEOUT = 135249;
    private static final int EVENT_RSSI_CHANGE = 135171;
    private static final int EVENT_SCAN_COMPLETE = 135230;
    private static final int EVENT_SCAN_STARTED = 135229;
    private static final int EVENT_SCAN_TIMEOUT = 135231;
    private static final int EVENT_SCREEN_OFF = 135177;
    private static final int EVENT_SCREEN_ON = 135176;
    private static final int EVENT_SUPPLICANT_STATE_CHANGE = 135172;
    private static final int EVENT_USER_SELECTION = 135264;
    private static final int EVENT_WATCHDOG_SETTINGS_CHANGE = 135174;
    private static final int EVENT_WATCHDOG_TOGGLED = 135169;
    private static final int EVENT_WIFI_ICON_VSB = 135265;
    private static final int EVENT_WIFI_RADIO_STATE_CHANGE = 135173;
    private static final int EVENT_WIFI_TOGGLED = 135243;
    private static final double EXP_COEFFICIENT_MONITOR = 0.5d;
    private static final double EXP_COEFFICIENT_RECORD = 0.1d;
    static final int GOOD_LINK_DETECTED = 135190;
    private static final double GOOD_LINK_LOSS_THRESHOLD = 0.05d;
    private static final int GOOD_LINK_RSSI_RANGE_MAX = 20;
    private static final int GOOD_LINK_RSSI_RANGE_MIN = 5;
    static final int HANDLE_ON_AVAILABLE = 135470;
    static final int HANDLE_ON_LOST = 135469;
    static final int HANDLE_RESULT_ROAM_IN_HIGH_QUALITY = 135479;
    private static final int HIDE_ICON_HISTORY_COUNT_MAX = 100;
    private static final String IMS_REGISTRATION = "com.samsung.ims.action.IMS_REGISTRATION";
    private static final String INTERFACENAMEOFWLAN = "wlan0";
    static final int INVALIDATED_DETECTED = 135473;
    static final int INVALIDATED_DETECTED_AGAIN = 135475;
    private static final boolean INVALID_ICON_INVISIBILE = true;
    private static final int INVALID_OVERCOME_COUNT = 8;
    static final int LINK_DETECTION_DISABLED = 135191;
    private static final long LINK_MONITORING_SAMPLING_INTERVAL_MS = 500;
    /* access modifiers changed from: private */
    public static long LINK_SAMPLING_INTERVAL_MS = 1000;
    public static final int LINK_STATUS_EXTRA_INFO_NONE = 0;
    public static final int LINK_STATUS_EXTRA_INFO_NO_INTERNET = 2;
    public static final int LINK_STATUS_EXTRA_INFO_POOR_LINK = 1;
    private static final int MAX_DNS_RESULTS_COUNT = 50;
    private static final int MAX_PACKET_RECORDS = 500;
    private static final int MAX_SCAN_COUNTRY_CODE_LOG_COUNT = 10;
    private static final int MAX_TIME_AVOID_LIMIT = 10;
    private static final int MESSAGE_NOT_TRIGGERED_FROM_WCM = 11111;
    private static final long MIN_MAINTAIN_CHINA_TIME = 86400000;
    private static final int MIN_SAMPLE_SIZE_TO_UPDATE_SCAN_COUNTRY_CODE = 5;
    private static final int MODE_AGG = 3;
    private static final int MODE_INVALID_NONSWITCHABLE = 1;
    private static final int MODE_INVALID_SWITCHABLE = 2;
    private static final int MODE_LOW_QUALITY = 3;
    private static final int MODE_NON_SWITCHABLE = 1;
    private static final int MODE_NORMAL = 2;
    private static final int MODE_NO_CHECK = 0;
    private static final int MODE_VALID = 0;
    private static final int MONITORING_TIMEOUT = 30;
    static final int NEED_FETCH_RSSI_AND_LINKSPEED = 135192;
    private static final String NETWISE_CONTENT_URL = "content://com.smithmicro.netwise.director.comcast.oem.apiprovider/managed_networks";
    private static final int NETWORK_STAT_CHECK_DNS = 135223;
    private static int NETWORK_STAT_HISTORY_COUNT_MAX = 10;
    private static final int NETWORK_STAT_SET_GOOD_RX_STATE_NOW = 135224;
    public static final int NUM_LOG_RECS_DBG = 1000;
    public static final int NUM_LOG_RECS_NORMAL = 500;
    static final int POOR_LINK_DETECTED = 135189;
    private static final double POOR_LINK_LOSS_THRESHOLD = 0.5d;
    /* access modifiers changed from: private */
    public static final double POOR_LINK_MIN_VOLUME = ((((double) LINK_SAMPLING_INTERVAL_MS) * 2.0d) / 1000.0d);
    private static final int POOR_LINK_SAMPLE_COUNT = 3;
    private static final int QC_FAIL_DNS_CHECK_FAIL = 3;
    private static final int QC_FAIL_DNS_NO_DNS_LIST = 2;
    private static final int QC_FAIL_DNS_PRIVATE_IP = 1;
    private static final int QC_FAIL_DNS_TIMEOUT = 4;
    private static final int QC_FAIL_ELE_BY_GEO_DETECTED = 22;
    private static final int QC_FAIL_ELE_DETECTED = 21;
    private static final int QC_FAIL_FAST_DISCONNECTION = 14;
    private static final int QC_FAIL_FAST_DISCONNECTION_ROAM = 15;
    private static final int QC_FAIL_INVALIDATED_DETECTED = 17;
    private static final int QC_FAIL_POOR_LINK_LOSS = 12;
    private static final int QC_FAIL_POOR_LINK_LOSS_ROAM = 13;
    private static final int QC_FAIL_PROVISIONING_FAIL = 16;
    private static final int QC_FAIL_ROAM_FAIL_HIGH_QUALITY = 18;
    private static final int QC_FAIL_TRAFFIC_HIGH_LOSS = 11;
    private static final int QC_HISTORY_COUNT_MAX = 30;
    private static final int QC_RESET_204_CHECK_INTERVAL = 135208;
    static final int QC_RESULT_NOT_RECEIVED = 135480;
    private static final int QC_STEP_FIRST_QC_AT_CONNECTION = 3;
    private static final int QC_STEP_GOOGLE_DNS = 1;
    private static final int QC_STEP_RSSI_FETCH = 4;
    private static final int QC_STEP_VALIDATION_CHECK = 5;
    private static final int QC_TRIGGER_AGG_CONTINUOUS_LOSS_VALID = 23;
    private static final int QC_TRIGGER_AGG_POOR_RX = 38;
    private static final int QC_TRIGGER_AT_EVENT_DHCP_COMPLETE = 53;
    private static final int QC_TRIGGER_AT_EVENT_ROAM_COMPLETE = 52;
    private static final int QC_TRIGGER_AT_NETWORK_PROPERTIES_UPDATE = 51;
    private static final int QC_TRIGGER_BY_RECOVERY_FROM_ELE = 17;
    private static final int QC_TRIGGER_BY_SCORE_INVALID = 16;
    private static final int QC_TRIGGER_BY_SCORE_VALID = 27;
    private static final int QC_TRIGGER_CONTINUOUS_POOR_RX = 36;
    private static final int QC_TRIGGER_DNS_ABNORMAL_RESPONSE = 32;
    private static final int QC_TRIGGER_ELE_CHECK = 61;
    private static final int QC_TRIGGER_FAST_DISCONNECTION = 18;
    private static final int QC_TRIGGER_FIRST_QC_AT_CONNECTION = 1;
    private static final int QC_TRIGGER_GOOD_RSSI_INVALID = 12;
    private static final int QC_TRIGGER_IWC_REQUEST_INTERNET_CHECK = 2;
    private static final int QC_TRIGGER_LOSS_VALID = 24;
    private static final int QC_TRIGGER_LOSS_WEAK_SIGNAL_VALID = 25;
    private static final int QC_TRIGGER_LOSS_WEAK_SIGNAL_VALID_ROAM = 26;
    private static final int QC_TRIGGER_MAX_AVOID_TIME_INVALID = 13;
    private static final int QC_TRIGGER_NO_RX_DURING_STREAMING = 35;
    private static final int QC_TRIGGER_NO_SYNACK = 34;
    private static final int QC_TRIGGER_PERIODIC_DNS_CHECK_FAIL = 44;
    private static final int QC_TRIGGER_PROVISIONING_FAIL = 3;
    private static final int QC_TRIGGER_PULL_OUT_LINE = 31;
    private static final int QC_TRIGGER_REACHABILITY_LOST = 4;
    private static final int QC_TRIGGER_RECOVER_TO_VALID = 19;
    private static final int QC_TRIGGER_RESET_WATCHDOG = 5;
    private static final int QC_TRIGGER_RSSI_LEVEL0_VALID = 21;
    private static final int QC_TRIGGER_RX_VISIBLE_INVALID = 11;
    private static final int QC_TRIGGER_SCREEN_ON_GOOD_RSSI = 28;
    private static final int QC_TRIGGER_SING_DNS_CHECK_FAILURE = 45;
    private static final int QC_TRIGGER_STAYING_LAST_POOR_RSSI_VALID = 22;
    private static final int QC_TRIGGER_STAYING_LOW_MCS_DURING_STREAMING = 40;
    private static final int QC_TRIGGER_STOPPED_CONTINUOUS_STREAMING = 39;
    private static final int QC_TRIGGER_SUSPICIOUS_NO_RX_STATE = 33;
    private static final int QC_TRIGGER_SUSPICIOUS_POOR_RX_STATE = 37;
    private static final int QC_TRIGGER_TCP_BACKHAUL_DETECTION = 128;
    private static final int QC_TRIGGER_VALIDATION_CHECK_FORCE = 20;
    private static final int REPORT_NETWORK_CONNECTIVITY = 135204;
    private static final int REPORT_QC_RESULT = 135203;
    private static final int RESULT_DNS_CHECK = 135202;
    private static final int ROAM_TIMEOUT = 30000;
    private static final int RSSI_FETCH_DISCONNECTED_MODE = 2;
    private static final int RSSI_FETCH_GOOD_LINK_DETECT_MODE = 0;
    private static final int RSSI_FETCH_POOR_LINK_DETECT_MODE = 1;
    private static final int RSSI_PATCH_HISTORY_COUNT_MAX = 18000;
    private static final int SCAN_TIMEOUT = 5000;
    private static final int SCORE_QUALITY_CHECK_STATE_NONE = 0;
    private static final int SCORE_QUALITY_CHECK_STATE_POOR_CHECK = 3;
    private static final int SCORE_QUALITY_CHECK_STATE_POOR_MONITOR = 2;
    private static final int SCORE_QUALITY_CHECK_STATE_RECOVERY = 4;
    private static final int SCORE_QUALITY_CHECK_STATE_VALID_CHECK = 1;
    private static final int SCORE_TXBAD_RATIO_THRESHOLD = 15;
    static final int SECURITY_EAP = 3;
    static final int SECURITY_NONE = 0;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_SAE = 4;
    static final int SECURITY_WEP = 1;
    /* access modifiers changed from: private */
    public static boolean SMARTCM_DBG = false;
    public static final int SNS_FW_VERSION = 1;
    public static int SNS_VERSION = 1;
    static final int STOP_BLINKING_WIFI_ICON = 135468;
    private static final boolean SUPPORT_WPA3_SAE = (!"0".equals("2"));
    private static final String TAG = "WifiConnectivityMonitor";
    private static final int TCP_BACKHAUL_DETECTION_START = 135226;
    /* access modifiers changed from: private */
    public static int TCP_STAT_LOGGING_FIRST = 1;
    /* access modifiers changed from: private */
    public static int TCP_STAT_LOGGING_RESET = 0;
    /* access modifiers changed from: private */
    public static int TCP_STAT_LOGGING_SECOND = 2;
    private static final int TRAFFIC_POLL_HISTORY_COUNT_MAX = 3000;
    static final int VALIDATED_DETECTED = 135472;
    static final int VALIDATED_DETECTED_AGAIN = 135474;
    private static int VALIDATION_CHECK_COUNT = 32;
    private static final int VALIDATION_CHECK_FORCE = 135207;
    /* access modifiers changed from: private */
    public static int VALIDATION_CHECK_MAX_COUNT = 4;
    /* access modifiers changed from: private */
    public static int VALIDATION_CHECK_TIMEOUT = WifiConnectivityManager.BSSID_BLACKLIST_EXPIRE_TIME_MS;
    private static final int VERSION = 3;
    private static Object lock = new Object();
    private static long mBigDataQualityCheckCycle = 86400000;
    /* access modifiers changed from: private */
    public static int mCPCheckTriggeredByRoam = 0;
    /* access modifiers changed from: private */
    public static boolean mCurrentApDefault = false;
    /* access modifiers changed from: private */
    public static Object mCurrentBssidLock = new Object();
    /* access modifiers changed from: private */
    public static boolean mInitialResultSentToSystemUi = false;
    private static boolean mIsComcastWifiSupported = SemCscFeature.getInstance().getBoolean(C0852CscFeatureTagCommon.TAG_CSCFEATURE_COMMON_SUPPORTCOMCASTWIFI);
    /* access modifiers changed from: private */
    public static boolean mIsECNTReportedConnection = false;
    /* access modifiers changed from: private */
    public static boolean mIsNoCheck = false;
    /* access modifiers changed from: private */
    public static boolean mIsPassedLevel1State = false;
    private static boolean mIsValidState = false;
    /* access modifiers changed from: private */
    public static int mLinkDetectMode = 2;
    private static int mMaxBigDataQualityCheckLogging = 10;
    /* access modifiers changed from: private */
    public static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    public static long mPoorNetworkAvoidanceEnabledTime = 0;
    private static double mRawRssiEMA = -200.0d;
    /* access modifiers changed from: private */
    public static boolean mUserSelectionConfirmed = false;
    private static final WifiInfo sDummyWifiInfo = new WifiInfo();
    private static final ConcurrentHashMap<String, LocalLog> sPktLogsWlan = new ConcurrentHashMap<>();
    /* access modifiers changed from: private */
    public static double[] sPresetLoss;
    /* access modifiers changed from: private */
    public static boolean sWifiOnly = false;
    private final long ACTIVE_THROUGHPUT_THRESHOLD = 500000;
    /* access modifiers changed from: private */
    public int BSSID_DNS_RESULT_NO_INTERNET = 2;
    /* access modifiers changed from: private */
    public int BSSID_DNS_RESULT_POOR_CONNECTION = 1;
    /* access modifiers changed from: private */
    public int BSSID_DNS_RESULT_SUCCESS = 0;
    /* access modifiers changed from: private */
    public int BSSID_DNS_RESULT_UNKNOWN = -1;
    private final int BUCKET_END = -20;
    private final int BUCKET_START = -90;
    private final int BUCKET_WIDTH = 10;
    private final boolean CSC_WIFI_SUPPORT_VZW_EAP_AKA = OpBrandingLoader.getInstance().isSupportEapAka();
    /* access modifiers changed from: private */
    public final int[] INDEX_TO_SCORE = {0, 5, 10, 20, 30};
    /* access modifiers changed from: private */
    public final String[] INDEX_TO_STRING = {SystemInfo.UNKNOWN_INFO, "Slow", "Okay", "Fast", "Very Fast"};
    /* access modifiers changed from: private */
    public final String[] INDEX_TO_STRING_SHORT = {"UN", "SL", "OK", "FA", "VF"};
    private final int INITIAL_VALIDATION_TIME_OUT = 7000;
    private final int LEVEL_VALUE_MAX = 3;
    final int MAX_NETSTATTHREAD = 10;
    private final int MAX_THROUGHPUT_DECAY_RATE = 200000;
    private final int NM_QUALITY_CHECK_TIME_OUT = 20000;
    private final String OPEN_NETWORK_QOS_SHARING_VERSION = "1.72";
    public int QUALITY_INDEX_FAST = 3;
    public int QUALITY_INDEX_OKAY = 2;
    public int QUALITY_INDEX_SLOW = 1;
    public int QUALITY_INDEX_UNKNOWN = 0;
    public int QUALITY_INDEX_VERY_FAST = 4;
    private final long QUALITY_MIN_ACTIVE_TIME = 30000;
    private final long QUALITY_MIN_DWELL_TIME = 60000;
    private final long QUALITY_MIN_RX_TOTAL_BYTES = 1000000;
    private final long QUALITY_MIN_TX_TOTAL_PACKETS = 1500;
    private final int TOAST_INTERVAL = 30;
    private final int WEIGHT_ACTIVE_TPUT = 34;
    private final int WEIGHT_MAX_TPUT = 33;
    private final int WEIGHT_PER = 33;
    /* access modifiers changed from: private */
    public boolean bSetQcResult = false;
    private int incrDnsResults = 0;
    private int incrScanResult = 0;
    private boolean isNetstatLoggingHangs = false;
    private String isQCExceptionSummary = "";
    private boolean m407ResponseReceived = false;
    /* access modifiers changed from: private */
    public ActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public boolean mAggressiveModeEnabled;
    /* access modifiers changed from: private */
    public boolean mAirPlaneMode = false;
    private final AlarmManager mAlarmMgr;
    /* access modifiers changed from: private */
    public int mAnalyticsDisconnectReason = 0;
    /* access modifiers changed from: private */
    public String mApOui = null;
    private int mBigDataQualityCheckLoggingCount = 0;
    private long mBigDataQualityCheckStartOfCycle = -1;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public LruCache<String, BssidStatistics> mBssidCache = new LruCache<>(20);
    /* access modifiers changed from: private */
    public CaptivePortalState mCaptivePortalState = new CaptivePortalState();
    /* access modifiers changed from: private */
    public boolean mCheckRoamedNetwork = false;
    /* access modifiers changed from: private */
    public final ClientModeImpl mClientModeImpl;
    private boolean mCmccAlertSupport = false;
    /* access modifiers changed from: private */
    public ConnectedState mConnectedState = new ConnectedState();
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager = null;
    private LocalLog mConnectivityPacketLogForWlan0;
    private ContentResolver mContentResolver = null;
    /* access modifiers changed from: private */
    public Context mContext;
    private String mCountryCodeFromScanResult = null;
    /* access modifiers changed from: private */
    public String mCountryIso = null;
    /* access modifiers changed from: private */
    public BssidStatistics mCurrentBssid;
    private int mCurrentLinkSpeed;
    /* access modifiers changed from: private */
    public VolumeWeightedEMA mCurrentLoss = null;
    /* access modifiers changed from: private */
    public int mCurrentMode = 1;
    int mCurrentNetstatThread = 0;
    /* access modifiers changed from: private */
    public QcFailHistory mCurrentQcFail = new QcFailHistory();
    private int mCurrentRssi;
    /* access modifiers changed from: private */
    public int mCurrentSignalLevel;
    private boolean mDataRoamingSetting = false;
    private boolean mDataRoamingState = false;
    private DefaultState mDefaultState = new DefaultState();
    private String mDeviceCountryCode = null;
    /* access modifiers changed from: private */
    public long mDnsThreadID = 0;
    private int mEleGoodScoreCnt = 0;
    /* access modifiers changed from: private */
    public final BssidStatistics mEmptyBssid = new BssidStatistics("00:00:00:00:00:00", -1);
    private EvaluatedState mEvaluatedState = new EvaluatedState();
    private String mFirstLogged = "";
    private String mFirstTCPLoggedTime = "";
    /* access modifiers changed from: private */
    public long mFrontAppAppearedTime;
    private int mGoodScoreCount = 0;
    private int mGoodScoreTotal = 0;
    /* access modifiers changed from: private */
    public int mGoodTargetCount;
    private String[] mHideIconHistory = new String[100];
    private int mHideIconHistoryHead = 0;
    private int mHideIconHistoryTotal = 0;
    /* access modifiers changed from: private */
    public AsyncChannel mIWCChannel = null;
    /* access modifiers changed from: private */
    public boolean mImsRegistered = false;
    /* access modifiers changed from: private */
    public boolean mInAggGoodStateNow = false;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public long mInvalidStartTime = 0;
    /* access modifiers changed from: private */
    public InvalidState mInvalidState = new InvalidState();
    /* access modifiers changed from: private */
    public boolean mInvalidStateTesting = false;
    /* access modifiers changed from: private */
    public int mInvalidatedTime = 0;
    /* access modifiers changed from: private */
    public QcFailHistory mInvalidationFailHistory = new QcFailHistory();
    /* access modifiers changed from: private */
    public int mInvalidationRssi;
    /* access modifiers changed from: private */
    public boolean mIs204CheckInterval = false;
    private boolean mIsFmcNetwork = false;
    private boolean mIsIcmpPingWaiting = false;
    /* access modifiers changed from: private */
    public boolean mIsInDhcpSession = false;
    /* access modifiers changed from: private */
    public boolean mIsInRoamSession = false;
    /* access modifiers changed from: private */
    public boolean mIsMobileActiveNetwork = false;
    /* access modifiers changed from: private */
    public boolean mIsRoamingNetwork = false;
    /* access modifiers changed from: private */
    public boolean mIsScanning = false;
    /* access modifiers changed from: private */
    public boolean mIsScreenOn = true;
    /* access modifiers changed from: private */
    public boolean mIsUsingProxy = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiEnabled;
    /* access modifiers changed from: private */
    public int mIwcCurrentQai = -1;
    /* access modifiers changed from: private */
    public int mIwcRequestQcId = -1;
    private int mLastCheckedMobileHotspotNetworkId = -1;
    private boolean mLastCheckedMobileHotspotValue = false;
    private long mLastChinaConfirmedTime = 0;
    private int mLastGoodScore = 1000;
    private String mLastLogged = "";
    private int mLastPoorScore = 100;
    private String mLastTCPLoggedTime = "";
    /* access modifiers changed from: private */
    public int mLastTxBad;
    /* access modifiers changed from: private */
    public int mLastTxGood;
    int mLastVisibilityOfWifiIcon = 0;
    private int mLatestIcmpPingRtt = -1;
    /* access modifiers changed from: private */
    public Level1State mLevel1State = new Level1State();
    /* access modifiers changed from: private */
    public Level2State mLevel2State = new Level2State();
    /* access modifiers changed from: private */
    public boolean mLevel2StateTransitionPending = false;
    /* access modifiers changed from: private */
    public int mLinkLossOccurred = 0;
    /* access modifiers changed from: private */
    public LinkProperties mLinkProperties;
    /* access modifiers changed from: private */
    public int mLossHasGone = 0;
    /* access modifiers changed from: private */
    public int mLossSampleCount;
    /* access modifiers changed from: private */
    public QcFailHistory mLowQualityFailHistory = new QcFailHistory();
    /* access modifiers changed from: private */
    public int mMaxAvoidCount = 0;
    private boolean mMobilePolicyDataEnable = true;
    /* access modifiers changed from: private */
    public boolean mMptcpEnabled = false;
    int mNetstatBufferCount = 0;
    ArrayList<String> mNetstatTestBuffer = new ArrayList<>();
    boolean[] mNetstatThreadInUse;
    private ExecutorService mNetstatThreadPool = null;
    /* access modifiers changed from: private */
    public Network mNetwork;
    /* access modifiers changed from: private */
    public NetworkCallbackController mNetworkCallbackController = null;
    private String[] mNetworkStatHistory;
    private int mNetworkStatHistoryIndex;
    /* access modifiers changed from: private */
    public boolean mNetworkStatHistoryUpdate;
    /* access modifiers changed from: private */
    public NetworkStatsAnalyzer mNetworkStatsAnalyzer;
    /* access modifiers changed from: private */
    public NotConnectedState mNotConnectedState = new NotConnectedState();
    private WifiConnectivityNotificationManager mNotifier;
    /* access modifiers changed from: private */
    public int mNumOfToggled;
    private ArrayList<OpenNetworkQosCallback> mOpenNetworkQosCallbackList = null;
    /* access modifiers changed from: private */
    public int mOvercomingCount = 0;
    private WcmConnectivityPacketTracker mPacketTrackerForWlan0;
    /* access modifiers changed from: private */
    public ParameterManager mParam;
    private boolean mPoorCheckInProgress = false;
    /* access modifiers changed from: private */
    public int mPoorConnectionDisconnectedNetId = -1;
    private String mPoorNetworkAvoidanceSummary = null;
    /* access modifiers changed from: private */
    public boolean mPoorNetworkDetectionEnabled;
    private String mPoorNetworkDetectionSummary = null;
    /* access modifiers changed from: private */
    public long mPrevAppAppearedTime;
    /* access modifiers changed from: private */
    public double mPreviousLoss = POOR_LINK_MIN_VOLUME;
    /* access modifiers changed from: private */
    public String mPreviousPackageName = "default";
    private int[] mPreviousScore = new int[3];
    private long mPreviousTxBad = 0;
    private long mPreviousTxBadTxGoodRatio = 0;
    private long mPreviousTxSuccess = 0;
    /* access modifiers changed from: private */
    public int mPreviousUid = -1;
    private int mPrevoiusScoreAverage = 0;
    /* access modifiers changed from: private */
    public String mProxyAddress = null;
    /* access modifiers changed from: private */
    public int mProxyPort = -1;
    private String[] mQcDumpHistory = new String[30];
    private String mQcDumpVer = "2.1";
    private QcFailHistory[] mQcHistory = new QcFailHistory[30];
    private int mQcHistoryHead = -1;
    private int mQcHistoryTotal = 0;
    LinkedList<Long> mRawRssi = new LinkedList<>();
    /* access modifiers changed from: private */
    public boolean mReportedPoorNetworkDetectionEnabled = false;
    /* access modifiers changed from: private */
    public int mReportedQai = -1;
    /* access modifiers changed from: private */
    public int mRssiFetchToken = 0;
    private String[] mRssiPatchHistory = new String[RSSI_PATCH_HISTORY_COUNT_MAX];
    private int mRssiPatchHistoryHead = -1;
    private int mRssiPatchHistoryTotal = 0;
    /* access modifiers changed from: private */
    public long mRxPkts;
    /* access modifiers changed from: private */
    public long mSamplingIntervalMS = LINK_SAMPLING_INTERVAL_MS;
    /* access modifiers changed from: private */
    public List<ScanResult> mScanResults = new ArrayList();
    /* access modifiers changed from: private */
    public final Object mScanResultsLock = new Object();
    private int mScoreIntervalCnt = 0;
    /* access modifiers changed from: private */
    public int mScoreQualityCheckMode = 0;
    private boolean mScoreSkipModeEnalbed = false;
    private boolean mScoreSkipPolling = false;
    private SnsBigDataManager mSnsBigDataManager;
    /* access modifiers changed from: private */
    public SnsBigDataSCNT mSnsBigDataSCNT;
    private INetworkStatsService mStatsService = null;
    private INetworkStatsSession mStatsSession = null;
    /* access modifiers changed from: private */
    public int mStayingPoorRssi = 0;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager = null;
    private String mTempLogged = "";
    private String mTempTCPLoggedTime = "";
    private String[] mTrafficPollHistory = new String[3000];
    private int mTrafficPollHistoryHead = -1;
    private int mTrafficPollHistoryTotal = 0;
    private String mTrafficPollPackageName = "";
    /* access modifiers changed from: private */
    public int mTrafficPollToken = 0;
    /* access modifiers changed from: private */
    public boolean mTransitionPendingByIwc = false;
    /* access modifiers changed from: private */
    public int mTransitionScore = 0;
    /* access modifiers changed from: private */
    public long mTxPkts;
    /* access modifiers changed from: private */
    public boolean mUIEnabled;
    /* access modifiers changed from: private */
    public boolean mUsagePackageChanged = false;
    private IUsageStatsManager mUsageStatsManager;
    /* access modifiers changed from: private */
    public String mUsageStatsPackageName;
    /* access modifiers changed from: private */
    public int mUsageStatsUid;
    private final IUsageStatsWatcher.Stub mUsageStatsWatcher = new IUsageStatsWatcher.Stub() {
        public void noteResumeComponent(ComponentName resumeComponentName, Intent intent) {
            if (resumeComponentName == null) {
                try {
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "resumeComponentName is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String packagName = resumeComponentName.getPackageName();
                if (!WifiConnectivityMonitor.this.mUsageStatsPackageName.equals(packagName)) {
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "IUsageStatsWatcher noteResumeComponent: " + packagName);
                    }
                    int unused = WifiConnectivityMonitor.this.mPreviousUid = WifiConnectivityMonitor.this.mUsageStatsUid;
                    String unused2 = WifiConnectivityMonitor.this.mPreviousPackageName = WifiConnectivityMonitor.this.mUsageStatsPackageName;
                    long unused3 = WifiConnectivityMonitor.this.mPrevAppAppearedTime = WifiConnectivityMonitor.this.mFrontAppAppearedTime;
                    int unused4 = WifiConnectivityMonitor.this.mUsageStatsUid = WifiConnectivityMonitor.this.mContext.getPackageManager().getApplicationInfo(packagName, 128).uid;
                    String unused5 = WifiConnectivityMonitor.this.mUsageStatsPackageName = packagName;
                    boolean unused6 = WifiConnectivityMonitor.this.mUsagePackageChanged = true;
                    long unused7 = WifiConnectivityMonitor.this.mFrontAppAppearedTime = System.currentTimeMillis();
                } else if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.d(WifiConnectivityMonitor.TAG, "resumeComponentName same package: " + WifiConnectivityMonitor.this.mUsageStatsPackageName);
                }
            }
        }

        public void notePauseComponent(ComponentName pauseComponentName) {
        }

        public void noteStopComponent(ComponentName stopComponentName) {
        }
    };
    /* access modifiers changed from: private */
    public boolean mUserOwner = true;
    /* access modifiers changed from: private */
    public ValidNoCheckState mValidNoCheckState = new ValidNoCheckState();
    /* access modifiers changed from: private */
    public ValidNonSwitchableState mValidNonSwitchableState = new ValidNonSwitchableState();
    /* access modifiers changed from: private */
    public long mValidStartTime = 0;
    /* access modifiers changed from: private */
    public ValidState mValidState = new ValidState();
    /* access modifiers changed from: private */
    public ValidSwitchableState mValidSwitchableState = new ValidSwitchableState();
    /* access modifiers changed from: private */
    public int mValidatedTime = 0;
    private boolean mValidationBlock = false;
    /* access modifiers changed from: private */
    public int mValidationCheckCount = 0;
    /* access modifiers changed from: private */
    public long mValidationCheckEnabledTime = 0;
    /* access modifiers changed from: private */
    public boolean mValidationCheckMode = false;
    /* access modifiers changed from: private */
    public int mValidationCheckTime = 32;
    /* access modifiers changed from: private */
    public int mValidationResultCount = 0;
    Message mWCMQCResult = null;
    private ClientModeImpl.WifiClientModeChannel mWifiClientModeChannel;
    private final WifiConfigManager mWifiConfigManager;
    /* access modifiers changed from: private */
    public WifiEleStateTracker mWifiEleStateTracker = null;
    /* access modifiers changed from: private */
    public WifiInfo mWifiInfo;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private final WifiNative mWifiNative;
    /* access modifiers changed from: private */
    public boolean mWifiNeedRecoveryFromEle = false;
    /* access modifiers changed from: private */
    public WifiSwitchForIndividualAppsService mWifiSwitchForIndividualAppsService;
    private WifiTransportLayerMonitor mWifiTransportLayerMonitor;
    private String[] summaryCountryCodeFromScanResults = new String[10];
    private String[] summaryDnsResults = new String[50];
    /* access modifiers changed from: private */
    public int toastCount = 0;

    static /* synthetic */ int access$10208(WifiConnectivityMonitor x0) {
        int i = x0.mStayingPoorRssi;
        x0.mStayingPoorRssi = i + 1;
        return i;
    }

    static /* synthetic */ int access$10304(WifiConnectivityMonitor x0) {
        int i = x0.mLossSampleCount + 1;
        x0.mLossSampleCount = i;
        return i;
    }

    static /* synthetic */ int access$10404(WifiConnectivityMonitor x0) {
        int i = x0.mOvercomingCount + 1;
        x0.mOvercomingCount = i;
        return i;
    }

    static /* synthetic */ int access$10508(WifiConnectivityMonitor x0) {
        int i = x0.mLinkLossOccurred;
        x0.mLinkLossOccurred = i + 1;
        return i;
    }

    static /* synthetic */ int access$10604(WifiConnectivityMonitor x0) {
        int i = x0.mLossHasGone + 1;
        x0.mLossHasGone = i;
        return i;
    }

    static /* synthetic */ int access$13604(WifiConnectivityMonitor x0) {
        int i = x0.mRssiFetchToken + 1;
        x0.mRssiFetchToken = i;
        return i;
    }

    static /* synthetic */ int access$13608(WifiConnectivityMonitor x0) {
        int i = x0.mRssiFetchToken;
        x0.mRssiFetchToken = i + 1;
        return i;
    }

    static /* synthetic */ int access$16304(WifiConnectivityMonitor x0) {
        int i = x0.mGoodTargetCount + 1;
        x0.mGoodTargetCount = i;
        return i;
    }

    static /* synthetic */ int access$16604(WifiConnectivityMonitor x0) {
        int i = x0.mTrafficPollToken + 1;
        x0.mTrafficPollToken = i;
        return i;
    }

    static /* synthetic */ int access$23408(WifiConnectivityMonitor x0) {
        int i = x0.toastCount;
        x0.toastCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$5508(WifiConnectivityMonitor x0) {
        int i = x0.mValidationResultCount;
        x0.mValidationResultCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$5608(WifiConnectivityMonitor x0) {
        int i = x0.mValidationCheckCount;
        x0.mValidationCheckCount = i + 1;
        return i;
    }

    /* access modifiers changed from: private */
    public ConnectivityManager getCm() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager;
    }

    /* access modifiers changed from: private */
    public ClientModeImpl.WifiClientModeChannel getClientModeChannel() {
        if (this.mWifiClientModeChannel == null) {
            this.mWifiClientModeChannel = this.mClientModeImpl.makeWifiClientModeChannel();
        }
        return this.mWifiClientModeChannel;
    }

    class ParameterManager {
        public final int AggressiveModeHigherPassBytes = 0;
        public final int AggressiveModeMonitorLinkLoss = 2;
        public final int AggressiveModeQCTriggerByRssi = 1;
        public final int DEFAULT_ENHANCED_TARGET_RSSI = 5;
        public final int DEFAULT_GOOD_RX_PACKETS_BASE = 30;
        public final int DEFAULT_MIN_DNS_RESPONSES = 1;
        public final int DEFAULT_MSS = 1430;
        public final int DEFAULT_NO_RX_PACKETS_LIMIT = 2;
        public final int DEFAULT_NUM_DNS_PINGS = 2;
        public final int DEFAULT_PACKET_SIZE = 1484;
        public final int DEFAULT_POOR_RX_PACKETS_LIMIT = 15;
        public final int DEFAULT_QC_TIMEOUT_SEC = 1;
        public final int DEFAULT_RESTORE_TARGET_RSSI_SEC = 30;
        public final int DEFAULT_SINGLE_DNS_PING_TIMEOUT_MS = 10000;
        public final String DEFAULT_URL = "http://www.google.com";
        public final String DEFAULT_URL_CHINA = "http://www.qq.com";
        public String DEFAULT_URL_STRING = "www.google.com";
        public final String DEFAULT_URL_STRING_CHINA = "www.qq.com";
        public final int DNS_INTRATEST_PING_INTERVAL_MS = 0;
        public final int DNS_START_DELAY_MS = 100;
        public final double FD_DISCONNECT_DEVIATION_EMA_THRESHOLD = 4.0d;
        public double FD_DISCONNECT_THRESHOLD = 8.0d;
        public int FD_EMA_ALPHA = 9;
        public int FD_EVALUATE_COUNT = 6;
        public int FD_EVAL_LEAD_TIME = 2;
        public int FD_MA_UNIT = 3;
        public int FD_MA_UNIT_SAMPLE_COUNT = 6;
        public final int FD_RAW_RSSI_SIZE = (this.FD_MA_UNIT_SAMPLE_COUNT + this.FD_EVALUATE_COUNT);
        public int FD_RSSI_LOW_THRESHOLD = -80;
        public final double FD_RSSI_SLOPE_EXP_COEFFICIENT = 0.2d;
        public final String PATH_OF_RESULT = "/data/log/";
        public final int QC_INIT_ID = 1;
        public final int RSSI_THRESHOLD_AGG_MODE_2G = -70;
        public final int RSSI_THRESHOLD_AGG_MODE_5G = -75;
        public final String SMARTCM_VALUE_FILE = "/data/misc/wifi/.smartCM";
        public final int TCP_HEADER_SIZE = 54;
        public final int VERIFYING_STATE_PASS_PACKETS_AGGRESSIVE_MODE = 75;
        public int WEAK_SIGNAL_FREQUENT_QC_CYCLE_SEC = 30;
        public int WEAK_SIGNAL_POOR_DETECTED_RSSI_MIN = -89;
        public final String WLANQCPATH_PROP_NAME = "wlan.qc.path";
        public boolean[] mAggressiveModeFeatureEnabled = {true, true, true};
        public int mGoodRxPacketsBase;
        public long mLastPoorDetectedTime = 0;
        public int mNoRxPacketsLimit;
        public int mPassBytes;
        public int mPassBytesAggressiveMode;
        public int mPoorRxPacketsLimit;
        public int mRssiThresholdAggMode2G;
        public int mRssiThresholdAggMode5G;
        public int mRssiThresholdAggModeCurrentAP;
        public long mWeakSignalQCStartTime = 0;

        public ParameterManager() {
            resetParameters();
        }

        public void resetParameters() {
            boolean unused = WifiConnectivityMonitor.SMARTCM_DBG = false;
            this.mRssiThresholdAggMode2G = -70;
            this.mRssiThresholdAggMode5G = -75;
            this.mNoRxPacketsLimit = 2;
            this.mPoorRxPacketsLimit = 15;
            this.mGoodRxPacketsBase = 30;
            this.mPassBytesAggressiveMode = 111300;
        }

        public void setEvaluationParameters() {
            if (WifiConnectivityMonitor.DBG) {
                setEvaluationParameters(loadSmartCMFile());
            }
        }

        public void setEvaluationParameters(String data) {
            String[] line;
            if (data != null && (line = data.split("\n")) != null) {
                for (String str : line) {
                    if (str != null && str.startsWith("dbg=")) {
                        boolean unused = WifiConnectivityMonitor.SMARTCM_DBG = getStringValue(str, WifiConnectivityMonitor.SMARTCM_DBG ? "1" : "0").equals("1");
                        Log.i(WifiConnectivityMonitor.TAG, "SMARTCM_DBG : " + WifiConnectivityMonitor.SMARTCM_DBG);
                    }
                }
            }
        }

        private String getStringValue(String str, String defalutValue) {
            int idx;
            if (str != null && (idx = str.indexOf("=") + 1) <= str.length()) {
                return str.substring(idx, str.length());
            }
            return defalutValue;
        }

        private int getIntValue(String str, int defaultValue) {
            try {
                return Integer.parseInt(getStringValue(str, String.valueOf(defaultValue)));
            } catch (Exception e) {
                Log.e(WifiConnectivityMonitor.TAG, "wrong int:" + str);
                return defaultValue;
            }
        }

        private long getLongValue(String str, long defaultValue) {
            try {
                return Long.parseLong(getStringValue(str, String.valueOf(defaultValue)));
            } catch (Exception e) {
                Log.e(WifiConnectivityMonitor.TAG, "wrong double:" + str);
                return defaultValue;
            }
        }

        private double getDoubleValue(String str, double defaultValue) {
            try {
                return Double.parseDouble(getStringValue(str, String.valueOf(defaultValue)));
            } catch (Exception e) {
                Log.e(WifiConnectivityMonitor.TAG, "wrong double:" + str);
                return defaultValue;
            }
        }

        private String loadSmartCMFile() {
            FileReader reader = null;
            BufferedReader br = null;
            String data = "";
            try {
                FileReader reader2 = new FileReader("/data/misc/wifi/.smartCM");
                BufferedReader br2 = new BufferedReader(reader2);
                while (true) {
                    String readLine = br2.readLine();
                    String line = readLine;
                    if (readLine != null) {
                        data = data + line + "\n";
                    } else {
                        try {
                            br2.close();
                            reader2.close();
                            return data;
                        } catch (IOException e) {
                            Log.e(WifiConnectivityMonitor.TAG, "IOException closing file");
                            return null;
                        }
                    }
                }
            } catch (Exception e2) {
                Log.d(WifiConnectivityMonitor.TAG, "no file");
                if (br != null) {
                    br.close();
                }
                if (reader == null) {
                    return null;
                }
                reader.close();
                return null;
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        Log.e(WifiConnectivityMonitor.TAG, "IOException closing file");
                        throw th;
                    }
                }
                if (reader != null) {
                    reader.close();
                }
                throw th;
            }
        }

        public int createSmartCMFile(String data) {
            FileWriter out = null;
            int ret = -1;
            try {
                File file = new File("/data/misc/wifi/.smartCM");
                if (file.exists()) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "removed smartCM");
                    }
                    file.delete();
                }
                file.createNewFile();
                FileWriter out2 = new FileWriter("/data/misc/wifi/.smartCM");
                if (data != null) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "created smartCM");
                    }
                    out2.write(data);
                }
                ret = 1;
                try {
                    out2.close();
                } catch (IOException e) {
                    Log.e(WifiConnectivityMonitor.TAG, "IOException closing file");
                }
            } catch (Exception e2) {
                Log.e(WifiConnectivityMonitor.TAG, "Exception creating file");
                if (out != null) {
                    out.close();
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                        Log.e(WifiConnectivityMonitor.TAG, "IOException closing file");
                    }
                }
                throw th;
            }
            return ret;
        }

        public String getAggressiveModeFeatureStatus() {
            return "(" + this.mAggressiveModeFeatureEnabled[0] + "/" + this.mAggressiveModeFeatureEnabled[1] + "/" + this.mAggressiveModeFeatureEnabled[2] + ")";
        }
    }

    private static class QcFailHistory {
        int apIndex = -1;
        boolean avoidance = false;
        String bssid = "";
        int bytes = -1;
        List<InetAddress> currentDnsList = null;
        int dataRate = -1;
        boolean detection = false;
        int error = -1;
        int line = -1;
        String netstat = "";
        int qcStep = -1;
        int qcStepTemp = -1;
        int qcTrigger = -1;
        int qcTriggerTemp = -1;
        int qcType = -1;
        int qcUrlIndex = -1;
        int rssi = -1;
        String ssid = "";
        String state = "";
        Date time;

        QcFailHistory() {
        }
    }

    /* access modifiers changed from: private */
    public void initNetworkStatHistory() {
        this.mNetworkStatHistory = new String[NETWORK_STAT_HISTORY_COUNT_MAX];
        this.mNetworkStatHistoryIndex = 0;
        this.mNetworkStatHistoryUpdate = true;
    }

    /* access modifiers changed from: private */
    public void addNetworkStatHistory(String log) {
        String[] strArr = this.mNetworkStatHistory;
        if (strArr != null) {
            int i = this.mNetworkStatHistoryIndex;
            strArr[i] = log;
            int i2 = i + 1;
            this.mNetworkStatHistoryIndex = i2;
            if (i2 >= NETWORK_STAT_HISTORY_COUNT_MAX) {
                this.mNetworkStatHistoryIndex = 0;
            }
        }
    }

    private String getNetworkStatHistory() {
        if (this.mNetworkStatHistory == null) {
            return null;
        }
        String str = "";
        int idx = this.mNetworkStatHistoryIndex;
        for (int count = 0; count < NETWORK_STAT_HISTORY_COUNT_MAX; count++) {
            if (this.mNetworkStatHistory[idx] != null) {
                str = str + this.mNetworkStatHistory[idx] + "/";
            }
            idx = (idx + 1) % NETWORK_STAT_HISTORY_COUNT_MAX;
        }
        return str;
    }

    /* access modifiers changed from: private */
    public WifiInfo syncGetCurrentWifiInfo() {
        WifiNative.SignalPollResult pollResult;
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            this.mCurrentLinkSpeed = wifiInfo.getLinkSpeed();
            this.mCurrentRssi = wifiInfo.getRssi();
            if (!this.mIsScreenOn && (pollResult = this.mWifiNative.signalPoll(INTERFACENAMEOFWLAN)) != null) {
                wifiInfo.setRssi(pollResult.currentRssi);
            }
            return wifiInfo;
        }
        Log.e(TAG, "WifiInfo is null");
        this.mCurrentLinkSpeed = 0;
        this.mCurrentRssi = -100;
        return sDummyWifiInfo;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    private WifiConnectivityMonitor(Context context, ClientModeImpl clientModeImpl, WifiInjector wifiInjector) {
        super(TAG);
        Context context2 = context;
        ClientModeImpl clientModeImpl2 = clientModeImpl;
        this.mContext = context2;
        this.mContentResolver = context.getContentResolver();
        this.mWifiManager = (WifiManager) context2.getSystemService("wifi");
        this.mClientModeImpl = clientModeImpl2;
        this.mWifiConfigManager = wifiInjector.getWifiConfigManager();
        this.mWifiNative = wifiInjector.getWifiNative();
        this.mNotifier = new WifiConnectivityNotificationManager(this.mContext);
        getClientModeChannel();
        try {
            this.mClientModeImpl.registerWifiNetworkCallbacks(new WCMCallbacks());
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        getCm();
        this.mNetworkCallbackController = new NetworkCallbackController();
        addState(this.mDefaultState);
        addState(this.mNotConnectedState, this.mDefaultState);
        addState(this.mConnectedState, this.mDefaultState);
        addState(this.mCaptivePortalState, this.mConnectedState);
        addState(this.mEvaluatedState, this.mConnectedState);
        addState(this.mInvalidState, this.mEvaluatedState);
        addState(this.mValidState, this.mEvaluatedState);
        addState(this.mValidNonSwitchableState, this.mValidState);
        addState(this.mValidSwitchableState, this.mValidState);
        addState(this.mLevel1State, this.mValidSwitchableState);
        addState(this.mLevel2State, this.mValidSwitchableState);
        addState(this.mValidNoCheckState, this.mValidState);
        setInitialState(this.mNotConnectedState);
        setLogRecSize(DBG ? 1000 : 500);
        updateCountryIsoCode();
        this.mParam = new ParameterManager();
        this.mWifiInfo = sDummyWifiInfo;
        HandlerThread networkStatsThread = new HandlerThread("NetworkStatsThread");
        networkStatsThread.start();
        this.mNetworkStatsAnalyzer = new NetworkStatsAnalyzer(networkStatsThread.getLooper());
        this.mPreviousUid = -1;
        this.mPreviousPackageName = "default";
        this.mSnsBigDataManager = new SnsBigDataManager(this.mContext);
        this.mAlarmMgr = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mSnsBigDataSCNT = (SnsBigDataSCNT) this.mSnsBigDataManager.getBigDataFeature("SSMA");
        PendingIntent alarmIntentForScnt = PendingIntent.getBroadcast(this.mContext, 0, new Intent(BIG_DATA_SNS_SCNT_INTENT), 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, 11);
        calendar.set(12, 55);
        this.mAlarmMgr.setInexactRepeating(1, calendar.getTimeInMillis(), 86400000, alarmIntentForScnt);
        if (!"FINISH".equalsIgnoreCase(SystemProperties.get("persist.sys.setupwizard", "NOTSET"))) {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", -1) == -1) {
                if ("DEFAULT_ON".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSNSSTATUS))) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 1);
                } else {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 0);
                }
            }
            if (!"".equals(CONFIG_SECURE_SVC_INTEGRATION) || SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) || SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEDEFAULTMWIPS)) {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_mwips", 0);
            } else {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_mwips", 1);
            }
        }
        HandlerThread tcpMonitorThread = new HandlerThread("WifiTransportLayerMonitor");
        tcpMonitorThread.start();
        this.mWifiTransportLayerMonitor = new WifiTransportLayerMonitor(tcpMonitorThread.getLooper(), this, clientModeImpl2, context2);
        HandlerThread tcpHandlerThread = new HandlerThread("WifiSwitchForIndividualAppsService");
        tcpHandlerThread.start();
        WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService = r1;
        Calendar calendar2 = calendar;
        WifiSwitchForIndividualAppsService wifiSwitchForIndividualAppsService2 = new WifiSwitchForIndividualAppsService(tcpHandlerThread.getLooper(), clientModeImpl, this.mWifiTransportLayerMonitor, this, context);
        this.mWifiSwitchForIndividualAppsService = wifiSwitchForIndividualAppsService;
        try {
            this.mUsageStatsUid = -1;
            this.mUsageStatsPackageName = "default";
            this.mUsageStatsManager = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
            this.mUsageStatsManager.registerUsageStatsWatcher(this.mUsageStatsWatcher);
        } catch (Exception e3) {
            Log.w(TAG, "Exception occured while register UsageStatWatcher " + e3);
            e3.printStackTrace();
        }
        this.mNumOfToggled = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_num_of_switch_to_mobile_data_toggle", 0);
        this.mLastVisibilityOfWifiIcon = -1;
        Settings.Global.putInt(this.mContentResolver, "check_private_ip_mode", 1);
        this.mCountryCodeFromScanResult = Settings.Global.getString(this.mContentResolver, "wifi_wcm_country_code_from_scan_result");
        Log.d(TAG, "Initial WIFI_WCM_COUNTRY_CODE_FROM_SCAN_RESULT: " + this.mCountryCodeFromScanResult);
    }

    public static WifiConnectivityMonitor makeWifiConnectivityMonitor(Context context, ClientModeImpl clientModeImpl, WifiInjector wifiInjector) {
        ContentResolver contentResolver = context.getContentResolver();
        sWifiOnly = "wifi-only".equalsIgnoreCase(SystemProperties.get("ro.carrier", SystemInfo.UNKNOWN_INFO).trim()) || "yes".equalsIgnoreCase(SystemProperties.get("ro.radio.noril", "no").trim());
        WifiConnectivityMonitor wcm = new WifiConnectivityMonitor(context, clientModeImpl, wifiInjector);
        wcm.start();
        checkVersion(context);
        return wcm;
    }

    /* access modifiers changed from: private */
    public void setupNetworkReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.RSSI_CHANGED")) {
                    WifiConnectivityMonitor.this.obtainMessage(WifiConnectivityMonitor.EVENT_RSSI_CHANGE, intent.getIntExtra("newRssi", -200), 0).sendToTarget();
                } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_NETWORK_STATE_CHANGE, intent);
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_SCREEN_ON);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_SCREEN_OFF);
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WIFI_RADIO_STATE_CHANGE, intent.getIntExtra("wifi_state", 4));
                } else if (action.equals("ACTION_WCM_CONFIGURATION_CHANGED")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                } else if (action.equals("com.android.intent.action.DATAUSAGE_REACH_TO_LIMIT")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                } else if (SemCscFeature.getInstance().getBoolean(CscFeatureTagRIL.TAG_CSCFEATURE_RIL_SHOWDATASELECTPOPUPONBOOTUP) && action.equals("android.intent.action.ACTION_DATA_SELECTION_POPUP_PRESSED")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_CONNECTIVITY_ACTION_CHANGE);
                } else if (action.equals("android.intent.action.SERVICE_STATE") && WifiConnectivityMonitor.this.mTelephonyManager != null) {
                    String networkCountyIso = WifiConnectivityMonitor.this.mTelephonyManager.getNetworkCountryIso();
                    Log.i(WifiConnectivityMonitor.TAG, "ACTION_SERVICE_STATE_CHANGED: " + networkCountyIso);
                    if (networkCountyIso != null && !networkCountyIso.isEmpty() && !networkCountyIso.equals(WifiConnectivityMonitor.this.mCountryIso)) {
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(WifiConnectivityMonitor.TAG, "Network country change is detected.");
                        }
                        WifiConnectivityMonitor.this.updateCountryIsoCode();
                    }
                } else if (action.equals("android.intent.action.USER_BACKGROUND")) {
                    Log.d(WifiConnectivityMonitor.TAG, "OWNER is background");
                    boolean unused = WifiConnectivityMonitor.this.mUserOwner = false;
                    WifiConnectivityMonitor.this.updatePoorNetworkParameters();
                } else if (action.equals("android.intent.action.USER_FOREGROUND")) {
                    Log.d(WifiConnectivityMonitor.TAG, "OWNER is foreground");
                    boolean unused2 = WifiConnectivityMonitor.this.mUserOwner = true;
                    WifiConnectivityMonitor.this.updatePoorNetworkParameters();
                } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                    Log.d(WifiConnectivityMonitor.TAG, "ACTION_BOOT_COMPLETED");
                } else if (action.equals("android.net.conn.INET_CONDITION_ACTION")) {
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_INET_CONDITION_CHANGE, intent);
                } else if (action.equals(WifiConnectivityMonitor.IMS_REGISTRATION)) {
                    boolean prevImsRegistered = WifiConnectivityMonitor.this.mImsRegistered;
                    boolean unused3 = WifiConnectivityMonitor.this.mImsRegistered = intent.getBooleanExtra("VOWIFI", false);
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "IMS_REGISTRATION - " + WifiConnectivityMonitor.this.mImsRegistered);
                    }
                    if (prevImsRegistered != WifiConnectivityMonitor.this.mImsRegistered) {
                        WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                    }
                } else if (action.equals("com.samsung.android.server.wifi.WCM_SCAN_STARTED")) {
                    Log.d(WifiConnectivityMonitor.TAG, "com.samsung.android.server.wifi.WCM_SCAN_STARTED");
                    WifiConnectivityMonitor.this.scanStarted();
                } else if (action.equals("android.net.wifi.LINK_CONFIGURATION_CHANGED")) {
                    LinkProperties lp = (LinkProperties) intent.getParcelableExtra("linkProperties");
                    if (lp != null) {
                        LinkProperties unused4 = WifiConnectivityMonitor.this.mLinkProperties = lp;
                    }
                } else if (action.equals("android.net.wifi.SCAN_RESULTS")) {
                    WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.EVENT_SCAN_TIMEOUT);
                    boolean partialScan = intent.getBooleanExtra("isPartialScan", false);
                    WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor.sendMessage(wifiConnectivityMonitor.obtainMessage(WifiConnectivityMonitor.EVENT_SCAN_COMPLETE, partialScan, 0));
                    WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.EVENT_SCAN_COMPLETE);
                } else if (action.equals(WifiConnectivityMonitor.BIG_DATA_SNS_SCNT_INTENT)) {
                    WifiConnectivityMonitor.this.sendBigDataFeatureForSCNT();
                } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    Log.d(WifiConnectivityMonitor.TAG, "AIRPLANE_MODE_CHANGED");
                    WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mIntentFilter.addAction("ACTION_WCM_CONFIGURATION_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.SERVICE_STATE");
        this.mIntentFilter.addAction("android.intent.action.ANY_DATA_STATE");
        this.mIntentFilter.addAction("com.android.intent.action.DATAUSAGE_REACH_TO_LIMIT");
        this.mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mIntentFilter.addAction("android.intent.action.USER_FOREGROUND");
        this.mIntentFilter.addAction("android.intent.action.USER_BACKGROUND");
        if (SemCscFeature.getInstance().getBoolean(CscFeatureTagRIL.TAG_CSCFEATURE_RIL_SHOWDATASELECTPOPUPONBOOTUP)) {
            this.mIntentFilter.addAction("android.intent.action.ACTION_DATA_SELECTION_POPUP_PRESSED");
        }
        this.mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mIntentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mIntentFilter.addAction(BIG_DATA_SNS_SCNT_INTENT);
        this.mIntentFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
        this.mIntentFilter.addAction(IMS_REGISTRATION);
        this.mIntentFilter.addAction("com.samsung.android.server.wifi.WCM_SCAN_STARTED");
        this.mIntentFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    /* access modifiers changed from: private */
    public void registerForWatchdogToggle() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_on"), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_TOGGLED);
            }
        });
    }

    /* access modifiers changed from: private */
    public void registerForSettingsChanges() {
        ContentObserver contentObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                int i = 0;
                int unused = wifiConnectivityMonitor.mNumOfToggled = Settings.Global.getInt(wifiConnectivityMonitor.mContext.getContentResolver(), "wifi_num_of_switch_to_mobile_data_toggle", 0);
                WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE);
                if (WifiConnectivityMonitor.this.mIWCChannel != null) {
                    AsyncChannel access$1500 = WifiConnectivityMonitor.this.mIWCChannel;
                    int i2 = WifiConnectivityMonitor.this.mUIEnabled ? 1 : 0;
                    if (WifiConnectivityMonitor.this.mAggressiveModeEnabled) {
                        i = 1;
                    }
                    access$1500.sendMessage(IWCMonitor.SNS_SETTINGS_CHANGED, i2, i);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_poor_network_test_enabled"), false, contentObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_poor_network_aggressive_mode_on"), false, contentObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, contentObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("ultra_powersaving_mode"), false, contentObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("data_roaming"), false, contentObserver);
    }

    /* access modifiers changed from: private */
    public void registerForMptcpChange() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("mptcp_value_internal"), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                try {
                    WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                    boolean z = true;
                    if (Settings.System.getInt(WifiConnectivityMonitor.this.mContext.getContentResolver(), "mptcp_value_internal") != 1) {
                        z = false;
                    }
                    boolean unused = wifiConnectivityMonitor.mMptcpEnabled = z;
                    Log.d(WifiConnectivityMonitor.TAG, "MPTCP mode changed, enabled ? = " + WifiConnectivityMonitor.this.mMptcpEnabled);
                } catch (Settings.SettingNotFoundException e) {
                    Log.e(WifiConnectivityMonitor.TAG, "Exception in getting 'MPTCP mode' setting " + e);
                }
            }
        });
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.setupNetworkReceiver();
            WifiConnectivityMonitor.this.registerForSettingsChanges();
            WifiConnectivityMonitor.this.registerForWatchdogToggle();
            WifiConnectivityMonitor.this.registerForMptcpChange();
            WifiConnectivityMonitor.this.updateSettings();
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:101:0x0292, code lost:
            return true;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(android.os.Message r11) {
            /*
                r10 = this;
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.logStateAndMessage(r11, r10)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1 = 0
                r0.setLogOnlyTransitions(r1)
                int r0 = r11.what
                r2 = 0
                java.lang.String r3 = "networkInfo"
                r4 = -1
                java.lang.String r5 = "WifiConnectivityMonitor"
                r6 = 1
                switch(r0) {
                    case 135170: goto L_0x02f3;
                    case 135171: goto L_0x02de;
                    case 135172: goto L_0x02dd;
                    case 135173: goto L_0x02ab;
                    case 135174: goto L_0x02a5;
                    case 135175: goto L_0x02a4;
                    case 135176: goto L_0x029e;
                    case 135177: goto L_0x0293;
                    default: goto L_0x0017;
                }
            L_0x0017:
                switch(r0) {
                    case 135179: goto L_0x0225;
                    case 135180: goto L_0x01ec;
                    default: goto L_0x001a;
                }
            L_0x001a:
                r3 = 135372(0x210cc, float:1.89697E-40)
                switch(r0) {
                    case 135188: goto L_0x01e6;
                    case 135193: goto L_0x01e6;
                    case 135249: goto L_0x01d4;
                    case 135368: goto L_0x018d;
                    case 135371: goto L_0x0162;
                    case 135375: goto L_0x0139;
                    case 135473: goto L_0x0117;
                    case 135478: goto L_0x010a;
                    case 135480: goto L_0x00e7;
                    default: goto L_0x0020;
                }
            L_0x0020:
                switch(r0) {
                    case 135229: goto L_0x00db;
                    case 135230: goto L_0x00a7;
                    case 135231: goto L_0x02a4;
                    case 135232: goto L_0x02a4;
                    default: goto L_0x0023;
                }
            L_0x0023:
                switch(r0) {
                    case 135244: goto L_0x004a;
                    case 135245: goto L_0x02a4;
                    case 135246: goto L_0x003e;
                    case 135247: goto L_0x02a4;
                    default: goto L_0x0026;
                }
            L_0x0026:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "Unhandled message "
                r0.append(r1)
                int r1 = r11.what
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.e(r5, r0)
                goto L_0x0292
            L_0x003e:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x0049
                java.lang.String r0 = "EVENT_PARALLEL_CP_CHECK_RESULT"
                android.util.Log.d(r5, r0)
            L_0x0049:
                return r6
            L_0x004a:
                int r0 = r11.arg1
                if (r0 == r4) goto L_0x00a6
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.util.LruCache r1 = r1.mBssidCache
                java.util.Map r1 = r1.snapshot()
                if (r1 == 0) goto L_0x00a6
                java.util.Set r2 = r1.keySet()
                java.util.Iterator r2 = r2.iterator()
            L_0x0062:
                boolean r3 = r2.hasNext()
                if (r3 == 0) goto L_0x00a6
                java.lang.Object r3 = r2.next()
                java.lang.String r3 = (java.lang.String) r3
                java.lang.Object r4 = r1.get(r3)
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = (com.android.server.wifi.WifiConnectivityMonitor.BssidStatistics) r4
                int r7 = r4.netId
                if (r7 != r0) goto L_0x00a5
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "BssidStatistics removed - "
                r7.append(r8)
                java.lang.String r8 = r4.mBssid
                r9 = 9
                java.lang.String r8 = r8.substring(r9)
                r7.append(r8)
                java.lang.String r7 = r7.toString()
                android.util.Log.d(r5, r7)
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.util.LruCache r7 = r7.mBssidCache
                java.lang.String r8 = r4.mBssid
                r7.remove(r8)
            L_0x00a5:
                goto L_0x0062
            L_0x00a6:
                return r6
            L_0x00a7:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isConnectedState()
                if (r0 != 0) goto L_0x00b4
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mIsScanning = r1
            L_0x00b4:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x00bf
                java.lang.String r0 = "EVENT_SCAN_COMPLETE"
                android.util.Log.d(r5, r0)
            L_0x00bf:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r11.arg1
                if (r2 != r6) goto L_0x00c6
                r1 = r6
            L_0x00c6:
                r0.scanCompleted(r1)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.mIsRoamingNetwork
                if (r0 != 0) goto L_0x00da
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r0.isRoamingNetwork()
                boolean unused = r0.mIsRoamingNetwork = r1
            L_0x00da:
                return r6
            L_0x00db:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x00e6
                java.lang.String r0 = "EVENT_SCAN_STARTED"
                android.util.Log.d(r5, r0)
            L_0x00e6:
                return r6
            L_0x00e7:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                if (r0 == 0) goto L_0x00f8
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                r0.recycle()
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.mWCMQCResult = r2
            L_0x00f8:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isValidState()
                if (r0 == 0) goto L_0x0109
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.TCP_STAT_LOGGING_SECOND
                r0.setLoggingForTCPStat(r1)
            L_0x0109:
                return r6
            L_0x010a:
                java.lang.Object r0 = r11.obj
                android.net.LinkProperties r0 = (android.net.LinkProperties) r0
                if (r0 == 0) goto L_0x0292
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.LinkProperties unused = r1.mLinkProperties = r0
                goto L_0x0292
            L_0x0117:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x0122
                java.lang.String r0 = "INVALIDATED_DETECTED"
                android.util.Log.d(r5, r0)
            L_0x0122:
                int r0 = r11.arg1
                if (r0 != r6) goto L_0x012d
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mInvalidStateTesting = r6
                goto L_0x0292
            L_0x012d:
                int r0 = r11.arg1
                r2 = 2
                if (r0 != r2) goto L_0x0292
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mInvalidStateTesting = r1
                goto L_0x0292
            L_0x0139:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x0144
                java.lang.String r0 = "QC did not start, or is not concluded within 15 seconds."
                android.util.Log.d(r5, r0)
            L_0x0144:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r0 = r0.mIwcRequestQcId
                if (r0 == r4) goto L_0x0292
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.AsyncChannel r0 = r0.mIWCChannel
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mIwcRequestQcId
                r0.sendMessage(r3, r4, r1)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r0.mIwcRequestQcId = r4
                goto L_0x0292
            L_0x0162:
                android.os.Bundle r0 = r11.getData()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                java.lang.String r2 = "cid"
                int r2 = r0.getInt(r2)
                int unused = r1.mIwcRequestQcId = r2
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x017c
                java.lang.String r1 = "CMD_IWC_REQUEST_INTERNET_CHECK is not received in ConnectedState."
                android.util.Log.d(r5, r1)
            L_0x017c:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.AsyncChannel r1 = r1.mIWCChannel
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mIwcRequestQcId
                r1.sendMessage(r3, r4, r2)
                goto L_0x0292
            L_0x018d:
                android.os.Bundle r0 = r11.getData()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                java.lang.String r2 = "qai"
                int r2 = r0.getInt(r2)
                int unused = r1.mIwcCurrentQai = r2
                java.lang.String r1 = "bssid"
                java.lang.String r1 = r0.getString(r1)
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r2 == 0) goto L_0x01ca
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r3 = "CMD_IWC_CURRENT_QAI: "
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r3.mIwcCurrentQai
                r2.append(r3)
                java.lang.String r3 = ", "
                r2.append(r3)
                r2.append(r1)
                java.lang.String r2 = r2.toString()
                android.util.Log.d(r5, r2)
            L_0x01ca:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r3 = 135174(0x21006, float:1.89419E-40)
                r2.sendMessage(r3)
                goto L_0x0292
            L_0x01d4:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x01df
                java.lang.String r0 = "EVENT_ROAM_TIMEOUT"
                android.util.Log.d(r5, r0)
            L_0x01df:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mIsInRoamSession = r1
                goto L_0x0292
            L_0x01e6:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.setLogOnlyTransitions(r6)
                return r6
            L_0x01ec:
                java.lang.Object r0 = r11.obj
                android.content.Intent r0 = (android.content.Intent) r0
                android.os.Parcelable r1 = r0.getParcelableExtra(r3)
                android.net.NetworkInfo r1 = (android.net.NetworkInfo) r1
                if (r1 == 0) goto L_0x0292
                int r2 = r1.getType()
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.ConnectivityManager r3 = r3.getCm()
                android.net.Network r3 = r3.getNetworkForType(r2)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.ConnectivityManager r4 = r4.getCm()
                android.net.NetworkCapabilities r4 = r4.getNetworkCapabilities(r3)
                r5 = 1
                if (r4 == 0) goto L_0x021c
                r7 = 16
                boolean r7 = r4.hasCapability(r7)
                if (r7 != 0) goto L_0x021c
                r5 = 0
            L_0x021c:
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                r8 = 135245(0x2104d, float:1.89519E-40)
                r7.sendMessage(r8, r2, r5)
                goto L_0x0292
            L_0x0225:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.ConnectivityManager r0 = r0.mConnectivityManager
                android.net.NetworkInfo r0 = r0.getActiveNetworkInfo()
                r2 = 0
                if (r0 == 0) goto L_0x0292
                int r3 = r0.getType()
                if (r3 != 0) goto L_0x028d
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "TYPE_MOBILE received. isConnected="
                r3.append(r4)
                boolean r4 = r0.isConnected()
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r5, r3)
                boolean r3 = r0.isConnected()
                if (r3 == 0) goto L_0x0287
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r3.mIsMobileActiveNetwork
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r3.mIsMobileActiveNetwork = r6
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mIsMobileActiveNetwork
                if (r2 == r3) goto L_0x0292
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x027e
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.content.Context r3 = r3.mContext
                java.lang.String r4 = "EVENT_MOBILE_CONNECTED"
                android.widget.Toast r1 = android.widget.Toast.makeText(r3, r4, r1)
                r1.show()
            L_0x027e:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r3 = 135232(0x21040, float:1.895E-40)
                r1.sendMessage(r3)
                goto L_0x0292
            L_0x0287:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r3.mIsMobileActiveNetwork = r1
                goto L_0x0292
            L_0x028d:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r3.mIsMobileActiveNetwork = r1
            L_0x0292:
                return r6
            L_0x0293:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mIsScreenOn = r1
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.screenOffEleInitialize()
                return r6
            L_0x029e:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.mIsScreenOn = r6
                return r6
            L_0x02a4:
                return r6
            L_0x02a5:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.updateSettings()
                return r6
            L_0x02ab:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x02c7
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "Wi-Fi Radio state change : "
                r0.append(r1)
                int r1 = r11.arg1
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r5, r0)
            L_0x02c7:
                int r0 = r11.arg1
                if (r0 != r6) goto L_0x02dc
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isConnectedState()
                if (r0 == 0) goto L_0x02dc
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$NotConnectedState r1 = r0.mNotConnectedState
                r0.transitionTo(r1)
            L_0x02dc:
                return r6
            L_0x02dd:
                return r6
            L_0x02de:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r11.arg1
                int r1 = r0.calculateSignalLevel(r1)
                int unused = r0.mCurrentSignalLevel = r1
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r1 = r0.syncGetCurrentWifiInfo()
                android.net.wifi.WifiInfo unused = r0.mWifiInfo = r1
                return r6
            L_0x02f3:
                java.lang.String r0 = "EVENT_NETWORK_STATE_CHANGE"
                android.util.Log.d(r5, r0)
                java.lang.Object r0 = r11.obj
                android.content.Intent r0 = (android.content.Intent) r0
                android.os.Parcelable r1 = r0.getParcelableExtra(r3)
                android.net.NetworkInfo r1 = (android.net.NetworkInfo) r1
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r3 == 0) goto L_0x0320
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r7 = "Network state change "
                r3.append(r7)
                android.net.NetworkInfo$DetailedState r7 = r1.getDetailedState()
                r3.append(r7)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r5, r3)
            L_0x0320:
                android.net.NetworkInfo$DetailedState r3 = r1.getDetailedState()
                android.net.NetworkInfo$DetailedState r5 = android.net.NetworkInfo.DetailedState.CONNECTED
                boolean r3 = r3.equals(r5)
                if (r3 == 0) goto L_0x0387
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r3 = r3.syncGetCurrentWifiInfo()
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r5 = r5.mCurrentBssid
                if (r5 == 0) goto L_0x0377
                if (r3 == 0) goto L_0x0371
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r5 = r5.mCurrentBssid
                java.lang.String r5 = r5.mBssid
                if (r5 == 0) goto L_0x0371
                java.lang.String r5 = r3.getBSSID()
                if (r5 != 0) goto L_0x034f
                goto L_0x0371
            L_0x034f:
                java.lang.String r2 = r3.getBSSID()
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                java.lang.String r4 = r4.mBssid
                boolean r2 = r2.equals(r4)
                if (r2 != 0) goto L_0x0386
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                java.lang.String r4 = r3.getBSSID()
                int r5 = r3.getNetworkId()
                r2.updateCurrentBssid(r4, r5)
                goto L_0x0386
            L_0x0371:
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                r5.updateCurrentBssid(r2, r4)
                goto L_0x0386
            L_0x0377:
                if (r3 == 0) goto L_0x0386
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                java.lang.String r4 = r3.getBSSID()
                int r5 = r3.getNetworkId()
                r2.updateCurrentBssid(r4, r5)
            L_0x0386:
                goto L_0x03af
            L_0x0387:
                android.net.NetworkInfo$DetailedState r2 = r1.getDetailedState()
                android.net.NetworkInfo$DetailedState r3 = android.net.NetworkInfo.DetailedState.DISCONNECTED
                boolean r2 = r2.equals(r3)
                if (r2 == 0) goto L_0x039e
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.setCaptivePortalMode(r6)
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.stopPacketTracker()
                goto L_0x03af
            L_0x039e:
                android.net.NetworkInfo$DetailedState r2 = r1.getDetailedState()
                android.net.NetworkInfo$DetailedState r3 = android.net.NetworkInfo.DetailedState.OBTAINING_IPADDR
                boolean r2 = r2.equals(r3)
                if (r2 == 0) goto L_0x03af
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.startPacketTracker()
            L_0x03af:
                return r6
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.DefaultState.processMessage(android.os.Message):boolean");
        }
    }

    class NotConnectedState extends State {
        NotConnectedState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            boolean unused = WifiConnectivityMonitor.mInitialResultSentToSystemUi = false;
            WifiConnectivityMonitor.this.setCaptivePortalMode(1);
            boolean unused2 = WifiConnectivityMonitor.mIsNoCheck = false;
            int unused3 = WifiConnectivityMonitor.this.mValidationResultCount = 0;
            int unused4 = WifiConnectivityMonitor.this.mValidationCheckCount = 0;
            boolean unused5 = WifiConnectivityMonitor.this.mValidationCheckMode = false;
            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
            boolean unused6 = WifiConnectivityMonitor.this.mIsRoamingNetwork = false;
            Log.i(WifiConnectivityMonitor.TAG, "SCORE_QUALITY_CHECK_STATE_NONE");
            int unused7 = WifiConnectivityMonitor.this.mScoreQualityCheckMode = 0;
            if (!WifiConnectivityMonitor.sWifiOnly && WifiConnectivityMonitor.this.mWifiEleStateTracker != null) {
                WifiConnectivityMonitor.this.mWifiEleStateTracker.enableEleMobileRssiPolling(false, false);
                WifiConnectivityMonitor.this.mWifiEleStateTracker.unregisterElePedometer();
                WifiConnectivityMonitor.this.mWifiEleStateTracker.unregisterEleGeomagneticListener();
                WifiConnectivityMonitor.this.mWifiEleStateTracker.clearEleValidBlockFlag();
                boolean unused8 = WifiConnectivityMonitor.this.mWifiNeedRecoveryFromEle = false;
            }
            String unused9 = WifiConnectivityMonitor.this.mApOui = null;
            long unused10 = WifiConnectivityMonitor.this.mValidStartTime = 0;
            int unused11 = WifiConnectivityMonitor.this.mValidatedTime = 0;
            long unused12 = WifiConnectivityMonitor.this.mInvalidStartTime = 0;
            int unused13 = WifiConnectivityMonitor.this.mInvalidatedTime = 0;
            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON);
            WifiConnectivityMonitor.this.setRoamEventToNM(0);
            WifiConnectivityMonitor.this.updateCurrentBssid((String) null, -1);
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            int i = msg.what;
            if (i == WifiConnectivityMonitor.EVENT_WIFI_TOGGLED) {
                boolean on = msg.arg1 == 1;
                if (on) {
                    if (!WifiConnectivityMonitor.this.mIsWifiEnabled) {
                        WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                    }
                } else if (WifiConnectivityMonitor.this.mIsWifiEnabled) {
                    WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                }
                boolean unused = WifiConnectivityMonitor.this.mIsWifiEnabled = on;
                return true;
            } else if (i != WifiConnectivityMonitor.HANDLE_ON_AVAILABLE) {
                return false;
            } else {
                boolean unused2 = WifiConnectivityMonitor.mInitialResultSentToSystemUi = false;
                boolean unused3 = WifiConnectivityMonitor.mUserSelectionConfirmed = false;
                WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor.transitionTo(wifiConnectivityMonitor.mConnectedState);
                WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON);
                WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON, 7000);
                WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_FIRST);
                WifiConnectivityMonitor.this.mNetworkCallbackController.handleConnected();
                WifiConnectivityMonitor.this.mInvalidationFailHistory.qcStepTemp = 3;
                WifiConnectivityMonitor.this.mInvalidationFailHistory.qcTriggerTemp = 1;
                return true;
            }
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            int message;
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.setForceWifiIcon(1);
            if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                WifiConnectivityMonitor.this.mCurrentBssid.newLinkDetected();
            }
            if (WifiConnectivityMonitor.this.mCurrentLoss == null) {
                WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                VolumeWeightedEMA unused = wifiConnectivityMonitor.mCurrentLoss = new VolumeWeightedEMA(0.5d);
            }
            WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
            WifiInfo unused2 = wifiConnectivityMonitor2.mWifiInfo = wifiConnectivityMonitor2.syncGetCurrentWifiInfo();
            WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
            int unused3 = wifiConnectivityMonitor3.mInvalidationRssi = wifiConnectivityMonitor3.mWifiInfo.getRssi();
            WifiConnectivityMonitor.this.updateSettings();
            WifiConnectivityMonitor.this.determineMode();
            if (WifiConnectivityMonitor.this.mLinkProperties != null) {
                WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                boolean unused4 = wifiConnectivityMonitor4.mIsUsingProxy = (wifiConnectivityMonitor4.mLinkProperties.getHttpProxy() == null || WifiConnectivityMonitor.this.mLinkProperties.getHttpProxy().getHost() == null || WifiConnectivityMonitor.this.mLinkProperties.getHttpProxy().getPort() == -1) ? false : true;
                if (WifiConnectivityMonitor.this.mIsUsingProxy) {
                    WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                    String unused5 = wifiConnectivityMonitor5.mProxyAddress = wifiConnectivityMonitor5.mLinkProperties.getHttpProxy().getHost();
                    WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                    int unused6 = wifiConnectivityMonitor6.mProxyPort = wifiConnectivityMonitor6.mLinkProperties.getHttpProxy().getPort();
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "HTTP Proxy is in use. Proxy: " + WifiConnectivityMonitor.this.mProxyAddress + ":" + WifiConnectivityMonitor.this.mProxyPort);
                    }
                }
            } else {
                boolean unused7 = WifiConnectivityMonitor.this.mIsUsingProxy = false;
            }
            if (WifiConnectivityMonitor.DBG) {
                WifiConnectivityMonitor.this.mParam.setEvaluationParameters();
            }
            WifiConnectivityMonitor wifiConnectivityMonitor7 = WifiConnectivityMonitor.this;
            WifiConfiguration config = wifiConnectivityMonitor7.getWifiConfiguration(wifiConnectivityMonitor7.mWifiInfo.getNetworkId());
            if (config != null) {
                boolean unused8 = WifiConnectivityMonitor.mCurrentApDefault = config.semIsVendorSpecificSsid;
            }
            boolean isManualSelection = WifiConnectivityMonitor.this.getClientModeChannel().getManualSelection();
            Log.d(WifiConnectivityMonitor.TAG, "network manually connect : " + isManualSelection);
            if (!isManualSelection) {
                boolean unused9 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
            }
            if (WifiConnectivityMonitor.this.mCurrentBssid == null) {
                WifiConnectivityMonitor wifiConnectivityMonitor8 = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor8.updateCurrentBssid(wifiConnectivityMonitor8.mWifiInfo.getBSSID(), WifiConnectivityMonitor.this.mWifiInfo.getNetworkId());
            } else if (WifiConnectivityMonitor.this.mCurrentBssid.mBssid == null || WifiConnectivityMonitor.this.mWifiInfo.getBSSID() == null) {
                WifiConnectivityMonitor.this.updateCurrentBssid((String) null, -1);
            } else if (!WifiConnectivityMonitor.this.mWifiInfo.getBSSID().equals(WifiConnectivityMonitor.this.mCurrentBssid.mBssid)) {
                WifiConnectivityMonitor wifiConnectivityMonitor9 = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor9.updateCurrentBssid(wifiConnectivityMonitor9.mWifiInfo.getBSSID(), WifiConnectivityMonitor.this.mWifiInfo.getNetworkId());
            }
            if (!WifiConnectivityMonitor.sWifiOnly) {
                try {
                    WifiConnectivityMonitor.this.createEleObjects();
                } catch (Exception e) {
                    Log.e(WifiConnectivityMonitor.TAG, "createEleObjects exception happend! " + e.toString());
                }
            }
            if ((WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3) && WifiConnectivityMonitor.this.mWifiEleStateTracker != null) {
                WifiConnectivityMonitor.this.mWifiEleStateTracker.registerElePedometer();
                WifiConnectivityMonitor.this.mWifiEleStateTracker.clearEleValidBlockFlag();
            }
            boolean unused10 = WifiConnectivityMonitor.this.mIsInDhcpSession = false;
            boolean unused11 = WifiConnectivityMonitor.this.mIsScanning = false;
            boolean unused12 = WifiConnectivityMonitor.this.mIsInRoamSession = false;
            boolean unused13 = WifiConnectivityMonitor.this.mCheckRoamedNetwork = false;
            int unused14 = WifiConnectivityMonitor.mCPCheckTriggeredByRoam = 0;
            WifiConnectivityMonitor.this.setValidationBlock(false);
            int unused15 = WifiConnectivityMonitor.this.mValidationResultCount = 0;
            int unused16 = WifiConnectivityMonitor.this.mValidationCheckCount = 0;
            boolean unused17 = WifiConnectivityMonitor.this.mValidationCheckMode = false;
            boolean unused18 = WifiConnectivityMonitor.this.mIs204CheckInterval = false;
            int unused19 = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 0;
            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
            String macAddress = WifiConnectivityMonitor.this.mWifiInfo.getBSSID();
            if (macAddress != null && macAddress.length() > 8) {
                String unused20 = WifiConnectivityMonitor.this.mApOui = macAddress.substring(0, 8);
            }
            if (WifiConnectivityMonitor.this.mWifiSwitchForIndividualAppsService != null) {
                WifiConnectivityMonitor wifiConnectivityMonitor10 = WifiConnectivityMonitor.this;
                boolean unused21 = wifiConnectivityMonitor10.mReportedPoorNetworkDetectionEnabled = wifiConnectivityMonitor10.mPoorNetworkDetectionEnabled;
                if (WifiConnectivityMonitor.this.mReportedPoorNetworkDetectionEnabled) {
                    message = WifiSwitchForIndividualAppsService.SWITCH_TO_MOBILE_DATA_ENABLED;
                } else {
                    message = WifiSwitchForIndividualAppsService.SWITCH_TO_MOBILE_DATA_DISABLED;
                }
                WifiConnectivityMonitor.this.mWifiSwitchForIndividualAppsService.sendEmptyMessage(message);
                WifiConnectivityMonitor wifiConnectivityMonitor11 = WifiConnectivityMonitor.this;
                int unused22 = wifiConnectivityMonitor11.mReportedQai = wifiConnectivityMonitor11.mIwcCurrentQai;
                WifiSwitchForIndividualAppsService access$9400 = WifiConnectivityMonitor.this.mWifiSwitchForIndividualAppsService;
                WifiConnectivityMonitor wifiConnectivityMonitor12 = WifiConnectivityMonitor.this;
                access$9400.sendMessage(wifiConnectivityMonitor12.obtainMessage(WifiSwitchForIndividualAppsService.SWITCH_TO_MOBILE_DATA_QAI, wifiConnectivityMonitor12.mReportedQai));
            }
            if (WifiConnectivityMonitor.this.mWifiManager != null && WifiConnectivityMonitor.this.inChinaNetwork()) {
                Message msg = new Message();
                msg.what = 330;
                Bundle args = new Bundle();
                args.putString("publicDnsServer", WifiConnectivityMonitor.CHN_PUBLIC_DNS_IP);
                msg.obj = args;
                WifiConnectivityMonitor.this.mWifiManager.callSECApi(msg);
            }
            boolean unused23 = WifiConnectivityMonitor.mIsECNTReportedConnection = false;
            boolean unused24 = WifiConnectivityMonitor.mIsPassedLevel1State = false;
            int unused25 = WifiConnectivityMonitor.this.mStayingPoorRssi = 0;
            int unused26 = WifiConnectivityMonitor.this.mLossSampleCount = 0;
            int unused27 = WifiConnectivityMonitor.this.mOvercomingCount = 0;
            int unused28 = WifiConnectivityMonitor.this.mLinkLossOccurred = 0;
            int unused29 = WifiConnectivityMonitor.this.mLossHasGone = 0;
            double unused30 = WifiConnectivityMonitor.this.mPreviousLoss = WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
            WifiConnectivityMonitor.this.changeWifiIcon(1);
            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.EVENT_ROAM_TIMEOUT);
            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.EVENT_SCAN_TIMEOUT);
            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_STOP);
        }

        public boolean processMessage(Message msg) {
            String str;
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            boolean z = false;
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            switch (msg.what) {
                case WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE /*135174*/:
                    WifiConnectivityMonitor.this.updateSettings();
                    WifiConnectivityMonitor.this.determineMode();
                    return true;
                case WifiConnectivityMonitor.EVENT_SCREEN_ON /*135176*/:
                    boolean unused = WifiConnectivityMonitor.this.mIsScreenOn = true;
                    return true;
                case WifiConnectivityMonitor.EVENT_SCREEN_OFF /*135177*/:
                    boolean unused2 = WifiConnectivityMonitor.this.mIsScreenOn = false;
                    WifiConnectivityMonitor.this.screenOffEleInitialize();
                    return true;
                case WifiConnectivityMonitor.EVENT_DHCP_SESSION_STARTED /*135236*/:
                    boolean unused3 = WifiConnectivityMonitor.this.mIsInDhcpSession = true;
                    return true;
                case WifiConnectivityMonitor.EVENT_DHCP_SESSION_COMPLETE /*135237*/:
                    if (WifiConnectivityMonitor.this.mIsInDhcpSession) {
                        boolean unused4 = WifiConnectivityMonitor.this.mIsInDhcpSession = false;
                        if (WifiConnectivityMonitor.this.isInvalidState() && !WifiConnectivityMonitor.this.mIsInRoamSession) {
                            WifiConnectivityMonitor.this.setValidationBlock(false);
                            WifiConnectivityMonitor.this.requestInternetCheck(53);
                        }
                    }
                    return true;
                case WifiConnectivityMonitor.EVENT_WIFI_TOGGLED /*135243*/:
                    boolean on = msg.arg1 == 1;
                    if (!on) {
                        WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor.transitionTo(wifiConnectivityMonitor.mNotConnectedState);
                        if (WifiConnectivityMonitor.this.mIsWifiEnabled) {
                            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                        }
                    }
                    boolean unused5 = WifiConnectivityMonitor.this.mIsWifiEnabled = on;
                    return true;
                case WifiConnectivityMonitor.CMD_IWC_REQUEST_NETWORK_SWITCH_TO_MOBILE /*135369*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_IWC_REQUEST_NETWORK_SWITCH_TO_MOBILE");
                    }
                    IState currentState = WifiConnectivityMonitor.this.getCurrentState();
                    if (WifiConnectivityMonitor.this.isIwcModeEnabled()) {
                        WifiConnectivityMonitor.this.setBigDataValidationChanged();
                        if (WifiConnectivityMonitor.this.isValidState()) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mIwcWM++;
                            long unused6 = WifiConnectivityMonitor.this.mInvalidStartTime = SystemClock.elapsedRealtime();
                        }
                        WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor2.transitionTo(wifiConnectivityMonitor2.mInvalidState);
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_IWC_REQUEST_NETWORK_SWITCH_TO_WIFI /*135370*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_IWC_REQUEST_NETWORK_SWITCH_TO_WIFI");
                    }
                    IState currentState2 = WifiConnectivityMonitor.this.getCurrentState();
                    if (WifiConnectivityMonitor.this.isIwcModeEnabled()) {
                        WifiConnectivityMonitor.this.setBigDataValidationChanged();
                        if (!WifiConnectivityMonitor.this.isValidState()) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mIwcMW++;
                            long unused7 = WifiConnectivityMonitor.this.mValidStartTime = SystemClock.elapsedRealtime();
                        }
                        WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor3.transitionTo(wifiConnectivityMonitor3.mValidState);
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_IWC_REQUEST_INTERNET_CHECK /*135371*/:
                    int unused8 = WifiConnectivityMonitor.this.mIwcRequestQcId = msg.getData().getInt("cid");
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_IWC_REQUEST_INTERNET_CHECK, reqId: " + WifiConnectivityMonitor.this.mIwcRequestQcId);
                    }
                    if (WifiConnectivityMonitor.this.mCurrentMode != 1) {
                        boolean unused9 = WifiConnectivityMonitor.this.isInvalidState();
                    }
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.qcTriggerTemp = 2;
                    WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.CMD_IWC_QC_RESULT_TIMEOUT, 15000);
                    return true;
                case WifiConnectivityMonitor.CMD_REACHABILITY_LOST /*135400*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_REACHABILITY_LOST");
                    }
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnReachabilityLost();
                    }
                    return true;
                case WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON /*135468*/:
                    if (!WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                        Log.d(WifiConnectivityMonitor.TAG, "Send initial result to System UI - Invalid or CaptivePortal");
                        boolean unused10 = WifiConnectivityMonitor.mInitialResultSentToSystemUi = true;
                        WifiConnectivityMonitor.this.sendBroadcastWCMTestResult(false);
                    }
                    WifiConnectivityMonitor.this.setBigDataValidationChanged();
                    if (WifiConnectivityMonitor.this.isSkipInternetCheck()) {
                        boolean unused11 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                    WifiInfo unused12 = wifiConnectivityMonitor4.mWifiInfo = wifiConnectivityMonitor4.syncGetCurrentWifiInfo();
                    WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                    int unused13 = wifiConnectivityMonitor5.mInvalidationRssi = wifiConnectivityMonitor5.mWifiInfo.getRssi();
                    long unused14 = WifiConnectivityMonitor.this.mInvalidStartTime = SystemClock.elapsedRealtime();
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.error = 17;
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor6.setQcFailHistory(wifiConnectivityMonitor6.mInvalidationFailHistory);
                    WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_SECOND);
                    WifiConnectivityMonitor wifiConnectivityMonitor7 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor7.transitionTo(wifiConnectivityMonitor7.mInvalidState);
                    return true;
                case WifiConnectivityMonitor.HANDLE_ON_LOST /*135469*/:
                    boolean unused15 = WifiConnectivityMonitor.mInitialResultSentToSystemUi = false;
                    WifiConnectivityMonitor wifiConnectivityMonitor8 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor8.transitionTo(wifiConnectivityMonitor8.mNotConnectedState);
                    return true;
                case WifiConnectivityMonitor.CAPTIVE_PORTAL_DETECTED /*135471*/:
                    WifiConnectivityMonitor.this.setValidationBlock(false);
                    if (!WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                        boolean unused16 = WifiConnectivityMonitor.mInitialResultSentToSystemUi = true;
                        Log.d(WifiConnectivityMonitor.TAG, "Send initial result to System UI - CaptivePortal");
                        WifiConnectivityMonitor.this.sendBroadcastWCMTestResult(false);
                    }
                    boolean unused17 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                    MobileWipsFrameworkService mwfs = MobileWipsFrameworkService.getInstance();
                    if (mwfs != null) {
                        Message msgWips = Message.obtain();
                        msgWips.what = 14;
                        msgWips.arg1 = 1;
                        if (WifiConnectivityMonitor.this.isCaptivePortalExceptionOnly((String) null) || WifiConnectivityMonitor.this.isIgnorableNetwork((String) null)) {
                            z = true;
                        }
                        msgWips.arg2 = z ? 1 : 0;
                        mwfs.sendMessage(msgWips);
                    }
                    if (WifiConnectivityMonitor.this.getCurrentState() != WifiConnectivityMonitor.this.mCaptivePortalState) {
                        WifiConnectivityMonitor wifiConnectivityMonitor9 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor9.transitionTo(wifiConnectivityMonitor9.mCaptivePortalState);
                    }
                    return true;
                case WifiConnectivityMonitor.VALIDATED_DETECTED /*135472*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED");
                    }
                    if (msg.arg1 == 2) {
                        boolean unused18 = WifiConnectivityMonitor.this.mInvalidStateTesting = false;
                    }
                    if (!WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                        WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON);
                        Log.d(WifiConnectivityMonitor.TAG, "Send initial result to System UI - Valid");
                        boolean unused19 = WifiConnectivityMonitor.mInitialResultSentToSystemUi = true;
                        WifiConnectivityMonitor.this.sendBroadcastWCMTestResult(true);
                    }
                    boolean unused20 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                    if (WifiConnectivityMonitor.this.isValidState()) {
                        WifiConnectivityMonitor.this.setBigDataQualityCheck(true);
                    } else if (WifiConnectivityMonitor.mIsNoCheck) {
                        WifiConnectivityMonitor.this.setCaptivePortalMode(1);
                        WifiConnectivityMonitor wifiConnectivityMonitor10 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor10.transitionTo(wifiConnectivityMonitor10.mValidNoCheckState);
                    } else {
                        WifiConnectivityMonitor.this.setBigDataValidationChanged();
                        if (WifiConnectivityMonitor.this.getLinkDetectMode() == 1) {
                            if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                                WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqNon++;
                            } else if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                                WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqNormal++;
                            } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                                WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqAgg++;
                            }
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqNon++;
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqNormal++;
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqAgg++;
                        }
                        WifiConnectivityMonitor wifiConnectivityMonitor11 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor11.transitionTo(wifiConnectivityMonitor11.mValidState);
                        WifiConnectivityMonitor.this.setBigDataQCandNS(true);
                        long unused21 = WifiConnectivityMonitor.this.mValidStartTime = SystemClock.elapsedRealtime();
                    }
                    return true;
                case WifiConnectivityMonitor.INVALIDATED_DETECTED /*135473*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED");
                    }
                    if (msg.arg1 == 1) {
                        boolean unused22 = WifiConnectivityMonitor.this.mInvalidStateTesting = true;
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor12 = WifiConnectivityMonitor.this;
                    WifiInfo unused23 = wifiConnectivityMonitor12.mWifiInfo = wifiConnectivityMonitor12.syncGetCurrentWifiInfo();
                    WifiConnectivityMonitor wifiConnectivityMonitor13 = WifiConnectivityMonitor.this;
                    int unused24 = wifiConnectivityMonitor13.mInvalidationRssi = wifiConnectivityMonitor13.mWifiInfo.getRssi();
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.error = 17;
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    WifiConnectivityMonitor wifiConnectivityMonitor14 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor14.setQcFailHistory(wifiConnectivityMonitor14.mInvalidationFailHistory);
                    boolean unused25 = WifiConnectivityMonitor.this.bSetQcResult = false;
                    if (!WifiConnectivityMonitor.this.isInvalidState()) {
                        WifiConnectivityMonitor.this.setBigDataValidationChanged();
                        if (WifiConnectivityMonitor.this.isSkipInternetCheck()) {
                            boolean unused26 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                        }
                        WifiConnectivityMonitor.this.setBigDataQCandNS(false);
                        long unused27 = WifiConnectivityMonitor.this.mInvalidStartTime = SystemClock.elapsedRealtime();
                        WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_SECOND);
                        WifiConnectivityMonitor wifiConnectivityMonitor15 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor15.transitionTo(wifiConnectivityMonitor15.mInvalidState);
                    } else {
                        WifiConnectivityMonitor.this.setBigDataQualityCheck(false);
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_CHANGE_WIFI_ICON_VISIBILITY /*135476*/:
                    WifiConnectivityMonitor.this.changeWifiIcon(0);
                    return true;
                case WifiConnectivityMonitor.CMD_ROAM_START_COMPLETE /*135477*/:
                    String mRoamSessionState = (String) msg.obj;
                    if (mRoamSessionState != null) {
                        if ("start".equals(mRoamSessionState)) {
                            boolean unused28 = WifiConnectivityMonitor.this.mIsInRoamSession = true;
                            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.EVENT_ROAM_TIMEOUT);
                            WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.EVENT_ROAM_TIMEOUT, 30000);
                        } else if ("complete".equals(mRoamSessionState) && WifiConnectivityMonitor.this.mIsInRoamSession) {
                            boolean unused29 = WifiConnectivityMonitor.this.mIsInRoamSession = false;
                            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.EVENT_ROAM_TIMEOUT);
                            if (WifiConnectivityMonitor.this.getCurrentState() == WifiConnectivityMonitor.this.mInvalidState || WifiConnectivityMonitor.this.getCurrentState() == WifiConnectivityMonitor.this.mLevel2State || WifiConnectivityMonitor.this.mNetworkCallbackController.isCaptivePortal()) {
                                if (WifiConnectivityMonitor.this.getCurrentState() == WifiConnectivityMonitor.this.mLevel2State) {
                                    boolean unused30 = WifiConnectivityMonitor.this.mCheckRoamedNetwork = true;
                                }
                                WifiConnectivityMonitor.this.setValidationBlock(false);
                                if (WifiConnectivityMonitor.this.mNetworkCallbackController.isCaptivePortal()) {
                                    WifiConnectivityMonitor.this.setRoamEventToNM(1);
                                }
                                WifiConnectivityMonitor.this.requestInternetCheck(52);
                            }
                        }
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_NETWORK_PROPERTIES_UPDATED /*135478*/:
                    LinkProperties lp = (LinkProperties) msg.obj;
                    if (lp != null) {
                        LinkProperties unused31 = WifiConnectivityMonitor.this.mLinkProperties = lp;
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_UPDATE_CURRENT_BSSID_ON_THROUGHPUT_UPDATE /*135488*/:
                    WifiConnectivityMonitor.this.setLogOnlyTransitions(true);
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        Bundle bund = msg.getData();
                        WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnTputUpdate(Long.valueOf(bund.getLong("timeDelta", 0)).longValue(), Long.valueOf(bund.getLong("diffTxBytes", 0)).longValue(), Long.valueOf(bund.getLong("diffRxBytes", 0)).longValue());
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT /*135489*/:
                    WifiConnectivityMonitor.this.setLogOnlyTransitions(true);
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnDnsResult(msg.arg1, msg.arg2);
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT_TYPE /*135490*/:
                    WifiConnectivityMonitor.this.setLogOnlyTransitions(true);
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        WifiConnectivityMonitor.this.mCurrentBssid.updateBssidLatestDnsResultType(msg.arg1);
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor16 = WifiConnectivityMonitor.this;
                    StringBuilder sb = new StringBuilder();
                    sb.append(msg.arg1);
                    if (msg.arg1 == 0) {
                        str = " / " + msg.arg2;
                    } else {
                        str = "";
                    }
                    sb.append(str);
                    wifiConnectivityMonitor16.setDnsResultHistory(sb.toString());
                    return true;
                case WifiConnectivityMonitor.CMD_UPDATE_CURRENT_BSSID_ON_QC_RESULT /*135491*/:
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        BssidStatistics access$2600 = WifiConnectivityMonitor.this.mCurrentBssid;
                        if (msg.arg1 == 1) {
                            z = true;
                        }
                        access$2600.updateBssidQosMapOnQcResult(z);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class CaptivePortalState extends State {
        CaptivePortalState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.changeWifiIcon(1);
            WifiConnectivityMonitor.this.mCurrentBssid.mIsCaptivePortal = true;
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        public boolean processMessage(Message msg) {
            return false;
        }
    }

    class EvaluatedState extends State {
        private static final int TIME_204_CHECK_INTERVAL = 30000;
        private int mCheckFastDisconnection = 0;
        private int mGoodLinkLastRssi = 0;
        private long mLastRxGood;
        private int mPoorLinkLastRssi = -200;

        EvaluatedState() {
        }

        public void enter() {
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            WifiInfo unused = wifiConnectivityMonitor.mWifiInfo = wifiConnectivityMonitor.syncGetCurrentWifiInfo();
            if (WifiConnectivityMonitor.this.mCurrentBssid.mGoodLinkTargetRssi <= WifiConnectivityMonitor.this.mWifiInfo.getRssi() || WifiConnectivityMonitor.this.mCurrentBssid.mBssidAvoidTimeMax <= SystemClock.elapsedRealtime() || WifiConnectivityMonitor.this.mCurrentBssid.mLastPoorReason != 1) {
                WifiConnectivityMonitor.this.setLinkDetectMode(1);
            } else {
                Log.i(WifiConnectivityMonitor.TAG, "Connedted But link might be still poor, " + WifiConnectivityMonitor.this.mCurrentBssid.mGoodLinkTargetRssi);
                WifiConnectivityMonitor.this.setLinkDetectMode(0);
            }
            this.mGoodLinkLastRssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:120:0x02e3  */
        /* JADX WARNING: Removed duplicated region for block: B:127:0x0326  */
        /* JADX WARNING: Removed duplicated region for block: B:134:0x037f  */
        /* JADX WARNING: Removed duplicated region for block: B:139:0x0390  */
        /* JADX WARNING: Removed duplicated region for block: B:152:0x043f  */
        /* JADX WARNING: Removed duplicated region for block: B:155:0x0444 A[RETURN] */
        /* JADX WARNING: Removed duplicated region for block: B:156:0x0445  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(android.os.Message r33) {
            /*
                r32 = this;
                r0 = r32
                r1 = r33
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.logStateAndMessage(r1, r0)
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r3 = 0
                r2.setLogOnlyTransitions(r3)
                int r2 = r1.what
                java.lang.String r4 = "wlan0"
                r5 = 135193(0x21019, float:1.89446E-40)
                r6 = 135188(0x21014, float:1.89439E-40)
                r7 = 3
                r8 = 2
                java.lang.String r9 = "WifiConnectivityMonitor"
                r10 = 1
                switch(r2) {
                    case 135176: goto L_0x0f89;
                    case 135177: goto L_0x0f39;
                    case 135188: goto L_0x0128;
                    case 135193: goto L_0x0023;
                    default: goto L_0x0021;
                }
            L_0x0021:
                r1 = 0
                return r1
            L_0x0023:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.isConnectedState()
                if (r2 != 0) goto L_0x002c
                return r10
            L_0x002c:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r2 = r2.mCurrentBssid
                if (r2 != 0) goto L_0x0035
                return r10
            L_0x0035:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                if (r2 != 0) goto L_0x003e
                return r10
            L_0x003e:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                if (r2 == r8) goto L_0x004e
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                if (r2 != r7) goto L_0x0055
            L_0x004e:
                int r2 = com.android.server.wifi.WifiConnectivityMonitor.mLinkDetectMode
                if (r2 != 0) goto L_0x0055
                return r10
            L_0x0055:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.setLogOnlyTransitions(r10)
                int r2 = r1.arg1
                com.android.server.wifi.WifiConnectivityMonitor r6 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r6.mTrafficPollToken
                if (r2 != r6) goto L_0x0082
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.mIsScreenOn
                if (r2 != 0) goto L_0x0072
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r2 != 0) goto L_0x0082
            L_0x0072:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = com.android.server.wifi.WifiConnectivityMonitor.access$16604(r2)
                android.os.Message r3 = r2.obtainMessage(r5, r6, r3)
                r5 = 3000(0xbb8, double:1.482E-320)
                r2.sendMessageDelayed(r3, r5)
                goto L_0x00b3
            L_0x0082:
                int r2 = r1.arg1
                com.android.server.wifi.WifiConnectivityMonitor r6 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r6.mTrafficPollToken
                if (r2 == r6) goto L_0x00b3
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.mIsScreenOn
                if (r2 != 0) goto L_0x009a
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r2 != 0) goto L_0x00b3
            L_0x009a:
                java.lang.String r2 = "mTrafficPollToken MisMatch!!!"
                android.util.Log.d(r9, r2)
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.removeMessages(r5)
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = com.android.server.wifi.WifiConnectivityMonitor.access$16604(r2)
                android.os.Message r3 = r2.obtainMessage(r5, r6, r3)
                r5 = 3000(0xbb8, double:1.482E-320)
                r2.sendMessageDelayed(r3, r5)
            L_0x00b3:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r2 = r2.mTxPkts
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r5 = r5.mRxPkts
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r8 = android.net.TrafficStats.getTxPackets(r4)
                long unused = r7.mTxPkts = r8
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r8 = android.net.TrafficStats.getRxPackets(r4)
                long unused = r7.mRxPkts = r8
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r7 = r7.mTxPkts
                int r7 = (r2 > r7 ? 1 : (r2 == r7 ? 0 : -1))
                if (r7 != 0) goto L_0x00e6
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r7 = r7.mRxPkts
                int r7 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1))
                if (r7 != 0) goto L_0x00e6
                return r10
            L_0x00e6:
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.app.ActivityManager r7 = r7.mActivityManager
                if (r7 != 0) goto L_0x00ff
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.content.Context r8 = r7.mContext
                java.lang.String r9 = "activity"
                java.lang.Object r8 = r8.getSystemService(r9)
                android.app.ActivityManager r8 = (android.app.ActivityManager) r8
                android.app.ActivityManager unused = r7.mActivityManager = r8
            L_0x00ff:
                long r7 = android.net.TrafficStats.getTxBytes(r4)
                long r20 = android.net.TrafficStats.getRxBytes(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mUsageStatsUid
                long r22 = android.net.TrafficStats.getUidTxBytes(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mUsageStatsUid
                long r24 = android.net.TrafficStats.getUidRxBytes(r4)
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                r12 = r7
                r14 = r20
                r16 = r22
                r18 = r24
                r11.setTrafficPollHistory(r12, r14, r16, r18)
                return r10
            L_0x0128:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.isConnectedState()
                if (r2 != 0) goto L_0x0131
                return r10
            L_0x0131:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.isMobileHotspot()
                if (r2 == 0) goto L_0x0140
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r2 == 0) goto L_0x0140
                return r10
            L_0x0140:
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.setLogOnlyTransitions(r10)
                r2 = 0
                int r5 = r1.arg1
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r11 = r11.mRssiFetchToken
                if (r5 != r11) goto L_0x0178
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r5 = r5.mIsScreenOn
                if (r5 != 0) goto L_0x015e
                boolean r5 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r5 != 0) goto L_0x0178
            L_0x015e:
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$TxPacketInfo r2 = r5.fetchPacketCount()
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r11 = com.android.server.wifi.WifiConnectivityMonitor.access$13604(r5)
                android.os.Message r6 = r5.obtainMessage(r6, r11, r3)
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r11 = r11.mSamplingIntervalMS
                r5.sendMessageDelayed(r6, r11)
                goto L_0x01b3
            L_0x0178:
                int r5 = r1.arg1
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r11 = r11.mRssiFetchToken
                if (r5 == r11) goto L_0x01b3
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r5 = r5.mIsScreenOn
                if (r5 != 0) goto L_0x0190
                boolean r5 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r5 != 0) goto L_0x01b3
            L_0x0190:
                java.lang.String r5 = "msg.arg1 != mRssiFetchToken"
                android.util.Log.e(r9, r5)
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                r5.removeMessages(r6)
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$TxPacketInfo r2 = r5.fetchPacketCount()
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r11 = com.android.server.wifi.WifiConnectivityMonitor.access$13604(r5)
                android.os.Message r6 = r5.obtainMessage(r6, r11, r3)
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r11 = r11.mSamplingIntervalMS
                r5.sendMessageDelayed(r6, r11)
            L_0x01b3:
                if (r2 != 0) goto L_0x01b6
                return r10
            L_0x01b6:
                int r5 = r2.result
                if (r5 == r8) goto L_0x0f17
                int r5 = r2.result
                if (r5 != r7) goto L_0x01c6
                r23 = r2
                goto L_0x0f19
            L_0x01c6:
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r5 = r5.mCurrentBssid
                if (r5 != 0) goto L_0x01cf
                return r10
            L_0x01cf:
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r5 = r5.mInvalidationFailHistory
                r6 = 4
                r5.qcStepTemp = r6
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r5 = r5.mLowQualityFailHistory
                r5.qcStep = r6
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r11 = r5.syncGetCurrentWifiInfo()
                android.net.wifi.WifiInfo unused = r5.mWifiInfo = r11
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r5 = r5.mWifiInfo
                int r5 = r5.getRssi()
                int r11 = r2.mTxbad
                int r12 = r2.mTxgood
                long r13 = android.net.TrafficStats.getRxPackets(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mIsScreenOn
                r6 = 30000(0x7530, double:1.4822E-319)
                if (r4 == 0) goto L_0x0262
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r4 = r4.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r15 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r15 = r15.mInvalidState
                if (r4 != r15) goto L_0x0262
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                if (r4 != r10) goto L_0x0262
                long r3 = r0.mLastRxGood
                r19 = 0
                int r3 = (r3 > r19 ? 1 : (r3 == r19 ? 0 : -1))
                if (r3 <= 0) goto L_0x0262
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mIs204CheckInterval
                if (r3 != 0) goto L_0x0262
                long r3 = r0.mLastRxGood
                long r3 = r13 - r3
                int r3 = (int) r3
                r4 = 10
                if (r3 < r4) goto L_0x0262
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r4 == 0) goto L_0x0243
                java.lang.String r4 = "Rx packets are visible"
                android.util.Log.d(r9, r4)
            L_0x0243:
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r4 == 0) goto L_0x024e
                java.lang.String r4 = "check Internet connectivity - reportNetworkConnectivity"
                android.util.Log.d(r9, r4)
            L_0x024e:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                r15 = 11
                r4.requestInternetCheck(r15)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r4.mIs204CheckInterval = r10
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                r15 = 135208(0x21028, float:1.89467E-40)
                r4.sendMessageDelayed(r15, r6)
            L_0x0262:
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x0282
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Fetch Detect Mode : "
                r3.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.getLinkDetectMode()
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r9, r3)
            L_0x0282:
                int r3 = com.android.server.wifi.WifiConnectivityMonitor.mLinkDetectMode
                java.lang.String r4 = " rssi="
                java.lang.String r15 = "#.##"
                r19 = 4636737291354636288(0x4059000000000000, double:100.0)
                if (r3 != r10) goto L_0x0b07
                int r3 = r0.mGoodLinkLastRssi
                int r3 = r3 + r5
                int r3 = r3 / r8
                com.android.server.wifi.WifiConnectivityMonitor r6 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r6.mIwcCurrentQai
                r7 = 3
                if (r6 == r7) goto L_0x02b9
                com.android.server.wifi.WifiConnectivityMonitor r6 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r6.mCurrentMode
                if (r6 == r8) goto L_0x02ae
                com.android.server.wifi.WifiConnectivityMonitor r6 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r6.mCurrentMode
                if (r6 != r7) goto L_0x02ac
                goto L_0x02ae
            L_0x02ac:
                r6 = r15
                goto L_0x02ba
            L_0x02ae:
                r6 = r15
                int r7 = r0.mCheckFastDisconnection
                if (r7 != r10) goto L_0x02b5
                r7 = 0
                goto L_0x02b6
            L_0x02b5:
                r7 = r10
            L_0x02b6:
                r0.mCheckFastDisconnection = r7
                goto L_0x02bd
            L_0x02b9:
                r6 = r15
            L_0x02ba:
                r7 = 0
                r0.mCheckFastDisconnection = r7
            L_0x02bd:
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r7 = r7.mCurrentMode
                if (r7 == 0) goto L_0x02dd
                int r7 = r0.mCheckFastDisconnection
                if (r7 != 0) goto L_0x02dd
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.AsyncChannel r7 = r7.mIWCChannel
                r15 = 135373(0x210cd, float:1.89698E-40)
                int r8 = r2.mTxbad
                int r10 = r2.mTxgood
                r7.sendMessage(r15, r8, r10)
            L_0x02dd:
                boolean r7 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r7 == 0) goto L_0x031c
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                int r8 = r0.mCheckFastDisconnection
                r10 = 1
                if (r8 != r10) goto L_0x02f0
                java.lang.String r8 = "[FD]"
                goto L_0x02f2
            L_0x02f0:
                java.lang.String r8 = ""
            L_0x02f2:
                r7.append(r8)
                java.lang.String r8 = "Fetch RSSI succeed, rssi="
                r7.append(r8)
                r7.append(r3)
                java.lang.String r8 = " mrssi="
                r7.append(r8)
                r7.append(r3)
                java.lang.String r8 = " txbad="
                r7.append(r8)
                r7.append(r11)
                java.lang.String r8 = " txgood="
                r7.append(r8)
                r7.append(r12)
                java.lang.String r7 = r7.toString()
                android.util.Log.d(r9, r7)
            L_0x031c:
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r7 = r7.mParam
                int r7 = r7.WEAK_SIGNAL_POOR_DETECTED_RSSI_MIN
                if (r3 > r7) goto L_0x037f
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "RSSI is under than level 0 - rssi:"
                r7.append(r8)
                r7.append(r3)
                java.lang.String r7 = r7.toString()
                android.util.Log.d(r9, r7)
                long r7 = android.os.SystemClock.elapsedRealtime()
                com.android.server.wifi.WifiConnectivityMonitor r10 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r10 = r10.mParam
                r23 = r2
                long r1 = r10.mWeakSignalQCStartTime
                long r7 = r7 - r1
                r1 = 1000(0x3e8, double:4.94E-321)
                long r7 = r7 / r1
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                long r1 = r1.mWeakSignalQCStartTime
                r24 = 0
                int r1 = (r1 > r24 ? 1 : (r1 == r24 ? 0 : -1))
                if (r1 == 0) goto L_0x036a
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                int r1 = r1.WEAK_SIGNAL_FREQUENT_QC_CYCLE_SEC
                long r1 = (long) r1
                int r1 = (r7 > r1 ? 1 : (r7 == r1 ? 0 : -1))
                if (r1 <= 0) goto L_0x0368
                goto L_0x036a
            L_0x0368:
                r1 = 1
                goto L_0x0382
            L_0x036a:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 21
                r1.requestInternetCheck(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                long r9 = android.os.SystemClock.elapsedRealtime()
                r1.mWeakSignalQCStartTime = r9
                r1 = 1
                return r1
            L_0x037f:
                r23 = r2
                r1 = 1
            L_0x0382:
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.mCurrentApDefault
                if (r2 == 0) goto L_0x043f
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                if (r2 == r1) goto L_0x043f
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.fastDisconnectUpdateRssi(r3)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                int r1 = r1.FD_RSSI_LOW_THRESHOLD
                if (r3 >= r1) goto L_0x0438
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.fastDisconnectEvaluate()
                if (r1 == 0) goto L_0x0436
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                boolean unused = r1.bSetQcResult = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                r2 = 1
                r1.poorLinkDetected(r3, r2)
                r0.mPoorLinkLastRssi = r5
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$Level1State r2 = r2.mLevel1State
                if (r1 != r2) goto L_0x042b
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.skipPoorConnectionReport()
                if (r1 != 0) goto L_0x0405
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 14
                r1.error = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                java.lang.Thread r2 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r2 = r2.getStackTrace()
                r7 = 2
                r2 = r2[r7]
                int r2 = r2.getLineNumber()
                r1.line = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mInvalidationFailHistory
                r2 = 18
                r1.qcTriggerTemp = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.requestInternetCheck(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.checkTransitionToLevel2State()
                goto L_0x042b
            L_0x0405:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 15
                r1.error = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                java.lang.Thread r2 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r2 = r2.getStackTrace()
                r7 = 2
                r2 = r2[r7]
                int r2 = r2.getLineNumber()
                r1.line = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.handleNeedToRoamInLevel1State()
            L_0x042b:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r2 = r1.mLowQualityFailHistory
                r1.setQcFailHistory(r2)
                r2 = 1
                goto L_0x0440
            L_0x0436:
                r2 = 1
                goto L_0x0440
            L_0x0438:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 1
                r1.setBigDataQualityCheck(r2)
                goto L_0x0440
            L_0x043f:
                r2 = r1
            L_0x0440:
                int r1 = r0.mCheckFastDisconnection
                if (r1 != r2) goto L_0x0445
                return r2
            L_0x0445:
                long r1 = android.os.SystemClock.elapsedRealtime()
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r7 = r7.mCurrentBssid
                long r7 = r7.mLastTimeSample
                long r7 = r1 - r7
                long r24 = com.android.server.wifi.WifiConnectivityMonitor.LINK_SAMPLING_INTERVAL_MS
                r26 = 2
                long r24 = r24 * r26
                int r7 = (r7 > r24 ? 1 : (r7 == r24 ? 0 : -1))
                if (r7 >= 0) goto L_0x0a3d
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r7 = r7.mCurrentLoss
                r24 = r13
                r13 = 4602678819172646912(0x3fe0000000000000, double:0.5)
                if (r7 != 0) goto L_0x0477
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r8 = new com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA
                r8.<init>(r13)
                com.android.server.wifi.WifiConnectivityMonitor.VolumeWeightedEMA unused = r7.mCurrentLoss = r8
            L_0x0477:
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r7 = r7.mLastTxBad
                int r7 = r11 - r7
                com.android.server.wifi.WifiConnectivityMonitor r8 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r8 = r8.mLastTxGood
                int r8 = r12 - r8
                int r10 = r7 + r8
                if (r10 <= 0) goto L_0x09f6
                com.android.server.wifi.WifiConnectivityMonitor r15 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r15 = r15.mCurrentLoss
                if (r15 == 0) goto L_0x09f6
                double r13 = (double) r7
                r28 = r11
                r29 = r12
                double r11 = (double) r10
                double r13 = r13 / r11
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r11 = r11.mCurrentLoss
                r11.update(r13, r10)
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r11 = r11.mCurrentBssid
                r11.updateLoss(r3, r13, r10)
                com.android.server.wifi.WifiConnectivityMonitor r11 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r11 = r11.mCurrentBssid
                r11.updateBssidQosMapOnPerUpdate(r3, r7, r8)
                java.lang.String r11 = ""
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r12 = r12.mCurrentMode
                r15 = 3
                if (r12 != r15) goto L_0x08a1
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r12 = r12.mStayingPoorRssi
                r15 = 4
                if (r12 <= r15) goto L_0x04d4
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                r15 = 3
                int unused = r12.mLinkLossOccurred = r15
                r30 = r1
                r12 = r3
                goto L_0x06b0
            L_0x04d4:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r12 = r12.mInAggGoodStateNow
                if (r12 != 0) goto L_0x069c
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r12.mWifiInfo
                boolean r12 = r12.is5GHz()
                if (r12 == 0) goto L_0x04f6
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r12.mWifiInfo
                int r12 = r12.getRssi()
                r15 = -64
                if (r12 > r15) goto L_0x0504
            L_0x04f6:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r12.mWifiInfo
                int r12 = r12.getRssi()
                r15 = -55
                if (r12 <= r15) goto L_0x0509
            L_0x0504:
                r30 = r1
                r12 = r3
                goto L_0x069f
            L_0x0509:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r12 = r12.mCurrentBssid
                int r12 = r12.mLastGoodRxRssi
                if (r12 >= 0) goto L_0x056e
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r12 = r12.mCurrentBssid
                int r12 = r12.mLastGoodRxRssi
                if (r12 > r3) goto L_0x056e
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x052c
                java.lang.String r12 = "@L - beyond Last good rssi"
                android.util.Log.i(r9, r12)
            L_0x052c:
                if (r7 <= 0) goto L_0x0557
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r12 = r12.mLinkLossOccurred
                if (r12 != 0) goto L_0x0547
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x0541
                java.lang.String r12 = "@L - loss begin occurring"
                android.util.Log.e(r9, r12)
            L_0x0541:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                r15 = 1
                int unused = r12.mLinkLossOccurred = r15
            L_0x0547:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                r15 = 0
                int unused = r12.mLossHasGone = r15
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                double unused = r12.mPreviousLoss = r13
                r30 = r1
                r12 = r3
                goto L_0x06b0
            L_0x0557:
                r15 = 0
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r12.mLinkLossOccurred = r15
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r12.mLossHasGone = r15
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                r30 = r1
                r1 = 0
                double unused = r12.mPreviousLoss = r1
                r12 = r3
                goto L_0x06b0
            L_0x056e:
                r30 = r1
                r1 = 30
                if (r7 >= r1) goto L_0x063a
                r1 = 4602678819172646912(0x3fe0000000000000, double:0.5)
                int r12 = (r13 > r1 ? 1 : (r13 == r1 ? 0 : -1))
                if (r12 < 0) goto L_0x057c
                goto L_0x063a
            L_0x057c:
                r1 = 4591870180066957722(0x3fb999999999999a, double:0.1)
                r12 = 4
                if (r7 <= r12) goto L_0x059a
                int r12 = (r13 > r1 ? 1 : (r13 == r1 ? 0 : -1))
                if (r12 < 0) goto L_0x059a
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0593
                java.lang.String r1 = "@L - (dbad > 4)&&(loss >= 0.1)"
                android.util.Log.e(r9, r1)
            L_0x0593:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                goto L_0x0663
            L_0x059a:
                r12 = -65
                if (r3 >= r12) goto L_0x05c3
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r12.mWifiInfo
                boolean r12 = r12.is24GHz()
                if (r12 == 0) goto L_0x05c3
                r12 = 4
                if (r7 > r12) goto L_0x05b1
                int r12 = (r13 > r1 ? 1 : (r13 == r1 ? 0 : -1))
                if (r12 < 0) goto L_0x05c3
            L_0x05b1:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x05bc
                java.lang.String r1 = "@L - rssi < -65) && (mWifiInfo.is24GHz()) && ((dbad > 4)||(loss >= 0.1))"
                android.util.Log.e(r9, r1)
            L_0x05bc:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                goto L_0x0663
            L_0x05c3:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r12.mWifiInfo
                int r12 = r12.getLinkSpeed()
                r15 = 6
                if (r12 > r15) goto L_0x05e6
                int r12 = (r13 > r1 ? 1 : (r13 == r1 ? 0 : -1))
                if (r12 < 0) goto L_0x05e6
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x05df
                java.lang.String r1 = "@L - (mWifiInfo.getLinkSpeed() <= 6) && (loss >= 0.1)"
                android.util.Log.e(r9, r1)
            L_0x05df:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                goto L_0x0663
            L_0x05e6:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r12 = r12.mLossHasGone
                if (r12 != 0) goto L_0x0613
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                double r26 = r12.mPreviousLoss
                int r12 = (r13 > r26 ? 1 : (r13 == r26 ? 0 : -1))
                if (r12 <= 0) goto L_0x0613
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                double r26 = r12.mPreviousLoss
                int r1 = (r26 > r1 ? 1 : (r26 == r1 ? 0 : -1))
                if (r1 < 0) goto L_0x0613
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x060d
                java.lang.String r1 = "@L - loss increasing"
                android.util.Log.e(r9, r1)
            L_0x060d:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                goto L_0x0663
            L_0x0613:
                if (r7 <= 0) goto L_0x0663
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mLinkLossOccurred
                if (r1 != 0) goto L_0x062e
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0628
                java.lang.String r1 = "@L - loss begin occurring"
                android.util.Log.e(r9, r1)
            L_0x0628:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                goto L_0x0663
            L_0x062e:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0663
                java.lang.String r1 = "@L - loss still can be seen, keep the value!"
                android.util.Log.i(r9, r1)
                goto L_0x0663
            L_0x063a:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0645
                java.lang.String r1 = "@L - (dbad >= 30) || (loss >= 0.5)"
                android.util.Log.e(r9, r1)
            L_0x0645:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                r1 = 4602678819172646912(0x3fe0000000000000, double:0.5)
                int r1 = (r13 > r1 ? 1 : (r13 == r1 ? 0 : -1))
                if (r1 < 0) goto L_0x0663
                r1 = 5
                if (r7 < r1) goto L_0x0663
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10508(r1)
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0663
                java.lang.String r1 = "@L - (loss >= 0.5) && (dbad >= 5)"
                android.util.Log.e(r9, r1)
            L_0x0663:
                if (r7 != 0) goto L_0x068f
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.access$10604(r1)
                r2 = 1
                if (r1 <= r2) goto L_0x068d
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0679
                java.lang.String r1 = "@L - loss has gone"
                android.util.Log.i(r9, r1)
            L_0x0679:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mLinkLossOccurred = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mLossHasGone = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r12 = r3
                r2 = 0
                double unused = r1.mPreviousLoss = r2
                goto L_0x06b0
            L_0x068d:
                r12 = r3
                goto L_0x06b0
            L_0x068f:
                r12 = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mLossHasGone = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                double unused = r1.mPreviousLoss = r13
                goto L_0x06b0
            L_0x069c:
                r30 = r1
                r12 = r3
            L_0x069f:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x06aa
                java.lang.String r1 = "@L - In Agg good Rx state"
                android.util.Log.i(r9, r1)
            L_0x06aa:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mLinkLossOccurred = r2
            L_0x06b0:
                java.text.DecimalFormat r1 = new java.text.DecimalFormat
                r1.<init>(r6)
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                r2.append(r4)
                r2.append(r12)
                java.lang.String r3 = " [V]Incremental loss="
                r2.append(r3)
                r2.append(r7)
                java.lang.String r3 = "/"
                r2.append(r3)
                r2.append(r10)
                java.lang.String r3 = " cumulative loss="
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r3 = r3.mCurrentLoss
                double r3 = r3.mValue
                double r3 = r3 * r19
                java.lang.String r3 = r1.format(r3)
                r2.append(r3)
                java.lang.String r3 = "% loss="
                r2.append(r3)
                double r3 = r13 * r19
                java.lang.String r3 = r1.format(r3)
                r2.append(r3)
                java.lang.String r3 = "% volume="
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r3 = r3.mCurrentLoss
                double r3 = r3.mVolume
                java.lang.String r3 = r1.format(r3)
                r2.append(r3)
                java.lang.String r3 = " mLinkLossOccurred="
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r3.mLinkLossOccurred
                r2.append(r3)
                java.lang.String r3 = " linkspeed="
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r3 = r3.mWifiInfo
                int r3 = r3.getLinkSpeed()
                r2.append(r3)
                java.lang.String r2 = r2.toString()
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x0739
                android.util.Log.d(r9, r2)
            L_0x0739:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r3.mLinkLossOccurred
                r4 = 3
                if (r3 < r4) goto L_0x089e
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "@L - dbad : "
                r3.append(r4)
                r3.append(r7)
                java.lang.String r4 = " loss : "
                r3.append(r4)
                r3.append(r13)
                java.lang.String r4 = " mLinkLossOccurred : "
                r3.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mLinkLossOccurred
                r3.append(r4)
                r3.append(r2)
                java.lang.String r2 = r3.toString()
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x0775
                android.util.Log.e(r9, r2)
            L_0x0775:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r3.mStayingPoorRssi
                r4 = 4
                if (r3 <= r4) goto L_0x07ce
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x07a3
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.content.Context r3 = r3.mContext
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r6 = "@L - Staying under last Poor link, r="
                r4.append(r6)
                r4.append(r12)
                java.lang.String r4 = r4.toString()
                r6 = 0
                android.widget.Toast r3 = android.widget.Toast.makeText(r3, r4, r6)
                r3.show()
            L_0x07a3:
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x07bd
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "@L - Staying under last Poor link, r="
                r3.append(r4)
                r3.append(r12)
                java.lang.String r3 = r3.toString()
                android.util.Log.d(r9, r3)
            L_0x07bd:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                int unused = r3.mStayingPoorRssi = r4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r3 = r3.mLowQualityFailHistory
                r4 = 22
                r3.qcTrigger = r4
                goto L_0x07ee
            L_0x07ce:
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x07e4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.content.Context r3 = r3.mContext
                java.lang.String r4 = "HIT @L!!!"
                r6 = 0
                android.widget.Toast r3 = android.widget.Toast.makeText(r3, r4, r6)
                r3.show()
            L_0x07e4:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r3 = r3.mLowQualityFailHistory
                r4 = 23
                r3.qcTrigger = r4
            L_0x07ee:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                int unused = r3.mLinkLossOccurred = r4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r16 = r5
                r4 = 0
                double unused = r3.mPreviousLoss = r4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                int unused = r3.mLossHasGone = r4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r3.bSetQcResult = r4
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r3 = r3.mCurrentBssid
                r4 = 1
                r3.poorLinkDetected(r12, r4)
                r3 = r16
                r0.mPoorLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r4 = r4.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$Level1State r5 = r5.mLevel1State
                if (r4 != r5) goto L_0x0894
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.skipPoorConnectionReport()
                if (r4 != 0) goto L_0x086e
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r4 = r4.mLowQualityFailHistory
                r5 = 12
                r4.error = r5
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r4 = r4.mLowQualityFailHistory
                java.lang.Thread r5 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r5 = r5.getStackTrace()
                r6 = 2
                r5 = r5[r6]
                int r5 = r5.getLineNumber()
                r4.line = r5
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r4 = r4.mInvalidationFailHistory
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r5 = r5.mLowQualityFailHistory
                int r5 = r5.qcTrigger
                r4.qcTriggerTemp = r5
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r5 = r4.mLowQualityFailHistory
                int r5 = r5.qcTrigger
                r4.requestInternetCheck(r5)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4.checkTransitionToLevel2State()
                goto L_0x0894
            L_0x086e:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r4 = r4.mLowQualityFailHistory
                r5 = 13
                r4.error = r5
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r4 = r4.mLowQualityFailHistory
                java.lang.Thread r5 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r5 = r5.getStackTrace()
                r6 = 2
                r5 = r5[r6]
                int r5 = r5.getLineNumber()
                r4.line = r5
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4.handleNeedToRoamInLevel1State()
            L_0x0894:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r5 = r4.mLowQualityFailHistory
                r4.setQcFailHistory(r5)
                goto L_0x089f
            L_0x089e:
                r3 = r5
            L_0x089f:
                goto L_0x0a3c
            L_0x08a1:
                r30 = r1
                r12 = r3
                r3 = r5
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r1 = r1.mCurrentLoss
                double r1 = r1.mValue
                r4 = 4602678819172646912(0x3fe0000000000000, double:0.5)
                int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
                if (r1 <= 0) goto L_0x09ef
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r1 = r1.mCurrentLoss
                double r1 = r1.mVolume
                double r4 = com.android.server.wifi.WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME
                int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
                if (r1 <= 0) goto L_0x09ef
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x08e1
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "Poor link for link sample count, rssi="
                r1.append(r2)
                r1.append(r12)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x08e1:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 11
                r1.error = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.access$10304(r1)
                r2 = 3
                if (r1 < r2) goto L_0x0a3c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 24
                r1.qcTrigger = r2
                r1 = -80
                if (r12 >= r1) goto L_0x09c1
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                boolean unused = r1.bSetQcResult = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                r2 = 1
                r1.poorLinkDetected(r12, r2)
                r0.mPoorLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                if (r1 != r2) goto L_0x092f
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                r1.setLinkDetectMode(r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.samsung.android.server.wifi.sns.SnsBigDataSCNT r1 = r1.mSnsBigDataSCNT
                int r4 = r1.mGqPqNon
                int r4 = r4 + r2
                r1.mGqPqNon = r4
                goto L_0x09b6
            L_0x092f:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                r2 = 2
                if (r1 != r2) goto L_0x09b6
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$Level1State r2 = r2.mLevel1State
                if (r1 != r2) goto L_0x09b6
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.skipPoorConnectionReport()
                if (r1 != 0) goto L_0x0990
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                java.lang.Thread r2 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r2 = r2.getStackTrace()
                r4 = 2
                r2 = r2[r4]
                int r2 = r2.getLineNumber()
                r1.line = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 25
                r1.qcTrigger = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mInvalidationFailHistory
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r2 = r2.mLowQualityFailHistory
                int r2 = r2.qcTrigger
                r1.qcTriggerTemp = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r2 = r1.mLowQualityFailHistory
                int r2 = r2.qcTrigger
                r1.requestInternetCheck(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.checkTransitionToLevel2State()
                goto L_0x09b6
            L_0x0990:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                java.lang.Thread r2 = java.lang.Thread.currentThread()
                java.lang.StackTraceElement[] r2 = r2.getStackTrace()
                r4 = 2
                r2 = r2[r4]
                int r2 = r2.getLineNumber()
                r1.line = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r1 = r1.mLowQualityFailHistory
                r2 = 26
                r1.qcTrigger = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.handleNeedToRoamInLevel1State()
            L_0x09b6:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r2 = r1.mLowQualityFailHistory
                r1.setQcFailHistory(r2)
                goto L_0x0a3c
            L_0x09c1:
                r1 = -75
                if (r12 < r1) goto L_0x09d2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r1 = r1.mWifiInfo
                int r1 = r1.getLinkSpeed()
                r2 = 6
                if (r1 > r2) goto L_0x0a3c
            L_0x09d2:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x09dd
                java.lang.String r1 = "from LinkMonitoring"
                android.util.Log.e(r9, r1)
            L_0x09dd:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mMaxAvoidCount = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$QcFailHistory r2 = r1.mLowQualityFailHistory
                int r2 = r2.qcTrigger
                r1.requestInternetCheck(r2)
                goto L_0x0a3c
            L_0x09ef:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mLossSampleCount = r2
                goto L_0x0a3c
            L_0x09f6:
                r30 = r1
                r28 = r11
                r29 = r12
                r12 = r3
                r3 = r5
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r1 = r1.mCurrentLoss
                if (r1 == 0) goto L_0x0a3c
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0a47
                java.text.DecimalFormat r1 = new java.text.DecimalFormat
                r1.<init>(r6)
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                r2.append(r4)
                r2.append(r12)
                java.lang.String r4 = " [V]Incremental loss=0/0 cumulative loss="
                r2.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r4 = r4.mCurrentLoss
                double r4 = r4.mValue
                double r4 = r4 * r19
                java.lang.String r4 = r1.format(r4)
                r2.append(r4)
                java.lang.String r2 = r2.toString()
                android.util.Log.d(r9, r2)
                goto L_0x0a47
            L_0x0a3c:
                goto L_0x0a47
            L_0x0a3d:
                r30 = r1
                r28 = r11
                r29 = r12
                r24 = r13
                r12 = r3
                r3 = r5
            L_0x0a47:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r2 = r2.mInvalidState
                if (r1 != r2) goto L_0x0ad3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                r2 = 2
                if (r1 == r2) goto L_0x0a6c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                r2 = 3
                if (r1 != r2) goto L_0x0a68
                goto L_0x0a6c
            L_0x0a68:
                r4 = r30
                goto L_0x0ad5
            L_0x0a6c:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mInvalidationRssi
                int r1 = r1 + 5
                if (r3 < r1) goto L_0x0a9d
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.access$10404(r1)
                r2 = 8
                if (r1 < r2) goto L_0x0a9a
                java.lang.String r1 = "enable to get validation result."
                android.util.Log.d(r9, r1)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                r1.setValidationBlock(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 19
                r1.requestInternetCheck(r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mOvercomingCount = r2
                r4 = r30
                goto L_0x0ad5
            L_0x0a9a:
                r4 = r30
                goto L_0x0ad5
            L_0x0a9d:
                r2 = 0
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mOvercomingCount = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.ConnectivityManager r1 = r1.mConnectivityManager
                boolean r1 = r1.getMultiNetwork()
                if (r1 != 0) goto L_0x0ad0
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r1 = r1.mValidationCheckEnabledTime
                long r1 = r30 - r1
                int r4 = com.android.server.wifi.WifiConnectivityMonitor.VALIDATION_CHECK_TIMEOUT
                long r4 = (long) r4
                int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
                if (r1 <= 0) goto L_0x0acd
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.enableValidationCheck()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = r30
                long unused = r1.mValidationCheckEnabledTime = r4
                goto L_0x0ad5
            L_0x0acd:
                r4 = r30
                goto L_0x0ad5
            L_0x0ad0:
                r4 = r30
                goto L_0x0ad5
            L_0x0ad3:
                r4 = r30
            L_0x0ad5:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                long unused = r1.mLastTimeSample = r4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = r28
                int unused = r1.mLastTxBad = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r8 = r29
                int unused = r1.mLastTxGood = r8
                r10 = r24
                r0.mLastRxGood = r10
                r0.mGoodLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r6 = r1.mLastTxBad
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r7 = r7.mLastTxGood
                r1.setRssiPatchHistory(r6, r7, r10)
                r16 = r3
                r4 = r8
                r8 = r10
                goto L_0x0f15
            L_0x0b07:
                r23 = r2
                r3 = r5
                r2 = r11
                r8 = r12
                r10 = r13
                r1 = r15
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r5 = r5.mCurrentMode
                if (r5 == 0) goto L_0x0b2a
                com.android.server.wifi.WifiConnectivityMonitor r5 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.AsyncChannel r5 = r5.mIWCChannel
                r12 = 135373(0x210cd, float:1.89698E-40)
                int r13 = r23.mTxbad
                int r14 = r23.mTxgood
                r5.sendMessage(r12, r13, r14)
            L_0x0b2a:
                boolean r5 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r5 == 0) goto L_0x0b44
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r12 = "Fetch RSSI succeed, rssi="
                r5.append(r12)
                r5.append(r3)
                java.lang.String r5 = r5.toString()
                android.util.Log.d(r9, r5)
            L_0x0b44:
                int r5 = r0.mPoorLinkLastRssi
                int r5 = r5 + r3
                r12 = 2
                int r5 = r5 / r12
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x0b83
                java.lang.StringBuilder r12 = new java.lang.StringBuilder
                r12.<init>()
                java.lang.String r13 = "[Invalid]Fetch RSSI succeed, rssi="
                r12.append(r13)
                r12.append(r3)
                java.lang.String r13 = " mrssi="
                r12.append(r13)
                r12.append(r5)
                java.lang.String r13 = " txbad="
                r12.append(r13)
                r12.append(r2)
                java.lang.String r13 = " txgood="
                r12.append(r13)
                r12.append(r8)
                java.lang.String r13 = " rxgood="
                r12.append(r13)
                r12.append(r10)
                java.lang.String r12 = r12.toString()
                android.util.Log.d(r9, r12)
            L_0x0b83:
                long r12 = android.os.SystemClock.elapsedRealtime()
                com.android.server.wifi.WifiConnectivityMonitor r14 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r14 = r14.mCurrentBssid
                long r14 = r14.mLastTimeSample
                long r14 = r12 - r14
                r16 = 2000(0x7d0, double:9.88E-321)
                int r14 = (r14 > r16 ? 1 : (r14 == r16 ? 0 : -1))
                if (r14 >= 0) goto L_0x0c91
                com.android.server.wifi.WifiConnectivityMonitor r14 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r14 = r14.mLastTxBad
                int r14 = r2 - r14
                com.android.server.wifi.WifiConnectivityMonitor r15 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r15 = r15.mLastTxGood
                int r15 = r8 - r15
                int r6 = r14 + r15
                if (r6 <= 0) goto L_0x0c4b
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r7 = r7.mCurrentLoss
                if (r7 == 0) goto L_0x0c4b
                r29 = r8
                double r7 = (double) r14
                r24 = r10
                double r10 = (double) r6
                double r7 = r7 / r10
                com.android.server.wifi.WifiConnectivityMonitor r10 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r10 = r10.mCurrentLoss
                r10.update(r7, r6)
                boolean r10 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r10 == 0) goto L_0x0c36
                java.text.DecimalFormat r10 = new java.text.DecimalFormat
                r10.<init>(r1)
                r1 = r10
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r4)
                r10.append(r3)
                java.lang.String r4 = " [I]Incremental loss="
                r10.append(r4)
                r10.append(r14)
                java.lang.String r4 = "/"
                r10.append(r4)
                r10.append(r6)
                java.lang.String r4 = " Current loss="
                r10.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r4 = r4.mCurrentLoss
                double r26 = r4.mValue
                r30 = r12
                double r11 = r26 * r19
                java.lang.String r4 = r1.format(r11)
                r10.append(r4)
                java.lang.String r4 = "% volume="
                r10.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r4 = r4.mCurrentLoss
                double r11 = r4.mVolume
                java.lang.String r4 = r1.format(r11)
                r10.append(r4)
                java.lang.String r4 = " linkspeed="
                r10.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r4 = r4.mWifiInfo
                int r4 = r4.getLinkSpeed()
                r10.append(r4)
                java.lang.String r4 = r10.toString()
                android.util.Log.d(r9, r4)
                goto L_0x0c38
            L_0x0c36:
                r30 = r12
            L_0x0c38:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                r1.updateLoss(r5, r7, r6)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                r1.updateBssidQosMapOnPerUpdate(r5, r14, r15)
                goto L_0x0c90
            L_0x0c4b:
                r29 = r8
                r24 = r10
                r30 = r12
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r7 = r7.mCurrentLoss
                if (r7 == 0) goto L_0x0c90
                boolean r7 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r7 == 0) goto L_0x0c97
                java.text.DecimalFormat r7 = new java.text.DecimalFormat
                r7.<init>(r1)
                r1 = r7
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                r7.append(r4)
                r7.append(r3)
                java.lang.String r4 = " [I]Incremental loss=0/0 cumulative loss="
                r7.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$VolumeWeightedEMA r4 = r4.mCurrentLoss
                double r10 = r4.mValue
                double r10 = r10 * r19
                java.lang.String r4 = r1.format(r10)
                r7.append(r4)
                java.lang.String r4 = r7.toString()
                android.util.Log.d(r9, r4)
                goto L_0x0c97
            L_0x0c90:
                goto L_0x0c97
            L_0x0c91:
                r29 = r8
                r24 = r10
                r30 = r12
            L_0x0c97:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                long r6 = r1.mBssidAvoidTimeMax
                long r6 = r6 - r30
                long r10 = android.os.SystemClock.elapsedRealtime()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                long r12 = r1.mLastPoorDetectedTime
                long r10 = r10 - r12
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                java.util.Objects.requireNonNull(r1)
                r1 = 30000(0x7530, float:4.2039E-41)
                long r12 = (long) r1
                int r1 = (r10 > r12 ? 1 : (r10 == r12 ? 0 : -1))
                if (r1 <= 0) goto L_0x0cef
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                int r1 = r1.mEnhancedTargetRssi
                if (r1 == 0) goto L_0x0cef
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                int r4 = r4.mEnhancedTargetRssi
                com.android.server.wifi.WifiConnectivityMonitor.BssidStatistics.access$13120(r1, r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                r4 = 0
                r1.mEnhancedTargetRssi = r4
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0cef
                java.lang.String r1 = "restore target rssi"
                android.util.Log.d(r9, r1)
            L_0x0cef:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.mIsScreenOn
                if (r1 != 0) goto L_0x0d28
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.mInitialResultSentToSystemUi
                if (r1 == 0) goto L_0x0d28
                r12 = 30000(0x7530, double:1.4822E-319)
                int r1 = (r6 > r12 ? 1 : (r6 == r12 ? 0 : -1))
                if (r1 >= 0) goto L_0x0d28
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                long r8 = android.os.SystemClock.elapsedRealtime()
                long r8 = r8 + r12
                long unused = r1.mBssidAvoidTimeMax = r8
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                int unused = r1.mGoodTargetCount = r4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                long r8 = r1.mBssidAvoidTimeMax
                long r12 = android.os.SystemClock.elapsedRealtime()
                long r8 = r8 - r12
                r1 = 1
                return r1
            L_0x0d28:
                r1 = 1
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                if (r4 != r1) goto L_0x0dee
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.mIsScreenOn
                if (r1 == 0) goto L_0x0dea
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                int r1 = r1.mGoodLinkTargetRssi
                if (r5 < r1) goto L_0x0dbc
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.access$16304(r1)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                int r4 = r4.mGoodLinkTargetCount
                if (r1 < r4) goto L_0x0db8
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0d71
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r4 = "Good link detected, rssi="
                r1.append(r4)
                r1.append(r5)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0d71:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x0d7c
                java.lang.String r1 = "check Internet connectivity - reportNetworkConnectivity"
                android.util.Log.d(r9, r1)
            L_0x0d7c:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 1
                r1.setLinkDetectMode(r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.samsung.android.server.wifi.sns.SnsBigDataSCNT r1 = r1.mSnsBigDataSCNT
                int r8 = r1.mPqGqNon
                int r8 = r8 + r4
                r1.mPqGqNon = r8
                r0.mGoodLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r4 = r4.mInvalidState
                if (r1 != r4) goto L_0x0da4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 12
                r1.requestInternetCheck(r4)
            L_0x0da4:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 1
                boolean unused = r1.mIs204CheckInterval = r4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 135208(0x21028, float:1.89467E-40)
                r8 = 30000(0x7530, double:1.4822E-319)
                r1.sendMessageDelayed(r4, r8)
                r12 = r30
                goto L_0x0ee5
            L_0x0db8:
                r12 = r30
                goto L_0x0ee5
            L_0x0dbc:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                int unused = r1.mGoodTargetCount = r4
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0de6
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r4 = "Link is still poor, "
                r1.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                int r4 = r4.mGoodLinkTargetRssi
                r1.append(r4)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0de6:
                r12 = r30
                goto L_0x0ee5
            L_0x0dea:
                r12 = r30
                goto L_0x0ee5
            L_0x0dee:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                int r1 = r1.mGoodLinkTargetRssi
                if (r3 < r1) goto L_0x0e76
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.access$16304(r1)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                int r4 = r4.mGoodLinkTargetCount
                if (r1 < r4) goto L_0x0e73
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0e26
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r4 = "Good link detected, rssi="
                r1.append(r4)
                r1.append(r3)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0e26:
                r0.mGoodLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$Level2State r4 = r4.mLevel2State
                if (r1 != r4) goto L_0x0e3d
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.checkTransitionToLevel1StateState()
                r4 = 0
                goto L_0x0e65
            L_0x0e3d:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 1
                r1.setLinkDetectMode(r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r4 = r4.mInvalidState
                if (r1 != r4) goto L_0x0e64
                java.lang.String r1 = "enable to get validation result."
                android.util.Log.d(r9, r1)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = 0
                r1.setValidationBlock(r4)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r8 = 12
                r1.requestInternetCheck(r8)
                goto L_0x0e65
            L_0x0e64:
                r4 = 0
            L_0x0e65:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mGoodTargetCount = r4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mMaxAvoidCount = r4
                r12 = r30
                goto L_0x0ee5
            L_0x0e73:
                r12 = r30
                goto L_0x0ee5
            L_0x0e76:
                r4 = 0
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mGoodTargetCount = r4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r4 = r4.mInvalidState
                if (r1 != r4) goto L_0x0eb7
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.ConnectivityManager r1 = r1.mConnectivityManager
                boolean r1 = r1.getMultiNetwork()
                if (r1 != 0) goto L_0x0eb7
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r12 = r1.mValidationCheckEnabledTime
                long r12 = r30 - r12
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.VALIDATION_CHECK_TIMEOUT
                long r14 = (long) r1
                int r1 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1))
                if (r1 <= 0) goto L_0x0eb4
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.enableValidationCheck()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r12 = r30
                long unused = r1.mValidationCheckEnabledTime = r12
                goto L_0x0eb9
            L_0x0eb4:
                r12 = r30
                goto L_0x0eb9
            L_0x0eb7:
                r12 = r30
            L_0x0eb9:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x0ee5
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r4 = "Link is still poor, time left="
                r1.append(r4)
                r1.append(r6)
                java.lang.String r4 = ", "
                r1.append(r4)
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r4 = r4.mCurrentBssid
                int r4 = r4.mGoodLinkTargetRssi
                r1.append(r4)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0ee5:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mCurrentBssid
                long unused = r1.mLastTimeSample = r12
                r8 = r24
                r0.mLastRxGood = r8
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mLastTxBad = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r4 = r29
                int unused = r1.mLastTxGood = r4
                r0.mPoorLinkLastRssi = r3
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r14 = r1.mLastTxBad
                com.android.server.wifi.WifiConnectivityMonitor r15 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r15 = r15.mLastTxGood
                r28 = r2
                r16 = r3
                long r2 = r0.mLastRxGood
                r1.setRssiPatchHistory(r14, r15, r2)
            L_0x0f15:
                r1 = 1
                return r1
            L_0x0f17:
                r23 = r2
            L_0x0f19:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x0f37
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "RSSI_FETCH_FAILED reason : "
                r1.append(r2)
                int r2 = r23.result
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0f37:
                r1 = 1
                return r1
            L_0x0f39:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x0f59
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "Fetch Detect Mode : "
                r1.append(r2)
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.getLinkDetectMode()
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x0f59:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                boolean unused = r1.mIsScreenOn = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.screenOffEleInitialize()
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.removeMessages(r6)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.removeMessages(r5)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$13608(r1)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$NetworkStatsAnalyzer r1 = r1.mNetworkStatsAnalyzer
                if (r1 == 0) goto L_0x0f87
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$NetworkStatsAnalyzer r1 = r1.mNetworkStatsAnalyzer
                r2 = 135220(0x21034, float:1.89484E-40)
                r1.sendEmptyMessage(r2)
            L_0x0f87:
                r1 = 1
                return r1
            L_0x0f89:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                if (r1 == 0) goto L_0x0feb
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = com.android.server.wifi.WifiConnectivityMonitor.access$13604(r1)
                r3 = 0
                android.os.Message r2 = r1.obtainMessage(r6, r2, r3)
                r1.sendMessage(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.isValidState()
                if (r1 == 0) goto L_0x0fce
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$Level2State r2 = r2.mLevel2State
                if (r1 == r2) goto L_0x0fce
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$NetworkStatsAnalyzer r1 = r1.mNetworkStatsAnalyzer
                if (r1 == 0) goto L_0x0fc9
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$NetworkStatsAnalyzer r1 = r1.mNetworkStatsAnalyzer
                r2 = 135219(0x21033, float:1.89482E-40)
                r1.sendEmptyMessage(r2)
            L_0x0fc9:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.startEleCheck()
            L_0x0fce:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                r2 = 1
                if (r1 == r2) goto L_0x0fdd
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.mLinkDetectMode
                if (r1 != r2) goto L_0x0feb
            L_0x0fdd:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = com.android.server.wifi.WifiConnectivityMonitor.access$16604(r1)
                r3 = 0
                android.os.Message r2 = r1.obtainMessage(r5, r2, r3)
                r1.sendMessage(r2)
            L_0x0feb:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x1009
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "Fetch Detect Mode : "
                r1.append(r2)
                int r2 = com.android.server.wifi.WifiConnectivityMonitor.mLinkDetectMode
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r9, r1)
            L_0x1009:
                int r1 = com.android.server.wifi.WifiConnectivityMonitor.mLinkDetectMode
                r2 = 1
                if (r1 != r2) goto L_0x101c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mLossSampleCount = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mOvercomingCount = r2
                goto L_0x105c
            L_0x101c:
                r2 = 0
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r1.mGoodTargetCount = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.mIsScreenOn
                if (r1 != 0) goto L_0x105c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.IState r1 = r1.getCurrentState()
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$InvalidState r2 = r2.mInvalidState
                if (r1 != r2) goto L_0x105c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r2 = r1.syncGetCurrentWifiInfo()
                android.net.wifi.WifiInfo unused = r1.mWifiInfo = r2
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r1 = r1.mWifiInfo
                int r1 = r1.getRssi()
                r2 = -66
                if (r1 < r2) goto L_0x105c
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                r1.setValidationBlock(r2)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 28
                r1.requestInternetCheck(r2)
            L_0x105c:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 1
                boolean unused = r1.mIsScreenOn = r2
                return r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.EvaluatedState.processMessage(android.os.Message):boolean");
        }
    }

    /* access modifiers changed from: private */
    public int getLinkDetectMode() {
        return mLinkDetectMode;
    }

    /* access modifiers changed from: private */
    public void setLinkDetectMode(int mode) {
        if (this.mCurrentMode != 0) {
            Log.d(TAG, "setLinkDetectMode : " + mode);
            if (mLinkDetectMode != mode) {
                if (mode == 0) {
                    sendMessage(CMD_LINK_POOR_ENTERED);
                } else if (mode == 1) {
                    this.mLinkLossOccurred = 0;
                    this.mLossSampleCount = 0;
                    this.mOvercomingCount = 0;
                    sendMessage(CMD_LINK_GOOD_ENTERED);
                }
            }
            mLinkDetectMode = mode;
        }
    }

    class InvalidState extends State {
        private static final int TIME_204_CHECK_INTERVAL = 30000;
        private long mLastRxGood;

        InvalidState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            boolean unused = WifiConnectivityMonitor.this.bSetQcResult = false;
            WifiConnectivityMonitor.this.determineMode();
            boolean unused2 = WifiConnectivityMonitor.this.mIs204CheckInterval = false;
            WifiConnectivityMonitor.this.mIWCChannel.sendMessage(IWCMonitor.TRANSIT_TO_INVALID);
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            wifiConnectivityMonitor.sendMessage(wifiConnectivityMonitor.obtainMessage(WifiConnectivityMonitor.CMD_RSSI_FETCH, WifiConnectivityMonitor.access$13604(wifiConnectivityMonitor), 0));
            WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
            wifiConnectivityMonitor2.sendMessage(wifiConnectivityMonitor2.obtainMessage(WifiConnectivityMonitor.CMD_TRAFFIC_POLL, WifiConnectivityMonitor.access$16604(wifiConnectivityMonitor2), 0));
            WifiConnectivityMonitor.this.stopScoreQualityCheck();
            boolean unused3 = WifiConnectivityMonitor.this.mWifiNeedRecoveryFromEle = false;
            if (WifiConnectivityMonitor.this.mWifiEleStateTracker != null) {
                WifiConnectivityMonitor.this.mWifiEleStateTracker.clearEleValidBlockFlag();
            }
            if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                WifiConnectivityMonitor.this.setValidationBlock(false);
                WifiConnectivityMonitor.this.changeWifiIcon(1);
            } else if (WifiConnectivityMonitor.mUserSelectionConfirmed) {
                Log.d(WifiConnectivityMonitor.TAG, "mUserSelectionConfirmed : " + WifiConnectivityMonitor.mUserSelectionConfirmed);
                WifiConnectivityMonitor.this.changeWifiIcon(0);
            }
            if (WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3) {
                long unused4 = WifiConnectivityMonitor.this.mValidationCheckEnabledTime = SystemClock.elapsedRealtime();
                WifiConnectivityMonitor.this.mClientModeImpl.startScanFromWcm();
                if (WifiConnectivityMonitor.DBG) {
                    Log.d(WifiConnectivityMonitor.TAG, "Start scan to find alternative networks. " + WifiConnectivityMonitor.this.getCurrentState() + WifiConnectivityMonitor.this.getCurrentMode());
                }
            }
            WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
            WifiInfo unused5 = wifiConnectivityMonitor3.mWifiInfo = wifiConnectivityMonitor3.syncGetCurrentWifiInfo();
            int currentRssi = WifiConnectivityMonitor.this.mWifiInfo.getNetworkId() != -1 ? WifiConnectivityMonitor.this.mWifiInfo.getRssi() : -61;
            if (!WifiConnectivityMonitor.mIsECNTReportedConnection && WifiConnectivityMonitor.mIsPassedLevel1State && currentRssi > -60) {
                WifiConnectivityMonitor.this.uploadNoInternetECNTBigData();
            }
            if (currentRssi > -64) {
                WifiConnectivityMonitor.this.mContext.sendBroadcastAsUser(new Intent("com.sec.android.HEAT_WIFI_UNWANTED"), UserHandle.ALL);
            }
            WifiConnectivityMonitor.this.getClientModeChannel().unwantedMoreDump();
            WifiConnectivityMonitor.this.sendBroadcastWCMStatusChanged();
            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.NETWORK_STAT_CHECK_DNS);
            WifiConnectivityMonitor.this.mCurrentBssid.updateBssidNoInternet();
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
            boolean unused = WifiConnectivityMonitor.this.mTransitionPendingByIwc = false;
            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL);
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            switch (msg.what) {
                case WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE /*135174*/:
                    WifiConnectivityMonitor.this.updateSettings();
                    int previousMode = WifiConnectivityMonitor.this.mCurrentMode;
                    WifiConnectivityMonitor.this.determineMode();
                    if (previousMode != WifiConnectivityMonitor.this.mCurrentMode) {
                        if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                            WifiConnectivityMonitor.this.setValidationBlock(false);
                            WifiConnectivityMonitor.this.changeWifiIcon(1);
                            if (!WifiConnectivityMonitor.this.mAirPlaneMode) {
                                WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, !WifiConnectivityMonitor.this.mPoorNetworkDetectionEnabled);
                            }
                            if (WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                                WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                            }
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                            if (previousMode == 1) {
                                WifiConnectivityMonitor.this.setValidationBlock(true);
                                WifiConnectivityMonitor.this.changeWifiIcon(0);
                                WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, !WifiConnectivityMonitor.this.mPoorNetworkDetectionEnabled);
                                if (WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                                    int unused = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 0;
                                    Log.d(WifiConnectivityMonitor.TAG, "POOR_LINK_DETECT_sent");
                                }
                            }
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                            if (previousMode == 1) {
                                WifiConnectivityMonitor.this.setValidationBlock(true);
                                WifiConnectivityMonitor.this.changeWifiIcon(0);
                                WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, !WifiConnectivityMonitor.this.mPoorNetworkDetectionEnabled);
                                if (WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                                    int unused2 = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 0;
                                }
                            }
                            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor.sendMessage(wifiConnectivityMonitor.obtainMessage(WifiConnectivityMonitor.CMD_RSSI_FETCH, WifiConnectivityMonitor.access$13604(wifiConnectivityMonitor), 0));
                        } else {
                            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                            WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor2.transitionTo(wifiConnectivityMonitor2.mValidState);
                        }
                    }
                    return true;
                case WifiConnectivityMonitor.REPORT_NETWORK_CONNECTIVITY /*135204*/:
                    WifiConnectivityMonitor.this.reportNetworkConnectivityToNM(msg.arg1, msg.arg2);
                    return true;
                case WifiConnectivityMonitor.CONNECTIVITY_VALIDATION_RESULT /*135206*/:
                    boolean valid = msg.arg1 == 1;
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CONNECTIVITY_VALIDATION_RESULT : " + valid);
                    }
                    if (!valid) {
                        boolean unused3 = WifiConnectivityMonitor.this.mValidationCheckMode = false;
                        int unused4 = WifiConnectivityMonitor.this.mValidationResultCount = 0;
                        int unused5 = WifiConnectivityMonitor.this.mValidationCheckCount = 0;
                        WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.VALIDATION_CHECK_FORCE);
                        return true;
                    }
                    WifiConnectivityMonitor.access$5508(WifiConnectivityMonitor.this);
                    return true;
                case WifiConnectivityMonitor.VALIDATION_CHECK_FORCE /*135207*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "VALIDATION_CHECK_FORCE");
                    }
                    long unused6 = WifiConnectivityMonitor.this.mValidationCheckEnabledTime = SystemClock.elapsedRealtime();
                    WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                    int unused7 = wifiConnectivityMonitor3.mValidationCheckTime = wifiConnectivityMonitor3.mValidationCheckTime / 2;
                    WifiConnectivityMonitor.access$5608(WifiConnectivityMonitor.this);
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "mValidationCheckCount : " + WifiConnectivityMonitor.this.mValidationCheckCount);
                    }
                    if (WifiConnectivityMonitor.this.mWCMQCResult != null) {
                        WifiConnectivityMonitor.this.mWCMQCResult.recycle();
                        WifiConnectivityMonitor.this.mWCMQCResult = null;
                    }
                    WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.QC_RESULT_NOT_RECEIVED);
                    boolean queried = WifiConnectivityMonitor.this.reportNetworkConnectivityToNM(true, 5, 20);
                    if (WifiConnectivityMonitor.this.mValidationCheckCount > WifiConnectivityMonitor.VALIDATION_CHECK_MAX_COUNT) {
                        boolean unused8 = WifiConnectivityMonitor.this.mValidationCheckMode = false;
                        WifiConnectivityMonitor.this.setValidationBlock(false);
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(WifiConnectivityMonitor.TAG, "mValidationCheckCount expired");
                        }
                        if (queried) {
                            int unused9 = WifiConnectivityMonitor.this.mValidationResultCount = 0;
                            int unused10 = WifiConnectivityMonitor.this.mValidationCheckCount = 0;
                        } else {
                            WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.VALIDATION_CHECK_FORCE, 10000);
                        }
                        return true;
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor4.sendMessageDelayed(WifiConnectivityMonitor.VALIDATION_CHECK_FORCE, (long) (wifiConnectivityMonitor4.mValidationCheckTime * 1000));
                    return true;
                case WifiConnectivityMonitor.QC_RESET_204_CHECK_INTERVAL /*135208*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "QC_RESET_204_CHECK_INTERVAL");
                    }
                    boolean unused11 = WifiConnectivityMonitor.this.mIs204CheckInterval = false;
                    return true;
                case WifiConnectivityMonitor.EVENT_MOBILE_CONNECTED /*135232*/:
                    if (WifiConnectivityMonitor.mInitialResultSentToSystemUi && WifiConnectivityMonitor.mUserSelectionConfirmed && (WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3)) {
                        WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                        WifiInfo unused12 = wifiConnectivityMonitor5.mWifiInfo = wifiConnectivityMonitor5.syncGetCurrentWifiInfo();
                        WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                        int unused13 = wifiConnectivityMonitor6.mPoorConnectionDisconnectedNetId = wifiConnectivityMonitor6.mWifiInfo.getNetworkId();
                        if (WifiConnectivityMonitor.this.mNumOfToggled < 1) {
                            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(true);
                        }
                        WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.CMD_CHANGE_WIFI_ICON_VISIBILITY, 0);
                    }
                    return true;
                case WifiConnectivityMonitor.EVENT_WIFI_TOGGLED /*135243*/:
                    boolean on = msg.arg1 == 1;
                    String bssid = (String) msg.obj;
                    if (!on && bssid != null) {
                        if (WifiConnectivityMonitor.this.mBssidCache.get(bssid) != null) {
                            Log.d(WifiConnectivityMonitor.TAG, "BssidStatistics removed - " + bssid.substring(9));
                            WifiConnectivityMonitor.this.mBssidCache.remove(bssid);
                        }
                        WifiConnectivityMonitor.this.updateCurrentBssid((String) null, -1);
                        WifiConnectivityMonitor wifiConnectivityMonitor7 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor7.transitionTo(wifiConnectivityMonitor7.mNotConnectedState);
                    }
                    if (!on && WifiConnectivityMonitor.this.mIsWifiEnabled) {
                        WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
                    }
                    boolean unused14 = WifiConnectivityMonitor.this.mIsWifiEnabled = on;
                    return true;
                case WifiConnectivityMonitor.EVENT_USER_SELECTION /*135264*/:
                    boolean keepConnection = msg.arg1 == 1;
                    Log.d(WifiConnectivityMonitor.TAG, getName() + " EVENT_USER_SELECTION : " + keepConnection);
                    if (!keepConnection) {
                        WifiConnectivityMonitor.this.changeWifiIcon(0);
                    }
                    boolean unused15 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                    WifiConnectivityMonitor.this.determineMode();
                    return true;
                case WifiConnectivityMonitor.CMD_QUALITY_CHECK_BY_SCORE /*135388*/:
                    WifiConnectivityMonitor.this.requestInternetCheck(16);
                    return true;
                case WifiConnectivityMonitor.VALIDATED_DETECTED /*135472*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED");
                    }
                    boolean unused16 = WifiConnectivityMonitor.mUserSelectionConfirmed = true;
                    if (WifiConnectivityMonitor.this.getLinkDetectMode() == 1) {
                        if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqNon++;
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqNormal++;
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                            WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvGqAgg++;
                        }
                    } else if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqNon++;
                    } else if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqNormal++;
                    } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mInvPqAgg++;
                    }
                    WifiConnectivityMonitor.this.setBigDataQCandNS(true);
                    long unused17 = WifiConnectivityMonitor.this.mValidStartTime = SystemClock.elapsedRealtime();
                    WifiConnectivityMonitor wifiConnectivityMonitor8 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor8.transitionTo(wifiConnectivityMonitor8.mValidState);
                    return true;
                case WifiConnectivityMonitor.INVALIDATED_DETECTED /*135473*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED");
                    }
                    int unused18 = WifiConnectivityMonitor.this.mGoodTargetCount = 0;
                    if (!WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                        int unused19 = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 0;
                    }
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.error = 17;
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    WifiConnectivityMonitor wifiConnectivityMonitor9 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor9.setQcFailHistory(wifiConnectivityMonitor9.mInvalidationFailHistory);
                    boolean unused20 = WifiConnectivityMonitor.this.bSetQcResult = false;
                    WifiConnectivityMonitor.this.setBigDataQualityCheck(false);
                    return true;
                case WifiConnectivityMonitor.CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL /*135481*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CHECK_ALTERNATIVE_NETWORKS - mInvalidState && SNS ON");
                    }
                    WifiConnectivityMonitor.this.mClientModeImpl.checkAlternativeNetworksForWmc(false);
                    return true;
                default:
                    return false;
            }
        }
    }

    class ValidState extends State {
        ValidState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            if (WifiConnectivityMonitor.this.mCurrentMode != 0) {
                WifiConnectivityMonitor.this.mIWCChannel.sendMessage(IWCMonitor.TRANSIT_TO_VALID);
            }
            long unused = WifiConnectivityMonitor.this.mCurrentBssid.mBssidAvoidTimeMax = SystemClock.elapsedRealtime();
            WifiConnectivityMonitor.this.updateTargetRssiForCurrentAP(false);
            WifiConnectivityMonitor.this.determineMode();
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_TRANSIT_ON_VALID);
            WifiConnectivityMonitor.this.setValidationBlock(false);
            int unused2 = WifiConnectivityMonitor.this.mValidationResultCount = 0;
            int unused3 = WifiConnectivityMonitor.this.mValidationCheckCount = 0;
            boolean unused4 = WifiConnectivityMonitor.this.mValidationCheckMode = false;
            if (WifiConnectivityMonitor.this.mWifiEleStateTracker != null) {
                WifiConnectivityMonitor.this.mWifiEleStateTracker.clearEleValidBlockFlag();
            }
            if (WifiConnectivityMonitor.this.mCurrentMode != 0) {
                WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor.sendMessage(wifiConnectivityMonitor.obtainMessage(WifiConnectivityMonitor.CMD_RSSI_FETCH, WifiConnectivityMonitor.access$13604(wifiConnectivityMonitor), 0));
                WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor2.sendMessage(wifiConnectivityMonitor2.obtainMessage(WifiConnectivityMonitor.CMD_TRAFFIC_POLL, WifiConnectivityMonitor.access$16604(wifiConnectivityMonitor2), 0));
                WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_START);
                WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.NETWORK_STAT_CHECK_DNS);
                WifiConnectivityMonitor.this.startPoorQualityScoreCheck();
            }
            WifiConnectivityMonitor.this.sendBroadcastWCMStatusChanged();
            WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_RESET);
            WifiConnectivityMonitor.this.mCurrentBssid.updateBssidNoInternet();
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_STOP);
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            switch (msg.what) {
                case WifiConnectivityMonitor.EVENT_WATCHDOG_SETTINGS_CHANGE /*135174*/:
                    WifiConnectivityMonitor.this.updateSettings();
                    int previousMode = WifiConnectivityMonitor.this.mCurrentMode;
                    WifiConnectivityMonitor.this.determineMode();
                    if (previousMode != WifiConnectivityMonitor.this.mCurrentMode) {
                        if (previousMode == 0) {
                            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor.sendMessage(wifiConnectivityMonitor.obtainMessage(WifiConnectivityMonitor.CMD_RSSI_FETCH, WifiConnectivityMonitor.access$13604(wifiConnectivityMonitor), 0));
                            WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor2.sendMessage(wifiConnectivityMonitor2.obtainMessage(WifiConnectivityMonitor.CMD_TRAFFIC_POLL, WifiConnectivityMonitor.access$16604(wifiConnectivityMonitor2), 0));
                            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_START);
                            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.NETWORK_STAT_CHECK_DNS);
                        } else if (WifiConnectivityMonitor.this.mCurrentMode != 0 && (OpBrandingLoader.Vendor.SKT == WifiConnectivityMonitor.mOpBranding || OpBrandingLoader.Vendor.KTT == WifiConnectivityMonitor.mOpBranding || OpBrandingLoader.Vendor.LGU == WifiConnectivityMonitor.mOpBranding)) {
                            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_STOP);
                            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_START);
                        }
                        if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                            if (!WifiConnectivityMonitor.this.mAirPlaneMode) {
                                WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, !WifiConnectivityMonitor.this.mPoorNetworkDetectionEnabled);
                            }
                            WifiConnectivityMonitor.this.setWifiNetworkEnabled(true);
                            WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor3.transitionTo(wifiConnectivityMonitor3.mValidNonSwitchableState);
                        } else if (previousMode == 1 && (WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3)) {
                            WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, false);
                            WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor4.transitionTo(wifiConnectivityMonitor4.mValidSwitchableState);
                        } else if (WifiConnectivityMonitor.this.mCurrentMode == 0) {
                            WifiConnectivityMonitor.this.mNetworkStatsAnalyzer.sendEmptyMessage(WifiConnectivityMonitor.ACTIVITY_CHECK_STOP);
                            WifiConnectivityMonitor.access$13604(WifiConnectivityMonitor.this);
                            WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor5.transitionTo(wifiConnectivityMonitor5.mValidNoCheckState);
                        }
                    }
                    return true;
                case WifiConnectivityMonitor.REPORT_NETWORK_CONNECTIVITY /*135204*/:
                    return true;
                case WifiConnectivityMonitor.CMD_TRANSIT_ON_VALID /*135299*/:
                    if (WifiConnectivityMonitor.this.mCurrentMode == 0) {
                        WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor6.transitionTo(wifiConnectivityMonitor6.mValidNoCheckState);
                    } else if (WifiConnectivityMonitor.this.mCurrentMode == 1) {
                        WifiConnectivityMonitor wifiConnectivityMonitor7 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor7.transitionTo(wifiConnectivityMonitor7.mValidNonSwitchableState);
                    } else {
                        WifiConnectivityMonitor wifiConnectivityMonitor8 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor8.transitionTo(wifiConnectivityMonitor8.mValidSwitchableState);
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_QUALITY_CHECK_BY_SCORE /*135388*/:
                    WifiConnectivityMonitor.this.requestInternetCheck(27);
                    return true;
                case WifiConnectivityMonitor.CMD_REACHABILITY_LOST /*135400*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_REACHABILITY_LOST");
                    }
                    WifiConnectivityMonitor.this.requestInternetCheck(4);
                    if (WifiConnectivityMonitor.this.mCurrentBssid != null) {
                        WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnReachabilityLost();
                    }
                    return true;
                case WifiConnectivityMonitor.CMD_PROVISIONING_FAIL /*135401*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "CMD_PROVISIONING_FAIL");
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor9 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor9.reportNetworkConnectivityToNM(true, wifiConnectivityMonitor9.mInvalidationFailHistory.qcStepTemp, 3);
                    return true;
                default:
                    return false;
            }
        }
    }

    class ValidNonSwitchableState extends State {
        public int mMinQualifiedRssi = 0;

        ValidNonSwitchableState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.changeWifiIcon(1);
            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            WifiInfo unused = wifiConnectivityMonitor.mWifiInfo = wifiConnectivityMonitor.syncGetCurrentWifiInfo();
            int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
            switch (msg.what) {
                case WifiConnectivityMonitor.EVENT_SCREEN_OFF /*135177*/:
                    int unused2 = WifiConnectivityMonitor.this.mLossSampleCount = 0;
                    return false;
                case WifiConnectivityMonitor.REPORT_NETWORK_CONNECTIVITY /*135204*/:
                    WifiConnectivityMonitor.this.reportNetworkConnectivityToNM(msg.arg1, msg.arg2);
                    return true;
                case WifiConnectivityMonitor.VALIDATED_DETECTED /*135472*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED");
                    }
                    WifiConnectivityMonitor.this.setBigDataQualityCheck(true);
                    WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_RESET);
                    return true;
                case WifiConnectivityMonitor.INVALIDATED_DETECTED /*135473*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED");
                    }
                    WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                    WifiInfo unused3 = wifiConnectivityMonitor2.mWifiInfo = wifiConnectivityMonitor2.syncGetCurrentWifiInfo();
                    WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                    int unused4 = wifiConnectivityMonitor3.mInvalidationRssi = wifiConnectivityMonitor3.mWifiInfo.getRssi();
                    if (WifiConnectivityMonitor.this.mInvalidationRssi >= -55 && WifiConnectivityMonitor.this.mWifiInfo.getLinkSpeed() >= 54) {
                        WifiConnectivityMonitor.this.setValidationBlock(false);
                    }
                    WifiConnectivityMonitor.this.setBigDataValidationChanged();
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.error = 17;
                    WifiConnectivityMonitor.this.mInvalidationFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor4.setQcFailHistory(wifiConnectivityMonitor4.mInvalidationFailHistory);
                    boolean unused5 = WifiConnectivityMonitor.this.bSetQcResult = false;
                    if (WifiConnectivityMonitor.this.getLinkDetectMode() == 1) {
                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mGqInvNon++;
                    } else {
                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mPqInvNon++;
                    }
                    WifiConnectivityMonitor.this.setBigDataQCandNS(false);
                    WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_SECOND);
                    WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor5.transitionTo(wifiConnectivityMonitor5.mInvalidState);
                    long unused6 = WifiConnectivityMonitor.this.mInvalidStartTime = SystemClock.elapsedRealtime();
                    return true;
                default:
                    return false;
            }
        }
    }

    class ValidSwitchableState extends State {
        ValidSwitchableState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            boolean unused = WifiConnectivityMonitor.this.bSetQcResult = false;
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_TRANSIT_ON_SWITCHABLE);
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int i = msg.what;
            if (i != WifiConnectivityMonitor.REPORT_NETWORK_CONNECTIVITY) {
                if (i != WifiConnectivityMonitor.CMD_TRANSIT_ON_SWITCHABLE) {
                    switch (i) {
                        case WifiConnectivityMonitor.VALIDATED_DETECTED /*135472*/:
                        case WifiConnectivityMonitor.VALIDATED_DETECTED_AGAIN /*135474*/:
                            WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_RESET);
                            if (WifiConnectivityMonitor.DBG) {
                                if (msg.what == WifiConnectivityMonitor.VALIDATED_DETECTED) {
                                    Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED");
                                }
                                if (msg.what == WifiConnectivityMonitor.VALIDATED_DETECTED_AGAIN) {
                                    Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED_AGAIN");
                                }
                            }
                            if (WifiConnectivityMonitor.this.mCheckRoamedNetwork) {
                                boolean unused = WifiConnectivityMonitor.this.mCheckRoamedNetwork = false;
                                if (WifiConnectivityMonitor.this.getCurrentState() == WifiConnectivityMonitor.this.mLevel2State) {
                                    if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mPqGqNormal++;
                                    } else {
                                        WifiConnectivityMonitor.this.mSnsBigDataSCNT.mPqGqAgg++;
                                    }
                                    WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                                    wifiConnectivityMonitor.transitionTo(wifiConnectivityMonitor.mLevel1State);
                                }
                            }
                            WifiConnectivityMonitor.this.setBigDataQualityCheck(true);
                            return true;
                        case WifiConnectivityMonitor.INVALIDATED_DETECTED /*135473*/:
                        case WifiConnectivityMonitor.INVALIDATED_DETECTED_AGAIN /*135475*/:
                            if (WifiConnectivityMonitor.DBG) {
                                if (msg.what == WifiConnectivityMonitor.INVALIDATED_DETECTED) {
                                    Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED");
                                }
                                if (msg.what == WifiConnectivityMonitor.INVALIDATED_DETECTED_AGAIN) {
                                    Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED_AGAIN");
                                }
                            }
                            if (WifiConnectivityMonitor.this.mCheckRoamedNetwork) {
                                boolean unused2 = WifiConnectivityMonitor.this.mCheckRoamedNetwork = false;
                            }
                            WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                            WifiInfo unused3 = wifiConnectivityMonitor2.mWifiInfo = wifiConnectivityMonitor2.syncGetCurrentWifiInfo();
                            WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                            int unused4 = wifiConnectivityMonitor3.mInvalidationRssi = wifiConnectivityMonitor3.mWifiInfo.getRssi();
                            if (WifiConnectivityMonitor.this.mInvalidationRssi < -55 || WifiConnectivityMonitor.this.mWifiInfo.getLinkSpeed() < 54) {
                                int unused5 = WifiConnectivityMonitor.this.mOvercomingCount = 0;
                                WifiConnectivityMonitor.this.setValidationBlock(true);
                            } else {
                                WifiConnectivityMonitor.this.setValidationBlock(false);
                            }
                            if (msg.arg1 == 1) {
                                boolean unused6 = WifiConnectivityMonitor.this.mInvalidStateTesting = true;
                            }
                            WifiConnectivityMonitor.this.mInvalidationFailHistory.error = 17;
                            WifiConnectivityMonitor.this.mInvalidationFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                            WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor4.setQcFailHistory(wifiConnectivityMonitor4.mInvalidationFailHistory);
                            boolean unused7 = WifiConnectivityMonitor.this.bSetQcResult = false;
                            if (WifiConnectivityMonitor.this.getLinkDetectMode() == 1) {
                                if (WifiConnectivityMonitor.this.mCurrentMode == 2) {
                                    WifiConnectivityMonitor.this.mSnsBigDataSCNT.mGqInvNormal++;
                                } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                                    WifiConnectivityMonitor.this.mSnsBigDataSCNT.mGqInvAgg++;
                                }
                            }
                            WifiConnectivityMonitor.this.setBigDataQCandNS(false);
                            WifiConnectivityMonitor.this.setLoggingForTCPStat(WifiConnectivityMonitor.TCP_STAT_LOGGING_SECOND);
                            WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                            wifiConnectivityMonitor5.transitionTo(wifiConnectivityMonitor5.mInvalidState);
                            long unused8 = WifiConnectivityMonitor.this.mInvalidStartTime = SystemClock.elapsedRealtime();
                            return true;
                        default:
                            return false;
                    }
                } else {
                    if (WifiConnectivityMonitor.mLinkDetectMode == 1) {
                        WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor6.sendMessage(wifiConnectivityMonitor6.obtainMessage(WifiConnectivityMonitor.CMD_TRAFFIC_POLL, WifiConnectivityMonitor.access$16604(wifiConnectivityMonitor6), 0));
                        WifiConnectivityMonitor wifiConnectivityMonitor7 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor7.transitionTo(wifiConnectivityMonitor7.mLevel1State);
                    } else {
                        WifiConnectivityMonitor.this.checkTransitionToLevel2State();
                    }
                    return true;
                }
            } else if (WifiConnectivityMonitor.this.isMobileHotspot()) {
                return true;
            } else {
                if (WifiConnectivityMonitor.DBG) {
                    Log.i(WifiConnectivityMonitor.TAG, "[ValidSwitchable] REPORT_NETWORK_CONNECTIVITY");
                }
                WifiConnectivityMonitor.this.reportNetworkConnectivityToNM(msg.arg1, msg.arg2);
                return true;
            }
        }
    }

    class Level1State extends State {
        Level1State() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.setLinkDetectMode(1);
            if (WifiConnectivityMonitor.this.mCheckRoamedNetwork) {
                boolean unused = WifiConnectivityMonitor.this.mCheckRoamedNetwork = false;
            }
            boolean unused2 = WifiConnectivityMonitor.mIsPassedLevel1State = true;
            WifiConnectivityMonitor.this.setWifiNetworkEnabled(true);
            WifiConnectivityMonitor.this.changeWifiIcon(1);
            WifiConnectivityMonitor.this.startPoorQualityScoreCheck();
            WifiConnectivityMonitor.this.showNetworkSwitchedNotification(false);
            WifiConnectivityMonitor.this.determineMode();
            if (WifiConnectivityMonitor.this.mWifiEleStateTracker != null) {
                boolean unused3 = WifiConnectivityMonitor.this.mWifiNeedRecoveryFromEle = false;
                WifiConnectivityMonitor.this.mWifiEleStateTracker.enableEleMobileRssiPolling(true, false);
                WifiConnectivityMonitor.this.mWifiEleStateTracker.registerElePedometer();
            }
            if (WifiConnectivityMonitor.this.mCurrentMode != 0) {
                WifiConnectivityMonitor.this.mIWCChannel.sendMessage(IWCMonitor.TRANSIT_TO_VALID);
            }
            WifiConnectivityMonitor.this.sendBroadcastWCMStatusChanged();
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
            boolean unused = WifiConnectivityMonitor.this.mLevel2StateTransitionPending = false;
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            boolean roamFound = false;
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            switch (msg.what) {
                case WifiConnectivityMonitor.CMD_ELE_DETECTED /*135283*/:
                case WifiConnectivityMonitor.CMD_ELE_BY_GEO_DETECTED /*135284*/:
                    WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                    int unused = wifiConnectivityMonitor.mTransitionScore = wifiConnectivityMonitor.mClientModeImpl.getWifiScoreReport().getCurrentScore();
                    if (msg.what == WifiConnectivityMonitor.CMD_ELE_DETECTED) {
                        WifiConnectivityMonitor.this.mLowQualityFailHistory.error = 21;
                    } else {
                        WifiConnectivityMonitor.this.mLowQualityFailHistory.error = 22;
                    }
                    boolean unused2 = WifiConnectivityMonitor.this.bSetQcResult = false;
                    WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                    WifiInfo unused3 = wifiConnectivityMonitor2.mWifiInfo = wifiConnectivityMonitor2.syncGetCurrentWifiInfo();
                    WifiConnectivityMonitor.this.mCurrentBssid.poorLinkDetected(WifiConnectivityMonitor.this.mWifiInfo.getRssi(), 1);
                    WifiConnectivityMonitor.this.mLowQualityFailHistory.qcTrigger = 61;
                    WifiConnectivityMonitor.this.mLowQualityFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor3.setQcFailHistory(wifiConnectivityMonitor3.mLowQualityFailHistory);
                    WifiConnectivityMonitor.this.mSnsBigDataSCNT.mEleGP++;
                    WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                    wifiConnectivityMonitor4.transitionTo(wifiConnectivityMonitor4.mLevel2State);
                    return true;
                case WifiConnectivityMonitor.HANDLE_RESULT_ROAM_IN_HIGH_QUALITY /*135479*/:
                    boolean unused4 = WifiConnectivityMonitor.this.mLevel2StateTransitionPending = false;
                    if (msg.arg1 == 1) {
                        roamFound = true;
                    }
                    if (roamFound) {
                        Log.d(WifiConnectivityMonitor.TAG, "HANDLE_RESULT_ROAM_IN_HIGH_QUALITY - Roam target found");
                    } else {
                        Log.d(WifiConnectivityMonitor.TAG, "HANDLE_RESULT_ROAM_IN_HIGH_QUALITY - Roam target not found");
                        WifiConnectivityMonitor wifiConnectivityMonitor5 = WifiConnectivityMonitor.this;
                        WifiInfo unused5 = wifiConnectivityMonitor5.mWifiInfo = wifiConnectivityMonitor5.syncGetCurrentWifiInfo();
                        WifiConnectivityMonitor.this.mCurrentBssid.poorLinkDetected(WifiConnectivityMonitor.this.mWifiInfo.getRssi(), 1);
                        WifiConnectivityMonitor.this.mLowQualityFailHistory.error = 18;
                        WifiConnectivityMonitor.this.mLowQualityFailHistory.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                        WifiConnectivityMonitor wifiConnectivityMonitor6 = WifiConnectivityMonitor.this;
                        wifiConnectivityMonitor6.setQcFailHistory(wifiConnectivityMonitor6.mLowQualityFailHistory);
                        WifiConnectivityMonitor.this.checkTransitionToLevel2State();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class Level2State extends State {
        Level2State() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.setLinkDetectMode(0);
            WifiConnectivityMonitor.this.setWifiNetworkEnabled(false);
            WifiConnectivityMonitor.this.changeWifiIcon(0);
            if (WifiConnectivityMonitor.this.mWifiEleStateTracker == null || !WifiConnectivityMonitor.this.mWifiEleStateTracker.checkEleValidBlockState()) {
                WifiConnectivityMonitor.this.stopScoreQualityCheck();
            } else {
                WifiConnectivityMonitor.this.startRecoveryScoreCheck();
            }
            if (WifiConnectivityMonitor.this.mWifiEleStateTracker != null && !WifiConnectivityMonitor.this.mWifiEleStateTracker.checkEleValidBlockState()) {
                WifiConnectivityMonitor.this.mWifiEleStateTracker.delayedEleCheckDisable();
            }
            WifiConnectivityMonitor.this.mClientModeImpl.startScanFromWcm();
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, "Start scan to find alternative networks. " + WifiConnectivityMonitor.this.getCurrentState() + WifiConnectivityMonitor.this.getCurrentMode());
            }
            if (!WifiConnectivityMonitor.mIsECNTReportedConnection && WifiConnectivityMonitor.mIsPassedLevel1State) {
                WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                WifiInfo unused = wifiConnectivityMonitor.mWifiInfo = wifiConnectivityMonitor.syncGetCurrentWifiInfo();
                if ((WifiConnectivityMonitor.this.mWifiInfo.getNetworkId() != -1 ? WifiConnectivityMonitor.this.mWifiInfo.getRssi() : -61) > -60) {
                    WifiConnectivityMonitor.this.uploadNoInternetECNTBigData();
                }
            }
            WifiConnectivityMonitor.this.mIWCChannel.sendMessage(IWCMonitor.TRANSIT_TO_INVALID);
            WifiConnectivityMonitor.this.sendBroadcastWCMStatusChanged();
            WifiConnectivityMonitor.this.getClientModeChannel().unwantedMoreDump();
            WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnLevel2State(true);
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
            WifiConnectivityMonitor.this.mCurrentBssid.updateBssidQosMapOnLevel2State(false);
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            int i = msg.what;
            if (i == WifiConnectivityMonitor.EVENT_MOBILE_CONNECTED) {
                if (WifiConnectivityMonitor.mInitialResultSentToSystemUi && (WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3)) {
                    WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
                    WifiInfo unused = wifiConnectivityMonitor.mWifiInfo = wifiConnectivityMonitor.syncGetCurrentWifiInfo();
                    WifiConnectivityMonitor wifiConnectivityMonitor2 = WifiConnectivityMonitor.this;
                    int unused2 = wifiConnectivityMonitor2.mPoorConnectionDisconnectedNetId = wifiConnectivityMonitor2.mWifiInfo.getNetworkId();
                    if (WifiConnectivityMonitor.this.mNumOfToggled < 1) {
                        WifiConnectivityMonitor.this.showNetworkSwitchedNotification(true);
                    }
                    WifiConnectivityMonitor.this.sendMessageDelayed(WifiConnectivityMonitor.CMD_CHANGE_WIFI_ICON_VISIBILITY, 0);
                }
                return true;
            } else if (i != WifiConnectivityMonitor.CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE) {
                return false;
            } else {
                WifiConnectivityMonitor wifiConnectivityMonitor3 = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor3.sendMessage(wifiConnectivityMonitor3.obtainMessage(WifiConnectivityMonitor.CMD_TRAFFIC_POLL, WifiConnectivityMonitor.access$16604(wifiConnectivityMonitor3), 0));
                WifiConnectivityMonitor.this.mSnsBigDataSCNT.mElePG++;
                WifiConnectivityMonitor wifiConnectivityMonitor4 = WifiConnectivityMonitor.this;
                wifiConnectivityMonitor4.transitionTo(wifiConnectivityMonitor4.mLevel1State);
                return true;
            }
        }
    }

    class ValidNoCheckState extends State {
        ValidNoCheckState() {
        }

        public void enter() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName());
            }
            WifiConnectivityMonitor.this.changeWifiIcon(1);
        }

        public void exit() {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, getName() + " exit\n");
            }
        }

        public boolean processMessage(Message msg) {
            WifiConnectivityMonitor.this.logStateAndMessage(msg, this);
            WifiConnectivityMonitor.this.setLogOnlyTransitions(false);
            switch (msg.what) {
                case WifiConnectivityMonitor.VALIDATED_DETECTED /*135472*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "VALIDATED_DETECTED");
                    }
                    return true;
                case WifiConnectivityMonitor.INVALIDATED_DETECTED /*135473*/:
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "INVALIDATED_DETECTED");
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateSettings() {
        WifiInfo info;
        if (DBG) {
            Log.d(TAG, "Updating secure settings");
        }
        if (sWifiOnly) {
            Log.d(TAG, "Disabling poor network avoidance for wi-fi only device");
            this.mPoorNetworkDetectionEnabled = false;
        } else {
            updatePoorNetworkParameters();
            if (!isConnectedState()) {
                info = null;
            } else {
                info = syncGetCurrentWifiInfo();
            }
            loge("current state: " + getCurrentState());
            if (info != null && info.getNetworkId() != -1 && this.mPoorNetworkDetectionEnabled && isQCExceptionOnly()) {
                if (DBG) {
                    Log.e(TAG, "updatePoorNetworkDetection = false because it is an QCExceptionOnly");
                }
                this.mPoorNetworkDetectionEnabled = false;
            }
            if (!this.mPoorNetworkDetectionEnabled || !this.mUIEnabled) {
                sendMessage(EVENT_LINK_DETECTION_DISABLED);
            } else if (DBG) {
                this.mParam.setEvaluationParameters();
            }
        }
        if (!DBG) {
            return;
        }
        if (isAggressiveModeSupported()) {
            Log.d(TAG, "Updating secure settings - mPoorNetworkDetectionEnabled/mUIEnabled/mAggressiveModeEnabled : " + this.mPoorNetworkDetectionEnabled + "/" + this.mUIEnabled + "/" + this.mAggressiveModeEnabled + this.mParam.getAggressiveModeFeatureStatus());
            return;
        }
        Log.d(TAG, "Updating secure settings - mPoorNetworkDetectionEnabled/mUIEnabled : " + this.mPoorNetworkDetectionEnabled + "/" + this.mUIEnabled);
    }

    /* access modifiers changed from: private */
    public void determineMode() {
        int i;
        String ssid = this.mWifiInfo.getSSID();
        if (this.mCurrentMode != 0) {
            if (isIgnorableNetwork(ssid)) {
                setCurrentMode(0);
            } else if (!this.mPoorNetworkDetectionEnabled) {
                setCurrentMode(1);
            } else if (isQCExceptionOnly()) {
                if (SMARTCM_DBG) {
                    logi("isQCExceptionOnly");
                }
                setCurrentMode(1);
            } else if (isAggressiveModeEnabled()) {
                if (SMARTCM_DBG) {
                    logi("mAggressiveModeEnabled");
                }
                setCurrentMode(3);
            } else {
                setCurrentMode(2);
            }
        }
        String currentMode = null;
        int i2 = this.mCurrentMode;
        if (i2 == 2 || i2 == 3) {
            currentMode = "1";
        }
        boolean networkAvoidBadWifi = "1".equals(Settings.Global.getString(this.mContext.getContentResolver(), "network_avoid_bad_wifi"));
        if ((!networkAvoidBadWifi && ((i = this.mCurrentMode) == 2 || i == 3)) || (networkAvoidBadWifi && this.mCurrentMode == 1)) {
            Settings.Global.putString(this.mContext.getContentResolver(), "network_avoid_bad_wifi", currentMode);
        }
        if (DBG) {
            logi("current mode : " + this.mCurrentMode);
        }
    }

    private void setCurrentMode(int setMode) {
        this.mCurrentMode = setMode;
        int i = this.mCurrentMode;
        if ((i == 2 || i == 3) && isValidState() && getCurrentState() != this.mLevel2State) {
            this.mSamplingIntervalMS = LINK_MONITORING_SAMPLING_INTERVAL_MS;
            WifiEleStateTracker wifiEleStateTracker = this.mWifiEleStateTracker;
            if ((wifiEleStateTracker == null || !wifiEleStateTracker.getEleCheckEnabled()) && this.mIwcCurrentQai == 3) {
                this.mSamplingIntervalMS = LINK_SAMPLING_INTERVAL_MS;
            }
            this.mScoreSkipModeEnalbed = true;
        } else {
            this.mSamplingIntervalMS = LINK_SAMPLING_INTERVAL_MS;
            this.mScoreSkipModeEnalbed = false;
        }
        WifiEleStateTracker wifiEleStateTracker2 = this.mWifiEleStateTracker;
        if (wifiEleStateTracker2 == null) {
            return;
        }
        if (this.mCurrentMode == 3) {
            wifiEleStateTracker2.setEleAggTxBadDetection(true);
        } else {
            wifiEleStateTracker2.setEleAggTxBadDetection(false);
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02c6  */
    /* JADX WARNING: Removed duplicated region for block: B:179:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updatePoorNetworkParameters() {
        /*
            r18 = this;
            r0 = r18
            android.telephony.TelephonyManager r1 = r0.mTelephonyManager
            if (r1 != 0) goto L_0x0012
            android.content.Context r1 = r0.mContext
            java.lang.String r2 = "phone"
            java.lang.Object r1 = r1.getSystemService(r2)
            android.telephony.TelephonyManager r1 = (android.telephony.TelephonyManager) r1
            r0.mTelephonyManager = r1
        L_0x0012:
            android.net.ConnectivityManager r1 = r18.getCm()
            boolean r1 = r1.semIsMobilePolicyDataEnabled()
            r0.mMobilePolicyDataEnable = r1
            boolean r1 = r0.mUIEnabled
            boolean r2 = r0.mAggressiveModeEnabled
            r3 = 0
            r4 = 0
            android.telephony.TelephonyManager r5 = r0.mTelephonyManager
            r6 = 0
            r7 = 1
            if (r5 == 0) goto L_0x0032
            int r5 = r5.getPhoneCount()
            if (r5 <= r7) goto L_0x0030
            r5 = r7
            goto L_0x0031
        L_0x0030:
            r5 = r6
        L_0x0031:
            r4 = r5
        L_0x0032:
            android.telephony.TelephonyManager r5 = r0.mTelephonyManager
            java.lang.String r8 = "WifiConnectivityMonitor"
            if (r5 != 0) goto L_0x003a
            r3 = 0
            goto L_0x0081
        L_0x003a:
            android.telephony.TelephonyManager r5 = android.telephony.TelephonyManager.getDefault()
            int r5 = r5.getPhoneCount()
            if (r5 > r7) goto L_0x004e
            if (r4 == 0) goto L_0x0047
            goto L_0x004e
        L_0x0047:
            android.telephony.TelephonyManager r5 = r0.mTelephonyManager
            int r3 = r5.getSimState()
            goto L_0x0081
        L_0x004e:
            android.telephony.TelephonyManager r5 = r0.mTelephonyManager
            int r5 = com.android.server.wifi.util.TelephonyUtil.semGetMultiSimState(r5)
            if (r5 == r7) goto L_0x005f
            r9 = 2
            if (r5 == r9) goto L_0x005f
            r9 = 3
            if (r5 != r9) goto L_0x005d
            goto L_0x005f
        L_0x005d:
            r3 = 0
            goto L_0x0060
        L_0x005f:
            r3 = 5
        L_0x0060:
            boolean r9 = DBG
            if (r9 == 0) goto L_0x0080
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r10 = "multiSimState : "
            r9.append(r10)
            r9.append(r5)
            java.lang.String r10 = "simState = "
            r9.append(r10)
            r9.append(r3)
            java.lang.String r9 = r9.toString()
            android.util.Log.d(r8, r9)
        L_0x0080:
        L_0x0081:
            android.content.Context r5 = r0.mContext
            android.content.ContentResolver r5 = r5.getContentResolver()
            java.lang.String r9 = "wifi_watchdog_poor_network_test_enabled"
            int r5 = android.provider.Settings.Global.getInt(r5, r9, r6)
            if (r5 == 0) goto L_0x0091
            r5 = r7
            goto L_0x0092
        L_0x0091:
            r5 = r6
        L_0x0092:
            r0.mUIEnabled = r5
            boolean r5 = r18.isAggressiveModeSupported()
            if (r5 == 0) goto L_0x00df
            android.content.Context r5 = r0.mContext
            android.content.ContentResolver r5 = r5.getContentResolver()
            java.lang.String r10 = "wifi_watchdog_poor_network_aggressive_mode_on"
            int r5 = android.provider.Settings.Global.getInt(r5, r10, r6)
            if (r5 == 0) goto L_0x00aa
            r5 = r7
            goto L_0x00ab
        L_0x00aa:
            r5 = r6
        L_0x00ab:
            r0.mAggressiveModeEnabled = r5
            boolean r5 = r0.mAggressiveModeEnabled
            if (r5 == r2) goto L_0x00df
            boolean r5 = SMARTCM_DBG
            if (r5 == 0) goto L_0x00d1
            android.content.Context r5 = r0.mContext
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = "[@_ON] : "
            r10.append(r11)
            boolean r11 = r0.mAggressiveModeEnabled
            r10.append(r11)
            java.lang.String r10 = r10.toString()
            android.widget.Toast r5 = android.widget.Toast.makeText(r5, r10, r6)
            r5.show()
        L_0x00d1:
            com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r5 = r0.mParam
            java.util.Objects.requireNonNull(r5)
            boolean r5 = r0.isAggressiveModeEnabled(r7)
            if (r5 == 0) goto L_0x00df
            r0.updateTargetRssiForCurrentAP(r7)
        L_0x00df:
            android.content.Context r5 = r0.mContext
            android.content.ContentResolver r5 = r5.getContentResolver()
            java.lang.String r10 = "mobile_data"
            int r5 = android.provider.Settings.Global.getInt(r5, r10, r7)
            if (r5 == 0) goto L_0x00ef
            r5 = r7
            goto L_0x00f0
        L_0x00ef:
            r5 = r6
        L_0x00f0:
            r10 = 0
            android.content.Context r11 = r0.mContext
            android.content.ContentResolver r11 = r11.getContentResolver()
            java.lang.String r12 = "ultra_powersaving_mode"
            int r11 = android.provider.Settings.System.getInt(r11, r12, r6)
            if (r11 != r7) goto L_0x0100
            r10 = 1
        L_0x0100:
            android.content.Context r11 = r0.mContext
            android.content.ContentResolver r11 = r11.getContentResolver()
            java.lang.String r12 = "airplane_mode_on"
            int r11 = android.provider.Settings.Global.getInt(r11, r12, r6)
            if (r11 != 0) goto L_0x0118
            if (r5 == 0) goto L_0x0118
            boolean r11 = r0.mIsFmcNetwork
            if (r11 != 0) goto L_0x0118
            if (r10 != 0) goto L_0x0118
            r11 = r7
            goto L_0x0119
        L_0x0118:
            r11 = r6
        L_0x0119:
            r0.mPoorNetworkDetectionEnabled = r11
            boolean r11 = r18.isSimCheck()
            r13 = 5
            if (r11 == 0) goto L_0x0131
            boolean r11 = r0.mPoorNetworkDetectionEnabled
            if (r11 == 0) goto L_0x012e
            if (r3 != r13) goto L_0x012e
            boolean r11 = r0.mMobilePolicyDataEnable
            if (r11 == 0) goto L_0x012e
            r11 = r7
            goto L_0x012f
        L_0x012e:
            r11 = r6
        L_0x012f:
            r0.mPoorNetworkDetectionEnabled = r11
        L_0x0131:
            android.content.Context r11 = r0.mContext
            android.content.ContentResolver r11 = r11.getContentResolver()
            java.lang.String r14 = "wifi_wwsm_patch_remove_sns_menu_from_settings"
            int r11 = android.provider.Settings.Secure.getInt(r11, r14, r6)
            if (r11 != 0) goto L_0x0141
            r11 = r7
            goto L_0x0142
        L_0x0141:
            r11 = r6
        L_0x0142:
            boolean r14 = r0.mPoorNetworkDetectionEnabled
            if (r14 == 0) goto L_0x0152
            boolean r14 = r0.mUserOwner
            if (r14 == 0) goto L_0x0152
            boolean r14 = r0.mImsRegistered
            if (r14 != 0) goto L_0x0152
            if (r11 == 0) goto L_0x0152
            r14 = r7
            goto L_0x0153
        L_0x0152:
            r14 = r6
        L_0x0153:
            r0.mPoorNetworkDetectionEnabled = r14
            boolean r14 = r0.mPoorNetworkDetectionEnabled
            if (r14 == 0) goto L_0x015f
            boolean r14 = r0.mUIEnabled
            if (r14 == 0) goto L_0x015f
            r14 = r7
            goto L_0x0160
        L_0x015f:
            r14 = r6
        L_0x0160:
            r0.mPoorNetworkDetectionEnabled = r14
            android.content.Context r14 = r0.mContext
            android.content.ContentResolver r14 = r14.getContentResolver()
            java.lang.String r15 = "data_roaming"
            int r14 = android.provider.Settings.Global.getInt(r14, r15, r6)
            if (r14 != r7) goto L_0x0172
            r14 = r7
            goto L_0x0173
        L_0x0172:
            r14 = r6
        L_0x0173:
            r0.mDataRoamingSetting = r14
            boolean r14 = r0.mDataRoamingState
            boolean r15 = r0.mPoorNetworkDetectionEnabled
            if (r15 == 0) goto L_0x017f
            if (r14 != 0) goto L_0x017f
            r15 = r7
            goto L_0x0180
        L_0x017f:
            r15 = r6
        L_0x0180:
            r0.mPoorNetworkDetectionEnabled = r15
            boolean r15 = DBG
            java.lang.String r13 = "/"
            if (r15 != 0) goto L_0x018a
            if (r14 == 0) goto L_0x01b3
        L_0x018a:
            java.lang.StringBuilder r15 = new java.lang.StringBuilder
            r15.<init>()
            java.lang.String r6 = "mDataRoamingState / !mDataRoamingSetting : "
            r15.append(r6)
            boolean r6 = r0.mDataRoamingState
            r15.append(r6)
            r15.append(r13)
            boolean r6 = r0.mDataRoamingSetting
            r6 = r6 ^ r7
            r15.append(r6)
            if (r14 == 0) goto L_0x01a7
            java.lang.String r6 = "     (Mobile data blocked by Data roaming condition)"
            goto L_0x01a9
        L_0x01a7:
            java.lang.String r6 = ""
        L_0x01a9:
            r15.append(r6)
            java.lang.String r6 = r15.toString()
            android.util.Log.d(r8, r6)
        L_0x01b3:
            if (r1 != 0) goto L_0x01c8
            boolean r6 = r0.mUIEnabled
            if (r6 == 0) goto L_0x01c8
            boolean r6 = r0.mPoorNetworkDetectionEnabled
            if (r6 == 0) goto L_0x01c8
            long r16 = android.os.SystemClock.elapsedRealtime()
            mPoorNetworkAvoidanceEnabledTime = r16
            java.lang.String r6 = "SNS turned on. Do not start scan for a while."
            android.util.Log.d(r8, r6)
        L_0x01c8:
            boolean r6 = r0.mPoorNetworkDetectionEnabled
            if (r6 != 0) goto L_0x01cf
            r6 = 0
            r0.mTransitionPendingByIwc = r6
        L_0x01cf:
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService r6 = r0.mWifiSwitchForIndividualAppsService
            if (r6 == 0) goto L_0x0202
            boolean r6 = r0.mReportedPoorNetworkDetectionEnabled
            boolean r15 = r0.mPoorNetworkDetectionEnabled
            if (r6 == r15) goto L_0x01ec
            r0.mReportedPoorNetworkDetectionEnabled = r15
            boolean r6 = r0.mReportedPoorNetworkDetectionEnabled
            if (r6 != r7) goto L_0x01e3
            r6 = 135678(0x211fe, float:1.90125E-40)
            goto L_0x01e6
        L_0x01e3:
            r6 = 135679(0x211ff, float:1.90127E-40)
        L_0x01e6:
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService r15 = r0.mWifiSwitchForIndividualAppsService
            r15.sendEmptyMessage(r6)
        L_0x01ec:
            int r6 = r0.mReportedQai
            int r15 = r0.mIwcCurrentQai
            if (r6 == r15) goto L_0x0202
            r0.mReportedQai = r15
            com.android.server.wifi.tcp.WifiSwitchForIndividualAppsService r6 = r0.mWifiSwitchForIndividualAppsService
            r15 = 135680(0x21200, float:1.90128E-40)
            int r7 = r0.mReportedQai
            android.os.Message r7 = r0.obtainMessage(r15, r7)
            r6.sendMessage(r7)
        L_0x0202:
            android.content.Context r6 = r0.mContext
            android.content.ContentResolver r6 = r6.getContentResolver()
            r7 = 0
            int r6 = android.provider.Settings.Global.getInt(r6, r12, r7)
            if (r6 == 0) goto L_0x0211
            r6 = 1
            goto L_0x0212
        L_0x0211:
            r6 = 0
        L_0x0212:
            r0.mAirPlaneMode = r6
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            android.content.Context r7 = r0.mContext
            android.content.ContentResolver r7 = r7.getContentResolver()
            r12 = 0
            int r7 = android.provider.Settings.Global.getInt(r7, r9, r12)
            if (r7 == 0) goto L_0x0228
            r7 = 1
            goto L_0x0229
        L_0x0228:
            r7 = 0
        L_0x0229:
            r6.append(r7)
            r6.append(r13)
            boolean r7 = r0.mIsFmcNetwork
            r12 = 1
            r7 = r7 ^ r12
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            r0.mPoorNetworkAvoidanceSummary = r6
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            boolean r7 = r0.mAirPlaneMode
            r12 = 1
            r7 = r7 ^ r12
            r6.append(r7)
            r6.append(r13)
            r6.append(r5)
            r6.append(r13)
            r7 = 5
            if (r3 != r7) goto L_0x0256
            r7 = 1
            goto L_0x0257
        L_0x0256:
            r7 = 0
        L_0x0257:
            r6.append(r7)
            r6.append(r13)
            boolean r7 = r0.mMobilePolicyDataEnable
            r6.append(r7)
            r6.append(r13)
            boolean r7 = r0.mIsFmcNetwork
            r12 = 1
            r7 = r7 ^ r12
            r6.append(r7)
            r6.append(r13)
            boolean r7 = r0.mUserOwner
            r6.append(r7)
            r6.append(r13)
            boolean r7 = r0.mImsRegistered
            r7 = r7 ^ r12
            r6.append(r7)
            r6.append(r13)
            if (r10 != 0) goto L_0x0284
            r7 = 1
            goto L_0x0285
        L_0x0284:
            r7 = 0
        L_0x0285:
            r6.append(r7)
            r6.append(r13)
            r7 = r14 ^ 1
            r6.append(r7)
            r6.append(r13)
            r6.append(r11)
            java.lang.String r6 = r6.toString()
            r0.mPoorNetworkDetectionSummary = r6
            boolean r6 = r0.mAirPlaneMode
            if (r6 != 0) goto L_0x02be
            r6 = 5
            if (r3 != r6) goto L_0x02be
            if (r5 == 0) goto L_0x02be
            java.lang.String r6 = CSC_VENDOR_NOTI_STYLE
            java.lang.String r7 = "CMCC"
            boolean r6 = r7.equals(r6)
            if (r6 == 0) goto L_0x02bc
            boolean r6 = DBG
            if (r6 == 0) goto L_0x02b8
            java.lang.String r6 = "CMCC Alert is support"
            android.util.Log.d(r8, r6)
        L_0x02b8:
            r7 = 1
            r0.mCmccAlertSupport = r7
            goto L_0x02c2
        L_0x02bc:
            r7 = 1
            goto L_0x02bf
        L_0x02be:
            r7 = 1
        L_0x02bf:
            r6 = 0
            r0.mCmccAlertSupport = r6
        L_0x02c2:
            boolean r6 = DBG
            if (r6 == 0) goto L_0x0368
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r12 = "updatePoorNetworkAvoidance - Poor Network Test Enabled / !mIsFmcNetwork : "
            r6.append(r12)
            java.lang.String r12 = r0.mPoorNetworkAvoidanceSummary
            r6.append(r12)
            java.lang.String r12 = " - mUIEnabled:"
            r6.append(r12)
            boolean r12 = r0.mUIEnabled
            java.lang.String r13 = "enabled"
            java.lang.String r15 = "disabled"
            if (r12 == 0) goto L_0x02e4
            r12 = r13
            goto L_0x02e5
        L_0x02e4:
            r12 = r15
        L_0x02e5:
            r6.append(r12)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r8, r6)
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r12 = "updatePoorNetworkDetection - Airplane Mode Off / Mobile Data Enabled / SIM State-Ready / MobilePolicyDataDisabled / !mIsFmcNetwork / mUserOwner / !mImsRegistered / isEnabledUltraSaving / !mobileDataBlockedByRoaming /snsDisabled: "
            r6.append(r12)
            java.lang.String r12 = r0.mPoorNetworkDetectionSummary
            r6.append(r12)
            java.lang.String r12 = " - mPoorNetworkDetectionEnabled: "
            r6.append(r12)
            boolean r12 = r0.mPoorNetworkDetectionEnabled
            if (r12 == 0) goto L_0x0309
            r12 = r13
            goto L_0x030a
        L_0x0309:
            r12 = r15
        L_0x030a:
            r6.append(r12)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r8, r6)
            boolean r6 = r18.isAggressiveModeSupported()
            if (r6 == 0) goto L_0x0352
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r12 = "WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED: "
            r6.append(r12)
            android.content.Context r12 = r0.mContext
            android.content.ContentResolver r12 = r12.getContentResolver()
            r7 = 0
            int r9 = android.provider.Settings.Global.getInt(r12, r9, r7)
            if (r9 == 0) goto L_0x0332
            r7 = 1
        L_0x0332:
            r6.append(r7)
            java.lang.String r7 = " - mAggressiveModeEnabled:"
            r6.append(r7)
            boolean r7 = r0.mAggressiveModeEnabled
            if (r7 == 0) goto L_0x033f
            r15 = r13
        L_0x033f:
            r6.append(r15)
            com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r7 = r0.mParam
            java.lang.String r7 = r7.getAggressiveModeFeatureStatus()
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r8, r6)
        L_0x0352:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "mIwcCurrentQai: "
            r6.append(r7)
            int r7 = r0.mIwcCurrentQai
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            android.util.Log.d(r8, r6)
        L_0x0368:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.updatePoorNetworkParameters():void");
    }

    public void resetWatchdogSettings() {
        sendMessage(EVENT_WATCHDOG_SETTINGS_CHANGE);
        requestInternetCheck(5);
    }

    private boolean isSimCheck() {
        if (!DBG || !SystemProperties.get("SimCheck.disable").equals("1")) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public synchronized void setQcFailHistory(QcFailHistory history) {
        setQcFailHistory(history, (String) null);
    }

    private synchronized void setQcFailHistory(QcFailHistory history, String dumpLog) {
        StringBuilder builder = new StringBuilder();
        if (!this.bSetQcResult) {
            if (this.mQcHistoryHead == -1) {
                this.mQcHistoryHead++;
            } else {
                this.mQcHistoryHead %= 30;
            }
            String currentTime = "" + (System.currentTimeMillis() / 1000);
            try {
                currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            } catch (RuntimeException e) {
            }
            try {
                builder.append(currentTime);
                builder.append(", " + getCurrentState().getName());
                if (dumpLog == null) {
                    builder.append(", [s]" + history.qcStep);
                    builder.append(", [t]" + history.qcTrigger);
                    builder.append(", [e]" + history.error);
                    builder.append(", [i]" + this.mInvalidationRssi);
                    builder.append(", [v]" + SNS_VERSION);
                    builder.append(", " + this.mCurrentRssi);
                    builder.append(", " + this.mCurrentLinkSpeed);
                    builder.append(", " + this.mCurrentBssid.mGoodLinkTargetRssi);
                    builder.append(", " + this.mPoorNetworkDetectionEnabled + "(" + this.mPoorNetworkDetectionSummary + ")");
                    builder.append(", " + this.mUIEnabled + "(" + this.mPoorNetworkAvoidanceSummary + ")");
                    if (isAggressiveModeSupported()) {
                        builder.append(", " + this.mAggressiveModeEnabled + this.mParam.getAggressiveModeFeatureStatus());
                    }
                    WifiInfo wifiInfo = syncGetCurrentWifiInfo();
                    String ssid = wifiInfo.getSSID();
                    String bssid = wifiInfo.getBSSID();
                    builder.append(", " + wifiInfo.getNetworkId());
                    String hexSsid = "";
                    for (int j = 0; j < ssid.length(); j++) {
                        hexSsid = hexSsid + Integer.toHexString(ssid.charAt(j));
                    }
                    builder.append(", " + hexSsid);
                    if (bssid.length() > 16) {
                        StringBuilder patchedBssid = new StringBuilder();
                        patchedBssid.append(bssid.charAt(0));
                        patchedBssid.append(bssid.charAt(1));
                        patchedBssid.append(bssid.charAt(12));
                        patchedBssid.append(bssid.charAt(13));
                        patchedBssid.append(bssid.charAt(15));
                        patchedBssid.append(bssid.charAt(16));
                        builder.append(", " + patchedBssid.toString());
                    }
                    builder.append(", " + history.line);
                    if (getCurrentState() == this.mValidSwitchableState) {
                        builder.append(", [ns]" + getNetworkStatHistory());
                    }
                } else {
                    builder.append(", " + dumpLog);
                }
            } catch (RuntimeException e2) {
                builder.append(currentTime + ", ex");
            }
            Log.i(TAG, builder.toString());
            this.mQcDumpHistory[this.mQcHistoryHead] = builder.toString();
            this.bSetQcResult = true;
            this.mQcHistoryHead++;
            this.mQcHistoryTotal++;
        }
    }

    private boolean isAggressiveModeEnabled() {
        int i = this.mIwcCurrentQai;
        if (i == 1) {
            return true;
        }
        if (i == 2 || i == 3) {
            return false;
        }
        if (OpBrandingLoader.Vendor.KTT == mOpBranding) {
            if (!isAggressiveModeSupported() || !this.mPoorNetworkDetectionEnabled || (!this.mAggressiveModeEnabled && !this.mMptcpEnabled)) {
                return false;
            }
            return true;
        } else if (!isAggressiveModeSupported() || !this.mPoorNetworkDetectionEnabled || !this.mAggressiveModeEnabled) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isAggressiveModeEnabled(int feature) {
        return isAggressiveModeEnabled() && this.mParam.mAggressiveModeFeatureEnabled[feature];
    }

    private boolean isAggressiveModeSupported() {
        return true;
    }

    private boolean isQCExceptionOnly() {
        String ssid = syncGetCurrentWifiInfo().getSSID();
        int result = -1;
        if (isSpecificPackageOnScreen(this.mContext)) {
            result = 1;
        } else if (isSkipInternetCheck()) {
            result = 2;
        } else if ("\"gogoinflight\"".equals(ssid) || "\"Carnival-WiFi\"".equals(ssid) || "\"orange\"".equals(ssid) || "\"ChinaNet\"".equals(ssid)) {
            result = 3;
        } else if (this.m407ResponseReceived) {
            result = 4;
        } else if (this.mIsFmcNetwork) {
            result = 5;
        }
        if (result != -1) {
            this.isQCExceptionSummary = "" + result;
            StringBuilder sb = new StringBuilder();
            sb.append(DBG ? "isQCExceptionOnly - reason #" : "QCEO #");
            sb.append(result);
            logd(sb.toString());
            return true;
        }
        this.isQCExceptionSummary = "None";
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isSkipInternetCheck() {
        int networkId;
        WifiInfo wifiInfo = this.mWifiInfo;
        if (wifiInfo == null || (networkId = wifiInfo.getNetworkId()) == -1) {
            return false;
        }
        WifiConfiguration wifiConfiguration = getWifiConfiguration(networkId);
        if (wifiConfiguration != null) {
            return wifiConfiguration.isNoInternetAccessExpected();
        }
        Log.d(TAG, "isSkipInternetCheck - config == null");
        return false;
    }

    /* access modifiers changed from: private */
    public int calculateSignalLevel(int rssi) {
        int signalLevel = WifiManager.calculateSignalLevel(rssi, 5);
        if (DBG) {
            Log.d(TAG, "RSSI current: " + this.mCurrentSignalLevel + " new: " + rssi + ", " + signalLevel);
        }
        return signalLevel;
    }

    private boolean isSpecificPackageOnScreen(Context context) {
        for (ActivityManager.RunningTaskInfo runningTaskInfo : ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1)) {
            if (DBG) {
                Log.d(TAG, "isSpecificPackageOnScreen: top:" + runningTaskInfo.topActivity.getClassName());
            }
            if (runningTaskInfo.topActivity.getPackageName().equals("com.akazam.android.wlandialer")) {
                if (DBG) {
                    Log.i(TAG, " Specific Package(com.akazam.android.wlandialer) is on SCREEN! ");
                }
                return true;
            }
        }
        return false;
    }

    private boolean isSpecificPackageOnScreen(Context context, String specificPakageName) {
        if (specificPakageName == null || specificPakageName.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo runningTaskInfo : ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1)) {
            if (DBG) {
                Log.d(TAG, "isSpecificPackageOnScreen: top:" + runningTaskInfo.topActivity.getClassName());
            }
            if (runningTaskInfo.topActivity.getClassName().equals(specificPakageName)) {
                if (DBG) {
                    Log.i(TAG, " Specific Package(" + specificPakageName + ") is on SCREEN! ");
                }
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isValidState() {
        IState state = getCurrentState();
        if (state == this.mValidState || state == this.mValidNonSwitchableState || state == this.mValidSwitchableState || state == this.mValidNoCheckState || state == this.mLevel1State || state == this.mLevel2State) {
            if (!DBG) {
                return true;
            }
            Log.d(TAG, "In Valid state");
            return true;
        } else if (!DBG) {
            return false;
        } else {
            Log.d(TAG, "Not in Valid state");
            return false;
        }
    }

    private boolean isLevel1StateState() {
        if (getCurrentState() == this.mLevel1State) {
            if (!DBG) {
                return true;
            }
            Log.d(TAG, "In Level1State state");
            return true;
        } else if (!DBG) {
            return false;
        } else {
            Log.d(TAG, "Not in Level1State state");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean isInvalidState() {
        if (getCurrentState() == this.mInvalidState) {
            if (!DBG) {
                return true;
            }
            Log.d(TAG, "In Invalid state");
            return true;
        } else if (!DBG) {
            return false;
        } else {
            Log.d(TAG, "Not in Invalid state");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void updateCurrentBssid(String bssid, int netId) {
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Update current BSSID to ");
            sb.append(bssid != null ? bssid : "null");
            Log.d(TAG, sb.toString());
        }
        synchronized (mCurrentBssidLock) {
            if (bssid == null) {
                this.mCurrentBssid = this.mEmptyBssid;
                if (DBG) {
                    Log.d(TAG, "BSSID changed");
                }
                this.mValidNonSwitchableState.mMinQualifiedRssi = 0;
                sendMessage(EVENT_BSSID_CHANGE);
            } else if (this.mCurrentBssid == null || !bssid.equals(this.mCurrentBssid.mBssid)) {
                BssidStatistics currentBssid = this.mBssidCache.get(bssid);
                if (currentBssid == null) {
                    currentBssid = new BssidStatistics(bssid, netId);
                    this.mBssidCache.put(bssid, currentBssid);
                }
                this.mCurrentBssid = currentBssid;
                this.mCurrentBssid.initOnConnect();
                updateOpenNetworkQosScoreSummary();
                if (DBG) {
                    Log.d(TAG, "BSSID changed");
                }
                this.mValidNonSwitchableState.mMinQualifiedRssi = 0;
                sendMessage(EVENT_BSSID_CHANGE);
            }
        }
    }

    /* access modifiers changed from: private */
    public void requestInternetCheck(int trigger) {
        requestInternetCheck(this.mInvalidationFailHistory.qcStepTemp, trigger);
    }

    /* access modifiers changed from: private */
    public void requestInternetCheck(int step, int trigger) {
        WifiEleStateTracker wifiEleStateTracker = this.mWifiEleStateTracker;
        if (wifiEleStateTracker != null && wifiEleStateTracker.checkEleValidBlockState()) {
            Log.d(TAG, "REPORT_NETWORK_CONNECTIVITY ignored by ele block");
        } else if (this.mIsInRoamSession || this.mIsInDhcpSession) {
            Log.d(TAG, "REPORT_NETWORK_CONNECTIVITY ignored In Roam Session");
        } else if (syncGetCurrentWifiInfo().getRssi() >= -55 || !this.mIsRoamingNetwork) {
            sendMessage(REPORT_NETWORK_CONNECTIVITY, step, trigger);
        } else {
            Log.d(TAG, "REPORT_NETWORK_CONNECTIVITY ignored by possible roaming");
        }
    }

    private class NetworkStatsAnalyzer extends Handler {
        private static final int ACTIVITY_POLLING_INTERVAL = 1000;
        private static final int BACKHAUL_DETECTION_REASON_DNS_CHECK_INTERVAL = 16;
        private static final int BACKHAUL_DETECTION_REASON_IN_ERROR_SEG_FAIL = 4;
        private static final int BACKHAUL_DETECTION_REASON_NO_RESPONSE_FAIL = 8;
        private static final int BACKHAUL_DETECTION_REASON_RETRANS_SEG_FAIL = 2;
        private static final int BACKHAUL_DETECTION_REASON_TCP_SOCKET_FAIL = 1;
        private static final int GOOD_RX_VALID_DURATION = 300000;
        private static final int LEAST_AGGRESSIVE_MODE_HIGH_THRESHOLD_INTERVAL = 300000;
        private static final int LEAST_AGGRESSIVE_MODE_QC_INTERVAL = 20000;
        private static final int MAX_OPTION_TARGET_RSSI_DELTA = 5;
        private static final int NETSTATS_INIT_DELAY_TIME = 20000;
        private static final int OPTION_RSSI_INCREMENT_INTERVAL = 60000;
        public static final int QC_PASS_INCREASE_WAITINGCYCLE = 3;
        private static final int RSSI_AVERAGE_WINDOW_SIZE = 3;
        private static final int RSSI_THRESHOLD_LOW_HIGH_DELTA = 3;
        private static final String TAG = "WifiConnectivityMonitor.NetworkStatsAnalyzer";
        private final String FILE_NAME_SNMP = "/proc/net/snmp";
        private final String FILE_NAME_SOCKSTAT_IPV4 = "/proc/net/sockstat";
        private final String FILE_NAME_SOCKSTAT_IPV6 = "/proc/net/sockstat6";
        private final int RSSI_LOW_SIGNAL_THRESHOLD = -70;
        private final int RSSI_POOR_SIGNAL_THRESHOLD = -83;
        private final int RSSI_QC_TRIGGER_INTERVAL = -3;
        private final int SNMP_TCP_COUNT_ROW = 6;
        private final int SNMP_TCP_ESTABLISH_COUNT_COLOUMN = 9;
        private final int SNMP_TCP_IN_SEG_COUNT_COLOUMN = 10;
        private final int SNMP_TCP_IN_SEG_ERROR_COUNT_COLOUMN = 13;
        private final int SNMP_TCP_OUT_SEG_COUNT_COLOUMN = 11;
        private final int SNMP_TCP_RETRANS_SEG_COUNT_COLOUMN = 14;
        private final int SOCKSTAT6_SOCK_COUNT_ROW = 1;
        private final int SOCKSTAT6_TCP_INUSE_COUNT_COLOUMN = 2;
        private final int SOCKSTAT_ORPHAN_COUNT_COLOUMN = 4;
        private final int SOCKSTAT_TCP_COUNT_ROW = 2;
        private final int SOCKSTAT_TCP_INUSE_COUNT_COLOUMN = 2;
        private final int SOCKSTAT_TIMEWAIT_COUNT_COLOUMN = 6;
        private final int TCP_POOR_SEG_RX = 0;
        private final int THRESHOLD_BACKHAUL_CONNECTIVITY_CHECK_HIGH = 5;
        private final int THRESHOLD_BACKHAUL_CONNECTIVITY_CHECK_LOW = 2;
        private final int THRESHOLD_BACKHAUL_CONNECTIVITY_CHECK_POOR = 2;
        private final int THRESHOLD_MAX_WAITING_CYCLE = 60;
        private final int THRESHOLD_TCP_POOR_SEG_RX_TX = 15;
        private final int THRESHOLD_WAITING_CYCLE_CHECK_HIGH = 5;
        private final int THRESHOLD_WAITING_CYCLE_CHECK_LOW = 3;
        private final int THRESHOLD_WAITING_CYCLE_CHECK_POOR = 2;
        private final int THRESHOLD_ZERO = 0;
        private final int TIME_QC_TRIGGER_INTERVAL = 10000;
        long lastPollTime = 0;
        int mBackhaulDetectionReason = 0;
        private long mBackhaulLastDnsCheckTime = 0;
        private int mBackhaulQcGoodRssi = 0;
        private long mBackhaulQcResultTime = 0;
        private int mBackhaulQcTriggeredRssi = 0;
        private ArrayList<Integer> mCumulativePoorRx = new ArrayList<>();
        private int mCurrentRxStats = 0;
        private boolean mDnsInterrupted = false;
        private boolean mDnsQueried = false;
        private int mGoodRxRate = 0;
        private int mGoodRxRssi = -200;
        private long mGoodRxTime = 0;
        int mInErrorSegWaitingCycle = 0;
        int mInternetConnectivityCounter = 0;
        int mInternetConnectivityWaitingCylce = 0;
        private long mLastDnsCheckTime = 0;
        private long mLastNeedCheckByPoorRxTime = 0;
        private int mLastRssi = 0;
        private int mMaybeUseStreaming = 0;
        private int mNsaQcStep = 0;
        private int mNsaQcTrigger = 0;
        private boolean mPollingStarted = false;
        int mPrevInSegCount = 0;
        int mPrevInSegErrorCount = 0;
        int mPrevOutSegCount = 0;
        private boolean mPrevQcPassed = false;
        int mPrevRetranSegCount = 0;
        int mPrevTcpEstablishedCount = 0;
        int mPrevTcpInUseCount = 0;
        int mPrevTimeWaitCount = 0;
        long mPreviousRx = 0;
        long mPreviousTx = 0;
        private boolean mPublicDnsCheckProcess = false;
        int mRetransSegWaitingCycle = 0;
        private int[] mRssiAverageWindow = new int[3];
        private int mRssiIndex = 0;
        private long mRxBytes = 0;
        private ArrayList<Integer> mRxHistory = new ArrayList<>();
        private long mRxPackets = 0;
        private boolean mSYNPacketOnly = false;
        private boolean mSkipRemainingDnsResults = false;
        private int mStayingLowMCS = 0;
        private long mTxBytes = 0;
        private long mTxPackets = 0;

        private boolean isBackhaulDetectionEnabled() {
            if (WifiConnectivityMonitor.this.mCurrentMode == 2 || WifiConnectivityMonitor.this.mCurrentMode == 3) {
                return true;
            }
            return false;
        }

        public NetworkStatsAnalyzer(Looper looper) {
            super(looper);
            sendEmptyMessageDelayed(WifiConnectivityMonitor.CMD_DELAY_NETSTATS_SESSION_INIT, 20000);
        }

        /* access modifiers changed from: package-private */
        public void checkPublicDns() {
            if (WifiConnectivityMonitor.this.inChinaNetwork()) {
                this.mPublicDnsCheckProcess = false;
                return;
            }
            this.mPublicDnsCheckProcess = true;
            this.mNsaQcStep = 1;
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            String str = wifiConnectivityMonitor.mParam.DEFAULT_URL_STRING;
            Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
            DnsThread dnsThread = new DnsThread(true, str, this, 10000);
            dnsThread.start();
            long unused = WifiConnectivityMonitor.this.mDnsThreadID = dnsThread.getId();
            if (WifiConnectivityMonitor.DBG) {
                Log.d(TAG, "wait publicDnsThread results [" + WifiConnectivityMonitor.this.mDnsThreadID + "]");
            }
        }

        /* access modifiers changed from: package-private */
        public void setGoodRxStateNow(long now) {
            if (now == 0) {
                this.mGoodRxTime = 0;
                this.mGoodRxRssi = -200;
                this.mGoodRxRate = 0;
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.w(TAG, "lose Good Rx status.");
                    return;
                }
                return;
            }
            WifiInfo info = WifiConnectivityMonitor.this.syncGetCurrentWifiInfo();
            int i = this.mGoodRxRate;
            if (i == 0 || now - this.mGoodRxTime >= 300000 || i > info.getLinkSpeed() || (this.mGoodRxRate == info.getLinkSpeed() && this.mGoodRxRssi > info.getRssi())) {
                this.mGoodRxRssi = info.getRssi();
                this.mGoodRxRate = info.getLinkSpeed();
            }
            this.mGoodRxTime = now;
            if (WifiConnectivityMonitor.SMARTCM_DBG) {
                Log.i(TAG, String.format("obtain Good Rx status [rssi : %ddbm, rate : %dMbps]", new Object[]{Integer.valueOf(this.mGoodRxRssi), Integer.valueOf(this.mGoodRxRate)}));
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:161:0x03ef, code lost:
            if (r8.mRetransSegWaitingCycle > 0) goto L_0x03f1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:439:0x0ba6, code lost:
            if (r11 < ((long) (com.android.server.wifi.WifiConnectivityMonitor.access$8300(r13).mPassBytesAggressiveMode * 2))) goto L_0x0bad;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:650:0x0fca, code lost:
            if (r45 < ((long) 7420)) goto L_0x0fcc;
         */
        /* JADX WARNING: Removed duplicated region for block: B:184:0x0428 A[SYNTHETIC, Splitter:B:184:0x0428] */
        /* JADX WARNING: Removed duplicated region for block: B:189:0x0433  */
        /* JADX WARNING: Removed duplicated region for block: B:192:0x0437  */
        /* JADX WARNING: Removed duplicated region for block: B:196:0x043e A[SYNTHETIC, Splitter:B:196:0x043e] */
        /* JADX WARNING: Removed duplicated region for block: B:203:0x0451  */
        /* JADX WARNING: Removed duplicated region for block: B:209:0x0460  */
        /* JADX WARNING: Removed duplicated region for block: B:216:0x0471  */
        /* JADX WARNING: Removed duplicated region for block: B:222:0x0480  */
        /* JADX WARNING: Removed duplicated region for block: B:223:0x0486  */
        /* JADX WARNING: Removed duplicated region for block: B:289:0x06f7 A[SYNTHETIC, Splitter:B:289:0x06f7] */
        /* JADX WARNING: Removed duplicated region for block: B:294:0x06ff A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:296:0x0704 A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:298:0x0709 A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:300:0x070e A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:302:0x0713 A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:304:0x0718 A[Catch:{ IOException -> 0x06fb }] */
        /* JADX WARNING: Removed duplicated region for block: B:311:0x0823  */
        /* JADX WARNING: Removed duplicated region for block: B:312:0x0826  */
        /* JADX WARNING: Removed duplicated region for block: B:315:0x0860  */
        /* JADX WARNING: Removed duplicated region for block: B:316:0x0863  */
        /* JADX WARNING: Removed duplicated region for block: B:321:0x088f A[SYNTHETIC, Splitter:B:321:0x088f] */
        /* JADX WARNING: Removed duplicated region for block: B:326:0x0897 A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:328:0x089c A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:330:0x08a1 A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:332:0x08a6 A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:334:0x08ab A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:336:0x08b0 A[Catch:{ IOException -> 0x0893 }] */
        /* JADX WARNING: Removed duplicated region for block: B:371:0x09ad  */
        /* JADX WARNING: Removed duplicated region for block: B:374:0x09c7  */
        /* JADX WARNING: Removed duplicated region for block: B:377:0x09e7  */
        /* JADX WARNING: Removed duplicated region for block: B:378:0x09ef  */
        /* JADX WARNING: Removed duplicated region for block: B:387:0x0a10  */
        /* JADX WARNING: Removed duplicated region for block: B:388:0x0a13  */
        /* JADX WARNING: Removed duplicated region for block: B:402:0x0a64  */
        /* JADX WARNING: Removed duplicated region for block: B:405:0x0a7e  */
        /* JADX WARNING: Removed duplicated region for block: B:408:0x0ab8  */
        /* JADX WARNING: Removed duplicated region for block: B:411:0x0ad2  */
        /* JADX WARNING: Removed duplicated region for block: B:414:0x0aec  */
        /* JADX WARNING: Removed duplicated region for block: B:415:0x0af5  */
        /* JADX WARNING: Removed duplicated region for block: B:418:0x0afe  */
        /* JADX WARNING: Removed duplicated region for block: B:419:0x0b01  */
        /* JADX WARNING: Removed duplicated region for block: B:422:0x0b0a  */
        /* JADX WARNING: Removed duplicated region for block: B:425:0x0b24  */
        /* JADX WARNING: Removed duplicated region for block: B:428:0x0b40  */
        /* JADX WARNING: Removed duplicated region for block: B:431:0x0b68  */
        /* JADX WARNING: Removed duplicated region for block: B:434:0x0b84  */
        /* JADX WARNING: Removed duplicated region for block: B:435:0x0b86  */
        /* JADX WARNING: Removed duplicated region for block: B:438:0x0b95  */
        /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba9  */
        /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbe  */
        /* JADX WARNING: Removed duplicated region for block: B:447:0x0bc8  */
        /* JADX WARNING: Removed duplicated region for block: B:455:0x0bfc  */
        /* JADX WARNING: Removed duplicated region for block: B:462:0x0c2a  */
        /* JADX WARNING: Removed duplicated region for block: B:557:0x0e18  */
        /* JADX WARNING: Removed duplicated region for block: B:667:0x1007  */
        /* JADX WARNING: Removed duplicated region for block: B:669:0x100e  */
        /* JADX WARNING: Removed duplicated region for block: B:676:0x1032  */
        /* JADX WARNING: Removed duplicated region for block: B:679:0x103f  */
        /* JADX WARNING: Removed duplicated region for block: B:692:0x10f7  */
        /* JADX WARNING: Removed duplicated region for block: B:694:0x1117  */
        /* JADX WARNING: Removed duplicated region for block: B:698:0x1146  */
        /* JADX WARNING: Removed duplicated region for block: B:728:0x11e3  */
        /* JADX WARNING: Removed duplicated region for block: B:731:0x11ed  */
        /* JADX WARNING: Removed duplicated region for block: B:732:0x121b  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r56) {
            /*
                r55 = this;
                r8 = r55
                r9 = r56
                java.lang.String r1 = "Exception: "
                long r10 = android.os.SystemClock.elapsedRealtime()
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.wifi.WifiInfo r12 = r0.syncGetCurrentWifiInfo()
                int r0 = r9.what
                r2 = 1
                java.lang.String r15 = "WifiConnectivityMonitor.NetworkStatsAnalyzer"
                switch(r0) {
                    case 135202: goto L_0x12af;
                    case 135203: goto L_0x12a9;
                    default: goto L_0x0018;
                }
            L_0x0018:
                r4 = 135220(0x21034, float:1.89484E-40)
                r13 = 135226(0x2103a, float:1.89492E-40)
                r5 = 0
                switch(r0) {
                    case 135219: goto L_0x126f;
                    case 135220: goto L_0x123a;
                    case 135221: goto L_0x090d;
                    case 135222: goto L_0x0906;
                    case 135223: goto L_0x08ed;
                    case 135224: goto L_0x08d9;
                    case 135225: goto L_0x08cc;
                    case 135226: goto L_0x009b;
                    default: goto L_0x0023;
                }
            L_0x0023:
                switch(r0) {
                    case 135229: goto L_0x004d;
                    case 135230: goto L_0x0047;
                    case 135231: goto L_0x004d;
                    default: goto L_0x0026;
                }
            L_0x0026:
                switch(r0) {
                    case 135235: goto L_0x0065;
                    case 135236: goto L_0x0065;
                    case 135237: goto L_0x0065;
                    default: goto L_0x0029;
                }
            L_0x0029:
                switch(r0) {
                    case 135241: goto L_0x0065;
                    case 135242: goto L_0x0065;
                    default: goto L_0x002c;
                }
            L_0x002c:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "Ignore msg id : "
                r0.append(r1)
                int r1 = r9.what
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.e(r15, r0)
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x0047:
                r0 = 135231(0x2103f, float:1.89499E-40)
                r8.removeMessages(r0)
            L_0x004d:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isConnectedState()
                if (r0 != 0) goto L_0x0065
                boolean r0 = r8.mPollingStarted
                if (r0 != 0) goto L_0x005d
                boolean r0 = r8.mDnsQueried
                if (r0 == 0) goto L_0x0065
            L_0x005d:
                r8.sendEmptyMessage(r4)
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x0065:
                int r0 = r9.what
                r8.removeMessages(r0)
                boolean r0 = r8.mDnsQueried
                if (r0 == 0) goto L_0x0096
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x008f
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "["
                r0.append(r1)
                int r1 = r9.what
                r0.append(r1)
                java.lang.String r1 = "] DNS query ongoing. -> Pass the next result"
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x008f:
                r8.mDnsInterrupted = r2
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x0096:
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x009b:
                long r2 = r8.mBackhaulQcResultTime
                int r4 = (r2 > r5 ? 1 : (r2 == r5 ? 0 : -1))
                if (r4 <= 0) goto L_0x00a8
                r4 = 10000(0x2710, double:4.9407E-320)
                long r2 = r2 + r4
                int r2 = (r2 > r10 ? 1 : (r2 == r10 ? 0 : -1))
                if (r2 > 0) goto L_0x00b4
            L_0x00a8:
                int r2 = r8.mBackhaulQcGoodRssi
                if (r2 >= 0) goto L_0x00fb
                int r2 = r2 + -3
                int r3 = r12.getRssi()
                if (r2 >= r3) goto L_0x00fb
            L_0x00b4:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x00ee
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "TCP_BACKHAUL_DETECTION_START skipped - mQcResultTime:"
                r0.append(r1)
                long r1 = r8.mBackhaulQcResultTime
                r0.append(r1)
                java.lang.String r1 = ", now:"
                r0.append(r1)
                r0.append(r10)
                java.lang.String r1 = ", mQcGoodRssi:"
                r0.append(r1)
                int r1 = r8.mBackhaulQcGoodRssi
                r0.append(r1)
                java.lang.String r1 = ", rssi:"
                r0.append(r1)
                int r1 = r12.getRssi()
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x00ee:
                r8.removeMessages(r13)
                r0 = 1000(0x3e8, double:4.94E-321)
                r8.sendEmptyMessageDelayed(r13, r0)
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x00fb:
                boolean r2 = r8.mPollingStarted
                if (r2 != 0) goto L_0x0104
                r2 = r9
                r34 = r12
                goto L_0x13ab
            L_0x0104:
                r2 = 0
                r3 = 0
                r4 = 0
                r5 = 0
                r6 = 0
                r16 = 0
                r18 = 0
                r19 = 0
                r20 = 0
                r22 = 0
                r23 = 0
                r24 = 0
                r25 = 0
                r26 = 0
                r27 = 0
                r28 = 0
                r29 = 0
                r30 = 0
                r31 = 0
                java.lang.String r32 = ""
                r33 = 0
                r34 = 0
                r35 = 0
                long r13 = android.net.TrafficStats.getTotalTxPackets()
                r38 = r2
                r39 = r3
                long r2 = android.net.TrafficStats.getTotalRxPackets()
                r40 = r1
                long r0 = r8.mPreviousRx
                r42 = r4
                r43 = r5
                long r4 = r2 - r0
                long r0 = r8.mPreviousTx
                long r7 = r13 - r0
                r0 = 0
                if (r12 == 0) goto L_0x0176
                int r0 = r12.getRssi()
                r1 = -70
                if (r0 < r1) goto L_0x015c
                r34 = 5
                r35 = 5
                r1 = r0
                r21 = r6
                r6 = r34
                goto L_0x017b
            L_0x015c:
                if (r0 >= r1) goto L_0x016c
                r1 = -83
                if (r0 <= r1) goto L_0x016c
                r34 = 2
                r35 = 3
                r1 = r0
                r21 = r6
                r6 = r34
                goto L_0x017b
            L_0x016c:
                r34 = 2
                r35 = 2
                r1 = r0
                r21 = r6
                r6 = r34
                goto L_0x017b
            L_0x0176:
                r1 = r0
                r21 = r6
                r6 = r34
            L_0x017b:
                r34 = r12
                r45 = r13
                r12 = r7
                r8 = r55
                int r0 = r8.mBackhaulQcGoodRssi
                if (r0 >= 0) goto L_0x018d
                if (r1 < r0) goto L_0x018d
                int r35 = r35 + 3
                r14 = r35
                goto L_0x018f
            L_0x018d:
                r14 = r35
            L_0x018f:
                java.io.FileReader r0 = new java.io.FileReader     // Catch:{ Exception -> 0x06d1, all -> 0x06b9 }
                java.lang.String r7 = "/proc/net/snmp"
                r0.<init>(r7)     // Catch:{ Exception -> 0x06d1, all -> 0x06b9 }
                r7 = r0
                java.io.FileReader r0 = new java.io.FileReader     // Catch:{ Exception -> 0x06a7, all -> 0x068d }
                java.lang.String r9 = "/proc/net/sockstat"
                r0.<init>(r9)     // Catch:{ Exception -> 0x06a7, all -> 0x068d }
                r9 = r0
                java.io.FileReader r0 = new java.io.FileReader     // Catch:{ Exception -> 0x067b, all -> 0x0663 }
                r47 = r2
                java.lang.String r2 = "/proc/net/sockstat6"
                r0.<init>(r2)     // Catch:{ Exception -> 0x0653, all -> 0x063d }
                r2 = r0
                java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ Exception -> 0x062e, all -> 0x0616 }
                r0.<init>(r7)     // Catch:{ Exception -> 0x062e, all -> 0x0616 }
                r3 = r0
                java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ Exception -> 0x0605, all -> 0x05ef }
                r0.<init>(r9)     // Catch:{ Exception -> 0x0605, all -> 0x05ef }
                r21 = r0
                java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ Exception -> 0x0605, all -> 0x05ef }
                r0.<init>(r2)     // Catch:{ Exception -> 0x0605, all -> 0x05ef }
                r16 = r0
                r0 = 6
                r35 = r0
                r38 = r7
            L_0x01c2:
                java.lang.String r35 = r3.readLine()     // Catch:{ Exception -> 0x05e0, all -> 0x05cc }
                r32 = r35
                if (r35 == 0) goto L_0x02d5
                int r7 = r31 + 1
                if (r7 != r0) goto L_0x02cd
                r31 = r7
                java.lang.String r7 = " +"
                r49 = r4
                r4 = r32
                java.lang.String[] r5 = r4.split(r7)     // Catch:{ Exception -> 0x029e, all -> 0x028a }
                r32 = r4
                r7 = 0
                r4 = r5[r7]     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                java.lang.String r7 = "Icmp"
                boolean r4 = r4.contains(r7)     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                if (r4 == 0) goto L_0x0218
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                if (r4 == 0) goto L_0x020f
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                r4.<init>()     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                java.lang.String r7 = "checkBackhaulConnection: "
                r4.append(r7)     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                r4.append(r0)     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                java.lang.String r7 = ", "
                r4.append(r7)     // Catch:{ Exception -> 0x027d, all -> 0x026b }
                r51 = r12
                r7 = 0
                r12 = r5[r7]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r4.append(r12)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                java.lang.String r4 = r4.toString()     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                android.util.Log.d(r15, r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                goto L_0x0211
            L_0x020f:
                r51 = r12
            L_0x0211:
                int r0 = r0 + 2
                r4 = r49
                r12 = r51
                goto L_0x01c2
            L_0x0218:
                r51 = r12
                r4 = 9
                r4 = r5[r4]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                int r4 = java.lang.Integer.parseInt(r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r18 = r4
                r4 = 14
                r4 = r5[r4]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                int r4 = java.lang.Integer.parseInt(r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r26 = r4
                r4 = 13
                r4 = r5[r4]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                int r4 = java.lang.Integer.parseInt(r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r25 = r4
                r4 = 10
                r4 = r5[r4]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                int r4 = java.lang.Integer.parseInt(r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r23 = r4
                r4 = 11
                r4 = r5[r4]     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                int r4 = java.lang.Integer.parseInt(r4)     // Catch:{ Exception -> 0x0260, all -> 0x0250 }
                r24 = r4
                r4 = r18
                goto L_0x02db
            L_0x0250:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x0260:
                r0 = move-exception
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
                r5 = r40
                goto L_0x06e0
            L_0x026b:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x027d:
                r0 = move-exception
                r51 = r12
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
                r5 = r40
                goto L_0x06e0
            L_0x028a:
                r0 = move-exception
                r32 = r4
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x029e:
                r0 = move-exception
                r32 = r4
                r51 = r12
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
                r5 = r40
                goto L_0x06e0
            L_0x02ad:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r4
                r5 = r40
                goto L_0x05de
            L_0x02be:
                r0 = move-exception
                r49 = r4
                r51 = r12
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
                r5 = r40
                goto L_0x05ed
            L_0x02cd:
                r49 = r4
                r31 = r7
                r51 = r12
                goto L_0x01c2
            L_0x02d5:
                r49 = r4
                r51 = r12
                r4 = r18
            L_0x02db:
                r5 = 0
                r31 = r5
            L_0x02de:
                java.lang.String r5 = r21.readLine()     // Catch:{ Exception -> 0x05be, all -> 0x05ab }
                r12 = r5
                if (r5 == 0) goto L_0x033c
                int r5 = r31 + 1
                java.lang.String r7 = " +"
                java.lang.String[] r7 = r12.split(r7)     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                r13 = r7
                r7 = 2
                if (r5 != r7) goto L_0x0310
                r18 = r13[r7]     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                int r7 = java.lang.Integer.parseInt(r18)     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                r19 = r7
                r7 = 4
                r7 = r13[r7]     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                int r7 = java.lang.Integer.parseInt(r7)     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                r20 = r7
                r7 = 6
                r7 = r13[r7]     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                int r7 = java.lang.Integer.parseInt(r7)     // Catch:{ Exception -> 0x032b, all -> 0x0315 }
                r22 = r7
                r31 = r5
                r5 = r22
                goto L_0x033e
            L_0x0310:
                r31 = r5
                r32 = r12
                goto L_0x02de
            L_0x0315:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r18 = r4
                r31 = r5
                r32 = r12
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x032b:
                r0 = move-exception
                r43 = r3
                r18 = r4
                r31 = r5
                r3 = r9
                r32 = r12
                r5 = r40
                r4 = r2
                r2 = r38
                goto L_0x06e0
            L_0x033c:
                r5 = r22
            L_0x033e:
                r7 = 0
                r31 = r7
                r32 = r12
            L_0x0343:
                java.lang.String r7 = r16.readLine()     // Catch:{ Exception -> 0x0599, all -> 0x0582 }
                r12 = r7
                if (r7 == 0) goto L_0x0396
                int r7 = r31 + 1
                java.lang.String r13 = " +"
                java.lang.String[] r13 = r12.split(r13)     // Catch:{ Exception -> 0x0383, all -> 0x036b }
                r35 = r0
                r0 = 1
                if (r7 != r0) goto L_0x0364
                r18 = 2
                r22 = r13[r18]     // Catch:{ Exception -> 0x0383, all -> 0x036b }
                int r18 = java.lang.Integer.parseInt(r22)     // Catch:{ Exception -> 0x0383, all -> 0x036b }
                int r19 = r19 + r18
                r31 = r7
                goto L_0x0398
            L_0x0364:
                r31 = r7
                r32 = r12
                r0 = r35
                goto L_0x0343
            L_0x036b:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r18 = r4
                r22 = r5
                r31 = r7
                r32 = r12
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x0383:
                r0 = move-exception
                r43 = r3
                r18 = r4
                r22 = r5
                r31 = r7
                r3 = r9
                r32 = r12
                r5 = r40
                r4 = r2
                r2 = r38
                goto L_0x06e0
            L_0x0396:
                r35 = r0
            L_0x0398:
                int r7 = r19 - r20
                int r13 = r8.mPrevInSegCount     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                int r29 = r23 - r13
                int r13 = r8.mPrevOutSegCount     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                int r30 = r24 - r13
                int r13 = r8.mPrevInSegErrorCount     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                int r28 = r25 - r13
                int r13 = r8.mPrevRetranSegCount     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                int r27 = r26 - r13
                if (r28 <= 0) goto L_0x03b2
                int r13 = r29 + r30
                r0 = 15
                if (r13 < r0) goto L_0x03b6
            L_0x03b2:
                int r0 = r8.mInErrorSegWaitingCycle     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r0 <= 0) goto L_0x03e9
            L_0x03b6:
                int r0 = r8.mInErrorSegWaitingCycle     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r13 = 1
                int r0 = r0 + r13
                r13 = r0
                r8.mInErrorSegWaitingCycle = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                goto L_0x03e9
            L_0x03be:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r18 = r4
                r22 = r5
                r19 = r7
                r32 = r12
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x03d6:
                r0 = move-exception
                r43 = r3
                r18 = r4
                r22 = r5
                r19 = r7
                r3 = r9
                r32 = r12
                r5 = r40
                r4 = r2
                r2 = r38
                goto L_0x06e0
            L_0x03e9:
                if (r27 <= 0) goto L_0x03ed
                if (r29 == 0) goto L_0x03f1
            L_0x03ed:
                int r13 = r8.mRetransSegWaitingCycle     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r13 <= 0) goto L_0x03f7
            L_0x03f1:
                int r13 = r8.mRetransSegWaitingCycle     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r0 = 1
                int r13 = r13 + r0
                r8.mRetransSegWaitingCycle = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x03f7:
                int r13 = r8.mPrevTcpEstablishedCount     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r4 > r13) goto L_0x041b
                boolean r13 = r8.mPrevQcPassed     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                if (r13 == 0) goto L_0x0400
                goto L_0x041b
            L_0x0400:
                int r13 = r8.mPrevTimeWaitCount     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                if (r5 <= r13) goto L_0x040d
                r13 = 2
                if (r14 <= r13) goto L_0x040d
                r13 = 0
                r8.mInternetConnectivityCounter = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r8.mInternetConnectivityWaitingCylce = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                goto L_0x0424
            L_0x040d:
                int r13 = r8.mPrevTcpInUseCount     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                if (r7 <= r13) goto L_0x0424
                int r13 = r8.mInternetConnectivityCounter     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r8.mPrevTcpInUseCount     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r7 - r0
                int r13 = r13 + r0
                r8.mInternetConnectivityCounter = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                goto L_0x0424
            L_0x041b:
                r0 = 0
                r8.mInternetConnectivityCounter = r0     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                r8.mInternetConnectivityWaitingCylce = r0     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                r8.mInErrorSegWaitingCycle = r0     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                r8.mRetransSegWaitingCycle = r0     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
            L_0x0424:
                int r0 = r8.mInternetConnectivityCounter     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r0 <= 0) goto L_0x042f
                int r0 = r8.mInternetConnectivityWaitingCylce     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r13 = 1
                int r0 = r0 + r13
                r13 = r0
                r8.mInternetConnectivityWaitingCylce = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x042f:
                boolean r13 = r8.mPrevQcPassed     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r13 == 0) goto L_0x0437
                r13 = 0
                r8.mPrevQcPassed = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                goto L_0x0438
            L_0x0437:
                r13 = 0
            L_0x0438:
                r8.mBackhaulDetectionReason = r13     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                int r13 = r8.mInternetConnectivityCounter     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r13 <= r6) goto L_0x044d
                int r13 = r8.mInternetConnectivityWaitingCylce     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                if (r13 <= r14) goto L_0x044d
                java.lang.String r13 = "Backhaul Disconnection due to TCP Sockets"
                android.util.Log.d(r15, r13)     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r13 = r8.mBackhaulDetectionReason     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r0 = 1
                int r13 = r13 + r0
                r8.mBackhaulDetectionReason = r13     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x044d:
                int r0 = r8.mRetransSegWaitingCycle     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r0 <= r14) goto L_0x045c
                java.lang.String r0 = "Backhaul Disconnection due to RetransSeg"
                android.util.Log.d(r15, r0)     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r8.mBackhaulDetectionReason     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                r13 = 2
                int r0 = r0 + r13
                r8.mBackhaulDetectionReason = r0     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x045c:
                int r0 = r8.mInErrorSegWaitingCycle     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r0 <= r14) goto L_0x046b
                java.lang.String r0 = "Backhaul Disconnection due to InErrorSeg"
                android.util.Log.d(r15, r0)     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r8.mBackhaulDetectionReason     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r0 + 4
                r8.mBackhaulDetectionReason = r0     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x046b:
                int r0 = r8.mInternetConnectivityWaitingCylce     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                r13 = 60
                if (r0 <= r13) goto L_0x047c
                java.lang.String r0 = "Backhaul Disconnection due to no response from network"
                android.util.Log.d(r15, r0)     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r8.mBackhaulDetectionReason     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
                int r0 = r0 + 8
                r8.mBackhaulDetectionReason = r0     // Catch:{ Exception -> 0x03d6, all -> 0x03be }
            L_0x047c:
                int r0 = r8.mBackhaulDetectionReason     // Catch:{ Exception -> 0x056c, all -> 0x0551 }
                if (r0 <= 0) goto L_0x0486
                r33 = 1
                r13 = r4
                r17 = r5
                goto L_0x04d1
            L_0x0486:
                r13 = r4
                r17 = r5
                long r4 = r8.mBackhaulLastDnsCheckTime     // Catch:{ Exception -> 0x053e, all -> 0x0526 }
                long r4 = r10 - r4
                r18 = 60000(0xea60, double:2.9644E-319)
                int r0 = (r4 > r18 ? 1 : (r4 == r18 ? 0 : -1))
                if (r0 <= 0) goto L_0x04d1
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ Exception -> 0x04be, all -> 0x04a6 }
                if (r0 == 0) goto L_0x049f
                java.lang.String r0 = "Do 1 min DNS check"
                android.util.Log.d(r15, r0)     // Catch:{ Exception -> 0x04be, all -> 0x04a6 }
            L_0x049f:
                r33 = 1
                r0 = 16
                r8.mBackhaulDetectionReason = r0     // Catch:{ Exception -> 0x04be, all -> 0x04a6 }
                goto L_0x04d1
            L_0x04a6:
                r0 = move-exception
                r35 = r1
                r42 = r2
                r19 = r7
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                r5 = r40
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x04be:
                r0 = move-exception
                r4 = r2
                r43 = r3
                r19 = r7
                r3 = r9
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                r5 = r40
                goto L_0x06e0
            L_0x04d1:
                r38.close()     // Catch:{ IOException -> 0x04e7 }
                r9.close()     // Catch:{ IOException -> 0x04e7 }
                r2.close()     // Catch:{ IOException -> 0x04e7 }
                r9.close()     // Catch:{ IOException -> 0x04e7 }
                r3.close()     // Catch:{ IOException -> 0x04e7 }
                r21.close()     // Catch:{ IOException -> 0x04e7 }
                r16.close()     // Catch:{ IOException -> 0x04e7 }
                goto L_0x0500
            L_0x04e7:
                r0 = move-exception
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r5 = r40
                r4.append(r5)
                r4.append(r0)
                java.lang.String r4 = r4.toString()
                android.util.Log.w(r15, r4)
                r0.printStackTrace()
            L_0x0500:
                r19 = r2
                r43 = r3
                r18 = r9
                r32 = r12
                r0 = r17
                r22 = r20
                r12 = r26
                r5 = r27
                r9 = r28
                r3 = r29
                r2 = r30
                r4 = r33
                r17 = r38
                r38 = r10
                r20 = r16
                r10 = r23
                r16 = r24
                r11 = r25
                goto L_0x0757
            L_0x0526:
                r0 = move-exception
                r5 = r40
                r35 = r1
                r42 = r2
                r19 = r7
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x053e:
                r0 = move-exception
                r5 = r40
                r4 = r2
                r43 = r3
                r19 = r7
                r3 = r9
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                goto L_0x06e0
            L_0x0551:
                r0 = move-exception
                r13 = r4
                r17 = r5
                r5 = r40
                r35 = r1
                r42 = r2
                r19 = r7
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x056c:
                r0 = move-exception
                r13 = r4
                r17 = r5
                r5 = r40
                r4 = r2
                r43 = r3
                r19 = r7
                r3 = r9
                r32 = r12
                r18 = r13
                r22 = r17
                r2 = r38
                goto L_0x06e0
            L_0x0582:
                r0 = move-exception
                r13 = r4
                r17 = r5
                r5 = r40
                r35 = r1
                r42 = r2
                r18 = r13
                r22 = r17
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x0599:
                r0 = move-exception
                r13 = r4
                r17 = r5
                r5 = r40
                r4 = r2
                r43 = r3
                r3 = r9
                r18 = r13
                r22 = r17
                r2 = r38
                goto L_0x06e0
            L_0x05ab:
                r0 = move-exception
                r13 = r4
                r5 = r40
                r35 = r1
                r42 = r2
                r18 = r13
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x05be:
                r0 = move-exception
                r13 = r4
                r5 = r40
                r4 = r2
                r43 = r3
                r3 = r9
                r18 = r13
                r2 = r38
                goto L_0x06e0
            L_0x05cc:
                r0 = move-exception
                r49 = r4
                r5 = r40
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
            L_0x05de:
                goto L_0x088d
            L_0x05e0:
                r0 = move-exception
                r49 = r4
                r51 = r12
                r5 = r40
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
            L_0x05ed:
                goto L_0x06e0
            L_0x05ef:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r5 = r40
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x0605:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r51 = r12
                r5 = r40
                r4 = r2
                r43 = r3
                r3 = r9
                r2 = r38
                goto L_0x06e0
            L_0x0616:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r5 = r40
                r35 = r1
                r42 = r2
                r51 = r12
                r2 = r38
                r3 = r43
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x062e:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r51 = r12
                r5 = r40
                r4 = r2
                r3 = r9
                r2 = r38
                goto L_0x06e0
            L_0x063d:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r5 = r40
                r35 = r1
                r51 = r12
                r2 = r38
                r3 = r43
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x0653:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r51 = r12
                r5 = r40
                r3 = r9
                r2 = r38
                r4 = r42
                goto L_0x06e0
            L_0x0663:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r5 = r40
                r35 = r1
                r47 = r2
                r51 = r12
                r2 = r38
                r3 = r43
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x067b:
                r0 = move-exception
                r47 = r2
                r49 = r4
                r38 = r7
                r51 = r12
                r5 = r40
                r3 = r9
                r2 = r38
                r4 = r42
                goto L_0x06e0
            L_0x068d:
                r0 = move-exception
                r49 = r4
                r38 = r7
                r5 = r40
                r35 = r1
                r47 = r2
                r51 = r12
                r2 = r38
                r9 = r39
                r3 = r43
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x06a7:
                r0 = move-exception
                r47 = r2
                r49 = r4
                r38 = r7
                r51 = r12
                r5 = r40
                r2 = r38
                r3 = r39
                r4 = r42
                goto L_0x06e0
            L_0x06b9:
                r0 = move-exception
                r49 = r4
                r5 = r40
                r35 = r1
                r47 = r2
                r51 = r12
                r2 = r38
                r9 = r39
                r3 = r43
                r1 = r0
                r38 = r10
                r11 = r49
                goto L_0x088d
            L_0x06d1:
                r0 = move-exception
                r47 = r2
                r49 = r4
                r51 = r12
                r5 = r40
                r2 = r38
                r3 = r39
                r4 = r42
            L_0x06e0:
                java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x087f }
                r7.<init>()     // Catch:{ all -> 0x087f }
                r7.append(r5)     // Catch:{ all -> 0x087f }
                r7.append(r0)     // Catch:{ all -> 0x087f }
                java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x087f }
                android.util.Log.w(r15, r7)     // Catch:{ all -> 0x087f }
                r0.printStackTrace()     // Catch:{ all -> 0x087f }
                if (r2 == 0) goto L_0x06fd
                r2.close()     // Catch:{ IOException -> 0x06fb }
                goto L_0x06fd
            L_0x06fb:
                r0 = move-exception
                goto L_0x071c
            L_0x06fd:
                if (r3 == 0) goto L_0x0702
                r3.close()     // Catch:{ IOException -> 0x06fb }
            L_0x0702:
                if (r4 == 0) goto L_0x0707
                r4.close()     // Catch:{ IOException -> 0x06fb }
            L_0x0707:
                if (r3 == 0) goto L_0x070c
                r3.close()     // Catch:{ IOException -> 0x06fb }
            L_0x070c:
                if (r43 == 0) goto L_0x0711
                r43.close()     // Catch:{ IOException -> 0x06fb }
            L_0x0711:
                if (r21 == 0) goto L_0x0716
                r21.close()     // Catch:{ IOException -> 0x06fb }
            L_0x0716:
                if (r16 == 0) goto L_0x0732
                r16.close()     // Catch:{ IOException -> 0x06fb }
                goto L_0x0732
            L_0x071c:
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                r7.append(r5)
                r7.append(r0)
                java.lang.String r5 = r7.toString()
                android.util.Log.w(r15, r5)
                r0.printStackTrace()
                goto L_0x0733
            L_0x0732:
            L_0x0733:
                r17 = r2
                r38 = r10
                r13 = r18
                r7 = r19
                r0 = r22
                r10 = r23
                r11 = r25
                r12 = r26
                r5 = r27
                r9 = r28
                r2 = r30
                r18 = r3
                r19 = r4
                r22 = r20
                r3 = r29
                r4 = r33
                r20 = r16
                r16 = r24
            L_0x0757:
                r23 = r10
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r24 = r11
                java.lang.String r11 = "RSSI:"
                r10.append(r11)
                r10.append(r1)
                java.lang.String r11 = ", CE:"
                r10.append(r11)
                r10.append(r13)
                java.lang.String r11 = ", PE:"
                r10.append(r11)
                int r11 = r8.mPrevTcpEstablishedCount
                r10.append(r11)
                java.lang.String r11 = ", TI:"
                r10.append(r11)
                r10.append(r7)
                java.lang.String r11 = ", PTI:"
                r10.append(r11)
                int r11 = r8.mPrevTcpInUseCount
                r10.append(r11)
                java.lang.String r11 = ", TW:"
                r10.append(r11)
                r10.append(r0)
                java.lang.String r11 = ", PTW:"
                r10.append(r11)
                int r11 = r8.mPrevTimeWaitCount
                r10.append(r11)
                java.lang.String r11 = ", Tx:"
                r10.append(r11)
                r25 = r12
                r11 = r51
                r10.append(r11)
                r35 = r1
                java.lang.String r1 = ", Rx:"
                r10.append(r1)
                r11 = r49
                r10.append(r11)
                java.lang.String r1 = ", TxS:"
                r10.append(r1)
                r10.append(r2)
                java.lang.String r1 = ", RxS:"
                r10.append(r1)
                r10.append(r3)
                java.lang.String r1 = ", RESULT:"
                r10.append(r1)
                r10.append(r4)
                java.lang.String r1 = ", IC:"
                r10.append(r1)
                int r1 = r8.mInternetConnectivityCounter
                r10.append(r1)
                java.lang.String r1 = ", ICT:"
                r10.append(r1)
                r10.append(r6)
                java.lang.String r1 = ", WC:"
                r10.append(r1)
                int r1 = r8.mInternetConnectivityWaitingCylce
                r10.append(r1)
                java.lang.String r1 = ", WCT:"
                r10.append(r1)
                r10.append(r14)
                java.lang.String r1 = ", R:"
                r10.append(r1)
                r10.append(r5)
                java.lang.String r1 = ", RC:"
                r10.append(r1)
                int r1 = r8.mRetransSegWaitingCycle
                r10.append(r1)
                java.lang.String r1 = ", IE:"
                r10.append(r1)
                r10.append(r9)
                java.lang.String r1 = ", EC:"
                r10.append(r1)
                int r1 = r8.mInErrorSegWaitingCycle
                r10.append(r1)
                java.lang.String r1 = r10.toString()
                if (r4 != 0) goto L_0x0826
                boolean r10 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r10 == 0) goto L_0x0823
                goto L_0x0826
            L_0x0823:
                r26 = r2
                goto L_0x083c
            L_0x0826:
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r26 = r2
                java.lang.String r2 = "Backhaul result - "
                r10.append(r2)
                r10.append(r1)
                java.lang.String r2 = r10.toString()
                android.util.Log.d(r15, r2)
            L_0x083c:
                r8.mPrevTcpEstablishedCount = r13
                r8.mPrevTimeWaitCount = r0
                r8.mPrevTcpInUseCount = r7
                r2 = r25
                r8.mPrevRetranSegCount = r2
                r10 = r24
                r8.mPrevInSegErrorCount = r10
                r24 = r1
                r1 = r47
                r8.mPreviousRx = r1
                r1 = r45
                r8.mPreviousTx = r1
                r15 = r23
                r8.mPrevInSegCount = r15
                r23 = r0
                r0 = r16
                r8.mPrevOutSegCount = r0
                if (r4 == 0) goto L_0x0863
                r16 = 10000(0x2710, float:1.4013E-41)
                goto L_0x0865
            L_0x0863:
                r16 = 1000(0x3e8, float:1.401E-42)
            L_0x0865:
                r27 = r16
                r45 = r1
                r1 = 135226(0x2103a, float:1.89492E-40)
                r8.removeMessages(r1)
                r16 = r3
                r2 = r27
                r27 = r4
                long r3 = (long) r2
                r8.sendEmptyMessageDelayed(r1, r3)
                r2 = r56
                r10 = r38
                goto L_0x13ab
            L_0x087f:
                r0 = move-exception
                r35 = r1
                r1 = r2
                r38 = r10
                r11 = r49
                r9 = r3
                r42 = r4
                r3 = r43
                r1 = r0
            L_0x088d:
                if (r2 == 0) goto L_0x0895
                r2.close()     // Catch:{ IOException -> 0x0893 }
                goto L_0x0895
            L_0x0893:
                r0 = move-exception
                goto L_0x08b4
            L_0x0895:
                if (r9 == 0) goto L_0x089a
                r9.close()     // Catch:{ IOException -> 0x0893 }
            L_0x089a:
                if (r42 == 0) goto L_0x089f
                r42.close()     // Catch:{ IOException -> 0x0893 }
            L_0x089f:
                if (r9 == 0) goto L_0x08a4
                r9.close()     // Catch:{ IOException -> 0x0893 }
            L_0x08a4:
                if (r3 == 0) goto L_0x08a9
                r3.close()     // Catch:{ IOException -> 0x0893 }
            L_0x08a9:
                if (r21 == 0) goto L_0x08ae
                r21.close()     // Catch:{ IOException -> 0x0893 }
            L_0x08ae:
                if (r16 == 0) goto L_0x08ca
                r16.close()     // Catch:{ IOException -> 0x0893 }
                goto L_0x08ca
            L_0x08b4:
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r4.append(r5)
                r4.append(r0)
                java.lang.String r4 = r4.toString()
                android.util.Log.w(r15, r4)
                r0.printStackTrace()
                goto L_0x08cb
            L_0x08ca:
            L_0x08cb:
                throw r1
            L_0x08cc:
                r38 = r10
                r34 = r12
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean unused = r0.initDataUsage()
                r2 = r56
                goto L_0x13ab
            L_0x08d9:
                r38 = r10
                r34 = r12
                r9 = r56
                java.lang.Object r0 = r9.obj
                java.lang.Long r0 = (java.lang.Long) r0
                long r0 = r0.longValue()
                r8.setGoodRxStateNow(r0)
                r2 = r9
                goto L_0x13ab
            L_0x08ed:
                r38 = r10
                r34 = r12
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isMobileHotspot()
                if (r0 == 0) goto L_0x08fe
                r2 = r9
                r10 = r38
                goto L_0x13ab
            L_0x08fe:
                r55.checkPublicDns()
                r2 = r9
                r10 = r38
                goto L_0x13ab
            L_0x0906:
                r38 = r10
                r34 = r12
                r2 = r9
                goto L_0x13ab
            L_0x090d:
                r38 = r10
                r34 = r12
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x092d
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "mPollingStarted : "
                r1.append(r2)
                boolean r2 = r8.mPollingStarted
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                android.util.Log.i(r15, r1)
            L_0x092d:
                boolean r1 = r8.mPollingStarted
                if (r1 != 0) goto L_0x0936
                r2 = r9
                r10 = r38
                goto L_0x13ab
            L_0x0936:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r10 = r1.mCurrentBssid
                if (r10 == 0) goto L_0x1225
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$BssidStatistics r1 = r1.mEmptyBssid
                if (r10 != r1) goto L_0x094b
                r2 = r10
                r10 = r38
                goto L_0x1228
            L_0x094b:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.internal.util.AsyncChannel r1 = r1.mIWCChannel
                r2 = 135376(0x210d0, float:1.89702E-40)
                r1.sendMessage(r2)
                int r1 = r34.getRssi()
                r2 = -90
                if (r1 >= r2) goto L_0x099e
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.ClientModeImpl r2 = r2.mClientModeImpl
                boolean r2 = r2.isConnected()
                if (r2 != 0) goto L_0x098d
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r2 = "already disconnected : "
                r0.append(r2)
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.i(r15, r0)
                r0 = 135221(0x21035, float:1.89485E-40)
                r8.removeMessages(r0)
                r8.sendEmptyMessage(r4)
                r2 = r9
                r10 = r38
                goto L_0x13ab
            L_0x098d:
                r2 = -95
                if (r1 >= r2) goto L_0x099e
                r2 = -127(0xffffffffffffff81, float:NaN)
                if (r1 != r2) goto L_0x099a
                r2 = r9
                r10 = r38
                goto L_0x13ab
            L_0x099a:
                r1 = -95
                r11 = r1
                goto L_0x099f
            L_0x099e:
                r11 = r1
            L_0x099f:
                int r1 = r8.mLastRssi
                int r1 = r1 + r11
                r2 = 2
                int r12 = r1 / 2
                r8.mLastRssi = r11
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x09c1
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "rssi : "
                r1.append(r2)
                r1.append(r12)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r15, r1)
            L_0x09c1:
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x09df
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "linkspeed : "
                r1.append(r2)
                int r2 = r34.getLinkSpeed()
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                android.util.Log.d(r15, r1)
            L_0x09df:
                r1 = 0
                int r2 = r34.getLinkSpeed()
                r3 = 6
                if (r2 > r3) goto L_0x09ef
                int r2 = r8.mStayingLowMCS
                r0 = 1
                int r2 = r2 + r0
                r8.mStayingLowMCS = r2
                r1 = 1
                goto L_0x09f2
            L_0x09ef:
                r2 = 0
                r8.mStayingLowMCS = r2
            L_0x09f2:
                int r2 = r8.mLastRssi
                r3 = -75
                if (r2 >= r3) goto L_0x0a01
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                r3 = 2
                if (r2 == r3) goto L_0x0a10
            L_0x0a01:
                int r2 = r8.mLastRssi
                r3 = -83
                if (r2 >= r3) goto L_0x0a13
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r2 = r2.mCurrentMode
                r0 = 1
                if (r2 != r0) goto L_0x0a13
            L_0x0a10:
                r1 = 1
                r13 = r1
                goto L_0x0a14
            L_0x0a13:
                r13 = r1
            L_0x0a14:
                int r1 = r8.mGoodRxRate
                if (r1 <= 0) goto L_0x0a40
                long r1 = r8.mGoodRxTime
                long r1 = r38 - r1
                r3 = 300000(0x493e0, double:1.482197E-318)
                int r1 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
                if (r1 >= 0) goto L_0x0a3d
                int r1 = r34.getLinkSpeed()
                int r2 = r8.mGoodRxRate
                if (r1 < r2) goto L_0x0a3d
                int r1 = r34.getLinkSpeed()
                int r2 = r8.mGoodRxRate
                if (r1 != r2) goto L_0x0a40
                int r1 = r34.getRssi()
                int r2 = r8.mGoodRxRssi
                int r2 = r2 + -5
                if (r1 >= r2) goto L_0x0a40
            L_0x0a3d:
                r8.setGoodRxStateNow(r5)
            L_0x0a40:
                long r3 = r8.mTxPackets
                long r1 = r8.mRxPackets
                java.lang.String r14 = "wlan0"
                long r5 = android.net.TrafficStats.getTxPackets(r14)
                r8.mTxPackets = r5
                long r5 = android.net.TrafficStats.getRxPackets(r14)
                r8.mRxPackets = r5
                long r5 = r8.mRxPackets
                long r5 = r5 - r1
                r24 = r1
                long r0 = r8.mTxPackets
                long r1 = r0 - r3
                int r0 = (int) r5
                r8.mCurrentRxStats = r0
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0a78
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r7 = "diffRx : "
                r0.append(r7)
                r0.append(r5)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0a78:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0a92
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r7 = "diffTx : "
                r0.append(r7)
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0a92:
                r26 = r3
                long r3 = r8.mTxBytes
                r28 = r10
                long r9 = r8.mRxBytes
                r29 = r11
                r30 = r12
                long r11 = android.net.TrafficStats.getTxBytes(r14)
                r8.mTxBytes = r11
                long r11 = android.net.TrafficStats.getRxBytes(r14)
                r8.mRxBytes = r11
                long r11 = r8.mRxBytes
                long r11 = r11 - r9
                r31 = r9
                long r9 = r8.mTxBytes
                long r9 = r9 - r3
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0acc
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r7 = "diffRxBytes : "
                r0.append(r7)
                r0.append(r11)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0acc:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0ae6
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r7 = "diffTxBytes : "
                r0.append(r7)
                r0.append(r9)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0ae6:
                r22 = 0
                int r0 = (r5 > r22 ? 1 : (r5 == r22 ? 0 : -1))
                if (r0 <= 0) goto L_0x0af5
                long r35 = r11 / r5
                r53 = r3
                r3 = r35
                r35 = r53
                goto L_0x0af9
            L_0x0af5:
                r35 = r3
                r3 = r22
            L_0x0af9:
                int r14 = (int) r3
                int r0 = (r1 > r22 ? 1 : (r1 == r22 ? 0 : -1))
                if (r0 <= 0) goto L_0x0b01
                long r3 = r9 / r1
                goto L_0x0b03
            L_0x0b01:
                r3 = 0
            L_0x0b03:
                int r7 = (int) r3
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0b1e
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "rxBytesPerPacket : "
                r0.append(r3)
                r0.append(r14)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0b1e:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0b38
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "txBytesPerPacket : "
                r0.append(r3)
                r0.append(r7)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x0b38:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.mNetworkStatHistoryUpdate
                if (r0 == 0) goto L_0x0b59
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                r3.append(r5)
                java.lang.String r4 = ","
                r3.append(r4)
                r3.append(r1)
                java.lang.String r3 = r3.toString()
                r0.addNetworkStatHistory(r3)
            L_0x0b59:
                r3 = 0
                r4 = 0
                r33 = 0
                java.util.ArrayList<java.lang.Integer> r0 = r8.mRxHistory
                int r0 = r0.size()
                r40 = r3
                r3 = 6
                if (r0 != r3) goto L_0x0b73
                java.util.ArrayList<java.lang.Integer> r0 = r8.mRxHistory
                r3 = 0
                r0.remove(r3)
                java.util.ArrayList<java.lang.Integer> r0 = r8.mRxHistory
                r0.trimToSize()
            L_0x0b73:
                java.util.ArrayList<java.lang.Integer> r0 = r8.mRxHistory
                java.lang.Integer r3 = new java.lang.Integer
                r42 = r4
                int r4 = (int) r5
                r3.<init>(r4)
                r0.add(r3)
                r0 = 1430(0x596, float:2.004E-42)
                if (r14 <= r0) goto L_0x0b86
                r0 = 1
                goto L_0x0b87
            L_0x0b86:
                r0 = 0
            L_0x0b87:
                r4 = r0
                r3 = r28
                r0 = r30
                r3.updateMaxThroughput(r0, r11, r4)
                r28 = r13
                com.android.server.wifi.WifiConnectivityMonitor r13 = com.android.server.wifi.WifiConnectivityMonitor.this
                if (r4 != 0) goto L_0x0ba9
                r30 = r4
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r4 = r13.mParam
                int r4 = r4.mPassBytesAggressiveMode
                r37 = 2
                int r4 = r4 * 2
                r43 = r9
                long r9 = (long) r4
                int r4 = (r11 > r9 ? 1 : (r11 == r9 ? 0 : -1))
                if (r4 >= 0) goto L_0x0bbc
                goto L_0x0bad
            L_0x0ba9:
                r30 = r4
                r43 = r9
            L_0x0bad:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r4 = r4.mParam
                int r4 = r4.mPassBytesAggressiveMode
                int r4 = r4 * 20
                long r9 = (long) r4
                int r4 = (r11 > r9 ? 1 : (r11 == r9 ? 0 : -1))
                if (r4 < 0) goto L_0x0bbe
            L_0x0bbc:
                r4 = 1
                goto L_0x0bbf
            L_0x0bbe:
                r4 = 0
            L_0x0bbf:
                boolean unused = r13.mInAggGoodStateNow = r4
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r4 == 0) goto L_0x0be2
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r9 = "mInAggGoodStateNow : "
                r4.append(r9)
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r9 = r9.mInAggGoodStateNow
                r4.append(r9)
                java.lang.String r4 = r4.toString()
                android.util.Log.d(r15, r4)
            L_0x0be2:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                r9 = 3
                if (r4 != r9) goto L_0x0bf6
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mInAggGoodStateNow
                if (r4 == 0) goto L_0x0bf6
                r3.updateGoodRssi(r0)
            L_0x0bf6:
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r4 == 0) goto L_0x0c12
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r9 = "mMaybeUseStreaming : "
                r4.append(r9)
                int r9 = r8.mMaybeUseStreaming
                r4.append(r9)
                java.lang.String r4 = r4.toString()
                android.util.Log.d(r15, r4)
            L_0x0c12:
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mIsScanning
                if (r4 != 0) goto L_0x1117
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mIsInRoamSession
                if (r4 != 0) goto L_0x1117
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mIsInDhcpSession
                if (r4 != 0) goto L_0x1117
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mIsScreenOn
                if (r4 != 0) goto L_0x0c45
                r9 = r0
                r41 = r3
                r22 = r5
                r20 = r7
                r45 = r11
                r10 = r38
                r49 = r43
                r12 = 1000(0x3e8, double:4.94E-321)
                r38 = r1
                goto L_0x1128
            L_0x0c45:
                boolean r4 = r8.mPublicDnsCheckProcess
                if (r4 != 0) goto L_0x1105
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                if (r4 == 0) goto L_0x1105
                boolean r4 = r8.mDnsQueried
                if (r4 == 0) goto L_0x0cec
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                r4.append(r5)
                java.lang.String r9 = " "
                r4.append(r9)
                r4.append(r1)
                r4.append(r9)
                r4.append(r11)
                r4.append(r9)
                r45 = r11
                r10 = r43
                r4.append(r10)
                r4.append(r9)
                r4.append(r14)
                r4.append(r9)
                r4.append(r7)
                java.lang.String r4 = r4.toString()
                android.util.Log.d(r15, r4)
                boolean r4 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r4 == 0) goto L_0x0c93
                java.lang.String r4 = "waiting dns responses or the quality result now!"
                android.util.Log.i(r15, r4)
            L_0x0c93:
                r4 = 0
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r9 = r9.mCurrentMode
                r12 = 3
                if (r9 != r12) goto L_0x0ca7
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r9 = r9.mInAggGoodStateNow
                if (r9 == 0) goto L_0x0cc2
                r4 = 1
                goto L_0x0cc2
            L_0x0ca7:
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r9 = r9.mParam
                int r9 = r9.mGoodRxPacketsBase
                long r12 = (long) r9
                int r9 = (r5 > r12 ? 1 : (r5 == r12 ? 0 : -1))
                if (r9 < 0) goto L_0x0cba
                r9 = 500(0x1f4, float:7.0E-43)
                if (r14 <= r9) goto L_0x0cba
                r4 = 1
                goto L_0x0cc2
            L_0x0cba:
                r12 = 100000(0x186a0, double:4.94066E-319)
                int r9 = (r10 > r12 ? 1 : (r10 == r12 ? 0 : -1))
                if (r9 < 0) goto L_0x0cc2
                r4 = 1
            L_0x0cc2:
                if (r4 == 0) goto L_0x0cdb
                boolean r9 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r9 == 0) goto L_0x0ccf
                java.lang.String r9 = "Good Rx!, don't need to keep evaluating quality!"
                android.util.Log.i(r15, r9)
            L_0x0ccf:
                boolean r9 = r8.mDnsQueried
                if (r9 == 0) goto L_0x0cdb
                r9 = 1
                r8.mSkipRemainingDnsResults = r9
                r9 = 0
                r8.mDnsQueried = r9
                r8.mDnsInterrupted = r9
            L_0x0cdb:
                r9 = r0
                r41 = r3
                r22 = r5
                r20 = r7
                r49 = r10
                r10 = r38
                r12 = 1000(0x3e8, double:4.94E-321)
                r38 = r1
                goto L_0x1134
            L_0x0cec:
                r9 = r0
                r45 = r11
                r10 = r43
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                r12 = 3
                if (r4 != r12) goto L_0x0d32
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r4 = r4.mInAggGoodStateNow
                if (r4 == 0) goto L_0x0d32
                java.util.ArrayList<java.lang.Integer> r4 = r8.mCumulativePoorRx
                r4.clear()
                r4 = 0
                r8.mSYNPacketOnly = r4
                if (r30 == 0) goto L_0x0d12
                int r4 = r8.mMaybeUseStreaming
                r0 = 1
                int r4 = r4 + r0
                r8.mMaybeUseStreaming = r4
            L_0x0d12:
                long r12 = r8.mLastDnsCheckTime
                long r12 = r38 - r12
                r43 = 7000(0x1b58, double:3.4585E-320)
                int r0 = (r12 > r43 ? 1 : (r12 == r43 ? 0 : -1))
                if (r0 <= 0) goto L_0x0d22
                r12 = 7000(0x1b58, double:3.4585E-320)
                long r12 = r38 - r12
                r8.mLastDnsCheckTime = r12
            L_0x0d22:
                r41 = r3
                r22 = r5
                r20 = r7
                r49 = r10
                r10 = r38
                r12 = 1000(0x3e8, double:4.94E-321)
                r38 = r1
                goto L_0x1134
            L_0x0d32:
                r12 = 0
                int r4 = (r5 > r12 ? 1 : (r5 == r12 ? 0 : -1))
                if (r4 > 0) goto L_0x0d42
                int r4 = (r1 > r12 ? 1 : (r1 == r12 ? 0 : -1))
                if (r4 <= 0) goto L_0x0d3d
                goto L_0x0d42
            L_0x0d3d:
                r43 = r3
                r12 = 1000(0x3e8, double:4.94E-321)
                goto L_0x0d8e
            L_0x0d42:
                long r12 = r8.mLastDnsCheckTime
                long r12 = r38 - r12
                com.android.server.wifi.WifiConnectivityMonitor r4 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r4 = r4.mCurrentMode
                r0 = 3
                if (r4 != r0) goto L_0x0d52
                r0 = 30000(0x7530, float:4.2039E-41)
                goto L_0x0d55
            L_0x0d52:
                r0 = 60000(0xea60, float:8.4078E-41)
            L_0x0d55:
                r43 = r3
                long r3 = (long) r0
                int r0 = (r12 > r3 ? 1 : (r12 == r3 ? 0 : -1))
                if (r0 <= 0) goto L_0x0d8c
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0d83
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "PERIODIC DNS CHECK TRIGGER (SIMPLE CONNECTION TEST) - Last DNS check was "
                r0.append(r3)
                long r3 = r8.mLastDnsCheckTime
                long r3 = r38 - r3
                r12 = 1000(0x3e8, double:4.94E-321)
                long r3 = r3 / r12
                r0.append(r3)
                java.lang.String r3 = " seconds ago."
                r0.append(r3)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
                goto L_0x0d85
            L_0x0d83:
                r12 = 1000(0x3e8, double:4.94E-321)
            L_0x0d85:
                r0 = 44
                r8.mNsaQcTrigger = r0
                r33 = 1
                goto L_0x0d8e
            L_0x0d8c:
                r12 = 1000(0x3e8, double:4.94E-321)
            L_0x0d8e:
                r3 = 2
                int r0 = (r5 > r3 ? 1 : (r5 == r3 ? 0 : -1))
                if (r0 > 0) goto L_0x0da9
                r3 = 10
                int r0 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
                if (r0 < 0) goto L_0x0da9
                r0 = 1000(0x3e8, float:1.401E-42)
                if (r7 >= r0) goto L_0x0da9
                java.lang.String r0 = "pull out the line???"
                android.util.Log.i(r15, r0)
                r0 = 31
                r8.mNsaQcTrigger = r0
                r33 = 1
            L_0x0da9:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mNoRxPacketsLimit
                long r3 = (long) r0
                int r0 = (r5 > r3 ? 1 : (r5 == r3 ? 0 : -1))
                if (r0 <= 0) goto L_0x0e25
                long r3 = r8.mLastNeedCheckByPoorRxTime
                long r3 = r38 - r3
                r19 = 30000(0x7530, double:1.4822E-319)
                int r0 = (r3 > r19 ? 1 : (r3 == r19 ? 0 : -1))
                if (r0 <= 0) goto L_0x0df2
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                java.util.Objects.requireNonNull(r0)
                r0 = 22260(0x56f4, float:3.1193E-41)
                long r3 = (long) r0
                int r0 = (r45 > r3 ? 1 : (r45 == r3 ? 0 : -1))
                if (r0 >= 0) goto L_0x0df2
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mGoodRxPacketsBase
                long r3 = (long) r0
                int r0 = (r5 > r3 ? 1 : (r5 == r3 ? 0 : -1))
                if (r0 >= 0) goto L_0x0df2
                r3 = 0
                int r0 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
                if (r0 <= 0) goto L_0x0df2
                r0 = -70
                if (r9 >= r0) goto L_0x0df2
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                int r3 = (int) r5
                java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
                r0.add(r3)
                goto L_0x0df7
            L_0x0df2:
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.clear()
            L_0x0df7:
                r0 = 56
                if (r0 >= r7) goto L_0x0e9d
                r0 = 73
                if (r7 >= r0) goto L_0x0e9d
                r3 = 100
                long r3 = r3 * r1
                long r3 = r3 / r5
                int r0 = (int) r3
                r3 = 90
                if (r3 >= r0) goto L_0x0e23
                r3 = 110(0x6e, float:1.54E-43)
                if (r0 >= r3) goto L_0x0e23
                int r3 = r7 + -10
                if (r3 >= r14) goto L_0x0e23
                if (r14 > r7) goto L_0x0e23
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r3 == 0) goto L_0x0e1d
                java.lang.String r3 = "DNS queries and abnormal responses"
                android.util.Log.w(r15, r3)
            L_0x0e1d:
                r3 = 32
                r8.mNsaQcTrigger = r3
                r33 = 1
            L_0x0e23:
                goto L_0x0e9d
            L_0x0e25:
                boolean r0 = r8.mSYNPacketOnly
                if (r0 == 0) goto L_0x0e5e
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0e34
                java.lang.String r0 = "No [SYN,ACK] or No subsequent transaction"
                android.util.Log.w(r15, r0)
            L_0x0e34:
                long r3 = r8.mGoodRxTime
                r19 = 0
                int r0 = (r3 > r19 ? 1 : (r3 == r19 ? 0 : -1))
                if (r0 <= 0) goto L_0x0e50
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0e47
                java.lang.String r0 = "suspicious No Rx state but staying in good Rx state now"
                android.util.Log.w(r15, r0)
            L_0x0e47:
                r0 = 33
                r8.mNsaQcTrigger = r0
                r33 = 1
                r3 = r40
                goto L_0x0e5a
            L_0x0e50:
                if (r28 == 0) goto L_0x0e58
                r0 = 34
                r8.mNsaQcTrigger = r0
                r3 = 1
                goto L_0x0e5a
            L_0x0e58:
                r3 = r40
            L_0x0e5a:
                r0 = 0
                r8.mSYNPacketOnly = r0
                goto L_0x0e80
            L_0x0e5e:
                r3 = 0
                int r0 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
                if (r0 <= 0) goto L_0x0e79
                int r0 = (r5 > r3 ? 1 : (r5 == r3 ? 0 : -1))
                if (r0 != 0) goto L_0x0e6e
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.clear()
                goto L_0x0e7e
            L_0x0e6e:
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                int r3 = (int) r5
                java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
                r0.add(r3)
                goto L_0x0e7e
            L_0x0e79:
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.clear()
            L_0x0e7e:
                r3 = r40
            L_0x0e80:
                int r0 = r8.mMaybeUseStreaming
                r4 = 3
                if (r0 < r4) goto L_0x0e9b
                if (r28 == 0) goto L_0x0e9b
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0e92
                java.lang.String r0 = "could be in No service state during streaming!"
                android.util.Log.w(r15, r0)
            L_0x0e92:
                r0 = 35
                r8.mNsaQcTrigger = r0
                r33 = 1
                r40 = r3
                goto L_0x0e9d
            L_0x0e9b:
                r40 = r3
            L_0x0e9d:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0ebd
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "mCumulativePoorRx.size : "
                r0.append(r3)
                java.util.ArrayList<java.lang.Integer> r3 = r8.mCumulativePoorRx
                int r3 = r3.size()
                r0.append(r3)
                java.lang.String r0 = r0.toString()
                android.util.Log.i(r15, r0)
            L_0x0ebd:
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                int r0 = r0.size()
                r3 = 3
                if (r0 != r3) goto L_0x0f98
                r0 = 0
                java.util.ArrayList<java.lang.Integer> r3 = r8.mCumulativePoorRx
                java.util.Iterator r3 = r3.iterator()
            L_0x0ecd:
                boolean r4 = r3.hasNext()
                if (r4 == 0) goto L_0x0edf
                java.lang.Object r4 = r3.next()
                java.lang.Integer r4 = (java.lang.Integer) r4
                int r4 = r4.intValue()
                int r0 = r0 + r4
                goto L_0x0ecd
            L_0x0edf:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r3 = r3.mParam
                int r3 = r3.mPoorRxPacketsLimit
                r4 = 3
                int r3 = r3 * r4
                if (r0 >= r3) goto L_0x0f86
                r3 = r38
                r8.mLastNeedCheckByPoorRxTime = r3
                if (r28 == 0) goto L_0x0ef8
                r19 = 1
                r12 = 36
                r8.mNsaQcTrigger = r12
                goto L_0x0efa
            L_0x0ef8:
                r19 = r42
            L_0x0efa:
                r12 = 0
            L_0x0efb:
                r13 = 3
                if (r12 >= r13) goto L_0x0f28
                java.util.ArrayList<java.lang.Integer> r13 = r8.mRxHistory
                java.lang.Object r13 = r13.get(r12)
                java.lang.Integer r13 = (java.lang.Integer) r13
                int r13 = r13.intValue()
                r38 = r0
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mGoodRxPacketsBase
                if (r13 < r0) goto L_0x0f23
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0f21
                java.lang.String r0 = "It's hard to say poor rx"
                android.util.Log.i(r15, r0)
            L_0x0f21:
                r0 = 0
                goto L_0x0f2c
            L_0x0f23:
                int r12 = r12 + 1
                r0 = r38
                goto L_0x0efb
            L_0x0f28:
                r38 = r0
                r0 = r19
            L_0x0f2c:
                if (r0 == 0) goto L_0x0f76
                long r12 = r8.mGoodRxTime
                r22 = 0
                int r12 = (r12 > r22 ? 1 : (r12 == r22 ? 0 : -1))
                if (r12 <= 0) goto L_0x0f4b
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x0f41
                java.lang.String r12 = "suspicious Poor Rx state but staying in good Rx state now"
                android.util.Log.w(r15, r12)
            L_0x0f41:
                r12 = 37
                r8.mNsaQcTrigger = r12
                r33 = 1
                r0 = 0
                r42 = r0
                goto L_0x0f9c
            L_0x0f4b:
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x0f56
                java.lang.String r12 = "Cumulative Rx is in poor status!"
                android.util.Log.w(r15, r12)
            L_0x0f56:
                com.android.server.wifi.WifiConnectivityMonitor r12 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r12 = r12.mCurrentMode
                r13 = 3
                if (r12 != r13) goto L_0x0f74
                r0 = 0
                boolean r12 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r12 == 0) goto L_0x0f6b
                java.lang.String r12 = "check dns in poor rx status of AGG"
                android.util.Log.i(r15, r12)
            L_0x0f6b:
                r12 = 38
                r8.mNsaQcTrigger = r12
                r33 = 1
                r42 = r0
                goto L_0x0f9c
            L_0x0f74:
                r13 = 0
                goto L_0x0f83
            L_0x0f76:
                r22 = 0
                java.util.ArrayList<java.lang.Integer> r12 = r8.mCumulativePoorRx
                r13 = 0
                r12.remove(r13)
                java.util.ArrayList<java.lang.Integer> r12 = r8.mCumulativePoorRx
                r12.trimToSize()
            L_0x0f83:
                r42 = r0
                goto L_0x0f9c
            L_0x0f86:
                r3 = r38
                r13 = 0
                r22 = 0
                r38 = r0
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.remove(r13)
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.trimToSize()
                goto L_0x0f9c
            L_0x0f98:
                r3 = r38
                r22 = 0
            L_0x0f9c:
                long r12 = r8.mLastDnsCheckTime
                long r12 = r3 - r12
                r38 = 1500(0x5dc, double:7.41E-321)
                int r0 = (r12 > r38 ? 1 : (r12 == r38 ? 0 : -1))
                if (r0 <= 0) goto L_0x0fdb
                r12 = 2
                int r0 = (r1 > r12 ? 1 : (r1 == r12 ? 0 : -1))
                if (r0 < 0) goto L_0x0fdb
                r0 = 59
                if (r0 > r7) goto L_0x0fdb
                r0 = 62
                if (r7 > r0) goto L_0x0fdb
                int r0 = (r45 > r10 ? 1 : (r45 == r10 ? 0 : -1))
                if (r0 <= 0) goto L_0x0fcc
                r0 = 500(0x1f4, float:7.0E-43)
                if (r14 >= r0) goto L_0x0fdb
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                java.util.Objects.requireNonNull(r0)
                r0 = 7420(0x1cfc, float:1.0398E-41)
                long r12 = (long) r0
                int r0 = (r45 > r12 ? 1 : (r45 == r12 ? 0 : -1))
                if (r0 >= 0) goto L_0x0fdb
            L_0x0fcc:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x0fd7
                java.lang.String r0 = "SYN packets might be transmitted"
                android.util.Log.i(r15, r0)
            L_0x0fd7:
                r0 = 1
                r8.mSYNPacketOnly = r0
                goto L_0x0fde
            L_0x0fdb:
                r12 = 0
                r8.mSYNPacketOnly = r12
            L_0x0fde:
                int r12 = r8.mMaybeUseStreaming
                com.android.server.wifi.WifiConnectivityMonitor r13 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r13 = r13.mParam
                int r13 = r13.mPoorRxPacketsLimit
                r38 = r1
                long r0 = (long) r13
                int r0 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
                if (r0 <= 0) goto L_0x0ff7
                if (r30 == 0) goto L_0x0ff7
                int r0 = r8.mMaybeUseStreaming
                r1 = 1
                int r2 = r0 + 1
                goto L_0x0ff8
            L_0x0ff7:
                r2 = 0
            L_0x0ff8:
                r8.mMaybeUseStreaming = r2
                r1 = 5
                if (r12 < r1) goto L_0x1014
                int r1 = r8.mMaybeUseStreaming
                if (r1 != 0) goto L_0x1014
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r1 == 0) goto L_0x100c
                java.lang.String r1 = "need to check if there are problems on streaming service"
                android.util.Log.w(r15, r1)
            L_0x100c:
                if (r28 == 0) goto L_0x1014
                r1 = 39
                r8.mNsaQcTrigger = r1
                r33 = 1
            L_0x1014:
                if (r42 != 0) goto L_0x1018
                if (r40 == 0) goto L_0x103d
            L_0x1018:
                long r1 = r8.mLastDnsCheckTime
                long r1 = r3 - r1
                r47 = 20000(0x4e20, double:9.8813E-320)
                int r1 = (r1 > r47 ? 1 : (r1 == r47 ? 0 : -1))
                if (r1 < 0) goto L_0x103d
                java.util.ArrayList<java.lang.Integer> r1 = r8.mCumulativePoorRx
                r1.clear()
                r1 = 0
                r8.mLastDnsCheckTime = r3
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.isInvalidState()
                if (r2 != 0) goto L_0x103b
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r13 = r8.mNsaQcStep
                int r0 = r8.mNsaQcTrigger
                r2.requestInternetCheck(r13, r0)
            L_0x103b:
                r33 = r1
            L_0x103d:
                if (r33 == 0) goto L_0x10f7
                long r0 = r8.mLastDnsCheckTime
                long r0 = r3 - r0
                r47 = 20000(0x4e20, double:9.8813E-320)
                int r0 = (r0 > r47 ? 1 : (r0 == r47 ? 0 : -1))
                if (r0 < 0) goto L_0x10e9
                java.util.ArrayList<java.lang.Integer> r0 = r8.mCumulativePoorRx
                r0.clear()
                r0 = 0
                r8.mSkipRemainingDnsResults = r0
                r0 = 1
                r8.mDnsQueried = r0
                r8.mNsaQcStep = r0
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                java.util.Objects.requireNonNull(r0)
                r0 = 10000(0x2710, float:1.4013E-41)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mCurrentMode
                r2 = 3
                if (r1 != r2) goto L_0x106c
                r0 = 5000(0x1388, float:7.006E-42)
            L_0x106c:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r1 = r1.mParam
                java.lang.String r1 = r1.DEFAULT_URL_STRING
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r2 = r2.inChinaNetwork()
                if (r2 == 0) goto L_0x1089
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r2 = r2.mParam
                java.util.Objects.requireNonNull(r2)
                java.lang.String r1 = "www.qq.com"
                r13 = r1
                goto L_0x108a
            L_0x1089:
                r13 = r1
            L_0x108a:
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread r19 = new com.android.server.wifi.WifiConnectivityMonitor$DnsThread
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r41 = 1
                r47 = r3
                long r3 = (long) r0
                r1 = r19
                r49 = r10
                r10 = r47
                r53 = r3
                r4 = r43
                r43 = r53
                r3 = r41
                r41 = r4
                r4 = r13
                r22 = r5
                r5 = r55
                r20 = r7
                r21 = r12
                r47 = r13
                r12 = 1000(0x3e8, double:4.94E-321)
                r6 = r43
                r1.<init>(r3, r4, r5, r6)
                r1.start()
                r8.mLastDnsCheckTime = r10
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r3 = r1.getId()
                long unused = r2.mDnsThreadID = r3
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r2 == 0) goto L_0x1134
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r3 = "wait needCheck DnsThread results ["
                r2.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r3 = r3.mDnsThreadID
                r2.append(r3)
                java.lang.String r3 = "]"
                r2.append(r3)
                java.lang.String r2 = r2.toString()
                android.util.Log.d(r15, r2)
                goto L_0x1134
            L_0x10e9:
                r22 = r5
                r20 = r7
                r49 = r10
                r21 = r12
                r41 = r43
                r12 = 1000(0x3e8, double:4.94E-321)
                r10 = r3
                goto L_0x1134
            L_0x10f7:
                r22 = r5
                r20 = r7
                r49 = r10
                r21 = r12
                r41 = r43
                r12 = 1000(0x3e8, double:4.94E-321)
                r10 = r3
                goto L_0x1134
            L_0x1105:
                r9 = r0
                r41 = r3
                r22 = r5
                r20 = r7
                r45 = r11
                r10 = r38
                r49 = r43
                r12 = 1000(0x3e8, double:4.94E-321)
                r38 = r1
                goto L_0x1134
            L_0x1117:
                r9 = r0
                r41 = r3
                r22 = r5
                r20 = r7
                r45 = r11
                r10 = r38
                r49 = r43
                r12 = 1000(0x3e8, double:4.94E-321)
                r38 = r1
            L_0x1128:
                r0 = 0
                r8.mSYNPacketOnly = r0
                java.util.ArrayList<java.lang.Integer> r1 = r8.mCumulativePoorRx
                r1.clear()
                r8.mMaybeUseStreaming = r0
                r8.mStayingLowMCS = r0
            L_0x1134:
                r0 = 135221(0x21035, float:1.89485E-40)
                r8.removeMessages(r0)
                r8.sendEmptyMessageDelayed(r0, r12)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r0 = r0.mCurrentMode
                r1 = 3
                if (r0 != r1) goto L_0x11e3
                r0 = -99
                if (r9 >= r0) goto L_0x114d
                r12 = -99
                r9 = r12
            L_0x114d:
                if (r9 <= 0) goto L_0x1152
                r0 = 0
                r12 = r0
                goto L_0x1153
            L_0x1152:
                r12 = r9
            L_0x1153:
                int r0 = r41.mLastPoorRssi
                if (r12 > r0) goto L_0x11d9
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mPassBytesAggressiveMode
                r1 = 2
                int r0 = r0 / r1
                long r2 = (long) r0
                int r0 = (r45 > r2 ? 1 : (r45 == r2 ? 0 : -1))
                if (r0 >= 0) goto L_0x11c1
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mPassBytesAggressiveMode
                int r0 = r0 / r1
                long r2 = (long) r0
                int r0 = (r49 > r2 ? 1 : (r49 == r2 ? 0 : -1))
                if (r0 >= 0) goto L_0x11c1
                long[] r0 = r41.mMaxThroughput
                int r2 = -r12
                r2 = r0[r2]
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$ParameterManager r0 = r0.mParam
                int r0 = r0.mPassBytesAggressiveMode
                int r0 = r0 / r1
                long r0 = (long) r0
                int r0 = (r2 > r0 ? 1 : (r2 == r0 ? 0 : -1))
                if (r0 >= 0) goto L_0x11c1
                boolean r0 = r34.is24GHz()
                if (r0 == 0) goto L_0x1195
                r0 = -75
                if (r12 <= r0) goto L_0x1199
            L_0x1195:
                r0 = -80
                if (r12 > r0) goto L_0x11c1
            L_0x1199:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor.access$10208(r0)
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x11be
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "mStayingPoorRssi : "
                r0.append(r1)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r1 = r1.mStayingPoorRssi
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x11be:
                r2 = r41
                goto L_0x11e1
            L_0x11c1:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x11cc
                java.lang.String r0 = "reset poor rssi"
                android.util.Log.d(r15, r0)
            L_0x11cc:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1 = 0
                int unused = r0.mStayingPoorRssi = r1
                r0 = 5
                r2 = r41
                com.android.server.wifi.WifiConnectivityMonitor.BssidStatistics.access$20620(r2, r0)
                goto L_0x11e1
            L_0x11d9:
                r2 = r41
                r1 = 0
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int unused = r0.mStayingPoorRssi = r1
            L_0x11e1:
                r9 = r12
                goto L_0x11e5
            L_0x11e3:
                r2 = r41
            L_0x11e5:
                long r0 = r8.lastPollTime
                r3 = 0
                int r0 = (r0 > r3 ? 1 : (r0 == r3 ? 0 : -1))
                if (r0 == 0) goto L_0x121b
                android.os.Message r0 = android.os.Message.obtain()
                android.os.Bundle r1 = new android.os.Bundle
                r1.<init>()
                long r3 = r8.lastPollTime
                long r3 = r10 - r3
                java.lang.String r5 = "timeDelta"
                r1.putLong(r5, r3)
                java.lang.String r3 = "diffTxBytes"
                r4 = r49
                r1.putLong(r3, r4)
                java.lang.String r3 = "diffRxBytes"
                r6 = r45
                r1.putLong(r3, r6)
                r3 = 135488(0x21140, float:1.89859E-40)
                r0.what = r3
                r0.setData(r1)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r3.sendMessageToWcmStateMachine(r0)
                goto L_0x121f
            L_0x121b:
                r6 = r45
                r4 = r49
            L_0x121f:
                r8.lastPollTime = r10
                r2 = r56
                goto L_0x13ab
            L_0x1225:
                r2 = r10
                r10 = r38
            L_0x1228:
                java.lang.String r0 = "currentBssid is null."
                android.util.Log.e(r15, r0)
                r0 = 135221(0x21035, float:1.89485E-40)
                r8.removeMessages(r0)
                r8.sendEmptyMessage(r4)
                r2 = r56
                goto L_0x13ab
            L_0x123a:
                r3 = r5
                r34 = r12
                r0 = 135221(0x21035, float:1.89485E-40)
                r8.removeMessages(r0)
                r1 = 135226(0x2103a, float:1.89492E-40)
                r8.removeMessages(r1)
                r0 = 0
                r8.mPollingStarted = r0
                r8.mPublicDnsCheckProcess = r0
                r8.setGoodRxStateNow(r3)
                java.util.ArrayList<java.lang.Integer> r1 = r8.mCumulativePoorRx
                r1.clear()
                java.util.ArrayList<java.lang.Integer> r1 = r8.mRxHistory
                r1.clear()
                r8.mDnsQueried = r0
                r8.mDnsInterrupted = r0
                r8.mMaybeUseStreaming = r0
                r8.mSYNPacketOnly = r0
                r8.mStayingLowMCS = r0
                r8.mNsaQcStep = r0
                r8.mNsaQcTrigger = r0
                r8.lastPollTime = r3
                r2 = r56
                goto L_0x13ab
            L_0x126f:
                r34 = r12
                boolean r1 = r8.mPollingStarted
                if (r1 == 0) goto L_0x1279
                r2 = r56
                goto L_0x13ab
            L_0x1279:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r1 = r1.isMobileHotspot()
                if (r1 == 0) goto L_0x1285
                r2 = r56
                goto L_0x13ab
            L_0x1285:
                boolean r1 = r55.isBackhaulDetectionEnabled()
                if (r1 == 0) goto L_0x1291
                r1 = 135226(0x2103a, float:1.89492E-40)
                r8.sendEmptyMessage(r1)
            L_0x1291:
                r1 = 135221(0x21035, float:1.89485E-40)
                r8.sendEmptyMessage(r1)
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r1.initNetworkStatHistory()
                int r1 = r34.getRssi()
                r8.mLastRssi = r1
                r0 = 1
                r8.mPollingStarted = r0
                r2 = r56
                goto L_0x13ab
            L_0x12a9:
                r34 = r12
                r2 = r56
                goto L_0x13ab
            L_0x12af:
                r34 = r12
                boolean r1 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r1 == 0) goto L_0x12e3
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "[RESULT_DNS_CHECK] : "
                r1.append(r2)
                r2 = r56
                int r3 = r2.arg1
                r1.append(r3)
                java.lang.String r3 = ", from DnsThread id("
                r1.append(r3)
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                long r3 = r3.mDnsThreadID
                r1.append(r3)
                java.lang.String r3 = ")"
                r1.append(r3)
                java.lang.String r1 = r1.toString()
                android.util.Log.i(r15, r1)
                goto L_0x12e5
            L_0x12e3:
                r2 = r56
            L_0x12e5:
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r2.arg1
                int r4 = r2.arg2
                int r1 = r1.checkDnsThreadResult(r3, r4)
                r3 = 0
                r8.mDnsQueried = r3
                boolean r4 = r8.mDnsInterrupted
                if (r4 == 0) goto L_0x1319
                r8.mDnsInterrupted = r3
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x13a7
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "Result: "
                r0.append(r3)
                r0.append(r1)
                java.lang.String r3 = " - This DNS query is interrupted."
                r0.append(r3)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
                goto L_0x13a7
            L_0x1319:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mIsInDhcpSession
                if (r3 != 0) goto L_0x1388
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mIsScanning
                if (r3 != 0) goto L_0x1388
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mIsInRoamSession
                if (r3 == 0) goto L_0x1332
                goto L_0x1388
            L_0x1332:
                if (r1 == 0) goto L_0x13a7
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r3 == 0) goto L_0x133f
                java.lang.String r3 = "single DNS Checking FAILURE"
                android.util.Log.e(r15, r3)
            L_0x133f:
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r3.mCurrentMode
                r4 = 3
                if (r3 != r4) goto L_0x135f
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r3 = r3.mInAggGoodStateNow
                if (r3 == 0) goto L_0x135f
                boolean r3 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r3 == 0) goto L_0x135b
                java.lang.String r3 = "But, do not check the quality in AGG good rx state"
                android.util.Log.e(r15, r3)
            L_0x135b:
                r0 = 1
                r8.mSkipRemainingDnsResults = r0
                goto L_0x13a7
            L_0x135f:
                boolean r0 = r55.isBackhaulDetectionEnabled()
                if (r0 == 0) goto L_0x1372
                int r0 = r34.getRssi()
                r8.mBackhaulQcTriggeredRssi = r0
                int r0 = r8.mBackhaulDetectionReason
                int r0 = r0 + 128
                r8.mNsaQcTrigger = r0
                goto L_0x1376
            L_0x1372:
                r0 = 45
                r8.mNsaQcTrigger = r0
            L_0x1376:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                boolean r0 = r0.isInvalidState()
                if (r0 != 0) goto L_0x13a7
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                int r3 = r8.mNsaQcStep
                int r4 = r8.mNsaQcTrigger
                r0.requestInternetCheck(r3, r4)
                goto L_0x13a7
            L_0x1388:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r0 == 0) goto L_0x13a7
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r3 = "Result: "
                r0.append(r3)
                r0.append(r1)
                java.lang.String r3 = " - This DNS query is interrupted by DHCP session or Scanning."
                r0.append(r3)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r15, r0)
            L_0x13a7:
                r0 = 0
                r8.mPublicDnsCheckProcess = r0
            L_0x13ab:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.NetworkStatsAnalyzer.handleMessage(android.os.Message):void");
        }
    }

    /* access modifiers changed from: private */
    public void sendMessageToWcmStateMachine(Message msg) {
        sendMessage(msg);
    }

    final class DnsThread extends Thread {
        private static final int DNS_DEFAULT_TIMEOUT_MS = 3000;
        private static final String TAG = "WifiConnectivityMonitor.DnsThread";
        private final int DNS_TIMEOUT = -1;
        /* access modifiers changed from: private */
        public final CountDownLatch latch = new CountDownLatch(1);
        /* access modifiers changed from: private */
        public boolean mAlreadyFinished = false;
        /* access modifiers changed from: private */
        public final Handler mCallBackHandler;
        private DnsPingerHandler mDnsPingerHandler = null;
        private final boolean mForce;
        /* access modifiers changed from: private */
        public InetAddress mForcedCheckAddress = null;
        /* access modifiers changed from: private */
        public int mForcedCheckResult = 3;
        /* access modifiers changed from: private */
        public int mForcedCheckRtt = -1;
        private final InetAddressThread mInetAddressThread;
        private long mTimeout = 3000;
        private String mUrl;

        public DnsThread(boolean force, String url, Handler handler, long timeout) {
            this.mInetAddressThread = new InetAddressThread(url);
            this.mCallBackHandler = handler;
            if (timeout >= 1000) {
                this.mTimeout = timeout;
            }
            this.mForce = force;
            this.mUrl = url;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:12:0x0088, code lost:
            if (r1 != null) goto L_0x008a;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:13:0x008a, code lost:
            com.android.server.wifi.WifiConnectivityMonitor.DnsThread.DnsPingerHandler.access$20900(r1);
            r14.mDnsPingerHandler = null;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:14:0x008f, code lost:
            r0.quit();
            r0.interrupt();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:0x0096, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:24:0x00cc, code lost:
            if (r1 == null) goto L_0x008f;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:26:0x00d1, code lost:
            if (r1 == null) goto L_0x008f;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
                r14 = this;
                java.lang.String r0 = "]"
                com.android.server.wifi.WifiConnectivityMonitor r1 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2 = 0
                int unused = r1.mAnalyticsDisconnectReason = r2
                boolean r1 = r14.mForce
                java.lang.String r2 = "DNS_CHECK_TIMEOUT ["
                r3 = -1
                r4 = 3
                r5 = 135202(0x21022, float:1.89458E-40)
                r6 = 0
                java.lang.String r7 = "WifiConnectivityMonitor.DnsThread"
                if (r1 == 0) goto L_0x00d4
                android.os.HandlerThread r0 = new android.os.HandlerThread
                java.lang.String r1 = "dnsPingerThread"
                r0.<init>(r1)
                r0.start()
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler r1 = new com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler     // Catch:{ Exception -> 0x0099 }
                android.os.Looper r10 = r0.getLooper()     // Catch:{ Exception -> 0x0099 }
                android.os.Handler r11 = r14.mCallBackHandler     // Catch:{ Exception -> 0x0099 }
                long r12 = r14.getId()     // Catch:{ Exception -> 0x0099 }
                r8 = r1
                r9 = r14
                r8.<init>(r10, r11, r12)     // Catch:{ Exception -> 0x0099 }
                r14.mDnsPingerHandler = r1     // Catch:{ Exception -> 0x0099 }
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler r1 = r14.mDnsPingerHandler     // Catch:{ Exception -> 0x0099 }
                java.lang.String r8 = r14.mUrl     // Catch:{ Exception -> 0x0099 }
                long r9 = r14.mTimeout     // Catch:{ Exception -> 0x0099 }
                r1.sendDnsPing(r8, r9)     // Catch:{ Exception -> 0x0099 }
                java.util.concurrent.CountDownLatch r1 = r14.latch     // Catch:{ Exception -> 0x0099 }
                long r8 = r14.mTimeout     // Catch:{ Exception -> 0x0099 }
                java.util.concurrent.TimeUnit r10 = java.util.concurrent.TimeUnit.MILLISECONDS     // Catch:{ Exception -> 0x0099 }
                boolean r1 = r1.await(r8, r10)     // Catch:{ Exception -> 0x0099 }
                if (r1 != 0) goto L_0x0075
                boolean r8 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ Exception -> 0x0099 }
                if (r8 == 0) goto L_0x0069
                java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0099 }
                r8.<init>()     // Catch:{ Exception -> 0x0099 }
                r8.append(r2)     // Catch:{ Exception -> 0x0099 }
                long r9 = r14.getId()     // Catch:{ Exception -> 0x0099 }
                r8.append(r9)     // Catch:{ Exception -> 0x0099 }
                java.lang.String r9 = "-F] - latch timeout"
                r8.append(r9)     // Catch:{ Exception -> 0x0099 }
                java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x0099 }
                android.util.Log.d(r7, r8)     // Catch:{ Exception -> 0x0099 }
            L_0x0069:
                android.os.Handler r8 = r14.mCallBackHandler     // Catch:{ Exception -> 0x0099 }
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x0099 }
                android.os.Message r9 = r9.obtainMessage(r5, r4, r3, r6)     // Catch:{ Exception -> 0x0099 }
                r8.sendMessage(r9)     // Catch:{ Exception -> 0x0099 }
                goto L_0x0086
            L_0x0075:
                android.os.Handler r8 = r14.mCallBackHandler     // Catch:{ Exception -> 0x0099 }
                com.android.server.wifi.WifiConnectivityMonitor r9 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x0099 }
                int r10 = r14.mForcedCheckResult     // Catch:{ Exception -> 0x0099 }
                int r11 = r14.mForcedCheckRtt     // Catch:{ Exception -> 0x0099 }
                java.net.InetAddress r12 = r14.mForcedCheckAddress     // Catch:{ Exception -> 0x0099 }
                android.os.Message r9 = r9.obtainMessage(r5, r10, r11, r12)     // Catch:{ Exception -> 0x0099 }
                r8.sendMessage(r9)     // Catch:{ Exception -> 0x0099 }
            L_0x0086:
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler r1 = r14.mDnsPingerHandler
                if (r1 == 0) goto L_0x008f
            L_0x008a:
                r1.finish()
                r14.mDnsPingerHandler = r6
            L_0x008f:
                r0.quit()
                r0.interrupt()
                r0 = 0
                return
            L_0x0097:
                r1 = move-exception
                goto L_0x00cf
            L_0x0099:
                r1 = move-exception
                boolean r8 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ all -> 0x0097 }
                if (r8 == 0) goto L_0x00be
                java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ all -> 0x0097 }
                r8.<init>()     // Catch:{ all -> 0x0097 }
                r8.append(r2)     // Catch:{ all -> 0x0097 }
                long r9 = r14.getId()     // Catch:{ all -> 0x0097 }
                r8.append(r9)     // Catch:{ all -> 0x0097 }
                java.lang.String r2 = "-F] "
                r8.append(r2)     // Catch:{ all -> 0x0097 }
                r8.append(r1)     // Catch:{ all -> 0x0097 }
                java.lang.String r2 = r8.toString()     // Catch:{ all -> 0x0097 }
                android.util.Log.d(r7, r2)     // Catch:{ all -> 0x0097 }
            L_0x00be:
                android.os.Handler r2 = r14.mCallBackHandler     // Catch:{ all -> 0x0097 }
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ all -> 0x0097 }
                android.os.Message r3 = r7.obtainMessage(r5, r4, r3, r6)     // Catch:{ all -> 0x0097 }
                r2.sendMessage(r3)     // Catch:{ all -> 0x0097 }
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler r1 = r14.mDnsPingerHandler
                if (r1 == 0) goto L_0x008f
                goto L_0x008a
            L_0x00cf:
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$DnsPingerHandler r1 = r14.mDnsPingerHandler
                if (r1 == 0) goto L_0x008f
                goto L_0x008a
            L_0x00d4:
                com.samsung.android.server.wifi.Stopwatch r1 = new com.samsung.android.server.wifi.Stopwatch
                r1.<init>()
                com.samsung.android.server.wifi.Stopwatch r1 = r1.start()
                r8 = 1
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$InetAddressThread r9 = r14.mInetAddressThread     // Catch:{ InterruptedException -> 0x01a3 }
                r9.start()     // Catch:{ InterruptedException -> 0x01a3 }
                boolean r9 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ InterruptedException -> 0x01a3 }
                if (r9 == 0) goto L_0x0106
                java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ InterruptedException -> 0x01a3 }
                r9.<init>()     // Catch:{ InterruptedException -> 0x01a3 }
                java.lang.String r10 = "wait mInetAddress result ["
                r9.append(r10)     // Catch:{ InterruptedException -> 0x01a3 }
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$InetAddressThread r10 = r14.mInetAddressThread     // Catch:{ InterruptedException -> 0x01a3 }
                long r10 = r10.getId()     // Catch:{ InterruptedException -> 0x01a3 }
                r9.append(r10)     // Catch:{ InterruptedException -> 0x01a3 }
                r9.append(r0)     // Catch:{ InterruptedException -> 0x01a3 }
                java.lang.String r9 = r9.toString()     // Catch:{ InterruptedException -> 0x01a3 }
                android.util.Log.d(r7, r9)     // Catch:{ InterruptedException -> 0x01a3 }
            L_0x0106:
                java.util.concurrent.CountDownLatch r9 = r14.latch     // Catch:{ InterruptedException -> 0x01a3 }
                long r10 = r14.mTimeout     // Catch:{ InterruptedException -> 0x01a3 }
                java.util.concurrent.TimeUnit r12 = java.util.concurrent.TimeUnit.MILLISECONDS     // Catch:{ InterruptedException -> 0x01a3 }
                boolean r9 = r9.await(r10, r12)     // Catch:{ InterruptedException -> 0x01a3 }
                boolean r10 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ InterruptedException -> 0x01a3 }
                if (r10 == 0) goto L_0x012a
                java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ InterruptedException -> 0x01a3 }
                r10.<init>()     // Catch:{ InterruptedException -> 0x01a3 }
                java.lang.String r11 = "latch result : "
                r10.append(r11)     // Catch:{ InterruptedException -> 0x01a3 }
                r10.append(r9)     // Catch:{ InterruptedException -> 0x01a3 }
                java.lang.String r10 = r10.toString()     // Catch:{ InterruptedException -> 0x01a3 }
                android.util.Log.d(r7, r10)     // Catch:{ InterruptedException -> 0x01a3 }
            L_0x012a:
                if (r9 != 0) goto L_0x015c
                boolean r10 = com.android.server.wifi.WifiConnectivityMonitor.DBG     // Catch:{ InterruptedException -> 0x01a3 }
                if (r10 == 0) goto L_0x014b
                java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ InterruptedException -> 0x01a3 }
                r10.<init>()     // Catch:{ InterruptedException -> 0x01a3 }
                r10.append(r2)     // Catch:{ InterruptedException -> 0x01a3 }
                long r11 = r14.getId()     // Catch:{ InterruptedException -> 0x01a3 }
                r10.append(r11)     // Catch:{ InterruptedException -> 0x01a3 }
                r10.append(r0)     // Catch:{ InterruptedException -> 0x01a3 }
                java.lang.String r2 = r10.toString()     // Catch:{ InterruptedException -> 0x01a3 }
                android.util.Log.d(r7, r2)     // Catch:{ InterruptedException -> 0x01a3 }
            L_0x014b:
                r14.mAlreadyFinished = r8     // Catch:{ InterruptedException -> 0x01a3 }
                android.os.Handler r2 = r14.mCallBackHandler     // Catch:{ InterruptedException -> 0x01a3 }
                com.android.server.wifi.WifiConnectivityMonitor r10 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ InterruptedException -> 0x01a3 }
                android.os.Message r10 = r10.obtainMessage(r5, r4, r3, r6)     // Catch:{ InterruptedException -> 0x01a3 }
                r2.sendMessage(r10)     // Catch:{ InterruptedException -> 0x01a3 }
                r1.stop()     // Catch:{ InterruptedException -> 0x01a3 }
                return
            L_0x015c:
                boolean r2 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                if (r2 == 0) goto L_0x017e
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r3 = "send DNS CHECK Result ["
                r2.append(r3)
                long r3 = r14.getId()
                r2.append(r3)
                r2.append(r0)
                java.lang.String r0 = r2.toString()
                android.util.Log.d(r7, r0)
            L_0x017e:
                android.os.Handler r0 = r14.mCallBackHandler
                if (r0 == 0) goto L_0x019d
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$InetAddressThread r3 = r14.mInetAddressThread
                int r3 = r3.getType()
                long r6 = r1.stop()
                int r4 = (int) r6
                com.android.server.wifi.WifiConnectivityMonitor$DnsThread$InetAddressThread r6 = r14.mInetAddressThread
                java.net.InetAddress r6 = r6.getResultIp()
                android.os.Message r2 = r2.obtainMessage(r5, r3, r4, r6)
                r0.sendMessage(r2)
                goto L_0x01a2
            L_0x019d:
                java.lang.String r0 = "There is no callback handler"
                android.util.Log.d(r7, r0)
            L_0x01a2:
                return
            L_0x01a3:
                r2 = move-exception
                java.lang.StringBuilder r9 = new java.lang.StringBuilder
                r9.<init>()
                java.lang.String r10 = "InterruptedException ["
                r9.append(r10)
                long r10 = r14.getId()
                r9.append(r10)
                r9.append(r0)
                java.lang.String r0 = r9.toString()
                android.util.Log.d(r7, r0)
                boolean r0 = r14.mAlreadyFinished
                if (r0 == 0) goto L_0x01c4
                return
            L_0x01c4:
                r14.mAlreadyFinished = r8
                android.os.Handler r0 = r14.mCallBackHandler
                com.android.server.wifi.WifiConnectivityMonitor r7 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r3 = r7.obtainMessage(r5, r4, r3, r6)
                r0.sendMessage(r3)
                r1.stop()
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.DnsThread.run():void");
        }

        final class InetAddressThread extends Thread {
            private static final String TAG = "WifiConnectivityMonitor.InetAddressThread";
            private final String mHostToResolve;
            private volatile InetAddress mResultIp = null;
            private volatile int mResultType = 0;

            public InetAddressThread(String url) {
                this.mHostToResolve = url;
            }

            public InetAddress getResultIp() {
                return this.mResultIp;
            }

            public int getType() {
                return this.mResultType;
            }

            public void run() {
                char c = 2;
                int i = 1;
                try {
                    if (WifiConnectivityMonitor.this.mNetwork == null) {
                        this.mResultType = 9;
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(TAG, "already disconnected!");
                        }
                        DnsThread.this.latch.countDown();
                        return;
                    }
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(TAG, "DNS requested, Host : " + this.mHostToResolve);
                    }
                    InetAddress[] addresses = WifiConnectivityMonitor.this.mNetwork.getAllByName(this.mHostToResolve);
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(TAG, "DNS response arrived from InetThread [" + getId() + "]");
                    }
                    if (!DnsThread.this.mAlreadyFinished) {
                        boolean unused = DnsThread.this.mAlreadyFinished = true;
                        int length = addresses.length;
                        InetAddress resultIpv6 = null;
                        int i2 = 0;
                        while (i2 < length) {
                            InetAddress address = addresses[i2];
                            if (address instanceof Inet4Address) {
                                int ipByte_1st = address.getAddress()[0] & 255;
                                int ipByte_2nd = address.getAddress()[i] & 255;
                                int ipByte_3rd = address.getAddress()[c] & 255;
                                int ipByte_4th = address.getAddress()[3] & 255;
                                if (ipByte_1st != 10 && (!(ipByte_1st == 192 && ipByte_2nd == 168) && (ipByte_1st != 172 || ipByte_2nd < 16 || ipByte_2nd > 31))) {
                                    if (ipByte_1st != i || ipByte_2nd != 33 || ipByte_3rd != 203 || ipByte_4th != 39) {
                                        this.mResultIp = address;
                                        this.mResultType = 0;
                                        if (WifiConnectivityMonitor.DBG) {
                                            Log.d(TAG, "DNS_CHECK_RESULT_SUCCESS: " + this.mResultIp.toString());
                                        }
                                        DnsThread.this.latch.countDown();
                                        return;
                                    }
                                }
                                if (WifiConnectivityMonitor.DBG) {
                                    WifiConnectivityMonitor.this.log(this.mHostToResolve + " - Dns Response with private Network IP Address !!! - " + ipByte_1st + "." + ipByte_2nd + "." + ipByte_3rd + "." + ipByte_4th);
                                }
                                this.mResultIp = address;
                                this.mResultType = 2;
                                if (WifiConnectivityMonitor.DBG) {
                                    Log.d(TAG, "DNS_CHECK_RESULT_PRIVATE_IP: " + this.mResultIp.toString());
                                }
                            } else {
                                resultIpv6 = address;
                            }
                            i2++;
                            c = 2;
                            i = 1;
                        }
                        if (this.mResultIp == null && resultIpv6 != null) {
                            if (WifiConnectivityMonitor.DBG) {
                                Log.d(TAG, "Dns Response with IPv6");
                            }
                            this.mResultIp = resultIpv6;
                            this.mResultType = 0;
                            if (WifiConnectivityMonitor.DBG) {
                                Log.d(TAG, "DNS_CHECK_RESULT_SUCCESS: " + this.mResultIp.toString());
                            }
                        }
                        DnsThread.this.latch.countDown();
                    } else if (WifiConnectivityMonitor.DBG) {
                        Log.d(TAG, "already finished");
                    }
                } catch (UnknownHostException uhe) {
                    String message = uhe.getLocalizedMessage();
                    if (message == null || !message.contains("DNS service refused")) {
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(TAG, "DNS_CHECK_RESULT_UNKNOWNHOSTEXCEPTION");
                        }
                        this.mResultType = 6;
                    } else {
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(TAG, "DNS_CHECK_RESULT_NO_INTERNET");
                        }
                        int unused2 = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 2;
                        this.mResultType = 1;
                    }
                    DnsThread.this.latch.countDown();
                } catch (SecurityException se) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.e(TAG, "SecurityException : " + se);
                    }
                    this.mResultType = 7;
                    DnsThread.this.latch.countDown();
                } catch (NullPointerException ne) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.e(TAG, "NullPointerException : " + ne);
                    }
                    this.mResultType = 8;
                    DnsThread.this.latch.countDown();
                }
            }
        }

        private class DnsPingerHandler extends Handler {
            Handler mCallbackHandler;
            private DnsCheck mDnsPingerCheck;
            long mId;

            public DnsPingerHandler(Looper looper, Handler callbackHandler, long id) {
                super(looper);
                this.mDnsPingerCheck = new DnsCheck(this, "WifiConnectivityMonitor.DnsPingerHandler");
                this.mCallbackHandler = callbackHandler;
                this.mId = id;
            }

            public void sendDnsPing(String url, long timeout) {
                if (!this.mDnsPingerCheck.requestDnsQuerying(1, (int) timeout, url)) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.e(DnsThread.TAG, "DNS List is empty, need to check quality");
                    }
                    if (DnsThread.this.mCallBackHandler != null) {
                        DnsThread.this.mCallBackHandler.sendMessage(obtainMessage(WifiConnectivityMonitor.RESULT_DNS_CHECK, 3, -1, (Object) null));
                        DnsThread.this.latch.countDown();
                    }
                }
            }

            /* access modifiers changed from: private */
            public void finish() {
                this.mDnsPingerCheck.quit();
                this.mDnsPingerCheck = null;
            }

            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 593920 || i == 593925) {
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.i(DnsThread.TAG, "[DNS_PING_RESULT_SPECIFIC]");
                    }
                    DnsCheck dnsCheck = this.mDnsPingerCheck;
                    if (dnsCheck != null) {
                        try {
                            int dnsResult = dnsCheck.checkDnsResult(msg.arg1, msg.arg2, 1);
                            if (dnsResult != 10) {
                                if (WifiConnectivityMonitor.DBG) {
                                    Log.d(DnsThread.TAG, "send DNS CHECK Result [" + this.mId + "]");
                                }
                                int unused = DnsThread.this.mForcedCheckResult = dnsResult;
                                int unused2 = DnsThread.this.mForcedCheckRtt = msg.arg2;
                                InetAddress unused3 = DnsThread.this.mForcedCheckAddress = (InetAddress) msg.obj;
                                DnsThread.this.latch.countDown();
                            } else if (WifiConnectivityMonitor.SMARTCM_DBG) {
                                Log.d(DnsThread.TAG, "wait until the responses about remained DNS Request arrive!");
                            }
                        } catch (NullPointerException ne) {
                            Log.e(DnsThread.TAG, "DnsPingerHandler - " + ne);
                        }
                    }
                } else {
                    Log.e(DnsThread.TAG, "Ignore msg id : " + msg.what);
                }
            }
        }

        class DnsCheck {
            private int[] mDnsCheckSuccesses;
            private String mDnsCheckTAG = null;
            private List<InetAddress> mDnsList;
            private DnsPinger mDnsPinger;
            private String[] mDnsResponseStrs;
            private List<InetAddress> mDnsServerList = null;
            private HashMap<Integer, Integer> mIdDnsMap = new HashMap<>();

            public DnsCheck(Handler handler, String tag) {
                this.mDnsPinger = new DnsPinger(WifiConnectivityMonitor.this.mContext, tag, handler.getLooper(), handler, 1);
                this.mDnsCheckTAG = tag;
                this.mDnsPinger.setCurrentLinkProperties(WifiConnectivityMonitor.this.mLinkProperties);
            }

            public boolean requestDnsQuerying(int num, int timeoutMS, String url) {
                List<InetAddress> dnses;
                boolean requested = false;
                this.mDnsList = new ArrayList();
                if (!(WifiConnectivityMonitor.this.mLinkProperties == null || (dnses = WifiConnectivityMonitor.this.mLinkProperties.getDnsServers()) == null || dnses.size() == 0)) {
                    this.mDnsServerList = new ArrayList(dnses);
                }
                List<InetAddress> dnses2 = this.mDnsServerList;
                if (dnses2 != null) {
                    this.mDnsList.addAll(dnses2);
                }
                int numDnses = this.mDnsList.size();
                this.mDnsCheckSuccesses = new int[numDnses];
                this.mDnsResponseStrs = new String[numDnses];
                for (int i = 0; i < numDnses; i++) {
                    this.mDnsResponseStrs[i] = "";
                }
                WifiInfo info = WifiConnectivityMonitor.this.syncGetCurrentWifiInfo();
                if (WifiConnectivityMonitor.DBG) {
                    try {
                        Log.d(DnsThread.TAG, String.format("Pinging %s on ssid [%s]: ", new Object[]{this.mDnsList, info.getSSID()}));
                    } catch (Exception e) {
                        if (WifiConnectivityMonitor.SMARTCM_DBG) {
                            e.printStackTrace();
                        }
                    }
                }
                this.mIdDnsMap.clear();
                for (int i2 = 0; i2 < num; i2++) {
                    int j = 0;
                    while (j < numDnses) {
                        try {
                            if (this.mDnsList.get(j) == null || this.mDnsList.get(j).isLoopbackAddress()) {
                                Log.d(DnsThread.TAG, "Loopback address (::1) is detected at DNS" + j);
                            } else {
                                if (url == null) {
                                    HashMap<Integer, Integer> hashMap = this.mIdDnsMap;
                                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                                    hashMap.put(Integer.valueOf(this.mDnsPinger.pingDnsAsync(this.mDnsList.get(j), timeoutMS, (i2 * 0) + 100)), Integer.valueOf(j));
                                } else {
                                    HashMap<Integer, Integer> hashMap2 = this.mIdDnsMap;
                                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                                    hashMap2.put(Integer.valueOf(this.mDnsPinger.pingDnsAsyncSpecificForce(this.mDnsList.get(j), timeoutMS, (i2 * 0) + 100, url)), Integer.valueOf(j));
                                }
                                requested = true;
                            }
                            j++;
                        } catch (IndexOutOfBoundsException e2) {
                            if (WifiConnectivityMonitor.DBG) {
                                Log.i(DnsThread.TAG, "IndexOutOfBoundsException");
                            }
                        }
                    }
                }
                if (WifiConnectivityMonitor.SMARTCM_DBG != 0) {
                    Log.i(DnsThread.TAG, "[REQUEST] " + this.mDnsCheckTAG + " : " + this.mIdDnsMap);
                }
                return requested;
            }

            public int checkDnsResult(int pingID, int pingResponseTime, int minDnsResponse) {
                int rssi;
                int result = checkDnsResultCore(pingID, pingResponseTime, minDnsResponse);
                if (result == 10) {
                    return result;
                }
                if (result == 3 && WifiConnectivityMonitor.this.mWifiInfo != null && (rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi()) >= -50) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(DnsThread.TAG, "Dns Timeout but RSSI high : " + rssi + " dBm. Link is okay and DNS service is not responsive. -> NO_INTERNET");
                    }
                    result = 5;
                }
                WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT, result, pingResponseTime);
                return result;
            }

            public int checkDnsResultCore(int pingID, int pingResponseTime, int minDnsResponse) {
                Integer dnsServerId = this.mIdDnsMap.get(Integer.valueOf(pingID));
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.i(DnsThread.TAG, "[RESPONSE] " + this.mDnsCheckTAG + " : " + this.mIdDnsMap);
                }
                if (dnsServerId == null) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(DnsThread.TAG, "Skip a Dns response with ID - " + pingID);
                    }
                    return 10;
                }
                int[] iArr = this.mDnsCheckSuccesses;
                if (iArr == null || iArr.length <= dnsServerId.intValue()) {
                    Log.e(DnsThread.TAG, "Not available to check dns results");
                    quit();
                    WifiConnectivityMonitor.this.mCurrentQcFail.error = 3;
                    WifiConnectivityMonitor.this.mCurrentQcFail.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                    return 3;
                }
                this.mIdDnsMap.remove(Integer.valueOf(pingID));
                if (pingResponseTime >= 0) {
                    int[] iArr2 = this.mDnsCheckSuccesses;
                    int intValue = dnsServerId.intValue();
                    iArr2[intValue] = iArr2[intValue] + 1;
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.e(DnsThread.TAG, "mDnsCheckSuccesses[" + dnsServerId + "] " + this.mDnsCheckSuccesses[dnsServerId.intValue()]);
                    }
                }
                try {
                    if (this.mDnsResponseStrs == null) {
                        Log.e(DnsThread.TAG, "mDnsResponseStrs is null");
                    } else if (pingResponseTime >= 0) {
                        StringBuilder sb = new StringBuilder();
                        String[] strArr = this.mDnsResponseStrs;
                        int intValue2 = dnsServerId.intValue();
                        sb.append(strArr[intValue2]);
                        sb.append("|");
                        sb.append(pingResponseTime);
                        strArr[intValue2] = sb.toString();
                    } else {
                        StringBuilder sb2 = new StringBuilder();
                        String[] strArr2 = this.mDnsResponseStrs;
                        int intValue3 = dnsServerId.intValue();
                        sb2.append(strArr2[intValue3]);
                        sb2.append("|x");
                        strArr2[intValue3] = sb2.toString();
                    }
                    if (this.mDnsCheckSuccesses[dnsServerId.intValue()] >= minDnsResponse) {
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(DnsThread.TAG, makeLogString() + "  SUCCESS");
                        } else {
                            Log.d(DnsThread.TAG, makeLogString());
                        }
                        quit();
                        if (pingResponseTime != 2) {
                            return 0;
                        }
                        WifiConnectivityMonitor.this.mCurrentQcFail.error = 1;
                        WifiConnectivityMonitor.this.mCurrentQcFail.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                        int unused = WifiConnectivityMonitor.this.mAnalyticsDisconnectReason = 1;
                        return 2;
                    } else if (pingResponseTime == -3) {
                        List<Integer> removePingIdList = new ArrayList<>();
                        for (Map.Entry<Integer, Integer> ent : this.mIdDnsMap.entrySet()) {
                            if (dnsServerId.equals(ent.getValue())) {
                                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                                    Log.d(DnsThread.TAG, "checkDnsResult - Ping# " + ent.getKey() + " to DnsServer " + ent.getValue() + " (removed)");
                                }
                                removePingIdList.add(ent.getKey());
                            } else if (WifiConnectivityMonitor.SMARTCM_DBG) {
                                Log.d(DnsThread.TAG, "checkDnsResult - Ping# " + ent.getKey() + " to DnsServer# " + ent.getValue());
                            }
                        }
                        for (Integer removeId : removePingIdList) {
                            this.mIdDnsMap.remove(removeId);
                        }
                        if (!this.mIdDnsMap.isEmpty()) {
                            return 10;
                        }
                        if (WifiConnectivityMonitor.DBG) {
                            Log.e(DnsThread.TAG, "DNS gets no results");
                        }
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(DnsThread.TAG, makeLogString() + "  FAILURE ");
                        } else {
                            Log.d(DnsThread.TAG, makeLogString());
                        }
                        quit();
                        WifiConnectivityMonitor.this.mCurrentQcFail.error = 2;
                        WifiConnectivityMonitor.this.mCurrentQcFail.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                        return 1;
                    } else if (!this.mIdDnsMap.isEmpty()) {
                        return 10;
                    } else {
                        if (WifiConnectivityMonitor.DBG) {
                            Log.e(DnsThread.TAG, "DNS Checking FAILURE");
                        }
                        if (WifiConnectivityMonitor.DBG) {
                            Log.d(DnsThread.TAG, makeLogString() + "  FAILURE");
                        } else {
                            Log.d(DnsThread.TAG, makeLogString());
                        }
                        quit();
                        WifiConnectivityMonitor.this.mCurrentQcFail.error = 4;
                        WifiConnectivityMonitor.this.mCurrentQcFail.line = Thread.currentThread().getStackTrace()[2].getLineNumber();
                        WifiConnectivityMonitor.this.mCurrentQcFail.currentDnsList = this.mDnsServerList;
                        return 3;
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.i(DnsThread.TAG, "mDnsResponseStrs IndexOutOfBoundsException");
                    return 3;
                }
            }

            public void quit() {
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.i(DnsThread.TAG, "[quit] " + this.mDnsCheckTAG);
                }
                this.mIdDnsMap.clear();
                this.mDnsPinger.cancelPings();
            }

            private void clear() {
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.i(DnsThread.TAG, "[clear] " + this.mDnsCheckTAG);
                }
                this.mDnsPinger.clear();
            }

            public boolean isDnsCheckOngoing() {
                HashMap<Integer, Integer> hashMap = this.mIdDnsMap;
                if (hashMap == null || hashMap.isEmpty()) {
                    return false;
                }
                return true;
            }

            private String makeLogString() {
                String logStr = "";
                String[] strArr = this.mDnsResponseStrs;
                if (strArr != null) {
                    for (String respStr : strArr) {
                        logStr = logStr + " [" + respStr + "]";
                    }
                }
                return logStr;
            }
        }
    }

    public int checkDnsThreadResult(int resultType, int responseTime) {
        WifiInfo wifiInfo;
        int rssi;
        Log.d(TAG, "DNS resultType : " + resultType + ", responseTime : " + responseTime);
        if (!this.mIsUsingProxy || resultType == 0) {
            if (resultType == 3 && (wifiInfo = this.mWifiInfo) != null && (rssi = wifiInfo.getRssi()) >= -50) {
                if (DBG) {
                    Log.d(TAG, "Dns Timeout but RSSI high : " + rssi + " dBm. Link is okay and DNG service is not responsive. -> NO_INTERNET");
                }
                resultType = 5;
            }
            sendMessage(CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT_TYPE, resultType, responseTime);
            return resultType;
        }
        Log.d(TAG, "DNS check result is not successful. TYPE: " + resultType + " Proxy is being used. Ignore the result.");
        return 11;
    }

    /* access modifiers changed from: private */
    public boolean initDataUsage() {
        try {
            NetworkInfo activeNetwork = getCm().getActiveNetworkInfo();
            boolean isMobile = false;
            if (activeNetwork != null) {
                boolean isConnected = activeNetwork.isConnectedOrConnecting();
                isMobile = activeNetwork.getType() == 0;
            }
            State currentState = getCurrentState();
            if ((!isValidState() || currentState == this.mValidNoCheckState) && !isMobile && this.mStatsService != null && this.mStatsSession != null) {
                return true;
            }
            if (this.mStatsService == null) {
                this.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
            }
            if (this.mStatsSession == null) {
                try {
                    this.mStatsSession = this.mStatsService.openSession();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.mStatsSession = null;
                    return false;
                }
            }
            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private long requestDataUsage(int networkType, int uid) {
        NetworkTemplate mNetworkTemplate;
        if (networkType == 0) {
            mNetworkTemplate = NetworkTemplate.buildTemplateMobileAll(getSubscriberId(networkType));
        } else if (networkType != 1) {
            return -1;
        } else {
            mNetworkTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        }
        try {
            NetworkStatsHistory networkStatsHistory = collectHistoryForUid(mNetworkTemplate, uid, -1);
            if (DBG) {
                Log.i(TAG, "load:: " + networkType + " :: [uid-" + uid + "] getTotalBytes : " + Formatter.formatFileSize(this.mContext, networkStatsHistory.getTotalBytes()));
            }
            return networkStatsHistory.getTotalBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getSubscriberId(int networkType) {
        if (networkType == 0) {
            return ((TelephonyManager) this.mContext.getSystemService("phone")).getSubscriberId();
        }
        return "";
    }

    private NetworkStatsHistory collectHistoryForUid(NetworkTemplate template, int uid, int set) throws RemoteException {
        initDataUsage();
        return this.mStatsSession.getHistoryForUid(template, uid, set, 0, 10);
    }

    private class VolumeWeightedEMA {
        private final double mAlpha;
        private double mProduct = WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;
        /* access modifiers changed from: private */
        public double mValue = WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;
        /* access modifiers changed from: private */
        public double mVolume = WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;

        public VolumeWeightedEMA(double coefficient) {
            this.mAlpha = coefficient;
        }

        public void update(double newValue, int newVolume) {
            if (newVolume > 0) {
                double d = this.mAlpha;
                this.mProduct = (d * ((double) newVolume) * newValue) + ((1.0d - d) * this.mProduct);
                this.mVolume = (((double) newVolume) * d) + ((1.0d - d) * this.mVolume);
                this.mValue = this.mProduct / this.mVolume;
            }
        }
    }

    private class BssidStatistics {
        /* access modifiers changed from: private */
        public final String mBssid;
        /* access modifiers changed from: private */
        public long mBssidAvoidTimeMax;
        public boolean mBssidNoInternet = false;
        public HashMap<Integer, RssiLevelQosInfo> mBssidQosMap = new HashMap<>();
        public ScanResult mCurrentBssidScanInfo = null;
        private boolean mDnsAvailable;
        public int mEnhancedTargetRssi = 0;
        private VolumeWeightedEMA[] mEntries;
        private int mEntriesSize;
        /* access modifiers changed from: private */
        public int mGoodLinkTargetCount;
        private int mGoodLinkTargetIndex;
        /* access modifiers changed from: private */
        public int mGoodLinkTargetRssi;
        public boolean mIsCaptivePortal = false;
        /* access modifiers changed from: private */
        public int mLastGoodRxRssi;
        /* access modifiers changed from: private */
        public int mLastPoorReason;
        /* access modifiers changed from: private */
        public int mLastPoorRssi;
        private long mLastTimeGood;
        private long mLastTimePoor;
        /* access modifiers changed from: private */
        public long mLastTimeSample;
        public int mLatestDnsResult = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_UNKNOWN;
        public int mLatestLevel2Rssi = -99;
        public int mLatestQcFailRssi = -99;
        private long[] mMaxStreamTP;
        /* access modifiers changed from: private */
        public long[] mMaxThroughput;
        public int mNumberOfConnections;
        private int mRssiBase;
        public String mSsid;
        /* access modifiers changed from: private */
        public int netId;

        static /* synthetic */ int access$13120(BssidStatistics x0, int x1) {
            int i = x0.mGoodLinkTargetRssi - x1;
            x0.mGoodLinkTargetRssi = i;
            return i;
        }

        static /* synthetic */ int access$20620(BssidStatistics x0, int x1) {
            int i = x0.mLastPoorRssi - x1;
            x0.mLastPoorRssi = i;
            return i;
        }

        public BssidStatistics(String bssid, int netId2) {
            this.mBssid = bssid;
            this.netId = netId2;
            this.mRssiBase = WifiConnectivityMonitor.BSSID_STAT_RANGE_LOW_DBM;
            this.mEntriesSize = 61;
            this.mEntries = new VolumeWeightedEMA[this.mEntriesSize];
            for (int i = 0; i < this.mEntriesSize; i++) {
                this.mEntries[i] = new VolumeWeightedEMA(WifiConnectivityMonitor.EXP_COEFFICIENT_RECORD);
            }
            this.mMaxThroughput = new long[100];
            this.mMaxStreamTP = new long[100];
            for (int i2 = 0; i2 < 100; i2++) {
                this.mMaxThroughput[i2] = 0;
                this.mMaxStreamTP[i2] = 0;
            }
            this.mGoodLinkTargetRssi = -200;
            this.mBssidAvoidTimeMax = 0;
            this.mLastGoodRxRssi = 0;
            this.mLastPoorRssi = -200;
            this.mLastPoorReason = 1;
            this.mDnsAvailable = false;
            this.mNumberOfConnections = 0;
        }

        public void updateLoss(int rssi, double value, int volume) {
            int index;
            if (volume > 0 && (index = rssi - this.mRssiBase) >= 0 && index < this.mEntriesSize) {
                this.mEntries[index].update(value, volume);
                if (rssi >= this.mLastGoodRxRssi && value >= 0.2d && this.mEntries[index].mValue >= WifiConnectivityMonitor.EXP_COEFFICIENT_RECORD) {
                    this.mLastGoodRxRssi = 0;
                    Log.d(WifiConnectivityMonitor.TAG, "lose good rx position : " + rssi + " loss=" + value);
                }
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    DecimalFormat df = new DecimalFormat("#.##");
                    Log.d(WifiConnectivityMonitor.TAG, "Cache updated: loss[" + rssi + "]=" + df.format(this.mEntries[index].mValue * 100.0d) + "% volume=" + df.format(this.mEntries[index].mVolume));
                }
            }
        }

        public void updateGoodRssi(int rssi) {
            if (rssi < this.mLastGoodRxRssi) {
                this.mLastGoodRxRssi = rssi;
                int i = this.mGoodLinkTargetRssi;
                int i2 = this.mLastGoodRxRssi;
                if (i > i2) {
                    this.mGoodLinkTargetRssi = i2;
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.i(WifiConnectivityMonitor.TAG, "lower mGoodLinkTargetRssi : " + this.mLastPoorRssi);
                    }
                }
                int i3 = this.mLastPoorRssi;
                if (i3 >= this.mLastGoodRxRssi) {
                    this.mLastPoorRssi = i3 - 3;
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.i(WifiConnectivityMonitor.TAG, "lower mLastPoorRssi : " + this.mLastPoorRssi);
                    }
                }
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Log.i(WifiConnectivityMonitor.TAG, "new good RSSI : " + rssi);
                }
                if (WifiConnectivityMonitor.SMARTCM_DBG) {
                    Context access$1400 = WifiConnectivityMonitor.this.mContext;
                    Toast.makeText(access$1400, "new good RSSI : " + rssi, 0).show();
                }
            }
        }

        public void updateMaxThroughput(int rssi, long tput, boolean isStreaming) {
            if (-100 < rssi && rssi < 0) {
                if (isStreaming) {
                    long[] jArr = this.mMaxStreamTP;
                    if (jArr[-rssi] < tput) {
                        jArr[-rssi] = tput;
                        if (WifiConnectivityMonitor.SMARTCM_DBG) {
                            Log.i(WifiConnectivityMonitor.TAG, "new Max stream TP[" + rssi + "] : " + tput);
                            return;
                        }
                        return;
                    }
                    return;
                }
                long[] jArr2 = this.mMaxThroughput;
                if (jArr2[-rssi] < tput) {
                    jArr2[-rssi] = tput;
                    if (WifiConnectivityMonitor.SMARTCM_DBG) {
                        Log.i(WifiConnectivityMonitor.TAG, "new Max TP[" + rssi + "] : " + tput);
                    }
                }
            }
        }

        public double presetLoss(int rssi) {
            if (rssi <= -90) {
                return 1.0d;
            }
            if (rssi > 0) {
                return WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;
            }
            if (WifiConnectivityMonitor.sPresetLoss == null) {
                double[] unused = WifiConnectivityMonitor.sPresetLoss = new double[90];
                for (int i = 0; i < 90; i++) {
                    WifiConnectivityMonitor.sPresetLoss[i] = 1.0d / Math.pow((double) (90 - i), 1.5d);
                }
            }
            return WifiConnectivityMonitor.sPresetLoss[-rssi];
        }

        public boolean poorLinkDetected(int rssi, int extraInfo) {
            if (!WifiConnectivityMonitor.this.mClientModeImpl.isConnected()) {
                Log.i(WifiConnectivityMonitor.TAG, "already disconnected");
                return true;
            } else if (extraInfo == 2) {
                return true;
            } else {
                this.mLastPoorReason = extraInfo;
                this.mLastPoorRssi = rssi;
                poorLinkDetected(rssi);
                if (WifiConnectivityMonitor.SMARTCM_DBG && rssi <= WifiConnectivityMonitor.BSSID_STAT_RANGE_HIGH_DBM && rssi > -100 && rssi <= 0) {
                    Log.d(WifiConnectivityMonitor.TAG, "[" + rssi + "] loss=" + this.mEntries[rssi - this.mRssiBase].mValue + ", maxTP=" + this.mMaxThroughput[-rssi] + ", maxStream=" + this.mMaxStreamTP[-rssi]);
                }
                this.mBssidAvoidTimeMax = SystemClock.elapsedRealtime() + 300000;
                if (WifiConnectivityMonitor.DBG) {
                    Log.d(WifiConnectivityMonitor.TAG, "Poor link detected enhanced recovery, avoidMax=" + 300000 + ", mBssidAvoidTimeMax=" + this.mBssidAvoidTimeMax);
                }
                if (this.mGoodLinkTargetRssi < -82) {
                    this.mGoodLinkTargetRssi = -82;
                }
                if (!WifiConnectivityMonitor.this.isValidState() || this.mGoodLinkTargetRssi - rssi >= 10) {
                    int i = this.mEnhancedTargetRssi;
                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                    if (i != 5) {
                        this.mEnhancedTargetRssi = 0;
                    }
                } else {
                    Objects.requireNonNull(WifiConnectivityMonitor.this.mParam);
                    this.mEnhancedTargetRssi = 5;
                    this.mGoodLinkTargetRssi += this.mEnhancedTargetRssi;
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "mGoodLinkTargetRssi is updated : " + this.mGoodLinkTargetRssi);
                    }
                }
                return true;
            }
        }

        public boolean poorLinkDetected(int rssi) {
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, "Poor link detected, rssi=" + rssi);
            }
            long now = SystemClock.elapsedRealtime();
            long lastGood = now - this.mLastTimeGood;
            long lastPoor = now - this.mLastTimePoor;
            int from = (WifiConnectivityMonitor.this.mCurrentMode == 3 ? 3 : 5) + rssi;
            int i = WifiConnectivityMonitor.this.mCurrentMode == 3 ? 15 : 20;
            WifiConnectivityMonitor.this.mParam.mLastPoorDetectedTime = now;
            int newRssiTarget = findRssiTarget(from, i + rssi, WifiConnectivityMonitor.GOOD_LINK_LOSS_THRESHOLD);
            if (newRssiTarget > this.mGoodLinkTargetRssi) {
                this.mGoodLinkTargetRssi = newRssiTarget;
            }
            this.mGoodLinkTargetCount = 8;
            this.mBssidAvoidTimeMax = now + 30000;
            Log.d(WifiConnectivityMonitor.TAG, "goodRssi=" + this.mGoodLinkTargetRssi + " goodCount=" + this.mGoodLinkTargetCount + " lastGood=" + lastGood + " lastPoor=" + lastPoor + " avoidMax=" + 30000);
            return true;
        }

        public void newLinkDetected() {
            long now = SystemClock.elapsedRealtime();
            WifiInfo info = WifiConnectivityMonitor.this.mWifiInfo;
            if (this.mBssidAvoidTimeMax > now) {
                if (WifiConnectivityMonitor.DBG) {
                    Log.d(WifiConnectivityMonitor.TAG, "Previous avoidance still in effect, rssi=" + this.mGoodLinkTargetRssi + " count=" + this.mGoodLinkTargetCount);
                }
                if (this.mBssidAvoidTimeMax >= now + 30000) {
                    return;
                }
                if (info == null || info.getRssi() <= -64) {
                    this.mBssidAvoidTimeMax = 120000 + now;
                } else {
                    this.mBssidAvoidTimeMax = 30000 + now;
                }
            } else {
                this.mDnsAvailable = false;
                if (this.mGoodLinkTargetRssi > -200) {
                    this.mGoodLinkTargetCount = 5;
                } else {
                    this.mGoodLinkTargetRssi = findRssiTarget(WifiConnectivityMonitor.BSSID_STAT_RANGE_LOW_DBM, WifiConnectivityMonitor.BSSID_STAT_RANGE_HIGH_DBM, WifiConnectivityMonitor.GOOD_LINK_LOSS_THRESHOLD);
                    this.mGoodLinkTargetCount = 0;
                }
                this.mBssidAvoidTimeMax = now;
                if (WifiConnectivityMonitor.DBG) {
                    Log.d(WifiConnectivityMonitor.TAG, "New link verifying target set, rssi=" + this.mGoodLinkTargetRssi + " count=" + this.mGoodLinkTargetCount);
                }
            }
        }

        public int findRssiTarget(int from, int to, double threshold) {
            int i = from;
            int i2 = to;
            if (this.mGoodLinkTargetRssi == -200) {
                Log.d(WifiConnectivityMonitor.TAG, "Scan target found: initial rssi=-90");
                return -90;
            } else if (WifiConnectivityMonitor.this.mCurrentMode == 3) {
                return findAGGRssiTarget(i, i2, WifiConnectivityMonitor.this.mParam.mPassBytesAggressiveMode);
            } else {
                int i3 = this.mRssiBase;
                int from2 = i - i3;
                int to2 = i2 - i3;
                int emptyCount = 0;
                int d = from2 < to2 ? 1 : -1;
                for (int i4 = from2; i4 != to2; i4 += d) {
                    if (i4 < 0 || i4 >= this.mEntriesSize || this.mEntries[i4].mVolume <= 1.0d) {
                        emptyCount++;
                        if (emptyCount >= 3) {
                            int rssi = this.mRssiBase + i4;
                            double lossPreset = presetLoss(rssi);
                            if (lossPreset < threshold) {
                                if (WifiConnectivityMonitor.DBG) {
                                    DecimalFormat df = new DecimalFormat("#.##");
                                    Log.d(WifiConnectivityMonitor.TAG, "Scan target found: rssi=" + rssi + " threshold=" + df.format(threshold * 100.0d) + "% value=" + df.format(100.0d * lossPreset) + "% volume=preset");
                                }
                                return rssi;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        emptyCount = 0;
                        if (this.mEntries[i4].mValue < threshold) {
                            int rssi2 = this.mRssiBase + i4;
                            if (WifiConnectivityMonitor.DBG) {
                                DecimalFormat df2 = new DecimalFormat("#.##");
                                Log.d(WifiConnectivityMonitor.TAG, "Scan target found: rssi=" + rssi2 + " threshold=" + df2.format(threshold * 100.0d) + "% value=" + df2.format(this.mEntries[i4].mValue * 100.0d) + "% volume=" + df2.format(this.mEntries[i4].mVolume));
                            }
                            return rssi2;
                        }
                    }
                }
                return this.mRssiBase + to2;
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:33:0x0096, code lost:
            if (com.android.server.wifi.WifiConnectivityMonitor.access$000() == false) goto L_0x00ac;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x0098, code lost:
            android.util.Log.i(com.android.server.wifi.WifiConnectivityMonitor.TAG, "found max TP RSSI : " + r6);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:35:0x00ac, code lost:
            r2 = r6;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int findAGGRssiTarget(int r12, int r13, int r14) {
            /*
                r11 = this;
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                java.lang.String r1 = "WifiConnectivityMonitor"
                if (r0 == 0) goto L_0x0052
                r0 = r12
            L_0x0009:
                if (r0 > r13) goto L_0x0052
                r2 = -100
                if (r0 <= r2) goto L_0x004f
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r3 = "W["
                r2.append(r3)
                r2.append(r0)
                java.lang.String r3 = "] : "
                r2.append(r3)
                long[] r4 = r11.mMaxThroughput
                int r5 = -r0
                r4 = r4[r5]
                r2.append(r4)
                java.lang.String r2 = r2.toString()
                android.util.Log.i(r1, r2)
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r4 = "S["
                r2.append(r4)
                r2.append(r0)
                r2.append(r3)
                long[] r3 = r11.mMaxStreamTP
                int r4 = -r0
                r3 = r3[r4]
                r2.append(r3)
                java.lang.String r2 = r2.toString()
                android.util.Log.i(r1, r2)
            L_0x004f:
                int r0 = r0 + 1
                goto L_0x0009
            L_0x0052:
                r0 = -45
                if (r12 < r0) goto L_0x0057
                return r12
            L_0x0057:
                if (r13 <= r0) goto L_0x005b
                r13 = -45
            L_0x005b:
                r0 = -99
                if (r12 >= r0) goto L_0x0061
                r12 = -99
            L_0x0061:
                int r2 = r12 + 2
                int r3 = r14 * 10
                int r4 = r11.mRssiBase
                int r12 = r12 - r4
                int r13 = r13 - r4
                if (r12 >= r13) goto L_0x006d
                r4 = 1
                goto L_0x006e
            L_0x006d:
                r4 = -1
            L_0x006e:
                r5 = r12
            L_0x006f:
                if (r5 == r13) goto L_0x00ad
                int r6 = r11.mRssiBase
                int r6 = r6 + r5
                if (r6 >= r0) goto L_0x0078
                r6 = -99
            L_0x0078:
                if (r6 <= 0) goto L_0x007b
                r6 = 0
            L_0x007b:
                long r7 = (long) r14
                long[] r9 = r11.mMaxThroughput
                int r10 = -r6
                r9 = r9[r10]
                int r7 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1))
                if (r7 <= 0) goto L_0x0092
                long r7 = (long) r3
                long[] r9 = r11.mMaxStreamTP
                int r10 = -r6
                r9 = r9[r10]
                int r7 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1))
                if (r7 > 0) goto L_0x0090
                goto L_0x0092
            L_0x0090:
                int r5 = r5 + r4
                goto L_0x006f
            L_0x0092:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.SMARTCM_DBG
                if (r0 == 0) goto L_0x00ac
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r7 = "found max TP RSSI : "
                r0.append(r7)
                r0.append(r6)
                java.lang.String r0 = r0.toString()
                android.util.Log.i(r1, r0)
            L_0x00ac:
                r2 = r6
            L_0x00ad:
                boolean r0 = com.android.server.wifi.WifiConnectivityMonitor.DBG
                java.lang.String r5 = "Scan target found: rssi="
                if (r0 == 0) goto L_0x00f0
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                r0.append(r5)
                r0.append(r2)
                java.lang.String r5 = " threshold="
                r0.append(r5)
                r0.append(r14)
                java.lang.String r5 = " maxTP="
                r0.append(r5)
                long[] r6 = r11.mMaxThroughput
                int r7 = -r2
                r6 = r6[r7]
                r0.append(r6)
                java.lang.String r6 = " strema threshold="
                r0.append(r6)
                r0.append(r3)
                r0.append(r5)
                long[] r5 = r11.mMaxStreamTP
                int r6 = -r2
                r5 = r5[r6]
                r0.append(r5)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r1, r0)
                goto L_0x0102
            L_0x00f0:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                r0.append(r5)
                r0.append(r2)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r1, r0)
            L_0x0102:
                return r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.BssidStatistics.findAGGRssiTarget(int, int, int):int");
        }

        public void updateBssidQosMapOnScan() {
            int currFreqPrimary;
            int scanFreqPrimary;
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                int currChannelWidth = 0;
                int currFreqSecondary = 0;
                int apCountsOnChannel = 0;
                synchronized (WifiConnectivityMonitor.this.mScanResultsLock) {
                    if (this.mCurrentBssidScanInfo == null) {
                        Iterator it = WifiConnectivityMonitor.this.mScanResults.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            ScanResult scanResult = (ScanResult) it.next();
                            if (this.mBssid.equalsIgnoreCase(scanResult.BSSID)) {
                                this.mCurrentBssidScanInfo = scanResult;
                                break;
                            }
                        }
                    }
                    if (this.mCurrentBssidScanInfo == null) {
                        currFreqPrimary = WifiConnectivityMonitor.this.mWifiInfo.getFrequency();
                    } else {
                        currChannelWidth = this.mCurrentBssidScanInfo.channelWidth;
                        if (currChannelWidth == 0) {
                            currFreqPrimary = this.mCurrentBssidScanInfo.frequency;
                        } else {
                            currFreqPrimary = this.mCurrentBssidScanInfo.centerFreq0;
                            currFreqSecondary = this.mCurrentBssidScanInfo.centerFreq1;
                        }
                    }
                    for (ScanResult scanResult2 : WifiConnectivityMonitor.this.mScanResults) {
                        if (!this.mBssid.equalsIgnoreCase(scanResult2.BSSID)) {
                            int currBandwidth = WifiConnectivityMonitor.this.getBandwidth(currChannelWidth);
                            int scanBandwidth = WifiConnectivityMonitor.this.getBandwidth(scanResult2.channelWidth);
                            int scanFreqSecondary = 0;
                            if (scanResult2.channelWidth == 0) {
                                scanFreqPrimary = scanResult2.frequency;
                            } else {
                                scanFreqPrimary = scanResult2.centerFreq0;
                                scanFreqSecondary = scanResult2.centerFreq1;
                            }
                            if (currFreqPrimary - (currBandwidth / 2) < (scanBandwidth / 2) + scanFreqPrimary && (currBandwidth / 2) + currFreqPrimary > scanFreqPrimary - (scanBandwidth / 2)) {
                                apCountsOnChannel++;
                            } else if (currFreqSecondary != 0 && currFreqSecondary - (currBandwidth / 2) < (scanBandwidth / 2) + scanFreqPrimary && (currBandwidth / 2) + currFreqSecondary > scanFreqPrimary - (scanBandwidth / 2)) {
                                apCountsOnChannel++;
                            } else if (scanFreqSecondary != 0 && currFreqPrimary - (currBandwidth / 2) < (scanBandwidth / 2) + scanFreqSecondary && (currBandwidth / 2) + currFreqPrimary > scanFreqSecondary - (scanBandwidth / 2)) {
                                apCountsOnChannel++;
                            } else if (currFreqSecondary != 0 && scanFreqSecondary != 0 && currFreqSecondary - (currBandwidth / 2) < (scanBandwidth / 2) + scanFreqSecondary && (currBandwidth / 2) + currFreqSecondary > scanFreqSecondary - (scanBandwidth / 2)) {
                                apCountsOnChannel++;
                            }
                        }
                    }
                }
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).mApCountOnChannel = ((double) apCountsOnChannel) / ((double) WifiConnectivityMonitor.this.getBandwidthIn20MhzChannels(currChannelWidth));
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidQosMapOnQcResult(boolean pass) {
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                if (pass) {
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mQcPassCount++;
                    this.mLatestQcFailRssi = -99;
                } else {
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mQcFailCount++;
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "QC Failure occured at RSSI value " + rssi + " dBm.");
                    }
                    if (rssi > this.mLatestQcFailRssi) {
                        this.mLatestQcFailRssi = rssi;
                    }
                }
                updateBssidNoInternet();
                updateBssidPoorConnection();
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidQosMapOnLevel2State(boolean enter) {
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                if (enter) {
                    if (WifiConnectivityMonitor.DBG) {
                        Log.d(WifiConnectivityMonitor.TAG, "Level2State entered at RSSI value " + rssi + " dBm.");
                    }
                    this.mLatestLevel2Rssi = rssi;
                } else {
                    this.mLatestLevel2Rssi = -99;
                }
                updateBssidPoorConnection();
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidQosMapOnDnsResult(int result, int pingResponseTime) {
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                if (result == 0) {
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mCumulativeDnsResponseTime += (long) pingResponseTime;
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mDnsPassCount++;
                } else {
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mDnsFailCount++;
                }
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidQosMapOnTputUpdate(long pollInterval, long diffTxByte, long diffRxByte) {
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                if (pollInterval > 0 && pollInterval < RttServiceImpl.HAL_RANGING_TIMEOUT_MS) {
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mDwellTime += pollInterval;
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTotalTxBytes += diffTxByte;
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTotalRxBytes += diffRxByte;
                    long intervalThroughput = ((diffRxByte * 1000) * 8) / pollInterval;
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mCurrentThroughput = intervalThroughput;
                    if (intervalThroughput > 500000) {
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveTime += pollInterval;
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveTxBytes += diffTxByte;
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveRxBytes += diffRxByte;
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveThroughput = ((this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveRxBytes * 1000) * 8) / this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveTime;
                    }
                    this.mBssidQosMap.get(Integer.valueOf(levelValue)).mAverageThroughput = ((this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTotalRxBytes * 1000) * 8) / this.mBssidQosMap.get(Integer.valueOf(levelValue)).mDwellTime;
                    long targetMaxThroughput = this.mBssidQosMap.get(Integer.valueOf(levelValue)).mMaximumThroughput - ((200000 * pollInterval) / 60000);
                    if (targetMaxThroughput < intervalThroughput) {
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mMaximumThroughput = intervalThroughput;
                    } else if (targetMaxThroughput > this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveThroughput) {
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mMaximumThroughput = targetMaxThroughput;
                    } else {
                        this.mBssidQosMap.get(Integer.valueOf(levelValue)).mMaximumThroughput = this.mBssidQosMap.get(Integer.valueOf(levelValue)).mActiveThroughput;
                    }
                }
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidQosMapOnPerUpdate(int rssi, int diffTxBad, int difTxGood) {
            int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
            if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
            }
            this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTxBad += diffTxBad;
            this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTxGood += difTxGood;
            int totalTxBad = this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTxBad;
            int totalTxGood = this.mBssidQosMap.get(Integer.valueOf(levelValue)).mTxGood;
            if (totalTxBad + totalTxGood != 0) {
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).mPer = (((double) totalTxBad) / (((double) totalTxBad) + ((double) totalTxGood))) * 100.0d;
            }
            this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
        }

        public void updateBssidQosMapOnReachabilityLost() {
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                int rssi = WifiConnectivityMonitor.this.mWifiInfo.getRssi();
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(levelValue))) {
                    this.mBssidQosMap.put(Integer.valueOf(levelValue), new RssiLevelQosInfo(levelValue));
                }
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).mIpReachabilityLostCount++;
                this.mBssidQosMap.get(Integer.valueOf(levelValue)).updateQualityScore(rssi);
            }
        }

        public void updateBssidLatestDnsResultType(int result) {
            int res;
            if (WifiConnectivityMonitor.this.isConnectedState()) {
                Log.d(WifiConnectivityMonitor.TAG, "updateBssidLatestDnsResultType - result: " + result);
                if (result == 0 || result == 11) {
                    res = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_SUCCESS;
                } else if (result == 1 || result == 2 || result == 5) {
                    res = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_NO_INTERNET;
                } else {
                    res = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_POOR_CONNECTION;
                }
                if (res == WifiConnectivityMonitor.this.BSSID_DNS_RESULT_SUCCESS || res == WifiConnectivityMonitor.this.BSSID_DNS_RESULT_NO_INTERNET) {
                    this.mLatestDnsResult = res;
                } else if (this.mLatestDnsResult != WifiConnectivityMonitor.this.BSSID_DNS_RESULT_NO_INTERNET) {
                    this.mLatestDnsResult = res;
                }
                updateBssidNoInternet();
            }
        }

        public void updateBssidNoInternet() {
            boolean prev = this.mBssidNoInternet;
            this.mBssidNoInternet = WifiConnectivityMonitor.this.isInvalidState() && (this.mLatestDnsResult == WifiConnectivityMonitor.this.BSSID_DNS_RESULT_NO_INTERNET || WifiConnectivityMonitor.this.getLevelValue(this.mLatestQcFailRssi) == 3);
            if (prev != this.mBssidNoInternet) {
                Log.d(WifiConnectivityMonitor.TAG, "updateBssidNoInternet: mBssidNoInternet = " + this.mBssidNoInternet);
                WifiConnectivityMonitor.this.reportOpenNetworkQosNoInternetStatus();
            }
        }

        public void updateBssidPoorConnection() {
            int poorRssi = this.mLatestQcFailRssi;
            int i = this.mLatestLevel2Rssi;
            if (poorRssi <= i) {
                poorRssi = i;
            }
            int poorLevel = WifiConnectivityMonitor.this.getLevelValue(poorRssi);
            for (int level = 1; level <= 3; level++) {
                if (!this.mBssidQosMap.containsKey(Integer.valueOf(level))) {
                    this.mBssidQosMap.put(Integer.valueOf(level), new RssiLevelQosInfo(level));
                }
                if (level <= poorLevel) {
                    this.mBssidQosMap.get(Integer.valueOf(level)).mForcedSetScore = WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_SLOW];
                } else {
                    this.mBssidQosMap.get(Integer.valueOf(level)).mForcedSetScore = WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN];
                }
                this.mBssidQosMap.get(Integer.valueOf(level)).updateQualityScore();
            }
        }

        public void initOnConnect() {
            this.mNumberOfConnections++;
            this.mCurrentBssidScanInfo = null;
            this.mLatestDnsResult = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_UNKNOWN;
            this.mBssidNoInternet = false;
            WifiConfiguration config = WifiConnectivityMonitor.this.mWifiManager.getSpecificNetwork(this.netId);
            if (config != null) {
                this.mSsid = config.SSID;
                if (config.isCaptivePortal) {
                    this.mIsCaptivePortal = true;
                }
            }
        }

        public void clearBssidQosMap() {
            this.mBssidQosMap.clear();
            this.mNumberOfConnections = 0;
            this.mSsid = null;
            this.mCurrentBssidScanInfo = null;
            this.mIsCaptivePortal = false;
            this.mCurrentBssidScanInfo = null;
            this.mLatestDnsResult = WifiConnectivityMonitor.this.BSSID_DNS_RESULT_UNKNOWN;
        }
    }

    public int maxTputToIndex(long maxTput) {
        if (maxTput < 1000000) {
            return this.QUALITY_INDEX_SLOW;
        }
        if (maxTput < 5000000) {
            return this.QUALITY_INDEX_OKAY;
        }
        if (maxTput < 20000000) {
            return this.QUALITY_INDEX_FAST;
        }
        return this.QUALITY_INDEX_VERY_FAST;
    }

    public int activeTputToIndex(long activeTput) {
        if (activeTput < 1000000) {
            return this.QUALITY_INDEX_SLOW;
        }
        if (activeTput < 3000000) {
            return this.QUALITY_INDEX_OKAY;
        }
        if (activeTput < 10000000) {
            return this.QUALITY_INDEX_FAST;
        }
        return this.QUALITY_INDEX_VERY_FAST;
    }

    public int perToIndex(double per) {
        if (per < 2.0d) {
            return this.QUALITY_INDEX_VERY_FAST;
        }
        if (per < 5.0d) {
            return this.QUALITY_INDEX_FAST;
        }
        if (per < 12.0d) {
            return this.QUALITY_INDEX_OKAY;
        }
        return this.QUALITY_INDEX_SLOW;
    }

    public int calculateScore(int indexByMaxTput, int indexByActiveTput, int indexByPer) {
        int totalScore = 0;
        int totalWeight = 0;
        if (indexByMaxTput != this.QUALITY_INDEX_UNKNOWN) {
            totalScore = 0 + (this.INDEX_TO_SCORE[indexByMaxTput] * 33);
            totalWeight = 0 + 33;
        }
        if (indexByActiveTput != this.QUALITY_INDEX_UNKNOWN) {
            totalScore += this.INDEX_TO_SCORE[indexByActiveTput] * 34;
            totalWeight += 34;
        }
        if (indexByPer != this.QUALITY_INDEX_UNKNOWN) {
            totalScore += this.INDEX_TO_SCORE[indexByPer] * 33;
            totalWeight += 33;
        }
        if (totalWeight != 0) {
            return totalScore / totalWeight;
        }
        return 0;
    }

    private class RssiLevelQosInfo {
        public long mActiveRxBytes = 0;
        public long mActiveThroughput = 0;
        public long mActiveTime = 0;
        public long mActiveTxBytes = 0;
        public double mApCountOnChannel = -1.0d;
        public long mAverageThroughput = 0;
        public int mCalculatedScore;
        public long mCumulativeDnsResponseTime = 0;
        public long mCurrentThroughput = 0;
        public int mDnsFailCount = 0;
        public int mDnsPassCount = 0;
        public long mDwellTime = 0;
        public int mForcedSetScore;
        public int mIpReachabilityLostCount = 0;
        private long mLastActiveRxBytes = this.mActiveRxBytes;
        private long mLastActiveTime = this.mActiveTime;
        private long mLastCalculatedScore = ((long) this.mCalculatedScore);
        private int mLastTxBad = this.mTxBad;
        private int mLastTxGood = this.mTxGood;
        public String mLatestCloudScoreSummary = "";
        public int mLevelValue;
        public long mMaximumThroughput = 0;
        public double mPer = WifiConnectivityMonitor.POOR_LINK_MIN_VOLUME;
        public int mQcFailCount = 0;
        public int mQcPassCount = 0;
        public int mScore;
        public long mTotalRxBytes = 0;
        public long mTotalTxBytes = 0;
        public int mTxBad = 0;
        public int mTxGood = 0;

        public RssiLevelQosInfo(int levelValue) {
            this.mLevelValue = levelValue;
            this.mScore = WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN];
            this.mCalculatedScore = WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN];
            this.mForcedSetScore = WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN];
        }

        public int getQualityIndexByMaxTput() {
            if (this.mDwellTime < 60000 || this.mTotalRxBytes < 1000000) {
                return WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN;
            }
            return WifiConnectivityMonitor.this.maxTputToIndex(this.mMaximumThroughput);
        }

        public int getQualityIndexByActiveTput() {
            if (this.mActiveTime < 30000 || this.mTotalRxBytes < 1000000) {
                return WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN;
            }
            return WifiConnectivityMonitor.this.activeTputToIndex(this.mActiveThroughput);
        }

        public int getQualityIndexByPer() {
            if (this.mCalculatedScore == WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN]) {
                return WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN;
            }
            if (((long) (this.mTxBad + this.mTxGood)) < 1500) {
                return WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN;
            }
            return WifiConnectivityMonitor.this.perToIndex(this.mPer);
        }

        public int getQualityIndex() {
            return WifiConnectivityMonitor.this.getQualityIndexFromScore(this.mScore);
        }

        public void updateQualityScore() {
            updateQualityScore(-100);
        }

        public void updateQualityScore(int rssi) {
            int resultScore;
            int score = WifiConnectivityMonitor.this.calculateScore(getQualityIndexByMaxTput(), getQualityIndexByActiveTput(), getQualityIndexByPer());
            if (score != 0) {
                this.mCalculatedScore = score;
            }
            if (this.mForcedSetScore != WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN]) {
                resultScore = this.mForcedSetScore;
            } else {
                resultScore = this.mCalculatedScore;
            }
            if (this.mScore != resultScore) {
                this.mScore = resultScore;
                WifiConnectivityMonitor.this.reportOpenNetworkQosQualityScoreChange();
            }
            if (WifiConnectivityMonitor.DBG || WifiConnectivityMonitor.SMARTCM_DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateQualityScore - ");
                sb.append(WifiConnectivityMonitor.this.mCurrentBssid.mSsid);
                sb.append(" [");
                sb.append(WifiConnectivityMonitor.this.mCurrentBssid.mBssid);
                sb.append("], rssi: ");
                sb.append(rssi == -100 ? "N/A" : Integer.valueOf(rssi));
                sb.append(", #Conn: ");
                sb.append(WifiConnectivityMonitor.this.mCurrentBssid.mNumberOfConnections);
                sb.append(" - ");
                sb.append(toString());
                Log.d(WifiConnectivityMonitor.TAG, sb.toString());
            }
            if (WifiConnectivityMonitor.SMARTCM_DBG) {
                showToastBssidQosMapInfo(rssi);
            }
        }

        public Bundle getScoreForCloud() {
            long activeTimeDelta = this.mActiveTime - this.mLastActiveTime;
            Bundle bund = new Bundle();
            if (activeTimeDelta < 30000) {
                bund.putInt("score", WifiConnectivityMonitor.this.INDEX_TO_SCORE[WifiConnectivityMonitor.this.QUALITY_INDEX_UNKNOWN]);
                bund.putLong("weight", 0);
                return bund;
            }
            long activeTroughputDelta = (((this.mActiveRxBytes - this.mLastActiveRxBytes) * 1000) * 8) / activeTimeDelta;
            int txBadDelta = this.mTxBad - this.mLastTxBad;
            int txGoodDelta = this.mTxGood - this.mLastTxGood;
            Bundle bund2 = bund;
            String str = "weight";
            double perDelta = (((double) txBadDelta) / (((double) txBadDelta) + ((double) txGoodDelta))) * 100.0d;
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            int calculatedScoreFromDelta = wifiConnectivityMonitor.calculateScore(wifiConnectivityMonitor.maxTputToIndex(this.mMaximumThroughput), WifiConnectivityMonitor.this.activeTputToIndex(activeTroughputDelta), WifiConnectivityMonitor.this.perToIndex(perDelta));
            updateQualityScore();
            DecimalFormat df = new DecimalFormat("#.##");
            String currentTime = "" + (System.currentTimeMillis() / 1000);
            try {
                currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            } catch (RuntimeException e) {
            }
            StringBuilder sb = new StringBuilder();
            sb.append("getScoreForCloud[");
            sb.append(currentTime);
            sb.append("]: LastScore: ");
            String str2 = currentTime;
            sb.append(this.mLastCalculatedScore);
            sb.append(" @ ");
            String str3 = "score";
            sb.append(this.mLastActiveTime);
            sb.append(", CurrentScore: ");
            sb.append(this.mCalculatedScore);
            sb.append(" @ ");
            sb.append(this.mActiveTime);
            sb.append(", calculatedScoreFromDelta: ");
            sb.append(calculatedScoreFromDelta);
            sb.append(" @ ");
            sb.append(activeTimeDelta);
            sb.append(", Mx/dAc: ");
            sb.append(this.mMaximumThroughput / 1000);
            sb.append("/");
            long activeTimeDelta2 = activeTimeDelta;
            sb.append(activeTroughputDelta / 1000);
            sb.append(", dTB/dTG: ");
            sb.append(txBadDelta);
            sb.append("/");
            sb.append(txGoodDelta);
            sb.append(" [");
            sb.append(df.format(perDelta));
            sb.append("%]");
            this.mLatestCloudScoreSummary = sb.toString();
            if (WifiConnectivityMonitor.DBG) {
                Log.d(WifiConnectivityMonitor.TAG, this.mLatestCloudScoreSummary);
            }
            this.mLastCalculatedScore = (long) this.mCalculatedScore;
            this.mLastActiveRxBytes = this.mActiveRxBytes;
            this.mLastActiveTime = this.mActiveTime;
            this.mLastTxBad = this.mTxBad;
            this.mLastTxGood = this.mTxGood;
            Bundle bund3 = bund2;
            bund3.putInt(str3, calculatedScoreFromDelta);
            bund3.putLong(str, activeTimeDelta2);
            return bund3;
        }

        public String toString() {
            String str;
            DecimalFormat df = new DecimalFormat("#.##");
            StringBuilder sb = new StringBuilder();
            long j = 0;
            if (WifiConnectivityMonitor.SMARTCM_DBG) {
                sb.append("RSSI Level[" + this.mLevelValue + "] - ");
                sb.append("Quality: " + WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndex()] + "[" + this.mScore + "(" + this.mForcedSetScore + "/" + this.mCalculatedScore + ")-" + WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByMaxTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByActiveTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByPer()] + "] ");
                StringBuilder sb2 = new StringBuilder();
                sb2.append("DwellTime/ActiveTime: ");
                sb2.append(this.mDwellTime / 1000);
                sb2.append("/");
                sb2.append(this.mActiveTime / 1000);
                sb2.append(" sec, ");
                sb.append(sb2.toString());
                sb.append("Max/Ave/Active/Curr Tput " + (this.mMaximumThroughput / 1000) + "/" + (this.mAverageThroughput / 1000) + "/" + (this.mActiveThroughput / 1000) + "/" + (this.mCurrentThroughput / 1000) + " kbps, ");
                StringBuilder sb3 = new StringBuilder();
                sb3.append("TxBad/TxGood: ");
                sb3.append(this.mTxBad);
                sb3.append("/");
                sb3.append(this.mTxGood);
                sb3.append(" [");
                sb3.append(df.format(this.mPer));
                sb3.append("%], ");
                sb.append(sb3.toString());
                sb.append("TxBytes: " + Formatter.formatFileSize(WifiConnectivityMonitor.this.mContext, this.mTotalTxBytes) + ", ");
                sb.append("RxBytes: " + Formatter.formatFileSize(WifiConnectivityMonitor.this.mContext, this.mTotalRxBytes) + ", ");
                sb.append("QC P/F: " + this.mQcPassCount + "/" + this.mQcFailCount + ", ");
                sb.append("DNS P/F: " + this.mDnsPassCount + "/" + this.mQcFailCount + ", ");
                StringBuilder sb4 = new StringBuilder();
                sb4.append("DNS RTT: ");
                int i = this.mDnsPassCount;
                if (i != 0) {
                    j = this.mCumulativeDnsResponseTime / ((long) i);
                }
                sb4.append(j);
                sb4.append("msec, ");
                sb.append(sb4.toString());
                sb.append("ApCount: " + df.format(this.mApCountOnChannel) + ", ");
                if (this.mIpReachabilityLostCount == 0) {
                    str = "";
                } else {
                    str = "mIpReachabilityLostCount: " + this.mIpReachabilityLostCount;
                }
                sb.append(str);
            } else {
                sb.append("Lev[" + this.mLevelValue + "] - ");
                sb.append("Q: " + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[getQualityIndex()] + "[" + this.mScore + "(" + this.mForcedSetScore + "/" + this.mCalculatedScore + ")-" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[getQualityIndexByMaxTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[getQualityIndexByActiveTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[getQualityIndexByPer()] + "]");
                StringBuilder sb5 = new StringBuilder();
                sb5.append(", DT/AT: ");
                sb5.append(this.mDwellTime / 1000);
                sb5.append("/");
                sb5.append(this.mActiveTime / 1000);
                sb.append(sb5.toString());
                StringBuilder sb6 = new StringBuilder();
                sb6.append(", Mx/Av/Ac/Cr: ");
                sb6.append(this.mMaximumThroughput / 1000);
                sb6.append("/");
                sb6.append(this.mAverageThroughput / 1000);
                sb6.append("/");
                sb6.append(this.mActiveThroughput / 1000);
                sb6.append("/");
                sb6.append(this.mCurrentThroughput / 1000);
                sb.append(sb6.toString());
                sb.append(", TB/TG: " + this.mTxBad + "/" + this.mTxGood + " [" + df.format(this.mPer) + "%]");
                StringBuilder sb7 = new StringBuilder();
                sb7.append(", Tx/Rx: ");
                sb7.append(Formatter.formatFileSize(WifiConnectivityMonitor.this.mContext, this.mTotalTxBytes));
                sb7.append("/");
                sb7.append(Formatter.formatFileSize(WifiConnectivityMonitor.this.mContext, this.mTotalRxBytes));
                sb.append(sb7.toString());
                sb.append(", Q P/F: " + this.mQcPassCount + "/" + this.mQcFailCount);
                sb.append(", D P/F: " + this.mDnsPassCount + "/" + this.mQcFailCount);
                StringBuilder sb8 = new StringBuilder();
                sb8.append(", D RTT: ");
                int i2 = this.mDnsPassCount;
                if (i2 != 0) {
                    j = this.mCumulativeDnsResponseTime / ((long) i2);
                }
                sb8.append(j);
                sb.append(sb8.toString());
            }
            return sb.toString();
        }

        private void showToastBssidQosMapInfo(int rssi) {
            String str;
            WifiConnectivityMonitor.access$23408(WifiConnectivityMonitor.this);
            if (WifiConnectivityMonitor.this.toastCount % 30 == 0) {
                int levelValue = WifiConnectivityMonitor.this.getLevelValue(rssi);
                DecimalFormat df = new DecimalFormat("#.##");
                StringBuilder sb = new StringBuilder();
                sb.append("Level[");
                sb.append(rssi == -100 ? "N/A" : Integer.valueOf(levelValue));
                sb.append("]  ");
                if (rssi == -100) {
                    str = "N/A";
                } else {
                    str = rssi + " dBm ";
                }
                sb.append(str);
                sb.append(this.mCurrentThroughput / 1000);
                sb.append(" kbps\nMax Tput: ");
                sb.append(this.mMaximumThroughput / 1000000);
                sb.append(" Mbps - ");
                sb.append(WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByMaxTput()]);
                sb.append("\nActive Tput: ");
                sb.append(this.mActiveThroughput / 1000000);
                sb.append(" Mbps - ");
                sb.append(WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByActiveTput()]);
                sb.append("\nPER: ");
                sb.append(df.format(this.mPer));
                sb.append("% - ");
                sb.append(WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndexByPer()]);
                sb.append("\nRESULT: ");
                sb.append(WifiConnectivityMonitor.this.INDEX_TO_STRING[getQualityIndex()]);
                Toast.makeText(WifiConnectivityMonitor.this.mContext, sb.toString(), 1).show();
            } else if (WifiConnectivityMonitor.this.toastCount % 30 == 15) {
                StringBuilder sb2 = new StringBuilder();
                synchronized (WifiConnectivityMonitor.mCurrentBssidLock) {
                    sb2.append(WifiConnectivityMonitor.this.mCurrentBssid.mSsid + " [" + WifiConnectivityMonitor.this.mCurrentBssid.mBssid + "] - #Conn: " + WifiConnectivityMonitor.this.mCurrentBssid.mNumberOfConnections + ", isCaptivePortal: " + WifiConnectivityMonitor.this.mCurrentBssid.mIsCaptivePortal + ", Latest DNS Result: " + WifiConnectivityMonitor.this.mCurrentBssid.mLatestDnsResult + "\n");
                    for (RssiLevelQosInfo levelInfo : WifiConnectivityMonitor.this.mCurrentBssid.mBssidQosMap.values()) {
                        sb2.append("Level[" + levelInfo.mLevelValue + "]");
                        sb2.append(" - Quality: " + WifiConnectivityMonitor.this.INDEX_TO_STRING[levelInfo.getQualityIndex()] + "[" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[levelInfo.getQualityIndexByMaxTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[levelInfo.getQualityIndexByActiveTput()] + "/" + WifiConnectivityMonitor.this.INDEX_TO_STRING_SHORT[levelInfo.getQualityIndexByPer()] + "] ");
                        sb2.append("\n");
                    }
                }
                Toast.makeText(WifiConnectivityMonitor.this.mContext, sb2.toString(), 1).show();
            }
        }
    }

    /* access modifiers changed from: private */
    public int getLevelValue(int rssi) {
        if (rssi < -75) {
            return 0;
        }
        if (rssi < -65) {
            return 1;
        }
        if (rssi < -55) {
            return 2;
        }
        return 3;
    }

    /* access modifiers changed from: private */
    public int getQualityIndexFromScore(int score) {
        int i = this.QUALITY_INDEX_UNKNOWN;
        if (score == i) {
            return i;
        }
        int[] iArr = this.INDEX_TO_SCORE;
        int i2 = this.QUALITY_INDEX_SLOW;
        int i3 = iArr[i2];
        int i4 = this.QUALITY_INDEX_OKAY;
        if (score < (i3 + iArr[i4]) / 2) {
            return i2;
        }
        int i5 = iArr[i4];
        int i6 = this.QUALITY_INDEX_FAST;
        if (score < (i5 + iArr[i6]) / 2) {
            return i4;
        }
        int i7 = iArr[i6];
        int i8 = this.QUALITY_INDEX_VERY_FAST;
        if (score < (i7 + iArr[i8]) / 2) {
            return i6;
        }
        return i8;
    }

    /* access modifiers changed from: private */
    public int getBandwidth(int channelWidth) {
        if (channelWidth == 0) {
            return 20;
        }
        if (channelWidth == 1) {
            return 40;
        }
        if (channelWidth == 2) {
            return 80;
        }
        if (channelWidth == 3) {
            return SemWifiConstants.ROUTER_OUI_TYPE;
        }
        if (channelWidth == 4) {
            return 80;
        }
        return 20;
    }

    /* access modifiers changed from: private */
    public int getBandwidthIn20MhzChannels(int channelWidth) {
        if (channelWidth == 0) {
            return 1;
        }
        if (channelWidth == 1) {
            return 2;
        }
        if (channelWidth == 2) {
            return 4;
        }
        if (channelWidth == 3 || channelWidth == 4) {
            return 8;
        }
        return 1;
    }

    private void updateOpenNetworkQosScoreSummary() {
        String s = null;
        boolean noInternet = getOpenNetworkQosNoInternetStatus();
        int[] scores = getOpenNetworkQosScores();
        if (scores != null) {
            String s2 = noInternet ? " [ No Internet - " : " [ ";
            for (int i : scores) {
                s2 = s2 + this.INDEX_TO_STRING_SHORT[getQualityIndexFromScore(i)] + " ";
            }
            s = s2 + "] ";
        }
        if (DBG) {
            Log.d(TAG, "updateOpenNetworkQosScoreSummary: " + s);
        }
        Settings.Global.putString(this.mContentResolver, "wifi_wcm_qos_sharing_score_summary", s);
    }

    public boolean getOpenNetworkQosNoInternetStatus() {
        BssidStatistics bssidStatistics = this.mCurrentBssid;
        if (bssidStatistics == null || bssidStatistics == this.mEmptyBssid || bssidStatistics.netId == -1) {
            return false;
        }
        Log.d(TAG, "getOpenNetworkQosNoInternetStatus: " + this.mCurrentBssid.mBssidNoInternet);
        return this.mCurrentBssid.mBssidNoInternet;
    }

    public int[] getOpenNetworkQosScores() {
        BssidStatistics bssidStatistics = this.mCurrentBssid;
        if (bssidStatistics == null || bssidStatistics == this.mEmptyBssid || bssidStatistics.netId == -1) {
            return null;
        }
        int[] retScores = new int[3];
        if (this.mCurrentBssid.mBssidQosMap.containsKey(3)) {
            retScores[0] = this.mCurrentBssid.mBssidQosMap.get(3).mScore;
        } else {
            retScores[0] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        if (this.mCurrentBssid.mBssidQosMap.containsKey(2)) {
            retScores[1] = this.mCurrentBssid.mBssidQosMap.get(2).mScore;
        } else {
            retScores[1] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        if (this.mCurrentBssid.mBssidQosMap.containsKey(1)) {
            retScores[2] = this.mCurrentBssid.mBssidQosMap.get(1).mScore;
        } else {
            retScores[2] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        String s = "";
        for (int i : retScores) {
            s = s + i + " ";
        }
        Log.d(TAG, "getOpenNetworkQosScores: " + s);
        return retScores;
    }

    public int[] getOpenNetworkQosScores(String bssid) {
        BssidStatistics bssidStat;
        if (bssid == null) {
            return getOpenNetworkQosScores();
        }
        if (!this.mBssidCache.snapshot().containsKey(bssid) || (bssidStat = this.mBssidCache.get(bssid)) == null || bssidStat.netId == -1) {
            return null;
        }
        int[] retScores = new int[3];
        if (bssidStat.mBssidQosMap.containsKey(3)) {
            retScores[0] = bssidStat.mBssidQosMap.get(3).mScore;
        } else {
            retScores[0] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        if (bssidStat.mBssidQosMap.containsKey(2)) {
            retScores[1] = bssidStat.mBssidQosMap.get(2).mScore;
        } else {
            retScores[1] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        if (bssidStat.mBssidQosMap.containsKey(1)) {
            retScores[2] = bssidStat.mBssidQosMap.get(1).mScore;
        } else {
            retScores[2] = this.INDEX_TO_SCORE[this.QUALITY_INDEX_UNKNOWN];
        }
        String s = "";
        for (int i : retScores) {
            s = s + i + " ";
        }
        Log.d(TAG, "getOpenNetworkQosScores[" + bssid + "]: " + s);
        return retScores;
    }

    public static class OpenNetworkQosCallback {
        /* access modifiers changed from: package-private */
        public void onNoInternetStatusChange(boolean valid) {
        }

        /* access modifiers changed from: package-private */
        public void onQualityScoreChanged() {
        }
    }

    public void registerOpenNetworkQosCallback(OpenNetworkQosCallback callback) {
        if (this.mOpenNetworkQosCallbackList == null) {
            this.mOpenNetworkQosCallbackList = new ArrayList<>();
        }
        this.mOpenNetworkQosCallbackList.add(callback);
    }

    /* access modifiers changed from: private */
    public void reportOpenNetworkQosNoInternetStatus() {
        if (this.mCurrentBssid != null) {
            if (DBG) {
                Log.d(TAG, "reportOpenNetworkQosNoInternetStatus");
            }
            updateOpenNetworkQosScoreSummary();
            ArrayList<OpenNetworkQosCallback> arrayList = this.mOpenNetworkQosCallbackList;
            if (arrayList != null) {
                Iterator<OpenNetworkQosCallback> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().onNoInternetStatusChange(this.mCurrentBssid.mBssidNoInternet);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void reportOpenNetworkQosQualityScoreChange() {
        if (this.mCurrentBssid != null) {
            if (DBG) {
                Log.d(TAG, "reportOpenNetworkQosQualityScoreChange");
            }
            updateOpenNetworkQosScoreSummary();
            ArrayList<OpenNetworkQosCallback> arrayList = this.mOpenNetworkQosCallbackList;
            if (arrayList != null) {
                Iterator<OpenNetworkQosCallback> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().onQualityScoreChanged();
                }
            }
        }
    }

    private String dumpBssidQosMap() {
        StringBuilder sb = new StringBuilder();
        synchronized (mCurrentBssidLock) {
            for (BssidStatistics bssidStat : this.mBssidCache.snapshot().values()) {
                sb.append(bssidStat.mSsid + " [" + bssidStat.mBssid + "] - #Conn: " + bssidStat.mNumberOfConnections + ", CP: " + bssidStat.mIsCaptivePortal + ", L_Dns: " + bssidStat.mLatestDnsResult + ", L_F_R: " + bssidStat.mLatestQcFailRssi + ", L_2_R: " + bssidStat.mLatestLevel2Rssi + "\n");
                for (RssiLevelQosInfo levelInfo : bssidStat.mBssidQosMap.values()) {
                    sb.append("    ");
                    sb.append(levelInfo.toString());
                    sb.append("\n");
                    if (!levelInfo.mLatestCloudScoreSummary.isEmpty()) {
                        sb.append("        ");
                        sb.append(levelInfo.mLatestCloudScoreSummary);
                        sb.append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private void clearAllBssidQosMaps() {
        synchronized (mCurrentBssidLock) {
            for (BssidStatistics bssidStat : this.mBssidCache.snapshot().values()) {
                bssidStat.clearBssidQosMap();
            }
        }
    }

    /* access modifiers changed from: private */
    public void scanStarted() {
        if (!this.mIsScanning) {
            this.mIsScanning = true;
            removeMessages(EVENT_SCAN_TIMEOUT);
            sendMessage(EVENT_SCAN_STARTED);
            sendMessageDelayed(EVENT_SCAN_TIMEOUT, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
            this.mNetworkStatsAnalyzer.sendEmptyMessage(EVENT_SCAN_STARTED);
            this.mNetworkStatsAnalyzer.sendEmptyMessageDelayed(EVENT_SCAN_TIMEOUT, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        } else if (DBG) {
            Log.d(TAG, "startScan but already in scanning state");
        }
    }

    /* access modifiers changed from: private */
    public void scanCompleted(boolean partialScan) {
        int i;
        BssidStatistics bssidStatistics;
        this.mIsScanning = false;
        checkDisabledNetworks();
        if (!partialScan) {
            synchronized (this.mScanResultsLock) {
                List<ScanResult> scanResults = this.mWifiManager.getScanResults();
                if (scanResults.isEmpty()) {
                    this.mScanResults = new ArrayList();
                } else {
                    this.mScanResults = scanResults;
                }
            }
            checkCountryCodeFromScanResults();
            if (isConnectedState() && (bssidStatistics = this.mCurrentBssid) != null) {
                bssidStatistics.updateBssidQosMapOnScan();
            }
        }
        if (mUserSelectionConfirmed && !this.mConnectivityManager.getMultiNetwork()) {
            if (getCurrentState() == this.mLevel2State) {
                if (DBG) {
                    Log.d(TAG, "CHECK_ALTERNATIVE_NETWORKS - mLevel2State");
                }
                this.mClientModeImpl.checkAlternativeNetworksForWmc(false);
            } else if (getCurrentState() == this.mInvalidState && ((i = this.mCurrentMode) == 2 || i == 3)) {
                if (!this.mNetworkCallbackController.isCaptivePortal()) {
                    if (DBG) {
                        Log.d(TAG, "CHECK_ALTERNATIVE_NETWORKS - mInvalidState && SNS ON");
                    }
                    this.mClientModeImpl.checkAlternativeNetworksForWmc(false);
                    return;
                }
                removeMessages(CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL);
                sendMessageDelayed(CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL, 3000);
            } else if (!this.mLevel2StateTransitionPending) {
            } else {
                if (getCurrentState() == this.mLevel1State) {
                    if (DBG) {
                        Log.d(TAG, "CHECK_ALTERNATIVE_NETWORKS - mLevel1State - Need to roam in Level1State");
                    }
                    this.mClientModeImpl.checkAlternativeNetworksForWmc(true);
                    return;
                }
                this.mLevel2StateTransitionPending = false;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void checkCountryCodeFromScanResults() {
        int scanCount = 0;
        int bssidWithCountryInfo = 0;
        HashMap hashMap = new HashMap();
        StringBuilder sb = new StringBuilder();
        synchronized (this.mScanResultsLock) {
            for (ScanResult scanResult : this.mScanResults) {
                scanCount++;
                ScanResult.InformationElement[] informationElementArr = scanResult.informationElements;
                int length = informationElementArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    ScanResult.InformationElement ie = informationElementArr[i];
                    if (ie.id == 7) {
                        bssidWithCountryInfo++;
                        if (ie.bytes.length >= 2) {
                            String country = new String(ie.bytes, 0, 2, StandardCharsets.UTF_8).toUpperCase();
                            if ("HK".equals(country) || "MO".equals(country)) {
                                country = "CN";
                            }
                            if (hashMap.containsKey(country)) {
                                hashMap.put(country, Integer.valueOf(((Integer) hashMap.get(country)).intValue() + 1));
                            } else {
                                hashMap.put(country, 1);
                            }
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        if (bssidWithCountryInfo != 0) {
            int winnerCount = 0;
            if (hashMap.containsKey("CN")) {
                int intValue = ((Integer) hashMap.get("CN")).intValue();
            }
            String pollWinner = "";
            String stat = "";
            for (String c : hashMap.keySet()) {
                stat = (stat + c + ": " + hashMap.get(c)) + "  ";
                if (((Integer) hashMap.get(c)).intValue() > winnerCount || (((Integer) hashMap.get(c)).intValue() >= winnerCount && "CN".equals(c))) {
                    pollWinner = c;
                    winnerCount = ((Integer) hashMap.get(c)).intValue();
                }
            }
            String currentTime = "" + (System.currentTimeMillis() / 1000);
            try {
                currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            } catch (RuntimeException e) {
            }
            sb.append(currentTime + "  |  ");
            sb.append("available data: " + bssidWithCountryInfo + "/" + scanCount + "  |  ");
            sb.append("Win: " + pollWinner + " - " + winnerCount + "[" + ((winnerCount * 100) / bssidWithCountryInfo) + "%]  |  ");
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Stat - ");
            sb2.append(stat);
            sb2.append("  |  ");
            sb.append(sb2.toString());
            if (winnerCount >= 5) {
                if ("CN".equals(pollWinner)) {
                    this.mLastChinaConfirmedTime = System.currentTimeMillis();
                }
                if ("CN".equals(this.mCountryCodeFromScanResult) && !"CN".equals(pollWinner)) {
                    long remainingTime = 86400000 - (System.currentTimeMillis() - this.mLastChinaConfirmedTime);
                    if (remainingTime < 0) {
                        sb.append("  |  CISO Updated [24h expired] - CN -> " + pollWinner);
                        this.mCountryCodeFromScanResult = pollWinner;
                        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_wcm_country_code_from_scan_result", this.mCountryCodeFromScanResult);
                    } else {
                        sb.append("  |  CISO changed but not updated - CN -X-> " + pollWinner + " , maintain CN for next " + (remainingTime / 1000) + " seconds");
                    }
                } else if (!pollWinner.equals(this.mCountryCodeFromScanResult)) {
                    sb.append("  |  Updated - " + this.mCountryCodeFromScanResult + "->" + pollWinner);
                    this.mCountryCodeFromScanResult = pollWinner;
                    Settings.Global.putString(this.mContext.getContentResolver(), "wifi_wcm_country_code_from_scan_result", this.mCountryCodeFromScanResult);
                }
            }
            String[] strArr = this.summaryCountryCodeFromScanResults;
            int i2 = this.incrScanResult;
            this.incrScanResult = i2 + 1;
            strArr[i2 % 10] = sb.toString();
            if (DBG) {
                Log.d(TAG, sb.toString());
            }
        }
    }

    private void checkDisabledNetworks() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                WifiConfiguration.NetworkSelectionStatus netStatus = config.getNetworkSelectionStatus();
                int disableReason = netStatus.getNetworkSelectionDisableReason();
                if (!netStatus.isNetworkEnabled() && ((disableReason == 6 || disableReason == 16 || disableReason == 17) && this.mWifiConfigManager.tryEnableNetwork(config.networkId))) {
                    Log.d(TAG, config.SSID + " network enabled.");
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNeedToRoamInLevel1State() {
        if (!this.mLevel2StateTransitionPending) {
            this.mLevel2StateTransitionPending = true;
            if (DBG) {
                Log.d(TAG, "Start scan to find possible roaming networks. " + getCurrentState() + getCurrentMode());
            }
            this.mClientModeImpl.startScanFromWcm();
        }
    }

    /* access modifiers changed from: private */
    public boolean isMobileHotspot() {
        WifiInfo wifiInfo;
        if (!"vzw".equalsIgnoreCase(SemCscFeature.getInstance().getString("SalesCode")) && !mCurrentApDefault && (wifiInfo = syncGetCurrentWifiInfo()) != null) {
            int networkId = wifiInfo.getNetworkId();
            if (networkId == this.mLastCheckedMobileHotspotNetworkId) {
                return this.mLastCheckedMobileHotspotValue;
            }
            this.mLastCheckedMobileHotspotNetworkId = networkId;
            this.mLastCheckedMobileHotspotValue = false;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isConnectedState() {
        if (getCurrentState() == this.mNotConnectedState) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean inChinaNetwork() {
        String str = this.mCountryIso;
        if (str == null || str.length() != 2) {
            updateCountryIsoCode();
        }
        if (!isChineseIso(this.mCountryIso)) {
            return false;
        }
        if (!DBG) {
            return true;
        }
        Log.d(TAG, "Need to skip captive portal check. CISO: " + this.mCountryIso);
        return true;
    }

    private boolean isChineseIso(String countryIso) {
        return "cn".equalsIgnoreCase(countryIso) || "hk".equalsIgnoreCase(countryIso) || "mo".equalsIgnoreCase(countryIso);
    }

    /* access modifiers changed from: private */
    public void updateCountryIsoCode() {
        if (this.mTelephonyManager == null) {
            try {
                this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            } catch (Exception e) {
                Log.e(TAG, "Exception occured at updateCountryIsoCode(), while retrieving Context.TELEPHONY_SERVICE");
            }
        }
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager != null) {
            this.mCountryIso = telephonyManager.getNetworkCountryIso();
            Log.i(TAG, "updateCountryIsoCode() via TelephonyManager : mCountryIso: " + this.mCountryIso);
        }
        String str = this.mCountryIso;
        if (str == null || str.length() != 2) {
            try {
                String countryCode = SemCscFeature.getInstance().getString("CountryISO").toLowerCase();
                if (countryCode == null || countryCode.length() != 2) {
                    this.mCountryIso = " ";
                } else {
                    this.mCountryIso = countryCode;
                }
            } catch (Exception e2) {
            }
            if (DBG) {
                Log.d(TAG, "updateCountryIsoCode() via Property(CSC) : mCountryIso: " + this.mCountryIso);
            }
        } else {
            if (Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_sns_visited_country_iso") == null) {
                Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_sns_visited_country_iso", this.mCountryIso);
                Log.e(TAG, "WIFI_SNS_VISITED_COUNTRY_ISO is null, putString:" + this.mCountryIso);
            }
            if ("LGU".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING)) && !Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_sns_visited_country_iso").equals(this.mCountryIso)) {
                Log.e(TAG, "WIFI_SNS_VISITED_COUNTRY_ISO need to be updated from/to : " + Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_sns_visited_country_iso") + "/" + this.mCountryIso + " Initialize WIFI_POOR_CONNECTION_WARNING to 0");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_poor_connection_warning", 0);
                Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_sns_visited_country_iso", this.mCountryIso);
            }
        }
        try {
            String deviceCountryCode = SemCscFeature.getInstance().getString("CountryISO");
            if (deviceCountryCode != null) {
                this.mDeviceCountryCode = deviceCountryCode;
            } else {
                this.mDeviceCountryCode = " ";
            }
        } catch (Exception e3) {
        }
    }

    private static void checkVersion(Context context) {
        try {
            int i = 0;
            int mWatchdogVersionFromSettings = Settings.Global.getInt(context.getContentResolver(), "wifi_watchdog_version", 0);
            int storedOSver = (-65536 & mWatchdogVersionFromSettings) >>> 16;
            int updatingOSver = 0;
            String propertyOsVersion = SystemProperties.get("ro.build.version.release");
            for (int i2 = 0; i2 < propertyOsVersion.length(); i2++) {
                if (Character.isDigit(propertyOsVersion.charAt(i2))) {
                    updatingOSver = (updatingOSver << 4) + Character.getNumericValue(propertyOsVersion.charAt(i2));
                }
            }
            if (updatingOSver == 0) {
                Log.e(TAG, "Cannot retrieve version info from SystemProperties.");
                return;
            }
            if (DBG) {
                Log.d(TAG, "checkVersion - Current version: 0x" + Integer.toHexString(mWatchdogVersionFromSettings) + ", New version: 0x" + Integer.toHexString((updatingOSver << 16) + 3));
            }
            boolean backupAgg = Settings.Global.getInt(context.getContentResolver(), "wifi_watchdog_poor_network_aggressive_mode_on", 1) != 0;
            ContentResolver contentResolver = context.getContentResolver();
            if (backupAgg) {
                i = 1;
            }
            Settings.Global.putInt(contentResolver, "wifi_iwc_backup_aggressive_mode_on", i);
            Settings.Global.putInt(context.getContentResolver(), "wifi_watchdog_poor_network_aggressive_mode_on", 1);
            if (storedOSver != 0) {
                if (storedOSver != 1058) {
                    if (storedOSver != 67) {
                        if (storedOSver != 68) {
                        }
                    }
                }
            }
            if (mWatchdogVersionFromSettings != (updatingOSver << 16) + 3) {
                Log.d(TAG, "Version chaged. Updating the version...");
                Settings.Global.putInt(context.getContentResolver(), "wifi_watchdog_version", (updatingOSver << 16) + 3);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkVersion - failed.");
        }
    }

    /* access modifiers changed from: private */
    public void updateTargetRssiForCurrentAP(boolean resetAggressiveMode) {
        ParameterManager parameterManager = this.mParam;
        parameterManager.mRssiThresholdAggModeCurrentAP = parameterManager.mRssiThresholdAggMode2G + 3;
        WifiInfo info = syncGetCurrentWifiInfo();
        WifiConfiguration config = getWifiConfiguration(info.getNetworkId());
        if (config == null) {
            Log.e(TAG, "updateTargetRssiForCurrentAP - config == null");
            return;
        }
        int targetRssi = config.nextTargetRssi;
        boolean is5G = info.getFrequency() > 4000;
        ParameterManager parameterManager2 = this.mParam;
        int defaultThreshold = (is5G ? parameterManager2.mRssiThresholdAggMode5G : parameterManager2.mRssiThresholdAggMode2G) + 3;
        if (targetRssi < defaultThreshold) {
            this.mParam.mRssiThresholdAggModeCurrentAP = targetRssi;
        } else {
            this.mParam.mRssiThresholdAggModeCurrentAP = defaultThreshold;
        }
        if (resetAggressiveMode) {
            this.mNetworkStatsAnalyzer.sendEmptyMessage(ACTIVITY_RESTART_AGGRESSIVE_MODE);
        }
        if (DBG) {
            Log.i(TAG, "updateTargetRssiForCurrentAP - SSID: " + config.SSID + ", frequency: " + info.getFrequency() + ", is5G: " + is5G + ", mParam.mRssiThreshold@CurrentAP: " + this.mParam.mRssiThresholdAggModeCurrentAP);
        }
    }

    /* access modifiers changed from: private */
    public void sendBroadcastWCMTestResult(boolean valid) {
        Intent intent = new Intent("com.sec.android.WIFI_CONNECTIVITY_ACTION");
        intent.putExtra("valid", valid);
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Send broadcast WCM initial test result - action:" + intent.getAction());
        }
    }

    private void sendBroadCastWCMHideIcon(int visible) {
        if (DBG) {
            Log.d(TAG, "WCM vsb : " + visible);
        }
        sendMessage(EVENT_WIFI_ICON_VSB, visible);
        Intent intent = new Intent("com.sec.android.WIFI_ICON_HIDE_ACTION");
        intent.putExtra("visible", visible);
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Send broadcast WCM Hide wifi icon result - action:" + intent.getAction());
        }
        setWifiHideIconHistory(visible);
    }

    /* access modifiers changed from: private */
    public void sendBroadcastWCMStatusChanged() {
        Intent intent = new Intent("com.sec.android.WIFI_WCM_STATE_CHANGED_ACTION");
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Send broadcast WCM status changed - action:" + intent.getAction());
        }
    }

    /* access modifiers changed from: package-private */
    public void changeWifiIcon(int visible) {
        if (visible <= 1 && visible >= 0 && visible != this.mLastVisibilityOfWifiIcon) {
            if (visible != 0 || (!this.mConnectivityManager.getMultiNetwork() && isMobileDataConnected())) {
                sendBroadCastWCMHideIcon(visible);
                this.mLastVisibilityOfWifiIcon = visible;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setForceWifiIcon(int visible) {
        sendBroadCastWCMHideIcon(visible);
        this.mLastVisibilityOfWifiIcon = visible;
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

    public int getCurrentMode() {
        return this.mCurrentMode;
    }

    public int getCurrentStatusMode() {
        if (getCurrentState() == this.mLevel2State) {
            return 3;
        }
        if (getCurrentState() != this.mInvalidState) {
            return 0;
        }
        if (this.mPoorNetworkDetectionEnabled) {
            return 2;
        }
        return 1;
    }

    public boolean getValidState() {
        if (!mInitialResultSentToSystemUi || !isValidState() || getCurrentState() == this.mLevel2State) {
            return false;
        }
        return true;
    }

    public static int getEverQualityTested() {
        if (!mInitialResultSentToSystemUi || !mUserSelectionConfirmed) {
            return 0;
        }
        return 1;
    }

    public void setWifiEnabled(boolean enable, String bssid) {
        sendMessage(EVENT_WIFI_TOGGLED, enable, 0, bssid);
    }

    public void networkRemoved(int netId) {
        sendMessage(EVENT_NETWORK_REMOVED, netId, 0, (Object) null);
    }

    public boolean reportNetworkConnectivityToNM(int step, int trigger) {
        return reportNetworkConnectivityToNM(false, step, trigger);
    }

    public boolean reportNetworkConnectivityToNM(boolean force, int step, int trigger) {
        if ((!force && trigger != 52 && ((!this.mIsScreenOn && mInitialResultSentToSystemUi) || isMobileHotspot())) || this.mIsInRoamSession || this.mIsInDhcpSession) {
            return false;
        }
        if (this.mNetwork != null) {
            if (this.mWCMQCResult != null) {
                Log.d(TAG, "QC is already queried to NM");
                return true;
            }
            Log.d(TAG, "QC is queried to NM. Waiting for result");
            removeMessages(QC_RESULT_NOT_RECEIVED);
            this.mWCMQCResult = obtainMessage();
            getCm().reportNetworkConnectivityForResult(this.mNetwork, this.mWCMQCResult);
            QcFailHistory qcFailHistory = this.mInvalidationFailHistory;
            qcFailHistory.qcStep = step;
            qcFailHistory.qcTrigger = trigger;
            qcFailHistory.qcStepTemp = -1;
            sendMessageDelayed(QC_RESULT_NOT_RECEIVED, 20000);
            if (isValidState()) {
                setLoggingForTCPStat(TCP_STAT_LOGGING_FIRST);
            }
        }
        return true;
    }

    private class NetworkCallbackController {
        private static final String TAG = "WifiConnectivityMonitor.NetworkCallbackController";
        private ConnectivityManager.NetworkCallback mCaptivePortalCallback;
        private ConnectivityManager.NetworkCallback mConnectionDetector;
        private ConnectivityManager.NetworkCallback mDefaultNetworkCallback;
        public boolean mDisableRequired = false;
        /* access modifiers changed from: private */
        public boolean mHasCaptivePortalCapa = false;
        /* access modifiers changed from: private */
        public boolean mHasValidatedCapa = false;
        /* access modifiers changed from: private */
        public boolean mIsScoreChangedForCaptivePortal = false;
        public int mNetId = -1;
        private ConnectivityManager.NetworkCallback mNetworkCallback;
        /* access modifiers changed from: private */
        public ConnectivityManager.NetworkCallback mNetworkCallbackDummy;

        public NetworkCallbackController() {
            init();
            this.mDisableRequired = false;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:15:0x0056, code lost:
            if (r4.this$0.mWCMQCResult != null) goto L_0x0077;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:21:0x0075, code lost:
            if (r4.this$0.mWCMQCResult == null) goto L_0x007e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:22:0x0077, code lost:
            r4.this$0.mWCMQCResult.recycle();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:23:0x007e, code lost:
            r4.this$0.mWCMQCResult = null;
            r4.mHasCaptivePortalCapa = false;
            r4.mIsScoreChangedForCaptivePortal = false;
            r4.mHasValidatedCapa = false;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:24:0x008c, code lost:
            if (r4.mConnectionDetector != null) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x008e, code lost:
            registerConnectionDetector();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:?, code lost:
            return;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void init() {
            /*
                r4 = this;
                r0 = 135480(0x21138, float:1.89848E-40)
                r1 = 0
                android.net.ConnectivityManager$NetworkCallback r2 = r4.mNetworkCallback     // Catch:{ Exception -> 0x005b }
                if (r2 == 0) goto L_0x0013
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager r2 = r2.getCm()     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager$NetworkCallback r3 = r4.mNetworkCallback     // Catch:{ Exception -> 0x005b }
                r2.unregisterNetworkCallback(r3)     // Catch:{ Exception -> 0x005b }
            L_0x0013:
                android.net.ConnectivityManager$NetworkCallback r2 = r4.mNetworkCallbackDummy     // Catch:{ Exception -> 0x005b }
                if (r2 == 0) goto L_0x0022
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager r2 = r2.getCm()     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager$NetworkCallback r3 = r4.mNetworkCallbackDummy     // Catch:{ Exception -> 0x005b }
                r2.unregisterNetworkCallback(r3)     // Catch:{ Exception -> 0x005b }
            L_0x0022:
                android.net.ConnectivityManager$NetworkCallback r2 = r4.mCaptivePortalCallback     // Catch:{ Exception -> 0x005b }
                if (r2 == 0) goto L_0x0031
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager r2 = r2.getCm()     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager$NetworkCallback r3 = r4.mCaptivePortalCallback     // Catch:{ Exception -> 0x005b }
                r2.unregisterNetworkCallback(r3)     // Catch:{ Exception -> 0x005b }
            L_0x0031:
                android.net.ConnectivityManager$NetworkCallback r2 = r4.mDefaultNetworkCallback     // Catch:{ Exception -> 0x005b }
                if (r2 == 0) goto L_0x0040
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager r2 = r2.getCm()     // Catch:{ Exception -> 0x005b }
                android.net.ConnectivityManager$NetworkCallback r3 = r4.mDefaultNetworkCallback     // Catch:{ Exception -> 0x005b }
                r2.unregisterNetworkCallback(r3)     // Catch:{ Exception -> 0x005b }
            L_0x0040:
                r4.mNetworkCallback = r1
                r4.mNetworkCallbackDummy = r1
                r4.mCaptivePortalCallback = r1
                r4.mDefaultNetworkCallback = r1
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.Network unused = r2.mNetwork = r1
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.removeMessages(r0)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                if (r0 == 0) goto L_0x007e
                goto L_0x0077
            L_0x0059:
                r2 = move-exception
                goto L_0x0092
            L_0x005b:
                r2 = move-exception
                r2.printStackTrace()     // Catch:{ all -> 0x0059 }
                r4.mNetworkCallback = r1
                r4.mNetworkCallbackDummy = r1
                r4.mCaptivePortalCallback = r1
                r4.mDefaultNetworkCallback = r1
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.Network unused = r2.mNetwork = r1
                com.android.server.wifi.WifiConnectivityMonitor r2 = com.android.server.wifi.WifiConnectivityMonitor.this
                r2.removeMessages(r0)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                if (r0 == 0) goto L_0x007e
            L_0x0077:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                r0.recycle()
            L_0x007e:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.mWCMQCResult = r1
                r0 = 0
                r4.mHasCaptivePortalCapa = r0
                r4.mIsScoreChangedForCaptivePortal = r0
                r4.mHasValidatedCapa = r0
                android.net.ConnectivityManager$NetworkCallback r0 = r4.mConnectionDetector
                if (r0 != 0) goto L_0x0091
                r4.registerConnectionDetector()
            L_0x0091:
                return
            L_0x0092:
                r4.mNetworkCallback = r1
                r4.mNetworkCallbackDummy = r1
                r4.mCaptivePortalCallback = r1
                r4.mDefaultNetworkCallback = r1
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.net.Network unused = r3.mNetwork = r1
                com.android.server.wifi.WifiConnectivityMonitor r3 = com.android.server.wifi.WifiConnectivityMonitor.this
                r3.removeMessages(r0)
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                if (r0 == 0) goto L_0x00b1
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                android.os.Message r0 = r0.mWCMQCResult
                r0.recycle()
            L_0x00b1:
                com.android.server.wifi.WifiConnectivityMonitor r0 = com.android.server.wifi.WifiConnectivityMonitor.this
                r0.mWCMQCResult = r1
                throw r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.NetworkCallbackController.init():void");
        }

        public void handleConnected() {
            registerNetworkCallbacks();
        }

        public boolean isCaptivePortal() {
            boolean z = this.mHasCaptivePortalCapa;
            if (z) {
                return z;
            }
            WifiConnectivityMonitor wifiConnectivityMonitor = WifiConnectivityMonitor.this;
            WifiInfo unused = wifiConnectivityMonitor.mWifiInfo = wifiConnectivityMonitor.syncGetCurrentWifiInfo();
            int networkId = -1;
            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                networkId = WifiConnectivityMonitor.this.mWifiInfo.getNetworkId();
            }
            WifiConfiguration wifiConfiguration = null;
            if (networkId != -1) {
                wifiConfiguration = WifiConnectivityMonitor.this.getWifiConfiguration(networkId);
            }
            return wifiConfiguration != null && wifiConfiguration.isCaptivePortal;
        }

        private void registerConnectionDetector() {
            NetworkRequest.Builder req = new NetworkRequest.Builder();
            req.addTransportType(1);
            req.removeCapability(6);
            this.mConnectionDetector = new ConnectivityManager.NetworkCallback() {
                public void onAvailable(Network network) {
                    Log.i(NetworkCallbackController.TAG, "mConnectionDetector: " + network.toString());
                    NetworkCapabilities nc = WifiConnectivityMonitor.this.getCm().getNetworkCapabilities(network);
                    if (nc == null) {
                        Log.e(NetworkCallbackController.TAG, "mConnectionDetector ignore this network. NetworkCapabilities instance is null");
                    } else if (nc.hasCapability(6)) {
                        Log.i(NetworkCallbackController.TAG, "mConnectionDetector ignore this network. It is Wifi_p2p");
                    } else {
                        if (WifiConnectivityMonitor.this.isConnectedState() && WifiConnectivityMonitor.this.mNetwork != network) {
                            try {
                                Log.d(NetworkCallbackController.TAG, "onAvailable called on different network instance");
                                if (WifiConnectivityMonitor.this.mNetwork != null) {
                                    Log.d(NetworkCallbackController.TAG, "OLD NETWORK : " + WifiConnectivityMonitor.this.mNetwork);
                                }
                                Log.d(NetworkCallbackController.TAG, "NEW NETWORK : " + network);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (!WifiConnectivityMonitor.this.isConnectedState()) {
                            Network unused = WifiConnectivityMonitor.this.mNetwork = network;
                            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.HANDLE_ON_AVAILABLE);
                        }
                    }
                }

                public void onLost(Network network) {
                    Log.i(NetworkCallbackController.TAG, "mConnectionDetector onLost : " + network.toString());
                    if (WifiConnectivityMonitor.this.mNetwork == network) {
                        WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.HANDLE_ON_LOST);
                        NetworkCallbackController.this.init();
                        return;
                    }
                    try {
                        Log.d(NetworkCallbackController.TAG, "onLost called on different network instance (ignore)");
                        if (WifiConnectivityMonitor.this.mNetwork != null) {
                            Log.d(NetworkCallbackController.TAG, "OLD NETWORK : " + WifiConnectivityMonitor.this.mNetwork);
                        }
                        Log.d(NetworkCallbackController.TAG, "NEW NETWORK : " + network);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            WifiConnectivityMonitor.this.getCm().registerNetworkCallback(req.build(), this.mConnectionDetector);
        }

        private void registerNetworkCallbacks() {
            NetworkRequest.Builder req = new NetworkRequest.Builder();
            req.addTransportType(1);
            this.mNetworkCallback = getCallback();
            WifiConnectivityMonitor.this.getCm().registerNetworkCallback(req.build(), this.mNetworkCallback);
            new NetworkRequest.Builder();
            req.addTransportType(1);
            this.mNetworkCallbackDummy = getDummyCallback();
            WifiConnectivityMonitor.this.getCm().requestNetwork(req.build(), this.mNetworkCallbackDummy);
            NetworkRequest.Builder req2 = new NetworkRequest.Builder();
            req2.addTransportType(1);
            req2.addCapability(17);
            this.mCaptivePortalCallback = getCaptivePortalCallback();
            WifiConnectivityMonitor.this.getCm().registerNetworkCallback(req2.build(), this.mCaptivePortalCallback);
            this.mDefaultNetworkCallback = getDefaultNetworkCallback();
            WifiConnectivityMonitor.this.getCm().registerDefaultNetworkCallback(this.mDefaultNetworkCallback);
        }

        private ConnectivityManager.NetworkCallback getDefaultNetworkCallback() {
            return new ConnectivityManager.NetworkCallback() {
                public void onAvailable(Network network) {
                    NetworkCapabilities np = WifiConnectivityMonitor.this.getCm().getNetworkCapabilities(network);
                    if (np != null && np.hasTransport(3)) {
                        try {
                            if (NetworkCallbackController.this.mNetworkCallbackDummy != null) {
                                WifiConnectivityMonitor.this.getCm().unregisterNetworkCallback(NetworkCallbackController.this.mNetworkCallbackDummy);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } catch (Throwable th) {
                            ConnectivityManager.NetworkCallback unused = NetworkCallbackController.this.mNetworkCallbackDummy = null;
                            throw th;
                        }
                        ConnectivityManager.NetworkCallback unused2 = NetworkCallbackController.this.mNetworkCallbackDummy = null;
                    }
                }

                public void onLost(Network network) {
                }
            };
        }

        private ConnectivityManager.NetworkCallback getCallback() {
            return new ConnectivityManager.NetworkCallback() {
                public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                    Log.i(NetworkCallbackController.TAG, "mNetworkCallback onCapabilitiesChanged: " + network.toString() + nc.toString());
                    if (nc.hasCapability(16)) {
                        if (!NetworkCallbackController.this.mHasValidatedCapa) {
                            boolean unused = NetworkCallbackController.this.mHasValidatedCapa = true;
                            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.VALIDATED_DETECTED);
                        }
                        if (NetworkCallbackController.this.mHasCaptivePortalCapa) {
                            WifiConnectivityMonitor.this.getClientModeChannel().updateNetworkSelectionStatus(NetworkCallbackController.this.mNetId, 0);
                        }
                    } else if (NetworkCallbackController.this.mHasValidatedCapa && WifiConnectivityMonitor.mInitialResultSentToSystemUi) {
                        boolean unused2 = NetworkCallbackController.this.mHasValidatedCapa = false;
                        WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.INVALIDATED_DETECTED);
                    }
                }
            };
        }

        private ConnectivityManager.NetworkCallback getDummyCallback() {
            return new ConnectivityManager.NetworkCallback() {
                public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                }

                public void onLost(Network network) {
                }
            };
        }

        private ConnectivityManager.NetworkCallback getCaptivePortalCallback() {
            return new ConnectivityManager.NetworkCallback() {
                public void onAvailable(Network network) {
                    Log.i(NetworkCallbackController.TAG, "mCaptivePortalCallback: " + network.toString());
                }

                public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                    Log.i(NetworkCallbackController.TAG, "mCaptivePortalCallback onCapabilitiesChanged: " + network.toString() + nc.toString());
                    if (nc.hasCapability(17)) {
                        WifiInfo unused = WifiConnectivityMonitor.this.mWifiInfo = WifiConnectivityMonitor.this.syncGetCurrentWifiInfo();
                        if (!NetworkCallbackController.this.mHasCaptivePortalCapa) {
                            if (WifiConnectivityMonitor.this.mCurrentMode == 1 && !WifiConnectivityMonitor.this.mAirPlaneMode) {
                                WifiConnectivityMonitor.this.getCm().setWcmAcceptUnvalidated(WifiConnectivityMonitor.this.mNetwork, false);
                            }
                            boolean unused2 = NetworkCallbackController.this.mHasCaptivePortalCapa = true;
                            NetworkCallbackController networkCallbackController = NetworkCallbackController.this;
                            networkCallbackController.mNetId = WifiConnectivityMonitor.this.mWifiInfo.getNetworkId();
                            Log.i(NetworkCallbackController.TAG, "Disable an unauthenticated Captive Portal AP");
                            WifiConnectivityMonitor.this.getClientModeChannel().updateNetworkSelectionStatus(NetworkCallbackController.this.mNetId, 15);
                            WifiConnectivityMonitor.this.getClientModeChannel().setCaptivePortal(NetworkCallbackController.this.mNetId, true);
                            WifiConnectivityMonitor.this.removeMessages(WifiConnectivityMonitor.STOP_BLINKING_WIFI_ICON);
                        }
                        if (WifiConnectivityMonitor.getEverQualityTested() == 1) {
                            int networkId = -1;
                            if (WifiConnectivityMonitor.this.mWifiInfo != null) {
                                networkId = WifiConnectivityMonitor.this.mWifiInfo.getNetworkId();
                            }
                            WifiConfiguration wifiConfiguration = null;
                            if (networkId != -1) {
                                wifiConfiguration = WifiConnectivityMonitor.this.getWifiConfiguration(networkId);
                            }
                            if (wifiConfiguration != null && wifiConfiguration.isCaptivePortal && !NetworkCallbackController.this.mIsScoreChangedForCaptivePortal) {
                                WifiConnectivityMonitor.this.setWifiNetworkEnabled(false);
                                boolean unused3 = NetworkCallbackController.this.mIsScoreChangedForCaptivePortal = true;
                            }
                        }
                        if (WifiConnectivityMonitor.this.getCurrentState() != WifiConnectivityMonitor.this.mCaptivePortalState) {
                            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CAPTIVE_PORTAL_DETECTED);
                        }
                    }
                }

                public void onLost(Network network) {
                    Log.i(NetworkCallbackController.TAG, "mCaptivePortalCallback onLost : " + network.toString());
                    if (NetworkCallbackController.this.mIsScoreChangedForCaptivePortal) {
                        boolean unused = NetworkCallbackController.this.mIsScoreChangedForCaptivePortal = false;
                        WifiConnectivityMonitor.this.setWifiNetworkEnabled(true);
                    }
                }
            };
        }
    }

    public void setCaptivePortalMode(int enabled) {
        Settings.Global.putInt(this.mContext.getContentResolver(), WCMCallbacks.CAPTIVE_PORTAL_MODE, enabled);
    }

    public class WCMCallbacks implements ClientModeImpl.ClientModeChannel.WifiNetworkCallback {
        public static final String CAPTIVE_PORTAL_MODE = "captive_portal_mode";
        public static final String TAG = "WifiConnectivityMonitor.WCMCallbacks";

        public WCMCallbacks() {
            Log.i(TAG, "WCMCallbacks created");
        }

        public void checkIsCaptivePortalException(String ssid) {
            ContentResolver contentResolver = WifiConnectivityMonitor.this.mContext.getContentResolver();
            boolean isCaptivePortalCheckEnabled = !WifiConnectivityMonitor.this.isCaptivePortalExceptionOnly(ssid) && !WifiConnectivityMonitor.this.isIgnorableNetwork(ssid);
            Log.i(TAG, "isCaptivePortalCheckEnabled result on " + ssid + ": " + isCaptivePortalCheckEnabled);
            boolean unused = WifiConnectivityMonitor.mIsNoCheck = !isCaptivePortalCheckEnabled;
            if (isCaptivePortalCheckEnabled) {
                WifiConnectivityMonitor.this.setCaptivePortalMode(1);
                return;
            }
            WifiConnectivityMonitor.this.setCaptivePortalMode(0);
            int ret = Settings.Global.getInt(contentResolver, CAPTIVE_PORTAL_MODE, 1);
            Log.i(TAG, "It is CaptivePortalCheck Enabled - : " + ret);
            if (ret != 0) {
                for (int idx = 0; idx < 10; idx++) {
                    WifiConnectivityMonitor.this.setCaptivePortalMode(0);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ret = Settings.Global.getInt(contentResolver, CAPTIVE_PORTAL_MODE, 1);
                    if (ret == 0) {
                        break;
                    }
                }
                Log.i(TAG, "It is CaptivePortalCheck Enabled(1000 later) - : " + ret);
            }
        }

        public void notifyRoamSession(String startComplete) {
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_ROAM_START_COMPLETE, 0, 0, startComplete);
        }

        public void notifyDhcpSession(String startComplete) {
            if ("start".equals(startComplete)) {
                WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_DHCP_SESSION_STARTED);
            } else if ("complete".equals(startComplete)) {
                WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.EVENT_DHCP_SESSION_COMPLETE);
            }
        }

        public void notifyLinkPropertiesUpdated(LinkProperties lp) {
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_NETWORK_PROPERTIES_UPDATED, 0, 0, lp);
        }

        public void notifyReachabilityLost() {
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_REACHABILITY_LOST);
        }

        public void notifyProvisioningFail() {
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.CMD_PROVISIONING_FAIL);
        }

        public void handleResultRoamInLevel1State(boolean roamFound) {
            Log.d(TAG, "HANDLE_RESULT_ROAM_IN_HIGH_QUALITY - roamFound: " + roamFound);
            WifiConnectivityMonitor.this.sendMessage(WifiConnectivityMonitor.HANDLE_RESULT_ROAM_IN_HIGH_QUALITY, roamFound, 0, (Object) null);
        }
    }

    public void setUserSelection(boolean keepConnection) {
        if (DBG) {
            Log.e(TAG, "setUserSelect : " + keepConnection);
        }
        sendMessage(EVENT_USER_SELECTION, keepConnection);
    }

    /* access modifiers changed from: private */
    public boolean skipPoorConnectionReport() {
        if (this.mWifiInfo == null || !this.mClientModeImpl.isConnected() || getCurrentState() != this.mLevel1State || this.mWifiInfo.getRssi() <= -80 || !this.mIsRoamingNetwork) {
            return false;
        }
        Log.d(TAG, "skipPoorConnectionReport - Condition satisfied.");
        return true;
    }

    public WifiConfiguration getWifiConfiguration(int netID) {
        if (netID == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(netID);
    }

    public boolean isCaptivePortalExceptionOnly(String _ssid) {
        int reason = -1;
        String ssid = null;
        int networkId = -1;
        WifiInfo wifiInfo = this.mWifiInfo;
        if (wifiInfo != null && _ssid == null) {
            ssid = wifiInfo.getSSID();
            networkId = this.mWifiInfo.getNetworkId();
        }
        if (ssid == null && _ssid != null) {
            ssid = _ssid;
        }
        WifiConfiguration wifiConfiguration = null;
        if (networkId != -1) {
            wifiConfiguration = getWifiConfiguration(networkId);
        }
        if (!(!"WeChatWiFi".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSOCIALSVCINTEGRATIONN)) || networkId == -1 || this.mWifiManager == null || wifiConfiguration == null)) {
            Log.d(TAG, "isCaptivePortalExceptionOnly, isWeChatAp: " + wifiConfiguration.semIsWeChatAp);
        }
        if ("CCT".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CAPTIVEPORTALEXCEPTION)) && isPackageExists("com.smithmicro.netwise.director.comcast.oem") && isComcastSsid(ssid)) {
            reason = 1;
        } else if ("\"attwifi\"".equals(ssid) && "FINISH".equals(SystemProperties.get("persist.sys.setupwizard"))) {
            reason = 2;
        } else if ("\"SFR WiFi\"".equals(ssid) || "\"SFR WiFi Public\"".equals(ssid) || "\"SFR WiFi Gares\"".equals(ssid) || "\"SFR WiFi FON\"".equals(ssid) || "\"WiFi Partenaires\"".equals(ssid)) {
            reason = 4;
        } else if (!"FINISH".equals(SystemProperties.get("persist.sys.setupwizard")) && readReactiveLockFlag(this.mContext)) {
            reason = 5;
        } else if ("\"CelcomWifi\"".equals(ssid)) {
            reason = 6;
        } else if ("\"O2 Wifi\"".equals(ssid)) {
            reason = 7;
        } else if ("\"UL Mobile\"".equals(ssid)) {
            reason = 8;
        } else if (this.CSC_WIFI_SUPPORT_VZW_EAP_AKA && wifiConfiguration != null && wifiConfiguration.semIsVendorSpecificSsid) {
            reason = 9;
        } else if (this.mIsFmcNetwork) {
            reason = 10;
        } else if (("WeChatWiFi".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSOCIALSVCINTEGRATIONN)) && isPackageRunning(this.mContext, "com.tencent.mm")) || (wifiConfiguration != null && wifiConfiguration.semIsWeChatAp)) {
            reason = 11;
        }
        if (reason == -1) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(DBG ? "isCaptivePortalExceptionOnly - reason #" : "CPEO #");
        sb.append(reason);
        logd(sb.toString());
        return true;
    }

    public boolean isIgnorableNetwork(String _ssid) {
        int reason = -1;
        String ssid = null;
        int networkId = -1;
        WifiInfo wifiInfo = this.mWifiInfo;
        if (wifiInfo != null && _ssid == null) {
            ssid = wifiInfo.getSSID();
            networkId = this.mWifiInfo.getNetworkId();
        }
        if (ssid == null && _ssid != null) {
            ssid = _ssid;
        }
        WifiConfiguration wifiConfiguration = null;
        if (networkId != -1) {
            wifiConfiguration = getWifiConfiguration(networkId);
        }
        if ("ATT".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CAPTIVEPORTALEXCEPTION)) && isPackageRunning(this.mContext, "com.synchronoss.dcs.att.r2g")) {
            reason = 1;
        } else if (ssid != null && ssid.contains("DIRECT-") && ssid.contains(":NEX-")) {
            reason = 2;
        } else if (isPackageRunning(this.mContext, "de.telekom.hotspotlogin")) {
            reason = 3;
        } else if (isPackageRunning(this.mContext, "com.belgacom.fon")) {
            reason = 4;
        } else if ("CHM".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CAPTIVEPORTALEXCEPTION)) && (isPackageRunning(this.mContext, "com.chinamobile.cmccwifi") || isPackageRunning(this.mContext, "com.chinamobile.cmccwifi.WelcomeActivity") || isPackageRunning(this.mContext, "com.chinamobile.cmccwifi.MainActivity") || isPackageRunning(this.mContext, "com.android.settings.wifi.CMCCChargeWarningDialog"))) {
            reason = 6;
        } else if (("\"au_Wi-Fi\"".equals(ssid) || "\"Wi2\"".equals(ssid) || "\"Wi2premium\"".equals(ssid) || "\"Wi2premium_club\"".equals(ssid) || "\"UQ_Wi-Fi\"".equals(ssid) || "\"wifi_square\"".equals(ssid)) && (isPackageExists("com.kddi.android.au_wifi_connect") || isPackageExists("com.kddi.android.au_wifi_connect2"))) {
            reason = 7;
        } else if (FactoryTest.isFactoryBinary()) {
            reason = 8;
        } else if ("\"mailsky\"".equals(ssid) && this.mIsUsingProxy) {
            reason = 9;
        } else if ("\"COPconnect\"".equals(ssid) && wifiConfiguration.allowedKeyManagement.get(2)) {
            reason = 10;
        } else if ("\"SpirentATTEVSAP\"".equals(ssid)) {
            reason = 11;
        }
        if (reason == -1) {
            return false;
        }
        Log.d(TAG, "isIgnorableNetwork - No need to check connectivity: " + ssid + ", reason: " + reason);
        return true;
    }

    private boolean isPackageExists(String targetPackage) {
        try {
            PackageInfo info = this.mContext.getPackageManager().getPackageInfo(targetPackage, 0);
            if (info != null) {
                Log.d(TAG, "isPackageExists - matched: " + info.packageName);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (DBG) {
                Log.w(TAG, "NameNotFoundException + " + e.toString());
            }
        }
        return false;
    }

    private boolean isPackageRunning(Context context, String packageName) {
        if (context == null) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo runningTaskInfo : ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1)) {
            Log.d(TAG, "isPackageRunning - top:" + runningTaskInfo.topActivity.getClassName());
            if (runningTaskInfo.topActivity.getPackageName().contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    private static String getAccountEmail(Context context, String account_type) {
        String accountEmail = null;
        Account[] accountArray = AccountManager.get(context).getAccountsByType(account_type);
        if (accountArray.length > 0) {
            accountEmail = accountArray[0].name;
        }
        if (DBG) {
            Log.d(TAG, "getAccountEmail : " + accountEmail);
        }
        return accountEmail;
    }

    private static boolean readReactiveLockFlag(Context context) {
        boolean value = false;
        int flagResult = new ReactiveServiceManager(context).getStatus();
        if (flagResult < 0 || flagResult > 1) {
            Log.e(TAG, "readReactiveLockFlag - exception occured:" + flagResult);
        } else if (flagResult == 1 && getAccountEmail(context, "com.google") == null) {
            Log.d(TAG, "readReactiveLockFlag : Activated - " + flagResult);
            value = true;
        }
        if (DBG) {
            Log.d(TAG, "readReactiveLockFlag - result: " + value);
        }
        return value;
    }

    private class TxPacketInfo {
        private static final int DISCONNECTED = 3;
        private static final int FAILED = 2;
        private static final int SUCCESS = 1;
        /* access modifiers changed from: private */
        public int mTxbad;
        /* access modifiers changed from: private */
        public int mTxgood;
        /* access modifiers changed from: private */
        public int result;

        private TxPacketInfo() {
        }
    }

    private void eleCheck(int txBad) {
        try {
            if (this.mWifiEleStateTracker == null) {
                return;
            }
            if (this.mWifiEleStateTracker.getEleCheckDoorOpenState()) {
                this.mWifiEleStateTracker.checkEleDoorOpen(this.mTelephonyManager.getSignalStrength().getDbm(), getWifiEleBeaconStats(), getCurrentRssi());
            } else if (!this.mWifiEleStateTracker.getEleCheckEnabled()) {
            } else {
                if (this.mWifiEleStateTracker.getElePollingEnabled()) {
                    int retVal = this.mWifiEleStateTracker.checkEleEnvironment(this.mTelephonyManager.getSignalStrength().getDbm(), getWifiEleBeaconStats(), getCurrentRssi(), txBad);
                    if (retVal != 0) {
                        if (retVal == 2) {
                            sendMessage(CMD_ELE_DETECTED);
                        } else {
                            sendMessage(CMD_ELE_BY_GEO_DETECTED);
                        }
                        if (getClientModeChannel().eleDetectedDebug()) {
                            Toast.makeText(this.mContext, "Ele!", 0).show();
                        }
                    } else {
                        this.mWifiEleStateTracker.setElePollingSkip(false);
                    }
                    return;
                }
                this.mWifiEleStateTracker.setElePollingSkip(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "mWifiEleStateTracker exception happend : " + e.toString());
        }
    }

    /* access modifiers changed from: private */
    public TxPacketInfo fetchPacketCount() {
        Message msg = getClientModeChannel().fetchPacketCountNative();
        TxPacketInfo txPacketInfo = new TxPacketInfo();
        int i = msg.what;
        if (i == 2) {
            int unused = txPacketInfo.result = 1;
            int unused2 = txPacketInfo.mTxgood = msg.arg1;
            int unused3 = txPacketInfo.mTxbad = msg.arg2;
            eleCheck(txPacketInfo.mTxbad);
            if (this.mScoreQualityCheckMode != 0) {
                boolean goPolling = false;
                if (!this.mScoreSkipModeEnalbed) {
                    goPolling = true;
                } else if (this.mScoreSkipPolling) {
                    this.mScoreSkipPolling = false;
                } else {
                    this.mScoreSkipPolling = true;
                    goPolling = true;
                }
                if (goPolling) {
                    int i2 = this.mScoreIntervalCnt;
                    this.mScoreIntervalCnt = i2 + 1;
                    if (i2 == 0) {
                        checkScoreBasedQuality(this.mClientModeImpl.getWifiScoreReport().getCurrentScore(), txPacketInfo.mTxgood, txPacketInfo.mTxbad);
                    }
                    if (this.mScoreIntervalCnt >= 3) {
                        this.mScoreIntervalCnt = 0;
                    }
                }
            }
        } else if (i == 3) {
            int unused4 = txPacketInfo.result = 2;
        } else if (i != 4) {
            int unused5 = txPacketInfo.result = 2;
        }
        msg.recycle();
        return txPacketInfo;
    }

    /* access modifiers changed from: private */
    public void setWifiNetworkEnabled(boolean valid) {
        Log.d(TAG, "set Wifi Network Enabled : " + valid);
        getClientModeChannel().setWifiNetworkEnabled(valid);
    }

    public double getCurrentLoss() {
        VolumeWeightedEMA volumeWeightedEMA = this.mCurrentLoss;
        if (volumeWeightedEMA != null) {
            return volumeWeightedEMA.mValue;
        }
        return POOR_LINK_MIN_VOLUME;
    }

    public void fastDisconnectUpdateRssi(int rssi) {
        if (DBG) {
            Log.d(TAG, "fastDisconnectUpdateRssi: Enter. " + rssi);
        }
        if (this.mRawRssi.size() >= this.mParam.FD_RAW_RSSI_SIZE) {
            this.mRawRssi.removeLast();
        }
        this.mRawRssi.addFirst(Long.valueOf((long) rssi));
    }

    public boolean fastDisconnectEvaluate() {
        int count = 0;
        double total = -0.0d;
        double oldestMARssi = -200.0d;
        double latestMARssi = -200.0d;
        double currentMARssi = -200.0d;
        if (DBG) {
            Log.d(TAG, "fastDisconnectEvaluate: Enter.");
        }
        if (this.mRawRssi.size() < this.mParam.FD_RAW_RSSI_SIZE) {
            if (DBG) {
                Log.d(TAG, "Not enough data to evaluate FD.");
            }
            return false;
        }
        for (int i = 0; i < this.mParam.FD_EVALUATE_COUNT; i++) {
            int j = 0;
            while (j < this.mParam.FD_MA_UNIT_SAMPLE_COUNT) {
                total += (double) this.mRawRssi.get(j + count).longValue();
                j++;
                currentMARssi = currentMARssi;
            }
            currentMARssi = total / ((double) this.mParam.FD_MA_UNIT_SAMPLE_COUNT);
            if (i == 0) {
                latestMARssi = currentMARssi;
            } else if (i == this.mParam.FD_EVALUATE_COUNT - 1) {
                oldestMARssi = currentMARssi;
            }
            count++;
            total = POOR_LINK_MIN_VOLUME;
        }
        double d = currentMARssi;
        double diffMARssi = oldestMARssi - latestMARssi;
        if (DBG) {
            Log.d(TAG, "fastDisconnectEvaluate: oldest=" + oldestMARssi + ", latest=" + latestMARssi + ", diff=" + (oldestMARssi - latestMARssi));
        }
        if (mRawRssiEMA == -200.0d) {
            mRawRssiEMA = diffMARssi;
        } else {
            Objects.requireNonNull(this.mParam);
            Objects.requireNonNull(this.mParam);
            mRawRssiEMA = (0.2d * diffMARssi) + (0.8d * mRawRssiEMA);
        }
        if (diffMARssi > this.mParam.FD_DISCONNECT_THRESHOLD) {
            if (DBG) {
                Log.d(TAG, "A sharp fall! Disconnect!");
            }
            return true;
        }
        double d2 = mRawRssiEMA;
        Objects.requireNonNull(this.mParam);
        if (d2 < 4.0d) {
            return false;
        }
        if (DBG) {
            Log.d(TAG, "A sharp fall trend! Disconnect!");
        }
        return true;
    }

    public void fastDisconnectClear() {
        for (int i = 0; i < this.mRawRssi.size(); i++) {
            this.mRawRssi.remove();
        }
        mRawRssiEMA = -200.0d;
    }

    /* access modifiers changed from: private */
    public void setRssiPatchHistory(int txbad, int txgood, long rxgood) {
        setRssiPatchHistory((String) null, txbad, txgood, rxgood);
    }

    private void setRssiPatchHistory(String dumpLog, int txbad, int txgood, long rxgood) {
        StringBuilder builder = new StringBuilder();
        int i = this.mRssiPatchHistoryHead;
        if (i == -1) {
            this.mRssiPatchHistoryHead = i + 1;
        } else {
            this.mRssiPatchHistoryHead = i % RSSI_PATCH_HISTORY_COUNT_MAX;
        }
        String currentTime = "" + (System.currentTimeMillis() / 1000);
        try {
            currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        } catch (RuntimeException e) {
        }
        if (dumpLog == null) {
            try {
                builder.append(currentTime);
                builder.append(": " + txbad);
                builder.append(", " + txgood);
                builder.append(", " + rxgood);
                builder.append(", " + this.mWifiInfo.getRxLinkSpeedMbps());
            } catch (RuntimeException e2) {
                builder.append(currentTime + ", ex");
            }
        } else {
            builder.append(", " + dumpLog);
        }
        this.mRssiPatchHistory[this.mRssiPatchHistoryHead] = builder.toString();
        this.mRssiPatchHistoryHead++;
        this.mRssiPatchHistoryTotal++;
    }

    private void setWifiHideIconHistory(int visible) {
        StringBuilder builder = new StringBuilder();
        this.mHideIconHistoryHead %= 100;
        String currentTime = "" + (System.currentTimeMillis() / 1000);
        try {
            currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        } catch (RuntimeException e) {
        }
        try {
            builder.append(currentTime);
            builder.append(": " + visible);
        } catch (RuntimeException e2) {
            builder.append(currentTime + ", ex");
        }
        this.mHideIconHistory[this.mHideIconHistoryHead] = builder.toString();
        this.mHideIconHistoryHead++;
        this.mHideIconHistoryTotal++;
    }

    /* access modifiers changed from: private */
    public void uploadNoInternetECNTBigData() {
        int reason;
        Log.d(TAG, "uploadNoInternetECNTBigData");
        mIsECNTReportedConnection = true;
        if (this.mPreviousTxBadTxGoodRatio > 100) {
            reason = 2;
        } else {
            reason = 0;
        }
        this.mWifiNative.requestFwBigDataParams(INTERFACENAMEOFWLAN, 0, reason, 0);
        mIsECNTReportedConnection = true;
        mIsPassedLevel1State = false;
    }

    /* access modifiers changed from: private */
    public void createEleObjects() {
        Log.d(TAG, "createEleObjects");
        if (this.mWifiEleStateTracker != null || !WifiEleStateTracker.checkPedometerSensorAvailable(this.mContext)) {
            Log.d(TAG, "createEleObjects ignored due to not available condition");
            return;
        }
        this.mWifiEleStateTracker = new WifiEleStateTracker(this.mContext, this.mWifiNative, this);
        Log.d(TAG, "createEleObjects done");
    }

    private int getWifiEleBeaconStats() {
        WifiLinkLayerStats stats = this.mWifiNative.getWifiLinkLayerStats(INTERFACENAMEOFWLAN);
        if (stats != null) {
            return stats.beacon_rx;
        }
        return 0;
    }

    /* access modifiers changed from: private */
    public void startEleCheck() {
        if (isLevel1StateState()) {
            int i = this.mCurrentMode;
            if ((i == 2 || i == 3) && this.mWifiEleStateTracker != null) {
                Log.i(TAG, "enableEleMobileRssiPolling true and enable WifiPedometerChecker by EVENT_SCREEN_ON");
                this.mWifiEleStateTracker.enableEleMobileRssiPolling(true, true);
                this.mWifiEleStateTracker.checkStepCntChangeForGeoMagneticSensor();
                this.mWifiEleStateTracker.screenSet(true);
            }
        }
    }

    /* access modifiers changed from: private */
    public void screenOffEleInitialize() {
        WifiEleStateTracker wifiEleStateTracker = this.mWifiEleStateTracker;
        if (wifiEleStateTracker != null && wifiEleStateTracker.getScreenOffResetRequired()) {
            Log.i(TAG, "disable eleEleMobileRssiPolling and disable WifiPedometerChecker by EVENT_SCREEN_OFF");
            this.mWifiEleStateTracker.enableEleMobileRssiPolling(false, false);
            this.mWifiEleStateTracker.getCurrentStepCnt();
            if (getCurrentState() == this.mLevel2State && (this.mWifiEleStateTracker.checkEleValidBlockState() || this.mWifiNeedRecoveryFromEle)) {
                int i = this.mTrafficPollToken + 1;
                this.mTrafficPollToken = i;
                sendMessage(obtainMessage(CMD_TRAFFIC_POLL, i, 0));
                if (this.mCurrentMode == 2) {
                    this.mSnsBigDataSCNT.mPqGqNormal++;
                } else {
                    this.mSnsBigDataSCNT.mPqGqAgg++;
                }
                transitionTo(this.mLevel1State);
            }
            this.mWifiNeedRecoveryFromEle = false;
            this.mWifiEleStateTracker.clearEleValidBlockFlag();
            this.mWifiEleStateTracker.resetEleParameters(0, true, true);
            this.mWifiEleStateTracker.screenSet(false);
        }
    }

    private int getCurrentRssi() {
        WifiNative.SignalPollResult pollResult = this.mWifiNative.signalPoll(INTERFACENAMEOFWLAN);
        if (pollResult == null) {
            return -1;
        }
        return pollResult.currentRssi;
    }

    public void enableRecoveryFromEle() {
        if (getCurrentState() == this.mLevel2State && this.mCurrentMode == 2) {
            sendMessage(CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE);
        } else {
            this.mWifiNeedRecoveryFromEle = true;
        }
    }

    public void eleCheckFinished() {
        determineMode();
    }

    /* access modifiers changed from: private */
    public void startPoorQualityScoreCheck() {
        Log.i(TAG, "SCORE_QUALITY_CHECK_STATE_POOR_MONITOR");
        this.mScoreQualityCheckMode = 2;
        int[] iArr = this.mPreviousScore;
        iArr[0] = 0;
        iArr[1] = 0;
        iArr[2] = 0;
        this.mPrevoiusScoreAverage = 0;
        this.mPreviousTxBad = 0;
        this.mPreviousTxSuccess = 0;
        this.mPreviousTxBadTxGoodRatio = 0;
        this.mLastGoodScore = 1000;
        this.mLastPoorScore = 100;
        this.mPoorCheckInProgress = false;
        this.mScoreIntervalCnt = 0;
        this.mWifiNeedRecoveryFromEle = false;
    }

    /* access modifiers changed from: private */
    public void startRecoveryScoreCheck() {
        Log.i(TAG, "SCORE_QUALITY_CHECK_STATE_RECOVERY");
        this.mScoreQualityCheckMode = 4;
        this.mPreviousTxBad = 0;
        this.mPreviousTxSuccess = 0;
        this.mPreviousTxBadTxGoodRatio = 0;
        this.mEleGoodScoreCnt = 0;
    }

    private void startGoodQualityScoreCheck() {
        Log.i(TAG, "SCORE_QUALITY_CHECK_STATE_VALID_CHECK");
        this.mScoreQualityCheckMode = 1;
        this.mGoodScoreCount = 0;
        this.mGoodScoreTotal = 0;
    }

    /* access modifiers changed from: private */
    public void stopScoreQualityCheck() {
        Log.i(TAG, "SCORE_QUALITY_CHECK_STATE_NONE");
        this.mScoreQualityCheckMode = 0;
    }

    private void checkScoreBasedQuality(int s2Score, int txGood, int txBad) {
        long currentTxBadRatio;
        int i = s2Score;
        int i2 = txGood;
        int i3 = txBad;
        int i4 = this.mScoreQualityCheckMode;
        if (i4 >= 2) {
            long txBadDiff = ((long) i3) - this.mPreviousTxBad;
            long TxGoodDiff = ((long) i2) - this.mPreviousTxSuccess;
            int validScoreCnt = 0;
            this.mPreviousTxBad = (long) i3;
            this.mPreviousTxSuccess = (long) i2;
            if (i4 != 4) {
                if (i <= 50) {
                    Log.i(TAG, "checkScoreBasedQuality - less than 50 : s2Score : " + i);
                    if (i < this.mLastPoorScore) {
                        this.mLastPoorScore = i;
                    }
                }
                for (int x = 0; x < 3; x++) {
                    if (this.mPreviousScore[x] != 0) {
                        validScoreCnt++;
                    }
                }
                int[] iArr = this.mPreviousScore;
                int scoreTotal = iArr[0] + iArr[1] + iArr[2];
                if (validScoreCnt > 0) {
                    this.mPrevoiusScoreAverage = scoreTotal / validScoreCnt;
                } else {
                    this.mPrevoiusScoreAverage = i;
                }
                if (this.mScoreQualityCheckMode != 3 || this.mLastGoodScore <= i) {
                    if (txBadDiff > 5) {
                        if (TxGoodDiff > 0) {
                            currentTxBadRatio = (long) ((int) ((((float) txBadDiff) * 100.0f) / ((float) TxGoodDiff)));
                        } else {
                            currentTxBadRatio = txBadDiff + 100;
                        }
                        Log.i(TAG, "checkScoreBasedQuality -  currentTxBadRatio:" + currentTxBadRatio);
                        if (currentTxBadRatio > 15) {
                            if (this.mScoreQualityCheckMode != 2) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("SCORE_QUALITY_CHECK_STATE_POOR_CHECK:  mPreviousTxBadTxGoodRatio:");
                                long j = txBadDiff;
                                sb.append(this.mPreviousTxBadTxGoodRatio);
                                Log.i(TAG, sb.toString());
                                long j2 = this.mPreviousTxBadTxGoodRatio;
                                if (j2 != 0 && currentTxBadRatio > j2) {
                                    if (this.mPoorCheckInProgress) {
                                        this.mPoorCheckInProgress = false;
                                    } else {
                                        Log.i(TAG, "checkScoreBasedQuality - Score Quality Check by txBadRatio increase");
                                        sendMessage(CMD_QUALITY_CHECK_BY_SCORE);
                                        this.mPoorCheckInProgress = true;
                                    }
                                }
                            } else if (i * validScoreCnt < scoreTotal) {
                                Log.i(TAG, "SCORE_QUALITY_CHECK_STATE_POOR_CHECK");
                                this.mScoreQualityCheckMode = 3;
                                if (this.mPoorCheckInProgress) {
                                    this.mPoorCheckInProgress = false;
                                    long j3 = txBadDiff;
                                } else {
                                    this.mPoorCheckInProgress = true;
                                    Log.i(TAG, "checkScoreBasedQuality - Score Quality Check by averageScore decrease");
                                    sendMessage(CMD_QUALITY_CHECK_BY_SCORE);
                                    long j4 = txBadDiff;
                                }
                            } else {
                                long j5 = txBadDiff;
                            }
                            if (this.mPreviousTxBadTxGoodRatio < currentTxBadRatio) {
                                this.mPreviousTxBadTxGoodRatio = currentTxBadRatio;
                            }
                        }
                    }
                } else if (this.mPoorCheckInProgress) {
                    this.mPoorCheckInProgress = false;
                    long j6 = txBadDiff;
                } else {
                    this.mPoorCheckInProgress = true;
                    Log.i(TAG, "checkScoreBasedQuality - Score Quality Check by score decrease");
                    sendMessage(CMD_QUALITY_CHECK_BY_SCORE);
                    long j7 = txBadDiff;
                }
                int[] iArr2 = this.mPreviousScore;
                iArr2[0] = iArr2[1];
                iArr2[1] = iArr2[2];
                if (!this.mPoorCheckInProgress && this.mLastGoodScore > iArr2[2]) {
                    this.mLastGoodScore = iArr2[2];
                }
                this.mPreviousScore[2] = i;
            } else if (this.mWifiNeedRecoveryFromEle) {
                Log.i(TAG, "checkScoreBasedQuality recovery condition check from Ele");
                if (i <= 50 || txBadDiff != 0 || TxGoodDiff <= 0) {
                    this.mEleGoodScoreCnt = 0;
                } else {
                    this.mEleGoodScoreCnt++;
                }
                if (this.mEleGoodScoreCnt >= 2 || (i >= this.mTransitionScore + 3 && txBadDiff == 0)) {
                    sendMessage(CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE);
                }
            }
        } else {
            this.mGoodScoreCount++;
            this.mGoodScoreTotal += i;
            if (this.mGoodScoreCount >= 3) {
                int newAverage = this.mGoodScoreTotal / 3;
                Log.i(TAG, "checkScoreBasedQuality - newAverage: " + newAverage);
                if (newAverage > this.mPrevoiusScoreAverage) {
                    Log.i(TAG, "checkScoreBasedQuality - Score Quality Check by score increase");
                    sendMessage(CMD_QUALITY_CHECK_BY_SCORE);
                    this.mPrevoiusScoreAverage = newAverage;
                }
                this.mGoodScoreTotal = 0;
                this.mGoodScoreCount = 0;
            }
        }
    }

    /* access modifiers changed from: private */
    public void showNetworkSwitchedNotification(boolean visible) {
        String ssid = this.mWifiInfo.getSSID();
        int networkId = this.mWifiInfo.getNetworkId();
        if ("DEFAULT_ON".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSNSSTATUS))) {
            if (!visible || Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_poor_connection_warning", 0) != 1) {
                if (!visible) {
                    this.mPoorConnectionDisconnectedNetId = -1;
                }
                this.mNotifier.showWifiPoorConnectionNotification(ssid, networkId, visible);
                return;
            }
            Log.e(TAG, "Ignore msg from WCM because of WIFI_POOR_CONNECTION_WARNING(DoNotShow flag is true)");
        }
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(Message message, State state) {
        if (SMARTCM_DBG) {
            Log.i(TAG, " " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }
    }

    private boolean isComcastSsid(String ssid) {
        if (!mIsComcastWifiSupported || TextUtils.isEmpty(ssid)) {
            return false;
        }
        String ssid2 = ssid.replace("\"", "");
        Cursor cursor = this.mContext.getContentResolver().query(Uri.parse(NETWISE_CONTENT_URL), (String[]) null, (String) null, (String[]) null, (String) null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        String netwiseSsid = cursor.getString(cursor.getColumnIndex("network"));
                        Log.d(TAG, "netwiseSsid = " + netwiseSsid);
                        if (ssid2.equals(netwiseSsid)) {
                            return true;
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();
        if (this.mPoorNetworkDetectionEnabled) {
            sb.append("!");
        }
        sb.append(smToString(msg.what));
        switch (msg.what) {
            case REPORT_NETWORK_CONNECTIVITY /*135204*/:
                sb.append(" step :");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" trigger :");
                sb.append(Integer.toString(msg.arg2));
                break;
            case EVENT_INET_CONDITION_ACTION /*135245*/:
                sb.append(" networkType :");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" valid :");
                sb.append(Integer.toString(msg.arg2));
                break;
            case EVENT_USER_SELECTION /*135264*/:
                sb.append(" keepConnection:");
                sb.append(Integer.toString(msg.arg1));
                break;
            case CMD_IWC_CURRENT_QAI /*135368*/:
                int qai = msg.getData().getInt("qai");
                sb.append(" ");
                sb.append(Integer.toString(qai));
                break;
            case CMD_ROAM_START_COMPLETE /*135477*/:
                sb.append(" ");
                sb.append((String) msg.obj);
                break;
            default:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                break;
        }
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public String smToString(int what) {
        String s;
        switch (what) {
            case EVENT_NETWORK_STATE_CHANGE /*135170*/:
                s = "EVENT_NETWORK_STATE_CHANGE";
                break;
            case EVENT_RSSI_CHANGE /*135171*/:
                s = "EVENT_RSSI_CHANGE";
                break;
            case EVENT_WIFI_RADIO_STATE_CHANGE /*135173*/:
                s = "EVENT_WIFI_RADIO_STATE_CHANGE";
                break;
            case EVENT_WATCHDOG_SETTINGS_CHANGE /*135174*/:
                s = "EVENT_WATCHDOG_SETTINGS_CHANGE";
                break;
            case EVENT_BSSID_CHANGE /*135175*/:
                s = "EVENT_BSSID_CHANGE";
                break;
            case EVENT_SCREEN_ON /*135176*/:
                s = "EVENT_SCREEN_ON";
                break;
            case EVENT_SCREEN_OFF /*135177*/:
                s = "EVENT_SCREEN_OFF";
                break;
            case CMD_RSSI_FETCH /*135188*/:
                s = "CMD_RSSI_FETCH";
                break;
            case CMD_TRAFFIC_POLL /*135193*/:
                s = "CMD_TRAFFIC_POLL";
                break;
            case REPORT_QC_RESULT /*135203*/:
                s = "REPORT_QC_RESULT";
                break;
            case REPORT_NETWORK_CONNECTIVITY /*135204*/:
                s = "REPORT_NETWORK_CONNECTIVITY";
                break;
            case CONNECTIVITY_VALIDATION_BLOCK /*135205*/:
                s = "CONNECTIVITY_VALIDATION_BLOCK";
                break;
            case CONNECTIVITY_VALIDATION_RESULT /*135206*/:
                s = "CONNECTIVITY_VALIDATION_RESULT";
                break;
            case VALIDATION_CHECK_FORCE /*135207*/:
                s = "VALIDATION_CHECK_FORCE";
                break;
            case EVENT_SCAN_STARTED /*135229*/:
                s = "EVENT_SCAN_STARTED";
                break;
            case EVENT_SCAN_COMPLETE /*135230*/:
                s = "EVENT_SCAN_COMPLETE";
                break;
            case EVENT_SCAN_TIMEOUT /*135231*/:
                s = "EVENT_SCAN_TIMEOUT";
                break;
            case EVENT_MOBILE_CONNECTED /*135232*/:
                s = "EVENT_MOBILE_CONNECTED";
                break;
            case EVENT_NETWORK_PROPERTIES_CHANGED /*135235*/:
                s = "EVENT_NETWORK_PROPERTIES_CHANGED";
                break;
            case EVENT_DHCP_SESSION_STARTED /*135236*/:
                s = "EVENT_DHCP_SESSION_STARTED";
                break;
            case EVENT_DHCP_SESSION_COMPLETE /*135237*/:
                s = "EVENT_DHCP_SESSION_COMPLETE";
                break;
            case EVENT_NETWORK_REMOVED /*135244*/:
                s = "EVENT_NETWORK_REMOVED";
                break;
            case EVENT_INET_CONDITION_ACTION /*135245*/:
                s = "EVENT_INET_CONDITION_ACTION";
                break;
            case EVENT_ROAM_TIMEOUT /*135249*/:
                s = "EVENT_ROAM_TIMEOUT";
                break;
            case EVENT_USER_SELECTION /*135264*/:
                s = "EVENT_USER_SELECTION";
                break;
            case EVENT_WIFI_ICON_VSB /*135265*/:
                s = "EVENT_WIFI_ICON_VSB";
                break;
            case CMD_ELE_DETECTED /*135283*/:
                s = "CMD_ELE_DETECTED";
                break;
            case CMD_ELE_BY_GEO_DETECTED /*135284*/:
                s = "CMD_ELE_BY_GEO_DETECTED";
                break;
            case CMD_TRANSIT_ON_VALID /*135299*/:
                s = "CMD_TRANSIT_ON_VALID";
                break;
            case CMD_TRANSIT_ON_SWITCHABLE /*135300*/:
                s = "CMD_TRANSIT_ON_SWITCHABLE";
                break;
            case CMD_IWC_CURRENT_QAI /*135368*/:
                s = "CMD_IWC_CURRENT_QAI";
                break;
            case CMD_QUALITY_CHECK_BY_SCORE /*135388*/:
                s = "CMD_QUALITY_CHECK_BY_SCORE";
                break;
            case CMD_RECOVERY_TO_HIGH_QUALITY_FROM_LOW_LINK /*135389*/:
                s = "CMD_RECOVERY_TO_HIGH_QUALITY_FROM_LOW_LINK";
                break;
            case CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE /*135390*/:
                s = "CMD_RECOVERY_TO_HIGH_QUALITY_FROM_ELE";
                break;
            case CMD_LINK_GOOD_ENTERED /*135398*/:
                s = "CMD_LINK_GOOD_ENTERED";
                break;
            case CMD_LINK_POOR_ENTERED /*135399*/:
                s = "CMD_LINK_POOR_ENTERED";
                break;
            case CMD_REACHABILITY_LOST /*135400*/:
                s = "CMD_REACHABILITY_LOST";
                break;
            case CMD_PROVISIONING_FAIL /*135401*/:
                s = "CMD_PROVISIONING_FAIL";
                break;
            case STOP_BLINKING_WIFI_ICON /*135468*/:
                s = "STOP_BLINKING_WIFI_ICON";
                break;
            case HANDLE_ON_LOST /*135469*/:
                s = "HANDLE_ON_LOST";
                break;
            case HANDLE_ON_AVAILABLE /*135470*/:
                s = "HANDLE_ON_AVAILABLE";
                break;
            case CAPTIVE_PORTAL_DETECTED /*135471*/:
                s = "CAPTIVE_PORTAL_DETECTED";
                break;
            case VALIDATED_DETECTED /*135472*/:
                s = "VALIDATED_DETECTED";
                break;
            case INVALIDATED_DETECTED /*135473*/:
                s = "INVALIDATED_DETECTED";
                break;
            case VALIDATED_DETECTED_AGAIN /*135474*/:
                s = "VALIDATED_DETECTED_AGAIN";
                break;
            case INVALIDATED_DETECTED_AGAIN /*135475*/:
                s = "INVALIDATED_DETECTED_AGAIN";
                break;
            case CMD_ROAM_START_COMPLETE /*135477*/:
                s = "CMD_ROAM_START_COMPLETE";
                break;
            case CMD_NETWORK_PROPERTIES_UPDATED /*135478*/:
                s = "CMD_NETWORK_PROPERTIES_UPDATED";
                break;
            case CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL /*135481*/:
                s = "CMD_CHECK_ALTERNATIVE_FOR_CAPTIVE_PORTAL";
                break;
            case CMD_UPDATE_CURRENT_BSSID_ON_THROUGHPUT_UPDATE /*135488*/:
                s = "CMD_UPDATE_CURRENT_BSSID_ON_THROUGHPUT_UPDATE";
                break;
            case CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT /*135489*/:
                s = "CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT";
                break;
            case CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT_TYPE /*135490*/:
                s = "CMD_UPDATE_CURRENT_BSSID_ON_DNS_RESULT_TYPE";
                break;
            case CMD_UPDATE_CURRENT_BSSID_ON_QC_RESULT /*135491*/:
                s = "CMD_UPDATE_CURRENT_BSSID_ON_QC_RESULT";
                break;
            default:
                s = "what:" + Integer.toString(what);
                break;
        }
        return "(" + getKernelTime() + ") " + s;
    }

    private String getKernelTime() {
        return Double.toString(((double) (System.nanoTime() / 1000000)) / 1000.0d);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int cCount;
        int cCount2;
        int dCount;
        int dCount2;
        PrintWriter printWriter = pw;
        WifiConnectivityMonitor.super.dump(fd, pw, args);
        printWriter.println("mWifiInfo: [" + this.mWifiInfo + "]");
        printWriter.println("mLinkProperties: [" + this.mLinkProperties + "]");
        printWriter.println("mCurrentSignalLevel: [" + this.mCurrentSignalLevel + "]");
        printWriter.println("mPoorNetworkDetectionEnabled: [" + this.mPoorNetworkDetectionEnabled + "(" + this.mPoorNetworkDetectionSummary + ")]");
        printWriter.println("mUIEnabled: [" + this.mUIEnabled + "(" + this.mPoorNetworkAvoidanceSummary + ")]");
        if (isAggressiveModeSupported()) {
            printWriter.println("mAggressiveModeEnabled: [" + this.mAggressiveModeEnabled + this.mParam.getAggressiveModeFeatureStatus() + "]");
        }
        printWriter.println("InvalidIconVisibility : [invisible]");
        printWriter.println("mLastVisibilityOfWifiIcon : " + this.mLastVisibilityOfWifiIcon);
        printWriter.println("mIwcCurrentQai: " + this.mIwcCurrentQai);
        printWriter.println("mMptcpEnabled: [" + this.mMptcpEnabled + "]");
        if (this.isQCExceptionSummary != null) {
            printWriter.println("isQCExceptionSummary: " + this.isQCExceptionSummary);
        }
        printWriter.println("mQcHistoryTotal: [" + this.mQcHistoryTotal + "], mQcDumpVer: [" + this.mQcDumpVer + "]");
        printWriter.println("info: l2");
        printWriter.println("info: fd");
        ArrayList<String> arrayList = this.mNetstatTestBuffer;
        if (arrayList != null && arrayList.size() > 0) {
            printWriter.println("========NetStat history=======");
            Iterator<String> iterNetstat = this.mNetstatTestBuffer.iterator();
            while (iterNetstat.hasNext()) {
                printWriter.println(iterNetstat.next());
            }
            printWriter.println(" ");
        }
        printWriter.println(" ");
        if (this.mQcHistoryTotal > 0) {
            synchronized (lock) {
                int count = 0;
                int i = this.mQcHistoryTotal < 30 ? 0 : this.mQcHistoryHead % 30;
                while (count < 30) {
                    try {
                        if (this.mQcDumpHistory[i] == null) {
                            count++;
                        } else {
                            printWriter.println("[" + count + "]: " + this.mQcDumpHistory[i]);
                            i = (i + 1) % 30;
                            count++;
                        }
                    } catch (RuntimeException e) {
                        printWriter.println("[" + count + "]: ex");
                    }
                }
            }
            printWriter.println(" ");
        }
        printWriter.println("========TCP STAT========");
        printWriter.println("[1] " + this.mFirstTCPLoggedTime);
        printWriter.println(this.mFirstLogged);
        printWriter.println("[2] " + this.mLastTCPLoggedTime);
        printWriter.println(this.mLastLogged);
        if (this.mWifiSwitchForIndividualAppsService != null) {
            printWriter.println("========SWITCH FOR INDIVIDUAL APPS========");
            printWriter.println(this.mWifiSwitchForIndividualAppsService.dump());
        }
        printWriter.println(" ");
        printWriter.println("[CISO history]");
        printWriter.println("CISO from Scan: " + this.mCountryCodeFromScanResult);
        int i2 = this.incrScanResult;
        if (i2 < 10) {
            cCount = this.incrScanResult;
            cCount2 = 0;
        } else {
            int cStart = i2 % 10;
            cCount = 10;
            cCount2 = cStart;
        }
        for (int i3 = cCount2; i3 < cCount2 + cCount; i3++) {
            printWriter.println(this.summaryCountryCodeFromScanResults[i3 % 10]);
        }
        printWriter.println(" ");
        printWriter.println("[summary D History]");
        int i4 = this.incrDnsResults;
        if (i4 < 50) {
            dCount = this.incrDnsResults;
            dCount2 = 0;
        } else {
            int dStart = i4 % 50;
            dCount = 50;
            dCount2 = dStart;
        }
        for (int i5 = dCount2; i5 < dCount2 + dCount; i5++) {
            printWriter.println(this.summaryDnsResults[i5 % 50]);
        }
        printWriter.println(" ");
        printWriter.println("========HIDE ICON========");
        int count2 = 0;
        int hide_index = this.mHideIconHistoryTotal < 100 ? 0 : this.mHideIconHistoryHead % 100;
        while (count2 < 100) {
            try {
                if (this.mHideIconHistory[hide_index] == null) {
                    count2++;
                } else {
                    printWriter.println("[" + hide_index + "]: " + this.mHideIconHistory[hide_index]);
                    hide_index = (hide_index + 1) % 100;
                    count2++;
                }
            } catch (RuntimeException e2) {
                printWriter.println("[" + hide_index + "]: pre");
            }
        }
        if (this.mConnectivityPacketLogForWlan0 != null) {
            printWriter.println(" ");
            printWriter.println("[connectivity frame log]");
            printWriter.println("WifiConnectivityMonitor Name of interface : wlan0");
            pw.println();
            this.mConnectivityPacketLogForWlan0.readOnlyLocalLog().dump(fd, printWriter, args);
            pw.println();
        } else {
            FileDescriptor fileDescriptor = fd;
            String[] strArr = args;
        }
        printWriter.println("[BSSID QoS Map] - Ver: 1.72");
        printWriter.println(dumpBssidQosMap());
        if (getOpenNetworkQosScores() != null) {
            printWriter.println("getOpenNetworkQosNoInternetStatus: " + getOpenNetworkQosNoInternetStatus());
            String s = "";
            for (int i6 : getOpenNetworkQosScores()) {
                s = s + i6 + " ";
            }
            printWriter.println("getOpenNetworkQosScores: " + s);
            printWriter.println(" ");
        }
        printWriter.println("========PKTCNT_POLL========");
        int count3 = 0;
        int j = this.mRssiPatchHistoryTotal < RSSI_PATCH_HISTORY_COUNT_MAX ? 0 : this.mRssiPatchHistoryHead % RSSI_PATCH_HISTORY_COUNT_MAX;
        while (count3 < RSSI_PATCH_HISTORY_COUNT_MAX) {
            try {
                if (this.mRssiPatchHistory[j] == null) {
                    count3++;
                } else {
                    printWriter.println("[" + count3 + "]: " + this.mRssiPatchHistory[j]);
                    j = (j + 1) % RSSI_PATCH_HISTORY_COUNT_MAX;
                    count3++;
                }
            } catch (RuntimeException e3) {
                printWriter.println("[" + count3 + "]: ex");
            }
        }
        printWriter.println(" ");
        printWriter.println("========TRAFFIC_POLL========");
        int count4 = 0;
        int poll_index = this.mTrafficPollHistoryTotal < 3000 ? 0 : this.mTrafficPollHistoryHead % 3000;
        while (count4 < 3000) {
            try {
                if (this.mTrafficPollHistory[poll_index] == null) {
                    count4++;
                } else {
                    printWriter.println("[" + count4 + "]: " + this.mTrafficPollHistory[poll_index]);
                    poll_index = (poll_index + 1) % 3000;
                    count4++;
                }
            } catch (RuntimeException e4) {
                printWriter.println("[" + count4 + "]: pre");
            }
        }
    }

    /* access modifiers changed from: private */
    public void setTrafficPollHistory(long txBytesLogging, long rxBytesLogging, long fgrndTxBytes, long fgrndRxBytes) {
        StringBuilder builder = new StringBuilder();
        int i = this.mTrafficPollHistoryHead;
        if (i == -1) {
            this.mTrafficPollHistoryHead = i + 1;
        } else {
            this.mTrafficPollHistoryHead = i % 3000;
        }
        String currentTime = "" + (System.currentTimeMillis() / 1000);
        try {
            currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        } catch (RuntimeException e) {
        }
        try {
            builder.append(currentTime);
            builder.append(": " + this.mUsageStatsUid);
            builder.append(", " + txBytesLogging);
            builder.append(", " + rxBytesLogging);
            builder.append(", " + fgrndTxBytes);
            builder.append(", " + fgrndRxBytes);
            if (this.mUsagePackageChanged) {
                this.mTrafficPollPackageName = this.mUsageStatsPackageName;
                builder.append(", " + this.mTrafficPollPackageName);
                this.mUsagePackageChanged = false;
            }
        } catch (RuntimeException e2) {
            builder.append(currentTime + ", ex");
        }
        this.mTrafficPollHistory[this.mTrafficPollHistoryHead] = builder.toString();
        this.mTrafficPollHistoryHead++;
        this.mTrafficPollHistoryTotal++;
    }

    /* access modifiers changed from: private */
    public void setLoggingForTCPStat(int mCheckId) {
        if (!this.isNetstatLoggingHangs) {
            if (mCheckId == TCP_STAT_LOGGING_FIRST) {
                this.mNetstatTestBuffer.add("#" + this.mNetstatBufferCount + ") Netstat TCP_STAT_LOGGING_FIRST with thread " + this.mCurrentNetstatThread);
            } else if (mCheckId == TCP_STAT_LOGGING_SECOND) {
                this.mNetstatTestBuffer.add("#" + this.mNetstatBufferCount + ") Netstat TCP_STAT_LOGGING_SECOND with thread " + this.mCurrentNetstatThread);
                this.mNetstatBufferCount = (this.mNetstatBufferCount + 1) % 50;
            }
            ExecutorService executorService = this.mNetstatThreadPool;
            if (executorService != null) {
                int numOfThreadsInUse = 0;
                for (boolean threadInUse : this.mNetstatThreadInUse) {
                    numOfThreadsInUse += threadInUse;
                }
                if (numOfThreadsInUse >= 10) {
                    this.isNetstatLoggingHangs = true;
                    this.mNetstatTestBuffer.add("#" + this.mNetstatBufferCount + ") Netstat thread hangs detected on " + this.mCurrentNetstatThread);
                }
            } else if (executorService == null) {
                this.mNetstatThreadPool = Executors.newFixedThreadPool(10);
                this.mNetstatThreadInUse = new boolean[10];
                this.mCurrentNetstatThread = 0;
            }
            try {
                this.mNetstatThreadPool.submit(runNetstat(mCheckId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (DBG) {
            Log.i(TAG, "Netstat is not available because of exec command hang");
        }
    }

    private Runnable runNetstat(int mCheckId) {
        return new Runnable(mCheckId) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiConnectivityMonitor.this.lambda$runNetstat$0$WifiConnectivityMonitor(this.f$1);
            }
        };
    }

    /* Debug info: failed to restart local var, previous not found, register: 11 */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00d2, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00d3, code lost:
        if (r6 != null) goto L_0x00d5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        r6.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:?, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00e0, code lost:
        if (r6 != null) goto L_0x00e2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:?, code lost:
        r6.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00ee, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00ef, code lost:
        r11.isNetstatLoggingHangs = false;
        r11.mNetstatThreadInUse[r1] = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00f5, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00f7, code lost:
        r11.isNetstatLoggingHangs = false;
        r11.mNetstatThreadInUse[r1] = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:?, code lost:
        return;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00ee A[ExcHandler: all (r0v3 'th' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:10:0x004a] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:26:0x00cb=Splitter:B:26:0x00cb, B:36:0x00de=Splitter:B:36:0x00de} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public /* synthetic */ void lambda$runNetstat$0$WifiConnectivityMonitor(int r12) {
        /*
            r11 = this;
            java.lang.String r0 = "WifiConnectivityMonitor"
            int r1 = r11.mCurrentNetstatThread
            int r2 = r11.mCurrentNetstatThread
            r3 = 1
            int r2 = r2 + r3
            int r2 = r2 % 10
            r11.mCurrentNetstatThread = r2
            boolean[] r2 = r11.mNetstatThreadInUse
            r2[r1] = r3
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = ""
            r2.append(r3)
            long r4 = java.lang.System.currentTimeMillis()
            r6 = 1000(0x3e8, double:4.94E-321)
            long r4 = r4 / r6
            r2.append(r4)
            java.lang.String r2 = r2.toString()
            android.icu.text.SimpleDateFormat r4 = new android.icu.text.SimpleDateFormat     // Catch:{ RuntimeException -> 0x003c }
            java.lang.String r5 = "MM-dd HH:mm:ss.SSS"
            java.util.Locale r6 = java.util.Locale.US     // Catch:{ RuntimeException -> 0x003c }
            r4.<init>(r5, r6)     // Catch:{ RuntimeException -> 0x003c }
            java.util.Date r5 = new java.util.Date     // Catch:{ RuntimeException -> 0x003c }
            r5.<init>()     // Catch:{ RuntimeException -> 0x003c }
            java.lang.String r5 = r4.format(r5)     // Catch:{ RuntimeException -> 0x003c }
            r2 = r5
            goto L_0x003d
        L_0x003c:
            r4 = move-exception
        L_0x003d:
            int r4 = TCP_STAT_LOGGING_RESET
            if (r12 != r4) goto L_0x0047
            r11.mTempLogged = r3
            r11.mTempTCPLoggedTime = r3
            return
        L_0x0047:
            java.lang.String r3 = ""
            r4 = 0
            java.lang.Runtime r5 = java.lang.Runtime.getRuntime()     // Catch:{ Exception -> 0x00f6, all -> 0x00ee }
            r6 = 0
            java.lang.String r7 = "netstat -tlpanW"
            java.lang.Process r7 = r5.exec(r7)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.io.BufferedReader r8 = new java.io.BufferedReader     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.io.InputStreamReader r9 = new java.io.InputStreamReader     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.io.InputStream r10 = r7.getInputStream()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r9.<init>(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.<init>(r9)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r6 = r8
        L_0x0064:
            java.lang.String r8 = r6.readLine()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r9 = r8
            if (r8 == 0) goto L_0x0081
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.<init>()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.append(r3)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.append(r9)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r10 = "\n"
            r8.append(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r3 = r8
            goto L_0x0064
        L_0x0081:
            int r8 = TCP_STAT_LOGGING_FIRST     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            if (r12 != r8) goto L_0x008a
            r11.mTempTCPLoggedTime = r2     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r11.mTempLogged = r3     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            goto L_0x00ca
        L_0x008a:
            int r8 = TCP_STAT_LOGGING_SECOND     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            if (r12 != r8) goto L_0x00ca
            java.lang.String r8 = r11.mTempTCPLoggedTime     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r11.mFirstTCPLoggedTime = r8     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r11.mLastTCPLoggedTime = r2     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r8 = r11.mTempLogged     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r11.mFirstLogged = r8     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r11.mLastLogged = r3     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            boolean r8 = DBG     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            if (r8 == 0) goto L_0x00ca
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.<init>()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r10 = "1.TCP stat\n"
            r8.append(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r10 = r11.mFirstLogged     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.append(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            android.util.Log.d(r0, r8)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.<init>()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r10 = "2.TCP stat\n"
            r8.append(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r10 = r11.mLastLogged     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            r8.append(r10)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            java.lang.String r8 = r8.toString()     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
            android.util.Log.d(r0, r8)     // Catch:{ Exception -> 0x00df, all -> 0x00d2 }
        L_0x00ca:
            r6.close()     // Catch:{ Exception -> 0x00d0, all -> 0x00ee }
            r0 = 0
            goto L_0x00e7
        L_0x00d0:
            r0 = move-exception
            goto L_0x00e7
        L_0x00d2:
            r0 = move-exception
            if (r6 == 0) goto L_0x00dc
            r6.close()     // Catch:{ Exception -> 0x00da, all -> 0x00ee }
            r6 = 0
            goto L_0x00dc
        L_0x00da:
            r7 = move-exception
            goto L_0x00dd
        L_0x00dc:
        L_0x00dd:
            throw r0     // Catch:{ Exception -> 0x00f6, all -> 0x00ee }
        L_0x00df:
            r0 = move-exception
            if (r6 == 0) goto L_0x00e6
            r6.close()     // Catch:{ Exception -> 0x00d0, all -> 0x00ee }
            r6 = 0
        L_0x00e6:
        L_0x00e7:
            r11.isNetstatLoggingHangs = r4
            boolean[] r0 = r11.mNetstatThreadInUse
            r0[r1] = r4
            goto L_0x00fd
        L_0x00ee:
            r0 = move-exception
            r11.isNetstatLoggingHangs = r4
            boolean[] r5 = r11.mNetstatThreadInUse
            r5[r1] = r4
            throw r0
        L_0x00f6:
            r0 = move-exception
            r11.isNetstatLoggingHangs = r4
            boolean[] r0 = r11.mNetstatThreadInUse
            r0[r1] = r4
        L_0x00fd:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiConnectivityMonitor.lambda$runNetstat$0$WifiConnectivityMonitor(int):void");
    }

    /* access modifiers changed from: private */
    public void checkTransitionToLevel1StateState() {
        WifiEleStateTracker wifiEleStateTracker = this.mWifiEleStateTracker;
        if (wifiEleStateTracker == null || !wifiEleStateTracker.checkEleValidBlockState()) {
            int i = this.mTrafficPollToken + 1;
            this.mTrafficPollToken = i;
            sendMessage(obtainMessage(CMD_TRAFFIC_POLL, i, 0));
            if (this.mCurrentMode == 2) {
                this.mSnsBigDataSCNT.mPqGqNormal++;
            } else {
                this.mSnsBigDataSCNT.mPqGqAgg++;
            }
            setBigDataQCandNS(true);
            transitionTo(this.mLevel1State);
            return;
        }
        Log.i(TAG, "Tansition to Level1State blocked by EleBlock");
    }

    /* access modifiers changed from: private */
    public void checkTransitionToLevel2State() {
        if (this.mIwcCurrentQai != 3) {
            if (this.mCurrentMode == 2) {
                this.mSnsBigDataSCNT.mGqPqNormal++;
            } else {
                this.mSnsBigDataSCNT.mGqPqAgg++;
            }
            setBigDataQCandNS(false);
            transitionTo(this.mLevel2State);
            return;
        }
        Log.i(TAG, "Tansition to Level2State blocked by QAI 3");
        setLinkDetectMode(0);
    }

    private void checkTransitionToValidState() {
        transitionTo(this.mValidState);
    }

    public boolean isRoamingNetwork() {
        int configuredSecurity;
        if (!isConnectedState()) {
            return false;
        }
        WifiConfiguration currConfig = null;
        int candidateCount = 0;
        WifiInfo wifiInfo = this.mWifiInfo;
        if (wifiInfo != null) {
            currConfig = getSpecificNetwork(wifiInfo.getNetworkId());
        }
        if (currConfig == null) {
            return false;
        }
        String configSsid = currConfig.SSID;
        if (currConfig.allowedKeyManagement.get(1) || currConfig.allowedKeyManagement.get(4) || currConfig.allowedKeyManagement.get(22)) {
            configuredSecurity = 2;
        } else if (currConfig.allowedKeyManagement.get(8)) {
            configuredSecurity = 4;
        } else if (currConfig.allowedKeyManagement.get(2) || currConfig.allowedKeyManagement.get(3) || currConfig.allowedKeyManagement.get(24)) {
            configuredSecurity = 3;
        } else {
            configuredSecurity = currConfig.wepKeys[0] != null ? 1 : 0;
        }
        synchronized (this.mScanResultsLock) {
            for (ScanResult scanResult : this.mScanResults) {
                int scanedSecurity = 0;
                if (scanResult != null) {
                    if (scanResult.capabilities.contains("WEP")) {
                        scanedSecurity = 1;
                    } else if (SUPPORT_WPA3_SAE && scanResult.capabilities.contains("SAE")) {
                        scanedSecurity = 4;
                    } else if (scanResult.capabilities.contains("PSK")) {
                        scanedSecurity = 2;
                    } else if (scanResult.capabilities.contains("EAP")) {
                        scanedSecurity = 3;
                    }
                    if (scanResult.SSID != null && configSsid != null && configSsid.length() > 2 && scanResult.SSID.equals(configSsid.substring(1, configSsid.length() - 1)) && configuredSecurity == scanedSecurity && !currConfig.isCaptivePortal && scanResult.BSSID != null && !scanResult.BSSID.equals(this.mWifiInfo.getBSSID())) {
                        candidateCount++;
                    }
                }
            }
        }
        if (candidateCount <= 0) {
            return false;
        }
        Log.d(TAG, "isRoamingNetwork: " + candidateCount + " additional BSSID(s) found for network " + this.mWifiInfo.getSSID());
        return true;
    }

    public WifiConfiguration getSpecificNetwork(int netID) {
        if (netID == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(netID);
    }

    private void resetBigDataFeatureForSCNT() {
        this.mSnsBigDataSCNT.initialize();
        this.mSnsBigDataManager.clearFeature("SSMA");
    }

    /* access modifiers changed from: private */
    public void sendBigDataFeatureForSCNT() {
        Log.d(TAG, "Sns Big Data SCNT logging");
        if (this.mSnsBigDataManager.addOrUpdateFeatureAllValue("SSMA")) {
            this.mSnsBigDataManager.insertLog("SSMA", -1);
        } else {
            Log.e(TAG, "error on Loging Big Data for SCNT");
        }
        resetBigDataFeatureForSCNT();
    }

    /* access modifiers changed from: private */
    public void setBigDataQualityCheck(boolean pass) {
        setBigDataQualityCheck(pass, false);
    }

    private void setBigDataQualityCheck(boolean pass, boolean doNotReset) {
        boolean z = pass;
        long currentTime = System.currentTimeMillis();
        long bootingElapsedTime = SystemClock.elapsedRealtime();
        if (this.mBigDataQualityCheckStartOfCycle == -1) {
            this.mBigDataQualityCheckStartOfCycle = currentTime - ((long) new Random(currentTime).nextInt((int) mBigDataQualityCheckCycle));
        }
        long j = mBigDataQualityCheckCycle;
        if (currentTime - this.mBigDataQualityCheckStartOfCycle > j) {
            this.mBigDataQualityCheckStartOfCycle = (currentTime / j) * j;
            this.mBigDataQualityCheckLoggingCount = 0;
        }
        int trigger = this.mInvalidationFailHistory.qcTrigger;
        String toString = (((((((((((("setBigDataQualityCheck - " + getCurrentState().getName()) + ", COUNT: " + this.mBigDataQualityCheckLoggingCount) + ", pass: " + z) + ", time: " + bootingElapsedTime) + ", [type]" + this.mInvalidationFailHistory.qcType) + ", [s]" + this.mInvalidationFailHistory.qcStep) + ", [t]" + this.mInvalidationFailHistory.qcTrigger) + ", [e]" + this.mInvalidationFailHistory.error) + ", RSSI: " + this.mCurrentRssi) + ", SPEED: " + this.mCurrentLinkSpeed) + ", " + this.mPoorNetworkDetectionEnabled) + "/" + this.mUIEnabled) + "/" + this.mAggressiveModeEnabled;
        if (this.mBigDataQualityCheckLoggingCount < mMaxBigDataQualityCheckLogging) {
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_RESULT, z ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_TYPE, this.mInvalidationFailHistory.qcType);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_STEP, this.mInvalidationFailHistory.qcStep);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_TRIGGER, trigger);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_FAIL_REASON, this.mInvalidationFailHistory.error);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_BOOTING_ELAPSED_TIME, bootingElapsedTime);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_TOP_PACKAGE_DURATION, convertMiliSecondToSecond(System.currentTimeMillis() - this.mFrontAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_SECOND_PACKAGE, this.mPreviousPackageName);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_SECOND_PACKAGE_DURATION, convertMiliSecondToSecond(this.mFrontAppAppearedTime - this.mPrevAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, requestDataUsage(0, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_SECOND_PACKAGE_WIFI_USAGE, requestDataUsage(1, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_STATE, getCurrentState().getName());
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_RSSI, this.mCurrentRssi);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_LINK_SPEED, this.mCurrentLinkSpeed);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_POOR_DETECTION_ENABLED, this.mPoorNetworkDetectionEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_QC_UI_ENABLED, this.mUIEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_WFQC, SnsBigDataWFQC.KEY_QC_AGGRESSIVE_MODE_ENABLED, this.mAggressiveModeEnabled ? 1 : 0);
            this.mSnsBigDataManager.insertLog(SnsBigDataManager.FEATURE_WFQC);
            this.mSnsBigDataManager.clearFeature(SnsBigDataManager.FEATURE_WFQC);
        } else {
            toString = "**" + toString;
        }
        this.mBigDataQualityCheckLoggingCount++;
        if (DBG) {
            Log.i(TAG, toString);
        }
        if (!doNotReset) {
            initCurrentQcFailRecord();
        }
    }

    private void initCurrentQcFailRecord() {
        if (DBG) {
            Log.d(TAG, "initCurrentQcFailRecord");
        }
        QcFailHistory qcFailHistory = this.mInvalidationFailHistory;
        qcFailHistory.qcType = -1;
        qcFailHistory.qcStep = -1;
        qcFailHistory.qcTrigger = -1;
        qcFailHistory.error = -1;
        qcFailHistory.line = -1;
    }

    /* access modifiers changed from: private */
    public void setBigDataValidationChanged() {
        long bootingElapsedTime = SystemClock.elapsedRealtime();
        if (isValidState()) {
            if (SMARTCM_DBG) {
                Log.d(TAG, "BigData Validation changed, Valid > Invalid");
            }
            this.mValidatedTime = (int) ((SystemClock.elapsedRealtime() - this.mValidStartTime) / 1000);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_BOOTING_ELAPSED_TIME, bootingElapsedTime);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_TOP_PACKAGE, this.mUsageStatsPackageName);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_TOP_PACKAGE_DURATION, convertMiliSecondToSecond(System.currentTimeMillis() - this.mFrontAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE, requestDataUsage(0, this.mUsageStatsUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_TOP_PACKAGE_WIFI_USAGE, requestDataUsage(1, this.mUsageStatsUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_SECOND_PACKAGE, this.mPreviousPackageName);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_SECOND_PACKAGE_DURATION, convertMiliSecondToSecond(this.mFrontAppAppearedTime - this.mPrevAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, requestDataUsage(0, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_SECOND_PACKAGE_WIFI_USAGE, requestDataUsage(1, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_STATE, getCurrentState().getName());
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_RSSI, this.mCurrentRssi);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_LINK_SPEED, this.mCurrentLinkSpeed);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_POOR_DETECTION_ENABLED, this.mPoorNetworkDetectionEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_QC_UI_ENABLED, this.mUIEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_QC_AGGRESSIVE_MODE_ENABLED, this.mAggressiveModeEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_SNS_GOOD_LINK_TARGET_RSSI, this.mCurrentBssid.mGoodLinkTargetRssi);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_SNS_CONNECTED_STAY_TIME, this.mValidatedTime);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSVI, SnsBigDataSSVI.KEY_AP_OUI, this.mApOui);
            this.mSnsBigDataManager.insertLog(SnsBigDataManager.FEATURE_SSVI);
            this.mSnsBigDataManager.clearFeature(SnsBigDataManager.FEATURE_SSVI);
            this.mInvalidStartTime = SystemClock.elapsedRealtime();
        } else {
            if (SMARTCM_DBG) {
                Log.d(TAG, "BigData Validation Switch, Invalid > Valid");
            }
            this.mInvalidatedTime = (int) ((SystemClock.elapsedRealtime() - this.mInvalidStartTime) / 1000);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_BOOTING_ELAPSED_TIME, bootingElapsedTime);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_TOP_PACKAGE, this.mUsageStatsPackageName);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_TOP_PACKAGE_DURATION, convertMiliSecondToSecond(System.currentTimeMillis() - this.mFrontAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE, requestDataUsage(0, this.mUsageStatsUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_TOP_PACKAGE_WIFI_USAGE, requestDataUsage(1, this.mUsageStatsUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_SECOND_PACKAGE, this.mPreviousPackageName);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_SECOND_PACKAGE_DURATION, convertMiliSecondToSecond(this.mFrontAppAppearedTime - this.mPrevAppAppearedTime, true));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, requestDataUsage(0, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_SECOND_PACKAGE_WIFI_USAGE, requestDataUsage(1, this.mPreviousUid));
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_STATE, getCurrentState().getName());
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_RSSI, this.mCurrentRssi);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_LINK_SPEED, this.mCurrentLinkSpeed);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_POOR_DETECTION_ENABLED, this.mPoorNetworkDetectionEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_QC_UI_ENABLED, this.mUIEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_QC_AGGRESSIVE_MODE_ENABLED, this.mAggressiveModeEnabled ? 1 : 0);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_SNS_GOOD_LINK_TARGET_RSSI, this.mCurrentBssid.mGoodLinkTargetRssi);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_SNS_L2_CONNECTED_STAY_TIME, this.mInvalidatedTime);
            this.mSnsBigDataManager.addOrUpdateFeatureValue(SnsBigDataManager.FEATURE_SSIV, SnsBigDataSSIV.KEY_AP_OUI, this.mApOui);
            this.mSnsBigDataManager.insertLog(SnsBigDataManager.FEATURE_SSIV);
            this.mSnsBigDataManager.clearFeature(SnsBigDataManager.FEATURE_SSIV);
            this.mValidStartTime = SystemClock.elapsedRealtime();
        }
        this.mValidatedTime = 0;
        this.mInvalidatedTime = 0;
    }

    /* access modifiers changed from: private */
    public void setBigDataQCandNS(boolean pass) {
        setBigDataQualityCheck(pass, true);
        setBigDataValidationChanged();
        initCurrentQcFailRecord();
    }

    private int convertMiliSecondToSecond(long miliSecond, boolean aDayLimit) {
        int resultValue = Long.valueOf(miliSecond / 1000).intValue();
        if (!aDayLimit || resultValue <= 86400) {
            return resultValue;
        }
        return 86400;
    }

    /* access modifiers changed from: private */
    public boolean isIwcModeEnabled() {
        int i = this.mIwcCurrentQai;
        if (i == 1 || i == 2 || !this.mPoorNetworkDetectionEnabled) {
            return false;
        }
        if (i != -1) {
            return true;
        }
        return false;
    }

    public void setIWCMonitorAsyncChannel(Handler dst) {
        if (this.mIWCChannel == null) {
            if (DBG) {
                Log.i(TAG, "New mIWCChannel created");
            }
            this.mIWCChannel = new AsyncChannel();
        }
        this.mIWCChannel.connectSync(this.mContext, getHandler(), dst);
        if (DBG) {
            Log.i(TAG, "mIWCChannel connected");
        }
    }

    public void sendQCResultToWCM(Message msg) {
        boolean isNotRequestedFromWCM = false;
        if (!(msg == null || msg.getData() == null)) {
            isNotRequestedFromWCM = msg.what == MESSAGE_NOT_TRIGGERED_FROM_WCM;
            boolean isQCResultValid = msg.getData().getBoolean("valid");
            boolean captivePortalDetected = msg.getData().getBoolean("captivePortalDetected");
            StringBuilder sb = new StringBuilder();
            sb.append(isNotRequestedFromWCM ? " [isNotRequestedFromWCM] " : "");
            sb.append("QC Result = ");
            sb.append(isQCResultValid);
            sb.append(", captivePortalDetected: ");
            sb.append(captivePortalDetected);
            Log.i(TAG, sb.toString());
            if (!this.mClientModeImpl.isConnected()) {
                Log.d(TAG, "Disconnected. Do not update BssidQosMap.");
            } else if (!captivePortalDetected) {
                sendMessage(CMD_UPDATE_CURRENT_BSSID_ON_QC_RESULT, isQCResultValid);
            }
        }
        if (!isNotRequestedFromWCM) {
            if (!(msg == null || msg.getData() == null || this.mWCMQCResult == null)) {
                boolean isQCResultValid2 = msg.getData().getBoolean("valid");
                if (this.mCheckRoamedNetwork) {
                    if (isQCResultValid2) {
                        sendMessage(VALIDATED_DETECTED_AGAIN);
                    } else {
                        sendMessage(INVALIDATED_DETECTED_AGAIN);
                    }
                }
            }
            Message message = this.mWCMQCResult;
            if (message != null) {
                message.recycle();
                this.mWCMQCResult = null;
            }
            removeMessages(QC_RESULT_NOT_RECEIVED);
        }
    }

    /* access modifiers changed from: private */
    public void setValidationBlock(boolean block) {
        if (!this.mConnectivityManager.getMultiNetwork()) {
            if (DBG) {
                Log.d(TAG, "validationBlock : " + block);
            }
            this.mValidationBlock = block;
            getCm().setWifiValidationBlock(block);
            sendMessage(CONNECTIVITY_VALIDATION_BLOCK, block);
        }
    }

    /* access modifiers changed from: private */
    public void enableValidationCheck() {
        if (DBG) {
            Log.d(TAG, "ValidationCheckMode : " + this.mValidationCheckMode + ", ValidationCheckCount : " + this.mValidationCheckCount + ", ValidationBlock : " + this.mValidationBlock);
        }
        if (!this.mValidationCheckMode || this.mValidationCheckCount <= 0) {
            if (DBG) {
                Log.d(TAG, "Validation Check enabled.");
            }
            this.mValidationResultCount = 0;
            this.mValidationCheckCount = 0;
            this.mValidationCheckMode = true;
            this.mValidationCheckTime = VALIDATION_CHECK_COUNT;
            Message message = this.mWCMQCResult;
            if (message != null) {
                message.recycle();
                this.mWCMQCResult = null;
            }
            removeMessages(QC_RESULT_NOT_RECEIVED);
            boolean queried = reportNetworkConnectivityToNM(true, 5, 20);
            if (!this.mValidationBlock) {
                return;
            }
            if (queried) {
                this.mValidationCheckCount = 1;
                if (DBG) {
                    Log.d(TAG, "mValidationCheckCount : " + this.mValidationCheckCount);
                }
                sendMessageDelayed(VALIDATION_CHECK_FORCE, (long) (this.mValidationCheckTime * 1000));
                return;
            }
            if (DBG) {
                Log.d(TAG, "Starting to check VALIDATION_CHECK_FORCE is delayed.");
            }
            this.mValidationCheckTime = VALIDATION_CHECK_COUNT * 2;
            sendMessageDelayed(VALIDATION_CHECK_FORCE, 10000);
        } else if (DBG) {
            Log.d(TAG, "Validation Check was already enabled.");
        }
    }

    public void setValidationCheckStart() {
        if (!this.mConnectivityManager.getMultiNetwork()) {
            if (DBG) {
                Log.d(TAG, "request to check validation from CS");
            }
            enableValidationCheck();
            this.mValidationCheckEnabledTime = SystemClock.elapsedRealtime();
        }
    }

    public void sendValidationCheckModeResult(boolean valid) {
        if (DBG) {
            Log.d(TAG, "sendValidationCheckModeResult : " + valid);
        }
        sendMessage(CONNECTIVITY_VALIDATION_RESULT, valid);
    }

    private WcmConnectivityPacketTracker createPacketTracker(InterfaceParams mInterfaceParams, LocalLog mLog) {
        try {
            return new WcmConnectivityPacketTracker(getHandler(), mInterfaceParams, mLog);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get ConnectivityPacketTracker object: " + e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void startPacketTracker() {
        if (this.mPacketTrackerForWlan0 == null) {
            sPktLogsWlan.putIfAbsent(INTERFACENAMEOFWLAN, new LocalLog(500));
            this.mConnectivityPacketLogForWlan0 = sPktLogsWlan.get(INTERFACENAMEOFWLAN);
            this.mPacketTrackerForWlan0 = createPacketTracker(InterfaceParams.getByName(INTERFACENAMEOFWLAN), this.mConnectivityPacketLogForWlan0);
            if (this.mPacketTrackerForWlan0 != null) {
                Log.d(TAG, "mPacketTrackerForwlan0 start");
                try {
                    this.mPacketTrackerForWlan0.start(INTERFACENAMEOFWLAN);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Failed to start tracking interface : " + e);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void stopPacketTracker() {
        if (this.mPacketTrackerForWlan0 != null) {
            Log.d(TAG, "mPacketTrackerForwlan0 stop");
            this.mPacketTrackerForWlan0.stop();
            this.mPacketTrackerForWlan0 = null;
        }
    }

    /* access modifiers changed from: private */
    public void setRoamEventToNM(int enable) {
        Settings.Global.putInt(this.mContentResolver, "wifi_wcm_event_roam_complete", enable);
    }

    /* access modifiers changed from: private */
    public void setDnsResultHistory(String s) {
        String currentTime = "" + (System.currentTimeMillis() / 1000);
        try {
            currentTime = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        } catch (RuntimeException e) {
        }
        Network network = this.mNetwork;
        int netId = network != null ? network.netId : -1;
        synchronized (lock) {
            String[] strArr = this.summaryDnsResults;
            int i = this.incrDnsResults;
            this.incrDnsResults = i + 1;
            strArr[i % 50] = currentTime + " - " + netId + " - " + s;
        }
    }
}
