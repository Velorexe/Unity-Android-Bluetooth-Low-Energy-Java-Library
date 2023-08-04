package com.velorexe.unityandroidble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.unity3d.player.UnityPlayer;
import com.velorexe.unityandroidble.connection.ConnectionRunnable;
import com.velorexe.unityandroidble.connection.ConnectionService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnityAndroidBLE {

    private static UnityAndroidBLE mInstance = null;

    //MonoBehaviour GameObject to catch the BLE Messages
    private static final String mUnityBLEReceiver = "BleAdapter";

    //Command to send Bluetooth Low Energy data
    private static final String mUnityBLECommand = "OnBleMessage";
    //Command to send Unity Debug Logs
    private static final String mUnityLogCommand = "LogMessage";

    private static BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothLeScanner mBluetoothLeScanner = null;

    public static LeDeviceListAdapter mLeDeviceListAdapter = null;
    private static Map<BluetoothDevice, BluetoothGatt> mLeGattServers = null;

    private static Map<BluetoothDevice, ConnectionService> mConnectedServers = null;

    public static boolean mScanning = false;
    private Handler handler = new Handler();

    private static Context mContext;

    /**
     * Gets called by Unity to Initialize the UnityAndroidBLE library
     *
     * @return UnityAndroidBLE manager
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static UnityAndroidBLE getInstance() {
        if (mInstance == null) {
            mInstance = new UnityAndroidBLE();
        }
        //Reset in case that it already exists
        else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.enable();

            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            mLeDeviceListAdapter = new LeDeviceListAdapter();

            mLeGattServers = new HashMap<BluetoothDevice, BluetoothGatt>();
            mConnectedServers = new HashMap<BluetoothDevice, ConnectionService>();
        }

        mContext = UnityPlayer.currentActivity.getApplicationContext();

        //Checks to see if the device features Bluetooth Low Energy
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BleObject obj = new BleObject("Initialized");

            obj.setError("Device doesn't support Bluetooth Low Energy");

            sendToUnity(obj);
        }

        //Enable Bluetooth if it isn't enabled
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();

        sendToUnity(new BleObject("Initialized"));

        return mInstance;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void checkPermissions(Context context, Activity activity) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    public static void deInitialize() {
        //Close all the connected Gatt Servers
        for (Map.Entry<BluetoothDevice, BluetoothGatt> set : mLeGattServers.entrySet()) {
            set.getValue().close();
        }

        mLeGattServers = null;
    }

    /**
     * Constructor for UnityAndroidBLE
     * Enables Bluetooth and creates instances for properties
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public UnityAndroidBLE() {
        checkPermissions(UnityPlayer.currentActivity.getApplicationContext(), UnityPlayer.currentActivity);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mLeDeviceListAdapter = new LeDeviceListAdapter();

        mLeGattServers = new HashMap<BluetoothDevice, BluetoothGatt>();
        mConnectedServers = new HashMap<BluetoothDevice, ConnectionService>();
    }

    //region Scanning

    /**
     * Scans for Bluetooth Low Energy devices nearby
     *
     * @param scanPeriod the period of time the device scans for
     */
    public void scanBleDevices(int scanPeriod) {
        if (!mScanning) {
            this.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(bleScanCallback);

                    sendToUnity(new BleObject("FinishedDiscovering"));
                }
            }, scanPeriod);

            mScanning = true;
            mBluetoothLeScanner.startScan(bleScanCallback);

            unityLog("Starting Scan");

            return;
        } else {
            unityLog("BLE Manager is already scanning.");
        }
    }

    /**
     * Gets called when a new Bluetooth Low Energy device is discovered.
     * Sends a message to Unity containing the DiscoveredDevice command,
     * the device Address and a Name if the device contains one
     */
    private ScanCallback bleScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice device = result.getDevice();
                    if (mLeDeviceListAdapter.AddDevice(device)) {
                        BleObject obj = new BleObject("DiscoveredDevice");
                        obj.device = device.getAddress();

                        if (device.getName() != null) {
                            obj.name = device.getName();
                        }

                        sendToUnity(obj);
                    }
                }
            };

    public void stopScanBleDevices() {
        if (mScanning) {
            mBluetoothLeScanner.stopScan(bleScanCallback);
            mScanning = false;
        }
    }
    //endregion

    //region Connecting

    /**
     * Connects to a Bluetooth device with the given UUID
     *
     * @param deviceUuid the UUID of the device that the BluetoothAdapter should connect to
     */
    public void connectToDevice(final String deviceUuid) {
        BluetoothDevice device = mLeDeviceListAdapter.getItem(deviceUuid);
        BleObject obj = new BleObject("StartConnection");

        if (device != null && !mConnectedServers.containsKey(device)) {
            obj.device = device.getAddress();

            sendToUnity(obj);

            ConnectionService service = new ConnectionService(this);
            device.connectGatt(UnityPlayer.currentActivity.getApplicationContext(), true, service.gattCallback);

            mConnectedServers.put(device, service);
        } else {
            obj.setError("BluetoothDevice hasn't been discovered yet");
            sendToUnity(obj);
        }
    }


    /**
     * Passes through to Unity that the device is connected to the Gatt Server
     *
     * @param gattServer the GattServer that the device is connected to
     */
    public void connectedToGattServer(BluetoothGatt gattServer) {
        if (!mLeGattServers.containsKey(gattServer.getDevice())) {
            mLeGattServers.put(gattServer.getDevice(), gattServer);
        }

        BleObject obj = new BleObject("ConnectedToGattServer");
        obj.device = gattServer.getDevice().getAddress();

        sendToUnity(obj);
    }

    public void disconnectDevice(String deviceAddress) {
        BluetoothDevice device = mLeDeviceListAdapter.getItem(deviceAddress);
        BluetoothGatt gatt = mLeGattServers.get(device);

        BleObject obj = new BleObject("DisconnectedFromGattServer");
        obj.device = device.getAddress();

        if (gatt != null) {
            gatt.close();
            gatt.disconnect();

            mConnectedServers.remove(mLeDeviceListAdapter.getItem(deviceAddress));
            mLeGattServers.remove(device);
        }
        else {
            obj.setError("Can't find connected device with address " + deviceAddress);
        }

        sendToUnity(obj);
    }

    /**
     * Passes through to Unity that the device is disconnected from the Gatt Server
     *
     * @param gattServer the GattServer that the device lost connection to
     */
    public void disconnectedFromGattServer(BluetoothGatt gattServer) {
        disconnectDevice(gattServer.getDevice().getAddress());
    }
    //endregion

    //region Discovering

    /**
     * Passes the found Services and Characteristics to Unity from the given Gatt
     *
     * @param gatt the Gatt device which the services have been found from
     */
    public void discoveredService(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();

        for (int i = 0; i < services.size(); i++) {
            BleObject obj = new BleObject("DiscoveredService");

            obj.device = gatt.getDevice().getAddress();
            obj.service = services.get(i).getUuid().toString();

            sendToUnity(obj);

            List<BluetoothGattCharacteristic> characteristics = services.get(i).getCharacteristics();
            for (int j = 0; j < characteristics.size(); j++) {
                obj.command = "DiscoveredCharacteristic";
                obj.characteristic = characteristics.get(j).getUuid().toString();

                sendToUnity(obj);
            }
        }

        BleObject obj = new BleObject("DeviceConnected");
        obj.device = gatt.getDevice().getAddress();

        sendToUnity(obj);
    }
    //endregion

    //region Reading

    /**
     * Subscribes to a given Characteristic
     *
     * @param device         the device MAC Address
     * @param service        the UUID of the service under which the Characteristic is specified
     * @param characteristic the UUID of the Characteristic to subscribe to
     */
    public void subscribeToGattCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString("0000" + service + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString("0000" + characteristic + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptorUUID);

        gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattDescriptor.setValue(new byte[]{0x01, 0x00});
        gattServer.writeDescriptor(gattDescriptor);

        BleObject obj = new BleObject("StartedSubscribingToCharacteristic");

        if (gattServer.setCharacteristicNotification(gattCharacteristic, true)) {
            obj.device = device;
            obj.service = service;
            obj.characteristic = characteristic;
        } else {
            obj.setError("Couldn't connect to the specified characteristic " + characteristic);
        }

        sendToUnity(obj);
    }

    public void unsubscribeFromGattCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString("0000" + service + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString("0000" + characteristic + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptorUUID);

        gattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattDescriptor.setValue(new byte[]{0x00, 0x00});
        gattServer.writeDescriptor(gattDescriptor);

        BleObject obj = new BleObject("StartedUnsubscribingFromCharacteristic");

        if (gattServer.setCharacteristicNotification(gattCharacteristic, false)) {
            obj.device = device;
            obj.service = service;
            obj.characteristic = characteristic;

        } else {
            obj.setError("Couldn't connect to the specified characteristic " + characteristic);
        }

        sendToUnity(obj);
    }

    public void subscribeToCustomGattCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString(service);
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString(characteristic);
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptorUUID);

        gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattDescriptor.setValue(new byte[]{0x01, 0x00});
        gattServer.writeDescriptor(gattDescriptor);

        BleObject obj = new BleObject("StartedSubscribingToCharacteristic");

        if (gattServer.setCharacteristicNotification(gattCharacteristic, true)) {
            obj.device = device;
            obj.service = service;
            obj.characteristic = characteristic;
        } else {
            obj.setError("Couldn't connect to the specified characteristic " + characteristic);
        }

        sendToUnity(obj);
    }

    public void unsubscribeFromCustomGattCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString(service);
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString(characteristic);
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptorUUID);

        gattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattDescriptor.setValue(new byte[]{0x01, 0x00});
        gattServer.writeDescriptor(gattDescriptor);

        BleObject obj = new BleObject("StartedUnsubscribingFromCharacteristic");

        if (gattServer.setCharacteristicNotification(gattCharacteristic, false)) {
            obj.device = device;
            obj.service = service;
            obj.characteristic = characteristic;
        } else {
            obj.setError("Couldn't connect to the specified characteristic " + characteristic);
        }

        sendToUnity(obj);
    }

    /**
     * Passes the new value from the Characteristic to Unity
     *
     * @param gatt           the Gatt device from which the Characteristic value has changed
     * @param characteristic the Characteristic from which the value has changed
     */
    public void characteristicValueChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        BleObject obj = new BleObject("CharacteristicValueChanged");

        obj.device = gatt.getDevice().getAddress();
        obj.service = characteristic.getService().getUuid().toString();
        obj.characteristic = characteristic.getUuid().toString();

        androidLog(Arrays.toString(data));

        obj.base64Message = Base64.encodeToString(data, 0);

        sendToUnity(obj);
    }

    public void readFromCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString("0000" + service + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString("0000" + characteristic + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        gattServer.readCharacteristic(gattCharacteristic);
    }

    @SuppressLint("MissingPermission")
    public void readFromCustomCharacteristic(String device, String service, String characteristic) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString(service);
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString(characteristic);
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        gattServer.readCharacteristic(gattCharacteristic);
    }
    //endregion

    //region Writing
    @SuppressLint("MissingPermission")
    public void writeToGattCharacteristic(String device, String service, String characteristic, byte[] message) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString("0000" + service + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString("0000" + characteristic + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        gattCharacteristic.setValue(message);
        gattServer.writeCharacteristic(gattCharacteristic);
    }

    public void writeToGattCharacteristic(String device, String service, String characteristic, String message) {
        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString("0000" + service + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString("0000" + characteristic + "-0000-1000-8000-00805f9b34fb");
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        gattCharacteristic.setValue(message);
        gattServer.writeCharacteristic(gattCharacteristic);
    }

    public void writeToCustomGattCharacteristic(String device, String service, String characteristic, String message) {
        byte[] decodedBytes = Base64.decode(message, 0);
        androidLog(Arrays.toString(decodedBytes));

        BluetoothDevice bDevice = mLeDeviceListAdapter.getItem(device);
        BluetoothGatt gattServer = mLeGattServers.get(bDevice);

        UUID serviceUUID = UUID.fromString(service);
        BluetoothGattService gattService = gattServer.getService(serviceUUID);

        UUID gattUUID = UUID.fromString(characteristic);
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(gattUUID);

        gattCharacteristic.setValue(decodedBytes);
        gattServer.writeCharacteristic(gattCharacteristic);
    }
    //endregion

    //region Unity

    /**
     * Sends the given message to the Unity BLE Adapter
     * @param message the message to be send to Unity
     */
//    public static void sendToUnity(String message) {
//        if(IS_ANDROID) {
//            Log.i("UnityAndroidBLE", message);
//        } else {
//            UnityPlayer.UnitySendMessage(mUnityBLEReceiver, mUnityBLECommand, message);
//        }
//    }

    /**
     * Sends the given Byte array encoded to a string to Unity BLE Adapter
     * @param data the data which needs to be send
     * @param length the amount from the Byte array that needs to be send
     */
//    public static void sendToUnity(byte[] data, int length) {
//        UnityPlayer.UnitySendMessage(mUnityBLEReceiver, mUnityBLECommand, Base64.encodeToString(Arrays.copyOfRange(data, 0, length), 0));
//    }

    /**
     * Sends the given message to Unity BLE Adapter to log inside the Unity stack trace
     *
     * @param message the message to be logged
     */
    public static void unityLog(String message) {
        UnityPlayer.UnitySendMessage(mUnityBLEReceiver, mUnityLogCommand, message);
    }

    public static void sendToUnity(BleObject obj) {
        UnityPlayer.UnitySendMessage(mUnityBLEReceiver, mUnityBLECommand, obj.toJson());
    }

    /**
     * Logs a message using Log.i
     * This removes the clog of messages that Unity sends when using Debug.Log on Android
     *
     * @param message the message to log using Android log
     */
    public static void androidLog(String message) {
        Log.i("UnityAndroidBLE", message);
    }
    //endregion
}