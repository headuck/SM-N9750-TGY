package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.Handler;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.HalDeviceManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiAwareNativeManager {
    private static final String TAG = "WifiAwareNativeManager";
    private static final boolean VDBG = true;
    boolean mDbg = true;
    /* access modifiers changed from: private */
    public HalDeviceManager mHalDeviceManager;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public InterfaceAvailableForRequestListener mInterfaceAvailableForRequestListener = new InterfaceAvailableForRequestListener();
    private InterfaceDestroyedListener mInterfaceDestroyedListener;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private int mReferenceCount = 0;
    private WifiAwareNativeCallback mWifiAwareNativeCallback;
    /* access modifiers changed from: private */
    public WifiAwareStateManager mWifiAwareStateManager;
    /* access modifiers changed from: private */
    public IWifiNanIface mWifiNanIface = null;

    WifiAwareNativeManager(WifiAwareStateManager awareStateManager, HalDeviceManager halDeviceManager, WifiAwareNativeCallback wifiAwareNativeCallback) {
        this.mWifiAwareStateManager = awareStateManager;
        this.mHalDeviceManager = halDeviceManager;
        this.mWifiAwareNativeCallback = wifiAwareNativeCallback;
    }

    public android.hardware.wifi.V1_2.IWifiNanIface mockableCastTo_1_2(IWifiNanIface iface) {
        return android.hardware.wifi.V1_2.IWifiNanIface.castFrom(iface);
    }

    public void start(Handler handler) {
        this.mHandler = handler;
        this.mHalDeviceManager.initialize();
        this.mHalDeviceManager.registerStatusListener(new HalDeviceManager.ManagerStatusListener() {
            public void onStatusChanged() {
                Log.v(WifiAwareNativeManager.TAG, "onStatusChanged");
                if (WifiAwareNativeManager.this.mHalDeviceManager.isStarted()) {
                    WifiAwareNativeManager.this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, WifiAwareNativeManager.this.mInterfaceAvailableForRequestListener, WifiAwareNativeManager.this.mHandler);
                } else {
                    WifiAwareNativeManager.this.awareIsDown();
                }
            }
        }, this.mHandler);
        if (this.mHalDeviceManager.isStarted()) {
            this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, this.mInterfaceAvailableForRequestListener, this.mHandler);
        }
    }

    @VisibleForTesting
    public IWifiNanIface getWifiNanIface() {
        IWifiNanIface iWifiNanIface;
        synchronized (this.mLock) {
            iWifiNanIface = this.mWifiNanIface;
        }
        return iWifiNanIface;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00b4, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void tryToGetAware() {
        /*
            r7 = this;
            java.lang.Object r0 = r7.mLock
            monitor-enter(r0)
            boolean r1 = r7.mDbg     // Catch:{ all -> 0x00d1 }
            if (r1 == 0) goto L_0x0029
            java.lang.String r1 = "WifiAwareNativeManager"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x00d1 }
            r2.<init>()     // Catch:{ all -> 0x00d1 }
            java.lang.String r3 = "tryToGetAware: mWifiNanIface="
            r2.append(r3)     // Catch:{ all -> 0x00d1 }
            android.hardware.wifi.V1_0.IWifiNanIface r3 = r7.mWifiNanIface     // Catch:{ all -> 0x00d1 }
            r2.append(r3)     // Catch:{ all -> 0x00d1 }
            java.lang.String r3 = ", mReferenceCount="
            r2.append(r3)     // Catch:{ all -> 0x00d1 }
            int r3 = r7.mReferenceCount     // Catch:{ all -> 0x00d1 }
            r2.append(r3)     // Catch:{ all -> 0x00d1 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x00d1 }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x00d1 }
        L_0x0029:
            android.hardware.wifi.V1_0.IWifiNanIface r1 = r7.mWifiNanIface     // Catch:{ all -> 0x00d1 }
            if (r1 == 0) goto L_0x002f
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            return
        L_0x002f:
            com.android.server.wifi.HalDeviceManager r1 = r7.mHalDeviceManager     // Catch:{ all -> 0x00d1 }
            if (r1 != 0) goto L_0x003f
            java.lang.String r1 = "WifiAwareNativeManager"
            java.lang.String r2 = "tryToGetAware: mHalDeviceManager is null!?"
            android.util.Log.e(r1, r2)     // Catch:{ all -> 0x00d1 }
            r7.awareIsDown()     // Catch:{ all -> 0x00d1 }
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            return
        L_0x003f:
            com.android.server.wifi.aware.WifiAwareNativeManager$InterfaceDestroyedListener r1 = new com.android.server.wifi.aware.WifiAwareNativeManager$InterfaceDestroyedListener     // Catch:{ all -> 0x00d1 }
            r2 = 0
            r1.<init>()     // Catch:{ all -> 0x00d1 }
            r7.mInterfaceDestroyedListener = r1     // Catch:{ all -> 0x00d1 }
            com.android.server.wifi.HalDeviceManager r1 = r7.mHalDeviceManager     // Catch:{ all -> 0x00d1 }
            com.android.server.wifi.aware.WifiAwareNativeManager$InterfaceDestroyedListener r2 = r7.mInterfaceDestroyedListener     // Catch:{ all -> 0x00d1 }
            android.os.Handler r3 = r7.mHandler     // Catch:{ all -> 0x00d1 }
            android.hardware.wifi.V1_0.IWifiNanIface r1 = r1.createNanIface(r2, r3)     // Catch:{ all -> 0x00d1 }
            if (r1 != 0) goto L_0x005e
            java.lang.String r2 = "WifiAwareNativeManager"
            java.lang.String r3 = "Was not able to obtain an IWifiNanIface (even though enabled!?)"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x00d1 }
            r7.awareIsDown()     // Catch:{ all -> 0x00d1 }
            goto L_0x00b3
        L_0x005e:
            boolean r2 = r7.mDbg     // Catch:{ all -> 0x00d1 }
            if (r2 == 0) goto L_0x0069
            java.lang.String r2 = "WifiAwareNativeManager"
            java.lang.String r3 = "Obtained an IWifiNanIface"
            android.util.Log.v(r2, r3)     // Catch:{ all -> 0x00d1 }
        L_0x0069:
            android.hardware.wifi.V1_2.IWifiNanIface r2 = r7.mockableCastTo_1_2(r1)     // Catch:{ RemoteException -> 0x00b5 }
            r3 = 1
            if (r2 != 0) goto L_0x007c
            com.android.server.wifi.aware.WifiAwareNativeCallback r4 = r7.mWifiAwareNativeCallback     // Catch:{ RemoteException -> 0x00b5 }
            r5 = 0
            r4.mIsHal12OrLater = r5     // Catch:{ RemoteException -> 0x00b5 }
            com.android.server.wifi.aware.WifiAwareNativeCallback r4 = r7.mWifiAwareNativeCallback     // Catch:{ RemoteException -> 0x00b5 }
            android.hardware.wifi.V1_0.WifiStatus r4 = r1.registerEventCallback(r4)     // Catch:{ RemoteException -> 0x00b5 }
            goto L_0x0086
        L_0x007c:
            com.android.server.wifi.aware.WifiAwareNativeCallback r4 = r7.mWifiAwareNativeCallback     // Catch:{ RemoteException -> 0x00b5 }
            r4.mIsHal12OrLater = r3     // Catch:{ RemoteException -> 0x00b5 }
            com.android.server.wifi.aware.WifiAwareNativeCallback r4 = r7.mWifiAwareNativeCallback     // Catch:{ RemoteException -> 0x00b5 }
            android.hardware.wifi.V1_0.WifiStatus r4 = r2.registerEventCallback_1_2(r4)     // Catch:{ RemoteException -> 0x00b5 }
        L_0x0086:
            int r5 = r4.code     // Catch:{ RemoteException -> 0x00b5 }
            if (r5 == 0) goto L_0x00ae
            java.lang.String r3 = "WifiAwareNativeManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x00b5 }
            r5.<init>()     // Catch:{ RemoteException -> 0x00b5 }
            java.lang.String r6 = "IWifiNanIface.registerEventCallback error: "
            r5.append(r6)     // Catch:{ RemoteException -> 0x00b5 }
            java.lang.String r6 = statusString(r4)     // Catch:{ RemoteException -> 0x00b5 }
            r5.append(r6)     // Catch:{ RemoteException -> 0x00b5 }
            java.lang.String r5 = r5.toString()     // Catch:{ RemoteException -> 0x00b5 }
            android.util.Log.e(r3, r5)     // Catch:{ RemoteException -> 0x00b5 }
            com.android.server.wifi.HalDeviceManager r3 = r7.mHalDeviceManager     // Catch:{ RemoteException -> 0x00b5 }
            r3.removeIface(r1)     // Catch:{ RemoteException -> 0x00b5 }
            r7.awareIsDown()     // Catch:{ RemoteException -> 0x00b5 }
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            return
        L_0x00ae:
            r7.mWifiNanIface = r1     // Catch:{ all -> 0x00d1 }
            r7.mReferenceCount = r3     // Catch:{ all -> 0x00d1 }
        L_0x00b3:
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            return
        L_0x00b5:
            r2 = move-exception
            java.lang.String r3 = "WifiAwareNativeManager"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x00d1 }
            r4.<init>()     // Catch:{ all -> 0x00d1 }
            java.lang.String r5 = "IWifiNanIface.registerEventCallback exception: "
            r4.append(r5)     // Catch:{ all -> 0x00d1 }
            r4.append(r2)     // Catch:{ all -> 0x00d1 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x00d1 }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x00d1 }
            r7.awareIsDown()     // Catch:{ all -> 0x00d1 }
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            return
        L_0x00d1:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x00d1 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.aware.WifiAwareNativeManager.tryToGetAware():void");
    }

    public void releaseAware() {
        if (this.mDbg) {
            Log.d(TAG, "releaseAware: mWifiNanIface=" + this.mWifiNanIface + ", mReferenceCount=" + this.mReferenceCount);
        }
        if (this.mWifiNanIface != null) {
            if (this.mHalDeviceManager == null) {
                Log.e(TAG, "releaseAware: mHalDeviceManager is null!?");
                return;
            }
            synchronized (this.mLock) {
                this.mReferenceCount--;
                if (this.mReferenceCount == 0) {
                    this.mInterfaceDestroyedListener.active = false;
                    this.mInterfaceDestroyedListener = null;
                    this.mHalDeviceManager.removeIface(this.mWifiNanIface);
                    this.mWifiNanIface = null;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void awareIsDown() {
        synchronized (this.mLock) {
            if (this.mDbg) {
                Log.d(TAG, "awareIsDown: mWifiNanIface=" + this.mWifiNanIface + ", mReferenceCount =" + this.mReferenceCount);
            }
            this.mWifiNanIface = null;
            this.mReferenceCount = 0;
            this.mWifiAwareStateManager.onAwaredownResponseTimeout();
            this.mWifiAwareStateManager.disableUsage();
        }
    }

    private class InterfaceDestroyedListener implements HalDeviceManager.InterfaceDestroyedListener {
        public boolean active;

        private InterfaceDestroyedListener() {
            this.active = true;
        }

        public void onDestroyed(String ifaceName) {
            if (WifiAwareNativeManager.this.mDbg) {
                Log.d(WifiAwareNativeManager.TAG, "Interface was destroyed: mWifiNanIface=" + WifiAwareNativeManager.this.mWifiNanIface + ", active=" + this.active);
            }
            if (this.active && WifiAwareNativeManager.this.mWifiNanIface != null) {
                WifiAwareNativeManager.this.awareIsDown();
            }
        }
    }

    private class InterfaceAvailableForRequestListener implements HalDeviceManager.InterfaceAvailableForRequestListener {
        private InterfaceAvailableForRequestListener() {
        }

        public void onAvailabilityChanged(boolean isAvailable) {
            if (WifiAwareNativeManager.this.mDbg) {
                Log.d(WifiAwareNativeManager.TAG, "Interface availability = " + isAvailable + ", mWifiNanIface=" + WifiAwareNativeManager.this.mWifiNanIface);
            }
            synchronized (WifiAwareNativeManager.this.mLock) {
                if (isAvailable) {
                    WifiAwareNativeManager.this.mWifiAwareStateManager.enableUsage();
                } else if (WifiAwareNativeManager.this.mWifiNanIface == null) {
                    WifiAwareNativeManager.this.mWifiAwareStateManager.disableUsage();
                }
            }
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        return status.code + " (" + status.description + ")";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeManager:");
        pw.println("  mWifiNanIface: " + this.mWifiNanIface);
        pw.println("  mReferenceCount: " + this.mReferenceCount);
        this.mWifiAwareNativeCallback.dump(fd, pw, args);
        this.mHalDeviceManager.dump(fd, pw, args);
    }
}
