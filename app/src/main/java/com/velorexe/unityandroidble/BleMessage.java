package com.velorexe.unityandroidble;

import org.json.JSONException;
import org.json.JSONObject;

public class BleMessage {

    public String id;
    public String command;

    public String device;
    public String name;

    public String service;
    public String characteristic;

    public byte[] data;

    public String jsonData;

    public boolean hasError = false;
    public String errorMessage;

    public BleMessage(String id, String command) {
        this.id = id;
        this.command = command;
    }

    public void setError(String errorMessage) {
        hasError = true;
        this.errorMessage = errorMessage;
    }

    public BleMessage setService(String service) {
        this.service = service;
        return this;
    }

    public BleMessage setCharacteristic(String characteristic) {
        this.characteristic = characteristic;
        return this;
    }

    public String toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("id", id);
            obj.put("command", command);

            obj.put("device", device);
            obj.put("name", name);

            obj.put("service", service);
            obj.put("characteristic", characteristic);

            obj.put("data", data);
            obj.put("jsonData", jsonData);

            if(hasError) {
                obj.put("hasError", hasError);
                obj.put("errorMessage", errorMessage);
            }

            return obj.toString();
        }
        catch(JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }
}
