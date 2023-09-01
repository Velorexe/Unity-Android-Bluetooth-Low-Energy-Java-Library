package com.velorexe.unityandroidble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.unity3d.player.UnityPlayer;
import com.velorexe.unityandroidble.connecting.BluetoothLeService;
import com.velorexe.unityandroidble.searching.LeDeviceListAdapter;
import com.velorexe.unityandroidble.searching.LeScanCallback;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class UnityAndroidBLE {

    private final int SDK_INT = Build.VERSION.SDK_INT;

    private static UnityAndroidBLE mInstance = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothLeScanner mBluetoothLeScanner = null;

    private Context mContext = null;

    private boolean mIsScanning = false;
    private Handler mHandler = null;

    private final LeScanCallback mScanCallback;
    private LeDeviceListAdapter mDeviceListAdapter = new LeDeviceListAdapter();
    private Map<BluetoothDevice, BluetoothLeService> mConnectedServers = new HashMap<>();

    public UnityAndroidBLE() {
        mContext = UnityPlayer.currentActivity.getApplicationContext();

        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (SDK_INT <= Build.VERSION_CODES.S &&
                mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else {
            // Warn user that Bluetooth isn't enabled
        }

        // Setup for scanning BLE devices
        mHandler = new Handler(Looper.getMainLooper());
        mScanCallback = new LeScanCallback(mDeviceListAdapter, this);
    }

    public static UnityAndroidBLE getInstance() {
        if (mInstance == null) {
            Context ctx = UnityPlayer.currentActivity.getApplicationContext();

            PackageManager packageManager = ctx.getPackageManager();

            // Check if Device has Bluetooth and Bluetooth Low Energy features
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                return null;
            }

            int sdkInt = Build.VERSION.SDK_INT;
            Activity activity = UnityPlayer.currentActivity;

            if (sdkInt <= Build.VERSION_CODES.R) {
                if (!checkPermissionsAndroid11AndBelow(ctx, activity)) {
                    return null;
                }
            }
            // Assume that the version is above Android 11 (30)
            else {
                if (!checkPermissionsAndroid12AndUp(ctx, activity)) {
                    return null;
                }
            }

            mInstance = new UnityAndroidBLE();
        }

        return mInstance;
    }

    private static boolean checkPermissionsAndroid11AndBelow(Context ctx, Activity activity) {
        // Check if App has permissions for Bluetooth necessary features
        activity.requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        }, 1);

        return ctx.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                && ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static boolean checkPermissionsAndroid12AndUp(Context ctx, Activity activity) {
        activity.requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
        }, 1);

        return ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    // UnityAndroidBLE can't be created without the proper Permissions
    public void searchForBleDevices(String taskId, int scanPeriod) {
        if (!mIsScanning) {
            mScanCallback.setCallbackId(taskId);
            mHandler.postDelayed(() -> {
                mIsScanning = false;
                mBluetoothLeScanner.stopScan(mScanCallback);

                BleMessage message = new BleMessage(taskId, "searchStop");
                sendTaskResponse(message);
            }, scanPeriod);

            mIsScanning = true;
            mBluetoothLeScanner.startScan(mScanCallback);
        }
    }

    @SuppressLint("MissingPermission")
    // UnityAndroidBLE can't be created without the proper Permissions
    public void connectToBleDevice(String taskId, String macAddress, int transport) {
        BluetoothDevice device = mDeviceListAdapter.getItem(macAddress);

        if (device != null) {
            BluetoothLeService leService = new BluetoothLeService(this, taskId);
            device.connectGatt(mContext, false, leService.GattCallback, transport);

            mConnectedServers.put(device, leService);
        } else {
            BleMessage msg = new BleMessage(taskId, "connectToDevice");
            msg.setError("Can't connect to a BluetoothDevice that hasn't been discovered yet.");

            sendTaskResponse(msg);
        }
    }

    public void sendTaskResponse(BleMessage message) {
        if (!message.id.isEmpty()) {
            UnityPlayer.UnitySendMessage("BleMessageAdapter", "OnBleMessage", message.toJson());
        }
    }
}
