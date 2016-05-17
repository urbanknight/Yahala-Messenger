/**
 * Copyright 2012 Novoda Ltd
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahala.ImageLoader.loader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.widget.ImageView;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.OnImageLoadedListener;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.ImageLoader.model.ImageWrapper;

import java.io.File;
import java.lang.ref.WeakReference;

public class LoaderTask extends AsyncTask<String, Void, Bitmap> {

    private final LoaderSettings loaderSettings;
    private final WeakReference<OnImageLoadedListener> onImageLoadedListener;
    private String url;
    private boolean saveScaledImage;
    private boolean useCacheOnly;
    private int width;
    private int height;
    private int notFoundResourceId;
    private ImageView imageView;
    private Context context;
    private File imageFile;
    private Animation animation;

    public LoaderTask(ImageWrapper imageWrapper, LoaderSettings loaderSettings) {
        this(imageWrapper, loaderSettings, null);
    }

    public LoaderTask(ImageWrapper imageWrapper, LoaderSettings loaderSettings, WeakReference<OnImageLoadedListener> onImageLoadedListener) {
        this.loaderSettings = loaderSettings;
        this.onImageLoadedListener = onImageLoadedListener;
        if (imageWrapper != null) {
            extractWrapperData(imageWrapper);
        }
    }

    @Override
    protected Bitmap doInBackground(String... args) {
        if (isCancelled()) {
            return null;
        }

        BitmapRetriever imageRetriever = new BitmapRetriever(url, imageFile, width, height, notFoundResourceId, useCacheOnly, saveScaledImage, imageView, loaderSettings, context);
        return imageRetriever.getBitmap();
    }


    private void extractWrapperData(ImageWrapper imageWrapper) {
        url = imageWrapper.getUrl();
        width = imageWrapper.getWidth();
        height = imageWrapper.getHeight();
        notFoundResourceId = imageWrapper.getNotFoundResourceId();
        useCacheOnly = imageWrapper.isUseCacheOnly();
        imageView = imageWrapper.getImageView();
        context = imageWrapper.getContext();
        imageFile = getImageFile(imageWrapper);
        animation = imageView.getAnimation();
    }

    private File getImageFile(ImageWrapper imageWrapper) {
        File imageFile = null;
        if (imageWrapper.isSaveThumbnail()) {
            imageFile = loaderSettings.getFileManager().getFile(url, width, height);
        }
        if (imageFile == null || !imageFile.exists()) {
            imageFile = loaderSettings.getFileManager().getFile(url);
            if (imageWrapper.isSaveThumbnail()) {
                saveScaledImage = true;
            }
        }
        return imageFile;
    }

    private boolean hasImageViewUrlChanged() {
        if (url == null) {
            return false;
        } else {
            return !url.equals(((ImageTag) imageView.getTag()).getUrl());
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (isCancelled()) {
            bitmap.recycle();
            return;
        }
        if (!hasImageViewUrlChanged()) {
            listenerCallback();
            imageView.setImageBitmap(bitmap);
            stopExistingAnimation();
            if (animation != null) {
                imageView.startAnimation(animation);
            }
        }
    }

    private void stopExistingAnimation() {
        Animation old = imageView.getAnimation();
        if (old != null && !old.hasEnded()) {
            old.cancel();
        }
    }

    private void listenerCallback() {
        if (onImageLoadedListener != null && onImageLoadedListener.get() != null) {
            onImageLoadedListener.get().onImageLoaded(imageView);
        }
    }

    public String getUrl() {
        return url;
    }

}
