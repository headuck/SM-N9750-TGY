package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.WifiHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;

public class SarManager {
    private static final int CHECK_VOICE_STREAM_INTERVAL_MS = 5000;
    private static final String TAG = "WifiSarManager";
    /* access modifiers changed from: private */
    public final Context mContext;
    private final Handler mHandler;
    private boolean mIsVoiceStreamCheckEnabled = false;
    private final Looper mLooper;
    private final WifiPhoneStateListener mPhoneStateListener;
    /* access modifiers changed from: private */
    public SarInfo mSarInfo;
    private int mSarSensorEventFreeSpace;
    private int mSarSensorEventNearBody;
    private int mSarSensorEventNearHand;
    private int mSarSensorEventNearHead;
    private boolean mScreenOn = false;
    private final SarSensorEventListener mSensorListener;
    /* access modifiers changed from: private */
    public final SensorManager mSensorManager;
    private boolean mSupportSarSensor;
    private boolean mSupportSarSoftAp;
    /* access modifiers changed from: private */
    public boolean mSupportSarTxPowerLimit;
    /* access modifiers changed from: private */
    public boolean mSupportSarVoiceCall;
    private final TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public boolean mVerboseLoggingEnabled = true;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;

    SarManager(Context context, TelephonyManager telephonyManager, Looper looper, WifiNative wifiNative, SensorManager sensorManager, WifiMetrics wifiMetrics) {
        this.mContext = context;
        this.mTelephonyManager = telephonyManager;
        this.mWifiNative = wifiNative;
        this.mLooper = looper;
        this.mHandler = new WifiHandler(TAG, looper);
        this.mSensorManager = sensorManager;
        this.mWifiMetrics = wifiMetrics;
        this.mPhoneStateListener = new WifiPhoneStateListener(looper);
        this.mSensorListener = new SarSensorEventListener();
        readSarConfigs();
        if (this.mSupportSarTxPowerLimit) {
            this.mSarInfo = new SarInfo();
            setSarConfigsInInfo();
            registerListeners();
        }
    }

    public void handleScreenStateChanged(boolean screenOn) {
        if (this.mSupportSarVoiceCall && this.mScreenOn != screenOn) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "handleScreenStateChanged: screenOn = " + screenOn);
            }
            this.mScreenOn = screenOn;
            if (this.mScreenOn && !this.mIsVoiceStreamCheckEnabled) {
                this.mHandler.post(new Runnable() {
                    public final void run() {
                        SarManager.this.lambda$handleScreenStateChanged$0$SarManager();
                    }
                });
                this.mIsVoiceStreamCheckEnabled = true;
            }
        }
    }

    private boolean isVoiceCallOnEarpiece() {
        return ((AudioManager) this.mContext.getSystemService("audio")).getDevicesForStream(0) == 1;
    }

    /* access modifiers changed from: private */
    public boolean isVoiceCallStreamActive() {
        return AudioSystem.isStreamActive(0, 0);
    }

    /* access modifiers changed from: private */
    /* renamed from: checkAudioDevice */
    public void lambda$handleScreenStateChanged$0$SarManager() {
        boolean earPieceActive;
        boolean voiceStreamActive = isVoiceCallStreamActive();
        if (voiceStreamActive) {
            earPieceActive = isVoiceCallOnEarpiece();
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "EarPiece active = " + earPieceActive);
            }
        } else {
            earPieceActive = false;
        }
        if (earPieceActive != this.mSarInfo.isEarPieceActive) {
            this.mSarInfo.isEarPieceActive = earPieceActive;
            updateSarScenario();
        }
        if (this.mScreenOn || voiceStreamActive) {
            this.mHandler.postDelayed(new Runnable() {
                public final void run() {
                    SarManager.this.lambda$checkAudioDevice$1$SarManager();
                }
            }, RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
        } else {
            this.mIsVoiceStreamCheckEnabled = false;
        }
    }

    private void readSarConfigs() {
        this.mSupportSarTxPowerLimit = this.mContext.getResources().getBoolean(17891603);
        if (!this.mSupportSarTxPowerLimit) {
            this.mSupportSarVoiceCall = false;
            this.mSupportSarSoftAp = false;
            this.mSupportSarSensor = false;
            return;
        }
        this.mSupportSarVoiceCall = true;
        this.mSupportSarSoftAp = this.mContext.getResources().getBoolean(17891604);
        this.mSupportSarSensor = this.mContext.getResources().getBoolean(17891602);
        if (this.mSupportSarSensor) {
            this.mSarSensorEventFreeSpace = this.mContext.getResources().getInteger(17694998);
            this.mSarSensorEventNearBody = this.mContext.getResources().getInteger(17694999);
            this.mSarSensorEventNearHand = this.mContext.getResources().getInteger(17695000);
            this.mSarSensorEventNearHead = this.mContext.getResources().getInteger(17695001);
        }
    }

    private void setSarConfigsInInfo() {
        SarInfo sarInfo = this.mSarInfo;
        sarInfo.sarVoiceCallSupported = this.mSupportSarVoiceCall;
        sarInfo.sarSapSupported = this.mSupportSarSoftAp;
        sarInfo.sarSensorSupported = this.mSupportSarSensor;
    }

    private void registerListeners() {
        if (this.mSupportSarVoiceCall) {
            registerPhoneStateListener();
            registerVoiceStreamListener();
        }
        if (this.mSupportSarSensor && !registerSensorListener()) {
            Log.e(TAG, "Failed to register sensor listener, setting Sensor to NearHead");
            this.mSarInfo.sensorState = 3;
            this.mWifiMetrics.incrementNumSarSensorRegistrationFailures();
        }
    }

    private void registerVoiceStreamListener() {
        Log.i(TAG, "Registering for voice stream status");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (SarManager.this.isVoiceCallStreamActive()) {
                    String action = intent.getAction();
                    int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                    int device = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", -1);
                    int oldDevice = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", -1);
                    if (streamType == 0) {
                        boolean earPieceActive = SarManager.this.mSarInfo.isEarPieceActive;
                        if (device == 1) {
                            if (SarManager.this.mVerboseLoggingEnabled) {
                                Log.d(SarManager.TAG, "Switching to earpiece : HEAD ON");
                                Log.d(SarManager.TAG, "Old device = " + oldDevice);
                            }
                            earPieceActive = true;
                        } else if (oldDevice == 1) {
                            if (SarManager.this.mVerboseLoggingEnabled) {
                                Log.d(SarManager.TAG, "Switching from earpiece : HEAD OFF");
                                Log.d(SarManager.TAG, "New device = " + device);
                            }
                            earPieceActive = false;
                        }
                        if (earPieceActive != SarManager.this.mSarInfo.isEarPieceActive) {
                            SarManager.this.mSarInfo.isEarPieceActive = earPieceActive;
                            SarManager.this.updateSarScenario();
                        }
                    }
                }
            }
        }, filter, (String) null, this.mHandler);
    }

    private void registerPhoneStateListener() {
        Log.i(TAG, "Registering for telephony call state changes");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
    }

    private boolean registerSensorListener() {
        Log.i(TAG, "Registering for Sensor notification Listener");
        return this.mSensorListener.register();
    }

    public void setClientWifiState(int state) {
        boolean newIsEnabled;
        if (this.mSupportSarTxPowerLimit) {
            if (state == 1) {
                newIsEnabled = false;
            } else if (state == 3) {
                newIsEnabled = true;
            } else {
                return;
            }
            if (this.mSarInfo.isWifiClientEnabled != newIsEnabled) {
                this.mSarInfo.isWifiClientEnabled = newIsEnabled;
                updateSarScenario();
            }
        }
    }

    public void setSapWifiState(int state) {
        boolean newIsEnabled;
        if (this.mSupportSarTxPowerLimit) {
            if (state == 11) {
                newIsEnabled = false;
            } else if (state == 13) {
                newIsEnabled = true;
            } else {
                return;
            }
            if (this.mSarInfo.isWifiSapEnabled != newIsEnabled) {
                this.mSarInfo.isWifiSapEnabled = newIsEnabled;
                updateSarScenario();
            }
        }
    }

    public void setScanOnlyWifiState(int state) {
        boolean newIsEnabled;
        if (this.mSupportSarTxPowerLimit) {
            if (state == 1) {
                newIsEnabled = false;
            } else if (state == 3) {
                newIsEnabled = true;
            } else {
                return;
            }
            if (this.mSarInfo.isWifiScanOnlyEnabled != newIsEnabled) {
                this.mSarInfo.isWifiScanOnlyEnabled = newIsEnabled;
                updateSarScenario();
            }
        }
    }

    /* access modifiers changed from: private */
    public void onCellStateChangeEvent(int state) {
        boolean newIsVoiceCall;
        if (state == 0) {
            newIsVoiceCall = false;
        } else if (state == 1 || state == 2) {
            newIsVoiceCall = true;
        } else {
            Log.e(TAG, "Invalid Cell State: " + state);
            return;
        }
        if (this.mSarInfo.isVoiceCall != newIsVoiceCall) {
            this.mSarInfo.isVoiceCall = newIsVoiceCall;
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Voice Call = " + newIsVoiceCall);
            }
            updateSarScenario();
        }
    }

    /* access modifiers changed from: private */
    public void onSarSensorEvent(int sarSensorEvent) {
        int newSensorState;
        if (sarSensorEvent == this.mSarSensorEventFreeSpace) {
            newSensorState = 1;
        } else if (sarSensorEvent == this.mSarSensorEventNearBody) {
            newSensorState = 4;
        } else if (sarSensorEvent == this.mSarSensorEventNearHand) {
            newSensorState = 2;
        } else if (sarSensorEvent == this.mSarSensorEventNearHead) {
            newSensorState = 3;
        } else {
            Log.e(TAG, "Invalid SAR sensor event id: " + sarSensorEvent);
            return;
        }
        if (this.mSarInfo.sensorState != newSensorState) {
            Log.d(TAG, "Setting Sensor state to " + SarInfo.sensorStateToString(newSensorState));
            this.mSarInfo.sensorState = newSensorState;
            updateSarScenario();
        }
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of SarManager");
        pw.println("isSarSupported: " + this.mSupportSarTxPowerLimit);
        pw.println("isSarVoiceCallSupported: " + this.mSupportSarVoiceCall);
        pw.println("isSarSoftApSupported: " + this.mSupportSarSoftAp);
        pw.println("isSarSensorSupported: " + this.mSupportSarSensor);
        pw.println("");
        SarInfo sarInfo = this.mSarInfo;
        if (sarInfo != null) {
            sarInfo.dump(fd, pw, args);
        }
    }

    private class WifiPhoneStateListener extends PhoneStateListener {
        WifiPhoneStateListener(Looper looper) {
            super(looper);
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(SarManager.TAG, "Received Phone State Change: " + state);
            if (SarManager.this.mSupportSarTxPowerLimit && SarManager.this.mSupportSarVoiceCall) {
                SarManager.this.onCellStateChangeEvent(state);
            }
        }
    }

    private class SarSensorEventListener implements SensorEventListener {
        private Sensor mSensor;

        private SarSensorEventListener() {
        }

        /* access modifiers changed from: private */
        public boolean register() {
            String sensorType = SarManager.this.mContext.getResources().getString(17040006);
            if (TextUtils.isEmpty(sensorType)) {
                Log.e(SarManager.TAG, "Empty SAR sensor type");
                return false;
            }
            Sensor sensor = null;
            Iterator<Sensor> it = SarManager.this.mSensorManager.getSensorList(-1).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Sensor s = it.next();
                if (sensorType.equals(s.getStringType())) {
                    sensor = s;
                    break;
                }
            }
            if (sensor == null) {
                Log.e(SarManager.TAG, "Failed to Find the SAR Sensor");
                return false;
            } else if (SarManager.this.mSensorManager.registerListener(this, sensor, 3)) {
                return true;
            } else {
                Log.e(SarManager.TAG, "Failed to register SAR Sensor Listener");
                return false;
            }
        }

        public void onSensorChanged(SensorEvent event) {
            SarManager.this.onSarSensorEvent((int) event.values[0]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /* access modifiers changed from: private */
    public void updateSarScenario() {
        if (this.mSarInfo.shouldReport()) {
            if (this.mWifiNative.selectTxPowerScenario(this.mSarInfo)) {
                this.mSarInfo.reportingSuccessful();
            } else {
                Log.e(TAG, "Failed in WifiNative.selectTxPowerScenario()");
            }
        }
    }
}
