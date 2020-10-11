package com.samsung.android.server.wifi.wlansniffer;

import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemService;
import android.util.Log;

class WlanDutServiceCaller {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final int DUT_COMMAND = 1;
    private static final String TAG = "WlanSniffer_WlanDutServiceCaller";
    private static final String WLANDUTSERVICE_PROP_NAME = "init.svc.wlandutservice";

    private IBinder semGetWlanDutService() {
        if (DBG) {
            Log.d(TAG, "semGetWlanDutService");
        }
        IBinder result = null;
        if (!SystemProperties.get(WLANDUTSERVICE_PROP_NAME).contains("run")) {
            Log.e(TAG, "...Start wlandutservice !!!");
            SystemService.start("wlandutservice");
        } else {
            Log.e(TAG, "...wlservice is already started!!!");
        }
        int tryCount = 10;
        while (true) {
            int tryCount2 = tryCount - 1;
            if (tryCount > 0) {
                if (SystemProperties.get(WLANDUTSERVICE_PROP_NAME).contains("run") && (result = ServiceManager.getService("WlanDutService")) != null) {
                    Log.i(TAG, "get WlanDutService successfully");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tryCount = tryCount2;
            } else {
                break;
            }
        }
        return result;
    }

    public String semWlanDutServiceCommand(int cmdId, String command) {
        if (DBG) {
            Log.d(TAG, "semWlanDutServiceCommand : cmdId = " + cmdId + " : command = " + command);
        }
        IBinder mWLService = semGetWlanDutService();
        new StringBuilder();
        if (mWLService != null) {
            try {
                Parcel dutcmd = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                dutcmd.writeInterfaceToken("android.wifi.IWlanDutService");
                dutcmd.writeInt(cmdId);
                dutcmd.writeString(command);
                mWLService.transact(1, dutcmd, reply, 0);
                dutcmd.recycle();
                String dutResult = reply.readString();
                reply.recycle();
                return dutResult;
            } catch (RemoteException ex) {
                Log.e(TAG, "Fail to operate WLService" + ex.toString());
                return "ERROR: Remote exception";
            }
        } else {
            Log.e(TAG, "Can not get WLService");
            return "ERROR: Service is not running (wlandutservice)";
        }
    }

    public String stopWlanDutService() {
        if (!SystemProperties.get(WLANDUTSERVICE_PROP_NAME).contains("run")) {
            return "OK";
        }
        Log.e(TAG, ".....stopping wlandutservice");
        SystemService.stop("wlandutservice");
        return "OK";
    }
}
