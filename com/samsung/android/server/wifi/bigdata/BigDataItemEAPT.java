package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemEAPT extends BaseBigDataItem {
    private static final String[][] EAPINFO = {new String[]{KEY_ET_EAP_TYPE, ""}, new String[]{KEY_ET_PHASE1_TYPE, ""}, new String[]{KEY_ET_PHASE2_TYPE, ""}, new String[]{KEY_ET_USE_CACERTI, ""}, new String[]{KEY_ET_USE_ANONYMOUS, ""}, new String[]{KEY_ET_EAP_SSID, ""}, new String[]{KEY_ET_EAP_STATE, ""}, new String[]{KEY_ET_EAP_NOTI, ""}, new String[]{KEY_ET_EAP_KEYMGMT, ""}};
    private static final String KEY_ET_EAP_KEYMGMT = "et_kmt";
    private static final String KEY_ET_EAP_NOTI = "et_not";
    private static final String KEY_ET_EAP_SSID = "et_sid";
    private static final String KEY_ET_EAP_STATE = "et_stt";
    private static final String KEY_ET_EAP_TYPE = "et_typ";
    private static final String KEY_ET_PHASE1_TYPE = "et_pho";
    private static final String KEY_ET_PHASE2_TYPE = "et_pht";
    private static final String KEY_ET_USE_ANONYMOUS = "et_ani";
    private static final String KEY_ET_USE_CACERTI = "et_cac";

    public BigDataItemEAPT(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return getKeyValueStrings(EAPINFO);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = EAPINFO;
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
