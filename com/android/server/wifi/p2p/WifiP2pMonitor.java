package com.android.server.wifi.p2p;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiP2pMonitor {
    public static final int AP_STA_CONNECTED_EVENT = 147498;
    public static final int AP_STA_DISCONNECTED_EVENT = 147497;
    public static final int AP_STA_POSSIBLE_PSK_MISMATCH = 147499;
    private static final int BASE = 147456;
    public static final int P2P_BIGDATA_CONNECTION_RESULT_EVENT = 147537;
    public static final int P2P_BIGDATA_DISCONNECT_EVENT = 147536;
    public static final int P2P_BIGDATA_GROUP_OWNER_INTENT_EVENT = 147538;
    public static final int P2P_DEVICE_FOUND_EVENT = 147477;
    public static final int P2P_DEVICE_LOST_EVENT = 147478;
    public static final int P2P_FIND_STOPPED_EVENT = 147493;
    public static final int P2P_GOPS_EVENT = 147505;
    public static final int P2P_GO_NEGOTIATION_FAILURE_EVENT = 147482;
    public static final int P2P_GO_NEGOTIATION_REQUEST_EVENT = 147479;
    public static final int P2P_GO_NEGOTIATION_SUCCESS_EVENT = 147481;
    public static final int P2P_GROUP_FORMATION_FAILURE_EVENT = 147484;
    public static final int P2P_GROUP_FORMATION_SUCCESS_EVENT = 147483;
    public static final int P2P_GROUP_REMOVED_EVENT = 147486;
    public static final int P2P_GROUP_STARTED_EVENT = 147485;
    public static final int P2P_INVITATION_RECEIVED_EVENT = 147487;
    public static final int P2P_INVITATION_RESULT_EVENT = 147488;
    public static final int P2P_NO_COMMON_CHANNEL = 147516;
    public static final int P2P_P2P_SCONNECT_PROBE_REQ_EVENT = 147526;
    public static final int P2P_PERSISTENT_PSK_FAIL_EVENT = 147496;
    public static final int P2P_PROV_DISC_ENTER_PIN_EVENT = 147491;
    public static final int P2P_PROV_DISC_FAILURE_EVENT = 147495;
    public static final int P2P_PROV_DISC_PBC_REQ_EVENT = 147489;
    public static final int P2P_PROV_DISC_PBC_RSP_EVENT = 147490;
    public static final int P2P_PROV_DISC_SHOW_PIN_EVENT = 147492;
    public static final int P2P_PROV_DISC_USER_REJECT_EVENT = 147527;
    public static final int P2P_SERV_DISC_RESP_EVENT = 147494;
    public static final int P2P_WPS_SKIP_EVENT = 147506;
    public static final int SUP_CONNECTION_EVENT = 147457;
    public static final int SUP_DISCONNECTION_EVENT = 147458;
    private static final String TAG = "WifiP2pMonitor";
    private boolean mConnected = false;
    private final Map<String, SparseArray<Set<Handler>>> mHandlerMap = new HashMap();
    private final Map<String, Boolean> mMonitoringMap = new HashMap();
    private boolean mVerboseLoggingEnabled = false;
    private final WifiInjector mWifiInjector;

    public WifiP2pMonitor(WifiInjector wifiInjector) {
        this.mWifiInjector = wifiInjector;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    public synchronized void registerHandler(String iface, int what, Handler handler) {
        SparseArray<Set<Handler>> ifaceHandlers = this.mHandlerMap.get(iface);
        if (ifaceHandlers == null) {
            ifaceHandlers = new SparseArray<>();
            this.mHandlerMap.put(iface, ifaceHandlers);
        }
        Set<Handler> ifaceWhatHandlers = ifaceHandlers.get(what);
        if (ifaceWhatHandlers == null) {
            ifaceWhatHandlers = new ArraySet<>();
            ifaceHandlers.put(what, ifaceWhatHandlers);
        }
        ifaceWhatHandlers.add(handler);
    }

    private boolean isMonitoring(String iface) {
        Boolean val = this.mMonitoringMap.get(iface);
        if (val == null) {
            return false;
        }
        return val.booleanValue();
    }

    @VisibleForTesting
    public void setMonitoring(String iface, boolean enabled) {
        this.mMonitoringMap.put(iface, Boolean.valueOf(enabled));
    }

    private void setMonitoringNone() {
        for (String iface : this.mMonitoringMap.keySet()) {
            setMonitoring(iface, false);
        }
    }

    public synchronized void startMonitoring(String iface) {
        setMonitoring(iface, true);
        broadcastSupplicantConnectionEvent(iface);
    }

    public synchronized void stopMonitoring(String iface) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "stopMonitoring(" + iface + ")");
        }
        setMonitoring(iface, true);
        broadcastSupplicantDisconnectionEvent(iface);
        setMonitoring(iface, false);
    }

    public synchronized void stopAllMonitoring() {
        this.mConnected = false;
        setMonitoringNone();
    }

    private void sendMessage(String iface, int what) {
        sendMessage(iface, Message.obtain((Handler) null, what));
    }

    private void sendMessage(String iface, int what, Object obj) {
        sendMessage(iface, Message.obtain((Handler) null, what, obj));
    }

    private void sendMessage(String iface, int what, int arg1) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, 0));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, arg2));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2, Object obj) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, arg2, obj));
    }

    private void sendMessage(String iface, Message message) {
        SparseArray<Set<Handler>> ifaceHandlers = this.mHandlerMap.get(iface);
        if (iface == null || ifaceHandlers == null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Sending to all monitors because there's no matching iface");
            }
            for (Map.Entry<String, SparseArray<Set<Handler>>> entry : this.mHandlerMap.entrySet()) {
                if (isMonitoring(entry.getKey())) {
                    for (Handler handler : (Set) entry.getValue().get(message.what)) {
                        if (handler != null) {
                            sendMessage(handler, Message.obtain(message));
                        }
                    }
                }
            }
        } else if (isMonitoring(iface)) {
            Set<Handler> ifaceWhatHandlers = ifaceHandlers.get(message.what);
            if (ifaceWhatHandlers != null) {
                for (Handler handler2 : ifaceWhatHandlers) {
                    if (handler2 != null) {
                        sendMessage(handler2, Message.obtain(message));
                    }
                }
            }
        } else if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Dropping event because (" + iface + ") is stopped");
        }
        message.recycle();
    }

    private void sendMessage(Handler handler, Message message) {
        message.setTarget(handler);
        message.sendToTarget();
    }

    public void broadcastSupplicantConnectionEvent(String iface) {
        sendMessage(iface, 147457);
    }

    public void broadcastSupplicantDisconnectionEvent(String iface) {
        sendMessage(iface, 147458);
    }

    public void broadcastP2pDeviceFound(String iface, WifiP2pDevice device) {
        if (device != null) {
            sendMessage(iface, (int) P2P_DEVICE_FOUND_EVENT, (Object) device);
        }
    }

    public void broadcastP2pDeviceLost(String iface, WifiP2pDevice device) {
        if (device != null) {
            sendMessage(iface, (int) P2P_DEVICE_LOST_EVENT, (Object) device);
        }
    }

    public void broadcastP2pFindStopped(String iface) {
        sendMessage(iface, (int) P2P_FIND_STOPPED_EVENT);
    }

    public void broadcastP2pGoNegotiationRequest(String iface, WifiP2pConfig config) {
        if (config != null) {
            sendMessage(iface, (int) P2P_GO_NEGOTIATION_REQUEST_EVENT, (Object) config);
        }
    }

    public void broadcastP2pGoNegotiationSuccess(String iface) {
        sendMessage(iface, (int) P2P_GO_NEGOTIATION_SUCCESS_EVENT);
    }

    public void broadcastP2pGoNegotiationFailure(String iface, WifiP2pServiceImpl.P2pStatus reason) {
        sendMessage(iface, (int) P2P_GO_NEGOTIATION_FAILURE_EVENT, (Object) reason);
    }

    public void broadcastP2pGroupFormationSuccess(String iface) {
        sendMessage(iface, (int) P2P_GROUP_FORMATION_SUCCESS_EVENT);
    }

    public void broadcastP2pGroupFormationFailure(String iface, String reason) {
        WifiP2pServiceImpl.P2pStatus err = WifiP2pServiceImpl.P2pStatus.UNKNOWN;
        if (reason.equals("FREQ_CONFLICT")) {
            err = WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL;
        }
        sendMessage(iface, (int) P2P_GROUP_FORMATION_FAILURE_EVENT, (Object) err);
    }

    public void broadcastP2pGroupStarted(String iface, WifiP2pGroup group) {
        if (group != null) {
            sendMessage(iface, (int) P2P_GROUP_STARTED_EVENT, (Object) group);
        }
    }

    public void broadcastP2pGroupRemoved(String iface, WifiP2pGroup group) {
        if (group != null) {
            sendMessage(iface, (int) P2P_GROUP_REMOVED_EVENT, (Object) group);
        }
    }

    public void broadcastP2pInvitationReceived(String iface, WifiP2pGroup group) {
        if (group != null) {
            sendMessage(iface, (int) P2P_INVITATION_RECEIVED_EVENT, (Object) group);
        }
    }

    public void broadcastP2pInvitationResult(String iface, WifiP2pServiceImpl.P2pStatus result) {
        sendMessage(iface, (int) P2P_INVITATION_RESULT_EVENT, (Object) result);
    }

    public void broadcastP2pProvisionDiscoveryPbcRequest(String iface, WifiP2pProvDiscEvent event) {
        if (event != null) {
            sendMessage(iface, (int) P2P_PROV_DISC_PBC_REQ_EVENT, (Object) event);
        }
    }

    public void broadcastP2pProvisionDiscoveryPbcResponse(String iface, WifiP2pProvDiscEvent event) {
        if (event != null) {
            sendMessage(iface, (int) P2P_PROV_DISC_PBC_RSP_EVENT, (Object) event);
        }
    }

    public void broadcastP2pProvisionDiscoveryEnterPin(String iface, WifiP2pProvDiscEvent event) {
        if (event != null) {
            sendMessage(iface, (int) P2P_PROV_DISC_ENTER_PIN_EVENT, (Object) event);
        }
    }

    public void broadcastP2pProvisionDiscoveryShowPin(String iface, WifiP2pProvDiscEvent event) {
        if (event != null) {
            sendMessage(iface, (int) P2P_PROV_DISC_SHOW_PIN_EVENT, (Object) event);
        }
    }

    public void broadcastP2pProvisionDiscoveryFailure(String iface, int status) {
        if (status == 2) {
            sendMessage(iface, 147527);
        } else {
            sendMessage(iface, (int) P2P_PROV_DISC_FAILURE_EVENT);
        }
    }

    public void broadcastP2pProvisionDiscoveryFailure(String iface, WifiP2pProvDiscEvent event, int status) {
        if (status == 2) {
            sendMessage(iface, 147527, (Object) event);
        } else {
            sendMessage(iface, (int) P2P_PROV_DISC_FAILURE_EVENT);
        }
    }

    public void broadcastP2pServiceDiscoveryResponse(String iface, List<WifiP2pServiceResponse> services) {
        sendMessage(iface, (int) P2P_SERV_DISC_RESP_EVENT, (Object) services);
    }

    public void broadcastP2pApStaConnected(String iface, WifiP2pDevice device) {
        sendMessage(iface, (int) AP_STA_CONNECTED_EVENT, (Object) device);
    }

    public void broadcastP2pApStaDisconnected(String iface, WifiP2pDevice device) {
        sendMessage(iface, (int) AP_STA_DISCONNECTED_EVENT, (Object) device);
    }

    public void broadcastGoPsEvent(String iface, String dataString) {
        sendMessage(iface, (int) P2P_GOPS_EVENT, (Object) dataString);
    }

    public void broadcastBigDataEvent(String iface, String message) {
        if (message.startsWith("P2P-BIGDATA-DISCONNECT")) {
            sendMessage(iface, (int) P2P_BIGDATA_DISCONNECT_EVENT, (Object) message);
        } else if (message.startsWith("P2P-BIGDATA-CONNECTION-RESULT")) {
            sendMessage(iface, (int) P2P_BIGDATA_CONNECTION_RESULT_EVENT, (Object) message);
        } else if (message.startsWith("P2P-BIGDATA-GROUP-OWNER-INTENT")) {
            sendMessage(iface, (int) P2P_BIGDATA_GROUP_OWNER_INTENT_EVENT, (Object) message);
        }
    }

    public void broadcastSconnectEvent(String iface, String dataString) {
        sendMessage(iface, (int) P2P_P2P_SCONNECT_PROBE_REQ_EVENT, (Object) dataString);
    }

    public void broadcastP2pEventNotify(String iface, String event, String param) {
        if (event.startsWith("AP-STA-POSSIBLE-PSK-MISMATCH")) {
            sendMessage(iface, 147499, (Object) param);
        } else if (event.startsWith("P2P-PERSISTENT-PSK-FAIL")) {
            sendMessage(iface, (int) P2P_PERSISTENT_PSK_FAIL_EVENT, (Object) param);
        }
    }
}
