package com.samsung.android.server.wifi.sns;

import android.os.Build;
import android.util.Log;

public class SnsBigDataWFSN extends SnsBigDataFeature {
    public static final String SNS_CURRENT_STATUS = "Scst";
    public static final String SNS_FRONTAPP_USEDTIME = "SNfu";
    public static final String SNS_MOBILEDATA_FRONT = "SNmf";
    public static final String SNS_MOBILEDATA_PREV = "SNmp";
    public static final String SNS_PACKAGE_FRONT = "SNpf";
    public static final String SNS_PREVAPP_USEDTIME = "SNpu";
    public static final String SNS_PREVIOUS_STATUS = "Spst";
    public static final String SNS_PREVIOUS_STAUS_USED_TIME = "Sput";
    public static final String SNS_PREV_PACKAGE = "SNpp";
    public static final String SNS_STATE_INFO = "SNsi";
    public static final String SNS_STATE_TIME = "SNst";
    public static final String SNS_STATUS_CHANGING_METHOD = "Sscm";
    public static final String SNS_Version = "Sver";
    public static final String SNS_WIFIDATA_FRONT = "SNwf";
    public static final String SNS_WIFIDATA_PREV = "SNwp";
    private static final String SNSverBasedOnDate = "20170731";
    private static String[][] mDataArray = {new String[]{SNS_Version, "0"}, new String[]{SNS_CURRENT_STATUS, "0"}, new String[]{SNS_PREVIOUS_STATUS, "0"}, new String[]{SNS_PREVIOUS_STAUS_USED_TIME, "0"}, new String[]{SNS_STATUS_CHANGING_METHOD, "0"}, new String[]{SNS_STATE_INFO, ""}, new String[]{SNS_STATE_TIME, "0"}, new String[]{SNS_PACKAGE_FRONT, ""}, new String[]{SNS_PREV_PACKAGE, ""}, new String[]{SNS_MOBILEDATA_FRONT, "0"}, new String[]{SNS_MOBILEDATA_PREV, "0"}, new String[]{SNS_WIFIDATA_FRONT, "0"}, new String[]{SNS_WIFIDATA_PREV, "0"}, new String[]{SNS_FRONTAPP_USEDTIME, "0"}, new String[]{SNS_PREVAPP_USEDTIME, "0"}};
    private static String mSnsVers = null;

    public SnsBigDataWFSN() {
        setSNSver();
    }

    public SnsBigDataWFSN(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
        setSNSver();
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
        return sb.toString();
    }
}
