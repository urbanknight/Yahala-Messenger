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
package com.yahala.ImageLoader.model;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.yahala.ImageLoader.loader.util.BitmapDisplayer;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;


public class ImageWrapper {

    private static final String URL_ERROR = "_url_error";

    private final ImageView imageView;

    private String url;
    private String previewUrl;
    private int width;
    private int height;
    private int previewWidth;
    private int previewHeight;
    private int loadingResourceId;
    private int notFoundResourceId;
    private boolean isUseCacheOnly;
    private boolean saveThumbnail;
    private Animation animation;

    public ImageWrapper(ImageView imageView) {
        this.imageView = imageView;
        initWrapper(imageView);
    }

    private void initWrapper(ImageView imageView) {
        ImageTag tag = (ImageTag) imageView.getTag();
        if (tag == null) {
            return;
        }
        this.url = tag.getUrl();
        this.loadingResourceId = tag.getLoadingResourceId();
        this.notFoundResourceId = tag.getNotFoundResourceId();
        this.isUseCacheOnly = tag.isUseOnlyCache();
        this.height = tag.getHeight();
        this.width = tag.getWidth();
        this.previewHeight = tag.getPreviewHeight();
        this.previewWidth = tag.getPreviewWidth();
        this.saveThumbnail = tag.isSaveThumbnail();
        if (notFoundResourceId == 0) {
            this.notFoundResourceId = tag.getLoadingResourceId();
        }
        this.previewUrl = tag.getPreviewUrl();
        this.animation = tag.getAnimation();
    }

    public String getCurrentUrl() {
        ImageTag tag = (ImageTag) imageView.getTag();

        if (tag.getUrl() != null) {
            return tag.getUrl();
        }
        return URL_ERROR;
    }

    public String getUrl() {
        return url;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void runOnUiThread(BitmapDisplayer displayer) {
        Activity a = (Activity) imageView.getContext();
        a.runOnUiThread(displayer);
    }

    public Context getContext() {
        return imageView.getContext();
    }

    public void setBitmap(Bitmap bitmap, boolean animated) {
        imageView.setImageBitmap(bitmap);

        stopExistingAnimation();
        if (animation != null && animated) {
            imageView.startAnimation(animation);
            FileLog.e("build", "AnimationHelper");
        } else {
            if (animated) {
                animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
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

    public void setResourceBitmap(Bitmap resourceAsBitmap) {
        imageView.setImageBitmap(resourceAsBitmap);
    }

    public boolean isCorrectUrl(String url) {
        return url.equals(getUrl());
    }

    public int getLoadingResourceId() {
        return loadingResourceId;
    }

    public int getNotFoundResourceId() {
        return notFoundResourceId;
    }

    public boolean isUrlChanged() {
        return !getUrl().equals(getCurrentUrl());
    }

    public boolean isUseCacheOnly() {
        return isUseCacheOnly;
    }

    public boolean isSaveThumbnail() {
        return saveThumbnail;
    }

    public void setSaveThumbnail(boolean saveThumbnail) {
        this.saveThumbnail = saveThumbnail;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

}
