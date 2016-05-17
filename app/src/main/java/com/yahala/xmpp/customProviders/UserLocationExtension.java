package com.yahala.xmpp.customProviders;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.PacketExtension;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Represents a chat state which is an extension to message packets which is used to indicate
 * the current status of a chat participant.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.chatstates.ChatState
 */
public class UserLocationExtension implements PacketExtension {

    public static final String NAMESPACE = "http://jabber.org/protocol/geoloc";
    public static final String ELEMENT_NAME = "geoloc";
    public Location location;

    /**
     * Default constructor. The argument provided is the state that the extension will represent.
     *
     * @param location the state that the extension represents.
     */
    public UserLocationExtension(Location location) {
        this.location = location;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngelBracket();

        xml.openElement("lon");

        xml.escape(location.lon + "");
        xml.closeElement("lon");


        xml.openElement("lat");

        xml.escape(location.lat + "");
        xml.closeElement("lat");
        xml.closeElement(this);
        return xml;
    }

    public static final class UserLocationProvider extends ExtensionElementProvider {
        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            /* String jid = parser.getAttributeValue(null, "jid");
            String nodeId = parser.getAttributeValue(null, "node");
            String subId = parser.getAttributeValue(null, "subid");
            String state = parser.getAttributeValue(null, "subscription");*/
            Double lon = 0d;
            Double lat = 0d;
            boolean done = false;
            while (!done) {
                int eventType = parser.next();

                if (eventType == XmlPullParser.START_TAG) {
                    if ("lon".equalsIgnoreCase(parser.getName())) {
                        String longValue = parser.nextText();

                       /* if (longValue.equals("0.0"))
                            longValue="0";*/
                        lon = Double.parseDouble(longValue);
                        parser.nextTag();

                        if ("lat".equalsIgnoreCase(parser.getName())) {
                            String latValue = parser.nextText();
                           /* if (latValue.equals("0.0"))
                                latValue="0";*/
                            lat = Double.parseDouble(latValue);
                        }
                        done = true;
                    }


                }
            }


            return new UserLocationExtension(new Location(lon, lat));
        }
    }
    /*public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            Location location;
            try {
            //    location =parser.getName());
            }
            catch (Exception ex) {
                location = ChatState.active;
            }
            return new UserLocationExtension(location);
        }
    }
}public static final class Provider implements PacketExtensionProvider {

        @Override
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {

            long lon = 0;
            long lat = 0;
            boolean done=false;
            while (!done)
            {
                int eventType = parser.next();


                if (eventType == XmlPullParser.END_TAG)
                {
                    if ("url".equals(parser.getName())) {
                        done = true;
                    }
                }
                else if (eventType == XmlPullParser.TEXT ) {
                    url = parser.getText();
                }
            }

            if (url != null)
                return new Location(lon, lat);
            else
                return null;
        }

    }*/
}