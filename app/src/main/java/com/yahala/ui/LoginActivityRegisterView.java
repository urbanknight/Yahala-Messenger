/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.yahala.messenger.R;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.ui.Views.BackupImageView2;
import com.yahala.ui.Views.SlideView;
import com.yahala.xmpp.XMPPManager;

import com.yahala.messenger.Utilities;

//import com.yahala.messenger.ConnectionsManager;
//import com.yahala.messenger.ContactsController;
//import com.yahala.messenger.MessagesStorage;
//import com.yahala.messenger.RPCRequest;
//import com.yahala.messenger.MessagesStorage;

public class LoginActivityRegisterView extends SlideView implements NotificationCenter.NotificationCenterDelegate {
    private static TLRPC.TL_userSelf userToRegister;
    private EditText firstNameField;
    private EditText lastNameField;
    private String requestPhone;
    private String phoneHash;
    private String phoneCode;
    private BackupImageView2 avatarImage;
    //public AvatarUpdater avatarUpdater = new AvatarUpdater();
    //private TLRPC.PhotoSize avatarPhoto = null;
    //private TLRPC.PhotoSize avatarPhotoBig = null;
    private Bundle currentParams;

    public LoginActivityRegisterView(Context context) {
        super(context);
    }

    public LoginActivityRegisterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginActivityRegisterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

//        avatarUpdater.parentActivity = (Activity)delegate;
//        avatarUpdater.delegate = new AvatarUpdater.AvatarUpdaterDelegate() {
//            @Override
//            public void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big) {
//                avatarPhotoBig = big;
//                avatarPhoto = small;
//                if (avatarImage != null) {
//                    avatarImage.setImage(small.location, null, R.drawable.user_placeholder);
//                }
//            }
//        };
//        avatarUpdater.returnOnly = true;

        // ImageButton avatarButton = (ImageButton)findViewById(R.id.settings_change_avatar_button);
        firstNameField = (EditText) findViewById(R.id.login_first_name_field);
        firstNameField.setHint(LocaleController.getString("FirstName", R.string.FirstName));
        lastNameField = (EditText) findViewById(R.id.login_last_name_field);
        lastNameField.setHint(LocaleController.getString("LastName", R.string.LastName));
        avatarImage = (BackupImageView2) findViewById(R.id.settings_avatar_image);

        TextView textView = (TextView) findViewById(R.id.login_register_info);
        textView.setText(LocaleController.getString("RegisterText", R.string.RegisterText));

        TextView wrongNumber = (TextView) findViewById(R.id.changed_mind);
        wrongNumber.setText(LocaleController.getString("CancelRegistration", R.string.CancelRegistration));

        wrongNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                delegate.setPage(0, true, null, true);
            }
        });

        firstNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    lastNameField.requestFocus();
                    return true;
                }
                return false;
            }
        });

        //       avatarButton.setOnClickListener(new View.OnClickListener() {
        //          @Override
        //         public void onClick(View view) {
        //             AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//                CharSequence[] items;
//
//                if (avatarPhoto != null) {
//                    items = new CharSequence[]{getString(R.string.FromCamera), getString(R.string.FromGalley), getString(R.string.DeletePhoto)};
//                } else {
//                    items = new CharSequence[]{getString(R.string.FromCamera), getString(R.string.FromGalley)};
//                }
//
//                builder.setItems(items, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        if (i == 0) {
//                            avatarUpdater.openCamera();
//                        } else if (i == 1) {
//                            avatarUpdater.openGallery();
//                        } else if (i == 2) {
//                            resetAvatar();
//                        }
//                    }
//                });
//                builder.show().setCanceledOnTouchOutside(true);
        //         }
        //       });
    }

    public void resetAvatar() {
//        avatarPhoto = null;
//        avatarPhotoBig = null;
//        if (avatarImage != null) {
//            avatarImage.setImageResource(R.drawable.user_placeholder);
//        }
    }

    @Override
    public void onDestroyActivity() {
        super.onDestroyActivity();
//        if (avatarUpdater != null) {
//            avatarUpdater.clear();
//            avatarUpdater = null;
//        }
    }

    @Override
    public void onBackPressed() {
        currentParams = null;
    }

    @Override
    public String getHeaderName() {
        return getResources().getString(R.string.YourName);
    }

    @Override
    public void onShow() {
        super.onShow();
        if (firstNameField != null) {
            firstNameField.requestFocus();
            firstNameField.setSelection(firstNameField.length());
        }
    }

    @Override
    public void setParams(Bundle params) {
        if (params == null) {
            return;
        }
        firstNameField.setText("");
        lastNameField.setText("");
        requestPhone = params.getString("phoneFormated");
        phoneHash = params.getString("phoneHash");
        phoneCode = params.getString("code");
        currentParams = params;
        resetAvatar();
    }

    @Override
    public void onNextPressed() {
        NotificationCenter.getInstance().addObserver(this, XMPPManager.accountCreated);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.userAuthenticated);
        NotificationCenter.getInstance().addObserver(this, com.yahala.messenger.MessagesController.contactsDidLoaded);
        TLRPC.TL_auth_signUp req = new TLRPC.TL_auth_signUp();
        req.phone_code = phoneCode;
        req.phone_code_hash = phoneHash;
        req.phone_number = requestPhone;
        req.first_name = firstNameField.getText().toString();
        req.last_name = lastNameField.getText().toString();
        delegate.needShowProgress();


        //   if (error == null) {
        //    final TLRPC.TL_auth_authorization res = (TLRPC.TL_auth_authorization)response;
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                TLRPC.TL_userSelf user = new TLRPC.TL_userSelf();
                user.first_name = firstNameField.getText().toString();
                user.last_name = lastNameField.getText().toString();
                user.phone = requestPhone;

                user.jid = requestPhone;

                userToRegister = user;
                XMPPManager.getInstance().createAccount(userToRegister.phone, "admin", userToRegister);
            }
        });
          /*      } else {
                    if (delegate != null) {
                        if (error.text.contains("PHONE_NUMBER_INVALID")) {
                            delegate.needShowAlert(LocaleController.getString("InvalidPhoneNumber", R.string.InvalidPhoneNumber));
                        } else if (error.text.contains("PHONE_CODE_EMPTY") || error.text.contains("PHONE_CODE_INVALID")) {
                            delegate.needShowAlert(LocaleController.getString("InvalidCode", R.string.InvalidCode));
                        } else if (error.text.contains("PHONE_CODE_EXPIRED")) {
                            delegate.needShowAlert(LocaleController.getString("CodeExpired", R.string.CodeExpired));
                        } else if (error.text.contains("FIRSTNAME_INVALID")) {
                            delegate.needShowAlert(LocaleController.getString("InvalidFirstName", R.string.InvalidFirstName));
                        } else if (error.text.contains("LASTNAME_INVALID")) {
                            delegate.needShowAlert(LocaleController.getString("InvalidLastName", R.string.InvalidLastName));
                        } else {
                            delegate.needShowAlert(error.text);
                        }
                    }
                }
            }
        }, null, true, RPCRequest.RPCRequestClassGeneric);*/

    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, firstNameField.getText().toString(), lastNameField.getText().toString(), currentParams);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentParams = savedState.params;
        if (currentParams != null) {
            setParams(currentParams);
        }
        firstNameField.setText(savedState.firstName);
        lastNameField.setText(savedState.lastName);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == XMPPManager.accountCreated) {
            //FileLog.e("Yahala"," XmppManager.accountCreated");
            //FileLog.e("Yahala"," currentUser name : " + userToRegister.first_name);

            //UserConfig.clearConfig();
            //MessagesStorage.getInstance().cleanUp();
            //MessagesController.getInstance().cleanUp();
            //UserConfig.currentUser = userToRegister;
            //UserConfig.clientActivated = true;
            //UserConfig.clientUserJid = userToRegister.jid;
            //UserConfig.clientUserId = userToRegister.id;
            //UserConfig.saveConfig(true);
            //ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
            //users.add(userToRegister);
            // MessagesStorage.getInstance().putUsersAndChats(users, null, true, true);
            //MessagesController.getInstance().uploadAndApplyUserAvatar(avatarPhotoBig);
            //MessagesController.getInstance().users.put(userToRegister.jid, userToRegister);

            //sdasdas// //zxzxzxzxzxzx       // XmppManager.getInstance().xmppRequestStateChange(,true);

            //com.yahala.xmpp.MessagesStorage.getInstance()
            // ContactsController.getInstance().addContact(new TLRPC.User());
            // ContactsController.getInstance().checkAppAccount();


            //
        } else if (id == XMPPManager.userAuthenticated) {
            XMPPManager.getInstance().setUserVCard(null);
            com.yahala.xmpp.ContactsController.getInstance().readContacts(true);

        } else if (id == com.yahala.messenger.MessagesController.contactsDidLoaded) {
            if (delegate != null) {
                delegate.needHideProgress();
                delegate.needFinishActivity();
            }
        }
    }

    protected static class SavedState extends View.BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public String firstName;
        public String lastName;
        public Bundle params;

        private SavedState(Parcelable superState, String text1, String text2, Bundle p1) {
            super(superState);
            firstName = text1;
            lastName = text2;
            if (firstName == null) {
                firstName = "";
            }
            if (lastName == null) {
                lastName = "";
            }
            params = p1;
        }

        private SavedState(Parcel in) {
            super(in);
            firstName = in.readString();
            lastName = in.readString();
            params = in.readBundle();
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeString(firstName);
            destination.writeString(lastName);
            destination.writeBundle(params);
        }
    }
}
