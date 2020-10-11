package com.android.server.wifi;

/* compiled from: WifiEleGeoMagnetic */
class AR_MAGFEATURE {
    static final int AR_BUFFER_SIZE_MAG = 5;
    int index;
    AR_MAG_SENSORDATA magGrad = new AR_MAG_SENSORDATA();
    float magGradNorm;
    AR_MAG_SENSORDATA[] magRaw = new AR_MAG_SENSORDATA[5];

    public AR_MAGFEATURE() {
        for (int i = 0; i < 5; i++) {
            this.magRaw[i] = new AR_MAG_SENSORDATA();
        }
    }
}
