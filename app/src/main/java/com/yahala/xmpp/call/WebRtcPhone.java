package com.yahala.xmpp.call;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.Utilities;
import com.yahala.ui.ApplicationLoader;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.R;
import com.yahala.messenger.UserConfig;
import com.yahala.xmpp.XMPPManager;

import org.appspot.apprtc.CallActivity;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Created by user on 5/24/2014.
 */
public class WebRtcPhone {
    private static final String TAG = "WebRtcPhone";
    private static final int CONNECTION_REQUEST = 1;
    private static boolean commandLineRun = false;
    private static boolean mSendCallRequest = false;
    private SharedPreferences sharedPref;
    public String jid;
    private int delay = 2000;//// TODO: 1/3/2016 for testing only
    private String mRoomID;
    private String keyprefVideoCallEnabled;
    private String keyprefResolution;
    private String keyprefFps;
    private String keyprefCaptureQualitySlider;
    private String keyprefVideoBitrateType;
    private String keyprefVideoBitrateValue;
    private String keyprefVideoCodec;
    private String keyprefAudioBitrateType;
    private String keyprefAudioBitrateValue;
    private String keyprefAudioCodec;
    private String keyprefHwCodecAcceleration;
    private String keyprefCaptureToTexture;
    private String keyprefNoAudioProcessingPipeline;
    private String keyprefOpenSLES;
    private String keyprefDisplayHud;
    private String keyprefTracing;
    private String keyprefRoomServerUrl;
    private String keyprefRoom;
    private String keyprefRoomList;
    private boolean videoCall = false;
    public ToneGenerator tg;
    Ringtone ringtone;
    private ArrayList<String> roomList;
    private ArrayAdapter<String> adapter;
    private String keyprefRoomLis;
    private static volatile WebRtcPhone Instance = null;

    public WebRtcPhone() {
        Initiate();
    }

    public interface PhoneEvents {
        public void onRinging(final String message);

        public void onCallReceived();

        public void onCallEnded(final String description);
    }

    public static WebRtcPhone getInstance() {
        WebRtcPhone localInstance = Instance;
        if (localInstance == null) {
            synchronized (WebRtcPhone.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new WebRtcPhone();
                }
            }
        }
        return localInstance;
    }

    public boolean IsSendRequest() {
        return mSendCallRequest;
    }

    public void Call(String id, boolean sendCallRequest, boolean videoCall) {
        mSendCallRequest = sendCallRequest;


        if (mSendCallRequest == true) {
            this.jid = id;
            this.videoCall = videoCall;
        } else {
            mRoomID = id;
        }

        if (!mSendCallRequest) {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(ApplicationLoader.applicationContext, notification);
            ringtone.play();

            connectToRoom(false, 0, true, videoCall);
              /*  Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {

                    }
              },delay);*/
        } else {

            connectToRoom(false, 0, false, videoCall);
        }

    }

    public void AnswerCall() {

        if (ringtone != null) {
            ringtone.stop();

        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (tg != null) {
                    tg.stopTone();
                }
            }
        });


    }

    public void Receive() {

    }


    public void CallRequest(String roomId) {

        if (mSendCallRequest) {
            XMPPManager.getInstance().sendMessage(jid, 0, new CallParameter(roomId, videoCall, false));
          /*  audioManager = (AudioManager)ApplicationLoader.applicationContext.getSystemService(ApplicationLoader.applicationContext.AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(true);
           */
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
                    tg.startTone(ToneGenerator.TONE_SUP_RINGTONE);
                }
            });

        }

    }

    public void Initiate() {

        // Get setting keys.
        PreferenceManager.setDefaultValues(ApplicationLoader.applicationContext, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext);
        keyprefVideoCallEnabled = ApplicationLoader.applicationContext.getString(R.string.pref_videocall_key);
        keyprefResolution = ApplicationLoader.applicationContext.getString(R.string.pref_resolution_key);
        keyprefFps = ApplicationLoader.applicationContext.getString(R.string.pref_fps_key);
        keyprefCaptureQualitySlider = ApplicationLoader.applicationContext.getString(R.string.pref_capturequalityslider_key);
        keyprefVideoBitrateType = ApplicationLoader.applicationContext.getString(R.string.pref_startvideobitrate_key);
        keyprefVideoBitrateValue = ApplicationLoader.applicationContext.getString(R.string.pref_startvideobitratevalue_key);
        keyprefVideoCodec = ApplicationLoader.applicationContext.getString(R.string.pref_videocodec_key);
        keyprefHwCodecAcceleration = ApplicationLoader.applicationContext.getString(R.string.pref_hwcodec_key);
        keyprefCaptureToTexture = ApplicationLoader.applicationContext.getString(R.string.pref_capturetotexture_key);
        keyprefAudioBitrateType = ApplicationLoader.applicationContext.getString(R.string.pref_startaudiobitrate_key);
        keyprefAudioBitrateValue = ApplicationLoader.applicationContext.getString(R.string.pref_startaudiobitratevalue_key);
        keyprefAudioCodec = ApplicationLoader.applicationContext.getString(R.string.pref_audiocodec_key);
        keyprefNoAudioProcessingPipeline = ApplicationLoader.applicationContext.getString(R.string.pref_noaudioprocessing_key);
        keyprefOpenSLES = ApplicationLoader.applicationContext.getString(R.string.pref_opensles_key);
        keyprefDisplayHud = ApplicationLoader.applicationContext.getString(R.string.pref_displayhud_key);
        keyprefTracing = ApplicationLoader.applicationContext.getString(R.string.pref_tracing_key);
        keyprefRoomServerUrl = ApplicationLoader.applicationContext.getString(R.string.pref_room_server_url_key);
        keyprefRoom = ApplicationLoader.applicationContext.getString(R.string.pref_room_key);
        keyprefRoomList = ApplicationLoader.applicationContext.getString(R.string.pref_room_list_key);

        // If an implicit VIEW intent is launching the app, go directly to that URL.
        /*  final Intent intent =new Intent(,ApplicationLoader.applicationContext);
        if ("android.intent.action.VIEW".equals(intent.getAction())
                && !commandLineRun) {
            commandLineRun = true;
            boolean loopback = intent.getBooleanExtra(
                    CallActivity.EXTRA_LOOPBACK, false);
            int runTimeMs = intent.getIntExtra(
                    CallActivity.EXTRA_RUNTIME, 0);
            String room = sharedPref.getString(keyprefRoom, "");
            roomEditText.setText(room);
            connectToRoom(loopback, runTimeMs);
            return;
        }*/
    }

    private void connectToRoom(boolean loopback, int runTimeMs) {
        connectToRoom(loopback, runTimeMs, false, false);
    }

    private void connectToRoom(boolean loopback, int runTimeMs, boolean Received, boolean videoCallEnabled) {
        // Get room name (random for loopback).
        String roomId;


        if (mSendCallRequest == true) {
            UUID serviceUUID = UUID.randomUUID();
            roomId = serviceUUID.toString();

        } else {
            roomId = mRoomID;
        }

        /*if (loopback) {
          roomId = Integer.toString((new Random()).nextInt(100000000));
        } else {
          roomId = getSelectedItem();
          if (roomId == null) {
            UUID serviceUUID = UUID.randomUUID();
            roomId =serviceUUID.toString();// roomEditText.getText().toString();
          }
        }
        */
        String roomUrl = sharedPref.getString(keyprefRoomServerUrl, ApplicationLoader.applicationContext.getString(R.string.pref_room_server_url_default));//update the server and use ur own compiled version from apprtc server
        // Video call enabled flag.
        //// boolean videoCallEnabled =sharedPref.getBoolean(keyprefVideoCallEnabled, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_videocall_default)));
        // Get default codecs.
        String videoCodec = sharedPref.getString(keyprefVideoCodec, ApplicationLoader.applicationContext.getString(R.string.pref_videocodec_default));
        String audioCodec = sharedPref.getString(keyprefAudioCodec, ApplicationLoader.applicationContext.getString(R.string.pref_audiocodec_default));
        // Check HW codec flag.
        boolean hwCodec = sharedPref.getBoolean(keyprefHwCodecAcceleration, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_hwcodec_default)));
        // Check Capture to texture.
        boolean captureToTexture = sharedPref.getBoolean(keyprefCaptureToTexture, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_capturetotexture_default)));
        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = sharedPref.getBoolean(keyprefNoAudioProcessingPipeline, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_noaudioprocessing_default)));
        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = sharedPref.getBoolean(keyprefOpenSLES, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_opensles_default)));

        // Get video resolution from settings.
        int videoWidth = 0;
        int videoHeight = 0;
        String resolution = sharedPref.getString(keyprefResolution, ApplicationLoader.applicationContext.getString(R.string.pref_resolution_default));
        String[] dimensions = resolution.split("[ x]+");

        if (dimensions.length == 2) {
            try {
                videoWidth = Integer.parseInt(dimensions[0]);
                videoHeight = Integer.parseInt(dimensions[1]);
            } catch (NumberFormatException e) {
                videoWidth = 0;
                videoHeight = 0;
                Log.e(TAG, "Wrong video resolution setting: " + resolution);
            }
        }

        // Get camera fps from settings.
        int cameraFps = 0;
        String fps = sharedPref.getString(keyprefFps,
                ApplicationLoader.applicationContext.getString(R.string.pref_fps_default));
        String[] fpsValues = fps.split("[ x]+");
        if (fpsValues.length == 2) {
            try {
                cameraFps = Integer.parseInt(fpsValues[0]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Wrong camera fps setting: " + fps);
            }
        }

        // Check capture quality slider flag.
        boolean captureQualitySlider = sharedPref.getBoolean(keyprefCaptureQualitySlider,
                Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_capturequalityslider_default)));

        // Get video and audio start bitrate.
        int videoStartBitrate = 0;
        String bitrateTypeDefault = ApplicationLoader.applicationContext.getString(R.string.pref_startvideobitrate_default);
        String bitrateType = sharedPref.getString(
                keyprefVideoBitrateType, bitrateTypeDefault);

        if (!bitrateType.equals(bitrateTypeDefault)) {
            String bitrateValue = sharedPref.getString(keyprefVideoBitrateValue, ApplicationLoader.applicationContext.getString(R.string.pref_startvideobitratevalue_default));
            videoStartBitrate = Integer.parseInt(bitrateValue);
        }

        int audioStartBitrate = 0;
        bitrateTypeDefault = ApplicationLoader.applicationContext.getString(R.string.pref_startaudiobitrate_default);
        bitrateType = sharedPref.getString(
                keyprefAudioBitrateType, bitrateTypeDefault);

        if (!bitrateType.equals(bitrateTypeDefault)) {
            String bitrateValue = sharedPref.getString(keyprefAudioBitrateValue,
                    ApplicationLoader.applicationContext.getString(R.string.pref_startaudiobitratevalue_default));
            audioStartBitrate = Integer.parseInt(bitrateValue);
        }

        // Check statistics display option.
        boolean displayHud = sharedPref.getBoolean(keyprefDisplayHud,
                Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_displayhud_default)));

        boolean tracing = sharedPref.getBoolean(
                keyprefTracing, Boolean.valueOf(ApplicationLoader.applicationContext.getString(R.string.pref_tracing_default)));

        // Start AppRTCDemo activity.
        Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl);


        if (validateUrl(roomUrl)) {
            Uri uri = Uri.parse(roomUrl);
            Intent intent = new Intent(ApplicationLoader.applicationContext, CallActivity.class);
            intent.setData(uri);
            intent.putExtra(CallActivity.EXTRA_ROOMID, roomId);
            intent.putExtra(CallActivity.EXTRA_RECEIVED, Received);
            intent.putExtra(CallActivity.EXTRA_LOOPBACK, loopback);
            intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled);
            intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth);
            intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight);
            intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);
            intent.putExtra(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
            intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, videoStartBitrate);
            intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, videoCodec);
            intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec);
            intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
            intent.putExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
            intent.putExtra(CallActivity.EXTRA_OPENSLES_ENABLED, useOpenSLES);
            intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate);
            intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec);
            intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud);
            intent.putExtra(CallActivity.EXTRA_TRACING, tracing);
            intent.putExtra(CallActivity.EXTRA_CMDLINE, commandLineRun);
            intent.putExtra(CallActivity.EXTRA_RUNTIME, runTimeMs);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ApplicationLoader.applicationContext.startActivity(intent);
        }
    }

    private boolean validateUrl(String url) {

        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
            return true;
        }

        FileLog.e(TAG + ": " + ApplicationLoader.applicationContext.getText(R.string.invalid_url_title), ApplicationLoader.applicationContext.getString(R.string.invalid_url_text, url));

        /* new AlertDialog.Builder(this)
        .setTitle(getText(R.string.invalid_url_title))
        .setMessage(getString(R.string.invalid_url_text, url))
        .setCancelable(false)
        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.cancel();
            }
          }).create().show();*/

        return false;
    }
}
