package com.samsung.android.server.wifi.sns;

import android.util.Log;

public class SnsBigDataSCNT extends SnsBigDataFeature {
    public static final String KEY_SNS_ELE_GP = "SEGP";
    public static final String KEY_SNS_ELE_PG = "SEPG";
    public static final String KEY_SNS_GQ_INV_AGG = "GIAG";
    public static final String KEY_SNS_GQ_INV_NON = "GINS";
    public static final String KEY_SNS_GQ_INV_NORMAL = "GINO";
    public static final String KEY_SNS_GQ_PQ_AGG = "QPAG";
    public static final String KEY_SNS_GQ_PQ_NON = "QPNS";
    public static final String KEY_SNS_GQ_PQ_NORMAL = "QPNO";
    public static final String KEY_SNS_INV_GQ_AGG = "IGAG";
    public static final String KEY_SNS_INV_GQ_NON = "IGNS";
    public static final String KEY_SNS_INV_GQ_NORMAL = "IGNO";
    public static final String KEY_SNS_INV_PQ_AGG = "IPAG";
    public static final String KEY_SNS_INV_PQ_NON = "IPNS";
    public static final String KEY_SNS_INV_PQ_NORMAL = "IPNO";
    public static final String KEY_SNS_IV_AGG = "IVAG";
    public static final String KEY_SNS_IV_NONSWITCHABLE = "IVNS";
    public static final String KEY_SNS_IV_NORMAL = "IVNO";
    public static final String KEY_SNS_IWC_MW = "SIMW";
    public static final String KEY_SNS_IWC_WM = "SIWM";
    public static final String KEY_SNS_PQ_GQ_AGG = "QPAG";
    public static final String KEY_SNS_PQ_GQ_NON = "QPNS";
    public static final String KEY_SNS_PQ_GQ_NORMAL = "QPNO";
    public static final String KEY_SNS_PQ_INV_AGG = "PIAG";
    public static final String KEY_SNS_PQ_INV_NON = "PINS";
    public static final String KEY_SNS_PQ_INV_NORMAL = "PINO";
    public static final String KEY_SNS_TURN_OFF = "SSMO";
    public static final String KEY_SNS_TURN_ON_AGG = "SSMA";
    public static final String KEY_SNS_TURN_ON_NORMAL = "SSMN";
    private static final String KEY_SNS_VERSION = "SVER";
    public static final String KEY_SNS_VI_AGG = "VIAG";
    public static final String KEY_SNS_VI_NONSWITCHABLE = "VINS";
    public static final String KEY_SNS_VI_NORMAL = "VINO";
    private static final String[][] SCNT = {new String[]{KEY_SNS_VERSION, "2020001029"}, new String[]{KEY_SNS_IV_NONSWITCHABLE, "0"}, new String[]{KEY_SNS_IV_NORMAL, "0"}, new String[]{KEY_SNS_IV_AGG, "0"}, new String[]{KEY_SNS_VI_NONSWITCHABLE, "0"}, new String[]{KEY_SNS_VI_NORMAL, "0"}, new String[]{KEY_SNS_VI_AGG, "0"}, new String[]{KEY_SNS_GQ_INV_NON, "0"}, new String[]{KEY_SNS_PQ_INV_NON, "0"}, new String[]{KEY_SNS_INV_GQ_NON, "0"}, new String[]{KEY_SNS_INV_PQ_NON, "0"}, new String[]{"QPNS", "0"}, new String[]{"QPNS", "0"}, new String[]{KEY_SNS_GQ_INV_NORMAL, "0"}, new String[]{KEY_SNS_PQ_INV_NORMAL, "0"}, new String[]{KEY_SNS_INV_GQ_NORMAL, "0"}, new String[]{KEY_SNS_INV_PQ_NORMAL, "0"}, new String[]{"QPNO", "0"}, new String[]{"QPNO", "0"}, new String[]{KEY_SNS_GQ_INV_AGG, "0"}, new String[]{KEY_SNS_PQ_INV_AGG, "0"}, new String[]{KEY_SNS_INV_GQ_AGG, "0"}, new String[]{KEY_SNS_INV_PQ_AGG, "0"}, new String[]{"QPAG", "0"}, new String[]{"QPAG", "0"}, new String[]{KEY_SNS_IWC_WM, "0"}, new String[]{KEY_SNS_IWC_MW, "0"}, new String[]{KEY_SNS_ELE_GP, "0"}, new String[]{KEY_SNS_ELE_PG, "0"}, new String[]{KEY_SNS_TURN_OFF, "0"}, new String[]{KEY_SNS_TURN_ON_NORMAL, "0"}, new String[]{"SSMA", "0"}};
    public int mEleGP;
    public int mElePG;
    public int mGqInvAgg;
    public int mGqInvNon;
    public int mGqInvNormal;
    public int mGqPqAgg;
    public int mGqPqNon;
    public int mGqPqNormal;
    public int mIVAGG;
    public int mIVNonSwitchable;
    public int mIVNormal;
    public int mInvGqAgg;
    public int mInvGqNon;
    public int mInvGqNormal;
    public int mInvPqAgg;
    public int mInvPqNon;
    public int mInvPqNormal;
    public int mIwcMW;
    public int mIwcWM;
    public int mPqGqAgg;
    public int mPqGqNon;
    public int mPqGqNormal;
    public int mPqInvAgg;
    public int mPqInvNon;
    public int mPqInvNormal;
    public int mTurnedOff;
    public int mTurnedOnAGG;
    public int mTurnedOnNormal;
    public int mVIAGG;
    public int mVINonSwitchable;
    public int mVINormal;

    public SnsBigDataSCNT() {
        initialize();
    }

    public SnsBigDataSCNT(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
        initialize();
    }

    public void initialize() {
        this.mIVNonSwitchable = 0;
        this.mIVNormal = 0;
        this.mIVAGG = 0;
        this.mVINonSwitchable = 0;
        this.mVINormal = 0;
        this.mVIAGG = 0;
        this.mGqInvNon = 0;
        this.mPqInvNon = 0;
        this.mInvGqNon = 0;
        this.mInvPqNon = 0;
        this.mGqPqNon = 0;
        this.mPqGqNon = 0;
        this.mGqInvNormal = 0;
        this.mPqInvNormal = 0;
        this.mInvGqNormal = 0;
        this.mInvPqNormal = 0;
        this.mGqPqNormal = 0;
        this.mPqGqNormal = 0;
        this.mGqInvAgg = 0;
        this.mPqInvAgg = 0;
        this.mInvGqAgg = 0;
        this.mInvPqAgg = 0;
        this.mGqPqAgg = 0;
        this.mPqGqAgg = 0;
        this.mIwcWM = 0;
        this.mIwcMW = 0;
        this.mEleGP = 0;
        this.mElePG = 0;
        this.mTurnedOff = 0;
        this.mTurnedOnNormal = 0;
        this.mTurnedOnAGG = 0;
    }

    public void addOrUpdateAllValue() {
        addOrUpdateValue(KEY_SNS_IV_NONSWITCHABLE, this.mIVNonSwitchable);
        addOrUpdateValue(KEY_SNS_IV_NORMAL, this.mIVNormal);
        addOrUpdateValue(KEY_SNS_IV_AGG, this.mIVAGG);
        addOrUpdateValue(KEY_SNS_VI_NONSWITCHABLE, this.mVINonSwitchable);
        addOrUpdateValue(KEY_SNS_VI_NORMAL, this.mVINormal);
        addOrUpdateValue(KEY_SNS_VI_AGG, this.mVIAGG);
        addOrUpdateValue(KEY_SNS_GQ_INV_NON, this.mGqInvNon);
        addOrUpdateValue(KEY_SNS_PQ_INV_NON, this.mPqInvNon);
        addOrUpdateValue(KEY_SNS_INV_GQ_NON, this.mInvGqNon);
        addOrUpdateValue(KEY_SNS_INV_PQ_NON, this.mInvPqNon);
        addOrUpdateValue("QPNS", this.mGqPqNon);
        addOrUpdateValue("QPNS", this.mPqGqNon);
        addOrUpdateValue(KEY_SNS_GQ_INV_NORMAL, this.mGqInvNormal);
        addOrUpdateValue(KEY_SNS_PQ_INV_NORMAL, this.mPqInvNormal);
        addOrUpdateValue(KEY_SNS_INV_GQ_NORMAL, this.mInvGqNormal);
        addOrUpdateValue(KEY_SNS_INV_PQ_NORMAL, this.mInvPqNormal);
        addOrUpdateValue("QPNO", this.mGqPqNormal);
        addOrUpdateValue("QPNO", this.mPqGqNormal);
        addOrUpdateValue(KEY_SNS_GQ_INV_AGG, this.mGqInvAgg);
        addOrUpdateValue(KEY_SNS_PQ_INV_AGG, this.mPqInvAgg);
        addOrUpdateValue(KEY_SNS_INV_GQ_AGG, this.mInvGqAgg);
        addOrUpdateValue(KEY_SNS_INV_PQ_AGG, this.mInvPqAgg);
        addOrUpdateValue("QPAG", this.mGqPqAgg);
        addOrUpdateValue("QPAG", this.mPqGqAgg);
        addOrUpdateValue(KEY_SNS_IWC_WM, this.mIwcWM);
        addOrUpdateValue(KEY_SNS_IWC_MW, this.mIwcMW);
        addOrUpdateValue(KEY_SNS_ELE_GP, this.mEleGP);
        addOrUpdateValue(KEY_SNS_ELE_PG, this.mElePG);
        addOrUpdateValue(KEY_SNS_TURN_OFF, this.mTurnedOff);
        addOrUpdateValue(KEY_SNS_TURN_ON_NORMAL, this.mTurnedOnNormal);
        addOrUpdateValue("SSMA", this.mTurnedOnAGG);
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(getKeyValueStrings(SCNT));
        if (DBG) {
            String str = this.TAG;
            Log.d(str, "getJsonFormat - " + sb.toString());
        }
        return sb.toString();
    }

    public void putKeyValueString(String[] key) {
        if (key != null) {
            for (int i = 0; i < key.length; i++) {
                SCNT[i][1] = key[i];
            }
        }
    }
}
