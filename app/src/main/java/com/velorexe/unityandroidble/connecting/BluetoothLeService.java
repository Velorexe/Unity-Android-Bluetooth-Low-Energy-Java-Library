package com.velorexe.unityandroidble.connecting;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.velorexe.unityandroidble.BleMessage;
import com.velorexe.unityandroidble.UnityAndroidBLE;
import com.velorexe.unityandroidble.connecting.data.ServiceDataMappers;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BluetoothLeService {

    private final UnityAndroidBLE mUnityAndroidBle;
    private final String mTaskId;

    public BluetoothGatt DeviceGatt;

    public BluetoothLeService(UnityAndroidBLE bleManager, String taskId) {
        mUnityAndroidBle = bleManager;
        mTaskId = taskId;
    }

    private Map<BluetoothGattCharacteristic, String> mCharTask = new HashMap<>();

    private String mMtuSizeTaskId = "";

    public void registerRead(BluetoothGattCharacteristic characteristic, String taskId) {
        if (mCharTask.containsKey(characteristic)) {
            BleMessage msg = new BleMessage(taskId, "readFromCharacteristic");
            msg.setError("You're queueing tasks too fast, wait until the previous read task is done.");

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        mCharTask.put(characteristic, taskId);
    }

    public void registerWrite(BluetoothGattCharacteristic characteristic, String taskId) {
        if (mCharTask.containsKey(characteristic)) {
            BleMessage msg = new BleMessage(taskId, "writeToCharacteristic");
            msg.setError("You're queueing tasks too fast, wait until the previous read task is done.");

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        mCharTask.put(characteristic, taskId);
    }

    public void registerSubscribe(BluetoothGattCharacteristic characteristic, String taskId) {
        if (mCharTask.containsKey(characteristic)) {
            BleMessage msg = new BleMessage(taskId, "subscribeToCharacteristic");
            msg.setError("You're queueing tasks too fast, wait until the previous read task is done.");

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        mCharTask.put(characteristic, taskId);
    }

    public void unregisterSubscribe(BluetoothGattCharacteristic characteristic) {
        if(mCharTask.containsKey(characteristic)) {
            BleMessage msg = createBleMessage(mCharTask.get(characteristic), "unsubscribeFromCharacteristic", DeviceGatt.getDevice());
            mUnityAndroidBle.sendTaskResponse(msg);

            mCharTask.remove(characteristic);
        }
    }

    public void registerMtuSizeTask(String taskId) {
        if(mMtuSizeTaskId.isEmpty()) {
            mMtuSizeTaskId = taskId;
        }
    }

    public void unRegisterMtuSizeTask() {
        mMtuSizeTaskId = "";
    }

    @SuppressLint("MissingPermission")
    // UnityAndroidBLE can't be created without the proper Permissions
    private BleMessage createBleMessage(String taskId, String command, BluetoothDevice device) {
        BleMessage msg = new BleMessage(taskId, command);
        msg.device = device.getAddress();
        msg.name = device.getName();
        return msg;
    }

    public final BluetoothGattCallback GattCallback = new BluetoothGattCallback() {

        @Override
        @SuppressLint("MissingPermission")
        // UnityAndroidBLE can't be created without the proper Permissions
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BleMessage msg = createBleMessage(mTaskId, "toBeDetermined", gatt.getDevice());

            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    msg.command = "disconnectedFromDevice";
                    gatt.close();
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    msg.command = "connectingToDevice";
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    msg.command = "connectedToDevice";
                    DeviceGatt = gatt;

                    new Handler(Looper.getMainLooper()).post(gatt::discoverServices);
                    break;
            }

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        @Override
        @SuppressLint("MissingPermission")
        // UnityAndroidBLE can't be created without the proper Permissions
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BleMessage msg = createBleMessage(mTaskId, "discoveredServicesAndCharacteristics", gatt.getDevice());

            if(status == BluetoothGatt.GATT_SUCCESS) {
                msg.jsonData = ServiceDataMappers.mapServicesToJson(gatt.getServices());

                mUnityAndroidBle.sendTaskResponse(msg);
            } else {
                msg.setError("Something went wrong while discovering all Services and Characteristics from the BLE device.");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mCharTask.containsKey(characteristic)) {
                BleMessage msg = createBleMessage(mCharTask.get(characteristic), "readFromCharacteristic", gatt.getDevice());
                msg.setService(characteristic.getService().getUuid().toString()).setCharacteristic(characteristic.getUuid().toString());

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    msg.base64Data = Base64.encodeToString(characteristic.getValue(), 0);
                    mUnityAndroidBle.sendTaskResponse(msg);

                    mCharTask.remove(characteristic);
                } else {
                    msg.setError("Something went wrong while reading the value from the Characteristic.");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mCharTask.containsKey(characteristic)) {
                BleMessage msg = createBleMessage(mCharTask.get(characteristic), "writeToCharacteristic", gatt.getDevice());
                msg.setService(characteristic.getService().getUuid().toString()).setCharacteristic(characteristic.getUuid().toString());

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mUnityAndroidBle.sendTaskResponse(msg);
                    mCharTask.remove(characteristic);
                } else {
                    msg.setError("Something went wrong while writing the value to the Characteristic.");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(mCharTask.containsKey(characteristic)) {
                BleMessage msg = createBleMessage(mCharTask.get(characteristic), "characteristicValueChanged", gatt.getDevice());
                msg.setService(characteristic.getService().getUuid().toString()).setCharacteristic(characteristic.getUuid().toString());

                msg.base64Data = Base64.encodeToString(characteristic.getValue(), 0);
                mUnityAndroidBle.sendTaskResponse(msg);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            BleMessage msg = createBleMessage(mMtuSizeTaskId, "requestMtuSize", DeviceGatt.getDevice());
            msg.base64Data = mtu + "";

            if(status == BluetoothGatt.GATT_SUCCESS) {
                mUnityAndroidBle.sendTaskResponse(msg);
            } else {
                msg.setError("Could not set the MTU size of the BLE device.");
                mUnityAndroidBle.sendTaskResponse(msg);
            }
        }
    };
}
