package com.velorexe.unityandroidble;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;
import java.util.Map;

public class LeDeviceListAdapter {
    public Map<String, BluetoothDevice> mLeDevicesMap = new HashMap<String, BluetoothDevice>();

    public boolean AddDevice(BluetoothDevice device) {
        if (this.mLeDevicesMap.get(device.getAddress()) != null) {
            return false;
        }

        this.mLeDevicesMap.put(device.getAddress(), device);
        return true;
    }

    public int getCount() {
        return this.mLeDevicesMap.size();
    }

    public BluetoothDevice getItem(String i) {
        return this.mLeDevicesMap.get(i);
    }
}
