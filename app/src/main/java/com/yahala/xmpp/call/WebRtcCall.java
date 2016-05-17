package com.yahala.xmpp.call;

/**
 * Created by Work on 1/5/2016.
 */
public class WebRtcCall {
    private static volatile WebRtcCall Instance = null;


    public static WebRtcCall getInstance() {
        WebRtcCall localInstance = Instance;
        if (localInstance == null) {
            synchronized (WebRtcPhone.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new WebRtcCall();
                }
            }
        }
        return localInstance;
    }


}
