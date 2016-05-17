package com.yahala.android.emoji;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.yahala.messenger.FileLog;
import com.yahala.android.OSUtilities;
import com.yahala.ui.ApplicationLoader;

import com.yahala.messenger.Utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiManager {


    private static EmojiManager mInstance = null;

    public Map<Pattern, Emoji> emoticons = new HashMap<Pattern, Emoji>();
    public Map<String, Emoji> emoticonsDict = new HashMap<String, Emoji>();


    public Map<String, Sticker> stickers = new HashMap<String, Sticker>();

    public Map<String, EmojiGroup> categoriesDict = new HashMap<String, EmojiGroup>();
    public ArrayList<EmojiGroup> categories = new ArrayList<EmojiGroup>();

    public Map<String, StickersGroup> stickersCategoriesDict = new HashMap<String, StickersGroup>();
    public ArrayList<StickersGroup> stickersCategories = new ArrayList<StickersGroup>();

    public ArrayList<String> categoriesNames = new ArrayList<String>(Arrays.asList("yolks", "theblacy", "lazycrazy"/*, "faces", "symbols"*/));
    public ArrayList<String> stickersCategoriesNames = new ArrayList<String>(/*Arrays.asList("stickers")*/);

    private Context mContext;
    private static int emojiFullSize;
    private final static String PLUGIN_CONSTANT = "com.yahala.os.emoji.STICKER_PACK";

    //public Collection<EmojiGroup> emojiGroups;


    public static class EmojiSpan extends ImageSpan /*implements LineHeightSpan.WithDensity*/ {
        private Paint.FontMetricsInt fontMetrics = null;
        int size = 0;
        int extraHeight = 0;
        int extraDescent = 0;
        boolean fixHeight = true;

        //private static float proportion = 0;
        public EmojiSpan(Drawable d, int verticalAlignment, int s, Paint.FontMetricsInt original) {
            super(d, verticalAlignment);
            init(s, original, false);
        }

        public EmojiSpan(Drawable d, int verticalAlignment, int s, Paint.FontMetricsInt original, boolean applyFix) {
            super(d, verticalAlignment);

            init(s, original, applyFix);
        }

        private void init(int s, Paint.FontMetricsInt original, boolean applyFix) {
            if (applyFix) {
                extraHeight = 5;
                extraDescent = 2;
            }

            size = s;
            fontMetrics = original;
            if (original != null) {
                size = Math.abs(fontMetrics.descent) + Math.abs(fontMetrics.ascent);
                if (size == 0) {
                    size = OSUtilities.dp(20);
                }
            }
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm == null) {
                fm = new Paint.FontMetricsInt();
            }

            if (fontMetrics == null) {
                int sz = super.getSize(paint, text, start, end, fm);
                int offset = OSUtilities.dp(8);
                int w = OSUtilities.dp(10);
                fm.top = -w - offset;
                fm.bottom = w - offset;
                fm.ascent = -w - offset;
                fm.leading = 0;
                fm.descent = w - offset + OSUtilities.dp(extraDescent);
                return sz;
            } else {
                if (fm != null) {
                    fm.ascent = fontMetrics.ascent + OSUtilities.dp(0);
                    fm.descent = fontMetrics.descent + OSUtilities.dp(extraDescent);
                    fm.top = fontMetrics.top + OSUtilities.dp(0);
                    fm.bottom = fontMetrics.bottom + OSUtilities.dp(0);
                }

                if (getDrawable() != null) {
                    getDrawable().setBounds(0, 0, size + OSUtilities.dp(extraHeight), size + OSUtilities.dp(extraHeight));
                }

                return size + OSUtilities.dp(extraHeight);
            }
        }



        /*@Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
             Paint.FontMetricsInt fm) {chooseHeight(text, start, end, spanstartv, v, fm, null);
        }
        public void chooseHeight(CharSequence text, int start, int end,
                                 int spanstartv, int v,
                                 Paint.FontMetricsInt fm, TextPaint paint) {
            if (paint != null) {
                size *= paint.density;
            }

            if (fm.bottom - fm.top < size) {
                fm.top = fm.bottom - size;
                fm.ascent = fm.ascent - size;
            } else {
                if (proportion == 0) {


                    Paint p = new Paint();
                    p.setTextSize(100);
                    Rect r = new Rect();
                    p.getTextBounds("ABCDEFG", 0, 7, r);

                    proportion = (r.top) / p.ascent();
                }

                int need = (int) Math.ceil(-fm.top * proportion);

                if (size - fm.descent >= need) {


                    fm.top = fm.bottom - size;
                    fm.ascent = fm.descent - size;
                } else if (size >= need) {


                    fm.top = fm.ascent = -need;
                    fm.bottom = fm.descent = fm.top + size;
                } else {


                    fm.top = fm.ascent = -size;
                    fm.bottom = fm.descent = 0;
                }
            }
        }*/
    }

    private EmojiManager() {
        init();
    }

    private void init() {
        Utilities.globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {


                    mContext = ApplicationLoader.applicationContext;
                    // to clear recents, for testing
                    // mContext.getSharedPreferences("emoji", 0).edit().putString("recents", "").commit();


                    for (String categoryname : categoriesNames) {
                        EmojiGroup category = new EmojiGroup();
                        category.emojis = new ArrayList<Emoji>();
                        category.category = categoryname;
                        categoriesDict.put(categoryname, category);
                        categories.add(category);
                    }
                    for (String categoryname : stickersCategoriesNames) {
                        StickersGroup category = new StickersGroup();
                        category.stickers = new ArrayList<Sticker>();
                        category.category = categoryname;
                        stickersCategoriesDict.put(categoryname, category);
                        stickersCategories.add(category);
                    }
                    //  addStickersJsonDefinitions("rageface.json", "rageface", "png");
                    //   addStickersJsonDefinitions("onionClub.json", "onionClub", "gif");
                    //  addJsonDefinitions("phantomsmiles.json", "emoji", "png");
                    addJsonDefinitions("yolkssmiles.json", "yolks", "png");
                    addJsonDefinitions("lazycrazysmiles.json", "lazyCrazy2", "png");
                    addJsonDefinitions("theblacysmiles.json", "theBlacy", "png");

                    //emojiGroups  = getEmojiGroups();
                } catch (JsonSyntaxException jse) {
                    FileLog.e("Emoji", "could not parse json", jse);
               /*} catch (IOException fe) {
                    Log.e("Emoji", "could not load emoji definition", fe);*/
                } catch (Exception fe) {
                    Log.e("Emoji", "could not load emoji definition", fe);
                }
            }
        });
    }


    public void addJsonPlugins() throws IOException, JsonSyntaxException {
        PackageManager packageManager = mContext.getPackageManager();
        Intent stickerIntent = new Intent(PLUGIN_CONSTANT);
        List<ResolveInfo> stickerPack = packageManager.queryIntentActivities(stickerIntent, 0);

        for (ResolveInfo ri : stickerPack) {

            try {
                Resources res = packageManager.getResourcesForApplication(ri.activityInfo.applicationInfo);

                String[] files = res.getAssets().list("");

                for (String file : files) {
                    if (file.endsWith(".json"))
                        addJsonDefinitions(file, file.substring(0, file.length() - 5), "png", res);
                }

            } catch (NameNotFoundException e) {
                Log.e("emoji", "unable to find application for emoji plugin");
            }
        }

    }

    public void addJsonDefinitions(String assetPathJson, String basePath, String fileExt) throws IOException, JsonSyntaxException {
        addJsonDefinitions(assetPathJson, basePath, fileExt, mContext.getResources());
    }

    public void addJsonDefinitions(final String assetPathJson, final String basePath, final String fileExt, final Resources res) throws IOException, JsonSyntaxException {

        Gson gson = new Gson();

        Reader reader = null;
        try {
            reader = new InputStreamReader(res.getAssets().open(assetPathJson));
            Type collectionType = new TypeToken<ArrayList<Emoji>>() {
            }.getType();
            Collection<Emoji> emojis = gson.fromJson(reader, collectionType);

            for (Emoji emoji : emojis) {
                //FileLog.e("basePath", "" + basePath + '/' + emoji.name + '.' + fileExt);
                emoji.assetPath = basePath + '/' + emoji.name + '.' + fileExt;
                emoji.res = res;

                try {
                    res.getAssets().open(emoji.assetPath);

                    addPattern(':' + emoji.name + ':', emoji);
                    emoticonsDict.put(':' + emoji.name + ':', emoji);
                    if (emoji.moji != null)
                        addPattern(emoji.moji, emoji);

                    if (emoji.emoticon != null)
                        addPattern(emoji.emoticon, emoji);


                    if (emoji.category != null)
                        addEmojiToCategory(emoji.category, emoji);
                } catch (FileNotFoundException fe) {
                    //should not be added as a valid emoji
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addStickersJsonDefinitions(String assetPathJson, String basePath, String fileExt) throws IOException, JsonSyntaxException {
        addStickersJsonDefinitions(assetPathJson, basePath, fileExt, mContext.getResources());
    }

    public void addStickersJsonDefinitions(final String assetPathJson, final String basePath, final String fileExt, final Resources res) throws IOException, JsonSyntaxException {

        Gson gson = new Gson();

        Reader reader = null;
        try {
            reader = new InputStreamReader(res.getAssets().open(assetPathJson));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Type collectionType = new TypeToken<ArrayList<Sticker>>() {
        }.getType();
        Collection<Sticker> stickers = gson.fromJson(reader, collectionType);

        for (Sticker sticker : stickers) {

            sticker.smallPath = basePath + "/small/" + sticker.name + '.' + fileExt;
            sticker.largePath = basePath + "/large/" + sticker.name + '.' + fileExt;
            sticker.res = res;
            FileLog.e("sticker basePath", "" + basePath + '/' + sticker.name + '.' + fileExt);
            try {
                res.getAssets().open(sticker.smallPath);

                this.stickers.put(':' + sticker.name + ':', sticker);
                StickersGroup stickersGroup = stickersCategoriesDict.get(sticker.category);

                if (stickersGroup == null) {
                    stickersGroup = new StickersGroup();
                    stickersGroup.category = sticker.category;
                    stickersGroup.stickers = new ArrayList<Sticker>();
                }

                stickersGroup.stickers.add(sticker);
                if (!stickersCategoriesDict.containsKey(sticker.category)) {
                    stickersCategories.add(stickersGroup);
                    stickersCategoriesDict.put(sticker.category, stickersGroup);
                }

            } catch (FileNotFoundException fe) {
                //should not be added as a valid emoji
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public Collection<EmojiGroup> getEmojiGroups() {
        return categoriesDict.values();
    }

    public String getAssetPath(Emoji emoji) {
        return emoji.name;
    }

    public synchronized void addEmojiToCategory(String category, Emoji emoji) {
        EmojiGroup emojiGroup = categoriesDict.get(category);

        if (emojiGroup == null) {
            emojiGroup = new EmojiGroup();
            emojiGroup.category = category;
            emojiGroup.emojis = new ArrayList<Emoji>();
        }

        emojiGroup.emojis.add(emoji);
        if (!categoriesDict.containsKey(category)) {
            categories.add(emojiGroup);
            categoriesDict.put(category, emojiGroup);
        }
    }

    public static synchronized EmojiManager getInstance() {

        if (mInstance == null)
            mInstance = new EmojiManager();


        return mInstance;

    }


    private void addPattern(String pattern, Emoji resource) {

        emoticons.put(Pattern.compile(pattern, Pattern.LITERAL), resource);

    }

    private void addPattern(char charPattern, Emoji resource) {

        emoticons.put(Pattern.compile(charPattern + "", Pattern.UNICODE_CASE), resource);
    }

    public CharSequence replaceEmoji(CharSequence cs, Paint.FontMetricsInt fontMetrics, int size) {
        return replaceEmoji(cs, fontMetrics, size, false);
    }

    public CharSequence replaceEmoji(CharSequence cs, Paint.FontMetricsInt fontMetrics, int size, boolean applyFix) {
        if (cs == null || cs.length() == 0) {
            return cs;
        }
        Spannable s;
        SpannableStringBuilder text;
        if (cs instanceof Spannable) {
            s = (Spannable) cs;
        } else {
            s = Spannable.Factory.getInstance().newSpannable(cs);
        }
        for (Entry<Pattern, Emoji> entry : emoticons.entrySet()) {
            Matcher matcher = entry.getKey().matcher(cs);
            while (matcher.find()) {
                boolean set = true;
                for (ImageSpan span : s.getSpans(matcher.start(),
                        matcher.end(), ImageSpan.class))

                    if (s.getSpanStart(span) >= matcher.start()
                            && s.getSpanEnd(span) <= matcher.end())
                        s.removeSpan(span);
                    else {
                        set = false;
                        break;
                    }
                if (set) {
                    Emoji emoji = entry.getValue();
                    Drawable d = getEmojiDrawable(emoji);
                    EmojiSpan span = new EmojiSpan(d, DynamicDrawableSpan.ALIGN_BOTTOM, size, fontMetrics, applyFix);

                    //s.setSpan(new AbsoluteSizeSpan(30) ,matcher.start(), matcher.end(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    s.setSpan(span, matcher.start(), matcher.end(), 0);
                    // text = new SpannableStringBuilder(s);
                    // text.setSpan(new AbsoluteSizeSpan(40), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    //  return text;
                    /* s.setSpan(new ImageSpan(BitmapFactory.decodeStream(emoji.res.getAssets().open(emoji.assetPath))),
                                matcher.start(), matcher.end(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);*/
                }
            }
        }
        return s;
    }

    public static Drawable getEmojiDrawable(Emoji emoji) {
        try {
            return new BitmapDrawable(BitmapFactory.decodeStream(emoji.res.getAssets().open(emoji.assetPath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
