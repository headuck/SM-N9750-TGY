package com.samsung.android.server.wifi.dqa;

import android.os.Bundle;
import android.os.Debug;
import com.android.server.wifi.WifiConfigManager;
import java.util.Collection;
import java.util.List;

public abstract class WifiIssuePattern {
    protected static final boolean DBG = Debug.semIsProductDev();
    public static final String KEY_HUMAN_READABLE_TIME = "htime";
    public static final String KEY_TIME = "time";
    private ReportData mLastSeenData;
    private List<ReportData> mLogs;

    public abstract Collection<Integer> getAssociatedKeys();

    public abstract Bundle getBigDataParams();

    public abstract String getPatternId();

    public abstract boolean isAssociated(int i, ReportData reportData);

    public abstract boolean matches();

    public boolean matches(List<ReportData> logs) {
        this.mLogs = logs;
        boolean ret = matches();
        this.mLastSeenData = logs.get(logs.size() - 1);
        return ret;
    }

    /* access modifiers changed from: protected */
    public Bundle getBigDataBundle(String feature, String data) {
        if (data == null || data.length() == 0) {
            return null;
        }
        Bundle args = new Bundle();
        args.putBoolean("bigdata", true);
        args.putString("feature", feature);
        args.putString("data", data);
        args.putString(ReportIdKey.KEY_PATTERN_ID, getPatternId());
        return args;
    }

    /* access modifiers changed from: protected */
    public ReportData getLastIndexOfData(int reportId) {
        return getLastIndexOfData(reportId, 1);
    }

    /* access modifiers changed from: protected */
    public ReportData getLastIndexOfData(int reportId, int counter) {
        List<ReportData> list = this.mLogs;
        if (list == null) {
            return null;
        }
        int foundCounter = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            ReportData data = this.mLogs.get(i);
            if (data == this.mLastSeenData) {
                return null;
            }
            if (data.mReportId == reportId && counter == (foundCounter = foundCounter + 1)) {
                return data;
            }
        }
        return null;
    }

    public static <V> V getValue(ReportData data, String key, V defaultValue) {
        return getValue(data.mData, key, defaultValue);
    }

    public static <V> V getValue(Bundle args, String key, V defaultValue) {
        V ret = defaultValue;
        Object mReturnValue = null;
        if (args == null || key == null || !args.containsKey(key)) {
            return ret;
        }
        Object value = args.get(key);
        if (value == null) {
            mReturnValue = defaultValue;
        } else if (value instanceof String) {
            String valueString = (String) value;
            if (defaultValue != null) {
                try {
                    if (defaultValue instanceof Boolean) {
                        mReturnValue = Boolean.valueOf(Boolean.parseBoolean(valueString));
                    } else if (defaultValue instanceof Integer) {
                        mReturnValue = Integer.valueOf(Integer.parseInt(valueString));
                    } else if (defaultValue instanceof Long) {
                        mReturnValue = Long.valueOf(Long.parseLong(valueString));
                    } else if (defaultValue instanceof String) {
                        mReturnValue = valueString;
                    }
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        } else {
            mReturnValue = value;
        }
        return mReturnValue;
    }

    /* access modifiers changed from: protected */
    public boolean isApiCalledBySystemApk(String appName) {
        if ("com.android.settings".equals(appName) || WifiConfigManager.SYSUI_PACKAGE_NAME.equals(appName) || "com.android.shell".equals(appName) || "android".equals(appName)) {
            return true;
        }
        if (appName == null || !appName.startsWith("system")) {
            return false;
        }
        return true;
    }
}
