package com.velorexe.unityandroidble.searching;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import com.velorexe.unityandroidble.BleMessage;
import com.velorexe.unityandroidble.UnityAndroidBLE;

public class LeScanCallback extends ScanCallback {

    private LeDeviceListAdapter mDeviceListAdapter;
    private UnityAndroidBLE mUnityAndroidBle;
    private String mCallbackId;

    public LeScanCallback(LeDeviceListAdapter adapter, UnityAndroidBLE manager) {
        mDeviceListAdapter = adapter;
        mUnityAndroidBle = manager;
    }

    public void setCallbackId(String callbackId) {
        mCallbackId = callbackId;
    }

    @SuppressLint("MissingPermission") // UnityAndroidBLE can't be created without the proper Permissions
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        BluetoothDevice device = result.getDevice();

        if(mDeviceListAdapter.AddDevice(device)) {
            BleMessage message = new BleMessage(mCallbackId, "deviceFound");
            message.device = device.getAddress();

            if(device.getName() != null) {
                message.name = device.getName();
            }

            mUnityAndroidBle.sendTaskResponse(message);
        }
    }
}
