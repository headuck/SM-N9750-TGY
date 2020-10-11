package com.android.server.wifi.iwc.rlengine;

import com.android.server.wifi.iwc.IWCLogFile;
import com.android.server.wifi.iwc.RewardEvent;
import com.android.server.wifi.iwc.rlengine.QTable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QTableBuilder {
    private static final boolean DBG = false;
    private static final String TAG = "IWCMonitor.QTableBuilder";
    private JSONObject mJsonObject = null;
    private IWCLogFile mLog = null;

    public QTableBuilder setIWCJsonObject(JSONObject json) {
        this.mJsonObject = json;
        return this;
    }

    public QTableBuilder setIWCLogFile(IWCLogFile logFile) {
        this.mLog = logFile;
        return this;
    }

    public QTable create() throws Exception {
        if (this.mJsonObject == null) {
            return new QTable(QTableContainer.PARAM, this.mLog);
        }
        QTable.SavedMembers savedMembers = new QTable.SavedMembers();
        savedMembers.STEADSTATETHRESHOLD = this.mJsonObject.getInt("STEADSTATETHRESHOLD");
        savedMembers.discountFactor = (float) this.mJsonObject.getDouble("discountFactor");
        savedMembers.eventBuffer = new ArrayList<>();
        JSONArray eventBuffer = this.mJsonObject.getJSONArray("eventBuffer");
        if (eventBuffer != null) {
            for (int b = 0; b < eventBuffer.length(); b++) {
                savedMembers.eventBuffer.add(Integer.valueOf(eventBuffer.getInt(b)));
            }
        }
        savedMembers.eventBufferLimit = this.mJsonObject.getInt("eventBufferLimit");
        savedMembers.firstIndexToggling = this.mJsonObject.getInt("firstIndexToggling");
        savedMembers.isSteadyState = this.mJsonObject.getBoolean("isSteadyState");
        savedMembers.lastAction = this.mJsonObject.getInt("lastAction");
        savedMembers.lastEvent = RewardEvent.valueOf(this.mJsonObject.getString("lastEvent"));
        savedMembers.lastState = this.mJsonObject.getInt("lastState");
        savedMembers.lastUpdateTime = this.mJsonObject.getLong("lastUpdateTime");
        savedMembers.learningRate = (float) this.mJsonObject.getDouble("learningRate");
        savedMembers.mLastAGG = this.mJsonObject.getInt("mLastAGG");
        savedMembers.mLastSNS = this.mJsonObject.getInt("mLastSNS");
        savedMembers.movedFirstIndexToggling = this.mJsonObject.getInt("movedFirstIndexToggling");
        savedMembers.numActions = this.mJsonObject.getInt("numActions");
        savedMembers.numStates = this.mJsonObject.getInt("numStates");
        savedMembers.qTable = (float[][]) Array.newInstance(float.class, new int[]{savedMembers.numStates, savedMembers.numActions});
        JSONArray qTable = this.mJsonObject.getJSONArray("qTable");
        if (qTable != null) {
            for (int b2 = 0; b2 < qTable.length(); b2++) {
                JSONArray each = qTable.getJSONArray(b2);
                if (each != null) {
                    for (int c = 0; c < each.length(); c++) {
                        savedMembers.qTable[b2][c] = (float) each.getDouble(c);
                    }
                }
            }
        }
        savedMembers.state = this.mJsonObject.getInt("state");
        savedMembers.steakTogglingCnt = this.mJsonObject.getInt("steakTogglingCnt");
        savedMembers.zeroIndexReached = this.mJsonObject.getInt("zeroIndexReached");
        return savedMembers.readResolve(this.mLog);
    }

    public JSONObject toJo(QTable qt) throws JSONException {
        JSONObject root = new JSONObject();
        if (qt == null) {
            return root;
        }
        QTable.SavedMembers savedMembers = qt.writeReplace();
        root.put("STEADSTATETHRESHOLD", savedMembers.STEADSTATETHRESHOLD);
        root.put("discountFactor", (double) savedMembers.discountFactor);
        root.put("eventBuffer", getEventBufferArray(savedMembers.eventBuffer));
        root.put("eventBufferLimit", savedMembers.eventBufferLimit);
        root.put("firstIndexToggling", savedMembers.firstIndexToggling);
        root.put("isSteadyState", savedMembers.isSteadyState);
        root.put("lastAction", savedMembers.lastAction);
        root.put("lastEvent", savedMembers.lastEvent);
        root.put("lastState", savedMembers.lastState);
        root.put("lastUpdateTime", savedMembers.lastUpdateTime);
        root.put("learningRate", (double) savedMembers.learningRate);
        root.put("mLastAGG", savedMembers.mLastAGG);
        root.put("mLastSNS", savedMembers.mLastSNS);
        root.put("movedFirstIndexToggling", savedMembers.movedFirstIndexToggling);
        root.put("numActions", savedMembers.numActions);
        root.put("numStates", savedMembers.numStates);
        root.put("qTable", getQtableArray(savedMembers.qTable, savedMembers.numStates, savedMembers.numActions));
        root.put("state", savedMembers.state);
        root.put("steakTogglingCnt", savedMembers.steakTogglingCnt);
        root.put("zeroIndexReached", savedMembers.zeroIndexReached);
        return root;
    }

    private JSONArray getEventBufferArray(ArrayList<Integer> list) throws JSONException {
        JSONArray eventBuffer = new JSONArray();
        if (list != null) {
            Iterator<Integer> it = list.iterator();
            while (it.hasNext()) {
                eventBuffer.put(it.next());
            }
        }
        return eventBuffer;
    }

    private JSONArray getQtableArray(float[][] qTable, int stateNum, int actionNum) throws JSONException {
        JSONArray states = new JSONArray();
        for (int i = 0; i < stateNum; i++) {
            JSONArray actions = new JSONArray();
            for (int j = 0; j < actionNum; j++) {
                actions.put(Float.toString(qTable[i][j]));
            }
            states.put(actions);
        }
        return states;
    }
}
