package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_1.ISupplicant;
import android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_2.ISupplicantStaIfaceCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.net.IpConfiguration;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.os.HidlSupport;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.SupplicantStaIfaceHal;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.ANQPParser;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.NativeUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.server.wifi.WifiRoamingAssistant;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicant;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantIface;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaIface;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaIfaceCallback;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork;

@ThreadSafe
public class SupplicantStaIfaceHal {
    /* access modifiers changed from: private */
    public static final String CONFIG_SECURE_SVC_INTEGRATION = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSECURESVCINTEGRATION);
    @VisibleForTesting
    public static final String HAL_INSTANCE_NAME = "default";
    @VisibleForTesting
    public static final String INIT_SERVICE_NAME = "wpa_supplicant";
    @VisibleForTesting
    public static final String INIT_START_PROPERTY = "ctl.start";
    @VisibleForTesting
    public static final String INIT_STOP_PROPERTY = "ctl.stop";
    private static final String TAG = "SupplicantStaIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    /* access modifiers changed from: private */
    public final Context mContext;
    private HashMap<String, WifiConfiguration> mCurrentNetworkLocalConfigs = new HashMap<>();
    private HashMap<String, SupplicantStaNetworkHal> mCurrentNetworkRemoteHandles = new HashMap<>();
    private WifiNative.SupplicantDeathEventHandler mDeathEventHandler;
    /* access modifiers changed from: private */
    public long mDeathRecipientCookie = 0;
    /* access modifiers changed from: private */
    public WifiNative.DppEventCallback mDppCallback = null;
    /* access modifiers changed from: private */
    public final Handler mEventHandler;
    private ISehSupplicant mISehSupplicant;
    private HashMap<String, ISehSupplicantStaIfaceCallback> mISehSupplicantStaIfaceCallbacks = new HashMap<>();
    private HashMap<String, ISehSupplicantStaIface> mISehSupplicantStaIfaces = new HashMap<>();
    /* access modifiers changed from: private */
    public IServiceManager mIServiceManager = null;
    private ISupplicant mISupplicant;
    private HashMap<String, ISupplicantStaIfaceCallback> mISupplicantStaIfaceCallbacks = new HashMap<>();
    private HashMap<String, ISupplicantStaIface> mISupplicantStaIfaces = new HashMap<>();
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private final PropertyService mPropertyService;
    private ServiceManagerDeathRecipient mServiceManagerDeathRecipient;
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.i(SupplicantStaIfaceHal.TAG, "IServiceNotification.onRegistration for: " + fqName + ", " + name + " preexisting=" + preexisting);
                }
                if (!SupplicantStaIfaceHal.this.initSupplicantService()) {
                    Log.e(SupplicantStaIfaceHal.TAG, "initalizing ISupplicant failed.");
                    SupplicantStaIfaceHal.this.supplicantServiceDiedHandler(SupplicantStaIfaceHal.this.mDeathRecipientCookie);
                } else {
                    Log.i(SupplicantStaIfaceHal.TAG, "Completed initialization of ISupplicant.");
                }
                if (!SupplicantStaIfaceHal.this.initSupplicantExtService()) {
                    Log.e(SupplicantStaIfaceHal.TAG, "initalizing ISehSupplicant failed.");
                    SupplicantStaIfaceHal.this.supplicantExtServiceDiedHandler(SupplicantStaIfaceHal.this.mSupplicantExtDeathRecipientCookie);
                } else {
                    Log.i(SupplicantStaIfaceHal.TAG, "Completed initialization of ISehSupplicant.");
                }
            }
        }
    };
    private SupplicantDeathRecipient mSupplicantDeathRecipient;
    private SupplicantExtDeathRecipient mSupplicantExtDeathRecipient;
    /* access modifiers changed from: private */
    public long mSupplicantExtDeathRecipientCookie = 0;
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    /* access modifiers changed from: private */
    public final WifiMonitor mWifiMonitor;

    private class ServiceManagerDeathRecipient implements IHwBinder.DeathRecipient {
        private ServiceManagerDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            SupplicantStaIfaceHal.this.mEventHandler.post(new Runnable(cookie) {
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    SupplicantStaIfaceHal.ServiceManagerDeathRecipient.this.mo2609x797b8313(this.f$1);
                }
            });
        }

        /* renamed from: lambda$serviceDied$0$SupplicantStaIfaceHal$ServiceManagerDeathRecipient */
        public /* synthetic */ void mo2609x797b8313(long cookie) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                Log.w(SupplicantStaIfaceHal.TAG, "IServiceManager died: cookie=" + cookie);
                SupplicantStaIfaceHal.this.supplicantServiceDiedHandler(SupplicantStaIfaceHal.this.mDeathRecipientCookie);
                SupplicantStaIfaceHal.this.supplicantExtServiceDiedHandler(SupplicantStaIfaceHal.this.mSupplicantExtDeathRecipientCookie);
                IServiceManager unused = SupplicantStaIfaceHal.this.mIServiceManager = null;
            }
        }
    }

    private class SupplicantDeathRecipient implements IHwBinder.DeathRecipient {
        private SupplicantDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            SupplicantStaIfaceHal.this.mEventHandler.post(new Runnable(cookie) {
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    SupplicantStaIfaceHal.SupplicantDeathRecipient.this.mo2611x1322cf9e(this.f$1);
                }
            });
        }

        /* renamed from: lambda$serviceDied$0$SupplicantStaIfaceHal$SupplicantDeathRecipient */
        public /* synthetic */ void mo2611x1322cf9e(long cookie) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                Log.w(SupplicantStaIfaceHal.TAG, "ISupplicant died: cookie=" + cookie);
                SupplicantStaIfaceHal.this.supplicantServiceDiedHandler(cookie);
            }
        }
    }

    private class SupplicantExtDeathRecipient implements IHwBinder.DeathRecipient {
        private SupplicantExtDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            SupplicantStaIfaceHal.this.mEventHandler.post(new Runnable(cookie) {
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    SupplicantStaIfaceHal.SupplicantExtDeathRecipient.this.mo2613x67982a4d(this.f$1);
                }
            });
        }

        /* renamed from: lambda$serviceDied$0$SupplicantStaIfaceHal$SupplicantExtDeathRecipient */
        public /* synthetic */ void mo2613x67982a4d(long cookie) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                Log.w(SupplicantStaIfaceHal.TAG, "ISehSupplicant died: cookie=" + cookie);
                SupplicantStaIfaceHal.this.supplicantExtServiceDiedHandler(cookie);
            }
        }
    }

    public SupplicantStaIfaceHal(Context context, WifiMonitor monitor, PropertyService propertyService, Looper looper) {
        this.mContext = context;
        this.mWifiMonitor = monitor;
        this.mPropertyService = propertyService;
        this.mEventHandler = new Handler(looper);
        this.mServiceManagerDeathRecipient = new ServiceManagerDeathRecipient();
        this.mSupplicantDeathRecipient = new SupplicantDeathRecipient();
        this.mSupplicantExtDeathRecipient = new SupplicantExtDeathRecipient();
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                supplicantServiceDiedHandler(this.mDeathRecipientCookie);
                supplicantExtServiceDiedHandler(this.mSupplicantExtDeathRecipientCookie);
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering ISupplicant service ready callback.");
            }
            this.mISupplicant = null;
            this.mISupplicantStaIfaces.clear();
            this.mISehSupplicant = null;
            this.mISehSupplicantStaIfaces.clear();
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                } else if (!linkToServiceManagerDeath()) {
                    return false;
                } else {
                    if (!this.mIServiceManager.registerForNotifications(ISupplicant.kInterfaceName, "", this.mServiceNotificationCallback)) {
                        Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.supplicant@1.0::ISupplicant");
                        this.mIServiceManager = null;
                        return false;
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for ISupplicant service: " + e);
                supplicantServiceDiedHandler(this.mDeathRecipientCookie);
                supplicantExtServiceDiedHandler(this.mSupplicantExtDeathRecipientCookie);
            }
        }
        return true;
    }

    private boolean linkToSupplicantDeath() {
        synchronized (this.mLock) {
            if (this.mISupplicant == null) {
                return false;
            }
            try {
                ISupplicant iSupplicant = this.mISupplicant;
                SupplicantDeathRecipient supplicantDeathRecipient = this.mSupplicantDeathRecipient;
                long j = this.mDeathRecipientCookie + 1;
                this.mDeathRecipientCookie = j;
                if (iSupplicant.linkToDeath(supplicantDeathRecipient, j)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on ISupplicant");
                supplicantServiceDiedHandler(this.mDeathRecipientCookie);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.linkToDeath exception", e);
                return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean initSupplicantService() {
        synchronized (this.mLock) {
            try {
                this.mISupplicant = getSupplicantMockable();
                if (this.mISupplicant == null) {
                    Log.e(TAG, "Got null ISupplicant service. Stopping supplicant HIDL startup");
                    return false;
                } else if (!linkToSupplicantDeath()) {
                    return false;
                } else {
                    return true;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.getService exception: " + e);
                return false;
            } catch (NoSuchElementException e2) {
                Log.e(TAG, "ISupplicant.getService exception: " + e2);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean linkToSupplicantExtDeath() {
        synchronized (this.mLock) {
            if (this.mISehSupplicant == null) {
                return false;
            }
            try {
                ISehSupplicant iSehSupplicant = this.mISehSupplicant;
                SupplicantExtDeathRecipient supplicantExtDeathRecipient = this.mSupplicantExtDeathRecipient;
                long j = this.mSupplicantExtDeathRecipientCookie + 1;
                this.mSupplicantExtDeathRecipientCookie = j;
                if (iSehSupplicant.linkToDeath(supplicantExtDeathRecipient, j)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on ISehSupplicant");
                supplicantExtServiceDiedHandler(this.mSupplicantExtDeathRecipientCookie);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "ISehSupplicant.linkToDeath exception", e);
                return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean initSupplicantExtService() {
        synchronized (this.mLock) {
            try {
                this.mISehSupplicant = getSupplicantExtMockable();
                if (this.mISehSupplicant == null) {
                    Log.e(TAG, "Got null ISehSupplicant service. Stopping supplicant HIDL startup");
                    return false;
                } else if (!linkToSupplicantExtDeath()) {
                    return false;
                } else {
                    return true;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "ISehSupplicant.getService exception: " + e);
                return false;
            } catch (NoSuchElementException e2) {
                Log.e(TAG, "ISehSupplicant.getService exception: " + e2);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    /* access modifiers changed from: private */
    public int getCurrentNetworkId(String ifaceName) {
        synchronized (this.mLock) {
            WifiConfiguration currentConfig = getCurrentNetworkLocalConfig(ifaceName);
            if (currentConfig == null) {
                return -1;
            }
            int i = currentConfig.networkId;
            return i;
        }
    }

    public boolean setupIface(String ifaceName) {
        ISupplicantIface ifaceHwBinder;
        if (checkSupplicantStaIfaceAndLogFailure(ifaceName, "setupIface") != null) {
            return false;
        }
        boolean completed = false;
        int completedTries = 0;
        while (true) {
            if (completed) {
                break;
            }
            int completedTries2 = completedTries + 1;
            if (completedTries >= 50 || (completed = isInitializationComplete())) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            completedTries = completedTries2;
        }
        if (!completed) {
            Log.e(TAG, "setupIface couldn't be completed init");
            return false;
        }
        if (isV1_1()) {
            ifaceHwBinder = addIfaceV1_1(ifaceName);
        } else {
            ifaceHwBinder = getIfaceV1_0(ifaceName);
        }
        if (ifaceHwBinder == null) {
            Log.e(TAG, "setupIface got null iface");
            return false;
        }
        ISehSupplicantIface ifaceExtHwBinder = addIfaceExt(ifaceName);
        if (ifaceExtHwBinder == null) {
            Log.e(TAG, "setupIface got null ifaceExt");
            return false;
        }
        SupplicantStaIfaceHalCallback callback = new SupplicantStaIfaceHalCallback(ifaceName);
        if (isV1_2()) {
            android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface iface = getStaIfaceMockableV1_2(ifaceHwBinder);
            SupplicantStaIfaceHalCallbackV1_1 callbackV11 = new SupplicantStaIfaceHalCallbackV1_1(ifaceName, callback);
            if (!registerCallbackV1_2(iface, new SupplicantStaIfaceHalCallbackV1_2(callbackV11))) {
                return false;
            }
            this.mISupplicantStaIfaces.put(ifaceName, iface);
            this.mISupplicantStaIfaceCallbacks.put(ifaceName, callbackV11);
        } else if (isV1_1()) {
            android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iface2 = getStaIfaceMockableV1_1(ifaceHwBinder);
            SupplicantStaIfaceHalCallbackV1_1 callbackV1_1 = new SupplicantStaIfaceHalCallbackV1_1(ifaceName, callback);
            if (!registerCallbackV1_1(iface2, callbackV1_1)) {
                return false;
            }
            this.mISupplicantStaIfaces.put(ifaceName, iface2);
            this.mISupplicantStaIfaceCallbacks.put(ifaceName, callbackV1_1);
        } else {
            ISupplicantStaIface iface3 = getStaIfaceMockable(ifaceHwBinder);
            if (!registerCallback(iface3, callback)) {
                return false;
            }
            this.mISupplicantStaIfaces.put(ifaceName, iface3);
            this.mISupplicantStaIfaceCallbacks.put(ifaceName, callback);
        }
        SupplicantStaIfaceHalCallbackExt callbackExt = new SupplicantStaIfaceHalCallbackExt(ifaceName);
        ISehSupplicantStaIface ifaceExt = getStaIfaceExtMockable(ifaceExtHwBinder);
        if (!registerCallbackExt(ifaceExt, callbackExt)) {
            return false;
        }
        this.mISehSupplicantStaIfaces.put(ifaceName, ifaceExt);
        this.mISehSupplicantStaIfaceCallbacks.put(ifaceName, callbackExt);
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:?, code lost:
        r9.mISupplicant.getInterface(r5, new com.android.server.wifi.$$Lambda$SupplicantStaIfaceHal$QKKt5Vr7ONbMGW5Dn_SiW886n4(r3));
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.hardware.wifi.supplicant.V1_0.ISupplicantIface getIfaceV1_0(java.lang.String r10) {
        /*
            r9 = this;
            java.lang.Object r0 = r9.mLock
            monitor-enter(r0)
            android.hardware.wifi.supplicant.V1_0.ISupplicant r1 = r9.mISupplicant     // Catch:{ all -> 0x0098 }
            r2 = 0
            if (r1 != 0) goto L_0x000a
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            return r2
        L_0x000a:
            java.util.ArrayList r1 = new java.util.ArrayList     // Catch:{ all -> 0x0098 }
            r1.<init>()     // Catch:{ all -> 0x0098 }
            android.hardware.wifi.supplicant.V1_0.ISupplicant r3 = r9.mISupplicant     // Catch:{ RemoteException -> 0x007a }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$baCrbI3eo-P-3PfmPzF28q6ekLM r4 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$baCrbI3eo-P-3PfmPzF28q6ekLM     // Catch:{ RemoteException -> 0x007a }
            r4.<init>(r1)     // Catch:{ RemoteException -> 0x007a }
            r3.listInterfaces(r4)     // Catch:{ RemoteException -> 0x007a }
            int r3 = r1.size()     // Catch:{ all -> 0x0098 }
            if (r3 != 0) goto L_0x0029
            java.lang.String r3 = "SupplicantStaIfaceHal"
            java.lang.String r4 = "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup."
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x0098 }
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            return r2
        L_0x0029:
            android.os.HidlSupport$Mutable r3 = new android.os.HidlSupport$Mutable     // Catch:{ all -> 0x0098 }
            r3.<init>()     // Catch:{ all -> 0x0098 }
            java.util.Iterator r4 = r1.iterator()     // Catch:{ all -> 0x0098 }
        L_0x0032:
            boolean r5 = r4.hasNext()     // Catch:{ all -> 0x0098 }
            if (r5 == 0) goto L_0x0074
            java.lang.Object r5 = r4.next()     // Catch:{ all -> 0x0098 }
            android.hardware.wifi.supplicant.V1_0.ISupplicant$IfaceInfo r5 = (android.hardware.wifi.supplicant.V1_0.ISupplicant.IfaceInfo) r5     // Catch:{ all -> 0x0098 }
            int r6 = r5.type     // Catch:{ all -> 0x0098 }
            if (r6 != 0) goto L_0x0073
            java.lang.String r6 = r5.name     // Catch:{ all -> 0x0098 }
            boolean r6 = r10.equals(r6)     // Catch:{ all -> 0x0098 }
            if (r6 == 0) goto L_0x0073
            android.hardware.wifi.supplicant.V1_0.ISupplicant r4 = r9.mISupplicant     // Catch:{ RemoteException -> 0x0055 }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$QKKt5Vr7ONbMGW-5Dn_SiW886n4 r6 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$QKKt5Vr7ONbMGW-5Dn_SiW886n4     // Catch:{ RemoteException -> 0x0055 }
            r6.<init>(r3)     // Catch:{ RemoteException -> 0x0055 }
            r4.getInterface(r5, r6)     // Catch:{ RemoteException -> 0x0055 }
            goto L_0x0074
        L_0x0055:
            r4 = move-exception
            java.lang.String r6 = "SupplicantStaIfaceHal"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x0098 }
            r7.<init>()     // Catch:{ all -> 0x0098 }
            java.lang.String r8 = "ISupplicant.getInterface exception: "
            r7.append(r8)     // Catch:{ all -> 0x0098 }
            r7.append(r4)     // Catch:{ all -> 0x0098 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x0098 }
            android.util.Log.e(r6, r7)     // Catch:{ all -> 0x0098 }
            java.lang.String r6 = "getInterface"
            r9.handleRemoteException(r4, r6)     // Catch:{ all -> 0x0098 }
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            return r2
        L_0x0073:
            goto L_0x0032
        L_0x0074:
            java.lang.Object r2 = r3.value     // Catch:{ all -> 0x0098 }
            android.hardware.wifi.supplicant.V1_0.ISupplicantIface r2 = (android.hardware.wifi.supplicant.V1_0.ISupplicantIface) r2     // Catch:{ all -> 0x0098 }
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            return r2
        L_0x007a:
            r3 = move-exception
            java.lang.String r4 = "SupplicantStaIfaceHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x0098 }
            r5.<init>()     // Catch:{ all -> 0x0098 }
            java.lang.String r6 = "ISupplicant.listInterfaces exception: "
            r5.append(r6)     // Catch:{ all -> 0x0098 }
            r5.append(r3)     // Catch:{ all -> 0x0098 }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x0098 }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x0098 }
            java.lang.String r4 = "listInterfaces"
            r9.handleRemoteException(r3, r4)     // Catch:{ all -> 0x0098 }
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            return r2
        L_0x0098:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0098 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.getIfaceV1_0(java.lang.String):android.hardware.wifi.supplicant.V1_0.ISupplicantIface");
    }

    static /* synthetic */ void lambda$getIfaceV1_0$0(ArrayList supplicantIfaces, SupplicantStatus status, ArrayList ifaces) {
        if (status.code != 0) {
            Log.e(TAG, "Getting Supplicant Interfaces failed: " + status.code);
            return;
        }
        supplicantIfaces.addAll(ifaces);
    }

    static /* synthetic */ void lambda$getIfaceV1_0$1(HidlSupport.Mutable supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code != 0) {
            Log.e(TAG, "Failed to get ISupplicantIface " + status.code);
            return;
        }
        supplicantIface.value = iface;
    }

    private ISupplicantIface addIfaceV1_1(String ifaceName) {
        ISupplicantIface iSupplicantIface;
        synchronized (this.mLock) {
            ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.type = 0;
            HidlSupport.Mutable<ISupplicantIface> supplicantIface = new HidlSupport.Mutable<>();
            try {
                getSupplicantMockableV1_1().addInterface(ifaceInfo, new ISupplicant.addInterfaceCallback(supplicantIface) {
                    private final /* synthetic */ HidlSupport.Mutable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
                        SupplicantStaIfaceHal.lambda$addIfaceV1_1$2(this.f$0, supplicantStatus, iSupplicantIface);
                    }
                });
                iSupplicantIface = (ISupplicantIface) supplicantIface.value;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.addInterface exception: " + e);
                handleRemoteException(e, "addInterface");
                return null;
            } catch (NoSuchElementException e2) {
                Log.e(TAG, "ISupplicant.addInterface exception: " + e2);
                handleNoSuchElementException(e2, "addInterface");
                return null;
            }
        }
        return iSupplicantIface;
    }

    static /* synthetic */ void lambda$addIfaceV1_1$2(HidlSupport.Mutable supplicantIface, SupplicantStatus status, ISupplicantIface iface) {
        if (status.code == 0 || status.code == 5) {
            supplicantIface.value = iface;
            return;
        }
        Log.e(TAG, "Failed to create ISupplicantIface " + status.code);
    }

    private ISehSupplicantIface addIfaceExt(String ifaceName) {
        synchronized (this.mLock) {
            ISehSupplicant.IfaceInfo ifaceInfo = new ISehSupplicant.IfaceInfo();
            ifaceInfo.name = ifaceName;
            ifaceInfo.type = 0;
            HidlSupport.Mutable<ISehSupplicantIface> supplicantIfaceExt = new HidlSupport.Mutable<>();
            try {
                if (getSupplicantExtMockable() == null) {
                    Log.e(TAG, "ISehSupplicant.addInterface getSupplicantExtMockable is null");
                    return null;
                }
                getSupplicantExtMockable().addInterface(ifaceInfo, new ISehSupplicant.addInterfaceCallback(supplicantIfaceExt) {
                    private final /* synthetic */ HidlSupport.Mutable f$0;

                    {
                        this.f$0 = r1;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ISehSupplicantIface iSehSupplicantIface) {
                        SupplicantStaIfaceHal.lambda$addIfaceExt$3(this.f$0, supplicantStatus, iSehSupplicantIface);
                    }
                });
                ISehSupplicantIface iSehSupplicantIface = (ISehSupplicantIface) supplicantIfaceExt.value;
                return iSehSupplicantIface;
            } catch (RemoteException e) {
                Log.e(TAG, "ISehSupplicant.addInterface exception: " + e);
                handleRemoteExceptionExt(e, "addInterface");
                return null;
            } catch (NoSuchElementException e2) {
                Log.e(TAG, "ISehSupplicant.addInterface exception: " + e2);
                handleNoSuchElementExceptionExt(e2, "addInterface");
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$addIfaceExt$3(HidlSupport.Mutable supplicantIfaceExt, SupplicantStatus status, ISehSupplicantIface iface) {
        if (status.code == 0 || status.code == 5) {
            supplicantIfaceExt.value = iface;
            return;
        }
        Log.e(TAG, "Failed to create ISehSupplicantIface " + status.code);
    }

    public boolean teardownIface(String ifaceName) {
        synchronized (this.mLock) {
            if (checkSupplicantStaIfaceAndLogFailure(ifaceName, "teardownIface") == null) {
                return false;
            }
            if (isV1_1() && !removeIfaceV1_1(ifaceName)) {
                Log.e(TAG, "Failed to remove iface = " + ifaceName);
                return false;
            } else if (this.mISupplicantStaIfaces.remove(ifaceName) == null) {
                Log.e(TAG, "Trying to teardown unknown inteface");
                return false;
            } else {
                this.mISupplicantStaIfaceCallbacks.remove(ifaceName);
                if (checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "teardownIface") == null) {
                    return false;
                }
                if (this.mISehSupplicantStaIfaces.remove(ifaceName) == null) {
                    Log.e(TAG, "Trying to teardown unknown inteface ext");
                    return false;
                }
                this.mISehSupplicantStaIfaceCallbacks.remove(ifaceName);
                return true;
            }
        }
    }

    private boolean removeIfaceV1_1(String ifaceName) {
        synchronized (this.mLock) {
            try {
                ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
                ifaceInfo.name = ifaceName;
                ifaceInfo.type = 0;
                SupplicantStatus status = getSupplicantMockableV1_1().removeInterface(ifaceInfo);
                if (status.code == 0) {
                    return true;
                }
                Log.e(TAG, "Failed to remove iface " + status.code);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.removeInterface exception: " + e);
                handleRemoteException(e, "removeInterface");
                return false;
            } catch (NoSuchElementException e2) {
                Log.e(TAG, "ISupplicant.removeInterface exception: " + e2);
                handleNoSuchElementException(e2, "removeInterface");
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean registerDeathHandler(WifiNative.SupplicantDeathEventHandler handler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = handler;
        return true;
    }

    public boolean deregisterDeathHandler() {
        if (this.mDeathEventHandler == null) {
            Log.e(TAG, "No Death handler present");
        }
        this.mDeathEventHandler = null;
        return true;
    }

    private void clearState() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantStaIfaces.clear();
            this.mCurrentNetworkLocalConfigs.clear();
            this.mCurrentNetworkRemoteHandles.clear();
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003b, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void supplicantServiceDiedHandler(long r5) {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mLock
            monitor-enter(r0)
            long r1 = r4.mDeathRecipientCookie     // Catch:{ all -> 0x003c }
            int r1 = (r1 > r5 ? 1 : (r1 == r5 ? 0 : -1))
            if (r1 == 0) goto L_0x0012
            java.lang.String r1 = "SupplicantStaIfaceHal"
            java.lang.String r2 = "Ignoring stale death recipient notification"
            android.util.Log.i(r1, r2)     // Catch:{ all -> 0x003c }
            monitor-exit(r0)     // Catch:{ all -> 0x003c }
            return
        L_0x0012:
            java.util.HashMap<java.lang.String, android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface> r1 = r4.mISupplicantStaIfaces     // Catch:{ all -> 0x003c }
            java.util.Set r1 = r1.keySet()     // Catch:{ all -> 0x003c }
            java.util.Iterator r1 = r1.iterator()     // Catch:{ all -> 0x003c }
        L_0x001c:
            boolean r2 = r1.hasNext()     // Catch:{ all -> 0x003c }
            if (r2 == 0) goto L_0x002e
            java.lang.Object r2 = r1.next()     // Catch:{ all -> 0x003c }
            java.lang.String r2 = (java.lang.String) r2     // Catch:{ all -> 0x003c }
            com.android.server.wifi.WifiMonitor r3 = r4.mWifiMonitor     // Catch:{ all -> 0x003c }
            r3.broadcastSupplicantDisconnectionEvent(r2)     // Catch:{ all -> 0x003c }
            goto L_0x001c
        L_0x002e:
            r4.clearState()     // Catch:{ all -> 0x003c }
            com.android.server.wifi.WifiNative$SupplicantDeathEventHandler r1 = r4.mDeathEventHandler     // Catch:{ all -> 0x003c }
            if (r1 == 0) goto L_0x003a
            com.android.server.wifi.WifiNative$SupplicantDeathEventHandler r1 = r4.mDeathEventHandler     // Catch:{ all -> 0x003c }
            r1.onDeath()     // Catch:{ all -> 0x003c }
        L_0x003a:
            monitor-exit(r0)     // Catch:{ all -> 0x003c }
            return
        L_0x003c:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x003c }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.supplicantServiceDiedHandler(long):void");
    }

    private void clearStateExt() {
        synchronized (this.mLock) {
            this.mISehSupplicant = null;
            this.mISehSupplicantStaIfaces.clear();
        }
    }

    /* access modifiers changed from: private */
    public void supplicantExtServiceDiedHandler(long cookie) {
        synchronized (this.mLock) {
            if (this.mSupplicantExtDeathRecipientCookie != cookie) {
                Log.i(TAG, "supplicantExtServiceDiedHandler: Ignoring stale death recipient notification");
            } else {
                clearStateExt();
            }
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mISupplicant == null || this.mISehSupplicant == null) ? false : true;
        }
        return z;
    }

    private boolean startDaemon_V1_1() {
        synchronized (this.mLock) {
            try {
                getSupplicantMockableV1_1();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to start supplicant: " + e);
                supplicantServiceDiedHandler(this.mDeathRecipientCookie);
                supplicantExtServiceDiedHandler(this.mSupplicantExtDeathRecipientCookie);
                return false;
            } catch (NoSuchElementException e2) {
                Log.d(TAG, "Successfully triggered start of supplicant using HIDL");
            }
        }
        return true;
    }

    public boolean startDaemon() {
        synchronized (this.mLock) {
            if (isV1_1()) {
                Log.i(TAG, "Starting supplicant using HIDL");
                boolean startDaemon_V1_1 = startDaemon_V1_1();
                return startDaemon_V1_1;
            }
            Log.i(TAG, "Starting supplicant using init");
            this.mPropertyService.set(INIT_START_PROPERTY, INIT_SERVICE_NAME);
            return true;
        }
    }

    private void terminate_V1_1() {
        synchronized (this.mLock) {
            if (checkSupplicantAndLogFailure("terminate")) {
                try {
                    getSupplicantMockableV1_1().terminate();
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                } catch (NoSuchElementException e2) {
                    handleNoSuchElementException(e2, "terminate");
                }
            }
        }
    }

    public void terminate() {
        synchronized (this.mLock) {
            if (isV1_1()) {
                Log.i(TAG, "Terminating supplicant using HIDL");
                terminate_V1_1();
            } else {
                Log.i(TAG, "Terminating supplicant using init");
                this.mPropertyService.set(INIT_STOP_PROPERTY, INIT_SERVICE_NAME);
            }
        }
    }

    /* access modifiers changed from: protected */
    public IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_0.ISupplicant getSupplicantMockable() throws RemoteException, NoSuchElementException {
        android.hardware.wifi.supplicant.V1_0.ISupplicant service;
        synchronized (this.mLock) {
            service = android.hardware.wifi.supplicant.V1_0.ISupplicant.getService();
        }
        return service;
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_1.ISupplicant getSupplicantMockableV1_1() throws RemoteException, NoSuchElementException {
        android.hardware.wifi.supplicant.V1_1.ISupplicant castFrom;
        synchronized (this.mLock) {
            castFrom = android.hardware.wifi.supplicant.V1_1.ISupplicant.castFrom(android.hardware.wifi.supplicant.V1_0.ISupplicant.getService());
        }
        return castFrom;
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_2.ISupplicant getSupplicantMockableV1_2() throws RemoteException, NoSuchElementException {
        android.hardware.wifi.supplicant.V1_2.ISupplicant castFrom;
        synchronized (this.mLock) {
            castFrom = android.hardware.wifi.supplicant.V1_2.ISupplicant.castFrom(android.hardware.wifi.supplicant.V1_0.ISupplicant.getService());
        }
        return castFrom;
    }

    /* access modifiers changed from: protected */
    public ISehSupplicant getSupplicantExtMockable() throws RemoteException, NoSuchElementException {
        ISehSupplicant service;
        synchronized (this.mLock) {
            service = ISehSupplicant.getService();
        }
        return service;
    }

    /* access modifiers changed from: protected */
    public ISehSupplicantStaIface getStaIfaceExtMockable(ISehSupplicantIface ifaceExt) {
        ISehSupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = ISehSupplicantStaIface.asInterface(ifaceExt.asBinder());
        }
        return asInterface;
    }

    /* access modifiers changed from: protected */
    public ISupplicantStaIface getStaIfaceMockable(ISupplicantIface iface) {
        ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface getStaIfaceMockableV1_1(ISupplicantIface iface) {
        android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface getStaIfaceMockableV1_2(ISupplicantIface iface) {
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface asInterface;
        synchronized (this.mLock) {
            asInterface = android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface.asInterface(iface.asBinder());
        }
        return asInterface;
    }

    private boolean isV1_1() {
        return checkHalVersionByInterfaceName(android.hardware.wifi.supplicant.V1_1.ISupplicant.kInterfaceName);
    }

    private boolean isV1_2() {
        return checkHalVersionByInterfaceName(android.hardware.wifi.supplicant.V1_2.ISupplicant.kInterfaceName);
    }

    private boolean checkHalVersionByInterfaceName(String interfaceName) {
        boolean z = false;
        if (interfaceName == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                Log.e(TAG, "checkHalVersionByInterfaceName: called but mServiceManager is null");
                return false;
            }
            try {
                if (this.mIServiceManager.getTransport(interfaceName, "default") != 0) {
                    z = true;
                }
                return z;
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while operating on IServiceManager: " + e);
                handleRemoteException(e, "getTransport");
                return false;
            }
        }
    }

    private ISupplicantStaIface getStaIface(String ifaceName) {
        return this.mISupplicantStaIfaces.get(ifaceName);
    }

    private ISehSupplicantStaIface getStaIfaceExt(String ifaceName) {
        return this.mISehSupplicantStaIfaces.get(ifaceName);
    }

    private SupplicantStaNetworkHal getCurrentNetworkRemoteHandle(String ifaceName) {
        return this.mCurrentNetworkRemoteHandles.get(ifaceName);
    }

    /* access modifiers changed from: private */
    public WifiConfiguration getCurrentNetworkLocalConfig(String ifaceName) {
        return this.mCurrentNetworkLocalConfigs.get(ifaceName);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0063, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.util.Pair<com.android.server.wifi.SupplicantStaNetworkHal, android.net.wifi.WifiConfiguration> addNetworkAndSaveConfig(java.lang.String r9, android.net.wifi.WifiConfiguration r10) {
        /*
            r8 = this;
            java.lang.Object r0 = r8.mLock
            monitor-enter(r0)
            java.lang.String r1 = "addSupplicantStaNetwork via HIDL"
            logi(r1)     // Catch:{ all -> 0x0070 }
            r1 = 0
            if (r10 != 0) goto L_0x0012
            java.lang.String r2 = "Cannot add NULL network!"
            loge(r2)     // Catch:{ all -> 0x0070 }
            monitor-exit(r0)     // Catch:{ all -> 0x0070 }
            return r1
        L_0x0012:
            com.android.server.wifi.SupplicantStaNetworkHal r2 = r8.addNetwork(r9)     // Catch:{ all -> 0x0070 }
            if (r2 != 0) goto L_0x001f
            java.lang.String r3 = "Failed to add a network!"
            loge(r3)     // Catch:{ all -> 0x0070 }
            monitor-exit(r0)     // Catch:{ all -> 0x0070 }
            return r1
        L_0x001f:
            r3 = 0
            boolean r4 = r2.saveWifiConfiguration(r10)     // Catch:{ IllegalArgumentException -> 0x0026 }
            r3 = r4
            goto L_0x003d
        L_0x0026:
            r4 = move-exception
            java.lang.String r5 = "SupplicantStaIfaceHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x0070 }
            r6.<init>()     // Catch:{ all -> 0x0070 }
            java.lang.String r7 = "Exception while saving config params: "
            r6.append(r7)     // Catch:{ all -> 0x0070 }
            r6.append(r10)     // Catch:{ all -> 0x0070 }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x0070 }
            android.util.Log.e(r5, r6, r4)     // Catch:{ all -> 0x0070 }
        L_0x003d:
            if (r3 != 0) goto L_0x0064
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0070 }
            r4.<init>()     // Catch:{ all -> 0x0070 }
            java.lang.String r5 = "Failed to save variables for: "
            r4.append(r5)     // Catch:{ all -> 0x0070 }
            java.lang.String r5 = r10.configKey()     // Catch:{ all -> 0x0070 }
            r4.append(r5)     // Catch:{ all -> 0x0070 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0070 }
            loge(r4)     // Catch:{ all -> 0x0070 }
            boolean r4 = r8.removeAllNetworks(r9)     // Catch:{ all -> 0x0070 }
            if (r4 != 0) goto L_0x0062
            java.lang.String r4 = "Failed to remove all networks on failure."
            loge(r4)     // Catch:{ all -> 0x0070 }
        L_0x0062:
            monitor-exit(r0)     // Catch:{ all -> 0x0070 }
            return r1
        L_0x0064:
            android.util.Pair r1 = new android.util.Pair     // Catch:{ all -> 0x0070 }
            android.net.wifi.WifiConfiguration r4 = new android.net.wifi.WifiConfiguration     // Catch:{ all -> 0x0070 }
            r4.<init>(r10)     // Catch:{ all -> 0x0070 }
            r1.<init>(r2, r4)     // Catch:{ all -> 0x0070 }
            monitor-exit(r0)     // Catch:{ all -> 0x0070 }
            return r1
        L_0x0070:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0070 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.addNetworkAndSaveConfig(java.lang.String, android.net.wifi.WifiConfiguration):android.util.Pair");
    }

    public boolean connectToNetwork(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            logd("connectToNetwork " + config.configKey());
            WifiConfiguration currentConfig = getCurrentNetworkLocalConfig(ifaceName);
            if (!WifiConfigurationUtil.isSameNetwork(config, currentConfig)) {
                this.mCurrentNetworkRemoteHandles.remove(ifaceName);
                this.mCurrentNetworkLocalConfigs.remove(ifaceName);
                if (!removeAllNetworks(ifaceName)) {
                    loge("Failed to remove existing networks");
                    return false;
                }
                Pair<SupplicantStaNetworkHal, WifiConfiguration> pair = addNetworkAndSaveConfig(ifaceName, config);
                if (pair == null) {
                    loge("Failed to add/save network configuration: " + config.configKey());
                    return false;
                }
                this.mCurrentNetworkRemoteHandles.put(ifaceName, (SupplicantStaNetworkHal) pair.first);
                this.mCurrentNetworkLocalConfigs.put(ifaceName, (WifiConfiguration) pair.second);
            } else if (Objects.equals(config.getNetworkSelectionStatus().getNetworkSelectionBSSID(), currentConfig.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                logd("Network is already saved, will not trigger remove and add operation.");
            } else {
                logd("Network is already saved, but need to update BSSID.");
                if (!setCurrentNetworkBssid(ifaceName, config.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                    loge("Failed to set current network BSSID.");
                    return false;
                }
                this.mCurrentNetworkLocalConfigs.put(ifaceName, new WifiConfiguration(config));
            }
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "connectToNetwork");
            if (networkHandle != null) {
                if (networkHandle.select()) {
                    return true;
                }
            }
            loge("Failed to select network configuration: " + config.configKey());
            return false;
        }
    }

    public boolean roamToNetwork(String ifaceName, WifiConfiguration config) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId(ifaceName) != config.networkId) {
                Log.w(TAG, "Cannot roam to a different network, initiate new connection. Current network ID: " + getCurrentNetworkId(ifaceName));
                boolean connectToNetwork = connectToNetwork(ifaceName, config);
                return connectToNetwork;
            }
            String bssid = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            logd("roamToNetwork" + config.configKey() + " (bssid " + bssid + ")");
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "roamToNetwork");
            if (networkHandle != null) {
                if (networkHandle.setBssid(bssid)) {
                    if (reassociate(ifaceName)) {
                        return true;
                    }
                    loge("Failed to trigger reassociate");
                    return false;
                }
            }
            loge("Failed to set new bssid on network: " + config.configKey());
            return false;
        }
    }

    public boolean loadNetworks(String ifaceName, Map<String, WifiConfiguration> configs, SparseArray<Map<String, String>> networkExtras) {
        WifiConfiguration config;
        String str = ifaceName;
        SparseArray<Map<String, String>> sparseArray = networkExtras;
        synchronized (this.mLock) {
            try {
                List<Integer> networkIds = listNetworks(ifaceName);
                boolean z = false;
                if (networkIds == null) {
                    Log.e(TAG, "Failed to list networks");
                    return false;
                }
                for (Integer networkId : networkIds) {
                    SupplicantStaNetworkHal network = getNetwork(str, networkId.intValue());
                    if (network == null) {
                        Log.e(TAG, "Failed to get network with ID: " + networkId);
                        return z;
                    }
                    config = new WifiConfiguration();
                    Map<String, String> networkExtra = new HashMap<>();
                    boolean loadSuccess = false;
                    loadSuccess = network.loadWifiConfiguration(config, networkExtra);
                    if (!loadSuccess) {
                        Log.e(TAG, "Failed to load wifi configuration for network with ID: " + networkId + ". Skipping...");
                    } else {
                        config.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                        config.setProxySettings(IpConfiguration.ProxySettings.NONE);
                        sparseArray.put(networkId.intValue(), networkExtra);
                        String configKey = networkExtra.get("configKey");
                        try {
                            WifiConfiguration duplicateConfig = configs.put(configKey, config);
                            if (duplicateConfig != null) {
                                StringBuilder sb = new StringBuilder();
                                String str2 = configKey;
                                sb.append("Replacing duplicate network: ");
                                sb.append(duplicateConfig.networkId);
                                Log.i(TAG, sb.toString());
                                removeNetwork(str, duplicateConfig.networkId);
                                sparseArray.remove(duplicateConfig.networkId);
                            }
                            z = false;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                }
                Map<String, WifiConfiguration> map = configs;
                return true;
            } catch (IllegalArgumentException e) {
                IllegalArgumentException e2 = e;
                Log.wtf(TAG, "Exception while loading config params: " + config, e2);
            } catch (Throwable th2) {
                th = th2;
                Map<String, WifiConfiguration> map2 = configs;
                throw th;
            }
        }
    }

    public void removeNetworkIfCurrent(String ifaceName, int networkId) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId(ifaceName) == networkId) {
                removeAllNetworks(ifaceName);
            }
        }
    }

    public boolean removeAllNetworks(String ifaceName) {
        synchronized (this.mLock) {
            ArrayList<Integer> networks = listNetworks(ifaceName);
            if (networks == null) {
                Log.e(TAG, "removeAllNetworks failed, got null networks");
                return false;
            }
            Iterator<Integer> it = networks.iterator();
            while (it.hasNext()) {
                int id = it.next().intValue();
                if (!removeNetwork(ifaceName, id)) {
                    Log.e(TAG, "removeAllNetworks failed to remove network: " + id);
                    return false;
                }
            }
            this.mCurrentNetworkRemoteHandles.remove(ifaceName);
            this.mCurrentNetworkLocalConfigs.remove(ifaceName);
            return true;
        }
    }

    public boolean setCurrentNetworkBssid(String ifaceName, String bssidStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "setCurrentNetworkBssid");
            if (networkHandle == null) {
                return false;
            }
            boolean bssid = networkHandle.setBssid(bssidStr);
            return bssid;
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "getCurrentNetworkWpsNfcConfigurationToken");
            if (networkHandle == null) {
                return null;
            }
            String wpsNfcConfigurationToken = networkHandle.getWpsNfcConfigurationToken();
            return wpsNfcConfigurationToken;
        }
    }

    public String getCurrentNetworkEapAnonymousIdentity(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "getCurrentNetworkEapAnonymousIdentity");
            if (networkHandle == null) {
                return null;
            }
            String fetchEapAnonymousIdentity = networkHandle.fetchEapAnonymousIdentity();
            return fetchEapAnonymousIdentity;
        }
    }

    public boolean sendCurrentNetworkEapIdentityResponse(String ifaceName, String identity, String encryptedIdentity) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapIdentityResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapIdentityResponse = networkHandle.sendNetworkEapIdentityResponse(identity, encryptedIdentity);
            return sendNetworkEapIdentityResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimGsmAuthResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthResponse = networkHandle.sendNetworkEapSimGsmAuthResponse(paramsStr);
            return sendNetworkEapSimGsmAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthFailure(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimGsmAuthFailure");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimGsmAuthFailure = networkHandle.sendNetworkEapSimGsmAuthFailure();
            return sendNetworkEapSimGsmAuthFailure;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAuthResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthResponse = networkHandle.sendNetworkEapSimUmtsAuthResponse(paramsStr);
            return sendNetworkEapSimUmtsAuthResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAutsResponse(String ifaceName, String paramsStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAutsResponse");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAutsResponse = networkHandle.sendNetworkEapSimUmtsAutsResponse(paramsStr);
            return sendNetworkEapSimUmtsAutsResponse;
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthFailure(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "sendCurrentNetworkEapSimUmtsAuthFailure");
            if (networkHandle == null) {
                return false;
            }
            boolean sendNetworkEapSimUmtsAuthFailure = networkHandle.sendNetworkEapSimUmtsAuthFailure();
            return sendNetworkEapSimUmtsAuthFailure;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00a8, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.server.wifi.SupplicantStaNetworkHal addNetwork(java.lang.String r13) {
        /*
            r12 = this;
            java.lang.Object r0 = r12.mLock
            monitor-enter(r0)
            java.lang.String r1 = "addNetwork"
            java.lang.String r2 = "addNetwork"
            android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface r2 = r12.checkSupplicantStaIfaceAndLogFailure(r13, r2)     // Catch:{ all -> 0x00a9 }
            r3 = 0
            if (r2 != 0) goto L_0x0010
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x0010:
            android.os.HidlSupport$Mutable r4 = new android.os.HidlSupport$Mutable     // Catch:{ all -> 0x00a9 }
            r4.<init>()     // Catch:{ all -> 0x00a9 }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$P0XrIC01M2gx34WN4z0GsE6Ur-Q r5 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$P0XrIC01M2gx34WN4z0GsE6Ur-Q     // Catch:{ RemoteException -> 0x001e }
            r5.<init>(r4)     // Catch:{ RemoteException -> 0x001e }
            r2.addNetwork(r5)     // Catch:{ RemoteException -> 0x001e }
            goto L_0x0024
        L_0x001e:
            r5 = move-exception
            java.lang.String r6 = "addNetwork"
            r12.handleRemoteException(r5, r6)     // Catch:{ all -> 0x00a9 }
        L_0x0024:
            java.lang.String r5 = "getId"
            java.lang.Object r6 = r4.value     // Catch:{ all -> 0x00a9 }
            if (r6 != 0) goto L_0x0033
            java.lang.String r6 = "SupplicantStaIfaceHal"
            java.lang.String r7 = "Can't call getId, newNetwork.value is null"
            android.util.Log.e(r6, r7)     // Catch:{ all -> 0x00a9 }
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x0033:
            android.util.MutableInt r6 = new android.util.MutableInt     // Catch:{ all -> 0x00a9 }
            r7 = -1
            r6.<init>(r7)     // Catch:{ all -> 0x00a9 }
            android.util.MutableBoolean r8 = new android.util.MutableBoolean     // Catch:{ RemoteException -> 0x004c }
            r9 = 0
            r8.<init>(r9)     // Catch:{ RemoteException -> 0x004c }
            java.lang.Object r9 = r4.value     // Catch:{ RemoteException -> 0x004c }
            android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork r9 = (android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork) r9     // Catch:{ RemoteException -> 0x004c }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$dyCGlnxsszfCInkJpXjG17LBIgY r10 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$dyCGlnxsszfCInkJpXjG17LBIgY     // Catch:{ RemoteException -> 0x004c }
            r10.<init>(r8, r6)     // Catch:{ RemoteException -> 0x004c }
            r9.getId(r10)     // Catch:{ RemoteException -> 0x004c }
            goto L_0x0052
        L_0x004c:
            r8 = move-exception
            java.lang.String r9 = "getId"
            r12.handleRemoteException(r8, r9)     // Catch:{ all -> 0x00a9 }
        L_0x0052:
            java.lang.String r8 = "getNetwork"
            int r9 = r6.value     // Catch:{ all -> 0x00a9 }
            if (r9 != r7) goto L_0x0061
            java.lang.String r7 = "SupplicantStaIfaceHal"
            java.lang.String r9 = "Can't call getNetwork, gotId.value is INVALID_NETWORK_ID"
            android.util.Log.e(r7, r9)     // Catch:{ all -> 0x00a9 }
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x0061:
            java.lang.String r7 = "getNetwork"
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaIface r7 = r12.checkSupplicantStaIfaceExtAndLogFailure(r13, r7)     // Catch:{ all -> 0x00a9 }
            if (r7 != 0) goto L_0x006b
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x006b:
            android.os.HidlSupport$Mutable r9 = new android.os.HidlSupport$Mutable     // Catch:{ all -> 0x00a9 }
            r9.<init>()     // Catch:{ all -> 0x00a9 }
            int r10 = r6.value     // Catch:{ RemoteException -> 0x007b }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$v3r20J7_Pg6hImfJfqE58TW5LMk r11 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$v3r20J7_Pg6hImfJfqE58TW5LMk     // Catch:{ RemoteException -> 0x007b }
            r11.<init>(r9)     // Catch:{ RemoteException -> 0x007b }
            r7.getNetwork(r10, r11)     // Catch:{ RemoteException -> 0x007b }
            goto L_0x0081
        L_0x007b:
            r10 = move-exception
            java.lang.String r11 = "getNetwork"
            r12.handleRemoteExceptionExt(r10, r11)     // Catch:{ all -> 0x00a9 }
        L_0x0081:
            java.lang.Object r10 = r4.value     // Catch:{ all -> 0x00a9 }
            if (r10 == 0) goto L_0x00a7
            java.lang.Object r10 = r9.value     // Catch:{ all -> 0x00a9 }
            if (r10 == 0) goto L_0x00a7
            java.lang.Object r3 = r4.value     // Catch:{ all -> 0x00a9 }
            android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork r3 = (android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork) r3     // Catch:{ all -> 0x00a9 }
            android.os.IHwBinder r3 = r3.asBinder()     // Catch:{ all -> 0x00a9 }
            android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork r3 = android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.asInterface(r3)     // Catch:{ all -> 0x00a9 }
            java.lang.Object r10 = r9.value     // Catch:{ all -> 0x00a9 }
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork r10 = (vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork) r10     // Catch:{ all -> 0x00a9 }
            android.os.IHwBinder r10 = r10.asBinder()     // Catch:{ all -> 0x00a9 }
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork r10 = vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork.asInterface(r10)     // Catch:{ all -> 0x00a9 }
            com.android.server.wifi.SupplicantStaNetworkHal r3 = r12.getStaNetworkMockable(r13, r3, r10)     // Catch:{ all -> 0x00a9 }
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x00a7:
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            return r3
        L_0x00a9:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x00a9 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.addNetwork(java.lang.String):com.android.server.wifi.SupplicantStaNetworkHal");
    }

    public /* synthetic */ void lambda$addNetwork$4$SupplicantStaIfaceHal(HidlSupport.Mutable newNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (checkStatusAndLogFailure(status, "addNetwork")) {
            newNetwork.value = network;
        }
    }

    public /* synthetic */ void lambda$addNetwork$5$SupplicantStaIfaceHal(MutableBoolean statusOk, MutableInt gotId, SupplicantStatus status, int idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            gotId.value = idValue;
        } else {
            checkStatusAndLogFailure(status, "getId");
        }
    }

    public /* synthetic */ void lambda$addNetwork$6$SupplicantStaIfaceHal(HidlSupport.Mutable gotNetworkExt, SupplicantStatus status, ISehSupplicantNetwork networkExt) {
        if (checkStatusAndLogFailureExt(status, "getNetwork")) {
            gotNetworkExt.value = networkExt;
        }
    }

    private boolean removeNetwork(String ifaceName, int id) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "removeNetwork");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.removeNetwork(id), "removeNetwork");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "removeNetwork");
                return false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public SupplicantStaNetworkHal getStaNetworkMockable(String ifaceName, ISupplicantStaNetwork iSupplicantStaNetwork, ISehSupplicantStaNetwork iSehSupplicantStaNetwork) {
        SupplicantStaNetworkHal network;
        synchronized (this.mLock) {
            network = new SupplicantStaNetworkHal(iSupplicantStaNetwork, iSehSupplicantStaNetwork, ifaceName, this.mContext, this.mWifiMonitor);
            network.enableVerboseLogging(this.mVerboseLoggingEnabled);
        }
        return network;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0069, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.server.wifi.SupplicantStaNetworkHal getNetwork(java.lang.String r10, int r11) {
        /*
            r9 = this;
            java.lang.Object r0 = r9.mLock
            monitor-enter(r0)
            java.lang.String r1 = "getNetwork"
            java.lang.String r2 = "getNetwork"
            android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface r2 = r9.checkSupplicantStaIfaceAndLogFailure(r10, r2)     // Catch:{ all -> 0x006a }
            r3 = 0
            if (r2 != 0) goto L_0x0010
            monitor-exit(r0)     // Catch:{ all -> 0x006a }
            return r3
        L_0x0010:
            android.os.HidlSupport$Mutable r4 = new android.os.HidlSupport$Mutable     // Catch:{ all -> 0x006a }
            r4.<init>()     // Catch:{ all -> 0x006a }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$ABkBYFuzhhZpyhxl_UjrdG2pLLE r5 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$ABkBYFuzhhZpyhxl_UjrdG2pLLE     // Catch:{ RemoteException -> 0x001e }
            r5.<init>(r4)     // Catch:{ RemoteException -> 0x001e }
            r2.getNetwork(r11, r5)     // Catch:{ RemoteException -> 0x001e }
            goto L_0x0024
        L_0x001e:
            r5 = move-exception
            java.lang.String r6 = "getNetwork"
            r9.handleRemoteException(r5, r6)     // Catch:{ all -> 0x006a }
        L_0x0024:
            java.lang.String r5 = "getNetwork"
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaIface r5 = r9.checkSupplicantStaIfaceExtAndLogFailure(r10, r5)     // Catch:{ all -> 0x006a }
            if (r5 != 0) goto L_0x002e
            monitor-exit(r0)     // Catch:{ all -> 0x006a }
            return r3
        L_0x002e:
            android.os.HidlSupport$Mutable r6 = new android.os.HidlSupport$Mutable     // Catch:{ all -> 0x006a }
            r6.<init>()     // Catch:{ all -> 0x006a }
            com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$9V4yrC5JxcjQIh7984PTy1jEWYE r7 = new com.android.server.wifi.-$$Lambda$SupplicantStaIfaceHal$9V4yrC5JxcjQIh7984PTy1jEWYE     // Catch:{ RemoteException -> 0x003c }
            r7.<init>(r6)     // Catch:{ RemoteException -> 0x003c }
            r5.getNetwork(r11, r7)     // Catch:{ RemoteException -> 0x003c }
            goto L_0x0042
        L_0x003c:
            r7 = move-exception
            java.lang.String r8 = "getNetwork"
            r9.handleRemoteExceptionExt(r7, r8)     // Catch:{ all -> 0x006a }
        L_0x0042:
            java.lang.Object r7 = r4.value     // Catch:{ all -> 0x006a }
            if (r7 == 0) goto L_0x0068
            java.lang.Object r7 = r6.value     // Catch:{ all -> 0x006a }
            if (r7 == 0) goto L_0x0068
            java.lang.Object r3 = r4.value     // Catch:{ all -> 0x006a }
            android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork r3 = (android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork) r3     // Catch:{ all -> 0x006a }
            android.os.IHwBinder r3 = r3.asBinder()     // Catch:{ all -> 0x006a }
            android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork r3 = android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.asInterface(r3)     // Catch:{ all -> 0x006a }
            java.lang.Object r7 = r6.value     // Catch:{ all -> 0x006a }
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork r7 = (vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantNetwork) r7     // Catch:{ all -> 0x006a }
            android.os.IHwBinder r7 = r7.asBinder()     // Catch:{ all -> 0x006a }
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork r7 = vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork.asInterface(r7)     // Catch:{ all -> 0x006a }
            com.android.server.wifi.SupplicantStaNetworkHal r3 = r9.getStaNetworkMockable(r10, r3, r7)     // Catch:{ all -> 0x006a }
            monitor-exit(r0)     // Catch:{ all -> 0x006a }
            return r3
        L_0x0068:
            monitor-exit(r0)     // Catch:{ all -> 0x006a }
            return r3
        L_0x006a:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x006a }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.getNetwork(java.lang.String, int):com.android.server.wifi.SupplicantStaNetworkHal");
    }

    public /* synthetic */ void lambda$getNetwork$7$SupplicantStaIfaceHal(HidlSupport.Mutable gotNetwork, SupplicantStatus status, ISupplicantNetwork network) {
        if (checkStatusAndLogFailure(status, "getNetwork")) {
            gotNetwork.value = network;
        }
    }

    public /* synthetic */ void lambda$getNetwork$8$SupplicantStaIfaceHal(HidlSupport.Mutable gotNetworkExt, SupplicantStatus status, ISehSupplicantNetwork networkExt) {
        if (checkStatusAndLogFailureExt(status, "getNetwork")) {
            gotNetworkExt.value = networkExt;
        }
    }

    private boolean registerCallback(ISupplicantStaIface iface, ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.registerCallback(callback), "registerCallback");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean registerCallbackExt(ISehSupplicantStaIface ifaceExt, ISehSupplicantStaIfaceCallback callbackExt) {
        synchronized (this.mLock) {
            if (ifaceExt == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(ifaceExt.registerCallback(callbackExt), "registerCallback");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean registerCallbackV1_1(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.registerCallback_1_1(callback), "registerCallback_1_1");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback_1_1");
                return false;
            }
        }
    }

    private boolean registerCallbackV1_2(android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface iface, android.hardware.wifi.supplicant.V1_2.ISupplicantStaIfaceCallback callback) {
        synchronized (this.mLock) {
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.registerCallback_1_2(callback), "registerCallback_1_2");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback_1_2");
                return false;
            }
        }
    }

    private ArrayList<Integer> listNetworks(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "listNetworks");
            if (iface == null) {
                return null;
            }
            HidlSupport.Mutable<ArrayList<Integer>> networkIdList = new HidlSupport.Mutable<>();
            try {
                iface.listNetworks(new ISupplicantIface.listNetworksCallback(networkIdList) {
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaIfaceHal.this.lambda$listNetworks$9$SupplicantStaIfaceHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "listNetworks");
            }
            ArrayList<Integer> arrayList = (ArrayList) networkIdList.value;
            return arrayList;
        }
    }

    public /* synthetic */ void lambda$listNetworks$9$SupplicantStaIfaceHal(HidlSupport.Mutable networkIdList, SupplicantStatus status, ArrayList networkIds) {
        if (checkStatusAndLogFailure(status, "listNetworks")) {
            networkIdList.value = networkIds;
        }
    }

    public boolean setWpsDeviceName(String ifaceName, String name) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsDeviceName");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsDeviceName(name), "setWpsDeviceName");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceName");
                return false;
            }
        }
    }

    public boolean setWpsDeviceType(String ifaceName, String typeStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = WPS_DEVICE_TYPE_PATTERN.matcher(typeStr);
                if (match.find()) {
                    if (match.groupCount() == 3) {
                        short categ = Short.parseShort(match.group(1));
                        byte[] oui = NativeUtil.hexStringToByteArray(match.group(2));
                        short subCateg = Short.parseShort(match.group(3));
                        byte[] bytes = new byte[8];
                        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                        byteBuffer.putShort(categ);
                        byteBuffer.put(oui);
                        byteBuffer.putShort(subCateg);
                        boolean wpsDeviceType = setWpsDeviceType(ifaceName, bytes);
                        return wpsDeviceType;
                    }
                }
                Log.e(TAG, "Malformed WPS device type " + typeStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + typeStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean setWpsDeviceType(String ifaceName, byte[] type) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsDeviceType");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsDeviceType(type), "setWpsDeviceType");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceType");
                return false;
            }
        }
    }

    public boolean setWpsManufacturer(String ifaceName, String manufacturer) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsManufacturer");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsManufacturer(manufacturer), "setWpsManufacturer");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsManufacturer");
                return false;
            }
        }
    }

    public boolean setWpsModelName(String ifaceName, String modelName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsModelName");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsModelName(modelName), "setWpsModelName");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelName");
                return false;
            }
        }
    }

    public boolean setWpsModelNumber(String ifaceName, String modelNumber) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsModelNumber");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsModelNumber(modelNumber), "setWpsModelNumber");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelNumber");
                return false;
            }
        }
    }

    public boolean setWpsSerialNumber(String ifaceName, String serialNumber) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsSerialNumber");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsSerialNumber(serialNumber), "setWpsSerialNumber");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsSerialNumber");
                return false;
            }
        }
    }

    public boolean setWpsConfigMethods(String ifaceName, String configMethodsStr) {
        boolean wpsConfigMethods;
        synchronized (this.mLock) {
            short configMethodsMask = 0;
            String[] configMethodsStrArr = configMethodsStr.split("\\s+");
            for (String stringToWpsConfigMethod : configMethodsStrArr) {
                configMethodsMask = (short) (stringToWpsConfigMethod(stringToWpsConfigMethod) | configMethodsMask);
            }
            wpsConfigMethods = setWpsConfigMethods(ifaceName, configMethodsMask);
        }
        return wpsConfigMethods;
    }

    private boolean setWpsConfigMethods(String ifaceName, short configMethods) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setWpsConfigMethods");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setWpsConfigMethods(configMethods), "setWpsConfigMethods");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsConfigMethods");
                return false;
            }
        }
    }

    public boolean reassociate(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "reassociate");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.reassociate(), "reassociate");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "reassociate");
                return false;
            }
        }
    }

    public boolean reconnect(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "reconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.reconnect(), "reconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "reconnect");
                return false;
            }
        }
    }

    public boolean disconnect(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "disconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.disconnect(), "disconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "disconnect");
                return false;
            }
        }
    }

    public boolean setPowerSave(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setPowerSave");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setPowerSave(enable), "setPowerSave");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setPowerSave");
                return false;
            }
        }
    }

    public boolean initiateTdlsDiscover(String ifaceName, String macAddress) {
        boolean initiateTdlsDiscover;
        synchronized (this.mLock) {
            try {
                initiateTdlsDiscover = initiateTdlsDiscover(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return initiateTdlsDiscover;
    }

    private boolean initiateTdlsDiscover(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsDiscover");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsDiscover(macAddress), "initiateTdlsDiscover");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsDiscover");
                return false;
            }
        }
    }

    public boolean initiateTdlsSetup(String ifaceName, String macAddress) {
        boolean initiateTdlsSetup;
        synchronized (this.mLock) {
            try {
                initiateTdlsSetup = initiateTdlsSetup(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return initiateTdlsSetup;
    }

    private boolean initiateTdlsSetup(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsSetup");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsSetup(macAddress), "initiateTdlsSetup");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsSetup");
                return false;
            }
        }
    }

    public boolean initiateTdlsTeardown(String ifaceName, String macAddress) {
        boolean initiateTdlsTeardown;
        synchronized (this.mLock) {
            try {
                initiateTdlsTeardown = initiateTdlsTeardown(ifaceName, NativeUtil.macAddressToByteArray(macAddress));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + macAddress, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return initiateTdlsTeardown;
    }

    private boolean initiateTdlsTeardown(String ifaceName, byte[] macAddress) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateTdlsTeardown");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateTdlsTeardown(macAddress), "initiateTdlsTeardown");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsTeardown");
                return false;
            }
        }
    }

    public boolean initiateAnqpQuery(String ifaceName, String bssid, ArrayList<Short> infoElements, ArrayList<Integer> hs20SubTypes) {
        boolean initiateAnqpQuery;
        synchronized (this.mLock) {
            try {
                initiateAnqpQuery = initiateAnqpQuery(ifaceName, NativeUtil.macAddressToByteArray(bssid), infoElements, hs20SubTypes);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssid, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return initiateAnqpQuery;
    }

    private boolean initiateAnqpQuery(String ifaceName, byte[] macAddress, ArrayList<Short> infoElements, ArrayList<Integer> subTypes) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateAnqpQuery");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateAnqpQuery(macAddress, infoElements, subTypes), "initiateAnqpQuery");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateAnqpQuery");
                return false;
            }
        }
    }

    public boolean initiateHs20IconQuery(String ifaceName, String bssid, String fileName) {
        boolean initiateHs20IconQuery;
        synchronized (this.mLock) {
            try {
                initiateHs20IconQuery = initiateHs20IconQuery(ifaceName, NativeUtil.macAddressToByteArray(bssid), fileName);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssid, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return initiateHs20IconQuery;
    }

    private boolean initiateHs20IconQuery(String ifaceName, byte[] macAddress, String fileName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "initiateHs20IconQuery");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.initiateHs20IconQuery(macAddress, fileName), "initiateHs20IconQuery");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateHs20IconQuery");
                return false;
            }
        }
    }

    public String getMacAddress(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "getMacAddress");
            if (iface == null) {
                return null;
            }
            HidlSupport.Mutable<String> gotMac = new HidlSupport.Mutable<>();
            try {
                iface.getMacAddress(new ISupplicantStaIface.getMacAddressCallback(gotMac) {
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaIfaceHal.this.lambda$getMacAddress$10$SupplicantStaIfaceHal(this.f$1, supplicantStatus, bArr);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getMacAddress");
            }
            String str = (String) gotMac.value;
            return str;
        }
    }

    public /* synthetic */ void lambda$getMacAddress$10$SupplicantStaIfaceHal(HidlSupport.Mutable gotMac, SupplicantStatus status, byte[] macAddr) {
        if (checkStatusAndLogFailure(status, "getMacAddress")) {
            gotMac.value = NativeUtil.macAddressFromByteArray(macAddr);
        }
    }

    public boolean startRxFilter(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startRxFilter(), "startRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startRxFilter");
                return false;
            }
        }
    }

    public boolean stopRxFilter(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "stopRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.stopRxFilter(), "stopRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "stopRxFilter");
                return false;
            }
        }
    }

    public boolean addRxFilter(String ifaceName, int type) {
        byte halType;
        synchronized (this.mLock) {
            if (type == 0) {
                halType = 0;
            } else if (type != 1) {
                try {
                    Log.e(TAG, "Invalid Rx Filter type: " + type);
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                halType = 1;
            }
            boolean addRxFilter = addRxFilter(ifaceName, halType);
            return addRxFilter;
        }
    }

    private boolean addRxFilter(String ifaceName, byte type) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "addRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.addRxFilter(type), "addRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "addRxFilter");
                return false;
            }
        }
    }

    public boolean removeRxFilter(String ifaceName, int type) {
        byte halType;
        synchronized (this.mLock) {
            if (type == 0) {
                halType = 0;
            } else if (type != 1) {
                try {
                    Log.e(TAG, "Invalid Rx Filter type: " + type);
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                halType = 1;
            }
            boolean removeRxFilter = removeRxFilter(ifaceName, halType);
            return removeRxFilter;
        }
    }

    private boolean removeRxFilter(String ifaceName, byte type) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "removeRxFilter");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.removeRxFilter(type), "removeRxFilter");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "removeRxFilter");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceMode(String ifaceName, int mode) {
        byte halMode;
        synchronized (this.mLock) {
            if (mode == 0) {
                halMode = 0;
            } else if (mode == 1) {
                halMode = 1;
            } else if (mode != 2) {
                try {
                    Log.e(TAG, "Invalid Bt Coex mode: " + mode);
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                halMode = 2;
            }
            boolean btCoexistenceMode = setBtCoexistenceMode(ifaceName, halMode);
            return btCoexistenceMode;
        }
    }

    private boolean setBtCoexistenceMode(String ifaceName, byte mode) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setBtCoexistenceMode");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setBtCoexistenceMode(mode), "setBtCoexistenceMode");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceMode");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceScanModeEnabled(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setBtCoexistenceScanModeEnabled");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setBtCoexistenceScanModeEnabled(enable), "setBtCoexistenceScanModeEnabled");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceScanModeEnabled");
                return false;
            }
        }
    }

    public boolean setSuspendModeEnabled(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setSuspendModeEnabled");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setSuspendModeEnabled(enable), "setSuspendModeEnabled");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setSuspendModeEnabled");
                return false;
            }
        }
    }

    public boolean setCountryCode(String ifaceName, String codeStr) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(codeStr)) {
                return false;
            }
            byte[] countryCodeBytes = NativeUtil.stringToByteArray(codeStr);
            if (countryCodeBytes.length != 2) {
                return false;
            }
            boolean countryCode = setCountryCode(ifaceName, countryCodeBytes);
            return countryCode;
        }
    }

    private boolean setCountryCode(String ifaceName, byte[] code) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setCountryCode");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setCountryCode(code), "setCountryCode");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setCountryCode");
                return false;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0036, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startWpsRegistrar(java.lang.String r7, java.lang.String r8, java.lang.String r9) {
        /*
            r6 = this;
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            boolean r1 = android.text.TextUtils.isEmpty(r8)     // Catch:{ all -> 0x0037 }
            r2 = 0
            if (r1 != 0) goto L_0x0035
            boolean r1 = android.text.TextUtils.isEmpty(r9)     // Catch:{ all -> 0x0037 }
            if (r1 == 0) goto L_0x0011
            goto L_0x0035
        L_0x0011:
            byte[] r1 = com.android.server.wifi.util.NativeUtil.macAddressToByteArray(r8)     // Catch:{ IllegalArgumentException -> 0x001c }
            boolean r1 = r6.startWpsRegistrar((java.lang.String) r7, (byte[]) r1, (java.lang.String) r9)     // Catch:{ IllegalArgumentException -> 0x001c }
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            return r1
        L_0x001c:
            r1 = move-exception
            java.lang.String r3 = "SupplicantStaIfaceHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x0037 }
            r4.<init>()     // Catch:{ all -> 0x0037 }
            java.lang.String r5 = "Illegal argument "
            r4.append(r5)     // Catch:{ all -> 0x0037 }
            r4.append(r8)     // Catch:{ all -> 0x0037 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x0037 }
            android.util.Log.e(r3, r4, r1)     // Catch:{ all -> 0x0037 }
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            return r2
        L_0x0035:
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            return r2
        L_0x0037:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0037 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.startWpsRegistrar(java.lang.String, java.lang.String, java.lang.String):boolean");
    }

    private boolean startWpsRegistrar(String ifaceName, byte[] bssid, String pin) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsRegistrar");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsRegistrar(bssid, pin), "startWpsRegistrar");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsRegistrar");
                return false;
            }
        }
    }

    public boolean startWpsPbc(String ifaceName, String bssidStr) {
        boolean startWpsPbc;
        synchronized (this.mLock) {
            try {
                startWpsPbc = startWpsPbc(ifaceName, NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return startWpsPbc;
    }

    private boolean startWpsPbc(String ifaceName, byte[] bssid) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPbc");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsPbc(bssid), "startWpsPbc");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPbc");
                return false;
            }
        }
    }

    public boolean startWpsPinKeypad(String ifaceName, String pin) {
        if (TextUtils.isEmpty(pin)) {
            return false;
        }
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPinKeypad");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.startWpsPinKeypad(pin), "startWpsPinKeypad");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinKeypad");
                return false;
            }
        }
    }

    public String startWpsPinDisplay(String ifaceName, String bssidStr) {
        String startWpsPinDisplay;
        synchronized (this.mLock) {
            try {
                startWpsPinDisplay = startWpsPinDisplay(ifaceName, NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return null;
            } catch (Throwable th) {
                throw th;
            }
        }
        return startWpsPinDisplay;
    }

    private String startWpsPinDisplay(String ifaceName, byte[] bssid) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startWpsPinDisplay");
            if (iface == null) {
                return null;
            }
            HidlSupport.Mutable<String> gotPin = new HidlSupport.Mutable<>();
            try {
                iface.startWpsPinDisplay(bssid, new ISupplicantStaIface.startWpsPinDisplayCallback(gotPin) {
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaIfaceHal.this.lambda$startWpsPinDisplay$11$SupplicantStaIfaceHal(this.f$1, supplicantStatus, str);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinDisplay");
            }
            String str = (String) gotPin.value;
            return str;
        }
    }

    public /* synthetic */ void lambda$startWpsPinDisplay$11$SupplicantStaIfaceHal(HidlSupport.Mutable gotPin, SupplicantStatus status, String pin) {
        if (checkStatusAndLogFailure(status, "startWpsPinDisplay")) {
            gotPin.value = pin;
        }
    }

    public boolean cancelWps(String ifaceName) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "cancelWps");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.cancelWps(), "cancelWps");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "cancelWps");
                return false;
            }
        }
    }

    public boolean setExternalSim(String ifaceName, boolean useExternalSim) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "setExternalSim");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.setExternalSim(useExternalSim), "setExternalSim");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setExternalSim");
                return false;
            }
        }
    }

    public boolean enableAutoReconnect(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "enableAutoReconnect");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iface.enableAutoReconnect(enable), "enableAutoReconnect");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "enableAutoReconnect");
                return false;
            }
        }
    }

    private boolean setExtendedCommand(String ifaceName, String cmd) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "setExtendedCommand");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(iface.setExtendedCommand(cmd), "setExtendedCommand");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setExtendedCommand");
                return false;
            }
        }
    }

    private String getExtendedInfomation(String ifaceName, String cmd) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "getExtendedInfomation");
            if (iface == null) {
                return null;
            }
            HidlSupport.Mutable<String> gotInfo = new HidlSupport.Mutable<>();
            try {
                iface.getExtendedInfomation(cmd, new ISehSupplicantStaIface.getExtendedInfomationCallback(gotInfo) {
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaIfaceHal.this.lambda$getExtendedInfomation$12$SupplicantStaIfaceHal(this.f$1, supplicantStatus, str);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getExtendedInfomation");
            }
            String str = (String) gotInfo.value;
            return str;
        }
    }

    public /* synthetic */ void lambda$getExtendedInfomation$12$SupplicantStaIfaceHal(HidlSupport.Mutable gotInfo, SupplicantStatus status, String info) {
        if (checkStatusAndLogFailureExt(status, "getExtendedInfomation")) {
            gotInfo.value = info;
        }
    }

    public boolean setApRadioPowerSaveMode(String ifaceName, String iface, boolean enable) {
        int isEnabled = enable;
        if (isEnabled == 0) {
            try {
                return setExtendedCommand(ifaceName, "SET_AP_RPS 0 " + iface);
            } catch (Exception e) {
                Log.e(TAG, "General exception " + null, e);
                return false;
            }
        } else if (isEnabled != 1) {
            return false;
        } else {
            try {
                setExtendedCommand(ifaceName, "SET_AP_RPS_PARAMS 14 3 10 0 " + iface);
                return setExtendedCommand(ifaceName, "SET_AP_RPS 1 " + iface);
            } catch (Exception e2) {
                Log.e(TAG, "General exception " + null, e2);
                return false;
            }
        }
    }

    public boolean setApInterface(String ifaceName, String iface, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append(enable ? "INTERFACE_CREATE" : "INTERFACE_DELETE");
        sb.append(" ");
        sb.append(iface);
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setFccChannel(String ifaceName, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET_FCC_CHANNEL ");
        sb.append(enable ? "0" : "-1");
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setAdps(String ifaceName, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET_ADPS ");
        sb.append(enable ? "1" : "0");
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setTxPowerBackOff(String ifaceName, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET_TX_POWER_CALLING ");
        sb.append(enable ? "0" : "-1");
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setTxPowerBackOff(String ifaceName, int mode) {
        String cmd = "SET_TX_POWER_CALLING " + mode;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setTxPowerBackOff(String ifaceName, int mode, int ant) {
        String cmd = "SET_TX_POWER_CALLING " + mode + " " + ant;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setMaxDtimInSuspend(String ifaceName, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("MAX_DTIM_IN_SUSPEND ");
        sb.append(enable ? "1" : "0");
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setDtimInSuspend(String ifaceName, int interval) {
        String cmd = "SET_DTIM_IN_SUSPEND " + interval;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setPktlogFilter(String ifaceName, String filter) {
        String cmd = "PKTLOG_FILTER_ADD " + filter;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean removePktlogFilter(String ifaceName, String filter) {
        String cmd = "PKTLOG_FILTER_DEL " + filter;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean changePktlogSize(String ifaceName, String size) {
        String cmd = "PKTLOG_CHANGE_SIZE " + size;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean enablePktlogFilter(String ifaceName, boolean enable) {
        String cmd;
        if (enable) {
            cmd = "PKTLOG_FILTER_ENABLE";
        } else {
            cmd = "PKTLOG_FILTER_DISABLE";
        }
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setAffinityBooster(String ifaceName, int enable) {
        String cmd = "PCIE_IRQ_CORE " + enable;
        Log.i(TAG, "setAffinityBooster");
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setRoamTrigger(String ifaceName, int roamTrigger) {
        String cmd = "SETROAMTRIGGER " + roamTrigger;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setRoamTriggerLegacy(String ifaceName, int roamTrigger) {
        String cmd = "SETROAMTRIGGER_LEGACY " + roamTrigger;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getRoamTrigger(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETROAMTRIGGER");
        if (TextUtils.isEmpty(ret)) {
            return -1;
        }
        String[] tokens = ret.split(" ");
        try {
            if (tokens.length == 2) {
                return Integer.parseInt(tokens[1]);
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getRoamTriggerLegacy(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETROAMTRIGGER_LEGACY");
        if (TextUtils.isEmpty(ret)) {
            return -1;
        }
        String[] tokens = ret.split(" ");
        try {
            if (tokens.length == 2) {
                return Integer.parseInt(tokens[1]);
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean setRoamDelta(String ifaceName, int roamDelta) {
        String cmd = "SETROAMDELTA " + roamDelta;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getRoamDelta(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETROAMDELTA");
        if (TextUtils.isEmpty(ret)) {
            return -1;
        }
        String[] tokens = ret.split(" ");
        try {
            if (tokens.length == 2) {
                return Integer.parseInt(tokens[1]);
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean setRoamScanPeriod(String ifaceName, int roamScanPeriod) {
        String cmd = "SETROAMSCANPERIOD " + roamScanPeriod;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getRoamScanPeriod(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETROAMSCANPERIOD");
        if (TextUtils.isEmpty(ret)) {
            return -1;
        }
        String[] tokens = ret.split(" ");
        try {
            if (tokens.length == 2) {
                return Integer.parseInt(tokens[1]);
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean setRoamBand(String ifaceName, int band) {
        String cmd = "SETBAND " + band;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getRoamBand(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETBAND");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public int getDfsScanMode(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETDFSSCANMODE");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setDfsScanMode(String ifaceName, int mode) {
        String cmd = "SETDFSSCANMODE " + mode;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getWesMode(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETWESMODE");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setWesMode(String ifaceName, int mode) {
        String cmd = "SETWESMODE " + mode;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getNCHOMode(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETNCHOMODE");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setNCHOMode(String ifaceName, int mode) {
        String cmd = "SETNCHOMODE " + mode;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setBand(String ifaceName, int band) {
        String cmd = "SETBAND " + band;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setCountryRev(String ifaceName, String countryRev) {
        String cmd = "SETCOUNTRYREV " + countryRev;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public String getCountryRev(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETCOUNTRYREV");
        if (TextUtils.isEmpty(ret)) {
            return null;
        }
        String[] tokens = ret.split(" ");
        if (tokens.length != 3) {
            return null;
        }
        return tokens[1] + " " + tokens[2];
    }

    public String getPstime(String ifaceName, String iface) {
        String ret = getExtendedInfomation(ifaceName, "GET_AP_RPS " + iface);
        if (!TextUtils.isEmpty(ret)) {
            return ret;
        }
        return null;
    }

    public int getScanChannelTime(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETSCANCHANNELTIME");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setScanChannelTime(String ifaceName, String time) {
        String cmd = "SETSCANCHANNELTIME " + time;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getScanHomeTime(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETSCANHOMETIME");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setScanHomeTime(String ifaceName, String time) {
        String cmd = "SETSCANHOMETIME " + time;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getScanHomeAwayTime(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETSCANHOMEAWAYTIME");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setScanHomeAwayTime(String ifaceName, String time) {
        String cmd = "SETSCANHOMEAWAYTIME " + time;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getScanNProbes(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GETSCANNPROBES");
        if (!TextUtils.isEmpty(ret)) {
            String[] tokens = ret.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setScanNProbes(String ifaceName, String num) {
        String cmd = "SETSCANNPROBES " + num;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean reAssoc(String ifaceName, String param) {
        String cmd = "REASSOC " + param;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean reAssocLegacy(String ifaceName, String param) {
        String cmd = "REASSOC_LEGACY " + param;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean sendActionFrame(String ifaceName, String param) {
        String cmd = "SENDACTIONFRAME " + param;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public void setScanChannelTimeLegacy(String ifaceName, String time) {
        String cmd = "SETSCANCHANNELTIME_LEGACY " + time;
        try {
            if (!setExtendedCommand(ifaceName, cmd)) {
                Log.i(TAG, "Failed to " + cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
        }
    }

    public void setScanPassiveTimeLegacy(String ifaceName, String time) {
        String cmd = "SETSCANPASSIVETIME_LEGACY " + time;
        try {
            if (!setExtendedCommand(ifaceName, cmd)) {
                Log.i(TAG, "Failed to " + cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
        }
    }

    public void setScanHomeTimeLegacy(String ifaceName, String time) {
        String cmd = "SETSCANHOMETIME_LEGACY " + time;
        try {
            if (!setExtendedCommand(ifaceName, cmd)) {
                Log.i(TAG, "Failed to " + cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
        }
    }

    public void setScanHomeAwayTimeLegacy(String ifaceName, String time) {
        String cmd = "SETSCANHOMEAWAYTIME_LEGACY " + time;
        try {
            if (!setExtendedCommand(ifaceName, cmd)) {
                Log.i(TAG, "Failed to " + cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
        }
    }

    public int fetchFrequency(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GET FREQUENCY");
        if (TextUtils.isEmpty(stringResult)) {
            return -1;
        }
        try {
            return Integer.parseInt(stringResult);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean setAmpdu(String ifaceName, int ampdu) {
        String cmd = "AMPDU_MPDU " + ampdu;
        if (ampdu < -1 || ampdu > 32) {
            return false;
        }
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setRoamScanControl(String ifaceName, int mode) {
        String cmd = "SETROAMSCANCONTROL " + mode;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setRoamScanChannels(String ifaceName, String chinfo) {
        String cmd = "SETROAMSCANCHANNELS " + chinfo;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean addRoamScanChannels(String ifaceName, String chinfo) {
        String cmd = "ADDROAMSCANCHANNELS " + chinfo;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean addRoamScanChannelsLegacy(String ifaceName, String chinfo) {
        String cmd = "ADDROAMSCANCHANNELS_LEGACY " + chinfo;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getRoamScanControl(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GETROAMSCANCONTROL");
        if (!TextUtils.isEmpty(stringResult)) {
            String[] tokens = stringResult.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public String getRoamScanChannels(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GETROAMSCANCHANNELS");
        if (TextUtils.isEmpty(stringResult) || stringResult.length() <= 20 || stringResult.substring(0, 20).compareTo("GETROAMSCANCHANNELS ") != 0) {
            return null;
        }
        return stringResult.substring(20);
    }

    public String getRoamScanChannelsLegacy(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GETROAMSCANCHANNELS_LEGACY");
        if (TextUtils.isEmpty(stringResult) || stringResult.length() <= 27 || stringResult.substring(0, 27).compareTo("GETROAMSCANCHANNELS_LEGACY ") != 0) {
            return null;
        }
        return stringResult.substring(27);
    }

    public boolean setLqcmEnable(String ifaceName, int enable) {
        String cmd = "SET_LQCM_ENABLE " + enable;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getLqcmReport(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GET_LQCM_REPORT");
        if (!TextUtils.isEmpty(stringResult)) {
            String[] tokens = stringResult.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public int getSnr(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GET_SNR");
        if (!TextUtils.isEmpty(stringResult)) {
            String[] tokens = stringResult.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean setLatencyCritical(String ifaceName, int enable) {
        String cmd = "SET_LATENCY_CRT_DATA " + enable;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getApCu(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GET_CU");
        if (!TextUtils.isEmpty(stringResult)) {
            String[] tokens = stringResult.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean notifyStateScanOnly(String ifaceName, boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("SETSINGLEANT ");
        sb.append(enable ? "1" : "0");
        String cmd = sb.toString();
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setTidMode(String ifaceName, int mode, int uid, int tid) {
        String cmd = "SET_TID " + mode + " " + uid + " " + tid;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public String getTidMode(String ifaceName) {
        String ret = getExtendedInfomation(ifaceName, "GET_TID");
        if (TextUtils.isEmpty(ret)) {
            return null;
        }
        String[] tokens = ret.split(" ");
        if (tokens.length != 4) {
            return null;
        }
        return tokens[1] + " " + tokens[2] + " " + tokens[3];
    }

    public int getBand(String ifaceName) {
        String stringResult = getExtendedInfomation(ifaceName, "GETBAND");
        if (!TextUtils.isEmpty(stringResult)) {
            String[] tokens = stringResult.split(" ");
            try {
                if (tokens.length == 2) {
                    return Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public void requestFwBigDataParams(String ifaceName, int type, int additionalType, int moreType) {
        String cmd = "REQUEST_DRIVER_BIGDATA " + type + " " + additionalType + " " + moreType;
        try {
            getExtendedInfomation(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
        }
    }

    public boolean saveDebugDump(String ifaceName) {
        try {
            return setExtendedCommand(ifaceName, "DEBUG_DUMP");
        } catch (Exception e) {
            Log.e(TAG, "General exception DEBUG_DUMP", e);
            return false;
        }
    }

    public boolean simAbsent(String ifaceName) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "simAbsent");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(iface.simAbsent(), "simAbsent");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "simAbsent");
                return false;
            }
        }
    }

    public boolean setInterwokingEnabled(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "setInterwokingEnabled");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(iface.setInterwokingEnabled(enable), "setInterwokingEnabled");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setInterwokingEnabled");
                return false;
            }
        }
    }

    public boolean setAnalyticsDisconnectReason(String ifaceName, short reason) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "setAnalyticsDisconnectReason");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(iface.setAnalyticsDisconnectReason(reason), "setAnalyticsDisconnectReason");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setAnalyticsDisconnectReason");
                return false;
            }
        }
    }

    public boolean setSafeMode(String ifaceName, boolean enable) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface iface = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "setSafeMode");
            if (iface == null) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(iface.setSafeMode(enable), "setSafeMode");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setSafeMode");
                return false;
            }
        }
    }

    public boolean setExtendedAnalyticsDisconnectReason(String ifaceName, short reason) {
        String cmd = "SET_DISCONNECT_IES dd090000f0220301020" + Integer.toHexString(reason);
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean updateCellularCapabilities(String ifaceName, byte state, byte type, byte scale, byte[] cellId) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface ifaceExt = checkSupplicantStaIfaceExtAndLogFailure(ifaceName, "updateCellularCapabilities");
            if (ifaceExt == null) {
                return false;
            }
            if (cellId.length != 2) {
                return false;
            }
            try {
                Log.d(TAG, "updateCellularCapabilities called");
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(ifaceExt.updateCellularCapabilities(state, type, scale, cellId), "updateCellularCapabilities");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "updateCellularCapabilities");
                return false;
            }
        }
    }

    public boolean setWifiToCellular(String ifaceName, int wtcMode, int scanMode, int rssiThreshold, int candidateRssiThreshold24G, int candidateRssiThreshold5G, int candidateRssiThreshold6G) {
        String cmd = "SETWTCMODE " + wtcMode + " " + scanMode + " " + rssiThreshold + " " + candidateRssiThreshold24G + " " + candidateRssiThreshold5G + " " + candidateRssiThreshold6G;
        try {
            Log.d(TAG, "setWifiToCellular called : " + cmd);
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public boolean setLogLevel(boolean turnOnVerbose) {
        int logLevel;
        boolean debugParams;
        synchronized (this.mLock) {
            if (turnOnVerbose) {
                logLevel = 2;
            } else {
                logLevel = 3;
            }
            debugParams = setDebugParams(logLevel, false, false);
        }
        return debugParams;
    }

    private boolean setDebugParams(int level, boolean showTimestamp, boolean showKeys) {
        synchronized (this.mLock) {
            if (!checkSupplicantAndLogFailure("setDebugParams")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicant.setDebugParams(level, showTimestamp, showKeys), "setDebugParams");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setDebugParams");
                return false;
            }
        }
    }

    public boolean setConcurrencyPriority(boolean isStaHigherPriority) {
        synchronized (this.mLock) {
            if (isStaHigherPriority) {
                boolean concurrencyPriority = setConcurrencyPriority(0);
                return concurrencyPriority;
            }
            boolean concurrencyPriority2 = setConcurrencyPriority(1);
            return concurrencyPriority2;
        }
    }

    private boolean setConcurrencyPriority(int type) {
        synchronized (this.mLock) {
            if (!checkSupplicantAndLogFailure("setConcurrencyPriority")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicant.setConcurrencyPriority(type), "setConcurrencyPriority");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setConcurrencyPriority");
                return false;
            }
        }
    }

    private boolean checkSupplicantAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISupplicant != null) {
                return true;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISupplicant is null");
            return false;
        }
    }

    private boolean checkSupplicantExtAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISehSupplicant != null) {
                return true;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISehSupplicant is null");
            return false;
        }
    }

    private ISupplicantStaIface checkSupplicantStaIfaceAndLogFailure(String ifaceName, String methodStr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iface = getStaIface(ifaceName);
            if (iface != null) {
                return iface;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISupplicantStaIface is null");
            return null;
        }
    }

    private ISehSupplicantStaIface checkSupplicantStaIfaceExtAndLogFailure(String ifaceName, String methodStr) {
        synchronized (this.mLock) {
            ISehSupplicantStaIface ifaceExt = getStaIfaceExt(ifaceName);
            if (ifaceExt != null) {
                return ifaceExt;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISehSupplicantStaIface is null");
            return null;
        }
    }

    private SupplicantStaNetworkHal checkSupplicantStaNetworkAndLogFailure(String ifaceName, String methodStr) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHal = getCurrentNetworkRemoteHandle(ifaceName);
            if (networkHal != null) {
                return networkHal;
            }
            Log.e(TAG, "Can't call " + methodStr + ", SupplicantStaNetwork is null");
            return null;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed: " + status);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIface." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailureExt(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISehSupplicantStaIface." + methodStr + " failed: " + status);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISehSupplicantStaIface." + methodStr + " succeeded");
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIfaceCallback." + methodStr + " received");
            }
        }
    }

    /* access modifiers changed from: private */
    public void logCallbackExt(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISehSupplicantStaIfaceCallback." + methodStr + " received");
            }
        }
    }

    private void handleNoSuchElementException(NoSuchElementException e, String methodStr) {
        synchronized (this.mLock) {
            clearState();
            Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed with exception", e);
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            clearState();
            Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed with exception", e);
        }
    }

    private void handleNoSuchElementExceptionExt(NoSuchElementException e, String methodStr) {
        synchronized (this.mLock) {
            clearState();
            Log.e(TAG, "ISupplicantStaIface." + methodStr + " failed with exception", e);
        }
    }

    private void handleRemoteExceptionExt(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            clearStateExt();
            Log.e(TAG, "ISehSupplicantStaIface." + methodStr + " failed with exception", e);
        }
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static short stringToWpsConfigMethod(java.lang.String r5) {
        /*
            int r0 = r5.hashCode()
            r1 = 8
            r2 = 4
            r3 = 2
            r4 = 1
            switch(r0) {
                case -1781962557: goto L_0x0099;
                case -1419358249: goto L_0x008f;
                case -1134657068: goto L_0x0085;
                case -614489202: goto L_0x007a;
                case -522593958: goto L_0x006f;
                case -423872603: goto L_0x0065;
                case -416734217: goto L_0x005b;
                case 3388229: goto L_0x0050;
                case 3599197: goto L_0x0046;
                case 102727412: goto L_0x003b;
                case 179612103: goto L_0x0030;
                case 1146869903: goto L_0x0024;
                case 1671764162: goto L_0x0019;
                case 2010140181: goto L_0x000e;
                default: goto L_0x000c;
            }
        L_0x000c:
            goto L_0x00a4
        L_0x000e:
            java.lang.String r0 = "int_nfc_token"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = r2
            goto L_0x00a5
        L_0x0019:
            java.lang.String r0 = "display"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 3
            goto L_0x00a5
        L_0x0024:
            java.lang.String r0 = "physical_push_button"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 10
            goto L_0x00a5
        L_0x0030:
            java.lang.String r0 = "ext_nfc_token"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 5
            goto L_0x00a5
        L_0x003b:
            java.lang.String r0 = "label"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = r3
            goto L_0x00a5
        L_0x0046:
            java.lang.String r0 = "usba"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 0
            goto L_0x00a5
        L_0x0050:
            java.lang.String r0 = "p2ps"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 11
            goto L_0x00a5
        L_0x005b:
            java.lang.String r0 = "push_button"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 7
            goto L_0x00a5
        L_0x0065:
            java.lang.String r0 = "nfc_interface"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 6
            goto L_0x00a5
        L_0x006f:
            java.lang.String r0 = "physical_display"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 13
            goto L_0x00a5
        L_0x007a:
            java.lang.String r0 = "virtual_display"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 12
            goto L_0x00a5
        L_0x0085:
            java.lang.String r0 = "keypad"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = r1
            goto L_0x00a5
        L_0x008f:
            java.lang.String r0 = "ethernet"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = r4
            goto L_0x00a5
        L_0x0099:
            java.lang.String r0 = "virtual_push_button"
            boolean r0 = r5.equals(r0)
            if (r0 == 0) goto L_0x000c
            r0 = 9
            goto L_0x00a5
        L_0x00a4:
            r0 = -1
        L_0x00a5:
            switch(r0) {
                case 0: goto L_0x00e0;
                case 1: goto L_0x00df;
                case 2: goto L_0x00de;
                case 3: goto L_0x00dd;
                case 4: goto L_0x00da;
                case 5: goto L_0x00d7;
                case 6: goto L_0x00d4;
                case 7: goto L_0x00d1;
                case 8: goto L_0x00ce;
                case 9: goto L_0x00cb;
                case 10: goto L_0x00c8;
                case 11: goto L_0x00c5;
                case 12: goto L_0x00c2;
                case 13: goto L_0x00bf;
                default: goto L_0x00a8;
            }
        L_0x00a8:
            java.lang.IllegalArgumentException r0 = new java.lang.IllegalArgumentException
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Invalid WPS config method: "
            r1.append(r2)
            r1.append(r5)
            java.lang.String r1 = r1.toString()
            r0.<init>(r1)
            throw r0
        L_0x00bf:
            r0 = 16392(0x4008, float:2.297E-41)
            return r0
        L_0x00c2:
            r0 = 8200(0x2008, float:1.149E-41)
            return r0
        L_0x00c5:
            r0 = 4096(0x1000, float:5.74E-42)
            return r0
        L_0x00c8:
            r0 = 1152(0x480, float:1.614E-42)
            return r0
        L_0x00cb:
            r0 = 640(0x280, float:8.97E-43)
            return r0
        L_0x00ce:
            r0 = 256(0x100, float:3.59E-43)
            return r0
        L_0x00d1:
            r0 = 128(0x80, float:1.794E-43)
            return r0
        L_0x00d4:
            r0 = 64
            return r0
        L_0x00d7:
            r0 = 16
            return r0
        L_0x00da:
            r0 = 32
            return r0
        L_0x00dd:
            return r1
        L_0x00de:
            return r2
        L_0x00df:
            return r3
        L_0x00e0:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.stringToWpsConfigMethod(java.lang.String):short");
    }

    /* access modifiers changed from: private */
    public static SupplicantState supplicantHidlStateToFrameworkState(int state) {
        switch (state) {
            case 0:
                return SupplicantState.DISCONNECTED;
            case 1:
                return SupplicantState.INTERFACE_DISABLED;
            case 2:
                return SupplicantState.INACTIVE;
            case 3:
                return SupplicantState.SCANNING;
            case 4:
                return SupplicantState.AUTHENTICATING;
            case 5:
                return SupplicantState.ASSOCIATING;
            case 6:
                return SupplicantState.ASSOCIATED;
            case 7:
                return SupplicantState.FOUR_WAY_HANDSHAKE;
            case 8:
                return SupplicantState.GROUP_HANDSHAKE;
            case 9:
                return SupplicantState.COMPLETED;
            default:
                throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    public boolean updateCurrentBss(String ifaceName) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal networkHandle = checkSupplicantStaNetworkAndLogFailure(ifaceName, "updateCurrentBss");
            if (networkHandle == null) {
                return false;
            }
            boolean updateCurrentBss = networkHandle.updateCurrentBss();
            return updateCurrentBss;
        }
    }

    private class SupplicantStaIfaceHalCallback extends ISupplicantStaIfaceCallback.Stub {
        private String mIfaceName;
        private boolean mStateIsFourway = false;

        SupplicantStaIfaceHalCallback(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        private ANQPElement parseAnqpElement(Constants.ANQPElementType infoID, ArrayList<Byte> payload) {
            ANQPElement aNQPElement;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                try {
                    if (Constants.getANQPElementID(infoID) != null) {
                        aNQPElement = ANQPParser.parseElement(infoID, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(payload)));
                    } else {
                        aNQPElement = ANQPParser.parseHS20Element(infoID, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(payload)));
                    }
                } catch (IOException | BufferUnderflowException e) {
                    Log.e(SupplicantStaIfaceHal.TAG, "Failed parsing ANQP element payload: " + infoID, e);
                    return null;
                } catch (Throwable th) {
                    throw th;
                }
            }
            return aNQPElement;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:11:0x001a, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:13:0x001c, code lost:
            return;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void addAnqpElementToMap(java.util.Map<com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType, com.android.server.wifi.hotspot2.anqp.ANQPElement> r3, com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType r4, java.util.ArrayList<java.lang.Byte> r5) {
            /*
                r2 = this;
                com.android.server.wifi.SupplicantStaIfaceHal r0 = com.android.server.wifi.SupplicantStaIfaceHal.this
                java.lang.Object r0 = r0.mLock
                monitor-enter(r0)
                if (r5 == 0) goto L_0x001b
                boolean r1 = r5.isEmpty()     // Catch:{ all -> 0x001d }
                if (r1 == 0) goto L_0x0010
                goto L_0x001b
            L_0x0010:
                com.android.server.wifi.hotspot2.anqp.ANQPElement r1 = r2.parseAnqpElement(r4, r5)     // Catch:{ all -> 0x001d }
                if (r1 == 0) goto L_0x0019
                r3.put(r4, r1)     // Catch:{ all -> 0x001d }
            L_0x0019:
                monitor-exit(r0)     // Catch:{ all -> 0x001d }
                return
            L_0x001b:
                monitor-exit(r0)     // Catch:{ all -> 0x001d }
                return
            L_0x001d:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x001d }
                throw r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaIfaceHal.SupplicantStaIfaceHalCallback.addAnqpElementToMap(java.util.Map, com.android.server.wifi.hotspot2.anqp.Constants$ANQPElementType, java.util.ArrayList):void");
        }

        public void onNetworkAdded(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkAdded");
            }
        }

        public void onNetworkRemoved(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkRemoved");
                this.mStateIsFourway = false;
            }
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onStateChanged");
                SupplicantState newSupplicantState = SupplicantStaIfaceHal.supplicantHidlStateToFrameworkState(newState);
                WifiSsid wifiSsid = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(ssid));
                String bssidStr = NativeUtil.macAddressFromByteArray(bssid);
                this.mStateIsFourway = newState == 7;
                if (newSupplicantState == SupplicantState.COMPLETED) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkConnectionEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), bssidStr);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastSupplicantStateChangeEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), wifiSsid, bssidStr, newSupplicantState);
            }
        }

        public void onAnqpQueryDone(byte[] bssid, ISupplicantStaIfaceCallback.AnqpData data, ISupplicantStaIfaceCallback.Hs20AnqpData hs20Data) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAnqpQueryDone");
                Map<Constants.ANQPElementType, ANQPElement> elementsMap = new HashMap<>();
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQPVenueName, data.venueName);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQPRoamingConsortium, data.roamingConsortium);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQPIPAddrAvailability, data.ipAddrTypeAvailability);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQPNAIRealm, data.naiRealm);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQP3GPPNetwork, data.anqp3gppCellularNetwork);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.ANQPDomName, data.domainName);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.HSFriendlyName, hs20Data.operatorFriendlyName);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.HSWANMetrics, hs20Data.wanMetrics);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.HSConnCapability, hs20Data.connectionCapability);
                addAnqpElementToMap(elementsMap, Constants.ANQPElementType.HSOSUProviders, hs20Data.osuProvidersList);
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAnqpDoneEvent(this.mIfaceName, new AnqpEvent(NativeUtil.macAddressToLong(bssid).longValue(), elementsMap));
            }
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20IconQueryDone");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastIconDoneEvent(this.mIfaceName, new IconEvent(NativeUtil.macAddressToLong(bssid).longValue(), fileName, data.size(), NativeUtil.byteArrayFromArrayList(data)));
            }
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20SubscriptionRemediation");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWnmEvent(this.mIfaceName, new WnmData(NativeUtil.macAddressToLong(bssid).longValue(), url, osuMethod));
            }
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20DeauthImminentNotice");
                WifiMonitor access$1600 = SupplicantStaIfaceHal.this.mWifiMonitor;
                String str = this.mIfaceName;
                long longValue = NativeUtil.macAddressToLong(bssid).longValue();
                boolean z = true;
                if (reasonCode != 1) {
                    z = false;
                }
                access$1600.broadcastWnmEvent(str, new WnmData(longValue, url, z, reAuthDelayInSec));
            }
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            MobileWipsFrameworkService mwfs;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onDisconnected");
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.e(SupplicantStaIfaceHal.TAG, "onDisconnected 4way=" + this.mStateIsFourway + " locallyGenerated=" + locallyGenerated + " reasonCode=" + reasonCode);
                }
                if (this.mStateIsFourway && (!locallyGenerated || reasonCode != 17)) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 2, -1, NativeUtil.macAddressFromByteArray(bssid));
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkDisconnectionEvent(this.mIfaceName, locallyGenerated ? 1 : 0, reasonCode, NativeUtil.macAddressFromByteArray(bssid));
                if ("".equals(SupplicantStaIfaceHal.CONFIG_SECURE_SVC_INTEGRATION) && !SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEMWIPS) && (mwfs = MobileWipsFrameworkService.getInstance()) != null) {
                    mwfs.sendEmptyMessage(10);
                }
            }
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            WifiConfiguration curConfiguration;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAssociationRejected");
                if (statusCode == 1 && (curConfiguration = SupplicantStaIfaceHal.this.getCurrentNetworkLocalConfig(this.mIfaceName)) != null && curConfiguration.allowedKeyManagement.get(8)) {
                    SupplicantStaIfaceHal.this.logCallback("SAE incorrect password");
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 2, -1);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociationRejectionEvent(this.mIfaceName, statusCode, timedOut, NativeUtil.macAddressFromByteArray(bssid));
            }
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAuthenticationTimeout");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 1, -1);
            }
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onBssidChanged");
                if (reason == 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastTargetBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                } else if (reason == 1) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociatedBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bssid));
                }
            }
        }

        public void onEapFailure() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, -1);
            }
        }

        public void onWpsEventSuccess() {
            SupplicantStaIfaceHal.this.logCallback("onWpsEventSuccess");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsSuccessEvent(this.mIfaceName);
            }
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventFail");
                if (configError == 16 && errorInd == 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsTimeoutEvent(this.mIfaceName);
                } else {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsFailEvent(this.mIfaceName, configError, errorInd);
                }
            }
        }

        public void onWpsEventPbcOverlap() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventPbcOverlap");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsOverlapEvent(this.mIfaceName);
            }
        }

        public void onExtRadioWorkStart(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkStart");
            }
        }

        public void onExtRadioWorkTimeout(int id) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkTimeout");
            }
        }
    }

    private class SupplicantStaIfaceHalCallbackExt extends ISehSupplicantStaIfaceCallback.Stub {
        private String mIfaceName;

        SupplicantStaIfaceHalCallbackExt(String ifaceName) {
            this.mIfaceName = ifaceName;
        }

        public void onNotifyBigdata(String feature, String params) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onNotifyBigdata");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastBigdataEvent(this.mIfaceName, feature, params);
            }
        }

        public void onEapEvent(int event, String notification, String message) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onEapEvent");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastEapEvent(this.mIfaceName, event, notification, message);
            }
        }

        public void onDriverHang(String state, String msg) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onDriverHang");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastDriverHangEvent(this.mIfaceName, state, msg);
            }
        }

        public void onBeaconInterval(String ssid, String bssid, int channel, int beacon_interval, long timestamp, long system_time) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onBcnInterval");
                MobileWipsFrameworkService mwfs = MobileWipsFrameworkService.getInstance();
                if (mwfs != null) {
                    mwfs.broadcastBcnIntervalEvent(this.mIfaceName, ssid, bssid, channel, beacon_interval, timestamp, system_time);
                }
            }
        }

        public void onBeaconEventAbort(int abort_reason) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onBeaconEventAbort");
                MobileWipsFrameworkService mwfs = MobileWipsFrameworkService.getInstance();
                if (mwfs != null) {
                    mwfs.broadcastBcnEventAbort(this.mIfaceName, abort_reason);
                }
            }
        }

        public void onBssidPruned(String ssid, String bssid, int rssi, int reason, int timeRemaining) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onBssidPruned");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastBssidPrunedEvent(this.mIfaceName, ssid, bssid, rssi, reason, timeRemaining);
            }
        }

        public void onRoamingChannelListUpdate(String ssid, ArrayList<Integer> channelList) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallbackExt("onRclUpdate");
                WifiRoamingAssistant mWifiRoamingAssistant = WifiRoamingAssistant.getInstance();
                if (mWifiRoamingAssistant != null) {
                    StringBuffer buf = new StringBuffer();
                    Iterator<Integer> it = channelList.iterator();
                    while (it.hasNext()) {
                        buf.append(it.next() + " ");
                    }
                    Log.i(SupplicantStaIfaceHal.TAG, "onRclUpdate: " + ssid + buf.toString());
                    mWifiRoamingAssistant.onDriverEventReceived(ssid, channelList);
                }
            }
        }
    }

    private class SupplicantStaIfaceHalCallbackV1_1 extends ISupplicantStaIfaceCallback.Stub {
        private SupplicantStaIfaceHalCallback mCallbackV1_0;
        private String mIfaceName;

        SupplicantStaIfaceHalCallbackV1_1(String ifaceName, SupplicantStaIfaceHalCallback callback) {
            this.mIfaceName = ifaceName;
            this.mCallbackV1_0 = callback;
        }

        public void onNetworkAdded(int id) {
            this.mCallbackV1_0.onNetworkAdded(id);
        }

        public void onNetworkRemoved(int id) {
            this.mCallbackV1_0.onNetworkRemoved(id);
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            this.mCallbackV1_0.onStateChanged(newState, bssid, id, ssid);
        }

        public void onAnqpQueryDone(byte[] bssid, ISupplicantStaIfaceCallback.AnqpData data, ISupplicantStaIfaceCallback.Hs20AnqpData hs20Data) {
            this.mCallbackV1_0.onAnqpQueryDone(bssid, data, hs20Data);
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            this.mCallbackV1_0.onHs20IconQueryDone(bssid, fileName, data);
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            this.mCallbackV1_0.onHs20SubscriptionRemediation(bssid, osuMethod, url);
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            this.mCallbackV1_0.onHs20DeauthImminentNotice(bssid, reasonCode, reAuthDelayInSec, url);
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            this.mCallbackV1_0.onDisconnected(bssid, locallyGenerated, reasonCode);
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            this.mCallbackV1_0.onAssociationRejected(bssid, statusCode, timedOut);
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            this.mCallbackV1_0.onAuthenticationTimeout(bssid);
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            this.mCallbackV1_0.onBssidChanged(reason, bssid);
        }

        public void onEapFailure() {
            this.mCallbackV1_0.onEapFailure();
        }

        public void onEapFailure_1_1(int code) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure_1_1");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, code);
            }
        }

        public void onWpsEventSuccess() {
            this.mCallbackV1_0.onWpsEventSuccess();
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            this.mCallbackV1_0.onWpsEventFail(bssid, configError, errorInd);
        }

        public void onWpsEventPbcOverlap() {
            this.mCallbackV1_0.onWpsEventPbcOverlap();
        }

        public void onExtRadioWorkStart(int id) {
            this.mCallbackV1_0.onExtRadioWorkStart(id);
        }

        public void onExtRadioWorkTimeout(int id) {
            this.mCallbackV1_0.onExtRadioWorkTimeout(id);
        }
    }

    private class SupplicantStaIfaceHalCallbackV1_2 extends ISupplicantStaIfaceCallback.Stub {
        private SupplicantStaIfaceHalCallbackV1_1 mCallbackV1_1;

        SupplicantStaIfaceHalCallbackV1_2(SupplicantStaIfaceHalCallbackV1_1 callback) {
            this.mCallbackV1_1 = callback;
        }

        public void onNetworkAdded(int id) {
            this.mCallbackV1_1.onNetworkAdded(id);
        }

        public void onNetworkRemoved(int id) {
            this.mCallbackV1_1.onNetworkRemoved(id);
        }

        public void onStateChanged(int newState, byte[] bssid, int id, ArrayList<Byte> ssid) {
            this.mCallbackV1_1.onStateChanged(newState, bssid, id, ssid);
        }

        public void onAnqpQueryDone(byte[] bssid, ISupplicantStaIfaceCallback.AnqpData data, ISupplicantStaIfaceCallback.Hs20AnqpData hs20Data) {
            this.mCallbackV1_1.onAnqpQueryDone(bssid, data, hs20Data);
        }

        public void onHs20IconQueryDone(byte[] bssid, String fileName, ArrayList<Byte> data) {
            this.mCallbackV1_1.onHs20IconQueryDone(bssid, fileName, data);
        }

        public void onHs20SubscriptionRemediation(byte[] bssid, byte osuMethod, String url) {
            this.mCallbackV1_1.onHs20SubscriptionRemediation(bssid, osuMethod, url);
        }

        public void onHs20DeauthImminentNotice(byte[] bssid, int reasonCode, int reAuthDelayInSec, String url) {
            this.mCallbackV1_1.onHs20DeauthImminentNotice(bssid, reasonCode, reAuthDelayInSec, url);
        }

        public void onDisconnected(byte[] bssid, boolean locallyGenerated, int reasonCode) {
            this.mCallbackV1_1.onDisconnected(bssid, locallyGenerated, reasonCode);
        }

        public void onAssociationRejected(byte[] bssid, int statusCode, boolean timedOut) {
            this.mCallbackV1_1.onAssociationRejected(bssid, statusCode, timedOut);
        }

        public void onAuthenticationTimeout(byte[] bssid) {
            this.mCallbackV1_1.onAuthenticationTimeout(bssid);
        }

        public void onBssidChanged(byte reason, byte[] bssid) {
            this.mCallbackV1_1.onBssidChanged(reason, bssid);
        }

        public void onEapFailure() {
            this.mCallbackV1_1.onEapFailure();
        }

        public void onEapFailure_1_1(int code) {
            this.mCallbackV1_1.onEapFailure_1_1(code);
        }

        public void onWpsEventSuccess() {
            this.mCallbackV1_1.onWpsEventSuccess();
        }

        public void onWpsEventFail(byte[] bssid, short configError, short errorInd) {
            this.mCallbackV1_1.onWpsEventFail(bssid, configError, errorInd);
        }

        public void onWpsEventPbcOverlap() {
            this.mCallbackV1_1.onWpsEventPbcOverlap();
        }

        public void onExtRadioWorkStart(int id) {
            this.mCallbackV1_1.onExtRadioWorkStart(id);
        }

        public void onExtRadioWorkTimeout(int id) {
            this.mCallbackV1_1.onExtRadioWorkTimeout(id);
        }

        public void onDppSuccessConfigReceived(ArrayList<Byte> ssid, String password, byte[] psk, int securityAkm) {
            if (SupplicantStaIfaceHal.this.mDppCallback == null) {
                SupplicantStaIfaceHal.loge("onDppSuccessConfigReceived callback is null");
                return;
            }
            WifiConfiguration newWifiConfiguration = new WifiConfiguration();
            WifiSsid wifiSsid = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(ssid));
            newWifiConfiguration.SSID = "\"" + wifiSsid.toString() + "\"";
            if (password != null) {
                newWifiConfiguration.preSharedKey = "\"" + password + "\"";
            } else if (psk != null) {
                newWifiConfiguration.preSharedKey = psk.toString();
            }
            if (securityAkm == 2 || securityAkm == 1) {
                newWifiConfiguration.allowedKeyManagement.set(8);
                newWifiConfiguration.requirePMF = true;
            } else if (securityAkm == 0) {
                newWifiConfiguration.allowedKeyManagement.set(1);
            } else {
                onDppFailure(7);
                return;
            }
            newWifiConfiguration.creatorName = SupplicantStaIfaceHal.this.mContext.getPackageManager().getNameForUid(1010);
            newWifiConfiguration.allowedAuthAlgorithms.set(0);
            newWifiConfiguration.allowedPairwiseCiphers.set(2);
            newWifiConfiguration.allowedProtocols.set(1);
            newWifiConfiguration.status = 2;
            SupplicantStaIfaceHal.this.mDppCallback.onSuccessConfigReceived(newWifiConfiguration);
        }

        public void onDppSuccessConfigSent() {
            if (SupplicantStaIfaceHal.this.mDppCallback != null) {
                SupplicantStaIfaceHal.this.mDppCallback.onSuccessConfigSent();
            } else {
                SupplicantStaIfaceHal.loge("onSuccessConfigSent callback is null");
            }
        }

        public void onDppProgress(int code) {
            if (SupplicantStaIfaceHal.this.mDppCallback != null) {
                SupplicantStaIfaceHal.this.mDppCallback.onProgress(code);
            } else {
                SupplicantStaIfaceHal.loge("onDppProgress callback is null");
            }
        }

        public void onDppFailure(int code) {
            if (SupplicantStaIfaceHal.this.mDppCallback != null) {
                SupplicantStaIfaceHal.this.mDppCallback.onFailure(code);
            } else {
                SupplicantStaIfaceHal.loge("onDppFailure callback is null");
            }
        }
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    private static void logi(String s) {
        Log.i(TAG, s);
    }

    /* access modifiers changed from: private */
    public static void loge(String s) {
        Log.e(TAG, s);
    }

    public boolean beaconIntervalStart(String ifaceName) {
        try {
            return setExtendedCommand(ifaceName, "BEACON_RECV start");
        } catch (Exception e) {
            Log.e(TAG, "General exception BEACON_RECV start", e);
            return false;
        }
    }

    public boolean beaconIntervalStop(String ifaceName) {
        try {
            return setExtendedCommand(ifaceName, "BEACON_RECV stop");
        } catch (Exception e) {
            Log.e(TAG, "General exception BEACON_RECV stop", e);
            return false;
        }
    }

    public boolean setIndoorChannels(String ifaceName, int numOfChannels, String channels) {
        String tmpCmd = "SET_INDOOR_CHANNELS " + numOfChannels;
        String[] channelList = channels.split(" ");
        for (int i = 0; i < numOfChannels; i++) {
            tmpCmd = tmpCmd + " " + channelList[i];
        }
        String cmd = tmpCmd;
        try {
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }

    public int getAdvancedKeyMgmtCapabilities(String ifaceName) {
        int advancedCapabilities = 0;
        int keyMgmtCapabilities = getKeyMgmtCapabilities(ifaceName);
        if ((keyMgmtCapabilities & 1024) != 0) {
            advancedCapabilities = 0 | 134217728;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "getAdvancedKeyMgmtCapabilities: SAE supported");
            }
        }
        if ((131072 & keyMgmtCapabilities) != 0) {
            advancedCapabilities |= 268435456;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "getAdvancedKeyMgmtCapabilities: SUITE_B supported");
            }
        }
        if ((4194304 & keyMgmtCapabilities) != 0) {
            advancedCapabilities |= 536870912;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "getAdvancedKeyMgmtCapabilities: OWE supported");
            }
        }
        if ((8388608 & keyMgmtCapabilities) != 0) {
            advancedCapabilities |= Integer.MIN_VALUE;
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "getAdvancedKeyMgmtCapabilities: DPP supported");
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "getAdvancedKeyMgmtCapabilities: Capability flags = " + keyMgmtCapabilities);
        }
        return advancedCapabilities;
    }

    private int getKeyMgmtCapabilities(String ifaceName) {
        MutableBoolean status = new MutableBoolean(false);
        MutableInt keyMgmtMask = new MutableInt(0);
        if (isV1_2()) {
            ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "getKeyMgmtCapabilities");
            if (iface == null) {
                return 0;
            }
            android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
            if (staIfaceV12 == null) {
                Log.e(TAG, "getKeyMgmtCapabilities: ISupplicantStaIface is null, cannot get advanced capabilities");
                return 0;
            }
            try {
                staIfaceV12.getKeyMgmtCapabilities(new ISupplicantStaIface.getKeyMgmtCapabilitiesCallback(status, keyMgmtMask) {
                    private final /* synthetic */ MutableBoolean f$1;
                    private final /* synthetic */ MutableInt f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaIfaceHal.this.lambda$getKeyMgmtCapabilities$13$SupplicantStaIfaceHal(this.f$1, this.f$2, supplicantStatus, i);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getKeyMgmtCapabilities");
            }
        } else {
            Log.e(TAG, "Method getKeyMgmtCapabilities is not supported in existing HAL");
        }
        return keyMgmtMask.value;
    }

    public /* synthetic */ void lambda$getKeyMgmtCapabilities$13$SupplicantStaIfaceHal(MutableBoolean status, MutableInt keyMgmtMask, SupplicantStatus statusInternal, int keyMgmtMaskInternal) {
        status.value = statusInternal.code == 0;
        if (status.value) {
            keyMgmtMask.value = keyMgmtMaskInternal;
        }
        checkStatusAndLogFailure(statusInternal, "getKeyMgmtCapabilities");
    }

    public int addDppPeerUri(String ifaceName, String uri) {
        MutableBoolean status = new MutableBoolean(false);
        MutableInt bootstrapId = new MutableInt(-1);
        if (!isV1_2()) {
            Log.e(TAG, "Method addDppPeerUri is not supported in existing HAL");
            return -1;
        }
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "addDppPeerUri");
        if (iface == null) {
            return -1;
        }
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
        if (staIfaceV12 == null) {
            Log.e(TAG, "addDppPeerUri: ISupplicantStaIface is null");
            return -1;
        }
        try {
            staIfaceV12.addDppPeerUri(uri, new ISupplicantStaIface.addDppPeerUriCallback(status, bootstrapId) {
                private final /* synthetic */ MutableBoolean f$1;
                private final /* synthetic */ MutableInt f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void onValues(SupplicantStatus supplicantStatus, int i) {
                    SupplicantStaIfaceHal.this.lambda$addDppPeerUri$14$SupplicantStaIfaceHal(this.f$1, this.f$2, supplicantStatus, i);
                }
            });
            return bootstrapId.value;
        } catch (RemoteException e) {
            handleRemoteException(e, "addDppPeerUri");
            return -1;
        }
    }

    public /* synthetic */ void lambda$addDppPeerUri$14$SupplicantStaIfaceHal(MutableBoolean status, MutableInt bootstrapId, SupplicantStatus statusInternal, int bootstrapIdInternal) {
        status.value = statusInternal.code == 0;
        if (status.value) {
            bootstrapId.value = bootstrapIdInternal;
        }
        checkStatusAndLogFailure(statusInternal, "addDppPeerUri");
    }

    public boolean removeDppUri(String ifaceName, int bootstrapId) {
        if (!isV1_2()) {
            Log.e(TAG, "Method removeDppUri is not supported in existing HAL");
            return false;
        }
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "removeDppUri");
        if (iface == null) {
            return false;
        }
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
        if (staIfaceV12 == null) {
            Log.e(TAG, "removeDppUri: ISupplicantStaIface is null");
            return false;
        }
        try {
            return checkStatusAndLogFailure(staIfaceV12.removeDppUri(bootstrapId), "removeDppUri");
        } catch (RemoteException e) {
            handleRemoteException(e, "removeDppUri");
            return false;
        }
    }

    public boolean stopDppInitiator(String ifaceName) {
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface iface;
        if (!isV1_2() || (iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "stopDppInitiator")) == null) {
            return false;
        }
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
        if (staIfaceV12 == null) {
            Log.e(TAG, "stopDppInitiator: ISupplicantStaIface is null");
            return false;
        }
        try {
            return checkStatusAndLogFailure(staIfaceV12.stopDppInitiator(), "stopDppInitiator");
        } catch (RemoteException e) {
            handleRemoteException(e, "stopDppInitiator");
            return false;
        }
    }

    public boolean startDppConfiguratorInitiator(String ifaceName, int peerBootstrapId, int ownBootstrapId, String ssid, String password, String psk, int netRole, int securityAkm) {
        if (!isV1_2()) {
            Log.e(TAG, "Method startDppConfiguratorInitiator is not supported in existing HAL");
            return false;
        }
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startDppConfiguratorInitiator");
        if (iface == null) {
            return false;
        }
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
        if (staIfaceV12 == null) {
            Log.e(TAG, "startDppConfiguratorInitiator: ISupplicantStaIface is null");
            return false;
        }
        try {
            return checkStatusAndLogFailure(staIfaceV12.startDppConfiguratorInitiator(peerBootstrapId, ownBootstrapId, ssid, password != null ? password : "", psk != null ? psk : "", netRole, securityAkm), "startDppConfiguratorInitiator");
        } catch (RemoteException e) {
            handleRemoteException(e, "startDppConfiguratorInitiator");
            return false;
        }
    }

    public boolean startDppEnrolleeInitiator(String ifaceName, int peerBootstrapId, int ownBootstrapId) {
        if (!isV1_2()) {
            Log.e(TAG, "Method startDppEnrolleeInitiator is not supported in existing HAL");
            return false;
        }
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface iface = checkSupplicantStaIfaceAndLogFailure(ifaceName, "startDppEnrolleeInitiator");
        if (iface == null) {
            return false;
        }
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaIface staIfaceV12 = getStaIfaceMockableV1_2(iface);
        if (staIfaceV12 == null) {
            Log.e(TAG, "startDppEnrolleeInitiator: ISupplicantStaIface is null");
            return false;
        }
        try {
            return checkStatusAndLogFailure(staIfaceV12.startDppEnrolleeInitiator(peerBootstrapId, ownBootstrapId), "startDppEnrolleeInitiator");
        } catch (RemoteException e) {
            handleRemoteException(e, "startDppEnrolleeInitiator");
            return false;
        }
    }

    public void registerDppCallback(WifiNative.DppEventCallback dppCallback) {
        this.mDppCallback = dppCallback;
    }

    public boolean isStatusCleared() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mISupplicant == null && this.mISehSupplicant == null;
        }
        return z;
    }

    public boolean setWifiApRttFeatureBandwidth(String ifaceName, int bandwidth) {
        String cmd = "NAN_RANGING_SET_BW " + bandwidth;
        try {
            Log.d(TAG, "setWifiApRttFeature:" + cmd);
            return setExtendedCommand(ifaceName, cmd);
        } catch (Exception e) {
            Log.e(TAG, "General exception " + cmd, e);
            return false;
        }
    }
}
