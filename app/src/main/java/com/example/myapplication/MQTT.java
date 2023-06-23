package com.example.myapplication;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.example.myapplication.MainActivity.TAG;

public class MQTT {

    public void setCallback(MqttClient mqttClient, TextToSpeech textToSpeech, Activity activity){
        // L'oggetto di callback implementa un'interfaccia specifica che definisce i metodi che verranno chiamati
        // in risposta a determinati eventi MQTT, come l'arrivo di un messaggio, la conferma della connessione o la disconnessione.
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connessione persa al broker MQTT");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // viene gestita la ricezione di un messaggio
                String payload = new String(message.getPayload());
                Log.d(TAG, "Messaggio ricevuto su topic: " + topic);
                Log.d(TAG, "Contenuto del messaggio: " + payload);

                updateTextView(payload, activity);
                speak(payload, textToSpeech);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }


    // metodo per leggere il messaggio arrivato
    public void speak(String text, TextToSpeech textToSpeech) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId");
    }

    // metodo per aggiornare la view col messaggio appena arrivato
    private void updateTextView(String message, Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView label = activity.findViewById(R.id.label_textview);
                label.setText(message);
            }
        });
    }


}
