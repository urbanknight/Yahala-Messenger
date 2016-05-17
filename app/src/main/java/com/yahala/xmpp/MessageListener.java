package com.yahala.xmpp;

/**
 * Created by user on 5/22/2014.
 */

import com.yahala.messenger.FileLog;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Stanza;

import java.util.logging.Logger;

public class MessageListener implements StanzaListener {
    private final static Logger LOGGER = Logger.getLogger(MessageListener.class.getName());


    MessageListener() {

    }

    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        Message m = (Message) packet;

        if (m.getType() == Message.Type.chat || m.getType() == Message.Type.normal) {
            // somebody has news for us
            FileLog.e(LOGGER.getName(), "got message: " + m);
            MessagesController.getInstance().processChatMessage(m);
        }


        // error message
        else if (m.getType() == Message.Type.error) {
            FileLog.e(LOGGER.getName(), "got error message: " + m);
        } else {
            FileLog.e(LOGGER.getName(), "unknown message type: " + m.getType());
        }
    }
}
