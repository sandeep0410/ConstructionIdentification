package com.umn.mto.android.workzonealert.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.umn.mto.android.workzonealert.dto.BLETag;
import com.umn.mto.android.workzonealert.dto.WorkZonePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sandeep on 10/3/2015.
 */
public class DBSQLiteHelper extends SQLiteOpenHelper {
    private static final String WORKZONE_ALERT = "WORKZONE_ALERT";
    private static final int DB_VERSION = 1;
    private static final String KEY_ID = "id";

    public DBSQLiteHelper(Context context) {
        super(context, WORKZONE_ALERT, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create Table for WorkZones
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE TABLE ").append(DBUtils.WZ_TABLE)
                .append("(")
                .append(KEY_ID)
                .append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(DBUtils.WZ_ID)
                .append(" INTEGER, ")
                .append(DBUtils.WZ_POINT_ID)
                .append(" INTEGER, ")
                .append(DBUtils.WZ_LATITUDE)
                .append(" DOUBLE, ")
                .append(DBUtils.WZ_LONGITUDE)
                .append(" DOUBLE);");
        db.execSQL(sb.toString());
        sb.setLength(0);
        //create Table for BLE Tags
        sb.append("CREATE TABLE ")
                .append(DBUtils.BLETAG_TABLE)
                .append(" (")
                .append(KEY_ID)
                .append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(DBUtils.BLETAG_WZ_ID)
                .append(" INTEGER, ")
                .append(DBUtils.BLETAG_BLE_MAC)
                .append(" TEXT, ")
                .append(DBUtils.BLETAG_LATITUDE)
                .append(" DOUBLE, ")
                .append(DBUtils.BLETAG_LONGITUDE)
                .append(" DOUBLE, ")
                .append(DBUtils.BLETAG_SPEED_LIMIT)
                .append(" INTEGER, ")
                .append(DBUtils.BLETAG_MESSAGE)
                .append(" TEXT, ")
                .append(DBUtils.BLETAG_FLAG)
                .append(" INTEGER, ")
                .append(DBUtils.BLETAG_FILEPATH)
                .append(" TEXT);");
        db.execSQL(sb.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addWorkZoneData(WorkZonePoint one) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBUtils.WZ_ID, one.getId());
        values.put(DBUtils.WZ_POINT_ID, one.getPointId());
        values.put(DBUtils.WZ_LATITUDE, one.getLat());
        values.put(DBUtils.WZ_LONGITUDE, one.getLon());
        db.insert(DBUtils.WZ_TABLE, null, values);
        db.close();

    }


    public void addBLETagData(BLETag one) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBUtils.BLETAG_WZ_ID, one.getWorkzoneID());
        values.put(DBUtils.BLETAG_BLE_MAC, one.getBleMac());
        values.put(DBUtils.BLETAG_LATITUDE, one.getLat());
        values.put(DBUtils.BLETAG_LONGITUDE, one.getLon());
        values.put(DBUtils.BLETAG_SPEED_LIMIT, one.getSpeedLimit());
        values.put(DBUtils.BLETAG_MESSAGE, one.getMessage());
        values.put(DBUtils.BLETAG_FLAG, one.getFlag());
        values.put(DBUtils.BLETAG_FILEPATH, one.getFileName());
        db.insert(DBUtils.BLETAG_TABLE, null, values);
        db.close();

    }

    public List<WorkZonePoint> getAllWorkZoneData() {
        List<WorkZonePoint> wzPoints = new ArrayList<WorkZonePoint>();
        String selectQuery = "SELECT *FROM " + DBUtils.WZ_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                WorkZonePoint wzp = new WorkZonePoint(-1, -1, -1, -1);
                wzp.setId(Integer.parseInt(cursor.getString(1)));
                wzp.setPointId(Integer.parseInt(cursor.getString(2)));
                wzp.setLat(Double.parseDouble(cursor.getString(3)));
                wzp.setLon(Double.parseDouble(cursor.getString(4)));
                wzPoints.add(wzp);
            } while (cursor.moveToNext());
        }
        db.close();
        return wzPoints;
    }

    public List<BLETag> getAllBLEData() {
        List<BLETag> bleTags = new ArrayList<BLETag>();
        String selectQuery = "SELECT *FROM " + DBUtils.BLETAG_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                BLETag bt = new BLETag(-1, "", -1, -1, -1, "", -1, "");
                bt.setWorkzoneID(Integer.parseInt(cursor.getString(1)));
                bt.setBleMac(cursor.getString(2));
                bt.setLat(Double.parseDouble(cursor.getString(3)));
                bt.setLon(Double.parseDouble(cursor.getString(4)));
                bt.setSpeedLimit(Integer.parseInt(cursor.getString(5)));
                bt.setMessage(cursor.getString(6));
                bt.setFlag(Integer.parseInt(cursor.getString(7)));
                bt.setFileName(cursor.getString(8));
                bleTags.add(bt);
            } while (cursor.moveToNext());
        }
        db.close();
        return bleTags;
    }

    public void deleteAll(String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + table);
        db.close();
    }

    public BLETag getBleTag(String address) {
        String query = "SELECT * FROM " + DBUtils.BLETAG_TABLE + " WHERE " + DBUtils.BLETAG_BLE_MAC + " LIKE '%" + address + "%'";
        Log.d("sandeep", "printing query: " + query);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        BLETag bt = null;

        if (cursor.moveToFirst()) {
            do {
                bt = new BLETag(-1, "", -1, -1, -1, "", -1, "");
                bt.setWorkzoneID(Integer.parseInt(cursor.getString(1)));
                bt.setBleMac(cursor.getString(2));
                bt.setLat(Double.parseDouble(cursor.getString(3)));
                bt.setLon(Double.parseDouble(cursor.getString(4)));
                bt.setSpeedLimit(Integer.parseInt(cursor.getString(5)));
                bt.setMessage(cursor.getString(6));
                bt.setFlag(Integer.parseInt(cursor.getString(7)));
                bt.setFileName(cursor.getString(8));
            } while (cursor.moveToNext());
        }
        Log.d("sandeep", "printing after query: " + bt);
        db.close();
        return bt;
    }
}
