package com.example.myapplication;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String MQTT_BROKER = "tcp://192.168.1.25:1883";  // Indirizzo del broker MQTT
    private static final String MQTT_TOPIC = "topic/allarme";  // Il topic a cui ci si sottoscrive
    private TextToSpeech textToSpeech;

    private MqttClient mqttClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);


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
                    speak("MISURA ARRIVATA!");
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

    public void sub(View view) {
        try {
            mqttClient.subscribe(MQTT_TOPIC, 0);
            Log.d(TAG, "Sottoscritto al topic: " + MQTT_TOPIC);
        } catch (Exception e) {

        }

    }

    public void unsub(View view) {
        try {
            mqttClient.unsubscribe(MQTT_TOPIC);
            Log.d(TAG, "Unsottoscritto al topic: " + MQTT_TOPIC);
        } catch (Exception e) {

        }
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
}
