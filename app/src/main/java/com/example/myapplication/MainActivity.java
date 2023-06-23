package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Locale;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String MQTT_BROKER = "tcp://192.168.1.25:1883";  // Indirizzo del broker MQTT
    private MqttClient mqttClient;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    public static final int REQUEST_ACCESS_LOCATION = 2;



    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private TextToSpeech textToSpeech;
    MQTT mqtt = new MQTT();
    Bluetooth bluetooth = new Bluetooth();
    Map<String, String> beaconMapping = new HashMap<>();


    @Override
    // in onCreate operazioni necessarie durante la creazione dell'interfaccia utente dell'attività
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanCallback = new SampleScanCallback();

        // mapping fra id del beacon e idMacchinario
        beaconMapping.put("sega1", "FE:BE:B3:37:16:CB");
        beaconMapping.put("tornio1", "C4:6D:B5:03:3C:FA");

        // Verifica se il dispositivo supporta il BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("MainActivity", "BLE non supportato.");
            finish();
        }

        // richiesta di permesso per l'accesso alla posizione approssimativa (coarse location) su dispositivi Android da 6.0 in su
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

        try {
            mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); //significa che il broker non manterrà traccia dello stato precedente del client, come i messaggi non consegnati.

            mqttClient.connect(options); //viene eseguita la connessione al broker con le opzioni specificate

            // L'oggetto di callback implementa un'interfaccia specifica che definisce i metodi che verranno chiamati
            // in risposta a determinati eventi MQTT, come l'arrivo di un messaggio, la conferma della connessione o la disconnessione.
            mqtt.setCallback(mqttClient, textToSpeech, this);
        } catch (MqttException e) {
            Log.e(TAG, "Errore durante la connessione al broker MQTT", e);
        }
    }

    @Override
    // chiamato quando si chiude l'attività manualmente, quando l'utente esce
    // dall'applicazione o quando il sistema decide di eliminare l'attività per liberare memoria
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
    // chiamato quando il TextToSpeech Engine è stato inizializzato
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault()); // imposta la lingua sulla lingua del dispositivo
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Lingua non supportato
                Toast.makeText(this, "Lingua non supportata", Toast.LENGTH_SHORT).show();
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
        }
            bluetooth.startScanning(bluetoothLeScanner, scanCallback, this, this);
        }



    private class SampleScanCallback extends ScanCallback {
        // chiamato quando viene rilevato un device bluetooth durante la scansione
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            bluetooth.manageTopics(device, beaconMapping, result.getRssi(), mqttClient);
        }

        // chiamato quando la scansione BLE fallisce
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
                bluetooth.startScanning(bluetoothLeScanner, scanCallback, this, this);
            } else {
                // Il permesso è stato negato, potresti mostrare un messaggio all'utente o disabilitare la funzionalità BLE
                Log.e("MainActivity", "Permesso Bluetooth negato.");
            }
        }
        if (requestCode == REQUEST_ACCESS_LOCATION) {
            // Verifica se l'utente ha concesso il permesso
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utente ha concesso il permesso, puoi avviare la scansione Bluetooth
                bluetooth.stopScanning(bluetoothLeScanner, scanCallback, this, this);
            } else {
                // L'utente non ha concesso il permesso, gestisci il caso di mancato permesso qui
                // Puoi mostrare un messaggio all'utente o disabilitare la funzionalità Bluetooth
            }
        }
    }
}
