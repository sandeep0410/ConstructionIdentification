package com.umn.mto.android.workzonealert.db;


import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.umn.mto.android.workzonealert.SpeedDetectionService;
import com.umn.mto.android.workzonealert.dto.BLETag;
import com.umn.mto.android.workzonealert.dto.WorkZonePoint;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by Sandeep on 10/9/2015.
 */
public class DbDownloader extends Thread {
    private URL url;
    HttpURLConnection urlConnection;
    String table;
    private Context mContext;

    public DbDownloader(Context context, String table) {
        try {
            mContext = context;
            url = new URL("http://128.101.111.92:8080/ConstructionServer/constructionserver");
            //url = new URL("http://10.0.0.47:8080/ConstructionServer/constructionserver");
            this.table = table;
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DBSQLiteHelper db = new DBSQLiteHelper(mContext);
        String data = downloadTable();
        if (data != null) {
            switch (table) {
                case "geofences":
                    List<WorkZonePoint> geofence = parseGeofenceData(data);
                    if (null != geofence) {
                        db.deleteAll(table);
                        Log.d("sandeep", "printing after first delete");
                        printWorkZoneData(db);
                        for (WorkZonePoint wz : geofence) {
                            db.addWorkZoneData(wz);
                        }
                        Log.d("sandeep", "printing after dataa addition");
                        printWorkZoneData(db);
                    }
                    break;
                case "ble_tags":
                    List<BLETag> bletags = parseBLE(data);
                    if (null != bletags) {
                        db.deleteAll(table);
                        Log.d("sandeep", "BTprinting after first delete");
                        printBluetootheData(db);
                        for (BLETag bt : bletags) {
                            db.addBLETagData(bt);
                        }
                        Log.d("sandeep", "BTprinting after dataa addition");
                        printBluetootheData(db);
                    }
                    break;
            }
        }

    }

    private void printBluetootheData(DBSQLiteHelper db) {
        {
            List<BLETag> arr = db.getAllBLEData();
            for (BLETag w : arr) {
                Log.d("sandeep", w.toString());
            }
        }
    }

    private void printWorkZoneData(DBSQLiteHelper db) {
        List<WorkZonePoint> arr = db.getAllWorkZoneData();
        for (WorkZonePoint w : arr) {
           Log.d("sandeep", w.toString());
        }
    }

    public String downloadTable() {
        try {
            urlConnection = (HttpURLConnection) new URL(url.toString() + "?table=" + table+"&latitude="+SpeedDetectionService.updateLat
            +"&longitude="+SpeedDetectionService.updateLon).openConnection();
            /*urlConnection.setRequestProperty("Table", "geofences");
            urlConnection.setRequestProperty("latitude", ""+SpeedDetectionService.updateLat);
            urlConnection.setRequestProperty("longitude", ""+SpeedDetectionService.updateLon);*/
            urlConnection.setConnectTimeout(2000);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            //********Reading Response From Server********
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();
            return data;
            //********Reading Response From Server*********//

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != urlConnection)
                urlConnection.disconnect();
        }
        return null;
    }

    private List<BLETag> parseBLE(String data) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<BLETag>>() {
        }.getType();
        List<BLETag> pojoList = gson.fromJson(data, listType);
        if (pojoList == null) {
            Log.d("sandeep", "null parsed");
            return null;
        }
        return pojoList;
    }

    private List<WorkZonePoint> parseGeofenceData(String data) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<WorkZonePoint>>() {
        }.getType();
        List<WorkZonePoint> pojoList = gson.fromJson(data, listType);
        if (pojoList == null) {
            Log.d("sandeep", "null parsed");
            return null;
        }
        return pojoList;
    }

}
