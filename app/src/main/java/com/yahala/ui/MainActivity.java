package com.yahala.ui;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Messenger;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;


import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.ui.Views.PagerSlidingTabStripEmoji;
import com.yahala.xmpp.MessagesStorage;
import com.yahala.xmpp.XMPPManager;

import java.util.ArrayList;

public class MainActivity extends BaseFragment implements PasswordActivity.PasswordDelegate, NotificationCenter.NotificationCenterDelegate, ViewPager.OnPageChangeListener {
    private View backStatusButton;
    private View statusBackground;
    private TextView statusText;
    private TextView unreadCount;
    private View statusView;
    private int currentConnectionState;
    private Messenger activityMessenger;
    private View background_view;
    View homeView;
    TextView titleText;
    ImageView home_button;
    Dialog dialog;
    Toolbar mToolbar;
    private android.support.design.widget.TabLayout tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;


    private Drawable oldBackground = null;
    private int currentColor = 0xFF3F9FE0;
    private float CurrentAlpha = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        FileLog.e("onOptionsItemSelected", "itemId:" + itemId);
        FragmentActivity inflaterActivity = parentActivity;


        switch (itemId) {
            case R.id.contacts_list_menu_settings: {


                ((LaunchActivity) inflaterActivity).presentFragment(new SettingsActivity(), "settings", false);


                return true;
            }

            case R.id.InviteFriends: {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, LocaleController.getString("InviteText", R.string.InviteText));
                startActivity(intent);
                return true;
            }
            case R.id.NewGroup: {
                ((LaunchActivity) inflaterActivity).presentFragment(new GroupCreateActivity(), "Group", false);
                return true;
            }
            case R.id.Online: {
                XMPPManager.getInstance().setPresence(XMPPManager.Available, true);
                ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();
                return true;
            }
            case R.id.Away: {
                XMPPManager.getInstance().setPresence(XMPPManager.Away, true);
                ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();
                return true;
            }
            case R.id.DoNotDisturb: {
                XMPPManager.getInstance().setPresence(XMPPManager.DoNotDisturb, true);
                ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();
                return true;
            }
            case R.id.FreeToChat: {
                XMPPManager.getInstance().setPresence(XMPPManager.FreeToChat, true);
                ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();
                return true;
            }
            case R.id.Offline: {

                XMPPManager.getInstance().setPresence(5, true);
                ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();

                return true;
            }
        }
       /* ((LaunchActivity)(parentActivity)).invalidateOptionsMenu();
        ((LaunchActivity)(parentActivity)).menu.clear();
        ((LaunchActivity)(parentActivity)).getMenuInflater().inflate(R.menu.contacts, ((LaunchActivity)(parentActivity)).menu);

        ((LaunchActivity)(parentActivity)).updatePresenceMenuIcon();*/
        return false;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mToolbar.getMenu().clear();
        menu.clear();
        mToolbar.removeView(homeView);
        inflater.inflate(R.menu.contacts, menu);

        ActionBar actionBar = parentActivity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        // actionBar.setHomeAsUpIndicator(R.drawable.ab_icon_fixed2);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflator = (LayoutInflater) getParentActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        homeView = inflator.inflate(R.layout.home_actionbar, null);
        statusBackground = homeView.findViewById(R.id.back_button_background);
        home_button = (ImageView) homeView.findViewById(R.id.home_button);
        titleText = (TextView) homeView.findViewById(R.id.title_text);
        titleText.setText("");
        titleText.setText(LocaleController.getString("AppName", R.string.AppName));
        //actionBar.setCustomView(homeView);

        mToolbar.addView(homeView);

        actionBar.setSubtitle(null);
        homeView.setVisibility(View.VISIBLE);

        homeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflator = (LayoutInflater) getParentActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);


                //  passwordGrid.setAtt();

                PasswordActivity passwordActivity = new PasswordActivity(ApplicationLoader.applicationContext, null);
                passwordActivity.setDelegate(MainActivity.this);

                dialog = new Dialog(parentActivity, R.style.PatternLoackDialogStyle);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(passwordActivity);

                //dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                //dialog.setContentView(R.layout.lock_pattern);
                //dialog.setTitle("Lock Pattern");
                OSUtilities.changeOrientation(getParentActivity());
                //Utilities.lockOrientation(getParentActivity());
                dialog.show();
            }
        });

        // actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));

        // ((LaunchActivity) (parentActivity)).menu = menu;
        // ((LaunchActivity) (parentActivity)).updatePresenceMenuIcon();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance().addObserver(this, XMPPManager.updateInterfaces);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, XMPPManager.updateInterfaces);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            setHasOptionsMenu(true);
            /*if(!XmppManager.getInstance().isConnected()){
                XmppManager.getInstance().xmppRequestStateChange(ConnectionState.ONLINE);
            }*/

            //FileLog.e("MainActivity", "onCreateView");
            if (fragmentView == null) {
                fragmentView = inflater.inflate(R.layout.activity_main, container, false);
                background_view = (View) fragmentView.findViewById(R.id.background_view);
                mToolbar = (Toolbar) fragmentView.findViewById(R.id.toolbar);

                parentActivity.setSupportActionBar(mToolbar);
                tabs = (TabLayout) fragmentView.findViewById(R.id.tabs);


                // tabs = (android.support.design.widget.TabLayout) fragmentView.findViewById(R.id.tabs);
                // tabs.setIndicatorHeight(5);
                // tabs.setTabBackground(Color.parseColor("#FF3f9fe0"));
                // tabs.setTabBackground(R.drawable.bar_selector_main);
                /*

                tabs.setTextSize(OSUtilities.dp(16));
                tabs.setSelectedTabIndicatorColor(Color.parseColor("#FFffffff"));
                tabs.setDividerColor(Color.parseColor("#ff2289ce"));
                tabs.setTabTextColors(getResources().getColorStateList(R.color.White));
                tabs.setSelectedTabIndicatorHeight(2);
                tabs.setUnderlineColor(Color.parseColor("#2180bf"));
                //tabs.setShouldExpand(true);

                if (LocaleController.isRTL) {
                    tabs.setTextSize(OSUtilities.dp(15));
                } else {
                    tabs.setTextSize(OSUtilities.dp(13));
                }
                tabs.setOnTabSelectedListener(this);
                tabs.setOnPageChangeListener(this);*/

                /*Typeface font = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "Roboto-Bold.ttf");

                tabs.setTypeface(font,Typeface.BOLD);*/

                tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        try {
                            CoordinatorLayout coordinator = (CoordinatorLayout) parentActivity.findViewById(R.id.coordinator);
                            AppBarLayout appbar = (AppBarLayout) parentActivity.findViewById(R.id.AppBarLayout);
                            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appbar.getLayoutParams();
                            AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
                            if (tab.getPosition() == 1 || tab.getPosition() == 2 || tab.getPosition() == 3) {
                                behavior.onNestedFling(coordinator, appbar, null, 0, -1000, true);

                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {

                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {

                    }
                });

                pager = (ViewPager) fragmentView.findViewById(R.id.pager);
                pager.setOffscreenPageLimit(4);
                //pager.setAnimationCacheEnabled(true);
                //  pager.setPersistentDrawingCache(ViewGroup.PERSISTENT_SCROLLING_CACHE);
                //pager.setAlwaysDrawnWithCacheEnabled(true);
                // pager.setPageTransformer(true, new bgPageTransformer());
                // pager.setOnPageChangeListener(this);


                //Typeface font =Typeface.create("Roboto",Typeface.BOLD);
                adapter = new MyPagerAdapter(parentActivity.getSupportFragmentManager());
                tabs.setTabsFromPagerAdapter(adapter);

                pager.setAdapter(adapter);
                pager.setCurrentItem(1);

                final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, getResources()
                        .getDisplayMetrics());

                pager.setPageMargin(pageMargin);

                tabs.setupWithViewPager(pager);

                /* try {
                    Field mScroller;
                    mScroller = ViewPager.class.getDeclaredField("mScroller");
                    mScroller.setAccessible(true);
                    Interpolator sInterpolator = new DecelerateInterpolator();
                    FixedSpeedScroller scroller = new FixedSpeedScroller(pager.getContext(), sInterpolator);
                    // scroller.setFixedDuration(5000);
                    mScroller.set(pager, scroller);
                } catch (NoSuchFieldException e) {
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                }
                */

                int unreadMsgs = MessagesStorage.getInstance().getUnreadUpdatesCount();
                unreadCount = (TextView) fragmentView.findViewById(R.id.unreadCount);

                if (unreadMsgs > 0) {
                    unreadCount.setText(unreadMsgs + "");
                    unreadCount.setVisibility(View.VISIBLE);
                    // tabs.setUnreadIconVisibility(View.VISIBLE, 1);
                } else {
                    unreadCount.setVisibility(View.GONE);
                }

            } else {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
            }

            // containerView = findViewById(R.id.container);
            //statusText = (TextView)statusView.findViewById(R.id.status_text);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fragmentView;

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == XMPPManager.updateInterfaces) {
            try {

                int unreadMsgs = MessagesStorage.getInstance().getUnreadUpdatesCount();
                if (unreadMsgs > 0) {
                    unreadCount.setText(unreadMsgs + "");
                    unreadCount.setVisibility(View.VISIBLE);
                    // tabs.setUnreadIconVisibility(View.VISIBLE, 1);
                } else {
                    unreadCount.setVisibility(View.GONE);
                }

              /*  if (MessagesStorage.getInstance().getUnreadUpdatesCount() > 0) {
                   // tabs.setUnreadIconVisibility(View.VISIBLE, 1);
                } else {
                  //  tabs.setUnreadIconVisibility(View.GONE, 1);
                }*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void applySelfActionBar() {
        try {
            if (parentActivity == null) {
                return;
            }
            final ActionBar actionBar = parentActivity.getSupportActionBar();
            //  actionBar.hide();
            //ImageView view = (ImageView) fragmentView.findViewById(16908332);
            // if (view == null) {
            ImageView view = (ImageView) fragmentView.findViewById(R.id.home);
            // }
            if (view != null) {
                view.setPadding(OSUtilities.dp(6), 0, OSUtilities.dp(6), 0);
            }
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);

            actionBar.setCustomView(null);
            actionBar.setSubtitle(null);
            actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // FileLog.e("setOnPageChangeListener","position:"+position+"  positionOffset:" + positionOffset);
        // fragmentView.setAlpha(1 - position);
        // background_view.setAlpha(1);


        if (LocaleController.isRTL) {
            if (position == 1) {

                background_view.setAlpha(positionOffset);
                CurrentAlpha = positionOffset;
                //  FileLog.e("setOnPageChangeListener position == 1",position+" CurrentAlpha:"+CurrentAlpha);
            } else if (position == 0 && CurrentAlpha > 0.5F) {
                background_view.setAlpha(0);
                CurrentAlpha = positionOffset;
                //   FileLog.e("setOnPageChangeListener position == 0",position +"CurrentAlpha:"+CurrentAlpha);
            } else if (position == 2 && CurrentAlpha > 0.5F) {
                background_view.setAlpha(0);
                CurrentAlpha = 0;
                //  FileLog.e("setOnPageChangeListener position == 2",position + "CurrentAlpha:"+CurrentAlpha);
            }
        } else {
            if (position == 0) {
                //FileLog.e("setOnPageChangeListener","position:"+(1 - positionOffset));
                background_view.setAlpha(1 - positionOffset);
                CurrentAlpha = 1 - positionOffset;
            } else if (position == 1 /*&& CurrentAlpha > 0.50F*/) {
                background_view.setAlpha(0);
                //FileLog.e("setOnPageChangeListener","ALPHA:"+(1 - positionOffset));
                // background_view.setAlpha(1 - positionOffset);
                //CurrentAlpha = 1 - positionOffset;
                CurrentAlpha = 0;

            } else if (position == 2 /*&& CurrentAlpha > 0.50F*/) {
                //FileLog.e("setOnPageChangeListener","position:"+(CurrentAlpha));

                background_view.setAlpha(0);
                CurrentAlpha = 0;

            } else if (position == 3) {

            }
        }
    }


    @Override
    public void onPageSelected(int position) {
        //   FileLog.e("setOnPageChangeListener","position:" + position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
         /*if(state==ViewPager.SCROLL_STATE_IDLE){
           if(pager.getCurrentItem()==INDEX_OF_ANIMATED_VIEW){
                pager.setOffscreenPageLimit(1);
            }else{
                pager.setOffscreenPageLimit(OLD_PAGE_LENGTH);
            }
        }*/
    }

    @Override
    public void needFinish() { //Utilities.unlockOrientation(getParentActivity());
        dialog.hide();
    }

    @Override
    public void onPasswordComplete(ArrayList<String> photos) {

    }


    @Override
    public void onContinueClicked() {

    }

    public class MyPagerAdapter extends FragmentPagerAdapter /*implements PagerSlidingTabStripEmoji.IconTabProvider */ {

        private String[] TITLES;
        private int[] Icons;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            //  Configuration config = getResources().getConfiguration();
            //FileLog.e("config.getLayoutDirection()",View.LAYOUT_DIRECTION_RTL+"");
            // FileLog.e("config.getLayoutDirection()", config.getLayoutDirection()+"");
         /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                getWindow().getDecorView().setLayoutDirection(
                        View.LAYOUT_DIRECTION_RTL);
            }*/
            // if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (LocaleController.isRTL) {
                TITLES = new String[]{/*LocaleController.getString("Settings", R.string.Settings),*/
                        LocaleController.getString("Contacts", R.string.Contacts),
                        LocaleController.getString("Chats", R.string.Chats),
                        LocaleController.getString("Updates", R.string.Updates)};
                Icons = new int[]{R.drawable.ic_main_settings, R.drawable.ic_main_users, R.drawable.ic_main_conversation, R.drawable.ic_updates_unselected};

            } else {
                TITLES = new String[]{LocaleController.getString("Updates", R.string.Updates),
                        LocaleController.getString("Chats", R.string.Chats),
                        LocaleController.getString("Contacts", R.string.Contacts)/*,
                        LocaleController.getString("Settings", R.string.Settings)*/};
                Icons = new int[]{R.drawable.ic_main_updates, R.drawable.ic_main_conversation, R.drawable.ic_main_users, R.drawable.ic_main_settings};
            }
        }

        /*
            @Override
            public CharSequence getPageTitle(int position) {
                return TITLES[position];
            }
        */

        @Override
        public int getCount() {
            return TITLES.length;
        }


        @Override
        public Fragment getItem(int position) {
            //Configuration config = getResources().getConfiguration();

            // if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (LocaleController.isRTL) {
                switch (position) {
                   /* case 0:
                        // Top Rated fragment activity

                        BaseFragment fragment4 = new SettingsActivity();
                        fragment4.onFragmentCreate();

                        return fragment4;*/
                    case 0:
                        // Movies fragment activity
                        BaseFragment fragment3 = new ContactsActivity();
                        fragment3.onFragmentCreate();
                        return fragment3;
                    case 1:
                        // Games fragment activity
                        BaseFragment fragment2 = new ChatsActivity();
                        fragment2.onFragmentCreate();
                        return fragment2;

                    case 2:
                        // Top Rated fragment activity

                        BaseFragment fragment = new UpdatesActivity();
                        fragment.onFragmentCreate();

                        return fragment;


                }
            } else {
                switch (position) {
                    case 0:
                        // Top Rated fragment activity

                        BaseFragment fragment = new UpdatesActivity();
                        fragment.onFragmentCreate();
                        //  fragment.applySelfActionBar();
                        return fragment;
                    case 1:
                        // Games fragment activity
                        BaseFragment fragment2 = new ChatsActivity();
                        fragment2.onFragmentCreate();
                        return fragment2;
                    case 2:
                        // Movies fragment activity
                        BaseFragment fragment3 = new ContactsActivity();
                        fragment3.onFragmentCreate();
                        return fragment3;
                    /*case 3:
                        // Top Rated fragment activity

                        BaseFragment fragment4 = new SettingsActivity();
                        fragment4.onFragmentCreate();

                        return fragment4;*/
                }
            }
            return null;
            //return CardFragment.newInstance(position);
        }

        @Override
        public String getPageTitle(int position) {
            return TITLES[position];
        }
        /*@Override
        public int getPageIconResId(int position) {
            return Icons[position];
        }*/
    }


    @Override
    public void onResume() {
        super.onResume();
        final ActionBar actionBar = parentActivity.getSupportActionBar();

        parentActivity.setSupportActionBar(mToolbar);
        // ((LaunchActivity) parentActivity).updateActionBar();
        //// actionBar.hide();
    }

    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.93f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public class bgPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.94f;
        private static final float MIN_ALPHA = 0.7f;
        private boolean animating = false;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

           /* if (position >= 1) {

                background_view.setAlpha(1 * position);
                //fragmentView.setBackgroundColor(0xFFcecece);
            }else{
                background_view.setAlpha(0);
                //fragmentView.setBackgroundColor(0xFFffffff);

            }*/
            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.

                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public class FixedSpeedScroller extends Scroller {

        private int mDuration = 200;

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }


        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }
    }


}
