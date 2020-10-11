package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemPDC2 extends BaseBigDataItem {
    private static final String KEY_ALGORITH_VERSION = "aver";
    private static final String KEY_ASSOC_COUNT = "cntA";
    private static final String KEY_HANG_REASON = "hanR";
    private static final String KEY_ISSUE_DETECTOR_CATEGORY = "isct";
    private static final String KEY_LAST_PROCESS_STATE = "pres";
    private static final String KEY_PACKAGE_NAME = "pkgN";
    private static final String KEY_PRE_PROCESS_MSG = "prem";
    static final String KEY_PRIVATE_BSSID = "bsid";
    private static final String KEY_PRIVATE_SSID = "ssid";
    private static final String KEY_REASON = "resn";
    private static final String KEY_SUPPLICANT_RETRY_COUNT = "cntR";
    private static final String[][] PDC2 = {new String[]{KEY_ISSUE_DETECTOR_CATEGORY, "0"}, new String[]{KEY_LAST_PROCESS_STATE, "UnknownState"}, new String[]{KEY_PRE_PROCESS_MSG, "0"}, new String[]{KEY_HANG_REASON, "0"}, new String[]{KEY_SUPPLICANT_RETRY_COUNT, "0"}, new String[]{KEY_REASON, "0"}, new String[]{KEY_ASSOC_COUNT, "0"}, new String[]{"ap_rsi", "0"}, new String[]{"ap_oui", "00:00:00"}, new String[]{"ap_sec", "0"}, new String[]{"ap_chn", "0"}, new String[]{KEY_PACKAGE_NAME, "unknown"}, new String[]{KEY_ALGORITH_VERSION, "0"}, new String[]{"ssid", "unknown"}, new String[]{KEY_PRIVATE_BSSID, "00:00:00:00:00:00"}};

    public BigDataItemPDC2(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        return null;
    }

    public String getJsonFormatFor(int type) {
        int lastIndexOfCommonData = PDC2.length - 2;
        if (type == 2) {
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append(wifiChipInfo.toString());
            sb.append(",");
            for (int i = 0; i < lastIndexOfCommonData; i++) {
                String[][] strArr = PDC2;
                sb.append(getKeyValueString(strArr[i][0], strArr[i][1]));
                if (i != lastIndexOfCommonData - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        } else if (type != 3) {
            return null;
        } else {
            StringBuilder sb2 = new StringBuilder();
            int i2 = lastIndexOfCommonData;
            while (true) {
                String[][] strArr2 = PDC2;
                if (i2 >= strArr2.length) {
                    return sb2.toString();
                }
                sb2.append(getKeyValueString(strArr2[i2][0], strArr2[i2][1]));
                if (i2 != PDC2.length - 1) {
                    sb2.append(",");
                }
                i2++;
            }
        }
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array != null) {
            int length = array.length;
            String[][] strArr = PDC2;
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

    public String getHitType() {
        return BaseBigDataItem.HIT_TYPE_IMMEDIATLY;
    }

    public boolean isAvailableLogging(int type) {
        if (type == 2 || type == 3) {
            return true;
        }
        return false;
    }
}
