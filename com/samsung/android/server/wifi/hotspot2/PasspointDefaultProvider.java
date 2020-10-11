package com.samsung.android.server.wifi.hotspot2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Debug;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.WifiServiceImpl;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PasspointDefaultProvider {
    /* access modifiers changed from: private */
    public static boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "PasspointDefaultProvider";
    private static final int UPDATE_CONFIG_COMPLETE = 2;
    private static final int UPDATE_CONFIG_REQUEST_FROM_CSC = 1;
    private static final int UPDATE_CONFIG_REQUEST_FROM_REBOOTING = 0;
    /* access modifiers changed from: private */
    public static final ArrayList<PasspointConfiguration> mDefaultProvderConfigList = new ArrayList<>();
    /* access modifiers changed from: private */
    public static boolean mIsloadInternalDataCompleted = false;
    /* access modifiers changed from: private */
    public static int mNeedtoUpdateProviderConfig = 0;
    /* access modifiers changed from: private */
    public static File mPasspointCredentialFile = new File("/data/misc/wifi/cred.conf");
    private final int LocalLogSize = 256;
    private final Context mContext;
    private final LocalLog mLocalLog;
    /* access modifiers changed from: private */
    public int mPreviousSimState = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            PasspointDefaultProvider passpointDefaultProvider = PasspointDefaultProvider.this;
            passpointDefaultProvider.logi(PasspointDefaultProvider.TAG, "onReceive, action: " + action);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED") || action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                int currentSimState = TelephonyManager.getDefault().getSimState();
                if (PasspointDefaultProvider.DBG) {
                    PasspointDefaultProvider passpointDefaultProvider2 = PasspointDefaultProvider.this;
                    passpointDefaultProvider2.logv(PasspointDefaultProvider.TAG, "onReceive, SIM state changed from " + PasspointDefaultProvider.this.mPreviousSimState + " to " + currentSimState);
                }
                if (PasspointDefaultProvider.DBG) {
                    PasspointDefaultProvider passpointDefaultProvider3 = PasspointDefaultProvider.this;
                    passpointDefaultProvider3.logv(PasspointDefaultProvider.TAG, "onReceive, mIsloadInternalDataCompleted: " + PasspointDefaultProvider.mIsloadInternalDataCompleted + ", mDefaultProvderConfigList size: " + PasspointDefaultProvider.mDefaultProvderConfigList.size());
                }
                if (currentSimState == 5 && currentSimState != PasspointDefaultProvider.this.mPreviousSimState) {
                    String imsi = PasspointDefaultProvider.this.getImsi();
                    if (TextUtils.isEmpty(imsi) || !PasspointDefaultProvider.mIsloadInternalDataCompleted) {
                        int unused = PasspointDefaultProvider.this.mPreviousSimState = 6;
                        return;
                    }
                    if (PasspointDefaultProvider.mDefaultProvderConfigList.isEmpty() && PasspointDefaultProvider.this.readPasspointCredendtialFile(PasspointDefaultProvider.mPasspointCredentialFile, 1)) {
                        PasspointDefaultProvider passpointDefaultProvider4 = PasspointDefaultProvider.this;
                        passpointDefaultProvider4.logi(PasspointDefaultProvider.TAG, "onReceive, added to the mDefaultProvderConfigList, size: " + PasspointDefaultProvider.mDefaultProvderConfigList.size());
                    }
                    PasspointDefaultProvider.this.updateSimCredential(imsi);
                }
                int unused2 = PasspointDefaultProvider.this.mPreviousSimState = currentSimState;
            } else if (action.equals("com.samsung.intent.action.CSC_WIFI_UPDATE_HOTSPOT20_CONFIG")) {
                PasspointDefaultProvider.this.updatePasspointSettings();
                PasspointDefaultProvider.this.dropFile(PasspointDefaultProvider.mPasspointCredentialFile);
                if (PasspointCscUtils.getInstance().parsingCsc()) {
                    PasspointDefaultProvider.this.logi(PasspointDefaultProvider.TAG, "cred.conf file created");
                }
                if (PasspointDefaultProvider.mIsloadInternalDataCompleted) {
                    PasspointDefaultProvider.this.removeOldPasspointConfig();
                    if (PasspointDefaultProvider.this.readPasspointCredendtialFile(PasspointDefaultProvider.mPasspointCredentialFile, 1)) {
                        PasspointDefaultProvider.this.addDefaultProviderConfiguration();
                    }
                    int unused3 = PasspointDefaultProvider.mNeedtoUpdateProviderConfig = 2;
                    return;
                }
                int unused4 = PasspointDefaultProvider.mNeedtoUpdateProviderConfig = 1;
            } else if (action.equals("com.samsung.android.net.wifi.LOAD_INTERNAL_DATA_COMPLETE")) {
                if (intent.getBooleanExtra("passpointConfiguration", false) && !PasspointDefaultProvider.mIsloadInternalDataCompleted) {
                    if (PasspointDefaultProvider.DBG) {
                        PasspointDefaultProvider passpointDefaultProvider5 = PasspointDefaultProvider.this;
                        passpointDefaultProvider5.logd(PasspointDefaultProvider.TAG, "mNeedtoUpdateProviderConfig, " + PasspointDefaultProvider.mNeedtoUpdateProviderConfig);
                    }
                    if (PasspointDefaultProvider.mNeedtoUpdateProviderConfig == 1) {
                        PasspointDefaultProvider.this.removeOldPasspointConfig();
                        if (PasspointDefaultProvider.this.readPasspointCredendtialFile(PasspointDefaultProvider.mPasspointCredentialFile, 1)) {
                            PasspointDefaultProvider.this.addDefaultProviderConfiguration();
                        }
                    } else if (PasspointDefaultProvider.this.readPasspointCredendtialFile(PasspointDefaultProvider.mPasspointCredentialFile, 1)) {
                        PasspointDefaultProvider passpointDefaultProvider6 = PasspointDefaultProvider.this;
                        passpointDefaultProvider6.logi(PasspointDefaultProvider.TAG, "loadInternalData Completed (size: " + PasspointDefaultProvider.mDefaultProvderConfigList.size() + ")");
                        PasspointDefaultProvider.this.checkAndUpdateDefaultProviderConfiguration();
                    }
                    int unused5 = PasspointDefaultProvider.mNeedtoUpdateProviderConfig = 2;
                    boolean unused6 = PasspointDefaultProvider.mIsloadInternalDataCompleted = true;
                }
            } else if (action.equals("com.samsung.intent.action.SETTINGS_RESET_WIFI")) {
                PasspointDefaultProvider.this.addDefaultProviderFromCredential();
            }
        }
    };
    private WifiServiceImpl mWifiServiceImpl;

    /* renamed from: sb */
    StringBuffer f54sb = new StringBuffer();

    public PasspointDefaultProvider(Context context, WifiServiceImpl wifiServiceImpl) {
        this.mContext = context;
        this.mWifiServiceImpl = wifiServiceImpl;
        this.mLocalLog = new LocalLog(256);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("com.samsung.intent.action.CSC_WIFI_UPDATE_HOTSPOT20_CONFIG");
        filter.addAction("com.samsung.android.net.wifi.LOAD_INTERNAL_DATA_COMPLETE");
        filter.addAction("com.samsung.intent.action.SETTINGS_RESET_WIFI");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    /* access modifiers changed from: private */
    public boolean readPasspointCredendtialFile(File path, int providerType) {
        if (!path.exists()) {
            loge(TAG, "readPasspointCredendtialFile, file(" + path + ") is not founded.");
            return false;
        }
        if (providerType == 1) {
            mDefaultProvderConfigList.clear();
        }
        BufferedReader in = null;
        try {
            BufferedReader in2 = new BufferedReader(new FileReader(path));
            while (in2.ready()) {
                String strLine = in2.readLine();
                if (DBG) {
                    logd(TAG, "readPasspointCredendtialFile, strLine: " + strLine);
                }
                if (strLine != null && strLine.startsWith("cred")) {
                    PasspointConfigManager configManager = parsingCredentialFile(in2);
                    if (configManager == null) {
                        loge(TAG, "readPasspointCredendtialFile, configManager is null");
                    } else {
                        configManager.setProviderType(providerType);
                        PasspointConfiguration config = configManager.createConfig();
                        if (config != null) {
                            mDefaultProvderConfigList.add(config);
                        }
                    }
                }
            }
            try {
                in2.close();
                if (DBG) {
                    int i = 0;
                    Iterator<PasspointConfiguration> it = mDefaultProvderConfigList.iterator();
                    while (it.hasNext()) {
                        HomeSp homeSp = it.next().getHomeSp();
                        if (homeSp == null) {
                            loge(TAG, "readPasspointCredendtialFile, homeSp is null");
                        } else {
                            logd(TAG, "readPasspointCredendtialFile, added to the mDefaultProvderConfigList (" + i + ") - Fqdn: " + homeSp.getFqdn() + ", FriendlyName: " + homeSp.getFriendlyName());
                            i++;
                        }
                    }
                }
                return true;
            } catch (IOException e) {
                loge(TAG, "readPasspointCredendtialFile, IOException 2");
                return false;
            }
        } catch (IOException e2) {
            loge(TAG, "readPasspointCredendtialFile, IOException 1: " + e2.getMessage());
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                    loge(TAG, "readPasspointCredendtialFile, IOException 2");
                    return false;
                }
            }
            return false;
        } catch (Throwable e4) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                    loge(TAG, "readPasspointCredendtialFile, IOException 2");
                    return false;
                }
            }
            throw e4;
        }
    }

    private PasspointConfigManager parsingCredentialFile(BufferedReader in) {
        PasspointConfigManager configManager = new PasspointConfigManager(this.mContext, this.mLocalLog);
        while (true) {
            try {
                if (!in.ready()) {
                    break;
                }
                String strLine = in.readLine();
                if (DBG) {
                    logd(TAG, "parsingCredentialFile, strLine: " + strLine);
                }
                if (strLine == null) {
                    break;
                } else if (strLine.startsWith("}")) {
                    break;
                } else {
                    String strLine2 = strLine.trim();
                    if (strLine2.startsWith("domain=\"")) {
                        configManager.mFqdn = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("friendlyname=\"")) {
                        configManager.mFriendlyName = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("roaming_consortium=")) {
                        configManager.mRoamingConsortiumOis = parseLongArray(strLine2.substring(strLine2.indexOf(61) + 1));
                    }
                    if (strLine2.startsWith("realm=\"")) {
                        configManager.mRealm = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("eap=")) {
                        configManager.mEapType = strLine2.substring(strLine2.indexOf(61) + 1);
                    }
                    if (strLine2.startsWith("username=\"")) {
                        configManager.mUsername = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("password=")) {
                        configManager.mPassword = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("phase2=\"")) {
                        configManager.mNonEAPInnerMethod = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("imsi=\"")) {
                        configManager.mImsi = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("ca_cert=\"")) {
                        configManager.mCaCertificateKey = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("client_cert=")) {
                        configManager.mClientCertificate = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("private_key=\"")) {
                        configManager.mClientPrivateKey = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("private_key_password=\"")) {
                        configManager.mClientKeyPassword = strLine2.substring(strLine2.indexOf(34) + 1, strLine2.lastIndexOf(34));
                    }
                    if (strLine2.startsWith("priority=")) {
                        configManager.mPriority = strLine2.substring(strLine2.indexOf(61) + 1);
                    }
                }
            } catch (IOException e) {
                loge(TAG, "parsingCredentialFile, IOException");
                return null;
            }
        }
        return configManager;
    }

    /* access modifiers changed from: private */
    public void addDefaultProviderConfiguration() {
        int i = 0;
        Iterator<PasspointConfiguration> it = mDefaultProvderConfigList.iterator();
        while (it.hasNext()) {
            PasspointConfiguration config = it.next();
            boolean ret = this.mWifiServiceImpl.addOrUpdatePasspointConfiguration(config, "SamsungPasspoint");
            if (ret) {
                HomeSp homeSp = config.getHomeSp();
                if (homeSp == null) {
                    loge(TAG, "addDefaultProviderConfiguration, homeSp is null");
                } else {
                    logv(TAG, "addDefaultProviderConfiguration, mDefaultProvderConfigList(" + i + ") - " + homeSp.getFqdn() + ", add: " + ret);
                }
            }
            i++;
        }
    }

    /* access modifiers changed from: private */
    public void checkAndUpdateDefaultProviderConfiguration() {
        boolean ret;
        int i = 0;
        List<PasspointConfiguration> savedPasspointConfigs = this.mWifiServiceImpl.getPasspointConfigurations("SamsungPasspoint");
        Iterator<PasspointConfiguration> it = mDefaultProvderConfigList.iterator();
        while (it.hasNext()) {
            PasspointConfiguration provderConfig = it.next();
            String vendorFqdn = provderConfig.getHomeSp().getFqdn();
            if (vendorFqdn != null) {
                boolean isMatched = false;
                if (savedPasspointConfigs != null) {
                    Iterator<PasspointConfiguration> it2 = savedPasspointConfigs.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        PasspointConfiguration savedConfig = it2.next();
                        if (savedConfig != null) {
                            String savedFqdn = savedConfig.getHomeSp().getFqdn();
                            if (savedConfig.getHomeSp().isVendorSpecificSsid() && vendorFqdn.equals(savedFqdn)) {
                                isMatched = true;
                                logv(TAG, "checkAndUpdateDefaultProviderConfiguration, mDefaultProvderConfigList(" + i + ") " + savedConfig.getHomeSp().getFqdn() + " already exist.");
                                break;
                            }
                        }
                    }
                }
                if (!isMatched && ret) {
                    logv(TAG, "checkAndUpdateDefaultProviderConfiguration, mDefaultProvderConfigList (" + i + ") " + provderConfig.getHomeSp().getFqdn() + ", add: " + (ret = this.mWifiServiceImpl.addOrUpdatePasspointConfiguration(provderConfig, "SamsungPasspoint")));
                }
                i++;
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeOldPasspointConfig() {
        List<PasspointConfiguration> savedPasspointConfigs = this.mWifiServiceImpl.getPasspointConfigurations("SamsungPasspoint");
        if (savedPasspointConfigs != null) {
            for (PasspointConfiguration savedConfig : savedPasspointConfigs) {
                if (savedConfig != null && savedConfig.getHomeSp().isVendorSpecificSsid()) {
                    String savedFqdn = savedConfig.getHomeSp().getFqdn();
                    boolean ret = this.mWifiServiceImpl.removePasspointConfiguration(savedFqdn, "SamsungPasspoint");
                    logv(TAG, "removeOldPasspointConfig, old vendor passpoint " + savedFqdn + " remove : " + ret);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void dropFile(File path) {
        if (path.exists()) {
            if (DBG) {
                logd(TAG, "delete cred.conf file");
            }
            path.delete();
        }
    }

    private static long[] parseLongArray(String string) throws IOException {
        String[] strArray = string.split(",");
        long[] longArray = new long[strArray.length];
        int i = 0;
        while (i < longArray.length) {
            try {
                longArray[i] = Long.parseLong(strArray[i], 16);
                i++;
            } catch (NumberFormatException e) {
                throw new IOException("parseLongArray, Invalid long integer value: " + strArray[i]);
            }
        }
        return longArray;
    }

    /* access modifiers changed from: private */
    public void updateSimCredential(String imsi) {
        int i = 0;
        List<PasspointConfiguration> savedPasspointConfigs = this.mWifiServiceImpl.getPasspointConfigurations("SamsungPasspoint");
        if (savedPasspointConfigs != null) {
            if (DBG) {
                logd(TAG, "updateSimCredential, saved PasspointConfigurations size: " + savedPasspointConfigs.size() + " ");
            }
            for (PasspointConfiguration config : savedPasspointConfigs) {
                HomeSp savedHomeSp = config.getHomeSp();
                if (DBG) {
                    logd(TAG, "updateSimCredential, saved PasspointConfiguration (" + i + ") Fqdn: " + savedHomeSp.getFqdn() + ", FriendlyName: " + savedHomeSp.getFriendlyName() + ", isVendorSpecificSsid: " + savedHomeSp.isVendorSpecificSsid());
                }
                i++;
                if (config.getCredential().getSimCredential() != null) {
                    Credential credential = config.getCredential();
                    String configImsi = credential.getSimCredential().getImsi();
                    if (DBG) {
                        logd(TAG, "updateSimCredential, config Imsi: " + configImsi + ", new imsi: " + imsi);
                    }
                    if (!TextUtils.isEmpty(configImsi)) {
                        Credential.SimCredential simCredential = config.getCredential().getSimCredential();
                        if ("00101*".equals(configImsi) || !containsNonDigits(configImsi)) {
                            simCredential.setImsi(imsi);
                        } else {
                            simCredential.setImsi(configImsi);
                        }
                        credential.setSimCredential(simCredential);
                        config.setCredential(credential);
                        boolean ret = this.mWifiServiceImpl.addOrUpdatePasspointConfiguration(config, "SamsungPasspoint");
                        if (DBG) {
                            logd(TAG, "updateSimCredential, addOrUpdatePasspointConfiguration Fqdn: " + savedHomeSp.getFqdn() + ", ret: " + ret);
                        }
                    }
                }
            }
        }
    }

    private boolean containsNonDigits(String configImsi) {
        char stopChar = 0;
        int nonDigit = 0;
        while (nonDigit < configImsi.length() && (stopChar = configImsi.charAt(nonDigit)) >= '0' && stopChar <= '9') {
            nonDigit++;
        }
        if (nonDigit != configImsi.length() - 1 || stopChar != '*') {
            return false;
        }
        logd(TAG, "containsNonDigits, configImsi has no digit(*), ignores the SIM check");
        return true;
    }

    /* access modifiers changed from: private */
    public String getImsi() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm == null) {
            loge(TAG, "getImsi, TelephonyManager is null");
            return null;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        String actualSubscriberId = tm.getSubscriberId(subId);
        if (DBG) {
            logd(TAG, "getImsi, actualSubscriberId (" + subId + ") : " + actualSubscriberId);
        }
        return actualSubscriberId;
    }

    /* access modifiers changed from: private */
    public void updatePasspointSettings() {
        boolean isPasspointConnected = false;
        int passpointEnabled = 3;
        try {
            passpointEnabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_hotspot20_enable");
        } catch (Settings.SettingNotFoundException e) {
            loge(TAG, "updatePasspointSettings, WIFI_HOTSPOT20_ENABLE SettingNotFoundException");
        }
        boolean z = true;
        if (passpointEnabled != 0 && passpointEnabled != 1) {
            String passpointCscFeature = OpBrandingLoader.getInstance().getMenuStatusForPasspoint();
            logd(TAG, "updatePasspointSettings, passpointCscFeature: " + passpointCscFeature);
            if (passpointCscFeature != null) {
                if (passpointCscFeature.contains("DEFAULT_OFF")) {
                    passpointEnabled = 2;
                } else {
                    passpointEnabled = 3;
                }
            }
            if (DBG) {
                logd(TAG, "updatePasspointSettings, Settings WIFI_HOTSPOT20_ENABLE to " + passpointEnabled);
            }
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_hotspot20_enable", passpointEnabled);
        } else if (OpBrandingLoader.Vendor.VZW == OpBrandingLoader.getInstance().getOpBranding()) {
            passpointEnabled = 3;
            if (DBG) {
                logd(TAG, "updatePasspointSettings, Settings WIFI_HOTSPOT20_ENABLE to " + 3);
            }
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_hotspot20_enable", 3);
        }
        try {
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_hotspot20_connected_history") != 1) {
                z = false;
            }
            isPasspointConnected = z;
        } catch (Settings.SettingNotFoundException e2) {
            loge(TAG, "updatePasspointSettings, WIFI_HOTSPOT20_CONNECTED_HISTORY SettingNotFoundException");
        }
        logi(TAG, "updatePasspointSettings, passpointEnabled: " + passpointEnabled + ", isPasspointConnected: " + isPasspointConnected);
    }

    public void addDefaultProviderFromCredential() {
        logi(TAG, "addDefaultProviderFromCredential");
        if (readPasspointCredendtialFile(mPasspointCredentialFile, 1)) {
            addDefaultProviderConfiguration();
        }
    }

    /* access modifiers changed from: protected */
    public void loge(String tag, String s) {
        Log.e(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logd(String tag, String s) {
        Log.d(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logv(String tag, String s) {
        Log.v(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logi(String tag, String s) {
        Log.i(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String passpointCscFeature = OpBrandingLoader.getInstance().getMenuStatusForPasspoint();
        pw.println("passpointSecProductFeature: " + "DEFAULT_ON,MENU_OFF");
        pw.println("passpointCscFeature: " + passpointCscFeature);
        pw.println("mNeedtoUpdateProviderConfig: " + mNeedtoUpdateProviderConfig);
        pw.println("Dump of PasspointDefaultProvider");
        pw.println("PasspointDefaultProvider - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("PasspointDefaultProvider - Log End ----");
    }
}
