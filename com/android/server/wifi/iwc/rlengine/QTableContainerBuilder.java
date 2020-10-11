package com.android.server.wifi.iwc.rlengine;

import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.rlengine.QTableContainer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QTableContainerBuilder {
    private static final boolean DBG = false;
    private static final String TAG = "IWCMonitor.QTableContainerBuilder";
    private String mJson = null;
    private IWCLogFile mLog = null;

    public QTableContainerBuilder setIWCJson(String json) {
        this.mJson = json;
        return this;
    }

    public QTableContainerBuilder setIWCLogFile(IWCLogFile logFile) {
        this.mLog = logFile;
        return this;
    }

    /* Debug info: failed to restart local var, previous not found, register: 14 */
    public QTableContainer create() {
        JSONArray qTableList;
        JSONArray qTableIndexList;
        if (this.mJson == null) {
            return new QTableContainer(this.mLog);
        }
        QTableContainer.SavedMembers savedMembers = new QTableContainer.SavedMembers();
        try {
            JSONObject JSON = new JSONObject(this.mJson);
            try {
                savedMembers.candidateApList = getApListFrom(JSON.getJSONObject("candidateApList"));
            } catch (JSONException je) {
                je.printStackTrace();
                savedMembers.candidateApList = new ConcurrentHashMap();
            }
            try {
                savedMembers.coreApList = getApListFrom(JSON.getJSONObject("coreApList"));
            } catch (JSONException je2) {
                je2.printStackTrace();
                savedMembers.coreApList = new ConcurrentHashMap();
            }
            try {
                savedMembers.probationApList = getApListFrom(JSON.getJSONObject("probationApList"));
            } catch (JSONException je3) {
                je3.printStackTrace();
                savedMembers.probationApList = new ConcurrentHashMap();
            }
            try {
                savedMembers.bssidPerSsidList = getBssidPerSsidListFrom(JSON.getJSONObject("bssidPerSsidList"));
            } catch (JSONException je4) {
                je4.printStackTrace();
                savedMembers.bssidPerSsidList = new ConcurrentHashMap();
            }
            try {
                savedMembers.forcedqai = JSON.getInt("forcedqai");
            } catch (JSONException je5) {
                je5.printStackTrace();
                savedMembers.forcedqai = -1;
            }
            try {
                savedMembers.qai = JSON.getInt("qai");
            } catch (JSONException je6) {
                je6.printStackTrace();
                savedMembers.qai = -1;
            }
            try {
                savedMembers.version = JSON.getLong("version");
            } catch (JSONException je7) {
                je7.printStackTrace();
                savedMembers.version = QTableContainer.serialVersionUID;
            }
            try {
                qTableIndexList = JSON.getJSONArray("qTableIndexList");
                qTableList = JSON.getJSONArray("qTableList");
                if (qTableIndexList == null || qTableList == null || qTableIndexList.length() != qTableList.length()) {
                    throw new JSONException("qTableList is corrupted");
                }
                if (!(qTableIndexList == null || qTableList == null)) {
                    ArrayList<QTableContainer.IndexNode> dstQTableIndexList = new ArrayList<>();
                    ArrayList<QTable> dstQTableList = new ArrayList<>();
                    int a = 0;
                    while (a < qTableIndexList.length()) {
                        try {
                            QTableContainer.IndexNode node = new QTableContainer.IndexNode(qTableIndexList.getJSONObject(a).getString("bssid"));
                            JSONObject sub2 = qTableList.getJSONObject(a);
                            QTableBuilder qTableBuilder = new QTableBuilder();
                            qTableBuilder.setIWCJsonObject(sub2).setIWCLogFile(this.mLog);
                            QTable qtable = qTableBuilder.create();
                            dstQTableIndexList.add(node);
                            dstQTableList.add(qtable);
                            a++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    savedMembers.qTableIndexList = dstQTableIndexList;
                    savedMembers.qTableList = dstQTableList;
                }
                return savedMembers.readResolve(this.mLog);
            } catch (JSONException je8) {
                je8.printStackTrace();
                savedMembers.qTableIndexList = new ArrayList<>();
                savedMembers.qTableList = new ArrayList<>();
                qTableIndexList = null;
                qTableList = null;
            }
        } catch (JSONException je9) {
            je9.printStackTrace();
            return new QTableContainer(this.mLog);
        }
    }

    private Map<String, Set<Long>> getBssidPerSsidListFrom(JSONObject jsonBssidList) {
        Iterator<String> iterator = jsonBssidList.keys();
        Map<String, Set<Long>> bssidList = new ConcurrentHashMap<>();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                JSONArray sub = jsonBssidList.getJSONArray(key);
                Set<Long> v = new HashSet<>();
                int a = 0;
                while (a < sub.length()) {
                    try {
                        v.add(Long.valueOf(sub.getLong(a)));
                        a++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                bssidList.put(key, v);
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }
        return bssidList;
    }

    private Map<String, QTableContainer.ApListNode> getApListFrom(JSONObject jsonApList) {
        Iterator<String> iterator = jsonApList.keys();
        Map<String, QTableContainer.ApListNode> apList = new ConcurrentHashMap<>();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                JSONObject sub = jsonApList.getJSONObject(key);
                apList.put(key, new QTableContainer.ApListNode(sub.getInt("activityScore"), sub.getLong("firstAdded"), sub.getLong("lastAccessed")));
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }
        return apList;
    }

    /* access modifiers changed from: package-private */
    public JSONArray getIndexNodeList(ArrayList<QTableContainer.IndexNode> list) throws JSONException {
        JSONArray qTableIndexList = new JSONArray();
        if (list != null) {
            Iterator<QTableContainer.IndexNode> it = list.iterator();
            while (it.hasNext()) {
                JSONObject jo = new JSONObject();
                jo.put("bssid", it.next().bssid);
                qTableIndexList.put(jo);
            }
        }
        return qTableIndexList;
    }

    /* access modifiers changed from: package-private */
    public JSONArray getQTableList(ArrayList<QTable> list) throws JSONException {
        JSONArray qTableList = new JSONArray();
        if (list != null) {
            QTableBuilder builder = new QTableBuilder();
            Iterator<QTable> it = list.iterator();
            while (it.hasNext()) {
                qTableList.put(builder.toJo(it.next()));
            }
        }
        return qTableList;
    }

    /* access modifiers changed from: package-private */
    public JSONObject getApListNode(Map<String, QTableContainer.ApListNode> map) throws JSONException {
        JSONObject bssidMap = new JSONObject();
        for (String key : map.keySet()) {
            QTableContainer.ApListNode node = map.get(key);
            JSONObject apListNode = new JSONObject();
            apListNode.put("activityScore", node.activityScore);
            apListNode.put("lastAccessed", node.lastAccessed);
            apListNode.put("firstAdded", node.firstAdded);
            bssidMap.put(key, apListNode);
        }
        return bssidMap;
    }

    /* access modifiers changed from: package-private */
    public JSONObject getBssidPerSsidList(Map<String, Set<Long>> map) throws JSONException {
        JSONObject bssidPerSsidMap = new JSONObject();
        for (String key : map.keySet()) {
            JSONArray bssidArray = new JSONArray();
            for (Long put : map.get(key)) {
                bssidArray.put(put);
            }
            bssidPerSsidMap.put(key, bssidArray);
        }
        return bssidPerSsidMap;
    }

    public String toJsonString(QTableContainer container) throws JSONException {
        String res = new String();
        if (container == null) {
            return res;
        }
        QTableContainer.SavedMembers savedMembers = container.writeReplace();
        JSONObject root = new JSONObject();
        root.put("candidateApList", getApListNode(savedMembers.candidateApList));
        root.put("coreApList", getApListNode(savedMembers.coreApList));
        root.put("forcedqai", savedMembers.forcedqai);
        root.put("probationApList", getApListNode(savedMembers.probationApList));
        root.put("bssidPerSsidList", getBssidPerSsidList(savedMembers.bssidPerSsidList));
        root.put("qTableIndexList", getIndexNodeList(savedMembers.qTableIndexList));
        root.put("qTableList", getQTableList(savedMembers.qTableList));
        root.put("qai", savedMembers.qai);
        root.put("version", savedMembers.version);
        return root.toString();
    }
}
