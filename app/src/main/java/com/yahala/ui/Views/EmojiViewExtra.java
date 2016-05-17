/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;

import com.yahala.android.OSUtilities;

import com.yahala.android.emoji.Emoji;
import com.yahala.android.emoji.EmojiGroup;
import com.yahala.android.emoji.EmojiManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EmojiViewExtra extends LinearLayout {
    private ArrayList<EmojiGridAdapter> adapters = new ArrayList<EmojiGridAdapter>();

    private EmojiPagerAdapter emojiPagerAdapter;
    private int[] icons = {
            R.drawable.ic_emoji_recent,
            R.drawable.ic_emoji_smile,
            R.drawable.ic_emoji_smile,
            R.drawable.ic_emoji_smile,
            R.drawable.ic_emoji_smile,
            R.drawable.ic_emoji_symbol,
            R.drawable.ic_emoji_car,
            R.drawable.ic_emoji_car

    };
    private Listener listener;
    private ViewPager pager;
    private FrameLayout recentsWrap;
    private ArrayList<GridView> views = new ArrayList<GridView>();

    public EmojiViewExtra(Context paramContext) {
        super(paramContext);
        init();
    }

    public EmojiViewExtra(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    public EmojiViewExtra(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init();
    }

    private void addToRecent(Emoji emoji) {
        if (this.pager.getCurrentItem() == 0) {
            return;
        }


        if (EmojiManager.getInstance().categories.get(0).emojis.contains(emoji)) {
            EmojiManager.getInstance().categories.get(0).emojis.remove(emoji);
            EmojiManager.getInstance().categories.get(0).emojis.add(0, emoji);
        } else {
            EmojiManager.getInstance().categories.get(0).emojis.add(0, emoji);

            if (EmojiManager.getInstance().categories.get(0).emojis.size() >= 50) {
                EmojiManager.getInstance().categories.get(0).emojis.remove(EmojiManager.getInstance().categories.get(0).emojis.size() - 1);
            }


        }

        emojiPagerAdapter.gias[0].notifyDataSetChanged();

        saveRecents();
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        loadRecents();
        setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#FF373737"), Color.parseColor("#FF575757"), Color.parseColor("#FF666666")}));


        emojiPagerAdapter = new EmojiPagerAdapter(getContext(), EmojiManager.getInstance().categories);
        pager = new ViewPager(getContext());
        pager.setOffscreenPageLimit(5);
        pager.setAdapter(emojiPagerAdapter);
        FileLog.e("EmojiManager.emojiGroups", "" + EmojiManager.getInstance().categories.size());
        PagerSlidingTabStripEmoji tabs = new PagerSlidingTabStripEmoji(getContext());
        tabs.setViewPager(pager);
        tabs.setShouldExpand(false);
        tabs.setMinimumWidth(OSUtilities.dp(50));
        tabs.setTabPaddingLeftRight(OSUtilities.dp(10));
        tabs.setIndicatorHeight(3);
        //tabs.setTabBackground(Color.parseColor("#FF3f9fe0"));
        tabs.setTabBackground(R.drawable.bar_selector_main);


        tabs.setIndicatorColor(Color.parseColor("#FFffffff"));
        tabs.setDividerColor(Color.parseColor("#ff222222"));
        tabs.setUnderlineHeight(2);
        tabs.setUnderlineColor(Color.parseColor("#ff373737"));

        //tabs.setTabBackground(0);
        LinearLayout localLinearLayout = new LinearLayout(getContext());
        localLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        localLinearLayout.addView(tabs, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f));
        ImageView localImageView = new ImageView(getContext());
        localImageView.setImageResource(R.drawable.ic_emoji_backspace);
        localImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        localImageView.setBackgroundResource(R.drawable.bg_emoji_bs);
        localImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (EmojiViewExtra.this.listener != null) {
                    EmojiViewExtra.this.listener.onBackspace();
                }
            }
        });
        localLinearLayout.addView(localImageView, new LayoutParams(OSUtilities.dpf(61.0f), LayoutParams.MATCH_PARENT));
       /* recentsWrap = new FrameLayout(getContext());
        recentsWrap.addView(views.get(0));
        TextView localTextView = new TextView(getContext());
        localTextView.setText(LocaleController.getString("NoRecent", R.string.NoRecent));
        localTextView.setTextSize(18.0f);
        localTextView.setTextColor(-7829368);
        localTextView.setGravity(17);
        recentsWrap.addView(localTextView);
        views.get(0).setEmptyView(localTextView);*/
        addView(localLinearLayout, new LayoutParams(-1, OSUtilities.dpf(48.0f)));

        addView(pager);

        if (!EmojiManager.getInstance().categoriesDict.containsKey("recents") || EmojiManager.getInstance().categoriesDict.get("recents").emojis.size() == 0) {
            pager.setCurrentItem(1);
        }
    }

    private void saveRecents() {

        ArrayList<String> localArrayList = new ArrayList<String>();
        EmojiGroup recent = EmojiManager.getInstance().categories.get(0);

        if (recent.emojis.size() <= 0) {
            return;
        }
        int i = recent.emojis.size(); // FileLog.e("recents emoji",""+ getContext().getSharedPreferences("emoji", 0).getString("recents", ""));
        for (int j = 0; ; j++) {
            try {
                if (j >= i) {
                    getContext().getSharedPreferences("emoji", 0).edit().putString("recents", TextUtils.join(",", localArrayList)).commit();

                    return;
                }
                Emoji emoji = recent.emojis.get(j);
                localArrayList.add(Pattern.compile(':' + emoji.name + ':').toString());
            /*if (emoji.moji != null)
                localArrayList.add(Pattern.compile(emoji.moji,Pattern.LITERAL).toString());
            if (emoji.emoticon != null)
                localArrayList.add(Pattern.compile(emoji.emoticon,Pattern.LITERAL).toString());
            if (emoji.category != null)
                localArrayList.add(emoji.category);*/

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void loadRecents() {
        String str = getContext().getSharedPreferences("emoji", 0).getString("recents", "");
        FileLog.e("getSharedPreferences emoji", "" + str);
        String[] arrayOfString = null;
        EmojiGroup emojiGroup = new EmojiGroup();// EmojiManager.getInstance().categories.get("recents");
        emojiGroup.category = "recents";
        emojiGroup.emojis = new ArrayList<Emoji>();
        if ((str != null) && (str.length() > 0)) {
            arrayOfString = str.split(",");


            for (String pattern : arrayOfString) {
                FileLog.e("pattern", "pattern:" + pattern);
                Emoji emoji = EmojiManager.getInstance().emoticonsDict.get(pattern);

                if (emojiGroup.emojis.contains(emoji)) {
                    emojiGroup.emojis.remove(emoji);
                    emojiGroup.emojis.add(emoji);
                } else {
                    if (emojiGroup.emojis.size() <= 50) {
                        emojiGroup.emojis.add(emoji);
                    }

                }
            }

            // EmojiManager.getInstance().categories.add(emojiGroup);

        }
        if (!EmojiManager.getInstance().categoriesDict.containsKey("recents")) {
            EmojiManager.getInstance().categories.add(0, emojiGroup);
        }


        EmojiManager.getInstance().categoriesDict.put("recents", emojiGroup);
        if (arrayOfString != null) {
            //emojiPagerAdapter.notifyDataSetChanged();
        }
    }

    public void onMeasure(int paramInt1, int paramInt2) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(paramInt1), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(paramInt2), MeasureSpec.EXACTLY));
    }

    public void setListener(Listener paramListener) {
        this.listener = paramListener;
    }

    public void invalidateViews() {
        for (GridView gridView : views) {
            if (gridView != null) {
                gridView.invalidateViews();
            }
        }
    }

    public class EmojiGridAdapter extends BaseAdapter {
        private Context mContext;
        ArrayList<Emoji> mEmoji;

        public EmojiGridAdapter(Context c, ArrayList<Emoji> emoji) {
            mContext = c;
            mEmoji = emoji;

        }


        public int getCount() {
            return mEmoji.size();
        }

        public Object getItem(int position) {
            return mEmoji.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView localObject = null;
            try {

                final Emoji emoji = mEmoji.get(position);
                if (convertView != null && convertView instanceof ImageView)
                    localObject = (ImageView) convertView;
                else {
                    localObject = new ImageView(mContext);


                    localObject = new ImageView(mContext);//OSUtilities.dp(32)

                    localObject.setLayoutParams(new GridView.LayoutParams(OSUtilities.dp(34), OSUtilities.dp(34)));
                    localObject.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    localObject.setBackgroundResource(R.drawable.list_selector);
                    localObject.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //FileLog.e("onItemClick",emoji.name+" t.name");
                            if (EmojiViewExtra.this.listener != null) {
                                EmojiViewExtra.this.listener.onEmojiSelected(mEmoji.get((Integer) view.getTag()));
                            }
                            EmojiViewExtra.this.addToRecent(mEmoji.get((Integer) view.getTag()));
                        }
                    });


                }
                localObject.setTag(position);

                InputStream is = emoji.res.getAssets().open(mEmoji.get(position).assetPath);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                localObject.setImageBitmap(bmp);
            } catch (Exception e) {
                Log.e("grid", "problem rendering grid", e);
            }
            return localObject;
        }
    }


    public class EmojiPagerAdapter extends PagerAdapter implements PagerSlidingTabStripEmoji.IconTabProvider {


        public EmojiGridAdapter[] gias;
        List<EmojiGroup> mEmojiGroups;

        Context mContext;

        public EmojiPagerAdapter(Context context, List<EmojiGroup> emojiGroups) {
            super();

            mContext = context;


            // Collections.reverse(emojiGroups);
            mEmojiGroups = emojiGroups;
            gias = new EmojiGridAdapter[mEmojiGroups.size()];

        }

        @Override
        public Object instantiateItem(View collection, int position) {

            gias[position] = new EmojiGridAdapter(mContext, mEmojiGroups.get(position).emojis);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

            GridView imagegrid = (GridView) inflater.inflate(R.layout.emojigrid, null);


            imagegrid.setAdapter(gias[position]);

            ((ViewPager) collection).addView(imagegrid);
            return imagegrid;
        }

        @Override
        public int getCount() {
            return mEmojiGroups.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object arg2) {
            ((ViewPager) collection).removeView((ViewGroup) arg2);
        }


        @Override
        public Parcelable saveState() {
            return null;
        }


        @Override
        public void startUpdate(ViewGroup collection) {
        }

        @Override
        public void finishUpdate(ViewGroup collection) {
        }


        @Override
        public CharSequence getPageTitle(int position) {

            return mEmojiGroups.get(position).category;


        }

        public int getPageIconResId(int paramInt) {
            return EmojiViewExtra.this.icons[paramInt];
        }
    }

    public static abstract interface Listener {
        public abstract void onBackspace();

        public abstract void onEmojiSelected(Emoji t);
    }
}
