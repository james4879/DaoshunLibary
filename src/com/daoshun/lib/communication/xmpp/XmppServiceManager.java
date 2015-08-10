package com.daoshun.lib.communication.xmpp;

import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * This class is to manage the notificatin service and to load the configuration.
 */
public final class XmppServiceManager {

    private static final String TAG = XmppServiceManager.class.getName();

    private Context context;

    private SharedPreferences sharedPrefs;

    private Properties props;

    private String version = "0.5.0";

    private String apiKey;

    private String xmppHost;

    private String xmppPort;

    public XmppServiceManager(Context context, String pushReceivedAction) {
        this.context = context;

        props = loadProperties();
        apiKey = props.getProperty("apiKey", "");
        xmppHost = props.getProperty("xmppHost", "127.0.0.1");
        xmppPort = props.getProperty("xmppPort", "5222");

        sharedPrefs =
                context.getSharedPreferences(XmppConstants.SHARED_PREFERENCE_NAME,
                        Context.MODE_PRIVATE);
        Editor editor = sharedPrefs.edit();
        editor.putString(XmppConstants.API_KEY, apiKey);
        editor.putString(XmppConstants.VERSION, version);
        editor.putString(XmppConstants.XMPP_HOST, xmppHost);
        editor.putInt(XmppConstants.XMPP_PORT, Integer.parseInt(xmppPort));
        editor.putString(XmppConstants.XMPP_RECEIVED_ACTION, pushReceivedAction);
        editor.commit();
    }

    public void startService() {
        Thread serviceThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(context, NotificationService.class);
                context.startService(intent);
            }
        });
        serviceThread.start();
    }

    public void stopService() {
        Intent intent = new Intent(context, NotificationService.class);
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
