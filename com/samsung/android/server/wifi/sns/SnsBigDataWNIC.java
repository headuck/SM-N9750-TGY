package com.samsung.android.server.wifi.sns;

import android.os.Build;
import android.util.Log;

public class SnsBigDataWNIC extends SnsBigDataFeature {
    public static final String KEY_FRONT_PACKAGE_NAME = "Wfpn";
    public static final String KEY_FRONT_PACKAGE_NETWORKCAPABILITIES_INTERNET = "Wfci_i";
    public static final String KEY_FRONT_PACKAGE_NETWORKTRANSPORT_WIFI = "Wftw_i";
    public static final String KEY_FRONT_PACKAGE_USED_TIME = "Wfut_i";
    public static final String KEY_IS_MANUALLY_CONNECTED = "Wimc_i";
    public static final String KEY_POP_UP_HOLDING_TIME = "Wpht_i";
    public static final String KEY_PREVIOUS_PACKAGE_NAME = "Wpfp";
    public static final String KEY_PREVIOUS_PACKAGE_NETWORKCAPABILITIES_INTERNET = "Wpci_i";
    public static final String KEY_PREVIOUS_PACKAGE_NETWORKTRANSPORT_WIFI = "Wptw_i";
    public static final String KEY_PREVIOUS_PACKAGE_USED_TIME = "Wput_i";
    public static final String KEY_VERSION_OF_WNIC_FEATURE = "Wbfv";
    public static final String KEY_WIFI_NO_INTERNET_EVENT = "Wnie_i";
    public static final String KEY_WIFI_POOR_AP_OUI = "Woui";
    public static final String KEY_WIFI_RSSI_STRENGTH = "Wrsi_i";
    private static final String SNSverBasedOnDate = "20171201";
    public static long frontAppearedTime = 0;
    private static String[][] mDataArray = {new String[]{KEY_VERSION_OF_WNIC_FEATURE, "2017120126"}, new String[]{KEY_WIFI_NO_INTERNET_EVENT, "0"}, new String[]{KEY_WIFI_POOR_AP_OUI, ""}, new String[]{KEY_POP_UP_HOLDING_TIME, "0"}, new String[]{KEY_IS_MANUALLY_CONNECTED, "0"}, new String[]{KEY_WIFI_RSSI_STRENGTH, "0"}, new String[]{KEY_FRONT_PACKAGE_NAME, ""}, new String[]{KEY_PREVIOUS_PACKAGE_NAME, ""}, new String[]{KEY_FRONT_PACKAGE_NETWORKCAPABILITIES_INTERNET, "0"}, new String[]{KEY_FRONT_PACKAGE_NETWORKTRANSPORT_WIFI, "0"}, new String[]{KEY_PREVIOUS_PACKAGE_NETWORKCAPABILITIES_INTERNET, "0"}, new String[]{KEY_PREVIOUS_PACKAGE_NETWORKTRANSPORT_WIFI, "0"}, new String[]{KEY_FRONT_PACKAGE_USED_TIME, "0"}, new String[]{KEY_PREVIOUS_PACKAGE_USED_TIME, "0"}};
    public static Object mLock;
    private static String mSnsVers = null;
    public static long prevAppearedTime = 0;

    public SnsBigDataWNIC() {
        mSnsVers = null;
        setSNSver();
        init();
    }

    public SnsBigDataWNIC(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
        mLock = new Object();
        mSnsVers = null;
        setSNSver();
        init();
    }

    public void init() {
        mLock = new Object();
    }

    public static String setSNSver() {
        if (mSnsVers == null) {
            String releaseVer = Integer.toString(Build.VERSION.SDK_INT);
            mSnsVers = SNSverBasedOnDate + releaseVer;
        }
        String[] strArr = mDataArray[0];
        String str = mSnsVers;
        strArr[1] = str;
        return str;
    }

    public String getJsonFormat() {
        setSNSver();
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(mDataArray));
            if (DBG) {
                String str = this.TAG;
                Log.d(str, "getJsonFormat - " + sb.toString());
            }
        } catch (Exception e) {
            if (DBG) {
                String str2 = this.TAG;
                Log.w(str2, "Exception occured on getJsonFormat - " + e);
            }
            e.printStackTrace();
        }
        init();
        return sb.toString();
    }
}
