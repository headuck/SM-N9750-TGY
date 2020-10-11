package com.android.server.wifi.hotspot2;

import android.net.RssiCurve;
import android.net.wifi.ScanResult;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSWanMetricsElement;
import com.android.server.wifi.hotspot2.anqp.IPAddressTypeAvailabilityElement;
import com.android.server.wifi.util.GeneralUtil;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import java.util.HashMap;
import java.util.Map;

public class PasspointNetworkScore {
    private static final int BAND_5GHZ_AWARD = 20;
    private static final int BAND_5GHZ_CHANNEL_EXTRA_AWARD = 10;
    private static final int BLUETOOTH_CONNECTION_ACTIVE_PENALTY = 50;
    private static final RssiCurve CHANNEL_UTILIZATION_SCORES = new RssiCurve(0, 85, new byte[]{1, 0, -1});
    private static final RssiCurve ESTIMATED_AIR_TIME_FRACTION_SCORES = new RssiCurve(0, 85, new byte[]{-1, 0, 1});
    @VisibleForTesting
    public static final int HOME_PROVIDER_AWARD = 100;
    @VisibleForTesting
    public static final int INTERNET_ACCESS_AWARD = 50;
    private static final Map<Integer, Integer> IPV4_SCORES = new HashMap();
    private static final Map<Integer, Integer> IPV6_SCORES = new HashMap();
    private static final Map<NetworkDetail.Ant, Integer> NETWORK_TYPE_SCORES = new HashMap();
    @VisibleForTesting
    public static final int PERSONAL_OR_EMERGENCY_NETWORK_AWARDS = 2;
    @VisibleForTesting
    public static final int PUBLIC_OR_PRIVATE_NETWORK_AWARDS = 4;
    @VisibleForTesting
    public static final int RESTRICTED_OR_UNKNOWN_IP_AWARDS = 1;
    @VisibleForTesting
    public static final RssiCurve RSSI_SCORE = new RssiCurve(-80, 20, new byte[]{-10, 0, 10, 20, 30, SemWifiApSmartUtil.BLE_BATT_5}, 20);
    @VisibleForTesting
    public static final int UNRESTRICTED_IP_AWARDS = 2;
    @VisibleForTesting
    public static final int WAN_PORT_DOWN_OR_CAPPED_PENALTY = 50;

    static {
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.FreePublic, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.ChargeablePublic, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.PrivateWithGuest, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Private, 4);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Personal, 2);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.EmergencyOnly, 2);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.Wildcard, 0);
        NETWORK_TYPE_SCORES.put(NetworkDetail.Ant.TestOrExperimental, 0);
        IPV4_SCORES.put(0, 0);
        IPV4_SCORES.put(2, 1);
        IPV4_SCORES.put(5, 1);
        IPV4_SCORES.put(6, 1);
        IPV4_SCORES.put(7, 1);
        IPV4_SCORES.put(1, 2);
        IPV4_SCORES.put(3, 2);
        IPV4_SCORES.put(4, 2);
        IPV6_SCORES.put(0, 0);
        IPV6_SCORES.put(2, 1);
        IPV6_SCORES.put(1, 2);
    }

    public static int calculateScore(boolean isHomeProvider, ScanDetail scanDetail, Map<Constants.ANQPElementType, ANQPElement> anqpElements, boolean isActiveNetwork, StringBuffer sbuf, boolean bluetoothConnected) {
        Map<Constants.ANQPElementType, ANQPElement> map = anqpElements;
        boolean z = isActiveNetwork;
        StringBuffer stringBuffer = sbuf;
        NetworkDetail networkDetail = scanDetail.getNetworkDetail();
        ScanResult scanResult = scanDetail.getScanResult();
        int score = 0;
        stringBuffer.append("[ ");
        stringBuffer.append(scanResult.SSID);
        stringBuffer.append(" ");
        stringBuffer.append(scanResult.BSSID);
        stringBuffer.append(" RSSI:");
        stringBuffer.append(scanResult.level);
        stringBuffer.append(" ] ");
        if (isHomeProvider) {
            score = 0 + 100;
            stringBuffer.append(" Home provider bonus: ");
            stringBuffer.append(100);
            stringBuffer.append(",");
        }
        int score2 = score + ((networkDetail.isInternet() ? 1 : -1) * 50);
        stringBuffer.append(" Internet accessibility score: ");
        stringBuffer.append((networkDetail.isInternet() ? 1 : -1) * 50);
        stringBuffer.append(",");
        Integer ndScore = NETWORK_TYPE_SCORES.get(networkDetail.getAnt());
        if (ndScore != null) {
            score2 += ndScore.intValue();
        }
        stringBuffer.append(" Network type score: ");
        stringBuffer.append(NETWORK_TYPE_SCORES.get(networkDetail.getAnt()));
        stringBuffer.append(",");
        if (map != null) {
            HSWanMetricsElement wm = (HSWanMetricsElement) map.get(Constants.ANQPElementType.HSWANMetrics);
            if (wm != null && (wm.getStatus() != 1 || wm.isCapped())) {
                score2 -= 50;
                stringBuffer.append(" Wan port down or capped penalty: ");
                stringBuffer.append(-50);
                stringBuffer.append(",");
            }
            IPAddressTypeAvailabilityElement ipa = (IPAddressTypeAvailabilityElement) map.get(Constants.ANQPElementType.ANQPIPAddrAvailability);
            if (ipa != null) {
                Integer v4Score = IPV4_SCORES.get(Integer.valueOf(ipa.getV4Availability()));
                Integer v6Score = IPV6_SCORES.get(Integer.valueOf(ipa.getV6Availability()));
                Integer v4Score2 = Integer.valueOf(v4Score != null ? v4Score.intValue() : 0);
                stringBuffer.append(" Ipv4 availability score: ");
                stringBuffer.append(v4Score2);
                stringBuffer.append(",");
                Integer v6Score2 = Integer.valueOf(v6Score != null ? v6Score.intValue() : 0);
                stringBuffer.append(" Ipv6 availability score: ");
                stringBuffer.append(v6Score2);
                stringBuffer.append(",");
                score2 += v4Score2.intValue() + v6Score2.intValue();
            }
        }
        int score3 = score2 + RSSI_SCORE.lookupScore(scanDetail.getScanResult().level, z);
        stringBuffer.append(" RSSI score: ");
        stringBuffer.append(RSSI_SCORE.lookupScore(scanDetail.getScanResult().level, z));
        stringBuffer.append(",");
        if (scanResult.is5GHz()) {
            score3 += 20;
            stringBuffer.append(" 5GHz bonus: ");
            stringBuffer.append(20);
            stringBuffer.append(",");
            if (scanResult.channelWidth >= 2) {
                score3 += 20;
                stringBuffer.append(" Channel bandwidth bonus: 20");
                stringBuffer.append(",");
            }
            if (networkDetail.getWifiMode() > 5) {
                score3 += 20;
                stringBuffer.append(" Mode 11ax bonus: 20");
                stringBuffer.append(",");
            }
            if (GeneralUtil.isDomesticModel() && GeneralUtil.isDomesticDfsChannel(scanResult.frequency)) {
                score3 += 10;
                stringBuffer.append(" DFS channel bonus: 10");
                stringBuffer.append(",");
            }
        }
        if (bluetoothConnected && scanResult.is24GHz()) {
            score3 -= 50;
            stringBuffer.append(" Active Bluetooth connection penalty: ");
            stringBuffer.append(-50);
            stringBuffer.append(",");
        }
        int score4 = score3 + CHANNEL_UTILIZATION_SCORES.lookupScore(networkDetail.getChannelUtilization());
        stringBuffer.append(" Channel utilization award: ");
        stringBuffer.append(CHANNEL_UTILIZATION_SCORES.lookupScore(networkDetail.getChannelUtilization()));
        stringBuffer.append(",");
        int estimatedAirTimeFraction = networkDetail.getEstimatedAirTimeFraction(1);
        if (estimatedAirTimeFraction != -1) {
            score4 += ESTIMATED_AIR_TIME_FRACTION_SCORES.lookupScore(estimatedAirTimeFraction);
            stringBuffer.append(" Estimated air fractional time (AC_BE) award: ");
            stringBuffer.append(ESTIMATED_AIR_TIME_FRACTION_SCORES.lookupScore(estimatedAirTimeFraction));
            stringBuffer.append(",");
        }
        stringBuffer.append(" ## Total score: ");
        stringBuffer.append(score4);
        stringBuffer.append("\n");
        return score4;
    }
}
