package com.umn.mto.android.constructionidentification;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.umn.mto.android.constructionidentification.receiver.PhoneCallStateListener;
import com.umn.mto.android.constructionidentification.settings.Settings;
import com.umn.mto.android.constructionidentification.warning.WarningActivity;
import com.umn.mto.android.constructionidentification.warning.WarningReceiver;

public class SpeedDetectionService extends Service {
    static final Double EARTH_RADIUS = 6371.00;
    public static float mSpeed = 0;
    Thread t;
    private LocationManager locManager;
    private LocationListener locListener = new myLocationListener();
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(getBaseContext(), "Service Started", Toast.LENGTH_SHORT).show();

        final Runnable r = new Runnable() {
            public void run() {
                Log.v("Debug", "Hello");
                location();
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(r, 5000);
        storeSharedSettings();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Exiting Service", Toast.LENGTH_SHORT).show();
    }

    public void location() {
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            gps_enabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
        Log.v("Debug", "in on create.. 2");
        if (gps_enabled) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
            Log.v("Debug", "Enabled..");
        }
        if (network_enabled) {
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
            Log.v("Debug", "Disabled..");
        }
        Log.v("Debug", "in on create..3");
    }

    public double CalculationByDistance(double lat1, double lon1, double lat2, double lon2) {
        double Radius = EARTH_RADIUS;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return Radius * c;
    }
    private void storeSharedSettings() {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.umn.mto.android.constructionidentification", Context.MODE_PRIVATE);
        Settings.vibration = prefs.getBoolean(Settings.VIBRATION,false);
        Settings.alarm = prefs.getBoolean(Settings.ALARM, true);
        Settings.data_collection = prefs.getBoolean(Settings.DATA_COLLECTION,  true);
        Settings.display_alert = prefs.getBoolean(Settings.DISPLAY_ALERT, true);
        Settings.enable_calls = prefs.getBoolean(Settings.ENABLE_CALLS, false);
        Settings.rssi_value = prefs.getInt(Settings.RSSI_VALUE, 128);
        Settings.scan_Time = prefs.getInt(Settings.SCAN_TIME, 100);
        Settings.overspeed_block = prefs.getBoolean(Settings.OVERSPEED_BLOCK, false);
    }
    private class myLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
        /*    Toast.makeText(getApplicationContext(), "Current speed:" + location.getSpeed(),
            	Toast.LENGTH_SHORT).show();*/
            Log.d("sandeep", "" + location.getSpeed());
            if (!location.hasSpeed() || location.getSpeed() == 0) {
                Location locationNET = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (null != locationNET)
                    mSpeed = locationNET.getSpeed();
            } else
                mSpeed = location.getSpeed();
            if(mSpeed > PhoneCallStateListener.MAX_ALLOWED_SPEED && Settings.overspeed_block){
                if(WarningActivity.getInstance() == null && ScanningActivity.getInstance() == null){
                    Intent i = new Intent();
                    i.setAction(WarningReceiver.RAISE_WARNING);
                    sendBroadcast(i);
                }
            }else{
                if(WarningActivity.getInstance()!=null){
                    Intent i = new Intent();
                    i.setAction(WarningReceiver.STOP_WARNING);
                    sendBroadcast(i);
                }

            }

        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
} 

