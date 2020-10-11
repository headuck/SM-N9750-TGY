package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataWFQC extends SnsBigDataFeature {
    public static final String KEY_QC_AGGRESSIVE_MODE_ENABLED = "QAME";
    public static final String KEY_QC_BOOTING_ELAPSED_TIME = "QCBT";
    public static final String KEY_QC_FAIL_REASON = "QFRN";
    public static final String KEY_QC_LINK_SPEED = "QLSP";
    public static final String KEY_QC_POOR_DETECTION_ENABLED = "QPDE";
    public static final String KEY_QC_QC_UI_ENABLED = "QUIE";
    public static final String KEY_QC_QC_URL_INDEX = "QUID";
    public static final String KEY_QC_RESULT = "QRES";
    public static final String KEY_QC_RSSI = "QRSS";
    public static final String KEY_QC_SECOND_PACKAGE = "QSPG";
    public static final String KEY_QC_SECOND_PACKAGE_DURATION = "QSPD";
    public static final String KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE = "QSPM";
    public static final String KEY_QC_SECOND_PACKAGE_WIFI_USAGE = "QSPW";
    public static final String KEY_QC_STATE = "QSTA";
    public static final String KEY_QC_STEP = "QSTE";
    public static final String KEY_QC_TOP_PACKAGE = "QTPG";
    public static final String KEY_QC_TOP_PACKAGE_DURATION = "QTPD";
    public static final String KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE = "QTPM";
    public static final String KEY_QC_TOP_PACKAGE_WIFI_USAGE = "QTPW";
    public static final String KEY_QC_TRIGGER = "QTRI";
    public static final String KEY_QC_TYPE = "QTYP";
    private static final String KEY_QC_VERSION = "QVER";
    private static final String[][] WFQC = {new String[]{KEY_QC_VERSION, "2020001029"}, new String[]{KEY_QC_RESULT, "-1"}, new String[]{KEY_QC_TYPE, "-1"}, new String[]{KEY_QC_STEP, "-1"}, new String[]{KEY_QC_TRIGGER, "-1"}, new String[]{KEY_QC_FAIL_REASON, "-1"}, new String[]{KEY_QC_BOOTING_ELAPSED_TIME, "-1"}, new String[]{KEY_QC_TOP_PACKAGE, ""}, new String[]{KEY_QC_TOP_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_TOP_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE, ""}, new String[]{KEY_QC_SECOND_PACKAGE_DURATION, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_MOBILEDATA_USAGE, "-1"}, new String[]{KEY_QC_SECOND_PACKAGE_WIFI_USAGE, "-1"}, new String[]{KEY_QC_STATE, ""}, new String[]{KEY_QC_QC_URL_INDEX, "-1"}, new String[]{KEY_QC_RSSI, "-1"}, new String[]{KEY_QC_LINK_SPEED, "-1"}, new String[]{KEY_QC_POOR_DETECTION_ENABLED, "-1"}, new String[]{KEY_QC_QC_UI_ENABLED, "-1"}, new String[]{KEY_QC_AGGRESSIVE_MODE_ENABLED, "-1"}};

    public SnsBigDataWFQC(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public SnsBigDataWFQC() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(WFQC));
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
