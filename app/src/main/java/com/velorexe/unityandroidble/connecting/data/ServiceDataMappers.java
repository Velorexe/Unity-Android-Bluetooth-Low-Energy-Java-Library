package com.velorexe.unityandroidble.connecting.data;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ServiceDataMappers {

    public static String mapServicesToJson(List<BluetoothGattService> services) {
        JSONObject dataWrapper = new JSONObject();
        JSONArray serviceArray = new JSONArray();

        try {
            for (int i = 0; i < services.size(); i++) {
                JSONObject serviceObject =new JSONObject();

                serviceObject.put("uuid", services.get(i).getUuid().toString());

                List<BluetoothGattCharacteristic> characteristics = services.get(i).getCharacteristics();
                JSONArray characteristicArray = new JSONArray();

                for (int j = 0; j < characteristics.size(); j++) {
                    JSONObject characteristicObject = new JSONObject();
                    BluetoothGattCharacteristic characteristic = characteristics.get(j);

                    characteristicObject.put("uuid", characteristic.getUuid());

                    characteristicObject.put("permissions", characteristic.getPermissions());
                    characteristicObject.put("properties", characteristic.getProperties());

                    characteristicObject.put("writeTypes", characteristic.getWriteType());

                    characteristicArray.put(characteristicObject);
                }

                serviceObject.put("characteristics", characteristicArray);

                serviceArray.put(serviceObject);
                dataWrapper.put("data", serviceArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return dataWrapper.toString();
    }
}
