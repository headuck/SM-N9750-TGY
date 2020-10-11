package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.os.Debug;
import android.os.SystemProperties;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.CarrierNetworkConfig;
import com.android.server.wifi.WifiNative;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.server.wifi.mobilewips.external.NetworkConstants;
import com.sec.android.app.CscFeatureTagWifi;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nonnull;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class TelephonyUtil {
    public static final String ANONYMOUS_IDENTITY = "anonymous";
    public static final int CARRIER_INVALID_TYPE = -1;
    public static final int CARRIER_MNO_TYPE = 0;
    public static final int CARRIER_MVNO_TYPE = 1;
    private static final String CONFIG_VENDOR_SSID_LIST = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGVENDORSSIDLIST);
    private static final String CSC_CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    private static final String CSC_EAP_METHOD = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_EAPMETHODSETTING);
    public static final String DEFAULT_EAP_PREFIX = "\u0000";
    private static final HashMap<Integer, String> EAP_METHOD_PREFIX = new HashMap<>();
    private static final String IMSI_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPwithSHA-256andMGF1Padding";
    private static final int KC_LEN = 8;
    public static final int SIM_SLOT_1 = 0;
    public static final int SIM_SLOT_2 = 1;
    public static final int SLOT12_BOTH_NOT_READY = 4;
    public static final int SLOT12_BOTH_READY = 3;
    public static final int SLOT1_ONLY_READY = 1;
    public static final int SLOT2_ONLY_READY = 2;
    private static final int SRES_LEN = 4;
    private static final int START_KC_POS = 4;
    private static final int START_SRES_POS = 0;
    public static final String TAG = "TelephonyUtil";
    public static final String THREE_GPP_NAI_REALM_FORMAT = "wlan.mnc%s.mcc%s.3gppnetwork.org";
    private static int mSimIndex = 1;
    private static int mSubscriptionId = 1;

    static {
        EAP_METHOD_PREFIX.put(5, "0");
        EAP_METHOD_PREFIX.put(4, "1");
        EAP_METHOD_PREFIX.put(6, "6");
    }

    public static Pair<String, String> getSimIdentity(TelephonyManager tm, TelephonyUtil telephonyUtil, WifiConfiguration config, CarrierNetworkConfig carrierNetworkConfig) {
        if (tm == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        TelephonyManager defaultDataTm = tm.createForSubscriptionId(mSubscriptionId);
        if (carrierNetworkConfig == null) {
            Log.e(TAG, "No valid CarrierNetworkConfig");
            return null;
        }
        String imsi = defaultDataTm.getSubscriberId();
        String mccMnc = "";
        if (defaultDataTm.getSimState() == 5) {
            mccMnc = defaultDataTm.getSimOperator();
        }
        String identity = buildIdentity(getSimMethodForConfig(config), imsi, mccMnc, false);
        if (identity == null) {
            Log.e(TAG, "Failed to build the identity");
            return null;
        }
        try {
            ImsiEncryptionInfo imsiEncryptionInfo = defaultDataTm.getCarrierInfoForImsiEncryption(2);
            if (imsiEncryptionInfo == null) {
                return Pair.create(identity, "");
            }
            String encryptedIdentity = buildEncryptedIdentity(telephonyUtil, identity, imsiEncryptionInfo);
            if (encryptedIdentity != null) {
                return Pair.create(identity, encryptedIdentity);
            }
            Log.e(TAG, "failed to encrypt the identity");
            return Pair.create(identity, "");
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get imsi encryption info: " + e.getMessage());
            return Pair.create(identity, "");
        }
    }

    public static String getAnonymousIdentityWith3GppRealm(@Nonnull TelephonyManager tm) {
        String mccMnc;
        if (tm == null) {
            return null;
        }
        TelephonyManager defaultDataTm = tm.createForSubscriptionId(mSubscriptionId);
        if (defaultDataTm.getSimState() != 5 || (mccMnc = defaultDataTm.getSimOperator()) == null || mccMnc.isEmpty()) {
            return null;
        }
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        if (mnc.length() == 2) {
            mnc = "0" + mnc;
        }
        return "anonymous@" + String.format(THREE_GPP_NAI_REALM_FORMAT, new Object[]{mnc, mcc});
    }

    @VisibleForTesting
    public String encryptDataUsingPublicKey(PublicKey key, byte[] data, int encodingFlag) {
        try {
            Cipher cipher = Cipher.getInstance(IMSI_CIPHER_TRANSFORMATION);
            cipher.init(1, key);
            byte[] encryptedBytes = cipher.doFinal(data);
            return Base64.encodeToString(encryptedBytes, 0, encryptedBytes.length, encodingFlag);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage());
            return null;
        }
    }

    private static String buildEncryptedIdentity(TelephonyUtil telephonyUtil, String identity, ImsiEncryptionInfo imsiEncryptionInfo) {
        if (imsiEncryptionInfo == null) {
            Log.e(TAG, "imsiEncryptionInfo is not valid");
            return null;
        } else if (identity == null) {
            Log.e(TAG, "identity is not valid");
            return null;
        } else {
            String encryptedIdentity = telephonyUtil.encryptDataUsingPublicKey(imsiEncryptionInfo.getPublicKey(), identity.getBytes(), 2);
            if (encryptedIdentity == null) {
                Log.e(TAG, "Failed to encrypt IMSI");
                return null;
            }
            String encryptedIdentity2 = DEFAULT_EAP_PREFIX + encryptedIdentity;
            if (imsiEncryptionInfo.getKeyIdentifier() == null) {
                return encryptedIdentity2;
            }
            return encryptedIdentity2 + "," + imsiEncryptionInfo.getKeyIdentifier();
        }
    }

    private static String buildIdentity(int eapMethod, String imsi, String mccMnc, boolean isEncrypted) {
        String mnc;
        String mcc;
        if (imsi == null || imsi.isEmpty()) {
            Log.e(TAG, "No IMSI or IMSI is null");
            return null;
        }
        String prefix = isEncrypted ? DEFAULT_EAP_PREFIX : EAP_METHOD_PREFIX.get(Integer.valueOf(eapMethod));
        if (prefix == null) {
            return null;
        }
        if (mccMnc == null || mccMnc.isEmpty()) {
            mcc = imsi.substring(0, 3);
            mnc = imsi.substring(3, 6);
        } else {
            mcc = mccMnc.substring(0, 3);
            mnc = mccMnc.substring(3);
            if (mnc.length() == 2) {
                mnc = "0" + mnc;
            }
        }
        return prefix + imsi + buildVendorRealm(mcc, mnc, imsi);
    }

    private static int getSimMethodForConfig(WifiConfiguration config) {
        if (config == null || config.enterpriseConfig == null) {
            return -1;
        }
        int eapMethod = config.enterpriseConfig.getEapMethod();
        if (eapMethod == 0) {
            int phase2Method = config.enterpriseConfig.getPhase2Method();
            if (phase2Method == 5) {
                eapMethod = 4;
            } else if (phase2Method == 6) {
                eapMethod = 5;
            } else if (phase2Method == 7) {
                eapMethod = 6;
            }
        }
        if (isSimEapMethod(eapMethod)) {
            return eapMethod;
        }
        return -1;
    }

    public static boolean isSimConfig(WifiConfiguration config) {
        return getSimMethodForConfig(config) != -1;
    }

    public static boolean isAnonymousAtRealmIdentity(String identity) {
        if (identity == null) {
            return false;
        }
        return identity.startsWith("anonymous@");
    }

    public static boolean isSimEapMethod(int eapMethod) {
        return eapMethod == 4 || eapMethod == 5 || eapMethod == 6;
    }

    private static int parseHex(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('a' <= ch && ch <= 'f') {
            return (ch - 'a') + 10;
        }
        if ('A' <= ch && ch <= 'F') {
            return (ch - 'A') + 10;
        }
        throw new NumberFormatException("" + ch + " is not a valid hex digit");
    }

    private static byte[] parseHex(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        if (hex.length() % 2 == 0) {
            byte[] result = new byte[((hex.length() / 2) + 1)];
            result[0] = (byte) (hex.length() / 2);
            int i = 0;
            int j = 1;
            while (i < hex.length()) {
                result[j] = (byte) (((parseHex(hex.charAt(i)) * 16) + parseHex(hex.charAt(i + 1))) & 255);
                i += 2;
                j++;
            }
            return result;
        }
        throw new NumberFormatException(hex + " is not a valid hex string");
    }

    private static byte[] parseHexWithoutLength(String hex) {
        byte[] tmpRes = parseHex(hex);
        if (tmpRes.length == 0) {
            return tmpRes;
        }
        byte[] result = new byte[(tmpRes.length - 1)];
        System.arraycopy(tmpRes, 1, result, 0, tmpRes.length - 1);
        return result;
    }

    private static String makeHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[i])}));
        }
        return sb.toString();
    }

    private static String makeHex(byte[] bytes, int from, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[from + i])}));
        }
        return sb.toString();
    }

    private static byte[] concatHex(byte[] array1, byte[] array2) {
        byte[] result = new byte[(array1.length + array2.length)];
        int index = 0;
        if (array1.length != 0) {
            int index2 = 0;
            for (byte b : array1) {
                result[index2] = b;
                index2++;
            }
            index = index2;
        }
        if (array2.length != 0) {
            for (byte b2 : array2) {
                result[index] = b2;
                index++;
            }
        }
        return result;
    }

    public static String getGsmSimAuthResponse(String[] requestData, TelephonyManager tm) {
        return getGsmAuthResponseWithLength(requestData, tm, 2);
    }

    public static String getGsmSimpleSimAuthResponse(String[] requestData, TelephonyManager tm) {
        return getGsmAuthResponseWithLength(requestData, tm, 1);
    }

    private static String getGsmAuthResponseWithLength(String[] requestData, TelephonyManager tm, int appType) {
        Object obj;
        String str;
        String[] strArr = requestData;
        TelephonyManager telephonyManager = tm;
        Object obj2 = null;
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        TelephonyManager defaultDataTm = telephonyManager.createForSubscriptionId(mSubscriptionId);
        StringBuilder sb = new StringBuilder();
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            String challenge = strArr[i2];
            if (challenge == null) {
                int i3 = appType;
                obj = obj2;
            } else if (challenge.isEmpty()) {
                int i4 = appType;
                obj = obj2;
            } else {
                Log.d(TAG, "RAND = " + challenge);
                try {
                    byte[] rand = parseHex(challenge);
                    String tmResponse = defaultDataTm.getIccAuthentication(appType, 128, Base64.encodeToString(rand, 2));
                    Log.v(TAG, "Raw Response - " + tmResponse);
                    if (tmResponse == null) {
                    } else if (tmResponse.length() <= 4) {
                        byte[] bArr = rand;
                    } else {
                        byte[] result = Base64.decode(tmResponse, i);
                        Log.v(TAG, "Hex Response -" + makeHex(result));
                        byte sresLen = result[i];
                        if (sresLen < 0) {
                            byte b = sresLen;
                            str = null;
                        } else if (sresLen >= result.length) {
                            byte[] bArr2 = rand;
                            byte b2 = sresLen;
                            str = null;
                        } else {
                            String sres = makeHex(result, 1, sresLen);
                            byte[] bArr3 = rand;
                            int kcOffset = sresLen + 1;
                            if (kcOffset >= result.length) {
                                Log.e(TAG, "malformed response - " + tmResponse);
                                return null;
                            }
                            byte kcLen = result[kcOffset];
                            if (kcLen >= 0) {
                                byte b3 = sresLen;
                                if (kcOffset + kcLen <= result.length) {
                                    String kc = makeHex(result, kcOffset + 1, kcLen);
                                    sb.append(":" + kc + ":" + sres);
                                    Log.v(TAG, "kc:" + kc + " sres:" + sres);
                                    obj = null;
                                }
                            }
                            Log.e(TAG, "malformed response - " + tmResponse);
                            return null;
                        }
                        Log.e(TAG, "malformed response - " + tmResponse);
                        return str;
                    }
                    Log.e(TAG, "bad response - " + tmResponse);
                    return null;
                } catch (NumberFormatException e) {
                    int i5 = appType;
                    obj = obj2;
                    NumberFormatException numberFormatException = e;
                    Log.e(TAG, "malformed challenge");
                }
            }
            i2++;
            i = 0;
            TelephonyManager telephonyManager2 = tm;
            obj2 = obj;
            strArr = requestData;
        }
        int i6 = appType;
        return sb.toString();
    }

    public static String getGsmSimpleSimNoLengthAuthResponse(String[] requestData, TelephonyManager tm) {
        String[] strArr = requestData;
        TelephonyManager telephonyManager = tm;
        String str = null;
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        TelephonyManager defaultDataTm = telephonyManager.createForSubscriptionId(mSubscriptionId);
        StringBuilder sb = new StringBuilder();
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            String challenge = strArr[i2];
            if (challenge != null && !challenge.isEmpty()) {
                Log.d(TAG, "RAND = " + challenge);
                try {
                    String tmResponse = defaultDataTm.getIccAuthentication(1, 128, Base64.encodeToString(parseHexWithoutLength(challenge), 2));
                    Log.v(TAG, "Raw Response - " + tmResponse);
                    if (tmResponse == null || tmResponse.length() <= 4) {
                        Log.e(TAG, "bad response - " + tmResponse);
                        return null;
                    }
                    byte[] result = Base64.decode(tmResponse, i);
                    if (12 != result.length) {
                        Log.e(TAG, "malformed response - " + tmResponse);
                        return str;
                    }
                    Log.v(TAG, "Hex Response -" + makeHex(result));
                    String sres = makeHex(result, 0, 4);
                    String kc = makeHex(result, 4, 8);
                    sb.append(":" + kc + ":" + sres);
                    Log.v(TAG, "kc:" + kc + " sres:" + sres);
                    str = null;
                } catch (NumberFormatException e) {
                    NumberFormatException numberFormatException = e;
                    Log.e(TAG, "malformed challenge");
                }
            }
            i2++;
            i = 0;
        }
        return sb.toString();
    }

    public static class SimAuthRequestData {
        public String[] data;
        public int networkId;
        public int protocol;
        public String ssid;

        public SimAuthRequestData() {
        }

        public SimAuthRequestData(int networkId2, int protocol2, String ssid2, String[] data2) {
            this.networkId = networkId2;
            this.protocol = protocol2;
            this.ssid = ssid2;
            this.data = data2;
        }
    }

    public static class SimAuthResponseData {
        public String response;
        public String type;

        public SimAuthResponseData(String type2, String response2) {
            this.type = type2;
            this.response = response2;
        }
    }

    public static SimAuthResponseData get3GAuthResponse(SimAuthRequestData requestData, TelephonyManager tm) {
        SimAuthRequestData simAuthRequestData = requestData;
        TelephonyManager telephonyManager = tm;
        StringBuilder sb = new StringBuilder();
        byte[] rand = null;
        byte[] authn = null;
        String resType = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTH;
        if (simAuthRequestData.data.length == 2) {
            try {
                rand = parseHex(simAuthRequestData.data[0]);
                authn = parseHex(simAuthRequestData.data[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "malformed challenge");
            }
        } else {
            Log.e(TAG, "malformed challenge");
        }
        String tmResponse = "";
        if (!(rand == null || authn == null)) {
            String base64Challenge = Base64.encodeToString(concatHex(rand, authn), 2);
            if (telephonyManager != null) {
                tmResponse = telephonyManager.createForSubscriptionId(mSubscriptionId).getIccAuthentication(2, NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, base64Challenge);
                Log.v(TAG, "Raw Response - " + tmResponse);
            } else {
                Log.e(TAG, "No valid TelephonyManager");
            }
        }
        boolean goodReponse = false;
        if (tmResponse == null || tmResponse.length() <= 4) {
            Log.e(TAG, "bad response - " + tmResponse);
        } else {
            byte[] result = Base64.decode(tmResponse, 0);
            Log.e(TAG, "Hex Response - " + makeHex(result));
            byte tag = result[0];
            if (tag == -37) {
                Log.v(TAG, "successful 3G authentication ");
                byte resLen = result[1];
                String res = makeHex(result, 2, resLen);
                byte ckLen = result[resLen + 2];
                String ck = makeHex(result, resLen + 3, ckLen);
                byte ikLen = result[resLen + ckLen + 3];
                String ik = makeHex(result, resLen + ckLen + 4, ikLen);
                byte b = ikLen;
                sb.append(":" + ik + ":" + ck + ":" + res);
                Log.v(TAG, "ik:" + ik + "ck:" + ck + " res:" + res);
                goodReponse = true;
            } else if (tag == -36) {
                Log.e(TAG, "synchronisation failure");
                String auts = makeHex(result, 2, result[1]);
                resType = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTS;
                sb.append(":" + auts);
                Log.v(TAG, "auts:" + auts);
                goodReponse = true;
            } else {
                Log.e(TAG, "bad response - unknown tag = " + tag);
            }
        }
        if (!goodReponse) {
            return null;
        }
        String response = sb.toString();
        Log.v(TAG, "Supplicant Response -" + response);
        return new SimAuthResponseData(resType, response);
    }

    public static int getCarrierType(TelephonyManager tm) {
        if (tm == null) {
            return -1;
        }
        TelephonyManager defaultDataTm = tm.createForSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (defaultDataTm.getSimState() != 5) {
            return -1;
        }
        if (defaultDataTm.getCarrierIdFromSimMccMnc() == defaultDataTm.getSimCarrierId()) {
            return 0;
        }
        return 1;
    }

    public static boolean isSimPresent(@Nonnull SubscriptionManager sm) {
        return sm.getActiveSubscriptionIdList().length > 0;
    }

    public static void setSimIndex(int index) {
        if (index >= 1) {
            mSimIndex = index;
            int[] mSubId = SubscriptionManager.getSubId(mSimIndex - 1);
            if (mSubId == null || mSubId.length <= 0) {
                mSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                Log.e(TAG, "mSubscriptionId is null or 0 length, so get DefaultSubId!!");
            } else {
                mSubscriptionId = mSubId[0];
            }
            Log.i(TAG, "setSimIndex() simIndex: " + mSimIndex + ", SubscriptionId : " + mSubscriptionId);
        }
    }

    private static String buildVendorRealm(String mcc, String mnc, String imsi) {
        String realm = "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
        if (imsi.startsWith("22801") && !imsi.startsWith("22801389")) {
            return "@scm-eapsim.ch";
        }
        if (imsi.startsWith("45006")) {
            return "@upluswifi.co.kr";
        }
        if (imsi.startsWith("48031")) {
            return "@wlan.mnc311.mcc480.3gppnetwork.org";
        }
        return realm;
    }

    public static boolean isSimCardReady(TelephonyManager tm) {
        if (tm == null) {
            Log.e(TAG, "TelephonyManager is null, SIM is not ready");
            return false;
        } else if (semGetMultiSimState(tm) != 4) {
            return true;
        } else {
            if (!Debug.semIsProductDev() || !"1".equals(SystemProperties.get("SimCheck.disable"))) {
                return false;
            }
            return true;
        }
    }

    public static int semGetMultiSimState(TelephonyManager tm) {
        if (tm == null) {
            Log.e(TAG, "semGetMultiSimState() TelephonyManager is null, SIM is not ready");
            return -1;
        }
        int multisimState1 = tm.getSimState(0);
        int multisimState2 = tm.getSimState(1);
        Log.i(TAG, "semGetMultiSimState() multisimState1 : " + multisimState1 + " , multisimState2 : " + multisimState2);
        if (multisimState1 == 5) {
            if (multisimState2 == 5) {
                return 3;
            }
            return 1;
        } else if (multisimState2 == 5) {
            return 2;
        } else {
            return 4;
        }
    }

    public static boolean isVendorApUsimUseable(TelephonyManager tm) {
        int subId;
        if (tm == null) {
            Log.e(TAG, "isVendorApUsimUseable: TELEPHONY_SERVICE does not work, tm is null");
            return true;
        }
        ArrayList<String> vendorMccmncLists = new ArrayList<>();
        if ("AIS".equals(CSC_CONFIG_OP_BRANDING)) {
            vendorMccmncLists.add("52000");
            vendorMccmncLists.add("52001");
            vendorMccmncLists.add("52003");
            vendorMccmncLists.add("52004");
        } else if ("SKT".equals(CSC_CONFIG_OP_BRANDING)) {
            vendorMccmncLists.add("45000");
            vendorMccmncLists.add("45005");
        } else if ("KTT".equals(CSC_CONFIG_OP_BRANDING)) {
            vendorMccmncLists.add("45008");
        } else if ("TMB".equals(CSC_CONFIG_OP_BRANDING)) {
            vendorMccmncLists.add("310260");
            vendorMccmncLists.add("310310");
        }
        Log.w(TAG, "isVendorApUsimUseable: required vendorMccmnc [" + vendorMccmncLists + "] ");
        int num = vendorMccmncLists.size();
        if (num == 0) {
            Log.i(TAG, "isVendorApUsimUseable: There is no required vendor mccmnc ");
            return true;
        }
        boolean isSimPresent = false;
        int simSlotCount = tm.getPhoneCount();
        List<String> mccMncLists = new ArrayList<>();
        for (int slotId = 0; slotId < simSlotCount; slotId++) {
            if (tm.getSimState(slotId) != 1) {
                isSimPresent = true;
                int[] mSubId = SubscriptionManager.getSubId(slotId);
                if (mSubId == null || mSubId.length <= 0) {
                    subId = SubscriptionManager.getDefaultSubscriptionId();
                    Log.e(TAG, "subID is null or 0 length, so get DefaultSubId!!");
                } else {
                    subId = mSubId[0];
                }
                if (tm.getSimState(slotId) == 5) {
                    mccMncLists.add(tm.getSimOperator(subId));
                } else {
                    Log.e(TAG, "isVendorApUsimUseable() slotId : " + slotId + " is not ready for sim ");
                }
            }
        }
        if (!isSimPresent || mccMncLists.size() <= 0) {
            Log.d(TAG, "isVendorApUsimUseable: sim state absent or no mccMnc");
            return false;
        }
        Log.w(TAG, "isVendorApUsimUseable: get mccMncLists [" + mccMncLists + "] ");
        for (int i = 0; i < num; i++) {
            if (mccMncLists.contains(vendorMccmncLists.get(i))) {
                Log.i(TAG, "isVendorApUsimUseable: sim has vendor mccmnc ");
                return true;
            }
        }
        Log.i(TAG, "isVendorApUsimUseable: sim has no vendor mccmnc ");
        return false;
    }

    public static boolean isVendorApUsimUseable(String ssid, TelephonyManager tm) {
        String imsi;
        if (tm == null) {
            Log.e(TAG, "isVendorApUsimUseable: TELEPHONY_SERVICE does not work, tm is null");
            return false;
        }
        boolean isSimPresent = false;
        int simSlotCount = tm.getPhoneCount();
        for (int slotId = 0; slotId < simSlotCount; slotId++) {
            if (tm.getSimState(slotId) != 1) {
                isSimPresent = true;
            }
        }
        if (!isSimPresent) {
            Log.d(TAG, "isVendorApUsimUseable: sim state absent");
            return false;
        } else if (!CSC_EAP_METHOD.startsWith("AKA") || (imsi = tm.getSubscriberId()) == null) {
            return true;
        } else {
            if (!CONFIG_VENDOR_SSID_LIST.contains(ssid) || !"SKT".equals(CSC_CONFIG_OP_BRANDING)) {
                if (!CONFIG_VENDOR_SSID_LIST.contains(ssid) || !"KTT".equals(CSC_CONFIG_OP_BRANDING)) {
                    if (!CONFIG_VENDOR_SSID_LIST.contains(ssid) || !"LGU".equals(CSC_CONFIG_OP_BRANDING)) {
                        if (!"UPC Wi-Free".equals(ssid) || !imsi.startsWith("20601")) {
                            return true;
                        }
                        Log.i(TAG, "ATO_USIM this mccmnc is not allowed");
                        return false;
                    } else if (imsi.startsWith("45006")) {
                        if (!imsi.startsWith("450069")) {
                            return true;
                        }
                        Log.i(TAG, "LGT_USIM But unauthenticated LGT_USIM");
                        return false;
                    }
                } else if (imsi.startsWith("45008") || imsi.startsWith("45002")) {
                    return true;
                }
            } else if (imsi.startsWith("45005") || imsi.startsWith("45000")) {
                return true;
            }
            Log.d(TAG, "USIM does not match with Subscriber. SSID:" + ssid);
            return false;
        }
    }
}
