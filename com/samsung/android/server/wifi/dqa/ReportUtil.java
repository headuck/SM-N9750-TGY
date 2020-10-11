package com.samsung.android.server.wifi.dqa;

import android.net.DhcpResults;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

public class ReportUtil {
    private static final String TAG = "WifiIssueDetector";

    public static Bundle getReportDataFromBigDataParamsOfDISC(String dataString, int apInteralType, int internalReason, int netId) {
        String[] dataArray = dataString.split(" ");
        if (dataArray.length == 22 || dataArray.length == 24 || dataArray.length == 25) {
            Bundle reportData = new Bundle();
            int indexOfData = 0 + 1;
            reportData.putString(ReportIdKey.KEY_WPA_SECURE_TYPE, dataArray[0]);
            int indexOfData2 = indexOfData + 1;
            reportData.putString(ReportIdKey.KEY_WPA_STATE, dataArray[indexOfData]);
            int indexOfData3 = indexOfData2 + 1;
            reportData.putString(ReportIdKey.KEY_SCAN_COUNT, dataArray[indexOfData2]);
            int indexOfData4 = indexOfData3 + 1;
            reportData.putString(ReportIdKey.KEY_SCAN_COUNT_SAME_CHANNEL, dataArray[indexOfData3]);
            int indexOfData5 = indexOfData4 + 1;
            reportData.putString(ReportIdKey.KEY_DISCONNECT_REASON, dataArray[indexOfData4]);
            reportData.putString(ReportIdKey.KEY_LOCALLY_GENERATED, dataArray[indexOfData5]);
            int indexOfData6 = indexOfData5 + 1 + 1;
            int indexOfData7 = indexOfData6 + 1;
            reportData.putString(ReportIdKey.KEY_OUI, dataArray[indexOfData6]);
            int indexOfData8 = indexOfData7 + 1;
            reportData.putString(ReportIdKey.KEY_FREQUENCY, dataArray[indexOfData7]);
            int indexOfData9 = indexOfData8 + 1;
            reportData.putString(ReportIdKey.KEY_BAND_WIDTH, dataArray[indexOfData8]);
            int indexOfData10 = indexOfData9 + 1;
            reportData.putString(ReportIdKey.KEY_RSSI, dataArray[indexOfData9]);
            int indexOfData11 = indexOfData10 + 1;
            reportData.putString(ReportIdKey.KEY_DATA_RATE, dataArray[indexOfData10]);
            int indexOfData12 = indexOfData11 + 1;
            reportData.putString(ReportIdKey.KEY_80211_MODE, dataArray[indexOfData11]);
            int indexOfData13 = indexOfData12 + 1;
            reportData.putString(ReportIdKey.KEY_ANTENNA, dataArray[indexOfData12]);
            int indexOfData14 = indexOfData13 + 1;
            reportData.putString(ReportIdKey.KEY_MU_MIMO, dataArray[indexOfData13]);
            int indexOfData15 = indexOfData14 + 1;
            reportData.putString(ReportIdKey.KEY_PASSPOINT, dataArray[indexOfData14]);
            int indexOfData16 = indexOfData15 + 1;
            reportData.putString(ReportIdKey.KEY_SNR, dataArray[indexOfData15]);
            int indexOfData17 = indexOfData16 + 1;
            reportData.putString(ReportIdKey.KEY_NOISE, dataArray[indexOfData16]);
            int indexOfData18 = indexOfData17 + 1;
            reportData.putString(ReportIdKey.KEY_AKM, dataArray[indexOfData17]);
            int indexOfData19 = indexOfData18 + 1;
            reportData.putString(ReportIdKey.KEY_ROAMING_COUNT, dataArray[indexOfData18]);
            int indexOfData20 = indexOfData19 + 1;
            reportData.putString(ReportIdKey.KEY_11KV, dataArray[indexOfData19]);
            int indexOfData21 = indexOfData20 + 1;
            reportData.putString(ReportIdKey.KEY_11KV_IE, dataArray[indexOfData20]);
            if (dataArray.length == 24 || dataArray.length == 25) {
                int indexOfData22 = indexOfData21 + 1;
                reportData.putString(ReportIdKey.KEY_ROAMING_FULLS_SCAN_COUNT, dataArray[indexOfData21]);
                int indexOfData23 = indexOfData22 + 1;
                reportData.putString(ReportIdKey.KEY_ROAMING_PARTIAL_SCAN_COUNT, dataArray[indexOfData22]);
                if (dataArray.length == 25) {
                    reportData.putString(ReportIdKey.KEY_ADPS_DISCONNECT, dataArray[indexOfData23]);
                    int i = indexOfData23 + 1;
                }
            }
            reportData.putInt(ReportIdKey.KEY_AP_INTERNAL_TYPE, apInteralType);
            reportData.putInt(ReportIdKey.KEY_AP_INTERNAL_REASON, internalReason);
            reportData.putInt(ReportIdKey.KEY_NET_ID, netId);
            return reportData;
        }
        Log.e("WifiIssueDetector", "bigdata DISC args length changed " + dataArray.length);
        return null;
    }

    public static Bundle getReportDataFromBigDataParamsOfASSO(String dataString, int netId) {
        String[] dataArray = dataString.split(" ");
        if (dataArray.length == 9) {
            Bundle reportData = new Bundle();
            int indexOfData = 0 + 1;
            reportData.putString("reason", dataArray[indexOfData]);
            int indexOfData2 = indexOfData + 1 + 1;
            int indexOfData3 = indexOfData2 + 1;
            reportData.putString(ReportIdKey.KEY_WPA_SECURE_TYPE, dataArray[indexOfData2]);
            int indexOfData4 = indexOfData3 + 1;
            reportData.putString(ReportIdKey.KEY_SCAN_COUNT, dataArray[indexOfData3]);
            int indexOfData5 = indexOfData4 + 1;
            reportData.putString(ReportIdKey.KEY_SCAN_COUNT_SAME_CHANNEL, dataArray[indexOfData4]);
            int indexOfData6 = indexOfData5 + 1;
            reportData.putString(ReportIdKey.KEY_FREQUENCY, dataArray[indexOfData5]);
            int indexOfData7 = indexOfData6 + 1;
            reportData.putString(ReportIdKey.KEY_RSSI, dataArray[indexOfData6]);
            int i = indexOfData7 + 1;
            reportData.putString(ReportIdKey.KEY_OUI, dataArray[indexOfData7]);
            reportData.putInt(ReportIdKey.KEY_NET_ID, netId);
            return reportData;
        }
        Log.e("WifiIssueDetector", "bigdata ASSO args length changed " + dataArray.length);
        return null;
    }

    public static Bundle getReportDataFromBigDataParamsOfONOF(String dataString) {
        String[] dataArray = dataString.split(" ");
        if (dataArray.length == 4) {
            Bundle reportData = new Bundle();
            reportData.putString(ReportIdKey.KEY_WIFI_STATE, dataArray[0]);
            reportData.putString(ReportIdKey.KEY_CALL_BY, dataArray[1]);
            reportData.putString(ReportIdKey.KEY_WIFI_CONNECTED, dataArray[2]);
            reportData.putString(ReportIdKey.KEY_SNS_STATE, dataArray[3]);
            return reportData;
        }
        Log.e("WifiIssueDetector", "bigdata ONOF args length changed " + dataArray.length);
        return null;
    }

    public static Bundle getReportDataForSupplicantStartFail(int failureCount) {
        Bundle reportData = getReportDataForStateMachine();
        reportData.putInt(ReportIdKey.KEY_COUNT, failureCount);
        return reportData;
    }

    public static Bundle getReportDataForFwHang(String hangMessage) {
        Bundle reportData = getReportDataForStateMachine();
        if (hangMessage != null) {
            String[] hangArray = hangMessage.split(" ");
            if (hangArray.length >= 2) {
                reportData.putString("reason", hangArray[1]);
            }
        }
        return reportData;
    }

    public static Bundle getReportDataForInitDelay(int durationSeconds) {
        Bundle reportData = getReportDataForStateMachine();
        reportData.putInt(ReportIdKey.KEY_DELAY_SECONDS, durationSeconds);
        return reportData;
    }

    public static Bundle getReportDataForHidlDeath(int reason) {
        Bundle reportData = getReportDataForStateMachine();
        reportData.putInt("reason", reason);
        return reportData;
    }

    public static Bundle getReportDataForStateMachine() {
        Bundle reportData = new Bundle();
        reportData.putString(ReportIdKey.KEY_LAST_HANDLE_STATE, ReportStore.getInstance().getLastWifiStateMachineStateName());
        reportData.putInt(ReportIdKey.KEY_PROCESS_MESSAGE, ReportStore.getInstance().getLastProceedMessageId());
        reportData.putInt(ReportIdKey.KEY_PREV_PROCESS_MESSAGE, ReportStore.getInstance().getPrevProceedMessageId());
        return reportData;
    }

    public static Bundle getReportDataForChangeState(boolean state) {
        Bundle reportData = new Bundle();
        reportData.putInt("state", state);
        return reportData;
    }

    public static Bundle getReportDataForUnstableAp(int networkId, String bssid) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("bssid", bssid);
        return reportData;
    }

    public static Bundle getReportDataForTryToConnect(int networkId, String ssid, int numAssociation, String bssid, boolean isLinkDebouncing) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("ssid", ssid);
        reportData.putInt(ReportIdKey.KEY_NUM_ASSOCIATION, numAssociation);
        reportData.putString("bssid", bssid);
        reportData.putInt(ReportIdKey.KEY_IS_LINK_DEBOUNCING, isLinkDebouncing);
        return reportData;
    }

    public static Bundle getReportDataForL2Connected(int networkId, String bssid) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("bssid", bssid);
        return reportData;
    }

    public static Bundle getReportDataForL2ConnectFail(int networkId, String bssid) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("bssid", bssid);
        return reportData;
    }

    public static Bundle getReportDataForCallingSpecificApiFrequently(String apiName, String packageName, int count) {
        Bundle reportData = new Bundle();
        reportData.putString(ReportIdKey.KEY_API_NAME, apiName);
        reportData.putString(ReportIdKey.KEY_CALL_BY, packageName);
        reportData.putInt(ReportIdKey.KEY_COUNT, count);
        return reportData;
    }

    public static Bundle getReportDataForAuthFail(int networkId, int reasonCode, int networkStatus, int numAssociation, int networkSelectionStatus, int networkSelectionDisableReason) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putInt("reason", reasonCode);
        reportData.putInt(ReportIdKey.KEY_CONFIG_STATUS, networkStatus);
        reportData.putInt(ReportIdKey.KEY_NUM_ASSOCIATION, numAssociation);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_STATUS, networkSelectionStatus);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_DISABLE_REASON, networkSelectionDisableReason);
        return reportData;
    }

    public static Bundle getReportDataForAuthFailForEap(int networkId, int eapEvent, int networkStatus, int numAssociation, int networkSelectionStatus, int networkSelectionDisableReason) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putInt(ReportIdKey.KEY_EAP_EVENT, eapEvent);
        reportData.putInt(ReportIdKey.KEY_CONFIG_STATUS, networkStatus);
        reportData.putInt(ReportIdKey.KEY_NUM_ASSOCIATION, numAssociation);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_STATUS, networkSelectionStatus);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_DISABLE_REASON, networkSelectionDisableReason);
        return reportData;
    }

    public static Bundle getReportDataForAssocReject(int networkId, String bssid, int reasonCode, int networkStatus, int numAssociation, int networkSelectionStatus, int networkSelectionDisableReason) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("bssid", bssid);
        reportData.putInt("reason", reasonCode);
        reportData.putInt(ReportIdKey.KEY_CONFIG_STATUS, networkStatus);
        reportData.putInt(ReportIdKey.KEY_NUM_ASSOCIATION, numAssociation);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_STATUS, networkSelectionStatus);
        reportData.putInt(ReportIdKey.KEY_CONFIG_SELECTION_DISABLE_REASON, networkSelectionDisableReason);
        return reportData;
    }

    public static Bundle getReportDataForConnectTranstion(int apInteralType) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_AP_INTERNAL_TYPE, apInteralType);
        appendWifiInfo(reportData);
        appendDhcpInfo(reportData, false);
        return reportData;
    }

    public static Bundle getReportDataForDisconnectTranstion(boolean screenOn, int sleepPolicy, int adpsState) {
        Bundle reportData = getReportDataForStateMachine();
        reportData.putString(ReportIdKey.KEY_SCREEN_ON, screenOn ? "1" : "0");
        reportData.putInt(ReportIdKey.KEY_SLEEP_POLICY, sleepPolicy);
        reportData.putString(ReportIdKey.KEY_ADPS_STATE, Integer.toString(adpsState));
        reportData.putString(ReportIdKey.KEY_CONNECT_DURATION, Long.toString(ReportStore.getInstance().getConnectedDurationMin()));
        appendWifiInfo(reportData);
        appendDhcpInfo(reportData, true);
        return reportData;
    }

    private static void appendWifiInfo(Bundle reportData) {
        WifiInfo wifiInfo = ReportStore.getInstance().getLastWifiInfo();
        reportData.putString("ssid", wifiInfo.getSSID());
        reportData.putString("bssid", wifiInfo.getBSSID());
        reportData.putInt(ReportIdKey.KEY_NET_ID, wifiInfo.getNetworkId());
    }

    private static void appendDhcpInfo(Bundle reportData, boolean doClear) {
        DhcpResults dhcpResults = ReportStore.getInstance().getLastDhcpResults();
        if (dhcpResults != null) {
            reportData.putInt(ReportIdKey.KEY_DHCP, dhcpResults.leaseDuration != 0 ? 1 : 0);
            if (dhcpResults.ipAddress != null && (dhcpResults.ipAddress.getAddress() instanceof Inet4Address)) {
                reportData.putString(ReportIdKey.KEY_IP, dhcpResults.ipAddress.getAddress().getHostAddress());
            }
            if (dhcpResults.gateway != null) {
                reportData.putString(ReportIdKey.KEY_GATEWAY, dhcpResults.gateway.getHostAddress());
            }
            if (dhcpResults.ipAddress != null) {
                reportData.putInt(ReportIdKey.KEY_NETWORK_PREFIX, dhcpResults.ipAddress.getNetworkPrefixLength());
            }
            int dnsFound = 0;
            Iterator it = dhcpResults.dnsServers.iterator();
            while (it.hasNext()) {
                InetAddress dns = (InetAddress) it.next();
                if (dns instanceof Inet4Address) {
                    if (dnsFound == 0) {
                        reportData.putString(ReportIdKey.KEY_DNS1, dns.getHostAddress());
                    } else {
                        reportData.putString(ReportIdKey.KEY_DNS2, dns.getHostAddress());
                    }
                    dnsFound++;
                    if (dnsFound > 1) {
                        break;
                    }
                }
            }
            if (doClear) {
                dhcpResults.clear();
            }
        }
    }

    public static Bundle getReportDataForRoamingEnter(String type, String ssid, String bssid, int rssi) {
        Bundle reportData = getReportDataForStateMachine();
        reportData.putString(ReportIdKey.KEY_ROAMING_TYPE, type);
        reportData.putString("ssid", ssid);
        reportData.putString("bssid", bssid);
        reportData.putInt(ReportIdKey.KEY_RSSI, rssi);
        return reportData;
    }

    public static Bundle getReportDataForUnwantedMessage(int unwantedReason) {
        WifiInfo wifiInfo = ReportStore.getInstance().getLastWifiInfo();
        Bundle reportData = new Bundle();
        reportData.putString("ssid", wifiInfo.getSSID());
        reportData.putString("bssid", wifiInfo.getBSSID());
        reportData.putInt(ReportIdKey.KEY_UNWANTED_REASON, unwantedReason);
        return reportData;
    }

    public static Bundle getReportDataForDisconnectBySleepPolicy(int sleepPolicy) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_SLEEP_POLICY, sleepPolicy);
        return reportData;
    }

    public static Bundle getReportDataForRoamingDhcpStart(String ssid, String bssid, int roamDhcpMode, boolean isStartingNudProbe) {
        Bundle reportData = new Bundle();
        reportData.putString("ssid", ssid);
        reportData.putString("bssid", bssid);
        reportData.putInt(ReportIdKey.KEY_ROAMING_DHCP, roamDhcpMode);
        reportData.putString(ReportIdKey.KEY_ROAMING_DHCP_ENABLE_NUD_PROBE, isStartingNudProbe ? "1" : "0");
        return reportData;
    }

    public static Bundle getReportDataForWifiManagerConnectApi(boolean isAllowed, int networkId, String ssid, String apiName, String callUidName, boolean hasPassword) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_IS_ALLOWED, isAllowed);
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString("ssid", ssid);
        reportData.putString(ReportIdKey.KEY_API_NAME, apiName);
        reportData.putString(ReportIdKey.KEY_CALL_BY_UID, callUidName);
        reportData.putInt(ReportIdKey.KEY_HAS_PASSWORD, hasPassword);
        return reportData;
    }

    public static Bundle getReportDataForWifiManagerAddOrUpdateApi(int networkId, boolean hasPassword, String apiName, String callUidName, String packageName) {
        Bundle reportData = getReportDataForWifiManagerApi(networkId, apiName, callUidName, packageName);
        reportData.putInt(ReportIdKey.KEY_HAS_PASSWORD, hasPassword);
        return reportData;
    }

    public static Bundle getReportDataForWifiManagerApi(int networkId, String apiName, String callUidName, String packageName) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_NET_ID, networkId);
        reportData.putString(ReportIdKey.KEY_API_NAME, apiName);
        reportData.putString(ReportIdKey.KEY_CALL_BY_UID, callUidName);
        reportData.putString(ReportIdKey.KEY_CALL_BY, packageName);
        return reportData;
    }

    public static Bundle getReportDataForWifiManagerApi(String ssid, String apiName, String callUidName, String packageName) {
        Bundle reportData = new Bundle();
        reportData.putString("ssid", ssid);
        reportData.putString(ReportIdKey.KEY_API_NAME, apiName);
        reportData.putString(ReportIdKey.KEY_CALL_BY_UID, callUidName);
        reportData.putString(ReportIdKey.KEY_CALL_BY, packageName);
        return reportData;
    }

    public static Bundle getReportDataForDhcpResult(int reason) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_DHCP_FAIL_REASON, reason);
        if (reason == 1) {
            appendDhcpInfo(reportData, false);
        }
        return reportData;
    }

    public static Bundle getReportDatatForW24H(String params) {
        Bundle reportData = new Bundle();
        reportData.putString(ReportIdKey.KEY_W24H, params);
        return reportData;
    }

    public static Bundle getReportDataForScanFail(int count, int reason) {
        Bundle reportData = new Bundle();
        reportData.putInt(ReportIdKey.KEY_COUNT, count);
        reportData.putInt("reason", reason);
        return reportData;
    }

    public static Bundle getReportDataForOpenRoaming(String fqdn) {
        Bundle reportData = new Bundle();
        reportData.putString("fqdn", fqdn);
        return reportData;
    }

    public static void startTimerDuringConnection() {
        ReportStore.getInstance().startTimerDuringConnection();
    }

    public static void updateWifiInfo(WifiInfo info) {
        ReportStore.getInstance().updateWifiInfo(info);
    }

    public static void updateDhcpResults(DhcpResults dhcpResults) {
        ReportStore.getInstance().updateDhcpResults(dhcpResults);
    }

    public static void updateWifiStateMachineProcessMessage(String state, int messageId) {
        ReportStore.getInstance().updateWifiStateMachineProcessMessage(state, messageId);
    }

    private static class ReportStore {
        private static ReportStore sInstance = null;
        private long mConnectedEnterTimestamp = 0;
        private DhcpResults mDhcpResults = new DhcpResults();
        private int mLastProceedMessageId = -1;
        private String mLastStateString = "InitialState";
        private int mPrevProceedMessageId = -1;
        private WifiInfo mWifiInfo = new WifiInfo();

        private ReportStore() {
        }

        public static synchronized ReportStore getInstance() {
            ReportStore reportStore;
            synchronized (ReportStore.class) {
                if (sInstance == null) {
                    sInstance = new ReportStore();
                }
                reportStore = sInstance;
            }
            return reportStore;
        }

        public void startTimerDuringConnection() {
            this.mConnectedEnterTimestamp = SystemClock.elapsedRealtime();
        }

        public void updateWifiInfo(WifiInfo info) {
            this.mWifiInfo = new WifiInfo(info);
        }

        public WifiInfo getLastWifiInfo() {
            return this.mWifiInfo;
        }

        public void updateDhcpResults(DhcpResults dhcpResults) {
            this.mDhcpResults = new DhcpResults(dhcpResults);
        }

        public DhcpResults getLastDhcpResults() {
            return this.mDhcpResults;
        }

        public void updateWifiStateMachineProcessMessage(String state, int messageId) {
            this.mLastStateString = state;
            int i = this.mLastProceedMessageId;
            if (i != messageId) {
                this.mPrevProceedMessageId = i;
                this.mLastProceedMessageId = messageId;
            }
        }

        public String getLastWifiStateMachineStateName() {
            return this.mLastStateString;
        }

        public int getLastProceedMessageId() {
            return this.mLastProceedMessageId;
        }

        public int getPrevProceedMessageId() {
            return this.mPrevProceedMessageId;
        }

        public long getConnectedDurationMin() {
            long durationMin = (SystemClock.elapsedRealtime() - this.mConnectedEnterTimestamp) / 60000;
            if (durationMin > 4320) {
                return 4320;
            }
            return durationMin;
        }
    }
}
