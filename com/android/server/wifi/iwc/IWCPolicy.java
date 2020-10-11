package com.android.server.wifi.iwc;

public interface IWCPolicy {
    void adopt();

    void detect();

    void discard();

    boolean isValid();
}
