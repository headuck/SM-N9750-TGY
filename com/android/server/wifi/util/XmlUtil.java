package com.android.server.wifi.util;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.XmlUtils;
import com.android.server.wifi.WifiBackupRestore;
import com.samsung.android.server.wifi.WifiPasswordManager;
import com.samsung.android.server.wifi.share.McfDataUtil;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtil {
    private static final String TAG = "WifiXmlUtil";

    private static void gotoStartTag(XmlPullParser in) throws XmlPullParserException, IOException {
        int type = in.getEventType();
        while (type != 2 && type != 1) {
            type = in.next();
        }
    }

    private static void gotoEndTag(XmlPullParser in) throws XmlPullParserException, IOException {
        int type = in.getEventType();
        while (type != 3 && type != 1) {
            type = in.next();
        }
    }

    public static void gotoDocumentStart(XmlPullParser in, String headerName) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(in, headerName);
    }

    public static boolean gotoNextSectionOrEnd(XmlPullParser in, String[] headerName, int outerDepth) throws XmlPullParserException, IOException {
        if (!XmlUtils.nextElementWithin(in, outerDepth)) {
            return false;
        }
        headerName[0] = in.getName();
        return true;
    }

    public static boolean gotoNextSectionWithNameOrEnd(XmlPullParser in, String expectedName, int outerDepth) throws XmlPullParserException, IOException {
        String[] headerName = new String[1];
        if (!gotoNextSectionOrEnd(in, headerName, outerDepth)) {
            return false;
        }
        if (headerName[0].equals(expectedName)) {
            return true;
        }
        throw new XmlPullParserException("Next section name does not match expected name: " + expectedName);
    }

    public static void gotoNextSectionWithName(XmlPullParser in, String expectedName, int outerDepth) throws XmlPullParserException, IOException {
        if (!gotoNextSectionWithNameOrEnd(in, expectedName, outerDepth)) {
            throw new XmlPullParserException("Section not found. Expected: " + expectedName);
        }
    }

    public static boolean isNextSectionEnd(XmlPullParser in, int sectionDepth) throws XmlPullParserException, IOException {
        return !XmlUtils.nextElementWithin(in, sectionDepth);
    }

    public static Object readCurrentValue(XmlPullParser in, String[] valueName) throws XmlPullParserException, IOException {
        Object value = XmlUtils.readValueXml(in, valueName);
        gotoEndTag(in);
        return value;
    }

    public static Object readNextValueWithName(XmlPullParser in, String expectedName) throws XmlPullParserException, IOException {
        String[] valueName = new String[1];
        XmlUtils.nextElement(in);
        Object value = readCurrentValue(in, valueName);
        if (valueName[0].equals(expectedName)) {
            return value;
        }
        throw new XmlPullParserException("Value not found. Expected: " + expectedName + ", but got: " + valueName[0]);
    }

    public static void writeDocumentStart(XmlSerializer out, String headerName) throws IOException {
        out.startDocument((String) null, true);
        out.startTag((String) null, headerName);
    }

    public static void writeDocumentEnd(XmlSerializer out, String headerName) throws IOException {
        out.endTag((String) null, headerName);
        out.endDocument();
    }

    public static void writeNextSectionStart(XmlSerializer out, String headerName) throws IOException {
        out.startTag((String) null, headerName);
    }

    public static void writeNextSectionEnd(XmlSerializer out, String headerName) throws IOException {
        out.endTag((String) null, headerName);
    }

    public static void writeNextValue(XmlSerializer out, String name, Object value) throws XmlPullParserException, IOException {
        XmlUtils.writeValueXml(value, name, out);
    }

    public static class WifiConfigurationXmlUtil {
        public static final String XML_TAG_ALLOWED_AUTH_ALGOS = "AllowedAuthAlgos";
        public static final String XML_TAG_ALLOWED_GROUP_CIPHERS = "AllowedGroupCiphers";
        public static final String XML_TAG_ALLOWED_GROUP_MGMT_CIPHERS = "AllowedGroupMgmtCiphers";
        public static final String XML_TAG_ALLOWED_KEY_MGMT = "AllowedKeyMgmt";
        public static final String XML_TAG_ALLOWED_PAIRWISE_CIPHERS = "AllowedPairwiseCiphers";
        public static final String XML_TAG_ALLOWED_PROTOCOLS = "AllowedProtocols";
        public static final String XML_TAG_ALLOWED_SUITE_B_CIPHERS = "AllowedSuiteBCiphers";
        public static final String XML_TAG_AUTHENTICATED = "Authenticated";
        public static final String XML_TAG_AUTO_RECONNECT = "AutoReconnect";
        public static final String XML_TAG_AUTO_WIFI_SCORE_KEY = "SEM_AUTO_WIFI_SCORE";
        public static final String XML_TAG_BSSID = "BSSID";
        public static final String XML_TAG_BSSID_WHITELIST_KEY = "BssidWhitelist";
        public static final String XML_TAG_BSSID_WHITELIST_UPDATE_TIME_KEY = "BssidWhitelistUpdateTime";
        public static final String XML_TAG_CAPTIVE_PORTAL = "CaptivePortal";
        public static final String XML_TAG_CONFIG_KEY = "ConfigKey";
        public static final String XML_TAG_CREATION_TIME = "CreationTime";
        public static final String XML_TAG_CREATOR_NAME = "CreatorName";
        public static final String XML_TAG_CREATOR_UID = "CreatorUid";
        public static final String XML_TAG_DEFAULT_GW_MAC_ADDRESS = "DefaultGwMacAddress";
        public static final String XML_TAG_ENTRY_RSSI_24GHZ = "EntryRssi24GHz";
        public static final String XML_TAG_ENTRY_RSSI_5GHZ = "EntryRssi5GHz";
        public static final String XML_TAG_FQDN = "FQDN";
        public static final String XML_TAG_HIDDEN_SSID = "HiddenSSID";
        public static final String XML_TAG_IS_LEGACY_PASSPOINT_CONFIG = "IsLegacyPasspointConfig";
        public static final String XML_TAG_IS_RECOMMENDED = "semIsRecommended";
        public static final String XML_TAG_LAST_CONNECTED_TIME_KEY = "LastConnectedTime";
        public static final String XML_TAG_LAST_CONNECT_UID = "LastConnectUid";
        public static final String XML_TAG_LAST_UPDATE_NAME = "LastUpdateName";
        public static final String XML_TAG_LAST_UPDATE_UID = "LastUpdateUid";
        public static final String XML_TAG_LINKED_NETWORKS_LIST = "LinkedNetworksList";
        public static final String XML_TAG_LOGIN_URL = "LoginUrl";
        public static final String XML_TAG_MAC_RANDOMIZATION_SETTING = "MacRandomizationSetting";
        public static final String XML_TAG_METERED_HINT = "MeteredHint";
        public static final String XML_TAG_METERED_OVERRIDE = "MeteredOverride";
        public static final String XML_TAG_NEXT_TARGET_RSSI = "NextTargetRssi";
        public static final String XML_TAG_NO_INTERNET_ACCESS_EXPECTED = "NoInternetAccessExpected";
        public static final String XML_TAG_NUM_ASSOCIATION = "NumAssociation";
        public static final String XML_TAG_PRE_SHARED_KEY = "PreSharedKey";
        public static final String XML_TAG_PROVIDER_FRIENDLY_NAME = "ProviderFriendlyName";
        public static final String XML_TAG_RANDOMIZED_MAC_ADDRESS = "RandomizedMacAddress";
        public static final String XML_TAG_REQUIRE_PMF = "RequirePMF";
        public static final String XML_TAG_ROAMING_CONSORTIUM_OIS = "RoamingConsortiumOIs";
        public static final String XML_TAG_SAMSUNG_SPECIFIC_FLAGS = "SamsungSpecificFlags";
        public static final String XML_TAG_SEM_CREATION_TIME_KEY = "semCreationTime";
        public static final String XML_TAG_SEM_LOCATION_LATI = "SemLocationLatitude";
        public static final String XML_TAG_SEM_LOCATION_LONG = "SemLocationLongitude";
        public static final String XML_TAG_SEM_UPDATE_TIME_KEY = "semUpdateTime";
        public static final String XML_TAG_SHARED = "Shared";
        public static final String XML_TAG_SKIP_INTERNET_CHECK = "SkipInternetCheck";
        public static final String XML_TAG_SSID = "SSID";
        public static final String XML_TAG_STATUS = "Status";
        public static final String XML_TAG_USABLE_INTERNET = "UsableInternet";
        public static final String XML_TAG_USER_APPROVED = "UserApproved";
        public static final String XML_TAG_USE_EXTERNAL_SCORES = "UseExternalScores";
        public static final String XML_TAG_VALIDATED_INTERNET_ACCESS = "ValidatedInternetAccess";
        public static final String XML_TAG_VENDOR_SSID = "isVendorSpecificSsid";
        public static final String XML_TAG_WAPI_AS_CERT = "WapiAsCert";
        public static final String XML_TAG_WAPI_CERT_INDEX = "WapiCertIndex";
        public static final String XML_TAG_WAPI_PSK_KEY_TYPE = "WapiPskKeyType";
        public static final String XML_TAG_WAPI_USER_CERT = "WapiUserCert";
        public static final String XML_TAG_WEP_KEYS = "WEPKeys";
        public static final String XML_TAG_WEP_TX_KEY_INDEX = "WEPTxKeyIndex";

        private static void writeWepKeysToXml(XmlSerializer out, String[] wepKeys, boolean withEncryption) throws XmlPullParserException, IOException {
            String[] wepKeysToWrite = new String[wepKeys.length];
            boolean hasWepKey = false;
            for (int i = 0; i < wepKeys.length; i++) {
                if (wepKeys[i] == null) {
                    wepKeysToWrite[i] = new String();
                } else {
                    if (withEncryption) {
                        wepKeysToWrite[i] = WifiPasswordManager.getInstance().encrypt(wepKeys[i]);
                    } else {
                        wepKeysToWrite[i] = wepKeys[i];
                    }
                    hasWepKey = true;
                }
            }
            if (hasWepKey) {
                XmlUtil.writeNextValue(out, XML_TAG_WEP_KEYS, wepKeysToWrite);
            } else {
                XmlUtil.writeNextValue(out, XML_TAG_WEP_KEYS, (Object) null);
            }
        }

        public static void writeCommonElementsToXml(XmlSerializer out, WifiConfiguration configuration, boolean withEncryption) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(out, XML_TAG_CONFIG_KEY, configuration.configKey());
            XmlUtil.writeNextValue(out, XML_TAG_SSID, configuration.SSID);
            XmlUtil.writeNextValue(out, XML_TAG_BSSID, configuration.BSSID);
            if (withEncryption) {
                XmlUtil.writeNextValue(out, XML_TAG_PRE_SHARED_KEY, WifiPasswordManager.getInstance().encrypt(configuration.preSharedKey));
            } else {
                XmlUtil.writeNextValue(out, XML_TAG_PRE_SHARED_KEY, configuration.preSharedKey);
            }
            writeWepKeysToXml(out, configuration.wepKeys, withEncryption);
            XmlUtil.writeNextValue(out, XML_TAG_WEP_TX_KEY_INDEX, Integer.valueOf(configuration.wepTxKeyIndex));
            XmlUtil.writeNextValue(out, XML_TAG_HIDDEN_SSID, Boolean.valueOf(configuration.hiddenSSID));
            XmlUtil.writeNextValue(out, XML_TAG_REQUIRE_PMF, Boolean.valueOf(configuration.requirePMF));
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_KEY_MGMT, configuration.allowedKeyManagement.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_PROTOCOLS, configuration.allowedProtocols.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_AUTH_ALGOS, configuration.allowedAuthAlgorithms.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_GROUP_CIPHERS, configuration.allowedGroupCiphers.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_PAIRWISE_CIPHERS, configuration.allowedPairwiseCiphers.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_GROUP_MGMT_CIPHERS, configuration.allowedGroupManagementCiphers.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_ALLOWED_SUITE_B_CIPHERS, configuration.allowedSuiteBCiphers.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_SHARED, Boolean.valueOf(configuration.shared));
        }

        public static void writeToXmlForBackup(XmlSerializer out, WifiConfiguration configuration) throws XmlPullParserException, IOException {
            writeCommonElementsToXml(out, configuration, false);
            XmlUtil.writeNextValue(out, XML_TAG_METERED_OVERRIDE, Integer.valueOf(configuration.meteredOverride));
        }

        public static void writeToXmlForConfigStore(XmlSerializer out, WifiConfiguration configuration) throws XmlPullParserException, IOException {
            writeCommonElementsToXml(out, configuration, WifiPasswordManager.getInstance().isSupported());
            XmlUtil.writeNextValue(out, XML_TAG_STATUS, Integer.valueOf(configuration.status));
            XmlUtil.writeNextValue(out, XML_TAG_FQDN, configuration.FQDN);
            XmlUtil.writeNextValue(out, XML_TAG_PROVIDER_FRIENDLY_NAME, configuration.providerFriendlyName);
            XmlUtil.writeNextValue(out, XML_TAG_LINKED_NETWORKS_LIST, configuration.linkedConfigurations);
            XmlUtil.writeNextValue(out, XML_TAG_DEFAULT_GW_MAC_ADDRESS, configuration.defaultGwMacAddress);
            XmlUtil.writeNextValue(out, XML_TAG_VALIDATED_INTERNET_ACCESS, Boolean.valueOf(configuration.validatedInternetAccess));
            XmlUtil.writeNextValue(out, XML_TAG_NO_INTERNET_ACCESS_EXPECTED, Boolean.valueOf(configuration.noInternetAccessExpected));
            XmlUtil.writeNextValue(out, XML_TAG_USER_APPROVED, Integer.valueOf(configuration.userApproved));
            XmlUtil.writeNextValue(out, XML_TAG_METERED_HINT, Boolean.valueOf(configuration.meteredHint));
            XmlUtil.writeNextValue(out, XML_TAG_METERED_OVERRIDE, Integer.valueOf(configuration.meteredOverride));
            XmlUtil.writeNextValue(out, XML_TAG_USE_EXTERNAL_SCORES, Boolean.valueOf(configuration.useExternalScores));
            XmlUtil.writeNextValue(out, XML_TAG_NUM_ASSOCIATION, Integer.valueOf(configuration.numAssociation));
            XmlUtil.writeNextValue(out, XML_TAG_CREATOR_UID, Integer.valueOf(configuration.creatorUid));
            XmlUtil.writeNextValue(out, XML_TAG_CREATOR_NAME, configuration.creatorName);
            XmlUtil.writeNextValue(out, XML_TAG_CREATION_TIME, configuration.creationTime);
            XmlUtil.writeNextValue(out, XML_TAG_LAST_UPDATE_UID, Integer.valueOf(configuration.lastUpdateUid));
            XmlUtil.writeNextValue(out, XML_TAG_LAST_UPDATE_NAME, configuration.lastUpdateName);
            XmlUtil.writeNextValue(out, XML_TAG_LAST_CONNECT_UID, Integer.valueOf(configuration.lastConnectUid));
            XmlUtil.writeNextValue(out, XML_TAG_IS_LEGACY_PASSPOINT_CONFIG, Boolean.valueOf(configuration.isLegacyPasspointConfig));
            XmlUtil.writeNextValue(out, XML_TAG_ROAMING_CONSORTIUM_OIS, configuration.roamingConsortiumIds);
            XmlUtil.writeNextValue(out, XML_TAG_RANDOMIZED_MAC_ADDRESS, configuration.getRandomizedMacAddress().toString());
            XmlUtil.writeNextValue(out, XML_TAG_MAC_RANDOMIZATION_SETTING, Integer.valueOf(configuration.macRandomizationSetting));
            XmlUtil.writeNextValue(out, XML_TAG_SEM_CREATION_TIME_KEY, Long.valueOf(configuration.semCreationTime));
            XmlUtil.writeNextValue(out, XML_TAG_SEM_UPDATE_TIME_KEY, Long.valueOf(configuration.semUpdateTime));
            XmlUtil.writeNextValue(out, XML_TAG_LAST_CONNECTED_TIME_KEY, Long.valueOf(configuration.lastConnected));
            if (configuration.bssidWhitelist != null && configuration.bssidWhitelist.size() > 0) {
                for (Map.Entry<String, Long> entry : configuration.bssidWhitelist.entrySet()) {
                    XmlUtil.writeNextValue(out, XML_TAG_BSSID_WHITELIST_KEY, entry.getKey());
                    XmlUtil.writeNextValue(out, XML_TAG_BSSID_WHITELIST_UPDATE_TIME_KEY, entry.getValue());
                }
            }
            XmlUtil.writeNextValue(out, XML_TAG_VENDOR_SSID, Boolean.valueOf(configuration.semIsVendorSpecificSsid));
            XmlUtil.writeNextValue(out, XML_TAG_AUTO_RECONNECT, Integer.valueOf(configuration.semAutoReconnect));
            XmlUtil.writeNextValue(out, XML_TAG_IS_RECOMMENDED, Boolean.valueOf(configuration.semIsRecommended));
            XmlUtil.writeNextValue(out, XML_TAG_SAMSUNG_SPECIFIC_FLAGS, configuration.semSamsungSpecificFlags.toByteArray());
            XmlUtil.writeNextValue(out, XML_TAG_AUTO_WIFI_SCORE_KEY, Integer.valueOf(configuration.semAutoWifiScore));
            XmlUtil.writeNextValue(out, XML_TAG_WAPI_PSK_KEY_TYPE, Integer.valueOf(configuration.wapiPskType));
            XmlUtil.writeNextValue(out, XML_TAG_WAPI_CERT_INDEX, Integer.valueOf(configuration.wapiCertIndex));
            XmlUtil.writeNextValue(out, "WapiAsCert", configuration.wapiAsCert);
            XmlUtil.writeNextValue(out, "WapiUserCert", configuration.wapiUserCert);
            XmlUtil.writeNextValue(out, XML_TAG_ENTRY_RSSI_24GHZ, Integer.valueOf(configuration.entryRssi24GHz));
            XmlUtil.writeNextValue(out, XML_TAG_ENTRY_RSSI_5GHZ, Integer.valueOf(configuration.entryRssi5GHz));
            XmlUtil.writeNextValue(out, XML_TAG_CAPTIVE_PORTAL, Boolean.valueOf(configuration.isCaptivePortal));
        }

        private static void populateWepKeysFromXmlValue(Object value, String[] wepKeys, boolean withEncryption) throws XmlPullParserException, IOException {
            String[] wepKeysInData = (String[]) value;
            if (wepKeysInData != null) {
                if (wepKeysInData.length == wepKeys.length) {
                    for (int i = 0; i < wepKeys.length; i++) {
                        if (wepKeysInData[i].isEmpty()) {
                            wepKeys[i] = null;
                        } else if (withEncryption) {
                            wepKeys[i] = WifiPasswordManager.getInstance().decrypt(wepKeysInData[i]);
                        } else {
                            wepKeys[i] = wepKeysInData[i];
                        }
                    }
                    return;
                }
                throw new XmlPullParserException("Invalid Wep Keys length: " + wepKeysInData.length);
            }
        }

        public static Pair<String, WifiConfiguration> parseFromXml(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
            if (WifiPasswordManager.getInstance().isSupported()) {
                return parseFromXml(in, outerTagDepth, true);
            }
            return parseFromXml(in, outerTagDepth, false);
        }

        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* JADX WARNING: Code restructure failed: missing block: B:110:0x01bf, code lost:
            if (r10.equals(XML_TAG_SSID) != false) goto L_0x02ff;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static android.util.Pair<java.lang.String, android.net.wifi.WifiConfiguration> parseFromXml(org.xmlpull.v1.XmlPullParser r13, int r14, boolean r15) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
                android.net.wifi.WifiConfiguration r0 = new android.net.wifi.WifiConfiguration
                r0.<init>()
                r1 = 0
                r2 = 0
                r3 = 0
                r4 = 0
            L_0x000a:
                boolean r6 = com.android.server.wifi.util.XmlUtil.isNextSectionEnd(r13, r14)
                r7 = 0
                if (r6 != 0) goto L_0x0591
                r6 = 1
                java.lang.String[] r8 = new java.lang.String[r6]
                java.lang.Object r9 = com.android.server.wifi.util.XmlUtil.readCurrentValue(r13, r8)
                r10 = r8[r7]
                if (r10 == 0) goto L_0x0589
                r10 = r8[r7]
                r11 = -1
                int r12 = r10.hashCode()
                switch(r12) {
                    case -2072453770: goto L_0x02f3;
                    case -1845648133: goto L_0x02e8;
                    case -1819699067: goto L_0x02dd;
                    case -1808614382: goto L_0x02d2;
                    case -1793081233: goto L_0x02c7;
                    case -1757331736: goto L_0x02bc;
                    case -1704616680: goto L_0x02b1;
                    case -1681005553: goto L_0x02a6;
                    case -1663465224: goto L_0x029b;
                    case -1568560548: goto L_0x028f;
                    case -1406508014: goto L_0x0283;
                    case -1268502125: goto L_0x0277;
                    case -1173588624: goto L_0x026b;
                    case -1089007030: goto L_0x025f;
                    case -1073823059: goto L_0x0253;
                    case -1053711239: goto L_0x0247;
                    case -984540844: goto L_0x023b;
                    case -922179420: goto L_0x022f;
                    case -852165191: goto L_0x0223;
                    case -346924001: goto L_0x0217;
                    case -244338402: goto L_0x020b;
                    case -181205965: goto L_0x01ff;
                    case -135994866: goto L_0x01f3;
                    case -125790735: goto L_0x01e7;
                    case -94981986: goto L_0x01db;
                    case -51197516: goto L_0x01cf;
                    case 2165397: goto L_0x01c3;
                    case 2554747: goto L_0x01b9;
                    case 63507133: goto L_0x01ae;
                    case 95088315: goto L_0x01a2;
                    case 395107740: goto L_0x0196;
                    case 553095444: goto L_0x018a;
                    case 581636034: goto L_0x017e;
                    case 617552438: goto L_0x0172;
                    case 682791106: goto L_0x0166;
                    case 694485576: goto L_0x015a;
                    case 709971552: goto L_0x014e;
                    case 736944625: goto L_0x0142;
                    case 785209343: goto L_0x0136;
                    case 797043831: goto L_0x012b;
                    case 943896851: goto L_0x011f;
                    case 1035394844: goto L_0x0113;
                    case 1067884558: goto L_0x0107;
                    case 1190461055: goto L_0x00fb;
                    case 1199498141: goto L_0x00f0;
                    case 1266081442: goto L_0x00e4;
                    case 1327619296: goto L_0x00d8;
                    case 1350351025: goto L_0x00cc;
                    case 1476993207: goto L_0x00c0;
                    case 1748177418: goto L_0x00b4;
                    case 1750336108: goto L_0x00a8;
                    case 1851050768: goto L_0x009c;
                    case 1882132039: goto L_0x0090;
                    case 1903492587: goto L_0x0084;
                    case 1905126713: goto L_0x0079;
                    case 1955037270: goto L_0x006e;
                    case 1965854789: goto L_0x0063;
                    case 2018202939: goto L_0x0057;
                    case 2025240199: goto L_0x004b;
                    case 2026125174: goto L_0x003f;
                    case 2087394662: goto L_0x0033;
                    case 2143705732: goto L_0x0028;
                    default: goto L_0x0026;
                }
            L_0x0026:
                goto L_0x02fe
            L_0x0028:
                java.lang.String r6 = "RequirePMF"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 7
                goto L_0x02ff
            L_0x0033:
                java.lang.String r6 = "LoginUrl"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 54
                goto L_0x02ff
            L_0x003f:
                java.lang.String r6 = "AllowedSuiteBCiphers"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 14
                goto L_0x02ff
            L_0x004b:
                java.lang.String r6 = "ProviderFriendlyName"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 18
                goto L_0x02ff
            L_0x0057:
                java.lang.String r6 = "NumAssociation"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 27
                goto L_0x02ff
            L_0x0063:
                java.lang.String r6 = "HiddenSSID"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 6
                goto L_0x02ff
            L_0x006e:
                java.lang.String r6 = "WEPKeys"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 4
                goto L_0x02ff
            L_0x0079:
                java.lang.String r6 = "WEPTxKeyIndex"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 5
                goto L_0x02ff
            L_0x0084:
                java.lang.String r6 = "WapiCertIndex"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 49
                goto L_0x02ff
            L_0x0090:
                java.lang.String r6 = "semCreationTime"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 38
                goto L_0x02ff
            L_0x009c:
                java.lang.String r6 = "AllowedAuthAlgos"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 10
                goto L_0x02ff
            L_0x00a8:
                java.lang.String r6 = "CreationTime"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 30
                goto L_0x02ff
            L_0x00b4:
                java.lang.String r6 = "BssidWhitelist"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 41
                goto L_0x02ff
            L_0x00c0:
                java.lang.String r6 = "CreatorName"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 29
                goto L_0x02ff
            L_0x00cc:
                java.lang.String r6 = "LastUpdateUid"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 31
                goto L_0x02ff
            L_0x00d8:
                java.lang.String r6 = "BssidWhitelistUpdateTime"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 42
                goto L_0x02ff
            L_0x00e4:
                java.lang.String r6 = "CaptivePortal"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 52
                goto L_0x02ff
            L_0x00f0:
                java.lang.String r6 = "ConfigKey"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = r7
                goto L_0x02ff
            L_0x00fb:
                java.lang.String r6 = "SemLocationLongitude"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 61
                goto L_0x02ff
            L_0x0107:
                java.lang.String r6 = "EntryRssi24GHz"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 58
                goto L_0x02ff
            L_0x0113:
                java.lang.String r6 = "LinkedNetworksList"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 19
                goto L_0x02ff
            L_0x011f:
                java.lang.String r6 = "UseExternalScores"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 26
                goto L_0x02ff
            L_0x012b:
                java.lang.String r6 = "PreSharedKey"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 3
                goto L_0x02ff
            L_0x0136:
                java.lang.String r6 = "isVendorSpecificSsid"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 43
                goto L_0x02ff
            L_0x0142:
                java.lang.String r6 = "AllowedGroupCiphers"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 11
                goto L_0x02ff
            L_0x014e:
                java.lang.String r6 = "LastConnectedTime"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 40
                goto L_0x02ff
            L_0x015a:
                java.lang.String r6 = "AutoReconnect"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 44
                goto L_0x02ff
            L_0x0166:
                java.lang.String r6 = "AllowedPairwiseCiphers"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 12
                goto L_0x02ff
            L_0x0172:
                java.lang.String r6 = "semIsRecommended"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 45
                goto L_0x02ff
            L_0x017e:
                java.lang.String r6 = "UserApproved"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 23
                goto L_0x02ff
            L_0x018a:
                java.lang.String r6 = "WapiPskKeyType"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 48
                goto L_0x02ff
            L_0x0196:
                java.lang.String r6 = "SemLocationLatitude"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 60
                goto L_0x02ff
            L_0x01a2:
                java.lang.String r6 = "SamsungSpecificFlags"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 46
                goto L_0x02ff
            L_0x01ae:
                java.lang.String r6 = "BSSID"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 2
                goto L_0x02ff
            L_0x01b9:
                java.lang.String r12 = "SSID"
                boolean r10 = r10.equals(r12)
                if (r10 == 0) goto L_0x0026
                goto L_0x02ff
            L_0x01c3:
                java.lang.String r6 = "FQDN"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 17
                goto L_0x02ff
            L_0x01cf:
                java.lang.String r6 = "MeteredOverride"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 25
                goto L_0x02ff
            L_0x01db:
                java.lang.String r6 = "MacRandomizationSetting"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 37
                goto L_0x02ff
            L_0x01e7:
                java.lang.String r6 = "semUpdateTime"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 39
                goto L_0x02ff
            L_0x01f3:
                java.lang.String r6 = "IsLegacyPasspointConfig"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 34
                goto L_0x02ff
            L_0x01ff:
                java.lang.String r6 = "AllowedProtocols"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 9
                goto L_0x02ff
            L_0x020b:
                java.lang.String r6 = "NoInternetAccessExpected"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 22
                goto L_0x02ff
            L_0x0217:
                java.lang.String r6 = "RoamingConsortiumOIs"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 35
                goto L_0x02ff
            L_0x0223:
                java.lang.String r6 = "WapiAsCert"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 50
                goto L_0x02ff
            L_0x022f:
                java.lang.String r6 = "CreatorUid"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 28
                goto L_0x02ff
            L_0x023b:
                java.lang.String r6 = "SEM_AUTO_WIFI_SCORE"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 47
                goto L_0x02ff
            L_0x0247:
                java.lang.String r6 = "UsableInternet"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 55
                goto L_0x02ff
            L_0x0253:
                java.lang.String r6 = "EntryRssi5GHz"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 59
                goto L_0x02ff
            L_0x025f:
                java.lang.String r6 = "LastUpdateName"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 32
                goto L_0x02ff
            L_0x026b:
                java.lang.String r6 = "AllowedGroupMgmtCiphers"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 13
                goto L_0x02ff
            L_0x0277:
                java.lang.String r6 = "ValidatedInternetAccess"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 21
                goto L_0x02ff
            L_0x0283:
                java.lang.String r6 = "WapiUserCert"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 51
                goto L_0x02ff
            L_0x028f:
                java.lang.String r6 = "LastConnectUid"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 33
                goto L_0x02ff
            L_0x029b:
                java.lang.String r6 = "RandomizedMacAddress"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 36
                goto L_0x02ff
            L_0x02a6:
                java.lang.String r6 = "Authenticated"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 53
                goto L_0x02ff
            L_0x02b1:
                java.lang.String r6 = "AllowedKeyMgmt"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 8
                goto L_0x02ff
            L_0x02bc:
                java.lang.String r6 = "SkipInternetCheck"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 56
                goto L_0x02ff
            L_0x02c7:
                java.lang.String r6 = "MeteredHint"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 24
                goto L_0x02ff
            L_0x02d2:
                java.lang.String r6 = "Status"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 16
                goto L_0x02ff
            L_0x02dd:
                java.lang.String r6 = "Shared"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 15
                goto L_0x02ff
            L_0x02e8:
                java.lang.String r6 = "NextTargetRssi"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 57
                goto L_0x02ff
            L_0x02f3:
                java.lang.String r6 = "DefaultGwMacAddress"
                boolean r6 = r10.equals(r6)
                if (r6 == 0) goto L_0x0026
                r6 = 20
                goto L_0x02ff
            L_0x02fe:
                r6 = r11
            L_0x02ff:
                switch(r6) {
                    case 0: goto L_0x0583;
                    case 1: goto L_0x057d;
                    case 2: goto L_0x0577;
                    case 3: goto L_0x0561;
                    case 4: goto L_0x055b;
                    case 5: goto L_0x0551;
                    case 6: goto L_0x0547;
                    case 7: goto L_0x053d;
                    case 8: goto L_0x0533;
                    case 9: goto L_0x0529;
                    case 10: goto L_0x051f;
                    case 11: goto L_0x0514;
                    case 12: goto L_0x0508;
                    case 13: goto L_0x04fc;
                    case 14: goto L_0x04f0;
                    case 15: goto L_0x04e5;
                    case 16: goto L_0x04d7;
                    case 17: goto L_0x04d0;
                    case 18: goto L_0x04c9;
                    case 19: goto L_0x04c2;
                    case 20: goto L_0x04bb;
                    case 21: goto L_0x04b0;
                    case 22: goto L_0x04a5;
                    case 23: goto L_0x049a;
                    case 24: goto L_0x048f;
                    case 25: goto L_0x0484;
                    case 26: goto L_0x0479;
                    case 27: goto L_0x046e;
                    case 28: goto L_0x0463;
                    case 29: goto L_0x045c;
                    case 30: goto L_0x0455;
                    case 31: goto L_0x044a;
                    case 32: goto L_0x0443;
                    case 33: goto L_0x0438;
                    case 34: goto L_0x042d;
                    case 35: goto L_0x0426;
                    case 36: goto L_0x041a;
                    case 37: goto L_0x040e;
                    case 38: goto L_0x03fa;
                    case 39: goto L_0x03e6;
                    case 40: goto L_0x03d2;
                    case 41: goto L_0x03cd;
                    case 42: goto L_0x03a6;
                    case 43: goto L_0x039b;
                    case 44: goto L_0x0390;
                    case 45: goto L_0x0385;
                    case 46: goto L_0x037a;
                    case 47: goto L_0x036f;
                    case 48: goto L_0x0364;
                    case 49: goto L_0x0359;
                    case 50: goto L_0x0352;
                    case 51: goto L_0x034b;
                    case 52: goto L_0x0340;
                    case 53: goto L_0x0335;
                    case 54: goto L_0x0333;
                    case 55: goto L_0x0333;
                    case 56: goto L_0x0333;
                    case 57: goto L_0x0333;
                    case 58: goto L_0x0328;
                    case 59: goto L_0x031d;
                    case 60: goto L_0x031b;
                    case 61: goto L_0x031b;
                    default: goto L_0x0302;
                }
            L_0x0302:
                org.xmlpull.v1.XmlPullParserException r6 = new org.xmlpull.v1.XmlPullParserException
                java.lang.StringBuilder r10 = new java.lang.StringBuilder
                r10.<init>()
                java.lang.String r11 = "Unknown value name found: "
                r10.append(r11)
                r7 = r8[r7]
                r10.append(r7)
                java.lang.String r7 = r10.toString()
                r6.<init>(r7)
                throw r6
            L_0x031b:
                goto L_0x0587
            L_0x031d:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.entryRssi5GHz = r6
                goto L_0x0587
            L_0x0328:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.entryRssi24GHz = r6
                goto L_0x0587
            L_0x0333:
                goto L_0x0587
            L_0x0335:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.isAuthenticated = r6
                goto L_0x0587
            L_0x0340:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.isCaptivePortal = r6
                goto L_0x0587
            L_0x034b:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.wapiUserCert = r6
                goto L_0x0587
            L_0x0352:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.wapiAsCert = r6
                goto L_0x0587
            L_0x0359:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.wapiCertIndex = r6
                goto L_0x0587
            L_0x0364:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.wapiPskType = r6
                goto L_0x0587
            L_0x036f:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.semAutoWifiScore = r6
                goto L_0x0587
            L_0x037a:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.semSamsungSpecificFlags = r7
                goto L_0x0587
            L_0x0385:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.semIsRecommended = r6
                goto L_0x0587
            L_0x0390:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.semAutoReconnect = r6
                goto L_0x0587
            L_0x039b:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.semIsVendorSpecificSsid = r6
                goto L_0x0587
            L_0x03a6:
                java.lang.String r6 = java.lang.String.valueOf(r9)
                long r10 = java.lang.Long.parseLong(r6)
                java.lang.Long r7 = java.lang.Long.valueOf(r10)
                long r4 = r7.longValue()
                if (r3 == 0) goto L_0x0587
                r10 = 0
                int r10 = (r4 > r10 ? 1 : (r4 == r10 ? 0 : -1))
                if (r10 == 0) goto L_0x0587
                android.net.wifi.WifiConfiguration$BssidWhitelist r10 = r0.bssidWhitelist
                if (r10 == 0) goto L_0x0587
                android.net.wifi.WifiConfiguration$BssidWhitelist r10 = r0.bssidWhitelist
                java.lang.Long r11 = java.lang.Long.valueOf(r4)
                r10.put(r3, r11)
                goto L_0x0587
            L_0x03cd:
                r3 = r9
                java.lang.String r3 = (java.lang.String) r3
                goto L_0x0587
            L_0x03d2:
                java.lang.String r6 = java.lang.String.valueOf(r9)
                long r10 = java.lang.Long.parseLong(r6)
                java.lang.Long r7 = java.lang.Long.valueOf(r10)
                long r10 = r7.longValue()
                r0.lastConnected = r10
                goto L_0x0587
            L_0x03e6:
                java.lang.String r6 = java.lang.String.valueOf(r9)
                long r10 = java.lang.Long.parseLong(r6)
                java.lang.Long r7 = java.lang.Long.valueOf(r10)
                long r10 = r7.longValue()
                r0.semUpdateTime = r10
                goto L_0x0587
            L_0x03fa:
                java.lang.String r6 = java.lang.String.valueOf(r9)
                long r10 = java.lang.Long.parseLong(r6)
                java.lang.Long r7 = java.lang.Long.valueOf(r10)
                long r10 = r7.longValue()
                r0.semCreationTime = r10
                goto L_0x0587
            L_0x040e:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.macRandomizationSetting = r6
                r2 = 1
                goto L_0x0587
            L_0x041a:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                android.net.MacAddress r6 = android.net.MacAddress.fromString(r6)
                r0.setRandomizedMacAddress(r6)
                goto L_0x0587
            L_0x0426:
                r6 = r9
                long[] r6 = (long[]) r6
                r0.roamingConsortiumIds = r6
                goto L_0x0587
            L_0x042d:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.isLegacyPasspointConfig = r6
                goto L_0x0587
            L_0x0438:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.lastConnectUid = r6
                goto L_0x0587
            L_0x0443:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.lastUpdateName = r6
                goto L_0x0587
            L_0x044a:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.lastUpdateUid = r6
                goto L_0x0587
            L_0x0455:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.creationTime = r6
                goto L_0x0587
            L_0x045c:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.creatorName = r6
                goto L_0x0587
            L_0x0463:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.creatorUid = r6
                goto L_0x0587
            L_0x046e:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.numAssociation = r6
                goto L_0x0587
            L_0x0479:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.useExternalScores = r6
                goto L_0x0587
            L_0x0484:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.meteredOverride = r6
                goto L_0x0587
            L_0x048f:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.meteredHint = r6
                goto L_0x0587
            L_0x049a:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.userApproved = r6
                goto L_0x0587
            L_0x04a5:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.noInternetAccessExpected = r6
                goto L_0x0587
            L_0x04b0:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.validatedInternetAccess = r6
                goto L_0x0587
            L_0x04bb:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.defaultGwMacAddress = r6
                goto L_0x0587
            L_0x04c2:
                r6 = r9
                java.util.HashMap r6 = (java.util.HashMap) r6
                r0.linkedConfigurations = r6
                goto L_0x0587
            L_0x04c9:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.providerFriendlyName = r6
                goto L_0x0587
            L_0x04d0:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.FQDN = r6
                goto L_0x0587
            L_0x04d7:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                if (r6 != 0) goto L_0x04e1
                r6 = 2
            L_0x04e1:
                r0.status = r6
                goto L_0x0587
            L_0x04e5:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.shared = r6
                goto L_0x0587
            L_0x04f0:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedSuiteBCiphers = r7
                goto L_0x0587
            L_0x04fc:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedGroupManagementCiphers = r7
                goto L_0x0587
            L_0x0508:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedPairwiseCiphers = r7
                goto L_0x0587
            L_0x0514:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedGroupCiphers = r7
                goto L_0x0587
            L_0x051f:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedAuthAlgorithms = r7
                goto L_0x0587
            L_0x0529:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedProtocols = r7
                goto L_0x0587
            L_0x0533:
                r6 = r9
                byte[] r6 = (byte[]) r6
                java.util.BitSet r7 = java.util.BitSet.valueOf(r6)
                r0.allowedKeyManagement = r7
                goto L_0x0587
            L_0x053d:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.requirePMF = r6
                goto L_0x0587
            L_0x0547:
                r6 = r9
                java.lang.Boolean r6 = (java.lang.Boolean) r6
                boolean r6 = r6.booleanValue()
                r0.hiddenSSID = r6
                goto L_0x0587
            L_0x0551:
                r6 = r9
                java.lang.Integer r6 = (java.lang.Integer) r6
                int r6 = r6.intValue()
                r0.wepTxKeyIndex = r6
                goto L_0x0587
            L_0x055b:
                java.lang.String[] r6 = r0.wepKeys
                populateWepKeysFromXmlValue(r9, r6, r15)
                goto L_0x0587
            L_0x0561:
                if (r15 == 0) goto L_0x0571
                com.samsung.android.server.wifi.WifiPasswordManager r6 = com.samsung.android.server.wifi.WifiPasswordManager.getInstance()
                r7 = r9
                java.lang.String r7 = (java.lang.String) r7
                java.lang.String r6 = r6.decrypt(r7)
                r0.preSharedKey = r6
                goto L_0x0587
            L_0x0571:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.preSharedKey = r6
                goto L_0x0587
            L_0x0577:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.BSSID = r6
                goto L_0x0587
            L_0x057d:
                r6 = r9
                java.lang.String r6 = (java.lang.String) r6
                r0.SSID = r6
                goto L_0x0587
            L_0x0583:
                r1 = r9
                java.lang.String r1 = (java.lang.String) r1
            L_0x0587:
                goto L_0x000a
            L_0x0589:
                org.xmlpull.v1.XmlPullParserException r6 = new org.xmlpull.v1.XmlPullParserException
                java.lang.String r7 = "Missing value name"
                r6.<init>(r7)
                throw r6
            L_0x0591:
                if (r2 != 0) goto L_0x0595
                r0.macRandomizationSetting = r7
            L_0x0595:
                android.util.Pair r6 = android.util.Pair.create(r1, r0)
                return r6
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.util.XmlUtil.WifiConfigurationXmlUtil.parseFromXml(org.xmlpull.v1.XmlPullParser, int, boolean):android.util.Pair");
        }
    }

    public static class IpConfigurationXmlUtil {
        public static final String XML_TAG_DNS_SERVER_ADDRESSES = "DNSServers";
        public static final String XML_TAG_GATEWAY_ADDRESS = "GatewayAddress";
        public static final String XML_TAG_IP_ASSIGNMENT = "IpAssignment";
        public static final String XML_TAG_LINK_ADDRESS = "LinkAddress";
        public static final String XML_TAG_LINK_PREFIX_LENGTH = "LinkPrefixLength";
        public static final String XML_TAG_PROXY_EXCLUSION_LIST = "ProxyExclusionList";
        public static final String XML_TAG_PROXY_HOST = "ProxyHost";
        public static final String XML_TAG_PROXY_PAC_FILE = "ProxyPac";
        public static final String XML_TAG_PROXY_PASSWORD = "ProxyPassword";
        public static final String XML_TAG_PROXY_PORT = "ProxyPort";
        public static final String XML_TAG_PROXY_SETTINGS = "ProxySettings";
        public static final String XML_TAG_PROXY_USERNAME = "ProxyUsername";

        private static void writeStaticIpConfigurationToXml(XmlSerializer out, StaticIpConfiguration staticIpConfiguration) throws XmlPullParserException, IOException {
            if (staticIpConfiguration.ipAddress != null) {
                XmlUtil.writeNextValue(out, XML_TAG_LINK_ADDRESS, staticIpConfiguration.ipAddress.getAddress().getHostAddress());
                XmlUtil.writeNextValue(out, XML_TAG_LINK_PREFIX_LENGTH, Integer.valueOf(staticIpConfiguration.ipAddress.getPrefixLength()));
            } else {
                XmlUtil.writeNextValue(out, XML_TAG_LINK_ADDRESS, (Object) null);
                XmlUtil.writeNextValue(out, XML_TAG_LINK_PREFIX_LENGTH, (Object) null);
            }
            if (staticIpConfiguration.gateway != null) {
                XmlUtil.writeNextValue(out, XML_TAG_GATEWAY_ADDRESS, staticIpConfiguration.gateway.getHostAddress());
            } else {
                XmlUtil.writeNextValue(out, XML_TAG_GATEWAY_ADDRESS, (Object) null);
            }
            if (staticIpConfiguration.dnsServers != null) {
                String[] dnsServers = new String[staticIpConfiguration.dnsServers.size()];
                int dnsServerIdx = 0;
                Iterator it = staticIpConfiguration.dnsServers.iterator();
                while (it.hasNext()) {
                    dnsServers[dnsServerIdx] = ((InetAddress) it.next()).getHostAddress();
                    dnsServerIdx++;
                }
                XmlUtil.writeNextValue(out, XML_TAG_DNS_SERVER_ADDRESSES, dnsServers);
                return;
            }
            XmlUtil.writeNextValue(out, XML_TAG_DNS_SERVER_ADDRESSES, (Object) null);
        }

        public static void writeToXmlForBackup(XmlSerializer out, IpConfiguration ipConfiguration) throws XmlPullParserException, IOException {
            Uri pacUrl;
            XmlUtil.writeNextValue(out, XML_TAG_IP_ASSIGNMENT, ipConfiguration.ipAssignment.toString());
            if (C05801.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipConfiguration.ipAssignment.ordinal()] == 1) {
                writeStaticIpConfigurationToXml(out, ipConfiguration.getStaticIpConfiguration());
            }
            if (ipConfiguration.proxySettings == null || ipConfiguration.httpProxy == null) {
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_SETTINGS, IpConfiguration.ProxySettings.NONE.toString());
                return;
            }
            XmlUtil.writeNextValue(out, XML_TAG_PROXY_SETTINGS, ipConfiguration.proxySettings.toString());
            int i = C05801.$SwitchMap$android$net$IpConfiguration$ProxySettings[ipConfiguration.proxySettings.ordinal()];
            if (i == 1) {
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_HOST, ipConfiguration.httpProxy.getHost());
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_PORT, Integer.valueOf(ipConfiguration.httpProxy.getPort()));
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_EXCLUSION_LIST, ipConfiguration.httpProxy.getExclusionListAsString());
            } else if (i == 2 && (pacUrl = ipConfiguration.httpProxy.getPacFileUrl()) != null) {
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_PAC_FILE, pacUrl.toString());
            }
        }

        public static void writeToXml(XmlSerializer out, IpConfiguration ipConfiguration) throws XmlPullParserException, IOException {
            writeToXmlForBackup(out, ipConfiguration);
            if (C05801.$SwitchMap$android$net$IpConfiguration$ProxySettings[ipConfiguration.proxySettings.ordinal()] == 1) {
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_USERNAME, ipConfiguration.httpProxy.getUsername());
                XmlUtil.writeNextValue(out, XML_TAG_PROXY_PASSWORD, ipConfiguration.httpProxy.getPassword());
            }
        }

        private static StaticIpConfiguration parseStaticIpConfigurationFromXml(XmlPullParser in) throws XmlPullParserException, IOException {
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            String linkAddressString = (String) XmlUtil.readNextValueWithName(in, XML_TAG_LINK_ADDRESS);
            Integer linkPrefixLength = (Integer) XmlUtil.readNextValueWithName(in, XML_TAG_LINK_PREFIX_LENGTH);
            if (!(linkAddressString == null || linkPrefixLength == null)) {
                LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(linkAddressString), linkPrefixLength.intValue());
                if (linkAddress.getAddress() instanceof Inet4Address) {
                    staticIpConfiguration.ipAddress = linkAddress;
                } else {
                    Log.w(XmlUtil.TAG, "Non-IPv4 address: " + linkAddress);
                }
            }
            String gatewayAddressString = (String) XmlUtil.readNextValueWithName(in, XML_TAG_GATEWAY_ADDRESS);
            if (gatewayAddressString != null) {
                InetAddress gateway = NetworkUtils.numericToInetAddress(gatewayAddressString);
                RouteInfo route = new RouteInfo((LinkAddress) null, gateway);
                if (route.isIPv4Default()) {
                    staticIpConfiguration.gateway = gateway;
                } else {
                    Log.w(XmlUtil.TAG, "Non-IPv4 default route: " + route);
                }
            }
            String[] dnsServerAddressesString = (String[]) XmlUtil.readNextValueWithName(in, XML_TAG_DNS_SERVER_ADDRESSES);
            if (dnsServerAddressesString != null) {
                for (String dnsServerAddressString : dnsServerAddressesString) {
                    staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(dnsServerAddressString));
                }
            }
            return staticIpConfiguration;
        }

        public static IpConfiguration parseFromXml(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
            XmlPullParser xmlPullParser = in;
            IpConfiguration ipConfiguration = new IpConfiguration();
            IpConfiguration.IpAssignment ipAssignment = IpConfiguration.IpAssignment.valueOf((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_IP_ASSIGNMENT));
            ipConfiguration.setIpAssignment(ipAssignment);
            int i = C05801.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignment.ordinal()];
            if (i == 1) {
                ipConfiguration.setStaticIpConfiguration(parseStaticIpConfigurationFromXml(in));
            } else if (!(i == 2 || i == 3)) {
                throw new XmlPullParserException("Unknown ip assignment type: " + ipAssignment);
            }
            IpConfiguration.ProxySettings proxySettings = IpConfiguration.ProxySettings.valueOf((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_SETTINGS));
            ipConfiguration.setProxySettings(proxySettings);
            int i2 = C05801.$SwitchMap$android$net$IpConfiguration$ProxySettings[proxySettings.ordinal()];
            if (i2 == 1) {
                int proxyPort = ((Integer) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_PORT)).intValue();
                ProxyInfo proxyInfo = r9;
                ProxyInfo proxyInfo2 = new ProxyInfo((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_HOST), proxyPort, (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_USERNAME), (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_PASSWORD), (String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_EXCLUSION_LIST));
                ipConfiguration.setHttpProxy(proxyInfo);
            } else if (i2 == 2) {
                ipConfiguration.setHttpProxy(new ProxyInfo((String) XmlUtil.readNextValueWithName(xmlPullParser, XML_TAG_PROXY_PAC_FILE)));
            } else if (!(i2 == 3 || i2 == 4)) {
                throw new XmlPullParserException("Unknown proxy settings type: " + proxySettings);
            }
            return ipConfiguration;
        }
    }

    /* renamed from: com.android.server.wifi.util.XmlUtil$1 */
    static /* synthetic */ class C05801 {
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$IpAssignment = new int[IpConfiguration.IpAssignment.values().length];
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$ProxySettings = new int[IpConfiguration.ProxySettings.values().length];

        static {
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.DHCP.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public static class NetworkSelectionStatusXmlUtil {
        public static final String XML_TAG_CONNECT_CHOICE = "ConnectChoice";
        public static final String XML_TAG_CONNECT_CHOICE_TIMESTAMP = "ConnectChoiceTimeStamp";
        public static final String XML_TAG_DISABLE_REASON = "DisableReason";
        public static final String XML_TAG_HAS_EVER_CONNECTED = "HasEverConnected";
        public static final String XML_TAG_SELECTION_STATUS = "SelectionStatus";

        public static void writeToXml(XmlSerializer out, WifiConfiguration.NetworkSelectionStatus selectionStatus) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(out, XML_TAG_SELECTION_STATUS, selectionStatus.getNetworkStatusString());
            XmlUtil.writeNextValue(out, XML_TAG_DISABLE_REASON, selectionStatus.getNetworkDisableReasonString());
            XmlUtil.writeNextValue(out, XML_TAG_CONNECT_CHOICE, selectionStatus.getConnectChoice());
            XmlUtil.writeNextValue(out, XML_TAG_CONNECT_CHOICE_TIMESTAMP, Long.valueOf(selectionStatus.getConnectChoiceTimestamp()));
            XmlUtil.writeNextValue(out, XML_TAG_HAS_EVER_CONNECTED, Boolean.valueOf(selectionStatus.getHasEverConnected()));
        }

        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v5, resolved type: java.lang.Object} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v3, resolved type: java.lang.String} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r2v3, resolved type: java.lang.String} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v3, resolved type: java.lang.String} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v5, resolved type: java.lang.Long} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v9, resolved type: java.lang.Boolean} */
        /* JADX WARNING: Multi-variable type inference failed */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static android.net.wifi.WifiConfiguration.NetworkSelectionStatus parseFromXml(org.xmlpull.v1.XmlPullParser r13, int r14) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
                android.net.wifi.WifiConfiguration$NetworkSelectionStatus r0 = new android.net.wifi.WifiConfiguration$NetworkSelectionStatus
                r0.<init>()
                java.lang.String r1 = ""
                java.lang.String r2 = ""
            L_0x0009:
                boolean r3 = com.android.server.wifi.util.XmlUtil.isNextSectionEnd(r13, r14)
                r4 = -1
                r5 = 1
                if (r3 != 0) goto L_0x00ac
                java.lang.String[] r3 = new java.lang.String[r5]
                java.lang.Object r6 = com.android.server.wifi.util.XmlUtil.readCurrentValue(r13, r3)
                r7 = 0
                r8 = r3[r7]
                if (r8 == 0) goto L_0x00a4
                r8 = r3[r7]
                int r9 = r8.hashCode()
                r10 = 4
                r11 = 3
                r12 = 2
                switch(r9) {
                    case -1529270479: goto L_0x0051;
                    case -822052309: goto L_0x0047;
                    case -808576245: goto L_0x003d;
                    case -85195988: goto L_0x0033;
                    case 1452117118: goto L_0x0029;
                    default: goto L_0x0028;
                }
            L_0x0028:
                goto L_0x005a
            L_0x0029:
                java.lang.String r9 = "SelectionStatus"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0028
                r4 = r7
                goto L_0x005a
            L_0x0033:
                java.lang.String r9 = "DisableReason"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0028
                r4 = r5
                goto L_0x005a
            L_0x003d:
                java.lang.String r9 = "ConnectChoice"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0028
                r4 = r12
                goto L_0x005a
            L_0x0047:
                java.lang.String r9 = "ConnectChoiceTimeStamp"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0028
                r4 = r11
                goto L_0x005a
            L_0x0051:
                java.lang.String r9 = "HasEverConnected"
                boolean r8 = r8.equals(r9)
                if (r8 == 0) goto L_0x0028
                r4 = r10
            L_0x005a:
                if (r4 == 0) goto L_0x009e
                if (r4 == r5) goto L_0x009a
                if (r4 == r12) goto L_0x0093
                if (r4 == r11) goto L_0x0088
                if (r4 != r10) goto L_0x006f
                r4 = r6
                java.lang.Boolean r4 = (java.lang.Boolean) r4
                boolean r4 = r4.booleanValue()
                r0.setHasEverConnected(r4)
                goto L_0x00a2
            L_0x006f:
                org.xmlpull.v1.XmlPullParserException r4 = new org.xmlpull.v1.XmlPullParserException
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r8 = "Unknown value name found: "
                r5.append(r8)
                r7 = r3[r7]
                r5.append(r7)
                java.lang.String r5 = r5.toString()
                r4.<init>(r5)
                throw r4
            L_0x0088:
                r4 = r6
                java.lang.Long r4 = (java.lang.Long) r4
                long r4 = r4.longValue()
                r0.setConnectChoiceTimestamp(r4)
                goto L_0x00a2
            L_0x0093:
                r4 = r6
                java.lang.String r4 = (java.lang.String) r4
                r0.setConnectChoice(r4)
                goto L_0x00a2
            L_0x009a:
                r2 = r6
                java.lang.String r2 = (java.lang.String) r2
                goto L_0x00a2
            L_0x009e:
                r1 = r6
                java.lang.String r1 = (java.lang.String) r1
            L_0x00a2:
                goto L_0x0009
            L_0x00a4:
                org.xmlpull.v1.XmlPullParserException r4 = new org.xmlpull.v1.XmlPullParserException
                java.lang.String r5 = "Missing value name"
                r4.<init>(r5)
                throw r4
            L_0x00ac:
                java.lang.String[] r3 = android.net.wifi.WifiConfiguration.NetworkSelectionStatus.QUALITY_NETWORK_SELECTION_STATUS
                java.util.List r3 = java.util.Arrays.asList(r3)
                int r3 = r3.indexOf(r1)
                java.lang.String[] r6 = android.net.wifi.WifiConfiguration.NetworkSelectionStatus.QUALITY_NETWORK_SELECTION_DISABLE_REASON
                java.util.List r6 = java.util.Arrays.asList(r6)
                int r6 = r6.indexOf(r2)
                if (r3 == r4) goto L_0x00c6
                if (r6 == r4) goto L_0x00c6
                if (r3 != r5) goto L_0x00c8
            L_0x00c6:
                r3 = 0
                r6 = 0
            L_0x00c8:
                r0.setNetworkSelectionStatus(r3)
                r0.setNetworkSelectionDisableReason(r6)
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.util.XmlUtil.NetworkSelectionStatusXmlUtil.parseFromXml(org.xmlpull.v1.XmlPullParser, int):android.net.wifi.WifiConfiguration$NetworkSelectionStatus");
        }
    }

    public static class WifiEnterpriseConfigXmlUtil {
        public static final String XML_TAG_ALT_SUBJECT_MATCH = "AltSubjectMatch";
        public static final String XML_TAG_ANON_IDENTITY = "AnonIdentity";
        public static final String XML_TAG_CA_CERT = "CaCert";
        public static final String XML_TAG_CA_PATH = "CaPath";
        public static final String XML_TAG_CLIENT_CERT = "ClientCert";
        public static final String XML_TAG_DOM_SUFFIX_MATCH = "DomSuffixMatch";
        public static final String XML_TAG_EAP_METHOD = "EapMethod";
        public static final String XML_TAG_ENGINE = "Engine";
        public static final String XML_TAG_ENGINE_ID = "EngineId";
        public static final String XML_TAG_IDENTITY = "Identity";
        public static final String XML_TAG_PAC_FILE = "PacFile";
        public static final String XML_TAG_PASSWORD = "Password";
        public static final String XML_TAG_PHASE1_METHOD = "Phase1Method";
        public static final String XML_TAG_PHASE2_METHOD = "Phase2Method";
        public static final String XML_TAG_PLMN = "PLMN";
        public static final String XML_TAG_PRIVATE_KEY_ID = "PrivateKeyId";
        public static final String XML_TAG_REALM = "Realm";
        public static final String XML_TAG_SIM_NUMBER = "SimNumber";
        public static final String XML_TAG_SUBJECT_MATCH = "SubjectMatch";
        public static final String XML_TAG_WAPI_AS_CERT = "WapiAsCert";
        public static final String XML_TAG_WAPI_USER_CERT = "WapiUserCert";

        public static void writeToXml(XmlSerializer out, WifiEnterpriseConfig enterpriseConfig) throws XmlPullParserException, IOException {
            writeToXml(out, enterpriseConfig, WifiPasswordManager.getInstance().isSupported());
        }

        public static void writeToXml(XmlSerializer out, WifiEnterpriseConfig enterpriseConfig, boolean withEncryption) throws XmlPullParserException, IOException {
            XmlUtil.writeNextValue(out, XML_TAG_IDENTITY, enterpriseConfig.getFieldValue("identity"));
            XmlUtil.writeNextValue(out, XML_TAG_ANON_IDENTITY, enterpriseConfig.getFieldValue("anonymous_identity"));
            if (withEncryption) {
                XmlUtil.writeNextValue(out, XML_TAG_PASSWORD, WifiPasswordManager.getInstance().encrypt(enterpriseConfig.getFieldValue(McfDataUtil.McfData.JSON_PASSWORD)));
            } else {
                XmlUtil.writeNextValue(out, XML_TAG_PASSWORD, enterpriseConfig.getFieldValue(McfDataUtil.McfData.JSON_PASSWORD));
            }
            XmlUtil.writeNextValue(out, XML_TAG_CLIENT_CERT, enterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT));
            XmlUtil.writeNextValue(out, XML_TAG_CA_CERT, enterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT));
            XmlUtil.writeNextValue(out, XML_TAG_SUBJECT_MATCH, enterpriseConfig.getFieldValue("subject_match"));
            XmlUtil.writeNextValue(out, XML_TAG_ENGINE, enterpriseConfig.getFieldValue("engine"));
            XmlUtil.writeNextValue(out, XML_TAG_ENGINE_ID, enterpriseConfig.getFieldValue("engine_id"));
            XmlUtil.writeNextValue(out, XML_TAG_PRIVATE_KEY_ID, enterpriseConfig.getFieldValue("key_id"));
            XmlUtil.writeNextValue(out, XML_TAG_ALT_SUBJECT_MATCH, enterpriseConfig.getFieldValue("altsubject_match"));
            XmlUtil.writeNextValue(out, XML_TAG_DOM_SUFFIX_MATCH, enterpriseConfig.getFieldValue("domain_suffix_match"));
            XmlUtil.writeNextValue(out, XML_TAG_CA_PATH, enterpriseConfig.getFieldValue(WifiBackupRestore.SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH));
            XmlUtil.writeNextValue(out, XML_TAG_EAP_METHOD, Integer.valueOf(enterpriseConfig.getEapMethod()));
            XmlUtil.writeNextValue(out, XML_TAG_PHASE2_METHOD, Integer.valueOf(enterpriseConfig.getPhase2Method()));
            XmlUtil.writeNextValue(out, XML_TAG_PLMN, enterpriseConfig.getPlmn());
            XmlUtil.writeNextValue(out, XML_TAG_REALM, enterpriseConfig.getRealm());
            XmlUtil.writeNextValue(out, XML_TAG_SIM_NUMBER, enterpriseConfig.getFieldValue("sim_num"));
            XmlUtil.writeNextValue(out, XML_TAG_PHASE1_METHOD, enterpriseConfig.getPhase1Method());
        }

        public static WifiEnterpriseConfig parseFromXml(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
            return parseFromXml(in, outerTagDepth, WifiPasswordManager.getInstance().isSupported());
        }

        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* JADX WARNING: Code restructure failed: missing block: B:68:0x010a, code lost:
            if (r5.equals(XML_TAG_ANON_IDENTITY) != false) goto L_0x010e;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static android.net.wifi.WifiEnterpriseConfig parseFromXml(org.xmlpull.v1.XmlPullParser r8, int r9, boolean r10) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
                android.net.wifi.WifiEnterpriseConfig r0 = new android.net.wifi.WifiEnterpriseConfig
                r0.<init>()
            L_0x0005:
                boolean r1 = com.android.server.wifi.util.XmlUtil.isNextSectionEnd(r8, r9)
                if (r1 != 0) goto L_0x022f
                r1 = 1
                java.lang.String[] r2 = new java.lang.String[r1]
                java.lang.Object r3 = com.android.server.wifi.util.XmlUtil.readCurrentValue(r8, r2)
                r4 = 0
                r5 = r2[r4]
                if (r5 == 0) goto L_0x0227
                r5 = r2[r4]
                int r6 = r5.hashCode()
                r7 = -1
                switch(r6) {
                    case -1956487222: goto L_0x0104;
                    case -1766961550: goto L_0x00f9;
                    case -1406508014: goto L_0x00ee;
                    case -1362213863: goto L_0x00e4;
                    case -1273966921: goto L_0x00d9;
                    case -1199574865: goto L_0x00cf;
                    case -1085249824: goto L_0x00c4;
                    case -852165191: goto L_0x00b9;
                    case -596361182: goto L_0x00ae;
                    case -386463240: goto L_0x00a2;
                    case -71117602: goto L_0x0097;
                    case 2458781: goto L_0x008b;
                    case 11048245: goto L_0x007f;
                    case 78834287: goto L_0x0073;
                    case 856496398: goto L_0x0067;
                    case 1146405943: goto L_0x005b;
                    case 1281629883: goto L_0x0050;
                    case 1885134621: goto L_0x0045;
                    case 2009831362: goto L_0x003a;
                    case 2010214851: goto L_0x002e;
                    case 2080171618: goto L_0x0023;
                    default: goto L_0x0021;
                }
            L_0x0021:
                goto L_0x010d
            L_0x0023:
                java.lang.String r1 = "Engine"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 6
                goto L_0x010e
            L_0x002e:
                java.lang.String r1 = "CaPath"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 11
                goto L_0x010e
            L_0x003a:
                java.lang.String r1 = "CaCert"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 4
                goto L_0x010e
            L_0x0045:
                java.lang.String r1 = "EngineId"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 7
                goto L_0x010e
            L_0x0050:
                java.lang.String r1 = "Password"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 2
                goto L_0x010e
            L_0x005b:
                java.lang.String r1 = "PrivateKeyId"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 8
                goto L_0x010e
            L_0x0067:
                java.lang.String r1 = "PacFile"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 18
                goto L_0x010e
            L_0x0073:
                java.lang.String r1 = "Realm"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 15
                goto L_0x010e
            L_0x007f:
                java.lang.String r1 = "EapMethod"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 12
                goto L_0x010e
            L_0x008b:
                java.lang.String r1 = "PLMN"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 14
                goto L_0x010e
            L_0x0097:
                java.lang.String r1 = "Identity"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = r4
                goto L_0x010e
            L_0x00a2:
                java.lang.String r1 = "Phase2Method"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 13
                goto L_0x010e
            L_0x00ae:
                java.lang.String r1 = "AltSubjectMatch"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 9
                goto L_0x010e
            L_0x00b9:
                java.lang.String r1 = "WapiAsCert"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 19
                goto L_0x010e
            L_0x00c4:
                java.lang.String r1 = "SimNumber"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 16
                goto L_0x010e
            L_0x00cf:
                java.lang.String r1 = "ClientCert"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 3
                goto L_0x010e
            L_0x00d9:
                java.lang.String r1 = "Phase1Method"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 17
                goto L_0x010e
            L_0x00e4:
                java.lang.String r1 = "SubjectMatch"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 5
                goto L_0x010e
            L_0x00ee:
                java.lang.String r1 = "WapiUserCert"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 20
                goto L_0x010e
            L_0x00f9:
                java.lang.String r1 = "DomSuffixMatch"
                boolean r1 = r5.equals(r1)
                if (r1 == 0) goto L_0x0021
                r1 = 10
                goto L_0x010e
            L_0x0104:
                java.lang.String r6 = "AnonIdentity"
                boolean r5 = r5.equals(r6)
                if (r5 == 0) goto L_0x0021
                goto L_0x010e
            L_0x010d:
                r1 = r7
            L_0x010e:
                switch(r1) {
                    case 0: goto L_0x021c;
                    case 1: goto L_0x0213;
                    case 2: goto L_0x01f8;
                    case 3: goto L_0x01ef;
                    case 4: goto L_0x01e6;
                    case 5: goto L_0x01dd;
                    case 6: goto L_0x01d4;
                    case 7: goto L_0x01cb;
                    case 8: goto L_0x01c2;
                    case 9: goto L_0x01b9;
                    case 10: goto L_0x01af;
                    case 11: goto L_0x01a5;
                    case 12: goto L_0x0199;
                    case 13: goto L_0x018d;
                    case 14: goto L_0x0185;
                    case 15: goto L_0x017d;
                    case 16: goto L_0x0173;
                    case 17: goto L_0x014b;
                    case 18: goto L_0x0149;
                    case 19: goto L_0x012a;
                    case 20: goto L_0x012a;
                    default: goto L_0x0111;
                }
            L_0x0111:
                org.xmlpull.v1.XmlPullParserException r1 = new org.xmlpull.v1.XmlPullParserException
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.lang.String r6 = "Unknown value name found: "
                r5.append(r6)
                r4 = r2[r4]
                r5.append(r4)
                java.lang.String r4 = r5.toString()
                r1.<init>(r4)
                throw r1
            L_0x012a:
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r5 = "dummy field "
                r1.append(r5)
                r4 = r2[r4]
                r1.append(r4)
                java.lang.String r4 = " to avoid XmlPullParserException"
                r1.append(r4)
                java.lang.String r1 = r1.toString()
                java.lang.String r4 = "WifiXmlUtil"
                android.util.Log.i(r4, r1)
                goto L_0x0225
            L_0x0149:
                goto L_0x0225
            L_0x014b:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = ""
                java.lang.String r5 = "fast_provisioning="
                java.lang.String r1 = r1.replaceAll(r5, r4)
                java.lang.String r5 = "NULL"
                boolean r5 = r1.equals(r5)
                if (r5 != 0) goto L_0x016e
                boolean r4 = r1.equals(r4)
                if (r4 == 0) goto L_0x0165
                goto L_0x016e
            L_0x0165:
                int r4 = java.lang.Integer.parseInt(r1)
                r0.setPhase1Method(r4)
                goto L_0x0225
            L_0x016e:
                r0.setPhase1Method(r7)
                goto L_0x0225
            L_0x0173:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "sim_num"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x017d:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                r0.setRealm(r1)
                goto L_0x0225
            L_0x0185:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                r0.setPlmn(r1)
                goto L_0x0225
            L_0x018d:
                r1 = r3
                java.lang.Integer r1 = (java.lang.Integer) r1
                int r1 = r1.intValue()
                r0.setPhase2Method(r1)
                goto L_0x0225
            L_0x0199:
                r1 = r3
                java.lang.Integer r1 = (java.lang.Integer) r1
                int r1 = r1.intValue()
                r0.setEapMethod(r1)
                goto L_0x0225
            L_0x01a5:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "ca_path"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01af:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "domain_suffix_match"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01b9:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "altsubject_match"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01c2:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "key_id"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01cb:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "engine_id"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01d4:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "engine"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01dd:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "subject_match"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01e6:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "ca_cert"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01ef:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "client_cert"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x01f8:
                java.lang.String r1 = "password"
                if (r10 == 0) goto L_0x020c
                com.samsung.android.server.wifi.WifiPasswordManager r4 = com.samsung.android.server.wifi.WifiPasswordManager.getInstance()
                r5 = r3
                java.lang.String r5 = (java.lang.String) r5
                java.lang.String r4 = r4.decrypt(r5)
                r0.setFieldValue(r1, r4)
                goto L_0x0225
            L_0x020c:
                r4 = r3
                java.lang.String r4 = (java.lang.String) r4
                r0.setFieldValue(r1, r4)
                goto L_0x0225
            L_0x0213:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "anonymous_identity"
                r0.setFieldValue(r4, r1)
                goto L_0x0225
            L_0x021c:
                r1 = r3
                java.lang.String r1 = (java.lang.String) r1
                java.lang.String r4 = "identity"
                r0.setFieldValue(r4, r1)
            L_0x0225:
                goto L_0x0005
            L_0x0227:
                org.xmlpull.v1.XmlPullParserException r1 = new org.xmlpull.v1.XmlPullParserException
                java.lang.String r4 = "Missing value name"
                r1.<init>(r4)
                throw r1
            L_0x022f:
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.util.XmlUtil.WifiEnterpriseConfigXmlUtil.parseFromXml(org.xmlpull.v1.XmlPullParser, int, boolean):android.net.wifi.WifiEnterpriseConfig");
        }
    }
}
