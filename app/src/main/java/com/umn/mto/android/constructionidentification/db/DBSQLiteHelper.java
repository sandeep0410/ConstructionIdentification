package com.umn.mto.android.constructionidentification.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.umn.mto.android.constructionidentification.com.umn.mto.android.constructionidentification.dto.WorkZonePoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sandeep on 10/3/2015.
 */
public class DBSQLiteHelper extends SQLiteOpenHelper {
    private static final String MTO_CONSTRUCTION = "MTO_Construction";
    private static final int DB_VERSION = 1;
    private static final String KEY_ID = "id";

    public DBSQLiteHelper(Context context) {
        super(context, MTO_CONSTRUCTION, null, DB_VERSION);
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
        values.put(DBUtils.WZ_POINT_ID,one.getPointId());
        values.put(DBUtils.WZ_LATITUDE, one.getLat());
        values.put(DBUtils.WZ_LONGITUDE, one.getLon());
        db.insert(DBUtils.WZ_TABLE, null, values);
        db.close();

    }

    public List<WorkZonePoint> getAllWorkZoneData(){
        List<WorkZonePoint> wzPoints = new ArrayList<WorkZonePoint>();
        String selectQuery = "SELECT *FROM " +DBUtils.WZ_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst()){
            do{
                WorkZonePoint wzp = new WorkZonePoint(-1,-1,-1,-1);
                wzp.setId(Integer.parseInt(cursor.getString(1)));
                wzp.setPointId(Integer.parseInt(cursor.getString(2)));
                wzp.setLat(Integer.parseInt(cursor.getString(3)));
                wzp.setLon(Integer.parseInt(cursor.getString(4)));
                wzPoints.add(wzp);
            }while(cursor.moveToNext());
        }
        return wzPoints;
    }
}
