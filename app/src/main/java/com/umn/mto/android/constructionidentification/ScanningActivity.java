package com.umn.mto.android.constructionidentification;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
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
import com.umn.mto.android.constructionidentification.settings.SettingDialogFragment;
import com.umn.mto.android.constructionidentification.settings.Settings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;

public class ScanningActivity extends ListActivity implements LocationListener {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;
    public static boolean mScanning;
    ToneGenerator mBeep;
    Vibrator mVibrator;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private float mSpeed = 0;
    private Handler mHandler;
    private HashMap<BluetoothDevice, Integer> scannedDevices = new HashMap<BluetoothDevice, Integer>();
    private static String mDistance;
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private Toast mToast;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if(null != device.getName() && !device.getName().startsWith("mto"))
                        return;
                    if (null == device.getName())
                        return;
                    startNotificationToneAndVibrate(device, rssi);
                    writeDatatoFile(device, rssi);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActionBar().setTitle(R.string.title_devices);
        mToast = Toast.makeText(ScanningActivity.this, "",
                Toast.LENGTH_SHORT);
        mHandler = new Handler();
        mDistance = "-1";
        if (!speedDetectionServiceRunning()) {
            startService(new Intent(ScanningActivity.this, SpeedDetectionService.class));
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
    }

    private boolean speedDetectionServiceRunning() {

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
        mToast.setText("Current speed:" + location.getSpeed());
        mToast.show();
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

        MenuItem enable_calls = menu.findItem(R.id.enable_calls_driving);
        MenuItem disable_calls = menu.findItem(R.id.disable_calls_driving);
        if (speedDetectionServiceRunning()) {
            enable_calls.setVisible(true);
            disable_calls.setVisible(false);
        } else {
            enable_calls.setVisible(false);
            disable_calls.setVisible(true);
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
            case R.id.enable_calls_driving:
                stopService(new Intent(ScanningActivity.this, SpeedDetectionService.class));
                break;
            case R.id.disable_calls_driving:
                startService(new Intent(ScanningActivity.this, SpeedDetectionService.class));
                break;
            case R.id.settings:
                createSettingsDialog();
        }
        return true;
    }

    private void createSettingsDialog(){
        SettingDialogFragment dialog = new SettingDialogFragment(this);
        dialog.show(getFragmentManager(), "dialog");
    }

    private void deleteCSVfile() {
        DeleteDialogFragment delete = new DeleteDialogFragment();
        delete.show(getFragmentManager(), null);

    }

    protected void deletePreviousData() {

        File dir = new File(Environment.getExternalStorageDirectory() + "/BLE");
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
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mScanning = false;
        MyApplication.onPauseCalled();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        /*final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		if (device == null) return;
		final Intent intent = new Intent(this, ScanningActivity.class);
		intent.putExtra(ScanningActivity.EXTRAS_DEVICE_NAME, device.getName());
		intent.putExtra(ScanningActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
		if (mScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanning = false;
		}
		scannedDevices.clear();
		startActivity(intent);*/

        Toast.makeText(getApplicationContext(), "CLicking of Item disabled", Toast.LENGTH_SHORT).show();
    }

    private void scanLeDevice(final boolean enable) {
        //deletePreviousData();
        if (enable) {

            scannedDevices.clear();
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    //openTheBestSignalDevice();

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
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
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
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
                if(Settings.vibration) {
                    mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    mVibrator.vibrate(2000);
                }
                if(Settings.alarm) {
                    mBeep = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                    mBeep.startTone(ToneGenerator.TONE_DTMF_0, 2000);
                }
                //beep.release();
            }
        });
        vibrate.start();


    }

    protected void writeDatatoFile(BluetoothDevice device, int rssi) {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MTO_BLE/");
        File temp = new File(f.getAbsolutePath() + "/data.csv");
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
                            + "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + " " + (c.get(Calendar.AM_PM) == 0 ? "AM" : "PM"),
                    name,
                    address,
                    Integer.toString(rssi),
                    mDistance,
                    "" + mSpeed,
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

    class DeleteDialogFragment extends DialogFragment {

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
            input.setPadding(20,10,20,10);
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
