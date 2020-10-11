package com.android.server.wifi.iwc;

public class IWCBDTracking {

    /* renamed from: NA */
    private static int f28NA = -10;
    private static long NAL = -10;
    private int mCLN;
    private String mCS = null;
    private int mDQ;
    private String[] mDQA = {"", "", "", "", ""};
    private int mDQC;
    private long mDQD;
    private long mDQT;
    private long mDSB;
    private String mDefaultSns = "-1";

    /* renamed from: mE */
    private int[] f29mE = new int[13];
    private String mEL = null;
    private int mID;
    private int mIdx;
    private int mNQ;
    private String mOUI = null;
    private int mPLN;
    private int mPON;
    private int mPQ;
    private String mQT = null;
    private int mQTN;
    private int mRLN;
    private boolean mSAV;
    private int[] mSS = new int[3];
    private int mSSN;
    private long mSST;
    private int mSTO;
    private int mSUI;
    private int mTCL;
    private long mTSD;
    private String mVersion = "V.0003";

    public IWCBDTracking() {
        int i = f28NA;
        this.mPQ = i;
        this.mNQ = i;
        this.mID = 3;
        this.mSST = NAL;
        this.mQTN = i;
        this.mSSN = i;
        this.mCLN = i;
        this.mRLN = i;
        this.mPLN = i;
        this.mSUI = i;
        this.mSTO = i;
        for (int a = 0; a < 13; a++) {
            this.f29mE[a] = 0;
        }
        for (int a2 = 0; a2 < 3; a2++) {
            this.mSS[a2] = 0;
        }
        int i2 = f28NA;
        this.mDQ = i2;
        this.mPON = i2;
        this.mTSD = 0;
        this.mTCL = 0;
        this.mDQC = 0;
        this.mDQD = 0;
        this.mDQT = 0;
        this.mDSB = 0;
        for (int a3 = 0; a3 < 5; a3++) {
            this.mDQA[a3] = null;
        }
        this.mIdx = 0;
        this.mSAV = false;
    }

    public void setPoorLinkCountInfo(int c) {
        this.mPON = c;
    }

    public int getPoorLinkCountInfo() {
        return this.mPON;
    }

    public void setSNSUIStateInfo(int ui, int togglec) {
        this.mSUI = ui;
        this.mSTO = togglec;
    }

    public int getSNSUIStateInfo() {
        return this.mSUI;
    }

    public int getSNSToggleInfo() {
        return this.mSTO;
    }

    public void setIdInfo(int id) {
        this.mID = id;
    }

    public int getIdInfo() {
        return this.mID;
    }

    public void setQTCountInfo(int qtn, int ssn) {
        this.mQTN = qtn;
        this.mSSN = ssn;
    }

    public int getQTCountInfo(int key) {
        if (key == 0) {
            return this.mQTN;
        }
        return this.mSSN;
    }

    public void setListCountInfo(int cln, int rln, int pln) {
        this.mCLN = cln;
        this.mRLN = rln;
        this.mPLN = pln;
    }

    public int getListCountInfo(int key) {
        if (key == 0) {
            return this.mCLN;
        }
        if (key == 1) {
            return this.mRLN;
        }
        return this.mPLN;
    }

    public void setSSCountInfo(int s1, int s2, int s3) {
        int[] iArr = this.mSS;
        iArr[0] = s1;
        iArr[1] = s2;
        iArr[2] = s3;
    }

    public int getSSCountInfo(int key) {
        return this.mSS[key - 1];
    }

    public void setDefaultQaiInfo(int qai) {
        this.mDQ = qai;
    }

    public int getDefaultQaiInfo() {
        return this.mDQ;
    }

    public void cleanBD() {
        if (this.mID == 3) {
            this.mOUI = null;
            this.mCS = null;
            this.mEL = null;
            this.mQT = null;
            int i = f28NA;
            this.mPQ = i;
            this.mNQ = i;
            this.mSST = NAL;
            this.mQTN = i;
            this.mSSN = i;
            this.mCLN = i;
            this.mRLN = i;
            this.mPLN = i;
            this.mSUI = i;
            this.mSTO = i;
            for (int a = 0; a < 13; a++) {
                this.f29mE[a] = 0;
            }
            for (int a2 = 0; a2 < 3; a2++) {
                this.mSS[a2] = 0;
            }
            int a3 = f28NA;
            this.mDQ = a3;
            this.mPON = a3;
            this.mDQC = 0;
            this.mDQD = 0;
            this.mDQT = 0;
            this.mDSB = 0;
            for (int a4 = 0; a4 < 5; a4++) {
                this.mDQA[a4] = null;
            }
            this.mIdx = 0;
            this.mSAV = false;
        }
        this.mID = 3;
    }

    public void set24HEventAccWithIdx(int idx) {
        int[] iArr = this.f29mE;
        iArr[idx] = iArr[idx] + 1;
    }

    public int get24HEventAccWithIdx(int key) {
        return this.f29mE[key - 1];
    }

    public void setOUIInfo(String bss) {
        if (bss != null) {
            this.mOUI = bss.substring(0, 8);
        }
    }

    public String getOUIInfo() {
        return this.mOUI;
    }

    public void setStateInfo(boolean sw) {
        if (sw) {
            this.mCS = "switched";
        } else {
            this.mCS = "connected";
        }
    }

    public String getStateInfo() {
        return this.mCS;
    }

    public void setQAIInfo(int prev, int now) {
        this.mPQ = prev + 1;
        this.mNQ = now + 1;
    }

    public int getQAIInfo(int key) {
        if (key == 0) {
            return this.mPQ;
        }
        return this.mNQ;
    }

    public void setEVInfo(String ev) {
        this.mEL = ev;
    }

    public String getEVInfo() {
        return this.mEL;
    }

    public void setQTableValueInfo(String qtv) {
        this.mQT = qtv;
    }

    public String getQTableValueInfo() {
        return this.mQT;
    }

    public void setSSTakenTimeInfo(long time) {
        this.mSST = time;
    }

    public long getSSTakenTimeInfo() {
        return this.mSST;
    }

    public long getTipsShowingDuration() {
        return this.mTSD;
    }

    public void setTipsShowingDuration(long time) {
        this.mTSD = time;
    }

    public int getTipsClick() {
        return this.mTCL;
    }

    public void setTipsClick(boolean isClick) {
        this.mTCL = isClick;
    }

    public void setCntOfDQAI() {
        this.mDQC++;
    }

    public int getCntOfDQAI() {
        return this.mDQC;
    }

    public void setAvgDurOfDQAI(long duration) {
        this.mDQD += duration;
    }

    public long getAvgDurOfDQAI() {
        int i = this.mDQC;
        if (i > 0) {
            return this.mDQD / ((long) i);
        }
        return 0;
    }

    public void setAvgDataRateOfDQAI(long trxBytes) {
        this.mDQT += trxBytes;
    }

    public long getAvgDataRateOfDQAI() {
        long j = this.mDQD;
        if (j > 1000) {
            return (this.mDQT / (j / 1000)) * 8;
        }
        return 0;
    }

    public void setAppNameOfDQAI(String name) {
        if (name != null) {
            if (this.mIdx > 4) {
                this.mIdx = 0;
            }
            String[] strArr = this.mDQA;
            int i = this.mIdx;
            this.mIdx = i + 1;
            strArr[i] = name;
        }
    }

    public String getAppNameOfDQAI() {
        String buf = "";
        for (int i = 0; i < 5; i++) {
            buf = buf + this.mDQA[i];
            if (i < 5) {
                buf = buf + " ";
            }
        }
        return (((buf + "DEFAULT_ON:") + this.mDefaultSns) + " ") + this.mVersion;
    }

    public void setIsSaverOfDQAI(boolean isSaver) {
        this.mSAV = isSaver;
    }

    public void setSavedBytes(long bytes) {
        this.mDSB += bytes;
    }

    public long getSavedBytes() {
        return this.mDSB;
    }

    public boolean getIsSaverOfDQAI() {
        return this.mSAV;
    }

    public void setDefaultSns(String value) {
        this.mDefaultSns = value;
    }
}
