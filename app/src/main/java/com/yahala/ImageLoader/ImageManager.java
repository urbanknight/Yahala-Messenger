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
package com.yahala.ImageLoader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;


import com.yahala.ImageLoader.cache.CacheManager;
import com.yahala.ImageLoader.exception.ImageNotFoundException;
import com.yahala.ImageLoader.file.FileManager;
import com.yahala.ImageLoader.loader.Loader;
import com.yahala.ImageLoader.network.NetworkManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * ImageManager has the responsibility to provide a
 * simple and easy interface to access three fundamental part of the imageLoader
 * library : the FileManager, the NetworkManager, and the CacheManager.
 * An ImageManager instance can be instantiated at the application level and used
 * statically across the application.
 * <p/>
 * Manifest.permission.WRITE_EXTERNAL_STORAGE and Manifest.permission.INTERNET are
 * currently necessary for the imageLoader library to work properly.
 */
public class ImageManager {

    private final LoaderSettings loaderSettings;
    private final Map<Integer, WeakReference<OnImageLoadedListener>> onImageLoadedListeners;

    public ImageManager(LoaderSettings settings) {
        this(null, settings);
    }

    public ImageManager(Context context, LoaderSettings settings) {
        if (context != null) {
            verifyPermissions(context);
        }
        this.loaderSettings = settings;
        onImageLoadedListeners = new HashMap<Integer, WeakReference<OnImageLoadedListener>>();
    }

    private void verifyPermissions(Context context) {
        verifyPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        verifyPermission(context, Manifest.permission.INTERNET);
    }

    private void verifyPermission(Context c, String permission) {
        int p = c.getPackageManager().checkPermission(permission, c.getPackageName());
        if (p == PackageManager.PERMISSION_DENIED) {
            throw new RuntimeException("ImageLoader : please add the permission " + permission + " to the manifest");
        }
    }

    public Loader getLoader() {
        return loaderSettings.getLoader();
    }

    public FileManager getFileManager() {
        return loaderSettings.getFileManager();
    }

    public NetworkManager getNetworkManager() {
        return loaderSettings.getNetworkManager();
    }

    public CacheManager getCacheManager() {
        return loaderSettings.getCacheManager();
    }

    public void setOnImageLoadedListener(OnImageLoadedListener listener) {
        onImageLoadedListeners.put(listener.hashCode(), new WeakReference<OnImageLoadedListener>(listener));
        loaderSettings.getLoader().setLoadListener(onImageLoadedListeners.get(listener.hashCode()));
    }

    public void unRegisterOnImageLoadedListener(OnImageLoadedListener listener) {
        onImageLoadedListeners.remove(listener.hashCode());
    }

    /**
     * Loads an image into the cache, it does not bind the image to any view.
     * This method can be used for pre-fetching images.
     * If the image is already cached, the image is not fetched from the net.
     * <p/>
     * This method runs in the same thread as the caller method.
     * Hence, make sure that this method is not called from the main thread.
     * <p/>
     * If the image could be retrieved and decoded the resulting bitmap is cached.
     *
     * @param url Url of image to be pre-fetched
     * @width size of the cached image
     * @height size of the cached image
     */
    public void cacheImage(String url, int width, int height) {
        Bitmap bm = loaderSettings.getCacheManager().get(url, width, height);
        if (bm == null) {

            try {
                File imageFile = loaderSettings.getFileManager().getFile(url, width, height);
                if (!imageFile.exists()) {
                    loaderSettings.getNetworkManager().retrieveImage(url, imageFile);
                }
                Bitmap b;
                if (loaderSettings.isAlwaysUseOriginalSize()) {
                    b = loaderSettings.getBitmapUtil().decodeFile(imageFile, width, height);
                } else {
                    b = loaderSettings.getBitmapUtil().decodeFileAndScale(imageFile, width, height, loaderSettings.isAllowUpsampling());
                }

                if (b != null) {
                    loaderSettings.getCacheManager().put(url, b);
                }
            } catch (ImageNotFoundException inf) {
                // no-op
                inf.printStackTrace();
            }

        }
    }

}
