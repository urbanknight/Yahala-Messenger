package com.yahala.sip;


import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.*;
import android.os.PowerManager;
import android.util.Log;

import com.yahala.messenger.FileLog;
import com.yahala.ui.ApplicationLoader;

/**
 * Listens for incoming SIP calls, intercepts and hands them off to WalkieTalkieActivity.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     *
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
    PowerManager.WakeLock fullWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        SipAudioCall incomingCall = null;
        FileLog.e("IncomingCallReceiver", "Call received");
        Intent intent2 = new Intent(ApplicationLoader.applicationContext, WalkieTalkieActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // EditText editText = (EditText) findViewById(R.id.edit_message);
        // String message = editText.getText().toString();
        // intent.putExtra(EXTRA_MESSAGE, message);
        ApplicationLoader.applicationContext.startActivity(intent2);

        PowerManager powerManager = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "Loneworker - FULL WAKE LOCK");
        //PowerManager.WakeLock partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loneworker - PARTIAL WAKE LOCK");

        try {
            fullWakeLock.acquire();
           /* KeyguardManager keyguardManager = (KeyguardManager) ApplicationLoader.applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
            final KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
            keyguardLock.disableKeyguard();*/
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCallEnded(SipAudioCall call) {
                    FileLog.e("IncomingCallReceiver", "onCallEnded");
                    // keyguardLock.reenableKeyguard();
                    fullWakeLock.release();
                }

                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    FileLog.e("IncomingCallReceiver", "onRinging");
                   /* try {
                        call.answerCall(30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    FileLog.e("IncomingCallReceiver", "onCallEstablished");
                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    FileLog.e("IncomingCallReceiver", "onCallBusy");
                }

                @Override
                public void onCallHeld(SipAudioCall call) {
                    FileLog.e("IncomingCallReceiver", "onCallHeld");
                }

                @Override
                public void onReadyToCall(SipAudioCall call) {
                    FileLog.e("IncomingCallReceiver", "onReadyToCall");
                }

                @Override
                public void onCalling(SipAudioCall call) {

                    FileLog.e("IncomingCallReceiver", "onCalling");
                }

                @Override
                public void onError(SipAudioCall call, int errorCode, String message) {

                    // keyguardLock.reenableKeyguard();
                    fullWakeLock.release();
                    FileLog.e("IncomingCallReceiver", "onError: " + errorCode + " - " + message);
                }

            };

            //WalkieTalkieActivity wtActivity = (WalkieTalkieActivity) context;


            incomingCall = SIPManager.getInstance().manager.takeAudioCall(intent, listener);
            /*
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);

            if(incomingCall.isMuted()) {
               incomingCall.toggleMute();
            }

            */

            SIPManager.getInstance().call = incomingCall;
            SIPManager.getInstance().updateStatus();

        } catch (Exception e) {
            fullWakeLock.release();
            e.printStackTrace();
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }

}
