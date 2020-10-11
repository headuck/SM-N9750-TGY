package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataWFCP extends SnsBigDataFeature {
    public static final String KEY_CP_AUTO = "CAut";
    public static final String KEY_CP_CONNECTION_DURATION = "CDur";
    public static final String KEY_CP_DETECTION = "CDet";
    public static final String KEY_CP_OPTION = "COpt";
    public static final String KEY_CP_OUI = "COui";
    public static final String KEY_CP_PAGE_COUNT = "CPag";
    public static final String KEY_CP_REDIRECT_URL = "CRed";
    public static final String KEY_CP_RESULT = "CRes";
    public static final String KEY_CP_SECURE_TYPE = "CSec";
    public static final String KEY_CP_UNAUTHENTICATED_DURATION = "CUna";
    private static final String KEY_CP_VERSION = "CVer";
    public static final String KEY_CP_WEBVIEW = "CWeb";
    private static final String[][] WFCP = {new String[]{KEY_CP_VERSION, "2017061924"}, new String[]{KEY_CP_DETECTION, "0"}, new String[]{KEY_CP_RESULT, "0"}, new String[]{KEY_CP_WEBVIEW, "0"}, new String[]{KEY_CP_AUTO, ""}, new String[]{KEY_CP_OPTION, "0"}, new String[]{KEY_CP_PAGE_COUNT, "0"}, new String[]{KEY_CP_REDIRECT_URL, "0"}, new String[]{KEY_CP_UNAUTHENTICATED_DURATION, "0"}, new String[]{KEY_CP_CONNECTION_DURATION, "0"}, new String[]{KEY_CP_SECURE_TYPE, "0"}, new String[]{KEY_CP_OUI, ""}};

    public SnsBigDataWFCP(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public SnsBigDataWFCP() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(WFCP));
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
