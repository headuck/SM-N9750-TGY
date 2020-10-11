package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.NanBandSpecificConfig;
import android.hardware.wifi.V1_0.NanConfigRequest;
import android.hardware.wifi.V1_0.NanEnableRequest;
import android.hardware.wifi.V1_0.NanInitiateDataPathRequest;
import android.hardware.wifi.V1_0.NanPublishRequest;
import android.hardware.wifi.V1_0.NanRespondToDataPathIndicationRequest;
import android.hardware.wifi.V1_0.NanSubscribeRequest;
import android.hardware.wifi.V1_0.NanTransmitFollowupRequest;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiNanIface;
import android.hardware.wifi.V1_2.NanConfigRequestSupplemental;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.aware.WifiAwareShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import libcore.util.HexEncoding;

public class WifiAwareNativeApi implements WifiAwareShellCommand.DelegatedShellCommand {
    static final String PARAM_DISCOVERY_BEACON_INTERVAL_MS = "disc_beacon_interval_ms";
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_DEFAULT = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_IDLE = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_INACTIVE = 0;
    static final String PARAM_DW_24GHZ = "dw_24ghz";
    private static final int PARAM_DW_24GHZ_DEFAULT = 1;
    private static final int PARAM_DW_24GHZ_IDLE = 4;
    private static final int PARAM_DW_24GHZ_INACTIVE = 4;
    static final String PARAM_DW_5GHZ = "dw_5ghz";
    private static final int PARAM_DW_5GHZ_DEFAULT = 1;
    private static final int PARAM_DW_5GHZ_IDLE = 0;
    private static final int PARAM_DW_5GHZ_INACTIVE = 0;
    static final String PARAM_ENABLE_DW_EARLY_TERM = "enable_dw_early_term";
    private static final int PARAM_ENABLE_DW_EARLY_TERM_DEFAULT = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_IDLE = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_INACTIVE = 0;
    static final String PARAM_MAC_RANDOM_INTERVAL_SEC = "mac_random_interval_sec";
    private static final int PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT = 1800;
    static final String PARAM_MASTER_PREF = "master_pref";
    private static final int PARAM_MASTER_PREF_DEFAULT = 0;
    private static final int PARAM_MASTER_PREF_IDLE = 2;
    private static final int PARAM_MASTER_PREF_INACTIVE = 2;
    static final String PARAM_NUM_SS_IN_DISCOVERY = "num_ss_in_discovery";
    private static final int PARAM_NUM_SS_IN_DISCOVERY_DEFAULT = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_IDLE = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_INACTIVE = 0;
    private static final int PARAM_PRIVATE_COMMAND_ENABLE_MERGE_REQUEST = 1073741824;
    private static final int PARAM_PRIVATE_COMMAND_VENDOR = Integer.MIN_VALUE;
    static final String POWER_PARAM_DEFAULT_KEY = "default";
    static final String POWER_PARAM_IDLE_KEY = "idle";
    static final String POWER_PARAM_INACTIVE_KEY = "inactive";
    @VisibleForTesting
    static final String SERVICE_NAME_FOR_OOB_DATA_PATH = "Wi-Fi Aware Data Path";
    private static final String TAG = "WifiAwareNativeApi";
    private static final boolean VDBG = true;
    boolean mDbg = true;
    private final WifiAwareNativeManager mHal;
    private Map<String, Integer> mSettableParameters = new HashMap();
    private Map<String, Map<String, Integer>> mSettablePowerParameters = new HashMap();
    private SparseIntArray mTransactionIds;

    public WifiAwareNativeApi(WifiAwareNativeManager wifiAwareNativeManager) {
        this.mHal = wifiAwareNativeManager;
        onReset();
        this.mTransactionIds = new SparseIntArray();
    }

    private void recordTransactionId(int transactionId) {
        if (transactionId != 0) {
            int count = this.mTransactionIds.get(transactionId);
            if (count != 0) {
                Log.wtf(TAG, "Repeated transaction ID == " + transactionId);
            }
            this.mTransactionIds.append(transactionId, count + 1);
        }
    }

    public IWifiNanIface mockableCastTo_1_2(android.hardware.wifi.V1_0.IWifiNanIface iface) {
        return IWifiNanIface.castFrom(iface);
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(android.os.ShellCommand r17) {
        /*
            r16 = this;
            r1 = r16
            java.io.PrintWriter r2 = r17.getErrPrintWriter()
            java.lang.String r3 = r17.getNextArgRequired()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "onCommand: subCmd='"
            r0.append(r4)
            r0.append(r3)
            java.lang.String r4 = "'"
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            java.lang.String r5 = "WifiAwareNativeApi"
            android.util.Log.v(r5, r0)
            int r0 = r3.hashCode()
            r6 = 3
            r7 = 2
            r8 = 1
            r10 = -1
            switch(r0) {
                case -502265894: goto L_0x004f;
                case -287648818: goto L_0x0045;
                case 102230: goto L_0x003b;
                case 113762: goto L_0x0031;
                default: goto L_0x0030;
            }
        L_0x0030:
            goto L_0x0059
        L_0x0031:
            java.lang.String r0 = "set"
            boolean r0 = r3.equals(r0)
            if (r0 == 0) goto L_0x0030
            r0 = 0
            goto L_0x005a
        L_0x003b:
            java.lang.String r0 = "get"
            boolean r0 = r3.equals(r0)
            if (r0 == 0) goto L_0x0030
            r0 = r7
            goto L_0x005a
        L_0x0045:
            java.lang.String r0 = "get-power"
            boolean r0 = r3.equals(r0)
            if (r0 == 0) goto L_0x0030
            r0 = r6
            goto L_0x005a
        L_0x004f:
            java.lang.String r0 = "set-power"
            boolean r0 = r3.equals(r0)
            if (r0 == 0) goto L_0x0030
            r0 = r8
            goto L_0x005a
        L_0x0059:
            r0 = r10
        L_0x005a:
            java.lang.String r11 = "Can't convert value to integer -- '"
            java.lang.String r12 = "onCommand: name='"
            java.lang.String r13 = "Unknown parameter name -- '"
            if (r0 == 0) goto L_0x01f4
            java.lang.String r14 = "' in mode '"
            java.lang.String r15 = "', name='"
            java.lang.String r9 = "onCommand: mode='"
            if (r0 == r8) goto L_0x0148
            if (r0 == r7) goto L_0x00fc
            if (r0 == r6) goto L_0x0074
            java.lang.String r0 = "Unknown 'wifiaware native_api <cmd>'"
            r2.println(r0)
            return r10
        L_0x0074:
            java.lang.String r0 = r17.getNextArgRequired()
            java.lang.String r6 = r17.getNextArgRequired()
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r7.append(r9)
            r7.append(r0)
            r7.append(r15)
            r7.append(r6)
            r7.append(r4)
            java.lang.String r7 = r7.toString()
            android.util.Log.v(r5, r7)
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r5 = r1.mSettablePowerParameters
            boolean r5 = r5.containsKey(r0)
            if (r5 != 0) goto L_0x00b7
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r7 = "Unknown mode -- '"
            r5.append(r7)
            r5.append(r0)
            r5.append(r4)
            java.lang.String r4 = r5.toString()
            r2.println(r4)
            return r10
        L_0x00b7:
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r5 = r1.mSettablePowerParameters
            java.lang.Object r5 = r5.get(r0)
            java.util.Map r5 = (java.util.Map) r5
            boolean r5 = r5.containsKey(r6)
            if (r5 != 0) goto L_0x00e1
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r13)
            r5.append(r6)
            r5.append(r14)
            r5.append(r0)
            r5.append(r4)
            java.lang.String r4 = r5.toString()
            r2.println(r4)
            return r10
        L_0x00e1:
            java.io.PrintWriter r4 = r17.getOutPrintWriter()
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r5 = r1.mSettablePowerParameters
            java.lang.Object r5 = r5.get(r0)
            java.util.Map r5 = (java.util.Map) r5
            java.lang.Object r5 = r5.get(r6)
            java.lang.Integer r5 = (java.lang.Integer) r5
            int r5 = r5.intValue()
            r4.println(r5)
            r4 = 0
            return r4
        L_0x00fc:
            java.lang.String r0 = r17.getNextArgRequired()
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            r6.append(r12)
            r6.append(r0)
            r6.append(r4)
            java.lang.String r6 = r6.toString()
            android.util.Log.v(r5, r6)
            java.util.Map<java.lang.String, java.lang.Integer> r5 = r1.mSettableParameters
            boolean r5 = r5.containsKey(r0)
            if (r5 != 0) goto L_0x0133
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r13)
            r5.append(r0)
            r5.append(r4)
            java.lang.String r4 = r5.toString()
            r2.println(r4)
            return r10
        L_0x0133:
            java.io.PrintWriter r4 = r17.getOutPrintWriter()
            java.util.Map<java.lang.String, java.lang.Integer> r5 = r1.mSettableParameters
            java.lang.Object r5 = r5.get(r0)
            java.lang.Integer r5 = (java.lang.Integer) r5
            int r5 = r5.intValue()
            r4.println(r5)
            r4 = 0
            return r4
        L_0x0148:
            java.lang.String r6 = r17.getNextArgRequired()
            java.lang.String r7 = r17.getNextArgRequired()
            java.lang.String r8 = r17.getNextArgRequired()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r9)
            r0.append(r6)
            r0.append(r15)
            r0.append(r7)
            java.lang.String r9 = "', value='"
            r0.append(r9)
            r0.append(r8)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Log.v(r5, r0)
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r0 = r1.mSettablePowerParameters
            boolean r0 = r0.containsKey(r6)
            if (r0 != 0) goto L_0x0197
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r5 = "Unknown mode name -- '"
            r0.append(r5)
            r0.append(r6)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            r2.println(r0)
            return r10
        L_0x0197:
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r0 = r1.mSettablePowerParameters
            java.lang.Object r0 = r0.get(r6)
            java.util.Map r0 = (java.util.Map) r0
            boolean r0 = r0.containsKey(r7)
            if (r0 != 0) goto L_0x01c3
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r5 = "Unknown parameter name '"
            r0.append(r5)
            r0.append(r7)
            r0.append(r14)
            r0.append(r6)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            r2.println(r0)
            return r10
        L_0x01c3:
            java.lang.Integer r0 = java.lang.Integer.valueOf(r8)     // Catch:{ NumberFormatException -> 0x01dd }
            int r0 = r0.intValue()     // Catch:{ NumberFormatException -> 0x01dd }
            java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>> r4 = r1.mSettablePowerParameters
            java.lang.Object r4 = r4.get(r6)
            java.util.Map r4 = (java.util.Map) r4
            java.lang.Integer r5 = java.lang.Integer.valueOf(r0)
            r4.put(r7, r5)
            r4 = 0
            return r4
        L_0x01dd:
            r0 = move-exception
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r11)
            r5.append(r8)
            r5.append(r4)
            java.lang.String r4 = r5.toString()
            r2.println(r4)
            return r10
        L_0x01f4:
            java.lang.String r6 = r17.getNextArgRequired()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r12)
            r0.append(r6)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Log.v(r5, r0)
            java.util.Map<java.lang.String, java.lang.Integer> r0 = r1.mSettableParameters
            boolean r0 = r0.containsKey(r6)
            if (r0 != 0) goto L_0x022b
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r13)
            r0.append(r6)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            r2.println(r0)
            return r10
        L_0x022b:
            java.lang.String r7 = r17.getNextArgRequired()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r8 = "onCommand: valueStr='"
            r0.append(r8)
            r0.append(r7)
            r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Log.v(r5, r0)
            java.lang.Integer r0 = java.lang.Integer.valueOf(r7)     // Catch:{ NumberFormatException -> 0x025a }
            int r0 = r0.intValue()     // Catch:{ NumberFormatException -> 0x025a }
            java.util.Map<java.lang.String, java.lang.Integer> r4 = r1.mSettableParameters
            java.lang.Integer r5 = java.lang.Integer.valueOf(r0)
            r4.put(r6, r5)
            r4 = 0
            return r4
        L_0x025a:
            r0 = move-exception
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r11)
            r5.append(r7)
            r5.append(r4)
            java.lang.String r4 = r5.toString()
            r2.println(r4)
            return r10
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.aware.WifiAwareNativeApi.onCommand(android.os.ShellCommand):int");
    }

    public void onReset() {
        Map<String, Integer> defaultMap = new HashMap<>();
        defaultMap.put(PARAM_DW_24GHZ, 1);
        defaultMap.put(PARAM_DW_5GHZ, 1);
        defaultMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        defaultMap.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        defaultMap.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        defaultMap.put(PARAM_MASTER_PREF, 0);
        Map<String, Integer> inactiveMap = new HashMap<>();
        inactiveMap.put(PARAM_DW_24GHZ, 4);
        inactiveMap.put(PARAM_DW_5GHZ, 0);
        inactiveMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        inactiveMap.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        inactiveMap.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        inactiveMap.put(PARAM_MASTER_PREF, 2);
        Map<String, Integer> idleMap = new HashMap<>();
        idleMap.put(PARAM_DW_24GHZ, 4);
        idleMap.put(PARAM_DW_5GHZ, 0);
        idleMap.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        idleMap.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        idleMap.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        idleMap.put(PARAM_MASTER_PREF, 2);
        this.mSettablePowerParameters.put("default", defaultMap);
        this.mSettablePowerParameters.put(POWER_PARAM_INACTIVE_KEY, inactiveMap);
        this.mSettablePowerParameters.put(POWER_PARAM_IDLE_KEY, idleMap);
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT));
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        pw.println("  " + command);
        pw.println("    set <name> <value>: sets named parameter to value. Names: " + this.mSettableParameters.keySet());
        pw.println("    set-power <mode> <name> <value>: sets named power parameter to value. Modes: " + this.mSettablePowerParameters.keySet() + ", Names: " + this.mSettablePowerParameters.get("default").keySet());
        StringBuilder sb = new StringBuilder();
        sb.append("    get <name>: gets named parameter value. Names: ");
        sb.append(this.mSettableParameters.keySet());
        pw.println(sb.toString());
        pw.println("    get-power <mode> <name>: gets named parameter value. Modes: " + this.mSettablePowerParameters.keySet() + ", Names: " + this.mSettablePowerParameters.get("default").keySet());
    }

    public boolean getCapabilities(short transactionId) {
        if (this.mDbg) {
            Log.v(TAG, "getCapabilities: transactionId=" + transactionId);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "getCapabilities: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.getCapabilitiesRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "getCapabilities: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "getCapabilities: exception: " + e);
            return false;
        }
    }

    public boolean enableAndConfigure(short transactionId, ConfigRequest configRequest, boolean notifyIdentityChange, boolean initialConfiguration, boolean isInteractive, boolean isIdle) {
        WifiStatus status;
        short s = transactionId;
        ConfigRequest configRequest2 = configRequest;
        boolean z = notifyIdentityChange;
        boolean z2 = initialConfiguration;
        boolean z3 = isInteractive;
        boolean z4 = isIdle;
        if (this.mDbg) {
            Log.v(TAG, "enableAndConfigure: transactionId=" + s + ", configRequest=" + configRequest2 + ", notifyIdentityChange=" + z + ", initialConfiguration=" + z2 + ", isInteractive=" + z3 + ", isIdle=" + z4);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "enableAndConfigure: null interface");
            return false;
        }
        IWifiNanIface iface12 = mockableCastTo_1_2(iface);
        NanConfigRequestSupplemental configSupplemental12 = new NanConfigRequestSupplemental();
        if (iface12 != null) {
            Log.v(TAG, "HAL 1.2 detected");
            configSupplemental12.discoveryBeaconIntervalMs = 0;
            configSupplemental12.numberOfSpatialStreamsInDiscovery = 0;
            configSupplemental12.enableDiscoveryWindowEarlyTermination = false;
            configSupplemental12.enableRanging = true;
        }
        if (z2) {
            try {
                NanEnableRequest req = new NanEnableRequest();
                req.operateInBand[0] = true;
                req.operateInBand[1] = configRequest2.mSupport5gBand;
                req.hopCountMax = 2;
                req.configParams.masterPref = (byte) configRequest2.mMasterPreference;
                req.configParams.disableDiscoveryAddressChangeIndication = !z;
                req.configParams.disableStartedClusterIndication = !z;
                req.configParams.disableJoinedClusterIndication = false;
                req.configParams.includePublishServiceIdsInBeacon = true;
                req.configParams.numberOfPublishServiceIdsInBeacon = 0;
                req.configParams.includeSubscribeServiceIdsInBeacon = true;
                req.configParams.numberOfSubscribeServiceIdsInBeacon = 0;
                req.configParams.rssiWindowSize = 8;
                req.configParams.macAddressRandomizationIntervalSec = this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC).intValue();
                NanBandSpecificConfig config24 = new NanBandSpecificConfig();
                config24.rssiClose = 60;
                config24.rssiMiddle = 70;
                config24.rssiCloseProximity = 60;
                config24.dwellTimeMs = -56;
                config24.scanPeriodSec = 20;
                if (configRequest2.mDiscoveryWindowInterval[0] == -1) {
                    config24.validDiscoveryWindowIntervalVal = false;
                } else {
                    config24.validDiscoveryWindowIntervalVal = true;
                    config24.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[0];
                }
                req.configParams.bandSpecificConfig[0] = config24;
                NanBandSpecificConfig config5 = new NanBandSpecificConfig();
                config5.rssiClose = 60;
                config5.rssiMiddle = 75;
                config5.rssiCloseProximity = 60;
                config5.dwellTimeMs = -56;
                config5.scanPeriodSec = 20;
                NanBandSpecificConfig nanBandSpecificConfig = config24;
                if (configRequest2.mDiscoveryWindowInterval[1] == -1) {
                    config5.validDiscoveryWindowIntervalVal = false;
                } else {
                    config5.validDiscoveryWindowIntervalVal = true;
                    config5.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[1];
                }
                req.configParams.bandSpecificConfig[1] = config5;
                req.debugConfigs.validClusterIdVals = true;
                req.debugConfigs.clusterIdTopRangeVal = (short) configRequest2.mClusterHigh;
                req.debugConfigs.clusterIdBottomRangeVal = (short) configRequest2.mClusterLow;
                req.debugConfigs.validIntfAddrVal = false;
                req.debugConfigs.validOuiVal = false;
                req.debugConfigs.ouiVal = 0;
                req.debugConfigs.validRandomFactorForceVal = false;
                req.debugConfigs.randomFactorForceVal = 0;
                req.debugConfigs.validHopCountForceVal = false;
                req.debugConfigs.hopCountForceVal = 0;
                req.debugConfigs.validDiscoveryChannelVal = false;
                req.debugConfigs.discoveryChannelMhzVal[0] = 0;
                req.debugConfigs.discoveryChannelMhzVal[1] = 0;
                req.debugConfigs.validUseBeaconsInBandVal = false;
                req.debugConfigs.useBeaconsInBandVal[0] = true;
                req.debugConfigs.useBeaconsInBandVal[1] = true;
                req.debugConfigs.validUseSdfInBandVal = false;
                req.debugConfigs.useSdfInBandVal[0] = true;
                req.debugConfigs.useSdfInBandVal[1] = true;
                int masterPref = configRequest2.mMasterPreference;
                if (masterPref <= 1 || masterPref >= 255) {
                    masterPref = new Random().nextInt(126) + 2;
                }
                NanBandSpecificConfig nanBandSpecificConfig2 = config5;
                this.mSettablePowerParameters.get("default").put(PARAM_MASTER_PREF, Integer.valueOf(masterPref));
                updateConfigForPowerSettings(req.configParams, configSupplemental12, z3, z4);
                if (this.mDbg) {
                    Log.v(TAG, "enableAndConfigure: NAN Enable Request=" + req);
                }
                if (iface12 != null) {
                    status = iface12.enableRequest_1_2(s, req, configSupplemental12);
                } else {
                    status = iface.enableRequest(s, req);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "enableAndConfigure: exception: " + e);
                return false;
            }
        } else {
            NanConfigRequest req2 = new NanConfigRequest();
            req2.masterPref = (byte) configRequest2.mMasterPreference;
            req2.disableDiscoveryAddressChangeIndication = !z;
            req2.disableStartedClusterIndication = !z;
            req2.disableJoinedClusterIndication = false;
            req2.includePublishServiceIdsInBeacon = true;
            req2.numberOfPublishServiceIdsInBeacon = 0;
            req2.includeSubscribeServiceIdsInBeacon = true;
            req2.numberOfSubscribeServiceIdsInBeacon = 0;
            req2.rssiWindowSize = 8;
            req2.macAddressRandomizationIntervalSec = this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC).intValue();
            NanBandSpecificConfig config242 = new NanBandSpecificConfig();
            config242.rssiClose = 60;
            config242.rssiMiddle = 70;
            config242.rssiCloseProximity = 60;
            config242.dwellTimeMs = -56;
            config242.scanPeriodSec = 20;
            if (configRequest2.mDiscoveryWindowInterval[0] == -1) {
                config242.validDiscoveryWindowIntervalVal = false;
            } else {
                config242.validDiscoveryWindowIntervalVal = true;
                config242.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[0];
            }
            req2.bandSpecificConfig[0] = config242;
            NanBandSpecificConfig config52 = new NanBandSpecificConfig();
            config52.rssiClose = 60;
            config52.rssiMiddle = 75;
            config52.rssiCloseProximity = 60;
            config52.dwellTimeMs = -56;
            config52.scanPeriodSec = 20;
            if (configRequest2.mDiscoveryWindowInterval[1] == -1) {
                config52.validDiscoveryWindowIntervalVal = false;
            } else {
                config52.validDiscoveryWindowIntervalVal = true;
                config52.discoveryWindowIntervalVal = (byte) configRequest2.mDiscoveryWindowInterval[1];
            }
            req2.bandSpecificConfig[1] = config52;
            updateConfigForPowerSettings(req2, configSupplemental12, z3, z4);
            if (this.mDbg) {
                Log.v(TAG, "enableAndConfigure: NAN Enable Request=" + req2);
            }
            if (iface12 != null) {
                status = iface12.configRequest_1_2(s, req2, configSupplemental12);
            } else {
                status = iface.configRequest(s, req2);
            }
        }
        if (status.code == 0) {
            return true;
        }
        Log.e(TAG, "enableAndConfigure: error: " + statusString(status));
        return false;
    }

    public boolean enableClusterMergeConfigure(short transactionId, ConfigRequest configRequest, boolean notifyIdentityChange, boolean initialConfiguration, boolean isInteractive, boolean isIdle, int enable) {
        int privateCommand;
        int i = enable;
        int previousCommand = this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC).intValue();
        int privateCommand2 = PARAM_PRIVATE_COMMAND_VENDOR | previousCommand;
        if (i == 1) {
            privateCommand = privateCommand2 | PARAM_PRIVATE_COMMAND_ENABLE_MERGE_REQUEST;
        } else {
            privateCommand = privateCommand2;
        }
        Log.v(TAG, "PreviousCommand=" + previousCommand + " Private Command=" + privateCommand + " enable=" + i);
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(privateCommand));
        boolean ret = enableAndConfigure(transactionId, configRequest, notifyIdentityChange, false, isInteractive, isIdle);
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(previousCommand));
        return ret;
    }

    public boolean disable(short transactionId) {
        if (this.mDbg) {
            Log.d(TAG, "disable");
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "disable: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.disableRequest(transactionId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "disable: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "disable: exception: " + e);
            return false;
        }
    }

    public boolean publish(short transactionId, byte publishId, PublishConfig publishConfig) {
        if (this.mDbg) {
            Log.d(TAG, "publish: transactionId=" + transactionId + ", publishId=" + publishId + ", config=" + publishConfig);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "publish: null interface");
            return false;
        }
        NanPublishRequest req = new NanPublishRequest();
        req.baseConfigs.sessionId = publishId;
        req.baseConfigs.ttlSec = (short) publishConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = 1;
        req.baseConfigs.discoveryCount = 0;
        convertNativeByteArrayToArrayList(publishConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 2;
        convertNativeByteArrayToArrayList(publishConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(publishConfig.mMatchFilter, publishConfig.mPublishType == 0 ? req.baseConfigs.txMatchFilter : req.baseConfigs.rxMatchFilter);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = !publishConfig.mEnableTerminateNotification;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        req.autoAcceptDataPathRequests = false;
        req.baseConfigs.rangingRequired = publishConfig.mEnableRanging;
        req.baseConfigs.securityConfig.securityType = 0;
        req.publishType = publishConfig.mPublishType;
        req.txType = 0;
        try {
            WifiStatus status = iface.startPublishRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "publish: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "publish: exception: " + e);
            return false;
        }
    }

    public boolean subscribe(short transactionId, byte subscribeId, SubscribeConfig subscribeConfig) {
        if (this.mDbg) {
            Log.d(TAG, "subscribe: transactionId=" + transactionId + ", subscribeId=" + subscribeId + ", config=" + subscribeConfig);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "subscribe: null interface");
            return false;
        }
        NanSubscribeRequest req = new NanSubscribeRequest();
        req.baseConfigs.sessionId = subscribeId;
        req.baseConfigs.ttlSec = (short) subscribeConfig.mTtlSec;
        req.baseConfigs.discoveryWindowPeriod = 1;
        req.baseConfigs.discoveryCount = 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceName, req.baseConfigs.serviceName);
        req.baseConfigs.discoveryMatchIndicator = 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceSpecificInfo, req.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(subscribeConfig.mMatchFilter, subscribeConfig.mSubscribeType == 1 ? req.baseConfigs.txMatchFilter : req.baseConfigs.rxMatchFilter);
        req.baseConfigs.useRssiThreshold = false;
        req.baseConfigs.disableDiscoveryTerminationIndication = !subscribeConfig.mEnableTerminateNotification;
        req.baseConfigs.disableMatchExpirationIndication = true;
        req.baseConfigs.disableFollowupReceivedIndication = false;
        req.baseConfigs.rangingRequired = subscribeConfig.mMinDistanceMmSet || subscribeConfig.mMaxDistanceMmSet;
        req.baseConfigs.configRangingIndications = 0;
        if (subscribeConfig.mMinDistanceMmSet) {
            req.baseConfigs.distanceEgressCm = (short) Math.min(subscribeConfig.mMinDistanceMm / 10, 32767);
            req.baseConfigs.configRangingIndications |= 4;
        }
        if (subscribeConfig.mMaxDistanceMmSet) {
            req.baseConfigs.distanceIngressCm = (short) Math.min(subscribeConfig.mMaxDistanceMm / 10, 32767);
            req.baseConfigs.configRangingIndications |= 2;
        }
        req.baseConfigs.securityConfig.securityType = 0;
        req.subscribeType = subscribeConfig.mSubscribeType;
        try {
            WifiStatus status = iface.startSubscribeRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "subscribe: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "subscribe: exception: " + e);
            return false;
        }
    }

    public boolean sendMessage(short transactionId, byte pubSubId, int requestorInstanceId, byte[] dest, byte[] message, int messageId) {
        Object obj;
        int i;
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("sendMessage: transactionId=");
            sb.append(transactionId);
            sb.append(", pubSubId=");
            sb.append(pubSubId);
            sb.append(", requestorInstanceId=");
            sb.append(requestorInstanceId);
            sb.append(", dest=");
            sb.append(String.valueOf(HexEncoding.encode(dest)));
            sb.append(", messageId=");
            sb.append(messageId);
            sb.append(", message=");
            if (message == null) {
                obj = "<null>";
            } else {
                obj = HexEncoding.encode(message);
            }
            sb.append(obj);
            sb.append(", message.length=");
            if (message == null) {
                i = 0;
            } else {
                i = message.length;
            }
            sb.append(i);
            Log.d(TAG, sb.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "sendMessage: null interface");
            return false;
        }
        NanTransmitFollowupRequest req = new NanTransmitFollowupRequest();
        req.discoverySessionId = pubSubId;
        req.peerId = requestorInstanceId;
        copyArray(dest, req.addr);
        req.isHighPriority = false;
        req.shouldUseDiscoveryWindow = true;
        convertNativeByteArrayToArrayList(message, req.serviceSpecificInfo);
        req.disableFollowupResultIndication = false;
        try {
            WifiStatus status = iface.transmitFollowupRequest(transactionId, req);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "sendMessage: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage: exception: " + e);
            return false;
        }
    }

    public boolean stopPublish(short transactionId, byte pubSubId) {
        if (this.mDbg) {
            Log.d(TAG, "stopPublish: transactionId=" + transactionId + ", pubSubId=" + pubSubId);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopPublish: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.stopPublishRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "stopPublish: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopPublish: exception: " + e);
            return false;
        }
    }

    public boolean stopSubscribe(short transactionId, byte pubSubId) {
        if (this.mDbg) {
            Log.d(TAG, "stopSubscribe: transactionId=" + transactionId + ", pubSubId=" + pubSubId);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "stopSubscribe: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.stopSubscribeRequest(transactionId, pubSubId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "stopSubscribe: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopSubscribe: exception: " + e);
            return false;
        }
    }

    public boolean createAwareNetworkInterface(short transactionId, String interfaceName) {
        if (this.mDbg) {
            Log.v(TAG, "createAwareNetworkInterface: transactionId=" + transactionId + ", interfaceName=" + interfaceName);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "createAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.createDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "createAwareNetworkInterface: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "createAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean deleteAwareNetworkInterface(short transactionId, String interfaceName) {
        if (this.mDbg) {
            Log.v(TAG, "deleteAwareNetworkInterface: transactionId=" + transactionId + ", interfaceName=" + interfaceName);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "deleteAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.deleteDataInterfaceRequest(transactionId, interfaceName);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "deleteAwareNetworkInterface: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "deleteAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean initiateDataPath(short transactionId, int peerId, int channelRequestType, int channel, byte[] peer, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand, byte[] appInfo, Capabilities capabilities) {
        short s = transactionId;
        int i = peerId;
        int i2 = channelRequestType;
        int i3 = channel;
        String str = interfaceName;
        byte[] bArr = pmk;
        boolean z = isOutOfBand;
        byte[] bArr2 = appInfo;
        Capabilities capabilities2 = capabilities;
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("initiateDataPath: transactionId=");
            sb.append(s);
            sb.append(", peerId=");
            sb.append(i);
            sb.append(", channelRequestType=");
            sb.append(i2);
            sb.append(", channel=");
            sb.append(i3);
            sb.append(", peer=");
            sb.append(String.valueOf(HexEncoding.encode(peer)));
            sb.append(", interfaceName=");
            sb.append(str);
            sb.append(", pmk=");
            String str2 = "<*>";
            sb.append(bArr == null ? "<null>" : str2);
            sb.append(", passphrase=");
            if (TextUtils.isEmpty(passphrase)) {
                str2 = "<empty>";
            }
            sb.append(str2);
            sb.append(", isOutOfBand=");
            sb.append(z);
            sb.append(", appInfo.length=");
            sb.append(bArr2 == null ? 0 : bArr2.length);
            sb.append(", capabilities=");
            sb.append(capabilities2);
            Log.v(TAG, sb.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "initiateDataPath: null interface");
            return false;
        } else if (capabilities2 == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanInitiateDataPathRequest req = new NanInitiateDataPathRequest();
            req.peerId = i;
            copyArray(peer, req.peerDiscMacAddr);
            req.channelRequestType = i2;
            req.channel = i3;
            req.ifaceName = str;
            req.securityConfig.securityType = 0;
            if (!(bArr == null || bArr.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(bArr, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && z) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            convertNativeByteArrayToArrayList(bArr2, req.appInfo);
            try {
                WifiStatus status = iface.initiateDataPathRequest(s, req);
                if (status.code == 0) {
                    return true;
                }
                Log.e(TAG, "initiateDataPath: error: " + statusString(status));
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "initiateDataPath: exception: " + e);
                return false;
            }
        }
    }

    public boolean respondToDataPathRequest(short transactionId, boolean accept, int ndpId, String interfaceName, byte[] pmk, String passphrase, byte[] appInfo, boolean isOutOfBand, Capabilities capabilities) {
        short s = transactionId;
        boolean z = accept;
        int i = ndpId;
        String str = interfaceName;
        byte[] bArr = pmk;
        byte[] bArr2 = appInfo;
        Capabilities capabilities2 = capabilities;
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("respondToDataPathRequest: transactionId=");
            sb.append(s);
            sb.append(", accept=");
            sb.append(z);
            sb.append(", int ndpId=");
            sb.append(i);
            sb.append(", interfaceName=");
            sb.append(str);
            sb.append(", appInfo.length=");
            sb.append(bArr2 == null ? 0 : bArr2.length);
            Log.v(TAG, sb.toString());
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "respondToDataPathRequest: null interface");
            return false;
        } else if (capabilities2 == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        } else {
            NanRespondToDataPathIndicationRequest req = new NanRespondToDataPathIndicationRequest();
            req.acceptRequest = z;
            req.ndpInstanceId = i;
            req.ifaceName = str;
            req.securityConfig.securityType = 0;
            if (!(bArr == null || bArr.length == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 1;
                copyArray(bArr, req.securityConfig.pmk);
            }
            if (!(passphrase == null || passphrase.length() == 0)) {
                req.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities2.supportedCipherSuites);
                req.securityConfig.securityType = 2;
                convertNativeByteArrayToArrayList(passphrase.getBytes(), req.securityConfig.passphrase);
            }
            if (req.securityConfig.securityType != 0 && isOutOfBand) {
                convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), req.serviceNameOutOfBand);
            }
            convertNativeByteArrayToArrayList(bArr2, req.appInfo);
            try {
                WifiStatus status = iface.respondToDataPathIndicationRequest(s, req);
                if (status.code == 0) {
                    return true;
                }
                Log.e(TAG, "respondToDataPathRequest: error: " + statusString(status));
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "respondToDataPathRequest: exception: " + e);
                return false;
            }
        }
    }

    public boolean endDataPath(short transactionId, int ndpId) {
        if (this.mDbg) {
            Log.v(TAG, "endDataPath: transactionId=" + transactionId + ", ndpId=" + ndpId);
        }
        recordTransactionId(transactionId);
        android.hardware.wifi.V1_0.IWifiNanIface iface = this.mHal.getWifiNanIface();
        if (iface == null) {
            Log.e(TAG, "endDataPath: null interface");
            return false;
        }
        try {
            WifiStatus status = iface.terminateDataPathRequest(transactionId, ndpId);
            if (status.code == 0) {
                return true;
            }
            Log.e(TAG, "endDataPath: error: " + statusString(status));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "endDataPath: exception: " + e);
            return false;
        }
    }

    private void updateConfigForPowerSettings(NanConfigRequest req, NanConfigRequestSupplemental configSupplemental12, boolean isInteractive, boolean isIdle) {
        String key = "default";
        if (isIdle) {
            key = POWER_PARAM_IDLE_KEY;
        } else if (!isInteractive) {
            key = POWER_PARAM_INACTIVE_KEY;
        }
        boolean z = true;
        updateSingleConfigForPowerSettings(req.bandSpecificConfig[1], ((Integer) this.mSettablePowerParameters.get(key).get(PARAM_DW_5GHZ)).intValue());
        updateSingleConfigForPowerSettings(req.bandSpecificConfig[0], ((Integer) this.mSettablePowerParameters.get(key).get(PARAM_DW_24GHZ)).intValue());
        updateMasterPrefForPowerSettings(req, ((Integer) this.mSettablePowerParameters.get(key).get(PARAM_MASTER_PREF)).intValue());
        configSupplemental12.discoveryBeaconIntervalMs = ((Integer) this.mSettablePowerParameters.get(key).get(PARAM_DISCOVERY_BEACON_INTERVAL_MS)).intValue();
        configSupplemental12.numberOfSpatialStreamsInDiscovery = ((Integer) this.mSettablePowerParameters.get(key).get(PARAM_NUM_SS_IN_DISCOVERY)).intValue();
        if (((Integer) this.mSettablePowerParameters.get(key).get(PARAM_ENABLE_DW_EARLY_TERM)).intValue() == 0) {
            z = false;
        }
        configSupplemental12.enableDiscoveryWindowEarlyTermination = z;
    }

    private void updateSingleConfigForPowerSettings(NanBandSpecificConfig cfg, int override) {
        if (override != -1) {
            cfg.validDiscoveryWindowIntervalVal = true;
            cfg.discoveryWindowIntervalVal = (byte) override;
        }
    }

    private void updateMasterPrefForPowerSettings(NanConfigRequest req, int override) {
        Log.d(TAG, "updateMasterPrefForPowerSettings: override = " + override);
        if (override != -1) {
            req.masterPref = (byte) override;
        }
    }

    private int getStrongestCipherSuiteType(int supportedCipherSuites) {
        if ((supportedCipherSuites & 2) != 0) {
            return 2;
        }
        if ((supportedCipherSuites & 1) != 0) {
            return 1;
        }
        return 0;
    }

    private ArrayList<Byte> convertNativeByteArrayToArrayList(byte[] from, ArrayList<Byte> to) {
        if (from == null) {
            from = new byte[0];
        }
        if (to == null) {
            to = new ArrayList<>(from.length);
        } else {
            to.ensureCapacity(from.length);
        }
        for (byte valueOf : from) {
            to.add(Byte.valueOf(valueOf));
        }
        return to;
    }

    private void copyArray(byte[] from, byte[] to) {
        if (from == null || to == null || from.length != to.length) {
            Log.e(TAG, "copyArray error: from=" + from + ", to=" + to);
            return;
        }
        for (int i = 0; i < from.length; i++) {
            to[i] = from[i];
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        return status.code + " (" + status.description + ")";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareNativeApi:");
        pw.println("  mSettableParameters: " + this.mSettableParameters);
        this.mHal.dump(fd, pw, args);
    }
}
