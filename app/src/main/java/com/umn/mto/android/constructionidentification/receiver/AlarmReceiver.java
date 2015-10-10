package com.umn.mto.android.constructionidentification.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.umn.mto.android.constructionidentification.db.DBSQLiteHelper;
import com.umn.mto.android.constructionidentification.db.DbDownloader;

/**
 * Created by Sandeep on 10/9/2015.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DBSQLiteHelper db = new DBSQLiteHelper(context);
        DbDownloader downloadWZ = new DbDownloader(context, "geofences");
        DbDownloader downloadBT = new DbDownloader(context, "ble_tags");
        //***** Download WZ table and update local database****//
        downloadWZ.start();
        downloadBT.start();

    }


}