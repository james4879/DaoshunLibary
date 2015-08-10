package com.daoshun.lib.communication.xmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * A broadcast receiver to handle the changes in network connectiion states.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityReceiver.class.getName();

    private NotificationService notificationService;

    public ConnectivityReceiver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {
            if (networkInfo.isConnectedOrConnecting()) {
                notificationService.connect();
            }
        } else {
            Log.e(TAG, "Network unavailable");
            notificationService.disconnect();
        }
    }

}
