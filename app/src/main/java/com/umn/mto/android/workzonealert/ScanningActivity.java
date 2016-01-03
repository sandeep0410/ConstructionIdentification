package com.umn.mto.android.workzonealert;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import com.umn.mto.android.workzonealert.db.DBSQLiteHelper;
import com.umn.mto.android.workzonealert.dto.BLETag;
import com.umn.mto.android.workzonealert.dto.BluetoothDeviceObject;
import com.umn.mto.android.workzonealert.settings.ImageNotificationDialogFragment;
import com.umn.mto.android.workzonealert.settings.SettingDialogFragment;
import com.umn.mto.android.workzonealert.settings.Settings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScanningActivity extends ListActivity implements LocationListener {
    static ScanningActivity _instance;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DIR_NAME = "MTO_BLE";
    public static final String TO_SPEAK = "Data Not Found!";
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;
    private static final Map<String, Integer> imageIDs = new HashMap<>();
    public static boolean mScanning;
    ToneGenerator mBeep;
    Vibrator mVibrator;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mScanner;
    private float mSpeed = 0;
    private Handler mHandler;
    private HashMap<BluetoothDevice, Integer> scannedDevices = new HashMap<BluetoothDevice, Integer>();
    private static String mDistance;
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private Toast mToast;
    private int mSdkVersion;
    TextToSpeech tts;
    AudioManager audiomanager;
    ScheduledExecutorService mExecutor;
    volatile private Map<String, BluetoothDeviceObject> currentScannedList = new HashMap<String, BluetoothDeviceObject>();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d("sandeep", "writing.. " + currentScannedList.size());
            for (Map.Entry<String, BluetoothDeviceObject> bluetoothobject : currentScannedList.entrySet()) {
                writeDatatoFile(bluetoothobject.getValue().device, bluetoothobject.getValue().rssi);
            }
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d("sandeep", "new Device: " + device.getName());
                    if (null == device.getName() || !device.getName().startsWith("MTO"))
                        return;
                    if (null == device.getName())
                        return;
                    if (rssi < (-1 * Settings.rssi_value))
                        return;
                    startNotificationToneAndVibrate(device, rssi);
                    /*if (Settings.data_collection)
                        writeDatatoFile(device, rssi);*/
                    currentScannedList.put(device.getName(), new BluetoothDeviceObject(device, rssi));
                    if (scannedDevices.containsKey(device.getName())) {
                        if (scannedDevices.get(device) < rssi)
                            scannedDevices.put(device, rssi);
                    } else {
                        scannedDevices.put(device, rssi);
                    }
                    Log.d("sandeep", "device name and rssi: " + device.getName() + "  " + rssi);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private ScanCallback mLatestScanCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActionBar().setTitle(R.string.title_devices);
        createImageMap();
        mToast = Toast.makeText(ScanningActivity.this, "",
                Toast.LENGTH_SHORT);
        mHandler = new Handler();
        mSdkVersion = Build.VERSION.SDK_INT;
        mDistance = "-1";
        if (!speedDetectionServiceRunning()) {
            Intent i = new Intent(ScanningActivity.this, SpeedDetectionService.class);
            i.setAction(SpeedDetectionService.NotificationConstants.START_SPEED_DETECTION_SERVICE);
            startService(i);
        }
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (mSdkVersion > Build.VERSION_CODES.KITKAT) {
            initializeLatestScanCallBack();
        }
        LocationManager mLocationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        Location locationNET = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (null != locationGPS) {
            mLatitude = locationGPS.getLatitude();
            mLongitude = locationGPS.getLongitude();
        } else if (null != locationNET) {
            mLatitude = locationNET.getLatitude();
            mLongitude = locationNET.getLongitude();
        }
        createDistanceDialog();
        //createImageWarningDialog();
        audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        tts = new TextToSpeech(ScanningActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                int curVolume = Integer.parseInt(s);
                LogUtils.log("Utterance started: "+curVolume);
                if (curVolume < (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)))

                {
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
                }
                LogUtils.log("after starting: " +audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }

            @Override
            public void onDone(String s) {
                LogUtils.log("after completion: " +audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
                int curVolume = Integer.parseInt(s);
                LogUtils.log("Utterance completed: "+curVolume);
                audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
            }

            @Override
            public void onError(String s) {

            }
        });


    }

    private void createImageMap() {
        imageIDs.put("trafficwarning.jpg", R.drawable.trafficwarning);
        imageIDs.put("workzone.jpg", R.drawable.workzone);
        imageIDs.put("image1.jpg", R.drawable.trafficwarning);
    }

    @TargetApi(21)
    private void initializeLatestScanCallBack() {
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mLatestScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d("sandeep", "new Device: " + result.toString() + " " + result.describeContents());
                final BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi();
                if (device == null)
                    return;
                if (rssi < (-1 * Settings.rssi_value))
                    return;
                if (null == device.getName() || !device.getName().startsWith("MTO"))
                    return;

                startNotificationToneAndVibrate(device, rssi);
                currentScannedList.put(device.getName(), new BluetoothDeviceObject(device, rssi));
                /*if (Settings.data_collection)
                    writeDatatoFile(device, rssi);*/
                if (scannedDevices.containsKey(device.getName())) {
                    if (scannedDevices.get(device) < rssi)
                        scannedDevices.put(device, rssi);
                } else {
                    scannedDevices.put(device, rssi);
                }
                Log.d("sandeep", "device name and rssi: " + device.getName() + "  " + rssi);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

    }

    public boolean speedDetectionServiceRunning() {

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((SpeedDetectionService.class).getName().equals(service.service.getClassName())) {
                Log.d("sandeep", "Running");
                return true;
            }
        }
        Log.d("sandeep", "Not Running");
        return false;

    }

    private void createDistanceDialog() {
        InputDistanceDialog dialog = new InputDistanceDialog();
        dialog.show(getFragmentManager(), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scanning, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_deletefile);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
       /* mToast.setText("Current speed:" + location.getSpeed());
        mToast.show();*/
        Log.d("sandeep", "" + location.getSpeed());
        if (!location.hasSpeed() || location.getSpeed() == 0) {
            Location locationNET = ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (null != locationNET)
                mSpeed = locationNET.getSpeed();
        } else
            mSpeed = location.getSpeed();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mScanning) {
            menu.findItem(R.id.menu_refresh).getActionView().setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    deleteCSVfile();
                }
            });
        } else {

            menu.findItem(R.id.menu_refresh).getActionView().setOnClickListener(null);

        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.enter_distance:
                scanLeDevice(false);
                createDistanceDialog();
                break;
           /* case R.id.enable_calls_driving:
                stopService(new Intent(ScanningActivity.this, SpeedDetectionService.class));
                break;
            case R.id.disable_calls_driving:
                startService(new Intent(ScanningActivity.this, SpeedDetectionService.class));
                break;*/
            case R.id.settings:
                createSettingsDialog();
                break;
            case R.id.test:
                testTTS();
                testGeoPositions();
                break;
        }
        return true;
    }

    @TargetApi(21)
    private void testTTS() {
        String message = "Hello everyone, the quick brown fox jumps over the lazy dog.";
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "" + audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private void testGeoPositions() {

        LocationValidator validator = new LocationValidator();
        List<Location> list = new ArrayList<Location>();

        Location location = new Location("ManualTest");
        location.setLatitude(41.893368);
        location.setLongitude(-87.622709);

        Location location1 = new Location("ManualTest");
        location1.setLatitude(41.893447);
        location1.setLongitude(-87.617795);

        Location location2 = new Location("ManualTest");
        location2.setLatitude(41.891075);
        location2.setLongitude(-87.617709);

        Location location3 = new Location("ManualTest");
        location3.setLatitude(41.890956);
        location3.setLongitude(-87.622644);

        Location test1 = new Location("ManualTest");
        test1.setLatitude(41.892689);
        test1.setLongitude(-87.614630);

        Location test2 = new Location("ManualTest");
        test2.setLatitude(41.892625);
        test2.setLongitude(-87.620263);

        list.add(location);
        list.add(location1);
        list.add(location2);
        list.add(location3);

        mToast.setText("Result: " + test1.getLatitude() + " " + test1.getLongitude() + validator.isPointInPolygon(list, test1));
        mToast.show();
        //mToast.setText("Result: " +test2.getLatitude() +" " +test2.getLongitude() + validator.isPointInPolygon(list, test2));
        //mToast.show();
    }

    private void createSettingsDialog() {
        SettingDialogFragment dialog = new SettingDialogFragment();
        dialog.show(getFragmentManager(), "settings");
    }

    private void createImageWarningDialogForOlderDevices(BluetoothDevice device, int rssi) {
        if (!Settings.display_alert || tts.isSpeaking())
            return;
        String message = TO_SPEAK;
        int drawableId = Integer.MIN_VALUE;
        BLETag tag = queryDataBase(device);
        if (tag != null) {
            Log.d("sandeep", "tag from db: " + tag.toString());
            String imageID = tag.getFileName();
            if (tag.getMessage() != null)
                message = tag.getMessage();
            if (imageID != null)
                drawableId = imageIDs.get(imageID);
        }
        if (Settings.display_alert && drawableId > Integer.MIN_VALUE) {
            ImageNotificationDialogFragment dialog = new ImageNotificationDialogFragment();
            Bundle args = new Bundle();
            args.putInt("drawable", drawableId);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), "image");
        }
        if (!tts.isSpeaking()) {
            speak(tts, message);
        }
    }

    private void speak(TextToSpeech tts, String message) {
        int mode = audiomanager.getRingerMode();
        int curVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (curVolume < (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))) {
            audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
        audiomanager.setRingerMode(mode);
    }

    private BLETag queryDataBase(BluetoothDevice device) {
        DBSQLiteHelper db = new DBSQLiteHelper(_instance);
        if (db == null)
            return null;
        return db.getBleTag(device.getAddress());
    }

    @TargetApi(21)
    private void createImageWarningDialog(BluetoothDevice device, int rssi) {
        if (!Settings.display_alert || tts.isSpeaking())
            return;
        String message = TO_SPEAK;
        int drawableId = Integer.MIN_VALUE;
        BLETag tag = queryDataBase(device);
        if (tag != null) {
            Log.d("sandeep", "tag from db: " + tag.toString());
            String imageID = tag.getFileName();
            if (tag.getMessage() != null)
                message = tag.getMessage();
            if (imageID != null) {
                if (imageIDs.containsKey(imageID))
                    drawableId = imageIDs.get(imageID);
                else
                    drawableId = Integer.MIN_VALUE;
            }
        }
        if (Settings.display_alert && drawableId > Integer.MIN_VALUE) {
            ImageNotificationDialogFragment dialog = new ImageNotificationDialogFragment();
            Bundle args = new Bundle();
            args.putInt("drawable", drawableId);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), "image");
        }

        if (!tts.isSpeaking()) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, ""+audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    private void deleteCSVfile() {
        DeleteDialogFragment delete = new DeleteDialogFragment();
        delete.show(getFragmentManager(), null);

    }

    protected static void deletePreviousData() {

        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + DIR_NAME);
        Log.d("sandeep1", "" + dir);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        _instance = this;
        Log.d("sandeep", isExternalStorageReadable() + " " + isExternalStorageWritable());
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        //scanLeDevice(true);
        MyApplication.onResumeCalled();
        storeSharedSettings();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        _instance = null;
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mScanning = false;
        MyApplication.onPauseCalled();
    }

    public static ScanningActivity getInstance() {
        return _instance;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {


        Toast.makeText(getApplicationContext(), "CLicking of Item disabled", Toast.LENGTH_SHORT).show();
    }

    private void scanLeDevice(final boolean enable) {
        //deletePreviousData();
        if (enable) {

            scannedDevices.clear();
            currentScannedList.clear();
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanningforDevices();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (mSdkVersion > Build.VERSION_CODES.KITKAT) {
                StartScanForLatestAndroid();
            } else
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (Settings.data_collection)
                startWritingService();
        } else {
            stopScanningforDevices();
        }
        invalidateOptionsMenu();
    }

    private void stopExecutorService() {
        if (mExecutor != null)
            mExecutor.shutdownNow();
        currentScannedList.clear();
    }

    private void startWritingService() {
        mExecutor = Executors.newScheduledThreadPool(1);
        mExecutor.scheduleAtFixedRate(mRunnable, 200, 200, TimeUnit.MILLISECONDS);
    }

    @TargetApi(21)
    private void StartScanForLatestAndroid() {
        if (null != mScanner) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            mScanner.startScan(filters, settings, mLatestScanCallback);
        }
    }

    @TargetApi(21)
    private void StopScanForLatestAndroid() {
        if (null != mScanner) {
            mScanner.stopScan(mLatestScanCallback);
        }
    }

    protected void openTheBestSignalDevice() {
        Entry<BluetoothDevice, Integer> maxEntry = null;
        for (Entry<BluetoothDevice, Integer> entry : scannedDevices.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue())
                maxEntry = entry;
        }

        if (maxEntry == null) return;

        final Intent intent = new Intent(this, ScanningActivity.class);
        intent.putExtra(ScanningActivity.EXTRAS_DEVICE_NAME, maxEntry.getKey().getName());
        intent.putExtra(ScanningActivity.EXTRAS_DEVICE_ADDRESS, maxEntry.getKey().getAddress());
        if (mScanning) {
            stopScanningforDevices();
        }
        startActivity(intent);

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    protected void startNotificationToneAndVibrate(final BluetoothDevice device, final int rssi) {
        Thread vibrate = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d("sandeep", "vibrator called " + device.getName() + " " + rssi);
                if (scannedDevices.containsKey(device))
                    return;
                if (Settings.vibration) {
                    mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    mVibrator.vibrate(2000);
                }
                if (Settings.alarm) {
                    mBeep = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                    mBeep.startTone(ToneGenerator.TONE_DTMF_0, 2000);
                }
                //beep.release();
            }
        });
        vibrate.start();
        Fragment prev = getFragmentManager().findFragmentByTag("image");
        if (prev == null) {

            if (mSdkVersion > Build.VERSION_CODES.KITKAT)
                createImageWarningDialog(device, rssi);
            else
                createImageWarningDialogForOlderDevices(device, rssi);
        }
    }

    protected void writeDatatoFile(BluetoothDevice device, int rssi) {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DIR_NAME + File.separator);
        File temp = new File(f.getAbsolutePath() + File.separator + "data.csv");
        Log.d("sandeep1", f.getAbsolutePath() + " " + temp.getAbsolutePath());
        Log.d("sandeep1", "" + (f.exists()) + " " + temp.exists());
        CSVWriter writer = null;
        if (!temp.exists()) {
            if (!f.exists())
                f.mkdir();
            temp = new File(f, "data.csv");
            try {
                writer = new CSVWriter(new FileWriter(temp, true));
                String[] entries = "Time#Device Name#Device ID#Device Rssi#Input observation note#Speed#Latitude#Longitude".split("#"); // array of your values
                writer.writeNext(entries);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            writer = new CSVWriter(new FileWriter(temp, true));
            Calendar c = Calendar.getInstance();
            String name = device.getName();
            String address = device.getAddress();
            if (name.equals(""))
                name = "Unknown";
            String[] entries = {
                    (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH)
                            + "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "." + c.get(Calendar.MILLISECOND) + " " + (c.get(Calendar.AM_PM) == 0 ? "AM" : "PM"),
                    name,
                    address,
                    Integer.toString(rssi),
                    mDistance,
                    "" + SpeedDetectionService.mSpeed,
                    "" + mLatitude,
                    "" + mLongitude
            };
            writer.writeNext(entries);
            writer.close();
        } catch (IOException e) {
            //error
        }


        Log.d("sandeep1", "" + f.exists());
    }

    /*Stop Scanning for Bluetooth Devices*/
    public void stopScanningforDevices() {
        mScanning = false;
        if (mSdkVersion > Build.VERSION_CODES.KITKAT)
            StopScanForLatestAndroid();
        else
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        invalidateOptionsMenu();
        //openTheBestSignalDevice();
        stopExecutorService();
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mVibrator != null)
            mVibrator.cancel();
        if (mBeep != null)
            mBeep.release();
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public static class DeleteDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("This will delete all your data stored till now. Continue?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            deletePreviousData();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }


    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanningActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);


            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    public static class InputDistanceDialog extends DialogFragment {
        EditText input;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            input = new EditText(getActivity());
            input.setPadding(20, 10, 20, 10);
            builder.setTitle(getResources().getString(R.string.distance_dialog_title));
            input.setHint(getResources().getString(R.string.distance_default_text));
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    mDistance = input.getText().toString();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    dismiss();
                }
            });
            return builder.create();
        }
    }
}
