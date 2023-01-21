package com.velorexe.unityandroidble.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.velorexe.unityandroidble.BleObject;
import com.velorexe.unityandroidble.UnityAndroidBLE;

public class ConnectionService {
    public static final String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private static final int STATE_CONNECTED = 2;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_DISCONNECTED = 0;

    private static final String TAG = ConnectionService.class.getSimpleName();

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;

    private String bluetoothDeviceAddress;

    public int connectionState = 0;
    private final UnityAndroidBLE mUnityAndroidBLE;

    public ConnectionService(UnityAndroidBLE bleManager) {
        mUnityAndroidBLE = bleManager;
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    public final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 2) {
                String intentAction = ACTION_GATT_CONNECTED;

                connectionState = 2;
                mUnityAndroidBLE.connectedToGattServer(gatt);

                gatt.discoverServices();
            } else if (newState == 0) {
                String intentAction = ACTION_GATT_DISCONNECTED;

                connectionState = 0;
                mUnityAndroidBLE.disconnectedFromGattServer(gatt);

                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mUnityAndroidBLE.discoveredService(gatt);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            mUnityAndroidBLE.characteristicValueChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();

            BleObject obj = new BleObject("ReadFromCharacteristic");

            obj.device = gatt.getDevice().getAddress();
            obj.service = characteristic.getService().getUuid().toString();
            obj.characteristic = characteristic.getUuid().toString();

            obj.base64Message = Base64.encodeToString(data, 0);

            UnityAndroidBLE.sendToUnity(obj);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };
}
