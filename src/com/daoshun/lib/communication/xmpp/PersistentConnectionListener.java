package com.daoshun.lib.communication.xmpp;

import org.jivesoftware.smack.ConnectionListener;

/**
 * A listener class for monitoring connection closing and reconnection events.
 */
public class PersistentConnectionListener implements ConnectionListener {

    private static final String TAG = PersistentConnectionListener.class.getName();

    private final XmppManager xmppManager;

    public PersistentConnectionListener(XmppManager xmppManager) {
        this.xmppManager = xmppManager;
    }

    @Override
    public void connectionClosed() {}

    @Override
    public void connectionClosedOnError(Exception e) {
        if (xmppManager.getConnection() != null && xmppManager.getConnection().isConnected()) {
            xmppManager.getConnection().disconnect();
        }
        xmppManager.startReconnectionThread();
    }

    @Override
    public void reconnectingIn(int seconds) {}

    @Override
    public void reconnectionFailed(Exception e) {}

    @Override
    public void reconnectionSuccessful() {}

}
