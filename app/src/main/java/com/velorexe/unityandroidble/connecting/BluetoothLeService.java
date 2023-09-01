package com.velorexe.unityandroidble.connecting;

import android.annotation.SuppressLint;
import android.app.Service;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.velorexe.unityandroidble.BleMessage;
import com.velorexe.unityandroidble.UnityAndroidBLE;
import com.velorexe.unityandroidble.connecting.data.ServiceDataMappers;

import org.json.JSONObject;

public class BluetoothLeService {

    private final UnityAndroidBLE mUnityAndroidBle;
    private final String mTaskId;

    public BluetoothLeService(UnityAndroidBLE bleManager, String taskId) {
        mUnityAndroidBle = bleManager;
        mTaskId = taskId;
    }

    public final BluetoothGattCallback GattCallback = new BluetoothGattCallback() {

        @Override
        @SuppressLint("MissingPermission")
        // UnityAndroidBLE can't be created without the proper Permissions
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BleMessage msg = new BleMessage(mTaskId, "toBeDetermined");

            msg.device = gatt.getDevice().getAddress();
            msg.name = gatt.getDevice().getName();

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
                    new Handler(Looper.getMainLooper()).post(gatt::discoverServices);
                    break;
            }

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        @Override
        @SuppressLint("MissingPermission")
        // UnityAndroidBLE can't be created without the proper Permissions
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BleMessage msg = new BleMessage(mTaskId, "discoveredServicesAndCharacteristics");

            msg.device = gatt.getDevice().getAddress();
            msg.name = gatt.getDevice().getName();

            msg.jsonData = ServiceDataMappers.mapServicesToJson(gatt.getServices());

            mUnityAndroidBle.sendTaskResponse(msg);
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };
}
