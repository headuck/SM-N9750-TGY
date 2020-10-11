package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifi;
import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChipEventCallback;
import android.hardware.wifi.V1_0.IWifiEventCallback;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hidl.manager.V1_0.IServiceNotification;
import android.hidl.manager.V1_2.IServiceManager;
import android.os.Handler;
import android.os.HidlSupport;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.HalDeviceManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vendor.samsung.hardware.wifi.V2_0.ISehWifi;

public class HalDeviceManager {
    /* access modifiers changed from: private */
    public static final int[] IFACE_TYPES_BY_PRIORITY = {1, 0, 2, 3};
    private static final int START_HAL_RETRY_INTERVAL_MS = 20;
    @VisibleForTesting
    public static final int START_HAL_RETRY_TIMES = 3;
    private static final String TAG = "HalDevMgr";
    private static final boolean VDBG = true;
    private final Clock mClock;
    private boolean mDbg = true;
    private final SparseArray<IWifiChipEventCallback.Stub> mDebugCallbacks = new SparseArray<>();
    private final IHwBinder.DeathRecipient mISehWifiDeathRecipient = new IHwBinder.DeathRecipient() {
        public final void serviceDied(long j) {
            HalDeviceManager.this.lambda$new$3$HalDeviceManager(j);
        }
    };
    private final IHwBinder.DeathRecipient mIWifiDeathRecipient = new IHwBinder.DeathRecipient() {
        public final void serviceDied(long j) {
            HalDeviceManager.this.lambda$new$2$HalDeviceManager(j);
        }
    };
    private IWifiRttController mIWifiRttController;
    private final SparseArray<Map<InterfaceAvailableForRequestListenerProxy, Boolean>> mInterfaceAvailableForRequestListeners = new SparseArray<>();
    private final Map<Pair<String, Integer>, InterfaceCacheEntry> mInterfaceInfoCache = new HashMap();
    private boolean mIsReady;
    private boolean mIsVendorHalSupported = false;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private final Set<ManagerStatusListenerProxy> mManagerStatusListeners = new HashSet();
    private final Set<InterfaceRttControllerLifecycleCallbackProxy> mRttControllerLifecycleCallbacks = new HashSet();
    private ISehWifi mSehWifi;
    private IServiceManager mServiceManager;
    private final IHwBinder.DeathRecipient mServiceManagerDeathRecipient = new IHwBinder.DeathRecipient() {
        public final void serviceDied(long j) {
            HalDeviceManager.this.lambda$new$1$HalDeviceManager(j);
        }
    };
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.d(HalDeviceManager.TAG, "IWifi registration notification: fqName=" + fqName + ", name=" + name + ", preexisting=" + preexisting);
            synchronized (HalDeviceManager.this.mLock) {
                HalDeviceManager.this.initIWifiIfNecessary();
                HalDeviceManager.this.initISehWifiIfNeccessary();
            }
        }
    };
    private IWifi mWifi;
    private final WifiEventCallback mWifiEventCallback = new WifiEventCallback();

    public interface InterfaceAvailableForRequestListener {
        void onAvailabilityChanged(boolean z);
    }

    public interface InterfaceDestroyedListener {
        void onDestroyed(String str);
    }

    public interface InterfaceRttControllerLifecycleCallback {
        void onNewRttController(IWifiRttController iWifiRttController);

        void onRttControllerDestroyed();
    }

    public interface ManagerStatusListener {
        void onStatusChanged();
    }

    public HalDeviceManager(Clock clock) {
        this.mClock = clock;
        this.mInterfaceAvailableForRequestListeners.put(0, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(1, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(2, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(3, new HashMap());
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
        this.mDbg = true;
    }

    public void initialize() {
        initializeInternal();
    }

    public void registerStatusListener(ManagerStatusListener listener, Handler handler) {
        synchronized (this.mLock) {
            if (!this.mManagerStatusListeners.add(new ManagerStatusListenerProxy(listener, handler))) {
                Log.w(TAG, "registerStatusListener: duplicate registration ignored");
            }
        }
    }

    public boolean isSupported() {
        return this.mIsVendorHalSupported;
    }

    public boolean isReady() {
        return this.mIsReady;
    }

    public boolean isStarted() {
        return isWifiStarted();
    }

    public boolean start() {
        return startWifi();
    }

    public void stop() {
        stopWifi();
        this.mWifi = null;
    }

    public Set<Integer> getSupportedIfaceTypes() {
        return getSupportedIfaceTypesInternal((IWifiChip) null);
    }

    public Set<Integer> getSupportedIfaceTypes(IWifiChip chip) {
        return getSupportedIfaceTypesInternal(chip);
    }

    public IWifiStaIface createStaIface(boolean lowPrioritySta, InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiStaIface) createIface(0, lowPrioritySta, destroyedListener, handler);
    }

    public IWifiApIface createApIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiApIface) createIface(1, false, destroyedListener, handler);
    }

    public IWifiP2pIface createP2pIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiP2pIface) createIface(2, false, destroyedListener, handler);
    }

    public IWifiNanIface createNanIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiNanIface) createIface(3, false, destroyedListener, handler);
    }

    public boolean removeIface(IWifiIface iface) {
        boolean success = removeIfaceInternal(iface);
        dispatchAvailableForRequestListeners();
        return success;
    }

    public IWifiChip getChip(IWifiIface iface) {
        String name = getName(iface);
        int type = getType(iface);
        Log.d(TAG, "getChip: iface(name)=" + name);
        synchronized (this.mLock) {
            InterfaceCacheEntry cacheEntry = this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (cacheEntry == null) {
                Log.e(TAG, "getChip: no entry for iface(name)=" + name);
                return null;
            }
            IWifiChip iWifiChip = cacheEntry.chip;
            return iWifiChip;
        }
    }

    public boolean registerDestroyedListener(IWifiIface iface, InterfaceDestroyedListener destroyedListener, Handler handler) {
        String name = getName(iface);
        int type = getType(iface);
        Log.d(TAG, "registerDestroyedListener: iface(name)=" + name);
        synchronized (this.mLock) {
            InterfaceCacheEntry cacheEntry = this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (cacheEntry == null) {
                Log.e(TAG, "registerDestroyedListener: no entry for iface(name)=" + name);
                return false;
            }
            boolean add = cacheEntry.destroyedListeners.add(new InterfaceDestroyedListenerProxy(name, destroyedListener, handler));
            return add;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0062, code lost:
        r0 = getAllChipInfo();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0066, code lost:
        if (r0 != null) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0068, code lost:
        android.util.Log.e(TAG, "registerInterfaceAvailableForRequestListener: no chip info found - but possibly registered pre-started - ignoring");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x006f, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0070, code lost:
        dispatchAvailableForRequestListenersForType(r6, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0073, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerInterfaceAvailableForRequestListener(int r6, com.android.server.wifi.HalDeviceManager.InterfaceAvailableForRequestListener r7, android.os.Handler r8) {
        /*
            r5 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "registerInterfaceAvailableForRequestListener: ifaceType="
            r0.append(r1)
            r0.append(r6)
            java.lang.String r1 = ", listener="
            r0.append(r1)
            r0.append(r7)
            java.lang.String r1 = ", handler="
            r0.append(r1)
            r0.append(r8)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.d(r1, r0)
            java.lang.Object r0 = r5.mLock
            monitor-enter(r0)
            com.android.server.wifi.HalDeviceManager$InterfaceAvailableForRequestListenerProxy r1 = new com.android.server.wifi.HalDeviceManager$InterfaceAvailableForRequestListenerProxy     // Catch:{ all -> 0x0074 }
            r1.<init>(r7, r8)     // Catch:{ all -> 0x0074 }
            android.util.SparseArray<java.util.Map<com.android.server.wifi.HalDeviceManager$InterfaceAvailableForRequestListenerProxy, java.lang.Boolean>> r2 = r5.mInterfaceAvailableForRequestListeners     // Catch:{ all -> 0x0074 }
            java.lang.Object r2 = r2.get(r6)     // Catch:{ all -> 0x0074 }
            java.util.Map r2 = (java.util.Map) r2     // Catch:{ all -> 0x0074 }
            boolean r2 = r2.containsKey(r1)     // Catch:{ all -> 0x0074 }
            if (r2 == 0) goto L_0x0054
            java.lang.String r2 = "HalDevMgr"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0074 }
            r3.<init>()     // Catch:{ all -> 0x0074 }
            java.lang.String r4 = "registerInterfaceAvailableForRequestListener: dup listener skipped: "
            r3.append(r4)     // Catch:{ all -> 0x0074 }
            r3.append(r7)     // Catch:{ all -> 0x0074 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0074 }
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x0074 }
            monitor-exit(r0)     // Catch:{ all -> 0x0074 }
            return
        L_0x0054:
            android.util.SparseArray<java.util.Map<com.android.server.wifi.HalDeviceManager$InterfaceAvailableForRequestListenerProxy, java.lang.Boolean>> r2 = r5.mInterfaceAvailableForRequestListeners     // Catch:{ all -> 0x0074 }
            java.lang.Object r2 = r2.get(r6)     // Catch:{ all -> 0x0074 }
            java.util.Map r2 = (java.util.Map) r2     // Catch:{ all -> 0x0074 }
            r3 = 0
            r2.put(r1, r3)     // Catch:{ all -> 0x0074 }
            monitor-exit(r0)     // Catch:{ all -> 0x0074 }
            com.android.server.wifi.HalDeviceManager$WifiChipInfo[] r0 = r5.getAllChipInfo()
            if (r0 != 0) goto L_0x0070
            java.lang.String r1 = "HalDevMgr"
            java.lang.String r2 = "registerInterfaceAvailableForRequestListener: no chip info found - but possibly registered pre-started - ignoring"
            android.util.Log.e(r1, r2)
            return
        L_0x0070:
            r5.dispatchAvailableForRequestListenersForType(r6, r0)
            return
        L_0x0074:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0074 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.registerInterfaceAvailableForRequestListener(int, com.android.server.wifi.HalDeviceManager$InterfaceAvailableForRequestListener, android.os.Handler):void");
    }

    public void unregisterInterfaceAvailableForRequestListener(int ifaceType, InterfaceAvailableForRequestListener listener) {
        Log.d(TAG, "unregisterInterfaceAvailableForRequestListener: ifaceType=" + ifaceType);
        synchronized (this.mLock) {
            this.mInterfaceAvailableForRequestListeners.get(ifaceType).remove(new InterfaceAvailableForRequestListenerProxy(listener, (Handler) null));
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x005f, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerRttControllerLifecycleCallback(com.android.server.wifi.HalDeviceManager.InterfaceRttControllerLifecycleCallback r6, android.os.Handler r7) {
        /*
            r5 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "registerRttControllerLifecycleCallback: callback="
            r0.append(r1)
            r0.append(r6)
            java.lang.String r1 = ", handler="
            r0.append(r1)
            r0.append(r7)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.d(r1, r0)
            if (r6 == 0) goto L_0x0063
            if (r7 != 0) goto L_0x0023
            goto L_0x0063
        L_0x0023:
            java.lang.Object r0 = r5.mLock
            monitor-enter(r0)
            com.android.server.wifi.HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy r1 = new com.android.server.wifi.HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy     // Catch:{ all -> 0x0060 }
            r1.<init>(r6, r7)     // Catch:{ all -> 0x0060 }
            java.util.Set<com.android.server.wifi.HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy> r2 = r5.mRttControllerLifecycleCallbacks     // Catch:{ all -> 0x0060 }
            boolean r2 = r2.add(r1)     // Catch:{ all -> 0x0060 }
            if (r2 != 0) goto L_0x004b
            java.lang.String r2 = "HalDevMgr"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0060 }
            r3.<init>()     // Catch:{ all -> 0x0060 }
            java.lang.String r4 = "registerRttControllerLifecycleCallback: registering an existing callback="
            r3.append(r4)     // Catch:{ all -> 0x0060 }
            r3.append(r6)     // Catch:{ all -> 0x0060 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0060 }
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x0060 }
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return
        L_0x004b:
            android.hardware.wifi.V1_0.IWifiRttController r2 = r5.mIWifiRttController     // Catch:{ all -> 0x0060 }
            if (r2 != 0) goto L_0x0055
            android.hardware.wifi.V1_0.IWifiRttController r2 = r5.createRttControllerIfPossible()     // Catch:{ all -> 0x0060 }
            r5.mIWifiRttController = r2     // Catch:{ all -> 0x0060 }
        L_0x0055:
            android.hardware.wifi.V1_0.IWifiRttController r2 = r5.mIWifiRttController     // Catch:{ all -> 0x0060 }
            if (r2 == 0) goto L_0x005e
            android.hardware.wifi.V1_0.IWifiRttController r2 = r5.mIWifiRttController     // Catch:{ all -> 0x0060 }
            r1.onNewRttController(r2)     // Catch:{ all -> 0x0060 }
        L_0x005e:
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return
        L_0x0060:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            throw r1
        L_0x0063:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "registerRttControllerLifecycleCallback with nulls!? callback="
            r0.append(r1)
            r0.append(r6)
            java.lang.String r1 = ", handler="
            r0.append(r1)
            r0.append(r7)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.wtf(r1, r0)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.registerRttControllerLifecycleCallback(com.android.server.wifi.HalDeviceManager$InterfaceRttControllerLifecycleCallback, android.os.Handler):void");
    }

    public static String getName(IWifiIface iface) {
        if (iface == null) {
            return "<null>";
        }
        HidlSupport.Mutable<String> nameResp = new HidlSupport.Mutable<>();
        try {
            iface.getName(new IWifiIface.getNameCallback(nameResp) {
                private final /* synthetic */ HidlSupport.Mutable f$0;

                {
                    this.f$0 = r1;
                }

                public final void onValues(WifiStatus wifiStatus, String str) {
                    HalDeviceManager.lambda$getName$0(this.f$0, wifiStatus, str);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on getName: " + e);
        }
        return (String) nameResp.value;
    }

    static /* synthetic */ void lambda$getName$0(HidlSupport.Mutable nameResp, WifiStatus status, String name) {
        if (status.code == 0) {
            nameResp.value = name;
            return;
        }
        Log.e(TAG, "Error on getName: " + statusString(status));
    }

    private class InterfaceCacheEntry {
        public IWifiChip chip;
        public int chipId;
        public long creationTime;
        public Set<InterfaceDestroyedListenerProxy> destroyedListeners;
        public boolean isLowPriority;
        public String name;
        public int type;

        private InterfaceCacheEntry() {
            this.destroyedListeners = new HashSet();
        }

        public String toString() {
            return "{name=" + this.name + ", type=" + this.type + ", destroyedListeners.size()=" + this.destroyedListeners.size() + ", creationTime=" + this.creationTime + ", isLowPriority=" + this.isLowPriority + "}";
        }
    }

    private class WifiIfaceInfo {
        public IWifiIface iface;
        public String name;

        private WifiIfaceInfo() {
        }
    }

    private class WifiChipInfo {
        public ArrayList<IWifiChip.ChipMode> availableModes;
        public IWifiChip chip;
        public int chipId;
        public int currentModeId;
        public boolean currentModeIdValid;
        public WifiIfaceInfo[][] ifaces;

        private WifiChipInfo() {
            this.ifaces = new WifiIfaceInfo[HalDeviceManager.IFACE_TYPES_BY_PRIORITY.length][];
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{chipId=");
            sb.append(this.chipId);
            sb.append(", availableModes=");
            sb.append(this.availableModes);
            sb.append(", currentModeIdValid=");
            sb.append(this.currentModeIdValid);
            sb.append(", currentModeId=");
            sb.append(this.currentModeId);
            for (int type : HalDeviceManager.IFACE_TYPES_BY_PRIORITY) {
                sb.append(", ifaces[" + type + "].length=");
                sb.append(this.ifaces[type].length);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    /* access modifiers changed from: protected */
    public ISehWifi getSehWifiServiceMockable() {
        try {
            return ISehWifi.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting ISehWifi service: " + e);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public IWifi getWifiServiceMockable() {
        try {
            return IWifi.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IWifi service: " + e);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public IServiceManager getServiceManagerMockable() {
        try {
            return IServiceManager.getService();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IServiceManager: " + e);
            return null;
        }
    }

    private void initializeInternal() {
        initIServiceManagerIfNecessary();
        if (this.mIsVendorHalSupported) {
            initIWifiIfNecessary();
            initISehWifiIfNeccessary();
        }
    }

    /* access modifiers changed from: private */
    public void teardownInternal() {
        managerStatusListenerDispatch();
        dispatchAllDestroyedListeners();
        this.mInterfaceAvailableForRequestListeners.get(0).clear();
        this.mInterfaceAvailableForRequestListeners.get(1).clear();
        this.mInterfaceAvailableForRequestListeners.get(2).clear();
        this.mInterfaceAvailableForRequestListeners.get(3).clear();
        this.mIWifiRttController = null;
        dispatchRttControllerLifecycleOnDestroyed();
        this.mRttControllerLifecycleCallbacks.clear();
    }

    public /* synthetic */ void lambda$new$1$HalDeviceManager(long cookie) {
        Log.wtf(TAG, "IServiceManager died: cookie=" + cookie);
        synchronized (this.mLock) {
            this.mServiceManager = null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0076, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initIServiceManagerIfNecessary() {
        /*
            r6 = this;
            boolean r0 = r6.mDbg
            if (r0 == 0) goto L_0x000b
            java.lang.String r0 = "HalDevMgr"
            java.lang.String r1 = "initIServiceManagerIfNecessary"
            android.util.Log.d(r0, r1)
        L_0x000b:
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            android.hidl.manager.V1_2.IServiceManager r1 = r6.mServiceManager     // Catch:{ all -> 0x0077 }
            if (r1 == 0) goto L_0x0014
            monitor-exit(r0)     // Catch:{ all -> 0x0077 }
            return
        L_0x0014:
            android.hidl.manager.V1_2.IServiceManager r1 = r6.getServiceManagerMockable()     // Catch:{ all -> 0x0077 }
            r6.mServiceManager = r1     // Catch:{ all -> 0x0077 }
            android.hidl.manager.V1_2.IServiceManager r1 = r6.mServiceManager     // Catch:{ all -> 0x0077 }
            if (r1 != 0) goto L_0x0026
            java.lang.String r1 = "HalDevMgr"
            java.lang.String r2 = "Failed to get IServiceManager instance"
            android.util.Log.wtf(r1, r2)     // Catch:{ all -> 0x0077 }
            goto L_0x0075
        L_0x0026:
            r1 = 0
            android.hidl.manager.V1_2.IServiceManager r2 = r6.mServiceManager     // Catch:{ RemoteException -> 0x0056 }
            android.os.IHwBinder$DeathRecipient r3 = r6.mServiceManagerDeathRecipient     // Catch:{ RemoteException -> 0x0056 }
            r4 = 0
            boolean r2 = r2.linkToDeath(r3, r4)     // Catch:{ RemoteException -> 0x0056 }
            if (r2 != 0) goto L_0x003e
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "Error on linkToDeath on IServiceManager"
            android.util.Log.wtf(r2, r3)     // Catch:{ RemoteException -> 0x0056 }
            r6.mServiceManager = r1     // Catch:{ RemoteException -> 0x0056 }
            monitor-exit(r0)     // Catch:{ all -> 0x0077 }
            return
        L_0x003e:
            android.hidl.manager.V1_2.IServiceManager r2 = r6.mServiceManager     // Catch:{ RemoteException -> 0x0056 }
            java.lang.String r3 = "android.hardware.wifi@1.0::IWifi"
            java.lang.String r4 = ""
            android.hidl.manager.V1_0.IServiceNotification r5 = r6.mServiceNotificationCallback     // Catch:{ RemoteException -> 0x0056 }
            boolean r2 = r2.registerForNotifications(r3, r4, r5)     // Catch:{ RemoteException -> 0x0056 }
            if (r2 != 0) goto L_0x0055
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "Failed to register a listener for IWifi service"
            android.util.Log.wtf(r2, r3)     // Catch:{ RemoteException -> 0x0056 }
            r6.mServiceManager = r1     // Catch:{ RemoteException -> 0x0056 }
        L_0x0055:
            goto L_0x006f
        L_0x0056:
            r2 = move-exception
            java.lang.String r3 = "HalDevMgr"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0077 }
            r4.<init>()     // Catch:{ all -> 0x0077 }
            java.lang.String r5 = "Exception while operating on IServiceManager: "
            r4.append(r5)     // Catch:{ all -> 0x0077 }
            r4.append(r2)     // Catch:{ all -> 0x0077 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0077 }
            android.util.Log.wtf(r3, r4)     // Catch:{ all -> 0x0077 }
            r6.mServiceManager = r1     // Catch:{ all -> 0x0077 }
        L_0x006f:
            boolean r1 = r6.isSupportedInternal()     // Catch:{ all -> 0x0077 }
            r6.mIsVendorHalSupported = r1     // Catch:{ all -> 0x0077 }
        L_0x0075:
            monitor-exit(r0)     // Catch:{ all -> 0x0077 }
            return
        L_0x0077:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0077 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.initIServiceManagerIfNecessary():void");
    }

    private boolean isSupportedInternal() {
        Log.d(TAG, "isSupportedInternal");
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mServiceManager == null) {
                Log.e(TAG, "isSupported: called but mServiceManager is null!?");
                return false;
            }
            try {
                if (!this.mServiceManager.listManifestByInterface(IWifi.kInterfaceName).isEmpty()) {
                    z = true;
                }
                return z;
            } catch (RemoteException e) {
                Log.wtf(TAG, "Exception while operating on IServiceManager: " + e);
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$new$2$HalDeviceManager(long cookie) {
        Log.e(TAG, "IWifi HAL service died! Have a listener for it ... cookie=" + cookie);
        synchronized (this.mLock) {
            this.mWifi = null;
            this.mIsReady = false;
            teardownInternal();
        }
    }

    /* access modifiers changed from: private */
    public void initIWifiIfNecessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIWifiIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                try {
                    this.mWifi = getWifiServiceMockable();
                    if (this.mWifi == null) {
                        Log.e(TAG, "IWifi not (yet) available - but have a listener for it ...");
                    } else if (!this.mWifi.linkToDeath(this.mIWifiDeathRecipient, 0)) {
                        Log.e(TAG, "Error on linkToDeath on IWifi - will retry later");
                    } else {
                        WifiStatus status = this.mWifi.registerEventCallback(this.mWifiEventCallback);
                        if (status.code != 0) {
                            Log.e(TAG, "IWifi.registerEventCallback failed: " + statusString(status));
                            this.mWifi = null;
                            return;
                        }
                        stopWifi();
                        this.mIsReady = true;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while operating on IWifi: " + e);
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$3$HalDeviceManager(long cookie) {
        Log.e(TAG, "ISehWifi HAL service died! Have a listener for it ... cookie=" + cookie);
        synchronized (this.mLock) {
            this.mSehWifi = null;
        }
    }

    /* access modifiers changed from: private */
    public void initISehWifiIfNeccessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIWifiExtIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mSehWifi == null) {
                try {
                    this.mSehWifi = getSehWifiServiceMockable();
                    if (this.mSehWifi == null) {
                        Log.e(TAG, "ISehWifi not (yet) available - but have a listener for it ...");
                    } else if (!this.mSehWifi.linkToDeath(this.mISehWifiDeathRecipient, 0)) {
                        Log.e(TAG, "Error on linkToDeath on ISehWifi - will retry later");
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while operating on ISehWifi: " + e);
                }
            }
        }
    }

    public ISehWifi getSehWifiService() {
        return this.mSehWifi;
    }

    private void initIWifiChipDebugListeners() {
        Log.d(TAG, "initIWifiChipDebugListeners");
        synchronized (this.mLock) {
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                HidlSupport.Mutable<ArrayList<Integer>> chipIdsResp = new HidlSupport.Mutable<>();
                this.mWifi.getChipIds(new IWifi.getChipIdsCallback(statusOk, chipIdsResp) {
                    private final /* synthetic */ MutableBoolean f$0;
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$0 = r1;
                        this.f$1 = r2;
                    }

                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        HalDeviceManager.lambda$initIWifiChipDebugListeners$4(this.f$0, this.f$1, wifiStatus, arrayList);
                    }
                });
                if (statusOk.value) {
                    Log.d(TAG, "getChipIds=" + chipIdsResp.value);
                    if (((ArrayList) chipIdsResp.value).size() == 0) {
                        Log.e(TAG, "Should have at least 1 chip!");
                        return;
                    }
                    HidlSupport.Mutable<IWifiChip> chipResp = new HidlSupport.Mutable<>();
                    Iterator it = ((ArrayList) chipIdsResp.value).iterator();
                    while (it.hasNext()) {
                        Integer chipId = (Integer) it.next();
                        this.mWifi.getChip(chipId.intValue(), new IWifi.getChipCallback(statusOk, chipResp) {
                            private final /* synthetic */ MutableBoolean f$0;
                            private final /* synthetic */ HidlSupport.Mutable f$1;

                            {
                                this.f$0 = r1;
                                this.f$1 = r2;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiChip iWifiChip) {
                                HalDeviceManager.lambda$initIWifiChipDebugListeners$5(this.f$0, this.f$1, wifiStatus, iWifiChip);
                            }
                        });
                        if (statusOk.value) {
                            IWifiChipEventCallback.Stub callback = new IWifiChipEventCallback.Stub() {
                                public void onChipReconfigured(int modeId) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onChipReconfigured: modeId=" + modeId);
                                }

                                public void onChipReconfigureFailure(WifiStatus status) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onChipReconfigureFailure: status=" + HalDeviceManager.statusString(status));
                                }

                                public void onIfaceAdded(int type, String name) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onIfaceAdded: type=" + type + ", name=" + name);
                                }

                                public void onIfaceRemoved(int type, String name) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onIfaceRemoved: type=" + type + ", name=" + name);
                                }

                                public void onDebugRingBufferDataAvailable(WifiDebugRingBufferStatus status, ArrayList<Byte> arrayList) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onDebugRingBufferDataAvailable");
                                }

                                public void onDebugErrorAlert(int errorCode, ArrayList<Byte> arrayList) throws RemoteException {
                                    Log.d(HalDeviceManager.TAG, "onDebugErrorAlert");
                                }
                            };
                            this.mDebugCallbacks.put(chipId.intValue(), callback);
                            WifiStatus status = ((IWifiChip) chipResp.value).registerEventCallback(callback);
                            if (status.code != 0) {
                                Log.e(TAG, "registerEventCallback failed: " + statusString(status));
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "initIWifiChipDebugListeners: exception: " + e);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    static /* synthetic */ void lambda$initIWifiChipDebugListeners$4(MutableBoolean statusOk, HidlSupport.Mutable chipIdsResp, WifiStatus status, ArrayList chipIds) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipIdsResp.value = chipIds;
            return;
        }
        Log.e(TAG, "getChipIds failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$initIWifiChipDebugListeners$5(MutableBoolean statusOk, HidlSupport.Mutable chipResp, WifiStatus status, IWifiChip chip) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipResp.value = chip;
            return;
        }
        Log.e(TAG, "getChip failed: " + statusString(status));
    }

    private WifiChipInfo[] getAllChipInfo() {
        Log.d(TAG, "getAllChipInfo");
        synchronized (this.mLock) {
            WifiChipInfo[] wifiChipInfoArr = null;
            if (this.mWifi == null) {
                Log.e(TAG, "getAllChipInfo: called but mWifi is null!?");
                return null;
            }
            try {
                boolean z = false;
                HidlSupport.Mutable<ArrayList<String>> statusOk = new MutableBoolean<>(false);
                HidlSupport.Mutable<ArrayList<Integer>> chipIdsResp = new HidlSupport.Mutable<>();
                this.mWifi.getChipIds(new IWifi.getChipIdsCallback(statusOk, chipIdsResp) {
                    private final /* synthetic */ MutableBoolean f$0;
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$0 = r1;
                        this.f$1 = r2;
                    }

                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        HalDeviceManager.lambda$getAllChipInfo$6(this.f$0, this.f$1, wifiStatus, arrayList);
                    }
                });
                if (!statusOk.value) {
                    return null;
                }
                Log.d(TAG, "getChipIds=" + chipIdsResp.value);
                if (((ArrayList) chipIdsResp.value).size() == 0) {
                    Log.e(TAG, "Should have at least 1 chip!");
                    return null;
                }
                WifiChipInfo[] chipsInfo = new WifiChipInfo[((ArrayList) chipIdsResp.value).size()];
                HidlSupport.Mutable<IWifiChip> chipResp = new HidlSupport.Mutable<>();
                HidlSupport.Mutable<ArrayList<String>> ifaceNamesResp = ((ArrayList) chipIdsResp.value).iterator();
                int chipInfoIndex = 0;
                while (ifaceNamesResp.hasNext() != 0) {
                    Integer chipId = (Integer) ifaceNamesResp.next();
                    this.mWifi.getChip(chipId.intValue(), new IWifi.getChipCallback(statusOk, chipResp) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, IWifiChip iWifiChip) {
                            HalDeviceManager.lambda$getAllChipInfo$7(this.f$0, this.f$1, wifiStatus, iWifiChip);
                        }
                    });
                    if (!statusOk.value) {
                        return wifiChipInfoArr;
                    }
                    HidlSupport.Mutable<ArrayList<IWifiChip.ChipMode>> availableModesResp = new HidlSupport.Mutable<>();
                    ((IWifiChip) chipResp.value).getAvailableModes(new IWifiChip.getAvailableModesCallback(statusOk, availableModesResp) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$8(this.f$0, this.f$1, wifiStatus, arrayList);
                        }
                    });
                    if (!statusOk.value) {
                        return wifiChipInfoArr;
                    }
                    MutableBoolean currentModeValidResp = new MutableBoolean(z);
                    MutableInt currentModeResp = new MutableInt(z ? 1 : 0);
                    ((IWifiChip) chipResp.value).getMode(new IWifiChip.getModeCallback(statusOk, currentModeValidResp, currentModeResp) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ MutableBoolean f$1;
                        private final /* synthetic */ MutableInt f$2;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void onValues(WifiStatus wifiStatus, int i) {
                            HalDeviceManager.lambda$getAllChipInfo$9(this.f$0, this.f$1, this.f$2, wifiStatus, i);
                        }
                    });
                    if (!statusOk.value) {
                        return wifiChipInfoArr;
                    }
                    HidlSupport.Mutable<ArrayList<String>> ifaceNamesResp2 = new HidlSupport.Mutable<>();
                    MutableInt ifaceIndex = new MutableInt(z);
                    ((IWifiChip) chipResp.value).getStaIfaceNames(new IWifiChip.getStaIfaceNamesCallback(statusOk, ifaceNamesResp2) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$10(this.f$0, this.f$1, wifiStatus, arrayList);
                        }
                    });
                    if (!statusOk.value) {
                        return wifiChipInfoArr;
                    }
                    WifiIfaceInfo[] staIfaces = new WifiIfaceInfo[((ArrayList) ifaceNamesResp2.value).size()];
                    Iterator it = ((ArrayList) ifaceNamesResp2.value).iterator();
                    while (it.hasNext()) {
                        HidlSupport.Mutable<ArrayList<Integer>> chipIdsResp2 = chipIdsResp;
                        String ifaceName = (String) it.next();
                        MutableInt ifaceIndex2 = ifaceIndex;
                        HidlSupport.Mutable<ArrayList<String>> mutable = ifaceNamesResp;
                        HidlSupport.Mutable<ArrayList<String>> ifaceNamesResp3 = ifaceNamesResp2;
                        MutableInt currentModeResp2 = currentModeResp;
                        MutableBoolean currentModeValidResp2 = currentModeValidResp;
                        WifiIfaceInfo[] wifiIfaceInfoArr = staIfaces;
                        WifiIfaceInfo[] staIfaces2 = staIfaces;
                        HidlSupport.Mutable<ArrayList<IWifiChip.ChipMode>> availableModesResp2 = availableModesResp;
                        ((IWifiChip) chipResp.value).getStaIface(ifaceName, new IWifiChip.getStaIfaceCallback(statusOk, ifaceName, wifiIfaceInfoArr, ifaceIndex2) {
                            private final /* synthetic */ MutableBoolean f$1;
                            private final /* synthetic */ String f$2;
                            private final /* synthetic */ HalDeviceManager.WifiIfaceInfo[] f$3;
                            private final /* synthetic */ MutableInt f$4;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                                this.f$3 = r4;
                                this.f$4 = r5;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
                                HalDeviceManager.this.lambda$getAllChipInfo$11$HalDeviceManager(this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiStaIface);
                            }
                        });
                        if (!statusOk.value) {
                            return null;
                        }
                        availableModesResp = availableModesResp2;
                        ifaceNamesResp2 = ifaceNamesResp3;
                        chipIdsResp = chipIdsResp2;
                        ifaceIndex = ifaceIndex2;
                        ifaceNamesResp = mutable;
                        currentModeResp = currentModeResp2;
                        currentModeValidResp = currentModeValidResp2;
                        staIfaces = staIfaces2;
                    }
                    MutableInt currentModeResp3 = currentModeResp;
                    MutableBoolean currentModeValidResp3 = currentModeValidResp;
                    WifiIfaceInfo[] staIfaces3 = staIfaces;
                    HidlSupport.Mutable<ArrayList<Integer>> chipIdsResp3 = chipIdsResp;
                    HidlSupport.Mutable<ArrayList<String>> mutable2 = ifaceNamesResp;
                    HidlSupport.Mutable<ArrayList<String>> ifaceNamesResp4 = ifaceNamesResp2;
                    HidlSupport.Mutable<ArrayList<IWifiChip.ChipMode>> availableModesResp3 = availableModesResp;
                    MutableInt ifaceIndex3 = ifaceIndex;
                    ifaceIndex3.value = 0;
                    ((IWifiChip) chipResp.value).getApIfaceNames(new IWifiChip.getApIfaceNamesCallback(statusOk, ifaceNamesResp4) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$12(this.f$0, this.f$1, wifiStatus, arrayList);
                        }
                    });
                    if (!statusOk.value) {
                        return null;
                    }
                    WifiIfaceInfo[] apIfaces = new WifiIfaceInfo[((ArrayList) ifaceNamesResp4.value).size()];
                    Iterator it2 = ((ArrayList) ifaceNamesResp4.value).iterator();
                    while (it2.hasNext()) {
                        String ifaceName2 = (String) it2.next();
                        HidlSupport.Mutable<ArrayList<IWifiChip.ChipMode>> availableModesResp4 = availableModesResp3;
                        $$Lambda$HalDeviceManager$LisNucJKN8TgUZ4F_hMe1s79mng r10 = r1;
                        WifiChipInfo[] chipsInfo2 = chipsInfo;
                        WifiIfaceInfo[] apIfaces2 = apIfaces;
                        $$Lambda$HalDeviceManager$LisNucJKN8TgUZ4F_hMe1s79mng r1 = new IWifiChip.getApIfaceCallback(statusOk, ifaceName2, apIfaces, ifaceIndex3) {
                            private final /* synthetic */ MutableBoolean f$1;
                            private final /* synthetic */ String f$2;
                            private final /* synthetic */ HalDeviceManager.WifiIfaceInfo[] f$3;
                            private final /* synthetic */ MutableInt f$4;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                                this.f$3 = r4;
                                this.f$4 = r5;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
                                HalDeviceManager.this.lambda$getAllChipInfo$13$HalDeviceManager(this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiApIface);
                            }
                        };
                        ((IWifiChip) chipResp.value).getApIface(ifaceName2, r10);
                        if (!statusOk.value) {
                            return null;
                        }
                        availableModesResp3 = availableModesResp4;
                        chipsInfo = chipsInfo2;
                        apIfaces = apIfaces2;
                    }
                    HidlSupport.Mutable<ArrayList<IWifiChip.ChipMode>> availableModesResp5 = availableModesResp3;
                    WifiIfaceInfo[] apIfaces3 = apIfaces;
                    WifiChipInfo[] chipsInfo3 = chipsInfo;
                    ifaceIndex3.value = 0;
                    ((IWifiChip) chipResp.value).getP2pIfaceNames(new IWifiChip.getP2pIfaceNamesCallback(statusOk, ifaceNamesResp4) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$14(this.f$0, this.f$1, wifiStatus, arrayList);
                        }
                    });
                    if (!statusOk.value) {
                        return null;
                    }
                    WifiIfaceInfo[] p2pIfaces = new WifiIfaceInfo[((ArrayList) ifaceNamesResp4.value).size()];
                    Iterator it3 = ((ArrayList) ifaceNamesResp4.value).iterator();
                    while (it3.hasNext()) {
                        String ifaceName3 = (String) it3.next();
                        Iterator it4 = it3;
                        $$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4 r11 = r1;
                        WifiIfaceInfo[] wifiIfaceInfoArr2 = p2pIfaces;
                        WifiIfaceInfo[] p2pIfaces2 = p2pIfaces;
                        IWifiChip iWifiChip = (IWifiChip) chipResp.value;
                        $$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4 r12 = new IWifiChip.getP2pIfaceCallback(statusOk, ifaceName3, wifiIfaceInfoArr2, ifaceIndex3) {
                            private final /* synthetic */ MutableBoolean f$1;
                            private final /* synthetic */ String f$2;
                            private final /* synthetic */ HalDeviceManager.WifiIfaceInfo[] f$3;
                            private final /* synthetic */ MutableInt f$4;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                                this.f$3 = r4;
                                this.f$4 = r5;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
                                HalDeviceManager.this.lambda$getAllChipInfo$15$HalDeviceManager(this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiP2pIface);
                            }
                        };
                        iWifiChip.getP2pIface(ifaceName3, r11);
                        if (!statusOk.value) {
                            return null;
                        }
                        it3 = it4;
                        p2pIfaces = p2pIfaces2;
                    }
                    WifiIfaceInfo[] p2pIfaces3 = p2pIfaces;
                    ifaceIndex3.value = 0;
                    ((IWifiChip) chipResp.value).getNanIfaceNames(new IWifiChip.getNanIfaceNamesCallback(statusOk, ifaceNamesResp4) {
                        private final /* synthetic */ MutableBoolean f$0;
                        private final /* synthetic */ HidlSupport.Mutable f$1;

                        {
                            this.f$0 = r1;
                            this.f$1 = r2;
                        }

                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$16(this.f$0, this.f$1, wifiStatus, arrayList);
                        }
                    });
                    if (!statusOk.value) {
                        return null;
                    }
                    WifiIfaceInfo[] nanIfaces = new WifiIfaceInfo[((ArrayList) ifaceNamesResp4.value).size()];
                    Iterator it5 = ((ArrayList) ifaceNamesResp4.value).iterator();
                    while (it5.hasNext()) {
                        String ifaceName4 = (String) it5.next();
                        Iterator it6 = it5;
                        $$Lambda$HalDeviceManager$OTxRCq8TAZZlX8UFhmqaHcpXJYQ r112 = r1;
                        HidlSupport.Mutable<ArrayList<String>> ifaceNamesResp5 = ifaceNamesResp4;
                        IWifiChip iWifiChip2 = (IWifiChip) chipResp.value;
                        $$Lambda$HalDeviceManager$OTxRCq8TAZZlX8UFhmqaHcpXJYQ r13 = new IWifiChip.getNanIfaceCallback(statusOk, ifaceName4, nanIfaces, ifaceIndex3) {
                            private final /* synthetic */ MutableBoolean f$1;
                            private final /* synthetic */ String f$2;
                            private final /* synthetic */ HalDeviceManager.WifiIfaceInfo[] f$3;
                            private final /* synthetic */ MutableInt f$4;

                            {
                                this.f$1 = r2;
                                this.f$2 = r3;
                                this.f$3 = r4;
                                this.f$4 = r5;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
                                HalDeviceManager.this.lambda$getAllChipInfo$17$HalDeviceManager(this.f$1, this.f$2, this.f$3, this.f$4, wifiStatus, iWifiNanIface);
                            }
                        };
                        iWifiChip2.getNanIface(ifaceName4, r112);
                        if (!statusOk.value) {
                            return null;
                        }
                        it5 = it6;
                        ifaceNamesResp4 = ifaceNamesResp5;
                    }
                    WifiChipInfo chipInfo = new WifiChipInfo();
                    int chipInfoIndex2 = chipInfoIndex + 1;
                    chipsInfo3[chipInfoIndex] = chipInfo;
                    chipInfo.chip = (IWifiChip) chipResp.value;
                    chipInfo.chipId = chipId.intValue();
                    chipInfo.availableModes = (ArrayList) availableModesResp5.value;
                    chipInfo.currentModeIdValid = currentModeValidResp3.value;
                    chipInfo.currentModeId = currentModeResp3.value;
                    chipInfo.ifaces[0] = staIfaces3;
                    chipInfo.ifaces[1] = apIfaces3;
                    chipInfo.ifaces[2] = p2pIfaces3;
                    chipInfo.ifaces[3] = nanIfaces;
                    chipInfoIndex = chipInfoIndex2;
                    z = false;
                    chipIdsResp = chipIdsResp3;
                    chipsInfo = chipsInfo3;
                    ifaceNamesResp = mutable2;
                    wifiChipInfoArr = null;
                }
                WifiChipInfo[] chipsInfo4 = chipsInfo;
                return chipsInfo4;
            } catch (RemoteException e) {
                Log.e(TAG, "getAllChipInfoAndValidateCache exception: " + e);
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$getAllChipInfo$6(MutableBoolean statusOk, HidlSupport.Mutable chipIdsResp, WifiStatus status, ArrayList chipIds) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipIdsResp.value = chipIds;
            return;
        }
        Log.e(TAG, "getChipIds failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$7(MutableBoolean statusOk, HidlSupport.Mutable chipResp, WifiStatus status, IWifiChip chip) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipResp.value = chip;
            return;
        }
        Log.e(TAG, "getChip failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$8(MutableBoolean statusOk, HidlSupport.Mutable availableModesResp, WifiStatus status, ArrayList modes) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            availableModesResp.value = modes;
            return;
        }
        Log.e(TAG, "getAvailableModes failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$9(MutableBoolean statusOk, MutableBoolean currentModeValidResp, MutableInt currentModeResp, WifiStatus status, int modeId) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            currentModeValidResp.value = true;
            currentModeResp.value = modeId;
        } else if (status.code == 5) {
            statusOk.value = true;
        } else {
            Log.e(TAG, "getMode failed: " + statusString(status));
        }
    }

    static /* synthetic */ void lambda$getAllChipInfo$10(MutableBoolean statusOk, HidlSupport.Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        Log.e(TAG, "getStaIfaceNames failed: " + statusString(status));
    }

    public /* synthetic */ void lambda$getAllChipInfo$11$HalDeviceManager(MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] staIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiStaIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            staIfaces[i] = ifaceInfo;
            return;
        }
        Log.e(TAG, "getStaIface failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$12(MutableBoolean statusOk, HidlSupport.Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        Log.e(TAG, "getApIfaceNames failed: " + statusString(status));
    }

    public /* synthetic */ void lambda$getAllChipInfo$13$HalDeviceManager(MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] apIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiApIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            apIfaces[i] = ifaceInfo;
            return;
        }
        Log.e(TAG, "getApIface failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$14(MutableBoolean statusOk, HidlSupport.Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        Log.e(TAG, "getP2pIfaceNames failed: " + statusString(status));
    }

    public /* synthetic */ void lambda$getAllChipInfo$15$HalDeviceManager(MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] p2pIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiP2pIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            p2pIfaces[i] = ifaceInfo;
            return;
        }
        Log.e(TAG, "getP2pIface failed: " + statusString(status));
    }

    static /* synthetic */ void lambda$getAllChipInfo$16(MutableBoolean statusOk, HidlSupport.Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        Log.e(TAG, "getNanIfaceNames failed: " + statusString(status));
    }

    public /* synthetic */ void lambda$getAllChipInfo$17$HalDeviceManager(MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] nanIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiNanIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            nanIfaces[i] = ifaceInfo;
            return;
        }
        Log.e(TAG, "getNanIface failed: " + statusString(status));
    }

    private boolean validateInterfaceCache(WifiChipInfo[] chipInfos) {
        Log.d(TAG, "validateInterfaceCache");
        synchronized (this.mLock) {
            for (InterfaceCacheEntry entry : this.mInterfaceInfoCache.values()) {
                WifiChipInfo matchingChipInfo = null;
                int length = chipInfos.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    WifiChipInfo ci = chipInfos[i];
                    if (ci.chipId == entry.chipId) {
                        matchingChipInfo = ci;
                        break;
                    }
                    i++;
                }
                if (matchingChipInfo == null) {
                    Log.e(TAG, "validateInterfaceCache: no chip found for " + entry);
                    return false;
                }
                WifiIfaceInfo[] ifaceInfoList = matchingChipInfo.ifaces[entry.type];
                if (ifaceInfoList == null) {
                    Log.e(TAG, "validateInterfaceCache: invalid type on entry " + entry);
                    return false;
                }
                boolean matchFound = false;
                int length2 = ifaceInfoList.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    } else if (ifaceInfoList[i2].name.equals(entry.name)) {
                        matchFound = true;
                        break;
                    } else {
                        i2++;
                    }
                }
                if (!matchFound) {
                    Log.e(TAG, "validateInterfaceCache: no interface found for " + entry);
                    return false;
                }
            }
            return true;
        }
    }

    private boolean isWifiStarted() {
        Log.d(TAG, "isWifiStart");
        synchronized (this.mLock) {
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "isWifiStarted called but mWifi is null!?");
                    return false;
                }
                boolean isStarted = this.mWifi.isStarted();
                return isStarted;
            } catch (RemoteException e) {
                Log.e(TAG, "isWifiStarted exception: " + e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean startWifi() {
        Log.d(TAG, "startWifi");
        initIWifiIfNecessary();
        synchronized (this.mLock) {
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "startWifi called but mWifi is null!?");
                    return false;
                }
                int triedCount = 0;
                while (triedCount <= 3) {
                    WifiStatus status = this.mWifi.start();
                    if (status.code == 0) {
                        initIWifiChipDebugListeners();
                        managerStatusListenerDispatch();
                        if (triedCount != 0) {
                            Log.d(TAG, "start IWifi succeeded after trying " + triedCount + " times");
                        }
                        return true;
                    } else if (status.code == 5) {
                        Log.e(TAG, "Cannot start IWifi: " + statusString(status) + ", Retrying...");
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                        triedCount++;
                    } else {
                        Log.e(TAG, "Cannot start IWifi: " + statusString(status));
                        return false;
                    }
                }
                Log.e(TAG, "Cannot start IWifi after trying " + triedCount + " times");
                return false;
            } catch (RemoteException e2) {
                Log.e(TAG, "startWifi exception: " + e2);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void stopWifi() {
        Log.d(TAG, "stopWifi");
        synchronized (this.mLock) {
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "stopWifi called but mWifi is null!?");
                } else {
                    WifiStatus status = this.mWifi.stop();
                    if (status.code != 0) {
                        Log.e(TAG, "Cannot stop IWifi: " + statusString(status));
                    }
                    teardownInternal();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "stopWifi exception: " + e);
            }
        }
    }

    private class WifiEventCallback extends IWifiEventCallback.Stub {
        private WifiEventCallback() {
        }

        public void onStart() throws RemoteException {
            Log.d(HalDeviceManager.TAG, "IWifiEventCallback.onStart");
        }

        public void onStop() throws RemoteException {
            Log.d(HalDeviceManager.TAG, "IWifiEventCallback.onStop");
        }

        public void onFailure(WifiStatus status) throws RemoteException {
            Log.e(HalDeviceManager.TAG, "IWifiEventCallback.onFailure: " + HalDeviceManager.statusString(status));
            HalDeviceManager.this.teardownInternal();
        }
    }

    private void managerStatusListenerDispatch() {
        synchronized (this.mLock) {
            for (ManagerStatusListenerProxy cb : this.mManagerStatusListeners) {
                cb.trigger();
            }
        }
    }

    private class ManagerStatusListenerProxy extends ListenerProxy<ManagerStatusListener> {
        ManagerStatusListenerProxy(ManagerStatusListener statusListener, Handler handler) {
            super(statusListener, handler, "ManagerStatusListenerProxy");
        }

        /* access modifiers changed from: protected */
        public void action() {
            ((ManagerStatusListener) this.mListener).onStatusChanged();
        }
    }

    /* access modifiers changed from: package-private */
    public Set<Integer> getSupportedIfaceTypesInternal(IWifiChip chip) {
        IWifiChip iWifiChip = chip;
        Set<Integer> results = new HashSet<>();
        WifiChipInfo[] chipInfos = getAllChipInfo();
        if (chipInfos == null) {
            Log.e(TAG, "getSupportedIfaceTypesInternal: no chip info found");
            return results;
        }
        MutableInt chipIdIfProvided = new MutableInt(0);
        if (iWifiChip != null) {
            MutableBoolean statusOk = new MutableBoolean(false);
            try {
                iWifiChip.getId(new IWifiChip.getIdCallback(chipIdIfProvided, statusOk) {
                    private final /* synthetic */ MutableInt f$0;
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$0 = r1;
                        this.f$1 = r2;
                    }

                    public final void onValues(WifiStatus wifiStatus, int i) {
                        HalDeviceManager.lambda$getSupportedIfaceTypesInternal$18(this.f$0, this.f$1, wifiStatus, i);
                    }
                });
                if (!statusOk.value) {
                    return results;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getSupportedIfaceTypesInternal IWifiChip.getId() exception: " + e);
                return results;
            }
        }
        for (WifiChipInfo wci : chipInfos) {
            if (iWifiChip == null || wci.chipId == chipIdIfProvided.value) {
                Iterator<IWifiChip.ChipMode> it = wci.availableModes.iterator();
                while (it.hasNext()) {
                    Iterator<IWifiChip.ChipIfaceCombination> it2 = it.next().availableCombinations.iterator();
                    while (it2.hasNext()) {
                        Iterator<IWifiChip.ChipIfaceCombinationLimit> it3 = it2.next().limits.iterator();
                        while (it3.hasNext()) {
                            Iterator<Integer> it4 = it3.next().types.iterator();
                            while (it4.hasNext()) {
                                results.add(Integer.valueOf(it4.next().intValue()));
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    static /* synthetic */ void lambda$getSupportedIfaceTypesInternal$18(MutableInt chipIdIfProvided, MutableBoolean statusOk, WifiStatus status, int id) {
        if (status.code == 0) {
            chipIdIfProvided.value = id;
            statusOk.value = true;
            return;
        }
        Log.e(TAG, "getSupportedIfaceTypesInternal: IWifiChip.getId() error: " + statusString(status));
        statusOk.value = false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:23:0x005f, code lost:
        return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.hardware.wifi.V1_0.IWifiIface createIface(int r10, boolean r11, com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener r12, android.os.Handler r13) {
        /*
            r9 = this;
            boolean r0 = r9.mDbg
            if (r0 == 0) goto L_0x0022
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "createIface: ifaceType="
            r0.append(r1)
            r0.append(r10)
            java.lang.String r1 = ", lowPriority="
            r0.append(r1)
            r0.append(r11)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.d(r1, r0)
        L_0x0022:
            java.lang.Object r0 = r9.mLock
            monitor-enter(r0)
            com.android.server.wifi.HalDeviceManager$WifiChipInfo[] r1 = r9.getAllChipInfo()     // Catch:{ all -> 0x0060 }
            r8 = 0
            if (r1 != 0) goto L_0x0038
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "createIface: no chip info found"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x0060 }
            r9.stopWifi()     // Catch:{ all -> 0x0060 }
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return r8
        L_0x0038:
            boolean r2 = r9.validateInterfaceCache(r1)     // Catch:{ all -> 0x0060 }
            if (r2 != 0) goto L_0x004a
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "createIface: local cache is invalid!"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x0060 }
            r9.stopWifi()     // Catch:{ all -> 0x0060 }
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return r8
        L_0x004a:
            r2 = r9
            r3 = r1
            r4 = r10
            r5 = r11
            r6 = r12
            r7 = r13
            android.hardware.wifi.V1_0.IWifiIface r2 = r2.createIfaceIfPossible(r3, r4, r5, r6, r7)     // Catch:{ all -> 0x0060 }
            if (r2 == 0) goto L_0x005e
            boolean r3 = r9.dispatchAvailableForRequestListeners()     // Catch:{ all -> 0x0060 }
            if (r3 != 0) goto L_0x005e
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return r8
        L_0x005e:
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            return r2
        L_0x0060:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0060 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.createIface(int, boolean, com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListener, android.os.Handler):android.hardware.wifi.V1_0.IWifiIface");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:42:0x0147, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.hardware.wifi.V1_0.IWifiIface createIfaceIfPossible(com.android.server.wifi.HalDeviceManager.WifiChipInfo[] r23, int r24, boolean r25, com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener r26, android.os.Handler r27) {
        /*
            r22 = this;
            r7 = r22
            r8 = r23
            r9 = r24
            r10 = r25
            r11 = r26
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "createIfaceIfPossible: chipInfos="
            r0.append(r1)
            java.lang.String r1 = java.util.Arrays.deepToString(r23)
            r0.append(r1)
            java.lang.String r1 = ", ifaceType="
            r0.append(r1)
            r0.append(r9)
            java.lang.String r1 = ", lowPriority="
            r0.append(r1)
            r0.append(r10)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.d(r1, r0)
            java.lang.Object r12 = r7.mLock
            monitor-enter(r12)
            r0 = 0
            int r13 = r8.length     // Catch:{ all -> 0x0148 }
            r1 = r0
            r0 = 0
        L_0x003b:
            if (r0 >= r13) goto L_0x00d4
            r2 = r8[r0]     // Catch:{ all -> 0x0148 }
            r15 = r2
            java.util.ArrayList<android.hardware.wifi.V1_0.IWifiChip$ChipMode> r2 = r15.availableModes     // Catch:{ all -> 0x0148 }
            java.util.Iterator r16 = r2.iterator()     // Catch:{ all -> 0x0148 }
        L_0x0046:
            boolean r2 = r16.hasNext()     // Catch:{ all -> 0x0148 }
            if (r2 == 0) goto L_0x00ce
            java.lang.Object r2 = r16.next()     // Catch:{ all -> 0x0148 }
            android.hardware.wifi.V1_0.IWifiChip$ChipMode r2 = (android.hardware.wifi.V1_0.IWifiChip.ChipMode) r2     // Catch:{ all -> 0x0148 }
            r6 = r2
            java.util.ArrayList<android.hardware.wifi.V1_0.IWifiChip$ChipIfaceCombination> r2 = r6.availableCombinations     // Catch:{ all -> 0x0148 }
            java.util.Iterator r17 = r2.iterator()     // Catch:{ all -> 0x0148 }
        L_0x0059:
            boolean r2 = r17.hasNext()     // Catch:{ all -> 0x0148 }
            if (r2 == 0) goto L_0x00c8
            java.lang.Object r2 = r17.next()     // Catch:{ all -> 0x0148 }
            android.hardware.wifi.V1_0.IWifiChip$ChipIfaceCombination r2 = (android.hardware.wifi.V1_0.IWifiChip.ChipIfaceCombination) r2     // Catch:{ all -> 0x0148 }
            r5 = r2
            int[][] r2 = r7.expandIfaceCombos(r5)     // Catch:{ all -> 0x0148 }
            r3 = r2
            java.lang.String r2 = "HalDevMgr"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0148 }
            r4.<init>()     // Catch:{ all -> 0x0148 }
            r4.append(r5)     // Catch:{ all -> 0x0148 }
            java.lang.String r14 = " expands to "
            r4.append(r14)     // Catch:{ all -> 0x0148 }
            java.lang.String r14 = java.util.Arrays.deepToString(r3)     // Catch:{ all -> 0x0148 }
            r4.append(r14)     // Catch:{ all -> 0x0148 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0148 }
            android.util.Log.d(r2, r4)     // Catch:{ all -> 0x0148 }
            int r14 = r3.length     // Catch:{ all -> 0x0148 }
            r2 = 0
        L_0x008a:
            if (r2 >= r14) goto L_0x00be
            r4 = r3[r2]     // Catch:{ all -> 0x0148 }
            r8 = r1
            r1 = r22
            r18 = r2
            r2 = r15
            r19 = r3
            r3 = r6
            r20 = r5
            r5 = r24
            r21 = r6
            r6 = r25
            com.android.server.wifi.HalDeviceManager$IfaceCreationData r1 = r1.canIfaceComboSupportRequest(r2, r3, r4, r5, r6)     // Catch:{ all -> 0x0148 }
            boolean r2 = r7.compareIfaceCreationData(r1, r8)     // Catch:{ all -> 0x0148 }
            if (r2 == 0) goto L_0x00b2
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "new proposal accepted"
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x0148 }
            r2 = r1
            goto L_0x00b3
        L_0x00b2:
            r1 = r8
        L_0x00b3:
            int r2 = r18 + 1
            r8 = r23
            r3 = r19
            r5 = r20
            r6 = r21
            goto L_0x008a
        L_0x00be:
            r8 = r1
            r19 = r3
            r20 = r5
            r21 = r6
            r8 = r23
            goto L_0x0059
        L_0x00c8:
            r21 = r6
            r8 = r23
            goto L_0x0046
        L_0x00ce:
            int r0 = r0 + 1
            r8 = r23
            goto L_0x003b
        L_0x00d4:
            r0 = 0
            if (r1 == 0) goto L_0x0144
            android.hardware.wifi.V1_0.IWifiIface r2 = r7.executeChipReconfiguration(r1, r9)     // Catch:{ all -> 0x0148 }
            if (r2 == 0) goto L_0x0141
            com.android.server.wifi.HalDeviceManager$InterfaceCacheEntry r3 = new com.android.server.wifi.HalDeviceManager$InterfaceCacheEntry     // Catch:{ all -> 0x0148 }
            r3.<init>()     // Catch:{ all -> 0x0148 }
            r0 = r3
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r3 = r1.chipInfo     // Catch:{ all -> 0x0148 }
            android.hardware.wifi.V1_0.IWifiChip r3 = r3.chip     // Catch:{ all -> 0x0148 }
            r0.chip = r3     // Catch:{ all -> 0x0148 }
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r3 = r1.chipInfo     // Catch:{ all -> 0x0148 }
            int r3 = r3.chipId     // Catch:{ all -> 0x0148 }
            r0.chipId = r3     // Catch:{ all -> 0x0148 }
            java.lang.String r3 = getName(r2)     // Catch:{ all -> 0x0148 }
            r0.name = r3     // Catch:{ all -> 0x0148 }
            r0.type = r9     // Catch:{ all -> 0x0148 }
            if (r11 == 0) goto L_0x0108
            java.util.Set<com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListenerProxy> r3 = r0.destroyedListeners     // Catch:{ all -> 0x0148 }
            com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListenerProxy r4 = new com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListenerProxy     // Catch:{ all -> 0x0148 }
            java.lang.String r5 = r0.name     // Catch:{ all -> 0x0148 }
            r6 = r27
            r4.<init>(r5, r11, r6)     // Catch:{ all -> 0x014d }
            r3.add(r4)     // Catch:{ all -> 0x014d }
            goto L_0x010a
        L_0x0108:
            r6 = r27
        L_0x010a:
            com.android.server.wifi.Clock r3 = r7.mClock     // Catch:{ all -> 0x014d }
            long r3 = r3.getUptimeSinceBootMillis()     // Catch:{ all -> 0x014d }
            r0.creationTime = r3     // Catch:{ all -> 0x014d }
            r0.isLowPriority = r10     // Catch:{ all -> 0x014d }
            boolean r3 = r7.mDbg     // Catch:{ all -> 0x014d }
            if (r3 == 0) goto L_0x012e
            java.lang.String r3 = "HalDevMgr"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x014d }
            r4.<init>()     // Catch:{ all -> 0x014d }
            java.lang.String r5 = "createIfaceIfPossible: added cacheEntry="
            r4.append(r5)     // Catch:{ all -> 0x014d }
            r4.append(r0)     // Catch:{ all -> 0x014d }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x014d }
            android.util.Log.d(r3, r4)     // Catch:{ all -> 0x014d }
        L_0x012e:
            java.util.Map<android.util.Pair<java.lang.String, java.lang.Integer>, com.android.server.wifi.HalDeviceManager$InterfaceCacheEntry> r3 = r7.mInterfaceInfoCache     // Catch:{ all -> 0x014d }
            java.lang.String r4 = r0.name     // Catch:{ all -> 0x014d }
            int r5 = r0.type     // Catch:{ all -> 0x014d }
            java.lang.Integer r5 = java.lang.Integer.valueOf(r5)     // Catch:{ all -> 0x014d }
            android.util.Pair r4 = android.util.Pair.create(r4, r5)     // Catch:{ all -> 0x014d }
            r3.put(r4, r0)     // Catch:{ all -> 0x014d }
            monitor-exit(r12)     // Catch:{ all -> 0x014d }
            return r2
        L_0x0141:
            r6 = r27
            goto L_0x0146
        L_0x0144:
            r6 = r27
        L_0x0146:
            monitor-exit(r12)     // Catch:{ all -> 0x014d }
            return r0
        L_0x0148:
            r0 = move-exception
            r6 = r27
        L_0x014b:
            monitor-exit(r12)     // Catch:{ all -> 0x014d }
            throw r0
        L_0x014d:
            r0 = move-exception
            goto L_0x014b
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.createIfaceIfPossible(com.android.server.wifi.HalDeviceManager$WifiChipInfo[], int, boolean, com.android.server.wifi.HalDeviceManager$InterfaceDestroyedListener, android.os.Handler):android.hardware.wifi.V1_0.IWifiIface");
    }

    private boolean isItPossibleToCreateIface(WifiChipInfo[] chipInfos, int ifaceType) {
        WifiChipInfo[] wifiChipInfoArr = chipInfos;
        Log.d(TAG, "isItPossibleToCreateIface: chipInfos=" + Arrays.deepToString(chipInfos) + ", ifaceType=" + ifaceType);
        int length = wifiChipInfoArr.length;
        for (int i = 0; i < length; i++) {
            WifiChipInfo chipInfo = wifiChipInfoArr[i];
            Iterator<IWifiChip.ChipMode> it = chipInfo.availableModes.iterator();
            while (it.hasNext()) {
                IWifiChip.ChipMode chipMode = it.next();
                Iterator<IWifiChip.ChipIfaceCombination> it2 = chipMode.availableCombinations.iterator();
                while (true) {
                    if (it2.hasNext()) {
                        IWifiChip.ChipIfaceCombination chipIfaceCombo = it2.next();
                        int[][] expandedIfaceCombos = expandIfaceCombos(chipIfaceCombo);
                        Log.d(TAG, chipIfaceCombo + " expands to " + Arrays.deepToString(expandedIfaceCombos));
                        int length2 = expandedIfaceCombos.length;
                        int i2 = 0;
                        while (i2 < length2) {
                            int i3 = i2;
                            int i4 = length2;
                            int[][] expandedIfaceCombos2 = expandedIfaceCombos;
                            IWifiChip.ChipIfaceCombination chipIfaceCombo2 = chipIfaceCombo;
                            if (canIfaceComboSupportRequest(chipInfo, chipMode, expandedIfaceCombos[i2], ifaceType, false) != null) {
                                return true;
                            }
                            i2 = i3 + 1;
                            length2 = i4;
                            expandedIfaceCombos = expandedIfaceCombos2;
                            chipIfaceCombo = chipIfaceCombo2;
                        }
                        IWifiChip.ChipIfaceCombination chipIfaceCombination = chipIfaceCombo;
                    }
                }
            }
        }
        return false;
    }

    private int[][] expandIfaceCombos(IWifiChip.ChipIfaceCombination chipIfaceCombo) {
        int numOfCombos = 1;
        Iterator<IWifiChip.ChipIfaceCombinationLimit> it = chipIfaceCombo.limits.iterator();
        while (it.hasNext()) {
            IWifiChip.ChipIfaceCombinationLimit limit = it.next();
            for (int i = 0; i < limit.maxIfaces; i++) {
                numOfCombos *= limit.types.size();
            }
        }
        int[][] expandedIfaceCombos = (int[][]) Array.newInstance(int.class, new int[]{numOfCombos, IFACE_TYPES_BY_PRIORITY.length});
        int span = numOfCombos;
        Iterator<IWifiChip.ChipIfaceCombinationLimit> it2 = chipIfaceCombo.limits.iterator();
        while (it2.hasNext()) {
            IWifiChip.ChipIfaceCombinationLimit limit2 = it2.next();
            for (int i2 = 0; i2 < limit2.maxIfaces; i2++) {
                span /= limit2.types.size();
                for (int k = 0; k < numOfCombos; k++) {
                    int[] iArr = expandedIfaceCombos[k];
                    int intValue = limit2.types.get((k / span) % limit2.types.size()).intValue();
                    iArr[intValue] = iArr[intValue] + 1;
                }
            }
        }
        return expandedIfaceCombos;
    }

    private class IfaceCreationData {
        public WifiChipInfo chipInfo;
        public int chipModeId;
        public List<WifiIfaceInfo> interfacesToBeRemovedFirst;

        private IfaceCreationData() {
        }

        public String toString() {
            return "{chipInfo=" + this.chipInfo + ", chipModeId=" + this.chipModeId + ", interfacesToBeRemovedFirst=" + this.interfacesToBeRemovedFirst + ")";
        }
    }

    /* JADX WARNING: type inference failed for: r8v0 */
    /* JADX WARNING: type inference failed for: r8v5, types: [com.android.server.wifi.HalDeviceManager$1, com.android.server.wifi.HalDeviceManager$IfaceCreationData] */
    /* JADX WARNING: type inference failed for: r8v17 */
    private IfaceCreationData canIfaceComboSupportRequest(WifiChipInfo chipInfo, IWifiChip.ChipMode chipMode, int[] chipIfaceCombo, int ifaceType, boolean lowPriority) {
        boolean z;
        WifiChipInfo wifiChipInfo = chipInfo;
        IWifiChip.ChipMode chipMode2 = chipMode;
        int[] iArr = chipIfaceCombo;
        int i = ifaceType;
        boolean z2 = lowPriority;
        Log.d(TAG, "canIfaceComboSupportRequest: chipInfo=" + wifiChipInfo + ", chipMode=" + chipMode2 + ", chipIfaceCombo=" + iArr + ", ifaceType=" + i + ", lowPriority=" + z2);
        ? r8 = 0;
        if (iArr[i] == 0) {
            Log.d(TAG, "Requested type not supported by combo");
            return null;
        }
        int i2 = 0;
        if (wifiChipInfo.currentModeIdValid && wifiChipInfo.currentModeId != chipMode2.f0id) {
            int[] iArr2 = IFACE_TYPES_BY_PRIORITY;
            int length = iArr2.length;
            while (i2 < length) {
                int type = iArr2[i2];
                if (wifiChipInfo.ifaces[type].length == 0) {
                    z = r8;
                } else if (z2) {
                    Log.d(TAG, "Couldn't delete existing type " + type + " interfaces for a low priority request");
                    return r8;
                } else if (!allowedToDeleteIfaceTypeForRequestedType(type, i, wifiChipInfo.ifaces, wifiChipInfo.ifaces[type].length)) {
                    Log.d(TAG, "Couldn't delete existing type " + type + " interfaces for requested type");
                    return null;
                } else {
                    z = false;
                }
                i2++;
                r8 = z;
            }
            IfaceCreationData ifaceCreationData = new IfaceCreationData();
            ifaceCreationData.chipInfo = wifiChipInfo;
            ifaceCreationData.chipModeId = chipMode2.f0id;
            return ifaceCreationData;
        }
        List<WifiIfaceInfo> interfacesToBeRemovedFirst = new ArrayList<>();
        int[] iArr3 = IFACE_TYPES_BY_PRIORITY;
        int length2 = iArr3.length;
        while (i2 < length2) {
            int type2 = iArr3[i2];
            int tooManyInterfaces = wifiChipInfo.ifaces[type2].length - iArr[type2];
            if (type2 == i) {
                tooManyInterfaces++;
            }
            if (tooManyInterfaces > 0) {
                if (z2) {
                    Log.d(TAG, "Couldn't delete existing type " + type2 + " interfaces for a low priority request");
                    return null;
                } else if (!allowedToDeleteIfaceTypeForRequestedType(type2, i, wifiChipInfo.ifaces, tooManyInterfaces)) {
                    Log.d(TAG, "Would need to delete some higher priority interfaces");
                    return null;
                } else {
                    interfacesToBeRemovedFirst = selectInterfacesToDelete(tooManyInterfaces, wifiChipInfo.ifaces[type2]);
                }
            }
            i2++;
            iArr = chipIfaceCombo;
        }
        IfaceCreationData ifaceCreationData2 = new IfaceCreationData();
        ifaceCreationData2.chipInfo = wifiChipInfo;
        ifaceCreationData2.chipModeId = chipMode2.f0id;
        ifaceCreationData2.interfacesToBeRemovedFirst = interfacesToBeRemovedFirst;
        return ifaceCreationData2;
    }

    private boolean compareIfaceCreationData(IfaceCreationData val1, IfaceCreationData val2) {
        int numIfacesToDelete1;
        int numIfacesToDelete2;
        Log.d(TAG, "compareIfaceCreationData: val1=" + val1 + ", val2=" + val2);
        if (val1 == null) {
            return false;
        }
        if (val2 == null) {
            return true;
        }
        for (int type : IFACE_TYPES_BY_PRIORITY) {
            if (!val1.chipInfo.currentModeIdValid || val1.chipInfo.currentModeId == val1.chipModeId) {
                numIfacesToDelete1 = val1.interfacesToBeRemovedFirst.size();
            } else {
                numIfacesToDelete1 = val1.chipInfo.ifaces[type].length;
            }
            if (!val2.chipInfo.currentModeIdValid || val2.chipInfo.currentModeId == val2.chipModeId) {
                numIfacesToDelete2 = val2.interfacesToBeRemovedFirst.size();
            } else {
                numIfacesToDelete2 = val2.chipInfo.ifaces[type].length;
            }
            if (numIfacesToDelete1 < numIfacesToDelete2) {
                Log.d(TAG, "decision based on type=" + type + ": " + numIfacesToDelete1 + " < " + numIfacesToDelete2);
                return true;
            }
        }
        Log.d(TAG, "proposals identical - flip a coin");
        return false;
    }

    private boolean allowedToDeleteIfaceTypeForRequestedType(int existingIfaceType, int requestedIfaceType, WifiIfaceInfo[][] currentIfaces, int numNecessaryInterfaces) {
        int numAvailableLowPriorityInterfaces = 0;
        for (InterfaceCacheEntry entry : this.mInterfaceInfoCache.values()) {
            if (entry.type == existingIfaceType && entry.isLowPriority) {
                numAvailableLowPriorityInterfaces++;
            }
        }
        if (numAvailableLowPriorityInterfaces >= numNecessaryInterfaces) {
            return true;
        }
        if (existingIfaceType == requestedIfaceType || currentIfaces[requestedIfaceType].length != 0) {
            return false;
        }
        if (currentIfaces[existingIfaceType].length > 1) {
            return true;
        }
        if (requestedIfaceType == 3) {
            if (existingIfaceType == 2) {
                return true;
            }
            return false;
        } else if (requestedIfaceType != 2 || existingIfaceType == 3) {
            return true;
        } else {
            return false;
        }
    }

    private List<WifiIfaceInfo> selectInterfacesToDelete(int excessInterfaces, WifiIfaceInfo[] interfaces) {
        Log.d(TAG, "selectInterfacesToDelete: excessInterfaces=" + excessInterfaces + ", interfaces=" + Arrays.toString(interfaces));
        boolean lookupError = false;
        LongSparseArray<WifiIfaceInfo> orderedListLowPriority = new LongSparseArray<>();
        LongSparseArray<WifiIfaceInfo> orderedList = new LongSparseArray<>();
        int length = interfaces.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            WifiIfaceInfo info = interfaces[i];
            InterfaceCacheEntry cacheEntry = this.mInterfaceInfoCache.get(Pair.create(info.name, Integer.valueOf(getType(info.iface))));
            if (cacheEntry == null) {
                Log.e(TAG, "selectInterfacesToDelete: can't find cache entry with name=" + info.name);
                lookupError = true;
                break;
            }
            if (cacheEntry.isLowPriority) {
                orderedListLowPriority.append(cacheEntry.creationTime, info);
            } else {
                orderedList.append(cacheEntry.creationTime, info);
            }
            i++;
        }
        if (lookupError) {
            Log.e(TAG, "selectInterfacesToDelete: falling back to arbitrary selection");
            return Arrays.asList((WifiIfaceInfo[]) Arrays.copyOf(interfaces, excessInterfaces));
        }
        List<WifiIfaceInfo> result = new ArrayList<>(excessInterfaces);
        for (int i2 = 0; i2 < excessInterfaces; i2++) {
            int lowPriorityNextIndex = (orderedListLowPriority.size() - i2) - 1;
            if (lowPriorityNextIndex >= 0) {
                result.add(orderedListLowPriority.valueAt(lowPriorityNextIndex));
            } else {
                result.add(orderedList.valueAt(((orderedList.size() - i2) + orderedListLowPriority.size()) - 1));
            }
        }
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x003e A[Catch:{ RemoteException -> 0x013a, all -> 0x0138 }] */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0056 A[Catch:{ RemoteException -> 0x013a, all -> 0x0138 }] */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00a0 A[SYNTHETIC, Splitter:B:30:0x00a0] */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00c5 A[Catch:{ RemoteException -> 0x013a, all -> 0x0138 }] */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00f5 A[Catch:{ RemoteException -> 0x013a, all -> 0x0138 }] */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x010a A[Catch:{ RemoteException -> 0x013a, all -> 0x0138 }] */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0132 A[SYNTHETIC, Splitter:B:53:0x0132] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.hardware.wifi.V1_0.IWifiIface executeChipReconfiguration(com.android.server.wifi.HalDeviceManager.IfaceCreationData r14, int r15) {
        /*
            r13 = this;
            boolean r0 = r13.mDbg
            if (r0 == 0) goto L_0x0022
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "executeChipReconfiguration: ifaceCreationData="
            r0.append(r1)
            r0.append(r14)
            java.lang.String r1 = ", ifaceType="
            r0.append(r1)
            r0.append(r15)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "HalDevMgr"
            android.util.Log.d(r1, r0)
        L_0x0022:
            java.lang.Object r0 = r13.mLock
            monitor-enter(r0)
            r1 = 0
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r2 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            boolean r2 = r2.currentModeIdValid     // Catch:{ RemoteException -> 0x013a }
            r3 = 0
            r4 = 1
            if (r2 == 0) goto L_0x0039
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r2 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            int r2 = r2.currentModeId     // Catch:{ RemoteException -> 0x013a }
            int r5 = r14.chipModeId     // Catch:{ RemoteException -> 0x013a }
            if (r2 == r5) goto L_0x0037
            goto L_0x0039
        L_0x0037:
            r2 = r3
            goto L_0x003a
        L_0x0039:
            r2 = r4
        L_0x003a:
            boolean r5 = r13.mDbg     // Catch:{ RemoteException -> 0x013a }
            if (r5 == 0) goto L_0x0054
            java.lang.String r5 = "HalDevMgr"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x013a }
            r6.<init>()     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r7 = "isModeConfigNeeded="
            r6.append(r7)     // Catch:{ RemoteException -> 0x013a }
            r6.append(r2)     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r6 = r6.toString()     // Catch:{ RemoteException -> 0x013a }
            android.util.Log.d(r5, r6)     // Catch:{ RemoteException -> 0x013a }
        L_0x0054:
            if (r2 == 0) goto L_0x00a0
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r5 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.HalDeviceManager$WifiIfaceInfo[][] r5 = r5.ifaces     // Catch:{ RemoteException -> 0x013a }
            int r6 = r5.length     // Catch:{ RemoteException -> 0x013a }
            r7 = r3
        L_0x005c:
            if (r7 >= r6) goto L_0x0072
            r8 = r5[r7]     // Catch:{ RemoteException -> 0x013a }
            int r9 = r8.length     // Catch:{ RemoteException -> 0x013a }
            r10 = r3
        L_0x0062:
            if (r10 >= r9) goto L_0x006f
            r11 = r8[r10]     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiIface r12 = r11.iface     // Catch:{ RemoteException -> 0x013a }
            r13.removeIfaceInternal(r12)     // Catch:{ RemoteException -> 0x013a }
            int r10 = r10 + 1
            goto L_0x0062
        L_0x006f:
            int r7 = r7 + 1
            goto L_0x005c
        L_0x0072:
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r3 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiChip r3 = r3.chip     // Catch:{ RemoteException -> 0x013a }
            int r5 = r14.chipModeId     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.WifiStatus r3 = r3.configureChip(r5)     // Catch:{ RemoteException -> 0x013a }
            r13.updateRttControllerOnModeChange()     // Catch:{ RemoteException -> 0x013a }
            int r5 = r3.code     // Catch:{ RemoteException -> 0x013a }
            if (r5 == 0) goto L_0x009f
            java.lang.String r4 = "HalDevMgr"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x013a }
            r5.<init>()     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r6 = "executeChipReconfiguration: configureChip error: "
            r5.append(r6)     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r6 = statusString(r3)     // Catch:{ RemoteException -> 0x013a }
            r5.append(r6)     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r5 = r5.toString()     // Catch:{ RemoteException -> 0x013a }
            android.util.Log.e(r4, r5)     // Catch:{ RemoteException -> 0x013a }
            monitor-exit(r0)     // Catch:{ all -> 0x0138 }
            return r1
        L_0x009f:
            goto L_0x00b9
        L_0x00a0:
            java.util.List<com.android.server.wifi.HalDeviceManager$WifiIfaceInfo> r3 = r14.interfacesToBeRemovedFirst     // Catch:{ RemoteException -> 0x013a }
            java.util.Iterator r3 = r3.iterator()     // Catch:{ RemoteException -> 0x013a }
        L_0x00a6:
            boolean r5 = r3.hasNext()     // Catch:{ RemoteException -> 0x013a }
            if (r5 == 0) goto L_0x00b9
            java.lang.Object r5 = r3.next()     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.HalDeviceManager$WifiIfaceInfo r5 = (com.android.server.wifi.HalDeviceManager.WifiIfaceInfo) r5     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiIface r6 = r5.iface     // Catch:{ RemoteException -> 0x013a }
            r13.removeIfaceInternal(r6)     // Catch:{ RemoteException -> 0x013a }
            goto L_0x00a6
        L_0x00b9:
            android.os.HidlSupport$Mutable r3 = new android.os.HidlSupport$Mutable     // Catch:{ RemoteException -> 0x013a }
            r3.<init>()     // Catch:{ RemoteException -> 0x013a }
            android.os.HidlSupport$Mutable r5 = new android.os.HidlSupport$Mutable     // Catch:{ RemoteException -> 0x013a }
            r5.<init>()     // Catch:{ RemoteException -> 0x013a }
            if (r15 == 0) goto L_0x00f5
            if (r15 == r4) goto L_0x00e8
            r4 = 2
            if (r15 == r4) goto L_0x00db
            r4 = 3
            if (r15 == r4) goto L_0x00ce
            goto L_0x0102
        L_0x00ce:
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r4 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiChip r4 = r4.chip     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.-$$Lambda$HalDeviceManager$rMUl3IrUZdoNc-Vrb1rqn8XExY0 r6 = new com.android.server.wifi.-$$Lambda$HalDeviceManager$rMUl3IrUZdoNc-Vrb1rqn8XExY0     // Catch:{ RemoteException -> 0x013a }
            r6.<init>(r3, r5)     // Catch:{ RemoteException -> 0x013a }
            r4.createNanIface(r6)     // Catch:{ RemoteException -> 0x013a }
            goto L_0x0102
        L_0x00db:
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r4 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiChip r4 = r4.chip     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.-$$Lambda$HalDeviceManager$LydIQHqKB4e2ETtZbZ2Ps6wJmZg r6 = new com.android.server.wifi.-$$Lambda$HalDeviceManager$LydIQHqKB4e2ETtZbZ2Ps6wJmZg     // Catch:{ RemoteException -> 0x013a }
            r6.<init>(r3, r5)     // Catch:{ RemoteException -> 0x013a }
            r4.createP2pIface(r6)     // Catch:{ RemoteException -> 0x013a }
            goto L_0x0102
        L_0x00e8:
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r4 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiChip r4 = r4.chip     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.-$$Lambda$HalDeviceManager$Sk1PB19thsUnVIURe7jAUQxhiGk r6 = new com.android.server.wifi.-$$Lambda$HalDeviceManager$Sk1PB19thsUnVIURe7jAUQxhiGk     // Catch:{ RemoteException -> 0x013a }
            r6.<init>(r3, r5)     // Catch:{ RemoteException -> 0x013a }
            r4.createApIface(r6)     // Catch:{ RemoteException -> 0x013a }
            goto L_0x0102
        L_0x00f5:
            com.android.server.wifi.HalDeviceManager$WifiChipInfo r4 = r14.chipInfo     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiChip r4 = r4.chip     // Catch:{ RemoteException -> 0x013a }
            com.android.server.wifi.-$$Lambda$HalDeviceManager$csull9RuGux3O9fMU2TmHd3K8YE r6 = new com.android.server.wifi.-$$Lambda$HalDeviceManager$csull9RuGux3O9fMU2TmHd3K8YE     // Catch:{ RemoteException -> 0x013a }
            r6.<init>(r3, r5)     // Catch:{ RemoteException -> 0x013a }
            r4.createStaIface(r6)     // Catch:{ RemoteException -> 0x013a }
        L_0x0102:
            java.lang.Object r4 = r3.value     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.WifiStatus r4 = (android.hardware.wifi.V1_0.WifiStatus) r4     // Catch:{ RemoteException -> 0x013a }
            int r4 = r4.code     // Catch:{ RemoteException -> 0x013a }
            if (r4 == 0) goto L_0x0132
            java.lang.String r4 = "HalDevMgr"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x013a }
            r6.<init>()     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r7 = "executeChipReconfiguration: failed to create interface ifaceType="
            r6.append(r7)     // Catch:{ RemoteException -> 0x013a }
            r6.append(r15)     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r7 = ": "
            r6.append(r7)     // Catch:{ RemoteException -> 0x013a }
            java.lang.Object r7 = r3.value     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.WifiStatus r7 = (android.hardware.wifi.V1_0.WifiStatus) r7     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r7 = statusString(r7)     // Catch:{ RemoteException -> 0x013a }
            r6.append(r7)     // Catch:{ RemoteException -> 0x013a }
            java.lang.String r6 = r6.toString()     // Catch:{ RemoteException -> 0x013a }
            android.util.Log.e(r4, r6)     // Catch:{ RemoteException -> 0x013a }
            monitor-exit(r0)     // Catch:{ all -> 0x0138 }
            return r1
        L_0x0132:
            java.lang.Object r4 = r5.value     // Catch:{ RemoteException -> 0x013a }
            android.hardware.wifi.V1_0.IWifiIface r4 = (android.hardware.wifi.V1_0.IWifiIface) r4     // Catch:{ RemoteException -> 0x013a }
            monitor-exit(r0)     // Catch:{ all -> 0x0138 }
            return r4
        L_0x0138:
            r1 = move-exception
            goto L_0x0153
        L_0x013a:
            r2 = move-exception
            java.lang.String r3 = "HalDevMgr"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0138 }
            r4.<init>()     // Catch:{ all -> 0x0138 }
            java.lang.String r5 = "executeChipReconfiguration exception: "
            r4.append(r5)     // Catch:{ all -> 0x0138 }
            r4.append(r2)     // Catch:{ all -> 0x0138 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0138 }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x0138 }
            monitor-exit(r0)     // Catch:{ all -> 0x0138 }
            return r1
        L_0x0153:
            monitor-exit(r0)     // Catch:{ all -> 0x0138 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.executeChipReconfiguration(com.android.server.wifi.HalDeviceManager$IfaceCreationData, int):android.hardware.wifi.V1_0.IWifiIface");
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$19(HidlSupport.Mutable statusResp, HidlSupport.Mutable ifaceResp, WifiStatus status, IWifiStaIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$20(HidlSupport.Mutable statusResp, HidlSupport.Mutable ifaceResp, WifiStatus status, IWifiApIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$21(HidlSupport.Mutable statusResp, HidlSupport.Mutable ifaceResp, WifiStatus status, IWifiP2pIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$22(HidlSupport.Mutable statusResp, HidlSupport.Mutable ifaceResp, WifiStatus status, IWifiNanIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    private boolean removeIfaceInternal(IWifiIface iface) {
        String name = getName(iface);
        int type = getType(iface);
        if (this.mDbg) {
            Log.d(TAG, "removeIfaceInternal: iface(name)=" + name + ", type=" + type);
        }
        if (type == -1) {
            Log.e(TAG, "removeIfaceInternal: can't get type -- iface(name)=" + name);
            return false;
        }
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                Log.e(TAG, "removeIfaceInternal: null IWifi -- iface(name)=" + name);
                return false;
            }
            IWifiChip chip = getChip(iface);
            if (chip == null) {
                Log.e(TAG, "removeIfaceInternal: null IWifiChip -- iface(name)=" + name);
                return false;
            } else if (name == null) {
                Log.e(TAG, "removeIfaceInternal: can't get name");
                return false;
            } else {
                WifiStatus status = null;
                if (type == 0) {
                    status = chip.removeStaIface(name);
                } else if (type == 1) {
                    status = chip.removeApIface(name);
                } else if (type == 2) {
                    status = chip.removeP2pIface(name);
                } else if (type != 3) {
                    try {
                        Log.wtf(TAG, "removeIfaceInternal: invalid type=" + type);
                        return false;
                    } catch (RemoteException e) {
                        Log.e(TAG, "IWifiChip.removeXxxIface exception: " + e);
                    }
                } else {
                    status = chip.removeNanIface(name);
                }
                dispatchDestroyedListeners(name, type);
                if (status != null && status.code == 0) {
                    return true;
                }
                Log.e(TAG, "IWifiChip.removeXxxIface failed: " + statusString(status));
                return false;
            }
        }
    }

    private boolean dispatchAvailableForRequestListeners() {
        Log.d(TAG, "dispatchAvailableForRequestListeners");
        synchronized (this.mLock) {
            WifiChipInfo[] chipInfos = getAllChipInfo();
            if (chipInfos == null) {
                Log.e(TAG, "dispatchAvailableForRequestListeners: no chip info found");
                stopWifi();
                return false;
            }
            Log.d(TAG, "dispatchAvailableForRequestListeners: chipInfos=" + Arrays.deepToString(chipInfos));
            for (int ifaceType : IFACE_TYPES_BY_PRIORITY) {
                if (ifaceType == 2 || ifaceType == 3) {
                    Log.d(TAG, "Do not dispatch for iface type of p2p or nan");
                } else {
                    dispatchAvailableForRequestListenersForType(ifaceType, chipInfos);
                }
            }
            return true;
        }
    }

    private void dispatchAvailableForRequestListenersForType(int ifaceType, WifiChipInfo[] chipInfos) {
        Log.d(TAG, "dispatchAvailableForRequestListenersForType: ifaceType=" + ifaceType);
        synchronized (this.mLock) {
            Map<InterfaceAvailableForRequestListenerProxy, Boolean> listeners = this.mInterfaceAvailableForRequestListeners.get(ifaceType);
            if (listeners.size() != 0) {
                boolean isAvailable = isItPossibleToCreateIface(chipInfos, ifaceType);
                Log.d(TAG, "Interface available for: ifaceType=" + ifaceType + " = " + isAvailable);
                for (Map.Entry<InterfaceAvailableForRequestListenerProxy, Boolean> listenerEntry : listeners.entrySet()) {
                    if (listenerEntry.getValue() == null || listenerEntry.getValue().booleanValue() != isAvailable) {
                        Log.d(TAG, "Interface available listener dispatched: ifaceType=" + ifaceType + ", listener=" + listenerEntry.getKey());
                        listenerEntry.getKey().triggerWithArg(isAvailable);
                    }
                    listenerEntry.setValue(Boolean.valueOf(isAvailable));
                }
            }
        }
    }

    private void dispatchDestroyedListeners(String name, int type) {
        Log.d(TAG, "dispatchDestroyedListeners: iface(name)=" + name);
        synchronized (this.mLock) {
            InterfaceCacheEntry entry = this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (entry == null) {
                Log.e(TAG, "dispatchDestroyedListeners: no cache entry for iface(name)=" + name);
                return;
            }
            for (InterfaceDestroyedListenerProxy listener : entry.destroyedListeners) {
                listener.trigger();
            }
            entry.destroyedListeners.clear();
            this.mInterfaceInfoCache.remove(Pair.create(name, Integer.valueOf(type)));
        }
    }

    private void dispatchAllDestroyedListeners() {
        Log.d(TAG, "dispatchAllDestroyedListeners");
        synchronized (this.mLock) {
            Iterator<Map.Entry<Pair<String, Integer>, InterfaceCacheEntry>> it = this.mInterfaceInfoCache.entrySet().iterator();
            while (it.hasNext()) {
                InterfaceCacheEntry entry = (InterfaceCacheEntry) it.next().getValue();
                for (InterfaceDestroyedListenerProxy listener : entry.destroyedListeners) {
                    listener.trigger();
                }
                entry.destroyedListeners.clear();
                it.remove();
            }
        }
    }

    private abstract class ListenerProxy<LISTENER> {
        private Handler mHandler;
        protected LISTENER mListener;

        public boolean equals(Object obj) {
            return this.mListener == ((ListenerProxy) obj).mListener;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }

        /* access modifiers changed from: package-private */
        public void trigger() {
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.post(new Runnable() {
                    public final void run() {
                        HalDeviceManager.ListenerProxy.this.lambda$trigger$0$HalDeviceManager$ListenerProxy();
                    }
                });
            } else {
                lambda$trigger$0$HalDeviceManager$ListenerProxy();
            }
        }

        /* access modifiers changed from: package-private */
        public void triggerWithArg(boolean arg) {
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.post(new Runnable(arg) {
                    private final /* synthetic */ boolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        HalDeviceManager.ListenerProxy.this.lambda$triggerWithArg$1$HalDeviceManager$ListenerProxy(this.f$1);
                    }
                });
            } else {
                lambda$triggerWithArg$1$HalDeviceManager$ListenerProxy(arg);
            }
        }

        /* access modifiers changed from: protected */
        /* renamed from: action */
        public void lambda$trigger$0$HalDeviceManager$ListenerProxy() {
        }

        /* access modifiers changed from: protected */
        /* renamed from: actionWithArg */
        public void lambda$triggerWithArg$1$HalDeviceManager$ListenerProxy(boolean arg) {
        }

        ListenerProxy(LISTENER listener, Handler handler, String tag) {
            this.mListener = listener;
            this.mHandler = handler;
        }
    }

    private class InterfaceDestroyedListenerProxy extends ListenerProxy<InterfaceDestroyedListener> {
        private final String mIfaceName;

        InterfaceDestroyedListenerProxy(String ifaceName, InterfaceDestroyedListener destroyedListener, Handler handler) {
            super(destroyedListener, handler, "InterfaceDestroyedListenerProxy");
            this.mIfaceName = ifaceName;
        }

        /* access modifiers changed from: protected */
        public void action() {
            ((InterfaceDestroyedListener) this.mListener).onDestroyed(this.mIfaceName);
        }
    }

    private class InterfaceAvailableForRequestListenerProxy extends ListenerProxy<InterfaceAvailableForRequestListener> {
        InterfaceAvailableForRequestListenerProxy(InterfaceAvailableForRequestListener destroyedListener, Handler handler) {
            super(destroyedListener, handler, "InterfaceAvailableForRequestListenerProxy");
        }

        /* access modifiers changed from: protected */
        public void actionWithArg(boolean isAvailable) {
            ((InterfaceAvailableForRequestListener) this.mListener).onAvailabilityChanged(isAvailable);
        }
    }

    private class InterfaceRttControllerLifecycleCallbackProxy implements InterfaceRttControllerLifecycleCallback {
        private InterfaceRttControllerLifecycleCallback mCallback;
        private Handler mHandler;

        InterfaceRttControllerLifecycleCallbackProxy(InterfaceRttControllerLifecycleCallback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public boolean equals(Object obj) {
            return this.mCallback == ((InterfaceRttControllerLifecycleCallbackProxy) obj).mCallback;
        }

        public int hashCode() {
            return this.mCallback.hashCode();
        }

        /* renamed from: lambda$onNewRttController$0$HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy */
        public /* synthetic */ void mo2166x65fa2706(IWifiRttController controller) {
            this.mCallback.onNewRttController(controller);
        }

        public void onNewRttController(IWifiRttController controller) {
            this.mHandler.post(new Runnable(controller) {
                private final /* synthetic */ IWifiRttController f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HalDeviceManager.InterfaceRttControllerLifecycleCallbackProxy.this.mo2166x65fa2706(this.f$1);
                }
            });
        }

        /* renamed from: lambda$onRttControllerDestroyed$1$HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy */
        public /* synthetic */ void mo2167xa613fe64() {
            this.mCallback.onRttControllerDestroyed();
        }

        public void onRttControllerDestroyed() {
            this.mHandler.post(new Runnable() {
                public final void run() {
                    HalDeviceManager.InterfaceRttControllerLifecycleCallbackProxy.this.mo2167xa613fe64();
                }
            });
        }
    }

    private void dispatchRttControllerLifecycleOnNew() {
        Log.v(TAG, "dispatchRttControllerLifecycleOnNew: # cbs=" + this.mRttControllerLifecycleCallbacks.size());
        for (InterfaceRttControllerLifecycleCallbackProxy cbp : this.mRttControllerLifecycleCallbacks) {
            cbp.onNewRttController(this.mIWifiRttController);
        }
    }

    private void dispatchRttControllerLifecycleOnDestroyed() {
        for (InterfaceRttControllerLifecycleCallbackProxy cbp : this.mRttControllerLifecycleCallbacks) {
            cbp.onRttControllerDestroyed();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0030, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateRttControllerOnModeChange() {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mLock
            monitor-enter(r0)
            android.hardware.wifi.V1_0.IWifiRttController r1 = r4.mIWifiRttController     // Catch:{ all -> 0x0031 }
            if (r1 == 0) goto L_0x0009
            r1 = 1
            goto L_0x000a
        L_0x0009:
            r1 = 0
        L_0x000a:
            r2 = 0
            r4.mIWifiRttController = r2     // Catch:{ all -> 0x0031 }
            java.util.Set<com.android.server.wifi.HalDeviceManager$InterfaceRttControllerLifecycleCallbackProxy> r2 = r4.mRttControllerLifecycleCallbacks     // Catch:{ all -> 0x0031 }
            int r2 = r2.size()     // Catch:{ all -> 0x0031 }
            if (r2 != 0) goto L_0x001e
            java.lang.String r2 = "HalDevMgr"
            java.lang.String r3 = "updateRttController: no one is interested in RTT controllers"
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x0031 }
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            return
        L_0x001e:
            android.hardware.wifi.V1_0.IWifiRttController r2 = r4.createRttControllerIfPossible()     // Catch:{ all -> 0x0031 }
            if (r2 != 0) goto L_0x002a
            if (r1 == 0) goto L_0x002f
            r4.dispatchRttControllerLifecycleOnDestroyed()     // Catch:{ all -> 0x0031 }
            goto L_0x002f
        L_0x002a:
            r4.mIWifiRttController = r2     // Catch:{ all -> 0x0031 }
            r4.dispatchRttControllerLifecycleOnNew()     // Catch:{ all -> 0x0031 }
        L_0x002f:
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            return
        L_0x0031:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HalDeviceManager.updateRttControllerOnModeChange():void");
    }

    private IWifiRttController createRttControllerIfPossible() {
        synchronized (this.mLock) {
            if (!isWifiStarted()) {
                Log.d(TAG, "createRttControllerIfPossible: Wifi is not started");
                return null;
            }
            WifiChipInfo[] chipInfos = getAllChipInfo();
            if (chipInfos == null) {
                Log.d(TAG, "createRttControllerIfPossible: no chip info found - most likely chip not up yet");
                return null;
            }
            for (WifiChipInfo chipInfo : chipInfos) {
                if (!chipInfo.currentModeIdValid) {
                    Log.d(TAG, "createRttControllerIfPossible: chip not configured yet: " + chipInfo);
                } else {
                    HidlSupport.Mutable<IWifiRttController> rttResp = new HidlSupport.Mutable<>();
                    try {
                        chipInfo.chip.createRttController((IWifiIface) null, new IWifiChip.createRttControllerCallback(rttResp) {
                            private final /* synthetic */ HidlSupport.Mutable f$0;

                            {
                                this.f$0 = r1;
                            }

                            public final void onValues(WifiStatus wifiStatus, IWifiRttController iWifiRttController) {
                                HalDeviceManager.lambda$createRttControllerIfPossible$23(this.f$0, wifiStatus, iWifiRttController);
                            }
                        });
                    } catch (RemoteException e) {
                        Log.e(TAG, "IWifiChip.createRttController exception: " + e);
                    }
                    if (rttResp.value != null) {
                        IWifiRttController iWifiRttController = (IWifiRttController) rttResp.value;
                        return iWifiRttController;
                    }
                }
            }
            Log.w(TAG, "createRttControllerIfPossible: not available from any of the chips");
            return null;
        }
    }

    static /* synthetic */ void lambda$createRttControllerIfPossible$23(HidlSupport.Mutable rttResp, WifiStatus status, IWifiRttController rtt) {
        if (status.code == 0) {
            rttResp.value = rtt;
            return;
        }
        Log.e(TAG, "IWifiChip.createRttController failed: " + statusString(status));
    }

    /* access modifiers changed from: private */
    public static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        return status.code + " (" + status.description + ")";
    }

    private static int getType(IWifiIface iface) {
        MutableInt typeResp = new MutableInt(-1);
        try {
            iface.getType(new IWifiIface.getTypeCallback(typeResp) {
                private final /* synthetic */ MutableInt f$0;

                {
                    this.f$0 = r1;
                }

                public final void onValues(WifiStatus wifiStatus, int i) {
                    HalDeviceManager.lambda$getType$24(this.f$0, wifiStatus, i);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on getType: " + e);
        }
        return typeResp.value;
    }

    static /* synthetic */ void lambda$getType$24(MutableInt typeResp, WifiStatus status, int type) {
        if (status.code == 0) {
            typeResp.value = type;
            return;
        }
        Log.e(TAG, "Error on getType: " + statusString(status));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HalDeviceManager:");
        pw.println("  mServiceManager: " + this.mServiceManager);
        pw.println("  mWifi: " + this.mWifi);
        pw.println("  mManagerStatusListeners: " + this.mManagerStatusListeners);
        pw.println("  mInterfaceAvailableForRequestListeners: " + this.mInterfaceAvailableForRequestListeners);
        pw.println("  mInterfaceInfoCache: " + this.mInterfaceInfoCache);
    }
}
