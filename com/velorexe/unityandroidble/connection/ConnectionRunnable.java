package com.velorexe.unityandroidble.connection;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.velorexe.unityandroidble.UnityAndroidBLE;

public class ConnectionRunnable implements Runnable {
    private final UnityAndroidBLE mBleManager;
    private final BluetoothDevice mDevice;

    private final Context mApplicationContext;

    private final ConnectionService mService;

    public ConnectionRunnable(UnityAndroidBLE bleManager, BluetoothDevice device, Context applicationContext) {
        mBleManager = bleManager;
        mDevice = device;

        mApplicationContext = applicationContext;

        mService = new ConnectionService(mBleManager);
    }

    @Override
    public void run() {

    }
}
