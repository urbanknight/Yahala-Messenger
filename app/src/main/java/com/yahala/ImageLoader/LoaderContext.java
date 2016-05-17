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


import com.yahala.ImageLoader.bitmap.BitmapUtil;
import com.yahala.ImageLoader.cache.CacheManager;
import com.yahala.ImageLoader.file.FileManager;
import com.yahala.ImageLoader.network.NetworkManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * LoaderContext provides a generic context for the imageLoader where different objects can access different levels of caching, the BitmapUtil, and all the
 * customized settings.
 * <p/>
 * This class is supposed to be used internally.
 */
@Deprecated
public class LoaderContext {
    private FileManager fileManager;
    private NetworkManager networkManager;
    private CacheManager cache;
    private CacheManager resBitmapCache;
    private LoaderSettings settings;
    private final BitmapUtil bitmapUtil = new BitmapUtil();
    private final Map<Integer, WeakReference<OnImageLoadedListener>> weakListeners;
    private int listenerKey;

    public LoaderContext() {
        weakListeners = new HashMap<Integer, WeakReference<OnImageLoadedListener>>();
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public LoaderSettings getSettings() {
        return settings;
    }

    public void setSettings(LoaderSettings settings) {
        this.settings = settings;
    }

    public CacheManager getResBitmapCache() {
        return resBitmapCache;
    }

    public void setResBitmapCache(CacheManager resBitmapCache) {
        this.resBitmapCache = resBitmapCache;
    }

    public CacheManager getCache() {
        return cache;
    }

    public void setCache(CacheManager cache) {
        this.cache = cache;
    }

    public BitmapUtil getBitmapUtil() {
        return bitmapUtil;
    }

    public void setListener(OnImageLoadedListener listener) {
        listenerKey = listener.hashCode();
        WeakReference<OnImageLoadedListener> weakReference = new WeakReference<OnImageLoadedListener>(listener);
        weakListeners.put(listenerKey, weakReference);
    }

    public WeakReference<OnImageLoadedListener> getListener() {
        return weakListeners.get(listenerKey);
    }

    public void removeOnImageLoadedListener(int listenerKey) {
        weakListeners.remove(listenerKey);
    }

}
