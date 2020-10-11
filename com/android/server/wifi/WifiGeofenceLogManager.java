package com.android.server.wifi;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WifiGeofenceLogManager extends Handler {
    public static final String TAG = "WifiGeofenceLogManager";
    private final Object mGeofenceDumpLogLock = new Object();
    private List<String> mGeofenceIntentHistoricalDumpLogs = new ArrayList();

    public WifiGeofenceLogManager(Looper workerLooper) {
        super(workerLooper);
    }

    public void handleMessage(Message msg) {
        addGeofenceIntentHistoricalDumpLog((String) msg.obj);
    }

    private void addGeofenceIntentHistoricalDumpLog(String log) {
        StringBuilder logPrint = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, hh:mm:ss a");
        Date dt = new Date();
        logPrint.append(sdf.format(dt).toString() + "  " + log);
        synchronized (this.mGeofenceDumpLogLock) {
            try {
                if (this.mGeofenceIntentHistoricalDumpLogs.size() > 3000) {
                    this.mGeofenceIntentHistoricalDumpLogs.remove(0);
                }
                this.mGeofenceIntentHistoricalDumpLogs.add(logPrint.toString());
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Geofence Historical log ArrayIndexOutOfBoundsException !!");
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mGeofenceDumpLogLock) {
            pw.println("Geofence intent history : ");
            for (String geofenceDump : this.mGeofenceIntentHistoricalDumpLogs) {
                pw.println(geofenceDump);
            }
            pw.println();
        }
    }
}
