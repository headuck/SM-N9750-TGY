package com.android.server.wifi.iwc;

public enum RewardEvent {
    LESSEVENT_INDEXLIMIT(-10),
    MOREEVENT_INDEXLIMIT(10),
    OTHEREVENT_INDEXLIMIT(100),
    NO_EVENT(101),
    POOR_LINK(102),
    MANUAL_SWITCH(11),
    MANUAL_SWITCH_G(103),
    MANUAL_SWITCH_L(-11),
    CONNECTION_SWITCHED_TOO_SHORT(-12),
    MANUAL_RECONNECTION(-13),
    WIFI_OFF(13),
    AUTO_DISCONNECTION(0),
    AGG_SNS_ON(104),
    AGG_SNS_OFF(105),
    CELLULAR_DATA_OFF(-14),
    MANUAL_DISCONNECT(12),
    NETWORK_DISCONNECTED(106),
    NETWORK_CONNECTED(107),
    SNS_ON(50),
    SNS_OFF(-50),
    NETWORK_CONNECTED_WITH_SNS_ON(14),
    NETWORK_CONNECTED_WITH_SNS_OFF(-15);
    
    private int value;

    private RewardEvent(int value2) {
        this.value = value2;
    }

    public int getValue() {
        return this.value;
    }
}
