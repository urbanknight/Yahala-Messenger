package com.yahala.xmpp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;
import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.SQLite.Contacts;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.ApplicationLoader;


import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppStringUtils;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by user on 4/10/2014.
 */
public class ContactsController {
    private static volatile ContactsController Instance = null;
    public boolean contactsLoaded = false;
    public boolean first = false;
    public ConcurrentHashMap<String, Contact> ServerContacts = new ConcurrentHashMap<String, Contact>();
    public ArrayList<Contact> contacts = new ArrayList<Contact>();
    public ArrayList<TLRPC.User> friends = new ArrayList<TLRPC.User>();
    public ConcurrentHashMap<String, TLRPC.User> friendsDict = new ConcurrentHashMap<String, TLRPC.User>(100, 1.0f, 2);
    public ConcurrentHashMap<Integer, Contact> contactsMap = new ConcurrentHashMap<Integer, Contact>();
    public ConcurrentHashMap<String, ArrayList<TLRPC.User>> usersSectionsDict = new ConcurrentHashMap<String, ArrayList<TLRPC.User>>();
    public ArrayList<String> sortedUsersSectionsArray = new ArrayList<String>();
    private String[] projectionPhones = {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
    };
    private String[] projectionNames = {
            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME
    };

    public static ContactsController getInstance() {
        ContactsController localInstance = Instance;
        if (localInstance == null) {
            synchronized (ContactsController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ContactsController();
                }
            }
        }
        return localInstance;
    }

    public void readContacts(final Boolean first) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {


                // FileLog.e("Test readContacts","isConnected() ");
                ServerContacts.clear();
                if (first)
                    performSyncPhoneBook(true);
                else {
//                if(XmppManager.getInstance().getConnection().isConnected()) {
                    // performSyncPhoneBook(false);
                    //     }

                    MessagesStorage.getInstance().getContacts();


                }


            }
        });
    }

    public void processLoadedContacts(final List<Contacts> cs) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    friends.clear();
                    friendsDict.clear();

                    for (Contacts contact : cs) {
                        // if (!ServerContacts.containsKey(contact.getJid())) {
                        Contact c = new Contact();
                        c.first_name = contact.getFname();
                        c.last_name = contact.getSname();
                        c.jid = contact.getJid();
                        c.avatar = contact.getAvatar();
                        c.phones.add(XmppStringUtils.parseLocalpart(c.jid));
                        FileLog.e(" c.phones", "p :" + c.phones.get(0));
                        //ServerContacts.put(c.jid , c);
                        // contacts.add(c);


                        TLRPC.User user = new TLRPC.User();
                        //FileLog.e("Test phones",""+c.jid);

                        if (c.avatar == null || c.avatar.isEmpty()) {
                            int num = 7;
                            Random rand = new Random();
                            int ran = rand.nextInt(num);
                            String imgUri = "drawable://" + Utilities.arrUsersAvatars[ran];
                            user.avatar = ImageLoader.getInstance().loadImageSync(imgUri);

                        } else {
                            int num = 7;
                            Random rand = new Random();
                            int ran = rand.nextInt(num);
                            DisplayImageOptions options = new DisplayImageOptions.Builder()
                                    .cacheInMemory(false)
                                    .cacheOnDisk(true)
                                    .considerExifParams(true)
                                    .showImageForEmptyUri(Utilities.arrUsersAvatars[ran])
                                    .showImageOnFail(Utilities.arrUsersAvatars[ran])
                                    .showImageOnLoading(Utilities.arrUsersAvatars[ran])
                                    .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                                    .preProcessor(new BitmapProcessor() {
                                        public Bitmap process(Bitmap src) {
                                            return OSUtilities.scaleCenterCrop(src, OSUtilities.dp(100), OSUtilities.dp(100));
                                        }
                                    })
                                    .build();
                            try {
                                user.avatar = ImageLoader.getInstance().loadImageSync("file:///" + c.avatar, options);
                            } catch (Exception ex) {
                                ex.printStackTrace();

                            }

                        }

                        //FileLog.e(" c.jid","jid: "+ c.jid);
                        user.avatarUrl = c.avatar;
                        user.jid = c.jid;
                        user.last_seen = c.last_seen;
                        user.first_name = c.first_name;
                        user.last_name = c.last_name;
                        user.presence = c.presence;
                        user.phone = c.phones.get(0);
                        friends.add(user);
                        friendsDict.put(user.jid, user);
                        //FileLog.e("Test", " jid :" + c.jid + "presence:" + c.presence.getType());
                        // }
                    }
                    // FileLog.e("size","friends size:"+friends.size());
                    Collections.sort(friends, new Comparator<TLRPC.User>() {
                        @Override
                        public int compare(TLRPC.User tl_contact, TLRPC.User tl_contact2) {
                            try {
                                TLRPC.User user1 = friendsDict.get(tl_contact.jid);
                                TLRPC.User user2 = friendsDict.get(tl_contact2.jid);
                                String name1 = user1.first_name;
                                if (name1 == null || name1.length() == 0) {
                                    name1 = user1.last_name;
                                }
                                String name2 = user2.first_name;
                                if (name2 == null || name2.length() == 0) {
                                    name2 = user2.last_name;
                                }
                                return name1.compareTo(name2);
                            } catch (Exception e) {
                                return 0;
                            }
                        }
                    });


                    final SparseArray<TLRPC.TL_contact> contactsDictionary = new SparseArray<TLRPC.TL_contact>();
                    final ConcurrentHashMap<String, ArrayList<TLRPC.User>> sectionsDict = new ConcurrentHashMap<String, ArrayList<TLRPC.User>>();
                    final ArrayList<String> sortedSectionsArray = new ArrayList<String>();
                    ConcurrentHashMap<String, TLRPC.TL_contact> contactsByPhonesDict = null;


                    final ConcurrentHashMap<String, TLRPC.TL_contact> contactsByPhonesDictFinal = contactsByPhonesDict;

                    for (ListIterator<TLRPC.User> itr = friends.listIterator(); itr.hasNext(); ) {

                        final TLRPC.User user = itr.next();
                        //  TLRPC.User user = usersDict.get(value.user_id);
                        try {
                            if (user == null) {
                                continue;
                            }


                            String key = user.first_name;
                            if (key == null || key.length() == 0) {
                                key = user.last_name;
                            }
                            if (key.length() == 0) {
                                key = "#";
                            } else {
                                key = key.toUpperCase();
                            }
                            if (key.length() > 1) {
                                key = key.substring(0, 1);
                            }
                            ArrayList<TLRPC.User> arr = sectionsDict.get(key);
                            if (arr == null) {
                                arr = new ArrayList<TLRPC.User>();
                                sectionsDict.put(key, arr);
                                sortedSectionsArray.add(key);
                            }
                            arr.add(user);
                        } catch (Exception e) {
                        }
                    }

                    Collections.sort(sortedSectionsArray, new Comparator<String>() {
                        @Override
                        public int compare(String s, String s2) {
                            char cv1 = s.charAt(0);
                            char cv2 = s2.charAt(0);
                            if (cv1 == '#') {
                                return 1;
                            } else if (cv2 == '#') {
                                return -1;
                            }
                            return s.compareTo(s2);
                        }
                    });


                    usersSectionsDict = sectionsDict;
                    sortedUsersSectionsArray = sortedSectionsArray;

                    // NotificationCenter.getInstance().postNotificationName(MessagesController.contactsDidLoaded);


                    TLRPC.User section = new TLRPC.User();
                    section.first_name = "2";
                    section.last_name = LocaleController.getString("Favorite", R.string.Favorite);//"Favorites";
                    section.jid = "1";
                    section.phone = friends.size() + "";

                    TLRPC.User invite = new TLRPC.User();
                    invite.first_name = "2";
                    invite.jid = "2";
                    invite.last_name = LocaleController.getString("InviteFriends", R.string.InviteFriends);

               /* TLRPC.User group = new TLRPC.User();
                group.first_name = "3";
                group.jid = "3";
                group.last_name = LocaleController.getString("NewGroup", R.string.NewGroup);
*/
                    friends.add(0, invite);
                    //  friends.add(1, group);
                    friends.add(1, section);
                    friendsDict.put(invite.jid, invite);
                    //    friendsDict.put(group.jid, group);
                    friendsDict.put(section.jid, section);
                    contactsLoaded = true;
                    //Utilities.RunOnUIThread(new Runnable() {
                    //   @Override
                    //   public void run() {
                    // updateFriendsPresence();
                    NotificationCenter.getInstance().postNotificationName(MessagesController.contactsDidLoaded);
                    //   }
                    // });

                } catch (Exception e) {
                    FileLog.e("yahala", "processLoadedContacts " + friends.size());
                }
            }
        });


    }

    public void updateFriendsPresence() {
        FileLog.e("updateFriendsPresence", "updateFriendsPresence " + friends.size());

        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (ListIterator<TLRPC.User> itr = friends.listIterator(); itr.hasNext(); ) {

                    final TLRPC.User user = itr.next();
                    //  for (final TLRPC.User user : friends) {
                    try {
                        if (XMPPManager.getInstance().isConnected()) {
                            Roster roster = Roster.getInstanceFor(XMPPManager.getInstance().getConnection());
                            final Presence bestPresence = roster.getPresence(JidCreate.bareFrom(user.jid));
                            user.presence = bestPresence;
                            //FileLog.e("Test getLastSeenMessage", " jid :" + user.jid + " " + user.last_seen);
                            // if(c.presence.getType()== Presence.Type.available)
                            //  {
                            user.last_seen = XMPPManager.getInstance().getLastSeenMessage(user.jid);
                        } else {
                            user.last_seen = XMPPManager.getInstance().getLastSeenMessage(user.jid);
                            user.presence = null;
                        }
                        FileLog.e("user.last_seen", user.last_seen);
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(XMPPManager.presenceDidChanged, user.jid, user.presence, user.last_seen);
                            }
                        });
                    } catch (Exception e) {
                        FileLog.e("yahala updateFriendsPresence", e.toString());

                    }

                }

            }

        });

    }

    public void performSyncPhoneBook(final boolean writeToCache) {

        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                ServerContacts.clear();
                first = true;
                contactsLoaded = false;
                contactsMap = readContactsFromPhoneBook();

                String country = "";
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        country = telephonyManager.getSimCountryIso().toUpperCase();
                    } else {
                        country = "JO";
                    }
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
                ArrayList<TLRPC.User> roasterUsers = XMPPManager.getInstance().getFriends(false);
                for (Map.Entry entry : contactsMap.entrySet()) {

                    Contact c = (Contact) entry.getValue();
                    String phone = "";
                    int cc = -1;
                    try {
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        Phonenumber.PhoneNumber NumberProto = phoneUtil.parse(c.phones.get(0), country);
                        if (String.valueOf(NumberProto.getNationalNumber()).length() < 9) {
                            continue;
                        }
                        phone = phoneUtil.format(NumberProto, PhoneNumberUtil.PhoneNumberFormat.E164);


                    } catch (NumberParseException e) {
                        System.err.println("NumberParseException was thrown: " + e.toString());
                    }
                    phone = phone.replace("+", "");
                    c.jid = phone;
                    try {
                        if (XMPPManager.getInstance().searchUsers(phone).size() > 0) {
                            if (UserConfig.currentUser.phone.equals(phone))
                                continue;
                            c.jid = c.jid + XMPPManager.DOMAIN;
                            if (!ServerContacts.containsKey(c.jid)) {
                                ServerContacts.put(c.jid, c);
                                if (writeToCache) {
                                    FileLog.e("contacts add friend:", "add user");
                                    XMPPManager.getInstance().addUser(c.jid, c.first_name + " " + c.last_name);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        FileLog.e("performSyncPhoneBook:", ex.toString());

                    }
                    //FileLog.e("contacts:",x +" - locale:"+country+" name:"+c.first_name +" "+c.last_name+" sPhone:"+phone);
                }

                for (TLRPC.User user : roasterUsers) {
                    if (ServerContacts.containsKey(user.jid))
                        continue;
                    FileLog.e("contacts writeToCache:", "getFriends " + user.jid);
                    Contact contact = new Contact();
                    contact.jid = user.jid;
                    contact.phones.add(user.phone);
                    contact.last_name = user.last_name;
                    contact.first_name = user.first_name;
                    XMPPManager.getInstance().getUserVCard(contact.jid);

                    ServerContacts.put(user.jid, contact);

                }

                if (writeToCache) {
                    FileLog.e("contacts writeToCache:", "writeToCache");
                    MessagesStorage.getInstance().putContactsToCache();

                }
                FileLog.e("contacts:", friends.size() + "");
                /*cc=  XmppManager.getInstance().searchUsers(String.valueOf(NumberProto.getCountryCode()) +
                        String.valueOf(NumberProto.getNationalNumber())).size();*/
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ApplicationLoader.applicationContext, "Contacts  is synced", Toast.LENGTH_LONG).show();
                        NotificationCenter.getInstance().postNotificationName(MessagesController.contactsDidLoaded);
                        NotificationCenter.getInstance().postNotificationName(MessagesController.updateInterfaces);
                        contactsLoaded = true;
                        first = true;
                    }
                });


            }
        });
    }

    private ConcurrentHashMap<Integer, Contact> readContactsFromPhoneBook() {
        ConcurrentHashMap<Integer, Contact> contactsMap = new ConcurrentHashMap<Integer, Contact>();
        try {
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();

            HashMap<String, Contact> shortContacts = new HashMap<String, Contact>();
            String ids = "";
            Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionPhones, null, null, null);
            if (pCur != null) {
                if (pCur.getCount() > 0) {
                    while (pCur.moveToNext()) {
                        String number = pCur.getString(1);
                        if (number == null || number.length() == 0) {
                            continue;
                        }
                        number = PhoneFormat.stripExceptNumbers(number, true);
                        if (number.length() == 0) {
                            continue;
                        }

                        String shortNumber = number;

                        if (number.startsWith("+")) {
                            shortNumber = number.substring(1);
                        }

                        if (shortContacts.containsKey(shortNumber)) {
                            continue;
                        }

                        Integer id = pCur.getInt(0);
                        if (ids.length() != 0) {
                            ids += ",";
                        }
                        ids += id;

                        int type = pCur.getInt(2);
                        Contact contact = contactsMap.get(id);
                        if (contact == null) {
                            contact = new Contact();
                            contact.first_name = "";
                            contact.last_name = "";
                            contact.id = id;
                            contactsMap.put(id, contact);
                        }

                        contact.shortPhones.add(shortNumber);
                        contact.phones.add(number);
                        contact.phoneDeleted.add(0);

                        if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                            contact.phoneTypes.add(pCur.getString(3));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneHome", R.string.PhoneHome));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneMobile", R.string.PhoneMobile));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneWork", R.string.PhoneWork));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneMain", R.string.PhoneMain));
                        } else {
                            contact.phoneTypes.add(LocaleController.getString("PhoneOther", R.string.PhoneOther));
                        }
                        shortContacts.put(shortNumber, contact);
                    }
                }
                pCur.close();
            }

            pCur = cr.query(ContactsContract.Data.CONTENT_URI, projectionNames, ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " IN (" + ids + ") AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'", null, null);
            if (pCur != null && pCur.getCount() > 0) {
                while (pCur.moveToNext()) {
                    int id = pCur.getInt(0);
                    String fname = pCur.getString(1);
                    String sname = pCur.getString(2);
                    String sname2 = pCur.getString(3);
                    String mname = pCur.getString(4);
                    Contact contact = contactsMap.get(id);
                    if (contact != null) {
                        contact.first_name = fname;
                        contact.last_name = sname;
                        if (contact.first_name == null) {
                            contact.first_name = "";
                        }
                        if (mname != null && mname.length() != 0) {
                            if (contact.first_name.length() != 0) {
                                contact.first_name += " " + mname;
                            } else {
                                contact.first_name = mname;
                            }
                        }
                        if (contact.last_name == null) {
                            contact.last_name = "";
                        }
                        if (contact.last_name.length() == 0 && contact.first_name.length() == 0 && sname2 != null && sname2.length() != 0) {
                            contact.first_name = sname2;
                        }
                    }
                }
                pCur.close();
            }

            try {
                pCur = cr.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"display_name", ContactsContract.RawContacts.SYNC1, ContactsContract.RawContacts.CONTACT_ID}, ContactsContract.RawContacts.ACCOUNT_TYPE + " = " + "'com.whatsapp'", null, null);
                if (pCur != null) {
                    while ((pCur.moveToNext())) {
                        String phone = pCur.getString(1);
                        if (phone == null || phone.length() == 0) {
                            continue;
                        }
                        boolean withPlus = phone.startsWith("+");
                        phone = Utilities.parseIntToString(phone);
                        if (phone == null || phone.length() == 0) {
                            continue;
                        }
                        String shortPhone = phone;
                        if (!withPlus) {
                            phone = "+" + phone;
                        }

                        if (shortContacts.containsKey(shortPhone)) {
                            continue;
                        }

                        String name = pCur.getString(0);
                        if (name == null || name.length() == 0) {
                            name = PhoneFormat.getInstance().format(phone);
                        }

                        String[] args = name.split(" ", 2);

                        Contact contact = new Contact();
                        if (args.length > 0) {
                            contact.first_name = args[0];
                        } else {
                            contact.first_name = "";
                        }
                        if (args.length > 1) {
                            contact.last_name = args[1];
                        } else {
                            contact.last_name = "";
                        }
                        contact.id = pCur.getInt(2);
                        contactsMap.put(contact.id, contact);

                        contact.phoneDeleted.add(0);
                        contact.shortPhones.add(shortPhone);
                        contact.phones.add(phone);
                        contact.phoneTypes.add(LocaleController.getString("PhoneMobile", R.string.PhoneMobile));
                        shortContacts.put(shortPhone, contact);
                    }
                    pCur.close();
                }
            } catch (Exception e) {
                FileLog.e("yahala (pCur != null) {", e);
            }
        } catch (Exception e) {
            FileLog.e("yahala readContactsFromPhoneBook", e);
            contactsMap.clear();
        }
        return contactsMap;
    }

    public static class Contact {
        public int id;
        public String jid;
        public Presence presence;
        public String last_seen;
        public String avatar;
        public ArrayList<String> phones = new ArrayList<String>();
        public ArrayList<String> phoneTypes = new ArrayList<String>();
        public ArrayList<String> shortPhones = new ArrayList<String>();
        public ArrayList<Integer> phoneDeleted = new ArrayList<Integer>();
        public String first_name;
        public String last_name;
    }
}


/*

Let's say you have a string representing a phone number from Switzerland. This is how you parse/normalize it into a PhoneNumber object:

        String swissNumberStr = "044 668 18 00"
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
        PhoneNumber swissNumberProto = phoneUtil.parse(swissNumberStr, "CH");
        } catch (NumberParseException e) {
        System.err.println("NumberParseException was thrown: " + e.toString());
        }

        At this point, swissNumberProto contains:

        {
        country_code: 41
        national_number: 446681800
        }

        PhoneNumber is a class that is auto-generated from the phonenumber.proto with necessary modifications for efficiency. For details on the meaning of each field, refer to https://code.google.com/p/libphonenumber/source/browse/trunk/resources/phonenumber.proto

        Now let us validate whether the number is valid:

        boolean isValid = phoneUtil.isValidNumber(swissNumberProto); // returns true

        There are a few formats supported by the formatting method, as illustrated below:

// Produces "+41 44 668 18 00"
        System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.INTERNATIONAL));
// Produces "044 668 18 00"
        System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.NATIONAL));
// Produces "+41446681800"
        System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.E164));

        You could also choose to format the number in the way it is dialed from another country:

// Produces "011 41 44 668 1800", the number when it is dialed in the United States.
        System.out.println(phoneUtil.formatOutOfCountryCallingNumber(swissNumberProto, "US"));

        Formatting Phone Numbers 'as you type'

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("US");
        System.out.println(formatter.inputDigit('6'));  // Outputs "6"
        ...  // Input more digits
        System.out.println(formatter.inputDigit('3'));  // Now outputs "650 253"

        Geocoding Phone Numbers offline

        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
// Outputs "Zurich"
        System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ENGLISH));
// Outputs "Zürich"
        System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.GERMAN));
// Outputs "Zurigo"
        System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ITALIAN));

        Mapping Phone Numbers to carrier

        PhoneNumber swissMobileNumber =
        new PhoneNumber().setCountryCode(41).setNationalNumber(798765432L);
        PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();
// Outputs "Swisscom"
        System.out.println(carrierMapper.getNameForNumber(swissMobileNumber, Locale.ENGLISH));

        More examples on how to use the library can be found in the unittests at http://code.google.com/p/libphonenumber/source/browse/#svn/trunk/java/libphonenumber/test/com/google/i18n/phonenumbers
        */
