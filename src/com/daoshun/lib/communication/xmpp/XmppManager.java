/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.daoshun.lib.communication.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.util.Log;

/**
 * This class is to manage the XMPP connection between client and server.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class XmppManager {

    private static final String TAG = XmppManager.class.getName();

    private static final String XMPP_RESOURCE_NAME = "AndroidpnClient";

    private NotificationService context;

    private NotificationService.TaskSubmitter taskSubmitter;

    private NotificationService.TaskTracker taskTracker;

    private SharedPreferences sharedPrefs;

    private String xmppHost;

    private int xmppPort;

    private XMPPConnection connection;

    private String username;

    private String password;

    private String action;

    private ConnectionListener connectionListener;

    private PacketListener notificationPacketListener;

    private Handler handler;

    private List<Runnable> taskList;

    private boolean running = false;

    private Future<?> futureTask;

    private Thread reconnection;

    public XmppManager(NotificationService notificationService) {
        context = notificationService;
        taskSubmitter = notificationService.getTaskSubmitter();
        taskTracker = notificationService.getTaskTracker();
        sharedPrefs = notificationService.getSharedPreferences();

        xmppHost = sharedPrefs.getString(XmppConstants.XMPP_HOST, "localhost");
        xmppPort = sharedPrefs.getInt(XmppConstants.XMPP_PORT, 5222);
        username = sharedPrefs.getString(XmppConstants.XMPP_USERNAME, "");
        password = sharedPrefs.getString(XmppConstants.XMPP_PASSWORD, "");
        setAction(sharedPrefs.getString(XmppConstants.XMPP_RECEIVED_ACTION,
                "com.totyu.lib.communication.xmpp.XMPP_RECEIVED_ACTION"));

        connectionListener = new PersistentConnectionListener(this);
        notificationPacketListener = new NotificationPacketListener(this);

        handler = new Handler();
        taskList = new ArrayList<Runnable>();
        reconnection = new ReconnectionThread(this);
    }

    public Context getContext() {
        return context;
    }

    public void connect() {
        submitLoginTask();
    }

    public void disconnect() {
        terminatePersistentConnection();
    }

    public void terminatePersistentConnection() {
        Runnable runnable = new Runnable() {

            final XmppManager xmppManager = XmppManager.this;

            public void run() {
                if (xmppManager.isConnected()) {
                    xmppManager.getConnection().removePacketListener(
                            xmppManager.getNotificationPacketListener());
                    xmppManager.getConnection().disconnect();
                }
                xmppManager.runTask();
            }

        };
        addTask(runnable);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public PacketListener getNotificationPacketListener() {
        return notificationPacketListener;
    }

    public void startReconnectionThread() {
        synchronized (reconnection) {
            if (!reconnection.isAlive()) {
                reconnection.setName("Xmpp Reconnection Thread");
                reconnection.start();
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public void reregisterAccount() {
        removeAccount();
        submitLoginTask();
        runTask();
    }

    public List<Runnable> getTaskList() {
        return taskList;
    }

    public Future<?> getFutureTask() {
        return futureTask;
    }

    public void runTask() {
        synchronized (taskList) {
            running = false;
            futureTask = null;
            if (!taskList.isEmpty()) {
                Runnable runnable = (Runnable) taskList.get(0);
                taskList.remove(0);
                running = true;
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }
            }
        }
        taskTracker.decrease();
    }

    private String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        return uuidRaw.replaceAll("-", "");
    }

    private boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    private boolean isAuthenticated() {
        return connection != null && connection.isConnected() && connection.isAuthenticated();
    }

    private boolean isRegistered() {
        return sharedPrefs.contains(XmppConstants.XMPP_USERNAME)
                && sharedPrefs.contains(XmppConstants.XMPP_PASSWORD);
    }

    private void submitConnectTask() {
        addTask(new ConnectTask());
    }

    private void submitRegisterTask() {
        submitConnectTask();
        addTask(new RegisterTask());
    }

    private void submitLoginTask() {
        submitRegisterTask();
        addTask(new LoginTask());
    }

    private void addTask(Runnable runnable) {
        taskTracker.increase();
        synchronized (taskList) {
            if (taskList.isEmpty() && !running) {
                running = true;
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }
            } else {
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        runTask();
                    }
                }, 30000);
                taskList.add(runnable);
            }
        }
    }

    private void removeAccount() {
        Editor editor = sharedPrefs.edit();
        editor.remove(XmppConstants.XMPP_USERNAME);
        editor.remove(XmppConstants.XMPP_PASSWORD);
        editor.commit();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * A runnable task to connect the server.
     */
    private class ConnectTask implements Runnable {

        final XmppManager xmppManager;

        private ConnectTask() {
            this.xmppManager = XmppManager.this;
        }

        public void run() {
            if (!xmppManager.isConnected()) {
                // Create the configuration for this new connection
                ConnectionConfiguration connConfig =
                        new ConnectionConfiguration(xmppHost, xmppPort);
                // connConfig.setSecurityMode(SecurityMode.disabled);
                connConfig.setSecurityMode(SecurityMode.required);
                connConfig.setSASLAuthenticationEnabled(false);
                connConfig.setCompressionEnabled(false);

                XMPPConnection connection = new XMPPConnection(connConfig);
                xmppManager.setConnection(connection);

                try {
                    // Connect to the server
                    connection.connect();

                    // packet provider
                    ProviderManager.getInstance().addIQProvider("notification",
                            "androidpn:iq:notification", new NotificationIQProvider());

                } catch (XMPPException e) {
                    Log.e(TAG, "XMPP connection failed", e);
                }

                xmppManager.runTask();

            } else {
                xmppManager.runTask();
            }
        }
    }

    /**
     * A runnable task to register a new user onto the server.
     */
    private class RegisterTask implements Runnable {

        final XmppManager xmppManager;

        private RegisterTask() {
            xmppManager = XmppManager.this;
        }

        public void run() {
            if (!xmppManager.isRegistered()) {
                // final String newUsername = newRandomUUID();
                // final String newPassword = newRandomUUID();
                final String newUsername = "1386" + context.getDeviceId().toLowerCase();
                final String newPassword = "1386" + context.getDeviceId().toLowerCase();

                Registration registration = new Registration();

                PacketFilter packetFilter =
                        new AndFilter(new PacketIDFilter(registration.getPacketID()),
                                new PacketTypeFilter(IQ.class));

                PacketListener packetListener = new PacketListener() {

                    public void processPacket(Packet packet) {
                        if (packet instanceof IQ) {
                            IQ response = (IQ) packet;
                            if (response.getType() == IQ.Type.ERROR) {
                                if (!response.getError().toString().contains("409")) {
                                    Log.e(TAG, "Unknown error while registering XMPP account! "
                                            + response.getError().getCondition());
                                }
                            } else if (response.getType() == IQ.Type.RESULT) {
                                xmppManager.setUsername(newUsername);
                                xmppManager.setPassword(newPassword);

                                Editor editor = sharedPrefs.edit();
                                editor.putString(XmppConstants.XMPP_USERNAME, newUsername);
                                editor.putString(XmppConstants.XMPP_PASSWORD, newPassword);
                                editor.commit();
                                xmppManager.runTask();
                            }
                        }
                    }
                };

                connection.addPacketListener(packetListener, packetFilter);

                registration.setType(IQ.Type.SET);
//                 registration.setTo(xmppHost);
//                 Map<String, String> attributes = new HashMap<String, String>();
//                 attributes.put("username", newUsername);
//                 attributes.put("password", newPassword);
//                 registration.setAttributes(attributes);
                registration.addAttribute("username", newUsername);
                registration.addAttribute("password", newPassword);
//                 registration.setAttributes(attributes);
                 connection.sendPacket(registration);

            } else {
                xmppManager.runTask();
            }
        }
    }

    /**
     * A runnable task to log into the server.
     */
    private class LoginTask implements Runnable {

        final XmppManager xmppManager;

        private LoginTask() {
            this.xmppManager = XmppManager.this;
        }

        public void run() {
            if (!xmppManager.isAuthenticated()) {

                try {
                    xmppManager.getConnection().login(xmppManager.getUsername(),
                            xmppManager.getPassword(), XMPP_RESOURCE_NAME);

                    // connection listener
                    if (xmppManager.getConnectionListener() != null) {
                        xmppManager.getConnection().addConnectionListener(
                                xmppManager.getConnectionListener());
                    }

                    // packet filter
                    PacketFilter packetFilter = new PacketTypeFilter(NotificationIQ.class);
                    // packet listener
                    PacketListener packetListener = xmppManager.getNotificationPacketListener();
                    connection.addPacketListener(packetListener, packetFilter);

                    xmppManager.runTask();

                } catch (XMPPException e) {
                    Log.e(TAG, "LoginTask.run()... xmpp error");
                    Log.e(TAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
                    String INVALID_CREDENTIALS_ERROR_CODE = "401";
                    String errorMessage = e.getMessage();
                    if (errorMessage != null
                            && errorMessage.contains(INVALID_CREDENTIALS_ERROR_CODE)) {
                        xmppManager.reregisterAccount();
                        return;
                    }
                    xmppManager.startReconnectionThread();

                } catch (Exception e) {
                    Log.e(TAG, "LoginTask.run()... other error");
                    Log.e(TAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
                    xmppManager.startReconnectionThread();
                }

            } else {
                xmppManager.runTask();
            }

        }
    }

}
