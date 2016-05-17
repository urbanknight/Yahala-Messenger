/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.OSUtilities;
import com.yahala.android.emoji.EmojiManager;
import com.yahala.android.emoji.Sticker;
import com.yahala.android.emoji.StickersGroup;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StickersView extends LinearLayout {
    private ArrayList<EmojiGridAdapter> adapters = new ArrayList<EmojiGridAdapter>();

    private EmojiPagerAdapter emojiPagerAdapter;
    private int[] icons = {
            R.drawable.ic_emoji_recent,
            R.drawable.ic_smiles_smile,
            R.drawable.ic_smiles_smile
    };
    private Listener listener;
    private ViewPager pager;
    private FrameLayout recentsWrap;
    private ArrayList<GridView> views = new ArrayList<GridView>();

    public StickersView(Context paramContext) {
        super(paramContext);
        init();
    }

    public StickersView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    public StickersView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init();
    }

    private void addToRecent(Sticker sticker) {
        if (this.pager.getCurrentItem() == 0) {
            return;
        }


        if (EmojiManager.getInstance().stickersCategories.get(0).stickers.contains(sticker)) {
            EmojiManager.getInstance().stickersCategories.get(0).stickers.remove(sticker);
            EmojiManager.getInstance().stickersCategories.get(0).stickers.add(0, sticker);
        } else {
            EmojiManager.getInstance().stickersCategories.get(0).stickers.add(0, sticker);

            if (EmojiManager.getInstance().stickersCategories.get(0).stickers.size() >= 50) {
                EmojiManager.getInstance().stickersCategories.get(0).stickers.remove(EmojiManager.getInstance().stickersCategories.get(0).stickers.size() - 1);
            }


        }

        emojiPagerAdapter.gias[0].notifyDataSetChanged();

        saveRecents();
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        loadRecents();
        setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#FF373737"), Color.parseColor("#FF575757"), Color.parseColor("#FF666666")}));


        emojiPagerAdapter = new EmojiPagerAdapter(getContext(), EmojiManager.getInstance().stickersCategories);
        pager = new ViewPager(getContext());
        pager.setAdapter(emojiPagerAdapter);
        FileLog.e("EmojiManager.stickersCategories", "" + EmojiManager.getInstance().stickersCategories.size());
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
        localImageView.setScaleType(ImageView.ScaleType.CENTER);
        localImageView.setBackgroundResource(R.drawable.bg_emoji_bs);
        localImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (StickersView.this.listener != null) {
                    StickersView.this.listener.onBackspace();
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

        if (!EmojiManager.getInstance().stickersCategoriesDict.containsKey("recents") || EmojiManager.getInstance().stickersCategoriesDict.get("recents").stickers.size() == 0) {
            pager.setCurrentItem(1);
        }
    }

    private void saveRecents() {

        ArrayList<String> localArrayList = new ArrayList<String>();
        StickersGroup recent = EmojiManager.getInstance().stickersCategories.get(0);

        if (recent.stickers.size() <= 0) {
            return;
        }
        int i = recent.stickers.size(); // FileLog.e("recents emoji",""+ getContext().getSharedPreferences("emoji", 0).getString("recents", ""));
        for (int j = 0; ; j++) {
            try {
                if (j >= i) {
                    getContext().getSharedPreferences("sticker", 0).edit().putString("recents", TextUtils.join(",", localArrayList)).commit();

                    return;
                }
                Sticker sticker = recent.stickers.get(j);
                localArrayList.add(Pattern.compile(':' + sticker.name + ':').toString());
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
        String str = getContext().getSharedPreferences("sticker", 0).getString("recents", "");
        FileLog.e("getSharedPreferences sticker", "" + str);
        String[] arrayOfString = null;
        StickersGroup stickersGroup = new StickersGroup();// EmojiManager.getInstance().stickersCategories.get("recents");
        stickersGroup.category = "recents";
        stickersGroup.stickers = new ArrayList<Sticker>();
        if ((str != null) && (str.length() > 0)) {
            arrayOfString = str.split(",");


            for (String pattern : arrayOfString) {
                FileLog.e("pattern", "pattern:" + pattern);
                Sticker sticker = EmojiManager.getInstance().stickers.get(pattern);

                if (stickersGroup.stickers.contains(sticker)) {
                    stickersGroup.stickers.remove(sticker);
                    stickersGroup.stickers.add(sticker);
                } else {
                    if (stickersGroup.stickers.size() <= 50) {
                        stickersGroup.stickers.add(sticker);
                    }

                }
            }

            // EmojiManager.getInstance().stickersCategories.add(emojiGroup);

        }
        if (!EmojiManager.getInstance().stickersCategoriesDict.containsKey("recents")) {
            EmojiManager.getInstance().stickersCategories.add(0, stickersGroup);
        }


        EmojiManager.getInstance().stickersCategoriesDict.put("recents", stickersGroup);
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
        ArrayList<Sticker> mSticker;

        public EmojiGridAdapter(Context c, ArrayList<Sticker> sticker) {
            mContext = c;
            mSticker = sticker;

        }


        public int getCount() {
            return mSticker.size();
        }

        public Object getItem(int position) {
            return mSticker.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView localObject = null;
            GifMovieView gifWebView = null;

            try {

                final Sticker sticker = mSticker.get(position);
                if (convertView != null && convertView instanceof ImageView)

                    localObject = (ImageView) convertView;
                else if (convertView != null && convertView instanceof GifMovieView) {
                    gifWebView = (GifMovieView) convertView;
                } else {
                    if (mSticker.get(position).smallPath.toLowerCase().contains("gif")) {
                        InputStream is = sticker.res.getAssets().open(mSticker.get(position).smallPath);
                        gifWebView = new GifMovieView(mContext);


                        //localObject = new BackupImageView(mContext);//OSUtilities.dp(32)

                        gifWebView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, OSUtilities.dp(70)));


                        gifWebView.setBackgroundResource(R.drawable.list_selector);
                        gifWebView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //FileLog.e("onItemClick",emoji.name+" t.name");
                                if (StickersView.this.listener != null) {
                                    StickersView.this.listener.onStickerSelected(mSticker.get((Integer) view.getTag()));
                                }
                                StickersView.this.addToRecent(mSticker.get((Integer) view.getTag()));
                            }
                        });
                    } else {


                        localObject = new ImageView(mContext);


                        //localObject = new BackupImageView(mContext);//OSUtilities.dp(32)

                        localObject.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, OSUtilities.dp(70)));

                        localObject.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        localObject.setBackgroundResource(R.drawable.list_selector);
                        localObject.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //FileLog.e("onItemClick",emoji.name+" t.name");
                                if (StickersView.this.listener != null) {
                                    StickersView.this.listener.onStickerSelected(mSticker.get((Integer) view.getTag()));
                                }
                                StickersView.this.addToRecent(mSticker.get((Integer) view.getTag()));
                            }
                        });


                    }
                }

          /* //  FileLog.e("mSticker.get(position).smallPath",mSticker.get(position).smallPath);
            InputStream is = sticker.res.getAssets().open(mSticker.get(position).smallPath);
           //  localObject.imageReceiver.setImage(mSticker.get(position).smallPath);
            localObject.setTag(position);
             //   ImageLoader.getInstance().displayImage("assets://"+mSticker.get(position).smallPath,localObject);

            Bitmap bmp = BitmapFactory.decodeStream(is);
            localObject.setImageBitmap(bmp);*/
                if (mSticker.get(position).smallPath.toLowerCase().contains("gif")) {
                    //localObject.setImageBitmap(ImageLoader.getInstance().loadImageSync("assets://"+mSticker.get(position).smallPath));
                   /* com.yahala.ui.Views.GifDrawable gifDrawable = new com.yahala.ui.Views.GifDrawable("/data/data/com.yahala.app/stickers/"+mSticker.get(position).smallPath);
                    if (gifDrawable != null) {
                        gifDrawable.start();
                        gifDrawable.invalidateSelf();
                        localObject.setImageDrawable(gifDrawable);
                        localObject.invalidate();
                    }*/
                    InputStream is = sticker.res.getAssets().open(mSticker.get(position).smallPath);
                    // Drawable d = Drawable.createFromStream(sticker.res.getAssets().open(mSticker.get(position).smallPath), null);
                    gifWebView.setMovieResource(is);

                } else {
                    localObject.setImageBitmap(ImageLoader.getInstance().loadImageSync("assets://" + mSticker.get(position).smallPath));
                }

            } catch (Exception e) {
                FileLog.e("grid", "problem rendering grid", e);
            }
            if (mSticker.get(position).smallPath.toLowerCase().contains("gif")) {
                return gifWebView;
            } else {
                return localObject;
            }

        }
    }


    public class EmojiPagerAdapter extends PagerAdapter implements PagerSlidingTabStripEmoji.IconTabProvider {


        public EmojiGridAdapter[] gias;
        List<StickersGroup> mStickersGroup;

        Context mContext;

        public EmojiPagerAdapter(Context context, List<StickersGroup> stickersGroup) {
            super();

            mContext = context;


            // Collections.reverse(emojiGroups);
            mStickersGroup = stickersGroup;
            gias = new EmojiGridAdapter[mStickersGroup.size()];

        }

        @Override
        public Object instantiateItem(View collection, int position) {

            gias[position] = new EmojiGridAdapter(mContext, mStickersGroup.get(position).stickers);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

            GridView imagegrid = (GridView) inflater.inflate(R.layout.stickers_grid, null);


            imagegrid.setAdapter(gias[position]);

            ((ViewPager) collection).addView(imagegrid);
            return imagegrid;
        }

        @Override
        public int getCount() {
            return mStickersGroup.size();
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
            return mStickersGroup.get(position).category;
        }

        public int getPageIconResId(int paramInt) {
            return StickersView.this.icons[paramInt];
        }
    }

    public static abstract interface Listener {
        public abstract void onBackspace();

        public abstract void onStickerSelected(Sticker t);
    }
}
