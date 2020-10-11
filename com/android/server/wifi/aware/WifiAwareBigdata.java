package com.android.server.wifi.aware;

import android.util.Log;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;

public class WifiAwareBigdata {
    public static final String APP_ID = "android.net.wifi.aware";
    private static final String KEY_CLIENT_ID = "CLID";
    private static final String KEY_CNM = "WCVN";
    private static final String KEY_DRV_VER = "WDRV";
    private static final String KEY_FW_VER = "WFWV";
    private static final String KEY_NAN_OP_DURATION = "NOPD";
    private static final String KEY_OP_TYPE = "OPER";
    private static final String KEY_PKG_NAME = "PKGN";
    private static final String KEY_RANGE_MM = "RNGM";
    private static final String KEY_SERVICE_NAME = "SVCN";
    private static final String TAG = "WifiAwareBigdata";
    public String mChipsetName;
    public String mClientId;
    public String mDriverVer;
    public String mFirmwareVer;
    public String mNanOpDuration;
    public String mOperationType;
    public String mPackageName;
    public String mRangeMm;
    public String mServiceName;

    public void initialize() {
        WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
        if ((this.mFirmwareVer == null || this.mDriverVer == null || this.mChipsetName == null) && wifiChipInfo != null && wifiChipInfo.isReady()) {
            this.mFirmwareVer = wifiChipInfo.getFirmwareVer(false);
            this.mDriverVer = wifiChipInfo.getDriverVer();
            this.mChipsetName = WifiChipInfo.getChipsetName();
        }
        this.mPackageName = "";
        this.mOperationType = "";
        this.mClientId = "";
        this.mRangeMm = "";
        this.mServiceName = "";
        this.mNanOpDuration = "";
    }

    public String getJsonFormat(String feature) {
        if (feature == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if ("WAOR".equals(feature)) {
            sb.append(convertToQuotedString(KEY_FW_VER) + ":");
            sb.append(convertToQuotedString(this.mFirmwareVer) + ",");
            sb.append(convertToQuotedString(KEY_DRV_VER) + ":");
            sb.append(convertToQuotedString(this.mDriverVer) + ",");
            sb.append(convertToQuotedString(KEY_CNM) + ":");
            sb.append(convertToQuotedString(this.mChipsetName) + ",");
            sb.append(convertToQuotedString(KEY_PKG_NAME) + ":");
            sb.append(convertToQuotedString(this.mPackageName) + ",");
            sb.append(convertToQuotedString(KEY_OP_TYPE) + ":");
            sb.append(convertToQuotedString(this.mOperationType) + ",");
            sb.append(convertToQuotedString(KEY_CLIENT_ID) + ":");
            sb.append(convertToQuotedString(this.mClientId) + ",");
            sb.append(convertToQuotedString(KEY_RANGE_MM) + ":");
            sb.append(convertToQuotedString(this.mRangeMm) + ",");
            sb.append(convertToQuotedString(KEY_SERVICE_NAME) + ":");
            sb.append(convertToQuotedString(this.mServiceName) + ",");
            sb.append(convertToQuotedString(KEY_NAN_OP_DURATION) + ":");
            sb.append(convertToQuotedString(this.mNanOpDuration));
        } else {
            Log.e(TAG, "Undefined feature : " + feature);
        }
        return sb.toString();
    }

    public boolean parseData(String feature, String data) {
        if (feature == null || data == null) {
            return false;
        }
        String[] array = data.split("\\s+");
        if (!"WAOR".equals(feature)) {
            Log.e(TAG, "Undefined feature : " + feature);
            return false;
        } else if (array.length != 6) {
            Log.d(TAG, "Wrong parseData for WAOR, length : " + array.length);
            return false;
        } else {
            int index = 0 + 1;
            this.mPackageName = array[0];
            int index2 = index + 1;
            this.mOperationType = array[index];
            int index3 = index2 + 1;
            this.mClientId = array[index2];
            int index4 = index3 + 1;
            this.mRangeMm = array[index3];
            int index5 = index4 + 1;
            this.mServiceName = array[index4];
            int i = index5 + 1;
            this.mNanOpDuration = array[index5];
            return true;
        }
    }

    private String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private String removeDoubleQuotes(String string) {
        if (string == null) {
            return null;
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
