package com.yahala.sip;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.R;
import com.yahala.messenger.UserConfig;
import com.yahala.messenger.Utilities;
import com.yahala.ui.ApplicationLoader;

import java.text.ParseException;

/**
 * Created by user on 4/20/2015.
 */
public class SIPManager {
    private static volatile SIPManager Instance = null;

    public static String HOST =/*"172.17.1.52"*/ "188.247.90.132";
    public static int PORT = 5060;
    public static int SPORT = 5060;
    public static String Password = "android";
    public static String Username = "android";
    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;
    private int count = 1;
    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;
    private boolean canReconnect = true;
    public static String RESOURCE = "android";

    public static SIPManager getInstance() {
        SIPManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (SIPManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new SIPManager();
                }
            }
        }
        return localInstance;
    }

    public SIPManager() {
        // initializeManager();
    }

    public void initializeManager() {
        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.
        FileLog.e("Sip Manager", "initializeManager");
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.yahala.sip.IncomingCallReceiver");
        callReceiver = new IncomingCallReceiver();
        ApplicationLoader.applicationContext.registerReceiver(callReceiver, filter);

        // "Push to talk" can be a serious pain when the screen keeps turning off.
        // Let's prevent that.
        if (manager == null) {
            manager = SipManager.newInstance(ApplicationLoader.applicationContext);
        }

        initializeLocalProfile();
    }

    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }


        FileLog.e("Sip Manager", "initializeManager");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        String username = "123456";// prefs.getString("namePref", "");
        String domain = HOST; //prefs.getString("domainPref", "");
        String password = "123456";// prefs.getString("passPref", "");

        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            //showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder("123456", HOST); //new SipProfile.Builder(username, domain);
            builder.setDisplayName("Wael Almahameed");
            builder.setProfileName("Yahala");
            builder.setPassword(password);
            builder.setSendKeepAlive(true);
            //builder.setProtocol("TCP")
            //builder.setOutboundProxy("172.17.100.25");
            builder.setAutoRegistration(true);
            builder.setPort(5060);
            builder.setProtocol("UDP");

            me = builder.build();
            /* if (me != null) {
                closeLocalProfile();
            }*/
            Intent i = new Intent();
            i.setAction("com.yahala.sip.IncomingCallReceiver");
            PendingIntent pi = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);


            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.
            SipRegistrationListener registrationListener = new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    updateStatus("Registering with SIP Server...");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateStatus("Ready");
                    count = 1;
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    updateStatus("Sip Registration failed. error \n" + errorCode + " : " + errorMessage);
                    /* if (canReconnect){
                    Utilities.globalQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus("Registration failed.  Please check settings. Trying after " + (5000 * count / 60) + "s");
                            initializeLocalProfile();
                            if (count <= 6)
                            {count++;}
                            canReconnect=true;
                        }
                    },5000*count);}
                    canReconnect=false;*/
                }
            };
            manager.setRegistrationListener(me.getUriString(), registrationListener);

        } catch (ParseException pe) {
            updateStatus("Connection Error.");
        } catch (SipException se) {
            updateStatus("Connection error.");
        }
    }

    public void answerCall() {
        try {
            call.answerCall(30);
            call.startAudio();
            call.setSpeakerMode(true);
            if (call.isMuted()) {
                call.toggleMute();
            }
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */
    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }
        try {
            if (me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            FileLog.e("Sip manager", "Failed to close local profile.", ee);
        }
    }

    /**
     * Make an outgoing call.
     */
    public void initiateCall(String sipAddress) {

        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    call.toggleMute();
                    updateStatus();
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Call Ended");
                }
            };

            call = manager.makeAudioCall(me.getUriString(), sipAddress, listener, 30);

        } catch (Exception e) {
            FileLog.e("WalkieTalkieActivity/InitiateCall", "Error when trying to close manager.", e);
            if (me != null) {
                try {
                    manager.close(me.getUriString());
                } catch (Exception ee) {
                    FileLog.e("WalkieTalkieActivity/InitiateCall",
                            "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
            }
        }
    }

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     *
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        Utilities.RunOnUIThread(new Runnable() {
            public void run() {

                FileLog.e("Yahala SIP", status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current call.
     */
    public void updateStatus() {

        String useName = call.getPeerProfile().getDisplayName();
        if (useName == null) {
            useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

}


