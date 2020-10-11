package com.android.server.wifi;

import com.android.server.wifi.p2p.common.DefaultImageRequest;

/* compiled from: WifiEleGeoMagnetic */
class AR_CalcFeature {
    AR_CalcFeature() {
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_SaveAccData(AR_FEATURE ARFeatureData, AR_SENSORDATA acc) {
        ARFeatureData.accData = acc;
        ARFeatureData.accFeature.accelNorm = (float) (Math.pow((double) acc.f15x, 2.0d) + Math.pow((double) acc.f16y, 2.0d) + Math.pow((double) acc.f17z, 2.0d));
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_SaveBaroData(AR_FEATURE ARFeatureData, AR_BARODATA baro) {
        ARFeatureData.baroData = baro;
        if (ARFeatureData.bufIsReady == 0) {
            ARFeatureData.baroFeature.lowpassBaro[ARFeatureData.baroFeature.index] = baro.data;
        } else {
            ARFeatureData.baroFeature.lowpassBaro[ARFeatureData.baroFeature.index] = (ARFeatureData.baroFeature.lowpassBaro[((ARFeatureData.baroFeature.index + 5) - 1) % 5] * 0.7f) + (baro.data * 0.3f);
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_SaveMagData(AR_FEATURE ARFeatureData, AR_MAG_SENSORDATA mag) {
        ARFeatureData.magData = mag;
        ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f12x = mag.f12x;
        ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f13y = mag.f13y;
        ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f14z = mag.f14z;
        if (ARFeatureData.bufIsReadyMag == 0) {
            ARFeatureData.magFeature.magGrad.f12x = DefaultImageRequest.OFFSET_DEFAULT;
            ARFeatureData.magFeature.magGrad.f13y = DefaultImageRequest.OFFSET_DEFAULT;
            ARFeatureData.magFeature.magGrad.f14z = DefaultImageRequest.OFFSET_DEFAULT;
        } else {
            ARFeatureData.magFeature.magGrad.f12x = ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f12x - ARFeatureData.magFeature.magRaw[(ARFeatureData.magFeature.index + 1) % 5].f12x;
            ARFeatureData.magFeature.magGrad.f13y = ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f13y - ARFeatureData.magFeature.magRaw[(ARFeatureData.magFeature.index + 1) % 5].f13y;
            ARFeatureData.magFeature.magGrad.f14z = ARFeatureData.magFeature.magRaw[ARFeatureData.magFeature.index].f14z - ARFeatureData.magFeature.magRaw[(ARFeatureData.magFeature.index + 1) % 5].f14z;
        }
        ARFeatureData.magFeature.magGradNorm = (float) (Math.pow((double) ARFeatureData.magFeature.magGrad.f12x, 2.0d) + Math.pow((double) ARFeatureData.magFeature.magGrad.f13y, 2.0d) + Math.pow((double) ARFeatureData.magFeature.magGrad.f14z, 2.0d));
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_UpdateBaroIndex(AR_FEATURE ARFeatureData) {
        ARFeatureData.baroFeature.index = (ARFeatureData.baroFeature.index + 1) % 5;
        if (ARFeatureData.baroFeature.index == 0) {
            ARFeatureData.bufIsReady = 1;
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_UpdateMagIndex(AR_FEATURE ARFeatureData) {
        ARFeatureData.magFeature.index = (ARFeatureData.magFeature.index + 1) % 5;
        if (ARFeatureData.magFeature.index == 0) {
            ARFeatureData.bufIsReadyMag = 1;
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Calc_Feature(AR_FEATURE ARFeatureData, AR_SENSORDATA acc, AR_BARODATA baro, AR_MAG_SENSORDATA mag) {
        AR_Calc_SaveAccData(ARFeatureData, acc);
        AR_Calc_SaveBaroData(ARFeatureData, baro);
        AR_Calc_SaveMagData(ARFeatureData, mag);
    }
}
