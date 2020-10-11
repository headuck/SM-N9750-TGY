package com.samsung.android.server.wifi.softap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.sec.enterprise.auditlog.AuditLog;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class SemWifiApClientInfo {
    public static final int AP_STA_DISCONNECT_DELAY = 60000;
    public static final int AP_STA_RECONNECT_DELAY = 10000;
    /* access modifiers changed from: private */
    public static final boolean MHSDBG = ("eng".equals(Build.TYPE) || Debug.semIsProductDev());
    private static final String TAG = "SemWifiApClientInfo";
    private static final String WIFI_AP_DRIVER_STATE_HANGED = "com.samsung.android.net.wifi.WIFI_AP_DRIVER_STATE_HANGED";
    private static final String WIFI_AP_STA_DHCPACK_EVENT = "com.samsung.android.net.wifi.WIFI_AP_STA_DHCPACK_EVENT";
    private static long mMHSOffTime = 0;
    /* access modifiers changed from: private */
    public Intent intent;
    private String mApInterfaceName;
    /* access modifiers changed from: private */
    public boolean mChannelSwitch = false;
    /* access modifiers changed from: private */
    public int mClients = 0;
    /* access modifiers changed from: private */
    public Context mContext;
    private Handler mHandler;
    private Looper mLooper;
    /* access modifiers changed from: private */
    public Hashtable<String, ClientInfo> mMHSClients = new Hashtable<>();
    private List<String> mMHSDumpCSALogs = new ArrayList();
    private List<String> mMHSDumpLogs = new ArrayList();
    private SemWifiApMonitor mSemWifiApMonitor;
    private final BroadcastReceiver mSoftApReceiver;
    private final IntentFilter mSoftApReceiverFilter;
    /* access modifiers changed from: private */
    public String[] mStr = null;
    /* access modifiers changed from: private */
    public WifiNative mWifiNative;
    private String mac;

    public SemWifiApClientInfo(Context context, Looper looper) {
        this.mContext = context;
        this.mLooper = looper;
        this.mSoftApReceiverFilter = new IntentFilter(WIFI_AP_STA_DHCPACK_EVENT);
        this.mSoftApReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(SemWifiApClientInfo.WIFI_AP_STA_DHCPACK_EVENT)) {
                    Log.d(SemWifiApClientInfo.TAG, "softApManager got WIFI_AP_STA_DHCPACK_EVENT");
                    String mMac = (String) intent.getExtra("MAC");
                    if (SemWifiApClientInfo.this.mMHSClients.containsKey(mMac)) {
                        ClientInfo ci = (ClientInfo) SemWifiApClientInfo.this.mMHSClients.get(mMac);
                        String preState = ci.mState;
                        SemWifiApClientInfo.this.MHSClientSetState(mMac, "sta_dhcpack", -1);
                        ci.mIp = (String) intent.getExtra("IP");
                        ci.mDeviceName = (String) intent.getExtra("DEVICE");
                        ci.isInUIList = true;
                        if (preState.equals("sta_assoc")) {
                            ci.mConnectedTime = System.currentTimeMillis();
                            SemWifiApClientInfo semWifiApClientInfo = SemWifiApClientInfo.this;
                            semWifiApClientInfo.addMHSDumpLog("dnsmasq dhcpack mac:" + SemWifiApClientInfo.this.showMacAddress(mMac) + " ip:" + ci.mIp + " name:" + ci.mDeviceName + " mConnectedTime:" + ci.mConnectedTime);
                            int dhcpcnt = SemWifiApClientInfo.this.getClientCntDhcpack();
                            Intent intent2 = new Intent("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
                            intent2.putExtra("EVENT", "sta_join");
                            intent2.putExtra("MAC", ci.mMac);
                            intent2.putExtra("IP", ci.mIp);
                            intent2.putExtra("DEVICE", ci.mDeviceName);
                            intent2.putExtra("TIME", ci.mConnectedTime);
                            intent2.putExtra("NUM", dhcpcnt);
                            Log.d(SemWifiApClientInfo.TAG, "mhs client cnt:" + SemWifiApClientInfo.this.mMHSClients.size() + " d:" + dhcpcnt + " h:" + SemWifiApClientInfo.this.getConnectedDeviceLength());
                            if (SemWifiApClientInfo.MHSDBG) {
                                String unused = SemWifiApClientInfo.this.showClientsInfo();
                            }
                            context.sendBroadcastAsUser(intent2, UserHandle.ALL);
                            intent2.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver");
                            context.sendBroadcastAsUser(intent2, UserHandle.ALL);
                        }
                    }
                }
            }
        };
    }

    private class ClientInfo {
        public boolean isInUIList = false;
        public int mAntmode = 0;
        public int mBw = 0;
        public long mConnectedTime = 0;
        public int mDataRate = 0;
        public String mDeviceName = "";
        public int mDis = 0;
        public String mIp = "";
        public String mMac = "";
        public int mMode = 0;
        public int mMumimo = 9;
        public String mOui = "aa:aa:aa";
        public long mRemovedTime = 0;
        public int mRssi = 100;
        public int mSrsn = 0;
        public String mState = "";
        public int mWrsn = -1;

        ClientInfo(String mac) {
            this.mMac = mac;
            this.mOui = mac.substring(0, 8);
        }

        public void setState(String state, int wrsn) {
            SemWifiApClientInfo.this.addMHSDumpLog("MHSClient setState() [" + SemWifiApClientInfo.this.showMacAddress(this.mMac) + "] " + this.mState + " > " + state + " wrsn: " + wrsn);
            if (state.equals("sta_notidisassoc") || state.equals("sta_disconn")) {
                if (this.mState.equals("sta_assoc")) {
                    if (this.mIp.equals("")) {
                        this.mDis = 1;
                        this.mSrsn = 1;
                    }
                } else if (this.mSrsn == 0) {
                    this.mDis = 2;
                }
            } else if (state.equals("sta_mismatch")) {
                this.mDis = 1;
                this.mSrsn = 2;
            } else if (state.equals("sta_notallow")) {
                this.mDis = 1;
                this.mSrsn = 3;
            } else if (state.equals("disassoc_sta")) {
                this.mDis = 1;
                this.mSrsn = 4;
            } else if (state.equals("sta_disassoc")) {
                this.mDis = 1;
            } else if (state.equals("sta_deauth")) {
                this.mDis = 1;
            }
            if (state.equals("sta_remove")) {
                if (SemWifiApClientInfo.this.mWifiNative != null) {
                    String staList = SemWifiApClientInfo.this.mWifiNative.sendHostapdCommand("GET_STA_INFO " + this.mMac);
                    if (staList != null) {
                        String[] part = staList.split("=|\\s");
                        try {
                            this.mBw = Integer.parseInt(part[10]);
                            this.mRssi = Integer.parseInt(part[11]);
                            this.mDataRate = Integer.parseInt(part[12]);
                            this.mMode = Integer.parseInt(part[13]);
                            this.mAntmode = Integer.parseInt(part[14]);
                            this.mMumimo = Integer.parseInt(part[15]);
                            this.mWrsn = Integer.parseInt(part[16]);
                        } catch (NumberFormatException e) {
                            if (SemWifiApClientInfo.MHSDBG) {
                                Log.d(SemWifiApClientInfo.TAG, "MHDC NumberFormatException occurs");
                            }
                        } catch (ArrayIndexOutOfBoundsException e2) {
                            if (SemWifiApClientInfo.MHSDBG) {
                                Log.d(SemWifiApClientInfo.TAG, "MHDC ArrayIndexOutOfBoundsException occurs");
                            }
                        }
                    }
                }
                int i = this.mSrsn;
                if (i == 1) {
                    Log.d(SemWifiApClientInfo.TAG, "MHSClient => send MHDC ip failed");
                } else if (i == 2) {
                    Log.d(SemWifiApClientInfo.TAG, "MHSClient => send MHDC wrong password ");
                } else if (i == 3) {
                    Log.d(SemWifiApClientInfo.TAG, "MHSClient => send MHDC not allowed");
                } else if (i == 4) {
                    Log.d(SemWifiApClientInfo.TAG, "MHSClient => send MHDC Client removed from allowed list");
                }
                String tdata = this.mOui + " " + this.mDis + " " + this.mSrsn + " " + this.mWrsn + " " + this.mBw + " " + this.mRssi + " " + this.mDataRate + " " + this.mMode + " " + this.mAntmode + " " + this.mMumimo;
                Log.d(SemWifiApClientInfo.TAG, "   =>  send MHDC : " + tdata);
                SemWifiApClientInfo.this.sendMHSBigdata(tdata);
                this.mRemovedTime = System.currentTimeMillis();
                if (!this.isInUIList) {
                    SemWifiApClientInfo.this.mMHSClients.remove(this.mMac);
                }
                if (SemWifiApClientInfo.MHSDBG) {
                    String unused = SemWifiApClientInfo.this.showClientsInfo();
                }
            }
            this.mState = state;
        }

        public String getState() {
            return this.mState;
        }
    }

    public void addMHSDumpLog(String log) {
        StringBuffer value = new StringBuffer();
        Log.i(TAG, log + " mhs: " + this.mMHSDumpLogs.size());
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (this.mMHSDumpLogs.size() > 100) {
            this.mMHSDumpLogs.remove(0);
        }
        this.mMHSDumpLogs.add(value.toString());
    }

    public void addMHSDumpCSALog(String log) {
        StringBuffer value = new StringBuffer();
        Log.i(TAG, log + " mhs: " + this.mMHSDumpCSALogs.size());
        value.append(new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Long.valueOf(System.currentTimeMillis())) + " " + log + "\n");
        if (this.mMHSDumpCSALogs.size() > 100) {
            this.mMHSDumpCSALogs.remove(0);
        }
        this.mMHSDumpCSALogs.add(value.toString());
    }

    public String getDumpLogs() {
        StringBuffer retValue = new StringBuffer();
        retValue.append("--WifiApClientInfo history \n");
        retValue.append(this.mMHSDumpLogs.toString());
        retValue.append("\n--showClientsInfo \n");
        retValue.append(showClientsInfo());
        retValue.append("\n--CSA history \n");
        retValue.append(this.mMHSDumpCSALogs.toString());
        return retValue.toString();
    }

    /* access modifiers changed from: private */
    public void sendMHSBigdata(String aStr) {
        Log.d(TAG, "sendMHSBigdata MHDC " + aStr);
        Message msg = new Message();
        msg.what = 77;
        Bundle args = new Bundle();
        args.putBoolean("bigdata", true);
        args.putString("feature", "MHDC");
        args.putString("data", aStr);
        msg.obj = args;
        ((WifiManager) this.mContext.getSystemService("wifi")).callSECApi(msg);
    }

    public void startReceivingHostapdEvents(String minterface) {
        this.mApInterfaceName = minterface;
        this.mSemWifiApMonitor = WifiInjector.getInstance().getWifiApMonitor();
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        Context context = this.mContext;
        if (context != null) {
            context.registerReceiver(this.mSoftApReceiver, this.mSoftApReceiverFilter);
        }
        if (this.mHandler != null) {
            Log.d(TAG, "mHandler is not null");
            stopReceivingEvents();
            this.mHandler = null;
        }
        if (mMHSOffTime != 0) {
            long tgap = System.currentTimeMillis() - mMHSOffTime;
            Log.i(TAG, " mhs on gap:" + tgap);
            if (tgap > 60000) {
                this.mMHSClients.clear();
            }
        }
        this.mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message message) {
                Message message2 = message;
                Log.d(SemWifiApClientInfo.TAG, "handleMessage" + message2.what);
                switch (message2.what) {
                    case SemWifiApMonitor.AP_STA_DISCONNECTED_EVENT /*548865*/:
                        String str = SemWifiApClientInfo.TAG;
                        String mac = (String) message2.obj;
                        Log.d(str, "AP_STA_DISCONNECTED_EVENT - disconnected_device : " + SemWifiApClientInfo.this.showMacAddress(mac) + " remaining_cnt :" + SemWifiApClientInfo.this.getConnectedDeviceLength());
                        AuditLog.log(5, 4, true, Process.myPid(), "SoftApManager", "Client device disconnected from Wi-Fi hotspot");
                        if (SemWifiApClientInfo.this.mMHSClients.containsKey(mac)) {
                            String str2 = ((ClientInfo) SemWifiApClientInfo.this.mMHSClients.get(mac)).mState;
                            SemWifiApClientInfo.this.MHSClientSetState(mac, "sta_disconn", -1);
                        }
                        Log.d(str, "Channel switch status:" + SemWifiApClientInfo.this.mChannelSwitch);
                        if (SemWifiApClientInfo.this.mChannelSwitch) {
                            Log.d(str, "Wait for 10 sec for reconnection of client. Sending CMD_AP_STA_RECONNECT");
                            sendMessageDelayed(obtainMessage(SemWifiApMonitor.CMD_AP_STA_RECONNECT), 10000);
                            return;
                        }
                        return;
                    case SemWifiApMonitor.AP_STA_ASSOCIATION_EVENT /*548866*/:
                        String mac2 = (String) message2.obj;
                        Log.d(SemWifiApClientInfo.TAG, "AP_STA_ASSOCIATION_EVENT " + SemWifiApClientInfo.this.showMacAddress(mac2) + " remaining_cnt: " + SemWifiApClientInfo.this.getConnectedDeviceLength());
                        SemWifiApClientInfo.this.MHSClientSetState(mac2, "sta_assoc", -1);
                        if (SemWifiApClientInfo.this.mMHSClients.containsKey(mac2)) {
                            ClientInfo ci = (ClientInfo) SemWifiApClientInfo.this.mMHSClients.get(mac2);
                            long tNow = System.currentTimeMillis();
                            String str3 = "com.android.settings";
                            long tgap = tNow - ci.mRemovedTime;
                            SemWifiApClientInfo semWifiApClientInfo = SemWifiApClientInfo.this;
                            StringBuilder sb = new StringBuilder();
                            String str4 = "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver";
                            sb.append("sta_assoc ");
                            sb.append(SemWifiApClientInfo.this.showMacAddress(mac2));
                            sb.append(" gap:");
                            sb.append(tgap);
                            sb.append(" mConnectedTime:");
                            String str5 = SemWifiApClientInfo.TAG;
                            String str6 = "MAC";
                            sb.append(ci.mConnectedTime);
                            semWifiApClientInfo.addMHSDumpLog(sb.toString());
                            if (ci.mConnectedTime != 0 && tNow - ci.mRemovedTime < 60000) {
                                SemWifiApClientInfo.this.MHSClientSetState(mac2, "sta_dhcpack", -1);
                                SemWifiApClientInfo semWifiApClientInfo2 = SemWifiApClientInfo.this;
                                semWifiApClientInfo2.addMHSDumpLog("roaming dhcpack mac:" + SemWifiApClientInfo.this.showMacAddress(mac2) + " ip:" + ci.mIp + " name:" + ci.mDeviceName + " mConnectedTime:" + ci.mConnectedTime + " gap :" + tgap);
                                ci.isInUIList = true;
                                int dhcpcnt = SemWifiApClientInfo.this.getClientCntDhcpack();
                                Intent intent = new Intent("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
                                intent.putExtra("EVENT", "sta_join");
                                intent.putExtra(str6, ci.mMac);
                                intent.putExtra("IP", ci.mIp);
                                intent.putExtra("DEVICE", ci.mDeviceName);
                                intent.putExtra("TIME", ci.mConnectedTime);
                                intent.putExtra("NUM", dhcpcnt);
                                Log.d(str5, "mhs client cnt:" + SemWifiApClientInfo.this.mMHSClients.size() + " d:" + dhcpcnt + " h:" + SemWifiApClientInfo.this.getConnectedDeviceLength());
                                if (SemWifiApClientInfo.MHSDBG) {
                                    String unused = SemWifiApClientInfo.this.showClientsInfo();
                                }
                                SemWifiApClientInfo.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                                intent.setClassName(str3, str4);
                                SemWifiApClientInfo.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                                return;
                            }
                            return;
                        }
                        return;
                    case SemWifiApMonitor.AP_STA_DISASSOCIATION_EVENT /*548867*/:
                        String[] unused2 = SemWifiApClientInfo.this.mStr = ((String) message2.obj).split(" ");
                        String mac3 = SemWifiApClientInfo.this.mStr[0];
                        Log.d(SemWifiApClientInfo.TAG, "AP_STA_DISASSOCIATION_EVENT" + ((String) message2.obj));
                        SemWifiApClientInfo semWifiApClientInfo3 = SemWifiApClientInfo.this;
                        semWifiApClientInfo3.MHSClientSetState(mac3, "sta_disassoc", Integer.parseInt(semWifiApClientInfo3.mStr[1]));
                        return;
                    case SemWifiApMonitor.AP_STA_POSSIBLE_PSK_MISMATCH_EVENT /*548872*/:
                        SemWifiApClientInfo.this.MHSClientSetState((String) message2.obj, "sta_mismatch", -1);
                        return;
                    case SemWifiApMonitor.CTRL_EVENT_DRIVER_STATE_EVENT /*548873*/:
                        Intent unused3 = SemWifiApClientInfo.this.intent = new Intent(SemWifiApClientInfo.WIFI_AP_DRIVER_STATE_HANGED);
                        SemWifiApClientInfo.this.mContext.sendBroadcast(SemWifiApClientInfo.this.intent);
                        SemWifiApClientInfo.this.intent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver");
                        SemWifiApClientInfo.this.mContext.sendBroadcast(SemWifiApClientInfo.this.intent);
                        return;
                    case SemWifiApMonitor.AP_CSA_FINISHED_EVENT /*548874*/:
                        if (WifiInjector.getInstance().getSemWifiApChipInfo().supportWifiSharingLite()) {
                            String frequency = ((String) message2.obj).split("=")[1];
                            Log.d(SemWifiApClientInfo.TAG, "AP_CSA_FINISHED_EVENT : " + frequency);
                            SemWifiApClientInfo.this.addMHSDumpCSALog(frequency);
                            if (frequency != null && frequency.startsWith("5")) {
                                SemWifiApClientInfo semWifiApClientInfo4 = SemWifiApClientInfo.this;
                                int unused4 = semWifiApClientInfo4.mClients = Integer.parseInt(semWifiApClientInfo4.mWifiNative.sendHostapdCommand("NUM_STA"));
                                boolean unused5 = SemWifiApClientInfo.this.mChannelSwitch = true;
                                Log.d(SemWifiApClientInfo.TAG, "Channel switched from 2.4GHz to 5GHz: " + frequency + " Switch flag set to:" + SemWifiApClientInfo.this.mChannelSwitch);
                                sendMessageDelayed(obtainMessage(SemWifiApMonitor.CMD_AP_STA_DISCONNECT), 60000);
                                return;
                            }
                            return;
                        }
                        return;
                    case SemWifiApMonitor.AP_CHANGED_CHANNEL_EVENT /*548875*/:
                        return;
                    case SemWifiApMonitor.AP_STA_NEW_EVENT /*548877*/:
                        String mac4 = (String) message2.obj;
                        ClientInfo ci2 = (ClientInfo) SemWifiApClientInfo.this.mMHSClients.get(mac4);
                        if (ci2 == null || (!ci2.getState().equals("sta_assoc") && !ci2.getState().equals("sta_dhcpack"))) {
                            SemWifiApClientInfo.this.MHSClientSetState(mac4, "sta_new", -1);
                            if (SemWifiApClientInfo.MHSDBG) {
                                String unused6 = SemWifiApClientInfo.this.showClientsInfo();
                            }
                            if (SemWifiApClientInfo.this.mChannelSwitch) {
                                Log.d(SemWifiApClientInfo.TAG, "Resetting the mChannelSwitch");
                                boolean unused7 = SemWifiApClientInfo.this.mChannelSwitch = false;
                                return;
                            }
                            return;
                        }
                        Log.e(SemWifiApClientInfo.TAG, "Got sta_new, but already in associated state, ignoring");
                        return;
                    case SemWifiApMonitor.AP_STA_NOTALLOW_EVENT /*548878*/:
                        SemWifiApClientInfo.this.MHSClientSetState((String) message2.obj, "sta_notallow", -1);
                        return;
                    case SemWifiApMonitor.AP_STA_NOTIFY_DISASSOCIATION_EVENT /*548879*/:
                        SemWifiApClientInfo.this.MHSClientSetState((String) message2.obj, "sta_notidisassoc", -1);
                        return;
                    case SemWifiApMonitor.AP_STA_REMOVE_EVENT /*548880*/:
                        String[] unused8 = SemWifiApClientInfo.this.mStr = ((String) message2.obj).split(" ");
                        String mac5 = SemWifiApClientInfo.this.mStr[0];
                        Log.d(SemWifiApClientInfo.TAG, "AP_STA_REMOVE_EVENT" + ((String) message2.obj));
                        if (SemWifiApClientInfo.this.mMHSClients.containsKey(mac5)) {
                            ClientInfo ci3 = (ClientInfo) SemWifiApClientInfo.this.mMHSClients.get(mac5);
                            String str7 = ci3.mState;
                            SemWifiApClientInfo semWifiApClientInfo5 = SemWifiApClientInfo.this;
                            semWifiApClientInfo5.MHSClientSetState(mac5, "sta_remove", Integer.parseInt(semWifiApClientInfo5.mStr[1]));
                            if (ci3.isInUIList) {
                                ci3.isInUIList = false;
                                Intent unused9 = SemWifiApClientInfo.this.intent = new Intent("com.samsung.android.net.wifi.WIFI_AP_STA_STATUS_CHANGED");
                                SemWifiApClientInfo.this.intent.putExtra("EVENT", "sta_leave");
                                SemWifiApClientInfo.this.intent.putExtra("MAC", mac5);
                                SemWifiApClientInfo.this.intent.putExtra("NUM", SemWifiApClientInfo.this.getClientCntDhcpack());
                                SemWifiApClientInfo.this.mContext.sendBroadcastAsUser(SemWifiApClientInfo.this.intent, UserHandle.ALL);
                                SemWifiApClientInfo.this.intent.setClassName("com.android.settings", "com.samsung.android.settings.wifi.mobileap.WifiApBroadcastReceiver");
                                SemWifiApClientInfo.this.mContext.sendBroadcastAsUser(SemWifiApClientInfo.this.intent, UserHandle.ALL);
                                return;
                            }
                            return;
                        }
                        return;
                    case SemWifiApMonitor.AP_STA_DEAUTH_EVENT /*548881*/:
                        String[] unused10 = SemWifiApClientInfo.this.mStr = ((String) message2.obj).split(" ");
                        String mac6 = SemWifiApClientInfo.this.mStr[0];
                        Log.d(SemWifiApClientInfo.TAG, "AP_STA_DEAUTH_EVENT" + ((String) message2.obj));
                        SemWifiApClientInfo semWifiApClientInfo6 = SemWifiApClientInfo.this;
                        semWifiApClientInfo6.MHSClientSetState(mac6, "sta_deauth", Integer.parseInt(semWifiApClientInfo6.mStr[1]));
                        return;
                    case SemWifiApMonitor.CMD_AP_STA_DISCONNECT /*548884*/:
                        Log.d(SemWifiApClientInfo.TAG, "CMD_AP_STA_DISCONNECT.Current val" + SemWifiApClientInfo.this.mChannelSwitch);
                        boolean unused11 = SemWifiApClientInfo.this.mChannelSwitch = false;
                        Log.d(SemWifiApClientInfo.TAG, "CMD_AP_STA_DISCONNECT.Reset val" + SemWifiApClientInfo.this.mChannelSwitch);
                        return;
                    case SemWifiApMonitor.CMD_AP_STA_RECONNECT /*548885*/:
                        Log.d(SemWifiApClientInfo.TAG, "CMD_AP_STA_RECONNECT.Current val" + SemWifiApClientInfo.this.mChannelSwitch);
                        Log.d(SemWifiApClientInfo.TAG, "Old client list" + SemWifiApClientInfo.this.mClients + "New client list" + SemWifiApClientInfo.this.mWifiNative.sendHostapdCommand("NUM_STA"));
                        int num = Integer.parseInt(SemWifiApClientInfo.this.mWifiNative.sendHostapdCommand("NUM_STA"));
                        if (SemWifiApClientInfo.this.mChannelSwitch && SemWifiApClientInfo.this.mClients > num) {
                            Log.d(SemWifiApClientInfo.TAG, "Reconnect didn't happen in 10 sec");
                            boolean unused12 = SemWifiApClientInfo.this.mChannelSwitch = false;
                            Log.d(SemWifiApClientInfo.TAG, "Sending Broadcast com.samsung.actoin.24GHZ_AP_STA_DISCONNECTED");
                            Intent unused13 = SemWifiApClientInfo.this.intent = new Intent(SemWifiApBroadcastReceiver.AP_STA_24GHZ_DISCONNECTED);
                            SemWifiApClientInfo.this.mContext.sendBroadcast(SemWifiApClientInfo.this.intent);
                            Log.d(SemWifiApClientInfo.TAG, "Channel switch flag reset status:" + SemWifiApClientInfo.this.mChannelSwitch);
                            return;
                        }
                        return;
                    default:
                        Log.d(SemWifiApClientInfo.TAG, "Not Impplemented");
                        return;
                }
            }
        };
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_ASSOCIATION_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_DISCONNECTED_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_DISASSOCIATION_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_POSSIBLE_PSK_MISMATCH_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.CTRL_EVENT_DRIVER_STATE_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_CSA_FINISHED_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_CHANGED_CHANNEL_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_NEW_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_NOTALLOW_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_NOTIFY_DISASSOCIATION_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_REMOVE_EVENT, this.mHandler);
        this.mSemWifiApMonitor.registerHandler(this.mApInterfaceName, SemWifiApMonitor.AP_STA_DEAUTH_EVENT, this.mHandler);
    }

    public void stopReceivingEvents() {
        this.mHandler = null;
        mMHSOffTime = System.currentTimeMillis();
        for (String key : this.mMHSClients.keySet()) {
            MHSClientSetState(key, "sta_disconn", -1);
            this.mMHSClients.get(key).mRemovedTime = System.currentTimeMillis();
            this.mMHSClients.get(key).isInUIList = false;
        }
        Context context = this.mContext;
        if (context != null) {
            context.unregisterReceiver(this.mSoftApReceiver);
        }
        this.mSemWifiApMonitor.unRegisterHandler();
    }

    /* access modifiers changed from: private */
    public synchronized String showClientsInfo() {
        StringBuilder sb;
        Log.d(TAG, "showClientsInfo() size : " + this.mMHSClients.size());
        sb = new StringBuilder();
        int i = 0;
        for (String key : this.mMHSClients.keySet()) {
            ClientInfo ci = this.mMHSClients.get(key);
            sb.append("idx : " + i + " " + showMacAddress(key) + " " + showMacAddress(ci.mMac) + " " + ci.mIp + " " + ci.mDeviceName + " ct:" + ci.mConnectedTime + " rt:" + ci.mRemovedTime + " " + ci.getState() + " isInUIList:" + ci.isInUIList + "\n");
            i++;
        }
        if (MHSDBG) {
            Log.d(TAG, sb.toString());
        }
        return sb.toString();
    }

    /* access modifiers changed from: private */
    public synchronized int getClientCntDhcpack() {
        int rtn;
        rtn = 0;
        int i = 0;
        for (String key : this.mMHSClients.keySet()) {
            if (this.mMHSClients.get(key).getState().equals("sta_dhcpack")) {
                rtn++;
            }
            if (MHSDBG) {
                Log.d(TAG, "idx : " + i + " rtn : " + rtn + " " + showMacAddress(key) + " " + showMacAddress(this.mMHSClients.get(key).mMac) + " " + this.mMHSClients.get(key).getState() + " " + this.mMHSClients.get(key).mConnectedTime);
                i++;
            }
        }
        return rtn;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x006e, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void MHSClientSetState(java.lang.String r6, java.lang.String r7, int r8) {
        /*
            r5 = this;
            monitor-enter(r5)
            java.lang.String r0 = r6.toLowerCase()     // Catch:{ all -> 0x006f }
            java.util.Hashtable<java.lang.String, com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo> r1 = r5.mMHSClients     // Catch:{ all -> 0x006f }
            boolean r1 = r1.containsKey(r0)     // Catch:{ all -> 0x006f }
            if (r1 == 0) goto L_0x0019
            java.util.Hashtable<java.lang.String, com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo> r1 = r5.mMHSClients     // Catch:{ all -> 0x006f }
            java.lang.Object r1 = r1.get(r0)     // Catch:{ all -> 0x006f }
            com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo r1 = (com.samsung.android.server.wifi.softap.SemWifiApClientInfo.ClientInfo) r1     // Catch:{ all -> 0x006f }
            r1.setState(r7, r8)     // Catch:{ all -> 0x006f }
            goto L_0x006d
        L_0x0019:
            java.lang.String r1 = "sta_new"
            boolean r1 = r7.equals(r1)     // Catch:{ all -> 0x006f }
            if (r1 != 0) goto L_0x0045
            java.lang.String r1 = "SemWifiApClientInfo"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x006f }
            r2.<init>()     // Catch:{ all -> 0x006f }
            java.lang.String r3 = " MHSClient do not add "
            r2.append(r3)     // Catch:{ all -> 0x006f }
            java.lang.String r3 = r5.showMacAddress(r6)     // Catch:{ all -> 0x006f }
            r2.append(r3)     // Catch:{ all -> 0x006f }
            java.lang.String r3 = " state :"
            r2.append(r3)     // Catch:{ all -> 0x006f }
            r2.append(r7)     // Catch:{ all -> 0x006f }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x006f }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x006f }
            monitor-exit(r5)
            return
        L_0x0045:
            com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo r1 = new com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo     // Catch:{ all -> 0x006f }
            r1.<init>(r0)     // Catch:{ all -> 0x006f }
            java.util.Hashtable<java.lang.String, com.samsung.android.server.wifi.softap.SemWifiApClientInfo$ClientInfo> r2 = r5.mMHSClients     // Catch:{ all -> 0x006f }
            r2.put(r0, r1)     // Catch:{ all -> 0x006f }
            r2 = -1
            r1.setState(r7, r2)     // Catch:{ all -> 0x006f }
            java.lang.String r2 = "SemWifiApClientInfo"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x006f }
            r3.<init>()     // Catch:{ all -> 0x006f }
            java.lang.String r4 = "new client :"
            r3.append(r4)     // Catch:{ all -> 0x006f }
            java.lang.String r4 = r5.showMacAddress(r0)     // Catch:{ all -> 0x006f }
            r3.append(r4)     // Catch:{ all -> 0x006f }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x006f }
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x006f }
        L_0x006d:
            monitor-exit(r5)
            return
        L_0x006f:
            r6 = move-exception
            monitor-exit(r5)
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.SemWifiApClientInfo.MHSClientSetState(java.lang.String, java.lang.String, int):void");
    }

    /* access modifiers changed from: private */
    public String showMacAddress(String aMac) {
        if (MHSDBG) {
            return aMac;
        }
        return aMac.substring(0, 3) + aMac.substring(12, 17);
    }

    /* access modifiers changed from: protected */
    public int getConnectedDeviceLength() {
        int num = 0;
        String staList = this.mWifiNative.sendHostapdCommand("GET_STA_LIST");
        if (staList != null) {
            num = staList.length() / 18;
        }
        Log.d(TAG, "getAccessPointStaList num is " + num);
        return num;
    }

    public void setAccessPointDisassocSta(String mMac) {
        MHSClientSetState(mMac, "disassoc_sta", -1);
    }

    public synchronized List<String> getWifiApStaListDetail() {
        List<String> rListString;
        rListString = new ArrayList<>();
        for (String key : this.mMHSClients.keySet()) {
            ClientInfo ci = this.mMHSClients.get(key);
            if (ci.getState().equals("sta_dhcpack")) {
                rListString.add(ci.mMac + " " + ci.mIp + " " + ci.mDeviceName + " " + ci.mConnectedTime);
                Log.d(TAG, "wifiap list detail: " + showMacAddress(ci.mMac) + " " + ci.mIp + " " + ci.mDeviceName + " " + ci.mConnectedTime);
            }
        }
        return rListString;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }
}
