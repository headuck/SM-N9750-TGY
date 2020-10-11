package com.android.server.wifi.tcp;

import android.os.Debug;
import android.util.Log;
import com.android.server.wifi.util.XmlUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiTransportLayerFileManager {
    private static final boolean DBG = Debug.semIsProductDev();
    public static final String FILE_TCP_MONITOR_AP_INFO = "/data/misc/wifi/TcpMonitorApInfo.json";
    public static final String FILE_TCP_MONITOR_PACKAGE_INFO = "/data/misc/wifi/TcpMonitorPackageInfo.json";
    public static final String FILE_TCP_SWITCHABLE_UID_INFO = "/data/misc/wifi/TcpMonitorSwitchEnabledUID.xml";
    private static final String TAG = "WifiTransportLayerFileManager";
    private final String TEXT_AP_ACCUMULATED_CONNECTION_COUNT = "AccumulatedConnectionCount";
    private final String TEXT_AP_ACCUMULATED_CONNECTION_TIME = "AccumulatedConnectionTime";
    private final String TEXT_AP_DATA = "Data";
    private final String TEXT_AP_DETECTED_LAST_TIME = "PackageLastDetectedTime";
    private final String TEXT_AP_DETECTED_PACKAGE_COUNT = "PackageDetectedCount";
    private final String TEXT_AP_DETECTED_PACKAGE_LIST = "DetectedPackageList";
    private final String TEXT_AP_DETECTED_PACKAGE_NAME = "PackageName";
    private final String TEXT_AP_DETECTED_PACKAGE_NORMAL_OPERATION_TIME = "PackageNormalOperationTime";
    private final String TEXT_AP_SSID = XmlUtil.WifiConfigurationXmlUtil.XML_TAG_SSID;
    private final String TEXT_AP_SWITCH_FOR_INDIVIDUAL_APPS_DETECTION_COUNT = "SwitchForIndividualAppsDetectionCount";
    private final String TEXT_BROWSING = "Browsing";
    private final String TEXT_CATEGORY = "Category";
    private final String TEXT_CATEGORY_UPDATE_FAIL_COUNT = "CategoryUpdateFailCount";
    private final String TEXT_CHATTING_APP = "ChattingApp";
    private final String TEXT_DATA = "Data";
    private final String TEXT_DATA_USAGE = "DataUsage";
    private final String TEXT_DETECTED_COUNT = "DetectedCount";
    private final String TEXT_GAME = "Game";
    private final String TEXT_INTERNET_PERMISSION = "InternetPermission";
    private final String TEXT_LAUNCHABLE = "Launchable";
    private final String TEXT_PACKAGE_NAME = "PackageName";
    private final String TEXT_SWITCHABLE = "Switchable";
    private final String TEXT_SYSTEM_APP = "SystemApp";
    private final String TEXT_UID = "UID";
    private final String TEXT_USAGE_PATTERN = "UsagePattern";
    private final String TEXT_VOIP = "VoIP";

    public HashMap<Integer, WifiPackageInfo> loadWifiPackageInfoFromFile() {
        Log.d(TAG, "loadWifiPackageInfoFromFile");
        return readWifiPackageInfoList();
    }

    public boolean saveWifiPackageInfoToFile(HashMap<Integer, WifiPackageInfo> info) {
        Log.d(TAG, "saveWifiPackageInfoToFile");
        return writeWifiPackageInfoList(info);
    }

    private HashMap<Integer, WifiPackageInfo> readWifiPackageInfoList() {
        Log.d(TAG, "readWifiPackageInfoList");
        HashMap<Integer, WifiPackageInfo> list = new HashMap<>();
        try {
            JSONObject object = readJSONObjectFromFile(FILE_TCP_MONITOR_PACKAGE_INFO);
            if (object != null) {
                JSONArray array = object.getJSONArray("Data");
                for (int index = 0; index < array.length(); index++) {
                    JSONObject item = array.getJSONObject(index);
                    WifiPackageInfo info = new WifiPackageInfo(item.getInt("UID"), item.getString("PackageName"), item.getString("Category"), item.getBoolean("ChattingApp"), item.getBoolean("VoIP"), item.getBoolean("Game"), item.getBoolean("Browsing"), item.getBoolean("SystemApp"), item.getBoolean("Launchable"), item.getBoolean("Switchable"), item.getInt("DetectedCount"), item.getInt("DataUsage"), item.getInt("UsagePattern"), item.getInt("CategoryUpdateFailCount"), item.getBoolean("InternetPermission"));
                    list.put(Integer.valueOf(info.getUid()), info);
                }
            }
        } catch (JSONException e) {
            if (DBG) {
                Log.w(TAG, "readWifiPackageInfoList - JSONException " + e);
            }
            e.printStackTrace();
        }
        return list;
    }

    private boolean writeWifiPackageInfoList(HashMap<Integer, WifiPackageInfo> list) {
        Log.d(TAG, "writeWifiPackageInfoList");
        if (list == null) {
            return false;
        }
        try {
            JSONArray array = new JSONArray();
            JSONObject finalObject = new JSONObject();
            if (list.isEmpty()) {
                return false;
            }
            for (WifiPackageInfo info : list.values()) {
                JSONObject object = new JSONObject();
                object.put("UID", info.getUid());
                object.put("PackageName", info.getPackageName());
                object.put("Category", info.getCategory());
                object.put("ChattingApp", info.isChatApp());
                object.put("VoIP", info.isVoip());
                object.put("Game", info.isGamingApp());
                object.put("Browsing", info.isBrowsingApp());
                object.put("SystemApp", info.isSystemApp());
                object.put("Launchable", info.isLaunchable());
                object.put("Switchable", info.isSwitchable());
                object.put("DetectedCount", info.getDetectedCount());
                object.put("DataUsage", info.getDataUsage());
                object.put("UsagePattern", info.getUsagePattern());
                object.put("CategoryUpdateFailCount", info.getCategoryUpdateFailCount());
                object.put("InternetPermission", info.hasInternetPermission());
                array.put(object);
            }
            finalObject.put("Data", array);
            return writeJSONObjectToFile(finalObject, FILE_TCP_MONITOR_PACKAGE_INFO);
        } catch (JSONException e) {
            if (DBG) {
                Log.w(TAG, "writeWifiPackageInfoList - JSONException " + e);
            }
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<Integer> loadSwitchEnabledUidListFromFile() {
        Log.d(TAG, "loadSwitchEnabledUidListFromFile");
        return readSwitchEnabledUidInfoList();
    }

    public boolean saveSwitchEnabledUidListToFile(ArrayList<Integer> info) {
        Log.d(TAG, "saveSwitchEnabledUidListToFile");
        return writeSwitchEnabledUidInfoList(info);
    }

    private ArrayList<Integer> readSwitchEnabledUidInfoList() {
        Log.d(TAG, "readSwitchEnabledUidInfoList");
        ArrayList<Integer> list = new ArrayList<>();
        try {
            BufferedReader bufReader = new BufferedReader(new FileReader(new File(FILE_TCP_SWITCHABLE_UID_INFO)));
            while (true) {
                String readLine = bufReader.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                } else if (line != null) {
                    list.add(Integer.valueOf(Integer.parseInt(line)));
                }
            }
        } catch (FileNotFoundException e) {
            if (DBG) {
                Log.w(TAG, "readSwitchEnabledUidInfoList - FileNotFoundException " + e);
            }
            e.printStackTrace();
        } catch (IOException e2) {
            if (DBG) {
                Log.w(TAG, "readSwitchEnabledUidInfoList - IOException " + e2);
            }
            e2.printStackTrace();
        } catch (Exception e3) {
            if (DBG) {
                Log.w(TAG, "readSwitchEnabledUidInfoList - Exception " + e3);
            }
            e3.printStackTrace();
        }
        return list;
    }

    private boolean writeSwitchEnabledUidInfoList(ArrayList<Integer> list) {
        StringBuilder sb;
        String data = "";
        if (list != null && !list.isEmpty()) {
            Iterator<Integer> it = list.iterator();
            while (it.hasNext()) {
                data = data + it.next().intValue() + "\n";
            }
        }
        Log.d(TAG, "writeSwitchEnabledUidInfoList - " + data);
        File fileUidBlocked = new File(FILE_TCP_SWITCHABLE_UID_INFO);
        FileWriter out = null;
        if (fileUidBlocked.exists()) {
            fileUidBlocked.delete();
        }
        try {
            fileUidBlocked.createNewFile();
            FileWriter out2 = new FileWriter(FILE_TCP_SWITCHABLE_UID_INFO);
            if (data != null) {
                Log.d(TAG, "setUidBlockedList: " + data);
                out2.write(data);
                out2.flush();
            }
            try {
                out2.close();
            } catch (IOException e) {
                e = e;
                if (DBG) {
                    sb = new StringBuilder();
                    sb.append("writeSwitchEnabledUidInfoList - IOException ");
                    sb.append(e);
                    Log.w(TAG, sb.toString());
                }
                e.printStackTrace();
                return false;
            }
        } catch (IOException e2) {
            Log.w(TAG, "setUidBlockedList: IOException:" + e2);
            e2.printStackTrace();
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                    e = e3;
                    if (DBG) {
                        sb = new StringBuilder();
                        sb.append("writeSwitchEnabledUidInfoList - IOException ");
                        sb.append(e);
                        Log.w(TAG, sb.toString());
                    }
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e4) {
                    if (DBG) {
                        Log.w(TAG, "writeSwitchEnabledUidInfoList - IOException " + e4);
                    }
                    e4.printStackTrace();
                }
            }
            throw th;
        }
        return false;
    }

    public HashMap<String, WifiApInfo> loadWifiApInfoFromFile() {
        Log.d(TAG, "loadWifiApInfoFromFile");
        HashMap<String, WifiApInfo> list = readWifiApInfoList();
        if (DBG) {
            Log.d(TAG, "loadWifiPackageInfoFromFile - " + list.size());
        }
        return list;
    }

    public boolean saveWifiApInfoToFile(HashMap<String, WifiApInfo> info) {
        Log.d(TAG, "saveWifiApInfoToFile");
        return writeWifiApInfoList(info);
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x00fb  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.HashMap<java.lang.String, com.android.server.wifi.tcp.WifiApInfo> readWifiApInfoList() {
        /*
            r19 = this;
            java.lang.String r1 = "PackageName"
            java.lang.String r2 = "SSID"
            java.lang.String r3 = "readWifiApInfoList - JSONException "
            java.lang.String r4 = "WifiTransportLayerFileManager"
            java.lang.String r0 = "readWifiApInfoList"
            android.util.Log.d(r4, r0)
            java.util.HashMap r0 = new java.util.HashMap
            r0.<init>()
            r5 = r0
            java.lang.String r0 = "/data/misc/wifi/TcpMonitorApInfo.json"
            r6 = r19
            org.json.JSONObject r7 = r6.readJSONObjectFromFile(r0)
            if (r7 == 0) goto L_0x017e
            r8 = 0
            java.lang.String r0 = "Data"
            org.json.JSONArray r0 = r7.getJSONArray(r0)     // Catch:{ JSONException -> 0x0026 }
            r8 = r0
            goto L_0x0040
        L_0x0026:
            r0 = move-exception
            boolean r9 = DBG
            if (r9 == 0) goto L_0x003d
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            r9.append(r3)
            r9.append(r0)
            java.lang.String r9 = r9.toString()
            android.util.Log.w(r4, r9)
        L_0x003d:
            r0.printStackTrace()
        L_0x0040:
            if (r8 == 0) goto L_0x0179
            r0 = 0
            r9 = r0
        L_0x0044:
            int r0 = r8.length()
            if (r9 >= r0) goto L_0x0174
            r10 = 0
            org.json.JSONObject r0 = r8.getJSONObject(r9)     // Catch:{ JSONException -> 0x0051 }
            r10 = r0
            goto L_0x006d
        L_0x0051:
            r0 = move-exception
            r11 = r0
            r0 = r11
            boolean r11 = DBG
            if (r11 == 0) goto L_0x006a
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            r11.append(r3)
            r11.append(r0)
            java.lang.String r11 = r11.toString()
            android.util.Log.w(r4, r11)
        L_0x006a:
            r0.printStackTrace()
        L_0x006d:
            java.util.HashMap r0 = new java.util.HashMap
            r0.<init>()
            r15 = r0
            r11 = 0
            java.lang.String r0 = "DetectedPackageList"
            org.json.JSONArray r0 = r10.getJSONArray(r0)     // Catch:{ JSONException -> 0x007d }
            r11 = r0
            r14 = r11
            goto L_0x0098
        L_0x007d:
            r0 = move-exception
            boolean r12 = DBG
            if (r12 == 0) goto L_0x0094
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            r12.append(r3)
            r12.append(r0)
            java.lang.String r12 = r12.toString()
            android.util.Log.w(r4, r12)
        L_0x0094:
            r0.printStackTrace()
            r14 = r11
        L_0x0098:
            if (r14 == 0) goto L_0x011e
            r0 = 0
            r11 = r0
        L_0x009c:
            int r0 = r14.length()
            if (r11 >= r0) goto L_0x0119
            r12 = 0
            org.json.JSONObject r0 = r14.getJSONObject(r11)     // Catch:{ JSONException -> 0x00a9 }
            r12 = r0
            goto L_0x00c5
        L_0x00a9:
            r0 = move-exception
            r13 = r0
            r0 = r13
            boolean r13 = DBG
            if (r13 == 0) goto L_0x00c2
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            r13.append(r3)
            r13.append(r0)
            java.lang.String r13 = r13.toString()
            android.util.Log.w(r4, r13)
        L_0x00c2:
            r0.printStackTrace()
        L_0x00c5:
            com.android.server.wifi.tcp.WifiApInfo$DetectedPackageInfo r0 = new com.android.server.wifi.tcp.WifiApInfo$DetectedPackageInfo     // Catch:{ JSONException -> 0x00f2 }
            java.lang.String r13 = r12.getString(r1)     // Catch:{ JSONException -> 0x00f2 }
            java.lang.String r6 = "PackageDetectedCount"
            int r6 = r12.getInt(r6)     // Catch:{ JSONException -> 0x00f2 }
            r17 = r7
            java.lang.String r7 = "PackageLastDetectedTime"
            java.lang.String r7 = r12.getString(r7)     // Catch:{ JSONException -> 0x00ee }
            r18 = r8
            java.lang.String r8 = "PackageNormalOperationTime"
            int r8 = r12.getInt(r8)     // Catch:{ JSONException -> 0x00ec }
            r0.<init>(r13, r6, r7, r8)     // Catch:{ JSONException -> 0x00ec }
            java.lang.String r6 = r12.getString(r1)     // Catch:{ JSONException -> 0x00ec }
            r15.put(r6, r0)     // Catch:{ JSONException -> 0x00ec }
            goto L_0x0110
        L_0x00ec:
            r0 = move-exception
            goto L_0x00f7
        L_0x00ee:
            r0 = move-exception
            r18 = r8
            goto L_0x00f7
        L_0x00f2:
            r0 = move-exception
            r17 = r7
            r18 = r8
        L_0x00f7:
            boolean r6 = DBG
            if (r6 == 0) goto L_0x010d
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            r6.append(r3)
            r6.append(r0)
            java.lang.String r6 = r6.toString()
            android.util.Log.w(r4, r6)
        L_0x010d:
            r0.printStackTrace()
        L_0x0110:
            int r11 = r11 + 1
            r6 = r19
            r7 = r17
            r8 = r18
            goto L_0x009c
        L_0x0119:
            r17 = r7
            r18 = r8
            goto L_0x0122
        L_0x011e:
            r17 = r7
            r18 = r8
        L_0x0122:
            com.android.server.wifi.tcp.WifiApInfo r0 = new com.android.server.wifi.tcp.WifiApInfo     // Catch:{ JSONException -> 0x014e }
            java.lang.String r12 = r10.getString(r2)     // Catch:{ JSONException -> 0x014e }
            java.lang.String r6 = "AccumulatedConnectionCount"
            int r13 = r10.getInt(r6)     // Catch:{ JSONException -> 0x014e }
            java.lang.String r6 = "AccumulatedConnectionTime"
            int r6 = r10.getInt(r6)     // Catch:{ JSONException -> 0x014e }
            java.lang.String r7 = "SwitchForIndividualAppsDetectionCount"
            int r7 = r10.getInt(r7)     // Catch:{ JSONException -> 0x014e }
            r11 = r0
            r8 = r14
            r14 = r6
            r6 = r15
            r15 = r7
            r16 = r6
            r11.<init>(r12, r13, r14, r15, r16)     // Catch:{ JSONException -> 0x014c }
            java.lang.String r7 = r10.getString(r2)     // Catch:{ JSONException -> 0x014c }
            r5.put(r7, r0)     // Catch:{ JSONException -> 0x014c }
            goto L_0x016a
        L_0x014c:
            r0 = move-exception
            goto L_0x0151
        L_0x014e:
            r0 = move-exception
            r8 = r14
            r6 = r15
        L_0x0151:
            boolean r7 = DBG
            if (r7 == 0) goto L_0x0167
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r7.append(r3)
            r7.append(r0)
            java.lang.String r7 = r7.toString()
            android.util.Log.w(r4, r7)
        L_0x0167:
            r0.printStackTrace()
        L_0x016a:
            int r9 = r9 + 1
            r6 = r19
            r7 = r17
            r8 = r18
            goto L_0x0044
        L_0x0174:
            r17 = r7
            r18 = r8
            goto L_0x0180
        L_0x0179:
            r17 = r7
            r18 = r8
            goto L_0x0180
        L_0x017e:
            r17 = r7
        L_0x0180:
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerFileManager.readWifiApInfoList():java.util.HashMap");
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x00cc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean writeWifiApInfoList(java.util.HashMap<java.lang.String, com.android.server.wifi.tcp.WifiApInfo> r16) {
        /*
            r15 = this;
            java.lang.String r1 = "WifiTransportLayerFileManager"
            java.lang.String r0 = "writeWifiApInfoList"
            android.util.Log.d(r1, r0)
            r2 = 0
            if (r16 == 0) goto L_0x00e4
            boolean r0 = r16.isEmpty()     // Catch:{ JSONException -> 0x00c6 }
            if (r0 != 0) goto L_0x00e4
            org.json.JSONArray r0 = new org.json.JSONArray     // Catch:{ JSONException -> 0x00c6 }
            r0.<init>()     // Catch:{ JSONException -> 0x00c6 }
            org.json.JSONObject r3 = new org.json.JSONObject     // Catch:{ JSONException -> 0x00c6 }
            r3.<init>()     // Catch:{ JSONException -> 0x00c6 }
            java.util.Collection r4 = r16.values()     // Catch:{ JSONException -> 0x00c6 }
            java.util.Iterator r5 = r4.iterator()     // Catch:{ JSONException -> 0x00c6 }
        L_0x0022:
            boolean r6 = r5.hasNext()     // Catch:{ JSONException -> 0x00c6 }
            if (r6 == 0) goto L_0x00b5
            java.lang.Object r6 = r5.next()     // Catch:{ JSONException -> 0x00c6 }
            com.android.server.wifi.tcp.WifiApInfo r6 = (com.android.server.wifi.tcp.WifiApInfo) r6     // Catch:{ JSONException -> 0x00c6 }
            org.json.JSONObject r7 = new org.json.JSONObject     // Catch:{ JSONException -> 0x00c6 }
            r7.<init>()     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r8 = "SSID"
            java.lang.String r9 = r6.getSsid()     // Catch:{ JSONException -> 0x00c6 }
            r7.put(r8, r9)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r8 = "AccumulatedConnectionCount"
            int r9 = r6.getAccumulatedConnectionCount()     // Catch:{ JSONException -> 0x00c6 }
            r7.put(r8, r9)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r8 = "AccumulatedConnectionTime"
            int r9 = r6.getAccumulatedConnectionTime()     // Catch:{ JSONException -> 0x00c6 }
            r7.put(r8, r9)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r8 = "SwitchForIndividualAppsDetectionCount"
            int r9 = r6.getSwitchForIndivdiaulAppsDetectionCount()     // Catch:{ JSONException -> 0x00c6 }
            r7.put(r8, r9)     // Catch:{ JSONException -> 0x00c6 }
            java.util.HashMap r8 = r6.getDetectedPackageList()     // Catch:{ JSONException -> 0x00c6 }
            if (r8 == 0) goto L_0x00af
            boolean r9 = r8.isEmpty()     // Catch:{ JSONException -> 0x00c6 }
            if (r9 != 0) goto L_0x00af
            org.json.JSONArray r9 = new org.json.JSONArray     // Catch:{ JSONException -> 0x00c6 }
            r9.<init>()     // Catch:{ JSONException -> 0x00c6 }
            java.util.Collection r10 = r8.values()     // Catch:{ JSONException -> 0x00c6 }
            java.util.Iterator r10 = r10.iterator()     // Catch:{ JSONException -> 0x00c6 }
        L_0x0070:
            boolean r11 = r10.hasNext()     // Catch:{ JSONException -> 0x00c6 }
            if (r11 == 0) goto L_0x00aa
            java.lang.Object r11 = r10.next()     // Catch:{ JSONException -> 0x00c6 }
            com.android.server.wifi.tcp.WifiApInfo$DetectedPackageInfo r11 = (com.android.server.wifi.tcp.WifiApInfo.DetectedPackageInfo) r11     // Catch:{ JSONException -> 0x00c6 }
            org.json.JSONObject r12 = new org.json.JSONObject     // Catch:{ JSONException -> 0x00c6 }
            r12.<init>()     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r13 = "PackageName"
            java.lang.String r14 = r11.getPackageName()     // Catch:{ JSONException -> 0x00c6 }
            r12.put(r13, r14)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r13 = "PackageDetectedCount"
            int r14 = r11.getDetectedCount()     // Catch:{ JSONException -> 0x00c6 }
            r12.put(r13, r14)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r13 = "PackageLastDetectedTime"
            java.lang.String r14 = r11.getLastDetectedTime()     // Catch:{ JSONException -> 0x00c6 }
            r12.put(r13, r14)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r13 = "PackageNormalOperationTime"
            int r14 = r11.getPackageNormalOperationTime()     // Catch:{ JSONException -> 0x00c6 }
            r12.put(r13, r14)     // Catch:{ JSONException -> 0x00c6 }
            r9.put(r12)     // Catch:{ JSONException -> 0x00c6 }
            goto L_0x0070
        L_0x00aa:
            java.lang.String r10 = "DetectedPackageList"
            r7.put(r10, r9)     // Catch:{ JSONException -> 0x00c6 }
        L_0x00af:
            r0.put(r7)     // Catch:{ JSONException -> 0x00c6 }
            goto L_0x0022
        L_0x00b5:
            java.lang.String r5 = "Data"
            r3.put(r5, r0)     // Catch:{ JSONException -> 0x00c6 }
            java.lang.String r5 = "/data/misc/wifi/TcpMonitorApInfo.json"
            r6 = r15
            boolean r1 = r15.writeJSONObjectToFile(r3, r5)     // Catch:{ JSONException -> 0x00c4 }
            r2 = r1
            goto L_0x00e5
        L_0x00c4:
            r0 = move-exception
            goto L_0x00c8
        L_0x00c6:
            r0 = move-exception
            r6 = r15
        L_0x00c8:
            boolean r3 = DBG
            if (r3 == 0) goto L_0x00e0
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "writeWifiApInfoList - JSONException "
            r3.append(r4)
            r3.append(r0)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r1, r3)
        L_0x00e0:
            r0.printStackTrace()
            goto L_0x00e6
        L_0x00e4:
            r6 = r15
        L_0x00e5:
        L_0x00e6:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.tcp.WifiTransportLayerFileManager.writeWifiApInfoList(java.util.HashMap):boolean");
    }

    private JSONObject readJSONObjectFromFile(String filePath) {
        Log.d(TAG, "readJSONObjectFromFile");
        try {
            return new JSONObject(new BufferedReader(new FileReader(new File(filePath))).readLine());
        } catch (FileNotFoundException e) {
            if (DBG) {
                Log.w(TAG, "readJSONObjectFromFile - " + e);
            }
            e.printStackTrace();
            return null;
        } catch (IOException e2) {
            if (DBG) {
                Log.w(TAG, "readJSONObjectFromFile - " + e2);
            }
            e2.printStackTrace();
            return null;
        } catch (JSONException e3) {
            if (DBG) {
                Log.w(TAG, "readJSONObjectFromFile - " + e3);
            }
            e3.printStackTrace();
            return null;
        } catch (Exception e4) {
            if (DBG) {
                Log.w(TAG, "readJSONObjectFromFile - " + e4);
            }
            e4.printStackTrace();
            return null;
        }
    }

    private boolean writeJSONObjectToFile(JSONObject obj, String filePath) {
        if (DBG) {
            Log.d(TAG, "writeJSONObjectToFile");
        }
        if (obj == null) {
            return false;
        }
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            FileWriter out = new FileWriter(filePath);
            out.write(obj.toString());
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            if (DBG) {
                Log.w(TAG, "writeJSONObjectToFile - " + e);
            }
            e.printStackTrace();
            return false;
        } catch (Exception e2) {
            if (DBG) {
                Log.w(TAG, "writeJSONObjectToFile - " + e2);
            }
            e2.printStackTrace();
            return false;
        }
    }
}
