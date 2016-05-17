/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.xmpp.XMPPManager;

public class SettingsChangeNameActivity extends BaseFragment {
    private EditText firstNameField;
    private EditText lastNameField;
    private EditText statusField;
    private View headerLabelView;
    private View doneButton;

    public SettingsChangeNameActivity() {
        animationType = 1;
    }

    @Override
    public boolean canApplyUpdateStatus() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinish) {
            return;
        }
        ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setSubtitle(null);

        actionBar.setCustomView(R.layout.settings_do_action_layout);
        Button cancelButton = (Button) actionBar.getCustomView().findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LaunchActivity) parentActivity).finishFragment(false);
                finishFragment();
                getFragmentManager().popBackStack();
                ((LaunchActivity) parentActivity).updateActionBar();
            }
        });
        doneButton = actionBar.getCustomView().findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firstNameField.getText().length() != 0) {
                    saveName();
                    ((LaunchActivity) parentActivity).finishFragment(false);
                    finishFragment();
                    getFragmentManager().popBackStack();
                    ((LaunchActivity) parentActivity).updateActionBar();
                }
            }
        });

        cancelButton.setText(LocaleController.getString("Cancel", R.string.Cancel));
        TextView textView = (TextView) doneButton.findViewById(R.id.done_button_text);
        textView.setText(LocaleController.getString("Done", R.string.Done));

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            firstNameField.requestFocus();
            OSUtilities.showKeyboard(firstNameField);
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != 0) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);

            anim.setAnimationListener(new Animation.AnimationListener() {

                public void onAnimationStart(Animation animation) {
                    SettingsChangeNameActivity.this.onAnimationStart();
                }

                public void onAnimationRepeat(Animation animation) {

                }

                public void onAnimationEnd(Animation animation) {
                    SettingsChangeNameActivity.this.onAnimationEnd();
                    firstNameField.requestFocus();
                    OSUtilities.showKeyboard(firstNameField);
                }
            });

            return anim;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (fragmentView == null) {
            fragmentView = inflater.inflate(R.layout.settings_change_name_layout, container, false);

            TLRPC.User user = MessagesController.getInstance().users.get(UserConfig.clientUserId);
            if (user == null) {
                user = UserConfig.currentUser;
            }

            firstNameField = (EditText) fragmentView.findViewById(R.id.first_name_field);
            firstNameField.setHint(LocaleController.getString("FirstName", R.string.FirstName));
            firstNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_NEXT) {
                        lastNameField.requestFocus();
                        lastNameField.setSelection(lastNameField.length());
                        return true;
                    }
                    return false;
                }
            });
            lastNameField = (EditText) fragmentView.findViewById(R.id.last_name_field);
            lastNameField.setHint(LocaleController.getString("LastName", R.string.LastName));
            lastNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        doneButton.performClick();
                        return true;
                    }
                    return false;
                }
            });

            statusField = (EditText) fragmentView.findViewById(R.id.last_name_field);
            if (user != null) {
                firstNameField.setText(user.first_name);
                firstNameField.setSelection(firstNameField.length());
                lastNameField.setText(user.last_name);
            }

            TextView headerLabel = (TextView) fragmentView.findViewById(R.id.settings_section_text);
            headerLabel.setText(LocaleController.getString("YourFirstNameAndLastName", R.string.YourFirstNameAndLastName));
        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    private void saveName() {

        if (UserConfig.currentUser == null || lastNameField.getText() == null || firstNameField.getText() == null || statusField.getText() == null) {
            return;
        }
        UserConfig.currentUser.first_name = firstNameField.getText().toString();
        UserConfig.currentUser.last_name = lastNameField.getText().toString();

        TLRPC.User user = MessagesController.getInstance().users.get(UserConfig.clientUserId);
        if (user != null) {
            user.first_name = UserConfig.currentUser.first_name;
            user.last_name = UserConfig.currentUser.last_name;
        }
        UserConfig.saveConfig(true);
        NotificationCenter.getInstance().postNotificationName(MessagesController.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
        XMPPManager.getInstance().setUserVCard(null);

       /* ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {

            }
        }, null, true, RPCRequest.RPCRequestClassGeneric);*/
    }
}
