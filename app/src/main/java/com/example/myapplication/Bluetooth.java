package com.example.myapplication;

import static com.example.myapplication.MainActivity.REQUEST_ACCESS_LOCATION;
import static com.example.myapplication.MainActivity.TAG;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Map;

public class Bluetooth {

    private static final String MQTT_TOPIC_SEGA = "allarme/sega/1";  // Il topic a cui ci si sottoscrive
    private static final String MQTT_TOPIC_TORNIO = "allarme/tornio/1";  // Il topic a cui ci si sottoscrive
    private int sottoscrittoSega = 0;
    private int sottoscrittoTornio = 0;


    public void startScanning(BluetoothLeScanner bluetoothLeScanner, ScanCallback scanCallback, Context context, Activity activity) {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //Questa modalità fornisce una maggiore velocità di scansione, ma potrebbe richiedere più risorse di energia
                .build();
        //viene usato l'accesso alla posizione precisa
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
        bluetoothLeScanner.startScan(null, settings, scanCallback);
    }

    public void stopScanning(BluetoothLeScanner bluetoothLeScanner, ScanCallback scanCallback, Context context, Activity activity) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
        bluetoothLeScanner.stopScan(scanCallback);
    }



    // una volta che viene rilevato un device bluetooth, viene preso il suo id per vedere se corrisponde a uno dei nostri
    // beacon e vengono gestite le sottoscrizioni
    public void manageTopics(BluetoothDevice device, Map<String, String> beaconMapping, int rssi, MqttClient mqttClient){
        if(device.getAddress().equals(beaconMapping.get("sega1"))) {
            Log.d("MainActivity", "Dispositivo rilevato: " + device.getAddress() + " con RSSI pari a: " + rssi);

            //ti sottoscrivi se stai vicino al macchinario Sega
            if(rssi > -60 ) {
                if(sottoscrittoSega==0) {
                    try {
                        mqttClient.subscribe(MQTT_TOPIC_SEGA, 0);
                        sottoscrittoSega = 1;

                        Log.d(TAG, "Sottoscritto al topic: " + MQTT_TOPIC_SEGA);
                    } catch (Exception e) {

                    }
                }
            }

            //cancelli la sottoscrizione se sei lontano dal macchinario sega
            else {
                if(sottoscrittoSega==1) {
                    try {
                        mqttClient.unsubscribe(MQTT_TOPIC_SEGA);
                        Log.d(TAG, "Unsottoscritto al topic: " + MQTT_TOPIC_SEGA);
                        sottoscrittoSega=0;
                    } catch (MqttException e) {

                    }
                }
            }

        }

        if(device.getAddress().equals(beaconMapping.get("tornio1"))) {
            Log.d("MainActivity", "Dispositivo rilevato: " + device.getAddress() + " con RSSI pari a: " + rssi);

            //ti sottoscrivi se stai vicino al macchinario tornio
            if(rssi > -60) {
                if(sottoscrittoTornio==0) {
                    try {
                        mqttClient.subscribe(MQTT_TOPIC_TORNIO, 0);
                        sottoscrittoTornio = 1;
                        Log.d(TAG, "Sottoscritto al topic: " + MQTT_TOPIC_TORNIO);
                    } catch (Exception e) {

                    }
                }
            }

            //cancelli la sottoscrizione se sei lontano dal macchinario sega
            else {
                if(sottoscrittoTornio==1) {
                    try {
                        mqttClient.unsubscribe(MQTT_TOPIC_TORNIO);
                        Log.d(TAG, "Unsottoscritto al topic: " + MQTT_TOPIC_TORNIO);
                        sottoscrittoTornio=0;
                    } catch (MqttException e) {

                    }
                }
            }

        }
    }


}
