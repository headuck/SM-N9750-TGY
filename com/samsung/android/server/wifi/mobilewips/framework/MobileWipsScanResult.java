package com.samsung.android.server.wifi.mobilewips.framework;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MobileWipsScanResult implements Parcelable {
    public static final int CHANNEL_WIDTH_160MHZ = 3;
    public static final int CHANNEL_WIDTH_20MHZ = 0;
    public static final int CHANNEL_WIDTH_40MHZ = 1;
    public static final int CHANNEL_WIDTH_80MHZ = 2;
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
    public static final int CIPHER_CCMP = 3;
    public static final int CIPHER_NONE = 0;
    public static final int CIPHER_NO_GROUP_ADDRESSED = 1;
    public static final int CIPHER_TKIP = 2;
    public static final Parcelable.Creator<MobileWipsScanResult> CREATOR = new Parcelable.Creator<MobileWipsScanResult>() {
        public MobileWipsScanResult createFromParcel(Parcel in) {
            Parcel parcel = in;
            MobileWipsWifiSsid mobileWipsWifiSsid = null;
            boolean z = true;
            if (in.readInt() == 1) {
                mobileWipsWifiSsid = MobileWipsWifiSsid.CREATOR.createFromParcel(parcel);
            }
            MobileWipsScanResult mobileWipsScanResult = new MobileWipsScanResult(mobileWipsWifiSsid, in.readString(), in.readString(), in.readLong(), in.readInt(), in.readString(), in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), false);
            mobileWipsScanResult.seen = in.readLong();
            mobileWipsScanResult.untrusted = in.readInt() != 0;
            mobileWipsScanResult.numUsage = in.readInt();
            mobileWipsScanResult.venueName = in.readString();
            mobileWipsScanResult.operatorFriendlyName = in.readString();
            mobileWipsScanResult.flags = in.readLong();
            int n = in.readInt();
            if (n != 0) {
                mobileWipsScanResult.informationElements = new InformationElement[n];
                for (int i = 0; i < n; i++) {
                    mobileWipsScanResult.informationElements[i] = new InformationElement();
                    mobileWipsScanResult.informationElements[i].f56id = in.readInt();
                    mobileWipsScanResult.informationElements[i].bytes = new byte[in.readInt()];
                    parcel.readByteArray(mobileWipsScanResult.informationElements[i].bytes);
                }
            }
            int n2 = in.readInt();
            if (n2 != 0) {
                mobileWipsScanResult.anqpLines = new ArrayList();
                for (int i2 = 0; i2 < n2; i2++) {
                    mobileWipsScanResult.anqpLines.add(in.readString());
                }
            }
            int n3 = in.readInt();
            if (n3 != 0) {
                mobileWipsScanResult.anqpElements = new AnqpInformationElement[n3];
                for (int i3 = 0; i3 < n3; i3++) {
                    int vendorId = in.readInt();
                    int elementId = in.readInt();
                    byte[] payload = new byte[in.readInt()];
                    parcel.readByteArray(payload);
                    mobileWipsScanResult.anqpElements[i3] = new AnqpInformationElement(vendorId, elementId, payload);
                }
            }
            if (in.readInt() == 0) {
                z = false;
            }
            mobileWipsScanResult.isCarrierAp = z;
            mobileWipsScanResult.carrierApEapType = in.readInt();
            mobileWipsScanResult.carrierName = in.readString();
            int n4 = in.readInt();
            if (n4 != 0) {
                mobileWipsScanResult.radioChainInfos = new RadioChainInfo[n4];
                for (int i4 = 0; i4 < n4; i4++) {
                    mobileWipsScanResult.radioChainInfos[i4] = new RadioChainInfo();
                    mobileWipsScanResult.radioChainInfos[i4].f57id = in.readInt();
                    mobileWipsScanResult.radioChainInfos[i4].level = in.readInt();
                }
            }
            mobileWipsScanResult.wifiMode = in.readInt();
            mobileWipsScanResult.semVendorSpecificInfo = in.readString();
            mobileWipsScanResult.semBssLoadElement = in.readString();
            mobileWipsScanResult.semKtVendorSpecificInfo = in.readString();
            return mobileWipsScanResult;
        }

        public MobileWipsScanResult[] newArray(int size) {
            return new MobileWipsScanResult[size];
        }
    };
    public static final long FLAG_80211mc_RESPONDER = 2;
    public static final long FLAG_PASSPOINT_NETWORK = 1;
    public static final int KEY_MGMT_CCKM = 14;
    public static final int KEY_MGMT_EAP = 2;
    public static final int KEY_MGMT_EAP_SHA256 = 6;
    public static final int KEY_MGMT_EAP_SUITE_B_192 = 10;
    public static final int KEY_MGMT_FT_EAP = 4;
    public static final int KEY_MGMT_FT_PSK = 3;
    public static final int KEY_MGMT_FT_SAE = 15;
    public static final int KEY_MGMT_NONE = 0;
    public static final int KEY_MGMT_OSEN = 7;
    public static final int KEY_MGMT_OWE = 9;
    public static final int KEY_MGMT_PSK = 1;
    public static final int KEY_MGMT_PSK_SHA256 = 5;
    public static final int KEY_MGMT_SAE = 8;
    public static final int KEY_MGMT_WAPI_CERT = 13;
    public static final int KEY_MGMT_WAPI_PSK = 12;
    public static final int MODE_11A = 1;
    public static final int MODE_11AC = 5;
    public static final int MODE_11AX = 6;
    public static final int MODE_11B = 2;
    public static final int MODE_11G = 3;
    public static final int MODE_11N = 4;
    public static final int MODE_UNDEFINED = 0;
    public static final int PROTOCOL_NONE = 0;
    public static final int PROTOCOL_OSEN = 3;
    public static final int PROTOCOL_RSN = 5;
    public static final int PROTOCOL_WAPI = 4;
    public static final int PROTOCOL_WPA = 1;
    public static final int PROTOCOL_WPA2 = 2;
    public static final int UNSPECIFIED = -1;
    public String BSSID;
    public String SSID;
    public int anqpDomainId;
    public AnqpInformationElement[] anqpElements;
    public List<String> anqpLines;
    public String capabilities;
    public int carrierApEapType;
    public String carrierName;
    public int centerFreq0;
    public int centerFreq1;
    public int channelWidth;
    public int distanceCm;
    public int distanceSdCm;
    public long flags;
    public int frequency;
    public long hessid;
    public InformationElement[] informationElements;
    public boolean is80211McRTTResponder;
    public boolean isCarrierAp;
    public int level;
    public MobileWipsWifiSsid mobileWipsWifiSsid;
    public int numUsage;
    public CharSequence operatorFriendlyName;
    public RadioChainInfo[] radioChainInfos;
    public long seen;
    public String semBssLoadElement;
    public String semKtVendorSpecificInfo;
    public String semVendorSpecificInfo;
    public long timestamp;
    public boolean untrusted;
    public CharSequence venueName;
    public int wifiMode;

    public static class RadioChainInfo {

        /* renamed from: id */
        public int f57id;
        public int level;

        public String toString() {
            return "RadioChainInfo: id=" + this.f57id + ", level=" + this.level;
        }

        public boolean equals(Object otherObj) {
            if (this == otherObj) {
                return true;
            }
            if (!(otherObj instanceof RadioChainInfo)) {
                return false;
            }
            RadioChainInfo other = (RadioChainInfo) otherObj;
            if (this.f57id == other.f57id && this.level == other.level) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(this.f57id), Integer.valueOf(this.level)});
        }
    }

    public void setFlag(long flag) {
        this.flags |= flag;
    }

    public void clearFlag(long flag) {
        this.flags &= ~flag;
    }

    public boolean is80211mcResponder() {
        return (this.flags & 2) != 0;
    }

    public boolean isPasspointNetwork() {
        return (this.flags & 1) != 0;
    }

    public boolean is24GHz() {
        return is24GHz(this.frequency);
    }

    public static boolean is24GHz(int freq) {
        return freq > 2400 && freq < 2500;
    }

    public boolean is5GHz() {
        return is5GHz(this.frequency);
    }

    public static boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    public static class AnqpInformationElement {
        public static final int ANQP_3GPP_NETWORK = 264;
        public static final int ANQP_CAPABILITY_LIST = 257;
        public static final int ANQP_CIVIC_LOC = 266;
        public static final int ANQP_DOM_NAME = 268;
        public static final int ANQP_EMERGENCY_ALERT = 269;
        public static final int ANQP_EMERGENCY_NAI = 271;
        public static final int ANQP_EMERGENCY_NUMBER = 259;
        public static final int ANQP_GEO_LOC = 265;
        public static final int ANQP_IP_ADDR_AVAILABILITY = 262;
        public static final int ANQP_LOC_URI = 267;
        public static final int ANQP_NAI_REALM = 263;
        public static final int ANQP_NEIGHBOR_REPORT = 272;
        public static final int ANQP_NWK_AUTH_TYPE = 260;
        public static final int ANQP_QUERY_LIST = 256;
        public static final int ANQP_ROAMING_CONSORTIUM = 261;
        public static final int ANQP_TDLS_CAP = 270;
        public static final int ANQP_VENDOR_SPEC = 56797;
        public static final int ANQP_VENUE_NAME = 258;
        public static final int HOTSPOT20_VENDOR_ID = 5271450;
        public static final int HS_CAPABILITY_LIST = 2;
        public static final int HS_CONN_CAPABILITY = 5;
        public static final int HS_FRIENDLY_NAME = 3;
        public static final int HS_ICON_FILE = 11;
        public static final int HS_ICON_REQUEST = 10;
        public static final int HS_NAI_HOME_REALM_QUERY = 6;
        public static final int HS_OPERATING_CLASS = 7;
        public static final int HS_OSU_PROVIDERS = 8;
        public static final int HS_QUERY_LIST = 1;
        public static final int HS_WAN_METRICS = 4;
        private final int mElementId;
        private final byte[] mPayload;
        private final int mVendorId;

        public AnqpInformationElement(int vendorId, int elementId, byte[] payload) {
            this.mVendorId = vendorId;
            this.mElementId = elementId;
            this.mPayload = payload;
        }

        public int getVendorId() {
            return this.mVendorId;
        }

        public int getElementId() {
            return this.mElementId;
        }

        public byte[] getPayload() {
            return this.mPayload;
        }
    }

    public static class InformationElement {
        public static final int EID_BSS_LOAD = 11;
        public static final int EID_ERP = 42;
        public static final int EID_EXTENDED_CAPS = 127;
        public static final int EID_EXTENDED_SUPPORTED_RATES = 50;
        public static final int EID_EXTENSION = 255;
        public static final int EID_EXT_ASSOC_DELAY_INFO = 1;
        public static final int EID_EXT_ESTIMATED_SERVICE_PARAMS = 11;
        public static final int EID_EXT_EXTENDED_REQUEST = 10;
        public static final int EID_EXT_FILS_HLP_CONTAINER = 5;
        public static final int EID_EXT_FILS_IP_ADDR_ASSIGN = 6;
        public static final int EID_EXT_FILS_KEY_CONFIRM = 3;
        public static final int EID_EXT_FILS_NONCE = 13;
        public static final int EID_EXT_FILS_PUBLIC_KEY = 12;
        public static final int EID_EXT_FILS_REQ_PARAMS = 2;
        public static final int EID_EXT_FILS_SESSION = 4;
        public static final int EID_EXT_FILS_WRAPPED_DATA = 8;
        public static final int EID_EXT_FTM_SYNC_INFO = 9;
        public static final int EID_EXT_FUTURE_CHANNEL_GUIDANCE = 14;
        public static final int EID_EXT_HE_CAPABILITIES = 35;
        public static final int EID_EXT_HE_OPERATION = 36;
        public static final int EID_EXT_KEY_DELIVERY = 7;
        public static final int EID_EXT_OWE_DH_PARAM = 32;
        public static final int EID_HT_CAPABILITIES = 45;
        public static final int EID_HT_OPERATION = 61;
        public static final int EID_INTERWORKING = 107;
        public static final int EID_ROAMING_CONSORTIUM = 111;
        public static final int EID_RSN = 48;
        public static final int EID_SSID = 0;
        public static final int EID_SUPPORTED_RATES = 1;
        public static final int EID_TIM = 5;
        public static final int EID_VHT_CAPABILITIES = 191;
        public static final int EID_VHT_OPERATION = 192;
        public static final int EID_VSA = 221;
        public static final int EID_WAPI = 68;
        public byte[] bytes;

        /* renamed from: id */
        public int f56id;

        public InformationElement() {
        }

        public InformationElement(InformationElement rhs) {
            this.f56id = rhs.f56id;
            this.bytes = (byte[]) rhs.bytes.clone();
        }
    }

    public MobileWipsScanResult(MobileWipsWifiSsid mobileWipsWifiSsid2, String BSSID2, long hessid2, int anqpDomainId2, byte[] osuProviders, String caps, int level2, int frequency2, long tsf) {
        MobileWipsWifiSsid mobileWipsWifiSsid3 = mobileWipsWifiSsid2;
        byte[] bArr = osuProviders;
        this.mobileWipsWifiSsid = mobileWipsWifiSsid3;
        this.SSID = mobileWipsWifiSsid3 != null ? mobileWipsWifiSsid2.toString() : MobileWipsWifiSsid.NONE;
        this.BSSID = BSSID2;
        this.hessid = hessid2;
        this.anqpDomainId = anqpDomainId2;
        if (bArr != null) {
            this.anqpElements = new AnqpInformationElement[1];
            this.anqpElements[0] = new AnqpInformationElement(5271450, 8, bArr);
        }
        this.capabilities = caps;
        this.level = level2;
        this.frequency = frequency2;
        this.timestamp = tsf;
        this.distanceCm = -1;
        this.distanceSdCm = -1;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public MobileWipsScanResult(MobileWipsWifiSsid mobileWipsWifiSsid2, String BSSID2, String caps, int level2, int frequency2, long tsf, int distCm, int distSdCm) {
        this.mobileWipsWifiSsid = mobileWipsWifiSsid2;
        this.SSID = mobileWipsWifiSsid2 != null ? mobileWipsWifiSsid2.toString() : MobileWipsWifiSsid.NONE;
        this.BSSID = BSSID2;
        this.capabilities = caps;
        this.level = level2;
        this.frequency = frequency2;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = -1;
        this.centerFreq0 = -1;
        this.centerFreq1 = -1;
        this.flags = 0;
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public MobileWipsScanResult(String Ssid, String BSSID2, long hessid2, int anqpDomainId2, String caps, int level2, int frequency2, long tsf, int distCm, int distSdCm, int channelWidth2, int centerFreq02, int centerFreq12, boolean is80211McRTTResponder2) {
        this.SSID = Ssid;
        this.BSSID = BSSID2;
        this.hessid = hessid2;
        this.anqpDomainId = anqpDomainId2;
        this.capabilities = caps;
        this.level = level2;
        this.frequency = frequency2;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
        this.channelWidth = channelWidth2;
        this.centerFreq0 = centerFreq02;
        this.centerFreq1 = centerFreq12;
        if (is80211McRTTResponder2) {
            this.flags = 2;
        } else {
            this.flags = 0;
        }
        this.isCarrierAp = false;
        this.carrierApEapType = -1;
        this.carrierName = null;
        this.radioChainInfos = null;
    }

    public MobileWipsScanResult(MobileWipsWifiSsid mobileWipsWifiSsid2, String Ssid, String BSSID2, long hessid2, int anqpDomainId2, String caps, int level2, int frequency2, long tsf, int distCm, int distSdCm, int channelWidth2, int centerFreq02, int centerFreq12, boolean is80211McRTTResponder2) {
        this(Ssid, BSSID2, hessid2, anqpDomainId2, caps, level2, frequency2, tsf, distCm, distSdCm, channelWidth2, centerFreq02, centerFreq12, is80211McRTTResponder2);
        this.mobileWipsWifiSsid = mobileWipsWifiSsid2;
    }

    public MobileWipsScanResult(MobileWipsScanResult source) {
        if (source != null) {
            this.mobileWipsWifiSsid = source.mobileWipsWifiSsid;
            this.SSID = source.SSID;
            this.BSSID = source.BSSID;
            this.hessid = source.hessid;
            this.anqpDomainId = source.anqpDomainId;
            this.informationElements = source.informationElements;
            this.anqpElements = source.anqpElements;
            this.capabilities = source.capabilities;
            this.level = source.level;
            this.frequency = source.frequency;
            this.channelWidth = source.channelWidth;
            this.centerFreq0 = source.centerFreq0;
            this.centerFreq1 = source.centerFreq1;
            this.timestamp = source.timestamp;
            this.distanceCm = source.distanceCm;
            this.distanceSdCm = source.distanceSdCm;
            this.seen = source.seen;
            this.untrusted = source.untrusted;
            this.numUsage = source.numUsage;
            this.venueName = source.venueName;
            this.operatorFriendlyName = source.operatorFriendlyName;
            this.flags = source.flags;
            this.isCarrierAp = source.isCarrierAp;
            this.carrierApEapType = source.carrierApEapType;
            this.carrierName = source.carrierName;
            this.radioChainInfos = source.radioChainInfos;
            this.wifiMode = source.wifiMode;
            this.semVendorSpecificInfo = source.semVendorSpecificInfo;
            this.semBssLoadElement = source.semBssLoadElement;
            this.semKtVendorSpecificInfo = source.semKtVendorSpecificInfo;
        }
    }

    public MobileWipsScanResult() {
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SSID: ");
        Object obj = this.mobileWipsWifiSsid;
        if (obj == null) {
            obj = MobileWipsWifiSsid.NONE;
        }
        sb.append(obj);
        sb.append(", BSSID: ");
        String str = this.BSSID;
        if (str == null) {
            str = "<none>";
        }
        sb.append(str);
        sb.append(", capabilities: ");
        String str2 = this.capabilities;
        if (str2 == null) {
            str2 = "<none>";
        }
        sb.append(str2);
        sb.append(", level: ");
        sb.append(this.level);
        sb.append(", frequency: ");
        sb.append(this.frequency);
        sb.append(", timestamp: ");
        sb.append(this.timestamp);
        sb.append(", distance: ");
        int i = this.distanceCm;
        Object obj2 = "?";
        sb.append(i != -1 ? Integer.valueOf(i) : obj2);
        sb.append("(cm)");
        sb.append(", distanceSd: ");
        int i2 = this.distanceSdCm;
        if (i2 != -1) {
            obj2 = Integer.valueOf(i2);
        }
        sb.append(obj2);
        sb.append("(cm)");
        sb.append(", passpoint: ");
        String str3 = "yes";
        sb.append((this.flags & 1) != 0 ? str3 : "no");
        sb.append(", ChannelBandwidth: ");
        sb.append(this.channelWidth);
        sb.append(", centerFreq0: ");
        sb.append(this.centerFreq0);
        sb.append(", centerFreq1: ");
        sb.append(this.centerFreq1);
        sb.append(", 80211mcResponder: ");
        sb.append((this.flags & 2) != 0 ? "is supported" : "is not supported");
        sb.append(", Carrier AP: ");
        if (!this.isCarrierAp) {
            str3 = "no";
        }
        sb.append(str3);
        sb.append(", Carrier AP EAP Type: ");
        sb.append(this.carrierApEapType);
        sb.append(", Carrier name: ");
        sb.append(this.carrierName);
        sb.append(", Radio Chain Infos: ");
        sb.append(Arrays.toString(this.radioChainInfos));
        sb.append(", wifiMode: ");
        sb.append(this.wifiMode);
        sb.append(", semVendorSpecificInfo: ");
        sb.append(this.semVendorSpecificInfo);
        sb.append(", semBssLoadElement: ");
        sb.append(this.semBssLoadElement);
        sb.append(", semKtVendorSpecificInfo: ");
        sb.append(this.semKtVendorSpecificInfo);
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags2) {
        if (this.mobileWipsWifiSsid != null) {
            dest.writeInt(1);
            this.mobileWipsWifiSsid.writeToParcel(dest, flags2);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.SSID);
        dest.writeString(this.BSSID);
        dest.writeLong(this.hessid);
        dest.writeInt(this.anqpDomainId);
        dest.writeString(this.capabilities);
        dest.writeInt(this.level);
        dest.writeInt(this.frequency);
        dest.writeLong(this.timestamp);
        dest.writeInt(this.distanceCm);
        dest.writeInt(this.distanceSdCm);
        dest.writeInt(this.channelWidth);
        dest.writeInt(this.centerFreq0);
        dest.writeInt(this.centerFreq1);
        dest.writeLong(this.seen);
        dest.writeInt(this.untrusted ? 1 : 0);
        dest.writeInt(this.numUsage);
        CharSequence charSequence = this.venueName;
        String str = "";
        dest.writeString(charSequence != null ? charSequence.toString() : str);
        CharSequence charSequence2 = this.operatorFriendlyName;
        dest.writeString(charSequence2 != null ? charSequence2.toString() : str);
        dest.writeLong(this.flags);
        InformationElement[] informationElementArr = this.informationElements;
        if (informationElementArr != null) {
            dest.writeInt(informationElementArr.length);
            int i = 0;
            while (true) {
                InformationElement[] informationElementArr2 = this.informationElements;
                if (i >= informationElementArr2.length) {
                    break;
                }
                dest.writeInt(informationElementArr2[i].f56id);
                dest.writeInt(this.informationElements[i].bytes.length);
                dest.writeByteArray(this.informationElements[i].bytes);
                i++;
            }
        } else {
            dest.writeInt(0);
        }
        List<String> list = this.anqpLines;
        if (list != null) {
            dest.writeInt(list.size());
            for (int i2 = 0; i2 < this.anqpLines.size(); i2++) {
                dest.writeString(this.anqpLines.get(i2));
            }
        } else {
            dest.writeInt(0);
        }
        AnqpInformationElement[] anqpInformationElementArr = this.anqpElements;
        if (anqpInformationElementArr != null) {
            dest.writeInt(anqpInformationElementArr.length);
            for (AnqpInformationElement element : this.anqpElements) {
                dest.writeInt(element.getVendorId());
                dest.writeInt(element.getElementId());
                dest.writeInt(element.getPayload().length);
                dest.writeByteArray(element.getPayload());
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.isCarrierAp ? 1 : 0);
        dest.writeInt(this.carrierApEapType);
        dest.writeString(this.carrierName);
        RadioChainInfo[] radioChainInfoArr = this.radioChainInfos;
        if (radioChainInfoArr != null) {
            dest.writeInt(radioChainInfoArr.length);
            int i3 = 0;
            while (true) {
                RadioChainInfo[] radioChainInfoArr2 = this.radioChainInfos;
                if (i3 >= radioChainInfoArr2.length) {
                    break;
                }
                dest.writeInt(radioChainInfoArr2[i3].f57id);
                dest.writeInt(this.radioChainInfos[i3].level);
                i3++;
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.wifiMode);
        String str2 = this.semVendorSpecificInfo;
        dest.writeString(str2 != null ? str2.toString() : str);
        String str3 = this.semBssLoadElement;
        dest.writeString(str3 != null ? str3.toString() : str);
        String str4 = this.semKtVendorSpecificInfo;
        if (str4 != null) {
            str = str4.toString();
        }
        dest.writeString(str);
    }
}
