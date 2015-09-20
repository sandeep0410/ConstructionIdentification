package com.umn.mto.android.constructionidentification.com.umn.mto.android.constructionidentification.dto;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Sandeep on 9/20/2015.
 */
public class BluetoothDeviceObject {
    public BluetoothDevice device;
    public int rssi;

    public BluetoothDeviceObject(BluetoothDevice device, int rssi){
        this.device = device;
        this.rssi = rssi;
    }

}
