package com.android.server.wifi.aware;

import android.content.Context;
import android.content.Intent;
import android.hardware.wifi.V1_2.NanDataPathChannelInfo;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;
import android.net.wifi.aware.TlvBufferUtils;
import android.net.wifi.aware.WifiAwareAgentNetworkSpecifier;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareUtils;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.aware.WifiAwareDataPathStateManager;
import com.android.server.wifi.aware.WifiAwareDiscoverySessionState;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import libcore.util.HexEncoding;

public class WifiAwareDataPathStateManager {
    @VisibleForTesting
    public static final int ADDRESS_VALIDATION_RETRY_INTERVAL_MS = 1000;
    @VisibleForTesting
    public static final int ADDRESS_VALIDATION_TIMEOUT_MS = 5000;
    private static final String AGENT_TAG_PREFIX = "WIFI_AWARE_AGENT_";
    private static final String AWARE_INTERFACE_PREFIX = "aware_data";
    private static final int NETWORK_FACTORY_BANDWIDTH_AVAIL = 1;
    private static final int NETWORK_FACTORY_SCORE_AVAIL = 1;
    private static final int NETWORK_FACTORY_SIGNAL_STRENGTH_AVAIL = 1;
    private static final String NETWORK_TAG = "WIFI_AWARE_FACTORY";
    private static final String TAG = "WifiAwareDataPathStMgr";
    private static final boolean VDBG = true;
    private static int mCountDataPath = 0;
    /* access modifiers changed from: private */
    public static final NetworkCapabilities sNetworkCapabilitiesFilter = new NetworkCapabilities();
    boolean mAllowNdpResponderFromAnyOverride = false;
    private WifiAwareMetrics mAwareMetrics;
    /* access modifiers changed from: private */
    public final Clock mClock;
    private Context mContext;
    boolean mDbg = true;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public final Set<String> mInterfaces = new HashSet();
    private Looper mLooper;
    /* access modifiers changed from: private */
    public final WifiAwareStateManager mMgr;
    private WifiAwareNetworkFactory mNetworkFactory;
    /* access modifiers changed from: private */
    public final Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> mNetworkRequestsCache = new ArrayMap();
    public NetworkInterfaceWrapper mNiWrapper = new NetworkInterfaceWrapper();
    public INetworkManagementService mNwService;
    /* access modifiers changed from: private */
    public WifiPermissionsWrapper mPermissionsWrapper;
    /* access modifiers changed from: private */
    public WifiPermissionsUtil mWifiPermissionsUtil;

    public WifiAwareDataPathStateManager(WifiAwareStateManager mgr, Clock clock) {
        this.mMgr = mgr;
        this.mClock = clock;
    }

    public void start(Context context, Looper looper, WifiAwareMetrics awareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionsWrapper) {
        Log.v(TAG, "start");
        this.mContext = context;
        this.mAwareMetrics = awareMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mPermissionsWrapper = permissionsWrapper;
        this.mLooper = looper;
        this.mHandler = new Handler(this.mLooper);
        sNetworkCapabilitiesFilter.clearAll();
        sNetworkCapabilitiesFilter.addTransportType(5);
        sNetworkCapabilitiesFilter.addCapability(15).addCapability(11).addCapability(18).addCapability(20).addCapability(13).addCapability(14);
        sNetworkCapabilitiesFilter.setNetworkSpecifier(new MatchAllNetworkSpecifier());
        sNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setSignalStrength(1);
        this.mNetworkFactory = new WifiAwareNetworkFactory(looper, context, sNetworkCapabilitiesFilter);
        this.mNetworkFactory.setScoreFilter(1);
        this.mNetworkFactory.register();
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByNdpId(int ndpId) {
        for (Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (entry.getValue().ndpId == ndpId) {
                return entry;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByCanonicalDescriptor(CanonicalConnectionInfo cci) {
        Log.v(TAG, "getNetworkRequestByCanonicalDescriptor: cci=" + cci);
        for (Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            Log.v(TAG, "getNetworkRequestByCanonicalDescriptor: entry=" + entry.getValue() + " --> cci=" + entry.getValue().getCanonicalDescriptor());
            if (entry.getValue().getCanonicalDescriptor().matches(cci)) {
                return entry;
            }
        }
        return null;
    }

    public void createAllInterfaces() {
        Log.v(TAG, "createAllInterfaces");
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "createAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            String name = AWARE_INTERFACE_PREFIX + i;
            if (this.mInterfaces.contains(name)) {
                Log.e(TAG, "createAllInterfaces(): interface already up, " + name + ", possibly failed to delete - deleting/creating again to be safe");
                this.mMgr.deleteDataPathInterface(name);
                this.mInterfaces.remove(name);
            }
            this.mMgr.createDataPathInterface(name);
        }
    }

    public void deleteAllInterfaces() {
        Log.v(TAG, "deleteAllInterfaces");
        onAwareDownCleanupDataPaths();
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "deleteAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            this.mMgr.deleteDataPathInterface(AWARE_INTERFACE_PREFIX + i);
        }
        this.mMgr.releaseAwareInterface();
    }

    public void onInterfaceCreated(String interfaceName) {
        Log.v(TAG, "onInterfaceCreated: interfaceName=" + interfaceName);
        if (this.mInterfaces.contains(interfaceName)) {
            Log.w(TAG, "onInterfaceCreated: already contains interface -- " + interfaceName);
        }
        this.mInterfaces.add(interfaceName);
    }

    public void onInterfaceDeleted(String interfaceName) {
        Log.v(TAG, "onInterfaceDeleted: interfaceName=" + interfaceName);
        if (!this.mInterfaces.contains(interfaceName)) {
            Log.w(TAG, "onInterfaceDeleted: interface not on list -- " + interfaceName);
        }
        this.mInterfaces.remove(interfaceName);
    }

    public void onDataPathInitiateSuccess(WifiAwareNetworkSpecifier networkSpecifier, int ndpId) {
        Log.v(TAG, "onDataPathInitiateSuccess: networkSpecifier=" + networkSpecifier + ", ndpId=" + ndpId);
        AwareNetworkRequestInformation nnri = this.mNetworkRequestsCache.get(networkSpecifier);
        if (nnri == null) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request not found for networkSpecifier=" + networkSpecifier);
            this.mMgr.endDataPath(ndpId);
        } else if (nnri.state != 103) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request in incorrect state: state=" + nnri.state);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            declareUnfullfillableAndEndDp(nnri, ndpId);
        } else {
            nnri.state = 101;
            nnri.ndpId = ndpId;
        }
    }

    public void onDataPathInitiateFail(WifiAwareNetworkSpecifier networkSpecifier, int reason) {
        Log.v(TAG, "onDataPathInitiateFail: networkSpecifier=" + networkSpecifier + ", reason=" + reason);
        AwareNetworkRequestInformation nnri = this.mNetworkRequestsCache.remove(networkSpecifier);
        if (nnri == null) {
            Log.w(TAG, "onDataPathInitiateFail: network request not found for networkSpecifier=" + networkSpecifier);
            return;
        }
        this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri);
        if (nnri.state != 103) {
            Log.w(TAG, "onDataPathInitiateFail: network request in incorrect state: state=" + nnri.state);
        }
        this.mAwareMetrics.recordNdpStatus(reason, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v6, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v23, resolved type: android.net.wifi.aware.WifiAwareNetworkSpecifier} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v7, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v5, resolved type: com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.net.wifi.aware.WifiAwareNetworkSpecifier onDataPathRequest(int r17, byte[] r18, int r19, byte[] r20) {
        /*
            r16 = this;
            r0 = r16
            r1 = r17
            r2 = r18
            r11 = r19
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onDataPathRequest: pubSubId="
            r3.append(r4)
            r3.append(r1)
            java.lang.String r4 = ", mac="
            r3.append(r4)
            char[] r5 = libcore.util.HexEncoding.encode(r18)
            java.lang.String r5 = java.lang.String.valueOf(r5)
            r3.append(r5)
            java.lang.String r5 = ", ndpId="
            r3.append(r5)
            r3.append(r11)
            java.lang.String r3 = r3.toString()
            java.lang.String r5 = "WifiAwareDataPathStMgr"
            android.util.Log.v(r5, r3)
            r3 = 0
            r6 = 0
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r7 = r0.mNetworkRequestsCache
            java.util.Set r7 = r7.entrySet()
            java.util.Iterator r7 = r7.iterator()
        L_0x0042:
            boolean r8 = r7.hasNext()
            if (r8 == 0) goto L_0x009a
            java.lang.Object r8 = r7.next()
            java.util.Map$Entry r8 = (java.util.Map.Entry) r8
            java.lang.Object r9 = r8.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r9 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r9
            int r9 = r9.pubSubId
            if (r9 == 0) goto L_0x0063
            java.lang.Object r9 = r8.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r9 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r9
            int r9 = r9.pubSubId
            if (r9 == r1) goto L_0x0063
            goto L_0x0042
        L_0x0063:
            java.lang.Object r9 = r8.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r9 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r9
            byte[] r9 = r9.peerDiscoveryMac
            if (r9 == 0) goto L_0x007c
            java.lang.Object r9 = r8.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r9 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r9
            byte[] r9 = r9.peerDiscoveryMac
            boolean r9 = java.util.Arrays.equals(r9, r2)
            if (r9 != 0) goto L_0x007c
            goto L_0x0042
        L_0x007c:
            java.lang.Object r9 = r8.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r9 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r9
            int r9 = r9.state
            r10 = 104(0x68, float:1.46E-43)
            if (r9 == r10) goto L_0x0089
            goto L_0x0042
        L_0x0089:
            java.lang.Object r7 = r8.getKey()
            r3 = r7
            android.net.wifi.aware.WifiAwareNetworkSpecifier r3 = (android.net.wifi.aware.WifiAwareNetworkSpecifier) r3
            java.lang.Object r7 = r8.getValue()
            r6 = r7
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r6 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r6
            r13 = r3
            r12 = r6
            goto L_0x009c
        L_0x009a:
            r13 = r3
            r12 = r6
        L_0x009c:
            java.util.Map$Entry r14 = r0.getNetworkRequestByNdpId(r11)
            r15 = 0
            if (r14 == 0) goto L_0x00ee
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onDataPathRequest: initiator-side indication for "
            r3.append(r4)
            java.lang.Object r4 = r14.getValue()
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.v(r5, r3)
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$NetworkInformationData$ParsedResults r3 = com.android.server.wifi.aware.WifiAwareDataPathStateManager.NetworkInformationData.parseTlv(r20)
            if (r3 == 0) goto L_0x00ed
            int r4 = r3.port
            if (r4 == 0) goto L_0x00d0
            java.lang.Object r4 = r14.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r4 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r4
            int r5 = r3.port
            r4.peerPort = r5
        L_0x00d0:
            int r4 = r3.transportProtocol
            r5 = -1
            if (r4 == r5) goto L_0x00df
            java.lang.Object r4 = r14.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r4 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r4
            int r5 = r3.transportProtocol
            r4.peerTransportProtocol = r5
        L_0x00df:
            byte[] r4 = r3.ipv6Override
            if (r4 == 0) goto L_0x00ed
            java.lang.Object r4 = r14.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r4 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r4
            byte[] r5 = r3.ipv6Override
            r4.peerIpv6Override = r5
        L_0x00ed:
            return r15
        L_0x00ee:
            if (r12 != 0) goto L_0x0137
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r6 = "onDataPathRequest: can't find a request with specified pubSubId="
            r3.append(r6)
            r3.append(r1)
            r3.append(r4)
            char[] r4 = libcore.util.HexEncoding.encode(r18)
            java.lang.String r4 = java.lang.String.valueOf(r4)
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r5, r3)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onDataPathRequest: network request cache = "
            r3.append(r4)
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r4 = r0.mNetworkRequestsCache
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.v(r5, r3)
            com.android.server.wifi.aware.WifiAwareStateManager r3 = r0.mMgr
            r4 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            java.lang.String r6 = ""
            r5 = r19
            r3.respondToDataPathRequest(r4, r5, r6, r7, r8, r9, r10)
            return r15
        L_0x0137:
            byte[] r3 = r12.peerDiscoveryMac
            if (r3 != 0) goto L_0x013d
            r12.peerDiscoveryMac = r2
        L_0x013d:
            java.lang.String r3 = r0.selectInterfaceForRequest(r12)
            r12.interfaceName = r3
            java.lang.String r3 = r12.interfaceName
            if (r3 != 0) goto L_0x0179
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onDataPathRequest: request "
            r3.append(r4)
            r3.append(r13)
            java.lang.String r4 = " no interface available"
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r5, r3)
            com.android.server.wifi.aware.WifiAwareStateManager r3 = r0.mMgr
            r4 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            java.lang.String r6 = ""
            r5 = r19
            r3.respondToDataPathRequest(r4, r5, r6, r7, r8, r9, r10)
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r3 = r0.mNetworkRequestsCache
            r3.remove(r13)
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$WifiAwareNetworkFactory r3 = r0.mNetworkFactory
            r3.letAppKnowThatRequestsAreUnavailable(r12)
            return r15
        L_0x0179:
            r3 = 105(0x69, float:1.47E-43)
            r12.state = r3
            r12.ndpId = r11
            com.android.server.wifi.Clock r3 = r0.mClock
            long r3 = r3.getElapsedSinceBootMillis()
            r12.startTimestamp = r3
            com.android.server.wifi.aware.WifiAwareStateManager r3 = r0.mMgr
            r4 = 1
            java.lang.String r6 = r12.interfaceName
            android.net.wifi.aware.WifiAwareNetworkSpecifier r5 = r12.networkSpecifier
            byte[] r7 = r5.pmk
            android.net.wifi.aware.WifiAwareNetworkSpecifier r5 = r12.networkSpecifier
            java.lang.String r8 = r5.passphrase
            android.net.wifi.aware.WifiAwareNetworkSpecifier r5 = r12.networkSpecifier
            int r5 = r5.port
            android.net.wifi.aware.WifiAwareNetworkSpecifier r9 = r12.networkSpecifier
            int r9 = r9.transportProtocol
            byte[] r9 = com.android.server.wifi.aware.WifiAwareDataPathStateManager.NetworkInformationData.buildTlv(r5, r9)
            android.net.wifi.aware.WifiAwareNetworkSpecifier r5 = r12.networkSpecifier
            boolean r10 = r5.isOutOfBand()
            r5 = r19
            r3.respondToDataPathRequest(r4, r5, r6, r7, r8, r9, r10)
            return r13
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.aware.WifiAwareDataPathStateManager.onDataPathRequest(int, byte[], int, byte[]):android.net.wifi.aware.WifiAwareNetworkSpecifier");
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v14, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v4, resolved type: android.net.wifi.aware.WifiAwareNetworkSpecifier} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v15, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r2v2, resolved type: com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onRespondToDataPathRequest(int r7, boolean r8, int r9) {
        /*
            r6 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "onRespondToDataPathRequest: ndpId="
            r0.append(r1)
            r0.append(r7)
            java.lang.String r1 = ", success="
            r0.append(r1)
            r0.append(r8)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "WifiAwareDataPathStMgr"
            android.util.Log.v(r1, r0)
            r0 = 0
            r2 = 0
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r3 = r6.mNetworkRequestsCache
            java.util.Set r3 = r3.entrySet()
            java.util.Iterator r3 = r3.iterator()
        L_0x002a:
            boolean r4 = r3.hasNext()
            if (r4 == 0) goto L_0x0050
            java.lang.Object r4 = r3.next()
            java.util.Map$Entry r4 = (java.util.Map.Entry) r4
            java.lang.Object r5 = r4.getValue()
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r5 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r5
            int r5 = r5.ndpId
            if (r5 != r7) goto L_0x004f
            java.lang.Object r3 = r4.getKey()
            r0 = r3
            android.net.wifi.aware.WifiAwareNetworkSpecifier r0 = (android.net.wifi.aware.WifiAwareNetworkSpecifier) r0
            java.lang.Object r3 = r4.getValue()
            r2 = r3
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation r2 = (com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation) r2
            goto L_0x0050
        L_0x004f:
            goto L_0x002a
        L_0x0050:
            if (r2 != 0) goto L_0x007d
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onRespondToDataPathRequest: can't find a request with specified ndpId="
            r3.append(r4)
            r3.append(r7)
            java.lang.String r3 = r3.toString()
            android.util.Log.w(r1, r3)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "onRespondToDataPathRequest: network request cache = "
            r3.append(r4)
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r4 = r6.mNetworkRequestsCache
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.v(r1, r3)
            return
        L_0x007d:
            java.lang.String r3 = "onRespondToDataPathRequest: request "
            if (r8 != 0) goto L_0x00b3
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r3)
            r4.append(r0)
            java.lang.String r3 = " failed responding"
            r4.append(r3)
            java.lang.String r3 = r4.toString()
            android.util.Log.w(r1, r3)
            com.android.server.wifi.aware.WifiAwareStateManager r1 = r6.mMgr
            r1.endDataPath(r7)
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r1 = r6.mNetworkRequestsCache
            r1.remove(r0)
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$WifiAwareNetworkFactory r1 = r6.mNetworkFactory
            r1.letAppKnowThatRequestsAreUnavailable(r2)
            com.android.server.wifi.aware.WifiAwareMetrics r1 = r6.mAwareMetrics
            boolean r3 = r0.isOutOfBand()
            long r4 = r2.startTimestamp
            r1.recordNdpStatus(r9, r3, r4)
            return
        L_0x00b3:
            int r4 = r2.state
            r5 = 105(0x69, float:1.47E-43)
            if (r4 == r5) goto L_0x00e5
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r3)
            r4.append(r0)
            java.lang.String r3 = " is incorrect state="
            r4.append(r3)
            int r3 = r2.state
            r4.append(r3)
            java.lang.String r3 = r4.toString()
            android.util.Log.w(r1, r3)
            com.android.server.wifi.aware.WifiAwareStateManager r1 = r6.mMgr
            r1.endDataPath(r7)
            java.util.Map<android.net.wifi.aware.WifiAwareNetworkSpecifier, com.android.server.wifi.aware.WifiAwareDataPathStateManager$AwareNetworkRequestInformation> r1 = r6.mNetworkRequestsCache
            r1.remove(r0)
            com.android.server.wifi.aware.WifiAwareDataPathStateManager$WifiAwareNetworkFactory r1 = r6.mNetworkFactory
            r1.letAppKnowThatRequestsAreUnavailable(r2)
            return
        L_0x00e5:
            r1 = 101(0x65, float:1.42E-43)
            r2.state = r1
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.aware.WifiAwareDataPathStateManager.onRespondToDataPathRequest(int, boolean, int):void");
    }

    public WifiAwareNetworkSpecifier onDataPathConfirm(int ndpId, byte[] mac, boolean accept, int reason, byte[] message, List<NanDataPathChannelInfo> channelInfo) {
        NetworkInformationData.ParsedResults peerServerInfo;
        int i = ndpId;
        boolean z = accept;
        int i2 = reason;
        byte[] bArr = message;
        List<NanDataPathChannelInfo> list = channelInfo;
        StringBuilder sb = new StringBuilder();
        sb.append("onDataPathConfirm: ndpId=");
        sb.append(i);
        sb.append(", mac=");
        sb.append(String.valueOf(HexEncoding.encode(mac)));
        sb.append(", accept=");
        sb.append(z);
        sb.append(", reason=");
        sb.append(i2);
        sb.append(", message.length=");
        sb.append(bArr == null ? 0 : bArr.length);
        sb.append(", channelInfo=");
        sb.append(list);
        Log.v(TAG, sb.toString());
        Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
        if (nnriE == null) {
            Log.w(TAG, "onDataPathConfirm: network request not found for ndpId=" + i);
            if (z) {
                this.mMgr.endDataPath(i);
            }
            return null;
        }
        WifiAwareNetworkSpecifier networkSpecifier = nnriE.getKey();
        AwareNetworkRequestInformation nnri = nnriE.getValue();
        if (nnri.state != 101) {
            Log.w(TAG, "onDataPathConfirm: invalid state=" + nnri.state);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri);
            if (z) {
                this.mMgr.endDataPath(i);
            }
            return networkSpecifier;
        } else if (z) {
            nnri.state = 102;
            nnri.peerDataMac = mac;
            nnri.channelInfo = list;
            NetworkInfo networkInfo = new NetworkInfo(-1, 0, NETWORK_TAG, "");
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(sNetworkCapabilitiesFilter);
            LinkProperties linkProperties = new LinkProperties();
            if (!isInterfaceUpAndUsedByAnotherNdp(nnri)) {
                try {
                    this.mNwService.setInterfaceUp(nnri.interfaceName);
                    this.mNwService.enableIpv6(nnri.interfaceName);
                } catch (Exception e) {
                    Log.e(TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't configure network - " + e);
                    declareUnfullfillableAndEndDp(nnri, i);
                    return networkSpecifier;
                }
            } else {
                Log.v(TAG, "onDataPathConfirm: interface already configured: " + nnri.interfaceName);
            }
            if (nnri.networkSpecifier.role == 0 && (peerServerInfo = NetworkInformationData.parseTlv(message)) != null) {
                if (peerServerInfo.port != 0) {
                    nnri.peerPort = peerServerInfo.port;
                }
                if (peerServerInfo.transportProtocol != -1) {
                    nnri.peerTransportProtocol = peerServerInfo.transportProtocol;
                }
                if (peerServerInfo.ipv6Override != null) {
                    nnri.peerIpv6Override = peerServerInfo.ipv6Override;
                }
            }
            try {
                if (nnri.peerIpv6Override == null) {
                    nnri.peerIpv6 = Inet6Address.getByAddress((String) null, MacAddress.fromBytes(mac).getLinkLocalIpv6FromEui48Mac().getAddress(), NetworkInterface.getByName(nnri.interfaceName));
                } else {
                    byte[] addr = new byte[16];
                    addr[0] = -2;
                    addr[1] = Byte.MIN_VALUE;
                    addr[8] = nnri.peerIpv6Override[0];
                    addr[9] = nnri.peerIpv6Override[1];
                    addr[10] = nnri.peerIpv6Override[2];
                    addr[11] = nnri.peerIpv6Override[3];
                    addr[12] = nnri.peerIpv6Override[4];
                    addr[13] = nnri.peerIpv6Override[5];
                    addr[14] = nnri.peerIpv6Override[6];
                    addr[15] = nnri.peerIpv6Override[7];
                    nnri.peerIpv6 = Inet6Address.getByAddress((String) null, addr, NetworkInterface.getByName(nnri.interfaceName));
                }
            } catch (SocketException | UnknownHostException e2) {
                Log.e(TAG, "onDataPathConfirm: error obtaining scoped IPv6 address -- " + e2);
                nnri.peerIpv6 = null;
            }
            if (nnri.peerIpv6 != null) {
                networkCapabilities.setTransportInfo(new WifiAwareNetworkInfo(nnri.peerIpv6, nnri.peerPort, nnri.peerTransportProtocol));
            }
            Log.v(TAG, "onDataPathConfirm: AwareNetworkInfo=" + networkCapabilities.getTransportInfo());
            NetworkInterfaceWrapper networkInterfaceWrapper = this.mNiWrapper;
            Set<NetworkRequest> set = nnri.equivalentRequests;
            String str = NETWORK_TAG;
            String str2 = "";
            if (!networkInterfaceWrapper.configureAgentProperties(nnri, set, ndpId, networkInfo, networkCapabilities, linkProperties)) {
                declareUnfullfillableAndEndDp(nnri, i);
                return networkSpecifier;
            }
            Looper looper = this.mLooper;
            Context context = this.mContext;
            String str3 = AGENT_TAG_PREFIX + nnri.ndpId;
            NetworkInfo networkInfo2 = new NetworkInfo(-1, 0, str, str2);
            Looper looper2 = looper;
            AwareNetworkRequestInformation nnri2 = nnri;
            Context context2 = context;
            WifiAwareNetworkSpecifier networkSpecifier2 = networkSpecifier;
            int i3 = i2;
            nnri2.networkAgent = new WifiAwareNetworkAgent(this, looper2, context2, str3, networkInfo2, networkCapabilities, linkProperties, 1, nnri2);
            nnri2.startValidationTimestamp = this.mClock.getElapsedSinceBootMillis();
            lambda$handleAddressValidation$0$WifiAwareDataPathStateManager(nnri2, linkProperties, networkInfo, ndpId, networkSpecifier2.isOutOfBand());
            return networkSpecifier2;
        } else {
            AwareNetworkRequestInformation nnri3 = nnri;
            WifiAwareNetworkSpecifier networkSpecifier3 = networkSpecifier;
            int i4 = i2;
            Log.v(TAG, "onDataPathConfirm: data-path for networkSpecifier=" + networkSpecifier3 + " rejected - reason=" + i4);
            this.mNetworkRequestsCache.remove(networkSpecifier3);
            this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri3);
            this.mAwareMetrics.recordNdpStatus(i4, networkSpecifier3.isOutOfBand(), nnri3.startTimestamp);
            return networkSpecifier3;
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: handleAddressValidation */
    public void lambda$handleAddressValidation$0$WifiAwareDataPathStateManager(AwareNetworkRequestInformation nnri, LinkProperties linkProperties, NetworkInfo networkInfo, int ndpId, boolean isOutOfBand) {
        AwareNetworkRequestInformation awareNetworkRequestInformation = nnri;
        int i = ndpId;
        if (this.mNiWrapper.isAddressUsable(linkProperties)) {
            this.mNiWrapper.sendAgentNetworkInfo(awareNetworkRequestInformation.networkAgent, networkInfo);
            this.mAwareMetrics.recordNdpStatus(0, isOutOfBand, awareNetworkRequestInformation.startTimestamp);
            awareNetworkRequestInformation.startTimestamp = this.mClock.getElapsedSinceBootMillis();
            this.mAwareMetrics.recordNdpCreation(awareNetworkRequestInformation.uid, this.mNetworkRequestsCache);
            mCountDataPath++;
            sendAwareDataPathChangedBroadcast();
            Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(i);
            if (nnriE != null) {
                int clientId = nnriE.getKey().clientId;
                WifiAwareClientState client = this.mMgr.getClient(clientId);
                this.mMgr.sendAwareBigdata(client != null ? client.getCallingPackage() : null, 4, clientId, -1, "", -1);
                return;
            }
            return;
        }
        NetworkInfo networkInfo2 = networkInfo;
        boolean z = isOutOfBand;
        if (this.mClock.getElapsedSinceBootMillis() - awareNetworkRequestInformation.startValidationTimestamp > RttServiceImpl.HAL_RANGING_TIMEOUT_MS) {
            Log.e(TAG, "Timed-out while waiting for IPv6 address to be usable");
            declareUnfullfillableAndEndDp(awareNetworkRequestInformation, i);
            return;
        }
        if (this.mDbg) {
            Log.d(TAG, "Failed address validation");
        }
        this.mHandler.postDelayed(new Runnable(nnri, linkProperties, networkInfo, ndpId, isOutOfBand) {
            private final /* synthetic */ WifiAwareDataPathStateManager.AwareNetworkRequestInformation f$1;
            private final /* synthetic */ LinkProperties f$2;
            private final /* synthetic */ NetworkInfo f$3;
            private final /* synthetic */ int f$4;
            private final /* synthetic */ boolean f$5;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
                this.f$5 = r6;
            }

            public final void run() {
                WifiAwareDataPathStateManager.this.lambda$handleAddressValidation$0$WifiAwareDataPathStateManager(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
            }
        }, 1000);
    }

    private void declareUnfullfillableAndEndDp(AwareNetworkRequestInformation nnri, int ndpId) {
        this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri);
        this.mMgr.endDataPath(ndpId);
        nnri.state = 106;
    }

    public void onDataPathEnd(int ndpId) {
        Log.v(TAG, "onDataPathEnd: ndpId=" + ndpId);
        Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
        if (nnriE == null) {
            Log.v(TAG, "onDataPathEnd: network request not found for ndpId=" + ndpId);
            return;
        }
        tearDownInterfaceIfPossible(nnriE.getValue());
        if (nnriE.getValue().state == 102 || nnriE.getValue().state == 106) {
            this.mAwareMetrics.recordNdpSessionDuration(nnriE.getValue().startTimestamp);
            String callingPackage = null;
            int clientId = nnriE.getKey().clientId;
            WifiAwareClientState client = this.mMgr.getClient(clientId);
            if (client != null) {
                callingPackage = client.getCallingPackage();
            }
            if (client == null || callingPackage == null) {
                callingPackage = this.mMgr.getDisconnectedCallingPackage(clientId);
            }
            String str = callingPackage;
            int i = clientId;
            this.mMgr.sendAwareBigdata(str, 5, i, -1, "", (this.mClock.getElapsedSinceBootMillis() - nnriE.getValue().startTimestamp) / 1000);
        }
        this.mNetworkRequestsCache.remove(nnriE.getKey());
        this.mNetworkFactory.tickleConnectivityIfWaiting();
        int i2 = mCountDataPath;
        if (i2 >= 1) {
            mCountDataPath = i2 - 1;
        } else {
            mCountDataPath = 0;
        }
        sendAwareDataPathChangedBroadcast();
    }

    public void onDataPathSchedUpdate(byte[] peerMac, List<Integer> ndpIds, List<NanDataPathChannelInfo> channelInfo) {
        Log.v(TAG, "onDataPathSchedUpdate: peerMac=" + MacAddress.fromBytes(peerMac).toString() + ", ndpIds=" + ndpIds + ", channelInfo=" + channelInfo);
        for (Integer intValue : ndpIds) {
            int ndpId = intValue.intValue();
            Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
            if (nnriE == null) {
                Log.e(TAG, "onDataPathSchedUpdate: ndpId=" + ndpId + " - not found");
            } else if (!Arrays.equals(peerMac, nnriE.getValue().peerDiscoveryMac)) {
                Log.e(TAG, "onDataPathSchedUpdate: ndpId=" + ndpId + ", report NMI=" + MacAddress.fromBytes(peerMac).toString() + " doesn't match NDP NMI=" + MacAddress.fromBytes(nnriE.getValue().peerDiscoveryMac).toString());
            } else {
                nnriE.getValue().channelInfo = channelInfo;
            }
        }
    }

    public void onAwareDownCleanupDataPaths() {
        Log.v(TAG, "onAwareDownCleanupDataPaths");
        Iterator<Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation>> it = this.mNetworkRequestsCache.entrySet().iterator();
        while (it.hasNext()) {
            tearDownInterfaceIfPossible((AwareNetworkRequestInformation) it.next().getValue());
            it.remove();
        }
        mCountDataPath = 0;
        this.mMgr.resetAwareTrafficPollToken();
        sendAwareDataPathChangedBroadcast();
    }

    public void handleDataPathTimeout(NetworkSpecifier networkSpecifier) {
        if (this.mDbg) {
            Log.v(TAG, "handleDataPathTimeout: networkSpecifier=" + networkSpecifier);
        }
        AwareNetworkRequestInformation nnri = this.mNetworkRequestsCache.remove(networkSpecifier);
        if (nnri != null) {
            this.mAwareMetrics.recordNdpStatus(1, nnri.networkSpecifier.isOutOfBand(), nnri.startTimestamp);
            this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri);
            this.mMgr.endDataPath(nnri.ndpId);
            nnri.state = 106;
        } else if (this.mDbg) {
            Log.v(TAG, "handleDataPathTimeout: network request not found for networkSpecifier=" + networkSpecifier);
        }
    }

    private class WifiAwareNetworkFactory extends NetworkFactory {
        private boolean mWaitingForTermination = false;

        WifiAwareNetworkFactory(Looper looper, Context context, NetworkCapabilities filter) {
            super(looper, context, WifiAwareDataPathStateManager.NETWORK_TAG, filter);
        }

        public void tickleConnectivityIfWaiting() {
            if (this.mWaitingForTermination) {
                Log.v(WifiAwareDataPathStateManager.TAG, "tickleConnectivityIfWaiting: was waiting!");
                this.mWaitingForTermination = false;
                reevaluateAllRequests();
            }
        }

        public boolean acceptRequest(NetworkRequest request, int score) {
            NetworkRequest networkRequest = request;
            Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + ", score=" + score);
            if (!WifiAwareDataPathStateManager.this.mMgr.isUsageEnabled() && !WifiAwareDataPathStateManager.this.mMgr.isRunningLocationWhiteList()) {
                Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " -- Aware disabled");
                return false;
            } else if (WifiAwareDataPathStateManager.this.mInterfaces.isEmpty()) {
                Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " -- No Aware interfaces are up");
                return false;
            } else {
                NetworkSpecifier networkSpecifierBase = networkRequest.networkCapabilities.getNetworkSpecifier();
                if (!(networkSpecifierBase instanceof WifiAwareNetworkSpecifier)) {
                    Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " - not a WifiAwareNetworkSpecifier");
                    return false;
                }
                WifiAwareNetworkSpecifier networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierBase;
                AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
                if (nnri != null) {
                    Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " - already in cache with state=" + nnri.state);
                    if (nnri.state != 106) {
                        return true;
                    }
                    this.mWaitingForTermination = true;
                    return false;
                }
                AwareNetworkRequestInformation nnri2 = AwareNetworkRequestInformation.processNetworkSpecifier(request, networkSpecifier, WifiAwareDataPathStateManager.this.mMgr, WifiAwareDataPathStateManager.this.mWifiPermissionsUtil, WifiAwareDataPathStateManager.this.mPermissionsWrapper, WifiAwareDataPathStateManager.this.mAllowNdpResponderFromAnyOverride);
                if (nnri2 == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " - can't parse network specifier");
                    releaseRequestAsUnfulfillableByAnyFactory(request);
                    return false;
                }
                Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> primaryRequest = WifiAwareDataPathStateManager.this.getNetworkRequestByCanonicalDescriptor(nnri2.getCanonicalDescriptor());
                if (primaryRequest != null) {
                    Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + ", already has a primary request=" + primaryRequest.getKey() + " with state=" + primaryRequest.getValue().state);
                    if (primaryRequest.getValue().state == 106) {
                        this.mWaitingForTermination = true;
                    } else {
                        primaryRequest.getValue().updateToSupportNewRequest(networkRequest);
                    }
                    return false;
                }
                WifiAwareDataPathStateManager.this.mNetworkRequestsCache.put(networkSpecifier, nnri2);
                return true;
            }
        }

        /* access modifiers changed from: protected */
        public void needNetworkFor(NetworkRequest networkRequest, int score) {
            NetworkRequest networkRequest2 = networkRequest;
            Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.needNetworkFor: networkRequest=" + networkRequest2 + ", score=" + score);
            NetworkSpecifier networkSpecifierObj = networkRequest2.networkCapabilities.getNetworkSpecifier();
            WifiAwareNetworkSpecifier networkSpecifier = null;
            if (networkSpecifierObj instanceof WifiAwareNetworkSpecifier) {
                networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierObj;
            }
            AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
            if (nnri == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.needNetworkFor: networkRequest=" + networkRequest2 + " not in cache!?");
            } else if (nnri.state != 100) {
                Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.needNetworkFor: networkRequest=" + networkRequest2 + " - already in progress");
            } else if (nnri.networkSpecifier.role == 0) {
                nnri.interfaceName = WifiAwareDataPathStateManager.this.selectInterfaceForRequest(nnri);
                if (nnri.interfaceName == null) {
                    Log.w(WifiAwareDataPathStateManager.TAG, "needNetworkFor: request " + networkSpecifier + " no interface available");
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(networkSpecifier);
                    letAppKnowThatRequestsAreUnavailable(nnri);
                    return;
                }
                AwareNetworkRequestInformation nnri2 = nnri;
                WifiAwareDataPathStateManager.this.mMgr.initiateDataPathSetup(networkSpecifier, nnri.peerInstanceId, 0, WifiAwareDataPathStateManager.this.selectChannelForRequest(nnri), nnri.peerDiscoveryMac, nnri.interfaceName, nnri.networkSpecifier.pmk, nnri.networkSpecifier.passphrase, nnri.networkSpecifier.isOutOfBand(), (byte[]) null);
                nnri2.state = 103;
                nnri2.startTimestamp = WifiAwareDataPathStateManager.this.mClock.getElapsedSinceBootMillis();
            } else {
                nnri.state = 104;
            }
        }

        /* access modifiers changed from: protected */
        public void releaseNetworkFor(NetworkRequest networkRequest) {
            Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=" + networkRequest);
            NetworkSpecifier networkSpecifierObj = networkRequest.networkCapabilities.getNetworkSpecifier();
            WifiAwareNetworkSpecifier networkSpecifier = null;
            if (networkSpecifierObj instanceof WifiAwareNetworkSpecifier) {
                networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierObj;
            }
            AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
            if (nnri == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=" + networkRequest + " not in cache!?");
            } else if (nnri.networkAgent != null) {
                Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=" + networkRequest + ", nnri=" + nnri + ": agent already created - deferring ending data-path to agent.unwanted()");
            } else {
                nnri.removeSupportForRequest(networkRequest);
                if (nnri.equivalentRequests.isEmpty()) {
                    Log.v(WifiAwareDataPathStateManager.TAG, "releaseNetworkFor: there are no further requests, networkRequest=" + networkRequest);
                    if (nnri.ndpId != 0) {
                        Log.v(WifiAwareDataPathStateManager.TAG, "releaseNetworkFor: in progress NDP being terminated");
                        WifiAwareDataPathStateManager.this.mMgr.endDataPath(nnri.ndpId);
                        nnri.state = 106;
                        return;
                    }
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(networkSpecifier);
                    if (nnri.networkAgent != null) {
                        letAppKnowThatRequestsAreUnavailable(nnri);
                        return;
                    }
                    return;
                }
                Log.v(WifiAwareDataPathStateManager.TAG, "releaseNetworkFor: equivalent requests exist - not terminating networkRequest=" + networkRequest);
            }
        }

        /* access modifiers changed from: package-private */
        public void letAppKnowThatRequestsAreUnavailable(AwareNetworkRequestInformation nnri) {
            for (NetworkRequest nr : nnri.equivalentRequests) {
                releaseRequestAsUnfulfillableByAnyFactory(nr);
            }
        }
    }

    @VisibleForTesting
    public class WifiAwareNetworkAgent extends NetworkAgent {
        private AwareNetworkRequestInformation mAwareNetworkRequestInfo;
        private NetworkInfo mNetworkInfo;
        final /* synthetic */ WifiAwareDataPathStateManager this$0;

        /* JADX INFO: super call moved to the top of the method (can break code semantics) */
        WifiAwareNetworkAgent(WifiAwareDataPathStateManager this$02, Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, AwareNetworkRequestInformation anri) {
            super(looper, context, logTag, ni, nc, lp, score);
            this.this$0 = this$02;
            this.mNetworkInfo = ni;
            this.mAwareNetworkRequestInfo = anri;
        }

        /* access modifiers changed from: protected */
        public void unwanted() {
            Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkAgent.unwanted: request=" + this.mAwareNetworkRequestInfo);
            this.this$0.mMgr.endDataPath(this.mAwareNetworkRequestInfo.ndpId);
            this.mAwareNetworkRequestInfo.state = 106;
        }

        /* access modifiers changed from: package-private */
        public void reconfigureAgentAsDisconnected() {
            Log.v(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkAgent.reconfigureAgentAsDisconnected: request=" + this.mAwareNetworkRequestInfo);
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, (String) null, "");
            sendNetworkInfo(this.mNetworkInfo);
        }
    }

    private void tearDownInterfaceIfPossible(AwareNetworkRequestInformation nnri) {
        Log.v(TAG, "tearDownInterfaceIfPossible: nnri=" + nnri);
        if (!TextUtils.isEmpty(nnri.interfaceName)) {
            if (isInterfaceUpAndUsedByAnotherNdp(nnri)) {
                Log.v(TAG, "tearDownInterfaceIfPossible: interfaceName=" + nnri.interfaceName + ", still in use - not turning down");
            } else {
                try {
                    this.mNwService.setInterfaceDown(nnri.interfaceName);
                } catch (Exception e) {
                    Log.e(TAG, "tearDownInterfaceIfPossible: nnri=" + nnri + ": can't bring interface down - " + e);
                }
            }
        }
        if (nnri.networkAgent == null) {
            this.mNetworkFactory.letAppKnowThatRequestsAreUnavailable(nnri);
        } else {
            nnri.networkAgent.reconfigureAgentAsDisconnected();
        }
    }

    private boolean isInterfaceUpAndUsedByAnotherNdp(AwareNetworkRequestInformation nri) {
        for (AwareNetworkRequestInformation lnri : this.mNetworkRequestsCache.values()) {
            if (lnri != nri && nri.interfaceName.equals(lnri.interfaceName)) {
                if (lnri.state == 102 || lnri.state == 106) {
                    return true;
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public String selectInterfaceForRequest(AwareNetworkRequestInformation req) {
        SortedSet<String> potential = new TreeSet<>(this.mInterfaces);
        Set<String> used = new HashSet<>();
        String reuseIface = null;
        Log.v(TAG, "selectInterfaceForRequest: req=" + req + ", mNetworkRequestsCache=" + this.mNetworkRequestsCache);
        for (AwareNetworkRequestInformation nnri : this.mNetworkRequestsCache.values()) {
            if (nnri != req && Arrays.equals(req.peerDiscoveryMac, nnri.peerDiscoveryMac)) {
                used.add(nnri.interfaceName);
                reuseIface = nnri.interfaceName;
            }
        }
        Log.v(TAG, "selectInterfaceForRequest: potential=" + potential + ", used=" + used);
        for (String ifName : potential) {
            if (!used.contains(ifName)) {
                return ifName;
            }
        }
        if (reuseIface != null) {
            Log.i(TAG, "selectInterfaceForRequest: no interfaces available!, Select used=" + reuseIface);
            return reuseIface;
        }
        Log.e(TAG, "selectInterfaceForRequest: req=" + req + " - no interfaces available!");
        return null;
    }

    /* access modifiers changed from: private */
    public int selectChannelForRequest(AwareNetworkRequestInformation req) {
        return 2437;
    }

    @VisibleForTesting
    public static class AwareNetworkRequestInformation {
        static final int STATE_CONFIRMED = 102;
        static final int STATE_IDLE = 100;
        static final int STATE_INITIATOR_WAIT_FOR_REQUEST_RESPONSE = 103;
        static final int STATE_RESPONDER_WAIT_FOR_REQUEST = 104;
        static final int STATE_RESPONDER_WAIT_FOR_RESPOND_RESPONSE = 105;
        static final int STATE_TERMINATING = 106;
        static final int STATE_WAIT_FOR_CONFIRM = 101;
        public List<NanDataPathChannelInfo> channelInfo;
        public Set<NetworkRequest> equivalentRequests = new HashSet();
        public String interfaceName;
        public int ndpId = 0;
        public WifiAwareNetworkAgent networkAgent;
        public WifiAwareNetworkSpecifier networkSpecifier;
        public byte[] peerDataMac;
        public byte[] peerDiscoveryMac = null;
        public int peerInstanceId = 0;
        public Inet6Address peerIpv6;
        public byte[] peerIpv6Override = null;
        public int peerPort = 0;
        public int peerTransportProtocol = -1;
        public int pubSubId = 0;
        public long startTimestamp = 0;
        public long startValidationTimestamp = 0;
        public int state;
        public int uid;

        /* access modifiers changed from: package-private */
        public void updateToSupportNewRequest(NetworkRequest ns) {
            Log.v(WifiAwareDataPathStateManager.TAG, "updateToSupportNewRequest: ns=" + ns);
            if (this.equivalentRequests.add(ns) && this.state == 102) {
                WifiAwareNetworkAgent wifiAwareNetworkAgent = this.networkAgent;
                if (wifiAwareNetworkAgent == null) {
                    Log.wtf(WifiAwareDataPathStateManager.TAG, "updateToSupportNewRequest: null agent in CONFIRMED state!?");
                } else {
                    wifiAwareNetworkAgent.sendNetworkCapabilities(getNetworkCapabilities());
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void removeSupportForRequest(NetworkRequest ns) {
            Log.v(WifiAwareDataPathStateManager.TAG, "removeSupportForRequest: ns=" + ns);
            this.equivalentRequests.remove(ns);
        }

        private NetworkCapabilities getNetworkCapabilities() {
            NetworkCapabilities nc = new NetworkCapabilities(WifiAwareDataPathStateManager.sNetworkCapabilitiesFilter);
            nc.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) ((List) this.equivalentRequests.stream().map(C0489xf449489d.INSTANCE).collect(Collectors.toList())).toArray(new WifiAwareNetworkSpecifier[0])));
            Inet6Address inet6Address = this.peerIpv6;
            if (inet6Address != null) {
                nc.setTransportInfo(new WifiAwareNetworkInfo(inet6Address, this.peerPort, this.peerTransportProtocol));
            }
            return nc;
        }

        /* access modifiers changed from: package-private */
        public CanonicalConnectionInfo getCanonicalDescriptor() {
            return new CanonicalConnectionInfo(this.peerDiscoveryMac, this.networkSpecifier.pmk, this.networkSpecifier.sessionId, this.networkSpecifier.passphrase);
        }

        static AwareNetworkRequestInformation processNetworkSpecifier(NetworkRequest request, WifiAwareNetworkSpecifier ns, WifiAwareStateManager mgr, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionWrapper, boolean allowNdpResponderFromAnyOverride) {
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = ns;
            int pubSubId2 = 0;
            int peerInstanceId2 = 0;
            byte[] peerMac = wifiAwareNetworkSpecifier.peerMac;
            Log.v(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier);
            if (wifiAwareNetworkSpecifier.type < 0) {
                NetworkRequest networkRequest = request;
                WifiAwareStateManager wifiAwareStateManager = mgr;
                WifiPermissionsUtil wifiPermissionsUtil2 = wifiPermissionsUtil;
                WifiPermissionsWrapper wifiPermissionsWrapper = permissionWrapper;
            } else if (wifiAwareNetworkSpecifier.type > 3) {
                NetworkRequest networkRequest2 = request;
                WifiAwareStateManager wifiAwareStateManager2 = mgr;
                WifiPermissionsUtil wifiPermissionsUtil3 = wifiPermissionsUtil;
                WifiPermissionsWrapper wifiPermissionsWrapper2 = permissionWrapper;
            } else if (wifiAwareNetworkSpecifier.role != 0 && wifiAwareNetworkSpecifier.role != 1) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid 'role' value");
                return null;
            } else if (wifiAwareNetworkSpecifier.role != 0 || wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 2) {
                WifiAwareClientState client = mgr.getClient(wifiAwareNetworkSpecifier.clientId);
                if (client == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- not client with this id -- clientId=" + wifiAwareNetworkSpecifier.clientId);
                    return null;
                }
                int uid2 = client.getUid();
                if (!allowNdpResponderFromAnyOverride) {
                    if (!(wifiPermissionsUtil.isTargetSdkLessThan(client.getCallingPackage(), 28) || wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 2)) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no ANY specifications allowed for this API level");
                        return null;
                    }
                } else {
                    WifiPermissionsUtil wifiPermissionsUtil4 = wifiPermissionsUtil;
                }
                if (wifiAwareNetworkSpecifier.port < 0) {
                    NetworkRequest networkRequest3 = request;
                    WifiPermissionsWrapper wifiPermissionsWrapper3 = permissionWrapper;
                } else if (wifiAwareNetworkSpecifier.transportProtocol < -1) {
                    NetworkRequest networkRequest4 = request;
                    WifiPermissionsWrapper wifiPermissionsWrapper4 = permissionWrapper;
                } else {
                    if (!(wifiAwareNetworkSpecifier.port == 0 && wifiAwareNetworkSpecifier.transportProtocol == -1)) {
                        if (wifiAwareNetworkSpecifier.role != 1) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- port/transportProtocol can only be specified on responder");
                            return null;
                        } else if (TextUtils.isEmpty(wifiAwareNetworkSpecifier.passphrase) && wifiAwareNetworkSpecifier.pmk == null) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- port/transportProtocol can only be specified on secure ndp");
                            return null;
                        }
                    }
                    if (wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 1) {
                        WifiAwareDiscoverySessionState session = client.getSession(wifiAwareNetworkSpecifier.sessionId);
                        if (session == null) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no session with this id -- sessionId=" + wifiAwareNetworkSpecifier.sessionId);
                            return null;
                        } else if ((session.isPublishSession() && wifiAwareNetworkSpecifier.role != 1) || (!session.isPublishSession() && wifiAwareNetworkSpecifier.role != 0)) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid role for session type");
                            return null;
                        } else if (wifiAwareNetworkSpecifier.type == 0) {
                            int pubSubId3 = session.getPubSubId();
                            WifiAwareDiscoverySessionState.PeerInfo peerInfo = session.getPeerInfo(wifiAwareNetworkSpecifier.peerId);
                            if (peerInfo == null) {
                                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no peer info associated with this peer id -- peerId=" + wifiAwareNetworkSpecifier.peerId);
                                return null;
                            }
                            peerInstanceId2 = peerInfo.mInstanceId;
                            try {
                                peerMac = peerInfo.mMac;
                                if (peerMac != null) {
                                    if (peerMac.length == 6) {
                                        pubSubId2 = pubSubId3;
                                    }
                                }
                                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid peer MAC address");
                                return null;
                            } catch (IllegalArgumentException e) {
                                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid peer MAC address -- e=" + e);
                                return null;
                            }
                        }
                    }
                    if (wifiAwareNetworkSpecifier.requestorUid != uid2) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- UID mismatch to clientId's uid=" + uid2);
                        return null;
                    }
                    if (wifiAwareNetworkSpecifier.pmk == null || wifiAwareNetworkSpecifier.pmk.length == 0) {
                        WifiPermissionsWrapper wifiPermissionsWrapper5 = permissionWrapper;
                    } else {
                        if (permissionWrapper.getUidPermission("android.permission.CONNECTIVITY_INTERNAL", wifiAwareNetworkSpecifier.requestorUid) != 0) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- UID doesn't have permission to use PMK API");
                            return null;
                        }
                    }
                    if (!TextUtils.isEmpty(wifiAwareNetworkSpecifier.passphrase) && !WifiAwareUtils.validatePassphrase(wifiAwareNetworkSpecifier.passphrase)) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- invalid passphrase length: " + wifiAwareNetworkSpecifier.passphrase.length());
                        return null;
                    } else if (wifiAwareNetworkSpecifier.pmk == null || WifiAwareUtils.validatePmk(wifiAwareNetworkSpecifier.pmk)) {
                        AwareNetworkRequestInformation nnri = new AwareNetworkRequestInformation();
                        nnri.state = 100;
                        nnri.uid = uid2;
                        nnri.pubSubId = pubSubId2;
                        nnri.peerInstanceId = peerInstanceId2;
                        nnri.peerDiscoveryMac = peerMac;
                        nnri.networkSpecifier = wifiAwareNetworkSpecifier;
                        NetworkRequest networkRequest5 = request;
                        nnri.equivalentRequests.add(request);
                        return nnri;
                    } else {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- invalid pmk length: " + wifiAwareNetworkSpecifier.pmk.length);
                        return null;
                    }
                }
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid port/transportProtocol");
                return null;
            } else {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid 'type' value for INITIATOR (only IB and OOB are permitted)");
                return null;
            }
            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + ", invalid 'type' value");
            return null;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("AwareNetworkRequestInformation: ");
            sb.append("state=");
            sb.append(this.state);
            sb.append(", ns=");
            sb.append(this.networkSpecifier);
            sb.append(", uid=");
            sb.append(this.uid);
            sb.append(", interfaceName=");
            sb.append(this.interfaceName);
            sb.append(", pubSubId=");
            sb.append(this.pubSubId);
            sb.append(", peerInstanceId=");
            sb.append(this.peerInstanceId);
            sb.append(", peerDiscoveryMac=");
            byte[] bArr = this.peerDiscoveryMac;
            String str2 = "";
            if (bArr == null) {
                str = str2;
            } else {
                str = String.valueOf(HexEncoding.encode(bArr));
            }
            sb.append(str);
            sb.append(", ndpId=");
            sb.append(this.ndpId);
            sb.append(", peerDataMac=");
            byte[] bArr2 = this.peerDataMac;
            if (bArr2 != null) {
                str2 = String.valueOf(HexEncoding.encode(bArr2));
            }
            sb.append(str2);
            sb.append(", peerIpv6=");
            sb.append(this.peerIpv6);
            sb.append(", peerPort=");
            sb.append(this.peerPort);
            sb.append(", peerTransportProtocol=");
            sb.append(this.peerTransportProtocol);
            sb.append(", startTimestamp=");
            sb.append(this.startTimestamp);
            sb.append(", channelInfo=");
            sb.append(this.channelInfo);
            sb.append(", equivalentSpecifiers=[");
            for (NetworkRequest nr : this.equivalentRequests) {
                sb.append(nr.toString());
                sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static class CanonicalConnectionInfo {
        public final String passphrase;
        public final byte[] peerDiscoveryMac;
        public final byte[] pmk;
        public final int sessionId;

        CanonicalConnectionInfo(byte[] peerDiscoveryMac2, byte[] pmk2, int sessionId2, String passphrase2) {
            this.peerDiscoveryMac = peerDiscoveryMac2;
            this.pmk = pmk2;
            this.sessionId = sessionId2;
            this.passphrase = passphrase2;
        }

        public boolean matches(CanonicalConnectionInfo other) {
            byte[] bArr = other.peerDiscoveryMac;
            return (bArr == null || Arrays.equals(this.peerDiscoveryMac, bArr)) && Arrays.equals(this.pmk, other.pmk) && TextUtils.equals(this.passphrase, other.passphrase) && (TextUtils.isEmpty(this.passphrase) || this.sessionId == other.sessionId);
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("CanonicalConnectionInfo: [");
            sb.append("peerDiscoveryMac=");
            byte[] bArr = this.peerDiscoveryMac;
            String str2 = "";
            if (bArr == null) {
                str = str2;
            } else {
                str = String.valueOf(HexEncoding.encode(bArr));
            }
            sb.append(str);
            sb.append(", pmk=");
            sb.append(this.pmk == null ? str2 : "*");
            sb.append(", sessionId=");
            sb.append(this.sessionId);
            sb.append(", passphrase=");
            if (this.passphrase != null) {
                str2 = "*";
            }
            sb.append(str2);
            sb.append("]");
            return sb.toString();
        }
    }

    @VisibleForTesting
    public class NetworkInterfaceWrapper {
        public NetworkInterfaceWrapper() {
        }

        public boolean configureAgentProperties(AwareNetworkRequestInformation nnri, Set<NetworkRequest> networkRequests, int ndpId, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            InetAddress linkLocal = null;
            try {
                NetworkInterface ni = NetworkInterface.getByName(nnri.interfaceName);
                if (ni == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't get network interface (null)");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                    nnri.state = 106;
                    return false;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (true) {
                    if (!addresses.hasMoreElements()) {
                        break;
                    }
                    InetAddress ip = addresses.nextElement();
                    if ((ip instanceof Inet6Address) && ip.isLinkLocalAddress()) {
                        linkLocal = ip;
                        break;
                    }
                }
                if (linkLocal == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": no link local addresses");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                    nnri.state = 106;
                    return false;
                }
                networkInfo.setIsAvailable(true);
                networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, (String) null, (String) null);
                networkCapabilities.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) ((List) networkRequests.stream().map(C0490x6231c9a9.INSTANCE).collect(Collectors.toList())).toArray(new WifiAwareNetworkSpecifier[0])));
                linkProperties.setInterfaceName(nnri.interfaceName);
                linkProperties.addLinkAddress(new LinkAddress(linkLocal, 64));
                linkProperties.addRoute(new RouteInfo(new IpPrefix("fe80::/64"), (InetAddress) null, nnri.interfaceName));
                return true;
            } catch (SocketException e) {
                Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't get network interface - " + e);
                WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                nnri.state = 106;
                return false;
            }
        }

        public boolean isAddressUsable(LinkProperties linkProperties) {
            InetAddress address = linkProperties.getLinkAddresses().get(0).getAddress();
            DatagramSocket testDatagramSocket = null;
            try {
                new DatagramSocket(0, address).close();
                return true;
            } catch (SocketException e) {
                if (WifiAwareDataPathStateManager.this.mDbg) {
                    Log.d(WifiAwareDataPathStateManager.TAG, "Can't create socket on address " + address + " -- " + e);
                }
                if (testDatagramSocket != null) {
                    testDatagramSocket.close();
                }
                return false;
            } catch (Throwable th) {
                if (testDatagramSocket != null) {
                    testDatagramSocket.close();
                }
                throw th;
            }
        }

        public void sendAgentNetworkInfo(WifiAwareNetworkAgent networkAgent, NetworkInfo networkInfo) {
            networkAgent.sendNetworkInfo(networkInfo);
        }
    }

    @VisibleForTesting
    public static class NetworkInformationData {
        static final int GENERIC_SERVICE_PROTOCOL_TYPE = 2;
        static final int IPV6_LL_TYPE = 0;
        static final int SERVICE_INFO_TYPE = 1;
        static final int SUB_TYPE_PORT = 0;
        static final int SUB_TYPE_TRANSPORT_PROTOCOL = 1;
        static final byte[] WFA_OUI = {80, 111, -102};

        public static byte[] buildTlv(int port, int transportProtocol) {
            if (port == 0 && transportProtocol == -1) {
                return null;
            }
            TlvBufferUtils.TlvConstructor tlvc = new TlvBufferUtils.TlvConstructor(1, 2);
            tlvc.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            tlvc.allocate(20);
            tlvc.putRawByteArray(WFA_OUI);
            tlvc.putRawByte((byte) 2);
            if (port != 0) {
                tlvc.putShort(0, (short) port);
            }
            if (transportProtocol != -1) {
                tlvc.putByte(1, (byte) transportProtocol);
            }
            byte[] subTypes = tlvc.getArray();
            tlvc.allocate(20);
            tlvc.putByteArray(1, subTypes);
            return tlvc.getArray();
        }

        static class ParsedResults {
            public byte[] ipv6Override = null;
            public int port = 0;
            public int transportProtocol = -1;

            ParsedResults(int port2, int transportProtocol2, byte[] ipv6Override2) {
                this.port = port2;
                this.transportProtocol = transportProtocol2;
                this.ipv6Override = ipv6Override2;
            }
        }

        public static ParsedResults parseTlv(byte[] tlvs) {
            int port = 0;
            int transportProtocol = -1;
            byte[] ipv6Override = null;
            try {
                TlvBufferUtils.TlvIterable tlvi = new TlvBufferUtils.TlvIterable(1, 2, tlvs);
                tlvi.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                Iterator it = tlvi.iterator();
                while (it.hasNext()) {
                    TlvBufferUtils.TlvElement tlve = (TlvBufferUtils.TlvElement) it.next();
                    int i = tlve.type;
                    if (i != 0) {
                        if (i != 1) {
                            Log.w(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: ignoring unknown T -- " + tlve.type);
                        } else {
                            Pair<Integer, Integer> serviceInfo = parseServiceInfoTlv(tlve.getRawData());
                            if (serviceInfo == null) {
                                return null;
                            }
                            port = ((Integer) serviceInfo.first).intValue();
                            transportProtocol = ((Integer) serviceInfo.second).intValue();
                        }
                    } else if (tlve.length != 8) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid IPv6 TLV -- length: " + tlve.length);
                        return null;
                    } else {
                        ipv6Override = tlve.getRawData();
                    }
                }
                return new ParsedResults(port, transportProtocol, ipv6Override);
            } catch (Exception e) {
                Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: error parsing TLV -- " + e);
                return null;
            }
        }

        private static Pair<Integer, Integer> parseServiceInfoTlv(byte[] tlv) {
            int port = 0;
            int transportProtocol = -1;
            if (tlv.length < 4) {
                Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid SERVICE_INFO_TYPE length");
                return null;
            }
            byte b = tlv[0];
            byte[] bArr = WFA_OUI;
            if (b != bArr[0] || tlv[1] != bArr[1] || tlv[2] != bArr[2]) {
                Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: unexpected OUI");
                return null;
            } else if (tlv[3] != 2) {
                Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid type -- " + tlv[3]);
                return null;
            } else {
                TlvBufferUtils.TlvIterable subTlvi = new TlvBufferUtils.TlvIterable(1, 2, Arrays.copyOfRange(tlv, 4, tlv.length));
                subTlvi.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                Iterator it = subTlvi.iterator();
                while (it.hasNext()) {
                    TlvBufferUtils.TlvElement subTlve = (TlvBufferUtils.TlvElement) it.next();
                    int i = subTlve.type;
                    if (i != 0) {
                        if (i != 1) {
                            Log.w(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: ignoring unknown SERVICE_INFO.T -- " + subTlve.type);
                        } else if (subTlve.length != 1) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid transport protocol TLV length -- " + subTlve.length);
                            return null;
                        } else {
                            transportProtocol = subTlve.getByte();
                            if (transportProtocol < 0) {
                                transportProtocol += 256;
                            }
                        }
                    } else if (subTlve.length != 2) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid port TLV length -- " + subTlve.length);
                        return null;
                    } else {
                        port = subTlve.getShort();
                        if (port < 0) {
                            port += 65536;
                        }
                        if (port == 0) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "NetworkInformationData: invalid port " + port);
                            return null;
                        }
                    }
                }
                return Pair.create(Integer.valueOf(port), Integer.valueOf(transportProtocol));
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareDataPathStateManager:");
        pw.println("  mInterfaces: " + this.mInterfaces);
        pw.println("  sNetworkCapabilitiesFilter: " + sNetworkCapabilitiesFilter);
        pw.println("  mNetworkRequestsCache: " + this.mNetworkRequestsCache);
        pw.println("  mNetworkFactory:");
        this.mNetworkFactory.dump(fd, pw, args);
    }

    public int getCountDataPath() {
        return mCountDataPath;
    }

    private String changeAwareInterfacesString() {
        Set<String> set = this.mInterfaces;
        if (set == null || set.isEmpty()) {
            return null;
        }
        return String.join("/", this.mInterfaces);
    }

    private void sendAwareDataPathChangedBroadcast() {
        Log.i(TAG, "sending aware datapath changed broadcast: ndp:" + mCountDataPath);
        Intent intent = new Intent("android.net.wifi.aware.datapath_changed");
        intent.addFlags(67108864);
        intent.putExtra("ndp", mCountDataPath);
        intent.putExtra("aware_interfaces", changeAwareInterfacesString());
        Context context = this.mContext;
        if (context != null) {
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public Set<String> getAllInterfaces() {
        if (this.mInterfaces == null) {
            Log.v(TAG, "[NAN] mInterfaces == null");
        }
        return this.mInterfaces;
    }
}
