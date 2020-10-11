package com.samsung.android.server.wifi.sns;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.SemHqmManager;
import android.os.UserHandle;
import android.util.Log;
import com.samsung.android.feature.SemFloatingFeature;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import java.util.HashMap;

public class SnsBigDataManager {
    private static final String ACTION_USE_APP_FEATURE_SURVEY = "com.samsung.android.providers.context.log.action.USE_APP_FEATURE_SURVEY";
    private static final String ACTION_USE_MULTI_APP_FEATURE_SURVEY = "com.samsung.android.providers.context.log.action.USE_MULTI_APP_FEATURE_SURVEY";
    private static final String ARGS_APP_ID_STR = "app_id";
    private static final String ARGS_DATA_STR = "data";
    private static final String ARGS_EXTRA_STR = "extra";
    private static final String ARGS_FEATURE_NAME = "feature";
    private static final String ARGS_VALUE_STR = "value";
    private static boolean DBG = Debug.semIsProductDev();
    public static final boolean ENABLE_SURVEY_MODE = "TRUE".equals(SemFloatingFeature.getInstance().getString("SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE"));
    public static final boolean ENABLE_UNIFIED_HQM_SERVER = true;
    public static final String FEATURE_SSIV = "SSIV";
    public static final String FEATURE_SSMA = "SSMA";
    public static final String FEATURE_SSVI = "SSVI";
    public static final String FEATURE_TCPE = "TCPE";
    public static final String FEATURE_WFCP = "WFCP";
    public static final String FEATURE_WFMH = "WFMH";
    public static final String FEATURE_WFQC = "WFQC";
    public static final String FEATURE_WFSN = "WFSN";
    public static final String FEATURE_WNIC = "WNIC";
    private static final String PACKAGE_NAME_SURVEY = "com.samsung.android.providers.context";
    private static boolean SMART_CM = false;
    private static final String TAG = "SnsBigDataManager";
    public final String APP_ID = "android.net.wifi";
    public final HashMap<String, SnsBigDataFeature> mBigDataFeatures;
    private final Context mContext;
    private SemHqmManager mSemHqmManager;

    public SnsBigDataManager(Context context) {
        this.mContext = context;
        this.mBigDataFeatures = new HashMap<>();
        this.mSemHqmManager = (SemHqmManager) this.mContext.getSystemService("HqmManagerService");
        initialize();
    }

    private void initialize() {
        this.mBigDataFeatures.clear();
        this.mBigDataFeatures.put(FEATURE_WFCP, new SnsBigDataWFCP());
        this.mBigDataFeatures.put("SSMA", new SnsBigDataSCNT());
        this.mBigDataFeatures.put(FEATURE_WFSN, new SnsBigDataWFSN(true, "WWSN"));
        this.mBigDataFeatures.put(FEATURE_WFQC, new SnsBigDataWFQC());
        this.mBigDataFeatures.put(FEATURE_SSVI, new SnsBigDataSSVI());
        this.mBigDataFeatures.put(FEATURE_SSIV, new SnsBigDataSSIV());
        this.mBigDataFeatures.put(FEATURE_WFMH, new SnsBigDataWFMH());
        this.mBigDataFeatures.put("TCPE", new SnsBigDataTCPE());
        this.mBigDataFeatures.put(FEATURE_WNIC, new SnsBigDataWNIC(true, "NIAC"));
    }

    public boolean addOrUpdateFeatureValue(String feature, String param, int value) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateValue(param, value);
        return true;
    }

    public boolean addOrUpdateFeatureValue(String feature, String param, long value) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateValue(param, value);
        return true;
    }

    public boolean addOrUpdateFeatureValue(String feature, String param, double value) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateValue(param, value);
        return true;
    }

    public boolean addOrUpdateFeatureValue(String feature, String param, String value) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateValue(param, value);
        return true;
    }

    public boolean addOrUpdateFeatureValue(String feature, HashMap<String, String> values) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateValues(values);
        return true;
    }

    public boolean addOrUpdateFeatureAllValue(String feature) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).addOrUpdateAllValue();
        return true;
    }

    public SnsBigDataFeature getBigDataFeature(String feature) {
        if (this.mBigDataFeatures.containsKey(feature)) {
            return this.mBigDataFeatures.get(feature);
        }
        return null;
    }

    public boolean clearFeature(String feature) {
        if (!this.mBigDataFeatures.containsKey(feature)) {
            return false;
        }
        this.mBigDataFeatures.get(feature).resetData();
        return true;
    }

    public boolean clearAllFeatures() {
        if (this.mBigDataFeatures.isEmpty()) {
            return false;
        }
        for (SnsBigDataFeature feature : this.mBigDataFeatures.values()) {
            feature.resetData();
        }
        return false;
    }

    public void setSmartCmLog(boolean enable) {
        SMART_CM = enable;
    }

    public void insertLog(String feature) {
        insertLog(feature, -1);
    }

    public void insertLog(String feature, long value) {
        if (this.mBigDataFeatures.containsKey(feature)) {
            String extra = this.mBigDataFeatures.get(feature).getJsonFormat();
            if (this.mBigDataFeatures.get(feature).getIsDqaEnabled()) {
                if (this.mBigDataFeatures.get(feature).getDqaFeatureName() != null) {
                    sendHWParamToHQMwithAppId(this.mBigDataFeatures.get(feature).getDqaFeatureName(), extra);
                } else {
                    sendHWParamToHQMwithAppId(feature, extra);
                }
            }
            sendBroadcastToContextFramework(feature, extra, value);
        }
    }

    public void insertLog(String feature, boolean isArrayMode) {
        String[] extras;
        if (!isArrayMode) {
            insertLog(feature);
        } else if (this.mBigDataFeatures.containsKey(feature) && (extras = this.mBigDataFeatures.get(feature).getJsonFormatArray()) != null) {
            String[] features = new String[extras.length];
            long[] values = new long[extras.length];
            for (int i = 0; i < features.length; i++) {
                features[i] = feature;
                values[i] = -1;
            }
            sendBroadcastToContextFramework(features, extras, values);
        }
    }

    public void insertLog(String feature, String[] extras, boolean isArrayMode) {
        if (!ENABLE_SURVEY_MODE) {
            if (DBG) {
                Log.d(TAG, "survey mode is not enabled");
            }
        } else if (feature == null) {
            if (DBG) {
                Log.d(TAG, "feature is not enabled");
            }
        } else if (extras != null) {
            String[] features = new String[extras.length];
            long[] values = new long[extras.length];
            if (!isArrayMode) {
                insertLog(feature);
            } else if (this.mBigDataFeatures.containsKey(feature)) {
                for (int i = 0; i < features.length; i++) {
                    features[i] = feature;
                    values[i] = -1;
                }
                sendBroadcastToContextFramework(features, extras, values);
            }
        }
    }

    private void sendBroadcastToContextFramework(String feature, String extra, long value) {
        if (DBG) {
            Log.d(TAG, "sendBroadcastToContextFramework - feature : " + feature + ", extra : " + extra + ", value : " + value);
        }
        try {
            if (!ENABLE_SURVEY_MODE) {
                if (DBG) {
                    Log.d(TAG, "survey mode is not enabled");
                }
            } else if (feature != null) {
                ContentValues cv = new ContentValues();
                cv.put(ARGS_APP_ID_STR, "android.net.wifi");
                cv.put(ARGS_FEATURE_NAME, feature);
                if (extra != null && extra.length() > 0) {
                    if (extra.charAt(0) == '{') {
                        cv.put(ARGS_EXTRA_STR, extra);
                    } else {
                        cv.put(ARGS_EXTRA_STR, "{" + extra + "}");
                    }
                }
                if (value != -1) {
                    cv.put(ARGS_VALUE_STR, Long.valueOf(value));
                }
                Intent broadcastIntent = new Intent(ACTION_USE_APP_FEATURE_SURVEY);
                broadcastIntent.putExtra(ARGS_DATA_STR, cv);
                broadcastIntent.setPackage(PACKAGE_NAME_SURVEY);
                this.mContext.sendBroadcastAsUser(broadcastIntent, UserHandle.ALL);
            } else if (DBG) {
                Log.d(TAG, "feature is not enabled");
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception occured on sendBroadcastToContextFramework:" + e);
            e.printStackTrace();
        }
    }

    private void sendBroadcastToContextFramework(String[] features, String[] extras, long[] values) {
        try {
            if (DBG) {
                Log.d(TAG, "sendBroadcastToContextFramework - features : " + features.length + ", extras : " + extras.length + ", values : " + values.length);
            }
            if (!ENABLE_SURVEY_MODE) {
                if (DBG) {
                    Log.d(TAG, "survey mode is not enabled");
                }
            } else if (features != null) {
                ContentValues[] cv = new ContentValues[extras.length];
                for (int i = 0; i < features.length; i++) {
                    cv[i] = new ContentValues();
                    cv[i].put(ARGS_APP_ID_STR, "android.net.wifi");
                    cv[i].put(ARGS_FEATURE_NAME, features[i]);
                    if (extras[i] != null && extras[i].length() > 0) {
                        if (extras[i].charAt(0) == '{') {
                            cv[i].put(ARGS_EXTRA_STR, extras[i]);
                        } else {
                            cv[i].put(ARGS_EXTRA_STR, "{" + extras[i] + "}");
                        }
                    }
                    if (values[i] != -1) {
                        cv[i].put(ARGS_VALUE_STR, Long.valueOf(values[i]));
                    }
                }
                Intent broadcastIntent = new Intent(ACTION_USE_MULTI_APP_FEATURE_SURVEY);
                broadcastIntent.putExtra(ARGS_DATA_STR, cv);
                broadcastIntent.setPackage(PACKAGE_NAME_SURVEY);
                this.mContext.sendBroadcast(broadcastIntent);
            } else if (DBG) {
                Log.d(TAG, "feature is not enabled");
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception occured on sendBroadcastToContextFramework:" + e);
            e.printStackTrace();
        }
    }

    private void sendHWParamToHQMwithAppId(String feature, String basicCustomDataSet) {
        String str = feature;
        if (this.mSemHqmManager == null) {
            String str2 = basicCustomDataSet;
        } else if (str == null) {
            String str3 = basicCustomDataSet;
        } else {
            if (DBG) {
                Log.d(TAG, "send H/W Parameters to HQM with appid - feature : " + str + ", logmaps : " + basicCustomDataSet);
            } else {
                String str4 = basicCustomDataSet;
            }
            WifiChipInfo wifiChipInfo = WifiChipInfo.getInstance();
            String compManufacture = WifiChipInfo.getChipsetName();
            this.mSemHqmManager.sendHWParamToHQMwithAppId(0, BaseBigDataItem.COMPONENT_ID, feature, BaseBigDataItem.HIT_TYPE_ONCE_A_DAY, wifiChipInfo.getCidInfo(), compManufacture, "", basicCustomDataSet, "", "android.net.wifi");
        }
    }
}
