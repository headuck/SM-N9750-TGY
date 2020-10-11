package com.android.server.wifi.rtt;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.RttResult;
import android.net.MacAddress;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.rtt.IRttCallback;
import android.net.wifi.rtt.IWifiRttManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.ResponderConfig;
import android.net.wifi.rtt.ResponderLocation;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class RttServiceImpl extends IWifiRttManager.Stub {
    private static final int CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_DEFAULT = 0;
    private static final String CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME = "override_assume_no_privilege";
    private static final int CONVERSION_US_TO_MS = 1000;
    private static final long DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS = 1800000;
    @VisibleForTesting
    public static final long HAL_AWARE_RANGING_TIMEOUT_MS = 10000;
    @VisibleForTesting
    public static final long HAL_RANGING_TIMEOUT_MS = 5000;
    static final String HAL_RANGING_TIMEOUT_TAG = "RttServiceImpl HAL Ranging Timeout";
    static final int MAX_QUEUED_PER_UID = 20;
    private static final String TAG = "RttServiceImpl";
    private static final boolean VDBG = false;
    /* access modifiers changed from: private */
    public ActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public IWifiAwareManager mAwareBinder;
    /* access modifiers changed from: private */
    public long mBackgroundProcessExecGapMs;
    /* access modifiers changed from: private */
    public Clock mClock;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDbg = false;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public RttMetrics mRttMetrics;
    /* access modifiers changed from: private */
    public RttNative mRttNative;
    /* access modifiers changed from: private */
    public RttServiceSynchronized mRttServiceSynchronized;
    private final RttShellCommand mShellCommand;
    /* access modifiers changed from: private */
    public WifiPermissionsUtil mWifiPermissionsUtil;

    public RttServiceImpl(Context context) {
        this.mContext = context;
        this.mShellCommand = new RttShellCommand();
        this.mShellCommand.reset();
    }

    private class RttShellCommand extends ShellCommand {
        private Map<String, Integer> mControlParams;

        private RttShellCommand() {
            this.mControlParams = new HashMap();
        }

        public int onCommand(String cmd) {
            int uid = Binder.getCallingUid();
            if (uid == 0) {
                PrintWriter pw = getErrPrintWriter();
                try {
                    if ("reset".equals(cmd)) {
                        reset();
                        return 0;
                    } else if ("get".equals(cmd)) {
                        String name = getNextArgRequired();
                        if (!this.mControlParams.containsKey(name)) {
                            pw.println("Unknown parameter name -- '" + name + "'");
                            return -1;
                        }
                        getOutPrintWriter().println(this.mControlParams.get(name));
                        return 0;
                    } else if ("set".equals(cmd)) {
                        String name2 = getNextArgRequired();
                        String valueStr = getNextArgRequired();
                        if (!this.mControlParams.containsKey(name2)) {
                            pw.println("Unknown parameter name -- '" + name2 + "'");
                            return -1;
                        }
                        try {
                            this.mControlParams.put(name2, Integer.valueOf(valueStr));
                            return 0;
                        } catch (NumberFormatException e) {
                            pw.println("Can't convert value to integer -- '" + valueStr + "'");
                            return -1;
                        }
                    } else if ("get_capabilities".equals(cmd)) {
                        RttCapabilities cap = RttServiceImpl.this.mRttNative.getRttCapabilities();
                        JSONObject j = new JSONObject();
                        if (cap != null) {
                            try {
                                j.put("rttOneSidedSupported", cap.rttOneSidedSupported);
                                j.put("rttFtmSupported", cap.rttFtmSupported);
                                j.put("lciSupported", cap.lciSupported);
                                j.put("lcrSupported", cap.lcrSupported);
                                j.put("responderSupported", cap.responderSupported);
                                j.put("mcVersion", cap.mcVersion);
                            } catch (JSONException e2) {
                                Log.e(RttServiceImpl.TAG, "onCommand: get_capabilities e=" + e2);
                            }
                        }
                        getOutPrintWriter().println(j.toString());
                        return 0;
                    } else {
                        handleDefaultCommands(cmd);
                        return -1;
                    }
                } catch (Exception e3) {
                    pw.println("Exception: " + e3);
                }
            } else {
                throw new SecurityException("Uid " + uid + " does not have access to wifirtt commands");
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Wi-Fi RTT (wifirt) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  reset");
            pw.println("    Reset parameters to default values.");
            pw.println("  get_capabilities: prints out the RTT capabilities as a JSON string");
            pw.println("  get <name>");
            pw.println("    Get the value of the control parameter.");
            pw.println("  set <name> <value>");
            pw.println("    Set the value of the control parameter.");
            pw.println("  Control parameters:");
            for (String name : this.mControlParams.keySet()) {
                pw.println("    " + name);
            }
            pw.println();
        }

        public int getControlParam(String name) {
            if (this.mControlParams.containsKey(name)) {
                return this.mControlParams.get(name).intValue();
            }
            Log.wtf(RttServiceImpl.TAG, "getControlParam for unknown variable: " + name);
            return 0;
        }

        public void reset() {
            this.mControlParams.put(RttServiceImpl.CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME, 0);
        }
    }

    public void start(Looper looper, Clock clock, IWifiAwareManager awareBinder, RttNative rttNative, RttMetrics rttMetrics, WifiPermissionsUtil wifiPermissionsUtil, final FrameworkFacade frameworkFacade) {
        this.mClock = clock;
        this.mAwareBinder = awareBinder;
        this.mRttNative = rttNative;
        this.mRttMetrics = rttMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mRttServiceSynchronized = new RttServiceSynchronized(looper, rttNative);
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (RttServiceImpl.this.mDbg) {
                    Log.v(RttServiceImpl.TAG, "BroadcastReceiver: action=" + action);
                }
                if (!"android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                    return;
                }
                if (RttServiceImpl.this.mPowerManager.isDeviceIdleMode()) {
                    RttServiceImpl.this.disable();
                } else {
                    RttServiceImpl.this.enableIfPossible();
                }
            }
        }, intentFilter);
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            public void onChange(boolean selfChange) {
                RttServiceImpl rttServiceImpl = RttServiceImpl.this;
                rttServiceImpl.enableVerboseLogging(frameworkFacade.getIntegerSetting(rttServiceImpl.mContext, "wifi_verbose_logging_enabled", 0));
            }
        });
        enableVerboseLogging(frameworkFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0));
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_rtt_background_exec_gap_ms"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            public void onChange(boolean selfChange) {
                RttServiceImpl.this.updateBackgroundThrottlingInterval(frameworkFacade);
            }
        });
        updateBackgroundThrottlingInterval(frameworkFacade);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (RttServiceImpl.this.mDbg) {
                    Log.v(RttServiceImpl.TAG, "onReceive: MODE_CHANGED_ACTION: intent=" + intent);
                }
                if (RttServiceImpl.this.mWifiPermissionsUtil.isLocationModeEnabled()) {
                    RttServiceImpl.this.enableIfPossible();
                } else {
                    RttServiceImpl.this.disable();
                }
            }
        }, intentFilter2);
        this.mRttServiceSynchronized.mHandler.post(new Runnable(rttNative) {
            private final /* synthetic */ RttNative f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                RttServiceImpl.this.lambda$start$0$RttServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$start$0$RttServiceImpl(RttNative rttNative) {
        rttNative.start(this.mRttServiceSynchronized.mHandler);
    }

    /* access modifiers changed from: private */
    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
        RttNative rttNative = this.mRttNative;
        boolean z = this.mDbg;
        rttNative.mDbg = z;
        this.mRttMetrics.mDbg = z;
    }

    /* access modifiers changed from: private */
    public void updateBackgroundThrottlingInterval(FrameworkFacade frameworkFacade) {
        this.mBackgroundProcessExecGapMs = frameworkFacade.getLongSetting(this.mContext, "wifi_rtt_background_exec_gap_ms", DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS);
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void enableIfPossible() {
        if (isAvailable()) {
            sendRttStateChangedBroadcast(true);
            this.mRttServiceSynchronized.mHandler.post(new Runnable() {
                public final void run() {
                    RttServiceImpl.this.lambda$enableIfPossible$1$RttServiceImpl();
                }
            });
        }
    }

    public /* synthetic */ void lambda$enableIfPossible$1$RttServiceImpl() {
        this.mRttServiceSynchronized.executeNextRangingRequestIfPossible(false);
    }

    public void disable() {
        sendRttStateChangedBroadcast(false);
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            public final void run() {
                RttServiceImpl.this.lambda$disable$2$RttServiceImpl();
            }
        });
    }

    public /* synthetic */ void lambda$disable$2$RttServiceImpl() {
        this.mRttServiceSynchronized.cleanUpOnDisable();
    }

    /* JADX WARNING: type inference failed for: r1v0, types: [android.os.Binder] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onShellCommand(java.io.FileDescriptor r9, java.io.FileDescriptor r10, java.io.FileDescriptor r11, java.lang.String[] r12, android.os.ShellCallback r13, android.os.ResultReceiver r14) {
        /*
            r8 = this;
            com.android.server.wifi.rtt.RttServiceImpl$RttShellCommand r0 = r8.mShellCommand
            r1 = r8
            r2 = r9
            r3 = r10
            r4 = r11
            r5 = r12
            r6 = r13
            r7 = r14
            r0.exec(r1, r2, r3, r4, r5, r6, r7)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.rtt.RttServiceImpl.onShellCommand(java.io.FileDescriptor, java.io.FileDescriptor, java.io.FileDescriptor, java.lang.String[], android.os.ShellCallback, android.os.ResultReceiver):void");
    }

    public boolean isAvailable() {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mRttNative.isReady() && !this.mPowerManager.isDeviceIdleMode() && this.mWifiPermissionsUtil.isLocationModeEnabled();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void startRanging(IBinder binder, String callingPackage, WorkSource workSource, RangingRequest request, IRttCallback callback) throws RemoteException {
        final IBinder iBinder = binder;
        RangingRequest rangingRequest = request;
        IRttCallback iRttCallback = callback;
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (rangingRequest == null || rangingRequest.mRttPeers == null || rangingRequest.mRttPeers.size() == 0) {
            throw new IllegalArgumentException("Request must not be null or empty");
        } else {
            for (ResponderConfig responder : rangingRequest.mRttPeers) {
                if (responder == null) {
                    throw new IllegalArgumentException("Request must not contain null Responders");
                }
            }
            if (iRttCallback != null) {
                rangingRequest.enforceValidity(this.mAwareBinder != null);
                if (!isAvailable()) {
                    try {
                        this.mRttMetrics.recordOverallStatus(3);
                        iRttCallback.onRangingFailure(2);
                    } catch (RemoteException e) {
                        Log.e(TAG, "startRanging: disabled, callback failed -- " + e);
                    }
                } else {
                    final int uid = getMockableCallingUid();
                    enforceAccessPermission();
                    enforceChangePermission();
                    this.mWifiPermissionsUtil.enforceFineLocationPermission(callingPackage, uid);
                    if (workSource != null) {
                        enforceLocationHardware();
                        workSource.clearNames();
                    }
                    boolean isCalledFromPrivilegedContext = checkLocationHardware() && this.mShellCommand.getControlParam(CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME) == 0;
                    C05565 r8 = new IBinder.DeathRecipient() {
                        public void binderDied() {
                            if (RttServiceImpl.this.mDbg) {
                                Log.v(RttServiceImpl.TAG, "binderDied: uid=" + uid);
                            }
                            iBinder.unlinkToDeath(this, 0);
                            RttServiceImpl.this.mRttServiceSynchronized.mHandler.post(new Runnable(uid) {
                                private final /* synthetic */ int f$1;

                                {
                                    this.f$1 = r2;
                                }

                                public final void run() {
                                    RttServiceImpl.C05565.this.lambda$binderDied$0$RttServiceImpl$5(this.f$1);
                                }
                            });
                        }

                        public /* synthetic */ void lambda$binderDied$0$RttServiceImpl$5(int uid) {
                            RttServiceImpl.this.mRttServiceSynchronized.cleanUpClientRequests(uid, (WorkSource) null);
                        }
                    };
                    try {
                        iBinder.linkToDeath(r8, 0);
                        Handler handler = this.mRttServiceSynchronized.mHandler;
                        $$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0 r11 = r1;
                        C05565 r16 = r8;
                        $$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0 r1 = new Runnable(workSource, uid, binder, r8, callingPackage, request, callback, isCalledFromPrivilegedContext) {
                            private final /* synthetic */ WorkSource f$1;
                            private final /* synthetic */ int f$2;
                            private final /* synthetic */ IBinder f$3;
                            private final /* synthetic */ IBinder.DeathRecipient f$4;
                            private final /* synthetic */ String f$5;
                            private final /* synthetic */ RangingRequest f$6;
                            private final /* synthetic */ IRttCallback f$7;
                            private final /* synthetic */ boolean f$8;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                                this.f$3 = r4;
                                this.f$4 = r5;
                                this.f$5 = r6;
                                this.f$6 = r7;
                                this.f$7 = r8;
                                this.f$8 = r9;
                            }

                            public final void run() {
                                RttServiceImpl.this.lambda$startRanging$3$RttServiceImpl(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8);
                            }
                        };
                        handler.post(r11);
                    } catch (RemoteException e2) {
                        C05565 r162 = r8;
                        Log.e(TAG, "Error on linkToDeath - " + e2);
                    }
                }
            } else {
                throw new IllegalArgumentException("Callback must not be null");
            }
        }
    }

    public /* synthetic */ void lambda$startRanging$3$RttServiceImpl(WorkSource workSource, int uid, IBinder binder, IBinder.DeathRecipient dr, String callingPackage, RangingRequest request, IRttCallback callback, boolean isCalledFromPrivilegedContext) {
        WorkSource sourceToUse = workSource;
        if (workSource == null || workSource.isEmpty()) {
            int i = uid;
            sourceToUse = new WorkSource(uid);
        } else {
            int i2 = uid;
        }
        this.mRttServiceSynchronized.queueRangingRequest(uid, sourceToUse, binder, dr, callingPackage, request, callback, isCalledFromPrivilegedContext);
    }

    public void cancelRanging(WorkSource workSource) throws RemoteException {
        enforceLocationHardware();
        if (workSource != null) {
            workSource.clearNames();
        }
        if (workSource == null || workSource.isEmpty()) {
            Log.e(TAG, "cancelRanging: invalid work-source -- " + workSource);
            return;
        }
        this.mRttServiceSynchronized.mHandler.post(new Runnable(workSource) {
            private final /* synthetic */ WorkSource f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                RttServiceImpl.this.lambda$cancelRanging$4$RttServiceImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$cancelRanging$4$RttServiceImpl(WorkSource workSource) {
        this.mRttServiceSynchronized.cleanUpClientRequests(0, workSource);
    }

    public void onRangingResults(int cmdId, List<RttResult> results) {
        this.mRttServiceSynchronized.mHandler.post(new Runnable(cmdId, results) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ List f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                RttServiceImpl.this.lambda$onRangingResults$5$RttServiceImpl(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$onRangingResults$5$RttServiceImpl(int cmdId, List results) {
        this.mRttServiceSynchronized.onRangingResults(cmdId, results);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationHardware() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", TAG);
    }

    private boolean checkLocationHardware() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE") == 0;
    }

    private void sendRttStateChangedBroadcast(boolean enabled) {
        Intent intent = new Intent("android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED");
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump RttService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Wi-Fi RTT Service");
        this.mRttServiceSynchronized.dump(fd, pw, args);
    }

    private class RttServiceSynchronized {
        public Handler mHandler;
        private int mNextCommandId = 1000;
        private WakeupMessage mRangingTimeoutMessage = null;
        private RttNative mRttNative;
        private List<RttRequestInfo> mRttRequestQueue = new LinkedList();
        private Map<Integer, RttRequesterInfo> mRttRequesterInfo = new HashMap();

        RttServiceSynchronized(Looper looper, RttNative rttNative) {
            this.mRttNative = rttNative;
            this.mHandler = new Handler(looper);
            this.mRangingTimeoutMessage = new WakeupMessage(RttServiceImpl.this.mContext, this.mHandler, RttServiceImpl.HAL_RANGING_TIMEOUT_TAG, new Runnable() {
                public final void run() {
                    RttServiceImpl.RttServiceSynchronized.this.lambda$new$0$RttServiceImpl$RttServiceSynchronized();
                }
            });
        }

        private void cancelRanging(RttRequestInfo rri) {
            ArrayList<byte[]> macAddresses = new ArrayList<>();
            for (ResponderConfig peer : rri.request.mRttPeers) {
                macAddresses.add(peer.macAddress.toByteArray());
            }
            this.mRttNative.rangeCancel(rri.cmdId, macAddresses);
        }

        /* access modifiers changed from: private */
        public void cleanUpOnDisable() {
            for (RttRequestInfo rri : this.mRttRequestQueue) {
                try {
                    if (rri.dispatchedToNative) {
                        cancelRanging(rri);
                    }
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    rri.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled, callback failed -- " + e);
                }
                rri.binder.unlinkToDeath(rri.f31dr, 0);
            }
            this.mRttRequestQueue.clear();
            this.mRangingTimeoutMessage.cancel();
        }

        /* access modifiers changed from: private */
        public void cleanUpClientRequests(int uid, WorkSource workSource) {
            boolean dispatchedRequestAborted = false;
            ListIterator<RttRequestInfo> it = this.mRttRequestQueue.listIterator();
            while (true) {
                boolean match = true;
                if (!it.hasNext()) {
                    break;
                }
                RttRequestInfo rri = it.next();
                if (rri.uid != uid) {
                    match = false;
                }
                if (!(rri.workSource == null || workSource == null)) {
                    rri.workSource.remove(workSource);
                    if (rri.workSource.isEmpty()) {
                        match = true;
                    }
                }
                if (match) {
                    if (!rri.dispatchedToNative) {
                        it.remove();
                        rri.binder.unlinkToDeath(rri.f31dr, 0);
                    } else {
                        dispatchedRequestAborted = true;
                        Log.d(RttServiceImpl.TAG, "Client death - cancelling RTT operation in progress: cmdId=" + rri.cmdId);
                        this.mRangingTimeoutMessage.cancel();
                        cancelRanging(rri);
                    }
                }
            }
            if (dispatchedRequestAborted) {
                executeNextRangingRequestIfPossible(true);
            }
        }

        /* access modifiers changed from: private */
        /* renamed from: timeoutRangingRequest */
        public void lambda$new$0$RttServiceImpl$RttServiceSynchronized() {
            if (this.mRttRequestQueue.size() == 0) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: but nothing in queue!?");
                return;
            }
            RttRequestInfo rri = this.mRttRequestQueue.get(0);
            if (!rri.dispatchedToNative) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: command not dispatched to native!?");
                return;
            }
            cancelRanging(rri);
            try {
                RttServiceImpl.this.mRttMetrics.recordOverallStatus(4);
                rri.callback.onRangingFailure(1);
            } catch (RemoteException e) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: callback failed: " + e);
            }
            executeNextRangingRequestIfPossible(true);
        }

        /* access modifiers changed from: private */
        public void queueRangingRequest(int uid, WorkSource workSource, IBinder binder, IBinder.DeathRecipient dr, String callingPackage, RangingRequest request, IRttCallback callback, boolean isCalledFromPrivilegedContext) {
            RttServiceImpl.this.mRttMetrics.recordRequest(workSource, request);
            if (isRequestorSpamming(workSource)) {
                Log.w(RttServiceImpl.TAG, "Work source " + workSource + " is spamming, dropping request: " + request);
                binder.unlinkToDeath(dr, 0);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                    callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.queueRangingRequest: spamming, callback failed -- " + e);
                }
            } else {
                RttRequestInfo newRequest = new RttRequestInfo();
                newRequest.uid = uid;
                newRequest.workSource = workSource;
                newRequest.binder = binder;
                newRequest.f31dr = dr;
                newRequest.callingPackage = callingPackage;
                newRequest.request = request;
                newRequest.callback = callback;
                newRequest.isCalledFromPrivilegedContext = isCalledFromPrivilegedContext;
                this.mRttRequestQueue.add(newRequest);
                executeNextRangingRequestIfPossible(false);
            }
        }

        private boolean isRequestorSpamming(WorkSource ws) {
            SparseIntArray counts = new SparseIntArray();
            for (RttRequestInfo rri : this.mRttRequestQueue) {
                for (int i = 0; i < rri.workSource.size(); i++) {
                    int uid = rri.workSource.get(i);
                    counts.put(uid, counts.get(uid) + 1);
                }
                ArrayList<WorkSource.WorkChain> workChains = rri.workSource.getWorkChains();
                if (workChains != null) {
                    for (int i2 = 0; i2 < workChains.size(); i2++) {
                        int uid2 = workChains.get(i2).getAttributionUid();
                        counts.put(uid2, counts.get(uid2) + 1);
                    }
                }
            }
            for (int i3 = 0; i3 < ws.size(); i3++) {
                if (counts.get(ws.get(i3)) < 20) {
                    return false;
                }
            }
            ArrayList<WorkSource.WorkChain> workChains2 = ws.getWorkChains();
            if (workChains2 != null) {
                for (int i4 = 0; i4 < workChains2.size(); i4++) {
                    if (counts.get(workChains2.get(i4).getAttributionUid()) < 20) {
                        return false;
                    }
                }
            }
            if (RttServiceImpl.this.mDbg) {
                Log.v(RttServiceImpl.TAG, "isRequestorSpamming: ws=" + ws + ", someone is spamming: " + counts);
            }
            return true;
        }

        /* access modifiers changed from: private */
        public void executeNextRangingRequestIfPossible(boolean popFirst) {
            if (popFirst) {
                if (this.mRttRequestQueue.size() == 0) {
                    Log.w(RttServiceImpl.TAG, "executeNextRangingRequestIfPossible: pop requested - but empty queue!? Ignoring pop.");
                } else {
                    RttRequestInfo topOfQueueRequest = this.mRttRequestQueue.remove(0);
                    topOfQueueRequest.binder.unlinkToDeath(topOfQueueRequest.f31dr, 0);
                }
            }
            if (this.mRttRequestQueue.size() != 0) {
                RttRequestInfo nextRequest = this.mRttRequestQueue.get(0);
                if (!nextRequest.peerHandlesTranslated && !nextRequest.dispatchedToNative) {
                    startRanging(nextRequest);
                }
            }
        }

        private void startRanging(RttRequestInfo nextRequest) {
            if (!RttServiceImpl.this.isAvailable()) {
                Log.d(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    nextRequest.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled, callback failed -- " + e);
                    executeNextRangingRequestIfPossible(true);
                    return;
                }
            }
            if (!processAwarePeerHandles(nextRequest)) {
                if (!preExecThrottleCheck(nextRequest.workSource)) {
                    Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: execution throttled - nextRequest=" + nextRequest + ", mRttRequesterInfo=" + this.mRttRequesterInfo);
                    try {
                        RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                        nextRequest.callback.onRangingFailure(1);
                    } catch (RemoteException e2) {
                        Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: throttled, callback failed -- " + e2);
                    }
                    executeNextRangingRequestIfPossible(true);
                    return;
                }
                int i = this.mNextCommandId;
                this.mNextCommandId = i + 1;
                nextRequest.cmdId = i;
                if (this.mRttNative.rangeRequest(nextRequest.cmdId, nextRequest.request, nextRequest.isCalledFromPrivilegedContext)) {
                    long timeout = RttServiceImpl.HAL_RANGING_TIMEOUT_MS;
                    Iterator it = nextRequest.request.mRttPeers.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (((ResponderConfig) it.next()).responderType == 4) {
                                timeout = 10000;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    this.mRangingTimeoutMessage.schedule(RttServiceImpl.this.mClock.getElapsedSinceBootMillis() + timeout);
                } else {
                    Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: native rangeRequest call failed");
                    try {
                        RttServiceImpl.this.mRttMetrics.recordOverallStatus(6);
                        nextRequest.callback.onRangingFailure(1);
                    } catch (RemoteException e3) {
                        Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: HAL request failed, callback failed -- " + e3);
                    }
                    executeNextRangingRequestIfPossible(true);
                }
                nextRequest.dispatchedToNative = true;
            }
        }

        private boolean preExecThrottleCheck(WorkSource ws) {
            boolean allUidsInBackground = true;
            int i = 0;
            while (true) {
                if (i >= ws.size()) {
                    break;
                } else if (RttServiceImpl.this.mActivityManager.getUidImportance(ws.get(i)) <= 125) {
                    allUidsInBackground = false;
                    break;
                } else {
                    i++;
                }
            }
            ArrayList<WorkSource.WorkChain> workChains = ws.getWorkChains();
            if (allUidsInBackground && workChains != null) {
                int i2 = 0;
                while (true) {
                    if (i2 >= workChains.size()) {
                        break;
                    } else if (RttServiceImpl.this.mActivityManager.getUidImportance(workChains.get(i2).getAttributionUid()) <= 125) {
                        allUidsInBackground = false;
                        break;
                    } else {
                        i2++;
                    }
                }
            }
            boolean allowExecution = false;
            long mostRecentExecutionPermitted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis() - RttServiceImpl.this.mBackgroundProcessExecGapMs;
            if (allUidsInBackground) {
                int i3 = 0;
                while (true) {
                    if (i3 >= ws.size()) {
                        break;
                    }
                    RttRequesterInfo info = this.mRttRequesterInfo.get(Integer.valueOf(ws.get(i3)));
                    if (info == null || info.lastRangingExecuted < mostRecentExecutionPermitted) {
                        allowExecution = true;
                    } else {
                        i3++;
                    }
                }
                allowExecution = true;
                int i4 = 0;
                int i5 = workChains != null ? 1 : 0;
                if (!allowExecution) {
                    i4 = 1;
                }
                if ((i4 & i5) != 0) {
                    int i6 = 0;
                    while (true) {
                        if (i6 >= workChains.size()) {
                            break;
                        }
                        RttRequesterInfo info2 = this.mRttRequesterInfo.get(Integer.valueOf(workChains.get(i6).getAttributionUid()));
                        if (info2 == null || info2.lastRangingExecuted < mostRecentExecutionPermitted) {
                            allowExecution = true;
                        } else {
                            i6++;
                        }
                    }
                    allowExecution = true;
                }
            } else {
                allowExecution = true;
            }
            if (allowExecution) {
                for (int i7 = 0; i7 < ws.size(); i7++) {
                    RttRequesterInfo info3 = this.mRttRequesterInfo.get(Integer.valueOf(ws.get(i7)));
                    if (info3 == null) {
                        info3 = new RttRequesterInfo();
                        this.mRttRequesterInfo.put(Integer.valueOf(ws.get(i7)), info3);
                    }
                    info3.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                }
                if (workChains != null) {
                    for (int i8 = 0; i8 < workChains.size(); i8++) {
                        WorkSource.WorkChain wc = workChains.get(i8);
                        RttRequesterInfo info4 = this.mRttRequesterInfo.get(Integer.valueOf(wc.getAttributionUid()));
                        if (info4 == null) {
                            info4 = new RttRequesterInfo();
                            this.mRttRequesterInfo.put(Integer.valueOf(wc.getAttributionUid()), info4);
                        }
                        info4.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                    }
                }
            }
            return allowExecution;
        }

        private boolean processAwarePeerHandles(final RttRequestInfo request) {
            List<Integer> peerIdsNeedingTranslation = new ArrayList<>();
            for (ResponderConfig rttPeer : request.request.mRttPeers) {
                if (rttPeer.peerHandle != null && rttPeer.macAddress == null) {
                    peerIdsNeedingTranslation.add(Integer.valueOf(rttPeer.peerHandle.peerId));
                }
            }
            if (peerIdsNeedingTranslation.size() == 0) {
                return false;
            }
            if (request.peerHandlesTranslated) {
                Log.w(RttServiceImpl.TAG, "processAwarePeerHandles: request=" + request + ": PeerHandles translated - but information still missing!?");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    request.callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: onRangingResults failure -- " + e);
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
            request.peerHandlesTranslated = true;
            try {
                RttServiceImpl.this.mAwareBinder.requestMacAddresses(request.uid, peerIdsNeedingTranslation, new IWifiAwareMacAddressProvider.Stub() {
                    public void macAddress(Map peerIdToMacMap) {
                        RttServiceSynchronized.this.mHandler.post(
                        /*  JADX ERROR: Method code generation error
                            jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x000b: INVOKE  
                              (wrap: android.os.Handler : 0x0002: IGET  (r0v1 android.os.Handler) = 
                              (wrap: com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized : 0x0000: IGET  (r0v0 com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized) = 
                              (r3v0 'this' com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1 A[THIS])
                             com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.1.this$1 com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized)
                             com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.mHandler android.os.Handler)
                              (wrap: com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk : 0x0008: CONSTRUCTOR  (r2v0 com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk) = 
                              (r3v0 'this' com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1 A[THIS])
                              (wrap: com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo : 0x0004: IGET  (r1v0 com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo) = 
                              (r3v0 'this' com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1 A[THIS])
                             com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.1.val$request com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo)
                              (r4v0 'peerIdToMacMap' java.util.Map)
                             call: com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk.<init>(com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1, com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo, java.util.Map):void type: CONSTRUCTOR)
                             android.os.Handler.post(java.lang.Runnable):boolean type: VIRTUAL in method: com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.1.macAddress(java.util.Map):void, dex: classes.dex
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:221)
                            	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:211)
                            	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:204)
                            	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:318)
                            	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:271)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:240)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.InsnGen.inlineAnonymousConstructor(InsnGen.java:676)
                            	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:607)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:364)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                            	at jadx.core.codegen.InsnGen.addWrappedArg(InsnGen.java:123)
                            	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:107)
                            	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:787)
                            	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:728)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:368)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:250)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:221)
                            	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:109)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:98)
                            	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:311)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:68)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:92)
                            	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:58)
                            	at jadx.core.codegen.MethodGen.addRegionInsns(MethodGen.java:211)
                            	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:204)
                            	at jadx.core.codegen.ClassGen.addMethodCode(ClassGen.java:318)
                            	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:271)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:240)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:112)
                            	at jadx.core.codegen.ClassGen.addInnerClass(ClassGen.java:249)
                            	at jadx.core.codegen.ClassGen.lambda$addInnerClsAndMethods$2(ClassGen.java:238)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
                            	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
                            	at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
                            	at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:258)
                            	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:485)
                            	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
                            	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
                            	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
                            	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
                            	at jadx.core.codegen.ClassGen.addInnerClsAndMethods(ClassGen.java:236)
                            	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:227)
                            	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:112)
                            	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:78)
                            	at jadx.core.codegen.CodeGen.wrapCodeGen(CodeGen.java:44)
                            	at jadx.core.codegen.CodeGen.generateJavaCode(CodeGen.java:33)
                            	at jadx.core.codegen.CodeGen.generate(CodeGen.java:21)
                            	at jadx.core.ProcessClass.generateCode(ProcessClass.java:61)
                            	at jadx.core.dex.nodes.ClassNode.decompile(ClassNode.java:273)
                            Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0008: CONSTRUCTOR  (r2v0 com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk) = 
                              (r3v0 'this' com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1 A[THIS])
                              (wrap: com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo : 0x0004: IGET  (r1v0 com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo) = 
                              (r3v0 'this' com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1 A[THIS])
                             com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.1.val$request com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo)
                              (r4v0 'peerIdToMacMap' java.util.Map)
                             call: com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk.<init>(com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized$1, com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo, java.util.Map):void type: CONSTRUCTOR in method: com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.1.macAddress(java.util.Map):void, dex: classes.dex
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:256)
                            	at jadx.core.codegen.InsnGen.addWrappedArg(InsnGen.java:123)
                            	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:107)
                            	at jadx.core.codegen.InsnGen.generateMethodArguments(InsnGen.java:787)
                            	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:728)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:368)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:250)
                            	... 89 more
                            Caused by: jadx.core.utils.exceptions.JadxRuntimeException: Expected class to be processed at this point, class: com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk, state: NOT_LOADED
                            	at jadx.core.dex.nodes.ClassNode.ensureProcessed(ClassNode.java:260)
                            	at jadx.core.codegen.InsnGen.makeConstructor(InsnGen.java:606)
                            	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:364)
                            	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:231)
                            	... 95 more
                            */
                        /*
                            this = this;
                            com.android.server.wifi.rtt.RttServiceImpl$RttServiceSynchronized r0 = com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.this
                            android.os.Handler r0 = r0.mHandler
                            com.android.server.wifi.rtt.RttServiceImpl$RttRequestInfo r1 = r9
                            com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk r2 = new com.android.server.wifi.rtt.-$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk
                            r2.<init>(r3, r1, r4)
                            r0.post(r2)
                            return
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.rtt.RttServiceImpl.RttServiceSynchronized.C05571.macAddress(java.util.Map):void");
                    }

                    public /* synthetic */ void lambda$macAddress$0$RttServiceImpl$RttServiceSynchronized$1(RttRequestInfo request, Map peerIdToMacMap) {
                        RttServiceSynchronized.this.processReceivedAwarePeerMacAddresses(request, peerIdToMacMap);
                    }
                });
                return true;
            } catch (RemoteException e1) {
                Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: exception while calling requestMacAddresses -- " + e1 + ", aborting request=" + request);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    request.callback.onRangingFailure(1);
                } catch (RemoteException e2) {
                    Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: onRangingResults failure -- " + e2);
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
        }

        /* access modifiers changed from: private */
        public void processReceivedAwarePeerMacAddresses(RttRequestInfo request, Map<Integer, byte[]> peerIdToMacMap) {
            RttRequestInfo rttRequestInfo = request;
            RangingRequest.Builder newRequestBuilder = new RangingRequest.Builder();
            for (ResponderConfig rttPeer : rttRequestInfo.request.mRttPeers) {
                if (rttPeer.peerHandle == null || rttPeer.macAddress != null) {
                    Map<Integer, byte[]> map = peerIdToMacMap;
                    newRequestBuilder.addResponder(rttPeer);
                } else {
                    byte[] mac = peerIdToMacMap.get(Integer.valueOf(rttPeer.peerHandle.peerId));
                    if (mac == null || mac.length != 6) {
                        Log.e(RttServiceImpl.TAG, "processReceivedAwarePeerMacAddresses: received an invalid MAC address for peerId=" + rttPeer.peerHandle.peerId);
                    } else {
                        newRequestBuilder.addResponder(new ResponderConfig(MacAddress.fromBytes(mac), rttPeer.peerHandle, rttPeer.responderType, rttPeer.supports80211mc, rttPeer.channelWidth, rttPeer.frequency, rttPeer.centerFreq0, rttPeer.centerFreq1, rttPeer.preamble));
                    }
                }
            }
            Map<Integer, byte[]> map2 = peerIdToMacMap;
            rttRequestInfo.request = newRequestBuilder.build();
            startRanging(request);
        }

        /* access modifiers changed from: private */
        public void onRangingResults(int cmdId, List<RttResult> results) {
            if (this.mRttRequestQueue.size() == 0) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: no current RTT request pending!?");
                return;
            }
            this.mRangingTimeoutMessage.cancel();
            boolean permissionGranted = false;
            RttRequestInfo topOfQueueRequest = this.mRttRequestQueue.get(0);
            if (topOfQueueRequest.cmdId != cmdId) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: cmdId=" + cmdId + ", does not match pending RTT request cmdId=" + topOfQueueRequest.cmdId);
                return;
            }
            if (RttServiceImpl.this.mWifiPermissionsUtil.checkCallersLocationPermission(topOfQueueRequest.callingPackage, topOfQueueRequest.uid, false) && RttServiceImpl.this.mWifiPermissionsUtil.isLocationModeEnabled()) {
                permissionGranted = true;
            }
            if (permissionGranted) {
                try {
                    List<RangingResult> finalResults = postProcessResults(topOfQueueRequest.request, results, topOfQueueRequest.isCalledFromPrivilegedContext);
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(1);
                    RttServiceImpl.this.mRttMetrics.recordResult(topOfQueueRequest.request, results);
                    topOfQueueRequest.callback.onRangingResults(finalResults);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: callback exception -- " + e);
                }
            } else {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: location permission revoked - not forwarding results");
                RttServiceImpl.this.mRttMetrics.recordOverallStatus(8);
                topOfQueueRequest.callback.onRangingFailure(1);
            }
            executeNextRangingRequestIfPossible(true);
        }

        private List<RangingResult> postProcessResults(RangingRequest request, List<RttResult> results, boolean isCalledFromPrivilegedContext) {
            Map<MacAddress, RttResult> resultEntries;
            ResponderLocation responderLocation;
            byte[] lcr;
            byte[] lci;
            RttServiceSynchronized rttServiceSynchronized = this;
            RangingRequest rangingRequest = request;
            Map<MacAddress, RttResult> resultEntries2 = new HashMap<>();
            for (RttResult result : results) {
                resultEntries2.put(MacAddress.fromBytes(result.addr), result);
            }
            List<RangingResult> finalResults = new ArrayList<>(rangingRequest.mRttPeers.size());
            for (ResponderConfig peer : rangingRequest.mRttPeers) {
                RttResult resultForRequest = resultEntries2.get(peer.macAddress);
                if (resultForRequest == null) {
                    if (RttServiceImpl.this.mDbg) {
                        Log.v(RttServiceImpl.TAG, "postProcessResults: missing=" + peer.macAddress);
                    }
                    int errorCode = 1;
                    if (!isCalledFromPrivilegedContext && !peer.supports80211mc) {
                        errorCode = 2;
                    }
                    if (peer.peerHandle == null) {
                        RangingResult rangingResult = r8;
                        RangingResult rangingResult2 = new RangingResult(errorCode, peer.macAddress, 0, 0, 0, 0, 0, (byte[]) null, (byte[]) null, (ResponderLocation) null, 0);
                        finalResults.add(rangingResult);
                    } else {
                        RangingResult rangingResult3 = r8;
                        RangingResult rangingResult4 = new RangingResult(errorCode, peer.peerHandle, 0, 0, 0, 0, 0, (byte[]) null, (byte[]) null, (ResponderLocation) null, 0);
                        finalResults.add(rangingResult3);
                    }
                    resultEntries = resultEntries2;
                } else {
                    int status = resultForRequest.status == 0 ? 0 : 1;
                    byte[] lci2 = NativeUtil.byteArrayFromArrayList(resultForRequest.lci.data);
                    byte[] lcr2 = NativeUtil.byteArrayFromArrayList(resultForRequest.lcr.data);
                    try {
                        responderLocation = new ResponderLocation(lci2, lcr2);
                        if (!responderLocation.isValid()) {
                            responderLocation = null;
                        }
                    } catch (Exception e) {
                        Log.e(RttServiceImpl.TAG, "ResponderLocation: lci/lcr parser failed exception -- " + e);
                        responderLocation = null;
                    }
                    if (responderLocation == null) {
                        lci = null;
                        lcr = null;
                    } else if (!isCalledFromPrivilegedContext) {
                        lci = null;
                        responderLocation.setCivicLocationSubelementDefaults();
                        lcr = null;
                    } else {
                        lci = lci2;
                        lcr = lcr2;
                    }
                    if (resultForRequest.successNumber <= 1 && resultForRequest.distanceSdInMm != 0) {
                        if (RttServiceImpl.this.mDbg) {
                            Log.w(RttServiceImpl.TAG, "postProcessResults: non-zero distance stdev with 0||1 num samples!? result=" + resultForRequest);
                        }
                        resultForRequest.distanceSdInMm = 0;
                    }
                    if (peer.peerHandle == null) {
                        resultEntries = resultEntries2;
                        finalResults.add(new RangingResult(status, peer.macAddress, resultForRequest.distanceInMm, resultForRequest.distanceSdInMm, resultForRequest.rssi / -2, resultForRequest.numberPerBurstPeer, resultForRequest.successNumber, lci, lcr, responderLocation, resultForRequest.timeStampInUs / 1000));
                    } else {
                        resultEntries = resultEntries2;
                        finalResults.add(new RangingResult(status, peer.peerHandle, resultForRequest.distanceInMm, resultForRequest.distanceSdInMm, resultForRequest.rssi / -2, resultForRequest.numberPerBurstPeer, resultForRequest.successNumber, lci, lcr, responderLocation, resultForRequest.timeStampInUs / 1000));
                    }
                }
                rttServiceSynchronized = this;
                RangingRequest rangingRequest2 = request;
                resultEntries2 = resultEntries;
            }
            return finalResults;
        }

        /* access modifiers changed from: protected */
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("  mNextCommandId: " + this.mNextCommandId);
            pw.println("  mRttRequesterInfo: " + this.mRttRequesterInfo);
            pw.println("  mRttRequestQueue: " + this.mRttRequestQueue);
            pw.println("  mRangingTimeoutMessage: " + this.mRangingTimeoutMessage);
            RttServiceImpl.this.mRttMetrics.dump(fd, pw, args);
            this.mRttNative.dump(fd, pw, args);
        }
    }

    private static class RttRequestInfo {
        public IBinder binder;
        public IRttCallback callback;
        public String callingPackage;
        public int cmdId;
        public boolean dispatchedToNative;

        /* renamed from: dr */
        public IBinder.DeathRecipient f31dr;
        public boolean isCalledFromPrivilegedContext;
        public boolean peerHandlesTranslated;
        public RangingRequest request;
        public int uid;
        public WorkSource workSource;

        private RttRequestInfo() {
            this.cmdId = 0;
            this.dispatchedToNative = false;
            this.peerHandlesTranslated = false;
        }

        public String toString() {
            return "RttRequestInfo: uid=" + this.uid + ", workSource=" + this.workSource + ", binder=" + this.binder + ", dr=" + this.f31dr + ", callingPackage=" + this.callingPackage + ", request=" + this.request.toString() + ", callback=" + this.callback + ", cmdId=" + this.cmdId + ", peerHandlesTranslated=" + this.peerHandlesTranslated + ", isCalledFromPrivilegedContext=" + this.isCalledFromPrivilegedContext;
        }
    }

    private static class RttRequesterInfo {
        public long lastRangingExecuted;

        private RttRequesterInfo() {
        }

        public String toString() {
            return "RttRequesterInfo: lastRangingExecuted=" + this.lastRangingExecuted;
        }
    }
}
