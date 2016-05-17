package com.yahala.xmpp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import com.yahala.SQLite.Messages;
import com.yahala.android.OSUtilities;
import com.yahala.android.NotificationsController;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Rows.ConnectionsManager;
import com.yahala.ui.LaunchActivity;
import com.yahala.xmpp.call.WebRtcPhone;
import com.yahala.xmpp.customProviders.OutOfBandData;
import com.yahala.xmpp.customProviders.PhoneCall;
import com.yahala.xmpp.customProviders.UserLocationExtension;

import org.appspot.apprtc.CallActivity;
import org.appspot.apprtc.CallFragment;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppStringUtils;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by user on 4/16/2014.
 */
public class MessagesController {
    public static final int updateInterfaces = 3;
    public static final int dialogsNeedReload = 4;
    public static final int closeChats = 5;
    public static final int messagesDeleted = 6;
    public static final int messagesReaded = 7;
    public static final int chatStateUpdated = 8;
    public static final int messagesDidLoadedd = 1010;
    public static final int dialogDidLoaded = 1110;
    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;
    public static final int messageSendError = 11;
    public static final int UPDATE_MASK_NAME = 1;
    public static final int UPDATE_MASK_AVATAR = 2;
    public static final int UPDATE_MASK_STATUS = 4;
    public static final int UPDATE_MASK_CHAT_AVATAR = 8;
    public static final int UPDATE_MASK_CHAT_NAME = 16;
    public static final int UPDATE_MASK_CHAT_MEMBERS = 32;
    public static final int UPDATE_MASK_USER_PRINT = 64;
    public static final int UPDATE_MASK_USER_PHONE = 128;
    public static final int UPDATE_MASK_READ_DIALOG_MESSAGE = 256;
    private CallFragment.OnCallEvents callEvents;
    public static final int UPDATE_MASK_ALL = UPDATE_MASK_AVATAR | UPDATE_MASK_STATUS | UPDATE_MASK_NAME | UPDATE_MASK_CHAT_AVATAR | UPDATE_MASK_CHAT_NAME | UPDATE_MASK_CHAT_MEMBERS | UPDATE_MASK_USER_PRINT | UPDATE_MASK_USER_PHONE | UPDATE_MASK_READ_DIALOG_MESSAGE;
    private static volatile MessagesController Instance = null;
    public int fontSize = OSUtilities.dp(16);
    public MessageObject currentPushMessage;
    public ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<TLRPC.TL_dialog>();
    public HashMap<String, TLRPC.TL_dialog> dialogs_dict = new HashMap<String, TLRPC.TL_dialog>();
    public boolean dialogsLoaded = false;
    public ConcurrentHashMap<String, TLRPC.User> users = new ConcurrentHashMap<String, TLRPC.User>(100, 1.0f, 2);
    public ConcurrentHashMap<String, ArrayList<PrintingUser>> printingUsers = new ConcurrentHashMap<String, ArrayList<PrintingUser>>(100, 1.0f, 2);
    public HashMap<String, CharSequence> printingStrings = new HashMap<String, CharSequence>();
    public String openned_dialog_id;
    private SoundPool soundPool;
    private int sound;
    private long lastSoundPlay = 0;
    private int lastPrintingStringCount = 0;

    public MessagesController() {

        MessagesStorage storage = MessagesStorage.getInstance();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        fontSize = preferences.getInt("fons_size", 16);

        try {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
            sound = soundPool.load(ApplicationLoader.applicationContext, R.raw.electronic, 1);
        } catch (Exception e) {
            FileLog.e("yahala", e);
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

    public static void processUnsentMessages(List<Messages> unsentMessages) {
        for (Messages message : unsentMessages) {
            if (message.getType() == 0) {
                XMPPManager.getInstance().sendOfflineMessage(message);
            }
        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(updateInterfaces);
            }
        });

    }

    public void LoadDialogs(boolean update) {
        MessagesStorage.getInstance().getDialogs(update);
    }

    public TLRPC.TL_photo generatePhotoSizes(String path, Uri imageUri) {
        long time = System.currentTimeMillis();
        Bitmap bitmap = FileLoader.loadBitmap(path, imageUri, 800, 800);
        FileLog.e("mesagesController generatePhotoSizes", path + " ");
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
            FileLog.e("test sizes.isEmpty()", "isEmpty");
            return null;
        } else {
            UserConfig.saveConfig(false);
            TLRPC.TL_photo photo = new TLRPC.TL_photo();
            photo.user_id = UserConfig.clientUserId + "";
            photo.date = ConnectionsManager.getInstance().getCurrentTime();
            photo.sizes = sizes;
            photo.caption = "";
            photo.geo = new TLRPC.TL_geoPointEmpty();
            return photo;
        }
    }

    //db.execute('DELETE FROM producten WHERE id NOT IN (' + ids.join(",") + ')');
    public void deleteMessages(ArrayList<Integer> messages) {
        MessagesStorage.getInstance().deleteMessages(messages);
        // MessagesStorage.getInstance().markMessagesAsDeleted(messages, true);
        //MessagesStorage.getInstance().updateDialogsWithDeletedMessages(messages, true);
        NotificationCenter.getInstance().postNotificationName(messagesDeleted, messages);


    }

    public void processLoadedDialogs(ArrayList<TLRPC.TL_dialog> ds, boolean update) {
        Collections.sort((List) ds, new Comparator<TLRPC.TL_dialog>() {

            @Override
            public int compare(TLRPC.TL_dialog tl_dialog1, TLRPC.TL_dialog tl_dialog2) {
                //TLRPC.TL_dialog dialog1 = dialogs_dict.get(tl_dialog1.jid);
                // TLRPC.TL_dialog dialog2 = dialogs_dict.get(tl_dialog2.jid);
                long t1 = tl_dialog1.topMessage.getDate().getTime();
                long t2 = tl_dialog2.topMessage.getDate().getTime();
                if (t2 > t1)
                    return 1;
                else if (t1 > t2)
                    return -1;
                else
                    return 0;
            }
        });
        dialogs = ds;
        for (TLRPC.TL_dialog dialog : dialogs) {
            TLRPC.User c = ContactsController.getInstance().friendsDict.get(dialog.jid);
            if (c != null) {
                dialog.presence = c.presence;
                dialog.avatar = c.avatar;
                dialog.avatarUrl = c.avatarUrl;
            }
            dialogs_dict.put(dialog.jid, dialog);
        }
        //Utilities.RunOnUIThread(new Runnable() {
        //   @Override
        //    public void run() {
        if (update)
            NotificationCenter.getInstance().postNotificationName(dialogDidLoaded);
        dialogsLoaded = true;
        //  }
        //});

    }

    public void processLoadedMessages(final List<Messages> messages, final String dialog_id, final int offset, final int count, final long max_id, final boolean isCache, final int classGuid, final int first_unread, final int last_unread, final int unread_count, final int last_date, final boolean isForward) {
        final ArrayList<MessageObject> objects = new ArrayList<MessageObject>();
        for (Messages message : messages) {
            // message.dialog_id = dialog_id;
            message.id = (int) (long) message.getId();

            message = MessagesStorage.getInstance().getMedia(message);
            objects.add(new MessageObject(message, null));
        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {

                NotificationCenter.getInstance().postNotificationName(messagesDidLoadedd, dialog_id, offset, count, objects, isCache, first_unread, last_unread, unread_count, last_date, isForward);
            }
        });

    }

    public void loadMessages(final String dialog_id, final int offset, final int count, final long max_id, boolean fromCache, Date midDate, final int classGuid, boolean from_unread, boolean forward) {
        //int lower_part = (int)dialog_id;
        if (fromCache) {
            MessagesStorage.getInstance().getMessages(dialog_id, offset, count, max_id, midDate, classGuid, from_unread, forward);
        } else {
        }
    }

    public void markDialogAsRead(final String dialog_id, Messages messages) {
        messages.setRead_state(1);
        MessagesStorage.getInstance().updateMessage(messages);
    }

    public void updatePrintingStrings() {
        final HashMap<String, CharSequence> newPrintingStrings = new HashMap<String, CharSequence>();

        ArrayList<String> keys = new ArrayList<String>(printingUsers.keySet());
        for (String key : keys) {
            if (key != "" || key == "0") {
                newPrintingStrings.put(key, LocaleController.getString("Typing", R.string.Typing));
            } else {
                ArrayList<PrintingUser> arr = printingUsers.get(key);
                int count = 0;
                String label = "";
                for (PrintingUser pu : arr) {
                    TLRPC.User user = users.get(pu.jid);
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

        lastPrintingStringCount = newPrintingStrings.size();

        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                printingStrings = newPrintingStrings;
            }
        });
    }

    public void processChatMessage(Message m) {
        final String fromJid = m.getFrom().toString().replace("/android", "");
        FileLog.e("Message Controller", "fromJid: " + fromJid);

        final ExtensionElement chatStateExtension = m.getExtension("http://jabber.org/protocol/chatstates");
        final ExtensionElement userLocationExtension = m.getExtension("http://jabber.org/protocol/geoloc");
        if (chatStateExtension != null && chatStateExtension instanceof ChatStateExtension) {
            if (chatStateExtension.getElementName() != null) {
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(chatStateUpdated, chatStateExtension.getElementName(), fromJid);
                    }
                });

            }
            // FileLog.e("processChatMessage","new state: " + chatStateExtension.getElementName());
        }
        if (DeliveryReceiptManager.hasDeliveryReceiptRequest(m)) {
        /* // DeliveryReceipt p= new DeliveryReceipt(m.getPacketID());

          Stanza received = new Message();

          received.addExtension(new DeliveryReceipt(m.getStanzaId()));
          received.setTo(m.getFrom());


          try {
              XMPPManager.getInstance().connection.sendStanza(received);
          } catch (SmackException.NotConnectedException e) {
              e.printStackTrace();
          }*/
        }

        if (userLocationExtension != null && userLocationExtension instanceof UserLocationExtension) {
            FileLog.e("userLocationExtension", userLocationExtension.toXML().toString());

            if (userLocationExtension.getElementName() != null) {
                final Messages newMsg = new Messages();
                newMsg.setId(Long.parseLong(String.valueOf(UserConfig.getNewMessageId())));
                newMsg.setJid(fromJid);
                newMsg.setMessage("");
                newMsg.setRead_state(0);
                newMsg.setOut(0);
                newMsg.setDate(new Date());
                newMsg.setType(5);
                newMsg.setMessage("");
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.message = "";
                newMsg.tl_message.media = new TLRPC.TL_messageMediaGeo();
                newMsg.tl_message.media.geo = new TLRPC.TL_geoPoint();
                newMsg.tl_message.media.geo.lat = ((UserLocationExtension) userLocationExtension).location.lat;
                newMsg.tl_message.media.geo._long = ((UserLocationExtension) userLocationExtension).location.lon;

                newMsg.tl_message.from_id = fromJid;

                // newMsg.tl_message.date=0;
                //create the message obj and get its local id


                if (!ContactsController.getInstance().friendsDict.containsKey(fromJid)) {
                    MessagesStorage.getInstance().putContactToCache(fromJid);
                    ContactsController.getInstance().readContacts(false);
                }

                MessageObject messageObject = new MessageObject(newMsg, null);
                ArrayList<MessageObject> messageObjects = new ArrayList<MessageObject>();
                messageObjects.add(messageObject);
                NotificationsController.getInstance().processNewMessages(messageObjects, true);

                //////////// showInAppNotification(messagesObject);
                /* XmppManager.getInstance().postNotification(LocaleController.getString("NotificationMessageMap",R.string.NotificationMessageMap), ContactsController.getInstance().friendsDict.get(fromJid).first_name + " " +
                        ContactsController.getInstance().friendsDict.get(fromJid).last_name);*/
                UserConfig.saveConfig(false);
                MessagesStorage.getInstance().putMessage(newMsg);

                //test
                // YXmppNotifications.getInstance().messageEventManager.sendDeliveredNotification(fromJid,packet.getPacketID());
                // YXmppNotifications.getInstance().messageEventManager.sendCancelledNotification(fromJid,packet.getPacketID());

                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                        NotificationCenter.getInstance().postNotificationName(XMPPManager.didReceivedNewMessages, newMsg);
                    }
                });

                return;
            }
            // FileLog.e("processChatMessage","new state: " + chatStateExtension.getElementName());
        }
        // FileLog.e("MessagesController","got message: " + m.toXML());

        //final Message message = (Message) packet;

        // Packet received = new Message();
        // received.addExtension(new DeliveryReceipt(packet.getPacketID()));
        // received.setTo(packet.getFrom());
        // getConnection().sendPacket(received);
        //FileLog.e("test", "packet DeliveryReceipt sent to [" + m.getFrom() + "]");

        if (m.getBody() != null) {

            //FileLog.e("yahala", "Got text [" + m.getBody() + "] from [" + fromJid + "]");
            if (!ContactsController.getInstance().friendsDict.containsKey(fromJid)) {
                MessagesStorage.getInstance().putContactToCache(fromJid);
                ContactsController.getInstance().readContacts(false);
            }

            final Messages newMsg = new Messages();
            newMsg.setId(Long.parseLong(String.valueOf(UserConfig.getNewMessageId())));
            newMsg.setJid(fromJid);

            newMsg.setMessage(m.getBody());
            newMsg.setRead_state(0);

            newMsg.setOut(0);

            newMsg.setDate(new Date());

            // out of band data
            ExtensionElement _media = m.getExtension("x", "jabber:x:oob");
            ExtensionElement _call = m.getExtension("x", "jabber:x:pc");

            if (_media != null && _media instanceof OutOfBandData) {
                //FileLog.e("MessagesController","out of band data not supported yet ." + m.toXML());
                newMsg.setMessage("-1");
                newMsg.setRead_state(0);
                FileLoader.getInstance().loadFile(((OutOfBandData) _media).getUrl(), newMsg);
                return;
            }

            if (_call != null && _call instanceof PhoneCall) {
                //FileLog.e("MessagesController","out of band data not supported yet ." + m.toXML());
                newMsg.setMessage("-1");
                newMsg.setRead_state(0);

                /*FileLog.e("Message Controller", "Call received: roomId(" + ((PhoneCall) _call).getRoomId() + ")");
                FileLog.e("Message Controller", "_call).getCallHangUp(): " + ((PhoneCall) _call).getCallHangUp() );
                FileLog.e("Message Controller", "_call).getVideoCall(): " + ((PhoneCall) _call).getVideoCall() );*/

                if (((PhoneCall) _call).getCallHangUp()) {
                    FileLog.e("MessageController", "onCallHangUp");
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(CallActivity.CALL_HANGUP);
                        }
                    });
                } else {
                    WebRtcPhone.getInstance().jid = newMsg.getJid();
                    WebRtcPhone.getInstance().Call(((PhoneCall) _call).getRoomId(), false, ((PhoneCall) _call).getVideoCall());
                }

                return;
            }

            newMsg.setType(0);
            newMsg.tl_message = new TLRPC.TL_message();
            newMsg.tl_message.message = m.getBody();
            newMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();
            newMsg.tl_message.from_id = fromJid;
            UserConfig.saveConfig(false);
            MessagesStorage.getInstance().putMessage(newMsg);

            //test
            // YXmppNotifications.getInstance().messageEventManager.sendDeliveredNotification(fromJid,packet.getPacketID());

            // YXmppNotifications.getInstance().messageEventManager.sendCancelledNotification(fromJid,packet.getPacketID());
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.didReceivedNewMessages, newMsg);
                }
            });

            MessageObject messageObject = new MessageObject(newMsg, null);
            ArrayList<MessageObject> messageObjects = new ArrayList<MessageObject>();
            messageObjects.add(messageObject);
            NotificationsController.getInstance().processNewMessages(messageObjects, true);
            // showInAppNotification(messageObject);
        } else {
            return;
        }

        /*XmppManager.getInstance().postNotification(m.getBody(), ContactsController.getInstance().friendsDict.get(fromJid).first_name + " " +
                ContactsController.getInstance().friendsDict.get(fromJid).last_name);*/
    }

    private void playNotificationSound() {
        if (lastSoundPlay > System.currentTimeMillis() - 1800) {
            return;
        }
        try {
            lastSoundPlay = System.currentTimeMillis();
            soundPool.play(sound, 1, 1, 1, 0, 1);
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
    }

    public void showInAppNotification(MessageObject messageObject) {
        if (!UserConfig.isClientActivated() || messageObject.isOut()) {
            return;
        }

        if (ConnectionsManager.lastPauseTime != 0) {
            ConnectionsManager.lastPauseTime = System.currentTimeMillis();
            FileLog.e("yahala", "reset sleep timeout by received message");
        }
        if (messageObject == null) {
            return;
        }

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        boolean globalEnabled = preferences.getBoolean("EnableAll", true);
        boolean groupEnabled = preferences.getBoolean("EnableGroup", true);

        String dialog_id = messageObject.messageOwner.getJid();
        String chat_id = "0";/*messageObject.messageOwner.tl_message.to_id.chat_id;*/
        String user_jid = messageObject.messageOwner.getJid();
        if (user_jid == "0") {
            user_jid = messageObject.messageOwner.tl_message.from_id;
        } else if (user_jid == UserConfig.currentUser.phone) {
            user_jid = messageObject.messageOwner.tl_message.from_id;
        }

       /* if (dialog_id == 0) {
            if (chat_id != "0") {
                dialog_id = -chat_id;
            } else if (user_id != 0) {
                dialog_id = user_id;
            }
        }*/

        int notify_override = preferences.getInt("notify2_" + dialog_id, 0);
        if (notify_override == 2 || (!globalEnabled || chat_id != "0" && !groupEnabled) && notify_override == 0) {
            //FileLog.e("tmessages", "notify_override == 2");
            return;
        }

        TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_jid);
        if (user == null) {
            //  FileLog.e("tmessages", "user == null");
            return;
        }
        /*TLRPC.Chat chat = null;
        if (chat_id != 0) {
            chat = chats.get(chat_id);
            if (chat == null) {
                return;
            }
        }*/
        FileLog.e("yahala", "int vibrate_override");

        int vibrate_override = preferences.getInt("vibrate_" + dialog_id, 0);

      /*  if (ConnectionsManager.lastPauseTime == 0 && ApplicationLoader.isScreenOn) {
            boolean inAppSounds = preferences.getBoolean("EnableInAppSounds", true);
            boolean inAppVibrate = preferences.getBoolean("EnableInAppVibrate", true);
            boolean inAppPreview = preferences.getBoolean("EnableInAppPreview", true);
            FileLog.e("tmessages", "notify_override == 2");
            if (inAppSounds || inAppVibrate || inAppPreview) {

                FileLog.e("tmessages", "ConnectionsManager.lastPauseTime == 0 && ApplicationLoader.isScreenOn");
                if (inAppPreview) {
                    NotificationCenter.getInstance().postNotificationName(701, messageObject);
                }
                if (inAppVibrate && vibrate_override == 0 || vibrate_override == 1) {
                    Vibrator v = (Vibrator)ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(100);
                }
                if (inAppSounds) {
                    playNotificationSound();
                }

            }
        } else {*/

        TLRPC.FileLocation photoPath = null;
        //String defaultPath = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.getPath();
        String defaultPath = "android.resource://com.yahala.app/" + R.raw.electronic;
        NotificationManager mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
        String msg = null;

        if (dialog_id != "0") {
            if (!chat_id.equals("0")) {
                intent.putExtra("chat_Id", chat_id);
            } else if (!user_jid.equals("0")) {
                intent.putExtra("user_jid", user_jid);
            }

            if (user.photo != null && user.photo.photo_small != null && user.photo.photo_small.volume_id != 0 && user.photo.photo_small.local_id != 0) {
                photoPath = user.photo.photo_small;
            }

            if (!user_jid.equals("0")) {
                if (preferences.getBoolean("EnablePreviewAll", true)) {
                    if (messageObject.messageOwner.tl_message instanceof TLRPC.TL_messageService) {
                        if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionUserJoined) {
                            msg = LocaleController.formatString("NotificationContactJoined", R.string.NotificationContactJoined, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionUserUpdatedPhoto) {
                            msg = LocaleController.formatString("NotificationContactNewPhoto", R.string.NotificationContactNewPhoto, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionLoginUnknownLocation) {
                            String date = String.format("%s %s %s", LocaleController.formatterYear.format(((long) messageObject.messageOwner.getDate().getTime()) * 1000), LocaleController.getString("OtherAt", R.string.OtherAt), LocaleController.formatterDay.format(((long) messageObject.messageOwner.getDate().getTime()) * 1000));
                            msg = LocaleController.formatString("NotificationUnrecognizedDevice", R.string.NotificationUnrecognizedDevice, UserConfig.getCurrentUser().first_name, date, messageObject.messageOwner.tl_message.action.title, messageObject.messageOwner.tl_message.action.address);
                        }
                    } else {
                        if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaEmpty) {
                            if (messageObject.messageOwner.getMessage() != null && messageObject.messageOwner.getMessage().length() != 0) {
                                msg = LocaleController.formatString("NotificationMessageText", R.string.NotificationMessageText, Utilities.formatName(user.first_name, user.last_name), messageObject.messageOwner.getMessage());
                            } else {
                                msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, Utilities.formatName(user.first_name, user.last_name));
                            }
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaPhoto) {
                            msg = LocaleController.formatString("NotificationMessagePhoto", R.string.NotificationMessagePhoto, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaVideo) {
                            msg = LocaleController.formatString("NotificationMessageVideo", R.string.NotificationMessageVideo, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaContact) {
                            msg = LocaleController.formatString("NotificationMessageContact", R.string.NotificationMessageContact, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaGeo) {
                            msg = LocaleController.formatString("NotificationMessageMap", R.string.NotificationMessageMap, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
                            msg = LocaleController.formatString("NotificationMessageDocument", R.string.NotificationMessageDocument, Utilities.formatName(user.first_name, user.last_name));
                        } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaAudio) {
                            msg = LocaleController.formatString("NotificationMessageAudio", R.string.NotificationMessageAudio, Utilities.formatName(user.first_name, user.last_name));
                        }
                    }
                } else {
                    msg = LocaleController.formatString("NotificationMessageNoText", R.string.NotificationMessageNoText, Utilities.formatName(user.first_name, user.last_name));
                }
            }/* else if (chat_id != "0") {
                    if (preferences.getBoolean("EnablePreviewGroup", true)) {
                        if (messageObject.messageOwner.tl_message instanceof TLRPC.TL_messageService) {
                            if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionChatAddUser) {
                                if (messageObject.messageOwner.tl_message.action.user_id == UserConfig.getClientUserId()) {
                                    msg = LocaleController.formatString("NotificationInvitedToGroup", R.string.NotificationInvitedToGroup, Utilities.formatName(user.first_name, user.last_name), chat.title);
                                } else {
                                    TLRPC.User u2 = users.get(messageObject.messageOwner.tl_message.action.user_id);
                                    if (u2 == null) {
                                        return;
                                    }
                                    msg = LocaleController.formatString("NotificationGroupAddMember", R.string.NotificationGroupAddMember, Utilities.formatName(user.first_name, user.last_name), chat.title, Utilities.formatName(u2.first_name, u2.last_name));
                                }
                            } else if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionChatEditTitle) {
                                msg = LocaleController.formatString("NotificationEditedGroupName", R.string.NotificationEditedGroupName, Utilities.formatName(user.first_name, user.last_name), messageObject.messageOwner.tl_message.action.title);
                            } else if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionChatEditPhoto || messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionChatDeletePhoto) {
                                msg = LocaleController.formatString("NotificationEditedGroupPhoto", R.string.NotificationEditedGroupPhoto, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.action instanceof TLRPC.TL_messageActionChatDeleteUser) {
                                if (messageObject.messageOwner.tl_message.action.user_id == UserConfig.getClientUserId()) {
                                    msg = LocaleController.formatString("NotificationGroupKickYou", R.string.NotificationGroupKickYou, Utilities.formatName(user.first_name, user.last_name), chat.title);
                                } else if (messageObject.messageOwner.tl_message.action.user_id == user.id) {
                                    msg = LocaleController.formatString("NotificationGroupLeftMember", R.string.NotificationGroupLeftMember, Utilities.formatName(user.first_name, user.last_name), chat.title);
                                } else {
                                    TLRPC.User u2 = users.get(messageObject.messageOwner.tl_message.action.user_id);
                                    if (u2 == null) {
                                        return;
                                    }
                                    msg = LocaleController.formatString("NotificationGroupKickMember", R.string.NotificationGroupKickMember, Utilities.formatName(user.first_name, user.last_name), chat.title, Utilities.formatName(u2.first_name, u2.last_name));
                                }
                            }
                        } else {
                            if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaEmpty) {
                                if (messageObject.messageOwner.tl_message.message != null && messageObject.messageOwner.tl_message.message.length() != 0) {
                                    msg = LocaleController.formatString("NotificationMessageGroupText", R.string.NotificationMessageGroupText, Utilities.formatName(user.first_name, user.last_name), chat.title, messageObject.messageOwner.getMessage());
                                } else {
                                    msg = LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, Utilities.formatName(user.first_name, user.last_name), chat.title);
                                }
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaPhoto) {
                                msg = LocaleController.formatString("NotificationMessageGroupPhoto", R.string.NotificationMessageGroupPhoto, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaVideo) {
                                msg = LocaleController.formatString("NotificationMessageGroupVideo", R.string.NotificationMessageGroupVideo, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaContact) {
                                msg = LocaleController.formatString("NotificationMessageGroupContact", R.string.NotificationMessageGroupContact, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaGeo) {
                                msg = LocaleController.formatString("NotificationMessageGroupMap", R.string.NotificationMessageGroupMap, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaDocument) {
                                msg = LocaleController.formatString("NotificationMessageGroupDocument", R.string.NotificationMessageGroupDocument, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            } else if (messageObject.messageOwner.tl_message.media instanceof TLRPC.TL_messageMediaAudio) {
                                msg = LocaleController.formatString("NotificationMessageGroupAudio", R.string.NotificationMessageGroupAudio, Utilities.formatName(user.first_name, user.last_name), chat.title);
                            }
                        }
                    } else {
                        msg = LocaleController.formatString("NotificationMessageGroupNoText", R.string.NotificationMessageGroupNoText, Utilities.formatName(user.first_name, user.last_name), chat.title);
                    }*/
        }
        FileLog.e("msg msg msg msg: ", msg);
        if (msg == null) {
            return;
        }

        boolean needVibrate = false;
        String choosenSoundPath = null;
        int ledColor = 0xff00ff00;

        choosenSoundPath = preferences.getString("sound_path_" + dialog_id, null);

           /* if (chat_id != 0) {
                if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                    choosenSoundPath = null;
                } else if (choosenSoundPath == null) {
                    choosenSoundPath = preferences.getString("GroupSoundPath", defaultPath);
                }
                needVibrate = preferences.getBoolean("EnableVibrateGroup", true);
                ledColor = preferences.getInt("GroupLed", 0xff00ff00);
            } else */
        if (user_jid != "0") {
            if (choosenSoundPath != null && choosenSoundPath.equals(defaultPath)) {
                choosenSoundPath = null;
            } else if (choosenSoundPath == null) {
                choosenSoundPath = preferences.getString("GlobalSoundPath", defaultPath);
            }
            needVibrate = preferences.getBoolean("EnableVibrateAll", true);
            ledColor = preferences.getInt("MessagesLed", 0xff00ff00);
        }
        if (preferences.contains("color_" + dialog_id)) {
            ledColor = preferences.getInt("color_" + dialog_id, 0);
        }

        if (!needVibrate && vibrate_override == 1) {
            needVibrate = true;
        } else if (needVibrate && vibrate_override == 2) {
            needVibrate = false;
        }

        String name = Utilities.formatName(user.first_name, user.last_name);
        if (dialog_id == "0") {
            name = LocaleController.getString("AppName", R.string.AppName);
        }
        String msgShort = msg.replace(name + ": ", "").replace(name + " ", "");

        intent.setAction("com.yahala.openchat" + Math.random() + Integer.MAX_VALUE);
        intent.setFlags(32768);
        PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                .setContentTitle(name)
                .setSmallIcon(R.drawable.ic_ab_logo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msgShort))
                .setContentText(msgShort)
                .setAutoCancel(true)
                .setTicker(msg)
                .setLargeIcon(user.avatar);
        if (photoPath != null) {
            Bitmap img = user.avatar; //FileLoader.getInstance().getImageFromMemory(photoPath, null, null, "50_50", false);
            if (img != null) {
                //     mBuilder.setLargeIcon(img);
            }
        }

        if (choosenSoundPath != null && !choosenSoundPath.equals("NoSound")) {
            if (choosenSoundPath.equals(defaultPath)) {
                mBuilder.setSound(Uri.parse(defaultPath), AudioManager.STREAM_NOTIFICATION);
            } else {
                mBuilder.setSound(Uri.parse(choosenSoundPath), AudioManager.STREAM_NOTIFICATION);
            }
        }

        currentPushMessage = null;
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.cancel(1);
        Notification notification = mBuilder.build();
        if (ledColor != 0) {
            notification.ledARGB = ledColor;
        }
        notification.ledOnMS = 1000;
        notification.ledOffMS = 1000;
        if (needVibrate) {
            notification.vibrate = new long[]{0, 100, 0, 100};
        } else {
            notification.vibrate = new long[]{0, 0};
        }
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        try {
            mNotificationManager.notify(1, notification);
            if (preferences.getBoolean("EnablePebbleNotifications", false)) {
                sendAlertToPebble(msg);
            }
            currentPushMessage = messageObject;
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        // }
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


    public static class PrintingUser {
        public long lastTime;
        public String jid;
    }

    private void updateInterfaceWithMessages(String uid, ArrayList<MessageObject> messages) {
        //MessageObject lastMessage = null;
        //TLRPC.TL_dialog dialog = dialogs_dict.get(uid);


        NotificationCenter.getInstance().postNotificationName(XMPPManager.didReceivedNewMessages, uid, messages);
/*
        for (MessageObject message : messages) {
            if (lastMessage == null || (message.messageOwner.id < lastMessage.messageOwner.id) || message.messageOwner.getDate().after(lastMessage.messageOwner.getDate())) {
                lastMessage = message;
            }
        }

        boolean changed = false;

        if (dialog == null) {
            dialog = new TLRPC.TL_dialog();
            dialog.id = uid;
            dialog.unread_count = 0;
            dialog.top_message = lastMessage.messageOwner.id;
            dialog.last_message_date = lastMessage.messageOwner.getDate();
            //dialog.lastRead=dialog;

            dialogs_dict.put(uid, dialog);
            dialogs.add(dialog);

           // dialogMessage.put(lastMessage.messageOwner.id, lastMessage);
            changed = true;
        } else {
            if (dialog.top_message > 0 && lastMessage.messageOwner.id > 0 && lastMessage.messageOwner.id > dialog.top_message ||
                    dialog.top_message < 0 && lastMessage.messageOwner.id < 0 && lastMessage.messageOwner.id < dialog.top_message ||
                    dialog.last_message_date < lastMessage.messageOwner.date) {
                dialogMessage.remove(dialog.top_message);
                dialog.top_message = lastMessage.messageOwner.id;
                dialog.last_message_date = lastMessage.messageOwner.date;
                dialogMessage.put(lastMessage.messageOwner.id, lastMessage);
                changed = true;
            }
        }

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
                if ((int)d.id != 0) {
                    dialogsServerOnly.add(d);
                }
            }
        }*/
    }

}
