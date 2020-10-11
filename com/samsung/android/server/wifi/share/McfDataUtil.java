package com.samsung.android.server.wifi.share;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.SystemClock;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

class McfDataUtil {
    private static final long MAX_SCAN_LIFE_TIME = 1200000;
    private static final String TAG = "WifiProfileShare.Util";
    private final HashMap<String, HashSet<String>> mConfigKeys = new HashMap<>();
    private String mConnectedConfigKey;
    private OnBssidChangeListener mListener;
    private final HashMap<String, ScanResult> mScanedAp = new HashMap<>();

    private enum McfDataType {
        QOS,
        PASSWORD
    }

    interface OnBssidChangeListener {
        void onBssidListChanged();
    }

    McfDataUtil() {
    }

    /* Debug info: failed to restart local var, previous not found, register: 11 */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x0130, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateScanResults(java.util.List<android.net.wifi.ScanResult> r12) {
        /*
            r11 = this;
            monitor-enter(r11)
            if (r12 == 0) goto L_0x012f
            int r0 = r12.size()     // Catch:{ all -> 0x012c }
            if (r0 != 0) goto L_0x000b
            goto L_0x012f
        L_0x000b:
            r0 = 0
            java.lang.Object r1 = r12.get(r0)     // Catch:{ all -> 0x012c }
            android.net.wifi.ScanResult r1 = (android.net.wifi.ScanResult) r1     // Catch:{ all -> 0x012c }
            java.lang.String r1 = r1.capabilities     // Catch:{ all -> 0x012c }
            java.lang.String r2 = "[PARTIAL]"
            boolean r1 = r1.contains(r2)     // Catch:{ all -> 0x012c }
            if (r1 == 0) goto L_0x001e
            monitor-exit(r11)
            return
        L_0x001e:
            r11.removeOldScans()     // Catch:{ all -> 0x012c }
            java.util.Iterator r1 = r12.iterator()     // Catch:{ all -> 0x012c }
        L_0x0025:
            boolean r2 = r1.hasNext()     // Catch:{ all -> 0x012c }
            if (r2 == 0) goto L_0x00f4
            java.lang.Object r2 = r1.next()     // Catch:{ all -> 0x012c }
            android.net.wifi.ScanResult r2 = (android.net.wifi.ScanResult) r2     // Catch:{ all -> 0x012c }
            java.lang.String r3 = r2.SSID     // Catch:{ all -> 0x012c }
            if (r3 == 0) goto L_0x0025
            java.lang.String r3 = r2.SSID     // Catch:{ all -> 0x012c }
            int r3 = r3.length()     // Catch:{ all -> 0x012c }
            if (r3 != 0) goto L_0x003e
            goto L_0x0025
        L_0x003e:
            java.lang.String r3 = r2.BSSID     // Catch:{ all -> 0x012c }
            if (r3 == 0) goto L_0x0025
            java.lang.String r3 = r2.BSSID     // Catch:{ all -> 0x012c }
            int r3 = r3.length()     // Catch:{ all -> 0x012c }
            if (r3 != 0) goto L_0x004b
            goto L_0x0025
        L_0x004b:
            java.lang.String r3 = r2.capabilities     // Catch:{ all -> 0x012c }
            java.lang.String r4 = "[IBSS]"
            boolean r3 = r3.contains(r4)     // Catch:{ all -> 0x012c }
            if (r3 == 0) goto L_0x0056
            goto L_0x0025
        L_0x0056:
            java.util.HashMap<java.lang.String, android.net.wifi.ScanResult> r3 = r11.mScanedAp     // Catch:{ all -> 0x012c }
            java.lang.String r4 = r2.BSSID     // Catch:{ all -> 0x012c }
            java.lang.String r4 = r4.toLowerCase()     // Catch:{ all -> 0x012c }
            r3.put(r4, r2)     // Catch:{ all -> 0x012c }
            java.lang.String[] r3 = r11.getSecurityString(r2, r0)     // Catch:{ all -> 0x012c }
            if (r3 == 0) goto L_0x00f2
            java.util.ArrayList r4 = new java.util.ArrayList     // Catch:{ all -> 0x012c }
            r4.<init>()     // Catch:{ all -> 0x012c }
            int r5 = r3.length     // Catch:{ all -> 0x012c }
            r6 = r0
        L_0x006e:
            if (r6 >= r5) goto L_0x007f
            r7 = r3[r6]     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r2.SSID     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r11.getConfigKey(r8, r7)     // Catch:{ all -> 0x012c }
            r4.add(r8)     // Catch:{ all -> 0x012c }
            int r6 = r6 + 1
            goto L_0x006e
        L_0x007f:
            java.util.Iterator r5 = r4.iterator()     // Catch:{ all -> 0x012c }
        L_0x0083:
            boolean r6 = r5.hasNext()     // Catch:{ all -> 0x012c }
            if (r6 == 0) goto L_0x00f2
            java.lang.Object r6 = r5.next()     // Catch:{ all -> 0x012c }
            java.lang.String r6 = (java.lang.String) r6     // Catch:{ all -> 0x012c }
            java.util.HashMap<java.lang.String, java.util.HashSet<java.lang.String>> r7 = r11.mConfigKeys     // Catch:{ all -> 0x012c }
            boolean r7 = r7.containsKey(r6)     // Catch:{ all -> 0x012c }
            if (r7 == 0) goto L_0x00de
            java.util.HashMap<java.lang.String, java.util.HashSet<java.lang.String>> r7 = r11.mConfigKeys     // Catch:{ all -> 0x012c }
            java.lang.Object r7 = r7.get(r6)     // Catch:{ all -> 0x012c }
            java.util.HashSet r7 = (java.util.HashSet) r7     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r2.BSSID     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r8.toLowerCase()     // Catch:{ all -> 0x012c }
            boolean r8 = r7.contains(r8)     // Catch:{ all -> 0x012c }
            if (r8 != 0) goto L_0x00dd
            java.lang.String r8 = r2.BSSID     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r8.toLowerCase()     // Catch:{ all -> 0x012c }
            r7.add(r8)     // Catch:{ all -> 0x012c }
            com.samsung.android.server.wifi.share.McfDataUtil$OnBssidChangeListener r8 = r11.mListener     // Catch:{ all -> 0x012c }
            if (r8 == 0) goto L_0x00dd
            java.lang.String r8 = r11.mConnectedConfigKey     // Catch:{ all -> 0x012c }
            boolean r8 = r6.equals(r8)     // Catch:{ all -> 0x012c }
            if (r8 == 0) goto L_0x00dd
            java.lang.String r8 = "WifiProfileShare.Util"
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ all -> 0x012c }
            r9.<init>()     // Catch:{ all -> 0x012c }
            java.lang.String r10 = "notify bssid list changed, new bssid was added "
            r9.append(r10)     // Catch:{ all -> 0x012c }
            java.lang.String r10 = r2.BSSID     // Catch:{ all -> 0x012c }
            r9.append(r10)     // Catch:{ all -> 0x012c }
            java.lang.String r9 = r9.toString()     // Catch:{ all -> 0x012c }
            android.util.Log.i(r8, r9)     // Catch:{ all -> 0x012c }
            com.samsung.android.server.wifi.share.McfDataUtil$OnBssidChangeListener r8 = r11.mListener     // Catch:{ all -> 0x012c }
            r8.onBssidListChanged()     // Catch:{ all -> 0x012c }
        L_0x00dd:
            goto L_0x00f1
        L_0x00de:
            java.util.HashSet r7 = new java.util.HashSet     // Catch:{ all -> 0x012c }
            r7.<init>()     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r2.BSSID     // Catch:{ all -> 0x012c }
            java.lang.String r8 = r8.toLowerCase()     // Catch:{ all -> 0x012c }
            r7.add(r8)     // Catch:{ all -> 0x012c }
            java.util.HashMap<java.lang.String, java.util.HashSet<java.lang.String>> r8 = r11.mConfigKeys     // Catch:{ all -> 0x012c }
            r8.put(r6, r7)     // Catch:{ all -> 0x012c }
        L_0x00f1:
            goto L_0x0083
        L_0x00f2:
            goto L_0x0025
        L_0x00f4:
            java.lang.String r0 = "WifiProfileShare.Util"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x012c }
            r1.<init>()     // Catch:{ all -> 0x012c }
            java.lang.String r2 = "updateScanResult, scan:"
            r1.append(r2)     // Catch:{ all -> 0x012c }
            int r2 = r12.size()     // Catch:{ all -> 0x012c }
            r1.append(r2)     // Catch:{ all -> 0x012c }
            java.lang.String r2 = " bssid:"
            r1.append(r2)     // Catch:{ all -> 0x012c }
            java.util.HashMap<java.lang.String, android.net.wifi.ScanResult> r2 = r11.mScanedAp     // Catch:{ all -> 0x012c }
            int r2 = r2.size()     // Catch:{ all -> 0x012c }
            r1.append(r2)     // Catch:{ all -> 0x012c }
            java.lang.String r2 = " configKey:"
            r1.append(r2)     // Catch:{ all -> 0x012c }
            java.util.HashMap<java.lang.String, java.util.HashSet<java.lang.String>> r2 = r11.mConfigKeys     // Catch:{ all -> 0x012c }
            int r2 = r2.size()     // Catch:{ all -> 0x012c }
            r1.append(r2)     // Catch:{ all -> 0x012c }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x012c }
            android.util.Log.v(r0, r1)     // Catch:{ all -> 0x012c }
            monitor-exit(r11)
            return
        L_0x012c:
            r12 = move-exception
            monitor-exit(r11)
            throw r12
        L_0x012f:
            monitor-exit(r11)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.share.McfDataUtil.updateScanResults(java.util.List):void");
    }

    private void removeOldScans() {
        long nowMs = SystemClock.elapsedRealtime();
        HashSet<String> connectedBssids = null;
        String str = this.mConnectedConfigKey;
        if (str != null) {
            connectedBssids = this.mConfigKeys.get(str);
        }
        List<String> oldBssids = new ArrayList<>();
        Iterator<ScanResult> iter = this.mScanedAp.values().iterator();
        while (iter.hasNext()) {
            ScanResult scanResult = iter.next();
            if (nowMs - (scanResult.timestamp / 1000) > MAX_SCAN_LIFE_TIME) {
                if (connectedBssids == null || !connectedBssids.contains(scanResult.BSSID)) {
                    oldBssids.add(scanResult.BSSID);
                    iter.remove();
                } else {
                    Log.d(TAG, "current network's scan result was expired, but skip op " + connectedBssids);
                }
            }
        }
        for (String configKey : this.mConfigKeys.keySet()) {
            Iterator<String> iter2 = this.mConfigKeys.get(configKey).iterator();
            while (iter2.hasNext()) {
                if (oldBssids.contains(iter2.next())) {
                    iter2.remove();
                }
            }
        }
        int size = oldBssids.size();
        if (size > 0) {
            Log.d(TAG, "remove old scan results, size:" + size);
        }
    }

    /* access modifiers changed from: package-private */
    public void registerListener(String configKey, OnBssidChangeListener listener) {
        this.mConnectedConfigKey = configKey;
        this.mListener = listener;
    }

    /* access modifiers changed from: package-private */
    public void unregisterListener() {
        this.mConnectedConfigKey = null;
        this.mListener = null;
    }

    /* access modifiers changed from: package-private */
    public McfData getMcfDataForQos(String bssid, int[] networkScore) {
        if (networkScore == null || networkScore.length != 4) {
            return null;
        }
        return new McfData(bssid, networkScore);
    }

    private synchronized String getBestAlternativeBssidExcept(String configKey, String exceptBssid) {
        String bestBssid;
        ScanResult scanItem;
        HashSet<String> bssids = this.mConfigKeys.get(configKey);
        bestBssid = null;
        int maxRssi = -200;
        if (bssids != null && bssids.size() > 1) {
            Iterator<String> it = bssids.iterator();
            while (it.hasNext()) {
                String roamBssid = it.next();
                if (!roamBssid.equals(exceptBssid) && roamBssid.substring(0, roamBssid.length() - 1).equals(exceptBssid.substring(0, exceptBssid.length() - 1)) && (scanItem = this.mScanedAp.get(roamBssid)) != null && scanItem.level > maxRssi) {
                    maxRssi = scanItem.level;
                    bestBssid = roamBssid;
                }
            }
        }
        return bestBssid;
    }

    /* access modifiers changed from: package-private */
    public McfData getMcfDataForRequestingPassword(String bssid, String configKey) {
        if (bssid == null || configKey == null) {
            Log.e(TAG, "getMcfDataForRequestingPassword - request configKey is null");
            return null;
        }
        McfData pwdReq = new McfData(bssid, configKey, "");
        String roamingBssid = getBestAlternativeBssidExcept(configKey, bssid);
        if (roamingBssid != null) {
            pwdReq.setRoamBssid(roamingBssid);
        }
        return pwdReq;
    }

    static McfData getMcfDataForCancelingPassword(String configKey) {
        return new McfData("00:00:00:00:00:00", configKey, "");
    }

    static McfData getMcfDataForQoS(byte[] deliveredQoSData) {
        if (deliveredQoSData == null || deliveredQoSData.length != 4) {
            return null;
        }
        return new McfData(McfDataType.QOS, deliveredQoSData);
    }

    static McfData getMcfDataForPassword(byte[] deliveredQoSData) {
        if (deliveredQoSData == null || deliveredQoSData.length != 4) {
            return null;
        }
        return new McfData(McfDataType.PASSWORD, deliveredQoSData);
    }

    static McfData getMcfData(JSONObject deliveredPasswordData) {
        if (deliveredPasswordData == null) {
            return null;
        }
        try {
            return new McfData(deliveredPasswordData);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public List<McfData> getMcfDataListForSharingPasswordForBssid(String bssid, String configKey, String password) {
        List<McfData> ret = new ArrayList<>();
        ret.add(new McfData(bssid, configKey, password));
        Log.d(TAG, "getMcfDataList size:" + ret.size());
        return ret;
    }

    /* access modifiers changed from: package-private */
    public synchronized List<McfData> getMcfDataListForSharingPassword(String configKey, String password) {
        List<McfData> ret;
        HashSet<String> bssids;
        ret = new ArrayList<>();
        if (!(configKey == null || (bssids = this.mConfigKeys.get(configKey)) == null)) {
            Iterator<String> it = bssids.iterator();
            while (it.hasNext()) {
                ret.add(new McfData(it.next(), configKey, password));
            }
        }
        Log.d(TAG, "getMcfDataList size:" + ret.size());
        return ret;
    }

    /* access modifiers changed from: package-private */
    public synchronized String findBssidFromScanResult(String partOfBssid) {
        if (partOfBssid != null) {
            if (partOfBssid.length() != 0) {
                for (String bssid : this.mScanedAp.keySet()) {
                    if (bssid != null) {
                        if (bssid.length() != 0) {
                            if (Arrays.equals(McfData.generatePBssid(bssid), McfData.generatePBssid(partOfBssid))) {
                                return bssid;
                            }
                        }
                    }
                }
            }
        }
        Log.e(TAG, "can not find network " + partOfBssid);
        return null;
    }

    /* access modifiers changed from: package-private */
    public synchronized List<String> getConfigKeys(String bssid) {
        List<String> ret;
        ret = new ArrayList<>();
        for (String configKey : this.mConfigKeys.keySet()) {
            HashSet<String> bssids = this.mConfigKeys.get(configKey);
            if (bssids != null && bssids.contains(bssid)) {
                ret.add(configKey);
            }
        }
        return ret;
    }

    /* access modifiers changed from: package-private */
    public synchronized String getConfigKeyForPassword(String bssid, boolean isSupportedSae) {
        String[] securityString;
        ScanResult scanItem = this.mScanedAp.get(bssid);
        if (scanItem == null || (securityString = getSecurityString(scanItem, true)) == null || securityString.length == 0) {
            return null;
        }
        if (securityString.length != 2 || isSupportedSae) {
            return getConfigKey(scanItem.SSID, securityString[0]);
        }
        return getConfigKey(scanItem.SSID, securityString[1]);
    }

    private String getConfigKey(String ssid, String securityString) {
        return "\"" + ssid + "\"" + securityString;
    }

    private String[] getSecurityString(ScanResult result, boolean onlyForSharing) {
        if (result.capabilities.contains("WEP")) {
            return new String[]{"WEP"};
        }
        if (result.capabilities.contains("WAPI-PSK")) {
            return new String[]{WifiConfiguration.KeyMgmt.strings[22]};
        } else if (result.capabilities.contains("WAPI-CERT")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[23]};
        } else if (result.capabilities.contains("PSK+SAE") || result.capabilities.contains("PSK+PSK-SHA256+SAE")) {
            return new String[]{WifiConfiguration.KeyMgmt.strings[8], WifiConfiguration.KeyMgmt.strings[1]};
        } else if (result.capabilities.contains("SAE")) {
            return new String[]{WifiConfiguration.KeyMgmt.strings[8]};
        } else if (result.capabilities.contains("PSK")) {
            return new String[]{WifiConfiguration.KeyMgmt.strings[1]};
        } else if (result.capabilities.contains("EAP_SUITE_B_192")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[10]};
        } else if (result.capabilities.contains("EAP")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[2]};
        } else if (result.capabilities.contains("OWE_TRANSITION")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[9], WifiConfiguration.KeyMgmt.strings[0]};
        } else if (result.capabilities.contains("OWE")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[9]};
        } else if (result.capabilities.contains("CCKM")) {
            if (onlyForSharing) {
                return null;
            }
            return new String[]{WifiConfiguration.KeyMgmt.strings[24]};
        } else if (onlyForSharing) {
            return null;
        } else {
            return new String[]{WifiConfiguration.KeyMgmt.strings[0]};
        }
    }

    static class McfData {
        public static final String JSON_CONFIGKEY = "configKey";
        public static final String JSON_CONFIGKEY_HOTSPOT = "configKey_hotspot";
        public static final String JSON_PASSWORD = "password";
        public static final String JSON_PBSSID = "pBssid";
        public static final String JSON_START_AT = "startAt";
        public static final String JSON_STATE = "state";
        private static final int PBSSID_LENGTH = 3;
        private static final int QOS_BYTE_LENGTH = 4;
        private final byte QOS_DATA_NO_INTERNET;
        private final byte QOS_DATA_UNSECURED;
        private final String configKey;
        private final byte[] pBssid;
        private byte pBssidRoam;
        private final String password;
        private byte qosData;
        private final McfDataType type;

        private McfData(String bssid, int[] qosData2) {
            this.qosData = 0;
            this.pBssidRoam = 0;
            this.QOS_DATA_NO_INTERNET = -6;
            this.QOS_DATA_UNSECURED = -5;
            this.type = McfDataType.QOS;
            this.pBssid = generatePBssid(bssid);
            if (qosData2 != null && qosData2.length == 4) {
                this.qosData = getNetworkScoreData(qosData2);
            }
            this.configKey = null;
            this.password = null;
        }

        private McfData(String bssid, String configKey2, String password2) {
            this.qosData = 0;
            this.pBssidRoam = 0;
            this.QOS_DATA_NO_INTERNET = -6;
            this.QOS_DATA_UNSECURED = -5;
            this.type = McfDataType.PASSWORD;
            this.pBssid = generatePBssid(bssid);
            this.configKey = configKey2;
            this.password = password2;
        }

        private McfData(McfDataType type2, byte[] pBssidWithQosData) {
            this.qosData = 0;
            this.pBssidRoam = 0;
            this.QOS_DATA_NO_INTERNET = -6;
            this.QOS_DATA_UNSECURED = -5;
            this.type = type2;
            this.pBssid = new byte[3];
            byte[] bArr = this.pBssid;
            bArr[0] = pBssidWithQosData[0];
            bArr[1] = pBssidWithQosData[1];
            bArr[2] = pBssidWithQosData[2];
            if (type2 == McfDataType.QOS) {
                this.qosData = pBssidWithQosData[3];
            } else {
                this.pBssidRoam = pBssidWithQosData[3];
            }
            this.configKey = null;
            this.password = null;
        }

        private McfData(JSONObject jsonObject) throws IllegalArgumentException {
            this.qosData = 0;
            this.pBssidRoam = 0;
            this.QOS_DATA_NO_INTERNET = -6;
            this.QOS_DATA_UNSECURED = -5;
            String[] jsonData = parsePasswordData(jsonObject);
            if (jsonData == null || jsonData.length < 3) {
                throw new IllegalArgumentException("wrong json contents");
            } else if (jsonData[0] == null || jsonData[0].length() == 0) {
                throw new IllegalArgumentException("wrong pBssid value " + jsonData[0]);
            } else {
                this.type = McfDataType.PASSWORD;
                this.pBssid = generatePBssid(jsonData[0]);
                this.qosData = 0;
                this.configKey = jsonData[1];
                this.password = jsonData[2];
            }
        }

        /* access modifiers changed from: package-private */
        public byte[] getByteArrayForSharing() {
            byte[] ret = new byte[4];
            byte[] bArr = this.pBssid;
            ret[0] = bArr[0];
            ret[1] = bArr[1];
            ret[2] = bArr[2];
            if (this.type == McfDataType.QOS) {
                ret[3] = this.qosData;
            } else {
                ret[3] = this.pBssidRoam;
            }
            return ret;
        }

        /* access modifiers changed from: package-private */
        public String[] parsePasswordData(JSONObject jsonObject) {
            String[] ret = new String[4];
            try {
                if (jsonObject.has(JSON_PBSSID)) {
                    ret[0] = jsonObject.getString(JSON_PBSSID);
                }
                if (jsonObject.has("configKey")) {
                    ret[1] = jsonObject.getString("configKey");
                }
                if (jsonObject.has(JSON_PASSWORD)) {
                    ret[2] = jsonObject.getString(JSON_PASSWORD);
                }
                if (jsonObject.has("state")) {
                    ret[3] = jsonObject.getString("state");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return ret;
        }

        /* access modifiers changed from: package-private */
        public JSONObject getPasswordJsonData(String state, boolean withPassword) {
            return getPasswordJsonData(state, withPassword, 0);
        }

        /* access modifiers changed from: package-private */
        public JSONObject getPasswordJsonData(String state, boolean withPassword, long startAt) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("state", state);
                jsonObject.put(JSON_PBSSID, getPartOfBssid());
                jsonObject.put("configKey", this.configKey);
                if (withPassword) {
                    jsonObject.put(JSON_PASSWORD, this.password);
                }
                if (startAt != 0) {
                    jsonObject.put(JSON_START_AT, startAt);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        /* access modifiers changed from: package-private */
        public int[] getQoSData() {
            return getNetworkScoreData(this.qosData);
        }

        /* access modifiers changed from: package-private */
        public String getPartOfBssid() {
            return byteArrayToMacString(this.pBssid);
        }

        /* access modifiers changed from: package-private */
        public String getConfigKey() {
            return this.configKey;
        }

        /* access modifiers changed from: package-private */
        public String getPassword() {
            return this.password;
        }

        /* access modifiers changed from: package-private */
        public boolean isPasswordCancelData() {
            if (this.type != McfDataType.PASSWORD) {
                return false;
            }
            int i = 0;
            while (true) {
                byte[] bArr = this.pBssid;
                if (i < bArr.length) {
                    if (bArr[i] != 0) {
                        return false;
                    }
                    i++;
                } else if (this.pBssidRoam == 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        /* access modifiers changed from: private */
        public static byte[] generatePBssid(String bssid) {
            byte[] bssidBytes = hexStringToByteArray(bssid.replace(":", ""));
            if (bssidBytes.length == 3) {
                return Arrays.copyOf(bssidBytes, 3);
            }
            if (bssidBytes.length != 6) {
                return null;
            }
            return new byte[]{(byte) (bssidBytes[3] ^ bssidBytes[1]), (byte) (bssidBytes[2] ^ bssidBytes[4]), (byte) (bssidBytes[0] ^ bssidBytes[5])};
        }

        private byte[] getRoamBssid() {
            if (this.pBssidRoam == 0) {
                return null;
            }
            byte[] otherBssidRoam = (byte[]) this.pBssid.clone();
            otherBssidRoam[otherBssidRoam.length - 1] = this.pBssidRoam;
            return otherBssidRoam;
        }

        /* access modifiers changed from: private */
        public void setRoamBssid(String bssid) {
            byte[] bssidBytes = hexStringToByteArray(bssid.replace(":", "").substring(6));
            this.pBssidRoam = (byte) (bssidBytes[0] ^ bssidBytes[bssidBytes.length - 1]);
        }

        private byte getNetworkScoreData(int[] data) {
            if (data == null || data.length != 4) {
                return 0;
            }
            if (data[0] == 2) {
                return -6;
            }
            if (data[0] >= 3) {
                return -5;
            }
            return (byte) ((((byte) data[0]) * 125) + (((byte) data[1]) * 25) + (((byte) data[2]) * 5) + ((byte) data[3]));
        }

        private int[] getNetworkScoreData(byte data) {
            int[] ret = {0, 0, 0, 0};
            if (data == -6) {
                ret[0] = 2;
            } else if (data == -5) {
                ret[0] = 3;
            } else {
                int calcData = Byte.toUnsignedInt(data);
                ret[0] = calcData / 125;
                int calcData2 = calcData - (ret[0] * 125);
                ret[1] = calcData2 / 25;
                int calcData3 = calcData2 - (ret[1] * 25);
                ret[2] = calcData3 / 5;
                ret[3] = calcData3 - (ret[2] * 5);
            }
            return ret;
        }

        private static byte[] hexStringToByteArray(String s) {
            int len = s.length();
            byte[] data = new byte[(len / 2)];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        }

        static String byteArrayToString(byte[] ba) {
            StringBuilder hex = new StringBuilder();
            int length = ba.length;
            for (int i = 0; i < length; i++) {
                hex.append(String.format("%02x", new Object[]{Byte.valueOf(ba[i])}));
            }
            return hex.toString();
        }

        private String byteArrayToMacString(byte[] ba) {
            StringBuilder hex = new StringBuilder();
            boolean isFirst = true;
            for (byte b : ba) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    hex.append(":");
                }
                hex.append(String.format("%02x", new Object[]{Byte.valueOf(b)}));
            }
            return hex.toString();
        }

        private boolean isSameBssid(byte[] otherPBssid) {
            int i = 0;
            while (true) {
                byte[] bArr = this.pBssid;
                if (i >= bArr.length || i >= otherPBssid.length) {
                    return true;
                }
                if (bArr[i] != otherPBssid[i]) {
                    return false;
                }
                i++;
            }
        }

        /* access modifiers changed from: package-private */
        public boolean maybeRoaming(McfData other) {
            byte[] otherBssidRoam = other.getRoamBssid();
            if (otherBssidRoam != null) {
                return isSameBssid(otherBssidRoam);
            }
            return false;
        }

        /* access modifiers changed from: package-private */
        public boolean matches(McfData other) {
            if (this.type != other.type) {
                return false;
            }
            return isSameBssid(other.pBssid);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof McfData)) {
                return false;
            }
            McfData other = (McfData) obj;
            if (this.type != other.type || !isSameBssid(other.pBssid)) {
                return false;
            }
            if (this.type != McfDataType.QOS) {
                String str = this.configKey;
                if (str != null) {
                    return str.equals(other.configKey);
                }
                if (other.configKey == null) {
                    return true;
                }
                return false;
            } else if (this.qosData == other.qosData) {
                return true;
            } else {
                return false;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("type:");
            sb.append(this.type.name());
            sb.append(", pBssid:");
            sb.append(String.format("%02x:%02x:%02x", new Object[]{Byte.valueOf(this.pBssid[0]), Byte.valueOf(this.pBssid[1]), Byte.valueOf(this.pBssid[2])}));
            if (this.type == McfDataType.QOS) {
                sb.append(", qosData:");
                sb.append(String.format("%02x", new Object[]{Byte.valueOf(this.qosData)}));
            } else {
                sb.append(", pBssidRoam:");
                sb.append(String.format("%02x", new Object[]{Byte.valueOf(this.pBssidRoam)}));
                if (this.configKey != null) {
                    sb.append(", configKey:");
                    sb.append(this.configKey);
                }
            }
            return sb.toString();
        }
    }
}
