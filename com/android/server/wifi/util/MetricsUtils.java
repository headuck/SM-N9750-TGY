package com.android.server.wifi.util;

import android.util.SparseIntArray;

public class MetricsUtils {

    public static class GenericBucket {
        public int count;
        public long end;
        public long start;
    }

    public static class LogHistParms {

        /* renamed from: b */
        public int f43b;

        /* renamed from: bb */
        public double[] f44bb;

        /* renamed from: m */
        public int f45m;
        public double mLog;

        /* renamed from: n */
        public int f46n;

        /* renamed from: p */
        public int f47p;

        /* renamed from: s */
        public int f48s;
        public double[] sbw;

        public LogHistParms(int b, int p, int m, int s, int n) {
            this.f43b = b;
            this.f47p = p;
            this.f45m = m;
            this.f48s = s;
            this.f46n = n;
            this.mLog = Math.log((double) m);
            this.f44bb = new double[n];
            this.sbw = new double[n];
            this.f44bb[0] = (double) (b + p);
            this.sbw[0] = (((double) p) * (((double) m) - 1.0d)) / ((double) s);
            for (int i = 1; i < n; i++) {
                double[] dArr = this.f44bb;
                dArr[i] = (((double) m) * (dArr[i - 1] - ((double) b))) + ((double) b);
                double[] dArr2 = this.sbw;
                dArr2[i] = ((double) m) * dArr2[i - 1];
            }
        }
    }

    public static int addValueToLogHistogram(long x, SparseIntArray histogram, LogHistParms hp) {
        int subBucketIndex;
        double logArg = ((double) (x - ((long) hp.f43b))) / ((double) hp.f47p);
        int bigBucketIndex = -1;
        if (logArg > 0.0d) {
            bigBucketIndex = (int) (Math.log(logArg) / hp.mLog);
        }
        if (bigBucketIndex < 0) {
            bigBucketIndex = 0;
            subBucketIndex = 0;
        } else if (bigBucketIndex >= hp.f46n) {
            bigBucketIndex = hp.f46n - 1;
            subBucketIndex = hp.f48s - 1;
        } else {
            subBucketIndex = (int) ((((double) x) - hp.f44bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
            if (subBucketIndex >= hp.f48s) {
                bigBucketIndex++;
                if (bigBucketIndex >= hp.f46n) {
                    bigBucketIndex = hp.f46n - 1;
                    subBucketIndex = hp.f48s - 1;
                } else {
                    subBucketIndex = (int) ((((double) x) - hp.f44bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
                }
            }
        }
        int key = (hp.f48s * bigBucketIndex) + subBucketIndex;
        int newValue = histogram.get(key) + 1;
        histogram.put(key, newValue);
        return newValue;
    }

    public static GenericBucket[] logHistogramToGenericBuckets(SparseIntArray histogram, LogHistParms hp) {
        GenericBucket[] protoArray = new GenericBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            int key = histogram.keyAt(i);
            protoArray[i] = new GenericBucket();
            protoArray[i].start = (long) (hp.f44bb[key / hp.f48s] + (hp.sbw[key / hp.f48s] * ((double) (key % hp.f48s))));
            protoArray[i].end = (long) (((double) protoArray[i].start) + hp.sbw[key / hp.f48s]);
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }

    public static int addValueToLinearHistogram(int x, SparseIntArray histogram, int[] hp) {
        int bucket = 0;
        int length = hp.length;
        int i = 0;
        while (i < length && x >= hp[i]) {
            bucket++;
            i++;
        }
        int newValue = histogram.get(bucket) + 1;
        histogram.put(bucket, newValue);
        return newValue;
    }

    public static GenericBucket[] linearHistogramToGenericBuckets(SparseIntArray histogram, int[] linearHistParams) {
        GenericBucket[] protoArray = new GenericBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            int bucket = histogram.keyAt(i);
            protoArray[i] = new GenericBucket();
            if (bucket == 0) {
                protoArray[i].start = -2147483648L;
                protoArray[i].end = (long) linearHistParams[0];
            } else if (bucket != linearHistParams.length) {
                protoArray[i].start = (long) linearHistParams[bucket - 1];
                protoArray[i].end = (long) linearHistParams[bucket];
            } else {
                protoArray[i].start = (long) linearHistParams[linearHistParams.length - 1];
                protoArray[i].end = 2147483647L;
            }
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }
}
