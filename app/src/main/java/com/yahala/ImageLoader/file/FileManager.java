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

import java.io.File;

/**
 * FileManager is an interface marking all the implementation of file cache managers.
 * It gives access to the cached files and re-sampled images.
 */
public interface FileManager {

    /**
     * Removes all the files in the cache directory.
     */
    void clean();

    /**
     * Removes the files in the cache directory where the
     * timestamp is older then the expiration time.
     */
    void cleanOldFiles();

    /**
     * Returns the absolute path of the cached content for the given url.
     *
     * @param url original url of the content
     * @return the absolute path as a string or null if there is no cached file
     */
    String getFilePath(String url);

    /**
     * Returns the file handle of the cached content for the given url.
     *
     * @param url original of the content
     * @return file handle of the cached content
     */
    File getFile(String url);

    /**
     * Stores the given bitmap. The width and height of the bitmap are used for naming only.
     * This helper method uses the same naming convention for resized images as getFile(String, width, height)
     *
     * @param fileName Absolute path where the image will be stored.
     * @param b        bitmap that should be stored.
     * @param width    width of the bitmap
     * @param height   height of new bitmap
     */
    void saveBitmap(String fileName, Bitmap b, int width, int height);

    /**
     * Returns the file handle of the cached content for the given url with a specified size.
     * Dimensions are only used for naming. The resized content is not created by calling this method.
     *
     * @param url    original url of the content
     * @param width  width of image
     * @param height height of image
     * @return file handle of cached resized content, must not return null
     */
    File getFile(String url, int width, int height);

}
