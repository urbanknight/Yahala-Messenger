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
package com.yahala.ImageLoader.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.yahala.ImageLoader.cache.util.LruCache;


/**
 * LruBitmapCache overcome the issue with soft reference cache.
 * It is in fact keeping all the certain amount of images in memory.
 * The size of the memory used for cache depends on the memory that the android
 * SDK provide to the application and the percentage specified (default percentage is 25%).
 */
public class LruBitmapCache implements CacheManager {

    public static final int DEFAULT_MEMORY_CACHE_PERCENTAGE = 25;
    private static final int DEFAULT_MEMORY_CAPACITY_FOR_DEVICES_OLDER_THAN_API_LEVEL_4 = 12;
    private LruCache<String, Bitmap> cache;
    private int capacity;

    /**
     * It is possible to set a specific percentage of memory to be used only for images.
     *
     * @param context
     * @param percentageOfMemoryForCache 1-80
     */
    public LruBitmapCache(Context context, int percentageOfMemoryForCache) {
        int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        this.capacity = calculateCacheSize(memClass, percentageOfMemoryForCache);
        reset();
    }

    /**
     * Setting the default memory size to 25% percent of the total memory
     * available of the application.
     *
     * @param context
     */
    public LruBitmapCache(Context context) {
        this(context, DEFAULT_MEMORY_CACHE_PERCENTAGE);
    }

    /**
     * Empty constructor for testing purposes
     */
    protected LruBitmapCache() {
    }

    private void reset() {
        if (cache != null) {
            cache.evictAll();
        }
        cache = new LruCache<String, Bitmap>(capacity) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    @Override
    public Bitmap get(String url, int width, int height) {
        return cache.get(url);
    }

    @Override
    public void put(String url, Bitmap bmp) {
        cache.put(url, bmp);
    }

    @Override
    public void remove(String url) {
        cache.remove(url);
    }

    @Override
    public void clean() {
        reset();
    }

    public int calculateCacheSize(int memClass, int percentageOfMemoryForCache) {
        if (memClass == 0) {
            memClass = DEFAULT_MEMORY_CAPACITY_FOR_DEVICES_OLDER_THAN_API_LEVEL_4;
        }
        if (percentageOfMemoryForCache < 0) {
            percentageOfMemoryForCache = 0;
        }
        if (percentageOfMemoryForCache > 81) {
            percentageOfMemoryForCache = 80;
        }
        int capacity = (int) ((memClass * percentageOfMemoryForCache * 1024L * 1024L) / 100L);
        if (capacity <= 0) {
            capacity = 1024 * 1024 * 4;
        }

        return capacity;
    }
}
