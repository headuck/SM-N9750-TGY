package com.samsung.android.server.wifi;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.application.ApplicationPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class WifiB2BConfigurationPolicy {
    private static final String CONF_KEY_CONFIGURATION = "wificonfiguration";
    private static final String CONF_KEY_NO_CONN_BLACKLIST = "noNetworkDisable";
    private static final String CONF_KEY_ROAM_DELTA = "roamdelta";
    private static final String CONF_KEY_ROAM_NO_DHCP_RENEWAL = "nodhcprenewal";
    private static final String CONF_KEY_ROAM_SCAN_PERIOD = "roamscanperiod";
    private static final String CONF_KEY_ROAM_SSID = "networkname";
    private static final String CONF_KEY_ROAM_TRIGGER = "roamtrigger";
    private static final int MAX_B2BCONF_COUNT = 5;
    private static final int MAX_SSID_LEN = 32;
    private static final String TAG = "WiFiB2BConf";
    public static final int VAL_NOT_CONFIGURED = Integer.MAX_VALUE;
    public static final int WIFICONF_ROAM_SCANPERIOD_DEFAULT = 10;
    private static final int WIFICONF_ROAM_SCANPERIOD_MAX = 60;
    private static final int WIFICONF_ROAM_SCANPERIOD_MIN = 0;
    public static final int WIFICONF_RSSI_ROAMDELTA_DEFAULT = 10;
    private static final int WIFICONF_RSSI_ROAMDELTA_MAX = 100;
    private static final int WIFICONF_RSSI_ROAMDELTA_MIN = 0;
    public static final int WIFICONF_RSSI_THRESHOLD_DEFAULT = -75;
    private static final int WIFICONF_RSSI_THRESHOLD_MAX = -50;
    private static final int WIFICONF_RSSI_THRESHOLD_MIN = -100;
    private static final int WIFI_CONF_ERR_DUPLICATED_NETWORK_CONF = 8;
    private static final int WIFI_CONF_ERR_EXCCEED_MAX_COUNT = 1;
    private static final int WIFI_CONF_ERR_EXCCEED_NETWORK_LEN_MAX = 4;
    private static final int WIFI_CONF_ERR_INVALID_ROAM_DELTA = 32;
    private static final int WIFI_CONF_ERR_INVALID_ROAM_SCAN_PEROD = 64;
    private static final int WIFI_CONF_ERR_INVALID_ROAM_TRIGGER = 16;
    private static final int WIFI_CONF_ERR_NETWORKNAME = 15;
    private static final int WIFI_CONF_ERR_NONE = 0;
    private static final int WIFI_CONF_ERR_NO_SSID_INFO = 2;
    private static final int WIFI_CONF_ERR_ROAM_DELTA = 33;
    private static final int WIFI_CONF_ERR_ROAM_SCAN_PERIOD = 65;
    private static final int WIFI_CONF_ERR_ROAM_TRIGGER = 17;
    private static WifiB2BConfigurationPolicy sWifiB2BConfigurationPolicy = null;
    private int mConfError = 0;
    private final Object mConfigLock = new Object();
    private Context mContext;
    private boolean mPolicyApplied = false;
    private boolean mVerboseLoggingEnabled = true;
    private HashMap<String, B2BConfiguration> mWiFiB2BConfPolicy = new HashMap<>();

    private WifiB2BConfigurationPolicy(Context ctx) {
        this.mContext = ctx;
    }

    public static synchronized WifiB2BConfigurationPolicy getInstance(Context ctx) {
        WifiB2BConfigurationPolicy wifiB2BConfigurationPolicy;
        synchronized (WifiB2BConfigurationPolicy.class) {
            if (sWifiB2BConfigurationPolicy == null) {
                sWifiB2BConfigurationPolicy = new WifiB2BConfigurationPolicy(ctx);
            }
            wifiB2BConfigurationPolicy = sWifiB2BConfigurationPolicy;
        }
        return wifiB2BConfigurationPolicy;
    }

    public B2BConfiguration getConfiguration(String ssid) {
        synchronized (this.mConfigLock) {
            if (this.mWiFiB2BConfPolicy == null) {
                return null;
            }
            B2BConfiguration b2BConfiguration = this.mWiFiB2BConfPolicy.get(ssid);
            return b2BConfiguration;
        }
    }

    public void setPolicyApplied(boolean enable) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setPolicyApplied: " + enable);
        }
        this.mPolicyApplied = enable;
    }

    public boolean isPolicyApplied() {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "isPolicyApplied: " + this.mPolicyApplied);
        }
        return this.mPolicyApplied;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    private void dumpPolicyBundle(Bundle bundle) {
        if (this.mVerboseLoggingEnabled) {
            if (bundle != null) {
                try {
                    Parcelable[] wifiConfigs = bundle.getParcelableArray(CONF_KEY_CONFIGURATION);
                    if (wifiConfigs != null) {
                        Log.d(TAG, "-------------------------------------------");
                        Log.d(TAG, "Roaming Conf size = " + wifiConfigs.length);
                        if (wifiConfigs.length > 0 && wifiConfigs.length <= 5) {
                            for (Parcelable wifiConfig : wifiConfigs) {
                                Log.d(TAG, "    Network Name: " + ((Bundle) wifiConfig).getString(CONF_KEY_ROAM_SSID));
                                Log.d(TAG, "    Roam Trigger: " + ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_TRIGGER, Integer.MAX_VALUE));
                                Log.d(TAG, "    Roam Delta: " + ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_DELTA, Integer.MAX_VALUE));
                                Log.d(TAG, "    Roam Scan Period: " + ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_SCAN_PERIOD, Integer.MAX_VALUE));
                                Log.d(TAG, "    Roam DHCP Skip: " + ((Bundle) wifiConfig).getBoolean(CONF_KEY_ROAM_NO_DHCP_RENEWAL, false));
                                Log.d(TAG, "    No BlackList: " + ((Bundle) wifiConfig).getBoolean(CONF_KEY_NO_CONN_BLACKLIST, false));
                                Log.d(TAG, "--------------------------------------");
                            }
                        }
                    }
                } catch (ClassCastException | NullPointerException e1) {
                    e1.printStackTrace();
                }
            } else {
                Log.e(TAG, "bundle is null");
            }
        }
    }

    private void dumpLogWiFiConfPolicy() {
        if (this.mVerboseLoggingEnabled) {
            try {
                if (this.mWiFiB2BConfPolicy != null) {
                    for (String key : this.mWiFiB2BConfPolicy.keySet()) {
                        Log.d(TAG, "----------------------------------------");
                        Log.d(TAG, "Conf Network SSID: " + key);
                        B2BConfiguration conf = this.mWiFiB2BConfPolicy.get(key);
                        if (conf != null) {
                            Log.d(TAG, "    Roam Trigger: " + conf.roamTrigger);
                            Log.d(TAG, "    Roam Delta: " + conf.roamDelta);
                            Log.d(TAG, "    Roam Scan Period: " + conf.roamScanPeriod);
                            Log.d(TAG, "    No DHCP after Roam: " + conf.noDHCPinRoam);
                            Log.d(TAG, "    No Black List: " + conf.noBlackList);
                        }
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private int checkConfigurationValidity(String networkName, int roamTrigger, int roamDelta, int roamScanPeriod) {
        int result = 0;
        if (networkName == null || networkName.isEmpty()) {
            Log.e(TAG, "checkConfigurationValidity No SSID info");
            result = 0 | 2;
        } else if (networkName.getBytes().length > 32) {
            Log.e(TAG, "checkConfigurationValidity SSID length is too long");
            result = 0 | 4;
        }
        if (this.mWiFiB2BConfPolicy.get(networkName) != null) {
            Log.e(TAG, "checkConfigurationValidity Duplicate Network Name:" + networkName);
            result |= 8;
        }
        if ((roamTrigger > WIFICONF_RSSI_THRESHOLD_MAX || roamTrigger < WIFICONF_RSSI_THRESHOLD_MIN) && roamTrigger != Integer.MAX_VALUE) {
            Log.e(TAG, "checkConfigurationValidity Wrong Roam Trigger: " + roamTrigger);
            result |= 16;
        }
        if ((roamDelta < 0 || roamDelta > 100) && roamDelta != Integer.MAX_VALUE) {
            Log.e(TAG, "checkConfigurationValidity Wrong Roam Delta: " + roamDelta);
            result |= 32;
        }
        if ((roamScanPeriod >= 0 && roamScanPeriod <= 60) || roamScanPeriod == Integer.MAX_VALUE) {
            return result;
        }
        Log.e(TAG, "checkConfigurationValidity Wrong Roam Scan Period: " + roamScanPeriod);
        return result | 64;
    }

    private void enforceEDMConfPermission() {
        EnterpriseDeviceManager edm;
        Context context = this.mContext;
        if (context != null && (edm = EnterpriseDeviceManager.getInstance(context)) != null) {
            edm.enforceActiveAdminPermission(new ArrayList(Arrays.asList(new String[]{"com.samsung.android.knox.permission.KNOX_DEVICE_CONFIGURATION"})));
        }
    }

    public void setWiFiConfiguration(Bundle bundle) {
        Bundle bundle2 = bundle;
        Log.d(TAG, "setWiFiConfiguration start");
        enforceEDMConfPermission();
        dumpPolicyBundle(bundle);
        synchronized (this.mConfigLock) {
            if (this.mWiFiB2BConfPolicy == null) {
                this.mWiFiB2BConfPolicy = new HashMap<>();
            }
            this.mWiFiB2BConfPolicy.clear();
            if (bundle2 == null) {
                Log.e(TAG, "setWiFiConfiguration bundle is null");
                return;
            }
            try {
                Parcelable[] wifiConfigs = bundle2.getParcelableArray(CONF_KEY_CONFIGURATION);
                if (wifiConfigs != null) {
                    if (wifiConfigs.length != 0) {
                        Log.d(TAG, "setWiFiConfiguration configuration count: " + wifiConfigs.length);
                        int length = wifiConfigs.length;
                        int configurationCount = 0;
                        int configurationCount2 = 0;
                        while (configurationCount2 < length) {
                            Parcelable wifiConfig = wifiConfigs[configurationCount2];
                            int result = 0;
                            configurationCount++;
                            if (configurationCount > 5) {
                                result = 0 | 1;
                            }
                            String networkName = ((Bundle) wifiConfig).getString(CONF_KEY_ROAM_SSID);
                            int roamThreshold = ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_TRIGGER, Integer.MAX_VALUE);
                            int roamDelta = ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_DELTA, Integer.MAX_VALUE);
                            int roamScanPeriod = ((Bundle) wifiConfig).getInt(CONF_KEY_ROAM_SCAN_PERIOD, Integer.MAX_VALUE);
                            int result2 = result | checkConfigurationValidity(networkName, roamThreshold, roamDelta, roamScanPeriod);
                            ((Bundle) wifiConfig).putString(CONF_KEY_ROAM_SSID, "0x" + Integer.toHexString(result2 & 15));
                            if (roamThreshold != Integer.MAX_VALUE) {
                                ((Bundle) wifiConfig).putInt(CONF_KEY_ROAM_TRIGGER, result2 & 17);
                            }
                            if (roamDelta != Integer.MAX_VALUE) {
                                ((Bundle) wifiConfig).putInt(CONF_KEY_ROAM_DELTA, result2 & 33);
                            }
                            if (roamScanPeriod != Integer.MAX_VALUE) {
                                ((Bundle) wifiConfig).putInt(CONF_KEY_ROAM_SCAN_PERIOD, result2 & 65);
                            }
                            if (result2 == 0) {
                                B2BConfiguration conf = new B2BConfiguration();
                                int unused = conf.roamTrigger = roamThreshold;
                                int unused2 = conf.roamDelta = roamDelta;
                                int unused3 = conf.roamScanPeriod = roamScanPeriod;
                                boolean unused4 = conf.noDHCPinRoam = ((Bundle) wifiConfig).getBoolean(CONF_KEY_ROAM_NO_DHCP_RENEWAL, false);
                                boolean unused5 = conf.noBlackList = ((Bundle) wifiConfig).getBoolean(CONF_KEY_NO_CONN_BLACKLIST, false);
                                this.mWiFiB2BConfPolicy.put(networkName, conf);
                            }
                            configurationCount2++;
                            Bundle bundle3 = bundle;
                        }
                        Bundle feedback = new Bundle();
                        feedback.putParcelableArray(CONF_KEY_CONFIGURATION, wifiConfigs);
                        updateEDMConfigurationResult(feedback);
                        dumpLogWiFiConfPolicy();
                        Log.d(TAG, "setWiFiConfiguration end");
                        return;
                    }
                }
                Log.e(TAG, "setWiFiConfiguration Empty Confi");
            } catch (NullPointerException e1) {
                e1.printStackTrace();
            } catch (ClassCastException e) {
                Log.e(TAG, "setWiFiConfiguration Invalid Conf Type");
            }
        }
    }

    private void updateEDMConfigurationResult(Bundle bundle) {
        Context context = this.mContext;
        if (context != null) {
            EnterpriseDeviceManager mEDM = EnterpriseDeviceManager.getInstance(context);
            if (mEDM == null) {
                Log.e(TAG, "setWiFiEDMConfiguration. mEDM is null");
                return;
            }
            ApplicationPolicy mApplicationPolicy = mEDM.getApplicationPolicy();
            if (mApplicationPolicy == null) {
                Log.e(TAG, "setWiFiEDMConfiguration mApplicationPolicy is null");
            } else {
                mApplicationPolicy.setApplicationRestrictions((ComponentName) null, "com.samsung.android.SettingsReceiver.feedback", bundle);
            }
        }
    }

    public static class B2BConfiguration {
        /* access modifiers changed from: private */
        public boolean noBlackList;
        /* access modifiers changed from: private */
        public boolean noDHCPinRoam;
        /* access modifiers changed from: private */
        public int roamDelta;
        /* access modifiers changed from: private */
        public int roamScanPeriod;
        /* access modifiers changed from: private */
        public int roamTrigger;

        public int getRoamTrigger() {
            return this.roamTrigger;
        }

        public int getRoamDelta() {
            return this.roamDelta;
        }

        public int getScanPeriod() {
            return this.roamScanPeriod;
        }

        public boolean skipDHCPRenewal() {
            return this.noDHCPinRoam;
        }

        public boolean skipAddingDisableNetwork() {
            return this.noBlackList;
        }

        public String toString() {
            return "Roam Trigger: " + this.roamTrigger + ", Roam Delta: " + this.roamDelta + ", Roam Scan Period: " + this.roamScanPeriod + ", DHCP skip after Roam: " + this.noDHCPinRoam + ", Not Adding Disable Network: " + this.noBlackList;
        }
    }
}
