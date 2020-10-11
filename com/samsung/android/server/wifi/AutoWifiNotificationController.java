package com.samsung.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Debug;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.samsung.android.server.wifi.AutoWifiController;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AutoWifiNotificationController {
    public static final String ACTION_AUTOWIFI_NOTIFICATION_CANCEL = "com.samsung.android.server.wifi.AutoWifiNotificationController.CANCEL_NOTI";
    public static final String ACTION_AUTOWIFI_NOTIFICATION_SETTINGS = "com.samsung.android.server.wifi.AutoWifiNotificationController.SETTINGS";
    private static final boolean DBG = Debug.semIsProductDev();
    private static final long DURATION_FOR_MINUS_SCORE = 600000;
    private static final long DURATION_FOR_POINT_1 = 600000;
    private static final long DURATION_FOR_POINT_2 = 14400000;
    private static final long DURATION_FOR_RECOVERY_SCORE = 3600000;
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final int IS_WIFI_OFF_NO = 0;
    private static final int IS_WIFI_OFF_YES = 1;
    private static final int IS_WIFI_OFF_YES_BY_USER = 2;
    private static final String JSON_KEY_ADD = "added";
    private static final String JSON_KEY_CONFIG = "config";
    private static final String JSON_KEY_REMOVE = "removed";
    private static final String PREF_KEY_AUTO_WIFI = "auto_wifi";
    public static final int SCORE_FOR_KT_HOME_AP = 101;
    private static final int SCORE_FOR_NOTIFICATION = 4;
    public static final int SCORE_FOR_NOTIFIED_AP = 100;
    private static final String TAG = "AutoWifiNotiController";
    private boolean isApWithNumerousCellId = false;
    final AutoWifiController.AutoWifiAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;
    private long mConnectedTime;
    final Context mContext;
    /* access modifiers changed from: private */
    public String mCurrentAddedNotiSsid;
    private String mCurrentAddedNotiTag;
    /* access modifiers changed from: private */
    public String mCurrentDeletedNotiSsid;
    private String mCurrentDeletedNotiTag;
    private HashMap<String, Integer> mDecreasedScores = new HashMap<>();
    private int mDeletedNotiId = 0;
    private long mDurationForMinusPoint = WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS;
    private long mDurationForPoint1 = WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS;
    private long mDurationForPoint2 = DURATION_FOR_POINT_2;
    private long mDurationForRecoveryPoint = 3600000;
    private boolean mEnableToUse = false;
    private List<String> mExceptNetworkKeys = null;
    private HashMap<String, Integer> mIncreasedScores = new HashMap<>();
    private IntentFilter mIntentFilterAttFactoryReset;
    private IntentFilter mIntentFilterCancel;
    private IntentFilter mIntentFilterLocaleChanged;
    private IntentFilter mIntentFilterSettings;
    private WifiConfiguration mLastNetworkConfig;
    private String mLastNetworkKey;
    /* access modifiers changed from: private */
    public int mNotiIcon = 0;

    public AutoWifiNotificationController(Context context, AutoWifiController.AutoWifiAdapter adapter, List<String> exceptNetworks) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mExceptNetworkKeys = exceptNetworks;
        this.mIntentFilterSettings = new IntentFilter(ACTION_AUTOWIFI_NOTIFICATION_SETTINGS);
        this.mIntentFilterCancel = new IntentFilter(ACTION_AUTOWIFI_NOTIFICATION_CANCEL);
        this.mIntentFilterAttFactoryReset = new IntentFilter("com.samsung.intent.action.SETTINGS_RESET_WIFI");
        this.mIntentFilterLocaleChanged = new IntentFilter("android.intent.action.LOCALE_CHANGED");
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d(AutoWifiNotificationController.TAG, "receive action " + intent.getAction());
                if (AutoWifiNotificationController.ACTION_AUTOWIFI_NOTIFICATION_SETTINGS.equals(intent.getAction())) {
                    context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                    if (AutoWifiNotificationController.this.mCurrentAddedNotiSsid != null) {
                        Intent intentSetting = new Intent();
                        intentSetting.setClassName("com.android.settings", "com.android.settings.Settings$ConfigureWifiSettingsActivity");
                        intentSetting.putExtra(":settings:fragment_args_key", AutoWifiNotificationController.PREF_KEY_AUTO_WIFI);
                        intentSetting.addFlags(268435456);
                        context.startActivity(intentSetting);
                        String unused = AutoWifiNotificationController.this.mCurrentAddedNotiSsid = null;
                    }
                } else if (AutoWifiNotificationController.ACTION_AUTOWIFI_NOTIFICATION_CANCEL.equals(intent.getAction())) {
                    int notiId = ((Integer) intent.getExtra("notiid")).intValue();
                    context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                    if (notiId == AutoWifiNotificationController.this.mNotiIcon) {
                        String unused2 = AutoWifiNotificationController.this.mCurrentAddedNotiSsid = null;
                    } else {
                        String unused3 = AutoWifiNotificationController.this.mCurrentDeletedNotiSsid = null;
                    }
                } else if ("com.samsung.intent.action.SETTINGS_RESET_WIFI".equals(intent.getAction())) {
                    AutoWifiNotificationController.this.factoryReset();
                } else if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    AutoWifiNotificationController.this.refreshNotification();
                }
            }
        };
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilterSettings);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilterCancel);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilterAttFactoryReset);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilterLocaleChanged);
        Log.d(TAG, "create AutoWifiNotificationController");
    }

    public void setGeofenceStateExit() {
        Log.d(TAG, "setGeofenceStateExit");
        this.mIncreasedScores.clear();
        this.mDecreasedScores.clear();
    }

    public void setConfigForTest(long point1, long point2, long minus, long recovery) {
        if (point1 >= 0) {
            this.mDurationForPoint1 = point1;
        }
        if (point2 >= 0) {
            this.mDurationForPoint2 = point2;
        }
        if (minus >= 0) {
            this.mDurationForMinusPoint = minus;
        }
        if (recovery >= 0) {
            this.mDurationForRecoveryPoint = recovery;
        }
    }

    public void setEnable(boolean enable) {
        this.mEnableToUse = enable;
    }

    public String dump() {
        StringBuffer sb = new StringBuffer();
        if (DBG) {
            sb.append("Duration For PlusPoint1:");
            sb.append(this.mDurationForPoint1 / 1000);
            sb.append(" seconds\n");
            sb.append("Duration For PlusPoint2:");
            sb.append(this.mDurationForPoint2 / 1000);
            sb.append(" seconds\n");
            sb.append("Duration For MinusPoint:");
            sb.append(this.mDurationForMinusPoint / 1000);
            sb.append(" seconds\n");
            sb.append("Duration For RecoveryPoint:");
            sb.append(this.mDurationForRecoveryPoint / 1000);
            sb.append(" seconds\n");
        }
        return sb.toString();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0053, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void setNetworkState(boolean r3, boolean r4, android.net.wifi.WifiConfiguration r5, java.lang.String r6) {
        /*
            r2 = this;
            monitor-enter(r2)
            if (r3 == 0) goto L_0x0031
            boolean r0 = r2.checkApReachToScore(r5)     // Catch:{ all -> 0x0054 }
            if (r0 == 0) goto L_0x001e
            boolean r0 = r2.mEnableToUse     // Catch:{ all -> 0x0054 }
            if (r0 != 0) goto L_0x0016
            java.lang.String r0 = "AutoWifiNotiController"
            java.lang.String r1 = "AutoWifi is disabled, do not add to Favorite APs"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x0054 }
            monitor-exit(r2)
            return
        L_0x0016:
            r0 = 1
            java.lang.Boolean r0 = java.lang.Boolean.valueOf(r0)     // Catch:{ all -> 0x0054 }
            r2.setAndShowNotification(r5, r6, r0)     // Catch:{ all -> 0x0054 }
        L_0x001e:
            android.net.wifi.WifiConfiguration r0 = r2.mLastNetworkConfig     // Catch:{ all -> 0x0054 }
            if (r0 != 0) goto L_0x0052
            java.util.Calendar r0 = java.util.Calendar.getInstance()     // Catch:{ all -> 0x0054 }
            long r0 = r0.getTimeInMillis()     // Catch:{ all -> 0x0054 }
            r2.mConnectedTime = r0     // Catch:{ all -> 0x0054 }
            r2.mLastNetworkConfig = r5     // Catch:{ all -> 0x0054 }
            r2.mLastNetworkKey = r6     // Catch:{ all -> 0x0054 }
            goto L_0x0052
        L_0x0031:
            android.net.wifi.WifiConfiguration r0 = r2.mLastNetworkConfig     // Catch:{ all -> 0x0054 }
            if (r0 == 0) goto L_0x0052
            if (r4 == 0) goto L_0x003a
            r2.checkAndDecreaseScore()     // Catch:{ all -> 0x0054 }
        L_0x003a:
            android.net.wifi.WifiConfiguration r0 = r2.mLastNetworkConfig     // Catch:{ all -> 0x0054 }
            int r0 = r0.semAutoWifiScore     // Catch:{ all -> 0x0054 }
            r1 = 4
            if (r0 < r1) goto L_0x0049
            android.net.wifi.WifiConfiguration r0 = r2.mLastNetworkConfig     // Catch:{ all -> 0x0054 }
            int r0 = r0.semAutoWifiScore     // Catch:{ all -> 0x0054 }
            r1 = 99
            if (r0 != r1) goto L_0x004c
        L_0x0049:
            r2.checkAndIncreaseScore()     // Catch:{ all -> 0x0054 }
        L_0x004c:
            r0 = 0
            r2.mLastNetworkConfig = r0     // Catch:{ all -> 0x0054 }
            r0 = 0
            r2.isApWithNumerousCellId = r0     // Catch:{ all -> 0x0054 }
        L_0x0052:
            monitor-exit(r2)
            return
        L_0x0054:
            r3 = move-exception
            monitor-exit(r2)
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.AutoWifiNotificationController.setNetworkState(boolean, boolean, android.net.wifi.WifiConfiguration, java.lang.String):void");
    }

    public void setPolicyForApWithNumerousCellId() {
        this.isApWithNumerousCellId = true;
    }

    public void setApInManyAreas(WifiConfiguration config, String networkKey) {
        if (!this.mEnableToUse) {
            Log.d(TAG, "AutoWifi is disabled, do not add KT home AP to favorite");
        } else if (config.semAutoWifiScore > 98) {
            setAndShowNotification(config, networkKey, false);
        }
    }

    public void setKTHomeApToFavorite(WifiConfiguration config, String networkKey) {
        if (!this.mEnableToUse) {
            config.semAutoWifiScore = 4;
            updateAutoWifiScore(config.networkId, config.semAutoWifiScore);
            return;
        }
        config.semAutoWifiScore = 101;
        setAndShowNotification(config, networkKey, true);
    }

    private void checkAndDecreaseScore() {
        WifiConfiguration wifiConfiguration = this.mLastNetworkConfig;
        if (wifiConfiguration != null && wifiConfiguration.semAutoWifiScore > 0) {
            if (isScoreFixedAP()) {
                Log.d(TAG, "Do not decrease score because SSID is " + removeDoubleQuotes(this.mLastNetworkConfig.SSID));
            } else if (!this.mDecreasedScores.containsKey(this.mLastNetworkKey) || this.mDecreasedScores.get(this.mLastNetworkKey).intValue() <= 0) {
                long duration = Calendar.getInstance().getTimeInMillis() - this.mConnectedTime;
                Log.d(TAG, "checkAndDecreaseScore." + this.mLastNetworkConfig.SSID + "prevscore: " + this.mLastNetworkConfig.semAutoWifiScore);
                if (duration < this.mDurationForMinusPoint) {
                    this.mLastNetworkConfig.semAutoWifiScore--;
                    this.mDecreasedScores.put(this.mLastNetworkKey, 1);
                    if (this.mLastNetworkConfig.semAutoWifiScore == 98) {
                        setAndShowNotification(this.mLastNetworkConfig, this.mLastNetworkKey, false);
                    } else {
                        updateAutoWifiScore(this.mLastNetworkConfig.networkId, this.mLastNetworkConfig.semAutoWifiScore);
                    }
                    Log.d(TAG, "duration: " + duration + " >= " + this.mDurationForMinusPoint + "ms, score: " + this.mLastNetworkConfig.semAutoWifiScore);
                    return;
                }
                Log.d(TAG, "duration: " + duration + " < " + this.mDurationForMinusPoint + "ms, score: " + this.mLastNetworkConfig.semAutoWifiScore);
            } else {
                Log.d(TAG, "AutoWifi score of AP is already decreased.");
            }
        }
    }

    private void checkAndIncreaseScore() {
        int score = this.mLastNetworkConfig.semAutoWifiScore;
        int prevScore = this.mLastNetworkConfig.semAutoWifiScore;
        long duration = Calendar.getInstance().getTimeInMillis() - this.mConnectedTime;
        if (isScoreFixedAP()) {
            Log.d(TAG, "Do not increase score because SSID is " + this.mLastNetworkConfig.SSID);
        } else if (score > 4) {
            if (duration >= this.mDurationForRecoveryPoint) {
                updateAutoWifiScore(this.mLastNetworkConfig.networkId, 100);
            }
            Log.d(TAG, this.mLastNetworkKey + "duration: " + duration + "ms, score: " + this.mLastNetworkConfig.semAutoWifiScore);
        } else {
            int increasedScore = 0;
            if (this.mIncreasedScores.containsKey(this.mLastNetworkKey)) {
                increasedScore = this.mIncreasedScores.get(this.mLastNetworkKey).intValue();
            }
            Log.d(TAG, this.mLastNetworkKey + ", increasedscore: " + increasedScore + ", prevScore: " + this.mLastNetworkConfig.semAutoWifiScore);
            if (duration >= this.mDurationForPoint1) {
                if (increasedScore < 1 && !this.isApWithNumerousCellId) {
                    score++;
                    increasedScore++;
                }
                if (duration >= this.mDurationForPoint2 && increasedScore < 2) {
                    score++;
                    increasedScore++;
                }
            }
            if (prevScore != score) {
                this.mIncreasedScores.put(this.mLastNetworkKey, Integer.valueOf(increasedScore));
                WifiConfiguration wifiConfiguration = this.mLastNetworkConfig;
                wifiConfiguration.semAutoWifiScore = score;
                updateAutoWifiScore(wifiConfiguration.networkId, score);
                Log.d(TAG, "duration: " + duration + "ms, score: " + this.mLastNetworkConfig.semAutoWifiScore);
                return;
            }
            Log.d(TAG, "duration: " + duration + "ms, score: " + this.mLastNetworkConfig.semAutoWifiScore + ", duration1/2 : " + this.mDurationForPoint1 + "ms/" + this.mDurationForPoint2 + "ms");
        }
    }

    private void updateAutoWifiScore(int networkId, int newScore) {
        WifiConfiguration config = this.mAdapter.getSpecificNetwork(networkId);
        if (config != null) {
            config.semAutoWifiScore = newScore;
            this.mAdapter.addOrUpdateNetwork(config);
            return;
        }
        Log.d(TAG, "Try to update score but Configuration is null");
    }

    private boolean checkApReachToScore(WifiConfiguration config) {
        int score = config.semAutoWifiScore;
        if (score < 4 || score >= 99) {
            return false;
        }
        return true;
    }

    public void factoryReset() {
        Log.d(TAG, "Reset Network Settings");
        this.mIncreasedScores.clear();
        this.mDecreasedScores.clear();
    }

    public synchronized void forgetNetwork(String networkKey, WifiConfiguration config) {
        Log.d(TAG, "forgetNetwork, " + networkKey);
        this.mIncreasedScores.remove(networkKey);
        this.mDecreasedScores.remove(networkKey);
        if (this.mLastNetworkKey != null && this.mLastNetworkKey.equals(networkKey)) {
            this.mLastNetworkConfig = null;
        }
        if (this.mCurrentAddedNotiTag != null && this.mCurrentAddedNotiTag.equals(networkKey)) {
            this.mCurrentAddedNotiSsid = null;
        }
        if (this.mCurrentDeletedNotiTag != null && this.mCurrentDeletedNotiTag.equals(networkKey)) {
            this.mCurrentDeletedNotiSsid = null;
        }
        if (isFavoriteAP(config)) {
            addRemoveApToDatebase(config);
        }
    }

    private boolean isFavoriteAP(WifiConfiguration config) {
        if (config.semAutoWifiScore > 98) {
            return true;
        }
        return false;
    }

    private boolean isScoreFixedAP() {
        List<String> list;
        WifiConfiguration wifiConfiguration = this.mLastNetworkConfig;
        if (wifiConfiguration == null || (list = this.mExceptNetworkKeys) == null || !list.contains(wifiConfiguration.configKey())) {
            return false;
        }
        return true;
    }

    private static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private void setAndShowNotification(WifiConfiguration config, String networkKey, Boolean isAddToFavoriteAPs) {
        if (!networkKey.equals(this.mCurrentAddedNotiTag)) {
            networkKey.equals(this.mCurrentDeletedNotiTag);
        }
        showNotification(config.SSID, isAddToFavoriteAPs, false);
        if (isAddToFavoriteAPs.booleanValue()) {
            this.mCurrentAddedNotiTag = networkKey;
            this.mCurrentAddedNotiSsid = config.SSID;
            if (config.semAutoWifiScore != 101) {
                config.semAutoWifiScore = 100;
            }
            addFavoriteApToDatebase(config);
        } else {
            this.mCurrentDeletedNotiTag = networkKey;
            this.mCurrentDeletedNotiSsid = config.SSID;
            config.semAutoWifiScore = 0;
            addRemoveApToDatebase(config);
        }
        updateAutoWifiScore(config.networkId, config.semAutoWifiScore);
        Log.d(TAG, "setAndShowNotification, " + networkKey + ", score: " + config.semAutoWifiScore);
    }

    /* access modifiers changed from: private */
    public void refreshNotification() {
        String str = this.mCurrentAddedNotiSsid;
        if (str != null) {
            showNotification(str, true, true);
        }
        String str2 = this.mCurrentDeletedNotiSsid;
        if (str2 != null) {
            showNotification(str2, false, true);
        }
    }

    private void addFavoriteApToDatebase(WifiConfiguration config) {
        String data = Settings.Global.getString(this.mContext.getContentResolver(), "sem_auto_wifi_added_removed_list");
        try {
            JSONObject obj = new JSONObject();
            JSONArray array = new JSONArray();
            if (!TextUtils.isEmpty(data)) {
                obj = new JSONObject(data);
                if (obj.has(JSON_KEY_ADD) && (array = obj.getJSONArray(JSON_KEY_ADD)) == null) {
                    return;
                }
            }
            String key = config.configKey().toString();
            JSONObject newObj = new JSONObject();
            newObj.put(JSON_KEY_CONFIG, key);
            array.put(newObj);
            obj.put(JSON_KEY_ADD, array);
            if (obj.has(JSON_KEY_REMOVE)) {
                array = (JSONArray) obj.get(JSON_KEY_REMOVE);
            }
            if (array != null && containsConfig(array, config.configKey().toString())) {
                obj.put(JSON_KEY_REMOVE, new JSONArray());
            }
            Settings.Global.putString(this.mContext.getContentResolver(), "sem_auto_wifi_added_removed_list", obj.toString());
            if (DBG) {
                Log.d(TAG, "Add " + config.configKey() + " to Database");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot put Auto Wi-Fi data");
        }
    }

    private void addRemoveApToDatebase(WifiConfiguration config) {
        if (TextUtils.isEmpty(Settings.Global.getString(this.mContext.getContentResolver(), "sem_auto_wifi_added_removed_list"))) {
            Log.e(TAG, "Remove Favorite AP but there is no remained DB");
            addFavoriteApToDatebase(config);
        }
        try {
            JSONObject obj = new JSONObject(Settings.Global.getString(this.mContext.getContentResolver(), "sem_auto_wifi_added_removed_list"));
            JSONArray array = new JSONArray();
            if (obj.has(JSON_KEY_ADD)) {
                array = obj.getJSONArray(JSON_KEY_ADD);
            }
            JSONArray newArray = new JSONArray();
            String key = config.configKey().toString();
            if (array != null) {
                int len = array.length();
                for (int i = 0; i < len; i++) {
                    JSONObject current = array.getJSONObject(i);
                    if (current != null && !key.equals(current.getString(JSON_KEY_CONFIG))) {
                        newArray.put(current);
                    }
                }
            }
            JSONArray arrayRemoved = new JSONArray();
            if (!containsConfig(arrayRemoved, key)) {
                JSONObject newObj = new JSONObject();
                newObj.put(JSON_KEY_CONFIG, key);
                arrayRemoved.put(newObj);
            }
            obj.put(JSON_KEY_ADD, newArray);
            obj.put(JSON_KEY_REMOVE, arrayRemoved);
            Settings.Global.putString(this.mContext.getContentResolver(), "sem_auto_wifi_added_removed_list", obj.toString());
            if (DBG) {
                Log.d(TAG, "Remove " + config.configKey() + " to Database");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot remove Auto Wi-Fi Data");
        }
    }

    private boolean containsConfig(JSONArray array, String config) {
        int len = array.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject current = array.getJSONObject(i);
                if (current != null && config.equals(current.getString(JSON_KEY_CONFIG))) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void showNotification(String ssid, Boolean isAddToFavoriteAPs, Boolean isRefresh) {
    }
}
