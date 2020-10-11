package com.samsung.android.server.wifi.softap;

import android.net.MacAddress;
import android.system.OsConstants;
import com.samsung.android.server.wifi.softap.DhcpPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;

public class SemConnectivityPacketSummary {
    public static final int ARP_HWTYPE_ETHER = 1;
    public static final int ARP_PAYLOAD_LEN = 28;
    public static final int ARP_REPLY = 2;
    public static final int ARP_REQUEST = 1;
    public static final int DHCP4_CLIENT_PORT = 68;
    public static final int ETHER_ADDR_LEN = 6;
    public static final int ETHER_DST_ADDR_OFFSET = 0;
    public static final int ETHER_HEADER_LEN = 14;
    public static final int ETHER_SRC_ADDR_OFFSET = 6;
    public static final int ETHER_TYPE_ARP = 2054;
    public static final int ETHER_TYPE_IPV4 = 2048;
    public static final int ETHER_TYPE_IPV6 = 34525;
    public static final int ETHER_TYPE_LENGTH = 2;
    public static final int ETHER_TYPE_OFFSET = 12;
    public static final int ICMPV6_HEADER_MIN_LEN = 4;
    public static final int ICMPV6_ND_OPTION_LENGTH_SCALING_FACTOR = 8;
    public static final int ICMPV6_ND_OPTION_MIN_LENGTH = 8;
    public static final int ICMPV6_ND_OPTION_MTU = 5;
    public static final int ICMPV6_ND_OPTION_SLLA = 1;
    public static final int ICMPV6_ND_OPTION_TLLA = 2;
    public static final int ICMPV6_NEIGHBOR_ADVERTISEMENT = 136;
    public static final int ICMPV6_NEIGHBOR_SOLICITATION = 135;
    public static final int ICMPV6_ROUTER_ADVERTISEMENT = 134;
    public static final int ICMPV6_ROUTER_SOLICITATION = 133;
    public static final int IPV4_ADDR_LEN = 4;
    public static final int IPV4_DST_ADDR_OFFSET = 16;
    public static final int IPV4_FLAGS_OFFSET = 6;
    public static final int IPV4_FRAGMENT_MASK = 8191;
    public static final int IPV4_HEADER_MIN_LEN = 20;
    public static final int IPV4_IHL_MASK = 15;
    public static final int IPV4_PROTOCOL_OFFSET = 9;
    public static final int IPV4_SRC_ADDR_OFFSET = 12;
    public static final int IPV6_HEADER_LEN = 40;
    public static final int IPV6_PROTOCOL_OFFSET = 6;
    public static final int IPV6_SRC_ADDR_OFFSET = 8;
    private static final String TAG = "SemConnectivityPacketSummary";
    public static final int UDP_HEADER_LEN = 8;
    private static int dnsInfoField = 2;
    private static int dnsPort = 53;
    private static final int dnsTran = 6;
    private static int mTypeFieldLength = 7;
    private static int positionDnsQueriesName = 16;
    private static int positionDnsQueriesResponse = 17;
    private static int startPositionOfDns = 42;
    private boolean isRefused;
    private final byte[] mBytes;
    private final byte[] mHwAddr;
    private final int mLength;
    private final ByteBuffer mPacket = ByteBuffer.wrap(this.mBytes, 0, this.mLength);
    private final String mSummary;

    public static String asString(int i) {
        return Integer.toString(i);
    }

    public static int asUint(byte b) {
        return b & 255;
    }

    public static int asUint(short s) {
        return 65535 & s;
    }

    public static String summarize(MacAddress hwaddr, byte[] buffer) {
        return summarize(hwaddr, buffer, buffer.length);
    }

    public static String summarize(MacAddress macAddr, byte[] buffer, int length) {
        if (macAddr == null || buffer == null) {
            return null;
        }
        return new SemConnectivityPacketSummary(macAddr, buffer, Math.min(length, buffer.length)).toString();
    }

    private SemConnectivityPacketSummary(MacAddress macAddr, byte[] buffer, int length) {
        this.mHwAddr = macAddr.toByteArray();
        this.mBytes = buffer;
        this.mLength = Math.min(length, this.mBytes.length);
        this.mPacket.order(ByteOrder.BIG_ENDIAN);
        StringJoiner sj = new StringJoiner(" ");
        parseEther(sj);
        this.mSummary = sj.toString();
    }

    public String toString() {
        return this.mSummary;
    }

    private void parseEther(StringJoiner sj) {
        if (this.mPacket.remaining() < 14) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        this.mPacket.position(6);
        ByteBuffer srcMac = (ByteBuffer) this.mPacket.slice().limit(6);
        sj.add(ByteBuffer.wrap(this.mHwAddr).equals(srcMac) ? "TX" : "RX");
        sj.add(getMacAddressString(srcMac));
        this.mPacket.position(0);
        sj.add(">").add(getMacAddressString((ByteBuffer) this.mPacket.slice().limit(6)));
        this.mPacket.position(12);
        int etherType = asUint(this.mPacket.getShort());
        if (etherType == 2048) {
            sj.add("ipv4");
            parseIPv4(sj);
        } else if (etherType == 2054) {
            sj.add("arp");
            parseARP(sj);
        } else if (etherType != 34525) {
            sj.add("ethtype").add(asString(etherType));
        } else {
            sj.add("ipv6");
            parseIPv6(sj);
        }
    }

    private void parseARP(StringJoiner sj) {
        if (this.mPacket.remaining() < 28) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
        } else if (asUint(this.mPacket.getShort()) == 1 && asUint(this.mPacket.getShort()) == 2048 && asUint(this.mPacket.get()) == 6 && asUint(this.mPacket.get()) == 4) {
            int opCode = asUint(this.mPacket.getShort());
            String senderHwAddr = getMacAddressString(this.mPacket);
            String senderIPv4 = getIPv4AddressString(this.mPacket);
            getMacAddressString(this.mPacket);
            String targetIPv4 = getIPv4AddressString(this.mPacket);
            if (opCode == 1) {
                sj.add("who-has").add(targetIPv4);
            } else if (opCode == 2) {
                sj.add("reply").add(senderIPv4).add(senderHwAddr);
            } else {
                sj.add("unknown opcode").add(asString(opCode));
            }
        } else {
            sj.add("unexpected header");
        }
    }

    private void parseIPv4(StringJoiner sj) {
        if (!this.mPacket.hasRemaining()) {
            sj.add("runt");
            return;
        }
        int startOfIpLayer = this.mPacket.position();
        int ipv4HeaderLength = (this.mPacket.get(startOfIpLayer) & 15) * 4;
        if (this.mPacket.remaining() < ipv4HeaderLength || this.mPacket.remaining() < 20) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int startOfTransportLayer = startOfIpLayer + ipv4HeaderLength;
        this.mPacket.position(startOfIpLayer + 6);
        boolean isFragment = (asUint(this.mPacket.getShort()) & 8191) != 0;
        this.mPacket.position(startOfIpLayer + 9);
        int protocol = asUint(this.mPacket.get());
        this.mPacket.position(startOfIpLayer + 12);
        String srcAddr = getIPv4AddressString(this.mPacket);
        this.mPacket.position(startOfIpLayer + 16);
        sj.add(srcAddr).add(">").add(getIPv4AddressString(this.mPacket));
        this.mPacket.position(startOfTransportLayer);
        if (protocol == OsConstants.IPPROTO_UDP) {
            sj.add("udp");
            if (isFragment) {
                sj.add("fragment");
            } else {
                parseUDP(sj);
            }
        } else {
            sj.add("proto").add(asString(protocol));
            if (isFragment) {
                sj.add("fragment");
            }
        }
    }

    private void parseIPv6(StringJoiner sj) {
        if (this.mPacket.remaining() < 40) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int startOfIpLayer = this.mPacket.position();
        this.mPacket.position(startOfIpLayer + 6);
        int protocol = asUint(this.mPacket.get());
        this.mPacket.position(startOfIpLayer + 8);
        String srcAddr = getIPv6AddressString(this.mPacket);
        sj.add(srcAddr).add(">").add(getIPv6AddressString(this.mPacket));
        this.mPacket.position(startOfIpLayer + 40);
        if (protocol == OsConstants.IPPROTO_ICMPV6) {
            sj.add("icmp6");
            parseICMPv6(sj);
            return;
        }
        sj.add("proto").add(asString(protocol));
    }

    private void parseICMPv6(StringJoiner sj) {
        if (this.mPacket.remaining() < 4) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int icmp6Type = asUint(this.mPacket.get());
        int icmp6Code = asUint(this.mPacket.get());
        this.mPacket.getShort();
        switch (icmp6Type) {
            case 133:
                sj.add("rs");
                parseICMPv6RouterSolicitation(sj);
                return;
            case 134:
                sj.add("ra");
                parseICMPv6RouterAdvertisement(sj);
                return;
            case 135:
                sj.add("ns");
                parseICMPv6NeighborMessage(sj);
                return;
            case 136:
                sj.add("na");
                parseICMPv6NeighborMessage(sj);
                return;
            default:
                sj.add("type").add(asString(icmp6Type));
                sj.add("code").add(asString(icmp6Code));
                return;
        }
    }

    private void parseICMPv6RouterSolicitation(StringJoiner sj) {
        if (this.mPacket.remaining() < 4) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 4);
        parseICMPv6NeighborDiscoveryOptions(sj);
    }

    private void parseICMPv6RouterAdvertisement(StringJoiner sj) {
        if (this.mPacket.remaining() < 12) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 12);
        parseICMPv6NeighborDiscoveryOptions(sj);
    }

    private void parseICMPv6NeighborMessage(StringJoiner sj) {
        if (this.mPacket.remaining() < 20) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 4);
        sj.add(getIPv6AddressString(this.mPacket));
        parseICMPv6NeighborDiscoveryOptions(sj);
    }

    private void parseICMPv6NeighborDiscoveryOptions(StringJoiner sj) {
        while (this.mPacket.remaining() >= 8) {
            int ndType = asUint(this.mPacket.get());
            int ndBytes = (asUint(this.mPacket.get()) * 8) - 2;
            if (ndBytes < 0 || ndBytes > this.mPacket.remaining()) {
                sj.add("<malformed>");
                return;
            }
            int position = this.mPacket.position();
            if (ndType == 1) {
                sj.add("slla");
                sj.add(getMacAddressString(this.mPacket));
            } else if (ndType == 2) {
                sj.add("tlla");
                sj.add(getMacAddressString(this.mPacket));
            } else if (ndType == 5) {
                sj.add("mtu");
                short s = this.mPacket.getShort();
                sj.add(asString(this.mPacket.getInt()));
            }
            this.mPacket.position(position + ndBytes);
        }
    }

    private void parseUDP(StringJoiner sj) {
        if (this.mPacket.remaining() < 8) {
            sj.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int previous = this.mPacket.position();
        int srcPort = asUint(this.mPacket.getShort());
        int dstPort = asUint(this.mPacket.getShort());
        sj.add(asString(srcPort)).add(">").add(asString(dstPort));
        this.mPacket.position(previous + 8);
        if (srcPort == 68 || dstPort == 68) {
            sj.add("dhcp4");
            parseDHCPv4(sj);
        }
        int i = dnsPort;
        if (srcPort == i || dstPort == i) {
            sj.add("dns");
            if (dstPort == dnsPort) {
                sj.add("Queries");
            } else {
                sj.add("Responses");
            }
            this.isRefused = false;
            parseDns(sj);
        }
    }

    private void parseDHCPv4(StringJoiner sj) {
        try {
            sj.add(DhcpPacket.decodeFullPacket(this.mBytes, this.mLength, 0).toString());
        } catch (DhcpPacket.ParseException e) {
            sj.add("parse error: " + e);
        }
    }

    private void parseDns(StringJoiner sj) {
        int previous = this.mPacket.position();
        sj.add("tid");
        sj.add(getDnsDataString((ByteBuffer) this.mPacket.slice().limit(dnsInfoField)));
        this.mPacket.position(dnsInfoField + previous);
        sj.add("flags");
        sj.add(getDnsDataString((ByteBuffer) this.mPacket.slice().limit(dnsInfoField)));
    }

    private static String getIPv4AddressString(ByteBuffer ipv4) {
        return getIpAddressString(ipv4, 4);
    }

    private static String getIPv6AddressString(ByteBuffer ipv6) {
        return getIpAddressString(ipv6, 16);
    }

    private static String getIpAddressString(ByteBuffer ip, int byteLength) {
        if (ip == null || ip.remaining() < byteLength) {
            return "invalid";
        }
        byte[] bytes = new byte[byteLength];
        ip.get(bytes, 0, byteLength);
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private static String getMacAddressString(ByteBuffer mac) {
        if (mac == null || mac.remaining() < 6) {
            return "invalid";
        }
        byte[] bytes = new byte[6];
        int i = 0;
        mac.get(bytes, 0, bytes.length);
        Object[] printableBytes = new Object[bytes.length];
        int i2 = 0;
        int length = bytes.length;
        while (i < length) {
            printableBytes[i2] = new Byte(bytes[i]);
            i++;
            i2++;
        }
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x", printableBytes);
    }

    private void getDnsQueriesName(int position, StringJoiner sj) {
        int i = 0;
        do {
            this.mPacket.position(position);
            String dnsQueryName = getDnsQueriesLenthOrTypeField();
            if (dnsQueryName != null && !dnsQueryName.isEmpty() && dnsQueryName.equals("00")) {
                return;
            }
            if (dnsQueryName == null || dnsQueryName.isEmpty() || !dnsQueryName.equals("c0")) {
                try {
                    int dnsQueryNamelength = Integer.parseInt(dnsQueryName, positionDnsQueriesName);
                    int position2 = position + 1;
                    this.mPacket.position(position2);
                    byte[] bytes1 = new byte[dnsQueryNamelength];
                    ((ByteBuffer) this.mPacket.slice().limit(dnsQueryNamelength)).get(bytes1, 0, bytes1.length);
                    sj.add(new String(bytes1));
                    position = position2 + dnsQueryNamelength;
                    i++;
                } catch (NumberFormatException e) {
                    return;
                }
            } else {
                this.mPacket.position(position + 1);
                try {
                    getDnsQueriesName(startPositionOfDns + Integer.parseInt(getDnsQueriesLenthOrTypeField(), positionDnsQueriesName), sj);
                    return;
                } catch (NumberFormatException e2) {
                    return;
                }
            }
        } while (i < 15);
    }

    private static String getDnsDataString(ByteBuffer tid) {
        if (tid == null || tid.remaining() < 2) {
            return "invalid";
        }
        byte[] bytes = new byte[2];
        int i = 0;
        tid.get(bytes, 0, bytes.length);
        Object[] printableBytes = new Object[bytes.length];
        int i2 = 0;
        int length = bytes.length;
        while (i < length) {
            printableBytes[i2] = new Byte(bytes[i]);
            i++;
            i2++;
        }
        return String.format("%02x%02x", printableBytes);
    }

    private static String getDnsQueriesString(ByteBuffer tid) {
        if (tid == null || tid.remaining() < 12) {
            return "invalid";
        }
        byte[] bytes = new byte[12];
        tid.get(bytes, 0, bytes.length);
        String queries = new String(bytes);
        if (!queries.isEmpty()) {
            return queries;
        }
        return "error";
    }

    private String getDnsQueriesLenthOrTypeField() {
        int j = 0;
        byte[] bytes = new byte[1];
        Object[] printableBytes = new Object[bytes.length];
        int i = 0;
        ((ByteBuffer) this.mPacket.slice().limit(1)).get(bytes, 0, bytes.length);
        int length = bytes.length;
        while (i < length) {
            printableBytes[j] = new Byte(bytes[i]);
            i++;
            j++;
        }
        return String.format("%02x", printableBytes);
    }

    private void getDnsIpOfAnswer(int position, StringJoiner sj) {
        int i = 0;
        do {
            this.mPacket.position(position);
            sj.add(String.valueOf(Integer.parseInt(getDnsQueriesLenthOrTypeField(), positionDnsQueriesName)));
            position++;
            i++;
        } while (i < 4);
    }
}
