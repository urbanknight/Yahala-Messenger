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
import android.widget.ImageView;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.OnImageLoadedListener;
import com.yahala.ImageLoader.exception.ImageNotFoundException;
import com.yahala.ImageLoader.loader.util.BitmapDisplayer;
import com.yahala.ImageLoader.loader.util.BitmapRetriever;
import com.yahala.ImageLoader.loader.util.SingleThreadedLoader;
import com.yahala.ImageLoader.model.ImageWrapper;

import java.io.File;
import java.lang.ref.WeakReference;

public class SimpleLoader implements Loader {

    private LoaderSettings loaderSettings;
    private SingleThreadedLoader singleThreadedLoader;
    private WeakReference<OnImageLoadedListener> onImageLoadedListener;

    public SimpleLoader(LoaderSettings loaderSettings) {
        this.loaderSettings = loaderSettings;
        this.singleThreadedLoader = new SingleThreadedLoader() {
            @Override
            protected Bitmap loadMissingBitmap(ImageWrapper iw) {
                return getBitmap(iw.getUrl(), iw.getWidth(), iw.getHeight(), iw.getImageView());
            }

            @Override
            protected void onBitmapLoaded(ImageWrapper iw, Bitmap bmp) {
                new BitmapDisplayer(bmp, iw).runOnUiThread();
                SimpleLoader.this.loaderSettings.getCacheManager().put(iw.getUrl(), bmp);
                onImageLoaded(iw.getImageView());
            }
        };
    }

    private void onImageLoaded(ImageView imageView) {
        if (onImageLoadedListener != null) {
            onImageLoadedListener.get().onImageLoaded(imageView);
        }
    }

    @Override
    public void load(ImageView imageView) {
        ImageWrapper w = new ImageWrapper(imageView);

        try {
            Bitmap b = loaderSettings.getCacheManager().get(w.getUrl(), w.getWidth(), w.getHeight());
            if (b != null && !b.isRecycled()) {
                w.setBitmap(b, false);
                return;
            }
            String thumbUrl = w.getPreviewUrl();
            if (thumbUrl != null) {
                b = loaderSettings.getCacheManager().get(thumbUrl, w.getPreviewHeight(), w.getPreviewWidth());
                if (b != null && !b.isRecycled()) {
                    w.setBitmap(b, false);
                } else {
                    setResource(w, w.getLoadingResourceId());
                }
            } else {
                setResource(w, w.getLoadingResourceId());
            }
            if (w.isUseCacheOnly()) {
                return;
            }
            singleThreadedLoader.push(w);
        } catch (ImageNotFoundException inf) {
            setResource(w, w.getNotFoundResourceId());
        } catch (Throwable t) {
            setResource(w, w.getNotFoundResourceId());
        }
    }

    @Override
    public void setLoadListener(WeakReference<OnImageLoadedListener> onImageLoadedListener) {
        this.onImageLoadedListener = onImageLoadedListener;
    }

    private Bitmap getBitmap(String url, int width, int height, ImageView imageView) {
        if (url != null && url.length() >= 0) {
            File f = loaderSettings.getFileManager().getFile(url);
            BitmapRetriever retriever = new BitmapRetriever(url, f, width, height, 0, false, true, imageView, loaderSettings, null);
            Bitmap b = retriever.getBitmap();
            return b;
        }
        return null;
    }

    private void setResource(ImageWrapper w, int resId) {
        Bitmap b = loaderSettings.getResCacheManager().get("" + resId, w.getWidth(), w.getHeight());
        if (b != null) {
            w.setBitmap(b, false);
            return;
        }
        b = loaderSettings.getBitmapUtil().decodeResourceBitmapAndScale(w, resId, loaderSettings.isAllowUpsampling());
        loaderSettings.getResCacheManager().put("" + resId, b);
        w.setBitmap(b, false);
    }

}
