package com.yahala.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.yahala.messenger.R;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.ui.Adapters.MessagesAdapter;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.xmpp.MessagesController;
import com.yahala.xmpp.MessagesStorage;
import com.yahala.xmpp.XMPPManager;

import org.jivesoftware.smack.packet.Presence;

import com.yahala.messenger.Utilities;

/**
 * Created by user on 4/24/2014.
 */
public class ChatsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private static boolean dialogsLoaded = false;
    private MessagesAdapter listViewAdapter;
    private TextView emptyTextView;
    private ListView listView;
    private boolean contactsLoaded = false;
    private boolean destroyAfterSelect;
    //private ArrayList<TLRPC.TL_dialog> ignoreUsers;
    private FrameLayout progressLayout;

    @SuppressWarnings("unchecked")
    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, MessagesController.dialogDidLoaded);
        // NotificationCenter.getInstance().addObserver(this, com.yahala.messenger.MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, XMPPManager.presenceDidChanged);

        NotificationCenter.getInstance().addObserver(this, XMPPManager.updateInterfaces);
        if (!dialogsLoaded) {
            MessagesController.getInstance().LoadDialogs(false);
            dialogsLoaded = true;
        }
        super.onFragmentCreate();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (fragmentView == null) {
                /*searching = false;
                searchWas = false;*/
            fragmentView = inflater.inflate(R.layout.conversation_layout, container, false);
            listView = (ListView) fragmentView.findViewById(R.id.listView);
            progressLayout = (FrameLayout) fragmentView.findViewById(R.id.progressLayout);

            listView.setVerticalScrollBarEnabled(true);
            emptyTextView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);
            emptyTextView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));

            //listViewAdapter = new ContactsActivityAdapter(parentActivity, onlyUsers, usersAsSections, ignoreUsers);
            listViewAdapter = new MessagesAdapter(parentActivity, R.id.listView, null);
            listViewAdapter.parentFragment = parentActivity;


            listView.setAdapter(listViewAdapter);
            if (MessagesController.getInstance().dialogs_dict.size() == 0) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    FragmentActivity inflaterActivity = parentActivity;
                    if (inflaterActivity == null) {
                        inflaterActivity = getActivity();
                    }

                    TLRPC.TL_dialog tl_dialog = listViewAdapter.getItem(i);
                    ChatActivity fragment = new ChatActivity();

                   /* Intent intent2 = new Intent(parentActivity, ChatActivity.class);
                    intent2.putExtra("user_id", 0);
                    intent2.putExtra("user_jid", tl_dialog.jid);
                    startActivity(intent2);*/

                    Bundle bundle = new Bundle();
                    bundle.putInt("user_id", 0);
                    bundle.putString("user_jid", tl_dialog.jid);
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


                }

            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                    final TLRPC.TL_dialog d;

                    d = MessagesController.getInstance().dialogs.get(i);

                    AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));

                    // if ((int)selectedDialog < 0) {
                    builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", R.string.ClearHistory), LocaleController.getString("DeleteChat", R.string.DeleteChat)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                MessagesStorage.getInstance().deleteAllMessages();
                            } else if (which == 1) {
                                MessagesStorage.getInstance().deleteMessage(d.jid);
                            }


                        }
                    });
                    //   } else {
                    //      builder.setItems(new CharSequence[]{LocaleController.getString("ClearHistory", R.string.ClearHistory), LocaleController.getString("Delete", R.string.Delete)}, new DialogInterface.OnClickListener() {
                    //         @Override
                    //      public void onClick(DialogInterface dialog, int which) {

                    //          }
                    //      });
                    //  }
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.show().setCanceledOnTouchOutside(true);
                    return true;
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

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getInstance().removeObserver(this, MessagesController.dialogDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, com.yahala.messenger.MessagesController.contactsDidLoaded);

        NotificationCenter.getInstance().removeObserver(this, XMPPManager.presenceDidChanged);
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.updateInterfaces);

    }


    @Override
    public void applySelfActionBar() {
       /* if (parentActivity == null) {
            return;
        }
        ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setSubtitle(null);
        actionBar.setCustomView(null);
        actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (title != null) {
            title.setPadding(0, 0, 0, 0);
            title.setCompoundDrawablePadding(0);
        }*/
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        // FileLog.e("didReceivedNotification", "" + id);
        if (id == MessagesController.dialogDidLoaded) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {

                    progressLayout.setVisibility(View.GONE);
                    listViewAdapter.notifyDataSetChanged();
                    emptyTextView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);

                    if (MessagesController.getInstance().dialogs_dict.size() == 0) {
                        emptyTextView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                    }
                }

            });
        } else if (id == com.yahala.messenger.MessagesController.contactsDidLoaded) {

            progressLayout.setVisibility(View.VISIBLE);
            MessagesController.getInstance().LoadDialogs(true);

        } else if (id == XMPPManager.updateInterfaces) {
            progressLayout.setVisibility(View.VISIBLE);
            MessagesController.getInstance().LoadDialogs(true);
           /* if (listViewAdapter.getCount() == 0){
                emptyTextView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);
                emptyTextView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
                emptyTextView.setVisibility(View.VISIBLE);
            }*/

        } else if (id == XMPPManager.presenceDidChanged) {

            String jid = (String) args[0];
            Presence presence = (Presence) args[1];
            // FileLog.e("Test presence +++", (String)args[0] + "," + presence);
            //  FileLog.e("Test presence did changed",  dialogs_dict.get(jid).fname);
            if (MessagesController.getInstance().dialogs_dict.containsKey(jid)) {
                TLRPC.TL_dialog obj = MessagesController.getInstance().dialogs_dict.get(jid);
                obj.presence = presence;
                MessagesController.getInstance().dialogs_dict.remove(jid);
                MessagesController.getInstance().dialogs_dict.put(jid, obj);
            }

            if (listView != null) {
                updateVisibleRows();
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
                        MessagesAdapter.ViewHolder holder = (MessagesAdapter.ViewHolder) tag;
                        holder.user = MessagesController.getInstance().dialogs_dict.get(holder.user.jid);
                        holder.update();
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

}
