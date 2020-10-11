package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemMHDC extends BaseBigDataItem {
    private static final String KEY_MHDC_ANT_MODE = "mh_antmode";
    private static final String KEY_MHDC_BW = "mh_bw";
    private static final String KEY_MHDC_DATA_RATE = "mh_datarate";
    private static final String KEY_MHDC_DIS = "mh_dis";
    private static final String KEY_MHDC_MODE = "mh_mode";
    private static final String KEY_MHDC_MU_MIMO = "mh_mumimo";
    private static final String KEY_MHDC_OUI = "mh_oui";
    private static final String KEY_MHDC_RSSI = "mh_rssi";
    private static final String KEY_MHDC_SAM_RSN = "mh_srsn";
    private static final String KEY_MHDC_WFA_RSN = "mh_wrsn";
    private static final String[][] MHDC = {new String[]{KEY_MHDC_OUI, ""}, new String[]{KEY_MHDC_DIS, ""}, new String[]{KEY_MHDC_SAM_RSN, ""}, new String[]{KEY_MHDC_WFA_RSN, ""}, new String[]{KEY_MHDC_BW, ""}, new String[]{KEY_MHDC_RSSI, ""}, new String[]{KEY_MHDC_DATA_RATE, ""}, new String[]{KEY_MHDC_MODE, ""}, new String[]{KEY_MHDC_ANT_MODE, ""}, new String[]{KEY_MHDC_MU_MIMO, ""}};

    public BigDataItemMHDC(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(MHDC);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = MHDC;
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
