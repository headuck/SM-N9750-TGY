package com.samsung.android.server.wifi.softap;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.shared.Inet4AddressUtils;
import android.os.Build;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public abstract class DhcpPacket {
    protected static final byte CLIENT_ID_ETHER = 1;
    protected static final byte DHCP_BOOTREPLY = 2;
    protected static final byte DHCP_BOOTREQUEST = 1;
    protected static final byte DHCP_BROADCAST_ADDRESS = 28;
    static final short DHCP_CLIENT = 68;
    protected static final byte DHCP_CLIENT_IDENTIFIER = 61;
    protected static final byte DHCP_DNS_SERVER = 6;
    protected static final byte DHCP_DOMAIN_NAME = 15;
    protected static final byte DHCP_HOST_NAME = 12;
    protected static final byte DHCP_LEASE_TIME = 51;
    private static final int DHCP_MAGIC_COOKIE = 1669485411;
    protected static final byte DHCP_MAX_MESSAGE_SIZE = 57;
    protected static final byte DHCP_MESSAGE = 56;
    protected static final byte DHCP_MESSAGE_TYPE = 53;
    protected static final byte DHCP_MESSAGE_TYPE_ACK = 5;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = 4;
    protected static final byte DHCP_MESSAGE_TYPE_DISCOVER = 1;
    protected static final byte DHCP_MESSAGE_TYPE_INFORM = 8;
    protected static final byte DHCP_MESSAGE_TYPE_NAK = 6;
    protected static final byte DHCP_MESSAGE_TYPE_OFFER = 2;
    protected static final byte DHCP_MESSAGE_TYPE_RELEASE = 7;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = 3;
    protected static final byte DHCP_MTU = 26;
    protected static final byte DHCP_OPTION_END = -1;
    protected static final byte DHCP_OPTION_OVERLOAD = 52;
    protected static final byte DHCP_OPTION_PAD = 0;
    protected static final byte DHCP_PARAMETER_LIST = 55;
    protected static final byte DHCP_REBINDING_TIME = 59;
    protected static final byte DHCP_RENEWAL_TIME = 58;
    protected static final byte DHCP_REQUESTED_IP = 50;
    protected static final byte DHCP_ROUTER = 3;
    static final short DHCP_SERVER = 67;
    protected static final byte DHCP_SERVER_IDENTIFIER = 54;
    protected static final byte DHCP_SUBNET_MASK = 1;
    protected static final byte DHCP_VENDOR_CLASS_ID = 60;
    protected static final byte DHCP_VENDOR_INFO = 43;
    public static final int ENCAP_BOOTP = 2;
    public static final int ENCAP_L2 = 0;
    public static final int ENCAP_L3 = 1;
    public static final byte[] ETHER_BROADCAST = {DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END};
    public static final int HWADDR_LEN = 16;
    public static final Inet4Address INADDR_ANY = ((Inet4Address) Inet4Address.ANY);
    public static final Inet4Address INADDR_BROADCAST = ((Inet4Address) Inet4Address.ALL);
    public static final int INFINITE_LEASE = -1;
    private static final int IPV4_MIN_MTU = 68;
    private static final short IP_FLAGS_OFFSET = 16384;
    private static final byte IP_TOS_LOWDELAY = 16;
    private static final byte IP_TTL = 64;
    private static final byte IP_TYPE_UDP = 17;
    private static final byte IP_VERSION_HEADER_LEN = 69;
    protected static final int MAX_LENGTH = 1500;
    private static final int MAX_MTU = 1500;
    public static final int MAX_OPTION_LEN = 255;
    public static final int MINIMUM_LEASE = 60;
    private static final int MIN_MTU = 1280;
    public static final int MIN_PACKET_LENGTH_BOOTP = 236;
    public static final int MIN_PACKET_LENGTH_L2 = 278;
    public static final int MIN_PACKET_LENGTH_L3 = 264;
    private static final byte OPTION_OVERLOAD_BOTH = 3;
    private static final byte OPTION_OVERLOAD_FILE = 1;
    private static final byte OPTION_OVERLOAD_SNAME = 2;
    protected static final String TAG = "DhcpPacket";
    public static final String VENDOR_INFO_ANDROID_METERED = "ANDROID_METERED";
    static String testOverrideHostname = null;
    static String testOverrideVendorId = null;
    protected boolean mBroadcast;
    protected Inet4Address mBroadcastAddress;
    protected byte[] mClientId;
    protected final Inet4Address mClientIp;
    protected final byte[] mClientMac;
    protected List<Inet4Address> mDnsServers;
    protected String mDomainName;
    protected List<Inet4Address> mGateways;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected Short mMaxMessageSize;
    protected String mMessage;
    protected Short mMtu;
    private final Inet4Address mNextIp;
    protected final Inet4Address mRelayIp;
    protected Inet4Address mRequestedIp;
    protected byte[] mRequestedParams;
    protected final short mSecs;
    protected String mServerHostName;
    protected Inet4Address mServerIdentifier;
    protected Inet4Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final int mTransId;
    protected String mVendorId;
    protected String mVendorInfo;
    protected final Inet4Address mYourIp;

    public abstract ByteBuffer buildPacket(int i, short s, short s2);

    /* access modifiers changed from: package-private */
    public abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac, boolean broadcast) {
        this.mTransId = transId;
        this.mSecs = secs;
        this.mClientIp = clientIp;
        this.mYourIp = yourIp;
        this.mNextIp = nextIp;
        this.mRelayIp = relayIp;
        this.mClientMac = clientMac;
        this.mBroadcast = broadcast;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public boolean hasExplicitClientId() {
        return this.mClientId != null;
    }

    public byte[] getExplicitClientIdOrNull() {
        if (hasExplicitClientId()) {
            return getClientId();
        }
        return null;
    }

    public byte[] getClientId() {
        if (hasExplicitClientId()) {
            byte[] bArr = this.mClientId;
            return Arrays.copyOf(bArr, bArr.length);
        }
        byte[] clientId = this.mClientMac;
        byte[] clientId2 = new byte[(clientId.length + 1)];
        clientId2[0] = 1;
        System.arraycopy(clientId, 0, clientId2, 1, clientId.length);
        return clientId2;
    }

    public boolean hasRequestedParam(byte paramId) {
        byte[] bArr = this.mRequestedParams;
        if (bArr == null) {
            return false;
        }
        for (byte reqParam : bArr) {
            if (reqParam == paramId) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void fillInPacket(int encap, Inet4Address destIp, Inet4Address srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode, boolean broadcast) {
        int i = encap;
        ByteBuffer byteBuffer = buf;
        byte[] destIpArray = destIp.getAddress();
        byte[] srcIpArray = srcIp.getAddress();
        int ipHeaderOffset = 0;
        int ipLengthOffset = 0;
        int ipChecksumOffset = 0;
        int endIpHeader = 0;
        int udpHeaderOffset = 0;
        int udpLengthOffset = 0;
        int udpChecksumOffset = 0;
        buf.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        if (i == 0) {
            byteBuffer.put(ETHER_BROADCAST);
            byteBuffer.put(this.mClientMac);
            byteBuffer.putShort((short) OsConstants.ETH_P_IP);
        }
        if (i <= 1) {
            ipHeaderOffset = buf.position();
            byteBuffer.put(IP_VERSION_HEADER_LEN);
            byteBuffer.put((byte) 16);
            ipLengthOffset = buf.position();
            byteBuffer.putShort(0);
            byteBuffer.putShort(0);
            byteBuffer.putShort(IP_FLAGS_OFFSET);
            byteBuffer.put((byte) 64);
            byteBuffer.put(IP_TYPE_UDP);
            ipChecksumOffset = buf.position();
            byteBuffer.putShort(0);
            byteBuffer.put(srcIpArray);
            byteBuffer.put(destIpArray);
            endIpHeader = buf.position();
            udpHeaderOffset = buf.position();
            byteBuffer.putShort(srcUdp);
            byteBuffer.putShort(destUdp);
            udpLengthOffset = buf.position();
            byteBuffer.putShort(0);
            udpChecksumOffset = buf.position();
            byteBuffer.putShort(0);
        } else {
            short s = destUdp;
            short s2 = srcUdp;
        }
        buf.put(requestCode);
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte) this.mClientMac.length);
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.mTransId);
        byteBuffer.putShort(this.mSecs);
        if (broadcast) {
            byteBuffer.putShort(Short.MIN_VALUE);
        } else {
            byteBuffer.putShort(0);
        }
        byteBuffer.put(this.mClientIp.getAddress());
        byteBuffer.put(this.mYourIp.getAddress());
        byteBuffer.put(this.mNextIp.getAddress());
        byteBuffer.put(this.mRelayIp.getAddress());
        byteBuffer.put(this.mClientMac);
        byteBuffer.position(buf.position() + (16 - this.mClientMac.length) + 64 + 128);
        byteBuffer.putInt(DHCP_MAGIC_COOKIE);
        finishPacket(byteBuffer);
        if ((buf.position() & 1) == 1) {
            byteBuffer.put((byte) 0);
        }
        if (i <= 1) {
            short udpLen = (short) (buf.position() - udpHeaderOffset);
            byteBuffer.putShort(udpLengthOffset, udpLen);
            byteBuffer.putShort(udpChecksumOffset, (short) checksum(byteBuffer, 0 + intAbs(byteBuffer.getShort(ipChecksumOffset + 2)) + intAbs(byteBuffer.getShort(ipChecksumOffset + 4)) + intAbs(byteBuffer.getShort(ipChecksumOffset + 6)) + intAbs(byteBuffer.getShort(ipChecksumOffset + 8)) + 17 + udpLen, udpHeaderOffset, buf.position()));
            byteBuffer.putShort(ipLengthOffset, (short) (buf.position() - ipHeaderOffset));
            byteBuffer.putShort(ipChecksumOffset, (short) checksum(byteBuffer, 0, ipHeaderOffset, endIpHeader));
        }
    }

    private static int intAbs(short v) {
        return 65535 & v;
    }

    private int checksum(ByteBuffer buf, int seed, int start, int end) {
        int sum = seed;
        int bufPosition = buf.position();
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();
        buf.position(bufPosition);
        short[] shortArray = new short[((end - start) / 2)];
        shortBuf.get(shortArray);
        for (short s : shortArray) {
            sum += intAbs(s);
        }
        int start2 = start + (shortArray.length * 2);
        if (end != start2) {
            short b = (short) buf.get(start2);
            if (b < 0) {
                b = (short) (b + 256);
            }
            sum += b * 256;
        }
        int sum2 = ((sum >> 16) & 65535) + (sum & 65535);
        return intAbs((short) (~((((sum2 >> 16) & 65535) + sum2) & 65535)));
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte value) {
        buf.put(type);
        buf.put((byte) 1);
        buf.put(value);
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length <= 255) {
            buf.put(type);
            buf.put((byte) payload.length);
            buf.put(payload);
            return;
        }
        throw new IllegalArgumentException("DHCP option too long: " + payload.length + " vs. " + 255);
    }

    protected static void addTlv(ByteBuffer buf, byte type, Inet4Address addr) {
        if (addr != null) {
            addTlv(buf, type, addr.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, List<Inet4Address> addrs) {
        if (addrs != null && addrs.size() != 0) {
            int optionLen = addrs.size() * 4;
            if (optionLen <= 255) {
                buf.put(type);
                buf.put((byte) optionLen);
                for (Inet4Address addr : addrs) {
                    buf.put(addr.getAddress());
                }
                return;
            }
            throw new IllegalArgumentException("DHCP option too long: " + optionLen + " vs. " + 255);
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Short value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 2);
            buf.putShort(value.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Integer value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 4);
            buf.putInt(value.intValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, String str) {
        if (str != null) {
            try {
                addTlv(buf, type, str.getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("String is not US-ASCII: " + str);
            }
        }
    }

    protected static void addTlvEnd(ByteBuffer buf) {
        buf.put(DHCP_OPTION_END);
    }

    private String getVendorId() {
        String str = testOverrideVendorId;
        if (str != null) {
            return str;
        }
        return "android-dhcp-" + Build.VERSION.RELEASE;
    }

    private String getHostname() {
        String str = testOverrideHostname;
        if (str != null) {
            return str;
        }
        return SystemProperties.get("net.hostname");
    }

    /* access modifiers changed from: protected */
    public void addCommonClientTlvs(ByteBuffer buf) {
        addTlv(buf, (byte) DHCP_MAX_MESSAGE_SIZE, (Short) 1500);
        addTlv(buf, (byte) DHCP_VENDOR_CLASS_ID, getVendorId());
        String hn = getHostname();
        if (!TextUtils.isEmpty(hn)) {
            addTlv(buf, (byte) DHCP_HOST_NAME, hn);
        }
    }

    /* access modifiers changed from: protected */
    public void addCommonServerTlvs(ByteBuffer buf) {
        addTlv(buf, (byte) DHCP_LEASE_TIME, this.mLeaseTime);
        Integer num = this.mLeaseTime;
        if (!(num == null || num.intValue() == -1)) {
            addTlv(buf, (byte) DHCP_RENEWAL_TIME, Integer.valueOf((int) (Integer.toUnsignedLong(this.mLeaseTime.intValue()) / 2)));
            addTlv(buf, (byte) DHCP_REBINDING_TIME, Integer.valueOf((int) ((Integer.toUnsignedLong(this.mLeaseTime.intValue()) * 875) / 1000)));
        }
        addTlv(buf, (byte) 1, this.mSubnetMask);
        addTlv(buf, (byte) DHCP_BROADCAST_ADDRESS, this.mBroadcastAddress);
        addTlv(buf, (byte) 3, this.mGateways);
        addTlv(buf, (byte) 6, this.mDnsServers);
        addTlv(buf, (byte) DHCP_DOMAIN_NAME, this.mDomainName);
        addTlv(buf, (byte) DHCP_HOST_NAME, this.mHostName);
        addTlv(buf, (byte) DHCP_VENDOR_INFO, this.mVendorInfo);
        Short sh = this.mMtu;
        if (sh != null && Short.toUnsignedInt(sh.shortValue()) >= 68) {
            addTlv(buf, (byte) DHCP_MTU, this.mMtu);
        }
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
        for (int i = 0; i < mac.length; i++) {
            String hexString = "0" + Integer.toHexString(mac[i]);
            macAddr = macAddr + hexString.substring(hexString.length() - 2);
            if (i != mac.length - 1) {
                macAddr = macAddr + ":";
            }
        }
        return macAddr;
    }

    public String toString() {
        return macToString(this.mClientMac);
    }

    private static Inet4Address readIpAddress(ByteBuffer packet) {
        byte[] ipAddr = new byte[4];
        packet.get(ipAddr);
        try {
            return (Inet4Address) Inet4Address.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer buf, int byteCount, boolean nullOk) {
        byte[] bytes = new byte[byteCount];
        buf.get(bytes);
        int length = bytes.length;
        if (!nullOk) {
            length = 0;
            while (length < bytes.length && bytes[length] != 0) {
                length++;
            }
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    }

    private static boolean isPacketToOrFromClient(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == 68 || udpDstPort == 68;
    }

    private static boolean isPacketServerToServer(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == 67 && udpDstPort == 67;
    }

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int errorCode2, String msg, Object... args) {
            super(String.format(msg, args));
            this.errorCode = errorCode2;
        }
    }

    /* JADX WARNING: type inference failed for: r0v30, types: [com.samsung.android.server.wifi.softap.DhcpPacket] */
    /* JADX WARNING: type inference failed for: r34v12, types: [com.samsung.android.server.wifi.softap.DhcpDiscoverPacket] */
    /* JADX WARNING: type inference failed for: r34v13, types: [com.samsung.android.server.wifi.softap.DhcpOfferPacket] */
    /* JADX WARNING: type inference failed for: r54v4, types: [com.samsung.android.server.wifi.softap.DhcpRequestPacket] */
    /* JADX WARNING: type inference failed for: r54v5, types: [com.samsung.android.server.wifi.softap.DhcpDeclinePacket] */
    /* JADX WARNING: type inference failed for: r34v14, types: [com.samsung.android.server.wifi.softap.DhcpAckPacket] */
    /* JADX WARNING: type inference failed for: r60v3, types: [com.samsung.android.server.wifi.softap.DhcpNakPacket] */
    /* JADX WARNING: type inference failed for: r54v6, types: [com.samsung.android.server.wifi.softap.DhcpReleasePacket] */
    /* JADX WARNING: type inference failed for: r47v3, types: [com.samsung.android.server.wifi.softap.DhcpInformPacket] */
    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x03ff  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x0406 A[SYNTHETIC] */
    @com.android.internal.annotations.VisibleForTesting
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static com.samsung.android.server.wifi.softap.DhcpPacket decodeFullPacket(java.nio.ByteBuffer r75, int r76) throws com.samsung.android.server.wifi.softap.DhcpPacket.ParseException {
        /*
            r1 = r75
            r2 = r76
            r3 = 0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r4 = r0
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r5 = r0
            r6 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            r11 = 0
            r12 = 0
            r13 = 0
            r0 = 0
            r14 = 0
            r15 = 0
            r16 = 0
            r17 = 0
            r18 = 0
            r19 = 0
            r20 = 0
            r21 = 0
            r22 = 0
            r23 = -1
            r24 = r0
            java.nio.ByteOrder r0 = java.nio.ByteOrder.BIG_ENDIAN
            r1.order(r0)
            r26 = r6
            if (r2 != 0) goto L_0x00a3
            int r6 = r75.remaining()
            r0 = 278(0x116, float:3.9E-43)
            if (r6 < r0) goto L_0x007f
            r0 = 6
            byte[] r6 = new byte[r0]
            r30 = r7
            byte[] r7 = new byte[r0]
            r1.get(r6)
            r1.get(r7)
            short r0 = r75.getShort()
            r31 = r6
            int r6 = android.system.OsConstants.ETH_P_IP
            if (r0 != r6) goto L_0x005a
            r33 = r8
            r7 = 1
            goto L_0x00a8
        L_0x005a:
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r6 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r32 = r7
            r7 = 2
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Short r29 = java.lang.Short.valueOf(r0)
            r28 = 0
            r7[r28] = r29
            int r28 = android.system.OsConstants.ETH_P_IP
            java.lang.Integer r28 = java.lang.Integer.valueOf(r28)
            r27 = 1
            r7[r27] = r28
            r27 = r0
            java.lang.String r0 = "Unexpected L2 type 0x%04x, expected 0x%04x"
            r33 = r8
            r8 = 16908288(0x1020000, float:2.387723E-38)
            r6.<init>(r8, r0, r7)
            throw r6
        L_0x007f:
            r30 = r7
            r33 = r8
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r6 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r8 = 2
            java.lang.Object[] r8 = new java.lang.Object[r8]
            int r25 = r75.remaining()
            java.lang.Integer r25 = java.lang.Integer.valueOf(r25)
            r28 = 0
            r8[r28] = r25
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r7 = 1
            r8[r7] = r0
            java.lang.String r0 = "L2 packet too short, %d < %d"
            r7 = 16842752(0x1010000, float:2.3693558E-38)
            r6.<init>(r7, r0, r8)
            throw r6
        L_0x00a3:
            r30 = r7
            r33 = r8
            r7 = 1
        L_0x00a8:
            r6 = 4
            if (r2 > r7) goto L_0x01a5
            int r7 = r75.remaining()
            r8 = 264(0x108, float:3.7E-43)
            if (r7 < r8) goto L_0x0183
            byte r7 = r75.get()
            r8 = r7 & 240(0xf0, float:3.36E-43)
            int r8 = r8 >> r6
            if (r8 != r6) goto L_0x016a
            byte r31 = r75.get()
            short r32 = r75.getShort()
            short r34 = r75.getShort()
            byte r35 = r75.get()
            byte r36 = r75.get()
            byte r37 = r75.get()
            byte r6 = r75.get()
            short r39 = r75.getShort()
            java.net.Inet4Address r24 = readIpAddress(r75)
            java.net.Inet4Address r14 = readIpAddress(r75)
            r0 = 17
            if (r6 != r0) goto L_0x014f
            r0 = r7 & 15
            r40 = 5
            int r0 = r0 + -5
            r41 = 0
            r42 = r7
            r7 = r41
        L_0x00f4:
            if (r7 >= r0) goto L_0x00fc
            r75.getInt()
            int r7 = r7 + 1
            goto L_0x00f4
        L_0x00fc:
            short r7 = r75.getShort()
            r41 = r0
            short r0 = r75.getShort()
            short r43 = r75.getShort()
            short r44 = r75.getShort()
            boolean r45 = isPacketToOrFromClient(r7, r0)
            if (r45 != 0) goto L_0x0145
            boolean r45 = isPacketServerToServer(r7, r0)
            if (r45 == 0) goto L_0x0121
            r45 = r9
            r46 = r10
            r7 = 1
            goto L_0x01a9
        L_0x0121:
            r45 = r9
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r9 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r46 = r10
            r10 = 2
            java.lang.Object[] r10 = new java.lang.Object[r10]
            java.lang.Short r29 = java.lang.Short.valueOf(r7)
            r28 = 0
            r10[r28] = r29
            java.lang.Short r28 = java.lang.Short.valueOf(r0)
            r47 = r7
            r7 = 1
            r10[r7] = r28
            java.lang.String r7 = "Unexpected UDP ports %d->%d"
            r27 = r0
            r0 = 50462720(0x3020000, float:3.8203566E-37)
            r9.<init>(r0, r7, r10)
            throw r9
        L_0x0145:
            r27 = r0
            r47 = r7
            r45 = r9
            r46 = r10
            r7 = 1
            goto L_0x01a9
        L_0x014f:
            r42 = r7
            r45 = r9
            r46 = r10
            r7 = 1
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r9 = 50397184(0x3010000, float:3.7909693E-37)
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Byte r10 = java.lang.Byte.valueOf(r6)
            r25 = 0
            r7[r25] = r10
            java.lang.String r10 = "Protocol not UDP: %d"
            r0.<init>(r9, r10, r7)
            throw r0
        L_0x016a:
            r42 = r7
            r45 = r9
            r7 = 1
            r25 = 0
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r6 = 33685504(0x2020000, float:9.550892E-38)
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Integer r9 = java.lang.Integer.valueOf(r8)
            r7[r25] = r9
            java.lang.String r9 = "Invalid IP version %d"
            r0.<init>(r6, r9, r7)
            throw r0
        L_0x0183:
            r45 = r9
            r25 = 0
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r6 = 33619968(0x2010000, float:9.477423E-38)
            r7 = 2
            java.lang.Object[] r7 = new java.lang.Object[r7]
            int r9 = r75.remaining()
            java.lang.Integer r9 = java.lang.Integer.valueOf(r9)
            r7[r25] = r9
            java.lang.Integer r8 = java.lang.Integer.valueOf(r8)
            r9 = 1
            r7[r9] = r8
            java.lang.String r8 = "L3 packet too short, %d < %d"
            r0.<init>(r6, r8, r7)
            throw r0
        L_0x01a5:
            r45 = r9
            r46 = r10
        L_0x01a9:
            r0 = 236(0xec, float:3.31E-43)
            r6 = 2
            if (r2 > r6) goto L_0x05fb
            int r6 = r75.remaining()
            if (r6 < r0) goto L_0x05fb
            byte r6 = r75.get()
            byte r7 = r75.get()
            byte r0 = r75.get()
            r8 = r0 & 255(0xff, float:3.57E-43)
            byte r9 = r75.get()
            int r10 = r75.getInt()
            short r31 = r75.getShort()
            short r32 = r75.getShort()
            r0 = 32768(0x8000, float:4.5918E-41)
            r0 = r32 & r0
            if (r0 == 0) goto L_0x01dc
            r65 = 1
            goto L_0x01de
        L_0x01dc:
            r65 = 0
        L_0x01de:
            r0 = 4
            byte[] r2 = new byte[r0]
            r1.get(r2)     // Catch:{ UnknownHostException -> 0x05da }
            java.net.InetAddress r0 = java.net.Inet4Address.getByAddress(r2)     // Catch:{ UnknownHostException -> 0x05da }
            r50 = r0
            java.net.Inet4Address r50 = (java.net.Inet4Address) r50     // Catch:{ UnknownHostException -> 0x05da }
            r1.get(r2)     // Catch:{ UnknownHostException -> 0x05da }
            java.net.InetAddress r0 = java.net.Inet4Address.getByAddress(r2)     // Catch:{ UnknownHostException -> 0x05da }
            r51 = r0
            java.net.Inet4Address r51 = (java.net.Inet4Address) r51     // Catch:{ UnknownHostException -> 0x05da }
            r1.get(r2)     // Catch:{ UnknownHostException -> 0x05da }
            java.net.InetAddress r0 = java.net.Inet4Address.getByAddress(r2)     // Catch:{ UnknownHostException -> 0x05da }
            r52 = r0
            java.net.Inet4Address r52 = (java.net.Inet4Address) r52     // Catch:{ UnknownHostException -> 0x05da }
            r1.get(r2)     // Catch:{ UnknownHostException -> 0x05da }
            java.net.InetAddress r0 = java.net.Inet4Address.getByAddress(r2)     // Catch:{ UnknownHostException -> 0x05da }
            r53 = r0
            java.net.Inet4Address r53 = (java.net.Inet4Address) r53     // Catch:{ UnknownHostException -> 0x05da }
            r0 = 16
            if (r8 <= r0) goto L_0x0215
            byte[] r0 = ETHER_BROADCAST
            int r8 = r0.length
        L_0x0215:
            r43 = r6
            byte[] r6 = new byte[r8]
            r1.get(r6)
            int r0 = r75.position()
            r34 = 16
            int r34 = 16 - r8
            int r0 = r0 + r34
            r1.position(r0)
            r0 = 64
            r44 = r7
            r66 = r8
            r7 = 0
            java.lang.String r8 = readAsciiString(r1, r0, r7)
            int r0 = r75.position()
            int r0 = r0 + 128
            r1.position(r0)
            int r0 = r75.remaining()
            r7 = 4
            if (r0 < r7) goto L_0x05c9
            int r7 = r75.getInt()
            r0 = 1669485411(0x63825363, float:4.808171E21)
            if (r7 != r0) goto L_0x05a6
            r0 = 1
            r69 = r2
            r70 = r7
            r71 = r8
            r67 = r9
            r7 = r12
            r8 = r13
            r68 = r14
            r14 = r15
            r9 = r16
            r13 = r18
            r72 = r19
            r2 = r20
            r73 = r21
            r74 = r22
            r12 = r26
            r19 = r30
            r15 = r33
            r18 = r45
            r16 = r46
            r20 = r11
            r11 = r0
        L_0x0274:
            int r0 = r75.position()
            r21 = r9
            int r9 = r75.limit()
            if (r0 >= r9) goto L_0x0477
            if (r11 == 0) goto L_0x0477
            byte r9 = r75.get()
            r0 = -1
            if (r9 != r0) goto L_0x028d
            r0 = 0
            r11 = r0
            goto L_0x0401
        L_0x028d:
            if (r9 != 0) goto L_0x0291
            goto L_0x0401
        L_0x0291:
            byte r0 = r75.get()     // Catch:{ BufferUnderflowException -> 0x045f }
            r0 = r0 & 255(0xff, float:3.57E-43)
            r22 = 0
            r26 = r11
            r11 = 3
            r30 = r13
            r13 = 1
            if (r9 == r13) goto L_0x03f1
            if (r9 == r11) goto L_0x03e1
            r13 = 6
            if (r9 == r13) goto L_0x03d1
            r13 = 12
            if (r9 == r13) goto L_0x03c4
            r13 = 15
            if (r9 == r13) goto L_0x03b7
            r13 = 26
            if (r9 == r13) goto L_0x03a9
            r13 = 28
            if (r9 == r13) goto L_0x039d
            r13 = 43
            if (r9 == r13) goto L_0x038e
            switch(r9) {
                case 50: goto L_0x0380;
                case 51: goto L_0x036f;
                case 52: goto L_0x035e;
                case 53: goto L_0x0350;
                case 54: goto L_0x0343;
                case 55: goto L_0x032f;
                case 56: goto L_0x0320;
                case 57: goto L_0x030e;
                case 58: goto L_0x02fc;
                case 59: goto L_0x02ea;
                case 60: goto L_0x02db;
                case 61: goto L_0x02ce;
                default: goto L_0x02bd;
            }
        L_0x02bd:
            r13 = 0
        L_0x02be:
            if (r13 >= r0) goto L_0x02c8
            int r22 = r22 + 1
            r75.get()     // Catch:{ BufferUnderflowException -> 0x045d }
            int r13 = r13 + 1
            goto L_0x02be
        L_0x02c8:
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x02ce:
            byte[] r13 = new byte[r0]     // Catch:{ BufferUnderflowException -> 0x045d }
            r1.get(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r22 = r0
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x02db:
            r22 = r0
            r13 = 1
            java.lang.String r33 = readAsciiString(r1, r0, r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r18 = r33
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x02ea:
            r22 = 4
            int r13 = r75.getInt()     // Catch:{ BufferUnderflowException -> 0x045d }
            java.lang.Integer r13 = java.lang.Integer.valueOf(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r74 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x02fc:
            r22 = 4
            int r13 = r75.getInt()     // Catch:{ BufferUnderflowException -> 0x045d }
            java.lang.Integer r13 = java.lang.Integer.valueOf(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r73 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x030e:
            r22 = 2
            short r13 = r75.getShort()     // Catch:{ BufferUnderflowException -> 0x045d }
            java.lang.Short r13 = java.lang.Short.valueOf(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r72 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x0320:
            r22 = r0
            r13 = 0
            java.lang.String r33 = readAsciiString(r1, r0, r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            r15 = r33
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x032f:
            byte[] r13 = new byte[r0]     // Catch:{ BufferUnderflowException -> 0x045d }
            r1.get(r13)     // Catch:{ BufferUnderflowException -> 0x033e }
            r22 = r0
            r20 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x033e:
            r0 = move-exception
            r20 = r13
            goto L_0x0464
        L_0x0343:
            java.net.Inet4Address r13 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r12 = r13
            r22 = 4
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x0350:
            byte r13 = r75.get()     // Catch:{ BufferUnderflowException -> 0x045d }
            r23 = r13
            r22 = 1
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x035e:
            r22 = 1
            byte r13 = r75.get()     // Catch:{ BufferUnderflowException -> 0x045d }
            r11 = r13 & 3
            byte r11 = (byte) r11     // Catch:{ BufferUnderflowException -> 0x045d }
            r17 = r11
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x036f:
            int r11 = r75.getInt()     // Catch:{ BufferUnderflowException -> 0x045d }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)     // Catch:{ BufferUnderflowException -> 0x045d }
            r2 = r11
            r22 = 4
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x0380:
            java.net.Inet4Address r11 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r22 = 4
            r21 = r11
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x038e:
            r22 = r0
            r11 = 1
            java.lang.String r13 = readAsciiString(r1, r0, r11)     // Catch:{ BufferUnderflowException -> 0x045d }
            r16 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x039d:
            java.net.Inet4Address r11 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r14 = r11
            r22 = 4
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x03a9:
            r22 = 2
            short r11 = r75.getShort()     // Catch:{ BufferUnderflowException -> 0x045d }
            java.lang.Short r11 = java.lang.Short.valueOf(r11)     // Catch:{ BufferUnderflowException -> 0x045d }
            r13 = r11
            r11 = r22
            goto L_0x03fd
        L_0x03b7:
            r22 = r0
            r11 = 0
            java.lang.String r13 = readAsciiString(r1, r0, r11)     // Catch:{ BufferUnderflowException -> 0x045d }
            r8 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x03c4:
            r11 = 0
            r22 = r0
            java.lang.String r13 = readAsciiString(r1, r0, r11)     // Catch:{ BufferUnderflowException -> 0x045d }
            r7 = r13
            r11 = r22
            r13 = r30
            goto L_0x03fd
        L_0x03d1:
            r11 = 0
        L_0x03d2:
            if (r11 >= r0) goto L_0x03de
            java.net.Inet4Address r13 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r4.add(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            int r11 = r11 + 4
            goto L_0x03d2
        L_0x03de:
            r13 = r30
            goto L_0x03fd
        L_0x03e1:
            r11 = 0
        L_0x03e2:
            if (r11 >= r0) goto L_0x03ee
            java.net.Inet4Address r13 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r5.add(r13)     // Catch:{ BufferUnderflowException -> 0x045d }
            int r11 = r11 + 4
            goto L_0x03e2
        L_0x03ee:
            r13 = r30
            goto L_0x03fd
        L_0x03f1:
            java.net.Inet4Address r11 = readIpAddress(r75)     // Catch:{ BufferUnderflowException -> 0x045d }
            r19 = r11
            r22 = 4
            r11 = r22
            r13 = r30
        L_0x03fd:
            if (r11 != r0) goto L_0x0406
            r11 = r26
        L_0x0401:
            r9 = r21
            goto L_0x0274
        L_0x0406:
            r1 = 67305472(0x4030000, float:1.5398976E-36)
            int r1 = android.net.metrics.DhcpErrorEvent.errorCodeWithOption(r1, r9)     // Catch:{ BufferUnderflowException -> 0x0453 }
            r22 = r2
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r2 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException     // Catch:{ BufferUnderflowException -> 0x0449 }
            r25 = r7
            java.lang.String r7 = "Invalid length %d for option %d, expected %d"
            r30 = r8
            r8 = 3
            java.lang.Object[] r8 = new java.lang.Object[r8]     // Catch:{ BufferUnderflowException -> 0x0435 }
            java.lang.Integer r33 = java.lang.Integer.valueOf(r0)     // Catch:{ BufferUnderflowException -> 0x0435 }
            r28 = 0
            r8[r28] = r33     // Catch:{ BufferUnderflowException -> 0x0435 }
            java.lang.Byte r33 = java.lang.Byte.valueOf(r9)     // Catch:{ BufferUnderflowException -> 0x0435 }
            r27 = 1
            r8[r27] = r33     // Catch:{ BufferUnderflowException -> 0x0435 }
            java.lang.Integer r27 = java.lang.Integer.valueOf(r11)     // Catch:{ BufferUnderflowException -> 0x0435 }
            r29 = 2
            r8[r29] = r27     // Catch:{ BufferUnderflowException -> 0x0435 }
            r2.<init>(r1, r7, r8)     // Catch:{ BufferUnderflowException -> 0x0435 }
            throw r2     // Catch:{ BufferUnderflowException -> 0x0435 }
        L_0x0435:
            r0 = move-exception
            r2 = r22
            r7 = r25
            r8 = r30
            r30 = r13
            goto L_0x0464
        L_0x043f:
            r0 = move-exception
            r30 = r8
            r2 = r22
            r7 = r25
            r30 = r13
            goto L_0x0464
        L_0x0449:
            r0 = move-exception
            r25 = r7
            r30 = r8
            r2 = r22
            r30 = r13
            goto L_0x0464
        L_0x0453:
            r0 = move-exception
            r22 = r2
            r25 = r7
            r30 = r8
            r30 = r13
            goto L_0x0464
        L_0x045d:
            r0 = move-exception
            goto L_0x0464
        L_0x045f:
            r0 = move-exception
            r26 = r11
            r30 = r13
        L_0x0464:
            r1 = 83951616(0x5010000, float:6.065551E-36)
            int r1 = android.net.metrics.DhcpErrorEvent.errorCodeWithOption(r1, r9)
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r11 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r13 = 0
            java.lang.Object[] r13 = new java.lang.Object[r13]
            r22 = r0
            java.lang.String r0 = "BufferUnderflowException"
            r11.<init>(r1, r0, r13)
            throw r11
        L_0x0477:
            r26 = r11
            r30 = r13
            switch(r23) {
                case -1: goto L_0x0595;
                case 0: goto L_0x047e;
                case 1: goto L_0x0531;
                case 2: goto L_0x0519;
                case 3: goto L_0x0505;
                case 4: goto L_0x04ef;
                case 5: goto L_0x04d7;
                case 6: goto L_0x04c6;
                case 7: goto L_0x04a5;
                case 8: goto L_0x0496;
                default: goto L_0x047e;
            }
        L_0x047e:
            r28 = r2
            r34 = r3
            r1 = 0
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r2 = 67436544(0x4050000, float:1.5634075E-36)
            r3 = 1
            java.lang.Object[] r3 = new java.lang.Object[r3]
            java.lang.Byte r25 = java.lang.Byte.valueOf(r23)
            r3[r1] = r25
            java.lang.String r1 = "Unimplemented DHCP type %d"
            r0.<init>(r2, r1, r3)
            throw r0
        L_0x0496:
            com.samsung.android.server.wifi.softap.DhcpInformPacket r0 = new com.samsung.android.server.wifi.softap.DhcpInformPacket
            r47 = r0
            r48 = r10
            r49 = r31
            r54 = r6
            r47.<init>(r48, r49, r50, r51, r52, r53, r54)
            goto L_0x0545
        L_0x04a5:
            if (r12 == 0) goto L_0x04ba
            com.samsung.android.server.wifi.softap.DhcpReleasePacket r0 = new com.samsung.android.server.wifi.softap.DhcpReleasePacket
            r54 = r0
            r55 = r10
            r56 = r12
            r57 = r50
            r58 = r53
            r59 = r6
            r54.<init>(r55, r56, r57, r58, r59)
            goto L_0x0545
        L_0x04ba:
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r1 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]
            java.lang.String r9 = "DHCPRELEASE without server identifier"
            r11 = 5
            r0.<init>(r11, r9, r1)
            throw r0
        L_0x04c6:
            com.samsung.android.server.wifi.softap.DhcpNakPacket r0 = new com.samsung.android.server.wifi.softap.DhcpNakPacket
            r60 = r0
            r61 = r10
            r62 = r31
            r63 = r53
            r64 = r6
            r60.<init>(r61, r62, r63, r64, r65)
            goto L_0x0545
        L_0x04d7:
            com.samsung.android.server.wifi.softap.DhcpAckPacket r0 = new com.samsung.android.server.wifi.softap.DhcpAckPacket
            r34 = r0
            r35 = r10
            r36 = r31
            r37 = r65
            r38 = r24
            r39 = r53
            r40 = r50
            r41 = r51
            r42 = r6
            r34.<init>(r35, r36, r37, r38, r39, r40, r41, r42)
            goto L_0x0545
        L_0x04ef:
            com.samsung.android.server.wifi.softap.DhcpDeclinePacket r0 = new com.samsung.android.server.wifi.softap.DhcpDeclinePacket
            r54 = r0
            r55 = r10
            r56 = r31
            r57 = r50
            r58 = r51
            r59 = r52
            r60 = r53
            r61 = r6
            r54.<init>(r55, r56, r57, r58, r59, r60, r61)
            goto L_0x0545
        L_0x0505:
            com.samsung.android.server.wifi.softap.DhcpRequestPacket r0 = new com.samsung.android.server.wifi.softap.DhcpRequestPacket
            r54 = r0
            r55 = r10
            r56 = r31
            r57 = r50
            r58 = r53
            r59 = r6
            r60 = r65
            r54.<init>(r55, r56, r57, r58, r59, r60)
            goto L_0x0545
        L_0x0519:
            com.samsung.android.server.wifi.softap.DhcpOfferPacket r0 = new com.samsung.android.server.wifi.softap.DhcpOfferPacket
            r34 = r0
            r35 = r10
            r36 = r31
            r37 = r65
            r38 = r24
            r39 = r53
            r40 = r50
            r41 = r51
            r42 = r6
            r34.<init>(r35, r36, r37, r38, r39, r40, r41, r42)
            goto L_0x0545
        L_0x0531:
            com.samsung.android.server.wifi.softap.DhcpDiscoverPacket r0 = new com.samsung.android.server.wifi.softap.DhcpDiscoverPacket
            r34 = r0
            r35 = r10
            r36 = r31
            r37 = r53
            r38 = r6
            r39 = r65
            r40 = r24
            r34.<init>(r35, r36, r37, r38, r39, r40)
        L_0x0545:
            r0.mBroadcastAddress = r14
            r0.mClientId = r3
            r0.mDnsServers = r4
            r0.mDomainName = r8
            r0.mGateways = r5
            r0.mHostName = r7
            r0.mLeaseTime = r2
            r0.mMessage = r15
            r13 = r30
            r0.mMtu = r13
            r1 = r21
            r0.mRequestedIp = r1
            r11 = r20
            r0.mRequestedParams = r11
            r0.mServerIdentifier = r12
            r9 = r19
            r0.mSubnetMask = r9
            r1 = r72
            r0.mMaxMessageSize = r1
            r19 = r1
            r1 = r73
            r0.mT1 = r1
            r20 = r1
            r1 = r74
            r0.mT2 = r1
            r22 = r1
            r1 = r18
            r0.mVendorId = r1
            r1 = r16
            r0.mVendorInfo = r1
            r16 = r17 & 2
            if (r16 != 0) goto L_0x058c
            r16 = r1
            r1 = r71
            r0.mServerHostName = r1
            goto L_0x0594
        L_0x058c:
            r16 = r1
            r1 = r71
            java.lang.String r1 = ""
            r0.mServerHostName = r1
        L_0x0594:
            return r0
        L_0x0595:
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r1 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]
            r28 = r2
            java.lang.String r2 = "No DHCP message type option"
            r34 = r3
            r3 = 67371008(0x4040000, float:1.5516525E-36)
            r0.<init>(r3, r2, r1)
            throw r0
        L_0x05a6:
            r69 = r2
            r34 = r3
            r70 = r7
            r71 = r8
            r1 = 0
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r2 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r3 = 67239936(0x4020000, float:1.5281427E-36)
            r7 = 2
            java.lang.Object[] r7 = new java.lang.Object[r7]
            java.lang.Integer r8 = java.lang.Integer.valueOf(r70)
            r7[r1] = r8
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r1 = 1
            r7[r1] = r0
            java.lang.String r0 = "Bad magic cookie 0x%08x, should be 0x%08x"
            r2.<init>(r3, r0, r7)
            throw r2
        L_0x05c9:
            r69 = r2
            r34 = r3
            r1 = 0
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r0 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r2 = 67502080(0x4060000, float:1.5751624E-36)
            java.lang.Object[] r1 = new java.lang.Object[r1]
            java.lang.String r3 = "not a DHCP message"
            r0.<init>(r2, r3, r1)
            throw r0
        L_0x05da:
            r0 = move-exception
            r69 = r2
            r34 = r3
            r43 = r6
            r44 = r7
            r67 = r9
            r68 = r14
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r1 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r2 = 33751040(0x2030000, float:9.62436E-38)
            r3 = 1
            java.lang.Object[] r3 = new java.lang.Object[r3]
            java.lang.String r6 = java.util.Arrays.toString(r69)
            r7 = 0
            r3[r7] = r6
            java.lang.String r6 = "Invalid IPv4 address: %s"
            r1.<init>(r2, r6, r3)
            throw r1
        L_0x05fb:
            r34 = r3
            r68 = r14
            com.samsung.android.server.wifi.softap.DhcpPacket$ParseException r1 = new com.samsung.android.server.wifi.softap.DhcpPacket$ParseException
            r2 = 67174400(0x4010000, float:1.5163877E-36)
            r3 = 2
            java.lang.Object[] r3 = new java.lang.Object[r3]
            int r6 = r75.remaining()
            java.lang.Integer r6 = java.lang.Integer.valueOf(r6)
            r7 = 0
            r3[r7] = r6
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r6 = 1
            r3[r6] = r0
            java.lang.String r0 = "Invalid type or BOOTP packet too short, %d < %d"
            r1.<init>(r2, r0, r3)
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.samsung.android.server.wifi.softap.DhcpPacket.decodeFullPacket(java.nio.ByteBuffer, int):com.samsung.android.server.wifi.softap.DhcpPacket");
    }

    public static DhcpPacket decodeFullPacket(byte[] packet, int length, int pktType) throws ParseException {
        try {
            return decodeFullPacket(ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN), pktType);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e2) {
            throw new ParseException(84082688, e2.getMessage(), new Object[0]);
        }
    }

    public DhcpResults toDhcpResults() {
        int prefixLength;
        Inet4Address ipAddress = this.mYourIp;
        if (ipAddress.equals((Inet4Address) Inet4Address.ANY)) {
            ipAddress = this.mClientIp;
            if (ipAddress.equals((Inet4Address) Inet4Address.ANY)) {
                return null;
            }
        }
        Inet4Address inet4Address = this.mSubnetMask;
        if (inet4Address != null) {
            try {
                prefixLength = Inet4AddressUtils.netmaskToPrefixLength(inet4Address);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            prefixLength = Inet4AddressUtils.getImplicitNetmask(ipAddress);
        }
        DhcpResults results = new DhcpResults();
        try {
            results.ipAddress = new LinkAddress(ipAddress, prefixLength);
            short s = 0;
            if (this.mGateways.size() > 0) {
                results.gateway = this.mGateways.get(0);
            }
            results.dnsServers.addAll(this.mDnsServers);
            results.domains = this.mDomainName;
            results.serverAddress = this.mServerIdentifier;
            results.vendorInfo = this.mVendorInfo;
            Integer num = this.mLeaseTime;
            results.leaseDuration = num != null ? num.intValue() : -1;
            Short sh = this.mMtu;
            if (sh != null && 1280 <= sh.shortValue() && this.mMtu.shortValue() <= 1500) {
                s = this.mMtu.shortValue();
            }
            results.mtu = s;
            results.serverHostName = this.mServerHostName;
            return results;
        } catch (IllegalArgumentException e2) {
            return null;
        }
    }

    public long getLeaseTimeMillis() {
        Integer num = this.mLeaseTime;
        if (num == null || num.intValue() == -1) {
            return 0;
        }
        if (this.mLeaseTime.intValue() < 0 || this.mLeaseTime.intValue() >= 60) {
            return (((long) this.mLeaseTime.intValue()) & Constants.INT_MASK) * 1000;
        }
        return 60000;
    }

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, short secs, byte[] clientMac, boolean broadcast, byte[] expectedParams) {
        Inet4Address inet4Address = INADDR_ANY;
        DhcpPacket pkt = new DhcpDiscoverPacket(transactionId, secs, inet4Address, clientMac, broadcast, inet4Address);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildOfferPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address relayIp, Inet4Address yourIp, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName, String hostname, boolean metered, short mtu) {
        DhcpPacket pkt = new DhcpOfferPacket(transactionId, 0, broadcast, serverIpAddr, relayIp, INADDR_ANY, yourIp, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mHostName = hostname;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mSubnetMask = netMask;
        pkt.mBroadcastAddress = bcAddr;
        pkt.mMtu = Short.valueOf(mtu);
        if (metered) {
            pkt.mVendorInfo = VENDOR_INFO_ANDROID_METERED;
        }
        int i = encap;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address relayIp, Inet4Address yourIp, Inet4Address requestClientIp, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName, String hostname, boolean metered, short mtu) {
        DhcpPacket pkt = new DhcpAckPacket(transactionId, 0, broadcast, serverIpAddr, relayIp, requestClientIp, yourIp, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mHostName = hostname;
        pkt.mSubnetMask = netMask;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mBroadcastAddress = bcAddr;
        pkt.mMtu = Short.valueOf(mtu);
        if (metered) {
            pkt.mVendorInfo = VENDOR_INFO_ANDROID_METERED;
        }
        int i = encap;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int encap, int transactionId, Inet4Address serverIpAddr, Inet4Address relayIp, byte[] mac, boolean broadcast, String message) {
        DhcpPacket pkt = new DhcpNakPacket(transactionId, 0, relayIp, mac, broadcast);
        pkt.mMessage = message;
        pkt.mServerIdentifier = serverIpAddr;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int encap, int transactionId, short secs, Inet4Address clientIp, boolean broadcast, byte[] clientMac, Inet4Address requestedIpAddress, Inet4Address serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpRequestPacket(transactionId, secs, clientIp, INADDR_ANY, clientMac, broadcast);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        int i = encap;
        return pkt.buildPacket(encap, DHCP_SERVER, DHCP_CLIENT);
    }
}
