/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.yahala.ImageLoader.ImageLoaderInitializer;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.ui.Adapters.BaseFragmentAdapter;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.ui.Views.CircleImageView.PictureImplCircleImageView;
import com.yahala.xmpp.ContactsController;
import com.yahala.xmpp.MessagesController;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;

public class UserProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, MessagesActivity.MessagesActivityDelegate {
    private ListView listView;
    private ListAdapter listAdapter;
    private String user_id;
    private String selectedPhone;
    private int totalMediaCount = -1;
    private boolean creatingChat = false;
    private String dialog_id;


    private int avatarRow;
    private int phoneSectionRow;
    private int phoneRow;
    private int settingsSectionRow;
    private int settingsTimerRow;
    private int settingsKeyRow;
    private int settingsNotificationsRow;
    private int sharedMediaSectionRow;
    private int sharedMediaRow;
    private int rowCount = 0;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, MessagesController.updateInterfaces);
        /*NotificationCenter.getInstance().addObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.mediaCountDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.encryptedChatCreated);
        NotificationCenter.getInstance().addObserver(this, MessagesController.encryptedChatUpdated);*/
        user_id = getArguments().getString("user_id", "0");
        dialog_id = getArguments().getString("dialog_id", "0");

        updateRowsIds();
        return ContactsController.getInstance().friendsDict.get(user_id) != null && super.onFragmentCreate();
    }


    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, MessagesController.updateInterfaces);
      /*  NotificationCenter.getInstance().removeObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.mediaCountDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.encryptedChatCreated);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.encryptedChatUpdated);*/
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void updateRowsIds() {
        rowCount = 0;
        avatarRow = rowCount++;
        phoneSectionRow = rowCount++;
        phoneRow = rowCount++;
        settingsSectionRow = rowCount++;

        settingsTimerRow = -1;
        settingsKeyRow = -1;

        settingsNotificationsRow = rowCount++;
        sharedMediaSectionRow = rowCount++;
        sharedMediaRow = rowCount++;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (fragmentView == null) {


                   /* else if (id == block_contact) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                TLRPC.User user = MessagesController.getInstance().users.get(user_id);
                                if (user == null) {
                                    return;
                                }
                                TLRPC.TL_contacts_block req = new TLRPC.TL_contacts_block();
                                req.id = MessagesController.getInputUser(user);
                                TLRPC.TL_contactBlocked blocked = new TLRPC.TL_contactBlocked();
                                blocked.user_id = user_id;
                                blocked.date = (int)(System.currentTimeMillis() / 1000);
                                ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                                    @Override
                                    public void run(TLObject response, TLRPC.TL_error error) {

                                    }
                                }, null, true, RPCRequest.RPCRequestClassGeneric);
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showAlertDialog(builder);
                    } else if (id == add_contact) {
                        TLRPC.User user = MessagesController.getInstance().users.get(user_id);
                        Bundle args = new Bundle();
                        args.putInt("user_id", user.id);
                        presentFragment(new ContactAddActivity(args));
                    } else if (id == share_contact) {
                        Bundle args = new Bundle();
                        args.putBoolean("onlySelect", true);
                        args.putBoolean("serverOnly", true);
                        MessagesActivity fragment = new MessagesActivity(args);
                        fragment.setDelegate(UserProfileActivity.this);
                        presentFragment(fragment);
                    } else if (id == edit_contact) {
                        Bundle args = new Bundle();
                        args.putInt("user_id", user_id);
                        presentFragment(new ContactAddActivity(args));
                    } else if (id == delete_contact) {
                        final TLRPC.User user = MessagesController.getInstance().users.get(user_id);
                        if (user == null || getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ArrayList<TLRPC.User> arrayList = new ArrayList<TLRPC.User>();
                                arrayList.add(user);
                                ContactsController.getInstance().deleteContact(arrayList);
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showAlertDialog(builder);
                    }
                }
            });
*/


            fragmentView = inflater.inflate(R.layout.user_profile_layout, container, false);
            listAdapter = new ListAdapter(getParentActivity());

            TextView textView = (TextView) fragmentView.findViewById(R.id.start_secret_button_text);
            textView.setText(LocaleController.getString("StartEncryptedChat", R.string.StartEncryptedChat));

            View startSecretButton = fragmentView.findViewById(R.id.start_secret_button);


            startSecretButton.setVisibility(View.GONE);


            listView = (ListView) fragmentView.findViewById(R.id.listView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == sharedMediaRow) {
                        /*Bundle args = new Bundle();
                        if (dialog_id != 0) {
                            args.putLong("dialog_id", dialog_id);
                        } else {
                            args.putLong("dialog_id", user_id);
                        }
                        presentFragment(new MediaActivity(args));*/
                    } else if (i == settingsKeyRow) {
                        /*Bundle args = new Bundle();
                        args.putInt("chat_id", (int)(dialog_id >> 32));
                        presentFragment(new IdenticonActivity(args));*/
                    } else if (i == settingsNotificationsRow) {
                        Bundle args = new Bundle();
                        args.putString("dialog_id", user_id);

                        ProfileNotificationsActivity profileNotificationsActivity = new ProfileNotificationsActivity();
                        profileNotificationsActivity.setArguments(args);
                        ((LaunchActivity) getParentActivity()).presentFragment(profileNotificationsActivity, "profileNotificationsActivity_" + user_id, false);
                    }
                }
            });
            /*if (dialog_id != 0) {
                MessagesController.getInstance().getMediaCount(dialog_id, classGuid, true);
            } else {
                MessagesController.getInstance().getMediaCount(user_id, classGuid, true);
            }*/
        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String name = null;
            if (ringtone != null) {
                Ringtone rng = RingtoneManager.getRingtone(ApplicationLoader.applicationContext, ringtone);
                if (rng != null) {
                    if (ringtone.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) {
                        name = LocaleController.getString("Default", R.string.Default);
                    } else {
                        name = rng.getTitle(parentActivity);
                    }
                    rng.stop();
                }
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            if (requestCode == 0) {
                if (name != null && ringtone != null) {
                    editor.putString("sound_" + user_id, name);
                    editor.putString("sound_path_" + user_id, ringtone.toString());
                } else {
                    editor.putString("sound_" + user_id, "NoSound");
                    editor.putString("sound_path_" + user_id, "NoSound");
                }
            }
            editor.commit();
            listView.invalidateViews();
        }
    }

    public void didReceivedNotification(int id, Object... args) {
        if (id == MessagesController.updateInterfaces) {
           /* int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                if (listView != null) {
                    listView.invalidateViews();
                }
            }*/
        /*} else if (id == MessagesController.contactsDidLoaded) {
            if (parentActivity != null) {
                parentActivity.supportInvalidateOptionsMenu();
            }
        } else if (id == MessagesController.mediaCountDidLoaded) {
            String uid = (String)args[0];
            if (user_id == uid && dialog_id == "0" || dialog_id != "0" && dialog_id == uid) {
                totalMediaCount = (Integer)args[1];
                if (listView != null) {
                    listView.invalidateViews();
                }
            }
        } *else if (id == MessagesController.encryptedChatCreated) {
            if (creatingChat) {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat)args[0];
                ChatAct fragment = new ChatAct();
                Bundle bundle = new Bundle();
                bundle.putInt("enc_id", encryptedChat.id);
                fragment.setArguments(bundle);
                ((LaunchActivity)parentActivity).presentFragment(fragment, "chat" + Math.random(), true, false);
            }
        } else if (id == MessagesController.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat)args[0];
            if (currentEncryptedChat != null && chat.id == currentEncryptedChat.id) {
                currentEncryptedChat = chat;
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }*/
        }
    }

    @Override
    public void applySelfActionBar() {
        if (parentActivity == null) {
            return;
        }
        ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setSubtitle(null);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setCustomView(null);
        if (dialog_id != "0") {
            actionBar.setTitle(LocaleController.getString("SecretTitle", R.string.SecretTitle));
        } else {
            actionBar.setTitle(LocaleController.getString("ContactInfo", R.string.ContactInfo));
        }

        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (title != null) {
            if (dialog_id != "0") {
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_white, 0, 0, 0);
                title.setCompoundDrawablePadding(OSUtilities.dp(4));
            } else {
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                title.setCompoundDrawablePadding(0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinish) {
            return;
        }
        if (getActivity() == null) {
            return;
        }
        if (!firstStart && listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        firstStart = false;
        ((LaunchActivity) parentActivity).showActionBar();
        ((LaunchActivity) parentActivity).updateActionBar();
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    private void fixLayout() {
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (dialog_id != "0") {
                        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
                        if (title == null) {
                            final int subtitleId = ApplicationLoader.applicationContext.getResources().getIdentifier("action_bar_title", "id", "android");
                            title = (TextView) parentActivity.findViewById(subtitleId);
                        }
                        if (title != null) {
                            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_white, 0, 0, 0);
                            title.setCompoundDrawablePadding(OSUtilities.dp(4));
                        }
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finishFragment();
                break;
            case R.id.block_contact: {
                TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                if (user == null) {
                    break;
                }
                TLRPC.TL_contacts_block req = new TLRPC.TL_contacts_block();
                // req.id = MessagesController.getInputUser(user);
                TLRPC.TL_contactBlocked blocked = new TLRPC.TL_contactBlocked();
                blocked.user_id = user_id;
                blocked.date = (int) (System.currentTimeMillis() / 1000);
                /*ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                    @Override
                    public void run(TLObject response, TLRPC.TL_error error) {

                    }
                }, null, true, RPCRequest.RPCRequestClassGeneric);*/
                break;
            }
            case R.id.add_contact: {
                TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                ContactAddActivity fragment = new ContactAddActivity();
                Bundle args = new Bundle();
                args.putInt("user_id", user.id);
                fragment.setArguments(args);
                ((LaunchActivity) parentActivity).presentFragment(fragment, "add_contact_" + user.id, false);
                break;
            }
            case R.id.share_contact: {
                MessagesActivity fragment = new MessagesActivity();
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putBoolean("serverOnly", true);
                fragment.setArguments(args);
                fragment.delegate = this;
                ((LaunchActivity) parentActivity).presentFragment(fragment, "chat_select", false);
                break;
            }
            case R.id.edit_contact: {
                ContactAddActivity fragment = new ContactAddActivity();
                Bundle args = new Bundle();
                args.putString("user_id", user_id);
                fragment.setArguments(args);
                ((LaunchActivity) parentActivity).presentFragment(fragment, "add_contact_" + user_id, false);
                break;
            }
            case R.id.delete_contact: {
                final TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                if (user == null) {
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ArrayList<TLRPC.User> arrayList = new ArrayList<TLRPC.User>();
                        arrayList.add(user);
                        //ContactsController.getInstance().deleteContact(arrayList);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show().setCanceledOnTouchOutside(true);
                break;
            }
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
       /* if (ContactsController.getInstance().contactsDict.get(user_id) == null) {
            TLRPC.User user = MessagesController.getInstance().users.get(user_id);
            if (user == null) {
                return;
            }
            if (user.phone != null && user.phone.length() != 0) {
                inflater.inflate(R.menu.user_profile_menu, menu);
            } else {
                inflater.inflate(R.menu.user_profile_block_menu, menu);
            }
        } else {
            inflater.inflate(R.menu.user_profile_contact_menu, menu);
        }*/
    }

    @Override
    public void didSelectDialog(MessagesActivity messageFragment, long dialog_id) {
        if (dialog_id != 0) {
           /* ChatAct fragment = new ChatAct();
            Bundle bundle = new Bundle();
            int lower_part = (int)dialog_id;
            if (lower_part != 0) {
                if (lower_part > 0) {
                    NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                    bundle.putInt("user_id", lower_part);
                    fragment.setArguments(bundle);
                    fragment.scrollToTopOnResume = true;
                    ((LaunchActivity)parentActivity).presentFragment(fragment, "chat" + Math.random(), true, false);
                    removeSelfFromStack();
                    messageFragment.removeSelfFromStack();
                } else if (lower_part < 0) {
                    NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                    bundle.putInt("chat_id", -lower_part);
                    fragment.setArguments(bundle);
                    fragment.scrollToTopOnResume = true;
                    ((LaunchActivity)parentActivity).presentFragment(fragment, "chat" + Math.random(), true, false);
                    messageFragment.removeSelfFromStack();
                    removeSelfFromStack();
                }
            } else {
                NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                int id = (int)(dialog_id >> 32);
                bundle.putInt("enc_id", id);
                fragment.setArguments(bundle);
                fragment.scrollToTopOnResume = true;
                ((LaunchActivity)parentActivity).presentFragment(fragment, "chat" + Math.random(), false);
                messageFragment.removeSelfFromStack();
                removeSelfFromStack();
            }
            TLRPC.User user = MessagesController.getInstance().users.get(user_id);
            MessagesController.getInstance().sendMessage(user, dialog_id);*/
        }
    }


    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == phoneRow || i == settingsTimerRow || i == settingsKeyRow || i == settingsNotificationsRow || i == sharedMediaRow;
        }

        @Override
        public int getCount() {
            return rowCount;
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
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                PictureImplCircleImageView avatarImage;
                TextView onlineText;
                TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.user_profile_avatar_layout, viewGroup, false);

                    onlineText = (TextView) view.findViewById(R.id.settings_online);
                    avatarImage = (PictureImplCircleImageView) view.findViewById(R.id.settings_avatar_image);
                    //avatarImage.processDetach = false;
                    avatarImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                            if (user.photo != null && user.photo.photo_big != null) {
                                // NotificationCenter.getInstance().addToMemCache(56, user_id);
                                //  NotificationCenter.getInstance().addToMemCache(53, user.photo.photo_big);
                                //Intent intent = new Intent(parentActivity, GalleryImageViewer.class);
                                //  startActivity(intent);
                            }
                        }
                    });
                } else {
                    avatarImage = (PictureImplCircleImageView) view.findViewById(R.id.settings_avatar_image);
                    onlineText = (TextView) view.findViewById(R.id.settings_online);
                }
                TextView textView = (TextView) view.findViewById(R.id.settings_name);

                Typeface typeface = OSUtilities.getTypeface("fonts/Roboto-Regular.ttf");
                textView.setTypeface(typeface);

                textView.setText(Utilities.formatName(user.first_name, user.last_name));
                onlineText.setText(LocaleController.formatUserStatus(user));

                TLRPC.FileLocation photo = null;
                TLRPC.FileLocation photoBig = null;
                ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue, 250, 250);
                ImageTag tag = ImageLoaderInitializer.getInstance().imageTagFactory.build(user.avatarUrl, ApplicationLoader.applicationContext);
                avatarImage.setTag(tag);
                ImageLoaderInitializer.getInstance().getImageLoader().getLoader().load(avatarImage);
                //com.yahala.ui.lazylist.ImageLoader.getInstance().DisplayImage( user.avatarUrl,avatarImage);
                return view;

            } else if (type == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_section_layout, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(R.id.settings_section_text);
                if (i == phoneSectionRow) {
                    textView.setText(LocaleController.getString("PHONE", R.string.PHONE));
                } else if (i == settingsSectionRow) {
                    textView.setText(LocaleController.getString("SETTINGS", R.string.SETTINGS));
                } else if (i == sharedMediaSectionRow) {
                    view.setVisibility(View.INVISIBLE);
                    textView.setText(LocaleController.getString("SHAREDMEDIA", R.string.SHAREDMEDIA));
                }
            } else if (type == 2) {
                final TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.user_profile_phone_layout, viewGroup, false);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (user.phone == null || user.phone.length() == 0 || getParentActivity() == null) {
                                return;
                            }
                            selectedPhone = user.phone;

                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                            builder.setItems(new CharSequence[]{LocaleController.getString("Copy", R.string.Copy), LocaleController.getString("Call", R.string.Call)}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 1) {
                                        try {
                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + selectedPhone));
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            getParentActivity().startActivity(intent);
                                        } catch (Exception e) {
                                            FileLog.e("tmessages", e);
                                        }
                                    } else if (i == 0) {
                                        int sdk = android.os.Build.VERSION.SDK_INT;
                                        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                                            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboard.setText(selectedPhone);
                                        } else {
                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                            android.content.ClipData clip = android.content.ClipData.newPlainText("label", selectedPhone);
                                            clipboard.setPrimaryClip(clip);
                                        }
                                    }
                                }
                            });
                            showAlertDialog(builder);
                        }
                    });
                }
                ImageButton button = (ImageButton) view.findViewById(R.id.settings_edit_name);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TLRPC.User user = ContactsController.getInstance().friendsDict.get(user_id);
                        if (user == null || user instanceof TLRPC.TL_userEmpty) {
                            return;
                        }
                        NotificationCenter.getInstance().postNotificationName(MessagesController.closeChats);
                        Bundle args = new Bundle();
                        args.putString("user_jid", user_id);
                        ChatActivity chatAct = new ChatActivity();
                        chatAct.setArguments(args);
                        ((LaunchActivity) getParentActivity()).presentFragment(chatAct, "chat_0a", false);
                    }
                });
                FrameLayout container = (FrameLayout) view.findViewById(R.id.container);
                TextView textView = (TextView) view.findViewById(R.id.settings_row_text);
                TextView detailTextView = (TextView) view.findViewById(R.id.settings_row_text_detail);
                View divider = view.findViewById(R.id.settings_row_divider);
                if (i == phoneRow) {
                    if (user.phone != null && user.phone.length() != 0) {
                        textView.setText(PhoneFormat.getInstance().format("+" + user.phone));
                    } else {
                        textView.setText("Unknown");
                    }
                    divider.setVisibility(View.INVISIBLE);
                    detailTextView.setText(LocaleController.getString("PhoneMobile", R.string.PhoneMobile));
                    container.setBackground(getResources().getDrawable(R.drawable.listitem_bg));
                }
            } else if (type == 3) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.user_profile_leftright_row_layout, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(R.id.settings_row_text);
                TextView detailTextView = (TextView) view.findViewById(R.id.settings_row_text_detail);

                View divider = view.findViewById(R.id.settings_row_divider);
                if (i == sharedMediaRow) {

                    textView.setText(LocaleController.getString("SharedMedia", R.string.SharedMedia));
                    if (totalMediaCount == -1) {
                        detailTextView.setText(LocaleController.getString("Loading", R.string.Loading));
                    } else {
                        detailTextView.setText(String.format("%d", totalMediaCount));
                    }
                    divider.setVisibility(View.INVISIBLE);
                    view.setVisibility(View.INVISIBLE);
                } else if (i == settingsTimerRow) {

                }
            } else if (type == 4) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.user_profile_identicon_layout, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(R.id.settings_row_text);
                View divider = view.findViewById(R.id.settings_row_divider);
                divider.setVisibility(View.VISIBLE);

            } else if (type == 5) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_row_button_layout, viewGroup, false);
                }
                LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
                TextView textView = (TextView) view.findViewById(R.id.settings_row_text);
                View divider = view.findViewById(R.id.settings_row_divider);
                if (i == settingsNotificationsRow) {
                    textView.setText(LocaleController.getString("NotificationsAndSounds", R.string.NotificationsAndSounds));
                    divider.setVisibility(View.INVISIBLE);
                    container.setBackground(getResources().getDrawable(R.drawable.listitem_bg));
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == avatarRow) {
                return 0;
            } else if (i == phoneSectionRow || i == settingsSectionRow || i == sharedMediaSectionRow) {
                return 1;
            } else if (i == phoneRow) {
                return 2;
            } else if (i == sharedMediaRow || i == settingsTimerRow) {
                return 3;
            } else if (i == settingsKeyRow) {
                return 4;
            } else if (i == settingsNotificationsRow) {
                return 5;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 6;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}