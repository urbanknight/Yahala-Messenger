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

import android.app.Activity;
import android.app.Fragment;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yahala.ImageLoader.ImageLoaderInitializer;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.ContactsController;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.R;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.messenger.Utilities;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.CircleImageView;
import com.yahala.xmpp.XMPPManager;
import com.yahala.xmpp.call.CallParameter;
import com.yahala.xmpp.call.WebRtcCall;
import com.yahala.xmpp.call.WebRtcPhone;

import org.webrtc.RendererCommon.ScalingType;

import java.io.File;

/**
 * Fragment for call control.
 */
public class CallFragment extends Fragment {
    private View controlView;
    private TextView contactView;
    private CircleImageView callerPhoto;
    private RelativeLayout disconnectButton;
    private RelativeLayout answerButton;
    private RelativeLayout cameraSwitchButton;
    private RelativeLayout videoScalingButton;
    private RelativeLayout callerId;
    private ImageView scalingImage;
    private TextView captureFormatText;
    private SeekBar captureFormatSlider;
    private OnCallEvents callEvents;
    private ScalingType scalingType;
    private boolean IsAnswered = false;
    private boolean videoCallEnabled = false;

    /**
     * Call control interface for container activity.
     */
    public interface OnCallEvents {
        public void onCallHangUp();

        public void onCameraSwitch();

        public void onVideoScalingSwitch(ScalingType scalingType);

        public void onCaptureFormatChange(int width, int height, int framerate);

        public void onCalling();

        public void onCallAnswer();

        public void onRinging();

        public void onMuteChange();

        public void onSpeakerChange();
    }

    public boolean onFragmentCreate() {

        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        controlView = inflater.inflate(R.layout.fragment_call, container, false);

        // Create UI controls.
        contactView = (TextView) controlView.findViewById(R.id.contact_name_call);
        disconnectButton = (RelativeLayout) controlView.findViewById(R.id.button_call_disconnect);
        callerPhoto = (CircleImageView) controlView.findViewById(R.id.caller_photo);
        callerPhoto.setBorderColor(0x9908a3cd);
        callerPhoto.setBorderWidth(OSUtilities.dp(10L));
        answerButton = (RelativeLayout) controlView.findViewById(R.id.button_call_answer);
        cameraSwitchButton = (RelativeLayout) controlView.findViewById(R.id.button_call_switch_camera);
        videoScalingButton = (RelativeLayout) controlView.findViewById(R.id.button_call_scaling_mode);
        callerId = (RelativeLayout) controlView.findViewById(R.id.caller_id);
        scalingImage = (ImageView) controlView.findViewById(R.id.view_scaling_mode);
        captureFormatText = (TextView) controlView.findViewById(R.id.capture_format_text_call);
        captureFormatSlider = (SeekBar) controlView.findViewById(R.id.capture_format_slider_call);

        // Add buttons click events.
        if (WebRtcPhone.getInstance().IsSendRequest()) {

        }
        answerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              /*if(WebRtcPhone.getInstance().tg != null){
                  WebRtcPhone.getInstance().tg.stopTone();
                  WebRtcPhone.getInstance().tg.release();
              }*/

                //  if (!WebRtcPhone.getInstance().IsSendRequest()) {
                IsAnswered = true;
                callEvents.onCallAnswer();




           /*   } else {
                  callEvents.onCallHangUp();
                  XMPPManager.getInstance().sendMessage(WebRtcPhone.getInstance().jid, 0, new CallParameter("s", false, true));
              }*/


                answerButton.setVisibility(View.GONE);
                disconnectButton.setVisibility(View.VISIBLE);
            }
        });
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                callEvents.onCallHangUp();
                XMPPManager.getInstance().sendMessage(WebRtcPhone.getInstance().jid, 0, new CallParameter("s", false, true));
                answerButton.setVisibility(View.VISIBLE);
                disconnectButton.setVisibility(View.GONE);
            }

        });

        cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callEvents.onCameraSwitch();
            }
        });

        videoScalingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
                    scalingImage.setBackgroundResource(
                            R.drawable.ic_action_full_screen);
                    scalingType = ScalingType.SCALE_ASPECT_FIT;
                } else {
                    scalingImage.setBackgroundResource(
                            R.drawable.ic_action_return_from_full_screen);
                    scalingType = ScalingType.SCALE_ASPECT_FILL;
                }
                callEvents.onVideoScalingSwitch(scalingType);
            }
        });
        scalingType = ScalingType.SCALE_ASPECT_FILL;

        return controlView;
    }

    public void updateUiCallingState(boolean isCalling) {


    }

    public void hideCallerId() {
        callerId.setVisibility(View.INVISIBLE);
        answerButton.setVisibility(View.GONE);
        disconnectButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        try {

            boolean captureSliderEnabled = false;
            Bundle args = getArguments();

            if (args != null) {
                //FileLog.e("caller id:",WebRtcPhone.getInstance().jid);
                TLRPC.User user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(WebRtcPhone.getInstance().jid);


                boolean isCalling = args.getBoolean(CallActivity.EXTRA_RECEIVED, false);

                if (isCalling) {
                    answerButton.setVisibility(View.VISIBLE);
                    disconnectButton.setVisibility(View.VISIBLE);
                } else {
                    answerButton.setVisibility(View.GONE);
                    disconnectButton.setVisibility(View.VISIBLE);
                }

                String contactName = Utilities.formatName(user.first_name, user.last_name);
                contactView.setText(contactName);
                videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true);
                captureSliderEnabled = videoCallEnabled && args.getBoolean(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, false);
                FileLog.e("user.avatarUrl", user.avatarUrl);
                if (user.avatarUrl != null) {
                  /*ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue,200,200);

                  ImageLoaderInitializer.getImageLoader().getLoader().load();
                  Picasso.with(ApplicationLoader.applicationContext)
                          .load(user.avatarUrl)
                          .placeholder(R.drawable.user_placeholder)
                          .error(R.drawable.user_placeholder)
                          .into(callerPhoto);*/
                    ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue, 100, 100);
                    ImageTag tag = ImageLoaderInitializer.getInstance().imageTagFactory.build(user.avatarUrl, ApplicationLoader.applicationContext);
                    callerPhoto.setTag(tag);
                    ImageLoaderInitializer.getInstance().getImageLoader().getLoader().load(callerPhoto);
                    // callerPhoto.setImageBitmap(user.avatar);

                }

            }

            if (!videoCallEnabled) {
                cameraSwitchButton.setVisibility(View.INVISIBLE);
            }

            if (captureSliderEnabled) {
                captureFormatSlider.setOnSeekBarChangeListener(new CaptureQualityController(captureFormatText, callEvents));
            } else {
                captureFormatText.setVisibility(View.GONE);
                captureFormatSlider.setVisibility(View.GONE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callEvents = (OnCallEvents) activity;
    }

}
