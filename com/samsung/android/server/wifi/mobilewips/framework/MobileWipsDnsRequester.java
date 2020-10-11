package com.samsung.android.server.wifi.mobilewips.framework;

import android.net.LinkProperties;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MobileWipsDnsRequester extends Handler {
    private static final int EVENT_DNS_BULK_QUERY = 1000;
    private static final String TAG = "MobileWips::dns";
    private ServiceHandler mDnsHandler = null;
    private HandlerThread mDnsThread = null;
    /* access modifiers changed from: private */
    public MobileWipsPacketSender mPacketSender = null;

    public MobileWipsDnsRequester(MobileWipsPacketSender packetSender) {
        this.mPacketSender = packetSender;
    }

    static final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1000) {
                Vector<DnsRequest> dnsReq = (Vector) msg.getData().getSerializable("dnsReq");
                if (dnsReq == null) {
                    Log.d(MobileWipsDnsRequester.TAG, "Bundle object null");
                } else if (dnsReq.isEmpty()) {
                    Log.d(MobileWipsDnsRequester.TAG, "dnsReq is Empty");
                } else {
                    ExecutorService poolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
                    Iterator<DnsRequest> it = dnsReq.iterator();
                    while (it.hasNext()) {
                        poolExecutor.execute(it.next());
                    }
                    poolExecutor.shutdown();
                    List<String> responses = new ArrayList<>();
                    try {
                        long timeout = Math.round(((double) dnsReq.size()) * 1.2d * ((double) msg.getData().getLong("timeoutOneQuery")));
                        if (poolExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                            Iterator<DnsRequest> it2 = dnsReq.iterator();
                            while (true) {
                                if (!it2.hasNext()) {
                                    break;
                                }
                                DnsRequest request = it2.next();
                                if (request.getResponse() == null) {
                                    responses.clear();
                                    break;
                                }
                                responses.add(MobileWipsFrameworkUtil.byteArrayToHexString(request.getResponse()));
                            }
                        } else {
                            Log.d(MobileWipsDnsRequester.TAG, "Dns spoofing queries pool executor timeout elapsed before termination: " + timeout + "ms");
                        }
                        MobileWipsFrameworkService.getInstance().onDnsResponses(responses, msg.getData().getString("dnsMac"));
                    } catch (InterruptedException e) {
                        Log.d(MobileWipsDnsRequester.TAG, "Iterrupted awaitTermination: " + e.getMessage());
                    }
                }
            }
        }
    }

    public class DnsRequest implements Runnable {
        private boolean isUDP;
        private byte[] mDnsMessage;
        private byte[] mDstAddr;
        private String mDstMac;
        private LinkProperties mLinkProperties;
        private byte[] mResponse = null;
        private byte[] mSrcAddr;
        private long[] mTimeoutMillis;

        public DnsRequest(LinkProperties linkProperties, long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, String dstMac, byte[] dnsMessage, boolean isUdp) {
            this.mLinkProperties = linkProperties;
            this.mTimeoutMillis = timeoutMillis;
            this.mSrcAddr = (byte[]) srcAddr.clone();
            this.mDstAddr = (byte[]) dstAddr.clone();
            this.mDstMac = dstMac;
            this.mDnsMessage = dnsMessage;
            this.isUDP = isUdp;
        }

        public byte[] getResponse() {
            return this.mResponse;
        }

        public void run() {
            try {
                long time = System.currentTimeMillis();
                Log.d(MobileWipsDnsRequester.TAG, "Start Thread Id: " + Thread.currentThread().getId() + " " + this.mDnsMessage[0] + "" + this.mDnsMessage[1]);
                this.mResponse = MobileWipsDnsRequester.this.mPacketSender.sendDns(this.mLinkProperties, this.mSrcAddr, this.mDstAddr, this.mDstMac, this.mDnsMessage, this.mTimeoutMillis, this.isUDP);
                Log.d(MobileWipsDnsRequester.TAG, "End Thread Id: " + Thread.currentThread().getId() + " " + this.mDnsMessage[0] + "" + this.mDnsMessage[1] + " Time: " + (System.currentTimeMillis() - time) + "ms");
            } catch (Exception e) {
                Log.d(MobileWipsDnsRequester.TAG, "Exception DnsRequest run: " + e);
            }
        }
    }

    public boolean sendDnsQueries(LinkProperties linkProperties, long[] timeoutMillis, byte[] srcAddr, byte[] dstAddr, String dstMac, List<String> dnsMessages, int tcpIndex) {
        long[] jArr = timeoutMillis;
        Log.d(TAG, "sendDnsQueries IN");
        boolean z = false;
        if (this.mPacketSender == null) {
            Log.d(TAG, "mPacketSender null");
            return false;
        }
        if (this.mDnsThread == null || this.mDnsHandler == null) {
            this.mDnsThread = new HandlerThread(TAG);
            this.mDnsThread.start();
            this.mDnsHandler = new ServiceHandler(this.mDnsThread.getLooper());
        }
        try {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            Vector vector = new Vector();
            int index = 0;
            for (String message : dnsMessages) {
                int index2 = index + 1;
                boolean isUDP = tcpIndex != index ? true : z;
                DnsRequest dnsRequest = r1;
                DnsRequest dnsRequest2 = new DnsRequest(linkProperties, timeoutMillis, srcAddr, dstAddr, dstMac, MobileWipsFrameworkUtil.hexStringToByteArray(message), isUDP);
                vector.add(dnsRequest);
                index = index2;
                z = false;
            }
            long timeoutTotal = 0;
            for (long time : jArr) {
                timeoutTotal += time;
            }
            try {
                bundle.putString("dnsMac", dstMac);
                bundle.putSerializable("dnsReq", vector);
                bundle.putLong("timeoutOneQuery", timeoutTotal);
                msg.what = 1000;
                msg.setData(bundle);
                this.mDnsHandler.sendMessage(msg);
                return true;
            } catch (Exception e) {
                e = e;
            }
        } catch (Exception e2) {
            e = e2;
            String str = dstMac;
            Log.d(TAG, "Exception : " + e);
            return false;
        }
    }

    public void stop() {
        Log.d(TAG, "stop DnsRequester");
        ServiceHandler serviceHandler = this.mDnsHandler;
        if (serviceHandler != null) {
            serviceHandler.removeMessages(1000);
        }
        HandlerThread handlerThread = this.mDnsThread;
        if (handlerThread != null) {
            handlerThread.interrupt();
            this.mDnsThread.quitSafely();
            this.mDnsThread = null;
        }
    }
}
