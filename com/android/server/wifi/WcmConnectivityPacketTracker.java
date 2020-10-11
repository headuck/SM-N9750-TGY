package com.android.server.wifi;

import android.net.NetworkUtils;
import android.net.util.InterfaceParams;
import android.os.Debug;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.util.HexDump;
import com.samsung.android.server.wifi.softap.PacketReader;
import com.samsung.android.server.wifi.softap.SemConnectivityPacketSummary;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WcmConnectivityPacketTracker {
    /* access modifiers changed from: private */
    public static final boolean DBG = Debug.semIsProductDev();
    private static final String MARK_NAMED_START = "--- START (%s) ---";
    private static final String MARK_NAMED_STOP = "--- STOP (%s) ---";
    private static final String MARK_START = "--- START ---";
    private static final String MARK_STOP = "--- STOP ---";
    private static final String TAG = "WcmConnectivityPacketTracker";
    /* access modifiers changed from: private */
    public String mDisplayName;
    /* access modifiers changed from: private */
    public final LocalLog mLog;
    private final PacketReader mPacketListener;
    /* access modifiers changed from: private */
    public boolean mRunning;
    /* access modifiers changed from: private */
    public final String mTag;

    public WcmConnectivityPacketTracker(Handler h, InterfaceParams ifParams, LocalLog log) {
        if (ifParams != null) {
            this.mTag = "WcmConnectivityPacketTracker." + ifParams.name;
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
                NetworkUtils.attachControlPacketFilter(s, OsConstants.ARPHRD_ETHER);
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
            String direction;
            if (WcmConnectivityPacketTracker.DBG) {
                String summary = SemConnectivityPacketSummary.summarize(this.mInterface.macAddr, recvbuf, length);
                if (summary != null) {
                    if (WcmConnectivityPacketTracker.DBG) {
                        Log.d(WcmConnectivityPacketTracker.this.mTag, summary);
                    }
                    addLogEntry(summary + "\n[" + HexDump.toHexString(recvbuf, 0, length) + "]");
                    return;
                }
                return;
            }
            ByteBuffer mPacket = ByteBuffer.wrap(recvbuf, 0, length);
            if (mPacket.remaining() < 14) {
                direction = "*";
            } else if (this.mInterface.macAddr != null) {
                mPacket.position(6);
                direction = ByteBuffer.wrap(this.mInterface.macAddr.toByteArray()).equals((ByteBuffer) mPacket.slice().limit(6)) ? ">" : "<";
            } else {
                direction = "*";
            }
            addLogEntry(direction + " [" + HexDump.toHexString(recvbuf, 0, length) + "]");
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            String msg;
            if (TextUtils.isEmpty(WcmConnectivityPacketTracker.this.mDisplayName)) {
                msg = WcmConnectivityPacketTracker.MARK_START;
            } else {
                msg = String.format(WcmConnectivityPacketTracker.MARK_NAMED_START, new Object[]{WcmConnectivityPacketTracker.this.mDisplayName});
            }
            WcmConnectivityPacketTracker.this.mLog.log(msg);
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            String msg;
            if (TextUtils.isEmpty(WcmConnectivityPacketTracker.this.mDisplayName)) {
                msg = WcmConnectivityPacketTracker.MARK_STOP;
            } else {
                msg = String.format(WcmConnectivityPacketTracker.MARK_NAMED_STOP, new Object[]{WcmConnectivityPacketTracker.this.mDisplayName});
            }
            if (!WcmConnectivityPacketTracker.this.mRunning) {
                msg = msg + " (packet listener stopped unexpectedly)";
            }
            WcmConnectivityPacketTracker.this.mLog.log(msg);
        }

        /* access modifiers changed from: protected */
        public void logError(String msg, Exception e) {
            Log.e(WcmConnectivityPacketTracker.this.mTag, msg, e);
            addLogEntry(msg + e);
        }

        private void addLogEntry(String entry) {
            WcmConnectivityPacketTracker.this.mLog.log(entry);
        }
    }
}
