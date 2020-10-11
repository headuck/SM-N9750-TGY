package com.android.server.wifi.hotspot2;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.DomainNameElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.RoamingConsortiumElement;
import com.android.server.wifi.hotspot2.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.hotspot2.anqp.eap.NonEAPInnerAuth;
import com.android.server.wifi.util.InformationElementUtil;
import com.samsung.android.feature.SemCscFeature;
import com.samsung.android.net.wifi.OpBrandingLoader;
import com.sec.android.app.CscFeatureTagWifi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PasspointProvider {
    private static final String ALIAS_ALIAS_REMEDIATION_TYPE = "REMEDIATION_";
    private static final String ALIAS_HS_TYPE = "HS2_";
    public static final String CONFIG_OP_BRANDING = SemCscFeature.getInstance().getString(CscFeatureTagWifi.TAG_CSCFEATURE_WIFI_CONFIGOPBRANDING);
    private static final String TAG = "PasspointProvider";
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private final AuthParam mAuthParam;
    private List<String> mCaCertificateAliases;
    private String mClientCertificateAlias;
    private String mClientPrivateKeyAlias;
    private final PasspointConfiguration mConfig;
    private final int mCreatorUid;
    private final int mEAPMethodID;
    private boolean mHasEverConnected;
    private final IMSIParameter mImsiParameter;
    private boolean mIsEphemeral;
    private boolean mIsShared;
    private final WifiKeyStore mKeyStore;
    private final List<String> mMatchingSIMImsiList;
    private final String mPackageName;
    private final long mProviderId;
    private String mRemediationCaCertificateAlias;

    public PasspointProvider(PasspointConfiguration config, WifiKeyStore keyStore, SIMAccessor simAccessor, long providerId, int creatorUid, String packageName) {
        this(config, keyStore, simAccessor, providerId, creatorUid, packageName, (List<String>) null, (String) null, (String) null, (String) null, false, false);
    }

    public PasspointProvider(PasspointConfiguration config, WifiKeyStore keyStore, SIMAccessor simAccessor, long providerId, int creatorUid, String packageName, List<String> caCertificateAliases, String clientCertificateAlias, String clientPrivateKeyAlias, String remediationCaCertificateAlias, boolean hasEverConnected, boolean isShared) {
        this.mIsEphemeral = false;
        this.mConfig = new PasspointConfiguration(config);
        this.mKeyStore = keyStore;
        this.mProviderId = providerId;
        this.mCreatorUid = creatorUid;
        this.mPackageName = packageName;
        this.mCaCertificateAliases = caCertificateAliases;
        this.mClientCertificateAlias = clientCertificateAlias;
        this.mClientPrivateKeyAlias = clientPrivateKeyAlias;
        this.mRemediationCaCertificateAlias = remediationCaCertificateAlias;
        this.mHasEverConnected = hasEverConnected;
        this.mIsShared = isShared;
        if (this.mConfig.getCredential().getUserCredential() != null) {
            this.mEAPMethodID = 21;
            this.mAuthParam = new NonEAPInnerAuth(NonEAPInnerAuth.getAuthTypeID(this.mConfig.getCredential().getUserCredential().getNonEapInnerMethod()));
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
            SIMAccessor sIMAccessor = simAccessor;
        } else if (this.mConfig.getCredential().getCertCredential() != null) {
            this.mEAPMethodID = 13;
            this.mAuthParam = null;
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
            SIMAccessor sIMAccessor2 = simAccessor;
        } else {
            this.mEAPMethodID = this.mConfig.getCredential().getSimCredential().getEapType();
            this.mAuthParam = null;
            this.mImsiParameter = IMSIParameter.build(this.mConfig.getCredential().getSimCredential().getImsi());
            this.mMatchingSIMImsiList = simAccessor.getMatchingImsis(this.mImsiParameter);
        }
    }

    public PasspointConfiguration getConfig() {
        return new PasspointConfiguration(this.mConfig);
    }

    public List<String> getCaCertificateAliases() {
        return this.mCaCertificateAliases;
    }

    public String getClientPrivateKeyAlias() {
        return this.mClientPrivateKeyAlias;
    }

    public String getClientCertificateAlias() {
        return this.mClientCertificateAlias;
    }

    public String getRemediationCaCertificateAlias() {
        return this.mRemediationCaCertificateAlias;
    }

    public long getProviderId() {
        return this.mProviderId;
    }

    public int getCreatorUid() {
        return this.mCreatorUid;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean getHasEverConnected() {
        return this.mHasEverConnected;
    }

    public void setHasEverConnected(boolean hasEverConnected) {
        this.mHasEverConnected = hasEverConnected;
    }

    public boolean isEphemeral() {
        return this.mIsEphemeral;
    }

    public void setEphemeral(boolean isEphemeral) {
        this.mIsEphemeral = isEphemeral;
    }

    public IMSIParameter getImsiParameter() {
        return this.mImsiParameter;
    }

    public boolean installCertsAndKeys() {
        X509Certificate[] x509Certificates = this.mConfig.getCredential().getCaCertificates();
        if (x509Certificates != null) {
            this.mCaCertificateAliases = new ArrayList();
            for (int i = 0; i < x509Certificates.length; i++) {
                String alias = String.format("%s%s_%d", new Object[]{ALIAS_HS_TYPE, Long.valueOf(this.mProviderId), Integer.valueOf(i)});
                WifiKeyStore wifiKeyStore = this.mKeyStore;
                if (!wifiKeyStore.putCertInKeyStore("CACERT_" + alias, x509Certificates[i])) {
                    Log.e(TAG, "Failed to install CA Certificate");
                    uninstallCertsAndKeys();
                    return false;
                }
                this.mCaCertificateAliases.add(alias);
            }
        }
        if (this.mConfig.getCredential().getClientPrivateKey() != null) {
            if (!this.mKeyStore.putKeyInKeyStore("USRPKEY_HS2_" + this.mProviderId, this.mConfig.getCredential().getClientPrivateKey())) {
                Log.e(TAG, "Failed to install client private key");
                uninstallCertsAndKeys();
                return false;
            }
            this.mClientPrivateKeyAlias = ALIAS_HS_TYPE + this.mProviderId;
        }
        if (this.mConfig.getCredential().getClientCertificateChain() != null) {
            X509Certificate clientCert = getClientCertificate(this.mConfig.getCredential().getClientCertificateChain(), this.mConfig.getCredential().getCertCredential().getCertSha256Fingerprint());
            if (clientCert == null) {
                Log.e(TAG, "Failed to locate client certificate");
                uninstallCertsAndKeys();
                return false;
            }
            if (!this.mKeyStore.putCertInKeyStore("USRCERT_HS2_" + this.mProviderId, clientCert)) {
                Log.e(TAG, "Failed to install client certificate");
                uninstallCertsAndKeys();
                return false;
            }
            this.mClientCertificateAlias = ALIAS_HS_TYPE + this.mProviderId;
        }
        if (this.mConfig.getSubscriptionUpdate() != null) {
            X509Certificate certificate = this.mConfig.getSubscriptionUpdate().getCaCertificate();
            if (certificate == null) {
                Log.e(TAG, "Failed to locate CA certificate for remediation");
                uninstallCertsAndKeys();
                return false;
            }
            this.mRemediationCaCertificateAlias = "HS2_REMEDIATION_" + this.mProviderId;
            if (!this.mKeyStore.putCertInKeyStore("CACERT_" + this.mRemediationCaCertificateAlias, certificate)) {
                Log.e(TAG, "Failed to install CA certificate for remediation");
                this.mRemediationCaCertificateAlias = null;
                uninstallCertsAndKeys();
                return false;
            }
        }
        this.mConfig.getCredential().setCaCertificates((X509Certificate[]) null);
        this.mConfig.getCredential().setClientPrivateKey((PrivateKey) null);
        this.mConfig.getCredential().setClientCertificateChain((X509Certificate[]) null);
        if (this.mConfig.getSubscriptionUpdate() != null) {
            this.mConfig.getSubscriptionUpdate().setCaCertificate((X509Certificate) null);
        }
        return true;
    }

    public void uninstallCertsAndKeys() {
        List<String> list = this.mCaCertificateAliases;
        if (list != null) {
            for (String certificateAlias : list) {
                WifiKeyStore wifiKeyStore = this.mKeyStore;
                if (!wifiKeyStore.removeEntryFromKeyStore("CACERT_" + certificateAlias)) {
                    Log.e(TAG, "Failed to remove entry: " + certificateAlias);
                }
            }
            this.mCaCertificateAliases = null;
        }
        if (this.mClientPrivateKeyAlias != null) {
            WifiKeyStore wifiKeyStore2 = this.mKeyStore;
            if (!wifiKeyStore2.removeEntryFromKeyStore("USRPKEY_" + this.mClientPrivateKeyAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mClientPrivateKeyAlias);
            }
            this.mClientPrivateKeyAlias = null;
        }
        if (this.mClientCertificateAlias != null) {
            WifiKeyStore wifiKeyStore3 = this.mKeyStore;
            if (!wifiKeyStore3.removeEntryFromKeyStore("USRCERT_" + this.mClientCertificateAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mClientCertificateAlias);
            }
            this.mClientCertificateAlias = null;
        }
        if (this.mRemediationCaCertificateAlias != null) {
            WifiKeyStore wifiKeyStore4 = this.mKeyStore;
            if (!wifiKeyStore4.removeEntryFromKeyStore("CACERT_" + this.mRemediationCaCertificateAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mRemediationCaCertificateAlias);
            }
            this.mRemediationCaCertificateAlias = null;
        }
    }

    public PasspointMatch match(Map<Constants.ANQPElementType, ANQPElement> anqpElements, InformationElementUtil.RoamingConsortium roamingConsortium) {
        PasspointMatch providerMatch = matchProviderExceptFor3GPP(anqpElements, roamingConsortium);
        if (this.mConfig.getCredential().getSimCredential() != null && (providerMatch == PasspointMatch.RoamingProvider || providerMatch == PasspointMatch.HomeProvider)) {
            if (ANQPMatcher.matchThreeGPPNetwork((ThreeGPPNetworkElement) anqpElements.get(Constants.ANQPElementType.ANQP3GPPNetwork), this.mImsiParameter, this.mMatchingSIMImsiList)) {
                return providerMatch;
            }
            if (OpBrandingLoader.Vendor.SKT == mOpBranding) {
                Log.e(TAG, "match, anqpElements is not matched to ThreeGPPNetworkElement(for SKT).");
                return PasspointMatch.None;
            }
        }
        if (providerMatch == PasspointMatch.None && ANQPMatcher.matchThreeGPPNetwork((ThreeGPPNetworkElement) anqpElements.get(Constants.ANQPElementType.ANQP3GPPNetwork), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.RoamingProvider;
        }
        int authMatch = ANQPMatcher.matchNAIRealm((NAIRealmElement) anqpElements.get(Constants.ANQPElementType.ANQPNAIRealm), this.mConfig.getCredential().getRealm(), this.mEAPMethodID, this.mAuthParam);
        if (authMatch == -1) {
            return PasspointMatch.None;
        }
        if ((authMatch & 4) == 0) {
            return providerMatch;
        }
        if (providerMatch == PasspointMatch.None) {
            return PasspointMatch.RoamingProvider;
        }
        return providerMatch;
    }

    public WifiConfiguration getWifiConfig() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.FQDN = this.mConfig.getHomeSp().getFqdn();
        if (this.mConfig.getHomeSp().getRoamingConsortiumOis() != null) {
            wifiConfig.roamingConsortiumIds = Arrays.copyOf(this.mConfig.getHomeSp().getRoamingConsortiumOis(), this.mConfig.getHomeSp().getRoamingConsortiumOis().length);
        }
        if (this.mConfig.getUpdateIdentifier() != Integer.MIN_VALUE) {
            wifiConfig.updateIdentifier = Integer.toString(this.mConfig.getUpdateIdentifier());
            if (isMeteredNetwork(this.mConfig)) {
                wifiConfig.meteredOverride = 1;
            }
        }
        wifiConfig.providerFriendlyName = this.mConfig.getHomeSp().getFriendlyName();
        wifiConfig.allowedKeyManagement.set(2);
        wifiConfig.allowedKeyManagement.set(3);
        wifiConfig.allowedProtocols.set(1);
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        enterpriseConfig.setRealm(this.mConfig.getCredential().getRealm());
        if (!"CCT".equals(CONFIG_OP_BRANDING)) {
            enterpriseConfig.setDomainSuffixMatch(this.mConfig.getHomeSp().getFqdn());
        }
        if (this.mConfig.getCredential().getUserCredential() != null) {
            buildEnterpriseConfigForUserCredential(enterpriseConfig, this.mConfig.getCredential().getUserCredential());
            setAnonymousIdentityToNaiRealm(enterpriseConfig, this.mConfig.getCredential().getRealm());
        } else if (this.mConfig.getCredential().getCertCredential() != null) {
            buildEnterpriseConfigForCertCredential(enterpriseConfig);
            setAnonymousIdentityToNaiRealm(enterpriseConfig, this.mConfig.getCredential().getRealm());
        } else {
            buildEnterpriseConfigForSimCredential(enterpriseConfig, this.mConfig.getCredential().getSimCredential());
        }
        wifiConfig.enterpriseConfig = enterpriseConfig;
        wifiConfig.shared = this.mIsShared;
        wifiConfig.semAutoReconnect = this.mConfig.getHomeSp().isAutoReconnectEnabled() ? 1 : 0;
        wifiConfig.semIsVendorSpecificSsid = this.mConfig.getHomeSp().isVendorSpecificSsid();
        return wifiConfig;
    }

    public boolean isSimCredential() {
        return this.mConfig.getCredential().getSimCredential() != null;
    }

    public static PasspointConfiguration convertFromWifiConfig(WifiConfiguration wifiConfig) {
        PasspointConfiguration passpointConfig = new PasspointConfiguration();
        HomeSp homeSp = new HomeSp();
        if (TextUtils.isEmpty(wifiConfig.FQDN)) {
            Log.e(TAG, "Missing FQDN");
            return null;
        }
        homeSp.setFqdn(wifiConfig.FQDN);
        homeSp.setFriendlyName(wifiConfig.providerFriendlyName);
        if (wifiConfig.roamingConsortiumIds != null) {
            homeSp.setRoamingConsortiumOis(Arrays.copyOf(wifiConfig.roamingConsortiumIds, wifiConfig.roamingConsortiumIds.length));
        }
        passpointConfig.setHomeSp(homeSp);
        Credential credential = new Credential();
        credential.setRealm(wifiConfig.enterpriseConfig.getRealm());
        int eapMethod = wifiConfig.enterpriseConfig.getEapMethod();
        if (eapMethod == 1) {
            Credential.CertificateCredential certCred = new Credential.CertificateCredential();
            certCred.setCertType("x509v3");
            credential.setCertCredential(certCred);
        } else if (eapMethod == 2) {
            credential.setUserCredential(buildUserCredentialFromEnterpriseConfig(wifiConfig.enterpriseConfig));
        } else if (eapMethod == 4) {
            credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(18, wifiConfig.enterpriseConfig));
        } else if (eapMethod == 5) {
            credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(23, wifiConfig.enterpriseConfig));
        } else if (eapMethod != 6) {
            Log.e(TAG, "Unsupport EAP method: " + wifiConfig.enterpriseConfig.getEapMethod());
            return null;
        } else {
            credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(50, wifiConfig.enterpriseConfig));
        }
        if (credential.getUserCredential() == null && credential.getCertCredential() == null && credential.getSimCredential() == null) {
            Log.e(TAG, "Missing credential");
            return null;
        }
        passpointConfig.setCredential(credential);
        return passpointConfig;
    }

    public boolean equals(Object thatObject) {
        List<String> list;
        PasspointConfiguration passpointConfiguration;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof PasspointProvider)) {
            return false;
        }
        PasspointProvider that = (PasspointProvider) thatObject;
        if (this.mProviderId != that.mProviderId || ((list = this.mCaCertificateAliases) != null ? !list.equals(that.mCaCertificateAliases) : that.mCaCertificateAliases != null) || !TextUtils.equals(this.mClientCertificateAlias, that.mClientCertificateAlias) || !TextUtils.equals(this.mClientPrivateKeyAlias, that.mClientPrivateKeyAlias) || ((passpointConfiguration = this.mConfig) != null ? !passpointConfiguration.equals(that.mConfig) : that.mConfig != null) || !TextUtils.equals(this.mRemediationCaCertificateAlias, that.mRemediationCaCertificateAlias)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Long.valueOf(this.mProviderId), this.mCaCertificateAliases, this.mClientCertificateAlias, this.mClientPrivateKeyAlias, this.mConfig, this.mRemediationCaCertificateAlias});
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProviderId: ");
        builder.append(this.mProviderId);
        builder.append("\n");
        builder.append("CreatorUID: ");
        builder.append(this.mCreatorUid);
        builder.append("\n");
        if (this.mPackageName != null) {
            builder.append("PackageName: ");
            builder.append(this.mPackageName);
            builder.append("\n");
        }
        builder.append("Configuration Begin ---\n");
        builder.append(this.mConfig);
        builder.append("Configuration End ---\n");
        return builder.toString();
    }

    private static X509Certificate getClientCertificate(X509Certificate[] certChain, byte[] expectedSha256Fingerprint) {
        if (certChain == null) {
            return null;
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            for (X509Certificate certificate : certChain) {
                digester.reset();
                if (Arrays.equals(expectedSha256Fingerprint, digester.digest(certificate.getEncoded()))) {
                    return certificate;
                }
            }
            return null;
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return null;
        }
    }

    private boolean isMeteredNetwork(PasspointConfiguration passpointConfig) {
        if (passpointConfig == null) {
            return false;
        }
        if (passpointConfig.getUsageLimitDataLimit() > 0 || passpointConfig.getUsageLimitTimeLimitInMinutes() > 0) {
            return true;
        }
        return false;
    }

    private PasspointMatch matchProviderExceptFor3GPP(Map<Constants.ANQPElementType, ANQPElement> anqpElements, InformationElementUtil.RoamingConsortium roamingConsortium) {
        if (ANQPMatcher.matchDomainName((DomainNameElement) anqpElements.get(Constants.ANQPElementType.ANQPDomName), this.mConfig.getHomeSp().getFqdn(), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.HomeProvider;
        }
        long[] providerOIs = this.mConfig.getHomeSp().getRoamingConsortiumOis();
        if (ANQPMatcher.matchRoamingConsortium((RoamingConsortiumElement) anqpElements.get(Constants.ANQPElementType.ANQPRoamingConsortium), providerOIs)) {
            return PasspointMatch.RoamingProvider;
        }
        long[] roamingConsortiums = roamingConsortium.getRoamingConsortiums();
        if (!(roamingConsortiums == null || providerOIs == null)) {
            for (long sta_oi : roamingConsortiums) {
                for (long ap_oi : providerOIs) {
                    if (sta_oi == ap_oi) {
                        return PasspointMatch.RoamingProvider;
                    }
                }
            }
        }
        return PasspointMatch.None;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0070, code lost:
        if (r5.equals("PAP") != false) goto L_0x0074;
     */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0097  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void buildEnterpriseConfigForUserCredential(android.net.wifi.WifiEnterpriseConfig r11, android.net.wifi.hotspot2.pps.Credential.UserCredential r12) {
        /*
            r10 = this;
            java.lang.String r0 = "PasspointProvider"
            r1 = 0
            java.lang.String r2 = r12.getPassword()     // Catch:{ IllegalArgumentException -> 0x0014 }
            byte[] r2 = android.util.Base64.decode(r2, r1)     // Catch:{ IllegalArgumentException -> 0x0014 }
            java.lang.String r3 = new java.lang.String     // Catch:{ IllegalArgumentException -> 0x0014 }
            java.nio.charset.Charset r4 = java.nio.charset.StandardCharsets.UTF_8     // Catch:{ IllegalArgumentException -> 0x0014 }
            r3.<init>(r2, r4)     // Catch:{ IllegalArgumentException -> 0x0014 }
            r2 = r3
            goto L_0x001e
        L_0x0014:
            r2 = move-exception
            java.lang.String r3 = "buildEnterpriseConfigForUserCredential, IllegalArgumentException bad base-64 password"
            android.util.Log.e(r0, r3)
            java.lang.String r2 = r12.getPassword()
        L_0x001e:
            r3 = 2
            r11.setEapMethod(r3)
            java.lang.String r4 = r12.getUsername()
            r11.setIdentity(r4)
            r11.setPassword(r2)
            java.util.List<java.lang.String> r4 = r10.mCaCertificateAliases
            if (r4 == 0) goto L_0x003b
            java.lang.String[] r5 = new java.lang.String[r1]
            java.lang.Object[] r4 = r4.toArray(r5)
            java.lang.String[] r4 = (java.lang.String[]) r4
            r11.setCaCertificateAliases(r4)
        L_0x003b:
            r4 = 0
            java.lang.String r5 = r12.getNonEapInnerMethod()
            r6 = -1
            int r7 = r5.hashCode()
            r8 = 78975(0x1347f, float:1.10668E-40)
            r9 = 1
            if (r7 == r8) goto L_0x006a
            r1 = 632512142(0x25b35e8e, float:3.1115623E-16)
            if (r7 == r1) goto L_0x0060
            r1 = 2038151963(0x797bbb1b, float:8.169134E34)
            if (r7 == r1) goto L_0x0056
        L_0x0055:
            goto L_0x0073
        L_0x0056:
            java.lang.String r1 = "MS-CHAP"
            boolean r1 = r5.equals(r1)
            if (r1 == 0) goto L_0x0055
            r1 = r9
            goto L_0x0074
        L_0x0060:
            java.lang.String r1 = "MS-CHAP-V2"
            boolean r1 = r5.equals(r1)
            if (r1 == 0) goto L_0x0055
            r1 = r3
            goto L_0x0074
        L_0x006a:
            java.lang.String r7 = "PAP"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0055
            goto L_0x0074
        L_0x0073:
            r1 = r6
        L_0x0074:
            if (r1 == 0) goto L_0x0097
            if (r1 == r9) goto L_0x0095
            if (r1 == r3) goto L_0x0093
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "Unsupported Auth: "
            r1.append(r3)
            java.lang.String r3 = r12.getNonEapInnerMethod()
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            android.util.Log.wtf(r0, r1)
            goto L_0x0099
        L_0x0093:
            r4 = 3
            goto L_0x0099
        L_0x0095:
            r4 = 2
            goto L_0x0099
        L_0x0097:
            r4 = 1
        L_0x0099:
            r11.setPhase2Method(r4)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.hotspot2.PasspointProvider.buildEnterpriseConfigForUserCredential(android.net.wifi.WifiEnterpriseConfig, android.net.wifi.hotspot2.pps.Credential$UserCredential):void");
    }

    private void buildEnterpriseConfigForCertCredential(WifiEnterpriseConfig config) {
        config.setEapMethod(1);
        config.setClientCertificateAlias(this.mClientCertificateAlias);
        config.setCaCertificateAliases((String[]) this.mCaCertificateAliases.toArray(new String[0]));
    }

    private void buildEnterpriseConfigForSimCredential(WifiEnterpriseConfig config, Credential.SimCredential credential) {
        int eapMethod = -1;
        int eapType = credential.getEapType();
        if (eapType == 18) {
            eapMethod = 4;
        } else if (eapType == 23) {
            eapMethod = 5;
        } else if (eapType != 50) {
            Log.wtf(TAG, "Unsupported EAP Method: " + credential.getEapType());
        } else {
            eapMethod = 6;
        }
        config.setEapMethod(eapMethod);
        config.setPlmn(credential.getImsi());
    }

    private static void setAnonymousIdentityToNaiRealm(WifiEnterpriseConfig config, String realm) {
        config.setAnonymousIdentity("anonymous@" + realm);
    }

    private static Credential.UserCredential buildUserCredentialFromEnterpriseConfig(WifiEnterpriseConfig config) {
        Credential.UserCredential userCredential = new Credential.UserCredential();
        userCredential.setEapType(21);
        if (TextUtils.isEmpty(config.getIdentity())) {
            Log.e(TAG, "Missing username for user credential");
            return null;
        }
        userCredential.setUsername(config.getIdentity());
        if (TextUtils.isEmpty(config.getPassword())) {
            Log.e(TAG, "Missing password for user credential");
            return null;
        }
        userCredential.setPassword(new String(Base64.encode(config.getPassword().getBytes(StandardCharsets.UTF_8), 0), StandardCharsets.UTF_8));
        int phase2Method = config.getPhase2Method();
        if (phase2Method == 1) {
            userCredential.setNonEapInnerMethod("PAP");
        } else if (phase2Method == 2) {
            userCredential.setNonEapInnerMethod("MS-CHAP");
        } else if (phase2Method != 3) {
            Log.e(TAG, "Unsupported phase2 method for TTLS: " + config.getPhase2Method());
            return null;
        } else {
            userCredential.setNonEapInnerMethod("MS-CHAP-V2");
        }
        return userCredential;
    }

    private static Credential.SimCredential buildSimCredentialFromEnterpriseConfig(int eapType, WifiEnterpriseConfig config) {
        Credential.SimCredential simCredential = new Credential.SimCredential();
        if (TextUtils.isEmpty(config.getPlmn())) {
            Log.e(TAG, "Missing IMSI for SIM credential");
            return null;
        }
        simCredential.setImsi(config.getPlmn());
        simCredential.setEapType(eapType);
        return simCredential;
    }
}
