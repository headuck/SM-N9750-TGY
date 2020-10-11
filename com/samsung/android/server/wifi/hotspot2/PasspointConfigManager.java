package com.samsung.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.hotspot2.pps.Policy;
import android.net.wifi.hotspot2.pps.UpdateParameter;
import android.os.Debug;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.LocalLog;
import android.util.Log;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class PasspointConfigManager {
    private static final int CERTIFICATE_FINGERPRINT_BYTES = 32;
    private static boolean DBG = Debug.semIsProductDev();
    private static final int MAX_URL_BYTES = 1023;
    private static final String SEC_FRIENDLY_NAME = "Samsung Hotspot2.0 Profile";
    private static final String TAG = "PasspointConfigManager";
    private static final String VENDOR_FRIENDLY_NAME = "Vendor Hotspot2.0 Profile";
    private static int mProviderType;
    public String mCaCertificateKey;
    public String mClientCertificate;
    public String mClientKeyPassword;
    public String mClientPrivateKey;
    private Context mContext;
    public String mEapType;
    public String mFqdn;
    public String mFriendlyName;
    public String mImsi;
    private final LocalLog mLocalLog;
    public String mNonEAPInnerMethod;
    public String mPassword;
    public String mPriority;
    public String mRealm;
    public long[] mRoamingConsortiumOis;
    public int mUpdateIdentifier;
    public String mUsername;

    /* renamed from: sb */
    StringBuffer f52sb = new StringBuffer();

    public PasspointConfigManager(Context context, LocalLog localLog) {
        this.mContext = context;
        this.mLocalLog = localLog;
        this.mFqdn = null;
        this.mFriendlyName = null;
        this.mEapType = null;
        this.mUsername = null;
        this.mPassword = null;
        this.mNonEAPInnerMethod = null;
        this.mRealm = null;
        this.mImsi = null;
        this.mCaCertificateKey = null;
        this.mClientCertificate = null;
        this.mClientPrivateKey = null;
        this.mClientKeyPassword = null;
        this.mPriority = null;
        this.mUpdateIdentifier = Integer.MIN_VALUE;
        this.mRoamingConsortiumOis = null;
    }

    private HomeSp createHomeSp() {
        if (TextUtils.isEmpty(this.mFqdn)) {
            loge(TAG, "createHomeSp fqdn is empty.");
            return null;
        }
        HomeSp homeSp = new HomeSp();
        logd(TAG, "createHomeSp, mFqdn:" + this.mFqdn + ", mFriendlyName: " + this.mFriendlyName);
        if (TextUtils.isEmpty(this.mFriendlyName)) {
            int i = mProviderType;
            if (i == 1) {
                this.mFriendlyName = VENDOR_FRIENDLY_NAME;
            } else if (i == 3) {
                this.mFriendlyName = SEC_FRIENDLY_NAME;
            } else {
                this.mFriendlyName = this.mFqdn;
            }
        }
        if (this.mRoamingConsortiumOis != null) {
            for (int i2 = 0; i2 < this.mRoamingConsortiumOis.length; i2++) {
                logd(TAG, "createHomeSp, mRoamingConsortiumOis[" + i2 + "]: " + this.mRoamingConsortiumOis[i2]);
            }
        }
        homeSp.setFqdn(this.mFqdn);
        homeSp.setFriendlyName(this.mFriendlyName);
        homeSp.setRoamingConsortiumOis(this.mRoamingConsortiumOis);
        homeSp.setProviderType(mProviderType);
        return homeSp;
    }

    private Credential createCredential() {
        int eapType;
        if (TextUtils.isEmpty(this.mRealm)) {
            this.mRealm = this.mFqdn;
        }
        Credential credential = new Credential();
        credential.setRealm(this.mRealm);
        if ("SIM".equals(this.mEapType) || "AKA".equals(this.mEapType) || "AKA'".equals(this.mEapType)) {
            if (TextUtils.isEmpty(this.mImsi)) {
                loge(TAG, "createCredential, mImsi is empty.");
                this.mImsi = "?";
            }
            if ("?".equals(this.mImsi)) {
                if (isUSimPresent()) {
                    this.mImsi = getImsi();
                    if (TextUtils.isEmpty(this.mImsi)) {
                        this.mImsi = "00101*";
                        loge(TAG, "createCredential, USIM is present, but IMSI is null. so set 00101 to IMSI");
                    } else if (DBG) {
                        logd(TAG, "createCredential, getSubscriberId from sim, IMSI: " + this.mImsi);
                    }
                } else {
                    this.mImsi = "00101*";
                }
            }
            if ("SIM".equals(this.mEapType)) {
                eapType = 18;
            } else if ("AKA".equals(this.mEapType)) {
                eapType = 23;
            } else {
                eapType = 50;
            }
            if (DBG) {
                logd(TAG, "createCredential, mRealm:" + this.mRealm + ", mEapType: " + this.mEapType + ", mImsi: " + this.mImsi);
            }
            Credential.SimCredential simCredential = new Credential.SimCredential();
            simCredential.setImsi(this.mImsi);
            simCredential.setEapType(eapType);
            credential.setSimCredential(simCredential);
            credential.setUserCredential((Credential.UserCredential) null);
            credential.setCertCredential((Credential.CertificateCredential) null);
        } else {
            if ("TLS".equals(this.mEapType)) {
                Credential.CertificateCredential certCredential = new Credential.CertificateCredential();
                certCredential.setCertType("x509v3");
                certCredential.setCredentialType(1);
                credential.setCertCredential(certCredential);
                logd(TAG, "createCredential, TLS , mRealm:" + this.mRealm + ", mEapType: " + this.mEapType);
            } else if ("TTLS".equals(this.mEapType) || (!TextUtils.isEmpty(this.mUsername) && !TextUtils.isEmpty(this.mPassword))) {
                Credential.UserCredential userCredential = new Credential.UserCredential();
                userCredential.setUsername(this.mUsername);
                if (TextUtils.isEmpty(this.mEapType)) {
                    this.mEapType = "TTLS";
                }
                if (!TextUtils.isEmpty(this.mPassword)) {
                    userCredential.setPassword(Base64.encodeToString(this.mPassword.getBytes(), 0));
                }
                if (TextUtils.isEmpty(this.mNonEAPInnerMethod)) {
                    this.mNonEAPInnerMethod = "MS-CHAP-V2";
                }
                userCredential.setEapType(21);
                userCredential.setNonEapInnerMethod(this.mNonEAPInnerMethod);
                userCredential.setCredentialType(1);
                credential.setUserCredential(userCredential);
                credential.setCertCredential((Credential.CertificateCredential) null);
                logd(TAG, "createCredential, TTLS, mRealm:" + this.mRealm + ", mEapType: " + this.mEapType + ", mUsername: " + this.mUsername + ", mPassword:" + this.mPassword + ", mNonEAPInnerMethod:" + this.mNonEAPInnerMethod);
            } else {
                loge(TAG, "createCredential, mEapType(" + this.mEapType + ") is unknow ");
                return null;
            }
            credential.setSimCredential((Credential.SimCredential) null);
        }
        credential.setCaCertificate((X509Certificate) null);
        credential.setClientCertificateChain((X509Certificate[]) null);
        credential.setClientPrivateKey((PrivateKey) null);
        return credential;
    }

    private Policy createPolicy() {
        return new Policy();
    }

    private UpdateParameter createSubscriptionUpdate() {
        return new UpdateParameter();
    }

    private boolean isUSimPresent() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm == null) {
            loge(TAG, "isUSimPresent, TelephonyManager is null.");
            return false;
        }
        boolean isUsimPresent = false;
        int simSlotCount = tm.getPhoneCount();
        if (DBG) {
            logd(TAG, "isUSimPresent, simSlotCount: " + simSlotCount);
        }
        for (int slotId = 0; slotId < simSlotCount; slotId++) {
            if (tm.getSimState(slotId) != 1) {
                isUsimPresent = true;
                if (DBG) {
                    logd(TAG, "isUSimPresent, getSubscriberId(" + slotId + "):" + tm.getSubscriberId(slotId));
                }
            }
        }
        if (DBG != 0) {
            logd(TAG, "isUSimPresent: " + isUsimPresent);
        }
        if (isUsimPresent) {
            return true;
        }
        return false;
    }

    private String getImsi() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm == null) {
            loge(TAG, "getImsi, TelephonyManager is null");
            return null;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        String actualSubscriberId = tm.getSubscriberId(subId);
        if (DBG) {
            logd(TAG, "getImsi, actualSubscriberId(" + subId + "): " + actualSubscriberId);
        }
        return actualSubscriberId;
    }

    public PasspointConfiguration createConfig() {
        PasspointConfiguration config = new PasspointConfiguration();
        HomeSp homeSp = createHomeSp();
        if (homeSp == null) {
            logd(TAG, "createConfig, homeSp is null");
            return null;
        }
        config.setHomeSp(homeSp);
        Credential credential = createCredential();
        if (credential == null) {
            logd(TAG, "createConfig, credential is null");
            return null;
        }
        config.setCredential(credential);
        this.mUpdateIdentifier = 1;
        config.setUpdateIdentifier(this.mUpdateIdentifier);
        return config;
    }

    public void setProviderType(int type) {
        mProviderType = type;
    }

    /* access modifiers changed from: protected */
    public void loge(String tag, String s) {
        Log.e(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logd(String tag, String s) {
        Log.d(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logv(String tag, String s) {
        Log.v(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }

    /* access modifiers changed from: protected */
    public void logi(String tag, String s) {
        Log.i(tag, s);
        LocalLog localLog = this.mLocalLog;
        localLog.log(tag + " : " + s);
    }
}
