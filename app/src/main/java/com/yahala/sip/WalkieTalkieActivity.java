package com.yahala.sip;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.net.sip.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.R;


import java.text.ParseException;

/**
 * Handles all calling, receiving calls, and UI interaction in the WalkieTalkie app.
 */
public class WalkieTalkieActivity extends Activity implements View.OnClickListener {


    public SipProfile me = null;
    public String sipAddress = null;
    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |

                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkie_talkie);

        Button pushToTalkButton = (Button) findViewById(R.id.pushToTalk);
        pushToTalkButton.setOnClickListener(this);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //  SIPManager.getInstance().initializeManager();
    }

    @Override
    public void onStart() {
        super.onStart();
        // When we get back from the preference setting Activity, assume
        // settings have changed, and re-login with new auth info.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SIPManager.getInstance().call != null) {
            try {
                SIPManager.getInstance().call.endCall();
            } catch (SipException e) {
                e.printStackTrace();
            }
            SIPManager.getInstance().call.close();
        }

        // SIPManager.getInstance().closeLocalProfile();

        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
    }

    /**
     * Updates the status box with the SIP address of the current call.
     *
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if (useName == null) {
            useName = call.getPeerProfile().getUserName();
        }
        SIPManager.getInstance().updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP Info.");
        menu.add(0, HANG_UP, 0, "End Current Call.");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                if (SIPManager.getInstance().call != null) {
                    try {
                        SIPManager.getInstance().call.endCall();
                        FileLog.e("WalkieTalkieActivity/onOptionsItemSelected",
                                "ending call.");
                    } catch (SipException se) {
                        se.printStackTrace();
                        FileLog.e("WalkieTalkieActivity/onOptionsItemSelected",
                                "Error ending call.");
                    }
                    SIPManager.getInstance().call.close();
                }
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone.")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText)
                                                (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        SIPManager.getInstance().initiateCall(sipAddress);

                                    }
                                })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                                })
                        .create();

            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                                })
                        .create();
        }
        return null;
    }

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
    }

    @Override
    public void onClick(View view) {
        if (SIPManager.getInstance().call == null) {

        } else {
            SIPManager.getInstance().answerCall();
        }


    }
}