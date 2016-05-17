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

/**
 * Interface for all the in memory cache managers.
 * There are three main type of memory cache managers :
 * NoChace, SoftMapCache, and LruBitmapCache
 */
public interface CacheManager {

    Bitmap get(String url, int width, int height);

    void put(String url, Bitmap bmp);

    void remove(String url);

    void clean();

}
