package com.yahala.xmpp;

import com.yahala.SQLite.Messages;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;

import org.jivesoftware.smack.XMPPConnection;
//import org.jivesoftware.smackx.xevent.DefaultMessageEventRequestListener;
//import org.jivesoftware.smackx.xevent.MessageEventManager;
//import org.jivesoftware.smackx.xevent.MessageEventNotificationListener;
import com.yahala.messenger.Utilities;

import java.io.File;

/**
 * Created by user on 4/29/2014.
 */
public class XmppNotifications {
    private static volatile XmppNotifications Instance = null;
    //  public MessageEventManager messageEventManager = null;

    public XmppNotifications() {

    }

    public static XmppNotifications getInstance() {
        XmppNotifications localInstance = Instance;
        if (localInstance == null) {
            synchronized (XMPPManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new XmppNotifications();
                }
            }
        }
        return localInstance;
    }

    public static void updateMessageSendState(String mid, String aknSentFromJid, String aknReqFromJid, int sendState) {
        try {
            Messages newMsg = MessagesStorage.getInstance().getMessage(mid);
            FileLog.e("Test", "onReceiptReceived : newMsg id " + newMsg.getId() + newMsg.getMessage() + " sendstate " + newMsg.getSend_state());
            newMsg.setSend_state(XMPPManager.MESSAGE_SEND_STATE_AKN);
            MessagesStorage.getInstance().updateMessage(newMsg);

            FileLog.e("Test", "onReceiptReceived : " + aknSentFromJid + ", " + aknReqFromJid + ", " + mid);
        } catch (Exception e) {
            FileLog.e("XmppNotifcation", e.toString());
        }
    }

    public static void addMessageEventListener(XMPPConnection connection) {
       /* PrivateDataManager privateDataManager = new PrivateDataManager(XmppManager.getInstance().getConnection());
       try {
           PrivateData privateData = privateDataManager.getPrivateData("yahala", "contacts");

       }
       catch (Exception ex){

       }*//*
        MessageEventManager messageEventManager = null;
        if (messageEventManager == null) {
            messageEventManager = new MessageEventManager(connection);
            // User2 adds the listener that will react to the event notifications requests
            messageEventManager.addMessageEventRequestListener(new DefaultMessageEventRequestListener() {
                public void deliveredNotificationRequested(
                        String from,
                        String packetID,
                        MessageEventManager messageEventManager) {
                    try {
                        super.deliveredNotificationRequested(from, packetID, messageEventManager);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // DefaultMessageEventRequestListener automatically responds that the message was delivered when receives this request
                    FileLog.e("Test YXmppNotifications", "Delivered Notification Requested (" + from + ", " + packetID + ")");
                }

                public void displayedNotificationRequested(
                        String from,
                        String packetID,
                        MessageEventManager messageEventManager) {
                    super.displayedNotificationRequested(from, packetID, messageEventManager);
                    // Send to the message's sender that the message was displayed
                    try {
                        messageEventManager.sendDisplayedNotification(from, packetID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FileLog.e("Test YXmppNotifications", "sendDisplayedNotification " + "from :" + from + ", packetID" + packetID + ")");
                }

                public void composingNotificationRequested(
                        String from,
                        String packetID,
                        MessageEventManager messageEventManager) {
                    super.composingNotificationRequested(from, packetID, messageEventManager);
                    // Send to the message's sender that the message's receiver is composing a reply
                    try {
                        messageEventManager.sendComposingNotification(from, packetID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FileLog.e("Test YXmppNotifications", "sendComposingNotification " + "from :" + from + ", packetID" + packetID + ")");
                }

                public void offlineNotificationRequested(
                        String from,
                        String packetID,
                        MessageEventManager messageEventManager) {
                    super.offlineNotificationRequested(from, packetID, messageEventManager);
                    // The XMPP server should take care of this request. Do nothing.
                    FileLog.e("Test YXmppNotifications", "Offline Notification Requested (" + from + ", " + packetID + ")");
                }
            });


            // Add the listener that will react to the event notifications
            messageEventManager.addMessageEventNotificationListener(new MessageEventNotificationListener() {
                public void deliveredNotification(final String from, final String packetID) {
                    FileLog.e("Test YXmppNotifications", "The message has been delivered (" + from + ", " + packetID + ")");
                    updateMessageSendState(packetID, from, "", XMPPManager.MESSAGE_SEND_STATE_SENT);
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageReceivedByAck, from, packetID);
                        }
                    });

                }

                public void displayedNotification(final String from, final String packetID) {
                    FileLog.e("Test YXmppNotifications", "The message has been displayed (" + from + ", " + packetID + ")");
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageRead, from, packetID);
                        }
                    });


                }

                public void composingNotification(final String from, final String packetID) {
                    FileLog.e("Test YXmppNotifications", "The message's receiver is composing a reply (" + from + ", " + packetID + ")");
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageComposing, from, packetID);
                        }
                    });
                }

                public void offlineNotification(final String from, final String packetID) {
                    FileLog.e("Test YXmppNotifications", "The message's receiver is offline (" + from + ", " + packetID + ")");
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageReceivedByServer, from, packetID);
                        }
                    });
                }

                public void cancelledNotification(final String from, final String packetID) {
                    FileLog.e("Test YXmppNotifications", "The message's receiver cancelled composing a reply (" + from + ", " + packetID + ")");
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageComposingCancelled, from, packetID);
                        }
                    });
                }
            });

        }*/
    }

}
