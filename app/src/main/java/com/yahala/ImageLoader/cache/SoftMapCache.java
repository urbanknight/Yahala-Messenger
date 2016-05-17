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

import android.graphics.Bitmap;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Very simple version of memory cache using soft reference.
 * Soft Reference do not provide the best solution for memory cache in android.
 * This because the garbage collector is removing very quickly soft referenced objects.
 */
public class SoftMapCache implements CacheManager {

    private Map<String, SoftReference<Bitmap>> cache = new HashMap<String, SoftReference<Bitmap>>();

    @Override
    public Bitmap get(String url, int width, int height) {
        SoftReference<Bitmap> bmpr = cache.get(url);
        if (bmpr == null) {
            return null;
        }
        return bmpr.get();
    }

    @Override
    public void put(String url, Bitmap bmp) {
        cache.put(url, new SoftReference<Bitmap>(bmp));
    }

    @Override
    public void remove(String url) {
        cache.remove(url);
    }

    @Override
    public void clean() {
        cache.clear();
    }

}
