package com.umn.mto.android.workzonealert.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.umn.mto.android.workzonealert.Util;
import com.umn.mto.android.workzonealert.db.DBSQLiteHelper;
import com.umn.mto.android.workzonealert.db.DbDownloader;

/**
 * Created by Sandeep on 10/9/2015.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()!=null && intent.getAction().equals("com.umn.mto.android.DOWNLOAD.START"))
            Log.d("sandeep", "Received Broadcast");
        if (Util.isOnline(context)) {
            DBSQLiteHelper db = new DBSQLiteHelper(context);
            DbDownloader downloadWZ = new DbDownloader(context, "geofences");
            DbDownloader downloadBT = new DbDownloader(context, "ble_tags");
            //***** Download WZ table and update local database****//
            downloadWZ.start();
            downloadBT.start();
        }

    }


}