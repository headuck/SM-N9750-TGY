package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataSSIV extends SnsBigDataFeature {
    public static final String KEY_AP_OUI = "IAPO";
    public static final String KEY_QC_AGGRESSIVE_MODE_ENABLED = "IAME";
    public static final String KEY_QC_BOOTING_ELAPSED_TIME = "ICBT";
    public static final String KEY_QC_COUNTRY_ISO = "IISO";
    public static final String KEY_QC_FAIL_REASON = "IFRN";
    public static final String KEY_QC_LINK_SPEED = "ILSP";
    public static final String KEY_QC_POOR_DETECTION_ENABLED = "IPDE";
    public static final String KEY_QC_QC_UI_ENABLED = "IUIE";
    public static final String KEY_QC_RSSI = "IRSS";
    public static final String KEY_QC_SECOND_PACKAGE = "ISPG";
    public static final String KEY_QC_SECOND_PACKAGE_DURATION = "ISPD";
    public static final String KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE = "ISPM";
    public static final String KEY_QC_SECOND_PACKAGE_WIFI_USAGE = "ISPW";
    public static final String KEY_QC_STATE = "ISTA";
    public static final String KEY_QC_STEP = "ISTE";
    public static final String KEY_QC_TOP_PACKAGE = "ITPG";
    public static final String KEY_QC_TOP_PACKAGE_DURATION = "ITPD";
    public static final String KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE = "ITPM";
    public static final String KEY_QC_TOP_PACKAGE_WIFI_USAGE = "ITPW";
    public static final String KEY_QC_TRIGGER = "ITRI";
    public static final String KEY_QC_TYPE = "ITYP";
    private static final String KEY_QC_VERSION = "IVER";
    public static final String KEY_SNS_GOOD_LINK_TARGET_RSSI = "IGLT";
    public static final String KEY_SNS_L2_CONNECTED_STAY_TIME = "ICST";
    private static final String[][] SSIV = {new String[]{KEY_QC_VERSION, "2020001029"}, new String[]{KEY_QC_STATE, "-1"}, new String[]{KEY_QC_TYPE, "-1"}, new String[]{KEY_QC_STEP, "-1"}, new String[]{KEY_QC_TRIGGER, "-1"}, new String[]{KEY_QC_FAIL_REASON, "-1"}, new String[]{KEY_QC_BOOTING_ELAPSED_TIME, "-1"}, new String[]{KEY_QC_TOP_PACKAGE, ""}, new String[]{KEY_QC_TOP_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE, ""}, new String[]{KEY_QC_SECOND_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_COUNTRY_ISO, ""}, new String[]{KEY_QC_RSSI, "-1"}, new String[]{KEY_QC_LINK_SPEED, "-1"}, new String[]{KEY_QC_POOR_DETECTION_ENABLED, "-1"}, new String[]{KEY_QC_QC_UI_ENABLED, "-1"}, new String[]{KEY_QC_AGGRESSIVE_MODE_ENABLED, "-1"}, new String[]{KEY_AP_OUI, "e0:cb:ee"}, new String[]{KEY_SNS_GOOD_LINK_TARGET_RSSI, "-1"}, new String[]{KEY_SNS_L2_CONNECTED_STAY_TIME, "-1"}};

    public SnsBigDataSSIV(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public SnsBigDataSSIV() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(SSIV));
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
        return sb.toString();
    }
}
