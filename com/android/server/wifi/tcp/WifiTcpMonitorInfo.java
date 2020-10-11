package com.android.server.wifi.tcp;

public class WifiTcpMonitorInfo {
    int actionDuration;
    int actionResult;
    int chatRetrans;
    int closing;
    String date;
    int dnsBlockCount;
    int establishAll;
    int establishIPv4;
    int establishIPv6;
    int fin;
    int laskAck;
    int linkSpeed;
    double loss;
    int maxSynCount;
    String packageName;
    int receivingQueue;
    int receivingQueueCount;
    String result;
    int retransCount;
    int retransmission;
    int rssi;

    /* renamed from: rx */
    long f32rx;
    int syn;
    int synBlockCount;
    int synBlockNoEstablish;

    /* renamed from: tx */
    long f33tx;
    int uid;

    public WifiTcpMonitorInfo(WifiTcpMonitorInfo info) {
        this.uid = info.uid;
        this.packageName = info.packageName;
        this.date = info.date;
        this.result = info.result;
        this.establishAll = info.establishAll;
        this.establishIPv4 = info.establishIPv4;
        this.establishIPv6 = info.establishIPv6;
        this.syn = info.syn;
        this.retransmission = info.retransmission;
        this.fin = info.fin;
        this.closing = info.closing;
        this.laskAck = info.laskAck;
        this.f33tx = info.f33tx;
        this.f32rx = info.f32rx;
        this.loss = info.loss;
        this.rssi = info.rssi;
        this.linkSpeed = info.linkSpeed;
        this.actionDuration = info.actionDuration;
        this.actionResult = info.actionResult;
        this.receivingQueue = info.receivingQueue;
        this.dnsBlockCount = info.dnsBlockCount;
    }

    public WifiTcpMonitorInfo(int uid2, String packageName2) {
        this.uid = uid2;
        this.packageName = packageName2;
    }

    public String toString() {
        return this.date + " [UID]" + this.uid + ", [PN]" + this.packageName + ", [R]" + this.result + ", [E]" + this.establishAll + ", [E4]" + this.establishIPv4 + ", [E6]" + this.establishIPv6 + ", [S]" + this.syn + ", [R]" + this.retransmission + ", [F]" + this.fin + ", [C]" + this.closing + ", [LA]" + this.laskAck + ", [TX]" + this.f33tx + ", [RX]" + this.f32rx + ", [LO]" + this.loss + ", [R]" + this.rssi + ", [LI]" + this.linkSpeed + ", [AD]" + this.actionDuration + ", [AR]" + this.actionResult + ", [RQ]" + this.receivingQueue + ", [CR]" + this.chatRetrans + ", [RC]" + this.retransCount + ", [SBC]" + this.synBlockCount + ", [MSC]" + this.maxSynCount + ", [SBNEC]" + this.synBlockNoEstablish + ", [RQC]" + this.receivingQueueCount + ", [DBC]" + this.dnsBlockCount;
    }
}
