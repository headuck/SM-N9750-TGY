package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.WifiNative;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.server.wifi.softap.SemWifiApClientInfo;
import com.samsung.android.server.wifi.softap.SemWifiApMonitor;
import com.samsung.android.server.wifi.softap.SemWifiApPowerSaveImpl;
import com.samsung.android.server.wifi.softap.SemWifiApTimeOutImpl;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SoftApManager implements ActiveModeManager {
    private static final String CONFIGOPBRANDING_CSC = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    private static final boolean DBG_PRODUCT_DEV = Debug.semIsProductDev();
    private static final boolean MHSDBG = ("eng".equals(Build.TYPE) || Debug.semIsProductDev());
    public static final int MHS_NR_MMWAVE_SAR_BACKOFF_DISABLED = 3;
    public static final int MHS_NR_MMWAVE_SAR_BACKOFF_ENABLED = 4;
    public static final int MHS_NR_SUB6_SAR_BACKOFF_DISABLED = 5;
    public static final int MHS_NR_SUB6_SAR_BACKOFF_ENABLED = 6;
    private static final int MIN_SOFT_AP_TIMEOUT_DELAY_MS = 600000;
    @VisibleForTesting
    public static final String SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG = "SoftApManager Soft AP Send Message Timeout";
    public static final boolean SUPPORTCONCURRENCYEFEATURE_SPF = false;
    public static final boolean SUPPORTDONGLEFEATURE_SPF = false;
    public static final boolean SUPPORTMOBILEAPONTRIGGER_CSC = SemCscFeature.getInstance().getBoolean(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_SUPPORTMOBILEAPONTRIGGER, false);
    public static final boolean SUPPORTMOBILEAPONTRIGGER_SPF = true;
    private static final String TAG = "SoftApManager";
    private static boolean bIsFirstCall = false;
    private static boolean bUseMobileData = false;
    private static long mAmountMobileRxBytes;
    private static long mAmountMobileTxBytes;
    private static long mAmountTimeOfMobileData;
    private static long mBaseRxBytes;
    private static long mBaseTxBytes = 0;
    private static long mStartTimeOfMobileData;
    private static TelephonyManager mTelephonyManagerForHotspot = null;
    private static PhoneStateListener mTelephonyPhoneStateListener = null;
    private static long mTempMobileRxBytes;
    private static long mTempMobileTxBytes;
    private static long mTimeOfStartMobileAp;
    private WifiConfiguration localConfig;
    /* access modifiers changed from: private */
    public WifiConfiguration mApConfig;
    /* access modifiers changed from: private */
    public String mApInterfaceName;
    private final IBatteryStats mBatteryStats;
    /* access modifiers changed from: private */
    public final WifiManager.SoftApCallback mCallback;
    /* access modifiers changed from: private */
    public final Context mContext;
    private String mCountryCode;
    /* access modifiers changed from: private */
    public final FrameworkFacade mFrameworkFacade;
    /* access modifiers changed from: private */
    public Timer mFwLogTimer = null;
    /* access modifiers changed from: private */
    public boolean mIfaceIsDestroyed;
    /* access modifiers changed from: private */
    public boolean mIfaceIsUp;
    /* access modifiers changed from: private */
    public final int mMode;
    /* access modifiers changed from: private */
    public int mNumAssociatedStations = 0;
    private INetworkManagementService mNwService = null;
    private int mPreviousTetherData = 0;
    private int mRVFMode = 0;
    /* access modifiers changed from: private */
    public int mReportedBandwidth = -1;
    /* access modifiers changed from: private */
    public int mReportedFrequency = -1;
    /* access modifiers changed from: private */
    public final SarManager mSarManager;
    /* access modifiers changed from: private */
    public SemWifiApClientInfo mSemWifiApClientInfo;
    private SemWifiApMonitor mSemWifiApMonitor;
    /* access modifiers changed from: private */
    public SemWifiApPowerSaveImpl mSemWifiApPowerSaveImpl;
    /* access modifiers changed from: private */
    public SemWifiApTimeOutImpl mSemWifiApTimeOutImpl;
    private final WifiNative.SoftApListener mSoftApListener = new WifiNative.SoftApListener() {
        public void onFailure() {
            SoftApManager.this.mStateMachine.sendMessage(2);
        }

        public void onNumAssociatedStationsChanged(int numStations) {
            SoftApManager.this.mStateMachine.sendMessage(4, numStations);
        }

        public void onSoftApChannelSwitched(int frequency, int bandwidth) {
            SoftApManager.this.mStateMachine.sendMessage(9, frequency, bandwidth);
        }
    };
    private long mStartTimestamp = -1;
    /* access modifiers changed from: private */
    public final SoftApStateMachine mStateMachine;
    /* access modifiers changed from: private */
    public boolean mTimeoutEnabled = false;
    private final WifiApConfigStore mWifiApConfigStore;
    /* access modifiers changed from: private */
    public final WifiMetrics mWifiMetrics;
    /* access modifiers changed from: private */
    public final WifiNative mWifiNative;

    public SoftApManager(Context context, Looper looper, FrameworkFacade framework, WifiNative wifiNative, String countryCode, WifiManager.SoftApCallback callback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration apConfig, WifiMetrics wifiMetrics, SarManager sarManager) {
        this.mContext = context;
        this.mFrameworkFacade = framework;
        this.mWifiNative = wifiNative;
        this.mCountryCode = countryCode;
        this.mCallback = callback;
        this.mWifiApConfigStore = wifiApConfigStore;
        this.mMode = apConfig.getTargetMode();
        WifiConfiguration config = apConfig.getWifiConfiguration();
        if (config == null) {
            this.mApConfig = this.mWifiApConfigStore.getApConfiguration();
        } else {
            this.mApConfig = config;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mSarManager = sarManager;
        this.mStateMachine = new SoftApStateMachine(looper);
        this.mSemWifiApTimeOutImpl = WifiInjector.getInstance().getSemWifiApTimeOutImpl();
        this.mSemWifiApClientInfo = WifiInjector.getInstance().getSemWifiApClientInfo();
        this.mSemWifiApPowerSaveImpl = WifiInjector.getInstance().getSemWifiApPowerSaveImpl();
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mFrameworkFacade.getService("batterystats"));
    }

    public void start() {
        this.mStateMachine.sendMessage(0, this.mApConfig);
    }

    public void stop() {
        Log.d(TAG, " currentstate: " + getCurrentStateName());
        if (this.mApInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateApState(10, 13, 0);
            } else {
                updateApState(10, 12, 0);
            }
        }
        this.mStateMachine.quitNow();
    }

    public int getScanMode() {
        return 0;
    }

    public int getIpMode() {
        return this.mMode;
    }

    public void setRvfMode(int value) {
        this.mRVFMode = value;
    }

    public int getRvfMode() {
        return this.mRVFMode;
    }

    public String sendHistoricalDumplog() {
        StringBuffer logstr = new StringBuffer();
        int size = this.mStateMachine.getLogRecSize();
        logstr.append(new String("SoftApStateMachine : size=" + size + " total records= " + this.mStateMachine.getLogRecCount()));
        logstr.append("\n");
        for (int i = 0; i < size; i++) {
            StateMachine.LogRec lr = this.mStateMachine.getLogRec(i);
            logstr.append(" rec[" + i + "]: ");
            if (lr == null) {
                logstr.append(" null");
            } else {
                logstr.append(new String(lr.toString()));
            }
            logstr.append("\n");
        }
        return logstr.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("--Dump of SoftApManager--");
        pw.println("current StateMachine mode: " + getCurrentStateName());
        pw.println("mApInterfaceName: " + this.mApInterfaceName);
        pw.println("mIfaceIsUp: " + this.mIfaceIsUp);
        pw.println("mMode: " + this.mMode);
        pw.println("mCountryCode: " + this.mCountryCode);
        if (this.mApConfig != null) {
            pw.println("mApConfig.SSID: " + this.mApConfig.SSID);
            pw.println("mApConfig.apBand: " + this.mApConfig.apBand);
            pw.println("mApConfig.hiddenSSID: " + this.mApConfig.hiddenSSID);
        } else {
            pw.println("mApConfig: null");
        }
        pw.println("mNumAssociatedStations: " + this.mNumAssociatedStations);
        pw.println("mTimeoutEnabled: " + this.mTimeoutEnabled);
        pw.println("mReportedFrequency: " + this.mReportedFrequency);
        pw.println("mReportedBandwidth: " + this.mReportedBandwidth);
        pw.println("mStartTimestamp: " + this.mStartTimestamp);
        WifiInjector.getInstance().getSemWifiApClientInfo().dump(fd, pw, args);
        pw.println("total records=" + this.mStateMachine.getLogRecCount());
        for (int i = 0; i < this.mStateMachine.getLogRecSize(); i++) {
            if (this.mStateMachine.getLogRec(i) == null) {
                pw.println(" rec[" + i + "]:  null");
            } else {
                pw.println(" rec[" + i + "]: " + this.mStateMachine.getLogRec(i).toString());
            }
            pw.flush();
        }
        runFwDump();
    }

    private void runFwDump() {
        Log.i(TAG, "runFwDump");
        if (((WifiManager) this.mContext.getSystemService("wifi")).getWifiState() != 3) {
            this.mWifiNative.saveDebugDumpForHotspot();
            runFwLogTimer();
        }
    }

    private void runFwLogTimer() {
        Log.i(TAG, "runFwLogTimer");
        if (!DBG_PRODUCT_DEV) {
            if (this.mFwLogTimer != null) {
                Log.i(TAG, "mFwLogTimer timer cancled");
                this.mFwLogTimer.cancel();
            }
            this.mFwLogTimer = new Timer();
            this.mFwLogTimer.schedule(new TimerTask() {
                public void run() {
                    Log.i(SoftApManager.TAG, "mFwLogTimer timer expired - start folder initialization");
                    SoftApManager.this.resetFwLogFolder();
                    Timer unused = SoftApManager.this.mFwLogTimer = null;
                }
            }, WifiDiagnostics.MIN_DUMP_TIME_WINDOW_MILLIS);
        }
    }

    /* access modifiers changed from: private */
    public void resetFwLogFolder() {
        if (!DBG_PRODUCT_DEV) {
            Log.i(TAG, "resetFwLogFolder");
            try {
                File folder = new File("/data/log/wifi/");
                if (folder.exists()) {
                    removeFolderFiles(folder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void removeFolderFiles(File folder) {
        try {
            File[] logFiles = folder.listFiles();
            if (logFiles != null && logFiles.length > 0) {
                for (File logFile : logFiles) {
                    Log.i(TAG, "SoftApManager : " + logFile + " deleted");
                    logFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    /* access modifiers changed from: private */
    public void updateApState(int newState, int currentState, int reason) {
        this.mCallback.onStateChanged(newState, reason);
        Log.d(TAG, "setWifiApState: " + newState);
        if (newState == 13) {
            try {
                this.mBatteryStats.noteWifiOn();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to note battery stats in wifi");
            }
        } else if (newState == 11) {
            this.mBatteryStats.noteWifiOff();
            setRvfMode(0);
        }
        if (mTelephonyManagerForHotspot == null) {
            mTelephonyManagerForHotspot = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (newState == 13) {
            ServiceState mServiceState = mTelephonyManagerForHotspot.getServiceState();
            if (mServiceState != null && mServiceState.getNrBearerStatus() == 1) {
                Log.i(TAG, "in 5G NR_5G_BEARER_STATUS_ALLOCATED mode");
                this.mWifiNative.setHotspotBackOff(6);
            } else if (mServiceState != null && mServiceState.getNrBearerStatus() == 2) {
                Log.i(TAG, "in 5G NR_5G_BEARER_STATUS_MMW_ALLOCATED mode");
                this.mWifiNative.setHotspotBackOff(4);
            }
        }
        boolean tempBackOff = false;
        if (new File("/sdcard/mhsbackoff").exists()) {
            tempBackOff = true;
        }
        if (MHSDBG) {
            Log.d(TAG, "MHS SPF backoff : true MHS CSC backoff : " + SUPPORTMOBILEAPONTRIGGER_CSC + ", tempBackOff : " + tempBackOff);
        }
        if (("".equals(SystemProperties.get("vold.encrypt_progress")) || (MHSDBG && tempBackOff)) && (newState == 13 || newState == 11 || newState == 14)) {
            boolean backOffState = false;
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (newState == 13) {
                backOffState = true;
            } else if (newState == 11 || newState == 14) {
                backOffState = false;
            }
            Log.i(TAG, syncGetWifiApStateByName(newState) + ", new api SAR backOffState = " + backOffState);
            try {
                phone.setTransmitPowerWithFlag(4, backOffState);
            } catch (NullPointerException e2) {
                Log.i(TAG, "NullPointerException, as ITelephony object is null");
            } catch (RemoteException e3) {
                Log.i(TAG, "RemoteException occurs in setTransmitPowerWithFlag");
            }
        }
        Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", newState);
        intent.putExtra("previous_wifi_state", currentState);
        if (newState == 14) {
            intent.putExtra("wifi_ap_error_code", reason);
        }
        intent.putExtra("wifi_ap_interface_name", this.mApInterfaceName);
        intent.putExtra("wifi_ap_mode", this.mMode);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if ("TMK".equals(CONFIGOPBRANDING_CSC) || "MTR".equals(CONFIGOPBRANDING_CSC)) {
            intent.setClassName("com.samsung.android.app.mhswrappermtr", "com.samsung.android.app.mhswrappermtr.MHSWidget");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else if ("TMB".equals(CONFIGOPBRANDING_CSC)) {
            intent.setClassName("com.samsung.android.app.mhswrappertmo", "com.samsung.android.app.mhswrappertmo.MHSWidget");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: private */
    public int startSoftAp(WifiConfiguration config) {
        if (config == null || config.SSID == null) {
            Log.e(TAG, "Unable to start soft AP without valid configuration");
            return 2;
        }
        WifiConfiguration localConfig2 = new WifiConfiguration(config);
        checkSoftApRVFmode(true);
        if (this.mCountryCode == null) {
            this.mCountryCode = SystemProperties.get("ro.csc.countryiso_code", "JP");
            this.mCountryCode = this.mCountryCode.toUpperCase(Locale.ROOT);
            Log.i(TAG, "set country code : " + this.mCountryCode);
        }
        WifiInjector.getInstance().getSemSoftapConfig().get(this.mApInterfaceName, localConfig2, this.mWifiNative, this.mCountryCode);
        if (!this.mWifiNative.startSoftAp(this.mApInterfaceName, localConfig2, this.mSoftApListener)) {
            Log.e(TAG, "Soft AP start failed");
            return 2;
        }
        this.mStartTimestamp = SystemClock.elapsedRealtime();
        Log.d(TAG, "Soft AP is started");
        return 0;
    }

    /* access modifiers changed from: private */
    public void stopSoftAp() {
        this.mWifiNative.teardownInterface(this.mApInterfaceName);
        Log.d(TAG, "Soft AP is stopped");
    }

    private void checkSoftApRVFmode(boolean enabled) {
        if (enabled) {
            int i = this.mRVFMode;
            if (i == 0) {
                if ("1".equals(SystemProperties.get("net.forward.disable"))) {
                    SystemProperties.set("net.forward.disable", "");
                }
                if ("100".equals(SystemProperties.get("net.leasetime"))) {
                    SystemProperties.set("net.leasetime", "");
                }
            } else if (i > 0) {
                SystemProperties.set("net.forward.disable", "1");
                SystemProperties.set("net.leasetime", "100");
                if (MHSDBG) {
                    Log.d(TAG, "net.forward.disable = 1, net.leasetime = 100");
                }
            }
        } else {
            if ("1".equals(SystemProperties.get("net.forward.disable"))) {
                SystemProperties.set("net.forward.disable", "");
                if (MHSDBG) {
                    Log.d(TAG, "net.forward.disable = null");
                }
            }
            if ("100".equals(SystemProperties.get("net.leasetime"))) {
                SystemProperties.set("net.leasetime", "");
                if (MHSDBG) {
                    Log.d(TAG, "net.leasetime = null");
                }
            }
            this.mRVFMode = 0;
            if (this.mPreviousTetherData == 1) {
                SystemProperties.set("persist.sys.tether_data", "1");
                this.mPreviousTetherData = 0;
                if (MHSDBG) {
                    Log.d(TAG, "persist.sys.tether_data = 1");
                }
            }
        }
    }

    public void setSoftApReset() {
        this.mStateMachine.sendMessage(10);
    }

    private boolean checkMobileApWifiChannel() {
        WifiApConfigStore mWifiApConfigStore2 = WifiInjector.getInstance().getWifiApConfigStore();
        WifiConfiguration tempWifiConfig = mWifiApConfigStore2.getApConfiguration();
        int wifiFrequency = getWifiConnectedFrequency();
        if (getWifiConnectedBand(wifiFrequency) != getMobileApBand(tempWifiConfig)) {
            return false;
        }
        Log.d(TAG, "Wifi and MobileAp are in same band. Now we verify for channel");
        int operatingWifiChannel = getWifiConnectedChannel(wifiFrequency);
        if (operatingWifiChannel == -1 || tempWifiConfig.apChannel == operatingWifiChannel) {
            return false;
        }
        Log.d(TAG, "Wifi and MobileAp are in different channel. Reset MobileAp with Wifi Channel");
        tempWifiConfig.apChannel = operatingWifiChannel;
        mWifiApConfigStore2.setApConfiguration(tempWifiConfig);
        return true;
    }

    private int getWifiConnectedChannel(int frequency) {
        int channel;
        int freqDiff = frequency - 2412;
        if (freqDiff == 0) {
            return 1;
        }
        int channel2 = (freqDiff / 5) + 1;
        if (channel2 <= 11 || channel2 > 15) {
            channel = channel2;
        } else {
            channel = 0;
        }
        if (channel > 15) {
            return 149;
        }
        return channel;
    }

    private int getWifiConnectedBand(int WifiFrequency) {
        if (WifiFrequency < 2412 || WifiFrequency > 2484) {
            return 5;
        }
        return 2;
    }

    private int getWifiConnectedFrequency() {
        WifiInfo wifi = WifiInjector.getInstance().getClientModeImpl().syncRequestConnectionInfo();
        if (wifi == null || wifi.getNetworkId() == -1) {
            return -1;
        }
        return wifi.getFrequency();
    }

    private int getMobileApBand(WifiConfiguration wifiConfig) {
        if (wifiConfig.apChannel <= 14) {
            return 2;
        }
        return 5;
    }

    private String convertBytesToMegaByte(long tempValue) {
        long valueOfDevided = tempValue / 1048576;
        if (valueOfDevided >= ((long) 500)) {
            return "over" + 500 + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.9d) {
            return (((double) 500) * 0.9d) + "~" + 500 + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.8d) {
            return (((double) 500) * 0.8d) + "~" + (((double) 500) * 0.9d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.7d) {
            return (((double) 500) * 0.7d) + "~" + (((double) 500) * 0.8d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.6d) {
            return (((double) 500) * 0.6d) + "~" + (((double) 500) * 0.7d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.5d) {
            return (((double) 500) * 0.5d) + "~" + (((double) 500) * 0.6d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.4d) {
            return (((double) 500) * 0.4d) + "~" + (((double) 500) * 0.5d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.3d) {
            return (((double) 500) * 0.3d) + "~" + (((double) 500) * 0.4d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.2d) {
            return (((double) 500) * 0.2d) + "~" + (((double) 500) * 0.3d) + "MB";
        } else if (((double) valueOfDevided) >= ((double) 500) * 0.1d) {
            return (((double) 500) * 0.1d) + "~" + (((double) 500) * 0.2d) + "MB";
        } else {
            return "0~" + (((double) 500) * 0.1d) + "MB";
        }
    }

    private String convertBytesToMegaByteForLogging(long tempValue) {
        return "" + (tempValue / 1048576);
    }

    private String convertMinute(long tempValue) {
        long valueOfDevided = tempValue;
        if (valueOfDevided >= 120) {
            return (valueOfDevided / 60) + "hour";
        } else if (valueOfDevided >= 100) {
            return "100~120";
        } else {
            if (valueOfDevided >= 80) {
                return "80~100";
            }
            if (valueOfDevided >= 60) {
                return "60~80";
            }
            if (valueOfDevided >= 40) {
                return "40~60";
            }
            if (valueOfDevided >= 20) {
                return "20~40";
            }
            return "0~20";
        }
    }

    private void resetParameterForHotspotLogging() {
        mTelephonyPhoneStateListener = null;
        mAmountMobileTxBytes = 0;
        mAmountMobileRxBytes = 0;
        mTempMobileRxBytes = 0;
        mTempMobileTxBytes = 0;
        mAmountTimeOfMobileData = 0;
        mTempMobileTxBytes = 0;
        mTempMobileRxBytes = 0;
        bIsFirstCall = false;
        mBaseTxBytes = 0;
        mBaseRxBytes = 0;
        mTelephonyManagerForHotspot = null;
    }

    public String syncGetWifiApStateByName(int state) {
        switch (state) {
            case 10:
                return "disabling";
            case 11:
                return "disabled";
            case 12:
                return "enabling";
            case 13:
                return "enabled";
            case 14:
                return "failed";
            default:
                return "[invalid state]";
        }
    }

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_FAILURE = 2;
        public static final int CMD_INTERFACE_DESTROYED = 7;
        public static final int CMD_INTERFACE_DOWN = 8;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_NO_ASSOCIATED_STATIONS_TIMEOUT = 5;
        public static final int CMD_NUM_ASSOCIATED_STATIONS_CHANGED = 4;
        public static final int CMD_RESET = 10;
        public static final int CMD_SOFT_AP_CHANNEL_SWITCHED = 9;
        public static final int CMD_START = 0;
        public static final int CMD_TIMEOUT_TOGGLE_CHANGED = 6;
        /* access modifiers changed from: private */
        public final State mIdleState = new IdleState();
        /* access modifiers changed from: private */
        public final State mStartedState = new StartedState();
        /* access modifiers changed from: private */
        public final WifiNative.InterfaceCallback mWifiNativeInterfaceCallback = new WifiNative.InterfaceCallback() {
            public void onDestroyed(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(7);
                }
            }

            public void onUp(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(3, 1);
                }
            }

            public void onDown(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(3, 0);
                }
            }
        };

        SoftApStateMachine(Looper looper) {
            super(SoftApManager.TAG, looper);
            setLogRecSize(100);
            setLogOnlyTransitions(false);
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }

        private class IdleState extends State {
            private IdleState() {
            }

            public void enter() {
                String unused = SoftApManager.this.mApInterfaceName = null;
                boolean unused2 = SoftApManager.this.mIfaceIsUp = false;
                boolean unused3 = SoftApManager.this.mIfaceIsDestroyed = false;
            }

            public boolean processMessage(Message message) {
                if (message.what == 0) {
                    String unused = SoftApManager.this.mApInterfaceName = SoftApManager.this.mWifiNative.setupInterfaceForSoftApMode(SoftApStateMachine.this.mWifiNativeInterfaceCallback);
                    if (TextUtils.isEmpty(SoftApManager.this.mApInterfaceName)) {
                        Log.e(SoftApManager.TAG, "setup failure when creating ap interface.");
                        SoftApManager.this.updateApState(14, 11, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                    } else {
                        SoftApManager.this.updateApState(12, 11, 0);
                        SoftApManager.this.mSemWifiApClientInfo.startReceivingHostapdEvents(SoftApManager.this.mApInterfaceName);
                        int result = SoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                        if (result != 0) {
                            int failureReason = 0;
                            if (result == 1) {
                                failureReason = 1;
                            }
                            SoftApManager.this.updateApState(14, 12, failureReason);
                            SoftApManager.this.stopSoftAp();
                            SoftApManager.this.mSemWifiApClientInfo.stopReceivingEvents();
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, failureReason);
                        } else {
                            SoftApStateMachine softApStateMachine = SoftApStateMachine.this;
                            softApStateMachine.transitionTo(softApStateMachine.mStartedState);
                        }
                    }
                }
                return true;
            }
        }

        private class StartedState extends State {
            private SoftApTimeoutEnabledSettingObserver mSettingObserver;
            private WakeupMessage mSoftApTimeoutMessage;
            private int mTimeoutDelay;

            private StartedState() {
            }

            private class SoftApTimeoutEnabledSettingObserver extends ContentObserver {
                SoftApTimeoutEnabledSettingObserver(Handler handler) {
                    super(handler);
                }

                public void register() {
                    SoftApManager.this.mFrameworkFacade.registerContentObserver(SoftApManager.this.mContext, Settings.Global.getUriFor("soft_ap_timeout_enabled"), true, this);
                    boolean unused = SoftApManager.this.mTimeoutEnabled = getValue();
                }

                public void unregister() {
                    SoftApManager.this.mFrameworkFacade.unregisterContentObserver(SoftApManager.this.mContext, this);
                }

                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    SoftApManager.this.mStateMachine.sendMessage(6, getValue() ? 1 : 0);
                }

                private boolean getValue() {
                    boolean enabled = true;
                    if (SoftApManager.this.mFrameworkFacade.getIntegerSetting(SoftApManager.this.mContext, "soft_ap_timeout_enabled", 0) != 1) {
                        enabled = false;
                    }
                    return enabled;
                }
            }

            private int getConfigSoftApTimeoutDelay() {
                int delay = SoftApManager.this.mContext.getResources().getInteger(17695003);
                if (delay < SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS) {
                    delay = SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS;
                    Log.w(SoftApManager.TAG, "Overriding timeout delay with minimum limit value");
                }
                Log.d(SoftApManager.TAG, "Timeout delay: " + delay);
                return delay;
            }

            private void scheduleTimeoutMessage() {
                if (SoftApManager.this.mTimeoutEnabled) {
                    this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeoutDelay));
                    Log.d(SoftApManager.TAG, "Timeout message scheduled");
                }
            }

            private void cancelTimeoutMessage() {
                this.mSoftApTimeoutMessage.cancel();
                Log.d(SoftApManager.TAG, "Timeout message canceled");
            }

            private void setNumAssociatedStations(int numStations) {
                if (SoftApManager.this.mNumAssociatedStations != numStations) {
                    int unused = SoftApManager.this.mNumAssociatedStations = numStations;
                    Log.d(SoftApManager.TAG, "Number of associated stations changed: " + SoftApManager.this.mNumAssociatedStations);
                    if (SoftApManager.this.mCallback != null) {
                        SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                    } else {
                        Log.e(SoftApManager.TAG, "SoftApCallback is null. Dropping NumClientsChanged event.");
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApNumAssociatedStationsChangedEvent(SoftApManager.this.mNumAssociatedStations, SoftApManager.this.mMode);
                    if (SoftApManager.this.mNumAssociatedStations == 0) {
                        scheduleTimeoutMessage();
                    } else {
                        cancelTimeoutMessage();
                    }
                }
            }

            private void onUpChanged(boolean isUp) {
                if (isUp != SoftApManager.this.mIfaceIsUp) {
                    boolean unused = SoftApManager.this.mIfaceIsUp = isUp;
                    if (isUp) {
                        Log.d(SoftApManager.TAG, "SoftAp is ready for use");
                        SoftApManager.this.updateApState(13, 12, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(true, 0);
                        if (SoftApManager.this.mCallback != null) {
                            SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                        }
                    } else {
                        SoftApStateMachine.this.sendMessage(8);
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(isUp, SoftApManager.this.mMode);
                }
            }

            public void enter() {
                boolean unused = SoftApManager.this.mIfaceIsUp = false;
                boolean unused2 = SoftApManager.this.mIfaceIsDestroyed = false;
                onUpChanged(SoftApManager.this.mWifiNative.isInterfaceUp(SoftApManager.this.mApInterfaceName));
                SoftApManager.this.mSemWifiApPowerSaveImpl.registerSoftApCallback(SoftApManager.this.mApInterfaceName, SoftApManager.this.mApConfig.maxclient);
                SoftApManager.this.mSemWifiApTimeOutImpl.registerSoftApCallback();
                this.mTimeoutDelay = getConfigSoftApTimeoutDelay();
                Handler handler = SoftApManager.this.mStateMachine.getHandler();
                this.mSoftApTimeoutMessage = new WakeupMessage(SoftApManager.this.mContext, handler, SoftApManager.SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG, 5);
                this.mSettingObserver = new SoftApTimeoutEnabledSettingObserver(handler);
                SoftApTimeoutEnabledSettingObserver softApTimeoutEnabledSettingObserver = this.mSettingObserver;
                if (softApTimeoutEnabledSettingObserver != null) {
                    softApTimeoutEnabledSettingObserver.register();
                }
                SoftApManager.this.mSarManager.setSapWifiState(13);
                Log.d(SoftApManager.TAG, "Resetting num stations on start");
                int unused3 = SoftApManager.this.mNumAssociatedStations = 0;
                scheduleTimeoutMessage();
            }

            public void exit() {
                if (!SoftApManager.this.mIfaceIsDestroyed) {
                    SoftApManager.this.stopSoftAp();
                }
                SoftApTimeoutEnabledSettingObserver softApTimeoutEnabledSettingObserver = this.mSettingObserver;
                if (softApTimeoutEnabledSettingObserver != null) {
                    softApTimeoutEnabledSettingObserver.unregister();
                }
                Log.d(SoftApManager.TAG, "Resetting num stations on stop");
                setNumAssociatedStations(0);
                cancelTimeoutMessage();
                SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(false, SoftApManager.this.mMode);
                SoftApManager.this.updateApState(11, 10, 0);
                SoftApManager.this.mSarManager.setSapWifiState(11);
                String unused = SoftApManager.this.mApInterfaceName = null;
                boolean unused2 = SoftApManager.this.mIfaceIsUp = false;
                boolean unused3 = SoftApManager.this.mIfaceIsDestroyed = false;
                SoftApManager.this.mStateMachine.quitNow();
                SoftApManager.this.mSemWifiApTimeOutImpl.unRegisterSoftApCallback();
                SoftApManager.this.mSemWifiApPowerSaveImpl.unRegisterSoftApCallback();
                SoftApManager.this.mSemWifiApClientInfo.stopReceivingEvents();
            }

            private void updateUserBandPreferenceViolationMetricsIfNeeded() {
                boolean bandPreferenceViolated = false;
                if (SoftApManager.this.mApConfig.apBand == 0 && ScanResult.is5GHz(SoftApManager.this.mReportedFrequency)) {
                    bandPreferenceViolated = true;
                } else if (SoftApManager.this.mApConfig.apBand == 1 && ScanResult.is24GHz(SoftApManager.this.mReportedFrequency)) {
                    bandPreferenceViolated = true;
                }
                if (bandPreferenceViolated) {
                    Log.e(SoftApManager.TAG, "Channel does not satisfy user band preference: " + SoftApManager.this.mReportedFrequency);
                    SoftApManager.this.mWifiMetrics.incrementNumSoftApUserBandPreferenceUnsatisfied();
                }
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    boolean isUp = false;
                    switch (i) {
                        case 2:
                            Log.w(SoftApManager.TAG, "hostapd failure, stop and report failure");
                            break;
                        case 3:
                            if (message.arg1 == 1) {
                                isUp = true;
                            }
                            onUpChanged(isUp);
                            break;
                        case 4:
                            if (message.arg1 >= 0) {
                                Log.d(SoftApManager.TAG, "Setting num stations on CMD_NUM_ASSOCIATED_STATIONS_CHANGED");
                                setNumAssociatedStations(message.arg1);
                                break;
                            } else {
                                Log.e(SoftApManager.TAG, "Invalid number of associated stations: " + message.arg1);
                                break;
                            }
                        case 5:
                            if (SoftApManager.this.mTimeoutEnabled) {
                                if (SoftApManager.this.mNumAssociatedStations == 0) {
                                    Log.i(SoftApManager.TAG, "Timeout message received. Stopping soft AP.");
                                    SoftApManager.this.updateApState(10, 13, 0);
                                    SoftApStateMachine softApStateMachine = SoftApStateMachine.this;
                                    softApStateMachine.transitionTo(softApStateMachine.mIdleState);
                                    break;
                                } else {
                                    Log.wtf(SoftApManager.TAG, "Timeout message received but has clients. Dropping.");
                                    break;
                                }
                            } else {
                                Log.wtf(SoftApManager.TAG, "Timeout message received while timeout is disabled. Dropping.");
                                break;
                            }
                        case 6:
                            if (message.arg1 == 1) {
                                isUp = true;
                            }
                            boolean isEnabled = isUp;
                            if (SoftApManager.this.mTimeoutEnabled != isEnabled) {
                                boolean unused = SoftApManager.this.mTimeoutEnabled = isEnabled;
                                if (!SoftApManager.this.mTimeoutEnabled) {
                                    cancelTimeoutMessage();
                                }
                                if (SoftApManager.this.mTimeoutEnabled && SoftApManager.this.mNumAssociatedStations == 0) {
                                    scheduleTimeoutMessage();
                                    break;
                                }
                            }
                            break;
                        case 7:
                            Log.d(SoftApManager.TAG, "Interface was cleanly destroyed.");
                            SoftApManager.this.updateApState(10, 13, 0);
                            boolean unused2 = SoftApManager.this.mIfaceIsDestroyed = true;
                            SoftApStateMachine softApStateMachine2 = SoftApStateMachine.this;
                            softApStateMachine2.transitionTo(softApStateMachine2.mIdleState);
                            break;
                        case 8:
                            break;
                        case 9:
                            int unused3 = SoftApManager.this.mReportedFrequency = message.arg1;
                            int unused4 = SoftApManager.this.mReportedBandwidth = message.arg2;
                            Log.d(SoftApManager.TAG, "Channel switched. Frequency: " + SoftApManager.this.mReportedFrequency + " Bandwidth: " + SoftApManager.this.mReportedBandwidth);
                            SoftApManager.this.mWifiMetrics.addSoftApChannelSwitchedEvent(SoftApManager.this.mReportedFrequency, SoftApManager.this.mReportedBandwidth, SoftApManager.this.mMode);
                            updateUserBandPreferenceViolationMetricsIfNeeded();
                            break;
                        default:
                            return false;
                    }
                    Log.w(SoftApManager.TAG, "interface error, stop and report failure");
                    SoftApManager.this.updateApState(14, 13, 0);
                    SoftApManager.this.updateApState(10, 14, 0);
                    SoftApStateMachine softApStateMachine3 = SoftApStateMachine.this;
                    softApStateMachine3.transitionTo(softApStateMachine3.mIdleState);
                }
                return true;
            }
        }
    }
}
