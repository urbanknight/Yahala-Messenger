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


import com.yahala.ImageLoader.exception.ImageCopyException;
import com.yahala.ImageLoader.util.Log;

import java.io.*;

/**
 * This class is internal to the imageLoader.
 * If you want to used it make sure to prepare for changes to the interface.
 */
public class FileUtil {

    private static final String ANDROID_ROOT = "/Android/data/";
    private static final String NOMEDIA_FILE_NAME = ".nomedia";
    private static final String DEFAULT_IMAGE_FOLDER_NAME = "/cache/images";
    private static final int BUFFER_SIZE = 60 * 1024;

    public void closeSilently(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Log.warning("Problem closing stream " + e.getMessage());
        }
    }

    public void copyStream(InputStream is, OutputStream os) {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int amountRead = is.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                os.write(buffer, 0, amountRead);
            }
        } catch (Exception e) {
            Log.warning("Exception : " + e.getMessage());
        }
    }

    public boolean deleteFileCache(String cacheDirFullPath) {
        return reduceFileCache(cacheDirFullPath, -1);
    }

    public boolean reduceFileCache(String cacheDirFullPath, long expirationPeriod) {
        File cacheDir = new File(cacheDirFullPath);
        if (cacheDir.isDirectory()) {
            File[] children = cacheDir.listFiles();
            long lastModifiedThreashold = System.currentTimeMillis() - expirationPeriod;
            for (File f : children) {
                if (f.lastModified() < lastModifiedThreashold) {
                    f.delete();
                }
            }
        }
        return true;
    }

    public void copy(File src, File dst) throws ImageCopyException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new ImageCopyException(e);
        } finally {
            closeSilently(out);
            closeSilently(in);
        }
    }

    public File prepareCacheDirectory(AndroidFileContext fileContext) {
        File dir = null;
        if (fileContext.isMounted()) {
            dir = prepareExternalCacheDir(fileContext);
        } else {
            dir = fileContext.preparePhoneCacheDir();
        }
        addNomediaFile(dir);
        return dir;
    }

    private File prepareExternalCacheDir(AndroidFileContext fileContext) {
        String relativepath = ANDROID_ROOT + fileContext.getPackageName() + DEFAULT_IMAGE_FOLDER_NAME;
        File file = new File(fileContext.getExternalStorageDirectory(), relativepath);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

    private void addNomediaFile(File dir) {
        try {
            new File(dir, NOMEDIA_FILE_NAME).createNewFile();
        } catch (Exception e) {
            Log.warning("Problem creating .nomedia file : " + e.getMessage());
        }
    }

}
