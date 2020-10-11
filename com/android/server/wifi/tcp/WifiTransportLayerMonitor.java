package com.android.server.wifi.tcp;

import android.app.ActivityManager;
import android.app.usage.IUsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.net.ConnectivityManager;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.WifiConnectivityMonitor;
import com.android.server.wifi.tcp.WifiApInfo;
import com.samsung.android.app.usage.IUsageStatsWatcher;
import com.samsung.android.feature.SemCscFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WifiTransportLayerMonitor extends Handler {
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "WifiTransportLayerMonitor";
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public final Context mContext;
    private WifiApInfo mCurrentWifiApInfo;
    /* access modifiers changed from: private */
    public WifiPackageInfo mCurrentWifiPackageInfo;
    private PackageManager mPackageManager;
    private BroadcastReceiver mPackageReceiver;
    /* access modifiers changed from: private */
    public PackageUpdateHandler mPackageUpdateHandler;
    private IUsageStatsManager mUsageStatsManager;
    /* access modifiers changed from: private */
    public String mUsageStatsPackageName;
    /* access modifiers changed from: private */
    public int mUsageStatsUid;
    private final IUsageStatsWatcher.Stub mUsageStatsWatcher = new IUsageStatsWatcher.Stub() {
        public void noteResumeComponent(ComponentName resumeComponentName, Intent intent) {
            if (resumeComponentName == null) {
                try {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d(WifiTransportLayerMonitor.TAG, "resumeComponentName is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String packageName = resumeComponentName.getPackageName();
                String packageName2 = ActivityManager.getService().getFocusedStackInfo().topActivity.getPackageName();
                if (!WifiTransportLayerMonitor.this.mUsageStatsPackageName.equals(packageName2)) {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d(WifiTransportLayerMonitor.TAG, "IUsageStatsWatcher resume package changed: " + packageName2);
                    }
                    int unused = WifiTransportLayerMonitor.this.mUsageStatsUid = WifiTransportLayerMonitor.this.getPackageManager().getApplicationInfo(packageName2, 128).uid;
                    String unused2 = WifiTransportLayerMonitor.this.mUsageStatsPackageName = packageName2;
                    WifiPackageInfo unused3 = WifiTransportLayerMonitor.this.mCurrentWifiPackageInfo = WifiTransportLayerMonitor.this.getOrCreatePackageInfo(WifiTransportLayerMonitor.this.mUsageStatsUid, WifiTransportLayerMonitor.this.mUsageStatsPackageName);
                }
            }
        }

        public void notePauseComponent(ComponentName pauseComponentName) {
            if (pauseComponentName == null) {
                try {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d(WifiTransportLayerMonitor.TAG, "pauseComponentName is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String packageName = pauseComponentName.getPackageName();
                String packageName2 = ActivityManager.getService().getFocusedStackInfo().topActivity.getPackageName();
                if (!WifiTransportLayerMonitor.this.mUsageStatsPackageName.equals(packageName2)) {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d(WifiTransportLayerMonitor.TAG, "IUsageStatsWatcher pause package changed: " + packageName2);
                    }
                    int unused = WifiTransportLayerMonitor.this.mUsageStatsUid = WifiTransportLayerMonitor.this.getPackageManager().getApplicationInfo(packageName2, 128).uid;
                    String unused2 = WifiTransportLayerMonitor.this.mUsageStatsPackageName = packageName2;
                    WifiPackageInfo unused3 = WifiTransportLayerMonitor.this.mCurrentWifiPackageInfo = WifiTransportLayerMonitor.this.getOrCreatePackageInfo(WifiTransportLayerMonitor.this.mUsageStatsUid, WifiTransportLayerMonitor.this.mUsageStatsPackageName);
                }
            }
        }

        public void noteStopComponent(ComponentName arg0) throws RemoteException {
        }
    };
    private HashMap<String, WifiApInfo> mWifiApInfoList;
    /* access modifiers changed from: private */
    public HashMap<Integer, WifiPackageInfo> mWifiPackageInfoList;
    /* access modifiers changed from: private */
    public final Object mWifiPackageInfoLock = new Object();
    private ArrayList<Integer> mWifiSwitchEnabledUidList = new ArrayList<>();
    private WifiTransportLayerFileManager mWifiTransportLayerFileManager = new WifiTransportLayerFileManager();

    public WifiTransportLayerMonitor(Looper looper, WifiConnectivityMonitor wifiConnectivityMonitor, ClientModeImpl cmi, Context context) {
        super(looper);
        this.mContext = context;
        HandlerThread networkStatsThread = new HandlerThread("NetworkStatsThread");
        networkStatsThread.start();
        this.mPackageUpdateHandler = new PackageUpdateHandler(networkStatsThread.getLooper());
        try {
            this.mUsageStatsUid = -1;
            this.mUsageStatsPackageName = "default";
            this.mUsageStatsManager = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
            this.mUsageStatsManager.registerUsageStatsWatcher(this.mUsageStatsWatcher);
        } catch (Exception e) {
            Log.w(TAG, "Exception occured while register UsageStatWatcher " + e);
            e.printStackTrace();
        }
        loadInfoFromFile();
        setupBroadcastReceiver();
        setAudioPlaybackCallback();
    }

    private class PackageUpdateHandler extends Handler {
        private static final int MSG_CREATE_PACKAGE_INFO = 3;
        private static final int MSG_RUN_UPDATE_PACKAGE_INFO = 1;
        private static final int MSG_UPDATE_CATEGORY = 2;
        private static final int MSG_UPDATE_PACKAGE_INFO = 4;
        private final String TAG = "WifiTransportLayerMonitor.PackageUpdateHandler";

        public PackageUpdateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                if (WifiTransportLayerMonitor.DBG != 0) {
                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "MSG_RUN_UPDATE_PACKAGE_INFO");
                }
                updateMissingPackageInfo();
                updatePackageCategoryInfo();
            } else if (i == 2) {
                int uidCreate = msg.arg1;
                String packageName = (String) msg.obj;
                if (WifiTransportLayerMonitor.DBG) {
                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "MSG_UPDATE_CATEGORY - " + packageName);
                }
                String category = WifiTransportLayerUtils.getApplicationCategory(WifiTransportLayerMonitor.this.mContext, packageName);
                if (!category.equals(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE)) {
                    WifiTransportLayerMonitor.this.updateWifiPackageInfoCategory(uidCreate, category);
                    WifiTransportLayerMonitor.this.saveWifiPackageInfoList();
                }
            } else if (i == 3) {
                int uidCreate2 = msg.arg1;
                String packageNameCreate = (String) msg.obj;
                if (WifiTransportLayerMonitor.DBG) {
                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "MSG_CREATE_PACKAGE_INFO - " + uidCreate2);
                }
                WifiPackageInfo unused = WifiTransportLayerMonitor.this.createWifiPackageInfo(uidCreate2, packageNameCreate);
            } else if (i == 4) {
                int uidUpdate = msg.arg1;
                if (WifiTransportLayerMonitor.DBG) {
                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "MSG_UPDATE_PACKAGE_INFO - " + uidUpdate);
                }
                WifiPackageInfo info = WifiTransportLayerMonitor.this.getWifiPackageInfo(uidUpdate);
                if (info != null) {
                    info.updatePackageInfo(WifiTransportLayerMonitor.this.mContext);
                    WifiTransportLayerMonitor.this.updateWifiPackageInfo(info, true);
                }
            }
        }

        private void updateMissingPackageInfo() {
            ArrayList<WifiPackageInfo> updateList = new ArrayList<>();
            try {
                for (ApplicationInfo app : WifiTransportLayerMonitor.this.getPackageManager().getInstalledApplications(0)) {
                    if (WifiTransportLayerMonitor.this.mWifiPackageInfoList != null && !WifiTransportLayerMonitor.this.mWifiPackageInfoList.containsKey(Integer.valueOf(app.uid))) {
                        if (WifiTransportLayerMonitor.DBG) {
                            Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "updateMissingPackageInfo (add) - " + app.uid + ":" + app.packageName);
                        }
                        updateList.add(new WifiPackageInfo(WifiTransportLayerMonitor.this.mContext, app.uid, app.packageName));
                    }
                }
                WifiTransportLayerMonitor.this.updateWifiPackageInfoList(updateList);
            } catch (Exception e) {
                if (WifiTransportLayerMonitor.DBG) {
                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "updateMissingPackageInfo - Exception " + e);
                }
                e.printStackTrace();
            }
        }

        private void updatePackageCategoryInfo() {
            if (WifiTransportLayerMonitor.this.mWifiPackageInfoList != null && WifiTransportLayerMonitor.this.isCategoryUpdateable()) {
                try {
                    ArrayList<Integer> uidList = new ArrayList<>();
                    uidList.addAll(WifiTransportLayerMonitor.this.mWifiPackageInfoList.keySet());
                    Iterator<Integer> it = uidList.iterator();
                    while (it.hasNext()) {
                        int uid = it.next().intValue();
                        if (WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE.equals(WifiTransportLayerMonitor.this.getWifiPackageInfoCategory(uid))) {
                            String packageName = WifiTransportLayerMonitor.this.getPackageName(uid);
                            String category = WifiTransportLayerUtils.getApplicationCategory(WifiTransportLayerMonitor.this.mContext, packageName);
                            if (WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE.equals(category)) {
                                WifiTransportLayerMonitor.this.updateWifiPackageInfoCategoryFailHistory(uid);
                            } else {
                                if (WifiTransportLayerMonitor.DBG) {
                                    Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "updatePackageCategoryInfo - " + packageName + "-" + category);
                                }
                                WifiTransportLayerMonitor.this.updateWifiPackageInfoCategory(uid, category);
                            }
                        }
                    }
                    WifiTransportLayerMonitor.this.saveWifiPackageInfoList();
                } catch (Exception e) {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d("WifiTransportLayerMonitor.PackageUpdateHandler", "updatePackageCategoryInfo - Exception " + e);
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public PackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = this.mContext.getPackageManager();
        }
        return this.mPackageManager;
    }

    private void setAudioPlaybackCallback() {
        if (DBG) {
            Log.d(TAG, "setAudioPlaybackCallback");
        }
        ((AudioManager) this.mContext.getSystemService("audio")).registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                if (configs != null) {
                    for (AudioPlaybackConfiguration config : configs) {
                        if (config.getAudioAttributes().getUsage() == 2) {
                            synchronized (WifiTransportLayerMonitor.this.mWifiPackageInfoLock) {
                                if (WifiTransportLayerMonitor.this.mWifiPackageInfoList != null && WifiTransportLayerMonitor.this.mWifiPackageInfoList.containsKey(Integer.valueOf(config.getClientUid())) && !((WifiPackageInfo) WifiTransportLayerMonitor.this.mWifiPackageInfoList.get(Integer.valueOf(config.getClientUid()))).isVoip()) {
                                    if (WifiTransportLayerMonitor.DBG) {
                                        Log.d(WifiTransportLayerMonitor.TAG, "onPlaybackConfigChanged - " + config.getClientUid() + " added");
                                    }
                                    ((WifiPackageInfo) WifiTransportLayerMonitor.this.mWifiPackageInfoList.get(Integer.valueOf(config.getClientUid()))).setIsVoip(true);
                                }
                            }
                        }
                    }
                }
                super.onPlaybackConfigChanged(configs);
            }
        }, this);
    }

    private void setupBroadcastReceiver() {
        if (DBG) {
            Log.d(TAG, "setupBroadcastReceiver");
        }
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (((action.hashCode() == 798292259 && action.equals("android.intent.action.BOOT_COMPLETED")) ? (char) 0 : 65535) == 0) {
                    if (WifiTransportLayerMonitor.DBG) {
                        Log.d(WifiTransportLayerMonitor.TAG, "ACTION_BOOT_COMPLETED");
                    }
                    WifiTransportLayerMonitor.this.mPackageUpdateHandler.sendEmptyMessage(1);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mPackageReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Removed duplicated region for block: B:18:0x0040  */
            /* JADX WARNING: Removed duplicated region for block: B:53:0x00fe  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(android.content.Context r12, android.content.Intent r13) {
                /*
                    r11 = this;
                    java.lang.String r0 = r13.getAction()
                    int r1 = r0.hashCode()
                    r2 = -810471698(0xffffffffcfb12eee, float:-5.9452856E9)
                    r3 = 2
                    r4 = 1
                    r5 = 0
                    if (r1 == r2) goto L_0x002f
                    r2 = 525384130(0x1f50b9c2, float:4.419937E-20)
                    if (r1 == r2) goto L_0x0025
                    r2 = 1544582882(0x5c1076e2, float:1.62652439E17)
                    if (r1 == r2) goto L_0x001b
                L_0x001a:
                    goto L_0x0039
                L_0x001b:
                    java.lang.String r1 = "android.intent.action.PACKAGE_ADDED"
                    boolean r1 = r0.equals(r1)
                    if (r1 == 0) goto L_0x001a
                    r1 = r5
                    goto L_0x003a
                L_0x0025:
                    java.lang.String r1 = "android.intent.action.PACKAGE_REMOVED"
                    boolean r1 = r0.equals(r1)
                    if (r1 == 0) goto L_0x001a
                    r1 = r4
                    goto L_0x003a
                L_0x002f:
                    java.lang.String r1 = "android.intent.action.PACKAGE_REPLACED"
                    boolean r1 = r0.equals(r1)
                    if (r1 == 0) goto L_0x001a
                    r1 = r3
                    goto L_0x003a
                L_0x0039:
                    r1 = -1
                L_0x003a:
                    r2 = 128(0x80, float:1.794E-43)
                    java.lang.String r6 = "WifiTransportLayerMonitor"
                    if (r1 == 0) goto L_0x00fe
                    if (r1 == r4) goto L_0x00a3
                    if (r1 == r3) goto L_0x0046
                    goto L_0x0175
                L_0x0046:
                    android.net.Uri r1 = r13.getData()
                    if (r1 == 0) goto L_0x0175
                    android.net.Uri r1 = r13.getData()
                    java.lang.String r3 = ""
                    if (r1 == 0) goto L_0x0175
                    java.lang.String r3 = r1.getSchemeSpecificPart()
                    if (r3 == 0) goto L_0x0175
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x009d }
                    android.content.pm.PackageManager r4 = r4.getPackageManager()     // Catch:{ NameNotFoundException -> 0x009d }
                    android.content.pm.ApplicationInfo r2 = r4.getApplicationInfo(r3, r2)     // Catch:{ NameNotFoundException -> 0x009d }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x009d }
                    java.util.HashMap r4 = r4.mWifiPackageInfoList     // Catch:{ NameNotFoundException -> 0x009d }
                    if (r4 == 0) goto L_0x009b
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x009d }
                    java.util.HashMap r4 = r4.mWifiPackageInfoList     // Catch:{ NameNotFoundException -> 0x009d }
                    int r7 = r2.uid     // Catch:{ NameNotFoundException -> 0x009d }
                    java.lang.Integer r7 = java.lang.Integer.valueOf(r7)     // Catch:{ NameNotFoundException -> 0x009d }
                    boolean r4 = r4.containsKey(r7)     // Catch:{ NameNotFoundException -> 0x009d }
                    if (r4 == 0) goto L_0x009b
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x009d }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor$PackageUpdateHandler r4 = r4.mPackageUpdateHandler     // Catch:{ NameNotFoundException -> 0x009d }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r7 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x009d }
                    r8 = 4
                    int r9 = r2.uid     // Catch:{ NameNotFoundException -> 0x009d }
                    android.os.Message r5 = r7.obtainMessage(r8, r9, r5)     // Catch:{ NameNotFoundException -> 0x009d }
                    r4.sendMessage(r5)     // Catch:{ NameNotFoundException -> 0x009d }
                    boolean r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.DBG     // Catch:{ NameNotFoundException -> 0x009d }
                    if (r4 == 0) goto L_0x009b
                    java.lang.String r4 = "ACTION_PACKAGE_REPLACED - updated"
                    android.util.Log.d(r6, r4)     // Catch:{ NameNotFoundException -> 0x009d }
                L_0x009b:
                    goto L_0x0175
                L_0x009d:
                    r2 = move-exception
                    r2.printStackTrace()
                    goto L_0x0175
                L_0x00a3:
                    java.lang.String r1 = "android.intent.extra.REPLACING"
                    boolean r1 = r13.getBooleanExtra(r1, r5)
                    if (r1 == 0) goto L_0x00b8
                    boolean r2 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.DBG
                    if (r2 == 0) goto L_0x0175
                    java.lang.String r2 = "ACTION_PACKAGE_REMOVED - remove app before replace"
                    android.util.Log.d(r6, r2)
                    goto L_0x0175
                L_0x00b8:
                    android.net.Uri r3 = r13.getData()
                    if (r3 == 0) goto L_0x0175
                    android.net.Uri r3 = r13.getData()
                    java.lang.String r4 = ""
                    if (r3 == 0) goto L_0x00fc
                    java.lang.String r4 = r3.getSchemeSpecificPart()
                    if (r4 == 0) goto L_0x00fc
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r5 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x00f8 }
                    android.content.pm.PackageManager r5 = r5.getPackageManager()     // Catch:{ NameNotFoundException -> 0x00f8 }
                    android.content.pm.ApplicationInfo r2 = r5.getApplicationInfo(r4, r2)     // Catch:{ NameNotFoundException -> 0x00f8 }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r5 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x00f8 }
                    int r7 = r2.uid     // Catch:{ NameNotFoundException -> 0x00f8 }
                    r5.removeWifiPackageInfo(r7)     // Catch:{ NameNotFoundException -> 0x00f8 }
                    boolean r5 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.DBG     // Catch:{ NameNotFoundException -> 0x00f8 }
                    if (r5 == 0) goto L_0x00f7
                    java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ NameNotFoundException -> 0x00f8 }
                    r5.<init>()     // Catch:{ NameNotFoundException -> 0x00f8 }
                    java.lang.String r7 = "ACTION_PACKAGE_REMOVED - "
                    r5.append(r7)     // Catch:{ NameNotFoundException -> 0x00f8 }
                    r5.append(r4)     // Catch:{ NameNotFoundException -> 0x00f8 }
                    java.lang.String r5 = r5.toString()     // Catch:{ NameNotFoundException -> 0x00f8 }
                    android.util.Log.d(r6, r5)     // Catch:{ NameNotFoundException -> 0x00f8 }
                L_0x00f7:
                    goto L_0x00fc
                L_0x00f8:
                    r2 = move-exception
                    r2.printStackTrace()
                L_0x00fc:
                    goto L_0x0175
                L_0x00fe:
                    android.net.Uri r1 = r13.getData()
                    if (r1 == 0) goto L_0x0175
                    android.net.Uri r1 = r13.getData()
                    java.lang.String r3 = ""
                    if (r1 == 0) goto L_0x0175
                    java.lang.String r3 = r1.getSchemeSpecificPart()
                    if (r3 == 0) goto L_0x0175
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x0171 }
                    android.content.pm.PackageManager r4 = r4.getPackageManager()     // Catch:{ NameNotFoundException -> 0x0171 }
                    android.content.pm.ApplicationInfo r2 = r4.getApplicationInfo(r3, r2)     // Catch:{ NameNotFoundException -> 0x0171 }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.util.HashMap r4 = r4.mWifiPackageInfoList     // Catch:{ NameNotFoundException -> 0x0171 }
                    if (r4 == 0) goto L_0x0142
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.util.HashMap r4 = r4.mWifiPackageInfoList     // Catch:{ NameNotFoundException -> 0x0171 }
                    int r7 = r2.uid     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.lang.Integer r7 = java.lang.Integer.valueOf(r7)     // Catch:{ NameNotFoundException -> 0x0171 }
                    boolean r4 = r4.containsKey(r7)     // Catch:{ NameNotFoundException -> 0x0171 }
                    if (r4 == 0) goto L_0x0142
                    boolean r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.DBG     // Catch:{ NameNotFoundException -> 0x0171 }
                    if (r4 == 0) goto L_0x0170
                    java.lang.String r4 = "ACTION_PACKAGE_ADDED - exist"
                    android.util.Log.d(r6, r4)     // Catch:{ NameNotFoundException -> 0x0171 }
                    goto L_0x0170
                L_0x0142:
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x0171 }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor$PackageUpdateHandler r4 = r4.mPackageUpdateHandler     // Catch:{ NameNotFoundException -> 0x0171 }
                    com.android.server.wifi.tcp.WifiTransportLayerMonitor r7 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.this     // Catch:{ NameNotFoundException -> 0x0171 }
                    r8 = 3
                    int r9 = r2.uid     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.lang.String r10 = r2.packageName     // Catch:{ NameNotFoundException -> 0x0171 }
                    android.os.Message r5 = r7.obtainMessage(r8, r9, r5, r10)     // Catch:{ NameNotFoundException -> 0x0171 }
                    r4.sendMessage(r5)     // Catch:{ NameNotFoundException -> 0x0171 }
                    boolean r4 = com.android.server.wifi.tcp.WifiTransportLayerMonitor.DBG     // Catch:{ NameNotFoundException -> 0x0171 }
                    if (r4 == 0) goto L_0x0170
                    java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ NameNotFoundException -> 0x0171 }
                    r4.<init>()     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.lang.String r5 = "ACTION_PACKAGE_ADDED - "
                    r4.append(r5)     // Catch:{ NameNotFoundException -> 0x0171 }
                    r4.append(r3)     // Catch:{ NameNotFoundException -> 0x0171 }
                    java.lang.String r4 = r4.toString()     // Catch:{ NameNotFoundException -> 0x0171 }
                    android.util.Log.d(r6, r4)     // Catch:{ NameNotFoundException -> 0x0171 }
                L_0x0170:
                    goto L_0x0175
                L_0x0171:
                    r2 = move-exception
                    r2.printStackTrace()
                L_0x0175:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.C05753.onReceive(android.content.Context, android.content.Intent):void");
            }
        };
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageReceiver, packageFilter);
    }

    private void loadInfoFromFile() {
        HashMap<String, WifiApInfo> hashMap;
        if (DBG) {
            Log.d(TAG, "loadInfoFromFile");
        }
        synchronized (this.mWifiPackageInfoLock) {
            this.mWifiPackageInfoList = this.mWifiTransportLayerFileManager.loadWifiPackageInfoFromFile();
        }
        this.mWifiApInfoList = this.mWifiTransportLayerFileManager.loadWifiApInfoFromFile();
        if (DBG && (hashMap = this.mWifiApInfoList) != null && !hashMap.isEmpty()) {
            for (WifiApInfo info : this.mWifiApInfoList.values()) {
                Log.d(TAG, "loadInfoFromFile - AP - " + info.toString());
            }
        }
        this.mWifiSwitchEnabledUidList = this.mWifiTransportLayerFileManager.loadSwitchEnabledUidListFromFile();
    }

    /* access modifiers changed from: private */
    public void saveWifiPackageInfoList() {
        if (DBG) {
            Log.d(TAG, "saveWifiPackageInfoList");
        }
        synchronized (this.mWifiPackageInfoLock) {
            this.mWifiTransportLayerFileManager.saveWifiPackageInfoToFile(this.mWifiPackageInfoList);
        }
    }

    private void saveWifiApInfoList() {
        if (DBG) {
            Log.d(TAG, "saveWifiApInfoList");
        }
        this.mWifiTransportLayerFileManager.saveWifiApInfoToFile(this.mWifiApInfoList);
    }

    private void saveWifiSwitchabledAppList() {
        if (DBG) {
            Log.d(TAG, "saveWifiSwitchableAppList");
        }
        this.mWifiTransportLayerFileManager.saveSwitchEnabledUidListToFile(this.mWifiSwitchEnabledUidList);
    }

    public int getUsageStatsCurrentUid() {
        return this.mUsageStatsUid;
    }

    public String getUsageStatsCurrentPackageName() {
        return this.mUsageStatsPackageName;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0026, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.String getWifiPackageInfoCategory(int r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mWifiPackageInfoLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0028 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0028 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0028 }
            java.lang.Object r1 = r1.get(r2)     // Catch:{ all -> 0x0028 }
            com.android.server.wifi.tcp.WifiPackageInfo r1 = (com.android.server.wifi.tcp.WifiPackageInfo) r1     // Catch:{ all -> 0x0028 }
            java.lang.String r1 = r1.getCategory()     // Catch:{ all -> 0x0028 }
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            return r1
        L_0x0025:
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            r0 = 0
            return r0
        L_0x0028:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.getWifiPackageInfoCategory(int):java.lang.String");
    }

    /* access modifiers changed from: private */
    public void updateWifiPackageInfoCategory(int uid, String category) {
        if (DBG) {
            Log.d(TAG, "updateWifiPackageInfoCategory - " + uid + ", " + category);
        }
        synchronized (this.mWifiPackageInfoLock) {
            if (this.mWifiPackageInfoList != null && this.mWifiPackageInfoList.containsKey(Integer.valueOf(uid))) {
                this.mWifiPackageInfoList.get(Integer.valueOf(uid)).setCategory(category);
                if (this.mCurrentWifiPackageInfo.getUid() == uid) {
                    this.mCurrentWifiPackageInfo.setCategory(category);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateWifiPackageInfoCategoryFailHistory(int uid) {
        if (DBG) {
            Log.d(TAG, "updateWifiPackageInfoCategoryFailHistory - " + uid);
        }
        synchronized (this.mWifiPackageInfoLock) {
            if (this.mWifiPackageInfoList != null && this.mWifiPackageInfoList.containsKey(Integer.valueOf(uid))) {
                this.mWifiPackageInfoList.get(Integer.valueOf(uid)).addCategoryUpdateFailCount();
                if (this.mWifiPackageInfoList.get(Integer.valueOf(uid)).getCategoryUpdateFailCount() > 3) {
                    this.mWifiPackageInfoList.get(Integer.valueOf(uid)).setCategory(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_FAILED);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateWifiPackageInfoList(ArrayList<WifiPackageInfo> list) {
        if (DBG) {
            Log.d(TAG, "updateWifiPackageInfoList");
        }
        if (list != null) {
            synchronized (this.mWifiPackageInfoLock) {
                Iterator<WifiPackageInfo> it = list.iterator();
                while (it.hasNext()) {
                    WifiPackageInfo info = it.next();
                    if (this.mWifiPackageInfoList != null && !this.mWifiPackageInfoList.containsKey(Integer.valueOf(info.getUid()))) {
                        this.mWifiPackageInfoList.put(Integer.valueOf(info.getUid()), info);
                    }
                }
            }
            saveWifiPackageInfoList();
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0060, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateWifiPackageInfo(com.android.server.wifi.tcp.WifiPackageInfo r4, boolean r5) {
        /*
            r3 = this;
            boolean r0 = DBG
            if (r0 == 0) goto L_0x0026
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "updateWifiPackageInfo - "
            r0.append(r1)
            int r1 = r4.getUid()
            r0.append(r1)
            java.lang.String r1 = ", "
            r0.append(r1)
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "WifiTransportLayerMonitor"
            android.util.Log.d(r1, r0)
        L_0x0026:
            java.lang.Object r0 = r3.mWifiPackageInfoLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0061 }
            if (r1 == 0) goto L_0x005f
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0061 }
            int r2 = r4.getUid()     // Catch:{ all -> 0x0061 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)     // Catch:{ all -> 0x0061 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0061 }
            if (r1 == 0) goto L_0x004f
            if (r5 == 0) goto L_0x004d
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0061 }
            int r2 = r4.getUid()     // Catch:{ all -> 0x0061 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)     // Catch:{ all -> 0x0061 }
            r1.remove(r2)     // Catch:{ all -> 0x0061 }
            goto L_0x004f
        L_0x004d:
            monitor-exit(r0)     // Catch:{ all -> 0x0061 }
            return
        L_0x004f:
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0061 }
            int r2 = r4.getUid()     // Catch:{ all -> 0x0061 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)     // Catch:{ all -> 0x0061 }
            r1.put(r2, r4)     // Catch:{ all -> 0x0061 }
            r3.saveWifiPackageInfoList()     // Catch:{ all -> 0x0061 }
        L_0x005f:
            monitor-exit(r0)     // Catch:{ all -> 0x0061 }
            return
        L_0x0061:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0061 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.updateWifiPackageInfo(com.android.server.wifi.tcp.WifiPackageInfo, boolean):void");
    }

    /* access modifiers changed from: private */
    public WifiPackageInfo getOrCreatePackageInfo(int uid, String packageName) {
        if (DBG) {
            Log.d(TAG, "getOrCreatePackageInfo - " + uid + " " + packageName);
        }
        WifiPackageInfo info = getWifiPackageInfo(uid);
        if (info == null) {
            Log.d(TAG, "getOrCreatePackageInfo - create new info");
            return createWifiPackageInfo(uid, packageName);
        } else if (!info.isSystemApp() && !info.getPackageName().equals(packageName)) {
            Log.d(TAG, "getOrCreatePackageInfo - invalid packageName");
            WifiPackageInfo info2 = createWifiPackageInfo(uid, packageName);
            updateWifiPackageInfo(info2, true);
            return info2;
        } else if (this.mPackageUpdateHandler == null || !info.getCategory().equals(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE)) {
            return info;
        } else {
            this.mPackageUpdateHandler.sendMessage(obtainMessage(2, uid, 0, packageName));
            return info;
        }
    }

    /* access modifiers changed from: private */
    public WifiPackageInfo createWifiPackageInfo(int uid, String packageName) {
        WifiPackageInfo info = new WifiPackageInfo(this.mContext, uid, packageName);
        updateWifiPackageInfo(info, true);
        if (this.mPackageUpdateHandler != null && info.getPackageName().equals(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE)) {
            this.mPackageUpdateHandler.sendMessage(obtainMessage(2, uid, 0, packageName));
        }
        return info;
    }

    /* access modifiers changed from: private */
    public void removeWifiPackageInfo(int uid) {
        if (DBG) {
            Log.d(TAG, "removeWifiPackageInfo - " + uid);
        }
        synchronized (this.mWifiPackageInfoLock) {
            if (this.mWifiPackageInfoList != null && this.mWifiPackageInfoList.containsKey(Integer.valueOf(uid))) {
                this.mWifiPackageInfoList.remove(Integer.valueOf(uid));
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0027, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.android.server.wifi.tcp.WifiPackageInfo getWifiPackageInfo(int r5) {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mWifiPackageInfoLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r4.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0026
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r4.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r5)     // Catch:{ all -> 0x0029 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0026
            com.android.server.wifi.tcp.WifiPackageInfo r1 = new com.android.server.wifi.tcp.WifiPackageInfo     // Catch:{ all -> 0x0029 }
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r2 = r4.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            java.lang.Integer r3 = java.lang.Integer.valueOf(r5)     // Catch:{ all -> 0x0029 }
            java.lang.Object r2 = r2.get(r3)     // Catch:{ all -> 0x0029 }
            com.android.server.wifi.tcp.WifiPackageInfo r2 = (com.android.server.wifi.tcp.WifiPackageInfo) r2     // Catch:{ all -> 0x0029 }
            r1.<init>(r2)     // Catch:{ all -> 0x0029 }
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            return r1
        L_0x0026:
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            r0 = 0
            return r0
        L_0x0029:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.getWifiPackageInfo(int):com.android.server.wifi.tcp.WifiPackageInfo");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0026, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isSwitchableApp(int r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mWifiPackageInfoLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0028 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0028 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0028 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0028 }
            java.lang.Object r1 = r1.get(r2)     // Catch:{ all -> 0x0028 }
            com.android.server.wifi.tcp.WifiPackageInfo r1 = (com.android.server.wifi.tcp.WifiPackageInfo) r1     // Catch:{ all -> 0x0028 }
            boolean r1 = r1.isSwitchable()     // Catch:{ all -> 0x0028 }
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            return r1
        L_0x0025:
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            r0 = 0
            return r0
        L_0x0028:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0028 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.isSwitchableApp(int):boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0026, code lost:
        return "";
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.String getPackageName(int r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mWifiPackageInfoLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0029 }
            boolean r1 = r1.containsKey(r2)     // Catch:{ all -> 0x0029 }
            if (r1 == 0) goto L_0x0025
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.tcp.WifiPackageInfo> r1 = r3.mWifiPackageInfoList     // Catch:{ all -> 0x0029 }
            java.lang.Integer r2 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0029 }
            java.lang.Object r1 = r1.get(r2)     // Catch:{ all -> 0x0029 }
            com.android.server.wifi.tcp.WifiPackageInfo r1 = (com.android.server.wifi.tcp.WifiPackageInfo) r1     // Catch:{ all -> 0x0029 }
            java.lang.String r1 = r1.getPackageName()     // Catch:{ all -> 0x0029 }
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            return r1
        L_0x0025:
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            java.lang.String r0 = ""
            return r0
        L_0x0029:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0029 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerMonitor.getPackageName(int):java.lang.String");
    }

    public WifiPackageInfo getCurrentPackageInfo() {
        try {
            String focusedPackageName = ActivityManager.getService().getFocusedStackInfo().topActivity.getPackageName();
            if (!this.mUsageStatsPackageName.equals(focusedPackageName)) {
                if (DBG) {
                    Log.d(TAG, "getCurrentPackageInfo package changed: " + focusedPackageName);
                }
                this.mUsageStatsUid = getPackageManager().getApplicationInfo(focusedPackageName, 128).uid;
                this.mUsageStatsPackageName = focusedPackageName;
                this.mCurrentWifiPackageInfo = getOrCreatePackageInfo(this.mUsageStatsUid, this.mUsageStatsPackageName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (DBG) {
                Log.w(TAG, "getCurrentPackageInfo - NameNotFoundException");
            }
            e.printStackTrace();
        } catch (RemoteException e2) {
            if (DBG) {
                Log.w(TAG, "getCurrentPackageInfo - RemoteException");
            }
            e2.printStackTrace();
        } catch (Exception e3) {
            if (DBG) {
                Log.w(TAG, "getCurrentPackageInfo - Exception");
            }
            e3.printStackTrace();
        }
        return this.mCurrentWifiPackageInfo;
    }

    public WifiApInfo getWifiApInfo(String ssid) {
        if (DBG) {
            Log.d(TAG, "getWifiApInfo - " + ssid);
        }
        HashMap<String, WifiApInfo> hashMap = this.mWifiApInfoList;
        if (hashMap == null || !hashMap.containsKey(ssid)) {
            return null;
        }
        return this.mWifiApInfoList.get(ssid);
    }

    public void updateWifiApInfo(WifiApInfo info) {
        if (DBG) {
            Log.d(TAG, "updateWifiApInfo - " + info);
        }
        HashMap<String, WifiApInfo> hashMap = this.mWifiApInfoList;
        if (hashMap != null) {
            if (hashMap.containsKey(info.getSsid())) {
                this.mWifiApInfoList.remove(info.getSsid());
            }
            this.mWifiApInfoList.put(info.getSsid(), info);
            saveWifiApInfoList();
        }
    }

    public WifiApInfo setSsid(String ssid) {
        if (DBG) {
            Log.d(TAG, "setSsid");
        }
        this.mCurrentWifiApInfo = getWifiApInfo(ssid);
        if (this.mCurrentWifiApInfo == null) {
            this.mCurrentWifiApInfo = new WifiApInfo(ssid);
            updateWifiApInfo(this.mCurrentWifiApInfo);
        }
        return this.mCurrentWifiApInfo;
    }

    public WifiApInfo getCurrentWifiApInfo() {
        if (DBG) {
            Log.d(TAG, "getCurrentWifiApInfo");
        }
        return this.mCurrentWifiApInfo;
    }

    public void resetSwitchForIndivdiaulAppsDetectionCount(String packageName) {
        if (DBG) {
            Log.d(TAG, "resetSwitchForIndivdiaulAppsDetectionCount - " + packageName);
        }
        HashMap<String, WifiApInfo> hashMap = this.mWifiApInfoList;
        if (hashMap != null) {
            for (WifiApInfo info : hashMap.values()) {
                info.resetSwitchForIndivdiaulAppsDetectionCount(packageName);
            }
        }
        saveWifiApInfoList();
    }

    public boolean isSwitchEnabledApp(int uid) {
        return this.mWifiSwitchEnabledUidList.contains(Integer.valueOf(uid));
    }

    public void enableSwitchEnabledAppInfo(int uid) {
        updateSwitchEnabledAppInfo(uid, true);
    }

    private void updateSwitchEnabledAppInfo(int uid, boolean enable) {
        if (DBG) {
            Log.d(TAG, "updateSwitchEnabledAppInfo - " + uid + " " + enable);
        }
        if (enable) {
            ArrayList<Integer> arrayList = this.mWifiSwitchEnabledUidList;
            if (arrayList != null && !arrayList.contains(Integer.valueOf(uid))) {
                this.mWifiSwitchEnabledUidList.add(Integer.valueOf(uid));
                saveWifiSwitchabledAppList();
                return;
            }
            return;
        }
        ArrayList<Integer> arrayList2 = this.mWifiSwitchEnabledUidList;
        if (arrayList2 != null && arrayList2.contains(Integer.valueOf(uid))) {
            ArrayList<Integer> arrayList3 = this.mWifiSwitchEnabledUidList;
            arrayList3.remove(arrayList3.indexOf(Integer.valueOf(uid)));
            saveWifiSwitchabledAppList();
        }
        resetDetectionHistory(uid);
    }

    public void updateSwitchEnabledAppList(ArrayList<Integer> list) {
        if (DBG) {
            Log.d(TAG, "updateSwitchEnabledAppList - " + list);
        }
        if (list != null) {
            Iterator<Integer> it = this.mWifiSwitchEnabledUidList.iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                if (!list.contains(Integer.valueOf(uid))) {
                    if (DBG) {
                        Log.d(TAG, "updateSwitchEnabledAppList - delete " + uid);
                    }
                    resetDetectionHistory(uid);
                    resetSwitchForIndivdiaulAppsDetectionCount(getPackageName(uid));
                }
            }
            Iterator<Integer> it2 = list.iterator();
            while (it2.hasNext()) {
                int uid2 = it2.next().intValue();
                if (!this.mWifiSwitchEnabledUidList.contains(Integer.valueOf(uid2)) && DBG) {
                    Log.d(TAG, "updateSwitchEnabledAppList - insert " + uid2);
                }
            }
            this.mWifiSwitchEnabledUidList = list;
            saveWifiSwitchabledAppList();
            saveWifiApInfoList();
        }
    }

    public void addWifiPackageDetectedCount(int uid) {
        synchronized (this.mWifiPackageInfoLock) {
            if (this.mWifiPackageInfoList != null && this.mWifiPackageInfoList.containsKey(Integer.valueOf(uid))) {
                this.mWifiPackageInfoList.get(Integer.valueOf(uid)).setDetectedCount(this.mWifiPackageInfoList.get(Integer.valueOf(uid)).getDetectedCount() + 1);
                saveWifiPackageInfoList();
            }
        }
    }

    private void resetDetectionHistory(int uid) {
        synchronized (this.mWifiPackageInfoLock) {
            if (this.mWifiPackageInfoList != null && this.mWifiPackageInfoList.containsKey(Integer.valueOf(uid))) {
                this.mWifiPackageInfoList.get(Integer.valueOf(uid)).setDetectedCount(0);
            }
        }
        HashMap<String, WifiApInfo> hashMap = this.mWifiApInfoList;
        if (hashMap != null && !hashMap.isEmpty()) {
            String packageName = getPackageName(uid);
            for (WifiApInfo apInfo : this.mWifiApInfoList.values()) {
                if (apInfo.getDetectedPackageList().containsKey(packageName)) {
                    apInfo.getDetectedPackageList().remove(packageName);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isCategoryUpdateable() {
        if (DBG) {
            Log.d(TAG, "isCategoryUpdateable - " + isNetworkConnected() + ", " + getCountryCode());
        }
        return isNetworkConnected() && !"CN".equalsIgnoreCase(getCountryCode());
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private String getCountryCode() {
        try {
            String deviceCountryCode = SemCscFeature.getInstance().getString("CountryISO");
            if (deviceCountryCode != null) {
                return deviceCountryCode;
            }
            return " ";
        } catch (Exception e) {
            return " ";
        }
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[SWITCH ENABLED PACKAGE INFO]\n");
        ArrayList<Integer> arrayList = this.mWifiSwitchEnabledUidList;
        if (arrayList != null && !arrayList.isEmpty()) {
            int index = 0;
            Iterator<Integer> it = this.mWifiSwitchEnabledUidList.iterator();
            while (it.hasNext()) {
                int uid = it.next().intValue();
                StringBuilder sb2 = new StringBuilder();
                sb2.append("[INDEX] ");
                int index2 = index + 1;
                sb2.append(index);
                sb.append(sb2.toString());
                sb.append(", [UID] " + uid);
                sb.append(", [PACKAGE] " + getPackageName(uid) + "\n");
                index = index2;
            }
        }
        sb.append("\n\n[AP INFO]\n");
        HashMap<String, WifiApInfo> hashMap = this.mWifiApInfoList;
        if (hashMap == null || hashMap.isEmpty()) {
            sb.append("EMTPY\n");
        } else {
            int index3 = 0;
            for (WifiApInfo info : this.mWifiApInfoList.values()) {
                if (info.getSwitchForIndivdiaulAppsDetectionCount() > 0) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("[INDEX] ");
                    int index4 = index3 + 1;
                    sb3.append(index3);
                    sb.append(sb3.toString());
                    sb.append(", [SSID] " + info.getSsid());
                    sb.append(", [ConnectionCount] " + info.getAccumulatedConnectionCount());
                    sb.append(", [ConnectionTime] " + info.getAccumulatedConnectionTime());
                    sb.append(", [DetectionCount] " + info.getSwitchForIndivdiaulAppsDetectionCount() + "\n");
                    HashMap<String, WifiApInfo.DetectedPackageInfo> detectedList = info.getDetectedPackageList();
                    if (detectedList != null && !detectedList.isEmpty()) {
                        for (WifiApInfo.DetectedPackageInfo packageInfo : detectedList.values()) {
                            sb.append("  [DetectedPackage] " + packageInfo.getPackageName());
                            sb.append(", [LastDetectedTime] " + packageInfo.getLastDetectedTime());
                            sb.append(", [DetectedCount] " + packageInfo.getDetectedCount());
                            sb.append(", [PackageNormalOperationTime] " + packageInfo.getPackageNormalOperationTime() + "\n");
                        }
                    }
                    index3 = index4;
                }
            }
        }
        sb.append("\n\n[PACKAGE INFO]\n");
        HashMap<Integer, WifiPackageInfo> hashMap2 = this.mWifiPackageInfoList;
        if (hashMap2 != null && !hashMap2.isEmpty()) {
            synchronized (this.mWifiPackageInfoLock) {
                int index5 = 0;
                int detectionMode = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_switch_for_individual_apps_detection_mode", 0);
                for (WifiPackageInfo info2 : this.mWifiPackageInfoList.values()) {
                    if ((detectionMode == 0 && info2.isChatApp()) || (detectionMode == 1 && info2.isSwitchable())) {
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("[INDEX] ");
                        int index6 = index5 + 1;
                        sb4.append(index5);
                        sb.append(sb4.toString());
                        sb.append(", [UID] " + info2.getUid());
                        sb.append(", [PackageName] " + info2.getPackageName());
                        sb.append(", [Switchable] " + info2.isSwitchable());
                        sb.append(", [Category] " + info2.getCategory());
                        sb.append(", [DetectedCount] " + info2.getDetectedCount());
                        sb.append(", [BrowsingApp] " + info2.isBrowsingApp());
                        sb.append(", [ChatApp] " + info2.isChatApp());
                        sb.append(", [GamingApp] " + info2.isGamingApp());
                        sb.append(", [Launchable] " + info2.isLaunchable());
                        sb.append(", [SystemApp] " + info2.isSystemApp());
                        sb.append(", [Voip] " + info2.isVoip());
                        sb.append(", [UsagePattern] " + info2.getUsagePattern() + "\n");
                        index5 = index6;
                    }
                }
            }
        }
        return sb.toString();
    }
}
