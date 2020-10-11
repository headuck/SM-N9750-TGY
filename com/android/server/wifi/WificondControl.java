package com.android.server.wifi;

import android.app.AlarmManager;
import android.net.wifi.IApInterface;
import android.net.wifi.IApInterfaceEventCallback;
import android.net.wifi.IClientInterface;
import android.net.wifi.IPnoScanEvent;
import android.net.wifi.IScanEvent;
import android.net.wifi.ISendMgmtFrameEvent;
import android.net.wifi.IWifiScannerImpl;
import android.net.wifi.IWificond;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WificondControl;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.wificond.ChannelSettings;
import com.android.server.wifi.wificond.HiddenNetwork;
import com.android.server.wifi.wificond.NativeScanResult;
import com.android.server.wifi.wificond.PnoNetwork;
import com.android.server.wifi.wificond.PnoSettings;
import com.android.server.wifi.wificond.SingleScanSettings;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class WificondControl implements IBinder.DeathRecipient {
    private static final boolean DBG = Debug.semIsProductDev();
    public static final int SCAN_TYPE_PNO_SCAN = 1;
    public static final int SCAN_TYPE_SINGLE_SCAN = 0;
    public static final int SEND_MGMT_FRAME_TIMEOUT_MS = 1000;
    private static final String TAG = "WificondControl";
    private static final String TIMEOUT_ALARM_TAG = "WificondControl Send Management Frame Timeout";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    /* access modifiers changed from: private */
    public AlarmManager mAlarmManager;
    private HashMap<String, IApInterfaceEventCallback> mApInterfaceListeners = new HashMap<>();
    private HashMap<String, IApInterface> mApInterfaces = new HashMap<>();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private HashMap<String, IClientInterface> mClientInterfaces = new HashMap<>();
    /* access modifiers changed from: private */
    public Clock mClock;
    private WifiNative.WificondDeathEventHandler mDeathEventHandler;
    /* access modifiers changed from: private */
    public Handler mEventHandler;
    private boolean mIsEnhancedOpenSupported;
    private boolean mIsEnhancedOpenSupportedInitialized = false;
    private HashMap<String, IPnoScanEvent> mPnoScanEventHandlers = new HashMap<>();
    private HashMap<String, IScanEvent> mScanEventHandlers = new HashMap<>();
    /* access modifiers changed from: private */
    public AtomicBoolean mSendMgmtFrameInProgress = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public WifiInjector mWifiInjector;
    /* access modifiers changed from: private */
    public WifiMonitor mWifiMonitor;
    private WifiNative mWifiNative = null;
    private IWificond mWificond;
    private HashMap<String, IWifiScannerImpl> mWificondScanners = new HashMap<>();

    private class ScanEventHandler extends IScanEvent.Stub {
        private String mIfaceName;

        ScanEventHandler(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        public void OnScanResultReady() {
            Log.d(WificondControl.TAG, "Scan result ready event");
            WificondControl.this.mWifiMonitor.broadcastScanResultEvent(this.mIfaceName);
        }

        public void OnScanFailed() {
            Log.d(WificondControl.TAG, "Scan failed event");
            WificondControl.this.mWifiMonitor.broadcastScanFailedEvent(this.mIfaceName);
        }
    }

    WificondControl(WifiInjector wifiInjector, WifiMonitor wifiMonitor, CarrierNetworkConfig carrierNetworkConfig, AlarmManager alarmManager, Looper looper, Clock clock) {
        this.mWifiInjector = wifiInjector;
        this.mWifiMonitor = wifiMonitor;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        this.mAlarmManager = alarmManager;
        this.mEventHandler = new Handler(looper);
        this.mClock = clock;
    }

    private class PnoScanEventHandler extends IPnoScanEvent.Stub {
        private String mIfaceName;

        PnoScanEventHandler(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        public void OnPnoNetworkFound() {
            Log.d(WificondControl.TAG, "Pno scan result event");
            WificondControl.this.mWifiMonitor.broadcastPnoScanResultEvent(this.mIfaceName);
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoFoundNetworkEventCount();
        }

        public void OnPnoScanFailed() {
            Log.d(WificondControl.TAG, "Pno Scan failed event");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
        }

        public void OnPnoScanOverOffloadStarted() {
            Log.d(WificondControl.TAG, "Pno scan over offload started");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanStartedOverOffloadCount();
        }

        public void OnPnoScanOverOffloadFailed(int reason) {
            Log.d(WificondControl.TAG, "Pno scan over offload failed");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedOverOffloadCount();
        }
    }

    private class ApInterfaceEventCallback extends IApInterfaceEventCallback.Stub {
        private WifiNative.SoftApListener mSoftApListener;

        ApInterfaceEventCallback(WifiNative.SoftApListener listener) {
            this.mSoftApListener = listener;
        }

        public void onNumAssociatedStationsChanged(int numStations) {
            this.mSoftApListener.onNumAssociatedStationsChanged(numStations);
        }

        public void onSoftApChannelSwitched(int frequency, int bandwidth) {
            this.mSoftApListener.onSoftApChannelSwitched(frequency, bandwidth);
        }
    }

    private class SendMgmtFrameEvent extends ISendMgmtFrameEvent.Stub {
        private WifiNative.SendMgmtFrameCallback mCallback;
        private AlarmManager.OnAlarmListener mTimeoutCallback = new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                WificondControl.SendMgmtFrameEvent.this.lambda$new$1$WificondControl$SendMgmtFrameEvent();
            }
        };
        private boolean mWasCalled = false;

        private void runIfFirstCall(Runnable r) {
            if (!this.mWasCalled) {
                this.mWasCalled = true;
                WificondControl.this.mSendMgmtFrameInProgress.set(false);
                r.run();
            }
        }

        SendMgmtFrameEvent(WifiNative.SendMgmtFrameCallback callback) {
            this.mCallback = callback;
            WificondControl.this.mAlarmManager.set(2, WificondControl.this.mClock.getElapsedSinceBootMillis() + 1000, WificondControl.TIMEOUT_ALARM_TAG, this.mTimeoutCallback, WificondControl.this.mEventHandler);
        }

        public /* synthetic */ void lambda$new$1$WificondControl$SendMgmtFrameEvent() {
            runIfFirstCall(new Runnable() {
                public final void run() {
                    WificondControl.SendMgmtFrameEvent.this.lambda$new$0$WificondControl$SendMgmtFrameEvent();
                }
            });
        }

        public /* synthetic */ void lambda$new$0$WificondControl$SendMgmtFrameEvent() {
            if (WificondControl.this.mVerboseLoggingEnabled) {
                Log.e(WificondControl.TAG, "Timed out waiting for ACK");
            }
            this.mCallback.onFailure(4);
        }

        public void OnAck(int elapsedTimeMs) {
            WificondControl.this.mEventHandler.post(new Runnable(elapsedTimeMs) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WificondControl.SendMgmtFrameEvent.this.lambda$OnAck$3$WificondControl$SendMgmtFrameEvent(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$OnAck$3$WificondControl$SendMgmtFrameEvent(int elapsedTimeMs) {
            runIfFirstCall(new Runnable(elapsedTimeMs) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WificondControl.SendMgmtFrameEvent.this.lambda$OnAck$2$WificondControl$SendMgmtFrameEvent(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$OnAck$2$WificondControl$SendMgmtFrameEvent(int elapsedTimeMs) {
            WificondControl.this.mAlarmManager.cancel(this.mTimeoutCallback);
            this.mCallback.onAck(elapsedTimeMs);
        }

        public void OnFailure(int reason) {
            WificondControl.this.mEventHandler.post(new Runnable(reason) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WificondControl.SendMgmtFrameEvent.this.lambda$OnFailure$5$WificondControl$SendMgmtFrameEvent(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$OnFailure$5$WificondControl$SendMgmtFrameEvent(int reason) {
            runIfFirstCall(new Runnable(reason) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    WificondControl.SendMgmtFrameEvent.this.lambda$OnFailure$4$WificondControl$SendMgmtFrameEvent(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$OnFailure$4$WificondControl$SendMgmtFrameEvent(int reason) {
            WificondControl.this.mAlarmManager.cancel(this.mTimeoutCallback);
            this.mCallback.onFailure(reason);
        }
    }

    public void binderDied() {
        this.mEventHandler.post(new Runnable() {
            public final void run() {
                WificondControl.this.lambda$binderDied$0$WificondControl();
            }
        });
    }

    public /* synthetic */ void lambda$binderDied$0$WificondControl() {
        Log.e(TAG, "Wificond died!");
        clearState();
        this.mWificond = null;
        WifiNative.WificondDeathEventHandler wificondDeathEventHandler = this.mDeathEventHandler;
        if (wificondDeathEventHandler != null) {
            wificondDeathEventHandler.onDeath();
        }
    }

    public void enableVerboseLogging(boolean enable) {
        this.mVerboseLoggingEnabled = enable;
    }

    public boolean initialize(WifiNative.WificondDeathEventHandler handler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = handler;
        tearDownInterfaces();
        return true;
    }

    private boolean retrieveWificondAndRegisterForDeath() {
        if (this.mWificond != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Wificond handle already retrieved");
            }
            return true;
        }
        this.mWificond = this.mWifiInjector.makeWificond();
        IWificond iWificond = this.mWificond;
        if (iWificond == null) {
            Log.e(TAG, "Failed to get reference to wificond");
            return false;
        }
        try {
            iWificond.asBinder().linkToDeath(this, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register death notification for wificond");
            return false;
        }
    }

    public IClientInterface setupInterfaceForClientMode(String ifaceName) {
        Log.d(TAG, "Setting up interface for client mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        try {
            IClientInterface clientInterface = this.mWificond.createClientInterface(ifaceName);
            if (clientInterface == null) {
                Log.e(TAG, "Could not get IClientInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(clientInterface.asBinder());
            this.mClientInterfaces.put(ifaceName, clientInterface);
            try {
                IWifiScannerImpl wificondScanner = clientInterface.getWifiScannerImpl();
                if (wificondScanner == null) {
                    Log.e(TAG, "Failed to get WificondScannerImpl");
                    return null;
                }
                this.mWificondScanners.put(ifaceName, wificondScanner);
                Binder.allowBlocking(wificondScanner.asBinder());
                ScanEventHandler scanEventHandler = new ScanEventHandler(ifaceName);
                this.mScanEventHandlers.put(ifaceName, scanEventHandler);
                wificondScanner.subscribeScanEvents(scanEventHandler);
                PnoScanEventHandler pnoScanEventHandler = new PnoScanEventHandler(ifaceName);
                this.mPnoScanEventHandlers.put(ifaceName, pnoScanEventHandler);
                wificondScanner.subscribePnoScanEvents(pnoScanEventHandler);
                return clientInterface;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to refresh wificond scanner due to remote exception");
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to get IClientInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownClientInterface(String ifaceName) {
        if (getClientInterface(ifaceName) == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return false;
        }
        try {
            IWifiScannerImpl scannerImpl = this.mWificondScanners.get(ifaceName);
            if (scannerImpl != null) {
                scannerImpl.unsubscribeScanEvents();
                scannerImpl.unsubscribePnoScanEvents();
            }
            try {
                if (!this.mWificond.tearDownClientInterface(ifaceName)) {
                    Log.e(TAG, "Failed to teardown client interface");
                    return false;
                }
                this.mClientInterfaces.remove(ifaceName);
                this.mWificondScanners.remove(ifaceName);
                this.mScanEventHandlers.remove(ifaceName);
                this.mPnoScanEventHandlers.remove(ifaceName);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to teardown client interface due to remote exception");
                return false;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to unsubscribe wificond scanner due to remote exception");
            return false;
        }
    }

    public IApInterface setupInterfaceForSoftApMode(String ifaceName) {
        Log.d(TAG, "Setting up interface for soft ap mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        try {
            IApInterface apInterface = this.mWificond.createApInterface(ifaceName);
            if (apInterface == null) {
                Log.e(TAG, "Could not get IApInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(apInterface.asBinder());
            this.mApInterfaces.put(ifaceName, apInterface);
            return apInterface;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get IApInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownSoftApInterface(String ifaceName) {
        if (getApInterface(ifaceName) == null) {
            Log.e(TAG, "No valid wificond ap interface handler");
            return false;
        }
        try {
            if (!this.mWificond.tearDownApInterface(ifaceName)) {
                Log.e(TAG, "Failed to teardown AP interface");
                return false;
            }
            this.mApInterfaces.remove(ifaceName);
            this.mApInterfaceListeners.remove(ifaceName);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to teardown AP interface due to remote exception");
            return false;
        }
    }

    public boolean tearDownInterfaces() {
        Log.d(TAG, "tearing down interfaces in wificond");
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            for (Map.Entry<String, IWifiScannerImpl> entry : this.mWificondScanners.entrySet()) {
                entry.getValue().unsubscribeScanEvents();
                entry.getValue().unsubscribePnoScanEvents();
            }
            this.mWificond.tearDownInterfaces();
            clearState();
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to tear down interfaces due to remote exception");
            return false;
        }
    }

    private IClientInterface getClientInterface(String ifaceName) {
        return this.mClientInterfaces.get(ifaceName);
    }

    public WifiNative.SignalPollResult signalPoll(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] resultArray = iface.signalPoll();
            if (resultArray == null || resultArray.length != 4) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            WifiNative.SignalPollResult pollResult = new WifiNative.SignalPollResult();
            pollResult.currentRssi = resultArray[0];
            pollResult.txBitrate = resultArray[1];
            pollResult.associationFrequency = resultArray[2];
            pollResult.rxBitrate = resultArray[3];
            return pollResult;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    public WifiNative.TxPacketCounters getTxPacketCounters(String ifaceName) {
        IClientInterface iface = getClientInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] resultArray = iface.getPacketCounters();
            if (resultArray == null || resultArray.length != 2) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            WifiNative.TxPacketCounters counters = new WifiNative.TxPacketCounters();
            counters.txSucceeded = resultArray[0];
            counters.txFailed = resultArray[1];
            return counters;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    private IWifiScannerImpl getScannerImpl(String ifaceName) {
        return this.mWificondScanners.get(ifaceName);
    }

    /* JADX WARNING: Removed duplicated region for block: B:75:0x01da  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.ArrayList<com.android.server.wifi.ScanDetail> getScanResults(java.lang.String r27, int r28) {
        /*
            r26 = this;
            r1 = r26
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r2 = r0
            android.net.wifi.IWifiScannerImpl r3 = r26.getScannerImpl(r27)
            java.lang.String r4 = "WificondControl"
            if (r3 != 0) goto L_0x0016
            java.lang.String r0 = "No valid wificond scanner interface handler"
            android.util.Log.e(r4, r0)
            return r2
        L_0x0016:
            if (r28 != 0) goto L_0x0023
            com.android.server.wifi.wificond.NativeScanResult[] r0 = r3.getScanResults()     // Catch:{ RemoteException -> 0x001e }
            r5 = r0
            goto L_0x0028
        L_0x001e:
            r0 = move-exception
            r20 = r3
            goto L_0x01d1
        L_0x0023:
            com.android.server.wifi.wificond.NativeScanResult[] r0 = r3.getPnoScanResults()     // Catch:{ RemoteException -> 0x01ce }
            r5 = r0
        L_0x0028:
            int r6 = r5.length     // Catch:{ RemoteException -> 0x01ce }
            r0 = 0
            r7 = r0
        L_0x002b:
            if (r7 >= r6) goto L_0x01c9
            r0 = r5[r7]     // Catch:{ RemoteException -> 0x01ce }
            r8 = r0
            byte[] r0 = r8.ssid     // Catch:{ RemoteException -> 0x01ce }
            android.net.wifi.WifiSsid r11 = android.net.wifi.WifiSsid.createFromByteArray(r0)     // Catch:{ RemoteException -> 0x01ce }
            byte[] r0 = r8.bssid     // Catch:{ IllegalArgumentException -> 0x01a3 }
            java.lang.String r0 = com.android.server.wifi.util.NativeUtil.macAddressFromByteArray(r0)     // Catch:{ IllegalArgumentException -> 0x01a3 }
            r15 = r0
            if (r15 != 0) goto L_0x004b
            java.lang.String r0 = "Illegal null bssid"
            android.util.Log.e(r4, r0)     // Catch:{ RemoteException -> 0x001e }
            r20 = r3
            r25 = r5
            goto L_0x01bf
        L_0x004b:
            byte[] r0 = r8.infoElement     // Catch:{ RemoteException -> 0x01ce }
            android.net.wifi.ScanResult$InformationElement[] r0 = com.android.server.wifi.util.InformationElementUtil.parseInformationElements(r0)     // Catch:{ RemoteException -> 0x01ce }
            r14 = r0
            com.android.server.wifi.util.InformationElementUtil$Capabilities r0 = new com.android.server.wifi.util.InformationElementUtil$Capabilities     // Catch:{ RemoteException -> 0x01ce }
            r0.<init>()     // Catch:{ RemoteException -> 0x01ce }
            r13 = r0
            java.util.BitSet r0 = r8.capability     // Catch:{ RemoteException -> 0x01ce }
            boolean r9 = r26.isEnhancedOpenSupported()     // Catch:{ RemoteException -> 0x01ce }
            r13.from(r14, r0, r9)     // Catch:{ RemoteException -> 0x01ce }
            java.lang.String r0 = r13.generateCapabilitiesString()     // Catch:{ RemoteException -> 0x01ce }
            r9 = r0
            com.android.server.wifi.hotspot2.NetworkDetail r10 = new com.android.server.wifi.hotspot2.NetworkDetail     // Catch:{ IllegalArgumentException -> 0x017c }
            r0 = 0
            int r12 = r8.frequency     // Catch:{ IllegalArgumentException -> 0x017c }
            r10.<init>(r15, r14, r0, r12)     // Catch:{ IllegalArgumentException -> 0x017c }
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r0 = com.samsung.android.net.wifi.OpBrandingLoader.Vendor.KTT     // Catch:{ RemoteException -> 0x01ce }
            com.samsung.android.net.wifi.OpBrandingLoader$Vendor r12 = mOpBranding     // Catch:{ RemoteException -> 0x01ce }
            r20 = r3
            r3 = 1
            if (r0 != r12) goto L_0x00da
            int r0 = r10.getChannelWidth()     // Catch:{ RemoteException -> 0x01c7 }
            if (r0 <= r3) goto L_0x0090
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x01c7 }
            r0.<init>()     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r9)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = "[VHT]"
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r0 = r0.toString()     // Catch:{ RemoteException -> 0x01c7 }
            r9 = r0
        L_0x0090:
            boolean r0 = DBG     // Catch:{ RemoteException -> 0x01c7 }
            if (r0 == 0) goto L_0x00d8
            java.lang.String r0 = r13.getKTVsd()     // Catch:{ RemoteException -> 0x01c7 }
            if (r0 == 0) goto L_0x00d8
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x01c7 }
            r0.<init>()     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = "get scan results with KT OUI: VSD = "
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = r13.getKTVsd()     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = ", ssid = "
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = r11.toString()     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = ", bssid = "
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r15)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = ", freq = "
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            int r12 = r8.frequency     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r12 = ", flags = "
            r0.append(r12)     // Catch:{ RemoteException -> 0x01c7 }
            r0.append(r9)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r0 = r0.toString()     // Catch:{ RemoteException -> 0x01c7 }
            android.util.Log.d(r4, r0)     // Catch:{ RemoteException -> 0x01c7 }
        L_0x00d8:
            r0 = r9
            goto L_0x00db
        L_0x00da:
            r0 = r9
        L_0x00db:
            com.android.server.wifi.ScanDetail r21 = new com.android.server.wifi.ScanDetail     // Catch:{ RemoteException -> 0x01c7 }
            int r9 = r8.signalMbm     // Catch:{ RemoteException -> 0x01c7 }
            int r16 = r9 / 100
            int r12 = r8.frequency     // Catch:{ RemoteException -> 0x01c7 }
            r22 = r4
            long r3 = r8.tsf     // Catch:{ RemoteException -> 0x019f }
            r19 = 0
            r9 = r21
            r17 = r12
            r12 = r15
            r23 = r13
            r13 = r0
            r24 = r14
            r14 = r16
            r25 = r5
            r5 = r15
            r15 = r17
            r16 = r3
            r18 = r24
            r9.<init>(r10, r11, r12, r13, r14, r15, r16, r18, r19)     // Catch:{ RemoteException -> 0x019f }
            r3 = r21
            android.net.wifi.ScanResult r4 = r3.getScanResult()     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult r9 = r3.getScanResult()     // Catch:{ RemoteException -> 0x019f }
            boolean r9 = com.android.server.wifi.util.ScanResultUtil.isScanResultForEapNetwork(r9)     // Catch:{ RemoteException -> 0x019f }
            if (r9 == 0) goto L_0x0138
            com.android.server.wifi.CarrierNetworkConfig r9 = r1.mCarrierNetworkConfig     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r12 = r11.toString()     // Catch:{ RemoteException -> 0x019f }
            boolean r9 = r9.isCarrierNetwork(r12)     // Catch:{ RemoteException -> 0x019f }
            if (r9 == 0) goto L_0x0138
            r9 = 1
            r4.isCarrierAp = r9     // Catch:{ RemoteException -> 0x019f }
            com.android.server.wifi.CarrierNetworkConfig r9 = r1.mCarrierNetworkConfig     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r12 = r11.toString()     // Catch:{ RemoteException -> 0x019f }
            int r9 = r9.getNetworkEapType(r12)     // Catch:{ RemoteException -> 0x019f }
            r4.carrierApEapType = r9     // Catch:{ RemoteException -> 0x019f }
            com.android.server.wifi.CarrierNetworkConfig r9 = r1.mCarrierNetworkConfig     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r12 = r11.toString()     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r9 = r9.getCarrierName(r12)     // Catch:{ RemoteException -> 0x019f }
            r4.carrierName = r9     // Catch:{ RemoteException -> 0x019f }
        L_0x0138:
            java.util.ArrayList<com.android.server.wifi.wificond.RadioChainInfo> r9 = r8.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            if (r9 == 0) goto L_0x0176
            java.util.ArrayList<com.android.server.wifi.wificond.RadioChainInfo> r9 = r8.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            int r9 = r9.size()     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult$RadioChainInfo[] r9 = new android.net.wifi.ScanResult.RadioChainInfo[r9]     // Catch:{ RemoteException -> 0x019f }
            r4.radioChainInfos = r9     // Catch:{ RemoteException -> 0x019f }
            r9 = 0
            java.util.ArrayList<com.android.server.wifi.wificond.RadioChainInfo> r12 = r8.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            java.util.Iterator r12 = r12.iterator()     // Catch:{ RemoteException -> 0x019f }
        L_0x014d:
            boolean r13 = r12.hasNext()     // Catch:{ RemoteException -> 0x019f }
            if (r13 == 0) goto L_0x0176
            java.lang.Object r13 = r12.next()     // Catch:{ RemoteException -> 0x019f }
            com.android.server.wifi.wificond.RadioChainInfo r13 = (com.android.server.wifi.wificond.RadioChainInfo) r13     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult$RadioChainInfo[] r14 = r4.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult$RadioChainInfo r15 = new android.net.wifi.ScanResult$RadioChainInfo     // Catch:{ RemoteException -> 0x019f }
            r15.<init>()     // Catch:{ RemoteException -> 0x019f }
            r14[r9] = r15     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult$RadioChainInfo[] r14 = r4.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            r14 = r14[r9]     // Catch:{ RemoteException -> 0x019f }
            int r15 = r13.chainId     // Catch:{ RemoteException -> 0x019f }
            r14.id = r15     // Catch:{ RemoteException -> 0x019f }
            android.net.wifi.ScanResult$RadioChainInfo[] r14 = r4.radioChainInfos     // Catch:{ RemoteException -> 0x019f }
            r14 = r14[r9]     // Catch:{ RemoteException -> 0x019f }
            int r15 = r13.level     // Catch:{ RemoteException -> 0x019f }
            r14.level = r15     // Catch:{ RemoteException -> 0x019f }
            int r9 = r9 + 1
            goto L_0x014d
        L_0x0176:
            r2.add(r3)     // Catch:{ RemoteException -> 0x019f }
            r4 = r22
            goto L_0x01bf
        L_0x017c:
            r0 = move-exception
            r20 = r3
            r22 = r4
            r25 = r5
            r23 = r13
            r24 = r14
            r5 = r15
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x019f }
            r3.<init>()     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r4 = "Illegal argument for scan result with bssid: "
            r3.append(r4)     // Catch:{ RemoteException -> 0x019f }
            r3.append(r5)     // Catch:{ RemoteException -> 0x019f }
            java.lang.String r3 = r3.toString()     // Catch:{ RemoteException -> 0x019f }
            r4 = r22
            android.util.Log.e(r4, r3, r0)     // Catch:{ RemoteException -> 0x01c7 }
            goto L_0x01bf
        L_0x019f:
            r0 = move-exception
            r4 = r22
            goto L_0x01d1
        L_0x01a3:
            r0 = move-exception
            r20 = r3
            r25 = r5
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x01c7 }
            r3.<init>()     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r5 = "Illegal argument "
            r3.append(r5)     // Catch:{ RemoteException -> 0x01c7 }
            byte[] r5 = r8.bssid     // Catch:{ RemoteException -> 0x01c7 }
            r3.append(r5)     // Catch:{ RemoteException -> 0x01c7 }
            java.lang.String r3 = r3.toString()     // Catch:{ RemoteException -> 0x01c7 }
            android.util.Log.e(r4, r3, r0)     // Catch:{ RemoteException -> 0x01c7 }
        L_0x01bf:
            int r7 = r7 + 1
            r3 = r20
            r5 = r25
            goto L_0x002b
        L_0x01c7:
            r0 = move-exception
            goto L_0x01d1
        L_0x01c9:
            r20 = r3
            r25 = r5
            goto L_0x01d6
        L_0x01ce:
            r0 = move-exception
            r20 = r3
        L_0x01d1:
            java.lang.String r3 = "Failed to create ScanDetail ArrayList"
            android.util.Log.e(r4, r3)
        L_0x01d6:
            boolean r0 = r1.mVerboseLoggingEnabled
            if (r0 == 0) goto L_0x01f7
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "get "
            r0.append(r3)
            int r3 = r2.size()
            r0.append(r3)
            java.lang.String r3 = " scan results from wificond"
            r0.append(r3)
            java.lang.String r0 = r0.toString()
            android.util.Log.d(r4, r0)
        L_0x01f7:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WificondControl.getScanResults(java.lang.String, int):java.util.ArrayList");
    }

    public NativeScanResult[] getRawScanResults(String ifaceName) {
        IWifiScannerImpl mWificondScanner = getScannerImpl(ifaceName);
        if (mWificondScanner == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return null;
        }
        try {
            return mWificondScanner.getScanResults();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get native scan results");
            return null;
        }
    }

    private static int getScanType(int scanType) {
        if (scanType == 0) {
            return 0;
        }
        if (scanType == 1) {
            return 1;
        }
        if (scanType == 2) {
            return 2;
        }
        throw new IllegalArgumentException("Invalid scan type " + scanType);
    }

    public boolean scan(String ifaceName, int scanType, Set<Integer> freqs, List<String> hiddenNetworkSSIDs) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        SingleScanSettings settings = new SingleScanSettings();
        try {
            settings.scanType = getScanType(scanType);
            settings.channelSettings = new ArrayList<>();
            settings.hiddenNetworks = new ArrayList<>();
            if (freqs != null) {
                for (Integer freq : freqs) {
                    ChannelSettings channel = new ChannelSettings();
                    channel.frequency = freq.intValue();
                    settings.channelSettings.add(channel);
                }
            }
            if (hiddenNetworkSSIDs != null) {
                for (String ssid : hiddenNetworkSSIDs) {
                    HiddenNetwork network = new HiddenNetwork();
                    try {
                        network.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(ssid));
                        if (!settings.hiddenNetworks.contains(network)) {
                            settings.hiddenNetworks.add(network);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Illegal argument " + ssid, e);
                    }
                }
            }
            try {
                return scannerImpl.scan(settings);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to request scan due to remote exception");
                return false;
            }
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "Invalid scan type ", e3);
            return false;
        }
    }

    public int getMaxNumScanSsids(String ifaceName) {
        IWifiScannerImpl mWificondScanner = getScannerImpl(ifaceName);
        if (mWificondScanner == null) {
            Log.e(TAG, "No valid wificond scanner interface handler, getMaxNumScanSsids");
            return 0;
        }
        try {
            return mWificondScanner.getMaxNumScanSsids();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to getMaxNumScanSsids due to remote exception");
            return 0;
        }
    }

    public void disableRandomMac(String ifaceName) {
        IWifiScannerImpl mWificondScanner = getScannerImpl(ifaceName);
        if (mWificondScanner == null) {
            Log.e(TAG, "No valid wificond scanner interface handler, disableRandomMac");
        }
        try {
            mWificondScanner.disableRandomMac();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request disable random mac due to remote exception");
        }
    }

    public boolean startPnoScan(String ifaceName, WifiNative.PnoSettings pnoSettings) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        PnoSettings settings = new PnoSettings();
        settings.pnoNetworks = new ArrayList<>();
        settings.intervalMs = pnoSettings.periodInMs;
        settings.min2gRssi = pnoSettings.min24GHzRssi;
        settings.min5gRssi = pnoSettings.min5GHzRssi;
        if (pnoSettings.networkList != null) {
            for (WifiNative.PnoNetwork network : pnoSettings.networkList) {
                PnoNetwork condNetwork = new PnoNetwork();
                boolean z = true;
                if ((network.flags & 1) == 0) {
                    z = false;
                }
                condNetwork.isHidden = z;
                try {
                    condNetwork.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(network.ssid));
                    condNetwork.frequencies = network.frequencies;
                    settings.pnoNetworks.add(condNetwork);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + network.ssid, e);
                }
            }
        }
        try {
            boolean success = scannerImpl.startPnoScan(settings);
            this.mWifiInjector.getWifiMetrics().incrementPnoScanStartAttempCount();
            if (!success) {
                this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
            }
            return success;
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to start pno scan due to remote exception");
            return false;
        }
    }

    public boolean stopPnoScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        try {
            return scannerImpl.stopPnoScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop pno scan due to remote exception");
            return false;
        }
    }

    public void abortScan(String ifaceName) {
        IWifiScannerImpl scannerImpl = getScannerImpl(ifaceName);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return;
        }
        try {
            scannerImpl.abortScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request abortScan due to remote exception");
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    public int[] getChannelsForBand(int band) {
        IWificond iWificond = this.mWificond;
        if (iWificond == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return null;
        } else if (band == 1) {
            return iWificond.getAvailable2gChannels();
        } else {
            if (band == 2) {
                return iWificond.getAvailable5gNonDFSChannels();
            }
            if (band == 4) {
                try {
                    return iWificond.getAvailableDFSChannels();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to request getChannelsForBand due to remote exception");
                    return null;
                }
            } else {
                throw new IllegalArgumentException("unsupported band " + band);
            }
        }
    }

    private IApInterface getApInterface(String ifaceName) {
        return this.mApInterfaces.get(ifaceName);
    }

    public boolean registerApListener(String ifaceName, WifiNative.SoftApListener listener) {
        IApInterface iface = getApInterface(ifaceName);
        if (iface == null) {
            Log.e(TAG, "No valid ap interface handler");
            return false;
        }
        try {
            IApInterfaceEventCallback callback = new ApInterfaceEventCallback(listener);
            this.mApInterfaceListeners.put(ifaceName, callback);
            if (iface.registerCallback(callback)) {
                return true;
            }
            Log.e(TAG, "Failed to register ap callback.");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in registering AP callback: " + e);
            return false;
        }
    }

    public void sendMgmtFrame(String ifaceName, byte[] frame, WifiNative.SendMgmtFrameCallback callback, int mcs) {
        if (callback == null) {
            Log.e(TAG, "callback cannot be null!");
        } else if (frame == null) {
            Log.e(TAG, "frame cannot be null!");
            callback.onFailure(1);
        } else {
            IClientInterface clientInterface = getClientInterface(ifaceName);
            if (clientInterface == null) {
                Log.e(TAG, "No valid wificond client interface handler");
                callback.onFailure(1);
            } else if (!this.mSendMgmtFrameInProgress.compareAndSet(false, true)) {
                Log.e(TAG, "An existing management frame transmission is in progress!");
                callback.onFailure(5);
            } else {
                SendMgmtFrameEvent sendMgmtFrameEvent = new SendMgmtFrameEvent(callback);
                try {
                    clientInterface.SendMgmtFrame(frame, sendMgmtFrameEvent, mcs);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while starting link probe: " + e);
                    sendMgmtFrameEvent.OnFailure(1);
                }
            }
        }
    }

    private void clearState() {
        this.mClientInterfaces.clear();
        this.mWificondScanners.clear();
        this.mPnoScanEventHandlers.clear();
        this.mScanEventHandlers.clear();
        this.mApInterfaces.clear();
        this.mApInterfaceListeners.clear();
        this.mSendMgmtFrameInProgress.set(false);
    }

    private boolean isEnhancedOpenSupported() {
        if (this.mIsEnhancedOpenSupportedInitialized) {
            return this.mIsEnhancedOpenSupported;
        }
        boolean z = false;
        if (this.mWifiNative == null) {
            this.mWifiNative = this.mWifiInjector.getWifiNative();
            if (this.mWifiNative == null) {
                return false;
            }
        }
        String iface = this.mWifiNative.getClientInterfaceName();
        if (iface == null) {
            return false;
        }
        this.mIsEnhancedOpenSupportedInitialized = true;
        if ((this.mWifiNative.getSupportedFeatureSet(iface) & 536870912) != 0) {
            z = true;
        }
        this.mIsEnhancedOpenSupported = z;
        return this.mIsEnhancedOpenSupported;
    }
}
