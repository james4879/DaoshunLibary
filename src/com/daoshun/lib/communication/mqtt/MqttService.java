package com.daoshun.lib.communication.mqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

/* 
 * PushService that does all of the work.
 * Most of the logic is borrowed from KeepAliveService.
 * http://code.google.com/p/android-random/source/browse/trunk/TestKeepAlive/src/org/devtcg/demo/keepalive/KeepAliveService.java?r=219
 */
public class MqttService extends Service {

    // this is the log tag
    public static final String TAG = "DemoPushService";

    // the IP address, where your MQTT broker is running.
    private String mMqttHost;
    // the port at which the broker is running.
    private String mMqttPort;
    // Let's not use the MQTT persistence.
    private static MqttPersistence MQTT_PERSISTENCE = null;
    // We don't need to remember any state between the connections, so we use a clean start.
    private static boolean MQTT_CLEAN_START = true;
    // Let's set the internal keep alive for MQTT to 15 mins. I haven't tested this value much. It
    // could probably be increased.
    private static short MQTT_KEEP_ALIVE = 60 * 15;
    // Set quality of services to 0 (at most once delivery), since we don't want push notifications
    // arrive more than once. However, this means that some messages might get lost (delivery is not
    // guaranteed)
    private static int[] MQTT_QUALITIES_OF_SERVICE = { 0 };
    private static int MQTT_QUALITY_OF_SERVICE = 0;
    // The broker should not retain any messages.
    private static boolean MQTT_RETAINED_PUBLISH = false;

    // MQTT client ID, which is given the broker. In this example, I also use this for the topic
    // header.
    // You can use this to run push notifications for multiple apps with one MQTT broker.
    public static String MQTT_CLIENT_ID = "tokudu";

    // These are the actions for the service (name are descriptive enough)
    private static final String ACTION_KEEPALIVE = MQTT_CLIENT_ID + ".KEEP_ALIVE";
    private static final String ACTION_RECONNECT = MQTT_CLIENT_ID + ".RECONNECT";

    // Connectivity manager to determining, when the phone loses connection
    private ConnectivityManager mConnMan;

    // Whether or not the service has been started.
    private boolean mStarted;

    // This the application level keep-alive interval, that is used by the AlarmManager
    // to keep the connection active, even when the device goes to sleep.
    private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;

    // Retry intervals, when the connection is lost.
    private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
    private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;

    // Preferences instance
    private SharedPreferences mPrefs;

    // This is the instance of an MQTT connection.
    private MQTTConnection mConnection;
    private long mStartTime;

    @Override
    public void onCreate() {
        super.onCreate();

        log("Creating service");
        mStartTime = System.currentTimeMillis();

        // Get instances of preferences, connectivity manager and notification manager
        mPrefs = getSharedPreferences(MqttConstants.SHARED_PREFERENCE_NAME, MODE_PRIVATE);

        mMqttHost = mPrefs.getString(MqttConstants.MQTT_HOST, "127.0.0.1");
        mMqttPort = mPrefs.getString(MqttConstants.MQTT_PORT, "1883");

        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        /*
         * If our process was reaped by the system for any reason we need to restore our state with
         * merely a call to onCreate. We record the last "started" value and restore it here if
         * necessary.
         */
        handleCrashedService();
    }

    // This method does any necessary clean-up need in case the server has been destroyed by the
    // system
    // and then restarted
    private void handleCrashedService() {
        if (wasStarted() == true) {
            log("Handling crashed service...");
            // stop the keep alives
            stopKeepAlives();

            // Do a clean start
            start();
        }
    }

    @Override
    public void onDestroy() {
        log("Service destroyed (started=" + mStarted + ")");

        // Stop the services, if it has been started
        if (mStarted == true) {
            stop();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent.getAction().equals(ACTION_KEEPALIVE) == true) {
            keepAlive();
        } else if (intent.getAction().equals(ACTION_RECONNECT) == true) {
            if (isNetworkAvailable()) {
                reconnectIfNecessary();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // log helper function
    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Throwable e) {
        if (e != null) {
            Log.e(TAG, message, e);

        } else {
            Log.i(TAG, message);
        }
    }

    // Reads whether or not the service has been started from the preferences
    private boolean wasStarted() {
        return mPrefs.getBoolean(MqttConstants.MQTT_STARTED, false);
    }

    // Sets whether or not the services has been started in the preferences.
    private void setStarted(boolean started) {
        mPrefs.edit().putBoolean(MqttConstants.MQTT_STARTED, started).commit();
        mStarted = started;
    }

    private synchronized void start() {
        log("Starting service...");

        // Do nothing, if the service is already running.
        if (mStarted == true) {
            Log.w(TAG, "Attempt to start connection that is already active");
            return;
        }

        // Establish an MQTT connection
        connect();

        // Register a connectivity listener
        registerReceiver(mConnectivityChanged, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private synchronized void stop() {
        // Do nothing, if the service is not running.
        if (mStarted == false) {
            Log.w(TAG, "Attempt to stop connection not active.");
            return;
        }

        // Save stopped state in the preferences
        setStarted(false);

        // Remove the connectivity receiver
        unregisterReceiver(mConnectivityChanged);
        // Any existing reconnect timers should be removed, since we explicitly stopping the
        // service.
        cancelReconnect();

        // Destroy the MQTT connection if there is one
        if (mConnection != null) {
            mConnection.disconnect();
            mConnection = null;
        }
    }

    //
    private synchronized void connect() {
        log("Connecting...");
        // fetch the device ID from the preferences.
        String deviceID = mPrefs.getString(MqttConstants.MQTT_DEVICE_ID, null);
        // Create a new connection only if the device id is not NULL
        if (deviceID == null) {
            log("Device ID not found.");
        } else {
            try {
                mConnection = new MQTTConnection(mMqttHost, deviceID);
            } catch (MqttException e) {
                // Schedule a reconnect, if we failed to connect
                log("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"));
                if (isNetworkAvailable()) {
                    scheduleReconnect(mStartTime);
                }
            }
            setStarted(true);
        }
    }

    private synchronized void keepAlive() {
        try {
            // Send a keep alive, if there is a connection.
            if (mStarted == true && mConnection != null) {
                mConnection.sendKeepAlive();
            }
        } catch (MqttException e) {
            log("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"), e);

            mConnection.disconnect();
            mConnection = null;
            cancelReconnect();
        }
    }

    // Schedule application level keep-alives using the AlarmManager
    private void startKeepAlives() {
        Intent i = new Intent(this, MqttService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, pi);
    }

    // Remove all scheduled keep alives
    private void stopKeepAlives() {
        Intent i = new Intent(this, MqttService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    // We schedule a reconnect based on the starttime of the service
    public void scheduleReconnect(long startTime) {
        // the last keep-alive interval
        long interval = mPrefs.getLong(MqttConstants.MQTT_RETRY, INITIAL_RETRY_INTERVAL);

        // Calculate the elapsed time since the start
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        // Set an appropriate interval based on the elapsed time since start
        if (elapsed < interval) {
            interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
        } else {
            interval = INITIAL_RETRY_INTERVAL;
        }

        log("Rescheduling connection in " + interval + "ms.");

        // Save the new internval
        mPrefs.edit().putLong(MqttConstants.MQTT_RETRY, interval).commit();

        // Schedule a reconnect using the alarm manager.
        Intent i = new Intent(this, MqttService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
    }

    // Remove the scheduled reconnect
    public void cancelReconnect() {
        Intent i = new Intent();
        i.setClass(this, MqttService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    private synchronized void reconnectIfNecessary() {
        if (mStarted == true && mConnection == null) {
            log("Reconnecting...");
            connect();
        }
    }

    // This receiver listeners for network changes and updates the MQTT connection
    // accordingly
    private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get network info
            NetworkInfo info =
                    (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            // Is there connectivity?
            boolean hasConnectivity = (info != null && info.isConnected()) ? true : false;

            log("Connectivity changed: connected=" + hasConnectivity);

            if (hasConnectivity) {
                reconnectIfNecessary();
            } else if (mConnection != null) {
                // if there no connectivity, make sure MQTT connection is destroyed
                mConnection.disconnect();
                cancelReconnect();
                mConnection = null;
            }
        }
    };

    // Check if we are online
    private boolean isNetworkAvailable() {
        NetworkInfo info = mConnMan.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        return info.isConnected();
    }

    // This inner class is a wrapper on top of MQTT client.
    private class MQTTConnection implements MqttSimpleCallback {

        IMqttClient mqttClient = null;

        // Creates a new connection given the broker address and initial topic
        public MQTTConnection(String brokerHostName, String initTopic) throws MqttException {
            // Create connection spec
            String mqttConnSpec = "tcp://" + brokerHostName + "@" + mMqttPort;
            // Create the client and connect
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
            String clientID =
                    MQTT_CLIENT_ID + "/" + mPrefs.getString(MqttConstants.MQTT_DEVICE_ID, "");
            mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);

            // register this client app has being able to receive messages
            mqttClient.registerSimpleHandler(this);

            // Subscribe to an initial topic, which is combination of client ID and device ID.
            initTopic = MQTT_CLIENT_ID + "/" + initTopic;
            subscribeToTopic(initTopic);

            log("Connection established to " + brokerHostName + " on topic " + initTopic);

            // Save start time
            mStartTime = System.currentTimeMillis();
            // Star the keep-alives
            startKeepAlives();
        }

        // Disconnect
        public void disconnect() {
            try {
                stopKeepAlives();
                mqttClient.disconnect();
            } catch (MqttPersistenceException e) {
                log("MqttException" + (e.getMessage() != null ? e.getMessage() : " NULL"), e);
            }
        }

        /*
         * Send a request to the message broker to be sent messages published with the specified
         * topic name. Wildcards are allowed.
         */
        private void subscribeToTopic(String topicName) throws MqttException {

            if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
                // quick sanity check - don't try and subscribe if we don't have
                // a connection
                log("Connection error" + "No connection");
            } else {
                String[] topics = { topicName };
                mqttClient.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
            }
        }

        /*
         * Sends a message to the message broker, requesting that it be published to the specified
         * topic.
         */
        private void publishToTopic(String topicName, String message) throws MqttException {
            if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
                // quick sanity check - don't try and publish if we don't have
                // a connection
                log("No connection to public to");
            } else {
                mqttClient.publish(topicName, message.getBytes(), MQTT_QUALITY_OF_SERVICE,
                        MQTT_RETAINED_PUBLISH);
            }
        }

        /*
         * Called if the application loses it's connection to the message broker.
         */
        public void connectionLost() throws Exception {
            log("Loss of connection" + "connection downed");
            stopKeepAlives();
            // null itself
            mConnection = null;
            if (isNetworkAvailable() == true) {
                reconnectIfNecessary();
            }
        }

        /*
         * Called when we receive a message from the message broker.
         */
        public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) {
            // Show a notification
            String message = new String(payload);
            Intent intent =
                    new Intent(mPrefs.getString(MqttConstants.MQTT_RECEIVED_ACTION,
                            "com.totyu.lib.communication.mqtt.MQTT_RECEIVED_ACTION"));
            intent.putExtra(MqttConstants.NOTIFICATION_TOPIC, topicName);
            intent.putExtra(MqttConstants.NOTIFICATION_MESSAGE, message);

            sendBroadcast(intent);
            log("Got message: " + message);
        }

        public void sendKeepAlive() throws MqttException {
            log("Sending keep alive");
            // publish to a keep-alive topic
            publishToTopic(MQTT_CLIENT_ID + "/keepalive",
                    mPrefs.getString(MqttConstants.MQTT_DEVICE_ID, ""));
        }
    }
}