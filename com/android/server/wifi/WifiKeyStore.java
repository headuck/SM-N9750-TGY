package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class WifiKeyStore {
    private static final String TAG = "WifiKeyStore";
    private final KeyStore mKeyStore;
    private boolean mVerboseLoggingEnabled = false;

    WifiKeyStore(KeyStore keyStore) {
        this.mKeyStore = keyStore;
    }

    /* access modifiers changed from: package-private */
    public void enableVerboseLogging(boolean verbose) {
        this.mVerboseLoggingEnabled = verbose;
    }

    private static boolean needsKeyStore(WifiEnterpriseConfig config) {
        return (config.getClientCertificate() == null && config.getCaCertificate() == null) ? false : true;
    }

    private static boolean isHardwareBackedKey(Key key) {
        return KeyChain.isBoundKeyAlgorithm(key.getAlgorithm());
    }

    private static boolean hasHardwareBackedKey(Certificate certificate) {
        return isHardwareBackedKey(certificate.getPublicKey());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:50:0x019b, code lost:
        r2 = putCertInKeyStore(r5, r21.getWapiAsCertificate());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x01b9, code lost:
        r2 = putCertInKeyStore(r6, r21.getWapiUserCertificate());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean installKeys(android.net.wifi.WifiEnterpriseConfig r20, android.net.wifi.WifiEnterpriseConfig r21, java.lang.String r22) {
        /*
            r19 = this;
            r0 = r19
            r1 = r22
            r2 = 1
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "USRPKEY_"
            r3.append(r4)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "USRCERT_"
            r4.append(r5)
            r4.append(r1)
            java.lang.String r4 = r4.toString()
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "WAPIAS_"
            r5.append(r6)
            r5.append(r1)
            java.lang.String r5 = r5.toString()
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "WAPIUSR_"
            r6.append(r7)
            r6.append(r1)
            java.lang.String r6 = r6.toString()
            java.security.cert.X509Certificate[] r7 = r21.getClientCertificateChain()
            r8 = 0
            r9 = 1010(0x3f2, float:1.415E-42)
            if (r7 == 0) goto L_0x00b3
            int r10 = r7.length
            if (r10 == 0) goto L_0x00b3
            java.security.PrivateKey r10 = r21.getClientPrivateKey()
            byte[] r10 = r10.getEncoded()
            boolean r11 = r0.mVerboseLoggingEnabled
            if (r11 == 0) goto L_0x009e
            java.security.PrivateKey r11 = r21.getClientPrivateKey()
            boolean r11 = isHardwareBackedKey(r11)
            java.lang.String r12 = "importing keys "
            java.lang.String r13 = "WifiKeyStore"
            if (r11 == 0) goto L_0x0087
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            r11.append(r12)
            r11.append(r1)
            java.lang.String r12 = " in hardware backed store"
            r11.append(r12)
            java.lang.String r11 = r11.toString()
            android.util.Log.d(r13, r11)
            goto L_0x009e
        L_0x0087:
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            r11.append(r12)
            r11.append(r1)
            java.lang.String r12 = " in software backed store"
            r11.append(r12)
            java.lang.String r11 = r11.toString()
            android.util.Log.d(r13, r11)
        L_0x009e:
            android.security.KeyStore r11 = r0.mKeyStore
            boolean r2 = r11.importKey(r3, r10, r9, r8)
            if (r2 != 0) goto L_0x00a7
            return r2
        L_0x00a7:
            boolean r2 = r0.putCertsInKeyStore(r4, r7)
            if (r2 != 0) goto L_0x00b3
            android.security.KeyStore r8 = r0.mKeyStore
            r8.delete(r3, r9)
            return r2
        L_0x00b3:
            java.security.cert.X509Certificate[] r10 = r21.getCaCertificates()
            android.util.ArraySet r11 = new android.util.ArraySet
            r11.<init>()
            if (r20 == 0) goto L_0x00d0
            java.lang.String[] r12 = r20.getCaCertificateAliases()
            if (r12 == 0) goto L_0x00d0
            java.lang.String[] r12 = r20.getCaCertificateAliases()
            java.util.List r12 = java.util.Arrays.asList(r12)
            r11.addAll(r12)
        L_0x00d0:
            r12 = 0
            java.lang.String r13 = "CACERT_"
            if (r10 == 0) goto L_0x016a
            java.util.ArrayList r14 = new java.util.ArrayList
            r14.<init>()
            r12 = r14
            r14 = 0
        L_0x00dc:
            int r15 = r10.length
            if (r14 >= r15) goto L_0x0165
            int r15 = r10.length
            r9 = 1
            if (r15 != r9) goto L_0x00e5
            r9 = r1
            goto L_0x00f6
        L_0x00e5:
            r15 = 2
            java.lang.Object[] r15 = new java.lang.Object[r15]
            r15[r8] = r1
            java.lang.Integer r16 = java.lang.Integer.valueOf(r14)
            r15[r9] = r16
            java.lang.String r9 = "%s_%d"
            java.lang.String r9 = java.lang.String.format(r9, r15)
        L_0x00f6:
            r11.remove(r9)
            java.lang.StringBuilder r15 = new java.lang.StringBuilder
            r15.<init>()
            r15.append(r13)
            r15.append(r9)
            java.lang.String r15 = r15.toString()
            r8 = r10[r14]
            boolean r2 = r0.putCertInKeyStore(r15, r8)
            if (r2 != 0) goto L_0x0155
            java.security.cert.X509Certificate r8 = r21.getClientCertificate()
            if (r8 == 0) goto L_0x0123
            android.security.KeyStore r8 = r0.mKeyStore
            r15 = 1010(0x3f2, float:1.415E-42)
            r8.delete(r3, r15)
            android.security.KeyStore r8 = r0.mKeyStore
            r8.delete(r4, r15)
        L_0x0123:
            java.util.Iterator r8 = r12.iterator()
        L_0x0127:
            boolean r15 = r8.hasNext()
            if (r15 == 0) goto L_0x0154
            java.lang.Object r15 = r8.next()
            java.lang.String r15 = (java.lang.String) r15
            android.security.KeyStore r1 = r0.mKeyStore
            r17 = r3
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r13)
            r3.append(r15)
            java.lang.String r3 = r3.toString()
            r18 = r4
            r4 = 1010(0x3f2, float:1.415E-42)
            r1.delete(r3, r4)
            r1 = r22
            r3 = r17
            r4 = r18
            goto L_0x0127
        L_0x0154:
            return r2
        L_0x0155:
            r17 = r3
            r18 = r4
            r12.add(r9)
            int r14 = r14 + 1
            r1 = r22
            r8 = 0
            r9 = 1010(0x3f2, float:1.415E-42)
            goto L_0x00dc
        L_0x0165:
            r17 = r3
            r18 = r4
            goto L_0x016e
        L_0x016a:
            r17 = r3
            r18 = r4
        L_0x016e:
            java.util.Iterator r1 = r11.iterator()
        L_0x0172:
            boolean r3 = r1.hasNext()
            if (r3 == 0) goto L_0x0195
            java.lang.Object r3 = r1.next()
            java.lang.String r3 = (java.lang.String) r3
            android.security.KeyStore r4 = r0.mKeyStore
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r13)
            r8.append(r3)
            java.lang.String r8 = r8.toString()
            r9 = 1010(0x3f2, float:1.415E-42)
            r4.delete(r8, r9)
            goto L_0x0172
        L_0x0195:
            java.security.cert.X509Certificate r1 = r21.getWapiAsCertificate()
            if (r1 == 0) goto L_0x01b3
            java.security.cert.X509Certificate r1 = r21.getWapiAsCertificate()
            boolean r2 = r0.putCertInKeyStore(r5, r1)
            if (r2 != 0) goto L_0x01b3
            java.security.cert.X509Certificate r1 = r21.getWapiAsCertificate()
            if (r1 == 0) goto L_0x01b2
            android.security.KeyStore r1 = r0.mKeyStore
            r3 = 1010(0x3f2, float:1.415E-42)
            r1.delete(r5, r3)
        L_0x01b2:
            return r2
        L_0x01b3:
            java.security.cert.X509Certificate r1 = r21.getWapiUserCertificate()
            if (r1 == 0) goto L_0x01d1
            java.security.cert.X509Certificate r1 = r21.getWapiUserCertificate()
            boolean r2 = r0.putCertInKeyStore(r6, r1)
            if (r2 != 0) goto L_0x01d1
            java.security.cert.X509Certificate r1 = r21.getWapiUserCertificate()
            if (r1 == 0) goto L_0x01d0
            android.security.KeyStore r1 = r0.mKeyStore
            r3 = 1010(0x3f2, float:1.415E-42)
            r1.delete(r6, r3)
        L_0x01d0:
            return r2
        L_0x01d1:
            java.security.cert.X509Certificate r1 = r21.getClientCertificate()
            if (r1 == 0) goto L_0x01dd
            r21.setClientCertificateAlias(r22)
            r21.resetClientKeyEntry()
        L_0x01dd:
            if (r10 == 0) goto L_0x01f5
            int r1 = r12.size()
            java.lang.String[] r1 = new java.lang.String[r1]
            java.lang.Object[] r1 = r12.toArray(r1)
            java.lang.String[] r1 = (java.lang.String[]) r1
            r3 = r21
            r3.setCaCertificateAliases(r1)
            r21.resetCaCertificate()
            goto L_0x01f7
        L_0x01f5:
            r3 = r21
        L_0x01f7:
            java.security.cert.X509Certificate r1 = r21.getWapiAsCertificate()
            if (r1 == 0) goto L_0x0203
            r21.setWapiASCertificateAlias(r22)
            r21.resetWapiAsCertificate()
        L_0x0203:
            java.security.cert.X509Certificate r1 = r21.getWapiUserCertificate()
            if (r1 == 0) goto L_0x020f
            r21.setWapiUserCertificateAlias(r22)
            r21.getWapiUserCertificate()
        L_0x020f:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiKeyStore.installKeys(android.net.wifi.WifiEnterpriseConfig, android.net.wifi.WifiEnterpriseConfig, java.lang.String):boolean");
    }

    public boolean putCertInKeyStore(String name, Certificate cert) {
        return putCertsInKeyStore(name, new Certificate[]{cert});
    }

    public boolean putCertsInKeyStore(String name, Certificate[] certs) {
        try {
            byte[] certData = Credentials.convertToPem(certs);
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "putting " + certs.length + " certificate(s) " + name + " in keystore");
            }
            return this.mKeyStore.put(name, certData, 1010, 0);
        } catch (IOException e) {
            return false;
        } catch (CertificateException e2) {
            return false;
        }
    }

    public boolean putKeyInKeyStore(String name, Key key) {
        return this.mKeyStore.importKey(name, key.getEncoded(), 1010, 0);
    }

    public boolean removeEntryFromKeyStore(String name) {
        return this.mKeyStore.delete(name, 1010);
    }

    public void removeKeys(WifiEnterpriseConfig config) {
        String[] aliases;
        if (config.isAppInstalledDeviceKeyAndCert()) {
            String client = config.getClientCertificateAlias();
            if (!TextUtils.isEmpty(client)) {
                if (this.mVerboseLoggingEnabled) {
                    Log.d(TAG, "removing client private key and user cert");
                }
                this.mKeyStore.delete("USRPKEY_" + client, 1010);
                this.mKeyStore.delete("USRCERT_" + client, 1010);
            }
        }
        if (config.isAppInstalledCaCert() && (aliases = config.getCaCertificateAliases()) != null) {
            for (String ca : aliases) {
                if (!TextUtils.isEmpty(ca)) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.d(TAG, "removing CA cert: " + ca);
                    }
                    this.mKeyStore.delete("CACERT_" + ca, 1010);
                }
            }
        }
        String wapiAs = config.getWapiASCertificateAlias();
        if (!TextUtils.isEmpty(wapiAs)) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "removing WAPI AS cert");
            }
            this.mKeyStore.delete("WAPIAS_" + wapiAs, 1010);
        }
        String wapiUser = config.getWapiUserCertificateAlias();
        if (!TextUtils.isEmpty(wapiUser)) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "removing WAPI User cert");
            }
            this.mKeyStore.delete("WAPIUSR_" + wapiUser, 1010);
        }
    }

    private X509Certificate buildCACertificate(byte[] certData) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certData));
        } catch (CertificateException e) {
            return null;
        }
    }

    public boolean updateNetworkKeys(WifiConfiguration config, WifiConfiguration existingConfig) {
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        if (!needsKeyStore(enterpriseConfig)) {
            return true;
        }
        try {
            if (!installKeys(existingConfig != null ? existingConfig.enterpriseConfig : null, enterpriseConfig, config.getKeyIdForCredentials(existingConfig))) {
                Log.e(TAG, config.SSID + ": failed to install keys");
                return false;
            }
            if (config.allowedKeyManagement.get(10)) {
                KeyStore keyStore = this.mKeyStore;
                byte[] certData = keyStore.get("CACERT_" + config.enterpriseConfig.getCaCertificateAlias(), 1010);
                if (certData == null) {
                    Log.e(TAG, "Failed reading CA certificate for Suite-B");
                    return false;
                }
                X509Certificate x509CaCert = buildCACertificate(certData);
                if (x509CaCert != null) {
                    String sigAlgOid = x509CaCert.getSigAlgOID();
                    if (this.mVerboseLoggingEnabled) {
                        Log.d(TAG, "Signature algorithm: " + sigAlgOid);
                    }
                    config.allowedSuiteBCiphers.clear();
                    if (sigAlgOid.equals("1.2.840.113549.1.1.12")) {
                        config.allowedSuiteBCiphers.set(1);
                        if (this.mVerboseLoggingEnabled) {
                            Log.d(TAG, "Selecting Suite-B RSA");
                        }
                    } else if (sigAlgOid.equals("1.2.840.10045.4.3.3")) {
                        config.allowedSuiteBCiphers.set(0);
                        if (this.mVerboseLoggingEnabled) {
                            Log.d(TAG, "Selecting Suite-B ECDSA");
                        }
                    } else {
                        Log.e(TAG, "Invalid CA certificate type for Suite-B: " + sigAlgOid);
                        return false;
                    }
                } else {
                    Log.e(TAG, "Invalid CA certificate for Suite-B");
                    return false;
                }
            }
            return true;
        } catch (IllegalStateException e) {
            Log.e(TAG, config.SSID + " invalid config for key installation: " + e.getMessage());
            return false;
        }
    }
}
