package com.android.server.wifi;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.wifi.WifiGeofenceDBHelper;
import com.android.server.wifi.util.TelephonyUtil;
import com.samsung.android.location.SemGeofence;
import com.samsung.android.location.SemLocationManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiGeofenceManager {
    private static final String TAG = "WifiGeofenceManager";
    private static final String WIFI_INTENT_ACTION_GEOFENCE_STATE = "com.sec.android.wifi.geofence.state";
    private static final String WIFI_INTENT_ACTION_GEOFENCE_TRIGGERED = "com.sec.android.wifi.GEOFENCE_TRIGGERED";
    /* access modifiers changed from: private */
    public static final Object mGeofenceLock = new Object();
    /* access modifiers changed from: private */
    public boolean DBG = true;
    private ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final WifiGeofenceDBHelper mGeofenceDBHelper;
    /* access modifiers changed from: private */
    public final HashMap<Integer, WifiGeofenceDBHelper.WifiGeofenceData> mGeofenceDataList;
    private final WifiGeofenceLogManager mGeofenceLogManager;
    private final boolean mGeofenceManagerEnabled;
    /* access modifiers changed from: private */
    public boolean mGeofenceStateByAnotherPackage;
    private boolean mInRange = true;
    private boolean mIntializedGeofence = false;
    public int mLastGeofenceState = -1;
    private final Looper mLooper;
    private NetworkInfo mNetworkInfo;
    private HashMap<String, Integer> mNotExistAPCheckMap = new HashMap<>();
    /* access modifiers changed from: private */
    public SemLocationManager mSLocationManager;
    private int mScanInterval;
    private int mScanMaxInterval;
    /* access modifiers changed from: private */
    public final Handler mStartGeofenceHandler;
    final Runnable mStartGeofenceThread = new Runnable() {
        public void run() {
            try {
                WifiGeofenceManager.this.initGeofence();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private TelephonyManager mTm;
    private boolean mTriggerStartLearning = false;
    private final WifiConfigManager mWifiConfigManager;
    private WifiConnectivityManager mWifiConnectivityManager;
    private ArrayList<WifiGeofenceStateListener> mWifiGeofenceListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public final AtomicInteger mWifiState = new AtomicInteger(1);

    public interface WifiGeofenceStateListener {
        void onGeofenceStateChanged(int i, List<String> list);
    }

    public WifiGeofenceManager(Context context, Looper looper, WifiConfigManager wifiConfigManager) {
        this.mContext = context;
        this.mLooper = looper;
        this.mWifiConfigManager = wifiConfigManager;
        this.mGeofenceDBHelper = new WifiGeofenceDBHelper(this.mContext);
        this.mGeofenceLogManager = new WifiGeofenceLogManager(this.mLooper);
        this.mGeofenceDataList = this.mGeofenceDBHelper.select();
        this.mStartGeofenceHandler = new Handler();
        this.mGeofenceManagerEnabled = true;
        if (this.mGeofenceManagerEnabled) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WIFI_INTENT_ACTION_GEOFENCE_TRIGGERED);
            intentFilter.addAction("com.samsung.android.location.SERVICE_READY");
            intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                /* JADX WARNING: Code restructure failed: missing block: B:79:?, code lost:
                    return;
                 */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void onReceive(android.content.Context r11, android.content.Intent r12) {
                    /*
                        r10 = this;
                        java.lang.StringBuilder r0 = new java.lang.StringBuilder
                        r0.<init>()
                        java.lang.String r1 = "BroadcastReceiver: "
                        r0.append(r1)
                        java.lang.String r1 = r12.getAction()
                        r0.append(r1)
                        java.lang.String r0 = r0.toString()
                        java.lang.String r1 = "WifiGeofenceManager"
                        android.util.Log.d(r1, r0)
                        java.lang.String r0 = r12.getAction()
                        java.lang.String r1 = "android.intent.action.AIRPLANE_MODE"
                        boolean r0 = r1.equals(r0)
                        r1 = 2
                        r2 = 1
                        if (r0 == 0) goto L_0x006f
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        android.content.Context r0 = r0.mContext
                        android.content.ContentResolver r0 = r0.getContentResolver()
                        r3 = 0
                        java.lang.String r4 = "airplane_mode_on"
                        int r0 = android.provider.Settings.Global.getInt(r0, r4, r3)
                        if (r0 != r2) goto L_0x003c
                        r3 = r2
                    L_0x003c:
                        r0 = r3
                        if (r0 == 0) goto L_0x004c
                        java.lang.String r1 = "WifiGeofenceManager"
                        java.lang.String r3 = "Airplain mode enabled -> Set max interval to 128s"
                        android.util.Log.d(r1, r3)
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this
                        r1.sendBroadcastForInOutRange(r2)
                        goto L_0x006d
                    L_0x004c:
                        com.android.server.wifi.WifiGeofenceManager r3 = com.android.server.wifi.WifiGeofenceManager.this
                        boolean r3 = r3.isGeofenceExit(r2)
                        if (r3 == 0) goto L_0x0061
                        java.lang.String r2 = "WifiGeofenceManager"
                        java.lang.String r3 = "Airplain mode disabled! But exit state -> Set max interval 1024s"
                        android.util.Log.d(r2, r3)
                        com.android.server.wifi.WifiGeofenceManager r2 = com.android.server.wifi.WifiGeofenceManager.this
                        r2.sendBroadcastForInOutRange(r1)
                        goto L_0x006d
                    L_0x0061:
                        java.lang.String r1 = "WifiGeofenceManager"
                        java.lang.String r3 = "Airplain mode disabled! Enter state -> Set max interval 128s"
                        android.util.Log.d(r1, r3)
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this
                        r1.sendBroadcastForInOutRange(r2)
                    L_0x006d:
                        goto L_0x022f
                    L_0x006f:
                        java.lang.String r0 = r12.getAction()
                        java.lang.String r3 = "android.intent.action.SIM_STATE_CHANGED"
                        boolean r0 = r3.equals(r0)
                        if (r0 == 0) goto L_0x00b5
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        boolean r0 = r0.isSimCardReady()
                        if (r0 == 0) goto L_0x00a7
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        boolean r0 = r0.isGeofenceExit(r2)
                        if (r0 == 0) goto L_0x0099
                        java.lang.String r0 = "WifiGeofenceManager"
                        java.lang.String r2 = "getSimState() is SIM_STATE_READY! But exit state -> Set max interval 1024s"
                        android.util.Log.d(r0, r2)
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        r0.sendBroadcastForInOutRange(r1)
                        goto L_0x022f
                    L_0x0099:
                        java.lang.String r0 = "WifiGeofenceManager"
                        java.lang.String r1 = "getSimState() is SIM_STATE_READY! Enter state -> Set max interval 128s"
                        android.util.Log.d(r0, r1)
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        r0.sendBroadcastForInOutRange(r2)
                        goto L_0x022f
                    L_0x00a7:
                        java.lang.String r0 = "WifiGeofenceManager"
                        java.lang.String r1 = "getSimState() is not SIM_STATE_READY! -> Set max interval to 128s"
                        android.util.Log.d(r0, r1)
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        r0.sendBroadcastForInOutRange(r2)
                        goto L_0x022f
                    L_0x00b5:
                        java.lang.String r0 = r12.getAction()
                        java.lang.String r3 = "com.sec.android.wifi.GEOFENCE_TRIGGERED"
                        boolean r0 = r3.equals(r0)
                        if (r0 == 0) goto L_0x01bc
                        r0 = -1
                        java.lang.String r3 = "id"
                        int r3 = r12.getIntExtra(r3, r0)
                        java.lang.String r4 = "transition"
                        int r0 = r12.getIntExtra(r4, r0)
                        java.lang.Object r4 = com.android.server.wifi.WifiGeofenceManager.mGeofenceLock
                        monitor-enter(r4)
                        com.android.server.wifi.WifiGeofenceManager r5 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        java.util.HashMap r5 = r5.mGeofenceDataList     // Catch:{ all -> 0x01b9 }
                        java.lang.Integer r6 = new java.lang.Integer     // Catch:{ all -> 0x01b9 }
                        r6.<init>(r3)     // Catch:{ all -> 0x01b9 }
                        java.lang.Object r5 = r5.get(r6)     // Catch:{ all -> 0x01b9 }
                        com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData r5 = (com.android.server.wifi.WifiGeofenceDBHelper.WifiGeofenceData) r5     // Catch:{ all -> 0x01b9 }
                        if (r5 != 0) goto L_0x00ef
                        java.lang.String r1 = "WifiGeofenceManager"
                        java.lang.String r2 = "WifiGeofenceData is null!"
                        android.util.Log.d(r1, r2)     // Catch:{ all -> 0x01b9 }
                        monitor-exit(r4)     // Catch:{ all -> 0x01b9 }
                        return
                    L_0x00ef:
                        r5.setIsGeofenceEnter(r0)     // Catch:{ all -> 0x01b9 }
                        com.android.server.wifi.WifiGeofenceManager r6 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        boolean r6 = r6.isGeofenceExit(r2)     // Catch:{ all -> 0x01b9 }
                        if (r6 == 0) goto L_0x00fc
                        r6 = r1
                        goto L_0x00fd
                    L_0x00fc:
                        r6 = r2
                    L_0x00fd:
                        com.android.server.wifi.WifiGeofenceManager r7 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        boolean r7 = r7.DBG     // Catch:{ all -> 0x01b9 }
                        if (r7 == 0) goto L_0x0130
                        java.lang.String r7 = "WifiGeofenceManager"
                        java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b9 }
                        r8.<init>()     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "id ["
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        r8.append(r3)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "], direction ["
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        r8.append(r0)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "], Result ["
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        r8.append(r6)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "]\n"
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r8 = r8.toString()     // Catch:{ all -> 0x01b9 }
                        android.util.Log.d(r7, r8)     // Catch:{ all -> 0x01b9 }
                    L_0x0130:
                        com.android.server.wifi.WifiGeofenceManager r7 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b9 }
                        r8.<init>()     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "                   [ id = "
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        r8.append(r3)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = ", direction = "
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        com.android.server.wifi.WifiGeofenceManager r9 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = r9.syncGetGeofenceStateByName(r0)     // Catch:{ all -> 0x01b9 }
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = "    Result : "
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        com.android.server.wifi.WifiGeofenceManager r9 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = r9.syncGetGeofenceStateByName(r6)     // Catch:{ all -> 0x01b9 }
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r9 = " ]"
                        r8.append(r9)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r8 = r8.toString()     // Catch:{ all -> 0x01b9 }
                        r7.addGeofenceIntentHistoricalDumpLog(r8)     // Catch:{ all -> 0x01b9 }
                        if (r0 == r2) goto L_0x018b
                        if (r0 != 0) goto L_0x016c
                        goto L_0x018b
                    L_0x016c:
                        if (r0 != r1) goto L_0x01b7
                        if (r6 != r1) goto L_0x0185
                        com.android.server.wifi.WifiGeofenceManager r2 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        boolean r2 = r2.DBG     // Catch:{ all -> 0x01b9 }
                        if (r2 == 0) goto L_0x017f
                        java.lang.String r2 = "WifiGeofenceManager"
                        java.lang.String r7 = "BroadcastReceiver() - All of AP are OUT. Increase scan max interval"
                        android.util.Log.d(r2, r7)     // Catch:{ all -> 0x01b9 }
                    L_0x017f:
                        com.android.server.wifi.WifiGeofenceManager r2 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        r2.sendBroadcastForInOutRange(r1)     // Catch:{ all -> 0x01b9 }
                        goto L_0x01b7
                    L_0x0185:
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        r1.sendBroadcastForInOutRange(r2)     // Catch:{ all -> 0x01b9 }
                        goto L_0x01b7
                    L_0x018b:
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        boolean r1 = r1.DBG     // Catch:{ all -> 0x01b9 }
                        if (r1 == 0) goto L_0x01b2
                        java.lang.String r1 = "WifiGeofenceManager"
                        java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x01b9 }
                        r2.<init>()     // Catch:{ all -> 0x01b9 }
                        java.lang.String r7 = "BroadcastReceiver() - configKey : "
                        r2.append(r7)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r7 = r5.getConfigKey()     // Catch:{ all -> 0x01b9 }
                        r2.append(r7)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r7 = " IN. Reduce scan max interval"
                        r2.append(r7)     // Catch:{ all -> 0x01b9 }
                        java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x01b9 }
                        android.util.Log.d(r1, r2)     // Catch:{ all -> 0x01b9 }
                    L_0x01b2:
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this     // Catch:{ all -> 0x01b9 }
                        r1.sendBroadcastForInOutRange(r0)     // Catch:{ all -> 0x01b9 }
                    L_0x01b7:
                        monitor-exit(r4)     // Catch:{ all -> 0x01b9 }
                        goto L_0x022e
                    L_0x01b9:
                        r1 = move-exception
                        monitor-exit(r4)     // Catch:{ all -> 0x01b9 }
                        throw r1
                    L_0x01bc:
                        java.lang.String r0 = r12.getAction()
                        java.lang.String r1 = "com.samsung.android.location.SERVICE_READY"
                        boolean r0 = r1.equals(r0)
                        if (r0 == 0) goto L_0x022e
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        android.content.Context r1 = r0.mContext
                        java.lang.String r2 = "sec_location"
                        java.lang.Object r1 = r1.getSystemService(r2)
                        com.samsung.android.location.SemLocationManager r1 = (com.samsung.android.location.SemLocationManager) r1
                        com.samsung.android.location.SemLocationManager unused = r0.mSLocationManager = r1
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        com.samsung.android.location.SemLocationManager r0 = r0.mSLocationManager
                        if (r0 == 0) goto L_0x020b
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        boolean r0 = r0.DBG
                        if (r0 == 0) goto L_0x01f0
                        java.lang.String r0 = "WifiGeofenceManager"
                        java.lang.String r1 = "mSLocationManager is ready"
                        android.util.Log.d(r0, r1)
                    L_0x01f0:
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        com.android.server.wifi.WifiGeofenceDBHelper r0 = r0.mGeofenceDBHelper
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this
                        com.samsung.android.location.SemLocationManager r1 = r1.mSLocationManager
                        r0.setSLocationManager(r1)
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        java.lang.String r1 = "syncGeofence ()  ,  mSLocationManager.ACTION_SERVICE_READY !! "
                        r0.addGeofenceIntentHistoricalDumpLog(r1)
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        r0.deleteNotExistAPFromSlocation()
                    L_0x020b:
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        java.util.concurrent.atomic.AtomicInteger r0 = r0.mWifiState
                        int r0 = r0.get()
                        r1 = 3
                        if (r0 == r1) goto L_0x0220
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        boolean r0 = r0.mGeofenceStateByAnotherPackage
                        if (r0 == 0) goto L_0x022f
                    L_0x0220:
                        com.android.server.wifi.WifiGeofenceManager r0 = com.android.server.wifi.WifiGeofenceManager.this
                        android.os.Handler r0 = r0.mStartGeofenceHandler
                        com.android.server.wifi.WifiGeofenceManager r1 = com.android.server.wifi.WifiGeofenceManager.this
                        java.lang.Runnable r1 = r1.mStartGeofenceThread
                        r0.post(r1)
                        goto L_0x022f
                    L_0x022e:
                    L_0x022f:
                        return
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiGeofenceManager.C04561.onReceive(android.content.Context, android.content.Intent):void");
                }
            }, intentFilter);
        }
    }

    private void sendBroadcastForGeofenceState(boolean state) {
        if (!this.DBG) {
            Intent intent = new Intent(WIFI_INTENT_ACTION_GEOFENCE_STATE);
            intent.putExtra("state", state);
            Context context = this.mContext;
            if (context != null) {
                try {
                    context.sendBroadcastAsUser(intent, UserHandle.ALL);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Send broadcast before boot - action:" + intent.getAction());
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public String syncGetGeofenceStateByName(int direction) {
        if (direction == 0) {
            return "UNKNOWN";
        }
        if (direction == 1) {
            return "ENTER  ";
        }
        if (direction != 2) {
            return "[invalid geofence state]";
        }
        return "EXIT   ";
    }

    /* access modifiers changed from: private */
    public void addGeofenceIntentHistoricalDumpLog(String log) {
        Message msg = Message.obtain();
        msg.obj = log;
        this.mGeofenceLogManager.sendMessage(msg);
    }

    /* access modifiers changed from: package-private */
    public void setScanInterval(int interval, int maxInterval) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "setScanInterval() - GeofenceManager is disabled");
            return;
        }
        if (this.DBG) {
            Log.e(TAG, "setScanInterval interval : " + interval + ", maxInterval : " + maxInterval);
        }
        this.mScanInterval = interval;
        this.mScanMaxInterval = maxInterval;
    }

    /* access modifiers changed from: package-private */
    public void notifyWifiState(int wifiState) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "setWifiState() - GeofenceManager is disabled");
        } else {
            this.mWifiState.set(wifiState);
        }
    }

    public String getGeofenceInformation() {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "getGeofenceInformation() - GeofenceManager is disabled");
            return "GeofenceManager is not supported";
        }
        int isExit = 1;
        if (isGeofenceExit(true)) {
            isExit = 2;
        }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Current geofence state : ");
        sbuf.append(syncGetGeofenceStateByName(isExit));
        sbuf.append("\n");
        if (this.mIntializedGeofence) {
            synchronized (mGeofenceLock) {
                if (this.mGeofenceDataList.size() > 0) {
                    sbuf.append("called initGeofence()");
                    sbuf.append("\n");
                } else {
                    sbuf.append("called initGeofence(), saved GeofenceAP is 0");
                    sbuf.append("\n");
                }
            }
        } else {
            sbuf.append("called deinitGeofence()");
            sbuf.append("\n");
        }
        if (this.mTriggerStartLearning) {
            sbuf.append("mTriggerStartLearning");
            sbuf.append("\n");
        } else {
            sbuf.append("mTriggerStopLearning");
            sbuf.append("\n");
        }
        sbuf.append("Scan Interval (now/max):");
        sbuf.append(this.mScanInterval);
        sbuf.append("/");
        sbuf.append(this.mScanMaxInterval);
        sbuf.append("\n");
        sbuf.append("Geofence Details:\n");
        synchronized (mGeofenceLock) {
            for (Integer intValue : this.mGeofenceDataList.keySet()) {
                int locationId = intValue.intValue();
                WifiGeofenceDBHelper.WifiGeofenceData data = this.mGeofenceDataList.get(Integer.valueOf(locationId));
                sbuf.append("id:");
                sbuf.append(data.getLocationId());
                sbuf.append(" st:");
                sbuf.append(data.getGeofenceStateToString());
                sbuf.append(" key:");
                sbuf.append(data.getConfigKey());
                sbuf.append(" cellcount:");
                sbuf.append(data.getCellCount(locationId));
                sbuf.append("\n");
            }
        }
        return sbuf.toString();
    }

    private int addGeofence(WifiConfiguration currentConfig) {
        int locationId;
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "addGeofence() - GeofenceManager is disabled");
            return -1;
        } else if (this.mSLocationManager == null || currentConfig == null) {
            return -1;
        } else {
            long nowMs = System.currentTimeMillis();
            if (this.mGeofenceDataList.size() >= 100) {
                int candidateId = this.mGeofenceDBHelper.getLocationIdFromOldTime();
                Log.d(TAG, "addGeofence() -  candidate id for delete : " + candidateId);
                if (candidateId < 0) {
                    return -1;
                }
                removeGeofence(this.mGeofenceDataList.get(Integer.valueOf(candidateId)).getConfigKey());
            }
            SemGeofence param = new SemGeofence(4, (String) null);
            int locationId2 = this.mSLocationManager.addGeofence(param);
            if (locationId2 < 0) {
                locationId = this.mSLocationManager.addGeofence(param);
            } else {
                locationId = locationId2;
            }
            addGeofenceIntentHistoricalDumpLog("addGeofence()    - [ locationId : " + locationId + " ], [ configKey : " + currentConfig.configKey() + " ]");
            if (this.DBG) {
                Log.d(TAG, "addGeofence() - [ configKey : " + currentConfig.configKey() + " ], [ locationId : " + locationId + " ]");
            }
            synchronized (mGeofenceLock) {
                if (locationId <= 0) {
                    return -1;
                }
                this.mGeofenceDBHelper.insert(locationId, currentConfig.networkId, currentConfig.configKey(), currentConfig.BSSID, nowMs);
                startGeofence(locationId);
                return locationId;
            }
        }
    }

    private void removeGeofence(String configKey) {
        WifiConfiguration conf;
        if (this.DBG) {
            Log.d(TAG, "removeGeofence() - Enter !!");
        }
        if (!this.mGeofenceManagerEnabled) {
            if (this.DBG) {
                Log.d(TAG, "removeGeofence() - GeofenceManager is disabled");
            }
        } else if (this.mSLocationManager != null && configKey != null) {
            int locationId = isFindLocationId(configKey);
            if (locationId > 0) {
                NetworkInfo networkInfo = this.mNetworkInfo;
                if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && (conf = this.mWifiConfigManager.getConfiguredNetwork(configKey)) != null && conf.configKey().equals(configKey)) {
                    if (this.DBG) {
                        Log.d(TAG, "removeGeofence() - stopLearning(" + locationId + ")");
                    }
                    int mResult = this.mSLocationManager.stopLearning(locationId);
                    if (mResult < 0) {
                        if (this.DBG) {
                            Log.d(TAG, "removeGeofence()- id : " + locationId + ", stopLearning() - ERROR !!, mResult : " + mResult + "");
                        }
                        addGeofenceIntentHistoricalDumpLog("removeGeofence() - [ locationId : " + locationId + " ], ERROR !!, mResult : " + mResult);
                    }
                }
                if (this.mWifiState.get() == 3) {
                    stopGeofence(locationId);
                }
                synchronized (mGeofenceLock) {
                    this.mGeofenceDBHelper.delete(configKey);
                    int mResult2 = this.mSLocationManager.removeGeofence(locationId);
                    if (mResult2 < 0) {
                        if (this.DBG) {
                            Log.d(TAG, "removeGeofence()- id : " + locationId + ", mSLocationManager.removeGeofence() ERROR !!, mResult : " + mResult2 + "");
                        }
                        addGeofenceIntentHistoricalDumpLog("removeGeofence() - [ locationId : " + locationId + " ], mSLocationManager.removeGeofence ERROR !!, mResult : " + mResult2);
                    } else {
                        addGeofenceIntentHistoricalDumpLog("removeGeofence() - [ locationId : " + locationId + " ], [ configKey : " + configKey + " ]");
                        if (this.DBG) {
                            Log.e(TAG, "removeGeofence() - [ configKey : " + configKey + " ], [ locationId : " + locationId + " ]");
                        }
                    }
                }
            }
            if (this.DBG) {
                Log.d(TAG, "removeGeofence() - Exit !!");
            }
        }
    }

    public void initGeofence() {
        Log.i(TAG, "initGeofence");
        if (this.mIntializedGeofence) {
            Log.d(TAG, "init geofence, already initialized");
        } else if (this.mSLocationManager == null) {
            Log.e(TAG, "SLocation service is not ready");
        } else {
            synchronized (mGeofenceLock) {
                for (Integer intValue : this.mGeofenceDataList.keySet()) {
                    startGeofence(intValue.intValue());
                }
            }
            deleteNotExistAP();
            this.mIntializedGeofence = true;
        }
    }

    public void deinitGeofence() {
        if (this.DBG) {
            Log.i(TAG, "deinitGeofence");
        }
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "deinitGeofence() - GeofenceManager is disabled");
        } else if (!this.mIntializedGeofence) {
            Log.d(TAG, "deinit geofence, alread deinitialized");
        } else {
            synchronized (mGeofenceLock) {
                for (Integer intValue : this.mGeofenceDataList.keySet()) {
                    stopGeofence(intValue.intValue());
                }
            }
            this.mIntializedGeofence = false;
        }
    }

    private void startGeofence(int locationId) {
        if (this.mSLocationManager == null) {
            Log.e(TAG, "SLocation service is not ready");
            return;
        }
        Intent intent = new Intent(WIFI_INTENT_ACTION_GEOFENCE_TRIGGERED);
        intent.putExtra("id", locationId);
        int mResult = this.mSLocationManager.startGeofenceMonitoring(locationId, PendingIntent.getBroadcast(this.mContext, locationId, intent, 0));
        if (mResult < 0) {
            SemLocationManager semLocationManager = this.mSLocationManager;
            if (mResult == -3) {
                this.mNotExistAPCheckMap.put(this.mGeofenceDataList.get(Integer.valueOf(locationId)).getConfigKey(), new Integer(locationId));
            }
            if (this.DBG) {
                Log.d(TAG, "startGeofence() - id : " + locationId + ", ERROR !!, mResult : " + mResult + "");
            }
            addGeofenceIntentHistoricalDumpLog("startGeofence()  - [ locationId : " + locationId + " ], ERROR !!, mResult : " + mResult);
            return;
        }
        if (this.DBG) {
            Log.d(TAG, "startGeofence() - id : " + locationId);
        }
        addGeofenceIntentHistoricalDumpLog("startGeofence()  - [ locationId : " + locationId + " ]");
    }

    private void stopGeofence(int locationId) {
        if (this.mSLocationManager == null) {
            Log.e(TAG, "SLocation service is not ready");
            return;
        }
        Intent intent = new Intent(WIFI_INTENT_ACTION_GEOFENCE_TRIGGERED);
        intent.putExtra("id", locationId);
        int mResult = this.mSLocationManager.stopGeofenceMonitoring(locationId, PendingIntent.getBroadcast(this.mContext, locationId, intent, 0));
        if (mResult < 0) {
            if (this.DBG) {
                Log.d(TAG, " stopGeofence() - id : " + locationId + ", ERROR !!, mResult : " + mResult + "");
            }
            addGeofenceIntentHistoricalDumpLog("stopGeofence()   - [ locationId : " + locationId + " ], ERROR !!, mResult : " + mResult);
            return;
        }
        if (this.DBG) {
            Log.d(TAG, " stopGeofence() - id : " + locationId);
        }
        addGeofenceIntentHistoricalDumpLog("stopGeofence()   - [ locationId : " + locationId + " ]");
    }

    /* access modifiers changed from: private */
    public void sendBroadcastForInOutRange(int state) {
        checkAndSetTelephonyManagerInstance();
        if (this.DBG) {
            Log.d(TAG, "sendBroadcastForInOutRange() - state : " + state + " getNetworkType() : " + this.mTm.getNetworkType());
        }
        TelephonyManager telephonyManager = this.mTm;
        if (telephonyManager == null || telephonyManager.getNetworkType() > 0) {
            if (isGeofenceExit(false)) {
                sendGeofenceStateChangedEvent(2);
            } else {
                sendGeofenceStateChangedEvent(1);
            }
            if (state == 1 || state == 0) {
                this.mInRange = true;
                WifiConnectivityManager wifiConnectivityManager = this.mWifiConnectivityManager;
                if (wifiConnectivityManager != null) {
                    if (wifiConnectivityManager.getPeriodicSingleScanInterval() > 128000) {
                        this.mWifiConnectivityManager.resetPeriodicScanTime();
                        if (this.DBG) {
                            Log.d(TAG, "sendBroadcastForInOutRange - resetPeiodicScanTime() : " + this.mWifiConnectivityManager.getPeriodicSingleScanInterval());
                        }
                    }
                    setScanInterval(this.mWifiConnectivityManager.getPeriodicSingleScanInterval() / 1000, 128);
                }
                sendBroadcastForGeofenceState(true);
            } else if (state == 2) {
                this.mInRange = false;
                WifiConnectivityManager wifiConnectivityManager2 = this.mWifiConnectivityManager;
                if (wifiConnectivityManager2 != null) {
                    setScanInterval(wifiConnectivityManager2.getPeriodicSingleScanInterval() / 1000, 1024);
                }
                sendBroadcastForGeofenceState(false);
            }
        } else {
            sendBroadcastForGeofenceState(true);
            sendGeofenceStateChangedEvent(2);
        }
    }

    public boolean isValidAccessPointToUseGeofence(WifiInfo info, WifiConfiguration config) {
        int[] mIgnorableApMASK = {2861248};
        new int[1][0] = 660652;
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - GeofenceManager is disabled");
            return false;
        } else if (info == null || config == null) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - info or config is null!");
            return false;
        } else if (config.semSamsungSpecificFlags.get(1)) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - Samsung mobile hotspot");
            return false;
        } else if (info.getMeteredHint()) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - Android Mobile Hotspot");
            return false;
        } else if (config.semIsVendorSpecificSsid) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - isVendorSpecificSsid is true");
            return false;
        } else if (config.isPasspoint()) {
            Log.d(TAG, "isValidAccessPointToUseGeofence() - PassPoint AP !!");
            return false;
        } else {
            for (int mask : mIgnorableApMASK) {
                if ((info.getIpAddress() & 16777215) == mask) {
                    Log.d(TAG, "isValidAccessPointToUseGeofence() - Masked Android Hotspot");
                    return false;
                }
            }
            return true;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0036, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0047, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x004e, code lost:
        if (r0 == false) goto L_0x005c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0052, code lost:
        if (r7.DBG == false) goto L_0x005b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0054, code lost:
        android.util.Log.d(TAG, "isGeofenceExit : return true");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x005b, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x005e, code lost:
        if (r7.DBG == false) goto L_0x0067;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0060, code lost:
        android.util.Log.d(TAG, "isGeofenceExit : return false, Geofence DB is empty");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0067, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isGeofenceExit(boolean r8) {
        /*
            r7 = this;
            r0 = 0
            java.lang.Object r1 = mGeofenceLock
            monitor-enter(r1)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData> r2 = r7.mGeofenceDataList     // Catch:{ all -> 0x0068 }
            java.util.Set r2 = r2.keySet()     // Catch:{ all -> 0x0068 }
            java.util.Iterator r2 = r2.iterator()     // Catch:{ all -> 0x0068 }
        L_0x000e:
            boolean r3 = r2.hasNext()     // Catch:{ all -> 0x0068 }
            r4 = 1
            r5 = 0
            if (r3 == 0) goto L_0x004d
            java.lang.Object r3 = r2.next()     // Catch:{ all -> 0x0068 }
            java.lang.Integer r3 = (java.lang.Integer) r3     // Catch:{ all -> 0x0068 }
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData> r6 = r7.mGeofenceDataList     // Catch:{ all -> 0x0068 }
            java.lang.Object r6 = r6.get(r3)     // Catch:{ all -> 0x0068 }
            com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData r6 = (com.android.server.wifi.WifiGeofenceDBHelper.WifiGeofenceData) r6     // Catch:{ all -> 0x0068 }
            int r6 = r6.getIsGeofenceEnter()     // Catch:{ all -> 0x0068 }
            if (r6 != r4) goto L_0x0037
            boolean r2 = r7.DBG     // Catch:{ all -> 0x0068 }
            if (r2 == 0) goto L_0x0035
            java.lang.String r2 = "WifiGeofenceManager"
            java.lang.String r4 = "isGeofenceExit : return false"
            android.util.Log.d(r2, r4)     // Catch:{ all -> 0x0068 }
        L_0x0035:
            monitor-exit(r1)     // Catch:{ all -> 0x0068 }
            return r5
        L_0x0037:
            if (r8 == 0) goto L_0x0048
            if (r6 != 0) goto L_0x0048
            boolean r2 = r7.DBG     // Catch:{ all -> 0x0068 }
            if (r2 == 0) goto L_0x0046
            java.lang.String r2 = "WifiGeofenceManager"
            java.lang.String r4 = "isGeofenceExit : return false"
            android.util.Log.d(r2, r4)     // Catch:{ all -> 0x0068 }
        L_0x0046:
            monitor-exit(r1)     // Catch:{ all -> 0x0068 }
            return r5
        L_0x0048:
            r4 = 2
            if (r6 != r4) goto L_0x004c
            r0 = 1
        L_0x004c:
            goto L_0x000e
        L_0x004d:
            monitor-exit(r1)     // Catch:{ all -> 0x0068 }
            if (r0 == 0) goto L_0x005c
            boolean r1 = r7.DBG
            if (r1 == 0) goto L_0x005b
            java.lang.String r1 = "WifiGeofenceManager"
            java.lang.String r2 = "isGeofenceExit : return true"
            android.util.Log.d(r1, r2)
        L_0x005b:
            return r4
        L_0x005c:
            boolean r1 = r7.DBG
            if (r1 == 0) goto L_0x0067
            java.lang.String r1 = "WifiGeofenceManager"
            java.lang.String r2 = "isGeofenceExit : return false, Geofence DB is empty"
            android.util.Log.d(r1, r2)
        L_0x0067:
            return r5
        L_0x0068:
            r2 = move-exception
            monitor-exit(r1)     // Catch:{ all -> 0x0068 }
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiGeofenceManager.isGeofenceExit(boolean):boolean");
    }

    public List<String> getGeofenceEnterKeys() {
        String key;
        List<String> configKeys = new ArrayList<>();
        synchronized (mGeofenceLock) {
            for (Integer locationId : this.mGeofenceDataList.keySet()) {
                if (this.mGeofenceDataList.get(locationId).getIsGeofenceEnter() == 1 && (key = this.mGeofenceDataList.get(locationId).getConfigKey()) != null) {
                    configKeys.add(key);
                }
            }
        }
        return configKeys;
    }

    public int getGeofenceCellCount(String configKey) {
        if (this.mSLocationManager == null) {
            return 0;
        }
        synchronized (mGeofenceLock) {
            for (Integer locationId : this.mGeofenceDataList.keySet()) {
                if (this.mGeofenceDataList.get(locationId).getConfigKey().equals(configKey)) {
                    int cellCountForEventGeofence = this.mSLocationManager.getCellCountForEventGeofence(locationId.intValue());
                    return cellCountForEventGeofence;
                }
            }
            return 0;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0053, code lost:
        if (r6.DBG == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0055, code lost:
        android.util.Log.d(TAG, "isFindLocationId() - failed to find!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:?, code lost:
        return -1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:?, code lost:
        return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int isFindLocationId(java.lang.String r7) {
        /*
            r6 = this;
            java.lang.Object r0 = mGeofenceLock
            monitor-enter(r0)
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData> r1 = r6.mGeofenceDataList     // Catch:{ all -> 0x005e }
            java.util.Set r1 = r1.keySet()     // Catch:{ all -> 0x005e }
            java.util.Iterator r1 = r1.iterator()     // Catch:{ all -> 0x005e }
        L_0x000d:
            boolean r2 = r1.hasNext()     // Catch:{ all -> 0x005e }
            if (r2 == 0) goto L_0x0050
            java.lang.Object r2 = r1.next()     // Catch:{ all -> 0x005e }
            java.lang.Integer r2 = (java.lang.Integer) r2     // Catch:{ all -> 0x005e }
            java.util.HashMap<java.lang.Integer, com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData> r3 = r6.mGeofenceDataList     // Catch:{ all -> 0x005e }
            java.lang.Object r3 = r3.get(r2)     // Catch:{ all -> 0x005e }
            com.android.server.wifi.WifiGeofenceDBHelper$WifiGeofenceData r3 = (com.android.server.wifi.WifiGeofenceDBHelper.WifiGeofenceData) r3     // Catch:{ all -> 0x005e }
            java.lang.String r3 = r3.getConfigKey()     // Catch:{ all -> 0x005e }
            boolean r4 = r7.equals(r3)     // Catch:{ all -> 0x005e }
            if (r4 == 0) goto L_0x004f
            boolean r1 = r6.DBG     // Catch:{ all -> 0x005e }
            if (r1 == 0) goto L_0x0049
            java.lang.String r1 = "WifiGeofenceManager"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x005e }
            r4.<init>()     // Catch:{ all -> 0x005e }
            java.lang.String r5 = "isFindLocationId() - Location Id : "
            r4.append(r5)     // Catch:{ all -> 0x005e }
            int r5 = r2.intValue()     // Catch:{ all -> 0x005e }
            r4.append(r5)     // Catch:{ all -> 0x005e }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x005e }
            android.util.Log.d(r1, r4)     // Catch:{ all -> 0x005e }
        L_0x0049:
            int r1 = r2.intValue()     // Catch:{ all -> 0x005e }
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            return r1
        L_0x004f:
            goto L_0x000d
        L_0x0050:
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            boolean r0 = r6.DBG
            if (r0 == 0) goto L_0x005c
            java.lang.String r0 = "WifiGeofenceManager"
            java.lang.String r1 = "isFindLocationId() - failed to find!"
            android.util.Log.d(r0, r1)
        L_0x005c:
            r0 = -1
            return r0
        L_0x005e:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiGeofenceManager.isFindLocationId(java.lang.String):int");
    }

    /* access modifiers changed from: private */
    public boolean isSimCardReady() {
        if ((!this.DBG || !SystemProperties.get("SimCheck.disable").equals("1")) && getSimState() != 5) {
            return false;
        }
        return true;
    }

    private int getSimState() {
        checkAndSetTelephonyManagerInstance();
        TelephonyManager telephonyManager = this.mTm;
        if (telephonyManager == null) {
            return 0;
        }
        int isMultiSim = TelephonyUtil.semGetMultiSimState(telephonyManager);
        if (this.DBG) {
            Log.d(TAG, "isMultiSim : " + isMultiSim);
        }
        if (isMultiSim < 4) {
            return 5;
        }
        return 0;
    }

    private void checkAndSetTelephonyManagerInstance() {
        if (this.mTm == null) {
            this.mTm = (TelephonyManager) this.mContext.getSystemService("phone");
        }
    }

    public void startGeofenceThread(List<WifiConfiguration> configs) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "startGeofenceThread() - GeofenceManager is disabled");
        } else if (this.mSLocationManager != null && configs != null) {
            if (this.DBG) {
                Log.e(TAG, "ConnectModeState - enter !! - mGeofenceManagerEnabled !!");
            }
            synchronized (mGeofenceLock) {
                Iterator<Integer> keyIterator = this.mGeofenceDataList.keySet().iterator();
                while (keyIterator.hasNext()) {
                    Integer locationId = keyIterator.next();
                    String configKey = this.mGeofenceDataList.get(locationId).getConfigKey();
                    boolean matched = false;
                    Iterator<WifiConfiguration> it = configs.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        } else if (configKey.equals(it.next().configKey())) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        if (this.DBG) {
                            Log.d(TAG, "delete config(Sync) - locationId : " + locationId + "  configKey() : " + configKey);
                        }
                        addGeofenceIntentHistoricalDumpLog("Delete config(sync)- [ locationId : " + locationId + " ], [ configKey : " + configKey + " ]");
                        this.mGeofenceDBHelper.delete(locationId.intValue());
                        keyIterator.remove();
                        int mResult = this.mSLocationManager.removeGeofence(locationId.intValue());
                        if (mResult < 0) {
                            if (this.DBG) {
                                Log.d(TAG, "delete config(Sync) - locationId : " + locationId + ", removeGeofence() ERROR !!, mResult : " + mResult);
                            }
                            addGeofenceIntentHistoricalDumpLog("Delete config(sync)- [ locationId : " + locationId + " ], removeGeofence ERROR !!, mResult : " + mResult);
                        } else {
                            if (this.DBG) {
                                Log.d(TAG, "delete config(Sync) - locationId : " + locationId + ", configKey : " + configKey + ", removeGeofence() Success !!");
                            }
                            addGeofenceIntentHistoricalDumpLog("Delete config(sync)- [ locationId : " + locationId + " ], [ configKey : " + configKey + " ]");
                        }
                    }
                }
            }
            this.mStartGeofenceHandler.post(this.mStartGeofenceThread);
        }
    }

    public void removeNetwork(WifiConfiguration config) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "removeNetwork() - GeofenceManager is disabled");
        } else if (config == null) {
            Log.d(TAG, "ConnectModeState() - REMOVE_NETWORK - config is null");
        } else {
            removeGeofence(config.configKey());
            if (this.DBG) {
                Log.d(TAG, "ConnectModeState() - REMOVE_NETWORK - configKey : " + config.configKey() + " !!");
            }
            addGeofenceIntentHistoricalDumpLog("RemoveNetwork    - [ configKey : " + config.configKey() + " ]");
            if (isGeofenceExit(true)) {
                sendBroadcastForInOutRange(2);
            } else {
                sendBroadcastForInOutRange(1);
            }
        }
    }

    public void forgetNetwork(WifiConfiguration config) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "forgetNetwork() - GeofenceManager is disabled");
        } else if (config == null) {
            Log.d(TAG, "ConnectModeState() - FORGET_NETWORK - config is null");
        } else {
            removeGeofence(config.configKey());
            if (this.DBG) {
                Log.d(TAG, "ConnectModeState() - FORGET_NETWORK - configKey : " + config.configKey() + " !!");
            }
            addGeofenceIntentHistoricalDumpLog("ForgetNetwork    - [ configKey : " + config.configKey() + " ]");
            if (isGeofenceExit(true)) {
                sendBroadcastForInOutRange(2);
            } else {
                sendBroadcastForInOutRange(1);
            }
        }
    }

    public void triggerStartLearning(WifiConfiguration currentConfig) {
        TelephonyManager telephonyManager;
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "triggerStartLearning() - GeofenceManager is disabled");
        } else if (currentConfig == null) {
            Log.d(TAG, "triggerStartLearning - currentConfig is null");
        } else {
            checkAndSetTelephonyManagerInstance();
            if (!(this.mSLocationManager == null || (telephonyManager = this.mTm) == null || telephonyManager.getNetworkType() <= 0)) {
                int locationId = isFindLocationId(currentConfig.configKey());
                long nowMs = System.currentTimeMillis();
                int mResult = -100;
                if (locationId < 0) {
                    locationId = addGeofence(currentConfig);
                    if (locationId > 0) {
                        mResult = this.mSLocationManager.startLearning(locationId);
                        if (this.DBG) {
                            Log.d(TAG, "triggerStartLearning - new locationId, startLearning id : " + locationId);
                        }
                    } else if (this.DBG) {
                        Log.d(TAG, "triggerStartLearning - locationId < 0 !!");
                    }
                } else {
                    this.mGeofenceDBHelper.update(locationId, nowMs);
                    startGeofence(locationId);
                    mResult = this.mSLocationManager.startLearning(locationId);
                    if (this.DBG) {
                        Log.d(TAG, "triggerStartLearning - startLearning id : " + locationId);
                    }
                }
                if (locationId < 0) {
                    if (this.DBG) {
                        Log.d(TAG, "triggerStartLearning - locationId < 0 !!");
                    }
                } else if (mResult < 0) {
                    SemLocationManager semLocationManager = this.mSLocationManager;
                    if (mResult == -3) {
                        deleteNotExistAP();
                    }
                    if (this.DBG) {
                        Log.d(TAG, "triggerStartLearning - id : " + locationId + ", enter() - ERROR !!, mResult : " + mResult + "");
                    }
                    addGeofenceIntentHistoricalDumpLog("trigStartLearning- [ locationId : " + locationId + " ], enter() - ERROR !!, mResult : " + mResult);
                }
                if (this.DBG) {
                    Log.d(TAG, "triggerStartLearning - id : " + locationId + ",  startLearning Success !!");
                }
                addGeofenceIntentHistoricalDumpLog("trigStartLearning- [ locationId : " + locationId + " ], startLearning Success !!");
            }
            this.mTriggerStartLearning = true;
        }
    }

    public void triggerStopLearning(WifiConfiguration currentConfig) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "triggerStopLearning() - GeofenceManager is disabled");
        } else if (currentConfig == null) {
            Log.d(TAG, "triggerStopLearning - currentConfig is null");
        } else {
            if (this.mSLocationManager != null) {
                int locationId = isFindLocationId(currentConfig.configKey());
                if (this.DBG) {
                    Log.d(TAG, "triggerStopLearning id : " + locationId);
                }
                if (locationId > 0) {
                    int mResult = this.mSLocationManager.stopLearning(locationId);
                    if (mResult < 0) {
                        if (this.DBG) {
                            Log.d(TAG, "triggerStopLearning - id : " + locationId + ", exit() - ERROR !!, mResult : " + mResult + "");
                        }
                        addGeofenceIntentHistoricalDumpLog("triggerStopLearning - [ locationId : " + locationId + " ], exit() - ERROR !!, mResult : " + mResult);
                    } else {
                        addGeofenceIntentHistoricalDumpLog("triggerStopLearning - [ locationId : " + locationId + " ], Success !! ");
                    }
                }
            }
            this.mTriggerStartLearning = false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isInRange() {
        return this.mInRange;
    }

    /* access modifiers changed from: package-private */
    public boolean isSupported() {
        return this.mGeofenceManagerEnabled;
    }

    /* access modifiers changed from: package-private */
    public void deleteNotExistAP() {
        if (this.mNotExistAPCheckMap.size() != 0) {
            synchronized (mGeofenceLock) {
                for (Map.Entry<String, Integer> element : this.mNotExistAPCheckMap.entrySet()) {
                    this.mGeofenceDBHelper.delete(element.getKey());
                    this.mSLocationManager.removeGeofence(element.getValue().intValue());
                }
                this.mNotExistAPCheckMap.clear();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void deleteNotExistAPFromSlocation() {
        new ArrayList();
        List<Integer> mSlocationDbList = this.mSLocationManager.getGeofenceIdList((String) null);
        HashMap<Integer, String> toBeEraseFromGeofenceDB = new HashMap<>();
        synchronized (mGeofenceLock) {
            for (Integer intValue : this.mGeofenceDataList.keySet()) {
                int locationId = intValue.intValue();
                if (!mSlocationDbList.contains(new Integer(locationId))) {
                    toBeEraseFromGeofenceDB.put(new Integer(locationId), this.mGeofenceDataList.get(Integer.valueOf(locationId)).getConfigKey());
                }
            }
        }
        if (!toBeEraseFromGeofenceDB.isEmpty()) {
            for (Map.Entry<Integer, String> entry : toBeEraseFromGeofenceDB.entrySet()) {
                synchronized (mGeofenceLock) {
                    addGeofenceIntentHistoricalDumpLog("Delete " + entry.getValue() + " AP, since the SLocation db does not have this profile");
                    this.mGeofenceDBHelper.delete(entry.getValue());
                }
            }
        }
        toBeEraseFromGeofenceDB.clear();
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void register(WifiConnectivityManager wifiConnectivityManager) {
        this.mWifiConnectivityManager = wifiConnectivityManager;
    }

    /* access modifiers changed from: package-private */
    public void register(NetworkInfo networkInfo) {
        this.mNetworkInfo = networkInfo;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mGeofenceLogManager.dump(fd, pw, args);
        this.mGeofenceDBHelper.dump(fd, pw, args);
    }

    public void setWifiGeofenceListener(WifiGeofenceStateListener listener) {
        this.mWifiGeofenceListeners.add(listener);
    }

    public void sendGeofenceStateChangedEvent(int state) {
        this.mLastGeofenceState = state;
        Iterator<WifiGeofenceStateListener> it = this.mWifiGeofenceListeners.iterator();
        while (it.hasNext()) {
            WifiGeofenceStateListener listener = it.next();
            if (listener != null) {
                listener.onGeofenceStateChanged(state, getGeofenceEnterKeys());
            }
        }
    }

    public int getCurrentGeofenceState() {
        return this.mLastGeofenceState;
    }

    public void setGeofenceStateByAnotherPackage(boolean enable) {
        this.mGeofenceStateByAnotherPackage = enable;
    }

    public boolean setLatitudeAndLongitude(WifiConfiguration config, double latitude, double longitude) {
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "setLatitudeAndLongitude() - GeofenceManager is disabled");
            return false;
        } else if (config == null) {
            Log.d(TAG, "setLatitudeAndLongitude - config is null");
            return false;
        } else {
            int locationId = isFindLocationId(config.configKey());
            if (locationId < 0) {
                if (this.DBG) {
                    Log.d(TAG, "setLatitudeAndLongitude - locationId < 0 !");
                }
                return false;
            }
            this.mGeofenceDBHelper.update(locationId, latitude, longitude);
            return true;
        }
    }

    public String getLatitudeAndLongitude(WifiConfiguration config) {
        double latitude;
        double longitude;
        if (!this.mGeofenceManagerEnabled) {
            Log.d(TAG, "setLatitudeAndLongitude() - GeofenceManager is disabled");
            return null;
        } else if (config == null) {
            Log.d(TAG, "setLatitudeAndLongitude - config is null");
            return null;
        } else {
            int locationId = isFindLocationId(config.configKey());
            if (locationId < 0) {
                if (this.DBG) {
                    Log.d(TAG, "getLatitudeAndLongitude - locationId < 0 !!");
                }
                return null;
            }
            synchronized (mGeofenceLock) {
                latitude = this.mGeofenceDataList.get(Integer.valueOf(locationId)).getLatitude();
                longitude = this.mGeofenceDataList.get(Integer.valueOf(locationId)).getLongitude();
            }
            if (this.DBG) {
                Log.e(TAG, "getLatitudeAndLongitude - " + latitude + ":" + longitude);
            }
            return latitude + ":" + longitude;
        }
    }
}
