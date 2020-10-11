package com.samsung.android.server.wifi.softap;

import com.samsung.android.server.wifi.mobilewips.external.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* compiled from: DhcpPacket */
class DhcpAckPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    DhcpAckPacket(int transId, short secs, boolean broadcast, Inet4Address serverAddress, Inet4Address relayIp, Inet4Address clientIp, Inet4Address yourIp, byte[] clientMac) {
        super(transId, secs, clientIp, yourIp, serverAddress, relayIp, clientMac, broadcast);
        this.mBroadcast = broadcast;
        this.mSrcIp = serverAddress;
    }

    public String toString() {
        String s = super.toString();
        String dnsServers = " DNS servers: ";
        for (Inet4Address dnsServer : this.mDnsServers) {
            dnsServers = dnsServers + dnsServer.toString() + " ";
        }
        return s + " ACK: your new IP " + this.mYourIp + ", netmask " + this.mSubnetMask + ", gateways " + this.mGateways + dnsServers + ", lease time " + this.mLeaseTime;
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    /* access modifiers changed from: package-private */
    public void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 5);
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addCommonServerTlvs(buffer);
        addTlvEnd(buffer);
    }

    private static final int getInt(Integer v) {
        if (v == null) {
            return 0;
        }
        return v.intValue();
    }
}
