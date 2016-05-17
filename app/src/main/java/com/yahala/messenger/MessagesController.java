package com.yahala.messenger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.text.Html;
import android.util.SparseArray;

import com.yahala.android.OSUtilities;
import com.yahala.android.LocaleController;
import com.yahala.messenger.R;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;
import com.yahala.xmpp.MessagesStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesController implements NotificationCenter.NotificationCenterDelegate {
    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;
    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_USER_PRINT = 64;
    public static final int UPDATE_MASK_USER_PHONE = 128;
    public static final int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    public static final int UPDATE_MASK_ALL = UPDATE_MASK_AVATAR | UPDATE_MASK_STATUS | UPDATE_MASK_NAME | UPDATE_MASK_CHAT_AVATAR | UPDATE_MASK_CHAT_NAME | UPDATE_MASK_CHAT_MEMBERS | UPDATE_MASK_USER_PRINT | UPDATE_MASK_USER_PHONE | UPDATE_MASK_READ_DIALOG_MESSAGE;
    public static final int didReceivedNewMessages = 1;
    public static final int updateInterfaces = 3;
    public static final int dialogsNeedReload = 4;
    public static final int closeChats = 5;
    public static final int messagesDeleted = 6;
    public static final int messagesReaded = 7;
    public static final int messagesDidLoaded = 8;
    public static final int messageReceivedByAck = 9;
    public static final int messageReceivedByServer = 10;
    public static final int messageSendError = 11;
    public static final int reloadSearchResults = 12;
    public static final int contactsDidLoaded = 13;
    public static final int chatDidCreated = 15;
    public static final int chatDidFailCreate = 16;
    public static final int chatInfoDidLoaded = 17;
    public static final int mediaDidLoaded = 18;
    public static final int mediaCountDidLoaded = 20;
    public static final int encryptedChatUpdated = 21;
    public static final int messagesReadedEncrypted = 22;
    public static final int encryptedChatCreated = 23;
    public static final int userPhotosLoaded = 24;
    public static final int removeAllMessagesFromDialog = 25;
    public static SecureRandom random = new SecureRandom();
    public static volatile boolean isScreenOn = true;
    private static volatile MessagesController Instance = null;
    public ConcurrentHashMap<Integer, TLRPC.Chat> chats = new ConcurrentHashMap<Integer, TLRPC.Chat>(100, 1.0f, 2);
    public ConcurrentHashMap<Integer, TLRPC.EncryptedChat> encryptedChats = new ConcurrentHashMap<Integer, TLRPC.EncryptedChat>(10, 1.0f, 2);
    public ConcurrentHashMap<String, TLRPC.User> users = new ConcurrentHashMap<String, TLRPC.User>(100, 1.0f, 2);
    public ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<TLRPC.TL_dialog>();
    public ArrayList<TLRPC.TL_dialog> dialogsServerOnly = new ArrayList<TLRPC.TL_dialog>();
    public ConcurrentHashMap<Long, TLRPC.TL_dialog> dialogs_dict = new ConcurrentHashMap<Long, TLRPC.TL_dialog>(100, 1.0f, 2);
    public SparseArray<MessageObject> dialogMessage = new SparseArray<MessageObject>();
    public ConcurrentHashMap<Long, ArrayList<PrintingUser>> printingUsers = new ConcurrentHashMap<Long, ArrayList<PrintingUser>>(100, 1.0f, 2);
    public HashMap<Long, CharSequence> printingStrings = new HashMap<Long, CharSequence>();
    public SparseArray<MessageObject> sendingMessages = new SparseArray<MessageObject>();
    public SparseArray<TLRPC.User> hidenAddToContacts = new SparseArray<TLRPC.User>();
    public ArrayList<TLRPC.Update> delayedEncryptedChatUpdates = new ArrayList<TLRPC.Update>();
    public int totalDialogsCount = 0;
    public boolean loadingDialogs = false;
    public boolean dialogsEndReached = false;
    public boolean gettingDifference = false;
    public boolean gettingDifferenceAgain = false;
    public boolean updatingState = false;
    public boolean firstGettingTask = false;
    public boolean registeringForPush = false;
    public boolean enableJoined = true;
    public int fontSize = OSUtilities.dp(16);
    public long scheduleContactsReload = 0;
    public MessageObject currentPushMessage;
    public long openned_dialog_id;
    private HashMap<String, ArrayList<DelayedMessage>> delayedMessages = new HashMap<String, ArrayList<DelayedMessage>>();
    private SparseArray<TLRPC.EncryptedChat> acceptingChats = new SparseArray<TLRPC.EncryptedChat>();
    private ArrayList<TLRPC.Updates> updatesQueue = new ArrayList<TLRPC.Updates>();
    private ArrayList<Long> pendingEncMessagesToDelete = new ArrayList<Long>();
    private long updatesStartWaitTime = 0;
    private boolean startingSecretChat = false;
    private boolean gettingNewDeleteTask = false;
    private int currentDeletingTaskTime = 0;
    private Long currentDeletingTask = null;
    private ArrayList<Integer> currentDeletingTaskMids = null;
    private long lastSoundPlay = 0;
    private long lastStatusUpdateTime = 0;
    private long statusRequest = 0;
    private int statusSettingState = 0;
    private boolean offlineSent = false;
    private String uploadingAvatar = null;
    private SoundPool soundPool;
    private int sound;

    public MessagesController() {
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("tmessages", "screen state = " + isScreenOn);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        MessagesStorage storage = MessagesStorage.getInstance();
        //  NotificationCenter.getInstance().addObserver(this, FileLoader.FileDidUpload);
        // NotificationCenter.getInstance().addObserver(this, FileLoader.FileDidFailUpload);
        NotificationCenter.getInstance().addObserver(this, messageReceivedByServer);
        addSupportUser();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        enableJoined = preferences.getBoolean("EnableContactJoined", true);
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        fontSize = preferences.getInt("fons_size", 16);

        try {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
            //sound = soundPool.load(ApplicationLoader.applicationContext, R.raw.sound_a, 1);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public static MessagesController getInstance() {
        MessagesController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MessagesController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MessagesController();
                }
            }
        }
        return localInstance;
    }

    public static TLRPC.InputUser getInputUser(TLRPC.User user) {
        if (user == null) {
            return null;
        }
        TLRPC.InputUser inputUser = null;
        if (user.id == UserConfig.clientUserId) {
            inputUser = new TLRPC.TL_inputUserSelf();
        } else if (user instanceof TLRPC.TL_userForeign || user instanceof TLRPC.TL_userRequest) {
            inputUser = new TLRPC.TL_inputUserForeign();
            inputUser.user_id = user.id;
            inputUser.access_hash = user.access_hash;
        } else {
            inputUser = new TLRPC.TL_inputUserContact();
            inputUser.user_id = user.id;
        }
        return inputUser;
    }

    public void addSupportUser() {
        TLRPC.TL_userForeign user = new TLRPC.TL_userForeign();
        user.phone = "333";
        user.id = 333000;
        user.jid = "962797982823";
        user.first_name = "Yahala";
        user.last_name = "";
        user.status = null;
        user.photo = new TLRPC.TL_userProfilePhotoEmpty();
        users.put(user.jid, user);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == messageReceivedByServer) {
         /*   Integer msgId = (Integer)args[0];
            MessageObject obj = dialogMessage.get(msgId);
            if (obj != null) {
                Integer newMsgId = (Integer)args[1];
                dialogMessage.remove(msgId);
                dialogMessage.put(newMsgId, obj);
                obj.messageOwner.id = newMsgId;
                obj.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SENT;

                long uid;
                if (obj.messageOwner.to_id.chat_id != 0) {
                    uid = -obj.messageOwner.to_id.chat_id;
                } else {
                    if (obj.messageOwner.to_id.user_id == UserConfig.clientUserId) {
                        obj.messageOwner.to_id.user_id = obj.messageOwner.from_id;
                    }
                    uid = obj.messageOwner.to_id.user_id;
                }

                TLRPC.TL_dialog dialog = dialogs_dict.get(uid);
                if (dialog != null) {
                    if (dialog.top_message == msgId) {
                        dialog.top_message = newMsgId;
                    }
                }
                NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
            }*/
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        //  NotificationCenter.getInstance().removeObserver(this, FileLoader.FileDidUpload);
        //     NotificationCenter.getInstance().removeObserver(this, FileLoader.FileDidFailUpload);
        NotificationCenter.getInstance().removeObserver(this, messageReceivedByServer);
    }

    public void cleanUp() {
        ContactsController.getInstance().cleanup();
        //  MediaController.getInstance().cleanup();

        dialogs_dict.clear();
        dialogs.clear();
        dialogsServerOnly.clear();
        acceptingChats.clear();
        users.clear();
        chats.clear();
        sendingMessages.clear();
        delayedMessages.clear();
        dialogMessage.clear();
        printingUsers.clear();
        printingStrings.clear();
        totalDialogsCount = 0;
        hidenAddToContacts.clear();
        updatesQueue.clear();
        pendingEncMessagesToDelete.clear();
        delayedEncryptedChatUpdates.clear();

        updatesStartWaitTime = 0;
        currentDeletingTaskTime = 0;
        scheduleContactsReload = 0;
        currentDeletingTaskMids = null;
        gettingNewDeleteTask = false;
        currentDeletingTask = null;
        loadingDialogs = false;
        dialogsEndReached = false;
        gettingDifference = false;
        gettingDifferenceAgain = false;
        firstGettingTask = false;
        updatingState = false;
        lastStatusUpdateTime = 0;
        offlineSent = false;
        registeringForPush = false;
        uploadingAvatar = null;
        startingSecretChat = false;
        statusRequest = 0;
        statusSettingState = 0;
        addSupportUser();
    }

    public void didAddedNewTask(final int minDate) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (currentDeletingTask == null && !gettingNewDeleteTask || currentDeletingTaskTime != 0 && minDate < currentDeletingTaskTime) {
                    getNewDeleteTask(null);
                }
            }
        });
    }

    public void getNewDeleteTask(final Long oldTask) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                gettingNewDeleteTask = true;
                //MessagesStorage.getInstance().getNewTask(oldTask);
            }
        });
    }

    private void checkDeletingTask() {
        //int currentServerTime = ConnectionsManager.getInstance().getCurrentTime();

       /*if (currentDeletingTask != null && currentDeletingTaskTime != 0 && currentDeletingTaskTime <= currentServerTime) {
            currentDeletingTaskTime = 0;
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    deleteMessages(currentDeletingTaskMids, null, null);

                    Utilities.stageQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            getNewDeleteTask(currentDeletingTask);
                            currentDeletingTaskTime = 0;
                            currentDeletingTask = null;
                        }
                    });
                }
            });
        }*/
    }

    public void processLoadedDeleteTask(final Long taskId, final int taskTime, final ArrayList<Integer> messages) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                gettingNewDeleteTask = false;
                if (taskId != null) {
                    currentDeletingTaskTime = taskTime;
                    currentDeletingTask = taskId;
                    currentDeletingTaskMids = messages;

                    checkDeletingTask();
                } else {
                    currentDeletingTaskTime = 0;
                    currentDeletingTask = null;
                    currentDeletingTaskMids = null;
                }
            }
        });
    }

    public void deleteAllAppAccounts() {
        try {
            AccountManager am = AccountManager.get(ApplicationLoader.applicationContext);
            Account[] accounts = am.getAccountsByType("org.telegram.messenger.account");
            for (Account c : accounts) {
                am.removeAccount(c, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadUserPhotos(final int uid, final int offset, final int count, final long max_id, final boolean fromCache, final int classGuid) {
        if (fromCache) {
            //MessagesStorage.getInstance().getUserPhotos(uid, offset, count, max_id, classGuid);
        } else {
            //TODO load user photo user from server
        }
    }

    public void processLoadedUserPhotos(final TLRPC.photos_Photos res, final int uid, final int offset, final int count, final long max_id, final boolean fromCache, final int classGuid) {
        if (!fromCache) {
            // MessagesStorage.getInstance().putUserPhotos(uid, res);
        } else if (res == null || res.photos.isEmpty()) {
            loadUserPhotos(uid, offset, count, max_id, false, classGuid);
            return;
        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(userPhotosLoaded, uid, offset, count, fromCache, classGuid, res.photos);
            }
        });
    }

    public void processLoadedMedia(final TLRPC.messages_Messages res, final long uid, int offset, int count, int max_id, final boolean fromCache, final int classGuid) {
        /*int lower_part = (int)uid;
        if (fromCache && res.messages.isEmpty() && lower_part != 0) {
            loadMedia(uid, offset, count, max_id, false, classGuid);
        } else {
            if (!fromCache) {
                MessagesStorage.getInstance().putUsersAndChats(res.users, res.chats, true, true);
                MessagesStorage.getInstance().putMedia(uid, res.messages);
            }

            final HashMap<String, TLRPC.User> usersLocal = new HashMap<String, TLRPC.User>();
            for (TLRPC.User u : res.users) {
                usersLocal.put(u.jid, u);
            }
            final ArrayList<MessageObject> objects = new ArrayList<MessageObject>();
            for (TLRPC.Message message : res.messages) {
                objects.add(new MessageObject(message, usersLocal));
            }

            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    int totalCount;
                    if (res instanceof TLRPC.TL_messages_messagesSlice) {
                        totalCount = res.count;
                    } else {
                        totalCount = res.messages.size();
                    }
                    for (TLRPC.User user : res.users) {
                        if (fromCache) {
                            users.putIfAbsent(user.jid, user);
                        } else {
                            users.put(user.jid, user);
                            if (user.id == UserConfig.clientUserId) {
                                UserConfig.currentUser = user;
                            }
                        }
                    }
                    for (TLRPC.Chat chat : res.chats) {
                        if (fromCache) {
                            chats.putIfAbsent(chat.id, chat);
                        } else {
                            chats.put(chat.id, chat);
                        }
                    }
                    NotificationCenter.getInstance().postNotificationName(mediaDidLoaded, uid, totalCount, objects, fromCache, classGuid);
                }
            });

        }*/
    }

    public void loadMedia(final String jid, final int offset, final int count, final Long max_id, final boolean fromCache, final int classGuid) {


        // MessagesStorage.getInstance().loadMedia(uid, offset, count, max_id, classGuid);


    }

    public void processLoadedMediaCount(final int count, final String jid, final int classGuid, final boolean fromCache) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {


                if (fromCache && count == -1) {
                    NotificationCenter.getInstance().postNotificationName(mediaCountDidLoaded, jid, 0, fromCache);
                } else {
                    NotificationCenter.getInstance().postNotificationName(mediaCountDidLoaded, jid, count, fromCache);
                }
            }

        });
    }

    public void getMediaCount(final String jid, final int classGuid, boolean fromCache) {


        //   MessagesStorage.getInstance().getMediaCount(uid, classGuid);

    }

    public void uploadAndApplyUserAvatar(TLRPC.PhotoSize bigPhoto) {
        if (bigPhoto != null) {
            uploadingAvatar = OSUtilities.getCacheDir() + "/" + bigPhoto.location.volume_id + "_" + bigPhoto.location.local_id + ".jpg";
            // FileLoader.getInstance().uploadFile(uploadingAvatar, null, null);
            //TODO Upload user avatar
        }
    }

    public void deleteMessages(ArrayList<Integer> messages, ArrayList<Long> randoms, TLRPC.EncryptedChat encryptedChat) {
        for (Integer id : messages) {
            MessageObject obj = dialogMessage.get(id);
            if (obj != null) {
                obj.deleted = true;
            }
        }
        // MessagesStorage.getInstance().markMessagesAsDeleted(messages, true);
        //  MessagesStorage.getInstance().updateDialogsWithDeletedMessages(messages, true);
        NotificationCenter.getInstance().postNotificationName(messagesDeleted, messages);


        ArrayList<Integer> toSend = new ArrayList<Integer>();
        for (Integer mid : messages) {
            if (mid > 0) {
                toSend.add(mid);
            }
        }
        if (toSend.isEmpty()) {
            return;
        }


    }

    public void deleteDialog(final long did, int offset, final boolean onlyHistory) {
        TLRPC.TL_dialog dialog = dialogs_dict.get(did);
        if (dialog != null) {
            int lower_part = (int) did;

            if (offset == 0) {
                if (!onlyHistory) {
                    dialogs.remove(dialog);
                    dialogsServerOnly.remove(dialog);
                    dialogs_dict.remove(did);
                    totalDialogsCount--;
                }
                dialogMessage.remove(dialog.top_message);
                //  MessagesStorage.getInstance().deleteDialog(did, onlyHistory);
                NotificationCenter.getInstance().postNotificationName(removeAllMessagesFromDialog, did);
                NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
            }


        }
    }

    public void loadChatInfo(final int chat_id) {
        // MessagesStorage.getInstance().loadChatInfo(chat_id);
    }

    public void processChatInfo(final int chat_id, final TLRPC.ChatParticipants info, final ArrayList<TLRPC.User> usersArr, final boolean fromCache) {
        if (info == null && fromCache) {


            //TODO get chat info from database
                  /*  Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            for (TLRPC.User user : res.users) {
                                users.put(user.id, user);
                                if (user.id == UserConfig.clientUserId) {
                                    UserConfig.currentUser = user;
                                }
                            }
                            for (TLRPC.Chat chat : res.chats) {
                                chats.put(chat.id, chat);
                            }
                            NotificationCenter.getInstance().postNotificationName(chatInfoDidLoaded, chat_id, res.full_chat.participants);
                        }
                    });
                }*/

        /*} else {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (TLRPC.User user : usersArr) {
                        if (fromCache) {
                            users.putIfAbsent(user.id, user);
                        } else {
                            users.put(user.jid, user);
                            if (user.jid == UserConfig.clientUserId) {
                                UserConfig.currentUser = user;
                            }
                        }
                    }
                    NotificationCenter.getInstance().postNotificationName(chatInfoDidLoaded, chat_id, info);
                }
            });*/
        }
    }

    public void updateTimerProc() {
        long currentTime = System.currentTimeMillis();

        checkDeletingTask();

       /* if (UserConfig.clientUserId != 0) {
            if (scheduleContactsReload != 0 && currentTime > scheduleContactsReload) {
                ContactsController.getInstance().performSyncPhoneBook(ContactsController.getInstance().getContactsCopy(ContactsController.getInstance().contactsBook), true, false, true);
                scheduleContactsReload = 0;
            }

            if (ApplicationLoader.lastPauseTime == 0) {
                if (statusSettingState != 1 && (lastStatusUpdateTime == 0 || lastStatusUpdateTime <= System.currentTimeMillis() - 55000 || offlineSent)) {
                    statusSettingState = 1;

                    if (statusRequest != 0) {
                        ConnectionsManager.getInstance().cancelRpc(statusRequest, true);
                    }

                    TLRPC.TL_account_updateStatus req = new TLRPC.TL_account_updateStatus();
                    req.offline = false;
                    statusRequest = ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                        @Override
                        public void run(TLObject response, TLRPC.TL_error error) {
                            if (error == null) {
                                lastStatusUpdateTime = System.currentTimeMillis();
                                offlineSent = false;
                                statusSettingState = 0;
                            } else {
                                if (lastStatusUpdateTime != 0) {
                                    lastStatusUpdateTime += 5000;
                                }
                            }
                            statusRequest = 0;
                        }
                    }, null, true, RPCRequest.RPCRequestClassGeneric);
                }
            } else if (statusSettingState != 2 && !offlineSent && ApplicationLoader.lastPauseTime <= System.currentTimeMillis() - 2000) {
                statusSettingState = 2;
                if (statusRequest != 0) {
                    ConnectionsManager.getInstance().cancelRpc(statusRequest, true);
                }
                TLRPC.TL_account_updateStatus req = new TLRPC.TL_account_updateStatus();
                req.offline = true;
                statusRequest = ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            offlineSent = true;
                        } else {
                            if (lastStatusUpdateTime != 0) {
                                lastStatusUpdateTime += 5000;
                            }
                        }
                        statusRequest = 0;
                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric);
            }

            if (updatesStartWaitTime != 0 && updatesStartWaitTime + 1500 < currentTime) {
                FileLog.e("tmessages", "UPDATES WAIT TIMEOUT - CHECK QUEUE");
                processUpdatesQueue(false);
            }
        } else {
            scheduleContactsReload = 0;
        }*/
        if (!printingUsers.isEmpty()) {
            boolean updated = false;
            ArrayList<Long> keys = new ArrayList<Long>(printingUsers.keySet());
            for (int b = 0; b < keys.size(); b++) {
                Long key = keys.get(b);
                ArrayList<PrintingUser> arr = printingUsers.get(key);
                for (int a = 0; a < arr.size(); a++) {
                    PrintingUser user = arr.get(a);
                    if (user.lastTime + 5900 < currentTime) {
                        updated = true;
                        arr.remove(user);
                        a--;
                    }
                }
                if (arr.isEmpty()) {
                    printingUsers.remove(key);
                    keys.remove(b);
                    b--;
                }
            }

            updatePrintingStrings();

            if (updated) {
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(updateInterfaces, UPDATE_MASK_USER_PRINT);
                    }
                });
            }
        }
    }

    public void updatePrintingStrings() {
        final HashMap<Long, CharSequence> newPrintingStrings = new HashMap<Long, CharSequence>();

        ArrayList<Long> keys = new ArrayList<Long>(printingUsers.keySet());
        for (Long key : keys) {
            if (key > 0) {
                newPrintingStrings.put(key, LocaleController.getString("Typing", R.string.Typing));
            } else {
                ArrayList<PrintingUser> arr = printingUsers.get(key);
                int count = 0;
                String label = "";
                for (PrintingUser pu : arr) {
                    TLRPC.User user = users.get(pu.userId);
                    if (user != null) {
                        if (label.length() != 0) {
                            label += ", ";
                        }
                        label += Utilities.formatName(user.first_name, user.last_name);
                        count++;
                    }
                    if (count == 2) {
                        break;
                    }
                }
                if (label.length() != 0) {
                    if (count > 1) {
                        if (arr.size() > 2) {
                            newPrintingStrings.put(key, Html.fromHtml(String.format("%s %s %s", label, String.format(LocaleController.getString("AndMoreTyping", R.string.AndMoreTyping), arr.size() - 2), LocaleController.getString("AreTyping", R.string.AreTyping))));
                        } else {
                            newPrintingStrings.put(key, Html.fromHtml(String.format("%s %s", label, LocaleController.getString("AreTyping", R.string.AreTyping))));
                        }
                    } else {
                        newPrintingStrings.put(key, Html.fromHtml(String.format("%s %s", label, LocaleController.getString("IsTyping", R.string.IsTyping))));
                    }
                }
            }
        }

        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                printingStrings = newPrintingStrings;
            }
        });
    }

    public void sendTyping(long dialog_id, int classGuid) {
        if (dialog_id == 0) {
            return;
        }
        //TODO send Typing
        /*
        int lower_part = (int)dialog_id;
        if (lower_part != 0) {
            TLRPC.TL_messages_setTyping req = new TLRPC.TL_messages_setTyping();
            if (lower_part < 0) {
                req.peer = new TLRPC.TL_inputPeerChat();
                req.peer.chat_id = -lower_part;
            } else {
                TLRPC.User user = users.get(lower_part);
                if (user != null) {
                    if (user instanceof TLRPC.TL_userForeign || user instanceof TLRPC.TL_userRequest) {
                        req.peer = new TLRPC.TL_inputPeerForeign();
                        req.peer.user_id = user.id;
                        req.peer.access_hash = user.access_hash;
                    } else {
                        req.peer = new TLRPC.TL_inputPeerContact();
                        req.peer.user_id = user.id;
                    }
                } else {
                    return;
                }
            }
            req.typing = true;
            long reqId = ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {

                }
            }, null, true, RPCRequest.RPCRequestClassGeneric);
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
        } else {
            int encId = (int)(dialog_id >> 32);
            TLRPC.EncryptedChat chat = encryptedChats.get(encId);
            if (chat.auth_key != null && chat.auth_key.length > 1 && chat instanceof TLRPC.TL_encryptedChat) {
                TLRPC.TL_messages_setEncryptedTyping req = new TLRPC.TL_messages_setEncryptedTyping();
                req.peer = new TLRPC.TL_inputEncryptedChat();
                req.peer.chat_id = chat.id;
                req.peer.access_hash = chat.access_hash;
                req.typing = true;
                long reqId = ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {

                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric);
                ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
            }
        }*/
    }

    public void loadMessages(final long dialog_id, final int offset, final int count, final long max_id, boolean fromCache, long midDate, final int classGuid, boolean from_unread, boolean forward) {
        int lower_part = (int) dialog_id;
        if (fromCache || lower_part == 0) {
            // MessagesStorage.getInstance().getMessages(dialog_id, offset, count, max_id, midDate, classGuid, from_unread, forward);
        }/* else {
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            if (lower_part < 0) {
                req.peer = new TLRPC.TL_inputPeerChat();
                req.peer.chat_id = -lower_part;
            } else {
                TLRPC.User user = users.get(lower_part);
                if (user instanceof TLRPC.TL_userForeign || user instanceof TLRPC.TL_userRequest) {
                    req.peer = new TLRPC.TL_inputPeerForeign();
                    req.peer.user_id = user.id;
                    req.peer.access_hash = user.access_hash;
                } else {
                    req.peer = new TLRPC.TL_inputPeerContact();
                    req.peer.user_id = user.id;
                }
            }
            req.offset = offset;
            req.limit = count;
            req.max_id = max_id;
            long reqId = ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                        processLoadedMessages(res, dialog_id, offset, count, max_id, false, classGuid, 0, 0, 0, 0, false);
                    }
                }
            }, null, true, RPCRequest.RPCRequestClassGeneric);
            ConnectionsManager.getInstance().bindRequestToGuid(reqId, classGuid);
        }*/
    }

    public void processLoadedMessages(final TLRPC.messages_Messages messagesRes, final long dialog_id, final int offset, final int count, final long max_id, final boolean isCache, final int classGuid, final int first_unread, final int last_unread, final int unread_count, final int last_date, final boolean isForward) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
           /*     int lower_id = (int)dialog_id;
                if (!isCache) {
                    MessagesStorage.getInstance().putMessages(messagesRes, dialog_id);
                }
                if (lower_id != 0 && isCache && messagesRes.messages.size() == 0 && !isForward) {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            loadMessages(dialog_id, offset, count, max_id, false, 0, classGuid, false, false);
                        }
                    });
                    return;
                }
                final HashMap<String, TLRPC.User> usersLocal = new HashMap<String, TLRPC.User>();
                for (TLRPC.User u : messagesRes.users) {
                    usersLocal.put(u.jid, u);
                }
                final ArrayList<MessageObject> objects = new ArrayList<MessageObject>();
                for (TLRPC.Message message : messagesRes.messages) {
                    message.dialog_id = dialog_id;
                    objects.add(new MessageObject(message, usersLocal));
                }
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        for (TLRPC.User u : messagesRes.users) {
                            if (isCache) {
                                if (u.id == UserConfig.clientUserId || u.id / 1000 == 333) {
                                    users.put(u.jid, u);
                                } else {
                                    users.putIfAbsent(u.jid, u);
                                }
                            } else {
                                users.put(u.jid, u);
                                if (u.id == UserConfig.clientUserId) {
                                    UserConfig.currentUser = u;
                                }
                            }
                        }
                        for (TLRPC.Chat c : messagesRes.chats) {
                            if (isCache) {
                                chats.putIfAbsent(c.id, c);
                            } else {
                                chats.put(c.id, c);
                            }
                        }
                        NotificationCenter.getInstance().postNotificationName(messagesDidLoaded, dialog_id, offset, count, objects, isCache, first_unread, last_unread, unread_count, last_date, isForward);
                    }
                });*/
            }
        });
    }

    public void loadDialogs(final int offset, final int serverOffset, final int count, boolean fromCache) {
        if (loadingDialogs) {
            return;
        }
        loadingDialogs = true;

        if (fromCache) {
            //MessagesStorage.getInstance().getDialogs(offset, serverOffset, count);
        }/* else {
            TLRPC.TL_messages_getDialogs req = new TLRPC.TL_messages_getDialogs();
            req.offset = serverOffset;
            req.limit = count;
            ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.messages_Dialogs dialogsRes = (TLRPC.messages_Dialogs) response;
                        processLoadedDialogs(dialogsRes, null, offset, serverOffset, count, false, false);
                    }
                }
            }, null, true, RPCRequest.RPCRequestClassGeneric);
        }*/
    }

    public void processDialogsUpdateRead(final HashMap<Long, Integer> dialogsToUpdate) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (HashMap.Entry<Long, Integer> entry : dialogsToUpdate.entrySet()) {
                    TLRPC.TL_dialog currentDialog = dialogs_dict.get(entry.getKey());
                    if (currentDialog != null) {
                        currentDialog.unread_count = entry.getValue();
                    }
                }


            }
        });
    }

    public void processDialogsUpdate(final TLRPC.messages_Dialogs dialogsRes, ArrayList<TLRPC.EncryptedChat> encChats) {
        //TODO tobe checked later
     /*   Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                final HashMap<Long, TLRPC.TL_dialog> new_dialogs_dict = new HashMap<Long, TLRPC.TL_dialog>();
                final HashMap<Integer, MessageObject> new_dialogMessage = new HashMap<Integer, MessageObject>();
                final HashMap<String, TLRPC.User> usersLocal = new HashMap<String, TLRPC.User>();

                for (TLRPC.User u : dialogsRes.users) {
                    usersLocal.put(u.jid, u);
                }

                for (TLRPC.Message m : dialogsRes.messages) {
                    new_dialogMessage.put(m.id, new MessageObject(m, usersLocal));
                }
                for (TLRPC.TL_dialog d : dialogsRes.dialogs) {
                    if (d.last_message_date == 0) {
                        MessageObject mess = new_dialogMessage.get(d.top_message);
                        if (mess != null) {
                            d.last_message_date = mess.messageOwner.date;
                        }
                    }
                    if (d.id == 0) {
                        if (d.peer instanceof TLRPC.TL_peerUser) {
                            d.id = d.peer.user_id;
                        } else if (d.peer instanceof TLRPC.TL_peerChat) {
                            d.id = -d.peer.chat_id;
                        }
                    }
                    new_dialogs_dict.put(d.id, d);
                }

                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        for (TLRPC.User u : dialogsRes.users) {
                            users.putIfAbsent(u.jid, u);
                        }
                        for (TLRPC.Chat c : dialogsRes.chats) {
                            chats.putIfAbsent(c.id, c);
                        }

                        for (HashMap.Entry<Long, TLRPC.TL_dialog> pair : new_dialogs_dict.entrySet()) {
                            long key = pair.getKey();
                            TLRPC.TL_dialog value = pair.getValue();
                            TLRPC.TL_dialog currentDialog = dialogs_dict.get(key);
                            if (currentDialog == null) {
                                dialogs_dict.put(key, value);
                                dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                            } else {
                                currentDialog.unread_count = value.unread_count;
                                MessageObject oldMsg = dialogMessage.get(currentDialog.top_message);
                                if (oldMsg == null || currentDialog.top_message > 0) {
                                    if (oldMsg != null && oldMsg.deleted || value.top_message > currentDialog.top_message) {
                                        dialogs_dict.put(key, value);
                                        if (oldMsg != null) {
                                            dialogMessage.remove(oldMsg.messageOwner.id);
                                        }
                                        dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                                    }
                                } else {
                                    MessageObject newMsg = new_dialogMessage.get(value.top_message);
                                    if (oldMsg.deleted || newMsg == null || newMsg.messageOwner.date > oldMsg.messageOwner.date) {
                                        dialogs_dict.put(key, value);
                                        dialogMessage.remove(oldMsg.messageOwner.id);
                                        dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                                    }
                                }
                            }
                        }

                        dialogs.clear();
                        dialogsServerOnly.clear();
                        dialogs.addAll(dialogs_dict.values());
                        Collections.sort(dialogs, new Comparator<TLRPC.TL_dialog>() {
                            @Override
                            public int compare(TLRPC.TL_dialog tl_dialog, TLRPC.TL_dialog tl_dialog2) {
                                if (tl_dialog.last_message_date == tl_dialog2.last_message_date) {
                                    return 0;
                                } else if (tl_dialog.last_message_date < tl_dialog2.last_message_date) {
                                    return 1;
                                } else {
                                    return -1;
                                }
                            }
                        });
                        for (TLRPC.TL_dialog d : dialogs) {
                            if ((int)d.id != 0) {
                                dialogsServerOnly.add(d);
                            }
                        }
                        NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                    }
                });
            }
        });*/
    }

    public void processLoadedDialogs(final TLRPC.messages_Dialogs dialogsRes, final ArrayList<TLRPC.EncryptedChat> encChats, final int offset, final int serverOffset, final int count, final boolean isCache, final boolean resetEnd) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (isCache && dialogsRes.dialogs.size() == 0) {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                          /*  for (TLRPC.User u : dialogsRes.users) {
                                if (isCache) {
                                    if (u.jid == UserConfig.clientUserJid || u.id / 1000 == 333) {
                                        users.put(u.jid, u);
                                    } else {
                                        users.putIfAbsent(u.jid, u);
                                    }
                                } else {
                                    users.put(u.jid, u);
                                    if (u.id == UserConfig.clientUserId) {
                                        UserConfig.currentUser = u;
                                    }
                                }
                            }
                            loadingDialogs = false;
                            if (resetEnd) {
                                dialogsEndReached = false;
                                NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                            }
                            loadDialogs(offset, serverOffset, count, false);*/
                        }
                    });
                    return;
                }
                final HashMap<Integer, TLRPC.TL_dialog> new_dialogs_dict = new HashMap<Integer, TLRPC.TL_dialog>();
                final HashMap<Integer, MessageObject> new_dialogMessage = new HashMap<Integer, MessageObject>();
                final HashMap<String, TLRPC.User> usersLocal = new HashMap<String, TLRPC.User>();
                int new_totalDialogsCount;

                if (!isCache) {
                    // MessagesStorage.getInstance().putDialogs(dialogsRes);
                }

                if (dialogsRes instanceof TLRPC.TL_messages_dialogsSlice) {
                    TLRPC.TL_messages_dialogsSlice slice = (TLRPC.TL_messages_dialogsSlice) dialogsRes;
                    new_totalDialogsCount = slice.count;
                } else {
                    new_totalDialogsCount = dialogsRes.dialogs.size();
                }

                for (TLRPC.User u : dialogsRes.users) {
                    usersLocal.put(u.jid, u);
                }

                for (TLRPC.Message m : dialogsRes.messages) {
                    // new_dialogMessage.put(m.id, new MessageObject(m, usersLocal));
                }
                for (TLRPC.TL_dialog d : dialogsRes.dialogs) {
                    if (d.last_message_date == 0) {
                        MessageObject mess = new_dialogMessage.get(d.top_message);
                        if (mess != null) {
                            //      d.last_message_date = mess.messageOwner.date;
                        }
                    }
                  /*  if (d.id == 0) {
                        if (d.peer instanceof TLRPC.TL_peerUser) {
                            d.id = d.peer.user_id;
                        } else if (d.peer instanceof TLRPC.TL_peerChat) {
                            d.id = -d.peer.chat_id;
                        }
                    }*/
                    new_dialogs_dict.put(d.id, d);
                }

                final int arg1 = new_totalDialogsCount;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        for (TLRPC.User u : dialogsRes.users) {
                            if (isCache) {
                                if (u.id == UserConfig.clientUserId || u.id / 1000 == 333) {
                                    users.put(u.jid, u);
                                } else {
                                    users.putIfAbsent(u.jid, u);
                                }
                            } else {
                                users.put(u.jid, u);
                                if (u.id == UserConfig.clientUserId) {
                                    UserConfig.currentUser = u;
                                }
                            }
                        }
                        for (TLRPC.Chat c : dialogsRes.chats) {
                            if (isCache) {
                                chats.putIfAbsent(c.id, c);
                            } else {
                                chats.put(c.id, c);
                            }
                        }
                        if (encChats != null) {
                            for (TLRPC.EncryptedChat encryptedChat : encChats) {
                                encryptedChats.put(encryptedChat.id, encryptedChat);
                            }
                        }
                        loadingDialogs = false;
                        totalDialogsCount = arg1;

                       /* for (HashMap.Entry<Long, TLRPC.TL_dialog> pair : new_dialogs_dict.entrySet()) {
                            long key = pair.getKey();
                            TLRPC.TL_dialog value = pair.getValue();
                            TLRPC.TL_dialog currentDialog = dialogs_dict.get(key);
                            if (currentDialog == null) {
                                dialogs_dict.put(key, value);
                                dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                            } else {
                                MessageObject oldMsg = dialogMessage.get(value.top_message);
                                if (oldMsg == null || currentDialog.top_message > 0) {
                                    if (oldMsg != null && oldMsg.deleted || value.top_message > currentDialog.top_message) {
                                        if (oldMsg != null) {
                                            dialogMessage.remove(oldMsg.messageOwner.id);
                                        }
                                        dialogs_dict.put(key, value);
                                        dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                                    }
                                } else {
                                    MessageObject newMsg = new_dialogMessage.get(value.top_message);
                                    if (oldMsg.deleted || newMsg == null || newMsg.messageOwner.date > oldMsg.messageOwner.date) {
                                        dialogMessage.remove(oldMsg.messageOwner.id);
                                        dialogs_dict.put(key, value);
                                        dialogMessage.put(value.top_message, new_dialogMessage.get(value.top_message));
                                    }
                                }
                            }*/
                    }

                        /*dialogs.clear();
                        dialogsServerOnly.clear();
                        dialogs.addAll(dialogs_dict.values());
                        Collections.sort(dialogs, new Comparator<TLRPC.TL_dialog>() {
                            @Override
                            public int compare(TLRPC.TL_dialog tl_dialog, TLRPC.TL_dialog tl_dialog2) {
                                if (tl_dialog.last_message_date == tl_dialog2.last_message_date) {
                                    return 0;
                                } else if (tl_dialog.last_message_date < tl_dialog2.last_message_date) {
                                    return 1;
                                } else {
                                    return -1;
                                }
                            }
                        });
                        for (TLRPC.TL_dialog d : dialogs) {
                            if ((int)d.id != 0) {
                                dialogsServerOnly.add(d);
                            }
                        }

                        dialogsEndReached = (dialogsRes.dialogs.size() == 0 || dialogsRes.dialogs.size() != count) && !isCache;
                        NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                    }*/
                });
            }
        });
    }

    public TLRPC.TL_photo generatePhotoSizes(String path, Uri imageUri) {
        //TODO generatePhotoSizes
      /*long time = System.currentTimeMillis();
        Bitmap bitmap = FileLoader.loadBitmap(path, imageUri, 800, 800);
          ArrayList<TLRPC.PhotoSize> sizes = new ArrayList<TLRPC.PhotoSize>();
        TLRPC.PhotoSize size = FileLoader.scaleAndSaveImage(bitmap, 90, 90, 55, true);
        if (size != null) {
            size.type = "s";
            sizes.add(size);
        }
        size = FileLoader.scaleAndSaveImage(bitmap, 320, 320, 87, false);
        if (size != null) {
            size.type = "m";
            sizes.add(size);
        }
        size = FileLoader.scaleAndSaveImage(bitmap, 800, 800, 87, false);
        if (size != null) {
            size.type = "x";
            sizes.add(size);
        }
        if (Build.VERSION.SDK_INT < 11) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        if (sizes.isEmpty()) {
            return null;
        } else {
            UserConfig.saveConfig(false);
            TLRPC.TL_photo photo = new TLRPC.TL_photo();
            photo.user_id = UserConfig.clientUserId;
            photo.date = ConnectionsManager.getInstance().getCurrentTime();
            photo.sizes = sizes;
            photo.caption = "";
            photo.geo = new TLRPC.TL_geoPointEmpty();
            return photo;
        }*/
        TLRPC.TL_photo photo = new TLRPC.TL_photo();
        return photo;
    }

    public void markDialogAsRead(final long dialog_id, final int max_id, final int max_positive_id, final int offset, final int max_date, final boolean was) {
        //TODO markDialogAsRead
       /*   int lower_part = (int)dialog_id;
        if (lower_part != 0) {
            if (max_id == 0 && offset == 0) {
                return;
            }
            TLRPC.TL_messages_readHistory req = new TLRPC.TL_messages_readHistory();
            if (lower_part < 0) {
                req.peer = new TLRPC.TL_inputPeerChat();
                req.peer.chat_id = -lower_part;
            } else {
                TLRPC.User user = users.get(lower_part);
                if (user instanceof TLRPC.TL_userForeign || user instanceof TLRPC.TL_userRequest) {
                    req.peer = new TLRPC.TL_inputPeerForeign();
                    req.peer.user_id = user.id;
                    req.peer.access_hash = user.access_hash;
                } else {
                    req.peer = new TLRPC.TL_inputPeerContact();
                    req.peer.user_id = user.id;
                }
            }
            req.max_id = max_positive_id;
            req.offset = offset;
            if (offset == 0) {
                MessagesStorage.getInstance().processPendingRead(dialog_id, max_positive_id, max_date, false);
            }
            if (req.max_id != Integer.MAX_VALUE) {
                ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        if (error == null) {
                            MessagesStorage.getInstance().processPendingRead(dialog_id, max_positive_id, max_date, true);
                            TLRPC.TL_messages_affectedHistory res = (TLRPC.TL_messages_affectedHistory) response;
                            if (res.offset > 0) {
                                markDialogAsRead(dialog_id, 0, max_positive_id, res.offset, max_date, was);
                            }

                            if (MessagesStorage.lastSeqValue + 1 == res.seq) {
                                MessagesStorage.lastSeqValue = res.seq;
                                MessagesStorage.lastPtsValue = res.pts;
                                MessagesStorage.getInstance().saveDiffParams(MessagesStorage.lastSeqValue, MessagesStorage.lastPtsValue, MessagesStorage.lastDateValue, MessagesStorage.lastQtsValue);
                            } else if (MessagesStorage.lastSeqValue != res.seq) {
                                FileLog.e("tmessages", "need get diff TL_messages_readHistory, seq: " + MessagesStorage.lastSeqValue + " " + res.seq);
                                if (gettingDifference || updatesStartWaitTime == 0 || updatesStartWaitTime != 0 && updatesStartWaitTime + 1500 > System.currentTimeMillis()) {
                                    if (updatesStartWaitTime == 0) {
                                        updatesStartWaitTime = System.currentTimeMillis();
                                    }
                                    FileLog.e("tmessages", "add TL_messages_readHistory to queue");
                                    UserActionUpdates updates = new UserActionUpdates();
                                    updates.seq = res.seq;
                                    updatesQueue.add(updates);
                                } else {
                                    getDifference();
                                }
                            }
                        }
                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric);
            }

            MessagesStorage.getInstance().storageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (offset == 0) {
                                TLRPC.TL_dialog dialog = dialogs_dict.get(dialog_id);
                                if (dialog != null) {
                                    dialog.unread_count = 0;
                                    NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                                }
                            }
                        }
                    });
                }
            });
            if (offset == 0) {
                TLRPC.TL_messages_receivedMessages req2 = new TLRPC.TL_messages_receivedMessages();
                req2.max_id = max_positive_id;
                ConnectionsManager.getInstance().performRpc(req2, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {

                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric | RPCRequest.RPCRequestClassFailOnServerErrors);
            }
        } else {
            if (max_date == 0) {
                return;
            }
            int encId = (int)(dialog_id >> 32);
            TLRPC.EncryptedChat chat = encryptedChats.get(encId);
            if (chat.auth_key != null && chat.auth_key.length > 1 && chat instanceof TLRPC.TL_encryptedChat) {
                TLRPC.TL_messages_readEncryptedHistory req = new TLRPC.TL_messages_readEncryptedHistory();
                req.peer = new TLRPC.TL_inputEncryptedChat();
                req.peer.chat_id = chat.id;
                req.peer.access_hash = chat.access_hash;
                req.max_date = max_date;

                ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {
                        //MessagesStorage.getInstance().processPendingRead(dialog_id, max_id, max_date, true);
                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric);
            }
            MessagesStorage.getInstance().processPendingRead(dialog_id, max_id, max_date, false);

            MessagesStorage.getInstance().storageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            TLRPC.TL_dialog dialog = dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                dialog.unread_count = 0;
                                NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                            }
                        }
                    });
                }
            });

            if (chat.ttl > 0 && was) {
                int serverTime = Math.max(ConnectionsManager.getInstance().getCurrentTime(), max_date);
                MessagesStorage.getInstance().createTaskForDate(chat.id, serverTime, serverTime, 0);
            }
        }*/
    }

    public void cancelSendingMessage(MessageObject object) {
        //TODO cancelSendingMessage
        /*String keyToRemvoe = null;
        boolean enc = false;
        for (HashMap.Entry<String, ArrayList<DelayedMessage>> entry : delayedMessages.entrySet()) {
            ArrayList<DelayedMessage> messages = entry.getValue();
            for (int a = 0; a < messages.size(); a++) {
                DelayedMessage message = messages.get(a);
                if (message.obj.messageOwner.id == object.messageOwner.id) {
                    messages.remove(a);
                    if (messages.size() == 0) {
                        keyToRemvoe = entry.getKey();
                        if (message.sendEncryptedRequest != null) {
                            enc = true;
                        }
                    }
                    break;
                }
            }
        }
        if (keyToRemvoe != null) {
            FileLoader.getInstance().cancelUploadFile(keyToRemvoe, enc);
        }
        ArrayList<Integer> messages = new ArrayList<Integer>();
        messages.add(object.messageOwner.id);
        deleteMessages(messages, null, null);*/
    }

    private long getNextRandomId() {
        long val = 0;
        while (val == 0) {
            val = random.nextLong();
        }
        return val;
    }

    public void sendMessage(TLRPC.User user, long peer) {
        sendMessage(null, 0, 0, null, null, null, null, user, null, null, peer);
    }

    public void sendMessage(MessageObject message, long peer) {
        sendMessage(null, 0, 0, null, null, message, null, null, null, null, peer);
    }

    public void sendMessage(TLRPC.TL_document document, long peer) {
        sendMessage(null, 0, 0, null, null, null, null, null, document, null, peer);
    }

    public void sendMessage(String message, long peer) {
        sendMessage(message, 0, 0, null, null, null, null, null, null, null, peer);
    }

    public void sendMessage(TLRPC.FileLocation location, long peer) {
        sendMessage(null, 0, 0, null, null, null, location, null, null, null, peer);
    }

    public void sendMessage(double lat, double lon, long peer) {
        sendMessage(null, lat, lon, null, null, null, null, null, null, null, peer);
    }

    public void sendMessage(TLRPC.TL_photo photo, long peer) {
        sendMessage(null, 0, 0, photo, null, null, null, null, null, null, peer);
    }

    public void sendMessage(TLRPC.TL_video video, long peer) {
        sendMessage(null, 0, 0, null, video, null, null, null, null, null, peer);
    }

    public void sendMessage(TLRPC.TL_audio audio, long peer) {
        sendMessage(null, 0, 0, null, null, null, null, null, null, audio, peer);
    }

    private void processPendingEncMessages() {
        if (pendingEncMessagesToDelete.isEmpty()) {
            return;
        }
        ArrayList<Long> arr = new ArrayList<Long>(pendingEncMessagesToDelete);
        //  MessagesStorage.getInstance().markMessagesAsDeletedByRandoms(arr);
        pendingEncMessagesToDelete.clear();
    }

    private void sendMessage(String message, double lat, double lon, TLRPC.TL_photo photo, TLRPC.TL_video video, MessageObject msgObj, TLRPC.FileLocation location, TLRPC.User user, TLRPC.TL_document document, TLRPC.TL_audio audio, long peer) {

    }

    private void processSentMessage(TLRPC.Message newMsg, TLRPC.Message sentMessage) {
        if (sentMessage != null) {
        }
    }

    private void performSendMessageRequest(TLObject req, final MessageObject newMsgObj) {
        //TODO Send Message Logic
    }

    private void putToDelayedMessages(String location, DelayedMessage message) {
        ArrayList<DelayedMessage> arrayList = delayedMessages.get(location);
        if (arrayList == null) {
            arrayList = new ArrayList<DelayedMessage>();
            delayedMessages.put(location, arrayList);
        }
        arrayList.add(message);
    }

    private void performSendDelayedMessage(final DelayedMessage message) {
        //TODO send file logic
      /* if (message.type == 0) {
            String location = Utilities.getCacheDir() + "/" + message.location.volume_id + "_" + message.location.local_id + ".jpg";
            putToDelayedMessages(location, message);
            if (message.sendRequest != null) {
                FileLoader.getInstance().uploadFile(location, null, null);
            } else {
                FileLoader.getInstance().uploadFile(location, message.sendEncryptedRequest.media.key, message.sendEncryptedRequest.media.iv);
            }
        } else if (message.type == 1) {
            if (message.sendRequest != null) {
                if (message.sendRequest.media.thumb == null) {
                    String location = Utilities.getCacheDir() + "/" + message.location.volume_id + "_" + message.location.local_id + ".jpg";
                    putToDelayedMessages(location, message);
                    FileLoader.getInstance().uploadFile(location, null, null);
                } else {
                    String location = message.videoLocation.path;
                    if (location == null) {
                        location = Utilities.getCacheDir() + "/" + message.videoLocation.id + ".mp4";
                    }
                    putToDelayedMessages(location, message);
                    FileLoader.getInstance().uploadFile(location, null, null);
                }
            } else {
                String location = message.videoLocation.path;
                if (location == null) {
                    location = Utilities.getCacheDir() + "/" + message.videoLocation.id + ".mp4";
                }
                putToDelayedMessages(location, message);
                FileLoader.getInstance().uploadFile(location, message.sendEncryptedRequest.media.key, message.sendEncryptedRequest.media.iv);
            }
        } else if (message.type == 2) {
            String location = message.documentLocation.path;
            putToDelayedMessages(location, message);
            if (message.sendRequest != null) {
                FileLoader.getInstance().uploadFile(location, null, null);
            } else {
                FileLoader.getInstance().uploadFile(location, message.sendEncryptedRequest.media.key, message.sendEncryptedRequest.media.iv);
            }
        } else if (message.type == 3) {
            String location = message.audioLocation.path;
            putToDelayedMessages(location, message);
            if (message.sendRequest != null) {
                FileLoader.getInstance().uploadFile(location, null, null);
            } else {
                FileLoader.getInstance().uploadFile(location, message.sendEncryptedRequest.media.key, message.sendEncryptedRequest.media.iv);
            }
        }*/
    }

    public void fileDidFailedUpload(final String location, final boolean enc) {
        if (uploadingAvatar != null && uploadingAvatar.equals(location)) {
            uploadingAvatar = null;
        } else {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<DelayedMessage> arr = delayedMessages.get(location);
                    if (arr != null) {
                        for (int a = 0; a < arr.size(); a++) {
                            DelayedMessage obj = arr.get(a);
                            if (enc && obj.sendEncryptedRequest != null || !enc && obj.sendRequest != null) {
                               /* obj.obj.messageOwner.send_state = MESSAGE_SEND_STATE_SEND_ERROR;
                                sendingMessages.remove(obj.obj.messageOwner.id);*/
                                arr.remove(a);
                                a--;
                             /*   NotificationCenter.getInstance().postNotificationName(messageSendError, obj.obj.messageOwner.id);*/
                            }
                        }
                        if (arr.isEmpty()) {
                            delayedMessages.remove(location);
                        }
                    }
                }
            });
        }
    }

    public void fileDidUploaded(final String location, final TLRPC.InputFile file, final TLRPC.InputEncryptedFile encryptedFile) {
        //TODO fileDidUploaded

    }

    public long createChat(String title, ArrayList<String> selectedContacts, final TLRPC.InputFile uploadedAvatar) {
        //TODO createChat

        return 1;
    }

    public void addUserToChat(int chat_id, final TLRPC.User user, final TLRPC.ChatParticipants info) {
        if (user == null) {
            return;
        }
//TODO addUserToChat
    }

    public void deleteUserFromChat(int chat_id, final TLRPC.User user, final TLRPC.ChatParticipants info) {
        if (user == null) {
            return;
        }
        //TODO deleteUserFromChat
    }

    public void changeChatTitle(int chat_id, String title) {
        //TODO changeChatTitle

    }

    public void changeChatAvatar(int chat_id, TLRPC.InputFile uploadedAvatar) {
        //TODO changeChatAvatar
    }

    public void unregistedPush() {
        //TODO unregistedPush
    }

    public void registerForPush(final String regid) {
        if (regid == null || regid.length() == 0 || registeringForPush || UserConfig.clientUserId == 0) {
            return;
        }
        if (UserConfig.registeredForPush && regid.equals(UserConfig.pushString)) {
            return;
        }
        registeringForPush = true;
        TLRPC.TL_account_registerDevice req = new TLRPC.TL_account_registerDevice();
        req.token_type = 2;
        req.token = regid;
        req.app_sandbox = false;
        try {
            req.lang_code = Locale.getDefault().getCountry();
            req.device_model = Build.MANUFACTURER + Build.MODEL;
            if (req.device_model == null) {
                req.device_model = "Android unknown";
            }
            req.system_version = "SDK " + Build.VERSION.SDK_INT;
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            req.app_version = pInfo.versionName;
            if (req.app_version == null) {
                req.app_version = "App version unknown";
            }

        } catch (Exception e) {
            FileLog.e("tmessages", e);
            req.lang_code = "en";
            req.device_model = "Android unknown";
            req.system_version = "SDK " + Build.VERSION.SDK_INT;
            req.app_version = "App version unknown";
        }

        if (req.lang_code == null || req.lang_code.length() == 0) {
            req.lang_code = "en";
        }
        if (req.device_model == null || req.device_model.length() == 0) {
            req.device_model = "Android unknown";
        }
        if (req.app_version == null || req.app_version.length() == 0) {
            req.app_version = "App version unknown";
        }
        if (req.system_version == null || req.system_version.length() == 0) {
            req.system_version = "SDK Unknown";
        }

        if (req.app_version != null) {
            FileLog.e("tmessages", "registered for push");
            UserConfig.registeredForPush = true;
            UserConfig.pushString = regid;
            UserConfig.saveConfig(false);

            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    registeringForPush = false;
                }
            });
        }
    }

    public void loadCurrentState() {
        if (updatingState) {
            return;
        }
        updatingState = true;
        //TODO loadCurrentState

    }

    private int getUpdateSeq(TLRPC.Updates updates) {
        if (updates instanceof TLRPC.TL_updatesCombined) {
            return updates.seq_start;
        } else {
            return updates.seq;
        }
    }

    private void processUpdatesQueue(boolean getDifference) {
        if (!updatesQueue.isEmpty()) {
            Collections.sort(updatesQueue, new Comparator<TLRPC.Updates>() {
                @Override
                public int compare(TLRPC.Updates updates, TLRPC.Updates updates2) {
                    int seq1 = getUpdateSeq(updates);
                    int seq2 = getUpdateSeq(updates2);
                    if (seq1 == seq2) {
                        return 0;
                    } else if (seq1 > seq2) {
                        return 1;
                    }
                    return -1;
                }
            });
            boolean anyProceed = false;
            for (int a = 0; a < updatesQueue.size(); a++) {
                TLRPC.Updates updates = updatesQueue.get(a);
                int seq = getUpdateSeq(updates);
                // if (MessagesStorage.lastSeqValue + 1 == seq || MessagesStorage.lastSeqValue == seq) {
                processUpdates(updates, true);
                anyProceed = true;
                updatesQueue.remove(a);
                a--;
                //  } else if (MessagesStorage.lastSeqValue < seq) {
                if (updatesStartWaitTime != 0 && (anyProceed || updatesStartWaitTime + 1500 > System.currentTimeMillis())) {
                    FileLog.e("tmessages", "HOLE IN UPDATES QUEUE - will wait more time");
                    if (anyProceed) {
                        updatesStartWaitTime = System.currentTimeMillis();
                    }
                    return;
                } else {
                    FileLog.e("tmessages", "HOLE IN UPDATES QUEUE - getDifference");
                    updatesStartWaitTime = 0;
                    updatesQueue.clear();
                    getDifference();
                    return;
                }
                //  } else {
                //  updatesQueue.remove(a);
                //   a--;
                //  }
            }
            updatesQueue.clear();
            FileLog.e("tmessages", "UPDATES QUEUE PROCEED - OK");
            updatesStartWaitTime = 0;
            if (getDifference) {
                final int stateCopy = 3; /*TODO getconnection state*/ // ConnectionsManager.getInstance().connectionState;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(703, stateCopy);
                    }
                });
            }
        } else {
            if (getDifference) {
                final int stateCopy = 3; /*TODO getconnection state*/ // ConnectionsManager.getInstance().connectionState;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(703, stateCopy);
                    }
                });
            } else {
                updatesStartWaitTime = 0;
            }
        }
    }

    public void getDifference() {
        //TODO getDifference save to database logic
    }

    public void processUpdates(final TLRPC.Updates updates, boolean fromQueue) {

    }

    public boolean processUpdateArray(ArrayList<TLRPC.Update> updates, final ArrayList<TLRPC.User> usersArr, final ArrayList<TLRPC.Chat> chatsArr) {
    /*    if (updates.isEmpty()) {
            return true;
        }
        long currentTime = System.currentTimeMillis();

        final HashMap<Long, ArrayList<MessageObject>> messages = new HashMap<Long, ArrayList<MessageObject>>();
        final ArrayList<TLRPC.Message> messagesArr = new ArrayList<TLRPC.Message>();
        final ArrayList<Integer> markAsReadMessages = new ArrayList<Integer>();
        final HashMap<Integer, Integer> markAsReadEncrypted = new HashMap<Integer, Integer>();
        final ArrayList<Integer> deletedMessages = new ArrayList<Integer>();
        final ArrayList<Long> printChanges = new ArrayList<Long>();
        final ArrayList<TLRPC.ChatParticipants> chatInfoToUpdate = new ArrayList<TLRPC.ChatParticipants>();
        final ArrayList<TLRPC.Update> updatesOnMainThread = new ArrayList<TLRPC.Update>();
        final ArrayList<TLRPC.TL_updateEncryptedMessagesRead> tasks = new ArrayList<TLRPC.TL_updateEncryptedMessagesRead>();
        final ArrayList<String> contactsIds = new ArrayList<String>();
        MessageObject lastMessage = null;

        boolean checkForUsers = true;
        ConcurrentHashMap<String, TLRPC.User> usersDict;
        ConcurrentHashMap<Integer, TLRPC.Chat> dialogs_dict;
        if (usersArr != null) {
            usersDict = new ConcurrentHashMap<String, TLRPC.User>();
            for (TLRPC.User user : usersArr) {
                usersDict.put(user.jid, user);
            }
        } else {
            checkForUsers = false;
            usersDict = users;
        }
        if (chatsArr != null) {
            dialogs_dict = new ConcurrentHashMap<Integer, TLRPC.Chat>();
            for (TLRPC.Chat chat : chatsArr) {
                dialogs_dict.put(chat.id, chat);
            }
        } else {
            checkForUsers = false;
            dialogs_dict = chats;
        }

        if (usersArr != null || chatsArr != null) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (usersArr != null) {
                        for (TLRPC.User user : usersArr) {
                            users.put(user.jid, user);
                            if (user.id == UserConfig.clientUserId) {
                                UserConfig.currentUser = user;
                            }
                        }
                    }
                    if (chatsArr != null) {
                        for (TLRPC.Chat chat : chatsArr) {
                            chats.put(chat.id, chat);
                        }
                    }
                }
            });
        }

        int interfaceUpdateMask = 0;

        for (TLRPC.Update update : updates) {
            if (update instanceof TLRPC.TL_updateNewMessage) {
                TLRPC.TL_updateNewMessage upd = (TLRPC.TL_updateNewMessage)update;
                if (checkForUsers) {
                    if (usersDict.get(upd.message.from_id) == null && users.get(upd.message.from_id) == null || upd.message.to_id.chat_id != 0 && dialogs_dict.get(upd.message.to_id.chat_id) == null && chats.get(upd.message.to_id.chat_id) == null) {
                        return false;
                    }
                }
                messagesArr.add(upd.message);
                MessageObject obj = new MessageObject(upd.message, usersDict);
                if (obj.type == 11) {
                    interfaceUpdateMask |= UPDATE_MASK_CHAT_AVATAR;
                } else if (obj.type == 10) {
                    interfaceUpdateMask |= UPDATE_MASK_CHAT_NAME;
                }
                long uid;
                if (upd.message.to_id.chat_id != 0) {
                    uid = -upd.message.to_id.chat_id;
                } else {
                    if (upd.message.to_id.user_id == UserConfig.clientUserId) {
                        upd.message.to_id.user_id = upd.message.from_id;
                    }
                    uid = upd.message.to_id.user_id;
                }
                ArrayList<MessageObject> arr = messages.get(uid);
                if (arr == null) {
                    arr = new ArrayList<MessageObject>();
                    messages.put(uid, arr);
                }
                arr.add(obj);
                MessagesStorage.lastPtsValue = update.pts;
                if (upd.message.from_id != UserConfig.clientUserId && upd.message.to_id != null) {
                    if (uid != openned_dialog_id || ApplicationLoader.lastPauseTime != 0) {
                        lastMessage = obj;
                    }
                }
            } else if (update instanceof TLRPC.TL_updateMessageID) {
                //can't be here
            } else if (update instanceof TLRPC.TL_updateReadMessages) {
                markAsReadMessages.addAll(update.messages);
                MessagesStorage.lastPtsValue = update.pts;
            } else if (update instanceof TLRPC.TL_updateDeleteMessages) {
                deletedMessages.addAll(update.messages);
                MessagesStorage.lastPtsValue = update.pts;
            } else if (update instanceof TLRPC.TL_updateRestoreMessages) {
                MessagesStorage.lastPtsValue = update.pts;
            } else if (update instanceof TLRPC.TL_updateUserTyping || update instanceof TLRPC.TL_updateChatUserTyping) {
                if (update.user_id != UserConfig.clientUserId) {
                    long uid = -update.chat_id;
                    if (uid == 0) {
                        uid = update.user_id;
                    }
                    ArrayList<PrintingUser> arr = printingUsers.get(uid);
                    if (arr == null) {
                        arr = new ArrayList<PrintingUser>();
                        printingUsers.put(uid, arr);
                    }
                    boolean exist = false;
                    for (PrintingUser u : arr) {
                        if (u.userId == update.user_id) {
                            exist = true;
                            u.lastTime = currentTime;
                            break;
                        }
                    }
                    if (!exist) {
                        PrintingUser newUser = new PrintingUser();
                        newUser.userId = update.user_id;
                        newUser.lastTime = currentTime;
                        arr.add(newUser);
                        if (!printChanges.contains(uid)) {
                            printChanges.add(uid);
                        }
                    }
                }
            } else if (update instanceof TLRPC.TL_updateChatParticipants) {
                interfaceUpdateMask |= UPDATE_MASK_CHAT_MEMBERS;
                chatInfoToUpdate.add(update.participants);
            } else if (update instanceof TLRPC.TL_updateUserStatus) {
                interfaceUpdateMask |= UPDATE_MASK_STATUS;
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateUserName) {
                interfaceUpdateMask |= UPDATE_MASK_NAME;
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateUserPhoto) {
                interfaceUpdateMask |= UPDATE_MASK_AVATAR;
                MessagesStorage.getInstance().clearUserPhotos(update.user_id);
               /*if (!(update.photo instanceof TLRPC.TL_userProfilePhotoEmpty)) { DEPRECATED
                    if (usersDict.containsKey(update.user_id)) {
                        TLRPC.TL_messageService newMessage = new TLRPC.TL_messageService();
                        newMessage.action = new TLRPC.TL_messageActionUserUpdatedPhoto();
                        newMessage.action.newUserPhoto = update.photo;
                        newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                        UserConfig.saveConfig(false);
                        newMessage.unread = true;
                        newMessage.date = update.date;
                        newMessage.from_id = update.user_id;
                        newMessage.to_id = new TLRPC.TL_peerUser();
                        newMessage.to_id.user_id = UserConfig.clientUserId;
                        newMessage.out = false;
                        newMessage.dialog_id = update.user_id;

                        messagesArr.add(newMessage);
                        MessageObject obj = new MessageObject(newMessage, usersDict);
                        ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                        if (arr == null) {
                            arr = new ArrayList<MessageObject>();
                            messages.put(newMessage.dialog_id, arr);
                        }
                        arr.add(obj);
                        if (newMessage.from_id != UserConfig.clientUserId && newMessage.to_id != null) {
                            if (newMessage.dialog_id != openned_dialog_id || ApplicationLoader.lastPauseTime != 0) {
                                lastMessage = obj;
                            }
                        }
                    }
                }
    * / *
                updatesOnMainThread.add(update);
            } else if (update instanceof TLRPC.TL_updateContactRegistered) {
                if (enableJoined && usersDict.containsKey(update.user_id)) {
                    TLRPC.TL_messageService newMessage = new TLRPC.TL_messageService();
                    newMessage.action = new TLRPC.TL_messageActionUserJoined();
                    newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                    UserConfig.saveConfig(false);
                    newMessage.unread = true;
                    newMessage.date = update.date;
                    newMessage.from_id = update.user_id;
                    newMessage.to_id = new TLRPC.TL_peerUser();
                    newMessage.to_id.user_id = UserConfig.clientUserId;
                    newMessage.out = false;
                    newMessage.dialog_id = update.user_id;

                    messagesArr.add(newMessage);
                    MessageObject obj = new MessageObject(newMessage, usersDict);
                    ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                    if (arr == null) {
                        arr = new ArrayList<MessageObject>();
                        messages.put(newMessage.dialog_id, arr);
                    }
                    arr.add(obj);
                    if (newMessage.from_id != UserConfig.clientUserId && newMessage.to_id != null) {
                        if (newMessage.dialog_id != openned_dialog_id || ApplicationLoader.lastPauseTime != 0) {
                            lastMessage = obj;
                        }
                    }
                }
//                if (!contactsIds.contains(update.user_id)) {
//                    contactsIds.add(update.user_id);
//                }
            } else if (update instanceof TLRPC.TL_updateContactLink) {
                if (update.my_link instanceof TLRPC.TL_contacts_myLinkContact || update.my_link instanceof TLRPC.TL_contacts_myLinkRequested && update.my_link.contact) {
                    int idx = contactsIds.indexOf(-update.user_id);
                    if (idx != -1) {
                        contactsIds.remove(idx);
                    }
                    if (!contactsIds.contains(update.user_id)) {
                        contactsIds.add(update.user_id);
                    }
                } else {
                    int idx = contactsIds.indexOf(update.user_id);
                    if (idx != -1) {
                        contactsIds.remove(idx);
                    }
                    if (!contactsIds.contains(update.user_id)) {
                        contactsIds.add(-update.user_id);
                    }
                }
            } else if (update instanceof TLRPC.TL_updateActivation) {
                //DEPRECATED
            } else if (update instanceof TLRPC.TL_updateNewAuthorization) {
                TLRPC.TL_messageService newMessage = new TLRPC.TL_messageService();
                newMessage.action = new TLRPC.TL_messageActionLoginUnknownLocation();
                newMessage.action.title = update.device;
                newMessage.action.address = update.location;
                newMessage.local_id = newMessage.id = UserConfig.getNewMessageId();
                UserConfig.saveConfig(false);
                newMessage.unread = true;
                newMessage.date = update.date;
                newMessage.from_id = 333000;
                newMessage.to_id = new TLRPC.TL_peerUser();
                newMessage.to_id.user_id = UserConfig.clientUserId;
                newMessage.out = false;
                newMessage.dialog_id = 333000;

                messagesArr.add(newMessage);
                MessageObject obj = new MessageObject(newMessage, usersDict);
                ArrayList<MessageObject> arr = messages.get(newMessage.dialog_id);
                if (arr == null) {
                    arr = new ArrayList<MessageObject>();
                    messages.put(newMessage.dialog_id, arr);
                }
                arr.add(obj);
                if (newMessage.from_id != UserConfig.clientUserId && newMessage.to_id != null) {
                    if (newMessage.dialog_id != openned_dialog_id || ApplicationLoader.lastPauseTime != 0) {
                        lastMessage = obj;
                    }
                }
            } else if (update instanceof TLRPC.TL_updateNewGeoChatMessage) {
                //DEPRECATED
            } else if (update instanceof TLRPC.TL_updateNewEncryptedMessage) {
                MessagesStorage.lastQtsValue = update.qts;
                TLRPC.Message message = decryptMessage(((TLRPC.TL_updateNewEncryptedMessage)update).message);
                if (message != null) {
                    int cid = ((TLRPC.TL_updateNewEncryptedMessage)update).message.chat_id;
                    messagesArr.add(message);
                    MessageObject obj = new MessageObject(message, usersDict);
                    long uid = ((long)cid) << 32;
                    ArrayList<MessageObject> arr = messages.get(uid);
                    if (arr == null) {
                        arr = new ArrayList<MessageObject>();
                        messages.put(uid, arr);
                    }
                    arr.add(obj);
                    if (message.from_id != UserConfig.clientUserId && message.to_id != null) {
                        if (uid != openned_dialog_id || ApplicationLoader.lastPauseTime != 0) {
                            lastMessage = obj;
                        }
                    }
                }
            } else if (update instanceof TLRPC.TL_updateEncryptedChatTyping) {
                long uid = ((long)update.chat_id) << 32;
                ArrayList<PrintingUser> arr = printingUsers.get(uid);
                if (arr == null) {
                    arr = new ArrayList<PrintingUser>();
                    printingUsers.put(uid, arr);
                }
                boolean exist = false;
                for (PrintingUser u : arr) {
                    if (u.userId == update.user_id) {
                        exist = true;
                        u.lastTime = currentTime;
                        break;
                    }
                }
                if (!exist) {
                    PrintingUser newUser = new PrintingUser();
                    newUser.userId = update.user_id;
                    newUser.lastTime = currentTime;
                    arr.add(newUser);
                    if (!printChanges.contains(uid)) {
                        printChanges.add(uid);
                    }
                }
            } else if (update instanceof TLRPC.TL_updateEncryptedMessagesRead) {
                markAsReadEncrypted.put(update.chat_id, Math.max(update.max_date, update.date));
                tasks.add((TLRPC.TL_updateEncryptedMessagesRead)update);
            } else if (update instanceof TLRPC.TL_updateChatParticipantAdd) {
                MessagesStorage.getInstance().updateChatInfo(update.chat_id, update.user_id, false, update.inviter_id, update.version);
            } else if (update instanceof TLRPC.TL_updateChatParticipantDelete) {
                MessagesStorage.getInstance().updateChatInfo(update.chat_id, update.user_id, true, 0, update.version);
            } else if (update instanceof TLRPC.TL_updateDcOptions) {
                //ConnectionsManager.getInstance().updateDcSettings(0);
            } else if (update instanceof TLRPC.TL_updateEncryption) {
                final TLRPC.EncryptedChat newChat = update.chat;
                long dialog_id = ((long)newChat.id) << 32;
                TLRPC.EncryptedChat existingChat = encryptedChats.get(newChat.id);
                if (existingChat == null) {
                    Semaphore semaphore = new Semaphore(0);
                    ArrayList<TLObject> result = new ArrayList<TLObject>();
                    MessagesStorage.getInstance().getEncryptedChat(newChat.id, semaphore, result);
                    try {
                        semaphore.acquire();
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    if (result.size() == 2) {
                        existingChat = (TLRPC.EncryptedChat)result.get(0);
                        TLRPC.User user = (TLRPC.User)result.get(1);
                        users.putIfAbsent(user.jid, user);
                    }
                }


            }
        }
        if (!messages.isEmpty()) {
            for (HashMap.Entry<Long, ArrayList<MessageObject>> pair : messages.entrySet()) {
                Long key = pair.getKey();
                ArrayList<MessageObject> value = pair.getValue();
                boolean printChanged = updatePrintingUsersWithNewMessages(key, value);
                if (printChanged && !printChanges.contains(key)) {
                    printChanges.add(key);
                }
            }
        }

        if (!printChanges.isEmpty()) {
            updatePrintingStrings();
        }

        final MessageObject lastMessageArg = lastMessage;
        final int interfaceUpdateMaskFinal = interfaceUpdateMask;

        processPendingEncMessages();

        if (!contactsIds.isEmpty()) {
            ContactsController.getInstance().processContactsUpdates(contactsIds, usersDict);
        }

        if (!messagesArr.isEmpty()) {
            MessagesStorage.getInstance().putMessages(messagesArr, true, true);
        }

        if (!messages.isEmpty() || !markAsReadMessages.isEmpty() || !deletedMessages.isEmpty() || !printChanges.isEmpty() || !chatInfoToUpdate.isEmpty() || !updatesOnMainThread.isEmpty() || !markAsReadEncrypted.isEmpty() || !contactsIds.isEmpty()) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    int updateMask = interfaceUpdateMaskFinal;

                    boolean avatarsUpdate = false;
                    if (!updatesOnMainThread.isEmpty()) {
                        ArrayList<TLRPC.User> dbUsers = new ArrayList<TLRPC.User>();
                        ArrayList<TLRPC.User> dbUsersStatus = new ArrayList<TLRPC.User>();
                        for (TLRPC.Update update : updatesOnMainThread) {
                            TLRPC.User toDbUser = new TLRPC.User();
                            toDbUser.id = update.user_id;
                            TLRPC.User currentUser = users.get(update.user_id);
                            if (update instanceof TLRPC.TL_updateUserStatus) {
                                if (currentUser != null) {
                                    currentUser.id = update.user_id;
                                    currentUser.status = update.status;
                                }
                                toDbUser.status = update.status;
                                dbUsersStatus.add(toDbUser);
                            } else if (update instanceof TLRPC.TL_updateUserName) {
                                if (currentUser != null) {
                                    currentUser.first_name = update.first_name;
                                    currentUser.last_name = update.last_name;
                                }
                                toDbUser.first_name = update.first_name;
                                toDbUser.last_name = update.last_name;
                                dbUsers.add(toDbUser);
                            } else if (update instanceof TLRPC.TL_updateUserPhoto) {
                                if (currentUser != null) {
                                    currentUser.photo = update.photo;
                                }
                                avatarsUpdate = true;
                                toDbUser.photo = update.photo;
                                dbUsers.add(toDbUser);
                            }
                        }
                        MessagesStorage.getInstance().updateUsers(dbUsersStatus, true, true, true);
                        MessagesStorage.getInstance().updateUsers(dbUsers, false, true, true);
                    }

                    if (!messages.isEmpty()) {
                        for (HashMap.Entry<Long, ArrayList<MessageObject>> entry : messages.entrySet()) {
                            Long key = entry.getKey();
                            ArrayList<MessageObject> value = entry.getValue();
                            updateInterfaceWithMessages(key, value);
                        }
                        NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
                    }
                    if (!markAsReadMessages.isEmpty()) {
                        for (Integer id : markAsReadMessages) {
                            MessageObject obj = dialogMessage.get(id);
                            if (obj != null) {
                                obj.messageOwner.unread = false;
                                updateMask |= UPDATE_MASK_READ_DIALOG_MESSAGE;
                            }
                        }

                        if (currentPushMessage != null && markAsReadMessages.contains(currentPushMessage.messageOwner.id)) {
                            NotificationManager mNotificationManager = (NotificationManager)ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.cancel(1);
                            currentPushMessage = null;
                        }
                    }
                    if (!markAsReadEncrypted.isEmpty()) {
                        for (HashMap.Entry<Integer, Integer> entry : markAsReadEncrypted.entrySet()) {
                            NotificationCenter.getInstance().postNotificationName(messagesReadedEncrypted, entry.getKey(), entry.getValue());
                            long dialog_id = (long)(entry.getKey()) << 32;
                            TLRPC.TL_dialog dialog = dialogs_dict.get(dialog_id);
                            if (dialog != null) {
                                MessageObject message = dialogMessage.get(dialog.top_message);
                                if (message != null && message.messageOwner.date <= entry.getValue()) {
                                    message.messageOwner.unread = false;
                                    updateMask |= UPDATE_MASK_READ_DIALOG_MESSAGE;
                                }
                            }
                        }
                    }
                    if (!deletedMessages.isEmpty()) {
                        NotificationCenter.getInstance().postNotificationName(messagesDeleted, deletedMessages);
                        for (Integer id : deletedMessages) {
                            MessageObject obj = dialogMessage.get(id);
                            if (obj != null) {
                                obj.deleted = true;
                            }
                        }
                    }
                    if (!printChanges.isEmpty()) {
                        updateMask |= UPDATE_MASK_USER_PRINT;
                    }
                    if (!contactsIds.isEmpty()) {
                        updateMask |= UPDATE_MASK_NAME;
                        updateMask |= UPDATE_MASK_USER_PHONE;
                    }
                    if (!chatInfoToUpdate.isEmpty()) {
                        for (TLRPC.ChatParticipants info : chatInfoToUpdate) {
                            MessagesStorage.getInstance().updateChatInfo(info.chat_id, info, true);
                            NotificationCenter.getInstance().postNotificationName(chatInfoDidLoaded, info.chat_id, info);
                        }
                    }
                    if (updateMask != 0) {
                        NotificationCenter.getInstance().postNotificationName(updateInterfaces, updateMask);
                    }
                    if (lastMessageArg != null) {
                        showInAppNotification(lastMessageArg);
                    }
                }
            });
        }

        if (!markAsReadMessages.isEmpty() || !markAsReadEncrypted.isEmpty()) {
            MessagesStorage.getInstance().storageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!markAsReadMessages.isEmpty()) {
                                NotificationCenter.getInstance().postNotificationName(messagesReaded, markAsReadMessages);
                            }
                        }
                    });
                }
            });
        }

        if (!markAsReadMessages.isEmpty() || !markAsReadEncrypted.isEmpty()) {
            if (!markAsReadMessages.isEmpty()) {
                MessagesStorage.getInstance().updateDialogsWithReadedMessages(markAsReadMessages, true);
            }
            MessagesStorage.getInstance().markMessagesAsRead(markAsReadMessages, markAsReadEncrypted, true);
        }
        if (!deletedMessages.isEmpty()) {
            MessagesStorage.getInstance().markMessagesAsDeleted(deletedMessages, true);
        }
        if (!deletedMessages.isEmpty()) {
            MessagesStorage.getInstance().updateDialogsWithDeletedMessages(deletedMessages, true);
        }
        if (!tasks.isEmpty()) {
            for (TLRPC.TL_updateEncryptedMessagesRead update : tasks) {
                MessagesStorage.getInstance().createTaskForDate(update.chat_id, update.max_date, update.date, 1);
            }
        }
*/
        return true;
    }

    private boolean updatePrintingUsersWithNewMessages(long uid, ArrayList<MessageObject> messages) {
        if (uid > 0) {
            ArrayList<PrintingUser> arr = printingUsers.get(uid);
            if (arr != null) {
                printingUsers.remove(uid);
                return true;
            }
        } else if (uid < 0) {
            ArrayList<Integer> messagesUsers = new ArrayList<Integer>();
            for (MessageObject message : messages) {
              /*  if (!messagesUsers.contains(message.messageOwner.from_id)) {
                    messagesUsers.add(message.messageOwner.from_id);
                }*/
            }

            ArrayList<PrintingUser> arr = printingUsers.get(uid);
            boolean changed = false;
            if (arr != null) {
                for (int a = 0; a < arr.size(); a++) {
                    PrintingUser user = arr.get(a);
                    if (messagesUsers.contains(user.userId)) {
                        arr.remove(a);
                        a--;
                        if (arr.isEmpty()) {
                            printingUsers.remove(uid);
                        }
                        changed = true;
                    }
                }
            }
            if (changed) {
                return true;
            }
        }
        return false;
    }

    private void playNotificationSound() {
        if (lastSoundPlay > System.currentTimeMillis() - 1800) {
            return;
        }
        try {
            lastSoundPlay = System.currentTimeMillis();
            soundPool.play(sound, 1, 1, 1, 0, 1);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    private void showInAppNotification(MessageObject messageObject) {
        if (!UserConfig.clientActivated) {
            return;
        }
        /*if (ApplicationLoader.lastPauseTime != 0) {
            ApplicationLoader.lastPauseTime = System.currentTimeMillis();
            FileLog.e("tmessages", "reset sleep timeout by recieved message");
        }*/
        if (messageObject == null) {
            return;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        boolean globalEnabled = preferences.getBoolean("EnableAll", true);
        if (!globalEnabled) {
            return;
        }


    }

    public void sendAlertToPebble(String message) {
        try {
            final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

            final HashMap<String, String> data = new HashMap<String, String>();
            data.put("title", LocaleController.getString("AppName", R.string.AppName));
            data.put("body", message);
            final JSONObject jsonData = new JSONObject(data);
            final String notificationData = new JSONArray().put(jsonData).toString();

            i.putExtra("messageType", "PEBBLE_ALERT");
            i.putExtra("sender", LocaleController.formatString("AppName", R.string.AppName));
            i.putExtra("notificationData", notificationData);

            ApplicationLoader.applicationContext.sendBroadcast(i);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public void dialogsUnreadCountIncr(final HashMap<Long, Integer> values) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (HashMap.Entry<Long, Integer> entry : values.entrySet()) {
                    TLRPC.TL_dialog dialog = dialogs_dict.get(entry.getKey());
                    if (dialog != null) {
                        dialog.unread_count += entry.getValue();
                    }
                }
                NotificationCenter.getInstance().postNotificationName(dialogsNeedReload);
            }
        });
    }

    private void updateInterfaceWithMessages(long uid, ArrayList<MessageObject> messages) {
        MessageObject lastMessage = null;
        TLRPC.TL_dialog dialog = dialogs_dict.get(uid);

        boolean isEncryptedChat = ((int) uid) == 0;

        NotificationCenter.getInstance().postNotificationName(didReceivedNewMessages, uid, messages);

        for (MessageObject message : messages) {

        }

        boolean changed = false;


        if (changed) {
            dialogsServerOnly.clear();
            Collections.sort(dialogs, new Comparator<TLRPC.TL_dialog>() {
                @Override
                public int compare(TLRPC.TL_dialog tl_dialog, TLRPC.TL_dialog tl_dialog2) {
                    if (tl_dialog.last_message_date == tl_dialog2.last_message_date) {
                        return 0;
                    } else if (tl_dialog.last_message_date < tl_dialog2.last_message_date) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
            for (TLRPC.TL_dialog d : dialogs) {
                if ((int) d.id != 0) {
                    dialogsServerOnly.add(d);
                }
            }
        }
    }

    public TLRPC.Message decryptMessage(TLRPC.EncryptedMessage message) {

        return null;
    }

    public static class PrintingUser {
        public long lastTime;
        public int userId;
    }

    private class UserActionUpdates extends TLRPC.Updates {

    }

    static {
        try {
            File URANDOM_FILE = new File("/dev/urandom");
            FileInputStream sUrandomIn = new FileInputStream(URANDOM_FILE);
            byte[] buffer = new byte[1024];
            sUrandomIn.read(buffer);
            sUrandomIn.close();
            random.setSeed(buffer);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    private class DelayedMessage {
        public TLRPC.TL_messages_sendMedia sendRequest;
        public TLRPC.TL_decryptedMessage sendEncryptedRequest;
        public int type;
        public TLRPC.FileLocation location;
        public TLRPC.TL_video videoLocation;
        public TLRPC.TL_audio audioLocation;
        public TLRPC.TL_document documentLocation;
        public MessageObject obj;
        public TLRPC.EncryptedChat encryptedChat;
    }
}
