package com.samsung.android.server.wifi.dqa;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SemWifiIssueDetector extends Handler {
    private static final int MAX_LIST_SIZE = (ActivityManager.isLowRamDeviceStatic() ? 100 : 500);
    public static final String TAG = "WifiIssueDetector";
    private final WifiIssueDetectorAdapter mAdapter;
    private final Context mContext;
    private final String mFilePath = "/data/misc/wifi/issue_detector.conf";
    private final List<Integer> mIssueReport = new ArrayList();
    private IExternalDiagnosticListener mListener;
    private Object mLogLock = new Object();
    private final List<ReportData> mLogs = new ArrayList();
    private final List<WifiIssuePattern> mPatterns = new ArrayList();

    public interface IExternalDiagnosticListener {
        void onReportAdded(int i);
    }

    public interface WifiIssueDetectorAdapter {
        void sendBigData(Bundle bundle);
    }

    public SemWifiIssueDetector(Context context, Looper workerLooper, WifiIssueDetectorAdapter adapter) {
        super(workerLooper);
        this.mContext = context;
        this.mAdapter = adapter;
        WifiIssuePattern pattern = new PatternWifiDisconnect();
        this.mPatterns.add(pattern);
        this.mIssueReport.addAll(pattern.getAssociatedKeys());
        WifiIssuePattern pattern2 = new PatternWifiConnecting();
        this.mPatterns.add(pattern2);
        this.mIssueReport.addAll(pattern2.getAssociatedKeys());
    }

    public void captureBugReport(int reportId, Bundle report) {
        sendMessage(obtainMessage(0, reportId, 0, report));
    }

    public void handleMessage(Message msg) {
        int reportId = msg.arg1;
        Bundle data = (Bundle) msg.obj;
        if (data != null) {
            report(reportId, data);
        }
    }

    private void report(int reportId, Bundle data) {
        ReportData reportData;
        if (reportId > 0) {
            if (data.containsKey(WifiIssuePattern.KEY_HUMAN_READABLE_TIME)) {
                data.remove(WifiIssuePattern.KEY_HUMAN_READABLE_TIME);
            }
            if (data.containsKey("time")) {
                long time = ((Long) WifiIssuePattern.getValue(data, "time", 0L)).longValue();
                data.remove("time");
                reportData = new ReportData(reportId, data, time);
            } else {
                reportData = new ReportData(reportId, data);
            }
            addLog(reportData);
            Log.d(TAG, "report " + reportData.toString());
            IExternalDiagnosticListener iExternalDiagnosticListener = this.mListener;
            if (iExternalDiagnosticListener != null) {
                iExternalDiagnosticListener.onReportAdded(reportId);
            }
            if (this.mIssueReport.contains(Integer.valueOf(reportId))) {
                attemptIssueDetection(reportId, reportData);
            }
        }
    }

    public void setExternalDiagnosticListener(IExternalDiagnosticListener listener) {
        this.mListener = listener;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SemWifiIssueDetector:");
        synchronized (this.mLogLock) {
            for (ReportData data : this.mLogs) {
                pw.println(data.toString());
            }
        }
        pw.println("SemWifiIssueDetectorHistory:");
        readFile(pw);
    }

    public String getRawData(int size) {
        int counter = 0;
        StringBuffer sb = new StringBuffer();
        synchronized (this.mLogLock) {
            try {
                int i = this.mLogs.size() - 1;
                while (true) {
                    if (i >= 0) {
                        int counter2 = counter + 1;
                        if (counter > size) {
                            counter = counter2;
                            break;
                        }
                        try {
                            sb.append(this.mLogs.get(i).toString());
                            sb.append("\n");
                            i--;
                            counter = counter2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                }
                return sb.toString();
            } catch (Throwable th2) {
                int i2 = counter;
                th = th2;
                throw th;
            }
        }
    }

    private void attemptIssueDetection(int reportId, ReportData reportData) {
        for (WifiIssuePattern pattern : this.mPatterns) {
            if (pattern.isAssociated(reportId, reportData)) {
                synchronized (this.mLogLock) {
                    if (pattern.matches(this.mLogs)) {
                        String patternId = pattern.getPatternId();
                        Log.i(TAG, "pattern matched! pid=" + patternId);
                        Bundle bigDataParams = pattern.getBigDataParams();
                        if (bigDataParams != null) {
                            bigDataParams.putString(ReportIdKey.KEY_PATTERN_ID, patternId);
                            this.mAdapter.sendBigData(bigDataParams);
                            sendBroadcastSecIssueDetected(bigDataParams.getString("feature"), patternId, bigDataParams.getInt(ReportIdKey.KEY_CATEGORY_ID, 0));
                            ReportData matchedReport = new ReportData(ReportIdKey.ID_PATTERN_MATCHED, (Bundle) bigDataParams.clone());
                            addLog(matchedReport);
                            writeLog(matchedReport.toString() + "\n");
                            IExternalDiagnosticListener iExternalDiagnosticListener = this.mListener;
                            if (iExternalDiagnosticListener != null) {
                                iExternalDiagnosticListener.onReportAdded(ReportIdKey.ID_PATTERN_MATCHED);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendBroadcastSecIssueDetected(String bigdataFeature, String patternId, int categoryId) {
        Intent intent = new Intent("com.samsung.android.net.wifi.ISSUE_DETECTED");
        intent.addFlags(67108864);
        intent.putExtra("bigdataFeature", bigdataFeature);
        intent.putExtra(ReportIdKey.KEY_PATTERN_ID, patternId);
        intent.putExtra(ReportIdKey.KEY_CATEGORY_ID, categoryId);
        sendBroadcastForCurrentUser(intent);
    }

    private void sendBroadcastForCurrentUser(Intent intent) {
        Context context = this.mContext;
        if (context != null) {
            try {
                context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Send broadcast before boot - action:" + intent.getAction());
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0039 A[SYNTHETIC, Splitter:B:24:0x0039] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void readFile(java.io.PrintWriter r6) {
        /*
            r5 = this;
            monitor-enter(r5)
            java.io.File r0 = new java.io.File     // Catch:{ all -> 0x0047 }
            java.lang.String r1 = "/data/misc/wifi/issue_detector.conf"
            r0.<init>(r1)     // Catch:{ all -> 0x0047 }
            r1 = 0
            boolean r2 = r0.exists()     // Catch:{ Exception -> 0x003f, all -> 0x0036 }
            if (r2 != 0) goto L_0x0019
            java.lang.String r2 = "not exist"
            r6.println(r2)     // Catch:{ Exception -> 0x0017, all -> 0x0015 }
            goto L_0x0019
        L_0x0015:
            r2 = move-exception
            goto L_0x0037
        L_0x0017:
            r2 = move-exception
            goto L_0x0040
        L_0x0019:
            java.io.RandomAccessFile r2 = new java.io.RandomAccessFile     // Catch:{ Exception -> 0x003f, all -> 0x0036 }
            java.lang.String r3 = "/data/misc/wifi/issue_detector.conf"
            java.lang.String r4 = "r"
            r2.<init>(r3, r4)     // Catch:{ Exception -> 0x003f, all -> 0x0036 }
            r1 = r2
            r2 = 0
        L_0x0024:
            java.lang.String r3 = r1.readLine()     // Catch:{ Exception -> 0x003f, all -> 0x0036 }
            r2 = r3
            if (r3 == 0) goto L_0x002f
            r6.println(r2)     // Catch:{ Exception -> 0x0017, all -> 0x0015 }
            goto L_0x0024
        L_0x002f:
            r1.close()     // Catch:{ Exception -> 0x0034 }
            goto L_0x0045
        L_0x0034:
            r2 = move-exception
            goto L_0x0045
        L_0x0036:
            r2 = move-exception
        L_0x0037:
            if (r1 == 0) goto L_0x003e
            r1.close()     // Catch:{ Exception -> 0x003d }
            goto L_0x003e
        L_0x003d:
            r3 = move-exception
        L_0x003e:
            throw r2     // Catch:{ all -> 0x0047 }
        L_0x003f:
            r2 = move-exception
        L_0x0040:
            if (r1 == 0) goto L_0x0045
            r1.close()     // Catch:{ Exception -> 0x0034 }
        L_0x0045:
            monitor-exit(r5)
            return
        L_0x0047:
            r6 = move-exception
            monitor-exit(r5)
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.dqa.SemWifiIssueDetector.readFile(java.io.PrintWriter):void");
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0045 A[SYNTHETIC, Splitter:B:24:0x0045] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void writeLog(java.lang.String r7) {
        /*
            r6 = this;
            monitor-enter(r6)
            java.io.File r0 = new java.io.File     // Catch:{ all -> 0x0053 }
            java.lang.String r1 = "/data/misc/wifi/issue_detector.conf"
            r0.<init>(r1)     // Catch:{ all -> 0x0053 }
            r1 = 0
            boolean r2 = r0.exists()     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            if (r2 != 0) goto L_0x0017
            r0.createNewFile()     // Catch:{ Exception -> 0x0015, all -> 0x0013 }
            goto L_0x0027
        L_0x0013:
            r2 = move-exception
            goto L_0x0043
        L_0x0015:
            r2 = move-exception
            goto L_0x004c
        L_0x0017:
            long r2 = r0.length()     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            r4 = 10000(0x2710, double:4.9407E-320)
            int r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1))
            if (r2 <= 0) goto L_0x0027
            r0.delete()     // Catch:{ Exception -> 0x0015, all -> 0x0013 }
            r0.createNewFile()     // Catch:{ Exception -> 0x0015, all -> 0x0013 }
        L_0x0027:
            java.io.RandomAccessFile r2 = new java.io.RandomAccessFile     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            java.lang.String r3 = "/data/misc/wifi/issue_detector.conf"
            java.lang.String r4 = "rw"
            r2.<init>(r3, r4)     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            r1 = r2
            long r2 = r1.length()     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            r1.seek(r2)     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            r1.writeBytes(r7)     // Catch:{ Exception -> 0x004b, all -> 0x0042 }
            r1.close()     // Catch:{ Exception -> 0x0040 }
            goto L_0x0051
        L_0x0040:
            r2 = move-exception
            goto L_0x0051
        L_0x0042:
            r2 = move-exception
        L_0x0043:
            if (r1 == 0) goto L_0x004a
            r1.close()     // Catch:{ Exception -> 0x0049 }
            goto L_0x004a
        L_0x0049:
            r3 = move-exception
        L_0x004a:
            throw r2     // Catch:{ all -> 0x0053 }
        L_0x004b:
            r2 = move-exception
        L_0x004c:
            if (r1 == 0) goto L_0x0051
            r1.close()     // Catch:{ Exception -> 0x0040 }
        L_0x0051:
            monitor-exit(r6)
            return
        L_0x0053:
            r7 = move-exception
            monitor-exit(r6)
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.dqa.SemWifiIssueDetector.writeLog(java.lang.String):void");
    }

    private void addLog(ReportData data) {
        synchronized (this.mLogLock) {
            if (this.mLogs.size() > MAX_LIST_SIZE) {
                this.mLogs.remove(0);
            }
            this.mLogs.add(data);
        }
    }
}
