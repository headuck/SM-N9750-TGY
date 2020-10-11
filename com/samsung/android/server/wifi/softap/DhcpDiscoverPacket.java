package com.samsung.android.server.wifi.softap;

import com.samsung.android.server.wifi.mobilewips.external.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* compiled from: DhcpPacket */
class DhcpDiscoverPacket extends DhcpPacket {
    final Inet4Address mSrcIp;

    DhcpDiscoverPacket(int transId, short secs, Inet4Address relayIp, byte[] clientMac, boolean broadcast, Inet4Address srcIp) {
        super(transId, secs, INADDR_ANY, INADDR_ANY, INADDR_ANY, relayIp, clientMac, broadcast);
        this.mSrcIp = srcIp;
    }

    public String toString() {
        String s = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(" DISCOVER ");
        sb.append(this.mBroadcast ? "broadcast " : "unicast ");
        return sb.toString();
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, INADDR_BROADCAST, this.mSrcIp, destUdp, srcUdp, result, (byte) 1, this.mBroadcast);
        result.flip();
        return result;
    }

    /* access modifiers changed from: package-private */
    public void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 1);
        addTlv(buffer, (byte) 61, getClientId());
        addCommonClientTlvs(buffer);
        addTlv(buffer, (byte) 55, this.mRequestedParams);
        addTlvEnd(buffer);
    }
}
