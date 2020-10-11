package com.samsung.android.server.wifi;

import android.net.wifi.WifiScanner;
import android.util.Log;
import com.android.server.wifi.WifiCountryCode;
import com.android.server.wifi.WifiInjector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoamingChannelList {
    private static final int HIT_ARRAY_LEN = 8;
    private static final String JTAG_CURRENT_SLOT = "current_slot";
    private static final String JTAG_FREQ = "frequency";
    private static final String JTAG_HITMAP = "hitmap";
    private static final String JTAG_HIT_TIME = "hit_time";
    private static final String JTAG_LAST_UPDATED_TIME = "last_updated_time";
    private static final String JTAG_NETWORK_KEY = "network_Key";
    private static final String JTAG_REMAIN_TIME = "remain_time";
    private static final String JTAG_TOTAL_COUNT = "total_count";
    /* access modifiers changed from: private */
    public static final String TAG = RoamingChannelList.class.getSimpleName();
    private static final long TIME_INTERVAL_PER_SLOT = 10800000;
    private List<Integer> mAvailableChannels;
    private final Comparator<ChannelData> mChannelDataComparator = new Comparator<ChannelData>() {
        public int compare(ChannelData c1, ChannelData c2) {
            if (c1.totalCount > c2.totalCount) {
                return -1;
            }
            if (c1.totalCount < c2.totalCount) {
                return 1;
            }
            if (c1.hitTime > c2.hitTime) {
                return -1;
            }
            if (c1.hitTime < c2.hitTime) {
                return 1;
            }
            return 0;
        }
    };
    private String mCountryCode;
    private ConcurrentHashMap<Integer, HitArray> mHitChannelMap;
    private long mLastUpdatedTime;
    private String mNetworkKey;
    private long mRemainingTime;
    private int mSlotIdx;

    private static class HitArray {
        long hitTime = 0;
        int[] timeSlot = new int[8];
        int totalCount = 0;

        HitArray() {
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("total: " + this.totalCount + ", Hit per slot: ");
            for (int i = 0; i < 8; i++) {
                sb.append(i + ":" + this.timeSlot[i] + "  ");
            }
            sb.append(", time: " + this.hitTime);
            return sb.toString();
        }

        public JSONObject toJson(int frequency) {
            JSONObject json = new JSONObject();
            try {
                json.put("frequency", frequency);
                json.put(RoamingChannelList.JTAG_HIT_TIME, this.hitTime);
                json.put(RoamingChannelList.JTAG_TOTAL_COUNT, this.totalCount);
                for (int i = 0; i < 8; i++) {
                    json.put(String.valueOf(i), this.timeSlot[i]);
                }
            } catch (JSONException e) {
                String access$000 = RoamingChannelList.TAG;
                Log.e(access$000, "error in HitArray toJson" + e.getMessage());
            }
            return json;
        }

        public static HitArray fromJson(JSONObject json) {
            HitArray hitArr = new HitArray();
            hitArr.hitTime = json.optLong(RoamingChannelList.JTAG_HIT_TIME, 0);
            hitArr.totalCount = json.optInt(RoamingChannelList.JTAG_TOTAL_COUNT, 0);
            for (int i = 0; i < 8; i++) {
                hitArr.timeSlot[i] = json.optInt(String.valueOf(i), 0);
            }
            return hitArr;
        }
    }

    public RoamingChannelList(String networkKey) {
        this.mNetworkKey = networkKey;
        this.mLastUpdatedTime = 0;
        this.mRemainingTime = TIME_INTERVAL_PER_SLOT;
        this.mSlotIdx = 0;
        this.mHitChannelMap = new ConcurrentHashMap<>();
        this.mCountryCode = null;
        this.mAvailableChannels = new ArrayList();
    }

    public String getNetworkKey() {
        return this.mNetworkKey;
    }

    public long getLastUpdatedTime() {
        return this.mLastUpdatedTime;
    }

    private static class ChannelData {
        int frequency;
        long hitTime;
        int totalCount;

        ChannelData(int frequency2, long hitTime2, int totalCount2) {
            this.frequency = frequency2;
            this.hitTime = hitTime2;
            this.totalCount = totalCount2;
        }
    }

    public List<Integer> getFrequentlyUsedChannel(int count) {
        if (this.mHitChannelMap.isEmpty()) {
            Log.w(TAG, "getFrequentlyUsedChannel, but no data");
            return new ArrayList();
        }
        ArrayList<ChannelData> sortedList = new ArrayList<>(this.mHitChannelMap.size());
        for (Map.Entry<Integer, HitArray> entry : this.mHitChannelMap.entrySet()) {
            if (entry.getValue().totalCount != 0) {
                sortedList.add(new ChannelData(entry.getKey().intValue(), entry.getValue().hitTime, entry.getValue().totalCount));
            }
        }
        Collections.sort(sortedList, this.mChannelDataComparator);
        WifiScanner scanner = WifiInjector.getInstance().getWifiScanner();
        if (scanner == null) {
            Log.d(TAG, "WifiScanner is null");
        } else {
            WifiCountryCode countryCode = WifiInjector.getInstance().getWifiCountryCode();
            String code = null;
            if (countryCode != null) {
                code = countryCode.getCountryCode();
            }
            String str = this.mCountryCode;
            if (str == null || !str.equals(code)) {
                Log.d(TAG, String.format(Locale.ENGLISH, "mCountryCode is changed [ %s > %s ]", new Object[]{this.mCountryCode, code}));
                this.mCountryCode = code;
                this.mAvailableChannels = scanner.getAvailableChannels(7);
            }
            if (this.mAvailableChannels.isEmpty()) {
                Log.d(TAG, "There is no available channels.");
            } else {
                int idx = sortedList.size();
                while (idx > 0) {
                    idx--;
                    if (!this.mAvailableChannels.contains(Integer.valueOf(sortedList.get(idx).frequency))) {
                        Log.d(TAG, "This is not a supported channel [" + sortedList.get(idx).frequency + "]");
                        sortedList.remove(idx);
                    }
                }
            }
        }
        if (count > sortedList.size()) {
            count = sortedList.size();
        }
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ret.add(Integer.valueOf(ieee80211_frequency_to_channel(sortedList.get(i).frequency)));
        }
        return ret;
    }

    public List<Integer> getFrequencyList() {
        return (List) this.mHitChannelMap.entrySet().stream().filter($$Lambda$RoamingChannelList$Ue4iRlwQcMgmw0tw7ikS5YjF_k.INSTANCE).map($$Lambda$htemI6hNv3kq1UVGrXpRlPIVXRU.INSTANCE).collect(Collectors.toList());
    }

    static /* synthetic */ boolean lambda$getFrequencyList$0(Map.Entry entry) {
        return ((HitArray) entry.getValue()).totalCount != 0;
    }

    private void increaseHitCount(int frequency, long timeStamp) {
        HitArray ha = this.mHitChannelMap.get(Integer.valueOf(frequency));
        if (ha == null) {
            ha = new HitArray();
        }
        if (timeStamp > 0) {
            ha.hitTime = timeStamp;
        }
        int[] iArr = ha.timeSlot;
        int i = this.mSlotIdx;
        iArr[i] = iArr[i] + 1;
        ha.totalCount++;
        this.mHitChannelMap.put(Integer.valueOf(frequency), ha);
    }

    private void moveCurSlot() {
        this.mSlotIdx++;
        if (this.mSlotIdx >= 8) {
            this.mSlotIdx = 0;
        }
        for (HitArray arr : this.mHitChannelMap.values()) {
            arr.totalCount -= arr.timeSlot[this.mSlotIdx];
            arr.timeSlot[this.mSlotIdx] = 0;
        }
    }

    public void update(long timeStamp, long timeDiff, int frequency) {
        while (timeDiff > TIME_INTERVAL_PER_SLOT) {
            timeDiff -= TIME_INTERVAL_PER_SLOT;
            moveCurSlot();
        }
        long j = this.mRemainingTime;
        if (j > timeDiff) {
            this.mRemainingTime = j - timeDiff;
        } else if (j < timeDiff) {
            moveCurSlot();
            this.mRemainingTime += TIME_INTERVAL_PER_SLOT - timeDiff;
        } else {
            moveCurSlot();
            this.mRemainingTime = TIME_INTERVAL_PER_SLOT;
        }
        if (frequency > 0) {
            increaseHitCount(frequency, timeStamp);
        }
        this.mLastUpdatedTime = timeStamp;
    }

    public void updateHitCount(int frequency) {
        if (frequency > 0) {
            increaseHitCount(frequency, 0);
        }
    }

    private JSONArray getHitMap() {
        JSONArray arr = new JSONArray();
        for (Map.Entry<Integer, HitArray> hcEntry : this.mHitChannelMap.entrySet()) {
            arr.put(hcEntry.getValue().toJson(hcEntry.getKey().intValue()));
        }
        return arr;
    }

    private void setHitMap(JSONArray arr) {
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                this.mHitChannelMap.put(Integer.valueOf(obj.optInt("frequency", 0)), HitArray.fromJson(obj));
            }
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("HitChannelMap : ");
        for (Map.Entry<Integer, HitArray> entry : this.mHitChannelMap.entrySet()) {
            buf.append("\n");
            buf.append("freq : " + entry.getKey() + ", " + entry.getValue().toString());
        }
        return String.format(Locale.ENGLISH, "NetworkKey: %s, slotIdx: %d, LastUpdatedTime: %d, RemainTime = %d\n%s", new Object[]{this.mNetworkKey, Integer.valueOf(this.mSlotIdx), Long.valueOf(this.mLastUpdatedTime), Long.valueOf(this.mRemainingTime), buf.toString()});
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JTAG_LAST_UPDATED_TIME, this.mLastUpdatedTime);
            json.put(JTAG_NETWORK_KEY, this.mNetworkKey);
            json.put(JTAG_CURRENT_SLOT, this.mSlotIdx);
            json.put(JTAG_REMAIN_TIME, this.mRemainingTime);
            json.put(JTAG_HITMAP, getHitMap());
        } catch (JSONException e) {
            String str = TAG;
            Log.e(str, "error in RCL toJson" + e.getMessage());
        }
        return json;
    }

    public static RoamingChannelList fromJson(JSONObject json) {
        RoamingChannelList rcl = new RoamingChannelList(json.optString(JTAG_NETWORK_KEY, ""));
        rcl.mLastUpdatedTime = json.optLong(JTAG_LAST_UPDATED_TIME, 0);
        rcl.mSlotIdx = json.optInt(JTAG_CURRENT_SLOT, 0);
        rcl.mRemainingTime = json.optLong(JTAG_REMAIN_TIME, 0);
        rcl.setHitMap(json.optJSONArray(JTAG_HITMAP));
        String str = TAG;
        Log.d(str, " RCL fromJson : " + rcl.toString());
        return rcl;
    }

    private int ieee80211_frequency_to_channel(int freq) {
        if (freq == 2484) {
            return 14;
        }
        if (freq < 2484) {
            return (freq - 2407) / 5;
        }
        if (freq >= 4910 && freq <= 4980) {
            return (freq - 4000) / 5;
        }
        if (freq <= 45000) {
            return (freq - 5000) / 5;
        }
        if (freq < 58320 || freq > 64800) {
            return 0;
        }
        return (freq - 56160) / 2160;
    }
}
