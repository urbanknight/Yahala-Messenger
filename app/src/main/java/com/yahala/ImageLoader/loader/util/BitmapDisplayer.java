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
package com.yahala.ImageLoader.loader.util;

import android.graphics.Bitmap;

import com.yahala.ImageLoader.model.ImageWrapper;


public class BitmapDisplayer implements Runnable {
    private Bitmap bitmap;
    private ImageWrapper imageWrapper;

    public BitmapDisplayer(Bitmap bitmap, ImageWrapper imageWrapper) {
        this.bitmap = bitmap;
        this.imageWrapper = imageWrapper;
    }

    public void runOnUiThread() {
        imageWrapper.runOnUiThread(this);
    }

    @Override
    public void run() {
        if (bitmap == null) {
            return;
        }
        if (imageWrapper.isUrlChanged()) {
            return;
        }
        imageWrapper.setBitmap(bitmap, false);
    }
}
