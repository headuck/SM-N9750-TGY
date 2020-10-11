package com.android.server.wifi;

import android.app.ActivityManager;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.tcp.WifiTransportLayerUtils;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.XmlUtil;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WifiBackupRestore {
    private static final String CHARSET_CN = "gbk";
    private static final String CHARSET_KOR = "ksc5601";
    /* access modifiers changed from: private */
    public static final String CONFIG_CHARSET = OpBrandingLoader.getInstance().getSupportCharacterSet();
    private static final float CURRENT_BACKUP_DATA_VERSION = 1.1f;
    private static final int INITIAL_BACKUP_DATA_VERSION = 1;
    private static final int MAX_CONFIG_COUNT_RESTORE = 300;
    private static final String PSK_MASK_LINE_MATCH_PATTERN = "<.*PreSharedKey.*>.*<.*>";
    private static final String PSK_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String PSK_MASK_SEARCH_PATTERN = "(<.*PreSharedKey.*>)(.*)(<.*>)";
    private static final String TAG = "WifiBackupRestore";
    private static final String WEP_KEYS_MASK_LINE_END_MATCH_PATTERN = "</string-array>";
    private static final String WEP_KEYS_MASK_LINE_START_MATCH_PATTERN = "<string-array.*WEPKeys.*num=\"[0-9]\">";
    private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String WEP_KEYS_MASK_SEARCH_PATTERN = "(<.*=)(.*)(/>)";
    private static final String XML_TAG_DOCUMENT_HEADER = "WifiBackupData";
    static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_VERSION = "Version";
    private byte[] mDebugLastBackupDataRestored;
    private byte[] mDebugLastBackupDataRetrieved;
    private byte[] mDebugLastSupplicantBackupDataRestored;
    private final LocalLog mLocalLog;
    private boolean mVerboseLoggingEnabled;
    private final WifiPermissionsUtil mWifiPermissionsUtil;

    public WifiBackupRestore(WifiPermissionsUtil wifiPermissionsUtil) {
        this.mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 128 : 256);
        this.mVerboseLoggingEnabled = false;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
    }

    public byte[] retrieveBackupDataFromConfigurations(List<WifiConfiguration> configurations) {
        if (configurations == null) {
            Log.e(TAG, "Invalid configuration list received");
            return new byte[0];
        }
        try {
            XmlSerializer out = new FastXmlSerializer();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            out.setOutput(outputStream, StandardCharsets.UTF_8.name());
            XmlUtil.writeDocumentStart(out, XML_TAG_DOCUMENT_HEADER);
            XmlUtil.writeNextValue(out, XML_TAG_VERSION, Float.valueOf(CURRENT_BACKUP_DATA_VERSION));
            writeNetworkConfigurationsToXml(out, configurations);
            XmlUtil.writeDocumentEnd(out, XML_TAG_DOCUMENT_HEADER);
            byte[] data = outputStream.toByteArray();
            if (this.mVerboseLoggingEnabled) {
                this.mDebugLastBackupDataRetrieved = data;
            }
            return data;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error retrieving the backup data: " + e);
            return new byte[0];
        } catch (IOException e2) {
            Log.e(TAG, "Error retrieving the backup data: " + e2);
            return new byte[0];
        }
    }

    private void writeNetworkConfigurationsToXml(XmlSerializer out, List<WifiConfiguration> configurations) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK_LIST);
        for (WifiConfiguration configuration : configurations) {
            if (!configuration.isEnterprise() && !configuration.isPasspoint()) {
                if (!this.mWifiPermissionsUtil.checkConfigOverridePermission(configuration.creatorUid)) {
                    Log.d(TAG, "Ignoring network from an app with no config override permission: " + configuration.configKey());
                } else {
                    XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK);
                    writeNetworkConfigurationToXml(out, configuration);
                    XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK);
                }
            }
        }
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK_LIST);
    }

    private void writeNetworkConfigurationToXml(XmlSerializer out, WifiConfiguration configuration) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.WifiConfigurationXmlUtil.writeToXmlForBackup(out, configuration);
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        XmlUtil.IpConfigurationXmlUtil.writeToXmlForBackup(out, configuration.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0069, code lost:
        r5 = 1;
        r6 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0098, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0099, code lost:
        android.util.Log.e(TAG, "Error parsing the backup data: " + r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00ad, code lost:
        return null;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0098 A[ExcHandler: IOException | ClassCastException | IllegalArgumentException | XmlPullParserException (r2v1 'e' java.lang.Exception A[CUSTOM_DECLARE]), Splitter:B:4:0x000a] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<android.net.wifi.WifiConfiguration> retrieveConfigurationsFromBackupData(byte[] r12) {
        /*
            r11 = this;
            java.lang.String r0 = "WifiBackupRestore"
            r1 = 0
            if (r12 == 0) goto L_0x00ae
            int r2 = r12.length
            if (r2 != 0) goto L_0x000a
            goto L_0x00ae
        L_0x000a:
            boolean r2 = r11.mVerboseLoggingEnabled     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            if (r2 == 0) goto L_0x0010
            r11.mDebugLastBackupDataRestored = r12     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
        L_0x0010:
            org.xmlpull.v1.XmlPullParser r2 = android.util.Xml.newPullParser()     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.io.ByteArrayInputStream r3 = new java.io.ByteArrayInputStream     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r3.<init>(r12)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.nio.charset.Charset r4 = java.nio.charset.StandardCharsets.UTF_8     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r4 = r4.name()     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r2.setInput(r3, r4)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r4 = "WifiBackupData"
            com.android.server.wifi.util.XmlUtil.gotoDocumentStart(r2, r4)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            int r4 = r2.getDepth()     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r5 = -1
            r6 = -1
            java.lang.String r7 = "Version"
            java.lang.Object r7 = com.android.server.wifi.util.XmlUtil.readNextValueWithName(r2, r7)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.Float r7 = (java.lang.Float) r7     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            float r7 = r7.floatValue()     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.Float r8 = new java.lang.Float     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r8.<init>(r7)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r8 = r8.toString()     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r9 = 46
            int r9 = r8.indexOf(r9)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r10 = -1
            if (r9 != r10) goto L_0x0052
            int r10 = java.lang.Integer.parseInt(r8)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r5 = r10
            r6 = 0
            goto L_0x0067
        L_0x0052:
            r10 = 0
            java.lang.String r10 = r8.substring(r10, r9)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            int r10 = java.lang.Integer.parseInt(r10)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r5 = r10
            int r10 = r9 + 1
            java.lang.String r10 = r8.substring(r10)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            int r10 = java.lang.Integer.parseInt(r10)     // Catch:{ ClassCastException -> 0x0068, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098, IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r6 = r10
        L_0x0067:
            goto L_0x006b
        L_0x0068:
            r7 = move-exception
            r5 = 1
            r6 = 0
        L_0x006b:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r7.<init>()     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r8 = "Version of backup data - major: "
            r7.append(r8)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r7.append(r5)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r8 = "; minor: "
            r7.append(r8)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            r7.append(r6)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            java.lang.String r7 = r7.toString()     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            android.util.Log.d(r0, r7)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            com.android.server.wifi.WifiBackupDataParser r7 = r11.getWifiBackupDataParser(r5)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            if (r7 != 0) goto L_0x0093
            java.lang.String r8 = "Major version of backup data is unknown to this Android version; not restoring"
            android.util.Log.w(r0, r8)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            return r1
        L_0x0093:
            java.util.List r0 = r7.parseNetworkConfigurationsFromXml(r2, r4, r6)     // Catch:{ IOException | ClassCastException | IllegalArgumentException | XmlPullParserException -> 0x0098 }
            return r0
        L_0x0098:
            r2 = move-exception
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Error parsing the backup data: "
            r3.append(r4)
            r3.append(r2)
            java.lang.String r3 = r3.toString()
            android.util.Log.e(r0, r3)
            return r1
        L_0x00ae:
            java.lang.String r0 = "Invalid backup data received"
            r11.localLog(r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiBackupRestore.retrieveConfigurationsFromBackupData(byte[]):java.util.List");
    }

    private WifiBackupDataParser getWifiBackupDataParser(int majorVersion) {
        if (majorVersion == 1) {
            return new WifiBackupDataV1Parser();
        }
        Log.e(TAG, "Unrecognized majorVersion of backup data: " + majorVersion);
        return null;
    }

    private String createLogFromBackupData(byte[] data) {
        StringBuilder sb = new StringBuilder();
        try {
            boolean wepKeysLine = false;
            String[] split = new String(data, StandardCharsets.UTF_8.name()).split("\n");
            int length = split.length;
            for (int i = 0; i < length; i++) {
                String line = split[i];
                if (line.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                    line = line.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*$3");
                }
                if (line.matches(WEP_KEYS_MASK_LINE_START_MATCH_PATTERN)) {
                    wepKeysLine = true;
                } else if (line.matches(WEP_KEYS_MASK_LINE_END_MATCH_PATTERN)) {
                    wepKeysLine = false;
                } else if (wepKeysLine) {
                    line = line.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*$3");
                }
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public List<WifiConfiguration> retrieveConfigurationsFromSupplicantBackupData(byte[] supplicantData, byte[] ipConfigData) {
        if (supplicantData == null || supplicantData.length == 0) {
            localLog("Invalid supplicant backup data received");
            return null;
        }
        localLog("retrieveConfigurationsFromSupplicantBackupData");
        if (this.mVerboseLoggingEnabled) {
            this.mDebugLastSupplicantBackupDataRestored = supplicantData;
        }
        SupplicantBackupMigration.SupplicantNetworks supplicantNetworks = new SupplicantBackupMigration.SupplicantNetworks();
        supplicantNetworks.readNetworksFromStream(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(supplicantData))));
        List<WifiConfiguration> configurations = supplicantNetworks.retrieveWifiConfigurations();
        if (ipConfigData == null || ipConfigData.length == 0) {
            localLog("ipconfig is empty");
        } else {
            SparseArray<IpConfiguration> networks = IpConfigStore.readIpAndProxyConfigurations(new ByteArrayInputStream(ipConfigData));
            if (networks != null) {
                for (int i = 0; i < networks.size(); i++) {
                    int id = networks.keyAt(i);
                    for (WifiConfiguration configuration : configurations) {
                        if (configuration.configKey().hashCode() == id) {
                            configuration.setIpConfiguration(networks.valueAt(i));
                        }
                    }
                }
            } else {
                localLog("Failed to parse ipconfig data");
            }
        }
        return configurations;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        if (!this.mVerboseLoggingEnabled) {
            this.mDebugLastBackupDataRetrieved = null;
            this.mDebugLastBackupDataRestored = null;
            this.mDebugLastSupplicantBackupDataRestored = null;
        }
    }

    private void localLog(String s) {
        Log.d(TAG, s);
        LocalLog localLog = this.mLocalLog;
        if (localLog != null) {
            localLog.log(s);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiBackupRestore");
        if (this.mDebugLastBackupDataRetrieved != null) {
            pw.println("Last backup data retrieved: " + createLogFromBackupData(this.mDebugLastBackupDataRetrieved));
        }
        if (this.mDebugLastBackupDataRestored != null) {
            pw.println("Last backup data restored: " + createLogFromBackupData(this.mDebugLastBackupDataRestored));
        }
        if (this.mDebugLastSupplicantBackupDataRestored != null) {
            pw.println("Last old backup data restored: " + SupplicantBackupMigration.createLogFromBackupData(this.mDebugLastSupplicantBackupDataRestored));
        }
        this.mLocalLog.dump(fd, pw, args);
        SupplicantBackupMigration.dump(fd, pw, args);
    }

    public static class SupplicantBackupMigration {
        private static final String PSK_MASK_LINE_MATCH_PATTERN = ".*psk.*=.*";
        private static final String PSK_MASK_REPLACE_PATTERN = "$1*";
        private static final String PSK_MASK_SEARCH_PATTERN = "(.*psk.*=)(.*)";
        public static final String SUPPLICANT_KEY_AUTH_ALG = "auth_alg";
        public static final String SUPPLICANT_KEY_AUTO_RECONNECT = "autojoin";
        public static final String SUPPLICANT_KEY_CA_CERT = "ca_cert";
        public static final String SUPPLICANT_KEY_CA_PATH = "ca_path";
        public static final String SUPPLICANT_KEY_CLIENT_CERT = "client_cert";
        public static final String SUPPLICANT_KEY_EAP = "eap";
        public static final String SUPPLICANT_KEY_HIDDEN = "scan_ssid";
        public static final String SUPPLICANT_KEY_ID_STR = "id_str";
        public static final String SUPPLICANT_KEY_KEY_MGMT = "key_mgmt";
        public static final String SUPPLICANT_KEY_PSK = "psk";
        public static final String SUPPLICANT_KEY_SSID = "ssid";
        public static final String SUPPLICANT_KEY_WEP_KEY0 = WifiConfiguration.wepKeyVarNames[0];
        public static final String SUPPLICANT_KEY_WEP_KEY1 = WifiConfiguration.wepKeyVarNames[1];
        public static final String SUPPLICANT_KEY_WEP_KEY2 = WifiConfiguration.wepKeyVarNames[2];
        public static final String SUPPLICANT_KEY_WEP_KEY3 = WifiConfiguration.wepKeyVarNames[3];
        public static final String SUPPLICANT_KEY_WEP_KEY_IDX = "wep_tx_keyidx";
        private static final String WEP_KEYS_MASK_LINE_MATCH_PATTERN = (".*" + SUPPLICANT_KEY_WEP_KEY0.replace("0", "") + ".*=.*");
        private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*";
        private static final String WEP_KEYS_MASK_SEARCH_PATTERN = ("(.*" + SUPPLICANT_KEY_WEP_KEY0.replace("0", "") + ".*=)(.*)");
        private static final LocalLog mLocalLog = new LocalLog(ActivityManager.isLowRamDeviceStatic() ? 128 : 256);

        public static String createLogFromBackupData(byte[] data) {
            StringBuilder sb = new StringBuilder();
            try {
                String[] split = new String(data, StandardCharsets.UTF_8.name()).split("\n");
                int length = split.length;
                for (int i = 0; i < length; i++) {
                    String line = split[i];
                    if (line.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                        line = line.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*");
                    }
                    if (line.matches(WEP_KEYS_MASK_LINE_MATCH_PATTERN)) {
                        line = line.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*");
                    }
                    sb.append(line);
                    sb.append("\n");
                }
                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        /* access modifiers changed from: private */
        public static void localLog(String s) {
            Log.d(WifiBackupRestore.TAG, s);
            LocalLog localLog = mLocalLog;
            if (localLog != null) {
                localLog.log(s);
            }
        }

        public static void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mLocalLog.dump(fd, pw, args);
        }

        static class SupplicantNetwork {
            public boolean certUsed = false;
            public boolean isEap = false;
            private String mParsedAuthAlgLine;
            private String mParsedAutoReconnectLine;
            private String mParsedHiddenLine;
            private String mParsedIdStrLine;
            /* access modifiers changed from: private */
            public String mParsedKeyMgmtLine;
            private String mParsedPskLine;
            /* access modifiers changed from: private */
            public String mParsedSSIDLine;
            private String[] mParsedWepKeyLines = new String[4];
            private String mParsedWepTxKeyIdxLine;

            SupplicantNetwork() {
            }

            public static SupplicantNetwork readNetworkFromStream(BufferedReader in) {
                String line;
                SupplicantNetwork n = new SupplicantNetwork();
                while (true) {
                    try {
                        if (!in.ready() || (line = in.readLine()) == null) {
                            break;
                        } else if (line.startsWith("}")) {
                            break;
                        } else {
                            n.parseLine(line);
                        }
                    } catch (IOException e) {
                        return null;
                    }
                }
                return n;
            }

            /* access modifiers changed from: package-private */
            public void parseLine(String line) {
                String line2 = line.trim();
                if (!line2.isEmpty()) {
                    if (line2.startsWith("ssid=")) {
                        this.mParsedSSIDLine = line2;
                    } else if (line2.startsWith("scan_ssid=")) {
                        this.mParsedHiddenLine = line2;
                    } else if (line2.startsWith("key_mgmt=")) {
                        this.mParsedKeyMgmtLine = line2;
                        if (line2.contains("EAP")) {
                            this.isEap = true;
                        }
                    } else if (line2.startsWith("client_cert=")) {
                        this.certUsed = true;
                    } else if (line2.startsWith("ca_cert=")) {
                        this.certUsed = true;
                    } else if (line2.startsWith("ca_path=")) {
                        this.certUsed = true;
                    } else if (line2.startsWith("eap=")) {
                        this.isEap = true;
                    } else if (line2.startsWith("psk=")) {
                        this.mParsedPskLine = line2;
                    } else {
                        if (line2.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY0 + "=")) {
                            this.mParsedWepKeyLines[0] = line2;
                            return;
                        }
                        if (line2.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY1 + "=")) {
                            this.mParsedWepKeyLines[1] = line2;
                            return;
                        }
                        if (line2.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY2 + "=")) {
                            this.mParsedWepKeyLines[2] = line2;
                            return;
                        }
                        if (line2.startsWith(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY3 + "=")) {
                            this.mParsedWepKeyLines[3] = line2;
                        } else if (line2.startsWith("wep_tx_keyidx=")) {
                            this.mParsedWepTxKeyIdxLine = line2;
                        } else if (line2.startsWith("id_str=")) {
                            this.mParsedIdStrLine = line2;
                        } else if (line2.startsWith("autojoin=")) {
                            this.mParsedAutoReconnectLine = line2;
                        } else if (line2.startsWith("auth_alg=")) {
                            this.mParsedAuthAlgLine = line2;
                        }
                    }
                }
            }

            /* access modifiers changed from: package-private */
            public String decodingSsid(String SSID) {
                String decodedSsid;
                byte[] byteArray = NativeUtil.hexStringToByteArray(SSID);
                String decodedSsid2 = null;
                if (WifiBackupRestore.CHARSET_CN.equals(WifiBackupRestore.CONFIG_CHARSET) || WifiBackupRestore.CHARSET_KOR.equals(WifiBackupRestore.CONFIG_CHARSET)) {
                    if (!NativeUtil.isUTF8String(byteArray, (long) byteArray.length) && NativeUtil.isUCNVString(byteArray, byteArray.length)) {
                        try {
                            decodedSsid2 = WifiBackupRestore.CHARSET_CN.equals(WifiBackupRestore.CONFIG_CHARSET) ? new String(byteArray, WifiBackupRestore.CHARSET_CN) : new String(byteArray, WifiBackupRestore.CHARSET_KOR);
                        } catch (Exception e) {
                            Log.e(WifiBackupRestore.TAG, " loadWifiConfiguration, byteArray decode error  = " + e.toString());
                        }
                    }
                    if (decodedSsid2 != null) {
                        decodedSsid = NativeUtil.addEnclosingQuotes(decodedSsid2);
                    } else {
                        decodedSsid = NativeUtil.encodeSsid(NativeUtil.byteArrayToArrayList(byteArray));
                    }
                } else {
                    decodedSsid = NativeUtil.encodeSsid(NativeUtil.byteArrayToArrayList(byteArray));
                }
                SupplicantBackupMigration.localLog("decoded SSID = " + decodedSsid);
                return decodedSsid;
            }

            public WifiConfiguration createWifiConfiguration() {
                String idString;
                String decodedSsid;
                if (this.mParsedSSIDLine == null) {
                    return null;
                }
                WifiConfiguration configuration = new WifiConfiguration();
                String str = this.mParsedSSIDLine;
                configuration.SSID = str.substring(str.indexOf(61) + 1);
                int length = 0;
                if (configuration.SSID != null) {
                    length = configuration.SSID.length();
                }
                if (!(length <= 2 || configuration.SSID.charAt(0) == '\"' || configuration.SSID.charAt(length - 1) == '\"' || (decodedSsid = decodingSsid(configuration.SSID)) == null)) {
                    configuration.SSID = decodedSsid;
                }
                String decodedSsid2 = this.mParsedHiddenLine;
                if (decodedSsid2 != null) {
                    configuration.hiddenSSID = Integer.parseInt(decodedSsid2.substring(decodedSsid2.indexOf(61) + 1)) != 0;
                }
                Log.v(WifiBackupRestore.TAG, "Parsed key_mgmt line: " + this.mParsedKeyMgmtLine);
                String str2 = this.mParsedKeyMgmtLine;
                if (str2 == null) {
                    configuration.allowedKeyManagement.set(1);
                    configuration.allowedKeyManagement.set(2);
                } else {
                    String[] typeStrings = str2.substring(str2.indexOf(61) + 1).split("\\s+");
                    int i = 0;
                    while (i < typeStrings.length) {
                        String ktype = typeStrings[i];
                        if (configuration.allowedKeyManagement.cardinality() != 1 || configuration.allowedKeyManagement.get(2) || configuration.allowedKeyManagement.get(3) || configuration.allowedKeyManagement.get(7)) {
                            if (ktype.equals(WifiTransportLayerUtils.CATEGORY_PLAYSTORE_NONE)) {
                                configuration.allowedKeyManagement.set(0);
                            } else if (ktype.equals("WPA-EAP")) {
                                configuration.allowedKeyManagement.set(2);
                            } else if (ktype.equals("IEEE8021X")) {
                                configuration.allowedKeyManagement.set(3);
                            } else if (ktype.equals("WPA-PSK")) {
                                configuration.allowedKeyManagement.set(1);
                            } else if (ktype.equals("FT-PSK")) {
                                configuration.allowedKeyManagement.set(1);
                            } else if (ktype.equals("FT-EAP")) {
                                configuration.allowedKeyManagement.set(2);
                            } else if (ktype.equals("WAPI-PSK")) {
                                configuration.allowedKeyManagement.set(22);
                            } else if (ktype.equals("WAPI-CERT")) {
                                configuration.allowedKeyManagement.set(23);
                            } else if (ktype.equals("CCKM")) {
                                configuration.allowedKeyManagement.set(2);
                            } else if (ktype.equals("SAE")) {
                                configuration.allowedKeyManagement.set(8);
                                configuration.requirePMF = true;
                            } else if (ktype.equals("OWE")) {
                                configuration.allowedKeyManagement.set(9);
                                configuration.requirePMF = true;
                            }
                            i++;
                        } else {
                            Log.e(WifiBackupRestore.TAG, "There are many secure types - skip");
                            return null;
                        }
                    }
                }
                String str3 = this.mParsedPskLine;
                if (str3 != null) {
                    configuration.preSharedKey = str3.substring(str3.indexOf(61) + 1);
                } else if (configuration.allowedKeyManagement.get(1) || configuration.allowedKeyManagement.get(22) || configuration.allowedKeyManagement.get(8)) {
                    Log.e(WifiBackupRestore.TAG, "error parsing network passphrase, ignoring network.");
                    return null;
                }
                if (this.mParsedWepKeyLines[0] != null) {
                    String[] strArr = configuration.wepKeys;
                    String[] strArr2 = this.mParsedWepKeyLines;
                    strArr[0] = strArr2[0].substring(strArr2[0].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[1] != null) {
                    String[] strArr3 = configuration.wepKeys;
                    String[] strArr4 = this.mParsedWepKeyLines;
                    strArr3[1] = strArr4[1].substring(strArr4[1].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[2] != null) {
                    String[] strArr5 = configuration.wepKeys;
                    String[] strArr6 = this.mParsedWepKeyLines;
                    strArr5[2] = strArr6[2].substring(strArr6[2].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[3] != null) {
                    String[] strArr7 = configuration.wepKeys;
                    String[] strArr8 = this.mParsedWepKeyLines;
                    strArr7[3] = strArr8[3].substring(strArr8[3].indexOf(61) + 1);
                }
                String str4 = this.mParsedWepTxKeyIdxLine;
                if (str4 != null) {
                    configuration.wepTxKeyIndex = Integer.valueOf(str4.substring(str4.indexOf(61) + 1)).intValue();
                }
                String str5 = this.mParsedIdStrLine;
                if (!(str5 == null || (idString = str5.substring(str5.indexOf(61) + 1)) == null)) {
                    Map<String, String> extras = SupplicantStaNetworkHal.parseNetworkExtra(NativeUtil.removeEnclosingQuotes(idString));
                    if (extras == null) {
                        Log.e(WifiBackupRestore.TAG, "Error parsing network extras, ignoring network.");
                        return null;
                    }
                    String configKey = extras.get("configKey");
                    if (configKey == null) {
                        Log.e(WifiBackupRestore.TAG, "Configuration key was not passed, ignoring network.");
                        return null;
                    }
                    if (!configKey.equals(configuration.configKey())) {
                        Log.w(WifiBackupRestore.TAG, "Configuration key does not match. Retrieved: " + configKey + ", Calculated: " + configuration.configKey());
                    }
                    if (Integer.parseInt(extras.get(SupplicantStaNetworkHal.ID_STRING_KEY_CREATOR_UID)) >= 10000) {
                        Log.d(WifiBackupRestore.TAG, "Ignoring network from non-system app: " + configuration.configKey());
                        return null;
                    }
                }
                String str6 = this.mParsedAutoReconnectLine;
                if (str6 != null) {
                    configuration.semAutoReconnect = Integer.parseInt(str6.substring(str6.indexOf(61) + 1));
                }
                String str7 = this.mParsedAuthAlgLine;
                if (str7 != null) {
                    if (str7.contains("OPEN")) {
                        configuration.allowedAuthAlgorithms.set(0);
                    }
                    if (this.mParsedAuthAlgLine.contains("SHARED")) {
                        configuration.allowedAuthAlgorithms.set(1);
                    }
                }
                return configuration;
            }
        }

        static class SupplicantNetworks {
            final ArrayList<SupplicantNetwork> mNetworks = new ArrayList<>(8);

            SupplicantNetworks() {
            }

            public void readNetworksFromStream(BufferedReader in) {
                while (in.ready()) {
                    try {
                        String line = in.readLine();
                        if (line != null && line.startsWith("network")) {
                            SupplicantNetwork net = SupplicantNetwork.readNetworkFromStream(in);
                            if (net == null) {
                                Log.e(WifiBackupRestore.TAG, "Error while parsing the network.");
                            } else {
                                if (!net.isEap) {
                                    if (!net.certUsed) {
                                        this.mNetworks.add(net);
                                    }
                                }
                                Log.d(WifiBackupRestore.TAG, "Skipping enterprise network for restore: " + net.mParsedSSIDLine + " / " + net.mParsedKeyMgmtLine);
                            }
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            }

            public List<WifiConfiguration> retrieveWifiConfigurations() {
                ArrayList<WifiConfiguration> wifiConfigurations = new ArrayList<>();
                int maxCount = 0;
                Iterator<SupplicantNetwork> it = this.mNetworks.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    try {
                        WifiConfiguration wifiConfiguration = it.next().createWifiConfiguration();
                        if (wifiConfiguration != null) {
                            Log.v(WifiBackupRestore.TAG, "Parsed Configuration: " + wifiConfiguration.configKey());
                            wifiConfigurations.add(wifiConfiguration);
                        }
                        maxCount++;
                        if (maxCount >= 300) {
                            Log.e(WifiBackupRestore.TAG, "Stop the restore because the number of restore networks over 300");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(WifiBackupRestore.TAG, "Error parsing wifi configuration: " + e);
                        return null;
                    }
                }
                return wifiConfigurations;
            }
        }
    }
}
