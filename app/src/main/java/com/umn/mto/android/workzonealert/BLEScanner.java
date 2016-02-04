package com.umn.mto.android.workzonealert;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.opencsv.CSVWriter;
import com.umn.mto.android.workzonealert.db.DBSQLiteHelper;
import com.umn.mto.android.workzonealert.dto.BLETag;
import com.umn.mto.android.workzonealert.dto.BluetoothDeviceObject;
import com.umn.mto.android.workzonealert.imagewarning.ImageWarningActivity;
import com.umn.mto.android.workzonealert.imagewarning.ImageWarningReceiver;
import com.umn.mto.android.workzonealert.settings.Settings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sandeep on 12/24/2015.
 */
public class BLEScanner {

    public static final String DIR_NAME = "MTO_BLE";
    private static final long SCAN_PERIOD = 100000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final Map<String, Integer> imageIDs = new HashMap<>();
    public static final String TO_SPEAK = "Data Not Found!";
    private static BLEScanner _instance;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private boolean scanning = false;
    private boolean bleSupported = true;
    private ScanCallback mLatestScanCallback;
    private Map<BluetoothDevice, Integer> scannedDevices = new HashMap<BluetoothDevice, Integer>();
    private volatile Map<String, BluetoothDeviceObject> currentScannedList = new HashMap<String, BluetoothDeviceObject>();
    private int mSdkVersion;
    private ToneGenerator mBeep;
    private Vibrator mVibrator;
    private Context mContext;
    private TextToSpeech tts;
    private AudioManager audiomanager;
    private Handler mHandler;
    private ScheduledExecutorService mExecutor;
    private static String mDistance;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            // LogUtils.log("writing.. " + currentScannedList.size());
            for (Map.Entry<String, BluetoothDeviceObject> bluetoothobject : currentScannedList.entrySet()) {
                writeDatatoFile(bluetoothobject.getValue().device, bluetoothobject.getValue().rssi);
            }
        }
    };

    public static BLEScanner getInstance(Context context) {
        if(_instance==null)
            _instance = new BLEScanner(context);
        return _instance;
    }

    public BLEScanner(Context context) {
        mContext = context;
        mSdkVersion = Build.VERSION.SDK_INT;
        mHandler = new Handler();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleSupported = false;
            return;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            bleSupported = false;
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
        if (mSdkVersion > Build.VERSION_CODES.KITKAT)
            initializeLatestScanCallBack();


        createImageMap();
        initializeAudioItems();
        _instance = this;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mBeep = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
    }

    private void createImageMap() {
        imageIDs.put("trafficwarning.jpg", R.drawable.trafficwarning);
        imageIDs.put("workzone.jpg", R.drawable.workzone);
        imageIDs.put("image1.jpg", R.drawable.trafficwarning);
    }

    private void initializeAudioItems() {
        audiomanager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        tts = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                int curVolume = Integer.parseInt(s);
                LogUtils.log("Utterance started: " + curVolume);
                if (curVolume < (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)))

                {
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (.5 * audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
                }
                LogUtils.log("after starting: " + audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }

            @Override
            public void onDone(String s) {
                LogUtils.log("after completion: " + audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
                int curVolume = Integer.parseInt(s);
                LogUtils.log("Utterance completed: " + curVolume);
                audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
            }

            @Override
            public void onError(String s) {

            }
        });
    }

    public boolean isScanning() {
        return scanning;
    }

    public boolean isBLESupported() {
        return bleSupported;
    }

    @TargetApi(21)
    private void initializeLatestScanCallBack() {
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mLatestScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // LogUtils.log("new Device: " + result.toString() + " " + result.describeContents());
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
                // LogUtils.log("device name and rssi: " + device.getName() + "  " + rssi);
            }
        };

    }

    protected void startNotificationToneAndVibrate(final BluetoothDevice device, final int rssi) {
        if (ImageWarningActivity.getInstance() == null) {
            Thread vibrate = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        LogUtils.log("vibrator called " + device.getName() + " " + rssi);
                        if (scannedDevices.containsKey(device))
                            return;
                        if (Settings.vibration) {
                            mVibrator.vibrate(2000);
                        }
                        if (Settings.alarm) {
                            mBeep.startTone(ToneGenerator.TONE_DTMF_0, 2000);
                            Thread.sleep(2000);
                            mBeep.release();

                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //beep.release();
                }
            });
            vibrate.start();
            //ImageWarningActivity.getInstance();

            createImageWarningDialog(device, rssi);

        }
    }

    public void scanLeDevice(final boolean enable) {
        //deletePreviousData();
        if (enable) {

            scannedDevices.clear();
            currentScannedList.clear();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanningforDevices();
                }
            }, SCAN_PERIOD);

            scanning = true;
            if (mSdkVersion > Build.VERSION_CODES.KITKAT)
                StartScanForLatestAndroid();

            if (Settings.data_collection)
                startWritingService();
        } else {
            stopScanningforDevices();
        }
    }

    @TargetApi(21)
    private void createImageWarningDialog(BluetoothDevice device, int rssi) {
        if (!Settings.display_alert || tts.isSpeaking())
            return;
        String message = TO_SPEAK;
        int drawableId = Integer.MIN_VALUE;
        BLETag tag = queryDataBase(device);
        if (tag != null) {
            LogUtils.log("tag from db: " + tag.toString());
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
            Intent i = new Intent();
            i.setAction(ImageWarningReceiver.RAISE_IMAGEWARNING);
            i.putExtra("drawable", drawableId);
            mContext.sendBroadcast(i);
        }

        if (!tts.isSpeaking()) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "" + audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    private BLETag queryDataBase(BluetoothDevice device) {
        DBSQLiteHelper db = DBSQLiteHelper.getInstance(mContext);
        if (db == null)
            return null;
        return db.getBleTag(device.getAddress());
    }


    @TargetApi(21)
    private void StartScanForLatestAndroid() {
        if (null != mScanner) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            mScanner.startScan(filters, settings, mLatestScanCallback);
            LogUtils.writeToFile("started");
        }
    }

    @TargetApi(21)
    private void StopScanForLatestAndroid() {
        if (null != mScanner) {
            mScanner.stopScan(mLatestScanCallback);
            LogUtils.writeToFile("stopped");
        }
    }

    /*Stop Scanning for Bluetooth Devices*/
    public void stopScanningforDevices() {
        scanning = false;
        if (mSdkVersion > Build.VERSION_CODES.KITKAT)
            StopScanForLatestAndroid();
        stopExecutorService();
        Intent i = new Intent();
        i.setAction(ImageWarningReceiver.STOP_IMAGEWARNING);
        mContext.sendBroadcast(i);
        _instance = null;

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

    private void writeDatatoFile(BluetoothDevice device, int rssi) {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DIR_NAME + File.separator);
        File temp = new File(f.getAbsolutePath() + File.separator + "data.csv");
        LogUtils.log(f.getAbsolutePath() + " " + temp.getAbsolutePath());
        LogUtils.log("" + (f.exists()) + " " + temp.exists());
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
                    "" + SpeedDetectionService.mLatitude,
                    "" + SpeedDetectionService.mLongitude
            };
            writer.writeNext(entries);
            writer.close();
        } catch (IOException e) {
            //error
        }


        LogUtils.log("" + f.exists());
    }
}
