package com.yahala.xmpp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.yahala.SQLite.Chats;
import com.yahala.SQLite.ChatsDao;
import com.yahala.SQLite.Contacts;
import com.yahala.SQLite.ContactsDao;
import com.yahala.SQLite.DaoMaster;
import com.yahala.SQLite.DaoSession;
import com.yahala.SQLite.Messages;
import com.yahala.SQLite.MessagesDao;
import com.yahala.SQLite.Wallpapers;
import com.yahala.SQLite.WallpapersDao;
import com.yahala.android.NotificationsController;
import com.yahala.messenger.DispatchQueue;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.SerializedData;
import com.yahala.messenger.TLClassStore;
import com.yahala.messenger.TLRPC;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import com.yahala.messenger.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;

/**
 * Created by user on 4/13/2014.
 */
public class MessagesStorage {
    // DaoMaster.DevOpenHelper databaseHelper;

    public static final int wallpapersDidLoaded = 171;
    private static volatile MessagesStorage Instance = null;
    public DispatchQueue storageQueue = new DispatchQueue("storageQueue");
    public DispatchQueue storageQueueAlt = new DispatchQueue("storageQueueAlt");


    public MessagesStorage() {
        storageQueue.setPriority(Thread.MAX_PRIORITY);
        openDatabase();
    }

    public static MessagesStorage getInstance() {
        MessagesStorage localInstance = Instance;
        if (localInstance == null) {
            synchronized (MessagesStorage.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MessagesStorage();
                }
            }
        }
        return localInstance;
    }

    public ArrayList<TLRPC.User> getUsers(final ArrayList<String> jids, final boolean[] error) {
        ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
        for (String jid : jids) {
            TLRPC.User user = ContactsController.getInstance().friendsDict.get(jids);
            if (user != null) {
                users.add(user);
            }

        }
       /* try {
            String uidsStr = "";

            for (Integer uid : uids) {
                if (uidsStr.length() != 0) {
                    uidsStr += ",";
                }
                uidsStr += uid;
            }

            SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, status FROM users WHERE uid IN (%s)", uidsStr));
            while (cursor.next()) {
                byte[] userData = cursor.byteArrayValue(0);
                if (userData != null) {
                    SerializedData data = new SerializedData(userData);
                    TLRPC.User user = (TLRPC.User) TLClassStore.Instance().TLdeserialize(data, data.readInt32());
                    if (user != null) {
                        if (user.status != null) {
                            user.status.expires = cursor.intValue(1);
                        }
                        users.add(user);
                    } else {
                        error[0] = true;
                        break;
                    }
                } else {
                    error[0] = true;
                    break;
                }
            }
            cursor.dispose();
        } catch (Exception e) {
            error[0] = true;
            FileLog.e("tmessages", e);
        }*/
        return users;
    }

    public void putContactsToCache() {
        if (ContactsController.getInstance().ServerContacts.size() > 0)

            for (final Map.Entry friend : ContactsController.getInstance().ServerContacts.entrySet()) {
                storageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                                "ycache.sqlite", null);
                        ContactsController.Contact contact = (ContactsController.Contact) friend.getValue();
                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                        DaoMaster daoMaster = new DaoMaster(database);
                        DaoSession daoSession = daoMaster.newSession();
                        ContactsDao contactsDao = daoSession.getContactsDao();
                        ContactsDao contactsDao2 = daoSession.getContactsDao();
                        VCard vcard = XMPPManager.getInstance().getUserVCard((String) friend.getKey());
                        FileLog.e("MessageStorege", "");
                        List<Contacts> contacts = contactsDao.queryBuilder().where(ContactsDao.Properties.Jid.eq(contact.jid)).build().list();
                        Contacts c = null;
                        if (contacts.size() > 0) {
                            contacts.get(0);
                            contacts.get(0).setFname(vcard.getFirstName());
                            contacts.get(0).setJid(contact.jid);
                            contacts.get(0).setSname(vcard.getLastName());
                            contacts.get(0).setAvatar(putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos"));
                            contactsDao.update(contacts.get(0));
                        } else {
                            c = new Contacts();
                            c.setFname(vcard.getFirstName());
                            c.setJid(contact.jid);
                            c.setSname(vcard.getLastName());
                            c.setAvatar(putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos"));
                            contactsDao.insert(c);
                        }

                        daoSession.clear();
                        database.close();
                        databaseHelper.close();
                        ContactsController.getInstance().readContacts(false);
                    }
                });


            }
    }

    public void putContactToCache(final String jid) {


        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                ContactsDao contactsDao = daoSession.getContactsDao();
                VCard vcard = XMPPManager.getInstance().getUserVCard(jid);
                ContactsController.Contact c = null;
                List<Contacts> contacts = contactsDao.queryBuilder().where(ContactsDao.Properties.Jid.eq(jid)).build().list();

                if (contacts.size() > 0) {
                    contacts.get(0);
                    contacts.get(0).setFname(vcard.getFirstName());
                    contacts.get(0).setJid(jid);
                    contacts.get(0).setSname(vcard.getLastName());
                    contacts.get(0).setAvatar(putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos"));
                    contactsDao.update(contacts.get(0));


                    c.first_name = contacts.get(0).getFname();
                    c.last_name = contacts.get(0).getSname();
                    c.jid = contacts.get(0).getJid();
                    c.avatar = contacts.get(0).getAvatar();

                } else {
                    Contacts cs = new Contacts();
                    cs.setFname(vcard.getFirstName());
                    cs.setJid(jid);
                    cs.setSname(vcard.getLastName());
                    cs.setAvatar(putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos"));
                    contactsDao.insert(cs);

                }

                if (!ContactsController.getInstance().ServerContacts.containsKey(jid)) {
                    try {
                        FileLog.e("yahala", ".ServerContacts.put");
                        ContactsController.Contact cs = new ContactsController.Contact();
                        cs.first_name = vcard.getFirstName();
                        cs.jid = jid;
                        cs.last_name = vcard.getLastName();
                        cs.avatar = putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos");
                        ContactsController.getInstance().ServerContacts.put(jid, cs);

                        XMPPManager.getInstance().addUser(cs.jid, cs.first_name + " " + cs.last_name);
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }



                   /* c.setFname(vcard.getFirstName());
                    c.setJid(jid);
                    c.setSname(vcard.getLastName());
                    c.setAvatar(putPhotoToStorage(vcard.getAvatar(), "/yahala/media/yahala Profile Photos"));

                    contactsDao.insert(c);*/
                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });


    }

    private String putPhotoToStorage(byte[] photo, String path) {
        if (photo != null) {
            Bitmap bmp;
            bmp = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);


            String root = Environment.getExternalStorageDirectory().toString();

            File myDir = new File(root + path);
            myDir.mkdirs();
            Random generator = new Random();
            int n = 10000;
            n = generator.nextInt(n);
            String fname = "profile-" + n + ".jpg";

            File file = new File(myDir, fname);
            FileLog.e("Test Photo to storage", "" + file);
            if (file.exists())
                file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return root + path + "/" + fname;
        }
        return "";
    }

    public void openDatabase() {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster.createAllTables(database, true);
        database.close();
        databaseHelper.close();
    }

    public void getUnsentMessages() {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                MessagesController.processUnsentMessages(
                        messagesDao.queryBuilder().where(MessagesDao.Properties.Send_state.eq(XMPPManager.MESSAGE_SEND_STATE_SENDING)).build().list()
                );

                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });

    }

    public void updateMessage(final Messages message) {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                messagesDao.update(message); //update...
                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });

    }

    public Messages getMessage(String mid) {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        MessagesDao messagesDao = daoSession.getMessagesDao();
        Messages messages = messagesDao.queryBuilder().where(MessagesDao.Properties.Id.eq(mid)).build().list().get(0);
        daoSession.clear();
        database.close();
        databaseHelper.close();
        return messages;
    }

    public void deleteAllMessages() {

        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();

                ChatsDao chatsDao = daoSession.getChatsDao();

                messagesDao.deleteAll();
                chatsDao.deleteAll();
                FileLog.e("Test deleteMessages", "delete All Messages and chats");


                daoSession.clear();
                database.close();
                databaseHelper.close();
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                    }
                });
            }
        });

    }

    public void deleteMessages(ArrayList<Integer> messages) {
        //db.execute('DELETE FROM producten WHERE id NOT IN (' + ids.join(",") + ')');
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        MessagesDao messagesDao = daoSession.getMessagesDao();
        ChatsDao chatsDao = daoSession.getChatsDao();

        //FileLog.e("Test deleteMessage", String.format(Locale.US, "id IN (%s)",join(messages,',')) + "");
        database.delete(messagesDao.getTablename(), String.format(Locale.US, "mid IN (%s)", join(messages, ',')), null);


        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
            }
        });
        daoSession.clear();
        database.close();
        databaseHelper.close();
    }

    public String join(ArrayList<Integer> list, char delimiter) {
        StringBuilder result = new StringBuilder();
        for (Iterator<?> i = list.iterator(); i.hasNext(); ) {
            result.append(i.next());
            if (i.hasNext()) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }

    public void deleteMessage(final String jid) {

        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                ChatsDao chatsDao = daoSession.getChatsDao();


                database.delete(chatsDao.getTablename(), String.format(Locale.US, "jid='%s'", jid), null);
                FileLog.e("Test deleteMessage", jid + "");

                database.delete(messagesDao.getTablename(), String.format(Locale.US, "jid='%s'", jid), null);

                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                    }
                });
                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });

    }

    public void putMessage(final Messages message) {

        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();

                SerializedData data = new SerializedData();
                message.tl_message.serializeToStream(data);

                byte[] bytes = data.toByteArray();
                message.setData(bytes);
                FileLog.e("Test putMessage bytes", bytes + "");
                //FileLog.e("Test putMessage", message.tl_message.media.video.duration +"  "+message.getMessage()+" Jid:"+message.getJid()+" video:"+ message.tl_message.media.video.thumb.location);
                messagesDao.insert(message); //insert...
                // messagesDao.delete(message); //delete...
                // messagesDao.update(message); //update...
                // customerArrayList=messagesDao.queryBuilder().where(MessagesDao.Properties.Id.eq(1)).list(); //Query...

                //List<LocalBox> boxes = getBoxDao(context).queryBuilder()
                //.where(LocalBoxDao.Properties.field.in(fieldValues)).list();// in condition
                if (getChat(message.getJid()) == null) {  //message.setJid(message.getJid().replace(XmppManager.DOMAIN,""));
                    Contacts contact = getContact(message.getJid(), true);

                    MessagesStorage.getInstance().putChat(new Chats(message.getJid(), contact.getFname() + " " + contact.getSname()));
                }
                loadUnreadMessages();
                MessageObject obj = new MessageObject(message, null);

                MessagesController.getInstance().showInAppNotification(obj);
                daoSession.clear();
                database.close();
                databaseHelper.close();

            }
        });

    }

    public ArrayList<Messages> loadMessages(String Jid) {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        MessagesDao messagesDao = daoSession.getMessagesDao();
        return (ArrayList) messagesDao.queryBuilder().limit(100).where(MessagesDao.Properties.Jid.eq(Jid)).orderDesc(MessagesDao.Properties.Id).list();
    }

    public void getMessages(final String dialog_id, final int offset, final int count, final long max_id, final Date minDate, final int classGuid, final boolean from_unread, final boolean forward) {
        //FileLog.e("Test","calling getMessages");
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                // messagesDao.deleteAll();

                int count_unread = 0;
                int count_query = count;
                int offset_query = offset;
                int min_unread_id = 0;
                int max_unread_id = 0;
                int max_unread_date = 0;
                Query query = null;
                ArrayList<Integer> loadedUsers = new ArrayList<Integer>();
                ArrayList<Integer> fromUser = new ArrayList<Integer>();
                Cursor cursor = null;
                QueryBuilder qb = messagesDao.queryBuilder();
                try {
                    // String lower_id = (int)dialog_id;

                    //   if (lower_id != 0) {

                    if (forward) {
                        //FileLog.e("yahala", "forward");
                        //   query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "jid = '%s' AND date >= %d AND mid > %d", dialog_id, minDate, max_id)))
                        //      .orderAsc(MessagesDao.Properties.Date,MessagesDao.Properties.Id).limit(count_query).build();


                        //  query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "jid = %s AND date >= %d AND mid > %d ORDER BY date ASC, mid ASC LIMIT %d", dialog_id, minDate, max_id, count_query))).build();
                        qb.where(MessagesDao.Properties.Jid.eq(dialog_id),
                                qb.and(MessagesDao.Properties.Date.ge(minDate), MessagesDao.Properties.Id.gt(max_id)))
                                .orderAsc(MessagesDao.Properties.Date, MessagesDao.Properties.Id).limit(count_query).build();

                    } else if (minDate != null) {
                        if (max_id != 0) {//FileLog.e("yahala", "max_id != 0");
                            qb.where(MessagesDao.Properties.Jid.eq(dialog_id),
                                    qb.and(MessagesDao.Properties.Date.le(minDate), MessagesDao.Properties.Id.lt(max_id)))
                                    .orderDesc(MessagesDao.Properties.Date, MessagesDao.Properties.Id).limit(count_query).build();

                            //query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "jid = %s AND date <= %d AND mid < %d ORDER BY date DESC, mid DESC LIMIT %d", dialog_id, minDate, max_id, count_query))).build();

                        } else {//FileLog.e("yahala", "else");
                            qb.where(MessagesDao.Properties.Jid.eq(dialog_id),
                                    qb.and(MessagesDao.Properties.Date.le(minDate), MessagesDao.Properties.Id.lt(max_id)))
                                    .orderDesc(MessagesDao.Properties.Date, MessagesDao.Properties.Id).limit(count_query).offset(count_query).build();
                            // query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "jid = '%s' AND date <= %d", dialog_id,minDate)))
                            //     .orderDesc(MessagesDao.Properties.Date,MessagesDao.Properties.Id).limit(count_query).offset(offset_query).build();

                            // query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, " ORDER BY date DESC, mid DESC LIMIT %d,%d", dialog_id, minDate, offset_query, count_query))).build();
                        }
                    } else {
                        //FileLog.e("yahala", "} else {");
                        if (from_unread) {
                            // FileLog.e("yahala", "  if (from_unread) {");
                            String[] cols = {"min(mid)", "max(mid)", "max(date)"};
                            String where = String.format(Locale.US, "jid = '%s' AND out = 0 AND read_state = 0 AND mid > 0", dialog_id);
                            cursor = database.query(messagesDao.getTablename(), cols, where, null, null, null, null);
                            // cursor = database.rawQuery();
                            //  query = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "SELECT min(mid), max(mid), max(date) FROM messages WHERE uid = %d AND out = 0 AND read_state = 0 AND mid > 0", dialog_id))).build();
                            // CloseableListIterator<Messages> messages=query.listIterator();
                            while (cursor.moveToNext()) {
                                min_unread_id = cursor.getInt(0);
                                max_unread_id = cursor.getInt(1);
                                max_unread_date = cursor.getInt(2);
                            }

                            cursor.close();
                        }

                        if (min_unread_id != 0) {
                            // FileLog.e("yahala", "min_unread_id != 0");
                            String[] cols = {"COUNT(*)"};
                            String where = String.format(Locale.US, "jid = '%s' AND mid >= %d AND out = 0 AND read_state = 0", dialog_id, min_unread_id);

                            Cursor c = database.query(messagesDao.getTablename(), cols, where, null, null, null, null);
                            while (c.moveToNext()) {
                                count_unread = cursor.getInt(0);
                            }
                            c.close();
                        }
                        // FileLog.e("yahala", "qb.where(MessagesDao.");
                        qb.where(MessagesDao.Properties.Jid.eq(dialog_id)).orderDesc(MessagesDao.Properties.Date, MessagesDao.Properties.Id).offset(offset_query).limit(count_query).build();

                              /*  qb = messagesDao.queryBuilder().where(new WhereCondition.StringCondition(String.format(Locale.US, "jid = '%s'", dialog_id)))
                                    .orderDesc(MessagesDao.Properties.Date,MessagesDao.Properties.Id).limit(count_query).offset(offset_query).build();*/
                    }

                    if (count_query > count_unread || count_unread < 4) {
                        count_query = Math.max(count_query, count_unread + 10);
                        if (count_unread < 4) {
                            count_unread = 0;
                            min_unread_id = 0;
                            max_unread_id = 0;
                        }
                    } else {
                        offset_query = count_unread - count_query;
                        count_query += 10;
                    }
                    //
                    //database.query(messagesDao.getTablename(), messagesDao.getAllColumns(), where, null, null, null, "date DESC, mid DESC", String.format("%d,%d",offset_query, count_query));


                    //  }
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                } finally {
                    List<Messages> messages = qb.list();

                    //FileLog.e("Test","messages count : "+messages.size());
                    MessagesController.getInstance().processLoadedMessages(messages, dialog_id, offset, count, max_id, true, classGuid, min_unread_id, max_unread_id, count_unread, max_unread_date, forward);

                    updateUnreadMessages(dialog_id);
                    loadUnreadMessages();
                    daoSession.clear();
                    database.close();
                    databaseHelper.close();
                }

            }
        });
    }

    public void updateUnreadMessages(final String dialog_id) {
        //  storageQueue.postRunnable(new Runnable() {
        // @Override
        //    public void run() {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        MessagesDao messagesDao = daoSession.getMessagesDao();
        ContentValues args = new ContentValues();
        args.put("read_state", 1);
        database.update(messagesDao.getTablename(), args, String.format(Locale.US, "jid='%s' AND out = 0 AND read_state = 0", dialog_id), null);
        //FileLog.e("test MessagesStorage", "update unread messages");
        daoSession.clear();
        database.close();
        databaseHelper.close();
    }

    //  });
    // }
    public Messages getMedia(Messages msg) {
        try {
            SerializedData data = new SerializedData(msg.getData());
            msg.tl_message = (TLRPC.TL_message) TLClassStore.Instance().TLdeserialize(data, data.readInt32());
        } catch (Exception e) {
            msg.tl_message = new TLRPC.TL_messageEmpty();
            FileLog.e("yahala", e);
        }
        return msg;
    }

    public void getContacts() {

        //storageQueue.setPriority(500);
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                ContactsDao contactsDao = daoSession.getContactsDao();

                //contactsDao.deleteAll();

                List<Contacts> contacts = contactsDao.queryBuilder().list();
                //FileLog.e("Test Contact Controller","Contact size:"+contacts.size()+"");
                //for(Contacts cc:contacts) {
                //   FileLog.e("Test MessageStorage","Contact jid:"+cc.getJid()+"");
                // }
                ContactsController.getInstance().processLoadedContacts(contacts);

                daoSession.clear();
                database.close();
                databaseHelper.close();

            }
        });
    }

    public Contacts getContact(String jid, boolean close) {
        Contacts contact = null;
       /* storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {*/
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        ContactsDao contactsDao = daoSession.getContactsDao();

        //contactsDao.deleteAll();
        List<Contacts> cs = contactsDao.queryBuilder().where(ContactsDao.Properties.Jid.eq(jid)).list();
        if (cs.size() > 0) {
            contact = cs.get(0);
            // FileLog.e("Test Contact Controller","Contact name:"+contact.getFname()+"");
        }


        // if(close)
        // {
        daoSession.clear();
        database.close();
        databaseHelper.close();
        //}
        return contact;

           /* }
        });*/
    }

    public void putChat(final Chats chat) {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();
                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                ChatsDao chatsDao = daoSession.getChatsDao();

                chatsDao.insert(chat);

                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });


    }

    public Chats getChat(final String dialog_id) {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        ChatsDao chatsDao = daoSession.getChatsDao();

        List<Chats> chats = chatsDao.queryBuilder().where(ChatsDao.Properties.Jid.eq(dialog_id)).list();
        Chats chat = null;
        if (chats.size() > 0)
            chat = chatsDao.queryBuilder().where(ChatsDao.Properties.Jid.eq(dialog_id)).list().get(0);

        daoSession.clear();
        database.close();
        databaseHelper.close();
        return chat;
    }

    public void loadUnreadMessages() {


        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                final HashMap<String, Integer> pushDialogs = new HashMap<String, Integer>();
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();

                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                ChatsDao chatsDao = daoSession.getChatsDao();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                List<Chats> chats = chatsDao.queryBuilder().list();
                ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<TLRPC.TL_dialog>();

                for (Chats chat : chats) {
                    try {
                        int unread_count = (int) (long) messagesDao.queryBuilder().where(
                                messagesDao.queryBuilder().and(MessagesDao.Properties.Jid.eq(chat.getJid()), MessagesDao.Properties.Read_state.eq(0),
                                        MessagesDao.Properties.Out.eq(0)
                                )
                        )
                                .buildCount().count();
                        String did = chat.getJid();

                        int count = unread_count;
                        pushDialogs.put(did, count);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationsController.getInstance().processLoadedUnreadMessages(pushDialogs);
                    }
                });

                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });
    }

    public synchronized int getUnreadUpdatesCount() {
        DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                "ycache.sqlite", null);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        DaoMaster daoMaster = new DaoMaster(database);
        DaoSession daoSession = daoMaster.newSession();
        ChatsDao chatsDao = daoSession.getChatsDao();
        MessagesDao messagesDao = daoSession.getMessagesDao();
        int unreadCount = (int) (long) messagesDao.queryBuilder().where(
                messagesDao.queryBuilder().and(MessagesDao.Properties.Read_state.eq(0),
                        MessagesDao.Properties.Out.eq(0))
        ).buildCount().count();
        daoSession.clear();
        database.close();
        databaseHelper.close();

        return unreadCount;
    }

    public void getDialogs(final boolean update) {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                        "ycache.sqlite", null);
                SQLiteDatabase database = databaseHelper.getWritableDatabase();

                DaoMaster daoMaster = new DaoMaster(database);
                DaoSession daoSession = daoMaster.newSession();
                ChatsDao chatsDao = daoSession.getChatsDao();
                MessagesDao messagesDao = daoSession.getMessagesDao();
                List<Chats> chats = chatsDao.queryBuilder().list();
                ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<TLRPC.TL_dialog>();

                for (Chats chat : chats) {
                    try {
                        TLRPC.TL_dialog tl_dialog = new TLRPC.TL_dialog();
                        tl_dialog.jid = chat.getJid();
                        Contacts contact = MessagesStorage.getInstance().getContact(chat.getJid(), false);
                        tl_dialog.fname = contact.getFname();
                        tl_dialog.lname = contact.getSname();

                        // tl_dialog.topMessage= messagesDao.queryBuilder().where(MessagesDao.Properties.Jid.eq(chat.getJid())).limit(1).list().get(0);
                        // QueryBuilder qb= messagesDao.queryBuilder();

                        tl_dialog.unread_count = (int) (long) messagesDao.queryBuilder().where(
                                messagesDao.queryBuilder().and(MessagesDao.Properties.Jid.eq(chat.getJid()), MessagesDao.Properties.Read_state.eq(0),
                                        MessagesDao.Properties.Out.eq(0)
                                )
                        )
                                .buildCount().count();

                        tl_dialog.topMessage = messagesDao.queryBuilder().where(MessagesDao.Properties.Jid.eq(chat.getJid()))
                                .orderDesc(MessagesDao.Properties.Date, MessagesDao.Properties.Id).limit(1).build().list().get(0);

                        //FileLog.e("Test getDialogs", " tl_dialog.topMessage " +  tl_dialog.topMessage.getSend_state()+" unread_count:"+  tl_dialog.unread_count);
                        dialogs.add(tl_dialog);
                    } catch (Exception e) {
                    }
                }
                MessagesController.getInstance().processLoadedDialogs(dialogs, update);
                //FileLog.e("Test getDialogs", "size" + dialogs.size());
                daoSession.clear();
                database.close();
                databaseHelper.close();
            }
        });

        // FileLog.e("Test getDialogs", " tl_dialog.topMessage");

    }

    public void putWallpapers(final ArrayList<TLRPC.WallPaper> wallPapers) {
      /*  storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    database.executeFast("DELETE FROM wallpapers WHERE 1").stepThis().dispose();
                    database.beginTransaction();
                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO wallpapers VALUES(?, ?)");
                    int num = 0;
                    for (TLRPC.WallPaper wallPaper : wallPapers) {
                        state.requery();
                        SerializedData data = new SerializedData();
                        wallPaper.serializeToStream(data);
                        state.bindInteger(1, num);
                        state.bindByteArray(2, data.toByteArray());
                        state.step();
                        num++;
                    }
                    state.dispose();
                    database.commitTransaction();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });*/
    }

    public void getWallpapers() {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    DaoMaster.DevOpenHelper databaseHelper = new DaoMaster.DevOpenHelper(ApplicationLoader.applicationContext,
                            "ycache.sqlite", null);
                    SQLiteDatabase database = databaseHelper.getWritableDatabase();

                    DaoMaster daoMaster = new DaoMaster(database);
                    DaoSession daoSession = daoMaster.newSession();
                    WallpapersDao wallpapersDao = daoSession.getWallpapersDao();
                    List<Wallpapers> wallpapers = wallpapersDao.queryBuilder().list();
                    ArrayList<TLRPC.WallPaper> wallPapers = new ArrayList<TLRPC.WallPaper>();
                    for (Wallpapers wallpaper : wallpapers) {
                        //SQLiteCursor cursor = database.queryFinalized("SELECT data FROM wallpapers WHERE 1");
                        byte[] bytes = wallpaper.getData();
                        if (bytes != null) {
                            SerializedData data = new SerializedData(bytes);
                            TLRPC.WallPaper wallPaper = (TLRPC.WallPaper) TLClassStore.Instance().TLdeserialize(data, data.readInt32());
                            wallPapers.add(wallPaper);
                        }
                    }

                    NotificationCenter.getInstance().postNotificationName(wallpapersDidLoaded, wallPapers);
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
            }
        });
    }
}
