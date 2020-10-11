package com.samsung.android.server.wifi;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.dqa.ReportIdKey;
import java.util.HashMap;
import java.util.Map;

public class WifiGuiderFeatureController {
    private static final String KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT = "autoiwifiTurnOffScanCount";
    private static final String KEY_AUTO_WIFI_TURN_OFF_SECONDS = "autowifiTurnOffSeconds";
    private static final String KEY_BSSID_COUNT = "autowifiMaxBssidCount";
    private static final String KEY_CELL_COUNT = "autowifiMaxCellCount";
    private static final String KEY_HOTSPOT20 = "hotspot20Enabled";
    private static final String KEY_MWIPS = "wifiMWipsEnabled";
    private static final String KEY_POWERSAVE = "powersaveEnabled";
    private static final String KEY_PROFILE_REQUEST = "wifiProfileRequest";
    private static final String KEY_PROFILE_SHARE = "wifiProfileShare";
    private static final String KEY_QOS_PROVIDER = "qosDeviceShare";
    private static final String KEY_RESET = "resetAll";
    private static final String KEY_RSSI24 = "rssi24threshold";
    private static final String KEY_RSSI5 = "rssi5threshold";
    private static final String KEY_SAFEMODE = "wifiSafeMode";
    private static final String KEY_SCORE_PROVIDER = "networkScoreProvider";
    public static final String TAG = "WifiGuiderFeatureController";
    private static final String[] mKeys = {KEY_RSSI24, KEY_RSSI5, KEY_CELL_COUNT, KEY_BSSID_COUNT, KEY_SAFEMODE, KEY_HOTSPOT20, KEY_POWERSAVE, KEY_AUTO_WIFI_TURN_OFF_SECONDS, KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT, KEY_MWIPS, KEY_QOS_PROVIDER, KEY_SCORE_PROVIDER, KEY_PROFILE_SHARE, KEY_PROFILE_REQUEST};
    private static WifiGuiderFeatureController sInstance;
    private final Context mContext;
    private final HashMap<String, Integer> mFeatureDefaultMap = new HashMap<>();
    private final HashMap<String, Integer> mFeatureMap = new HashMap<>();

    private WifiGuiderFeatureController(Context context) {
        this.mContext = context;
        initDefaultMap();
    }

    public static synchronized WifiGuiderFeatureController getInstance(Context context) {
        WifiGuiderFeatureController wifiGuiderFeatureController;
        synchronized (WifiGuiderFeatureController.class) {
            if (sInstance == null) {
                sInstance = new WifiGuiderFeatureController(context);
            }
            wifiGuiderFeatureController = sInstance;
        }
        return wifiGuiderFeatureController;
    }

    private void initDefaultMap() {
        this.mFeatureDefaultMap.put(KEY_RSSI24, -78);
        this.mFeatureDefaultMap.put(KEY_RSSI5, -75);
        this.mFeatureDefaultMap.put(KEY_CELL_COUNT, 100);
        this.mFeatureDefaultMap.put(KEY_BSSID_COUNT, 10);
        this.mFeatureDefaultMap.put(KEY_POWERSAVE, 0);
        this.mFeatureDefaultMap.put(KEY_SAFEMODE, 0);
        Log.d(TAG, "getPasspointDefaultValue():" + getPasspointDefaultValue());
        this.mFeatureDefaultMap.put(KEY_HOTSPOT20, Integer.valueOf(getPasspointDefaultValue()));
        this.mFeatureDefaultMap.put(KEY_AUTO_WIFI_TURN_OFF_SECONDS, Integer.valueOf(ReportIdKey.ID_DHCP_FAIL));
        this.mFeatureDefaultMap.put(KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT, 3);
        this.mFeatureDefaultMap.put(KEY_MWIPS, 1);
        this.mFeatureDefaultMap.put(KEY_QOS_PROVIDER, 1);
        this.mFeatureDefaultMap.put(KEY_SCORE_PROVIDER, 1);
        this.mFeatureDefaultMap.put(KEY_PROFILE_SHARE, 1);
        this.mFeatureDefaultMap.put(KEY_PROFILE_REQUEST, 1);
    }

    private void getFeatureFromDB() {
        String str = Settings.Global.getString(this.mContext.getContentResolver(), "wifi_guider_feature_control");
        if (str != null && !str.isEmpty()) {
            Log.d(TAG, "getFeatureFromDB:" + str);
            String[] arr = str.replace("{", "").replace("}", "").split(",");
            for (String split : arr) {
                String[] values = split.split("=");
                if (values.length == 2) {
                    String key = values[0].trim();
                    String value = values[1];
                    Log.d(TAG, key + "=" + value);
                    try {
                        this.mFeatureMap.put(key, Integer.valueOf(Integer.parseInt(value)));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isFeatureControlDBempty() {
        String str = Settings.Global.getString(this.mContext.getContentResolver(), "wifi_guider_feature_control");
        if (str == null || str.isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean checkAndReset(Bundle bundle) {
        Log.d(TAG, "checkAndReset");
        if (!bundle.containsKey(KEY_RESET) || bundle.getInt(KEY_RESET) != 1) {
            return false;
        }
        Log.d(TAG, "KEY_RESET");
        for (Map.Entry entry : this.mFeatureMap.entrySet()) {
            String key = (String) entry.getKey();
            int currentValue = ((Integer) entry.getValue()).intValue();
            int defaultValue = this.mFeatureDefaultMap.get(key).intValue();
            Log.d(TAG, "key:" + key + ", currentValue:" + currentValue);
            char c = 65535;
            switch (key.hashCode()) {
                case -200000289:
                    if (key.equals(KEY_POWERSAVE)) {
                        c = 1;
                        break;
                    }
                    break;
                case 667407668:
                    if (key.equals(KEY_HOTSPOT20)) {
                        c = 2;
                        break;
                    }
                    break;
                case 797931060:
                    if (key.equals(KEY_MWIPS)) {
                        c = 3;
                        break;
                    }
                    break;
                case 934677765:
                    if (key.equals(KEY_SAFEMODE)) {
                        c = 0;
                        break;
                    }
                    break;
            }
            if (c == 0 || c == 1 || c == 2 || c == 3) {
                int dbValue = getValueFromDB(key);
                Log.d(TAG, "key:" + key + ", currentValue:" + currentValue + ", defaultValue:" + defaultValue + ", dbValue:" + dbValue);
                if (dbValue == currentValue) {
                    putValueIntoDB(key, defaultValue);
                }
            }
        }
        if (!isFeatureControlDBempty()) {
            Log.d(TAG, "try to clear guider global db and feature map");
            Settings.Global.putString(this.mContext.getContentResolver(), "wifi_guider_feature_control", "");
            this.mFeatureMap.clear();
        }
        return true;
    }

    private boolean checkAndPutIntoMap(Bundle bundle) {
        Log.d(TAG, "checkAndPutIntoMap");
        boolean isChanged = false;
        int i = 0;
        while (true) {
            String[] strArr = mKeys;
            if (i >= strArr.length) {
                return isChanged;
            }
            String key = strArr[i];
            if (bundle.containsKey(key)) {
                int inputValue = bundle.getInt(key);
                Log.d(TAG, "key:" + key + ", inputValue:" + inputValue);
                if (this.mFeatureMap.containsKey(key)) {
                    Log.d(TAG, key + " is exists, value:" + this.mFeatureMap.get(key));
                }
                if (!this.mFeatureMap.containsKey(key) || this.mFeatureMap.get(key).intValue() != inputValue) {
                    Log.d(TAG, "try to put " + key + " to " + inputValue + " into Map");
                    this.mFeatureMap.put(key, Integer.valueOf(inputValue));
                    isChanged = true;
                }
            }
            i++;
        }
    }

    private boolean checkAndPutIntoDB(Bundle bundle) {
        int currentValue;
        Log.d(TAG, "checkAndPutIntoDB");
        for (String key : bundle.keySet()) {
            Log.d(TAG, "key:" + key);
            char c = 65535;
            switch (key.hashCode()) {
                case -200000289:
                    if (key.equals(KEY_POWERSAVE)) {
                        c = 0;
                        break;
                    }
                    break;
                case 667407668:
                    if (key.equals(KEY_HOTSPOT20)) {
                        c = 2;
                        break;
                    }
                    break;
                case 797931060:
                    if (key.equals(KEY_MWIPS)) {
                        c = 3;
                        break;
                    }
                    break;
                case 934677765:
                    if (key.equals(KEY_SAFEMODE)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            if (c == 0 || c == 1 || c == 2 || c == 3) {
                int inputValue = bundle.getInt(key);
                int defaultValue = this.mFeatureDefaultMap.get(key).intValue();
                int dbValue = getValueFromDB(key);
                if (this.mFeatureMap.containsKey(key)) {
                    Log.d(TAG, key + " is exists in Map");
                    currentValue = this.mFeatureMap.get(key).intValue();
                } else {
                    Log.d(TAG, "there is no " + key + " in Map");
                    currentValue = dbValue;
                }
                Log.d(TAG, "inputValue:" + inputValue + ", defaultValue:" + defaultValue + ", currentValue:" + currentValue + ", dbValue:" + dbValue);
                if (dbValue == currentValue && dbValue != inputValue) {
                    Log.d(TAG, "try to put " + key + ":" + inputValue + " into DB");
                    putValueIntoDB(key, inputValue);
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getValueFromDB(java.lang.String r7) {
        /*
            r6 = this;
            java.util.HashMap<java.lang.String, java.lang.Integer> r0 = r6.mFeatureDefaultMap
            java.lang.Object r0 = r0.get(r7)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r0 = r0.intValue()
            int r1 = r7.hashCode()
            r2 = 3
            r3 = 2
            r4 = 1
            r5 = -1
            switch(r1) {
                case -200000289: goto L_0x0036;
                case 667407668: goto L_0x002c;
                case 797931060: goto L_0x0022;
                case 934677765: goto L_0x0018;
                default: goto L_0x0017;
            }
        L_0x0017:
            goto L_0x0040
        L_0x0018:
            java.lang.String r1 = "wifiSafeMode"
            boolean r1 = r7.equals(r1)
            if (r1 == 0) goto L_0x0017
            r1 = r4
            goto L_0x0041
        L_0x0022:
            java.lang.String r1 = "wifiMWipsEnabled"
            boolean r1 = r7.equals(r1)
            if (r1 == 0) goto L_0x0017
            r1 = r2
            goto L_0x0041
        L_0x002c:
            java.lang.String r1 = "hotspot20Enabled"
            boolean r1 = r7.equals(r1)
            if (r1 == 0) goto L_0x0017
            r1 = r3
            goto L_0x0041
        L_0x0036:
            java.lang.String r1 = "powersaveEnabled"
            boolean r1 = r7.equals(r1)
            if (r1 == 0) goto L_0x0017
            r1 = 0
            goto L_0x0041
        L_0x0040:
            r1 = r5
        L_0x0041:
            if (r1 == 0) goto L_0x0071
            if (r1 == r4) goto L_0x0064
            if (r1 == r3) goto L_0x0057
            if (r1 == r2) goto L_0x004a
            return r5
        L_0x004a:
            android.content.Context r1 = r6.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r2 = "wifi_mwips"
            int r1 = android.provider.Settings.Secure.getInt(r1, r2, r0)
            return r1
        L_0x0057:
            android.content.Context r1 = r6.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r2 = "wifi_hotspot20_enable"
            int r1 = android.provider.Settings.Secure.getInt(r1, r2, r0)
            return r1
        L_0x0064:
            android.content.Context r1 = r6.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r2 = "safe_wifi"
            int r1 = android.provider.Settings.Global.getInt(r1, r2, r0)
            return r1
        L_0x0071:
            android.content.Context r1 = r6.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r2 = "wifi_adps_enable"
            int r1 = android.provider.Settings.Secure.getInt(r1, r2, r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiGuiderFeatureController.getValueFromDB(java.lang.String):int");
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void putValueIntoDB(java.lang.String r5, int r6) {
        /*
            r4 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "putValueIntoDB "
            r0.append(r1)
            r0.append(r5)
            java.lang.String r1 = ":"
            r0.append(r1)
            r0.append(r6)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "WifiGuiderFeatureController"
            android.util.Log.d(r1, r0)
            int r0 = r5.hashCode()
            r1 = 3
            r2 = 2
            r3 = 1
            switch(r0) {
                case -200000289: goto L_0x0047;
                case 667407668: goto L_0x003d;
                case 797931060: goto L_0x0033;
                case 934677765: goto L_0x0029;
                default: goto L_0x0028;
            }
        L_0x0028:
            goto L_0x0051
        L_0x0029:
            java.lang.String r0 = "wifiSafeMode"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0028
            r0 = r3
            goto L_0x0052
        L_0x0033:
            java.lang.String r0 = "wifiMWipsEnabled"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0028
            r0 = r1
            goto L_0x0052
        L_0x003d:
            java.lang.String r0 = "hotspot20Enabled"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0028
            r0 = r2
            goto L_0x0052
        L_0x0047:
            java.lang.String r0 = "powersaveEnabled"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x0028
            r0 = 0
            goto L_0x0052
        L_0x0051:
            r0 = -1
        L_0x0052:
            if (r0 == 0) goto L_0x007f
            if (r0 == r3) goto L_0x0073
            if (r0 == r2) goto L_0x0067
            if (r0 == r1) goto L_0x005b
            goto L_0x008b
        L_0x005b:
            android.content.Context r0 = r4.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r1 = "wifi_mwips"
            android.provider.Settings.Secure.putInt(r0, r1, r6)
            goto L_0x008b
        L_0x0067:
            android.content.Context r0 = r4.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r1 = "wifi_hotspot20_enable"
            android.provider.Settings.Secure.putInt(r0, r1, r6)
            goto L_0x008b
        L_0x0073:
            android.content.Context r0 = r4.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r1 = "safe_wifi"
            android.provider.Settings.Global.putInt(r0, r1, r6)
            goto L_0x008b
        L_0x007f:
            android.content.Context r0 = r4.mContext
            android.content.ContentResolver r0 = r0.getContentResolver()
            java.lang.String r1 = "wifi_adps_enable"
            android.provider.Settings.Secure.putInt(r0, r1, r6)
        L_0x008b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiGuiderFeatureController.putValueIntoDB(java.lang.String, int):void");
    }

    public void updateConditions(Bundle bundle) {
        Log.d(TAG, "updateConditions");
        boolean isChanged = false;
        if (bundle == null) {
            Log.d(TAG, "bundle is null");
        } else if (!checkAndReset(bundle)) {
            if (checkAndPutIntoDB(bundle)) {
                isChanged = true;
            }
            if (checkAndPutIntoMap(bundle)) {
                isChanged = true;
            }
            if (isChanged) {
                putFeatureIntoDB();
            }
        }
    }

    private int getPasspointDefaultValue() {
        return isPasspointDefaultOff() ? 0 : 1;
    }

    private boolean isPasspointDefaultOff() {
        String cscFeature = OpBrandingLoader.getInstance().getMenuStatusForPasspoint();
        if (cscFeature == null || !cscFeature.contains("DEFAULT_OFF")) {
            return false;
        }
        return true;
    }

    private void putFeatureIntoDB() {
        Log.d(TAG, "putFeatureIntoDB:" + this.mFeatureMap.toString());
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_guider_feature_control", this.mFeatureMap.toString());
    }

    public int getRssi24Threshold() {
        return (this.mFeatureMap.containsKey(KEY_RSSI24) ? this.mFeatureMap : this.mFeatureDefaultMap).get(KEY_RSSI24).intValue();
    }

    public int getRssi5Threshold() {
        return (this.mFeatureMap.containsKey(KEY_RSSI5) ? this.mFeatureMap : this.mFeatureDefaultMap).get(KEY_RSSI5).intValue();
    }

    public int getAutoWifiCellCount() {
        return (this.mFeatureMap.containsKey(KEY_CELL_COUNT) ? this.mFeatureMap : this.mFeatureDefaultMap).get(KEY_CELL_COUNT).intValue();
    }

    public int getAutoWifiBssidCount() {
        return (this.mFeatureMap.containsKey(KEY_BSSID_COUNT) ? this.mFeatureMap : this.mFeatureDefaultMap).get(KEY_BSSID_COUNT).intValue();
    }

    public int getAutoWifiTurnOffDurationSeconds() {
        Integer num;
        if (this.mFeatureMap.containsKey(KEY_AUTO_WIFI_TURN_OFF_SECONDS)) {
            num = this.mFeatureMap.get(KEY_AUTO_WIFI_TURN_OFF_SECONDS);
        } else {
            num = this.mFeatureDefaultMap.get(KEY_AUTO_WIFI_TURN_OFF_SECONDS);
        }
        return num.intValue();
    }

    public int getAutoWifiTurnOffScanCount() {
        Integer num;
        if (this.mFeatureMap.containsKey(KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT)) {
            num = this.mFeatureMap.get(KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT);
        } else {
            num = this.mFeatureDefaultMap.get(KEY_AUTO_WIFI_TURN_OFF_SCAN_COUNT);
        }
        return num.intValue();
    }

    public boolean isMWipsEnabled() {
        if (this.mFeatureMap.containsKey(KEY_MWIPS)) {
            if (this.mFeatureMap.get(KEY_MWIPS).intValue() == 1) {
                return true;
            }
            return false;
        } else if (this.mFeatureDefaultMap.get(KEY_MWIPS).intValue() == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSupportQosProvider() {
        if (this.mFeatureMap.containsKey(KEY_QOS_PROVIDER)) {
            if (this.mFeatureMap.get(KEY_QOS_PROVIDER).intValue() == 1) {
                return true;
            }
            return false;
        } else if (this.mFeatureDefaultMap.get(KEY_QOS_PROVIDER).intValue() == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSupportSamsungNetworkScore() {
        if (this.mFeatureMap.containsKey(KEY_SCORE_PROVIDER)) {
            if (this.mFeatureMap.get(KEY_SCORE_PROVIDER).intValue() == 1) {
                return true;
            }
            return false;
        } else if (this.mFeatureDefaultMap.get(KEY_SCORE_PROVIDER).intValue() == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSupportWifiProfileShare() {
        if (this.mFeatureMap.containsKey(KEY_PROFILE_SHARE)) {
            if (this.mFeatureMap.get(KEY_PROFILE_SHARE).intValue() == 1) {
                return true;
            }
            return false;
        } else if (this.mFeatureDefaultMap.get(KEY_PROFILE_SHARE).intValue() == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSupportWifiProfileRequest() {
        if (this.mFeatureMap.containsKey(KEY_PROFILE_REQUEST)) {
            if (this.mFeatureMap.get(KEY_PROFILE_REQUEST).intValue() == 1) {
                return true;
            }
            return false;
        } else if (this.mFeatureDefaultMap.get(KEY_PROFILE_REQUEST).intValue() == 1) {
            return true;
        } else {
            return false;
        }
    }
}
