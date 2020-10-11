package com.samsung.android.server.wifi.iwc;

import android.util.Log;

public class IwcBigDataMIWC extends IwcBigDataFeature {
    public static final String KEY_IWC_AP_OUI = "IWC_OUI";
    public static final String KEY_IWC_CANDIDATE_LIST_COUNT = "IWC_CLN";
    public static final String KEY_IWC_CORE_LIST_COUNT = "IWC_RLN";
    public static final String KEY_IWC_DEFAULT_QAI = "IWC_DQ";
    public static final String KEY_IWC_DQA = "IWC_DQA";
    public static final String KEY_IWC_DQC = "IWC_DQC";
    public static final String KEY_IWC_DQD = "IWC_DQD";
    public static final String KEY_IWC_DQT = "IWC_DQT";
    public static final String KEY_IWC_EVENT10_COUNT = "IWC_E10";
    public static final String KEY_IWC_EVENT11_COUNT = "IWC_E11";
    public static final String KEY_IWC_EVENT12_COUNT = "IWC_E12";
    public static final String KEY_IWC_EVENT13_COUNT = "IWC_E13";
    public static final String KEY_IWC_EVENT1_COUNT = "IWC_E1";
    public static final String KEY_IWC_EVENT2_COUNT = "IWC_E2";
    public static final String KEY_IWC_EVENT3_COUNT = "IWC_E3";
    public static final String KEY_IWC_EVENT4_COUNT = "IWC_E4";
    public static final String KEY_IWC_EVENT5_COUNT = "IWC_E5";
    public static final String KEY_IWC_EVENT6_COUNT = "IWC_E6";
    public static final String KEY_IWC_EVENT7_COUNT = "IWC_E7";
    public static final String KEY_IWC_EVENT8_COUNT = "IWC_E8";
    public static final String KEY_IWC_EVENT9_COUNT = "IWC_E9";
    public static final String KEY_IWC_EVENT_LIST = "IWC_EL";
    public static final String KEY_IWC_GET_CURRENT_STATE = "IWC_CS";
    public static final String KEY_IWC_ID = "IWC_ID";
    public static final String KEY_IWC_NEW_QAI = "IWC_NQ";
    public static final String KEY_IWC_POORLINK_COUNT = "IWC_PON";
    public static final String KEY_IWC_PREV_QAI = "IWC_PQ";
    public static final String KEY_IWC_PROBATION_LIST_COUNT = "IWC_PLN";
    public static final String KEY_IWC_QAI1_SS_QTABLE = "IWC_SS1";
    public static final String KEY_IWC_QAI2_SS_QTABLE = "IWC_SS2";
    public static final String KEY_IWC_QAI3_SS_QTABLE = "IWC_SS3";
    public static final String KEY_IWC_QTABLE = "IWC_QT";
    public static final String KEY_IWC_QTALBE_COUNT = "IWC_QTN";
    public static final String KEY_IWC_SAD = "IWC_SAD";
    public static final String KEY_IWC_SAV = "IWC_SAV";
    public static final String KEY_IWC_SNS_TOGGLE_COUNT = "IWC_STO";
    public static final String KEY_IWC_SNS_UI_STATE = "IWC_SUI";
    public static final String KEY_IWC_SS_QTALBE_COUNT = "IWC_SSN";
    public static final String KEY_IWC_SS_TIME = "IWC_SST";
    public static final String KEY_IWC_TCL = "IWC_TCL";
    public static final String KEY_IWC_TST = "IWC_TST";
    private static final String[][] MIWC = {new String[]{KEY_IWC_ID, "0"}, new String[]{KEY_IWC_AP_OUI, ""}, new String[]{KEY_IWC_GET_CURRENT_STATE, ""}, new String[]{KEY_IWC_PREV_QAI, "0"}, new String[]{KEY_IWC_NEW_QAI, "0"}, new String[]{KEY_IWC_EVENT_LIST, ""}, new String[]{KEY_IWC_QTABLE, ""}, new String[]{KEY_IWC_POORLINK_COUNT, "0"}, new String[]{KEY_IWC_SNS_UI_STATE, "0"}, new String[]{KEY_IWC_SNS_TOGGLE_COUNT, "0"}, new String[]{KEY_IWC_QAI1_SS_QTABLE, "0"}, new String[]{KEY_IWC_QAI2_SS_QTABLE, "0"}, new String[]{KEY_IWC_QAI3_SS_QTABLE, "0"}, new String[]{KEY_IWC_DEFAULT_QAI, "0"}, new String[]{KEY_IWC_SS_TIME, "0"}, new String[]{KEY_IWC_QTALBE_COUNT, "0"}, new String[]{KEY_IWC_SS_QTALBE_COUNT, "0"}, new String[]{KEY_IWC_CANDIDATE_LIST_COUNT, "0"}, new String[]{KEY_IWC_CORE_LIST_COUNT, "0"}, new String[]{KEY_IWC_PROBATION_LIST_COUNT, "0"}, new String[]{KEY_IWC_EVENT1_COUNT, "0"}, new String[]{KEY_IWC_EVENT2_COUNT, "0"}, new String[]{KEY_IWC_EVENT3_COUNT, "0"}, new String[]{KEY_IWC_EVENT4_COUNT, "0"}, new String[]{KEY_IWC_EVENT5_COUNT, "0"}, new String[]{KEY_IWC_EVENT6_COUNT, "0"}, new String[]{KEY_IWC_EVENT7_COUNT, "0"}, new String[]{KEY_IWC_EVENT8_COUNT, "0"}, new String[]{KEY_IWC_EVENT9_COUNT, "0"}, new String[]{KEY_IWC_EVENT10_COUNT, "0"}, new String[]{KEY_IWC_EVENT11_COUNT, "0"}, new String[]{KEY_IWC_EVENT12_COUNT, "0"}, new String[]{KEY_IWC_EVENT13_COUNT, "0"}, new String[]{KEY_IWC_TST, "0"}, new String[]{KEY_IWC_TCL, "0"}, new String[]{KEY_IWC_DQC, "0"}, new String[]{KEY_IWC_DQD, "0"}, new String[]{KEY_IWC_DQT, "0"}, new String[]{KEY_IWC_DQA, ""}, new String[]{KEY_IWC_SAV, "0"}, new String[]{KEY_IWC_SAD, "0"}};

    public IwcBigDataMIWC(boolean dqaEnabled, String dqaFeatureName) {
        super(dqaEnabled, dqaFeatureName);
    }

    public IwcBigDataMIWC() {
    }

    public String getJsonFormat() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(getKeyValueStrings(MIWC));
            if (DBG) {
                String str = this.TAG;
                Log.d(str, "getJsonFormat - " + sb.toString());
            }
        } catch (Exception e) {
            if (DBG) {
                String str2 = this.TAG;
                Log.w(str2, "Exception occured on getJsonFormat - " + e);
            }
            e.printStackTrace();
        }
        return sb.toString();
    }
}
