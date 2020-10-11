package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.samsung.android.server.wifi.WifiGuiderFeatureController;

public class ScoringParams {
    public static final int BAND2 = 2400;
    public static final int BAND5 = 5000;
    private static final String COMMA_KEY_VAL_STAR = "^(,[A-Za-z_][A-Za-z0-9_]*=[0-9.:+-]+)*$";
    private static final int ENTRY = 1;
    private static final int EXIT = 0;
    private static final int GOOD = 3;
    public static final int MINIMUM_5GHZ_BAND_FREQUENCY_IN_MEGAHERTZ = 5000;
    private static final int SUFFICIENT = 2;
    private static final String TAG = "WifiScoringParams";
    private Values mVal = new Values();

    private class Values {
        public static final String KEY_EXPID = "expid";
        public static final String KEY_HORIZON = "horizon";
        public static final String KEY_NUD = "nud";
        public static final String KEY_PPS = "pps";
        public static final String KEY_RSSI2 = "rssi2";
        public static final String KEY_RSSI5 = "rssi5";
        public static final int MAX_EXPID = Integer.MAX_VALUE;
        public static final int MAX_HORIZON = 60;
        public static final int MAX_NUD = 10;
        public static final int MIN_EXPID = 0;
        public static final int MIN_HORIZON = -9;
        public static final int MIN_NUD = 0;
        public int expid = 0;
        public int horizon = 15;
        public int nud = 8;
        public final int[] pps = {0, 1, 100};
        public final int[] rssi2 = {-83, -80, -73, -60};
        public final int[] rssi5 = {-80, -77, -70, -57};

        Values() {
        }

        Values(Values source) {
            int i = 0;
            while (true) {
                int[] iArr = this.rssi2;
                if (i >= iArr.length) {
                    break;
                }
                iArr[i] = source.rssi2[i];
                i++;
            }
            int i2 = 0;
            while (true) {
                int[] iArr2 = this.rssi5;
                if (i2 >= iArr2.length) {
                    break;
                }
                iArr2[i2] = source.rssi5[i2];
                i2++;
            }
            int i3 = 0;
            while (true) {
                int[] iArr3 = this.pps;
                if (i3 < iArr3.length) {
                    iArr3[i3] = source.pps[i3];
                    i3++;
                } else {
                    this.horizon = source.horizon;
                    this.nud = source.nud;
                    this.expid = source.expid;
                    return;
                }
            }
        }

        public void validate() throws IllegalArgumentException {
            validateRssiArray(this.rssi2);
            validateRssiArray(this.rssi5);
            validateOrderedNonNegativeArray(this.pps);
            validateRange(this.horizon, -9, 60);
            validateRange(this.nud, 0, 10);
            validateRange(this.expid, 0, Integer.MAX_VALUE);
        }

        private void validateRssiArray(int[] rssi) throws IllegalArgumentException {
            int low = -126;
            int high = Math.min(200, -1);
            for (int i = 0; i < rssi.length; i++) {
                validateRange(rssi[i], low, high);
                low = rssi[i];
            }
        }

        private void validateRange(int k, int low, int high) throws IllegalArgumentException {
            if (k < low || k > high) {
                throw new IllegalArgumentException();
            }
        }

        private void validateOrderedNonNegativeArray(int[] a) throws IllegalArgumentException {
            int low = 0;
            int i = 0;
            while (i < a.length) {
                if (a[i] >= low) {
                    low = a[i];
                    i++;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }

        public void parseString(String kvList) throws IllegalArgumentException {
            KeyValueListParser parser = new KeyValueListParser(',');
            parser.setString(kvList);
            int size = parser.size();
            if (size == ("" + kvList).split(",").length) {
                updateIntArray(this.rssi2, parser, KEY_RSSI2);
                updateIntArray(this.rssi5, parser, KEY_RSSI5);
                updateIntArray(this.pps, parser, KEY_PPS);
                this.horizon = updateInt(parser, KEY_HORIZON, this.horizon);
                this.nud = updateInt(parser, KEY_NUD, this.nud);
                this.expid = updateInt(parser, KEY_EXPID, this.expid);
                return;
            }
            throw new IllegalArgumentException("dup keys");
        }

        private int updateInt(KeyValueListParser parser, String key, int defaultValue) throws IllegalArgumentException {
            String value = parser.getString(key, (String) null);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }

        private void updateIntArray(int[] dest, KeyValueListParser parser, String key) throws IllegalArgumentException {
            if (parser.getString(key, (String) null) != null) {
                int[] ints = parser.getIntArray(key, (int[]) null);
                if (ints == null) {
                    throw new IllegalArgumentException();
                } else if (ints.length == dest.length) {
                    for (int i = 0; i < dest.length; i++) {
                        dest[i] = ints[i];
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendKey(sb, KEY_RSSI2);
            appendInts(sb, this.rssi2);
            appendKey(sb, KEY_RSSI5);
            appendInts(sb, this.rssi5);
            appendKey(sb, KEY_PPS);
            appendInts(sb, this.pps);
            appendKey(sb, KEY_HORIZON);
            sb.append(this.horizon);
            appendKey(sb, KEY_NUD);
            sb.append(this.nud);
            appendKey(sb, KEY_EXPID);
            sb.append(this.expid);
            return sb.toString();
        }

        private void appendKey(StringBuilder sb, String key) {
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(key);
            sb.append("=");
        }

        private void appendInts(StringBuilder sb, int[] a) {
            int n = a.length;
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    sb.append(":");
                }
                sb.append(a[i]);
            }
        }
    }

    public ScoringParams() {
    }

    public ScoringParams(Context context) {
        loadResources(context);
    }

    public ScoringParams(Context context, FrameworkFacade facade, Handler handler) {
        loadResources(context);
        setupContentObserver(context, facade, handler);
    }

    private void loadResources(Context context) {
        this.mVal.rssi2[0] = context.getResources().getInteger(17695004);
        this.mVal.rssi2[1] = WifiGuiderFeatureController.getInstance(context).getRssi24Threshold();
        this.mVal.rssi2[2] = context.getResources().getInteger(17695010);
        this.mVal.rssi2[3] = context.getResources().getInteger(17695008);
        this.mVal.rssi5[0] = context.getResources().getInteger(17695005);
        this.mVal.rssi5[1] = WifiGuiderFeatureController.getInstance(context).getRssi5Threshold();
        this.mVal.rssi5[2] = context.getResources().getInteger(17695011);
        this.mVal.rssi5[3] = context.getResources().getInteger(17695009);
        try {
            this.mVal.validate();
        } catch (IllegalArgumentException e) {
            Log.wtf(TAG, "Inconsistent config_wifi_framework_ resources: " + this, e);
        }
    }

    private void setupContentObserver(Context context, FrameworkFacade facade, Handler handler) {
        final FrameworkFacade frameworkFacade = facade;
        final Context context2 = context;
        final String scoringParams = toString();
        ContentObserver observer = new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                String params = frameworkFacade.getStringSetting(context2, "wifi_score_params");
                this.update(scoringParams);
                if (!this.update(params)) {
                    Log.e(ScoringParams.TAG, "Error in wifi_score_params: " + ScoringParams.this.sanitize(params));
                }
                Log.i(ScoringParams.TAG, this.toString());
            }
        };
        facade.registerContentObserver(context, Settings.Global.getUriFor("wifi_score_params"), true, observer);
        observer.onChange(false);
    }

    public boolean update(String kvList) {
        if (kvList == null || "".equals(kvList)) {
            return true;
        }
        if (!("," + kvList).matches(COMMA_KEY_VAL_STAR)) {
            return false;
        }
        Values v = new Values(this.mVal);
        try {
            v.parseString(kvList);
            v.validate();
            this.mVal = v;
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String sanitize(String params) {
        if (params == null) {
            return "";
        }
        String printable = params.replaceAll("[^A-Za-z_0-9=,:.+-]", "?");
        if (printable.length() <= 100) {
            return printable;
        }
        return printable.substring(0, 98) + "...";
    }

    public int getExitRssi(int frequencyMegaHertz) {
        return getRssiArray(frequencyMegaHertz)[0];
    }

    public int getEntryRssi(int frequencyMegaHertz) {
        return getRssiArray(frequencyMegaHertz)[1];
    }

    public int getSufficientRssi(int frequencyMegaHertz) {
        return getRssiArray(frequencyMegaHertz)[2];
    }

    public int getGoodRssi(int frequencyMegaHertz) {
        return getRssiArray(frequencyMegaHertz)[3];
    }

    public int getHorizonSeconds() {
        return this.mVal.horizon;
    }

    public int getYippeeSkippyPacketsPerSecond() {
        return this.mVal.pps[2];
    }

    public int getNudKnob() {
        return this.mVal.nud;
    }

    public int getExperimentIdentifier() {
        return this.mVal.expid;
    }

    private int[] getRssiArray(int frequency) {
        if (frequency < 5000) {
            return this.mVal.rssi2;
        }
        return this.mVal.rssi5;
    }

    public String toString() {
        return this.mVal.toString();
    }
}
