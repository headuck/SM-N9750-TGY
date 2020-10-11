package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.HidlSupport;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.NativeUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkService;
import com.sec.android.app.CscFeatureTagWifi;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import org.json.JSONException;
import org.json.JSONObject;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork;

@ThreadSafe
public class SupplicantStaNetworkHal {
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    private static final String CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    private static final String CONFIG_SECURE_SVC_INTEGRATION = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGSECURESVCINTEGRATION);
    private static final Pattern GSM_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile(":([0-9a-fA-F]+):([0-9a-fA-F]+)");
    @VisibleForTesting
    public static final String ID_STRING_KEY_CONFIG_KEY = "configKey";
    @VisibleForTesting
    public static final String ID_STRING_KEY_CREATOR_UID = "creatorUid";
    @VisibleForTesting
    public static final String ID_STRING_KEY_FQDN = "fqdn";
    private static final String PAC_FILE_PATH = "/data/vendor/wifi/wpa/wpa_supplicant.pac";
    private static final String TAG = "SupplicantStaNetworkHal";
    private static final Pattern UMTS_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+)$");
    private static final Pattern UMTS_AUTS_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+)$");
    private int mAuthAlgMask;
    private int mAutoReconnect;
    private byte[] mBssid;
    private Context mContext;
    private String mEapAltSubjectMatch;
    private ArrayList<Byte> mEapAnonymousIdentity;
    private String mEapCACert;
    private String mEapCAPath;
    private String mEapClientCert;
    private String mEapDomainSuffixMatch;
    private boolean mEapEngine;
    private String mEapEngineID;
    private ArrayList<Byte> mEapIdentity;
    private int mEapMethod;
    private String mEapPacFile;
    private ArrayList<Byte> mEapPassword;
    private int mEapPhase1Method;
    private int mEapPhase2Method;
    private String mEapPrivateKeyId;
    private String mEapSubjectMatch;
    private int mGroupCipherMask;
    private int mGroupMgmtCipherMask;
    private ISehSupplicantStaNetwork mISehSupplicantStaNetwork;
    private ISupplicantStaNetwork mISupplicantStaNetwork;
    private ISupplicantStaNetworkCallback mISupplicantStaNetworkCallback;
    private String mIdStr;
    /* access modifiers changed from: private */
    public final String mIfaceName;
    private int mKeyMgmtMask;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private int mNetworkId;
    private int mPairwiseCipherMask;
    private int mProtoMask;
    private byte[] mPsk;
    private String mPskPassphrase;
    private boolean mRequirePmf;
    private String mSaePassword;
    private String mSaePasswordId;
    private boolean mScanSsid;
    private int mSimNumber;
    private ArrayList<Byte> mSsid;
    private boolean mSystemSupportsFastBssTransition = false;
    /* access modifiers changed from: private */
    public boolean mSystemSupportsFilsKeyMgmt = false;
    private boolean mVendorSsid;
    private boolean mVerboseLoggingEnabled = false;
    private String mWapiAsCert;
    private int mWapiCertIndex;
    private int mWapiPskType;
    private String mWapiUserCert;
    private ArrayList<Byte> mWepKey;
    private int mWepTxKeyIdx;
    /* access modifiers changed from: private */
    public final WifiMonitor mWifiMonitor;

    SupplicantStaNetworkHal(ISupplicantStaNetwork iSupplicantStaNetwork, ISehSupplicantStaNetwork iSehSupplicantStaNetwork, String ifaceName, Context context, WifiMonitor monitor) {
        this.mISupplicantStaNetwork = iSupplicantStaNetwork;
        this.mISehSupplicantStaNetwork = iSehSupplicantStaNetwork;
        this.mIfaceName = ifaceName;
        this.mContext = context;
        this.mWifiMonitor = monitor;
        this.mSystemSupportsFastBssTransition = true;
        boolean needToRegisterObserverForWifiSafeMode = false;
        if ("1".equals("2")) {
            this.mSystemSupportsFilsKeyMgmt = true;
        } else if ("2".equals("2")) {
            this.mSystemSupportsFilsKeyMgmt = true ^ getWifiSafeModeFromDb();
            needToRegisterObserverForWifiSafeMode = true;
        }
        if (needToRegisterObserverForWifiSafeMode) {
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("safe_wifi"), false, new ContentObserver((Handler) null) {
                public void onChange(boolean selfChange) {
                    if ("2".equals("2")) {
                        SupplicantStaNetworkHal supplicantStaNetworkHal = SupplicantStaNetworkHal.this;
                        boolean unused = supplicantStaNetworkHal.mSystemSupportsFilsKeyMgmt = !supplicantStaNetworkHal.getWifiSafeModeFromDb();
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public boolean getWifiSafeModeFromDb() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "safe_wifi", 0) != 0;
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:113:0x020c A[Catch:{ Exception -> 0x0069 }] */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x009f A[Catch:{ Exception -> 0x0069 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean loadWifiConfiguration(android.net.wifi.WifiConfiguration r10, java.util.Map<java.lang.String, java.lang.String> r11) {
        /*
            r9 = this;
            java.lang.Object r0 = r9.mLock
            monitor-enter(r0)
            r1 = 0
            if (r10 != 0) goto L_0x0008
            monitor-exit(r0)     // Catch:{ all -> 0x021e }
            return r1
        L_0x0008:
            r2 = 0
            r10.SSID = r2     // Catch:{ all -> 0x021e }
            boolean r3 = r9.getSsid()     // Catch:{ all -> 0x021e }
            if (r3 == 0) goto L_0x0215
            java.util.ArrayList<java.lang.Byte> r3 = r9.mSsid     // Catch:{ all -> 0x021e }
            boolean r3 = com.android.internal.util.ArrayUtils.isEmpty(r3)     // Catch:{ all -> 0x021e }
            if (r3 != 0) goto L_0x0215
            java.lang.String r3 = "gbk"
            java.lang.String r4 = CONFIG_CHARSET     // Catch:{ all -> 0x021e }
            boolean r3 = r3.equals(r4)     // Catch:{ all -> 0x021e }
            if (r3 != 0) goto L_0x0037
            java.lang.String r3 = "ksc5601"
            java.lang.String r4 = CONFIG_CHARSET     // Catch:{ all -> 0x021e }
            boolean r3 = r3.equals(r4)     // Catch:{ all -> 0x021e }
            if (r3 == 0) goto L_0x002e
            goto L_0x0037
        L_0x002e:
            java.util.ArrayList<java.lang.Byte> r3 = r9.mSsid     // Catch:{ all -> 0x021e }
            java.lang.String r3 = com.android.server.wifi.util.NativeUtil.encodeSsid(r3)     // Catch:{ all -> 0x021e }
            r10.SSID = r3     // Catch:{ all -> 0x021e }
            goto L_0x0096
        L_0x0037:
            java.util.ArrayList<java.lang.Byte> r3 = r9.mSsid     // Catch:{ all -> 0x021e }
            byte[] r3 = com.android.server.wifi.util.NativeUtil.byteArrayFromArrayList(r3)     // Catch:{ all -> 0x021e }
            r4 = 0
            int r5 = r3.length     // Catch:{ all -> 0x021e }
            long r5 = (long) r5     // Catch:{ all -> 0x021e }
            boolean r5 = com.android.server.wifi.util.NativeUtil.isUTF8String(r3, r5)     // Catch:{ all -> 0x021e }
            if (r5 != 0) goto L_0x0084
            int r5 = r3.length     // Catch:{ all -> 0x021e }
            boolean r5 = com.android.server.wifi.util.NativeUtil.isUCNVString(r3, r5)     // Catch:{ all -> 0x021e }
            if (r5 == 0) goto L_0x0084
            java.lang.String r5 = "gbk"
            java.lang.String r6 = CONFIG_CHARSET     // Catch:{ Exception -> 0x0069 }
            boolean r5 = r5.equals(r6)     // Catch:{ Exception -> 0x0069 }
            if (r5 == 0) goto L_0x0060
            java.lang.String r5 = new java.lang.String     // Catch:{ Exception -> 0x0069 }
            java.lang.String r6 = "gbk"
            r5.<init>(r3, r6)     // Catch:{ Exception -> 0x0069 }
            r4 = r5
            goto L_0x0068
        L_0x0060:
            java.lang.String r5 = new java.lang.String     // Catch:{ Exception -> 0x0069 }
            java.lang.String r6 = "ksc5601"
            r5.<init>(r3, r6)     // Catch:{ Exception -> 0x0069 }
            r4 = r5
        L_0x0068:
            goto L_0x0084
        L_0x0069:
            r5 = move-exception
            java.lang.String r6 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x021e }
            r7.<init>()     // Catch:{ all -> 0x021e }
            java.lang.String r8 = " loadWifiConfiguration, byteArray decode error  = "
            r7.append(r8)     // Catch:{ all -> 0x021e }
            java.lang.String r8 = r5.toString()     // Catch:{ all -> 0x021e }
            r7.append(r8)     // Catch:{ all -> 0x021e }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x021e }
            android.util.Log.e(r6, r7)     // Catch:{ all -> 0x021e }
        L_0x0084:
            if (r4 == 0) goto L_0x008d
            java.lang.String r5 = com.android.server.wifi.util.NativeUtil.addEnclosingQuotes(r4)     // Catch:{ all -> 0x021e }
            r10.SSID = r5     // Catch:{ all -> 0x021e }
            goto L_0x0095
        L_0x008d:
            java.util.ArrayList<java.lang.Byte> r5 = r9.mSsid     // Catch:{ all -> 0x021e }
            java.lang.String r5 = com.android.server.wifi.util.NativeUtil.encodeSsid(r5)     // Catch:{ all -> 0x021e }
            r10.SSID = r5     // Catch:{ all -> 0x021e }
        L_0x0095:
        L_0x0096:
            r3 = -1
            r10.networkId = r3     // Catch:{ all -> 0x021e }
            boolean r4 = r9.getId()     // Catch:{ all -> 0x021e }
            if (r4 == 0) goto L_0x020c
            int r4 = r9.mNetworkId     // Catch:{ all -> 0x021e }
            r10.networkId = r4     // Catch:{ all -> 0x021e }
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r4 = r10.getNetworkSelectionStatus()     // Catch:{ all -> 0x021e }
            r4.setNetworkSelectionBSSID(r2)     // Catch:{ all -> 0x021e }
            boolean r4 = r9.getBssid()     // Catch:{ all -> 0x021e }
            if (r4 == 0) goto L_0x00c5
            byte[] r4 = r9.mBssid     // Catch:{ all -> 0x021e }
            boolean r4 = com.android.internal.util.ArrayUtils.isEmpty(r4)     // Catch:{ all -> 0x021e }
            if (r4 != 0) goto L_0x00c5
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r4 = r10.getNetworkSelectionStatus()     // Catch:{ all -> 0x021e }
            byte[] r5 = r9.mBssid     // Catch:{ all -> 0x021e }
            java.lang.String r5 = com.android.server.wifi.util.NativeUtil.macAddressFromByteArray(r5)     // Catch:{ all -> 0x021e }
            r4.setNetworkSelectionBSSID(r5)     // Catch:{ all -> 0x021e }
        L_0x00c5:
            r10.hiddenSSID = r1     // Catch:{ all -> 0x021e }
            boolean r4 = r9.getScanSsid()     // Catch:{ all -> 0x021e }
            if (r4 == 0) goto L_0x00d1
            boolean r4 = r9.mScanSsid     // Catch:{ all -> 0x021e }
            r10.hiddenSSID = r4     // Catch:{ all -> 0x021e }
        L_0x00d1:
            r10.requirePMF = r1     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getRequirePmf()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x00dd
            boolean r1 = r9.mRequirePmf     // Catch:{ all -> 0x021e }
            r10.requirePMF = r1     // Catch:{ all -> 0x021e }
        L_0x00dd:
            r10.wepTxKeyIndex = r3     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getWepTxKeyIdx()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x00e9
            int r1 = r9.mWepTxKeyIdx     // Catch:{ all -> 0x021e }
            r10.wepTxKeyIndex = r1     // Catch:{ all -> 0x021e }
        L_0x00e9:
            r1 = 0
        L_0x00ea:
            r4 = 4
            if (r1 >= r4) goto L_0x010c
            java.lang.String[] r4 = r10.wepKeys     // Catch:{ all -> 0x021e }
            r4[r1] = r2     // Catch:{ all -> 0x021e }
            boolean r4 = r9.getWepKey(r1)     // Catch:{ all -> 0x021e }
            if (r4 == 0) goto L_0x0109
            java.util.ArrayList<java.lang.Byte> r4 = r9.mWepKey     // Catch:{ all -> 0x021e }
            boolean r4 = com.android.internal.util.ArrayUtils.isEmpty(r4)     // Catch:{ all -> 0x021e }
            if (r4 != 0) goto L_0x0109
            java.lang.String[] r4 = r10.wepKeys     // Catch:{ all -> 0x021e }
            java.util.ArrayList<java.lang.Byte> r5 = r9.mWepKey     // Catch:{ all -> 0x021e }
            java.lang.String r5 = com.android.server.wifi.util.NativeUtil.bytesToHexOrQuotedString(r5)     // Catch:{ all -> 0x021e }
            r4[r1] = r5     // Catch:{ all -> 0x021e }
        L_0x0109:
            int r1 = r1 + 1
            goto L_0x00ea
        L_0x010c:
            r10.preSharedKey = r2     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getPskPassphrase()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x0125
            java.lang.String r1 = r9.mPskPassphrase     // Catch:{ all -> 0x021e }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x021e }
            if (r1 != 0) goto L_0x0125
            java.lang.String r1 = r9.mPskPassphrase     // Catch:{ all -> 0x021e }
            java.lang.String r1 = com.android.server.wifi.util.NativeUtil.addEnclosingQuotes(r1)     // Catch:{ all -> 0x021e }
            r10.preSharedKey = r1     // Catch:{ all -> 0x021e }
            goto L_0x013b
        L_0x0125:
            boolean r1 = r9.getPsk()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x013b
            byte[] r1 = r9.mPsk     // Catch:{ all -> 0x021e }
            boolean r1 = com.android.internal.util.ArrayUtils.isEmpty(r1)     // Catch:{ all -> 0x021e }
            if (r1 != 0) goto L_0x013b
            byte[] r1 = r9.mPsk     // Catch:{ all -> 0x021e }
            java.lang.String r1 = com.android.server.wifi.util.NativeUtil.hexStringFromByteArray(r1)     // Catch:{ all -> 0x021e }
            r10.preSharedKey = r1     // Catch:{ all -> 0x021e }
        L_0x013b:
            boolean r1 = r9.getKeyMgmt()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x015d
            int r1 = r9.mKeyMgmtMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationKeyMgmtMask(r1)     // Catch:{ all -> 0x021e }
            java.util.BitSet r4 = r9.removeFastTransitionFlags(r1)     // Catch:{ all -> 0x021e }
            r10.allowedKeyManagement = r4     // Catch:{ all -> 0x021e }
            java.util.BitSet r4 = r10.allowedKeyManagement     // Catch:{ all -> 0x021e }
            java.util.BitSet r4 = r9.removeSha256KeyMgmtFlags(r4)     // Catch:{ all -> 0x021e }
            r10.allowedKeyManagement = r4     // Catch:{ all -> 0x021e }
            java.util.BitSet r4 = r10.allowedKeyManagement     // Catch:{ all -> 0x021e }
            java.util.BitSet r4 = r9.removeFils256KeyMgmtFlags(r4)     // Catch:{ all -> 0x021e }
            r10.allowedKeyManagement = r4     // Catch:{ all -> 0x021e }
        L_0x015d:
            boolean r1 = r9.getProto()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x016b
            int r1 = r9.mProtoMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationProtoMask(r1)     // Catch:{ all -> 0x021e }
            r10.allowedProtocols = r1     // Catch:{ all -> 0x021e }
        L_0x016b:
            boolean r1 = r9.getAuthAlg()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x0179
            int r1 = r9.mAuthAlgMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationAuthAlgMask(r1)     // Catch:{ all -> 0x021e }
            r10.allowedAuthAlgorithms = r1     // Catch:{ all -> 0x021e }
        L_0x0179:
            boolean r1 = r9.getGroupCipher()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x0187
            int r1 = r9.mGroupCipherMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationGroupCipherMask(r1)     // Catch:{ all -> 0x021e }
            r10.allowedGroupCiphers = r1     // Catch:{ all -> 0x021e }
        L_0x0187:
            boolean r1 = r9.getPairwiseCipher()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x0195
            int r1 = r9.mPairwiseCipherMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationPairwiseCipherMask(r1)     // Catch:{ all -> 0x021e }
            r10.allowedPairwiseCiphers = r1     // Catch:{ all -> 0x021e }
        L_0x0195:
            boolean r1 = r9.getGroupMgmtCipher()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x01a3
            int r1 = r9.mGroupMgmtCipherMask     // Catch:{ all -> 0x021e }
            java.util.BitSet r1 = supplicantToWifiConfigurationGroupMgmtCipherMask(r1)     // Catch:{ all -> 0x021e }
            r10.allowedGroupManagementCiphers = r1     // Catch:{ all -> 0x021e }
        L_0x01a3:
            boolean r1 = r9.getIdStr()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x01bb
            java.lang.String r1 = r9.mIdStr     // Catch:{ all -> 0x021e }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x021e }
            if (r1 != 0) goto L_0x01bb
            java.lang.String r1 = r9.mIdStr     // Catch:{ all -> 0x021e }
            java.util.Map r1 = parseNetworkExtra(r1)     // Catch:{ all -> 0x021e }
            r11.putAll(r1)     // Catch:{ all -> 0x021e }
            goto L_0x01c2
        L_0x01bb:
            java.lang.String r1 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "getIdStr failed or empty"
            android.util.Log.w(r1, r4)     // Catch:{ all -> 0x021e }
        L_0x01c2:
            r10.wapiPskType = r3     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getWapiPskType()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x01ce
            int r1 = r9.mWapiPskType     // Catch:{ all -> 0x021e }
            r10.wapiPskType = r1     // Catch:{ all -> 0x021e }
        L_0x01ce:
            r10.wapiCertIndex = r3     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getWapiCertFormat()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x01da
            int r1 = r9.mWapiCertIndex     // Catch:{ all -> 0x021e }
            r10.wapiCertIndex = r1     // Catch:{ all -> 0x021e }
        L_0x01da:
            r10.wapiAsCert = r2     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getWapiAsCert()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x01ee
            java.lang.String r1 = r9.mWapiAsCert     // Catch:{ all -> 0x021e }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x021e }
            if (r1 != 0) goto L_0x01ee
            java.lang.String r1 = r9.mWapiAsCert     // Catch:{ all -> 0x021e }
            r10.wapiAsCert = r1     // Catch:{ all -> 0x021e }
        L_0x01ee:
            r10.wapiUserCert = r2     // Catch:{ all -> 0x021e }
            boolean r1 = r9.getWapiUserCert()     // Catch:{ all -> 0x021e }
            if (r1 == 0) goto L_0x0202
            java.lang.String r1 = r9.mWapiUserCert     // Catch:{ all -> 0x021e }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x021e }
            if (r1 != 0) goto L_0x0202
            java.lang.String r1 = r9.mWapiUserCert     // Catch:{ all -> 0x021e }
            r10.wapiUserCert = r1     // Catch:{ all -> 0x021e }
        L_0x0202:
            java.lang.String r1 = r10.SSID     // Catch:{ all -> 0x021e }
            android.net.wifi.WifiEnterpriseConfig r2 = r10.enterpriseConfig     // Catch:{ all -> 0x021e }
            boolean r1 = r9.loadWifiEnterpriseConfig(r1, r2)     // Catch:{ all -> 0x021e }
            monitor-exit(r0)     // Catch:{ all -> 0x021e }
            return r1
        L_0x020c:
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.String r3 = "getId failed"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x021e }
            monitor-exit(r0)     // Catch:{ all -> 0x021e }
            return r1
        L_0x0215:
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.String r3 = "failed to read ssid"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x021e }
            monitor-exit(r0)     // Catch:{ all -> 0x021e }
            return r1
        L_0x021e:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x021e }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.loadWifiConfiguration(android.net.wifi.WifiConfiguration, java.util.Map):boolean");
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00dc A[Catch:{ IllegalArgumentException -> 0x014b }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean saveWifiConfiguration(android.net.wifi.WifiConfiguration r12) {
        /*
            r11 = this;
            java.lang.Object r0 = r11.mLock
            monitor-enter(r0)
            r1 = 0
            if (r12 != 0) goto L_0x0008
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0008:
            java.lang.String r2 = r12.SSID     // Catch:{ all -> 0x04ff }
            if (r2 == 0) goto L_0x0166
            java.lang.String r2 = "gbk"
            java.lang.String r3 = CONFIG_CHARSET     // Catch:{ IllegalArgumentException -> 0x014b }
            boolean r2 = r2.equals(r3)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r2 != 0) goto L_0x0047
            java.lang.String r2 = "ksc5601"
            java.lang.String r3 = CONFIG_CHARSET     // Catch:{ IllegalArgumentException -> 0x014b }
            boolean r2 = r2.equals(r3)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r2 == 0) goto L_0x0021
            goto L_0x0047
        L_0x0021:
            java.lang.String r2 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            java.util.ArrayList r2 = com.android.server.wifi.util.NativeUtil.decodeSsid(r2)     // Catch:{ IllegalArgumentException -> 0x014b }
            boolean r2 = r11.setSsid(r2)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r2 != 0) goto L_0x014a
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r3.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r4 = "failed to set SSID: "
            r3.append(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r4 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            r3.append(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r3 = r3.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.e(r2, r3)     // Catch:{ IllegalArgumentException -> 0x014b }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0047:
            java.util.Map r2 = com.android.server.wifi.hotspot2.NetworkDetail.getNonUTF8SsidLists()     // Catch:{ IllegalArgumentException -> 0x014b }
            r3 = 0
            if (r2 == 0) goto L_0x0101
            int r4 = r2.size()     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r4 <= 0) goto L_0x0101
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r4 = r12.getNetworkSelectionStatus()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r4 = r4.getNetworkSelectionBSSID()     // Catch:{ IllegalArgumentException -> 0x014b }
            r5 = 0
            if (r4 == 0) goto L_0x0093
            java.lang.String r6 = "any"
            boolean r6 = r6.equals(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r6 == 0) goto L_0x0068
            goto L_0x0093
        L_0x0068:
            java.lang.String r6 = r4.toUpperCase()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.Object r6 = r2.get(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            com.android.server.wifi.hotspot2.NetworkDetail$NonUTF8Ssid r6 = (com.android.server.wifi.hotspot2.NetworkDetail.NonUTF8Ssid) r6     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r6 == 0) goto L_0x00da
            java.lang.String r7 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r8.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r9 = "target bssid "
            r8.append(r9)     // Catch:{ IllegalArgumentException -> 0x014b }
            r8.append(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r9 = " is NonUTF8 SSID"
            r8.append(r9)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r8 = r8.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.d(r7, r8)     // Catch:{ IllegalArgumentException -> 0x014b }
            byte[] r7 = r6.ssidOctets     // Catch:{ IllegalArgumentException -> 0x014b }
            r5 = r7
            goto L_0x00da
        L_0x0093:
            java.lang.String r6 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r6 = r11.removeDoubleQuotes(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.util.Collection r7 = r2.values()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.util.Iterator r7 = r7.iterator()     // Catch:{ IllegalArgumentException -> 0x014b }
        L_0x00a1:
            boolean r8 = r7.hasNext()     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r8 == 0) goto L_0x00d9
            java.lang.Object r8 = r7.next()     // Catch:{ IllegalArgumentException -> 0x014b }
            com.android.server.wifi.hotspot2.NetworkDetail$NonUTF8Ssid r8 = (com.android.server.wifi.hotspot2.NetworkDetail.NonUTF8Ssid) r8     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r6 == 0) goto L_0x00d8
            java.lang.String r9 = r8.ssid     // Catch:{ IllegalArgumentException -> 0x014b }
            boolean r9 = r6.equals(r9)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r9 == 0) goto L_0x00d8
            java.lang.String r7 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r9.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r10 = "target ssid "
            r9.append(r10)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r10 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            r9.append(r10)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r10 = " is NonUTF8 SSID"
            r9.append(r10)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r9 = r9.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.d(r7, r9)     // Catch:{ IllegalArgumentException -> 0x014b }
            byte[] r7 = r8.ssidOctets     // Catch:{ IllegalArgumentException -> 0x014b }
            r5 = r7
            goto L_0x00d9
        L_0x00d8:
            goto L_0x00a1
        L_0x00d9:
        L_0x00da:
            if (r5 == 0) goto L_0x0101
            java.lang.String r6 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r7.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r8 = " chage config.SSID "
            r7.append(r8)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r8 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            r7.append(r8)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r8 = ", to NonUTF8ssid "
            r7.append(r8)     // Catch:{ IllegalArgumentException -> 0x014b }
            r7.append(r5)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r7 = r7.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.d(r6, r7)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.util.ArrayList r6 = com.android.server.wifi.util.NativeUtil.byteArrayToArrayList(r5)     // Catch:{ IllegalArgumentException -> 0x014b }
            r3 = r6
        L_0x0101:
            if (r3 == 0) goto L_0x0123
            boolean r4 = r11.setSsid(r3)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r4 != 0) goto L_0x0149
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r5.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r6 = "failed to set SSID: "
            r5.append(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r6 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            r5.append(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r5 = r5.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.e(r4, r5)     // Catch:{ IllegalArgumentException -> 0x014b }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0123:
            java.lang.String r4 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            java.util.ArrayList r4 = com.android.server.wifi.util.NativeUtil.decodeSsid(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            boolean r4 = r11.setSsid(r4)     // Catch:{ IllegalArgumentException -> 0x014b }
            if (r4 != 0) goto L_0x0149
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ IllegalArgumentException -> 0x014b }
            r5.<init>()     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r6 = "failed to set SSID: "
            r5.append(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r6 = r12.SSID     // Catch:{ IllegalArgumentException -> 0x014b }
            r5.append(r6)     // Catch:{ IllegalArgumentException -> 0x014b }
            java.lang.String r5 = r5.toString()     // Catch:{ IllegalArgumentException -> 0x014b }
            android.util.Log.e(r4, r5)     // Catch:{ IllegalArgumentException -> 0x014b }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0149:
        L_0x014a:
            goto L_0x0166
        L_0x014b:
            r2 = move-exception
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r4.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = "Illegal argument "
            r4.append(r5)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r12.SSID     // Catch:{ all -> 0x04ff }
            r4.append(r5)     // Catch:{ all -> 0x04ff }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r3, r4, r2)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0166:
            android.net.wifi.WifiConfiguration$NetworkSelectionStatus r2 = r12.getNetworkSelectionStatus()     // Catch:{ all -> 0x04ff }
            java.lang.String r2 = r2.getNetworkSelectionBSSID()     // Catch:{ all -> 0x04ff }
            if (r2 == 0) goto L_0x0192
            byte[] r3 = com.android.server.wifi.util.NativeUtil.macAddressToByteArray(r2)     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setBssid((byte[]) r3)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x0192
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = "failed to set BSSID: "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            r5.append(r2)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0192:
            java.lang.String r3 = r12.preSharedKey     // Catch:{ all -> 0x04ff }
            if (r3 == 0) goto L_0x020d
            java.lang.String r3 = r12.preSharedKey     // Catch:{ all -> 0x04ff }
            java.lang.String r4 = "\""
            boolean r3 = r3.startsWith(r4)     // Catch:{ all -> 0x04ff }
            r4 = 8
            if (r3 == 0) goto L_0x01d4
            java.util.BitSet r3 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            boolean r3 = r3.get(r4)     // Catch:{ all -> 0x04ff }
            if (r3 == 0) goto L_0x01bf
            java.lang.String r3 = r12.preSharedKey     // Catch:{ all -> 0x04ff }
            java.lang.String r3 = com.android.server.wifi.util.NativeUtil.removeEnclosingQuotes(r3)     // Catch:{ all -> 0x04ff }
            boolean r3 = r11.setSaePassword(r3)     // Catch:{ all -> 0x04ff }
            if (r3 != 0) goto L_0x020d
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "failed to set sae password"
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x01bf:
            java.lang.String r3 = r12.preSharedKey     // Catch:{ all -> 0x04ff }
            java.lang.String r3 = com.android.server.wifi.util.NativeUtil.removeEnclosingQuotes(r3)     // Catch:{ all -> 0x04ff }
            boolean r3 = r11.setPskPassphrase(r3)     // Catch:{ all -> 0x04ff }
            if (r3 != 0) goto L_0x020d
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "failed to set psk passphrase"
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x01d4:
            java.util.BitSet r3 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            boolean r3 = r3.get(r4)     // Catch:{ all -> 0x04ff }
            if (r3 == 0) goto L_0x01de
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x01de:
            java.lang.String r3 = r12.preSharedKey     // Catch:{ IllegalArgumentException -> 0x01f4 }
            byte[] r3 = com.android.server.wifi.util.NativeUtil.hexStringToByteArray(r3)     // Catch:{ IllegalArgumentException -> 0x01f4 }
            boolean r3 = r11.setPsk(r3)     // Catch:{ IllegalArgumentException -> 0x01f4 }
            if (r3 != 0) goto L_0x01f3
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "failed to set psk"
            android.util.Log.e(r3, r4)     // Catch:{ IllegalArgumentException -> 0x01f4 }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x01f3:
            goto L_0x020d
        L_0x01f4:
            r3 = move-exception
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = "saveWifiConfiguration: IllegalArgumentException "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            r5.append(r3)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x020d:
            r3 = 0
            java.lang.String[] r4 = r12.wepKeys     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x0252
            r4 = r1
        L_0x0213:
            java.lang.String[] r5 = r12.wepKeys     // Catch:{ all -> 0x04ff }
            int r5 = r5.length     // Catch:{ all -> 0x04ff }
            if (r4 >= r5) goto L_0x0252
            java.lang.String[] r5 = r12.wepKeys     // Catch:{ all -> 0x04ff }
            r5 = r5[r4]     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x024f
            java.lang.String[] r5 = r12.wepKeys     // Catch:{ all -> 0x04ff }
            r5 = r5[r4]     // Catch:{ all -> 0x04ff }
            int r5 = r5.length()     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x024f
            java.lang.String[] r5 = r12.wepKeys     // Catch:{ all -> 0x04ff }
            r5 = r5[r4]     // Catch:{ all -> 0x04ff }
            java.util.ArrayList r5 = com.android.server.wifi.util.NativeUtil.hexOrQuotedStringToBytes(r5)     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setWepKey(r4, r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x024e
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r6.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = "failed to set wep_key "
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            r6.append(r4)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x024e:
            r3 = 1
        L_0x024f:
            int r4 = r4 + 1
            goto L_0x0213
        L_0x0252:
            if (r3 == 0) goto L_0x0276
            int r4 = r12.wepTxKeyIndex     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setWepTxKeyIdx(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x0276
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = "failed to set wep_tx_keyidx: "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            int r6 = r12.wepTxKeyIndex     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0276:
            boolean r4 = r12.hiddenSSID     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setScanSsid(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x029d
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r12.SSID     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = ": failed to set hiddenSSID: "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            boolean r6 = r12.hiddenSSID     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x029d:
            boolean r4 = r12.semIsVendorSpecificSsid     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setVendorSsid(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x02c4
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r12.SSID     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = ": failed to set hiddenSSID: "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            boolean r6 = r12.semIsVendorSpecificSsid     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x02c4:
            boolean r4 = r12.requirePMF     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setRequirePmf(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x02eb
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r5.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r12.SSID     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = ": failed to set requirePMF: "
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            boolean r6 = r12.requirePMF     // Catch:{ all -> 0x04ff }
            r5.append(r6)     // Catch:{ all -> 0x04ff }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x02eb:
            java.util.BitSet r4 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            int r4 = r4.cardinality()     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x032d
            java.util.BitSet r4 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            java.util.BitSet r4 = r11.addFastTransitionFlags(r4)     // Catch:{ all -> 0x04ff }
            java.util.BitSet r5 = r11.addSha256KeyMgmtFlags(r4)     // Catch:{ all -> 0x04ff }
            r4 = r5
            java.util.BitSet r5 = r11.addFils256KeyMgmtFlags(r4)     // Catch:{ all -> 0x04ff }
            r4 = r5
            int r5 = wifiConfigurationToSupplicantKeyMgmtMask(r4)     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setKeyMgmt(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x0316
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.String r6 = "failed to set Key Management"
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0316:
            r5 = 10
            boolean r5 = r4.get(r5)     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x032d
            boolean r5 = r11.saveSuiteBConfig(r12)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x032d
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.String r6 = "Failed to set Suite-B-192 configuration"
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x032d:
            java.util.BitSet r4 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            r5 = 23
            boolean r4 = r4.get(r5)     // Catch:{ all -> 0x04ff }
            r5 = 3
            if (r4 != 0) goto L_0x0342
            java.util.BitSet r4 = r12.allowedKeyManagement     // Catch:{ all -> 0x04ff }
            r6 = 22
            boolean r4 = r4.get(r6)     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x034f
        L_0x0342:
            java.util.BitSet r4 = r12.allowedProtocols     // Catch:{ all -> 0x04ff }
            boolean r4 = r4.get(r5)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x034f
            java.util.BitSet r4 = r12.allowedProtocols     // Catch:{ all -> 0x04ff }
            r4.set(r5)     // Catch:{ all -> 0x04ff }
        L_0x034f:
            java.util.BitSet r4 = r12.allowedProtocols     // Catch:{ all -> 0x04ff }
            int r4 = r4.cardinality()     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x036c
            java.util.BitSet r4 = r12.allowedProtocols     // Catch:{ all -> 0x04ff }
            int r4 = wifiConfigurationToSupplicantProtoMask(r4)     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setProto(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x036c
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.String r5 = "failed to set Security Protocol"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x036c:
            java.util.BitSet r4 = r12.allowedAuthAlgorithms     // Catch:{ all -> 0x04ff }
            int r4 = r4.cardinality()     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x038f
            boolean r4 = r11.isAuthAlgNeeded(r12)     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x038f
            java.util.BitSet r4 = r12.allowedAuthAlgorithms     // Catch:{ all -> 0x04ff }
            int r4 = wifiConfigurationToSupplicantAuthAlgMask(r4)     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setAuthAlg(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x038f
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.String r5 = "failed to set AuthAlgorithm"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x038f:
            java.util.BitSet r4 = r12.allowedGroupCiphers     // Catch:{ all -> 0x04ff }
            int r4 = r4.cardinality()     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x03ac
            java.util.BitSet r4 = r12.allowedGroupCiphers     // Catch:{ all -> 0x04ff }
            int r4 = wifiConfigurationToSupplicantGroupCipherMask(r4)     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setGroupCipher(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x03ac
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.String r5 = "failed to set Group Cipher"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x03ac:
            java.util.BitSet r4 = r12.allowedPairwiseCiphers     // Catch:{ all -> 0x04ff }
            int r4 = r4.cardinality()     // Catch:{ all -> 0x04ff }
            if (r4 == 0) goto L_0x03c9
            java.util.BitSet r4 = r12.allowedPairwiseCiphers     // Catch:{ all -> 0x04ff }
            int r4 = wifiConfigurationToSupplicantPairwiseCipherMask(r4)     // Catch:{ all -> 0x04ff }
            boolean r4 = r11.setPairwiseCipher(r4)     // Catch:{ all -> 0x04ff }
            if (r4 != 0) goto L_0x03c9
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.String r5 = "failed to set PairwiseCipher"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x03c9:
            java.util.HashMap r4 = new java.util.HashMap     // Catch:{ all -> 0x04ff }
            r4.<init>()     // Catch:{ all -> 0x04ff }
            boolean r6 = r12.isPasspoint()     // Catch:{ all -> 0x04ff }
            if (r6 == 0) goto L_0x03db
            java.lang.String r6 = "fqdn"
            java.lang.String r7 = r12.FQDN     // Catch:{ all -> 0x04ff }
            r4.put(r6, r7)     // Catch:{ all -> 0x04ff }
        L_0x03db:
            java.lang.String r6 = "configKey"
            java.lang.String r7 = r12.configKey()     // Catch:{ all -> 0x04ff }
            r4.put(r6, r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = "creatorUid"
            int r7 = r12.creatorUid     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = java.lang.Integer.toString(r7)     // Catch:{ all -> 0x04ff }
            r4.put(r6, r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = createNetworkExtra(r4)     // Catch:{ all -> 0x04ff }
            boolean r6 = r11.setIdStr(r6)     // Catch:{ all -> 0x04ff }
            if (r6 != 0) goto L_0x0402
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.String r6 = "failed to set id string"
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0402:
            java.lang.String r6 = r12.updateIdentifier     // Catch:{ all -> 0x04ff }
            if (r6 == 0) goto L_0x041b
            java.lang.String r6 = r12.updateIdentifier     // Catch:{ all -> 0x04ff }
            int r6 = java.lang.Integer.parseInt(r6)     // Catch:{ all -> 0x04ff }
            boolean r6 = r11.setUpdateIdentifier(r6)     // Catch:{ all -> 0x04ff }
            if (r6 != 0) goto L_0x041b
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.String r6 = "failed to set update identifier"
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x041b:
            java.util.BitSet r6 = r12.allowedProtocols     // Catch:{ all -> 0x04ff }
            boolean r5 = r6.get(r5)     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x04c7
            int r5 = r12.wapiPskType     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setWapiPskType(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x044a
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r6.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.SSID     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = ": failed to set wapiPskType: "
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            int r7 = r12.wapiPskType     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x044a:
            int r5 = r12.wapiCertIndex     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setWapiCertFormat(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x0471
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r6.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.SSID     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = ": failed to set wapiCertIndex: "
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            int r7 = r12.wapiCertIndex     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x0471:
            java.lang.String r5 = r12.wapiAsCert     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x049c
            java.lang.String r5 = r12.wapiAsCert     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setWapiAsCert(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x049c
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r6.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.SSID     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = ": failed to set wapiAsCert: "
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.wapiAsCert     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x049c:
            java.lang.String r5 = r12.wapiUserCert     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x04c7
            java.lang.String r5 = r12.wapiUserCert     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.setWapiUserCert(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x04c7
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x04ff }
            r6.<init>()     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.SSID     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = ": failed to set wapiUserCert: "
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.wapiUserCert     // Catch:{ all -> 0x04ff }
            r6.append(r7)     // Catch:{ all -> 0x04ff }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x04ff }
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x04c7:
            android.net.wifi.WifiEnterpriseConfig r5 = r12.enterpriseConfig     // Catch:{ all -> 0x04ff }
            if (r5 == 0) goto L_0x04e0
            android.net.wifi.WifiEnterpriseConfig r5 = r12.enterpriseConfig     // Catch:{ all -> 0x04ff }
            int r5 = r5.getEapMethod()     // Catch:{ all -> 0x04ff }
            r6 = -1
            if (r5 == r6) goto L_0x04e0
            java.lang.String r5 = r12.SSID     // Catch:{ all -> 0x04ff }
            android.net.wifi.WifiEnterpriseConfig r6 = r12.enterpriseConfig     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.saveWifiEnterpriseConfig(r5, r6)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x04e0
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x04e0:
            com.android.server.wifi.SupplicantStaNetworkHal$SupplicantStaNetworkHalCallback r5 = new com.android.server.wifi.SupplicantStaNetworkHal$SupplicantStaNetworkHalCallback     // Catch:{ all -> 0x04ff }
            int r6 = r12.networkId     // Catch:{ all -> 0x04ff }
            java.lang.String r7 = r12.SSID     // Catch:{ all -> 0x04ff }
            r5.<init>(r6, r7)     // Catch:{ all -> 0x04ff }
            r11.mISupplicantStaNetworkCallback = r5     // Catch:{ all -> 0x04ff }
            android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback r5 = r11.mISupplicantStaNetworkCallback     // Catch:{ all -> 0x04ff }
            boolean r5 = r11.registerCallback(r5)     // Catch:{ all -> 0x04ff }
            if (r5 != 0) goto L_0x04fc
            java.lang.String r5 = "SupplicantStaNetworkHal"
            java.lang.String r6 = "Failed to register callback"
            android.util.Log.e(r5, r6)     // Catch:{ all -> 0x04ff }
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            return r1
        L_0x04fc:
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            r0 = 1
            return r0
        L_0x04ff:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x04ff }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.saveWifiConfiguration(android.net.wifi.WifiConfiguration):boolean");
    }

    private boolean isAuthAlgNeeded(WifiConfiguration config) {
        if (!config.allowedKeyManagement.get(8)) {
            return true;
        }
        if (!this.mVerboseLoggingEnabled) {
            return false;
        }
        Log.d(TAG, "No need to set Auth Algorithm for SAE");
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0148, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean loadWifiEnterpriseConfig(java.lang.String r5, android.net.wifi.WifiEnterpriseConfig r6) {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mLock
            monitor-enter(r0)
            r1 = 0
            if (r6 != 0) goto L_0x0008
            monitor-exit(r0)     // Catch:{ all -> 0x015b }
            return r1
        L_0x0008:
            boolean r2 = r4.getEapMethod()     // Catch:{ all -> 0x015b }
            r3 = 1
            if (r2 == 0) goto L_0x0152
            int r2 = r4.mEapMethod     // Catch:{ all -> 0x015b }
            int r2 = supplicantToWifiConfigurationEapMethod(r2)     // Catch:{ all -> 0x015b }
            r6.setEapMethod(r2)     // Catch:{ all -> 0x015b }
            boolean r2 = r4.getEapPhase2Method()     // Catch:{ all -> 0x015b }
            if (r2 == 0) goto L_0x0149
            int r1 = r4.mEapPhase2Method     // Catch:{ all -> 0x015b }
            int r1 = supplicantToWifiConfigurationEapPhase2Method(r1)     // Catch:{ all -> 0x015b }
            r6.setPhase2Method(r1)     // Catch:{ all -> 0x015b }
            boolean r1 = r4.getEapIdentity()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0040
            java.util.ArrayList<java.lang.Byte> r1 = r4.mEapIdentity     // Catch:{ all -> 0x015b }
            boolean r1 = com.android.internal.util.ArrayUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0040
            java.lang.String r1 = "identity"
            java.util.ArrayList<java.lang.Byte> r2 = r4.mEapIdentity     // Catch:{ all -> 0x015b }
            java.lang.String r2 = com.android.server.wifi.util.NativeUtil.stringFromByteArrayList(r2)     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0040:
            boolean r1 = r4.getEapAnonymousIdentity()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0059
            java.util.ArrayList<java.lang.Byte> r1 = r4.mEapAnonymousIdentity     // Catch:{ all -> 0x015b }
            boolean r1 = com.android.internal.util.ArrayUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0059
            java.lang.String r1 = "anonymous_identity"
            java.util.ArrayList<java.lang.Byte> r2 = r4.mEapAnonymousIdentity     // Catch:{ all -> 0x015b }
            java.lang.String r2 = com.android.server.wifi.util.NativeUtil.stringFromByteArrayList(r2)     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0059:
            boolean r1 = r4.getEapPassword()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0072
            java.util.ArrayList<java.lang.Byte> r1 = r4.mEapPassword     // Catch:{ all -> 0x015b }
            boolean r1 = com.android.internal.util.ArrayUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0072
            java.lang.String r1 = "password"
            java.util.ArrayList<java.lang.Byte> r2 = r4.mEapPassword     // Catch:{ all -> 0x015b }
            java.lang.String r2 = com.android.server.wifi.util.NativeUtil.stringFromByteArrayList(r2)     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0072:
            boolean r1 = r4.getEapClientCert()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0087
            java.lang.String r1 = r4.mEapClientCert     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0087
            java.lang.String r1 = "client_cert"
            java.lang.String r2 = r4.mEapClientCert     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0087:
            boolean r1 = r4.getEapCACert()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x009c
            java.lang.String r1 = r4.mEapCACert     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x009c
            java.lang.String r1 = "ca_cert"
            java.lang.String r2 = r4.mEapCACert     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x009c:
            boolean r1 = r4.getEapSubjectMatch()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x00b1
            java.lang.String r1 = r4.mEapSubjectMatch     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x00b1
            java.lang.String r1 = "subject_match"
            java.lang.String r2 = r4.mEapSubjectMatch     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x00b1:
            boolean r1 = r4.getEapEngineID()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x00c6
            java.lang.String r1 = r4.mEapEngineID     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x00c6
            java.lang.String r1 = "engine_id"
            java.lang.String r2 = r4.mEapEngineID     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x00c6:
            boolean r1 = r4.getEapEngine()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x00e2
            java.lang.String r1 = r4.mEapEngineID     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x00e2
            java.lang.String r1 = "engine"
            boolean r2 = r4.mEapEngine     // Catch:{ all -> 0x015b }
            if (r2 == 0) goto L_0x00dd
            java.lang.String r2 = "1"
            goto L_0x00df
        L_0x00dd:
            java.lang.String r2 = "0"
        L_0x00df:
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x00e2:
            boolean r1 = r4.getEapPrivateKeyId()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x00f7
            java.lang.String r1 = r4.mEapPrivateKeyId     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x00f7
            java.lang.String r1 = "key_id"
            java.lang.String r2 = r4.mEapPrivateKeyId     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x00f7:
            boolean r1 = r4.getEapAltSubjectMatch()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x010c
            java.lang.String r1 = r4.mEapAltSubjectMatch     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x010c
            java.lang.String r1 = "altsubject_match"
            java.lang.String r2 = r4.mEapAltSubjectMatch     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x010c:
            boolean r1 = r4.getEapDomainSuffixMatch()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0121
            java.lang.String r1 = r4.mEapDomainSuffixMatch     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0121
            java.lang.String r1 = "domain_suffix_match"
            java.lang.String r2 = r4.mEapDomainSuffixMatch     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0121:
            boolean r1 = r4.getEapCAPath()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0136
            java.lang.String r1 = r4.mEapCAPath     // Catch:{ all -> 0x015b }
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch:{ all -> 0x015b }
            if (r1 != 0) goto L_0x0136
            java.lang.String r1 = "ca_path"
            java.lang.String r2 = r4.mEapCAPath     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0136:
            boolean r1 = r4.getSimIndex()     // Catch:{ all -> 0x015b }
            if (r1 == 0) goto L_0x0147
            java.lang.String r1 = "sim_num"
            int r2 = r4.mSimNumber     // Catch:{ all -> 0x015b }
            java.lang.String r2 = java.lang.Integer.toString(r2)     // Catch:{ all -> 0x015b }
            r6.setFieldValue(r1, r2)     // Catch:{ all -> 0x015b }
        L_0x0147:
            monitor-exit(r0)     // Catch:{ all -> 0x015b }
            return r3
        L_0x0149:
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.String r3 = "failed to get eap phase2 method"
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x015b }
            monitor-exit(r0)     // Catch:{ all -> 0x015b }
            return r1
        L_0x0152:
            java.lang.String r1 = "SupplicantStaNetworkHal"
            java.lang.String r2 = "failed to get eap method. Assumimg not an enterprise network"
            android.util.Log.e(r1, r2)     // Catch:{ all -> 0x015b }
            monitor-exit(r0)     // Catch:{ all -> 0x015b }
            return r3
        L_0x015b:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x015b }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.loadWifiEnterpriseConfig(java.lang.String, android.net.wifi.WifiEnterpriseConfig):boolean");
    }

    private boolean saveSuiteBConfig(WifiConfiguration config) {
        if (config.allowedGroupCiphers.cardinality() != 0 && !setGroupCipher(wifiConfigurationToSupplicantGroupCipherMask(config.allowedGroupCiphers))) {
            Log.e(TAG, "failed to set Group Cipher");
            return false;
        } else if (config.allowedPairwiseCiphers.cardinality() != 0 && !setPairwiseCipher(wifiConfigurationToSupplicantPairwiseCipherMask(config.allowedPairwiseCiphers))) {
            Log.e(TAG, "failed to set PairwiseCipher");
            return false;
        } else if (config.allowedGroupManagementCiphers.cardinality() == 0 || setGroupMgmtCipher(wifiConfigurationToSupplicantGroupMgmtCipherMask(config.allowedGroupManagementCiphers))) {
            if (config.allowedSuiteBCiphers.get(1)) {
                if (!enableTlsSuiteBEapPhase1Param(true)) {
                    Log.e(TAG, "failed to set TLSSuiteB");
                    return false;
                }
            } else if (config.allowedSuiteBCiphers.get(0) && !enableSuiteBEapOpenSslCiphers()) {
                Log.e(TAG, "failed to set OpensslCipher");
                return false;
            }
            return true;
        } else {
            Log.e(TAG, "failed to set GroupMgmtCipher");
            return false;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:124:0x030a, code lost:
        return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean saveWifiEnterpriseConfig(java.lang.String r7, android.net.wifi.WifiEnterpriseConfig r8) {
        /*
            r6 = this;
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            r1 = 0
            if (r8 != 0) goto L_0x0008
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0008:
            int r2 = r8.getEapMethod()     // Catch:{ all -> 0x030b }
            int r2 = wifiConfigurationToSupplicantEapMethod(r2)     // Catch:{ all -> 0x030b }
            boolean r2 = r6.setEapMethod(r2)     // Catch:{ all -> 0x030b }
            if (r2 != 0) goto L_0x0035
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r3.<init>()     // Catch:{ all -> 0x030b }
            r3.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = ": failed to set eap method: "
            r3.append(r4)     // Catch:{ all -> 0x030b }
            int r4 = r8.getEapMethod()     // Catch:{ all -> 0x030b }
            r3.append(r4)     // Catch:{ all -> 0x030b }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0035:
            int r2 = r8.getPhase2Method()     // Catch:{ all -> 0x030b }
            int r2 = wifiConfigurationToSupplicantEapPhase2Method(r2)     // Catch:{ all -> 0x030b }
            boolean r2 = r6.setEapPhase2Method(r2)     // Catch:{ all -> 0x030b }
            if (r2 != 0) goto L_0x0063
            java.lang.String r2 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r3.<init>()     // Catch:{ all -> 0x030b }
            r3.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = ": failed to set eap phase 2 method: "
            r3.append(r4)     // Catch:{ all -> 0x030b }
            int r4 = r8.getPhase2Method()     // Catch:{ all -> 0x030b }
            r3.append(r4)     // Catch:{ all -> 0x030b }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r2, r3)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0063:
            r2 = 0
            java.lang.String r3 = "identity"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0096
            java.util.ArrayList r3 = com.android.server.wifi.util.NativeUtil.stringToByteArrayList(r2)     // Catch:{ all -> 0x030b }
            boolean r3 = r6.setEapIdentity(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0096
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap identity: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0096:
            java.lang.String r3 = "anonymous_identity"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x00c8
            java.util.ArrayList r3 = com.android.server.wifi.util.NativeUtil.stringToByteArrayList(r2)     // Catch:{ all -> 0x030b }
            boolean r3 = r6.setEapAnonymousIdentity(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x00c8
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap anonymous identity: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x00c8:
            java.lang.String r3 = "password"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x00f7
            java.util.ArrayList r3 = com.android.server.wifi.util.NativeUtil.stringToByteArrayList(r2)     // Catch:{ all -> 0x030b }
            boolean r3 = r6.setEapPassword(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x00f7
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap password"
            r4.append(r5)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x00f7:
            java.lang.String r3 = "client_cert"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0125
            boolean r3 = r6.setEapClientCert(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0125
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap client cert: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0125:
            java.lang.String r3 = "ca_cert"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0153
            boolean r3 = r6.setEapCACert(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0153
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap ca cert: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0153:
            java.lang.String r3 = "subject_match"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0181
            boolean r3 = r6.setEapSubjectMatch(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0181
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap subject match: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0181:
            java.lang.String r3 = "engine_id"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x01af
            boolean r3 = r6.setEapEngineID(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x01af
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap engine id: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x01af:
            java.lang.String r3 = "engine"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            r4 = 1
            if (r3 != 0) goto L_0x01e9
            java.lang.String r3 = "1"
            boolean r3 = r2.equals(r3)     // Catch:{ all -> 0x030b }
            if (r3 == 0) goto L_0x01c7
            r3 = r4
            goto L_0x01c8
        L_0x01c7:
            r3 = r1
        L_0x01c8:
            boolean r3 = r6.setEapEngine(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x01e9
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap engine: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x01e9:
            java.lang.String r3 = "key_id"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0217
            boolean r3 = r6.setEapPrivateKeyId(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0217
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap private key: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0217:
            java.lang.String r3 = "altsubject_match"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0245
            boolean r3 = r6.setEapAltSubjectMatch(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0245
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap alt subject match: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0245:
            java.lang.String r3 = "domain_suffix_match"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0273
            boolean r3 = r6.setEapDomainSuffixMatch(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0273
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap domain suffix match: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0273:
            java.lang.String r3 = "ca_path"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x02a1
            boolean r3 = r6.setEapCAPath(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x02a1
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set eap ca path: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x02a1:
            java.lang.String r3 = "proactive_key_caching"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x02da
            java.lang.String r3 = "1"
            boolean r3 = r2.equals(r3)     // Catch:{ all -> 0x030b }
            if (r3 == 0) goto L_0x02b8
            r3 = r4
            goto L_0x02b9
        L_0x02b8:
            r3 = r1
        L_0x02b9:
            boolean r3 = r6.setEapProactiveKeyCaching(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x02da
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            r4.append(r7)     // Catch:{ all -> 0x030b }
            java.lang.String r5 = ": failed to set proactive key caching: "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x02da:
            java.lang.String r3 = "sim_num"
            java.lang.String r3 = r8.getFieldValue(r3)     // Catch:{ all -> 0x030b }
            r2 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0309
            int r3 = java.lang.Integer.parseInt(r2)     // Catch:{ all -> 0x030b }
            boolean r3 = r6.setSimIndex(r3)     // Catch:{ all -> 0x030b }
            if (r3 != 0) goto L_0x0309
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x030b }
            r4.<init>()     // Catch:{ all -> 0x030b }
            java.lang.String r5 = "failed to set simnum value "
            r4.append(r5)     // Catch:{ all -> 0x030b }
            r4.append(r2)     // Catch:{ all -> 0x030b }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x030b }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x030b }
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r1
        L_0x0309:
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            return r4
        L_0x030b:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x030b }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.saveWifiEnterpriseConfig(java.lang.String, android.net.wifi.WifiEnterpriseConfig):boolean");
    }

    private android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork getV1_2StaNetwork() {
        android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork supplicantStaNetworkForV1_2Mockable;
        synchronized (this.mLock) {
            supplicantStaNetworkForV1_2Mockable = getSupplicantStaNetworkForV1_2Mockable();
        }
        return supplicantStaNetworkForV1_2Mockable;
    }

    private static int wifiConfigurationToSupplicantKeyMgmtMask(BitSet keyMgmt) {
        int mask = 0;
        int bit = keyMgmt.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 4;
            } else if (bit == 1) {
                mask |= 2;
            } else if (bit == 2) {
                mask |= 1;
            } else if (bit == 3) {
                mask |= 8;
            } else if (bit == 20) {
                mask |= 262144;
            } else if (bit == 22) {
                mask |= 4096;
            } else if (bit != 23) {
                switch (bit) {
                    case 5:
                        mask |= 32768;
                        break;
                    case 6:
                        mask |= 64;
                        break;
                    case 7:
                        mask |= 32;
                        break;
                    case 8:
                        mask |= 1024;
                        break;
                    case 9:
                        mask |= 4194304;
                        break;
                    case 10:
                        mask |= ISupplicantStaNetwork.KeyMgmtMask.SUITE_B_192;
                        break;
                    case 11:
                        mask |= 256;
                        break;
                    case 12:
                        mask |= 128;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid protoMask bit in keyMgmt: " + bit);
                }
            } else {
                mask |= 8192;
            }
            bit = keyMgmt.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantProtoMask(BitSet protoMask) {
        int mask = 0;
        int bit = protoMask.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 1;
            } else if (bit == 1) {
                mask |= 2;
            } else if (bit == 2) {
                mask |= 8;
            } else if (bit == 3) {
                mask |= 4;
            } else {
                throw new IllegalArgumentException("Invalid protoMask bit in wificonfig: " + bit);
            }
            bit = protoMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantAuthAlgMask(BitSet authAlgMask) {
        int mask = 0;
        int bit = authAlgMask.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 1;
            } else if (bit == 1) {
                mask |= 2;
            } else if (bit == 2) {
                mask |= 4;
            } else {
                throw new IllegalArgumentException("Invalid authAlgMask bit in wificonfig: " + bit);
            }
            bit = authAlgMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantGroupCipherMask(BitSet groupCipherMask) {
        int mask = 0;
        int bit = groupCipherMask.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 2;
            } else if (bit == 1) {
                mask |= 4;
            } else if (bit == 2) {
                mask |= 8;
            } else if (bit == 3) {
                mask |= 16;
            } else if (bit == 4) {
                mask |= 16384;
            } else if (bit == 5) {
                mask |= 256;
            } else {
                throw new IllegalArgumentException("Invalid GroupCipherMask bit in wificonfig: " + bit);
            }
            bit = groupCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantGroupMgmtCipherMask(BitSet groupMgmtCipherMask) {
        int mask = 0;
        int bit = groupMgmtCipherMask.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 8192;
            } else if (bit == 1) {
                mask |= 2048;
            } else if (bit == 2) {
                mask |= 4096;
            } else {
                throw new IllegalArgumentException("Invalid GroupMgmtCipherMask bit in wificonfig: " + bit);
            }
            bit = groupMgmtCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantPairwiseCipherMask(BitSet pairwiseCipherMask) {
        int mask = 0;
        int bit = pairwiseCipherMask.nextSetBit(0);
        while (bit != -1) {
            if (bit == 0) {
                mask |= 1;
            } else if (bit == 1) {
                mask |= 8;
            } else if (bit == 2) {
                mask |= 16;
            } else if (bit == 3) {
                mask |= 256;
            } else {
                throw new IllegalArgumentException("Invalid pairwiseCipherMask bit in wificonfig: " + bit);
            }
            bit = pairwiseCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int supplicantToWifiConfigurationEapMethod(int value) {
        if (value == 18) {
            return 18;
        }
        if (value == 19) {
            return 19;
        }
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from supplicant: " + value);
                return -1;
        }
    }

    private static int supplicantToWifiConfigurationEapPhase1Method(int value) {
        if (value == 0) {
            return 0;
        }
        if (value == 1) {
            return 1;
        }
        if (value == 2) {
            return 2;
        }
        if (value == 3) {
            return 3;
        }
        Log.e(TAG, "invalid eap phase1 method value from supplicant: " + value);
        return -1;
    }

    private static int supplicantToWifiConfigurationEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from supplicant: " + value);
                return -1;
        }
    }

    private static int supplicantMaskValueToWifiConfigurationBitSet(int supplicantMask, int supplicantValue, BitSet bitset, int bitSetPosition) {
        bitset.set(bitSetPosition, (supplicantMask & supplicantValue) == supplicantValue);
        return (~supplicantValue) & supplicantMask;
    }

    private static BitSet supplicantToWifiConfigurationKeyMgmtMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 4, bitset, 0), 2, bitset, 1), 1, bitset, 2), 8, bitset, 3), 32768, bitset, 5), 64, bitset, 6), 32, bitset, 7), 1024, bitset, 8), 4194304, bitset, 9), ISupplicantStaNetwork.KeyMgmtMask.SUITE_B_192, bitset, 10), 256, bitset, 11), 128, bitset, 12), 262144, bitset, 20), 4096, bitset, 22), 8192, bitset, 23);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid key mgmt mask from supplicant: " + mask2);
    }

    private static BitSet supplicantToWifiConfigurationProtoMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 8, bitset, 2), 4, bitset, 3);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid proto mask from supplicant: " + mask2);
    }

    private static BitSet supplicantToWifiConfigurationAuthAlgMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 4, bitset, 2);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid auth alg mask from supplicant: " + mask2);
    }

    private static BitSet supplicantToWifiConfigurationGroupCipherMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 2, bitset, 0), 4, bitset, 1), 8, bitset, 2), 16, bitset, 3), 256, bitset, 5), 16384, bitset, 4);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid group cipher mask from supplicant: " + mask2);
    }

    private static BitSet supplicantToWifiConfigurationGroupMgmtCipherMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 2048, bitset, 1), 4096, bitset, 2), 8192, bitset, 0);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid group mgmt cipher mask from supplicant: " + mask2);
    }

    private static BitSet supplicantToWifiConfigurationPairwiseCipherMask(int mask) {
        BitSet bitset = new BitSet();
        int mask2 = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 8, bitset, 1), 16, bitset, 2), 256, bitset, 3);
        if (mask2 == 0) {
            return bitset;
        }
        throw new IllegalArgumentException("invalid pairwise cipher mask from supplicant: " + mask2);
    }

    private static int wifiConfigurationToSupplicantEapMethod(int value) {
        if (value == 18) {
            return 18;
        }
        if (value == 19) {
            return 19;
        }
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap method value from WifiConfiguration: " + value);
                return -1;
        }
    }

    private static int wifiConfigurationToSupplicantEapPhase1Method(String phase1str) {
        if (phase1str == null || TextUtils.isEmpty(phase1str) || phase1str.equals("NULL")) {
            Log.e(TAG, "eap phase1 method value from WifiConfiguration: NONE");
            return -1;
        } else if (phase1str.length() != 19 || !phase1str.contains("fast_provisioning=")) {
            Log.e(TAG, "invalid eap phase1 method value from WifiConfiguration: phase1str: " + phase1str);
            return -1;
        } else {
            int value = Integer.parseInt(phase1str.replaceAll("fast_provisioning=", ""));
            if (value == 0) {
                return 0;
            }
            if (value == 1) {
                return 1;
            }
            if (value == 2) {
                return 2;
            }
            if (value == 3) {
                return 3;
            }
            Log.e(TAG, "invalid eap phase1 method value from WifiConfiguration: " + value);
            return -1;
        }
    }

    private static int wifiConfigurationToSupplicantEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                Log.e(TAG, "invalid eap phase2 method value from WifiConfiguration: " + value);
                return -1;
        }
    }

    private boolean getId() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getId")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getId(new ISupplicantNetwork.getIdCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getId$0$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getId");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getId$0$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mNetworkId = idValue;
        } else {
            checkStatusAndLogFailure(status, "getId");
        }
    }

    private boolean registerCallback(ISupplicantStaNetworkCallback callback) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("registerCallback")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.registerCallback(callback), "registerCallback");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean setSsid(ArrayList<Byte> ssid) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setSsid")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setSsid(ssid), "setSsid");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setSsid");
                return false;
            }
        }
    }

    public boolean setBssid(String bssidStr) {
        boolean bssid;
        synchronized (this.mLock) {
            try {
                bssid = setBssid(NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + bssidStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return bssid;
    }

    private boolean setBssid(byte[] bssid) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setBssid")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setBssid(bssid), "setBssid");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setBssid");
                return false;
            }
        }
    }

    private boolean setScanSsid(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setScanSsid")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setScanSsid(enable), "setScanSsid");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setScanSsid");
                return false;
            }
        }
    }

    private boolean setKeyMgmt(int keyMgmtMask) {
        SupplicantStatus status;
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setKeyMgmt")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 != null) {
                    status = iSupplicantStaNetworkV12.setKeyMgmt_1_2(keyMgmtMask);
                } else {
                    status = this.mISupplicantStaNetwork.setKeyMgmt(keyMgmtMask);
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(status, "setKeyMgmt");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setKeyMgmt");
                return false;
            }
        }
    }

    private boolean setProto(int protoMask) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setProto")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProto(protoMask), "setProto");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setProto");
                return false;
            }
        }
    }

    private boolean setAuthAlg(int authAlgMask) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setAuthAlg")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setAuthAlg(authAlgMask), "setAuthAlg");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setAuthAlg");
                return false;
            }
        }
    }

    private boolean setGroupCipher(int groupCipherMask) {
        SupplicantStatus status;
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setGroupCipher")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 != null) {
                    status = iSupplicantStaNetworkV12.setGroupCipher_1_2(groupCipherMask);
                } else {
                    status = this.mISupplicantStaNetwork.setGroupCipher(groupCipherMask);
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(status, "setGroupCipher");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setGroupCipher");
                return false;
            }
        }
    }

    private boolean enableTlsSuiteBEapPhase1Param(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPhase1Params")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 != null) {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iSupplicantStaNetworkV12.enableTlsSuiteBEapPhase1Param(enable), "setEapPhase1Params");
                    return checkStatusAndLogFailure;
                }
                Log.e(TAG, "Supplicant HAL version does not support setEapPhase1Params");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPhase1Params");
                return false;
            }
        }
    }

    private boolean enableSuiteBEapOpenSslCiphers() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapOpenSslCiphers")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 != null) {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iSupplicantStaNetworkV12.enableSuiteBEapOpenSslCiphers(), "setEapOpenSslCiphers");
                    return checkStatusAndLogFailure;
                }
                Log.e(TAG, "Supplicant HAL version does not support setEapOpenSslCiphers");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapOpenSslCiphers");
                return false;
            }
        }
    }

    private boolean setPairwiseCipher(int pairwiseCipherMask) {
        SupplicantStatus status;
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setPairwiseCipher")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 != null) {
                    status = iSupplicantStaNetworkV12.setPairwiseCipher_1_2(pairwiseCipherMask);
                } else {
                    status = this.mISupplicantStaNetwork.setPairwiseCipher(pairwiseCipherMask);
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(status, "setPairwiseCipher");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setPairwiseCipher");
                return false;
            }
        }
    }

    private boolean setGroupMgmtCipher(int groupMgmtCipherMask) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setGroupMgmtCipher")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 == null) {
                    return false;
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iSupplicantStaNetworkV12.setGroupMgmtCipher(groupMgmtCipherMask), "setGroupMgmtCipher");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setGroupMgmtCipher");
                return false;
            }
        }
    }

    private boolean setPskPassphrase(String psk) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setPskPassphrase")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPskPassphrase(psk), "setPskPassphrase");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setPskPassphrase");
                return false;
            }
        }
    }

    private boolean setPsk(byte[] psk) {
        synchronized (this.mLock) {
            if (psk == null) {
                Log.e(TAG, "psk is null");
                return false;
            } else if (!checkISupplicantStaNetworkAndLogFailure("setPsk")) {
                return false;
            } else {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPsk(psk), "setPsk");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPsk");
                    return false;
                }
            }
        }
    }

    private boolean setWepKey(int keyIdx, ArrayList<Byte> wepKey) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setWepKey")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepKey(keyIdx, wepKey), "setWepKey");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWepKey");
                return false;
            }
        }
    }

    private boolean setWepTxKeyIdx(int keyIdx) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setWepTxKeyIdx")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepTxKeyIdx(keyIdx), "setWepTxKeyIdx");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setWepTxKeyIdx");
                return false;
            }
        }
    }

    private boolean setRequirePmf(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setRequirePmf")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setRequirePmf(enable), "setRequirePmf");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setRequirePmf");
                return false;
            }
        }
    }

    private boolean setUpdateIdentifier(int identifier) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setUpdateIdentifier")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setUpdateIdentifier(identifier), "setUpdateIdentifier");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setUpdateIdentifier");
                return false;
            }
        }
    }

    private boolean setEapMethod(int method) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapMethod")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISehSupplicantStaNetwork.setEapMethod(method), "setEapMethod");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapMethod");
                return false;
            }
        }
    }

    private boolean setEapPhase2Method(int method) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPhase2Method")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPhase2Method(method), "setEapPhase2Method");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPhase2Method");
                return false;
            }
        }
    }

    private boolean setEapIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapIdentity")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapIdentity(identity), "setEapIdentity");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapIdentity");
                return false;
            }
        }
    }

    private boolean setEapAnonymousIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapAnonymousIdentity")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAnonymousIdentity(identity), "setEapAnonymousIdentity");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapAnonymousIdentity");
                return false;
            }
        }
    }

    private boolean setEapPassword(ArrayList<Byte> password) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPassword")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPassword(password), "setEapPassword");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPassword");
                return false;
            }
        }
    }

    private boolean setEapCACert(String path) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapCACert")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCACert(path), "setEapCACert");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapCACert");
                return false;
            }
        }
    }

    private boolean setEapCAPath(String path) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapCAPath")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCAPath(path), "setEapCAPath");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapCAPath");
                return false;
            }
        }
    }

    private boolean setEapClientCert(String path) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapClientCert")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapClientCert(path), "setEapClientCert");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapClientCert");
                return false;
            }
        }
    }

    private boolean setEapPrivateKeyId(String id) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapPrivateKeyId")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPrivateKeyId(id), "setEapPrivateKeyId");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapPrivateKeyId");
                return false;
            }
        }
    }

    private boolean setEapSubjectMatch(String match) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapSubjectMatch")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapSubjectMatch(match), "setEapSubjectMatch");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapSubjectMatch");
                return false;
            }
        }
    }

    private boolean setEapAltSubjectMatch(String match) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapAltSubjectMatch")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAltSubjectMatch(match), "setEapAltSubjectMatch");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapAltSubjectMatch");
                return false;
            }
        }
    }

    private boolean setEapEngine(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapEngine")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngine(enable), "setEapEngine");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapEngine");
                return false;
            }
        }
    }

    private boolean setEapEngineID(String id) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapEngineID")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngineID(id), "setEapEngineID");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapEngineID");
                return false;
            }
        }
    }

    private boolean setEapDomainSuffixMatch(String match) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapDomainSuffixMatch")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapDomainSuffixMatch(match), "setEapDomainSuffixMatch");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapDomainSuffixMatch");
                return false;
            }
        }
    }

    private boolean setEapProactiveKeyCaching(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setEapProactiveKeyCaching")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProactiveKeyCaching(enable), "setEapProactiveKeyCaching");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setEapProactiveKeyCaching");
                return false;
            }
        }
    }

    private boolean setIdStr(String idString) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setIdStr")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setIdStr(idString), "setIdStr");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setIdStr");
                return false;
            }
        }
    }

    private boolean setVendorSsid(boolean enable) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setVendorSsid")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setVendorSsid(enable), "setVendorSsid");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setVendorSsid");
                return false;
            }
        }
    }

    private boolean setEapPhase1Method(int method) {
        if (method == -1) {
            method = 4;
        }
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setEapPhase1Method")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setEapPhase1Method(method), "setEapPhase1Method");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setEapPhase1Method");
                return false;
            }
        }
    }

    private boolean setEapPacFile(String pac_file) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setEapPacFile")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setEapPacFile(pac_file), "setEapPacFile");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setEapPacFile");
                return false;
            }
        }
    }

    private boolean setSimIndex(int index) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setSimIndex")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setSimIndex(index), "setSimIndex");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setSimIndex");
                return false;
            }
        }
    }

    private boolean setWapiPskType(int type) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setWapiPskType")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setWapiPskType(type), "setWapiPskType");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setWapiPskType");
                return false;
            }
        }
    }

    private boolean setWapiCertFormat(int index) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setWapiCertFormat")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setWapiCertFormat(index), "setWapiCertFormat");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setWapiCertFormat");
                return false;
            }
        }
    }

    private boolean setWapiAsCert(String path) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setWapiAsCert")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setWapiAsCert(path), "setWapiAsCert");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setWapiAsCert");
                return false;
            }
        }
    }

    private boolean setWapiUserCert(String path) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("setWapiUserCert")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailureExt = checkStatusAndLogFailureExt(this.mISehSupplicantStaNetwork.setWapiUserCert(path), "setWapiUserCert");
                return checkStatusAndLogFailureExt;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "setWapiUserCert");
                return false;
            }
        }
    }

    private boolean setSaePassword(String saePassword) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setSaePassword")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 == null) {
                    return false;
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iSupplicantStaNetworkV12.setSaePassword(saePassword), "setSaePassword");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setSaePassword");
                return false;
            }
        }
    }

    private boolean setSaePasswordId(String saePasswordId) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("setSaePasswordId")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 == null) {
                    return false;
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(iSupplicantStaNetworkV12.setSaePasswordId(saePasswordId), "setSaePasswordId");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "setSaePasswordId");
                return false;
            }
        }
    }

    private boolean getSsid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getSsid")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getSsid(new ISupplicantStaNetwork.getSsidCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.lambda$getSsid$1$SupplicantStaNetworkHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getSsid");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getSsid$1$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ArrayList ssidValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mSsid = ssidValue;
        } else {
            checkStatusAndLogFailure(status, "getSsid");
        }
    }

    private boolean getBssid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getBssid")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getBssid(new ISupplicantStaNetwork.getBssidCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaNetworkHal.this.lambda$getBssid$2$SupplicantStaNetworkHal(this.f$1, supplicantStatus, bArr);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getBssid");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getBssid$2$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, byte[] bssidValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mBssid = bssidValue;
        } else {
            checkStatusAndLogFailure(status, "getBssid");
        }
    }

    private boolean getScanSsid() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getScanSsid")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getScanSsid(new ISupplicantStaNetwork.getScanSsidCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.this.lambda$getScanSsid$3$SupplicantStaNetworkHal(this.f$1, supplicantStatus, z);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getScanSsid");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getScanSsid$3$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mScanSsid = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getScanSsid");
        }
    }

    private boolean getKeyMgmt() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getKeyMgmt")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getKeyMgmt(new ISupplicantStaNetwork.getKeyMgmtCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getKeyMgmt$4$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getKeyMgmt");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getKeyMgmt$4$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int keyMgmtMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mKeyMgmtMask = keyMgmtMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getKeyMgmt");
        }
    }

    private boolean getProto() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getProto")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getProto(new ISupplicantStaNetwork.getProtoCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getProto$5$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getProto");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getProto$5$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int protoMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mProtoMask = protoMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getProto");
        }
    }

    private boolean getAuthAlg() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getAuthAlg")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getAuthAlg(new ISupplicantStaNetwork.getAuthAlgCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getAuthAlg$6$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getAuthAlg");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getAuthAlg$6$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int authAlgMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mAuthAlgMask = authAlgMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getAuthAlg");
        }
    }

    private boolean getGroupCipher() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getGroupCipher")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getGroupCipher(new ISupplicantStaNetwork.getGroupCipherCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getGroupCipher$7$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getGroupCipher");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getGroupCipher$7$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int groupCipherMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mGroupCipherMask = groupCipherMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getGroupCipher");
        }
    }

    private boolean getPairwiseCipher() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPairwiseCipher")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPairwiseCipher(new ISupplicantStaNetwork.getPairwiseCipherCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getPairwiseCipher$8$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPairwiseCipher");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getPairwiseCipher$8$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int pairwiseCipherMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mPairwiseCipherMask = pairwiseCipherMaskValue;
        } else {
            checkStatusAndLogFailure(status, "getPairwiseCipher");
        }
    }

    private boolean getGroupMgmtCipher() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getGroupMgmtCipher")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 == null) {
                    return false;
                }
                MutableBoolean statusOk = new MutableBoolean(false);
                iSupplicantStaNetworkV12.getGroupMgmtCipher(new ISupplicantStaNetwork.getGroupMgmtCipherCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getGroupMgmtCipher$9$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getGroupMgmtCipher");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getGroupMgmtCipher$9$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int groupMgmtCipherMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mGroupMgmtCipherMask = groupMgmtCipherMaskValue;
        }
        checkStatusAndLogFailure(status, "getGroupMgmtCipher");
    }

    private boolean getPskPassphrase() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPskPassphrase")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPskPassphrase(new ISupplicantStaNetwork.getPskPassphraseCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getPskPassphrase$10$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPskPassphrase");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getPskPassphrase$10$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mPskPassphrase = pskValue;
        } else {
            checkStatusAndLogFailure(status, "getPskPassphrase");
        }
    }

    private boolean getSaePassword() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getSaePassword")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork iSupplicantStaNetworkV12 = getV1_2StaNetwork();
                if (iSupplicantStaNetworkV12 == null) {
                    return false;
                }
                MutableBoolean statusOk = new MutableBoolean(false);
                iSupplicantStaNetworkV12.getSaePassword(new ISupplicantStaNetwork.getSaePasswordCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getSaePassword$11$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getSaePassword");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getSaePassword$11$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String saePassword) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mSaePassword = saePassword;
        }
        checkStatusAndLogFailure(status, "getSaePassword");
    }

    private boolean getPsk() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getPsk")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getPsk(new ISupplicantStaNetwork.getPskCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaNetworkHal.this.lambda$getPsk$12$SupplicantStaNetworkHal(this.f$1, supplicantStatus, bArr);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getPsk");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getPsk$12$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, byte[] pskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mPsk = pskValue;
        } else {
            checkStatusAndLogFailure(status, "getPsk");
        }
    }

    private boolean getWepKey(int keyIdx) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("keyIdx")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getWepKey(keyIdx, new ISupplicantStaNetwork.getWepKeyCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.lambda$getWepKey$13$SupplicantStaNetworkHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "keyIdx");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWepKey$13$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ArrayList wepKeyValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWepKey = wepKeyValue;
            return;
        }
        Log.e(TAG, "keyIdx,  failed: " + status.debugMessage);
    }

    private boolean getWepTxKeyIdx() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getWepTxKeyIdx")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getWepTxKeyIdx(new ISupplicantStaNetwork.getWepTxKeyIdxCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getWepTxKeyIdx$14$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getWepTxKeyIdx");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWepTxKeyIdx$14$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int keyIdxValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWepTxKeyIdx = keyIdxValue;
        } else {
            checkStatusAndLogFailure(status, "getWepTxKeyIdx");
        }
    }

    private boolean getRequirePmf() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getRequirePmf")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getRequirePmf(new ISupplicantStaNetwork.getRequirePmfCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.this.lambda$getRequirePmf$15$SupplicantStaNetworkHal(this.f$1, supplicantStatus, z);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getRequirePmf");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getRequirePmf$15$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mRequirePmf = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getRequirePmf");
        }
    }

    private boolean getEapMethod() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapMethod")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getEapMethod(new ISehSupplicantStaNetwork.getEapMethodCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getEapMethod$16$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapMethod");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapMethod$16$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapMethod = methodValue;
        } else {
            checkStatusAndLogFailure(status, "getEapMethod");
        }
    }

    private boolean getEapPhase2Method() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPhase2Method")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPhase2Method(new ISupplicantStaNetwork.getEapPhase2MethodCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getEapPhase2Method$17$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPhase2Method");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapPhase2Method$17$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapPhase2Method = methodValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPhase2Method");
        }
    }

    private boolean getEapIdentity() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapIdentity")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapIdentity(new ISupplicantStaNetwork.getEapIdentityCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.lambda$getEapIdentity$18$SupplicantStaNetworkHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapIdentity");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapIdentity$18$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapIdentity = identityValue;
        } else {
            checkStatusAndLogFailure(status, "getEapIdentity");
        }
    }

    private boolean getEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapAnonymousIdentity")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapAnonymousIdentity(new ISupplicantStaNetwork.getEapAnonymousIdentityCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.lambda$getEapAnonymousIdentity$19$SupplicantStaNetworkHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapAnonymousIdentity");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapAnonymousIdentity$19$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapAnonymousIdentity = identityValue;
        } else {
            checkStatusAndLogFailure(status, "getEapAnonymousIdentity");
        }
    }

    public String fetchEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (!getEapAnonymousIdentity()) {
                return null;
            }
            String stringFromByteArrayList = NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity);
            return stringFromByteArrayList;
        }
    }

    private boolean getEapPassword() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPassword")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPassword(new ISupplicantStaNetwork.getEapPasswordCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.lambda$getEapPassword$20$SupplicantStaNetworkHal(this.f$1, supplicantStatus, arrayList);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPassword");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapPassword$20$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ArrayList passwordValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapPassword = passwordValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPassword");
        }
    }

    private boolean getEapCACert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapCACert")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapCACert(new ISupplicantStaNetwork.getEapCACertCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapCACert$21$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapCACert");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapCACert$21$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapCACert = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapCACert");
        }
    }

    private boolean getEapCAPath() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapCAPath")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapCAPath(new ISupplicantStaNetwork.getEapCAPathCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapCAPath$22$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapCAPath");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapCAPath$22$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapCAPath = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapCAPath");
        }
    }

    private boolean getEapClientCert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapClientCert")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapClientCert(new ISupplicantStaNetwork.getEapClientCertCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapClientCert$23$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapClientCert");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapClientCert$23$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapClientCert = pathValue;
        } else {
            checkStatusAndLogFailure(status, "getEapClientCert");
        }
    }

    private boolean getEapPrivateKeyId() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapPrivateKeyId")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapPrivateKeyId(new ISupplicantStaNetwork.getEapPrivateKeyIdCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapPrivateKeyId$24$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapPrivateKeyId");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapPrivateKeyId$24$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapPrivateKeyId = idValue;
        } else {
            checkStatusAndLogFailure(status, "getEapPrivateKeyId");
        }
    }

    private boolean getEapSubjectMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapSubjectMatch")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapSubjectMatch(new ISupplicantStaNetwork.getEapSubjectMatchCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapSubjectMatch$25$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapSubjectMatch");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapSubjectMatch$25$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapSubjectMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapSubjectMatch");
        }
    }

    private boolean getEapAltSubjectMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapAltSubjectMatch")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapAltSubjectMatch(new ISupplicantStaNetwork.getEapAltSubjectMatchCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapAltSubjectMatch$26$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapAltSubjectMatch");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapAltSubjectMatch$26$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapAltSubjectMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapAltSubjectMatch");
        }
    }

    private boolean getEapEngine() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapEngine")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapEngine(new ISupplicantStaNetwork.getEapEngineCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.this.lambda$getEapEngine$27$SupplicantStaNetworkHal(this.f$1, supplicantStatus, z);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapEngine");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapEngine$27$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapEngine = enabledValue;
        } else {
            checkStatusAndLogFailure(status, "getEapEngine");
        }
    }

    private boolean getEapEngineID() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapEngineID")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapEngineID(new ISupplicantStaNetwork.getEapEngineIDCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapEngineID$28$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapEngineID");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapEngineID$28$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapEngineID = idValue;
        } else {
            checkStatusAndLogFailure(status, "getEapEngineID");
        }
    }

    private boolean getEapDomainSuffixMatch() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getEapDomainSuffixMatch")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getEapDomainSuffixMatch(new ISupplicantStaNetwork.getEapDomainSuffixMatchCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapDomainSuffixMatch$29$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getEapDomainSuffixMatch");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapDomainSuffixMatch$29$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapDomainSuffixMatch = matchValue;
        } else {
            checkStatusAndLogFailure(status, "getEapDomainSuffixMatch");
        }
    }

    private boolean getIdStr() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getIdStr")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISupplicantStaNetwork.getIdStr(new ISupplicantStaNetwork.getIdStrCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getIdStr$30$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteException(e, "getIdStr");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getIdStr$30$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String idString) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mIdStr = idString;
        } else {
            checkStatusAndLogFailure(status, "getIdStr");
        }
    }

    private boolean getVendorSsidValue() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getVendorSsidValue")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getVendorSsidValue(new ISehSupplicantStaNetwork.getVendorSsidValueCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                        SupplicantStaNetworkHal.this.lambda$getVendorSsidValue$31$SupplicantStaNetworkHal(this.f$1, supplicantStatus, z);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getVendorSsidValue");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getVendorSsidValue$31$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mVendorSsid = enabledValue;
        } else {
            checkStatusAndLogFailureExt(status, "getVendorSsidValue");
        }
    }

    private boolean getAutoReconnectValue() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getAutoReconnectValue")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getAutoReconnectValue(new ISehSupplicantStaNetwork.getAutoReconnectValueCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getAutoReconnectValue$32$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getAutoReconnectValue");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getAutoReconnectValue$32$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mAutoReconnect = enabledValue;
        } else {
            checkStatusAndLogFailureExt(status, "getAutoReconnectValue");
        }
    }

    private boolean getEapPhase1Method() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getEapPhase1Method")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getEapPhase1Method(new ISehSupplicantStaNetwork.getEapPhase1MethodCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getEapPhase1Method$33$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getEapPhase1Method");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapPhase1Method$33$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapPhase1Method = methodValue;
        } else {
            checkStatusAndLogFailureExt(status, "getEapPhase1Method");
        }
    }

    private boolean getEapPacFile() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getEapPacFile")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getEapPacFile(new ISehSupplicantStaNetwork.getEapPacFileCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getEapPacFile$34$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getEapPacFile");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getEapPacFile$34$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pac_file) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mEapPacFile = pac_file;
        } else {
            checkStatusAndLogFailureExt(status, "getEapPacFile");
        }
    }

    private boolean getSimIndex() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getSimIndex")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getSimIndex(new ISehSupplicantStaNetwork.getSimIndexCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getSimIndex$35$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getSimIndex");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getSimIndex$35$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int index) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mSimNumber = index;
        } else {
            checkStatusAndLogFailureExt(status, "getSimIndex");
        }
    }

    private boolean getWapiPskType() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getWapiPskType")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getWapiPskType(new ISehSupplicantStaNetwork.getWapiPskTypeCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getWapiPskType$36$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getWapiPskType");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWapiPskType$36$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int wapiPskType) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWapiPskType = wapiPskType;
        } else {
            checkStatusAndLogFailureExt(status, "getWapiPskType");
        }
    }

    private boolean getWapiCertFormat() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getWapiCertFormat")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getWapiCertFormat(new ISehSupplicantStaNetwork.getWapiCertFormatCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        SupplicantStaNetworkHal.this.lambda$getWapiCertFormat$37$SupplicantStaNetworkHal(this.f$1, supplicantStatus, i);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getWapiCertFormat");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWapiCertFormat$37$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, int wapiCertIndex) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWapiCertIndex = wapiCertIndex;
        } else {
            checkStatusAndLogFailureExt(status, "getWapiCertFormat");
        }
    }

    private boolean getWapiAsCert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getWapiAsCert")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getWapiAsCert(new ISehSupplicantStaNetwork.getWapiAsCertCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getWapiAsCert$38$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getWapiAsCert");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWapiAsCert$38$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWapiAsCert = pathValue;
        } else {
            checkStatusAndLogFailureExt(status, "getWapiAsCert");
        }
    }

    private boolean getWapiUserCert() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkExtAndLogFailure("getWapiUserCert")) {
                return false;
            }
            try {
                MutableBoolean statusOk = new MutableBoolean(false);
                this.mISehSupplicantStaNetwork.getWapiUserCert(new ISehSupplicantStaNetwork.getWapiUserCertCallback(statusOk) {
                    private final /* synthetic */ MutableBoolean f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        SupplicantStaNetworkHal.this.lambda$getWapiUserCert$39$SupplicantStaNetworkHal(this.f$1, supplicantStatus, str);
                    }
                });
                boolean z = statusOk.value;
                return z;
            } catch (RemoteException e) {
                handleRemoteExceptionExt(e, "getWapiUserCert");
                return false;
            }
        }
    }

    public /* synthetic */ void lambda$getWapiUserCert$39$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            this.mWapiUserCert = pathValue;
        } else {
            checkStatusAndLogFailureExt(status, "getWapiUserCert");
        }
    }

    private boolean enable(boolean noConnect) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("enable")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.enable(noConnect), "enable");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "enable");
                return false;
            }
        }
    }

    private boolean disable() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("disable")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.disable(), "disable");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "disable");
                return false;
            }
        }
    }

    public boolean select() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("select")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.select(), "select");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "select");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimGsmAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = GSM_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                ArrayList<ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams> params = new ArrayList<>();
                while (match.find()) {
                    if (match.groupCount() != 2) {
                        Log.e(TAG, "Malformed gsm auth response params: " + paramsStr);
                        return false;
                    }
                    ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams param = new ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams();
                    byte[] kc = NativeUtil.hexStringToByteArray(match.group(1));
                    if (kc != null) {
                        if (kc.length == param.f8kc.length) {
                            byte[] sres = NativeUtil.hexStringToByteArray(match.group(2));
                            if (sres != null) {
                                if (sres.length == param.sres.length) {
                                    System.arraycopy(kc, 0, param.f8kc, 0, param.f8kc.length);
                                    System.arraycopy(sres, 0, param.sres, 0, param.sres.length);
                                    params.add(param);
                                }
                            }
                            Log.e(TAG, "Invalid sres value: " + match.group(2));
                            return false;
                        }
                    }
                    Log.e(TAG, "Invalid kc value: " + match.group(1));
                    return false;
                }
                if (params.size() <= 3) {
                    if (params.size() >= 2) {
                        boolean sendNetworkEapSimGsmAuthResponse = sendNetworkEapSimGsmAuthResponse(params);
                        return sendNetworkEapSimGsmAuthResponse;
                    }
                }
                Log.e(TAG, "Malformed gsm auth response params: " + paramsStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimGsmAuthResponse(ArrayList<ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams> params) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthResponse")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthResponse(params), "sendNetworkEapSimGsmAuthResponse");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimGsmAuthResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimGsmAuthFailure() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthFailure")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthFailure(), "sendNetworkEapSimGsmAuthFailure");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimGsmAuthFailure");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = UMTS_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find()) {
                    if (match.groupCount() == 3) {
                        ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams params = new ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams();
                        byte[] ik = NativeUtil.hexStringToByteArray(match.group(1));
                        if (ik != null) {
                            if (ik.length == params.f10ik.length) {
                                byte[] ck = NativeUtil.hexStringToByteArray(match.group(2));
                                if (ck != null) {
                                    if (ck.length == params.f9ck.length) {
                                        byte[] res = NativeUtil.hexStringToByteArray(match.group(3));
                                        if (res != null) {
                                            if (res.length != 0) {
                                                System.arraycopy(ik, 0, params.f10ik, 0, params.f10ik.length);
                                                System.arraycopy(ck, 0, params.f9ck, 0, params.f9ck.length);
                                                for (byte b : res) {
                                                    params.res.add(Byte.valueOf(b));
                                                }
                                                boolean sendNetworkEapSimUmtsAuthResponse = sendNetworkEapSimUmtsAuthResponse(params);
                                                return sendNetworkEapSimUmtsAuthResponse;
                                            }
                                        }
                                        Log.e(TAG, "Invalid res value: " + match.group(3));
                                        return false;
                                    }
                                }
                                Log.e(TAG, "Invalid ck value: " + match.group(2));
                                return false;
                            }
                        }
                        Log.e(TAG, "Invalid ik value: " + match.group(1));
                        return false;
                    }
                }
                Log.e(TAG, "Malformed umts auth response params: " + paramsStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAuthResponse(ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams params) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthResponse")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthResponse(params), "sendNetworkEapSimUmtsAuthResponse");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAuthResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAutsResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                Matcher match = UMTS_AUTS_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find()) {
                    if (match.groupCount() == 1) {
                        byte[] auts = NativeUtil.hexStringToByteArray(match.group(1));
                        if (auts != null) {
                            if (auts.length == 14) {
                                boolean sendNetworkEapSimUmtsAutsResponse = sendNetworkEapSimUmtsAutsResponse(auts);
                                return sendNetworkEapSimUmtsAutsResponse;
                            }
                        }
                        Log.e(TAG, "Invalid auts value: " + match.group(1));
                        return false;
                    }
                }
                Log.e(TAG, "Malformed umts auts response params: " + paramsStr);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + paramsStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAutsResponse(byte[] auts) {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAutsResponse")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAutsResponse(auts), "sendNetworkEapSimUmtsAutsResponse");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAutsResponse");
                return false;
            }
        }
    }

    public boolean sendNetworkEapSimUmtsAuthFailure() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthFailure")) {
                return false;
            }
            try {
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthFailure(), "sendNetworkEapSimUmtsAuthFailure");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapSimUmtsAuthFailure");
                return false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork getSupplicantStaNetworkForV1_1Mockable() {
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork iSupplicantStaNetwork = this.mISupplicantStaNetwork;
        if (iSupplicantStaNetwork == null) {
            return null;
        }
        return android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork.castFrom(iSupplicantStaNetwork);
    }

    /* access modifiers changed from: protected */
    public android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork getSupplicantStaNetworkForV1_2Mockable() {
        android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork iSupplicantStaNetwork = this.mISupplicantStaNetwork;
        if (iSupplicantStaNetwork == null) {
            return null;
        }
        return android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork.castFrom(iSupplicantStaNetwork);
    }

    public boolean sendNetworkEapIdentityResponse(String identityStr, String encryptedIdentityStr) {
        boolean sendNetworkEapIdentityResponse;
        synchronized (this.mLock) {
            try {
                ArrayList<Byte> unencryptedIdentity = NativeUtil.stringToByteArrayList(identityStr);
                ArrayList<Byte> encryptedIdentity = null;
                if (!TextUtils.isEmpty(encryptedIdentityStr)) {
                    encryptedIdentity = NativeUtil.stringToByteArrayList(encryptedIdentityStr);
                }
                sendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(unencryptedIdentity, encryptedIdentity);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + identityStr + "," + encryptedIdentityStr, e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
        return sendNetworkEapIdentityResponse;
    }

    private boolean sendNetworkEapIdentityResponse(ArrayList<Byte> unencryptedIdentity, ArrayList<Byte> encryptedIdentity) {
        SupplicantStatus status;
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("sendNetworkEapIdentityResponse")) {
                return false;
            }
            try {
                android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork iSupplicantStaNetworkV11 = getSupplicantStaNetworkForV1_1Mockable();
                if (iSupplicantStaNetworkV11 == null || encryptedIdentity == null) {
                    status = this.mISupplicantStaNetwork.sendNetworkEapIdentityResponse(unencryptedIdentity);
                } else {
                    status = iSupplicantStaNetworkV11.sendNetworkEapIdentityResponse_1_1(unencryptedIdentity, encryptedIdentity);
                }
                boolean checkStatusAndLogFailure = checkStatusAndLogFailure(status, "sendNetworkEapIdentityResponse");
                return checkStatusAndLogFailure;
            } catch (RemoteException e) {
                handleRemoteException(e, "sendNetworkEapIdentityResponse");
                return false;
            }
        }
    }

    public String getWpsNfcConfigurationToken() {
        synchronized (this.mLock) {
            ArrayList<Byte> token = getWpsNfcConfigurationTokenInternal();
            if (token == null) {
                return null;
            }
            String hexStringFromByteArray = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList(token));
            return hexStringFromByteArray;
        }
    }

    private ArrayList<Byte> getWpsNfcConfigurationTokenInternal() {
        synchronized (this.mLock) {
            if (!checkISupplicantStaNetworkAndLogFailure("getWpsNfcConfigurationToken")) {
                return null;
            }
            HidlSupport.Mutable<ArrayList<Byte>> gotToken = new HidlSupport.Mutable<>();
            try {
                this.mISupplicantStaNetwork.getWpsNfcConfigurationToken(new ISupplicantStaNetwork.getWpsNfcConfigurationTokenCallback(gotToken) {
                    private final /* synthetic */ HidlSupport.Mutable f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaNetworkHal.this.mo2667xe98c0c14(this.f$1, supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getWpsNfcConfigurationToken");
            }
            ArrayList<Byte> arrayList = (ArrayList) gotToken.value;
            return arrayList;
        }
    }

    /* renamed from: lambda$getWpsNfcConfigurationTokenInternal$40$SupplicantStaNetworkHal */
    public /* synthetic */ void mo2667xe98c0c14(HidlSupport.Mutable gotToken, SupplicantStatus status, ArrayList token) {
        if (checkStatusAndLogFailure(status, "getWpsNfcConfigurationToken")) {
            gotToken.value = token;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISupplicantStaNetwork." + methodStr + " failed: " + status);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetwork." + methodStr + " succeeded");
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailureExt(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            if (status.code != 0) {
                Log.e(TAG, "ISehSupplicantStaNetwork." + methodStr + " failed: " + status);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISehSupplicantStaNetwork." + methodStr + " succeeded");
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaNetworkCallback." + methodStr + " received");
            }
        }
    }

    private boolean checkISupplicantStaNetworkAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISupplicantStaNetwork != null) {
                return true;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISupplicantStaNetwork is null");
            return false;
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            this.mISupplicantStaNetwork = null;
            Log.e(TAG, "ISupplicantStaNetwork." + methodStr + " failed with exception", e);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0057, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean updateCurrentBss() {
        /*
            r6 = this;
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            java.lang.String r1 = ""
            java.lang.String r2 = CONFIG_SECURE_SVC_INTEGRATION     // Catch:{ all -> 0x0058 }
            boolean r1 = r1.equals(r2)     // Catch:{ all -> 0x0058 }
            r2 = 0
            if (r1 == 0) goto L_0x0056
            com.samsung.android.feature.SemCscFeature r1 = com.samsung.android.feature.SemCscFeature.getInstance()     // Catch:{ all -> 0x0058 }
            java.lang.String r3 = "CscFeature_Wifi_DisableMWIPS"
            boolean r1 = r1.getBoolean(r3)     // Catch:{ all -> 0x0058 }
            if (r1 != 0) goto L_0x0056
            java.lang.String r1 = "updateCurrentBss"
            java.lang.String r3 = "updateCurrentBss"
            boolean r3 = r6.checkISupplicantStaNetworkExtAndLogFailure(r3)     // Catch:{ all -> 0x0058 }
            if (r3 != 0) goto L_0x002d
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "MWIPS updateCurrentBss failed"
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x0058 }
            monitor-exit(r0)     // Catch:{ all -> 0x0058 }
            return r2
        L_0x002d:
            java.lang.String r3 = "SupplicantStaNetworkHal"
            java.lang.String r4 = "MWIPS updateCurrentBss"
            android.util.Log.d(r3, r4)     // Catch:{ RemoteException -> 0x0047 }
            android.util.MutableBoolean r3 = new android.util.MutableBoolean     // Catch:{ RemoteException -> 0x0047 }
            r3.<init>(r2)     // Catch:{ RemoteException -> 0x0047 }
            vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantStaNetwork r4 = r6.mISehSupplicantStaNetwork     // Catch:{ RemoteException -> 0x0047 }
            com.android.server.wifi.-$$Lambda$SupplicantStaNetworkHal$dY4dIpkzsy3ZvqihpmQ91SYO-XY r5 = new com.android.server.wifi.-$$Lambda$SupplicantStaNetworkHal$dY4dIpkzsy3ZvqihpmQ91SYO-XY     // Catch:{ RemoteException -> 0x0047 }
            r5.<init>(r3)     // Catch:{ RemoteException -> 0x0047 }
            r4.getBss(r5)     // Catch:{ RemoteException -> 0x0047 }
            boolean r2 = r3.value     // Catch:{ RemoteException -> 0x0047 }
            monitor-exit(r0)     // Catch:{ all -> 0x0058 }
            return r2
        L_0x0047:
            r3 = move-exception
            java.lang.String r4 = "updateCurrentBss"
            r6.handleRemoteExceptionExt(r3, r4)     // Catch:{ all -> 0x0058 }
            java.lang.String r4 = "SupplicantStaNetworkHal"
            java.lang.String r5 = "MWIPS getBss exception"
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x0058 }
            monitor-exit(r0)     // Catch:{ all -> 0x0058 }
            return r2
        L_0x0056:
            monitor-exit(r0)     // Catch:{ all -> 0x0058 }
            return r2
        L_0x0058:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0058 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.updateCurrentBss():boolean");
    }

    public /* synthetic */ void lambda$updateCurrentBss$41$SupplicantStaNetworkHal(MutableBoolean statusOk, SupplicantStatus status, ISehSupplicantStaNetwork.BssParam param) {
        statusOk.value = status.code == 0;
        MobileWipsFrameworkService mwfs = MobileWipsFrameworkService.getInstance();
        if (statusOk.value) {
            if (mwfs != null) {
                mwfs.setCurrentBss(param);
            }
            if (param != null) {
                Log.d(TAG, "MWIPS updateCurrentBss bssid " + NativeUtil.macAddressFromByteArray(param.bssid) + " " + param.ssid);
                return;
            }
            return;
        }
        checkStatusAndLogFailureExt(status, "updateCurrentBss");
        if (mwfs != null) {
            mwfs.setCurrentBss((ISehSupplicantStaNetwork.BssParam) null);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0026, code lost:
        return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.BitSet addFastTransitionFlags(java.util.BitSet r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mLock
            monitor-enter(r0)
            boolean r1 = r3.mSystemSupportsFastBssTransition     // Catch:{ all -> 0x0027 }
            if (r1 != 0) goto L_0x0009
            monitor-exit(r0)     // Catch:{ all -> 0x0027 }
            return r4
        L_0x0009:
            java.lang.Object r1 = r4.clone()     // Catch:{ all -> 0x0027 }
            java.util.BitSet r1 = (java.util.BitSet) r1     // Catch:{ all -> 0x0027 }
            r2 = 1
            boolean r2 = r4.get(r2)     // Catch:{ all -> 0x0027 }
            if (r2 == 0) goto L_0x001a
            r2 = 6
            r1.set(r2)     // Catch:{ all -> 0x0027 }
        L_0x001a:
            r2 = 2
            boolean r2 = r4.get(r2)     // Catch:{ all -> 0x0027 }
            if (r2 == 0) goto L_0x0025
            r2 = 7
            r1.set(r2)     // Catch:{ all -> 0x0027 }
        L_0x0025:
            monitor-exit(r0)     // Catch:{ all -> 0x0027 }
            return r1
        L_0x0027:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0027 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.addFastTransitionFlags(java.util.BitSet):java.util.BitSet");
    }

    private boolean checkISupplicantStaNetworkExtAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISehSupplicantStaNetwork != null) {
                return true;
            }
            Log.e(TAG, "Can't call " + methodStr + ", ISehSupplicantStaNetwork is null");
            return false;
        }
    }

    private void handleRemoteExceptionExt(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            this.mISehSupplicantStaNetwork = null;
            Log.e(TAG, "ISehSupplicantStaNetwork." + methodStr + " failed with exception", e);
        }
    }

    private BitSet removeFastTransitionFlags(BitSet keyManagementFlags) {
        BitSet modifiedFlags;
        synchronized (this.mLock) {
            modifiedFlags = (BitSet) keyManagementFlags.clone();
            modifiedFlags.clear(6);
            modifiedFlags.clear(7);
        }
        return modifiedFlags;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x002a, code lost:
        return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.BitSet addSha256KeyMgmtFlags(java.util.BitSet r5) {
        /*
            r4 = this;
            java.lang.Object r0 = r4.mLock
            monitor-enter(r0)
            java.lang.Object r1 = r5.clone()     // Catch:{ all -> 0x002b }
            java.util.BitSet r1 = (java.util.BitSet) r1     // Catch:{ all -> 0x002b }
            android.hardware.wifi.supplicant.V1_2.ISupplicantStaNetwork r2 = r4.getV1_2StaNetwork()     // Catch:{ all -> 0x002b }
            if (r2 != 0) goto L_0x0011
            monitor-exit(r0)     // Catch:{ all -> 0x002b }
            return r1
        L_0x0011:
            r3 = 1
            boolean r3 = r5.get(r3)     // Catch:{ all -> 0x002b }
            if (r3 == 0) goto L_0x001d
            r3 = 11
            r1.set(r3)     // Catch:{ all -> 0x002b }
        L_0x001d:
            r3 = 2
            boolean r3 = r5.get(r3)     // Catch:{ all -> 0x002b }
            if (r3 == 0) goto L_0x0029
            r3 = 12
            r1.set(r3)     // Catch:{ all -> 0x002b }
        L_0x0029:
            monitor-exit(r0)     // Catch:{ all -> 0x002b }
            return r1
        L_0x002b:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x002b }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.addSha256KeyMgmtFlags(java.util.BitSet):java.util.BitSet");
    }

    private BitSet removeSha256KeyMgmtFlags(BitSet keyManagementFlags) {
        BitSet modifiedFlags;
        synchronized (this.mLock) {
            modifiedFlags = (BitSet) keyManagementFlags.clone();
            modifiedFlags.clear(11);
            modifiedFlags.clear(12);
        }
        return modifiedFlags;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001c, code lost:
        return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.BitSet addFils256KeyMgmtFlags(java.util.BitSet r4) {
        /*
            r3 = this;
            java.lang.Object r0 = r3.mLock
            monitor-enter(r0)
            boolean r1 = r3.mSystemSupportsFilsKeyMgmt     // Catch:{ all -> 0x001d }
            if (r1 != 0) goto L_0x0009
            monitor-exit(r0)     // Catch:{ all -> 0x001d }
            return r4
        L_0x0009:
            java.lang.Object r1 = r4.clone()     // Catch:{ all -> 0x001d }
            java.util.BitSet r1 = (java.util.BitSet) r1     // Catch:{ all -> 0x001d }
            r2 = 2
            boolean r2 = r4.get(r2)     // Catch:{ all -> 0x001d }
            if (r2 == 0) goto L_0x001b
            r2 = 20
            r1.set(r2)     // Catch:{ all -> 0x001d }
        L_0x001b:
            monitor-exit(r0)     // Catch:{ all -> 0x001d }
            return r1
        L_0x001d:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x001d }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.SupplicantStaNetworkHal.addFils256KeyMgmtFlags(java.util.BitSet):java.util.BitSet");
    }

    private BitSet removeFils256KeyMgmtFlags(BitSet keyManagementFlags) {
        BitSet modifiedFlags;
        synchronized (this.mLock) {
            modifiedFlags = (BitSet) keyManagementFlags.clone();
            modifiedFlags.clear(20);
        }
        return modifiedFlags;
    }

    public static String createNetworkExtra(Map<String, String> values) {
        try {
            return URLEncoder.encode(new JSONObject(values).toString(), "UTF-8");
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e.toString());
            return null;
        } catch (UnsupportedEncodingException e2) {
            Log.e(TAG, "Unable to serialize networkExtra: " + e2.toString());
            return null;
        }
    }

    private String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static Map<String, String> parseNetworkExtra(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(URLDecoder.decode(encoded, "UTF-8"));
            Map<String, String> values = new HashMap<>();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                if (value instanceof String) {
                    values.put(key, (String) value);
                }
            }
            return values;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to deserialize networkExtra: " + e.toString());
            return null;
        } catch (JSONException e2) {
            return null;
        }
    }

    private class SupplicantStaNetworkHalCallback extends ISupplicantStaNetworkCallback.Stub {
        private final int mFramewokNetworkId;
        private final String mSsid;

        SupplicantStaNetworkHalCallback(int framewokNetworkId, String ssid) {
            this.mFramewokNetworkId = framewokNetworkId;
            this.mSsid = ssid;
        }

        public void onNetworkEapSimGsmAuthRequest(ISupplicantStaNetworkCallback.NetworkRequestEapSimGsmAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimGsmAuthRequest");
                String[] data = new String[params.rands.size()];
                int i = 0;
                Iterator<byte[]> it = params.rands.iterator();
                while (it.hasNext()) {
                    data[i] = NativeUtil.hexStringFromByteArray(it.next());
                    i++;
                }
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkGsmAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, data);
            }
        }

        public void onNetworkEapSimUmtsAuthRequest(ISupplicantStaNetworkCallback.NetworkRequestEapSimUmtsAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimUmtsAuthRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkUmtsAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, new String[]{NativeUtil.hexStringFromByteArray(params.rand), NativeUtil.hexStringFromByteArray(params.autn)});
            }
        }

        public void onNetworkEapIdentityRequest() {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapIdentityRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkIdentityRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid);
            }
        }
    }
}
