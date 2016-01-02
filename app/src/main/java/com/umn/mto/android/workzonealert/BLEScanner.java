package com.umn.mto.android.workzonealert;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Sandeep on 12/24/2015.
 */
public class BLEScanner {

    private static final int REQUEST_ENABLE_BT = 1;
    private static BLEScanner _instance;
    private BluetoothAdapter mBluetoothAdapter;

    public static BLEScanner getInstance(Context context){
        if(_instance==null)
            _instance = new BLEScanner(context);
        return _instance;
    }

    public BLEScanner(Context context){
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(enableBtIntent);
            }
        }
    }
}
