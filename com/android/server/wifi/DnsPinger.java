package com.android.server.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class DnsPinger extends Handler {
    private static final int ACTION_CANCEL_ALL_PINGS = 593923;
    private static final int ACTION_LISTEN_FOR_RESPONSE = 593922;
    private static final int ACTION_PING_DNS = 593921;
    private static final int ACTION_PING_DNS_SPECIFIC = 593924;
    private static final int BASE = 593920;
    public static final int CACHED_RESULT = 1;
    private static final boolean DBG = Debug.semIsProductDev();
    public static final int DNS_PING_RESULT = 593920;
    public static final int DNS_PING_RESULT_SPECIFIC = 593925;
    private static final int DNS_PORT = 53;
    private static final int DNS_RESPONSE_BUFFER_SIZE = 512;
    private static HashMap<String, DnsResult> MostRecentDnsResultMap = new HashMap<>();
    public static final int NO_INTERNET = -3;
    public static final int PRIVATE_IP_ADDRESS = 2;
    private static final int RECEIVE_POLL_INTERVAL_MS = 200;
    public static final int REQUESTED_URL_ALREADY_IP_ADDRESS = 3;
    private static final boolean SMARTCM_DBG = false;
    public static final int SOCKET_EXCEPTION = -2;
    private static final int SOCKET_TIMEOUT_MS = 1;
    public static final int TIMEOUT = -1;
    private static final AtomicInteger sCounter = new AtomicInteger();
    private static final Random sRandom = new Random();
    HashMap<String, List<DnsResult>> DnsResultMap = new HashMap<>();
    private String TAG;
    final Object lock = new Object();
    private List<ActivePing> mActivePings = new ArrayList();
    private final int mConnectionType;
    private ConnectivityManager mConnectivityManager = null;
    private final Context mContext;
    private AtomicInteger mCurrentToken = new AtomicInteger();
    private final ArrayList<InetAddress> mDefaultDns;
    private byte[] mDnsQuery;
    private int mEventCounter;
    LinkProperties mLp = null;
    private final Handler mTarget;
    WifiInfo mWifiInfo = null;

    private class ActivePing {
        int internalId;
        short packetId;
        Integer result;
        DatagramSocket socket;
        long start;
        int timeout;
        String url;

        private ActivePing() {
            this.start = SystemClock.elapsedRealtime();
        }
    }

    private class DnsArg {
        InetAddress dns;
        int seq;
        String targetUrl;

        DnsArg(InetAddress d, int s, String u) {
            this.dns = d;
            this.seq = s;
            this.targetUrl = u;
        }
    }

    private class DnsResult {
        InetAddress resultIp;
        long ttl;

        DnsResult(InetAddress ip, long t) {
            this.resultIp = ip;
            this.ttl = t;
        }
    }

    public DnsPinger(Context context, String TAG2, Looper looper, Handler target, int connectionType) {
        super(looper);
        this.TAG = TAG2;
        this.mContext = context;
        this.mTarget = target;
        this.mConnectionType = connectionType;
        if (ConnectivityManager.isNetworkTypeValid(connectionType)) {
            this.mDefaultDns = new ArrayList<>();
            this.mDefaultDns.add(getDefaultDns());
            this.mEventCounter = 0;
            return;
        }
        throw new IllegalArgumentException("Invalid connectionType in constructor: " + connectionType);
    }

    /* Debug info: failed to restart local var, previous not found, register: 18 */
    public void handleMessage(Message msg) {
        Object obj;
        ActivePing curPing;
        Message message = msg;
        int oldTag = TrafficStats.getAndSetThreadStatsTag(-190);
        try {
            short s = 1;
            switch (message.what) {
                case ACTION_PING_DNS /*593921*/:
                case ACTION_PING_DNS_SPECIFIC /*593924*/:
                    DnsArg dnsArg = (DnsArg) message.obj;
                    if (dnsArg.seq == this.mCurrentToken.get()) {
                        try {
                            ActivePing newActivePing = new ActivePing();
                            InetAddress dnsAddress = dnsArg.dns;
                            updateDnsQuery(dnsArg.targetUrl);
                            newActivePing.internalId = message.arg1;
                            newActivePing.timeout = message.arg2;
                            newActivePing.url = dnsArg.targetUrl;
                            newActivePing.socket = new DatagramSocket();
                            newActivePing.socket.setSoTimeout(1);
                            try {
                                Os.setsockoptIfreq(newActivePing.socket.getFileDescriptor$(), OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, getCurrentLinkProperties().getInterfaceName());
                            } catch (Exception e) {
                                loge("sendDnsPing::Error binding to socket " + e);
                            }
                            if (message.what == ACTION_PING_DNS) {
                                newActivePing.packetId = (short) (sRandom.nextInt() << 1);
                            } else {
                                newActivePing.packetId = (short) ((sRandom.nextInt() << 1) + 1);
                            }
                            byte[] buf = (byte[]) this.mDnsQuery.clone();
                            buf[0] = (byte) (newActivePing.packetId >> 8);
                            buf[1] = (byte) newActivePing.packetId;
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, dnsAddress, 53);
                            if (DBG) {
                                log(getKernelTime() + "Sending a ping " + newActivePing.internalId + " to " + dnsAddress.getHostAddress() + " with packetId " + newActivePing.packetId + "(" + Integer.toHexString(newActivePing.packetId & 65535) + ").");
                            }
                            newActivePing.socket.send(packet);
                            this.mActivePings.add(newActivePing);
                            this.mEventCounter++;
                            sendMessageDelayed(obtainMessage(ACTION_LISTEN_FOR_RESPONSE, this.mEventCounter, 0), 200);
                            break;
                        } catch (IOException e2) {
                            if (message.what != ACTION_PING_DNS) {
                                sendResponse(message.arg1, -9999, -2);
                                break;
                            } else {
                                sendResponse(message.arg1, -9998, -2);
                                break;
                            }
                        }
                    }
                    break;
                case ACTION_LISTEN_FOR_RESPONSE /*593922*/:
                    if (message.arg1 == this.mEventCounter) {
                        Iterator<ActivePing> it = this.mActivePings.iterator();
                        while (it.hasNext()) {
                            curPing = it.next();
                            byte[] responseBuf = new byte[512];
                            DatagramPacket replyPacket = new DatagramPacket(responseBuf, 512);
                            curPing.socket.receive(replyPacket);
                            boolean isUsableResponse = false;
                            if (responseBuf[0] == ((byte) (curPing.packetId >> 8)) && responseBuf[1] == ((byte) curPing.packetId)) {
                                isUsableResponse = true;
                            } else {
                                if (DBG) {
                                    log("response ID doesn't match with query ID.");
                                }
                                Iterator<ActivePing> it2 = this.mActivePings.iterator();
                                while (true) {
                                    if (it2.hasNext()) {
                                        ActivePing activePingForIdCheck = it2.next();
                                        if (responseBuf[0] == ((byte) (activePingForIdCheck.packetId >> 8)) && responseBuf[1] == ((byte) activePingForIdCheck.packetId) && curPing.url != null && curPing.url.equals(activePingForIdCheck.url)) {
                                            log("response ID didn't match, but DNS response is usable.");
                                            isUsableResponse = true;
                                        }
                                    }
                                }
                            }
                            if (!isUsableResponse) {
                                log("response ID didn't match, ignoring packet");
                            } else if ((responseBuf[3] & 15) != 0 || (responseBuf[6] == 0 && responseBuf[7] == 0)) {
                                if (DBG) {
                                    loge("Reply code is not 0(No Error) or Answer Record Count is 0");
                                }
                                curPing.result = -3;
                            } else {
                                curPing.result = Integer.valueOf((int) (SystemClock.elapsedRealtime() - curPing.start));
                                updateDnsDB((byte[]) responseBuf.clone(), replyPacket.getLength(), curPing.url);
                                if (isDnsResponsePrivateAddress(curPing.url)) {
                                    curPing.result = 2;
                                }
                            }
                        }
                        Iterator<ActivePing> iter = this.mActivePings.iterator();
                        while (iter.hasNext()) {
                            ActivePing curPing2 = iter.next();
                            if (curPing2.result != null) {
                                if ((curPing2.packetId & s) != s || curPing2.result.intValue() <= 0) {
                                    sendResponse(curPing2.internalId, curPing2.packetId, curPing2.result.intValue());
                                } else {
                                    Object obj2 = this.lock;
                                    synchronized (obj2) {
                                        try {
                                            List<DnsResult> list = this.DnsResultMap.get(curPing2.url);
                                            if (list == null || list.size() <= 0) {
                                                obj = obj2;
                                                if (DBG) {
                                                    Log.e(this.TAG, "There are no results about " + curPing2.url);
                                                }
                                                sendResponse(curPing2.internalId, curPing2.packetId, -2);
                                            } else {
                                                try {
                                                    obj = obj2;
                                                    try {
                                                        sendResponse(curPing2.internalId, curPing2.packetId, curPing2.result.intValue(), curPing2.url, sRandom.nextInt(this.DnsResultMap.get(curPing2.url).size()), 0);
                                                    } catch (Exception e3) {
                                                    }
                                                } catch (Exception e4) {
                                                    obj = obj2;
                                                }
                                            }
                                        } catch (Throwable th) {
                                            th = th;
                                            throw th;
                                        }
                                    }
                                }
                                curPing2.socket.close();
                                iter.remove();
                            } else if (SystemClock.elapsedRealtime() > curPing2.start + ((long) curPing2.timeout)) {
                                sendResponse(curPing2.internalId, curPing2.packetId, -1, curPing2.url);
                                curPing2.socket.close();
                                iter.remove();
                            }
                            s = 1;
                        }
                        if (!this.mActivePings.isEmpty()) {
                            sendMessageDelayed(obtainMessage(ACTION_LISTEN_FOR_RESPONSE, this.mEventCounter, 0), 200);
                            break;
                        }
                    } else {
                        break;
                    }
                    break;
                case ACTION_CANCEL_ALL_PINGS /*593923*/:
                    for (ActivePing activePing : this.mActivePings) {
                        activePing.socket.close();
                    }
                    this.mActivePings.clear();
                    break;
            }
            TrafficStats.setThreadStatsTag(oldTag);
        } catch (SocketTimeoutException e5) {
        } catch (Exception e6) {
            if (DBG) {
                log("DnsPinger.pingDns got socket exception: " + e6);
            }
            curPing.result = -2;
        } catch (Throwable th2) {
            TrafficStats.setThreadStatsTag(oldTag);
            throw th2;
        }
    }

    public List<InetAddress> getDnsList() {
        LinkProperties curLinkProps = getCurrentLinkProperties();
        if (curLinkProps == null) {
            loge("getCurLinkProperties:: LP for type" + this.mConnectionType + " is null!");
            return this.mDefaultDns;
        }
        Collection<InetAddress> dnses = curLinkProps.getDnsServers();
        if (dnses != null && dnses.size() != 0) {
            return new ArrayList(dnses);
        }
        loge("getDns::LinkProps has null dns - returning default");
        return this.mDefaultDns;
    }

    public int pingDnsAsync(InetAddress dns, int timeout, int delay) {
        int id = sCounter.incrementAndGet();
        updateDnsResultMap("www.google.com");
        sendMessageDelayed(obtainMessage(ACTION_PING_DNS, id, timeout, new DnsArg(dns, this.mCurrentToken.get(), "www.google.com")), (long) delay);
        return id;
    }

    public int pingDnsAsyncSpecificForce(InetAddress dns, int timeout, int delay, String url) {
        int id = sCounter.incrementAndGet();
        sendMessageDelayed(obtainMessage(ACTION_PING_DNS_SPECIFIC, id, timeout, new DnsArg(dns, this.mCurrentToken.get(), url)), (long) delay);
        return id;
    }

    public int pingDnsAsyncSpecific(InetAddress dns, int timeout, int delay, String url) {
        int numOfResults;
        int id = sCounter.incrementAndGet();
        try {
            InetAddress addr = NetworkUtils.numericToInetAddress(url);
            if (DBG) {
                log("URL is already an IP address. " + url);
            }
            this.mTarget.sendMessageDelayed(obtainMessage(DNS_PING_RESULT_SPECIFIC, id, 3, addr), 50);
            return id;
        } catch (IllegalArgumentException e) {
            synchronized (this.lock) {
                if (this.DnsResultMap.get(url) == null) {
                    if (DBG) {
                        log("DNS Result Hashmap - NO HIT!!! SENDING DNS QUERY!  " + url);
                    }
                    sendMessageDelayed(obtainMessage(ACTION_PING_DNS_SPECIFIC, id, timeout, new DnsArg(dns, this.mCurrentToken.get(), url)), (long) delay);
                } else {
                    updateDnsResultMap(url);
                    if (this.DnsResultMap.get(url) != null) {
                        numOfResults = this.DnsResultMap.get(url).size();
                    } else {
                        numOfResults = 0;
                    }
                    if (numOfResults == 0) {
                        if (DBG) {
                            log("DNS Result Hashmap - HIT!!! BUT NO RESULTS   (" + numOfResults + ")" + url);
                        }
                        sendMessageDelayed(obtainMessage(ACTION_PING_DNS_SPECIFIC, id, timeout, new DnsArg(dns, this.mCurrentToken.get(), url)), (long) delay);
                    } else {
                        if (DBG) {
                            log("DNS Result Hashmap - HIT!!! USE PREVIOUS RESULT   (" + numOfResults + ")" + url);
                        }
                        sendResponse(id, -11111, 1, url, sRandom.nextInt(numOfResults), 50);
                    }
                }
                return id;
            }
        }
    }

    public void clear() {
        synchronized (this.lock) {
            this.DnsResultMap.clear();
            MostRecentDnsResultMap.clear();
        }
    }

    public void cancelPings() {
        this.mCurrentToken.incrementAndGet();
        obtainMessage(ACTION_CANCEL_ALL_PINGS).sendToTarget();
    }

    private void sendResponse(int internalId, int externalId, int responseVal) {
        if (DBG) {
            log("Responding to packet " + internalId + " externalId " + externalId + " and val " + responseVal);
        }
        if ((externalId & 1) == 1) {
            this.mTarget.sendMessage(obtainMessage(DNS_PING_RESULT_SPECIFIC, internalId, responseVal, (InetAddress) null));
        } else {
            this.mTarget.sendMessage(obtainMessage(593920, internalId, responseVal));
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 6 */
    private void sendResponse(int internalId, int externalId, int responseVal, String url, int index, int delay) {
        if (DBG) {
            log("Responding to packet " + internalId + " externalId " + externalId + " and val " + responseVal);
            StringBuilder sb = new StringBuilder();
            sb.append("SPECIFIC DNS PING: url - ");
            sb.append(url);
            sb.append(", responseVal : ");
            sb.append(responseVal);
            log(sb.toString());
        }
        try {
            synchronized (this.lock) {
                this.mTarget.sendMessageDelayed(obtainMessage(DNS_PING_RESULT_SPECIFIC, internalId, responseVal, ((DnsResult) this.DnsResultMap.get(url).get(index)).resultIp), (long) delay);
            }
        } catch (Exception e) {
        }
    }

    private void sendResponse(int internalId, int externalId, int responseVal, String url) {
        DnsResult res;
        if (DBG) {
            log("Responding to packet " + internalId + " externalId " + externalId + " val " + responseVal + " url " + url);
        }
        InetAddress resultIp = null;
        synchronized (this.lock) {
            if (responseVal == -1) {
                if (MostRecentDnsResultMap.containsKey(url) && (res = MostRecentDnsResultMap.get(url)) != null) {
                    resultIp = res.resultIp;
                    if (DBG) {
                        log("Sending most recent DNS result, " + resultIp.toString() + ", expired " + (System.currentTimeMillis() - res.ttl) + " msec ago.");
                    }
                }
            }
        }
        if ((externalId & 1) == 1) {
            this.mTarget.sendMessage(obtainMessage(DNS_PING_RESULT_SPECIFIC, internalId, responseVal, resultIp));
        } else {
            this.mTarget.sendMessage(obtainMessage(593920, internalId, responseVal));
        }
    }

    public void setCurrentLinkProperties(LinkProperties lp) {
        if (lp != null) {
            String str = this.TAG;
            Log.d(str, "setCurrentLinkProperties: lp=" + lp);
        }
        this.mLp = lp;
    }

    private LinkProperties getCurrentLinkProperties() {
        LinkProperties linkProperties = this.mLp;
        if (linkProperties != null) {
            return linkProperties;
        }
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager.getLinkProperties(this.mConnectionType);
    }

    private InetAddress getDefaultDns() {
        String dns = Settings.Global.getString(this.mContext.getContentResolver(), "default_dns_server");
        if (dns == null || dns.length() == 0) {
            dns = this.mContext.getResources().getString(17039927);
        }
        try {
            return NetworkUtils.numericToInetAddress(dns);
        } catch (IllegalArgumentException e) {
            loge("getDefaultDns::malformed default dns address");
            return null;
        }
    }

    private void updateDnsQuery(String url) {
        byte[] header = {0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0};
        byte[] trailer = {0, 0, 1, 0, 1};
        int length = url.length();
        byte blockSize = 0;
        byte[] middle = (byte[]) ('.' + url).getBytes().clone();
        for (int i = length; i >= 0; i--) {
            if (middle[i] == 46) {
                middle[i] = blockSize;
                blockSize = 0;
            } else {
                blockSize = (byte) (blockSize + 1);
            }
        }
        byte[] query = new byte[(length + 18)];
        System.arraycopy(header, 0, query, 0, 12);
        System.arraycopy(middle, 0, query, 12, length + 1);
        System.arraycopy(trailer, 0, query, length + 13, 5);
        this.mDnsQuery = (byte[]) query.clone();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x01e9, code lost:
        r5 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateDnsDB(byte[] r28, int r29, java.lang.String r30) {
        /*
            r27 = this;
            r1 = r27
            r2 = r30
            long r3 = java.lang.System.currentTimeMillis()
            r0 = 0
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r5 = r5 << 8
            r6 = 1
            int r0 = r0 + r6
            byte r7 = r28[r0]
            r7 = r7 & 255(0xff, float:3.57E-43)
            int r5 = r5 + r7
            int r0 = r0 + r6
            byte r7 = r28[r0]
            r7 = r7 & 255(0xff, float:3.57E-43)
            int r7 = r7 << 8
            int r0 = r0 + r6
            byte r8 = r28[r0]
            r8 = r8 & 255(0xff, float:3.57E-43)
            int r7 = r7 + r8
            int r0 = r0 + r6
            byte r8 = r28[r0]
            r8 = r8 & 255(0xff, float:3.57E-43)
            int r8 = r8 << 8
            int r0 = r0 + r6
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            int r8 = r8 + r9
            int r0 = r0 + r6
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            int r9 = r9 << 8
            int r0 = r0 + r6
            byte r10 = r28[r0]
            r10 = r10 & 255(0xff, float:3.57E-43)
            int r9 = r9 + r10
            int r0 = r0 + r6
            byte r10 = r28[r0]
            r10 = r10 & 255(0xff, float:3.57E-43)
            int r10 = r10 << 8
            int r0 = r0 + r6
            byte r11 = r28[r0]
            r11 = r11 & 255(0xff, float:3.57E-43)
            int r10 = r10 + r11
            int r0 = r0 + r6
            byte r11 = r28[r0]
            r11 = r11 & 255(0xff, float:3.57E-43)
            int r11 = r11 << 8
            int r0 = r0 + r6
            byte r12 = r28[r0]
            r12 = r12 & 255(0xff, float:3.57E-43)
            int r11 = r11 + r12
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
        L_0x005c:
            int r0 = r0 + r6
            byte r13 = r28[r0]
            r14 = 0
            if (r13 == 0) goto L_0x0089
            r13 = 1
        L_0x0063:
            byte r15 = r28[r0]
            if (r13 > r15) goto L_0x0080
            java.lang.Object[] r15 = new java.lang.Object[r6]
            int r17 = r0 + r13
            byte r17 = r28[r17]
            java.lang.Byte r17 = java.lang.Byte.valueOf(r17)
            r15[r14] = r17
            java.lang.String r14 = "%c"
            java.lang.String r14 = java.lang.String.format(r14, r15)
            r12.append(r14)
            int r13 = r13 + 1
            r14 = 0
            goto L_0x0063
        L_0x0080:
            r13 = 46
            r12.append(r13)
            byte r13 = r28[r0]
            int r0 = r0 + r13
            goto L_0x005c
        L_0x0089:
            int r13 = r12.length()
            int r13 = r13 - r6
            r12.deleteCharAt(r13)
            java.lang.String r13 = r12.toString()
            r13.equals(r2)
            int r0 = r0 + 4
            java.util.ArrayList r13 = new java.util.ArrayList
            r13.<init>()
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            r15 = 0
        L_0x00a5:
            if (r15 >= r9) goto L_0x01e1
            int r6 = r0 + 12
            r19 = r5
            r5 = 512(0x200, float:7.175E-43)
            if (r6 >= r5) goto L_0x01da
            int r0 = r0 + 1
            byte r6 = r28[r0]
            r5 = 192(0xc0, float:2.69E-43)
            r6 = r6 & r5
            if (r6 != r5) goto L_0x00bc
            int r0 = r0 + 1
            r5 = 1
            goto L_0x00c3
        L_0x00bc:
            r5 = 1
            int r0 = r0 + r5
            byte r6 = r28[r0]
            if (r6 == 0) goto L_0x00c3
            goto L_0x00bc
        L_0x00c3:
            int r0 = r0 + r5
            byte r6 = r28[r0]
            r6 = r6 & 255(0xff, float:3.57E-43)
            int r6 = r6 << 8
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r6 = r6 + r5
            r5 = 1
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r5 = r5 << 8
            r21 = r7
            r7 = 1
            int r0 = r0 + r7
            byte r7 = r28[r0]
            r7 = r7 & 255(0xff, float:3.57E-43)
            int r5 = r5 + r7
            r7 = 1
            int r0 = r0 + r7
            byte r7 = r28[r0]
            r7 = r7 & 255(0xff, float:3.57E-43)
            int r7 = r7 << 24
            r22 = r5
            r5 = 1
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r5 = r5 << 16
            int r7 = r7 + r5
            r5 = 1
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r5 = r5 << 8
            int r7 = r7 + r5
            r5 = 1
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r7 = r7 + r5
            r5 = 1
            int r0 = r0 + r5
            byte r5 = r28[r0]
            r5 = r5 & 255(0xff, float:3.57E-43)
            int r5 = r5 << 8
            r23 = r8
            r8 = 1
            int r0 = r0 + r8
            byte r8 = r28[r0]
            r8 = r8 & 255(0xff, float:3.57E-43)
            int r5 = r5 + r8
            int r8 = r0 + r5
            r24 = r9
            r9 = 512(0x200, float:7.175E-43)
            if (r8 < r9) goto L_0x0120
            r5 = r0
            goto L_0x01ea
        L_0x0120:
            r8 = 1
            if (r6 != r8) goto L_0x019c
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            int r0 = r0 + 1
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            java.lang.String r9 = java.lang.Integer.toString(r9)
            r8.append(r9)
            r9 = 46
            r8.append(r9)
            r16 = 1
            int r0 = r0 + 1
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            java.lang.String r9 = java.lang.Integer.toString(r9)
            r8.append(r9)
            r9 = 46
            r8.append(r9)
            int r0 = r0 + 1
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            java.lang.String r9 = java.lang.Integer.toString(r9)
            r8.append(r9)
            r9 = 46
            r8.append(r9)
            int r0 = r0 + 1
            byte r9 = r28[r0]
            r9 = r9 & 255(0xff, float:3.57E-43)
            java.lang.String r9 = java.lang.Integer.toString(r9)
            r8.append(r9)
            com.android.server.wifi.DnsPinger$DnsResult r9 = new com.android.server.wifi.DnsPinger$DnsResult
            java.lang.String r20 = r8.toString()
            r25 = r0
            java.net.InetAddress r0 = android.net.NetworkUtils.numericToInetAddress(r20)
            r20 = r6
            int r6 = r7 * 1000
            r26 = r7
            long r6 = (long) r6
            long r6 = r6 + r3
            r9.<init>(r0, r6)
            r0 = r9
            r13.add(r0)
            java.lang.String r6 = "["
            r14.append(r6)
            java.lang.String r6 = r8.toString()
            r14.append(r6)
            java.lang.String r6 = "] "
            r14.append(r6)
            r0 = r25
            goto L_0x01cd
        L_0x019c:
            r20 = r6
            r26 = r7
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            r7 = 0
        L_0x01a6:
            if (r7 >= r5) goto L_0x01cd
            r8 = 91
            r6.append(r8)
            r8 = 1
            java.lang.Object[] r9 = new java.lang.Object[r8]
            int r0 = r0 + 1
            byte r18 = r28[r0]
            java.lang.Byte r18 = java.lang.Byte.valueOf(r18)
            r17 = 0
            r9[r17] = r18
            java.lang.String r8 = "%02X"
            java.lang.String r8 = java.lang.String.format(r8, r9)
            r6.append(r8)
            r8 = 93
            r6.append(r8)
            int r7 = r7 + 1
            goto L_0x01a6
        L_0x01cd:
            int r15 = r15 + 1
            r5 = r19
            r7 = r21
            r8 = r23
            r9 = r24
            r6 = 1
            goto L_0x00a5
        L_0x01da:
            r21 = r7
            r23 = r8
            r24 = r9
            goto L_0x01e9
        L_0x01e1:
            r19 = r5
            r21 = r7
            r23 = r8
            r24 = r9
        L_0x01e9:
            r5 = r0
        L_0x01ea:
            boolean r0 = DBG
            if (r0 == 0) goto L_0x0219
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r6 = r27.getKernelTime()
            r0.append(r6)
            java.lang.String r6 = "DNS Result - "
            r0.append(r6)
            java.lang.String r6 = r12.toString()
            r0.append(r6)
            java.lang.String r6 = ", "
            r0.append(r6)
            java.lang.String r6 = r14.toString()
            r0.append(r6)
            java.lang.String r0 = r0.toString()
            r1.log(r0)
        L_0x0219:
            java.lang.Object r6 = r1.lock
            monitor-enter(r6)
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r0 = r1.DnsResultMap     // Catch:{ all -> 0x02b0 }
            boolean r0 = r0.containsKey(r2)     // Catch:{ all -> 0x02b0 }
            if (r0 != 0) goto L_0x022a
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r0 = r1.DnsResultMap     // Catch:{ all -> 0x02b0 }
            r0.put(r2, r13)     // Catch:{ all -> 0x02b0 }
            goto L_0x0246
        L_0x022a:
            r0 = 0
            r7 = r0
        L_0x022c:
            int r0 = r13.size()     // Catch:{ all -> 0x02b0 }
            if (r7 >= r0) goto L_0x0246
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r0 = r1.DnsResultMap     // Catch:{ all -> 0x02b0 }
            java.lang.Object r0 = r0.get(r2)     // Catch:{ all -> 0x02b0 }
            java.util.List r0 = (java.util.List) r0     // Catch:{ all -> 0x02b0 }
            java.lang.Object r8 = r13.get(r7)     // Catch:{ all -> 0x02b0 }
            com.android.server.wifi.DnsPinger$DnsResult r8 = (com.android.server.wifi.DnsPinger.DnsResult) r8     // Catch:{ all -> 0x02b0 }
            r0.add(r8)     // Catch:{ all -> 0x02b0 }
            int r7 = r7 + 1
            goto L_0x022c
        L_0x0246:
            boolean r0 = r1.isDnsResponsePrivateAddress(r2)     // Catch:{ all -> 0x02b0 }
            if (r0 != 0) goto L_0x026f
            java.util.HashMap<java.lang.String, com.android.server.wifi.DnsPinger$DnsResult> r0 = MostRecentDnsResultMap     // Catch:{ all -> 0x02b0 }
            r7 = 0
            java.lang.Object r7 = r13.get(r7)     // Catch:{ all -> 0x02b0 }
            com.android.server.wifi.DnsPinger$DnsResult r7 = (com.android.server.wifi.DnsPinger.DnsResult) r7     // Catch:{ all -> 0x02b0 }
            r0.put(r2, r7)     // Catch:{ all -> 0x02b0 }
            java.util.HashMap<java.lang.String, com.android.server.wifi.DnsPinger$DnsResult> r0 = MostRecentDnsResultMap     // Catch:{ all -> 0x02b0 }
            java.util.Set r0 = r0.keySet()     // Catch:{ all -> 0x02b0 }
            java.util.Iterator r0 = r0.iterator()     // Catch:{ all -> 0x02b0 }
        L_0x0262:
            boolean r7 = r0.hasNext()     // Catch:{ all -> 0x02b0 }
            if (r7 == 0) goto L_0x026f
            java.lang.Object r7 = r0.next()     // Catch:{ all -> 0x02b0 }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ all -> 0x02b0 }
            goto L_0x0262
        L_0x026f:
            boolean r0 = DBG     // Catch:{ all -> 0x02b0 }
            if (r0 == 0) goto L_0x02ae
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x02b0 }
            r0.<init>()     // Catch:{ all -> 0x02b0 }
            java.lang.String r7 = "Hashmap DnsResultMap contains "
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r7 = r1.DnsResultMap     // Catch:{ all -> 0x02b0 }
            int r7 = r7.size()     // Catch:{ all -> 0x02b0 }
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            java.lang.String r7 = " entries, url: "
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            r0.append(r2)     // Catch:{ all -> 0x02b0 }
            java.lang.String r7 = " - "
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r7 = r1.DnsResultMap     // Catch:{ all -> 0x02b0 }
            java.lang.Object r7 = r7.get(r2)     // Catch:{ all -> 0x02b0 }
            java.util.List r7 = (java.util.List) r7     // Catch:{ all -> 0x02b0 }
            int r7 = r7.size()     // Catch:{ all -> 0x02b0 }
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            java.lang.String r7 = " IPs"
            r0.append(r7)     // Catch:{ all -> 0x02b0 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x02b0 }
            r1.log(r0)     // Catch:{ all -> 0x02b0 }
        L_0x02ae:
            monitor-exit(r6)     // Catch:{ all -> 0x02b0 }
            return
        L_0x02b0:
            r0 = move-exception
            monitor-exit(r6)     // Catch:{ all -> 0x02b0 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.DnsPinger.updateDnsDB(byte[], int, java.lang.String):void");
    }

    private void updateDnsResultMap(String url) {
        synchronized (this.lock) {
            List<DnsResult> mDnsResultList = this.DnsResultMap.get(url);
            long currTime = System.currentTimeMillis();
            if (mDnsResultList != null) {
                for (int i = mDnsResultList.size() - 1; i >= 0; i--) {
                    int ipByte1st = mDnsResultList.get(i).resultIp.getAddress()[0] & 255;
                    int ipByte2nd = mDnsResultList.get(i).resultIp.getAddress()[1] & 255;
                    int ipByte3rd = mDnsResultList.get(i).resultIp.getAddress()[2] & 255;
                    int ipByte4th = mDnsResultList.get(i).resultIp.getAddress()[3] & 255;
                    if (ipByte1st != 10 && (!(ipByte1st == 192 && ipByte2nd == 168) && (ipByte1st != 172 || ipByte2nd < 16 || ipByte2nd > 31))) {
                        if (ipByte1st != 1 || ipByte2nd != 33 || ipByte3rd != 203 || ipByte4th != 39) {
                            if (currTime > mDnsResultList.get(i).ttl) {
                                mDnsResultList.remove(i);
                            }
                        }
                    }
                    mDnsResultList.remove(i);
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00b7, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00b9, code lost:
        return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isDnsResponsePrivateAddress(java.lang.String r12) {
        /*
            r11 = this;
            java.lang.Object r0 = r11.lock
            monitor-enter(r0)
            java.util.HashMap<java.lang.String, java.util.List<com.android.server.wifi.DnsPinger$DnsResult>> r1 = r11.DnsResultMap     // Catch:{ all -> 0x00ba }
            java.lang.Object r1 = r1.get(r12)     // Catch:{ all -> 0x00ba }
            java.util.List r1 = (java.util.List) r1     // Catch:{ all -> 0x00ba }
            r2 = 0
            if (r1 == 0) goto L_0x00b8
            int r3 = r1.size()     // Catch:{ all -> 0x00ba }
            int r4 = r3 + -1
        L_0x0014:
            if (r4 < 0) goto L_0x00b8
            java.lang.Object r5 = r1.get(r4)     // Catch:{ all -> 0x00ba }
            com.android.server.wifi.DnsPinger$DnsResult r5 = (com.android.server.wifi.DnsPinger.DnsResult) r5     // Catch:{ all -> 0x00ba }
            java.net.InetAddress r5 = r5.resultIp     // Catch:{ all -> 0x00ba }
            byte[] r5 = r5.getAddress()     // Catch:{ all -> 0x00ba }
            byte r5 = r5[r2]     // Catch:{ all -> 0x00ba }
            r5 = r5 & 255(0xff, float:3.57E-43)
            java.lang.Object r6 = r1.get(r4)     // Catch:{ all -> 0x00ba }
            com.android.server.wifi.DnsPinger$DnsResult r6 = (com.android.server.wifi.DnsPinger.DnsResult) r6     // Catch:{ all -> 0x00ba }
            java.net.InetAddress r6 = r6.resultIp     // Catch:{ all -> 0x00ba }
            byte[] r6 = r6.getAddress()     // Catch:{ all -> 0x00ba }
            r7 = 1
            byte r6 = r6[r7]     // Catch:{ all -> 0x00ba }
            r6 = r6 & 255(0xff, float:3.57E-43)
            java.lang.Object r8 = r1.get(r4)     // Catch:{ all -> 0x00ba }
            com.android.server.wifi.DnsPinger$DnsResult r8 = (com.android.server.wifi.DnsPinger.DnsResult) r8     // Catch:{ all -> 0x00ba }
            java.net.InetAddress r8 = r8.resultIp     // Catch:{ all -> 0x00ba }
            byte[] r8 = r8.getAddress()     // Catch:{ all -> 0x00ba }
            r9 = 2
            byte r8 = r8[r9]     // Catch:{ all -> 0x00ba }
            r8 = r8 & 255(0xff, float:3.57E-43)
            java.lang.Object r9 = r1.get(r4)     // Catch:{ all -> 0x00ba }
            com.android.server.wifi.DnsPinger$DnsResult r9 = (com.android.server.wifi.DnsPinger.DnsResult) r9     // Catch:{ all -> 0x00ba }
            java.net.InetAddress r9 = r9.resultIp     // Catch:{ all -> 0x00ba }
            byte[] r9 = r9.getAddress()     // Catch:{ all -> 0x00ba }
            r10 = 3
            byte r9 = r9[r10]     // Catch:{ all -> 0x00ba }
            r9 = r9 & 255(0xff, float:3.57E-43)
            r10 = 10
            if (r5 == r10) goto L_0x0083
            r10 = 192(0xc0, float:2.69E-43)
            if (r5 != r10) goto L_0x0065
            r10 = 168(0xa8, float:2.35E-43)
            if (r6 == r10) goto L_0x0083
        L_0x0065:
            r10 = 172(0xac, float:2.41E-43)
            if (r5 != r10) goto L_0x0071
            r10 = 16
            if (r6 < r10) goto L_0x0071
            r10 = 31
            if (r6 <= r10) goto L_0x0083
        L_0x0071:
            if (r5 != r7) goto L_0x0080
            r10 = 33
            if (r6 != r10) goto L_0x0080
            r10 = 203(0xcb, float:2.84E-43)
            if (r8 != r10) goto L_0x0080
            r10 = 39
            if (r9 != r10) goto L_0x0080
            goto L_0x0083
        L_0x0080:
            int r4 = r4 + -1
            goto L_0x0014
        L_0x0083:
            boolean r2 = DBG     // Catch:{ all -> 0x00ba }
            if (r2 == 0) goto L_0x00b6
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x00ba }
            r2.<init>()     // Catch:{ all -> 0x00ba }
            r2.append(r12)     // Catch:{ all -> 0x00ba }
            java.lang.String r10 = " - Dns Response with Private Network IP Address !!! - "
            r2.append(r10)     // Catch:{ all -> 0x00ba }
            r2.append(r5)     // Catch:{ all -> 0x00ba }
            java.lang.String r10 = "."
            r2.append(r10)     // Catch:{ all -> 0x00ba }
            r2.append(r6)     // Catch:{ all -> 0x00ba }
            java.lang.String r10 = "."
            r2.append(r10)     // Catch:{ all -> 0x00ba }
            r2.append(r8)     // Catch:{ all -> 0x00ba }
            java.lang.String r10 = "."
            r2.append(r10)     // Catch:{ all -> 0x00ba }
            r2.append(r9)     // Catch:{ all -> 0x00ba }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x00ba }
            r11.log(r2)     // Catch:{ all -> 0x00ba }
        L_0x00b6:
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return r7
        L_0x00b8:
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            return r2
        L_0x00ba:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x00ba }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.DnsPinger.isDnsResponsePrivateAddress(java.lang.String):boolean");
    }

    public String macToString(byte[] mac) {
        String macAddr = "";
        if (mac == null) {
            return null;
        }
        for (int i = 0; i < mac.length; i++) {
            String hexString = "0" + Integer.toHexString(mac[i]);
            macAddr = macAddr + hexString.substring(hexString.length() - 2);
            if (i != mac.length - 1) {
                macAddr = macAddr + ":";
            }
        }
        return macAddr;
    }

    private void log(String s) {
        Log.d(this.TAG, s);
    }

    private void loge(String s) {
        Log.e(this.TAG, s);
    }

    private String getKernelTime() {
        return "(" + (((double) (System.nanoTime() / 1000000)) / 1000.0d) + ") ";
    }
}
