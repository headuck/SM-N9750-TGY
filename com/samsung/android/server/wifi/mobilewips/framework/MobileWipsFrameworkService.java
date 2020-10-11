package com.samsung.android.server.wifi.mobilewips.framework;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.SemHqmManager;
import android.provider.Settings;
import android.util.Log;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.iwc.IWCEventManager;
import com.android.server.wifi.util.NativeUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import com.samsung.android.server.wifi.mobilewips.client.IMobileWipsService;
import com.samsung.android.server.wifi.mobilewips.framework.IMobileWipsFramework;
import com.samsung.android.server.wifi.mobilewips.framework.IMobileWipsPacketSender;
import com.sec.android.app.CscFeatureTagWifi;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork;

public class MobileWipsFrameworkService {
    private static final boolean DBG = Debug.semIsProductDev();
    public static final boolean ENABLE_UNIFIED_HQM_SERVER = true;
    private static final int EVENT_BCN_ABORT = 1004;
    private static final int EVENT_BCN_INTERVAL = 1003;
    private static final int EVENT_SEND_SAVED_DATA = 1006;
    private static final int EVENT_START_SERVICE = 1001;
    private static final int EVENT_STOP_SERVICE = 1002;
    private static final int EVENT_WIPS_MONITOR = 1005;
    private static final int MAX_RETRY_COUNT = 2;
    private static final int MWIPS_FRAMEWORK_VERSION = 1;
    private static final String MWIPS_PACKAGE = "com.samsung.android.server.wifi.mobilewips.client";
    private static final String MWIPS_SERVICE = "com.samsung.android.server.wifi.mobilewips.client.MobileWipsService";
    private static final int REMOVE_NETWORK_DELAY = 3000;
    private static final String SEEN_TIME = "seen_time";
    private static final int SERVICE_START_DELAY = 3000;
    private static final int SERVICE_START_DELAY_BOOT_COMPLTED = 5000;
    private static final String TAG = "MobileWipsFrameworkService";
    private static final int WIPS_GOING_OFF = 2;
    private static final int WIPS_GOING_ON = 3;
    private static final int WIPS_MONITOR_INTERVAL = 3000;
    private static final int WIPS_OFF = 0;
    private static final int WIPS_ON = 1;
    private static MobileWipsFrameworkService mInstance;
    private static boolean mSpecialNetworkDeleted = false;
    /* access modifiers changed from: private */
    public boolean mBootCompleted = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                Log.d(MobileWipsFrameworkService.TAG, "received ACTION_BOOT_COMPLETED");
                boolean unused = MobileWipsFrameworkService.this.mBootCompleted = true;
                MobileWipsFrameworkService.this.turnOnWips(5000);
            } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("wifi_state", -1);
                boolean unused2 = MobileWipsFrameworkService.this.mIsConnected = false;
                if (state == 1) {
                    Log.i(MobileWipsFrameworkService.TAG, "Wifi disabled");
                    MobileWipsFrameworkService.this.checkSpecialNetwork(false);
                    MobileWipsFrameworkService.this.turnOffWips();
                } else if (state == 3) {
                    Log.i(MobileWipsFrameworkService.TAG, "Wifi enabled, try to start wips service");
                    MobileWipsFrameworkService.this.checkSpecialNetwork(true);
                    MobileWipsFrameworkService.this.turnOnWips();
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiverPackage = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            String action = intent.getAction();
            if (uri != null && MobileWipsFrameworkService.MWIPS_PACKAGE.equals(uri.getSchemeSpecificPart())) {
                Log.i(MobileWipsFrameworkService.TAG, "Intent.ACTION_PACKAGE " + action);
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -810471698) {
                    if (hashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 0;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                    c = 1;
                }
                if (c != 0 && c == 1) {
                    MobileWipsFrameworkService.this.turnOnWips();
                    int unused = MobileWipsFrameworkService.this.mRetrycount = 0;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public ClientModeImpl mClientModeImpl = null;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(MobileWipsFrameworkService.TAG, "MobileWipsFrameworkService service connected");
            MobileWipsFrameworkService mobileWipsFrameworkService = MobileWipsFrameworkService.this;
            WifiManager unused = mobileWipsFrameworkService.mWifiManager = (WifiManager) mobileWipsFrameworkService.mContext.getSystemService("wifi");
            MobileWipsFrameworkService mobileWipsFrameworkService2 = MobileWipsFrameworkService.this;
            ConnectivityManager unused2 = mobileWipsFrameworkService2.mConnectivityManager = (ConnectivityManager) mobileWipsFrameworkService2.mContext.getSystemService("connectivity");
            if (MobileWipsFrameworkService.this.mService == null) {
                IMobileWipsService unused3 = MobileWipsFrameworkService.this.mService = IMobileWipsService.Stub.asInterface(service);
            }
            if (MobileWipsFrameworkService.this.mService != null) {
                try {
                    MobileWipsFrameworkService.this.mService.registerCallback(MobileWipsFrameworkService.this.mWipsFrameworkApi);
                    MobileWipsFrameworkService.this.mService.registerPacketSender(MobileWipsFrameworkService.this.mPacketSenderFrameworkApi);
                    MobileWipsFrameworkService.this.mMobileWipsWifiScanner.start();
                    MobileWipsFrameworkService.this.getHandler().sendEmptyMessage(15);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (RemoteException e2) {
                    e2.printStackTrace();
                } catch (NullPointerException e3) {
                    e3.printStackTrace();
                    Log.e(MobileWipsFrameworkService.TAG, "onServiceConnected Exception!!" + e3);
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(MobileWipsFrameworkService.TAG, "MobileWipsFrameworkService service disconnected");
            if (MobileWipsFrameworkService.this.mService != null) {
                IMobileWipsService unused = MobileWipsFrameworkService.this.mService = null;
            }
            MobileWipsFrameworkService.this.mMobileWipsWifiScanner.stop();
            if (MobileWipsFrameworkService.this.mRetrycount >= 2) {
                Log.e(MobileWipsFrameworkService.TAG, "Retry stop");
                return;
            }
            MobileWipsFrameworkService.this.turnOnWips(IWCEventManager.wifiOFFPending_MS);
            MobileWipsFrameworkService.access$2008(MobileWipsFrameworkService.this);
        }
    };
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager = null;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public MobileWipsDnsRequester mDnsRequester = null;
    /* access modifiers changed from: private */
    public boolean mIsConnected = false;
    /* access modifiers changed from: private */
    public MobileWipsNetdEvent mMobileWipsNetdEvent = null;
    /* access modifiers changed from: private */
    public MobileWipsWifiScanner mMobileWipsWifiScanner = null;
    /* access modifiers changed from: private */
    public MobileWipsPacketSender mPacketSender = null;
    IMobileWipsPacketSender mPacketSenderFrameworkApi = new IMobileWipsPacketSender.Stub() {
        public List<String> sendArp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return null;
            }
            return MobileWipsFrameworkService.this.mPacketSender.sendArp(linkProperties, timeoutMillis, gateway, myAddr, myMac);
        }

        public List<String> sendArpToSniffing(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return null;
            }
            return MobileWipsFrameworkService.this.mPacketSender.sendArpToSniffing(linkProperties, timeoutMillis, gateway, myAddr, myMac);
        }

        public List<String> sendIcmp(int timeoutMillis, byte[] gateway, byte[] myAddr, String dstMac) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return null;
            }
            return MobileWipsFrameworkService.this.mPacketSender.sendIcmp(linkProperties, timeoutMillis, gateway, myAddr, dstMac);
        }

        public int sendDhcp(int timeoutMillis, byte[] myAddr, int equalOption, String equalString) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return 0;
            }
            return MobileWipsFrameworkService.this.mPacketSender.sendDhcp(linkProperties, timeoutMillis, myAddr, equalOption, equalString);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:14:?, code lost:
            return r2;
         */
        /* JADX WARNING: Exception block dominator not found, dom blocks: [] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public byte[] sendDns(long[] r14, byte[] r15, byte[] r16, byte[] r17, java.lang.String r18, boolean r19) throws android.os.RemoteException {
            /*
                r13 = this;
                r1 = r13
                r0 = 0
                byte[] r2 = new byte[r0]
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.this
                android.net.wifi.WifiManager r0 = r0.mWifiManager
                android.net.Network r3 = r0.getCurrentNetwork()
                r0 = 0
                if (r3 == 0) goto L_0x0038
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.this
                android.net.ConnectivityManager r4 = r4.mConnectivityManager
                android.net.LinkProperties r4 = r4.getLinkProperties(r3)
                if (r4 == 0) goto L_0x0039
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.this     // Catch:{ Exception -> 0x0036, all -> 0x0034 }
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender r5 = r0.mPacketSender     // Catch:{ Exception -> 0x0036, all -> 0x0034 }
                r6 = r4
                r7 = r15
                r8 = r16
                r9 = r18
                r10 = r17
                r11 = r14
                r12 = r19
                byte[] r0 = r5.sendDns(r6, r7, r8, r9, r10, r11, r12)     // Catch:{ Exception -> 0x0036, all -> 0x0034 }
                r2 = r0
            L_0x0033:
                goto L_0x0039
            L_0x0034:
                r0 = move-exception
                throw r0
            L_0x0036:
                r0 = move-exception
                goto L_0x0033
            L_0x0038:
                r4 = r0
            L_0x0039:
                return r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.C07492.sendDns(long[], byte[], byte[], byte[], java.lang.String, boolean):byte[]");
        }

        public boolean sendDnsQueries(long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, String dstMac, List<String> dnsMessages, int tcpIndex) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || !MobileWipsFrameworkService.this.mIsConnected || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return false;
            }
            return MobileWipsFrameworkService.this.mDnsRequester.sendDnsQueries(linkProperties, timeoutMillis, srcAddr, dstAddr, dstMac, dnsMessages, tcpIndex);
        }

        public boolean sendTcp(int timeoutMillis, byte[] gateway, byte[] myAddr, String myMac) throws RemoteException {
            LinkProperties linkProperties;
            Network network = MobileWipsFrameworkService.this.mWifiManager.getCurrentNetwork();
            if (network == null || (linkProperties = MobileWipsFrameworkService.this.mConnectivityManager.getLinkProperties(network)) == null) {
                return false;
            }
            return MobileWipsFrameworkService.this.mPacketSender.sendTcp(linkProperties, timeoutMillis, gateway, myAddr, myMac);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:7:?, code lost:
            return false;
         */
        /* JADX WARNING: Exception block dominator not found, dom blocks: [] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean pingTcp(byte[] r9, byte[] r10, int r11, int r12, int r13) throws android.os.RemoteException {
            /*
                r8 = this;
                r0 = 0
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService r1 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.this     // Catch:{ Exception -> 0x0014, all -> 0x0012 }
                com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender r2 = r1.mPacketSender     // Catch:{ Exception -> 0x0014, all -> 0x0012 }
                r3 = r9
                r4 = r10
                r5 = r11
                r6 = r12
                r7 = r13
                boolean r1 = r2.pingTcp(r3, r4, r5, r6, r7)     // Catch:{ Exception -> 0x0014, all -> 0x0012 }
                r0 = r1
                goto L_0x0015
            L_0x0012:
                r1 = move-exception
                throw r1
            L_0x0014:
                r1 = move-exception
            L_0x0015:
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService.C07492.pingTcp(byte[], byte[], int, int, int):boolean");
        }
    };
    /* access modifiers changed from: private */
    public int mRetrycount = 0;
    /* access modifiers changed from: private */
    public boolean mScanStarted = false;
    /* access modifiers changed from: private */
    public SemHqmManager mSemHqmManager = null;
    /* access modifiers changed from: private */
    public IMobileWipsService mService = null;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager = null;
    /* access modifiers changed from: private */
    public WifiNative mWifiNative = null;
    IMobileWipsFramework mWipsFrameworkApi = new IMobileWipsFramework.Stub() {
        public boolean invokeMethodBool(int index) throws RemoteException {
            if (MobileWipsFrameworkService.this.mService == null) {
                return false;
            }
            if (index == 26) {
                String mIfaceName = MobileWipsFrameworkService.this.mWifiNative.getClientInterfaceName();
                if (mIfaceName == null || !MobileWipsFrameworkService.this.mWifiNative.beaconIntervalStart(mIfaceName)) {
                    return false;
                }
                return true;
            } else if (index != 27) {
                if (index == 31) {
                    try {
                        MobileWipsFrameworkService.this.mService.updateWifiChipInfo(WifiChipInfo.getChipsetName(), WifiChipInfo.getInstance().getCidInfo());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (RemoteException e2) {
                        e2.printStackTrace();
                    } catch (NullPointerException e3) {
                        e3.printStackTrace();
                        Log.e(MobileWipsFrameworkService.TAG, "invokeMethodBool Exception!! " + e3);
                    } catch (Exception e4) {
                        e4.printStackTrace();
                    }
                } else if (index == 32) {
                    String mIfaceName2 = MobileWipsFrameworkService.this.mWifiNative.getClientInterfaceName();
                    if (mIfaceName2 != null) {
                        MobileWipsFrameworkService.this.mWifiNative.updateCurrentBss(mIfaceName2);
                    }
                } else if (index != 40) {
                    switch (index) {
                        case 42:
                            DhcpResults dhcpResults = MobileWipsFrameworkService.this.mClientModeImpl.syncGetDhcpResults();
                            if (dhcpResults != null) {
                                return dhcpResults.hasMeteredHint();
                            }
                            return false;
                        case 43:
                        case 44:
                        case 45:
                        case 46:
                        case 47:
                        case 48:
                            return handleNetdCallback(index);
                        case 49:
                            return isEnterprise();
                        case 50:
                            return isCaptivePortal();
                        case 51:
                            return isAndroidHotspot();
                        case 52:
                            if (MobileWipsFrameworkService.this.getWipsValue() == 3) {
                                MobileWipsFrameworkService.this.setWipsValue(1);
                            }
                            MobileWipsFrameworkService.this.sendEmptyMessage(MobileWipsFrameworkService.EVENT_SEND_SAVED_DATA);
                            break;
                        case 53:
                            MobileWipsFrameworkService.this.sendEmptyMessageDelayed(MobileWipsFrameworkService.EVENT_STOP_SERVICE, 0);
                            break;
                    }
                } else {
                    MobileWipsFrameworkService.this.sendEmptyMessage(40);
                }
                return true;
            } else {
                String mIfaceName3 = MobileWipsFrameworkService.this.mWifiNative.getClientInterfaceName();
                if (mIfaceName3 == null || !MobileWipsFrameworkService.this.mWifiNative.beaconIntervalStop(mIfaceName3)) {
                    return false;
                }
                return true;
            }
        }

        public void partialScanStart(Message msg) {
            if (MobileWipsFrameworkService.this.mMobileWipsWifiScanner != null) {
                MobileWipsFrameworkService.this.mMobileWipsWifiScanner.getHandler().sendMessage(msg);
            }
        }

        private boolean handleNetdCallback(int value) {
            if (MobileWipsFrameworkService.this.mMobileWipsNetdEvent != null) {
                return MobileWipsFrameworkService.this.mMobileWipsNetdEvent.setNetdEventStatus(value);
            }
            return false;
        }

        public String invokeMethodStr(int index) {
            if (MobileWipsFrameworkService.this.mService != null && index == 41) {
                return OpBrandingLoader.getInstance().getSupportCharacterSet();
            }
            return "";
        }

        public List<MobileWipsScanResult> getScanResults() {
            List<MobileWipsScanResult> scanResults = new ArrayList<>();
            try {
                for (ScanResult scanResult : ((WifiManager) MobileWipsFrameworkService.this.mContext.getSystemService("wifi")).getScanResults()) {
                    Parcel p = Parcel.obtain();
                    scanResult.writeToParcel(p, 0);
                    p.setDataPosition(0);
                    scanResults.add(MobileWipsScanResult.CREATOR.createFromParcel(p));
                }
            } catch (Exception e) {
                Log.e(MobileWipsFrameworkService.TAG, e.toString());
            }
            return scanResults;
        }

        private boolean isEnterprise() {
            WifiInfo wifiInfo = MobileWipsFrameworkService.this.mWifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.e(MobileWipsFrameworkService.TAG, "WifiInfo is null");
                return false;
            }
            WifiConfiguration wifiConf = MobileWipsFrameworkUtil.getSpecificNetwork(MobileWipsFrameworkService.this.mWifiManager, wifiInfo.getNetworkId());
            if (wifiConf == null || !wifiConf.isEnterprise()) {
                return false;
            }
            return true;
        }

        private boolean isCaptivePortal() {
            WifiInfo wifiInfo = MobileWipsFrameworkService.this.mWifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.e(MobileWipsFrameworkService.TAG, "WifiInfo is null");
                return false;
            }
            WifiConfiguration wifiConf = MobileWipsFrameworkUtil.getSpecificNetwork(MobileWipsFrameworkService.this.mWifiManager, wifiInfo.getNetworkId());
            if (wifiConf == null || !wifiConf.isCaptivePortal) {
                return false;
            }
            return true;
        }

        private boolean isAndroidHotspot() {
            WifiInfo wifiInfo = MobileWipsFrameworkService.this.mWifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.e(MobileWipsFrameworkService.TAG, "WifiInfo is null");
                return false;
            }
            WifiConfiguration wifiConf = MobileWipsFrameworkUtil.getSpecificNetwork(MobileWipsFrameworkService.this.mWifiManager, wifiInfo.getNetworkId());
            if (wifiConf == null || !wifiConf.semSamsungSpecificFlags.get(1)) {
                return false;
            }
            return true;
        }

        public void sendHWParamToHQMwithAppId(int type, String compId, String feature, String hitType, String compVer, String compManufacture, String devCustomDataSet, String basicCustomDataSet, String priCustomDataSet, String appId) {
            MobileWipsFrameworkService.this.mSemHqmManager.sendHWParamToHQMwithAppId(type, compId, feature, hitType, compVer, compManufacture, devCustomDataSet, basicCustomDataSet, priCustomDataSet, appId);
        }
    };
    private ContentObserver mWipsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            MobileWipsFrameworkService mobileWipsFrameworkService = MobileWipsFrameworkService.this;
            int unused = mobileWipsFrameworkService.mWipsValue = mobileWipsFrameworkService.getWipsValue();
            Log.d(MobileWipsFrameworkService.TAG, "mWipsObserver onChange " + MobileWipsFrameworkService.this.mWipsValue);
            if (MobileWipsFrameworkService.this.mWipsValue == 3) {
                if (!MobileWipsFrameworkService.this.mBootCompleted) {
                    MobileWipsFrameworkService.this.setWipsValue(1);
                } else {
                    MobileWipsFrameworkService.this.turnOnWips();
                }
            } else if (MobileWipsFrameworkService.this.mWipsValue != 2) {
            } else {
                if (!MobileWipsFrameworkService.this.mBootCompleted) {
                    MobileWipsFrameworkService.this.setWipsValue(0);
                } else {
                    MobileWipsFrameworkService.this.turnOffWips();
                }
            }
        }
    };
    private ServiceHandler mWipsServiceHandler;
    private HandlerThread mWipsThread;
    /* access modifiers changed from: private */
    public int mWipsValue = 0;

    static /* synthetic */ int access$2008(MobileWipsFrameworkService x0) {
        int i = x0.mRetrycount;
        x0.mRetrycount = i + 1;
        return i;
    }

    public static synchronized MobileWipsFrameworkService init(Context context) {
        MobileWipsFrameworkService mobileWipsFrameworkService;
        synchronized (MobileWipsFrameworkService.class) {
            if (mInstance == null) {
                mInstance = new MobileWipsFrameworkService(context);
            }
            mobileWipsFrameworkService = mInstance;
        }
        return mobileWipsFrameworkService;
    }

    public static synchronized MobileWipsFrameworkService getInstance() {
        MobileWipsFrameworkService mobileWipsFrameworkService;
        synchronized (MobileWipsFrameworkService.class) {
            mobileWipsFrameworkService = mInstance;
        }
        return mobileWipsFrameworkService;
    }

    private MobileWipsFrameworkService(Context context) {
        this.mContext = context;
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        this.mClientModeImpl = WifiInjector.getInstance().getClientModeImpl();
        this.mSemHqmManager = (SemHqmManager) this.mContext.getSystemService("HqmManagerService");
        this.mWipsThread = new HandlerThread(TAG);
        this.mWipsThread.start();
        this.mWipsServiceHandler = new ServiceHandler(this.mWipsThread.getLooper());
        this.mMobileWipsWifiScanner = new MobileWipsWifiScanner(context);
        this.mMobileWipsNetdEvent = new MobileWipsNetdEvent(context);
        this.mPacketSender = new MobileWipsPacketSender();
        this.mDnsRequester = new MobileWipsDnsRequester(this.mPacketSender);
        this.mWipsValue = getWipsValue();
        this.mRetrycount = 0;
        registerForBroadcastsForWifiWIPS();
    }

    public void sendMessage(Message msg) {
        this.mWipsServiceHandler.sendMessage(msg);
    }

    public void sendEmptyMessageDelayed(int id, int delay) {
        this.mWipsServiceHandler.sendEmptyMessageDelayed(id, (long) delay);
    }

    public void removeMessages(int id) {
        this.mWipsServiceHandler.removeMessages(id);
    }

    public void sendEmptyMessage(int id) {
        Message msg = Message.obtain();
        msg.what = id;
        sendMessage(msg);
    }

    public ServiceHandler getHandler() {
        return this.mWipsServiceHandler;
    }

    final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String mIfaceName;
            int i = msg.what;
            if (i == 7) {
                boolean unused = MobileWipsFrameworkService.this.mIsConnected = true;
            } else if (i == 40) {
                MobileWipsFrameworkService.this.removeSpecialSsidInternal();
            } else if (i != MobileWipsFrameworkService.EVENT_WIPS_MONITOR) {
                if (i != 9) {
                    if (i == 10) {
                        boolean unused2 = MobileWipsFrameworkService.this.mIsConnected = false;
                    } else if (i == 12) {
                        boolean unused3 = MobileWipsFrameworkService.this.mScanStarted = true;
                    } else if (i == 13) {
                        boolean unused4 = MobileWipsFrameworkService.this.mScanStarted = false;
                    } else if (i == MobileWipsFrameworkService.EVENT_START_SERVICE) {
                        MobileWipsFrameworkService.this.startWipsService();
                    } else if (i == MobileWipsFrameworkService.EVENT_STOP_SERVICE) {
                        MobileWipsFrameworkService.this.stopWipsService();
                    }
                }
                if (MobileWipsFrameworkService.this.mDnsRequester != null) {
                    MobileWipsFrameworkService.this.mDnsRequester.stop();
                }
            } else {
                MobileWipsFrameworkService.this.monitorWipsStatus();
            }
            if (MobileWipsFrameworkService.this.mService == null) {
                Log.e(MobileWipsFrameworkService.TAG, "unbinded can not send " + msg.what);
                if (msg.what == MobileWipsFrameworkService.EVENT_BCN_INTERVAL && (mIfaceName = MobileWipsFrameworkService.this.mWifiNative.getClientInterfaceName()) != null) {
                    MobileWipsFrameworkService.this.mWifiNative.beaconIntervalStop(mIfaceName);
                    return;
                }
                return;
            }
            int i2 = msg.what;
            if (!(i2 == 19 || i2 == 20 || i2 == 24 || i2 == 35)) {
                if (i2 == MobileWipsFrameworkService.EVENT_SEND_SAVED_DATA) {
                    MobileWipsFrameworkService.this.sendSavedData();
                    return;
                } else if (!(i2 == 37 || i2 == 38)) {
                    if (i2 == MobileWipsFrameworkService.EVENT_BCN_INTERVAL) {
                        try {
                            if (msg.obj instanceof Bundle) {
                                Bundle args = (Bundle) msg.obj;
                                MobileWipsFrameworkService.this.mService.broadcastBcnIntervalEvent(args.getString("iface"), args.getString("ssid"), args.getString("bssid"), args.getInt("channel"), args.getInt("beaconInterval"), args.getLong("timestamp"), args.getLong("systemtime"));
                                return;
                            }
                            return;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            return;
                        } catch (RemoteException e2) {
                            e2.printStackTrace();
                            return;
                        } catch (NullPointerException e3) {
                            e3.printStackTrace();
                            Log.e(MobileWipsFrameworkService.TAG, "broadcastBcnIntervalEvent Exception!!" + e3);
                            return;
                        } catch (Exception e4) {
                            e4.printStackTrace();
                            return;
                        }
                    } else if (i2 != MobileWipsFrameworkService.EVENT_BCN_ABORT) {
                        switch (i2) {
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                                break;
                            default:
                                switch (i2) {
                                    case 12:
                                    case 13:
                                    case 14:
                                    case 15:
                                    case 16:
                                    case 17:
                                        break;
                                    default:
                                        return;
                                }
                        }
                    } else {
                        try {
                            if (msg.obj instanceof Bundle) {
                                Bundle args2 = (Bundle) msg.obj;
                                MobileWipsFrameworkService.this.mService.broadcastBcnEventAbort(args2.getString("iface"), args2.getInt("abortReason"));
                                return;
                            }
                            return;
                        } catch (IllegalArgumentException e5) {
                            e5.printStackTrace();
                            return;
                        } catch (RemoteException e6) {
                            e6.printStackTrace();
                            return;
                        } catch (NullPointerException e7) {
                            e7.printStackTrace();
                            Log.e(MobileWipsFrameworkService.TAG, "broadcastBcnEventAbort Exception!!" + e7);
                            return;
                        } catch (Exception e8) {
                            e8.printStackTrace();
                            return;
                        }
                    }
                }
            }
            try {
                MobileWipsFrameworkService.this.mService.sendMessage(msg);
            } catch (IllegalArgumentException e9) {
                e9.printStackTrace();
            } catch (RemoteException e10) {
                e10.printStackTrace();
            } catch (NullPointerException e11) {
                e11.printStackTrace();
                Log.e(MobileWipsFrameworkService.TAG, "handleMessage Exception!!" + e11);
            } catch (Exception e12) {
                e12.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    public void monitorWipsStatus() {
        int wipsPid;
        WifiManager wifiManager;
        Log.d(TAG, "monitorWipsStatus " + this.mWipsValue);
        int i = this.mWipsValue;
        if (i == 3) {
            setWipsValue(2);
            stopWipsService();
            setWipsValue(0);
        } else if (i == 2) {
            stopWipsService();
            setWipsValue(0);
        }
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        if ((this.mWipsValue == 0 || ((wifiManager = this.mWifiManager) != null && wifiManager.getWifiState() == 1)) && (wipsPid = getWipsPid()) > -1) {
            Process.killProcess(wipsPid);
        }
    }

    public void broadcastBcnIntervalEvent(String iface, String ssid, String bssid, int channel, int beaconInterval, long timestamp, long systemtime) {
        Message msg = Message.obtain();
        msg.what = EVENT_BCN_INTERVAL;
        Bundle args = new Bundle();
        args.putString("iface", iface);
        args.putString("ssid", ssid);
        args.putString("bssid", bssid);
        args.putInt("channel", channel);
        args.putInt("beaconInterval", beaconInterval);
        args.putLong("timestamp", timestamp);
        args.putLong("systemtime", systemtime);
        msg.obj = args;
        sendMessage(msg);
    }

    public void broadcastBcnEventAbort(String iface, int abortReason) {
        Message msg = Message.obtain();
        msg.what = EVENT_BCN_ABORT;
        Bundle args = new Bundle();
        args.putString("iface", iface);
        args.putInt("abortReason", abortReason);
        msg.obj = args;
        sendMessage(msg);
    }

    public boolean checkMWIPS(String bssid, int freq) {
        IMobileWipsService iMobileWipsService = this.mService;
        if (iMobileWipsService == null) {
            return false;
        }
        try {
            return iMobileWipsService.checkMWIPS(bssid, freq);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (RemoteException e2) {
            e2.printStackTrace();
            return false;
        } catch (NullPointerException e3) {
            e3.printStackTrace();
            Log.e(TAG, "checkMWIPS Exception!!" + e3);
            return false;
        } catch (Exception e4) {
            e4.printStackTrace();
            return false;
        }
    }

    public boolean setCurrentBss(ISehSupplicantStaNetwork.BssParam param) {
        IMobileWipsService iMobileWipsService = this.mService;
        if (iMobileWipsService == null) {
            return false;
        }
        if (param == null) {
            return iMobileWipsService.setCurrentBss("", 0, new byte[1]);
        }
        try {
            String bssid = NativeUtil.macAddressFromByteArray(param.bssid);
            byte[] ieData = new byte[param.ieData.size()];
            for (int i = 0; i < param.ieData.size(); i++) {
                ieData[i] = param.ieData.get(i).byteValue();
            }
            return this.mService.setCurrentBss(bssid, param.freq, ieData);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (RemoteException e2) {
            e2.printStackTrace();
            return false;
        } catch (NullPointerException e3) {
            e3.printStackTrace();
            Log.e(TAG, "setCurrentBss Exception!!" + e3);
            return false;
        } catch (Exception e4) {
            e4.printStackTrace();
            return false;
        }
    }

    public void onScanResults(List<MobileWipsScanResult> scanResults) {
        IMobileWipsService iMobileWipsService = this.mService;
        if (iMobileWipsService != null) {
            try {
                iMobileWipsService.onScanResults(scanResults);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (RemoteException e2) {
                e2.printStackTrace();
            } catch (Exception e3) {
                e3.printStackTrace();
            }
        }
    }

    public void onDnsResponses(List<String> dnsResponses, String dstMac) {
        IMobileWipsService iMobileWipsService = this.mService;
        if (iMobileWipsService != null) {
            try {
                iMobileWipsService.onDnsResponses(dnsResponses, dstMac);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (RemoteException e2) {
                e2.printStackTrace();
            } catch (Exception e3) {
                e3.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkSpecialNetwork(boolean remove) {
        if (remove) {
            sendEmptyMessageDelayed(40, IWCEventManager.wifiOFFPending_MS);
        } else {
            removeMessages(40);
        }
    }

    /* access modifiers changed from: private */
    public void removeSpecialSsidInternal() {
        Log.d(TAG, "request removeSpecialSsid");
        if (mSpecialNetworkDeleted) {
            Log.d(TAG, "Aleady deleted");
            return;
        }
        int deletedCnt = 0;
        try {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            List<WifiConfiguration> savedNetworks = wifiManager.getConfiguredNetworks();
            if (savedNetworks != null) {
                for (WifiConfiguration config : savedNetworks) {
                    if (config.semSamsungSpecificFlags.get(6) && wifiManager.removeNetwork(config.networkId)) {
                        Log.d(TAG, "remove wifi configuration " + config.networkId);
                        deletedCnt++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "removeSpecialSsid error!! " + e);
        }
        if (deletedCnt == 2) {
            mSpecialNetworkDeleted = true;
            Log.d(TAG, "All the special networks are removed");
        }
    }

    /* access modifiers changed from: private */
    public void sendSavedData() {
        Message msg = new Message();
        if (this.mScanStarted) {
            msg.what = 12;
        } else {
            msg.what = 13;
        }
        IMobileWipsService iMobileWipsService = this.mService;
        if (iMobileWipsService != null) {
            try {
                iMobileWipsService.sendMessage(msg);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (RemoteException e2) {
                e2.printStackTrace();
            } catch (NullPointerException e3) {
                e3.printStackTrace();
                Log.e(TAG, "sendSavedData Exception!!" + e3);
            } catch (Exception e4) {
                e4.printStackTrace();
            }
        }
        if (this.mIsConnected) {
            msg.what = 7;
            IMobileWipsService iMobileWipsService2 = this.mService;
            if (iMobileWipsService2 != null) {
                try {
                    iMobileWipsService2.sendMessage(msg);
                } catch (IllegalArgumentException e5) {
                    e5.printStackTrace();
                } catch (RemoteException e6) {
                    e6.printStackTrace();
                } catch (NullPointerException e7) {
                    e7.printStackTrace();
                    Log.e(TAG, "sendSavedData Exception!!" + e7);
                } catch (Exception e8) {
                    e8.printStackTrace();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public synchronized void startWipsService() {
        int i;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(MWIPS_PACKAGE, MWIPS_SERVICE));
        intent.putExtra("action", "start");
        intent.putExtra("version", 1);
        if (SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEDEFAULTMWIPS)) {
            i = 0;
        } else {
            i = 1;
        }
        intent.putExtra("mobileWipsDefault", i);
        intent.putExtra("bigdataEnabled", true);
        try {
            if (this.mService == null) {
                this.mContext.startServiceAsUser(intent, Process.myUserHandle());
                this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle());
                Log.i(TAG, "startMobileWipsService started");
            } else {
                Log.i(TAG, "startMobileWipsService aleady binded");
            }
        } catch (Exception e) {
            Log.e(TAG, "startMobileWipsService failed " + e);
        }
        return;
    }

    /* access modifiers changed from: private */
    public synchronized void stopWipsService() {
        this.mMobileWipsWifiScanner.stop();
        this.mDnsRequester.stop();
        if (this.mService != null) {
            Log.i(TAG, "request unbind service");
            try {
                this.mService.unregisterCallback(this.mWipsFrameworkApi);
                this.mService.unregisterPacketSender(this.mPacketSenderFrameworkApi);
                this.mService = null;
                this.mContext.unbindService(this.mConnection);
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(MWIPS_PACKAGE, MWIPS_SERVICE));
                this.mContext.stopServiceAsUser(intent, Process.myUserHandle());
                removeBadge();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        if (this.mWifiManager != null && this.mWifiManager.getWifiState() == 3) {
            setWipsValue(0);
        }
    }

    private boolean checkMwipsPackageVersion() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        int mwipsAppVersion = -1;
        try {
            PackageInfo packageInfo = pm.getPackageInfo(MWIPS_PACKAGE, 0);
            if (packageInfo != null && (mwipsAppVersion = packageInfo.versionCode) >= 100000000 && mwipsAppVersion < 200000000) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "MobileWips package not installed");
            return false;
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        Log.e(TAG, "MobileWips package version missmatch, mwips " + mwipsAppVersion + " framework " + 1);
        return false;
    }

    private boolean hasWifiWIPSPermission(String packageName) {
        return this.mContext.getPackageManager().checkPermission("com.samsung.permission.WIFI_WIPS", packageName) == 0;
    }

    public void registerReciever() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, (String) null, (Handler) null);
    }

    public void unregisterReciever() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    private void clearWipsServiceStatusEvent() {
        Log.d(TAG, "clearWipsServiceStatusEvent");
        getHandler().removeCallbacksAndMessages((Object) null);
    }

    /* access modifiers changed from: private */
    public void setWipsValue(int index) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_mwips", index);
        this.mWipsValue = index;
    }

    /* access modifiers changed from: private */
    public int getWipsValue() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_mwips", (int) (SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_DISABLEDEFAULTMWIPS) ^ 1));
    }

    /* access modifiers changed from: private */
    public void turnOnWips() {
        turnOnWips(0);
    }

    /* access modifiers changed from: private */
    public void turnOnWips(int interval) {
        int i = this.mWipsValue;
        if (i == 0 || i == 2) {
            Log.e(TAG, "wips disabled, do not start service");
        } else if (!this.mBootCompleted) {
            Log.e(TAG, "booting not completed");
        } else if (checkMwipsPackageVersion()) {
            if (!hasWifiWIPSPermission(MWIPS_PACKAGE)) {
                Log.e(TAG, "com.samsung.android.server.wifi.mobilewips.client does not have valid permission");
                return;
            }
            if (this.mWifiManager == null) {
                this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            }
            WifiManager wifiManager = this.mWifiManager;
            if (wifiManager == null || wifiManager.getWifiState() != 1) {
                clearWipsServiceStatusEvent();
                sendEmptyMessageDelayed(EVENT_START_SERVICE, interval);
                sendEmptyMessageDelayed(EVENT_WIPS_MONITOR, interval + IWCEventManager.wifiOFFPending_MS);
                return;
            }
            Log.e(TAG, "wifi is turned off, do not turn on wips");
        }
    }

    /* access modifiers changed from: private */
    public void turnOffWips() {
        if (!this.mBootCompleted) {
            Log.e(TAG, "booting not completed");
            return;
        }
        clearWipsServiceStatusEvent();
        sendEmptyMessage(16);
        sendEmptyMessageDelayed(EVENT_WIPS_MONITOR, IWCEventManager.wifiOFFPending_MS);
    }

    public void registerPackageReciever() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiverPackage, intentFilter, (String) null, (Handler) null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("wifi_mwips"), false, this.mWipsObserver);
    }

    public void unregisterPackageReciever() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiverPackage);
        this.mContext.getContentResolver().unregisterContentObserver(this.mWipsObserver);
    }

    public void registerForBroadcastsForWifiWIPS() {
        registerReciever();
        registerPackageReciever();
    }

    public void unregisterForBroadcastsForWifiWIPS() {
        unregisterReciever();
        unregisterPackageReciever();
    }

    private int getWipsPid() {
        List<ActivityManager.RunningAppProcessInfo> pids = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        for (int i = 0; i < pids.size(); i++) {
            ActivityManager.RunningAppProcessInfo info = pids.get(i);
            if (info.processName.equalsIgnoreCase(MWIPS_PACKAGE)) {
                return info.pid;
            }
        }
        return -1;
    }

    private void removeBadge() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Uri uri = Uri.parse("content://com.samsung.server.wifi.mobilewips.client/detection");
        long timestamp = Long.parseLong(new SimpleDateFormat("yyMMddHHmmss").format(new Date(System.currentTimeMillis())));
        String[] selectionArgs = {"0"};
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("seen_time", Long.valueOf(timestamp));
            contentResolver.update(uri, contentValues, "seen_time=?", selectionArgs);
        } catch (SQLException ex) {
            Log.e(TAG, "error removeBadge() " + ex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
