package com.samsung.android.server.insync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.sepunion.AbsSemSystemService;
import com.samsung.android.net.ISemInsyncEventListener;
import com.samsung.android.net.ISemInsyncManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SemInsyncService extends ISemInsyncManager.Stub implements AbsSemSystemService {
    private static final int DELAY_TO_BIND_MCF_SERVICE_AFTER_BOOTED = 10000;
    private static final int DELAY_TO_QUICKLY_REBIND_MCF_SERVICE = 3000;
    private static final int DELAY_TO_REBIND_MCF_SERVICE = 60000;
    private static final int EVENT_BIND_MCF_SERVICE = 1;
    private static final int EVENT_FORCED_REBIND_MCF_SERVICE = 2;
    private static final int EVENT_SWITCH_USER = 3;
    private static final String MCF_PACKAGE_NAME = "com.samsung.android.mcfserver";
    /* access modifiers changed from: private */
    public static final String TAG = SemInsyncService.class.getSimpleName();
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private MainHandler mHandler;
    private boolean mIsBootCompleted = false;
    /* access modifiers changed from: private */
    public int mUserHandle;

    public SemInsyncService(Context context) {
        Log.d(TAG, "SemInsyncService created");
    }

    public void onCreate(Bundle opt) {
        String str = TAG;
        Log.d(str, "onCreate: opt=" + opt);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    private class InsyncBroadcastReceiver extends BroadcastReceiver {
        private InsyncBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -810471698) {
                    if (hashCode != 798292259) {
                        if (hashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 0;
                        }
                    } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        c = 2;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                    c = 1;
                }
                if (c == 0 || c == 1) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        String packageName = uri.getSchemeSpecificPart();
                        if (SemInsyncService.MCF_PACKAGE_NAME.equals(packageName)) {
                            Log.d(SemInsyncService.TAG, packageName + " installed");
                            SemInsyncService.this.tryToSendMessageToBindMcfService(0);
                        }
                    }
                } else if (c == 2) {
                    SemInsyncService.this.tryToSendMessageToBindMcfService(0);
                }
            }
        }
    }

    private boolean isPackageInstalled(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (ApplicationInfo packageInfo : this.mContext.getPackageManager().getInstalledApplications(128)) {
            if (packageName.equals(packageInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                SemInsyncService.this.bindMcfService();
            } else if (i == 2) {
                SemInsyncService.this.unbindMcfService();
                SemInsyncService.this.tryToSendMessageToBindMcfService(0);
            } else if (i == 3) {
                int unused = SemInsyncService.this.mUserHandle = msg.arg1;
                if (SemInsyncService.this.mUserHandle == 0) {
                    SemInsyncService.this.tryToSendMessageToBindMcfService(0);
                } else {
                    SemInsyncService.this.unbindMcfService();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void tryToSendMessageToBindMcfService(int delayMillis) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, (long) delayMillis);
    }

    private void forcedRecoverMcfServiceConnection() {
        Log.d(TAG, "forcedRecoverMcfServiceConnection: Recover MCF service connection after 3 seconds");
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 3000);
    }

    /* access modifiers changed from: private */
    public void unbindMcfService() {
        Log.d(TAG, "unbindMcfService ");
    }

    /* access modifiers changed from: private */
    public void bindMcfService() {
        Log.d(TAG, "bindMcfService ");
    }

    public boolean register(ISemInsyncEventListener listener) {
        String str = TAG;
        Log.d(str, "enter register: " + listener.asBinder());
        return false;
    }

    public boolean unregister(ISemInsyncEventListener listener) {
        String str = TAG;
        Log.d(str, "enter unregister: " + listener.asBinder());
        return false;
    }

    public void onStart() {
    }

    public void onBootPhase(int phase) {
    }

    public void onStartUser(int userHandle) {
    }

    public void onStopUser(int userHandle) {
    }

    public void onSwitchUser(int userHandle) {
        String str = TAG;
        Log.d(str, "onSwitchUser: userHandle=" + userHandle);
    }

    public void onUnlockUser(int userHandle) {
    }

    public void onCleanupUser(int userHandle) {
    }

    public AbsSemSystemService getSemSystemService(String name) {
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }
}
