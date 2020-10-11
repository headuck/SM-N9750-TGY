package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SemSystemProperties;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.feature.SemFloatingFeature;
import com.samsung.android.net.wifi.SemWifiApMacInfo;
import com.samsung.android.net.wifi.SemWifiApRestoreHelper;
import com.samsung.android.server.wifi.CscParser;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WifiApConfigStore {
    public static final String ACTION_HOTSPOT_CONFIG_USER_TAPPED_CONTENT = "com.android.server.wifi.WifiApConfigStoreUtil.HOTSPOT_CONFIG_USER_TAPPED_CONTENT";
    @VisibleForTesting
    public static final int AP_CHANNEL_DEFAULT = 0;
    private static final int AP_CONFIG_FILE_VERSION = 4;
    private static String CONFIGMOBILEAPDEFAULTPWD = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGMOBILEAPDEFAULTPWD, "SamsungDefault");
    private static String CONFIGMOBILEAPDEFAULTSSID = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGMOBILEAPDEFAULTSSID, "Default,Mac4Digits");
    public static final String CONFIGOPBRANDINGFORMOBILEAP = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDINGFORMOBILEAP, "ALL");
    private static final boolean DBG = ("eng".equals(Build.TYPE) || Debug.semIsProductDev());
    private static final String DEFAULT_AP_CONFIG_FILE = (Environment.getDataDirectory() + "/misc/wifi/softap.conf");
    private static final int DEFAULT_MAX_CLIENT = Integer.parseInt("10");
    public static final int MAX_CLIENT = SemCscFeature.getInstance().getInteger(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_MAXCLIENT4MOBILEAP, DEFAULT_MAX_CLIENT);
    @VisibleForTesting
    public static final int PSK_MAX_LEN = 63;
    @VisibleForTesting
    public static final int PSK_MIN_LEN = 8;
    private static final int RAND_SSID_INT_MAX = 9999;
    private static final int RAND_SSID_INT_MIN = 1000;
    private static boolean SPF_SupportMobileApDualPassword = false;
    @VisibleForTesting
    public static final int SSID_MAX_LEN = 32;
    @VisibleForTesting
    public static final int SSID_MIN_LEN = 1;
    private static final String TAG = "WifiApConfigStore";
    private static final String errPWD = "\tUSER#DEFINED#PWD#\n";
    private static final String errSSID = "\t#ERROR#SSID#\n";
    private String DEFAULTSSIDNPWD;
    private boolean USERANDOM4DIGITCOMBINATIONASSSID;
    private boolean isJDMDevice;
    private ArrayList<Integer> mAllowed2GChannel;
    private final String mApConfigFile;
    private final BackupManagerProxy mBackupManagerProxy;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private List<String> mMHSDumpLogs;
    /* access modifiers changed from: private */
    public String mMacAddress;
    private int mRVFMode;
    private boolean mRequiresApBandConversion;
    private WifiConfiguration mWifiApConfig;

    WifiApConfigStore(Context context, Looper looper, BackupManagerProxy backupManagerProxy, FrameworkFacade frameworkFacade) {
        this(context, looper, backupManagerProxy, frameworkFacade, DEFAULT_AP_CONFIG_FILE);
    }

    WifiApConfigStore(Context context, Looper looper, BackupManagerProxy backupManagerProxy, FrameworkFacade frameworkFacade, String apConfigFile) {
        this.DEFAULTSSIDNPWD = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DEFAULTSSIDNPWD);
        this.USERANDOM4DIGITCOMBINATIONASSSID = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_USERANDOM4DIGITCOMBINATIONASSSID);
        this.isJDMDevice = "in_house".contains("jdm");
        this.mWifiApConfig = null;
        this.mAllowed2GChannel = null;
        this.mRVFMode = 0;
        this.mRequiresApBandConversion = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
            /* JADX WARNING: Removed duplicated region for block: B:23:0x0090  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(android.content.Context r5, android.content.Intent r6) {
                /*
                    r4 = this;
                    java.lang.String r0 = r6.getAction()
                    int r1 = r0.hashCode()
                    r2 = -1875733435(0xffffffff90329445, float:-3.5218533E-29)
                    r3 = 1
                    if (r1 == r2) goto L_0x001e
                    r2 = 765958520(0x2da79978, float:1.9053856E-11)
                    if (r1 == r2) goto L_0x0014
                L_0x0013:
                    goto L_0x0028
                L_0x0014:
                    java.lang.String r1 = "com.android.server.wifi.WifiApConfigStoreUtil.HOTSPOT_CONFIG_USER_TAPPED_CONTENT"
                    boolean r0 = r0.equals(r1)
                    if (r0 == 0) goto L_0x0013
                    r0 = 0
                    goto L_0x0029
                L_0x001e:
                    java.lang.String r1 = "android.net.wifi.WIFI_STATE_CHANGED"
                    boolean r0 = r0.equals(r1)
                    if (r0 == 0) goto L_0x0013
                    r0 = r3
                    goto L_0x0029
                L_0x0028:
                    r0 = -1
                L_0x0029:
                    if (r0 == 0) goto L_0x0090
                    if (r0 == r3) goto L_0x0048
                    java.lang.StringBuilder r0 = new java.lang.StringBuilder
                    r0.<init>()
                    java.lang.String r1 = "Unknown action "
                    r0.append(r1)
                    java.lang.String r1 = r6.getAction()
                    r0.append(r1)
                    java.lang.String r0 = r0.toString()
                    java.lang.String r1 = "WifiApConfigStore"
                    android.util.Log.e(r1, r0)
                    goto L_0x0096
                L_0x0048:
                    r0 = 4
                    java.lang.String r1 = "wifi_state"
                    int r0 = r6.getIntExtra(r1, r0)
                    r1 = 3
                    if (r0 != r1) goto L_0x0096
                    com.android.server.wifi.WifiApConfigStore r0 = com.android.server.wifi.WifiApConfigStore.this
                    java.lang.String r0 = r0.mMacAddress
                    if (r0 != 0) goto L_0x0096
                    com.android.server.wifi.WifiApConfigStore r0 = com.android.server.wifi.WifiApConfigStore.this
                    com.samsung.android.net.wifi.SemWifiApMacInfo r1 = com.samsung.android.net.wifi.SemWifiApMacInfo.getInstance()
                    java.lang.String r1 = r1.readWifiMacInfo()
                    java.lang.String unused = r0.mMacAddress = r1
                    com.android.server.wifi.WifiApConfigStore r0 = com.android.server.wifi.WifiApConfigStore.this
                    java.lang.String r0 = r0.mMacAddress
                    if (r0 != 0) goto L_0x0096
                    com.android.server.wifi.WifiInjector r0 = com.android.server.wifi.WifiInjector.getInstance()
                    com.android.server.wifi.ClientModeImpl r0 = r0.getClientModeImpl()
                    java.lang.String r0 = r0.getFactoryMacAddress()
                    if (r0 == 0) goto L_0x008f
                    com.android.server.wifi.WifiApConfigStore r1 = com.android.server.wifi.WifiApConfigStore.this
                    java.lang.String unused = r1.mMacAddress = r0
                    com.samsung.android.net.wifi.SemWifiApMacInfo r1 = com.samsung.android.net.wifi.SemWifiApMacInfo.getInstance()
                    com.android.server.wifi.WifiApConfigStore r2 = com.android.server.wifi.WifiApConfigStore.this
                    java.lang.String r2 = r2.mMacAddress
                    r1.writeWifiMacInfo(r2)
                L_0x008f:
                    goto L_0x0096
                L_0x0090:
                    com.android.server.wifi.WifiApConfigStore r0 = com.android.server.wifi.WifiApConfigStore.this
                    r0.handleUserHotspotConfigTappedContent()
                L_0x0096:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiApConfigStore.C04211.onReceive(android.content.Context, android.content.Intent):void");
            }
        };
        this.mMacAddress = null;
        this.mMHSDumpLogs = new ArrayList();
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mBackupManagerProxy = backupManagerProxy;
        this.mFrameworkFacade = frameworkFacade;
        this.mApConfigFile = apConfigFile;
        String ap2GChannelListStr = this.mContext.getResources().getString(17040003);
        Log.d(TAG, "2G band allowed channels are:" + ap2GChannelListStr);
        if (ap2GChannelListStr != null) {
            this.mAllowed2GChannel = new ArrayList<>();
            for (String tmp : ap2GChannelListStr.split(",")) {
                try {
                    this.mAllowed2GChannel.add(Integer.valueOf(Integer.parseInt(tmp)));
                } catch (NumberFormatException e) {
                    Log.d(TAG, "NumberFormatException for ap2GChannelListStr " + tmp);
                }
            }
        }
        this.mRequiresApBandConversion = this.mContext.getResources().getBoolean(17891596);
        this.mWifiApConfig = loadApConfiguration(this.mApConfigFile);
        if (this.mWifiApConfig == null) {
            Log.d(TAG, "Fallback to use default AP configuration");
            this.mWifiApConfig = getDefaultApConfiguration();
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
            if (!errSSID.equals(this.mWifiApConfig.SSID)) {
                addMHSDumpLog("SSID is not error");
                SemWifiApRestoreHelper.setApConfiguration(this.mContext, this.mWifiApConfig);
            } else {
                addMHSDumpLog("SSID is error. do not save");
            }
        } else if (errSSID.equals(SemWifiApRestoreHelper.getSSID(this.mContext))) {
            addMHSDumpLog("save SSID is error");
            String newSSID = parseSecProductFeatureSsid("XXXX", false);
            addMHSDumpLog("save new ssid " + newSSID);
            SemWifiApRestoreHelper.setSSID(this.mContext, newSSID);
        }
        boolean singleSkuActivated = "true".equals(SemSystemProperties.get("mdc.singlesku.activated", "false"));
        int isTSSActivationHandled = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_Tss_handled", -1);
        addMHSDumpLog("isTSSActivationHandled:" + isTSSActivationHandled + " singleSkuActivated:" + singleSkuActivated);
        if (isTSSActivationHandled == -1) {
            if (singleSkuActivated) {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_Tss_handled", 1);
            } else {
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_Tss_handled", 0);
            }
        } else if (isTSSActivationHandled == 0 && singleSkuActivated) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_Tss_handled", 1);
            Log.d(TAG, "Generate default for TSS");
            this.mWifiApConfig = getDefaultApConfiguration();
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
            if (!errSSID.equals(this.mWifiApConfig.SSID)) {
                addMHSDumpLog("TSS activated SSID is not error");
                SemWifiApRestoreHelper.setApConfiguration(this.mContext, this.mWifiApConfig);
            } else {
                addMHSDumpLog("TSS activated SSID is error. do not save");
            }
        }
        if (!SPF_SupportMobileApDualPassword) {
        } else if (this.mWifiApConfig.guestPreSharedKey == null || this.mWifiApConfig.guestPreSharedKey.isEmpty()) {
            WifiConfiguration wifiConfiguration = this.mWifiApConfig;
            StringBuilder sb = new StringBuilder();
            boolean z = singleSkuActivated;
            sb.append(getRandomAlphabet(4, 0));
            sb.append(getRandomDigits(4, 1));
            wifiConfiguration.guestPreSharedKey = sb.toString();
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        } else {
            boolean z2 = singleSkuActivated;
        }
        if (DBG) {
            Log.d(TAG, " mWifiApConfig SSID: " + this.mWifiApConfig.SSID + " pwd:" + this.mWifiApConfig.preSharedKey + ",security:" + this.mWifiApConfig.getAuthType() + "guestKey:" + this.mWifiApConfig.guestPreSharedKey);
        }
        if (isCustomerChanged()) {
            this.mWifiApConfig.maxclient = MAX_CLIENT;
            if (!"TMO".equals(CONFIGOPBRANDINGFORMOBILEAP) && !"NEWCO".equals(CONFIGOPBRANDINGFORMOBILEAP) && this.mWifiApConfig.preSharedKey != null && this.mWifiApConfig.allowedKeyManagement != null && this.mWifiApConfig.preSharedKey.equals(errPWD) && this.mWifiApConfig.allowedKeyManagement.get(4)) {
                String password = parseSecProductFeaturePassword(true);
                if (password == null) {
                    this.mWifiApConfig.preSharedKey = getRandomAlphabet(4, 0) + getRandomDigits(4, 1);
                } else {
                    this.mWifiApConfig.preSharedKey = password;
                }
            }
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
            if (!errSSID.equals(this.mWifiApConfig.SSID)) {
                addMHSDumpLog("customerchanged SSID is not error");
                SemWifiApRestoreHelper.setApConfiguration(this.mContext, this.mWifiApConfig);
            } else {
                addMHSDumpLog("customerchanged SSID is error. do not save");
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HOTSPOT_CONFIG_USER_TAPPED_CONTENT);
        if (this.isJDMDevice) {
            filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        }
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter, (String) null, this.mHandler);
    }

    private String getWifiMacAddress() {
        try {
            if (this.mMacAddress == null) {
                this.mMacAddress = SemWifiApMacInfo.getInstance().readWifiMacInfo();
            }
            return this.mMacAddress;
        } catch (Exception e) {
            Log.d(TAG, "JDM MAC error" + e);
            return null;
        }
    }

    private boolean isSimCheck() {
        if (!DBG || !SystemProperties.get("SimCheck.disable").equals("1")) {
            return true;
        }
        return false;
    }

    private String getCSCRegion() {
        return new CscParser(CscParser.getCustomerPath()).get("GeneralInfo.Region");
    }

    public synchronized WifiConfiguration getApConfiguration() {
        WifiConfiguration config = apBandCheckConvert(this.mWifiApConfig);
        if (this.mWifiApConfig != config) {
            Log.d(TAG, "persisted config was converted, need to resave it");
            this.mWifiApConfig = config;
            persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
        }
        if (this.mWifiApConfig != null) {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            boolean isNoSimBlocked = false;
            String region = getCSCRegion();
            if (region != null && region.equals("KOR") && isSimCheck() && telephonyManager.getSimState() != 5) {
                isNoSimBlocked = true;
            }
            if (errSSID.equals(this.mWifiApConfig.SSID)) {
                if (this.isJDMDevice || !isNoSimBlocked) {
                    Log.d(TAG, "isNoSimBlocked:" + isNoSimBlocked);
                    addMHSDumpLog("isNoSimBlocked:" + isNoSimBlocked);
                    reGenerateAndWriteConfiguration();
                }
            } else if (!this.USERANDOM4DIGITCOMBINATIONASSSID && "".equals(this.mWifiApConfig.SSID)) {
                Log.d(TAG, " SSID is empty");
                addMHSDumpLog("SSID is empty");
                reGenerateAndWriteConfiguration();
            } else if (this.USERANDOM4DIGITCOMBINATIONASSSID && this.mWifiApConfig.SSID.equals("")) {
                generateDefaultSSID();
            }
        }
        if (DBG) {
            if (this.mWifiApConfig == null) {
                Log.e(TAG, "getWifiApConfiguration return null");
            } else {
                Log.d(TAG, " getWifiApConfiguration mWifiApConfig SSID: " + this.mWifiApConfig.SSID + " pwd:" + this.mWifiApConfig.preSharedKey + ",security:" + this.mWifiApConfig.getAuthType() + "guestpassword:" + this.mWifiApConfig.guestPreSharedKey);
            }
        } else if (this.mWifiApConfig == null) {
            Log.e(TAG, "getWifiApConfiguration return null");
        } else {
            Log.e(TAG, "getWifiApConfiguration ssid");
        }
        return this.mWifiApConfig;
    }

    public synchronized void setApConfiguration(WifiConfiguration config) {
        Log.i(TAG, "setApConfiguration() - Start");
        if (errSSID.equals(SemWifiApRestoreHelper.getSSID(this.mContext))) {
            Log.i(TAG, "setApConfiguration() - SSID before updating is errSSID");
            SemWifiApRestoreHelper.setSSID(this.mContext, parseSecProductFeatureSsid("XXXX", false));
        }
        if (config == null) {
            this.mWifiApConfig = getDefaultApConfiguration();
        } else {
            if (SPF_SupportMobileApDualPassword && (config.guestPreSharedKey == null || (config.guestPreSharedKey != null && config.guestPreSharedKey.isEmpty()))) {
                config.guestPreSharedKey = getRandomAlphabet(4, 0) + getRandomDigits(4, 1);
            }
            this.mWifiApConfig = apBandCheckConvert(config);
            config.semChannel = config.apChannel;
            this.mWifiApConfig = config;
            Log.d(TAG, "Before mWifiApConfig.getAuthType()=" + this.mWifiApConfig.getAuthType());
            if (this.mWifiApConfig.getAuthType() == 1) {
                if (this.mWifiApConfig.preSharedKey == null || this.mWifiApConfig.preSharedKey.length() < 8 || config.preSharedKey.length() > 32) {
                    this.mWifiApConfig.allowedKeyManagement.clear(1);
                    this.mWifiApConfig.allowedKeyManagement.clear(4);
                    this.mWifiApConfig.allowedKeyManagement.set(0);
                    Log.e(TAG, " conf changed to none from wpa");
                } else {
                    this.mWifiApConfig.allowedKeyManagement.set(4);
                    this.mWifiApConfig.allowedKeyManagement.clear(1);
                    this.mWifiApConfig.allowedKeyManagement.clear(0);
                    Log.e(TAG, " conf changed to wpa2 from wpa");
                }
            }
            Log.d(TAG, "After mWifiApConfig.getAuthType()=" + this.mWifiApConfig.getAuthType());
        }
        addMHSDumpLog(" setApConfiguration() " + this.mWifiApConfig.SSID + " " + this.mWifiApConfig.apChannel + " " + this.mWifiApConfig.semChannel);
        persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
    }

    public ArrayList<Integer> getAllowed2GChannel() {
        return this.mAllowed2GChannel;
    }

    public void notifyUserOfApBandConversion(String packageName) {
        Log.w(TAG, "ready to post notification - triggered by " + packageName);
        ((NotificationManager) this.mContext.getSystemService("notification")).notify(50, createConversionNotification());
    }

    private Notification createConversionNotification() {
        CharSequence title = this.mContext.getResources().getText(17042735);
        CharSequence contentSummary = this.mContext.getResources().getText(17042737);
        CharSequence content = this.mContext.getResources().getText(17042736);
        return new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS).setSmallIcon(17302996).setPriority(1).setCategory("sys").setContentTitle(title).setContentText(contentSummary).setContentIntent(getPrivateBroadcast(ACTION_HOTSPOT_CONFIG_USER_TAPPED_CONTENT)).setTicker(title).setShowWhen(false).setLocalOnly(true).setColor(this.mContext.getResources().getColor(17170460, this.mContext.getTheme())).setStyle(new Notification.BigTextStyle().bigText(content).setBigContentTitle(title).setSummaryText(contentSummary)).build();
    }

    private WifiConfiguration apBandCheckConvert(WifiConfiguration config) {
        if (this.mRequiresApBandConversion) {
            if (config.apBand == 1) {
                Log.w(TAG, "Supplied ap config band was 5GHz only, converting to ANY");
                WifiConfiguration convertedConfig = new WifiConfiguration(config);
                convertedConfig.apBand = -1;
                convertedConfig.apChannel = 0;
                return convertedConfig;
            }
        } else if (config.apBand == -1) {
            Log.w(TAG, "Supplied ap config band was ANY, converting to 5GHz");
            WifiConfiguration convertedConfig2 = new WifiConfiguration(config);
            convertedConfig2.apBand = 1;
            convertedConfig2.apChannel = 0;
            return convertedConfig2;
        }
        return config;
    }

    public synchronized int getApTxPower() {
        int txPowerMode;
        txPowerMode = -1;
        if (this.mWifiApConfig != null) {
            txPowerMode = this.mWifiApConfig.txPowerMode;
        }
        return txPowerMode;
    }

    public synchronized void setApTxPower(int txPowerMode) {
        if (this.mWifiApConfig != null) {
            this.mWifiApConfig.txPowerMode = txPowerMode;
        }
    }

    public synchronized void setWifiApConfigurationToDefault() {
        this.mWifiApConfig = getDefaultApConfiguration();
        setApConfiguration(this.mWifiApConfig);
    }

    private void persistConfigAndTriggerBackupManagerProxy(WifiConfiguration config) {
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        this.mBackupManagerProxy.notifyDataChanged();
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    public synchronized boolean isCustomerChanged() {
        String preCustomer = Settings.Secure.getString(this.mContext.getContentResolver(), "wifi_ap_customer");
        addMHSDumpLog("isCustomerChanged() pre:" + preCustomer + " Curr:" + CONFIGOPBRANDINGFORMOBILEAP);
        if (preCustomer == null) {
            Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_customer", CONFIGOPBRANDINGFORMOBILEAP);
            return false;
        } else if (preCustomer.equals(CONFIGOPBRANDINGFORMOBILEAP)) {
            return false;
        } else {
            if (TextUtils.isEmpty(preCustomer)) {
                Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_customer", CONFIGOPBRANDINGFORMOBILEAP);
                return false;
            }
            addMHSDumpLog(" diff, changed  return true");
            addMHSDumpLog(" put :" + CONFIGOPBRANDINGFORMOBILEAP);
            Settings.Secure.putString(this.mContext.getContentResolver(), "wifi_ap_customer", CONFIGOPBRANDINGFORMOBILEAP);
            return true;
        }
    }

    private WifiConfiguration loadApConfiguration(String filename) {
        WifiConfiguration config;
        StringBuilder sb;
        DataInputStream in = null;
        try {
            config = new WifiConfiguration();
            DataInputStream in2 = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            int version = in2.readInt();
            config.SSID = in2.readUTF();
            Log.d(TAG, "config.SSID:" + config.SSID);
            addMHSDumpLog("loadApConfiguration config.SSID:" + config.SSID);
            config.hiddenSSID = in2.readBoolean();
            config.apChannel = in2.readInt();
            config.semChannel = config.apChannel;
            if (version == 3) {
                if (config.apChannel < 14) {
                    config.apBand = 0;
                } else {
                    config.apBand = 1;
                }
            }
            if (version == 4) {
                config.apBand = in2.readInt();
            }
            config.macaddrAcl = in2.readInt();
            config.macaddrAcl = 3;
            config.maxclient = in2.readInt();
            if (config.maxclient == 0) {
                config.maxclient = MAX_CLIENT;
            }
            config.vendorIE = in2.readInt();
            config.apIsolate = in2.readInt();
            config.wpsStatus = in2.readInt();
            config.txPowerMode = in2.readInt();
            int authType = in2.readInt();
            config.allowedKeyManagement.set(authType);
            if (authType != 0) {
                config.preSharedKey = in2.readUTF();
            }
            if (SPF_SupportMobileApDualPassword) {
                config.guestPreSharedKey = in2.readUTF();
            }
            try {
                in2.close();
            } catch (IOException e) {
                e = e;
                sb = new StringBuilder();
            }
        } catch (IOException e2) {
            Log.e(TAG, "Error reading hotspot configuration " + e2);
            addMHSDumpLog("Error reading hotspot configuration " + e2);
            if (DBG) {
                Log.e(TAG, "Exception: " + e2);
            }
            if (4 < 4) {
                config = loadApConfigurationOldVer(filename);
            } else {
                config = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    e = e3;
                    sb = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e4) {
                    Log.e(TAG, "Error closing hotspot configuration during read" + e4);
                }
            }
            throw th;
        }
        return config;
        sb.append("Error closing hotspot configuration during read");
        sb.append(e);
        Log.e(TAG, sb.toString());
        return config;
    }

    private static WifiConfiguration loadApConfigurationOldVer(String filename) {
        WifiConfiguration config;
        Log.d(TAG, "loadApConfigurationOldVer()");
        DataInputStream in = null;
        try {
            config = new WifiConfiguration();
            DataInputStream in2 = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            int readInt = in2.readInt();
            config.SSID = in2.readUTF();
            config.hiddenSSID = in2.readBoolean();
            config.apChannel = in2.readInt();
            config.semChannel = config.apChannel;
            config.macaddrAcl = in2.readInt();
            config.macaddrAcl = 3;
            config.maxclient = in2.readInt();
            if (config.maxclient == 0) {
                config.maxclient = MAX_CLIENT;
            }
            int authType = in2.readInt();
            if (authType == 1) {
                config.allowedKeyManagement.set(4);
                Log.d(TAG, " conf changed to wpa2 from wpa");
            } else {
                config.allowedKeyManagement.set(authType);
            }
            if (authType != 0) {
                config.preSharedKey = in2.readUTF();
            }
            try {
                in2.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            Log.e(TAG, "loadApConfigurationOldVer() : IOException");
            config = null;
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                }
            }
            throw th;
        }
        return config;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:26:0x008d, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0096, code lost:
        throw r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void writeApConfiguration(java.lang.String r5, android.net.wifi.WifiConfiguration r6) {
        /*
            java.lang.String r0 = "WifiApConfigStore"
            java.io.DataOutputStream r1 = new java.io.DataOutputStream     // Catch:{ IOException -> 0x0097 }
            java.io.BufferedOutputStream r2 = new java.io.BufferedOutputStream     // Catch:{ IOException -> 0x0097 }
            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch:{ IOException -> 0x0097 }
            r3.<init>(r5)     // Catch:{ IOException -> 0x0097 }
            r2.<init>(r3)     // Catch:{ IOException -> 0x0097 }
            r1.<init>(r2)     // Catch:{ IOException -> 0x0097 }
            r2 = 4
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            java.lang.String r2 = r6.SSID     // Catch:{ all -> 0x008b }
            if (r2 == 0) goto L_0x001f
            java.lang.String r2 = r6.SSID     // Catch:{ all -> 0x008b }
            r1.writeUTF(r2)     // Catch:{ all -> 0x008b }
        L_0x001f:
            int r2 = r6.macaddrAcl     // Catch:{ all -> 0x008b }
            r3 = 1
            if (r2 != r3) goto L_0x0027
            r2 = 3
            r6.macaddrAcl = r2     // Catch:{ all -> 0x008b }
        L_0x0027:
            boolean r2 = r6.hiddenSSID     // Catch:{ all -> 0x008b }
            r1.writeBoolean(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.apChannel     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.apBand     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.macaddrAcl     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.maxclient     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.vendorIE     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.apIsolate     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.wpsStatus     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.txPowerMode     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            int r2 = r6.getAuthType()     // Catch:{ all -> 0x008b }
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x008b }
            r3.<init>()     // Catch:{ all -> 0x008b }
            java.lang.String r4 = " authType="
            r3.append(r4)     // Catch:{ all -> 0x008b }
            r3.append(r2)     // Catch:{ all -> 0x008b }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x008b }
            android.util.Log.d(r0, r3)     // Catch:{ all -> 0x008b }
            r1.writeInt(r2)     // Catch:{ all -> 0x008b }
            if (r2 == 0) goto L_0x007a
            java.lang.String r3 = r6.preSharedKey     // Catch:{ all -> 0x008b }
            if (r3 == 0) goto L_0x007a
            java.lang.String r3 = r6.preSharedKey     // Catch:{ all -> 0x008b }
            r1.writeUTF(r3)     // Catch:{ all -> 0x008b }
        L_0x007a:
            boolean r3 = SPF_SupportMobileApDualPassword     // Catch:{ all -> 0x008b }
            if (r3 == 0) goto L_0x0087
            java.lang.String r3 = r6.guestPreSharedKey     // Catch:{ all -> 0x008b }
            if (r3 == 0) goto L_0x0087
            java.lang.String r3 = r6.guestPreSharedKey     // Catch:{ all -> 0x008b }
            r1.writeUTF(r3)     // Catch:{ all -> 0x008b }
        L_0x0087:
            r1.close()     // Catch:{ IOException -> 0x0097 }
            goto L_0x00ac
        L_0x008b:
            r2 = move-exception
            throw r2     // Catch:{ all -> 0x008d }
        L_0x008d:
            r3 = move-exception
            r1.close()     // Catch:{ all -> 0x0092 }
            goto L_0x0096
        L_0x0092:
            r4 = move-exception
            r2.addSuppressed(r4)     // Catch:{ IOException -> 0x0097 }
        L_0x0096:
            throw r3     // Catch:{ IOException -> 0x0097 }
        L_0x0097:
            r1 = move-exception
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Error writing hotspot configuration"
            r2.append(r3)
            r2.append(r1)
            java.lang.String r2 = r2.toString()
            android.util.Log.e(r0, r2)
        L_0x00ac:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiApConfigStore.writeApConfiguration(java.lang.String, android.net.wifi.WifiConfiguration):void");
    }

    private WifiConfiguration getDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.apBand = 0;
        config.apChannel = 0;
        if (this.USERANDOM4DIGITCOMBINATIONASSSID) {
            config.SSID = "";
        } else {
            config.SSID = parseSecProductFeatureSsid(errSSID, true);
        }
        if (!"None".equals(CONFIGMOBILEAPDEFAULTPWD)) {
            String password = parseSecProductFeaturePassword(false);
            if (password == null) {
                config.SSID = errSSID;
                if ("SPRINT".equals(CONFIGOPBRANDINGFORMOBILEAP)) {
                    config.allowedKeyManagement.set(4);
                    config.preSharedKey = getRandomDigits(10, 1);
                }
            } else {
                config.allowedKeyManagement.set(4);
                if ("UserDefined".equals(CONFIGMOBILEAPDEFAULTPWD)) {
                    config.preSharedKey = errPWD;
                } else {
                    config.preSharedKey = password;
                }
            }
        } else {
            config.allowedKeyManagement.set(0);
        }
        if (SPF_SupportMobileApDualPassword) {
            config.guestPreSharedKey = getRandomAlphabet(4, 0) + getRandomDigits(4, 1);
        }
        config.hiddenSSID = false;
        config.semChannel = config.apChannel;
        config.maxclient = MAX_CLIENT;
        config.wpsStatus = 1;
        config.txPowerMode = 1;
        return config;
    }

    private String parseSecProductFeatureSsid(String errString, boolean flagErrorBreak) {
        String temp;
        String str = errString;
        StringBuilder ssid = new StringBuilder();
        String defaultSsid = CONFIGMOBILEAPDEFAULTSSID;
        addMHSDumpLog(" parseSecProductFeatureSsid() " + defaultSsid);
        String[] customSSID = defaultSsid.split(",");
        for (int i = 0; i < customSSID.length; i++) {
            if ("Default".equals(customSSID[i]) || "AndroidHotspot".equals(customSSID[i])) {
                if ("XXXX".equals(str)) {
                    temp = SemFloatingFeature.getInstance().getString("SEC_FLOATING_FEATURE_SETTINGS_CONFIG_BRAND_NAME");
                } else {
                    temp = Settings.Global.getString(this.mContext.getContentResolver(), "device_name");
                }
                addMHSDumpLog("global.device_name : " + temp);
                if ("".equals(temp)) {
                    ssid.append(getModelName());
                } else {
                    ssid.append(temp);
                }
            } else if ("ModelName".equals(customSSID[i])) {
                ssid.append(getModelName());
            } else if ("Random4Digits".equals(customSSID[i])) {
                ssid.append(getRandomDigits(4, 0));
            } else if ("Mac4Digits".equals(customSSID[i])) {
                String mac = getMacAddressLastDigits(4);
                if (mac != null) {
                    ssid.append(mac);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("Mac3Digits".equals(customSSID[i])) {
                String mac2 = getMacAddressLastDigits(3);
                if (mac2 != null) {
                    ssid.append(mac2);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("Mac2Digits".equals(customSSID[i])) {
                String mac3 = getMacAddressLastDigits(2);
                if (mac3 != null) {
                    ssid.append(mac3);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("Min4Digits".equals(customSSID[i])) {
                String min = getTelephonyNumber(4, false);
                if (min != null) {
                    ssid.append(min);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    String region = getCSCRegion();
                    if (region == null || !region.equals("KOR")) {
                        ssid.append(getRandomDigits(4, 0));
                    } else {
                        String mac4 = getMacAddressLast6DigitsForKOR();
                        if (mac4 != null) {
                            ssid.append(mac4);
                        } else {
                            ssid.append(str);
                        }
                    }
                }
            } else if ("Min2Digits".equals(customSSID[i])) {
                String min2 = getTelephonyNumber(2, false);
                if (min2 != null) {
                    ssid.append(min2);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("IMEILast2Digits".equals(customSSID[i])) {
                String imei = getLastIMEI(2);
                if (imei != null) {
                    ssid.append(imei);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("IMEILast4Digits".equals(customSSID[i])) {
                String imei2 = getLastIMEI(4);
                if (imei2 != null) {
                    ssid.append(imei2);
                } else if (flagErrorBreak) {
                    return str;
                } else {
                    ssid.append(str);
                }
            } else if ("Space".equals(customSSID[i])) {
                ssid.append(" ");
            } else if ("BrandName".equals(customSSID[i])) {
                String temp2 = SemFloatingFeature.getInstance().getString("SEC_FLOATING_FEATURE_SETTINGS_CONFIG_BRAND_NAME");
                if ("".equals(temp2)) {
                    ssid.append("brand");
                } else {
                    ssid.append(temp2);
                }
            } else if ("ChameleonSSID".equals(customSSID[i])) {
                ContentResolver cr = this.mContext.getContentResolver();
                Log.d(TAG, "updateHotspotS");
                String cscSSID = Settings.System.getString(cr, "chameleon_ssid");
                if (cscSSID == null || "null".equals(cscSSID)) {
                    Log.d(TAG, "ChameleonSSID is null -> ModelName is used.");
                    ssid.append(getModelName());
                } else {
                    ssid.append(cscSSID);
                    addMHSDumpLog(" parseSecProductFeatureSsid() ssid:" + ssid.toString());
                    return ssid.toString();
                }
            } else {
                ssid.append(customSSID[i]);
            }
        }
        addMHSDumpLog(" parseSecProductFeatureSsid() ssid:" + ssid.toString());
        if (ssid.toString().getBytes().length > 32) {
            String modifiedSsid = ssid.toString();
            if (modifiedSsid.getBytes().length > modifiedSsid.length()) {
                int index = 0;
                int mCharCounter = 0;
                int mByteCounter = 0;
                while (mByteCounter <= 32) {
                    mCharCounter = Character.charCount(modifiedSsid.codePointAt(index));
                    mByteCounter += modifiedSsid.substring(index, index + mCharCounter).getBytes().length;
                    index += mCharCounter;
                }
                return modifiedSsid.substring(0, index - mCharCounter);
            } else if (modifiedSsid.length() > 32) {
                return modifiedSsid.substring(0, 32);
            }
        }
        return ssid.toString();
    }

    /* Debug info: failed to restart local var, previous not found, register: 8 */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00a5, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0132, code lost:
        return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized java.lang.String parseSecProductFeaturePassword(boolean r9) {
        /*
            r8 = this;
            monitor-enter(r8)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r0.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = " parseSecProductFeaturePassword() "
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = " retry:"
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            r0.append(r9)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x01b0 }
            r8.addMHSDumpLog(r0)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = "SamsungDefault"
            java.lang.String r1 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r1)     // Catch:{ all -> 0x01b0 }
            r1 = 0
            r3 = 1
            r5 = 4
            if (r0 == 0) goto L_0x0047
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r0.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getRandomAlphabet(r5, r1)     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getRandomDigits(r5, r3)     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x0047:
            java.lang.String r0 = "Random12Chars"
            java.lang.String r6 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r6)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x0057
            java.lang.String r0 = r8.getRandom12Chars()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x0057:
            java.lang.String r0 = "Random8Chars"
            java.lang.String r6 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r6)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x0067
            java.lang.String r0 = r8.getRandom8Chars()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x0067:
            java.lang.String r0 = "Min10Digits"
            java.lang.String r6 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r6)     // Catch:{ all -> 0x01b0 }
            r6 = 0
            if (r0 == 0) goto L_0x00a6
            java.lang.String r0 = r8.getTelephonyNumber(r6, r9)     // Catch:{ all -> 0x01b0 }
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r1.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r2 = " telephonyNumber  retry:"
            r1.append(r2)     // Catch:{ all -> 0x01b0 }
            r1.append(r9)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x01b0 }
            r8.addMHSDumpLog(r1)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x00a4
            java.lang.String r1 = ""
            boolean r1 = r1.equals(r0)     // Catch:{ all -> 0x01b0 }
            if (r1 != 0) goto L_0x009c
            java.lang.String r1 = "0"
            boolean r1 = r1.equals(r0)     // Catch:{ all -> 0x01b0 }
            if (r1 == 0) goto L_0x00a4
        L_0x009c:
            r1 = 10
            java.lang.String r1 = r8.getRandomDigits(r1, r3)     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r1
        L_0x00a4:
            monitor-exit(r8)
            return r0
        L_0x00a6:
            java.lang.String r0 = "ModelWith4RandomDigits"
            java.lang.String r7 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r7)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x00c9
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r0.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getModelName()     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getRandom4Chars()     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x00c9:
            java.lang.String r0 = "IMEI5With5RandomDigits"
            java.lang.String r7 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r7)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x00f9
            r0 = 5
            java.lang.String r1 = r8.getIMEI(r0)     // Catch:{ all -> 0x01b0 }
            if (r1 == 0) goto L_0x00ef
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r2.<init>()     // Catch:{ all -> 0x01b0 }
            r2.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r8.getRandomDigits(r0, r3)     // Catch:{ all -> 0x01b0 }
            r2.append(r0)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r2.toString()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x00ef:
            java.lang.String r0 = "WifiApConfigStore"
            java.lang.String r2 = "Not generate default password : because imei is null"
            android.util.Log.e(r0, r2)     // Catch:{ all -> 0x01b0 }
            r0 = 0
            monitor-exit(r8)
            return r0
        L_0x00f9:
            java.lang.String r0 = "IMEILast8Digits"
            java.lang.String r7 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r7)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x0133
            r0 = 8
            java.lang.String r0 = r8.getLastIMEI(r0)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x010d
            monitor-exit(r8)
            return r0
        L_0x010d:
            java.lang.String r1 = "WifiApConfigStore"
            java.lang.String r2 = "Not generate default password : because imei is null"
            android.util.Log.e(r1, r2)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = "XXXXXXXX"
            r0 = r1
            boolean r1 = DBG     // Catch:{ all -> 0x01b0 }
            if (r1 == 0) goto L_0x0131
            java.lang.String r1 = "WifiApConfigStore"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r2.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r3 = "imei = "
            r2.append(r3)     // Catch:{ all -> 0x01b0 }
            r2.append(r0)     // Catch:{ all -> 0x01b0 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x01b0 }
            android.util.Log.i(r1, r2)     // Catch:{ all -> 0x01b0 }
        L_0x0131:
            monitor-exit(r8)
            return r0
        L_0x0133:
            java.lang.String r0 = "VZWRandomRule"
            java.lang.String r7 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r7)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x0181
            java.lang.String r0 = r8.getTelephonyNumber(r6, r9)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x015b
            java.lang.String r1 = ""
            boolean r1 = r1.equals(r0)     // Catch:{ all -> 0x01b0 }
            if (r1 != 0) goto L_0x015b
            java.lang.Long r1 = java.lang.Long.valueOf(r0)     // Catch:{ NumberFormatException -> 0x0154 }
            long r1 = r1.longValue()     // Catch:{ NumberFormatException -> 0x0154 }
        L_0x0153:
            goto L_0x015f
        L_0x0154:
            r1 = move-exception
            long r2 = android.os.SystemClock.uptimeMillis()     // Catch:{ all -> 0x01b0 }
            r1 = r2
            goto L_0x0153
        L_0x015b:
            long r1 = android.os.SystemClock.uptimeMillis()     // Catch:{ all -> 0x01b0 }
        L_0x015f:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r3.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r4 = r8.getRandomAlphabet(r5, r1)     // Catch:{ all -> 0x01b0 }
            r3.append(r4)     // Catch:{ all -> 0x01b0 }
            r4 = 3
            java.lang.String r4 = r8.getRandomDigits(r4, r1)     // Catch:{ all -> 0x01b0 }
            r3.append(r4)     // Catch:{ all -> 0x01b0 }
            r4 = 1
            java.lang.String r4 = r8.getRandomSymbol(r4, r1)     // Catch:{ all -> 0x01b0 }
            r3.append(r4)     // Catch:{ all -> 0x01b0 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r3
        L_0x0181:
            java.lang.String r0 = "UserDefined"
            java.lang.String r6 = CONFIGMOBILEAPDEFAULTPWD     // Catch:{ all -> 0x01b0 }
            boolean r0 = r0.equals(r6)     // Catch:{ all -> 0x01b0 }
            if (r0 == 0) goto L_0x0197
            if (r9 != 0) goto L_0x0191
            java.lang.String r0 = "\tUSER#DEFINED#PWD#\n"
            monitor-exit(r8)
            return r0
        L_0x0191:
            android.net.wifi.WifiConfiguration r0 = r8.mWifiApConfig     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r0.preSharedKey     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x0197:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b0 }
            r0.<init>()     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getRandomAlphabet(r5, r1)     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r1 = r8.getRandomDigits(r5, r3)     // Catch:{ all -> 0x01b0 }
            r0.append(r1)     // Catch:{ all -> 0x01b0 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x01b0 }
            monitor-exit(r8)
            return r0
        L_0x01b0:
            r9 = move-exception
            monitor-exit(r8)
            throw r9
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiApConfigStore.parseSecProductFeaturePassword(boolean):java.lang.String");
    }

    private void reGenerateAndWriteConfiguration() {
        Log.d(TAG, "Re-Generate SSID");
        String defaultSSID = parseSecProductFeatureSsid("XXXX", false);
        WifiConfiguration wifiConfiguration = this.mWifiApConfig;
        wifiConfiguration.SSID = defaultSSID;
        if (wifiConfiguration.allowedKeyManagement.cardinality() >= 1) {
            Log.d(TAG, "KeyManagement.cardinality() >= 1 => clear");
            this.mWifiApConfig.allowedKeyManagement.clear();
        }
        if (!"None".equals(CONFIGMOBILEAPDEFAULTPWD)) {
            this.mWifiApConfig.allowedKeyManagement.set(4);
            String password = parseSecProductFeaturePassword(true);
            if (password == null) {
                WifiConfiguration wifiConfiguration2 = this.mWifiApConfig;
                wifiConfiguration2.preSharedKey = getRandomAlphabet(4, 0) + getRandomDigits(4, 1);
            } else {
                this.mWifiApConfig.preSharedKey = password;
            }
        } else {
            this.mWifiApConfig.allowedKeyManagement.set(0);
        }
        if (SPF_SupportMobileApDualPassword) {
            WifiConfiguration wifiConfiguration3 = this.mWifiApConfig;
            wifiConfiguration3.guestPreSharedKey = getRandomAlphabet(4, 0) + getRandomDigits(4, 1);
        }
        if (!defaultSSID.contains("XXXX")) {
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
            if (!errSSID.equals(this.mWifiApConfig.SSID)) {
                addMHSDumpLog("regen SSID is not error");
                SemWifiApRestoreHelper.setApConfiguration(this.mContext, this.mWifiApConfig);
                return;
            }
            addMHSDumpLog("regen SSID is error. do not save");
        }
    }

    private String getRandom12Chars() {
        String randomUUID = UUID.randomUUID().toString();
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }

    private String getRandom8Chars() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getRandom4Chars() {
        return UUID.randomUUID().toString().substring(0, 4);
    }

    private String getMacAddressLast6DigitsForKOR() {
        String mac = WifiChipInfo.getInstance().getMacAddress();
        if (this.isJDMDevice) {
            addMHSDumpLog("before   JDM factory mac:" + showMacAddress(mac));
            mac = getWifiMacAddress();
            if (mac == null) {
                addMHSDumpLog("before   JDM random  mac:" + showMacAddress(mac));
                mac = getRandomDigits(6, 0);
            }
        }
        addMHSDumpLog(" getMacAddressLast6DigitsForKOR() mac:" + showMacAddress(mac));
        if (mac != null) {
            String temp = mac.replaceAll(":", "_");
            int len = temp.length();
            return new String(temp.substring(len - 8, len));
        }
        Log.e(TAG, "MAC read fail");
        return null;
    }

    private String getMacAddressLastDigits(int lastDigits) {
        String mac = WifiChipInfo.getInstance().getMacAddress();
        if (this.isJDMDevice) {
            addMHSDumpLog("before   JDM factory mac:" + showMacAddress(mac) + " lastDigits = " + lastDigits);
            mac = getWifiMacAddress();
            if (mac == null) {
                addMHSDumpLog("before   JDM random  mac:" + showMacAddress(mac) + " lastDigits = " + lastDigits);
                mac = getRandomDigits(lastDigits, 0);
            }
        }
        addMHSDumpLog(" MacAddressLastDigits final mac:" + showMacAddress(mac) + " lastDigits = " + lastDigits);
        if (mac != null) {
            String temp = mac.replaceAll(":", "");
            int len = temp.length();
            return new String(temp.substring(len - lastDigits, len));
        }
        Log.e(TAG, "MAC read fail , generating random digits");
        return null;
    }

    private String getTelephonyNumber(int digit, boolean retry) {
        String min = ((TelephonyManager) this.mContext.getSystemService("phone")).getLine1Number();
        if (min != null) {
            int len = min.length();
            StringBuilder sb = new StringBuilder();
            sb.append(" getTelephonyNumber() min:[");
            int i = 6;
            sb.append(min.substring(0, 6 > len ? len : 6));
            sb.append("]retry:");
            sb.append(retry);
            addMHSDumpLog(sb.toString());
            if (!retry) {
                if (6 > len) {
                    i = len;
                }
                if ("000000".equals(min.substring(0, i))) {
                    return null;
                }
            }
            if (digit == 0) {
                return min;
            }
            if (len >= digit) {
                return new String(min.substring(len - digit, len));
            }
        } else {
            addMHSDumpLog(" getTelephonyNumber() min:null");
            Log.e(TAG, "Fail to get MSISDN");
        }
        return null;
    }

    private String getIMEI(int digit) {
        String imei = ((TelephonyManager) this.mContext.getSystemService("phone")).getDeviceId();
        if (imei != null) {
            int len = imei.length();
            StringBuilder sb = new StringBuilder();
            sb.append(" getIMEI() imei : ");
            int i = 6;
            if (6 > len) {
                i = len;
            }
            sb.append(imei.substring(0, i));
            addMHSDumpLog(sb.toString());
            if (digit == 0) {
                return imei;
            }
            if (len >= digit) {
                return new String(imei.substring(0, digit));
            }
            return null;
        }
        addMHSDumpLog(" getIMEI() imei : null");
        Log.e(TAG, "Fail to get IMEI");
        return null;
    }

    private String getLastIMEI(int digit) {
        String imei = ((TelephonyManager) this.mContext.getSystemService("phone")).getDeviceId();
        addMHSDumpLog(" getLastIMEI() ");
        if (imei != null) {
            int len = imei.length();
            StringBuilder sb = new StringBuilder();
            sb.append(" getIMEI() imei : ");
            int i = 6;
            if (6 > len) {
                i = len;
            }
            sb.append(imei.substring(0, i));
            addMHSDumpLog(sb.toString());
            if (digit == 0) {
                return imei;
            }
            if (len >= digit) {
                return new String(imei.substring(len - digit, len));
            }
            return null;
        }
        addMHSDumpLog(" getLastIMEI() imei : null");
        Log.e(TAG, "Fail to get IMEI");
        return null;
    }

    private String getModelName() {
        String name = Build.MODEL;
        int len = name.length();
        if (len <= 8 || !"SAMSUNG-".equals(name.substring(0, 8))) {
            return name;
        }
        return name.substring(8, len);
    }

    private byte[] longToBytes(long seed) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) ((int) (255 & seed));
            seed >>= 8;
        }
        return result;
    }

    private String getRandomDigits(int digit, long seed) {
        if (digit == 0) {
            return "";
        }
        if (seed == 0) {
            seed = SystemClock.uptimeMillis();
        }
        if (seed == 1) {
            seed = SystemClock.uptimeMillis() + 1;
        }
        SecureRandom random = new SecureRandom(longToBytes(seed));
        int maxValue = 10;
        for (int i = 1; i < digit; i++) {
            maxValue *= 10;
        }
        String format = String.format(Locale.US, "%%0%dd", new Object[]{Integer.valueOf(digit)});
        return String.format(Locale.US, format, new Object[]{Integer.valueOf(random.nextInt(maxValue - 1))});
    }

    private String getRandomAlphabet(int digit, long seed) {
        String alphabet = new String("abcdefghijklmnopqrstuvwxyz");
        int len = alphabet.length();
        if (seed == 0) {
            seed = SystemClock.uptimeMillis();
        }
        SecureRandom random = new SecureRandom(longToBytes(seed));
        String result = "";
        for (int i = 0; i < digit; i++) {
            result = result + alphabet.charAt(random.nextInt(len));
        }
        return result;
    }

    private String getRandomSymbol(int digit, long seed) {
        String sym = new String("!@#$/^&*()");
        int len = sym.length();
        if (seed == 0) {
            seed = SystemClock.uptimeMillis();
        }
        SecureRandom random = new SecureRandom(longToBytes(seed));
        String result = "";
        for (int i = 0; i < digit; i++) {
            result = result + sym.charAt(random.nextInt(len));
        }
        return result;
    }

    private static int getRandomIntForDefaultSsid() {
        return new SecureRandom().nextInt(9000) + 1000;
    }

    public static WifiConfiguration generateLocalOnlyHotspotConfig(Context context, int apBand) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = context.getResources().getString(17042649) + "_" + getRandomIntForDefaultSsid();
        config.apBand = apBand;
        config.allowedKeyManagement.set(4);
        config.networkId = -2;
        String randomUUID = UUID.randomUUID().toString();
        config.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
        return config;
    }

    private static boolean validateApConfigSsid(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            Log.d(TAG, "SSID for softap configuration must be set.");
            return false;
        }
        try {
            byte[] ssid_bytes = ssid.getBytes(StandardCharsets.UTF_8);
            if (ssid_bytes.length >= 1) {
                if (ssid_bytes.length <= 32) {
                    return true;
                }
            }
            Log.d(TAG, "softap SSID is defined as UTF-8 and it must be at least 1 byte and not more than 32 bytes");
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "softap config SSID verification failed: malformed string " + ssid);
            return false;
        }
    }

    private static boolean validateApConfigPreSharedKey(String preSharedKey) {
        if (preSharedKey.length() < 8 || preSharedKey.length() > 63) {
            Log.d(TAG, "softap network password string size must be at least 8 and no more than 63");
            return false;
        }
        try {
            preSharedKey.getBytes(StandardCharsets.UTF_8);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "softap network password verification failed: malformed string");
            return false;
        }
    }

    static boolean validateApWifiConfiguration(WifiConfiguration apConfig) {
        if (!validateApConfigSsid(apConfig.SSID)) {
            return false;
        }
        if (apConfig.allowedKeyManagement == null) {
            Log.d(TAG, "softap config key management bitset was null");
            return false;
        }
        String preSharedKey = apConfig.preSharedKey;
        boolean hasPreSharedKey = !TextUtils.isEmpty(preSharedKey);
        try {
            int authType = apConfig.getAuthType();
            if (authType == 0) {
                if (hasPreSharedKey) {
                    Log.d(TAG, "open softap network should not have a password");
                    Log.d(TAG, "set empty password forcely");
                    apConfig.preSharedKey = "";
                }
            } else if (authType != 4 && authType != 25 && authType != 26) {
                Log.d(TAG, "softap configs must either be open or WPA2 PSK networks");
                return false;
            } else if (!hasPreSharedKey) {
                Log.d(TAG, "softap network password must be set");
                return false;
            } else if (!validateApConfigPreSharedKey(preSharedKey)) {
                return false;
            }
            return true;
        } catch (IllegalStateException e) {
            Log.d(TAG, "Unable to get AuthType for softap config: " + e.getMessage());
            return false;
        }
    }

    private void startSoftApSettings() {
        this.mContext.startActivity(new Intent("com.android.settings.WIFI_TETHER_SETTINGS").addFlags(268435456));
    }

    /* access modifiers changed from: private */
    public void handleUserHotspotConfigTappedContent() {
        startSoftApSettings();
        ((NotificationManager) this.mContext.getSystemService("notification")).cancel(50);
    }

    private PendingIntent getPrivateBroadcast(String action) {
        return this.mFrameworkFacade.getBroadcast(this.mContext, 0, new Intent(action).setPackage("android"), 134217728);
    }

    private void generateDefaultSSID() {
        String tempSSID;
        String defaultNumber;
        String defaultSsid;
        Log.e(TAG, "generateDefaultSSID..");
        String number = null;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (telephonyManager != null) {
            number = telephonyManager.getLine1Number();
        }
        if (number == null) {
            number = "";
        }
        if ("".equals(this.DEFAULTSSIDNPWD)) {
            tempSSID = this.mContext.getString(17042743);
        } else {
            tempSSID = this.DEFAULTSSIDNPWD.split(",")[0];
        }
        if (number.length() >= 4) {
            String last4digitNum = String.copyValueOf(number.toCharArray(), number.length() - 4, 4);
            defaultSsid = tempSSID.concat(last4digitNum);
            defaultNumber = last4digitNum;
        } else {
            int sequence = new SecureRandom(longToBytes(SystemClock.uptimeMillis())).nextInt(8999) + 1000;
            defaultSsid = tempSSID + sequence;
            defaultNumber = Integer.toString(sequence);
        }
        this.mWifiApConfig = new WifiConfiguration();
        WifiConfiguration wifiConfiguration = this.mWifiApConfig;
        wifiConfiguration.SSID = defaultSsid;
        wifiConfiguration.hiddenSSID = false;
        String randomUUID = UUID.randomUUID().toString();
        if ("".equals(this.DEFAULTSSIDNPWD)) {
            this.mWifiApConfig.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
        } else {
            String[] mSplitPwd = this.DEFAULTSSIDNPWD.split(",");
            if (mSplitPwd.length == 1) {
                this.mWifiApConfig.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
            } else {
                StringBuilder mPwd = new StringBuilder();
                for (int i = 1; i < mSplitPwd.length; i++) {
                    if ("LAST4DIGIT".equals(mSplitPwd[i])) {
                        mPwd.append(defaultNumber);
                    } else {
                        mPwd.append(mSplitPwd[i]);
                    }
                    this.mWifiApConfig.preSharedKey = mPwd.toString();
                }
            }
        }
        this.mWifiApConfig.allowedKeyManagement.set(4);
        Log.e(TAG, "Calling writeApConfiguration");
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
    }

    public void setRvfMode(int value) {
        this.mRVFMode = value;
        Log.i(TAG, "setRvfMode " + this.mRVFMode);
        if ("SPR".equals(SemCscFeature.getInstance().getString("SalesCode"))) {
            boolean isTemp = true;
            boolean isTemp2 = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_supporttemp_spr", 0) == 1;
            Log.i(TAG, "before setRvfMode " + this.mRVFMode + " " + SystemProperties.getInt("persist.sys.tether_data_wifi", 0) + " " + isTemp2);
            if (this.mRVFMode == 0) {
                if (isTemp2) {
                    SystemProperties.set("persist.sys.tether_data_wifi", "0");
                    Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_supporttemp_spr", 0);
                }
            } else if (SystemProperties.getInt("persist.sys.tether_data_wifi", 0) == 0) {
                SystemProperties.set("persist.sys.tether_data_wifi", "1");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_supporttemp_spr", 1);
            }
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_supporttemp_spr", 0) != 1) {
                isTemp = false;
            }
            Log.i(TAG, "after setRvfMode " + this.mRVFMode + " " + SystemProperties.getInt("persist.sys.tether_data_wifi", 0) + " " + isTemp);
        }
    }

    public int getRvfMode() {
        return this.mRVFMode;
    }

    private String showMacAddress(String aMac) {
        if (DBG) {
            return aMac;
        }
        if (aMac == null || aMac.length() != 17) {
            return "fe:dc:ab";
        }
        return aMac.substring(0, 3) + aMac.substring(12, 17);
    }

    public void addMHSDumpLog(String log) {
        StringBuffer value = new StringBuffer();
        Log.i(TAG, log + " mhs: " + this.mMHSDumpLogs.size());
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (this.mMHSDumpLogs.size() > 100) {
            this.mMHSDumpLogs.remove(0);
        }
        this.mMHSDumpLogs.add(value.toString());
    }

    public String getDumpLogs() {
        StringBuffer retValue = new StringBuffer();
        retValue.append("--WifiApConfigStore \n");
        retValue.append(this.mMHSDumpLogs.toString());
        return retValue.toString();
    }
}
