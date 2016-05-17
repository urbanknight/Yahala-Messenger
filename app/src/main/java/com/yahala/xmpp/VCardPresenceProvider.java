package com.yahala.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.PacketExtension;

import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by user on 5/5/2014.
 */
public class VCardPresenceProvider extends DataPacketProvider.PacketExtensionProvider {

    @Override
    public org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        return null;
    }
}
