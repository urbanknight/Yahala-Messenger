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
package com.yahala.ImageLoader.file.util;

import android.content.Context;

import java.io.File;

/**
 * Internal class to abstract the dependency to specific implementation
 * of android functionalities.
 * This class is for internal usage of the imageLoader.
 */
public class AndroidFileContext {

    private Context context;

    public AndroidFileContext(Context context) {
        this.context = context;
    }

    protected boolean isMounted() {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    protected String getPackageName() {
        return context.getPackageName();
    }

    protected File getExternalStorageDirectory() {
        return android.os.Environment.getExternalStorageDirectory();
    }

    protected File preparePhoneCacheDir() {
        return context.getCacheDir();
    }

}
