package com.samsung.android.server.wifi.bigdata;

import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SemHqmManager;
import android.util.Log;
import com.samsung.android.feature.SemFloatingFeature;
import java.util.HashMap;
import java.util.List;

public class WifiBigDataLogManager {
    private static final String ARGS_APP_ID_STR = "app_id";
    private static final String ARGS_BIGDATA_FLAG_STR = "bigdata";
    private static final String ARGS_DATA_STR = "data";
    private static final String ARGS_EXTRA_STR = "extra";
    private static final String ARGS_FEATURE_NAME = "feature";
    private static final String ARGS_VALUE_STR = "value";
    private static final int CMD_INSERT_LOG_FOR_ONOF = 1;
    private static boolean DBG = Debug.semIsProductDev();
    public static final boolean ENABLE_SURVEY_MODE = "TRUE".equals(SemFloatingFeature.getInstance().getString("SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE"));
    public static final boolean ENABLE_UNIFIED_HQM_SERVER = true;
    public static final String FEATURE_24HR = "W24H";
    public static final String FEATURE_ASSOC = "ASSO";
    public static final String FEATURE_BROADCOM_COUNTER_INFO = "CNTS";
    public static final String FEATURE_BROADCOM_COUNTER_INFO2 = "ECNT";
    public static final String FEATURE_CRASH = "CRSH";
    public static final String FEATURE_DISC = "DISC";
    public static final String FEATURE_EAP_INFO = "EAPT";
    public static final String FEATURE_GO_TO_WEBPAGE = "GOWP";
    public static final String FEATURE_HANG = "HANG";
    public static final String FEATURE_ISSUE_DETECTOR_DISC1 = "PDC1";
    public static final String FEATURE_ISSUE_DETECTOR_DISC2 = "PDC2";
    public static final String FEATURE_MHS_DISCONNECTION = "MHDC";
    public static final String FEATURE_MHS_INFO = "MHSI";
    public static final String FEATURE_MHS_ON_OF = "MHOF";
    public static final String FEATURE_MHS_POWERSAVEMODE = "MHPS";
    public static final String FEATURE_MHS_POWERSAVEMODE_TIME = "MHPT";
    public static final String FEATURE_MHS_SETTING = "MHSS";
    public static final String FEATURE_ON_OFF = "ONOF";
    public static final String FEATURE_SCAN = "SCAN";
    public static final String FEATURE_SI5G = "SI5G";
    public static final int LOGGING_TYPE_ADPS_STATE = 13;
    public static final int LOGGING_TYPE_BLUETOOTH_CONNECTION = 10;
    public static final int LOGGING_TYPE_CONFIG_NETWORK_TYPE = 11;
    public static final int LOGGING_TYPE_LOCAL_DISCONNECT_REASON = 8;
    public static final int LOGGING_TYPE_ROAM_TRIGGER = 7;
    public static final int LOGGING_TYPE_SET_CONNECTION_START_TIME = 12;
    public static final int LOGGING_TYPE_UPDATE_DATA_RATE = 9;
    private static final String PACKAGE_NAME_SURVEY = "com.samsung.android.providers.context";
    private static final String PROVIDER_NAME_SURVEY = "com.samsung.android.providers.context.log.action.USE_APP_FEATURE_SURVEY";
    private static final String TAG = "WifiBigDataLogManager";
    public final String APP_ID = "android.net.wifi";
    private final WifiBigDataLogAdapter mAdapter;
    private final HashMap<String, BaseBigDataItem> mBigDataItems = new HashMap<>();
    private final Context mContext;
    private final MainHandler mHandler;
    private int mLastUpdatedInternalReason = 0;
    /* access modifiers changed from: private */
    public boolean mLogMessages = DBG;
    private SemHqmManager mSemHqmManager;

    public interface WifiBigDataLogAdapter {
        String getChipsetOuis();

        List<WifiConfiguration> getSavedNetworks();
    }

    public WifiBigDataLogManager(Context context, Looper workerLooper, WifiBigDataLogAdapter adapter) {
        this.mHandler = new MainHandler(workerLooper);
        this.mContext = context;
        this.mAdapter = adapter;
        initialize();
    }

    private void initialize() {
        this.mBigDataItems.clear();
        this.mBigDataItems.put(FEATURE_DISC, new BigDataItemDISC(FEATURE_DISC));
        this.mBigDataItems.put(FEATURE_HANG, new BigDataItemHANG(FEATURE_HANG));
        this.mBigDataItems.put("MHSI", new BigDataItemMHSI("MHSI"));
        this.mBigDataItems.put(FEATURE_MHS_POWERSAVEMODE, new BigDataItemMHPS(FEATURE_MHS_POWERSAVEMODE));
        this.mBigDataItems.put(FEATURE_MHS_POWERSAVEMODE_TIME, new BigDataItemMHPT(FEATURE_MHS_POWERSAVEMODE_TIME));
        this.mBigDataItems.put("MHDC", new BigDataItemMHDC("MHDC"));
        this.mBigDataItems.put(FEATURE_MHS_ON_OF, new BigDataItemMHOF(FEATURE_MHS_ON_OF));
        this.mBigDataItems.put(FEATURE_MHS_SETTING, new BigDataItemMHSS(FEATURE_MHS_SETTING));
        this.mBigDataItems.put(FEATURE_ON_OFF, new BigDataItemONOF(FEATURE_ON_OFF));
        this.mBigDataItems.put(FEATURE_24HR, new BigDataItemW24H(FEATURE_24HR));
        this.mBigDataItems.put(FEATURE_BROADCOM_COUNTER_INFO2, new BigDataItemECNT(FEATURE_BROADCOM_COUNTER_INFO2));
        this.mBigDataItems.put(FEATURE_ISSUE_DETECTOR_DISC1, new BigDataItemPDC1(FEATURE_ISSUE_DETECTOR_DISC1));
        this.mBigDataItems.put(FEATURE_ISSUE_DETECTOR_DISC2, new BigDataItemPDC2(FEATURE_ISSUE_DETECTOR_DISC2));
    }

    private String getJsonFormat(BaseBigDataItem item, int type) {
        if (item == null) {
            return null;
        }
        if (!item.isAvailableLogging(type)) {
            if (DBG) {
                Log.i(TAG, "not supported logging feature:" + item.getFeatureName() + " for type:" + type);
            }
            return null;
        }
        if (DBG) {
            Log.i(TAG, "getJsonFormat - feature : " + item.getFeatureName() + ", type : " + type);
        }
        return item.getJsonFormatFor(type);
    }

    public void setLogVisible(boolean visible) {
        this.mLogMessages = visible;
        for (BaseBigDataItem item : this.mBigDataItems.values()) {
            item.setLogVisible(visible);
        }
    }

    public void clearData(String feature) {
        BaseBigDataItem item = getBigDataItem(feature);
        if (item != null) {
            item.clearData();
        }
    }

    private synchronized BaseBigDataItem getBigDataItem(String feature) {
        return this.mBigDataItems.get(feature);
    }

    public static Bundle getBigDataBundle(String feature, String data) {
        Bundle args = new Bundle();
        args.putBoolean(ARGS_BIGDATA_FLAG_STR, true);
        args.putString(ARGS_FEATURE_NAME, feature);
        args.putString(ARGS_DATA_STR, data);
        return args;
    }

    public static Bundle getGSIMBundle(String feature, String extra, int value) {
        Bundle args = getGSIMBundle(feature, extra);
        args.putLong(ARGS_VALUE_STR, (long) value);
        return args;
    }

    public static Bundle getGSIMBundle(String feature, String extra) {
        Bundle args = new Bundle();
        args.putString(ARGS_FEATURE_NAME, feature);
        args.putString(ARGS_EXTRA_STR, extra);
        return args;
    }

    /* access modifiers changed from: private */
    public void updateTime(String feature) {
        BaseBigDataItem item = getBigDataItem(feature);
        if (item != null) {
            item.updateTime();
        }
    }

    private void addOrUpdateValueInternal(String feature, String key, String value) {
        BaseBigDataItem item = getBigDataItem(feature);
        if (item != null) {
            item.addOrUpdateValue(key, value);
        }
    }

    private void addOrUpdateValueInternal(String feature, String key, int value) {
        BaseBigDataItem item = getBigDataItem(feature);
        if (item != null) {
            item.addOrUpdateValue(key, value);
        }
    }

    public int getAndResetLastInternalReason() {
        int ret = this.mLastUpdatedInternalReason;
        this.mLastUpdatedInternalReason = 0;
        return ret;
    }

    public boolean addOrUpdateValue(int loggingType, int value) {
        switch (loggingType) {
            case 7:
                addOrUpdateValueInternal(FEATURE_DISC, "cn_rom", value);
                return true;
            case 8:
                addOrUpdateValueInternal(FEATURE_DISC, "cn_irs", value);
                this.mLastUpdatedInternalReason = value;
                return true;
            case 9:
                addOrUpdateValueInternal(FEATURE_DISC, "max_drt", value);
                return true;
            case 10:
                addOrUpdateValueInternal(FEATURE_DISC, "bt_cnt", value);
                return true;
            case 11:
                addOrUpdateValueInternal(FEATURE_DISC, "apwe", value);
                return true;
            case 12:
                updateTime(FEATURE_DISC);
                return false;
            case 13:
                addOrUpdateValueInternal(FEATURE_DISC, "adps", value);
                return true;
            default:
                return false;
        }
    }

    public boolean addOrUpdateValue(int loggingType, String value) {
        return false;
    }

    public void insertLog(Bundle args) {
        boolean isBigDataLog = args.getBoolean(ARGS_BIGDATA_FLAG_STR, false);
        String feature = args.getString(ARGS_FEATURE_NAME, (String) null);
        if (this.mLogMessages) {
            Log.i(TAG, "insertLog bigData:" + isBigDataLog + " feature:" + feature);
        }
        if (isBigDataLog) {
            processBigDataLog(feature, args.getString(ARGS_DATA_STR, (String) null));
            clearData(feature);
        }
    }

    private void checkAndGetHqmManager() {
        if (this.mSemHqmManager == null) {
            this.mSemHqmManager = (SemHqmManager) this.mContext.getSystemService("HqmManagerService");
        }
    }

    private void sendHWParamToHQM(String feature, String devCustomDataSet) {
        String str = feature;
        if (this.mSemHqmManager == null) {
            String str2 = devCustomDataSet;
        } else if (str == null) {
            String str3 = devCustomDataSet;
        } else {
            if (DBG) {
                Log.d(TAG, "send H/W Parameters to HQM - feature : " + str + ", logmaps : " + devCustomDataSet);
            } else {
                String str4 = devCustomDataSet;
            }
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            String compManufacture = WifiChipInfo.getChipsetName();
            this.mSemHqmManager.sendHWParamToHQM(0, BaseBigDataItem.COMPONENT_ID, feature, BaseBigDataItem.HIT_TYPE_IMMEDIATLY, wifiChipInfo.getCidInfo(), compManufacture, devCustomDataSet, "", "");
        }
    }

    private void sendHWParamToHQMwithAppId(String feature, String hitType, String basicCustomDataSet, String devCustomDataSet) {
        String str = feature;
        String str2 = basicCustomDataSet;
        String devCustomDataSet2 = devCustomDataSet;
        if (this.mSemHqmManager != null && str != null) {
            if (DBG) {
                Log.d(TAG, "send H/W Parameters to HQM with appid - feature : " + str + ", logmaps: " + str2 + " private: " + devCustomDataSet2);
                StringBuilder sb = new StringBuilder();
                sb.append("basic data size : ");
                Object obj = "0";
                sb.append(str2 == null ? obj : Integer.valueOf(basicCustomDataSet.length()));
                sb.append(", custom data size : ");
                if (devCustomDataSet2 != null) {
                    obj = Integer.valueOf(devCustomDataSet.length());
                }
                sb.append(obj);
                Log.d(TAG, sb.toString());
            }
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            this.mSemHqmManager.sendHWParamToHQMwithAppId(0, BaseBigDataItem.COMPONENT_ID, feature, hitType, wifiChipInfo.getCidInfo(), WifiChipInfo.getChipsetName(), devCustomDataSet2 == null ? "" : devCustomDataSet2, basicCustomDataSet, "", "android.net.wifi");
        }
    }

    private void processBigDataLog(String feature, String data) {
        if (DBG) {
            Log.d(TAG, "insertLog - feature : " + feature + ", data : " + data);
        }
        if (feature != null && data != null) {
            int msgId = -1;
            long delayTimeMillis = 0;
            if (FEATURE_ON_OFF.equals(feature)) {
                msgId = 1;
                delayTimeMillis = 30000;
            } else if (FEATURE_DISC.equals(feature)) {
                data = data + " " + this.mAdapter.getChipsetOuis();
            }
            if (msgId == -1) {
                parseAndSendData(feature, data);
                return;
            }
            Bundle obj = new Bundle();
            obj.putString(ARGS_FEATURE_NAME, feature);
            obj.putString(ARGS_DATA_STR, data);
            MainHandler mainHandler = this.mHandler;
            mainHandler.sendMessageDelayed(mainHandler.obtainMessage(msgId, obj), delayTimeMillis);
        }
    }

    /* access modifiers changed from: private */
    public void parseAndSendData(String feature, String data) {
        if (feature != null && data != null) {
            BaseBigDataItem item = getBigDataItem(feature);
            if (item == null) {
                if (DBG) {
                    Log.d(TAG, "feature " + feature + " is disabled");
                }
            } else if (item.parseData(data)) {
                boolean isSentLogs = false;
                checkAndGetHqmManager();
                String extra = getJsonFormat(item, 2);
                if (extra != null) {
                    sendHWParamToHQMwithAppId(feature, item.getHitType(), extra, getJsonFormat(item, 3));
                    isSentLogs = true;
                }
                String privateExtra = this.mSemHqmManager;
                String extra2 = getJsonFormat(item, 0);
                if (extra2 != null) {
                    sendHWParamToHQMwithAppId(feature, BaseBigDataItem.HIT_TYPE_ONCE_A_DAY, extra2, (String) null);
                    isSentLogs = true;
                }
                if (!isSentLogs) {
                    Log.e(TAG, "parse error - extra is null");
                }
            } else {
                Log.e(TAG, "parse error - can't parse feature:" + feature + " data:" + data);
            }
        }
    }

    /* access modifiers changed from: private */
    public String getForegroundPackageName() {
        List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
        if (!tasks.isEmpty()) {
            return new String(tasks.get(0).topActivity.getPackageName());
        }
        return null;
    }

    /* access modifiers changed from: private */
    public String getConfiguredNetworksSize() {
        int allNetworkCount = 0;
        int openNetworkCount = 0;
        int favoriteApCount = 0;
        List<WifiConfiguration> configs = this.mAdapter.getSavedNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (config.allowedKeyManagement.get(0)) {
                    openNetworkCount++;
                }
                if (config.semAutoWifiScore > 4) {
                    favoriteApCount++;
                }
                allNetworkCount++;
            }
        }
        return String.valueOf(allNetworkCount) + " " + String.valueOf(openNetworkCount) + " " + String.valueOf(favoriteApCount);
    }

    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:3:0x0005, code lost:
            r0 = (android.os.Bundle) r8.obj;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r8) {
            /*
                r7 = this;
                java.lang.Object r0 = r8.obj
                if (r0 != 0) goto L_0x0005
                return
            L_0x0005:
                java.lang.Object r0 = r8.obj
                android.os.Bundle r0 = (android.os.Bundle) r0
                r1 = 0
                java.lang.String r2 = "feature"
                java.lang.String r2 = r0.getString(r2, r1)
                if (r2 != 0) goto L_0x0013
                return
            L_0x0013:
                java.lang.String r3 = "data"
                java.lang.String r1 = r0.getString(r3, r1)
                if (r1 != 0) goto L_0x001c
                return
            L_0x001c:
                com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r3 = com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.this
                boolean r3 = r3.mLogMessages
                if (r3 == 0) goto L_0x0044
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "handleMessage what="
                r3.append(r4)
                int r4 = r8.what
                r3.append(r4)
                java.lang.String r4 = " feature:"
                r3.append(r4)
                r3.append(r2)
                java.lang.String r3 = r3.toString()
                java.lang.String r4 = "WifiBigDataLogManager"
                android.util.Log.d(r4, r3)
            L_0x0044:
                int r3 = r8.what
                r4 = 1
                if (r3 == r4) goto L_0x004a
                goto L_0x0081
            L_0x004a:
                com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r3 = com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.this
                java.lang.String r3 = r3.getForegroundPackageName()
                if (r3 != 0) goto L_0x0054
                java.lang.String r3 = "x"
            L_0x0054:
                java.lang.StringBuffer r4 = new java.lang.StringBuffer
                r4.<init>()
                r4.append(r1)
                java.lang.String r5 = " "
                r4.append(r5)
                com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r6 = com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.this
                java.lang.String r6 = r6.getConfiguredNetworksSize()
                r4.append(r6)
                r4.append(r5)
                r4.append(r3)
                com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r5 = com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.this
                java.lang.String r6 = r4.toString()
                r5.parseAndSendData(r2, r6)
                com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager r5 = com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.this
                java.lang.String r6 = "ONOF"
                r5.updateTime(r6)
            L_0x0081:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager.MainHandler.handleMessage(android.os.Message):void");
        }
    }
}
