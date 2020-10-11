package com.samsung.android.server.wifi.softap;

import com.samsung.android.server.wifi.mobilewips.external.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* compiled from: DhcpPacket */
class DhcpOfferPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpOfferPacket(int transId, short secs, boolean broadcast, Inet4Address serverAddress, Inet4Address relayIp, Inet4Address clientIp, Inet4Address yourIp, byte[] clientMac) {
        super(transId, secs, clientIp, yourIp, serverAddress, relayIp, clientMac, broadcast);
        this.mSrcIp = serverAddress;
    }

    public String toString() {
        String s = super.toString();
        String dnsServers = ", DNS servers: ";
        if (this.mDnsServers != null) {
            for (Inet4Address dnsServer : this.mDnsServers) {
                dnsServers = dnsServers + dnsServer + " ";
            }
        }
        return s + " OFFER, ip " + this.mYourIp + ", mask " + this.mSubnetMask + dnsServers + ", gateways " + this.mGateways + " lease time " + this.mLeaseTime + ", domain " + this.mDomainName;
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    /* access modifiers changed from: package-private */
    public void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 2);
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addCommonServerTlvs(buffer);
        addTlvEnd(buffer);
    }
}
