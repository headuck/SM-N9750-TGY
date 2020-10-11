package com.samsung.android.server.wifi.dqa;

import android.os.Bundle;
import java.text.SimpleDateFormat;

public class ReportData {
    public Bundle mData;
    public int mReportId;
    public long mTime;

    public ReportData(int reportId, Bundle data) {
        this(reportId, data, System.currentTimeMillis());
    }

    public ReportData(int reportId, Bundle data, long time) {
        this.mReportId = reportId;
        this.mData = updateSpecialCharValues(data);
        this.mTime = time;
    }

    private Bundle updateSpecialCharValues(Bundle data) {
        for (String key : data.keySet()) {
            Object value = data.get(key);
            if (value instanceof String) {
                data.putString(key, ((String) value).replace(' ', '.').replace('\\', '.').replace('=', ':'));
            }
        }
        return data;
    }

    private String getHumanReadableTime() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Long.valueOf(this.mTime));
        } catch (Exception e) {
            return String.valueOf(this.mTime);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("htime=" + getHumanReadableTime());
        sb.append(" ");
        sb.append("time=" + this.mTime);
        sb.append(" ");
        sb.append("rid=" + this.mReportId);
        sb.append(" ");
        for (String key : this.mData.keySet()) {
            sb.append(key);
            sb.append("=");
            Object value = this.mData.get(key);
            if (value == null) {
                sb.append("null");
                sb.append(" ");
            } else {
                sb.append(value.toString());
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
