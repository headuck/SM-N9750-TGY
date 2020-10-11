package com.android.server.wifi.tcp;

import android.os.Debug;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class WifiApInfo {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "WifiApInfo";
    private int mAccumulatedConnectionCount = 0;
    private int mAccumulatedConnectionTime = 0;
    private HashMap<String, DetectedPackageInfo> mDetectedPackageList = new HashMap<>();
    private final String mSsid;
    private int mSwitchForIndivdiaulAppsDetectionCount = 0;

    public static class DetectedPackageInfo {
        private final int PREVENTION_NORMAL_DETECTION_COUNT = 3;
        private final int PREVENTION_NORMAL_OPERATION_TIME_MINUTE = 30;
        /* access modifiers changed from: private */
        public int mDetectedCount;
        /* access modifiers changed from: private */
        public String mLastDetectedTime;
        /* access modifiers changed from: private */
        public final String mPackageName;
        /* access modifiers changed from: private */
        public int mPackageNormalOperationTime;

        public DetectedPackageInfo(String packageName, boolean isDetected) {
            this.mPackageName = packageName;
            this.mDetectedCount = isDetected;
            this.mLastDetectedTime = isDetected ? getTimeString() : "";
            this.mPackageNormalOperationTime = 0;
        }

        public DetectedPackageInfo(String packageName, int runningTime) {
            this.mPackageName = packageName;
            this.mDetectedCount = 0;
            this.mLastDetectedTime = "";
            this.mPackageNormalOperationTime = runningTime;
        }

        public DetectedPackageInfo(String packageName, int detectedCount, String lastDetectedTime, int packageNormalOperationTime) {
            this.mPackageName = packageName;
            this.mDetectedCount = detectedCount;
            this.mLastDetectedTime = lastDetectedTime;
            this.mPackageNormalOperationTime = packageNormalOperationTime;
        }

        public void updateDetectedInfo() {
            increaseDetectedCount();
            setLastDetectedTime(getTimeString());
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getDetectedCount() {
            return this.mDetectedCount;
        }

        public String getLastDetectedTime() {
            return this.mLastDetectedTime;
        }

        public void resetPackageNormalOperationTime() {
            this.mPackageNormalOperationTime = 0;
        }

        public int getPackageNormalOperationTime() {
            return this.mPackageNormalOperationTime;
        }

        public int addPackageNormalOperationTime(int runningTime) {
            int i = this.mPackageNormalOperationTime + runningTime;
            this.mPackageNormalOperationTime = i;
            return i;
        }

        public boolean isPackageNormalOperationTimePrevention() {
            return this.mPackageNormalOperationTime > 30 && this.mDetectedCount < 3;
        }

        private void increaseDetectedCount() {
            this.mDetectedCount++;
        }

        public void resetDetectedCount() {
            this.mDetectedCount = 0;
        }

        private void setLastDetectedTime(String lastDetectedTime) {
            this.mLastDetectedTime = lastDetectedTime;
        }

        private String getTimeString() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date()).toString();
        }
    }

    public WifiApInfo(String ssid) {
        this.mSsid = ssid;
    }

    public WifiApInfo(String ssid, int accumulatedConnectionCount, int accumulatedConnectionTime, int switchForIndivdiaulAppsDetectionCount, HashMap<String, DetectedPackageInfo> detectedPackageList) {
        this.mSsid = ssid;
        this.mAccumulatedConnectionCount = accumulatedConnectionCount;
        this.mAccumulatedConnectionTime = accumulatedConnectionTime;
        this.mSwitchForIndivdiaulAppsDetectionCount = switchForIndivdiaulAppsDetectionCount;
        this.mDetectedPackageList = detectedPackageList;
    }

    public String getSsid() {
        return this.mSsid;
    }

    public int getAccumulatedConnectionCount() {
        if (DBG) {
            Log.d(TAG, "getAccumulatedConnectionCount - " + this.mAccumulatedConnectionCount);
        }
        return this.mAccumulatedConnectionCount;
    }

    public void setAccumulatedConnectionCount(int accumulatedConnectionCount) {
        if (DBG) {
            Log.d(TAG, "setAccumulatedConnectionCount - " + accumulatedConnectionCount);
        }
        this.mAccumulatedConnectionCount = accumulatedConnectionCount;
    }

    public int getAccumulatedConnectionTime() {
        return this.mAccumulatedConnectionTime;
    }

    public void setAccumulatedConnectionTime(int accumulatedConnectionTime) {
        if (DBG) {
            Log.d(TAG, "setAccumulatedConnectionTime - " + accumulatedConnectionTime);
        }
        this.mAccumulatedConnectionTime = accumulatedConnectionTime;
    }

    public int getSwitchForIndivdiaulAppsDetectionCount() {
        return this.mSwitchForIndivdiaulAppsDetectionCount;
    }

    public void setSwitchForIndivdiaulAppsDetectionCount(int switchForIndivdiaulAppsDetectionCount) {
        if (DBG) {
            Log.d(TAG, "setSwitchForIndivdiaulAppsDetectionCount - " + switchForIndivdiaulAppsDetectionCount);
        }
        this.mSwitchForIndivdiaulAppsDetectionCount = switchForIndivdiaulAppsDetectionCount;
    }

    public void addSwitchForIndivdiaulAppsDetectionCount(String packageName) {
        if (DBG) {
            Log.d(TAG, "addSwitchForIndivdiaulAppsDetectionCount - " + packageName);
        }
        this.mSwitchForIndivdiaulAppsDetectionCount++;
        updateDetectedPackageList(packageName);
    }

    public void addDetectedPakcageInfo(String packageName, int runningTime) {
        if (DBG) {
            Log.d(TAG, "addDetectedPakcageInfo - " + packageName);
        }
        if (packageName != null) {
            if (this.mDetectedPackageList.containsKey(packageName)) {
                this.mDetectedPackageList.remove(packageName);
            }
            this.mDetectedPackageList.put(packageName, new DetectedPackageInfo(packageName, runningTime));
        }
    }

    public void resetSwitchForIndivdiaulAppsDetectionCount(String packageName) {
        if (DBG) {
            Log.d(TAG, "resetSwitchForIndivdiaulAppsDetectionCount - " + packageName);
        }
        if (packageName != null && this.mDetectedPackageList.containsKey(packageName)) {
            this.mDetectedPackageList.get(packageName).resetDetectedCount();
        }
    }

    public HashMap<String, DetectedPackageInfo> getDetectedPackageList() {
        return this.mDetectedPackageList;
    }

    public void setDetectedPackageList(HashMap<String, DetectedPackageInfo> detectedPackageList) {
        if (DBG) {
            Log.d(TAG, "setDetectedPackageList - " + detectedPackageList.toString());
        }
        this.mDetectedPackageList = detectedPackageList;
    }

    public int getPackageDetectedCount(String packageName) {
        if (DBG) {
            Log.d(TAG, "getPackageDetectedCount - " + packageName);
        }
        if (packageName == null || !this.mDetectedPackageList.containsKey(packageName)) {
            return 0;
        }
        return this.mDetectedPackageList.get(packageName).getDetectedCount();
    }

    public void updateNormalOperationTime(String packageName, int runningTime) {
        if (DBG) {
            Log.d(TAG, "updateNormalOperationTime - " + runningTime);
        }
        if (this.mDetectedPackageList.containsKey(packageName)) {
            DetectedPackageInfo packageInfo = this.mDetectedPackageList.get(packageName);
            if (runningTime == 0) {
                packageInfo.resetPackageNormalOperationTime();
            } else if (packageInfo.getDetectedCount() == 0 || packageInfo.isPackageNormalOperationTimePrevention()) {
                packageInfo.resetDetectedCount();
                packageInfo.addPackageNormalOperationTime(runningTime);
            }
        }
    }

    public boolean isNormalRunningTimePrevention(String packageName) {
        if (DBG) {
            Log.d(TAG, "isNormalRunningTimePrevention - " + packageName);
        }
        if (this.mDetectedPackageList.containsKey(packageName)) {
            return this.mDetectedPackageList.get(packageName).isPackageNormalOperationTimePrevention();
        }
        return false;
    }

    private void updateDetectedPackageList(String packageName) {
        if (DBG) {
            Log.d(TAG, "updateDetectedPackageList - " + packageName);
        }
        if (packageName == null) {
            return;
        }
        if (this.mDetectedPackageList.containsKey(packageName)) {
            this.mDetectedPackageList.get(packageName).updateDetectedInfo();
        } else {
            this.mDetectedPackageList.put(packageName, new DetectedPackageInfo(packageName, true));
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + this.mSsid + "]");
        StringBuilder sb2 = new StringBuilder();
        sb2.append(", CC:");
        sb2.append(this.mAccumulatedConnectionCount);
        sb.append(sb2.toString());
        sb.append(", CT:" + this.mAccumulatedConnectionTime);
        sb.append(", SFIADC:" + this.mSwitchForIndivdiaulAppsDetectionCount);
        sb.append(", Packages");
        for (DetectedPackageInfo info : this.mDetectedPackageList.values()) {
            sb.append(" [PN:" + info.mPackageName);
            sb.append(" DC:" + info.mDetectedCount);
            sb.append(" LDT:" + info.mLastDetectedTime);
            sb.append(" NOT:" + info.mPackageNormalOperationTime + "]");
        }
        return sb.toString();
    }
}
