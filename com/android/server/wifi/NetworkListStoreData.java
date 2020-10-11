package com.android.server.wifi;

import android.content.Context;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class NetworkListStoreData implements WifiConfigStore.StoreData {
    private static final String TAG = "NetworkListStoreData";
    private static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    private static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_STATUS = "NetworkStatus";
    private static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION = "WifiEnterpriseConfiguration";
    private List<WifiConfiguration> mConfigurations;
    private final Context mContext;

    NetworkListStoreData(Context context) {
        this.mContext = context;
    }

    public void serializeData(XmlSerializer out) throws XmlPullParserException, IOException {
        serializeNetworkList(out, this.mConfigurations);
    }

    public void deserializeData(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
        if (in != null) {
            this.mConfigurations = parseNetworkList(in, outerTagDepth);
        }
    }

    public void resetData() {
        this.mConfigurations = null;
    }

    public boolean hasNewDataToSerialize() {
        return true;
    }

    public String getName() {
        return XML_TAG_SECTION_HEADER_NETWORK_LIST;
    }

    public void setConfigurations(List<WifiConfiguration> configs) {
        this.mConfigurations = configs;
    }

    public List<WifiConfiguration> getConfigurations() {
        List<WifiConfiguration> list = this.mConfigurations;
        if (list == null) {
            return new ArrayList();
        }
        return list;
    }

    private void serializeNetworkList(XmlSerializer out, List<WifiConfiguration> networkList) throws XmlPullParserException, IOException {
        if (networkList != null) {
            for (WifiConfiguration network : networkList) {
                serializeNetwork(out, network);
            }
        }
    }

    private void serializeNetwork(XmlSerializer out, WifiConfiguration config) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.WifiConfigurationXmlUtil.writeToXmlForConfigStore(out, config);
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        XmlUtil.NetworkSelectionStatusXmlUtil.writeToXml(out, config.getNetworkSelectionStatus());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        XmlUtil.IpConfigurationXmlUtil.writeToXml(out, config.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        if (!(config.enterpriseConfig == null || config.enterpriseConfig.getEapMethod() == -1)) {
            XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
            XmlUtil.WifiEnterpriseConfigXmlUtil.writeToXml(out, config.enterpriseConfig);
            XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
        }
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x002e, code lost:
        if (r3.allowedKeyManagement.get(6) != false) goto L_0x0030;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.List<android.net.wifi.WifiConfiguration> parseNetworkList(org.xmlpull.v1.XmlPullParser r10, int r11) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /*
            r9 = this;
            java.lang.String r0 = "NetworkListStoreData"
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            java.util.HashMap r2 = new java.util.HashMap
            r2.<init>()
        L_0x000c:
            java.lang.String r3 = "Network"
            boolean r3 = com.android.server.wifi.util.XmlUtil.gotoNextSectionWithNameOrEnd(r10, r3, r11)
            if (r3 == 0) goto L_0x0118
            int r3 = r11 + 1
            android.net.wifi.WifiConfiguration r3 = r9.parseNetwork(r10, r3)     // Catch:{ RuntimeException -> 0x0110 }
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            r5 = 1
            boolean r4 = r4.get(r5)     // Catch:{ RuntimeException -> 0x0110 }
            r6 = 64
            java.lang.String r7 = "illegal network detected "
            r8 = 6
            if (r4 != 0) goto L_0x0030
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            boolean r4 = r4.get(r8)     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 == 0) goto L_0x0083
        L_0x0030:
            java.lang.String r4 = r3.preSharedKey     // Catch:{ RuntimeException -> 0x0110 }
            int r4 = r4.length()     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 != 0) goto L_0x004f
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r7)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r5 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r5)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x0110 }
            android.util.Log.e(r0, r4)     // Catch:{ RuntimeException -> 0x0110 }
            goto L_0x000c
        L_0x004f:
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            int r4 = r4.cardinality()     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 <= r5) goto L_0x0083
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            boolean r4 = r4.get(r8)     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 == 0) goto L_0x0083
            java.util.BitSet r4 = new java.util.BitSet     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>(r6)     // Catch:{ RuntimeException -> 0x0110 }
            r3.allowedKeyManagement = r4     // Catch:{ RuntimeException -> 0x0110 }
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            r4.set(r5)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r8 = "illegal network detected, but recovery "
            r4.append(r8)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r8 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r8)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x0110 }
            android.util.Log.e(r0, r4)     // Catch:{ RuntimeException -> 0x0110 }
        L_0x0083:
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            r8 = 0
            boolean r4 = r4.get(r8)     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 == 0) goto L_0x00e5
            java.lang.String[] r4 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            r4 = r4[r8]     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 == 0) goto L_0x00d3
            java.lang.String[] r4 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            r4 = r4[r8]     // Catch:{ RuntimeException -> 0x0110 }
            int r4 = r4.length()     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 != 0) goto L_0x00b4
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r7)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r5 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r5)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x0110 }
            android.util.Log.e(r0, r4)     // Catch:{ RuntimeException -> 0x0110 }
            goto L_0x000c
        L_0x00b4:
            r4 = r5
        L_0x00b5:
            java.lang.String[] r5 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            int r5 = r5.length     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 >= r5) goto L_0x00d2
            java.lang.String[] r5 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            r5 = r5[r4]     // Catch:{ RuntimeException -> 0x0110 }
            if (r5 == 0) goto L_0x00cf
            java.lang.String[] r5 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            r5 = r5[r4]     // Catch:{ RuntimeException -> 0x0110 }
            int r5 = r5.length()     // Catch:{ RuntimeException -> 0x0110 }
            if (r5 != 0) goto L_0x00cf
            java.lang.String[] r5 = r3.wepKeys     // Catch:{ RuntimeException -> 0x0110 }
            r6 = 0
            r5[r4] = r6     // Catch:{ RuntimeException -> 0x0110 }
        L_0x00cf:
            int r4 = r4 + 1
            goto L_0x00b5
        L_0x00d2:
            goto L_0x00e5
        L_0x00d3:
            boolean r4 = r3.requirePMF     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 == 0) goto L_0x00e5
            java.util.BitSet r4 = new java.util.BitSet     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>(r6)     // Catch:{ RuntimeException -> 0x0110 }
            r3.allowedKeyManagement = r4     // Catch:{ RuntimeException -> 0x0110 }
            java.util.BitSet r4 = r3.allowedKeyManagement     // Catch:{ RuntimeException -> 0x0110 }
            r5 = 9
            r4.set(r5)     // Catch:{ RuntimeException -> 0x0110 }
        L_0x00e5:
            java.lang.String r4 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.Object r4 = r2.get(r4)     // Catch:{ RuntimeException -> 0x0110 }
            if (r4 != 0) goto L_0x00f7
            java.lang.String r4 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            r2.put(r4, r3)     // Catch:{ RuntimeException -> 0x0110 }
            goto L_0x0116
        L_0x00f7:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0110 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r5 = "duplicated network detected "
            r4.append(r5)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r5 = r3.configKey()     // Catch:{ RuntimeException -> 0x0110 }
            r4.append(r5)     // Catch:{ RuntimeException -> 0x0110 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x0110 }
            android.util.Log.i(r0, r4)     // Catch:{ RuntimeException -> 0x0110 }
            goto L_0x0116
        L_0x0110:
            r3 = move-exception
            java.lang.String r4 = "Failed to parse network config. Skipping..."
            android.util.Log.e(r0, r4, r3)
        L_0x0116:
            goto L_0x000c
        L_0x0118:
            java.util.Collection r0 = r2.values()
            r1.addAll(r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.NetworkListStoreData.parseNetworkList(org.xmlpull.v1.XmlPullParser, int):java.util.List");
    }

    private WifiConfiguration parseNetwork(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
        Pair<String, WifiConfiguration> parsedConfig = null;
        WifiConfiguration.NetworkSelectionStatus status = null;
        IpConfiguration ipConfiguration = null;
        WifiEnterpriseConfig enterpriseConfig = null;
        String[] headerName = new String[1];
        while (XmlUtil.gotoNextSectionOrEnd(in, headerName, outerTagDepth)) {
            String str = headerName[0];
            char c = 65535;
            switch (str.hashCode()) {
                case -148477024:
                    if (str.equals(XML_TAG_SECTION_HEADER_NETWORK_STATUS)) {
                        c = 1;
                        break;
                    }
                    break;
                case 46473153:
                    if (str.equals(XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION)) {
                        c = 0;
                        break;
                    }
                    break;
                case 325854959:
                    if (str.equals(XML_TAG_SECTION_HEADER_IP_CONFIGURATION)) {
                        c = 2;
                        break;
                    }
                    break;
                case 1285464096:
                    if (str.equals(XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION)) {
                        c = 3;
                        break;
                    }
                    break;
            }
            if (c != 0) {
                if (c != 1) {
                    if (c != 2) {
                        if (c != 3) {
                            throw new XmlPullParserException("Unknown tag under Network: " + headerName[0]);
                        } else if (enterpriseConfig == null) {
                            enterpriseConfig = XmlUtil.WifiEnterpriseConfigXmlUtil.parseFromXml(in, outerTagDepth + 1);
                        } else {
                            throw new XmlPullParserException("Detected duplicate tag for: WifiEnterpriseConfiguration");
                        }
                    } else if (ipConfiguration == null) {
                        ipConfiguration = XmlUtil.IpConfigurationXmlUtil.parseFromXml(in, outerTagDepth + 1);
                    } else {
                        throw new XmlPullParserException("Detected duplicate tag for: IpConfiguration");
                    }
                } else if (status == null) {
                    status = XmlUtil.NetworkSelectionStatusXmlUtil.parseFromXml(in, outerTagDepth + 1);
                } else {
                    throw new XmlPullParserException("Detected duplicate tag for: NetworkStatus");
                }
            } else if (parsedConfig == null) {
                parsedConfig = XmlUtil.WifiConfigurationXmlUtil.parseFromXml(in, outerTagDepth + 1);
            } else {
                throw new XmlPullParserException("Detected duplicate tag for: WifiConfiguration");
            }
        }
        if (parsedConfig == null || parsedConfig.first == null || parsedConfig.second == null) {
            throw new XmlPullParserException("XML parsing of wifi configuration failed");
        }
        String configKeyParsed = (String) parsedConfig.first;
        WifiConfiguration configuration = (WifiConfiguration) parsedConfig.second;
        String configKeyCalculated = configuration.configKey();
        if (configKeyParsed.equals(configKeyCalculated)) {
            String creatorName = this.mContext.getPackageManager().getNameForUid(configuration.creatorUid);
            if (creatorName == null) {
                Log.e(TAG, "Invalid creatorUid for saved network " + configuration.configKey() + ", creatorUid=" + configuration.creatorUid);
                configuration.creatorUid = 1000;
                configuration.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
            } else if (!creatorName.equals(configuration.creatorName)) {
                Log.w(TAG, "Invalid creatorName for saved network " + configuration.configKey() + ", creatorUid=" + configuration.creatorUid + ", creatorName=" + configuration.creatorName);
                configuration.creatorName = creatorName;
            }
            configuration.setNetworkSelectionStatus(status);
            configuration.setIpConfiguration(ipConfiguration);
            if (enterpriseConfig != null) {
                configuration.enterpriseConfig = enterpriseConfig;
            }
            return configuration;
        }
        throw new XmlPullParserException("Configuration key does not match. Retrieved: " + configKeyParsed + ", Calculated: " + configKeyCalculated);
    }
}
