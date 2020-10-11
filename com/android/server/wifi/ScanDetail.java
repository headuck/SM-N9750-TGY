package com.android.server.wifi;

import android.net.wifi.AnqpInformationElement;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiSsid;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSFriendlyNameElement;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.hotspot2.anqp.VenueNameElement;
import com.samsung.android.net.wifi.OpBrandingLoader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

public class ScanDetail {
    private static final OpBrandingLoader.Vendor mOpBranding = OpBrandingLoader.getInstance().getOpBranding();
    private volatile NetworkDetail mNetworkDetail;
    private final ScanResult mScanResult;
    private long mSeen;

    public ScanDetail(NetworkDetail networkDetail, WifiSsid wifiSsid, String bssid, String caps, int level, int frequency, long tsf, ScanResult.InformationElement[] informationElements, List<String> anqpLines) {
        this.mSeen = 0;
        this.mNetworkDetail = networkDetail;
        this.mScanResult = new ScanResult(wifiSsid, bssid, networkDetail.getHESSID(), networkDetail.getAnqpDomainID(), networkDetail.getOsuProviders(), caps, level, frequency, tsf);
        this.mSeen = System.currentTimeMillis();
        ScanResult scanResult = this.mScanResult;
        scanResult.seen = this.mSeen;
        scanResult.channelWidth = networkDetail.getChannelWidth();
        this.mScanResult.centerFreq0 = networkDetail.getCenterfreq0();
        this.mScanResult.centerFreq1 = networkDetail.getCenterfreq1();
        ScanResult scanResult2 = this.mScanResult;
        scanResult2.informationElements = informationElements;
        scanResult2.anqpLines = anqpLines;
        if (networkDetail.is80211McResponderSupport()) {
            this.mScanResult.setFlag(2);
        }
        if (networkDetail.isInterworking()) {
            this.mScanResult.setFlag(1);
        }
        this.mScanResult.wifiMode = networkDetail.getWifiMode();
        byte vsOuiType = this.mNetworkDetail.semGetVsOuiType();
        byte[] vsData = this.mNetworkDetail.semGetVsData();
        if (vsOuiType != 15 || vsData == null) {
            this.mScanResult.semVendorSpecificInfo = "null";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%02X", new Object[]{Byte.valueOf(vsOuiType)}));
            ByteBuffer bbuf = ByteBuffer.wrap(vsData).order(ByteOrder.BIG_ENDIAN);
            int i = 0;
            while (i < vsData.length) {
                try {
                    sb.append(String.format("%02X", new Object[]{Byte.valueOf(bbuf.get())}));
                    i++;
                } catch (BufferUnderflowException e) {
                }
            }
            this.mScanResult.semVendorSpecificInfo = sb.toString();
        }
        short stationCount = (short) this.mNetworkDetail.getStationCount();
        byte[] le_sc = leShortToByteArray(stationCount);
        byte channelUtilization = (byte) this.mNetworkDetail.getChannelUtilization();
        short capacity = (short) this.mNetworkDetail.getCapacity();
        byte[] le_cap = leShortToByteArray(capacity);
        if (stationCount == 0 && channelUtilization == 0 && capacity == 0) {
            this.mScanResult.semBssLoadElement = "null";
            return;
        }
        this.mScanResult.semBssLoadElement = String.format("%02X%02X", new Object[]{Byte.valueOf(le_sc[0]), Byte.valueOf(le_sc[1])}) + String.format("%02X", new Object[]{Byte.valueOf(channelUtilization)}) + String.format("%02X%02X", new Object[]{Byte.valueOf(le_cap[0]), Byte.valueOf(le_cap[1])});
    }

    public ScanDetail(WifiSsid wifiSsid, String bssid, String caps, int level, int frequency, long tsf, long seen) {
        this.mSeen = 0;
        this.mNetworkDetail = null;
        this.mScanResult = new ScanResult(wifiSsid, bssid, 0, -1, (byte[]) null, caps, level, frequency, tsf);
        this.mSeen = seen;
        ScanResult scanResult = this.mScanResult;
        scanResult.seen = this.mSeen;
        scanResult.channelWidth = 0;
        scanResult.centerFreq0 = 0;
        scanResult.centerFreq1 = 0;
        scanResult.flags = 0;
    }

    public ScanDetail(ScanResult scanResult, NetworkDetail networkDetail) {
        this.mSeen = 0;
        this.mScanResult = scanResult;
        this.mNetworkDetail = networkDetail;
        this.mSeen = this.mScanResult.seen == 0 ? System.currentTimeMillis() : this.mScanResult.seen;
    }

    public void propagateANQPInfo(Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        if (!anqpElements.isEmpty()) {
            this.mNetworkDetail = this.mNetworkDetail.complete(anqpElements);
            HSFriendlyNameElement fne = (HSFriendlyNameElement) anqpElements.get(Constants.ANQPElementType.HSFriendlyName);
            if (fne == null || fne.getNames().isEmpty()) {
                VenueNameElement vne = (VenueNameElement) anqpElements.get(Constants.ANQPElementType.ANQPVenueName);
                if (vne != null && !vne.getNames().isEmpty()) {
                    this.mScanResult.venueName = vne.getNames().get(0).getText();
                }
            } else {
                this.mScanResult.venueName = fne.getNames().get(0).getText();
            }
            RawByteElement osuProviders = (RawByteElement) anqpElements.get(Constants.ANQPElementType.HSOSUProviders);
            if (osuProviders != null) {
                ScanResult scanResult = this.mScanResult;
                scanResult.anqpElements = new AnqpInformationElement[1];
                scanResult.anqpElements[0] = new AnqpInformationElement(5271450, 8, osuProviders.getPayload());
            }
        }
    }

    public ScanResult getScanResult() {
        return this.mScanResult;
    }

    public NetworkDetail getNetworkDetail() {
        return this.mNetworkDetail;
    }

    public String getSSID() {
        return this.mNetworkDetail == null ? this.mScanResult.SSID : this.mNetworkDetail.getSSID();
    }

    public String getBSSIDString() {
        return this.mNetworkDetail == null ? this.mScanResult.BSSID : this.mNetworkDetail.getBSSIDString();
    }

    public String toKeyString() {
        NetworkDetail networkDetail = this.mNetworkDetail;
        if (networkDetail != null) {
            return networkDetail.toKeyString();
        }
        return String.format("'%s':%012x", new Object[]{this.mScanResult.BSSID, Long.valueOf(Utils.parseMac(this.mScanResult.BSSID))});
    }

    public long getSeen() {
        return this.mSeen;
    }

    public long setSeen() {
        this.mSeen = System.currentTimeMillis();
        ScanResult scanResult = this.mScanResult;
        long j = this.mSeen;
        scanResult.seen = j;
        return j;
    }

    public String toString() {
        try {
            return String.format("'%s'/%012x", new Object[]{this.mScanResult.SSID, Long.valueOf(Utils.parseMac(this.mScanResult.BSSID))});
        } catch (IllegalArgumentException e) {
            return String.format("'%s'/----", new Object[]{this.mScanResult.BSSID});
        }
    }

    private static byte[] leShortToByteArray(short i) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(i);
        return bb.array();
    }
}
