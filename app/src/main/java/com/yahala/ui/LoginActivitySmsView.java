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
import android.text.Html;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.android.OSUtilities;
import com.yahala.android.Utils;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.ui.Views.SlideView;

import com.yahala.messenger.Utilities;
import com.yahala.xmpp.FileOperation;
import com.yahala.xmpp.MySSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.security.KeyStore;
import java.util.Timer;
import java.util.TimerTask;

/*import com.yahala.messenger.ConnectionsManager;
import com.yahala.messenger.ContactsController;*/
//import com.yahala.messenger.MessagesStorage;
//import com.yahala.messenger.MessagesStorage;
//import com.yahala.RPCRequest;
//import org.telegram.messenger.TLRPC;

public class LoginActivitySmsView extends SlideView implements NotificationCenter.NotificationCenterDelegate {
    private final Integer timerSync = 1;
    private String phoneHash;
    private String requestPhone;
    private String registered;
    private EditText codeField;
    private TextView confirmTextView;
    private TextView timeText;
    private Bundle currentParams;
    private Timer timeTimer;
    private int time = 60000;
    private double lastCurrentTime;
    private boolean waitingForSms = false;

    public LoginActivitySmsView(Context context) {
        super(context);
    }

    public LoginActivitySmsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginActivitySmsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        confirmTextView = (TextView) findViewById(R.id.login_sms_confirm_text);
        codeField = (EditText) findViewById(R.id.login_sms_code_field);
        codeField.setHint(LocaleController.getString("Code", R.string.Code));
        timeText = (TextView) findViewById(R.id.login_time_text);
        TextView wrongNumber = (TextView) findViewById(R.id.wrong_number);
        wrongNumber.setText(LocaleController.getString("WrongNumber", R.string.WrongNumber));

        wrongNumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                delegate.setPage(0, true, null, true);
            }
        });

        codeField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    if (delegate != null) {
                        delegate.onNextAction();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public String getHeaderName() {
        return getResources().getString(R.string.YourCode);
    }

    @Override
    public void setParams(Bundle params) {
        codeField.setText("");
        OSUtilities.setWaitingForSms(true);
        NotificationCenter.getInstance().addObserver(this, 998);
        currentParams = params;
        waitingForSms = true;
        String phone = params.getString("phone");
        requestPhone = params.getString("phoneFormated");
        phoneHash = params.getString("phoneHash");

        registered = params.getString("registered");
        FileLog.e("yahala", requestPhone + "  phoneHash:" + phoneHash + "registered" + params.getString("registered"));
        time = params.getInt("calltime");

        String number = PhoneFormat.getInstance().format(phone);
        confirmTextView.setText(Html.fromHtml(String.format(ApplicationLoader.applicationContext.getResources().getString(R.string.SentSmsCode) + " <b>%s</b>", number)));

        OSUtilities.showKeyboard(codeField);
        codeField.requestFocus();

        try {
            synchronized (timerSync) {
                if (timeTimer != null) {
                    timeTimer.cancel();
                    timeTimer = null;
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        timeText.setText(String.format("%s 1:00", ApplicationLoader.applicationContext.getResources().getString(R.string.CallText)));
        lastCurrentTime = System.currentTimeMillis();
        timeTimer = new Timer();
        timeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                double currentTime = System.currentTimeMillis();
                double diff = currentTime - lastCurrentTime;
                time -= diff;
                lastCurrentTime = currentTime;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (time >= 1000) {
                            int minutes = time / 1000 / 60;
                            int seconds = time / 1000 - minutes * 60;
                            timeText.setText(String.format("%s %d:%02d", ApplicationLoader.applicationContext.getResources().getString(R.string.CallText), minutes, seconds));
                        } else {
                            timeText.setText(ApplicationLoader.applicationContext.getResources().getString(R.string.Calling));
                            synchronized (timerSync) {
                                if (timeTimer != null) {
                                    timeTimer.cancel();
                                    timeTimer = null;
                                }
                            }
                            TLRPC.TL_auth_sendCall req = new TLRPC.TL_auth_sendCall();
                            req.phone_number = requestPhone;
                            req.phone_code_hash = phoneHash;
                        }
                    }
                });
            }
        }, 0, 1000);
        sendSmsRequest();
    }

    public void sendSmsRequest() {
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpParams httpParams = new BasicHttpParams();

                    httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
                    httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

                    SchemeRegistry registry = new SchemeRegistry();

                    KeyStore trustStore = KeyStore.getInstance(KeyStore
                            .getDefaultType());
                    trustStore.load(null, null);
                    SSLSocketFactory sf = new MySSLSocketFactory(trustStore);

                    sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                    registry.register(new Scheme("http", new PlainSocketFactory(), 80));
                    registry.register(new Scheme("https", sf, 443));
                    ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);

                    HttpClient httpClient = new DefaultHttpClient(manager, httpParams);


                    // HttpGet httpget = new HttpGet("/nhttps://api.clickatell.com/http/sendmsg?user=***********&password=***********&api_id=***********&to=" + requestPhone + "&text=Your%20Yahala%20verification%20code:%20"+ phoneHash);
                    HttpGet httpget = new HttpGet("https://rest.nexmo.com/sms/json?api_key=***********&api_secret=***********=NEXMO&to=" + requestPhone + "&text=Your%20Yahala%20verification%20code:%20" + phoneHash);

                    if (!Utils.IsInDebugMode()) {
                        HttpResponse response = httpClient.execute(httpget);
                        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            FileLog.e("yahala", "Sms sent" + response.getEntity().getContent());
                        } else {
                            FileLog.e("/nyahala", "https://api.clickatell.com/http/sendmsg?user=***********&password=***********&api_id=***********&to=" + requestPhone + "&text=Your%20Yahala%20verification%20code:%20" + phoneHash);
                        }

                    } else {
                        Toast.makeText(getContext(), phoneHash, Toast.LENGTH_LONG);
                        FileLog.e("/nyahala", "https://api.clickatell.com/http/sendmsg?user=***********&password=***********&api_id=***********&to=" + requestPhone + "&text=Your%20Yahala%20verification%20code:%20" + phoneHash);

                    }

                } catch (Exception e) {

                    FileLog.e("yahala", e);

                }
            }
        });
    }

    @Override
    public void onNextPressed() {
        FileLog.e("yahala", "onNextPressed sms presses");
        waitingForSms = true;
        OSUtilities.setWaitingForSms(true);
        NotificationCenter.getInstance().removeObserver(this, 998);
        final TLRPC.TL_auth_signIn req = new TLRPC.TL_auth_signIn();
        req.phone_number = requestPhone;
        req.phone_code = codeField.getText().toString();
        req.phone_code_hash = phoneHash;
        try {
            synchronized (timerSync) {
                if (timeTimer != null) {
                    timeTimer.cancel();
                    timeTimer = null;
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        if (delegate != null) {
            delegate.needShowProgress();
        }
        /* start test*/
        if (delegate != null) {
            delegate.needHideProgress();
        }
        try {
            synchronized (timerSync) {
                if (timeTimer != null) {
                    timeTimer.cancel();
                    timeTimer = null;
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
      /*UserConfig.clearConfig();
        MessagesStorage.getInstance().cleanUp();
        MessagesController.get
        Instance().cleanUp();
        TLRPC.User user=new TLRPC.User();
        user.id=13;
        user.phone=requestPhone;
        UserConfig.currentUser = user;
        UserConfig.clientActivated = true;
        UserConfig.clientUserId = user.id;
        UserConfig.saveConfig(true);
        ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
        users.add(UserConfig.currentUser);
        MessagesStorage.getInstance().putUsersAndChats(users, null, true, true);
        MessagesController.getInstance().users.put(user.id, user);
        ContactsController.getInstance().checkAppAccount();
        if (delegate != null) {
            delegate.needFinishActivity();
        }*/
        if (codeField.getText().toString().equals(phoneHash)) {
            delegate.needHideProgress();
            Bundle params = new Bundle();
            params.putString("phoneFormated", requestPhone);
            params.putString("phoneHash", phoneHash);
            params.putString("code", req.phone_code);
            delegate.setPage(2, true, params, false);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            synchronized (timerSync) {
                if (timeTimer != null) {
                    timeTimer.cancel();
                    timeTimer = null;
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        currentParams = null;
        OSUtilities.setWaitingForSms(false);
        NotificationCenter.getInstance().removeObserver(this, 998);
        waitingForSms = false;
    }

    @Override
    public void onDestroyActivity() {
        super.onDestroyActivity();
        OSUtilities.setWaitingForSms(false);
        NotificationCenter.getInstance().removeObserver(this, 998);
        try {
            synchronized (timerSync) {
                if (timeTimer != null) {
                    timeTimer.cancel();
                    timeTimer = null;
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        waitingForSms = false;
    }

    @Override
    public void onShow() {
        super.onShow();
        if (codeField != null) {
            codeField.requestFocus();
            codeField.setSelection(codeField.length());
        }
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == 998) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (!waitingForSms) {
                        return;
                    }
                    if (codeField != null) {
                        codeField.setText("" + args[0]);
                        onNextPressed();
                    }
                }
            });
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, currentParams);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentParams = savedState.params;
        if (currentParams != null) {
            setParams(currentParams);
        }
    }

    protected static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public Bundle params;

        private SavedState(Parcelable superState, Bundle p1) {
            super(superState);
            params = p1;
        }

        private SavedState(Parcel in) {
            super(in);
            params = in.readBundle();
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeBundle(params);
        }
    }
}