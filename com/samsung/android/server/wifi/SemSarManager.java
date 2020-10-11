package com.samsung.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.p2p.common.DefaultImageRequest;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class SemSarManager {
    public static final int BODY_SAR_BACKOFF_DISABLED = 1;
    public static final int BODY_SAR_BACKOFF_ENABLED = 2;
    private static final boolean BODY_SAR_ENABLED = "".contains("GRIP");
    private static final String BODY_SAR_TYPE = "";
    private static final int CMD_LID_STATE_CHANGE = 0;
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final boolean DYNAMIC_TP_CONTROL_ENABLED = false;
    private static final String EXTRA_FLIP_OPEN = "flipOpen";
    private static final String EXTRA_RCV_ON = "android.samsung.media.extra.receiver";
    private static final boolean FLIPFOLDER_SAR = "".contains("FLIPFOLDER");
    private static final String FLIP_FOLDER_BODY_SAR_INTENT = "com.samsung.flipfolder.OPEN";
    private static final boolean HEAD_BODY_SAR_ENABLED = false;
    public static final int HEAD_DISABLED_BODY_DISABLED_SAR_BACKOFF = -1;
    public static final int HEAD_DISABLED_BODY_ENABLED_SAR_BACKOFF = 2;
    public static final int HEAD_ENABLED_BODY_ENABLED_SAR_BACKOFF = 0;
    public static final int HEAD_SAR_BACKOFF_DISABLED = -1;
    public static final int HEAD_SAR_BACKOFF_ENABLED = 0;
    private static final boolean HEAD_SAR_ENABLED = false;
    private static final boolean HEAD_SAR_ENABLED_RCV_ONLY = true;
    private static final String HEAD_SAR_INTENT = "android.samsung.media.action.receiver_sar";
    public static final int NR_MMWAVE_SAR_BACKOFF_DISABLED = 3;
    public static final int NR_MMWAVE_SAR_BACKOFF_ENABLED = 4;
    public static final int NR_SUB6_SAR_BACKOFF_DISABLED = 5;
    public static final int NR_SUB6_SAR_BACKOFF_ENABLED = 6;
    public static final int SAR_BACKOFF_DISABLE_ALL = 7;
    private static final String TAG = "SemSarManager";
    private static boolean mIsRfTestMode;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public float mFarValue;
    private Sensor mGripSensor1;
    private Sensor mGripSensor2;
    /* access modifiers changed from: private */
    public int mGripSensorChannel1 = 0;
    /* access modifiers changed from: private */
    public int mGripSensorChannel2;
    private final SensorEventListener mGripSensorListener1 = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (event != null && event.values != null) {
                if (SemSarManager.DBG) {
                    Log.i(SemSarManager.TAG, "onGripSensorChanged1 : " + event.values[SemSarManager.this.mGripSensorChannel1] + " / " + SemSarManager.this.mGripSensorChannel1);
                }
                if (!SemSarManager.this.mIsGripSensorEnabled) {
                    Log.i(SemSarManager.TAG, "mIsGripSensorEnabled is false");
                } else if (SemSarManager.this.mIsGta4L) {
                    if (event.values[SemSarManager.this.mGripSensorChannel1] >= 8.0f && !SemSarManager.this.mIsSarActive) {
                        Log.i(SemSarManager.TAG, "enable powerbackoff");
                        boolean unused = SemSarManager.this.mIsGrip = true;
                        SemSarManager.this.updateBackOffStatus(2);
                        boolean unused2 = SemSarManager.this.mIsSarActive = true;
                    } else if (event.values[SemSarManager.this.mGripSensorChannel1] < 8.0f && SemSarManager.this.mIsSarActive) {
                        Log.i(SemSarManager.TAG, "disable powerbackoff");
                        boolean unused3 = SemSarManager.this.mIsGrip = false;
                        if (!SemSarManager.this.mIsGrip_2) {
                            SemSarManager.this.updateBackOffStatus(1);
                            boolean unused4 = SemSarManager.this.mIsSarActive = false;
                        }
                    }
                } else if (event.values[SemSarManager.this.mGripSensorChannel1] == DefaultImageRequest.OFFSET_DEFAULT) {
                    Log.i(SemSarManager.TAG, "enable powerbackoff");
                    boolean unused5 = SemSarManager.this.mIsGrip = true;
                    if (SemSarManager.this.mIsDoubleGripSensor) {
                        SemSarManager.this.updateBackOffStatus(2, 0);
                    } else {
                        SemSarManager.this.updateBackOffStatus(2);
                    }
                } else if (event.values[SemSarManager.this.mGripSensorChannel1] == 5.0f) {
                    Log.i(SemSarManager.TAG, "disable powerbackoff");
                    boolean unused6 = SemSarManager.this.mIsGrip = false;
                    if (SemSarManager.this.mIsDoubleGripSensor) {
                        SemSarManager.this.updateBackOffStatus(1, 0);
                    } else if (!SemSarManager.this.mIsGrip_2) {
                        SemSarManager.this.updateBackOffStatus(1);
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final SensorEventListener mGripSensorListener2 = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (event != null && event.values != null) {
                int antChannel = 0;
                if (SemSarManager.this.mIsDoubleGripSensor) {
                    antChannel = SemSarManager.this.mGripSensorChannel2;
                }
                if (SemSarManager.DBG) {
                    Log.i(SemSarManager.TAG, "onGripSensorChanged2 : " + event.values[antChannel] + " / " + antChannel);
                }
                if (!SemSarManager.this.mIsGripSensorEnabled) {
                    Log.i(SemSarManager.TAG, "mIsGripSensorEnabled is false");
                } else if (event.values[antChannel] == DefaultImageRequest.OFFSET_DEFAULT) {
                    Log.i(SemSarManager.TAG, "enable powerbackoff");
                    boolean unused = SemSarManager.this.mIsGrip_2 = true;
                    if (SemSarManager.this.mIsDoubleGripSensor) {
                        SemSarManager.this.updateBackOffStatus(2, 1);
                    } else {
                        SemSarManager.this.updateBackOffStatus(2);
                    }
                } else if (event.values[antChannel] == 5.0f) {
                    Log.i(SemSarManager.TAG, "disable powerbackoff");
                    boolean unused2 = SemSarManager.this.mIsGrip_2 = false;
                    if (SemSarManager.this.mIsDoubleGripSensor) {
                        SemSarManager.this.updateBackOffStatus(1, 1);
                    } else if (!SemSarManager.this.mIsGrip) {
                        SemSarManager.this.updateBackOffStatus(1);
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                boolean isOpened = ((Boolean) msg.obj).booleanValue();
                Log.d(SemSarManager.TAG, "isOpened=" + isOpened);
                if (isOpened) {
                    SemSarManager.this.startGripSensorForBodySar(true);
                } else {
                    SemSarManager.this.startGripSensorForBodySar(false);
                }
            }
        }
    };
    private boolean mHeadSarBackOffByRcvOnly = false;
    private String mInterfaceName;
    private boolean mIsActive;
    /* access modifiers changed from: private */
    public boolean mIsClose;
    /* access modifiers changed from: private */
    public boolean mIsDoubleGripSensor;
    /* access modifiers changed from: private */
    public boolean mIsFolderOpen = false;
    private boolean mIsGipSensorMonitoring;
    /* access modifiers changed from: private */
    public boolean mIsGrip;
    /* access modifiers changed from: private */
    public boolean mIsGripSensorEnabled;
    /* access modifiers changed from: private */
    public boolean mIsGrip_2 = false;
    /* access modifiers changed from: private */
    public boolean mIsGta4L;
    private boolean mIsGta4XL;
    /* access modifiers changed from: private */
    public boolean mIsRCVOn = false;
    /* access modifiers changed from: private */
    public boolean mIsSarActive = false;
    private LidStateChangeListener mLidStateChangeListener;
    private final SensorEventListener mListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(SemSarManager.TAG, "Processing onAccuracyChanged event at : " + System.currentTimeMillis());
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == 8) {
                boolean unused = SemSarManager.this.mIsClose = event.values[0] < SemSarManager.this.mFarValue;
                Log.d(SemSarManager.TAG, "Processing onSensorChanged event at : " + System.currentTimeMillis() + " , mIsClose : " + SemSarManager.this.mIsClose);
                if (SemSarManager.this.mIsClose) {
                    Log.d(SemSarManager.TAG, "Something closed ");
                    SemSarManager.this.updateBackOffStatus(0);
                    return;
                }
                Log.d(SemSarManager.TAG, "Nothing closed");
                SemSarManager.this.updateBackOffStatus(-1);
            }
        }
    };
    private final INetworkManagementService mNMService;
    private int mNewRfModeType;
    private boolean mPowerBackOff = false;
    private Sensor mProxSensor;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SemSarManager.HEAD_SAR_INTENT.equals(action)) {
                boolean unused = SemSarManager.this.mIsRCVOn = intent.getBooleanExtra(SemSarManager.EXTRA_RCV_ON, false);
                Log.d(SemSarManager.TAG, "received android.samsung.media.action.receiver_sar, isRCV = " + SemSarManager.this.mIsRCVOn);
                if (!SemSarManager.this.mWifiStaEnabled) {
                    return;
                }
                if (SemSarManager.this.mIsRCVOn) {
                    SemSarManager.this.startProximitySensorForHeadSar();
                } else {
                    SemSarManager.this.stopProximitySensorForHeadSar();
                }
            } else if (SemSarManager.FLIP_FOLDER_BODY_SAR_INTENT.equals(action)) {
                boolean unused2 = SemSarManager.this.mIsFolderOpen = intent.getBooleanExtra(SemSarManager.EXTRA_FLIP_OPEN, false);
                Log.d(SemSarManager.TAG, "received com.samsung.flipfolder.OPEN, flipOpen = " + SemSarManager.this.mIsFolderOpen);
                if (!SemSarManager.this.mWifiStaEnabled) {
                    return;
                }
                if (SemSarManager.this.mIsFolderOpen) {
                    SemSarManager.this.startGripSensorForBodySar(true);
                } else {
                    SemSarManager.this.startGripSensorForBodySar(false);
                }
            } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                String strState = intent.getStringExtra("state");
                Log.d(SemSarManager.TAG, "received ACTION_PHONE_STATE_CHANGED , EXTRA_STATE = " + strState);
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(strState)) {
                    SemSarManager.this.stopProximitySensorForHeadSar();
                }
            } else if (intent.getAction().equals("android.intent.action.HEADSET_PLUG")) {
                int state = intent.getIntExtra("state", 0);
                Log.d(SemSarManager.TAG, "received HEADSET_PLUG state = " + state);
                if (state == 1) {
                    int unused3 = SemSarManager.this.setTouchKeyEarjack(true);
                }
            }
        }
    };
    private final SensorManager mSensorManager;
    private final WifiNative mWifiNative;
    /* access modifiers changed from: private */
    public boolean mWifiStaEnabled = false;

    public SemSarManager(Context context, WifiNative wifiNative) {
        String str;
        Log.d(TAG, "SemSarManager creator");
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mIsGta4XL = Build.PRODUCT.contains("gta4xl");
        this.mIsGta4L = Build.PRODUCT.contains("gta4l");
        String str2 = "enabled state";
        boolean z = true;
        if (BODY_SAR_ENABLED || FLIPFOLDER_SAR) {
            if ("".contains("GRIPI")) {
                this.mGripSensor1 = this.mSensorManager.getDefaultSensor(65560);
            } else {
                this.mGripSensor1 = this.mSensorManager.getDefaultSensor(65575);
                if (this.mIsGta4XL) {
                    this.mGripSensor2 = this.mSensorManager.getDefaultSensor(65560);
                }
            }
            this.mIsGripSensorEnabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_sensor_monitor_enable", 1) == 1;
            StringBuilder sb = new StringBuilder();
            sb.append("create sensor monitor with ");
            if (this.mIsGripSensorEnabled) {
                str = str2;
            } else {
                str = "disabled state";
            }
            sb.append(str);
            Log.i(TAG, sb.toString());
        }
        try {
            this.mIsDoubleGripSensor = !TextUtils.isEmpty("");
            if (this.mIsDoubleGripSensor) {
                String featureString = "";
                Log.d(TAG, "SEC_PRODUCT_FEATURE_WLAN_CONFIG_SEPARATE_ANT_BACKOFF=" + featureString);
                String[] featureArray = featureString.split(" ");
                this.mGripSensor1 = this.mSensorManager.getDefaultSensor(Integer.parseInt(featureArray[0].replace("0x", ""), 16));
                this.mGripSensorChannel1 = Integer.parseInt(featureArray[1]);
                this.mGripSensor2 = this.mSensorManager.getDefaultSensor(Integer.parseInt(featureArray[2].replace("0x", ""), 16));
                this.mGripSensorChannel2 = Integer.parseInt(featureArray[3]);
                if (Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_sensor_monitor_enable", 1) != 1) {
                    z = false;
                }
                this.mIsGripSensorEnabled = z;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("create sensor monitor with ");
                if (!this.mIsGripSensorEnabled) {
                    str2 = "disabled state";
                }
                sb2.append(str2);
                Log.i(TAG, sb2.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HEAD_SAR_INTENT);
        if (FLIPFOLDER_SAR) {
            intentFilter.addAction(FLIP_FOLDER_BODY_SAR_INTENT);
        }
        if (this.mIsGta4XL && "".contains("EARJACK")) {
            intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        }
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        mIsRfTestMode = checkRfMode(false);
        this.mNMService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        if (FLIPFOLDER_SAR) {
            this.mLidStateChangeListener = new LidStateChangeListener();
            this.mLidStateChangeListener.init();
        }
    }

    public void setClientWifiState(int state) {
        this.mInterfaceName = this.mWifiNative.getClientInterfaceName();
        Log.d(TAG, "setClientWifiState:" + state + ", mInterfaceName:" + this.mInterfaceName);
        boolean z = BODY_SAR_ENABLED;
        if (state == 1 && this.mWifiStaEnabled) {
            this.mWifiStaEnabled = false;
        } else if (state == 3 && !this.mWifiStaEnabled) {
            this.mWifiStaEnabled = true;
            if (FLIPFOLDER_SAR) {
                this.mLidStateChangeListener.checkAndStartGripSensorForBodySar();
            }
        }
        updateSensorStatus(this.mWifiStaEnabled);
    }

    public void updateSensorStatus(boolean wifiEnabled) {
        boolean z;
        if (wifiEnabled) {
            if (this.mIsRCVOn) {
                startProximitySensorForHeadSar();
            }
            if ((BODY_SAR_ENABLED || this.mIsGripSensorEnabled) && (!(z = FLIPFOLDER_SAR) || (z && this.mIsFolderOpen))) {
                startGripSensorForBodySar(true);
            }
            if (this.mIsDoubleGripSensor) {
                startGripSensorForBodySar(true);
                return;
            }
            return;
        }
        stopProximitySensorForHeadSar();
        if (BODY_SAR_ENABLED) {
            startGripSensorForBodySar(false);
        }
        if (this.mIsDoubleGripSensor) {
            startGripSensorForBodySar(false);
        }
    }

    /* access modifiers changed from: private */
    public void updateBackOffStatus(int mode) {
        Log.d(TAG, "updateBackOffStatus:" + mode);
        if (!FLIPFOLDER_SAR) {
            if (mode != -1) {
                if (mode != 0) {
                    if (mode != 1) {
                        if (mode != 2) {
                            return;
                        }
                    }
                }
                setPowerBackoff(true);
                if (DYNAMIC_TP_CONTROL_ENABLED) {
                    setTCRule(true, this.mInterfaceName, 2);
                    return;
                }
                return;
            }
            setPowerBackoff(false);
            if (DYNAMIC_TP_CONTROL_ENABLED) {
                setTCRule(false, this.mInterfaceName, -1);
                return;
            }
            return;
        }
        setPowerBackoff(mode);
    }

    /* access modifiers changed from: private */
    public void updateBackOffStatus(int mode, int ant) {
        Log.d(TAG, "updateBackOffStatus:" + mode + ", ant=" + ant);
        setPowerBackoff(mode, ant);
    }

    public void set5GSarBackOff(int mode) {
        Log.d(TAG, "set5GSarBackOff enabled=" + mode);
        setPowerBackoff(mode);
    }

    private void setPowerBackoff(int mode) {
        Log.d(TAG, "setPowerBackoff mode=" + mode);
        this.mWifiNative.setTxPowerBackOff(this.mInterfaceName, mode);
    }

    private void setPowerBackoff(int mode, int ant) {
        Log.d(TAG, "setPowerBackoff mode=" + mode + ", ant=" + ant);
        this.mWifiNative.setTxPowerBackOff(this.mInterfaceName, mode, ant);
    }

    private void setPowerBackoff(boolean enable) {
        this.mWifiNative.setTxPowerBackOff(this.mInterfaceName, enable);
    }

    class LidStateChangeListener implements InputManager.OnLidStateChangedListener {
        LidStateChangeListener() {
        }

        public void onLidStateChanged(long whenNanos, boolean lidOpen) {
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = Boolean.valueOf(lidOpen);
            SemSarManager.this.mHandler.sendMessage(msg);
        }

        public void checkAndStartGripSensorForBodySar() {
            Log.d(SemSarManager.TAG, "checkAndStartGripSensorForBodySar");
            if (((InputManager) SemSarManager.this.mContext.getSystemService(InputManager.class)).getLidState() != 1) {
                SemSarManager.this.startGripSensorForBodySar(true);
            }
        }

        /* access modifiers changed from: private */
        public void init() {
            Log.d(SemSarManager.TAG, "LidStateChangeListener init");
            ((InputManager) SemSarManager.this.mContext.getSystemService(InputManager.class)).registerOnLidStateChangedListener(this, SemSarManager.this.mHandler);
        }
    }

    public void enableGripSensorMonitor(boolean enable) {
        this.mIsGripSensorEnabled = enable;
        Settings.Secure.putInt(this.mContext.getContentResolver(), "wifi_sensor_monitor_enable", enable);
        if (!enable || (!this.mIsGipSensorMonitoring && this.mWifiStaEnabled)) {
            startGripSensorForBodySar(enable);
        }
    }

    public boolean isGripSensorEnabled() {
        return this.mIsGripSensorEnabled;
    }

    public boolean isMonitoring() {
        return this.mIsGipSensorMonitoring;
    }

    /* access modifiers changed from: private */
    public void startGripSensorForBodySar(boolean enable) {
        if (this.mGripSensor1 != null && this.mIsGipSensorMonitoring != enable) {
            Log.i(TAG, "enable sensor monitoring : " + enable);
            if (enable) {
                this.mSensorManager.registerListener(this.mGripSensorListener1, this.mGripSensor1, 3);
                if (this.mIsGta4XL || this.mIsDoubleGripSensor) {
                    this.mSensorManager.registerListener(this.mGripSensorListener2, this.mGripSensor2, 3);
                }
            } else {
                this.mSensorManager.unregisterListener(this.mGripSensorListener1);
                if (this.mIsGta4XL || this.mIsDoubleGripSensor) {
                    this.mSensorManager.unregisterListener(this.mGripSensorListener2);
                }
                if (this.mIsDoubleGripSensor) {
                    updateBackOffStatus(1, 0);
                    updateBackOffStatus(1, 1);
                } else {
                    updateBackOffStatus(1);
                }
            }
            this.mIsGipSensorMonitoring = enable;
        }
    }

    public boolean isBodySarGrip() {
        return BODY_SAR_ENABLED && (this.mIsGrip || this.mIsGrip_2);
    }

    public void stopProximitySensorForHeadSar() {
        if (this.mHeadSarBackOffByRcvOnly) {
            updateBackOffStatus(-1);
            this.mHeadSarBackOffByRcvOnly = false;
        } else if (this.mProxSensor != null && this.mIsActive && this.mNewRfModeType != 1) {
            Log.d(TAG, "Proximity sensor stopped at : " + System.currentTimeMillis());
            this.mIsActive = false;
            if (this.mIsClose) {
                if (this.mWifiStaEnabled) {
                    updateBackOffStatus(-1);
                }
                this.mIsClose = false;
            }
            this.mSensorManager.unregisterListener(this.mListener);
            Log.d(TAG, "ProxSensor unregisterListener complete");
        }
    }

    public void startProximitySensorForHeadSar() {
        if (!this.mHeadSarBackOffByRcvOnly) {
            updateBackOffStatus(0);
            this.mHeadSarBackOffByRcvOnly = true;
            return;
        }
        Sensor sensor = this.mProxSensor;
        if (sensor != null && !this.mIsActive) {
            this.mIsActive = true;
            this.mSensorManager.registerListener(this.mListener, sensor, 2);
            Log.d(TAG, "Proximity sensor registered at : " + System.currentTimeMillis());
        }
    }

    /* access modifiers changed from: private */
    public int setTouchKeyEarjack(boolean plugged) {
        Log.d(TAG, "setTouchKeyEarjack = " + plugged);
        if (!sysfsCheck("/sys/class/sensors/grip_sensor/grip_earjack")) {
            Log.d(TAG, "not found grip_earjack");
            return 0;
        }
        if (plugged) {
            Log.d(TAG, "grip_earjack plugged makes power backOff");
            setPowerBackoff(true);
        }
        return 1;
    }

    private boolean sysfsCheck(String sysfs) {
        try {
            try {
                new FileOutputStream(new File(sysfs)).close();
                return true;
            } catch (IOException e) {
                return true;
            }
        } catch (FileNotFoundException e2) {
            return false;
        }
    }

    private boolean checkRfMode(boolean checkProximitySensor) {
        this.mNewRfModeType = Settings.Secure.getInt(this.mContext.getContentResolver(), "wifi_new_rf_test_mode", 0);
        if (checkProximitySensor && this.mNewRfModeType == 1) {
            Log.d(TAG, "startProximitySensorForHeadSar by checkRfMode");
            startProximitySensorForHeadSar();
        }
        if (this.mNewRfModeType == 0) {
            return false;
        }
        return true;
    }

    public void enable_WiFi_PowerBackoff(boolean enable) {
        if (mIsRfTestMode) {
            Log.i(TAG, "block powerbackoff in RfTestMode");
        } else if (enable) {
            if (this.mPowerBackOff) {
                Log.i(TAG, "Aleady PowerBackOff by callSECApi");
            } else if (this.mIsDoubleGripSensor) {
                Log.i(TAG, "enable powerbackoff TWO ANT by callSECApi");
                updateBackOffStatus(2, 1);
                updateBackOffStatus(2, 0);
            } else {
                Log.i(TAG, "enable powerbackoff by callSECApi");
                updateBackOffStatus(2);
            }
            this.mPowerBackOff = true;
        } else {
            if (!this.mPowerBackOff) {
                Log.i(TAG, "Aleady PowerBackOff by callSECApi");
            } else if (this.mIsDoubleGripSensor) {
                Log.i(TAG, "disable powerbackoff TWO ANT by callSECApi");
                updateBackOffStatus(1, 1);
                updateBackOffStatus(1, 0);
            } else {
                Log.i(TAG, "disable powerbackoff by callSECApi");
                updateBackOffStatus(1);
            }
            this.mPowerBackOff = false;
        }
    }

    public static boolean isRfTestMode() {
        return mIsRfTestMode;
    }

    private void setTCRule(boolean enabled, String iface, int limit) {
        try {
            this.mNMService.setTCRule(enabled, iface, limit);
        } catch (RemoteException e) {
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of SemSarManager");
        pw.println("mIsRfTestMode " + mIsRfTestMode);
    }
}
