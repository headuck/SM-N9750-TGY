package com.android.server.wifi.iwc.p000le;

import android.os.Bundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/* renamed from: com.android.server.wifi.iwc.le.FileChannelCache */
public class FileChannelCache implements ChannelCache {
    private static final String FILENAME = "ccChannel.ser";
    private static final String TAG = "IWCLE";
    private String fileDir;
    private HashMap<String, ArrayList<Integer>> mCache;
    private DataFileControl<HashMap<String, ArrayList<Integer>>> mChannelFileControl = new DataFileControl<>();
    private Object mutex = new Object();

    public void initialize(Bundle param) {
        this.fileDir = param.getString(LEGlobalParams.KEY_FILE_DIR, LEGlobalParams.DEFAULT_FILE_DIR);
        loadChannelCache();
    }

    public void addChannel(String capBssid, int ch) {
        synchronized (this.mutex) {
            if (this.mCache.get(capBssid) == null) {
                this.mCache.put(capBssid, new ArrayList());
            }
            if (!this.mCache.get(capBssid).contains(Integer.valueOf(ch))) {
                this.mCache.get(capBssid).add(Integer.valueOf(ch));
                saveChannelCache();
            }
        }
    }

    public void addChannelList(String capBssid, List<Integer> chList) {
        synchronized (this.mutex) {
            if (this.mCache.get(capBssid) == null) {
                this.mCache.put(capBssid, new ArrayList());
            }
            for (Integer intValue : chList) {
                int ch = intValue.intValue();
                if (!this.mCache.get(capBssid).contains(Integer.valueOf(ch))) {
                    this.mCache.get(capBssid).add(Integer.valueOf(ch));
                }
            }
            saveChannelCache();
        }
    }

    public void setChannelList(String capBssid, List<Integer> chList) {
        synchronized (this.mutex) {
            if (chList != null) {
                this.mCache.put(capBssid, new ArrayList(chList));
            } else {
                this.mCache.remove(capBssid);
            }
            saveChannelCache();
        }
    }

    public void setChannelListIfNotExist(String capBssid, List<Integer> chList) {
        if (this.mCache.get(capBssid) == null) {
            setChannelList(capBssid, chList);
        }
    }

    public List<Integer> getChannelList(String capBssid) {
        ArrayList<Integer> list = this.mCache.get(capBssid);
        if (list == null || list.size() == 0) {
            return getFullChannelList();
        }
        return new ArrayList(list);
    }

    public List<Integer> getFullChannelList() {
        return new ArrayList(Arrays.asList(ChannelCache.ALL_CHANNELS));
    }

    private void loadChannelCache() {
        this.mCache = this.mChannelFileControl.loadObject(this.fileDir, FILENAME);
        if (this.mCache == null) {
            this.mCache = new HashMap<>();
        }
    }

    private void saveChannelCache() {
        this.mChannelFileControl.saveObject(this.fileDir, FILENAME, this.mCache, false);
    }
}
