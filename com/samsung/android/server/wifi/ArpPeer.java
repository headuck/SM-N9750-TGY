package com.samsung.android.server.wifi;

import android.net.LinkProperties;
import android.net.util.InterfaceParams;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.Log;
import com.samsung.android.server.wifi.bigdata.WifiChipInfo;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import libcore.io.IoBridge;

public class ArpPeer {
    private static final int ARP_LENGTH = 28;
    private static boolean A_DBG = true;
    private static final int ETHERNET_TYPE = 1;
    /* access modifiers changed from: private */
    public static final byte[] ETHER_ARP_TYPE = {8, 6};
    private static final int ETHER_HEADER_LENGTH = 14;
    private static final int IPV4_LENGTH = 4;
    private static final int MAC_ADDR_LENGTH = 6;
    private static final int MAX_LENGTH = 1500;
    private static final String TAG = "ArpPeer";
    /* access modifiers changed from: private */
    public byte[] L2_BROADCAST = {-1, -1, -1, -1, -1, -1};
    /* access modifiers changed from: private */
    public byte[] SRC_ADDR = new byte[0];
    private FileDescriptor mSocket;
    /* access modifiers changed from: private */
    public FileDescriptor mSocketGArp;
    private FileDescriptor mSocketGArpRecv;
    /* access modifiers changed from: private */
    public FileDescriptor mSocketRecv;

    /* access modifiers changed from: private */
    public void makeEthernet(ByteBuffer etherBuf, byte[] dstMAC, byte[] srcMAC, byte[] ethernetType) {
        if (etherBuf != null && dstMAC != null && srcMAC != null) {
            etherBuf.put(dstMAC);
            etherBuf.put(srcMAC);
            etherBuf.put(ethernetType);
            etherBuf.flip();
        }
    }

    private void makeARP(ByteBuffer buf, byte[] senderMAC, byte[] senderIP, byte[] targetIP) {
        if (buf != null && senderMAC != null && senderIP != null && targetIP != null) {
            buf.putShort(1);
            buf.putShort((short) OsConstants.ETH_P_IP);
            buf.put((byte) 6);
            buf.put((byte) 4);
            buf.putShort(1);
            buf.put(senderMAC);
            buf.put(senderIP);
            buf.put(new byte[6]);
            buf.put(targetIP);
            buf.flip();
        }
    }

    /* access modifiers changed from: private */
    public void makeGARP(ByteBuffer buf, byte[] senderMAC, byte[] senderIP) {
        if (buf != null && senderMAC != null && senderIP != null) {
            buf.putShort(1);
            buf.putShort((short) OsConstants.ETH_P_IP);
            buf.put((byte) 6);
            buf.put((byte) 4);
            buf.putShort(1);
            buf.put(senderMAC);
            buf.put(senderIP);
            buf.put(this.L2_BROADCAST);
            buf.put(senderIP);
            buf.flip();
        }
    }

    public void checkArpReply(final LinkProperties linkProperties, final int timeoutMillis, final byte[] gateway, byte[] myAddr, String myMac) {
        WifiChipInfo.getInstance().setArpResult(false);
        new Thread(new Runnable() {
            /* JADX WARNING: Code restructure failed: missing block: B:12:?, code lost:
                android.util.Log.e(com.samsung.android.server.wifi.ArpPeer.TAG, "Exception");
             */
            /* JADX WARNING: Code restructure failed: missing block: B:76:0x01d7, code lost:
                r0 = move-exception;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:78:?, code lost:
                android.util.Log.e(com.samsung.android.server.wifi.ArpPeer.TAG, "RuntimeException " + r0);
             */
            /* JADX WARNING: Code restructure failed: missing block: B:81:0x01f2, code lost:
                if (com.samsung.android.server.wifi.ArpPeer.access$000(r1.this$0) != null) goto L_0x01f4;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:83:?, code lost:
                libcore.io.IoBridge.closeAndSignalBlockedThreads(com.samsung.android.server.wifi.ArpPeer.access$000(r1.this$0));
             */
            /* JADX WARNING: Code restructure failed: missing block: B:84:0x01fe, code lost:
                r0 = move-exception;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:86:?, code lost:
                android.util.Log.e(com.samsung.android.server.wifi.ArpPeer.TAG, "IOException " + r0);
             */
            /* JADX WARNING: Code restructure failed: missing block: B:88:0x0218, code lost:
                r0 = e;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:89:0x0219, code lost:
                r2 = new java.lang.StringBuilder();
             */
            /* JADX WARNING: Failed to process nested try/catch */
            /* JADX WARNING: Removed duplicated region for block: B:76:0x01d7 A[ExcHandler: RuntimeException (r0v0 'e' java.lang.RuntimeException A[CUSTOM_DECLARE]), Splitter:B:1:0x0008] */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                    r17 = this;
                    r1 = r17
                    java.lang.String r2 = "IOException "
                    java.lang.String r3 = "Exception "
                    java.lang.String r4 = "ArpPeer"
                    android.net.LinkProperties r0 = r3     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r0 = r0.getInterfaceName()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r7 = r0
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r8 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r9 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r10 = 0
                    java.io.FileDescriptor r8 = android.system.Os.socket(r8, r9, r10)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.io.FileDescriptor unused = r0.mSocketRecv = r8     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.system.PacketSocketAddress r8 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r9 = android.system.OsConstants.ETH_P_ARP     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    short r9 = (short) r9     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r11 = r7.index     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r8.<init>(r9, r11)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.system.Os.bind(r0, r8)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    byte[] r0 = r5     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r8 = r0
                    long r11 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r0 = r4     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    long r13 = (long) r0     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    long r11 = r11 + r13
                    r0 = 1500(0x5dc, float:2.102E-42)
                    byte[] r0 = new byte[r0]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r9 = r0
                    r13 = 6
                    byte[] r0 = new byte[r13]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r14 = r0
                L_0x0048:
                    long r15 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r0 = (r15 > r11 ? 1 : (r15 == r11 ? 0 : -1))
                    if (r0 >= 0) goto L_0x012a
                    long r15 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    long r15 = r11 - r15
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r6 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    int r5 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.system.StructTimeval r13 = android.system.StructTimeval.fromMillis(r15)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.system.Os.setsockoptTimeval(r0, r6, r5, r13)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r5 = 0
                    java.lang.String r0 = "start to read recvSocket"
                    android.util.Log.d(r4, r0)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x007a, RuntimeException -> 0x01d7 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ Exception -> 0x007a, RuntimeException -> 0x01d7 }
                    int r6 = r9.length     // Catch:{ Exception -> 0x007a, RuntimeException -> 0x01d7 }
                    int r0 = android.system.Os.read(r0, r9, r10, r6)     // Catch:{ Exception -> 0x007a, RuntimeException -> 0x01d7 }
                    r5 = r0
                    goto L_0x0080
                L_0x007a:
                    r0 = move-exception
                    java.lang.String r6 = "Exception"
                    android.util.Log.e(r4, r6)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                L_0x0080:
                    java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r0.<init>()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r6 = "readLen:"
                    r0.append(r6)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r0.append(r5)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r0 = r0.toString()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.util.Log.d(r4, r0)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r0 = 28
                    if (r5 < r0) goto L_0x0126
                    r6 = 14
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r6 != 0) goto L_0x0126
                    r6 = 15
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r13 = 1
                    if (r6 != r13) goto L_0x0126
                    r6 = 16
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r13 = 8
                    if (r6 != r13) goto L_0x0126
                    r6 = 17
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r6 != 0) goto L_0x0126
                    r6 = 18
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r13 = 6
                    if (r6 != r13) goto L_0x0126
                    r6 = 19
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r13 = 4
                    if (r6 != r13) goto L_0x0126
                    r6 = 20
                    byte r6 = r9[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r6 != 0) goto L_0x0126
                    byte r0 = r9[r0]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    byte r6 = r8[r10]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r0 != r6) goto L_0x0126
                    r0 = 29
                    byte r0 = r9[r0]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r6 = 1
                    byte r13 = r8[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r0 != r13) goto L_0x0126
                    r0 = 30
                    byte r0 = r9[r0]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r6 = 2
                    byte r13 = r8[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r0 != r13) goto L_0x0126
                    r0 = 31
                    byte r0 = r9[r0]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r6 = 3
                    byte r6 = r8[r6]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    if (r0 != r6) goto L_0x0126
                    r0 = 22
                    r6 = 6
                    java.lang.System.arraycopy(r9, r0, r14, r10, r6)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r0 = com.samsung.android.server.wifi.ArpPeer.macToString(r14)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r13 = 21
                    byte r6 = r9[r13]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r10 = 1
                    if (r6 != r10) goto L_0x00ff
                    java.lang.String r6 = "ARP Request"
                    android.util.Log.d(r4, r6)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    goto L_0x0126
                L_0x00ff:
                    byte r6 = r9[r13]     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r10 = 2
                    if (r6 != r10) goto L_0x0126
                    com.samsung.android.server.wifi.bigdata.WifiChipInfo r6 = com.samsung.android.server.wifi.bigdata.WifiChipInfo.getInstance()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r10 = 1
                    r6.setArpResult(r10)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r6.<init>()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r10 = "ARP result("
                    r6.append(r10)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    r6.append(r0)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r10 = ")"
                    r6.append(r10)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    java.lang.String r6 = r6.toString()     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    android.util.Log.d(r4, r6)     // Catch:{ RuntimeException -> 0x01d7, Exception -> 0x0161 }
                    goto L_0x012a
                L_0x0126:
                    r10 = 0
                    r13 = 6
                    goto L_0x0048
                L_0x012a:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x0156 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ Exception -> 0x0156 }
                    if (r0 == 0) goto L_0x014f
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ IOException -> 0x013c }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ IOException -> 0x013c }
                    libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x013c }
                    goto L_0x014f
                L_0x013c:
                    r0 = move-exception
                    java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0156 }
                    r5.<init>()     // Catch:{ Exception -> 0x0156 }
                    r5.append(r2)     // Catch:{ Exception -> 0x0156 }
                    r5.append(r0)     // Catch:{ Exception -> 0x0156 }
                    java.lang.String r2 = r5.toString()     // Catch:{ Exception -> 0x0156 }
                    android.util.Log.e(r4, r2)     // Catch:{ Exception -> 0x0156 }
                L_0x014f:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x0156 }
                    r2 = 0
                    java.io.FileDescriptor unused = r0.mSocketRecv = r2     // Catch:{ Exception -> 0x0156 }
                    goto L_0x01c2
                L_0x0156:
                    r0 = move-exception
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder
                    r2.<init>()
                    goto L_0x01c9
                L_0x015d:
                    r0 = move-exception
                    r5 = r0
                    goto L_0x0220
                L_0x0161:
                    r0 = move-exception
                    java.lang.Throwable r5 = r0.getCause()     // Catch:{ all -> 0x015d }
                    boolean r6 = r5 instanceof android.system.ErrnoException     // Catch:{ all -> 0x015d }
                    if (r6 == 0) goto L_0x0197
                    r6 = r5
                    android.system.ErrnoException r6 = (android.system.ErrnoException) r6     // Catch:{ all -> 0x015d }
                    int r6 = r6.errno     // Catch:{ all -> 0x015d }
                    int r7 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x015d }
                    if (r6 == r7) goto L_0x0197
                    java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x015d }
                    r6.<init>()     // Catch:{ all -> 0x015d }
                    r6.append(r3)     // Catch:{ all -> 0x015d }
                    java.lang.Thread r7 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x015d }
                    java.lang.StackTraceElement[] r7 = r7.getStackTrace()     // Catch:{ all -> 0x015d }
                    r8 = 2
                    r7 = r7[r8]     // Catch:{ all -> 0x015d }
                    int r7 = r7.getLineNumber()     // Catch:{ all -> 0x015d }
                    r6.append(r7)     // Catch:{ all -> 0x015d }
                    r6.append(r0)     // Catch:{ all -> 0x015d }
                    java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x015d }
                    android.util.Log.e(r4, r6)     // Catch:{ all -> 0x015d }
                L_0x0197:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x01c3 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ Exception -> 0x01c3 }
                    if (r0 == 0) goto L_0x01bc
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ IOException -> 0x01a9 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ IOException -> 0x01a9 }
                    libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x01a9 }
                    goto L_0x01bc
                L_0x01a9:
                    r0 = move-exception
                    java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x01c3 }
                    r5.<init>()     // Catch:{ Exception -> 0x01c3 }
                    r5.append(r2)     // Catch:{ Exception -> 0x01c3 }
                    r5.append(r0)     // Catch:{ Exception -> 0x01c3 }
                    java.lang.String r2 = r5.toString()     // Catch:{ Exception -> 0x01c3 }
                    android.util.Log.e(r4, r2)     // Catch:{ Exception -> 0x01c3 }
                L_0x01bc:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x01c3 }
                    r2 = 0
                    java.io.FileDescriptor unused = r0.mSocketRecv = r2     // Catch:{ Exception -> 0x01c3 }
                L_0x01c2:
                    goto L_0x021f
                L_0x01c3:
                    r0 = move-exception
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder
                    r2.<init>()
                L_0x01c9:
                    r2.append(r3)
                    r2.append(r0)
                    java.lang.String r2 = r2.toString()
                    android.util.Log.e(r4, r2)
                    goto L_0x021f
                L_0x01d7:
                    r0 = move-exception
                    java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x015d }
                    r5.<init>()     // Catch:{ all -> 0x015d }
                    java.lang.String r6 = "RuntimeException "
                    r5.append(r6)     // Catch:{ all -> 0x015d }
                    r5.append(r0)     // Catch:{ all -> 0x015d }
                    java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x015d }
                    android.util.Log.e(r4, r5)     // Catch:{ all -> 0x015d }
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x0218 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ Exception -> 0x0218 }
                    if (r0 == 0) goto L_0x0211
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ IOException -> 0x01fe }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ IOException -> 0x01fe }
                    libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x01fe }
                    goto L_0x0211
                L_0x01fe:
                    r0 = move-exception
                    java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0218 }
                    r5.<init>()     // Catch:{ Exception -> 0x0218 }
                    r5.append(r2)     // Catch:{ Exception -> 0x0218 }
                    r5.append(r0)     // Catch:{ Exception -> 0x0218 }
                    java.lang.String r2 = r5.toString()     // Catch:{ Exception -> 0x0218 }
                    android.util.Log.e(r4, r2)     // Catch:{ Exception -> 0x0218 }
                L_0x0211:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x0218 }
                    r2 = 0
                    java.io.FileDescriptor unused = r0.mSocketRecv = r2     // Catch:{ Exception -> 0x0218 }
                    goto L_0x01c2
                L_0x0218:
                    r0 = move-exception
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder
                    r2.<init>()
                    goto L_0x01c9
                L_0x021f:
                    return
                L_0x0220:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x024c }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ Exception -> 0x024c }
                    if (r0 == 0) goto L_0x0245
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ IOException -> 0x0232 }
                    java.io.FileDescriptor r0 = r0.mSocketRecv     // Catch:{ IOException -> 0x0232 }
                    libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x0232 }
                    goto L_0x0245
                L_0x0232:
                    r0 = move-exception
                    java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x024c }
                    r6.<init>()     // Catch:{ Exception -> 0x024c }
                    r6.append(r2)     // Catch:{ Exception -> 0x024c }
                    r6.append(r0)     // Catch:{ Exception -> 0x024c }
                    java.lang.String r2 = r6.toString()     // Catch:{ Exception -> 0x024c }
                    android.util.Log.e(r4, r2)     // Catch:{ Exception -> 0x024c }
                L_0x0245:
                    com.samsung.android.server.wifi.ArpPeer r0 = com.samsung.android.server.wifi.ArpPeer.this     // Catch:{ Exception -> 0x024c }
                    r2 = 0
                    java.io.FileDescriptor unused = r0.mSocketRecv = r2     // Catch:{ Exception -> 0x024c }
                    goto L_0x025f
                L_0x024c:
                    r0 = move-exception
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder
                    r2.<init>()
                    r2.append(r3)
                    r2.append(r0)
                    java.lang.String r2 = r2.toString()
                    android.util.Log.e(r4, r2)
                L_0x025f:
                    throw r5
                */
                throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.ArpPeer.C07011.run():void");
            }
        }).start();
    }

    private byte[] getMacAddress(String interfaceName) {
        try {
            if (NetworkInterface.getNetworkInterfaces() == null) {
                Log.e(TAG, "NetworkInterface.getNetworkInterfaces() is null");
                return null;
            }
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (interfaceName == null || intf.getName().equalsIgnoreCase(interfaceName)) {
                    byte[] mac = intf.getHardwareAddress();
                    ByteBuffer concatBuf = ByteBuffer.allocate(6);
                    if (mac == null) {
                        Log.e(TAG, "Get hardware interface failed");
                        return null;
                    }
                    concatBuf.clear();
                    concatBuf.order(ByteOrder.BIG_ENDIAN);
                    for (int idx = 0; idx < mac.length; idx++) {
                        concatBuf.put(intToByteArray(Integer.valueOf(String.format("%02X", new Object[]{Byte.valueOf(mac[idx])}), 16).intValue())[3]);
                    }
                    concatBuf.flip();
                    return concatBuf.array();
                }
            }
            return null;
        } catch (Exception ex) {
            Log.e(TAG, "Exception " + ex);
        }
    }

    /* access modifiers changed from: private */
    public byte[] macStringToByteArray(String dstMac) {
        byte[] dstMAC = new byte[6];
        if (dstMac != null) {
            for (int i = 0; i < 6; i++) {
                dstMAC[i] = (byte) Integer.parseInt(dstMac.substring(i * 3, (i * 3) + 2), 16);
            }
        }
        return dstMAC;
    }

    private static Integer getInterfaceIndex(String ifname) {
        try {
            return Integer.valueOf(NetworkInterface.getByName(ifname).getIndex());
        } catch (NullPointerException | SocketException e) {
            return null;
        }
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
        if (mac == null) {
            return null;
        }
        int i = 0;
        while (i < mac.length) {
            try {
                String hexString = "0" + Integer.toHexString(mac[i]);
                macAddr = macAddr + hexString.substring(hexString.length() - 2);
                if (i != mac.length - 1) {
                    macAddr = macAddr + ":";
                }
                i++;
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "macAddressFromArpResult indexoutofboundsexception");
                return null;
            }
        }
        return macAddr;
    }

    public static byte[] intToByteArray(int value) {
        ByteBuffer converter = ByteBuffer.allocate(4);
        converter.order(ByteOrder.nativeOrder());
        converter.putInt(value);
        return converter.array();
    }

    public void sendGArp(final LinkProperties mLinkProperties, final byte[] myAddr, final String myMac) {
        new Thread(new Runnable() {
            public void run() {
                StringBuilder sb;
                try {
                    FileDescriptor unused = ArpPeer.this.mSocketGArp = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, 0);
                    InterfaceParams iParams = InterfaceParams.getByName(mLinkProperties.getInterfaceName());
                    Os.bind(ArpPeer.this.mSocketGArp, new PacketSocketAddress((short) OsConstants.ETH_P_IP, iParams.index));
                    byte[] macAddr = ArpPeer.this.macStringToByteArray(myMac);
                    byte[] unused2 = ArpPeer.this.SRC_ADDR = macAddr;
                    ByteBuffer sumBuf = ByteBuffer.allocate(1500);
                    ByteBuffer etherBuf = ByteBuffer.allocate(14);
                    ByteBuffer arpBuf = ByteBuffer.allocate(28);
                    sumBuf.clear();
                    sumBuf.order(ByteOrder.BIG_ENDIAN);
                    etherBuf.clear();
                    etherBuf.order(ByteOrder.BIG_ENDIAN);
                    arpBuf.clear();
                    arpBuf.order(ByteOrder.BIG_ENDIAN);
                    ArpPeer.this.makeEthernet(etherBuf, ArpPeer.this.L2_BROADCAST, ArpPeer.this.SRC_ADDR, ArpPeer.ETHER_ARP_TYPE);
                    ArpPeer.this.makeGARP(arpBuf, macAddr, myAddr);
                    sumBuf.put(etherBuf).put(arpBuf);
                    sumBuf.flip();
                    Os.sendto(ArpPeer.this.mSocketGArp, sumBuf.array(), 0, sumBuf.limit(), 0, new PacketSocketAddress(iParams.index, ArpPeer.this.L2_BROADCAST));
                    try {
                        if (ArpPeer.this.mSocketGArp != null) {
                            IoBridge.closeAndSignalBlockedThreads(ArpPeer.this.mSocketGArp);
                        }
                        FileDescriptor unused3 = ArpPeer.this.mSocketGArp = null;
                    } catch (IOException e) {
                        ex = e;
                        sb = new StringBuilder();
                        sb.append("IOException ");
                        sb.append(ex);
                        Log.e(ArpPeer.TAG, sb.toString());
                    }
                } catch (RuntimeException e2) {
                    Log.e(ArpPeer.TAG, "RuntimeException " + e2);
                    try {
                        if (ArpPeer.this.mSocketGArp != null) {
                            IoBridge.closeAndSignalBlockedThreads(ArpPeer.this.mSocketGArp);
                        }
                        FileDescriptor unused4 = ArpPeer.this.mSocketGArp = null;
                    } catch (IOException e3) {
                        ex = e3;
                        sb = new StringBuilder();
                        sb.append("IOException ");
                        sb.append(ex);
                        Log.e(ArpPeer.TAG, sb.toString());
                    }
                } catch (Exception e4) {
                    Throwable cause = e4.getCause();
                    if ((cause instanceof ErrnoException) && ((ErrnoException) cause).errno != OsConstants.EAGAIN) {
                        Log.e(ArpPeer.TAG, "Exception " + Thread.currentThread().getStackTrace()[2].getLineNumber() + e4);
                    }
                    try {
                        if (ArpPeer.this.mSocketGArp != null) {
                            IoBridge.closeAndSignalBlockedThreads(ArpPeer.this.mSocketGArp);
                        }
                        FileDescriptor unused5 = ArpPeer.this.mSocketGArp = null;
                    } catch (IOException e5) {
                        ex = e5;
                        sb = new StringBuilder();
                        sb.append("IOException ");
                        sb.append(ex);
                        Log.e(ArpPeer.TAG, sb.toString());
                    }
                } catch (Throwable th) {
                    try {
                        if (ArpPeer.this.mSocketGArp != null) {
                            IoBridge.closeAndSignalBlockedThreads(ArpPeer.this.mSocketGArp);
                        }
                        FileDescriptor unused6 = ArpPeer.this.mSocketGArp = null;
                    } catch (IOException ex) {
                        Log.e(ArpPeer.TAG, "IOException " + ex);
                    }
                    throw th;
                }
            }
        }).start();
    }
}
