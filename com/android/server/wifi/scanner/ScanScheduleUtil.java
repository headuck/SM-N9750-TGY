package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;
import java.util.List;

public class ScanScheduleUtil {
    public static boolean channelEquals(WifiNative.ChannelSettings channel1, WifiNative.ChannelSettings channel2) {
        if (channel1 == null || channel2 == null) {
            return false;
        }
        if (channel1 == channel2) {
            return true;
        }
        return channel1.frequency == channel2.frequency && channel1.dwell_time_ms == channel2.dwell_time_ms && channel1.passive == channel2.passive;
    }

    public static boolean bucketEquals(WifiNative.BucketSettings bucket1, WifiNative.BucketSettings bucket2) {
        if (bucket1 == null || bucket2 == null) {
            return false;
        }
        if (bucket1 == bucket2) {
            return true;
        }
        if (bucket1.bucket != bucket2.bucket || bucket1.band != bucket2.band || bucket1.period_ms != bucket2.period_ms || bucket1.report_events != bucket2.report_events || bucket1.num_channels != bucket2.num_channels) {
            return false;
        }
        for (int c = 0; c < bucket1.num_channels; c++) {
            if (!channelEquals(bucket1.channels[c], bucket2.channels[c])) {
                return false;
            }
        }
        return true;
    }

    public static boolean scheduleEquals(WifiNative.ScanSettings schedule1, WifiNative.ScanSettings schedule2) {
        if (schedule1 == null || schedule2 == null) {
            return false;
        }
        if (schedule1 == schedule2) {
            return true;
        }
        if (schedule1.base_period_ms != schedule2.base_period_ms || schedule1.max_ap_per_scan != schedule2.max_ap_per_scan || schedule1.report_threshold_percent != schedule2.report_threshold_percent || schedule1.report_threshold_num_scans != schedule2.report_threshold_num_scans || schedule1.num_buckets != schedule2.num_buckets) {
            return false;
        }
        for (int b = 0; b < schedule1.num_buckets; b++) {
            if (!bucketEquals(schedule1.buckets[b], schedule2.buckets[b])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBucketMaybeScanned(int scheduledBucket, int bucketsScannedBitSet) {
        if (bucketsScannedBitSet == 0 || scheduledBucket < 0 || ((1 << scheduledBucket) & bucketsScannedBitSet) != 0) {
            return true;
        }
        return false;
    }

    private static boolean isBucketDefinitlyScanned(int scheduledBucket, int bucketsScannedBitSet) {
        if (scheduledBucket < 0) {
            return true;
        }
        if (bucketsScannedBitSet == 0) {
            return false;
        }
        if (((1 << scheduledBucket) & bucketsScannedBitSet) != 0) {
            return true;
        }
        return false;
    }

    public static boolean shouldReportFullScanResultForSettings(ChannelHelper channelHelper, ScanResult result, int bucketsScanned, WifiScanner.ScanSettings settings, int scheduledBucket) {
        if (isBucketMaybeScanned(scheduledBucket, bucketsScanned)) {
            return channelHelper.settingsContainChannel(settings, result.frequency);
        }
        return false;
    }

    public static WifiScanner.ScanData[] filterResultsForSettings(ChannelHelper channelHelper, WifiScanner.ScanData[] scanDatas, WifiScanner.ScanSettings settings, int scheduledBucket) {
        WifiScanner.ScanData[] scanDataArr = scanDatas;
        WifiScanner.ScanSettings scanSettings = settings;
        int i = scheduledBucket;
        List<WifiScanner.ScanData> filteredScanDatas = new ArrayList<>(scanDataArr.length);
        List<ScanResult> filteredResults = new ArrayList<>();
        for (WifiScanner.ScanData scanData : scanDataArr) {
            if (isBucketMaybeScanned(i, scanData.getBucketsScanned())) {
                filteredResults.clear();
                ScanResult[] results = scanData.getResults();
                int length = results.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        ChannelHelper channelHelper2 = channelHelper;
                        break;
                    }
                    ScanResult scanResult = results[i2];
                    if (channelHelper.settingsContainChannel(scanSettings, scanResult.frequency)) {
                        filteredResults.add(scanResult);
                    }
                    if (scanSettings.numBssidsPerScan > 0 && filteredResults.size() >= scanSettings.numBssidsPerScan) {
                        break;
                    }
                    i2++;
                }
                if (filteredResults.size() == scanData.getResults().length) {
                    filteredScanDatas.add(scanData);
                } else if (filteredResults.size() > 0 || isBucketDefinitlyScanned(i, scanData.getBucketsScanned())) {
                    filteredScanDatas.add(new WifiScanner.ScanData(scanData.getId(), scanData.getFlags(), 0, scanData.getBandScanned(), (ScanResult[]) filteredResults.toArray(new ScanResult[filteredResults.size()])));
                }
            } else {
                ChannelHelper channelHelper3 = channelHelper;
            }
        }
        ChannelHelper channelHelper4 = channelHelper;
        if (filteredScanDatas.size() == 0) {
            return null;
        }
        return (WifiScanner.ScanData[]) filteredScanDatas.toArray(new WifiScanner.ScanData[filteredScanDatas.size()]);
    }
}
