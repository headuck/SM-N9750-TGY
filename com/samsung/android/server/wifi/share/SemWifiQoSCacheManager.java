package com.samsung.android.server.wifi.share;

import android.os.SystemClock;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class SemWifiQoSCacheManager {
    private static final long LIFE_TIME = 14400000;
    private static final long LIFE_TIME_FOR_SPECIAL_NETWORK = 28800000;
    private static final long REPLACE_MIN_TIME = 120000;
    private static final String TAG = "WifiProfileShare.Cache";
    private final Map<String, NetworkScoreData> mMap = new HashMap();

    interface ConfigKeyProvider {
        String getConfigKey(String str);
    }

    SemWifiQoSCacheManager() {
    }

    /* access modifiers changed from: package-private */
    public synchronized Map<String, Map<String, Integer>> getScores(List<String> bssids) {
        Map<String, Map<String, Integer>> ret;
        ret = new HashMap<>();
        if (bssids == null) {
            for (String bssid : this.mMap.keySet()) {
                ret.put(bssid, this.mMap.get(bssid).getRealNetworkScores());
            }
        } else if (bssids.size() == 0) {
            for (String bssid2 : this.mMap.keySet()) {
                ret.put(bssid2, this.mMap.get(bssid2).getNetworkScores());
            }
        } else {
            for (String bssid3 : bssids) {
                if (this.mMap.containsKey(bssid3)) {
                    ret.put(bssid3, this.mMap.get(bssid3).getNetworkScores());
                }
            }
            Log.v(TAG, "getSocres req:" + bssids.size() + " return:" + ret.size());
        }
        return ret;
    }

    /* access modifiers changed from: package-private */
    public synchronized void addOrUpdateScore(String bssid, int[] speedData) {
        if (!(bssid == null || speedData == null)) {
            if (speedData.length == 4) {
                NetworkScoreData score = this.mMap.get(bssid);
                if (score == null) {
                    score = new NetworkScoreData(NetworkType.from(speedData[0]), new Speed[]{Speed.from(speedData[1]), Speed.from(speedData[2]), Speed.from(speedData[3])});
                    Log.v(TAG, "add network score " + bssid);
                } else {
                    score.update(NetworkType.from(speedData[0]), new Speed[]{Speed.from(speedData[1]), Speed.from(speedData[2]), Speed.from(speedData[3])});
                    Log.v(TAG, "update network score " + bssid);
                }
                this.mMap.put(bssid, score);
                return;
            }
        }
        Log.e(TAG, "addOrUpdateScore - invalid score data");
    }

    /* access modifiers changed from: package-private */
    public synchronized void removeAll() {
        Log.v(TAG, "remove all cached score data");
        this.mMap.clear();
    }

    /* access modifiers changed from: package-private */
    public synchronized void removeOldItems() {
        long now = SystemClock.elapsedRealtime();
        Iterator<String> itor = this.mMap.keySet().iterator();
        while (itor.hasNext()) {
            String bssid = itor.next();
            NetworkScoreData data = this.mMap.get(bssid);
            if (data != null) {
                long diff = now - data.mLastUpdateTime;
                if (diff > LIFE_TIME && data.mNetworkType == NetworkType.NORMAL) {
                    Log.v(TAG, "remove old qos data of normal network " + bssid + " created at " + data.mLastUpdateTime);
                    itor.remove();
                } else if (diff > LIFE_TIME_FOR_SPECIAL_NETWORK) {
                    Log.v(TAG, "remove old qos data of special network " + bssid + " created at " + data.mLastUpdateTime);
                    itor.remove();
                }
            }
        }
    }

    static String printCachedScores(Map<String, Map<String, Integer>> cachedData, ConfigKeyProvider provider) {
        StringBuilder sb = new StringBuilder();
        if (cachedData != null) {
            sb.append(TAG);
            sb.append("_ScoreData: ");
            sb.append(cachedData.size());
            for (String bssid : cachedData.keySet()) {
                sb.append("\n");
                sb.append(provider.getConfigKey(bssid));
                sb.append(" (");
                sb.append(bssid);
                sb.append(") -");
                SortedMap<String, Integer> values = new TreeMap<>(cachedData.get(bssid));
                for (String valueKey : values.keySet()) {
                    sb.append(" ");
                    sb.append(valueKey);
                    sb.append(":");
                    sb.append(values.get(valueKey));
                }
            }
        }
        return sb.toString();
    }

    static int[] getQoSSpeedInt(int[] networkScore) {
        if (networkScore == null || networkScore.length != 4) {
            return null;
        }
        return new int[]{networkScore[0], NetworkScoreData.getSpeed(networkScore[1]).bitValue, NetworkScoreData.getSpeed(networkScore[2]).bitValue, NetworkScoreData.getSpeed(networkScore[3]).bitValue};
    }

    enum NetworkType {
        NORMAL,
        CAPTIVE_PORTAL,
        NO_INTERNET,
        SUSPICIOUS;

        static NetworkType from(int ordinal) {
            for (NetworkType type : values()) {
                if (type.ordinal() == ordinal) {
                    return type;
                }
            }
            return NORMAL;
        }
    }

    enum Speed {
        UNKNOWN(0, 0),
        SLOW(1, 5),
        NORMAL(2, 10),
        FAST(3, 20),
        VERY_FAST(4, 30);
        
        final int bitValue;
        final int score;

        private Speed(int bitValue2, int score2) {
            this.bitValue = bitValue2;
            this.score = score2;
        }

        static Speed from(int bitValue2) {
            if (bitValue2 <= 0) {
                return UNKNOWN;
            }
            for (Speed speed : values()) {
                if (speed.bitValue == bitValue2) {
                    return speed;
                }
            }
            return UNKNOWN;
        }
    }

    private static class NetworkScoreData {
        private static final String KEY_LEVEL_MAX_MINUS1_SPEED = "levelMax-1";
        private static final String KEY_LEVEL_MAX_MINUS2_SPEED = "levelMax-2";
        private static final String KEY_LEVEL_MAX_SPEED = "levelMax";
        private static final String KEY_NETWORK_TYPE = "networkType";
        long mLastUpdateTime;
        int[] mNetworkScores;
        NetworkType mNetworkType;
        Speed[] mSpeeds;

        private NetworkScoreData(NetworkType networkType, Speed[] speeds) {
            this.mSpeeds = new Speed[speeds.length];
            this.mNetworkScores = new int[speeds.length];
            update(networkType, speeds);
            this.mLastUpdateTime = SystemClock.elapsedRealtime();
        }

        /* access modifiers changed from: private */
        public Map<String, Integer> getNetworkScores() {
            Map<String, Integer> ret = new HashMap<>();
            ret.put(KEY_NETWORK_TYPE, Integer.valueOf(this.mNetworkType.ordinal()));
            ret.put(KEY_LEVEL_MAX_SPEED, Integer.valueOf(this.mSpeeds[0].score));
            ret.put(KEY_LEVEL_MAX_MINUS1_SPEED, Integer.valueOf(this.mSpeeds[1].score));
            ret.put(KEY_LEVEL_MAX_MINUS2_SPEED, Integer.valueOf(this.mSpeeds[2].score));
            return ret;
        }

        /* access modifiers changed from: private */
        public Map<String, Integer> getRealNetworkScores() {
            Map<String, Integer> ret = new HashMap<>();
            ret.put(KEY_NETWORK_TYPE, Integer.valueOf(this.mNetworkType.ordinal()));
            ret.put(KEY_LEVEL_MAX_SPEED, Integer.valueOf(this.mNetworkScores[0]));
            ret.put(KEY_LEVEL_MAX_MINUS1_SPEED, Integer.valueOf(this.mNetworkScores[1]));
            ret.put(KEY_LEVEL_MAX_MINUS2_SPEED, Integer.valueOf(this.mNetworkScores[2]));
            return ret;
        }

        private void validateNetworkScoreAndUpdateSpeed() {
            Speed lastValue = Speed.UNKNOWN;
            for (int i = this.mNetworkScores.length - 1; i >= 0; i--) {
                Speed current = getSpeed(this.mNetworkScores[i]);
                if (lastValue != Speed.UNKNOWN && current.bitValue < lastValue.bitValue) {
                    this.mSpeeds[i] = lastValue;
                }
                if (current != Speed.UNKNOWN) {
                    lastValue = current;
                }
            }
            Speed lastValue2 = Speed.UNKNOWN;
            int i2 = 0;
            while (true) {
                int[] iArr = this.mNetworkScores;
                if (i2 < iArr.length) {
                    Speed speed = getSpeed(iArr[i2]);
                    if (lastValue2 == Speed.UNKNOWN || !(this.mSpeeds[i2] == Speed.UNKNOWN || speed == Speed.UNKNOWN)) {
                        lastValue2 = speed;
                    } else {
                        if (i2 + 1 < this.mNetworkScores.length) {
                            Speed[] speedArr = this.mSpeeds;
                            if (speedArr[i2 + 1] == lastValue2) {
                                speedArr[i2] = lastValue2;
                                lastValue2 = this.mSpeeds[i2];
                            }
                        }
                        if (lastValue2 == Speed.SLOW) {
                            this.mSpeeds[i2] = lastValue2;
                        } else {
                            Speed newSpeed = Speed.from(lastValue2.bitValue - 1);
                            if (this.mSpeeds[i2].bitValue < newSpeed.bitValue) {
                                this.mSpeeds[i2] = newSpeed;
                            }
                        }
                        lastValue2 = this.mSpeeds[i2];
                    }
                    i2++;
                } else {
                    return;
                }
            }
        }

        /* access modifiers changed from: private */
        public void update(NetworkType networkType, Speed[] newSpeeds) {
            if (newSpeeds.length != this.mNetworkScores.length) {
                Log.e(SemWifiQoSCacheManager.TAG, "received QoS data length is invalid " + newSpeeds.length);
                return;
            }
            if (this.mNetworkType != NetworkType.SUSPICIOUS) {
                this.mNetworkType = networkType;
            }
            long now = SystemClock.elapsedRealtime();
            boolean replaceOldScore = now - this.mLastUpdateTime > SemWifiQoSCacheManager.REPLACE_MIN_TIME;
            for (int i = this.mNetworkScores.length - 1; i >= 0; i--) {
                if (replaceOldScore || this.mNetworkScores[i] == Speed.UNKNOWN.score) {
                    this.mNetworkScores[i] = newSpeeds[i].score;
                } else if (newSpeeds[i] != Speed.UNKNOWN) {
                    int[] iArr = this.mNetworkScores;
                    iArr[i] = (iArr[i] + newSpeeds[i].score) / 2;
                }
                if (this.mNetworkScores[i] != Speed.UNKNOWN.score) {
                    int i2 = i + 1;
                    int[] iArr2 = this.mNetworkScores;
                    if (i2 < iArr2.length) {
                        iArr2[i] = Math.max(iArr2[i], iArr2[i + 1]);
                    }
                }
                this.mSpeeds[i] = getSpeed(this.mNetworkScores[i]);
            }
            validateNetworkScoreAndUpdateSpeed();
            this.mLastUpdateTime = now;
        }

        /* access modifiers changed from: private */
        public static Speed getSpeed(int networkScore) {
            if (networkScore == Speed.UNKNOWN.score) {
                return Speed.UNKNOWN;
            }
            if (networkScore >= ((Speed.VERY_FAST.score - Speed.FAST.score) / 2) + Speed.FAST.score) {
                return Speed.VERY_FAST;
            }
            if (networkScore >= ((Speed.FAST.score - Speed.NORMAL.score) / 2) + Speed.NORMAL.score) {
                return Speed.FAST;
            }
            if (networkScore >= ((Speed.NORMAL.score - Speed.SLOW.score) / 2) + Speed.SLOW.score) {
                return Speed.NORMAL;
            }
            if (networkScore >= Speed.SLOW.score / 2) {
                return Speed.SLOW;
            }
            return Speed.UNKNOWN;
        }
    }
}
