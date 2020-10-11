package com.android.server.wifi.iwc.p000le;

import android.os.Bundle;
import java.util.List;

/* renamed from: com.android.server.wifi.iwc.le.ChannelCache */
public interface ChannelCache {
    public static final Integer[] ALL_CHANNELS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

    void addChannel(String str, int i);

    void addChannelList(String str, List<Integer> list);

    List<Integer> getChannelList(String str);

    List<Integer> getFullChannelList();

    void initialize(Bundle bundle);

    void setChannelList(String str, List<Integer> list);

    void setChannelListIfNotExist(String str, List<Integer> list);
}
