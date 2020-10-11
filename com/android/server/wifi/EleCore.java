package com.android.server.wifi;

import android.util.Log;

/* compiled from: WifiEleGeoMagnetic */
class EleCore {
    static final int AR_ELE_ACC_TIME = 10;
    static final int AR_ELE_BARO_SIGN = 20;
    static final int AR_ELE_BARO_TIME = 1;
    static final int AR_ELE_HIGH_BOUND_ACC = 121;
    static final int AR_ELE_LOW_BOUND_ACC = 64;
    static final int AR_ELE_MAG_TIME = 20;
    static final float AR_GRAD_BARO_ELE = 0.05f;
    static final float AR_GRAD_MAG_ELE = 15.0f;
    static final float TH_GRAD_BARO = 0.03f;
    AR_ELE_DATA gAREle = new AR_ELE_DATA();

    /* access modifiers changed from: package-private */
    public void AR_Ele_Classification_Baro() {
        if (Math.abs(this.gAREle.gradBaro) < AR_GRAD_BARO_ELE) {
            this.gAREle.eleBaroTime = 0;
        } else if (this.gAREle.eleBaroTime < 1) {
            AR_ELE_DATA ar_ele_data = this.gAREle;
            ar_ele_data.eleBaroTime = (byte) (ar_ele_data.eleBaroTime + 1);
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Ele_Classification_Mag(AR_MAGFEATURE magFeature) {
        if (magFeature.magGradNorm < AR_GRAD_MAG_ELE) {
            this.gAREle.eleMagTime = 0;
        } else if (this.gAREle.eleMagTime < 20) {
            AR_ELE_DATA ar_ele_data = this.gAREle;
            ar_ele_data.eleMagTime = (byte) (ar_ele_data.eleMagTime + 1);
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Ele_Classification_Accel(AR_ACCFEATURE accFeature) {
        if (accFeature.accelNorm < 64.0f || accFeature.accelNorm > 121.0f) {
            this.gAREle.eleAccTime = 0;
        } else if (this.gAREle.eleAccTime < 10) {
            AR_ELE_DATA ar_ele_data = this.gAREle;
            ar_ele_data.eleAccTime = (byte) (ar_ele_data.eleAccTime + 1);
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Ele_Classification(AR_MAIN_DATA ARMainData) {
        AR_Ele_Classification_Baro();
        AR_Ele_Classification_Accel(ARMainData.arFeature.accFeature);
        AR_Ele_Classification_Mag(ARMainData.arFeature.magFeature);
        if (this.gAREle.eleMagTime == 20 && this.gAREle.eleAccTime == 10 && this.gAREle.eleBaroTime == 1 && this.gAREle.gradBaro_Sign != 0) {
            ARMainData.EleInfo.isActivity = true;
        } else if (this.gAREle.eleMagTime != 20 && this.gAREle.eleBaroTime != 1) {
            ARMainData.EleInfo.isActivity = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void AR_Ele_GetGradient(AR_BAROFEATURE baroFeature) {
        this.gAREle.gradBaro = baroFeature.lowpassBaro[baroFeature.index] - baroFeature.lowpassBaro[(baroFeature.index + 1) % 5];
        if ((this.gAREle.gradBaro >= TH_GRAD_BARO && this.gAREle.gradBaro_UP >= 1) || (this.gAREle.gradBaro <= -0.03f && this.gAREle.gradBaro_DN >= 1)) {
            AR_ELE_DATA ar_ele_data = this.gAREle;
            ar_ele_data.gradBaro_Sign = 0;
            ar_ele_data.gradBaro_UP = 0;
            ar_ele_data.gradBaro_DN = 0;
        } else if (this.gAREle.gradBaro >= TH_GRAD_BARO) {
            this.gAREle.gradBaro_DN++;
            AR_ELE_DATA ar_ele_data2 = this.gAREle;
            ar_ele_data2.gradBaro_UP = 0;
            if (ar_ele_data2.gradBaro_DN >= 20) {
                this.gAREle.gradBaro_Sign = -1;
            } else {
                this.gAREle.gradBaro_Sign = 0;
            }
        } else if (this.gAREle.gradBaro <= -0.03f) {
            AR_ELE_DATA ar_ele_data3 = this.gAREle;
            ar_ele_data3.gradBaro_DN = 0;
            ar_ele_data3.gradBaro_UP++;
            if (this.gAREle.gradBaro_UP >= 20) {
                this.gAREle.gradBaro_Sign = 1;
            } else {
                this.gAREle.gradBaro_Sign = 0;
            }
        } else {
            AR_ELE_DATA ar_ele_data4 = this.gAREle;
            ar_ele_data4.gradBaro_Sign = 0;
            ar_ele_data4.gradBaro_DN = 0;
            ar_ele_data4.gradBaro_UP = 0;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean AR_Ele_Detect(AR_MAIN_DATA ARMainData) {
        if (ARMainData.arFeature.bufIsReady == 0) {
            return false;
        }
        ARMainData.EleInfo.previsActivity = ARMainData.EleInfo.isActivity;
        AR_Ele_GetGradient(ARMainData.arFeature.baroFeature);
        AR_Ele_Classification(ARMainData);
        if (ARMainData.EleInfo.previsActivity != ARMainData.EleInfo.isActivity) {
            String name = getClass().getName();
            Log.d(name, "STATE CHANGE FROM" + ARMainData.EleInfo.previsActivity + "TO " + ARMainData.EleInfo.isActivity);
        }
        return ARMainData.EleInfo.isActivity;
    }
}
