package com.yahala.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asghonim.salp.OnPasswordCompleteListener;
import com.asghonim.salp.PasswordGrid;
import com.yahala.messenger.R;
import com.yahala.android.LocaleController;
import com.yahala.messenger.SecurePreferences;
import com.yahala.xmpp.XMPPManager;
//import com.yahala.android.LocaleController;
import com.yahala.messenger.Utilities;

import java.util.ArrayList;

/**
 * Created by user on 7/12/2014.
 */
public class PasswordActivity extends RelativeLayout implements OnPasswordCompleteListener {
    private String newPattern;
    private String currentPattern;
    SecurePreferences preferences;
    // private String confirmationPattern;
    private PasswordGrid passwordGrid;
    private TextView mCancelButton;
    private TextView mDoneButton;
    private TextView mDescription;
    private ImageView mIcon;
    private LinearLayout mActionButtons;
    private LinearLayout cancelButtonContainer;
    private TextView mTitle;
    private boolean isDialog = true;
    private int minCount = 4;
    private int maxTryCount = 4;
    PasswordDelegate delegate;
    CurrentStep currentStep = CurrentStep.START;

    public static interface PasswordDelegate {
        public abstract void onPasswordComplete(ArrayList<String> photos);

        public abstract void needFinish();

        public abstract void onContinueClicked();
    }

    public enum CurrentStep {
        START,
        CREATE_LOCK_PATTERN,
        PATTERN_RECORDED,
        CONFIRM_LOCK_PATTERN,
        LOCK_PATTERN_RESET,
        FINAL_PATTERN,
        LOCKED
    }

    public PasswordActivity(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public PasswordActivity(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PasswordActivity(Context context, AttributeSet attrs, Boolean isDialog) {
        super(context, attrs);
        this.isDialog = isDialog;
        init(context);
    }

    public PasswordActivity(Context context) {
        super(context);
        init(context);
    }

    private void init(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        final View lockPattern;
        preferences = new SecurePreferences(context, "preferences", "Blacktow@111", true);
        String locked = preferences.getString("locked", "false");


        if (isDialog) {
            lockPattern = inflater.inflate(R.layout.lock_pattern_d, null, false);
            mTitle = (TextView) lockPattern.findViewById(R.id.title);
            mDescription = (TextView) lockPattern.findViewById(R.id.description);

            mTitle.setText(LocaleController.getString("Lock", R.string.Lock));
            cancelButtonContainer = (LinearLayout) lockPattern.findViewById(R.id.cancel_button_container);
            mCancelButton = (TextView) lockPattern.findViewById(R.id.cancel_button);
            mDoneButton = (TextView) lockPattern.findViewById(R.id.done_button);
            mIcon = (ImageView) lockPattern.findViewById(R.id.icon);
            if (locked.equals("true")) {
                currentStep = CurrentStep.LOCKED;
                mIcon.setVisibility(VISIBLE);
                cancelButtonContainer.setVisibility(VISIBLE);
                mDoneButton.setText(LocaleController.getString("resetLock", R.string.reset));
                mTitle.setText(LocaleController.getString("resetLock", R.string.ResetLock));
            }
            mDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    delegate.onContinueClicked();
                    if (currentStep.equals(CurrentStep.START)) {
                        currentStep = CurrentStep.CREATE_LOCK_PATTERN;
                        cancelButtonContainer.setVisibility(VISIBLE);
                        passwordGrid.setVisibility(VISIBLE);
                        mTitle.setVisibility(GONE);
                        mIcon.setVisibility(GONE);
                        mDoneButton.setEnabled(false);
                        mDescription.setText(LocaleController.getString("DrawPattern", R.string.DrawPattern));
                    } else if (currentStep.equals(CurrentStep.CREATE_LOCK_PATTERN)) {
                        currentStep = CurrentStep.CONFIRM_LOCK_PATTERN;
                        mDoneButton.setEnabled(false);
                        mCancelButton.setText(LocaleController.getString("Retry", R.string.Retry));
                        mDescription.setText(LocaleController.getString("PatternConfirm", R.string.PatternConfirm));

                        passwordGrid.Reset();
                    } else if (currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN)) {
                        currentStep = CurrentStep.FINAL_PATTERN;
                        mDoneButton.setEnabled(false);
                        mDoneButton.setText(LocaleController.getString("Confirm", R.string.Confirm));
                        mCancelButton.setText(LocaleController.getString("Retry", R.string.Cancel));
                        mDescription.setText(LocaleController.getString("PatternConfirm", R.string.YourNewPattern));


                    } else if (currentStep.equals(CurrentStep.FINAL_PATTERN)) {

                        preferences.put("lockPattern", newPattern);
                        preferences.put("locked", "true");

                        delegate.needFinish();
                    } else if (currentStep.equals(CurrentStep.LOCKED)) {
                        currentStep = CurrentStep.LOCK_PATTERN_RESET;
                        mDescription.setText(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                        passwordGrid.setVisibility(GONE);
                        mDoneButton.setEnabled(true);
                        mDoneButton.setText(LocaleController.getString("Continue", R.string.Continue));
                        // currentStep = CurrentStep.CREATE_LOCK_PATTERN;
                        //  mDoneButton.setEnabled(false);
                    } else if (currentStep.equals(CurrentStep.LOCK_PATTERN_RESET)) {
                        passwordGrid.setVisibility(VISIBLE);
                        mTitle.setVisibility(GONE);
                        mIcon.setVisibility(GONE);
                        mDescription.setText(LocaleController.getString("Unlock", R.string.Unlock));
                      /*  preferences.put("lockPattern", "");
                        preferences.put("locked", "false");

                        mDoneButton.setEnabled(false);
                        passwordGrid.setVisibility(GONE);
                         mTitle.setVisibility(GONE);

                        delegate.needFinish();
                        mDescription.setText(LocaleController.getString("DrawPattern",R.string.DrawPattern));*/
                    }
                }
            });
            mCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN) || currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN)) {
                        passwordGrid.Reset();
                        currentStep = CurrentStep.CREATE_LOCK_PATTERN;
                        mCancelButton.setText(LocaleController.getString("Retry", R.string.Cancel));
                        mDescription.setText(LocaleController.getString("DrawPattern", R.string.DrawPattern));
                        mDoneButton.setText(LocaleController.getString("Confirm", R.string.Continue));
                    } else {
                        delegate.needFinish();
                    }

                }
            });
        } else {
            lockPattern = inflater.inflate(R.layout.lock_pattern, null, false);
            mTitle = (TextView) lockPattern.findViewById(R.id.title);
            mDescription = (TextView) lockPattern.findViewById(R.id.description);
            mActionButtons = (LinearLayout) lockPattern.findViewById(R.id.action_buttons);
            mTitle.setVisibility(GONE);
            mActionButtons.setVisibility(GONE);
            mDescription.setText(LocaleController.getString("Unlock", R.string.Unlock));
        }

        passwordGrid = (PasswordGrid) lockPattern.findViewById(R.id.password_grid);
        passwordGrid.setListener(PasswordActivity.this);
        // passwordGrid.setColumnCount(3);
        if (!isDialog) {
            passwordGrid.setVisibility(VISIBLE);
        }
        addView(lockPattern);
    }


    public void setNewUpdateImageVisibility(int visibility) {

    }

    public void setImageResource(int rid) {
        String availableUri = "drawable://" + rid;

        // imageView.setImageResource(rid);
    }

    public void setDelegate(PasswordDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onPasswordComplete(String s) {
        if (isDialog) {
            mCancelButton.setEnabled(true);
            if (s.length() < minCount) {
                mDescription.setText(LocaleController.getString("MinDotsToConnect", R.string.MinDotsToConnect));
                passwordGrid.Reset();
                if (currentStep.equals(CurrentStep.FINAL_PATTERN)) {
                    currentStep = CurrentStep.CONFIRM_LOCK_PATTERN;
                }
                XMPPManager.scheduledTaskQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN)) {
                                    mDescription.setText(LocaleController.getString("DrawPattern", R.string.PatternConfirm));
                                } else {
                                    mDescription.setText(LocaleController.getString("DrawPattern", R.string.DrawPattern));
                                }
                            }
                        });

                    }
                }, 1000);
                mDoneButton.setEnabled(false);
                return;
            }
            if ((currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN) || currentStep.equals(CurrentStep.FINAL_PATTERN)) && !newPattern.equalsIgnoreCase(s)) {
                mDescription.setText(LocaleController.getString("PatternMismatched", R.string.PatternMismatched));
                currentStep = CurrentStep.CONFIRM_LOCK_PATTERN;

                mDoneButton.setEnabled(false);
                passwordGrid.Reset();
            } else if (currentStep.equals(CurrentStep.CONFIRM_LOCK_PATTERN) && newPattern.equalsIgnoreCase(s)) {
                currentStep = CurrentStep.FINAL_PATTERN;
                mDoneButton.setEnabled(true);
                mDoneButton.setText(LocaleController.getString("Confirm", R.string.Confirm));
                mCancelButton.setText(LocaleController.getString("Retry", R.string.Cancel));
                mDescription.setText(LocaleController.getString("PatternConfirm", R.string.YourNewPattern));
            } else if (currentStep.equals(CurrentStep.LOCK_PATTERN_RESET)) {
                if (!s.equals(preferences.getString("lockPattern", ""))) {
                    passwordGrid.Reset();
                    mDescription.setText(LocaleController.getString("InvalidPattern", R.string.InvalidPattern));
                } else {
                    preferences.put("lockPattern", "");
                    preferences.put("locked", "false");

                    //mDoneButton.setEnabled(false);
                    // passwordGrid.setVisibility(GONE);
                    // mTitle.setVisibility(GONE);

                    delegate.needFinish();
                    //mDescription.setText(LocaleController.getString("DrawPattern",R.string.DrawPattern));
                }
            } else {
                newPattern = s;
                mDoneButton.setEnabled(true);

                mDescription.setText(LocaleController.getString("PatternRecorded", R.string.PatternRecorded));

            }
        } else {

            if (!s.equals(preferences.getString("lockPattern", ""))) {
                passwordGrid.Reset();
                mDescription.setText(LocaleController.getString("InvalidPattern", R.string.InvalidPattern));
            } else {
                preferences.put("clientAuthenticated", "true");
                delegate.needFinish();
            }
        }
        // Toast.makeText(parentActivity, "Password: " + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDragStart() {
        if (isDialog) {
            mCancelButton.setEnabled(false);
            mDoneButton.setEnabled(false);
        }
        mDescription.setText(LocaleController.getString("ReleaseFinger", R.string.ReleaseFinger));
    }
}


