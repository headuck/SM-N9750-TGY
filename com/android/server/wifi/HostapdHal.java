package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.hostapd.V1_0.HostapdStatus;
import android.hardware.wifi.hostapd.V1_0.IHostapd;
import android.hardware.wifi.hostapd.V1_1.IHostapd;
import android.hardware.wifi.hostapd.V1_1.IHostapdCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.HostapdHal;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.NativeUtil;
import com.samsung.android.server.wifi.softap.SemWifiApMonitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.ThreadSafe;
import vendor.samsung.hardware.wifi.hostapd.V2_0.ISehHostapdCallback;
import vendor.samsung.hardware.wifi.hostapd.V2_1.ISehHostapd;

@ThreadSafe
public class HostapdHal {
    @VisibleForTesting
    public static final String HAL_INSTANCE_NAME = "default";
    private static final String TAG = "HostapdHal";
    private final int SamsungHotspotVSIE = 128;
    private final String SamsungOUI = "001632";
    private final List<IHostapd.AcsChannelRange> mAcsChannelRanges;
    private Context mContext;
    private WifiNative.HostapdDeathEventHandler mDeathEventHandler;
    /* access modifiers changed from: private */
    public long mDeathRecipientCookie = 0;
    private final boolean mEnableAcs;
    private final boolean mEnableIeee80211AC;
    /* access modifiers changed from: private */
    public final Handler mEventHandler;
    private HostapdDeathRecipient mHostapdDeathRecipient;
    private android.hardware.wifi.hostapd.V1_0.IHostapd mIHostapd;
    private ISehHostapd mISehHostapd;
    /* access modifiers changed from: private */
    public IServiceManager mIServiceManager = null;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private List<String> mMHSDumpLogs = new ArrayList();
    /* access modifiers changed from: private */
    public SemWifiApMonitor mSemWifiApMonitor = null;
    private ServiceManagerDeathRecipient mServiceManagerDeathRecipient;
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            synchronized (HostapdHal.this.mLock) {
                if (HostapdHal.this.mVerboseLoggingEnabled) {
                    Log.i(HostapdHal.TAG, "IServiceNotification.onRegistration for: " + fqName + ", " + name + " preexisting=" + preexisting);
                }
                HostapdHal hostapdHal = HostapdHal.this;
                hostapdHal.addMHSDumpLog("HostapdHal  IServiceNotification.onRegistration for: " + fqName + ", " + name + " preexisting=" + preexisting);
                if (!HostapdHal.this.initHostapdService()) {
                    Log.e(HostapdHal.TAG, "initalizing IHostapd failed.");
                    HostapdHal.this.addMHSDumpLog("HostapdHal  initalizing IHostapd failed.");
                    HostapdHal.this.hostapdServiceDiedHandler(HostapdHal.this.mDeathRecipientCookie);
                } else {
                    HostapdHal.this.addMHSDumpLog("HostapdHal  Completed initialization of IHostapd.");
                    Log.i(HostapdHal.TAG, "Completed initialization of IHostapd.");
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public HashMap<String, WifiNative.SoftApListener> mSoftApListeners = new HashMap<>();
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = false;
    private WifiManager mWifiManager;

    public void addMHSDumpLog(String log) {
        StringBuffer value = new StringBuffer();
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (this.mMHSDumpLogs.size() > 100) {
            this.mMHSDumpLogs.remove(0);
        }
        this.mMHSDumpLogs.add(value.toString());
    }

    private class ServiceManagerDeathRecipient implements IHwBinder.DeathRecipient {
        private ServiceManagerDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            HostapdHal.this.mEventHandler.post(new Runnable(cookie) {
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HostapdHal.ServiceManagerDeathRecipient.this.lambda$serviceDied$0$HostapdHal$ServiceManagerDeathRecipient(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$serviceDied$0$HostapdHal$ServiceManagerDeathRecipient(long cookie) {
            synchronized (HostapdHal.this.mLock) {
                Log.w(HostapdHal.TAG, "IServiceManager died: cookie=" + cookie);
                HostapdHal.this.hostapdServiceDiedHandler(HostapdHal.this.mDeathRecipientCookie);
                IServiceManager unused = HostapdHal.this.mIServiceManager = null;
            }
        }
    }

    private class HostapdDeathRecipient implements IHwBinder.DeathRecipient {
        private HostapdDeathRecipient() {
        }

        public void serviceDied(long cookie) {
            HostapdHal.this.mEventHandler.post(new Runnable(cookie) {
                private final /* synthetic */ long f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HostapdHal.HostapdDeathRecipient.this.lambda$serviceDied$0$HostapdHal$HostapdDeathRecipient(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$serviceDied$0$HostapdHal$HostapdDeathRecipient(long cookie) {
            synchronized (HostapdHal.this.mLock) {
                Log.w(HostapdHal.TAG, "IHostapd/IHostapd died: cookie=" + cookie);
                HostapdHal.this.hostapdServiceDiedHandler(cookie);
            }
        }
    }

    public HostapdHal(Context context, Looper looper) {
        this.mEventHandler = new Handler(looper);
        this.mContext = context;
        this.mEnableAcs = false;
        if ("in_house".equals("jdm")) {
            this.mEnableIeee80211AC = true;
        } else {
            this.mEnableIeee80211AC = false;
        }
        this.mAcsChannelRanges = toAcsChannelRanges(context.getResources().getString(17040007));
        this.mServiceManagerDeathRecipient = new ServiceManagerDeathRecipient();
        this.mHostapdDeathRecipient = new HostapdDeathRecipient();
        this.mSemWifiApMonitor = WifiInjector.getInstance().getWifiApMonitor();
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    private boolean isV1_1() {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mIServiceManager == null) {
                Log.e(TAG, "isV1_1: called but mServiceManager is null!?");
                return false;
            }
            try {
                if (this.mIServiceManager.getTransport(IHostapd.kInterfaceName, "default") != 0) {
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

    private boolean isSamsungV2_1() {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mIServiceManager == null) {
                Log.e(TAG, "isV1_1: called but mServiceManager is null!?");
                return false;
            }
            try {
                if (this.mIServiceManager.getTransport(ISehHostapd.kInterfaceName, "default") != 0) {
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
                hostapdServiceDiedHandler(this.mDeathRecipientCookie);
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering IHostapd service ready callback.");
            }
            this.mIHostapd = null;
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
                    if (this.mIServiceManager.registerForNotifications(android.hardware.wifi.hostapd.V1_0.IHostapd.kInterfaceName, "", this.mServiceNotificationCallback)) {
                        return true;
                    }
                    Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.hostapd@1.0::IHostapd");
                    this.mIServiceManager = null;
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for IHostapd service: " + e);
                hostapdServiceDiedHandler(this.mDeathRecipientCookie);
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    private boolean linkToHostapdDeath() {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                return false;
            }
            try {
                android.hardware.wifi.hostapd.V1_0.IHostapd iHostapd = this.mIHostapd;
                HostapdDeathRecipient hostapdDeathRecipient = this.mHostapdDeathRecipient;
                long j = this.mDeathRecipientCookie + 1;
                this.mDeathRecipientCookie = j;
                if (iHostapd.linkToDeath(hostapdDeathRecipient, j)) {
                    return true;
                }
                Log.wtf(TAG, "Error on linkToDeath on IHostapd");
                hostapdServiceDiedHandler(this.mDeathRecipientCookie);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapd.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean registerCallback(IHostapdCallback callback) {
        synchronized (this.mLock) {
            try {
                IHostapd iHostapdV1_1 = getHostapdMockableV1_1();
                if (iHostapdV1_1 == null) {
                    return false;
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iHostapdV1_1.registerCallback(callback), "registerCallback_1_1");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback_1_1");
                return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean initHostapdService() {
        synchronized (this.mLock) {
            try {
                this.mIHostapd = getHostapdMockable();
                try {
                    if (this.mIHostapd == null) {
                        Log.e(TAG, "Got null IHostapd service. Stopping hostapd HIDL startup");
                        return false;
                    } else if (!linkToHostapdDeath()) {
                        this.mIHostapd = null;
                        return false;
                    } else {
                        addMHSDumpLog("HostapdHal  getting mISehHostapd service ");
                        this.mISehHostapd = getSehHostapdMockable();
                        if (this.mISehHostapd == null) {
                            addMHSDumpLog("HostapdHal  Got null mISehHostapd service. Stopping hostapd HIDL startup");
                            Log.e(TAG, "Got null mISehHostapd service. Stopping hostapd HIDL startup");
                            this.mIHostapd = null;
                            return false;
                        }
                        Log.i(TAG, "IsehHostapd. Initialization incomplete.");
                        return true;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "mISehHostapd.getService exception: " + e);
                    return false;
                } catch (NoSuchElementException e2) {
                    Log.e(TAG, "mISehHostapd.getService exception: " + e2);
                    return false;
                } catch (Throwable th) {
                    throw th;
                }
            } catch (RemoteException e3) {
                Log.e(TAG, "IHostapd.getService exception: " + e3);
                return false;
            } catch (NoSuchElementException e4) {
                Log.e(TAG, "IHostapd.getService exception: " + e4);
                return false;
            }
        }
    }

    public boolean addAccessPoint(String ifaceName, WifiConfiguration config, WifiNative.SoftApListener listener) {
        String vendorIE;
        int i;
        String str = ifaceName;
        WifiConfiguration wifiConfiguration = config;
        synchronized (this.mLock) {
            try {
                IHostapd.IfaceParams ifaceParams = new IHostapd.IfaceParams();
                File file = new File("/data/misc/wifi_hostapd/hostapd.accept");
                ISehHostapd.SehParams mSehParams = new ISehHostapd.SehParams();
                boolean isGuestMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_multipassword_enabled", 0) == 1;
                if (isGuestMode) {
                    mSehParams.guestPskPassphrase = wifiConfiguration.guestPreSharedKey;
                }
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_11ax_mode_checked", 0) == 1) {
                    mSehParams.enable11ax = true;
                }
                mSehParams.v2_0.apIsolate = wifiConfiguration.apIsolate == 1;
                mSehParams.v2_0.maxNumSta = wifiConfiguration.maxclient;
                Log.d(TAG, "config.maxclient" + wifiConfiguration.maxclient);
                if (wifiConfiguration.vendorIE == 0) {
                    vendorIE = "DD05001632" + Integer.toHexString(128) + "00";
                } else if (wifiConfiguration.vendorIE <= 0 || wifiConfiguration.vendorIE >= 255) {
                    vendorIE = "";
                } else {
                    vendorIE = "DD05001632" + Integer.toHexString(wifiConfiguration.vendorIE) + "00";
                }
                if (!isShipBinary()) {
                    Log.i(TAG, "Add Vendor specific IE DD040000F0FE");
                    vendorIE = vendorIE + "DD040000F0FE";
                }
                String vendorIE2 = vendorIE + "DD080050F21102000000";
                mSehParams.v2_0.vendorIe = vendorIE2;
                mSehParams.v2_0.pmf = wifiConfiguration.requirePMF;
                mSehParams.v2_0.macAddressAcl = wifiConfiguration.macaddrAcl;
                Log.w(TAG, "isGuestMode:" + isGuestMode + ":vendorIE = " + vendorIE2 + " apIsolate " + mSehParams.v2_0.apIsolate + " pmf " + mSehParams.v2_0.pmf + " macAddressAcl  " + mSehParams.v2_0.macAddressAcl + " maxNumSta " + mSehParams.v2_0.maxNumSta + ",hostapd.accept exist:" + file.exists());
                ifaceParams.ifaceName = str;
                ifaceParams.hwModeParams.enable80211N = true;
                ifaceParams.hwModeParams.enable80211AC = this.mEnableIeee80211AC;
                try {
                    ifaceParams.channelParams.band = getBand(config);
                    if (this.mEnableAcs) {
                        ifaceParams.channelParams.enableAcs = true;
                        ifaceParams.channelParams.acsShouldExcludeDfs = true;
                    } else {
                        if (ifaceParams.channelParams.band == 2) {
                            Log.d(TAG, "ACS is not supported on this device, using 2.4 GHz band.");
                            ifaceParams.channelParams.band = 0;
                        }
                        ifaceParams.channelParams.enableAcs = false;
                        ifaceParams.channelParams.channel = wifiConfiguration.apChannel;
                    }
                    IHostapd.NetworkParams nwParams = new IHostapd.NetworkParams();
                    nwParams.ssid.addAll(NativeUtil.stringToByteArrayList(wifiConfiguration.SSID));
                    nwParams.isHidden = wifiConfiguration.hiddenSSID;
                    mSehParams.encryptionType = getEncryptionType(config);
                    String str2 = SystemProperties.get("mhs.wpa");
                    if (str2 != null && !str2.equals("")) {
                        int num = Integer.parseInt(str2);
                        Log.d(TAG, "debug WPA:" + num);
                        if (num == 0) {
                            mSehParams.encryptionType = 0;
                        }
                        if (num == 1) {
                            i = 2;
                            mSehParams.encryptionType = 2;
                        } else {
                            i = 2;
                        }
                        if (num == i) {
                            mSehParams.encryptionType = 4;
                        }
                        if (num == 3) {
                            mSehParams.encryptionType = 3;
                        }
                    }
                    nwParams.pskPassphrase = wifiConfiguration.preSharedKey != null ? wifiConfiguration.preSharedKey : "";
                    if (!checkHostapdAndLogFailure("addAccessPoint")) {
                        return false;
                    }
                    HostapdStatus status = null;
                    try {
                        if (file.exists()) {
                            readWhiteListFileToSendHostapd();
                        }
                        if (isV1_1()) {
                            IHostapd.IfaceParams ifaceParams1_1 = new IHostapd.IfaceParams();
                            ifaceParams1_1.V1_0 = ifaceParams;
                            if (this.mEnableAcs) {
                                ifaceParams1_1.channelParams.acsChannelRanges.addAll(this.mAcsChannelRanges);
                            }
                            if (isSamsungV2_1()) {
                                status = this.mISehHostapd.sehAddAccessPoint_2_1(ifaceParams1_1, nwParams, mSehParams);
                                Log.d(TAG, "sehAddAccessPoint2_1  with code=" + status.code);
                                addMHSDumpLog("HostapdHal  sehAddAccessPoint2_1  with code=" + status.code);
                            } else {
                                status = this.mISehHostapd.sehAddAccessPoint(ifaceParams1_1, nwParams, mSehParams.v2_0);
                                Log.d(TAG, "sehAddAccessPoint  with code=" + status.code);
                                addMHSDumpLog("HostapdHal  sehAddAccessPoint  with code=" + status.code);
                            }
                            if (status.code == 4) {
                                this.mIHostapd.removeAccessPoint(str);
                                if (isSamsungV2_1()) {
                                    HostapdStatus status2 = this.mISehHostapd.sehAddAccessPoint_2_1(ifaceParams1_1, nwParams, mSehParams);
                                    Log.d(TAG, "sehAddAccessPoint2_1  with code=" + status2.code);
                                    addMHSDumpLog("HostapdHal  sehAddAccessPoint2_1  with code=" + status2.code);
                                    status = status2;
                                } else {
                                    HostapdStatus status3 = this.mISehHostapd.sehAddAccessPoint(ifaceParams1_1, nwParams, mSehParams.v2_0);
                                    Log.d(TAG, "sehAddAccessPoint  with code=" + status3.code);
                                    addMHSDumpLog("HostapdHal  sehAddAccessPoint  with code=" + status3.code);
                                    status = status3;
                                }
                            }
                            if (!sehRegisterCallback(new SehHostapdCallback(str))) {
                                Log.e(TAG, "Callback failed. Initialization sehRegisterCallback.");
                                addMHSDumpLog("HostapdHal  Callback failed. Initialization sehRegisterCallback.");
                                return false;
                            }
                        }
                        if (!checkStatusAndLogFailure(status, "addAccessPoint")) {
                            return false;
                        }
                        try {
                            this.mSoftApListeners.put(str, listener);
                            return true;
                        } catch (NullPointerException e) {
                            e = e;
                            Log.e(TAG, "IHostapd.NullPointerExceptionaddAccessPoint failed with exception", e);
                            return false;
                        } catch (RemoteException e2) {
                            e = e2;
                            handleRemoteException(e, "addAccessPoint");
                            return false;
                        } catch (Throwable th) {
                            th = th;
                            Throwable th2 = th;
                            throw th2;
                        }
                    } catch (NullPointerException e3) {
                        e = e3;
                        WifiNative.SoftApListener softApListener = listener;
                        Log.e(TAG, "IHostapd.NullPointerExceptionaddAccessPoint failed with exception", e);
                        return false;
                    } catch (RemoteException e4) {
                        e = e4;
                        WifiNative.SoftApListener softApListener2 = listener;
                        handleRemoteException(e, "addAccessPoint");
                        return false;
                    }
                } catch (IllegalArgumentException e5) {
                    WifiNative.SoftApListener softApListener3 = listener;
                    IllegalArgumentException illegalArgumentException = e5;
                    Log.e(TAG, "Unrecognized apBand " + wifiConfiguration.apBand);
                    return false;
                }
            } catch (Throwable th3) {
                th = th3;
                WifiNative.SoftApListener softApListener4 = listener;
                Throwable th22 = th;
                throw th22;
            }
        }
    }

    public boolean removeAccessPoint(String ifaceName) {
        synchronized (this.mLock) {
            if (!checkHostapdAndLogFailure("removeAccessPoint")) {
                return false;
            }
            try {
                if (!checkStatusAndLogFailure(this.mIHostapd.removeAccessPoint(ifaceName), "removeAccessPoint")) {
                    return false;
                }
                this.mSoftApListeners.remove(ifaceName);
                return true;
            } catch (RemoteException e) {
                handleRemoteException(e, "removeAccessPoint");
                return false;
            }
        }
    }

    public boolean registerDeathHandler(WifiNative.HostapdDeathEventHandler handler) {
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
            this.mIHostapd = null;
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x001f, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void hostapdServiceDiedHandler(long r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mLock
            monitor-enter(r0)
            long r1 = r3.mDeathRecipientCookie     // Catch:{ all -> 0x0020 }
            int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
            if (r1 == 0) goto L_0x0012
            java.lang.String r1 = "HostapdHal"
            java.lang.String r2 = "Ignoring stale death recipient notification"
            android.util.Log.i(r1, r2)     // Catch:{ all -> 0x0020 }
            monitor-exit(r0)     // Catch:{ all -> 0x0020 }
            return
        L_0x0012:
            r3.clearState()     // Catch:{ all -> 0x0020 }
            com.android.server.wifi.WifiNative$HostapdDeathEventHandler r1 = r3.mDeathEventHandler     // Catch:{ all -> 0x0020 }
            if (r1 == 0) goto L_0x001e
            com.android.server.wifi.WifiNative$HostapdDeathEventHandler r1 = r3.mDeathEventHandler     // Catch:{ all -> 0x0020 }
            r1.onDeath()     // Catch:{ all -> 0x0020 }
        L_0x001e:
            monitor-exit(r0)     // Catch:{ all -> 0x0020 }
            return
        L_0x0020:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0020 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HostapdHal.hostapdServiceDiedHandler(long):void");
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
            z = this.mIHostapd != null;
        }
        return z;
    }

    public boolean startDaemon() {
        synchronized (this.mLock) {
            try {
                addMHSDumpLog("HostapdHal  startDaemon");
                getHostapdMockable();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to start hostapd: " + e);
                hostapdServiceDiedHandler(this.mDeathRecipientCookie);
                return false;
            } catch (NoSuchElementException e2) {
                Log.d(TAG, "Successfully triggered start of hostapd using HIDL");
            }
        }
        return true;
    }

    public void terminate() {
        synchronized (this.mLock) {
            if (checkHostapdAndLogFailure("terminate")) {
                try {
                    this.mIHostapd.terminate();
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public android.hardware.wifi.hostapd.V1_0.IHostapd getHostapdMockable() throws RemoteException {
        android.hardware.wifi.hostapd.V1_0.IHostapd service;
        synchronized (this.mLock) {
            service = android.hardware.wifi.hostapd.V1_0.IHostapd.getService();
        }
        return service;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public android.hardware.wifi.hostapd.V1_1.IHostapd getHostapdMockableV1_1() throws RemoteException {
        android.hardware.wifi.hostapd.V1_1.IHostapd castFrom;
        synchronized (this.mLock) {
            try {
                castFrom = android.hardware.wifi.hostapd.V1_1.IHostapd.castFrom(this.mIHostapd);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get IHostapd", e);
                return null;
            } catch (Throwable th) {
                throw th;
            }
        }
        return castFrom;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public ISehHostapd getSehHostapdMockable() throws RemoteException {
        ISehHostapd castFrom;
        synchronized (this.mLock) {
            try {
                castFrom = ISehHostapd.castFrom(getHostapdMockableV1_1());
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Failed to get ISehHostapd", e);
                return null;
            } catch (Throwable th) {
                throw th;
            }
        }
        return castFrom;
    }

    private static int getEncryptionType(WifiConfiguration localConfig) {
        int authType = localConfig.getAuthType();
        if (authType == 0) {
            return 0;
        }
        if (authType == 1) {
            return 1;
        }
        if (authType == 4) {
            return 2;
        }
        if (authType == 25) {
            return 4;
        }
        if (authType != 26) {
            return 0;
        }
        return 3;
    }

    private static int getBand(WifiConfiguration localConfig) {
        int bandType;
        int i = localConfig.apBand;
        if (i == -1) {
            bandType = 2;
        } else if (i == 0) {
            bandType = 0;
        } else if (i == 1) {
            bandType = 1;
        } else {
            throw new IllegalArgumentException();
        }
        if (localConfig.apChannel == 149) {
            return 1;
        }
        return bandType;
    }

    private List<IHostapd.AcsChannelRange> toAcsChannelRanges(String channelListStr) {
        ArrayList<IHostapd.AcsChannelRange> acsChannelRanges = new ArrayList<>();
        for (String channelRange : channelListStr.split(",")) {
            IHostapd.AcsChannelRange acsChannelRange = new IHostapd.AcsChannelRange();
            try {
                if (channelRange.contains("-")) {
                    String[] channels = channelRange.split("-");
                    if (channels.length != 2) {
                        Log.e(TAG, "Unrecognized channel range, length is " + channels.length);
                    } else {
                        int start = Integer.parseInt(channels[0]);
                        int end = Integer.parseInt(channels[1]);
                        if (start > end) {
                            Log.e(TAG, "Invalid channel range, from " + start + " to " + end);
                        } else {
                            acsChannelRange.start = start;
                            acsChannelRange.end = end;
                        }
                    }
                } else {
                    acsChannelRange.start = Integer.parseInt(channelRange);
                    acsChannelRange.end = acsChannelRange.start;
                }
                acsChannelRanges.add(acsChannelRange);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Malformed channel value detected: " + e);
            }
        }
        return acsChannelRanges;
    }

    private boolean checkHostapdAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mIHostapd != null) {
                return true;
            }
            Log.e(TAG, "Can't call " + methodStr + ", IHostapd is null");
            return false;
        }
    }

    private boolean checkStatusAndLogFailure(HostapdStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "IHostapd." + methodStr + " failed: " + status.code + ", " + status.debugMessage);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "IHostapd." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            hostapdServiceDiedHandler(this.mDeathRecipientCookie);
            Log.e(TAG, "IHostapd." + methodStr + " failed with exception", e);
        }
    }

    private class HostapdCallback extends IHostapdCallback.Stub {
        private HostapdCallback() {
        }

        public void onFailure(String ifaceName) {
            Log.w(HostapdHal.TAG, "Failure on iface " + ifaceName);
            WifiNative.SoftApListener listener = (WifiNative.SoftApListener) HostapdHal.this.mSoftApListeners.get(ifaceName);
            if (listener != null) {
                listener.onFailure();
            }
        }
    }

    public class SehHostapdCallback extends ISehHostapdCallback.Stub {
        public String mInterface = null;

        SehHostapdCallback(String iface) {
            this.mInterface = iface;
        }

        public void onFailure(String ifaceName) {
            Log.w(HostapdHal.TAG, "Failure on iface " + ifaceName);
            WifiNative.SoftApListener listener = (WifiNative.SoftApListener) HostapdHal.this.mSoftApListeners.get(ifaceName);
            if (listener != null) {
                listener.onFailure();
            }
        }

        public void sehHostapdCallbackEvent(String str) {
            Log.w(HostapdHal.TAG, "hostapdCallbackEvent=  " + str);
            if (this.mInterface != null) {
                HostapdHal.this.mSemWifiApMonitor.hostapdCallbackEvent(this.mInterface, str);
            }
        }
    }

    public void readWhiteListFileToSendHostapd() {
        try {
            this.mISehHostapd.sehResetWhiteList();
        } catch (Exception e) {
            Log.i(TAG, "Exception" + e);
        }
        Log.d(TAG, "readWhiteListFileToSendHostapd");
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader("/data/misc/wifi_hostapd/hostapd.accept"), 64);
            while (true) {
                String readLine = buf.readLine();
                String bufReadLine = readLine;
                if (readLine == null) {
                    try {
                        buf.close();
                        return;
                    } catch (IOException e2) {
                        Log.i(TAG, "IOException" + e2);
                        return;
                    }
                } else if (bufReadLine.startsWith("#")) {
                    try {
                        this.mISehHostapd.sehAddWhiteList(bufReadLine.substring(1), buf.readLine());
                    } catch (Exception e3) {
                        Log.i(TAG, "Exception" + e3);
                    }
                }
            }
        } catch (IOException e4) {
            Log.i(TAG, "IOException" + e4);
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e5) {
                    Log.i(TAG, "IOException" + e5);
                }
            }
        } catch (Throwable th) {
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e6) {
                    Log.i(TAG, "IOException" + e6);
                }
            }
            throw th;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0068, code lost:
        if (r2 == 13) goto L_0x006a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.String sendHostapdCommand(java.lang.String r7) {
        /*
            r6 = this;
            java.lang.String r0 = "HostapdHal"
            r1 = 0
            vendor.samsung.hardware.wifi.hostapd.V2_1.ISehHostapd r2 = r6.mISehHostapd
            r3 = 0
            if (r2 == 0) goto L_0x0097
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x0077 }
            r2.<init>()     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r4 = "hostapd command: "
            r2.append(r4)     // Catch:{ RemoteException -> 0x0077 }
            r2.append(r7)     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r2 = r2.toString()     // Catch:{ RemoteException -> 0x0077 }
            android.util.Log.d(r0, r2)     // Catch:{ RemoteException -> 0x0077 }
            android.content.Context r2 = r6.mContext     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r4 = "wifi"
            java.lang.Object r2 = r2.getSystemService(r4)     // Catch:{ RemoteException -> 0x0077 }
            android.net.wifi.WifiManager r2 = (android.net.wifi.WifiManager) r2     // Catch:{ RemoteException -> 0x0077 }
            r6.mWifiManager = r2     // Catch:{ RemoteException -> 0x0077 }
            android.net.wifi.WifiManager r2 = r6.mWifiManager     // Catch:{ RemoteException -> 0x0077 }
            int r2 = r2.getWifiApState()     // Catch:{ RemoteException -> 0x0077 }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x0077 }
            r4.<init>()     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r5 = "wifiApState "
            r4.append(r5)     // Catch:{ RemoteException -> 0x0077 }
            r4.append(r2)     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r4 = r4.toString()     // Catch:{ RemoteException -> 0x0077 }
            android.util.Log.d(r0, r4)     // Catch:{ RemoteException -> 0x0077 }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RemoteException -> 0x0077 }
            r4.<init>()     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r5 = "HostapdHal  sendHostapdCommand:"
            r4.append(r5)     // Catch:{ RemoteException -> 0x0077 }
            r4.append(r7)     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r5 = ",wifiApState:"
            r4.append(r5)     // Catch:{ RemoteException -> 0x0077 }
            r4.append(r2)     // Catch:{ RemoteException -> 0x0077 }
            java.lang.String r4 = r4.toString()     // Catch:{ RemoteException -> 0x0077 }
            r6.addMHSDumpLog(r4)     // Catch:{ RemoteException -> 0x0077 }
            android.net.wifi.WifiManager r4 = r6.mWifiManager     // Catch:{ RemoteException -> 0x0077 }
            r4 = 12
            if (r2 == r4) goto L_0x006a
            android.net.wifi.WifiManager r4 = r6.mWifiManager     // Catch:{ RemoteException -> 0x0077 }
            r4 = 13
            if (r2 != r4) goto L_0x0076
        L_0x006a:
            java.lang.String r4 = "hotspot enabling or enabled state"
            android.util.Log.d(r0, r4)     // Catch:{ RemoteException -> 0x0077 }
            vendor.samsung.hardware.wifi.hostapd.V2_1.ISehHostapd r4 = r6.mISehHostapd     // Catch:{ RemoteException -> 0x0077 }
            android.hardware.wifi.hostapd.V1_0.HostapdStatus r0 = r4.sehSendCommand(r7)     // Catch:{ RemoteException -> 0x0077 }
            r1 = r0
        L_0x0076:
            goto L_0x008c
        L_0x0077:
            r2 = move-exception
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "mIHostapd exception: "
            r4.append(r5)
            r4.append(r2)
            java.lang.String r4 = r4.toString()
            android.util.Log.e(r0, r4)
        L_0x008c:
            if (r1 == 0) goto L_0x0096
            int r0 = r1.code
            if (r0 == 0) goto L_0x0093
            goto L_0x0096
        L_0x0093:
            java.lang.String r0 = r1.debugMessage
            return r0
        L_0x0096:
            return r3
        L_0x0097:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.HostapdHal.sendHostapdCommand(java.lang.String):java.lang.String");
    }

    public boolean sehRegisterCallback(ISehHostapdCallback callback) {
        HostapdStatus mHostapdStatus = null;
        Log.d(TAG, "sehRegisterCallback ");
        ISehHostapd iSehHostapd = this.mISehHostapd;
        if (iSehHostapd == null) {
            Log.e(TAG, "mISehHostapd is null ");
            return false;
        }
        try {
            mHostapdStatus = iSehHostapd.sehRegisterCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "mIHostapd exception: " + e);
        }
        if (mHostapdStatus == null || mHostapdStatus.code != 0) {
            Log.e(TAG, "sehRegisterCallback failed ");
            return false;
        }
        Log.d(TAG, "sehRegisterCallback successful ");
        return true;
    }

    private boolean isShipBinary() {
        boolean result = isSepDevice() && !Debug.semIsProductDev();
        Log.i(TAG, "isShipBinary :" + result);
        return result;
    }

    private boolean isSepDevice() {
        int SEM_INT = 0;
        try {
            SEM_INT = Build.VERSION.class.getField("SEM_INT").getInt(Build.VERSION.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
        }
        return SEM_INT != 0;
    }

    public String getDumpLogs() {
        StringBuffer retValue = new StringBuffer();
        retValue.append("--HostapdHAL \n");
        retValue.append(this.mMHSDumpLogs.toString());
        return retValue.toString();
    }
}
