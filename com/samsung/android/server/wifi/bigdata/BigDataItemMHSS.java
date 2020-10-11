package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHSS extends BaseBigDataItem {
    private static final String KEY_MH_USED_MINIUTES = "mh_umi";
    private static final String KEY_MH_USED_MOBILE_MINIUTES = "mh_mmi";
    private static final String KEY_MH_USED_MOBILE_TIME = "mh_umt";
    private static final String KEY_MH_USED_MOBILE_TX_RX = "mh_mtr";
    private static final String KEY_MH_USED_MOBILE_TX_RX_PACKET = "mh_map";
    private static final String KEY_MH_USED_TIME = "mh_uti";
    private static final String KEY_MH_USED_TX_RX = "mh_utr";
    private static final String KEY_MH_USED_TX_RX_PACKET = "mh_uap";
    private static final String KEY_MH_USED_WIFI_MINIUTES = "mh_wmi";
    private static final String KEY_MH_USED_WIFI_TIME = "mh_uwt";
    private static final String KEY_MH_USED_WIFI_TX_RX = "mh_wtr";
    private static final String KEY_MH_USED_WIFI_TX_RX_PACKET = "mh_wap";
    private static final String[][] MHSS = {new String[]{KEY_MH_USED_MINIUTES, ""}, new String[]{KEY_MH_USED_MOBILE_MINIUTES, ""}, new String[]{KEY_MH_USED_WIFI_MINIUTES, ""}, new String[]{KEY_MH_USED_TIME, ""}, new String[]{KEY_MH_USED_MOBILE_TIME, ""}, new String[]{KEY_MH_USED_WIFI_TIME, ""}, new String[]{KEY_MH_USED_TX_RX, ""}, new String[]{KEY_MH_USED_MOBILE_TX_RX, ""}, new String[]{KEY_MH_USED_WIFI_TX_RX, ""}, new String[]{KEY_MH_USED_TX_RX_PACKET, ""}, new String[]{KEY_MH_USED_MOBILE_TX_RX_PACKET, ""}, new String[]{KEY_MH_USED_WIFI_TX_RX_PACKET, ""}};

    public BigDataItemMHSS(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHSS);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHSS;
            if (length == strArr.length) {
                putValues(strArr, array);
                return true;
            }
        }
        if (!this.mLogMessages) {
            return false;
        }
        String str = this.TAG;
        Log.e(str, "can't pase bigdata extra - data:" + data);
        return false;
    }
}
