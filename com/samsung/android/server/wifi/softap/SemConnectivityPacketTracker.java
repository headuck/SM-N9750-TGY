package com.samsung.android.server.wifi.softap;

import android.net.util.InterfaceParams;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.util.HexDump;
import java.io.FileDescriptor;
import java.io.IOException;

public class SemConnectivityPacketTracker {
    private static final boolean DBG = false;
    private static final String MARK_NAMED_START = "--- START (%s) ---";
    private static final String MARK_NAMED_STOP = "--- STOP (%s) ---";
    private static final String MARK_START = "--- START ---";
    private static final String MARK_STOP = "--- STOP ---";
    private static final String TAG = "SemConnectivityPacketTracker";
    /* access modifiers changed from: private */
    public String mDisplayName;
    /* access modifiers changed from: private */
    public final LocalLog mLog;
    private final PacketReader mPacketListener;
    /* access modifiers changed from: private */
    public boolean mRunning;
    /* access modifiers changed from: private */
    public final String mTag;

    public SemConnectivityPacketTracker(Handler h, InterfaceParams ifParams, LocalLog log) {
        if (ifParams != null) {
            this.mTag = "SemConnectivityPacketTracker." + ifParams.name;
            this.mLog = log;
            this.mPacketListener = new PacketListener(h, ifParams);
            return;
        }
        throw new IllegalArgumentException("null InterfaceParams");
    }

    public void start(String displayName) {
        this.mRunning = true;
        this.mDisplayName = displayName;
        this.mPacketListener.start();
    }

    public void stop() {
        this.mPacketListener.stop();
        this.mRunning = false;
        this.mDisplayName = null;
    }

    private final class PacketListener extends PacketReader {
        private final InterfaceParams mInterface;

        PacketListener(Handler h, InterfaceParams ifParams) {
            super(h, ifParams.defaultMtu);
            this.mInterface = ifParams;
        }

        /* access modifiers changed from: protected */
        public FileDescriptor createFd() {
            FileDescriptor s = null;
            try {
                s = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, 0);
                Os.bind(s, new PacketSocketAddress((short) OsConstants.ETH_P_ALL, this.mInterface.index));
                return s;
            } catch (ErrnoException | IOException e) {
                logError("Failed to create packet tracking socket: ", e);
                closeFd(s);
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void handlePacket(byte[] recvbuf, int length) {
            String summary = SemConnectivityPacketSummary.summarize(this.mInterface.macAddr, recvbuf, length);
            if (summary != null) {
                addLogEntry(summary + "\n[" + HexDump.toHexString(recvbuf, 0, length) + "]");
            }
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            String msg;
            if (TextUtils.isEmpty(SemConnectivityPacketTracker.this.mDisplayName)) {
                msg = SemConnectivityPacketTracker.MARK_START;
            } else {
                msg = String.format(SemConnectivityPacketTracker.MARK_NAMED_START, new Object[]{SemConnectivityPacketTracker.this.mDisplayName});
            }
            SemConnectivityPacketTracker.this.mLog.log(msg);
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            String msg;
            if (TextUtils.isEmpty(SemConnectivityPacketTracker.this.mDisplayName)) {
                msg = SemConnectivityPacketTracker.MARK_STOP;
            } else {
                msg = String.format(SemConnectivityPacketTracker.MARK_NAMED_STOP, new Object[]{SemConnectivityPacketTracker.this.mDisplayName});
            }
            if (!SemConnectivityPacketTracker.this.mRunning) {
                msg = msg + " (packet listener stopped unexpectedly)";
            }
            SemConnectivityPacketTracker.this.mLog.log(msg);
        }

        /* access modifiers changed from: protected */
        public void logError(String msg, Exception e) {
            Log.e(SemConnectivityPacketTracker.this.mTag, msg, e);
            addLogEntry(msg + e);
        }

        private void addLogEntry(String entry) {
            SemConnectivityPacketTracker.this.mLog.log(entry);
        }
    }
}
