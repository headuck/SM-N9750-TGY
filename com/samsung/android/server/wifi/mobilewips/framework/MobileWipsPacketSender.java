package com.samsung.android.server.wifi.mobilewips.framework;

import android.net.LinkProperties;
import android.net.TrafficStats;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.samsung.android.server.wifi.softap.DhcpPacket;
import com.samsung.android.server.wifi.softap.smarttethering.SemWifiApSmartUtil;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import libcore.io.IoBridge;

public class MobileWipsPacketSender {
    private static final int ARP_LENGTH = 28;
    private static final byte[] BOOTP_FLAGS = {Byte.MIN_VALUE, 0};
    private static final byte[] CI_YI_SI_AI_ADDR = {0, 0, 0, 0};
    private static final int CODE = 0;
    private static final byte[] DATA = {105, 46, 9, 0, 0, 0, 0, 0, SemWifiApSmartUtil.BLE_BATT_2, 17, 18, 19, 20, 21, 22, 23, SemWifiApSmartUtil.BLE_BATT_3, 25, 26, 27, 28, 29, 30, 31, SemWifiApSmartUtil.BLE_BATT_4, 33, 34, 35, 36, 37, 38, 39, SemWifiApSmartUtil.BLE_BATT_5, 41, 42, 43, 44, 45, 46, 47, SemWifiApSmartUtil.BLE_BATT_6, 49, 50, 51, 52, 53, 54, 55};
    private static final int DHCP_OPTION_START = 282;
    private static final int DNS_DPORT = 53;
    private static final int DNS_IPV4_MSG_TYPE_LOCATION = 42;
    private static final int DPORT = 67;
    private static final int ETHERNET_TYPE = 1;
    private static final byte[] ETHER_ARP_TYPE = {8, 6};
    private static final int ETHER_HEADER_LENGTH = 14;
    private static final byte[] ETHER_IP_TYPE = {8, 0};
    private static final int ETH_IPV4_MAC_SRC_LOCATION = 6;
    private static final byte[] FLAGS_FRAGMENT_OFFSET = {SemWifiApSmartUtil.BLE_WIFI, 0};
    private static final int HOPS = 0;
    private static final int HW_ADDR_LENGTH = 6;
    private static final int HW_TYPE = 1;
    private static final int ICMP_CHECKSUM = 0;
    private static final int ICMP_HEADER_LENGTH = 64;
    private static final int ICMP_REPLY_TTL_LOCATION = 22;
    private static final byte[] IDENTIFICATION = {-77, -40};
    private static final byte[] IDENTIFIER = {88, 6};
    private static final int IDENTIFIER_LOCATION = 38;
    private static final int IPV4_LENGTH = 4;
    private static final int IP_CHECKSUM = 0;
    private static final int IP_HEADER_LENGTH = 20;
    private static final int JAVA_IP_TTL = 25;
    private static final int MAC_ADDR_LENGTH = 6;
    private static final byte[] MAGIC_COOKIE = {99, -126, 83, 99};
    private static final int MAX_LENGTH = 1500;
    private static final int MSG_TYPE = 1;
    private static final int MSG_TYPE_LOCATION = 42;
    private static final int MSG_TYPE_OFFER = 2;
    private static final int MSG_TYPE_REQUEST = 1;
    private static final int OPTION_DISCOVER = 53;
    private static final int OPTION_DISCOVER_DHCP = 1;
    private static final int OPTION_DISCOVER_LENGTH = 1;
    private static final int OPTION_END = 255;
    private static final int OPTION_ROUTER = 3;
    private static final int OPTION_VENDOR = 43;
    private static final int PROTOCOL = 1;
    private static final int SECS = 0;
    private static final int SEQUENCE_LOCATION = 40;
    private static final byte[] SEQUENCE_NUMBER = {0, 2};
    private static final int SPORT = 68;
    private static final String TAG = "MobileWips::FrameworkPktSender";
    private static final byte[] TCP_ACK_NUMBER = {0, 0, 0, 100};
    private static final int TCP_CHECKSUM = 0;
    private static final int TCP_DPORT = 80;
    private static final int TCP_DPORT_DNS = 53;
    private static final int TCP_HEADER_LENGTH = 20;
    private static final byte[] TCP_HEADER_LENGTH_FLAGS = {80, 2};
    private static final int TCP_PROTOCOL = 6;
    private static final byte[] TCP_SEQ_NUMBER = {0, 0, 0, 100};
    private static final int TCP_SPORT = 65000;
    private static final int TCP_TOTAL_LENGTH = 40;
    private static final int TCP_WINDOW_SIZE = 4000;
    private static final byte[] TIMESTAMP = {-66, -29, 119, 90, 0, 0, 0, 0};
    private static final int TOTAL_LENGTH = 84;
    private static final byte[] TRANSACTION_ID = {-122, 22, 6, 2};
    private static final int TRANSACTION_ID_LOCATION = 46;
    private static final int TTL = 64;
    private static final int TYPE = 8;
    private static final int ToS = 0;
    private static final int UDP_CHECKSUM = 0;
    private static final int UDP_IPV4_DST_PORT_LOCATION = 36;
    private static final int UDP_IPV4_SRC_PORT_LOCATION = 34;
    private static final byte[] UDP_IP_DST_ADDR = {-1, -1, -1, -1};
    private static final byte[] UDP_IP_SRC_ADDR = {0, 0, 0, 0};
    private static final int UDP_LENGTH = 252;
    private static final int UDP_PROTOCOL = 17;
    private static final int UDP_TOTAL_LENGTH = 272;
    private static final int VERSION_HEADER_LENGTH = 69;
    private byte[] L2_BROADCAST = {-1, -1, -1, -1, -1, -1};
    private byte[] SRC_ADDR = new byte[0];
    private FileDescriptor mSocket;
    private FileDescriptor mSocketArpSniff;
    private FileDescriptor mSocketArpSniffRecv;
    private FileDescriptor mSocketDhcp;
    private FileDescriptor mSocketIcmp;
    private FileDescriptor mSocketRecv;

    private void makeEthernet(ByteBuffer etherBuf, byte[] dstMAC, byte[] srcMAC, byte[] ethernetType) {
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

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    private void makeIP(ByteBuffer ipBuf, int totalLength, int nextProtocol, byte[] srcIP, byte[] dstIP) {
        if (ipBuf != null && srcIP != null && dstIP != null) {
            ByteBuffer tmpBuf = ByteBuffer.allocate(2);
            tmpBuf.clear();
            tmpBuf.order(ByteOrder.BIG_ENDIAN);
            ipBuf.put((byte) 69);
            ipBuf.put((byte) 0);
            ipBuf.putShort((short) totalLength);
            ipBuf.put(IDENTIFICATION);
            ipBuf.put(FLAGS_FRAGMENT_OFFSET);
            ipBuf.put(SemWifiApSmartUtil.BLE_WIFI);
            ipBuf.put((byte) nextProtocol);
            ipBuf.putShort(0);
            ipBuf.put(srcIP);
            ipBuf.put(dstIP);
            ipBuf.flip();
            byte[] resIpChecksum = MobileWipsFrameworkUtil.longToBytes(calculationChecksum(ipBuf.array()));
            tmpBuf.put(resIpChecksum[6]);
            tmpBuf.put(resIpChecksum[7]);
            ipBuf.putShort(10, tmpBuf.getShort(0));
        }
    }

    private void makeUDP(ByteBuffer udpBuf, int sourcePort, int destinationPort, int lenght, int checkSum) {
        if (udpBuf != null) {
            udpBuf.putShort((short) sourcePort);
            udpBuf.putShort((short) destinationPort);
            udpBuf.putShort((short) lenght);
            udpBuf.putShort((short) checkSum);
            udpBuf.flip();
        }
    }

    private void makeTCP(ByteBuffer tcpBuf, ByteBuffer pseudoBuf, int tcpDestinationPort) {
        if (tcpBuf != null && pseudoBuf != null) {
            ByteBuffer tmpBuf = ByteBuffer.allocate(2);
            tmpBuf.clear();
            tmpBuf.order(ByteOrder.BIG_ENDIAN);
            tcpBuf.putShort(-536);
            tcpBuf.putShort((short) tcpDestinationPort);
            tcpBuf.put(TCP_SEQ_NUMBER);
            tcpBuf.put(TCP_ACK_NUMBER);
            tcpBuf.put(TCP_HEADER_LENGTH_FLAGS);
            tcpBuf.putShort(4000);
            tcpBuf.putShort(0);
            tcpBuf.putShort(0);
            tcpBuf.flip();
            pseudoBuf.put(tcpBuf);
            pseudoBuf.flip();
            byte[] resTcpChecksum = MobileWipsFrameworkUtil.longToBytes(calculationChecksum(pseudoBuf.array()));
            tmpBuf.put(resTcpChecksum[6]);
            tmpBuf.put(resTcpChecksum[7]);
            tcpBuf.putShort(16, tmpBuf.getShort(0));
            tcpBuf.flip();
        }
    }

    private void makePsuedoHeader(ByteBuffer pseudoBuf, byte[] srcIP, byte[] dstIP, int protocol, int protoHeaderLength) {
        if (pseudoBuf != null && srcIP != null && dstIP != null) {
            pseudoBuf.put(srcIP);
            pseudoBuf.put(dstIP);
            pseudoBuf.put((byte) 0);
            pseudoBuf.put((byte) protocol);
            pseudoBuf.putShort((short) protoHeaderLength);
        }
    }

    private void makeICMP(ByteBuffer icmpBuf) {
        if (icmpBuf != null) {
            ByteBuffer tmpBuf = ByteBuffer.allocate(2);
            tmpBuf.clear();
            tmpBuf.order(ByteOrder.BIG_ENDIAN);
            icmpBuf.put((byte) 8);
            icmpBuf.put((byte) 0);
            icmpBuf.putShort(0);
            icmpBuf.put(IDENTIFIER);
            icmpBuf.put(SEQUENCE_NUMBER);
            icmpBuf.put(TIMESTAMP);
            icmpBuf.put(DATA);
            icmpBuf.flip();
            byte[] resIcmpChecksum = MobileWipsFrameworkUtil.longToBytes(calculationChecksum(icmpBuf.array()));
            tmpBuf.put(resIcmpChecksum[6]);
            tmpBuf.put(resIcmpChecksum[7]);
            icmpBuf.putShort(2, tmpBuf.getShort(0));
        }
    }

    private void makeDHCP(ByteBuffer dhcpBuf, byte[] srcMAC) {
        if (dhcpBuf != null && srcMAC != null) {
            dhcpBuf.put((byte) 1);
            dhcpBuf.put((byte) 1);
            dhcpBuf.put((byte) 6);
            dhcpBuf.put((byte) 0);
            dhcpBuf.put(TRANSACTION_ID);
            dhcpBuf.putShort(0);
            dhcpBuf.put(BOOTP_FLAGS);
            dhcpBuf.put(CI_YI_SI_AI_ADDR);
            dhcpBuf.put(CI_YI_SI_AI_ADDR);
            dhcpBuf.put(CI_YI_SI_AI_ADDR);
            dhcpBuf.put(CI_YI_SI_AI_ADDR);
            dhcpBuf.put(srcMAC);
            dhcpBuf.position(DhcpPacket.MIN_PACKET_LENGTH_BOOTP);
            dhcpBuf.put(MAGIC_COOKIE);
            dhcpBuf.put((byte) 53);
            dhcpBuf.put((byte) 1);
            dhcpBuf.put((byte) 1);
            dhcpBuf.put((byte) -1);
            dhcpBuf.flip();
        }
    }

    private byte[] macStringToByteArray(String dstMac) {
        byte[] dstMAC = new byte[6];
        if (dstMac != null) {
            for (int i = 0; i < 6; i++) {
                dstMAC[i] = (byte) Integer.parseInt(dstMac.substring(i * 3, (i * 3) + 2), 16);
            }
        }
        return dstMAC;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:105:0x02cc, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r1.mSocket);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:108:0x02d8, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r1.mSocketRecv);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0225, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x0227, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0244, code lost:
        android.util.Log.e(TAG, "Exception " + java.lang.Thread.currentThread().getStackTrace()[2].getLineNumber() + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x026b, code lost:
        r7 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x0270, code lost:
        if (r7 < r5.size()) goto L_0x0272;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x0272, code lost:
        android.util.Log.d(TAG, "ARP result(" + r7 + ") = " + r5.get(r7));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0290, code lost:
        r7 = r7 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x0297, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r1.mSocket);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x02a3, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r1.mSocketRecv);
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x02cc A[Catch:{ IOException -> 0x02ac }] */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x02d8 A[Catch:{ IOException -> 0x02ac }] */
    /* JADX WARNING: Removed duplicated region for block: B:114:0x02e6 A[Catch:{ IOException -> 0x02fb }] */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x02f2 A[Catch:{ IOException -> 0x02fb }] */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x0227 A[ExcHandler: RuntimeException (e java.lang.RuntimeException), Splitter:B:4:0x00a6] */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0244 A[Catch:{ all -> 0x02ae }] */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x026b A[Catch:{ all -> 0x02ae }] */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0297 A[Catch:{ IOException -> 0x02ac }] */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x02a3 A[Catch:{ IOException -> 0x02ac }] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:77:0x0233=Splitter:B:77:0x0233, B:100:0x02b4=Splitter:B:100:0x02b4} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<java.lang.String> sendArp(android.net.LinkProperties r29, int r30, byte[] r31, byte[] r32, java.lang.String r33) {
        /*
            r28 = this;
            r1 = r28
            java.lang.String r2 = ") = "
            java.lang.String r3 = "ARP result("
            java.lang.String r4 = "MobileWips::FrameworkPktSender"
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r5 = r0
            r8 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r9 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r9, r8)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r1.mSocket = r0     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.lang.String r0 = r29.getInterfaceName()     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r9 = r0
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            android.system.PacketSocketAddress r10 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r11 = android.system.OsConstants.ETH_P_IP     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            short r11 = (short) r11     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r12 = r9.index     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r10.<init>(r11, r12)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            android.system.Os.bind(r0, r10)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r10 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r10, r8)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r1.mSocketRecv = r0     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            android.system.PacketSocketAddress r10 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r11 = android.system.OsConstants.ETH_P_ARP     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            short r11 = (short) r11     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            int r12 = r9.index     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r10.<init>(r11, r12)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            android.system.Os.bind(r0, r10)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.lang.String r0 = "wlan0"
            byte[] r0 = r1.getMacAddress(r0)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r1.SRC_ADDR = r0     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r10 = r31
            long r11 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r13 = r30
            long r14 = (long) r13     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            long r11 = r11 + r14
            r14 = r33
            byte[] r0 = r1.macStringToByteArray(r14)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r15 = r0
            r0 = 1500(0x5dc, float:2.102E-42)
            java.nio.ByteBuffer r16 = java.nio.ByteBuffer.allocate(r0)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r17 = r16
            r16 = 14
            java.nio.ByteBuffer r18 = java.nio.ByteBuffer.allocate(r16)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r19 = r18
            r7 = 28
            java.nio.ByteBuffer r20 = java.nio.ByteBuffer.allocate(r7)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r21 = r20
            r17.clear()     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.nio.ByteOrder r6 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r7 = r17
            r7.order(r6)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r19.clear()     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.nio.ByteOrder r6 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r8 = r19
            r8.order(r6)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r21.clear()     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            java.nio.ByteOrder r6 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r13 = r21
            r13.order(r6)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            byte[] r6 = r1.L2_BROADCAST     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            byte[] r0 = r1.SRC_ADDR     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            byte[] r14 = ETHER_ARP_TYPE     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r1.makeEthernet(r8, r6, r0, r14)     // Catch:{ RuntimeException -> 0x02b1, Exception -> 0x0230, all -> 0x022a }
            r6 = r31
            r14 = r32
            r1.makeARP(r13, r15, r14, r6)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.nio.ByteBuffer r0 = r7.put(r8)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r0.put(r13)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.flip()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            android.system.PacketSocketAddress r0 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            int r6 = r9.index     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r21 = r8
            byte[] r8 = r1.L2_BROADCAST     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r0.<init>(r6, r8)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r27 = r0
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            byte[] r23 = r7.array()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r24 = 0
            int r25 = r7.limit()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r26 = 0
            r22 = r0
            android.system.Os.sendto(r22, r23, r24, r25, r26, r27)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r0 = 1500(0x5dc, float:2.102E-42)
            byte[] r0 = new byte[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r6 = r0
            r8 = 6
            byte[] r0 = new byte[r8]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r19 = r0
        L_0x00dd:
            long r22 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            int r0 = (r22 > r11 ? 1 : (r22 == r11 ? 0 : -1))
            if (r0 >= 0) goto L_0x01dc
            long r22 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            long r22 = r11 - r22
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            int r8 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r25 = r7
            int r7 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r26 = r9
            android.system.StructTimeval r9 = android.system.StructTimeval.fromMillis(r22)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            android.system.Os.setsockoptTimeval(r0, r8, r7, r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7 = 0
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ Exception -> 0x0107, RuntimeException -> 0x0227 }
            int r8 = r6.length     // Catch:{ Exception -> 0x0107, RuntimeException -> 0x0227 }
            r9 = 0
            int r0 = android.system.Os.read(r0, r6, r9, r8)     // Catch:{ Exception -> 0x0107, RuntimeException -> 0x0227 }
            r7 = r0
            goto L_0x0108
        L_0x0107:
            r0 = move-exception
        L_0x0108:
            r8 = 28
            if (r7 < r8) goto L_0x01cf
            byte r0 = r6[r16]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != 0) goto L_0x01cf
            r0 = 15
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r8 = 1
            if (r0 != r8) goto L_0x01cf
            r0 = 16
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 8
            if (r0 != r9) goto L_0x01cf
            r0 = 17
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != 0) goto L_0x01cf
            r0 = 18
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 6
            if (r0 != r9) goto L_0x01cf
            r0 = 19
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 4
            if (r0 != r9) goto L_0x01cf
            r0 = 20
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != 0) goto L_0x01cf
            r9 = 28
            byte r0 = r6[r9]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r17 = 0
            byte r9 = r10[r17]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != r9) goto L_0x01cf
            r0 = 29
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            byte r9 = r10[r8]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != r9) goto L_0x01cf
            r0 = 30
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 2
            byte r8 = r10[r9]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != r8) goto L_0x01cf
            r0 = 31
            byte r0 = r6[r0]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r8 = 3
            byte r8 = r10[r8]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 != r8) goto L_0x01cf
            r0 = 22
            r8 = r19
            r9 = 6
            r19 = r7
            r7 = 0
            java.lang.System.arraycopy(r6, r0, r8, r7, r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.macToString(r8)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7 = 1
            r17 = 0
            r24 = r17
            r9 = r24
        L_0x0173:
            r24 = r7
            int r7 = r5.size()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r9 >= r7) goto L_0x018e
            java.lang.Object r7 = r5.get(r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            boolean r7 = r7.contains(r0)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r7 == 0) goto L_0x0189
            r7 = 0
            goto L_0x0190
        L_0x0189:
            int r9 = r9 + 1
            r7 = r24
            goto L_0x0173
        L_0x018e:
            r7 = r24
        L_0x0190:
            if (r7 == 0) goto L_0x01cc
            r9 = 21
            r24 = r7
            byte r7 = r6[r9]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 1
            if (r7 != r9) goto L_0x01b0
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.<init>()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r9 = "REQ"
            r7.append(r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r0)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r7 = r7.toString()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r5.add(r7)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            goto L_0x01d3
        L_0x01b0:
            r7 = 21
            byte r7 = r6[r7]     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r9 = 2
            if (r7 != r9) goto L_0x01d3
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.<init>()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r9 = "REP"
            r7.append(r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r0)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r7 = r7.toString()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r5.add(r7)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            goto L_0x01d3
        L_0x01cc:
            r24 = r7
            goto L_0x01d3
        L_0x01cf:
            r8 = r19
            r19 = r7
        L_0x01d3:
            r19 = r8
            r7 = r25
            r9 = r26
            r8 = 6
            goto L_0x00dd
        L_0x01dc:
            r25 = r7
            r26 = r9
            r8 = r19
            r7 = 0
            r0 = r7
        L_0x01e4:
            int r7 = r5.size()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            if (r0 >= r7) goto L_0x020b
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.<init>()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r3)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r0)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r2)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.Object r9 = r5.get(r0)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r9 = (java.lang.String) r9     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            r7.append(r9)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            java.lang.String r7 = r7.toString()     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            android.util.Log.d(r4, r7)     // Catch:{ RuntimeException -> 0x0227, Exception -> 0x0225 }
            int r0 = r0 + 1
            goto L_0x01e4
        L_0x020b:
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x0214
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x0214:
            r2 = 0
            r1.mSocket = r2     // Catch:{ IOException -> 0x02ac }
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x0220
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x0220:
            r2 = 0
            r1.mSocketRecv = r2     // Catch:{ IOException -> 0x02ac }
            goto L_0x02ab
        L_0x0225:
            r0 = move-exception
            goto L_0x0233
        L_0x0227:
            r0 = move-exception
            goto L_0x02b4
        L_0x022a:
            r0 = move-exception
            r14 = r32
        L_0x022d:
            r2 = r0
            goto L_0x02e2
        L_0x0230:
            r0 = move-exception
            r14 = r32
        L_0x0233:
            java.lang.Throwable r6 = r0.getCause()     // Catch:{ all -> 0x02ae }
            boolean r7 = r6 instanceof android.system.ErrnoException     // Catch:{ all -> 0x02ae }
            if (r7 == 0) goto L_0x026b
            r7 = r6
            android.system.ErrnoException r7 = (android.system.ErrnoException) r7     // Catch:{ all -> 0x02ae }
            int r7 = r7.errno     // Catch:{ all -> 0x02ae }
            int r8 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x02ae }
            if (r7 == r8) goto L_0x026b
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x02ae }
            r2.<init>()     // Catch:{ all -> 0x02ae }
            java.lang.String r3 = "Exception "
            r2.append(r3)     // Catch:{ all -> 0x02ae }
            java.lang.Thread r3 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x02ae }
            java.lang.StackTraceElement[] r3 = r3.getStackTrace()     // Catch:{ all -> 0x02ae }
            r7 = 2
            r3 = r3[r7]     // Catch:{ all -> 0x02ae }
            int r3 = r3.getLineNumber()     // Catch:{ all -> 0x02ae }
            r2.append(r3)     // Catch:{ all -> 0x02ae }
            r2.append(r0)     // Catch:{ all -> 0x02ae }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x02ae }
            android.util.Log.e(r4, r2)     // Catch:{ all -> 0x02ae }
            goto L_0x0293
        L_0x026b:
            r7 = 0
        L_0x026c:
            int r8 = r5.size()     // Catch:{ all -> 0x02ae }
            if (r7 >= r8) goto L_0x0293
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ all -> 0x02ae }
            r8.<init>()     // Catch:{ all -> 0x02ae }
            r8.append(r3)     // Catch:{ all -> 0x02ae }
            r8.append(r7)     // Catch:{ all -> 0x02ae }
            r8.append(r2)     // Catch:{ all -> 0x02ae }
            java.lang.Object r9 = r5.get(r7)     // Catch:{ all -> 0x02ae }
            java.lang.String r9 = (java.lang.String) r9     // Catch:{ all -> 0x02ae }
            r8.append(r9)     // Catch:{ all -> 0x02ae }
            java.lang.String r8 = r8.toString()     // Catch:{ all -> 0x02ae }
            android.util.Log.d(r4, r8)     // Catch:{ all -> 0x02ae }
            int r7 = r7 + 1
            goto L_0x026c
        L_0x0293:
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x029c
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x029c:
            r2 = 0
            r1.mSocket = r2     // Catch:{ IOException -> 0x02ac }
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x02a8
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x02a8:
            r2 = 0
            r1.mSocketRecv = r2     // Catch:{ IOException -> 0x02ac }
        L_0x02ab:
            goto L_0x02e1
        L_0x02ac:
            r0 = move-exception
            goto L_0x02e1
        L_0x02ae:
            r0 = move-exception
            goto L_0x022d
        L_0x02b1:
            r0 = move-exception
            r14 = r32
        L_0x02b4:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x02ae }
            r2.<init>()     // Catch:{ all -> 0x02ae }
            java.lang.String r3 = "RuntimeException "
            r2.append(r3)     // Catch:{ all -> 0x02ae }
            r2.append(r0)     // Catch:{ all -> 0x02ae }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x02ae }
            android.util.Log.e(r4, r2)     // Catch:{ all -> 0x02ae }
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x02d1
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x02d1:
            r2 = 0
            r1.mSocket = r2     // Catch:{ IOException -> 0x02ac }
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            if (r0 == 0) goto L_0x02dd
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02ac }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ac }
        L_0x02dd:
            r2 = 0
            r1.mSocketRecv = r2     // Catch:{ IOException -> 0x02ac }
            goto L_0x02ab
        L_0x02e1:
            return r5
        L_0x02e2:
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02fb }
            if (r0 == 0) goto L_0x02eb
            java.io.FileDescriptor r0 = r1.mSocket     // Catch:{ IOException -> 0x02fb }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02fb }
        L_0x02eb:
            r3 = 0
            r1.mSocket = r3     // Catch:{ IOException -> 0x02fb }
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02fb }
            if (r0 == 0) goto L_0x02f7
            java.io.FileDescriptor r0 = r1.mSocketRecv     // Catch:{ IOException -> 0x02fb }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02fb }
        L_0x02f7:
            r3 = 0
            r1.mSocketRecv = r3     // Catch:{ IOException -> 0x02fb }
            goto L_0x02fc
        L_0x02fb:
            r0 = move-exception
        L_0x02fc:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendArp(android.net.LinkProperties, int, byte[], byte[], java.lang.String):java.util.List");
    }

    /* Debug info: failed to restart local var, previous not found, register: 24 */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x01a2, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x01a5, code lost:
        r0 = e;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x02b2 A[Catch:{ IOException -> 0x028e }] */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x02be A[Catch:{ IOException -> 0x028e }] */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x02cf A[Catch:{ IOException -> 0x02e4 }] */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x02db A[Catch:{ IOException -> 0x02e4 }] */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x01a5 A[ExcHandler: RuntimeException (e java.lang.RuntimeException), Splitter:B:13:0x00d6] */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x021e A[Catch:{ all -> 0x0290 }] */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0247 A[Catch:{ all -> 0x0290 }] */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x0279 A[Catch:{ IOException -> 0x028e }] */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x0285 A[Catch:{ IOException -> 0x028e }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized java.util.List<java.lang.String> sendArpToSniffing(android.net.LinkProperties r25, int r26, byte[] r27, byte[] r28, java.lang.String r29) {
        /*
            r24 = this;
            r1 = r24
            monitor-enter(r24)
            java.util.ArrayList r0 = new java.util.ArrayList     // Catch:{ all -> 0x02e6 }
            r0.<init>()     // Catch:{ all -> 0x02e6 }
            r2 = r0
            r4 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r6 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r6, r4)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r1.mSocketArpSniff = r0     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            java.lang.String r0 = r25.getInterfaceName()     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r6 = r0
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            android.system.PacketSocketAddress r7 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r8 = android.system.OsConstants.ETH_P_IP     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            short r8 = (short) r8     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r9 = r6.index     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r7.<init>(r8, r9)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            android.system.Os.bind(r0, r7)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r7 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r7, r4)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r1.mSocketArpSniffRecv = r0     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            android.system.PacketSocketAddress r7 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r8 = android.system.OsConstants.ETH_P_ARP     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            short r8 = (short) r8     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            int r9 = r6.index     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r7.<init>(r8, r9)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            android.system.Os.bind(r0, r7)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            java.lang.String r0 = "wlan0"
            byte[] r0 = r1.getMacAddress(r0)     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r1.SRC_ADDR = r0     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r7 = r27
            long r8 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x0293, Exception -> 0x0208, all -> 0x0200 }
            r10 = r26
            long r11 = (long) r10
            long r8 = r8 + r11
            r11 = r29
            byte[] r0 = r1.macStringToByteArray(r11)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r12 = r0
            r0 = 1500(0x5dc, float:2.102E-42)
            java.nio.ByteBuffer r13 = java.nio.ByteBuffer.allocate(r0)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r14 = 14
            java.nio.ByteBuffer r15 = java.nio.ByteBuffer.allocate(r14)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r5 = 28
            java.nio.ByteBuffer r16 = java.nio.ByteBuffer.allocate(r5)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r17 = r16
            r13.clear()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r13.order(r3)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r15.clear()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r15.order(r3)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r17.clear()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r14 = r17
            r14.order(r3)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            byte[] r3 = r1.L2_BROADCAST     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            byte[] r5 = r1.SRC_ADDR     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            byte[] r4 = ETHER_ARP_TYPE     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r1.makeEthernet(r15, r3, r5, r4)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r3 = r27
            r4 = r28
            r1.makeARP(r14, r12, r4, r3)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            java.nio.ByteBuffer r5 = r13.put(r15)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r5.put(r14)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r13.flip()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            android.system.PacketSocketAddress r5 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            int r0 = r6.index     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            byte[] r3 = r1.L2_BROADCAST     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r5.<init>(r0, r3)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r23 = r5
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            byte[] r19 = r13.array()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r20 = 0
            int r21 = r13.limit()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r22 = 0
            r18 = r0
            android.system.Os.sendto(r18, r19, r20, r21, r22, r23)     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r0 = 1500(0x5dc, float:2.102E-42)
            byte[] r0 = new byte[r0]     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r3 = r0
            r5 = 6
            byte[] r0 = new byte[r5]     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            r18 = r0
        L_0x00ce:
            long r19 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            int r0 = (r19 > r8 ? 1 : (r19 == r8 ? 0 : -1))
            if (r0 >= 0) goto L_0x01a8
            long r19 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            long r19 = r8 - r19
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            int r5 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            int r4 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r22 = r6
            android.system.StructTimeval r6 = android.system.StructTimeval.fromMillis(r19)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            android.system.Os.setsockoptTimeval(r0, r5, r4, r6)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4 = 0
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ Exception -> 0x00f6, RuntimeException -> 0x01a5 }
            int r5 = r3.length     // Catch:{ Exception -> 0x00f6, RuntimeException -> 0x01a5 }
            r6 = 0
            int r0 = android.system.Os.read(r0, r3, r6, r5)     // Catch:{ Exception -> 0x00f6, RuntimeException -> 0x01a5 }
            r4 = r0
            goto L_0x00f7
        L_0x00f6:
            r0 = move-exception
        L_0x00f7:
            r5 = 28
            if (r4 < r5) goto L_0x0195
            r5 = 14
            byte r0 = r3[r5]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != 0) goto L_0x0195
            r0 = 15
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r6 = 1
            if (r0 != r6) goto L_0x0195
            r0 = 16
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r5 = 8
            if (r0 != r5) goto L_0x0195
            r0 = 17
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != 0) goto L_0x0195
            r0 = 18
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r5 = 6
            if (r0 != r5) goto L_0x0195
            r0 = 19
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r5 = 4
            if (r0 != r5) goto L_0x0195
            r0 = 20
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != 0) goto L_0x0195
            r5 = 28
            byte r0 = r3[r5]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r17 = 0
            byte r5 = r7[r17]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != r5) goto L_0x0195
            r0 = 29
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            byte r5 = r7[r6]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != r5) goto L_0x0195
            r0 = 30
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r5 = 2
            byte r6 = r7[r5]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != r6) goto L_0x0195
            r0 = 31
            byte r0 = r3[r0]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r5 = 3
            byte r5 = r7[r5]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            if (r0 != r5) goto L_0x0195
            r0 = 22
            r5 = r18
            r6 = 6
            r18 = r4
            r4 = 0
            java.lang.System.arraycopy(r3, r0, r5, r4, r6)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.macToString(r5)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4 = 21
            byte r6 = r3[r4]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4 = 1
            if (r6 != r4) goto L_0x0179
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r6 = "REQ"
            r4.append(r6)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4.append(r0)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r2.add(r4)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            goto L_0x0199
        L_0x0179:
            r4 = 21
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r6 = 2
            if (r4 != r6) goto L_0x0199
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r6 = "REP"
            r4.append(r6)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r4.append(r0)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r2.add(r4)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            goto L_0x0199
        L_0x0195:
            r5 = r18
            r18 = r4
        L_0x0199:
            r4 = r28
            r18 = r5
            r6 = r22
            r5 = 6
            goto L_0x00ce
        L_0x01a2:
            r0 = move-exception
            goto L_0x020d
        L_0x01a5:
            r0 = move-exception
            goto L_0x0298
        L_0x01a8:
            r22 = r6
            r5 = r18
            r4 = 0
            r0 = r4
        L_0x01ae:
            int r4 = r2.size()     // Catch:{ RuntimeException -> 0x01fd, Exception -> 0x01fb }
            if (r0 >= r4) goto L_0x01df
            java.lang.String r4 = "MobileWips::FrameworkPktSender"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r6.<init>()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r17 = r3
            java.lang.String r3 = "ARP result("
            r6.append(r3)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r6.append(r0)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r3 = ") = "
            r6.append(r3)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.Object r3 = r2.get(r0)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r3 = (java.lang.String) r3     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            r6.append(r3)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            java.lang.String r3 = r6.toString()     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            android.util.Log.d(r4, r3)     // Catch:{ RuntimeException -> 0x01a5, Exception -> 0x01a2 }
            int r0 = r0 + 1
            r3 = r17
            goto L_0x01ae
        L_0x01df:
            r17 = r3
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x01ea
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x01ea:
            r3 = 0
            r1.mSocketArpSniff = r3     // Catch:{ IOException -> 0x028e }
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x01f6
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x01f6:
            r3 = 0
            r1.mSocketArpSniffRecv = r3     // Catch:{ IOException -> 0x028e }
            goto L_0x028d
        L_0x01fb:
            r0 = move-exception
            goto L_0x020d
        L_0x01fd:
            r0 = move-exception
            goto L_0x0298
        L_0x0200:
            r0 = move-exception
            r10 = r26
            r11 = r29
            r3 = r0
            goto L_0x02cb
        L_0x0208:
            r0 = move-exception
            r10 = r26
            r11 = r29
        L_0x020d:
            java.lang.Throwable r3 = r0.getCause()     // Catch:{ all -> 0x0290 }
            boolean r4 = r3 instanceof android.system.ErrnoException     // Catch:{ all -> 0x0290 }
            if (r4 == 0) goto L_0x0247
            r4 = r3
            android.system.ErrnoException r4 = (android.system.ErrnoException) r4     // Catch:{ all -> 0x0290 }
            int r4 = r4.errno     // Catch:{ all -> 0x0290 }
            int r5 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x0290 }
            if (r4 == r5) goto L_0x0247
            java.lang.String r4 = "MobileWips::FrameworkPktSender"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ all -> 0x0290 }
            r5.<init>()     // Catch:{ all -> 0x0290 }
            java.lang.String r6 = "Exception "
            r5.append(r6)     // Catch:{ all -> 0x0290 }
            java.lang.Thread r6 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x0290 }
            java.lang.StackTraceElement[] r6 = r6.getStackTrace()     // Catch:{ all -> 0x0290 }
            r7 = 2
            r6 = r6[r7]     // Catch:{ all -> 0x0290 }
            int r6 = r6.getLineNumber()     // Catch:{ all -> 0x0290 }
            r5.append(r6)     // Catch:{ all -> 0x0290 }
            r5.append(r0)     // Catch:{ all -> 0x0290 }
            java.lang.String r5 = r5.toString()     // Catch:{ all -> 0x0290 }
            android.util.Log.e(r4, r5)     // Catch:{ all -> 0x0290 }
            goto L_0x0275
        L_0x0247:
            r4 = 0
        L_0x0248:
            int r5 = r2.size()     // Catch:{ all -> 0x0290 }
            if (r4 >= r5) goto L_0x0275
            java.lang.String r5 = "MobileWips::FrameworkPktSender"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ all -> 0x0290 }
            r6.<init>()     // Catch:{ all -> 0x0290 }
            java.lang.String r7 = "ARP result("
            r6.append(r7)     // Catch:{ all -> 0x0290 }
            r6.append(r4)     // Catch:{ all -> 0x0290 }
            java.lang.String r7 = ") = "
            r6.append(r7)     // Catch:{ all -> 0x0290 }
            java.lang.Object r7 = r2.get(r4)     // Catch:{ all -> 0x0290 }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ all -> 0x0290 }
            r6.append(r7)     // Catch:{ all -> 0x0290 }
            java.lang.String r6 = r6.toString()     // Catch:{ all -> 0x0290 }
            android.util.Log.d(r5, r6)     // Catch:{ all -> 0x0290 }
            int r4 = r4 + 1
            goto L_0x0248
        L_0x0275:
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x027e
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x027e:
            r3 = 0
            r1.mSocketArpSniff = r3     // Catch:{ IOException -> 0x028e }
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x028a
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x028a:
            r3 = 0
            r1.mSocketArpSniffRecv = r3     // Catch:{ IOException -> 0x028e }
        L_0x028d:
            goto L_0x02c7
        L_0x028e:
            r0 = move-exception
            goto L_0x02c7
        L_0x0290:
            r0 = move-exception
            r3 = r0
            goto L_0x02cb
        L_0x0293:
            r0 = move-exception
            r10 = r26
            r11 = r29
        L_0x0298:
            java.lang.String r3 = "MobileWips::FrameworkPktSender"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x02c9 }
            r4.<init>()     // Catch:{ all -> 0x02c9 }
            java.lang.String r5 = "RuntimeException "
            r4.append(r5)     // Catch:{ all -> 0x02c9 }
            r4.append(r0)     // Catch:{ all -> 0x02c9 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x02c9 }
            android.util.Log.e(r3, r4)     // Catch:{ all -> 0x02c9 }
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x02b7
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x02b7:
            r3 = 0
            r1.mSocketArpSniff = r3     // Catch:{ IOException -> 0x028e }
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            if (r0 == 0) goto L_0x02c3
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x028e }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x028e }
        L_0x02c3:
            r3 = 0
            r1.mSocketArpSniffRecv = r3     // Catch:{ IOException -> 0x028e }
            goto L_0x028d
        L_0x02c7:
            monitor-exit(r24)
            return r2
        L_0x02c9:
            r0 = move-exception
            r3 = r0
        L_0x02cb:
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x02e4 }
            if (r0 == 0) goto L_0x02d4
            java.io.FileDescriptor r0 = r1.mSocketArpSniff     // Catch:{ IOException -> 0x02e4 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02e4 }
        L_0x02d4:
            r4 = 0
            r1.mSocketArpSniff = r4     // Catch:{ IOException -> 0x02e4 }
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x02e4 }
            if (r0 == 0) goto L_0x02e0
            java.io.FileDescriptor r0 = r1.mSocketArpSniffRecv     // Catch:{ IOException -> 0x02e4 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02e4 }
        L_0x02e0:
            r4 = 0
            r1.mSocketArpSniffRecv = r4     // Catch:{ IOException -> 0x02e4 }
            goto L_0x02e5
        L_0x02e4:
            r0 = move-exception
        L_0x02e5:
            throw r3     // Catch:{ all -> 0x02e6 }
        L_0x02e6:
            r0 = move-exception
            monitor-exit(r24)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendArpToSniffing(android.net.LinkProperties, int, byte[], byte[], java.lang.String):java.util.List");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x019c, code lost:
        r11.add(r5);
        r11.add(java.lang.String.valueOf(r3[22] & 255));
        r4 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x01b1, code lost:
        r6 = r17;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:?, code lost:
        r4.append(r6);
        r17 = r1;
        r4.append(r11.get(0));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x01c4, code lost:
        r1 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r4.append(r1);
        r16 = r2;
        r4.append(r11.get(1));
        android.util.Log.d(TAG, r4.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x01e1, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x01e2, code lost:
        r1 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0224, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0225, code lost:
        r1 = r16;
        r6 = r17;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x022e, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x022f, code lost:
        r1 = " - ";
        r6 = "ICMP echo reply src : ";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0243, code lost:
        android.util.Log.e(TAG, "Exception " + java.lang.Thread.currentThread().getStackTrace()[2].getLineNumber() + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x026d, code lost:
        if (r11.size() == 2) goto L_0x026f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x026f, code lost:
        android.util.Log.d(TAG, r6 + r11.get(0) + r1 + r11.get(1));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x0299, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r7.mSocketIcmp);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x02a4, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:?, code lost:
        android.util.Log.e(TAG, "RuntimeException " + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x02bb, code lost:
        if (r7.mSocketIcmp != null) goto L_0x02bd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x02bd, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r7.mSocketIcmp);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x02c2, code lost:
        r7.mSocketIcmp = null;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0243 A[Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df, all -> 0x022a }] */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0269 A[Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df, all -> 0x022a }] */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x0299 A[Catch:{ IOException -> 0x02a2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x02a4 A[EDGE_INSN: B:26:?->B:61:0x02a4 ?: BREAK  , ExcHandler: RuntimeException (r0v1 'e' java.lang.RuntimeException A[CUSTOM_DECLARE]), Splitter:B:1:0x000f] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:55:0x0295=Splitter:B:55:0x0295, B:35:0x0216=Splitter:B:35:0x0216} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<java.lang.String> sendIcmp(android.net.LinkProperties r29, int r30, byte[] r31, byte[] r32, java.lang.String r33) {
        /*
            r28 = this;
            r7 = r28
            java.lang.String r8 = " - "
            java.lang.String r9 = "ICMP echo reply src : "
            java.lang.String r10 = "MobileWips::FrameworkPktSender"
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r11 = r0
            r14 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            int r1 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r1, r14)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r7.mSocketIcmp = r0     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.lang.String r0 = r29.getInterfaceName()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.io.FileDescriptor r1 = r7.mSocketIcmp     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            android.system.PacketSocketAddress r2 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            int r3 = android.system.OsConstants.ETH_P_IP     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            short r3 = (short) r3     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            int r4 = r0.index     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r2.<init>(r3, r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            android.system.Os.bind(r1, r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r5 = r32
            r6 = r31
            java.lang.String r1 = "wlan0"
            byte[] r1 = r7.getMacAddress(r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r7.SRC_ADDR = r1     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r15 = r33
            byte[] r1 = r7.macStringToByteArray(r15)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r4 = r1
            long r1 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r3 = r30
            long r12 = (long) r3     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            long r12 = r12 + r1
            r2 = 1500(0x5dc, float:2.102E-42)
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r16 = 14
            java.nio.ByteBuffer r16 = java.nio.ByteBuffer.allocate(r16)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r17 = r16
            r16 = 20
            java.nio.ByteBuffer r16 = java.nio.ByteBuffer.allocate(r16)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r18 = r16
            r16 = 64
            java.nio.ByteBuffer r16 = java.nio.ByteBuffer.allocate(r16)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r19 = r16
            r1.clear()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.nio.ByteOrder r2 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r1.order(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r17.clear()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.nio.ByteOrder r2 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r14 = r17
            r14.order(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r18.clear()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.nio.ByteOrder r2 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r3 = r18
            r3.order(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r19.clear()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            java.nio.ByteOrder r2 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r15 = r19
            r15.order(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            byte[] r2 = r7.SRC_ADDR     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r17 = r1
            byte[] r1 = ETHER_IP_TYPE     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r7.makeEthernet(r14, r4, r2, r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x022e }
            r18 = 84
            r19 = 1
            r2 = r17
            r1 = r28
            r20 = r2
            r2 = r3
            r16 = r8
            r8 = r3
            r3 = r18
            r17 = r9
            r9 = r4
            r4 = r19
            r1.makeIP(r2, r3, r4, r5, r6)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r7.makeICMP(r15)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r1 = r20
            java.nio.ByteBuffer r2 = r1.put(r14)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.nio.ByteBuffer r2 = r2.put(r8)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r2.put(r15)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r1.flip()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r2 = r1.array()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r3.<init>()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r4 = "ICMP : "
            r3.append(r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r3.append(r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r3 = r3.toString()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            android.util.Log.d(r10, r3)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            android.system.PacketSocketAddress r3 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            int r4 = r0.index     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r3.<init>(r4, r9)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r27 = r3
            java.io.FileDescriptor r3 = r7.mSocketIcmp     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r23 = r1.array()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r24 = 0
            int r25 = r1.limit()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r26 = 0
            r22 = r3
            android.system.Os.sendto(r22, r23, r24, r25, r26, r27)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r3 = 1500(0x5dc, float:2.102E-42)
            byte[] r3 = new byte[r3]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4 = 6
            r18 = r0
            byte[] r0 = new byte[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r20 = r1
            byte[] r1 = new byte[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
        L_0x0108:
            long r21 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            int r19 = (r21 > r12 ? 1 : (r21 == r12 ? 0 : -1))
            if (r19 >= 0) goto L_0x020e
            long r21 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            long r21 = r12 - r21
            java.io.FileDescriptor r4 = r7.mSocketIcmp     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r23 = r2
            int r2 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r24 = r5
            int r5 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r25 = r6
            android.system.StructTimeval r6 = android.system.StructTimeval.fromMillis(r21)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            android.system.Os.setsockoptTimeval(r4, r2, r5, r6)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.io.FileDescriptor r2 = r7.mSocketIcmp     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            int r4 = r3.length     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r5 = 0
            int r2 = android.system.Os.read(r2, r3, r5, r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4 = 38
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r5 = IDENTIFIER     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r6 = 0
            byte r5 = r5[r6]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r4 != r5) goto L_0x01f7
            r4 = 39
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r5 = IDENTIFIER     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r6 = 1
            byte r5 = r5[r6]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r4 != r5) goto L_0x01f7
            r4 = 40
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r5 = SEQUENCE_NUMBER     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r6 = 0
            byte r5 = r5[r6]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r4 != r5) goto L_0x01f7
            r4 = 41
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r5 = SEQUENCE_NUMBER     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r6 = 1
            byte r5 = r5[r6]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r4 != r5) goto L_0x01f7
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r5 = "ICMP Recv (length: "
            r4.append(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r5 = java.lang.String.valueOf(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4.append(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r5 = ") : "
            r4.append(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r5 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r3)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4.append(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r5 = " "
            r4.append(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.String r4 = r4.toString()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            android.util.Log.d(r10, r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4 = 6
            r5 = 0
            java.lang.System.arraycopy(r3, r5, r1, r5, r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.System.arraycopy(r3, r4, r0, r5, r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            byte[] r5 = r7.SRC_ADDR     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            boolean r5 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.compareByteArray(r5, r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r5 == 0) goto L_0x01ee
            java.lang.String r5 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.macToString(r0)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            if (r5 == 0) goto L_0x01e5
            r11.add(r5)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4 = 22
            byte r4 = r3[r4]     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4 = r4 & 255(0xff, float:3.57E-43)
            java.lang.String r4 = java.lang.String.valueOf(r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r11.add(r4)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r4.<init>()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x0224 }
            r6 = r17
            r4.append(r6)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01e1 }
            r17 = r1
            r1 = 0
            java.lang.Object r19 = r11.get(r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01e1 }
            r1 = r19
            java.lang.String r1 = (java.lang.String) r1     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01e1 }
            r4.append(r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01e1 }
            r1 = r16
            r4.append(r1)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            r16 = r2
            r2 = 1
            java.lang.Object r19 = r11.get(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            r2 = r19
            java.lang.String r2 = (java.lang.String) r2     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            r4.append(r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            java.lang.String r2 = r4.toString()     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            android.util.Log.d(r10, r2)     // Catch:{ RuntimeException -> 0x02a4, Exception -> 0x01df }
            goto L_0x0216
        L_0x01df:
            r0 = move-exception
            goto L_0x0231
        L_0x01e1:
            r0 = move-exception
            r1 = r16
            goto L_0x0231
        L_0x01e5:
            r6 = r17
            r17 = r1
            r1 = r16
            r16 = r2
            goto L_0x0200
        L_0x01ee:
            r6 = r17
            r17 = r1
            r1 = r16
            r16 = r2
            goto L_0x0200
        L_0x01f7:
            r6 = r17
            r4 = 6
            r17 = r1
            r1 = r16
            r16 = r2
        L_0x0200:
            r16 = r1
            r1 = r17
            r2 = r23
            r5 = r24
            r17 = r6
            r6 = r25
            goto L_0x0108
        L_0x020e:
            r17 = r1
            r23 = r2
            r24 = r5
            r25 = r6
        L_0x0216:
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            if (r0 == 0) goto L_0x021f
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02a2 }
        L_0x021f:
            r1 = 0
            r7.mSocketIcmp = r1     // Catch:{ IOException -> 0x02a2 }
            goto L_0x02a1
        L_0x0224:
            r0 = move-exception
            r1 = r16
            r6 = r17
            goto L_0x0231
        L_0x022a:
            r0 = move-exception
            r1 = r0
            goto L_0x02c7
        L_0x022e:
            r0 = move-exception
            r1 = r8
            r6 = r9
        L_0x0231:
            java.lang.Throwable r2 = r0.getCause()     // Catch:{ all -> 0x022a }
            boolean r3 = r2 instanceof android.system.ErrnoException     // Catch:{ all -> 0x022a }
            r4 = 2
            if (r3 == 0) goto L_0x0269
            r3 = r2
            android.system.ErrnoException r3 = (android.system.ErrnoException) r3     // Catch:{ all -> 0x022a }
            int r3 = r3.errno     // Catch:{ all -> 0x022a }
            int r5 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x022a }
            if (r3 == r5) goto L_0x0269
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x022a }
            r1.<init>()     // Catch:{ all -> 0x022a }
            java.lang.String r3 = "Exception "
            r1.append(r3)     // Catch:{ all -> 0x022a }
            java.lang.Thread r3 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x022a }
            java.lang.StackTraceElement[] r3 = r3.getStackTrace()     // Catch:{ all -> 0x022a }
            r3 = r3[r4]     // Catch:{ all -> 0x022a }
            int r3 = r3.getLineNumber()     // Catch:{ all -> 0x022a }
            r1.append(r3)     // Catch:{ all -> 0x022a }
            r1.append(r0)     // Catch:{ all -> 0x022a }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x022a }
            android.util.Log.e(r10, r1)     // Catch:{ all -> 0x022a }
            goto L_0x0295
        L_0x0269:
            int r3 = r11.size()     // Catch:{ all -> 0x022a }
            if (r3 != r4) goto L_0x0295
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x022a }
            r3.<init>()     // Catch:{ all -> 0x022a }
            r3.append(r6)     // Catch:{ all -> 0x022a }
            r4 = 0
            java.lang.Object r4 = r11.get(r4)     // Catch:{ all -> 0x022a }
            java.lang.String r4 = (java.lang.String) r4     // Catch:{ all -> 0x022a }
            r3.append(r4)     // Catch:{ all -> 0x022a }
            r3.append(r1)     // Catch:{ all -> 0x022a }
            r1 = 1
            java.lang.Object r1 = r11.get(r1)     // Catch:{ all -> 0x022a }
            java.lang.String r1 = (java.lang.String) r1     // Catch:{ all -> 0x022a }
            r3.append(r1)     // Catch:{ all -> 0x022a }
            java.lang.String r1 = r3.toString()     // Catch:{ all -> 0x022a }
            android.util.Log.d(r10, r1)     // Catch:{ all -> 0x022a }
        L_0x0295:
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            if (r0 == 0) goto L_0x029e
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02a2 }
        L_0x029e:
            r1 = 0
            r7.mSocketIcmp = r1     // Catch:{ IOException -> 0x02a2 }
        L_0x02a1:
            goto L_0x02c6
        L_0x02a2:
            r0 = move-exception
            goto L_0x02c6
        L_0x02a4:
            r0 = move-exception
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x022a }
            r1.<init>()     // Catch:{ all -> 0x022a }
            java.lang.String r2 = "RuntimeException "
            r1.append(r2)     // Catch:{ all -> 0x022a }
            r1.append(r0)     // Catch:{ all -> 0x022a }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x022a }
            android.util.Log.e(r10, r1)     // Catch:{ all -> 0x022a }
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            if (r0 == 0) goto L_0x02c2
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02a2 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02a2 }
        L_0x02c2:
            r1 = 0
            r7.mSocketIcmp = r1     // Catch:{ IOException -> 0x02a2 }
            goto L_0x02a1
        L_0x02c6:
            return r11
        L_0x02c7:
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02d4 }
            if (r0 == 0) goto L_0x02d0
            java.io.FileDescriptor r0 = r7.mSocketIcmp     // Catch:{ IOException -> 0x02d4 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02d4 }
        L_0x02d0:
            r2 = 0
            r7.mSocketIcmp = r2     // Catch:{ IOException -> 0x02d4 }
            goto L_0x02d5
        L_0x02d4:
            r0 = move-exception
        L_0x02d5:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendIcmp(android.net.LinkProperties, int, byte[], byte[], java.lang.String):java.util.List");
    }

    /* JADX WARNING: Removed duplicated region for block: B:103:0x0321 A[Catch:{ IOException -> 0x02ff }] */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x032f A[Catch:{ IOException -> 0x0338 }] */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02f6 A[Catch:{ IOException -> 0x02ff }] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:98:0x0309=Splitter:B:98:0x0309, B:82:0x02bb=Splitter:B:82:0x02bb} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int sendDhcp(android.net.LinkProperties r35, int r36, byte[] r37, int r38, java.lang.String r39) {
        /*
            r34 = this;
            r7 = r34
            r8 = r38
            r9 = r39
            java.lang.String r10 = "MobileWips::FrameworkPktSender"
            r11 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            int r1 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r14 = 0
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r1, r14)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r7.mSocketDhcp = r0     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.lang.String r0 = r35.getInterfaceName()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.io.FileDescriptor r1 = r7.mSocketDhcp     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            android.system.PacketSocketAddress r2 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            int r3 = android.system.OsConstants.ETH_P_IP     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            short r3 = (short) r3     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            int r4 = r0.index     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r2.<init>(r3, r4)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            android.system.Os.bind(r1, r2)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.lang.String r1 = "wlan0"
            byte[] r1 = r7.getMacAddress(r1)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r7.SRC_ADDR = r1     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r15 = r37
            r1 = 100
            byte[] r1 = new byte[r1]     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r6 = -1
            if (r8 == r6) goto L_0x004d
            if (r9 == 0) goto L_0x004d
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch:{ RuntimeException -> 0x004a, Exception -> 0x0047 }
            byte[] r2 = r9.getBytes(r2)     // Catch:{ RuntimeException -> 0x004a, Exception -> 0x0047 }
            r1 = r2
            r5 = r1
            goto L_0x004e
        L_0x0047:
            r0 = move-exception
            goto L_0x02bb
        L_0x004a:
            r0 = move-exception
            goto L_0x0309
        L_0x004d:
            r5 = r1
        L_0x004e:
            long r1 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r4 = r36
            long r12 = (long) r4     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            long r12 = r12 + r1
            r3 = 1500(0x5dc, float:2.102E-42)
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r2 = r1
            r1 = 14
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r1)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r17 = 20
            java.nio.ByteBuffer r17 = java.nio.ByteBuffer.allocate(r17)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r18 = r17
            r17 = 8
            java.nio.ByteBuffer r17 = java.nio.ByteBuffer.allocate(r17)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r19 = r17
            r17 = 245(0xf5, float:3.43E-43)
            java.nio.ByteBuffer r17 = java.nio.ByteBuffer.allocate(r17)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r20 = r17
            r2.clear()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r2.order(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r1.clear()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r1.order(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r18.clear()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r14 = r18
            r14.order(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r19.clear()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r4 = r19
            r4.order(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r20.clear()     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            java.nio.ByteOrder r3 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r18 = r4
            r4 = r20
            r4.order(r3)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            byte[] r3 = r7.L2_BROADCAST     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            byte[] r6 = r7.SRC_ADDR     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r20 = r2
            byte[] r2 = ETHER_IP_TYPE     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r7.makeEthernet(r1, r3, r6, r2)     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r3 = 272(0x110, float:3.81E-43)
            r6 = 17
            byte[] r21 = UDP_IP_SRC_ADDR     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            byte[] r22 = UDP_IP_DST_ADDR     // Catch:{ RuntimeException -> 0x0306, Exception -> 0x02b8, all -> 0x02b2 }
            r2 = r1
            r1 = r34
            r24 = r2
            r23 = r20
            r2 = r14
            r20 = r11
            r11 = 1500(0x5dc, float:2.102E-42)
            r11 = r4
            r4 = r6
            r6 = r5
            r5 = r21
            r19 = r6
            r21 = r15
            r15 = -1
            r6 = r22
            r1.makeIP(r2, r3, r4, r5, r6)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r3 = 68
            r4 = 67
            r5 = 252(0xfc, float:3.53E-43)
            r6 = 0
            r1 = r34
            r2 = r18
            r1.makeUDP(r2, r3, r4, r5, r6)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r1 = r7.SRC_ADDR     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r7.makeDHCP(r11, r1)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1 = r23
            r2 = r24
            java.nio.ByteBuffer r3 = r1.put(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.nio.ByteBuffer r3 = r3.put(r14)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r4 = r18
            java.nio.ByteBuffer r3 = r3.put(r4)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r3.put(r11)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.flip()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r3 = r1.array()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r5.<init>()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r6 = "DHCP : "
            r5.append(r6)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r6 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r3)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r5.append(r6)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r5 = r5.toString()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            android.util.Log.d(r10, r5)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            android.system.PacketSocketAddress r5 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            int r6 = r0.index     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r15 = r7.L2_BROADCAST     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r5.<init>(r6, r15)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r27 = r5
            java.io.FileDescriptor r5 = r7.mSocketDhcp     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r23 = r1.array()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r24 = 0
            int r25 = r1.limit()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r26 = 0
            r22 = r5
            android.system.Os.sendto(r22, r23, r24, r25, r26, r27)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r5 = 1500(0x5dc, float:2.102E-42)
            byte[] r5 = new byte[r5]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 4
            byte[] r15 = new byte[r6]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.net.InetSocketAddress r33 = new java.net.InetSocketAddress     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r33.<init>()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
        L_0x014a:
            long r22 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            int r17 = (r22 > r12 ? 1 : (r22 == r12 ? 0 : -1))
            if (r17 >= 0) goto L_0x026e
            long r22 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            long r22 = r12 - r22
            java.io.FileDescriptor r6 = r7.mSocketDhcp     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r24 = r0
            int r0 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r25 = r1
            int r1 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r26 = r2
            android.system.StructTimeval r2 = android.system.StructTimeval.fromMillis(r22)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            android.system.Os.setsockoptTimeval(r6, r0, r1, r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r30 = 0
            int r1 = r5.length     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r32 = 0
            r28 = r0
            r29 = r5
            r31 = r1
            int r0 = android.system.Os.recvfrom(r28, r29, r30, r31, r32, r33)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1 = 46
            byte r1 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r2 = TRANSACTION_ID     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 0
            byte r2 = r2[r6]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 != r2) goto L_0x025b
            r1 = 47
            byte r1 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r2 = TRANSACTION_ID     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 1
            byte r2 = r2[r6]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 != r2) goto L_0x025b
            r1 = 48
            byte r1 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r2 = TRANSACTION_ID     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r16 = 2
            byte r2 = r2[r16]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 != r2) goto L_0x025b
            r1 = 49
            byte r1 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            byte[] r2 = TRANSACTION_ID     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 3
            byte r2 = r2[r6]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 != r2) goto L_0x025b
            r1 = 42
            byte r1 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r2 = 2
            if (r1 != r2) goto L_0x025b
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.<init>()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = "DHCP Recv (length: "
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = java.lang.String.valueOf(r0)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = ") : "
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r5)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = " "
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r1 = r1.toString()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            android.util.Log.d(r10, r1)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1 = 282(0x11a, float:3.95E-43)
        L_0x01db:
            int r2 = r5.length     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 >= r2) goto L_0x0254
            byte r2 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 255(0xff, float:3.57E-43)
            r2 = r2 & r6
            if (r2 == r6) goto L_0x024d
            byte r2 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r2 != 0) goto L_0x01ef
            r29 = r3
            r0 = r19
            goto L_0x0261
        L_0x01ef:
            byte r2 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r6 = 3
            if (r2 != r6) goto L_0x020c
            r2 = -1
            if (r8 != r2) goto L_0x020c
            int r2 = r1 + 2
            r17 = r0
            r0 = 0
            r6 = 4
            java.lang.System.arraycopy(r5, r2, r15, r0, r6)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.ipToInt(r15)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r20 = r0
            r29 = r3
            r0 = r19
            goto L_0x0261
        L_0x020c:
            r17 = r0
            r0 = 4
            byte r2 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r2 != r8) goto L_0x0237
            if (r9 == 0) goto L_0x0237
            r2 = 0
            r6 = r2
        L_0x0217:
            r0 = r19
            int r2 = r0.length     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r6 >= r2) goto L_0x0231
            int r2 = r1 + 2
            int r2 = r2 + r6
            byte r2 = r5[r2]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r29 = r3
            byte r3 = r0[r6]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r2 == r3) goto L_0x0228
            goto L_0x0233
        L_0x0228:
            int r6 = r6 + 1
            r19 = r0
            r3 = r29
            r0 = 4
            r2 = 0
            goto L_0x0217
        L_0x0231:
            r29 = r3
        L_0x0233:
            r2 = 1
            r20 = r2
            goto L_0x0261
        L_0x0237:
            r29 = r3
            r0 = r19
            int r1 = r1 + 1
            int r2 = r5.length     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            if (r1 < r2) goto L_0x0241
            goto L_0x0261
        L_0x0241:
            byte r2 = r5[r1]     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            int r1 = r1 + r2
            r2 = 1
            int r1 = r1 + r2
            r19 = r0
            r0 = r17
            r3 = r29
            goto L_0x01db
        L_0x024d:
            r17 = r0
            r29 = r3
            r0 = r19
            goto L_0x0261
        L_0x0254:
            r17 = r0
            r29 = r3
            r0 = r19
            goto L_0x0261
        L_0x025b:
            r17 = r0
            r29 = r3
            r0 = r19
        L_0x0261:
            r19 = r0
            r0 = r24
            r1 = r25
            r2 = r26
            r3 = r29
            r6 = 4
            goto L_0x014a
        L_0x026e:
            r24 = r0
            r25 = r1
            r26 = r2
            r29 = r3
            r0 = r19
            r1 = -1
            if (r8 != r1) goto L_0x0293
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.<init>()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = "Router IP in DHCP Offer : "
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r2 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.ipToString(r15)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            r1.append(r2)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            java.lang.String r1 = r1.toString()     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
            android.util.Log.d(r10, r1)     // Catch:{ RuntimeException -> 0x02ae, Exception -> 0x02aa, all -> 0x02a6 }
        L_0x0293:
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02a0 }
            if (r0 == 0) goto L_0x029c
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02a0 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02a0 }
        L_0x029c:
            r1 = 0
            r7.mSocketDhcp = r1     // Catch:{ IOException -> 0x02a0 }
            goto L_0x02a2
        L_0x02a0:
            r0 = move-exception
        L_0x02a2:
            r11 = r20
            goto L_0x032a
        L_0x02a6:
            r0 = move-exception
            r1 = r0
            goto L_0x032b
        L_0x02aa:
            r0 = move-exception
            r11 = r20
            goto L_0x02bb
        L_0x02ae:
            r0 = move-exception
            r11 = r20
            goto L_0x0309
        L_0x02b2:
            r0 = move-exception
            r20 = r11
            r1 = r0
            goto L_0x032b
        L_0x02b8:
            r0 = move-exception
            r20 = r11
        L_0x02bb:
            java.lang.Throwable r1 = r0.getCause()     // Catch:{ all -> 0x0301 }
            boolean r2 = r1 instanceof android.system.ErrnoException     // Catch:{ all -> 0x0301 }
            if (r2 == 0) goto L_0x02f2
            r2 = r1
            android.system.ErrnoException r2 = (android.system.ErrnoException) r2     // Catch:{ all -> 0x0301 }
            int r2 = r2.errno     // Catch:{ all -> 0x0301 }
            int r3 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x0301 }
            if (r2 == r3) goto L_0x02f2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0301 }
            r2.<init>()     // Catch:{ all -> 0x0301 }
            java.lang.String r3 = "Exception "
            r2.append(r3)     // Catch:{ all -> 0x0301 }
            java.lang.Thread r3 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x0301 }
            java.lang.StackTraceElement[] r3 = r3.getStackTrace()     // Catch:{ all -> 0x0301 }
            r4 = 2
            r3 = r3[r4]     // Catch:{ all -> 0x0301 }
            int r3 = r3.getLineNumber()     // Catch:{ all -> 0x0301 }
            r2.append(r3)     // Catch:{ all -> 0x0301 }
            r2.append(r0)     // Catch:{ all -> 0x0301 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0301 }
            android.util.Log.e(r10, r2)     // Catch:{ all -> 0x0301 }
        L_0x02f2:
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02ff }
            if (r0 == 0) goto L_0x02fb
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02ff }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ff }
        L_0x02fb:
            r1 = 0
            r7.mSocketDhcp = r1     // Catch:{ IOException -> 0x02ff }
        L_0x02fe:
            goto L_0x032a
        L_0x02ff:
            r0 = move-exception
            goto L_0x032a
        L_0x0301:
            r0 = move-exception
            r1 = r0
            r20 = r11
            goto L_0x032b
        L_0x0306:
            r0 = move-exception
            r20 = r11
        L_0x0309:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x0301 }
            r1.<init>()     // Catch:{ all -> 0x0301 }
            java.lang.String r2 = "RuntimeException "
            r1.append(r2)     // Catch:{ all -> 0x0301 }
            r1.append(r0)     // Catch:{ all -> 0x0301 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x0301 }
            android.util.Log.e(r10, r1)     // Catch:{ all -> 0x0301 }
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02ff }
            if (r0 == 0) goto L_0x0326
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x02ff }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x02ff }
        L_0x0326:
            r1 = 0
            r7.mSocketDhcp = r1     // Catch:{ IOException -> 0x02ff }
            goto L_0x02fe
        L_0x032a:
            return r11
        L_0x032b:
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x0338 }
            if (r0 == 0) goto L_0x0334
            java.io.FileDescriptor r0 = r7.mSocketDhcp     // Catch:{ IOException -> 0x0338 }
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r0)     // Catch:{ IOException -> 0x0338 }
        L_0x0334:
            r2 = 0
            r7.mSocketDhcp = r2     // Catch:{ IOException -> 0x0338 }
            goto L_0x0339
        L_0x0338:
            r0 = move-exception
        L_0x0339:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendDhcp(android.net.LinkProperties, int, byte[], int, java.lang.String):int");
    }

    private ByteBuffer createPacketDns(int dnsSourcePort, byte[] srcIp, byte[] dstIp, byte[] dnsMessage, byte[] dstMac, boolean isUDP) {
        byte[] bArr = dnsMessage;
        ByteBuffer sumBuf = ByteBuffer.allocate(1500);
        ByteBuffer etherBuf = ByteBuffer.allocate(14);
        ByteBuffer ipBuf = ByteBuffer.allocate(20);
        sumBuf.clear();
        sumBuf.order(ByteOrder.BIG_ENDIAN);
        etherBuf.clear();
        etherBuf.order(ByteOrder.BIG_ENDIAN);
        ipBuf.clear();
        ipBuf.order(ByteOrder.BIG_ENDIAN);
        makeEthernet(etherBuf, dstMac, getMacAddress("wlan0"), ETHER_IP_TYPE);
        if (isUDP) {
            ByteBuffer udpBuf = ByteBuffer.allocate(8);
            ByteBuffer dnsBuf = ByteBuffer.allocate(bArr.length);
            udpBuf.clear();
            udpBuf.order(ByteOrder.BIG_ENDIAN);
            dnsBuf.clear();
            dnsBuf.order(ByteOrder.BIG_ENDIAN);
            makeIP(ipBuf, bArr.length + 28, 17, srcIp, dstIp);
            makeUDP(udpBuf, dnsSourcePort, 53, 8 + bArr.length, 0);
            sumBuf.put(etherBuf).put(ipBuf).put(udpBuf).put(ByteBuffer.wrap(dnsMessage));
            sumBuf.flip();
            return sumBuf;
        }
        ByteBuffer tcpBuf = ByteBuffer.allocate(20);
        ByteBuffer pseudoBuf = ByteBuffer.allocate(32);
        tcpBuf.clear();
        tcpBuf.order(ByteOrder.BIG_ENDIAN);
        pseudoBuf.clear();
        pseudoBuf.order(ByteOrder.BIG_ENDIAN);
        makeIP(ipBuf, 40, 6, srcIp, dstIp);
        makePsuedoHeader(pseudoBuf, srcIp, dstIp, 6, 20);
        makeTCP(tcpBuf, pseudoBuf, 53);
        sumBuf.put(etherBuf).put(ipBuf).put(tcpBuf);
        sumBuf.flip();
        return sumBuf;
    }

    public byte[] sendDns(LinkProperties linkProperties, byte[] srcAddr, byte[] dstAddr, String dstMac, byte[] dnsMessage, long[] timeoutInMs, boolean isUDP) throws IOException {
        if (isUDP) {
            return sendDnsToUDP(linkProperties, timeoutInMs, srcAddr, dstAddr, dnsMessage, dstMac);
        }
        return sendDNSToTCP(linkProperties, timeoutInMs, srcAddr, dstAddr, dnsMessage, dstMac);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:120:0x0348, code lost:
        libcore.io.IoBridge.closeAndSignalBlockedThreads(r14);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x01e3, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x01e4, code lost:
        r21 = r2;
        r22 = r3;
        r23 = r4;
        r17 = r5;
        r2 = r7;
        r6 = r29;
        r1 = r18;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0269, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x026a, code lost:
        r21 = r2;
        r2 = r7;
        r6 = r29;
        r1 = r0;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0316 A[Catch:{ IOException -> 0x031f }] */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x0348 A[Catch:{ IOException -> 0x0351 }] */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0269 A[ExcHandler: all (r0v27 'th' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:12:0x00ae] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] sendDnsToUDP(android.net.LinkProperties r31, long[] r32, byte[] r33, byte[] r34, byte[] r35, java.lang.String r36) throws java.io.IOException {
        /*
            r30 = this;
            r1 = r32
            java.lang.String r9 = "Exception "
            java.lang.String r10 = ") EMPTY  size recvBuf"
            java.lang.String r11 = "Closing Exception"
            java.lang.String r12 = "MobileWips::FrameworkPktSender"
            r13 = 0
            r14 = 0
            r2 = 0
            r0 = -190(0xffffffffffffff42, float:NaN)
            int r15 = android.net.TrafficStats.getAndSetThreadStatsTag(r0)
            java.net.DatagramSocket r0 = new java.net.DatagramSocket     // Catch:{ SocketException -> 0x0359 }
            r0.<init>()     // Catch:{ SocketException -> 0x0359 }
            r16 = r0
            int r8 = r16.getLocalPort()
            r7 = r30
            r6 = r36
            byte[] r5 = r7.macStringToByteArray(r6)
            r0 = 1
            r2 = r30
            r3 = r8
            r4 = r33
            r17 = r5
            r5 = r34
            r6 = r35
            r7 = r17
            r18 = r13
            r13 = r8
            r8 = r0
            java.nio.ByteBuffer r2 = r2.createPacketDns(r3, r4, r5, r6, r7, r8)
            r0 = 1500(0x5dc, float:2.102E-42)
            byte[] r7 = new byte[r0]
            r5 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            int r3 = android.system.OsConstants.SOCK_RAW     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r3, r5)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r14 = r0
            java.lang.String r0 = r31.getInterfaceName()     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r4 = r0
            android.system.PacketSocketAddress r0 = new android.system.PacketSocketAddress     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            int r3 = android.system.OsConstants.ETH_P_IP     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            short r3 = (short) r3     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            int r8 = r4.index     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r0.<init>(r3, r8)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            android.system.Os.bind(r14, r0)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r0.<init>()     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            java.lang.String r3 = "DNS : "
            r0.append(r3)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            byte[] r3 = r2.array()     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            java.lang.String r3 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r3)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r0.append(r3)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            android.util.Log.d(r12, r0)     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            android.system.PacketSocketAddress r0 = new android.system.PacketSocketAddress     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            int r3 = r4.index     // Catch:{ Exception -> 0x02b6, all -> 0x02ad }
            r8 = r17
            r0.<init>(r3, r8)     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r24 = r0
            int r3 = r1.length     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
        L_0x0089:
            if (r5 >= r3) goto L_0x027b
            r19 = r1[r5]     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r25 = r19
            long r19 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            long r27 = r19 + r25
            byte[] r20 = r2.array()     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r21 = 0
            int r22 = r2.limit()     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r23 = 0
            r19 = r14
            android.system.Os.sendto(r19, r20, r21, r22, r23, r24)     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            java.net.InetSocketAddress r0 = new java.net.InetSocketAddress     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r0.<init>()     // Catch:{ Exception -> 0x02a5, all -> 0x029d }
            r29 = r8
            r8 = r0
        L_0x00ae:
            long r19 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x0272, all -> 0x0269 }
            int r0 = (r19 > r27 ? 1 : (r19 == r27 ? 0 : -1))
            if (r0 >= 0) goto L_0x0255
            long r19 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x0272, all -> 0x0269 }
            long r19 = r27 - r19
            int r0 = android.system.OsConstants.SOL_SOCKET     // Catch:{ Exception -> 0x01e3, all -> 0x0269 }
            int r6 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ Exception -> 0x01e3, all -> 0x0269 }
            android.system.StructTimeval r1 = android.system.StructTimeval.fromMillis(r19)     // Catch:{ Exception -> 0x01e3, all -> 0x0269 }
            android.system.Os.setsockoptTimeval(r14, r0, r6, r1)     // Catch:{ Exception -> 0x01e3, all -> 0x0269 }
            r0 = 0
            int r6 = r7.length     // Catch:{ Exception -> 0x01e3, all -> 0x0269 }
            r1 = 0
            r22 = r3
            r3 = r14
            r23 = r4
            r4 = r7
            r17 = r5
            r5 = r0
            r21 = r2
            r2 = r7
            r7 = r1
            int r0 = android.system.Os.recvfrom(r3, r4, r5, r6, r7, r8)     // Catch:{ Exception -> 0x01dd, all -> 0x01d7 }
            r1 = r0
            r0 = 44
            if (r1 <= r0) goto L_0x01d1
            r3 = 2
            byte[] r0 = new byte[r3]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r4 = 34
            byte r4 = r2[r4]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r5 = 0
            r0[r5] = r4     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r4 = 35
            byte r4 = r2[r4]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r6 = 1
            r0[r6] = r4     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r0)     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r4 = 53
            if (r0 != r4) goto L_0x01c2
            byte[] r0 = new byte[r3]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r4 = 36
            byte r4 = r2[r4]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r0[r5] = r4     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r4 = 37
            byte r4 = r2[r4]     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            r0[r6] = r4     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r0)     // Catch:{ Exception -> 0x01cd, all -> 0x01c5 }
            if (r0 != r13) goto L_0x01bf
            r0 = 1
            r4 = r5
            r7 = r0
        L_0x0110:
            r6 = r29
            int r0 = r6.length     // Catch:{ Exception -> 0x01bd }
            if (r4 >= r0) goto L_0x0126
            int r0 = r4 + 6
            byte r0 = r2[r0]     // Catch:{ Exception -> 0x01bd }
            byte r5 = r6[r4]     // Catch:{ Exception -> 0x01bd }
            if (r0 == r5) goto L_0x011f
            r0 = 0
            r7 = r0
        L_0x011f:
            int r4 = r4 + 1
            r29 = r6
            r5 = 0
            r6 = 1
            goto L_0x0110
        L_0x0126:
            byte[] r4 = new byte[r3]     // Catch:{ Exception -> 0x01bd }
            r0 = 42
            byte r0 = r2[r0]     // Catch:{ Exception -> 0x01bd }
            r5 = 0
            r4[r5] = r0     // Catch:{ Exception -> 0x01bd }
            r0 = 43
            byte r0 = r2[r0]     // Catch:{ Exception -> 0x01bd }
            r5 = 1
            r4[r5] = r0     // Catch:{ Exception -> 0x01bd }
            int r4 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r4)     // Catch:{ Exception -> 0x01bd }
            if (r7 == 0) goto L_0x01d3
            byte[] r5 = new byte[r3]     // Catch:{ Exception -> 0x01bd }
            r18 = 0
            byte r29 = r35[r18]     // Catch:{ Exception -> 0x01bd }
            r5[r18] = r29     // Catch:{ Exception -> 0x01bd }
            r0 = 1
            byte r18 = r35[r0]     // Catch:{ Exception -> 0x01bd }
            r5[r0] = r18     // Catch:{ Exception -> 0x01bd }
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r5)     // Catch:{ Exception -> 0x01bd }
            if (r0 != r4) goto L_0x01d3
            int r0 = r1 + -42
            byte[] r0 = new byte[r0]     // Catch:{ Exception -> 0x01bd }
            r5 = r0
            r18 = 0
            r0 = r18
        L_0x0158:
            int r3 = r5.length     // Catch:{ Exception -> 0x01bd }
            if (r0 >= r3) goto L_0x0165
            int r3 = r0 + 42
            byte r3 = r2[r3]     // Catch:{ Exception -> 0x01bd }
            r5[r0] = r3     // Catch:{ Exception -> 0x01bd }
            int r0 = r0 + 1
            r3 = 2
            goto L_0x0158
        L_0x0165:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x01bd }
            r0.<init>()     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = "DNS Recv (length: "
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            r0.append(r1)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = ") ID: "
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            r0.append(r4)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x01bd }
            android.util.Log.d(r12, r0)     // Catch:{ Exception -> 0x01bd }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x01bd }
            r0.<init>()     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = "DNS ll Final (length: "
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            r0.append(r1)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = ") : "
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r5)     // Catch:{ Exception -> 0x01bd }
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r3 = " "
            r0.append(r3)     // Catch:{ Exception -> 0x01bd }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x01bd }
            android.util.Log.d(r12, r0)     // Catch:{ Exception -> 0x01bd }
            android.net.TrafficStats.setThreadStatsTag(r15)     // Catch:{ IOException -> 0x01b5 }
            if (r14 == 0) goto L_0x01af
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r14)     // Catch:{ IOException -> 0x01b5 }
        L_0x01af:
            r14 = 0
            r16.close()     // Catch:{ IOException -> 0x01b5 }
            goto L_0x01bc
        L_0x01b5:
            r0 = move-exception
            android.util.Log.d(r12, r11)
            r0.printStackTrace()
        L_0x01bc:
            return r5
        L_0x01bd:
            r0 = move-exception
            goto L_0x01f1
        L_0x01bf:
            r6 = r29
            goto L_0x01d3
        L_0x01c2:
            r6 = r29
            goto L_0x01d3
        L_0x01c5:
            r0 = move-exception
            r6 = r29
            r18 = r1
            r1 = r0
            goto L_0x0343
        L_0x01cd:
            r0 = move-exception
            r6 = r29
            goto L_0x01f1
        L_0x01d1:
            r6 = r29
        L_0x01d3:
            r18 = r1
            goto L_0x0243
        L_0x01d7:
            r0 = move-exception
            r6 = r29
            r1 = r0
            goto L_0x0343
        L_0x01dd:
            r0 = move-exception
            r6 = r29
            r1 = r18
            goto L_0x01f1
        L_0x01e3:
            r0 = move-exception
            r21 = r2
            r22 = r3
            r23 = r4
            r17 = r5
            r2 = r7
            r6 = r29
            r1 = r18
        L_0x01f1:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0252 }
            r3.<init>()     // Catch:{ Exception -> 0x0252 }
            java.lang.String r4 = "X-Exception -> DNS Recv (length: "
            r3.append(r4)     // Catch:{ Exception -> 0x0252 }
            r3.append(r1)     // Catch:{ Exception -> 0x0252 }
            r3.append(r10)     // Catch:{ Exception -> 0x0252 }
            int r4 = r2.length     // Catch:{ Exception -> 0x0252 }
            r3.append(r4)     // Catch:{ Exception -> 0x0252 }
            java.lang.String r3 = r3.toString()     // Catch:{ Exception -> 0x0252 }
            android.util.Log.d(r12, r3)     // Catch:{ Exception -> 0x0252 }
            java.lang.Throwable r3 = r0.getCause()     // Catch:{ Exception -> 0x0252 }
            boolean r4 = r3 instanceof android.system.ErrnoException     // Catch:{ Exception -> 0x0252 }
            if (r4 == 0) goto L_0x0241
            r4 = r3
            android.system.ErrnoException r4 = (android.system.ErrnoException) r4     // Catch:{ Exception -> 0x0252 }
            int r4 = r4.errno     // Catch:{ Exception -> 0x0252 }
            int r5 = android.system.OsConstants.EAGAIN     // Catch:{ Exception -> 0x0252 }
            if (r4 == r5) goto L_0x0241
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0252 }
            r4.<init>()     // Catch:{ Exception -> 0x0252 }
            r4.append(r9)     // Catch:{ Exception -> 0x0252 }
            java.lang.Thread r5 = java.lang.Thread.currentThread()     // Catch:{ Exception -> 0x0252 }
            java.lang.StackTraceElement[] r5 = r5.getStackTrace()     // Catch:{ Exception -> 0x0252 }
            r7 = 2
            r5 = r5[r7]     // Catch:{ Exception -> 0x0252 }
            int r5 = r5.getLineNumber()     // Catch:{ Exception -> 0x0252 }
            r4.append(r5)     // Catch:{ Exception -> 0x0252 }
            r4.append(r0)     // Catch:{ Exception -> 0x0252 }
            java.lang.String r4 = r4.toString()     // Catch:{ Exception -> 0x0252 }
            android.util.Log.e(r12, r4)     // Catch:{ Exception -> 0x0252 }
        L_0x0241:
            r18 = r1
        L_0x0243:
            r1 = r32
            r7 = r2
            r29 = r6
            r5 = r17
            r2 = r21
            r3 = r22
            r4 = r23
            goto L_0x00ae
        L_0x0252:
            r0 = move-exception
            goto L_0x02be
        L_0x0255:
            r21 = r2
            r22 = r3
            r23 = r4
            r17 = r5
            r2 = r7
            r6 = r29
            int r5 = r17 + 1
            r1 = r32
            r8 = r6
            r2 = r21
            goto L_0x0089
        L_0x0269:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r29
            r1 = r0
            goto L_0x0343
        L_0x0272:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r29
            r1 = r18
            goto L_0x02be
        L_0x027b:
            r21 = r2
            r23 = r4
            r2 = r7
            r6 = r8
            android.net.TrafficStats.setThreadStatsTag(r15)     // Catch:{ IOException -> 0x0292 }
            if (r14 == 0) goto L_0x0289
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r14)     // Catch:{ IOException -> 0x0292 }
        L_0x0289:
            r14 = 0
            r16.close()     // Catch:{ IOException -> 0x0292 }
            r1 = r18
            goto L_0x0327
        L_0x0292:
            r0 = move-exception
            android.util.Log.d(r12, r11)
            r0.printStackTrace()
            r1 = r18
            goto L_0x0327
        L_0x029d:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r8
            r1 = r0
            goto L_0x0343
        L_0x02a5:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r8
            r1 = r18
            goto L_0x02be
        L_0x02ad:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r17
            r1 = r0
            goto L_0x0343
        L_0x02b6:
            r0 = move-exception
            r21 = r2
            r2 = r7
            r6 = r17
            r1 = r18
        L_0x02be:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x033f }
            r3.<init>()     // Catch:{ all -> 0x033f }
            java.lang.String r4 = "Exception -> DNS Recv (length: "
            r3.append(r4)     // Catch:{ all -> 0x033f }
            r3.append(r1)     // Catch:{ all -> 0x033f }
            r3.append(r10)     // Catch:{ all -> 0x033f }
            int r4 = r2.length     // Catch:{ all -> 0x033f }
            r3.append(r4)     // Catch:{ all -> 0x033f }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x033f }
            android.util.Log.d(r12, r3)     // Catch:{ all -> 0x033f }
            java.lang.Throwable r3 = r0.getCause()     // Catch:{ all -> 0x033f }
            r0.printStackTrace()     // Catch:{ all -> 0x033f }
            boolean r4 = r3 instanceof android.system.ErrnoException     // Catch:{ all -> 0x033f }
            if (r4 == 0) goto L_0x0311
            r4 = r3
            android.system.ErrnoException r4 = (android.system.ErrnoException) r4     // Catch:{ all -> 0x033f }
            int r4 = r4.errno     // Catch:{ all -> 0x033f }
            int r5 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x033f }
            if (r4 == r5) goto L_0x0311
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x033f }
            r4.<init>()     // Catch:{ all -> 0x033f }
            r4.append(r9)     // Catch:{ all -> 0x033f }
            java.lang.Thread r5 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x033f }
            java.lang.StackTraceElement[] r5 = r5.getStackTrace()     // Catch:{ all -> 0x033f }
            r7 = 2
            r5 = r5[r7]     // Catch:{ all -> 0x033f }
            int r5 = r5.getLineNumber()     // Catch:{ all -> 0x033f }
            r4.append(r5)     // Catch:{ all -> 0x033f }
            r4.append(r0)     // Catch:{ all -> 0x033f }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x033f }
            android.util.Log.e(r12, r4)     // Catch:{ all -> 0x033f }
        L_0x0311:
            android.net.TrafficStats.setThreadStatsTag(r15)     // Catch:{ IOException -> 0x031f }
            if (r14 == 0) goto L_0x0319
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r14)     // Catch:{ IOException -> 0x031f }
        L_0x0319:
            r14 = 0
            r16.close()     // Catch:{ IOException -> 0x031f }
            goto L_0x0327
        L_0x031f:
            r0 = move-exception
            android.util.Log.d(r12, r11)
            r0.printStackTrace()
        L_0x0327:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "No Response > DNS Recv (length: "
            r0.append(r3)
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.d(r12, r0)
            r3 = 0
            byte[] r0 = new byte[r3]
            return r0
        L_0x033f:
            r0 = move-exception
            r18 = r1
            r1 = r0
        L_0x0343:
            android.net.TrafficStats.setThreadStatsTag(r15)     // Catch:{ IOException -> 0x0351 }
            if (r14 == 0) goto L_0x034b
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r14)     // Catch:{ IOException -> 0x0351 }
        L_0x034b:
            r14 = 0
            r16.close()     // Catch:{ IOException -> 0x0351 }
            goto L_0x0358
        L_0x0351:
            r0 = move-exception
            android.util.Log.d(r12, r11)
            r0.printStackTrace()
        L_0x0358:
            throw r1
        L_0x0359:
            r0 = move-exception
            r18 = r13
            java.io.IOException r1 = new java.io.IOException
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "SocketException of DatagramSocket. Message: "
            r3.append(r4)
            java.lang.String r4 = r0.getMessage()
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            r1.<init>(r3)
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendDnsToUDP(android.net.LinkProperties, long[], byte[], byte[], byte[], java.lang.String):byte[]");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0119, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0185, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x0186, code lost:
        r18 = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x0189, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x018a, code lost:
        r23 = r13;
        r13 = r18;
        r18 = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x01bf, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x01c0, code lost:
        r23 = r13;
        r10 = r17;
        r13 = r18;
        r18 = r19;
        r17 = r9;
        r19 = r12;
        r11 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0235, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x0236, code lost:
        r1 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x0252, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x0253, code lost:
        r17 = r9;
        r1 = r0;
        r11 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x025a, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x025b, code lost:
        r17 = r9;
        r8 = r16;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x02bf A[SYNTHETIC, Splitter:B:109:0x02bf] */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x02eb A[SYNTHETIC, Splitter:B:119:0x02eb] */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0235 A[ExcHandler: all (r0v31 'th' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:26:0x00e4] */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0252 A[ExcHandler: all (r0v25 'th' java.lang.Throwable A[CUSTOM_DECLARE]), PHI: r9 r16 
      PHI: (r9v8 'sumBuf' java.nio.ByteBuffer) = (r9v7 'sumBuf' java.nio.ByteBuffer), (r9v9 'sumBuf' java.nio.ByteBuffer), (r9v9 'sumBuf' java.nio.ByteBuffer), (r9v9 'sumBuf' java.nio.ByteBuffer) binds: [B:8:0x007a, B:14:0x00b6, B:15:?, B:17:0x00c5] A[DONT_GENERATE, DONT_INLINE]
      PHI: (r16v5 'readLen' int) = (r16v4 'readLen' int), (r16v6 'readLen' int), (r16v6 'readLen' int), (r16v6 'readLen' int) binds: [B:8:0x007a, B:14:0x00b6, B:15:?, B:17:0x00c5] A[DONT_GENERATE, DONT_INLINE], Splitter:B:8:0x007a] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] sendDNSToTCP(android.net.LinkProperties r34, long[] r35, byte[] r36, byte[] r37, byte[] r38, java.lang.String r39) {
        /*
            r33 = this;
            r1 = r35
            r2 = r39
            java.lang.String r3 = "Exception "
            java.lang.String r4 = "TCP SYN/ACK("
            java.lang.String r5 = ") : "
            java.lang.String r6 = "MobileWips::FrameworkPktSender"
            r7 = 0
            r8 = 0
            r15 = r33
            byte[] r14 = r15.macStringToByteArray(r2)
            r0 = 3
            byte[] r13 = new byte[r0]
            r10 = 0
            r0 = 0
            r9 = r33
            r11 = r36
            r12 = r37
            r16 = r14
            r15 = r0
            java.nio.ByteBuffer r9 = r9.createPacketDns(r10, r11, r12, r13, r14, r15)
            r10 = 2
            r11 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            int r12 = android.system.OsConstants.SOCK_RAW     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r12, r11)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r7 = r0
            java.lang.String r0 = r34.getInterfaceName()     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r12 = r0
            android.system.PacketSocketAddress r0 = new android.system.PacketSocketAddress     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            int r13 = android.system.OsConstants.ETH_P_IP     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            short r13 = (short) r13     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            int r14 = r12.index     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r0.<init>(r13, r14)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            android.system.Os.bind(r7, r0)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            byte[] r0 = r9.array()     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r13 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r0.<init>()     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            java.lang.String r14 = "TCP : "
            r0.append(r14)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            java.lang.String r14 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r13)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r0.append(r14)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            android.util.Log.d(r6, r0)     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            android.system.PacketSocketAddress r0 = new android.system.PacketSocketAddress     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            int r14 = r12.index     // Catch:{ Exception -> 0x0283, all -> 0x027b }
            r15 = r16
            r0.<init>(r14, r15)     // Catch:{ Exception -> 0x0277, all -> 0x0270 }
            r22 = r0
            java.lang.String r0 = "going to send now"
            android.util.Log.d(r6, r0)     // Catch:{ Exception -> 0x0277, all -> 0x0270 }
            int r14 = r1.length     // Catch:{ Exception -> 0x0277, all -> 0x0270 }
            r16 = r8
            r8 = r11
        L_0x0078:
            if (r8 >= r14) goto L_0x0260
            r17 = r1[r8]     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r29 = r17
            long r17 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            long r31 = r17 + r29
            byte[] r18 = r9.array()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r19 = 0
            int r20 = r9.limit()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r21 = 0
            r17 = r7
            android.system.Os.sendto(r17, r18, r19, r20, r21, r22)     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r0 = 1500(0x5dc, float:2.102E-42)
            byte[] r0 = new byte[r0]     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r17 = r0
            byte[] r0 = new byte[r10]     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r18 = r0
            byte[] r0 = new byte[r10]     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r19 = r0
            java.net.InetSocketAddress r28 = new java.net.InetSocketAddress     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            r28.<init>()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
        L_0x00a8:
            long r20 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            int r0 = (r20 > r31 ? 1 : (r20 == r31 ? 0 : -1))
            if (r0 >= 0) goto L_0x023c
            long r20 = android.os.SystemClock.elapsedRealtime()     // Catch:{ Exception -> 0x025a, all -> 0x0252 }
            long r20 = r31 - r20
            int r0 = android.system.OsConstants.SOL_SOCKET     // Catch:{ Exception -> 0x01bf, all -> 0x0252 }
            int r10 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ Exception -> 0x01bf, all -> 0x0252 }
            android.system.StructTimeval r11 = android.system.StructTimeval.fromMillis(r20)     // Catch:{ Exception -> 0x01bf, all -> 0x0252 }
            android.system.Os.setsockoptTimeval(r7, r0, r10, r11)     // Catch:{ Exception -> 0x01bf, all -> 0x0252 }
            r25 = 0
            r10 = r17
            int r0 = r10.length     // Catch:{ Exception -> 0x01b1, all -> 0x0252 }
            r27 = 0
            r23 = r7
            r24 = r10
            r26 = r0
            int r0 = android.system.Os.recvfrom(r23, r24, r25, r26, r27, r28)     // Catch:{ Exception -> 0x01b1, all -> 0x0252 }
            r11 = r0
            if (r11 <= 0) goto L_0x01a3
            int r0 = r10.length     // Catch:{ Exception -> 0x0197, all -> 0x0191 }
            r1 = 54
            if (r0 < r1) goto L_0x01a3
            r0 = 34
            r17 = r9
            r1 = r19
            r9 = 2
            r19 = r12
            r12 = 0
            java.lang.System.arraycopy(r10, r0, r1, r12, r9)     // Catch:{ Exception -> 0x0189, all -> 0x0235 }
            r0 = 36
            r23 = r13
            r13 = r18
            java.lang.System.arraycopy(r10, r0, r13, r12, r9)     // Catch:{ Exception -> 0x0185, all -> 0x0235 }
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r1)     // Catch:{ Exception -> 0x0185, all -> 0x0235 }
            r9 = 53
            if (r0 != r9) goto L_0x0182
            int r0 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r13)     // Catch:{ Exception -> 0x0185, all -> 0x0235 }
            r9 = 65000(0xfde8, float:9.1084E-41)
            if (r0 != r9) goto L_0x017f
            r0 = 1
            r9 = 0
            r12 = r9
            r9 = r0
        L_0x0105:
            int r0 = r15.length     // Catch:{ Exception -> 0x0185, all -> 0x0235 }
            if (r12 >= r0) goto L_0x011c
            int r0 = r12 + 6
            byte r0 = r10[r0]     // Catch:{ Exception -> 0x0185, all -> 0x0235 }
            r18 = r1
            byte r1 = r15[r12]     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            if (r0 == r1) goto L_0x0114
            r0 = 0
            r9 = r0
        L_0x0114:
            int r12 = r12 + 1
            r1 = r18
            goto L_0x0105
        L_0x0119:
            r0 = move-exception
            goto L_0x01ce
        L_0x011c:
            r18 = r1
            if (r9 == 0) goto L_0x01ad
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.<init>()     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            java.lang.String r1 = "TCP Recv (length: "
            r0.append(r1)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r11)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r5)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            java.lang.String r1 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r10)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r1)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            java.lang.String r1 = " "
            r0.append(r1)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            android.util.Log.d(r6, r0)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0 = 47
            byte r0 = r10[r0]     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r1 = 18
            if (r0 != r1) goto L_0x01ad
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.<init>()     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r4)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r2)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0.append(r5)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r1 = 1
            r0.append(r1)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            android.util.Log.d(r6, r0)     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r0 = 12
            byte[] r0 = new byte[r0]     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r12 = r0
            r16 = 0
            byte r0 = r38[r16]     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r12[r16] = r0     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            byte r0 = r38[r1]     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            r12[r1] = r0     // Catch:{ Exception -> 0x0119, all -> 0x0235 }
            if (r7 == 0) goto L_0x017c
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r7)     // Catch:{ IOException -> 0x017a }
            goto L_0x017c
        L_0x017a:
            r0 = move-exception
            goto L_0x017e
        L_0x017c:
            r7 = 0
        L_0x017e:
            return r12
        L_0x017f:
            r18 = r1
            goto L_0x01ad
        L_0x0182:
            r18 = r1
            goto L_0x01ad
        L_0x0185:
            r0 = move-exception
            r18 = r1
            goto L_0x01ce
        L_0x0189:
            r0 = move-exception
            r23 = r13
            r13 = r18
            r18 = r1
            goto L_0x01ce
        L_0x0191:
            r0 = move-exception
            r17 = r9
            r1 = r0
            goto L_0x02e9
        L_0x0197:
            r0 = move-exception
            r17 = r9
            r23 = r13
            r13 = r18
            r18 = r19
            r19 = r12
            goto L_0x01ce
        L_0x01a3:
            r17 = r9
            r23 = r13
            r13 = r18
            r18 = r19
            r19 = r12
        L_0x01ad:
            r16 = r11
            goto L_0x0223
        L_0x01b1:
            r0 = move-exception
            r17 = r9
            r23 = r13
            r13 = r18
            r18 = r19
            r19 = r12
            r11 = r16
            goto L_0x01ce
        L_0x01bf:
            r0 = move-exception
            r23 = r13
            r10 = r17
            r13 = r18
            r18 = r19
            r17 = r9
            r19 = r12
            r11 = r16
        L_0x01ce:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r1.<init>()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.String r9 = "X-Exception -> DNS Recv (length: "
            r1.append(r9)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r1.append(r11)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.String r9 = ") EMPTY  size recvBuf"
            r1.append(r9)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            int r9 = r10.length     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r1.append(r9)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.String r1 = r1.toString()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            android.util.Log.d(r6, r1)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.Throwable r1 = r0.getCause()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            boolean r9 = r1 instanceof android.system.ErrnoException     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            if (r9 == 0) goto L_0x0221
            r9 = r1
            android.system.ErrnoException r9 = (android.system.ErrnoException) r9     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            int r9 = r9.errno     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            int r12 = android.system.OsConstants.EAGAIN     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            if (r9 == r12) goto L_0x0221
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r9.<init>()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r9.append(r3)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.Thread r12 = java.lang.Thread.currentThread()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.StackTraceElement[] r12 = r12.getStackTrace()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r16 = 2
            r12 = r12[r16]     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            int r12 = r12.getLineNumber()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r9.append(r12)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            r9.append(r0)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            java.lang.String r9 = r9.toString()     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
            android.util.Log.e(r6, r9)     // Catch:{ Exception -> 0x0239, all -> 0x0235 }
        L_0x0221:
            r16 = r11
        L_0x0223:
            r1 = r35
            r9 = r17
            r12 = r19
            r11 = 0
            r17 = r10
            r19 = r18
            r10 = 2
            r18 = r13
            r13 = r23
            goto L_0x00a8
        L_0x0235:
            r0 = move-exception
            r1 = r0
            goto L_0x02e9
        L_0x0239:
            r0 = move-exception
            r8 = r11
            goto L_0x0288
        L_0x023c:
            r23 = r13
            r10 = r17
            r13 = r18
            r18 = r19
            r17 = r9
            r19 = r12
            int r8 = r8 + 1
            r1 = r35
            r13 = r23
            r10 = 2
            r11 = 0
            goto L_0x0078
        L_0x0252:
            r0 = move-exception
            r17 = r9
            r1 = r0
            r11 = r16
            goto L_0x02e9
        L_0x025a:
            r0 = move-exception
            r17 = r9
            r8 = r16
            goto L_0x0288
        L_0x0260:
            r17 = r9
            r19 = r12
            r23 = r13
            if (r7 == 0) goto L_0x026e
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r7)     // Catch:{ IOException -> 0x026c }
            goto L_0x026e
        L_0x026c:
            r0 = move-exception
            goto L_0x02ca
        L_0x026e:
            r7 = 0
            goto L_0x02ca
        L_0x0270:
            r0 = move-exception
            r17 = r9
            r1 = r0
            r11 = r8
            goto L_0x02e9
        L_0x0277:
            r0 = move-exception
            r17 = r9
            goto L_0x0288
        L_0x027b:
            r0 = move-exception
            r17 = r9
            r15 = r16
            r1 = r0
            r11 = r8
            goto L_0x02e9
        L_0x0283:
            r0 = move-exception
            r17 = r9
            r15 = r16
        L_0x0288:
            java.lang.Throwable r1 = r0.getCause()     // Catch:{ all -> 0x02e6 }
            boolean r9 = r1 instanceof android.system.ErrnoException     // Catch:{ all -> 0x02e6 }
            if (r9 == 0) goto L_0x02bd
            r9 = r1
            android.system.ErrnoException r9 = (android.system.ErrnoException) r9     // Catch:{ all -> 0x02e6 }
            int r9 = r9.errno     // Catch:{ all -> 0x02e6 }
            int r10 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x02e6 }
            if (r9 == r10) goto L_0x02bd
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ all -> 0x02e6 }
            r9.<init>()     // Catch:{ all -> 0x02e6 }
            r9.append(r3)     // Catch:{ all -> 0x02e6 }
            java.lang.Thread r3 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x02e6 }
            java.lang.StackTraceElement[] r3 = r3.getStackTrace()     // Catch:{ all -> 0x02e6 }
            r10 = 2
            r3 = r3[r10]     // Catch:{ all -> 0x02e6 }
            int r3 = r3.getLineNumber()     // Catch:{ all -> 0x02e6 }
            r9.append(r3)     // Catch:{ all -> 0x02e6 }
            r9.append(r0)     // Catch:{ all -> 0x02e6 }
            java.lang.String r3 = r9.toString()     // Catch:{ all -> 0x02e6 }
            android.util.Log.e(r6, r3)     // Catch:{ all -> 0x02e6 }
        L_0x02bd:
            if (r7 == 0) goto L_0x02c7
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r7)     // Catch:{ IOException -> 0x02c3 }
            goto L_0x02c7
        L_0x02c3:
            r0 = move-exception
            r16 = r8
            goto L_0x02ca
        L_0x02c7:
            r7 = 0
            r16 = r8
        L_0x02ca:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r4)
            r0.append(r2)
            r0.append(r5)
            r1 = 0
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.d(r6, r0)
            byte[] r0 = new byte[r1]
            return r0
        L_0x02e6:
            r0 = move-exception
            r1 = r0
            r11 = r8
        L_0x02e9:
            if (r7 == 0) goto L_0x02f1
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r7)     // Catch:{ IOException -> 0x02ef }
            goto L_0x02f1
        L_0x02ef:
            r0 = move-exception
            goto L_0x02f3
        L_0x02f1:
            r7 = 0
        L_0x02f3:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendDNSToTCP(android.net.LinkProperties, long[], byte[], byte[], byte[], java.lang.String):byte[]");
    }

    /* JADX WARNING: Removed duplicated region for block: B:113:0x02e8 A[SYNTHETIC, Splitter:B:113:0x02e8] */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x030d A[SYNTHETIC, Splitter:B:125:0x030d] */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x0314 A[SYNTHETIC, Splitter:B:128:0x0314] */
    /* JADX WARNING: Unknown top exception splitter block from list: {B:122:0x02f7=Splitter:B:122:0x02f7, B:106:0x02af=Splitter:B:106:0x02af} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean sendTcp(android.net.LinkProperties r37, int r38, byte[] r39, byte[] r40, java.lang.String r41) {
        /*
            r36 = this;
            r13 = r36
            r14 = r41
            java.lang.String r15 = "MobileWips::FrameworkPktSender"
            r1 = 0
            r16 = 0
            int r0 = android.system.OsConstants.AF_PACKET     // Catch:{ RuntimeException -> 0x02f5, Exception -> 0x02ad, all -> 0x02a7 }
            int r2 = android.system.OsConstants.SOCK_RAW     // Catch:{ RuntimeException -> 0x02f5, Exception -> 0x02ad, all -> 0x02a7 }
            r11 = 0
            java.io.FileDescriptor r0 = android.system.Os.socket(r0, r2, r11)     // Catch:{ RuntimeException -> 0x02f5, Exception -> 0x02ad, all -> 0x02a7 }
            r10 = r0
            java.lang.String r0 = r37.getInterfaceName()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            android.net.util.InterfaceParams r0 = android.net.util.InterfaceParams.getByName(r0)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            android.system.PacketSocketAddress r1 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            int r2 = android.system.OsConstants.ETH_P_IP     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            short r2 = (short) r2     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            int r3 = r0.index     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r1.<init>(r2, r3)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            android.system.Os.bind(r10, r1)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.lang.String r1 = "wlan0"
            byte[] r1 = r13.getMacAddress(r1)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r13.SRC_ADDR = r1     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r5 = r40
            r6 = r39
            byte[] r1 = r13.macStringToByteArray(r14)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r9 = r1
            long r1 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r8 = r38
            long r3 = (long) r8     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            long r29 = r1 + r3
            r7 = 1500(0x5dc, float:2.102E-42)
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r4 = r1
            r1 = 14
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r1)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r3 = r1
            r1 = 20
            java.nio.ByteBuffer r2 = java.nio.ByteBuffer.allocate(r1)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteBuffer r1 = java.nio.ByteBuffer.allocate(r1)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r17 = 32
            java.nio.ByteBuffer r17 = java.nio.ByteBuffer.allocate(r17)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r31 = r17
            r4.clear()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteOrder r7 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r4.order(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r3.clear()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteOrder r7 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r3.order(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r2.clear()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteOrder r7 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r2.order(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r1.clear()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteOrder r7 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r1.order(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r31.clear()     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            java.nio.ByteOrder r7 = java.nio.ByteOrder.BIG_ENDIAN     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r12 = r31
            r12.order(r7)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            byte[] r7 = r13.SRC_ADDR     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            byte[] r11 = ETHER_IP_TYPE     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r13.makeEthernet(r3, r9, r7, r11)     // Catch:{ RuntimeException -> 0x02a2, Exception -> 0x029d, all -> 0x0297 }
            r7 = 40
            r11 = 6
            r32 = r1
            r1 = r36
            r31 = r2
            r14 = r3
            r3 = r7
            r7 = r4
            r4 = r11
            r1.makeIP(r2, r3, r4, r5, r6)     // Catch:{ RuntimeException -> 0x0292, Exception -> 0x028d, all -> 0x0288 }
            r11 = 6
            r1 = 20
            r2 = r7
            r3 = 1500(0x5dc, float:2.102E-42)
            r7 = r36
            r8 = r12
            r4 = r9
            r9 = r5
            r33 = r10
            r10 = r6
            r34 = r5
            r3 = r12
            r5 = 2
            r12 = r1
            r7.makePsuedoHeader(r8, r9, r10, r11, r12)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r1 = 80
            r7 = r32
            r13.makeTCP(r7, r3, r1)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.nio.ByteBuffer r8 = r2.put(r14)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r9 = r31
            java.nio.ByteBuffer r8 = r8.put(r9)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r8.put(r7)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r2.flip()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            byte[] r8 = r2.array()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r10.<init>()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.lang.String r11 = "TCP : "
            r10.append(r11)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.lang.String r11 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r8)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r10.append(r11)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.lang.String r10 = r10.toString()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            android.util.Log.d(r15, r10)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            android.system.PacketSocketAddress r10 = new android.system.PacketSocketAddress     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            int r11 = r0.index     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r10.<init>(r11, r4)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r22 = r10
            byte[] r18 = r2.array()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r19 = 0
            int r20 = r2.limit()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r21 = 0
            r17 = r33
            android.system.Os.sendto(r17, r18, r19, r20, r21, r22)     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r10 = 1500(0x5dc, float:2.102E-42)
            byte[] r10 = new byte[r10]     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            byte[] r11 = new byte[r5]     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            byte[] r12 = new byte[r5]     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            java.net.InetSocketAddress r28 = new java.net.InetSocketAddress     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r28.<init>()     // Catch:{ RuntimeException -> 0x0280, Exception -> 0x0279, all -> 0x0271 }
            r35 = r16
        L_0x0115:
            long r16 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x0265, Exception -> 0x0259, all -> 0x024d }
            int r16 = (r16 > r29 ? 1 : (r16 == r29 ? 0 : -1))
            java.lang.String r1 = ") : "
            if (r16 >= 0) goto L_0x01dd
            long r18 = android.os.SystemClock.elapsedRealtime()     // Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01c9, all -> 0x01bf }
            long r18 = r29 - r18
            int r5 = android.system.OsConstants.SOL_SOCKET     // Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01c9, all -> 0x01bf }
            r21 = r0
            int r0 = android.system.OsConstants.SO_RCVTIMEO     // Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01c9, all -> 0x01bf }
            r31 = r2
            android.system.StructTimeval r2 = android.system.StructTimeval.fromMillis(r18)     // Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01c9, all -> 0x01bf }
            r32 = r3
            r3 = r33
            android.system.Os.setsockoptTimeval(r3, r5, r0, r2)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r25 = 0
            int r0 = r10.length     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r27 = 0
            r23 = r3
            r24 = r10
            r26 = r0
            int r0 = android.system.Os.recvfrom(r23, r24, r25, r26, r27, r28)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2 = 34
            r23 = r4
            r4 = 2
            r5 = 0
            java.lang.System.arraycopy(r10, r2, r12, r5, r4)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2 = 36
            java.lang.System.arraycopy(r10, r2, r11, r5, r4)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            int r2 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r12)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r4 = 80
            if (r2 != r4) goto L_0x0198
            int r2 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToInt(r11)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r4 = 65000(0xfde8, float:9.1084E-41)
            if (r2 != r4) goto L_0x0198
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2.<init>()     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            java.lang.String r4 = "TCP Recv (length: "
            r2.append(r4)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            java.lang.String r4 = java.lang.String.valueOf(r0)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2.append(r4)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2.append(r1)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            java.lang.String r1 = com.samsung.android.server.wifi.mobilewips.framework.MobileWipsFrameworkUtil.byteArrayToHexString(r10)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2.append(r1)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            java.lang.String r1 = " "
            r2.append(r1)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            java.lang.String r1 = r2.toString()     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            android.util.Log.d(r15, r1)     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r1 = 47
            byte r1 = r10[r1]     // Catch:{ RuntimeException -> 0x01b7, Exception -> 0x01af, all -> 0x01a7 }
            r2 = 18
            if (r1 != r2) goto L_0x0198
            r1 = 1
            r35 = r1
        L_0x0198:
            r33 = r3
            r0 = r21
            r4 = r23
            r2 = r31
            r3 = r32
            r1 = 80
            r5 = 2
            goto L_0x0115
        L_0x01a7:
            r0 = move-exception
            r2 = r41
            r1 = r0
            r16 = r35
            goto L_0x0312
        L_0x01af:
            r0 = move-exception
            r2 = r41
            r1 = r3
            r16 = r35
            goto L_0x02af
        L_0x01b7:
            r0 = move-exception
            r2 = r41
            r1 = r3
            r16 = r35
            goto L_0x02f7
        L_0x01bf:
            r0 = move-exception
            r3 = r33
            r2 = r41
            r1 = r0
            r16 = r35
            goto L_0x0312
        L_0x01c9:
            r0 = move-exception
            r3 = r33
            r2 = r41
            r1 = r3
            r16 = r35
            goto L_0x02af
        L_0x01d3:
            r0 = move-exception
            r3 = r33
            r2 = r41
            r1 = r3
            r16 = r35
            goto L_0x02f7
        L_0x01dd:
            r21 = r0
            r31 = r2
            r32 = r3
            r23 = r4
            r3 = r33
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ RuntimeException -> 0x0243, Exception -> 0x0239, all -> 0x022f }
            r0.<init>()     // Catch:{ RuntimeException -> 0x0243, Exception -> 0x0239, all -> 0x022f }
            java.lang.String r2 = "TCP SYN/ACK("
            r0.append(r2)     // Catch:{ RuntimeException -> 0x0243, Exception -> 0x0239, all -> 0x022f }
            r2 = r41
            r4 = r14
            r0.append(r2)     // Catch:{ RuntimeException -> 0x022d, Exception -> 0x022b, all -> 0x0229 }
            r0.append(r1)     // Catch:{ RuntimeException -> 0x022d, Exception -> 0x022b, all -> 0x0229 }
            r1 = r35
            r0.append(r1)     // Catch:{ RuntimeException -> 0x0223, Exception -> 0x021d, all -> 0x0217 }
            java.lang.String r0 = r0.toString()     // Catch:{ RuntimeException -> 0x0223, Exception -> 0x021d, all -> 0x0217 }
            android.util.Log.d(r15, r0)     // Catch:{ RuntimeException -> 0x0223, Exception -> 0x021d, all -> 0x0217 }
            if (r3 == 0) goto L_0x0212
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r3)     // Catch:{ IOException -> 0x020c }
            goto L_0x0212
        L_0x020c:
            r0 = move-exception
            r16 = r1
            r10 = r3
            goto L_0x0311
        L_0x0212:
            r10 = 0
            r16 = r1
            goto L_0x0311
        L_0x0217:
            r0 = move-exception
            r16 = r1
            r1 = r0
            goto L_0x0312
        L_0x021d:
            r0 = move-exception
            r16 = r1
            r1 = r3
            goto L_0x02af
        L_0x0223:
            r0 = move-exception
            r16 = r1
            r1 = r3
            goto L_0x02f7
        L_0x0229:
            r0 = move-exception
            goto L_0x0232
        L_0x022b:
            r0 = move-exception
            goto L_0x023c
        L_0x022d:
            r0 = move-exception
            goto L_0x0246
        L_0x022f:
            r0 = move-exception
            r2 = r41
        L_0x0232:
            r1 = r35
            r16 = r1
            r1 = r0
            goto L_0x0312
        L_0x0239:
            r0 = move-exception
            r2 = r41
        L_0x023c:
            r1 = r35
            r16 = r1
            r1 = r3
            goto L_0x02af
        L_0x0243:
            r0 = move-exception
            r2 = r41
        L_0x0246:
            r1 = r35
            r16 = r1
            r1 = r3
            goto L_0x02f7
        L_0x024d:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r35
            r16 = r1
            r1 = r0
            goto L_0x0312
        L_0x0259:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r35
            r16 = r1
            r1 = r3
            goto L_0x02af
        L_0x0265:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r35
            r16 = r1
            r1 = r3
            goto L_0x02f7
        L_0x0271:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r0
            goto L_0x0312
        L_0x0279:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r3
            goto L_0x02af
        L_0x0280:
            r0 = move-exception
            r2 = r41
            r3 = r33
            r1 = r3
            goto L_0x02f7
        L_0x0288:
            r0 = move-exception
            r2 = r41
            r3 = r10
            goto L_0x029a
        L_0x028d:
            r0 = move-exception
            r2 = r41
            r3 = r10
            goto L_0x02a0
        L_0x0292:
            r0 = move-exception
            r2 = r41
            r3 = r10
            goto L_0x02a5
        L_0x0297:
            r0 = move-exception
            r3 = r10
            r2 = r14
        L_0x029a:
            r1 = r0
            goto L_0x0312
        L_0x029d:
            r0 = move-exception
            r3 = r10
            r2 = r14
        L_0x02a0:
            r1 = r3
            goto L_0x02af
        L_0x02a2:
            r0 = move-exception
            r3 = r10
            r2 = r14
        L_0x02a5:
            r1 = r3
            goto L_0x02f7
        L_0x02a7:
            r0 = move-exception
            r2 = r14
            r3 = r1
            r1 = r0
            goto L_0x0312
        L_0x02ad:
            r0 = move-exception
            r2 = r14
        L_0x02af:
            java.lang.Throwable r3 = r0.getCause()     // Catch:{ all -> 0x02f1 }
            boolean r4 = r3 instanceof android.system.ErrnoException     // Catch:{ all -> 0x02f1 }
            if (r4 == 0) goto L_0x02e6
            r4 = r3
            android.system.ErrnoException r4 = (android.system.ErrnoException) r4     // Catch:{ all -> 0x02f1 }
            int r4 = r4.errno     // Catch:{ all -> 0x02f1 }
            int r5 = android.system.OsConstants.EAGAIN     // Catch:{ all -> 0x02f1 }
            if (r4 == r5) goto L_0x02e6
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x02f1 }
            r4.<init>()     // Catch:{ all -> 0x02f1 }
            java.lang.String r5 = "Exception "
            r4.append(r5)     // Catch:{ all -> 0x02f1 }
            java.lang.Thread r5 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x02f1 }
            java.lang.StackTraceElement[] r5 = r5.getStackTrace()     // Catch:{ all -> 0x02f1 }
            r6 = 2
            r5 = r5[r6]     // Catch:{ all -> 0x02f1 }
            int r5 = r5.getLineNumber()     // Catch:{ all -> 0x02f1 }
            r4.append(r5)     // Catch:{ all -> 0x02f1 }
            r4.append(r0)     // Catch:{ all -> 0x02f1 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x02f1 }
            android.util.Log.e(r15, r4)     // Catch:{ all -> 0x02f1 }
        L_0x02e6:
            if (r1 == 0) goto L_0x02ef
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r1)     // Catch:{ IOException -> 0x02ec }
            goto L_0x02ef
        L_0x02ec:
            r0 = move-exception
            r10 = r1
            goto L_0x0311
        L_0x02ef:
            r10 = 0
            goto L_0x0311
        L_0x02f1:
            r0 = move-exception
            r3 = r1
            r1 = r0
            goto L_0x0312
        L_0x02f5:
            r0 = move-exception
            r2 = r14
        L_0x02f7:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x02f1 }
            r3.<init>()     // Catch:{ all -> 0x02f1 }
            java.lang.String r4 = "RuntimeException "
            r3.append(r4)     // Catch:{ all -> 0x02f1 }
            r3.append(r0)     // Catch:{ all -> 0x02f1 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x02f1 }
            android.util.Log.e(r15, r3)     // Catch:{ all -> 0x02f1 }
            if (r1 == 0) goto L_0x02ef
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r1)     // Catch:{ IOException -> 0x02ec }
            goto L_0x02ef
        L_0x0311:
            return r16
        L_0x0312:
            if (r3 == 0) goto L_0x031a
            libcore.io.IoBridge.closeAndSignalBlockedThreads(r3)     // Catch:{ IOException -> 0x0318 }
            goto L_0x031a
        L_0x0318:
            r0 = move-exception
            goto L_0x031c
        L_0x031a:
            r3 = 0
        L_0x031c:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.mobilewips.framework.MobileWipsPacketSender.sendTcp(android.net.LinkProperties, int, byte[], byte[], java.lang.String):boolean");
    }

    public boolean pingTcp(byte[] srcAddrByte, byte[] dstAddrByte, int dstPort, int ttl, int timeoutMillis) throws IOException {
        InetAddress srcAddr = InetAddress.getByAddress(srcAddrByte);
        InetAddress dstAddr = InetAddress.getByAddress(dstAddrByte);
        int oldTag = TrafficStats.getAndSetThreadStatsTag(-190);
        FileDescriptor fd = null;
        try {
            fd = IoBridge.socket(OsConstants.AF_INET, OsConstants.SOCK_STREAM, 0);
            if (ttl > 0) {
                Os.setsockoptInt(fd, OsConstants.IPPROTO_IP, OsConstants.IP_TTL, Integer.valueOf(ttl).intValue());
            }
            if (srcAddr != null) {
                Os.bind(fd, srcAddr, 0);
            }
            Os.setsockoptIfreq(fd, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, "wlan0");
            Os.connect(fd, dstAddr, dstPort);
            return true;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (!(!(cause instanceof ErrnoException) || ((ErrnoException) cause).errno == OsConstants.EHOSTUNREACH || ((ErrnoException) cause).errno == OsConstants.EADDRNOTAVAIL)) {
                Log.d(TAG, "Exception : " + e);
            }
            return false;
        } finally {
            IoBridge.closeAndSignalBlockedThreads(fd);
            TrafficStats.setThreadStatsTag(oldTag);
        }
    }

    private long calculationChecksum(byte[] buf) {
        if (buf == null) {
            return 0;
        }
        int length = buf.length;
        int i = 0;
        long sum = 0;
        while (length > 1) {
            sum += (long) ((65280 & (buf[i] << 8)) | (buf[i + 1] & 255));
            if ((-65536 & sum) > 0) {
                sum = (sum & 65535) + 1;
            }
            i += 2;
            length -= 2;
        }
        if (length > 0) {
            sum += (long) (65280 & (buf[i] << 8));
            if ((-65536 & sum) > 0) {
                sum = (sum & 65535) + 1;
            }
        }
        return (~sum) & 65535;
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
                        concatBuf.put(MobileWipsFrameworkUtil.intToByteArray(Integer.valueOf(String.format("%02X", new Object[]{Byte.valueOf(mac[idx])}), 16).intValue())[3]);
                    }
                    concatBuf.flip();
                    return concatBuf.array();
                }
            }
            return null;
        } catch (Exception e) {
        }
    }
}
