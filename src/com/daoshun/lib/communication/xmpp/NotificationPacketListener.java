package com.daoshun.lib.communication.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.content.Intent;

/**
 * This class notifies the receiver of incoming notifcation packets asynchronously.
 */
public class NotificationPacketListener implements PacketListener {

    private static final String TAG = NotificationPacketListener.class.getName();

    private final XmppManager xmppManager;

    public NotificationPacketListener(XmppManager xmppManager) {
        this.xmppManager = xmppManager;
    }

    @Override
    public void processPacket(Packet packet) {
        if (packet instanceof NotificationIQ) {
            NotificationIQ notification = (NotificationIQ) packet;

            if (notification.getChildElementXML().contains("androidpn:iq:notification")) {
                String notificationId = notification.getId();
                String notificationApiKey = notification.getApiKey();
                String notificationTitle = notification.getTitle();
                String notificationMessage = notification.getMessage();
                // String notificationTicker = notification.getTicker();
                String notificationUri = notification.getUri();

                Intent intent = new Intent(xmppManager.getAction());
                intent.putExtra(XmppConstants.NOTIFICATION_ID, notificationId);
                intent.putExtra(XmppConstants.NOTIFICATION_API_KEY, notificationApiKey);
                intent.putExtra(XmppConstants.NOTIFICATION_TITLE, notificationTitle);
                intent.putExtra(XmppConstants.NOTIFICATION_MESSAGE, notificationMessage);
                intent.putExtra(XmppConstants.NOTIFICATION_URI, notificationUri);

                xmppManager.getContext().sendBroadcast(intent);
            }
        }
    }
}