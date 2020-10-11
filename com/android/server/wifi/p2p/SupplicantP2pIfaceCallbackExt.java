package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.util.Log;
import com.android.server.wifi.util.NativeUtil;
import java.util.ArrayList;
import vendor.samsung.hardware.wifi.supplicant.V2_0.ISehSupplicantP2pIfaceCallback;

public class SupplicantP2pIfaceCallbackExt extends ISehSupplicantP2pIfaceCallback.Stub {
    private static final String TAG = "SupplicantP2pIfaceCallbackExt";
    private static boolean sVerboseLoggingEnabled = true;
    private final String mInterface;
    private final WifiP2pMonitor mMonitor;

    public SupplicantP2pIfaceCallbackExt(String iface, WifiP2pMonitor monitor) {
        this.mInterface = iface;
        this.mMonitor = monitor;
    }

    public static void enableVerboseLogging(int verbose) {
        sVerboseLoggingEnabled = verbose > 0;
    }

    protected static void logd(String s) {
        if (sVerboseLoggingEnabled) {
            Log.d(TAG, s);
        }
    }

    public void onDeviceFound(byte[] srcAddress, byte[] p2pDeviceAddress, byte[] primaryDeviceType, String deviceName, short configMethods, byte deviceCapabilities, int groupCapabilities, byte[] wfdDeviceInfo, String extInfo) {
        WifiP2pDevice device = new WifiP2pDevice();
        device.deviceName = deviceName;
        if (deviceName == null) {
            Log.e(TAG, "Missing device name.");
            return;
        }
        try {
            device.deviceAddress = NativeUtil.macAddressFromByteArray(p2pDeviceAddress);
            try {
                device.primaryDeviceType = NativeUtil.wpsDevTypeStringFromByteArray(primaryDeviceType);
                device.deviceCapability = deviceCapabilities;
                device.groupCapability = groupCapabilities;
                device.wpsConfigMethodsSupported = configMethods;
                device.status = 3;
                if (device.isGroupOwner()) {
                    try {
                        device.interfaceAddress = NativeUtil.macAddressFromByteArray(srcAddress);
                    } catch (Exception e) {
                        Log.d(TAG, "Could not decode interface address.", e);
                    }
                }
                if (extInfo != null) {
                    device.updateAddtionalInfo(extInfo);
                }
                if (wfdDeviceInfo != null && wfdDeviceInfo.length >= 6) {
                    device.wfdInfo = new WifiP2pWfdInfo((wfdDeviceInfo[0] << 8) + wfdDeviceInfo[1], (wfdDeviceInfo[2] << 8) + wfdDeviceInfo[3], (wfdDeviceInfo[4] << 8) + wfdDeviceInfo[5]);
                }
                logd("Device discovered on " + this.mInterface + ": " + device);
                this.mMonitor.broadcastP2pDeviceFound(this.mInterface, device);
            } catch (Exception e2) {
                Log.e(TAG, "Could not encode device primary type.", e2);
            }
        } catch (Exception e3) {
            Log.e(TAG, "Could not decode device address.", e3);
        }
    }

    public void onGroupStarted(String groupIfName, boolean isGo, ArrayList<Byte> ssid, int frequency, byte[] psk, String passphrase, byte[] goDeviceAddress, boolean isPersistent, String extInfo) {
        if (groupIfName == null) {
            Log.e(TAG, "Missing group interface name.");
            return;
        }
        logd("Group " + groupIfName + " started on " + this.mInterface);
        WifiP2pGroup group = new WifiP2pGroup();
        group.setInterface(groupIfName);
        try {
            group.setNetworkName(NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid(ssid)));
            group.setIsGroupOwner(isGo);
            group.setPassphrase(passphrase);
            group.setFrequency(frequency);
            if (isPersistent) {
                group.setNetworkId(-2);
            } else {
                group.setNetworkId(-1);
            }
            if (extInfo != null) {
                group.updateAddtionalInfo(extInfo);
            }
            WifiP2pDevice owner = new WifiP2pDevice();
            try {
                owner.deviceAddress = NativeUtil.macAddressFromByteArray(goDeviceAddress);
                group.setOwner(owner);
                this.mMonitor.broadcastP2pGroupStarted(this.mInterface, group);
            } catch (Exception e) {
                Log.e(TAG, "Could not decode Group Owner address.", e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "Could not encode SSID.", e2);
        }
    }

    public void onProvisionDiscoveryCompleted(byte[] p2pDeviceAddress, boolean isRequest, byte status, short configMethods, String generatedPin, String extInfo) {
        if (status != 0) {
            Log.e(TAG, "Provision discovery failed: " + status);
            this.mMonitor.broadcastP2pProvisionDiscoveryFailure(this.mInterface, status);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Provision discovery ");
        sb.append(isRequest ? "request" : "response");
        sb.append(" for WPS Config method: ");
        sb.append(configMethods);
        logd(sb.toString());
        WifiP2pProvDiscEvent event = new WifiP2pProvDiscEvent();
        event.device = new WifiP2pDevice();
        try {
            event.device.deviceAddress = NativeUtil.macAddressFromByteArray(p2pDeviceAddress);
            if (extInfo != null) {
                event.updateAddtionalInfo(extInfo);
            }
            if ((configMethods & WpsConfigMethods.PUSHBUTTON) != 0) {
                if (isRequest) {
                    event.event = 1;
                    this.mMonitor.broadcastP2pProvisionDiscoveryPbcRequest(this.mInterface, event);
                    return;
                }
                event.event = 2;
                this.mMonitor.broadcastP2pProvisionDiscoveryPbcResponse(this.mInterface, event);
            } else if (!isRequest && (configMethods & 256) != 0) {
                event.event = 4;
                event.pin = generatedPin;
                this.mMonitor.broadcastP2pProvisionDiscoveryShowPin(this.mInterface, event);
            } else if (!isRequest && (configMethods & 8) != 0) {
                event.event = 3;
                this.mMonitor.broadcastP2pProvisionDiscoveryEnterPin(this.mInterface, event);
            } else if (isRequest && (configMethods & 8) != 0) {
                event.event = 4;
                event.pin = generatedPin;
                this.mMonitor.broadcastP2pProvisionDiscoveryShowPin(this.mInterface, event);
            } else if (!isRequest || (configMethods & 256) == 0) {
                Log.e(TAG, "Unsupported config methods: " + configMethods);
            } else {
                event.event = 3;
                this.mMonitor.broadcastP2pProvisionDiscoveryEnterPin(this.mInterface, event);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not decode MAC address.", e);
        }
    }

    public void onP2pEventReceived(String eventName, String data) {
        this.mMonitor.broadcastP2pEventNotify(this.mInterface, eventName, data);
    }

    public void onBigDataLogging(String message) {
        this.mMonitor.broadcastBigDataEvent(this.mInterface, message);
    }

    public void onGoPs(String data) {
        this.mMonitor.broadcastGoPsEvent(this.mInterface, data);
    }

    public void onApplicationDataReceived(byte[] srcAddress, String data) {
        this.mMonitor.broadcastSconnectEvent(this.mInterface, data);
    }
}
