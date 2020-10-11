package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.Objects;

public class ScanResultMatchInfo {
    public boolean isPasspoint;
    public String networkSsid;
    public int networkType;
    public boolean oweInTransitionMode;
    public boolean pskSaeInTransitionMode;

    public static int getNetworkType(WifiConfiguration config) {
        if (WifiConfigurationUtil.isConfigForSaeNetwork(config)) {
            return 4;
        }
        if (WifiConfigurationUtil.isConfigForPskNetwork(config)) {
            return 2;
        }
        if (WifiConfigurationUtil.isConfigForEapNetwork(config)) {
            return 3;
        }
        if (WifiConfigurationUtil.isConfigForEapSuiteBNetwork(config)) {
            return 5;
        }
        if (WifiConfigurationUtil.isConfigForWapiPskNetwork(config)) {
            return 100;
        }
        if (WifiConfigurationUtil.isConfigForWapiCertNetwork(config)) {
            return 101;
        }
        if (WifiConfigurationUtil.isConfigForWepNetwork(config)) {
            return 1;
        }
        if (WifiConfigurationUtil.isConfigForOweNetwork(config)) {
            return 6;
        }
        if (WifiConfigurationUtil.isConfigForOpenNetwork(config)) {
            return 0;
        }
        throw new IllegalArgumentException("Invalid WifiConfiguration: " + config);
    }

    public static ScanResultMatchInfo fromWifiConfiguration(WifiConfiguration config) {
        ScanResultMatchInfo info = new ScanResultMatchInfo();
        info.networkSsid = config.SSID;
        info.networkType = getNetworkType(config);
        info.isPasspoint = config.isPasspoint();
        return info;
    }

    public static int getNetworkType(ScanResult scanResult) {
        if (ScanResultUtil.isScanResultForSaeNetwork(scanResult)) {
            return 4;
        }
        if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
            return 2;
        }
        if (ScanResultUtil.isScanResultForWapiPskNetwork(scanResult)) {
            return 100;
        }
        if (ScanResultUtil.isScanResultForWapiCertNetwork(scanResult)) {
            return 101;
        }
        if (ScanResultUtil.isScanResultForEapSuiteBNetwork(scanResult)) {
            return 5;
        }
        if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
            return 3;
        }
        if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
            return 1;
        }
        if (ScanResultUtil.isScanResultForOweNetwork(scanResult)) {
            return 6;
        }
        if (ScanResultUtil.isScanResultForOpenNetwork(scanResult)) {
            return 0;
        }
        throw new IllegalArgumentException("Invalid ScanResult: " + scanResult);
    }

    public static ScanResultMatchInfo fromScanResult(ScanResult scanResult) {
        return fromScanResult(scanResult, false);
    }

    public static ScanResultMatchInfo fromScanResult(ScanResult scanResult, boolean onlyPasspoint) {
        ScanResultMatchInfo info = new ScanResultMatchInfo();
        info.networkSsid = ScanResultUtil.createQuotedSSID(scanResult.SSID);
        info.networkType = getNetworkType(scanResult);
        info.oweInTransitionMode = false;
        info.pskSaeInTransitionMode = false;
        int i = info.networkType;
        if (i == 4) {
            info.pskSaeInTransitionMode = ScanResultUtil.isScanResultForPskSaeTransitionNetwork(scanResult);
        } else if (i == 6) {
            info.oweInTransitionMode = ScanResultUtil.isScanResultForOweTransitionNetwork(scanResult);
        }
        info.isPasspoint = onlyPasspoint;
        return info;
    }

    public boolean equals(Object otherObj) {
        boolean networkTypeEquals = true;
        if (this == otherObj) {
            return true;
        }
        if (!(otherObj instanceof ScanResultMatchInfo)) {
            return false;
        }
        ScanResultMatchInfo other = (ScanResultMatchInfo) otherObj;
        if (!Objects.equals(this.networkSsid, other.networkSsid)) {
            return false;
        }
        if ((other.pskSaeInTransitionMode && this.networkType == 2) || (this.pskSaeInTransitionMode && other.networkType == 2)) {
            networkTypeEquals = true;
        } else if ((this.networkType == 0 && other.oweInTransitionMode) || (this.oweInTransitionMode && other.networkType == 0)) {
            networkTypeEquals = true;
        } else if (this.networkType != other.networkType) {
            networkTypeEquals = false;
        }
        if (!networkTypeEquals || this.isPasspoint == other.isPasspoint) {
            return networkTypeEquals;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.networkSsid});
    }

    public String toString() {
        return "ScanResultMatchInfo: ssid: " + this.networkSsid + ", type: " + this.networkType;
    }
}
