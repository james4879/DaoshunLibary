package com.daoshun.lib.communication.xmpp;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Service that continues to run in background and respond to the push notification events from the
 * server. This should be registered as service in AndroidManifest.xml.
 */
public class NotificationService extends Service {

    private static final String TAG = NotificationService.class.getName();

    private TelephonyManager telephonyManager;

    // private WifiManager wifiManager;
    //
    // private ConnectivityManager connectivityManager;

    private BroadcastReceiver connectivityReceiver;

    private PhoneStateListener phoneStateListener;

    private ExecutorService executorService;

    private TaskSubmitter taskSubmitter;

    private TaskTracker taskTracker;

    private XmppManager xmppManager;

    private SharedPreferences sharedPrefs;

    private String deviceId;

    public NotificationService() {
        connectivityReceiver = new ConnectivityReceiver(this);
        phoneStateListener = new PhoneStateChangeListener(this);
        executorService = Executors.newSingleThreadExecutor();
        taskSubmitter = new TaskSubmitter(this);
        taskTracker = new TaskTracker(this);
    }

    @Override
    public void onCreate() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // connectivityManager = (ConnectivityManager)
        // getSystemService(Context.CONNECTIVITY_SERVICE);

        sharedPrefs =
                getSharedPreferences(XmppConstants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);

        // Get deviceId
        deviceId = telephonyManager.getDeviceId();

        Editor editor = sharedPrefs.edit();
        editor.putString(XmppConstants.DEVICE_ID, deviceId);
        editor.commit();

        // If running on an emulator
        if (deviceId == null || deviceId.trim().length() == 0 || deviceId.matches("0+")) {
            if (sharedPrefs.contains("EMULATOR_DEVICE_ID")) {
                deviceId = sharedPrefs.getString(XmppConstants.EMULATOR_DEVICE_ID, "");
            } else {
                deviceId =
                        (new StringBuilder("EMU")).append(
                                (new Random(System.currentTimeMillis())).nextLong()).toString();
                editor.putString(XmppConstants.EMULATOR_DEVICE_ID, deviceId);
                editor.commit();
            }
        }

        xmppManager = new XmppManager(this);

        taskSubmitter.submit(new Runnable() {

            public void run() {
                NotificationService.this.start();
            }
        });
    }

    @Override
    public void onDestroy() {
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public TaskSubmitter getTaskSubmitter() {
        return taskSubmitter;
    }

    public TaskTracker getTaskTracker() {
        return taskTracker;
    }

    public XmppManager getXmppManager() {
        return xmppManager;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPrefs;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void connect() {
        taskSubmitter.submit(new Runnable() {

            public void run() {
                NotificationService.this.getXmppManager().connect();
            }
        });
    }

    public void disconnect() {
        taskSubmitter.submit(new Runnable() {

            public void run() {
                NotificationService.this.getXmppManager().disconnect();
            }
        });
    }

    private void registerConnectivityReceiver() {
        telephonyManager
                .listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        IntentFilter filter = new IntentFilter();
        // filter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    private void unregisterConnectivityReceiver() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(connectivityReceiver);
    }

    private void start() {
        registerConnectivityReceiver();
        // Intent intent = getIntent();
        // startService(intent);
        xmppManager.connect();
    }

    private void stop() {
        unregisterConnectivityReceiver();
        xmppManager.disconnect();
        executorService.shutdown();
    }

    /**
     * Class for summiting a new runnable task.
     */
    public class TaskSubmitter {

        final NotificationService notificationService;

        public TaskSubmitter(NotificationService notificationService) {
            this.notificationService = notificationService;
        }

        @SuppressWarnings("unchecked")
        public Future submit(Runnable task) {
            Future result = null;
            if (!notificationService.getExecutorService().isTerminated()
                    && !notificationService.getExecutorService().isShutdown() && task != null) {
                result = notificationService.getExecutorService().submit(task);
            }
            return result;
        }

    }

    /**
     * Class for monitoring the running task count.
     */
    public class TaskTracker {

        final NotificationService notificationService;

        public int count;

        public TaskTracker(NotificationService notificationService) {
            this.notificationService = notificationService;
            this.count = 0;
        }

        public void increase() {
            synchronized (notificationService.getTaskTracker()) {
                notificationService.getTaskTracker().count++;
            }
        }

        public void decrease() {
            synchronized (notificationService.getTaskTracker()) {
                notificationService.getTaskTracker().count--;
            }
        }
    }
}