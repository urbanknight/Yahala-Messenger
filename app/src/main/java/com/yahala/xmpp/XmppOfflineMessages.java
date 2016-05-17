package com.yahala.xmpp;

import android.util.Log;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.offline.OfflineMessageManager;

import java.util.List;

//This class is not needed any more, packet listener is more than enough : Wael
public class XmppOfflineMessages {
    public static void handleOfflineMessages(final XMPPConnection connection) {
        // Utilities.xmppQueue.postRunnable(new Runnable() {
        //  @Override
        //   public void run() {
        try {
            Log.i("XmppOfflineMessages", "Begin retrieval of offline messages from server");
            OfflineMessageManager offlineMessageManager = new OfflineMessageManager(connection);

            if (!offlineMessageManager.supportsFlexibleRetrieval()) {
                Log.i("XmppOfflineMessages", "Offline messages not supported");
                return;
            }

            if (offlineMessageManager.getMessageCount() == 0) {
                Log.i("XmppOfflineMessages", "No offline messages found on server");
            } else {

                List<org.jivesoftware.smack.packet.Message> messages = offlineMessageManager.getMessages();


                offlineMessageManager.deleteMessages();

                for (org.jivesoftware.smack.packet.Message message : messages) {
                    MessagesController.getInstance().processChatMessage(message);
                }
                Log.i("XmppOfflineMessages[[", "End of retrieval of offline messages from server");
            }
            Log.i("XmppOfflineMessages[[", "End of offline messages");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //    }
        //  });
    }
}
