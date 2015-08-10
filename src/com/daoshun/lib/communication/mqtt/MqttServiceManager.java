package com.daoshun.lib.communication.mqtt;

import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * This class is to manage the notificatin service and to load the configuration.
 */
public final class MqttServiceManager {

    private static final String TAG = MqttServiceManager.class.getName();

    private Context context;

    private SharedPreferences sharedPrefs;

    private Properties props;

    private String mqttHost;

    private String mqttPort;

    public MqttServiceManager(Context context, String pushReceivedAction) {
        this.context = context;

        props = loadProperties();
        mqttHost = props.getProperty("mqttHost", "127.0.0.1");
        mqttPort = props.getProperty("mqttPort", "1883");

        sharedPrefs =
                context.getSharedPreferences(MqttConstants.SHARED_PREFERENCE_NAME,
                        Context.MODE_PRIVATE);
        Editor editor = sharedPrefs.edit();
        editor.putString(MqttConstants.MQTT_HOST, mqttHost);
        editor.putInt(MqttConstants.MQTT_PORT, Integer.parseInt(mqttPort));
        editor.putString(MqttConstants.MQTT_RECEIVED_ACTION, pushReceivedAction);
        editor.commit();
    }

    public void startService() {
        Thread serviceThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(context, MqttService.class);
                context.startService(intent);
            }
        });
        serviceThread.start();
    }

    public void stopService() {
        Intent intent = new Intent(context, MqttService.class);
        context.stopService(intent);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try {
            int id = context.getResources().getIdentifier("push", "raw", context.getPackageName());
            props.load(context.getResources().openRawResource(id));
        } catch (Exception e) {
            Log.e(TAG, "Could not find the properties file.", e);
        }
        return props;
    }
}
