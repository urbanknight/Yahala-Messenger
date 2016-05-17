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
import android.text.TextUtils;
import android.util.Log;


import com.yahala.ImageLoader.exception.ImageNotFoundException;
import com.yahala.ImageLoader.model.ImageWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public abstract class SingleThreadedLoader {

    private static final String TAG = "ImageLoader";

    private BitmapLoader thread = new BitmapLoader();
    private Stack<ImageWrapper> stack = new Stack<ImageWrapper>();
    private List<String> notFoundImages = new ArrayList<String>();

    public void push(ImageWrapper image) {
        if (TextUtils.isEmpty(image.getUrl())) {
            return;
        }
        pushOnStack(image);
        startThread();
    }

    public int size() {
        synchronized (stack) {
            return stack.size();
        }
    }

    public ImageWrapper pop() {
        synchronized (stack) {
            try {
                return stack.pop();
            } catch (Exception e) {
                return null;
            }
        }
    }

    protected abstract Bitmap loadMissingBitmap(ImageWrapper iw);

    protected abstract void onBitmapLoaded(ImageWrapper iw, Bitmap bmp);

    private void clean(ImageWrapper p) {
        synchronized (stack) {
            for (int j = 0; j < stack.size(); j++) {
                if (stack.get(j).getUrl() != null
                        && stack.get(j).getUrl().equals(p.getUrl())) {
                    stack.remove(j);
                    j--;
                }
            }
        }
    }

    private void pushOnStack(ImageWrapper p) {
        synchronized (stack) {
            stack.push(p);
        }
    }

    private class BitmapLoader extends Thread {
        boolean isWaiting = false;

        public BitmapLoader() {
            setPriority(Thread.NORM_PRIORITY - 1);
        }

        @Override
        public void run() {
            while (true) {
                pauseThreadIfnecessary();
                ImageWrapper image = pop();
                if (image != null) {
                    loadAndShowImage(image);
                }
            }
        }

        private void pauseThreadIfnecessary() {
            if (size() != 0) {
                return;
            }
            synchronized (thread) {
                try {
                    isWaiting = true;
                    wait();
                } catch (Exception e) {
                    Log.v(TAG, "Pausing the thread error " + e.getMessage());
                }
            }
        }

        private void loadAndShowImage(ImageWrapper iw) {
            try {
                if (iw.isUrlChanged()) {
                    return;
                }
                Bitmap bmp = loadMissingBitmap(iw);
                if (bmp == null) {
                    clean(iw);
                    onBitmapLoaded(iw, bmp);
                    return;
                }
                onBitmapLoaded(iw, bmp);
            } catch (ImageNotFoundException inf) {
                notFoundImages.add(iw.getUrl());
            } catch (Throwable e) {
                Log.e(TAG, "Throwable : " + e.getMessage(), e);
            }
        }
    }

    private void startThread() {
        if (thread.getState() == Thread.State.NEW) {
            thread.start();
            return;
        }
        synchronized (thread) {
            if (thread.isWaiting) {
                try {
                    thread.isWaiting = false;
                    thread.notify();
                } catch (Exception ie) {
                    Log.e(TAG, "Check and resume the thread " + ie.getMessage());
                }
            }
        }
    }

}
