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

import android.view.animation.Animation;

/**
 * Model class for information of an image. The model is attached via the tag property of the ImageView.
 *
 * @author Novoda
 */
public class ImageTag {

    private String url;
    private String previewUrl;
    private final int loadingResourceId;
    private final int notFoundResourceId;
    private final int height;
    private final int width;

    private int previewHeight;
    private int previewWidth;
    private boolean useOnlyCache;
    private boolean saveThumbnail;
    private Animation animation;
    private String description;

    public ImageTag(String url, int loadingResourceId, int notFoundResourceId, int width, int height) {
        this.url = url;
        this.loadingResourceId = loadingResourceId;
        this.notFoundResourceId = notFoundResourceId;
        this.width = width;
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public int getNotFoundResourceId() {
        return notFoundResourceId;
    }

    public int getLoadingResourceId() {
        return loadingResourceId;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isUseOnlyCache() {
        return useOnlyCache;
    }

    public void setUseOnlyCache(boolean useOnlyCache) {
        this.useOnlyCache = useOnlyCache;
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

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
