package com.samsung.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiScanner;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class WifiScanController {
    private static final boolean DBG = Debug.semIsProductDev();
    public static final String DEFALUT_NLP_PACKAGE_NAME = "may.be.nlp.package";
    private static final int MAX_DURATION = 600000;
    public static final int SCANLOG_TYPE_1_6_11_CHANNEL_SCAN_COUNTER = 4;
    public static final int SCANLOG_TYPE_2_4GHZ_CHANNEL_SCAN_COUNTER = 3;
    public static final int SCANLOG_TYPE_CACHED_SCAN_COUNTER = 6;
    public static final int SCANLOG_TYPE_DELAYED_SCAN_COUNTER = 7;
    public static final int SCANLOG_TYPE_EXPT_DFS_CHANNEL_SCAN_COUNTER = 5;
    public static final int SCANLOG_TYPE_FULL_CHANNEL_SCAN_COUNTER = 2;
    public static final int SCANLOG_TYPE_SCAN_COUNT_TOTAL = 0;
    public static final int SCANLOG_TYPE_SCAN_INTERVAL = 1;
    public static final int SCAN_PROXY_TYPE_ADD_WHITELIST_FOR_NLP_BACKGROUND_APP = 200;
    public static final int SCAN_PROXY_TYPE_ADD_WHITELIST_FOR_NLP_FOREGROUND_APP = 100;
    public static final int SCAN_PROXY_TYPE_REMOVE_WHITELIST_FOR_NLP_BACKGROUND_APP = 201;
    public static final int SCAN_PROXY_TYPE_REMOVE_WHITELIST_FOR_NLP_FOREGROUND_APP = 101;
    public static final int SCAN_TYPE_CACHED_EXCEPT_PASSIVE_CHANNELS = 3;
    public static final int SCAN_TYPE_CACHED_FULL = 0;
    public static final int SCAN_TYPE_CACHED_PARTIAL_1_6_11_CHANNELS = 2;
    public static final int SCAN_TYPE_CACHED_PARTIAL_2_4_ONLY = 1;
    public static final int SCAN_TYPE_CACHED_USE_CACHED_RESULT = 4;
    public static final int SCAN_TYPE_DO_NOTHING = 5;
    public static final int SCAN_TYPE_FORCE_FULL = 6;
    public static final int SCAN_TYPE_MAX = 7;
    private static final String TAG = "WifiScanController";
    private final Context mContext;
    private String mDisabledReason = "";
    private boolean mEnabled = true;
    private long mLastActualScanActionTime = 0;
    private long mLastNLPScanRequestTime = 0;
    private String mLastRequestPackageName;
    private long mMaxDuration = WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS;
    private SemSmdMotionDetector mMotionDetector;
    private final HashMap<String, NLPScanSettings> mNLPPackages = new HashMap<>();
    private int mScanCounter2_4GHz;
    private int mScanCounterAll;
    private int mScanCounterCached;
    private int mScanCounterNlp;
    private final HashMap<String, ScanLog> mScanLogMap = new HashMap<>();

    public WifiScanController(Context context) {
        this.mContext = context;
        this.mMotionDetector = SemSmdMotionDetector.getInstance(context);
        updateNLPPackages();
    }

    public static class ScanLog {
        private static final int THRESHOULD_COUNT_FOR_BIG_DATA = 50;
        private static final long TIMEOUT_FOR_BIGDATA = 1200000;
        private int mCount = 0;
        private int mCount1_6_11Only = 0;
        private int mCount2_4Only = 0;
        private int mCountCached = 0;
        private int mCountDelayed = 0;
        private int mCountExceptDFS = 0;
        private int mCountForBigdata = 0;
        private int mCountFullChannel = 0;
        private final SimpleDateFormat mDateformat = new SimpleDateFormat("MM-dd HH:mm:ss");
        private final long mInitialTime;
        private long mLastTime = 0;
        public String mPackageName;
        private double mPeriod = 0.0d;
        private long mStartTimeForBigdata = 0;

        public ScanLog(String packagename) {
            this.mPackageName = packagename;
            this.mInitialTime = System.currentTimeMillis();
            this.mLastTime = System.currentTimeMillis();
            resetCounter();
        }

        public void addCounter(int scanType) {
            this.mCount++;
            this.mCountForBigdata++;
            this.mLastTime = System.currentTimeMillis();
            if (scanType == 1) {
                this.mCount2_4Only++;
            } else if (scanType == 2) {
                this.mCount1_6_11Only++;
            } else if (scanType == 3) {
                this.mCountExceptDFS++;
            } else if (scanType == 4) {
                this.mCountCached++;
            } else if (scanType != 5) {
                this.mCountFullChannel++;
            } else {
                this.mCountDelayed++;
            }
        }

        public int getScanLogData(int type) {
            switch (type) {
                case 0:
                    return this.mCountForBigdata;
                case 1:
                    calcPeriod();
                    return (int) this.mPeriod;
                case 2:
                    return this.mCountFullChannel;
                case 3:
                    return this.mCount2_4Only;
                case 4:
                    return this.mCount1_6_11Only;
                case 5:
                    return this.mCountExceptDFS;
                case 6:
                    return this.mCountCached;
                case 7:
                    return this.mCountDelayed;
                default:
                    return 0;
            }
        }

        public void resetCounter() {
            this.mCountForBigdata = 0;
            this.mCountDelayed = 0;
            this.mCountCached = 0;
            this.mCountFullChannel = 0;
            this.mCount2_4Only = 0;
            this.mCount1_6_11Only = 0;
            this.mCountExceptDFS = 0;
            this.mStartTimeForBigdata = this.mLastTime;
        }

        public boolean isReachedLimitation() {
            if (this.mLastTime - this.mStartTimeForBigdata <= TIMEOUT_FOR_BIGDATA) {
                return false;
            }
            if (this.mCountForBigdata > 50) {
                return true;
            }
            resetCounter();
            return false;
        }

        public String getBigDataExtra() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.mPackageName);
            sb.append(" ");
            sb.append(this.mCountForBigdata);
            sb.append(" ");
            sb.append(this.mCountDelayed);
            sb.append(" ");
            sb.append(this.mCountCached);
            sb.append(" ");
            sb.append(this.mCountFullChannel);
            sb.append(" ");
            sb.append(this.mCount2_4Only);
            sb.append(" ");
            sb.append(this.mCount1_6_11Only);
            sb.append(" ");
            sb.append(this.mCountExceptDFS);
            return sb.toString();
        }

        private void calcPeriod() {
            int tempCount = 1;
            int i = this.mCount;
            if (i > 1) {
                tempCount = i - 1;
            }
            this.mPeriod = (double) (((this.mLastTime - this.mInitialTime) / 1000) / ((long) tempCount));
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            calcPeriod();
            sb.append("         Count : " + this.mCount);
            sb.append("\n");
            sb.append("      Interval : " + this.mPeriod);
            sb.append("\n");
            sb.append("    Start Time : " + this.mDateformat.format(Long.valueOf(this.mInitialTime)));
            sb.append("\n");
            sb.append("     Last Time : " + this.mDateformat.format(Long.valueOf(this.mLastTime)));
            sb.append("\n");
            sb.append("  Total/20mins : " + this.mCountForBigdata);
            sb.append("\n");
            sb.append("        FullCH : " + this.mCountFullChannel);
            sb.append("\n");
            sb.append("   2.4GHz Only : " + this.mCount2_4Only);
            sb.append("\n");
            sb.append("     1,6,11 CH : " + this.mCount1_6_11Only);
            sb.append("\n");
            sb.append("     Ex.DFS CH : " + this.mCountExceptDFS);
            sb.append("\n");
            sb.append("        Cached : " + this.mCountCached);
            sb.append("\n");
            sb.append("       Delayed : " + this.mCountDelayed);
            sb.append("\n");
            return sb.toString();
        }
    }

    public void addHistoricalScanLog(String packageName, int scanType) {
        if (!"com.android.settings".equals(packageName)) {
            synchronized (this.mScanLogMap) {
                ScanLog scanLog = this.mScanLogMap.get(packageName);
                if (scanLog == null) {
                    scanLog = new ScanLog(packageName);
                    this.mScanLogMap.put(packageName, scanLog);
                }
                scanLog.addCounter(scanType);
                if (scanType == 1 || scanType == 2) {
                    this.mScanCounter2_4GHz++;
                } else if (scanType == 4) {
                    this.mScanCounterCached++;
                }
            }
        }
    }

    public String getScanCounterForBigdata(boolean resetCounter) {
        StringBuffer sb = new StringBuffer();
        sb.append(this.mScanCounterAll);
        sb.append(" ");
        sb.append(this.mScanCounterNlp);
        sb.append(" ");
        sb.append(this.mScanCounterCached);
        sb.append(" ");
        sb.append(this.mScanCounter2_4GHz);
        if (resetCounter) {
            this.mScanCounterAll = 0;
            this.mScanCounterNlp = 0;
            this.mScanCounterCached = 0;
            this.mScanCounter2_4GHz = 0;
        }
        return sb.toString();
    }

    public int getScanType(String packageName) {
        long now = SystemClock.elapsedRealtime();
        this.mLastRequestPackageName = packageName;
        this.mScanCounterAll++;
        boolean isNLPPackage = false;
        if (packageName == null) {
            Log.e(TAG, "request package name is null");
        } else {
            isNLPPackage = isNLPPackage(packageName);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("scan requested by ");
        sb.append(packageName);
        sb.append(isNLPPackage ? " (it's NLP package. check moving state)" : "");
        Log.i(TAG, sb.toString());
        if (!this.mEnabled) {
            Log.i(TAG, "scan disabled reason:" + this.mDisabledReason);
            addHistoricalScanLog(packageName, 5);
            return 5;
        } else if (isNLPPackage) {
            this.mScanCounterNlp++;
            this.mMotionDetector.resetTimer(now);
            if (checkScanDelayForCachedScan(packageName)) {
                Log.i(TAG, "Ignore scan request, reason:scan delay");
                addHistoricalScanLog(packageName, 5);
                return 5;
            }
            int scanType = getCustomScanType(packageName);
            if (scanType == 4 || scanType == 5) {
                Log.i(TAG, "Ignore scan request, policy:" + scanType);
                addHistoricalScanLog(packageName, scanType);
                return scanType;
            }
            if (scanType != 6 && this.mMotionDetector.isEnabled()) {
                if (this.mMotionDetector.getMovingStatus()) {
                    Log.i(TAG, "SMD detected. Force scan");
                    this.mLastNLPScanRequestTime = now;
                    this.mLastActualScanActionTime = now;
                    addHistoricalScanLog(packageName, scanType);
                    return scanType;
                }
                Log.d(TAG, "time diff: " + (now - this.mLastNLPScanRequestTime));
                if (now - this.mLastNLPScanRequestTime < this.mMaxDuration) {
                    Log.i(TAG, "Ignore scan request, use cached scan result");
                    addHistoricalScanLog(packageName, 4);
                    return 4;
                }
                if (DBG) {
                    Log.d(TAG, "reset timer");
                }
                this.mLastNLPScanRequestTime = SystemClock.elapsedRealtime();
            }
            Log.d(TAG, "force scan");
            this.mLastActualScanActionTime = now;
            addHistoricalScanLog(packageName, scanType);
            return scanType;
        } else {
            if (this.mMotionDetector.isSupported()) {
                this.mMotionDetector.checkAndStopMonitoring();
            }
            this.mLastActualScanActionTime = now;
            if (packageName != null) {
                addHistoricalScanLog(packageName, 0);
            }
            return 0;
        }
    }

    public void setEnableSMD(boolean enable) {
        this.mMotionDetector.setEnable(enable);
    }

    public void setEnableScan(boolean enabled, String reason) {
        Log.i(TAG, "setEnableScan " + enabled + " reason:" + reason);
        this.mEnabled = enabled;
        this.mDisabledReason = reason;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public String getDisableReason() {
        return this.mDisabledReason;
    }

    public void stopMonitoring() {
        this.mMotionDetector.stopMonitoring();
    }

    public void updateScanSettings(WifiScanner.ScanSettings settings, int scanType) {
        if (scanType == 0) {
            return;
        }
        if (scanType == 1) {
            settings.band = 0;
            settings.channels = new WifiScanner.ChannelSpec[11];
            settings.channels[0] = new WifiScanner.ChannelSpec(2412);
            settings.channels[1] = new WifiScanner.ChannelSpec(2417);
            settings.channels[2] = new WifiScanner.ChannelSpec(2422);
            settings.channels[3] = new WifiScanner.ChannelSpec(2427);
            settings.channels[4] = new WifiScanner.ChannelSpec(2432);
            settings.channels[5] = new WifiScanner.ChannelSpec(2437);
            settings.channels[6] = new WifiScanner.ChannelSpec(2442);
            settings.channels[7] = new WifiScanner.ChannelSpec(2447);
            settings.channels[8] = new WifiScanner.ChannelSpec(2452);
            settings.channels[9] = new WifiScanner.ChannelSpec(2457);
            settings.channels[10] = new WifiScanner.ChannelSpec(2462);
        } else if (scanType == 2) {
            settings.band = 0;
            settings.channels = new WifiScanner.ChannelSpec[3];
            settings.channels[0] = new WifiScanner.ChannelSpec(2412);
            settings.channels[1] = new WifiScanner.ChannelSpec(2437);
            settings.channels[2] = new WifiScanner.ChannelSpec(2462);
        } else if (scanType == 3) {
            settings.band = 2;
        } else if (scanType == 6) {
            settings.band = 7;
        }
    }

    private int getCustomScanType(String packageName) {
        String key = getNLPPackageKey(packageName);
        if (key == null) {
            return 0;
        }
        synchronized (this.mNLPPackages) {
            NLPScanSettings scanSetting = this.mNLPPackages.get(key);
            if (scanSetting == null) {
                return 0;
            }
            if (DBG) {
                Log.d(TAG, packageName + " scan type:" + scanSetting.mScanType);
            }
            int i = scanSetting.mScanType;
            return i;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkScanDelayForCachedScan(java.lang.String r10) {
        /*
            r9 = this;
            java.lang.String r0 = r9.getNLPPackageKey(r10)
            if (r0 == 0) goto L_0x0031
            java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.WifiScanController$NLPScanSettings> r1 = r9.mNLPPackages
            monitor-enter(r1)
            java.util.HashMap<java.lang.String, com.samsung.android.server.wifi.WifiScanController$NLPScanSettings> r2 = r9.mNLPPackages     // Catch:{ all -> 0x002e }
            java.lang.Object r2 = r2.get(r0)     // Catch:{ all -> 0x002e }
            com.samsung.android.server.wifi.WifiScanController$NLPScanSettings r2 = (com.samsung.android.server.wifi.WifiScanController.NLPScanSettings) r2     // Catch:{ all -> 0x002e }
            if (r2 == 0) goto L_0x002c
            long r3 = r2.mScanDelayMillis     // Catch:{ all -> 0x002e }
            r5 = 0
            int r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r3 == 0) goto L_0x002c
            long r3 = android.os.SystemClock.elapsedRealtime()     // Catch:{ all -> 0x002e }
            long r5 = r9.mLastActualScanActionTime     // Catch:{ all -> 0x002e }
            long r5 = r3 - r5
            long r7 = r2.mScanDelayMillis     // Catch:{ all -> 0x002e }
            int r5 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1))
            if (r5 >= 0) goto L_0x002c
            r5 = 1
            monitor-exit(r1)     // Catch:{ all -> 0x002e }
            return r5
        L_0x002c:
            monitor-exit(r1)     // Catch:{ all -> 0x002e }
            goto L_0x0031
        L_0x002e:
            r2 = move-exception
            monitor-exit(r1)     // Catch:{ all -> 0x002e }
            throw r2
        L_0x0031:
            r1 = 0
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiScanController.checkScanDelayForCachedScan(java.lang.String):boolean");
    }

    private ScanLog getScanLog(String packageName) {
        ScanLog scanLog;
        if (packageName == null) {
            return null;
        }
        synchronized (this.mScanLogMap) {
            scanLog = this.mScanLogMap.get(packageName);
        }
        return scanLog;
    }

    public int getScanLogData(String packageName, int type) {
        ScanLog scanLog = getScanLog(packageName);
        if (scanLog != null) {
            return scanLog.getScanLogData(type);
        }
        return 0;
    }

    public String getBigDataString(String packageName) {
        ScanLog scanLog = getScanLog(packageName);
        if (scanLog == null || !scanLog.isReachedLimitation()) {
            return null;
        }
        String result = scanLog.getBigDataExtra();
        scanLog.resetCounter();
        return result;
    }

    public void setCustomScanType(String packageName, int scanType, int scanDelaySeconds) {
        if (DBG) {
            Log.d(TAG, "pack:" + packageName + ", scanType: " + scanType + ", scanDelay:" + scanDelaySeconds + "(sec.)");
        }
        addOrUpdateNLPPackageSetting(packageName, scanType, ((long) scanDelaySeconds) * 1000);
    }

    public void setDurationSettings(int duration) {
        this.mMaxDuration = (long) duration;
    }

    public String dump() {
        StringBuffer sb = new StringBuffer();
        sb.append("WifiScanController:\n");
        sb.append("-Enabled:" + this.mEnabled);
        sb.append(", Reason:" + this.mDisabledReason);
        sb.append("\n");
        sb.append("-SMD Supported:" + this.mMotionDetector.isSupported());
        sb.append(", used:" + this.mMotionDetector.isEnabled());
        sb.append("\n");
        sb.append("-MAX Duration:" + this.mMaxDuration);
        sb.append("\n");
        synchronized (this.mNLPPackages) {
            for (String packageName : this.mNLPPackages.keySet()) {
                sb.append("-" + packageName);
                NLPScanSettings settings = this.mNLPPackages.get(packageName);
                sb.append(" type:" + settings.mScanType);
                sb.append(" delay:" + settings.mScanDelayMillis);
                sb.append("\n");
            }
        }
        sb.append("\nWifi scan command history\n");
        int scanlogNumber = 0;
        synchronized (this.mScanLogMap) {
            for (String key : this.mScanLogMap.keySet()) {
                ScanLog scanLog = this.mScanLogMap.get(key);
                if (scanLog != null) {
                    scanlogNumber++;
                    sb.append(scanlogNumber + ". ");
                    sb.append("PackageName : " + key);
                    sb.append("\n");
                    sb.append(scanLog.toString());
                    sb.append("\n");
                }
            }
        }
        sb.append("Counter ALL:");
        sb.append(this.mScanCounterAll);
        sb.append(" NLP:");
        sb.append(this.mScanCounterNlp);
        sb.append(" Cached:");
        sb.append(this.mScanCounterCached);
        sb.append(" 2.4GHz:");
        sb.append(this.mScanCounter2_4GHz);
        sb.append("\n");
        sb.append("Last request package:");
        sb.append(this.mLastRequestPackageName);
        sb.append("\n");
        return sb.toString();
    }

    private void updateNLPPackages() {
        synchronized (this.mNLPPackages) {
            this.mNLPPackages.clear();
            this.mNLPPackages.put("com.google.process.location", new NLPScanSettings(0, 0));
            this.mNLPPackages.put("com.google.android.location", new NLPScanSettings(0, 0));
            this.mNLPPackages.put("com.google.android.gms", new NLPScanSettings(0, 0));
            this.mNLPPackages.put(DEFALUT_NLP_PACKAGE_NAME, new NLPScanSettings(0, 0));
        }
    }

    private void addOrUpdateNLPPackageSetting(String packageName, int scanType, long scanDelay) {
        synchronized (this.mNLPPackages) {
            if (this.mNLPPackages.containsKey(packageName)) {
                this.mNLPPackages.remove(packageName);
            }
            if (scanType < 7) {
                this.mNLPPackages.put(packageName, new NLPScanSettings(scanType, scanDelay));
            } else {
                this.mNLPPackages.put(packageName, new NLPScanSettings(1, scanDelay));
            }
        }
    }

    private boolean isNLPPackage(String packageName) {
        if (packageName == null || getNLPPackageKey(packageName) == null) {
            return false;
        }
        return true;
    }

    private String getNLPPackageKey(String packageName) {
        if (packageName == null || packageName.length() <= 0) {
            return null;
        }
        synchronized (this.mNLPPackages) {
            for (String nlpPackageKey : this.mNLPPackages.keySet()) {
                if (packageName.startsWith(nlpPackageKey)) {
                    return nlpPackageKey;
                }
            }
            return null;
        }
    }

    private static class NLPScanSettings {
        public long mScanDelayMillis = 0;
        public int mScanType;

        public NLPScanSettings(int scanType, long scanDelay) {
            this.mScanType = scanType;
            this.mScanDelayMillis = scanDelay;
        }
    }
}
