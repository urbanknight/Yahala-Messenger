/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.util.LooperExecutor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.R;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.ApplicationLoader;
import com.yahala.xmpp.XMPPManager;
import com.yahala.xmpp.call.CallParameter;
import com.yahala.xmpp.call.WebRtcPhone;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SurfaceViewRenderer;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends Activity
        implements AppRTCClient.SignalingEvents, NotificationCenter.NotificationCenterDelegate,
        PeerConnectionClient.PeerConnectionEvents,
        CallFragment.OnCallEvents, SensorEventListener {

    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED = "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED = "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_RECEIVED = "org.appspot.apprtc.RECEIVED";
    public static final int CALL_HANGUP = 700;
    private static final String TAG = "CallRTCClient";
    public boolean isIncomingCall = false;
    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
    };

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private EglBase rootEglBase;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;
    private ScalingType scalingType;
    private Toast logToast;
    private boolean commandLineRun;
    private int runTimeMs;
    private boolean activityRunning;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean Received = false;
    private boolean iceConnected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private SensorManager mSensorManager;
    private Sensor myProximitySensor;

    // Controls
    CallFragment callFragment;
    HudFragment hudFragment;
    PowerManager.WakeLock mProximityWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        myProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        PowerManager powerManager = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
        mProximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));
        NotificationCenter.getInstance().addObserver(this, CallActivity.CALL_HANGUP);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_call);

        iceConnected = false;
        signalingParameters = null;
        scalingType = ScalingType.SCALE_ASPECT_FILL;

        // Create UI controls.
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
        localRenderLayout = (PercentFrameLayout) findViewById(R.id.local_video_layout);
        remoteRenderLayout = (PercentFrameLayout) findViewById(R.id.remote_video_layout);
        callFragment = new CallFragment();
        hudFragment = new HudFragment();

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };

        localRender.setOnClickListener(listener);
        remoteRender.setOnClickListener(listener);

        // Create video renderers.
        rootEglBase = new EglBase();
        localRender.init(rootEglBase.getContext(), null);
        remoteRender.init(rootEglBase.getContext(), null);
        localRender.setZOrderMediaOverlay(true);

        updateVideoView();

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        // Get Intent parameters.
        final Intent intent = getIntent();
        Uri roomUri = intent.getData();
        if (roomUri == null) {
            logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Didn't get any URL in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String roomId = intent.getStringExtra(EXTRA_ROOMID);
        if (roomId == null || roomId.length() == 0) {
            logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        boolean loopback = false; //intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);//// TODO: 12/27/2015 add new methode for video and voice call
        peerConnectionParameters = new PeerConnectionParameters(
                intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
                loopback,
                tracing,
                intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0),
                intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0),
                intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0),
                intent.getStringExtra(EXTRA_VIDEOCODEC),
                intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false),
                intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
                intent.getStringExtra(EXTRA_AUDIOCODEC),
                intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false));
        commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
        runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

        // Create connection client and connection parameters.
        appRtcClient = new WebSocketRTCClient(this, new LooperExecutor());
        roomConnectionParameters = new RoomConnectionParameters(
                roomUri.toString(), roomId, loopback);

        // Send intent arguments to fragments.
        callFragment.setArguments(intent.getExtras());

        hudFragment.setArguments(intent.getExtras());
        // Activate call and HUD fragments and start the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.add(R.id.call_fragment_container, callFragment);
        ft.add(R.id.hud_fragment_container, hudFragment);
        ft.commitAllowingStateLoss();

        //  if (true){return;}
        Received = intent.getBooleanExtra(EXTRA_RECEIVED, false);

        if (Received) {
            callFragment.updateUiCallingState(false);
        } else {
            callFragment.updateUiCallingState(true);
            startCall();
        }

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }

        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(
                CallActivity.this, peerConnectionParameters, CallActivity.this);
    }

    // Activity interfaces
    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        activityRunning = false;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, myProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        activityRunning = true;
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource(); //// TODO: 12/27/2015  add pause video or stop feature
        }
    }

    @Override
    protected void onDestroy() {

        disconnect();

        if (logToast != null) {
            logToast.cancel();
        }

        activityRunning = false;
        rootEglBase.release();
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        WebRtcPhone.getInstance().AnswerCall();
        disconnect();

        ToneGenerator tg2 = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
        tg2.startTone(ToneGenerator.TONE_PROP_BEEP2);
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public void onCalling() {

    }

    @Override
    public void onCallAnswer() {
        startCall();
        callFragment.hideCallerId();
        // if(WebRtcPhone.getInstance().tg != null){
        //   WebRtcPhone.getInstance().tg.stopTone();
        //   WebRtcPhone.getInstance().tg.release();
        // }
        WebRtcPhone.getInstance().AnswerCall();
    }

    @Override
    public void onRinging() {

    }

    @Override
    public void onMuteChange() {

    }

    @Override
    public void onSpeakerChange() {

    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void updateVideoView() {
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRender.setScalingType(scalingType);
        remoteRender.setMirror(false);

        if (iceConnected) {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            localRender.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        } else {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(true);

        localRender.requestLayout();
        remoteRender.requestLayout();
    }

    private void startCall() {

        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }

        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast(getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
        // callFragment.hideCallerId();
        // inputMediaStream.audioTracks.getFirst().setEnabled(false);


    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        callFragment.hideCallerId();
        WebRtcPhone.getInstance().tg.stopTone();
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (localRender != null) {
            localRender.release();
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.release();
            remoteRender = null;
        }
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            FileLog.d(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            disconnect();
            FileLog.d(TAG, errorMessage);
      /*new AlertDialog.Builder(this)
          .setTitle(getText(R.string.channel_error_title))
          .setMessage(errorMessage)
          .setCancelable(false)
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
              dialog.cancel();
              disconnect();
            }
          }).create().show();*/
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        FileLog.d(TAG, msg);
   /* if (logToast != null) {
      logToast.cancel();
    }
    logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
    logToast.show();*/
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        peerConnectionClient.createPeerConnection(rootEglBase.getContext(),
                localRender, remoteRender, signalingParameters);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                peerConnectionClient.setRemoteDescription(sdp);
                if (!signalingParameters.initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG,
                            "Received ICE candidate for non-initilized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                    if (signalingParameters.initiator) {
                        appRtcClient.sendOfferSdp(sdp);
                    } else {
                        appRtcClient.sendAnswerSdp(sdp);
                    }
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
                iceConnected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError && iceConnected) {
                    hudFragment.updateEncoderStatistics(reports);
                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == CALL_HANGUP) {
            disconnect();
            WebRtcPhone.getInstance().AnswerCall();

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] == 0) {
            /*  FileLog.d("onSensorChanged", "distance:" + event.values[0] + "");
              params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
              params.screenBrightness = 0;
              getWindow().setAttributes(params);*/
                if (!mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.acquire();
                }
            } else {
                if (mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.release();
                }
            /*params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
              params.screenBrightness = -1f;
              getWindow().setAttributes(params);*/
            }
          /*try {
            // Yeah, this is hidden field.
            //int field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
          } catch (Throwable ignored) {

          }*/
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
