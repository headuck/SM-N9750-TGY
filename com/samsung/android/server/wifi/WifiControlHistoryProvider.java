package com.samsung.android.server.wifi;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.CursorWindowAllocationException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;
import android.net.Uri;
import android.os.Debug;
import android.text.format.DateFormat;
import android.util.Log;
import java.io.PrintWriter;
import java.util.HashMap;

public class WifiControlHistoryProvider extends ContentProvider {
    private static final String AUTHORITY = "com.samsung.server.wifi";
    private static final Uri CONTENT_URI = Uri.parse(URI_STRING);
    private static final int CONTROL = 1;
    private static final String CONTROL_ID = "conrol_id";
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String DB_NAME = "WifiHistory.db";
    private static final String DB_TABLE = "WifiHistory";
    private static final int DB_VERSION = 1;
    private static final int DB_VERSION_NOP_UPGRADE_FROM = 0;
    private static final int DB_VERSION_NOP_UPGRADE_TO = 1;
    private static final String DISABLE_NUMBER = "disable_number";
    private static final int DISABLE_WIFI = 0;
    private static final String ENABLE_NUMBER = "enable_number";
    private static final int ENABLE_WIFI = 1;
    private static final String FACTORY_RESET = "factory.reset.";
    private static final String FIRST_CONTROL = "first_control";
    private static final String LAST_CONTROL = "last_control";
    private static final String PACKAGE_NAME = "package_name";
    private static final String TAG = "WifiControlHistoryProvider";
    private static final String TIME_STAMP = "time_stamp";
    private static final String URI_STRING = "content://com.samsung.server.wifi/control";
    private static final String WIFI_STATE_CHANGE_WARNING = "state_change_warning.";
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    private SQLiteOpenHelper mOpenHelper = null;

    static {
        sURIMatcher.addURI(AUTHORITY, "control", 1);
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, WifiControlHistoryProvider.DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            Log.v(WifiControlHistoryProvider.TAG, "populating new database");
            WifiControlHistoryProvider.this.createTable(db);
        }

        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (oldV == 0) {
                if (newV != 1) {
                    oldV = 1;
                } else {
                    return;
                }
            }
            Log.i(WifiControlHistoryProvider.TAG, "Upgrading downloads database from version " + oldV + " to " + newV + ", which will destroy all old data");
            WifiControlHistoryProvider.this.dropTable(db);
            WifiControlHistoryProvider.this.createTable(db);
        }
    }

    /* access modifiers changed from: private */
    public void createTable(SQLiteDatabase db) {
        Log.d(TAG, "createTable");
        try {
            db.execSQL("CREATE TABLE WifiHistory(conrol_id INTEGER PRIMARY KEY AUTOINCREMENT,package_name TEXT, time_stamp LONG, enable_number INTEGER, disable_number INTEGER, first_control LONG, last_control INTEGER) ");
        } catch (SQLException e) {
            Log.e(TAG, "couldn't create table in downloads database");
        }
    }

    /* access modifiers changed from: private */
    public void dropTable(SQLiteDatabase db) {
        Log.d(TAG, "dropTable");
        try {
            db.execSQL("DROP TABLE IF EXISTS WifiHistory");
        } catch (SQLException e) {
            Log.e(TAG, "couldn't drop table in downloads database");
        }
    }

    public String getType(Uri uri) {
        if (sURIMatcher.match(uri) == 1 || !DBG) {
            return null;
        }
        Log.d(TAG, "calling getType on an unknown URI: " + uri);
        return null;
    }

    private static final void copyString(String key, ContentValues from, ContentValues to) {
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }
    }

    private static final void copyInteger(String key, ContentValues from, ContentValues to) {
        Integer i = from.getAsInteger(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    private static final void copyLong(String key, ContentValues from, ContentValues to) {
        Long i = from.getAsLong(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Uri uri2 = uri;
        Log.d(TAG, "query");
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            if (sURIMatcher.match(uri2) != 1) {
                if (DBG) {
                    Log.d(TAG, "querying unknown URI: " + uri2);
                }
                return null;
            }
            qb.setTables(DB_TABLE);
            qb.setStrict(true);
            HashMap projectionMap = new HashMap();
            projectionMap.put("package_name", "PACKAGE_NAME");
            projectionMap.put("time_stamp", "TIME_STAMP");
            projectionMap.put(ENABLE_NUMBER, "DISABLE_NUMBER");
            projectionMap.put(DISABLE_NUMBER, "ENABLE_NUMBER");
            projectionMap.put(LAST_CONTROL, "LAST_CONTROL");
            projectionMap.put(FIRST_CONTROL, "FIRST_CONTROL");
            qb.setProjectionMap(projectionMap);
            try {
                Cursor ret = qb.query(db, projection, selection, selectionArgs, (String) null, (String) null, sortOrder);
                if (ret == null && DBG) {
                    Log.d(TAG, "query failed in downloads database");
                }
                return ret;
            } catch (SQLiteException e) {
                Log.e(TAG, "SQLiteException: " + e);
                return null;
            }
        } catch (SQLiteException e2) {
            Log.e(TAG, "Exception: " + e2);
            return null;
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert");
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            if (sURIMatcher.match(uri) != 1) {
                if (DBG) {
                    Log.d(TAG, "calling insert on an unknown/invalid URI: " + uri);
                }
                return null;
            }
            ContentValues filteredValues = new ContentValues();
            copyString("package_name", values, filteredValues);
            Long ts = values.getAsLong("time_stamp");
            if (ts == null) {
                ts = Long.valueOf(System.currentTimeMillis());
            }
            filteredValues.put("time_stamp", ts);
            copyInteger(ENABLE_NUMBER, values, filteredValues);
            copyInteger(DISABLE_NUMBER, values, filteredValues);
            Long ts2 = values.getAsLong(FIRST_CONTROL);
            if (ts2 == null) {
                ts2 = Long.valueOf(System.currentTimeMillis());
            }
            filteredValues.put(FIRST_CONTROL, ts2);
            copyInteger(LAST_CONTROL, values, filteredValues);
            long rowId = db.insert(DB_TABLE, (String) null, filteredValues);
            if (rowId != -1) {
                return Uri.parse(CONTENT_URI + "/" + rowId);
            } else if (!DBG) {
                return null;
            } else {
                Log.d(TAG, "couldn't insert into wificontrol database");
                return null;
            }
        } catch (SQLiteException ex) {
            Log.e(TAG, "Exception: " + ex);
            return null;
        }
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            if (sURIMatcher.match(uri) != 1) {
                if (!DBG) {
                    return 0;
                }
                Log.d(TAG, "updating unknown/invalid URI: " + uri);
                return 0;
            } else if (!values.containsKey("package_name")) {
                Log.e(TAG, "update, there is no kye");
                return 0;
            } else if (values.size() <= 0) {
                return 0;
            } else {
                try {
                    return db.update(DB_TABLE, values, "package_name=?", selectionArgs);
                } catch (SQLiteReadOnlyDatabaseException ex) {
                    if (!DBG) {
                        return 0;
                    }
                    Log.e(TAG, "update, Exception: " + ex);
                    return 0;
                } catch (SQLiteDiskIOException ex2) {
                    if (!DBG) {
                        return 0;
                    }
                    Log.e(TAG, "update, Exception: " + ex2);
                    return 0;
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "update, Exception: " + e);
            return 0;
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            if (sURIMatcher.match(uri) == 1) {
                try {
                    return db.delete(DB_TABLE, "package_name=?", selectionArgs);
                } catch (SQLiteReadOnlyDatabaseException ex) {
                    Log.e(TAG, "delete, Exception: " + ex);
                    return 0;
                } catch (SQLiteDiskIOException ex2) {
                    Log.e(TAG, "delete, Exception: " + ex2);
                    return 0;
                }
            } else if (!DBG) {
                return 0;
            } else {
                Log.d(TAG, "deleting unknown/invalid URI: " + uri);
                return 0;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Exception: " + e);
            return 0;
        }
    }

    public static void setControlHistory(Context context, String packageName, boolean enable) {
        Cursor cursor;
        int controlType;
        String str = packageName;
        if (str == null || packageName.isEmpty()) {
            Log.e(TAG, "setControlHistory, packageName is empty");
            return;
        } else if (!"system".equals(str) && !"android".equals(str) && !str.startsWith("state_change_warning.") && !str.startsWith(FACTORY_RESET)) {
            int count = 0;
            int enableNumber = 0;
            int disableNumber = 0;
            if (enable) {
            }
            Uri uri = Uri.parse(URI_STRING);
            try {
                Uri uri2 = uri;
                try {
                    cursor = context.getContentResolver().query(uri, (String[]) null, "(package_name == '" + str + "')", (String[]) null, (String) null);
                    if (cursor != null) {
                        try {
                            count = cursor.getCount();
                            if (count != 0) {
                                if (DBG) {
                                    Log.d(TAG, "setControlHistory, count is not 0");
                                }
                                cursor.moveToFirst();
                                if (enable) {
                                    enableNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ENABLE_NUMBER)) + 1;
                                } else {
                                    disableNumber = cursor.getInt(cursor.getColumnIndexOrThrow(DISABLE_NUMBER)) + 1;
                                }
                            }
                        } catch (CursorWindowAllocationException e) {
                            Log.e(TAG, "setControlHistory, CursorWindowAllocationException: " + e);
                        } catch (Throwable th) {
                            th = th;
                            Uri uri3 = uri2;
                        }
                        cursor.close();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("package_name", str);
                        if (enable) {
                            controlType = 1;
                        } else {
                            controlType = 0;
                        }
                        long controlTime = System.currentTimeMillis();
                        contentValues.put(LAST_CONTROL, Integer.valueOf(controlType));
                        contentValues.put("time_stamp", Long.valueOf(controlTime));
                        if (count > 0) {
                            if (enable) {
                                contentValues.put(ENABLE_NUMBER, Integer.valueOf(enableNumber));
                            } else {
                                contentValues.put(DISABLE_NUMBER, Integer.valueOf(disableNumber));
                            }
                            if (context.getContentResolver().update(uri2, contentValues, (String) null, new String[]{str}) <= 0) {
                                Log.e(TAG, "setControlHistory, update is faild!");
                                return;
                            } else if (DBG) {
                                Log.d(TAG, "setControlHistory, update is success!");
                                return;
                            } else {
                                return;
                            }
                        } else {
                            Uri uri4 = uri2;
                            if (DBG) {
                                Log.d(TAG, "setControlHistory, count is 0");
                            }
                            contentValues.put(ENABLE_NUMBER, Integer.valueOf(enableNumber));
                            contentValues.put(DISABLE_NUMBER, Integer.valueOf(disableNumber));
                            contentValues.put(FIRST_CONTROL, Long.valueOf(controlTime));
                            if (context.getContentResolver().insert(uri4, contentValues) == null) {
                                Log.e(TAG, "setControlHistory, insert is faild!");
                                return;
                            } else if (DBG) {
                                Log.d(TAG, "setControlHistory, insert is success!");
                                return;
                            } else {
                                return;
                            }
                        }
                    } else {
                        Log.e(TAG, "setControlHistory, cursor is null");
                        return;
                    }
                } catch (SecurityException e2) {
                    e = e2;
                    Uri uri5 = uri2;
                    Log.e(TAG, "setControlHistory, SecurityException: " + e);
                    return;
                }
            } catch (SecurityException e3) {
                e = e3;
                Uri uri6 = uri;
                Log.e(TAG, "setControlHistory, SecurityException: " + e);
                return;
            }
        } else {
            return;
        }
        cursor.close();
        throw th;
    }

    public static void dumpControlHistory(Context context, PrintWriter pw) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse(URI_STRING), (String[]) null, (String) null, (String[]) null, "time_stamp ASC");
            if (cursor == null) {
                Log.e(TAG, "dumpControlHistory, cursor is null");
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            cursor.moveToFirst();
            int indexPackageName = cursor.getColumnIndex("package_name");
            int indexLastControlTime = cursor.getColumnIndex("time_stamp");
            int indexOfLastControl = cursor.getColumnIndex(LAST_CONTROL);
            while (!cursor.isAfterLast()) {
                String packageName = cursor.getString(indexPackageName);
                int lastControl = cursor.getInt(indexOfLastControl);
                CharSequence lastControlTime = DateFormat.format("yy/MM/dd kk:mm:ss ", cursor.getLong(indexLastControlTime));
                StringBuilder sb = new StringBuilder();
                sb.append("lastControlTime: ");
                sb.append(lastControlTime);
                sb.append(", packageName: ");
                sb.append(packageName);
                sb.append(", lastControl: ");
                sb.append(lastControl == 0 ? "false" : "true");
                pw.println(sb.toString());
                cursor.moveToNext();
            }
            cursor.close();
        } catch (IllegalStateException e) {
            Log.e(TAG, "dumpControlHistory, IllegalStateException " + e);
            if (cursor == null) {
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }
}
