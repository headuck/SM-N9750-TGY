package com.android.server.wifi;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.SparseLongArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.ByteArrayRingBuffer;
import com.android.server.wifi.util.StringUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

class WifiDiagnostics extends BaseWifiDiagnostics {
    private static final boolean DBG = false;
    @VisibleForTesting
    public static final String DRIVER_DUMP_SECTION_HEADER = "Driver state dump";
    @VisibleForTesting
    public static final String FIRMWARE_DUMP_SECTION_HEADER = "FW Memory dump";
    public static final int MAX_ALERT_REPORTS = 1;
    public static final int MAX_BUG_REPORTS = 4;
    public static final long MIN_DUMP_TIME_WINDOW_MILLIS = 600000;
    private static final int[] MinBufferSizes = {0, 16384, 16384, 65536};
    private static final int[] MinWakeupIntervals = {0, 3600, 60, 10};
    public static final int REPORT_REASON_ASSOC_FAILURE = 1;
    public static final int REPORT_REASON_AUTH_FAILURE = 2;
    public static final int REPORT_REASON_AUTOROAM_FAILURE = 3;
    public static final int REPORT_REASON_DHCP_FAILURE = 4;
    public static final int REPORT_REASON_FATAL_FW_ALERT = 10;
    public static final int REPORT_REASON_NONE = 0;
    public static final int REPORT_REASON_REACHABILITY_LOST = 9;
    public static final int REPORT_REASON_SCAN_FAILURE = 6;
    public static final int REPORT_REASON_UNEXPECTED_DISCONNECT = 5;
    public static final int REPORT_REASON_USER_ACTION = 7;
    public static final int REPORT_REASON_WIFINATIVE_FAILURE = 8;
    public static final int RING_BUFFER_FLAG_HAS_ASCII_ENTRIES = 2;
    public static final int RING_BUFFER_FLAG_HAS_BINARY_ENTRIES = 1;
    public static final int RING_BUFFER_FLAG_HAS_PER_PACKET_ENTRIES = 4;
    private static final String TAG = "WifiDiags";
    public static final int VERBOSE_DETAILED_LOG_WITH_WAKEUP = 3;
    public static final int VERBOSE_LOG_WITH_WAKEUP = 2;
    public static final int VERBOSE_NORMAL_LOG = 1;
    public static final int VERBOSE_NO_LOG = 0;
    private final int RING_BUFFER_BYTE_LIMIT_LARGE;
    private final int RING_BUFFER_BYTE_LIMIT_SMALL;
    private final BuildProperties mBuildProperties;
    private Clock mClock;
    private final List<Integer> mFatalFirmwareAlertErrorCodeList;
    private final WifiNative.WifiLoggerEventHandler mHandler = new WifiNative.WifiLoggerEventHandler() {
        public void onRingBufferData(WifiNative.RingBufferStatus status, byte[] buffer) {
            WifiDiagnostics.this.onRingBufferData(status, buffer);
        }

        public void onWifiAlert(int errorCode, byte[] buffer) {
            WifiDiagnostics.this.onWifiAlert(errorCode, buffer);
        }
    };
    private boolean mIsLoggingEventHandlerRegistered;
    private final Runtime mJavaRuntime;
    private final LimitedCircularArray<BugReport> mLastAlerts = new LimitedCircularArray<>(1);
    private final LimitedCircularArray<BugReport> mLastBugReports = new LimitedCircularArray<>(4);
    private final SparseLongArray mLastDumpTime = new SparseLongArray();
    private final LastMileLogger mLastMileLogger;
    private final WifiLog mLog;
    private int mLogLevel = 0;
    private int mMaxRingBufferSizeBytes;
    private ArrayList<WifiNative.FateReport> mPacketFatesForLastFailure;
    private WifiNative.RingBufferStatus mPerPacketRingBuffer;
    private final HashMap<String, ByteArrayRingBuffer> mRingBufferData = new HashMap<>();
    private WifiNative.RingBufferStatus[] mRingBuffers;
    private WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;
    private final Handler mWorkerThreadHandler;

    public WifiDiagnostics(Context context, WifiInjector wifiInjector, WifiNative wifiNative, BuildProperties buildProperties, LastMileLogger lastMileLogger, Clock clock, Looper workerLooper) {
        super(wifiNative);
        this.RING_BUFFER_BYTE_LIMIT_SMALL = context.getResources().getInteger(17695012) * 1024;
        this.RING_BUFFER_BYTE_LIMIT_LARGE = context.getResources().getInteger(17695013) * 1024;
        int[] fatalFirmwareAlertErrorCodeArray = context.getResources().getIntArray(17236260);
        this.mBuildProperties = buildProperties;
        this.mIsLoggingEventHandlerRegistered = false;
        this.mMaxRingBufferSizeBytes = this.RING_BUFFER_BYTE_LIMIT_SMALL;
        this.mFatalFirmwareAlertErrorCodeList = (List) Arrays.stream(fatalFirmwareAlertErrorCodeArray).boxed().collect(Collectors.toList());
        this.mLog = wifiInjector.makeLog(TAG);
        this.mLastMileLogger = lastMileLogger;
        this.mJavaRuntime = wifiInjector.getJavaRuntime();
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mWifiInjector = wifiInjector;
        this.mClock = clock;
        this.mWorkerThreadHandler = new Handler(workerLooper);
    }

    public synchronized void startLogging(boolean verboseEnabled) {
        this.mFirmwareVersion = this.mWifiNative.getFirmwareVersion();
        this.mDriverVersion = this.mWifiNative.getDriverVersion();
        this.mSupportedFeatureSet = this.mWifiNative.getSupportedLoggerFeatureSet();
        if (!this.mIsLoggingEventHandlerRegistered) {
            this.mIsLoggingEventHandlerRegistered = this.mWifiNative.setLoggingEventHandler(this.mHandler);
        }
        if (verboseEnabled) {
            this.mLogLevel = 2;
            this.mMaxRingBufferSizeBytes = this.RING_BUFFER_BYTE_LIMIT_LARGE;
        } else {
            this.mLogLevel = 1;
            this.mMaxRingBufferSizeBytes = enableVerboseLoggingForDogfood() ? this.RING_BUFFER_BYTE_LIMIT_LARGE : this.RING_BUFFER_BYTE_LIMIT_SMALL;
            clearVerboseLogs();
        }
        if (this.mRingBuffers == null) {
            fetchRingBuffers();
        }
        if (this.mRingBuffers != null) {
            stopLoggingAllBuffers();
            resizeRingBuffers();
            startLoggingAllExceptPerPacketBuffers();
        }
        if (!this.mWifiNative.startPktFateMonitoring(this.mWifiNative.getClientInterfaceName())) {
            this.mLog.mo2097wC("Failed to start packet fate monitoring");
        }
    }

    public synchronized void startPacketLog() {
        if (this.mPerPacketRingBuffer != null) {
            startLoggingRingBuffer(this.mPerPacketRingBuffer);
        }
    }

    public synchronized void stopPacketLog() {
        if (this.mPerPacketRingBuffer != null) {
            stopLoggingRingBuffer(this.mPerPacketRingBuffer);
        }
    }

    public synchronized void stopLogging() {
        if (this.mIsLoggingEventHandlerRegistered) {
            if (!this.mWifiNative.resetLogHandler()) {
                this.mLog.mo2097wC("Fail to reset log handler");
            }
            this.mIsLoggingEventHandlerRegistered = false;
        }
        if (this.mLogLevel != 0) {
            stopLoggingAllBuffers();
            this.mRingBuffers = null;
            this.mLogLevel = 0;
        }
    }

    public synchronized void reportConnectionEvent(byte event) {
        this.mLastMileLogger.reportConnectionEvent(event);
        if (event == 2 || event == 3) {
            this.mPacketFatesForLastFailure = fetchPacketFates();
        }
    }

    /* renamed from: captureBugReportData */
    public synchronized void lambda$triggerBugReportDataCapture$0$WifiDiagnostics(int reason) {
        this.mLastBugReports.addLast(captureBugreport(reason, isVerboseLoggingEnabled()));
        flushDump(reason);
    }

    public void triggerBugReportDataCapture(int reason) {
        this.mWorkerThreadHandler.post(new Runnable(reason) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                WifiDiagnostics.this.lambda$triggerBugReportDataCapture$0$WifiDiagnostics(this.f$1);
            }
        });
    }

    private void triggerAlertDataCapture(int errorCode, byte[] alertData) {
        this.mWorkerThreadHandler.post(new Runnable(errorCode, alertData) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ byte[] f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                WifiDiagnostics.this.lambda$triggerAlertDataCapture$1$WifiDiagnostics(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$triggerAlertDataCapture$1$WifiDiagnostics(int errorCode, byte[] alertData) {
        synchronized (this) {
            BugReport report = captureBugreport(errorCode, isVerboseLoggingEnabled());
            report.alertData = alertData;
            this.mLastAlerts.addLast(report);
            if (this.mFatalFirmwareAlertErrorCodeList.contains(Integer.valueOf(errorCode))) {
                flushDump(10);
            }
        }
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(pw);
        for (int i = 0; i < this.mLastAlerts.size(); i++) {
            pw.println("--------------------------------------------------------------------");
            pw.println("Alert dump " + i);
            pw.print(this.mLastAlerts.get(i));
            pw.println("--------------------------------------------------------------------");
        }
        for (int i2 = 0; i2 < this.mLastBugReports.size(); i2++) {
            pw.println("--------------------------------------------------------------------");
            pw.println("Bug dump " + i2);
            pw.print(this.mLastBugReports.get(i2));
            pw.println("--------------------------------------------------------------------");
        }
        pw.println("Last Flush Time: " + this.mLastDumpTime.toString());
        pw.println("--------------------------------------------------------------------");
        dumpPacketFates(pw);
        this.mLastMileLogger.dump(pw);
        pw.println("--------------------------------------------------------------------");
    }

    public void takeBugReport(String bugTitle, String bugDetail) {
        if (!this.mBuildProperties.isUserBuild()) {
            try {
                this.mWifiInjector.getActivityManagerService().requestWifiBugReport(bugTitle, bugDetail);
            } catch (Exception e) {
                this.mLog.err("error taking bugreport: %").mo2070c(e.getClass().getName()).flush();
            }
        }
    }

    class BugReport {
        byte[] alertData;
        int errorCode;
        byte[] fwMemoryDump;
        LimitedCircularArray<String> kernelLogLines;
        long kernelTimeNanos;
        ArrayList<String> logcatLines;
        byte[] mDriverStateDump;
        HashMap<String, byte[][]> ringBuffers = new HashMap<>();
        long systemTimeMs;

        BugReport() {
        }

        /* access modifiers changed from: package-private */
        public void clearVerboseLogs() {
            this.fwMemoryDump = null;
            this.mDriverStateDump = null;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(this.systemTimeMs);
            builder.append("system time = ");
            builder.append(String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c}));
            builder.append("\n");
            long kernelTimeMs = this.kernelTimeNanos / 1000000;
            builder.append("kernel time = ");
            builder.append(kernelTimeMs / 1000);
            builder.append(".");
            builder.append(kernelTimeMs % 1000);
            builder.append("\n");
            if (this.alertData == null) {
                builder.append("reason = ");
                builder.append(this.errorCode);
                builder.append("\n");
            } else {
                builder.append("errorCode = ");
                builder.append(this.errorCode);
                builder.append("data \n");
                builder.append(WifiDiagnostics.this.compressToBase64(this.alertData));
                builder.append("\n");
            }
            if (this.kernelLogLines != null) {
                builder.append("kernel log: \n");
                for (int i = 0; i < this.kernelLogLines.size(); i++) {
                    builder.append(this.kernelLogLines.get(i));
                    builder.append("\n");
                }
                builder.append("\n");
            }
            if (this.logcatLines != null) {
                builder.append("system log: \n");
                for (int i2 = 0; i2 < this.logcatLines.size(); i2++) {
                    builder.append(this.logcatLines.get(i2));
                    builder.append("\n");
                }
                builder.append("\n");
            }
            for (Map.Entry<String, byte[][]> e : this.ringBuffers.entrySet()) {
                byte[][] buffers = e.getValue();
                builder.append("ring-buffer = ");
                builder.append(e.getKey());
                builder.append("\n");
                int size = 0;
                for (byte[] length : buffers) {
                    size += length.length;
                }
                byte[] buffer = new byte[size];
                int index = 0;
                int i3 = 0;
                while (i3 < buffers.length) {
                    System.arraycopy(buffers[i3], 0, buffer, index, buffers[i3].length);
                    index += buffers[i3].length;
                    i3++;
                    c = c;
                }
                builder.append(WifiDiagnostics.this.compressToBase64(buffer));
                builder.append("\n");
                c = c;
            }
            if (this.fwMemoryDump != null) {
                builder.append(WifiDiagnostics.FIRMWARE_DUMP_SECTION_HEADER);
                builder.append("\n");
                builder.append(WifiDiagnostics.this.compressToBase64(this.fwMemoryDump));
                builder.append("\n");
            }
            if (this.mDriverStateDump != null) {
                builder.append(WifiDiagnostics.DRIVER_DUMP_SECTION_HEADER);
                if (StringUtil.isAsciiPrintable(this.mDriverStateDump)) {
                    builder.append(" (ascii)\n");
                    builder.append(new String(this.mDriverStateDump, Charset.forName("US-ASCII")));
                    builder.append("\n");
                } else {
                    builder.append(" (base64)\n");
                    builder.append(WifiDiagnostics.this.compressToBase64(this.mDriverStateDump));
                }
            }
            return builder.toString();
        }
    }

    class LimitedCircularArray<E> {
        private ArrayList<E> mArrayList;
        private int mMax;

        LimitedCircularArray(int max) {
            this.mArrayList = new ArrayList<>(max);
            this.mMax = max;
        }

        public final void addLast(E e) {
            if (this.mArrayList.size() >= this.mMax) {
                this.mArrayList.remove(0);
            }
            this.mArrayList.add(e);
        }

        public final int size() {
            return this.mArrayList.size();
        }

        public final E get(int i) {
            return this.mArrayList.get(i);
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void onRingBufferData(WifiNative.RingBufferStatus status, byte[] buffer) {
        ByteArrayRingBuffer ring = this.mRingBufferData.get(status.name);
        if (ring != null) {
            ring.appendBuffer(buffer);
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void onWifiAlert(int errorCode, byte[] buffer) {
        triggerAlertDataCapture(errorCode, buffer);
        this.mWifiMetrics.logFirmwareAlert(errorCode);
    }

    private boolean isVerboseLoggingEnabled() {
        return this.mLogLevel > 1;
    }

    private void clearVerboseLogs() {
        this.mPacketFatesForLastFailure = null;
        for (int i = 0; i < this.mLastAlerts.size(); i++) {
            this.mLastAlerts.get(i).clearVerboseLogs();
        }
        for (int i2 = 0; i2 < this.mLastBugReports.size(); i2++) {
            this.mLastBugReports.get(i2).clearVerboseLogs();
        }
    }

    private boolean fetchRingBuffers() {
        if (this.mRingBuffers != null) {
            return true;
        }
        this.mRingBuffers = this.mWifiNative.getRingBufferStatus();
        WifiNative.RingBufferStatus[] ringBufferStatusArr = this.mRingBuffers;
        if (ringBufferStatusArr != null) {
            for (WifiNative.RingBufferStatus buffer : ringBufferStatusArr) {
                if (!this.mRingBufferData.containsKey(buffer.name)) {
                    this.mRingBufferData.put(buffer.name, new ByteArrayRingBuffer(this.mMaxRingBufferSizeBytes));
                }
                if ((buffer.flag & 4) != 0) {
                    this.mPerPacketRingBuffer = buffer;
                }
            }
        } else {
            this.mLog.mo2097wC("no ring buffers found");
        }
        if (this.mRingBuffers != null) {
            return true;
        }
        return false;
    }

    private void resizeRingBuffers() {
        for (ByteArrayRingBuffer byteArrayRingBuffer : this.mRingBufferData.values()) {
            byteArrayRingBuffer.resize(this.mMaxRingBufferSizeBytes);
        }
    }

    private boolean startLoggingAllExceptPerPacketBuffers() {
        WifiNative.RingBufferStatus[] ringBufferStatusArr = this.mRingBuffers;
        if (ringBufferStatusArr == null) {
            return false;
        }
        for (WifiNative.RingBufferStatus buffer : ringBufferStatusArr) {
            if ((buffer.flag & 4) == 0) {
                startLoggingRingBuffer(buffer);
            }
        }
        return true;
    }

    private boolean startLoggingRingBuffer(WifiNative.RingBufferStatus buffer) {
        int[] iArr = MinWakeupIntervals;
        int i = this.mLogLevel;
        int minInterval = iArr[i];
        if (!this.mWifiNative.startLoggingRingBuffer(this.mLogLevel, 0, minInterval, MinBufferSizes[i], buffer.name)) {
            return false;
        }
        return true;
    }

    private boolean stopLoggingRingBuffer(WifiNative.RingBufferStatus buffer) {
        this.mWifiNative.startLoggingRingBuffer(0, 0, 0, 0, buffer.name);
        return true;
    }

    private boolean stopLoggingAllBuffers() {
        WifiNative.RingBufferStatus[] ringBufferStatusArr = this.mRingBuffers;
        if (ringBufferStatusArr == null) {
            return true;
        }
        for (WifiNative.RingBufferStatus buffer : ringBufferStatusArr) {
            stopLoggingRingBuffer(buffer);
        }
        return true;
    }

    private boolean enableVerboseLoggingForDogfood() {
        return true;
    }

    private boolean flushDump(int errorCode) {
        if (errorCode == 7) {
            return false;
        }
        long currentTime = this.mClock.getWallClockMillis();
        int index = this.mLastDumpTime.indexOfKey(errorCode);
        if (index >= 0 && currentTime - this.mLastDumpTime.valueAt(index) < MIN_DUMP_TIME_WINDOW_MILLIS) {
            return false;
        }
        if (!this.mWifiNative.flushRingBufferData()) {
            this.mLog.mo2097wC("could not flush ringbuffer");
            return false;
        }
        this.mLastDumpTime.put(errorCode, currentTime);
        return true;
    }

    private BugReport captureBugreport(int errorCode, boolean captureFWDump) {
        BugReport report = new BugReport();
        report.errorCode = errorCode;
        report.systemTimeMs = System.currentTimeMillis();
        report.kernelTimeNanos = System.nanoTime();
        WifiNative.RingBufferStatus[] ringBufferStatusArr = this.mRingBuffers;
        if (ringBufferStatusArr != null) {
            for (WifiNative.RingBufferStatus buffer : ringBufferStatusArr) {
                this.mWifiNative.getRingBufferData(buffer.name);
                ByteArrayRingBuffer data = this.mRingBufferData.get(buffer.name);
                byte[][] buffers = new byte[data.getNumBuffers()][];
                for (int i = 0; i < data.getNumBuffers(); i++) {
                    buffers[i] = (byte[]) data.getBuffer(i).clone();
                }
                report.ringBuffers.put(buffer.name, buffers);
            }
        }
        report.logcatLines = getLogcat(127);
        report.kernelLogLines = getKernelLog(127);
        if (captureFWDump) {
            report.fwMemoryDump = this.mWifiNative.getFwMemoryDump();
            report.mDriverStateDump = this.mWifiNative.getDriverStateDump();
        }
        return report;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public synchronized LimitedCircularArray<BugReport> getBugReports() {
        return this.mLastBugReports;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public synchronized LimitedCircularArray<BugReport> getAlertReports() {
        return this.mLastAlerts;
    }

    /* access modifiers changed from: private */
    public String compressToBase64(byte[] input) {
        Deflater compressor = new Deflater();
        compressor.setLevel(1);
        compressor.setInput(input);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            bos.write(buf, 0, compressor.deflate(buf));
        }
        try {
            compressor.end();
            bos.close();
            byte[] compressed = bos.toByteArray();
            return Base64.encodeToString(compressed.length < input.length ? compressed : input, 0);
        } catch (IOException e) {
            this.mLog.mo2097wC("ByteArrayOutputStream close error");
            return Base64.encodeToString(input, 0);
        }
    }

    private ArrayList<String> getLogcat(int maxLines) {
        ArrayList<String> lines = new ArrayList<>(maxLines);
        if (Debug.semIsProductDev()) {
            try {
                Process process = this.mJavaRuntime.exec(String.format("logcat -t %d", new Object[]{Integer.valueOf(maxLines)}));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    lines.add(line);
                }
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while (true) {
                    String readLine2 = reader2.readLine();
                    String line2 = readLine2;
                    if (readLine2 == null) {
                        break;
                    }
                    lines.add(line2);
                }
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                this.mLog.dump("Exception while capturing logcat: %").mo2070c(e.toString()).flush();
            }
        }
        return lines;
    }

    private LimitedCircularArray<String> getKernelLog(int maxLines) {
        LimitedCircularArray<String> lines = new LimitedCircularArray<>(maxLines);
        String[] logLines = this.mWifiNative.readKernelLog().split("\n");
        for (String addLast : logLines) {
            lines.addLast(addLast);
        }
        return lines;
    }

    private ArrayList<WifiNative.FateReport> fetchPacketFates() {
        ArrayList<WifiNative.FateReport> mergedFates = new ArrayList<>();
        WifiNative.TxFateReport[] txFates = new WifiNative.TxFateReport[32];
        if (this.mWifiNative.getTxPktFates(this.mWifiNative.getClientInterfaceName(), txFates)) {
            int i = 0;
            while (i < txFates.length && txFates[i] != null) {
                mergedFates.add(txFates[i]);
                i++;
            }
        }
        WifiNative.RxFateReport[] rxFates = new WifiNative.RxFateReport[32];
        if (this.mWifiNative.getRxPktFates(this.mWifiNative.getClientInterfaceName(), rxFates)) {
            int i2 = 0;
            while (i2 < rxFates.length && rxFates[i2] != null) {
                mergedFates.add(rxFates[i2]);
                i2++;
            }
        }
        Collections.sort(mergedFates, new Comparator<WifiNative.FateReport>() {
            public int compare(WifiNative.FateReport lhs, WifiNative.FateReport rhs) {
                return Long.compare(lhs.mDriverTimestampUSec, rhs.mDriverTimestampUSec);
            }
        });
        return mergedFates;
    }

    private void dumpPacketFates(PrintWriter pw) {
        dumpPacketFatesInternal(pw, "Last failed connection fates", this.mPacketFatesForLastFailure, isVerboseLoggingEnabled());
        dumpPacketFatesInternal(pw, "Latest fates", fetchPacketFates(), isVerboseLoggingEnabled());
    }

    private static void dumpPacketFatesInternal(PrintWriter pw, String description, ArrayList<WifiNative.FateReport> fates, boolean verbose) {
        if (fates == null) {
            pw.format("No fates fetched for \"%s\"\n", new Object[]{description});
        } else if (fates.size() == 0) {
            pw.format("HAL provided zero fates for \"%s\"\n", new Object[]{description});
        } else {
            pw.format("--------------------- %s ----------------------\n", new Object[]{description});
            StringBuilder verboseOutput = new StringBuilder();
            pw.print(WifiNative.FateReport.getTableHeader());
            Iterator<WifiNative.FateReport> it = fates.iterator();
            while (it.hasNext()) {
                WifiNative.FateReport fate = it.next();
                pw.print(fate.toTableRowString());
                if (verbose) {
                    verboseOutput.append(fate.toVerboseStringWithPiiAllowed());
                    verboseOutput.append("\n");
                }
            }
            if (verbose) {
                pw.format("\n>>> VERBOSE PACKET FATE DUMP <<<\n\n", new Object[0]);
                pw.print(verboseOutput.toString());
            }
            pw.println("--------------------------------------------------------------------");
        }
    }
}
