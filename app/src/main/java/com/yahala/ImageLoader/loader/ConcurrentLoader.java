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
package com.yahala.ImageLoader.loader;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.OnImageLoadedListener;
import com.yahala.ImageLoader.exception.ImageNotFoundException;
import com.yahala.ImageLoader.loader.util.LoaderTask;
import com.yahala.ImageLoader.model.ImageWrapper;

import java.lang.ref.WeakReference;

public class ConcurrentLoader implements Loader {

    private final LoaderSettings loaderSettings;
    private WeakReference<OnImageLoadedListener> onImageLoadedListener;

    public ConcurrentLoader(LoaderSettings loaderSettings) {
        this.loaderSettings = loaderSettings;
    }

    @Override
    public void load(ImageView imageView) {
        if (!isValidImageView(imageView)) {
            Log.w("ImageLoader", "You should never call load if you don't set a ImageTag on the view");
            return;
        }
        loadBitmap(new ImageWrapper(imageView));
    }

    private boolean isValidImageView(ImageView imageView) {
        return imageView.getTag() != null;
    }

    private synchronized void loadBitmap(ImageWrapper w) {
        if (isBitmapAlreadyInCache(getCachedBitmap(w))) {
            Bitmap cachedBitmap = getCachedBitmap(w);
            w.setBitmap(cachedBitmap, false);
            return;
        }
        setDefaultImage(w);
        if (!w.isUseCacheOnly()) {
            startTask(w);
        }
    }

    private boolean isBitmapAlreadyInCache(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }

    private Bitmap getCachedBitmap(ImageWrapper w) {
        return loaderSettings.getCacheManager().get(w.getUrl(), w.getHeight(), w.getWidth());
    }

    private void setDefaultImage(ImageWrapper w) {
        if (hasPreviewUrl(w.getPreviewUrl())) {
            if (isBitmapAlreadyInCache(getPreviewCachedBitmap(w))) {
                w.setBitmap(getPreviewCachedBitmap(w), false);
            } else {
                w.setResourceBitmap(getResourceAsBitmap(w, w.getLoadingResourceId()));
            }
        } else {
            w.setResourceBitmap(getResourceAsBitmap(w, w.getLoadingResourceId()));
        }
    }

    private Bitmap getPreviewCachedBitmap(ImageWrapper w) {
        return loaderSettings.getCacheManager().get(w.getPreviewUrl(), w.getPreviewHeight(), w.getPreviewWidth());
    }

    private void startTask(ImageWrapper w) {
        try {
            LoaderTask task = createTask(w);
            task.execute();
        } catch (ImageNotFoundException inf) {
            w.setResourceBitmap(getResourceAsBitmap(w, w.getNotFoundResourceId()));
        } catch (Throwable t) {
            w.setResourceBitmap(getResourceAsBitmap(w, w.getNotFoundResourceId()));
        }
    }

    private Bitmap getResourceAsBitmap(ImageWrapper w, int resId) {
        Bitmap b = loaderSettings.getResCacheManager().get("" + resId, w.getWidth(), w.getHeight());
        if (b != null) {
            return b;
        }
        b = loaderSettings.getBitmapUtil().decodeResourceBitmapAndScale(w, resId, loaderSettings.isAllowUpsampling());
        loaderSettings.getResCacheManager().put(String.valueOf(resId), b);
        return b;
    }

    private boolean hasPreviewUrl(String previewUrl) {
        return previewUrl != null;
    }

    private LoaderTask createTask(ImageWrapper imageWrapper) {
        return onImageLoadedListener == null ? new LoaderTask(imageWrapper, loaderSettings)
                : new LoaderTask(imageWrapper, loaderSettings, onImageLoadedListener);
    }

    @Override
    public void setLoadListener(WeakReference<OnImageLoadedListener> onImageLoadedListener) {
        this.onImageLoadedListener = onImageLoadedListener;
    }

}
