package com.samsung.android.server.wifi.bigdata;

import android.util.Log;

class BigDataItemHANG extends BaseBigDataItem {
    private static final String HANG_START_STRING = "HANGED";
    private static final String[][] HANG_WITH_DUMP = {new String[]{KEY_FW_HANG_REASON, ""}, new String[]{KEY_VER, ""}, new String[]{KEY_COOK_TIME, ""}, new String[]{KEY_HANG_REASON, ""}, new String[]{KEY_HANG_HEAP_TOTAL, ""}, new String[]{KEY_HANG_FREE_MEM, ""}, new String[]{KEY_HANG_USED_MEM, ""}, new String[]{KEY_HANG_ALLOC_FAIL_COUNT, ""}, new String[]{KEY_HANG_TRAP_RAW, ""}, new String[]{KEY_HANG_STACK_RAW, ""}};
    private static final String KEY_COOK_TIME = "COOK";
    private static final String KEY_FW_HANG_REASON = "fw_han";
    private static final String KEY_HANG_ALLOC_FAIL_COUNT = "HG05";
    private static final String KEY_HANG_FREE_MEM = "HG03";
    private static final String KEY_HANG_HEAP_TOTAL = "HG02";
    private static final String KEY_HANG_REASON = "HG01";
    private static final String KEY_HANG_STACK_RAW = "RAW";
    private static final String KEY_HANG_TRAP_RAW = "HG06";
    private static final String KEY_HANG_USED_MEM = "HG04";
    private static final String KEY_UID = "UID";
    private static final String KEY_VER = "VER";

    public BigDataItemHANG(String featureName) {
        super(featureName);
    }

    public String getJsonFormat() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        return wifiChipInfo.toString() + "," + wifiChipInfo.getCidInfoForKeyValueType() + "," + getKeyValueStrings(HANG_WITH_DUMP);
    }

    public boolean parseData(String data) {
        String[] array = getArray(data);
        if (array.length == 1) {
            putValue(KEY_FW_HANG_REASON, "0");
            return true;
        } else if (array.length == 2) {
            putValue(KEY_FW_HANG_REASON, array[1]);
            return true;
        } else if (array.length == HANG_WITH_DUMP.length + 1) {
            String[] array2 = getArray(data.substring(HANG_START_STRING.length()).trim());
            if (array2 != null) {
                int length = array2.length;
                String[][] strArr = HANG_WITH_DUMP;
                if (length == strArr.length) {
                    putValues(strArr, array2);
                    return true;
                }
            }
            if (this.mLogMessages) {
                String str = this.TAG;
                Log.e(str, "can't parse bigdata extra - data:" + data);
            }
            return false;
        } else {
            if (this.mLogMessages) {
                String str2 = this.TAG;
                Log.e(str2, "can't parse bigdata extra - data:" + data);
            }
            return false;
        }
    }

    public boolean isAvailableLogging(int type) {
        if (type == 1) {
            return true;
        }
        return super.isAvailableLogging(type);
    }
}
