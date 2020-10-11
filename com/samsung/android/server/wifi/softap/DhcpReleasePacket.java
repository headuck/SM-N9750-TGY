package com.samsung.android.server.wifi.softap;

import com.samsung.android.server.wifi.mobilewips.external.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* compiled from: DhcpPacket */
class DhcpReleasePacket extends DhcpPacket {
    final Inet4Address mClientAddr;

    public DhcpReleasePacket(int transId, Inet4Address serverId, Inet4Address clientAddr, Inet4Address relayIp, byte[] clientMac) {
        super(transId, 0, clientAddr, INADDR_ANY, INADDR_ANY, relayIp, clientMac, false);
        this.mServerIdentifier = serverId;
        this.mClientAddr = clientAddr;
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, this.mServerIdentifier, this.mClientIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    /* access modifiers changed from: package-private */
    public void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 7);
        addTlv(buffer, (byte) 61, getClientId());
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addCommonClientTlvs(buffer);
        addTlvEnd(buffer);
    }
}
