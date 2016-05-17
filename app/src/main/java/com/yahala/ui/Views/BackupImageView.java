/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.TLRPC;

public class BackupImageView extends ImageView {
    public String currentPath;
    boolean makeRequest = true;
    TLRPC.FileLocation last_path;
    String last_httpUrl;
    String last_filter;
    int last_placeholder;
    Bitmap last_placeholderBitmap;
    int last_size;
    private boolean isPlaceholder;
    private boolean ignoreLayout = true;

    public BackupImageView(android.content.Context context) {
        super(context);

    }

    public BackupImageView(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public BackupImageView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setImage(TLRPC.FileLocation path, String filter, int placeholder) {
        setImage(path, null, filter, placeholder, null, 0);
    }

    public void setImage(Bitmap image) {
        if (image != null) {

            // Bitmap img=Bitmap.createScaledBitmap(image, 50, 50, false);
            Bitmap img = getResizedBitmap(image, 50, 50);
            //  setScaleType(ScaleType.FIT_XY);
            // setAdjustViewBounds(true);
            super.setImageBitmap(img);
        } else {

            // File imgFile = new  File("/sdcard/Images/test_image.jpg");
            // if(imgFile.exists()){

            // Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            //ImageView myImage = (ImageView) findViewById(R.id.);

            //if this is used frequently, may handle bitmaps explicitly
            //to reduce the intermediate drawable object
            // setImageDrawable(new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), bitmap));

            Drawable drawable = getResources().getDrawable(R.drawable.user_placeholder);
            super.setImageDrawable(drawable);

            // }
        }

    }


    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        Bitmap dstBmp = null;
        if (bm.getWidth() >= bm.getHeight()) {

            dstBmp = Bitmap.createBitmap(
                    bm,
                    bm.getWidth() / 2 - bm.getHeight() / 2,
                    0,
                    bm.getHeight(),
                    bm.getHeight()
            );

        } else {

            dstBmp = Bitmap.createBitmap(
                    bm,
                    0,
                    bm.getHeight() / 2 - bm.getWidth() / 2,
                    bm.getWidth(),
                    bm.getWidth()
            );
        }
        return dstBmp;
    }


    public void setImage(TLRPC.FileLocation path, String filter, Bitmap placeholderBitmap) {
        setImage(path, null, filter, 0, placeholderBitmap, 0);
    }

    public void setImage(TLRPC.FileLocation path, String filter, int placeholder, int size) {
        setImage(path, null, filter, placeholder, null, size);
    }

    public void setImage(TLRPC.FileLocation path, String filter, Bitmap placeholderBitmap, int size) {
        setImage(path, null, filter, 0, placeholderBitmap, size);
    }

    public void setImage(String path, String filter, int placeholder) {
        setImage(null, path, filter, placeholder, null, 0);
    }

    public void setImage(TLRPC.FileLocation path, String httpUrl, String filter, int placeholder, Bitmap placeholderBitmap, int size) {

    }

    public void setImageBitmap(Bitmap bitmap, String imgKey) {
        if (currentPath == null || !imgKey.equals(currentPath)) {
            return;
        }
        isPlaceholder = false;

        if (ignoreLayout) {
            makeRequest = false;
        }
        super.setImageBitmap(bitmap);
        if (ignoreLayout) {
            makeRequest = true;
        }
    }

    public void clearImage() {
        recycleBitmap(null);
    }

    private void recycleBitmap(Bitmap newBitmap) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Exception e) {

            FileLog.e("tmessages", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recycleBitmap(null);
        super.finalize();
    }

    public void setImageResourceMy(int resId) {
        if (ignoreLayout) {
            makeRequest = false;
        }
        super.setImageResource(resId);
        if (ignoreLayout) {
            makeRequest = true;
        }
    }

    public void setImageResource(int resId) {
        if (resId != 0) {
            recycleBitmap(null);
        }
        currentPath = null;
        last_path = null;
        last_httpUrl = null;
        last_filter = null;
        last_placeholder = 0;
        last_size = 0;
        last_placeholderBitmap = null;
        if (ignoreLayout) {
            makeRequest = false;
        }
        super.setImageResource(resId);
        if (ignoreLayout) {
            makeRequest = true;
        }
    }

    public void setImageBitmapMy(Bitmap bitmap) {
        if (ignoreLayout) {
            makeRequest = false;
        }
        super.setImageBitmap(bitmap);
        if (ignoreLayout) {
            makeRequest = true;
        }
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            recycleBitmap(null);
        }
        currentPath = null;
        last_path = null;
        last_httpUrl = null;
        last_filter = null;
        last_placeholder = 0;
        last_size = 0;
        last_placeholderBitmap = null;
        if (ignoreLayout) {
            makeRequest = false;
        }
        super.setImageBitmap(bitmap);
        if (ignoreLayout) {
            makeRequest = true;
        }
    }

    @Override
    public void requestLayout() {
        if (makeRequest) {
            super.requestLayout();
        }
    }
}
