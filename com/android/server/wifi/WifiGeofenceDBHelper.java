package com.android.server.wifi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.samsung.android.location.SemLocationManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class WifiGeofenceDBHelper extends SQLiteOpenHelper {
    public static final String CREATE_WIFI_GEOFENCE_TABLE = "CREATE TABLE geofence_wifi(_id INTEGER PRIMARY KEY AUTOINCREMENT,location_id INTEGER DEFAULT 0,network_id INTEGER DEFAULT 0,config_key STRING,bssid STRING,time LONG NOT NULL,latitude DOUBLE DEFAULT 1000.0,longitude DOUBLE DEFAULT 1000.0,time_major LONG NOT NULL,latitude_major DOUBLE DEFAULT 1000.0,longitude_major DOUBLE DEFAULT 1000.0)";
    public static final int DATABASE_MAX_SIZE = 100;
    private static final String DATABASE_NAME = "wifigeofence.db";
    public static final String KEY_BSSID = "bssid";
    public static final String KEY_CONFIG_KEY = "config_key";
    public static final String KEY_ID = "_id";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LATITUDE_MAJOR = "latitude_major";
    public static final String KEY_LOCATIONID = "location_id";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LONGITUDE_MAJOR = "longitude_major";
    public static final String KEY_NETWORKID = "network_id";
    public static final String KEY_TIME = "time";
    public static final String KEY_TIME_MAJOR = "time_major";
    public static final String TABLE_NAME = "geofence_wifi";
    private static final String TAG = "WifiGeofenceDBHelper";
    private static final int mDBVersion = 4;
    HashMap<Integer, WifiGeofenceData> mDataList = new HashMap<>();
    /* access modifiers changed from: private */
    public SemLocationManager mSLocationManager;

    public WifiGeofenceDBHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 4);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_WIFI_GEOFENCE_TABLE);
        Log.d(TAG, "onCreate() ");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() - oldVersion : " + oldVersion + ", newVersion : " + newVersion);
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN latitude DOUBLE DEFAULT 1000.0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN longitude DOUBLE DEFAULT 1000.0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN time_major LONG DEFAULT 0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN latitude_major DOUBLE DEFAULT 1000.0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN longitude_major DOUBLE DEFAULT 1000.0");
        } else if (oldVersion > 1 && oldVersion < 4) {
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN time_major LONG DEFAULT 0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN latitude_major DOUBLE DEFAULT 1000.0");
            db.execSQL("ALTER TABLE geofence_wifi ADD COLUMN longitude_major DOUBLE DEFAULT 1000.0");
        }
    }

    public void onRemove(SQLiteDatabase db) {
        Log.d(TAG, "onRemove() ");
        db.execSQL("DROP TABLE IF EXISTS geofence_wifi");
    }

    public void insert(int locationID, int networkID, String configKey, String BSSID, long time) {
        int i = locationID;
        String str = configKey;
        String str2 = BSSID;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues data = new ContentValues();
        data.put(KEY_LOCATIONID, Integer.valueOf(locationID));
        data.put(KEY_NETWORKID, Integer.valueOf(networkID));
        data.put(KEY_CONFIG_KEY, str);
        data.put("bssid", str2);
        data.put("time", 0L);
        data.put(KEY_LATITUDE, 0);
        data.put(KEY_LONGITUDE, 0);
        data.put(KEY_TIME_MAJOR, Long.valueOf(time));
        data.put(KEY_LATITUDE_MAJOR, 1000L);
        data.put(KEY_LONGITUDE_MAJOR, 1000L);
        db.insert(TABLE_NAME, (String) null, data);
        Log.d(TAG, "insert() - locationID : " + i + ", configKey : " + str + ", networkID : " + networkID + ", BSSID : " + str2 + ", time : " + time);
        ContentValues contentValues = data;
        this.mDataList.put(new Integer(i), new WifiGeofenceData(locationID, networkID, configKey, BSSID, time, 1000.0d, 1000.0d));
    }

    public void update(int locationID, long time) {
        SQLiteDatabase db = getWritableDatabase();
        if (find(locationID) != 0) {
            ContentValues data = new ContentValues();
            data.put(KEY_TIME_MAJOR, Long.valueOf(time));
            db.update(TABLE_NAME, data, "location_id=" + locationID, (String[]) null);
            WifiGeofenceData geofenceData = this.mDataList.get(new Integer(locationID));
            if (geofenceData != null) {
                Log.d(TAG, "update() - " + locationID + "*" + time);
                geofenceData.setLastConnectedTime(time);
            }
        }
    }

    public void update(int locationID, double latitude, double longitude) {
        SQLiteDatabase db = getWritableDatabase();
        if (find(locationID) != 0) {
            ContentValues data = new ContentValues();
            data.put(KEY_LATITUDE_MAJOR, Double.valueOf(latitude));
            data.put(KEY_LONGITUDE_MAJOR, Double.valueOf(longitude));
            db.update(TABLE_NAME, data, "location_id=" + locationID, (String[]) null);
            WifiGeofenceData geofenceData = this.mDataList.get(new Integer(locationID));
            if (geofenceData != null) {
                Log.d(TAG, "update Location - " + locationID);
                geofenceData.setLatitude(latitude);
                geofenceData.setLongitude(longitude);
            }
        }
    }

    public void delete(String configKey) {
        SQLiteDatabase db = getWritableDatabase();
        int locationId = getLocationId(configKey);
        Log.d(TAG, "WifiGeofenceDBHelper delete - " + configKey + ", location Id - " + locationId);
        if (locationId != -1) {
            db.delete(TABLE_NAME, "location_id=" + locationId, (String[]) null);
            this.mDataList.remove(Integer.valueOf(locationId));
        }
    }

    public void delete(int locationId) {
        SQLiteDatabase db = getWritableDatabase();
        Log.d(TAG, "WifiGeofenceDBHelper delete - location Id - " + locationId);
        if (locationId != -1) {
            db.delete(TABLE_NAME, "location_id=" + locationId, (String[]) null);
        }
    }

    private int getLocationId(String configKey) {
        int locationId = -1;
        Cursor cursor = getReadableDatabase().rawQuery("select * from geofence_wifi", (String[]) null);
        if (cursor != null) {
            while (true) {
                if (cursor.moveToNext()) {
                    if (configKey.equals(cursor.getString(3))) {
                        locationId = cursor.getInt(1);
                        break;
                    }
                } else {
                    break;
                }
            }
            cursor.close();
        }
        return locationId;
    }

    public int find(int locationid) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from geofence_wifi", (String[]) null);
        if (cursor == null) {
            return 0;
        }
        while (cursor.moveToNext()) {
            int _locationid = cursor.getInt(1);
            if (_locationid == locationid) {
                cursor.close();
                return _locationid;
            }
        }
        cursor.close();
        return 0;
    }

    public HashMap<Integer, WifiGeofenceData> select() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from geofence_wifi", (String[]) null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int _locationid = cursor.getInt(1);
                int _networkid = cursor.getInt(2);
                String _configKey = cursor.getString(3);
                String _bssid = cursor.getString(4);
                long _time = cursor.getLong(8);
                double _latitude = cursor.getDouble(9);
                double _longitude = cursor.getDouble(10);
                double _latitude2 = _latitude;
                SQLiteDatabase db2 = db;
                Cursor cursor2 = cursor;
                this.mDataList.put(new Integer(_locationid), new WifiGeofenceData(_locationid, _networkid, _configKey, _bssid, _time, _latitude2, _longitude));
                Log.e(TAG, "WifiGeofenceDBHelper select - *" + _locationid + "*" + _networkid + "*" + _configKey + "*" + _bssid + "*" + _time + "*" + isValidLocation(_latitude2, _longitude));
                db = db2;
                cursor = cursor2;
            }
            cursor.close();
        } else {
            Cursor cursor3 = cursor;
        }
        return this.mDataList;
    }

    /* access modifiers changed from: package-private */
    public boolean isValidLocation(double latitude, double longitude) {
        if (latitude < -90.0d || latitude > 90.0d || longitude < -180.0d || longitude > 180.0d) {
            return false;
        }
        return true;
    }

    public HashMap<Integer, WifiGeofenceData> getDataList() {
        return this.mDataList;
    }

    public int getLocationIdFromOldTime() {
        int locationId = -1;
        Cursor cursor = getReadableDatabase().rawQuery("select location_id from geofence_wifi order by time", (String[]) null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                locationId = cursor.getInt(0);
            }
            cursor.close();
        }
        return locationId;
    }

    public void setSLocationManager(SemLocationManager sLocationManager) {
        if (sLocationManager != null) {
            this.mSLocationManager = sLocationManager;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiGeofenceDB :");
        HashMap<Integer, WifiGeofenceData> hashMap = this.mDataList;
        if (hashMap != null) {
            for (Integer key : hashMap.keySet()) {
                pw.println(this.mDataList.get(key).printDump());
            }
            pw.println();
        }
    }

    public class WifiGeofenceData {
        private String mBssid;
        private int mCellCount = 0;
        private String mConfigKey;
        private int mIsGeofenceEnter = 0;
        private long mLastConnectedTime;
        private double mLatitude;
        private int mLocationid;
        private double mLongitude;
        private int mNetworkid;

        public WifiGeofenceData(int locationId, int networkId, String configKey, String bssid, long lastConnectedTime, double latitude, double longitude) {
            this.mLocationid = locationId;
            this.mNetworkid = networkId;
            this.mConfigKey = configKey;
            this.mBssid = bssid;
            this.mLastConnectedTime = lastConnectedTime;
            this.mLatitude = latitude;
            this.mLongitude = longitude;
        }

        public int getLocationId() {
            return this.mLocationid;
        }

        public int getNetworkId() {
            return this.mNetworkid;
        }

        public String getConfigKey() {
            return this.mConfigKey;
        }

        public void setConfigKey(String configKey) {
            this.mConfigKey = configKey;
        }

        public String getBssid() {
            return this.mBssid;
        }

        public long getLastConnectedTime() {
            return this.mLastConnectedTime;
        }

        public void setLastConnectedTime(long time) {
            this.mLastConnectedTime = time;
        }

        public double getLatitude() {
            return this.mLatitude;
        }

        public void setLatitude(double latitude) {
            this.mLatitude = latitude;
        }

        public double getLongitude() {
            return this.mLongitude;
        }

        public void setLongitude(double longitude) {
            this.mLongitude = longitude;
        }

        public void setIsGeofenceEnter(int isEnter) {
            this.mIsGeofenceEnter = isEnter;
        }

        public int getIsGeofenceEnter() {
            return this.mIsGeofenceEnter;
        }

        public String getGeofenceStateToString() {
            int i = this.mIsGeofenceEnter;
            if (i == 0) {
                return "UNKNOWN";
            }
            if (i == 1) {
                return "ENTER";
            }
            if (i != 2) {
                return "";
            }
            return "EXIT";
        }

        public int getCellCount(int locationId) {
            if (WifiGeofenceDBHelper.this.mSLocationManager != null) {
                Log.d(WifiGeofenceDBHelper.TAG, "getCellCount : " + WifiGeofenceDBHelper.this.mSLocationManager.getCellCountForEventGeofence(locationId));
                return WifiGeofenceDBHelper.this.mSLocationManager.getCellCountForEventGeofence(locationId);
            }
            Log.d(WifiGeofenceDBHelper.TAG, "getCellCount :  -1 ");
            return -1;
        }

        public String printDump() {
            return new String("locationId : " + this.mLocationid + ", networkId : " + this.mNetworkid + ", " + this.mConfigKey + " lastConnectedTime : " + this.mLastConnectedTime + " " + this.mIsGeofenceEnter + " " + getGeofenceStateToString() + ", number of cells : " + getCellCount(this.mLocationid));
        }
    }
}
