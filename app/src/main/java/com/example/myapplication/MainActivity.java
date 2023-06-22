package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Locale;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String MQTT_BROKER = "tcp://192.168.1.25:1883";  // Indirizzo del broker MQTT
    private static final String MQTT_TOPIC_SEGA = "topic/allarme/sega/1";  // Il topic a cui ci si sottoscrive
    private static final String MQTT_TOPIC_TORNIO = "topic/allarme/tornio/1";  // Il topic a cui ci si sottoscrive
    private TextToSpeech textToSpeech;
    private MqttClient mqttClient;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private static final int REQUEST_ACCESS_LOCATION = 2;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private int sottoscrittoSega = 0;
    private int sottoscrittoTornio = 0;

    Map<String, String> beaconMapping = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanCallback = new SampleScanCallback();
        beaconMapping.put("sega1", "FE:BE:B3:37:16:CB");
        beaconMapping.put("tornio1", "C4:6D:B5:03:3C:FA");

        // Verifica se il dispositivo supporta il BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("MainActivity", "BLE non supportato.");
            finish();
        }

        // Verifica i permessi necessari per la scansione BLE su Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        // Verifica se il dispositivo supporta il BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("MainActivity", "BLE non supportato.");
            finish();
        }

        // Verifica i permessi necessari per la scansione BLE su Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }


        try {
            mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Connessione persa al broker MQTT");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Messaggio ricevuto su topic: " + topic);
                    Log.d(TAG, "Contenuto del messaggio: " + payload);

                    updateTextView(payload);
                    speak(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Non è necessario implementare nulla in questo caso
                }
            });


        } catch (MqttException e) {
            Log.e(TAG, "Errore durante la connessione al broker MQTT", e);
        }
    }

    private void updateTextView(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView label = findViewById(R.id.label_textview);
                label.setText(message);
            }
        });
    }


    private void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();

            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                Log.e(TAG, "Errore durante la disconnessione dal broker MQTT", e);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Linguaggio non supportato
                Toast.makeText(this, "Linguaggio non supportato", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Errore durante l'inizializzazione
            Toast.makeText(this, "Errore durante l'inizializzazione", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verifica se il Bluetooth è abilitato sul dispositivo, altrimenti richiede l'abilitazione
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_ENABLE_BT);
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    private void startScanning() {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_LOCATION);
        }
        bluetoothLeScanner.startScan(null, settings, scanCallback);
    }

    private void stopScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_LOCATION);
        }
        bluetoothLeScanner.stopScan(scanCallback);
    }



    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();

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

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("MainActivity", "Scansione BLE fallita con codice di errore: " + errorCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Il permesso è stato concesso, puoi avviare la scansione BLE
                startScanning();
            } else {
                // Il permesso è stato negato, potresti mostrare un messaggio all'utente o disabilitare la funzionalità BLE
                Log.e("MainActivity", "Permesso Bluetooth negato.");
            }
        }
        if (requestCode == REQUEST_ACCESS_LOCATION) {
            // Verifica se l'utente ha concesso il permesso
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utente ha concesso il permesso, puoi avviare la scansione Bluetooth
                startScanning();
            } else {
                // L'utente non ha concesso il permesso, gestisci il caso di mancato permesso qui
                // Puoi mostrare un messaggio all'utente o disabilitare la funzionalità Bluetooth
            }
        }
    }


}
