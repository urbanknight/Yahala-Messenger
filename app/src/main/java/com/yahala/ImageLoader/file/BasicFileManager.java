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
package com.yahala.ImageLoader.file;

import android.graphics.Bitmap;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.file.util.FileUtil;
import com.yahala.ImageLoader.network.UrlUtil;
import com.yahala.ImageLoader.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * This is a basic implementation for the file manager.
 * On Startup it is running a cleanup of all the files in the cache, and removing
 * old images based on the expirationPeriod.
 */
public class BasicFileManager implements FileManager {

    private LoaderSettings loaderSettings;

    public BasicFileManager(LoaderSettings settings) {
        this.loaderSettings = settings;
        if (settings.isCleanOnSetup()) {
            cleanOldFiles();
        }
    }

    /**
     * Clean is removing all the files in the cache directory.
     */
    @Override
    public void clean() {
        deleteOldFiles(-1);
    }

    /**
     * CleanOldFile is removing all the files in the cache directory where the
     * timestamp is older then the expiration time.
     */
    @Override
    public void cleanOldFiles() {
        deleteOldFiles(loaderSettings.getExpirationPeriod());
    }

    @Override
    public String getFilePath(String imageUrl) {
        File f = getFile(imageUrl);
        if (f.exists()) {
            return f.getAbsolutePath();
        }
        return null;
    }

    @Override
    public void saveBitmap(String fileName, Bitmap b, int width, int height) {
        try {
            FileOutputStream out = new FileOutputStream(fileName + "-" + width + "x" + height);
            b.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            Log.warning("" + e.getMessage());
        }
    }

    @Override
    public File getFile(String url) {
        url = processUrl(url);
        String filename = String.valueOf(url.hashCode());
        return new File(loaderSettings.getCacheDir(), filename);
    }

    @Override
    public File getFile(String url, int width, int height) {
        url = processUrl(url);
        String filename = url.hashCode() + "-" + width + "x" + height;
        return new File(loaderSettings.getCacheDir(), filename);
    }

    private String processUrl(String url) {
        if (loaderSettings.isQueryIncludedInHash()) {
            return url;
        }
        return new UrlUtil().removeQuery(url);
    }

    private void deleteOldFiles(final long expirationPeriod) {
        final String cacheDir = loaderSettings.getCacheDir().getAbsolutePath();
        Thread cleaner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new FileUtil().reduceFileCache(cacheDir, expirationPeriod);
                } catch (Throwable t) {
                    // Don't have to fail in case there
                }
            }
        });
        cleaner.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        cleaner.start();
    }

}
