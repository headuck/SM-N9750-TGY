package com.samsung.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiInjector;

public final class SwitchBoardService {
    private static final boolean DBG = true;
    private static final int EVENT_BOOT_COMPLETED = 3;
    private static final int EVENT_ENABLE_DEBUG = 2;
    private static final int EVENT_ENABLE_OR_DISABLE_SWITCHBOARD = 1;
    private static final int EVENT_GET_WIFIINFO_POLL = 4;
    private static final int INVALID_RSSI = -127;
    private static final String LOG_TAG = SwitchBoardService.class.getSimpleName();
    private static final int NORMAL_WIFI_POLLING_INTERVAL = 3000;
    private static final String SWITCHBOARD_INTENT_ENABLE_DEBUG = "com.samsung.android.SwitchBoard.ENABLE_DEBUG";
    private static final String SWITCHBOARD_INTENT_START = "com.samsung.android.SwitchBoard.START";
    private static final String SWITCHBOARD_INTENT_STATE = "com.samsung.android.SwitchBoard.STATE";
    private static final String SWITCHBOARD_INTENT_STOP = "com.samsung.android.SwitchBoard.STOP";
    private static final String SWITCHBOARD_INTENT_SWITCH_INTERVAL = "com.samsung.android.SwitchBoard.SWITCH_INTERVAL";
    private static final String SWITCHBOARD_INTENT_WIFI_PREFERENCE_VALUE = "com.samsung.android.SwitchBoard.WIFI_PREFERENCE_VALUE";
    private static final String SWITCHBOARD_STATE = "switchboard_state";
    private static final int SWITCHBOARD_WIFI_POLLING_INTERVAL = 1000;
    /* access modifiers changed from: private */
    public static boolean VDBG = false;
    private static volatile SwitchBoardService mInstance = null;
    private static final boolean mIsEngBuild = "eng".equals(SystemProperties.get("ro.build.type"));
    private static final boolean mIsShipBuild = "true".equals(SystemProperties.get("ro.product_ship"));
    private final Clock mClock;
    private ClientModeImpl mCmi;
    private Context mContext;
    /* access modifiers changed from: private */
    public SwitchBoardHandler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsBootCompleted = false;
    /* access modifiers changed from: private */
    public boolean mIsEnableRequestBeforeBootComplete = false;
    private int mIsSwitchBoardPerferDataPathWifi = 1;
    private long mLastUpdatedTimeMs = 0;
    /* access modifiers changed from: private */
    public int mLteToWifiDelayMs = 5000;
    private int mOldWifiRssi;
    private long mOldWifiTxBad;
    private long mOldWifiTxRetries;
    private long mOldWifiTxSuccess;
    private boolean mSwitchBoardEnabled = false;
    private String mWifiIface = SystemProperties.get("wifi.interface");
    private WifiInfo mWifiInfo;
    /* access modifiers changed from: private */
    public boolean mWifiInfoPollingEnabled = false;
    /* access modifiers changed from: private */
    public boolean mWifiInfoPollingEnabledAlways = false;
    /* access modifiers changed from: private */
    public int mWifiInfoPollingInterval = -1;
    /* access modifiers changed from: private */
    public int mWifiToLteDelayMs = 0;

    public static SwitchBoardService getInstance(Context ctx, Looper looper, ClientModeImpl clientmodeimpl) {
        if (mInstance == null) {
            synchronized (SwitchBoardService.class) {
                if (mInstance == null) {
                    mInstance = new SwitchBoardService(ctx, looper, clientmodeimpl);
                }
            }
        }
        return mInstance;
    }

    private SwitchBoardService(Context context, Looper looper, ClientModeImpl Cmi) {
        this.mContext = context;
        this.mCmi = Cmi;
        this.mWifiInfo = this.mCmi.getWifiInfo();
        this.mClock = WifiInjector.getInstance().getClock();
        if (looper != null) {
            this.mHandler = new SwitchBoardHandler(looper);
            IntentFilter switchboardIntentFilter = new IntentFilter();
            switchboardIntentFilter.addAction(SWITCHBOARD_INTENT_START);
            switchboardIntentFilter.addAction(SWITCHBOARD_INTENT_STOP);
            switchboardIntentFilter.addAction(SWITCHBOARD_INTENT_ENABLE_DEBUG);
            switchboardIntentFilter.addAction(SWITCHBOARD_INTENT_SWITCH_INTERVAL);
            switchboardIntentFilter.addAction("android.intent.action.BOOT_COMPLETED");
            this.mContext.registerReceiver(new SwitchBoardReceiver(), switchboardIntentFilter);
            return;
        }
        loge("handlerThread.getLooper() returned null");
    }

    class SwitchBoardReceiver extends BroadcastReceiver {
        SwitchBoardReceiver() {
        }

        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(android.content.Context r9, android.content.Intent r10) {
            /*
                r8 = this;
                java.lang.String r0 = r10.getAction()
                int r1 = r0.hashCode()
                r2 = 4
                r3 = 3
                r4 = 2
                r5 = 1
                r6 = 0
                switch(r1) {
                    case -1897205914: goto L_0x0039;
                    case -1727841388: goto L_0x002f;
                    case -837322541: goto L_0x0025;
                    case -615389090: goto L_0x001b;
                    case 798292259: goto L_0x0011;
                    default: goto L_0x0010;
                }
            L_0x0010:
                goto L_0x0043
            L_0x0011:
                java.lang.String r1 = "android.intent.action.BOOT_COMPLETED"
                boolean r1 = r0.equals(r1)
                if (r1 == 0) goto L_0x0010
                r1 = r2
                goto L_0x0044
            L_0x001b:
                java.lang.String r1 = "com.samsung.android.SwitchBoard.STOP"
                boolean r1 = r0.equals(r1)
                if (r1 == 0) goto L_0x0010
                r1 = r5
                goto L_0x0044
            L_0x0025:
                java.lang.String r1 = "com.samsung.android.SwitchBoard.ENABLE_DEBUG"
                boolean r1 = r0.equals(r1)
                if (r1 == 0) goto L_0x0010
                r1 = r4
                goto L_0x0044
            L_0x002f:
                java.lang.String r1 = "com.samsung.android.SwitchBoard.SWITCH_INTERVAL"
                boolean r1 = r0.equals(r1)
                if (r1 == 0) goto L_0x0010
                r1 = r3
                goto L_0x0044
            L_0x0039:
                java.lang.String r1 = "com.samsung.android.SwitchBoard.START"
                boolean r1 = r0.equals(r1)
                if (r1 == 0) goto L_0x0010
                r1 = r6
                goto L_0x0044
            L_0x0043:
                r1 = -1
            L_0x0044:
                java.lang.String r7 = "SwitchBoardReceiver.onReceive: action="
                if (r1 == 0) goto L_0x011d
                if (r1 == r5) goto L_0x00f3
                if (r1 == r4) goto L_0x00d5
                if (r1 == r3) goto L_0x008d
                if (r1 == r2) goto L_0x0066
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "SwitchBoardReceiver.onReceive: undefined case: action="
                r1.append(r2)
                r1.append(r0)
                java.lang.String r1 = r1.toString()
                com.samsung.android.server.wifi.SwitchBoardService.logd(r1)
                goto L_0x015c
            L_0x0066:
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                r1.append(r7)
                r1.append(r0)
                java.lang.String r1 = r1.toString()
                com.samsung.android.server.wifi.SwitchBoardService.logv(r1)
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r1 = r1.mHandler
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r2 = r2.mHandler
                android.os.Message r2 = r2.obtainMessage(r3)
                r1.sendMessage(r2)
                goto L_0x015c
            L_0x008d:
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                java.lang.String r2 = "WifiToLteDelayMs"
                int r2 = r10.getIntExtra(r2, r6)
                int unused = r1.mWifiToLteDelayMs = r2
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                r2 = 5000(0x1388, float:7.006E-42)
                java.lang.String r3 = "LteToWifiDelayMs"
                int r2 = r10.getIntExtra(r3, r2)
                int unused = r1.mLteToWifiDelayMs = r2
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                r1.append(r7)
                r1.append(r0)
                java.lang.String r2 = ", mWifiToLteDelayMs: "
                r1.append(r2)
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                int r2 = r2.mWifiToLteDelayMs
                r1.append(r2)
                java.lang.String r2 = ", mLteToWifiDelayMs: "
                r1.append(r2)
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                int r2 = r2.mLteToWifiDelayMs
                r1.append(r2)
                java.lang.String r1 = r1.toString()
                com.samsung.android.server.wifi.SwitchBoardService.logd(r1)
                goto L_0x015c
            L_0x00d5:
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r1 = r1.mHandler
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r2 = r2.mHandler
                java.lang.String r3 = "DEBUG"
                boolean r3 = r10.getBooleanExtra(r3, r6)
                java.lang.Boolean r3 = java.lang.Boolean.valueOf(r3)
                android.os.Message r2 = r2.obtainMessage(r4, r3)
                r1.sendMessage(r2)
                goto L_0x015c
            L_0x00f3:
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                r1.append(r7)
                r1.append(r0)
                java.lang.String r1 = r1.toString()
                com.samsung.android.server.wifi.SwitchBoardService.logd(r1)
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r1 = r1.mHandler
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r2 = r2.mHandler
                java.lang.Boolean r3 = java.lang.Boolean.valueOf(r6)
                android.os.Message r2 = r2.obtainMessage(r5, r3)
                r1.sendMessage(r2)
                goto L_0x015c
            L_0x011d:
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                r1.append(r7)
                r1.append(r0)
                java.lang.String r2 = "AlwaysPolling"
                r1.append(r2)
                boolean r3 = r10.getBooleanExtra(r2, r6)
                r1.append(r3)
                java.lang.String r1 = r1.toString()
                com.samsung.android.server.wifi.SwitchBoardService.logd(r1)
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                boolean r2 = r10.getBooleanExtra(r2, r6)
                boolean unused = r1.mWifiInfoPollingEnabledAlways = r2
                com.samsung.android.server.wifi.SwitchBoardService r1 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r1 = r1.mHandler
                com.samsung.android.server.wifi.SwitchBoardService r2 = com.samsung.android.server.wifi.SwitchBoardService.this
                com.samsung.android.server.wifi.SwitchBoardService$SwitchBoardHandler r2 = r2.mHandler
                java.lang.Boolean r3 = java.lang.Boolean.valueOf(r5)
                android.os.Message r2 = r2.obtainMessage(r5, r3)
                r1.sendMessage(r2)
            L_0x015c:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.SwitchBoardService.SwitchBoardReceiver.onReceive(android.content.Context, android.content.Intent):void");
        }
    }

    private class SwitchBoardHandler extends Handler {
        SwitchBoardHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                boolean enable = ((Boolean) msg.obj).booleanValue();
                SwitchBoardService.logv("EVENT_ENABLE_OR_DISABLE_SWITCHBOARD: " + enable);
                SwitchBoardService.this.setSwitchBoardState(enable, "AppsRequest");
            } else if (i == 2) {
                boolean unused = SwitchBoardService.VDBG = ((Boolean) msg.obj).booleanValue();
                SwitchBoardService.logv("EVENT_ENABLE_DEBUG: VDBG=" + SwitchBoardService.VDBG);
            } else if (i == 3) {
                SwitchBoardService.logd("EVENT_BOOT_COMPLETED");
                boolean unused2 = SwitchBoardService.this.mIsBootCompleted = true;
                if (SwitchBoardService.this.mIsEnableRequestBeforeBootComplete) {
                    SwitchBoardService switchBoardService = SwitchBoardService.this;
                    switchBoardService.setSwitchBoardState(switchBoardService.mIsEnableRequestBeforeBootComplete, "EnableRequestBeforeBootComplete");
                }
            } else if (i != 4) {
                SwitchBoardService.logd("SwitchBoardHandler.handleMessage: undefined case: msg=" + msg.what);
            } else {
                SwitchBoardService.logv("EVENT_GET_WIFIINFO_POLL");
                SwitchBoardService.this.determineDataPathPriority();
                if (SwitchBoardService.this.mWifiInfoPollingEnabled) {
                    SwitchBoardService.this.mHandler.sendMessageDelayed(SwitchBoardService.this.mHandler.obtainMessage(4), (long) SwitchBoardService.this.mWifiInfoPollingInterval);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void setSwitchBoardState(boolean enable, String reason) {
        if (!this.mIsBootCompleted) {
            logd("setSwitchBoardState: pending a request before boot completed [enable = " + enable + "]");
            this.mIsEnableRequestBeforeBootComplete = enable;
            return;
        }
        logd("setSwitchBoardState: add a new request [enable=" + enable + ", reason=" + reason + ", Always Polling=" + this.mWifiInfoPollingEnabledAlways + "]");
        if (enable) {
            this.mLastUpdatedTimeMs = this.mClock.getWallClockMillis();
            enablePollingRssiForSwitchboard(this.mWifiInfoPollingEnabledAlways, 1000);
            enableWifiInfoPolling(true);
            setSBInternalState(true);
        } else {
            enablePollingRssiForSwitchboard(false, 3000);
            enableWifiInfoPolling(false);
            setSBInternalState(false);
        }
        broadcastSBStatus(getSBInternalState());
    }

    private void broadcastSBStatus(boolean state) {
        Intent intent = new Intent();
        intent.setAction(SWITCHBOARD_INTENT_STATE);
        intent.putExtra(SWITCHBOARD_STATE, state);
        logi("broadcastSBStatus: SwitchBoard state changed(" + state + "), so send broadcast=" + intent);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private boolean getSBInternalState() {
        return this.mSwitchBoardEnabled;
    }

    private void setSBInternalState(boolean enable) {
        logv("setSBInternalState(" + enable + ")");
        this.mSwitchBoardEnabled = enable;
    }

    private void enablePollingRssiForSwitchboard(boolean enable, int interval) {
        this.mWifiInfoPollingInterval = interval;
        this.mCmi.enablePollingRssiForSwitchboard(enable, this.mWifiInfoPollingInterval);
    }

    private void enableWifiInfoPolling(boolean enable) {
        if (enable != this.mWifiInfoPollingEnabled) {
            this.mWifiInfoPollingEnabled = enable;
            if (this.mWifiInfoPollingEnabled) {
                SwitchBoardHandler switchBoardHandler = this.mHandler;
                switchBoardHandler.sendMessageDelayed(switchBoardHandler.obtainMessage(4), (long) this.mWifiInfoPollingInterval);
                return;
            }
            this.mHandler.removeMessages(4);
        }
    }

    /* access modifiers changed from: private */
    public void determineDataPathPriority() {
        long calculatedTxBad;
        if (this.mCmi.isConnected() && this.mWifiInfo.getRssi() != -127 && this.mOldWifiRssi != -127) {
            logv("determineSubflowPriority: mIsSwitchBoardPerferDataPathWifi =" + this.mIsSwitchBoardPerferDataPathWifi);
            long calculatedTxBad2 = this.mWifiInfo.txBad - this.mOldWifiTxBad;
            long calculatedTxRetriesRate = 0;
            long txFrames = (this.mWifiInfo.txSuccess + this.mWifiInfo.txBad) - (this.mOldWifiTxSuccess + this.mOldWifiTxBad);
            if (txFrames > 0) {
                calculatedTxRetriesRate = (this.mWifiInfo.txRetries - this.mOldWifiTxRetries) / txFrames;
            }
            logv("wifiMetric New [" + String.format("%4d, ", new Object[]{Integer.valueOf(this.mWifiInfo.getRssi())}) + String.format("%4d, ", new Object[]{Long.valueOf(this.mWifiInfo.txRetries)}) + String.format("%4d, ", new Object[]{Long.valueOf(this.mWifiInfo.txSuccess)}) + String.format("%4d, ", new Object[]{Long.valueOf(this.mWifiInfo.txBad)}) + "]");
            StringBuilder sb = new StringBuilder();
            sb.append("wifiMetric Old [");
            sb.append(String.format("%4d, ", new Object[]{Integer.valueOf(this.mOldWifiRssi)}));
            sb.append(String.format("%4d, ", new Object[]{Long.valueOf(this.mOldWifiTxRetries)}));
            String str = "]";
            sb.append(String.format("%4d, ", new Object[]{Long.valueOf(this.mOldWifiTxSuccess)}));
            long calculatedTxBad3 = calculatedTxBad2;
            sb.append(String.format("%4d, ", new Object[]{Long.valueOf(this.mOldWifiTxBad)}));
            sb.append(str);
            logv(sb.toString());
            logv("wifiMetric [" + String.format("RSSI: %4d, ", new Object[]{Integer.valueOf(this.mWifiInfo.getRssi())}) + String.format("Retry: %4d, ", new Object[]{Long.valueOf(this.mWifiInfo.txRetries - this.mOldWifiTxRetries)}) + String.format("TXGood: %4d, ", new Object[]{Long.valueOf(txFrames)}) + String.format("TXBad: %4d, ", new Object[]{Long.valueOf(calculatedTxBad3)}) + String.format("RetryRate%4d", new Object[]{Long.valueOf(calculatedTxRetriesRate)}) + str);
            int i = this.mIsSwitchBoardPerferDataPathWifi;
            if (i == 1) {
                if (calculatedTxRetriesRate <= 1 && calculatedTxBad3 <= 2) {
                    calculatedTxBad = calculatedTxBad3;
                } else if (this.mWifiInfo.getRssi() >= -70 || this.mOldWifiRssi >= -70) {
                    calculatedTxBad = calculatedTxBad3;
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Case0, triggered - txRetriesRate(");
                    sb2.append(calculatedTxRetriesRate);
                    sb2.append("), txBad(");
                    long calculatedTxBad4 = calculatedTxBad3;
                    sb2.append(calculatedTxBad4);
                    sb2.append(")");
                    logd(sb2.toString());
                    setWifiDataPathPriority(0);
                    long j = calculatedTxBad4;
                }
                if (this.mWifiInfo.getRssi() >= -85 || this.mOldWifiRssi >= -85) {
                    long j2 = calculatedTxBad;
                } else {
                    logd("Case1, triggered");
                    setWifiDataPathPriority(0);
                    long j3 = calculatedTxBad;
                }
            } else {
                long calculatedTxBad5 = calculatedTxBad3;
                if (i == 0) {
                    if (txFrames > 0 && calculatedTxRetriesRate < 1 && calculatedTxBad5 < 1 && this.mWifiInfo.getRssi() > -75 && this.mOldWifiRssi > -75) {
                        logd("Case2, triggered - txRetriesRate(" + calculatedTxRetriesRate + "), txBad(" + calculatedTxBad5 + ")");
                        setWifiDataPathPriority(1);
                    } else if (txFrames > 0 && calculatedTxRetriesRate < 2 && calculatedTxBad5 < 1 && this.mWifiInfo.getRssi() > -70 && this.mOldWifiRssi > -70) {
                        logd("Case3, triggered - txRetriesRate(" + calculatedTxRetriesRate + "), txBad(" + calculatedTxBad5 + ")");
                        setWifiDataPathPriority(1);
                    } else if (this.mWifiInfo.getRssi() > -60 && this.mOldWifiRssi > -60) {
                        logd("Case4, triggered RSSI is higher than -60dBm");
                        setWifiDataPathPriority(1);
                    }
                }
            }
            this.mOldWifiRssi = this.mWifiInfo.getRssi();
            this.mOldWifiTxSuccess = this.mWifiInfo.txSuccess;
            this.mOldWifiTxBad = this.mWifiInfo.txBad;
            this.mOldWifiTxRetries = this.mWifiInfo.txRetries;
        } else if (this.mCmi.isConnected() && (this.mWifiInfo.getRssi() != -127 || this.mOldWifiRssi != -127)) {
            this.mOldWifiRssi = this.mWifiInfo.getRssi();
            this.mOldWifiTxSuccess = this.mWifiInfo.txSuccess;
            this.mOldWifiTxBad = this.mWifiInfo.txBad;
            this.mOldWifiTxRetries = this.mWifiInfo.txRetries;
        } else if (!this.mCmi.isConnected() && this.mOldWifiRssi != -127) {
            this.mOldWifiRssi = -127;
            this.mOldWifiTxSuccess = 0;
            this.mOldWifiTxBad = 0;
            this.mOldWifiTxRetries = 0;
        }
    }

    private void setWifiDataPathPriority(int preferValue) {
        long timeStamp = this.mClock.getWallClockMillis();
        if ((preferValue != 1 || timeStamp - this.mLastUpdatedTimeMs < ((long) this.mLteToWifiDelayMs)) && (preferValue != 0 || timeStamp - this.mLastUpdatedTimeMs < ((long) this.mWifiToLteDelayMs))) {
            logd("setWifiDataPathPriority: , mIsSwitchBoardPerferDataPathWifi: " + this.mIsSwitchBoardPerferDataPathWifi + "dalayed");
            return;
        }
        this.mLastUpdatedTimeMs = timeStamp;
        this.mIsSwitchBoardPerferDataPathWifi = preferValue;
        logd("setWifiDataPathPriority: , mIsSwitchBoardPerferDataPathWifi: " + this.mIsSwitchBoardPerferDataPathWifi);
        Intent intent = new Intent();
        intent.setAction(SWITCHBOARD_INTENT_WIFI_PREFERENCE_VALUE);
        intent.putExtra("Preference", this.mIsSwitchBoardPerferDataPathWifi);
        logv("Send broadcast=" + intent);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    protected static void loge(String str) {
        Log.e(LOG_TAG, str);
    }

    protected static void logw(String str) {
        Log.w(LOG_TAG, str);
    }

    protected static void logi(String str) {
        Log.i(LOG_TAG, str);
    }

    protected static void logd(String str) {
        if (!mIsShipBuild || VDBG) {
            Log.d(LOG_TAG, str);
        }
    }

    protected static void logv(String str) {
        if (VDBG) {
            Log.v(LOG_TAG, str);
        }
    }
}
