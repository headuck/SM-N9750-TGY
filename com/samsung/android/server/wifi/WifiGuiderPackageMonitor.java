package com.samsung.android.server.wifi;

import android.os.SystemClock;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class WifiGuiderPackageMonitor {
    private static final String TAG = "WifiGuiderPackageMonitor";
    private final Map<String, CallerInfo> mMap = new HashMap();

    public void registerApi(String apiName, int periodSeconds, int limitCount) {
        this.mMap.put(apiName, new CallerInfo(apiName, periodSeconds, limitCount));
    }

    public synchronized int addApiLog(String packageName, boolean isForeground, String apiName) {
        CallerInfo info = null;
        if (this.mMap.containsKey(apiName)) {
            info = this.mMap.get(apiName);
        }
        if (info == null) {
            Log.d(TAG, "unregistered api : " + apiName);
            return -1;
        } else if (!info.addCallLogAndCheck(packageName, isForeground)) {
            return 0;
        } else {
            int size = info.getSize(packageName);
            Log.d(TAG, packageName + " called " + apiName + " frequently " + size + "/" + info.mLimitCounter);
            return size;
        }
    }

    public synchronized String dump() {
        StringBuffer sb;
        sb = new StringBuffer();
        for (String apiKey : this.mMap.keySet()) {
            CallerInfo info = this.mMap.get(apiKey);
            sb.append(apiKey);
            sb.append(", ");
            sb.append(info.mLimitCounter);
            sb.append(", ");
            sb.append(info.mCheckPeriodSeconds);
            sb.append(" sec\n");
            sb.append(info.dump());
        }
        return sb.toString();
    }

    private static class CallerInfo {
        private final Map<String, List<Long>> mCalledTimeMap = new HashMap();
        final int mCheckPeriodSeconds;
        final int mLimitCounter;
        int mMaxCounter;
        int mTotalCallingCount;

        public CallerInfo(String apiString, int checkPeriodSeconds, int limitCount) {
            this.mCheckPeriodSeconds = checkPeriodSeconds;
            this.mLimitCounter = limitCount;
        }

        public boolean addCallLogAndCheck(String packageName, boolean isForeground) {
            List<Long> timeTable = getTimeTable(packageName);
            removeOldItemFrom(timeTable);
            this.mTotalCallingCount++;
            if (!isForeground) {
                timeTable.add(Long.valueOf(getTime(0)));
            }
            int currentCounter = timeTable.size();
            this.mMaxCounter = Math.max(currentCounter, this.mMaxCounter);
            if (currentCounter >= this.mLimitCounter) {
                return true;
            }
            return false;
        }

        public String dump() {
            StringBuffer sb = new StringBuffer();
            for (String packageName : this.mCalledTimeMap.keySet()) {
                List<Long> timeTable = getTimeTable(packageName);
                removeOldItemFrom(timeTable);
                sb.append(packageName);
                sb.append(":");
                sb.append(timeTable.size());
                sb.append(", max:");
                sb.append(this.mMaxCounter);
                sb.append(", total:");
                sb.append(this.mTotalCallingCount);
                sb.append("\n");
            }
            return sb.toString();
        }

        public int getSize(String packageName) {
            return getTimeTable(packageName).size();
        }

        private List<Long> getTimeTable(String packageName) {
            List<Long> timeTable = null;
            if (this.mCalledTimeMap.containsKey(packageName)) {
                timeTable = this.mCalledTimeMap.get(packageName);
            }
            if (timeTable != null) {
                return timeTable;
            }
            List<Long> timeTable2 = new ArrayList<>();
            this.mCalledTimeMap.put(packageName, timeTable2);
            return timeTable2;
        }

        private void removeOldItemFrom(List<Long> timeTable) {
            timeTable.removeIf(new Predicate<Long>() {
                public boolean test(Long item) {
                    long longValue = item.longValue();
                    CallerInfo callerInfo = CallerInfo.this;
                    if (longValue < callerInfo.getTime(callerInfo.mCheckPeriodSeconds)) {
                        return true;
                    }
                    return false;
                }
            });
        }

        /* access modifiers changed from: private */
        public long getTime(int seconds) {
            return SystemClock.elapsedRealtime() - ((long) (seconds * 1000));
        }
    }
}
