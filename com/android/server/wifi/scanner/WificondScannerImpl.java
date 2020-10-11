package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;

public class WificondScannerImpl extends WifiScannerImpl implements Handler.Callback {
    private static final boolean DBG = false;
    private static final int MAX_APS_PER_SCAN = 32;
    public static final int MAX_HIDDEN_NETWORK_IDS_PER_SCAN = 16;
    private static final int MAX_SCAN_BUCKETS = 16;
    private static final int RETURN_CACHED_SCAN_RESULTS_EVENT = 1;
    private static final int SCAN_BUFFER_CAPACITY = 10;
    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final String TAG = "WificondScannerImpl";
    public static final String TIMEOUT_ALARM_TAG = "WificondScannerImpl Scan Timeout";
    private final AlarmManager mAlarmManager;
    private final ChannelHelper mChannelHelper;
    private final Clock mClock;
    private final Context mContext;
    private final Handler mEventHandler;
    private final boolean mHwPnoScanSupported;
    private final String mIfaceName;
    private ArrayList<ScanDetail> mLastNativeResults;
    private LastPnoScanSettings mLastPnoScanSettings = null;
    private final int[] mLastRssiDiff = {0, 0};
    private LastScanSettings mLastScanSettings = null;
    private WifiScanner.ScanData mLatestSingleScanResult = new WifiScanner.ScanData(0, 0, new ScanResult[0]);
    private final LocalLog mLocalLog = new LocalLog(128);
    private int mMaxNumScanSsids = -1;
    private ArrayList<ScanDetail> mNativePnoScanResults;
    private ArrayList<ScanDetail> mNativeScanResults;
    private int mNextHiddenNetworkScanId = 0;
    @GuardedBy("mSettingsLock")
    private AlarmManager.OnAlarmListener mScanTimeoutListener;
    private final Object mSettingsLock = new Object();
    private final WifiMonitor mWifiMonitor;
    private final WifiNative mWifiNative;

    public WificondScannerImpl(Context context, String ifaceName, WifiNative wifiNative, WifiMonitor wifiMonitor, ChannelHelper channelHelper, Looper looper, Clock clock) {
        this.mContext = context;
        this.mIfaceName = ifaceName;
        this.mWifiNative = wifiNative;
        this.mWifiMonitor = wifiMonitor;
        this.mChannelHelper = channelHelper;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mEventHandler = new Handler(looper, this);
        this.mClock = clock;
        this.mHwPnoScanSupported = this.mContext.getResources().getBoolean(17891593);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.PNO_SCAN_RESULTS_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
    }

    public void cleanup() {
        synchronized (this.mSettingsLock) {
            stopHwPnoScan();
            this.mLastScanSettings = null;
            this.mLastPnoScanSettings = null;
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.PNO_SCAN_RESULTS_EVENT, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
            this.mNextHiddenNetworkScanId = 0;
        }
    }

    public boolean getScanCapabilities(WifiNative.ScanCapabilities capabilities) {
        capabilities.max_scan_cache_size = Integer.MAX_VALUE;
        capabilities.max_scan_buckets = 16;
        capabilities.max_ap_cache_per_scan = 32;
        capabilities.max_rssi_sample_size = 8;
        capabilities.max_scan_reporting_threshold = 10;
        return true;
    }

    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:61:0x01cc, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startSingleScan(com.android.server.wifi.WifiNative.ScanSettings r24, com.android.server.wifi.WifiNative.ScanEventHandler r25) {
        /*
            r23 = this;
            r1 = r23
            r2 = r24
            r9 = r25
            r0 = 0
            if (r9 == 0) goto L_0x01d0
            if (r2 != 0) goto L_0x000d
            goto L_0x01d0
        L_0x000d:
            java.lang.Object r10 = r1.mSettingsLock
            monitor-enter(r10)
            com.android.server.wifi.scanner.WificondScannerImpl$LastScanSettings r3 = r1.mLastScanSettings     // Catch:{ all -> 0x01cd }
            if (r3 == 0) goto L_0x001d
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.String r4 = "A single scan is already running"
            android.util.Log.w(r3, r4)     // Catch:{ all -> 0x01cd }
            monitor-exit(r10)     // Catch:{ all -> 0x01cd }
            return r0
        L_0x001d:
            com.android.server.wifi.scanner.ChannelHelper r3 = r1.mChannelHelper     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.scanner.ChannelHelper$ChannelCollection r3 = r3.createChannelCollection()     // Catch:{ all -> 0x01cd }
            r11 = r3
            r3 = 0
            r4 = r0
            r12 = r3
        L_0x0027:
            int r3 = r2.num_buckets     // Catch:{ all -> 0x01cd }
            if (r4 >= r3) goto L_0x003c
            com.android.server.wifi.WifiNative$BucketSettings[] r3 = r2.buckets     // Catch:{ all -> 0x01cd }
            r3 = r3[r4]     // Catch:{ all -> 0x01cd }
            int r5 = r3.report_events     // Catch:{ all -> 0x01cd }
            r5 = r5 & 2
            if (r5 == 0) goto L_0x0036
            r12 = 1
        L_0x0036:
            r11.addChannels((com.android.server.wifi.WifiNative.BucketSettings) r3)     // Catch:{ all -> 0x01cd }
            int r4 = r4 + 1
            goto L_0x0027
        L_0x003c:
            java.util.ArrayList r3 = new java.util.ArrayList     // Catch:{ all -> 0x01cd }
            r3.<init>()     // Catch:{ all -> 0x01cd }
            r13 = r3
            com.android.server.wifi.WifiNative$HiddenNetwork[] r3 = r2.hiddenNetworks     // Catch:{ all -> 0x01cd }
            r14 = r3
            r15 = 1
            if (r14 == 0) goto L_0x0115
            int r3 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            if (r3 >= 0) goto L_0x00af
            com.android.server.wifi.WifiNative r3 = r1.mWifiNative     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r1.mIfaceName     // Catch:{ all -> 0x01cd }
            int r3 = r3.getMaxNumScanSsids(r4)     // Catch:{ all -> 0x01cd }
            r1.mMaxNumScanSsids = r3     // Catch:{ all -> 0x01cd }
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = "mMaxNumScanSsids : "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            int r5 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x01cd }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x01cd }
            int r3 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            if (r3 >= r15) goto L_0x0092
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = "driver supported max scan ssids num is "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            int r5 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = ". so reset to zero"
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x01cd }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x01cd }
            r1.mMaxNumScanSsids = r0     // Catch:{ all -> 0x01cd }
            goto L_0x00af
        L_0x0092:
            int r3 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            int r3 = r3 - r15
            r1.mMaxNumScanSsids = r3     // Catch:{ all -> 0x01cd }
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = "max hidden network ids per scan is "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            int r5 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x01cd }
            android.util.Log.d(r3, r4)     // Catch:{ all -> 0x01cd }
        L_0x00af:
            int r3 = r14.length     // Catch:{ all -> 0x01cd }
            int r4 = r1.mMaxNumScanSsids     // Catch:{ all -> 0x01cd }
            int r3 = java.lang.Math.min(r3, r4)     // Catch:{ all -> 0x01cd }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = "processPendingScans: hiddenNetworks length = "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            int r5 = r14.length     // Catch:{ all -> 0x01cd }
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = ", next HiddenNetwork scanId = "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            int r5 = r1.mNextHiddenNetworkScanId     // Catch:{ all -> 0x01cd }
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = ", numHiddenNetworks = "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            r4.append(r3)     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x01cd }
            r1.localLog(r4)     // Catch:{ all -> 0x01cd }
            int r4 = r14.length     // Catch:{ all -> 0x01cd }
            if (r3 == r4) goto L_0x00e5
            int r4 = r1.mNextHiddenNetworkScanId     // Catch:{ all -> 0x01cd }
            int r5 = r14.length     // Catch:{ all -> 0x01cd }
            if (r4 < r5) goto L_0x00e7
        L_0x00e5:
            r1.mNextHiddenNetworkScanId = r0     // Catch:{ all -> 0x01cd }
        L_0x00e7:
            int r4 = r1.mNextHiddenNetworkScanId     // Catch:{ all -> 0x01cd }
            r5 = r0
        L_0x00ea:
            int r6 = r14.length     // Catch:{ all -> 0x01cd }
            if (r4 >= r6) goto L_0x00fb
            if (r5 >= r3) goto L_0x00fb
            r6 = r14[r4]     // Catch:{ all -> 0x01cd }
            java.lang.String r6 = r6.ssid     // Catch:{ all -> 0x01cd }
            r13.add(r6)     // Catch:{ all -> 0x01cd }
            int r5 = r5 + 1
            int r4 = r4 + 1
            goto L_0x00ea
        L_0x00fb:
            int r5 = r13.size()     // Catch:{ all -> 0x01cd }
            if (r5 >= r3) goto L_0x0113
            int r6 = r14.length     // Catch:{ all -> 0x01cd }
            if (r4 < r6) goto L_0x0113
            r4 = 0
        L_0x0105:
            if (r5 >= r3) goto L_0x0113
            r6 = r14[r4]     // Catch:{ all -> 0x01cd }
            java.lang.String r6 = r6.ssid     // Catch:{ all -> 0x01cd }
            r13.add(r6)     // Catch:{ all -> 0x01cd }
            int r4 = r4 + 1
            int r5 = r5 + 1
            goto L_0x0105
        L_0x0113:
            r1.mNextHiddenNetworkScanId = r4     // Catch:{ all -> 0x01cd }
        L_0x0115:
            com.android.server.wifi.scanner.WificondScannerImpl$LastScanSettings r8 = new com.android.server.wifi.scanner.WificondScannerImpl$LastScanSettings     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.Clock r3 = r1.mClock     // Catch:{ all -> 0x01cd }
            long r4 = r3.getElapsedSinceBootMillis()     // Catch:{ all -> 0x01cd }
            r3 = r8
            r6 = r12
            r7 = r11
            r0 = r8
            r8 = r25
            r3.<init>(r4, r6, r7, r8)     // Catch:{ all -> 0x01cd }
            r1.mLastScanSettings = r0     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.scanner.WificondScannerImpl$LastScanSettings r0 = r1.mLastScanSettings     // Catch:{ all -> 0x01cd }
            int r3 = r2.use_cached_scan     // Catch:{ all -> 0x01cd }
            if (r3 != r15) goto L_0x0130
            r3 = r15
            goto L_0x0131
        L_0x0130:
            r3 = 0
        L_0x0131:
            r0.useCachedScan = r3     // Catch:{ all -> 0x01cd }
            r0 = 0
            boolean r3 = r11.isEmpty()     // Catch:{ all -> 0x01cd }
            if (r3 != 0) goto L_0x0193
            java.util.Set r3 = r11.getScanFreqs()     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.scanner.WificondScannerImpl$LastScanSettings r4 = r1.mLastScanSettings     // Catch:{ all -> 0x01cd }
            boolean r4 = r4.useCachedScan     // Catch:{ all -> 0x01cd }
            if (r4 == 0) goto L_0x0153
            r0 = 1
            android.os.Handler r4 = r1.mEventHandler     // Catch:{ all -> 0x01cd }
            android.os.Handler r5 = r1.mEventHandler     // Catch:{ all -> 0x01cd }
            android.os.Message r5 = r5.obtainMessage(r15)     // Catch:{ all -> 0x01cd }
            r6 = 1500(0x5dc, double:7.41E-321)
            r4.sendMessageDelayed(r5, r6)     // Catch:{ all -> 0x01cd }
            goto L_0x017a
        L_0x0153:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = "processPendingScans: freqs = "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            r4.append(r3)     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = ", hNetworkSSIDSet = "
            r4.append(r5)     // Catch:{ all -> 0x01cd }
            r4.append(r13)     // Catch:{ all -> 0x01cd }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x01cd }
            r1.localLog(r4)     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.WifiNative r4 = r1.mWifiNative     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = r1.mIfaceName     // Catch:{ all -> 0x01cd }
            int r6 = r2.scanType     // Catch:{ all -> 0x01cd }
            boolean r4 = r4.scan(r5, r6, r3, r13)     // Catch:{ all -> 0x01cd }
            r0 = r4
        L_0x017a:
            if (r0 != 0) goto L_0x019a
            java.lang.String r4 = "WificondScannerImpl"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x01cd }
            r5.<init>()     // Catch:{ all -> 0x01cd }
            java.lang.String r6 = "Failed to start scan, freqs="
            r5.append(r6)     // Catch:{ all -> 0x01cd }
            r5.append(r3)     // Catch:{ all -> 0x01cd }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x01cd }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x01cd }
            goto L_0x019a
        L_0x0193:
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.String r4 = "Failed to start scan because there is no available channel to scan"
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x01cd }
        L_0x019a:
            if (r0 == 0) goto L_0x01c1
            com.android.server.wifi.scanner.WificondScannerImpl$1 r3 = new com.android.server.wifi.scanner.WificondScannerImpl$1     // Catch:{ all -> 0x01cd }
            r3.<init>()     // Catch:{ all -> 0x01cd }
            r1.mScanTimeoutListener = r3     // Catch:{ all -> 0x01cd }
            android.app.AlarmManager r3 = r1.mAlarmManager     // Catch:{ all -> 0x01cd }
            r17 = 2
            com.android.server.wifi.Clock r4 = r1.mClock     // Catch:{ all -> 0x01cd }
            long r4 = r4.getElapsedSinceBootMillis()     // Catch:{ all -> 0x01cd }
            r6 = 15000(0x3a98, double:7.411E-320)
            long r18 = r4 + r6
            java.lang.String r20 = "WificondScannerImpl Scan Timeout"
            android.app.AlarmManager$OnAlarmListener r4 = r1.mScanTimeoutListener     // Catch:{ all -> 0x01cd }
            android.os.Handler r5 = r1.mEventHandler     // Catch:{ all -> 0x01cd }
            r16 = r3
            r21 = r4
            r22 = r5
            r16.set(r17, r18, r20, r21, r22)     // Catch:{ all -> 0x01cd }
            goto L_0x01cb
        L_0x01c1:
            android.os.Handler r3 = r1.mEventHandler     // Catch:{ all -> 0x01cd }
            com.android.server.wifi.scanner.WificondScannerImpl$2 r4 = new com.android.server.wifi.scanner.WificondScannerImpl$2     // Catch:{ all -> 0x01cd }
            r4.<init>()     // Catch:{ all -> 0x01cd }
            r3.post(r4)     // Catch:{ all -> 0x01cd }
        L_0x01cb:
            monitor-exit(r10)     // Catch:{ all -> 0x01cd }
            return r15
        L_0x01cd:
            r0 = move-exception
            monitor-exit(r10)     // Catch:{ all -> 0x01cd }
            throw r0
        L_0x01d0:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "Invalid arguments for startSingleScan: settings="
            r0.append(r3)
            r0.append(r2)
            java.lang.String r3 = ",eventHandler="
            r0.append(r3)
            r0.append(r9)
            java.lang.String r0 = r0.toString()
            java.lang.String r3 = "WificondScannerImpl"
            android.util.Log.w(r3, r0)
            r0 = 0
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.scanner.WificondScannerImpl.startSingleScan(com.android.server.wifi.WifiNative$ScanSettings, com.android.server.wifi.WifiNative$ScanEventHandler):boolean");
    }

    public WifiScanner.ScanData getLatestSingleScanResults() {
        return this.mLatestSingleScanResult;
    }

    public boolean startBatchedScan(WifiNative.ScanSettings settings, WifiNative.ScanEventHandler eventHandler) {
        Log.w(TAG, "startBatchedScan() is not supported");
        return false;
    }

    public void stopBatchedScan() {
        Log.w(TAG, "stopBatchedScan() is not supported");
    }

    public void pauseBatchedScan() {
        Log.w(TAG, "pauseBatchedScan() is not supported");
    }

    public void restartBatchedScan() {
        Log.w(TAG, "restartBatchedScan() is not supported");
    }

    /* access modifiers changed from: private */
    public void handleScanTimeout() {
        synchronized (this.mSettingsLock) {
            Log.e(TAG, "Timed out waiting for scan result from wificond");
            reportScanFailure();
            this.mScanTimeoutListener = null;
        }
    }

    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i != 1 && i != 147461) {
            switch (i) {
                case WifiMonitor.SCAN_FAILED_EVENT:
                    Log.w(TAG, "Scan failed");
                    cancelScanTimeout();
                    reportScanFailure();
                    break;
                case WifiMonitor.PNO_SCAN_RESULTS_EVENT:
                    pollLatestScanDataForPno();
                    break;
            }
        } else {
            cancelScanTimeout();
            pollLatestScanData();
        }
        return true;
    }

    private void cancelScanTimeout() {
        synchronized (this.mSettingsLock) {
            if (this.mScanTimeoutListener != null) {
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                this.mScanTimeoutListener = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void reportScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                if (this.mLastScanSettings.singleScanEventHandler != null) {
                    this.mLastScanSettings.singleScanEventHandler.onScanStatus(3);
                }
                this.mLastScanSettings = null;
            }
        }
    }

    private void reportPnoScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                if (this.mLastPnoScanSettings.pnoScanEventHandler != null) {
                    this.mLastPnoScanSettings.pnoScanEventHandler.onPnoScanFailed();
                }
                this.mLastPnoScanSettings = null;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x007c, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void pollLatestScanDataForPno() {
        /*
            r9 = this;
            java.lang.Object r0 = r9.mSettingsLock
            monitor-enter(r0)
            com.android.server.wifi.scanner.WificondScannerImpl$LastPnoScanSettings r1 = r9.mLastPnoScanSettings     // Catch:{ all -> 0x007d }
            if (r1 != 0) goto L_0x0009
            monitor-exit(r0)     // Catch:{ all -> 0x007d }
            return
        L_0x0009:
            com.android.server.wifi.WifiNative r1 = r9.mWifiNative     // Catch:{ all -> 0x007d }
            java.lang.String r2 = r9.mIfaceName     // Catch:{ all -> 0x007d }
            java.util.ArrayList r1 = r1.getPnoScanResults(r2)     // Catch:{ all -> 0x007d }
            r9.mNativePnoScanResults = r1     // Catch:{ all -> 0x007d }
            java.util.ArrayList r1 = new java.util.ArrayList     // Catch:{ all -> 0x007d }
            r1.<init>()     // Catch:{ all -> 0x007d }
            r2 = 0
            r3 = 0
        L_0x001a:
            java.util.ArrayList<com.android.server.wifi.ScanDetail> r4 = r9.mNativePnoScanResults     // Catch:{ all -> 0x007d }
            int r4 = r4.size()     // Catch:{ all -> 0x007d }
            if (r3 >= r4) goto L_0x0044
            java.util.ArrayList<com.android.server.wifi.ScanDetail> r4 = r9.mNativePnoScanResults     // Catch:{ all -> 0x007d }
            java.lang.Object r4 = r4.get(r3)     // Catch:{ all -> 0x007d }
            com.android.server.wifi.ScanDetail r4 = (com.android.server.wifi.ScanDetail) r4     // Catch:{ all -> 0x007d }
            android.net.wifi.ScanResult r4 = r4.getScanResult()     // Catch:{ all -> 0x007d }
            long r5 = r4.timestamp     // Catch:{ all -> 0x007d }
            r7 = 1000(0x3e8, double:4.94E-321)
            long r5 = r5 / r7
            com.android.server.wifi.scanner.WificondScannerImpl$LastPnoScanSettings r7 = r9.mLastPnoScanSettings     // Catch:{ all -> 0x007d }
            long r7 = r7.startTime     // Catch:{ all -> 0x007d }
            int r7 = (r5 > r7 ? 1 : (r5 == r7 ? 0 : -1))
            if (r7 <= 0) goto L_0x003f
            r1.add(r4)     // Catch:{ all -> 0x007d }
            goto L_0x0041
        L_0x003f:
            int r2 = r2 + 1
        L_0x0041:
            int r3 = r3 + 1
            goto L_0x001a
        L_0x0044:
            if (r2 == 0) goto L_0x0061
            java.lang.String r3 = "WificondScannerImpl"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x007d }
            r4.<init>()     // Catch:{ all -> 0x007d }
            java.lang.String r5 = "Filtering out "
            r4.append(r5)     // Catch:{ all -> 0x007d }
            r4.append(r2)     // Catch:{ all -> 0x007d }
            java.lang.String r5 = " pno scan results."
            r4.append(r5)     // Catch:{ all -> 0x007d }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x007d }
            android.util.Log.d(r3, r4)     // Catch:{ all -> 0x007d }
        L_0x0061:
            com.android.server.wifi.scanner.WificondScannerImpl$LastPnoScanSettings r3 = r9.mLastPnoScanSettings     // Catch:{ all -> 0x007d }
            com.android.server.wifi.WifiNative$PnoEventHandler r3 = r3.pnoScanEventHandler     // Catch:{ all -> 0x007d }
            if (r3 == 0) goto L_0x007b
            int r3 = r1.size()     // Catch:{ all -> 0x007d }
            android.net.wifi.ScanResult[] r3 = new android.net.wifi.ScanResult[r3]     // Catch:{ all -> 0x007d }
            java.lang.Object[] r3 = r1.toArray(r3)     // Catch:{ all -> 0x007d }
            android.net.wifi.ScanResult[] r3 = (android.net.wifi.ScanResult[]) r3     // Catch:{ all -> 0x007d }
            com.android.server.wifi.scanner.WificondScannerImpl$LastPnoScanSettings r4 = r9.mLastPnoScanSettings     // Catch:{ all -> 0x007d }
            com.android.server.wifi.WifiNative$PnoEventHandler r4 = r4.pnoScanEventHandler     // Catch:{ all -> 0x007d }
            r4.onPnoNetworkFound(r3)     // Catch:{ all -> 0x007d }
        L_0x007b:
            monitor-exit(r0)     // Catch:{ all -> 0x007d }
            return
        L_0x007d:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x007d }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.scanner.WificondScannerImpl.pollLatestScanDataForPno():void");
    }

    public void updateTimeStampForCachedScanResult(ArrayList<ScanDetail> scanResults) {
        int index = 0;
        long currentTime = System.currentTimeMillis() + 800;
        int rssiDiff = ((int) (currentTime % 7)) - 3;
        if (rssiDiff == this.mLastRssiDiff[0]) {
            rssiDiff--;
        }
        int rssiDiff2 = ((int) ((currentTime / 2) % 7)) - 3;
        if (rssiDiff2 == this.mLastRssiDiff[1]) {
            rssiDiff2++;
        }
        Iterator<ScanDetail> it = scanResults.iterator();
        while (it.hasNext()) {
            ScanResult result = it.next().getScanResult();
            if (result != null) {
                result.timestamp = SystemClock.elapsedRealtime() * 1000;
                result.seen = currentTime;
                if (index % 2 == 0) {
                    result.level = (result.level - this.mLastRssiDiff[0]) + rssiDiff;
                } else {
                    result.level = (result.level - this.mLastRssiDiff[1]) + rssiDiff2;
                }
                index++;
            }
        }
        int[] iArr = this.mLastRssiDiff;
        iArr[0] = rssiDiff;
        iArr[1] = rssiDiff2;
    }

    private static int getBandScanned(ChannelHelper.ChannelCollection channelCollection) {
        if (channelCollection.containsBand(7)) {
            return 7;
        }
        if (channelCollection.containsBand(3)) {
            return 3;
        }
        if (channelCollection.containsBand(6)) {
            return 6;
        }
        if (channelCollection.containsBand(2)) {
            return 2;
        }
        if (channelCollection.containsBand(4)) {
            return 4;
        }
        if (channelCollection.containsBand(1)) {
            return 1;
        }
        return 0;
    }

    private void pollLatestScanData() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                if (this.mLastNativeResults == null || !this.mLastScanSettings.useCachedScan) {
                    this.mLastNativeResults = this.mWifiNative.getScanResults(this.mIfaceName);
                } else {
                    updateTimeStampForCachedScanResult(this.mLastNativeResults);
                }
                List<ScanResult> singleScanResults = new ArrayList<>();
                int numFilteredScanResults = 0;
                for (int i = 0; i < this.mLastNativeResults.size(); i++) {
                    ScanResult result = this.mLastNativeResults.get(i).getScanResult();
                    if (result.timestamp / 1000 <= this.mLastScanSettings.startTime) {
                        numFilteredScanResults++;
                    } else if (this.mLastScanSettings.singleScanFreqs.containsChannel(result.frequency)) {
                        singleScanResults.add(result);
                    }
                }
                if (numFilteredScanResults != 0) {
                    Log.d(TAG, "Filtering out " + numFilteredScanResults + " scan results.");
                }
                if (this.mLastScanSettings.singleScanEventHandler != null) {
                    if (this.mLastScanSettings.reportSingleScanFullResults) {
                        for (ScanResult scanResult : singleScanResults) {
                            this.mLastScanSettings.singleScanEventHandler.onFullScanResult(scanResult, 0);
                        }
                    }
                    Collections.sort(singleScanResults, SCAN_RESULT_SORT_COMPARATOR);
                    this.mLatestSingleScanResult = new WifiScanner.ScanData(0, 0, 0, getBandScanned(this.mLastScanSettings.singleScanFreqs), (ScanResult[]) singleScanResults.toArray(new ScanResult[singleScanResults.size()]));
                    this.mLastScanSettings.singleScanEventHandler.onScanStatus(0);
                }
                this.mLastScanSettings = null;
            }
        }
    }

    public WifiScanner.ScanData[] getLatestBatchedScanResults(boolean flush) {
        return null;
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private boolean startHwPnoScan(WifiNative.PnoSettings pnoSettings) {
        return this.mWifiNative.startPnoScan(this.mIfaceName, pnoSettings);
    }

    private void stopHwPnoScan() {
        this.mWifiNative.stopPnoScan(this.mIfaceName);
    }

    private boolean isHwPnoScanRequired(boolean isConnectedPno) {
        return !isConnectedPno && this.mHwPnoScanSupported;
    }

    public boolean setHwPnoList(WifiNative.PnoSettings settings, WifiNative.PnoEventHandler eventHandler) {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                Log.w(TAG, "Already running a PNO scan");
                return false;
            } else if (!isHwPnoScanRequired(settings.isConnected)) {
                return false;
            } else {
                if (startHwPnoScan(settings)) {
                    this.mLastPnoScanSettings = new LastPnoScanSettings(this.mClock.getElapsedSinceBootMillis(), settings.networkList, eventHandler);
                } else {
                    Log.e(TAG, "Failed to start PNO scan");
                    reportPnoScanFailure();
                }
                return true;
            }
        }
    }

    public boolean resetHwPnoList() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings == null) {
                Log.w(TAG, "No PNO scan running");
                return false;
            }
            this.mLastPnoScanSettings = null;
            stopHwPnoScan();
            return true;
        }
    }

    public boolean isHwPnoSupported(boolean isConnectedPno) {
        return isHwPnoScanRequired(isConnectedPno);
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mSettingsLock) {
            long nowMs = this.mClock.getElapsedSinceBootMillis();
            pw.println("Latest native scan results:");
            if (this.mLastNativeResults != null) {
                ScanResultUtil.dumpScanResults(pw, (List) this.mLastNativeResults.stream().map($$Lambda$WificondScannerImpl$CSjtYSyNiQ_mC6mOyQ4GpkylqY.INSTANCE).collect(Collectors.toList()), nowMs);
            }
            pw.println("Latest native pno scan results:");
            if (this.mNativePnoScanResults != null) {
                ScanResultUtil.dumpScanResults(pw, (List) this.mNativePnoScanResults.stream().map($$Lambda$WificondScannerImpl$VfxaUtYlcuU7Z28abhvk42O2k.INSTANCE).collect(Collectors.toList()), nowMs);
            }
            pw.println("WificondScannerImpl - Log Begin ----");
            this.mLocalLog.dump(fd, pw, args);
            pw.println("WificondScannerImpl - Log End ----");
        }
    }

    private static class LastScanSettings {
        public boolean reportSingleScanFullResults;
        public WifiNative.ScanEventHandler singleScanEventHandler;
        public ChannelHelper.ChannelCollection singleScanFreqs;
        public long startTime;
        public boolean useCachedScan;

        LastScanSettings(long startTime2, boolean reportSingleScanFullResults2, ChannelHelper.ChannelCollection singleScanFreqs2, WifiNative.ScanEventHandler singleScanEventHandler2) {
            this.startTime = startTime2;
            this.reportSingleScanFullResults = reportSingleScanFullResults2;
            this.singleScanFreqs = singleScanFreqs2;
            this.singleScanEventHandler = singleScanEventHandler2;
        }
    }

    private static class LastPnoScanSettings {
        public WifiNative.PnoNetwork[] pnoNetworkList;
        public WifiNative.PnoEventHandler pnoScanEventHandler;
        public long startTime;

        LastPnoScanSettings(long startTime2, WifiNative.PnoNetwork[] pnoNetworkList2, WifiNative.PnoEventHandler pnoScanEventHandler2) {
            this.startTime = startTime2;
            this.pnoNetworkList = pnoNetworkList2;
            this.pnoScanEventHandler = pnoScanEventHandler2;
        }
    }
}
