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

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.yahala.ImageLoader.util.AnimationHelper;
import com.yahala.messenger.FileLog;


public final class ImageTagFactory {

    private int previewImageWidth;
    private int previewImageHeight;
    private int width;
    private int height;
    private int defaultImageResId;
    private int errorImageResId;
    private boolean useOnlyCache;
    private boolean saveThumbnail;
    private boolean useSameUrlForPreviewImage;
    private int animationRes = AnimationHelper.ANIMATION_DISABLED;
    private String description;

    private ImageTagFactory() {
    }

    /**
     * Use newInstance instead
     *
     * @return
     */
    @Deprecated
    public static ImageTagFactory getInstance() {
        return newInstance();
    }

    /**
     * Use newInstance instead.
     * private
     *
     * @param width
     * @param height
     * @param defaultImageResId
     * @return
     */
    @Deprecated
    public static ImageTagFactory getInstance(int width, int height, int defaultImageResId) {
        return newInstance(width, height, defaultImageResId);
    }

    /**
     * Use newInstance instead.
     *
     * @param context
     * @param defaultImageResId
     * @return
     */
    @Deprecated
    public static ImageTagFactory getInstance(Context context, int defaultImageResId) {
        return newInstance(context, defaultImageResId);
    }

    /**
     * Creates a new ImageTagFactory without any further initialization.
     *
     * @return
     */
    public static ImageTagFactory newInstance() {
        return new ImageTagFactory();
    }

    /**
     * Creates a new ImageTagFactory using the given size and default placeholder image for all ImageTags.
     *
     * @param width             width of the image to be shown.
     * @param height            height of the image to be shown.
     * @param defaultImageResId resource id of an placeholder image to be used while the original image is loaded or as an error image.
     * @return
     */
    public static ImageTagFactory newInstance(int width, int height, int defaultImageResId) {
        ImageTagFactory imageTagFactory = new ImageTagFactory();
        imageTagFactory.setInitialSizeParams(imageTagFactory, width, height);
        imageTagFactory.setInitialImageId(imageTagFactory, defaultImageResId);
        return imageTagFactory;
    }

    /**
     * Creates a new ImageTagFactory using the size of device display and the default placeholder image for all ImageTags.
     *
     * @param context           Context used to access the device display
     * @param defaultImageResId resource id of an placeholder image to be used while the original image is loaded  or as an error image.
     * @return
     */
    public static ImageTagFactory newInstance(Context context, int defaultImageResId) {
        ImageTagFactory imageTagFactory = new ImageTagFactory();
        Display display = imageTagFactory.prepareDisplay(context);
        imageTagFactory.setInitialSizeParams(imageTagFactory, display.getWidth(), display.getHeight());
        imageTagFactory.setInitialImageId(imageTagFactory, defaultImageResId);
        return imageTagFactory;
    }

    private ImageTagFactory setInitialSizeParams(ImageTagFactory imageTagFactory, int width, int height) {
        imageTagFactory.setWidth(width);
        imageTagFactory.setHeight(height);
        return imageTagFactory;
    }

    private ImageTagFactory setInitialImageId(ImageTagFactory imageTagFactory, int defaultImageResId) {
        imageTagFactory.setDefaultImageResId(defaultImageResId);
        imageTagFactory.setErrorImageId(defaultImageResId);
        return imageTagFactory;
    }

    private Display prepareDisplay(Context context) {
        Display d = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics(dm);
        return d;
    }

    /**
     * Sets the default image to be used while the original image is loaded.
     * Affects ImageWrapper.loadingResourceId
     *
     * @param defaultImageResId resource id of an placeholder image
     */
    public void setDefaultImageResId(int defaultImageResId) {
        this.defaultImageResId = defaultImageResId;
    }

    /**
     * Set the width of the images
     * Affects directly ImageWrapper.width
     *
     * @param width width of the original image
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Set the height of the images
     * Affects directly ImageWrapper.height
     *
     * @param height height of the original image
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Sets the error image to be used when an error (e.g. network error) occurred during downloading the image.
     * Affects ImageWrapper.notFoundResourceId
     *
     * @param errorImageResId resource id of an placeholder image.
     */
    public void setErrorImageId(int errorImageResId) {
        this.errorImageResId = errorImageResId;
    }

    /**
     * Prepares this factory for using preview images (thumbnails).
     * Affects directly ImageWrapper.previewHeight, ImageWrapper.previewWidth and indirectly ImageWrapper.previewUrl
     *
     * @param previewImageWidth         width of the preview image (thumbnail)
     * @param previewImageHeight        height of the preview image (thumbnail)
     * @param useSameUrlForPreviewImage if true ImageTags are built with the same url for preview images as for the original images.
     */
    public void usePreviewImage(int previewImageWidth, int previewImageHeight, boolean useSameUrlForPreviewImage) {
        this.previewImageWidth = previewImageWidth;
        this.previewImageHeight = previewImageHeight;
        this.useSameUrlForPreviewImage = useSameUrlForPreviewImage;
    }

    /**
     * Sets a flag indicating whether only the cache should be used for image retrieval.
     * Affects directly ImageWrapper.useOnlyCache
     * <p/>
     * If true the Loader must not start downloading the image.
     * It depends on the loader what image is shown if the original image was not found in the cache.
     *
     * @param useOnlyCache if true only image cache is used.
     */
    public void setUseOnlyCache(boolean useOnlyCache) {
        this.useOnlyCache = useOnlyCache;
    }

    /**
     * Sets a flag indicating whether scaled images should be stored as file.
     * Affects directly ImageWrapper.saveThumbnail
     * <p/>
     * If true the image is scaled and stored as file and the image is loaded from the file system first if not found in the cache.
     *
     * @param saveThumbnail if true the image is scaled and saved
     */
    public void setSaveThumbnail(boolean saveThumbnail) {
        this.saveThumbnail = saveThumbnail;
    }

    public void setAnimation(int animationRes) {
        this.animationRes = animationRes;
    }

    public void setDescription(String description) {
        this.description = description;

    }

    /**
     * Creates a new ImageTag for the given image url. It uses the previously set parameters.
     * <p/>
     * If useSameUrlForPreviewImage is set to false the preview url has to be set after building the ImageTag.
     *
     * @param url url of original image to be shown in a ImageView
     * @return an ImageTag to be used as tag property of the ImageView.
     */
    public ImageTag build(String url, Context context) {
        return build(url, new AnimationHelper(context));
    }

    ImageTag build(String url, AnimationHelper animationHelper) {

        checkValidTagParameters();
        ImageTag it = new ImageTag(url, defaultImageResId, errorImageResId, width, height);
        it.setUseOnlyCache(useOnlyCache);
        it.setSaveThumbnail(saveThumbnail);
        if (useSameUrlForPreviewImage) {
            it.setPreviewUrl(url);
        }
        it.setPreviewHeight(previewImageHeight);
        it.setPreviewWidth(previewImageWidth);
        it.setDescription(description);
        setTagAnimation(animationHelper, it);

        return it;
    }

    private void setTagAnimation(AnimationHelper animationHelper, ImageTag it) {
        if (animationRes != AnimationHelper.ANIMATION_DISABLED) {
            it.setAnimation(animationHelper.loadAnimation(animationRes));
        }
    }

    private void checkValidTagParameters() {
        if (width == 0 || height == 0) {
            throw new RuntimeException("width or height was not set before calling build()");
        }
    }
}
