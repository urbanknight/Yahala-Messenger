package com.yahala.xmpp.customProviders;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


/**
 * XEP-0066: Out of Band Data
 * http://xmpp.org/extensions/xep-0066.html
 */
public class PhoneCall implements ExtensionElement {

    public static final String NAMESPACE = "jabber:x:pc";
    public static final String ELEMENT_NAME = "x";

    private final String mRoomId;
    private final Boolean mVideoCall;
    private final Boolean mCallHangUp;

    public PhoneCall(String roomId) {
        this(roomId, false, false);
    }

    public PhoneCall(String roomId, boolean videoCall, boolean callHangUp) {
        mRoomId = roomId;
        mVideoCall = videoCall;
        mCallHangUp = callHangUp;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public boolean getVideoCall() {
        return mVideoCall;
    }

    public boolean getCallHangUp() {
        return mCallHangUp;
    }

    @Override
    public String toXML() {
        /*
<x xmlns='jabber:x:oob'>
    <url type='image/png' length='2034782'>http://prime.kontalk.net/media/filename_or_hash</url>
</x>
*/
        StringBuilder xml = new StringBuilder();
        xml.append(String.format("<%s xmlns='%s'><roomId", ELEMENT_NAME, NAMESPACE));
        if (mVideoCall != null)
            xml.append(String.format(" vCall='%s'", mVideoCall));
        xml.append(String.format(" hangUp='%s'", mCallHangUp));
        xml.append(">");
        xml.append(mRoomId);
        xml.append(String.format("</roomId></%s>", ELEMENT_NAME));
        return xml.toString();
    }

    public static final class Provider extends ExtensionElementProvider {
        @Override
        public Element parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
            String roomId = null;
            boolean videoCall = false;
            boolean in_roomId = false, done = false, callHangUp = false;

            while (!done) {
                int eventType = parser.next();

                if (eventType == XmlPullParser.START_TAG) {
                    if ("roomId".equals(parser.getName())) {
                        in_roomId = true;
                        videoCall = Boolean.valueOf(parser.getAttributeValue(null, "vCall"));
                        callHangUp = Boolean.valueOf(parser.getAttributeValue(null, "hangUp"));
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("roomId".equals(parser.getName())) {
                        done = true;
                    }
                } else if (eventType == XmlPullParser.TEXT && in_roomId) {
                    roomId = parser.getText();
                }
            }

            if (roomId != null)
                return new PhoneCall(roomId, videoCall, callHangUp);
            else
                return null;
        }
    }

}
