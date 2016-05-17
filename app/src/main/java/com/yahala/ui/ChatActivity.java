package com.yahala.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yahala.ImageLoader.ImageLoaderInitializer;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.SQLite.Messages;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;
import com.yahala.objects.PhotoObject;
import com.yahala.sip.SIPManager;
import com.yahala.ui.Rows.ChatAudioCell;
import com.yahala.ui.Rows.ChatBaseCell;
import com.yahala.ui.Rows.ChatMediaCell;
import com.yahala.ui.Rows.ChatMessageCell;
import com.yahala.ui.Rows.ConnectionsManager;
import com.yahala.ui.Views.BackupImageView;
import com.yahala.ui.Views.BackupImageView2;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.ui.Views.ChatActivityEnterView;
import com.yahala.ui.Views.ImageReceiver;
import com.yahala.ui.Views.LayoutListView;

import com.yahala.ui.Views.MessageActionLayout;
import com.yahala.ui.Views.SizeNotifierRelativeLayout;
import com.yahala.xmpp.ContactsController;
import com.yahala.xmpp.FileLoader;
import com.yahala.xmpp.MessagesController;
import com.yahala.xmpp.MessagesStorage;
import com.yahala.xmpp.XMPPManager;
import com.yahala.xmpp.YHC;

import org.jivesoftware.smackx.chatstates.ChatState;

import com.yahala.android.MediaController;
import com.yahala.messenger.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Wael a on 25/03/14.
 */
public class ChatActivity extends BaseFragment implements
        PhotoViewer.PhotoViewerProvider, PhotoPickerActivity.PhotoPickerActivityDelegate,
        NotificationCenter.NotificationCenterDelegate, MessagesActivity.MessagesActivityDelegate,
        DocumentSelectActivity.DocumentSelectActivityDelegate, VideoEditorActivity.VideoEditorActivityDelegate

{
    private ChatActivityEnterView chatActivityEnterView;
    public boolean scrollToTopOnResume = false;
    AlertDialog visibleDialog = null;
    boolean first = true;
    ActionMode mActionMode = null;
    boolean firsts = true;
    private LayoutListView chatListView;
    private CircleImageView avatarImageView;
    private YHC.Chat currentChat;
    private TLRPC.User currentUser;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            menu.clear();
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.messages_full_menu, menu);

            menu.findItem(R.id.copy).setVisible(selectedMessagesCanCopyIds.size() != 0);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.copy: {
                    String str = "";
                    ArrayList<Integer> ids = new ArrayList<Integer>(selectedMessagesCanCopyIds.keySet());

                    Collections.sort(ids);

                    for (Integer id : ids) {
                        MessageObject messageObject = selectedMessagesCanCopyIds.get(id);
                        if (str.length() != 0) {
                            str += "\n";
                        }
                        str += messageObject.messageOwner.getMessage();
                    }
                    if (str.length() != 0) {
                        if (android.os.Build.VERSION.SDK_INT < 11) {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(str);
                        } else {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("label", str);
                            clipboard.setPrimaryClip(clip);
                        }
                    }
                    break;
                }
                case R.id.delete: {
                    ArrayList<Integer> ids = new ArrayList<Integer>(selectedMessagesIds.keySet());
                    MessagesController.getInstance().deleteMessages(ids);
                    break;
                }
               /* case R.id.forward: {
                    MessagesActivity fragment = new MessagesActivity();
                    fragment.selectAlertString = R.string.ForwardMessagesTo;
                    fragment.selectAlertStringDesc = "ForwardMessagesTo";
                    fragment.animationType = 1;
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putBoolean("serverOnly", true);
                    fragment.setArguments(args);
                    fragment.delegate = ChatAct.this;
                    ((LaunchActivity)parentActivity).presentFragment(fragment, "select_chat", false);
                    break;
                }*/
            }
            actionMode.finish();
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            updateVisibleRows();
        }
    };

    private boolean keyboardVisible;
    private int keyboardHeight = 0;
    private int keyboardHeightLand = 0;
    private View topPanel;
    private View secretChatPlaceholder;

    private View progressView;
    private boolean ignoreTextChange = false;
    private TextView emptyView;
    private View bottomOverlay;
    private View recordPanel;
    private TextView recordTimeText;
    private TextView bottomOverlayText;
    private ImageButton audioSendButton;
    private MessageObject selectedObject;
    private MessageObject forwardingMessage;
    private TextView secretViewStatusTextView;
    // private TimerButton timerButton;
    private Point displaySize = new Point();
    private boolean paused = true;
    private boolean readWhenResume = false;
    private boolean sendByEnter = false;
    private int readWithDate = 0;
    private int readWithMid = 0;
    private boolean swipeOpening = false;
    private boolean scrollToTopUnReadOnResume = false;
    private boolean isCustomTheme = false;
    private int downloadPhotos = 0;
    private int downloadAudios = 0;
    private ImageView topPlaneClose;
    private View pagedownButton;
    private TextView topPanelText;
    private String dialog_id;
    private SizeNotifierRelativeLayout sizeNotifierRelativeLayout;
    private HashMap<Integer, MessageObject> selectedMessagesIds = new HashMap<Integer, MessageObject>();
    private HashMap<Integer, MessageObject> selectedMessagesCanCopyIds = new HashMap<Integer, MessageObject>();
    private HashMap<Integer, MessageObject> messagesDict = new HashMap<Integer, MessageObject>();
    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap<String, ArrayList<MessageObject>>();
    private ArrayList<MessageObject> messages = new ArrayList<MessageObject>();
    private long maxMessageId = Long.MAX_VALUE;
    private long minMessageId = Long.MIN_VALUE;
    private Date maxDate = new Date(Long.MIN_VALUE);
    private boolean endReached = false;
    private boolean loading = false;
    private boolean cacheEndReached = false;
    private long lastTypingTimeSend = 0;
    private Date minDate = new Date();
    private int progressTag = 0;
    private boolean invalidateAfterAnimation = false;
    private int unread_to_load = 0;
    private int first_unread_id = 0;
    private int last_unread_id = 0;
    private boolean unread_end_reached = true;
    private boolean loadingForward = false;
    private MessageObject unreadMessageObject = null;
    private boolean recordingAudio = false;
    private String lastTimeString = null;
    private float startedDraggingX = -1;
    private float distCanMove = OSUtilities.dp(80);
    private PowerManager.WakeLock mWakeLock = null;
    private int prevOrientation = -10;
    private ChatAdapter chatAdapter;
    private String currentPicturePath;

    private TLRPC.ChatParticipants info = null;
    private int onlineCount = -1;
    private boolean isComposing = false;
    private HashMap<String, ProgressBar> progressBarMap = new HashMap<String, ProgressBar>();
    private HashMap<String, ArrayList<ProgressBar>> loadingFile = new HashMap<String, ArrayList<ProgressBar>>();
    private HashMap<Integer, String> progressByTag = new HashMap<Integer, String>();
    private CharSequence lastPrintString;
    private HashMap<String, TLRPC.User> users = new HashMap<String, TLRPC.User>();
    private Toolbar mToolbar;

    public static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targtetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targtetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targtetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        // SIPManager.getInstance().CreateProfile();
        scrollToTopOnResume = getArguments().getBoolean("scrollToTopOnResume", false);

       /* final Semaphore semaphore = new Semaphore(0);
        try {
            semaphore.acquire();
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }*/
        //  MessagesStorage.getInstance().storageQueue.postRunnable(new Runnable() {
        //      @Override
        //     public void run() {
        currentUser = ContactsController.getInstance().friendsDict.get(getArguments().getString("user_jid", ""));
        dialog_id = currentUser.jid;
        users.put(currentUser.jid, currentUser);
        //    semaphore.release();
        //       }
        //  });


        MessagesController.getInstance().users.put(currentUser.jid, currentUser);

        chatActivityEnterView = new ChatActivityEnterView();
        chatActivityEnterView.setDialogId(dialog_id);
        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {

            @Override
            public void onMessageSend() {

            }

            @Override
            public void needSendTyping() {

            }

        });

        NotificationCenter.getInstance().addObserver(this, 1010);
        NotificationCenter.getInstance().addObserver(this, 1003);
        NotificationCenter.getInstance().addObserver(this, 997);//user location
        NotificationCenter.getInstance().addObserver(this, XMPPManager.didReceivedNewMessages);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.messageReceivedByServer);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.messageReceivedByAck);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.messageSendError);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.uploadProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, MessagesController.closeChats);
        NotificationCenter.getInstance().addObserver(this, MediaController.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, MediaController.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, MediaController.recordProgressChanged);
        NotificationCenter.getInstance().addObserver(this, MessagesController.chatStateUpdated);
        NotificationCenter.getInstance().addObserver(this, MessagesController.messagesDeleted);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        sendByEnter = preferences.getBoolean("send_by_enter", false);


        try {
            if (currentUser.avatar == null) {
                currentUser.avatar = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                        R.drawable.user_blue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        loading = true;

        MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0, true, null, classGuid, true, false);

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setHasOptionsMenu(true);
        Display display = parentActivity.getWindowManager().getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT < 13) {
            displaySize.set(display.getWidth(), display.getHeight());
        } else {
            display.getSize(displaySize);
        }


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (fragmentView == null) {
            XMPPManager.getInstance().removeNotification();
            fragmentView = inflater.inflate(R.layout.chat_layout, container, false);
            fragmentView.setPadding(0, 0, 0, 0);

            mToolbar = (Toolbar) fragmentView.findViewById(R.id.toolbar);
            parentActivity.setSupportActionBar(mToolbar);

            View contentView = fragmentView.findViewById(R.id.chat_layout);
            emptyView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);
            emptyView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
            chatListView = (LayoutListView) fragmentView.findViewById(R.id.chat_list_view);
            chatListView.setAdapter(chatAdapter = new ChatAdapter(getParentActivity()));
            topPanel = fragmentView.findViewById(R.id.top_panel);
            topPlaneClose = (ImageView) fragmentView.findViewById(R.id.top_plane_close);
            topPanelText = (TextView) fragmentView.findViewById(R.id.top_panel_text);
            bottomOverlay = fragmentView.findViewById(R.id.bottom_overlay);
            bottomOverlayText = (TextView) fragmentView.findViewById(R.id.bottom_overlay_text);
            View bottomOverlayChat = fragmentView.findViewById(R.id.bottom_overlay_chat);
            progressView = fragmentView.findViewById(R.id.progressLayout);
            pagedownButton = fragmentView.findViewById(R.id.pagedown_button);

            View progressViewInner = progressView.findViewById(R.id.progressLayoutInner);


            chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
                    if (mActionMode == null) {
                        createMenu(view, false);
                    }
                    return true;
                }
            });

            chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (mActionMode != null) {
                        processRowSelect(view);
                        return;
                    }
                    createMenu(view, true);
                }
            });

            updateContactStatus();
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            int selectedBackground = preferences.getInt("selectedBackground", 1000001);
            int selectedColor = preferences.getInt("selectedColor", 0);

            if (selectedColor != 0) {
                contentView.setBackgroundColor(selectedColor);
                chatListView.setCacheColorHint(selectedColor);
            } else {
                chatListView.setCacheColorHint(0);
                try {
                    if (selectedBackground == 1000001) {
                        ((SizeNotifierRelativeLayout) contentView).setBackgroundImage(R.drawable.background_hd);
                    } else {
                        File toFile = new File(ApplicationLoader.applicationContext.getFilesDir(), "wallpaper.jpg");
                        if (toFile.exists()) {
                            if (ApplicationLoader.cachedWallpaper != null) {
                                ((SizeNotifierRelativeLayout) contentView).setBackgroundImage(ApplicationLoader.cachedWallpaper);
                            } else {
                                Drawable drawable = Drawable.createFromPath(toFile.getAbsolutePath());
                                if (drawable != null) {
                                    ((SizeNotifierRelativeLayout) contentView).setBackgroundImage(drawable);
                                    ApplicationLoader.cachedWallpaper = drawable;
                                } else {
                                    contentView.setBackgroundColor(-2693905);
                                    chatListView.setCacheColorHint(-2693905);
                                }
                            }
                            isCustomTheme = true;
                        } else {
                            ((SizeNotifierRelativeLayout) contentView).setBackgroundImage(R.drawable.background_hd);
                        }
                    }
                } catch (Exception e) {
                    contentView.setBackgroundColor(-2693905);
                    chatListView.setCacheColorHint(-2693905);
                    FileLog.e("Yahala", e);
                }
            }

            //   if (isCustomTheme) {
            progressViewInner.setBackgroundResource(R.drawable.system_loader2);
            emptyView.setBackgroundResource(R.drawable.system_black);
            // } else {
            //      progressViewInner.setBackgroundResource(R.drawable.system_loader1);
            //      emptyView.setBackgroundResource(R.drawable.system_blue);
            //  }
            emptyView.setPadding(OSUtilities.dp(7), OSUtilities.dp(1), OSUtilities.dp(7), OSUtilities.dp(1));

            if (currentUser != null && currentUser.id / 1000 == 333) {
                emptyView.setText(LocaleController.getString("GotAQuestion", R.string.GotAQuestion));
            }


            final Rect scrollRect = new Rect();

            chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (visibleItemCount > 0) {
                        if (firstVisibleItem <= 4) {// FileLog.e("Test (!cacheEndReaced && !loading)",String.valueOf(!cacheEndReaced)+" "+String.valueOf(!loading));
                            if (!cacheEndReached && !loading) {
                                if (messagesByDays.size() != 0) {
                                    //if(firsts) {
                                    MessagesController.getInstance().loadMessages(dialog_id, 0, 20, maxMessageId, !cacheEndReached, minDate, classGuid, false, false);
                                    // firsts=false;  }
                                } else {
                                    MessagesController.getInstance().loadMessages(dialog_id, 0, 20, 0, !cacheEndReached, minDate, classGuid, false, false);
                                }
                                loading = true;
                            }
                        }
                        if (firstVisibleItem + visibleItemCount >= totalItemCount - 6) {
                            if (!unread_end_reached && !loadingForward) {
                                MessagesController.getInstance().loadMessages(dialog_id, 0, 20, minMessageId, true, maxDate, classGuid, false, true);
                                loadingForward = true;
                            }
                        }
                        if (firstVisibleItem + visibleItemCount == totalItemCount && unread_end_reached) {
                            showPagedownButton(false, true);
                        }
                    } else {
                        showPagedownButton(false, false);
                    }
                    for (int a = 0; a < visibleItemCount; a++) {
                        View view = absListView.getChildAt(a);
                        if (view instanceof ChatMessageCell) {
                            // ChatMessageCell messageCell = (ChatMessageCell)view;
                            //  messageCell.getLocalVisibleRect(scrollRect);
                            // messageCell.setVisiblePart(scrollRect.top, scrollRect.bottom - scrollRect.top);
                        }
                    }
                }
            });


            TextView textView = (TextView) fragmentView.findViewById(R.id.slideToCancelTextView);
            textView.setText(LocaleController.getString("SlideToCancel", R.string.SlideToCancel));
            textView = (TextView) fragmentView.findViewById(R.id.bottom_overlay_chat_text);
            textView.setText(LocaleController.getString("DeleteThisGroup", R.string.DeleteThisGroup));


            if (loading && messages.isEmpty()) {
                progressView.setVisibility(View.VISIBLE);
                chatListView.setEmptyView(null);
            } else {
                progressView.setVisibility(View.GONE);

                chatListView.setEmptyView(emptyView);

            }

            pagedownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scrollToLastMessage();
                }
            });

            bottomOverlayChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (currentChat != null) {
                        if (getParentActivity() == null) {
                            return;
                        }
                     /*   AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MessagesController.getInstance().deleteDialog(dialog_id, 0, false);
                                finishFragment();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showAlertDialog(builder);*/
                    }
                }
            });

            chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (mActionMode != null) {
                        processRowSelect(view);
                        return;
                    }
                    createMenu(view, true);
                }
            });


            // if (currentChat != null && (currentChat instanceof TLRPC.TL_chatForbidden || currentChat.left)) {
            //     bottomOverlayChat.setVisibility(View.VISIBLE);
            // } else {
            bottomOverlayChat.setVisibility(View.GONE);

            chatActivityEnterView.setContainerView(getParentActivity(), fragmentView);
            //  }
        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }


    private void addToLoadingFile(String path, ProgressBar bar) {
        ArrayList<ProgressBar> arr = loadingFile.get(path);
        if (arr == null) {
            arr = new ArrayList<ProgressBar>();
            loadingFile.put(path, arr);
        }
        arr.add(bar);
    }

    private String getTrimmedString(String src) {
        String result = src.trim();
        if (result.length() == 0) {
            return result;
        }
        while (src.startsWith("\n")) {
            src = src.substring(1);
        }
        while (src.endsWith("\n")) {
            src = src.substring(0, src.length() - 1);
        }
        return src;
    }

    private void checkAndUpdateAvatar() {
        if (avatarImageView != null) {
            avatarImageView.setImageBitmap(currentUser.avatar);
        }
    }

    private void removeFromloadingFile(String path, ProgressBar bar) {
        ArrayList<ProgressBar> arr = loadingFile.get(path);
        if (arr != null) {
            arr.remove(bar);
        }
    }

    private View getRowParentView(View v) {
        if (v instanceof ChatBaseCell) {
            return v;
        } else {
            while (!(v.getTag() instanceof ChatListRowHolderEx)) {
                ViewParent parent = v.getParent();
                if (!(parent instanceof View)) {
                    return null;
                }
                v = (View) v.getParent();
                if (v == null) {
                    return null;
                }
            }
            return v;
        }
    }

    private void scrollToLastMessage() {
        if (unread_end_reached || first_unread_id == 0) {
            chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
        } else {
            messages.clear();
            messagesByDays.clear();
            messagesDict.clear();
            progressView.setVisibility(View.VISIBLE);
            chatListView.setEmptyView(null);

            maxMessageId = Long.MAX_VALUE;
            minMessageId = Long.MIN_VALUE;

            maxDate = new Date(Long.MIN_VALUE);
            minDate = new Date();
            unread_end_reached = true;
            MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0, true, null, classGuid, true, false);
            loading = true;

            chatAdapter.notifyDataSetChanged();

      /*  if (unread_end_reached || first_unread_id == 0) {
            chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
        } else {
            messages.clear();
            messagesByDays.clear();
            messagesDict.clear();
            progressView.setVisibility(View.VISIBLE);
            chatListView.setEmptyView(null);

            maxMessageId = Long.MAX_VALUE;
            minMessageId = Long.MIN_VALUE;

            maxDate = new Date(Long.MIN_VALUE);
            minDate = new Date();
            unread_end_reached = true;
            MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0, true, null, classGuid, true, false);
            loading = true;

            chatAdapter.notifyDataSetChanged();
        }*/

        }
    }

    public void createMenu(View v, boolean single) {
        if (mActionMode != null || parentActivity == null || getActivity() == null || isFinish || swipeOpening) {
            return;
        }
        View parentView = getRowParentView(v);
        if (parentView == null) {
            return;
        }
        MessageObject message = null;
        if (v instanceof ChatBaseCell) {
            message = ((ChatBaseCell) v).getMessageObject();
        } else {
            ChatListRowHolderEx holder = (ChatListRowHolderEx) parentView.getTag();
            message = holder.message;
        }
        final int type = getMessageType(message);

        selectedObject = null;
        selectedMessagesCanCopyIds.clear();
        selectedMessagesIds.clear();

        //  FileLog.e("createMenu","createMenu type : "+type+ "for message with type:"+message.type);

        //final int type =message.messageOwner.getType();
        if (single || type < 2) {
            if (type >= 0) {
                selectedObject = message;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                CharSequence[] items = null;


                if (type == 0) {
                    items = new CharSequence[]{LocaleController.getString("Retry", R.string.Retry), LocaleController.getString("Delete", R.string.Delete)};
                } else if (type == 1) {
                    items = new CharSequence[]{LocaleController.getString("Delete", R.string.Delete)};
                } else if (type == 2) {
                    items = new CharSequence[]{/*LocaleController.getString("Forward", R.string.Forward),*/ LocaleController.getString("Delete", R.string.Delete)};
                } else if (type == 3) {
                    items = new CharSequence[]{/*LocaleController.getString("Forward", R.string.Forward),*/ LocaleController.getString("Copy", R.string.Copy), LocaleController.getString("Delete", R.string.Delete)};
                } else if (type == 4) {
                    items = new CharSequence[]{LocaleController.getString(selectedObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument ? "SaveToDownloads" : "SaveToGallery",
                            selectedObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument ? R.string.SaveToDownloads : R.string.SaveToGallery),/* LocaleController.getString("Forward", R.string.Forward),*/ LocaleController.getString("Delete", R.string.Delete)};
                }


                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (type == 0) {
                            if (i == 0) {
                                processSelectedOption(0);
                            } else if (i == 1) {
                                processSelectedOption(1);
                            }
                        } else if (type == 1) {
                            processSelectedOption(1);
                        } else if (type == 2) {

                               /* if (i == 0) {
                                    processSelectedOption(2);
                                } else if (i == 1) {
                                    processSelectedOption(1);
                                }*/

                            processSelectedOption(1);


                        } else if (type == 3) {

                                /*if (i == 0) {
                                    processSelectedOption(2);
                                } else if (i == 1) {
                                    processSelectedOption(3);
                                } else if (i == 2) {
                                    processSelectedOption(1);
                                }*/
                            if (i == 0) {
                                processSelectedOption(3);
                            } else if (i == 1) {
                                processSelectedOption(1);
                            } else if (i == 2) {
                                processSelectedOption(1);
                            }

                        } else if (type == 4) {

                            if (i == 0) {
                                String fileName = selectedObject.messageOwner.tl_message.media.document.file_name;

                                /*if (selectedObject.type == 3) {
                                    MediaController.saveFile(fileName, selectedObject.messageOwner.tl_message.attachPath, getParentActivity(), 1, null);
                                } else if (selectedObject.type == 1) {
                                    MediaController.saveFile(fileName, selectedObject.messageOwner.tl_message.attachPath, getParentActivity(), 0, null);
                                } else* if (selectedObject.type == 8 || selectedObject.type == 9) {*/
                                MediaController.saveFile(fileName, selectedObject.messageOwner.tl_message.attachPath, getParentActivity(), 2, selectedObject.messageOwner.tl_message.media.document.file_name);
                                //}
                            } else if (i == 1) {
                                processSelectedOption(2);
                            } else if (i == 2) {
                                processSelectedOption(1);
                            }

                        }
                    }
                });

                builder.setTitle(R.string.Message);
                visibleDialog = builder.show();
                visibleDialog.setCanceledOnTouchOutside(true);

                visibleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        visibleDialog = null;
                    }
                });
            }

            return;
        }

        addToSelectedMessages(message);

        mActionMode = parentActivity.startSupportActionMode(mActionModeCallback);
        updateActionModeTitle();
        updateVisibleRows();


    }

    private void updateActionModeTitle() {
        if (mActionMode == null) {
            return;
        }
        if (selectedMessagesIds.isEmpty()) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.format("%s %d", LocaleController.getString("Selected", R.string.Selected), selectedMessagesIds.size()));
        }
    }

    private int getMessageType(MessageObject messageObject) {
        //FileLog.e("getMessageType","messageObject.messageOwner.getOut():" + messageObject.messageOwner.getOut());
        /*if (messageObject.messageOwner.getOut() != null && messageObject.messageOwner.getOut() == 1) {
            if (messageObject.messageOwner.getSend_state() == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
                return 0;
            } else {
                return -1;
            }
        } else {*/
        //  FileLog.e("","");
        if (messageObject.type == 15) {
            return -1;
        } else if (messageObject.type == 10 || messageObject.type == 11) {
            //if (messageObject.messageOwner.getJid() == "") {
            return -1;
            //}
            // return 1;
        } else {
            if (!(messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaEmpty)) {
                //FileLog.e("getMessageType", "not instanceof TLRPC.TL_messageMediaEmpty");
                if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaVideo || messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaPhoto || messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
                        /*File f = new File(Utilities.getCacheDir(), messageObject.messageOwner.tl_message.media.f.getFileName());
                        if (f.exists()) {
                            return 4;
                        }*/
                }
                return 4;
            } else {
                return 3;
            }
        }


        // }
    }

    private void addToSelectedMessages(MessageObject messageObject) {
        //  FileLog.e("messageObject.messageOwner.id", messageObject.messageOwner.id+"");
        if (selectedMessagesIds.containsKey((int) (long) messageObject.messageOwner.id)) {

            selectedMessagesIds.remove((int) (long) messageObject.messageOwner.id);
            if (messageObject.type == 0) {
                selectedMessagesCanCopyIds.remove((int) (long) messageObject.messageOwner.id);
            }
        } else {
            selectedMessagesIds.put((int) (long) messageObject.messageOwner.id, messageObject);
            if (messageObject.type == 0) {
                selectedMessagesCanCopyIds.put((int) (long) messageObject.messageOwner.id, messageObject);
            }
        }


        if (mActionMode != null && mActionMode.getMenu() != null) {
            mActionMode.getMenu().findItem(R.id.copy).setVisible(selectedMessagesCanCopyIds.size() != 0);
        }
    }

    private void processRowSelect(View view) {
        View parentView = getRowParentView(view);
        if (parentView == null) {
            return;
        }
        MessageObject message = null;
        if (view instanceof ChatBaseCell) {
            message = ((ChatBaseCell) view).getMessageObject();
        } else {
            ChatListRowHolderEx holder = (ChatListRowHolderEx) parentView.getTag();
            message = holder.message;
        }

        if (getMessageType(message) == -1) {
            return;
        }
        addToSelectedMessages(message);
        updateActionModeTitle();
        updateVisibleRows();
    }

    private void processSelectedOption(int option) {
        if (option == 0) {
            if (selectedObject != null && selectedObject.messageOwner.id < 0) {
                if (selectedObject.type == 0) {
                    if (selectedObject.messageOwner.tl_message instanceof TLRPC.TL_messageForwarded) {
                        //MessagesController.getInstance().sendMessage(selectedObject, dialog_id);
                    } else {
                        // MessagesController.getInstance().sendMessage(selectedObject.messageOwner.message, dialog_id);
                    }
                } else if (selectedObject.type == 4) {
                    // MessagesController.getInstance().sendMessage(selectedObject.messageOwner.media.geo.lat, selectedObject.messageOwner.media.geo._long, dialog_id);
                } else if (selectedObject.type == 1) {
                    if (selectedObject.messageOwner.tl_message instanceof TLRPC.TL_messageForwarded) {
                        //  MessagesController.getInstance().sendMessage(selectedObject, dialog_id);
                    } else {
                        //TLRPC.TL_photo photo = (TLRPC.TL_photo)selectedObject.messageOwner.media.photo;
                        // MessagesController.getInstance().sendMessage(photo, selectedObject.messageOwner.attachPath, dialog_id);
                    }
                } else if (selectedObject.type == 3) {
                    if (selectedObject.messageOwner.tl_message instanceof TLRPC.TL_messageForwarded) {
                        //  MessagesController.getInstance().sendMessage(selectedObject, dialog_id);
                    } else {
                        //  TLRPC.TL_video video = (TLRPC.TL_video)selectedObject.messageOwner.media.video;
                        // video.path = selectedObject.messageOwner.attachPath;
                        // MessagesController.getInstance().sendMessage(video, video.path, dialog_id);
                    }
                } else if (selectedObject.type == 12 || selectedObject.type == 13) {
                    // TLRPC.User user = MessagesController.getInstance().users.get(selectedObject.messageOwner.media.user_id);
                    // MessagesController.getInstance().sendMessage(user, dialog_id);
                } else if (selectedObject.type == 8 || selectedObject.type == 9) {
                    /// TLRPC.TL_document document = (TLRPC.TL_document)selectedObject.messageOwner.media.document;
                    //document.path = selectedObject.messageOwner.attachPath;
                    // MessagesController.getInstance().sendMessage(document, document.path, dialog_id);
                } else if (selectedObject.type == 2) {
                    // TLRPC.TL_audio audio = (TLRPC.TL_audio)selectedObject.messageOwner.media.audio;
                    ////   audio.path = selectedObject.messageOwner.attachPath;
                    //  MessagesController.getInstance().sendMessage(audio, dialog_id);
                }
                ArrayList<Integer> arr = new ArrayList<Integer>();
                arr.add(selectedObject.messageOwner.id);
                ArrayList<Long> random_ids = null;
                //  if (currentEncryptedChat != null && selectedObject.messageOwner.random_id != 0 && selectedObject.type != 10) {
                //      random_ids = new ArrayList<Long>();
                //      random_ids.add(selectedObject.messageOwner.random_id);
                // }
                //  MessagesController.getInstance().deleteMessages(arr, random_ids, currentEncryptedChat);
                chatListView.setSelection(messages.size() + 1);
            }
        } else if (option == 1) {
            if (selectedObject != null) {

                //removeUnreadPlane(true);

                ArrayList<Integer> ids = new ArrayList<Integer>();
                ids.add(selectedObject.messageOwner.id);
                MessagesController.getInstance().deleteMessages(ids);
                selectedObject = null;

            }
        } else if (option == 2) {
            if (selectedObject != null) {
               /*forwaringMessage = selectedObject;
                selectedObject = null;

                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putBoolean("serverOnly", true);
                args.putString("selectAlertString", LocaleController.getString("ForwardMessagesTo", R.string.ForwardMessagesTo));
                MessagesActivity fragment = new MessagesActivity(args);
                fragment.setDelegate(this);
                presentFragment(fragment);*/
            }
        } else if (option == 3) {
            if (selectedObject != null) {
                if (android.os.Build.VERSION.SDK_INT < 11) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(selectedObject.messageText);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("label", selectedObject.messageText);
                    clipboard.setPrimaryClip(clip);
                }
                selectedObject = null;
            }
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if (parentActivity == null) {
            return;
        }

        menu.clear();

        inflater.inflate(R.menu.chat_menu, menu);

        //SupportMenuItem timeItem = (SupportMenuItem) menu.findItem(R.id.chat_enc_timer);


        // actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));

        SupportMenuItem avatarItem = (SupportMenuItem) menu.findItem(R.id.chat_menu_avatar);
        View avatarLayout = avatarItem.getActionView();
        avatarImageView = (CircleImageView) avatarLayout.findViewById(R.id.chat_avatar_image);
        avatarImageView.setBorderWidth(OSUtilities.dp(0));

        avatarImageView.setBorderColor(0x00000000);
        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (parentActivity == null) {
                    return;
                }
                if (currentUser != null) {
                    UserProfileActivity fragment = new UserProfileActivity();
                    Bundle args = new Bundle();
                    args.putString("user_id", currentUser.jid);

                    fragment.setArguments(args);
                    ((LaunchActivity) parentActivity).presentFragment(fragment, "user_" + currentUser.jid, swipeOpening);
                } else if (currentChat != null) {
                    /*if (info != null) {
                        if (info instanceof TLRPC.TL_chatParticipantsForbidden) {
                            return;
                        }
                        NotificationCenter.getInstance().addToMemCache(5, info);
                    }
                    if (currentChat.participants_count == 0 || currentChat.left || currentChat instanceof TLRPC.TL_chatForbidden) {
                        return;
                    }
                    ChatProfileActivity fragment = new ChatProfileActivity();
                    Bundle args = new Bundle();
                    args.putInt("chat_id", currentChat.id);
                    fragment.setArguments(args);
                    ((LaunchActivity) parentActivity).presentFragment(fragment, "chat_" + currentChat.id, swipeOpening);*/
                }
            }
        });
        TLRPC.FileLocation photo = null;
        int placeHolderId = 0;
        if (currentUser != null) {
            if (currentUser.photo != null) {
                photo = currentUser.photo.photo_small;
            }
            placeHolderId = Utilities.getUserAvatarForId(currentUser.id);
        } else if (currentChat != null) {
            /*if (currentChat.photo != null) {
                photo = currentChat.photo.photo_small;
            }*/
            placeHolderId = Utilities.getGroupAvatarForId(currentChat.id);
            //      }
            //    avatarImageView.setImage(photo, "50_50",

        }
        ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue, 400, 400);
        ImageTag tag = ImageLoaderInitializer.getInstance().imageTagFactory.build(currentUser.avatarUrl, ApplicationLoader.applicationContext);
        avatarImageView.setTag(tag);
        ImageLoaderInitializer.getInstance().getImageLoader().getLoader().load(avatarImageView);
        //com.yahala.ui.lazylist.ImageLoader.getInstance().DisplayImage(currentUser.avatarUrl, avatarImageView);
        // avatarImageView.setImageBitmap(currentUser.avatar);

    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getInstance().removeObserver(this, 1010);
        NotificationCenter.getInstance().removeObserver(this, 1003);
        NotificationCenter.getInstance().removeObserver(this, 997);//user location
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.didReceivedNewMessages);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.messageReceivedByServer);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.messageReceivedByAck);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.messageSendError);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.uploadProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.closeChats);
        NotificationCenter.getInstance().removeObserver(this, MediaController.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, MediaController.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, MediaController.recordProgressChanged);
        /*NotificationCenter.getInstance().removeObserver(this, MediaController.recordStarted);
        NotificationCenter.getInstance().removeObserver(this, MediaController.recordStartError);
        NotificationCenter.getInstance().removeObserver(this, MediaController.recordStopped);*/
        NotificationCenter.getInstance().removeObserver(this, MessagesController.chatStateUpdated);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.messagesDeleted);

        if (sizeNotifierRelativeLayout != null) {
            sizeNotifierRelativeLayout.delegate = null;
            sizeNotifierRelativeLayout = null;
        }

        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }

        if (sizeNotifierRelativeLayout != null) {
            sizeNotifierRelativeLayout.delegate = null;
            sizeNotifierRelativeLayout = null;
        }

        if (mWakeLock != null) {
            try {
                mWakeLock.release();
                mWakeLock = null;
            } catch (Exception e) {
                FileLog.e("yahala", e);
            }
        }
        OSUtilities.unlockOrientation(getParentActivity());
        MediaController.getInstance().stopAudio();

    }
    /////// Emoji

    //not complete
    private void updateContactStatus() {
        if (topPanel == null) {
            return;
        }


        topPanel.setVisibility(View.GONE);
        topPanelText.setShadowLayer(1, 0, OSUtilities.dp(1), 0xff8797a3);
        if (isCustomTheme) {
            topPlaneClose.setImageResource(R.drawable.ic_msg_btn_cross_custom);
            topPanel.setBackgroundResource(R.drawable.top_pane_custom);
        } else {
            topPlaneClose.setImageResource(R.drawable.ic_msg_btn_cross_custom);
            topPanel.setBackgroundResource(R.drawable.top_pane);
        }

        topPanelText.setText(LocaleController.getString("ShareMyContactInfo", R.string.ShareMyContactInfo));
        topPlaneClose.setVisibility(View.GONE);
        topPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MessagesController.getInstance().hidenAddToContacts.put(currentUser.id, currentUser);
                topPanel.setVisibility(View.GONE);
                //  MessagesController.getInstance().sendMessage(UserConfig.currentUser, dialog_id);
                chatListView.post(new Runnable() {
                    @Override
                    public void run() {
                        // chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                    }
                });
            }
        });


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                // ((LaunchActivity) parentActivity).finish();
                // ((LaunchActivity) parentActivity).finishFragment(false);

                // ApplicationLoader.fragmentsStack.get(ApplicationLoader.fragmentsStack.size()-1).onFragmentDestroy();
                // ApplicationLoader.fragmentsStack.clear();
                finishFragment();
                /*final ActionBar actionBar = parentActivity.getSupportActionBar();
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayUseLogoEnabled(true);
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setSubtitle(null);
                //actionBar.setLogo(R.drawable.ab_icon_fixed2);
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
               // getFragmentManager().popBackStack();
               //((LaunchActivity) parentActivity).updateActionBar();
                //  parentActivity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
*/
                //((LaunchActivity) parentActivity).updateActionBar();
                break;
            case R.id.attach_photo: {
                try {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File image = Utilities.generatePicturePath();
                    if (image != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                        currentPicturePath = image.getAbsolutePath();
                    }
                    startActivityForResult(takePictureIntent, 0);
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
                break;
            }
            case R.id.attach_gallery: {
                try {
                   /* Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 1);*/
                    PhotoPickerActivity fragment = new PhotoPickerActivity();
                    fragment.setDelegate(ChatActivity.this);


                    ((LaunchActivity) parentActivity).presentFragment(fragment, "PhotoPicker", false);
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }

                /*PhotoPickerActivity fragment = new PhotoPickerActivity();
                fragment.setDelegate(ChatActivity.this);
                presentFragment(fragment);*/
                break;
            }
            case R.id.attach_video: {
                try {
                    Intent pickIntent = new Intent();
                    pickIntent.setType("video/*");
                    pickIntent.setAction(Intent.ACTION_GET_CONTENT);
                    pickIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1000));
                    Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    File video = Utilities.generateVideoPath();
                    if (video != null) {
                        if (android.os.Build.VERSION.SDK_INT > 16) {
                            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(video));
                        }
                        takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1000));
                        currentPicturePath = video.getAbsolutePath();
                    }
                    Intent chooserIntent = Intent.createChooser(pickIntent, "");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeVideoIntent});

                    startActivityForResult(chooserIntent, 2);
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
                break;
            }
            case R.id.attach_location: {
                if (!isGoogleMapsInstalled()) {
                    return true;
                }
                LocationActivity fragment = new LocationActivity();
                ((LaunchActivity) parentActivity).presentFragment(fragment, "location", false);
                break;
            }
            case R.id.attach_document: {
                DocumentSelectActivity fragment = new DocumentSelectActivity();
                fragment.setDelegate(this);
                ((LaunchActivity) parentActivity).presentFragment(fragment, "document", false);
                break;
            }
        }
        return true;
    }


    @Override
    public void applySelfActionBar() {
        if (parentActivity == null) {
            return;
        }

        try {
            ActionBar actionBar = parentActivity.getSupportActionBar();
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setCustomView(null);
            updateSubtitle();
            ((LaunchActivity) parentActivity).fixBackButton();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void willBeHidden() {
        super.willBeHidden();
        paused = true;
    }


    public boolean isGoogleMapsInstalled() {
        try {
            ApplicationInfo info = ApplicationLoader.applicationContext.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
            builder.setMessage("Install Google Maps?");
            builder.setCancelable(true);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                        startActivity(intent);
                    } catch (Exception e) {
                        FileLog.e("yahala", e);
                    }
                }
            });
            builder.setNegativeButton(R.string.Cancel, null);
            visibleDialog = builder.create();
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.show();
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinish) {
            return;
        }
        if (!firstStart && chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }


        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
        // NotificationsController.getInstance().setOpennedDialogId(dialog_id);
        MessagesController.getInstance().openned_dialog_id = dialog_id;

        if (scrollToTopOnResume) {
            if (scrollToTopUnReadOnResume && unreadMessageObject != null) {
                if (chatListView != null) {
                    chatListView.setSelectionFromTop(messages.size() - messages.indexOf(unreadMessageObject), -chatListView.getPaddingTop() - OSUtilities.dp(7));
                }
            } else {
                if (chatListView != null) {
                    chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                }
            }
            scrollToTopUnReadOnResume = false;
            scrollToTopOnResume = false;
        }

        paused = false;
        if (readWhenResume && !messages.isEmpty()) {
            for (MessageObject messageObject : messages) {
                if (!messageObject.isUnread() && !messageObject.isFromMe()) {
                    break;
                }
                //  messageObject.messageOwner.unread = false;
            }
            readWhenResume = false;
            // MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).messageOwner.id, readWithMid, 0, readWithDate, true);
        }

        fixLayout(true);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        String lastMessageText = preferences.getString("dialog_" + dialog_id, null);
        if (lastMessageText != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("dialog_" + dialog_id);
            editor.commit();
            chatActivityEnterView.setFieldText(lastMessageText);
        }
        chatActivityEnterView.setFieldFocused(true);


        //((LaunchActivity) parentActivity).showActionBar();
        ((LaunchActivity) parentActivity).updateActionBar();


    }

    @Override
    public void onPause() {
        super.onPause();
        chatActivityEnterView.hideEmojiPopup();
        String text = chatActivityEnterView.getFieldText();
        if (text != null) {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("dialog_" + dialog_id, text);
            editor.commit();
        }

        chatActivityEnterView.setFieldFocused(false);
    }

    private void setTypingAnimation(boolean start) {
        FileLog.e("yahala", "setTypingAnimation");
        TextView subtitle = (TextView) parentActivity.findViewById(R.id.action_bar_subtitle);
        if (subtitle == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_subtitle", "id", "android");
            subtitle = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (subtitle != null) {
            if (start) {
                try {
                    //if (currentChat != null) {
                    //   subtitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.typing_dots_chat, 0);
                    // } else {
                    //subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.typing_dots, 0, 0, 0);
                    // }
                    AnimationDrawable mAnim = null;
                    if (LocaleController.isRTL) {
                        subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.typing_dots_chat, 0, 0, 0);
                        mAnim = (AnimationDrawable) subtitle.getCompoundDrawables()[0];
                    } else {
                        subtitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.typing_dots_chat, 0);
                        mAnim = (AnimationDrawable) subtitle.getCompoundDrawables()[2];
                    }
                    subtitle.setCompoundDrawablePadding(OSUtilities.dp(4));


                    mAnim.setAlpha(200);
                    mAnim.start();
                    //FileLog.e("yahala", "mAnim.start();");
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
            } else {
                subtitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }


    private void showPagedownButton(boolean show, boolean animated) {
        if (pagedownButton == null) {
            return;
        }
        if (show) {
            if (pagedownButton.getVisibility() == View.GONE) {
                if (android.os.Build.VERSION.SDK_INT >= 16 && animated) {
                    pagedownButton.setVisibility(View.VISIBLE);
                    pagedownButton.setAlpha(0);
                    pagedownButton.animate().alpha(1).setDuration(200).start();
                } else {
                    pagedownButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (pagedownButton.getVisibility() == View.VISIBLE) {
                if (android.os.Build.VERSION.SDK_INT >= 16 && animated) {
                    pagedownButton.animate().alpha(0).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            pagedownButton.setVisibility(View.GONE);
                        }
                    }).setDuration(200).start();
                } else {
                    pagedownButton.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (invalidateAfterAnimation) {
            if (chatListView != null) {
                updateVisibleRows();
            }
        }
    }

    private void updateSubtitle() {
        if (isFinish) {
            return;
        }

        if (paused || getActivity() == null) {
            return;
        }

        final ActionBar actionBar = parentActivity.getSupportActionBar();

        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }

        if (currentChat != null) {
            mToolbar.setTitle(currentChat.title);
            if (title != null) {
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                title.setCompoundDrawablePadding(0);
            }
        } else if (currentUser != null) {
            //  /*if (currentUser.id / 1000 != 333 && ContactsController.getInstance().contactsDict.get(currentUser.id) == null && (ContactsController.getInstance().contactsDict.size() != 0 || !ContactsController.getInstance().loadingContacts)) {
            //      if (currentUser.phone != null && currentUser.phone.length() != 0) {
            //        actionBar.setTitle(PhoneFormat.getInstance().format("+" + currentUser.phone));
            //  } else {
            //        actionBar.setTitle(Utilities.formatName(currentUser.first_name, currentUser.last_name));
            //   }
            //    } else {
            // if (XmppManager.getInstance().getConnection().isConnected())
            mToolbar.setTitle(currentUser.first_name + " " + currentUser.last_name);//Utilities.formatName(currentUser.first_name, currentUser.last_name)
            //  else
            //  {
            //   actionBar.setTitle(currentUser.first_name + " " + currentUser.last_name);//Utilities.formatName(currentUser.first_name, currentUser.last_name)

            //  }
            //);
            //}

            if (title != null) {

                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                title.setCompoundDrawablePadding(0);

            }
        }

        CharSequence printString = MessagesController.getInstance().printingStrings.get(dialog_id);
        if (!isComposing) {
            lastPrintString = null;
            setTypingAnimation(false);

            if (currentChat != null) {
                //  /*if (currentChat instanceof TLRPC.TL_chatForbidden) {
                //      actionBar.setSubtitle(LocaleController.getString("YouWereKicked", R.string.YouWereKicked));
                //  } else if (currentChat.left) {
                //       actionBar.setSubtitle(LocaleController.getString("YouLeft", R.string.YouLeft));
                //  } else {
                //    if (onlineCount > 0 && currentChat.participants_count != 0) {
                //         actionBar.setSubtitle(String.format("%d %s, %d %s", currentChat.participants_count, LocaleController.getString("Members", R.string.Members), onlineCount, LocaleController.getString("Online", R.string.Online)));
                //    } else {
                //         actionBar.setSubtitle(String.format("%d %s", currentChat.participants_count, LocaleController.getString("Members", R.string.Members)));
                //     }
                // }*/
            } else if (currentUser != null) {
                try {
                    mToolbar.setSubtitle(LocaleController.formatUserStatus(currentUser));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else {
            lastPrintString = LocaleController.getString("Typing", R.string.Typing).replace("...", "").replace("�", ""); //printString;
            printString = lastPrintString;
            mToolbar.setSubtitle(printString);
            setTypingAnimation(true);
        }

    }

    private void fixLayout(final boolean resume) {
        if (chatListView != null) {
            final int lastPos = chatListView.getLastVisiblePosition();
            ViewTreeObserver obs = chatListView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (parentActivity == null) {
                        chatListView.getViewTreeObserver().removeOnPreDrawListener(this);
                        return false;
                    }
                    WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
                    Display display = manager.getDefaultDisplay();
                    int rotation = Surface.ROTATION_0;
                    if (display != null) {
                        rotation = display.getRotation();
                    }
                    int height;
                    int currentActionBarHeight = parentActivity.getSupportActionBar().getHeight();

                    if (currentActionBarHeight != OSUtilities.dp(48) && currentActionBarHeight != OSUtilities.dp(40)) {
                        height = currentActionBarHeight;
                    } else {
                        height = OSUtilities.dp(48);
                        if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                            height = OSUtilities.dp(40);
                        }
                    }

                    /*  if (avatarImageView != null) {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) avatarImageView.getLayoutParams();
                        params.width = height;
                        params.height = height;
                        avatarImageView.setLayoutParams(params);
                    }*/

                    chatListView.getViewTreeObserver().removeOnPreDrawListener(this);

                    if (!resume && lastPos >= messages.size() - 1) {
                        chatListView.post(new Runnable() {
                            @Override
                            public void run() {
                                chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                            }
                        });
                    }

                    return false;
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout(false);
        /*if (parentActivity != null) {
            Display display = parentActivity.getWindowManager().getDefaultDisplay();
            if (android.os.Build.VERSION.SDK_INT < 13) {
                displaySize.set(display.getWidth(), display.getHeight());
            } else {
                display.getSize(displaySize);
            }
        }*/
    }

    @Override
    public boolean onBackPressed() {
        if (mActionMode != null) {
            selectedMessagesIds.clear();
            selectedMessagesCanCopyIds.clear();
            updateVisibleRows();
            finishFragment();
            return false;
        } else if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true);
            return false;
        } else if (chatActivityEnterView.isEmojiPopupShowing()) {
            chatActivityEnterView.hideEmojiPopup();
            return false;
        }
        // ((LaunchActivity) parentActivity). updateActionBar();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
                Utilities.addMediaToGallery(currentPicturePath);
                processSendingPhoto(currentPicturePath, null);
                currentPicturePath = null;
            } else if (requestCode == 1) {
                if (data == null) {
                    return;
                }
                processSendingPhoto(null, data.getData());
            } else if (requestCode == 2) {
                String videoPath = null;
                if (data != null) {
                    Uri uri = data.getData();
                    boolean fromCamera = false;
                    if (uri != null && uri.getScheme() != null) {
                        fromCamera = uri.getScheme().contains("file");
                    } else if (uri == null) {
                        fromCamera = true;
                    }
                    if (fromCamera) {
                        if (uri != null) {
                            videoPath = uri.getPath();
                        } else {
                            videoPath = currentPicturePath;
                        }
                        Utilities.addMediaToGallery(currentPicturePath);
                        currentPicturePath = null;
                    } else {
                        try {
                            videoPath = Utilities.getPath(uri);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    }
                }
                if (videoPath == null && currentPicturePath != null) {
                    File f = new File(currentPicturePath);
                    if (f.exists()) {
                        videoPath = currentPicturePath;
                    }
                    currentPicturePath = null;
                }
               /* if(android.os.Build.VERSION.SDK_INT >= 10) {
                   Bundle args = new Bundle();
                    args.putString("videoPath", videoPath);
                    VideoEditorActivity fragment = new VideoEditorActivity();
                    fragment.setArguments(args);
                    fragment.setDelegate(this);
                    ((LaunchActivity)parentActivity).presentFragment(fragment,"Video Editor Activity",false);
                } else {*/
                processSendingVideo(videoPath);
                // }

            } else if (requestCode == 21) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                String tempPath = Utilities.getPath(data.getData());
                String originalPath = tempPath;
                if (tempPath == null) {
                    originalPath = data.toString();
                    tempPath = MediaController.copyDocumentToCache(data.getData(), "file");
                }
                if (tempPath == null) {
                    showAttachmentError();
                    return;
                }
                processSendingDocument(tempPath, originalPath);
            }
        }
    }

    @Override
    public void didFinishedVideoConverting(String videoPath) {
        processSendingVideo(videoPath);
    }

    private void showAttachmentError() {
        if (getParentActivity() == null) {
            return;
        }
        Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("UnsupportedAttachment", R.string.UnsupportedAttachment), Toast.LENGTH_SHORT);
        toast.show();
    }

    public void processSendingPhoto(String imageFilePath, Uri imageUri) {
        if ((imageFilePath == null || imageFilePath.length() == 0) && imageUri == null) {
            return;
        }
        TLRPC.TL_photo photo = MessagesController.getInstance().generatePhotoSizes(imageFilePath, imageUri);
        if (photo != null) {
            int id = UserConfig.getNewMessageId();//Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));

            XMPPManager.getInstance().sendMessage(photo, dialog_id, id);
            if (chatListView != null) {
                chatListView.setSelection(messages.size() + 1);
            }
            scrollToTopOnResume = true;
        }
    }

    public void processSendingPhotos(ArrayList<String> paths, ArrayList<Uri> uris) {
        if (paths == null && uris == null || paths != null && paths.isEmpty() || uris != null && uris.isEmpty()) {
            return;
        }
        final ArrayList<String> pathsCopy = new ArrayList<String>();
        final ArrayList<Uri> urisCopy = new ArrayList<Uri>();
        if (paths != null) {
            pathsCopy.addAll(paths);
        }
        if (uris != null) {
            urisCopy.addAll(uris);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = !pathsCopy.isEmpty() ? pathsCopy.size() : urisCopy.size();
                String path = null;
                Uri uri = null;
                for (int a = 0; a < count; a++) {
                    if (!pathsCopy.isEmpty()) {
                        path = pathsCopy.get(a);
                    } else if (!urisCopy.isEmpty()) {
                        uri = urisCopy.get(a);
                    }
                    final TLRPC.TL_photo photo = MessagesController.getInstance().generatePhotoSizes(path, uri);
                    final String paths = path;
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (photo != null) {
                                int id = UserConfig.getNewMessageId(); //Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));
                                FileLog.e("", paths + " " + dialog_id);
                                XMPPManager.getInstance().sendMessage(photo, dialog_id, id);
                                if (chatListView != null) {
                                    chatListView.setSelection(messages.size() + 1);
                                }
                                scrollToTopOnResume = true;
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public void processSendingVideo(final String videoPath) {
     /*

        if (videoPath == null || videoPath.length() == 0) {
            return;
        }
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
        TLRPC.PhotoSize size = FileLoader.scaleAndSaveImage(thumb, 120, 120, 100, false);
        if (size == null) {
            return;
        }
        size.type = "s";
        TLRPC.TL_video video = new TLRPC.TL_video();
        video.thumb = size;
        video.caption = "";
        video.id = 0;
        video.path = videoPath;
        File temp = new File(videoPath);
        if (temp != null && temp.exists()) {
            video.size = (int) temp.length();
        }
        UserConfig.lastLocalId--;
        UserConfig.saveConfig(false);

        MediaPlayer mp = MediaPlayer.create(ApplicationLoader.applicationContext, Uri.fromFile(new File(videoPath)));
        if (mp == null) {
            return;
        }
        video.duration = (int) Math.ceil(mp.getDuration() / 1000.0f);
        video.w = mp.getVideoWidth();
        video.h = mp.getVideoHeight();
        mp.release();

        MediaStore.Video.Media media = new MediaStore.Video.Media();
        Long id = Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));
        XMPPManager.getInstance().sendMessage(video, dialog_id, id);
        if (chatListView != null) {
            chatListView.setSelection(messages.size() + 1);
        }
        scrollToTopOnResume = true;*/

        if (videoPath == null || videoPath.length() == 0) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String originalPath = videoPath;
                File temp = new File(originalPath);
                originalPath += temp.length() + "_" + temp.lastModified();

                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
                TLRPC.PhotoSize size = FileLoader.scaleAndSaveImage(thumb, 90, 90, 55, false);
                final TLRPC.TL_video video = new TLRPC.TL_video();
                if (size == null) {
                    return;
                }
                size.type = "s";
                video.thumb = size;
                video.caption = "";
                video.mime_type = "video/mp4";
                video.id = 0;
                if (temp != null && temp.exists()) {
                    video.size = (int) temp.length();
                }
                UserConfig.lastLocalId--;
                UserConfig.saveConfig(false);

                MediaPlayer mp = MediaPlayer.create(ApplicationLoader.applicationContext, Uri.fromFile(new File(videoPath)));
                if (mp == null) {
                    return;
                }
                video.duration = (int) Math.ceil(mp.getDuration() / 1000.0f);
                video.w = mp.getVideoWidth();
                video.h = mp.getVideoHeight();
                mp.release();

                video.path = videoPath;

                final TLRPC.TL_video videoFinal = video;
                final String originalPathFinal = originalPath;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        int id = UserConfig.getNewMessageId(); //Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));
                        XMPPManager.getInstance().sendMessage(video, dialog_id, id);
                        if (chatListView != null) {
                            chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                        }
                        if (paused) {
                            scrollToTopOnResume = true;
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void didSelectDialog(MessagesActivity activity, long did) {
        if (dialog_id != "" && (forwardingMessage != null || !selectedMessagesIds.isEmpty())) {
           /* if (did != dialog_id) {
                int lower_part = (int)did;
                if (lower_part != 0) {
                    ActionBarActivity inflaterActivity = parentActivity;
                    if (inflaterActivity == null) {
                        inflaterActivity = (ActionBarActivity)getActivity();
                    }
                    activity.removeSelfFromStack();
                    ChatAct fragment = new ChatAct();
                    Bundle bundle = new Bundle();
                    if (lower_part > 0) {
                        bundle.putInt("user_id", lower_part);
                        fragment.setArguments(bundle);
                        fragment.scrollToTopOnResume = true;
                        ActionBarActivity act = (ActionBarActivity)getActivity();
                        if (inflaterActivity != null) {
                            ((LaunchActivity)inflaterActivity).presentFragment(fragment, "chat" + Math.random(), false);
                        }
                    } else if (lower_part < 0) {
                        bundle.putInt("chat_id", -lower_part);
                        fragment.setArguments(bundle);
                        fragment.scrollToTopOnResume = true;
                        if (inflaterActivity != null) {
                            ((LaunchActivity)inflaterActivity).presentFragment(fragment, "chat" + Math.random(), false);
                        }
                    }
                    removeSelfFromStack();
                    if (forwardingMessage != null) {
                        if (forwardingMessage.messageOwner.id > 0) {
                          //  MessagesController.getInstance().sendMessage(forwardingMessage, did);
                        }
                        forwardingMessage = null;
                    } else {
                        ArrayList<Integer> ids = new ArrayList<Integer>(selectedMessagesIds.keySet());
                        Collections.sort(ids);
                        for (Integer id : ids) {
                            if (id > 0) {
                               // MessagesController.getInstance().sendMessage(selectedMessagesIds.get(id), did);
                            }
                        }
                        selectedMessagesIds.clear();
                    }
                } else {
                    activity.finishFragment();
                }
            } else {*/
            activity.finishFragment();
            if (forwardingMessage != null) {
                //MessagesController.getInstance().sendMessage(forwardingMessage, did);
                forwardingMessage = null;
            } else {
                  /*  ArrayList<String> ids = new ArrayList<Integer>(selectedMessagesIds.keySet());

                    Collections.sort(ids, new Comparator<String>() {
                        @Override
                        public int compare(Integer lhs, Integer rhs) {
                            return lhs.compareTo(rhs);
                        }
                    });
                    for (Integer id : ids) {
                       // MessagesController.getInstance().sendMessage(selectedMessagesIds.get(id), did);
                    }
                    selectedMessagesIds.clear();
                }
                chatListView.setSelection(messages.size() + 1);
                scrollToTopOnResume = true;
            //}
        }*/
            }
        }
    }

    private void updateVisibleRows() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);

            Object tag = view.getTag();
            if (tag instanceof ChatListRowHolderEx) {
                ChatListRowHolderEx holder = (ChatListRowHolderEx) tag;
                holder.update();

                boolean disableSelection = false;
                boolean selected = false;
                if (mActionMode != null) {
                    if (selectedMessagesIds.containsKey(holder.message.messageOwner.id)) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }
                updateRowBackground(holder, disableSelection, selected);
            } else if (view instanceof ChatBaseCell) {
                ChatBaseCell cell = (ChatBaseCell) view;

                boolean disableSelection = false;
                boolean selected = false;
                if (mActionMode != null) {
                    if (selectedMessagesIds.containsKey(cell.getMessageObject().messageOwner.id)) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                cell.setMessageObject(cell.getMessageObject());

                cell.setCheckPressed(!disableSelection, disableSelection && selected);
            }
        }
    }

    private void updateRowBackground(ChatListRowHolderEx holder, boolean disableSelection, boolean selected) {
        int messageType = holder.message.type;
        if (!disableSelection) {
            if (messageType == 12) {
                holder.chatBubbleView.setBackgroundResource(R.drawable.chat_outgoing_text_states);
                holder.chatBubbleView.setPadding(OSUtilities.dp(6), OSUtilities.dp(6), OSUtilities.dp(18), 0);
            } else if (messageType == 13) {
                holder.chatBubbleView.setBackgroundResource(R.drawable.chat_incoming_text_states);
                holder.chatBubbleView.setPadding(OSUtilities.dp(15), OSUtilities.dp(6), OSUtilities.dp(9), 0);
            } else if (messageType == 8) {
                holder.chatBubbleView.setBackgroundResource(R.drawable.chat_outgoing_text_states);
                holder.chatBubbleView.setPadding(OSUtilities.dp(9), OSUtilities.dp(9), OSUtilities.dp(18), 0);
            } else if (messageType == 9) {
                holder.chatBubbleView.setBackgroundResource(R.drawable.chat_incoming_text_states);
                holder.chatBubbleView.setPadding(OSUtilities.dp(18), OSUtilities.dp(9), OSUtilities.dp(9), 0);
            }
        } else {
            if (messageType == 12) {
                if (selected) {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_out_selected);
                } else {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_out);
                }
                holder.chatBubbleView.setPadding(OSUtilities.dp(6), OSUtilities.dp(6), OSUtilities.dp(18), 0);
            } else if (messageType == 13) {
                if (selected) {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_in_selected);
                } else {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_in);
                }
                holder.chatBubbleView.setPadding(OSUtilities.dp(15), OSUtilities.dp(6), OSUtilities.dp(9), 0);
            } else if (messageType == 8) {
                if (selected) {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_out_selected);
                } else {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_out);
                }
                holder.chatBubbleView.setPadding(OSUtilities.dp(9), OSUtilities.dp(9), OSUtilities.dp(18), 0);
            } else if (messageType == 9) {
                if (selected) {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_in_selected);
                } else {
                    holder.chatBubbleView.setBackgroundResource(R.drawable.msg_in);
                }
                holder.chatBubbleView.setPadding(OSUtilities.dp(18), OSUtilities.dp(9), OSUtilities.dp(9), 0);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
//         FileLog.e("Test","id:"+id + " args[2]"+args[1]+" size " +users.size());

        if (id == MessagesController.messagesDidLoadedd) {
            //  Utilities.stageQueue.postRunnable(new Runnable() {
            //     @Override
            //    public void run() {

            String did = (String) args[0];
            //FileLog.e("Test", "messagesDidLoadedd");
            if (did == dialog_id) {
                loading = false;
                int offset = (Integer) args[1];
                int count = (Integer) args[2];
                boolean isCache = (Boolean) args[4];
                int fnid = (Integer) args[5];
                int last_unread_date = (Integer) args[8];
                boolean forwardLoad = (Boolean) args[9];
                boolean wasUnread = false;
                boolean positionToUnread = false;
                if (fnid != 0) {
                    first_unread_id = (Integer) args[5];
                    last_unread_id = (Integer) args[6];
                    unread_to_load = (Integer) args[7];
                    positionToUnread = true;
                }
                ArrayList<MessageObject> messArr = (ArrayList<MessageObject>) args[3];
              /*  for (Messages m : messArr) {
                    FileLog.e("Test Mssages array", m.getMessage());

                }*/
                //FileLog.e("Test","messArr.size()"+ messArr.size());
                int newRowsCount = 0;
                unread_end_reached = last_unread_id == 0;
                for (int a = 0; a < messArr.size(); a++) {
                    MessageObject obj = messArr.get(a);
                    if (messagesDict.containsKey(obj.messageOwner.id)) {
                        continue;
                    }

                    if (obj.messageOwner.id > 0) {
                        maxMessageId = Math.min(obj.messageOwner.id, maxMessageId);
                        minMessageId = Math.max(obj.messageOwner.id, minMessageId);
                    }
                    if (obj.messageOwner.getDate().after(maxDate)) {
                        maxDate = obj.messageOwner.getDate();
                    }

                    if (minDate == null || obj.messageOwner.getDate().before(minDate)) {
                        minDate = obj.messageOwner.getDate();
                        //FileLog.e("Test", "minDate:"+minDate);
                    }

                    if (obj.messageOwner.getOut() != 1 && obj.messageOwner.getRead_state() == 1) {
                        wasUnread = true;
                    }
                    messagesDict.put(obj.messageOwner.id, obj);
                    ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);

                    if (dayArray == null) {
                        dayArray = new ArrayList<MessageObject>();
                        messagesByDays.put(obj.dateKey, dayArray);

                        Messages dateMsg = new Messages();
                        dateMsg.tl_message = new TLRPC.TL_message();
                        dateMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();
                        dateMsg.setMessage(LocaleController.formatDateChat(obj.messageOwner.getDate().getTime()));
                        dateMsg.id = 0;
                        dateMsg.setId(0l);
                        dateMsg.setDate(obj.messageOwner.getDate());
                        MessageObject dateObj = new MessageObject(dateMsg, null);
                        dateObj.contentType = dateObj.type = 10;
                        if (forwardLoad) {
                            messages.add(0, dateObj);
                        } else {
                            messages.add(dateObj);
                        }
                        newRowsCount++;
                    }

                    newRowsCount++;
                    dayArray.add(obj);
                    if (forwardLoad) {
                        messages.add(0, obj);
                    } else {
                        messages.add(messages.size() - 1, obj);
                    }

                    if (!forwardLoad) {
                        if (obj.messageOwner.id == first_unread_id) {
                            Messages dateMsg = new Messages();
                            dateMsg.tl_message = new TLRPC.TL_message();
                            dateMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();
                            dateMsg.setMessage("");
                            dateMsg.id = 0;
                            dateMsg.setId(0l);
                            dateMsg.setDate(obj.messageOwner.getDate());
                            MessageObject dateObj = new MessageObject(dateMsg, null);
                            dateObj.contentType = dateObj.type = 7;
                            boolean dateAdded = true;
                            if (a != messArr.size() - 1) {
                                MessageObject next = messArr.get(a + 1);
                                dateAdded = !next.dateKey.equals(obj.dateKey);
                            }
                            messages.add(messages.size() - (dateAdded ? 0 : 1), dateObj);
                            unreadMessageObject = dateObj;
                            newRowsCount++;
                        }
                        if (obj.messageOwner.id == last_unread_id) {
                            unread_end_reached = true;
                        }
                    }

                }
                //FileLog.e("Test", "-  (mindate,maxdate)=("+minDate+","+maxDate+")");
                if (unread_end_reached) {
                    first_unread_id = 0;
                    last_unread_id = 0;
                }

                if (forwardLoad) {
                    if (messArr.size() != count) {
                        unread_end_reached = true;
                        first_unread_id = 0;
                        last_unread_id = 0;
                    }
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            chatAdapter.notifyDataSetChanged();
                        }
                    });

                    loadingForward = false;
                } else {
                    //FileLog.e("test","messArr.size() != count : "+messArr.size()+" != "+ count);
                    if (messArr.size() != count) {
                        if (isCache) {
                            cacheEndReached = true;
                            endReached = true;
                            /*if (currentEncryptedChat != null) {
                                endReached = true;
                            }*/
                      /* } else {
                            cacheEndReaced = true;
                            endReached = true;*/
                        }
                    }
                    loading = false;

                    if (chatListView != null) {
                        if (first || scrollToTopOnResume) {
                            //  Utilities.RunOnUIThread(new Runnable() {
                            //       @Override
                            //     public void run() {
                            chatAdapter.notifyDataSetChanged();
                            //      }
                            // });

                            if (positionToUnread && unreadMessageObject != null) {
                                if (messages.get(messages.size() - 1) == unreadMessageObject) {
                                    chatListView.setSelectionFromTop(0, OSUtilities.dp(-11));
                                } else {
                                    chatListView.setSelectionFromTop(messages.size() - messages.indexOf(unreadMessageObject), OSUtilities.dp(-11));
                                }
                                ViewTreeObserver obs = chatListView.getViewTreeObserver();
                                obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                    @Override
                                    public boolean onPreDraw() {
                                        if (messages.get(messages.size() - 1) == unreadMessageObject) {
                                            chatListView.setSelectionFromTop(0, OSUtilities.dp(-11));
                                        } else {
                                            chatListView.setSelectionFromTop(messages.size() - messages.indexOf(unreadMessageObject), OSUtilities.dp(-11));
                                        }
                                        chatListView.getViewTreeObserver().removeOnPreDrawListener(this);
                                        return false;
                                    }
                                });
                                chatListView.invalidate();
                                showPagedownButton(true, true);
                            } else {
                                chatListView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                                    }
                                });
                            }
                        } else {
                            final int firstVisPos = chatListView.getLastVisiblePosition();
                            View firstVisView = chatListView.getChildAt(chatListView.getChildCount() - 1);
                            int top = ((firstVisView == null) ? 0 : firstVisView.getTop()) - chatListView.getPaddingTop();
                            final int newRowsCountT = newRowsCount;
                            final int topT = top;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatAdapter.notifyDataSetChanged();
                                    chatListView.setSelectionFromTop(firstVisPos + newRowsCountT, topT);
                                }
                            });

                        }

                        if (paused) {
                            scrollToTopOnResume = true;
                            if (positionToUnread && unreadMessageObject != null) {
                                scrollToTopUnReadOnResume = true;
                            }
                        }

                        if (first) {
                            if (chatListView.getEmptyView() == null) {
                                chatListView.setEmptyView(emptyView);
                            }
                        }
                    } else {
                        scrollToTopOnResume = true;
                        if (positionToUnread && unreadMessageObject != null) {
                            scrollToTopUnReadOnResume = true;
                        }
                    }
                }

                if (first && messages.size() > 0) {
                    if (last_unread_id != 0) {
                        MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).messageOwner);
                    } else {
                        MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).messageOwner);
                    }
                    first = false;
                }

                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                /*for (Messages m : messages) {
                    FileLog.e("Test Mssages array  :", m.getMessage()+ "- id:"+m.getId());

                }*/
                //chatAdapter.notifyDataSetChanged();
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                    }
                });
            }
        } else if (id == XMPPManager.didReceivedNewMessages) {
            // final String jid = (String) args[1];
            // final String message = (String) args[0];

            final Messages msg = (Messages) args[0];
            FileLog.e("didReceivedNewMessages", "didReceivedNewMessages " + msg.getSend_state());
            if (!users.containsKey(msg.getJid())) {
                return;
            }
            if (messagesDict.containsKey(msg.id)) {
                return;
            }
            boolean markAsRead = false;
            int oldCount = messages.size();
            if (msg.getId() > 0) {
                maxMessageId = msg.getId();

            }
            maxDate = msg.getDate();
            MessageObject obj = new MessageObject(msg, null);

            messagesDict.put((int) (long) msg.getId(), obj);

            ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);
            if (dayArray == null) {
                dayArray = new ArrayList<MessageObject>();
                messagesByDays.put(obj.dateKey, dayArray);

                Messages dateMsg = new Messages();
                dateMsg.setMessage(LocaleController.formatDateChat(obj.messageOwner.getDate().getTime()));
                dateMsg.setId(0L);
                dateMsg.setDate(msg.getDate());
                dateMsg.tl_message = new TLRPC.TL_message();
                dateMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();

                dateMsg.setType(10);
                dateMsg.contentType = 10;
                MessageObject messageObject = new MessageObject(dateMsg, null);
                messageObject.type = messageObject.contentType = 10;
                messages.add(0, messageObject);

            }


            dayArray.add(0, obj);
            messages.add(0, obj);

            markAsRead = true;


            if (progressView != null) {
                progressView.setVisibility(View.GONE);
            }
            if (chatAdapter != null) {
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        chatAdapter.notifyDataSetChanged();

                    }
                });

            } else {
                scrollToTopOnResume = true;

            }
            if (chatListView != null && chatAdapter != null) {
                int lastVisible = chatListView.getLastVisiblePosition();
                if (endReached) {
                    lastVisible++;
                }
                if (lastVisible == oldCount) {
                    if (paused) {
                        scrollToTopOnResume = true;
                    } else {
                        chatListView.post(new Runnable() {
                            @Override
                            public void run() {
                                //chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                                chatListView.smoothScrollToPosition(chatAdapter.getCount() - 1);
                            }
                        });
                    }
                } else {
                    if (msg.tl_message.media instanceof TLRPC.TL_messageMediaEmpty) {
                        chatListView.post(new Runnable() {
                            @Override
                            public void run() {
                                //chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                                chatListView.smoothScrollToPosition(chatAdapter.getCount() - 1);
                            }
                        });
                    } else {
                        if (obj.isOut()) {
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    showPagedownButton(true, true);
                                }
                            });
                        }
                    }
                }
            } else {
                scrollToTopOnResume = true;
            }
/*
        if (markAsRead) {
           if (paused) {
                readWhenResume = true;
                readWithDate = maxDate;
                readWithMid = minMessageId;
            } else {
                MessagesController.getInstance().markDialogAsRead(dialog_id, msg);
          //  }
        }*/
            MessagesStorage.getInstance().updateMessage(msg);//markDialogAsRead
/*
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {

                   // FileLog.e("Test", "+users.get(jid).first_name:" + users.get(jid).first_name);
                    //msg.user = users.get(msg.getJid());

                    //messages.add(0,msg);
                    chatAdapter.notifyDataSetChanged();
                    chatListView.post(new Runnable() {
                        @Override
                        public void run() {
                            showPagedownButton(true, true);
                            // chatListView.setSelectionFromTop(messages.size() - 1, -100000 - chatListView.getPaddingTop());
                        }
                 });
                  }
            });*/


        } else if (id == MessagesController.updateInterfaces) {
            int updateMask = (Integer) args[0];
            if ((updateMask & MessagesController.UPDATE_MASK_NAME) != 0 || (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0) {
                updateSubtitle();
                // updateOnlineCount();
            }
            if ((updateMask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_NAME) != 0) {
                // checkAndUpdateAvatar();
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                CharSequence printString = MessagesController.getInstance().printingStrings.get(dialog_id);
                if (lastPrintString != null && printString == null || lastPrintString == null && printString != null || lastPrintString != null && printString != null && !lastPrintString.equals(printString)) {
                    updateSubtitle();
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PHONE) != 0) {
                updateContactStatus();
            }
        } else if (id == XMPPManager.messageReceivedByServer) {

            Integer msgId = (int) (long) ((Long) args[1]);
            MessageObject obj = messagesDict.get(msgId);
//            FileLog.e("messageReceivedByServer","messageReceivedByServer "+obj.messageOwner.getSend_state());
            if (obj != null) {
                if (obj.messageOwner.tl_message.attachPath != null && obj.messageOwner.tl_message.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.tl_message.attachPath);
                }
                obj.messageOwner.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENT);

                //messagesDict.remove(msgId);
                //messagesDict.put(msgId, obj);
                updateVisibleRows();
            }
        } else if (id == XMPPManager.messageReceivedByAck) {


            Integer msgId = Integer.parseInt((String) args[1]);

            MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                if (obj.messageOwner.tl_message.attachPath != null && obj.messageOwner.tl_message.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.tl_message.attachPath);
                }
                obj.messageOwner.setSend_state(XMPPManager.MESSAGE_SEND_STATE_AKN);
                // messagesDict.remove(msgId);
                // messagesDict.put(msgId, obj);
                updateVisibleRows();
            }
        } else if (id == XMPPManager.messageSendError) {

            String jid = (String) args[0];
            Integer msgId = (int) (long) ((Long) args[1]);

            MessageObject obj = messagesDict.get((int) (long) msgId);
            if (!users.containsKey(obj.messageOwner.getJid())) {
                return;
            }
            if (obj != null) {
                obj.messageOwner.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR);
                messagesDict.remove(msgId);
                messagesDict.put((int) (long) msgId, obj);
                if (animationInProgress) {
                    invalidateAfterAnimation = true;
                } else {
                    if (chatListView != null) {
                        updateVisibleRows();
                    }
                }
              /*  if (obj.messageOwner.attachPath != null && obj.messageOwner.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.attachPath);
                } else if (id == MediaController.recordStartError || id == MediaController.recordStopped) {
                if (recordingAudio) {
                    recordingAudio = false;
                    updateAudioRecordIntefrace();
                }
            } else if (id == MediaController.recordStarted) {
                if (!recordingAudio) {
                    recordingAudio = true;
                    updateAudioRecordIntefrace();
                }
            }*/
            }

        } else if (id == FileLoader.FileUploadProgressChanged) {
            String location = (String) args[0];
            ProgressBar bar;
            if ((bar = progressBarMap.get(location)) != null) {
                Float progress = (Float) args[1];
                bar.setProgress((int) (progress * 100));
            }
        } else if (id == FileLoader.FileDidFailedLoad) {

            String location = (String) args[0];

            if (loadingFile.containsKey(location)) {
                loadingFile.remove(location);
                updateVisibleRows();
            }
        } else if (id == FileLoader.FileDidLoaded) {
            String location = (String) args[0];
            Integer msgId = (Integer) args[1];
            if (loadingFile.containsKey(location)) {
                loadingFile.remove(location);
                updateVisibleRows();
            }
          /*  MessageObject obj = messagesDict.get(msgId);
            if (obj != null) {
                if (obj.messageOwner.tl_message.attachPath != null && obj.messageOwner.tl_message.attachPath.length() != 0) {
                    progressBarMap.remove(obj.messageOwner.tl_message.attachPath);
                }
                obj.messageOwner.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENT);
                // messagesDict.remove(msgId);
                // messagesDict.put(msgId, obj);
                updateVisibleRows();
            }*/


        } else if (id == FileLoader.FileLoadProgressChanged) {
            String location = (String) args[0];
            ArrayList<ProgressBar> arr = loadingFile.get(location);
            if (arr != null) {
                Float progress = (Float) args[1];
                for (ProgressBar bar : arr) {
                    bar.setProgress((int) (progress * 100));
                }
            }
        } else if (id == MediaController.audioDidReset) {
            Integer mid = (Integer) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().messageOwner.id == mid) {
                            cell.updateButtonState();
                            break;
                        }
                    }
                }
            }
        } else if (id == MediaController.audioProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().messageOwner.id == mid) {
                            cell.updateProgress();
                            break;
                        }
                    }
                }
            }
        } else if (id == MessagesController.closeChats) {
            removeSelfFromStack();
        } else if (id == MediaController.screenshotTook) {
            updateInformationForScreenshotDetector();
        } else if (id == MessagesController.chatStateUpdated) {
            String chatState = (String) args[0];
            String jid = (String) args[1];


            if (!users.containsKey(jid)) {
                return;
            }
            isComposing = false;
            if (chatState == ChatState.composing.name()) {
                isComposing = true;
            }
            //FileLog.e("Test messageReceivedByAck",  "setTypingAnimation(true);");

            updateSubtitle();
        } else if (id == MessagesController.messagesDeleted) {
            ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
            boolean updated = false;
            for (Integer ids : markAsDeletedMessages) {
                MessageObject obj = messagesDict.get(ids);
                if (obj != null) {
                    int index = messages.indexOf(obj);
                    if (index != -1) {
                        messages.remove(index);
                        messagesDict.remove(ids);
                        ArrayList<MessageObject> dayArr = messagesByDays.get(obj.dateKey);
                        dayArr.remove(obj);
                        if (dayArr.isEmpty()) {
                            messagesByDays.remove(obj.dateKey);
                            messages.remove(index);
                        }
                        updated = true;
                    }
                }
            }
            if (messages.isEmpty()) {
                if (!endReached && !loading) {
                    progressView.setVisibility(View.GONE);
                    chatListView.setEmptyView(null);

                    maxMessageId = Integer.MAX_VALUE;
                    minMessageId = Integer.MIN_VALUE;

                    // maxDate = Integer.MIN_VALUE;
                    //minDate = 0;
                    // MessagesController.getInstance().loadMessages(dialog_id, 0, 30, 0, true, null, classGuid, true, false);
                    //loading = true;
                }
            }
            if (updated && chatAdapter != null) {
                //  removeUnreadPlane(false);
                chatAdapter.notifyDataSetChanged();
            }

        } else if (id == 997) {
            int mid = UserConfig.getNewMessageId();//Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));
            XMPPManager.getInstance().sendMessage((Double) args[0], (Double) args[1], dialog_id, mid);
            if (chatListView != null) {
                chatListView.setSelection(messages.size() + 1);
                scrollToTopOnResume = true;
            }
        }
    }

    private void updateInformationForScreenshotDetector() {
    }

    @Override
    public void didSelectFile(DocumentSelectActivity activity, String path) {
        activity.finishFragment();
        processSendingDocument(path, path);
    }

    @Override
    public void startDocumentSelectActivity() {
        try {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("*/*");
            getParentActivity().startActivityForResult(photoPickerIntent, 21);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (messageObject == null) {
            return null;
        }
        int count = chatListView.getChildCount();

        for (int a = 0; a < count; a++) {
            MessageObject messageToOpen = null;
            ImageReceiver imageReceiver = null;
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatMediaCell) {
                ChatMediaCell cell = (ChatMediaCell) view;
                MessageObject message = cell.getMessageObject();
                if (message != null && message.messageOwner.id == messageObject.messageOwner.id) {
                    messageToOpen = message;
                    imageReceiver = cell.getPhotoImage();
                }
            } else if (view.getTag() != null) {
                Object tag = view.getTag();
                if (tag instanceof ChatListRowHolderEx) {
                    ChatListRowHolderEx holder = (ChatListRowHolderEx) tag;
                    if (holder.message != null && holder.message.messageOwner.id == messageObject.messageOwner.id) {
                        messageToOpen = holder.message;
                        imageReceiver = holder.photoImage.imageReceiver;
                        view = holder.photoImage;
                    }
                }
            }

            if (messageToOpen != null) {
                int coords[] = new int[2];
                view.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - OSUtilities.statusBarHeight;
                object.parentView = chatListView;
                object.imageReceiver = imageReceiver;
                object.thumb = object.imageReceiver.getBitmap();
                return object;
            }
        }
        return null;
    }

    public void processSendingDocument(String path, String originalPath) {
        if (path == null || originalPath == null) {
            return;
        }
        ArrayList<String> paths = new ArrayList<String>();
        ArrayList<String> originalPaths = new ArrayList<String>();
        paths.add(path);
        originalPaths.add(originalPath);
        processSendingDocuments(paths, originalPaths);
    }

    public void processSendingDocuments(final ArrayList<String> paths, final ArrayList<String> originalPaths) {
        if (paths == null && originalPaths == null || paths != null && originalPaths != null && paths.size() != originalPaths.size()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int a = 0; a < paths.size(); a++) {
                    processSendingDocumentInternal(paths.get(a), originalPaths.get(a));
                }
            }
        }).start();
    }

    private void processSendingDocumentInternal(String path, String originalPath) {
        if (path == null || path.length() == 0) {
            return;
        }
        final File f = new File(path);
        if (!f.exists() || f.length() == 0) {
            return;
        }

        String name = f.getName();
        if (name == null) {
            name = "noname";
        }
        String ext = "";
        int idx = path.lastIndexOf(".");
        if (idx != -1) {
            ext = path.substring(idx + 1);
        }
        if (originalPath != null) {
            originalPath += "" + f.length();
        }

        TLRPC.TL_document document = new TLRPC.TL_document();
        document.thumb = new TLRPC.TL_photoSizeEmpty();
        document.thumb.type = "s";
        document.id = 0;
        document.user_id = UserConfig.clientUserId;
        document.date = ConnectionsManager.getInstance().getCurrentTime();
        document.file_name = name;
        document.size = (int) f.length();
        document.dc_id = 0;

        document.path = path;
        if (ext.length() != 0) {
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            String mimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
            if (mimeType != null) {
                document.mime_type = mimeType;
            } else {
                document.mime_type = "application/octet-stream";
            }
        } else {
            document.mime_type = "application/octet-stream";
        }
        if (document.mime_type.equals("image/gif")) {
            try {
                Bitmap bitmap = FileLoader.loadBitmap(f.getAbsolutePath(), null, 90, 90);
                if (bitmap != null) {
                    document.thumb = FileLoader.scaleAndSaveImage(bitmap, 90, 90, 55, false);
                    document.thumb.type = "s";
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
        if (document.thumb == null) {
            document.thumb = new TLRPC.TL_photoSizeEmpty();
            document.thumb.type = "s";
        }
        int id = UserConfig.getNewMessageId();// Long.parseLong(String.valueOf(UserConfig.getNewMessageId()));
        XMPPManager.getInstance().sendMessage(document, dialog_id, id);
    }

    private void alertUserOpenError(MessageObject message) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setPositiveButton(R.string.OK, null);
        if (message.type == 3) {
            builder.setMessage(R.string.NoPlayerInstalled);
        } else {
            builder.setMessage(LocaleController.formatString("NoHandleAppInstalled", R.string.NoHandleAppInstalled, message.messageOwner.tl_message.media.document.mime_type));
        }
        showAlertDialog(builder);
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {

    }

    @Override
    public void willHidePhotoViewer() {

    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {

    }

    @Override
    public void cancelButtonPressed() {

    }

    @Override
    public void sendButtonPressed(int index) {

    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    @Override
    public void didSelectPhotos(ArrayList<String> photos) {
        processSendingPhotos(photos, null);
    }

    @Override
    public void startPhotoSelectActivity() {

    }

    private class ChatAdapter extends BaseAdapter {
        private int mLastPosition;
        private Context mContext;

        public ChatAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            int count = messages.size();
            if (count != 0) {
                if (!endReached) {
                    count++;
                }
                if (!unread_end_reached) {
                    count++;
                }
            }
            return count;
        }


        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int offset = 1;
            if ((!cacheEndReached || !unread_end_reached) && messages.size() != 0) {
                if (!cacheEndReached) {
                    offset = 0;
                }
                if (i == 0 && !cacheEndReached || !unread_end_reached && i == (messages.size() + 1 - offset)) {
                    if (view == null) {
                        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        view = li.inflate(R.layout.chat_loading_layout, viewGroup, false);
                        View progressBar = view.findViewById(R.id.progressLayout);
                        //  if (isCustomTheme) {
                        progressBar.setBackgroundResource(R.drawable.system_loader2);
                        // } else {
                        //     progressBar.setBackgroundResource(R.drawable.system_loader1);
                        // }
                    }
                    return view;
                }
            }
            MessageObject message = messages.get(messages.size() - i - offset);
            int type = message.contentType;
            if (view == null) {
                LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (type == 0) {
                    view = new ChatMessageCell(mContext);
                }
                if (type == 1) {
                    view = new ChatMediaCell(mContext);
                    ((ChatMediaCell) view).downloadPhotos = downloadPhotos;
                } else if (type == 10) {
                    view = li.inflate(R.layout.chat_action_message_layout, viewGroup, false);
                } else if (type == 11) {
                    view = li.inflate(R.layout.chat_action_change_photo_layout, viewGroup, false);
                } else if (type == 4) {
                    view = li.inflate(R.layout.chat_outgoing_contact_layout, viewGroup, false);
                } else if (type == 5) {
                    if (currentChat != null) {
                        view = li.inflate(R.layout.chat_group_incoming_contact_layout, viewGroup, false);
                    } else {
                        view = li.inflate(R.layout.chat_incoming_contact_layout, viewGroup, false);
                    }
                } else if (type == 7) {
                    view = li.inflate(R.layout.chat_unread_layout, viewGroup, false);
                } else if (type == 8) {
                    view = li.inflate(R.layout.chat_outgoing_document_layout, viewGroup, false);
                } else if (type == 9) {
                    if (currentChat != null) {
                        view = li.inflate(R.layout.chat_group_incoming_document_layout, viewGroup, false);
                    } else {
                        view = li.inflate(R.layout.chat_incoming_document_layout, viewGroup, false);
                    }
                } else if (type == 2) {
                    view = new ChatAudioCell(mContext);
                }
            }

            boolean selected = false;
            boolean disableSelection = false;
            if (mActionMode != null) {
                if (selectedMessagesIds.containsKey(message.messageOwner.id)) {
                    view.setBackgroundColor(0x6633b5e5);
                    selected = true;
                } else {
                    view.setBackgroundColor(0);
                }
                disableSelection = true;
            } else {
                view.setBackgroundColor(0);
            }

            if (view instanceof ChatBaseCell) {
                ((ChatBaseCell) view).delegate = new ChatBaseCell.ChatBaseCellDelegate() {
                    @Override
                    public void didPressedUserAvatar(ChatBaseCell cell, TLRPC.User user) {
                        if (user != null && user.id != UserConfig.getClientUserId()) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            //   presentFragment(new UserProfileActivity(args));
                        }
                    }

                    @Override
                    public void didPressedCancelSendButton(ChatBaseCell cell) {
                        MessageObject message = cell.getMessageObject();
                        FileLog.e("didPressedCancelSendButton", "cancelSendingMessage");
                        // if (message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SENT) {
                        XMPPManager.getInstance().cancelSendingMessage(message);
                        // }
                    }

                    @Override
                    public void didLongPressed(ChatBaseCell cell) {
                        createMenu(cell, false);
                    }

                    @Override
                    public boolean canPerformActions() {
                        return mActionMode == null;
                    }
                };
                if (view instanceof ChatMediaCell) {
                    ((ChatMediaCell) view).mediaDelegate = new ChatMediaCell.ChatMediaCellDelegate() {
                        @Override
                        public void didPressedImage(ChatMediaCell cell) {
                            MessageObject message = cell.getMessageObject();
                            if (message.messageOwner.getSend_state() != null) {
                                if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR) {
                                    createMenu(cell, false);
                                    return;
                                } else if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SENDING) {
                                    return;
                                }
                            }

                            if (message.type == 1) {
                                PhotoViewer.getInstance().setParentActivity(getActivity());
                                PhotoViewer.getInstance().openPhoto(message, ChatActivity.this);

                            } else if (message.type == 3) {
                                try {
                                    File f = null;
                                    if (message.messageOwner.tl_message.attachPath != null && message.messageOwner.tl_message.attachPath.length() != 0) {
                                        f = new File(message.messageOwner.tl_message.attachPath);
                                    }
                                    if (f == null || f != null && !f.exists()) {
                                        f = new File(OSUtilities.getCacheDir(), message.getFileName());
                                    }
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(f), "video/mp4");
                                    getParentActivity().startActivity(intent);
                                } catch (Exception e) {
                                    alertUserOpenError(message);
                                }
                            } else if (message.type == 4) {
                                if (!isGoogleMapsInstalled()) {
                                    return;
                                }
                                LocationActivity fragment = new LocationActivity();
                                fragment.setMessageObject(message);
                                ((LaunchActivity) getParentActivity()).presentFragment(fragment, "profile", false);
                            }
                        }
                    };
                }

                ((ChatBaseCell) view).isChat = currentChat != null;
                ((ChatBaseCell) view).setMessageObject(message);
                ((ChatBaseCell) view).setCheckPressed(!disableSelection, disableSelection && selected);
                if (view instanceof ChatAudioCell && (downloadAudios == 0 || downloadAudios == 2 && ConnectionsManager.isConnectedToWiFi())) {
                    ((ChatAudioCell) view).downloadAudioIfNeed();
                } else if (view instanceof ChatMediaCell) {
                    ((ChatMediaCell) view).downloadPhotos = downloadPhotos;
                }
            } else {
                ChatListRowHolderEx holder = (ChatListRowHolderEx) view.getTag();
                if (holder == null) {
                    holder = new ChatListRowHolderEx(view, message.type);
                    view.setTag(holder);
                }
                holder.message = message;
                updateRowBackground(holder, disableSelection, selected);
                holder.update();
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            int offset = 1;
            int type;
            if (!endReached && messages.size() != 0) {
                offset = 0;
                if (i == 0) {
                    return 11;
                }
            }
            if (!unread_end_reached && i == (messages.size() + 1 - offset)) {
                return 11;
            }
            MessageObject message = messages.get(messages.size() - i - offset);
            try {
                type = message.contentType;

            } catch (Exception e) {
                type = 1;
            }

            return type;

        }

        @Override
        public int getViewTypeCount() {
            return 12;
        }

        @Override
        public boolean isEmpty() {
            int count = messages.size();
            if (count != 0) {
                if (!endReached) {
                    count++;
                }
                if (!unread_end_reached) {
                    count++;
                }
            }
            return count == 0;
        }
    }

    public class ChatListRowHolderEx {
        public BackupImageView avatarImageView;
        public TextView nameTextView;
        public TextView messageTextView;
        public MessageActionLayout messageLayoutAction;
        public TextView timeTextView;
        public BackupImageView2 photoImage;
        public ImageView halfCheckImage;
        public ImageView checkImage;
        public TextView actionAttachButton;
        public TextView videoTimeText;
        public MessageObject message;
        public TextView phoneTextView;
        public BackupImageView2 contactAvatar;
        public View contactView;
        public ImageView addContactButton;
        public View addContactView;
        public View chatBubbleView;

        public ProgressBar actionProgress;
        public View actionView;
        public ImageView actionCancelButton;

        private PhotoObject photoObjectToSet = null;
        private File photoFile = null;
        private String photoFilter = null;

        public void update() {
            TLRPC.User fromUser = null;
            try {
                fromUser = ContactsController.getInstance().friendsDict.get(message.messageOwner.getJid());
            } catch (Exception e) {
                // e.printStackTrace();
            }
            int type = message.type;

            if (timeTextView != null) {
                timeTextView.setText(LocaleController.formatterDay.format(message.messageOwner.getDate()));
            }

            if (avatarImageView != null && fromUser != null) {
                TLRPC.FileLocation photo = null;
                if (fromUser.photo != null) {
                    photo = fromUser.photo.photo_small;
                }
                int placeHolderId = Utilities.getUserAvatarForId(fromUser.id);
                avatarImageView.setImage(photo, "50_50", placeHolderId);
            }

            if (type != 12 && type != 13 && nameTextView != null && fromUser != null && type != 8 && type != 9) {
                nameTextView.setText(Utilities.formatName(fromUser.first_name, fromUser.last_name));
                nameTextView.setTextColor(Utilities.getColorForId((int) (long) message.messageOwner.id));
            }

            if (type == 11 || type == 10) {
                int width = OSUtilities.displaySize.x - OSUtilities.dp(30);

                try {
                    messageTextView.setText(message.messageText);
                    messageTextView.setMaxWidth(width);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (type == 11) {
                    if (message.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                        photoImage.setImage(message.messageOwner.tl_message.action.newUserPhoto.photo_small, "50_50", Utilities.getUserAvatarForId(currentUser.id));
                    } else {
                        PhotoObject photo = PhotoObject.getClosestImageWithSize(message.photoThumbs, OSUtilities.dp(64), OSUtilities.dp(64));
                        if (photo != null) {
                            if (photo.image != null) {
                                photoImage.setImageBitmap(photo.image);
                            } else {
                                photoImage.setImage(photo.photoOwner.location, "50_50", Utilities.getGroupAvatarForId(currentChat.id));
                            }
                        } else {
                            photoImage.setImageResource(Utilities.getGroupAvatarForId(currentChat.id));
                        }
                    }
                    photoImage.imageReceiver.setVisible(!PhotoViewer.getInstance().isShowingImage(message), false);
                }
            } else if (type == 12 || type == 13) {
                TLRPC.User contactUser = MessagesController.getInstance().users.get(message.messageOwner.tl_message.media.user_id);
                if (contactUser != null) {
                    nameTextView.setText(Utilities.formatName(message.messageOwner.tl_message.media.first_name, message.messageOwner.tl_message.media.last_name));
                    nameTextView.setTextColor(Utilities.getColorForId(contactUser.id));
                    String phone = message.messageOwner.tl_message.media.phone_number;
                    if (phone != null && phone.length() != 0) {
                        if (!phone.startsWith("+")) {
                            phone = "+" + phone;
                        }
                        phoneTextView.setText(PhoneFormat.getInstance().format(phone));
                    } else {
                        phoneTextView.setText("Unknown");
                    }
                    TLRPC.FileLocation photo = null;
                    if (contactUser.photo != null) {
                        photo = contactUser.photo.photo_small;
                    }
                    int placeHolderId = Utilities.getUserAvatarForId(contactUser.id);
                    contactAvatar.setImage(photo, "50_50", placeHolderId);
                   /* if (contactUser.id != UserConfig.getClientUserId() && ContactsController.getInstance().contactsDict.get(contactUser.id) == null) {
                        addContactView.setVisibility(View.VISIBLE);
                    } else {
                        addContactView.setVisibility(View.GONE);
                    }*/
                } else {
                    nameTextView.setText(Utilities.formatName(message.messageOwner.tl_message.media.first_name, message.messageOwner.tl_message.media.last_name));
                    nameTextView.setTextColor(Utilities.getColorForId(message.messageOwner.tl_message.media.user_id));
                    String phone = message.messageOwner.tl_message.media.phone_number;
                    if (phone != null && phone.length() != 0) {
                        if (message.messageOwner.tl_message.media.user_id != 0 && !phone.startsWith("+")) {
                            phone = "+" + phone;
                        }
                        phoneTextView.setText(PhoneFormat.getInstance().format(phone));
                    } else {
                        phoneTextView.setText("Unknown");
                    }
                    contactAvatar.setImageResource(Utilities.getUserAvatarForId(message.messageOwner.tl_message.media.user_id));
                    addContactView.setVisibility(View.GONE);
                }
            } else if (type == 7) {
                if (unread_to_load == 1) {
                    messageTextView.setText(LocaleController.formatString("OneNewMessage", R.string.OneNewMessage, unread_to_load));
                } else {
                    messageTextView.setText(LocaleController.formatString("FewNewMessages", R.string.FewNewMessages, unread_to_load));
                }
            } else if (type == 8 || type == 9) {
                TLRPC.Document document = message.messageOwner.tl_message.media.document;
                if (document instanceof TLRPC.TL_document || document instanceof TLRPC.TL_documentEncrypted) {
                    nameTextView.setText(message.messageOwner.tl_message.media.document.file_name);

                    String fileName = message.getFileName();
                    int idx = fileName.lastIndexOf(".");
                    String ext = null;
                    if (idx != -1) {
                        ext = fileName.substring(idx + 1);
                    }
                    if (ext == null || ext.length() == 0) {
                        ext = message.messageOwner.tl_message.media.document.mime_type;
                    }
                    ext = ext.toUpperCase();
                    if (document.size < 1024) {
                        phoneTextView.setText(String.format("%d B %s", document.size, ext));
                    } else if (document.size < 1024 * 1024) {
                        phoneTextView.setText(String.format("%.1f KB %s", document.size / 1024.0f, ext));
                    } else {
                        phoneTextView.setText(String.format("%.1f MB %s", document.size / 1024.0f / 1024.0f, ext));
                    }
                    if (document.thumb instanceof TLRPC.TL_photoSize) {
                        contactAvatar.setImage(document.thumb.location, "50_50", type == 8 ? R.drawable.doc_green : R.drawable.doc_blue);
                    } else if (document.thumb instanceof TLRPC.TL_photoCachedSize) {
                        contactAvatar.setImage(document.thumb.location, "50_50", type == 8 ? R.drawable.doc_green : R.drawable.doc_blue);
                    } else {
                        if (type == 8) {
                            contactAvatar.setImageResource(R.drawable.doc_green);
                        } else {
                            contactAvatar.setImageResource(R.drawable.doc_blue);
                        }
                    }
                } else {
                    nameTextView.setText("Error");
                    phoneTextView.setText("Error");
                    if (type == 8) {
                        contactAvatar.setImageResource(R.drawable.doc_green);
                    } else {
                        contactAvatar.setImageResource(R.drawable.doc_blue);
                    }
                }
            }

           /* if (message.messageOwner.id < 0 && message.messageOwner.getSend_state() != MessagesController.MESSAGE_SEND_STATE_SEND_ERROR && message.messageOwner.getSend_state() != MessagesController.MESSAGE_SEND_STATE_SENT) {
                if (MessagesController.getInstance().sendingMessages.get(message.messageOwner.id) == null) {
                    message.messageOwner.getSend_state() = MessagesController.MESSAGE_SEND_STATE_SEND_ERROR;
                }
            }*/

            if (message.isFromMe()) {
                if (halfCheckImage != null) {
                    if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SENDING) {
                        checkImage.setVisibility(View.INVISIBLE);
                        halfCheckImage.setImageResource(R.drawable.msg_clock);
                        halfCheckImage.setVisibility(View.VISIBLE);
                        if (actionView != null) {
                            if (actionView != null) {
                                actionView.setVisibility(View.VISIBLE);
                            }
                            Float progress = null;
                            if (message.messageOwner.tl_message.attachPath != null && message.messageOwner.tl_message.attachPath.length() != 0) {
                                progress = FileLoader.getInstance().fileProgresses.get(message.messageOwner.tl_message.attachPath);
                                progressByTag.put((Integer) actionProgress.getTag(), message.messageOwner.tl_message.attachPath);
                                progressBarMap.put(message.messageOwner.tl_message.attachPath, actionProgress);
                            }
                            if (progress != null) {
                                actionProgress.setProgress((int) (progress * 100));
                            } else {
                                actionProgress.setProgress(0);
                            }
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.GONE);
                        }
                    } else if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR) {
                        halfCheckImage.setVisibility(View.VISIBLE);
                        halfCheckImage.setImageResource(R.drawable.msg_warning);
                        if (checkImage != null) {
                            checkImage.setVisibility(View.INVISIBLE);
                        }
                        if (actionView != null) {
                            actionView.setVisibility(View.GONE);
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.GONE);
                        }
                    } else if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SENT) {
                        if (message.messageOwner.getRead_state() != 1) {
                            halfCheckImage.setVisibility(View.VISIBLE);
                            checkImage.setVisibility(View.VISIBLE);
                            halfCheckImage.setImageResource(R.drawable.msg_halfcheck);
                        } else {
                            halfCheckImage.setVisibility(View.VISIBLE);
                            checkImage.setVisibility(View.INVISIBLE);
                            halfCheckImage.setImageResource(R.drawable.msg_check);
                        }
                        if (actionView != null) {
                            actionView.setVisibility(View.GONE);
                        }
                        if (actionAttachButton != null) {
                            actionAttachButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            if (message.messageOwner.getSend_state() == null) {
                message.messageOwner.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENT);
            }
            if (message.type == 8 || message.type == 9) {
                Integer tag = (Integer) actionProgress.getTag();
                String file = progressByTag.get(tag);
                if (file != null) {
                    removeFromloadingFile(file, actionProgress);
                }
                if (message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SENDING && message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR) {
                    if (file != null) {
                        progressBarMap.remove(file);
                    }
                    String fileName = message.getFileName();
                    boolean load = false;
                    if (message.type != 2 && message.type != 3 && message.messageOwner.tl_message.attachPath != null && message.messageOwner.tl_message.attachPath.length() != 0) {
                        File f = new File(message.messageOwner.tl_message.attachPath);
                        if (f.exists()) {
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 8 || message.type == 9) {
                                    actionAttachButton.setText(LocaleController.getString("Open", R.string.Open));
                                }
                            }
                            if (actionView != null) {
                                actionView.setVisibility(View.GONE);
                            }
                        } else {
                            load = true;
                        }
                    }
                    if (load && message.messageOwner.tl_message.attachPath != null && message.messageOwner.tl_message.attachPath.length() != 0 || !load && (message.messageOwner.tl_message.attachPath == null || message.messageOwner.tl_message.attachPath.length() == 0)) {
                        File cacheFile = null;
                        if ((cacheFile = new File(OSUtilities.getCacheDir(), fileName)).exists()) {
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 8 || message.type == 9) {
                                    actionAttachButton.setText(LocaleController.getString("Open", R.string.Open));
                                }
                            }
                            if (actionView != null) {
                                actionView.setVisibility(View.GONE);
                            }
                            load = false;
                        } else {
                            load = true;
                        }
                    }
                    if (load) {
                        Float progress = FileLoader.getInstance().fileProgresses.get(fileName);
                        if (loadingFile.containsKey(fileName) || progress != null) {
                            if (progress != null) {
                                actionProgress.setProgress((int) (progress * 100));
                            } else {
                                actionProgress.setProgress(0);
                            }
                            progressByTag.put((Integer) actionProgress.getTag(), fileName);
                            addToLoadingFile(fileName, actionProgress);
                            if (actionView != null) {
                                actionView.setVisibility(View.VISIBLE);
                            }
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.GONE);
                            }
                        } else {
                            if (actionView != null) {
                                actionView.setVisibility(View.GONE);
                            }
                            if (actionAttachButton != null) {
                                actionAttachButton.setVisibility(View.VISIBLE);
                                if (message.type == 8 || message.type == 9) {
                                    actionAttachButton.setText(LocaleController.getString("DOWNLOAD", R.string.DOWNLOAD));
                                }
                            }
                        }
                    }
                }
                if (message.type == 8 || message.type == 9) {
                    int width;
                    if (currentChat != null && type != 8) {
                        if (actionView.getVisibility() == View.VISIBLE) {
                            width = OSUtilities.displaySize.x - OSUtilities.dp(290);
                        } else {
                            width = OSUtilities.displaySize.x - OSUtilities.dp(270);
                        }
                    } else {
                        if (actionView.getVisibility() == View.VISIBLE) {
                            width = OSUtilities.displaySize.x - OSUtilities.dp(240);
                        } else {
                            width = OSUtilities.displaySize.x - OSUtilities.dp(220);
                        }
                    }
                    nameTextView.setMaxWidth(width);
                    phoneTextView.setMaxWidth(width);
                }
            }


        }

        public ChatListRowHolderEx(View view, int type) {
            avatarImageView = (BackupImageView) view.findViewById(R.id.chat_group_avatar_image);
            nameTextView = (TextView) view.findViewById(R.id.chat_user_group_name);
            messageLayoutAction = (MessageActionLayout) view.findViewById(R.id.message_action_layout);
            timeTextView = (TextView) view.findViewById(R.id.chat_time_text);
            photoImage = (BackupImageView2) view.findViewById(R.id.chat_photo_image);
            halfCheckImage = (ImageView) view.findViewById(R.id.chat_row_halfcheck);
            checkImage = (ImageView) view.findViewById(R.id.chat_row_check);
            actionAttachButton = (TextView) view.findViewById(R.id.chat_view_action_button);
            messageTextView = (TextView) view.findViewById(R.id.chat_message_text);
            videoTimeText = (TextView) view.findViewById(R.id.chat_video_time);
            actionView = view.findViewById(R.id.chat_view_action_layout);
            actionProgress = (ProgressBar) view.findViewById(R.id.chat_view_action_progress);
            actionCancelButton = (ImageView) view.findViewById(R.id.chat_view_action_cancel_button);
            phoneTextView = (TextView) view.findViewById(R.id.phone_text_view);
            contactAvatar = (BackupImageView2) view.findViewById(R.id.contact_avatar);
            contactView = view.findViewById(R.id.shared_layout);
            addContactButton = (ImageView) view.findViewById(R.id.add_contact_button);
            addContactView = view.findViewById(R.id.add_contact_view);
            chatBubbleView = view.findViewById(R.id.chat_bubble_layout);
            if (messageTextView != null) {
                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MessagesController.getInstance().fontSize);
            }

            if (actionProgress != null) {
                actionProgress.setTag(progressTag);
                progressTag++;
            }

            if (type != 2 && type != 3) {
                if (actionView != null) {
                    //   if (isCustomTheme) {
                    actionView.setBackgroundResource(R.drawable.system_black);
                    //  } else {
                    //     actionView.setBackgroundResource(R.drawable.system_blue);
                    //  }
                }
            }

            if (messageLayoutAction != null) {
                //   if (isCustomTheme) {
                messageLayoutAction.setBackgroundResource(R.drawable.system_black);
                //  } else {
                //      messageLayoutAction.setBackgroundResource(R.drawable.system_blue);
                //  }
            }

            if (addContactButton != null) {
                addContactButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionMode != null) {
                            processRowSelect(view);
                            return;
                        }
                        Bundle args = new Bundle();
                        args.putInt("user_id", message.messageOwner.tl_message.media.user_id);
                        args.putString("phone", message.messageOwner.tl_message.media.phone_number);
                        //  presentFragment(new ContactAddActivity(args));
                    }
                });

                addContactButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        createMenu(v, false);
                        return true;
                    }
                });
            }

            if (contactView != null) {
                contactView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (message.type == 8 || message.type == 9) {
                            processOnClick(view);
                        } else if (message.type == 12 || message.type == 13) {
                            if (mActionMode != null) {
                                processRowSelect(view);
                                return;
                            }
                            if (message.messageOwner.tl_message.media.user_id != UserConfig.getClientUserId()) {
                                TLRPC.User user = null;
                                if (message.messageOwner.tl_message.media.user_id != 0) {
                                    user = ContactsController.getInstance().friendsDict.get(message.messageOwner.getJid());
                                }
                                if (user != null) {
                                    Bundle args = new Bundle();
                                    args.putInt("user_id", message.messageOwner.tl_message.media.user_id);
                                    UserProfileActivity userProfileActivity = new UserProfileActivity();
                                    userProfileActivity.setArguments(args);
                                    ((LaunchActivity) getParentActivity()).presentFragment(userProfileActivity, "profile", false);
                                } else {
                                    if (message.messageOwner.tl_message.media.phone_number == null || message.messageOwner.tl_message.media.phone_number.length() == 0) {
                                        return;
                                    }
                                    if (getParentActivity() == null) {
                                        return;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                    builder.setItems(new CharSequence[]{LocaleController.getString("Copy", R.string.Copy), LocaleController.getString("Call", R.string.Call)}, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 1) {
                                                        try {
                                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + message.messageOwner.tl_message.media.phone_number));
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            getParentActivity().startActivity(intent);
                                                        } catch (Exception e) {
                                                            FileLog.e("yahala", e);
                                                        }
                                                    } else if (i == 0) {
                                                        int sdk = android.os.Build.VERSION.SDK_INT;
                                                        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                                                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                                            clipboard.setText(message.messageOwner.tl_message.media.phone_number);
                                                        } else {
                                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                                            android.content.ClipData clip = android.content.ClipData.newPlainText("label", message.messageOwner.tl_message.media.phone_number);
                                                            clipboard.setPrimaryClip(clip);
                                                        }
                                                    }
                                                }
                                            }
                                    );
                                    showAlertDialog(builder);
                                }
                            }
                        }
                    }
                });

                contactView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        createMenu(v, false);
                        return true;
                    }
                });
            }

            if (contactAvatar != null) {
                contactAvatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
            }

            if (actionAttachButton != null) {
                actionAttachButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        processOnClick(view);
                    }
                });
            }

            if (avatarImageView != null) {
                avatarImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mActionMode != null) {
                            processRowSelect(view);
                            return;
                        }
                        if (message != null) {
                            Bundle args = new Bundle();
                            args.putString("user_id", message.messageOwner.getJid());
                            UserProfileActivity userProfileActivity = new UserProfileActivity();
                            userProfileActivity.setArguments(args);
                            ((LaunchActivity) getParentActivity()).presentFragment(userProfileActivity, "profile", false);
                        }
                    }
                });
            }

            if (actionCancelButton != null) {
                actionCancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (message != null) {
                            FileLog.e(".getSend_state()", "cancelSendingMessage");
                            Integer tag = (Integer) actionProgress.getTag();
                            if (message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SENT) {

                                XMPPManager.getInstance().cancelSendingMessage(message);
                                String file = progressByTag.get(tag);
                                if (file != null) {
                                    progressBarMap.remove(file);
                                }
                            } else if (message.type == 8 || message.type == 9) {
                                String file = progressByTag.get(tag);
                                if (file != null) {
                                    loadingFile.remove(file);
                                    if (message.type == 8 || message.type == 9) {
                                        FileLoader.getInstance().cancelLoadFile(null, null, message.messageOwner.tl_message.media.document, null);
                                    }
                                    updateVisibleRows();
                                }
                            }
                        }
                    }
                });
            }

            if (photoImage != null) {
                photoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        processOnClick(view);
                    }
                });

                photoImage.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        createMenu(v, false);
                        return true;
                    }
                });
            }
        }

        private void processOnClick(View view) {
            if (mActionMode != null) {
                processRowSelect(view);
                return;
            }
            if (message != null) {
                if (message.type == 11) {
                    PhotoViewer.getInstance().setParentActivity(getActivity());
                    PhotoViewer.getInstance().openPhoto(message, ChatActivity.this);
                } else if (message.type == 8 || message.type == 9) {
                    File f = null;
                    String fileName = message.getFileName();
                    if (message.messageOwner.tl_message.attachPath != null && message.messageOwner.tl_message.attachPath.length() != 0) {
                        f = new File(message.messageOwner.tl_message.attachPath);
                    }
                    if (f == null || f != null && !f.exists()) {
                        f = new File(OSUtilities.getCacheDir(), fileName);
                    }
                    if (f != null && f.exists()) {
                        String realMimeType = null;
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (message.type == 8 || message.type == 9) {
                                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                                int idx = fileName.lastIndexOf(".");
                                if (idx != -1) {
                                    String ext = fileName.substring(idx + 1);
                                    realMimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                                    if (realMimeType != null) {
                                        intent.setDataAndType(Uri.fromFile(f), realMimeType);
                                    } else {
                                        intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                    }
                                } else {
                                    intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                }
                            }
                            if (realMimeType != null) {
                                try {
                                    getParentActivity().startActivity(intent);
                                } catch (Exception e) {
                                    intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                    getParentActivity().startActivity(intent);
                                }
                            } else {
                                getParentActivity().startActivity(intent);
                            }
                        } catch (Exception e) {
                            alertUserOpenError(message);
                        }
                    } else {
                        if (message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR && message.messageOwner.getSend_state() != XMPPManager.MESSAGE_SEND_STATE_SENDING || message.messageOwner.getOut() != 1) {
                            if (!loadingFile.containsKey(fileName)) {
                                progressByTag.put((Integer) actionProgress.getTag(), fileName);
                                addToLoadingFile(fileName, actionProgress);
                                if (message.type == 8 || message.type == 9) {
                                    FileLoader.getInstance().loadFile(null, null, message.messageOwner.tl_message.media.document, null);
                                }
                                updateVisibleRows();
                            }
                        } else {
                            if (message.messageOwner.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SEND_ERROR) {
                                createMenu(view, false);
                            }
                        }
                    }
                }
            }
        }
    }
}
