package com.yahala.xmpp.call;

/**
 * Created by Work on 1/6/2016.
 */
public class CallParameter {
    public String mRoomId = "";
    public boolean mVideoCall = false;
    public boolean mCallHangUp = false;

    public CallParameter(String roomId, boolean videoCall, boolean callHangUp) {
        mRoomId = roomId;
        mVideoCall = videoCall;
        mCallHangUp = callHangUp;
    }
}
