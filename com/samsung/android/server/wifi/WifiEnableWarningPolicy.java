package com.samsung.android.server.wifi;

import android.content.Context;
import android.os.Debug;
import android.provider.Settings;
import android.util.Log;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.C0852CscFeatureTagCommon;
import java.io.File;

public class WifiEnableWarningPolicy {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String DEX_SYSTEM_UI_PACKAGE_NAME = "com.samsung.desktopsystemui";
    private static final String FACTORY_RESET = "factory.reset.";
    private static final String P2P_PACKAGE_NAME = "com.android.server.wifi.p2p";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "WifiEnableWarningPolicy";
    public static final String WIFI_STATE_CHANGE_WARNING = "state_change_warning.";
    private String CSC_COMMON_CHINA_NAL_SECURITY_TYPE = SemCscFeature.getInstance().getString(C0852CscFeatureTagCommon.TAG_CSCFEATURE_COMMON_CONFIGLOCALSECURITYPOLICY);
    private final Context mContext;
    private boolean mEnableForTest = false;

    public WifiEnableWarningPolicy(Context context) {
        this.mContext = context;
    }

    public void testConfig(boolean enable) {
        this.mEnableForTest = enable;
    }

    public boolean needToShowWarningDialog(int wifiApState, int wifiState, boolean isWifiSharingEnabled, String packageName) {
        if ((wifiApState != 11 && wifiApState != 10 && !isWifiSharingEnabled) || wifiState == 3 || wifiState == 2 || isUserAction(packageName)) {
            return false;
        }
        SemWifiFrameworkUxUtils.showWarningDialog(this.mContext, 1, new String[]{packageName});
        return true;
    }

    public boolean isAllowWifiWarning() {
        if (!DBG || !this.mEnableForTest) {
            boolean isCscWifiEnableWarning = "ChinaNalSecurity".equals(this.CSC_COMMON_CHINA_NAL_SECURITY_TYPE);
            if (DBG) {
                Log.i(TAG, "isAllowWifiWarning: isCscWifiEnableWarning = " + isCscWifiEnableWarning);
            }
            boolean isAllowPopup = Settings.Secure.getInt(this.mContext.getContentResolver(), "wlan_permission_available", 1) == 1;
            if (!isCscWifiEnableWarning || !isAllowPopup) {
                return false;
            }
            return true;
        }
        Log.i(TAG, "it's test mode, Wi-Fi warning policy is enabled");
        return true;
    }

    private boolean isUserAction(String packageName) {
        Log.d(TAG, "processName = " + packageName);
        if ("android".equals(packageName.toLowerCase()) || P2P_PACKAGE_NAME.equals(packageName.toLowerCase()) || "com.android.systemui".equals(packageName.toLowerCase()) || DEX_SYSTEM_UI_PACKAGE_NAME.equals(packageName.toLowerCase()) || SETTINGS_PACKAGE_NAME.equals(packageName.toLowerCase()) || AutoWifiController.AUTO_WIFI_PACKAGE_NAME.equals(packageName.toLowerCase()) || "com.sec.android.app.secsetupwizard".equals(packageName.toLowerCase()) || "com.sec.android.easysettings".equals(packageName.toLowerCase()) || "com.sec.android.emergencymode.service".equals(packageName.toLowerCase()) || "com.sec.NetworkPowerSaving".equals(packageName) || "com.android.nfc".equals(packageName) || "com.salab.act".equals(packageName) || "com.sec.android.app.wlandebugtool".equals(packageName) || "com.samsung.android.oneconnect".equals(packageName.toLowerCase()) || "com.samsung.android.app.sreminder".equals(packageName.toLowerCase()) || "com.samsung.android.app.routines".equals(packageName.toLowerCase()) || "com.samsung.android.mcfserver".equals(packageName.toLowerCase()) || packageName.startsWith(WIFI_STATE_CHANGE_WARNING) || packageName.startsWith(FACTORY_RESET)) {
            return true;
        }
        if (!"com.sec.knox.kccagent".equals(packageName.toLowerCase()) || isCustomizedByKccAgent()) {
            return false;
        }
        return true;
    }

    private boolean isCustomizedByKccAgent() {
        return new File("/data/data/com.sec.knox.kccagent/shared_prefs/customized.xml").exists();
    }
}
