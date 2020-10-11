package com.android.server.wifi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import com.android.server.wifi.p2p.common.DefaultImageRequest;

public class WifiEleGeoMagnetic {
    private static final int LOG_SAMPLINGTIME = 10;
    AR_SENSORDATA acc = null;
    private Sensor accSensor = null;
    float[] arrAccData = new float[3];
    float[] arrMagData = new float[3];
    AR_BARODATA baro = null;
    float baroData = DefaultImageRequest.OFFSET_DEFAULT;
    private Sensor baroSensor = null;
    AR_CalcFeature calc_Feature = null;
    private OnEleDetectInterface eleCallback;
    EleCore eleCore = null;
    AR_MAIN_DATA gARData = null;
    /* access modifiers changed from: private */
    public Handler handler = new Handler();
    private boolean isAPISupportedFlag;
    private Context mContext;
    private boolean mPrevState = false;
    private SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();
            if (sensorType == 1) {
                for (int i = 0; i < 3; i++) {
                    WifiEleGeoMagnetic.this.arrAccData[i] = event.values[i];
                }
            } else if (sensorType == 2) {
                for (int i2 = 0; i2 < 3; i2++) {
                    WifiEleGeoMagnetic.this.arrMagData[i2] = event.values[i2];
                }
            } else if (sensorType == 6) {
                WifiEleGeoMagnetic.this.baroData = event.values[0];
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorManager mSensorManager = null;
    AR_MAG_SENSORDATA mag = null;
    private Sensor magSensor = null;
    private SensorThread thread = null;

    interface OnEleDetectInterface {
        void onEleDetect();

        void onEleNotDetect();
    }

    public boolean getAPISupportFlag() {
        return this.isAPISupportedFlag;
    }

    public WifiEleGeoMagnetic(Context context) {
        this.mContext = context;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            this.isAPISupportedFlag = true;
            this.accSensor = sensorManager.getDefaultSensor(1);
            if (this.accSensor == null) {
                Log.e(getClass().getName(), "GeoMagEleAPI is not supported because accelerometer is not supported");
                this.isAPISupportedFlag = false;
            }
            this.magSensor = this.mSensorManager.getDefaultSensor(2);
            if (this.magSensor == null) {
                Log.e(getClass().getName(), "GeoMagEleAPI is not supported because magnetometer is not supported");
                this.isAPISupportedFlag = false;
            }
            this.baroSensor = this.mSensorManager.getDefaultSensor(6);
            if (this.baroSensor == null) {
                Log.e(getClass().getName(), "GeoMagEleAPI is not supported because barometer is not supported");
                this.isAPISupportedFlag = false;
                return;
            }
            return;
        }
        this.isAPISupportedFlag = false;
    }

    public boolean registerEleDetector(OnEleDetectInterface callback) {
        try {
            this.gARData = new AR_MAIN_DATA();
            this.calc_Feature = new AR_CalcFeature();
            this.eleCore = new EleCore();
            this.baro = new AR_BARODATA();
            this.acc = new AR_SENSORDATA();
            this.mag = new AR_MAG_SENSORDATA();
            if (!(this.gARData == null || this.calc_Feature == null || this.eleCore == null || this.baro == null || this.acc == null)) {
                if (this.mag != null) {
                    Log.e(getClass().getName(), "Ele detector register");
                    this.eleCallback = callback;
                    this.mSensorManager.registerListener(this.mSensorListener, this.mSensorManager.getDefaultSensor(1), 100000);
                    this.mSensorManager.registerListener(this.mSensorListener, this.mSensorManager.getDefaultSensor(2), 100000);
                    this.mSensorManager.registerListener(this.mSensorListener, this.mSensorManager.getDefaultSensor(6), 200000);
                    this.thread = new SensorThread();
                    if (this.thread == null) {
                        return false;
                    }
                    this.thread.setDaemon(true);
                    this.thread.start();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void unregisterEleDetector() {
        SensorThread sensorThread = this.thread;
        if (sensorThread != null) {
            sensorThread.interrupt();
        }
        initSensorData();
        this.mSensorManager.unregisterListener(this.mSensorListener);
        this.eleCallback = null;
    }

    private void initSensorData() {
        this.arrAccData = new float[3];
        this.arrMagData = new float[3];
        this.baroData = DefaultImageRequest.OFFSET_DEFAULT;
    }

    /* access modifiers changed from: private */
    public boolean EleDetectNotifier() {
        AR_SENSORDATA ar_sensordata = this.acc;
        float[] fArr = this.arrAccData;
        ar_sensordata.f15x = fArr[0];
        ar_sensordata.f16y = fArr[1];
        ar_sensordata.f17z = fArr[2];
        this.baro.data = this.baroData;
        AR_MAG_SENSORDATA ar_mag_sensordata = this.mag;
        float[] fArr2 = this.arrMagData;
        ar_mag_sensordata.f12x = fArr2[0];
        ar_mag_sensordata.f13y = fArr2[1];
        ar_mag_sensordata.f14z = fArr2[2];
        this.calc_Feature.AR_Calc_Feature(this.gARData.arFeature, this.acc, this.baro, this.mag);
        boolean result = this.eleCore.AR_Ele_Detect(this.gARData);
        OnEleDetectInterface onEleDetectInterface = this.eleCallback;
        if (onEleDetectInterface != null) {
            if (result) {
                if (!this.mPrevState) {
                    onEleDetectInterface.onEleDetect();
                }
            } else if (this.mPrevState) {
                onEleDetectInterface.onEleNotDetect();
            }
        }
        this.mPrevState = result;
        this.calc_Feature.AR_Calc_UpdateBaroIndex(this.gARData.arFeature);
        this.calc_Feature.AR_Calc_UpdateMagIndex(this.gARData.arFeature);
        return result;
    }

    private class SensorThread extends Thread {
        private SensorThread() {
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WifiEleGeoMagnetic.this.handler.post(new Runnable() {
                        public void run() {
                            int flag = 0;
                            if (WifiEleGeoMagnetic.this.EleDetectNotifier()) {
                                flag = 1;
                            }
                            String str = WifiEleGeoMagnetic.this.arrAccData[0] + "," + WifiEleGeoMagnetic.this.arrAccData[1] + "," + WifiEleGeoMagnetic.this.arrAccData[2] + "," + WifiEleGeoMagnetic.this.arrMagData[0] + "," + WifiEleGeoMagnetic.this.arrMagData[1] + "," + WifiEleGeoMagnetic.this.arrMagData[2] + "," + WifiEleGeoMagnetic.this.baroData + "," + flag + "\n";
                        }
                    });
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                } catch (Throwable th) {
                    Log.d(getClass().getName(), "Ele detector remove");
                    throw th;
                }
            }
            Log.d(getClass().getName(), "Ele detector remove");
        }
    }
}
