package com.samsung.android.server.wifi.sns;

import android.os.Debug;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class SnsBigDataFeature {
    protected static boolean DBG = Debug.semIsProductDev();
    protected final String TAG;
    private final HashMap<String, String> mDataMap;
    private final String mDqaFeatureName;
    private final boolean mIsDqaEnabled;
    private ArrayList<String> mJsonFormatArray;

    public abstract String getJsonFormat();

    public SnsBigDataFeature() {
        this.mIsDqaEnabled = true;
        this.mDqaFeatureName = null;
        this.TAG = getClass().getSimpleName();
        this.mDataMap = new HashMap<>();
        this.mJsonFormatArray = new ArrayList<>();
    }

    public SnsBigDataFeature(boolean dqaEnabled, String dqaFeatureName) {
        this.mIsDqaEnabled = dqaEnabled;
        this.mDqaFeatureName = dqaFeatureName;
        this.TAG = getClass().getSimpleName();
        this.mDataMap = new HashMap<>();
        this.mJsonFormatArray = new ArrayList<>();
    }

    public void addOrUpdateValue(String key, String value) {
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "addOrUpdateValue - " + key + ", " + value);
        }
        putValue(key, value);
    }

    public void addOrUpdateValue(String key, int value) {
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "addOrUpdateValue - " + key + ", " + value);
        }
        putValue(key, String.valueOf(value));
    }

    public void addOrUpdateValue(String key, long value) {
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "addOrUpdateValue - " + key + ", " + value);
        }
        putValue(key, String.valueOf(value));
    }

    public void addOrUpdateValue(String key, double value) {
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "addOrUpdateValue - " + key + ", " + value);
        }
        putValue(key, String.valueOf(value));
    }

    public void addOrUpdateValues(HashMap<String, String> values) {
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "addOrUpdateValues - " + values.size() + " items");
        }
        putValues(values);
    }

    public void addOrUpdateAllValue() {
    }

    public void resetData() {
        if (DBG) {
            Log.d(this.TAG, "resetData");
        }
        synchronized (this.mDataMap) {
            this.mDataMap.clear();
        }
    }

    public void addCurrentDataToJsonFormatArray() {
        if (DBG) {
            Log.d(this.TAG, "addCurrentDataToJsonFormatArray");
        }
        this.mJsonFormatArray.add(getJsonFormat());
    }

    public String[] getJsonFormatArray() {
        if (DBG) {
            Log.d(this.TAG, "getJsonFormatArray");
        }
        ArrayList<String> arrayList = this.mJsonFormatArray;
        if (arrayList == null || arrayList.size() <= 0) {
            return null;
        }
        return (String[]) this.mJsonFormatArray.toArray();
    }

    public String getDqaFeatureName() {
        return this.mDqaFeatureName;
    }

    public boolean getIsDqaEnabled() {
        return this.mIsDqaEnabled;
    }

    /* access modifiers changed from: protected */
    public void putValue(String key, String value) {
        if (key != null && value != null) {
            synchronized (this.mDataMap) {
                if (this.mDataMap.containsKey(key)) {
                    this.mDataMap.remove(key);
                }
                this.mDataMap.put(key, value);
            }
        } else if (DBG) {
            String str = this.TAG;
            Log.d(str, "param is null - " + key + ", " + value);
        }
    }

    /* access modifiers changed from: protected */
    public void putValues(HashMap<String, String> values) {
        synchronized (this.mDataMap) {
            for (String key : values.keySet()) {
                if (DBG) {
                    String str = this.TAG;
                    Log.d(str, "putValues - " + key + ", " + values.get(key));
                }
                if (this.mDataMap.containsKey(key)) {
                    this.mDataMap.remove(key);
                }
                this.mDataMap.put(key, values.get(key));
            }
        }
    }

    protected static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    /* access modifiers changed from: protected */
    public String getValue(String key, String defaultValue) {
        synchronized (this.mDataMap) {
            if (!this.mDataMap.containsKey(key)) {
                return defaultValue;
            }
            String str = this.mDataMap.get(key);
            return str;
        }
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
}
