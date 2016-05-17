/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.R;
import com.yahala.messenger.ContactsController;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.objects.MessageObject;
import com.yahala.ui.Adapters.ContactsActivitySearchAdapter;
import com.yahala.ui.Adapters.ContactsAdapter;
import com.yahala.ui.Rows.ChatOrUserCell;
import com.yahala.ui.Views.BaseFragment;

import com.yahala.xmpp.XMPPManager;

import org.jivesoftware.smack.packet.Presence;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

//import com.yahala.messenger.ConnectionsManager;
//import com.yahala.messenger.RPCRequest;

public class ContactsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    public int selectAlertString = 0;
    public String selectAlertStringDesc = null;
    public ContactsActivityDelegate delegate;
    // private SectionedBaseAdapter listViewAdapter;
    private ContactsAdapter listViewAdapter;
    //private PinnedHeaderListView listView;
    private ListView listView;
    private ContactsActivitySearchAdapter searchListViewAdapter;
    private boolean searchWas;
    private boolean searching;
    private boolean onlyUsers;
    private boolean usersAsSections;
    private boolean contactsLoaded = false;
    private boolean destroyAfterSelect;
    private boolean returnAsResult;
    private boolean createSecretChat;
    private boolean creatingChat = false;
    private SearchView searchView;
    private TextView emptyTextView;
    //private HashMap<Integer, TLRPC.User> ignoreUsers;
    //private ArrayList<TLRPC.User> ignoreUsers;
    private SupportMenuItem searchItem;
    private FrameLayout progressLayout;
    private String inviteText;
    private boolean updatingInviteText = false;

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, MessagesController.encryptedChatCreated);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.userAuthenticated);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.presenceDidChanged);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.presenceRequestSent);
        super.onFragmentCreate();

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.encryptedChatCreated);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.userAuthenticated);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.presenceDidChanged);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.presenceRequestSent);
        delegate = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (fragmentView == null) {
            fragmentView = inflater.inflate(R.layout.contacts_layout, container, false);

            emptyTextView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);
            emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
            // searchListViewAdapter = new ContactsActivitySearchAdapter(parentActivity, ignoreUsers);
            // FileLog.e("Test", "Contact activity onCreateView");
            listView = (ListView) fragmentView.findViewById(R.id.listView);
            progressLayout = (FrameLayout) fragmentView.findViewById(R.id.progressLayout);

            listView.setVerticalScrollBarEnabled(true);
            progressLayout.setVisibility(View.GONE);

            listViewAdapter = new ContactsAdapter(parentActivity, R.id.listView, null);
            listView.setAdapter(listViewAdapter);
            /* else if(com.yahala.xmpp.ContactsController.getInstance().first){
                listViewAdapter.notifyDataSetChanged();
            }*/
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                   /* if (scrollState != 0)
                        listViewAdapter.isScrolling = true;
                    else {
                        listViewAdapter.isScrolling = false;
                        listViewAdapter.notifyDataSetChanged();
                    }*/
                }

                @Override
                public void onScroll(AbsListView absListView, int i, int i2, int i3) {

                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    TLRPC.User user = listViewAdapter.getItem(i);
                    //FileLog.e("tmessages", i+"");
                    if (i == 0) {

                        try {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, inviteText != null ? inviteText : LocaleController.getString("InviteText", R.string.InviteText));
                            startActivity(intent);
                        } catch (Exception e) {
                        }
                        return;
                    }
                  /*  if (i == 1) {
                        FragmentActivity inflaterActivity = parentActivity;
                        if (inflaterActivity == null) {
                            inflaterActivity = getActivity();
                        }
                        BaseFragment fragment = new GroupCreateActivity();
                        fragment.parentActivity = parentActivity;
                        //fragment3.applySelfActionBar();
                        ((LaunchActivity) parentActivity).presentFragment(fragment, "GroupCreate_" + Math.random(), false);
                        return;
                    }*/
                    if (i == 1) {

                        listView.getChildAt(i).setEnabled(false);
                        return;
                    } else {
                        FragmentActivity inflaterActivity = parentActivity;
                        if (inflaterActivity == null) {
                            inflaterActivity = getActivity();
                        }


                        ChatActivity fragment = new ChatActivity();
                        Bundle bundle = new Bundle();
                        bundle.putInt("user_id", 0);


                        bundle.putString("user_jid", user.jid);
                        // FileLog.e("Testing","listView onItemClick "+user.first_name);
                        //
                        // fragment.setArguments(bundle);
                        // ApplicationLoader.fragmentsStack.remove(ApplicationLoader.fragmentsStack.size() - 1);
                        // ApplicationLoader.fragmentsStack.add(fragment);

                        // ((LaunchActivity) parentActivity).current.onFragmentDestroy();


                        //fragment.setArguments(bundle);


                        fragment.setArguments(bundle);

                        ((LaunchActivity) inflaterActivity).presentFragment(fragment, "chat" + Math.random(), false);
                        //((CallMainActivity) parentActivity).presentFragment(fragment, "chat" + Math.random(), destroyAfterSelect, false);
                        return;
                    }

                }
            });


        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }

        return fragmentView;
    }

    private void didSelectResult(final TLRPC.User user, boolean useAlert) {
        if (useAlert && selectAlertString != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
            builder.setTitle(R.string.AppName);
            builder.setMessage(LocaleController.formatString(selectAlertStringDesc, selectAlertString, Utilities.formatName(user.first_name, user.last_name)));
            builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(user, false);
                }
            });
            builder.setNegativeButton(R.string.Cancel, null);
            builder.show().setCanceledOnTouchOutside(true);
        } else {
            if (delegate != null) {
                delegate.didSelectContact(user);
                delegate = null;
            }
            finishFragment();
            if (searchItem != null) {
                if (searchItem.isActionViewExpanded()) {
                    searchItem.collapseActionView();
                }
            }
        }
    }

    @Override
    public void applySelfActionBar() {
        FileLog.e("ContactsView", "applySelfActionBar");
        if (parentActivity == null) {
            return;
        }
       /* ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setCustomView(null);
        actionBar.setSubtitle(null);

        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (title != null) {
          //  title.setCompoundDrawablesWithIntrinsicBounds(Utilities.d(10), 0, 0, 0);
            title.setCompoundDrawablePadding(0);
        }
    */

        // ((LaunchActivity) parentActivity).fixBackButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        //FileLog.e("onResume", "onResume");
        if (isFinish) {
            return;
        }
        if (getActivity() == null) {
            return;
        }
        if (!firstStart && listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }
        firstStart = false;
        //((LaunchActivity) parentActivity).showActionBar();
        //((LaunchActivity) parentActivity).updateActionBar();
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == MessagesController.contactsDidLoaded) {

            if (listViewAdapter != null) {


                // if (com.yahala.xmpp.ContactsController.getInstance().ServerContacts.size()== 0)
                // {listView.setEmptyView(emptyTextView);}
              /*  for (Map.Entry<String, com.yahala.xmpp.ContactsController.Contact> entry : com.yahala.xmpp.ContactsController.getInstance().ServerContacts.entrySet()) {
                    com.yahala.xmpp.ContactsController.Contact c  =   entry.getValue();

                    TLRPC.User user=new TLRPC.User();
                        FileLog.e("Test phones",""+c.jid);

                           if ( c.avatar==null || c.avatar.isEmpty() )
                           {
                               int num = 7;
                               Random rand = new Random();
                               int ran = rand.nextInt(num);
                               String imgUri="drawable://"+ Utilities.arrUsersAvatars[ran];
                                 user.avatar= ImageLoader.getInstance().loadImageSync(imgUri);
                           }else{
                                 user.avatar =ImageLoader.getInstance().loadImageSync("file:///"+c.avatar); //BitmapFactory.decodeFile(c.avatar, options);

                           }


                    user.jid = c.jid;
                    user.last_seen=c.last_seen;
                    user.first_name = c.first_name;
                    user.last_name = c.last_name;
                    user.presence = c.presence;
                    ignoreUsers.add(user);
                    //FileLog.e("Test","dialogs_dict.put Jid " + user.jid  + "");
                    dialogs_dict.put(user.jid ,user);
                }
                contactsLoaded=true;
             */
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        progressLayout.setVisibility(View.GONE);
                        listViewAdapter.notifyDataSetChanged();
                    }
                });

               /*Utilities.stageQueue.postRunnable(new Runnable() {
                   @Override
                   public void run() {
                       FileLog.e("ContactsActivity","Reloading rosters");
                       XmppManager.getInstance().getConnection().getRoster().pre.reload();
                   }
               },2000);*/


            }
        } else if (id == MessagesController.updateInterfaces) {
               /* int mask = (Integer) args[0];
                if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    updateVisibleRows(mask);
                }*/
        } else if (id == XMPPManager.presenceRequestSent) {
            if (!ContactsController.getInstance().contactsLoaded) {
                //com.yahala.xmpp.ContactsController.getInstance().readContacts(false);
            }

        } else if (id == XMPPManager.presenceDidChanged) {
            try {
                String jid = (String) args[0];
                Presence presence = (Presence) args[1];
                String lastSeenMessage = (String) args[2];
                // FileLog.e("Test presence +++", (String)args[0] + "," + presence);
                // FileLog.e("Test presence did changed", com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(jid).first_name);
                if (com.yahala.xmpp.ContactsController.getInstance().friendsDict.containsKey(jid)) {
                    TLRPC.User obj = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(jid);
                    obj.presence = presence;
                    obj.last_seen = lastSeenMessage;
                    com.yahala.xmpp.ContactsController.getInstance().friendsDict.remove(jid);
                    com.yahala.xmpp.ContactsController.getInstance().friendsDict.put(jid, obj);
                }

                if (listView != null) {
                    updateVisibleRows();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void updateVisibleRows() {
        if (listView == null) {
            return;
        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    try {
                        View view = listView.getChildAt(a);
                        Object tag = view.getTag();
                        ContactsAdapter.ViewHolder holder = (ContactsAdapter.ViewHolder) tag;
                        holder.user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(holder.user.jid);
                        holder.update(false);
                    } catch (Exception e) {
                    }
                }


            }
        });
    }

    private void updateInviteText() {
        if (!updatingInviteText) {
            updatingInviteText = true;
            TLRPC.TL_help_getInviteText req = new TLRPC.TL_help_getInviteText();
            req.lang_code = Locale.getDefault().getCountry();
            if (req.lang_code == null || req.lang_code.length() == 0) {
                req.lang_code = "en";
            }
            /*ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    if (error == null) {
                        final TLRPC.TL_help_inviteText res = (TLRPC.TL_help_inviteText)response;
                        if (res.message.length() != 0) {
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    updatingInviteText = false;
                                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("invitetext", res.message);
                                    editor.putInt("invitetexttime", (int) (System.currentTimeMillis() / 1000));
                                    editor.commit();
                                }
                            });
                        }
                    }
                }
            }, null, true, RPCRequest.RPCRequestClassGeneric | RPCRequest.RPCRequestClassFailOnServerErrors);*/
        }
    }

    private void updateVisibleRows(int mask) {
        if (listView != null) {
            int count = listView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = listView.getChildAt(a);
                if (child instanceof ChatOrUserCell) {
                    ((ChatOrUserCell) child).update(mask);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //FileLog.e("Test presence +++", "onSaveInstanceState");
    }

    //private HashMap <String, TLRPC.User> dialogs_dict= new HashMap<String,  TLRPC.User>();
    public static interface ContactsActivityDelegate {
        public abstract void didSelectContact(TLRPC.User user);
    }

}
