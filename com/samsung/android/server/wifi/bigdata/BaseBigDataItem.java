package com.samsung.android.server.wifi.bigdata;

import android.os.SystemClock;
import android.util.Log;
import java.util.HashMap;

abstract class BaseBigDataItem {
    public static final String COMPONENT_ID = "WiFi";
    public static final String HIT_TYPE_IMMEDIATLY = "ph";
    public static final String HIT_TYPE_ONCE_A_DAY = "sm";
    public static final int TYPE_CONTEXT_FRAMEWORK = 0;
    public static final int TYPE_HQM_DQA = 2;
    public static final int TYPE_HQM_DQA_PRIVATE = 3;
    public static final int TYPE_HW_PARAM = 1;
    private final long DIV_MINUTE = 60000;
    protected final String TAG = getClass().getSimpleName();
    private final HashMap<String, String> mExtraData;
    private final String mFeatureName;
    protected boolean mLogMessages;
    private long mTime = 0;

    public abstract String getJsonFormat();

    public abstract boolean parseData(String str);

    public BaseBigDataItem(String featureName) {
        this.mFeatureName = featureName;
        this.mExtraData = new HashMap<>();
        this.mLogMessages = false;
    }

    public String getFeatureName() {
        return this.mFeatureName;
    }

    public void clearData() {
        synchronized (this.mExtraData) {
            this.mExtraData.clear();
        }
    }

    /* access modifiers changed from: protected */
    public String[] getArray(String data) {
        if (!this.mLogMessages) {
            return data.split("\\s+");
        }
        String[] array = data.split("\\s+");
        for (int i = 0; i < array.length; i++) {
            String str = this.TAG;
            Log.d(str, ", array[" + i + "] - " + array[i]);
        }
        return array;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0028, code lost:
        return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.String getValue(java.lang.String r5, java.lang.String r6) {
        /*
            r4 = this;
            java.util.HashMap<java.lang.String, java.lang.String> r0 = r4.mExtraData
            monitor-enter(r0)
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r4.mExtraData     // Catch:{ all -> 0x0029 }
            boolean r1 = r1.containsKey(r5)     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0027
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r4.mExtraData     // Catch:{ all -> 0x0029 }
            java.lang.Object r1 = r1.get(r5)     // Catch:{ all -> 0x0029 }
            java.lang.String r1 = (java.lang.String) r1     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0027
            java.lang.String r2 = "x"
            boolean r2 = r2.equals(r1)     // Catch:{ all -> 0x0029 }
            if (r2 != 0) goto L_0x0027
            java.lang.String r2 = "\""
            java.lang.String r3 = "'"
            java.lang.String r2 = r1.replace(r2, r3)     // Catch:{ all -> 0x0029 }
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            return r2
        L_0x0027:
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            return r6
        L_0x0029:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.bigdata.BaseBigDataItem.getValue(java.lang.String, java.lang.String):java.lang.String");
    }

    /* access modifiers changed from: protected */
    public String getKeyValueString(String key, String defaultValue) {
        return new String(convertToQuotedString(key) + ":" + convertToQuotedString(getValue(key, defaultValue)));
    }

    /* access modifiers changed from: protected */
    public String getKeyValueStrings(String[][] keys) {
        if (keys == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        boolean ignoreComma = true;
        for (String[] key : keys) {
            if (!ignoreComma) {
                sb.append(",");
            }
            sb.append(getKeyValueString(key[0], key[1]));
            ignoreComma = false;
        }
        return sb.toString();
    }

    public void addOrUpdateValue(String key, String value) {
        putValue(key, value);
    }

    public void addOrUpdateValue(String key, int value) {
        putValue(key, String.valueOf(value));
    }

    /* access modifiers changed from: protected */
    public void putValue(String key, String value) {
        synchronized (this.mExtraData) {
            if (this.mExtraData.containsKey(key)) {
                this.mExtraData.remove(key);
            }
            this.mExtraData.put(key, value);
        }
    }

    /* access modifiers changed from: protected */
    public void putValueAppend(String key, String value) {
        if (this.mExtraData.containsKey(key)) {
            value = this.mExtraData.remove(key) + "," + value;
        }
        this.mExtraData.put(key, value);
    }

    /* access modifiers changed from: protected */
    public void putValues(String[][] keys, String[] values) {
        putValues(keys, values, false);
    }

    /* access modifiers changed from: protected */
    public void putValues(String[][] keys, String[] values, boolean appendDuplicateData) {
        int index;
        if (keys != null && values != null) {
            int index2 = 0;
            for (String[] key : keys) {
                if (appendDuplicateData) {
                    index = index2 + 1;
                    putValueAppend(key[0], values[index2]);
                } else {
                    index = index2 + 1;
                    putValue(key[0], values[index2]);
                }
                index2 = index;
            }
        }
    }

    protected static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    /* access modifiers changed from: protected */
    public void resetTime() {
        this.mTime = 0;
    }

    /* access modifiers changed from: protected */
    public void updateTime() {
        this.mTime = SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: protected */
    public String getDurationTimeKeyValueString(String key) {
        StringBuffer sb = new StringBuffer();
        sb.append(convertToQuotedString(key));
        sb.append(":");
        sb.append(convertToQuotedString(String.valueOf(getDurationTime(50000))));
        return sb.toString();
    }

    private int getDurationTime(int maxMin) {
        if (this.mTime == 0) {
            return 0;
        }
        long diffMin = (SystemClock.elapsedRealtime() - this.mTime) / 60000;
        if (diffMin < 0) {
            return 0;
        }
        if (diffMin > ((long) maxMin)) {
            return maxMin;
        }
        return (int) diffMin;
    }

    public void setLogVisible(boolean visible) {
        this.mLogMessages = visible;
    }

    public boolean isAvailableLogging(int type) {
        if (type == 0) {
            return true;
        }
        return false;
    }

    public String getJsonFormatFor(int type) {
        return getJsonFormat();
    }

    public String getHitType() {
        return HIT_TYPE_ONCE_A_DAY;
    }
}
