package com.samsung.android.server.wifi.softap;

import android.annotation.SystemApi;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;

public class SemWifiApContentProvider extends ContentProvider {
    static final Uri CONTENT_URI = Uri.parse(URL);
    static final String CREATE_DB_TABLE = " CREATE TABLE SemWifiApContentProvider (_id INTEGER PRIMARY KEY AUTOINCREMENT,  name TEXT NOT NULL,  value TEXT NOT NULL);";
    static final String DATABASE_NAME = "SemWifiApContentProvider.db";
    static final int DATABASE_VERSION = 1;
    static final String NAME = "name";
    static final String PROVIDER_NAME = "com.samsung.android.wifi.softap";
    static final int SOFTAPINFO = 1;
    static final int SOFTAPINFO_ID = 2;
    private static HashMap<String, String> SOFTAPINFO_PROJECTION_MAP = null;
    static final String SOFTAPINFO_TABLE_NAME = "SemWifiApContentProvider";
    static final String URL = "content://com.samsung.android.wifi.softap/softapInfo";
    static final String VALUE = "value";
    static final String _ID = "_id";
    static final UriMatcher uriMatcher = new UriMatcher(-1);
    private final String TAG = SOFTAPINFO_TABLE_NAME;

    /* renamed from: db */
    private SQLiteDatabase f60db;

    static {
        uriMatcher.addURI(PROVIDER_NAME, "softapInfo", 1);
        uriMatcher.addURI(PROVIDER_NAME, "softapInfo/#", 2);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, SemWifiApContentProvider.DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SemWifiApContentProvider.CREATE_DB_TABLE);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS SemWifiApContentProvider");
            onCreate(db);
        }
    }

    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Uri insert(Uri uri, ContentValues values) {
        long rowID = this.f60db.insert(SOFTAPINFO_TABLE_NAME, "", values);
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            Log.i(SOFTAPINFO_TABLE_NAME, "inserted" + _uri);
            return _uri;
        }
        Log.e(SOFTAPINFO_TABLE_NAME, "Could not add" + uri);
        return null;
    }

    public boolean onCreate() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        Log.d(SOFTAPINFO_TABLE_NAME, "OnCreate");
        this.f60db = dbHelper.getWritableDatabase();
        return this.f60db != null;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SOFTAPINFO_TABLE_NAME);
        int match = uriMatcher.match(uri);
        if (match == 1) {
            qb.setProjectionMap(SOFTAPINFO_PROJECTION_MAP);
        } else if (match == 2) {
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));
        }
        return qb.query(this.f60db, projection, selection, selectionArgs, (String) null, (String) null, sortOrder);
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String str;
        int match = uriMatcher.match(uri);
        if (match == 1) {
            return this.f60db.delete(SOFTAPINFO_TABLE_NAME, selection, selectionArgs);
        }
        if (match != 2) {
            Log.d(SOFTAPINFO_TABLE_NAME, "delete Unknown URI " + uri);
            return 0;
        }
        SQLiteDatabase sQLiteDatabase = this.f60db;
        StringBuilder sb = new StringBuilder();
        sb.append("_id = ");
        sb.append(uri.getPathSegments().get(1));
        if (!TextUtils.isEmpty(selection)) {
            str = " AND (" + selection + ')';
        } else {
            str = "";
        }
        sb.append(str);
        return sQLiteDatabase.delete(SOFTAPINFO_TABLE_NAME, sb.toString(), selectionArgs);
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        String str;
        int match = uriMatcher.match(uri);
        if (match == 1) {
            count = this.f60db.update(SOFTAPINFO_TABLE_NAME, values, selection, selectionArgs);
        } else if (match != 2) {
            Log.e(SOFTAPINFO_TABLE_NAME, "Could not update" + uri);
            return 0;
        } else {
            SQLiteDatabase sQLiteDatabase = this.f60db;
            StringBuilder sb = new StringBuilder();
            sb.append("_id = ");
            sb.append(uri.getPathSegments().get(1));
            if (!TextUtils.isEmpty(selection)) {
                str = " AND (" + selection + ')';
            } else {
                str = "";
            }
            sb.append(str);
            count = sQLiteDatabase.update(SOFTAPINFO_TABLE_NAME, values, sb.toString(), selectionArgs);
        }
        Log.i(SOFTAPINFO_TABLE_NAME, "updated:" + uri);
        return count;
    }

    @SystemApi
    public static void insert(Context mContext, String key, String val) {
        ContentValues values = new ContentValues();
        if (val == null) {
            val = "";
        }
        values.put(NAME, key);
        values.put(VALUE, val);
        if (isKeypresent(mContext, key)) {
            int update = mContext.getContentResolver().update(CONTENT_URI, values, (String) null, (String[]) null);
        } else {
            Uri insert = mContext.getContentResolver().insert(CONTENT_URI, values);
        }
    }

    @SystemApi
    public static String get(Context mContext, String key) {
        Cursor c = mContext.getContentResolver().query(Uri.parse("content://com.samsung.android.wifi.softap"), (String[]) null, "name = ?", new String[]{key}, (String) null);
        String returnValue = "";
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    returnValue = c.getString(c.getColumnIndex(VALUE));
                }
            } finally {
                c.close();
            }
        }
        return returnValue;
    }

    private static boolean isKeypresent(Context mContext, String key) {
        Cursor c = mContext.getContentResolver().query(Uri.parse("content://com.samsung.android.wifi.softap"), (String[]) null, "name = ?", new String[]{key}, (String) null);
        if (c == null) {
            return false;
        }
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }
}
