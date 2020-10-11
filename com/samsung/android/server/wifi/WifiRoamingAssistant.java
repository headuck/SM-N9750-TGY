package com.samsung.android.server.wifi;

import android.content.Context;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WifiRoamingAssistant {
    private static final boolean DEV = Debug.semIsProductDev();
    private static final String JTAG_RCL_LIST = "rcl_list";
    private static final int MAX_RCL_COUNT = 16;
    private static final int MAX_RETURN_CHANNEL_COUNT = 5;
    private static final String RCL_FILE_DISABLE = "Disable.rcl";
    private static final String RCL_FILE_NAME = "RCL.json";
    private static final String TAG = WifiRoamingAssistant.class.getSimpleName();
    private static final String VERSION = "1.0";
    private static WifiRoamingAssistant mInstance;
    private int mCachedFrequency;
    private String mCachedNetworkKey;
    private final Clock mClock = WifiInjector.getInstance().getClock();
    private ArrayList<String> mExceptionalNetworks = new ArrayList<>();
    private String mLastConnectedNetworkKey;
    private long mLastUpdatedTime;
    private int mRclEnabled = 1;
    private File mRclFile;
    private ConcurrentHashMap<String, RoamingChannelList> mRclHash = new ConcurrentHashMap<>();
    private WifiState mState;
    private final WifiNative mWifiNative = WifiInjector.getInstance().getWifiNative();

    public enum WifiState {
        CONNECTED,
        DISCONNECTED,
        ROAM
    }

    public static synchronized WifiRoamingAssistant init(Context context) {
        WifiRoamingAssistant wifiRoamingAssistant;
        synchronized (WifiRoamingAssistant.class) {
            if (mInstance == null) {
                mInstance = new WifiRoamingAssistant(context);
            }
            wifiRoamingAssistant = mInstance;
        }
        return wifiRoamingAssistant;
    }

    public static synchronized WifiRoamingAssistant getInstance() {
        WifiRoamingAssistant wifiRoamingAssistant;
        synchronized (WifiRoamingAssistant.class) {
            wifiRoamingAssistant = mInstance;
        }
        return wifiRoamingAssistant;
    }

    private WifiRoamingAssistant(Context context) {
        String rclPath = Environment.getDataDirectory() + "/misc/wifi/";
        this.mRclFile = new File(rclPath + RCL_FILE_NAME);
        if (new File(rclPath + RCL_FILE_DISABLE).exists()) {
            this.mRclEnabled = 0;
        }
        this.mExceptionalNetworks.add("ollehWiFi");
        this.mExceptionalNetworks.add("olleh GiGA WiFi");
        this.mExceptionalNetworks.add("KT GiGA WiFi");
        this.mExceptionalNetworks.add("KT WiFi");
        this.mExceptionalNetworks.add("T wifi zone");
        this.mExceptionalNetworks.add("U+zone");
        this.mExceptionalNetworks.add("U+zone_5G");
        this.mExceptionalNetworks.add("5G_U+zone");
        this.mExceptionalNetworks.add("0000docomo");
        this.mExceptionalNetworks.add("0001docomo");
        this.mExceptionalNetworks.add("iptime");
        Log.d(TAG, "Initiate Roaming Assistant version 1.0");
        if (DEV) {
            Log.d(TAG, " RCL path " + rclPath);
        }
        this.mState = WifiState.DISCONNECTED;
        this.mLastConnectedNetworkKey = null;
        this.mCachedNetworkKey = null;
        this.mCachedFrequency = 0;
        setState(WifiState.DISCONNECTED);
        readFile();
    }

    private void setState(WifiState stat) {
        if (DEV) {
            Log.d(TAG, String.format(Locale.ENGLISH, " mState is changed [ %s > %s ]", new Object[]{this.mState.name(), stat.name()}));
        }
        this.mState = stat;
    }

    private void resetCache() {
        this.mCachedNetworkKey = null;
        this.mCachedFrequency = 0;
    }

    private void updateCache(String networkKey, int frequency) {
        this.mCachedNetworkKey = networkKey;
        this.mCachedFrequency = frequency;
    }

    private boolean isExceptionalNetwork(String networkKey) {
        Iterator<String> it = this.mExceptionalNetworks.iterator();
        while (it.hasNext()) {
            if (networkKey.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    private void updateHash(String networkKey, RoamingChannelList rcl) {
        if (this.mRclHash.get(networkKey) == null && this.mRclHash.size() >= 16) {
            RoamingChannelList del = null;
            for (Map.Entry<String, RoamingChannelList> entry : this.mRclHash.entrySet()) {
                RoamingChannelList tmp = entry.getValue();
                if (del == null || del.getLastUpdatedTime() > tmp.getLastUpdatedTime()) {
                    del = tmp;
                }
            }
            this.mRclHash.remove(del.getNetworkKey());
        }
        this.mRclHash.put(networkKey, rcl);
    }

    public void updateRcl(String networkKey, int frequency, boolean isConnected) {
        if (DEV) {
            Log.d(TAG, String.format(Locale.ENGLISH, " updateRCL[ %s ][ %d ][ %b ]", new Object[]{networkKey, Integer.valueOf(frequency), Boolean.valueOf(isConnected)}));
        }
        long timeStamp = this.mClock.getWallClockMillis();
        if (isConnected) {
            if (networkKey != null && !isExceptionalNetwork(networkKey)) {
                if (this.mState == WifiState.DISCONNECTED) {
                    RoamingChannelList rcl = this.mRclHash.get(networkKey);
                    if (rcl != null) {
                        sendFrequentlyUsedChannels(rcl);
                    }
                    updateCache(networkKey, frequency);
                    this.mLastConnectedNetworkKey = networkKey;
                    setState(WifiState.CONNECTED);
                } else {
                    long diff = timeStamp - this.mLastUpdatedTime;
                    RoamingChannelList rcl2 = this.mRclHash.get(this.mCachedNetworkKey);
                    if (rcl2 == null) {
                        rcl2 = new RoamingChannelList(this.mCachedNetworkKey);
                    }
                    rcl2.update(timeStamp, diff, this.mCachedFrequency);
                    updateHash(this.mCachedNetworkKey, rcl2);
                    updateCache(networkKey, frequency);
                    setState(WifiState.ROAM);
                }
            } else {
                return;
            }
        } else if (this.mState != WifiState.DISCONNECTED) {
            RoamingChannelList rcl3 = this.mRclHash.get(this.mCachedNetworkKey);
            if (rcl3 != null) {
                rcl3.update(timeStamp, timeStamp - this.mLastUpdatedTime, this.mCachedFrequency);
                updateHash(this.mCachedNetworkKey, rcl3);
                resetCache();
                writeFile();
            }
            setState(WifiState.DISCONNECTED);
        }
        this.mLastUpdatedTime = timeStamp;
    }

    public void onDriverEventReceived(String ssid, ArrayList<Integer> channelList) {
        String str = this.mLastConnectedNetworkKey;
        if (str == null || !str.contains(ssid)) {
            if (DEV) {
                Log.d(TAG, String.format(Locale.ENGLISH, " Discard driver RCL event [ %s ][ %s ]", new Object[]{this.mLastConnectedNetworkKey, ssid}));
            }
        } else if (!isExceptionalNetwork(this.mLastConnectedNetworkKey)) {
            RoamingChannelList rcl = this.mRclHash.get(this.mLastConnectedNetworkKey);
            if (rcl != null) {
                Iterator<Integer> it = channelList.iterator();
                while (it.hasNext()) {
                    rcl.updateHitCount(ieee80211_channel_to_frequency(it.next().intValue()));
                }
                updateHash(this.mLastConnectedNetworkKey, rcl);
                Log.d(TAG, "RCL updated by driver event");
                this.mLastConnectedNetworkKey = null;
            }
        } else if (DEV) {
            Log.d(TAG, " Discard driver RCL event - except network");
        }
    }

    public void forgetNetwork(String networkKey) {
        if (networkKey != null) {
            String str = TAG;
            Log.d(str, networkKey + " RCL removed - forget network");
            this.mRclHash.remove(networkKey);
            if (networkKey.equals(this.mCachedNetworkKey)) {
                setState(WifiState.DISCONNECTED);
            }
            writeFile();
        }
    }

    public void factoryReset() {
        Log.d(TAG, "RCL factory reset - Reset network settings");
        setState(WifiState.DISCONNECTED);
        this.mRclHash.clear();
        writeFile();
    }

    public List<Integer> getNetworkFrequencyList(String networkKey) {
        RoamingChannelList rcl;
        if (this.mState == WifiState.DISCONNECTED || (rcl = this.mRclHash.get(networkKey)) == null) {
            return null;
        }
        return rcl.getFrequencyList();
    }

    private void sendFrequentlyUsedChannels(RoamingChannelList rcl) {
        if (this.mRclEnabled == 0) {
            Log.d(TAG, "RCL is disabled, do not send RCL Command.");
            return;
        }
        List<Integer> list = rcl.getFrequentlyUsedChannel(5);
        if (!list.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            buf.append(list.size());
            for (int i = 0; i < list.size(); i++) {
                buf.append(" ");
                buf.append(list.get(i));
            }
            String mInterfaceName = this.mWifiNative.getClientInterfaceName();
            if (this.mWifiNative.getNCHOMode(mInterfaceName) == 0) {
                String str = TAG;
                Log.d(str, "RCL - addRoamScanChannelsLegacy " + buf);
                this.mWifiNative.addRoamScanChannelsLegacy(mInterfaceName, buf.toString());
                return;
            }
            String str2 = TAG;
            Log.d(str2, "RCL - addRoamScanChannels " + buf);
            this.mWifiNative.addRoamScanChannels(mInterfaceName, buf.toString());
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 7 */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0075, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
        $closeResource(r1, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0079, code lost:
        throw r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeFile() {
        /*
            r7 = this;
            java.io.File r0 = r7.mRclFile
            boolean r0 = r0.exists()
            if (r0 == 0) goto L_0x0019
            java.io.File r0 = r7.mRclFile
            r0.delete()
            boolean r0 = DEV
            if (r0 == 0) goto L_0x0024
            java.lang.String r0 = TAG
            java.lang.String r1 = " write RCL file - RCL file already exist, erase it"
            android.util.Log.d(r0, r1)
            goto L_0x0024
        L_0x0019:
            boolean r0 = DEV
            if (r0 == 0) goto L_0x0024
            java.lang.String r0 = TAG
            java.lang.String r1 = " write RCL file"
            android.util.Log.d(r0, r1)
        L_0x0024:
            java.util.concurrent.ConcurrentHashMap<java.lang.String, com.samsung.android.server.wifi.RoamingChannelList> r0 = r7.mRclHash
            int r0 = r0.size()
            if (r0 != 0) goto L_0x002d
            return
        L_0x002d:
            java.io.BufferedWriter r0 = new java.io.BufferedWriter     // Catch:{ IOException | JSONException -> 0x007a }
            java.io.FileWriter r1 = new java.io.FileWriter     // Catch:{ IOException | JSONException -> 0x007a }
            java.io.File r2 = r7.mRclFile     // Catch:{ IOException | JSONException -> 0x007a }
            r1.<init>(r2)     // Catch:{ IOException | JSONException -> 0x007a }
            r0.<init>(r1)     // Catch:{ IOException | JSONException -> 0x007a }
            r1 = 0
            org.json.JSONObject r2 = new org.json.JSONObject     // Catch:{ all -> 0x0073 }
            r2.<init>()     // Catch:{ all -> 0x0073 }
            org.json.JSONArray r3 = new org.json.JSONArray     // Catch:{ all -> 0x0073 }
            r3.<init>()     // Catch:{ all -> 0x0073 }
            java.util.concurrent.ConcurrentHashMap<java.lang.String, com.samsung.android.server.wifi.RoamingChannelList> r4 = r7.mRclHash     // Catch:{ all -> 0x0073 }
            java.util.Collection r4 = r4.values()     // Catch:{ all -> 0x0073 }
            java.util.Iterator r4 = r4.iterator()     // Catch:{ all -> 0x0073 }
        L_0x004e:
            boolean r5 = r4.hasNext()     // Catch:{ all -> 0x0073 }
            if (r5 == 0) goto L_0x0063
            java.lang.Object r5 = r4.next()     // Catch:{ all -> 0x0073 }
            com.samsung.android.server.wifi.RoamingChannelList r5 = (com.samsung.android.server.wifi.RoamingChannelList) r5     // Catch:{ all -> 0x0073 }
            org.json.JSONObject r6 = r5.toJson()     // Catch:{ all -> 0x0073 }
            r3.put(r6)     // Catch:{ all -> 0x0073 }
            goto L_0x004e
        L_0x0063:
            java.lang.String r4 = "rcl_list"
            r2.put(r4, r3)     // Catch:{ all -> 0x0073 }
            java.lang.String r4 = r2.toString()     // Catch:{ all -> 0x0073 }
            r0.write(r4)     // Catch:{ all -> 0x0073 }
            $closeResource(r1, r0)     // Catch:{ IOException | JSONException -> 0x007a }
            goto L_0x0082
        L_0x0073:
            r1 = move-exception
            throw r1     // Catch:{ all -> 0x0075 }
        L_0x0075:
            r2 = move-exception
            $closeResource(r1, r0)     // Catch:{ IOException | JSONException -> 0x007a }
            throw r2     // Catch:{ IOException | JSONException -> 0x007a }
        L_0x007a:
            r0 = move-exception
            java.lang.String r1 = TAG
            java.lang.String r2 = "writeFile exception"
            android.util.Log.e(r1, r2, r0)
        L_0x0082:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiRoamingAssistant.writeFile():void");
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 9 */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0076, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:?, code lost:
        $closeResource(r1, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x007a, code lost:
        throw r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readFile() {
        /*
            r9 = this;
            java.lang.String r0 = TAG
            java.lang.String r1 = "load RCL file"
            android.util.Log.d(r0, r1)
            java.io.File r0 = r9.mRclFile
            boolean r0 = r0.exists()
            if (r0 != 0) goto L_0x0017
            java.lang.String r0 = TAG
            java.lang.String r1 = "RCL file not exists.."
            android.util.Log.w(r0, r1)
            return
        L_0x0017:
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ IOException | JSONException -> 0x007b }
            java.io.FileReader r1 = new java.io.FileReader     // Catch:{ IOException | JSONException -> 0x007b }
            java.io.File r2 = r9.mRclFile     // Catch:{ IOException | JSONException -> 0x007b }
            r1.<init>(r2)     // Catch:{ IOException | JSONException -> 0x007b }
            r0.<init>(r1)     // Catch:{ IOException | JSONException -> 0x007b }
            r1 = 0
            java.lang.String r2 = r9.getStreamData(r0)     // Catch:{ all -> 0x0074 }
            if (r2 == 0) goto L_0x0069
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch:{ all -> 0x0074 }
            if (r3 == 0) goto L_0x0031
            goto L_0x0069
        L_0x0031:
            org.json.JSONObject r3 = new org.json.JSONObject     // Catch:{ all -> 0x0074 }
            r3.<init>(r2)     // Catch:{ all -> 0x0074 }
            java.lang.String r4 = "rcl_list"
            org.json.JSONArray r4 = r3.optJSONArray(r4)     // Catch:{ all -> 0x0074 }
            if (r4 != 0) goto L_0x0042
            $closeResource(r1, r0)     // Catch:{ IOException | JSONException -> 0x007b }
            return
        L_0x0042:
            r5 = 0
        L_0x0043:
            int r6 = r4.length()     // Catch:{ all -> 0x0074 }
            if (r5 >= r6) goto L_0x0070
            org.json.JSONObject r6 = r4.optJSONObject(r5)     // Catch:{ all -> 0x0074 }
            com.samsung.android.server.wifi.RoamingChannelList r6 = com.samsung.android.server.wifi.RoamingChannelList.fromJson(r6)     // Catch:{ all -> 0x0074 }
            java.lang.String r7 = ""
            java.lang.String r8 = r6.getNetworkKey()     // Catch:{ all -> 0x0074 }
            boolean r7 = r7.equals(r8)     // Catch:{ all -> 0x0074 }
            if (r7 != 0) goto L_0x0066
            java.util.concurrent.ConcurrentHashMap<java.lang.String, com.samsung.android.server.wifi.RoamingChannelList> r7 = r9.mRclHash     // Catch:{ all -> 0x0074 }
            java.lang.String r8 = r6.getNetworkKey()     // Catch:{ all -> 0x0074 }
            r7.put(r8, r6)     // Catch:{ all -> 0x0074 }
        L_0x0066:
            int r5 = r5 + 1
            goto L_0x0043
        L_0x0069:
            java.lang.String r3 = TAG     // Catch:{ all -> 0x0074 }
            java.lang.String r4 = "File Data is null"
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x0074 }
        L_0x0070:
            $closeResource(r1, r0)     // Catch:{ IOException | JSONException -> 0x007b }
            goto L_0x0083
        L_0x0074:
            r1 = move-exception
            throw r1     // Catch:{ all -> 0x0076 }
        L_0x0076:
            r2 = move-exception
            $closeResource(r1, r0)     // Catch:{ IOException | JSONException -> 0x007b }
            throw r2     // Catch:{ IOException | JSONException -> 0x007b }
        L_0x007b:
            r0 = move-exception
            java.lang.String r1 = TAG
            java.lang.String r2 = "readFile exception"
            android.util.Log.e(r1, r2, r0)
        L_0x0083:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.WifiRoamingAssistant.readFile():void");
    }

    private String getStreamData(Reader is) {
        if (is == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(2048);
        try {
            char[] tmp = new char[2048];
            while (true) {
                int read = is.read(tmp);
                int numRead = read;
                if (read <= 0) {
                    break;
                }
                sb.append(tmp, 0, numRead);
            }
        } catch (IOException e) {
            Log.e(TAG, "getStreamData exception", e);
        }
        return sb.toString();
    }

    private int ieee80211_channel_to_frequency(int chan) {
        if (chan <= 0 || chan > 196) {
            return 0;
        }
        if (chan == 14) {
            return 2484;
        }
        if (chan < 14) {
            return (chan * 5) + 2407;
        }
        if (chan >= 182) {
            return (chan * 5) + 4000;
        }
        return (chan * 5) + 5000;
    }
}
