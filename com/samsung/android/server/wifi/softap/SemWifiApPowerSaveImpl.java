package com.samsung.android.server.wifi.softap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.samsung.android.server.wifi.bigdata.WifiBigDataLogManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SemWifiApPowerSaveImpl {
    public static final String ACTION_SCREEN_OFF_BY_PROXIMITY = "android.intent.action.ACTION_SCREEN_OFF_BY_PROXIMITY";
    public static final String ACTION_SCREEN_ON_BY_PROXIMITY = "android.intent.action.ACTION_SCREEN_ON_BY_PROXIMITY";
    private static final String TAG = "SemWifiApPowerSaveImpl";
    /* access modifiers changed from: private */
    public int NumOfClientsConnected;
    private String SOFT_AP_SEND_MESSAGE_TIMEOUT_PACKET_CHECK_TAG = "SemWifiApPowerSaveImpl Soft AP Packet check Send Message Timeout";
    private String SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG = "SemWifiApPowerSaveImpl Soft AP Send Message Timeout";
    /* access modifiers changed from: private */
    public String mApInterfaceName;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public boolean mElnaEnable;
    /* access modifiers changed from: private */
    public boolean mIsEnabledSoftAp = false;
    /* access modifiers changed from: private */
    public boolean mIsLcdOn = false;
    private Looper mLooper;
    /* access modifiers changed from: private */
    public int mMacAddrAcl;
    /* access modifiers changed from: private */
    public int mMacMaxClient = -1;
    /* access modifiers changed from: private */
    public int mMaxClient;
    /* access modifiers changed from: private */
    public boolean mPacketScheduled;
    private int mPowerSaveChecked = 0;
    private SoftApCallback mSoftApCallback;
    /* access modifiers changed from: private */
    public WakeupMessage mSoftApPacketCheckTimeoutMessage;
    private final BroadcastReceiver mSoftApReceiver;
    private final IntentFilter mSoftApReceiverFilter;
    /* access modifiers changed from: private */
    public WakeupMessage mSoftApTimeoutMessage;
    /* access modifiers changed from: private */
    public long mStartTimeOfHotspot = 0;
    /* access modifiers changed from: private */
    public SoftApPowerSaveStateMachine mStateMachine = null;
    /* access modifiers changed from: private */
    public boolean mStateScheduled;
    /* access modifiers changed from: private */
    public long mTimeofPowersave = 0;
    /* access modifiers changed from: private */
    public boolean mUSBpuggedin;
    /* access modifiers changed from: private */
    public int mWifiApState;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public WifiNative mWifiNative;

    public SemWifiApPowerSaveImpl(Context context) {
        this.mContext = context;
        this.mSoftApReceiverFilter = new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED");
        this.mSoftApReceiverFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.mSoftApReceiverFilter.addAction("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
        this.mSoftApReceiverFilter.addAction("android.intent.action.SCREEN_ON");
        this.mSoftApReceiverFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mSoftApReceiverFilter.addAction(ACTION_SCREEN_OFF_BY_PROXIMITY);
        this.mSoftApReceiverFilter.addAction(ACTION_SCREEN_ON_BY_PROXIMITY);
        this.mSoftApReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.ACTION_POWER_CONNECTED") && SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                    Log.d(SemWifiApPowerSaveImpl.TAG, "plugged");
                    boolean unused = SemWifiApPowerSaveImpl.this.mUSBpuggedin = true;
                    SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(2);
                } else if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED") && SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                    Log.d(SemWifiApPowerSaveImpl.TAG, "Unplugged");
                    boolean unused2 = SemWifiApPowerSaveImpl.this.mUSBpuggedin = false;
                    SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(1);
                } else if (action.equals("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED")) {
                    int numClients = intent.getIntExtra("NUM", 0);
                    Log.d(SemWifiApPowerSaveImpl.TAG, "onNumClientsChanged:" + numClients);
                    SemWifiApPowerSaveImpl semWifiApPowerSaveImpl = SemWifiApPowerSaveImpl.this;
                    int unused3 = semWifiApPowerSaveImpl.mMacMaxClient = Settings.Secure.getInt(semWifiApPowerSaveImpl.mContext.getContentResolver(), "wifi_ap_number_of_max_macaddr_client", -1);
                    if (SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                        SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(3, numClients);
                    }
                } else if ("android.intent.action.SCREEN_ON".equals(action) || SemWifiApPowerSaveImpl.ACTION_SCREEN_ON_BY_PROXIMITY.equals(action)) {
                    boolean unused4 = SemWifiApPowerSaveImpl.this.mIsLcdOn = true;
                    SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(5);
                } else if ("android.intent.action.SCREEN_OFF".equals(action) || SemWifiApPowerSaveImpl.ACTION_SCREEN_OFF_BY_PROXIMITY.equals(action)) {
                    boolean unused5 = SemWifiApPowerSaveImpl.this.mIsLcdOn = false;
                    SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(6);
                }
            }
        };
    }

    private class SoftApCallback implements WifiManager.SoftApCallback {
        private SoftApCallback() {
        }

        public void onStateChanged(int state, int failureReason) {
            Log.d(SemWifiApPowerSaveImpl.TAG, "onStateChanged:" + state);
            int unused = SemWifiApPowerSaveImpl.this.mWifiApState = state;
            if (SemWifiApPowerSaveImpl.this.mWifiApState == 10 || SemWifiApPowerSaveImpl.this.mWifiApState == 11 || SemWifiApPowerSaveImpl.this.mWifiApState == 14) {
                if (SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                    SemWifiApPowerSaveImpl.this.mStateMachine.sendMessage(4);
                }
                boolean unused2 = SemWifiApPowerSaveImpl.this.mIsEnabledSoftAp = false;
            } else if (SemWifiApPowerSaveImpl.this.mWifiApState == 13) {
                long unused3 = SemWifiApPowerSaveImpl.this.mStartTimeOfHotspot = SystemClock.elapsedRealtime();
                WifiConfiguration mWificonfig = SemWifiApPowerSaveImpl.this.mWifiManager.getWifiApConfiguration();
                SemWifiApPowerSaveImpl semWifiApPowerSaveImpl = SemWifiApPowerSaveImpl.this;
                int unused4 = semWifiApPowerSaveImpl.mMacMaxClient = Settings.Secure.getInt(semWifiApPowerSaveImpl.mContext.getContentResolver(), "wifi_ap_number_of_max_macaddr_client", -1);
                int unused5 = SemWifiApPowerSaveImpl.this.mMacAddrAcl = mWificonfig.macaddrAcl;
                boolean unused6 = SemWifiApPowerSaveImpl.this.mIsEnabledSoftAp = true;
            }
        }

        public void onNumClientsChanged(int numClients) {
        }
    }

    private boolean isPlugged(Context context) {
        int plugged = context.registerReceiver((BroadcastReceiver) null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", -1);
        boolean z = true;
        if (!(plugged == 1 || plugged == 2 || plugged == 4)) {
            z = false;
        }
        boolean iisPlugged = z;
        Log.i(TAG, "iisPlugged:" + iisPlugged);
        return iisPlugged;
    }

    public void registerSoftApCallback(String ifaceName, int clients) {
        Log.e(TAG, "registerSoftApCallback with " + clients + " Max clients");
        this.mMaxClient = clients;
        this.mApInterfaceName = ifaceName;
        this.mPowerSaveChecked = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_powersave_mode_checked", 10);
        if (this.mPowerSaveChecked == 10) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_ap_powersave_mode_checked", 1);
            this.mPowerSaveChecked = 1;
        }
        if (this.mPowerSaveChecked == 0 || this.mApInterfaceName == null) {
            Log.e(TAG, "PowerSaveMode is not enabled");
            return;
        }
        this.mStateScheduled = false;
        this.mPacketScheduled = false;
        this.mElnaEnable = false;
        this.mUSBpuggedin = isPlugged(this.mContext);
        this.NumOfClientsConnected = 0;
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mIsLcdOn = pm == null ? false : pm.isInteractive();
        this.mStateMachine = new SoftApPowerSaveStateMachine();
        this.mSoftApTimeoutMessage = new WakeupMessage(this.mContext, this.mStateMachine.getHandler(), this.SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG, 0);
        this.mSoftApPacketCheckTimeoutMessage = new WakeupMessage(this.mContext, this.mStateMachine.getHandler(), this.SOFT_AP_SEND_MESSAGE_TIMEOUT_PACKET_CHECK_TAG, 7);
        this.mStateMachine.start();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mSoftApCallback = new SoftApCallback();
        this.mWifiManager.registerSoftApCallback(this.mSoftApCallback, (Handler) null);
        Context context = this.mContext;
        if (context != null) {
            context.registerReceiver(this.mSoftApReceiver, this.mSoftApReceiverFilter);
        }
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        Log.d(TAG, "registerSoftApCallback end");
    }

    public void unRegisterSoftApCallback() {
        Log.e(TAG, "unregisterSoftApCallback");
        this.mApInterfaceName = null;
        this.mPowerSaveChecked = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_powersave_mode_checked", 10);
        if (this.mPowerSaveChecked != 0) {
            this.mStateScheduled = false;
            this.mPacketScheduled = false;
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            try {
                this.mWifiManager.unregisterSoftApCallback(this.mSoftApCallback);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error: " + e);
            }
            this.mSoftApCallback = null;
            this.mSoftApTimeoutMessage = null;
            this.mSoftApPacketCheckTimeoutMessage = null;
            try {
                if (this.mContext != null) {
                    this.mContext.unregisterReceiver(this.mSoftApReceiver);
                }
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Error: " + e2);
            }
            this.mPowerSaveChecked = 0;
            SoftApPowerSaveStateMachine softApPowerSaveStateMachine = this.mStateMachine;
            if (softApPowerSaveStateMachine != null) {
                softApPowerSaveStateMachine.quitNow();
            }
            this.mStateMachine = null;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("--Dump of SoftApPowerSaveStateMachine--");
        pw.println("current StateMachine mode: " + getCurrentStateName());
        pw.println("MaxClient: " + this.mMaxClient);
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    private class SoftApPowerSaveStateMachine extends StateMachine {
        public static final int CMD_AP_DISABLED = 4;
        public static final int CMD_LCD_OFF = 6;
        public static final int CMD_LCD_ON = 5;
        public static final int CMD_NUM_ASSOCIATED_STATIONS_CHANGED = 3;
        public static final int CMD_PLUGGED_IN = 2;
        public static final int CMD_PLUGGED_OUT = 1;
        public static final int CMD_TIMEOUT = 0;
        public static final int CMD_TIMEOUT_PACKET_CHECK = 7;
        /* access modifiers changed from: private */
        public final State mDefaultState = new DefaultState();
        /* access modifiers changed from: private */
        public final State mPowerSaveAggressiveState = new PowerSaveAggressiveState();
        private final State mPowerSaveNoBeaconModeState = new PowerSaveNoBeaconModeState();
        /* access modifiers changed from: private */
        public final State mPowerSaveState = new PowerSaveState();
        /* access modifiers changed from: private */
        public final State mReadyOneState = new ReadyOneState();
        /* access modifiers changed from: private */
        public final State mReadyThreeState = new ReadyThreeState();
        /* access modifiers changed from: private */
        public final State mReadyTwoState = new ReadyTwoState();

        private int getPsTimeCal(int mTimeOfPs) {
            if (mTimeOfPs >= 720) {
                return 720;
            }
            return mTimeOfPs;
        }

        /* access modifiers changed from: private */
        public void sendBigdataForHotspotPowerSave(String mStateOfPowersave) {
            long tempTime = SystemClock.elapsedRealtime();
            Message msg = new Message();
            msg.what = 77;
            Bundle args = new Bundle();
            long realUsedTimeOfMhsPowerSave = (tempTime - SemWifiApPowerSaveImpl.this.mTimeofPowersave) / 1000;
            args.putBoolean("bigdata", true);
            args.putString("feature", WifiBigDataLogManager.FEATURE_MHS_POWERSAVEMODE);
            Log.d(SemWifiApPowerSaveImpl.TAG, "MHS logging for powersavemode mTimeofPowersave :" + SemWifiApPowerSaveImpl.this.mTimeofPowersave + " realUsedTimeOfMhsPowerSave :" + realUsedTimeOfMhsPowerSave + " seconds mStateOfPowersave : " + mStateOfPowersave);
            String tempPSTime = forBigdataOfPSTime(mStateOfPowersave, realUsedTimeOfMhsPowerSave);
            StringBuilder sb = new StringBuilder();
            sb.append(mStateOfPowersave);
            sb.append(" ");
            sb.append(tempPSTime);
            args.putString("data", sb.toString());
            msg.obj = args;
            ((WifiManager) SemWifiApPowerSaveImpl.this.mContext.getSystemService("wifi")).callSECApi(msg);
        }

        private String forBigdataOfPSTime(String stateValue, long time) {
            long devidedTime;
            if (stateValue.equals("ReadyOneState")) {
                devidedTime = 120;
            } else {
                devidedTime = 24;
            }
            if (time / devidedTime >= 5) {
                return "5";
            }
            return String.valueOf(time / devidedTime);
        }

        private int getPsTimeCalForRatio(int mRealTime, int mTimeOfPs) {
            return (mTimeOfPs * 100) / mRealTime;
        }

        /* access modifiers changed from: private */
        public void getPsTimeFuntion() {
            if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                String getTime = SemWifiApPowerSaveImpl.this.mWifiNative.getPstime(SemWifiApPowerSaveImpl.this.mApInterfaceName);
                if (SemWifiApPowerSaveImpl.this.mStartTimeOfHotspot != 0) {
                    int realTime = (int) ((SystemClock.elapsedRealtime() - SemWifiApPowerSaveImpl.this.mStartTimeOfHotspot) / 60000);
                    Log.d(SemWifiApPowerSaveImpl.TAG, "realTime : " + realTime + " getTime : " + getTime);
                    if (getTime != null && realTime != 0) {
                        String[] part = getTime.split("=|\\s");
                        if (realTime > 10) {
                            try {
                                if (Integer.parseInt(part[3]) >= 0 && Integer.parseInt(part[5]) >= 0 && Integer.parseInt(part[3]) <= 100) {
                                    if (Integer.parseInt(part[5]) <= 100) {
                                        String timeofPsBigdata = realTime + " " + getPsTimeCalForRatio(realTime, Integer.parseInt(part[3])) + " " + getPsTimeCalForRatio(realTime, Integer.parseInt(part[5]));
                                        Log.d(SemWifiApPowerSaveImpl.TAG, "timeofPsBigdata : " + timeofPsBigdata);
                                        Message msg = new Message();
                                        msg.what = 77;
                                        Bundle args = new Bundle();
                                        args.putBoolean("bigdata", true);
                                        args.putString("feature", WifiBigDataLogManager.FEATURE_MHS_POWERSAVEMODE_TIME);
                                        args.putString("data", timeofPsBigdata);
                                        msg.obj = args;
                                        ((WifiManager) SemWifiApPowerSaveImpl.this.mContext.getSystemService("wifi")).callSECApi(msg);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "NumberFormatException occurs");
                            } catch (ArrayIndexOutOfBoundsException e2) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "ArrayIndexOutOfBoundsException occurs");
                            }
                        }
                    }
                }
            }
        }

        /* access modifiers changed from: private */
        public void logStateAndMessage(Message message, State state) {
            Log.d(SemWifiApPowerSaveImpl.TAG, " " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }

        /* access modifiers changed from: package-private */
        public String printTime() {
            return " rt=" + SystemClock.uptimeMillis() + "/" + SystemClock.elapsedRealtime();
        }

        /* access modifiers changed from: protected */
        public String getLogRecString(Message msg) {
            StringBuilder sb = new StringBuilder();
            sb.append(smToString(msg));
            sb.append(" ");
            sb.append(printTime());
            int i = msg.what;
            sb.append(" ");
            sb.append(Integer.toString(msg.arg1));
            sb.append(" ");
            sb.append(Integer.toString(msg.arg2));
            return sb.toString();
        }

        /* access modifiers changed from: package-private */
        public String smToString(Message message) {
            return smToString(message.what);
        }

        /* access modifiers changed from: package-private */
        public String smToString(int what) {
            switch (what) {
                case 0:
                    return "CMD_TIMEOUT";
                case 1:
                    return "CMD_PLUGGED_OUT";
                case 2:
                    return "CMD_PLUGGED_IN";
                case 3:
                    return "CMD_NUM_ASSOCIATED_STATIONS_CHANGED";
                case 4:
                    return "CMD_AP_DISABLED";
                case 5:
                    return "CMD_LCD_ON";
                case 6:
                    return "CMD_LCD_OFF";
                case 7:
                    return "CMD_TIMEOUT_PACKET_CHECK";
                default:
                    return "what:" + Integer.toString(what);
            }
        }

        SoftApPowerSaveStateMachine() {
            super(SemWifiApPowerSaveImpl.TAG, Looper.getMainLooper());
            addState(this.mDefaultState);
            addState(this.mReadyOneState);
            addState(this.mReadyTwoState);
            addState(this.mReadyThreeState);
            addState(this.mPowerSaveState);
            addState(this.mPowerSaveAggressiveState);
            addState(this.mPowerSaveNoBeaconModeState);
            setLogRecSize(100);
            if (SemWifiApPowerSaveImpl.this.mUSBpuggedin) {
                setInitialState(this.mDefaultState);
            } else {
                setInitialState(this.mReadyOneState);
            }
        }

        private class DefaultState extends State {
            private DefaultState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "DefaultState Enter");
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 1:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected > 0) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mReadyTwoState);
                            return true;
                        }
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mReadyOneState);
                        return true;
                    case 3:
                        int unused = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                        if (SemWifiApPowerSaveImpl.this.mUSBpuggedin || SemWifiApPowerSaveImpl.this.NumOfClientsConnected <= 0) {
                            return true;
                        }
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mReadyOneState);
                        return true;
                    default:
                        return true;
                }
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "DefaultState exit");
            }
        }

        private class ReadyOneState extends State {
            private ReadyOneState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyOneState Enter");
                boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                long unused2 = SemWifiApPowerSaveImpl.this.mTimeofPowersave = SystemClock.elapsedRealtime();
                if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 300000);
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 0:
                        boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = false;
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mPowerSaveState);
                        return true;
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mDefaultState);
                        return true;
                    case 3:
                        int tempNumOfClientsConnected = SemWifiApPowerSaveImpl.this.NumOfClientsConnected;
                        int unused2 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                        int unused3 = SemWifiApPowerSaveImpl.this.mMacMaxClient = Settings.Secure.getInt(SemWifiApPowerSaveImpl.this.mContext.getContentResolver(), "wifi_ap_number_of_max_macaddr_client", -1);
                        Log.d(SemWifiApPowerSaveImpl.TAG, "mMaxClient : " + SemWifiApPowerSaveImpl.this.mMaxClient + " mMacMaxClient : " + SemWifiApPowerSaveImpl.this.mMacMaxClient);
                        if (message.arg1 == SemWifiApPowerSaveImpl.this.mMaxClient || (SemWifiApPowerSaveImpl.this.mMacAddrAcl != 3 && SemWifiApPowerSaveImpl.this.mMacMaxClient == SemWifiApPowerSaveImpl.this.NumOfClientsConnected)) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mPowerSaveState);
                            return true;
                        } else if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected < tempNumOfClientsConnected || SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine4 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine4.transitionTo(softApPowerSaveStateMachine4.mReadyOneState);
                            return true;
                        } else {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine5 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine5.transitionTo(softApPowerSaveStateMachine5.mReadyTwoState);
                            return true;
                        }
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine == null) {
                            return true;
                        }
                        SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                        return true;
                    case 6:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected != 0) {
                            return true;
                        }
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine6 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine6.transitionTo(softApPowerSaveStateMachine6.mReadyThreeState);
                        return true;
                    default:
                        return true;
                }
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyOneState exit");
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                SoftApPowerSaveStateMachine.this.sendBigdataForHotspotPowerSave("ReadyOneState");
            }
        }

        private class ReadyTwoState extends State {
            private ReadyTwoState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyTwoState Enter");
                boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                long unused2 = SemWifiApPowerSaveImpl.this.mTimeofPowersave = SystemClock.elapsedRealtime();
                if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 60000);
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 0:
                        boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = false;
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mPowerSaveState);
                        return true;
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mDefaultState);
                        return true;
                    case 3:
                        int tempNumOfClientsConnected = SemWifiApPowerSaveImpl.this.NumOfClientsConnected;
                        int unused2 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                        int unused3 = SemWifiApPowerSaveImpl.this.mMacMaxClient = Settings.Secure.getInt(SemWifiApPowerSaveImpl.this.mContext.getContentResolver(), "wifi_ap_number_of_max_macaddr_client", -1);
                        if (message.arg1 == SemWifiApPowerSaveImpl.this.mMaxClient || (SemWifiApPowerSaveImpl.this.mMacAddrAcl != 3 && SemWifiApPowerSaveImpl.this.mMacMaxClient == SemWifiApPowerSaveImpl.this.NumOfClientsConnected)) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mPowerSaveState);
                            return true;
                        } else if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected < tempNumOfClientsConnected || SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine4 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine4.transitionTo(softApPowerSaveStateMachine4.mReadyOneState);
                            return true;
                        } else {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine5 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine5.transitionTo(softApPowerSaveStateMachine5.mReadyTwoState);
                            return true;
                        }
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine == null) {
                            return true;
                        }
                        SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                        return true;
                    case 6:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected != 0) {
                            return true;
                        }
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine6 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine6.transitionTo(softApPowerSaveStateMachine6.mReadyThreeState);
                        return true;
                    default:
                        return true;
                }
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyTwoState exit");
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                SoftApPowerSaveStateMachine.this.sendBigdataForHotspotPowerSave("ReadyTwoState");
            }
        }

        private class ReadyThreeState extends State {
            private ReadyThreeState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyThreeState Enter");
                boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                long unused2 = SemWifiApPowerSaveImpl.this.mTimeofPowersave = SystemClock.elapsedRealtime();
                if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 50000);
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 0:
                        boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = false;
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mPowerSaveState);
                        return true;
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mDefaultState);
                        return true;
                    case 3:
                        int tempNumOfClientsConnected = SemWifiApPowerSaveImpl.this.NumOfClientsConnected;
                        int unused2 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                        int unused3 = SemWifiApPowerSaveImpl.this.mMacMaxClient = Settings.Secure.getInt(SemWifiApPowerSaveImpl.this.mContext.getContentResolver(), "wifi_ap_number_of_max_macaddr_client", -1);
                        if (message.arg1 == SemWifiApPowerSaveImpl.this.mMaxClient || (SemWifiApPowerSaveImpl.this.mMacAddrAcl != 3 && SemWifiApPowerSaveImpl.this.mMacMaxClient == SemWifiApPowerSaveImpl.this.NumOfClientsConnected)) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mPowerSaveState);
                            return true;
                        } else if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected < tempNumOfClientsConnected || SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine4 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine4.transitionTo(softApPowerSaveStateMachine4.mReadyOneState);
                            return true;
                        } else {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine5 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine5.transitionTo(softApPowerSaveStateMachine5.mReadyTwoState);
                            return true;
                        }
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine == null) {
                            return true;
                        }
                        SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                        return true;
                    case 5:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine6 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine6.transitionTo(softApPowerSaveStateMachine6.mReadyOneState);
                        return true;
                    default:
                        return true;
                }
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "ReadyThreeState exit");
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                SoftApPowerSaveStateMachine.this.sendBigdataForHotspotPowerSave("ReadyThreeState");
            }
        }

        private class PowerSaveState extends State {
            private int mPreviousMcBcastPacket;
            private int mPreviousRtpacket;
            private int packetOfBCMC;
            private int packetOfRetry;

            private PowerSaveState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveState Enter");
                this.mPreviousRtpacket = 0;
                this.mPreviousMcBcastPacket = 0;
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApRadioPowerSave(SemWifiApPowerSaveImpl.this.mApInterfaceName, true);
                }
                if (SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage != null) {
                    boolean unused = SemWifiApPowerSaveImpl.this.mPacketScheduled = true;
                    SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 10000);
                }
                if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null && !SemWifiApPowerSaveImpl.this.mIsLcdOn) {
                    boolean unused2 = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 100000);
                }
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null && SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, true);
                    boolean unused3 = SemWifiApPowerSaveImpl.this.mElnaEnable = true;
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 0:
                        boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = false;
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mPowerSaveAggressiveState);
                        break;
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mDefaultState);
                        break;
                    case 3:
                        if (SemWifiApPowerSaveImpl.this.mWifiNative != null && SemWifiApPowerSaveImpl.this.NumOfClientsConnected > 0 && SemWifiApPowerSaveImpl.this.mElnaEnable) {
                            SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                        }
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected >= message.arg1) {
                            if (message.arg1 == 0) {
                                int unused2 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                                SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                                softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mReadyOneState);
                                break;
                            }
                        } else {
                            int unused3 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine4 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine4.transitionTo(softApPowerSaveStateMachine4.mReadyTwoState);
                            break;
                        }
                        break;
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                            SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                            break;
                        }
                        break;
                    case 5:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected != 0) {
                            if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected > 0) {
                                SoftApPowerSaveStateMachine softApPowerSaveStateMachine5 = SoftApPowerSaveStateMachine.this;
                                softApPowerSaveStateMachine5.transitionTo(softApPowerSaveStateMachine5.mReadyTwoState);
                                break;
                            }
                        } else {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine6 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine6.transitionTo(softApPowerSaveStateMachine6.mReadyOneState);
                            break;
                        }
                        break;
                    case 6:
                        boolean unused4 = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                        if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null && SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                            SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 40000);
                            break;
                        }
                    case 7:
                        boolean unused5 = SemWifiApPowerSaveImpl.this.mPacketScheduled = false;
                        String staList = null;
                        if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                            staList = SemWifiApPowerSaveImpl.this.mWifiNative.semGetStationInfo("ALL");
                        }
                        if (staList != null) {
                            String[] part = staList.split("=|\\s");
                            try {
                                this.packetOfRetry = Integer.parseInt(part[3]);
                                this.packetOfBCMC = Integer.parseInt(part[5]);
                            } catch (NumberFormatException e) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "NumberFormatException occurs");
                                this.mPreviousRtpacket = 0;
                                this.mPreviousMcBcastPacket = 0;
                                break;
                            } catch (ArrayIndexOutOfBoundsException e2) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "ArrayIndexOutOfBoundsException occurs");
                                this.mPreviousRtpacket = 0;
                                this.mPreviousMcBcastPacket = 0;
                                break;
                            }
                        }
                        int mPacketOfRetry = this.packetOfRetry - this.mPreviousRtpacket;
                        int mPacketOfBCMC = this.packetOfBCMC - this.mPreviousMcBcastPacket;
                        Log.d(SemWifiApPowerSaveImpl.TAG, "packetOfRetry - mPreviousRtpacket : " + mPacketOfRetry + " mPreviousMcBcastPacket-packetOfBCMC : " + mPacketOfBCMC);
                        if (!(this.mPreviousRtpacket != 0 && mPacketOfRetry > 20) && !(this.mPreviousMcBcastPacket != 0 && mPacketOfBCMC > 20)) {
                            this.mPreviousRtpacket = this.packetOfRetry;
                            this.mPreviousMcBcastPacket = this.packetOfBCMC;
                            boolean unused6 = SemWifiApPowerSaveImpl.this.mPacketScheduled = true;
                            if (SemWifiApPowerSaveImpl.this.mIsEnabledSoftAp) {
                                SoftApPowerSaveStateMachine.this.sendMessageDelayed(7, 10000);
                                break;
                            }
                        } else {
                            Log.d(SemWifiApPowerSaveImpl.TAG, "by amount packet");
                            this.mPreviousRtpacket = 0;
                            this.mPreviousMcBcastPacket = 0;
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine7 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine7.transitionTo(softApPowerSaveStateMachine7.mReadyTwoState);
                            break;
                        }
                        break;
                }
                return true;
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveState exit");
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApRadioPowerSave(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                }
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                if (SemWifiApPowerSaveImpl.this.mPacketScheduled && SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage.cancel();
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(0)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(0);
                }
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null && SemWifiApPowerSaveImpl.this.mElnaEnable) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                    boolean unused = SemWifiApPowerSaveImpl.this.mElnaEnable = false;
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(7)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(7);
                }
            }
        }

        private class PowerSaveAggressiveState extends State {
            private int mPreviousMcBcastPacket;
            private int mPreviousRtpacket;
            private int packetOfBCMC;
            private int packetOfRetry;

            private PowerSaveAggressiveState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveAggressiveState Enter");
                this.mPreviousRtpacket = 0;
                this.mPreviousMcBcastPacket = 0;
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApRadioPowerSaveAggressive(SemWifiApPowerSaveImpl.this.mApInterfaceName, true);
                }
                if (SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage != null) {
                    boolean unused = SemWifiApPowerSaveImpl.this.mPacketScheduled = true;
                    SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 10000);
                }
                if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null && !SemWifiApPowerSaveImpl.this.mIsLcdOn) {
                    boolean unused2 = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 45000);
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 0:
                        boolean unused = SemWifiApPowerSaveImpl.this.mStateScheduled = false;
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                        }
                        break;
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mDefaultState);
                        break;
                    case 3:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected >= message.arg1) {
                            if (message.arg1 == 0) {
                                int unused2 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                                SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                                softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mReadyOneState);
                                break;
                            }
                        } else {
                            int unused3 = SemWifiApPowerSaveImpl.this.NumOfClientsConnected = message.arg1;
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine3 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine3.transitionTo(softApPowerSaveStateMachine3.mReadyTwoState);
                            break;
                        }
                        break;
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine != null) {
                            SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                            break;
                        }
                        break;
                    case 5:
                        if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected != 0) {
                            if (SemWifiApPowerSaveImpl.this.NumOfClientsConnected > 0) {
                                SoftApPowerSaveStateMachine softApPowerSaveStateMachine4 = SoftApPowerSaveStateMachine.this;
                                softApPowerSaveStateMachine4.transitionTo(softApPowerSaveStateMachine4.mReadyTwoState);
                                break;
                            }
                        } else {
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine5 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine5.transitionTo(softApPowerSaveStateMachine5.mReadyOneState);
                            break;
                        }
                        break;
                    case 6:
                        boolean unused4 = SemWifiApPowerSaveImpl.this.mStateScheduled = true;
                        if (SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null && SemWifiApPowerSaveImpl.this.NumOfClientsConnected == 0) {
                            SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + 40000);
                            break;
                        }
                    case 7:
                        boolean unused5 = SemWifiApPowerSaveImpl.this.mPacketScheduled = false;
                        String staList = null;
                        if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                            staList = SemWifiApPowerSaveImpl.this.mWifiNative.semGetStationInfo("ALL");
                        }
                        if (staList != null) {
                            String[] part = staList.split("=|\\s");
                            try {
                                this.packetOfRetry = Integer.parseInt(part[3]);
                                this.packetOfBCMC = Integer.parseInt(part[5]);
                            } catch (NumberFormatException e) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "NumberFormatException occurs");
                                this.mPreviousRtpacket = 0;
                                this.mPreviousMcBcastPacket = 0;
                                break;
                            } catch (ArrayIndexOutOfBoundsException e2) {
                                Log.d(SemWifiApPowerSaveImpl.TAG, "ArrayIndexOutOfBoundsException occurs");
                                this.mPreviousRtpacket = 0;
                                this.mPreviousMcBcastPacket = 0;
                                break;
                            }
                        }
                        int mPacketOfRetry = this.packetOfRetry - this.mPreviousRtpacket;
                        int mPacketOfBCMC = this.packetOfBCMC - this.mPreviousMcBcastPacket;
                        Log.d(SemWifiApPowerSaveImpl.TAG, "packetOfRetry - mPreviousRtpacket : " + mPacketOfRetry + " mPreviousMcBcastPacket-packetOfBCMC : " + mPacketOfBCMC);
                        if (!(this.mPreviousRtpacket != 0 && mPacketOfRetry > 15) && !(this.mPreviousMcBcastPacket != 0 && mPacketOfBCMC > 15)) {
                            if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                                String getTime = SemWifiApPowerSaveImpl.this.mWifiNative.getPstime(SemWifiApPowerSaveImpl.this.mApInterfaceName);
                                Log.d(SemWifiApPowerSaveImpl.TAG, "getTime : " + getTime);
                                if (getTime != null) {
                                    String[] part2 = getTime.split("=|\\s");
                                    try {
                                        if (Integer.parseInt(part2[1]) == 1 && !SemWifiApPowerSaveImpl.this.mElnaEnable) {
                                            SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, true);
                                            boolean unused6 = SemWifiApPowerSaveImpl.this.mElnaEnable = true;
                                        } else if (Integer.parseInt(part2[1]) == 0 && SemWifiApPowerSaveImpl.this.mElnaEnable) {
                                            SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                                            boolean unused7 = SemWifiApPowerSaveImpl.this.mElnaEnable = false;
                                        }
                                    } catch (NumberFormatException e3) {
                                        Log.d(SemWifiApPowerSaveImpl.TAG, "NumberFormatException occurs");
                                    } catch (ArrayIndexOutOfBoundsException e4) {
                                        Log.d(SemWifiApPowerSaveImpl.TAG, "ArrayIndexOutOfBoundsException occurs");
                                    }
                                }
                            }
                            this.mPreviousRtpacket = this.packetOfRetry;
                            this.mPreviousMcBcastPacket = this.packetOfBCMC;
                            boolean unused8 = SemWifiApPowerSaveImpl.this.mPacketScheduled = true;
                            if (SemWifiApPowerSaveImpl.this.mIsEnabledSoftAp) {
                                SoftApPowerSaveStateMachine.this.sendMessageDelayed(7, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
                                break;
                            }
                        } else {
                            Log.d(SemWifiApPowerSaveImpl.TAG, "by amount packet");
                            this.mPreviousRtpacket = 0;
                            this.mPreviousMcBcastPacket = 0;
                            SoftApPowerSaveStateMachine softApPowerSaveStateMachine6 = SoftApPowerSaveStateMachine.this;
                            softApPowerSaveStateMachine6.transitionTo(softApPowerSaveStateMachine6.mReadyTwoState);
                            break;
                        }
                        break;
                }
                return true;
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveAggressiveState exit");
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null && SemWifiApPowerSaveImpl.this.mElnaEnable) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApElnaEnable(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                    boolean unused = SemWifiApPowerSaveImpl.this.mElnaEnable = false;
                }
                if (SemWifiApPowerSaveImpl.this.mPacketScheduled && SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage.cancel();
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(0)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(0);
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(7)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(7);
                }
            }
        }

        private class PowerSaveNoBeaconModeState extends State {
            private PowerSaveNoBeaconModeState() {
            }

            public void enter() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveNoBeaconModeState Enter");
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApRadioPowerSaveNoBeacon(SemWifiApPowerSaveImpl.this.mApInterfaceName, true);
                }
            }

            public boolean processMessage(Message message) {
                SoftApPowerSaveStateMachine.this.logStateAndMessage(message, this);
                switch (message.what) {
                    case 2:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine.transitionTo(softApPowerSaveStateMachine.mDefaultState);
                        return true;
                    case 4:
                        SoftApPowerSaveStateMachine.this.getPsTimeFuntion();
                        if (SemWifiApPowerSaveImpl.this.mStateMachine == null) {
                            return true;
                        }
                        SemWifiApPowerSaveImpl.this.mStateMachine.quitNow();
                        return true;
                    case 5:
                        SoftApPowerSaveStateMachine softApPowerSaveStateMachine2 = SoftApPowerSaveStateMachine.this;
                        softApPowerSaveStateMachine2.transitionTo(softApPowerSaveStateMachine2.mReadyOneState);
                        return true;
                    default:
                        return true;
                }
            }

            public void exit() {
                Log.d(SemWifiApPowerSaveImpl.TAG, "PowerSaveNoBeaconModeState exit");
                if (SemWifiApPowerSaveImpl.this.mWifiNative != null) {
                    SemWifiApPowerSaveImpl.this.mWifiNative.semSetSoftApRadioPowerSaveNoBeacon(SemWifiApPowerSaveImpl.this.mApInterfaceName, false);
                }
                if (SemWifiApPowerSaveImpl.this.mStateScheduled && SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApTimeoutMessage.cancel();
                }
                if (SemWifiApPowerSaveImpl.this.mPacketScheduled && SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage != null) {
                    SemWifiApPowerSaveImpl.this.mSoftApPacketCheckTimeoutMessage.cancel();
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(0)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(0);
                }
                if (SoftApPowerSaveStateMachine.this.hasMessages(7)) {
                    SoftApPowerSaveStateMachine.this.removeMessages(7);
                }
            }
        }
    }
}
