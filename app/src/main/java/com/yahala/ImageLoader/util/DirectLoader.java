package com.yahala.ImageLoader.util;

import android.graphics.Bitmap;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.bitmap.BitmapUtil;
import com.yahala.ImageLoader.network.NetworkManager;
import com.yahala.ImageLoader.network.UrlNetworkManager;

import java.io.InputStream;

/**
 * Direct loader make use of the NetworkManager and the BitmapUtil
 * to provide a direct way to get a Bitmap given a http url.
 */
public class DirectLoader {

    private NetworkManager networkManager;
    private BitmapUtil bitmapUtil;

    public DirectLoader() {
        this(new UrlNetworkManager(new LoaderSettings()), new BitmapUtil());
    }

    public DirectLoader(NetworkManager networkManager, BitmapUtil bitmapUtil) {
        this.networkManager = networkManager;
        this.bitmapUtil = bitmapUtil;
    }

    public Bitmap download(String url) {
        if (url == null) {
            return null;
        }
        if (url.length() == 0) {
            return null;
        }
        InputStream is = networkManager.retrieveInputStream(url);
        if (is == null) {
            return null;
        }
        return bitmapUtil.decodeInputStream(is);
    }
}
