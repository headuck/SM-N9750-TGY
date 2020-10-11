package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.location.Address;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.IpConfiguration;
import android.net.KeepalivePacketData;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.NattKeepalivePacketData;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkSpecifier;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.SocketKeepalive;
import android.net.StaticIpConfiguration;
import android.net.TcpKeepalivePacketData;
import android.net.ip.IIpClient;
import android.net.ip.IpClientCallbacks;
import android.net.ip.IpClientManager;
import android.net.shared.ProvisioningConfiguration;
import android.net.util.InterfaceParams;
import android.net.wifi.INetworkRequestMatchCallback;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkAgentSpecifier;
import android.net.wifi.WifiSsid;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.p2p.IWifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.sec.enterprise.WifiPolicy;
import android.sec.enterprise.WifiPolicyCache;
import android.sec.enterprise.certificate.CertificatePolicy;
import android.system.OsConstants;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.StatsLog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.WifiController;
import com.android.server.wifi.WifiGeofenceManager;
import com.android.server.wifi.WifiMulticastLockManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSWanMetricsElement;
import com.android.server.wifi.hotspot2.anqp.VenueNameElement;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.StringUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.feature.SemFloatingFeature;
import com.samsung.android.knox.custom.CustomDeviceManagerProxy;
import com.samsung.android.location.SemLocationListener;
import com.samsung.android.location.SemLocationManager;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.ArpPeer;
import com.samsung.android.server.wifi.SemSarManager;
import com.samsung.android.server.wifi.SemWifiFrameworkUxUtils;
import com.samsung.android.server.wifi.SemWifiHiddenNetworkTracker;
import com.samsung.android.server.wifi.UnstableApController;
import com.samsung.android.server.wifi.WifiB2BConfigurationPolicy;
import com.samsung.android.server.wifi.WifiDelayDisconnect;
import com.samsung.android.server.wifi.WifiMobileDeviceManager;
import com.samsung.android.server.wifi.WifiRoamingAssistant;
import com.samsung.android.server.wifi.WlanTestHelper;
import com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import com.samsung.android.server.wifi.dqa.ReportUtil;
import com.samsung.android.server.wifi.dqa.SemWifiIssueDetector;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsScanResult;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsWifiSsid;
import com.samsung.android.server.wifi.softap.DhcpPacket;
import com.samsung.android.server.wifi.softap.SemConnectivityPacketTracker;
import com.samsung.android.server.wifi.softap.SemWifiApBroadcastReceiver;
import com.sec.android.app.C0852CscFeatureTagCommon;
import com.sec.android.app.CscFeatureTagCOMMON;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.ksoap2.SoapEnvelope;

public class ClientModeImpl extends StateMachine {
    private static final String ACTION_AP_LOCATION_PASSIVE_REQUEST = "com.android.server.wifi.AP_LOCATION_PASSIVE_REQUEST";
    private static final int ACTIVE_REQUEST_LOCATION = 1;
    static final int BASE = 131072;
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    private static final int CMD_24HOURS_PASSED_AFTER_BOOT = 131579;
    static final int CMD_ACCEPT_UNVALIDATED = 131225;
    static final int CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF = 131281;
    static final int CMD_ADD_OR_UPDATE_NETWORK = 131124;
    static final int CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG = 131178;
    static final int CMD_ASSOCIATED_BSSID = 131219;
    private static final int CMD_AUTO_CONNECT_CARRIER_AP_ENABLED = 131316;
    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE = 131103;
    static final int CMD_BOOT_COMPLETED = 131206;
    private static final int CMD_CHECK_ARP_RESULT = 131622;
    static final int CMD_CONFIG_ND_OFFLOAD = 131276;
    static final int CMD_DIAGS_CONNECT_TIMEOUT = 131324;
    static final int CMD_DISABLE_EPHEMERAL_NETWORK = 131170;
    static final int CMD_DISCONNECT = 131145;
    static final int CMD_DISCONNECTING_WATCHDOG_TIMER = 131168;
    static final int CMD_ENABLE_NETWORK = 131126;
    static final int CMD_ENABLE_RSSI_POLL = 131154;
    static final int CMD_ENABLE_TDLS = 131164;
    static final int CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER = 131238;
    static final int CMD_FORCINGLY_ENABLE_ALL_NETWORKS = 131402;
    static final int CMD_GET_ALL_MATCHING_FQDNS_FOR_SCAN_RESULTS = 131240;
    private static final int CMD_GET_A_CONFIGURED_NETWORK = 131581;
    static final int CMD_GET_CONFIGURED_NETWORKS = 131131;
    static final int CMD_GET_LINK_LAYER_STATS = 131135;
    static final int CMD_GET_MATCHING_OSU_PROVIDERS = 131181;
    static final int CMD_GET_MATCHING_PASSPOINT_CONFIGS_FOR_OSU_PROVIDERS = 131182;
    static final int CMD_GET_PASSPOINT_CONFIGS = 131180;
    static final int CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS = 131134;
    static final int CMD_GET_SUPPORTED_FEATURES = 131133;
    static final int CMD_GET_WIFI_CONFIGS_FOR_PASSPOINT_PROFILES = 131184;
    private static final int CMD_IMS_CALL_ESTABLISHED = 131315;
    static final int CMD_INITIALIZE = 131207;
    static final int CMD_INSTALL_PACKET_FILTER = 131274;
    static final int CMD_IPV4_PROVISIONING_FAILURE = 131273;
    static final int CMD_IPV4_PROVISIONING_SUCCESS = 131272;
    static final int CMD_IP_CONFIGURATION_LOST = 131211;
    static final int CMD_IP_CONFIGURATION_SUCCESSFUL = 131210;
    static final int CMD_IP_REACHABILITY_LOST = 131221;
    static final int CMD_MATCH_PROVIDER_NETWORK = 131177;
    static final int CMD_NETWORK_STATUS = 131220;
    static final int CMD_ONESHOT_RSSI_POLL = 131156;
    private static final int CMD_POST_DHCP_ACTION = 131329;
    @VisibleForTesting
    static final int CMD_PRE_DHCP_ACTION = 131327;
    private static final int CMD_PRE_DHCP_ACTION_COMPLETE = 131328;
    static final int CMD_QUERY_OSU_ICON = 131176;
    static final int CMD_READ_PACKET_FILTER = 131280;
    static final int CMD_REASSOCIATE = 131147;
    static final int CMD_RECONNECT = 131146;
    private static final int CMD_RELOAD_CONFIG_STORE_FILE = 131583;
    static final int CMD_REMOVE_APP_CONFIGURATIONS = 131169;
    static final int CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF = 131282;
    static final int CMD_REMOVE_NETWORK = 131125;
    static final int CMD_REMOVE_PASSPOINT_CONFIG = 131179;
    static final int CMD_REMOVE_USER_CONFIGURATIONS = 131224;
    private static final int CMD_REPLACE_PUBLIC_DNS = 131286;
    static final int CMD_RESET_SIM_NETWORKS = 131173;
    static final int CMD_RESET_SUPPLICANT_STATE = 131183;
    static final int CMD_ROAM_WATCHDOG_TIMER = 131166;
    static final int CMD_RSSI_POLL = 131155;
    static final int CMD_RSSI_THRESHOLD_BREACHED = 131236;
    public static final int CMD_SCAN_RESULT_AVAILABLE = 131584;
    static final int CMD_SCREEN_STATE_CHANGED = 131167;
    private static final int CMD_SEC_API = 131574;
    private static final int CMD_SEC_API_ASYNC = 131573;
    public static final int CMD_SEC_LOGGING = 131576;
    private static final int CMD_SEC_STRING_API = 131575;
    private static final int CMD_SEND_ARP = 131623;
    public static final int CMD_SEND_DHCP_RELEASE = 131283;
    static final int CMD_SET_ADPS_MODE = 131383;
    static final int CMD_SET_FALLBACK_PACKET_FILTERING = 131275;
    static final int CMD_SET_HIGH_PERF_MODE = 131149;
    static final int CMD_SET_OPERATIONAL_MODE = 131144;
    static final int CMD_SET_SUSPEND_OPT_ENABLED = 131158;
    private static final int CMD_SHOW_TOAST_MSG = 131582;
    static final int CMD_START_CONNECT = 131215;
    static final int CMD_START_IP_PACKET_OFFLOAD = 131232;
    static final int CMD_START_ROAM = 131217;
    static final int CMD_START_RSSI_MONITORING_OFFLOAD = 131234;
    private static final int CMD_START_SUBSCRIPTION_PROVISIONING = 131326;
    static final int CMD_STOP_IP_PACKET_OFFLOAD = 131233;
    static final int CMD_STOP_RSSI_MONITORING_OFFLOAD = 131235;
    static final int CMD_TARGET_BSSID = 131213;
    static final int CMD_THREE_TIMES_SCAN_IN_IDLE = 131381;
    static final int CMD_UNWANTED_NETWORK = 131216;
    private static final int CMD_UPDATE_CONFIG_LOCATION = 131332;
    static final int CMD_UPDATE_LINKPROPERTIES = 131212;
    static final int CMD_USER_STOP = 131279;
    static final int CMD_USER_SWITCH = 131277;
    static final int CMD_USER_UNLOCK = 131278;
    /* access modifiers changed from: private */
    public static final String CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    /* access modifiers changed from: private */
    public static final String CONFIG_SECURE_SVC_INTEGRATION = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSECURESVCINTEGRATION);
    public static final int CONNECT_MODE = 1;
    /* access modifiers changed from: private */
    public static final String CSC_CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    /* access modifiers changed from: private */
    public static final boolean CSC_SUPPORT_5G_ANT_SHARE = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORT5GANTSHARE);
    private static final boolean CSC_WIFI_ERRORCODE = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_ENABLEDETAILEAPERRORCODESANDSTATE);
    /* access modifiers changed from: private */
    public static final boolean CSC_WIFI_SUPPORT_VZW_EAP_AKA = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTEAPAKA);
    private static final String DATA_LIMIT_INTENT = "com.android.intent.action.DATAUSAGE_REACH_TO_LIMIT";
    /* access modifiers changed from: private */
    public static boolean DBG = false;
    /* access modifiers changed from: private */
    public static final boolean DBG_PRODUCT_DEV = Debug.semIsProductDev();
    private static final int DEFAULT_POLL_RSSI_INTERVAL_MSECS = 3000;
    @VisibleForTesting
    public static final long DIAGS_CONNECT_TIMEOUT_MILLIS = 60000;
    public static final int DISABLED_MODE = 4;
    static final int DISCONNECTING_GUARD_TIMER_MSEC = 5000;
    private static final int DISCONNECT_REASON_ADD_OR_UPDATE_NETWORK = 21;
    public static final int DISCONNECT_REASON_API = 2;
    private static final int DISCONNECT_REASON_ASSOC_REJECTED = 12;
    private static final int DISCONNECT_REASON_AUTH_FAIL = 13;
    private static final int DISCONNECT_REASON_DHCP_FAIL = 1;
    private static final int DISCONNECT_REASON_DHCP_FAIL_WITH_IPCLIENT_ISSUE = 24;
    private static final int DISCONNECT_REASON_DISABLE_NETWORK = 19;
    private static final int DISCONNECT_REASON_DISCONNECT_BY_MDM = 15;
    private static final int DISCONNECT_REASON_DISCONNECT_BY_P2P = 16;
    public static final int DISCONNECT_REASON_FACTORY_RESET = 23;
    private static final int DISCONNECT_REASON_NO_INTERNET = 10;
    private static final int DISCONNECT_REASON_NO_NETWORK = 22;
    private static final int DISCONNECT_REASON_REMOVE_NETWORK = 18;
    private static final int DISCONNECT_REASON_ROAM_FAIL = 6;
    private static final int DISCONNECT_REASON_ROAM_TIMEOUT = 5;
    private static final int DISCONNECT_REASON_SIM_REMOVED = 4;
    private static final int DISCONNECT_REASON_START_CONNECT = 3;
    private static final int DISCONNECT_REASON_TURN_OFF_WIFI = 17;
    private static final int DISCONNECT_REASON_UNKNOWN = 0;
    private static final int DISCONNECT_REASON_UNWANTED = 8;
    private static final int DISCONNECT_REASON_UNWANTED_BY_USER = 9;
    private static final int DISCONNECT_REASON_USER_SWITCH = 20;
    public static final int EAP_EVENT_ANONYMOUS_IDENTITY_UPDATED = 1;
    public static final int EAP_EVENT_DEAUTH_8021X_AUTH_FAILED = 2;
    public static final int EAP_EVENT_EAP_FAILURE = 3;
    public static final int EAP_EVENT_ERROR_MESSAGE = 4;
    public static final int EAP_EVENT_LOGGING = 5;
    public static final int EAP_EVENT_NOTIFICATION = 7;
    public static final int EAP_EVENT_NO_CREDENTIALS = 6;
    public static final int EAP_EVENT_SUCCESS = 8;
    public static final int EAP_EVENT_TLS_ALERT = 9;
    public static final int EAP_EVENT_TLS_CERT_ERROR = 10;
    public static final int EAP_EVENT_TLS_HANDSHAKE_FAIL = 11;
    public static final int EAP_NOTIFICATION_KT_WIFI_AUTH_FAIL = 5;
    public static final int EAP_NOTIFICATION_KT_WIFI_INVALID_IDPW = 4;
    public static final int EAP_NOTIFICATION_KT_WIFI_INVALID_USIM = 1;
    public static final int EAP_NOTIFICATION_KT_WIFI_NO_RESPONSE = 6;
    public static final int EAP_NOTIFICATION_KT_WIFI_NO_USIM = 2;
    public static final int EAP_NOTIFICATION_KT_WIFI_SUCCESS = 0;
    public static final int EAP_NOTIFICATION_KT_WIFI_WEP_PSK_INVALID_KEY = 3;
    public static final int EAP_NOTIFICATION_NO_NOTIFICATION_INFORMATION = 987654321;
    private static final boolean ENABLE_SUPPORT_ADPS = SemFloatingFeature.getInstance().getBoolean("SEC_FLOATING_FEATURE_WIFI_SUPPORT_ADPS");
    private static final boolean ENABLE_SUPPORT_QOSCONTROL = SemFloatingFeature.getInstance().getBoolean("SEC_FLOATING_FEATURE_WLAN_SUPPORT_QOS_CONTROL");
    /* access modifiers changed from: private */
    public static final boolean ENBLE_WLAN_CONFIG_ANALYTICS = (Integer.parseInt("1") == 1);
    private static final String EXTRA_OSU_ICON_QUERY_BSSID = "BSSID";
    private static final String EXTRA_OSU_ICON_QUERY_FILENAME = "FILENAME";
    private static final String EXTRA_OSU_PROVIDER = "OsuProvider";
    private static final String EXTRA_PACKAGE_NAME = "PackageName";
    private static final String EXTRA_PASSPOINT_CONFIGURATION = "PasspointConfiguration";
    private static final String EXTRA_UID = "uid";
    private static final int FAILURE = -1;
    private static final String GOOGLE_OUI = "DA-A1-19";
    private static final String INTERFACENAMEOFWLAN = "wlan0";
    /* access modifiers changed from: private */
    public static double INVALID_LATITUDE_LONGITUDE = 1000.0d;
    private static final int IPCLIENT_TIMEOUT_MS = 10000;
    private static final int ISSUE_TRACKER_SYSDUMP_DISC = 2;
    private static final int ISSUE_TRACKER_SYSDUMP_HANG = 0;
    private static final int ISSUE_TRACKER_SYSDUMP_UNWANTED = 1;
    @VisibleForTesting
    public static final int LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS = 30000;
    private static final int LINK_FLAPPING_DEBOUNCE_MSEC = 4000;
    private static final String LOGD_LEVEL_DEBUG = "D";
    private static final String LOGD_LEVEL_VERBOSE = "V";
    private static final int MAX_PACKET_RECORDS = 500;
    private static final int MAX_SCAN_RESULTS_EVENT_COUNT_IN_IDLE = 2;
    private static final int MESSAGE_HANDLING_STATUS_DEFERRED = -4;
    private static final int MESSAGE_HANDLING_STATUS_DISCARD = -5;
    private static final int MESSAGE_HANDLING_STATUS_FAIL = -2;
    private static final int MESSAGE_HANDLING_STATUS_HANDLING_ERROR = -7;
    private static final int MESSAGE_HANDLING_STATUS_LOOPED = -6;
    private static final int MESSAGE_HANDLING_STATUS_OBSOLETE = -3;
    private static final int MESSAGE_HANDLING_STATUS_OK = 1;
    private static final int MESSAGE_HANDLING_STATUS_PROCESSED = 2;
    private static final int MESSAGE_HANDLING_STATUS_REFUSED = -1;
    private static final int MESSAGE_HANDLING_STATUS_UNKNOWN = 0;
    private static final int[] MHS_PRIVATE_NETWORK_MASK = {2861248, 660652};
    private static final int NCHO_VER1_STATE_BACKUP = 1;
    private static final int NCHO_VER1_STATE_ERROR = -1;
    private static final int NCHO_VER1_STATE_INIT = 0;
    private static final int NCHO_VER2_STATE_DISABLED = 0;
    private static final int NCHO_VER2_STATE_ENABLED = 1;
    private static final int NCHO_VER2_STATE_ERROR = -1;
    private static final int NCHO_VERSION_1_0 = 1;
    private static final int NCHO_VERSION_2_0 = 2;
    private static final int NCHO_VERSION_ERROR = 0;
    private static final int NCHO_VERSION_UNKNOWN = -1;
    private static final String NETWORKTYPE = "WIFI";
    private static final int NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN = 2;
    private static final int NETWORK_STATUS_UNWANTED_DISCONNECT = 0;
    private static final int NETWORK_STATUS_UNWANTED_VALIDATION_FAILED = 1;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_NORMAL = 1000;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE = 3000;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE_LOW_MEMORY = 200;
    private static final int ONE_HOUR_MILLI = 3600000;
    private static final int ROAMING_ARP_INTERVAL_MS = 500;
    private static final int ROAMING_ARP_START_DELAY_MS = 100;
    private static final int ROAMING_ARP_TIMEOUT_MS = 2000;
    private static final int ROAM_DHCP_DEFAULT = 0;
    private static final int ROAM_DHCP_RESTART = 1;
    private static final int ROAM_DHCP_SKIP = 2;
    static final int ROAM_GUARD_TIMER_MSEC = 15000;
    private static final int RSSI_POLL_ENABLE_DURING_LCD_OFF_FOR_IMS = 1;
    private static final int RSSI_POLL_ENABLE_DURING_LCD_OFF_FOR_SWITCHBOARD = 2;
    public static final int SCAN_ONLY_MODE = 2;
    public static final int SCAN_ONLY_WITH_WIFI_OFF_MODE = 3;
    static final int SECURITY_EAP = 3;
    static final int SECURITY_NONE = 0;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_SAE = 4;
    static final int SECURITY_WEP = 1;
    private static final int SUCCESS = 1;
    public static final String SUPPLICANT_BSSID_ANY = "any";
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;
    private static final int SUPPLICANT_RESTART_TRIES = 5;
    private static final int SUSPEND_DUE_TO_DHCP = 1;
    private static final int SUSPEND_DUE_TO_HIGH_PERF = 2;
    private static final int SUSPEND_DUE_TO_SCREEN = 4;
    private static final String SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL = "log.tag.WifiHAL";
    private static final String TAG = "WifiClientModeImpl";
    public static final WorkSource WIFI_WORK_SOURCE = new WorkSource(1010);
    private static final int WLAN_ADVANCED_DEBUG_DISC = 4;
    private static final int WLAN_ADVANCED_DEBUG_ELE_DEBUG = 32;
    private static final int WLAN_ADVANCED_DEBUG_PKT = 1;
    private static final int WLAN_ADVANCED_DEBUG_RESET = 0;
    private static final int WLAN_ADVANCED_DEBUG_STATE = 64;
    private static final int WLAN_ADVANCED_DEBUG_UDI = 8;
    private static final int WLAN_ADVANCED_DEBUG_UNWANTED = 2;
    private static final int WLAN_ADVANCED_DEBUG_UNWANTED_PANIC = 16;
    private static byte mCellularCapaState = 0;
    /* access modifiers changed from: private */
    public static byte[] mCellularCellId = new byte[2];
    /* access modifiers changed from: private */
    public static byte mCellularSignalLevel = 0;
    /* access modifiers changed from: private */
    public static boolean mChanged = false;
    /* access modifiers changed from: private */
    public static boolean mIsPolicyMobileData = false;
    /* access modifiers changed from: private */
    public static boolean mIssueTrackerOn = false;
    private static int mLteuEnable = 0;
    private static int mLteuState = 0;
    private static byte mNetworktype = 0;
    /* access modifiers changed from: private */
    public static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    /* access modifiers changed from: private */
    public static int mPhoneStateEvent = 590096;
    private static int mRssiPollingScreenOffEnabled = 0;
    /* access modifiers changed from: private */
    public static boolean mScellEnter = false;
    /* access modifiers changed from: private */
    public static int mWlanAdvancedDebugState = 0;
    private static final SparseArray<String> sGetWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private static final Class[] sMessageClasses = {AsyncChannel.class, ClientModeImpl.class};
    /* access modifiers changed from: private */
    public static final ConcurrentHashMap<String, LocalLog> sPktLogsMhs = new ConcurrentHashMap<>();
    /* access modifiers changed from: private */
    public static final ConcurrentHashMap<String, LocalLog> sPktLogsWlan = new ConcurrentHashMap<>();
    private static int sScanAlarmIntentCount = 0;
    private final int LTEU_MOBILEHOTSPOT_5GHZ_ENABLED = 1;
    private final int LTEU_P2P_5GHZ_CONNECTED = 2;
    private final int LTEU_STA_5GHZ_CONNECTED = 4;
    public final List<String> MULTINETWORK_ALLOWING_SYSTEM_PACKAGE_LIST = Arrays.asList(new String[]{"com.samsung.android.oneconnect", "com.samsung.android.app.mirrorlink", "com.google.android.gms", "com.google.android.projection.gearhead"});
    public final List<String> MULTINETWORK_EXCEPTION_PACKAGE_LIST = Arrays.asList(new String[]{WifiConfigManager.SYSUI_PACKAGE_NAME, "android.uid.systemui", "com.samsung.android.app.aodservice", "com.sec.android.cover.ledcover", "com.samsung.android.app.routines", WifiConfigManager.SYSUI_PACKAGE_NAME, "com.samsung.desktopsystemui", "com.samsung.android.gesture.MotionRecognitionService", "com.android.systemui.sensor.PickupController", "com.samsung.uready.agent"});
    public final List<String> NETWORKSETTINGS_PERMISSION_EXCEPTION_PACKAGE_LIST = Arrays.asList(new String[]{"com.samsung.android.oneconnect", "sdet.pack", "sdet.pack.channel"});
    private CustomDeviceManagerProxy customDeviceManager = null;
    public boolean isDhcpStartSent = false;
    private int laaActiveState = -1;
    /* access modifiers changed from: private */
    public int laaEnterState = -1;
    /* access modifiers changed from: private */
    public String mApInterfaceName = "Not_use";
    private final BackupManagerProxy mBackupManagerProxy;
    private final IBatteryStats mBatteryStats;
    /* access modifiers changed from: private */
    public WifiBigDataLogManager mBigDataManager;
    private boolean mBlockFccChannelCmd = false;
    /* access modifiers changed from: private */
    public boolean mBluetoothConnectionActive = false;
    private final BuildProperties mBuildProperties;
    private int mCandidateRssiThreshold24G = -70;
    private int mCandidateRssiThreshold5G = -70;
    private int mCandidateRssiThreshold6G = -70;
    private ClientModeManager.Listener mClientModeCallback = null;
    /* access modifiers changed from: private */
    public final Clock mClock;
    /* access modifiers changed from: private */
    public ConnectivityManager mCm;
    /* access modifiers changed from: private */
    public boolean mConcurrentEnabled;
    private State mConnectModeState = new ConnectModeState();
    /* access modifiers changed from: private */
    public int mConnectedApInternalType = 0;
    /* access modifiers changed from: private */
    public boolean mConnectedMacRandomzationSupported;
    /* access modifiers changed from: private */
    public State mConnectedState = new ConnectedState();
    /* access modifiers changed from: private */
    public LocalLog mConnectivityPacketLogForHotspot;
    /* access modifiers changed from: private */
    public LocalLog mConnectivityPacketLogForWlan0;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public final WifiCountryCode mCountryCode;
    /* access modifiers changed from: private */
    public int mCurrentP2pFreq;
    private int mDefaultRoamDelta = 10;
    private int mDefaultRoamScanPeriod = 10;
    private int mDefaultRoamTrigger = -75;
    private State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public WifiDelayDisconnect mDelayDisconnect;
    private DhcpResults mDhcpResults;
    private final Object mDhcpResultsLock = new Object();
    /* access modifiers changed from: private */
    public boolean mDidBlackListBSSID = false;
    /* access modifiers changed from: private */
    public State mDisconnectedState = new DisconnectedState();
    /* access modifiers changed from: private */
    public State mDisconnectingState = new DisconnectingState();
    int mDisconnectingWatchdogCount = 0;
    /* access modifiers changed from: private */
    public boolean mEnableRssiPolling = false;
    HashMap<Integer, Long> mEventCounter = new HashMap<>();
    /* access modifiers changed from: private */
    public FrameworkFacade mFacade;
    /* access modifiers changed from: private */
    public boolean mFirstTurnOn = true;
    /* access modifiers changed from: private */
    public Timer mFwLogTimer = null;
    /* access modifiers changed from: private */
    public boolean mHandleIfaceIsUp = false;
    /* access modifiers changed from: private */
    public AsyncChannel mIWCMonitorChannel = null;
    /* access modifiers changed from: private */
    public String mInterfaceName;
    /* access modifiers changed from: private */
    public volatile IpClientManager mIpClient;
    private IpClientCallbacksImpl mIpClientCallbacks;
    /* access modifiers changed from: private */
    public boolean mIpReachabilityDisconnectEnabled = false;
    /* access modifiers changed from: private */
    public boolean mIsAutoRoaming = false;
    /* access modifiers changed from: private */
    public boolean mIsBootCompleted;
    /* access modifiers changed from: private */
    public boolean mIsImsCallEstablished;
    public boolean mIsManualSelection = false;
    private boolean mIsNchoParamSet = false;
    /* access modifiers changed from: private */
    public boolean mIsP2pConnected;
    /* access modifiers changed from: private */
    public boolean mIsPasspointEnabled;
    public boolean mIsRoamNetwork = false;
    private boolean mIsRoaming = false;
    private boolean mIsRunning = false;
    /* access modifiers changed from: private */
    public boolean mIsShutdown = false;
    private boolean mIsWifiOffByAirplane = false;
    private int mIsWifiOnly = -1;
    private SemWifiIssueDetector mIssueDetector;
    private State mL2ConnectedState = new L2ConnectedState();
    /* access modifiers changed from: private */
    public String mLastBssid;
    /* access modifiers changed from: private */
    public long mLastConnectAttemptTimestamp = 0;
    /* access modifiers changed from: private */
    public int mLastConnectedNetworkId;
    /* access modifiers changed from: private */
    public long mLastConnectedTime = -1;
    /* access modifiers changed from: private */
    public long mLastDriverRoamAttempt = 0;
    /* access modifiers changed from: private */
    public int mLastEAPFailureCount = 0;
    private int mLastEAPFailureNetworkId = -1;
    private Pair<String, String> mLastL2KeyAndGroupHint = null;
    /* access modifiers changed from: private */
    public WifiLinkLayerStats mLastLinkLayerStats;
    private long mLastLinkLayerStatsUpdate = 0;
    /* access modifiers changed from: private */
    public int mLastNetworkId;
    private long mLastOntimeReportTimeStamp = 0;
    private String mLastRequestPackageNameForGeofence = null;
    private final WorkSource mLastRunningWifiUids = new WorkSource();
    private long mLastScreenStateChangeTimeStamp = 0;
    /* access modifiers changed from: private */
    public int mLastSignalLevel = -1;
    /* access modifiers changed from: private */
    public final LinkProbeManager mLinkProbeManager;
    /* access modifiers changed from: private */
    public LinkProperties mLinkProperties;
    PendingIntent mLocationPendingIntent;
    /* access modifiers changed from: private */
    public int mLocationRequestNetworkId = -1;
    private final McastLockManagerFilterController mMcastLockManagerFilterController;
    /* access modifiers changed from: private */
    public int mMessageHandlingStatus = 0;
    private byte mMobileState = 3;
    private boolean mModeChange = false;
    private int mNcho10State = 0;
    private int mNcho20State = 0;
    private int mNchoVersion = -1;
    /* access modifiers changed from: private */
    @GuardedBy({"mNetworkAgentLock"})
    public WifiNetworkAgent mNetworkAgent;
    /* access modifiers changed from: private */
    public final Object mNetworkAgentLock = new Object();
    private final NetworkCapabilities mNetworkCapabilitiesFilter = new NetworkCapabilities();
    /* access modifiers changed from: private */
    public WifiNetworkFactory mNetworkFactory;
    /* access modifiers changed from: private */
    public NetworkInfo mNetworkInfo;
    /* access modifiers changed from: private */
    public final NetworkMisc mNetworkMisc = new NetworkMisc();
    private AtomicInteger mNullMessageCounter = new AtomicInteger(0);
    /* access modifiers changed from: private */
    public State mObtainingIpState = new ObtainingIpState();
    private LinkProperties mOldLinkProperties;
    private int mOnTime = 0;
    private int mOnTimeLastReport = 0;
    private int mOnTimeScreenStateChange = 0;
    /* access modifiers changed from: private */
    public int mOperationalMode = 4;
    /* access modifiers changed from: private */
    public final AtomicBoolean mP2pConnected = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public WifiController.P2pDisableListener mP2pDisableListener = null;
    private final boolean mP2pSupported;
    /* access modifiers changed from: private */
    public SemConnectivityPacketTracker mPacketTrackerForHotspot;
    /* access modifiers changed from: private */
    public SemConnectivityPacketTracker mPacketTrackerForWlan0;
    /* access modifiers changed from: private */
    public final PasspointManager mPasspointManager;
    private int mPeriodicScanToken = 0;
    /* access modifiers changed from: private */
    public int mPersistQosTid = 0;
    /* access modifiers changed from: private */
    public int mPersistQosUid = 0;
    /* access modifiers changed from: private */
    public PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.d(ClientModeImpl.TAG, "onDataConnectionStateChanged: state =" + String.valueOf(state) + ", networkType =" + TelephonyManager.getNetworkTypeName(networkType));
            ClientModeImpl.this.handleCellularCapabilities();
        }

        public void onUserMobileDataStateChanged(boolean enabled) {
            Log.d(ClientModeImpl.TAG, "onUserMobileDataStateChanged: enabled=" + enabled);
            ClientModeImpl.this.handleCellularCapabilities();
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            byte newLevel = (byte) signalStrength.getLevel();
            if (ClientModeImpl.mCellularSignalLevel != newLevel) {
                byte unused = ClientModeImpl.mCellularSignalLevel = newLevel;
                boolean unused2 = ClientModeImpl.mChanged = true;
                Log.d(ClientModeImpl.TAG, "onSignalStrengthsChanged: mCellularSignalLevel=" + ClientModeImpl.mCellularSignalLevel);
                ClientModeImpl.this.handleCellularCapabilities();
            }
        }

        public void onCellLocationChanged(CellLocation location) {
            Log.d(ClientModeImpl.TAG, "onCellLocationChanged: CellLocation=" + location);
            byte[] curCellularCellId = new byte[2];
            if (location instanceof GsmCellLocation) {
                curCellularCellId = ClientModeImpl.this.toBytes(((GsmCellLocation) location).getCid());
            } else if (location instanceof CdmaCellLocation) {
                curCellularCellId = ClientModeImpl.this.toBytes(((CdmaCellLocation) location).getBaseStationId());
            } else {
                Log.d(ClientModeImpl.TAG, "unknown location.");
                byte unused = ClientModeImpl.mCellularSignalLevel = (byte) 0;
            }
            if (!Arrays.equals(ClientModeImpl.mCellularCellId, curCellularCellId)) {
                System.arraycopy(curCellularCellId, 0, ClientModeImpl.mCellularCellId, 0, 2);
                boolean unused2 = ClientModeImpl.mChanged = true;
                ClientModeImpl.this.handleCellularCapabilities();
            }
        }

        public void onCarrierNetworkChange(boolean active) {
            Log.d(ClientModeImpl.TAG, "onCarrierNetworkChange: active=" + active);
            ClientModeImpl.this.handleCellularCapabilities();
        }
    };
    /* access modifiers changed from: private */
    public volatile int mPollRssiIntervalMsecs = 3000;
    private final PropertyService mPropertyService;
    /* access modifiers changed from: private */
    public boolean mQosGameIsRunning = false;
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private boolean mReportedRunning = false;
    private int mRoamDhcpPolicy = 0;
    /* access modifiers changed from: private */
    public int mRoamDhcpPolicyByB2bConfig = 0;
    private int mRoamFailCount = 0;
    int mRoamWatchdogCount = 0;
    /* access modifiers changed from: private */
    public State mRoamingState = new RoamingState();
    /* access modifiers changed from: private */
    public int mRssiPollToken = 0;
    /* access modifiers changed from: private */
    public byte[] mRssiRanges;
    private int mRssiThreshold = -80;
    int mRunningBeaconCount = 0;
    private final WorkSource mRunningWifiUids = new WorkSource();
    private int mRxTime = 0;
    private int mRxTimeLastReport = 0;
    /* access modifiers changed from: private */
    public final SarManager mSarManager;
    private int mScanMode = 1;
    /* access modifiers changed from: private */
    public final ScanRequestProxy mScanRequestProxy;
    private int mScanResultsEventCounter;
    /* access modifiers changed from: private */
    public boolean mScreenOn = false;
    /* access modifiers changed from: private */
    public SemLocationListener mSemLocationListener = new SemLocationListener() {
        public void onLocationAvailable(Location[] locations) {
        }

        public void onLocationChanged(Location location, Address address) {
            Log.d(ClientModeImpl.TAG, "onLocationChanged is called");
            if (ClientModeImpl.this.mLocationRequestNetworkId == -1) {
                if (ClientModeImpl.DBG) {
                    Log.d(ClientModeImpl.TAG, "There is no config to update location");
                }
            } else if (location == null) {
                Log.d(ClientModeImpl.TAG, "onLocationChanged is called but location is null");
            } else {
                WifiConfiguration config = ClientModeImpl.this.mWifiConfigManager.getConfiguredNetwork(ClientModeImpl.this.mLocationRequestNetworkId);
                if (config == null) {
                    Log.d(ClientModeImpl.TAG, "Try to updateLocation but config is null");
                    return;
                }
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                if (ClientModeImpl.this.isValidLocation(latitude, longitude)) {
                    ClientModeImpl.this.mWifiGeofenceManager.setLatitudeAndLongitude(config, latitude, longitude);
                    int unused = ClientModeImpl.this.mLocationRequestNetworkId = -1;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public SemLocationManager mSemLocationManager;
    /* access modifiers changed from: private */
    public final SemSarManager mSemSarManager;
    /* access modifiers changed from: private */
    public SemWifiHiddenNetworkTracker mSemWifiHiddenNetworkTracker;
    /* access modifiers changed from: private */
    public WifiSettingsStore mSettingsStore;
    private long mSupplicantScanIntervalMs;
    /* access modifiers changed from: private */
    public SupplicantStateTracker mSupplicantStateTracker;
    private int mSuspendOptNeedsDisabled = 0;
    /* access modifiers changed from: private */
    public PowerManager.WakeLock mSuspendWakeLock;
    /* access modifiers changed from: private */
    public int mTargetNetworkId = -1;
    /* access modifiers changed from: private */
    public String mTargetRoamBSSID = "any";
    /* access modifiers changed from: private */
    public WifiConfiguration mTargetWifiConfiguration = null;
    private final String mTcpBufferSizes;
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public boolean mTemporarilyDisconnectWifi = false;
    private int mTxTime = 0;
    private int mTxTimeLastReport = 0;
    /* access modifiers changed from: private */
    public UnstableApController mUnstableApController;
    private UntrustedWifiNetworkFactory mUntrustedNetworkFactory;
    /* access modifiers changed from: private */
    public AtomicBoolean mUserWantsSuspendOpt = new AtomicBoolean(true);
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    private PowerManager.WakeLock mWakeLock;
    /* access modifiers changed from: private */
    public AtomicBoolean mWifiAdpsEnabled = new AtomicBoolean(false);
    private final WifiB2BConfigurationPolicy mWifiB2bConfigPolicy;
    /* access modifiers changed from: private */
    public final WifiConfigManager mWifiConfigManager;
    /* access modifiers changed from: private */
    public final WifiConnectivityManager mWifiConnectivityManager;
    /* access modifiers changed from: private */
    public final WifiDataStall mWifiDataStall;
    /* access modifiers changed from: private */
    public BaseWifiDiagnostics mWifiDiagnostics;
    /* access modifiers changed from: private */
    public final WifiGeofenceManager mWifiGeofenceManager;
    /* access modifiers changed from: private */
    public final ExtendedWifiInfo mWifiInfo;
    /* access modifiers changed from: private */
    public final WifiInjector mWifiInjector;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;
    private final WifiMonitor mWifiMonitor;
    /* access modifiers changed from: private */
    public final WifiNative mWifiNative;
    /* access modifiers changed from: private */
    public ArrayList<ClientModeChannel.WifiNetworkCallback> mWifiNetworkCallbackList = null;
    private WifiNetworkSuggestionsManager mWifiNetworkSuggestionsManager;
    /* access modifiers changed from: private */
    public AsyncChannel mWifiP2pChannel;
    /* access modifiers changed from: private */
    public final WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private WifiPolicy mWifiPolicy;
    /* access modifiers changed from: private */
    public final WifiScoreCard mWifiScoreCard;
    /* access modifiers changed from: private */
    public final WifiScoreReport mWifiScoreReport;
    /* access modifiers changed from: private */
    public final AtomicInteger mWifiState = new AtomicInteger(1);
    /* access modifiers changed from: private */
    public WifiStateTracker mWifiStateTracker;
    /* access modifiers changed from: private */
    public final WifiTrafficPoller mWifiTrafficPoller;
    private final WrongPasswordNotifier mWrongPasswordNotifier;
    private int mWtcMode = 1;

    public interface ClientModeChannel {

        public interface WifiNetworkCallback {
            void checkIsCaptivePortalException(String str);

            void handleResultRoamInLevel1State(boolean z);

            void notifyDhcpSession(String str);

            void notifyLinkPropertiesUpdated(LinkProperties linkProperties);

            void notifyProvisioningFail();

            void notifyReachabilityLost();

            void notifyRoamSession(String str);
        }

        Message fetchPacketCountNative();
    }

    static /* synthetic */ int access$16508(ClientModeImpl x0) {
        int i = x0.mRssiPollToken;
        x0.mRssiPollToken = i + 1;
        return i;
    }

    static /* synthetic */ int access$20708(ClientModeImpl x0) {
        int i = x0.mRoamFailCount;
        x0.mRoamFailCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$22708(ClientModeImpl x0) {
        int i = x0.mScanResultsEventCounter;
        x0.mScanResultsEventCounter = i + 1;
        return i;
    }

    static /* synthetic */ int access$9972(int x0) {
        int i = mPhoneStateEvent & x0;
        mPhoneStateEvent = i;
        return i;
    }

    static /* synthetic */ int access$9976(int x0) {
        int i = mPhoneStateEvent | x0;
        mPhoneStateEvent = i;
        return i;
    }

    /* access modifiers changed from: protected */
    public void loge(String s) {
        Log.e(getName(), s);
    }

    /* access modifiers changed from: protected */
    public void logd(String s) {
        Log.d(getName(), s);
    }

    /* access modifiers changed from: protected */
    public void log(String s) {
        Log.d(getName(), s);
    }

    public WifiScoreReport getWifiScoreReport() {
        return this.mWifiScoreReport;
    }

    /* access modifiers changed from: private */
    public void processRssiThreshold(byte curRssi, int reason, WifiNative.WifiRssiEventHandler rssiHandler) {
        if (curRssi == Byte.MAX_VALUE || curRssi == Byte.MIN_VALUE) {
            Log.wtf(TAG, "processRssiThreshold: Invalid rssi " + curRssi);
            return;
        }
        int i = 0;
        while (true) {
            byte[] bArr = this.mRssiRanges;
            if (i >= bArr.length) {
                return;
            }
            if (curRssi < bArr[i]) {
                byte maxRssi = bArr[i];
                byte minRssi = bArr[i - 1];
                this.mWifiInfo.setRssi(curRssi);
                updateCapabilities();
                int ret = startRssiMonitoringOffload(maxRssi, minRssi, rssiHandler);
                Log.d(TAG, "Re-program RSSI thresholds for " + getWhatToString(reason) + ": [" + minRssi + ", " + maxRssi + "], curRssi=" + curRssi + " ret=" + ret);
                return;
            }
            i++;
        }
    }

    /* access modifiers changed from: package-private */
    public int getPollRssiIntervalMsecs() {
        return this.mPollRssiIntervalMsecs;
    }

    /* access modifiers changed from: package-private */
    public void setPollRssiIntervalMsecs(int newPollIntervalMsecs) {
        this.mPollRssiIntervalMsecs = newPollIntervalMsecs;
    }

    public boolean clearTargetBssid(String dbg) {
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (config == null) {
            return false;
        }
        if (!this.mHandleIfaceIsUp) {
            Log.w(TAG, "clearTargetBssid, mHandleIfaceIsUp is false");
            return false;
        }
        String bssid = "any";
        if (config.BSSID != null) {
            bssid = config.BSSID;
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "force BSSID to " + bssid + "due to config");
            }
        }
        if (this.mVerboseLoggingEnabled) {
            logd(dbg + " clearTargetBssid " + bssid + " key=" + config.configKey());
        }
        this.mTargetRoamBSSID = bssid;
        return this.mWifiNative.setConfiguredNetworkBSSID(this.mInterfaceName, bssid);
    }

    /* access modifiers changed from: private */
    public boolean setTargetBssid(WifiConfiguration config, String bssid) {
        if (config == null || bssid == null) {
            return false;
        }
        if (config.BSSID != null) {
            bssid = config.BSSID;
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "force BSSID to " + bssid + "due to config");
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setTargetBssid set to " + bssid + " key=" + config.configKey());
        }
        this.mTargetRoamBSSID = bssid;
        config.getNetworkSelectionStatus().setNetworkSelectionBSSID(bssid);
        return true;
    }

    /* access modifiers changed from: private */
    public TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    /* JADX WARNING: Illegal instructions before constructor call */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ClientModeImpl(android.content.Context r25, com.android.server.wifi.FrameworkFacade r26, android.os.Looper r27, android.os.UserManager r28, com.android.server.wifi.WifiInjector r29, com.android.server.wifi.BackupManagerProxy r30, com.android.server.wifi.WifiCountryCode r31, com.android.server.wifi.WifiNative r32, com.android.server.wifi.WrongPasswordNotifier r33, com.android.server.wifi.SarManager r34, com.android.server.wifi.WifiTrafficPoller r35, com.android.server.wifi.LinkProbeManager r36) {
        /*
            r24 = this;
            r1 = r24
            r2 = r25
            r3 = r27
            java.lang.String r4 = "WifiClientModeImpl"
            r1.<init>(r4, r3)
            r5 = 0
            r1.mVerboseLoggingEnabled = r5
            r1.mQosGameIsRunning = r5
            r1.mPersistQosTid = r5
            r1.mPersistQosUid = r5
            r1.mDidBlackListBSSID = r5
            r6 = -1
            r1.mLastConnectedTime = r6
            r6 = 1
            r1.LTEU_MOBILEHOTSPOT_5GHZ_ENABLED = r6
            r0 = 2
            r1.LTEU_P2P_5GHZ_CONNECTED = r0
            r0 = 4
            r1.LTEU_STA_5GHZ_CONNECTED = r0
            r7 = -1
            r1.laaEnterState = r7
            r1.laaActiveState = r7
            r1.mIsWifiOffByAirplane = r5
            r8 = 0
            r1.mFwLogTimer = r8
            r1.mP2pDisableListener = r8
            java.util.concurrent.atomic.AtomicBoolean r9 = new java.util.concurrent.atomic.AtomicBoolean
            r9.<init>(r5)
            r1.mP2pConnected = r9
            r1.mTemporarilyDisconnectWifi = r5
            r1.mScreenOn = r5
            r1.mLastSignalLevel = r7
            r1.mIpReachabilityDisconnectEnabled = r5
            r1.mHandleIfaceIsUp = r5
            r1.mConnectedApInternalType = r5
            r1.mIsNchoParamSet = r5
            r1.mNchoVersion = r7
            r1.mNcho10State = r5
            r9 = -75
            r1.mDefaultRoamTrigger = r9
            r9 = 10
            r1.mDefaultRoamDelta = r9
            r1.mDefaultRoamScanPeriod = r9
            r1.mNcho20State = r5
            java.lang.String r9 = "Not_use"
            r1.mApInterfaceName = r9
            r1.mFirstTurnOn = r6
            java.util.concurrent.atomic.AtomicBoolean r9 = new java.util.concurrent.atomic.AtomicBoolean
            r9.<init>(r5)
            r1.mWifiAdpsEnabled = r9
            r1.mRoamDhcpPolicy = r5
            r1.mRoamDhcpPolicyByB2bConfig = r5
            r1.mEnableRssiPolling = r5
            r9 = 3000(0xbb8, float:4.204E-42)
            r1.mPollRssiIntervalMsecs = r9
            r1.mRssiPollToken = r5
            r1.mOperationalMode = r0
            r1.mModeChange = r5
            r1.mClientModeCallback = r8
            r1.mBluetoothConnectionActive = r5
            r1.mPeriodicScanToken = r5
            java.lang.Object r0 = new java.lang.Object
            r0.<init>()
            r1.mDhcpResultsLock = r0
            r1.mIsAutoRoaming = r5
            r1.mBlockFccChannelCmd = r5
            r1.mRoamFailCount = r5
            java.lang.String r0 = "any"
            r1.mTargetRoamBSSID = r0
            r1.mTargetNetworkId = r7
            r10 = 0
            r1.mLastDriverRoamAttempt = r10
            r1.mTargetWifiConfiguration = r8
            r1.mIsShutdown = r5
            r1.mIsRoaming = r5
            com.android.internal.util.AsyncChannel r0 = new com.android.internal.util.AsyncChannel
            r0.<init>()
            r1.mReplyChannel = r0
            r1.mIWCMonitorChannel = r8
            java.lang.Object r0 = new java.lang.Object
            r0.<init>()
            r1.mNetworkAgentLock = r0
            android.net.NetworkCapabilities r0 = new android.net.NetworkCapabilities
            r0.<init>()
            r1.mNetworkCapabilitiesFilter = r0
            android.net.NetworkMisc r0 = new android.net.NetworkMisc
            r0.<init>()
            r1.mNetworkMisc = r0
            r1.mRoamWatchdogCount = r5
            r1.mDisconnectingWatchdogCount = r5
            r1.mSuspendOptNeedsDisabled = r5
            java.util.concurrent.atomic.AtomicBoolean r0 = new java.util.concurrent.atomic.AtomicBoolean
            r0.<init>(r6)
            r1.mUserWantsSuspendOpt = r0
            r1.mRunningBeaconCount = r5
            com.android.server.wifi.ClientModeImpl$DefaultState r0 = new com.android.server.wifi.ClientModeImpl$DefaultState
            r0.<init>()
            r1.mDefaultState = r0
            com.android.server.wifi.ClientModeImpl$ConnectModeState r0 = new com.android.server.wifi.ClientModeImpl$ConnectModeState
            r0.<init>()
            r1.mConnectModeState = r0
            com.android.server.wifi.ClientModeImpl$L2ConnectedState r0 = new com.android.server.wifi.ClientModeImpl$L2ConnectedState
            r0.<init>()
            r1.mL2ConnectedState = r0
            com.android.server.wifi.ClientModeImpl$ObtainingIpState r0 = new com.android.server.wifi.ClientModeImpl$ObtainingIpState
            r0.<init>()
            r1.mObtainingIpState = r0
            com.android.server.wifi.ClientModeImpl$ConnectedState r0 = new com.android.server.wifi.ClientModeImpl$ConnectedState
            r0.<init>()
            r1.mConnectedState = r0
            com.android.server.wifi.ClientModeImpl$RoamingState r0 = new com.android.server.wifi.ClientModeImpl$RoamingState
            r0.<init>()
            r1.mRoamingState = r0
            com.android.server.wifi.ClientModeImpl$DisconnectingState r0 = new com.android.server.wifi.ClientModeImpl$DisconnectingState
            r0.<init>()
            r1.mDisconnectingState = r0
            com.android.server.wifi.ClientModeImpl$DisconnectedState r0 = new com.android.server.wifi.ClientModeImpl$DisconnectedState
            r0.<init>()
            r1.mDisconnectedState = r0
            java.util.concurrent.atomic.AtomicInteger r0 = new java.util.concurrent.atomic.AtomicInteger
            r0.<init>(r6)
            r1.mWifiState = r0
            r1.mIsRunning = r5
            r1.mReportedRunning = r5
            android.os.WorkSource r0 = new android.os.WorkSource
            r0.<init>()
            r1.mRunningWifiUids = r0
            android.os.WorkSource r0 = new android.os.WorkSource
            r0.<init>()
            r1.mLastRunningWifiUids = r0
            r1.mLocationRequestNetworkId = r7
            r1.mLastConnectAttemptTimestamp = r10
            r1.mMessageHandlingStatus = r5
            r1.mOnTime = r5
            r1.mTxTime = r5
            r1.mRxTime = r5
            r1.mOnTimeScreenStateChange = r5
            r1.mLastOntimeReportTimeStamp = r10
            r1.mLastScreenStateChangeTimeStamp = r10
            r1.mOnTimeLastReport = r5
            r1.mTxTimeLastReport = r5
            r1.mRxTimeLastReport = r5
            r1.mLastLinkLayerStatsUpdate = r10
            r1.mLastRequestPackageNameForGeofence = r8
            java.util.concurrent.atomic.AtomicInteger r0 = new java.util.concurrent.atomic.AtomicInteger
            r0.<init>(r5)
            r1.mNullMessageCounter = r0
            r1.customDeviceManager = r8
            r1.mLastL2KeyAndGroupHint = r8
            com.android.server.wifi.ClientModeImpl$18 r0 = new com.android.server.wifi.ClientModeImpl$18
            r0.<init>()
            r1.mPhoneStateListener = r0
            r1.mWtcMode = r6
            r1.mScanMode = r6
            r0 = -80
            r1.mRssiThreshold = r0
            r0 = -70
            r1.mCandidateRssiThreshold24G = r0
            r1.mCandidateRssiThreshold5G = r0
            r1.mCandidateRssiThreshold6G = r0
            r10 = 3
            r1.mMobileState = r10
            r1.mIsRoamNetwork = r5
            java.lang.String r0 = "com.samsung.android.oneconnect"
            java.lang.String r11 = "sdet.pack"
            java.lang.String r12 = "sdet.pack.channel"
            java.lang.String[] r11 = new java.lang.String[]{r0, r11, r12}
            java.util.List r11 = java.util.Arrays.asList(r11)
            r1.NETWORKSETTINGS_PERMISSION_EXCEPTION_PACKAGE_LIST = r11
            java.lang.String r12 = "com.android.systemui"
            java.lang.String r13 = "android.uid.systemui"
            java.lang.String r14 = "com.samsung.android.app.aodservice"
            java.lang.String r15 = "com.sec.android.cover.ledcover"
            java.lang.String r16 = "com.samsung.android.app.routines"
            java.lang.String r17 = "com.android.systemui"
            java.lang.String r18 = "com.samsung.desktopsystemui"
            java.lang.String r19 = "com.samsung.android.gesture.MotionRecognitionService"
            java.lang.String r20 = "com.android.systemui.sensor.PickupController"
            java.lang.String r21 = "com.samsung.uready.agent"
            java.lang.String[] r11 = new java.lang.String[]{r12, r13, r14, r15, r16, r17, r18, r19, r20, r21}
            java.util.List r11 = java.util.Arrays.asList(r11)
            r1.MULTINETWORK_EXCEPTION_PACKAGE_LIST = r11
            java.lang.String r11 = "com.samsung.android.app.mirrorlink"
            java.lang.String r12 = "com.google.android.gms"
            java.lang.String r13 = "com.google.android.projection.gearhead"
            java.lang.String[] r0 = new java.lang.String[]{r0, r11, r12, r13}
            java.util.List r0 = java.util.Arrays.asList(r0)
            r1.MULTINETWORK_ALLOWING_SYSTEM_PACKAGE_LIST = r0
            r1.mIsWifiOnly = r7
            java.util.HashMap r0 = new java.util.HashMap
            r0.<init>()
            r1.mEventCounter = r0
            r1.mLastEAPFailureNetworkId = r7
            r1.mLastEAPFailureCount = r5
            r1.mWifiNetworkCallbackList = r8
            r1.isDhcpStartSent = r5
            r1.mIsManualSelection = r5
            com.android.server.wifi.ClientModeImpl$19 r0 = new com.android.server.wifi.ClientModeImpl$19
            r0.<init>()
            r1.mSemLocationListener = r0
            r11 = r29
            r1.mWifiInjector = r11
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiMetrics r0 = r0.getWifiMetrics()
            r1.mWifiMetrics = r0
            com.android.server.wifi.Clock r0 = r29.getClock()
            r1.mClock = r0
            com.android.server.wifi.PropertyService r0 = r29.getPropertyService()
            r1.mPropertyService = r0
            com.android.server.wifi.BuildProperties r0 = r29.getBuildProperties()
            r1.mBuildProperties = r0
            com.android.server.wifi.WifiScoreCard r0 = r29.getWifiScoreCard()
            r1.mWifiScoreCard = r0
            r1.mContext = r2
            r12 = r26
            r1.mFacade = r12
            r13 = r32
            r1.mWifiNative = r13
            r14 = r30
            r1.mBackupManagerProxy = r14
            r15 = r33
            r1.mWrongPasswordNotifier = r15
            r9 = r34
            r1.mSarManager = r9
            com.samsung.android.server.wifi.SemSarManager r0 = new com.samsung.android.server.wifi.SemSarManager
            android.content.Context r10 = r1.mContext
            com.android.server.wifi.WifiNative r7 = r1.mWifiNative
            r0.<init>(r10, r7)
            r1.mSemSarManager = r0
            r7 = r35
            r1.mWifiTrafficPoller = r7
            r10 = r36
            r1.mLinkProbeManager = r10
            android.net.NetworkInfo r0 = new android.net.NetworkInfo
            java.lang.String r8 = "WIFI"
            java.lang.String r7 = ""
            r0.<init>(r6, r5, r8, r7)
            r1.mNetworkInfo = r0
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            java.lang.String r7 = "batterystats"
            android.os.IBinder r0 = r0.getService(r7)
            com.android.internal.app.IBatteryStats r0 = com.android.internal.app.IBatteryStats.Stub.asInterface(r0)
            r1.mBatteryStats = r0
            com.android.server.wifi.WifiStateTracker r0 = r29.getWifiStateTracker()
            r1.mWifiStateTracker = r0
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            java.lang.String r7 = "network_management"
            android.os.IBinder r7 = r0.getService(r7)
            android.content.Context r0 = r1.mContext
            android.content.pm.PackageManager r0 = r0.getPackageManager()
            java.lang.String r8 = "android.hardware.wifi.direct"
            boolean r0 = r0.hasSystemFeature(r8)
            r1.mP2pSupported = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.util.WifiPermissionsUtil r0 = r0.getWifiPermissionsUtil()
            r1.mWifiPermissionsUtil = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiConfigManager r0 = r0.getWifiConfigManager()
            r1.mWifiConfigManager = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.hotspot2.PasspointManager r0 = r0.getPasspointManager()
            r1.mPasspointManager = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiMonitor r0 = r0.getWifiMonitor()
            r1.mWifiMonitor = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.BaseWifiDiagnostics r0 = r0.getWifiDiagnostics()
            r1.mWifiDiagnostics = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.ScanRequestProxy r0 = r0.getScanRequestProxy()
            r1.mScanRequestProxy = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.util.WifiPermissionsWrapper r0 = r0.getWifiPermissionsWrapper()
            r1.mWifiPermissionsWrapper = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiDataStall r0 = r0.getWifiDataStall()
            r1.mWifiDataStall = r0
            com.android.server.wifi.ExtendedWifiInfo r0 = new com.android.server.wifi.ExtendedWifiInfo
            r0.<init>()
            r1.mWifiInfo = r0
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            com.android.server.wifi.WifiConfigManager r8 = r1.mWifiConfigManager
            android.os.Handler r6 = r24.getHandler()
            com.android.server.wifi.SupplicantStateTracker r0 = r0.makeSupplicantStateTracker(r2, r8, r6)
            r1.mSupplicantStateTracker = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiConnectivityManager r0 = r0.makeWifiConnectivityManager(r1)
            r1.mWifiConnectivityManager = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiGeofenceManager r0 = r0.getWifiGeofenceManager()
            r1.mWifiGeofenceManager = r0
            com.android.server.wifi.WifiGeofenceManager r0 = r1.mWifiGeofenceManager
            boolean r0 = r0.isSupported()
            if (r0 == 0) goto L_0x02aa
            com.android.server.wifi.WifiGeofenceManager r0 = r1.mWifiGeofenceManager
            com.android.server.wifi.WifiConnectivityManager r6 = r1.mWifiConnectivityManager
            r0.register((com.android.server.wifi.WifiConnectivityManager) r6)
            com.android.server.wifi.WifiGeofenceManager r0 = r1.mWifiGeofenceManager
            android.net.NetworkInfo r6 = r1.mNetworkInfo
            r0.register((android.net.NetworkInfo) r6)
        L_0x02aa:
            android.net.LinkProperties r0 = new android.net.LinkProperties
            r0.<init>()
            r1.mLinkProperties = r0
            android.net.LinkProperties r0 = new android.net.LinkProperties
            r0.<init>()
            r1.mOldLinkProperties = r0
            com.android.server.wifi.ClientModeImpl$McastLockManagerFilterController r0 = new com.android.server.wifi.ClientModeImpl$McastLockManagerFilterController
            r0.<init>()
            r1.mMcastLockManagerFilterController = r0
            android.net.NetworkInfo r0 = r1.mNetworkInfo
            r0.setIsAvailable(r5)
            r0 = 0
            r1.mLastBssid = r0
            r0 = -1
            r1.mLastNetworkId = r0
            r1.mLastConnectedNetworkId = r0
            r1.mLastSignalLevel = r0
            r6 = r31
            r1.mCountryCode = r6
            com.android.server.wifi.WifiScoreReport r0 = new com.android.server.wifi.WifiScoreReport
            com.android.server.wifi.WifiInjector r8 = r1.mWifiInjector
            com.android.server.wifi.ScoringParams r8 = r8.getScoringParams()
            com.android.server.wifi.Clock r5 = r1.mClock
            r0.<init>(r8, r5)
            r1.mWifiScoreReport = r0
            com.samsung.android.server.wifi.WifiDelayDisconnect r0 = new com.samsung.android.server.wifi.WifiDelayDisconnect
            android.content.Context r5 = r1.mContext
            com.android.server.wifi.WifiInjector r8 = r1.mWifiInjector
            r0.<init>(r5, r8)
            r1.mDelayDisconnect = r0
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 1
            r0.addTransportType(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 12
            r0.addCapability(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 11
            r0.addCapability(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 18
            r0.addCapability(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 20
            r0.addCapability(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 13
            r0.addCapability(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r5 = 1048576(0x100000, float:1.469368E-39)
            r0.setLinkUpstreamBandwidthKbps(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            r0.setLinkDownstreamBandwidthKbps(r5)
            android.net.NetworkCapabilities r0 = r1.mNetworkCapabilitiesFilter
            android.net.MatchAllNetworkSpecifier r5 = new android.net.MatchAllNetworkSpecifier
            r5.<init>()
            r0.setNetworkSpecifier(r5)
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            android.net.NetworkCapabilities r5 = r1.mNetworkCapabilitiesFilter
            com.android.server.wifi.WifiConnectivityManager r8 = r1.mWifiConnectivityManager
            com.android.server.wifi.WifiNetworkFactory r0 = r0.makeWifiNetworkFactory(r5, r8)
            r1.mNetworkFactory = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            android.net.NetworkCapabilities r5 = r1.mNetworkCapabilitiesFilter
            com.android.server.wifi.WifiConnectivityManager r8 = r1.mWifiConnectivityManager
            com.android.server.wifi.UntrustedWifiNetworkFactory r0 = r0.makeUntrustedWifiNetworkFactory(r5, r8)
            r1.mUntrustedNetworkFactory = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiNetworkSuggestionsManager r0 = r0.getWifiNetworkSuggestionsManager()
            r1.mWifiNetworkSuggestionsManager = r0
            java.util.concurrent.atomic.AtomicBoolean r0 = r1.mWifiAdpsEnabled
            android.content.Context r5 = r1.mContext
            android.content.ContentResolver r5 = r5.getContentResolver()
            java.lang.String r8 = "wifi_adps_enable"
            r2 = 0
            int r5 = android.provider.Settings.Secure.getInt(r5, r8, r2)
            r2 = 1
            if (r5 != r2) goto L_0x035f
            r2 = 1
            goto L_0x0360
        L_0x035f:
            r2 = 0
        L_0x0360:
            r0.set(r2)
            android.content.IntentFilter r0 = new android.content.IntentFilter
            r0.<init>()
            r2 = r0
            java.lang.String r0 = "android.intent.action.SCREEN_ON"
            r2.addAction(r0)
            java.lang.String r0 = "android.intent.action.SCREEN_OFF"
            r2.addAction(r0)
            java.lang.String r0 = "android.intent.action.ACTION_SCREEN_ON_BY_PROXIMITY"
            r2.addAction(r0)
            java.lang.String r0 = "android.intent.action.ACTION_SCREEN_OFF_BY_PROXIMITY"
            r2.addAction(r0)
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$1 r5 = new com.android.server.wifi.ClientModeImpl$1
            r5.<init>()
            r0.registerReceiver(r5, r2)
            android.content.IntentFilter r0 = new android.content.IntentFilter
            r0.<init>()
            r5 = r0
            boolean r0 = ENABLE_SUPPORT_QOSCONTROL
            if (r0 == 0) goto L_0x039b
            java.lang.String r0 = "com.samsung.android.game.intent.action.WIFI_QOS_CONTROL_START"
            r5.addAction(r0)
            java.lang.String r0 = "com.samsung.android.game.intent.action.WIFI_QOS_CONTROL_END"
            r5.addAction(r0)
        L_0x039b:
            android.content.Context r0 = r1.mContext
            r21 = r2
            com.android.server.wifi.ClientModeImpl$2 r2 = new com.android.server.wifi.ClientModeImpl$2
            r2.<init>()
            java.lang.String r6 = "android.permission.HARDWARE_TEST"
            r22 = r7
            r7 = 0
            r0.registerReceiver(r2, r5, r6, r7)
            android.content.Context r0 = r1.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r2 = "wifi_hotspot20_enable"
            android.net.Uri r6 = android.provider.Settings.Secure.getUriFor(r2)
            com.android.server.wifi.ClientModeImpl$3 r7 = new com.android.server.wifi.ClientModeImpl$3
            r19 = r5
            android.os.Handler r5 = r24.getHandler()
            r7.<init>(r5)
            r5 = 0
            r0.registerContentObserver(r6, r5, r7)
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$4 r5 = new com.android.server.wifi.ClientModeImpl$4
            r5.<init>()
            android.content.IntentFilter r6 = new android.content.IntentFilter
            java.lang.String r7 = "com.sec.android.ISSUE_TRACKER_ONOFF"
            r6.<init>(r7)
            r0.registerReceiver(r5, r6)
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            android.content.Context r5 = r1.mContext
            java.lang.String r6 = "wifi_suspend_optimizations_enabled"
            android.net.Uri r6 = android.provider.Settings.Global.getUriFor(r6)
            com.android.server.wifi.ClientModeImpl$5 r7 = new com.android.server.wifi.ClientModeImpl$5
            android.os.Handler r9 = r24.getHandler()
            r7.<init>(r9)
            r9 = 0
            r0.registerContentObserver(r5, r6, r9, r7)
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            android.content.Context r5 = r1.mContext
            android.net.Uri r6 = android.provider.Settings.Secure.getUriFor(r8)
            com.android.server.wifi.ClientModeImpl$6 r7 = new com.android.server.wifi.ClientModeImpl$6
            android.os.Handler r8 = r24.getHandler()
            r7.<init>(r8)
            r0.registerContentObserver(r5, r6, r9, r7)
            android.content.Context r0 = r1.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r5 = "safe_wifi"
            android.net.Uri r5 = android.provider.Settings.Global.getUriFor(r5)
            com.android.server.wifi.ClientModeImpl$7 r6 = new com.android.server.wifi.ClientModeImpl$7
            android.os.Handler r7 = r24.getHandler()
            r6.<init>(r7)
            r0.registerContentObserver(r5, r9, r6)
            java.util.concurrent.atomic.AtomicBoolean r0 = r1.mUserWantsSuspendOpt
            com.android.server.wifi.FrameworkFacade r5 = r1.mFacade
            android.content.Context r6 = r1.mContext
            java.lang.String r7 = "wifi_suspend_optimizations_enabled"
            r8 = 1
            int r5 = r5.getIntegerSetting(r6, r7, r8)
            if (r5 != r8) goto L_0x042c
            r5 = 1
            goto L_0x042d
        L_0x042c:
            r5 = 0
        L_0x042d:
            r0.set(r5)
            android.content.Context r0 = r1.mContext
            java.lang.String r5 = "power"
            java.lang.Object r0 = r0.getSystemService(r5)
            r5 = r0
            android.os.PowerManager r5 = (android.os.PowerManager) r5
            java.lang.String r0 = r24.getName()
            r6 = 1
            android.os.PowerManager$WakeLock r0 = r5.newWakeLock(r6, r0)
            r1.mWakeLock = r0
            java.lang.String r0 = "WifiSuspend"
            android.os.PowerManager$WakeLock r0 = r5.newWakeLock(r6, r0)
            r1.mSuspendWakeLock = r0
            android.os.PowerManager$WakeLock r0 = r1.mSuspendWakeLock
            r6 = 0
            r0.setReferenceCounted(r6)
            android.content.IntentFilter r0 = new android.content.IntentFilter
            r0.<init>()
            r6 = r0
            boolean r0 = CSC_SUPPORT_5G_ANT_SHARE
            if (r0 == 0) goto L_0x0468
            java.lang.String r0 = "android.intent.action.coexstatus"
            r6.addAction(r0)
            r7 = 0
            r1.laaEnterState = r7
            r1.laaActiveState = r7
        L_0x0468:
            android.sec.enterprise.EnterpriseDeviceManager r0 = android.sec.enterprise.EnterpriseDeviceManager.getInstance()
            android.sec.enterprise.WifiPolicy r0 = r0.getWifiPolicy()
            r1.mWifiPolicy = r0
            java.lang.String r0 = "com.samsung.android.knox.intent.action.ENABLE_NETWORK_INTERNAL"
            r6.addAction(r0)
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$8 r7 = new com.android.server.wifi.ClientModeImpl$8
            r7.<init>()
            r0.registerReceiver(r7, r6)
            boolean r0 = CSC_SUPPORT_5G_ANT_SHARE
            if (r0 == 0) goto L_0x0496
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$9 r7 = new com.android.server.wifi.ClientModeImpl$9
            r7.<init>()
            android.content.IntentFilter r8 = new android.content.IntentFilter
            java.lang.String r9 = "android.net.wifi.p2p.CONNECTION_STATE_CHANGE"
            r8.<init>(r9)
            r0.registerReceiver(r7, r8)
        L_0x0496:
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$10 r7 = new com.android.server.wifi.ClientModeImpl$10
            r7.<init>()
            android.content.IntentFilter r8 = new android.content.IntentFilter
            java.lang.String r9 = "android.net.wifi.WIFI_AP_STATE_CHANGED"
            r8.<init>(r9)
            r0.registerReceiver(r7, r8)
            android.content.Context r0 = r1.mContext
            com.android.server.wifi.ClientModeImpl$11 r7 = new com.android.server.wifi.ClientModeImpl$11
            r7.<init>()
            android.content.IntentFilter r8 = new android.content.IntentFilter
            java.lang.String r9 = "com.android.server.wifi.AP_LOCATION_PASSIVE_REQUEST"
            r8.<init>(r9)
            r0.registerReceiver(r7, r8)
            android.sec.enterprise.EnterpriseDeviceManager r0 = android.sec.enterprise.EnterpriseDeviceManager.getInstance()
            android.sec.enterprise.WifiPolicy r0 = r0.getWifiPolicy()
            r1.mWifiPolicy = r0
            java.lang.String r0 = "com.samsung.android.knox.intent.action.ENABLE_NETWORK_INTERNAL"
            r6.addAction(r0)
            com.android.server.wifi.FrameworkFacade r0 = r1.mFacade
            android.content.Context r7 = r1.mContext
            java.lang.String r8 = "data_roaming"
            android.net.Uri r8 = android.provider.Settings.Global.getUriFor(r8)
            com.android.server.wifi.ClientModeImpl$12 r9 = new com.android.server.wifi.ClientModeImpl$12
            r23 = r5
            android.os.Handler r5 = r24.getHandler()
            r9.<init>(r5)
            r5 = 0
            r0.registerContentObserver(r7, r8, r5, r9)
            android.content.IntentFilter r0 = new android.content.IntentFilter
            r0.<init>()
            java.lang.String r5 = "android.net.conn.CONNECTIVITY_CHANGE"
            r0.addAction(r5)
            java.lang.String r5 = "com.android.intent.action.DATAUSAGE_REACH_TO_LIMIT"
            r0.addAction(r5)
            android.content.Context r5 = r1.mContext
            com.android.server.wifi.ClientModeImpl$13 r7 = new com.android.server.wifi.ClientModeImpl$13
            r7.<init>()
            r5.registerReceiver(r7, r0)
            android.content.Context r0 = r1.mContext
            android.content.res.Resources r0 = r0.getResources()
            r5 = 17891595(0x111010b, float:2.6633042E-38)
            boolean r0 = r0.getBoolean(r5)
            r1.mConnectedMacRandomzationSupported = r0
            com.android.server.wifi.ExtendedWifiInfo r0 = r1.mWifiInfo
            boolean r5 = r1.mConnectedMacRandomzationSupported
            r0.setEnableConnectedMacRandomization(r5)
            com.android.server.wifi.WifiMetrics r0 = r1.mWifiMetrics
            boolean r5 = r1.mConnectedMacRandomzationSupported
            r0.setIsMacRandomizationOn(r5)
            java.lang.String r0 = "524288,1048576,4194304,524288,1048576,4194304"
            r1.mTcpBufferSizes = r0
            com.android.internal.util.State r0 = r1.mDefaultState
            r1.addState(r0)
            com.android.internal.util.State r0 = r1.mConnectModeState
            com.android.internal.util.State r5 = r1.mDefaultState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mL2ConnectedState
            com.android.internal.util.State r5 = r1.mConnectModeState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mObtainingIpState
            com.android.internal.util.State r5 = r1.mL2ConnectedState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mConnectedState
            com.android.internal.util.State r5 = r1.mL2ConnectedState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mRoamingState
            com.android.internal.util.State r5 = r1.mL2ConnectedState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mDisconnectingState
            com.android.internal.util.State r5 = r1.mConnectModeState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mDisconnectedState
            com.android.internal.util.State r5 = r1.mConnectModeState
            r1.addState(r0, r5)
            com.android.internal.util.State r0 = r1.mDefaultState
            r1.setInitialState(r0)
            boolean r0 = DBG
            if (r0 == 0) goto L_0x055c
            r9 = 3000(0xbb8, float:4.204E-42)
            goto L_0x055e
        L_0x055c:
            r9 = 1000(0x3e8, float:1.401E-42)
        L_0x055e:
            r1.setLogRecSize(r9)
            r5 = 0
            r1.setLogOnlyTransitions(r5)
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r0 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.ATT
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r5 = mOpBranding
            if (r0 != r5) goto L_0x05a0
            com.android.server.wifi.WifiConfigManager r0 = r1.mWifiConfigManager
            android.content.Context r5 = r1.mContext
            android.content.ContentResolver r5 = r5.getContentResolver()
            java.lang.String r7 = "wifi_auto_connecct"
            r8 = 1
            int r5 = android.provider.Settings.Secure.getInt(r5, r7, r8)
            if (r5 != r8) goto L_0x057e
            r5 = 1
            goto L_0x057f
        L_0x057e:
            r5 = 0
        L_0x057f:
            r0.setNetworkAutoConnect(r5)
            boolean r0 = DBG
            if (r0 == 0) goto L_0x05a0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r5 = "ATT set mNetworkAutoConnectEnabled = "
            r0.append(r5)
            com.android.server.wifi.WifiConfigManager r5 = r1.mWifiConfigManager
            boolean r5 = r5.getNetworkAutoConnectEnabled()
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            r1.logi(r0)
        L_0x05a0:
            com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r0 = new com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager
            android.content.Context r5 = r1.mContext
            com.android.server.wifi.ClientModeImpl$14 r7 = new com.android.server.wifi.ClientModeImpl$14
            r7.<init>()
            r0.<init>(r5, r3, r7)
            r1.mBigDataManager = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.samsung.android.server.wifi.dqa.SemWifiIssueDetector r0 = r0.getIssueDetector()
            r1.mIssueDetector = r0
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.android.server.wifi.WifiSettingsStore r0 = r0.getWifiSettingsStore()
            r1.mSettingsStore = r0
            r5 = 0
            android.content.Context r0 = r1.mContext     // Catch:{ SettingNotFoundException -> 0x05d9 }
            android.content.ContentResolver r0 = r0.getContentResolver()     // Catch:{ SettingNotFoundException -> 0x05d9 }
            int r0 = android.provider.Settings.Secure.getInt(r0, r2)     // Catch:{ SettingNotFoundException -> 0x05d9 }
            r5 = r0
            r7 = 1
            if (r5 == r7) goto L_0x05d5
            r7 = 3
            if (r5 != r7) goto L_0x05d1
            goto L_0x05d5
        L_0x05d1:
            r7 = 0
            r1.mIsPasspointEnabled = r7     // Catch:{ SettingNotFoundException -> 0x05d9 }
            goto L_0x05d8
        L_0x05d5:
            r7 = 1
            r1.mIsPasspointEnabled = r7     // Catch:{ SettingNotFoundException -> 0x05d9 }
        L_0x05d8:
            goto L_0x0617
        L_0x05d9:
            r0 = move-exception
            java.lang.String r7 = "WIFI_HOTSPOT20_ENABLE SettingNotFoundException"
            android.util.Log.e(r4, r7)
            com.samsung.android.net.wifi.OpBrandingLoader r4 = com.samsung.android.net.wifi.OpBrandingLoader.getInstance()
            java.lang.String r4 = r4.getMenuStatusForPasspoint()
            boolean r7 = android.text.TextUtils.isEmpty(r4)
            if (r7 != 0) goto L_0x0600
            java.lang.String r7 = "DEFAULT_ON"
            boolean r7 = r4.contains(r7)
            if (r7 == 0) goto L_0x05f6
            goto L_0x0600
        L_0x05f6:
            java.lang.String r7 = "DEFAULT_OFF"
            boolean r7 = r4.contains(r7)
            if (r7 == 0) goto L_0x0601
            r5 = 2
            goto L_0x0601
        L_0x0600:
            r5 = 3
        L_0x0601:
            r7 = 1
            if (r5 == r7) goto L_0x060c
            r8 = 3
            if (r5 != r8) goto L_0x0608
            goto L_0x060c
        L_0x0608:
            r7 = 0
            r1.mIsPasspointEnabled = r7
            goto L_0x060e
        L_0x060c:
            r1.mIsPasspointEnabled = r7
        L_0x060e:
            android.content.Context r7 = r1.mContext
            android.content.ContentResolver r7 = r7.getContentResolver()
            android.provider.Settings.Secure.putInt(r7, r2, r5)
        L_0x0617:
            com.android.server.wifi.hotspot2.PasspointManager r0 = r1.mPasspointManager
            boolean r2 = r1.mIsPasspointEnabled
            r0.setPasspointEnabled(r2)
            com.android.server.wifi.hotspot2.PasspointManager r0 = r1.mPasspointManager
            r2 = 0
            r0.setVendorSimUseable(r2)
            com.android.server.wifi.WifiInjector r0 = r1.mWifiInjector
            com.samsung.android.server.wifi.WifiB2BConfigurationPolicy r0 = r0.getWifiB2bConfigPolicy()
            r1.mWifiB2bConfigPolicy = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ClientModeImpl.<init>(android.content.Context, com.android.server.wifi.FrameworkFacade, android.os.Looper, android.os.UserManager, com.android.server.wifi.WifiInjector, com.android.server.wifi.BackupManagerProxy, com.android.server.wifi.WifiCountryCode, com.android.server.wifi.WifiNative, com.android.server.wifi.WrongPasswordNotifier, com.android.server.wifi.SarManager, com.android.server.wifi.WifiTrafficPoller, com.android.server.wifi.LinkProbeManager):void");
    }

    public void start() {
        ClientModeImpl.super.start();
        handleScreenStateChanged(((PowerManager) this.mContext.getSystemService("power")).isInteractive());
    }

    private void registerForWifiMonitorEvents() {
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ANQP_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, 147499, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_START_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.HS20_REMEDIATION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.RX_HS20_ANQP_ICON_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_IDENTITY, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_SIM_AUTH, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, 147527, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.BSSID_PRUNED_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_BIGDATA_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, 147499, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, this.mWifiMetrics.getHandler());
    }

    /* access modifiers changed from: private */
    public void setMulticastFilter(boolean enabled) {
        if (this.mIpClient != null) {
            this.mIpClient.setMulticastFilter(enabled);
        }
    }

    class McastLockManagerFilterController implements WifiMulticastLockManager.FilterController {
        McastLockManagerFilterController() {
        }

        public void startFilteringMulticastPackets() {
            ClientModeImpl.this.setMulticastFilter(true);
        }

        public void stopFilteringMulticastPackets() {
            ClientModeImpl.this.setMulticastFilter(false);
        }
    }

    class IpClientCallbacksImpl extends IpClientCallbacks {
        private final ConditionVariable mWaitForCreationCv = new ConditionVariable(false);
        private final ConditionVariable mWaitForStopCv = new ConditionVariable(false);

        IpClientCallbacksImpl() {
        }

        public void onIpClientCreated(IIpClient ipClient) {
            ClientModeImpl clientModeImpl = ClientModeImpl.this;
            IpClientManager unused = clientModeImpl.mIpClient = new IpClientManager(ipClient, clientModeImpl.getName());
            this.mWaitForCreationCv.open();
        }

        public void onPreDhcpAction() {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_PRE_DHCP_ACTION);
        }

        public void onPostDhcpAction() {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_POST_DHCP_ACTION);
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            if (dhcpResults != null) {
                ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_IPV4_PROVISIONING_SUCCESS, dhcpResults);
            } else {
                ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_IPV4_PROVISIONING_FAILURE);
            }
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            ClientModeImpl.this.mWifiMetrics.logStaEvent(7);
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_UPDATE_LINKPROPERTIES, newLp);
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_IP_CONFIGURATION_SUCCESSFUL);
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            ClientModeImpl.this.mWifiMetrics.logStaEvent(8);
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_IP_CONFIGURATION_LOST);
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_UPDATE_LINKPROPERTIES, newLp);
        }

        public void onReachabilityLost(String logMsg) {
            ClientModeImpl.this.mWifiMetrics.logStaEvent(9);
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_IP_REACHABILITY_LOST, logMsg);
        }

        public void installPacketFilter(byte[] filter) {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_INSTALL_PACKET_FILTER, filter);
        }

        public void startReadPacketFilter() {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_READ_PACKET_FILTER);
        }

        public void setFallbackMulticastFilter(boolean enabled) {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_SET_FALLBACK_PACKET_FILTERING, Boolean.valueOf(enabled));
        }

        public void setNeighborDiscoveryOffload(boolean enabled) {
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_CONFIG_ND_OFFLOAD, enabled);
        }

        public void onQuit() {
            this.mWaitForStopCv.open();
        }

        /* access modifiers changed from: package-private */
        public boolean awaitCreation() {
            return this.mWaitForCreationCv.block(10000);
        }

        /* access modifiers changed from: package-private */
        public boolean awaitShutdown() {
            return this.mWaitForStopCv.block(10000);
        }
    }

    /* access modifiers changed from: private */
    public void stopIpClient() {
        handlePostDhcpSetup();
        if (this.mIpClient != null) {
            this.mIpClient.stop();
        }
    }

    /* access modifiers changed from: package-private */
    public void setSupplicantLogLevel() {
        this.mWifiNative.setSupplicantLogLevel(this.mVerboseLoggingEnabled);
    }

    public void enableVerboseLogging(int verbose) {
        boolean z = true;
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
            DBG = true;
            setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? 200 : 3000);
        } else {
            this.mVerboseLoggingEnabled = false;
            DBG = false;
            setLogRecSize(1000);
        }
        configureVerboseHalLogging(this.mVerboseLoggingEnabled);
        setSupplicantLogLevel();
        this.mCountryCode.enableVerboseLogging(verbose);
        this.mWifiScoreReport.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mWifiMonitor.enableVerboseLogging(verbose);
        this.mWifiNative.enableVerboseLogging(verbose);
        this.mWifiConfigManager.enableVerboseLogging(verbose);
        this.mSupplicantStateTracker.enableVerboseLogging(verbose);
        this.mPasspointManager.enableVerboseLogging(verbose);
        this.mWifiGeofenceManager.enableVerboseLogging(verbose);
        this.mNetworkFactory.enableVerboseLogging(verbose);
        this.mLinkProbeManager.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiB2bConfigPolicy.enableVerboseLogging(verbose);
        WifiBigDataLogManager wifiBigDataLogManager = this.mBigDataManager;
        if (!DBG_PRODUCT_DEV && !this.mVerboseLoggingEnabled) {
            z = false;
        }
        wifiBigDataLogManager.setLogVisible(z);
    }

    private void configureVerboseHalLogging(boolean enableVerbose) {
        if (!this.mBuildProperties.isUserBuild()) {
            this.mPropertyService.set(SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL, enableVerbose ? LOGD_LEVEL_VERBOSE : LOGD_LEVEL_DEBUG);
        }
    }

    private boolean setRandomMacOui() {
        String oui = this.mContext.getResources().getString(17040005);
        if (TextUtils.isEmpty(oui)) {
            oui = GOOGLE_OUI;
        }
        String[] ouiParts = oui.split("-");
        byte[] ouiBytes = {(byte) (Integer.parseInt(ouiParts[0], 16) & 255), (byte) (Integer.parseInt(ouiParts[1], 16) & 255), (byte) (Integer.parseInt(ouiParts[2], 16) & 255)};
        logd("Setting OUI to " + oui);
        return this.mWifiNative.setScanningMacOui(this.mInterfaceName, ouiBytes);
    }

    /* access modifiers changed from: private */
    public boolean connectToUserSelectNetwork(int netId, int uid, boolean forceReconnect) {
        logd("connectToUserSelectNetwork netId " + netId + ", uid " + uid + ", forceReconnect = " + forceReconnect);
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (config == null) {
            loge("connectToUserSelectNetwork Invalid network Id=" + netId);
            return false;
        }
        boolean result = WifiPolicyCache.getInstance(this.mContext).isNetworkAllowed(config, false);
        logd("connectToUserSelectNetwork isNetworkAllowed=" + result);
        Context context = this.mContext;
        WifiMobileDeviceManager.auditLog(context, 3, result, TAG, "Performing an attempt to connect with AP. SSID: " + config.SSID);
        if (result) {
            Context context2 = this.mContext;
            WifiMobileDeviceManager.auditLog(context2, 3, true, TAG, "Connecting to Wi-Fi network whose ID is " + netId + " succeeded");
            if (!this.mWifiConfigManager.enableNetwork(netId, true, uid) || !this.mWifiConfigManager.updateLastConnectUid(netId, uid)) {
                logi("connectToUserSelectNetwork Allowing uid " + uid + " with insufficient permissions to connect=" + netId);
            } else if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid)) {
                this.mWifiConnectivityManager.setUserConnectChoice(netId);
            }
            if (forceReconnect || this.mWifiInfo.getNetworkId() != netId) {
                this.mWifiConnectivityManager.prepareForForcedConnection(netId);
                if (uid == 1000) {
                    this.mWifiMetrics.setNominatorForNetwork(config.networkId, 1);
                }
                this.mWifiConfigManager.setUserSelectNetwork(true);
                startConnectToNetwork(netId, uid, "any");
            } else {
                logi("connectToUserSelectNetwork already connecting/connected=" + netId);
            }
            if (!isWifiOnly()) {
                this.mWifiConfigManager.resetEntryRssi(netId);
            }
            return true;
        }
        Context context3 = this.mContext;
        WifiMobileDeviceManager.auditLog(context3, 3, false, TAG, "Connecting to Wi-Fi network whose ID is " + netId + " failed");
        return false;
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    /* access modifiers changed from: package-private */
    public String reportOnTime() {
        long now = this.mClock.getWallClockMillis();
        StringBuilder sb = new StringBuilder();
        int i = this.mOnTime;
        int on = i - this.mOnTimeLastReport;
        this.mOnTimeLastReport = i;
        int i2 = this.mTxTime;
        int tx = i2 - this.mTxTimeLastReport;
        this.mTxTimeLastReport = i2;
        int i3 = this.mRxTime;
        int rx = i3 - this.mRxTimeLastReport;
        this.mRxTimeLastReport = i3;
        int period = (int) (now - this.mLastOntimeReportTimeStamp);
        this.mLastOntimeReportTimeStamp = now;
        sb.append(String.format("[on:%d tx:%d rx:%d period:%d]", new Object[]{Integer.valueOf(on), Integer.valueOf(tx), Integer.valueOf(rx), Integer.valueOf(period)}));
        sb.append(String.format(" from screen [on:%d period:%d]", new Object[]{Integer.valueOf(this.mOnTime - this.mOnTimeScreenStateChange), Integer.valueOf((int) (now - this.mLastScreenStateChangeTimeStamp))}));
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public WifiLinkLayerStats getWifiLinkLayerStats() {
        if (this.mInterfaceName == null) {
            logw("getWifiLinkLayerStats called without an interface");
            return null;
        }
        this.mLastLinkLayerStatsUpdate = this.mClock.getWallClockMillis();
        WifiLinkLayerStats stats = this.mWifiNative.getWifiLinkLayerStats(this.mInterfaceName);
        if (stats != null) {
            this.mOnTime = stats.on_time;
            this.mTxTime = stats.tx_time;
            this.mRxTime = stats.rx_time;
            this.mRunningBeaconCount = stats.beacon_rx;
            this.mWifiInfo.updatePacketRates(stats, this.mLastLinkLayerStatsUpdate);
        } else {
            long mTxPkts = this.mFacade.getTxPackets(this.mInterfaceName);
            this.mWifiInfo.updatePacketRates(mTxPkts, this.mFacade.getRxPackets(this.mInterfaceName), this.mLastLinkLayerStatsUpdate);
        }
        return stats;
    }

    private byte[] getDstMacForKeepalive(KeepalivePacketData packetData) throws SocketKeepalive.InvalidPacketException {
        try {
            return NativeUtil.macAddressToByteArray(macAddressFromRoute(RouteInfo.selectBestRoute(this.mLinkProperties.getRoutes(), packetData.dstAddress).getGateway().getHostAddress()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SocketKeepalive.InvalidPacketException(-21);
        }
    }

    private static int getEtherProtoForKeepalive(KeepalivePacketData packetData) throws SocketKeepalive.InvalidPacketException {
        if (packetData.dstAddress instanceof Inet4Address) {
            return OsConstants.ETH_P_IP;
        }
        if (packetData.dstAddress instanceof Inet6Address) {
            return OsConstants.ETH_P_IPV6;
        }
        throw new SocketKeepalive.InvalidPacketException(-21);
    }

    /* access modifiers changed from: private */
    public int startWifiIPPacketOffload(int slot, KeepalivePacketData packetData, int intervalSeconds) {
        SocketKeepalive.InvalidPacketException e;
        try {
            byte[] packet = packetData.getPacket();
            try {
                byte[] dstMac = getDstMacForKeepalive(packetData);
                try {
                    int ret = this.mWifiNative.startSendingOffloadedPacket(this.mInterfaceName, slot, dstMac, packet, getEtherProtoForKeepalive(packetData), intervalSeconds * 1000);
                    if (ret == 0) {
                        return 0;
                    }
                    loge("startWifiIPPacketOffload(" + slot + ", " + intervalSeconds + "): hardware error " + ret);
                    return -31;
                } catch (SocketKeepalive.InvalidPacketException e2) {
                    e = e2;
                    byte[] bArr = dstMac;
                    return e.error;
                }
            } catch (SocketKeepalive.InvalidPacketException e3) {
                e = e3;
                return e.error;
            }
        } catch (SocketKeepalive.InvalidPacketException e4) {
            e = e4;
            return e.error;
        }
    }

    /* access modifiers changed from: private */
    public int stopWifiIPPacketOffload(int slot) {
        int ret = this.mWifiNative.stopSendingOffloadedPacket(this.mInterfaceName, slot);
        if (ret == 0) {
            return 0;
        }
        loge("stopWifiIPPacketOffload(" + slot + "): hardware error " + ret);
        return -31;
    }

    private int startRssiMonitoringOffload(byte maxRssi, byte minRssi, WifiNative.WifiRssiEventHandler rssiHandler) {
        return this.mWifiNative.startRssiMonitoring(this.mInterfaceName, maxRssi, minRssi, rssiHandler);
    }

    /* access modifiers changed from: private */
    public int stopRssiMonitoringOffload() {
        return this.mWifiNative.stopRssiMonitoring(this.mInterfaceName);
    }

    public boolean isSupportedGeofence() {
        return this.mWifiGeofenceManager.isSupported();
    }

    public void setWifiGeofenceListener(WifiGeofenceManager.WifiGeofenceStateListener listener) {
        if (this.mWifiGeofenceManager.isSupported()) {
            this.mWifiGeofenceManager.setWifiGeofenceListener(listener);
        }
    }

    public int getCurrentGeofenceState() {
        if (this.mWifiGeofenceManager.isSupported()) {
            return this.mWifiGeofenceManager.getCurrentGeofenceState();
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public boolean isGeofenceUsedByAnotherPackage() {
        if (this.mLastRequestPackageNameForGeofence == null) {
            return false;
        }
        return true;
    }

    public void requestGeofenceState(boolean enabled, String packageName) {
        if (this.mWifiGeofenceManager.isSupported()) {
            if (enabled) {
                this.mLastRequestPackageNameForGeofence = packageName;
                this.mWifiGeofenceManager.initGeofence();
            } else {
                this.mLastRequestPackageNameForGeofence = null;
                this.mWifiGeofenceManager.deinitGeofence();
            }
            this.mWifiGeofenceManager.setGeofenceStateByAnotherPackage(enabled);
        }
    }

    public List<String> getGeofenceEnterKeys() {
        if (this.mWifiGeofenceManager.isSupported()) {
            return this.mWifiGeofenceManager.getGeofenceEnterKeys();
        }
        return new ArrayList();
    }

    public int getGeofenceCellCount(String configKey) {
        if (this.mWifiGeofenceManager.isSupported()) {
            return this.mWifiGeofenceManager.getGeofenceCellCount(configKey);
        }
        return 0;
    }

    public void setWifiStateForApiCalls(int newState) {
        if (newState != 0) {
            if (!(newState == 1 || newState == 2 || newState == 3 || newState == 4)) {
                Log.d(TAG, "attempted to set an invalid state: " + newState);
                return;
            }
        } else if (getCurrentState() == this.mConnectedState && !isWifiOffByAirplane() && ENBLE_WLAN_CONFIG_ANALYTICS) {
            setAnalyticsUserDisconnectReason(WifiNative.f20xe9115267);
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setting wifi state to: " + newState);
        }
        this.mWifiState.set(newState);
        if (newState == 2) {
            WifiMobileDeviceManager.auditLog(this.mContext, 5, true, ClientModeImpl.class.getSimpleName(), "Enabling Wifi");
        } else if (newState == 0) {
            WifiMobileDeviceManager.auditLog(this.mContext, 5, true, ClientModeImpl.class.getSimpleName(), "Disabling Wifi");
        }
        if (this.mWifiGeofenceManager.isSupported()) {
            this.mWifiGeofenceManager.notifyWifiState(newState);
        }
    }

    public int syncGetWifiState() {
        return this.mWifiState.get();
    }

    public String syncGetWifiStateByName() {
        int i = this.mWifiState.get();
        if (i == 0) {
            return "disabling";
        }
        if (i == 1) {
            return "disabled";
        }
        if (i == 2) {
            return "enabling";
        }
        if (i == 3) {
            return "enabled";
        }
        if (i != 4) {
            return "[invalid state]";
        }
        return "unknown state";
    }

    public boolean isConnected() {
        return getCurrentState() == this.mConnectedState;
    }

    public boolean isDisconnected() {
        return getCurrentState() == this.mDisconnectedState;
    }

    public boolean isSupplicantTransientState() {
        SupplicantState supplicantState = this.mWifiInfo.getSupplicantState();
        if (supplicantState == SupplicantState.ASSOCIATING || supplicantState == SupplicantState.AUTHENTICATING || supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE || supplicantState == SupplicantState.GROUP_HANDSHAKE) {
            if (!this.mVerboseLoggingEnabled) {
                return true;
            }
            Log.d(TAG, "Supplicant is under transient state: " + supplicantState);
            return true;
        } else if (!this.mVerboseLoggingEnabled) {
            return false;
        } else {
            Log.d(TAG, "Supplicant is under steady state: " + supplicantState);
            return false;
        }
    }

    public WifiInfo syncRequestConnectionInfo() {
        return new WifiInfo(this.mWifiInfo);
    }

    public WifiInfo getWifiInfo() {
        return this.mWifiInfo;
    }

    public NetworkInfo syncGetNetworkInfo() {
        return new NetworkInfo(this.mNetworkInfo);
    }

    public DhcpResults syncGetDhcpResults() {
        DhcpResults dhcpResults;
        synchronized (this.mDhcpResultsLock) {
            dhcpResults = new DhcpResults(this.mDhcpResults);
        }
        return dhcpResults;
    }

    public void handleIfaceDestroyed() {
        this.mHandleIfaceIsUp = false;
        handleNetworkDisconnect();
    }

    public void setIsWifiOffByAirplane(boolean enabled) {
        this.mIsWifiOffByAirplane = enabled;
    }

    private boolean isWifiOffByAirplane() {
        return this.mIsWifiOffByAirplane;
    }

    public void setOperationalMode(int mode, String ifaceName) {
        if (this.mVerboseLoggingEnabled) {
            log("setting operational mode to " + String.valueOf(mode) + " for iface: " + ifaceName);
        }
        this.mModeChange = true;
        if (mode != 1) {
            if (getCurrentState() == this.mConnectedState) {
                notifyDisconnectInternalReason(17);
                if (ENBLE_WLAN_CONFIG_ANALYTICS && isWifiOffByAirplane()) {
                    setIsWifiOffByAirplane(false);
                    setAnalyticsUserDisconnectReason(WifiNative.ANALYTICS_DISCONNECT_REASON_USER_TRIGGER_DISCON_AIRPLANE);
                }
                this.mDelayDisconnect.checkAndWait(this.mNetworkInfo);
            }
            transitionTo(this.mDefaultState);
        } else if (ifaceName != null) {
            this.mInterfaceName = ifaceName;
            transitionTo(this.mDisconnectedState);
            this.mHandleIfaceIsUp = true;
        } else {
            Log.e(TAG, "supposed to enter connect mode, but iface is null -> DefaultState");
            transitionTo(this.mDefaultState);
        }
        sendMessageAtFrontOfQueue(CMD_SET_OPERATIONAL_MODE);
    }

    public void takeBugReport(String bugTitle, String bugDetail) {
        this.mWifiDiagnostics.takeBugReport(bugTitle, bugDetail);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public int getOperationalModeForTest() {
        return this.mOperationalMode;
    }

    /* access modifiers changed from: package-private */
    public void setConcurrentEnabled(boolean enable) {
        this.mConcurrentEnabled = enable;
        this.mScanRequestProxy.setScanningEnabled(!this.mConcurrentEnabled, "SEC_COMMAND_ID_SET_WIFI_XXX_WITH_P2P");
        enableWifiConnectivityManager(!this.mConcurrentEnabled);
        this.mPasspointManager.setRequestANQPEnabled(!this.mConcurrentEnabled);
    }

    /* access modifiers changed from: package-private */
    public boolean getConcurrentEnabled() {
        return this.mConcurrentEnabled;
    }

    public void syncSetFccChannel(boolean enable) {
        if (DBG) {
            Log.d(TAG, "syncSetFccChannel: enable = " + enable);
        }
        if (!this.mIsRunning) {
            return;
        }
        if (this.mBlockFccChannelCmd) {
            Log.d(TAG, "Block setFccChannelNative CMD by WlanMacAddress");
        } else {
            this.mWifiNative.setFccChannel(this.mInterfaceName, enable);
        }
    }

    /* access modifiers changed from: package-private */
    public void setFccChannel() {
        if (DBG) {
            Log.d(TAG, "setFccChannel() is called");
        }
        boolean isAirplaneModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        if (isWifiOnly()) {
            syncSetFccChannel(true);
        } else if (isAirplaneModeEnabled) {
            syncSetFccChannel(true);
        } else {
            syncSetFccChannel(false);
        }
    }

    /* access modifiers changed from: protected */
    public WifiMulticastLockManager.FilterController getMcastLockManagerFilterController() {
        return this.mMcastLockManagerFilterController;
    }

    public boolean syncQueryPasspointIcon(AsyncChannel channel, long bssid, String fileName) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putLong("BSSID", bssid);
        bundle.putString(EXTRA_OSU_ICON_QUERY_FILENAME, fileName);
        Message resultMsg = channel.sendMessageSynchronously(CMD_QUERY_OSU_ICON, bundle);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        if (result == 1) {
            return true;
        }
        return false;
    }

    public int matchProviderWithCurrentNetwork(AsyncChannel channel, String fqdn) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return -1;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_MATCH_PROVIDER_NETWORK, fqdn);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public void deauthenticateNetwork(AsyncChannel channel, long holdoff, boolean ess) {
    }

    public void disableEphemeralNetwork(String ssid) {
        if (ssid != null) {
            sendMessage(CMD_DISABLE_EPHEMERAL_NETWORK, ssid);
        }
    }

    public void disconnectCommand() {
        sendMessage(CMD_DISCONNECT);
    }

    public void disconnectCommand(int uid, int reason) {
        sendMessage(CMD_DISCONNECT, uid, reason);
    }

    /* access modifiers changed from: private */
    public void notifyDisconnectInternalReason(int reason) {
        this.mBigDataManager.addOrUpdateValue(8, reason);
    }

    public void report(int reportId, Bundle report) {
        SemWifiIssueDetector semWifiIssueDetector = this.mIssueDetector;
        if (semWifiIssueDetector != null && report != null) {
            semWifiIssueDetector.captureBugReport(reportId, report);
        }
    }

    public void disableP2p(WifiController.P2pDisableListener mP2pDisableCallback) {
        if (this.mP2pSupported) {
            this.mP2pDisableListener = mP2pDisableCallback;
            if (this.mWifiP2pChannel != null) {
                Message message = new Message();
                message.what = WifiP2pServiceImpl.DISABLE_P2P;
                this.mWifiP2pChannel.sendMessage(message);
            }
        }
    }

    public void reconnectCommand(WorkSource workSource) {
        sendMessage(CMD_RECONNECT, workSource);
    }

    public void reassociateCommand() {
        sendMessage(CMD_REASSOCIATE);
    }

    private boolean messageIsNull(Message resultMsg) {
        if (resultMsg != null) {
            return false;
        }
        if (this.mNullMessageCounter.getAndIncrement() <= 0) {
            return true;
        }
        Log.wtf(TAG, "Persistent null Message", new RuntimeException());
        return true;
    }

    public int syncAddOrUpdateNetwork(AsyncChannel channel, WifiConfiguration config) {
        return syncAddOrUpdateNetwork(channel, 0, config);
    }

    public int syncAddOrUpdateNetwork(AsyncChannel channel, int from, WifiConfiguration config) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore add or update network because shutdown is held");
            return -1;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_NETWORK, from, 0, config);
        if (messageIsNull(resultMsg)) {
            return -1;
        }
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks(int uuid, AsyncChannel channel, int targetUid) {
        return syncGetConfiguredNetworks(uuid, channel, 0, targetUid);
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks(int uuid, AsyncChannel channel, int from, int targetUid) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return null;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CONFIGURED_NETWORKS, uuid, targetUid, new Integer(from));
        if (messageIsNull(resultMsg)) {
            return null;
        }
        if (!(resultMsg.obj instanceof List)) {
            Log.e(TAG, "Wrong type object is delivered. request what:131131, reply what:" + resultMsg.what + " arg1:" + resultMsg.arg1 + " arg2:" + resultMsg.arg2 + " obj:" + resultMsg.obj);
            return null;
        }
        List<WifiConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public WifiConfiguration syncGetSpecificNetwork(AsyncChannel channel, int networkId) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return null;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_A_CONFIGURED_NETWORK, networkId);
        if (messageIsNull(resultMsg)) {
            return null;
        }
        if (resultMsg.obj == null || !(resultMsg.obj instanceof WifiConfiguration)) {
            Log.d(TAG, "resultMsg.obj is null or not instance of WifiConfiguration");
            return null;
        }
        WifiConfiguration result = (WifiConfiguration) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetPrivilegedConfiguredNetwork(AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return null;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS);
        if (messageIsNull(resultMsg)) {
            return null;
        }
        List<WifiConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    /* access modifiers changed from: package-private */
    public Map<String, Map<Integer, List<ScanResult>>> syncGetAllMatchingFqdnsForScanResults(List<ScanResult> scanResults, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_ALL_MATCHING_FQDNS_FOR_SCAN_RESULTS, scanResults);
        if (messageIsNull(resultMsg)) {
            return new HashMap();
        }
        Map<String, Map<Integer, List<ScanResult>>> configs = (Map) resultMsg.obj;
        resultMsg.recycle();
        return configs;
    }

    public Map<OsuProvider, List<ScanResult>> syncGetMatchingOsuProviders(List<ScanResult> scanResults, AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return new HashMap();
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_MATCHING_OSU_PROVIDERS, scanResults);
        if (messageIsNull(resultMsg)) {
            return new HashMap();
        }
        Map<OsuProvider, List<ScanResult>> providers = (Map) resultMsg.obj;
        resultMsg.recycle();
        return providers;
    }

    public Map<OsuProvider, PasspointConfiguration> syncGetMatchingPasspointConfigsForOsuProviders(List<OsuProvider> osuProviders, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_MATCHING_PASSPOINT_CONFIGS_FOR_OSU_PROVIDERS, osuProviders);
        if (messageIsNull(resultMsg)) {
            return new HashMap();
        }
        Map<OsuProvider, PasspointConfiguration> result = (Map) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetWifiConfigsForPasspointProfiles(List<String> fqdnList, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_WIFI_CONFIGS_FOR_PASSPOINT_PROFILES, fqdnList);
        if (messageIsNull(resultMsg)) {
            return new ArrayList();
        }
        List<WifiConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncAddOrUpdatePasspointConfig(AsyncChannel channel, PasspointConfiguration config, int uid, String packageName) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_UID, uid);
        bundle.putString(EXTRA_PACKAGE_NAME, packageName);
        bundle.putParcelable(EXTRA_PASSPOINT_CONFIGURATION, config);
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG, bundle);
        if (messageIsNull(resultMsg)) {
            return false;
        }
        if (resultMsg.arg1 == 1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public boolean syncRemovePasspointConfig(AsyncChannel channel, String fqdn) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_PASSPOINT_CONFIG, fqdn);
        if (messageIsNull(resultMsg)) {
            return false;
        }
        if (resultMsg.arg1 == 1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public List<PasspointConfiguration> syncGetPasspointConfigs(AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return new ArrayList();
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_PASSPOINT_CONFIGS);
        if (messageIsNull(resultMsg)) {
            return new ArrayList();
        }
        List<PasspointConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncStartSubscriptionProvisioning(int callingUid, OsuProvider provider, IProvisioningCallback callback, AsyncChannel channel) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message msg = Message.obtain();
        msg.what = CMD_START_SUBSCRIPTION_PROVISIONING;
        msg.arg1 = callingUid;
        msg.obj = callback;
        msg.getData().putParcelable(EXTRA_OSU_PROVIDER, provider);
        Message resultMsg = channel.sendMessageSynchronously(msg);
        if (messageIsNull(resultMsg)) {
            return false;
        }
        if (resultMsg.arg1 != 0) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public long syncGetSupportedFeatures(AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return 0;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_SUPPORTED_FEATURES);
        if (messageIsNull(resultMsg)) {
            return 0;
        }
        long supportedFeatureSet = ((Long) resultMsg.obj).longValue();
        resultMsg.recycle();
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
            return supportedFeatureSet & -385;
        }
        return supportedFeatureSet;
    }

    public WifiLinkLayerStats syncGetLinkLayerStats(AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return null;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_LINK_LAYER_STATS);
        if (messageIsNull(resultMsg)) {
            return null;
        }
        WifiLinkLayerStats result = (WifiLinkLayerStats) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncRemoveNetwork(AsyncChannel channel, int networkId) {
        return syncRemoveNetwork(channel, 0, networkId);
    }

    public boolean syncRemoveNetwork(AsyncChannel channel, int from, int networkId) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_NETWORK, networkId, from);
        if (messageIsNull(resultMsg)) {
            return false;
        }
        if (resultMsg.arg1 != -1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public boolean syncEnableNetwork(AsyncChannel channel, int netId, boolean disableOthers) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_ENABLE_NETWORK, netId, disableOthers);
        if (messageIsNull(resultMsg)) {
            return false;
        }
        if (resultMsg.arg1 != -1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public void forcinglyEnableAllNetworks(AsyncChannel channel) {
        if (this.mIsShutdown) {
            Log.d(TAG, "can't forcingly enable all networks because shutdown is held");
        } else {
            channel.sendMessage(CMD_FORCINGLY_ENABLE_ALL_NETWORKS);
        }
    }

    public boolean syncDisableNetwork(AsyncChannel channel, int netId) {
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message resultMsg = channel.sendMessageSynchronously(151569, netId);
        boolean result = resultMsg.what != 151570;
        if (messageIsNull(resultMsg)) {
            return false;
        }
        resultMsg.recycle();
        return result;
    }

    public void enableRssiPolling(boolean enabled) {
        sendMessage(CMD_ENABLE_RSSI_POLL, enabled, 0);
    }

    public void setHighPerfModeEnabled(boolean enable) {
        sendMessage(CMD_SET_HIGH_PERF_MODE, enable, 0);
    }

    public synchronized void resetSimAuthNetworks(boolean simPresent) {
        sendMessage(CMD_RESET_SIM_NETWORKS, simPresent ? 1 : 0);
    }

    public Network getCurrentNetwork() {
        synchronized (this.mNetworkAgentLock) {
            if (this.mNetworkAgent == null) {
                return null;
            }
            Network network = new Network(this.mNetworkAgent.netId);
            return network;
        }
    }

    public void enableTdls(String remoteMacAddress, boolean enable) {
        sendMessage(CMD_ENABLE_TDLS, (int) enable, 0, remoteMacAddress);
    }

    public void sendBluetoothAdapterStateChange(int state) {
        sendMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, state, 0);
    }

    public void removeAppConfigs(String packageName, int uid) {
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        ai.uid = uid;
        sendMessage(CMD_REMOVE_APP_CONFIGURATIONS, ai);
    }

    public void removeUserConfigs(int userId) {
        sendMessage(CMD_REMOVE_USER_CONFIGURATIONS, userId);
    }

    public void updateBatteryWorkSource(WorkSource newSource) {
        synchronized (this.mRunningWifiUids) {
            if (newSource != null) {
                try {
                    this.mRunningWifiUids.set(newSource);
                } catch (RemoteException e) {
                }
            }
            if (this.mIsRunning) {
                if (!this.mReportedRunning) {
                    this.mBatteryStats.noteWifiRunning(this.mRunningWifiUids);
                    this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                    this.mReportedRunning = true;
                } else if (!this.mLastRunningWifiUids.equals(this.mRunningWifiUids)) {
                    this.mBatteryStats.noteWifiRunningChanged(this.mLastRunningWifiUids, this.mRunningWifiUids);
                    this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                }
            } else if (this.mReportedRunning) {
                this.mBatteryStats.noteWifiStopped(this.mLastRunningWifiUids);
                this.mLastRunningWifiUids.clear();
                this.mReportedRunning = false;
            }
            this.mWakeLock.setWorkSource(newSource);
        }
    }

    public void dumpIpClient(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mIpClient != null) {
            pw.println("IpClient logs have moved to dumpsys network_stack");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        LocalLog localLog;
        ClientModeImpl.super.dump(fd, pw, args);
        this.mSupplicantStateTracker.dump(fd, pw, args);
        pw.println("mLinkProperties " + this.mLinkProperties);
        pw.println("mWifiInfo " + this.mWifiInfo);
        pw.println("mDhcpResults " + this.mDhcpResults);
        pw.println("mNetworkInfo " + this.mNetworkInfo);
        pw.println("mLastSignalLevel " + this.mLastSignalLevel);
        pw.println("mLastBssid " + this.mLastBssid);
        pw.println("mLastNetworkId " + this.mLastNetworkId);
        pw.println("mOperationalMode " + this.mOperationalMode);
        pw.println("mUserWantsSuspendOpt " + this.mUserWantsSuspendOpt);
        pw.println("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        pw.println("mIsWifiOnly : " + isWifiOnly());
        pw.println("FactoryMAC: " + this.mWifiNative.getVendorConnFileInfo(0));
        this.mCountryCode.dump(fd, pw, args);
        this.mNetworkFactory.dump(fd, pw, args);
        this.mUntrustedNetworkFactory.dump(fd, pw, args);
        pw.println("Wlan Wake Reasons:" + this.mWifiNative.getWlanWakeReasonCount());
        pw.println();
        if (this.mWifiGeofenceManager.isSupported()) {
            this.mWifiGeofenceManager.dump(fd, pw, args);
        }
        this.mWifiConfigManager.dump(fd, pw, args);
        pw.println();
        this.mWifiInjector.getCarrierNetworkConfig().dump(fd, pw, args);
        pw.println();
        this.mPasspointManager.dump(pw);
        pw.println();
        this.mWifiDiagnostics.triggerBugReportDataCapture(7);
        this.mWifiDiagnostics.dump(fd, pw, args);
        dumpIpClient(fd, pw, args);
        this.mWifiConnectivityManager.dump(fd, pw, args);
        pw.println("mConcurrentEnabled " + this.mConcurrentEnabled);
        pw.println("mIsImsCallEstablished " + this.mIsImsCallEstablished);
        UnstableApController unstableApController = this.mUnstableApController;
        if (unstableApController != null) {
            unstableApController.dump(fd, pw, args);
        }
        pw.println("W24H (wifi scan auto fav sns agr ...):" + getWifiParameters(false));
        SemWifiIssueDetector semWifiIssueDetector = this.mIssueDetector;
        if (semWifiIssueDetector != null) {
            semWifiIssueDetector.dump(fd, pw, args);
        }
        this.mLinkProbeManager.dump(fd, pw, args);
        this.mWifiInjector.getWifiLastResortWatchdog().dump(fd, pw, args);
        this.mSemSarManager.dump(fd, pw, args);
        pw.println("WifiClientModeImpl connectivity packet log:");
        pw.println("WifiClientModeImpl Name of interface : " + this.mApInterfaceName);
        pw.println();
        LocalLog localLog2 = this.mConnectivityPacketLogForHotspot;
        if (localLog2 != null) {
            localLog2.readOnlyLocalLog().dump(fd, pw, args);
            pw.println("WifiClientModeImpl connectivity packet log:");
            pw.println("WifiClientModeImpl Name of interface : wlan0");
            if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharing() && (localLog = this.mConnectivityPacketLogForWlan0) != null) {
                localLog.readOnlyLocalLog().dump(fd, pw, args);
            }
        }
        runFwLogTimer();
    }

    /* access modifiers changed from: private */
    public SemConnectivityPacketTracker createPacketTracker(InterfaceParams mInterfaceParams, LocalLog mLog) {
        try {
            return new SemConnectivityPacketTracker(getHandler(), mInterfaceParams, mLog);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get ConnectivityPacketTracker object: " + e);
            return null;
        }
    }

    public void handleBootCompleted() {
        sendMessage(CMD_BOOT_COMPLETED);
    }

    public void handleUserSwitch(int userId) {
        sendMessage(CMD_USER_SWITCH, userId);
    }

    public void handleUserUnlock(int userId) {
        sendMessage(CMD_USER_UNLOCK, userId);
    }

    public void handleUserStop(int userId) {
        sendMessage(CMD_USER_STOP, userId);
    }

    private void runFwLogTimer() {
        if (!DBG_PRODUCT_DEV) {
            if (this.mFwLogTimer != null) {
                Log.i(TAG, "mFwLogTimer timer cancled");
                this.mFwLogTimer.cancel();
            }
            this.mFwLogTimer = new Timer();
            this.mFwLogTimer.schedule(new TimerTask() {
                public void run() {
                    Log.i(ClientModeImpl.TAG, "mFwLogTimer timer expired - start folder initialization");
                    ClientModeImpl.this.resetFwLogFolder();
                    Timer unused = ClientModeImpl.this.mFwLogTimer = null;
                }
            }, WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS);
        }
    }

    /* access modifiers changed from: private */
    public void resetFwLogFolder() {
        if (!DBG_PRODUCT_DEV) {
            Log.i(TAG, "resetFwLogFolder");
            try {
                File folder = new File("/data/log/wifi/");
                if (folder.exists()) {
                    removeFolderFiles(folder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!this.mWifiNative.removeVendorLogFiles()) {
                Log.e(TAG, "Removing vendor logs got failed.");
            }
        }
    }

    private void removeFolderFiles(File folder) {
        try {
            File[] logFiles = folder.listFiles();
            if (logFiles != null && logFiles.length > 0) {
                for (File logFile : logFiles) {
                    Log.i(TAG, "WifiStateMachine : " + logFile + " deleted");
                    logFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* access modifiers changed from: private */
    public String getTimeToString() {
        Calendar cal = Calendar.getInstance();
        DecimalFormat df = new DecimalFormat("00", DecimalFormatSymbols.getInstance(Locale.US));
        String month = df.format((long) (cal.get(2) + 1));
        String day = df.format((long) cal.get(5));
        String hour = df.format((long) cal.get(11));
        String min = df.format((long) cal.get(12));
        String sec = df.format((long) cal.get(13));
        String sysdump_time = cal.get(1) + month + day + hour + min;
        Log.i(TAG, "getTimeToString : " + sysdump_time);
        return sysdump_time;
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(Message message, State state) {
        ReportUtil.updateWifiStateMachineProcessMessage(state.getClass().getSimpleName(), message.what);
        this.mMessageHandlingStatus = 0;
        if (this.mVerboseLoggingEnabled) {
            logd(" " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }
    }

    /* access modifiers changed from: protected */
    public boolean recordLogRec(Message msg) {
        if (msg.what != CMD_RSSI_POLL) {
            return true;
        }
        return this.mVerboseLoggingEnabled;
    }

    /* access modifiers changed from: protected */
    public String getLogRecString(Message msg) {
        Message message = msg;
        StringBuilder sb = new StringBuilder();
        sb.append("screen=");
        sb.append(this.mScreenOn ? "on" : "off");
        if (this.mMessageHandlingStatus != 0) {
            sb.append("(");
            sb.append(this.mMessageHandlingStatus);
            sb.append(")");
        }
        if (message.sendingUid > 0 && message.sendingUid != 1010) {
            sb.append(" uid=" + message.sendingUid);
        }
        switch (message.what) {
            case CMD_ADD_OR_UPDATE_NETWORK /*131124*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    sb.append(" ");
                    sb.append(config.configKey());
                    sb.append(" prio=");
                    sb.append(config.priority);
                    sb.append(" status=");
                    sb.append(config.status);
                    if (config.BSSID != null) {
                        sb.append(" ");
                        sb.append(config.BSSID);
                    }
                    WifiConfiguration curConfig = getCurrentWifiConfiguration();
                    if (curConfig != null) {
                        if (!curConfig.configKey().equals(config.configKey())) {
                            sb.append(" current=");
                            sb.append(curConfig.configKey());
                            sb.append(" prio=");
                            sb.append(curConfig.priority);
                            sb.append(" status=");
                            sb.append(curConfig.status);
                            break;
                        } else {
                            sb.append(" is current");
                            break;
                        }
                    }
                }
                break;
            case CMD_ENABLE_NETWORK /*131126*/:
            case 151569:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                String key = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (key != null) {
                    sb.append(" last=");
                    sb.append(key);
                }
                WifiConfiguration config2 = this.mWifiConfigManager.getConfiguredNetwork(message.arg1);
                if (config2 != null && (key == null || !config2.configKey().equals(key))) {
                    sb.append(" target=");
                    sb.append(key);
                    break;
                }
            case CMD_GET_CONFIGURED_NETWORKS /*131131*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" num=");
                sb.append(this.mWifiConfigManager.getConfiguredNetworks().size());
                break;
            case CMD_RSSI_POLL /*131155*/:
            case CMD_ONESHOT_RSSI_POLL /*131156*/:
            case CMD_UNWANTED_NETWORK /*131216*/:
            case 151572:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (!(this.mWifiInfo.getSSID() == null || this.mWifiInfo.getSSID() == null)) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getSSID());
                }
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(" rssi=");
                sb.append(this.mWifiInfo.getRssi());
                sb.append(" f=");
                sb.append(this.mWifiInfo.getFrequency());
                sb.append(" sc=");
                sb.append(this.mWifiInfo.score);
                sb.append(" link=");
                sb.append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", new Object[]{Double.valueOf(this.mWifiInfo.txSuccessRate)}));
                sb.append(String.format(" %.1f,", new Object[]{Double.valueOf(this.mWifiInfo.txRetriesRate)}));
                sb.append(String.format(" %.1f ", new Object[]{Double.valueOf(this.mWifiInfo.txBadRate)}));
                sb.append(String.format(" rx=%.1f", new Object[]{Double.valueOf(this.mWifiInfo.rxSuccessRate)}));
                sb.append(String.format(" bcn=%d", new Object[]{Integer.valueOf(this.mRunningBeaconCount)}));
                sb.append(String.format(" snr=%d", new Object[]{Integer.valueOf(this.mWifiInfo.semGetSnr())}));
                sb.append(String.format(" lqcm_tx=%d", new Object[]{Integer.valueOf(this.mWifiInfo.semGetLqcmTx())}));
                sb.append(String.format(" lqcm_rx=%d", new Object[]{Integer.valueOf(this.mWifiInfo.semGetLqcmRx())}));
                sb.append(String.format(" ap_cu=%d", new Object[]{Integer.valueOf(this.mWifiInfo.semGetApCu())}));
                String report = reportOnTime();
                if (report != null) {
                    sb.append(" ");
                    sb.append(report);
                }
                sb.append(String.format(" score=%d", new Object[]{Integer.valueOf(this.mWifiInfo.score)}));
                break;
            case CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" cur=");
                sb.append(this.mRoamWatchdogCount);
                break;
            case CMD_DISCONNECTING_WATCHDOG_TIMER /*131168*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" cur=");
                sb.append(this.mDisconnectingWatchdogCount);
                break;
            case CMD_IP_CONFIGURATION_LOST /*131211*/:
                int count = -1;
                WifiConfiguration c = getCurrentWifiConfiguration();
                if (c != null) {
                    count = c.getNetworkSelectionStatus().getDisableReasonCounter(4);
                }
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" failures: ");
                sb.append(Integer.toString(count));
                sb.append("/");
                sb.append(Integer.toString(this.mFacade.getIntegerSetting(this.mContext, "wifi_max_dhcp_retry_count", 0)));
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(String.format(" bcn=%d", new Object[]{Integer.valueOf(this.mRunningBeaconCount)}));
                break;
            case CMD_UPDATE_LINKPROPERTIES /*131212*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                    break;
                }
                break;
            case CMD_TARGET_BSSID /*131213*/:
            case CMD_ASSOCIATED_BSSID /*131219*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    sb.append(" BSSID=");
                    sb.append((String) message.obj);
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" Target=");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                break;
            case CMD_START_CONNECT /*131215*/:
            case 151553:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration config3 = this.mWifiConfigManager.getConfiguredNetwork(message.arg1);
                if (config3 != null) {
                    sb.append(" ");
                    sb.append(config3.configKey());
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                WifiConfiguration config4 = getCurrentWifiConfiguration();
                if (config4 != null) {
                    sb.append(config4.configKey());
                    break;
                }
                break;
            case CMD_START_ROAM /*131217*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                ScanResult result = (ScanResult) message.obj;
                if (result != null) {
                    Long now = Long.valueOf(this.mClock.getWallClockMillis());
                    sb.append(" bssid=");
                    sb.append(result.BSSID);
                    sb.append(" rssi=");
                    sb.append(result.level);
                    sb.append(" freq=");
                    sb.append(result.frequency);
                    if (result.seen <= 0 || result.seen >= now.longValue()) {
                        sb.append(" !seen=");
                        sb.append(result.seen);
                    } else {
                        sb.append(" seen=");
                        sb.append(now.longValue() - result.seen);
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                sb.append(" fail count=");
                sb.append(Integer.toString(this.mRoamFailCount));
                break;
            case CMD_IP_REACHABILITY_LOST /*131221*/:
                if (message.obj != null) {
                    sb.append(" ");
                    sb.append((String) message.obj);
                    break;
                }
                break;
            case CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
            case CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
            case CMD_RSSI_THRESHOLD_BREACHED /*131236*/:
                sb.append(" rssi=");
                sb.append(Integer.toString(message.arg1));
                sb.append(" thresholds=");
                sb.append(Arrays.toString(this.mRssiRanges));
                break;
            case CMD_IPV4_PROVISIONING_SUCCESS /*131272*/:
                sb.append(" ");
                sb.append(message.obj);
                break;
            case CMD_INSTALL_PACKET_FILTER /*131274*/:
                sb.append(" len=" + ((byte[]) message.obj).length);
                break;
            case CMD_SET_FALLBACK_PACKET_FILTERING /*131275*/:
                sb.append(" enabled=" + ((Boolean) message.obj).booleanValue());
                break;
            case CMD_USER_SWITCH /*131277*/:
                sb.append(" userId=");
                sb.append(Integer.toString(message.arg1));
                break;
            case CMD_PRE_DHCP_ACTION /*131327*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" txpkts=");
                sb.append(this.mWifiInfo.txSuccess);
                sb.append(",");
                sb.append(this.mWifiInfo.txBad);
                sb.append(",");
                sb.append(this.mWifiInfo.txRetries);
                break;
            case CMD_POST_DHCP_ACTION /*131329*/:
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                    break;
                }
                break;
            case CMD_GET_A_CONFIGURED_NETWORK /*131581*/:
                sb.append(" networkId=");
                sb.append(Integer.toString(message.arg1));
                break;
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    NetworkInfo info = (NetworkInfo) message.obj;
                    NetworkInfo.State state = info.getState();
                    NetworkInfo.DetailedState detailedState = info.getDetailedState();
                    if (state != null) {
                        sb.append(" st=");
                        sb.append(state);
                    }
                    if (detailedState != null) {
                        sb.append("/");
                        sb.append(detailedState);
                        break;
                    }
                }
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" ");
                sb.append(this.mLastBssid);
                sb.append(" nid=");
                sb.append(this.mLastNetworkId);
                WifiConfiguration config5 = getCurrentWifiConfiguration();
                if (config5 != null) {
                    sb.append(" ");
                    sb.append(config5.configKey());
                }
                String key2 = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (key2 != null) {
                    sb.append(" last=");
                    sb.append(key2);
                    break;
                }
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                if (message.obj != null) {
                    sb.append(" ");
                    sb.append((String) message.obj);
                }
                sb.append(" nid=");
                sb.append(message.arg1);
                sb.append(" reason=");
                sb.append(message.arg2);
                if (this.mLastBssid != null) {
                    sb.append(" lastbssid=");
                    sb.append(this.mLastBssid);
                }
                if (this.mWifiInfo.getFrequency() != -1) {
                    sb.append(" freq=");
                    sb.append(this.mWifiInfo.getFrequency());
                    sb.append(" rssi=");
                    sb.append(this.mWifiInfo.getRssi());
                    break;
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                if (stateChangeResult != null) {
                    sb.append(stateChangeResult.toString());
                    break;
                }
                break;
            case 147499:
                sb.append(" ");
                sb.append(" timedOut=" + Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                String bssid = (String) message.obj;
                if (bssid != null && bssid.length() > 0) {
                    sb.append(" ");
                    sb.append(bssid);
                }
                sb.append(" blacklist=" + Boolean.toString(this.mDidBlackListBSSID));
                break;
            case WifiMonitor.ANQP_DONE_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    AnqpEvent anqpEvent = (AnqpEvent) message.obj;
                    if (anqpEvent.getBssid() != 0) {
                        sb.append(" BSSID=");
                        sb.append(Utils.macToString(anqpEvent.getBssid()));
                        break;
                    }
                }
                break;
            case WifiMonitor.BSSID_PRUNED_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    sb.append(" ");
                    sb.append(message.obj.toString());
                    break;
                }
                break;
            case 151556:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration config6 = (WifiConfiguration) message.obj;
                if (config6 != null) {
                    sb.append(" ");
                    sb.append(config6.configKey());
                    sb.append(" nid=");
                    sb.append(config6.networkId);
                    if (config6.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (config6.preSharedKey != null) {
                        sb.append(" hasPSK");
                    }
                    if (config6.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (config6.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(config6.creatorUid);
                    sb.append(" suid=");
                    sb.append(config6.lastUpdateUid);
                    WifiConfiguration.NetworkSelectionStatus netWorkSelectionStatus = config6.getNetworkSelectionStatus();
                    sb.append(" ajst=");
                    sb.append(netWorkSelectionStatus.getNetworkStatusString());
                    break;
                }
                break;
            case 151559:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration config7 = (WifiConfiguration) message.obj;
                if (config7 != null) {
                    sb.append(" ");
                    sb.append(config7.configKey());
                    sb.append(" nid=");
                    sb.append(config7.networkId);
                    if (config7.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (config7.preSharedKey != null && !config7.preSharedKey.equals("*")) {
                        sb.append(" hasPSK");
                    }
                    if (config7.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (config7.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(config7.creatorUid);
                    sb.append(" suid=");
                    sb.append(config7.lastUpdateUid);
                    break;
                }
                break;
            default:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                break;
        }
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public String getWhatToString(int what) {
        String s = sGetWhatToString.get(what);
        if (s != null) {
            return s;
        }
        switch (what) {
            case 69632:
                return "CMD_CHANNEL_HALF_CONNECTED";
            case 69636:
                return "CMD_CHANNEL_DISCONNECTED";
            case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                return "GROUP_CREATING_TIMED_OUT";
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                return "P2P_CONNECTION_CHANGED";
            case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                return "DISCONNECT_WIFI_REQUEST";
            case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                return "DISCONNECT_WIFI_RESPONSE";
            case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                return "SET_MIRACAST_MODE";
            case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                return "BLOCK_DISCOVERY";
            case WifiP2pServiceImpl.DISABLE_P2P_RSP /*143395*/:
                return "DISABLE_P2P_RSP";
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                return "NETWORK_CONNECTION_EVENT";
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                return "NETWORK_DISCONNECTION_EVENT";
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                return "SUPPLICANT_STATE_CHANGE_EVENT";
            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                return "AUTHENTICATION_FAILURE_EVENT";
            case WifiMonitor.SUP_BIGDATA_EVENT:
                return "WifiMonitor.SUP_BIGDATA_EVENT";
            case WifiMonitor.SUP_REQUEST_IDENTITY:
                return "SUP_REQUEST_IDENTITY";
            case 147499:
                return "ASSOCIATION_REJECTION_EVENT";
            case WifiMonitor.ANQP_DONE_EVENT:
                return "ANQP_DONE_EVENT";
            case WifiMonitor.BSSID_PRUNED_EVENT:
                return "WifiMonitor.BSSID_PRUNED_EVENT";
            case WifiMonitor.GAS_QUERY_START_EVENT:
                return "GAS_QUERY_START_EVENT";
            case WifiMonitor.GAS_QUERY_DONE_EVENT:
                return "GAS_QUERY_DONE_EVENT";
            case WifiMonitor.RX_HS20_ANQP_ICON_EVENT:
                return "RX_HS20_ANQP_ICON_EVENT";
            case WifiMonitor.HS20_REMEDIATION_EVENT:
                return "HS20_REMEDIATION_EVENT";
            case 147527:
                return "WifiMonitor.EAP_EVENT_MESSAGE";
            case 151553:
                return "CONNECT_NETWORK";
            case 151556:
                return "FORGET_NETWORK";
            case 151559:
                return "SAVE_NETWORK";
            case 151569:
                return "DISABLE_NETWORK";
            case 151572:
                return "RSSI_PKTCNT_FETCH";
            default:
                return "what:" + Integer.toString(what);
        }
    }

    /* access modifiers changed from: private */
    public void handleScreenStateChanged(boolean screenOn) {
        this.mScreenOn = screenOn;
        if (this.mVerboseLoggingEnabled) {
            logd(" handleScreenStateChanged Enter: screenOn=" + screenOn + " mUserWantsSuspendOpt=" + this.mUserWantsSuspendOpt + " state " + getCurrentState().getName() + " suppState:" + this.mSupplicantStateTracker.getSupplicantStateName());
        }
        enableRssiPolling(screenOn || mRssiPollingScreenOffEnabled != 0);
        if (this.mUserWantsSuspendOpt.get()) {
            int shouldReleaseWakeLock = 0;
            if (screenOn) {
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 0, 0);
            } else {
                if (isConnected()) {
                    this.mSuspendWakeLock.acquire(2000);
                    shouldReleaseWakeLock = 1;
                }
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 1, shouldReleaseWakeLock);
            }
        }
        getWifiLinkLayerStats();
        this.mOnTimeScreenStateChange = this.mOnTime;
        this.mLastScreenStateChangeTimeStamp = this.mLastLinkLayerStatsUpdate;
        this.mWifiMetrics.setScreenState(screenOn);
        this.mWifiConnectivityManager.handleScreenStateChanged(screenOn);
        this.mNetworkFactory.handleScreenStateChanged(screenOn);
        WifiLockManager wifiLockManager = this.mWifiInjector.getWifiLockManager();
        if (wifiLockManager != null) {
            wifiLockManager.handleScreenStateChanged(screenOn);
        }
        this.mSarManager.handleScreenStateChanged(screenOn);
        if (this.mVerboseLoggingEnabled) {
            log("handleScreenStateChanged Exit: " + screenOn);
        }
    }

    /* access modifiers changed from: private */
    public boolean checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        if (this.mCm != null) {
            return true;
        }
        Log.e(TAG, "Cannot retrieve connectivity service");
        return false;
    }

    /* access modifiers changed from: private */
    public void setSuspendOptimizationsNative(int reason, boolean enabled) {
        if (this.mVerboseLoggingEnabled) {
            log("setSuspendOptimizationsNative: " + reason + " " + enabled + " -want " + this.mUserWantsSuspendOpt.get() + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= ~reason;
            if (this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get()) {
                if (this.mVerboseLoggingEnabled) {
                    log("setSuspendOptimizationsNative do it " + reason + " " + enabled + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
                }
                this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, true);
                return;
            }
            return;
        }
        this.mSuspendOptNeedsDisabled |= reason;
        this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, false);
    }

    /* access modifiers changed from: private */
    public void setSuspendOptimizations(int reason, boolean enabled) {
        if (this.mVerboseLoggingEnabled) {
            log("setSuspendOptimizations: " + reason + " " + enabled);
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= ~reason;
        } else {
            this.mSuspendOptNeedsDisabled |= reason;
        }
        if (this.mVerboseLoggingEnabled) {
            log("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        }
    }

    /* access modifiers changed from: private */
    public void knoxAutoSwitchPolicy(int newRssi) {
        int thresholdRssi;
        int i = newRssi;
        if (this.mLastConnectedTime == -1) {
            logd("knoxCustom WifiAutoSwitch: not connected yet");
        } else if (i == -127) {
            logd("knoxCustom WifiAutoSwitch: newRssi is invalid");
        } else {
            if (this.customDeviceManager == null) {
                this.customDeviceManager = CustomDeviceManagerProxy.getInstance();
            }
            if (this.customDeviceManager.getWifiAutoSwitchState() && i < (thresholdRssi = this.customDeviceManager.getWifiAutoSwitchThreshold())) {
                if (DBG) {
                    logd("KnoxCustom WifiAutoSwitch: current = " + i);
                }
                long now = this.mClock.getElapsedSinceBootMillis();
                if (DBG) {
                    logd("KnoxCustom WifiAutoSwitch: last check was " + (now - this.mLastConnectedTime) + " ms ago");
                }
                int delay = this.customDeviceManager.getWifiAutoSwitchDelay();
                if (now < this.mLastConnectedTime + (((long) delay) * 1000)) {
                    logd("KnoxCustom WifiAutoSwitch: delay " + delay);
                    return;
                }
                int bestRssi = thresholdRssi;
                int bestNetworkId = -1;
                List<ScanResult> scanResults = this.mScanRequestProxy.getScanResults();
                for (WifiConfiguration config : this.mWifiConfigManager.getSavedNetworks(1010)) {
                    for (ScanResult result : scanResults) {
                        if (config.SSID.equals("\"" + result.SSID + "\"")) {
                            if (DBG) {
                                logd("KnoxCustom WifiAutoSwitch: " + config.SSID + " = " + result.level);
                            }
                            if (result.level > bestRssi) {
                                bestRssi = result.level;
                                bestNetworkId = config.networkId;
                            }
                        }
                        int i2 = newRssi;
                    }
                    int i3 = newRssi;
                }
                if (bestNetworkId != -1) {
                    if (DBG) {
                        logd("KnoxCustom WifiAutoSwitch: switching to " + bestNetworkId);
                    }
                    notifyDisconnectInternalReason(15);
                    this.mWifiNative.disconnect(this.mInterfaceName);
                    this.mWifiConfigManager.enableNetwork(bestNetworkId, true, 1000);
                    this.mWifiNative.reconnect(this.mInterfaceName);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void fetchRssiLinkSpeedAndFrequencyNative() {
        WifiNative.SignalPollResult pollResult = this.mWifiNative.signalPoll(this.mInterfaceName);
        if (pollResult != null) {
            int newRssi = pollResult.currentRssi;
            int newTxLinkSpeed = pollResult.txBitrate;
            int newFrequency = pollResult.associationFrequency;
            int newRxLinkSpeed = pollResult.rxBitrate;
            if (this.mVerboseLoggingEnabled) {
                logd("fetchRssiLinkSpeedAndFrequencyNative rssi=" + newRssi + " TxLinkspeed=" + newTxLinkSpeed + " freq=" + newFrequency + " RxLinkSpeed=" + newRxLinkSpeed);
            }
            if (newRssi <= -127 || newRssi >= 200) {
                this.mWifiInfo.setRssi(WifiMetrics.MIN_RSSI_DELTA);
                updateCapabilities();
            } else {
                if (newRssi > 0) {
                    Log.wtf(TAG, "Error! +ve value RSSI: " + newRssi);
                    newRssi += -256;
                }
                this.mWifiInfo.setRssi(newRssi);
                int newSignalLevel = WifiManager.calculateSignalLevel(newRssi, 5);
                if (newSignalLevel != this.mLastSignalLevel) {
                    updateCapabilities();
                    sendRssiChangeBroadcast(newRssi);
                    this.mBigDataManager.addOrUpdateValue(9, newTxLinkSpeed);
                }
                this.mLastSignalLevel = newSignalLevel;
            }
            if (newTxLinkSpeed > 0) {
                this.mWifiInfo.setLinkSpeed(newTxLinkSpeed);
                this.mWifiInfo.setTxLinkSpeedMbps(newTxLinkSpeed);
            }
            if (newRxLinkSpeed > 0) {
                this.mWifiInfo.setRxLinkSpeedMbps(newRxLinkSpeed);
            }
            if (newFrequency > 0) {
                this.mWifiInfo.setFrequency(newFrequency);
            }
            this.mWifiInfo.semSetBcnCnt(this.mRunningBeaconCount);
            int snr = this.mWifiNative.getSnr(this.mInterfaceName);
            int lqcmReport = this.mWifiNative.getLqcmReport(this.mInterfaceName);
            int ApCu = this.mWifiNative.getApCu(this.mInterfaceName);
            this.mWifiInfo.semSetSnr(snr > 0 ? snr : 0);
            int lqcmTxIndex = 255;
            int i = -1;
            int lqcmRxIndex = lqcmReport != -1 ? (16711680 & lqcmReport) >> 16 : 255;
            if (lqcmReport != -1) {
                lqcmTxIndex = (65280 & lqcmReport) >> 8;
            }
            this.mWifiInfo.semSetLqcmTx(lqcmTxIndex);
            this.mWifiInfo.semSetLqcmRx(lqcmRxIndex);
            ExtendedWifiInfo extendedWifiInfo = this.mWifiInfo;
            if (ApCu > -1) {
                i = ApCu;
            }
            extendedWifiInfo.semSetApCu(i);
            this.mWifiConfigManager.updateScanDetailCacheFromWifiInfo(this.mWifiInfo);
            this.mWifiMetrics.handlePollResult(this.mWifiInfo);
        }
    }

    /* access modifiers changed from: private */
    public void cleanWifiScore() {
        ExtendedWifiInfo extendedWifiInfo = this.mWifiInfo;
        extendedWifiInfo.txBadRate = 0.0d;
        extendedWifiInfo.txSuccessRate = 0.0d;
        extendedWifiInfo.txRetriesRate = 0.0d;
        extendedWifiInfo.rxSuccessRate = 0.0d;
        this.mWifiScoreReport.reset();
        this.mLastLinkLayerStats = null;
    }

    private void checkAndResetMtu() {
        int mtu;
        LinkProperties linkProperties = this.mLinkProperties;
        if (linkProperties != null && (mtu = linkProperties.getMtu()) != 1500 && mtu != 0) {
            Log.i(TAG, "reset MTU value from " + mtu);
            this.mWifiNative.initializeMtu(this.mInterfaceName);
        }
    }

    /* access modifiers changed from: private */
    public void updateLinkProperties(LinkProperties newLp) {
        log("Link configuration changed for netId: " + this.mLastNetworkId + " old: " + this.mLinkProperties + " new: " + newLp);
        this.mOldLinkProperties = this.mLinkProperties;
        this.mLinkProperties = newLp;
        WifiNetworkAgent wifiNetworkAgent = this.mNetworkAgent;
        if (wifiNetworkAgent != null) {
            wifiNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        if (getNetworkDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            sendLinkConfigurationChangedBroadcast();
            if (detectIpv6ProvisioningFailure(this.mOldLinkProperties, this.mLinkProperties)) {
                handleWifiNetworkCallbacks(11);
            }
        }
        if (this.mVerboseLoggingEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateLinkProperties nid: " + this.mLastNetworkId);
            sb.append(" state: " + getNetworkDetailedState());
            if (this.mLinkProperties != null) {
                sb.append(" ");
                sb.append(getLinkPropertiesSummary(this.mLinkProperties));
            }
            logd(sb.toString());
        }
        handleWifiNetworkCallbacks(7);
    }

    private void clearLinkProperties() {
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        this.mLinkProperties.clear();
        WifiNetworkAgent wifiNetworkAgent = this.mNetworkAgent;
        if (wifiNetworkAgent != null) {
            wifiNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
    }

    /* access modifiers changed from: private */
    public void sendRssiChangeBroadcast(int newRssi) {
        try {
            this.mBatteryStats.noteWifiRssiChanged(newRssi);
        } catch (RemoteException e) {
        }
        StatsLog.write(38, WifiManager.calculateSignalLevel(newRssi, 5));
        Intent intent = new Intent("android.net.wifi.RSSI_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("newRssi", newRssi);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.ACCESS_WIFI_STATE");
    }

    /* access modifiers changed from: private */
    public void sendNetworkStateChangeBroadcast(String bssid) {
        Intent intent = new Intent("android.net.wifi.STATE_CHANGE");
        intent.addFlags(67108864);
        NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
        networkInfo.setExtraInfo((String) null);
        intent.putExtra("networkInfo", networkInfo);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if ("VZW".equals(SemCscFeature.getInstance().getString(CscFeatureTagCOMMON.TAG_CSCFEATURE_COMMON_CONFIGIMPLICITBROADCASTS))) {
            Intent cloneIntent = (Intent) intent.clone();
            cloneIntent.setPackage("com.verizon.mips.services");
            this.mContext.sendBroadcastAsUser(cloneIntent, UserHandle.ALL);
        }
        if (SemFloatingFeature.getInstance().getBoolean("SEC_FLOATING_FEATURE_WLAN_SUPPORT_SECURE_WIFI")) {
            PackageManager pm = this.mContext.getPackageManager();
            if (pm.checkSignatures("android", "com.samsung.android.fast") == 0) {
                try {
                    pm.getPackageInfo("com.samsung.android.fast", 0);
                    Intent cloneIntent2 = (Intent) intent.clone();
                    cloneIntent2.setPackage("com.samsung.android.fast");
                    this.mContext.sendBroadcastAsUser(cloneIntent2, UserHandle.ALL);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        if ("TencentSecurityWiFi".equals(CONFIG_SECURE_SVC_INTEGRATION)) {
            try {
                this.mContext.getPackageManager().getPackageInfo("com.samsung.android.tencentwifisecurity", 0);
                Intent cloneIntent3 = (Intent) intent.clone();
                cloneIntent3.setPackage("com.samsung.android.tencentwifisecurity");
                this.mContext.sendBroadcastAsUser(cloneIntent3, UserHandle.ALL);
            } catch (PackageManager.NameNotFoundException e2) {
            }
        }
    }

    private void sendLinkConfigurationChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("linkProperties", new LinkProperties(this.mLinkProperties));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendSupplicantConnectionChangedBroadcast(boolean connected) {
        Intent intent = new Intent("android.net.wifi.supplicant.CONNECTION_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("connected", connected);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public boolean setNetworkDetailedState(NetworkInfo.DetailedState state) {
        boolean hidden = false;
        if (this.mIsAutoRoaming) {
            hidden = true;
        }
        if (this.mVerboseLoggingEnabled) {
            log("setDetailed state, old =" + this.mNetworkInfo.getDetailedState() + " and new state=" + state + " hidden=" + hidden);
        }
        if (OpBrandingLoader.Vendor.VZW == mOpBranding && this.mSemWifiHiddenNetworkTracker != null && state == NetworkInfo.DetailedState.CONNECTING) {
            this.mSemWifiHiddenNetworkTracker.stopTracking();
        }
        if (hidden || state == this.mNetworkInfo.getDetailedState()) {
            return false;
        }
        this.mNetworkInfo.setDetailedState(state, (String) null, (String) null);
        WifiNetworkAgent wifiNetworkAgent = this.mNetworkAgent;
        if (wifiNetworkAgent != null) {
            wifiNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        sendNetworkStateChangeBroadcast((String) null);
        return true;
    }

    /* access modifiers changed from: private */
    public NetworkInfo.DetailedState getNetworkDetailedState() {
        return this.mNetworkInfo.getDetailedState();
    }

    /* access modifiers changed from: private */
    public SupplicantState handleSupplicantStateChange(Message message) {
        StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
        SupplicantState state = stateChangeResult.state;
        this.mWifiScoreCard.noteSupplicantStateChanging(this.mWifiInfo, state);
        this.mWifiInfo.setSupplicantState(state);
        if (SupplicantState.isConnecting(state)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
            this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
            this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
        } else {
            this.mWifiInfo.setNetworkId(-1);
            this.mWifiInfo.setBSSID((String) null);
            this.mWifiInfo.setSSID((WifiSsid) null);
        }
        updateL2KeyAndGroupHint();
        updateCapabilities();
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config != null) {
            this.mWifiInfo.setEphemeral(config.ephemeral);
            this.mWifiInfo.setTrusted(config.trusted);
            this.mWifiInfo.setOsuAp(config.osu);
            if (config.fromWifiNetworkSpecifier || config.fromWifiNetworkSuggestion) {
                this.mWifiInfo.setNetworkSuggestionOrSpecifierPackageName(config.creatorName);
            }
            ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId);
            if (scanDetailCache != null) {
                ScanDetail scanDetail = scanDetailCache.getScanDetail(stateChangeResult.BSSID);
                if (scanDetail != null) {
                    updateWifiInfoForVendors(scanDetail.getScanResult());
                    this.mWifiInfo.setFrequency(scanDetail.getScanResult().frequency);
                    NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                    if (networkDetail != null && networkDetail.getAnt() == NetworkDetail.Ant.ChargeablePublic) {
                        Log.d(TAG, "setMeteredHint by ChargeablePublic");
                        this.mWifiInfo.setMeteredHint(true);
                    }
                    this.mWifiInfo.setWifiMode(scanDetail.getScanResult().wifiMode);
                } else {
                    Log.d(TAG, "can't update vendor infos, bssid: " + stateChangeResult.BSSID);
                }
            }
        }
        this.mSupplicantStateTracker.sendMessage(Message.obtain(message));
        this.mWifiScoreCard.noteSupplicantStateChanged(this.mWifiInfo);
        return state;
    }

    private void updateL2KeyAndGroupHint() {
        if (this.mIpClient != null) {
            Pair<String, String> p = this.mWifiScoreCard.getL2KeyAndGroupHint(this.mWifiInfo);
            if (p.equals(this.mLastL2KeyAndGroupHint)) {
                return;
            }
            if (this.mIpClient.setL2KeyAndGroupHint((String) p.first, (String) p.second)) {
                this.mLastL2KeyAndGroupHint = p;
            } else {
                this.mLastL2KeyAndGroupHint = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNetworkDisconnect() {
        if (this.mVerboseLoggingEnabled) {
            log("handleNetworkDisconnect: Stopping DHCP and clearing IP stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        WifiConfiguration wifiConfig = getCurrentWifiConfiguration();
        if (wifiConfig != null) {
            ScanResultMatchInfo fromWifiConfiguration = ScanResultMatchInfo.fromWifiConfiguration(wifiConfig);
            this.mWifiNetworkSuggestionsManager.handleDisconnect(wifiConfig, getCurrentBSSID());
        }
        stopRssiMonitoringOffload();
        clearTargetBssid("handleNetworkDisconnect");
        stopIpClient();
        this.mWifiScoreReport.reset();
        this.mWifiInfo.reset();
        this.mIsAutoRoaming = false;
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        synchronized (this.mNetworkAgentLock) {
            if (this.mNetworkAgent != null) {
                checkAndResetMtu();
                this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
                this.mNetworkAgent = null;
            }
        }
        clearLinkProperties();
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        this.mLastBssid = null;
        this.mLastLinkLayerStats = null;
        registerDisconnected();
        this.mLastNetworkId = -1;
        this.mWifiScoreCard.resetConnectionState();
        updateL2KeyAndGroupHint();
    }

    /* access modifiers changed from: package-private */
    public void handlePreDhcpSetup() {
        if (!this.mBluetoothConnectionActive) {
            this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 1);
        }
        setSuspendOptimizationsNative(1, false);
        setPowerSave(false);
        getWifiLinkLayerStats();
        if (this.mWifiP2pChannel != null) {
            Message msg = new Message();
            msg.what = WifiP2pServiceImpl.BLOCK_DISCOVERY;
            msg.arg1 = 1;
            msg.arg2 = CMD_PRE_DHCP_ACTION_COMPLETE;
            msg.obj = this;
            this.mWifiP2pChannel.sendMessage(msg);
        } else {
            sendMessage(CMD_PRE_DHCP_ACTION_COMPLETE);
        }
        handleWifiNetworkCallbacks(8);
    }

    /* access modifiers changed from: package-private */
    public void handlePostDhcpSetup() {
        setSuspendOptimizationsNative(1, true);
        setPowerSave(true);
        p2pSendMessage(WifiP2pServiceImpl.BLOCK_DISCOVERY, 0);
        if (!this.mHandleIfaceIsUp) {
            Log.w(TAG, "handlePostDhcpSetup, mHandleIfaceIsUp is false. skip setBluetoothCoexistenceMode");
            return;
        }
        this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 2);
        handleWifiNetworkCallbacks(9);
    }

    public boolean setPowerSave(boolean ps) {
        if (this.mInterfaceName != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Setting power save for: " + this.mInterfaceName + " to: " + ps);
            }
            if (!this.mHandleIfaceIsUp) {
                Log.w(TAG, "setPowerSave, mHandleIfaceIsUp is false");
                return false;
            }
            this.mWifiNative.setPowerSave(this.mInterfaceName, ps);
            return true;
        }
        Log.e(TAG, "Failed to setPowerSave, interfaceName is null");
        return false;
    }

    public boolean setLowLatencyMode(boolean enabled) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Setting low latency mode to " + enabled);
        }
        if (this.mWifiNative.setLowLatencyMode(enabled)) {
            return true;
        }
        Log.e(TAG, "Failed to setLowLatencyMode");
        return false;
    }

    /* access modifiers changed from: private */
    public void reportConnectionAttemptStart(WifiConfiguration config, String targetBSSID, int roamType) {
        this.mWifiMetrics.startConnectionEvent(config, targetBSSID, roamType);
        this.mWifiDiagnostics.reportConnectionEvent((byte) 0);
        this.mWrongPasswordNotifier.onNewConnectionAttempt();
        removeMessages(CMD_DIAGS_CONNECT_TIMEOUT);
        sendMessageDelayed(CMD_DIAGS_CONNECT_TIMEOUT, 60000);
    }

    private void handleConnectionAttemptEndForDiagnostics(int level2FailureCode) {
        if (level2FailureCode != 1 && level2FailureCode != 5) {
            removeMessages(CMD_DIAGS_CONNECT_TIMEOUT);
            this.mWifiDiagnostics.reportConnectionEvent((byte) 2);
        }
    }

    /* access modifiers changed from: private */
    public void reportConnectionAttemptEnd(int level2FailureCode, int connectivityFailureCode, int level2FailureReason) {
        if (level2FailureCode != 1) {
            this.mWifiScoreCard.noteConnectionFailure(this.mWifiInfo, level2FailureCode, connectivityFailureCode);
        }
        WifiConfiguration configuration = getCurrentWifiConfiguration();
        if (configuration == null) {
            configuration = getTargetWifiConfiguration();
        }
        this.mWifiMetrics.endConnectionEvent(level2FailureCode, connectivityFailureCode, level2FailureReason);
        this.mWifiConnectivityManager.handleConnectionAttemptEnded(level2FailureCode);
        if (configuration != null) {
            this.mNetworkFactory.handleConnectionAttemptEnded(level2FailureCode, configuration);
            this.mWifiNetworkSuggestionsManager.handleConnectionAttemptEnded(level2FailureCode, configuration, getCurrentBSSID());
        }
        handleConnectionAttemptEndForDiagnostics(level2FailureCode);
    }

    /* access modifiers changed from: private */
    public void handleIPv4Success(DhcpResults dhcpResults) {
        Inet4Address addr;
        if (this.mVerboseLoggingEnabled) {
            logd("handleIPv4Success <" + dhcpResults.toString() + ">");
            StringBuilder sb = new StringBuilder();
            sb.append("link address ");
            sb.append(dhcpResults.ipAddress);
            logd(sb.toString());
        }
        synchronized (this.mDhcpResultsLock) {
            this.mDhcpResults = dhcpResults;
            ReportUtil.updateDhcpResults(this.mDhcpResults);
            addr = (Inet4Address) dhcpResults.ipAddress.getAddress();
        }
        if (this.mIsAutoRoaming && this.mWifiInfo.getIpAddress() != NetworkUtils.inetAddressToInt(addr)) {
            logd("handleIPv4Success, roaming and address changed" + this.mWifiInfo + " got: " + addr);
        }
        this.mWifiInfo.setInetAddress(addr);
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config != null) {
            this.mWifiInfo.setEphemeral(config.ephemeral);
            this.mWifiInfo.setTrusted(config.trusted);
        }
        if (dhcpResults.hasMeteredHint()) {
            this.mWifiInfo.setMeteredHint(true);
        }
        updateCapabilities(config);
    }

    /* access modifiers changed from: private */
    public void handleSuccessfulIpConfiguration() {
        this.mLastSignalLevel = -1;
        WifiConfiguration c = getCurrentWifiConfiguration();
        if (c != null) {
            c.getNetworkSelectionStatus().clearDisableReasonCounter(4);
            updateCapabilities(c);
        }
        this.mWifiScoreCard.noteIpConfiguration(this.mWifiInfo);
    }

    /* access modifiers changed from: private */
    public void handleIPv4Failure() {
        this.mWifiDiagnostics.triggerBugReportDataCapture(4);
        if (this.mVerboseLoggingEnabled) {
            int count = -1;
            WifiConfiguration config = getCurrentWifiConfiguration();
            if (config != null) {
                count = config.getNetworkSelectionStatus().getDisableReasonCounter(4);
            }
            log("DHCP failure count=" + count);
        }
        reportConnectionAttemptEnd(10, 2, 0);
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            logd("handleIPv4Failure");
        }
    }

    /* access modifiers changed from: private */
    public void handleIpConfigurationLost() {
        this.mWifiInfo.setInetAddress((InetAddress) null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiConfigManager.updateNetworkSelectionStatus(this.mLastNetworkId, 4);
        this.mWifiNative.disconnect(this.mInterfaceName);
    }

    /* access modifiers changed from: private */
    public void handleIpReachabilityLost() {
        this.mWifiScoreCard.noteIpReachabilityLost(this.mWifiInfo);
        this.mWifiInfo.setInetAddress((InetAddress) null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiNative.disconnect(this.mInterfaceName);
    }

    private String macAddressFromRoute(String ipAddress) {
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
                    if (ipAddress.equals(ip)) {
                        macAddress = mac;
                        break;
                    }
                }
            }
            if (macAddress == null) {
                loge("Did not find remoteAddress {" + ipAddress + "} in /proc/net/arp");
            }
            try {
                reader2.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e2) {
            loge("Could not open /proc/net/arp to lookup mac address");
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e3) {
            loge("Could not read /proc/net/arp to lookup mac address");
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
        return macAddress;
    }

    /* access modifiers changed from: private */
    public boolean isPermanentWrongPasswordFailure(WifiConfiguration network, int reasonCode) {
        if (reasonCode != 2) {
            return false;
        }
        if (network == null || !network.getNetworkSelectionStatus().getHasEverConnected()) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void registerNetworkFactory() {
        if (checkAndSetConnectivityInstance()) {
            this.mNetworkFactory.register();
            this.mUntrustedNetworkFactory.register();
        }
    }

    public void sendBroadcastIssueTrackerSysDump(int reason) {
        Log.i(TAG, "sendBroadcastIssueTrackerSysDump reason : " + reason);
        if (mIssueTrackerOn) {
            Log.i(TAG, "sendBroadcastIssueTrackerSysDump mIssueTrackerOn true");
            Intent issueTrackerIntent = new Intent("com.sec.android.ISSUE_TRACKER_ACTION");
            issueTrackerIntent.putExtra("ERRPKG", "WifiStateMachine");
            if (reason == 0) {
                issueTrackerIntent.putExtra("ERRCODE", -110);
                issueTrackerIntent.putExtra("ERRNAME", "HANGED");
                issueTrackerIntent.putExtra("ERRMSG", "Wi-Fi chip HANGED");
            } else if (reason == 1) {
                issueTrackerIntent.putExtra("ERRCODE", -110);
                issueTrackerIntent.putExtra("ERRNAME", "UNWANTED");
                issueTrackerIntent.putExtra("ERRMSG", "Wi-Fi UNWANTED happend");
            } else if (reason == 2) {
                issueTrackerIntent.putExtra("ERRCODE", -110);
                issueTrackerIntent.putExtra("ERRNAME", WifiBigDataLogManager.FEATURE_DISC);
                issueTrackerIntent.putExtra("ERRMSG", "Wi-Fi DISC happend");
            }
            this.mContext.sendBroadcastAsUser(issueTrackerIntent, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: private */
    public void getAdditionalWifiServiceInterfaces() {
        WifiP2pServiceImpl wifiP2pServiceImpl;
        if (this.mP2pSupported && (wifiP2pServiceImpl = IWifiP2pManager.Stub.asInterface(this.mFacade.getService("wifip2p"))) != null) {
            this.mWifiP2pChannel = new AsyncChannel();
            this.mWifiP2pChannel.connect(this.mContext, getHandler(), wifiP2pServiceImpl.getP2pStateMachineMessenger());
        }
    }

    /* access modifiers changed from: private */
    public void configureRandomizedMacAddress(WifiConfiguration config) {
        if (config == null) {
            Log.e(TAG, "No config to change MAC address to");
            return;
        }
        MacAddress currentMac = MacAddress.fromString(this.mWifiNative.getMacAddress(this.mInterfaceName));
        MacAddress newMac = config.getOrCreateRandomizedMacAddress();
        this.mWifiConfigManager.setNetworkRandomizedMacAddress(config.networkId, newMac);
        if (!WifiConfiguration.isValidMacAddressForRandomization(newMac)) {
            Log.wtf(TAG, "Config generated an invalid MAC address");
        } else if (currentMac.equals(newMac)) {
            Log.d(TAG, "No changes in MAC address");
        } else {
            this.mWifiMetrics.logStaEvent(17, config);
            boolean setMacSuccess = this.mWifiNative.setMacAddress(this.mInterfaceName, newMac);
            if (DBG) {
                Log.d(TAG, "ConnectedMacRandomization SSID(" + config.getPrintableSsid() + "). setMacAddress(" + newMac.toString() + ") from " + currentMac.toString() + " = " + setMacSuccess);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setCurrentMacToFactoryMac(WifiConfiguration config) {
        MacAddress factoryMac = this.mWifiNative.getFactoryMacAddress(this.mInterfaceName);
        if (factoryMac == null) {
            Log.e(TAG, "Fail to set factory MAC address. Factory MAC is null.");
        } else if (TextUtils.equals(this.mWifiNative.getMacAddress(this.mInterfaceName), factoryMac.toString())) {
        } else {
            if (this.mWifiNative.setMacAddress(this.mInterfaceName, factoryMac)) {
                this.mWifiMetrics.logStaEvent(17, config);
                return;
            }
            Log.e(TAG, "Failed to set MAC address to '" + factoryMac.toString() + "'");
        }
    }

    public boolean isConnectedMacRandomizationEnabled() {
        return this.mConnectedMacRandomzationSupported;
    }

    public void failureDetected(int reason) {
        this.mWifiInjector.getSelfRecovery().trigger(2);
        SemWifiIssueDetector semWifiIssueDetector = this.mIssueDetector;
        if (semWifiIssueDetector != null) {
            semWifiIssueDetector.captureBugReport(17, ReportUtil.getReportDataForHidlDeath(2));
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_24HOURS_PASSED_AFTER_BOOT, ClientModeImpl.DBG_PRODUCT_DEV ? WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS : WifiConfigManager.DELETED_EPHEMERAL_SSID_EXPIRY_MS);
        }

        public boolean processMessage(Message message) {
            Message message2 = message;
            int addResult = -1;
            boolean disableOthers = false;
            switch (message2.what) {
                case 0:
                    Log.wtf(ClientModeImpl.TAG, "Error! empty message encountered");
                    break;
                case 69632:
                    if (((AsyncChannel) message2.obj) == ClientModeImpl.this.mWifiP2pChannel) {
                        if (message2.arg1 != 0) {
                            ClientModeImpl.this.loge("WifiP2pService connection failure, error=" + message2.arg1);
                            break;
                        } else {
                            boolean unused = ClientModeImpl.this.p2pSendMessage(69633);
                            break;
                        }
                    } else {
                        ClientModeImpl.this.loge("got HALF_CONNECTED for unknown channel");
                        break;
                    }
                case 69636:
                    if (((AsyncChannel) message2.obj) == ClientModeImpl.this.mWifiP2pChannel) {
                        ClientModeImpl.this.loge("WifiP2pService channel lost, message.arg1 =" + message2.arg1);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE /*131103*/:
                    ClientModeImpl clientModeImpl = ClientModeImpl.this;
                    if (message2.arg1 != 0) {
                        disableOthers = true;
                    }
                    boolean unused2 = clientModeImpl.mBluetoothConnectionActive = disableOthers;
                    if (ClientModeImpl.this.mWifiConnectivityManager != null) {
                        ClientModeImpl.this.mWifiConnectivityManager.setBluetoothConnected(ClientModeImpl.this.mBluetoothConnectionActive);
                    }
                    ClientModeImpl.this.mBigDataManager.addOrUpdateValue(10, ClientModeImpl.this.mBluetoothConnectionActive ? 1 : 0);
                    break;
                case ClientModeImpl.CMD_ADD_OR_UPDATE_NETWORK /*131124*/:
                    int from = message2.arg1;
                    WifiConfiguration config = (WifiConfiguration) message2.obj;
                    if (config.networkId == -1) {
                        config.priority = ClientModeImpl.this.mWifiConfigManager.increaseAndGetPriority();
                    }
                    ClientModeImpl.this.mWifiConfigManager.updateBssidWhitelist(config, ClientModeImpl.this.mScanRequestProxy.getScanResults());
                    NetworkUpdateResult result = ClientModeImpl.this.mWifiConfigManager.addOrUpdateNetwork(config, message2.sendingUid, from, (String) null);
                    if (!result.isSuccess()) {
                        int unused3 = ClientModeImpl.this.mMessageHandlingStatus = -2;
                    }
                    ClientModeImpl.this.replyToMessage(message2, message2.what, result.getNetworkId());
                    break;
                case ClientModeImpl.CMD_REMOVE_NETWORK /*131125*/:
                    boolean unused4 = ClientModeImpl.this.deleteNetworkConfigAndSendReply(message2, false);
                    break;
                case ClientModeImpl.CMD_ENABLE_NETWORK /*131126*/:
                    if (message2.arg2 == 1) {
                        disableOthers = true;
                    }
                    boolean ok = ClientModeImpl.this.mWifiConfigManager.enableNetwork(message2.arg1, disableOthers, message2.sendingUid);
                    if (!ok) {
                        int unused5 = ClientModeImpl.this.mMessageHandlingStatus = -2;
                    }
                    ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
                    int i = message2.what;
                    if (ok) {
                        addResult = 1;
                    }
                    clientModeImpl2.replyToMessage(message2, i, addResult);
                    break;
                case ClientModeImpl.CMD_GET_CONFIGURED_NETWORKS /*131131*/:
                    if (-1000 != ((Integer) message2.obj).intValue()) {
                        ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.mWifiConfigManager.getSavedNetworks(message2.arg2));
                        break;
                    } else {
                        ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.mWifiConfigManager.getConfiguredNetworks());
                        break;
                    }
                case ClientModeImpl.CMD_GET_SUPPORTED_FEATURES /*131133*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) Long.valueOf(ClientModeImpl.this.mWifiNative.getSupportedFeatureSet(ClientModeImpl.this.mInterfaceName)));
                    break;
                case ClientModeImpl.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS /*131134*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.mWifiConfigManager.getConfiguredNetworksWithPasswords());
                    break;
                case ClientModeImpl.CMD_GET_LINK_LAYER_STATS /*131135*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) null);
                    break;
                case ClientModeImpl.CMD_SET_OPERATIONAL_MODE /*131144*/:
                    break;
                case ClientModeImpl.CMD_DISCONNECT /*131145*/:
                case ClientModeImpl.CMD_RECONNECT /*131146*/:
                case ClientModeImpl.CMD_REASSOCIATE /*131147*/:
                case ClientModeImpl.CMD_RSSI_POLL /*131155*/:
                case ClientModeImpl.CMD_ONESHOT_RSSI_POLL /*131156*/:
                case ClientModeImpl.CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                case ClientModeImpl.CMD_DISCONNECTING_WATCHDOG_TIMER /*131168*/:
                case ClientModeImpl.CMD_DISABLE_EPHEMERAL_NETWORK /*131170*/:
                case ClientModeImpl.CMD_TARGET_BSSID /*131213*/:
                case ClientModeImpl.CMD_START_CONNECT /*131215*/:
                case ClientModeImpl.CMD_UNWANTED_NETWORK /*131216*/:
                case ClientModeImpl.CMD_START_ROAM /*131217*/:
                case ClientModeImpl.CMD_ASSOCIATED_BSSID /*131219*/:
                case ClientModeImpl.CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER /*131238*/:
                case ClientModeImpl.CMD_REPLACE_PUBLIC_DNS /*131286*/:
                case ClientModeImpl.CMD_PRE_DHCP_ACTION /*131327*/:
                case ClientModeImpl.CMD_PRE_DHCP_ACTION_COMPLETE /*131328*/:
                case ClientModeImpl.CMD_POST_DHCP_ACTION /*131329*/:
                case ClientModeImpl.CMD_THREE_TIMES_SCAN_IN_IDLE /*131381*/:
                case ClientModeImpl.CMD_SCAN_RESULT_AVAILABLE /*131584*/:
                case ClientModeImpl.CMD_CHECK_ARP_RESULT /*131622*/:
                case ClientModeImpl.CMD_SEND_ARP /*131623*/:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                case 147499:
                case WifiMonitor.BSSID_PRUNED_EVENT:
                    int unused6 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case ClientModeImpl.CMD_SET_HIGH_PERF_MODE /*131149*/:
                    if (message2.arg1 != 1) {
                        ClientModeImpl.this.setSuspendOptimizations(2, true);
                        break;
                    } else {
                        ClientModeImpl.this.setSuspendOptimizations(2, false);
                        break;
                    }
                case ClientModeImpl.CMD_ENABLE_RSSI_POLL /*131154*/:
                    ClientModeImpl clientModeImpl3 = ClientModeImpl.this;
                    if (message2.arg1 == 1) {
                        disableOthers = true;
                    }
                    boolean unused7 = clientModeImpl3.mEnableRssiPolling = disableOthers;
                    break;
                case ClientModeImpl.CMD_SET_SUSPEND_OPT_ENABLED /*131158*/:
                    if (message2.arg1 != 1) {
                        ClientModeImpl.this.setSuspendOptimizations(4, false);
                        break;
                    } else {
                        if (message2.arg2 == 1) {
                            ClientModeImpl.this.mSuspendWakeLock.release();
                        }
                        ClientModeImpl.this.setSuspendOptimizations(4, true);
                        break;
                    }
                case ClientModeImpl.CMD_SCREEN_STATE_CHANGED /*131167*/:
                    if (message2.arg1 != 0) {
                        disableOthers = true;
                    }
                    boolean screenOn = disableOthers;
                    if (ClientModeImpl.this.mScreenOn != screenOn) {
                        ClientModeImpl.this.handleScreenStateChanged(screenOn);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_REMOVE_APP_CONFIGURATIONS /*131169*/:
                    ClientModeImpl.this.deferMessage(message2);
                    break;
                case ClientModeImpl.CMD_RESET_SIM_NETWORKS /*131173*/:
                    int unused8 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DEFERRED;
                    ClientModeImpl.this.deferMessage(message2);
                    break;
                case ClientModeImpl.CMD_QUERY_OSU_ICON /*131176*/:
                case ClientModeImpl.CMD_MATCH_PROVIDER_NETWORK /*131177*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what);
                    break;
                case ClientModeImpl.CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG /*131178*/:
                    Bundle bundle = (Bundle) message2.obj;
                    if (ClientModeImpl.this.mPasspointManager.addOrUpdateProvider((PasspointConfiguration) bundle.getParcelable(ClientModeImpl.EXTRA_PASSPOINT_CONFIGURATION), bundle.getInt(ClientModeImpl.EXTRA_UID), bundle.getString(ClientModeImpl.EXTRA_PACKAGE_NAME))) {
                        addResult = 1;
                    }
                    ClientModeImpl.this.replyToMessage(message2, message2.what, addResult);
                    break;
                case ClientModeImpl.CMD_REMOVE_PASSPOINT_CONFIG /*131179*/:
                    String fqdn = (String) message2.obj;
                    if (ClientModeImpl.this.mPasspointManager.removeProvider((String) message2.obj)) {
                        addResult = 1;
                    }
                    int removeResult = addResult;
                    Iterator<WifiConfiguration> it = ClientModeImpl.this.mWifiConfigManager.getConfiguredNetworks().iterator();
                    while (true) {
                        if (it.hasNext()) {
                            WifiConfiguration network = it.next();
                            if (network.isPasspoint() && fqdn.equals(network.FQDN)) {
                                ClientModeImpl.this.mWifiConfigManager.removeNetwork(network.networkId, network.creatorUid);
                            }
                        }
                    }
                    ClientModeImpl.this.replyToMessage(message2, message2.what, removeResult);
                    break;
                case ClientModeImpl.CMD_GET_PASSPOINT_CONFIGS /*131180*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.mPasspointManager.getProviderConfigs());
                    break;
                case ClientModeImpl.CMD_GET_MATCHING_OSU_PROVIDERS /*131181*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) new HashMap());
                    break;
                case ClientModeImpl.CMD_GET_MATCHING_PASSPOINT_CONFIGS_FOR_OSU_PROVIDERS /*131182*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) new HashMap());
                    break;
                case ClientModeImpl.CMD_GET_WIFI_CONFIGS_FOR_PASSPOINT_PROFILES /*131184*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) new ArrayList());
                    break;
                case ClientModeImpl.CMD_BOOT_COMPLETED /*131206*/:
                    boolean unused9 = ClientModeImpl.this.mIsBootCompleted = true;
                    ClientModeImpl.this.initializeWifiChipInfo();
                    ClientModeImpl.this.getAdditionalWifiServiceInterfaces();
                    new MemoryStoreImpl(ClientModeImpl.this.mContext, ClientModeImpl.this.mWifiInjector, ClientModeImpl.this.mWifiScoreCard).start();
                    if (!ClientModeImpl.this.mWifiConfigManager.loadFromStore(false)) {
                        Log.e(ClientModeImpl.TAG, "Failed to load from config store, retry later");
                        ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_RELOAD_CONFIG_STORE_FILE, 1, 0, 1000);
                    }
                    ClientModeImpl.this.registerNetworkFactory();
                    ClientModeImpl.this.resetFwLogFolder();
                    ClientModeImpl.this.mWifiConfigManager.forcinglyEnableAllNetworks(1000);
                    break;
                case ClientModeImpl.CMD_INITIALIZE /*131207*/:
                    boolean ok2 = ClientModeImpl.this.mWifiNative.initialize();
                    ClientModeImpl.this.initializeWifiChipInfo();
                    ClientModeImpl clientModeImpl4 = ClientModeImpl.this;
                    int i2 = message2.what;
                    if (ok2) {
                        addResult = 1;
                    }
                    clientModeImpl4.replyToMessage(message2, i2, addResult);
                    break;
                case ClientModeImpl.CMD_IP_CONFIGURATION_SUCCESSFUL /*131210*/:
                case ClientModeImpl.CMD_IP_CONFIGURATION_LOST /*131211*/:
                case ClientModeImpl.CMD_IP_REACHABILITY_LOST /*131221*/:
                    int unused10 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case ClientModeImpl.CMD_UPDATE_LINKPROPERTIES /*131212*/:
                    ClientModeImpl.this.updateLinkProperties((LinkProperties) message2.obj);
                    break;
                case ClientModeImpl.CMD_REMOVE_USER_CONFIGURATIONS /*131224*/:
                    ClientModeImpl.this.deferMessage(message2);
                    break;
                case ClientModeImpl.CMD_START_IP_PACKET_OFFLOAD /*131232*/:
                case ClientModeImpl.CMD_STOP_IP_PACKET_OFFLOAD /*131233*/:
                case ClientModeImpl.CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF /*131281*/:
                case ClientModeImpl.CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF /*131282*/:
                    if (ClientModeImpl.this.mNetworkAgent != null) {
                        ClientModeImpl.this.mNetworkAgent.onSocketKeepaliveEvent(message2.arg1, -20);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
                    int unused11 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case ClientModeImpl.CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
                    int unused12 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case ClientModeImpl.CMD_GET_ALL_MATCHING_FQDNS_FOR_SCAN_RESULTS /*131240*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) new HashMap());
                    break;
                case ClientModeImpl.CMD_INSTALL_PACKET_FILTER /*131274*/:
                    ClientModeImpl.this.mWifiNative.installPacketFilter(ClientModeImpl.this.mInterfaceName, (byte[]) message2.obj);
                    break;
                case ClientModeImpl.CMD_SET_FALLBACK_PACKET_FILTERING /*131275*/:
                    if (!((Boolean) message2.obj).booleanValue()) {
                        ClientModeImpl.this.mWifiNative.stopFilteringMulticastV4Packets(ClientModeImpl.this.mInterfaceName);
                        break;
                    } else {
                        ClientModeImpl.this.mWifiNative.startFilteringMulticastV4Packets(ClientModeImpl.this.mInterfaceName);
                        break;
                    }
                case ClientModeImpl.CMD_USER_SWITCH /*131277*/:
                    Set<Integer> removedNetworkIds = ClientModeImpl.this.mWifiConfigManager.handleUserSwitch(message2.arg1);
                    if (removedNetworkIds.contains(Integer.valueOf(ClientModeImpl.this.mTargetNetworkId)) || removedNetworkIds.contains(Integer.valueOf(ClientModeImpl.this.mLastNetworkId))) {
                        ClientModeImpl.this.disconnectCommand(0, 20);
                        break;
                    }
                case ClientModeImpl.CMD_USER_UNLOCK /*131278*/:
                    ClientModeImpl.this.mWifiConfigManager.handleUserUnlock(message2.arg1);
                    break;
                case ClientModeImpl.CMD_USER_STOP /*131279*/:
                    ClientModeImpl.this.mWifiConfigManager.handleUserStop(message2.arg1);
                    break;
                case ClientModeImpl.CMD_READ_PACKET_FILTER /*131280*/:
                    byte[] data = ClientModeImpl.this.mWifiNative.readPacketFilter(ClientModeImpl.this.mInterfaceName);
                    if (ClientModeImpl.this.mIpClient != null) {
                        ClientModeImpl.this.mIpClient.readPacketFilterComplete(data);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_IMS_CALL_ESTABLISHED /*131315*/:
                    if (ClientModeImpl.this.mIsImsCallEstablished != (message2.arg1 == 1)) {
                        boolean unused13 = ClientModeImpl.this.mIsImsCallEstablished = message2.arg1 == 1;
                        if (ClientModeImpl.this.mWifiConnectivityManager != null) {
                            WifiConnectivityManager access$4700 = ClientModeImpl.this.mWifiConnectivityManager;
                            if (ClientModeImpl.this.mIsImsCallEstablished) {
                                disableOthers = true;
                            }
                            access$4700.changeMaxPeriodicScanMode(disableOthers ? 1 : 0);
                        }
                        if (ClientModeImpl.this.mWifiInjector.getWifiLowLatency() != null) {
                            ClientModeImpl.this.mWifiInjector.getWifiLowLatency().setImsCallingState(ClientModeImpl.this.mIsImsCallEstablished);
                            break;
                        }
                    }
                    break;
                case ClientModeImpl.CMD_AUTO_CONNECT_CARRIER_AP_ENABLED /*131316*/:
                    WifiConfigManager access$1500 = ClientModeImpl.this.mWifiConfigManager;
                    if (message2.arg1 == 1) {
                        disableOthers = true;
                    }
                    access$1500.setAutoConnectCarrierApEnabled(disableOthers);
                    break;
                case ClientModeImpl.CMD_DIAGS_CONNECT_TIMEOUT /*131324*/:
                    ClientModeImpl.this.mWifiDiagnostics.reportConnectionEvent((byte) 3);
                    break;
                case ClientModeImpl.CMD_START_SUBSCRIPTION_PROVISIONING /*131326*/:
                case ClientModeImpl.CMD_UPDATE_CONFIG_LOCATION /*131332*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, 0);
                    break;
                case ClientModeImpl.CMD_SET_ADPS_MODE /*131383*/:
                    int unused14 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case ClientModeImpl.CMD_FORCINGLY_ENABLE_ALL_NETWORKS /*131402*/:
                    ClientModeImpl.this.mWifiConfigManager.forcinglyEnableAllNetworks(message2.sendingUid);
                    break;
                case ClientModeImpl.CMD_SEC_API_ASYNC /*131573*/:
                    if (!ClientModeImpl.this.processMessageOnDefaultStateForCallSECApiAsync(message2)) {
                        int unused15 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_SEC_API /*131574*/:
                    Log.d(ClientModeImpl.TAG, "DefaultState::Handling CMD_SEC_API");
                    ClientModeImpl.this.replyToMessage(message2, message2.what, -1);
                    break;
                case ClientModeImpl.CMD_SEC_STRING_API /*131575*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.processMessageOnDefaultStateForCallSECStringApi(message2));
                    break;
                case ClientModeImpl.CMD_SEC_LOGGING /*131576*/:
                case WifiMonitor.SUP_BIGDATA_EVENT:
                    Bundle args = (Bundle) message2.obj;
                    String feature = null;
                    if (args != null) {
                        feature = args.getString("feature", (String) null);
                    }
                    if (!ClientModeImpl.this.mIsShutdown) {
                        if (ClientModeImpl.this.mIsBootCompleted) {
                            if (feature == null) {
                                if (ClientModeImpl.DBG) {
                                    Log.e(ClientModeImpl.TAG, "CMD_SEC_LOGGING - feature is null");
                                    break;
                                }
                            } else {
                                if (WifiBigDataLogManager.ENABLE_SURVEY_MODE) {
                                    ClientModeImpl.this.mBigDataManager.insertLog(args);
                                } else if (ClientModeImpl.DBG) {
                                    Log.e(ClientModeImpl.TAG, "survey mode is disabled");
                                }
                                if (!WifiBigDataLogManager.FEATURE_DISC.equals(feature)) {
                                    if (!WifiBigDataLogManager.FEATURE_ON_OFF.equals(feature)) {
                                        if (!WifiBigDataLogManager.FEATURE_ASSOC.equals(feature)) {
                                            if (!WifiBigDataLogManager.FEATURE_ISSUE_DETECTOR_DISC1.equals(feature)) {
                                                if (!WifiBigDataLogManager.FEATURE_ISSUE_DETECTOR_DISC2.equals(feature)) {
                                                    if (WifiBigDataLogManager.FEATURE_HANG.equals(feature)) {
                                                        ClientModeImpl.this.increaseCounter(WifiMonitor.DRIVER_HUNG_EVENT);
                                                        break;
                                                    }
                                                } else if (args.getInt(ReportIdKey.KEY_CATEGORY_ID, 0) == 1) {
                                                    ClientModeImpl.this.sendBroadcastIssueTrackerSysDump(0);
                                                    break;
                                                }
                                            } else {
                                                int i3 = args.getInt(ReportIdKey.KEY_CATEGORY_ID, 0);
                                                break;
                                            }
                                        } else {
                                            String dataString = args.getString("data", (String) null);
                                            if (dataString != null) {
                                                ClientModeImpl clientModeImpl5 = ClientModeImpl.this;
                                                clientModeImpl5.report(ReportIdKey.ID_BIGDATA_ASSOC_REJECT, ReportUtil.getReportDataFromBigDataParamsOfASSO(dataString, clientModeImpl5.mLastConnectedNetworkId));
                                                break;
                                            }
                                        }
                                    } else {
                                        String dataString2 = args.getString("data", (String) null);
                                        if (dataString2 != null) {
                                            ClientModeImpl.this.report(201, ReportUtil.getReportDataFromBigDataParamsOfONOF(dataString2));
                                            break;
                                        }
                                    }
                                } else {
                                    String dataString3 = args.getString("data", (String) null);
                                    if (dataString3 != null) {
                                        ClientModeImpl clientModeImpl6 = ClientModeImpl.this;
                                        clientModeImpl6.report(200, ReportUtil.getReportDataFromBigDataParamsOfDISC(dataString3, clientModeImpl6.mConnectedApInternalType, ClientModeImpl.this.mBigDataManager.getAndResetLastInternalReason(), ClientModeImpl.this.mLastConnectedNetworkId));
                                        break;
                                    }
                                }
                            }
                        } else {
                            ClientModeImpl clientModeImpl7 = ClientModeImpl.this;
                            clientModeImpl7.sendMessageDelayed(clientModeImpl7.obtainMessage(WifiMonitor.SUP_BIGDATA_EVENT, 0, 0, message2.obj), 20000);
                            break;
                        }
                    } else {
                        Log.d(ClientModeImpl.TAG, "shutdowning device");
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_24HOURS_PASSED_AFTER_BOOT /*131579*/:
                    String paramData = ClientModeImpl.this.getWifiParameters(true);
                    Log.i(ClientModeImpl.TAG, "Counter: " + paramData);
                    if (WifiBigDataLogManager.ENABLE_SURVEY_MODE) {
                        Bundle paramArgs = WifiBigDataLogManager.getBigDataBundle(WifiBigDataLogManager.FEATURE_24HR, paramData);
                        ClientModeImpl clientModeImpl8 = ClientModeImpl.this;
                        clientModeImpl8.sendMessage(clientModeImpl8.obtainMessage(ClientModeImpl.CMD_SEC_LOGGING, 0, 0, paramArgs));
                    }
                    ClientModeImpl.this.report(ReportIdKey.ID_BIGDATA_W24H, ReportUtil.getReportDatatForW24H(paramData));
                    ClientModeImpl.this.removeMessages(ClientModeImpl.CMD_24HOURS_PASSED_AFTER_BOOT);
                    ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_24HOURS_PASSED_AFTER_BOOT, WifiConfigManager.DELETED_EPHEMERAL_SSID_EXPIRY_MS);
                    break;
                case ClientModeImpl.CMD_GET_A_CONFIGURED_NETWORK /*131581*/:
                    ClientModeImpl.this.replyToMessage(message2, message2.what, (Object) ClientModeImpl.this.mWifiConfigManager.getConfiguredNetwork(message2.arg1));
                    break;
                case ClientModeImpl.CMD_SHOW_TOAST_MSG /*131582*/:
                    SemWifiFrameworkUxUtils.showToast(ClientModeImpl.this.mContext, message2.arg1, (String) message2.obj);
                    break;
                case ClientModeImpl.CMD_RELOAD_CONFIG_STORE_FILE /*131583*/:
                    if (message2.arg1 <= 3) {
                        WifiConfigManager access$15002 = ClientModeImpl.this.mWifiConfigManager;
                        if (message2.arg1 == 3) {
                            disableOthers = true;
                        }
                        if (!access$15002.loadFromStore(disableOthers)) {
                            Log.e(ClientModeImpl.TAG, "Failed to load from config store, retry " + message2.arg1);
                            ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_RELOAD_CONFIG_STORE_FILE, message2.arg1 + 1, 0, 3000);
                            break;
                        }
                    }
                    ClientModeImpl.this.mWifiConfigManager.forcinglyEnableAllNetworks(1000);
                    break;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                    ClientModeImpl.this.mP2pConnected.set(((NetworkInfo) message2.obj).isConnected());
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                    ClientModeImpl clientModeImpl9 = ClientModeImpl.this;
                    if (message2.arg1 == 1) {
                        disableOthers = true;
                    }
                    boolean unused16 = clientModeImpl9.mTemporarilyDisconnectWifi = disableOthers;
                    ClientModeImpl.this.replyToMessage(message2, WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                    break;
                case WifiP2pServiceImpl.DISABLE_P2P_RSP /*143395*/:
                    if (ClientModeImpl.this.mP2pDisableListener != null) {
                        Log.d(ClientModeImpl.TAG, "DISABLE_P2P_RSP mP2pDisableListener == " + ClientModeImpl.this.mP2pDisableListener);
                        ClientModeImpl.this.mP2pDisableListener.onDisable();
                        WifiController.P2pDisableListener unused17 = ClientModeImpl.this.mP2pDisableListener = null;
                        break;
                    }
                    break;
                case 147527:
                    if (message2.obj != null) {
                        int eapEvent = message2.arg1;
                        ClientModeImpl.this.processMessageForEap(eapEvent, message2.arg2, (String) message2.obj);
                        if (eapEvent == 2 || eapEvent == 3) {
                            ClientModeImpl.this.notifyDisconnectInternalReason(13);
                            if (!(ClientModeImpl.this.mTargetNetworkId == -1 || ClientModeImpl.this.mTargetWifiConfiguration == null)) {
                                ClientModeImpl clientModeImpl10 = ClientModeImpl.this;
                                clientModeImpl10.report(15, ReportUtil.getReportDataForAuthFailForEap(clientModeImpl10.mTargetNetworkId, eapEvent, ClientModeImpl.this.mTargetWifiConfiguration.status, ClientModeImpl.this.mTargetWifiConfiguration.numAssociation, ClientModeImpl.this.mTargetWifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionStatus(), ClientModeImpl.this.mTargetWifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionDisableReason()));
                                break;
                            }
                        }
                    }
                    break;
                case 151553:
                    ClientModeImpl.this.replyToMessage(message2, 151554, 2);
                    break;
                case 151556:
                    boolean unused18 = ClientModeImpl.this.deleteNetworkConfigAndSendReply(message2, true);
                    break;
                case 151559:
                    NetworkUpdateResult unused19 = ClientModeImpl.this.saveNetworkConfigAndSendReply(message2);
                    break;
                case 151569:
                    ClientModeImpl.this.replyToMessage(message2, 151570, 2);
                    break;
                case 151572:
                    ClientModeImpl.this.replyToMessage(message2, 151574, 2);
                    break;
                default:
                    ClientModeImpl.this.loge("Error! unhandled message" + message2);
                    break;
            }
            if (1 == 1) {
                ClientModeImpl.this.logStateAndMessage(message2, this);
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void setupClientMode() {
        Log.d(TAG, "setupClientMode() ifacename = " + this.mInterfaceName);
        setHighPerfModeEnabled(false);
        this.mWifiStateTracker.updateState(0);
        this.mIpClientCallbacks = new IpClientCallbacksImpl();
        this.mFacade.makeIpClient(this.mContext, this.mInterfaceName, this.mIpClientCallbacks);
        if (!this.mIpClientCallbacks.awaitCreation()) {
            loge("Timeout waiting for IpClient");
        }
        setMulticastFilter(true);
        registerForWifiMonitorEvents();
        this.mWifiInjector.getWifiLastResortWatchdog().clearAllFailureCounts();
        setSupplicantLogLevel();
        this.mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mWifiInfo.setMacAddress(this.mWifiNative.getMacAddress(this.mInterfaceName));
        sendSupplicantConnectionChangedBroadcast(true);
        this.mScanResultsEventCounter = 0;
        this.mWifiNative.setExternalSim(this.mInterfaceName, true);
        setRandomMacOui();
        this.mCountryCode.setReadyForChangeAndUpdate(true);
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            Log.e(TAG, "SupplicantStarted - enter() isAirplaneModeEnabled !!  ");
            this.mCountryCode.setCountryCodeNative("CSC", true);
        }
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mIsRunning = true;
        updateBatteryWorkSource((WorkSource) null);
        this.mWifiNative.setBluetoothCoexistenceScanMode(this.mInterfaceName, this.mBluetoothConnectionActive);
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        this.mWifiNative.stopFilteringMulticastV4Packets(this.mInterfaceName);
        this.mWifiNative.stopFilteringMulticastV6Packets(this.mInterfaceName);
        this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get());
        setPowerSave(true);
        this.mWifiNative.enableStaAutoReconnect(this.mInterfaceName, false);
        this.mWifiNative.setConcurrencyPriority(true);
        if (!isWifiOnly()) {
            if (this.mUnstableApController == null) {
                this.mUnstableApController = new UnstableApController(new UnstableApController.UnstableApAdapter() {
                    public void addToBlackList(String bssid) {
                        ClientModeImpl.this.mWifiConnectivityManager.trackBssid(bssid, false, 17);
                    }

                    public void updateUnstableApNetwork(int networkId, int reason) {
                        if (reason == 2) {
                            for (int i = 0; i < 5; i++) {
                                ClientModeImpl.this.mWifiConfigManager.updateNetworkSelectionStatus(networkId, reason);
                            }
                            return;
                        }
                        ClientModeImpl.this.mWifiConfigManager.updateNetworkSelectionStatus(networkId, reason);
                    }

                    public void enableNetwork(int networkId) {
                        ClientModeImpl.this.mWifiConfigManager.enableNetwork(networkId, false, 1000);
                    }

                    public WifiConfiguration getNetwork(int networkId) {
                        return ClientModeImpl.this.mWifiConfigManager.getConfiguredNetwork(networkId);
                    }
                });
            }
            this.mUnstableApController.clearAll();
            this.mUnstableApController.setSimCardState(TelephonyUtil.isSimCardReady(getTelephonyManager()));
        }
        if (OpBrandingLoader.Vendor.VZW == mOpBranding && this.mSemWifiHiddenNetworkTracker == null) {
            this.mSemWifiHiddenNetworkTracker = new SemWifiHiddenNetworkTracker(this.mContext, new SemWifiHiddenNetworkTracker.WifiHiddenNetworkAdapter() {
                public List<ScanResult> getScanResults() {
                    List<ScanResult> scanResults = new ArrayList<>();
                    scanResults.addAll(ClientModeImpl.this.mScanRequestProxy.getScanResults());
                    return scanResults;
                }
            });
        }
        if (ENABLE_SUPPORT_ADPS) {
            updateAdpsState();
            sendMessage(CMD_SET_ADPS_MODE);
        }
    }

    /* access modifiers changed from: private */
    public void stopClientMode() {
        this.mWifiDiagnostics.stopLogging();
        this.mIsRunning = false;
        updateBatteryWorkSource((WorkSource) null);
        if (this.mIpClient != null && this.mIpClient.shutdown()) {
            this.mIpClientCallbacks.awaitShutdown();
        }
        this.mNetworkInfo.setIsAvailable(false);
        WifiNetworkAgent wifiNetworkAgent = this.mNetworkAgent;
        if (wifiNetworkAgent != null) {
            wifiNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        this.mCountryCode.setReadyForChange(false);
        this.mInterfaceName = null;
        sendSupplicantConnectionChangedBroadcast(false);
        this.mWifiConfigManager.removeAllEphemeralOrPasspointConfiguredNetworks();
    }

    /* access modifiers changed from: package-private */
    public void registerConnected() {
        int i = this.mLastNetworkId;
        if (i != -1) {
            this.mWifiConfigManager.updateNetworkAfterConnect(i);
            WifiConfiguration currentNetwork = getCurrentWifiConfiguration();
            if (currentNetwork != null && currentNetwork.isPasspoint()) {
                this.mPasspointManager.onPasspointNetworkConnected(currentNetwork.FQDN);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void registerDisconnected() {
        int i = this.mLastNetworkId;
        if (i != -1) {
            this.mWifiConfigManager.updateNetworkAfterDisconnect(i);
        }
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        int i = this.mLastNetworkId;
        if (i == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(i);
    }

    private WifiConfiguration getTargetWifiConfiguration() {
        int i = this.mTargetNetworkId;
        if (i == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(i);
    }

    /* access modifiers changed from: package-private */
    public ScanResult getCurrentScanResult() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config == null) {
            return null;
        }
        String bssid = this.mWifiInfo.getBSSID();
        if (bssid == null) {
            bssid = this.mTargetRoamBSSID;
        }
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId);
        if (scanDetailCache == null) {
            return null;
        }
        return scanDetailCache.getScanResult(bssid);
    }

    /* access modifiers changed from: package-private */
    public String getCurrentBSSID() {
        return this.mLastBssid;
    }

    /* access modifiers changed from: private */
    public void handleCellularCapabilities() {
        handleCellularCapabilities(false);
    }

    /* access modifiers changed from: private */
    public void handleCellularCapabilities(boolean bForce) {
        byte curNetworkType = 0;
        byte curCellularCapaState = 2;
        if (isWifiOnly()) {
            mCellularCapaState = 3;
            mNetworktype = 0;
            mCellularSignalLevel = 0;
            if (this.mWifiState.get() == 3) {
                this.mWifiNative.updateCellularCapabilities(this.mInterfaceName, mCellularCapaState, mNetworktype, mCellularSignalLevel, mCellularCellId);
                return;
            }
            return;
        }
        try {
            TelephonyManager telephonyManager = getTelephonyManager();
            boolean isNetworkRoaming = telephonyManager.isNetworkRoaming();
            boolean isDataRoamingEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "data_roaming", 0) != 0;
            boolean isDataEnabled = telephonyManager.getDataEnabled();
            int simCardState = telephonyManager.getSimCardState();
            if (simCardState == 11) {
                curNetworkType = (byte) TelephonyManager.getNetworkClass(telephonyManager.getNetworkType());
                if (isNetworkRoaming) {
                    if (isDataRoamingEnabled && !mIsPolicyMobileData && isDataEnabled && curNetworkType != 0) {
                        curCellularCapaState = 1;
                    }
                } else if (isDataEnabled && !mIsPolicyMobileData && curNetworkType != 0) {
                    curCellularCapaState = 1;
                }
            } else {
                Arrays.fill(mCellularCellId, (byte) 0);
                mCellularSignalLevel = 0;
            }
            if (bForce && curNetworkType != 0) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    Iterator<CellInfo> it = cellInfoList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        CellInfo cellInfo = it.next();
                        Log.d(TAG, "isRegistered " + cellInfo.isRegistered());
                        if (cellInfo.isRegistered()) {
                            mCellularSignalLevel = (byte) getCellLevel(cellInfo);
                            mCellularCellId = getCellId(cellInfo);
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, "cellInfoList is null.");
                }
            }
            if (bForce) {
                mNetworktype = curNetworkType;
                mCellularCapaState = curCellularCapaState;
                if (this.mWifiState.get() == 3) {
                    this.mWifiNative.updateCellularCapabilities(this.mInterfaceName, mCellularCapaState, mNetworktype, mCellularSignalLevel, mCellularCellId);
                }
                mChanged = false;
            } else if (curNetworkType == mNetworktype && curCellularCapaState == mCellularCapaState && !mChanged) {
                Log.d(TAG, "handleCellularCapabilities duplicated values...so skip.");
            } else if (simCardState == 11 || curCellularCapaState != mCellularCapaState || mChanged) {
                mNetworktype = curNetworkType;
                mCellularCapaState = curCellularCapaState;
                if (this.mWifiState.get() == 3) {
                    this.mWifiNative.updateCellularCapabilities(this.mInterfaceName, mCellularCapaState, mNetworktype, mCellularSignalLevel, mCellularCellId);
                }
                mChanged = false;
            } else {
                Log.d(TAG, "handleCellularCapabilities sim not present...so skip.");
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCellularCapabilities exception " + e.toString());
        }
    }

    /* access modifiers changed from: private */
    public byte[] toBytes(int i) {
        Log.d(TAG, "toBytes:" + Integer.toHexString(i));
        byte[] result = {(byte) (i >> 8), (byte) i};
        Log.d(TAG, "toBytes:" + result[0] + "," + result[1]);
        return result;
    }

    private byte[] getCellId(CellInfo cellInfo) {
        int value = 0;
        if (cellInfo instanceof CellInfoLte) {
            value = ((CellInfoLte) cellInfo).getCellIdentity().getCi();
        } else if (cellInfo instanceof CellInfoWcdma) {
            value = ((CellInfoWcdma) cellInfo).getCellIdentity().getCid();
        } else if (cellInfo instanceof CellInfoGsm) {
            value = ((CellInfoGsm) cellInfo).getCellIdentity().getCid();
        } else if (cellInfo instanceof CellInfoCdma) {
            value = ((CellInfoCdma) cellInfo).getCellIdentity().getBasestationId();
        } else if (cellInfo instanceof CellInfoNr) {
            value = ((CellIdentityNr) ((CellInfoNr) cellInfo).getCellIdentity()).getPci();
        } else {
            Log.e(TAG, "Invalid CellInfo type");
        }
        return toBytes(value);
    }

    private int getCellLevel(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            return ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoWcdma) {
            return ((CellInfoWcdma) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoGsm) {
            return ((CellInfoGsm) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoCdma) {
            return ((CellInfoCdma) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoNr) {
            return ((CellInfoNr) cellInfo).getCellSignalStrength().getLevel();
        }
        Log.e(TAG, "Invalid CellInfo type");
        return 0;
    }

    private void updateMobileStateForWifiToCellular(byte mobileState) {
        if (this.mMobileState != mobileState) {
            this.mMobileState = mobileState;
            setWifiToCellular(false);
        }
    }

    private void updateParamForWifiToCellular(int scanMode, int rssiThreshold, int candidateRssiThreshold24G, int candidateRssiThreshold5G, int candidateRssiThreshold6G) {
        this.mScanMode = scanMode;
        this.mRssiThreshold = rssiThreshold;
        this.mCandidateRssiThreshold24G = candidateRssiThreshold24G;
        this.mCandidateRssiThreshold5G = candidateRssiThreshold5G;
        this.mCandidateRssiThreshold6G = candidateRssiThreshold6G;
        setWifiToCellular(this.mWtcMode == 0);
    }

    private void setWifiToCellular(boolean forceUpdate) {
        Log.d(TAG, "setWifiToCellular is called.");
        if (forceUpdate) {
            int i = this.mWtcMode;
            if (i != 0) {
                Log.e(TAG, "setWifiToCellular - forceUpdate shold be work only enable mode, it need to be check.");
            } else {
                this.mWifiNative.setWifiToCellular(this.mInterfaceName, i, this.mScanMode, this.mRssiThreshold, this.mCandidateRssiThreshold24G, this.mCandidateRssiThreshold5G, this.mCandidateRssiThreshold6G);
            }
        } else {
            byte b = this.mMobileState;
            if (b != 1) {
                if (b == 2) {
                    if (this.mWtcMode != 3) {
                        this.mWtcMode = 3;
                    } else {
                        return;
                    }
                } else if (this.mWtcMode != 1) {
                    this.mWtcMode = 1;
                } else {
                    return;
                }
                this.mWifiNative.setWifiToCellular(this.mInterfaceName, this.mWtcMode, 0, 0, 0, 0, 0);
            } else if (this.mWtcMode != 0) {
                this.mWtcMode = 0;
                this.mWifiNative.setWifiToCellular(this.mInterfaceName, this.mWtcMode, this.mScanMode, this.mRssiThreshold, this.mCandidateRssiThreshold24G, this.mCandidateRssiThreshold5G, this.mCandidateRssiThreshold6G);
            }
        }
    }

    /* access modifiers changed from: private */
    public void enable5GSarBackOff() {
        Log.d(TAG, "enable5GSarBackOff()");
        ServiceState serviceState = getTelephonyManager().getServiceState();
        if (serviceState != null) {
            int nrBearerStatus = serviceState.getNrBearerStatus();
            Log.d(TAG, "serviceState.getNrBearerStatus()=" + nrBearerStatus);
            if (((InputManager) this.mContext.getSystemService(InputManager.class)).getLidState() == 1) {
                return;
            }
            if (nrBearerStatus == 2 || nrBearerStatus == 1) {
                this.mSemSarManager.set5GSarBackOff(nrBearerStatus);
            }
        }
    }

    public void set5GSarBackOff(int mode) {
        Log.d(TAG, "set5GSarBackOff " + mode);
        this.mSemSarManager.set5GSarBackOff(mode);
    }

    class ConnectModeState extends State {
        ConnectModeState() {
        }

        public void enter() {
            Log.d(ClientModeImpl.TAG, "entering ConnectModeState: ifaceName = " + ClientModeImpl.this.mInterfaceName);
            boolean safeModeEnabled = true;
            int unused = ClientModeImpl.this.mOperationalMode = 1;
            ClientModeImpl.this.setupClientMode();
            if (!ClientModeImpl.this.mWifiNative.removeAllNetworks(ClientModeImpl.this.mInterfaceName)) {
                ClientModeImpl.this.loge("Failed to remove networks on entering connect mode");
            }
            ClientModeImpl.this.mWifiInfo.reset();
            ClientModeImpl.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            if (ClientModeImpl.CHARSET_CN.equals(ClientModeImpl.CONFIG_CHARSET) || ClientModeImpl.CHARSET_KOR.equals(ClientModeImpl.CONFIG_CHARSET)) {
                NetworkDetail.clearNonUTF8SsidLists();
            }
            ClientModeImpl.this.mNetworkInfo.setIsAvailable(true);
            if (ClientModeImpl.this.mNetworkAgent != null) {
                ClientModeImpl.this.mNetworkAgent.sendNetworkInfo(ClientModeImpl.this.mNetworkInfo);
            }
            boolean unused2 = ClientModeImpl.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            if (ClientModeImpl.this.mWifiGeofenceManager.isSupported() && ClientModeImpl.this.mWifiConfigManager.getSavedNetworks(1010).size() > 0) {
                ClientModeImpl.this.mWifiGeofenceManager.startGeofenceThread(ClientModeImpl.this.mWifiConfigManager.getSavedNetworks(1010));
            }
            ClientModeImpl.this.mWifiConnectivityManager.setWifiEnabled(true);
            ClientModeImpl.this.mNetworkFactory.setWifiState(true);
            ClientModeImpl.this.mWifiMetrics.setWifiState(2);
            ClientModeImpl.this.mWifiMetrics.logStaEvent(18);
            ClientModeImpl.this.mSarManager.setClientWifiState(3);
            ClientModeImpl.this.mSemSarManager.setClientWifiState(3);
            ClientModeImpl clientModeImpl = ClientModeImpl.this;
            clientModeImpl.setConcurrentEnabled(clientModeImpl.mConcurrentEnabled);
            ClientModeImpl.this.mWifiScoreCard.noteSupplicantStateChanged(ClientModeImpl.this.mWifiInfo);
            ClientModeImpl.this.initializeWifiChipInfo();
            ClientModeImpl.this.setFccChannel();
            try {
                if (ClientModeImpl.this.getTelephonyManager().getSimCardState() == 11) {
                    ClientModeImpl.access$9976(64);
                } else {
                    ClientModeImpl.access$9972(-65);
                }
                ClientModeImpl.this.getTelephonyManager().listen(ClientModeImpl.this.mPhoneStateListener, ClientModeImpl.mPhoneStateEvent);
            } catch (Exception e) {
                Log.e(ClientModeImpl.TAG, "TelephonyManager.listen exception happend : " + e.toString());
            }
            ClientModeImpl.this.handleCellularCapabilities(true);
            int unused3 = ClientModeImpl.this.mLastEAPFailureCount = 0;
            ClientModeImpl.this.enable5GSarBackOff();
            int unused4 = ClientModeImpl.this.getNCHOVersion();
            if (Settings.Global.getInt(ClientModeImpl.this.mContext.getContentResolver(), "safe_wifi", 0) != 1) {
                safeModeEnabled = false;
            }
            if (!ClientModeImpl.this.mWifiNative.setSafeMode(ClientModeImpl.this.mInterfaceName, safeModeEnabled)) {
                Log.e(ClientModeImpl.TAG, "Failed to set safe Wi-Fi mode");
            }
            if (ClientModeImpl.CSC_WIFI_SUPPORT_VZW_EAP_AKA) {
                ClientModeImpl.this.mWifiConfigManager.semRemoveUnneccessaryNetworks();
            }
        }

        public void exit() {
            int unused = ClientModeImpl.this.mOperationalMode = 4;
            if (ClientModeImpl.this.getCurrentState() == ClientModeImpl.this.mConnectedState) {
                ClientModeImpl.this.mDelayDisconnect.checkAndWait(ClientModeImpl.this.mNetworkInfo);
            }
            ClientModeImpl.this.mNetworkInfo.setIsAvailable(false);
            if (ClientModeImpl.this.mNetworkAgent != null) {
                ClientModeImpl.this.mNetworkAgent.sendNetworkInfo(ClientModeImpl.this.mNetworkInfo);
            }
            ClientModeImpl.this.mWifiConnectivityManager.setWifiEnabled(false);
            ClientModeImpl.this.mNetworkFactory.setWifiState(false);
            ClientModeImpl.this.mWifiMetrics.setWifiState(1);
            ClientModeImpl.this.mWifiMetrics.logStaEvent(19);
            ClientModeImpl.this.mWifiScoreCard.noteWifiDisabled(ClientModeImpl.this.mWifiInfo);
            ClientModeImpl.this.mSarManager.setClientWifiState(1);
            ClientModeImpl.this.mSemSarManager.setClientWifiState(1);
            if (!ClientModeImpl.this.mHandleIfaceIsUp) {
                Log.w(ClientModeImpl.TAG, "mHandleIfaceIsUp is false on exiting connect mode, skip removeAllNetworks");
            } else if (!ClientModeImpl.this.mWifiNative.removeAllNetworks(ClientModeImpl.this.mInterfaceName)) {
                ClientModeImpl.this.loge("Failed to remove networks on exiting connect mode");
            }
            ClientModeImpl.this.mWifiInfo.reset();
            ClientModeImpl.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            ClientModeImpl.this.mWifiScoreCard.noteSupplicantStateChanged(ClientModeImpl.this.mWifiInfo);
            ClientModeImpl.this.stopClientMode();
            ClientModeImpl.this.setConcurrentEnabled(false);
            if (ClientModeImpl.this.mWifiGeofenceManager.isSupported() && !ClientModeImpl.this.isGeofenceUsedByAnotherPackage()) {
                ClientModeImpl.this.mWifiGeofenceManager.deinitGeofence();
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:501:0x116c, code lost:
            if (com.android.server.wifi.ClientModeImpl.access$15200(r5, com.android.server.wifi.ClientModeImpl.access$3600(r5), r0) != false) goto L_0x116e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:520:0x1214, code lost:
            if (com.android.server.wifi.ClientModeImpl.access$15200(r6, com.android.server.wifi.ClientModeImpl.access$3600(r6), r5) != false) goto L_0x1216;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(android.os.Message r24) {
            /*
                r23 = this;
                r1 = r23
                r2 = r24
                r3 = 1
                int r0 = r2.what
                java.lang.String r4 = ""
                r8 = 151554(0x25002, float:2.12372E-40)
                r10 = 18
                r11 = 3
                r13 = 4
                r14 = -2
                r15 = 2
                java.lang.String r5 = "WifiClientModeImpl"
                r7 = 1
                r9 = 0
                switch(r0) {
                    case 131103: goto L_0x14a7;
                    case 131125: goto L_0x1462;
                    case 131126: goto L_0x142d;
                    case 131135: goto L_0x141e;
                    case 131146: goto L_0x140f;
                    case 131147: goto L_0x13f1;
                    case 131149: goto L_0x13de;
                    case 131158: goto L_0x13bc;
                    case 131164: goto L_0x139c;
                    case 131169: goto L_0x1367;
                    case 131170: goto L_0x133a;
                    case 131173: goto L_0x1260;
                    case 131176: goto L_0x1241;
                    case 131177: goto L_0x1238;
                    case 131178: goto L_0x11d2;
                    case 131179: goto L_0x1145;
                    case 131181: goto L_0x112e;
                    case 131182: goto L_0x1117;
                    case 131184: goto L_0x1100;
                    case 131213: goto L_0x10f1;
                    case 131215: goto L_0x0daf;
                    case 131217: goto L_0x0da7;
                    case 131219: goto L_0x0d59;
                    case 131224: goto L_0x0d1e;
                    case 131233: goto L_0x0d03;
                    case 131238: goto L_0x0cf2;
                    case 131240: goto L_0x0cdb;
                    case 131276: goto L_0x0cc4;
                    case 131326: goto L_0x0c98;
                    case 131383: goto L_0x0c91;
                    case 131573: goto L_0x0c88;
                    case 131574: goto L_0x0c79;
                    case 131575: goto L_0x0c67;
                    case 131584: goto L_0x0c0a;
                    case 135289: goto L_0x0bf6;
                    case 135290: goto L_0x0be7;
                    case 143372: goto L_0x0ba2;
                    case 147459: goto L_0x097d;
                    case 147460: goto L_0x094a;
                    case 147462: goto L_0x08cf;
                    case 147463: goto L_0x0675;
                    case 147471: goto L_0x0595;
                    case 147472: goto L_0x0563;
                    case 147499: goto L_0x0469;
                    case 147500: goto L_0x045a;
                    case 147501: goto L_0x035c;
                    case 147509: goto L_0x034d;
                    case 147517: goto L_0x033e;
                    case 151553: goto L_0x01d1;
                    case 151556: goto L_0x016f;
                    case 151559: goto L_0x00c9;
                    case 151569: goto L_0x001c;
                    default: goto L_0x0019;
                }
            L_0x0019:
                r3 = 0
                goto L_0x14dd
            L_0x001c:
                int r0 = r2.arg1
                r4 = 0
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                int r6 = r2.sendingUid
                boolean r5 = r5.canDisableNetwork(r0, r6)
                if (r5 != 0) goto L_0x0043
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = "Failed to disable network"
                r5.loge(r6)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int unused = r5.mMessageHandlingStatus = r14
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 151570(0x25012, float:2.12395E-40)
                r5.replyToMessage((android.os.Message) r2, (int) r6, (int) r9)
                goto L_0x14dd
            L_0x0043:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                if (r0 == r5) goto L_0x0053
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r0 != r5) goto L_0x0080
            L_0x0053:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.IState r5 = r5.getCurrentState()
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r6 = r6.mConnectedState
                if (r5 != r6) goto L_0x0080
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 151571(0x25013, float:2.12396E-40)
                r5.replyToMessage(r2, r6)
                r4 = 1
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 19
                r5.notifyDisconnectInternalReason(r6)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.WifiDelayDisconnect r5 = r5.mDelayDisconnect
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.NetworkInfo r6 = r6.mNetworkInfo
                r5.checkAndWait(r6)
            L_0x0080:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                int r6 = r2.sendingUid
                boolean r5 = r5.disableNetwork(r0, r6)
                if (r5 == 0) goto L_0x00b1
                if (r4 != 0) goto L_0x0098
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 151571(0x25013, float:2.12396E-40)
                r5.replyToMessage(r2, r6)
            L_0x0098:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                if (r0 == r5) goto L_0x00a8
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r0 != r5) goto L_0x14dd
            L_0x00a8:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 19
                r5.disconnectCommand(r9, r6)
                goto L_0x14dd
            L_0x00b1:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = "Failed to disable network"
                r5.loge(r6)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int unused = r5.mMessageHandlingStatus = r14
                if (r4 != 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 151570(0x25012, float:2.12395E-40)
                r5.replyToMessage((android.os.Message) r2, (int) r6, (int) r9)
                goto L_0x14dd
            L_0x00c9:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.NetworkUpdateResult r0 = r0.saveNetworkConfigAndSendReply(r2)
                int r4 = r0.getNetworkId()
                boolean r6 = r0.isSuccess()
                if (r6 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                int r6 = r6.getNetworkId()
                if (r6 != r4) goto L_0x14dd
                boolean r6 = r0.hasCredentialChanged()
                if (r6 == 0) goto L_0x0119
                java.lang.Object r5 = r2.obj
                android.net.wifi.WifiConfiguration r5 = (android.net.wifi.WifiConfiguration) r5
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "SAVE_NETWORK credential changed for config="
                r8.append(r9)
                java.lang.String r9 = r5.configKey()
                r8.append(r9)
                java.lang.String r9 = ", Reconnecting."
                r8.append(r9)
                java.lang.String r8 = r8.toString()
                r6.logi(r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r2.sendingUid
                java.lang.String r9 = "any"
                r6.startConnectToNetwork(r4, r8, r9)
                goto L_0x14dd
            L_0x0119:
                boolean r6 = r0.hasProxyChanged()
                if (r6 == 0) goto L_0x0149
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r6 = r6.mIpClient
                if (r6 == 0) goto L_0x0149
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = "Reconfiguring proxy on connection"
                r6.log(r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.getCurrentWifiConfiguration()
                if (r6 == 0) goto L_0x0144
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r8 = r8.mIpClient
                android.net.ProxyInfo r9 = r6.getHttpProxy()
                r8.setHttpProxy(r9)
                goto L_0x0149
            L_0x0144:
                java.lang.String r8 = "CMD_SAVE_NETWORK proxy change - but no current Wi-Fi config"
                android.util.Log.w(r5, r8)
            L_0x0149:
                boolean r6 = r0.hasIpChanged()
                if (r6 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = "Reconfiguring IP on connection"
                r6.log(r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.getCurrentWifiConfiguration()
                if (r6 == 0) goto L_0x0168
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r8 = r5.mObtainingIpState
                r5.transitionTo(r8)
                goto L_0x016d
            L_0x0168:
                java.lang.String r8 = "CMD_SAVE_NETWORK Ip change - but no current Wi-Fi config"
                android.util.Log.w(r5, r8)
            L_0x016d:
                goto L_0x14dd
            L_0x016f:
                int r0 = r2.arg1
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r4 = r4.mWifiConfigManager
                android.net.wifi.WifiConfiguration r4 = r4.getConfiguredNetwork((int) r0)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiGeofenceManager r5 = r5.mWifiGeofenceManager
                boolean r5 = r5.isSupported()
                if (r5 == 0) goto L_0x0190
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiGeofenceManager r5 = r5.mWifiGeofenceManager
                r5.forgetNetwork(r4)
            L_0x0190:
                com.samsung.android.server.wifi.WifiRoamingAssistant r5 = com.samsung.android.server.wifi.WifiRoamingAssistant.getInstance()
                if (r5 == 0) goto L_0x01a3
                if (r4 == 0) goto L_0x01a3
                com.samsung.android.server.wifi.WifiRoamingAssistant r5 = com.samsung.android.server.wifi.WifiRoamingAssistant.getInstance()
                java.lang.String r6 = r4.configKey()
                r5.forgetNetwork(r6)
            L_0x01a3:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                boolean r5 = r5.deleteNetworkConfigAndSendReply(r2, r7)
                if (r5 != 0) goto L_0x01ad
                goto L_0x14dd
            L_0x01ad:
                boolean r5 = com.android.server.wifi.ClientModeImpl.ENBLE_WLAN_CONFIG_ANALYTICS
                if (r5 == 0) goto L_0x01ba
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 304(0x130, float:4.26E-43)
                r5.setAnalyticsUserDisconnectReason(r6)
            L_0x01ba:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                if (r0 == r5) goto L_0x01ca
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r0 != r5) goto L_0x14dd
            L_0x01ca:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.disconnectCommand(r9, r10)
                goto L_0x14dd
            L_0x01d1:
                int r0 = r2.arg1
                java.lang.Object r4 = r2.obj
                android.net.wifi.WifiConfiguration r4 = (android.net.wifi.WifiConfiguration) r4
                r10 = 0
                r13 = 0
                if (r4 == 0) goto L_0x02d5
                com.android.server.wifi.ClientModeImpl r15 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r15 = r15.mWifiConfigManager
                int r15 = r15.increaseAndGetPriority()
                r4.priority = r15
                com.android.server.wifi.ClientModeImpl r15 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r15 = r15.mContext
                android.sec.enterprise.WifiPolicyCache r15 = android.sec.enterprise.WifiPolicyCache.getInstance(r15)
                boolean r15 = r15.isNetworkAllowed(r4, r9)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                r6.<init>()
                java.lang.String r12 = "CONNECT_NETWORK isAllowed="
                r6.append(r12)
                r6.append(r15)
                java.lang.String r6 = r6.toString()
                r7.logd(r6)
                if (r15 != 0) goto L_0x023a
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r6 = r6.mContext
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r12 = "Connecting to Wi-Fi network whose ID is "
                r7.append(r12)
                r7.append(r0)
                java.lang.String r12 = " failed"
                r7.append(r12)
                java.lang.String r7 = r7.toString()
                com.samsung.android.server.wifi.WifiMobileDeviceManager.auditLog(r6, r11, r9, r5, r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int unused = r5.mMessageHandlingStatus = r14
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 9
                r5.replyToMessage((android.os.Message) r2, (int) r8, (int) r6)
                goto L_0x14dd
            L_0x023a:
                boolean r5 = r4.isEphemeral()
                if (r5 != 0) goto L_0x0253
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ScanRequestProxy r6 = r6.mScanRequestProxy
                java.util.List r6 = r6.getScanResults()
                r5.updateBssidWhitelist(r4, r6)
            L_0x0253:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                int r6 = r2.sendingUid
                com.android.server.wifi.NetworkUpdateResult r5 = r5.addOrUpdateNetwork(r4, r6)
                boolean r6 = r5.isSuccess()
                if (r6 != 0) goto L_0x028c
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r11 = "CONNECT_NETWORK adding/updating config="
                r7.append(r11)
                r7.append(r4)
                java.lang.String r11 = " failed"
                r7.append(r11)
                java.lang.String r7 = r7.toString()
                r6.loge(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int unused = r6.mMessageHandlingStatus = r14
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r6.replyToMessage((android.os.Message) r2, (int) r8, (int) r9)
                goto L_0x14dd
            L_0x028c:
                java.lang.String r6 = com.android.server.wifi.ClientModeImpl.CSC_CONFIG_OP_BRANDING
                java.lang.String r7 = "VZW"
                boolean r6 = r7.equals(r6)
                if (r6 != 0) goto L_0x02a4
                java.lang.String r6 = com.android.server.wifi.ClientModeImpl.CSC_CONFIG_OP_BRANDING
                java.lang.String r7 = "SKT"
                boolean r6 = r7.equals(r6)
                if (r6 == 0) goto L_0x02c0
            L_0x02a4:
                java.lang.String r6 = r4.SSID
                boolean r6 = android.text.TextUtils.isEmpty(r6)
                if (r6 != 0) goto L_0x02c0
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                boolean r6 = r6.checkAndShowSimRemovedDialog(r4)
                if (r6 == 0) goto L_0x02c0
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int unused = r6.mMessageHandlingStatus = r14
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r6.replyToMessage((android.os.Message) r2, (int) r8, (int) r9)
                goto L_0x14dd
            L_0x02c0:
                int r0 = r5.getNetworkId()
                com.samsung.android.net.wifi.OpBrandingLoader$Vendor r6 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.VZW
                com.samsung.android.net.wifi.OpBrandingLoader$Vendor r7 = com.android.server.wifi.ClientModeImpl.mOpBranding
                if (r6 != r7) goto L_0x02d1
                boolean r6 = r5.isNewNetwork()
                r13 = r6
            L_0x02d1:
                boolean r10 = r5.hasCredentialChanged()
            L_0x02d5:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                r6 = -1
                if (r5 == r6) goto L_0x02eb
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r5 == r0) goto L_0x02eb
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.notifyDisconnectInternalReason(r11)
            L_0x02eb:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r2.sendingUid
                boolean r5 = r5.connectToUserSelectNetwork(r0, r6, r10)
                if (r5 != 0) goto L_0x0303
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int unused = r5.mMessageHandlingStatus = r14
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 9
                r5.replyToMessage((android.os.Message) r2, (int) r8, (int) r6)
                goto L_0x14dd
            L_0x0303:
                com.samsung.android.net.wifi.OpBrandingLoader$Vendor r5 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.VZW
                com.samsung.android.net.wifi.OpBrandingLoader$Vendor r6 = com.android.server.wifi.ClientModeImpl.mOpBranding
                if (r5 != r6) goto L_0x0324
                if (r13 == 0) goto L_0x0324
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.SemWifiHiddenNetworkTracker r5 = r5.mSemWifiHiddenNetworkTracker
                if (r5 == 0) goto L_0x0324
                if (r4 == 0) goto L_0x0324
                boolean r5 = r4.hiddenSSID
                if (r5 == 0) goto L_0x0324
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.SemWifiHiddenNetworkTracker r5 = r5.mSemWifiHiddenNetworkTracker
                r5.startTracking(r4)
            L_0x0324:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r5 = r5.mWifiMetrics
                r6 = 13
                r5.logStaEvent((int) r6, (android.net.wifi.WifiConfiguration) r4)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.broadcastWifiCredentialChanged(r9, r4)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 151555(0x25003, float:2.12374E-40)
                r5.replyToMessage(r2, r6)
                goto L_0x14dd
            L_0x033e:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r0 = r0.mPasspointManager
                java.lang.Object r4 = r2.obj
                com.android.server.wifi.hotspot2.WnmData r4 = (com.android.server.wifi.hotspot2.WnmData) r4
                r0.receivedWnmFrame(r4)
                goto L_0x14dd
            L_0x034d:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r0 = r0.mPasspointManager
                java.lang.Object r4 = r2.obj
                com.android.server.wifi.hotspot2.IconEvent r4 = (com.android.server.wifi.hotspot2.IconEvent) r4
                r0.notifyIconDone(r4)
                goto L_0x14dd
            L_0x035c:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r0.mDidBlackListBSSID = r9
                java.lang.Object r0 = r2.obj
                r4 = r0
                java.lang.String r4 = (java.lang.String) r4
                if (r4 != 0) goto L_0x036f
                java.lang.String r0 = "Bssid Pruned event: no obj in message!"
                android.util.Log.e(r5, r0)
                goto L_0x14dd
            L_0x036f:
                java.lang.String r0 = "\\s"
                java.lang.String[] r6 = r4.split(r0)
                int r0 = r6.length
                if (r0 == r13) goto L_0x037f
                java.lang.String r0 = "Bssid Pruned event: wrong string obj format!"
                android.util.Log.e(r5, r0)
                goto L_0x14dd
            L_0x037f:
                r7 = r6[r9]
                r8 = 1
                r10 = r6[r8]
                r8 = -2147483648(0xffffffff80000000, float:-0.0)
                r0 = 65536(0x10000, float:9.18355E-41)
                r12 = r6[r15]     // Catch:{ NumberFormatException -> 0x0443 }
                int r12 = java.lang.Integer.parseInt(r12)     // Catch:{ NumberFormatException -> 0x0443 }
                int r12 = r12 + r0
                r0 = 65537(0x10001, float:9.1837E-41)
                if (r12 == r0) goto L_0x0399
                r0 = 65538(0x10002, float:9.1838E-41)
                if (r12 != r0) goto L_0x03a1
            L_0x0399:
                r0 = r6[r11]     // Catch:{ NumberFormatException -> 0x0443 }
                int r0 = java.lang.Integer.parseInt(r0)     // Catch:{ NumberFormatException -> 0x0443 }
                int r8 = r0 * 1000
            L_0x03a1:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r11 = "Bssid Pruned event: ssid="
                r0.append(r11)
                r0.append(r7)
                java.lang.String r11 = " bssid="
                r0.append(r11)
                r0.append(r10)
                java.lang.String r11 = " reason code="
                r0.append(r11)
                r0.append(r12)
                java.lang.String r11 = " timeRemaining="
                r0.append(r11)
                r0.append(r8)
                java.lang.String r0 = r0.toString()
                android.util.Log.d(r5, r0)
                if (r10 == 0) goto L_0x03d6
                boolean r0 = android.text.TextUtils.isEmpty(r10)
                if (r0 == 0) goto L_0x03dc
            L_0x03d6:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r10 = r0.mTargetRoamBSSID
            L_0x03dc:
                if (r10 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r11 = com.android.server.wifi.ClientModeImpl.this
                int r11 = r11.mTargetNetworkId
                android.net.wifi.WifiConfiguration r0 = r0.getConfiguredNetwork((int) r11)
                if (r0 == 0) goto L_0x0434
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r11 = r0.getNetworkSelectionStatus()
                java.lang.String r11 = r11.getNetworkSelectionBSSID()
                java.lang.String r13 = "any"
                boolean r13 = r13.equals(r11)
                if (r13 != 0) goto L_0x0434
                com.android.server.wifi.ClientModeImpl r13 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r13 = r13.mWifiNative
                com.android.server.wifi.ClientModeImpl r14 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r14 = r14.mInterfaceName
                com.android.server.wifi.ClientModeImpl r15 = com.android.server.wifi.ClientModeImpl.this
                int r15 = r15.mTargetNetworkId
                r13.removeNetworkIfCurrent(r14, r15)
                java.lang.StringBuilder r13 = new java.lang.StringBuilder
                r13.<init>()
                java.lang.String r14 = "Bssid Pruned event: remove networkid="
                r13.append(r14)
                com.android.server.wifi.ClientModeImpl r14 = com.android.server.wifi.ClientModeImpl.this
                int r14 = r14.mTargetNetworkId
                r13.append(r14)
                java.lang.String r14 = " from supplicant"
                r13.append(r14)
                java.lang.String r13 = r13.toString()
                android.util.Log.d(r5, r13)
            L_0x0434:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r11 = r5.mWifiConnectivityManager
                boolean r9 = r11.trackBssid(r10, r9, r12, r8)
                boolean unused = r5.mDidBlackListBSSID = r9
                goto L_0x14dd
            L_0x0443:
                r0 = move-exception
                java.lang.StringBuilder r9 = new java.lang.StringBuilder
                r9.<init>()
                java.lang.String r11 = "Bssid Pruned event: wrong reasonCode or timeRemaining!"
                r9.append(r11)
                r9.append(r0)
                java.lang.String r9 = r9.toString()
                android.util.Log.e(r5, r9)
                goto L_0x14dd
            L_0x045a:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r0 = r0.mPasspointManager
                java.lang.Object r4 = r2.obj
                com.android.server.wifi.hotspot2.AnqpEvent r4 = (com.android.server.wifi.hotspot2.AnqpEvent) r4
                r0.notifyANQPDone(r4)
                goto L_0x14dd
            L_0x0469:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.BaseWifiDiagnostics r0 = r0.mWifiDiagnostics
                r4 = 1
                r0.triggerBugReportDataCapture(r4)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r0.mDidBlackListBSSID = r9
                java.lang.Object r0 = r2.obj
                java.lang.String r0 = (java.lang.String) r0
                int r4 = r2.arg1
                if (r4 <= 0) goto L_0x0482
                r4 = 1
                goto L_0x0483
            L_0x0482:
                r4 = r9
            L_0x0483:
                int r6 = r2.arg2
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "Association Rejection event: bssid="
                r7.append(r8)
                r7.append(r0)
                java.lang.String r8 = " reason code="
                r7.append(r8)
                r7.append(r6)
                java.lang.String r8 = " timedOut="
                r7.append(r8)
                java.lang.String r8 = java.lang.Boolean.toString(r4)
                r7.append(r8)
                java.lang.String r7 = r7.toString()
                android.util.Log.d(r5, r7)
                if (r0 == 0) goto L_0x04b5
                boolean r5 = android.text.TextUtils.isEmpty(r0)
                if (r5 == 0) goto L_0x04bb
            L_0x04b5:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r0 = r5.mTargetRoamBSSID
            L_0x04bb:
                if (r0 == 0) goto L_0x04ca
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r7 = r5.mWifiConnectivityManager
                boolean r7 = r7.trackBssid(r0, r9, r6)
                boolean unused = r5.mDidBlackListBSSID = r7
            L_0x04ca:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mTargetNetworkId
                r5.updateNetworkSelectionStatus((int) r7, (int) r15)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mTargetNetworkId
                r5.setRecentFailureAssociationStatus(r7, r6)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.SupplicantStateTracker r5 = r5.mSupplicantStateTracker
                r7 = 147499(0x2402b, float:2.0669E-40)
                r5.sendMessage(r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                if (r4 == 0) goto L_0x04fb
                r15 = 11
                goto L_0x04fc
            L_0x04fb:
            L_0x04fc:
                r7 = 1
                r5.reportConnectionAttemptEnd(r15, r7, r9)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiInjector r5 = r5.mWifiInjector
                com.android.server.wifi.WifiLastResortWatchdog r5 = r5.getWifiLastResortWatchdog()
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.getTargetSsid()
                r5.noteConnectionFailureAndTriggerIfNeeded(r8, r0, r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r7 = 12
                r5.notifyDisconnectInternalReason(r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                r7 = -1
                if (r5 == r7) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mTargetNetworkId
                android.net.wifi.WifiConfiguration r5 = r5.getConfiguredNetwork((int) r7)
                if (r5 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                r8 = 14
                int r16 = r7.mTargetNetworkId
                int r9 = r5.status
                int r10 = r5.numAssociation
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r11 = r5.getNetworkSelectionStatus()
                int r21 = r11.getNetworkSelectionStatus()
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r11 = r5.getNetworkSelectionStatus()
                int r22 = r11.getNetworkSelectionDisableReason()
                r17 = r0
                r18 = r6
                r19 = r9
                r20 = r10
                android.os.Bundle r9 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForAssocReject(r16, r17, r18, r19, r20, r21, r22)
                r7.report(r8, r9)
                goto L_0x14dd
            L_0x0563:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "Received SUP_REQUEST_SIM_AUTH"
                r0.logd(r4)
                java.lang.Object r0 = r2.obj
                com.android.server.wifi.util.TelephonyUtil$SimAuthRequestData r0 = (com.android.server.wifi.util.TelephonyUtil.SimAuthRequestData) r0
                if (r0 == 0) goto L_0x058c
                int r4 = r0.protocol
                if (r4 != r13) goto L_0x057b
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.handleGsmAuthRequest(r0)
                goto L_0x14dd
            L_0x057b:
                int r4 = r0.protocol
                r5 = 5
                if (r4 == r5) goto L_0x0585
                int r4 = r0.protocol
                r5 = 6
                if (r4 != r5) goto L_0x14dd
            L_0x0585:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.handle3GAuthRequest(r0)
                goto L_0x14dd
            L_0x058c:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r5 = "Invalid SIM auth request"
                r4.loge(r5)
                goto L_0x14dd
            L_0x0595:
                int r0 = r2.arg2
                r4 = 0
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                if (r6 == 0) goto L_0x062d
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                int r6 = r6.networkId
                if (r6 != r0) goto L_0x062d
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                boolean r6 = com.android.server.wifi.util.TelephonyUtil.isSimConfig(r6)
                if (r6 == 0) goto L_0x062d
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.telephony.TelephonyManager r6 = r6.getTelephonyManager()
                com.android.server.wifi.util.TelephonyUtil r7 = new com.android.server.wifi.util.TelephonyUtil
                r7.<init>()
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r8 = r8.mTargetWifiConfiguration
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiInjector r10 = r10.mWifiInjector
                com.android.server.wifi.CarrierNetworkConfig r10 = r10.getCarrierNetworkConfig()
                android.util.Pair r6 = com.android.server.wifi.util.TelephonyUtil.getSimIdentity(r6, r7, r8, r10)
                if (r6 == 0) goto L_0x0628
                java.lang.Object r7 = r6.first
                if (r7 == 0) goto L_0x0628
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "SUP_REQUEST_IDENTITY: identity ="
                r7.append(r8)
                java.lang.Object r8 = r6.first
                java.lang.String r8 = (java.lang.String) r8
                r10 = 7
                java.lang.String r8 = r8.substring(r9, r10)
                r7.append(r8)
                java.lang.String r7 = r7.toString()
                android.util.Log.i(r5, r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r5 = r5.mWifiNative
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mInterfaceName
                java.lang.Object r8 = r6.first
                java.lang.String r8 = (java.lang.String) r8
                java.lang.Object r9 = r6.second
                java.lang.String r9 = (java.lang.String) r9
                r5.simIdentityResponse(r7, r0, r8, r9)
                r4 = 1
                java.lang.Object r5 = r6.second
                java.lang.CharSequence r5 = (java.lang.CharSequence) r5
                boolean r5 = android.text.TextUtils.isEmpty(r5)
                if (r5 == 0) goto L_0x062d
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r7 = r5.mTargetWifiConfiguration
                java.lang.Object r8 = r6.first
                java.lang.String r8 = (java.lang.String) r8
                r5.updateIdentityOnWifiConfiguration(r7, r8)
                goto L_0x062d
            L_0x0628:
                java.lang.String r7 = "Unable to retrieve identity from Telephony"
                android.util.Log.e(r5, r7)
            L_0x062d:
                if (r4 != 0) goto L_0x14dd
                java.lang.Object r5 = r2.obj
                java.lang.String r5 = (java.lang.String) r5
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                if (r6 == 0) goto L_0x0668
                if (r5 == 0) goto L_0x0668
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                java.lang.String r6 = r6.SSID
                if (r6 == 0) goto L_0x0668
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r6 = r6.mTargetWifiConfiguration
                java.lang.String r6 = r6.SSID
                boolean r6 = r6.equals(r5)
                if (r6 == 0) goto L_0x0668
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r7 = r7.mTargetWifiConfiguration
                int r7 = r7.networkId
                r8 = 9
                r6.updateNetworkSelectionStatus((int) r7, (int) r8)
            L_0x0668:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r6 = r6.mWifiMetrics
                r7 = 15
                r6.logStaEvent((int) r7, (int) r15)
                goto L_0x14dd
            L_0x0675:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.BaseWifiDiagnostics r0 = r0.mWifiDiagnostics
                r0.triggerBugReportDataCapture(r15)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.SupplicantStateTracker r0 = r0.mSupplicantStateTracker
                r4 = 147463(0x24007, float:2.0664E-40)
                r0.sendMessage(r4)
                r0 = 3
                int r4 = r2.arg1
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mTargetNetworkId
                android.net.wifi.WifiConfiguration r6 = r6.getConfiguredNetwork((int) r7)
                if (r6 == 0) goto L_0x0705
                if (r15 != r4) goto L_0x0705
                java.lang.Object r7 = r2.obj
                java.lang.String r7 = (java.lang.String) r7
                r8 = 1
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r10 = r10.mWifiConfigManager
                int r12 = r6.networkId
                com.android.server.wifi.ScanDetailCache r10 = r10.getScanDetailCacheForNetwork(r12)
                if (r7 == 0) goto L_0x06c2
                if (r10 == 0) goto L_0x06c2
                android.net.wifi.ScanResult r12 = r10.getScanResult(r7)
                if (r12 != 0) goto L_0x06c2
                java.lang.String r14 = "authentication failure, but not for target network"
                android.util.Log.i(r5, r14)
                r8 = 0
            L_0x06c2:
                if (r8 == 0) goto L_0x0705
                java.util.BitSet r12 = r6.allowedKeyManagement
                r14 = 1
                boolean r12 = r12.get(r14)
                if (r12 != 0) goto L_0x0703
                java.util.BitSet r12 = r6.allowedKeyManagement
                r14 = 8
                boolean r12 = r12.get(r14)
                if (r12 != 0) goto L_0x0703
                java.util.BitSet r12 = r6.allowedKeyManagement
                boolean r12 = r12.get(r13)
                if (r12 != 0) goto L_0x0703
                java.util.BitSet r12 = r6.allowedKeyManagement
                r14 = 22
                boolean r12 = r12.get(r14)
                if (r12 != 0) goto L_0x0703
                java.util.BitSet r12 = r6.allowedKeyManagement
                boolean r12 = r12.get(r9)
                if (r12 == 0) goto L_0x06f8
                java.lang.String[] r12 = r6.wepKeys
                r12 = r12[r9]
                if (r12 == 0) goto L_0x06f8
                goto L_0x0703
            L_0x06f8:
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                boolean r12 = r12.isPermanentWrongPasswordFailure(r6, r4)
                if (r12 == 0) goto L_0x0705
                r0 = 13
                goto L_0x0705
            L_0x0703:
                r0 = 13
            L_0x0705:
                if (r4 != r11) goto L_0x0718
                int r7 = r2.arg2
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int r10 = r8.mTargetNetworkId
                r8.handleEapAuthFailure(r10, r7)
                r8 = 1031(0x407, float:1.445E-42)
                if (r7 != r8) goto L_0x0718
                r0 = 14
            L_0x0718:
                if (r6 == 0) goto L_0x074a
                android.net.wifi.WifiEnterpriseConfig r7 = r6.enterpriseConfig
                int r7 = r7.getEapMethod()
                boolean r7 = com.android.server.wifi.util.TelephonyUtil.isSimEapMethod(r7)
                if (r7 == 0) goto L_0x074a
                android.net.wifi.WifiEnterpriseConfig r7 = r6.enterpriseConfig
                java.lang.String r7 = r7.getAnonymousIdentity()
                boolean r7 = android.text.TextUtils.isEmpty(r7)
                if (r7 != 0) goto L_0x074a
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = "EAP Pseudonym reset due to AUTHENTICATION_FAILURE"
                r7.log(r8)
                android.net.wifi.WifiEnterpriseConfig r7 = r6.enterpriseConfig
                r8 = 0
                r7.setAnonymousIdentity(r8)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r7 = r7.mWifiConfigManager
                r8 = 1010(0x3f2, float:1.415E-42)
                r7.addOrUpdateNetwork(r6, r8)
            L_0x074a:
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r7 = r7.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r8.mTargetNetworkId
                r7.updateNetworkSelectionStatus((int) r8, (int) r0)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r7 = r7.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r8.mTargetNetworkId
                r7.clearRecentFailureReason(r8)
                if (r4 == 0) goto L_0x0779
                r7 = 1
                if (r4 == r7) goto L_0x0777
                if (r4 == r15) goto L_0x0775
                if (r4 == r11) goto L_0x0773
                r7 = 0
                goto L_0x077b
            L_0x0773:
                r7 = 4
                goto L_0x077b
            L_0x0775:
                r7 = 3
                goto L_0x077b
            L_0x0777:
                r7 = 2
                goto L_0x077b
            L_0x0779:
                r7 = 1
            L_0x077b:
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                r10 = 1
                r8.reportConnectionAttemptEnd(r11, r10, r7)
                if (r4 == r15) goto L_0x079c
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiInjector r8 = r8.mWifiInjector
                com.android.server.wifi.WifiLastResortWatchdog r8 = r8.getWifiLastResortWatchdog()
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r10 = r10.getTargetSsid()
                com.android.server.wifi.ClientModeImpl r11 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r11 = r11.mTargetRoamBSSID
                r8.noteConnectionFailureAndTriggerIfNeeded(r10, r11, r15)
            L_0x079c:
                java.lang.String r8 = "Wi-Fi is failed to connect to "
                if (r6 == 0) goto L_0x085d
                java.lang.String r10 = r6.SSID
                if (r10 == 0) goto L_0x085d
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                java.lang.String r11 = r6.getPrintableSsid()
                r10.append(r11)
                java.lang.String r11 = " network using "
                r10.append(r11)
                java.lang.String r8 = r10.toString()
                java.util.BitSet r10 = r6.allowedKeyManagement
                boolean r10 = r10.get(r9)
                if (r10 != 0) goto L_0x084b
                java.util.BitSet r10 = r6.allowedKeyManagement
                r11 = 1
                boolean r10 = r10.get(r11)
                if (r10 != 0) goto L_0x084b
                java.util.BitSet r10 = r6.allowedKeyManagement
                boolean r10 = r10.get(r13)
                if (r10 != 0) goto L_0x084b
                java.util.BitSet r10 = r6.allowedKeyManagement
                r11 = 22
                boolean r10 = r10.get(r11)
                if (r10 == 0) goto L_0x07e0
                goto L_0x084b
            L_0x07e0:
                android.net.wifi.WifiEnterpriseConfig r10 = r6.enterpriseConfig
                if (r10 == 0) goto L_0x087b
                android.net.wifi.WifiEnterpriseConfig r10 = r6.enterpriseConfig
                int r10 = r10.getEapMethod()
                r11 = 1
                if (r10 != r11) goto L_0x081c
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                java.lang.String r11 = "EAP-TLS channel"
                r10.append(r11)
                java.lang.String r8 = r10.toString()
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                java.lang.String r11 = "Wi-Fi is failed to connect to "
                r10.append(r11)
                java.lang.String r11 = r6.getPrintableSsid()
                r10.append(r11)
                java.lang.String r11 = " network using EAP-TLS channel"
                r10.append(r11)
                java.lang.String r10 = r10.toString()
                android.util.Log.e(r5, r10)
                goto L_0x087b
            L_0x081c:
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                java.lang.String r11 = "802.1X channel"
                r10.append(r11)
                java.lang.String r8 = r10.toString()
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                java.lang.String r11 = "Wi-Fi is connected to "
                r10.append(r11)
                java.lang.String r11 = r6.getPrintableSsid()
                r10.append(r11)
                java.lang.String r11 = " network using 802.1X channel"
                r10.append(r11)
                java.lang.String r10 = r10.toString()
                android.util.Log.e(r5, r10)
                goto L_0x087b
            L_0x084b:
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                java.lang.String r11 = "802.11-2012 channel."
                r10.append(r11)
                java.lang.String r8 = r10.toString()
                goto L_0x087b
            L_0x085d:
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                r10.append(r8)
                com.android.server.wifi.ClientModeImpl r11 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r11 = r11.mWifiInfo
                java.lang.String r11 = r11.getSSID()
                r10.append(r11)
                java.lang.String r11 = " network."
                r10.append(r11)
                java.lang.String r8 = r10.toString()
            L_0x087b:
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r10 = r10.mContext
                java.lang.StringBuilder r11 = new java.lang.StringBuilder
                r11.<init>()
                r11.append(r8)
                java.lang.String r12 = " Reason: Authentication failure."
                r11.append(r12)
                java.lang.String r11 = r11.toString()
                r12 = 5
                com.samsung.android.server.wifi.WifiMobileDeviceManager.auditLog(r10, r12, r9, r5, r11)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r8 = 13
                r5.notifyDisconnectInternalReason(r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                r8 = -1
                if (r5 == r8) goto L_0x14dd
                if (r6 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r8 = 15
                int r9 = r5.mTargetNetworkId
                int r10 = r2.arg2
                int r11 = r6.status
                int r12 = r6.numAssociation
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r13 = r6.getNetworkSelectionStatus()
                int r13 = r13.getNetworkSelectionStatus()
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r14 = r6.getNetworkSelectionStatus()
                int r14 = r14.getNetworkSelectionDisableReason()
                android.os.Bundle r9 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForAuthFail(r9, r10, r11, r12, r13, r14)
                r5.report(r8, r9)
                goto L_0x14dd
            L_0x08cf:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.SupplicantState r0 = r0.handleSupplicantStateChange(r2)
                android.net.wifi.SupplicantState r5 = android.net.wifi.SupplicantState.DISCONNECTED
                if (r0 != r5) goto L_0x0904
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.net.NetworkInfo r5 = r5.mNetworkInfo
                android.net.NetworkInfo$State r5 = r5.getState()
                android.net.NetworkInfo$State r6 = android.net.NetworkInfo.State.DISCONNECTED
                if (r5 == r6) goto L_0x0904
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                boolean r5 = r5.mVerboseLoggingEnabled
                if (r5 == 0) goto L_0x08f6
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = "Missed CTRL-EVENT-DISCONNECTED, disconnect"
                r5.log(r6)
            L_0x08f6:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.handleNetworkDisconnect()
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r6 = r5.mDisconnectedState
                r5.transitionTo(r6)
            L_0x0904:
                android.net.wifi.SupplicantState r5 = android.net.wifi.SupplicantState.COMPLETED
                if (r0 != r5) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r5 = r5.mIpClient
                if (r5 == 0) goto L_0x0919
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r5 = r5.mIpClient
                r5.confirmConfiguration()
            L_0x0919:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiScoreReport r5 = r5.mWifiScoreReport
                r5.noteIpCheck()
                java.lang.String r5 = com.android.server.wifi.ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION
                boolean r4 = r4.equals(r5)
                if (r4 == 0) goto L_0x14dd
                com.samsung.android.feature.SemCscFeature r4 = com.samsung.android.feature.SemCscFeature.getInstance()
                java.lang.String r5 = "CscFeature_Wifi_DisableMWIPS"
                boolean r4 = r4.getBoolean(r5)
                if (r4 != 0) goto L_0x14dd
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                if (r4 == 0) goto L_0x14dd
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                r5 = 17
                r4.sendEmptyMessage(r5)
                goto L_0x14dd
            L_0x094a:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean r0 = r0.mVerboseLoggingEnabled
                if (r0 == 0) goto L_0x0959
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "ConnectModeState: Network connection lost "
                r0.log(r4)
            L_0x0959:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r0.mTargetNetworkId
                java.lang.Object r5 = r2.obj
                java.lang.String r5 = (java.lang.String) r5
                int r6 = r2.arg1
                if (r6 == 0) goto L_0x0968
                r9 = 1
            L_0x0968:
                int r6 = r2.arg2
                r0.checkAndUpdateUnstableAp(r4, r5, r9, r6)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.handleNetworkDisconnect()
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r4 = r0.mDisconnectedState
                r0.transitionTo(r4)
                goto L_0x14dd
            L_0x097d:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean r0 = r0.mVerboseLoggingEnabled
                if (r0 == 0) goto L_0x098c
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "Network connection established"
                r0.log(r4)
            L_0x098c:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.arg1
                int unused = r0.mLastNetworkId = r4
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r0.mLastNetworkId
                int unused = r0.mLastConnectedNetworkId = r4
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.mLastNetworkId
                r0.clearRecentFailureReason(r4)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.Object r4 = r2.obj
                java.lang.String r4 = (java.lang.String) r4
                java.lang.String unused = r0.mLastBssid = r4
                int r0 = r2.arg2
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r4 = r4.getCurrentWifiConfiguration()
                if (r4 == 0) goto L_0x0b78
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mLastBssid
                r6.setBSSID(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mLastNetworkId
                r6.setNetworkId(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r7 = r7.mWifiNative
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mInterfaceName
                java.lang.String r7 = r7.getMacAddress(r8)
                r6.setMacAddress(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                int r7 = r4.networkId
                com.android.server.wifi.ScanDetailCache r6 = r6.getScanDetailCacheForNetwork(r7)
                if (r6 == 0) goto L_0x0a59
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mLastBssid
                if (r7 == 0) goto L_0x0a59
                java.lang.String r7 = "scan detail is in cache, find scanResult from cache"
                android.util.Log.d(r5, r7)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mLastBssid
                android.net.wifi.ScanResult r7 = r6.getScanResult(r7)
                if (r7 == 0) goto L_0x0a3e
                java.lang.String r8 = "found scanResult! update mWifiInfo"
                android.util.Log.d(r5, r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r5 = r5.mWifiInfo
                int r8 = r7.frequency
                r5.setFrequency(r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.updateWifiInfoForVendors(r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r5 = r5.mWifiInfo
                int r8 = r7.wifiMode
                r5.setWifiMode(r8)
                goto L_0x0acd
            L_0x0a3e:
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "can't update vendor infos, bssid: "
                r8.append(r9)
                com.android.server.wifi.ClientModeImpl r9 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r9 = r9.mLastBssid
                r8.append(r9)
                java.lang.String r8 = r8.toString()
                android.util.Log.d(r5, r8)
                goto L_0x0acd
            L_0x0a59:
                if (r6 != 0) goto L_0x0acd
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mLastBssid
                if (r7 == 0) goto L_0x0acd
                java.lang.String r7 = "scan detail is not in cache, find scanResult from last native scan results"
                android.util.Log.d(r5, r7)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r7 = r7.mWifiNative
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mInterfaceName
                java.util.ArrayList r7 = r7.getScanResults(r8)
                java.util.Iterator r8 = r7.iterator()
            L_0x0a7c:
                boolean r9 = r8.hasNext()
                if (r9 == 0) goto L_0x0ace
                java.lang.Object r9 = r8.next()
                com.android.server.wifi.ScanDetail r9 = (com.android.server.wifi.ScanDetail) r9
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r10 = r10.mLastBssid
                java.lang.String r11 = r9.getBSSIDString()
                boolean r10 = r10.equals(r11)
                if (r10 == 0) goto L_0x0acc
                android.net.wifi.ScanResult r8 = r9.getScanResult()
                java.lang.String r10 = "found scanResult! update the cache and mWifiInfo"
                android.util.Log.d(r5, r10)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                int r10 = r10.mLastNetworkId
                r5.updateScanDetailForNetwork(r10, r9)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r5 = r5.mWifiInfo
                int r10 = r8.frequency
                r5.setFrequency(r10)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.updateWifiInfoForVendors(r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r5 = r5.mWifiInfo
                int r10 = r8.wifiMode
                r5.setWifiMode(r10)
                goto L_0x0ace
            L_0x0acc:
                goto L_0x0a7c
            L_0x0acd:
            L_0x0ace:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r5 = r5.mWifiConnectivityManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mLastBssid
                r8 = 1
                r5.trackBssid(r7, r8, r0)
                android.net.wifi.WifiEnterpriseConfig r5 = r4.enterpriseConfig
                if (r5 == 0) goto L_0x0b38
                android.net.wifi.WifiEnterpriseConfig r5 = r4.enterpriseConfig
                int r5 = r5.getEapMethod()
                boolean r5 = com.android.server.wifi.util.TelephonyUtil.isSimEapMethod(r5)
                if (r5 == 0) goto L_0x0b38
                android.net.wifi.WifiEnterpriseConfig r5 = r4.enterpriseConfig
                java.lang.String r5 = r5.getAnonymousIdentity()
                boolean r5 = com.android.server.wifi.util.TelephonyUtil.isAnonymousAtRealmIdentity(r5)
                if (r5 != 0) goto L_0x0b38
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r5 = r5.mWifiNative
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mInterfaceName
                java.lang.String r5 = r5.getEapAnonymousIdentity(r7)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                boolean r7 = r7.mVerboseLoggingEnabled
                if (r7 == 0) goto L_0x0b28
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r9 = "EAP Pseudonym: "
                r8.append(r9)
                r8.append(r5)
                java.lang.String r8 = r8.toString()
                r7.log(r8)
            L_0x0b28:
                android.net.wifi.WifiEnterpriseConfig r7 = r4.enterpriseConfig
                r7.setAnonymousIdentity(r5)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r7 = r7.mWifiConfigManager
                r8 = 1010(0x3f2, float:1.415E-42)
                r7.addOrUpdateNetwork(r4, r8)
            L_0x0b38:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r5 = r5.mUnstableApController
                if (r5 == 0) goto L_0x0b4f
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r5 = r5.mUnstableApController
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mLastNetworkId
                r5.l2Connected(r7)
            L_0x0b4f:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r5.mLastBssid
                r5.sendNetworkStateChangeBroadcast(r7)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r7 = 12
                int r8 = r5.mLastNetworkId
                com.android.server.wifi.ClientModeImpl r9 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r9 = r9.mLastBssid
                android.os.Bundle r8 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForL2Connected(r8, r9)
                r5.report(r7, r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r7 = r5.mObtainingIpState
                r5.transitionTo(r7)
                goto L_0x14dd
            L_0x0b78:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                r6.<init>()
                java.lang.String r7 = "Connected to unknown networkId "
                r6.append(r7)
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r7.mLastNetworkId
                r6.append(r7)
                java.lang.String r7 = ", disconnecting..."
                r6.append(r7)
                java.lang.String r6 = r6.toString()
                r5.logw(r6)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r6 = 22
                r5.disconnectCommand(r9, r6)
                goto L_0x14dd
            L_0x0ba2:
                int r0 = r2.arg1
                r4 = 1
                if (r0 != r4) goto L_0x0bd1
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r0 = r0.mWifiMetrics
                r4 = 15
                r5 = 5
                r0.logStaEvent((int) r4, (int) r5)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r4 = 16
                r0.notifyDisconnectInternalReason(r4)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r0 = r0.mWifiNative
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = r4.mInterfaceName
                r0.disconnect(r4)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r4 = 1
                boolean unused = r0.mTemporarilyDisconnectWifi = r4
                goto L_0x14dd
            L_0x0bd1:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r0 = r0.mWifiNative
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = r4.mInterfaceName
                r0.reconnect(r4)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r0.mTemporarilyDisconnectWifi = r9
                goto L_0x14dd
            L_0x0be7:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                int r4 = r2.arg1
                int r5 = r2.arg2
                r0.updateNetworkSelectionStatus((int) r4, (int) r5)
                goto L_0x14dd
            L_0x0bf6:
                int r0 = r2.arg2
                r4 = 1
                if (r0 != r4) goto L_0x0bfc
                r9 = 1
            L_0x0bfc:
                r0 = r9
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r4 = r4.mWifiConfigManager
                int r5 = r2.arg1
                r4.setCaptivePortal(r5, r0)
                goto L_0x14dd
            L_0x0c0a:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r0 = r0.mUnstableApController
                if (r0 == 0) goto L_0x0c2d
                java.util.ArrayList r0 = new java.util.ArrayList
                r0.<init>()
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ScanRequestProxy r4 = r4.mScanRequestProxy
                java.util.List r4 = r4.getScanResults()
                r0.addAll(r4)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r4 = r4.mUnstableApController
                r4.verifyAll(r0)
            L_0x0c2d:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                boolean r0 = r0.mIsRoamNetwork     // Catch:{ Exception -> 0x0c61 }
                if (r0 != 0) goto L_0x0c5f
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                boolean r0 = r0.isRoamNetwork()     // Catch:{ Exception -> 0x0c61 }
                if (r0 == 0) goto L_0x0c5f
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                boolean r0 = r0.checkAndSetConnectivityInstance()     // Catch:{ Exception -> 0x0c61 }
                if (r0 == 0) goto L_0x0c55
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                boolean r0 = r0.isWifiOnly()     // Catch:{ Exception -> 0x0c61 }
                if (r0 != 0) goto L_0x0c55
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                android.net.ConnectivityManager r0 = r0.mCm     // Catch:{ Exception -> 0x0c61 }
                r4 = 1
                r0.setWifiRoamNetwork(r4)     // Catch:{ Exception -> 0x0c61 }
            L_0x0c55:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x0c61 }
                r4 = 1
                r0.mIsRoamNetwork = r4     // Catch:{ Exception -> 0x0c61 }
                java.lang.String r0 = "Roam Network"
                android.util.Log.i(r5, r0)     // Catch:{ Exception -> 0x0c61 }
            L_0x0c5f:
                goto L_0x14dd
            L_0x0c61:
                r0 = move-exception
                r0.printStackTrace()
                goto L_0x14dd
            L_0x0c67:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r0 = r0.processMessageForCallSECStringApi(r2)
                if (r0 == 0) goto L_0x0c78
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r2.what
                r4.replyToMessage((android.os.Message) r2, (int) r5, (java.lang.Object) r0)
                goto L_0x14dd
            L_0x0c78:
                return r9
            L_0x0c79:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r0 = r0.processMessageForCallSECApi(r2)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r2.what
                r4.replyToMessage((android.os.Message) r2, (int) r5, (int) r0)
                goto L_0x14dd
            L_0x0c88:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                boolean r0 = r0.processMessageForCallSECApiAsync(r2)
                if (r0 != 0) goto L_0x14dd
                return r9
            L_0x0c91:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.updateAdpsState()
                goto L_0x14dd
            L_0x0c98:
                java.lang.Object r0 = r2.obj
                android.net.wifi.hotspot2.IProvisioningCallback r0 = (android.net.wifi.hotspot2.IProvisioningCallback) r0
                android.os.Bundle r4 = r24.getData()
                java.lang.String r5 = "OsuProvider"
                android.os.Parcelable r4 = r4.getParcelable(r5)
                android.net.wifi.hotspot2.OsuProvider r4 = (android.net.wifi.hotspot2.OsuProvider) r4
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                int r6 = r2.arg1
                boolean r5 = r5.startSubscriptionProvisioning(r6, r4, r0)
                if (r5 == 0) goto L_0x0cb9
                r9 = 1
                goto L_0x0cba
            L_0x0cb9:
            L_0x0cba:
                r5 = r9
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r2.what
                r6.replyToMessage((android.os.Message) r2, (int) r7, (int) r5)
                goto L_0x14dd
            L_0x0cc4:
                int r0 = r2.arg1
                if (r0 <= 0) goto L_0x0cc9
                r9 = 1
            L_0x0cc9:
                r0 = r9
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r4 = r4.mWifiNative
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r5 = r5.mInterfaceName
                r4.configureNeighborDiscoveryOffload(r5, r0)
                goto L_0x14dd
            L_0x0cdb:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.what
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                java.lang.Object r6 = r2.obj
                java.util.List r6 = (java.util.List) r6
                java.util.Map r5 = r5.getAllMatchingFqdnsForScanResults(r6)
                r0.replyToMessage((android.os.Message) r2, (int) r4, (java.lang.Object) r5)
                goto L_0x14dd
            L_0x0cf2:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r0 = r0.mWifiConnectivityManager
                int r4 = r2.arg1
                r5 = 1
                if (r4 != r5) goto L_0x0cfe
                r9 = 1
            L_0x0cfe:
                r0.enable(r9)
                goto L_0x14dd
            L_0x0d03:
                int r0 = r2.arg1
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.stopWifiIPPacketOffload(r0)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ClientModeImpl$WifiNetworkAgent r5 = r5.mNetworkAgent
                if (r5 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ClientModeImpl$WifiNetworkAgent r5 = r5.mNetworkAgent
                r5.onSocketKeepaliveEvent(r0, r4)
                goto L_0x14dd
            L_0x0d1e:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                int r4 = r2.arg1
                java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
                int r4 = r4.intValue()
                java.util.Set r0 = r0.removeNetworksForUser(r4)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.mTargetNetworkId
                java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
                boolean r4 = r0.contains(r4)
                if (r4 != 0) goto L_0x0d52
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.mLastNetworkId
                java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
                boolean r4 = r0.contains(r4)
                if (r4 == 0) goto L_0x14dd
            L_0x0d52:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.disconnectCommand(r9, r10)
                goto L_0x14dd
            L_0x0d59:
                java.lang.Object r0 = r2.obj
                java.lang.String r0 = (java.lang.String) r0
                if (r0 == 0) goto L_0x0d7e
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r6.mTargetNetworkId
                com.android.server.wifi.ScanDetailCache r5 = r5.getScanDetailCacheForNetwork(r6)
                if (r5 == 0) goto L_0x0d7e
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r6 = r6.mWifiMetrics
                com.android.server.wifi.ScanDetail r7 = r5.getScanDetail(r0)
                r6.setConnectionScanDetail(r7)
            L_0x0d7e:
                java.lang.String r5 = com.android.server.wifi.ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION
                boolean r4 = r4.equals(r5)
                if (r4 == 0) goto L_0x0da4
                com.samsung.android.feature.SemCscFeature r4 = com.samsung.android.feature.SemCscFeature.getInstance()
                java.lang.String r5 = "CscFeature_Wifi_DisableMWIPS"
                boolean r4 = r4.getBoolean(r5)
                if (r4 != 0) goto L_0x0da4
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                if (r4 == 0) goto L_0x0da4
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                r5 = 8
                r4.sendEmptyMessage(r5)
            L_0x0da4:
                r3 = 0
                goto L_0x14dd
            L_0x0da7:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r4 = -5
                int unused = r0.mMessageHandlingStatus = r4
                goto L_0x14dd
            L_0x0daf:
                int r0 = r2.arg1
                int r6 = r2.arg2
                java.lang.Object r7 = r2.obj
                java.lang.String r7 = (java.lang.String) r7
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                boolean r12 = r12.hasConnectionRequests()
                if (r12 != 0) goto L_0x0de5
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ClientModeImpl$WifiNetworkAgent r12 = r12.mNetworkAgent
                if (r12 != 0) goto L_0x0dd0
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r5 = "CMD_START_CONNECT but no requests and not connected, bailing"
                r4.loge(r5)
                goto L_0x14dd
            L_0x0dd0:
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.util.WifiPermissionsUtil r12 = r12.mWifiPermissionsUtil
                boolean r12 = r12.checkNetworkSettingsPermission(r6)
                if (r12 != 0) goto L_0x0de5
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r5 = "CMD_START_CONNECT but no requests and connected, but app does not have sufficient permissions, bailing"
                r4.loge(r5)
                goto L_0x14dd
            L_0x0de5:
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r12 = r12.mWifiConfigManager
                android.net.wifi.WifiConfiguration r12 = r12.getConfiguredNetworkWithoutMasking(r0)
                com.android.server.wifi.ClientModeImpl r14 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r15 = new java.lang.StringBuilder
                r15.<init>()
                java.lang.String r8 = "CMD_START_CONNECT sup state "
                r15.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.SupplicantStateTracker r8 = r8.mSupplicantStateTracker
                java.lang.String r8 = r8.getSupplicantStateName()
                r15.append(r8)
                java.lang.String r8 = " my state "
                r15.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.IState r8 = r8.getCurrentState()
                java.lang.String r8 = r8.getName()
                r15.append(r8)
                java.lang.String r8 = " nid="
                r15.append(r8)
                java.lang.String r8 = java.lang.Integer.toString(r0)
                r15.append(r8)
                java.lang.String r8 = " roam="
                r15.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                boolean r8 = r8.mIsAutoRoaming
                java.lang.String r8 = java.lang.Boolean.toString(r8)
                r15.append(r8)
                java.lang.String r8 = r15.toString()
                r14.logd(r8)
                if (r12 != 0) goto L_0x0e4a
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r5 = "CMD_START_CONNECT and no config, bail out..."
                r4.loge(r5)
                goto L_0x14dd
            L_0x0e4a:
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiScoreCard r8 = r8.mWifiScoreCard
                com.android.server.wifi.ClientModeImpl r14 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r14 = r14.mWifiInfo
                r8.noteConnectionAttempt(r14)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int unused = r8.mTargetNetworkId = r0
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r8.setTargetBssid(r12, r7)
                boolean r8 = com.android.server.wifi.util.TelephonyUtil.isSimConfig(r12)
                if (r8 == 0) goto L_0x0e78
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                boolean r8 = r8.setPermanentIdentity(r12)
                if (r8 != 0) goto L_0x0e78
                java.lang.String r4 = "CMD_START_CONNECT , There is no Identity for EAP SimConfig network, skip connection"
                android.util.Log.i(r5, r4)
                goto L_0x14dd
            L_0x0e78:
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                int r8 = r8.getEapMethod()
                if (r8 != r10) goto L_0x0e9b
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                r8.setEapMethod(r9)
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                r10 = -1
                r8.setPhase1Method(r10)
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                r8.setPacFile(r4)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r4 = r4.mWifiConfigManager
                r8 = 1000(0x3e8, float:1.401E-42)
                r4.addOrUpdateNetwork(r12, r8)
            L_0x0e9b:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r4.mTargetRoamBSSID
                r10 = 5
                r4.reportConnectionAttemptStart(r12, r8, r10)
                boolean r4 = com.android.server.wifi.ClientModeImpl.ENBLE_WLAN_CONFIG_ANALYTICS
                if (r4 == 0) goto L_0x0edf
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                boolean r4 = r4.isDisconnected()
                if (r4 != 0) goto L_0x0edf
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r4 = r4.mContext
                android.content.ContentResolver r4 = r4.getContentResolver()
                java.lang.String r8 = "safe_wifi"
                int r4 = android.provider.Settings.Global.getInt(r4, r8, r9)
                r8 = 1
                if (r4 == r8) goto L_0x0edf
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r4 = r4.mWifiNative
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mInterfaceName
                r10 = 256(0x100, float:3.59E-43)
                boolean r4 = r4.setExtendedAnalyticsDisconnectReason(r8, r10)
                if (r4 != 0) goto L_0x0edf
                java.lang.String r4 = "Failed to set ExtendedAnalyticsDisconnectReason"
                android.util.Log.e(r5, r4)
            L_0x0edf:
                int r4 = r12.macRandomizationSetting
                r8 = 1
                if (r4 != r8) goto L_0x0efa
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                boolean r4 = r4.mConnectedMacRandomzationSupported
                if (r4 == 0) goto L_0x0efa
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                boolean r4 = r4.isSupportRandomMac(r12)
                if (r4 == 0) goto L_0x0efa
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.configureRandomizedMacAddress(r12)
                goto L_0x0eff
            L_0x0efa:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.setCurrentMacToFactoryMac(r12)
            L_0x0eff:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r4 = r4.mWifiNative
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mInterfaceName
                java.lang.String r4 = r4.getMacAddress(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r8 = r8.mWifiInfo
                r8.setMacAddress(r4)
                boolean r8 = com.android.server.wifi.ClientModeImpl.DBG
                if (r8 == 0) goto L_0x0f37
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r10 = "Connecting with "
                r8.append(r10)
                r8.append(r4)
                java.lang.String r10 = " as the mac address"
                r8.append(r10)
                java.lang.String r8 = r8.toString()
                android.util.Log.d(r5, r8)
            L_0x0f37:
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r8.mLastNetworkId
                r10 = -1
                if (r8 == r10) goto L_0x0f65
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r8.mLastNetworkId
                if (r8 == r0) goto L_0x0f65
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.IState r8 = r8.getCurrentState()
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r10 = r10.mConnectedState
                if (r8 != r10) goto L_0x0f65
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.WifiDelayDisconnect r8 = r8.mDelayDisconnect
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                android.net.NetworkInfo r10 = r10.mNetworkInfo
                r8.checkAndWait(r10)
            L_0x0f65:
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                if (r8 == 0) goto L_0x0ff6
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                int r8 = r8.getEapMethod()
                boolean r8 = com.android.server.wifi.util.TelephonyUtil.isSimEapMethod(r8)
                if (r8 == 0) goto L_0x0ff6
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiInjector r8 = r8.mWifiInjector
                com.android.server.wifi.CarrierNetworkConfig r8 = r8.getCarrierNetworkConfig()
                boolean r8 = r8.isCarrierEncryptionInfoAvailable()
                if (r8 == 0) goto L_0x0ff6
                android.net.wifi.WifiEnterpriseConfig r8 = r12.enterpriseConfig
                java.lang.String r8 = r8.getAnonymousIdentity()
                boolean r8 = android.text.TextUtils.isEmpty(r8)
                if (r8 == 0) goto L_0x0ff6
                boolean r8 = r12.semIsVendorSpecificSsid
                if (r8 == 0) goto L_0x0ff6
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                android.telephony.TelephonyManager r8 = r8.getTelephonyManager()
                java.lang.String r8 = com.android.server.wifi.util.TelephonyUtil.getAnonymousIdentityWith3GppRealm(r8)
                android.net.wifi.WifiEnterpriseConfig r10 = r12.enterpriseConfig
                int r10 = r10.getEapMethod()
                java.lang.String r14 = ""
                java.lang.String r15 = ""
                if (r10 != r13) goto L_0x0fae
                java.lang.String r14 = "1"
                goto L_0x0fce
            L_0x0fae:
                r13 = 5
                if (r10 != r13) goto L_0x0fb4
                java.lang.String r14 = "0"
                goto L_0x0fce
            L_0x0fb4:
                r13 = 6
                if (r10 != r13) goto L_0x0fba
                java.lang.String r14 = "6"
                goto L_0x0fce
            L_0x0fba:
                java.lang.StringBuilder r13 = new java.lang.StringBuilder
                r13.<init>()
                java.lang.String r11 = " config is not a valid EapMethod "
                r13.append(r11)
                r13.append(r10)
                java.lang.String r11 = r13.toString()
                android.util.Log.e(r5, r11)
            L_0x0fce:
                java.lang.StringBuilder r11 = new java.lang.StringBuilder
                r11.<init>()
                r11.append(r14)
                r11.append(r8)
                java.lang.String r11 = r11.toString()
                java.lang.StringBuilder r13 = new java.lang.StringBuilder
                r13.<init>()
                java.lang.String r15 = "setAnonymousIdentity "
                r13.append(r15)
                r13.append(r11)
                java.lang.String r13 = r13.toString()
                android.util.Log.i(r5, r13)
                android.net.wifi.WifiEnterpriseConfig r5 = r12.enterpriseConfig
                r5.setAnonymousIdentity(r11)
            L_0x0ff6:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r5 = r5.mWifiNative
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mInterfaceName
                boolean r5 = r5.connectToNetwork(r8, r12)
                if (r5 == 0) goto L_0x107f
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r5 = r5.mWifiMetrics
                r8 = 11
                r5.logStaEvent((int) r8, (android.net.wifi.WifiConfiguration) r12)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r10 = r12.SSID
                int r11 = r12.numAssociation
                android.os.Bundle r10 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForTryToConnect(r0, r10, r11, r7, r9)
                r5.report(r8, r10)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.Clock r8 = r5.mClock
                long r10 = r8.getWallClockMillis()
                long unused = r5.mLastConnectAttemptTimestamp = r10
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration unused = r5.mTargetWifiConfiguration = r12
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r5.mIsAutoRoaming = r9
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r5 = r12.getNetworkSelectionStatus()
                java.lang.String r5 = r5.getNetworkSelectionBSSID()
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r8 = r8.getCurrentWifiConfiguration()
                if (r8 != 0) goto L_0x1049
                r9 = 0
                goto L_0x1051
            L_0x1049:
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r9 = r8.getNetworkSelectionStatus()
                java.lang.String r9 = r9.getNetworkSelectionBSSID()
            L_0x1051:
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.IState r10 = r10.getCurrentState()
                com.android.server.wifi.ClientModeImpl r11 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r11 = r11.mDisconnectedState
                if (r10 == r11) goto L_0x107d
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                int r10 = r10.mLastNetworkId
                if (r10 != r0) goto L_0x106e
                boolean r10 = java.util.Objects.equals(r5, r9)
                if (r10 != 0) goto L_0x107d
            L_0x106e:
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                r11 = 3
                r10.notifyDisconnectInternalReason(r11)
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r11 = r10.mDisconnectingState
                r10.transitionTo(r11)
            L_0x107d:
                goto L_0x14dd
            L_0x107f:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                java.lang.String r10 = "CMD_START_CONNECT Failed to start connection to network "
                r8.append(r10)
                r8.append(r12)
                java.lang.String r8 = r8.toString()
                r5.loge(r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r8 = 5
                r10 = 1
                r5.reportConnectionAttemptEnd(r8, r10, r9)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r8 = 151554(0x25002, float:2.12372E-40)
                r5.replyToMessage((android.os.Message) r2, (int) r8, (int) r9)
                java.util.BitSet r5 = r12.allowedKeyManagement
                boolean r5 = r5.get(r10)
                if (r5 != 0) goto L_0x10d7
                java.util.BitSet r5 = r12.allowedKeyManagement
                r8 = 4
                boolean r5 = r5.get(r8)
                if (r5 != 0) goto L_0x10d7
                java.util.BitSet r5 = r12.allowedKeyManagement
                r8 = 8
                boolean r5 = r5.get(r8)
                if (r5 != 0) goto L_0x10d7
                java.util.BitSet r5 = r12.allowedKeyManagement
                r8 = 22
                boolean r5 = r5.get(r8)
                if (r5 != 0) goto L_0x10d7
                java.util.BitSet r5 = r12.allowedKeyManagement
                boolean r5 = r5.get(r9)
                if (r5 == 0) goto L_0x14dd
                java.lang.String[] r5 = r12.wepKeys
                r5 = r5[r9]
                if (r5 == 0) goto L_0x14dd
            L_0x10d7:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                r8 = 13
                r5.updateNetworkSelectionStatus((int) r0, (int) r8)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r5 = r5.mContext
                r9 = 60
                int r10 = r12.networkId
                com.samsung.android.server.wifi.SemWifiFrameworkUxUtils.sendShowInfoIntentToSettings(r5, r9, r10, r8)
                goto L_0x14dd
            L_0x10f1:
                java.lang.Object r0 = r2.obj
                if (r0 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.Object r4 = r2.obj
                java.lang.String r4 = (java.lang.String) r4
                java.lang.String unused = r0.mTargetRoamBSSID = r4
                goto L_0x14dd
            L_0x1100:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.what
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                java.lang.Object r6 = r2.obj
                java.util.List r6 = (java.util.List) r6
                java.util.List r5 = r5.getWifiConfigsForPasspointProfiles(r6)
                r0.replyToMessage((android.os.Message) r2, (int) r4, (java.lang.Object) r5)
                goto L_0x14dd
            L_0x1117:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.what
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                java.lang.Object r6 = r2.obj
                java.util.List r6 = (java.util.List) r6
                java.util.Map r5 = r5.getMatchingPasspointConfigsForOsuProviders(r6)
                r0.replyToMessage((android.os.Message) r2, (int) r4, (java.lang.Object) r5)
                goto L_0x14dd
            L_0x112e:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.what
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                java.lang.Object r6 = r2.obj
                java.util.List r6 = (java.util.List) r6
                java.util.Map r5 = r5.getMatchingOsuProviders(r6)
                r0.replyToMessage((android.os.Message) r2, (int) r4, (java.lang.Object) r5)
                goto L_0x14dd
            L_0x1145:
                java.lang.Object r0 = r2.obj
                java.lang.String r0 = (java.lang.String) r0
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r4 = r4.mPasspointManager
                boolean r4 = r4.removeProvider(r0)
                if (r4 == 0) goto L_0x11c8
                r4 = 0
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r5.mTargetNetworkId
                boolean r5 = r5.isProviderOwnedNetwork(r6, r0)
                if (r5 != 0) goto L_0x116e
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r5.mLastNetworkId
                boolean r5 = r5.isProviderOwnedNetwork(r6, r0)
                if (r5 == 0) goto L_0x1176
            L_0x116e:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = "Disconnect from current network since its provider is removed"
                r5.logd(r6)
                r4 = 1
            L_0x1176:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                java.util.List r5 = r5.getConfiguredNetworks()
                java.util.Iterator r6 = r5.iterator()
            L_0x1184:
                boolean r7 = r6.hasNext()
                if (r7 == 0) goto L_0x11ae
                java.lang.Object r7 = r6.next()
                android.net.wifi.WifiConfiguration r7 = (android.net.wifi.WifiConfiguration) r7
                boolean r8 = r7.isPasspoint()
                if (r8 != 0) goto L_0x1197
                goto L_0x1184
            L_0x1197:
                java.lang.String r8 = r7.FQDN
                boolean r8 = r0.equals(r8)
                if (r8 == 0) goto L_0x11ad
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                int r8 = r7.networkId
                int r11 = r7.creatorUid
                r6.removeNetwork(r8, r11)
                goto L_0x11ae
            L_0x11ad:
                goto L_0x1184
            L_0x11ae:
                if (r4 == 0) goto L_0x11b5
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r6.disconnectCommand(r9, r10)
            L_0x11b5:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                r6.removePasspointConfiguredNetwork(r0)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r2.what
                r8 = 1
                r6.replyToMessage((android.os.Message) r2, (int) r7, (int) r8)
                goto L_0x14dd
            L_0x11c8:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r2.what
                r6 = -1
                r4.replyToMessage((android.os.Message) r2, (int) r5, (int) r6)
                goto L_0x14dd
            L_0x11d2:
                java.lang.Object r0 = r2.obj
                android.os.Bundle r0 = (android.os.Bundle) r0
                java.lang.String r4 = "PasspointConfiguration"
                android.os.Parcelable r4 = r0.getParcelable(r4)
                android.net.wifi.hotspot2.PasspointConfiguration r4 = (android.net.wifi.hotspot2.PasspointConfiguration) r4
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r5 = r5.mPasspointManager
                java.lang.String r6 = "uid"
                int r6 = r0.getInt(r6)
                java.lang.String r7 = "PackageName"
                java.lang.String r7 = r0.getString(r7)
                boolean r5 = r5.addOrUpdateProvider(r4, r6, r7)
                if (r5 == 0) goto L_0x122e
                android.net.wifi.hotspot2.pps.HomeSp r5 = r4.getHomeSp()
                java.lang.String r5 = r5.getFqdn()
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r6.mTargetNetworkId
                boolean r6 = r6.isProviderOwnedNetwork(r7, r5)
                if (r6 != 0) goto L_0x1216
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r6.mLastNetworkId
                boolean r6 = r6.isProviderOwnedNetwork(r7, r5)
                if (r6 == 0) goto L_0x1224
            L_0x1216:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = "Disconnect from current network since its provider is updated"
                r6.logd(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r7 = 21
                r6.disconnectCommand(r9, r7)
            L_0x1224:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r7 = r2.what
                r8 = 1
                r6.replyToMessage((android.os.Message) r2, (int) r7, (int) r8)
                goto L_0x14dd
            L_0x122e:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r2.what
                r7 = -1
                r5.replyToMessage((android.os.Message) r2, (int) r6, (int) r7)
                goto L_0x14dd
            L_0x1238:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.what
                r0.replyToMessage((android.os.Message) r2, (int) r4, (int) r9)
                goto L_0x14dd
            L_0x1241:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r0 = r0.mPasspointManager
                java.lang.Object r4 = r2.obj
                android.os.Bundle r4 = (android.os.Bundle) r4
                java.lang.String r5 = "BSSID"
                long r4 = r4.getLong(r5)
                java.lang.Object r6 = r2.obj
                android.os.Bundle r6 = (android.os.Bundle) r6
                java.lang.String r7 = "FILENAME"
                java.lang.String r6 = r6.getString(r7)
                r0.queryPasspointIcon(r4, r6)
                goto L_0x14dd
            L_0x1260:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "resetting EAP-SIM/AKA/AKA' networks since SIM was changed"
                r0.log(r4)
                int r0 = r2.arg1
                r4 = 1
                if (r0 != r4) goto L_0x126e
                r0 = 1
                goto L_0x126f
            L_0x126e:
                r0 = r9
            L_0x126f:
                r4 = r0
                if (r4 != 0) goto L_0x1293
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.hotspot2.PasspointManager r0 = r0.mPasspointManager
                r0.removeEphemeralProviders()
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                r0.resetSimNetworks()
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r0 = r0.mWifiNative
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = r6.mInterfaceName
                r0.simAbsent(r6)
            L_0x1293:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r0 = r0.mUnstableApController
                if (r0 == 0) goto L_0x12ae
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r0 = r0.mUnstableApController
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.telephony.TelephonyManager r6 = r6.getTelephonyManager()
                boolean r6 = com.android.server.wifi.util.TelephonyUtil.isSimCardReady(r6)
                r0.setSimCardState(r6)
            L_0x12ae:
                if (r4 != 0) goto L_0x12df
                boolean r0 = com.android.server.wifi.ClientModeImpl.CSC_WIFI_SUPPORT_VZW_EAP_AKA
                if (r0 == 0) goto L_0x12df
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r0 = r0.getCurrentWifiConfiguration()
                if (r0 == 0) goto L_0x12df
                java.lang.String r6 = r0.SSID
                boolean r6 = android.text.TextUtils.isEmpty(r6)
                if (r6 != 0) goto L_0x12df
                boolean r6 = r0.semIsVendorSpecificSsid
                if (r6 == 0) goto L_0x12df
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.content.Context r6 = r6.mContext
                r7 = 1
                java.lang.String[] r8 = new java.lang.String[r7]
                java.lang.String r7 = r0.SSID
                java.lang.String r7 = com.android.server.wifi.util.StringUtil.removeDoubleQuotes(r7)
                r8[r9] = r7
                r7 = 3
                com.samsung.android.server.wifi.SemWifiFrameworkUxUtils.showWarningDialog(r6, r7, r8)
            L_0x12df:
                int r0 = r2.arg1
                if (r0 != 0) goto L_0x12e8
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.removePasspointNetworkIfSimAbsent()
            L_0x12e8:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.updateVendorApSimState()
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x131a }
                android.telephony.TelephonyManager r0 = r0.getTelephonyManager()     // Catch:{ Exception -> 0x131a }
                int r0 = r0.getSimCardState()     // Catch:{ Exception -> 0x131a }
                r6 = 11
                if (r0 != r6) goto L_0x1301
                r0 = 64
                com.android.server.wifi.ClientModeImpl.access$9976(r0)     // Catch:{ Exception -> 0x131a }
                goto L_0x1306
            L_0x1301:
                r0 = -65
                com.android.server.wifi.ClientModeImpl.access$9972(r0)     // Catch:{ Exception -> 0x131a }
            L_0x1306:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x131a }
                android.telephony.TelephonyManager r0 = r0.getTelephonyManager()     // Catch:{ Exception -> 0x131a }
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this     // Catch:{ Exception -> 0x131a }
                android.telephony.PhoneStateListener r6 = r6.mPhoneStateListener     // Catch:{ Exception -> 0x131a }
                int r7 = com.android.server.wifi.ClientModeImpl.mPhoneStateEvent     // Catch:{ Exception -> 0x131a }
                r0.listen(r6, r7)     // Catch:{ Exception -> 0x131a }
                goto L_0x1333
            L_0x131a:
                r0 = move-exception
                java.lang.StringBuilder r6 = new java.lang.StringBuilder
                r6.<init>()
                java.lang.String r7 = "TelephonyManager.listen exception happend : "
                r6.append(r7)
                java.lang.String r7 = r0.toString()
                r6.append(r7)
                java.lang.String r6 = r6.toString()
                android.util.Log.e(r5, r6)
            L_0x1333:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.handleCellularCapabilities()
                goto L_0x14dd
            L_0x133a:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                java.lang.Object r4 = r2.obj
                java.lang.String r4 = (java.lang.String) r4
                android.net.wifi.WifiConfiguration r0 = r0.disableEphemeralNetwork(r4)
                if (r0 == 0) goto L_0x14dd
                int r4 = r0.networkId
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                if (r4 == r5) goto L_0x135e
                int r4 = r0.networkId
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r4 != r5) goto L_0x14dd
            L_0x135e:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r5 = 19
                r4.disconnectCommand(r9, r5)
                goto L_0x14dd
            L_0x1367:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r0 = r0.mWifiConfigManager
                java.lang.Object r4 = r2.obj
                android.content.pm.ApplicationInfo r4 = (android.content.pm.ApplicationInfo) r4
                java.util.Set r0 = r0.removeNetworksForApp(r4)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.mTargetNetworkId
                java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
                boolean r4 = r0.contains(r4)
                if (r4 != 0) goto L_0x1395
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r4.mLastNetworkId
                java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
                boolean r4 = r0.contains(r4)
                if (r4 == 0) goto L_0x14dd
            L_0x1395:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.disconnectCommand(r9, r10)
                goto L_0x14dd
            L_0x139c:
                java.lang.Object r0 = r2.obj
                if (r0 == 0) goto L_0x14dd
                java.lang.Object r0 = r2.obj
                java.lang.String r0 = (java.lang.String) r0
                int r4 = r2.arg1
                r5 = 1
                if (r4 != r5) goto L_0x13aa
                r9 = 1
            L_0x13aa:
                r4 = r9
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r5 = r5.mWifiNative
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r6 = r6.mInterfaceName
                r5.startTdls(r6, r0, r4)
                goto L_0x14dd
            L_0x13bc:
                int r0 = r2.arg1
                r4 = 1
                if (r0 != r4) goto L_0x13d6
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r5 = 4
                r0.setSuspendOptimizationsNative(r5, r4)
                int r0 = r2.arg2
                if (r0 != r4) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                android.os.PowerManager$WakeLock r0 = r0.mSuspendWakeLock
                r0.release()
                goto L_0x14dd
            L_0x13d6:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r4 = 4
                r0.setSuspendOptimizationsNative(r4, r9)
                goto L_0x14dd
            L_0x13de:
                int r0 = r2.arg1
                r4 = 1
                if (r0 != r4) goto L_0x13ea
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.setSuspendOptimizationsNative(r15, r9)
                goto L_0x14dd
            L_0x13ea:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.setSuspendOptimizationsNative(r15, r4)
                goto L_0x14dd
            L_0x13f1:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.Clock r4 = r0.mClock
                long r4 = r4.getWallClockMillis()
                long unused = r0.mLastConnectAttemptTimestamp = r4
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r0 = r0.mWifiNative
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = r4.mInterfaceName
                r0.reassociate(r4)
                goto L_0x14dd
            L_0x140f:
                java.lang.Object r0 = r2.obj
                android.os.WorkSource r0 = (android.os.WorkSource) r0
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r4 = r4.mWifiConnectivityManager
                r4.forceConnectivityScan(r0)
                goto L_0x14dd
            L_0x141e:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiLinkLayerStats r0 = r0.getWifiLinkLayerStats()
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r2.what
                r4.replyToMessage((android.os.Message) r2, (int) r5, (java.lang.Object) r0)
                goto L_0x14dd
            L_0x142d:
                r7 = -1
                int r0 = r2.arg2
                r4 = 1
                if (r0 != r4) goto L_0x1435
                r0 = 1
                goto L_0x1436
            L_0x1435:
                r0 = r9
            L_0x1436:
                int r4 = r2.arg1
                if (r0 == 0) goto L_0x1443
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r6 = r2.sendingUid
                boolean r5 = r5.connectToUserSelectNetwork(r4, r6, r9)
                goto L_0x144f
            L_0x1443:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r5 = r5.mWifiConfigManager
                int r6 = r2.sendingUid
                boolean r5 = r5.enableNetwork(r4, r9, r6)
            L_0x144f:
                if (r5 != 0) goto L_0x1456
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int unused = r6.mMessageHandlingStatus = r14
            L_0x1456:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int r8 = r2.what
                if (r5 == 0) goto L_0x145d
                r7 = 1
            L_0x145d:
                r6.replyToMessage((android.os.Message) r2, (int) r8, (int) r7)
                goto L_0x14dd
            L_0x1462:
                int r0 = r2.arg1
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r4 = r4.mWifiConfigManager
                android.net.wifi.WifiConfiguration r4 = r4.getConfiguredNetwork((int) r0)
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                boolean r5 = r5.deleteNetworkConfigAndSendReply(r2, r9)
                if (r5 != 0) goto L_0x147c
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int unused = r5.mMessageHandlingStatus = r14
                goto L_0x14dd
            L_0x147c:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiGeofenceManager r5 = r5.mWifiGeofenceManager
                boolean r5 = r5.isSupported()
                if (r5 == 0) goto L_0x1491
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiGeofenceManager r5 = r5.mWifiGeofenceManager
                r5.removeNetwork(r4)
            L_0x1491:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mTargetNetworkId
                if (r0 == r5) goto L_0x14a1
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                if (r0 != r5) goto L_0x14dd
            L_0x14a1:
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                r5.disconnectCommand(r9, r10)
                goto L_0x14dd
            L_0x14a7:
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r2.arg1
                if (r4 == 0) goto L_0x14ae
                r9 = 1
            L_0x14ae:
                boolean unused = r0.mBluetoothConnectionActive = r9
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r0 = r0.mWifiNative
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = r4.mInterfaceName
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                boolean r5 = r5.mBluetoothConnectionActive
                r0.setBluetoothCoexistenceScanMode(r4, r5)
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r0 = r0.mWifiConnectivityManager
                if (r0 == 0) goto L_0x14dd
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConnectivityManager r0 = r0.mWifiConnectivityManager
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                boolean r4 = r4.mBluetoothConnectionActive
                r0.setBluetoothConnected(r4)
            L_0x14dd:
                r4 = 1
                if (r3 != r4) goto L_0x14e5
                com.android.server.wifi.ClientModeImpl r0 = com.android.server.wifi.ClientModeImpl.this
                r0.logStateAndMessage(r2, r1)
            L_0x14e5:
                return r3
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ClientModeImpl.ConnectModeState.processMessage(android.os.Message):boolean");
        }
    }

    /* access modifiers changed from: private */
    public boolean isRoamNetwork() {
        int configuredSecurity;
        if (this.mIsRoamNetwork) {
            return true;
        }
        List<ScanResult> scanResults = new ArrayList<>();
        scanResults.addAll(this.mScanRequestProxy.getScanResults());
        WifiConfiguration currConfig = this.mWifiConfigManager.getConfiguredNetwork(this.mWifiInfo.getNetworkId());
        if (currConfig != null) {
            String configSsid = currConfig.SSID;
            if (currConfig.allowedKeyManagement.get(1)) {
                configuredSecurity = 2;
            } else if (currConfig.allowedKeyManagement.get(8)) {
                configuredSecurity = 4;
            } else if (currConfig.allowedKeyManagement.get(2) || currConfig.allowedKeyManagement.get(3)) {
                configuredSecurity = 3;
            } else {
                configuredSecurity = currConfig.wepKeys[0] != null ? 1 : 0;
            }
            for (ScanResult scanResult : scanResults) {
                int scanedSecurity = 0;
                if (scanResult.capabilities.contains("WEP")) {
                    scanedSecurity = 1;
                } else if (scanResult.capabilities.contains("SAE")) {
                    scanedSecurity = 4;
                } else if (scanResult.capabilities.contains("PSK")) {
                    scanedSecurity = 2;
                } else if (scanResult.capabilities.contains("EAP")) {
                    scanedSecurity = 3;
                }
                if (scanResult.SSID != null && configSsid != null && configSsid.length() > 2 && scanResult.SSID.equals(configSsid.substring(1, configSsid.length() - 1)) && configuredSecurity == scanedSecurity && scanResult.BSSID != null && !scanResult.BSSID.equals(this.mWifiInfo.getBSSID())) {
                    return true;
                }
            }
        }
        return false;
    }

    private WifiNetworkAgentSpecifier createNetworkAgentSpecifier(WifiConfiguration currentWifiConfiguration, String currentBssid, int specificRequestUid, String specificRequestPackageName) {
        currentWifiConfiguration.BSSID = currentBssid;
        return new WifiNetworkAgentSpecifier(currentWifiConfiguration, specificRequestUid, specificRequestPackageName);
    }

    /* access modifiers changed from: private */
    public NetworkCapabilities getCapabilities(WifiConfiguration currentWifiConfiguration) {
        NetworkCapabilities result = new NetworkCapabilities(this.mNetworkCapabilitiesFilter);
        result.setNetworkSpecifier((NetworkSpecifier) null);
        if (currentWifiConfiguration == null) {
            return result;
        }
        if (!this.mWifiInfo.isTrusted()) {
            result.removeCapability(14);
        } else {
            result.addCapability(14);
        }
        if (!WifiConfiguration.isMetered(currentWifiConfiguration, this.mWifiInfo)) {
            result.addCapability(11);
        } else {
            result.removeCapability(11);
        }
        if (this.mWifiInfo.getRssi() != -127) {
            result.setSignalStrength(this.mWifiInfo.getRssi());
        } else {
            result.setSignalStrength(Integer.MIN_VALUE);
        }
        if (currentWifiConfiguration.osu) {
            result.removeCapability(12);
        }
        if (!this.mWifiInfo.getSSID().equals(MobileWipsWifiSsid.NONE)) {
            result.setSSID(this.mWifiInfo.getSSID());
        } else {
            result.setSSID((String) null);
        }
        Pair<Integer, String> specificRequestUidAndPackageName = this.mNetworkFactory.getSpecificNetworkRequestUidAndPackageName(currentWifiConfiguration);
        if (((Integer) specificRequestUidAndPackageName.first).intValue() != -1) {
            result.removeCapability(12);
        }
        result.setNetworkSpecifier(createNetworkAgentSpecifier(currentWifiConfiguration, getCurrentBSSID(), ((Integer) specificRequestUidAndPackageName.first).intValue(), (String) specificRequestUidAndPackageName.second));
        return result;
    }

    public void updateCapabilities() {
        updateCapabilities(getCurrentWifiConfiguration());
    }

    private void updateCapabilities(WifiConfiguration currentWifiConfiguration) {
        WifiNetworkAgent wifiNetworkAgent = this.mNetworkAgent;
        if (wifiNetworkAgent != null) {
            wifiNetworkAgent.sendNetworkCapabilities(getCapabilities(currentWifiConfiguration));
        }
    }

    /* access modifiers changed from: private */
    public boolean isProviderOwnedNetwork(int networkId, String providerFqdn) {
        WifiConfiguration config;
        if (networkId == -1 || (config = this.mWifiConfigManager.getConfiguredNetwork(networkId)) == null) {
            return false;
        }
        return TextUtils.equals(config.FQDN, providerFqdn);
    }

    /* access modifiers changed from: private */
    public void handleEapAuthFailure(int networkId, int errorCode) {
        WifiConfiguration targetedNetwork = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (targetedNetwork != null) {
            int eapMethod = targetedNetwork.enterpriseConfig.getEapMethod();
            if (eapMethod == 4 || eapMethod == 5 || eapMethod == 6) {
                if (errorCode == 16385) {
                    getTelephonyManager().createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId()).resetCarrierKeysForImsiEncryption();
                }
                if (CSC_WIFI_ERRORCODE) {
                    showEapNotificationToast(errorCode);
                }
            }
        }
    }

    private class WifiNetworkAgent extends NetworkAgent {
        private int mLastNetworkStatus = -1;
        final /* synthetic */ ClientModeImpl this$0;

        /* JADX INFO: super call moved to the top of the method (can break code semantics) */
        WifiNetworkAgent(ClientModeImpl clientModeImpl, Looper l, Context c, String tag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
            super(l, c, tag, ni, nc, lp, score, misc);
            this.this$0 = clientModeImpl;
        }

        /* access modifiers changed from: protected */
        public void unwanted() {
            if (this == this.this$0.mNetworkAgent) {
                if (this.this$0.mVerboseLoggingEnabled) {
                    log("WifiNetworkAgent -> Wifi unwanted score " + Integer.toString(this.this$0.mWifiInfo.score));
                }
                this.this$0.unwantedNetwork(0);
            }
        }

        /* access modifiers changed from: protected */
        public void networkStatus(int status, String redirectUrl) {
            if (this == this.this$0.mNetworkAgent && status != this.mLastNetworkStatus) {
                this.mLastNetworkStatus = status;
                if (status == 2) {
                    if (this.this$0.mVerboseLoggingEnabled) {
                        log("WifiNetworkAgent -> Wifi networkStatus invalid, score=" + Integer.toString(this.this$0.mWifiInfo.score));
                    }
                    this.this$0.unwantedNetwork(1);
                } else if (status == 1) {
                    if (this.this$0.mVerboseLoggingEnabled) {
                        log("WifiNetworkAgent -> Wifi networkStatus valid, score= " + Integer.toString(this.this$0.mWifiInfo.score));
                    }
                    this.this$0.mWifiMetrics.logStaEvent(14);
                    this.this$0.doNetworkStatus(status);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void saveAcceptUnvalidated(boolean accept) {
            if (this == this.this$0.mNetworkAgent) {
                this.this$0.sendMessage(ClientModeImpl.CMD_ACCEPT_UNVALIDATED, accept);
            }
        }

        /* access modifiers changed from: protected */
        public void startSocketKeepalive(Message msg) {
            this.this$0.sendMessage(ClientModeImpl.CMD_START_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        /* access modifiers changed from: protected */
        public void stopSocketKeepalive(Message msg) {
            this.this$0.sendMessage(ClientModeImpl.CMD_STOP_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        /* access modifiers changed from: protected */
        public void addKeepalivePacketFilter(Message msg) {
            this.this$0.sendMessage(ClientModeImpl.CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF, msg.arg1, msg.arg2, msg.obj);
        }

        /* access modifiers changed from: protected */
        public void removeKeepalivePacketFilter(Message msg) {
            this.this$0.sendMessage(ClientModeImpl.CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF, msg.arg1, msg.arg2, msg.obj);
        }

        /* access modifiers changed from: protected */
        public void setSignalStrengthThresholds(int[] thresholds) {
            log("Received signal strength thresholds: " + Arrays.toString(thresholds));
            if (thresholds.length == 0) {
                boolean isKnoxCustomAutoSwitchEnabled = false;
                CustomDeviceManagerProxy customDeviceManager = CustomDeviceManagerProxy.getInstance();
                if (customDeviceManager != null && customDeviceManager.getWifiAutoSwitchState()) {
                    isKnoxCustomAutoSwitchEnabled = true;
                    if (ClientModeImpl.DBG) {
                        this.this$0.logd("KnoxCustom WifiAutoSwitch: not stopping RSSI monitoring");
                    }
                }
                if (!isKnoxCustomAutoSwitchEnabled) {
                    ClientModeImpl clientModeImpl = this.this$0;
                    clientModeImpl.sendMessage(ClientModeImpl.CMD_STOP_RSSI_MONITORING_OFFLOAD, clientModeImpl.mWifiInfo.getRssi());
                    return;
                }
            }
            int[] rssiVals = Arrays.copyOf(thresholds, thresholds.length + 2);
            rssiVals[rssiVals.length - 2] = -128;
            rssiVals[rssiVals.length - 1] = 127;
            Arrays.sort(rssiVals);
            byte[] rssiRange = new byte[rssiVals.length];
            for (int i = 0; i < rssiVals.length; i++) {
                int val = rssiVals[i];
                if (val > 127 || val < -128) {
                    Log.e(ClientModeImpl.TAG, "Illegal value " + val + " for RSSI thresholds: " + Arrays.toString(rssiVals));
                    ClientModeImpl clientModeImpl2 = this.this$0;
                    clientModeImpl2.sendMessage(ClientModeImpl.CMD_STOP_RSSI_MONITORING_OFFLOAD, clientModeImpl2.mWifiInfo.getRssi());
                    return;
                }
                rssiRange[i] = (byte) val;
            }
            byte[] unused = this.this$0.mRssiRanges = rssiRange;
            ClientModeImpl clientModeImpl3 = this.this$0;
            clientModeImpl3.sendMessage(ClientModeImpl.CMD_START_RSSI_MONITORING_OFFLOAD, clientModeImpl3.mWifiInfo.getRssi());
        }

        /* access modifiers changed from: protected */
        public void preventAutomaticReconnect() {
            if (this == this.this$0.mNetworkAgent) {
                this.this$0.unwantedNetwork(2);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void unwantedNetwork(int reason) {
        sendMessage(CMD_UNWANTED_NETWORK, reason);
    }

    /* access modifiers changed from: package-private */
    public void doNetworkStatus(int status) {
        sendMessage(CMD_NETWORK_STATUS, status);
    }

    private String buildIdentity(int eapMethod, String imsi, String mccMnc) {
        String prefix;
        String mnc;
        String mcc;
        if (imsi == null || imsi.isEmpty()) {
            return "";
        }
        if (eapMethod == 4) {
            prefix = "1";
        } else if (eapMethod == 5) {
            prefix = "0";
        } else if (eapMethod != 6) {
            return "";
        } else {
            prefix = "6";
        }
        if (mccMnc == null || mccMnc.isEmpty()) {
            mcc = imsi.substring(0, 3);
            mnc = imsi.substring(3, 6);
        } else {
            mcc = mccMnc.substring(0, 3);
            mnc = mccMnc.substring(3);
            if (mnc.length() == 2) {
                mnc = "0" + mnc;
            }
        }
        return prefix + imsi + "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
    }

    class L2ConnectedState extends State {
        RssiEventHandler mRssiEventHandler = new RssiEventHandler();

        class RssiEventHandler implements WifiNative.WifiRssiEventHandler {
            RssiEventHandler() {
            }

            public void onRssiThresholdBreached(byte curRssi) {
                if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                    Log.e(ClientModeImpl.TAG, "onRssiThresholdBreach event. Cur Rssi = " + curRssi);
                }
                ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_RSSI_THRESHOLD_BREACHED, curRssi);
            }
        }

        L2ConnectedState() {
        }

        public void enter() {
            ClientModeImpl.access$16508(ClientModeImpl.this);
            if (ClientModeImpl.this.mEnableRssiPolling) {
                ClientModeImpl.this.mLinkProbeManager.resetOnNewConnection();
                ClientModeImpl clientModeImpl = ClientModeImpl.this;
                clientModeImpl.sendMessage(ClientModeImpl.CMD_RSSI_POLL, clientModeImpl.mRssiPollToken, 0);
            }
            if (ClientModeImpl.this.mNetworkAgent != null) {
                ClientModeImpl.this.loge("Have NetworkAgent when entering L2Connected");
                boolean unused = ClientModeImpl.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            }
            boolean unused2 = ClientModeImpl.this.setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTING);
            ClientModeImpl.this.handleWifiNetworkCallbacks(1);
            ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
            NetworkCapabilities nc = clientModeImpl2.getCapabilities(clientModeImpl2.getCurrentWifiConfiguration());
            synchronized (ClientModeImpl.this.mNetworkAgentLock) {
                WifiNetworkAgent unused3 = ClientModeImpl.this.mNetworkAgent = new WifiNetworkAgent(ClientModeImpl.this, ClientModeImpl.this.getHandler().getLooper(), ClientModeImpl.this.mContext, "WifiNetworkAgent", ClientModeImpl.this.mNetworkInfo, nc, ClientModeImpl.this.mLinkProperties, 60, ClientModeImpl.this.mNetworkMisc);
                ClientModeImpl.this.mWifiScoreReport.setNeteworkAgent(ClientModeImpl.this.mNetworkAgent);
            }
            ClientModeImpl.this.clearTargetBssid("L2ConnectedState");
            ClientModeImpl.this.mCountryCode.setReadyForChange(false);
            ClientModeImpl.this.mWifiMetrics.setWifiState(3);
            ClientModeImpl.this.mWifiScoreCard.noteNetworkAgentCreated(ClientModeImpl.this.mWifiInfo, ClientModeImpl.this.mNetworkAgent.netId);
            ClientModeImpl.this.isDhcpStartSent = false;
        }

        public void exit() {
            if (ClientModeImpl.this.mIpClient != null) {
                ClientModeImpl.this.mIpClient.stop();
            }
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                StringBuilder sb = new StringBuilder();
                sb.append("leaving L2ConnectedState state nid=" + Integer.toString(ClientModeImpl.this.mLastNetworkId));
                if (ClientModeImpl.this.mLastBssid != null) {
                    sb.append(" ");
                    sb.append(ClientModeImpl.this.mLastBssid);
                }
            }
            if (!(ClientModeImpl.this.mLastBssid == null && ClientModeImpl.this.mLastNetworkId == -1)) {
                ClientModeImpl.this.handleNetworkDisconnect();
            }
            ClientModeImpl.this.mCountryCode.setReadyForChange(true);
            ClientModeImpl.this.mWifiMetrics.setWifiState(2);
            ClientModeImpl.this.mWifiStateTracker.updateState(2);
            ClientModeImpl.this.mWifiInjector.getWifiLockManager().updateWifiClientConnected(false);
        }

        public boolean processMessage(Message message) {
            ScanDetailCache scanDetailCache;
            ScanResult scanResult;
            Message message2 = message;
            boolean handleStatus = true;
            switch (message2.what) {
                case ClientModeImpl.CMD_DISCONNECT /*131145*/:
                    ClientModeImpl.this.mWifiMetrics.logStaEvent(15, 2);
                    ClientModeImpl.this.notifyDisconnectInternalReason(message2.arg2);
                    if (ClientModeImpl.this.getCurrentState() == ClientModeImpl.this.mConnectedState) {
                        if (ClientModeImpl.ENBLE_WLAN_CONFIG_ANALYTICS && !ClientModeImpl.this.mSettingsStore.isWifiToggleEnabled()) {
                            ClientModeImpl.this.setAnalyticsUserDisconnectReason(WifiNative.f20xe9115267);
                        }
                        ClientModeImpl.this.mDelayDisconnect.checkAndWait(ClientModeImpl.this.mNetworkInfo);
                    }
                    ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                    ClientModeImpl clientModeImpl = ClientModeImpl.this;
                    clientModeImpl.transitionTo(clientModeImpl.mDisconnectingState);
                    break;
                case ClientModeImpl.CMD_RECONNECT /*131146*/:
                    ClientModeImpl.this.log(" Ignore CMD_RECONNECT request because wifi is already connected");
                    break;
                case ClientModeImpl.CMD_ENABLE_RSSI_POLL /*131154*/:
                    ClientModeImpl.this.cleanWifiScore();
                    boolean unused = ClientModeImpl.this.mEnableRssiPolling = message2.arg1 == 1;
                    ClientModeImpl.access$16508(ClientModeImpl.this);
                    if (ClientModeImpl.this.mEnableRssiPolling) {
                        int unused2 = ClientModeImpl.this.mLastSignalLevel = -1;
                        ClientModeImpl.this.mLinkProbeManager.resetOnScreenTurnedOn();
                        ClientModeImpl.this.fetchRssiLinkSpeedAndFrequencyNative();
                        ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
                        clientModeImpl2.sendMessageDelayed(clientModeImpl2.obtainMessage(ClientModeImpl.CMD_RSSI_POLL, clientModeImpl2.mRssiPollToken, 0), (long) ClientModeImpl.this.mPollRssiIntervalMsecs);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_RSSI_POLL /*131155*/:
                    if (message2.arg1 == ClientModeImpl.this.mRssiPollToken) {
                        WifiLinkLayerStats stats = updateLinkLayerStatsRssiAndScoreReportInternal();
                        ClientModeImpl.this.mWifiMetrics.updateWifiUsabilityStatsEntries(ClientModeImpl.this.mWifiInfo, stats);
                        if (ClientModeImpl.this.mWifiScoreReport.shouldCheckIpLayer()) {
                            if (ClientModeImpl.this.mIpClient != null) {
                                ClientModeImpl.this.mIpClient.confirmConfiguration();
                            }
                            ClientModeImpl.this.mWifiScoreReport.noteIpCheck();
                        }
                        int statusDataStall = ClientModeImpl.this.mWifiDataStall.checkForDataStall(ClientModeImpl.this.mLastLinkLayerStats, stats);
                        if (statusDataStall != 0) {
                            ClientModeImpl.this.mWifiMetrics.addToWifiUsabilityStatsList(2, ClientModeImpl.convertToUsabilityStatsTriggerType(statusDataStall), -1);
                        }
                        ClientModeImpl.this.mWifiMetrics.incrementWifiLinkLayerUsageStats(stats);
                        WifiLinkLayerStats unused3 = ClientModeImpl.this.mLastLinkLayerStats = stats;
                        ClientModeImpl.this.mWifiScoreCard.noteSignalPoll(ClientModeImpl.this.mWifiInfo);
                        ClientModeImpl.this.mLinkProbeManager.updateConnectionStats(ClientModeImpl.this.mWifiInfo, ClientModeImpl.this.mInterfaceName);
                        ClientModeImpl clientModeImpl3 = ClientModeImpl.this;
                        clientModeImpl3.sendMessageDelayed(clientModeImpl3.obtainMessage(ClientModeImpl.CMD_RSSI_POLL, clientModeImpl3.mRssiPollToken, 0), (long) ClientModeImpl.this.mPollRssiIntervalMsecs);
                        if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                            ClientModeImpl clientModeImpl4 = ClientModeImpl.this;
                            clientModeImpl4.sendRssiChangeBroadcast(clientModeImpl4.mWifiInfo.getRssi());
                        }
                        ClientModeImpl.this.mWifiTrafficPoller.notifyOnDataActivity(ClientModeImpl.this.mWifiInfo.txSuccess, ClientModeImpl.this.mWifiInfo.rxSuccess);
                        ClientModeImpl clientModeImpl5 = ClientModeImpl.this;
                        clientModeImpl5.knoxAutoSwitchPolicy(clientModeImpl5.mWifiInfo.getRssi());
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_ONESHOT_RSSI_POLL /*131156*/:
                    if (!ClientModeImpl.this.mEnableRssiPolling) {
                        updateLinkLayerStatsRssiAndScoreReportInternal();
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_RESET_SIM_NETWORKS /*131173*/:
                    if (message2.arg1 == 0 && ClientModeImpl.this.mLastNetworkId != -1 && TelephonyUtil.isSimConfig(ClientModeImpl.this.mWifiConfigManager.getConfiguredNetwork(ClientModeImpl.this.mLastNetworkId))) {
                        ClientModeImpl.this.mWifiMetrics.logStaEvent(15, 6);
                        ClientModeImpl.this.notifyDisconnectInternalReason(4);
                        ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                        ClientModeImpl clientModeImpl6 = ClientModeImpl.this;
                        clientModeImpl6.transitionTo(clientModeImpl6.mDisconnectingState);
                    }
                    handleStatus = false;
                    break;
                case ClientModeImpl.CMD_IP_CONFIGURATION_SUCCESSFUL /*131210*/:
                    if (ClientModeImpl.this.getCurrentWifiConfiguration() != null) {
                        ClientModeImpl.this.handleSuccessfulIpConfiguration();
                        if (ClientModeImpl.this.isRoaming()) {
                            ClientModeImpl.this.setRoamTriggered(false);
                        }
                        if (ClientModeImpl.this.getCurrentState() == ClientModeImpl.this.mConnectedState) {
                            if (ClientModeImpl.DBG) {
                                ClientModeImpl.this.log("DHCP renew post action!!! - Don't need to make state transition");
                                break;
                            }
                        } else {
                            ClientModeImpl.this.report(ReportIdKey.ID_DHCP_FAIL, ReportUtil.getReportDataForDhcpResult(1));
                            ClientModeImpl.this.sendConnectedState();
                            ClientModeImpl clientModeImpl7 = ClientModeImpl.this;
                            clientModeImpl7.transitionTo(clientModeImpl7.mConnectedState);
                            break;
                        }
                    } else {
                        ClientModeImpl.this.reportConnectionAttemptEnd(6, 1, 0);
                        ClientModeImpl.this.notifyDisconnectInternalReason(22);
                        ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                        ClientModeImpl clientModeImpl8 = ClientModeImpl.this;
                        clientModeImpl8.transitionTo(clientModeImpl8.mDisconnectingState);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_IP_CONFIGURATION_LOST /*131211*/:
                    if (ClientModeImpl.ENBLE_WLAN_CONFIG_ANALYTICS) {
                        ClientModeImpl.this.setAnalyticsUserDisconnectReason(WifiNative.ANALYTICS_DISCONNECT_REASON_DHCP_FAIL_UNSPECIFIED);
                    }
                    ClientModeImpl.this.getWifiLinkLayerStats();
                    ClientModeImpl.this.handleIpConfigurationLost();
                    ClientModeImpl.this.report(ReportIdKey.ID_DHCP_FAIL, ReportUtil.getReportDataForDhcpResult(2));
                    ClientModeImpl.this.reportConnectionAttemptEnd(10, 1, 0);
                    ClientModeImpl.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(ClientModeImpl.this.getTargetSsid(), ClientModeImpl.this.mTargetRoamBSSID, 3);
                    ClientModeImpl.this.notifyDisconnectInternalReason(1);
                    ClientModeImpl clientModeImpl9 = ClientModeImpl.this;
                    clientModeImpl9.transitionTo(clientModeImpl9.mDisconnectingState);
                    break;
                case ClientModeImpl.CMD_ASSOCIATED_BSSID /*131219*/:
                    if (((String) message2.obj) != null) {
                        if (ClientModeImpl.this.getNetworkDetailedState() == NetworkInfo.DetailedState.CONNECTED || ClientModeImpl.this.getNetworkDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
                            ClientModeImpl.this.log("NOT update Last BSSID");
                        } else {
                            String unused4 = ClientModeImpl.this.mLastBssid = (String) message2.obj;
                            if (ClientModeImpl.this.mLastBssid != null && (ClientModeImpl.this.mWifiInfo.getBSSID() == null || !ClientModeImpl.this.mLastBssid.equals(ClientModeImpl.this.mWifiInfo.getBSSID()))) {
                                ClientModeImpl.this.mWifiInfo.setBSSID(ClientModeImpl.this.mLastBssid);
                                WifiConfiguration config = ClientModeImpl.this.getCurrentWifiConfiguration();
                                if (!(config == null || (scanDetailCache = ClientModeImpl.this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId)) == null || (scanResult = scanDetailCache.getScanResult(ClientModeImpl.this.mLastBssid)) == null)) {
                                    ClientModeImpl.this.mWifiInfo.setFrequency(scanResult.frequency);
                                    ClientModeImpl.this.mWifiInfo.setWifiMode(scanResult.wifiMode);
                                }
                                ClientModeImpl clientModeImpl10 = ClientModeImpl.this;
                                clientModeImpl10.sendNetworkStateChangeBroadcast(clientModeImpl10.mLastBssid);
                            }
                        }
                        ClientModeImpl.this.notifyMobilewipsRoamEvent("start");
                        break;
                    } else {
                        ClientModeImpl.this.logw("Associated command w/o BSSID");
                        break;
                    }
                case ClientModeImpl.CMD_IP_REACHABILITY_LOST /*131221*/:
                    if (ClientModeImpl.this.mVerboseLoggingEnabled && message2.obj != null) {
                        ClientModeImpl.this.log((String) message2.obj);
                    }
                    ClientModeImpl.this.mWifiDiagnostics.triggerBugReportDataCapture(9);
                    ClientModeImpl.this.mWifiMetrics.logWifiIsUnusableEvent(5);
                    ClientModeImpl.this.mWifiMetrics.addToWifiUsabilityStatsList(2, 5, -1);
                    if (!ClientModeImpl.this.mIpReachabilityDisconnectEnabled) {
                        ClientModeImpl.this.logd("CMD_IP_REACHABILITY_LOST but disconnect disabled -- ignore");
                        ClientModeImpl.this.handleWifiNetworkCallbacks(10);
                        break;
                    } else {
                        ClientModeImpl.this.notifyDisconnectInternalReason(1);
                        ClientModeImpl.this.handleIpReachabilityLost();
                        ClientModeImpl clientModeImpl11 = ClientModeImpl.this;
                        clientModeImpl11.transitionTo(clientModeImpl11.mDisconnectingState);
                        break;
                    }
                case ClientModeImpl.CMD_START_IP_PACKET_OFFLOAD /*131232*/:
                    int slot = message2.arg1;
                    int result = ClientModeImpl.this.startWifiIPPacketOffload(slot, (KeepalivePacketData) message2.obj, message2.arg2);
                    if (ClientModeImpl.this.mNetworkAgent != null) {
                        ClientModeImpl.this.mNetworkAgent.onSocketKeepaliveEvent(slot, result);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
                case ClientModeImpl.CMD_RSSI_THRESHOLD_BREACHED /*131236*/:
                    ClientModeImpl.this.processRssiThreshold((byte) message2.arg1, message2.what, this.mRssiEventHandler);
                    break;
                case ClientModeImpl.CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
                    int unused5 = ClientModeImpl.this.stopRssiMonitoringOffload();
                    break;
                case ClientModeImpl.CMD_IPV4_PROVISIONING_SUCCESS /*131272*/:
                    ClientModeImpl.this.handleIPv4Success((DhcpResults) message2.obj);
                    ClientModeImpl clientModeImpl12 = ClientModeImpl.this;
                    clientModeImpl12.sendNetworkStateChangeBroadcast(clientModeImpl12.mLastBssid);
                    if ("".equals(ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && MobileWipsFrameworkService.getInstance() != null) {
                        MobileWipsFrameworkService.getInstance().sendEmptyMessage(19);
                    }
                    if (ClientModeImpl.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() && ClientModeImpl.this.mWifiNative.CheckWifiSoftApIpReset()) {
                        Log.d(ClientModeImpl.TAG, "IP Subnet of MobileAp needs to be modified. So Reset Mobile Ap");
                        Intent resetIntent = new Intent("com.samsung.android.intent.action.WIFIAP_RESET");
                        if (ClientModeImpl.this.mContext != null) {
                            ClientModeImpl.this.mContext.sendBroadcast(resetIntent);
                            resetIntent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver");
                            ClientModeImpl.this.mContext.sendBroadcast(resetIntent);
                            break;
                        }
                    }
                    break;
                case ClientModeImpl.CMD_IPV4_PROVISIONING_FAILURE /*131273*/:
                    ClientModeImpl.this.handleIPv4Failure();
                    ClientModeImpl.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(ClientModeImpl.this.getTargetSsid(), ClientModeImpl.this.mTargetRoamBSSID, 3);
                    break;
                case ClientModeImpl.CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF /*131281*/:
                    if (ClientModeImpl.this.mIpClient != null) {
                        int slot2 = message2.arg1;
                        if (!(message2.obj instanceof NattKeepalivePacketData)) {
                            if (message2.obj instanceof TcpKeepalivePacketData) {
                                ClientModeImpl.this.mIpClient.addKeepalivePacketFilter(slot2, (TcpKeepalivePacketData) message2.obj);
                                break;
                            }
                        } else {
                            ClientModeImpl.this.mIpClient.addKeepalivePacketFilter(slot2, (NattKeepalivePacketData) message2.obj);
                            break;
                        }
                    }
                    break;
                case ClientModeImpl.CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF /*131282*/:
                    if (ClientModeImpl.this.mIpClient != null) {
                        ClientModeImpl.this.mIpClient.removeKeepalivePacketFilter(message2.arg1);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_REPLACE_PUBLIC_DNS /*131286*/:
                    if (ClientModeImpl.this.mLinkProperties != null) {
                        String publicDnsIp = ((Bundle) message2.obj).getString("publicDnsServer");
                        ArrayList<InetAddress> dnsList = new ArrayList<>(ClientModeImpl.this.mLinkProperties.getDnsServers());
                        dnsList.add(NetworkUtils.numericToInetAddress(publicDnsIp));
                        ClientModeImpl.this.mLinkProperties.setDnsServers(dnsList);
                        ClientModeImpl clientModeImpl13 = ClientModeImpl.this;
                        clientModeImpl13.updateLinkProperties(clientModeImpl13.mLinkProperties);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_PRE_DHCP_ACTION /*131327*/:
                    ClientModeImpl.this.handlePreDhcpSetup();
                    break;
                case ClientModeImpl.CMD_PRE_DHCP_ACTION_COMPLETE /*131328*/:
                    if (ClientModeImpl.this.mIpClient != null) {
                        ClientModeImpl.this.mIpClient.completedPreDhcpAction();
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_POST_DHCP_ACTION /*131329*/:
                    ClientModeImpl.this.handlePostDhcpSetup();
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                    if (message2.arg1 == 1) {
                        ClientModeImpl.this.mWifiMetrics.logStaEvent(15, 5);
                        ClientModeImpl.this.notifyDisconnectInternalReason(16);
                        ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                        boolean unused6 = ClientModeImpl.this.mTemporarilyDisconnectWifi = true;
                        ClientModeImpl clientModeImpl14 = ClientModeImpl.this;
                        clientModeImpl14.transitionTo(clientModeImpl14.mDisconnectingState);
                        break;
                    }
                    break;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (ClientModeImpl.DBG != 0) {
                        ClientModeImpl.this.log("dongle roaming established");
                    }
                    ClientModeImpl.this.mWifiInfo.setBSSID((String) message2.obj);
                    int unused7 = ClientModeImpl.this.mLastNetworkId = message2.arg1;
                    ClientModeImpl clientModeImpl15 = ClientModeImpl.this;
                    int unused8 = clientModeImpl15.mLastConnectedNetworkId = clientModeImpl15.mLastNetworkId;
                    ClientModeImpl.this.mWifiInfo.setNetworkId(ClientModeImpl.this.mLastNetworkId);
                    ClientModeImpl.this.mWifiInfo.setMacAddress(ClientModeImpl.this.mWifiNative.getMacAddress(ClientModeImpl.this.mInterfaceName));
                    if (!ClientModeImpl.this.mLastBssid.equals(message2.obj)) {
                        ClientModeImpl.this.setRoamTriggered(true);
                        String unused9 = ClientModeImpl.this.mLastBssid = (String) message2.obj;
                        ScanDetailCache scanDetailCache2 = ClientModeImpl.this.mWifiConfigManager.getScanDetailCacheForNetwork(ClientModeImpl.this.mLastNetworkId);
                        ScanResult scanResult2 = null;
                        if (scanDetailCache2 != null) {
                            scanResult2 = scanDetailCache2.getScanResult(ClientModeImpl.this.mLastBssid);
                        }
                        if (scanResult2 == null) {
                            Log.d(ClientModeImpl.TAG, "roamed scan result is not in cache, find it from last native scan results");
                            Iterator<ScanDetail> it = ClientModeImpl.this.mWifiNative.getScanResults(ClientModeImpl.this.mInterfaceName).iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    ScanDetail scanDetail = it.next();
                                    if (ClientModeImpl.this.mLastBssid.equals(scanDetail.getBSSIDString())) {
                                        Log.d(ClientModeImpl.TAG, "found it! update the cache");
                                        scanResult2 = scanDetail.getScanResult();
                                        ClientModeImpl.this.mWifiConfigManager.updateScanDetailForNetwork(ClientModeImpl.this.mLastNetworkId, scanDetail);
                                    }
                                }
                            }
                        }
                        if (scanResult2 != null) {
                            ClientModeImpl.this.mWifiInfo.setFrequency(scanResult2.frequency);
                            ClientModeImpl.this.updateWifiInfoForVendors(scanResult2);
                            ClientModeImpl.this.mWifiInfo.setWifiMode(scanResult2.wifiMode);
                            if (ClientModeImpl.CSC_SUPPORT_5G_ANT_SHARE) {
                                ClientModeImpl clientModeImpl16 = ClientModeImpl.this;
                                clientModeImpl16.sendIpcMessageToRilForLteu(4, true, clientModeImpl16.mWifiInfo.is5GHz(), false);
                            }
                        } else {
                            Log.d(ClientModeImpl.TAG, "can't update vendor infos, bssid: " + ClientModeImpl.this.mLastBssid);
                        }
                        ClientModeImpl clientModeImpl17 = ClientModeImpl.this;
                        clientModeImpl17.sendNetworkStateChangeBroadcast(clientModeImpl17.mLastBssid);
                        int tmpDhcpRenewAfterRoamingMode = ClientModeImpl.this.getRoamDhcpPolicy();
                        WifiConfiguration currentConfig = ClientModeImpl.this.getCurrentWifiConfiguration();
                        boolean isUsingStaticIp = false;
                        if (currentConfig != null) {
                            isUsingStaticIp = currentConfig.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
                        }
                        if (isUsingStaticIp) {
                            ClientModeImpl.this.log("Static ip - skip renew");
                            tmpDhcpRenewAfterRoamingMode = -1;
                        }
                        if (ClientModeImpl.this.mRoamDhcpPolicyByB2bConfig == 2 || tmpDhcpRenewAfterRoamingMode == 2 || tmpDhcpRenewAfterRoamingMode == -1) {
                            ClientModeImpl clientModeImpl18 = ClientModeImpl.this;
                            clientModeImpl18.log("Skip Dhcp - mRoamDhcpPolicyByB2bConfig:" + ClientModeImpl.this.mRoamDhcpPolicyByB2bConfig + " ,tmpDhcpRenewAfterRoamingMode : " + tmpDhcpRenewAfterRoamingMode);
                            if ("".equals(ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && MobileWipsFrameworkService.getInstance() != null) {
                                MobileWipsFrameworkService.getInstance().sendEmptyMessage(20);
                            }
                            ClientModeImpl.this.setRoamTriggered(false);
                        } else if (ClientModeImpl.this.checkIfForceRestartDhcp()) {
                            ClientModeImpl.this.restartDhcp(currentConfig);
                        } else {
                            ClientModeImpl.this.CheckIfDefaultGatewaySame();
                            if ("".equals(ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && MobileWipsFrameworkService.getInstance() != null) {
                                MobileWipsFrameworkService.getInstance().sendEmptyMessage(20);
                            }
                        }
                        ClientModeImpl.this.handleCellularCapabilities(true);
                        if (!(WifiRoamingAssistant.getInstance() == null || currentConfig == null)) {
                            WifiRoamingAssistant.getInstance().updateRcl(currentConfig.configKey(), ClientModeImpl.this.mWifiInfo.getFrequency(), true);
                            break;
                        }
                    }
                    break;
                case 151553:
                    if (ClientModeImpl.this.mWifiInfo.getNetworkId() != message2.arg1) {
                        handleStatus = false;
                        break;
                    } else {
                        ClientModeImpl.this.replyToMessage(message2, 151555);
                        break;
                    }
                case 151572:
                    RssiPacketCountInfo info = new RssiPacketCountInfo();
                    ClientModeImpl.this.fetchRssiLinkSpeedAndFrequencyNative();
                    info.rssi = ClientModeImpl.this.mWifiInfo.getRssi();
                    ClientModeImpl clientModeImpl19 = ClientModeImpl.this;
                    clientModeImpl19.knoxAutoSwitchPolicy(clientModeImpl19.mWifiInfo.getRssi());
                    WifiNative.TxPacketCounters counters = ClientModeImpl.this.mWifiNative.getTxPacketCounters(ClientModeImpl.this.mInterfaceName);
                    if (counters == null) {
                        ClientModeImpl.this.replyToMessage(message2, 151574, 0);
                        break;
                    } else {
                        info.txgood = counters.txSucceeded;
                        info.txbad = counters.txFailed;
                        ClientModeImpl.this.replyToMessage(message2, 151573, (Object) info);
                        break;
                    }
                default:
                    handleStatus = false;
                    break;
            }
            if (handleStatus) {
                ClientModeImpl.this.logStateAndMessage(message2, this);
            }
            return handleStatus;
        }

        private WifiLinkLayerStats updateLinkLayerStatsRssiAndScoreReportInternal() {
            WifiLinkLayerStats stats = ClientModeImpl.this.getWifiLinkLayerStats();
            ClientModeImpl.this.fetchRssiLinkSpeedAndFrequencyNative();
            ClientModeImpl.this.mWifiScoreReport.calculateAndReportScore(ClientModeImpl.this.mWifiInfo, ClientModeImpl.this.mNetworkAgent, ClientModeImpl.this.mWifiMetrics);
            return stats;
        }
    }

    public void updateLinkLayerStatsRssiAndScoreReport() {
        sendMessage(CMD_ONESHOT_RSSI_POLL);
    }

    /* access modifiers changed from: private */
    public static int convertToUsabilityStatsTriggerType(int unusableEventTriggerType) {
        if (unusableEventTriggerType == 1) {
            return 1;
        }
        if (unusableEventTriggerType == 2) {
            return 2;
        }
        if (unusableEventTriggerType == 3) {
            return 3;
        }
        if (unusableEventTriggerType == 4) {
            return 4;
        }
        if (unusableEventTriggerType == 5) {
            return 5;
        }
        Log.e(TAG, "Unknown WifiIsUnusableEvent: " + unusableEventTriggerType);
        return 0;
    }

    class ObtainingIpState extends State {
        ObtainingIpState() {
        }

        public void enter() {
            StaticIpConfiguration staticIpConfig;
            WifiConfiguration currentConfig = ClientModeImpl.this.getCurrentWifiConfiguration();
            boolean isUsingStaticIp = false;
            if (currentConfig != null) {
                isUsingStaticIp = currentConfig.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
            }
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                String key = currentConfig.configKey();
                ClientModeImpl clientModeImpl = ClientModeImpl.this;
                clientModeImpl.log("enter ObtainingIpState netId=" + Integer.toString(ClientModeImpl.this.mLastNetworkId) + " " + key + "  roam=" + ClientModeImpl.this.mIsAutoRoaming + " static=" + isUsingStaticIp);
            }
            boolean unused = ClientModeImpl.this.setNetworkDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR);
            ClientModeImpl.this.clearTargetBssid("ObtainingIpAddress");
            ClientModeImpl.this.stopIpClient();
            ClientModeImpl.this.setTcpBufferAndProxySettingsForIpManager();
            if (!isUsingStaticIp) {
                staticIpConfig = new ProvisioningConfiguration.Builder().withPreDhcpAction().withApfCapabilities(ClientModeImpl.this.mWifiNative.getApfCapabilities(ClientModeImpl.this.mInterfaceName)).withNetwork(ClientModeImpl.this.getCurrentNetwork()).withDisplayName(currentConfig.SSID).withRandomMacAddress().build();
            } else {
                staticIpConfig = new ProvisioningConfiguration.Builder().withStaticConfiguration(currentConfig.getStaticIpConfiguration()).withApfCapabilities(ClientModeImpl.this.mWifiNative.getApfCapabilities(ClientModeImpl.this.mInterfaceName)).withNetwork(ClientModeImpl.this.getCurrentNetwork()).withDisplayName(currentConfig.SSID).build();
            }
            if (ClientModeImpl.this.mIpClient != null) {
                ClientModeImpl.this.mIpClient.startProvisioning(staticIpConfig);
                ClientModeImpl.this.getWifiLinkLayerStats();
                return;
            }
            Log.d(ClientModeImpl.TAG, "IpClient is not ready to use, going back to disconnected state");
            ClientModeImpl.this.notifyDisconnectInternalReason(24);
            ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
        }

        public boolean processMessage(Message message) {
            boolean handleStatus = true;
            switch (message.what) {
                case ClientModeImpl.CMD_SET_HIGH_PERF_MODE /*131149*/:
                    int unused = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DEFERRED;
                    ClientModeImpl.this.deferMessage(message);
                    break;
                case ClientModeImpl.CMD_START_CONNECT /*131215*/:
                    if ("any".equals((String) message.obj)) {
                        return false;
                    }
                    break;
                case ClientModeImpl.CMD_START_ROAM /*131217*/:
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    ClientModeImpl.this.reportConnectionAttemptEnd(6, 1, 0);
                    handleStatus = false;
                    break;
                case 151559:
                    int unused2 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DEFERRED;
                    ClientModeImpl.this.deferMessage(message);
                    break;
                default:
                    handleStatus = false;
                    break;
            }
            int unused3 = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
            if (handleStatus) {
                ClientModeImpl.this.logStateAndMessage(message, this);
            }
            return handleStatus;
        }

        public void exit() {
        }
    }

    @VisibleForTesting
    public boolean shouldEvaluateWhetherToSendExplicitlySelected(WifiConfiguration currentConfig) {
        if (currentConfig == null) {
            Log.wtf(TAG, "Current WifiConfiguration is null, but IP provisioning just succeeded");
            return false;
        }
        long currentTimeMillis = this.mClock.getElapsedSinceBootMillis();
        if (this.mWifiConfigManager.getLastSelectedNetwork() != currentConfig.networkId || currentTimeMillis - this.mWifiConfigManager.getLastSelectedTimeStamp() >= 30000) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void sendConnectedState() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        boolean explicitlySelected = false;
        int issuedUid = getIssueUidForConnectingNetwork(config);
        if (shouldEvaluateWhetherToSendExplicitlySelected(config)) {
            explicitlySelected = this.mWifiPermissionsUtil.checkNetworkSettingsPermission(issuedUid);
            if (this.mVerboseLoggingEnabled) {
                log("Network selected by UID " + issuedUid + " explicitlySelected=" + explicitlySelected);
            }
        }
        if (this.mVerboseLoggingEnabled) {
            log("explictlySelected=" + explicitlySelected + " acceptUnvalidated=" + config.noInternetAccessExpected);
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        String packageName = this.mContext.getPackageManager().getNameForUid(issuedUid);
        if (explicitlySelected && (config.isEphemeral() || (packageName != null && this.NETWORKSETTINGS_PERMISSION_EXCEPTION_PACKAGE_LIST.contains(packageName)))) {
            explicitlySelected = false;
            if (this.mVerboseLoggingEnabled) {
                log("explictlySelected Exception case for smarthings =" + false + " acceptUnvalidated=" + config.noInternetAccessExpected);
            }
        }
        Log.d(TAG, "noInternetAccessExpected : " + config.isNoInternetAccessExpected() + ", CUid : " + config.creatorUid + ",sLUid : " + config.lastUpdateUid);
        this.mIsManualSelection = explicitlySelected;
        this.mCm.setMultiNetwork(false, issuedUid);
        if (this.mNetworkAgent != null) {
            if (config.isCaptivePortal || (!config.isNoInternetAccessExpected() && (explicitlySelected || isPoorNetworkTestEnabled() || isMultiNetworkAvailableApp(config.creatorUid, issuedUid, packageName)))) {
                if (!explicitlySelected && (config.isEphemeral() || isMultiNetworkAvailableApp(config.creatorUid, issuedUid, packageName))) {
                    log("MultiNetwork - package : " + packageName);
                    this.mCm.setMultiNetwork(true, issuedUid);
                }
                this.mNetworkAgent.explicitlySelected(explicitlySelected, config.noInternetAccessExpected);
            } else {
                this.mNetworkAgent.explicitlySelected(true, true);
            }
        }
        setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTED);
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        if ("".equals(CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && MobileWipsFrameworkService.getInstance() != null) {
            MobileWipsFrameworkService.getInstance().sendEmptyMessage(7);
        }
    }

    public int getIssueUidForConnectingNetwork(WifiConfiguration config) {
        int[] uids = {config.creatorUid, config.lastUpdateUid, config.lastConnectUid};
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        PackageManager packageManager = this.mContext.getPackageManager();
        for (int uid : uids) {
            if (uid > 1010) {
                try {
                    if (this.MULTINETWORK_ALLOWING_SYSTEM_PACKAGE_LIST.contains(packageManager.getNameForUid(uid))) {
                        return uid;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return config.lastConnectUid >= 1000 ? config.lastConnectUid : config.creatorUid;
    }

    private boolean isMultiNetworkAvailableApp(int cuid, int issuedUid, String packageName) {
        if (cuid <= 1010 || issuedUid <= 1010) {
            return this.MULTINETWORK_ALLOWING_SYSTEM_PACKAGE_LIST.contains(packageName);
        }
        if (packageName != null && this.MULTINETWORK_EXCEPTION_PACKAGE_LIST.contains(packageName)) {
            return false;
        }
        if (issuedUid > 1010) {
            return true;
        }
        return false;
    }

    class RoamingState extends State {
        boolean mAssociated;

        RoamingState() {
        }

        public void enter() {
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                ClientModeImpl.this.log("RoamingState Enter mScreenOn=" + ClientModeImpl.this.mScreenOn);
            }
            ClientModeImpl.this.mRoamWatchdogCount++;
            ClientModeImpl.this.logd("Start Roam Watchdog " + ClientModeImpl.this.mRoamWatchdogCount);
            ClientModeImpl clientModeImpl = ClientModeImpl.this;
            clientModeImpl.sendMessageDelayed(clientModeImpl.obtainMessage(ClientModeImpl.CMD_ROAM_WATCHDOG_TIMER, clientModeImpl.mRoamWatchdogCount, 0), 15000);
            this.mAssociated = false;
            ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
            clientModeImpl2.report(3, ReportUtil.getReportDataForRoamingEnter("framework-start", clientModeImpl2.mWifiInfo.getSSID(), ClientModeImpl.this.mWifiInfo.getBSSID(), ClientModeImpl.this.mWifiInfo.getRssi()));
            ClientModeImpl.this.mBigDataManager.addOrUpdateValue(7, 1);
            ClientModeImpl.this.notifyMobilewipsRoamEvent("start");
        }

        public boolean processMessage(Message message) {
            boolean handleStatus = true;
            switch (message.what) {
                case ClientModeImpl.CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                    if (ClientModeImpl.this.mRoamWatchdogCount == message.arg1) {
                        if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                            ClientModeImpl.this.log("roaming watchdog! -> disconnect");
                        }
                        ClientModeImpl.this.mWifiMetrics.endConnectionEvent(9, 1, 0);
                        ClientModeImpl.access$20708(ClientModeImpl.this);
                        ClientModeImpl.this.handleNetworkDisconnect();
                        ClientModeImpl.this.mWifiMetrics.logStaEvent(15, 4);
                        ClientModeImpl.this.notifyDisconnectInternalReason(5);
                        ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                        ClientModeImpl clientModeImpl = ClientModeImpl.this;
                        clientModeImpl.transitionTo(clientModeImpl.mDisconnectedState);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_IP_CONFIGURATION_LOST /*131211*/:
                    if (ClientModeImpl.this.getCurrentWifiConfiguration() != null) {
                        ClientModeImpl.this.mWifiDiagnostics.triggerBugReportDataCapture(3);
                    }
                    handleStatus = false;
                    break;
                case ClientModeImpl.CMD_UNWANTED_NETWORK /*131216*/:
                    if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                        ClientModeImpl.this.log("Roaming and CS doesn't want the network -> ignore");
                        break;
                    }
                    break;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (!this.mAssociated) {
                        int unused = ClientModeImpl.this.mMessageHandlingStatus = ClientModeImpl.MESSAGE_HANDLING_STATUS_DISCARD;
                        break;
                    } else {
                        if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                            ClientModeImpl.this.log("roaming and Network connection established");
                        }
                        ClientModeImpl.this.setRoamTriggered(true);
                        int unused2 = ClientModeImpl.this.mLastNetworkId = message.arg1;
                        ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
                        int unused3 = clientModeImpl2.mLastConnectedNetworkId = clientModeImpl2.mLastNetworkId;
                        String unused4 = ClientModeImpl.this.mLastBssid = (String) message.obj;
                        ClientModeImpl.this.mWifiInfo.setBSSID(ClientModeImpl.this.mLastBssid);
                        ClientModeImpl.this.mWifiInfo.setNetworkId(ClientModeImpl.this.mLastNetworkId);
                        ClientModeImpl.this.mWifiConnectivityManager.trackBssid(ClientModeImpl.this.mLastBssid, true, message.arg2);
                        ClientModeImpl clientModeImpl3 = ClientModeImpl.this;
                        clientModeImpl3.sendNetworkStateChangeBroadcast(clientModeImpl3.mLastBssid);
                        ClientModeImpl.this.reportConnectionAttemptEnd(1, 1, 0);
                        ClientModeImpl clientModeImpl4 = ClientModeImpl.this;
                        clientModeImpl4.report(3, ReportUtil.getReportDataForRoamingEnter("framework-completed", clientModeImpl4.mWifiInfo.getSSID(), ClientModeImpl.this.mWifiInfo.getBSSID(), ClientModeImpl.this.mWifiInfo.getRssi()));
                        ClientModeImpl.this.clearTargetBssid("RoamingCompleted");
                        int tmpDhcpRenewAfterRoamingMode = ClientModeImpl.this.getRoamDhcpPolicy();
                        WifiConfiguration currentConfig = ClientModeImpl.this.getCurrentWifiConfiguration();
                        boolean isUsingStaticIp = false;
                        if (currentConfig != null) {
                            isUsingStaticIp = currentConfig.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
                        }
                        if (isUsingStaticIp) {
                            ClientModeImpl.this.log(" Static ip - skip renew");
                            tmpDhcpRenewAfterRoamingMode = -1;
                        }
                        if (ClientModeImpl.this.mRoamDhcpPolicyByB2bConfig == 2 || tmpDhcpRenewAfterRoamingMode == 2 || tmpDhcpRenewAfterRoamingMode == -1) {
                            ClientModeImpl clientModeImpl5 = ClientModeImpl.this;
                            clientModeImpl5.log(" Skip Dhcp - mRoamDhcpPolicyByB2bConfig:" + ClientModeImpl.this.mRoamDhcpPolicyByB2bConfig + " ,tmpDhcpRenewAfterRoamingMode : " + tmpDhcpRenewAfterRoamingMode);
                            ClientModeImpl.this.setRoamTriggered(false);
                        } else if (ClientModeImpl.this.checkIfForceRestartDhcp()) {
                            ClientModeImpl.this.restartDhcp(currentConfig);
                        } else {
                            ClientModeImpl.this.CheckIfDefaultGatewaySame();
                        }
                        ClientModeImpl clientModeImpl6 = ClientModeImpl.this;
                        clientModeImpl6.transitionTo(clientModeImpl6.mConnectedState);
                        break;
                    }
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    String bssid = (String) message.obj;
                    String target = "";
                    if (ClientModeImpl.this.mTargetRoamBSSID != null) {
                        target = ClientModeImpl.this.mTargetRoamBSSID;
                    }
                    ClientModeImpl clientModeImpl7 = ClientModeImpl.this;
                    clientModeImpl7.log("NETWORK_DISCONNECTION_EVENT in roaming state BSSID=" + bssid + " target=" + target);
                    if (bssid != null && bssid.equals(ClientModeImpl.this.mTargetRoamBSSID)) {
                        ClientModeImpl.this.handleNetworkDisconnect();
                        ClientModeImpl clientModeImpl8 = ClientModeImpl.this;
                        clientModeImpl8.transitionTo(clientModeImpl8.mDisconnectedState);
                        break;
                    }
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState access$11800 = ClientModeImpl.this.handleSupplicantStateChange(message);
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (stateChangeResult.state == SupplicantState.DISCONNECTED || stateChangeResult.state == SupplicantState.INACTIVE || stateChangeResult.state == SupplicantState.INTERFACE_DISABLED) {
                        if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                            ClientModeImpl clientModeImpl9 = ClientModeImpl.this;
                            clientModeImpl9.log("STATE_CHANGE_EVENT in roaming state " + stateChangeResult.toString());
                        }
                        if (stateChangeResult.BSSID != null && stateChangeResult.BSSID.equals(ClientModeImpl.this.mTargetRoamBSSID)) {
                            ClientModeImpl.this.notifyDisconnectInternalReason(6);
                            if (ClientModeImpl.this.mIWCMonitorChannel != null) {
                                ClientModeImpl.this.mIWCMonitorChannel.sendMessage(IWCMonitor.IWC_WIFI_DISCONNECTED, 0);
                            }
                            ClientModeImpl.this.handleNetworkDisconnect();
                            ClientModeImpl clientModeImpl10 = ClientModeImpl.this;
                            clientModeImpl10.transitionTo(clientModeImpl10.mDisconnectedState);
                        }
                    }
                    if (stateChangeResult.state == SupplicantState.ASSOCIATED) {
                        this.mAssociated = true;
                        if (stateChangeResult.BSSID != null) {
                            String unused5 = ClientModeImpl.this.mTargetRoamBSSID = stateChangeResult.BSSID;
                            break;
                        }
                    }
                    break;
                default:
                    handleStatus = false;
                    break;
            }
            if (handleStatus) {
                ClientModeImpl.this.logStateAndMessage(message, this);
            }
            return handleStatus;
        }

        public void exit() {
            ClientModeImpl.this.logd("ClientModeImpl: Leaving Roaming state");
            ClientModeImpl.this.mBigDataManager.addOrUpdateValue(7, 0);
        }
    }

    class ConnectedState extends State {
        private WifiConfiguration mCurrentConfig;

        ConnectedState() {
        }

        public void enter() {
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                ClientModeImpl clientModeImpl = ClientModeImpl.this;
                clientModeImpl.log("Enter ConnectedState  mScreenOn=" + ClientModeImpl.this.mScreenOn);
            }
            ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
            long unused = clientModeImpl2.mLastConnectedTime = clientModeImpl2.mClock.getElapsedSinceBootMillis();
            ClientModeImpl.this.reportConnectionAttemptEnd(1, 1, 0);
            ClientModeImpl.this.mWifiConnectivityManager.handleConnectionStateChanged(1);
            ClientModeImpl.this.registerConnected();
            long unused2 = ClientModeImpl.this.mLastConnectAttemptTimestamp = 0;
            WifiConfiguration unused3 = ClientModeImpl.this.mTargetWifiConfiguration = null;
            ClientModeImpl.this.mWifiScoreReport.reset();
            int unused4 = ClientModeImpl.this.mLastSignalLevel = -1;
            boolean unused5 = ClientModeImpl.this.mIsAutoRoaming = false;
            ClientModeImpl.this.mWifiConfigManager.setCurrentNetworkId(ClientModeImpl.this.mTargetNetworkId);
            long unused6 = ClientModeImpl.this.mLastDriverRoamAttempt = 0;
            int unused7 = ClientModeImpl.this.mTargetNetworkId = -1;
            ClientModeImpl.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(true);
            ClientModeImpl.this.mWifiStateTracker.updateState(3);
            this.mCurrentConfig = ClientModeImpl.this.getCurrentWifiConfiguration();
            WifiConfiguration wifiConfiguration = this.mCurrentConfig;
            if (wifiConfiguration != null && wifiConfiguration.isPasspoint() && Settings.Secure.getInt(ClientModeImpl.this.mContext.getContentResolver(), "wifi_hotspot20_connected_history", 0) == 0) {
                Log.d(ClientModeImpl.TAG, "ConnectedState, WIFI_HOTSPOT20_CONNECTED_HISTORY    is set to 1");
                Settings.Secure.putInt(ClientModeImpl.this.mContext.getContentResolver(), "wifi_hotspot20_connected_history", 1);
            }
            WifiConfiguration wifiConfiguration2 = this.mCurrentConfig;
            if (!(wifiConfiguration2 == null || wifiConfiguration2.SSID == null)) {
                if (this.mCurrentConfig.allowedKeyManagement.get(0) || this.mCurrentConfig.allowedKeyManagement.get(1) || this.mCurrentConfig.allowedKeyManagement.get(4) || this.mCurrentConfig.allowedKeyManagement.get(22)) {
                    Context access$500 = ClientModeImpl.this.mContext;
                    WifiMobileDeviceManager.auditLog(access$500, 5, true, ClientModeImpl.TAG, "Wi-Fi is connected to " + this.mCurrentConfig.getPrintableSsid() + " network using 802.11-2012 channel");
                } else if (this.mCurrentConfig.enterpriseConfig != null) {
                    if (this.mCurrentConfig.enterpriseConfig.getEapMethod() == 1) {
                        Context access$5002 = ClientModeImpl.this.mContext;
                        WifiMobileDeviceManager.auditLog(access$5002, 5, true, ClientModeImpl.TAG, "Wi-Fi is connected to " + this.mCurrentConfig.getPrintableSsid() + " network using EAP-TLS channel");
                    } else {
                        Context access$5003 = ClientModeImpl.this.mContext;
                        WifiMobileDeviceManager.auditLog(access$5003, 5, true, ClientModeImpl.TAG, "Wi-Fi is connected to " + this.mCurrentConfig.getPrintableSsid() + " network using 802.1X channel");
                    }
                }
            }
            ReportUtil.startTimerDuringConnection();
            ReportUtil.updateWifiInfo(ClientModeImpl.this.mWifiInfo);
            WifiConfiguration wifiConfiguration3 = this.mCurrentConfig;
            if (wifiConfiguration3 != null) {
                if (wifiConfiguration3.semIsVendorSpecificSsid) {
                    int unused8 = ClientModeImpl.this.mConnectedApInternalType = 4;
                } else if (this.mCurrentConfig.semSamsungSpecificFlags.get(1)) {
                    int unused9 = ClientModeImpl.this.mConnectedApInternalType = 3;
                } else if (this.mCurrentConfig.isPasspoint()) {
                    int unused10 = ClientModeImpl.this.mConnectedApInternalType = 2;
                } else if (this.mCurrentConfig.semIsWeChatAp) {
                    int unused11 = ClientModeImpl.this.mConnectedApInternalType = 1;
                } else if (this.mCurrentConfig.isCaptivePortal) {
                    int unused12 = ClientModeImpl.this.mConnectedApInternalType = 6;
                } else if (this.mCurrentConfig.semAutoWifiScore > 4) {
                    int unused13 = ClientModeImpl.this.mConnectedApInternalType = 5;
                } else if (this.mCurrentConfig.isEphemeral()) {
                    int unused14 = ClientModeImpl.this.mConnectedApInternalType = 7;
                } else {
                    int unused15 = ClientModeImpl.this.mConnectedApInternalType = 0;
                }
                ClientModeImpl.this.mBigDataManager.addOrUpdateValue(11, ClientModeImpl.this.mConnectedApInternalType);
            }
            ClientModeImpl.this.mBigDataManager.addOrUpdateValue(9, 0);
            ClientModeImpl.this.mBigDataManager.addOrUpdateValue(12, 0);
            ClientModeImpl clientModeImpl3 = ClientModeImpl.this;
            clientModeImpl3.report(2, ReportUtil.getReportDataForConnectTranstion(clientModeImpl3.mConnectedApInternalType));
            if (ClientModeImpl.this.mWifiGeofenceManager.isSupported() && ClientModeImpl.this.mWifiGeofenceManager.isValidAccessPointToUseGeofence(ClientModeImpl.this.mWifiInfo, this.mCurrentConfig)) {
                ClientModeImpl.this.mWifiGeofenceManager.triggerStartLearning(this.mCurrentConfig);
            }
            WifiConfiguration config = ClientModeImpl.this.getCurrentWifiConfiguration();
            if (ClientModeImpl.this.isLocationSupportedAp(config)) {
                double[] latitudeLongitude = ClientModeImpl.this.getLatitudeLongitude(config);
                if (latitudeLongitude[0] == ClientModeImpl.INVALID_LATITUDE_LONGITUDE || latitudeLongitude[1] == ClientModeImpl.INVALID_LATITUDE_LONGITUDE) {
                    int unused16 = ClientModeImpl.this.mLocationRequestNetworkId = config.networkId;
                    ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_UPDATE_CONFIG_LOCATION, 1, 10000);
                } else {
                    ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_UPDATE_CONFIG_LOCATION, 0);
                }
            }
            ClientModeImpl.this.mWifiInjector.getWifiLockManager().updateWifiClientConnected(true);
            if (ClientModeImpl.CSC_SUPPORT_5G_ANT_SHARE) {
                ClientModeImpl clientModeImpl4 = ClientModeImpl.this;
                clientModeImpl4.sendIpcMessageToRilForLteu(4, true, clientModeImpl4.mWifiInfo.is5GHz(), false);
            }
            ClientModeImpl.this.handleCellularCapabilities(true);
            ClientModeImpl.this.updateEDMWiFiPolicy();
            if (WifiRoamingAssistant.getInstance() != null && this.mCurrentConfig != null) {
                WifiRoamingAssistant.getInstance().updateRcl(this.mCurrentConfig.configKey(), ClientModeImpl.this.mWifiInfo.getFrequency(), true);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r25v5, resolved type: java.lang.Object} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r9v14, resolved type: android.net.wifi.ScanResult} */
        /* JADX WARNING: Multi-variable type inference failed */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(android.os.Message r34) {
            /*
                r33 = this;
                r0 = r33
                r1 = r34
                r2 = 0
                r3 = 1
                int r4 = r1.what
                r6 = 6
                r8 = 10
                r9 = 4
                java.lang.String r11 = ""
                r12 = 0
                r14 = 5
                java.lang.String r5 = "WifiClientModeImpl"
                r7 = 2
                r15 = 0
                r10 = 1
                switch(r4) {
                    case 131216: goto L_0x0763;
                    case 131217: goto L_0x0651;
                    case 131219: goto L_0x05f1;
                    case 131220: goto L_0x05a4;
                    case 131225: goto L_0x0581;
                    case 131283: goto L_0x0554;
                    case 131332: goto L_0x0547;
                    case 131622: goto L_0x051c;
                    case 131623: goto L_0x04f7;
                    case 135288: goto L_0x014b;
                    case 147460: goto L_0x0020;
                    default: goto L_0x0019;
                }
            L_0x0019:
                r24 = r3
                r4 = r10
                r3 = r1
                r1 = 0
                goto L_0x0843
            L_0x0020:
                r16 = 0
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.reportConnectionAttemptEnd(r6, r10, r15)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                long r18 = r4.mLastDriverRoamAttempt
                int r4 = (r18 > r12 ? 1 : (r18 == r12 ? 0 : -1))
                if (r4 == 0) goto L_0x0048
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.Clock r4 = r4.mClock
                long r18 = r4.getWallClockMillis()
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                long r20 = r4.mLastDriverRoamAttempt
                long r16 = r18 - r20
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                long unused = r4.mLastDriverRoamAttempt = r12
            L_0x0048:
                int r4 = r1.arg2
                boolean r4 = com.android.server.wifi.ClientModeImpl.unexpectedDisconnectedReason(r4)
                if (r4 == 0) goto L_0x0059
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.BaseWifiDiagnostics r4 = r4.mWifiDiagnostics
                r4.triggerBugReportDataCapture(r14)
            L_0x0059:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r2 = r4.getCurrentWifiConfiguration()
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r6 = "disconnected reason "
                r4.append(r6)
                int r6 = r1.arg2
                r4.append(r6)
                java.lang.String r4 = r4.toString()
                android.util.Log.d(r5, r4)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.AsyncChannel r4 = r4.mIWCMonitorChannel
                if (r4 == 0) goto L_0x008b
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.AsyncChannel r4 = r4.mIWCMonitorChannel
                r5 = 552985(0x87019, float:7.74897E-40)
                int r6 = r1.arg2
                r4.sendMessage(r5, r6)
            L_0x008b:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r4 = r4.mUnstableApController
                if (r4 == 0) goto L_0x00cc
                if (r2 == 0) goto L_0x00cc
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.samsung.android.server.wifi.UnstableApController r4 = r4.mUnstableApController
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r5 = r5.mWifiInfo
                java.lang.String r5 = r5.getBSSID()
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                int r6 = r6.getRssi()
                int r11 = r1.arg2
                boolean r4 = r4.disconnect(r5, r6, r2, r11)
                if (r4 == 0) goto L_0x00cc
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r2.networkId
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                java.lang.String r6 = r6.getBSSID()
                android.os.Bundle r5 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForUnstableAp(r5, r6)
                r4.report(r8, r5)
            L_0x00cc:
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                boolean r4 = r4.mVerboseLoggingEnabled
                if (r4 == 0) goto L_0x0137
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "NETWORK_DISCONNECTION_EVENT in connected state BSSID="
                r5.append(r6)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                java.lang.String r6 = r6.getBSSID()
                r5.append(r6)
                java.lang.String r6 = " RSSI="
                r5.append(r6)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                int r6 = r6.getRssi()
                r5.append(r6)
                java.lang.String r6 = " freq="
                r5.append(r6)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                int r6 = r6.getFrequency()
                r5.append(r6)
                java.lang.String r6 = " reason="
                r5.append(r6)
                int r6 = r1.arg2
                r5.append(r6)
                java.lang.String r6 = " Network Selection Status="
                r5.append(r6)
                if (r2 != 0) goto L_0x0125
                java.lang.String r6 = "Unavailable"
                goto L_0x012d
            L_0x0125:
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r6 = r2.getNetworkSelectionStatus()
                java.lang.String r6 = r6.getNetworkStatusString()
            L_0x012d:
                r5.append(r6)
                java.lang.String r5 = r5.toString()
                r4.log(r5)
            L_0x0137:
                int r4 = com.android.server.wifi.ClientModeImpl.mWlanAdvancedDebugState
                r4 = r4 & r9
                if (r4 == 0) goto L_0x0143
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.sendBroadcastIssueTrackerSysDump(r7)
            L_0x0143:
                r4 = r10
                r32 = r3
                r3 = r1
                r1 = r32
                goto L_0x0843
            L_0x014b:
                java.lang.String r4 = "CONNECTED : CHECK_ALTERNATIVE_NETWORKS"
                android.util.Log.d(r5, r4)
                int r4 = r1.arg1
                if (r4 != r10) goto L_0x0156
                r4 = r10
                goto L_0x0157
            L_0x0156:
                r4 = r15
            L_0x0157:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r12 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r12 = r12.mWifiInfo
                int r12 = r12.getNetworkId()
                android.net.wifi.WifiConfiguration r6 = r6.getConfiguredNetwork((int) r12)
                r12 = 0
                java.util.ArrayList r13 = new java.util.ArrayList
                r13.<init>()
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ScanRequestProxy r8 = r8.mScanRequestProxy
                java.util.List r8 = r8.getScanResults()
                r13.addAll(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r8 = r8.mWifiConfigManager
                r9 = 1010(0x3f2, float:1.415E-42)
                java.util.List r8 = r8.getSavedNetworks(r9)
                r9 = 0
                r16 = 0
                com.android.server.wifi.ClientModeImpl r14 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r2 = r14.getCurrentWifiConfiguration()
                r14 = 0
                r17 = -1
                com.android.server.wifi.ClientModeImpl r15 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r15 = r15.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r7 = r7.mWifiInfo
                int r7 = r7.getNetworkId()
                android.net.wifi.WifiConfiguration r7 = r15.getConfiguredNetwork((int) r7)
                r15 = 0
                java.lang.String r10 = "WEP"
                if (r7 == 0) goto L_0x030f
                r24 = r3
                java.lang.String r3 = r7.SSID
                r25 = -1
                r26 = r6
                java.util.BitSet r6 = r7.allowedKeyManagement
                r27 = r9
                r9 = 1
                boolean r6 = r6.get(r9)
                if (r6 == 0) goto L_0x01c5
                r6 = 2
                goto L_0x01f0
            L_0x01c5:
                java.util.BitSet r6 = r7.allowedKeyManagement
                r9 = 8
                boolean r6 = r6.get(r9)
                if (r6 == 0) goto L_0x01d1
                r6 = 4
                goto L_0x01f0
            L_0x01d1:
                java.util.BitSet r6 = r7.allowedKeyManagement
                r9 = 2
                boolean r6 = r6.get(r9)
                if (r6 != 0) goto L_0x01ef
                java.util.BitSet r6 = r7.allowedKeyManagement
                r9 = 3
                boolean r6 = r6.get(r9)
                if (r6 == 0) goto L_0x01e4
                goto L_0x01ef
            L_0x01e4:
                java.lang.String[] r6 = r7.wepKeys
                r9 = 0
                r6 = r6[r9]
                if (r6 == 0) goto L_0x01ed
                r6 = 1
                goto L_0x01ee
            L_0x01ed:
                r6 = 0
            L_0x01ee:
                goto L_0x01f0
            L_0x01ef:
                r6 = 3
            L_0x01f0:
                java.util.Iterator r9 = r13.iterator()
            L_0x01f4:
                boolean r25 = r9.hasNext()
                if (r25 == 0) goto L_0x02c3
                java.lang.Object r25 = r9.next()
                r28 = r9
                r9 = r25
                android.net.wifi.ScanResult r9 = (android.net.wifi.ScanResult) r9
                r25 = 0
                r29 = r12
                java.lang.String r12 = r9.capabilities
                boolean r12 = r12.contains(r10)
                if (r12 == 0) goto L_0x0217
                r25 = 1
                r30 = r11
                r11 = r25
                goto L_0x0248
            L_0x0217:
                java.lang.String r12 = r9.capabilities
                r30 = r11
                java.lang.String r11 = "SAE"
                boolean r11 = r12.contains(r11)
                if (r11 == 0) goto L_0x0228
                r25 = 4
                r11 = r25
                goto L_0x0248
            L_0x0228:
                java.lang.String r11 = r9.capabilities
                java.lang.String r12 = "PSK"
                boolean r11 = r11.contains(r12)
                if (r11 == 0) goto L_0x0237
                r25 = 2
                r11 = r25
                goto L_0x0248
            L_0x0237:
                java.lang.String r11 = r9.capabilities
                java.lang.String r12 = "EAP"
                boolean r11 = r11.contains(r12)
                if (r11 == 0) goto L_0x0246
                r25 = 3
                r11 = r25
                goto L_0x0248
            L_0x0246:
                r11 = r25
            L_0x0248:
                java.lang.String r12 = r9.SSID
                if (r12 == 0) goto L_0x02b5
                if (r3 == 0) goto L_0x02b5
                int r12 = r3.length()
                r1 = 2
                if (r12 <= r1) goto L_0x02b5
                java.lang.String r1 = r9.SSID
                int r12 = r3.length()
                r25 = r2
                r2 = 1
                int r12 = r12 - r2
                java.lang.String r12 = r3.substring(r2, r12)
                boolean r1 = r1.equals(r12)
                if (r1 == 0) goto L_0x02b7
                if (r6 != r11) goto L_0x02b7
                boolean r1 = r7.isCaptivePortal
                if (r1 != 0) goto L_0x02b7
                boolean r1 = r9.is24GHz()
                if (r1 == 0) goto L_0x027b
                int r1 = r9.level
                r2 = -64
                if (r1 > r2) goto L_0x0287
            L_0x027b:
                boolean r1 = r9.is5GHz()
                if (r1 == 0) goto L_0x02b7
                int r1 = r9.level
                r2 = -70
                if (r1 <= r2) goto L_0x02b7
            L_0x0287:
                java.lang.String r1 = r9.BSSID
                if (r1 == 0) goto L_0x02b7
                java.lang.String r1 = r9.BSSID
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r2 = r2.mWifiInfo
                java.lang.String r2 = r2.getBSSID()
                boolean r1 = r1.equals(r2)
                if (r1 != 0) goto L_0x02b7
                if (r14 != 0) goto L_0x02a6
                r1 = r9
                int r2 = r7.networkId
                r14 = r1
                r17 = r2
                goto L_0x02b2
            L_0x02a6:
                int r1 = r14.level
                int r2 = r9.level
                if (r1 >= r2) goto L_0x02b2
                r1 = r9
                int r2 = r7.networkId
                r14 = r1
                r17 = r2
            L_0x02b2:
                int r15 = r15 + 1
                goto L_0x02b7
            L_0x02b5:
                r25 = r2
            L_0x02b7:
                r1 = r34
                r2 = r25
                r9 = r28
                r12 = r29
                r11 = r30
                goto L_0x01f4
            L_0x02c3:
                r25 = r2
                r30 = r11
                r29 = r12
                if (r14 == 0) goto L_0x0303
                java.lang.String r1 = r14.BSSID
                if (r1 == 0) goto L_0x0303
                int r1 = r14.level
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r2 = r2.mWifiInfo
                int r2 = r2.getRssi()
                r9 = 5
                int r2 = r2 + r9
                if (r1 <= r2) goto L_0x0303
                r1 = 1
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r9 = "There's available BSSID to roam to. Reassociate to the BSSID. "
                r2.append(r9)
                boolean r9 = com.android.server.wifi.ClientModeImpl.DBG
                if (r9 == 0) goto L_0x02f5
                java.lang.String r11 = r14.toString()
                goto L_0x02f7
            L_0x02f5:
                r11 = r30
            L_0x02f7:
                r2.append(r11)
                java.lang.String r2 = r2.toString()
                android.util.Log.d(r5, r2)
                r9 = r1
                goto L_0x0305
            L_0x0303:
                r9 = r27
            L_0x0305:
                if (r15 <= 0) goto L_0x0309
                r1 = 1
                goto L_0x030a
            L_0x0309:
                r1 = 0
            L_0x030a:
                r16 = r1
                r1 = r17
                goto L_0x031d
            L_0x030f:
                r25 = r2
                r24 = r3
                r26 = r6
                r27 = r9
                r30 = r11
                r29 = r12
                r1 = r17
            L_0x031d:
                if (r9 != 0) goto L_0x0482
                if (r8 == 0) goto L_0x0482
                java.util.Iterator r2 = r8.iterator()
                r12 = r29
            L_0x0327:
                boolean r3 = r2.hasNext()
                if (r3 == 0) goto L_0x047b
                java.lang.Object r3 = r2.next()
                android.net.wifi.WifiConfiguration r3 = (android.net.wifi.WifiConfiguration) r3
                boolean r6 = r3.validatedInternetAccess
                if (r6 == 0) goto L_0x0465
                int r6 = r3.semAutoReconnect
                r11 = 1
                if (r6 != r11) goto L_0x0465
                java.lang.String r6 = r3.SSID
                r17 = -1
                r22 = r2
                java.util.BitSet r2 = r3.allowedKeyManagement
                boolean r2 = r2.get(r11)
                if (r2 != 0) goto L_0x0380
                java.util.BitSet r2 = r3.allowedKeyManagement
                r11 = 4
                boolean r2 = r2.get(r11)
                if (r2 == 0) goto L_0x0354
                goto L_0x0380
            L_0x0354:
                java.util.BitSet r2 = r3.allowedKeyManagement
                r11 = 8
                boolean r2 = r2.get(r11)
                if (r2 == 0) goto L_0x0360
                r2 = 4
                goto L_0x0381
            L_0x0360:
                java.util.BitSet r2 = r3.allowedKeyManagement
                r11 = 2
                boolean r2 = r2.get(r11)
                if (r2 != 0) goto L_0x037e
                java.util.BitSet r2 = r3.allowedKeyManagement
                r11 = 3
                boolean r2 = r2.get(r11)
                if (r2 == 0) goto L_0x0373
                goto L_0x037e
            L_0x0373:
                java.lang.String[] r2 = r3.wepKeys
                r11 = 0
                r2 = r2[r11]
                if (r2 == 0) goto L_0x037c
                r2 = 1
                goto L_0x037d
            L_0x037c:
                r2 = 0
            L_0x037d:
                goto L_0x0381
            L_0x037e:
                r2 = 3
                goto L_0x0381
            L_0x0380:
                r2 = 2
            L_0x0381:
                java.util.Iterator r11 = r13.iterator()
            L_0x0385:
                boolean r17 = r11.hasNext()
                if (r17 == 0) goto L_0x045c
                java.lang.Object r17 = r11.next()
                r27 = r7
                r7 = r17
                android.net.wifi.ScanResult r7 = (android.net.wifi.ScanResult) r7
                r17 = 0
                r28 = r8
                java.lang.String r8 = r7.capabilities
                boolean r8 = r8.contains(r10)
                if (r8 == 0) goto L_0x03a8
                r17 = 1
                r31 = r10
                r8 = r17
                goto L_0x03d9
            L_0x03a8:
                java.lang.String r8 = r7.capabilities
                r31 = r10
                java.lang.String r10 = "SAE"
                boolean r8 = r8.contains(r10)
                if (r8 == 0) goto L_0x03b9
                r17 = 4
                r8 = r17
                goto L_0x03d9
            L_0x03b9:
                java.lang.String r8 = r7.capabilities
                java.lang.String r10 = "PSK"
                boolean r8 = r8.contains(r10)
                if (r8 == 0) goto L_0x03c8
                r17 = 2
                r8 = r17
                goto L_0x03d9
            L_0x03c8:
                java.lang.String r8 = r7.capabilities
                java.lang.String r10 = "EAP"
                boolean r8 = r8.contains(r10)
                if (r8 == 0) goto L_0x03d7
                r17 = 3
                r8 = r17
                goto L_0x03d9
            L_0x03d7:
                r8 = r17
            L_0x03d9:
                com.android.server.wifi.ClientModeImpl r10 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r10 = r10.mWifiInfo
                int r10 = r10.getNetworkId()
                r17 = r11
                int r11 = r3.networkId
                if (r10 == r11) goto L_0x044e
                if (r6 == 0) goto L_0x044e
                int r10 = r6.length()
                r11 = 2
                if (r10 <= r11) goto L_0x044e
                java.lang.String r10 = r7.SSID
                int r11 = r6.length()
                r29 = r12
                r12 = 1
                int r11 = r11 - r12
                java.lang.String r11 = r6.substring(r12, r11)
                boolean r10 = r10.equals(r11)
                if (r10 == 0) goto L_0x0450
                if (r2 != r8) goto L_0x0450
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r10 = r3.getNetworkSelectionStatus()
                boolean r10 = r10.isNetworkEnabled()
                if (r10 == 0) goto L_0x0450
                boolean r10 = r3.isCaptivePortal
                if (r10 != 0) goto L_0x0450
                boolean r10 = r7.is24GHz()
                if (r10 == 0) goto L_0x0422
                int r10 = r7.level
                r11 = -64
                if (r10 > r11) goto L_0x042e
            L_0x0422:
                boolean r10 = r7.is5GHz()
                if (r10 == 0) goto L_0x0450
                int r10 = r7.level
                r11 = -70
                if (r10 <= r11) goto L_0x0450
            L_0x042e:
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                java.lang.String r11 = "There's internet available AP. Disable current AP. "
                r10.append(r11)
                boolean r11 = com.android.server.wifi.ClientModeImpl.DBG
                if (r11 == 0) goto L_0x0440
                r11 = r6
                goto L_0x0442
            L_0x0440:
                r11 = r30
            L_0x0442:
                r10.append(r11)
                java.lang.String r10 = r10.toString()
                android.util.Log.d(r5, r10)
                r12 = 1
                goto L_0x0471
            L_0x044e:
                r29 = r12
            L_0x0450:
                r11 = r17
                r7 = r27
                r8 = r28
                r12 = r29
                r10 = r31
                goto L_0x0385
            L_0x045c:
                r27 = r7
                r28 = r8
                r31 = r10
                r29 = r12
                goto L_0x046f
            L_0x0465:
                r22 = r2
                r27 = r7
                r28 = r8
                r31 = r10
                r29 = r12
            L_0x046f:
                r12 = r29
            L_0x0471:
                r2 = r22
                r7 = r27
                r8 = r28
                r10 = r31
                goto L_0x0327
            L_0x047b:
                r27 = r7
                r28 = r8
                r29 = r12
                goto L_0x0486
            L_0x0482:
                r27 = r7
                r28 = r8
            L_0x0486:
                if (r4 != 0) goto L_0x04d2
                if (r9 == 0) goto L_0x0492
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                r2.startRoamToNetwork(r1, r14)
                r2 = r25
                goto L_0x04f5
            L_0x0492:
                if (r29 == 0) goto L_0x04cf
                if (r25 == 0) goto L_0x04a1
                java.lang.String r2 = "Current config's validatedInternetAccess sets as false because alternativeNetwork is Found."
                android.util.Log.d(r5, r2)
                r2 = r25
                r3 = 0
                r2.validatedInternetAccess = r3
                goto L_0x04a3
            L_0x04a1:
                r2 = r25
            L_0x04a3:
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r3 = r2.getNetworkSelectionStatus()
                int r3 = r3.getNetworkSelectionStatus()
                r6 = 2
                if (r3 == r6) goto L_0x04c1
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r3 = r3.mWifiConfigManager
                int r6 = r2.networkId
                r7 = 16
                r3.updateNetworkSelectionStatus((int) r6, (int) r7)
                java.lang.String r3 = "Disable the current network temporarily. DISABLED_POOR_LINK"
                android.util.Log.d(r5, r3)
                goto L_0x04f5
            L_0x04c1:
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                r6 = 10
                r7 = 0
                r3.disconnectCommand(r7, r6)
                java.lang.String r3 = "Already permanently disabled"
                android.util.Log.d(r5, r3)
                goto L_0x04f5
            L_0x04cf:
                r2 = r25
                goto L_0x04f5
            L_0x04d2:
                r2 = r25
                if (r9 == 0) goto L_0x04db
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                r3.startRoamToNetwork(r1, r14)
            L_0x04db:
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                java.util.ArrayList r3 = r3.mWifiNetworkCallbackList
                java.util.Iterator r3 = r3.iterator()
            L_0x04e5:
                boolean r5 = r3.hasNext()
                if (r5 == 0) goto L_0x04f5
                java.lang.Object r5 = r3.next()
                com.android.server.wifi.ClientModeImpl$ClientModeChannel$WifiNetworkCallback r5 = (com.android.server.wifi.ClientModeImpl.ClientModeChannel.WifiNetworkCallback) r5
                r5.handleResultRoamInLevel1State(r9)
                goto L_0x04e5
            L_0x04f5:
                r3 = 1
                return r3
            L_0x04f7:
                r24 = r3
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r1 = r1.mIpClient
                if (r1 == 0) goto L_0x0518
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r1 = r1.mIpClient
                r1.confirmConfiguration()
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r3 = 500(0x1f4, double:2.47E-321)
                r5 = 131623(0x20227, float:1.84443E-40)
                r1.sendMessageDelayed(r5, r3)
                r3 = r34
                goto L_0x05ec
            L_0x0518:
                r3 = r34
                goto L_0x05ec
            L_0x051c:
                r24 = r3
                r5 = 131623(0x20227, float:1.84443E-40)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r1.removeMessages(r5)
                com.samsung.android.server.wifi.bigdata.WifiChipInfo r1 = com.samsung.android.server.wifi.bigdata.WifiChipInfo.getInstance()
                boolean r1 = r1.getArpResult()
                if (r1 != 0) goto L_0x053d
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r4 = r3.getCurrentWifiConfiguration()
                r3.restartDhcp(r4)
                r3 = r34
                goto L_0x05ec
            L_0x053d:
                com.android.server.wifi.ClientModeImpl r3 = com.android.server.wifi.ClientModeImpl.this
                r4 = 0
                r3.setRoamTriggered(r4)
                r3 = r34
                goto L_0x05ec
            L_0x0547:
                r24 = r3
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r3 = r34
                int r4 = r3.arg1
                r1.updateLocation(r4)
                goto L_0x05ec
            L_0x0554:
                r24 = r3
                r3 = r1
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r1 = r1.mIpClient
                if (r1 == 0) goto L_0x05ec
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.ip.IpClientManager r1 = r1.mIpClient
                r1.sendDhcpReleasePacket()
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                int r1 = r1.waitForDhcpRelease()
                if (r1 == 0) goto L_0x0579
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "waitForDhcpRelease error"
                r1.loge(r4)
                goto L_0x05ec
            L_0x0579:
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = "waitForDhcpRelease() Success"
                r1.loge(r4)
                goto L_0x05ec
            L_0x0581:
                r24 = r3
                r3 = r1
                int r1 = r3.arg1
                if (r1 == 0) goto L_0x058b
                r23 = 1
                goto L_0x058d
            L_0x058b:
                r23 = 0
            L_0x058d:
                r1 = r23
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r4 = r4.mWifiConfigManager
                com.android.server.wifi.ClientModeImpl r5 = com.android.server.wifi.ClientModeImpl.this
                int r5 = r5.mLastNetworkId
                r4.setNetworkNoInternetAccessExpected(r5, r1)
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.sendWcmConfigurationChanged()
                goto L_0x05ec
            L_0x05a4:
                r24 = r3
                r3 = r1
                int r1 = r3.arg1
                r4 = 1
                if (r1 != r4) goto L_0x05ec
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r5 = 131324(0x200fc, float:1.84024E-40)
                r1.removeMessages(r5)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.BaseWifiDiagnostics r1 = r1.mWifiDiagnostics
                r1.reportConnectionEvent(r4)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiScoreCard r1 = r1.mWifiScoreCard
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r4 = r4.mWifiInfo
                r1.noteValidationSuccess(r4)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r2 = r1.getCurrentWifiConfiguration()
                if (r2 == 0) goto L_0x05ec
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r1 = r1.mWifiConfigManager
                int r4 = r2.networkId
                r5 = 0
                r1.updateNetworkSelectionStatus((int) r4, (int) r5)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r1 = r1.mWifiConfigManager
                int r4 = r2.networkId
                r5 = 1
                r1.setNetworkValidatedInternetAccess(r4, r5)
            L_0x05ec:
                r1 = r24
                r4 = 1
                goto L_0x0843
            L_0x05f1:
                r24 = r3
                r30 = r11
                r3 = r1
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.Clock r4 = r1.mClock
                long r4 = r4.getWallClockMillis()
                long unused = r1.mLastDriverRoamAttempt = r4
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r4 = r1.mWifiInfo
                java.lang.String r4 = r4.getSSID()
                java.lang.Object r5 = r3.obj
                java.lang.String r5 = (java.lang.String) r5
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r6 = r6.mWifiInfo
                int r6 = r6.getRssi()
                java.lang.String r7 = "dongle"
                android.os.Bundle r4 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForRoamingEnter(r7, r4, r5, r6)
                r5 = 3
                r1.report(r5, r4)
                r1 = 0
                java.lang.String r4 = com.android.server.wifi.ClientModeImpl.CONFIG_SECURE_SVC_INTEGRATION
                r5 = r30
                boolean r4 = r5.equals(r4)
                if (r4 == 0) goto L_0x064e
                com.samsung.android.feature.SemCscFeature r4 = com.samsung.android.feature.SemCscFeature.getInstance()
                java.lang.String r5 = "CscFeature_Wifi_DisableMWIPS"
                boolean r4 = r4.getBoolean(r5)
                if (r4 != 0) goto L_0x064e
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                if (r4 == 0) goto L_0x064e
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.getInstance()
                r5 = 9
                r4.sendEmptyMessage(r5)
            L_0x064e:
                r4 = 1
                goto L_0x0843
            L_0x0651:
                r24 = r3
                r3 = r1
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                long unused = r1.mLastDriverRoamAttempt = r12
                int r1 = r3.arg1
                java.lang.Object r4 = r3.obj
                android.net.wifi.ScanResult r4 = (android.net.wifi.ScanResult) r4
                java.lang.String r5 = "any"
                if (r4 == 0) goto L_0x0665
                java.lang.String r5 = r4.BSSID
            L_0x0665:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r6 = r6.mWifiConfigManager
                android.net.wifi.WifiConfiguration r2 = r6.getConfiguredNetworkWithoutMasking(r1)
                if (r2 != 0) goto L_0x067a
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = "CMD_START_ROAM and no config, bail out..."
                r6.loge(r7)
                goto L_0x05ec
            L_0x067a:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiScoreCard r6 = r6.mWifiScoreCard
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.ExtendedWifiInfo r7 = r7.mWifiInfo
                r6.noteConnectionAttempt(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                boolean unused = r6.setTargetBssid(r2, r5)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                int unused = r6.mTargetNetworkId = r1
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "CMD_START_ROAM sup state "
                r7.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.SupplicantStateTracker r8 = r8.mSupplicantStateTracker
                java.lang.String r8 = r8.getSupplicantStateName()
                r7.append(r8)
                java.lang.String r8 = " my state "
                r7.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.IState r8 = r8.getCurrentState()
                java.lang.String r8 = r8.getName()
                r7.append(r8)
                java.lang.String r8 = " nid="
                r7.append(r8)
                java.lang.String r8 = java.lang.Integer.toString(r1)
                r7.append(r8)
                java.lang.String r8 = " config "
                r7.append(r8)
                java.lang.String r8 = r2.configKey()
                r7.append(r8)
                java.lang.String r8 = " targetRoamBSSID "
                r7.append(r8)
                com.android.server.wifi.ClientModeImpl r8 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r8 = r8.mTargetRoamBSSID
                r7.append(r8)
                java.lang.String r7 = r7.toString()
                r6.logd(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r6.mTargetRoamBSSID
                r8 = 3
                r6.reportConnectionAttemptStart(r2, r7, r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r6 = r6.mWifiNative
                com.android.server.wifi.ClientModeImpl r7 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r7 = r7.mInterfaceName
                boolean r6 = r6.roamToNetwork(r7, r2)
                if (r6 == 0) goto L_0x0735
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.Clock r7 = r6.mClock
                long r7 = r7.getWallClockMillis()
                long unused = r6.mLastConnectAttemptTimestamp = r7
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration unused = r6.mTargetWifiConfiguration = r2
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r7 = 1
                boolean unused = r6.mIsAutoRoaming = r7
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r6 = r6.mWifiMetrics
                r7 = 12
                r6.logStaEvent((int) r7, (android.net.wifi.WifiConfiguration) r2)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r7 = r6.mRoamingState
                r6.transitionTo(r7)
                goto L_0x05ec
            L_0x0735:
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                java.lang.StringBuilder r7 = new java.lang.StringBuilder
                r7.<init>()
                java.lang.String r8 = "CMD_START_ROAM Failed to start roaming to network "
                r7.append(r8)
                r7.append(r2)
                java.lang.String r7 = r7.toString()
                r6.loge(r7)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r7 = 5
                r8 = 0
                r9 = 1
                r6.reportConnectionAttemptEnd(r7, r9, r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r7 = 151554(0x25002, float:2.12372E-40)
                r6.replyToMessage((android.os.Message) r3, (int) r7, (int) r8)
                com.android.server.wifi.ClientModeImpl r6 = com.android.server.wifi.ClientModeImpl.this
                r7 = -2
                int unused = r6.mMessageHandlingStatus = r7
                goto L_0x05ec
            L_0x0763:
                r24 = r3
                r3 = r1
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                int r4 = r3.arg1
                android.os.Bundle r4 = com.samsung.android.server.wifi.dqa.ReportUtil.getReportDataForUnwantedMessage(r4)
                r7 = 5
                r1.report(r7, r4)
                int r1 = r3.arg1
                if (r1 != 0) goto L_0x07a3
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiMetrics r1 = r1.mWifiMetrics
                r4 = 15
                r5 = 3
                r1.logStaEvent((int) r4, (int) r5)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r4 = 8
                r1.notifyDisconnectInternalReason(r4)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiNative r1 = r1.mWifiNative
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                java.lang.String r4 = r4.mInterfaceName
                r1.disconnect(r4)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                com.android.internal.util.State r4 = r1.mDisconnectingState
                r1.transitionTo(r4)
                goto L_0x0831
            L_0x07a3:
                int r1 = r3.arg1
                r4 = 2
                if (r1 == r4) goto L_0x07ad
                int r1 = r3.arg1
                r4 = 1
                if (r1 != r4) goto L_0x0831
            L_0x07ad:
                int r1 = r3.arg1
                r4 = 2
                if (r1 != r4) goto L_0x07b5
                java.lang.String r1 = "NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN"
                goto L_0x07b7
            L_0x07b5:
                java.lang.String r1 = "NETWORK_STATUS_UNWANTED_VALIDATION_FAILED"
            L_0x07b7:
                android.util.Log.d(r5, r1)
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                android.net.wifi.WifiConfiguration r1 = r1.getCurrentWifiConfiguration()
                if (r1 == 0) goto L_0x0830
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r2 = r2.mWifiConfigManager
                int r4 = r1.networkId
                r7 = 0
                r2.setNetworkValidatedInternetAccess(r4, r7)
                int r2 = r3.arg1
                r4 = 2
                if (r2 != r4) goto L_0x07e1
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r2 = r2.mWifiConfigManager
                int r4 = r1.networkId
                r5 = 10
                r2.updateNetworkSelectionStatus((int) r4, (int) r5)
                goto L_0x0830
            L_0x07e1:
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                r4 = 131324(0x200fc, float:1.84024E-40)
                r2.removeMessages(r4)
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.BaseWifiDiagnostics r2 = r2.mWifiDiagnostics
                r4 = 2
                r2.reportConnectionEvent(r4)
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r2 = r2.mWifiConfigManager
                int r4 = r1.networkId
                r2.incrementNetworkNoInternetAccessReports(r4)
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                boolean r2 = r2.isWCMEnabled()
                if (r2 == 0) goto L_0x080e
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                boolean r2 = r2.isWifiOnly()
                if (r2 == 0) goto L_0x0830
            L_0x080e:
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r2 = r2.mWifiConfigManager
                int r2 = r2.getLastSelectedNetwork()
                int r4 = r1.networkId
                if (r2 == r4) goto L_0x0830
                boolean r2 = r1.noInternetAccessExpected
                if (r2 != 0) goto L_0x0830
                java.lang.String r2 = "Temporarily disabling network because ofno-internet access"
                android.util.Log.i(r5, r2)
                com.android.server.wifi.ClientModeImpl r2 = com.android.server.wifi.ClientModeImpl.this
                com.android.server.wifi.WifiConfigManager r2 = r2.mWifiConfigManager
                int r4 = r1.networkId
                r2.updateNetworkSelectionStatus((int) r4, (int) r6)
            L_0x0830:
                r2 = r1
            L_0x0831:
                int r1 = com.android.server.wifi.ClientModeImpl.mWlanAdvancedDebugState
                r4 = 4
                r1 = r1 & r4
                if (r1 == 0) goto L_0x0840
                com.android.server.wifi.ClientModeImpl r1 = com.android.server.wifi.ClientModeImpl.this
                r4 = 1
                r1.sendBroadcastIssueTrackerSysDump(r4)
                goto L_0x0841
            L_0x0840:
                r4 = 1
            L_0x0841:
                r1 = r24
            L_0x0843:
                if (r1 != r4) goto L_0x084a
                com.android.server.wifi.ClientModeImpl r4 = com.android.server.wifi.ClientModeImpl.this
                r4.logStateAndMessage(r3, r0)
            L_0x084a:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ClientModeImpl.ConnectedState.processMessage(android.os.Message):boolean");
        }

        public void exit() {
            ClientModeImpl.this.logd("ClientModeImpl: Leaving Connected state");
            long unused = ClientModeImpl.this.mLastConnectedTime = -1;
            ClientModeImpl.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
            long unused2 = ClientModeImpl.this.mLastDriverRoamAttempt = 0;
            ClientModeImpl.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(false);
            ClientModeImpl.this.mDelayDisconnect.setEnable(false, 0);
            ClientModeImpl.this.mWifiConfigManager.setCurrentNetworkId(-1);
            if (ClientModeImpl.CSC_SUPPORT_5G_ANT_SHARE) {
                ClientModeImpl.this.sendIpcMessageToRilForLteu(4, false, false, false);
            }
            WifiConfiguration wifiConfiguration = this.mCurrentConfig;
            if (!(wifiConfiguration == null || wifiConfiguration.SSID == null)) {
                if (this.mCurrentConfig.allowedKeyManagement.get(0) || this.mCurrentConfig.allowedKeyManagement.get(1) || this.mCurrentConfig.allowedKeyManagement.get(4) || this.mCurrentConfig.allowedKeyManagement.get(22)) {
                    Context access$500 = ClientModeImpl.this.mContext;
                    WifiMobileDeviceManager.auditLog(access$500, 5, true, ClientModeImpl.TAG, "Wi-Fi is disconnected from " + this.mCurrentConfig.getPrintableSsid() + " network using 802.11-2012 channel");
                } else if (this.mCurrentConfig.enterpriseConfig != null) {
                    if (this.mCurrentConfig.enterpriseConfig.getEapMethod() == 1) {
                        Context access$5002 = ClientModeImpl.this.mContext;
                        WifiMobileDeviceManager.auditLog(access$5002, 5, true, ClientModeImpl.TAG, "Wi-Fi is disconnected from " + this.mCurrentConfig.getPrintableSsid() + " network using EAP-TLS channel");
                    } else {
                        Context access$5003 = ClientModeImpl.this.mContext;
                        WifiMobileDeviceManager.auditLog(access$5003, 5, true, ClientModeImpl.TAG, "Wi-Fi is disconnected from " + this.mCurrentConfig.getPrintableSsid() + " network using 802.1X channel");
                    }
                }
            }
            if (ClientModeImpl.this.mWifiGeofenceManager.isSupported()) {
                ClientModeImpl.this.mWifiGeofenceManager.triggerStopLearning(this.mCurrentConfig);
            }
            if (ClientModeImpl.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() && OpBrandingLoader.Vendor.VZW != ClientModeImpl.mOpBranding) {
                ClientModeImpl clientModeImpl = ClientModeImpl.this;
                clientModeImpl.logd("Wifi got Disconnected in connectedstate, Send provisioning intent mIsAutoRoaming" + ClientModeImpl.this.mIsAutoRoaming);
                Intent provisionIntent = new Intent(SemWifiApBroadcastReceiver.START_PROVISIONING);
                provisionIntent.putExtra("wState", 2);
                if (ClientModeImpl.this.mContext != null) {
                    ClientModeImpl.this.mContext.sendBroadcast(provisionIntent);
                    provisionIntent.setPackage("com.android.settings");
                    ClientModeImpl.this.mContext.sendBroadcast(provisionIntent);
                }
            } else if (!ClientModeImpl.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() || OpBrandingLoader.Vendor.VZW != ClientModeImpl.mOpBranding || !ClientModeImpl.this.isWifiSharingProvisioning()) {
                if (!ClientModeImpl.this.mIsAutoRoaming && OpBrandingLoader.Vendor.VZW == ClientModeImpl.mOpBranding) {
                    SemWifiFrameworkUxUtils.showToast(ClientModeImpl.this.mContext, 30, (String) null);
                }
            } else if (!ClientModeImpl.this.mIsAutoRoaming) {
                Intent provisionIntent2 = new Intent(SemWifiApBroadcastReceiver.START_PROVISIONING);
                provisionIntent2.putExtra("wState", 2);
                if (ClientModeImpl.this.mContext != null) {
                    ClientModeImpl.this.mContext.sendBroadcast(provisionIntent2);
                    provisionIntent2.setPackage("com.android.settings");
                    ClientModeImpl.this.mContext.sendBroadcast(provisionIntent2);
                }
            }
            if (ClientModeImpl.this.mSemLocationManager != null) {
                if (ClientModeImpl.DBG) {
                    Log.d(ClientModeImpl.TAG, "Remove location updates");
                }
                if (ClientModeImpl.this.mLocationRequestNetworkId == -1) {
                    ClientModeImpl.this.mSemLocationManager.removeLocationUpdates(ClientModeImpl.this.mSemLocationListener);
                }
                if (ClientModeImpl.this.mLocationPendingIntent != null) {
                    ClientModeImpl.this.mSemLocationManager.removePassiveLocation(ClientModeImpl.this.mLocationPendingIntent);
                }
            }
            ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
            clientModeImpl2.report(1, ReportUtil.getReportDataForDisconnectTranstion(clientModeImpl2.mScreenOn, 2, ClientModeImpl.this.mWifiAdpsEnabled.get() ? 1 : 0));
            if (WifiRoamingAssistant.getInstance() != null) {
                WifiRoamingAssistant.getInstance().updateRcl((String) null, 0, false);
            }
            ClientModeImpl.this.clearEDMWiFiPolicy();
        }
    }

    class DisconnectingState extends State {
        DisconnectingState() {
        }

        public void enter() {
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                ClientModeImpl.this.logd(" Enter DisconnectingState State screenOn=" + ClientModeImpl.this.mScreenOn);
            }
            ClientModeImpl.this.mDisconnectingWatchdogCount++;
            ClientModeImpl.this.logd("Start Disconnecting Watchdog " + ClientModeImpl.this.mDisconnectingWatchdogCount);
            ClientModeImpl clientModeImpl = ClientModeImpl.this;
            clientModeImpl.sendMessageDelayed(clientModeImpl.obtainMessage(ClientModeImpl.CMD_DISCONNECTING_WATCHDOG_TIMER, clientModeImpl.mDisconnectingWatchdogCount, 0), RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        }

        public boolean processMessage(Message message) {
            boolean handleStatus = true;
            int i = message.what;
            if (i != ClientModeImpl.CMD_DISCONNECT) {
                if (i != ClientModeImpl.CMD_DISCONNECTING_WATCHDOG_TIMER) {
                    if (i != 147462) {
                        handleStatus = false;
                    } else {
                        ClientModeImpl.this.deferMessage(message);
                        ClientModeImpl.this.handleNetworkDisconnect();
                        ClientModeImpl clientModeImpl = ClientModeImpl.this;
                        clientModeImpl.transitionTo(clientModeImpl.mDisconnectedState);
                    }
                } else if (ClientModeImpl.this.mDisconnectingWatchdogCount == message.arg1) {
                    if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                        ClientModeImpl.this.log("disconnecting watchdog! -> disconnect");
                    }
                    ClientModeImpl.this.handleNetworkDisconnect();
                    ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
                    clientModeImpl2.transitionTo(clientModeImpl2.mDisconnectedState);
                }
            } else if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                ClientModeImpl.this.log("Ignore CMD_DISCONNECT when already disconnecting.");
            }
            if (handleStatus) {
                ClientModeImpl.this.logStateAndMessage(message, this);
            }
            return handleStatus;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.i(ClientModeImpl.TAG, "disconnectedstate enter");
            ClientModeImpl clientModeImpl = ClientModeImpl.this;
            clientModeImpl.mIsRoamNetwork = false;
            clientModeImpl.setRoamTriggered(false);
            if (ClientModeImpl.this.mTemporarilyDisconnectWifi) {
                boolean unused = ClientModeImpl.this.p2pSendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                return;
            }
            if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                ClientModeImpl clientModeImpl2 = ClientModeImpl.this;
                clientModeImpl2.logd(" Enter DisconnectedState screenOn=" + ClientModeImpl.this.mScreenOn);
            }
            ClientModeImpl.this.removeMessages(ClientModeImpl.CMD_THREE_TIMES_SCAN_IN_IDLE);
            ClientModeImpl.this.sendMessage(ClientModeImpl.CMD_THREE_TIMES_SCAN_IN_IDLE);
            ClientModeImpl.this.mWifiConnectivityManager.handleConnectionStateChanged(2);
            if (ClientModeImpl.this.mIsAutoRoaming && OpBrandingLoader.Vendor.VZW == ClientModeImpl.mOpBranding) {
                if (!ClientModeImpl.this.mWifiInjector.getSemWifiApChipInfo().supportWifiSharing() || !ClientModeImpl.this.isWifiSharingProvisioning()) {
                    SemWifiFrameworkUxUtils.showToast(ClientModeImpl.this.mContext, 30, (String) null);
                } else {
                    ClientModeImpl clientModeImpl3 = ClientModeImpl.this;
                    clientModeImpl3.logd("Wifi got in DisconnectedState, Send provisioning intent mIsAutoRoaming" + ClientModeImpl.this.mIsAutoRoaming);
                    Intent provisionIntent = new Intent(SemWifiApBroadcastReceiver.START_PROVISIONING);
                    provisionIntent.putExtra("wState", 2);
                    if (ClientModeImpl.this.mContext != null) {
                        ClientModeImpl.this.mContext.sendBroadcast(provisionIntent);
                        provisionIntent.setPackage("com.android.settings");
                        ClientModeImpl.this.mContext.sendBroadcast(provisionIntent);
                    }
                }
            }
            boolean unused2 = ClientModeImpl.this.mIsAutoRoaming = false;
            ClientModeImpl.this.mWifiScoreReport.resetNetworkAgent();
        }

        public boolean processMessage(Message message) {
            boolean handleStatus = true;
            boolean z = false;
            switch (message.what) {
                case ClientModeImpl.CMD_DISCONNECT /*131145*/:
                    ClientModeImpl.this.mWifiMetrics.logStaEvent(15, 2);
                    ClientModeImpl.this.mWifiNative.disconnect(ClientModeImpl.this.mInterfaceName);
                    break;
                case ClientModeImpl.CMD_RECONNECT /*131146*/:
                case ClientModeImpl.CMD_REASSOCIATE /*131147*/:
                    if (!ClientModeImpl.this.mTemporarilyDisconnectWifi) {
                        handleStatus = false;
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_SCREEN_STATE_CHANGED /*131167*/:
                    if (message.arg1 != 0) {
                        z = true;
                    }
                    boolean screenOn = z;
                    if (ClientModeImpl.this.mScreenOn != screenOn) {
                        ClientModeImpl.this.handleScreenStateChanged(screenOn);
                        break;
                    }
                    break;
                case ClientModeImpl.CMD_THREE_TIMES_SCAN_IN_IDLE /*131381*/:
                    if (ClientModeImpl.access$22708(ClientModeImpl.this) < 2 && ClientModeImpl.this.mScreenOn) {
                        Log.e(ClientModeImpl.TAG, "DisconnectedState  CMD_THREE_TIMES_SCAN_IN_IDLE && mScreenOn");
                        ClientModeImpl.this.mScanRequestProxy.startScan(1000, ClientModeImpl.this.mContext.getOpPackageName());
                        ClientModeImpl.this.sendMessageDelayed(ClientModeImpl.CMD_THREE_TIMES_SCAN_IN_IDLE, 8000);
                        break;
                    }
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                    ClientModeImpl.this.mP2pConnected.set(((NetworkInfo) message.obj).isConnected());
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (message.arg2 == 15) {
                        ClientModeImpl.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(ClientModeImpl.this.getTargetSsid(), message.obj == null ? ClientModeImpl.this.mTargetRoamBSSID : (String) message.obj, 2);
                    }
                    ClientModeImpl clientModeImpl = ClientModeImpl.this;
                    int access$7000 = clientModeImpl.mTargetNetworkId;
                    String str = (String) message.obj;
                    if (message.arg1 != 0) {
                        z = true;
                    }
                    clientModeImpl.checkAndUpdateUnstableAp(access$7000, str, z, message.arg2);
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (ClientModeImpl.this.mVerboseLoggingEnabled) {
                        ClientModeImpl.this.logd("SUPPLICANT_STATE_CHANGE_EVENT state=" + stateChangeResult.state + " -> state= " + WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    }
                    if (SupplicantState.isConnecting(stateChangeResult.state)) {
                        WifiConfiguration config = ClientModeImpl.this.mWifiConfigManager.getConfiguredNetwork(stateChangeResult.networkId);
                        ClientModeImpl.this.mWifiInfo.setFQDN((String) null);
                        ClientModeImpl.this.mWifiInfo.setOsuAp(false);
                        ClientModeImpl.this.mWifiInfo.setProviderFriendlyName((String) null);
                        if (config != null && (config.isPasspoint() || config.osu)) {
                            if (config.isPasspoint()) {
                                ClientModeImpl.this.mWifiInfo.setFQDN(config.FQDN);
                            } else {
                                ClientModeImpl.this.mWifiInfo.setOsuAp(true);
                            }
                            ClientModeImpl.this.mWifiInfo.setProviderFriendlyName(config.providerFriendlyName);
                        }
                    }
                    boolean unused = ClientModeImpl.this.setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    handleStatus = false;
                    break;
                default:
                    handleStatus = false;
                    break;
            }
            if (handleStatus) {
                ClientModeImpl.this.logStateAndMessage(message, this);
            }
            return handleStatus;
        }

        public void exit() {
            ClientModeImpl.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
        }
    }

    /* access modifiers changed from: private */
    public void replyToMessage(Message msg, int what) {
        if (msg.replyTo != null) {
            this.mReplyChannel.replyToMessage(msg, obtainMessageWithWhatAndArg2(msg, what));
        }
    }

    /* access modifiers changed from: private */
    public void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
            dstMsg.arg1 = arg1;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    /* access modifiers changed from: private */
    public void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
            dstMsg.obj = obj;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private Message obtainMessageWithWhatAndArg2(Message srcMsg, int what) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    /* access modifiers changed from: private */
    public void broadcastWifiCredentialChanged(int wifiCredentialEventType, WifiConfiguration config) {
        if (config != null && config.preSharedKey != null) {
            Intent intent = new Intent("android.net.wifi.WIFI_CREDENTIAL_CHANGED");
            intent.putExtra("ssid", config.SSID);
            intent.putExtra("et", wifiCredentialEventType);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.RECEIVE_WIFI_CREDENTIAL_CHANGE");
        }
    }

    /* access modifiers changed from: package-private */
    public void handleGsmAuthRequest(TelephonyUtil.SimAuthRequestData requestData) {
        WifiConfiguration wifiConfiguration = this.mTargetWifiConfiguration;
        if (wifiConfiguration == null || wifiConfiguration.networkId == requestData.networkId) {
            logd("id matches mTargetWifiConfiguration");
            String response = TelephonyUtil.getGsmSimAuthResponse(requestData.data, getTelephonyManager());
            if (response == null && (response = TelephonyUtil.getGsmSimpleSimAuthResponse(requestData.data, getTelephonyManager())) == null) {
                response = TelephonyUtil.getGsmSimpleSimNoLengthAuthResponse(requestData.data, getTelephonyManager());
            }
            if (response == null || response.length() == 0) {
                this.mWifiNative.simAuthFailedResponse(this.mInterfaceName, requestData.networkId);
                return;
            }
            logv("Supplicant Response -" + response);
            this.mWifiNative.simAuthResponse(this.mInterfaceName, requestData.networkId, WifiNative.SIM_AUTH_RESP_TYPE_GSM_AUTH, response);
            return;
        }
        logd("id does not match mTargetWifiConfiguration");
    }

    /* access modifiers changed from: package-private */
    public void handle3GAuthRequest(TelephonyUtil.SimAuthRequestData requestData) {
        WifiConfiguration wifiConfiguration = this.mTargetWifiConfiguration;
        if (wifiConfiguration == null || wifiConfiguration.networkId == requestData.networkId) {
            logd("id matches mTargetWifiConfiguration");
            TelephonyUtil.SimAuthResponseData response = TelephonyUtil.get3GAuthResponse(requestData, getTelephonyManager());
            if (response != null) {
                this.mWifiNative.simAuthResponse(this.mInterfaceName, requestData.networkId, response.type, response.response);
            } else {
                this.mWifiNative.umtsAuthFailedResponse(this.mInterfaceName, requestData.networkId);
            }
        } else {
            logd("id does not match mTargetWifiConfiguration");
        }
    }

    public void startConnectToNetwork(int networkId, int uid, String bssid) {
        sendMessage(CMD_START_CONNECT, networkId, uid, bssid);
    }

    public void startRoamToNetwork(int networkId, ScanResult scanResult) {
        sendMessage(CMD_START_ROAM, networkId, 0, scanResult);
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        sendMessage(CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER, enabled);
    }

    static boolean unexpectedDisconnectedReason(int reason) {
        return reason == 2 || reason == 6 || reason == 7 || reason == 8 || reason == 9 || reason == 14 || reason == 15 || reason == 16 || reason == 18 || reason == 19 || reason == 23 || reason == 34;
    }

    public void updateWifiMetrics() {
        this.mWifiMetrics.updateSavedNetworks(this.mWifiConfigManager.getSavedNetworks(1010));
        this.mPasspointManager.updateMetrics();
    }

    /* access modifiers changed from: private */
    public boolean deleteNetworkConfigAndSendReply(Message message, boolean calledFromForget) {
        boolean success = this.mWifiConfigManager.removeNetwork(message.arg1, message.sendingUid, message.arg2);
        if (!success) {
            loge("Failed to remove network");
        }
        if (calledFromForget) {
            if (success) {
                replyToMessage(message, 151558);
                broadcastWifiCredentialChanged(1, (WifiConfiguration) message.obj);
                return true;
            }
            replyToMessage(message, 151557, 0);
            return false;
        } else if (success) {
            replyToMessage(message, message.what, 1);
            return true;
        } else {
            this.mMessageHandlingStatus = -2;
            replyToMessage(message, message.what, -1);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public NetworkUpdateResult saveNetworkConfigAndSendReply(Message message) {
        WifiConfiguration config = (WifiConfiguration) message.obj;
        if (config == null) {
            loge("SAVE_NETWORK with null configuration " + this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + getCurrentState().getName());
            this.mMessageHandlingStatus = -2;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        }
        config.priority = this.mWifiConfigManager.increaseAndGetPriority();
        this.mWifiConfigManager.updateBssidWhitelist(config, this.mScanRequestProxy.getScanResults());
        NetworkUpdateResult result = this.mWifiConfigManager.addOrUpdateNetwork(config, message.sendingUid);
        if (!result.isSuccess()) {
            loge("SAVE_NETWORK adding/updating config=" + config + " failed");
            this.mMessageHandlingStatus = -2;
            replyToMessage(message, 151560, 0);
            return result;
        } else if (!this.mWifiConfigManager.enableNetwork(result.getNetworkId(), false, message.sendingUid)) {
            loge("SAVE_NETWORK enabling config=" + config + " failed");
            this.mMessageHandlingStatus = -2;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        } else {
            broadcastWifiCredentialChanged(0, config);
            replyToMessage(message, 151561);
            return result;
        }
    }

    private static String getLinkPropertiesSummary(LinkProperties lp) {
        List<String> attributes = new ArrayList<>(6);
        if (lp.hasIPv4Address()) {
            attributes.add("v4");
        }
        if (lp.hasIPv4DefaultRoute()) {
            attributes.add("v4r");
        }
        if (lp.hasIPv4DnsServer()) {
            attributes.add("v4dns");
        }
        if (lp.hasGlobalIPv6Address()) {
            attributes.add("v6");
        }
        if (lp.hasIPv6DefaultRoute()) {
            attributes.add("v6r");
        }
        if (lp.hasIPv6DnsServer()) {
            attributes.add("v6dns");
        }
        return TextUtils.join(" ", attributes);
    }

    /* access modifiers changed from: private */
    public String getTargetSsid() {
        WifiConfiguration currentConfig = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (currentConfig != null) {
            return currentConfig.SSID;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public boolean p2pSendMessage(int what) {
        AsyncChannel asyncChannel = this.mWifiP2pChannel;
        if (asyncChannel == null) {
            return false;
        }
        asyncChannel.sendMessage(what);
        return true;
    }

    private boolean p2pSendMessage(int what, int arg1) {
        AsyncChannel asyncChannel = this.mWifiP2pChannel;
        if (asyncChannel == null) {
            return false;
        }
        asyncChannel.sendMessage(what, arg1);
        return true;
    }

    /* access modifiers changed from: private */
    public boolean hasConnectionRequests() {
        return this.mNetworkFactory.hasConnectionRequests() || this.mUntrustedNetworkFactory.hasConnectionRequests();
    }

    public boolean getIpReachabilityDisconnectEnabled() {
        return this.mIpReachabilityDisconnectEnabled;
    }

    public void setIpReachabilityDisconnectEnabled(boolean enabled) {
        this.mIpReachabilityDisconnectEnabled = enabled;
    }

    public boolean syncInitialize(AsyncChannel channel) {
        boolean result = false;
        if (this.mIsShutdown) {
            Log.d(TAG, "ignore processing beause shutdown is held");
            return false;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_INITIALIZE);
        if (resultMsg.arg1 != -1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    /* access modifiers changed from: package-private */
    public void setAutoConnectCarrierApEnabled(boolean enabled) {
        sendMessage(CMD_AUTO_CONNECT_CARRIER_AP_ENABLED, enabled, 0);
    }

    public void addNetworkRequestMatchCallback(IBinder binder, INetworkRequestMatchCallback callback, int callbackIdentifier) {
        this.mNetworkFactory.addCallback(binder, callback, callbackIdentifier);
    }

    public void removeNetworkRequestMatchCallback(int callbackIdentifier) {
        this.mNetworkFactory.removeCallback(callbackIdentifier);
    }

    public void removeNetworkRequestUserApprovedAccessPointsForApp(String packageName) {
        this.mNetworkFactory.removeUserApprovedAccessPointsForApp(packageName);
    }

    public void clearNetworkRequestUserApprovedAccessPoints() {
        this.mNetworkFactory.clear();
    }

    public String getFactoryMacAddress() {
        MacAddress macAddress = this.mWifiNative.getFactoryMacAddress(this.mInterfaceName);
        if (macAddress != null) {
            return macAddress.toString();
        }
        if (!this.mConnectedMacRandomzationSupported) {
            return this.mWifiNative.getMacAddress(this.mInterfaceName);
        }
        return null;
    }

    public void setDeviceMobilityState(int state) {
        this.mWifiConnectivityManager.setDeviceMobilityState(state);
    }

    public void updateWifiUsabilityScore(int seqNum, int score, int predictionHorizonSec) {
        this.mWifiMetrics.incrementWifiUsabilityScoreCount(seqNum, score, predictionHorizonSec);
    }

    @VisibleForTesting
    public void probeLink(WifiNative.SendMgmtFrameCallback callback, int mcs) {
        this.mWifiNative.probeLink(this.mInterfaceName, MacAddress.fromString(this.mWifiInfo.getBSSID()), callback, mcs);
    }

    /* access modifiers changed from: private */
    public void updateWifiInfoForVendors(ScanResult scanResult) {
        WifiConfiguration config;
        ScanDetailCache scanDetailCache;
        ScanDetail scanDetail;
        String capabilities;
        if (mOpBranding == OpBrandingLoader.Vendor.KTT && (capabilities = scanResult.capabilities) != null) {
            if (!capabilities.contains("[VSI]") || !capabilities.contains("[VHT]")) {
                if (DBG) {
                    Log.d(TAG, "setGigaAp: false, bssid: " + scanResult.BSSID + ", capa: " + capabilities);
                }
                this.mWifiInfo.setGigaAp(false);
            } else {
                if (DBG) {
                    Log.d(TAG, "setGigaAp: true");
                }
                this.mWifiInfo.setGigaAp(true);
            }
        }
        if (this.mIsPasspointEnabled && mOpBranding == OpBrandingLoader.Vendor.SKT && this.mLastNetworkId != -1 && !TextUtils.isEmpty(scanResult.BSSID) && (config = this.mWifiConfigManager.getConfiguredNetwork(this.mLastNetworkId)) != null && config.semIsVendorSpecificSsid && config.isPasspoint() && !config.isHomeProviderNetwork && (scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(this.mLastNetworkId)) != null && (scanDetail = scanDetailCache.getScanDetail(scanResult.BSSID)) != null && scanDetail.getNetworkDetail().isInterworking()) {
            Map<Constants.ANQPElementType, ANQPElement> anqpElements = this.mPasspointManager.getANQPElements(scanResult);
            if (anqpElements == null || anqpElements.size() <= 0) {
                Log.d(TAG, "There is no anqpElements, so send anqp query to " + scanResult.SSID);
                this.mPasspointManager.forceRequestAnqp(scanResult);
                return;
            }
            HSWanMetricsElement hSWanMetricsElement = (HSWanMetricsElement) anqpElements.get(Constants.ANQPElementType.HSWANMetrics);
            VenueNameElement vne = (VenueNameElement) anqpElements.get(Constants.ANQPElementType.ANQPVenueName);
            if (vne != null && !vne.getNames().isEmpty()) {
                String venueName = vne.getNames().get(0).getText();
                Log.i(TAG, "updateVenueNameInWifiInfo: venueName is " + venueName);
                this.mWifiInfo.setVenueName(venueName);
            }
        }
    }

    public boolean isWifiOnly() {
        if (this.mIsWifiOnly == -1) {
            checkAndSetConnectivityInstance();
            ConnectivityManager connectivityManager = this.mCm;
            if (connectivityManager == null || !connectivityManager.isNetworkSupported(0)) {
                this.mIsWifiOnly = 1;
            } else {
                this.mIsWifiOnly = 0;
            }
        }
        if (this.mIsWifiOnly == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void setShutdown() {
        this.mIsShutdown = true;
        this.mWifiConfigManager.semStopToSaveStore();
        if (getCurrentState() == this.mConnectedState) {
            setAnalyticsUserDisconnectReason(WifiNative.f20xe9115267);
        }
        if (this.mDelayDisconnect.isEnabled()) {
            this.mDelayDisconnect.setEnable(false, 0);
        }
    }

    /* access modifiers changed from: private */
    public void updateAdpsState() {
        this.mWifiNative.setAdps(this.mInterfaceName, this.mWifiAdpsEnabled.get());
        this.mBigDataManager.addOrUpdateValue(13, this.mWifiAdpsEnabled.get() ? 1 : 0);
    }

    /* Debug info: failed to restart local var, previous not found, register: 13 */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x01d5, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:0x0284, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:195:0x02bd, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:212:0x02ee, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x008b, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x00cf, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x0123, code lost:
        return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int callSECApi(android.os.Message r14) {
        /*
            r13 = this;
            monitor-enter(r13)
            boolean r0 = r13.mVerboseLoggingEnabled     // Catch:{ all -> 0x02ef }
            if (r0 == 0) goto L_0x001f
            java.lang.String r0 = r13.getName()     // Catch:{ all -> 0x02ef }
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x02ef }
            r1.<init>()     // Catch:{ all -> 0x02ef }
            java.lang.String r2 = "callSECApi what="
            r1.append(r2)     // Catch:{ all -> 0x02ef }
            int r2 = r14.what     // Catch:{ all -> 0x02ef }
            r1.append(r2)     // Catch:{ all -> 0x02ef }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x02ef }
            android.util.Log.i(r0, r1)     // Catch:{ all -> 0x02ef }
        L_0x001f:
            r0 = -1
            int r1 = r14.what     // Catch:{ all -> 0x02ef }
            r2 = 24
            r3 = 1
            r4 = 0
            if (r1 == r2) goto L_0x02be
            r2 = 34
            if (r1 == r2) goto L_0x02b2
            r2 = 77
            if (r1 == r2) goto L_0x02a4
            r2 = 79
            if (r1 == r2) goto L_0x0285
            r2 = 222(0xde, float:3.11E-43)
            r5 = -1
            if (r1 == r2) goto L_0x01e4
            r2 = 267(0x10b, float:3.74E-43)
            if (r1 == r2) goto L_0x01d6
            r2 = 283(0x11b, float:3.97E-43)
            r6 = 4
            if (r1 == r2) goto L_0x0132
            r2 = 330(0x14a, float:4.62E-43)
            if (r1 == r2) goto L_0x0124
            r2 = 407(0x197, float:5.7E-43)
            if (r1 == r2) goto L_0x010b
            r2 = 500(0x1f4, float:7.0E-43)
            if (r1 == r2) goto L_0x00e7
            r2 = 81
            if (r1 == r2) goto L_0x00d7
            r2 = 82
            if (r1 == r2) goto L_0x00d0
            r2 = 280(0x118, float:3.92E-43)
            if (r1 == r2) goto L_0x008c
            r2 = 281(0x119, float:3.94E-43)
            if (r1 == r2) goto L_0x006b
            boolean r1 = r13.mVerboseLoggingEnabled     // Catch:{ all -> 0x02ef }
            if (r1 == 0) goto L_0x02bc
            java.lang.String r1 = "WifiClientModeImpl"
            java.lang.String r2 = "ignore message : not implementation yet"
            android.util.Log.e(r1, r2)     // Catch:{ all -> 0x02ef }
            goto L_0x02bc
        L_0x006b:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            if (r1 != 0) goto L_0x0073
            monitor-exit(r13)
            return r5
        L_0x0073:
            java.lang.String r2 = "enable"
            int r2 = r1.getInt(r2)     // Catch:{ all -> 0x02ef }
            if (r2 != r3) goto L_0x0083
            com.android.server.wifi.WifiNative r5 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r6 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            r5.setMaxDtimInSuspend(r6, r3)     // Catch:{ all -> 0x02ef }
            goto L_0x008a
        L_0x0083:
            com.android.server.wifi.WifiNative r3 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            r3.setMaxDtimInSuspend(r5, r4)     // Catch:{ all -> 0x02ef }
        L_0x008a:
            monitor-exit(r13)
            return r4
        L_0x008c:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            if (r1 != 0) goto L_0x0094
            monitor-exit(r13)
            return r5
        L_0x0094:
            java.lang.String r2 = "enable"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x00b0
            java.lang.String r2 = "enable"
            int r2 = r1.getInt(r2)     // Catch:{ all -> 0x02ef }
            if (r2 != r3) goto L_0x00aa
            com.samsung.android.server.wifi.SemSarManager r5 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            r5.enable_WiFi_PowerBackoff(r3)     // Catch:{ all -> 0x02ef }
            goto L_0x00cd
        L_0x00aa:
            com.samsung.android.server.wifi.SemSarManager r3 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            r3.enable_WiFi_PowerBackoff(r4)     // Catch:{ all -> 0x02ef }
            goto L_0x00cd
        L_0x00b0:
            java.lang.String r2 = "enable5G"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x00cd
            java.lang.String r2 = "enable5G"
            int r2 = r1.getInt(r2)     // Catch:{ all -> 0x02ef }
            if (r2 != r3) goto L_0x00c6
            com.samsung.android.server.wifi.SemSarManager r3 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            r3.set5GSarBackOff(r6)     // Catch:{ all -> 0x02ef }
            goto L_0x00ce
        L_0x00c6:
            com.samsung.android.server.wifi.SemSarManager r3 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            r5 = 3
            r3.set5GSarBackOff(r5)     // Catch:{ all -> 0x02ef }
            goto L_0x00ce
        L_0x00cd:
        L_0x00ce:
            monitor-exit(r13)
            return r4
        L_0x00d0:
            boolean r1 = r13.isRoaming()     // Catch:{ all -> 0x02ef }
            r0 = r1
            goto L_0x02bc
        L_0x00d7:
            com.samsung.android.server.wifi.WifiDelayDisconnect r1 = r13.mDelayDisconnect     // Catch:{ all -> 0x02ef }
            int r2 = r14.arg1     // Catch:{ all -> 0x02ef }
            if (r2 != r3) goto L_0x00de
            goto L_0x00df
        L_0x00de:
            r3 = r4
        L_0x00df:
            int r2 = r14.arg2     // Catch:{ all -> 0x02ef }
            long r5 = (long) r2     // Catch:{ all -> 0x02ef }
            r1.setEnable(r3, r5)     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r4
        L_0x00e7:
            com.samsung.android.server.wifi.WifiB2BConfigurationPolicy r1 = r13.mWifiB2bConfigPolicy     // Catch:{ all -> 0x02ef }
            java.lang.Object r2 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r2 = (android.os.Bundle) r2     // Catch:{ all -> 0x02ef }
            r1.setWiFiConfiguration(r2)     // Catch:{ all -> 0x02ef }
            r13.updateEDMWiFiPolicy()     // Catch:{ all -> 0x02ef }
            java.lang.String r1 = "WifiClientModeImpl"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x02ef }
            r2.<init>()     // Catch:{ all -> 0x02ef }
            java.lang.String r3 = "SEC_COMMAND_ID_SET_EDM_WIFI_POLICY: "
            r2.append(r3)     // Catch:{ all -> 0x02ef }
            r2.append(r0)     // Catch:{ all -> 0x02ef }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x02ef }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x02ef }
            goto L_0x02bc
        L_0x010b:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            if (r1 != 0) goto L_0x0113
            monitor-exit(r13)
            return r5
        L_0x0113:
            java.lang.String r2 = "enable"
            int r2 = r1.getInt(r2, r5)     // Catch:{ all -> 0x02ef }
            if (r2 == r5) goto L_0x0122
            com.android.server.wifi.WifiNative r3 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            r3.setLatencyCritical(r5, r2)     // Catch:{ all -> 0x02ef }
        L_0x0122:
            monitor-exit(r13)
            return r4
        L_0x0124:
            r1 = 131286(0x200d6, float:1.83971E-40)
            java.lang.Object r2 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Message r1 = r13.obtainMessage(r1, r4, r4, r2)     // Catch:{ all -> 0x02ef }
            r13.sendMessage(r1)     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r4
        L_0x0132:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            if (r1 != 0) goto L_0x013a
            monitor-exit(r13)
            return r5
        L_0x013a:
            java.lang.String r2 = "type"
            int r2 = r1.getInt(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x01ac
            if (r2 == r3) goto L_0x018f
            r3 = 2
            if (r2 == r3) goto L_0x0182
            if (r2 == r6) goto L_0x0175
            r3 = 16
            if (r2 == r3) goto L_0x0168
            r3 = 32
            if (r2 == r3) goto L_0x015b
            r3 = 64
            if (r2 == r3) goto L_0x0157
            goto L_0x01d4
        L_0x0157:
            int r3 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r3
        L_0x015b:
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "WLAN_ADVANCED_DEBUG_ELE_DEBUG changed to true"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            int r5 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            r3 = r3 | r5
            mWlanAdvancedDebugState = r3     // Catch:{ all -> 0x02ef }
            goto L_0x01d4
        L_0x0168:
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "WLAN_ADVANCED_DEBUG_UNWANTED_PANIC changed to true"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            int r5 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            r3 = r3 | r5
            mWlanAdvancedDebugState = r3     // Catch:{ all -> 0x02ef }
            goto L_0x01d4
        L_0x0175:
            java.lang.String r3 = "WifiClientModeImpl"
            java.lang.String r5 = "WLAN_ADVANCED_DEBUG_DISC changed to true"
            android.util.Log.i(r3, r5)     // Catch:{ all -> 0x02ef }
            int r3 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            r3 = r3 | r6
            mWlanAdvancedDebugState = r3     // Catch:{ all -> 0x02ef }
            goto L_0x01d4
        L_0x0182:
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "WLAN_ADVANCED_DEBUG_UNWANTED changed to true"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            int r5 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            r3 = r3 | r5
            mWlanAdvancedDebugState = r3     // Catch:{ all -> 0x02ef }
            goto L_0x01d4
        L_0x018f:
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "pktlog filter removed, size changed to 1280"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            int r5 = mWlanAdvancedDebugState     // Catch:{ all -> 0x02ef }
            r3 = r3 | r5
            mWlanAdvancedDebugState = r3     // Catch:{ all -> 0x02ef }
            com.android.server.wifi.WifiNative r3 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            r3.enablePktlogFilter(r5, r4)     // Catch:{ all -> 0x02ef }
            com.android.server.wifi.WifiNative r3 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            java.lang.String r6 = "1920"
            r3.changePktlogSize(r5, r6)     // Catch:{ all -> 0x02ef }
            goto L_0x01d4
        L_0x01ac:
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "pktlog filter enabled again, size changed to 320 default value again"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "WLAN_ADVANCED_DEBUG_UNWANTED changed to false"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = "WifiClientModeImpl"
            java.lang.String r6 = "WLAN_ADVANCED_DEBUG_DISC changed to false"
            android.util.Log.i(r5, r6)     // Catch:{ all -> 0x02ef }
            com.android.server.wifi.WifiNative r5 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r6 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            r5.enablePktlogFilter(r6, r3)     // Catch:{ all -> 0x02ef }
            com.android.server.wifi.WifiNative r3 = r13.mWifiNative     // Catch:{ all -> 0x02ef }
            java.lang.String r5 = r13.mInterfaceName     // Catch:{ all -> 0x02ef }
            java.lang.String r6 = "320"
            r3.changePktlogSize(r5, r6)     // Catch:{ all -> 0x02ef }
            mWlanAdvancedDebugState = r4     // Catch:{ all -> 0x02ef }
        L_0x01d4:
            monitor-exit(r13)
            return r4
        L_0x01d6:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            java.lang.String r2 = "enable"
            boolean r2 = r1.getBoolean(r2)     // Catch:{ all -> 0x02ef }
            r13.mBlockFccChannelCmd = r2     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r4
        L_0x01e4:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            java.lang.String r2 = "pkgNames"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x022c
            java.lang.String r2 = "scanTypes"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x022c
            java.lang.String r2 = "scanDelays"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x022c
            java.lang.String r2 = "pkgNames"
            java.lang.String[] r2 = r1.getStringArray(r2)     // Catch:{ all -> 0x02ef }
            java.lang.String r6 = "scanTypes"
            int[] r6 = r1.getIntArray(r6)     // Catch:{ all -> 0x02ef }
            java.lang.String r7 = "scanDelays"
            int[] r7 = r1.getIntArray(r7)     // Catch:{ all -> 0x02ef }
            int r8 = r2.length     // Catch:{ all -> 0x02ef }
            int r9 = r6.length     // Catch:{ all -> 0x02ef }
            if (r8 != r9) goto L_0x0264
            int r8 = r6.length     // Catch:{ all -> 0x02ef }
            int r9 = r7.length     // Catch:{ all -> 0x02ef }
            if (r8 != r9) goto L_0x0264
            r8 = r4
        L_0x021b:
            int r9 = r2.length     // Catch:{ all -> 0x02ef }
            if (r8 >= r9) goto L_0x0264
            com.android.server.wifi.ScanRequestProxy r9 = r13.mScanRequestProxy     // Catch:{ all -> 0x02ef }
            r10 = r2[r8]     // Catch:{ all -> 0x02ef }
            r11 = r6[r8]     // Catch:{ all -> 0x02ef }
            r12 = r7[r8]     // Catch:{ all -> 0x02ef }
            r9.semSetCustomScanPolicy(r10, r11, r12)     // Catch:{ all -> 0x02ef }
            int r8 = r8 + 1
            goto L_0x021b
        L_0x022c:
            java.lang.String r2 = "pkgName"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x0264
            java.lang.String r2 = "scanType"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x0264
            java.lang.String r2 = "scanDelay"
            boolean r2 = r1.containsKey(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x0265
            java.lang.String r2 = "pkgName"
            java.lang.String r2 = r1.getString(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x0265
            int r6 = r2.length()     // Catch:{ all -> 0x02ef }
            if (r6 <= 0) goto L_0x0265
            java.lang.String r6 = "scanType"
            int r6 = r1.getInt(r6, r4)     // Catch:{ all -> 0x02ef }
            java.lang.String r7 = "scanDelay"
            int r7 = r1.getInt(r7, r4)     // Catch:{ all -> 0x02ef }
            com.android.server.wifi.ScanRequestProxy r8 = r13.mScanRequestProxy     // Catch:{ all -> 0x02ef }
            r8.semSetCustomScanPolicy(r2, r6, r7)     // Catch:{ all -> 0x02ef }
            goto L_0x0265
        L_0x0264:
        L_0x0265:
            java.lang.String r2 = "duration"
            int r2 = r1.getInt(r2, r5)     // Catch:{ all -> 0x02ef }
            if (r2 == r5) goto L_0x0272
            com.android.server.wifi.ScanRequestProxy r6 = r13.mScanRequestProxy     // Catch:{ all -> 0x02ef }
            r6.semSetMaxDurationForCachedScan(r2)     // Catch:{ all -> 0x02ef }
        L_0x0272:
            java.lang.String r6 = "useSMD"
            int r6 = r1.getInt(r6, r5)     // Catch:{ all -> 0x02ef }
            if (r6 == r5) goto L_0x0283
            com.android.server.wifi.ScanRequestProxy r5 = r13.mScanRequestProxy     // Catch:{ all -> 0x02ef }
            if (r6 != r3) goto L_0x027f
            goto L_0x0280
        L_0x027f:
            r3 = r4
        L_0x0280:
            r5.semUseSMDForCachedScan(r3)     // Catch:{ all -> 0x02ef }
        L_0x0283:
            monitor-exit(r13)
            return r4
        L_0x0285:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            if (r1 != 0) goto L_0x0293
            com.samsung.android.server.wifi.SemSarManager r2 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            boolean r2 = r2.isGripSensorEnabled()     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r2
        L_0x0293:
            java.lang.String r2 = "enable"
            int r2 = r1.getInt(r2)     // Catch:{ all -> 0x02ef }
            com.samsung.android.server.wifi.SemSarManager r5 = r13.mSemSarManager     // Catch:{ all -> 0x02ef }
            if (r2 != r3) goto L_0x029e
            goto L_0x029f
        L_0x029e:
            r3 = r4
        L_0x029f:
            r5.enableGripSensorMonitor(r3)     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r4
        L_0x02a4:
            r1 = 131576(0x201f8, float:1.84377E-40)
            java.lang.Object r2 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Message r1 = r13.obtainMessage(r1, r4, r4, r2)     // Catch:{ all -> 0x02ef }
            r13.sendMessage(r1)     // Catch:{ all -> 0x02ef }
            monitor-exit(r13)
            return r4
        L_0x02b2:
            com.android.server.wifi.WifiConfigManager r1 = r13.mWifiConfigManager     // Catch:{ all -> 0x02ef }
            int r2 = r14.arg1     // Catch:{ all -> 0x02ef }
            boolean r1 = r1.isSkipInternetCheck(r2)     // Catch:{ all -> 0x02ef }
            r0 = r1
        L_0x02bc:
            monitor-exit(r13)
            return r0
        L_0x02be:
            java.lang.Object r1 = r14.obj     // Catch:{ all -> 0x02ef }
            android.os.Bundle r1 = (android.os.Bundle) r1     // Catch:{ all -> 0x02ef }
            java.lang.String r2 = "state"
            boolean r2 = r1.getBoolean(r2)     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x02d0
            int r2 = mRssiPollingScreenOffEnabled     // Catch:{ all -> 0x02ef }
            r2 = r2 | r3
            mRssiPollingScreenOffEnabled = r2     // Catch:{ all -> 0x02ef }
            goto L_0x02d6
        L_0x02d0:
            int r2 = mRssiPollingScreenOffEnabled     // Catch:{ all -> 0x02ef }
            r2 = r2 & -2
            mRssiPollingScreenOffEnabled = r2     // Catch:{ all -> 0x02ef }
        L_0x02d6:
            int r2 = mRssiPollingScreenOffEnabled     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x02e2
            boolean r2 = r13.mEnableRssiPolling     // Catch:{ all -> 0x02ef }
            if (r2 != 0) goto L_0x02ed
            r13.enableRssiPolling(r3)     // Catch:{ all -> 0x02ef }
            goto L_0x02ed
        L_0x02e2:
            boolean r2 = r13.mEnableRssiPolling     // Catch:{ all -> 0x02ef }
            if (r2 == 0) goto L_0x02ed
            boolean r2 = r13.mScreenOn     // Catch:{ all -> 0x02ef }
            if (r2 != 0) goto L_0x02ed
            r13.enableRssiPolling(r4)     // Catch:{ all -> 0x02ef }
        L_0x02ed:
            monitor-exit(r13)
            return r4
        L_0x02ef:
            r14 = move-exception
            monitor-exit(r13)
            throw r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ClientModeImpl.callSECApi(android.os.Message):int");
    }

    /* access modifiers changed from: private */
    public boolean processMessageOnDefaultStateForCallSECApiAsync(Message msg) {
        Message innerMsg = (Message) msg.obj;
        if (innerMsg == null) {
            Log.e(TAG, "CMD_SEC_API_ASYNC, invalid innerMsg");
            return false;
        } else if (innerMsg.what != 242) {
            return false;
        } else {
            this.mWifiConfigManager.removeFilesInDataMiscDirectory();
            this.mWifiConfigManager.removeFilesInDataMiscCeDirectory();
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void checkAndUpdateUnstableAp(int networkId, String bssid, boolean locallyGenerated, int disconnectReason) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "checkAndUpdateUnstableAp netId:" + networkId + ", " + bssid + ", locally:" + locallyGenerated + ", reason:" + disconnectReason);
        }
        if (networkId == -1) {
            Log.d(TAG, "disconnected, can't get network id");
        }
        if (TextUtils.isEmpty(bssid)) {
            Log.d(TAG, "disconnected, can't get bssid");
            return;
        }
        boolean isSameNetwork = true;
        boolean isHotspotAp = false;
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(networkId);
        if (scanDetailCache != null) {
            ScanResult scanResult = scanDetailCache.getScanResult(bssid);
            if (scanResult == null) {
                Log.i(TAG, "disconnected, but not for current network");
                isSameNetwork = false;
            } else if (scanResult.capabilities.contains("[SEC80]")) {
                isHotspotAp = true;
            }
        }
        UnstableApController unstableApController = this.mUnstableApController;
        if (unstableApController != null && !locallyGenerated && isSameNetwork) {
            if (disconnectReason == 77) {
                this.mWifiConfigManager.updateNetworkSelectionStatus(networkId, 20);
            } else {
                if (unstableApController.disconnectWithAuthFail(networkId, bssid, this.mWifiInfo.getRssi(), disconnectReason, getCurrentState() == this.mConnectedState, isHotspotAp)) {
                    report(10, ReportUtil.getReportDataForUnstableAp(networkId, bssid));
                }
            }
        }
        if (isSameNetwork && getCurrentState() != this.mConnectedState) {
            report(13, ReportUtil.getReportDataForL2ConnectFail(networkId, bssid));
        }
    }

    public boolean syncSetRoamTrigger(int roamTrigger) {
        return this.mWifiNative.setRoamTrigger(this.mInterfaceName, roamTrigger);
    }

    public int syncGetRoamTrigger() {
        return this.mWifiNative.getRoamTrigger(this.mInterfaceName);
    }

    public boolean syncSetRoamDelta(int roamDelta) {
        return this.mWifiNative.setRoamDelta(this.mInterfaceName, roamDelta);
    }

    public int syncGetRoamDelta() {
        return this.mWifiNative.getRoamDelta(this.mInterfaceName);
    }

    public boolean syncSetRoamScanPeriod(int roamScanPeriod) {
        return this.mWifiNative.setRoamScanPeriod(this.mInterfaceName, roamScanPeriod);
    }

    public int syncGetRoamScanPeriod() {
        return this.mWifiNative.getRoamScanPeriod(this.mInterfaceName);
    }

    public boolean syncSetRoamBand(int band) {
        return this.mWifiNative.setRoamBand(this.mInterfaceName, band);
    }

    public int syncGetRoamBand() {
        return this.mWifiNative.getRoamBand(this.mInterfaceName);
    }

    public boolean syncSetCountryRev(String countryRev) {
        return this.mWifiNative.setCountryRev(this.mInterfaceName, countryRev);
    }

    public String syncGetCountryRev() {
        return this.mWifiNative.getCountryRev(this.mInterfaceName);
    }

    /* access modifiers changed from: private */
    public boolean processMessageForCallSECApiAsync(Message msg) {
        Message innerMsg = (Message) msg.obj;
        if (innerMsg == null) {
            Log.e(TAG, "CMD_SEC_API_ASYNC, invalid innerMsg");
            return true;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "CMD_SEC_API_ASYNC, inner msg.what:" + innerMsg.what);
        }
        Bundle args = (Bundle) innerMsg.obj;
        int i = innerMsg.what;
        if (i != 1) {
            if (i != 18) {
                if (i != 26) {
                    if (i != 71) {
                        if (i != 74) {
                            if (i != 201) {
                                if (i == 242) {
                                    return false;
                                }
                                if (i == 282 && args != null) {
                                    this.mWifiNative.setAffinityBooster(this.mInterfaceName, args.getInt("enable"));
                                }
                            } else if (args != null) {
                                boolean keepConnection = args.getBoolean("keep_connection", false);
                                WifiConfiguration config = getCurrentWifiConfiguration();
                                if (config != null && keepConnection) {
                                    Log.d(TAG, "SEC_COMMAND_ID_ANS_EXCEPTION_ANSWER, networkId : " + config.networkId + ", keep connection : " + keepConnection);
                                    this.mWifiConfigManager.updateNetworkSelectionStatus(config.networkId, 0);
                                }
                            }
                        }
                    } else if (args != null) {
                        this.mWifiNative.setAffinityBooster(this.mInterfaceName, args.getInt("enable"));
                        Integer netId = Integer.valueOf(args.getInt("netId"));
                        Integer autoReconnect = Integer.valueOf(args.getInt("autoReconnect"));
                        Log.d(TAG, "SEC_COMMAND_ID_SET_AUTO_RECONNECT  autoReconnect: " + autoReconnect);
                        if (ENBLE_WLAN_CONFIG_ANALYTICS && autoReconnect.intValue() == 0 && (netId.intValue() == this.mTargetNetworkId || netId.intValue() == this.mLastNetworkId)) {
                            setAnalyticsUserDisconnectReason(WifiNative.f19x2e98dbf);
                        }
                        this.mWifiConfigManager.setAutoReconnect(netId.intValue(), autoReconnect.intValue());
                    }
                }
                if (args != null) {
                    boolean enable = args.getBoolean("enable");
                    boolean lock = args.getBoolean("lock");
                    setConcurrentEnabled(enable);
                    Log.d(TAG, "SEC_COMMAND_ID_SET_WIFI_XXX_WITH_P2P mConcurrentEnabled " + this.mConcurrentEnabled);
                    if (!enable && lock && this.mP2pSupported && this.mWifiP2pChannel != null) {
                        Message message = new Message();
                        message.what = 139268;
                        this.mWifiP2pChannel.sendMessage(message);
                    }
                }
            } else if (args != null) {
                this.mScanRequestProxy.setScanningEnabled(!args.getBoolean("stop", false), "SEC_COMMAND_ID_STOP_PERIODIC_SCAN");
            }
        } else if (args != null) {
            boolean enable2 = args.getBoolean("enable", false);
            Log.d(TAG, "SEC_COMMAND_ID_AUTO_CONNECT, enable: " + enable2);
            this.mWifiConfigManager.setNetworkAutoConnect(enable2);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public int processMessageForCallSECApi(Message message) {
        ConnectivityManager connectivityManager;
        Message innerMsg = (Message) message.obj;
        if (innerMsg == null) {
            Log.e(TAG, "CMD_SEC_API, invalid innerMsg");
            return -1;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "CMD_SEC_API, inner msg.what:" + innerMsg.what);
        }
        int i = innerMsg.what;
        if (i == 17) {
            Bundle args = (Bundle) innerMsg.obj;
            if (args != null) {
                return this.mWifiNative.setAmpdu(this.mInterfaceName, args.getInt("ampdu"));
            }
            return -1;
        } else if (i == 107) {
            Bundle args2 = (Bundle) innerMsg.obj;
            if (args2 != null) {
                return this.mWifiNative.setRoamScanControl(this.mInterfaceName, args2.getInt("mode"));
            }
            return -1;
        } else if (i == 301) {
            Integer netId = Integer.valueOf(((Bundle) innerMsg.obj).getInt("excluded_networkId"));
            if (DBG) {
                Log.d(TAG, "SEC_COMMAND_ID_SNS_DELETE_EXCLUDED : netId(" + netId + "), delete excluded network");
            }
            if (this.mWifiInfo.getNetworkId() == netId.intValue() && (connectivityManager = this.mCm) != null) {
                connectivityManager.setAcceptUnvalidated(getCurrentNetwork(), false, true);
            }
            this.mWifiConfigManager.setNetworkNoInternetAccessExpected(netId.intValue(), false);
            sendWcmConfigurationChanged();
            return 1;
        } else if (i == 150) {
            Bundle args3 = (Bundle) innerMsg.obj;
            if (args3 != null) {
                return this.mWifiNative.sendActionFrame(this.mInterfaceName, args3.getString("param"));
            }
            return -1;
        } else if (i == 151) {
            Bundle args4 = (Bundle) innerMsg.obj;
            if (args4 == null) {
                return -1;
            }
            if (getNCHOVersion() == 2 && getNCHO20State() == 0 && "eng".equals(Build.TYPE)) {
                return this.mWifiNative.reAssocLegacy(this.mInterfaceName, args4.getString("param"));
            }
            return this.mWifiNative.reAssoc(this.mInterfaceName, args4.getString("param"));
        } else if (i == 170) {
            return this.mWifiNative.getWesMode(this.mInterfaceName);
        } else {
            if (i != 171) {
                switch (i) {
                    case 100:
                        if (getNCHOVersion() == 2 && getNCHO20State() == 0 && "eng".equals(Build.TYPE)) {
                            return this.mWifiNative.getRoamTriggerLegacy(this.mInterfaceName);
                        }
                        return this.mWifiNative.getRoamTrigger(this.mInterfaceName);
                    case 101:
                        Bundle args5 = (Bundle) innerMsg.obj;
                        if (args5 == null) {
                            return -1;
                        }
                        this.mIsNchoParamSet = true;
                        return (getNCHOVersion() == 2 && getNCHO20State() == 0 && "eng".equals(Build.TYPE)) ? (int) this.mWifiNative.setRoamTriggerLegacy(this.mInterfaceName, args5.getInt("level")) : (int) this.mWifiNative.setRoamTrigger(this.mInterfaceName, args5.getInt("level"));
                    case 102:
                        return this.mWifiNative.getRoamDelta(this.mInterfaceName);
                    case 103:
                        Bundle args6 = (Bundle) innerMsg.obj;
                        if (args6 == null) {
                            return -1;
                        }
                        this.mIsNchoParamSet = true;
                        return (int) this.mWifiNative.setRoamDelta(this.mInterfaceName, args6.getInt("level"));
                    case 104:
                        return this.mWifiNative.getRoamScanPeriod(this.mInterfaceName);
                    case 105:
                        Bundle args7 = (Bundle) innerMsg.obj;
                        if (args7 == null) {
                            return -1;
                        }
                        this.mIsNchoParamSet = true;
                        return (int) this.mWifiNative.setRoamScanPeriod(this.mInterfaceName, args7.getInt("time"));
                    default:
                        switch (i) {
                            case 109:
                                Bundle args8 = (Bundle) innerMsg.obj;
                                if (args8 != null) {
                                    return this.mWifiNative.setRoamScanChannels(this.mInterfaceName, args8.getString("chinfo"));
                                }
                                return -1;
                            case SoapEnvelope.VER11 /*110*/:
                                if (getNCHOVersion() != 2) {
                                    return -1;
                                }
                                int intResult = this.mWifiNative.getNCHOMode(this.mInterfaceName);
                                if (this.mVerboseLoggingEnabled) {
                                    Log.d(TAG, "Get ncho mode: " + intResult);
                                }
                                if (intResult == 0 || intResult == 1) {
                                    setNCHO20State(intResult, false);
                                    return intResult;
                                }
                                Log.e(TAG, "Get ncho mode - Something Wrong: " + intResult);
                                return intResult;
                            case MobileWipsScanResult.InformationElement.EID_ROAMING_CONSORTIUM /*111*/:
                                Bundle args9 = (Bundle) innerMsg.obj;
                                if (args9 == null) {
                                    return -1;
                                }
                                int setVal = args9.getInt("mode");
                                if (setVal == 0 || setVal == 1) {
                                    int nchoversion = getNCHOVersion();
                                    if (nchoversion == 2) {
                                        int intResult2 = setNCHO20State(setVal, true);
                                        if (intResult2 == 1) {
                                            this.mIsNchoParamSet = false;
                                            return intResult2;
                                        }
                                        Log.e(TAG, "Fail to set NCHO to Firmware:" + ((int) intResult2));
                                        return intResult2;
                                    } else if (nchoversion != 1 || setVal != 0 || getNCHO10State() != 1) {
                                        return -1;
                                    } else {
                                        restoreNcho10Param();
                                        this.mIsNchoParamSet = false;
                                        return -1;
                                    }
                                } else {
                                    Log.e(TAG, "Set ncho mode - invalid set value: " + setVal);
                                    return -1;
                                }
                            default:
                                switch (i) {
                                    case 130:
                                        return this.mWifiNative.getScanChannelTime(this.mInterfaceName);
                                    case 131:
                                        Bundle args10 = (Bundle) innerMsg.obj;
                                        if (args10 != null) {
                                            return this.mWifiNative.setScanChannelTime(this.mInterfaceName, args10.getString("time"));
                                        }
                                        return -1;
                                    case 132:
                                        return this.mWifiNative.getScanHomeTime(this.mInterfaceName);
                                    case 133:
                                        Bundle args11 = (Bundle) innerMsg.obj;
                                        if (args11 != null) {
                                            return this.mWifiNative.setScanHomeTime(this.mInterfaceName, args11.getString("time"));
                                        }
                                        return -1;
                                    case 134:
                                        return this.mWifiNative.getScanHomeAwayTime(this.mInterfaceName);
                                    case 135:
                                        Bundle args12 = (Bundle) innerMsg.obj;
                                        if (args12 != null) {
                                            return this.mWifiNative.setScanHomeAwayTime(this.mInterfaceName, args12.getString("time"));
                                        }
                                        return -1;
                                    case 136:
                                        return this.mWifiNative.getScanNProbes(this.mInterfaceName);
                                    case 137:
                                        Bundle args13 = (Bundle) innerMsg.obj;
                                        if (args13 != null) {
                                            return this.mWifiNative.setScanNProbes(this.mInterfaceName, args13.getString("num"));
                                        }
                                        return -1;
                                    default:
                                        switch (i) {
                                            case 161:
                                                Bundle args14 = (Bundle) innerMsg.obj;
                                                if (args14 != null) {
                                                    return (int) this.mWifiNative.setCountryRev(this.mInterfaceName, args14.getString("country"));
                                                }
                                                return -1;
                                            case 162:
                                                return this.mWifiNative.getBand(this.mInterfaceName);
                                            case 163:
                                                Bundle args15 = (Bundle) innerMsg.obj;
                                                if (args15 != null) {
                                                    return (int) this.mWifiNative.setBand(this.mInterfaceName, args15.getInt("band"));
                                                }
                                                return -1;
                                            case 164:
                                                return this.mWifiNative.getDfsScanMode(this.mInterfaceName);
                                            case 165:
                                                Bundle args16 = (Bundle) innerMsg.obj;
                                                if (args16 != null) {
                                                    return (int) this.mWifiNative.setDfsScanMode(this.mInterfaceName, args16.getInt("mode"));
                                                }
                                                return -1;
                                            default:
                                                if (!this.mVerboseLoggingEnabled) {
                                                    return -1;
                                                }
                                                Log.e(TAG, "ignore message : not implementation yet");
                                                return -1;
                                        }
                                }
                        }
                }
            } else {
                Bundle args17 = (Bundle) innerMsg.obj;
                if (args17 != null) {
                    return (int) this.mWifiNative.setWesMode(this.mInterfaceName, args17.getInt("mode"));
                }
                return -1;
            }
        }
    }

    /* access modifiers changed from: private */
    public String processMessageOnDefaultStateForCallSECStringApi(Message message) {
        String data;
        String prop_name;
        int propType;
        int propType2;
        Message innerMsg = (Message) message.obj;
        if (innerMsg == null) {
            Log.e(TAG, "CMD_SEC_STRING_API, invalid innerMsg");
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "CMD_SEC_STRING_API, inner msg.what:" + innerMsg.what);
        }
        int i = innerMsg.what;
        if (i != 85) {
            if (i == 223) {
                return this.mScanRequestProxy.semDumpCachedScanController();
            }
            if (i == 300) {
                return this.mWifiGeofenceManager.getGeofenceInformation();
            }
            switch (i) {
                case 274:
                    if (innerMsg.arg1 < 10) {
                        return this.mWifiNative.getVendorConnFileInfo(innerMsg.arg1);
                    }
                    return null;
                case 275:
                    if (innerMsg.obj == null || (data = ((Bundle) innerMsg.obj).getString("data")) == null || innerMsg.arg1 >= 10) {
                        return null;
                    }
                    if ("!remove".equals(data)) {
                        if (this.mWifiNative.removeVendorConnFile(innerMsg.arg1)) {
                            return "OK";
                        }
                        return null;
                    } else if (this.mWifiNative.putVendorConnFile(innerMsg.arg1, data)) {
                        return "OK";
                    } else {
                        return null;
                    }
                case 276:
                    if (innerMsg.obj == null || (prop_name = ((Bundle) innerMsg.obj).getString("prop_name")) == null) {
                        return null;
                    }
                    if ("vendor.wlandriver.mode".equals(prop_name)) {
                        propType = 0;
                    } else if ("vendor.wlandriver.status".equals(prop_name) == 0) {
                        return null;
                    } else {
                        propType = 1;
                    }
                    return this.mWifiNative.getVendorProperty(propType);
                case 277:
                    if (innerMsg.obj == null) {
                        return null;
                    }
                    Bundle prop_info = (Bundle) innerMsg.obj;
                    String prop_name2 = prop_info.getString("prop_name");
                    String data2 = prop_info.getString("data");
                    if (prop_name2 == null) {
                        return null;
                    }
                    if ("vendor.wlandriver.mode".equals(prop_name2)) {
                        propType2 = 0;
                    } else if ("vendor.wlandriver.status".equals(prop_name2) == 0) {
                        return null;
                    } else {
                        propType2 = 1;
                    }
                    if (this.mWifiNative.setVendorProperty(propType2, data2)) {
                        return "OK";
                    }
                    return null;
                case DhcpPacket.MIN_PACKET_LENGTH_L2 /*278*/:
                    return WlanTestHelper.getConfigFileString();
                default:
                    return null;
            }
        } else if (this.mIssueDetector == null) {
            return null;
        } else {
            int size = innerMsg.arg1;
            return this.mIssueDetector.getRawData(size == 0 ? 5 : size);
        }
    }

    /* access modifiers changed from: private */
    public String processMessageForCallSECStringApi(Message message) {
        Message innerMsg = (Message) message.obj;
        if (innerMsg == null) {
            Log.e(TAG, "CMD_SEC_STRING_API, invalid innerMsg");
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "CMD_SEC_STRING_API, inner msg.what:" + innerMsg.what);
        }
        int i = innerMsg.what;
        if (i == 108) {
            return this.mWifiNative.getRoamScanChannels(this.mInterfaceName);
        }
        if (i == 160) {
            return this.mWifiNative.getCountryRev(this.mInterfaceName);
        }
        if (!this.mVerboseLoggingEnabled) {
            return null;
        }
        Log.e(TAG, "ignore message : not implementation yet");
        return null;
    }

    /* access modifiers changed from: package-private */
    public void sendCallSECApiAsync(Message msg, int callingPid) {
        if (this.mVerboseLoggingEnabled) {
            Log.i(TAG, "sendCallSECApiAsync what=" + msg.what);
        }
        sendMessage(obtainMessage(CMD_SEC_API_ASYNC, msg.what, callingPid, Message.obtain(msg)));
    }

    /* access modifiers changed from: package-private */
    public int syncCallSECApi(AsyncChannel channel, Message msg) {
        if (this.mVerboseLoggingEnabled) {
            Log.i(TAG, "syncCallSECApi what=" + msg.what);
        }
        if (this.mIsShutdown) {
            return -1;
        }
        if (channel == null) {
            Log.e(TAG, "Channel is not initialized");
            return -1;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_SEC_API, msg.what, 0, msg);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    /* access modifiers changed from: package-private */
    public String syncCallSECStringApi(AsyncChannel channel, Message msg) {
        if (this.mVerboseLoggingEnabled) {
            Log.i(TAG, "syncCallSECStringApi what=" + msg.what);
        }
        if (this.mIsShutdown) {
            return null;
        }
        Message resultMsg = channel.sendMessageSynchronously(CMD_SEC_STRING_API, msg.what, 0, msg);
        String result = (String) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public void showToastMsg(int type, String extraString) {
        sendMessage(CMD_SHOW_TOAST_MSG, type, 0, extraString);
    }

    /* access modifiers changed from: package-private */
    public void setImsCallEstablished(boolean isEstablished) {
        sendMessage(CMD_IMS_CALL_ESTABLISHED, isEstablished, 0);
    }

    /* access modifiers changed from: package-private */
    public boolean isImsCallEstablished() {
        return this.mIsImsCallEstablished;
    }

    public void resetPeriodicScanTimer() {
        WifiConnectivityManager wifiConnectivityManager = this.mWifiConnectivityManager;
        if (wifiConnectivityManager != null) {
            wifiConnectivityManager.resetPeriodicScanTime();
        }
    }

    /* access modifiers changed from: private */
    public void setTcpBufferAndProxySettingsForIpManager() {
        WifiConfiguration currentConfig = getCurrentWifiConfiguration();
        if (!(currentConfig == null || this.mIpClient == null)) {
            this.mIpClient.setHttpProxy(currentConfig.getHttpProxy());
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes) && this.mIpClient != null) {
            this.mIpClient.setTcpBufferSizes(this.mTcpBufferSizes);
        }
    }

    public void initializeWifiChipInfo() {
        if (!WifiChipInfo.getInstance().isReady()) {
            Log.d(TAG, "chipset information is not ready, try to get the information");
            String cidInfo = this.mWifiNative.getVendorConnFileInfo(1);
            if (DBG_PRODUCT_DEV) {
                Log.d(TAG, ".cid.info: " + cidInfo);
            }
            String wifiVerInfo = this.mWifiNative.getVendorConnFileInfo(5);
            if (DBG_PRODUCT_DEV) {
                Log.d(TAG, ".wifiver.info: " + wifiVerInfo);
            }
            WifiChipInfo.getInstance().updateChipInfos(cidInfo, wifiVerInfo);
            String macAddress = this.mWifiNative.getVendorConnFileInfo(0);
            Log.d(TAG, "chipset information is macAddress" + macAddress);
            if (macAddress != null && macAddress.length() >= 17) {
                WifiChipInfo.getInstance().setMacAddress(macAddress.substring(0, 17));
                if (!this.mWifiInfo.hasRealMacAddress()) {
                    this.mWifiInfo.setMacAddress(macAddress.substring(0, 17));
                }
            }
            String softapInfo = this.mWifiNative.getVendorConnFileInfo(6);
            Log.d(TAG, "chipset information is softapInfo" + softapInfo);
            this.mWifiInjector.getSemWifiApChipInfo().readSoftApInfo(softapInfo);
            if (WifiChipInfo.getInstance().isReady()) {
                Log.d(TAG, "chipset information is ready");
            }
        }
    }

    /* access modifiers changed from: private */
    public void increaseCounter(int what) {
        long value = 1;
        if (this.mEventCounter.containsKey(Integer.valueOf(what))) {
            value = this.mEventCounter.get(Integer.valueOf(what)).longValue() + 1;
        }
        this.mEventCounter.put(Integer.valueOf(what), Long.valueOf(value));
    }

    private long getCounter(int what, long defaultValue) {
        if (this.mEventCounter.containsKey(Integer.valueOf(what))) {
            return this.mEventCounter.get(Integer.valueOf(what)).longValue();
        }
        return defaultValue;
    }

    private void resetCounter() {
        this.mEventCounter.clear();
    }

    /* access modifiers changed from: private */
    public String getWifiParameters(boolean reset) {
        boolean z = reset;
        int wifiState = this.mWifiState.get() == 3 ? 1 : 0;
        int alwaysAllowScanningMode = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0);
        int smartNetworkSwitch = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 0);
        int agressiveSmartNetworkSwitch = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_aggressive_mode_on", 0);
        int favoriteApCount = Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_favorite_ap_count", 0);
        int isAutoWifiEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "sem_auto_wifi_control_enabled", 0);
        int safeModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "safe_wifi", 0);
        StringBuffer sb = new StringBuffer();
        sb.append(wifiState);
        sb.append(" ");
        sb.append(alwaysAllowScanningMode);
        sb.append(" ");
        sb.append(isAutoWifiEnabled);
        sb.append(" ");
        sb.append(favoriteApCount);
        sb.append(" ");
        sb.append(smartNetworkSwitch);
        sb.append(" ");
        sb.append(agressiveSmartNetworkSwitch);
        sb.append(" ");
        sb.append(safeModeEnabled);
        sb.append(" ");
        String scanValues = null;
        if (z) {
            scanValues = this.mScanRequestProxy.semGetScanCounterForBigData(z);
        }
        if (scanValues != null) {
            sb.append(scanValues);
            sb.append(" ");
        } else {
            sb.append("-1 -1 -1 -1 ");
        }
        int i = alwaysAllowScanningMode;
        sb.append(getCounter(151562, 0));
        sb.append(" ");
        sb.append(getCounter(151564, 0));
        sb.append(" ");
        sb.append(getCounter(151565, 0));
        sb.append(" ");
        sb.append(getCounter(WifiMonitor.DRIVER_HUNG_EVENT, 0));
        sb.append(" ");
        sb.append(this.mWifiAdpsEnabled.get() ? 1 : 0);
        sb.append(" ");
        sb.append(this.mWifiConfigManager.getSavedNetworks(1010).size());
        sb.append(" ");
        sb.append(this.laaEnterState);
        sb.append(" ");
        sb.append(this.laaActiveState);
        if (z) {
            resetCounter();
            if (CSC_SUPPORT_5G_ANT_SHARE) {
                this.laaEnterState = 0;
                this.laaActiveState = 0;
            }
        }
        return sb.toString();
    }

    public boolean isUnstableAp(String bssid) {
        UnstableApController unstableApController = this.mUnstableApController;
        if (unstableApController != null) {
            return unstableApController.isUnstableAp(bssid);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isWifiSharingProvisioning() {
        WifiManager mManager = (WifiManager) this.mContext.getSystemService("wifi");
        Log.i(TAG, "getProvisionSuccess : " + mManager.getProvisionSuccess() + " isWifiSharingEnabled " + mManager.isWifiSharingEnabled());
        return mManager.isWifiSharingEnabled() && mManager.getProvisionSuccess() == 2;
    }

    /* access modifiers changed from: private */
    public void updatePasspointNetworkSelectionStatus(boolean enabled) {
        if (!enabled) {
            for (WifiConfiguration network : this.mWifiConfigManager.getConfiguredNetworks()) {
                if (network.isPasspoint() && network.networkId != -1) {
                    this.mWifiConfigManager.disableNetwork(network.networkId, network.creatorUid);
                    this.mWifiConfigManager.removeNetwork(network.networkId, network.creatorUid);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void removePasspointNetworkIfSimAbsent() {
        for (WifiConfiguration network : this.mWifiConfigManager.getConfiguredNetworks()) {
            if (network.isPasspoint() && network.networkId != -1 && TelephonyUtil.isSimEapMethod(network.enterpriseConfig.getEapMethod())) {
                Log.w(TAG, "removePasspointNetworkIfSimAbsent : network " + network.configKey() + " try to remove");
                this.mWifiConfigManager.disableNetwork(network.networkId, network.creatorUid);
                this.mWifiConfigManager.removeNetwork(network.networkId, network.creatorUid);
            }
        }
    }

    public boolean isPasspointEnabled() {
        return this.mIsPasspointEnabled;
    }

    /* access modifiers changed from: private */
    public void updateVendorApSimState() {
        boolean isUseableVendorUsim = TelephonyUtil.isVendorApUsimUseable(getTelephonyManager());
        Log.i(TAG, "updateVendorApSimState : " + isUseableVendorUsim);
        this.mPasspointManager.setVendorSimUseable(isUseableVendorUsim);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_hotspot20_useable_vendor_usim", isUseableVendorUsim);
    }

    /* access modifiers changed from: private */
    public void updateWlanDebugLevel() {
        String memdumpInfo = this.mWifiNative.getVendorConnFileInfo(7);
        Log.i(TAG, "updateWlanDebugLevel : current level is " + memdumpInfo);
        if (memdumpInfo != null && !memdumpInfo.equals("2")) {
            if (this.mWifiNative.putVendorConnFile(7, "2")) {
                Log.i(TAG, "updateWlanDebugLevel : update to 2 succeed");
            } else {
                Log.i(TAG, "updateWlanDebugLevel : update to 2 failed");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean checkAndShowSimRemovedDialog(WifiConfiguration config) {
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        if (config.semIsVendorSpecificSsid && enterpriseConfig != null && (enterpriseConfig.getEapMethod() == 5 || enterpriseConfig.getEapMethod() == 6)) {
            int simState = getTelephonyManager().getSimState();
            Log.i(TAG, "simState is " + simState + " for " + config.SSID);
            if (simState == 1 || simState == 0) {
                Log.d(TAG, "trying to connect without SIM, show alert dialog");
                SemWifiFrameworkUxUtils.showWarningDialog(this.mContext, 2, new String[]{StringUtil.removeDoubleQuotes(config.SSID)});
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void showEapNotificationToast(int code) {
        Log.i(TAG, "eap code : " + code + ", targetId: " + this.mTargetNetworkId);
        if (code != 987654321) {
            if (CSC_WIFI_SUPPORT_VZW_EAP_AKA) {
                SemWifiFrameworkUxUtils.showEapToastVzw(this.mContext, code);
            } else if (CSC_WIFI_ERRORCODE) {
                SemWifiFrameworkUxUtils.showEapToast(this.mContext, code);
            }
        }
    }

    private boolean checkAndRetryConnect(int targetNetworkId) {
        if (this.mLastEAPFailureNetworkId != targetNetworkId) {
            this.mLastEAPFailureNetworkId = targetNetworkId;
            this.mLastEAPFailureCount = 0;
        }
        int i = this.mLastEAPFailureCount + 1;
        this.mLastEAPFailureCount = i;
        if (i > 3) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void processMessageForEap(int event, int status, String message) {
        String eapEventMsg;
        int i = event;
        int i2 = status;
        String str = message;
        String noti_status = i2 == 987654321 ? "none" : Integer.toString(status);
        Log.i(TAG, "eap message : event [" + i + "] , status [" + noti_status + "] , message '" + str + "', targetId: " + this.mTargetNetworkId);
        WifiConfiguration currentConfig = null;
        int i3 = this.mTargetNetworkId;
        if (i3 != -1) {
            currentConfig = this.mWifiConfigManager.getConfiguredNetwork(i3);
        }
        if (currentConfig == null) {
            Log.e(TAG, "ignore eap message : currentConfig is null");
            StringBuilder eapLogTemp = new StringBuilder();
            eapLogTemp.append("events: { EAP_EVENT_" + i + "},");
            eapLogTemp.append(" extra_info: { " + str + " }");
            this.mWifiMetrics.logStaEvent(22, eapLogTemp.toString());
        } else if (i < 1) {
            Log.e(TAG, "ignore eap message : event is not defined");
        } else {
            boolean hasEverConnected = currentConfig.getNetworkSelectionStatus().getHasEverConnected();
            boolean isNetworkPermanentlyDisabled = currentConfig.getNetworkSelectionStatus().isNetworkPermanentlyDisabled();
            String str2 = noti_status;
            String str3 = " }";
            switch (i) {
                case 1:
                    updateAnonymousIdentity(this.mTargetNetworkId);
                    eapEventMsg = "ANONYMOUS_IDENTITY_UPDATED ";
                    break;
                case 2:
                    if (TelephonyUtil.isSimEapMethod(currentConfig.enterpriseConfig.getEapMethod())) {
                        updateSimNumber(this.mTargetNetworkId);
                    }
                    Log.i(TAG, "network " + currentConfig.configKey() + " has ever connected " + hasEverConnected + ", isNetworkPermanentlyDisabled, " + isNetworkPermanentlyDisabled);
                    if (!isNetworkPermanentlyDisabled) {
                        this.mWifiConfigManager.updateNetworkSelectionStatus(this.mTargetNetworkId, 3);
                        if (checkAndRetryConnect(this.mTargetNetworkId)) {
                            Log.w(TAG, "update network status to auth failure , retry to conect ");
                            startConnectToNetwork(this.mTargetNetworkId, 1010, "any");
                        }
                        Log.w(TAG, "trackBssid for 802.1x auth failure : " + str);
                        if (!TextUtils.isEmpty(message) && !"00:00:00:00:00:00".equals(str)) {
                            this.mWifiConnectivityManager.trackBssid(str, false, 3);
                        }
                    }
                    eapEventMsg = "DEAUTH_8021X_AUTH_FAILED ";
                    break;
                case 3:
                    if (TelephonyUtil.isSimEapMethod(currentConfig.enterpriseConfig.getEapMethod())) {
                        updateSimNumber(this.mTargetNetworkId);
                    }
                    Log.i(TAG, "network " + currentConfig.configKey() + " has ever connected " + hasEverConnected + ", isNetworkPermanentlyDisabled, " + isNetworkPermanentlyDisabled);
                    int currentEapMethod = currentConfig.enterpriseConfig.getEapMethod();
                    if (!hasEverConnected) {
                        if (currentEapMethod == 0 || currentEapMethod == 2 || currentEapMethod == 3) {
                            Log.i(TAG, "update network status to wrong password ");
                            this.mWifiConfigManager.updateNetworkSelectionStatus(this.mTargetNetworkId, 13);
                        } else if (!isNetworkPermanentlyDisabled) {
                            this.mWifiConfigManager.updateNetworkSelectionStatus(this.mTargetNetworkId, 3);
                            if (checkAndRetryConnect(this.mTargetNetworkId)) {
                                Log.w(TAG, "update network status to eap failure , retry to conect , " + this.mLastEAPFailureCount);
                                startConnectToNetwork(this.mTargetNetworkId, 1010, "any");
                            }
                        }
                    }
                    eapEventMsg = "FAIL ";
                    break;
                case 4:
                    eapEventMsg = "ERROR ";
                    break;
                case 5:
                    eapEventMsg = "LOG ";
                    break;
                case 6:
                    eapEventMsg = "NO_CREDENTIALS ";
                    break;
                case 7:
                    showEapNotificationToast(i2);
                    eapEventMsg = "NOTIFICATION ";
                    break;
                case 8:
                    eapEventMsg = "SUCCESS ";
                    break;
                case 9:
                case 11:
                    WifiMobileDeviceManager.auditLog(this.mContext, 1, false, TAG, "EAP-TLS handshake failed: " + str);
                    eapEventMsg = "TLS_HANDSHAKE_FAIL ";
                    break;
                case 10:
                    new CertificatePolicy().notifyCertificateFailureAsUser("wifi_module", str, true, 0);
                    WifiMobileDeviceManager.auditLog(this.mContext, 1, false, TAG, "Certificate verification failed: " + str);
                    eapEventMsg = "TLS_CERT_ERROR ";
                    break;
                default:
                    if (this.mVerboseLoggingEnabled) {
                        Log.e(TAG, "ignore eap message : not implementation yet");
                    }
                    eapEventMsg = null;
                    break;
            }
            StringBuilder eapLog = new StringBuilder();
            eapLog.append("events: { EAP_EVENT_" + eapEventMsg + "},");
            if (i2 != 987654321) {
                eapLog.append(" notification_status=" + i2);
            }
            eapLog.append(" extra_info: { " + str + str3);
            this.mWifiMetrics.logStaEvent(22, eapLog.toString());
        }
    }

    private int getConfiguredSimNum(WifiConfiguration config) {
        int simNum = 1;
        String simNumStr = config.enterpriseConfig.getSimNumber();
        if (simNumStr != null && !simNumStr.isEmpty()) {
            try {
                simNum = Integer.parseInt(simNumStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "getConfiguredSimNum - failed to getSimNumber ");
            }
        }
        Log.i(TAG, "getConfiguredSimNum - previous saved simNum:" + simNum);
        return simNum;
    }

    /* access modifiers changed from: private */
    public boolean setPermanentIdentity(WifiConfiguration config) {
        if (this.mTargetNetworkId == -1) {
            Log.e(TAG, "PermanentIdentity : NetworkId is INVALID_NETWORK_ID");
            return false;
        }
        int simNum = getConfiguredSimNum(config);
        if (getTelephonyManager().getPhoneCount() > 1) {
            int multiSimState = TelephonyUtil.semGetMultiSimState(getTelephonyManager());
            if (multiSimState == 1) {
                simNum = 1;
            } else if (multiSimState == 2) {
                simNum = 2;
            }
        } else {
            simNum = 1;
        }
        Log.i(TAG, "PermanentIdentity set simNum:" + simNum);
        config.enterpriseConfig.setSimNumber(simNum);
        TelephonyUtil.setSimIndex(simNum);
        Pair<String, String> identityPair = TelephonyUtil.getSimIdentity(getTelephonyManager(), new TelephonyUtil(), config, this.mWifiInjector.getCarrierNetworkConfig());
        if (identityPair == null || identityPair.first == null) {
            Log.i(TAG, "PermanentIdentity identityPair is invalid ");
            return false;
        }
        if (!config.semIsVendorSpecificSsid || TextUtils.isEmpty((CharSequence) identityPair.second)) {
            String oldIdentity = config.enterpriseConfig.getIdentity();
            if (oldIdentity != null && !oldIdentity.equals(identityPair.first) && TelephonyUtil.isSimEapMethod(config.enterpriseConfig.getEapMethod())) {
                Log.d(TAG, "PermanentIdentity has been changed. setAnonymousIdentity to null for EAP method SIM/AKA/AKA'");
                config.enterpriseConfig.setAnonymousIdentity((String) null);
            }
            Log.d(TAG, "PermanentIdentity is set to : " + ((String) identityPair.first).substring(0, 7));
            config.enterpriseConfig.setIdentity((String) identityPair.first);
        } else {
            Log.i(TAG, "PermanentIdentity , identity is encrypted , need SUP_REQUEST_IDENTITY ");
            config.enterpriseConfig.setIdentity((String) null);
        }
        this.mWifiConfigManager.addOrUpdateNetwork(config, 1010);
        return true;
    }

    private void updateSimNumber(int netId) {
        Log.i(TAG, "updateSimNumber() netId : " + netId);
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (config != null && config.enterpriseConfig != null && TelephonyUtil.isSimConfig(config)) {
            int simNum = getConfiguredSimNum(config);
            if (getTelephonyManager().getPhoneCount() > 1) {
                int multiSimState = TelephonyUtil.semGetMultiSimState(getTelephonyManager());
                if (multiSimState == 1) {
                    simNum = 1;
                } else if (multiSimState == 2) {
                    simNum = 2;
                } else if (multiSimState == 3) {
                    if (simNum == 2) {
                        simNum = 1;
                    } else {
                        simNum = 2;
                    }
                }
            } else {
                simNum = 1;
            }
            Log.i(TAG, "updateSimNumber() set simNum:" + simNum);
            config.enterpriseConfig.setSimNumber(simNum);
            this.mWifiConfigManager.addOrUpdateNetwork(config, 1010);
        }
    }

    /* access modifiers changed from: private */
    public void updateIdentityOnWifiConfiguration(WifiConfiguration config, String identity) {
        Log.i(TAG, "updateIdentityOnWifiConfiguration -  network :" + config.configKey());
        if (config.enterpriseConfig != null && !identity.equals(config.enterpriseConfig.getIdentity())) {
            Log.d(TAG, "updateIdentityOnWifiConfiguration -  Identity has been changed. setAnonymousIdentity to null for EAP method SIM/AKA/AKA'");
            config.enterpriseConfig.setIdentity(identity);
            config.enterpriseConfig.setAnonymousIdentity((String) null);
            this.mWifiConfigManager.addOrUpdateNetwork(config, 1010);
        }
    }

    private void updateAnonymousIdentity(int netId) {
        Log.i(TAG, "updateAnonymousIdentity(" + netId + ")");
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (config != null && config.enterpriseConfig != null && TelephonyUtil.isSimEapMethod(config.enterpriseConfig.getEapMethod()) && !TextUtils.isEmpty(config.enterpriseConfig.getAnonymousIdentity())) {
            Log.i(TAG, "reset Anonymousidentity from supplicant, so reset it in WifiConfiguration.");
            config.enterpriseConfig.setAnonymousIdentity((String) null);
            this.mWifiConfigManager.addOrUpdateNetwork(config, 1010);
        }
    }

    /* access modifiers changed from: private */
    public void sendIpcMessageToRilForLteu(int LTEU_WIFI_STATE, boolean isEnabled, boolean is5GHz, boolean forceSend) {
        int lteuState;
        int lteuEnable;
        int lteuState2 = mLteuState;
        Log.d(TAG, "previous lteuState = " + lteuState2 + ", lteuEnable = " + mLteuEnable);
        if (!isEnabled || !is5GHz) {
            lteuState = lteuState2 & (~LTEU_WIFI_STATE);
            this.laaActiveState = 1;
        } else {
            lteuState = lteuState2 | LTEU_WIFI_STATE;
        }
        if (lteuState <= 0 || lteuState >= 8) {
            lteuEnable = 1;
        } else {
            lteuEnable = 0;
        }
        Log.d(TAG, "input = " + LTEU_WIFI_STATE + ", is5GHz = " + is5GHz);
        Log.d(TAG, "new lteuState = " + lteuState + ", lteuEnable = " + lteuEnable);
        if (forceSend || (mScellEnter && lteuState != mLteuState)) {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                dos.writeByte(17);
                dos.writeByte(144);
                dos.writeShort(5);
                dos.writeByte(lteuEnable);
                try {
                    dos.close();
                } catch (Exception e) {
                }
                try {
                    byte[] responseData = new byte[2048];
                    if (phone != null) {
                        int ret = phone.invokeOemRilRequestRaw(bos.toByteArray(), responseData);
                        Log.i(TAG, "invokeOemRilRequestRaw : return value: " + ret);
                    } else {
                        Log.d(TAG, "ITelephony is null");
                    }
                } catch (RemoteException e2) {
                    Log.e(TAG, "invokeOemRilRequestRaw : RemoteException: " + e2);
                }
            } catch (IOException e3) {
                Log.e(TAG, "IOException occurs in set lteuEnable");
                try {
                    dos.close();
                    return;
                } catch (Exception e4) {
                    return;
                }
            } catch (Throwable th) {
                try {
                    dos.close();
                } catch (Exception e5) {
                }
                throw th;
            }
        }
        mLteuState = lteuState;
        mLteuEnable = lteuEnable;
    }

    /* access modifiers changed from: private */
    public int waitForDhcpRelease() {
        try {
            Thread.sleep((long) 500);
            return 0;
        } catch (InterruptedException ex) {
            loge("waitForDhcpRelease sleep exception:" + ex);
            return 0;
        }
    }

    private boolean isSystem(int uid) {
        return uid < 10000;
    }

    public void registerWifiNetworkCallbacks(ClientModeChannel.WifiNetworkCallback wifiNetworkCallback) {
        if (isSystem(Binder.getCallingUid())) {
            Log.w(TAG, "registerWCMCallbacks");
            if (this.mWifiNetworkCallbackList == null) {
                this.mWifiNetworkCallbackList = new ArrayList<>();
            }
            this.mWifiNetworkCallbackList.add(wifiNetworkCallback);
            return;
        }
        Log.w(TAG, "This is only for system service");
        throw new SecurityException("This is only for system service");
    }

    /* access modifiers changed from: private */
    public void handleWifiNetworkCallbacks(int method) {
        if (method != 1) {
            switch (method) {
                case 5:
                    Iterator<ClientModeChannel.WifiNetworkCallback> it = this.mWifiNetworkCallbackList.iterator();
                    while (it.hasNext()) {
                        it.next().notifyRoamSession("start");
                    }
                    return;
                case 6:
                    Iterator<ClientModeChannel.WifiNetworkCallback> it2 = this.mWifiNetworkCallbackList.iterator();
                    while (it2.hasNext()) {
                        it2.next().notifyRoamSession("complete");
                    }
                    return;
                case 7:
                    Iterator<ClientModeChannel.WifiNetworkCallback> it3 = this.mWifiNetworkCallbackList.iterator();
                    while (it3.hasNext()) {
                        it3.next().notifyLinkPropertiesUpdated(this.mLinkProperties);
                    }
                    return;
                case 8:
                    this.isDhcpStartSent = true;
                    Iterator<ClientModeChannel.WifiNetworkCallback> it4 = this.mWifiNetworkCallbackList.iterator();
                    while (it4.hasNext()) {
                        it4.next().notifyDhcpSession("start");
                    }
                    return;
                case 9:
                    if (this.isDhcpStartSent) {
                        this.isDhcpStartSent = false;
                        Iterator<ClientModeChannel.WifiNetworkCallback> it5 = this.mWifiNetworkCallbackList.iterator();
                        while (it5.hasNext()) {
                            it5.next().notifyDhcpSession("complete");
                        }
                        return;
                    }
                    return;
                case 10:
                    Iterator<ClientModeChannel.WifiNetworkCallback> it6 = this.mWifiNetworkCallbackList.iterator();
                    while (it6.hasNext()) {
                        it6.next().notifyReachabilityLost();
                    }
                    return;
                case 11:
                    Iterator<ClientModeChannel.WifiNetworkCallback> it7 = this.mWifiNetworkCallbackList.iterator();
                    while (it7.hasNext()) {
                        it7.next().notifyProvisioningFail();
                    }
                    return;
                default:
                    return;
            }
        } else {
            Iterator<ClientModeChannel.WifiNetworkCallback> it8 = this.mWifiNetworkCallbackList.iterator();
            while (it8.hasNext()) {
                it8.next().checkIsCaptivePortalException(getTargetSsid());
            }
        }
    }

    public WifiClientModeChannel makeWifiClientModeChannel() {
        return new WifiClientModeChannel();
    }

    public class WifiClientModeChannel implements ClientModeChannel {
        public static final int CALLBACK_CHECK_IS_CAPTIVE_PORTAL_EXCEPTION = 1;
        public static final int CALLBACK_FETCH_RSSI_PKTCNT_DISCONNECTED = 4;
        public static final int CALLBACK_FETCH_RSSI_PKTCNT_FAILED = 3;
        public static final int CALLBACK_FETCH_RSSI_PKTCNT_SUCCESS = 2;
        public static final int CALLBACK_NOTIFY_DHCP_SESSION_COMPLETE = 9;
        public static final int CALLBACK_NOTIFY_DHCP_SESSION_START = 8;
        public static final int CALLBACK_NOTIFY_LINK_PROPERTIES_UPDATED = 7;
        public static final int CALLBACK_NOTIFY_PROVISIONING_FAIL = 11;
        public static final int CALLBACK_NOTIFY_REACHABILITY_LOST = 10;
        public static final int CALLBACK_NOTIFY_ROAM_SESSION_COMPLETE = 6;
        public static final int CALLBACK_NOTIFY_ROAM_SESSION_START = 5;

        public WifiClientModeChannel() {
            ClientModeImpl.this.isDhcpStartSent = false;
        }

        public Message fetchPacketCountNative() {
            Message msg = ClientModeImpl.this.obtainMessage();
            if (!ClientModeImpl.this.isConnected()) {
                msg.what = 4;
                return msg;
            }
            ClientModeImpl.this.fetchRssiLinkSpeedAndFrequencyNative();
            WifiNative.TxPacketCounters counters = ClientModeImpl.this.mWifiNative.getTxPacketCounters(ClientModeImpl.this.mInterfaceName);
            if (counters != null) {
                msg.what = 2;
                msg.arg1 = counters.txSucceeded;
                msg.arg2 = counters.txFailed;
            } else {
                msg.what = 3;
            }
            return msg;
        }

        public void setWifiNetworkEnabled(boolean valid) {
            ClientModeImpl.this.mWifiScoreReport.setWifiNetworkEnabled(valid);
        }

        public boolean getManualSelection() {
            return ClientModeImpl.this.mIsManualSelection;
        }

        public void setCaptivePortal(int netId, boolean captivePortal) {
            ClientModeImpl.this.sendMessage(WifiConnectivityMonitor.CMD_CONFIG_SET_CAPTIVE_PORTAL, netId, captivePortal);
        }

        public void updateNetworkSelectionStatus(int netId, int reason) {
            ClientModeImpl.this.sendMessage(WifiConnectivityMonitor.CMD_CONFIG_UPDATE_NETWORK_SELECTION, netId, reason);
        }

        public boolean eleDetectedDebug() {
            if (!ClientModeImpl.DBG_PRODUCT_DEV || (ClientModeImpl.mWlanAdvancedDebugState & 32) == 0) {
                return false;
            }
            Log.i(ClientModeImpl.TAG, "eleDetectedDebug return true");
            return true;
        }

        public void unwantedMoreDump() {
            if (ClientModeImpl.DBG_PRODUCT_DEV) {
                FileWriter writer = null;
                if ((ClientModeImpl.mWlanAdvancedDebugState & 16) != 0) {
                    try {
                        File file = new File("/data/log/mx_panic");
                        if (!file.exists()) {
                            file.createNewFile();
                            if (file.exists()) {
                                writer = new FileWriter(file);
                                writer.append("1");
                                writer.flush();
                            }
                        }
                        try {
                            try {
                                Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", "system/bin/cp /data/log/mx_panic /proc/driver/mxman_ctrl0/mx_panic"}).waitFor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        try {
                            String[] dumpCmd = {"/system/bin/sh", "-c", "system/bin/bugreport > /data/log/unwant_dumpState_" + ClientModeImpl.this.getTimeToString() + ".log"};
                            try {
                                Runtime.getRuntime().exec(dumpCmd).waitFor();
                            } catch (InterruptedException e3) {
                                e3.printStackTrace();
                            }
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e5) {
                                e5.printStackTrace();
                            }
                        }
                    } catch (Exception e6) {
                        e6.printStackTrace();
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (Throwable th) {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e7) {
                                e7.printStackTrace();
                            }
                        }
                        throw th;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendWcmConfigurationChanged() {
        Intent intent = new Intent();
        intent.setAction("ACTION_WCM_CONFIGURATION_CHANGED");
        Context context = this.mContext;
        if (context != null) {
            try {
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
            } catch (IllegalStateException e) {
                loge("Send broadcast - action:" + intent.getAction());
            }
        }
    }

    private boolean isPoorNetworkTestEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_watchdog_poor_network_test_enabled", 0) == 1;
    }

    /* access modifiers changed from: private */
    public boolean isWCMEnabled() {
        if ("REMOVED".equals(SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSNSSTATUS))) {
            return false;
        }
        return true;
    }

    public void setIWCMonitorAsyncChannel(Handler handler) {
        log("setIWCAsyncChannel");
        if (this.mIWCMonitorChannel == null) {
            if (DBG) {
                log("new mWcmChannel created");
            }
            this.mIWCMonitorChannel = new AsyncChannel();
        }
        if (DBG) {
            log("mWcmChannel connected");
        }
        this.mIWCMonitorChannel.connect(this.mContext, getHandler(), handler);
    }

    /* access modifiers changed from: private */
    public void setAnalyticsUserDisconnectReason(short reason) {
        Log.d(TAG, "setAnalyticsUserDisconnectReason " + reason);
        this.mWifiNative.setAnalyticsDisconnectReason(this.mInterfaceName, reason);
    }

    /* access modifiers changed from: private */
    public void notifyMobilewipsRoamEvent(String startComplete) {
        if ("".equals(CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && MobileWipsFrameworkService.getInstance() != null && startComplete.equals("start")) {
            MobileWipsFrameworkService.getInstance().sendEmptyMessage(24);
        }
    }

    /* access modifiers changed from: private */
    public void updateEDMWiFiPolicy() {
        this.mWifiConfigManager.forcinglyEnablePolicyUpdatedNetworks(1000);
        if (this.mWifiInfo == null || !isConnected()) {
            Log.d(TAG, "wifi Info is null or no connected AP.");
            return;
        }
        Log.d(TAG, "updateEDMWiFiPolicy. SSID: " + this.mWifiInfo.getSSID());
        if (this.mWifiInfo.getSSID() != null && !this.mWifiInfo.getSSID().isEmpty()) {
            WifiB2BConfigurationPolicy.B2BConfiguration conf = this.mWifiB2bConfigPolicy.getConfiguration(StringUtil.removeDoubleQuotes(this.mWifiInfo.getSSID()));
            if (conf != null) {
                boolean result = true;
                if (conf.getRoamTrigger() != Integer.MAX_VALUE || conf.getRoamDelta() != Integer.MAX_VALUE || conf.getScanPeriod() != Integer.MAX_VALUE) {
                    if (!this.mWifiB2bConfigPolicy.isPolicyApplied()) {
                        if (getNCHOVersion() == 2) {
                            result = setNCHO20State(1, true);
                        }
                        this.mWifiB2bConfigPolicy.setPolicyApplied(true);
                        Log.d(TAG, "updateEDMWiFiPolicy - setNCHOMode: " + result);
                    }
                    this.mWifiNative.setRoamTrigger(this.mInterfaceName, conf.getRoamTrigger() == Integer.MAX_VALUE ? -75 : conf.getRoamTrigger());
                    int roamScanPediod = 10;
                    this.mWifiNative.setRoamDelta(this.mInterfaceName, conf.getRoamDelta() == Integer.MAX_VALUE ? 10 : conf.getRoamDelta());
                    if (conf.getScanPeriod() != Integer.MAX_VALUE) {
                        roamScanPediod = conf.getScanPeriod();
                    }
                    this.mWifiNative.setRoamScanPeriod(this.mInterfaceName, roamScanPediod);
                } else if (this.mWifiB2bConfigPolicy.isPolicyApplied()) {
                    Log.d(TAG, "No policy exists, clear previous Policy");
                    clearEDMWiFiPolicy();
                }
                if (conf.skipDHCPRenewal() != 0) {
                    this.mRoamDhcpPolicyByB2bConfig = 2;
                } else {
                    this.mRoamDhcpPolicyByB2bConfig = 0;
                }
            } else {
                clearEDMWiFiPolicy();
            }
        }
    }

    /* access modifiers changed from: private */
    public void clearEDMWiFiPolicy() {
        Log.d(TAG, "clearEDMWiFiPolicy: " + this.mWifiB2bConfigPolicy.isPolicyApplied() + "/" + this.mIsNchoParamSet);
        this.mRoamDhcpPolicyByB2bConfig = 0;
        if (this.mWifiB2bConfigPolicy.isPolicyApplied() || this.mIsNchoParamSet) {
            int nchoVersion = getNCHOVersion();
            if (nchoVersion == 1 && getNCHO10State() == 1) {
                restoreNcho10Param();
            } else if (nchoVersion == 2) {
                setNCHO20State(0, true);
            }
        }
        this.mWifiB2bConfigPolicy.setPolicyApplied(false);
        this.mIsNchoParamSet = false;
    }

    /* access modifiers changed from: private */
    public int getNCHOVersion() {
        if (this.mNchoVersion == -1) {
            if (this.mWifiState.get() != 3) {
                Log.e(TAG, "getNCHOVersion Wi-Fi is not enabled state");
                return -1;
            }
            int result = this.mWifiNative.getNCHOMode(this.mInterfaceName);
            if (result == -1) {
                this.mNchoVersion = 1;
                if (getNCHO10State() != 1) {
                    backUpNcho10Param();
                }
            } else if (result == 0 || result == 1) {
                this.mNchoVersion = 2;
                setNCHO20State(result, false);
            } else {
                Log.e(TAG, "getNCHOVersion Error: " + this.mNchoVersion);
                this.mNchoVersion = 0;
            }
        }
        if (this.mVerboseLoggingEnabled != 0) {
            Log.d(TAG, "getNCHOVersion Version: " + this.mNchoVersion);
        }
        return this.mNchoVersion;
    }

    private int getNCHO10State() {
        if (getNCHOVersion() != 1) {
            Log.e(TAG, "getNCHO10State version is not 1.0");
            return -1;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "getNCHO10State: " + this.mNcho10State);
        }
        return this.mNcho10State;
    }

    private boolean setNCHO10State(int state) {
        if (getNCHOVersion() != 1) {
            Log.e(TAG, "setNCHO10State version is not 1.0");
            return false;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setNCHO10State: " + state);
        }
        this.mNcho10State = state;
        return true;
    }

    private int getNCHO20State() {
        if (getNCHOVersion() != 2) {
            Log.e(TAG, "getNCHO20State version is not 2.0");
            return -1;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "getNCHO20State: " + this.mNcho20State);
        }
        return this.mNcho20State;
    }

    private boolean setNCHO20State(int state, boolean setToDriver) {
        boolean result = true;
        if (getNCHOVersion() != 2) {
            Log.e(TAG, "setNCHO20State version is not 2.0");
            return false;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setNCHO20State: " + state + ", setToDriver: " + setToDriver);
        }
        if (setToDriver) {
            result = this.mWifiNative.setNCHOMode(this.mInterfaceName, state);
        }
        if (result) {
            this.mNcho20State = state;
        } else {
            Log.e(TAG, "setNCHO20State setNCHOMode fail");
        }
        return result;
    }

    private boolean backUpNcho10Param() {
        if (getNCHOVersion() != 1) {
            Log.e(TAG, "backUpNcho10Param NCHO version is not 1.0");
            return false;
        } else if (getNCHO10State() == 1) {
            Log.e(TAG, "backUpNcho10Param already backed up");
            return false;
        } else {
            this.mDefaultRoamTrigger = this.mWifiNative.getRoamTrigger(this.mInterfaceName);
            if (this.mDefaultRoamTrigger == -1) {
                this.mDefaultRoamTrigger = -75;
            }
            this.mDefaultRoamDelta = this.mWifiNative.getRoamDelta(this.mInterfaceName);
            if (this.mDefaultRoamDelta == -1) {
                this.mDefaultRoamDelta = 10;
            }
            this.mDefaultRoamScanPeriod = this.mWifiNative.getRoamScanPeriod(this.mInterfaceName);
            if (this.mDefaultRoamScanPeriod == -1) {
                this.mDefaultRoamScanPeriod = 10;
            }
            setNCHO10State(1);
            Log.d(TAG, "ncho10BackUp: " + this.mDefaultRoamTrigger + "/" + this.mDefaultRoamDelta + "/" + this.mDefaultRoamScanPeriod);
            return true;
        }
    }

    private boolean restoreNcho10Param() {
        if (getNCHOVersion() != 1) {
            Log.e(TAG, "ncho10BackUp NCHO version is not 1.0");
            return false;
        } else if (getNCHO10State() != 1) {
            Log.e(TAG, "ncho 10 is not backed up");
            return false;
        } else {
            Log.d(TAG, "restoreNcho10Param: " + this.mDefaultRoamTrigger + "/" + this.mDefaultRoamDelta + "/" + this.mDefaultRoamScanPeriod);
            this.mWifiNative.setRoamTrigger(this.mInterfaceName, this.mDefaultRoamTrigger);
            this.mWifiNative.setRoamDelta(this.mInterfaceName, this.mDefaultRoamDelta);
            this.mWifiNative.setRoamScanPeriod(this.mInterfaceName, this.mDefaultRoamScanPeriod);
            return true;
        }
    }

    public int setRoamDhcpPolicy(int mode) {
        this.mRoamDhcpPolicy = mode;
        Log.d(TAG, "Set mRoamDhcpPolicy : " + this.mRoamDhcpPolicy);
        return this.mRoamDhcpPolicy;
    }

    public int getRoamDhcpPolicy() {
        Log.d(TAG, "Get mRoamDhcpPolicy : " + this.mRoamDhcpPolicy);
        return this.mRoamDhcpPolicy;
    }

    /* access modifiers changed from: package-private */
    public boolean isValidLocation(double latitude, double longitude) {
        if (latitude >= -90.0d && latitude <= 90.0d && longitude >= -180.0d && longitude <= 180.0d) {
            return true;
        }
        Log.d(TAG, "invalid location");
        return false;
    }

    /* access modifiers changed from: package-private */
    public void updateLocation(int isActiveRequest) {
        Log.d(TAG, "updateLocation " + isActiveRequest);
        this.mSemLocationManager = (SemLocationManager) this.mContext.getSystemService("sec_location");
        if (isActiveRequest == 1) {
            this.mSemLocationManager.requestSingleLocation(100, 30, false, this.mSemLocationListener);
            return;
        }
        this.mLocationPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_AP_LOCATION_PASSIVE_REQUEST), 0);
        this.mSemLocationManager.requestPassiveLocation(this.mLocationPendingIntent);
    }

    /* access modifiers changed from: private */
    public boolean isLocationSupportedAp(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if (config.semSamsungSpecificFlags.get(1) || config.semIsVendorSpecificSsid) {
            Log.d(TAG, "This is a Samsung Hotspot");
            return false;
        }
        if (this.mWifiInfo != null) {
            for (int mask : MHS_PRIVATE_NETWORK_MASK) {
                if ((this.mWifiInfo.getIpAddress() & 16777215) == mask) {
                    Log.d(TAG, "This is a Mobile Hotspot");
                    return false;
                }
            }
        }
        if (!config.semIsVendorSpecificSsid) {
            return true;
        }
        Log.d(TAG, "This is vendor AP");
        return false;
    }

    public double[] getLatitudeLongitude(WifiConfiguration config) {
        String latitudeLongitude = this.mWifiGeofenceManager.getLatitudeAndLongitude(config);
        double[] latitudeLongitudeDouble = new double[2];
        if (latitudeLongitude != null) {
            String[] latitudeLongitudeString = latitudeLongitude.split(":");
            latitudeLongitudeDouble[0] = Double.parseDouble(latitudeLongitudeString[0]);
            latitudeLongitudeDouble[1] = Double.parseDouble(latitudeLongitudeString[1]);
        } else {
            double d = INVALID_LATITUDE_LONGITUDE;
            latitudeLongitudeDouble[0] = d;
            latitudeLongitudeDouble[1] = d;
        }
        return latitudeLongitudeDouble;
    }

    public void checkAlternativeNetworksForWmc(boolean mNeedRoamingInHighQuality) {
        sendMessage(WifiConnectivityMonitor.CHECK_ALTERNATIVE_NETWORKS, mNeedRoamingInHighQuality);
    }

    public void startScanFromWcm() {
        this.mScanRequestProxy.startScan(1000, this.mContext.getOpPackageName());
    }

    /* access modifiers changed from: private */
    public boolean checkIfForceRestartDhcp() {
        if (getRoamDhcpPolicy() == 1) {
            if (DBG) {
                log("ForceRestartDhcp by uready");
            }
            return true;
        }
        String ssid = this.mWifiInfo.getSSID();
        if ((ssid == null || (!ssid.contains("marente") && !ssid.contains("0001docomo") && !ssid.contains("ollehWiFi") && !ssid.contains("olleh GiGA WiFi") && !ssid.contains("KT WiFi") && !ssid.contains("KT GiGA WiFi"))) && !ssid.contains("T wifi zone")) {
            return false;
        }
        if (DBG) {
            log("ForceRestartDhcp");
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void restartDhcp(WifiConfiguration wc) {
        if (wc == null) {
            Log.d(TAG, "Stop restarting Dhcp as currentConfig is null");
            return;
        }
        if (this.mIpClient != null) {
            this.mIpClient.stop();
        }
        setTcpBufferAndProxySettingsForIpManager();
        ProvisioningConfiguration prov = new ProvisioningConfiguration.Builder().withPreDhcpAction().withApfCapabilities(this.mWifiNative.getApfCapabilities(this.mInterfaceName)).withNetwork(getCurrentNetwork()).withDisplayName(wc.SSID).withRandomMacAddress().build();
        if (this.mIpClient != null) {
            this.mIpClient.startProvisioning(prov);
        }
    }

    /* access modifiers changed from: private */
    public void CheckIfDefaultGatewaySame() {
        Log.d(TAG, "CheckIfDefaultGatewaySame");
        removeMessages(CMD_CHECK_ARP_RESULT);
        removeMessages(CMD_SEND_ARP);
        InetAddress inetAddress = null;
        InetAddress gateway = null;
        Iterator<LinkAddress> it = this.mLinkProperties.getLinkAddresses().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LinkAddress la = it.next();
            if (la.getAddress() instanceof Inet4Address) {
                inetAddress = la.getAddress();
                break;
            }
        }
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route.getGateway() instanceof Inet4Address) {
                gateway = route.getGateway();
            }
        }
        if (inetAddress == null || gateway == null) {
            restartDhcp(getCurrentWifiConfiguration());
            return;
        }
        try {
            ArpPeer peer = new ArpPeer();
            peer.checkArpReply(this.mLinkProperties, ROAMING_ARP_TIMEOUT_MS, gateway.getAddress(), inetAddress.getAddress(), this.mWifiInfo.getMacAddress());
            if (SemCscFeature.getInstance().getBoolean(C0852CscFeatureTagCommon.TAG_CSCFEATURE_COMMON_SUPPORTCOMCASTWIFI)) {
                peer.sendGArp(this.mLinkProperties, inetAddress.getAddress(), this.mWifiInfo.getMacAddress());
            }
            sendMessageDelayed(CMD_SEND_ARP, 100);
            sendMessageDelayed(CMD_CHECK_ARP_RESULT, 2000);
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e);
        }
    }

    /* access modifiers changed from: private */
    public boolean isSupportRandomMac(WifiConfiguration config) {
        if (config != null) {
            String ssid = StringUtil.removeDoubleQuotes(config.SSID);
            if (config.isPasspoint()) {
                return false;
            }
            if (!config.semIsVendorSpecificSsid) {
                if (config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(4)) {
                }
                if (mOpBranding.getCountry() != 1 || (!"ollehWiFi ".equals(ssid) && !"olleh GiGA WiFi ".equals(ssid) && !"KT WiFi ".equals(ssid) && !"KT GiGA WiFi ".equals(ssid) && !"T wifi zone".equals(ssid) && !"U+zone".equals(ssid) && !"U+zone_5G".equals(ssid) && !"5G_U+zone".equals(ssid))) {
                    return true;
                }
                return false;
            } else if (OpBrandingLoader.Vendor.DCM != mOpBranding || (!"0000docomo".equals(ssid) && !"0001docomo".equals(ssid))) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    public void enablePollingRssiForSwitchboard(boolean enable, int newPollIntervalMsecs) {
        if (enable) {
            mRssiPollingScreenOffEnabled |= 2;
        } else {
            mRssiPollingScreenOffEnabled &= -3;
        }
        setPollRssiIntervalMsecs(newPollIntervalMsecs);
        if (mRssiPollingScreenOffEnabled != 0) {
            if (!this.mEnableRssiPolling) {
                enableRssiPolling(true);
            }
        } else if (this.mEnableRssiPolling && !this.mScreenOn) {
            enableRssiPolling(false);
        }
    }

    public int getBeaconCount() {
        return this.mRunningBeaconCount;
    }

    /* access modifiers changed from: private */
    public void setRoamTriggered(boolean enabled) {
        this.mIsRoaming = enabled;
        if (enabled) {
            handleWifiNetworkCallbacks(5);
            try {
                if (!this.mIsRoamNetwork) {
                    if (checkAndSetConnectivityInstance() && !isWifiOnly()) {
                        this.mCm.setWifiRoamNetwork(true);
                    }
                    this.mIsRoamNetwork = true;
                    Log.i(TAG, "Roam Network");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            handleWifiNetworkCallbacks(6);
        }
    }

    /* access modifiers changed from: private */
    public boolean isRoaming() {
        return this.mIsRoaming;
    }

    private boolean detectIpv6ProvisioningFailure(LinkProperties oldLp, LinkProperties newLp) {
        if (oldLp == null || newLp == null) {
            return false;
        }
        boolean lostIPv6 = oldLp.isIpv6Provisioned() && !newLp.isIpv6Provisioned();
        boolean lostIPv4Address = oldLp.hasIpv4Address() && !newLp.hasIpv4Address();
        boolean lostIPv6Router = oldLp.hasIpv6DefaultRoute() && !newLp.hasIpv6DefaultRoute();
        if (lostIPv4Address) {
            return false;
        }
        if (lostIPv6) {
            Log.d(TAG, "lostIPv6");
            return true;
        } else if (!oldLp.hasGlobalIpv6Address() || !lostIPv6Router) {
            return false;
        } else {
            Log.d(TAG, "return true by ipv6 provisioning failure");
            return true;
        }
    }
}
