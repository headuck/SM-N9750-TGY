package com.android.server.wifi.util;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;
import java.io.PrintWriter;
import java.util.List;

public class ScanResultUtil {
    private ScanResultUtil() {
    }

    public static ScanDetail toScanDetail(ScanResult scanResult) {
        return new ScanDetail(scanResult, new NetworkDetail(scanResult.BSSID, scanResult.informationElements, scanResult.anqpLines, scanResult.frequency));
    }

    public static boolean isScanResultForPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK");
    }

    public static boolean isScanResultForEapNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP");
    }

    public static boolean isScanResultForEapSuiteBNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SUITE-B-192");
    }

    public static boolean isScanResultForWepNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WEP");
    }

    public static boolean isScanResultForOweNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE");
    }

    public static boolean isScanResultForOweTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("OWE_TRANSITION");
    }

    public static boolean isScanResultForSaeNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("SAE");
    }

    public static boolean isScanResultForPskSaeTransitionNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK+SAE") || scanResult.capabilities.contains("PSK-SHA256+SAE");
    }

    public static boolean isScanResultForWapiPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-PSK");
    }

    public static boolean isScanResultForWapiCertNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-CERT");
    }

    public static boolean isScanResultForOpenNetwork(ScanResult scanResult) {
        return !isScanResultForWepNetwork(scanResult) && !isScanResultForPskNetwork(scanResult) && !isScanResultForEapNetwork(scanResult) && !isScanResultForSaeNetwork(scanResult) && !isScanResultForWapiCertNetwork(scanResult) && !isScanResultForWapiPskNetwork(scanResult) && !isScanResultForEapSuiteBNetwork(scanResult);
    }

    @VisibleForTesting
    public static String createQuotedSSID(String ssid) {
        return "\"" + ssid + "\"";
    }

    public static WifiConfiguration createNetworkFromScanResult(ScanResult scanResult) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = createQuotedSSID(scanResult.SSID);
        setAllowedKeyManagementFromScanResult(scanResult, config);
        return config;
    }

    public static void setAllowedKeyManagementFromScanResult(ScanResult scanResult, WifiConfiguration config) {
        if (isScanResultForSaeNetwork(scanResult)) {
            config.setSecurityParams(4);
        } else if (isScanResultForPskNetwork(scanResult)) {
            config.setSecurityParams(2);
        } else if (isScanResultForEapSuiteBNetwork(scanResult)) {
            config.setSecurityParams(5);
        } else if (isScanResultForEapNetwork(scanResult)) {
            config.setSecurityParams(3);
        } else if (isScanResultForWepNetwork(scanResult)) {
            config.setSecurityParams(1);
        } else if (isScanResultForOweNetwork(scanResult)) {
            config.setSecurityParams(6);
        } else {
            config.setSecurityParams(0);
        }
    }

    public static void dumpScanResults(PrintWriter pw, List<ScanResult> scanResults, long nowMs) {
        String age;
        String rssiInfo;
        PrintWriter printWriter = pw;
        if (scanResults != null && scanResults.size() != 0) {
            printWriter.println("    BSSID              Frequency      RSSI      BSS Load Element       Age(sec)     SSID                                 Flags");
            for (ScanResult r : scanResults) {
                long timeStampMs = r.timestamp / 1000;
                if (timeStampMs <= 0) {
                    age = "___?___";
                } else if (nowMs < timeStampMs) {
                    age = "  0.000";
                } else if (timeStampMs < nowMs - 1000000) {
                    age = ">1000.0";
                } else {
                    age = String.format("%3.3f", new Object[]{Double.valueOf(((double) (nowMs - timeStampMs)) / 1000.0d)});
                }
                String hs20 = "";
                String ssid = r.SSID == null ? hs20 : r.SSID;
                if (ArrayUtils.size(r.radioChainInfos) == 1) {
                    rssiInfo = String.format("%5d(%1d:%3d)       ", new Object[]{Integer.valueOf(r.level), Integer.valueOf(r.radioChainInfos[0].id), Integer.valueOf(r.radioChainInfos[0].level)});
                } else if (ArrayUtils.size(r.radioChainInfos) == 2) {
                    rssiInfo = String.format("%5d(%1d:%3d/%1d:%3d)", new Object[]{Integer.valueOf(r.level), Integer.valueOf(r.radioChainInfos[0].id), Integer.valueOf(r.radioChainInfos[0].level), Integer.valueOf(r.radioChainInfos[1].id), Integer.valueOf(r.radioChainInfos[1].level)});
                } else {
                    rssiInfo = String.format("%9d         ", new Object[]{Integer.valueOf(r.level)});
                }
                printWriter.printf("  %17s  %9d  %18s %10s  %15s    %-32s  %s", new Object[]{r.BSSID, Integer.valueOf(r.frequency), rssiInfo, r.semBssLoadElement, age, String.format("%1.32s", new Object[]{ssid}), r.capabilities});
                if ((r.flags & 1) != 0) {
                    hs20 = "[HS20]";
                }
                printWriter.printf("%s\n", new Object[]{hs20});
            }
        }
    }
}
