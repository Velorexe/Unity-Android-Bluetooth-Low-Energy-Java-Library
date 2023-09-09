package com.velorexe.unityandroidble.searching;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class LeDeviceListAdapter {
    public Map<String, BluetoothDevice> mLeDevicesMap = new HashMap<String, BluetoothDevice>();

    public Map<BluetoothDevice, Short> mLeDeviceRssi = new HashMap<>();

    public boolean AddDevice(BluetoothDevice device) {
        if (this.mLeDevicesMap.get(device.getAddress()) != null) {
            return false;
        }

        this.mLeDevicesMap.put(device.getAddress(), device);
        return true;
    }

    public boolean setOrAdd(BluetoothDevice device, short rssi) {
        this.mLeDeviceRssi.put(device, rssi);
        return true;
    }

    public BluetoothDevice getItem(String i) {
        return this.mLeDevicesMap.get(i);
    }

    public short getRssi(BluetoothDevice device) {
        BluetoothDevice bleDevice = getItem(device.getAddress());
        if(bleDevice != null && mLeDeviceRssi.containsKey(bleDevice)) {
            return mLeDeviceRssi.get(bleDevice);
        }

        return Short.MIN_VALUE;
    }
}
