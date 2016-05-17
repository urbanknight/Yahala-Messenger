/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Messenger;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.BuildVars;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.SecurePreferences;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;

import com.yahala.ui.Rows.ConnectionsManager;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.ui.Views.NotificationView;
import com.yahala.android.ConnectionState;
import com.yahala.xmpp.ContactsController;
import com.yahala.xmpp.Util.ConnectivityManagerUtil;
import com.yahala.xmpp.XMPPManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

//import com.yahala.messenger.MessagesStorage;
//import com.yahala.messenger.ConnectionsManager;
//import com.yahala.objects.MessageObject;
//import com.yahala.ui.Views.NotificationView;
//import net.hockeyapp.android.CrashManager;
//import net.hockeyapp.android.UpdateManager;

public class LaunchActivity extends ActionBarActivity implements /*OnPasswordCompleteListener,*/ NotificationCenter.NotificationCenterDelegate, MessagesActivity.MessagesActivityDelegate {
    public Menu menu;
    private boolean second = false;
    private boolean finished = false;
    private NotificationView notificationView;
    private Uri photoPath = null;
    private String videoPath = null;
    private String sendingText = null;
    private String documentPath = null;
    private Uri[] imagesPathArray = null;
    private String[] documentsPathArray = null;
    private ArrayList<TLRPC.User> contactsToSend = null;
    private int currentConnectionState;
    private View statusView;
    private LinearLayout connectionStatusLayout;
    private TextView connectionStatus;
    private View backStatusButton;
    private View statusBackground;
    private TextView statusText;
    private View containerView;
    private Boolean isVisible = false;
    private Messenger activityMessenger;
    private Animation openAnimation;
    private Animation closeAnimation;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XMPPManager.foreground = false;
        ApplicationLoader.postInitApplication();

        this.setTheme(R.style.Theme_TMessages);
        try {
            openAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in);
            closeAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_out);
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        // getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        getWindow().setFormat(PixelFormat.RGB_888);

        if (!UserConfig.clientActivated) {
            Intent intent = getIntent();
            if (intent != null && intent.getAction() != null && (Intent.ACTION_SEND.equals(intent.getAction()) || intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))) {
                finish();
                return;
            }
            Intent intent2 = new Intent(this, IntroActivity.class);
            startActivity(intent2);
            finish();
            return;
        } else {
            SecurePreferences preferences = new SecurePreferences(this, "preferences", "Blacktow@111", true);
            String locked = preferences.getString("locked", "false");
            String lockedAuthenticated = preferences.getString("clientAuthenticated", "false");
            if (locked.equals("true") && lockedAuthenticated.equals("false") && !getIntent().getAction().startsWith("com.yahala.openchat")) {
                Intent intent = getIntent();
                if (intent != null && intent.getAction() != null && (Intent.ACTION_SEND.equals(intent.getAction()) || intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))) {
                    finish();
                    return;
                }
                Intent intent2 = new Intent(this, UnlockActivity.class);
                startActivity(intent2);
                finish();
                return;
            }


            preferences.put("clientAuthenticated", "false");
            //String user = preferences.getString("userId");
        }

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            OSUtilities.statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        NotificationCenter.getInstance().postNotificationName(702, this);
        // currentConnectionState = ConnectionsManager.getInstance().connectionState;
        for (BaseFragment fragment : ApplicationLoader.fragmentsStack) {
            if (fragment.fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragment.fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragment.fragmentView);
                }
                fragment.fragmentView = null;
            }
            fragment.parentActivity = this;
        }
        setContentView(R.layout.application_layout);
        NotificationCenter.getInstance().addObserver(this, 1234);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.connectionStateDidChanged);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.currentUserPresenceDidChanged);
        //NotificationCenter.getInstance().addObserver(this, 701);
        NotificationCenter.getInstance().addObserver(this, 702);
        /* NotificationCenter.getInstance().addObserver(this, 703);*/
        //  NotificationCenter.getInstance().addObserver(this, XmppManager.connectionSuccessful);
        //  NotificationCenter.getInstance().addObserver(this, 1003);
        //  NotificationCenter.getInstance().addObserver(this, GalleryImageViewer.needShowAllMedia);

        ////getSupportActionBar().setLogo(R.drawable.ab_icon_fixed2);

       /* statusView = getLayoutInflater().inflate(R.layout.updating_state_layout, null);
        statusBackground = statusView.findViewById(R.id.back_button_background);
        backStatusButton = statusView.findViewById(R.id.back_button);
        containerView = findViewById(R.id.container);
        statusText = (TextView)statusView.findViewById(R.id.status_text);*/
        connectionStatusLayout = (LinearLayout) findViewById(R.id.connection_status_layout);
        connectionStatus = (TextView) findViewById(R.id.connection_status);
/*        statusBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ApplicationLoader.fragmentsStack.size() > 1) {
                    onBackPressed();
                }
            }
        });*/


        if (XMPPManager.getInstance().connectionState == ConnectionState.ONLINE) {
            connectionStatusLayout.setVisibility(View.GONE);

        }

        if (ApplicationLoader.fragmentsStack.isEmpty()) {
            MainActivity fragment = new MainActivity();
            fragment.onFragmentCreate();
            ApplicationLoader.fragmentsStack.add(fragment);
        }

// savedInstanceState != null ===>>> possible orientation change
       /* if (savedInstanceState != null && savedInstanceState.containsKey("StatusLayoutIsShowing")) {
               connectionStatusLayout.setVisibility(View.VISIBLE);
        } else {
               connectionStatusLayout.setVisibility(View.GONE);
        }*/
        handleIntent(getIntent(), false, savedInstanceState != null);
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        FileLog.e("onOptionsItemSelected", "itemId:" + itemId);
       // FragmentActivity inflaterActivity = parentActivity;


        switch (itemId) {

            case 16908332: {
               // LayoutInflater li = (LayoutInflater) ((LaunchActivity) parentActivity).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                LayoutInflater li =(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = li.inflate(R.layout.lock_pattern, null, false);
                final PasswordGrid passwordGrid = (PasswordGrid) view.findViewById(R.id.password_grid);
                passwordGrid.setListener(this);
                passwordGrid.setColumnCount(3);
                //  passwordGrid.setAtt();


                dialog = new Dialog(this, R.style.PatternLoackDialogStyle);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(view);

                // dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                //dialog.setContentView(R.layout.lock_pattern);
                //dialog.setTitle("Lock Pattern");
                dialog.show();
                return true;
            }
        }
        return false;
    }*/
    public void updatePresenceMenuIcon() {

        try {
            MenuItem menuTypeAvailability = menu.findItem(R.id.menu_type_availability);
            if (menuTypeAvailability != null) {
                //FileLog.e("Test updatePresenceMenuIcon","!Null");
                if (XMPPManager.getInstance().currentPresence == XMPPManager.Available) {
                    menuTypeAvailability.setIcon(R.drawable.ic_type_available);
                } else if (XMPPManager.getInstance().currentPresence == XMPPManager.Away) {
                    menuTypeAvailability.setIcon(R.drawable.ic_type_away);
                } else if (XMPPManager.getInstance().currentPresence == 5) {
                    menuTypeAvailability.setIcon(R.drawable.ic_type_unavailable);
                } else if (XMPPManager.getInstance().currentPresence == XMPPManager.DoNotDisturb) {
                    menuTypeAvailability.setIcon(R.drawable.ic_type_dns);
                } else if (XMPPManager.getInstance().currentPresence == XMPPManager.FreeToChat) {
                    menuTypeAvailability.setIcon(R.drawable.ic_type_available);
                }
                menuTypeAvailability.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked") /* set actionbar animation*/
    private void prepareForHideShowActionBar() {
        try {
            Class firstClass = getSupportActionBar().getClass();
            Class aClass = firstClass.getSuperclass();
            if (aClass == ActionBar.class) {
                Method method = firstClass.getDeclaredMethod("setShowHideAnimationEnabled", boolean.class);
                method.invoke(getSupportActionBar(), false);
            } else {
                Field field = aClass.getDeclaredField("mActionBar");
                field.setAccessible(true);
                Method method = field.get(getSupportActionBar()).getClass().getDeclaredMethod("setShowHideAnimationEnabled", boolean.class);
                method.invoke(field.get(getSupportActionBar()), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showActionBar() {
     /*   prepareForHideShowActionBar();
        getSupportActionBar().show();*/
    }

    public void hideActionBar() {
      /*  prepareForHideShowActionBar();
        getSupportActionBar().hide();*/
    }

    private void handleIntent(Intent intent, boolean isNew, boolean restore) {
        boolean pushOpened = false;

        String push_user_jid = "0";
        Integer push_chat_id = 0;
        Integer push_enc_id = 0;
        Integer open_settings = 0;

        photoPath = null;
        videoPath = null;
        sendingText = null;
        documentPath = null;
        imagesPathArray = null;
        documentsPathArray = null;
        // FileLog.e("intent.getAction()", intent.getAction().toString());
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (intent.getAction() != null && intent.getAction().startsWith("com.yahala.openchat") && !restore) {
                int chatId = intent.getIntExtra("chatId", 0);
                String userId = intent.getStringExtra("user_jid");
                int encId = intent.getIntExtra("encId", 0);
                if (chatId != 0) {
                    TLRPC.Chat chat = MessagesController.getInstance().chats.get(chatId);
                    if (chat != null) {
                        NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                        push_chat_id = chatId;
                    }
                } else if (userId != null || userId != "") {
                    TLRPC.User user = ContactsController.getInstance().friendsDict.get(userId);
                    if (user != null) {
                        FileLog.d("closeChats", "" + userId);
                        NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                        push_user_jid = userId;
                    }
                }
            }
        }

        if (push_user_jid != "0") {
            if (push_user_jid == UserConfig.currentUser.phone) {
                open_settings = 1;
            } else {
                Bundle args = new Bundle();
                args.putString("user_jid", push_user_jid);
                ChatActivity fragment = new ChatActivity();
                fragment.setArguments(args);
                if (fragment.onFragmentCreate()) {
                    pushOpened = true;
                    ApplicationLoader.fragmentsStack.add(fragment);
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "chat" + Math.random()).commitAllowingStateLoss();
                }
            }
        } else if (push_chat_id != 0) {
            Bundle args = new Bundle();
            args.putInt("chat_id", push_chat_id);
            ChatActivity fragment = new ChatActivity();
            fragment.setArguments(args);
            ApplicationLoader.fragmentsStack.add(fragment);
            fragment.onFragmentCreate();
            pushOpened = true;
        } else if (push_enc_id != 0) {
            Bundle args = new Bundle();
            args.putInt("enc_id", push_enc_id);

            ChatActivity fragment = new ChatActivity();
            fragment.setArguments(args);
            ApplicationLoader.fragmentsStack.add(fragment);
            fragment.onFragmentCreate();
            pushOpened = true;

        }
        /* if (videoPath != null || photoPathsArray != null || sendingText != null || documentsPathsArray != null || contactsToSend != null) {
            NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putString("selectAlertString", LocaleController.getString("ForwardMessagesTo", R.string.ForwardMessagesTo));
            MessagesActivity fragment = new MessagesActivity(args);
            fragment.setDelegate(this);
            presentFragment(fragment, false, true);
            pushOpened = true;
        } */

        if (open_settings != 0) {
            ApplicationLoader.fragmentsStack.clear();
            SettingsActivity fragment = new SettingsActivity();
            ApplicationLoader.fragmentsStack.add(fragment);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "settings").commitAllowingStateLoss();
            pushOpened = true;
        }
        if (!pushOpened && !isNew) {
            BaseFragment fragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, fragment.getTag()).commitAllowingStateLoss();
        }

        intent.setAction(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent, true, false);
    }

    @Override
    public void didSelectDialog(MessagesActivity messageFragment, long dialog_id) {
        if (dialog_id != 0) {
            int lower_part = (int) dialog_id;

            ChatActivity fragment = new ChatActivity();
            Bundle bundle = new Bundle();
            if (lower_part != 0) {
                if (lower_part > 0) {
                    NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                    bundle.putInt("user_id", lower_part);
                    fragment.setArguments(bundle);
                    fragment.scrollToTopOnResume = true;
                    presentFragment(fragment, "chat" + Math.random(), true, false);
                } else if (lower_part < 0) {
                    NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                    bundle.putInt("chat_id", -lower_part);
                    fragment.setArguments(bundle);
                    fragment.scrollToTopOnResume = true;
                    presentFragment(fragment, "chat" + Math.random(), true, false);
                }
            } else {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                int chat_id = (int) (dialog_id >> 32);
                bundle.putInt("enc_id", chat_id);
                fragment.setArguments(bundle);
                fragment.scrollToTopOnResume = true;
                presentFragment(fragment, "chat" + Math.random(), true, false);
            }
            if (photoPath != null) {
                // fragment.processSendingPhoto(null, photoPath);
            } else if (videoPath != null) {
                // fragment.processSendingVideo(videoPath);
            } else if (sendingText != null) {
                //fragment.processSendingText(sendingText);
            } else if (documentPath != null) {
                //  fragment.processSendingDocument(documentPath);
            } else if (imagesPathArray != null) {
                for (Uri path : imagesPathArray) {
                    // fragment.processSendingPhoto(null, path);
                }
            } else if (documentsPathArray != null) {
                for (String path : documentsPathArray) {
                    //   fragment.processSendingDocument(path);
                }
            } else if (contactsToSend != null && !contactsToSend.isEmpty()) {
                for (TLRPC.User user : contactsToSend) {
                    MessagesController.getInstance().sendMessage(user, dialog_id);
                }
            }
            photoPath = null;
            videoPath = null;
            sendingText = null;
            documentPath = null;
            imagesPathArray = null;
            documentsPathArray = null;
            contactsToSend = null;
        }
    }

    private void checkForCrashes() {
        //]CrashManager.register(this, BuildVars.HOCKEY_APP_HASH);
    }

    private void checkForUpdates() {
        if (BuildVars.DEBUG_VERSION) {
            // UpdateManager.register(this, BuildVars.HOCKEY_APP_HASH);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        XMPPManager.foreground = false;
        ConnectionsManager.lastPauseTime = System.currentTimeMillis();
        if (notificationView != null) {
            notificationView.hide(false);
        }
        View focusView = getCurrentFocus();
        if (focusView instanceof EditText) {
            focusView.clearFocus();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XMPPManager.foreground = false;
        processOnFinish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (XMPPManager.getInstance().isConnected() && UserConfig.presence == -1/*
                UserConfig.presence != XMPPManager.DoNotDisturb &&  UserConfig.presence  != XMPPManager.Away*/) {
            XMPPManager.getInstance().setPresence(XMPPManager.Away, false/*XmppManager.unavailable*/);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        XMPPManager.foreground = true;
        if (notificationView == null && getLayoutInflater() != null) {
            notificationView = (NotificationView) getLayoutInflater().inflate(R.layout.notification_layout, null);
        }
        if (XMPPManager.getInstance().connectionState == ConnectionState.ONLINE) {
            connectionStatusLayout.setVisibility(View.GONE);
            connectionStatusLayout.setAlpha(0);

        }
        fixLayout();
        //  checkForCrashes();
        //   checkForUpdates();
        ConnectionsManager.resetLastPauseTime();
        supportInvalidateOptionsMenu();
        updateActionBar();
        try {
            NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
            MessagesController.getInstance().currentPushMessage = null;
        } catch (Exception e) {
            FileLog.e("Yahala", e);
        }

        if (XMPPManager.getInstance().isConnected()) {
          /*XmppManager.getInstance().mCurrentRetryCount = 0;
            XmppManager.getInstance().maybeStartReconnect();
            checkConnectionState();*/
            FileLog.e("onResume()", XMPPManager.getInstance().getStatusName(UserConfig.presence) + "");
            if (UserConfig.presence == -1) {
                XMPPManager.getInstance().setPresence(XMPPManager.Available, false);
            } else {
                XMPPManager.getInstance().setPresence(UserConfig.presence, false);
            }
        } else {
            //XmppManager.getInstance().presenceType = XmppManager.Available;
            if (ConnectivityManagerUtil.hasDataConnection(ApplicationLoader.applicationContext)) {
                XMPPManager.getInstance().mCurrentRetryCount = 0;
                XMPPManager.getInstance().maybeStartReconnect();
                checkConnectionState();
            }
        }


    }

    private void processOnFinish() {
        if (finished) {
            return;
        }
        finished = true;
        NotificationCenter.getInstance().removeObserver(this, 1234);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.connectionStateDidChanged);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.currentUserPresenceDidChanged);
        NotificationCenter.getInstance().removeObserver(this, 702);

        if (notificationView != null) {
            notificationView.hide(false);
            notificationView.destroy();
            notificationView = null;
        }
    }


    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        OSUtilities.checkDisplaySize();


        fixLayout();
        //if(XmppManager.getInstance().connectionState== ConnectionState.ONLINE)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }

    private void fixLayout() {
        if (containerView != null) {
            ViewTreeObserver obs = containerView.getViewTreeObserver();
            obs.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    int rotation = manager.getDefaultDisplay().getRotation();

                    int height;
                    int currentActionBarHeight = getSupportActionBar().getHeight();
                    if (currentActionBarHeight != OSUtilities.dp(48) && currentActionBarHeight != OSUtilities.dp(40)) {
                        height = currentActionBarHeight;
                    } else {
                        height = OSUtilities.dp(48);
                        if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                            height = OSUtilities.dp(40);
                        }
                    }

                    if (notificationView != null) {
                        notificationView.applyOrientationPaddings(rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90, height);
                    }

                    if (Build.VERSION.SDK_INT < 16) {
                        containerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        containerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    public void checkConnectionState() {
        final Animation animScalin;
        try {
            final Animation animScalout;
            animScalin = AnimationUtils.loadAnimation(getApplicationContext(),
                    R.anim.slide_down);
            animScalin.setFillEnabled(true);
            animScalin.setFillAfter(true);
            animScalin.setFillBefore(true);
            animScalout = AnimationUtils.loadAnimation(getApplicationContext(),
                    R.anim.slide_up);
            animScalout.setFillEnabled(true);
            animScalout.setFillAfter(true);
            animScalout.setStartOffset(3000);
            connectionStatusLayout.setVisibility(View.VISIBLE);
            connectionStatusLayout.setAlpha(1);
            isVisible = true;
            animScalout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    isVisible = false;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isVisible = false;
                    connectionStatusLayout.setVisibility(View.GONE);
                    connectionStatusLayout.setAlpha(0);

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            // connectionStatusLayout.startAnimation(animScalin);
            if (XMPPManager.getInstance().connectionState == ConnectionState.ONLINE) {
                connectionStatus.setText(LocaleController.getString("WaitingForNetwork", R.string.Updating));
                connectionStatusLayout.startAnimation(animScalout);


            } else if (XMPPManager.getInstance().connectionState == ConnectionState.RECONNECT_NETWORK) {
                connectionStatusLayout.startAnimation(animScalin);
                connectionStatus.setText(LocaleController.getString("WaitingForNetwork", R.string.WaitingForNetwork));
            } else if (XMPPManager.getInstance().connectionState == ConnectionState.DISCONNECTED) {
                connectionStatusLayout.startAnimation(animScalin);
                connectionStatus.setText(LocaleController.getString("WaitingForNetwork", R.string.WaitingForNetwork));
            } else if (XMPPManager.getInstance().connectionState == ConnectionState.RECONNECT_DELAYED) {
                connectionStatusLayout.startAnimation(animScalin);
                connectionStatus.setText(LocaleController.getString("WaitingForNetwork", R.string.WaitingForNetwork));
            } else if (XMPPManager.getInstance().connectionState == ConnectionState.CONNECTING) {
                connectionStatusLayout.startAnimation(animScalin);
                connectionStatus.setText(LocaleController.getString("WaitingForNetwork", R.string.Connecting));
            }
        } catch (Exception e) {
            FileLog.e("Yahala", e);
        }
    }

    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        // System.out.println("----main activity---onStart---");
        overridePendingTransition(R.anim.scale_out_in, R.anim.scale_in_out);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == XMPPManager.connectionStateDidChanged) {
            // Toast.makeText(ApplicationLoader.applicationContext, XmppManager.getInstance().connectionState+"",Toast.LENGTH_LONG).show();
            // connectionStatusLayout.setVisibility(View.VISIBLE);

            checkConnectionState();
            // try{
            //////      // updatePresenceMenuIcon();} catch (Exception e){e.printStackTrace();
            // }

            //XmppManager.getInstance().setStatus(true ,"");
        } else if (id == 1003) {
          /*XmppManager.getInstance().setStatus(true ,"");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if(alarmSound == null){
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if(alarmSound == null){
                    alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }
            long[] vibrate = { 0, 100, 200, 300 };

            NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher) // notification icon
                    .setContentTitle((String) args[0]) // title for notification
                    .setContentText((String) args[1]) // message for notification
                    .setAutoCancel(true)// clear notification after click
                    .setSound(alarmSound)
                    .setVibrate(vibrate);
            Intent intent = new Intent(this, LaunchActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this,0,intent,Intent.FLAG_ACTIVITY_NEW_TASK);
            mBuilder.setContentIntent(pi);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());*/
        } else if (id == 1234) {
            for (BaseFragment fragment : ApplicationLoader.fragmentsStack) {
                fragment.onFragmentDestroy();
            }

            ApplicationLoader.fragmentsStack.clear();
            Intent intent2 = new Intent(this, IntroActivity.class);
            startActivity(intent2);
            processOnFinish();
            finish();
       /* } else if (id == GalleryImageViewer.needShowAllMedia) {
            long dialog_id = (Long)args[0];
            MediaActivity fragment = new MediaActivity();
            Bundle bundle = new Bundle();
            if (dialog_id != 0) {
                bundle.putLong("dialog_id", dialog_id);
                fragment.setArguments(bundle);
                presentFragment(fragment, "media_" + dialog_id, false);
            }*/
        } else if (id == 658) {
            Integer push_user_id = (Integer) NotificationCenter.getInstance().getFromMemCache("push_user_id", 0);
            Integer push_chat_id = (Integer) NotificationCenter.getInstance().getFromMemCache("push_chat_id", 0);
            Integer push_enc_id = (Integer) NotificationCenter.getInstance().getFromMemCache("push_enc_id", 0);

            if (push_user_id != 0) {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                ChatActivity fragment = new ChatActivity();
                Bundle bundle = new Bundle();
                bundle.putInt("user_id", push_user_id);
                fragment.setArguments(bundle);
                if (fragment.onFragmentCreate()) {
                    if (ApplicationLoader.fragmentsStack.size() > 0) {
                        BaseFragment lastFragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
                        lastFragment.willBeHidden();
                    }
                    ApplicationLoader.fragmentsStack.add(fragment);
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "chat" + Math.random()).commitAllowingStateLoss();
                }
            } else if (push_chat_id != 0) {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                ChatActivity fragment = new ChatActivity();
                Bundle bundle = new Bundle();
                bundle.putInt("chat_id", push_chat_id);
                fragment.setArguments(bundle);
                if (fragment.onFragmentCreate()) {
                    if (ApplicationLoader.fragmentsStack.size() > 0) {
                        BaseFragment lastFragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
                        lastFragment.willBeHidden();
                    }
                    ApplicationLoader.fragmentsStack.add(fragment);
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "chat" + Math.random()).commitAllowingStateLoss();
                }
            } else if (push_enc_id != 0) {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                ChatActivity fragment = new ChatActivity();
                Bundle bundle = new Bundle();
                bundle.putInt("enc_id", push_enc_id);
                fragment.setArguments(bundle);
                if (fragment.onFragmentCreate()) {
                    if (ApplicationLoader.fragmentsStack.size() > 0) {
                        BaseFragment lastFragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
                        lastFragment.willBeHidden();
                    }
                    ApplicationLoader.fragmentsStack.add(fragment);
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "chat" + Math.random()).commitAllowingStateLoss();
                }
            }
        } else if (id == 701) {
            if (notificationView != null) {
                MessageObject message = (MessageObject) args[0];
                notificationView.show(message);
            }
        } else if (id == 702) {
            if (args[0] != this) {
                processOnFinish();
            }
        } else if (id == 703) {
            int state = (Integer) args[0];
            if (currentConnectionState != state) {
                FileLog.e("Yahala", "switch to state " + state);
                currentConnectionState = state;
                updateActionBar();
            }
        }
    }

    public void fixBackButton() {
        if (Build.VERSION.SDK_INT == 19) {
            //workaround for back button dissapear
            try {
                Class firstClass = getSupportActionBar().getClass();
                Class aClass = firstClass.getSuperclass();
                if (aClass == ActionBar.class) {
                } else {
                    Field field = aClass.getDeclaredField("mActionBar");
                    field.setAccessible(true);
                    android.app.ActionBar bar = (android.app.ActionBar) field.get(getSupportActionBar());

                    field = bar.getClass().getDeclaredField("mActionView");
                    field.setAccessible(true);
                    View v = (View) field.get(bar);
                    aClass = v.getClass();

                    field = aClass.getDeclaredField("mHomeLayout");
                    field.setAccessible(true);
                    v = (View) field.get(v);
                    v.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        BaseFragment currentFragment = null;
        if (!ApplicationLoader.fragmentsStack.isEmpty()) {
            currentFragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
        }
        boolean canApplyLoading = true;
        if (currentFragment != null && (currentConnectionState == 0 || !currentFragment.canApplyUpdateStatus() || statusView == null)) {
            currentFragment.applySelfActionBar();
            canApplyLoading = false;
        }
        if (canApplyLoading) {
            if (statusView != null) {
                statusView.setVisibility(View.VISIBLE);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayShowHomeEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayUseLogoEnabled(false);
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setSubtitle(null);

                if (ApplicationLoader.fragmentsStack.size() > 1) {
                    backStatusButton.setVisibility(View.VISIBLE);
                    statusBackground.setEnabled(true);
                } else {
                    backStatusButton.setVisibility(View.GONE);
                    statusBackground.setEnabled(false);
                }

                if (currentConnectionState == 1) {
                    statusText.setText(getString(R.string.WaitingForNetwork));
                } else if (currentConnectionState == 2) {
                    statusText.setText(getString(R.string.Connecting));
                } else if (currentConnectionState == 3) {
                    statusText.setText(getString(R.string.Updating));
                }
                if (actionBar.getCustomView() != statusView) {
                    actionBar.setCustomView(statusView);
                }

                try {
                    if (statusView.getLayoutParams() instanceof ActionBar.LayoutParams) {
                        ActionBar.LayoutParams statusParams = (ActionBar.LayoutParams) statusView.getLayoutParams();
                        statusText.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                        statusParams.width = (statusText.getMeasuredWidth() + OSUtilities.dp(54));
                        if (statusParams.height == 0) {
                            statusParams.height = actionBar.getHeight();
                        }
                        if (statusParams.width <= 0) {
                            statusParams.width = OSUtilities.dp(100);
                        }
                        statusParams.topMargin = 0;
                        statusParams.leftMargin = 0;
                        statusView.setLayoutParams(statusParams);
                    } else if (statusView.getLayoutParams() instanceof android.app.ActionBar.LayoutParams) {
                        android.app.ActionBar.LayoutParams statusParams = (android.app.ActionBar.LayoutParams) statusView.getLayoutParams();
                        statusText.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                        statusParams.width = (statusText.getMeasuredWidth() + OSUtilities.dp(54));
                        if (statusParams.height == 0) {
                            statusParams.height = actionBar.getHeight();
                        }
                        if (statusParams.width <= 0) {
                            statusParams.width = OSUtilities.dp(100);
                        }
                        statusParams.topMargin = 0;
                        statusParams.leftMargin = 0;
                        statusView.setLayoutParams(statusParams);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void presentFragment(BaseFragment fragment, String tag, boolean bySwipe) {
        presentFragment(fragment, tag, false, bySwipe);
    }

    public void presentFragment(BaseFragment fragment, String tag, boolean removeLast, boolean bySwipe) {
        if (getCurrentFocus() != null) {
            OSUtilities.hideKeyboard(getCurrentFocus());
        }
        if (!fragment.onFragmentCreate()) {
            return;
        }
        //fragment.setParentActivity(this);
        BaseFragment current = null;
        if (!ApplicationLoader.fragmentsStack.isEmpty()) {
            current = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
        }
        if (current != null) {
            current.willBeHidden();
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fTrans = fm.beginTransaction();
        if (removeLast && current != null) {
            ApplicationLoader.fragmentsStack.remove(ApplicationLoader.fragmentsStack.size() - 1);
            current.onFragmentDestroy();
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean animations = preferences.getBoolean("view_animations", true);
        if (animations) {
            if (bySwipe) {

                fTrans.setCustomAnimations(R.anim.no_anim, R.anim.no_anim);
            } else {
                fTrans.setCustomAnimations(R.anim.scale_in_out_d, R.anim.scale_out_in_d);
                //fTrans.setCustomAnimations(R.anim.scale_in ,R.anim.scale_out);
            }
        }
        try {
            fTrans.replace(R.id.container, fragment, tag);
            fTrans.commitAllowingStateLoss();
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        ApplicationLoader.fragmentsStack.add(fragment);
    }

    public void removeFromStack(BaseFragment fragment) {
        ApplicationLoader.fragmentsStack.remove(fragment);
        fragment.onFragmentDestroy();
    }

    public void finishFragment(boolean bySwipe) {

        if (getCurrentFocus() != null) {
            OSUtilities.hideKeyboard(getCurrentFocus());
        }
        if (ApplicationLoader.fragmentsStack.size() < 2) {
            for (BaseFragment fragment : ApplicationLoader.fragmentsStack) {
                fragment.onFragmentDestroy();
            }
            ApplicationLoader.fragmentsStack.clear();
            MainActivity fragment = new MainActivity();
            fragment.onFragmentCreate();
            ApplicationLoader.fragmentsStack.add(fragment);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, "chats").commitAllowingStateLoss();
            return;
        }


        BaseFragment fragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
        fragment.onFragmentDestroy();
        BaseFragment prev = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 2);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fTrans = fm.beginTransaction();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean animations = preferences.getBoolean("view_animations", true);
        if (animations) {
            if (bySwipe) {
                fTrans.setCustomAnimations(R.anim.no_anim, R.anim.no_anim);
            } else {
                fTrans.setCustomAnimations(R.anim.scale_in_out_d_r, R.anim.scale_out_in_d_r);
            }
        }
        fTrans.replace(R.id.container, prev, prev.getTag());
        fTrans.commitAllowingStateLoss();
        ApplicationLoader.fragmentsStack.remove(ApplicationLoader.fragmentsStack.size() - 1);
    }

    @Override
    public void onBackPressed() {
        if (ApplicationLoader.fragmentsStack.size() == 1) {
            FileLog.e("Yahala", "ApplicationLoader.fragmentsStack.size() == 1");
            ApplicationLoader.fragmentsStack.get(0).onFragmentDestroy();
            ApplicationLoader.fragmentsStack.clear();
            processOnFinish();
            finish();
            return;
        }
        if (!ApplicationLoader.fragmentsStack.isEmpty()) {
            BaseFragment lastFragment = ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size() - 1);
            if (lastFragment.onBackPressed()) {
                finishFragment(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            connectionStatusLayout.setVisibility(View.GONE);
            outState.putBoolean("StatusLayoutIsShowing", isVisible);
        } catch (Exception e) {
            FileLog.e("Yahala", e);
        }
    }
   /* @Override
    public void onPasswordComplete(String s) {
        Toast.makeText(this, "Password: " + s, Toast.LENGTH_SHORT).show();
    }*/
}