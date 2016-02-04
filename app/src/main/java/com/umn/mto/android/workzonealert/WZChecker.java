package com.umn.mto.android.workzonealert;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.umn.mto.android.workzonealert.db.DBSQLiteHelper;

import java.util.List;

/**
 * Created by Sandeep on 1/3/2016.
 */
public class WZChecker {
    static WZChecker _instance = null;
    Context mContext=null;
    Location mCurLocation;
    BLEScanner mScanner;
    Handler mHandler  = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1001:
                    mScanner.scanLeDevice(true);
                    break;
                case 1002:
                    _instance=null;
                    break;
                default:
                    break;
            }
        }
    };

    public WZChecker(Context context, Location location){
        mContext = context;
        mCurLocation = location;
        _instance=this;
    }

    public static WZChecker getInstance(){
        return _instance;
    }

    public void checkScan(BLEScanner scanner){
        mScanner = scanner;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DBSQLiteHelper db = DBSQLiteHelper.getInstance(mContext);
                List<Integer> workzones = db.getWorkZoneIDs();
                for(int id: workzones){
                    List<Location> locations = db.getWorkZonePoints(id);
                    LocationValidator validator = new LocationValidator();
                    if(validator.isPointInPolygon(locations, mCurLocation)) {
                        mHandler.sendEmptyMessage(1001);
                        break;
                    }
                }
                mHandler.sendEmptyMessage(1002);
            }
        });
    thread.start();
    }

}
