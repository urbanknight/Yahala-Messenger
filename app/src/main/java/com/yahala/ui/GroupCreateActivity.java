/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.ui.Views.PinnedHeaderListView;
import com.yahala.ui.Views.SectionedBaseAdapter;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

//import com.yahala.messenger.ConnectionsManager;

public class GroupCreateActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    public ArrayList<TLRPC.User> searchResult;
    public ArrayList<CharSequence> searchResultNames;
    private SectionedBaseAdapter listViewAdapter;
    private PinnedHeaderListView listView;
    private TextView emptyTextView;
    private EditText userSelectEditText;
    private boolean ignoreChange = false;
    private HashMap<String, XImageSpan> selectedContacts = new HashMap<String, XImageSpan>();
    private ArrayList<XImageSpan> allSpans = new ArrayList<XImageSpan>();
    private boolean searchWas;
    private boolean searching;
    private Timer searchTimer;
    private CharSequence changeString;
    private int beforeChangeIndex;

    public GroupCreateActivity() {
        animationType = 1;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, MessagesController.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, MessagesController.chatDidCreated);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, MessagesController.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, MessagesController.chatDidCreated);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (fragmentView == null) {

            searching = false;
            searchWas = false;

            fragmentView = inflater.inflate(R.layout.group_create_layout, container, false);

            emptyTextView = (TextView) fragmentView.findViewById(R.id.searchEmptyView);
            emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
            userSelectEditText = (EditText) fragmentView.findViewById(R.id.bubble_input_text);
            userSelectEditText.setHint(LocaleController.getString("SendMessageTo", R.string.SendMessageTo));
            if (Build.VERSION.SDK_INT >= 11) {
                userSelectEditText.setTextIsSelectable(false);
            }
            userSelectEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    if (!ignoreChange) {
                        beforeChangeIndex = userSelectEditText.getSelectionStart();
                        changeString = new SpannableString(charSequence);
                    }
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!ignoreChange) {
                        boolean search = false;
                        int afterChangeIndex = userSelectEditText.getSelectionEnd();
                        if (editable.toString().length() < changeString.toString().length()) {
                            String deletedString = "";
                            try {
                                deletedString = changeString.toString().substring(afterChangeIndex, beforeChangeIndex);
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                            if (deletedString.length() > 0) {
                                if (searching && searchWas) {
                                    search = true;
                                }
                                Spannable span = userSelectEditText.getText();
                                for (int a = 0; a < allSpans.size(); a++) {
                                    XImageSpan sp = allSpans.get(a);
                                    if (span.getSpanStart(sp) == -1) {
                                        allSpans.remove(sp);
                                        selectedContacts.remove(sp.jid);
                                    }
                                }
                                if (parentActivity != null) {
                                    ActionBar actionBar = parentActivity.getSupportActionBar();
                                    actionBar.setSubtitle(String.format("%d/200 %s", selectedContacts.size(), LocaleController.getString("Members", R.string.Members)));
                                }
                                listView.invalidateViews();
                            } else {
                                search = true;
                            }
                        } else {
                            search = true;
                        }
                        if (search) {
                            String text = userSelectEditText.getText().toString().replace("<", "");
                            if (text.length() != 0) {
                                searchDialogs(text);
                                searching = true;
                                searchWas = true;
                                emptyTextView.setText(LocaleController.getString("NoResult", R.string.NoResult));
                                listViewAdapter.notifyDataSetChanged();
                            } else {
                                searchResult = null;
                                searchResultNames = null;
                                searching = false;
                                searchWas = false;
                                emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));
                                listViewAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });

            listView = (PinnedHeaderListView) fragmentView.findViewById(R.id.listView);
            listView.setEmptyView(emptyTextView);
            listView.setVerticalScrollBarEnabled(false);

            listView.setAdapter(listViewAdapter = new ListAdapter(parentActivity));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    TLRPC.User user;
                    int section = listViewAdapter.getSectionForPosition(i);
                    int row = listViewAdapter.getPositionInSectionForPosition(i);
                    if (searching && searchWas) {
                        user = searchResult.get(row);
                    } else {
                        ArrayList<TLRPC.User> arr = com.yahala.xmpp.ContactsController.getInstance().usersSectionsDict.get(com.yahala.xmpp.ContactsController.getInstance().sortedUsersSectionsArray.get(section));
                        user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(arr.get(row).jid);
                        listView.invalidateViews();
                    }
                    if (selectedContacts.containsKey(user.jid)) {
                        XImageSpan span = selectedContacts.get(user.jid);
                        selectedContacts.remove(user.jid);
                        SpannableStringBuilder text = new SpannableStringBuilder(userSelectEditText.getText());
                        text.delete(text.getSpanStart(span), text.getSpanEnd(span));
                        allSpans.remove(span);
                        ignoreChange = true;
                        userSelectEditText.setText(text);
                        userSelectEditText.setSelection(text.length());
                        ignoreChange = false;
                    } else {
                        if (selectedContacts.size() == 200) {
                            return;
                        }
                        ignoreChange = true;
                        XImageSpan span = createAndPutChipForUser(user);
                        span.jid = user.jid;
                        ignoreChange = false;
                    }
                    if (parentActivity != null) {
                        ActionBar actionBar = parentActivity.getSupportActionBar();
                        actionBar.setSubtitle(String.format("%d/200 %s", selectedContacts.size(), LocaleController.getString("Members", R.string.Members)));
                    }
                    if (searching || searchWas) {
                        searching = false;
                        searchWas = false;
                        emptyTextView.setText(LocaleController.getString("NoContacts", R.string.NoContacts));

                        ignoreChange = true;
                        SpannableStringBuilder ssb = new SpannableStringBuilder("");
                        for (ImageSpan sp : allSpans) {
                            ssb.append("<<");
                            ssb.setSpan(sp, ssb.length() - 2, ssb.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        userSelectEditText.setText(ssb);
                        userSelectEditText.setSelection(ssb.length());
                        ignoreChange = false;

                        listViewAdapter.notifyDataSetChanged();
                    } else {
                        listView.invalidateViews();
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

    @Override
    public void applySelfActionBar() {
        if (parentActivity == null) {
            return;
        }
        ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setCustomView(null);
        actionBar.setTitle(LocaleController.getString("NewGroup", R.string.NewGroup));
        actionBar.setSubtitle(String.format("%d/200 %s", selectedContacts.size(), LocaleController.getString("Members", R.string.Members)));

        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (title != null) {
            title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            title.setCompoundDrawablePadding(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() == null) {
            return;
        }
        ((LaunchActivity) parentActivity).showActionBar();
        ((LaunchActivity) parentActivity).updateActionBar();
    }

    public XImageSpan createAndPutChipForUser(TLRPC.User user) {
        LayoutInflater lf = (LayoutInflater) parentActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View textView = lf.inflate(R.layout.group_create_bubble, null);
        TextView text = (TextView) textView.findViewById(R.id.bubble_text_view);
        String name = Utilities.formatName(user.first_name, user.last_name);
        if (name.length() == 0 && user.phone != null && user.phone.length() != 0) {
            name = PhoneFormat.getInstance().format("+" + user.phone);
        }
        text.setText(name + ", ");

        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(spec, spec);
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(textView.getWidth(), textView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.translate(-textView.getScrollX(), -textView.getScrollY());
        textView.draw(canvas);
        textView.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = textView.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        textView.destroyDrawingCache();

        final BitmapDrawable bmpDrawable = new BitmapDrawable(b);
        bmpDrawable.setBounds(0, 0, b.getWidth(), b.getHeight());

        SpannableStringBuilder ssb = new SpannableStringBuilder("");
        XImageSpan span = new XImageSpan(bmpDrawable, ImageSpan.ALIGN_BASELINE);
        allSpans.add(span);
        selectedContacts.put(user.jid, span);
        for (ImageSpan sp : allSpans) {
            ssb.append("<<");
            ssb.setSpan(sp, ssb.length() - 2, ssb.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        userSelectEditText.setText(ssb);
        userSelectEditText.setSelection(ssb.length());
        return span;
    }

    public void searchDialogs(final String query) {
        if (query == null) {
            searchResult = null;
            searchResultNames = null;
        } else {
            try {
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            searchTimer = new Timer();
            searchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        searchTimer.cancel();
                        searchTimer = null;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    processSearch(query);
                }
            }, 100, 300);
        }
    }

    private void processSearch(final String query) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<TLRPC.User> contactsCopy = new ArrayList<TLRPC.User>();
                contactsCopy.addAll(com.yahala.xmpp.ContactsController.getInstance().friends);
                Utilities.globalQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (query.length() == 0) {
                            updateSearchResults(new ArrayList<TLRPC.User>(), new ArrayList<CharSequence>());
                            return;
                        }
                        long time = System.currentTimeMillis();
                        ArrayList<TLRPC.User> resultArray = new ArrayList<TLRPC.User>();
                        ArrayList<CharSequence> resultArrayNames = new ArrayList<CharSequence>();
                        String q = query.toLowerCase();

                        for (TLRPC.User contact : contactsCopy) {
                            TLRPC.User user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(contact.jid);
                            if (user.first_name.toLowerCase().startsWith(q) || user.last_name.toLowerCase().startsWith(q)) {
                                if (user.jid == UserConfig.currentUser.phone) {
                                    continue;
                                }
                                resultArrayNames.add(Utilities.generateSearchName(user.first_name, user.last_name, q));
                                resultArray.add(user);
                            }
                        }

                        updateSearchResults(resultArray, resultArrayNames);
                    }
                });
            }
        });
    }

    private void updateSearchResults(final ArrayList<TLRPC.User> users, final ArrayList<CharSequence> names) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                searchResult = users;
                searchResultNames = names;
                listViewAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finishFragment();
                break;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.group_create_menu, menu);
        SupportMenuItem doneItem = (SupportMenuItem) menu.findItem(R.id.done_menu_item);
        TextView doneTextView = (TextView) doneItem.getActionView().findViewById(R.id.done_button);
        doneTextView.setText(LocaleController.getString("Next", R.string.Next));
        doneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!selectedContacts.isEmpty()) {
                    ArrayList<String> result = new ArrayList<String>();
                    result.addAll(selectedContacts.keySet());
                    Bundle args = new Bundle();
                    args.putStringArrayList("result", result);
                    FileLog.e("selectedContacts.size()", selectedContacts.size() + "");
                    GroupCreateFinalActivity groupCreateFinalActivity = new GroupCreateFinalActivity();
                    groupCreateFinalActivity.setArguments(args);
                    ((LaunchActivity) parentActivity).presentFragment(groupCreateFinalActivity, "group_create_final", false);

                } else {
                    return;
                }

            }
        });
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == MessagesController.contactsDidLoaded) {
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        } else if (id == MessagesController.updateInterfaces) {
            int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                if (listView != null) {
                    listView.invalidateViews();
                }
            }
        } else if (id == MessagesController.chatDidCreated) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    removeSelfFromStack();
                }
            });
        }
    }

    public static class XImageSpan extends ImageSpan {
        public String jid;

        public XImageSpan(Drawable d, int verticalAlignment) {
            super(d, verticalAlignment);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm == null) {
                fm = new Paint.FontMetricsInt();
            }

            int sz = super.getSize(paint, text, start, end, fm);

            int offset = OSUtilities.dp(6);
            int w = (fm.bottom - fm.top) / 2;
            fm.top = -w - offset;
            fm.bottom = w - offset;
            fm.ascent = -w - offset;
            fm.leading = 0;
            fm.descent = w - offset;

            return sz;
        }
    }

    public static class ContactListRowHolder {
        public CircleImageView avatarImage;
        public TextView messageTextView;
        public TextView nameTextView;

        public ContactListRowHolder(View view) {
            messageTextView = (TextView) view.findViewById(R.id.messages_list_row_message);
            nameTextView = (TextView) view.findViewById(R.id.messages_list_row_name);
            avatarImage = (CircleImageView) view.findViewById(R.id.messages_list_row_avatar);
            Typeface typefaceR = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Regular.ttf");
            Typeface typefaceL = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Light.ttf");
            nameTextView.setTypeface(typefaceR);
            messageTextView.setTypeface(typefaceL);
            if (avatarImage != null)
                avatarImage.setBorderColor(0x00000000);
        }
    }

    private class ListAdapter extends SectionedBaseAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object getItem(int section, int position) {
            return null;
        }

        @Override
        public long getItemId(int section, int position) {
            return 0;
        }

        @Override
        public int getSectionCount() {
            if (searching && searchWas) {
                return searchResult == null || searchResult.isEmpty() ? 0 : 1;
            }
            return com.yahala.xmpp.ContactsController.getInstance().sortedUsersSectionsArray.size();
        }

        @Override
        public int getCountForSection(int section) {
            if (searching && searchWas) {
                return searchResult == null ? 0 : searchResult.size();
            }
            ArrayList<TLRPC.User> arr = com.yahala.xmpp.ContactsController.getInstance().usersSectionsDict.get(com.yahala.xmpp.ContactsController.getInstance().sortedUsersSectionsArray.get(section));
            return arr.size();
        }

        @Override
        public View getItemView(int section, int position, View convertView, ViewGroup parent) {
            TLRPC.User user;
            int size;

            if (searchWas && searching) {
                user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(searchResult.get(position).jid);
                size = searchResult.size();
            } else {
                ArrayList<TLRPC.User> arr = com.yahala.xmpp.ContactsController.getInstance().usersSectionsDict.get(com.yahala.xmpp.ContactsController.getInstance().sortedUsersSectionsArray.get(section));
                user = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(arr.get(position).jid);
                size = arr.size();
            }

            if (convertView == null) {
                LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(R.layout.group_create_row_layout, parent, false);
            }
            ContactListRowHolder holder = (ContactListRowHolder) convertView.getTag();
            if (holder == null) {
                holder = new ContactListRowHolder(convertView);
                convertView.setTag(holder);
            }

            ImageView checkButton = (ImageView) convertView.findViewById(R.id.settings_row_check_button);
            if (selectedContacts.containsKey(user.jid)) {
                checkButton.setImageResource(R.drawable.btn_check_on_holo_light);
            } else {
                checkButton.setImageResource(R.drawable.btn_check_off_holo_light);
            }

            View divider = convertView.findViewById(R.id.settings_row_divider);
            if (position == size - 1) {
                divider.setVisibility(View.INVISIBLE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }

            if (searchWas && searching) {
                holder.nameTextView.setText(searchResultNames.get(position));
            } else {
                String name = Utilities.formatName(user.first_name, user.last_name);
                if (name.length() == 0) {
                    if (user.phone != null && user.phone.length() != 0) {
                        name = PhoneFormat.getInstance().format("+" + user.phone);
                    } else {
                        name = LocaleController.getString("HiddenName", R.string.HiddenName);
                    }
                }
                holder.nameTextView.setText(name);
            }

            TLRPC.FileLocation photo = null;
            if (user.photo != null) {
                photo = user.photo.photo_small;
            }
            //int placeHolderId = Utilities.getUserAvatarForId(user.jid);
            holder.avatarImage.setImageBitmap(user.avatar);
            try {
                holder.messageTextView.setText(user.presence.getStatus());
            } catch (Exception e) {
            }
            /*if (user.status == null) {
                holder.messageTextView.setText(LocaleController.getString("Offline", R.string.Offline));
                holder.messageTextView.setTextColor(0xff808080);
            } else {
                int currentTime = ConnectionsManager.getInstance().getCurrentTime();
                if (user.status.expires > currentTime) {
                    holder.messageTextView.setTextColor(0xff357aa8);
                    holder.messageTextView.setText(LocaleController.getString("Online", R.string.Online));
                } else {
                    if (user.status.expires <= 10000) {
                        holder.messageTextView.setText(LocaleController.getString("Invisible", R.string.Invisible));
                    } else {
                        holder.messageTextView.setText(LocaleController.formatDateOnline(user.status.expires));
                    }
                    holder.messageTextView.setTextColor(0xff808080);
                }
            }*/

            return convertView;
        }

        @Override
        public int getItemViewType(int section, int position) {
            return 0;
        }

        @Override
        public int getItemViewTypeCount() {
            return 1;
        }

        @Override
        public int getSectionHeaderViewType(int section) {
            return 0;
        }

        @Override
        public int getSectionHeaderViewTypeCount() {
            return 1;
        }

        @Override
        public View getSectionHeaderView(int section, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(R.layout.settings_section_layout, parent, false);
                convertView.setBackgroundColor(0xffffffff);
            }
            TextView textView = (TextView) convertView.findViewById(R.id.settings_section_text);
            if (searching && searchWas) {
                textView.setText(LocaleController.getString("AllContacts", R.string.AllContacts));
            } else {
                textView.setText(com.yahala.xmpp.ContactsController.getInstance().sortedUsersSectionsArray.get(section));
            }
            return convertView;
        }
    }
}
