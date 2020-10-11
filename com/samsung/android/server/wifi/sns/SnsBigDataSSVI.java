package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataSSVI extends SnsBigDataFeature {
    public static final String KEY_AP_OUI = "VAPO";
    public static final String KEY_QC_AGGRESSIVE_MODE_ENABLED = "VAME";
    public static final String KEY_QC_BOOTING_ELAPSED_TIME = "VCBT";
    public static final String KEY_QC_COUNTRY_ISO = "VISO";
    public static final String KEY_QC_FAIL_REASON = "VFRN";
    public static final String KEY_QC_LINK_SPEED = "VLSP";
    public static final String KEY_QC_POOR_DETECTION_ENABLED = "VPDE";
    public static final String KEY_QC_QC_UI_ENABLED = "VUIE";
    public static final String KEY_QC_RSSI = "VRSS";
    public static final String KEY_QC_SECOND_PACKAGE = "VSPG";
    public static final String KEY_QC_SECOND_PACKAGE_DURATION = "VSPD";
    public static final String KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE = "VSPM";
    public static final String KEY_QC_SECOND_PACKAGE_WIFI_USAGE = "VSPW";
    public static final String KEY_QC_STATE = "VSTA";
    public static final String KEY_QC_STEP = "VSTE";
    public static final String KEY_QC_TOP_PACKAGE = "VTPG";
    public static final String KEY_QC_TOP_PACKAGE_DURATION = "VTPD";
    public static final String KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE = "VTPM";
    public static final String KEY_QC_TOP_PACKAGE_WIFI_USAGE = "VTPW";
    public static final String KEY_QC_TRIGGER = "VTRI";
    public static final String KEY_QC_TYPE = "VTYP";
    private static final String KEY_QC_VERSION = "SVER";
    public static final String KEY_SNS_CONNECTED_STAY_TIME = "VCST";
    public static final String KEY_SNS_GOOD_LINK_TARGET_RSSI = "VGLT";
    private static final String[][] SSVI = {new String[]{KEY_QC_VERSION, "2020001029"}, new String[]{KEY_QC_STATE, "-1"}, new String[]{KEY_QC_TYPE, "-1"}, new String[]{KEY_QC_STEP, "-1"}, new String[]{KEY_QC_TRIGGER, "-1"}, new String[]{KEY_QC_FAIL_REASON, "-1"}, new String[]{KEY_QC_BOOTING_ELAPSED_TIME, "-1"}, new String[]{KEY_QC_TOP_PACKAGE, ""}, new String[]{KEY_QC_TOP_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE, ""}, new String[]{KEY_QC_SECOND_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_COUNTRY_ISO, ""}, new String[]{KEY_QC_RSSI, "-1"}, new String[]{KEY_QC_LINK_SPEED, "-1"}, new String[]{KEY_QC_POOR_DETECTION_ENABLED, "-1"}, new String[]{KEY_QC_QC_UI_ENABLED, "-1"}, new String[]{KEY_QC_AGGRESSIVE_MODE_ENABLED, "-1"}, new String[]{KEY_AP_OUI, "e0:cb:ee"}, new String[]{KEY_SNS_GOOD_LINK_TARGET_RSSI, "-1"}, new String[]{KEY_SNS_CONNECTED_STAY_TIME, "-1"}};

    public SnsBigDataSSVI(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public SnsBigDataSSVI() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(SSVI));
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
