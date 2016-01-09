package com.umn.mto.android.workzonealert;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.umn.mto.android.workzonealert.imagewarning.ImageWarningActivity;
import com.umn.mto.android.workzonealert.receiver.AlarmReceiver;
import com.umn.mto.android.workzonealert.receiver.PhoneCallStateListener;
import com.umn.mto.android.workzonealert.settings.Settings;
import com.umn.mto.android.workzonealert.warning.WarningActivity;
import com.umn.mto.android.workzonealert.warning.WarningReceiver;

public class SpeedDetectionService extends Service {
    public static class NotificationConstants {
        public static final String START_SPEED_DETECTION_SERVICE = "Start Speed Detection Service";
        public static final String MAIN = "Main";
    }

    static final Double EARTH_RADIUS = 6371.00;
    public static float mSpeed = 0;
    Thread t;
    private LocationManager locManager;
    private LocationListener locListener = new myLocationListener();
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    private Handler handler = new Handler();
    SharedPreferences mPrefs;
    private BLEScanner mScanner;
    private WZChecker wzChecker;
    public static double updateLat = -1;
    public static double updateLon = -1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Toast.makeText(getBaseContext(), "Service Started", Toast.LENGTH_SHORT).show();
        mPrefs = this.getSharedPreferences(
                "com.umn.mto.android.workzonealert", Context.MODE_PRIVATE);
        mSpeed = mPrefs.getFloat("Speed", mSpeed);
        startForeground(1, getNotification());

        final Runnable r = new Runnable() {
            public void run() {
                location();
            }
        };
        handler.postDelayed(r, 3000);
        storeSharedSettings();
        startDataBaseAlarm();
        return START_STICKY;
    }

    private void startDataBaseAlarm() {
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long interval = 30 * 60 * 1000;
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        if (mPrefs != null)
            mPrefs = this.getSharedPreferences(
                    "com.umn.mto.android.workzonealert", Context.MODE_PRIVATE);
        updateLat = mPrefs.getFloat("DatabaseUpdateLatitude", -1);
        updateLon = mPrefs.getFloat("DatabaseUpdateLongitude", -1);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Exiting Service", Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(
                "com.umn.mto.android.workzonealert", Context.MODE_PRIVATE).edit();
        editor.putFloat("Speed", mSpeed);
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
        LogUtils.log("in on create.. 2");
        if (gps_enabled) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
            LogUtils.log("Enabled..");
        }
        if (network_enabled) {
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
            LogUtils.log("Disabled..");
        }
        LogUtils.log("in on create..3");
    }


    private void storeSharedSettings() {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.umn.mto.android.workzonealert", Context.MODE_PRIVATE);
        Settings.vibration = prefs.getBoolean(Settings.VIBRATION, false);
        Settings.alarm = prefs.getBoolean(Settings.ALARM, true);
        Settings.data_collection = prefs.getBoolean(Settings.DATA_COLLECTION, true);
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
            if (!location.hasSpeed() || location.getSpeed() == 0) {
                Location locationNET = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (null != locationNET) {
                    if (locationNET.getSpeed() != mSpeed) {
                        mSpeed = locationNET.getSpeed();
                        updateNotification();
                    }
                }
            } else {
                if (location.getSpeed() != mSpeed) {
                    mSpeed = location.getSpeed();
                    updateNotification();
                }
            }
            if (mSpeed > PhoneCallStateListener.MAX_ALLOWED_SPEED && Settings.overspeed_block) {
                if (WarningActivity.getInstance() == null && ScanningActivity.getInstance() == null) {
                    Intent i = new Intent();
                    i.setAction(WarningReceiver.RAISE_WARNING);
                    sendBroadcast(i);
                }
            } else {
                if (WarningActivity.getInstance() != null) {
                    Intent i = new Intent();
                    i.setAction(WarningReceiver.STOP_WARNING);
                    sendBroadcast(i);
                }

            }

            if (location != null) {
                updateDBifRequired(location);
            }
            LogUtils.log("Location Update received");
            if (!(ScanningActivity.getInstance() != null && ScanningActivity.mScanning)) {
                if (BLEScanner.getInstance() == null || BLEScanner.getInstance().isScanning() == false) {
                    if(WZChecker.getInstance()==null && ImageWarningActivity.getInstance()==null){
                        LogUtils.log("Starting Scan");
                        wzChecker = new WZChecker(getApplicationContext(), location);
                        mScanner = new BLEScanner(getApplicationContext());
                        wzChecker.checkScan(mScanner);
                    }

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

    private void updateDBifRequired(Location loc) {
        if (updateLat < 0)
            sendUpdateDBBroadCast(loc);
        else {
            Location savedLoc = new Location("Old Location");
            savedLoc.setLongitude(updateLon);
            savedLoc.setLatitude(updateLat);
            if (loc.distanceTo(savedLoc) > 50 * 1609)
                sendUpdateDBBroadCast(loc);
        }
    }

    private void sendUpdateDBBroadCast(Location loc) {
        if (Util.isOnline(this)) {
            Intent i = new Intent();
            i.setAction("com.umn.mto.android.DOWNLOAD.START");
            sendBroadcast(i);
            SharedPreferences.Editor editor = this.getSharedPreferences(
                    "com.umn.mto.android.workzonealert", Context.MODE_PRIVATE).edit();
            editor.putFloat("DatabaseUpdateLatitude", (float) loc.getLatitude());
            editor.putFloat("DatabaseUpdateLongitude", (float) loc.getLongitude());
            editor.apply();
            updateLat = loc.getLatitude();
            updateLon = loc.getLongitude();
        }
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, getNotification());
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_stat);
        builder.setContentTitle("Speed Monitored");
        builder.setContentText("Current Speed: " + mSpeed);
        Notification notification = builder.build();
        return notification;
    }


} 

