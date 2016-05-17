package com.yahala.ui.lazylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Views.BackupImageView2;

public class ImageLoader {

    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private Map<BackupImageView2, String> imageViews = Collections.synchronizedMap(new WeakHashMap<BackupImageView2, String>());
    ExecutorService executorService;
    Handler handler = new Handler();//handler to display images in UI thread
    private static volatile ImageLoader Instance = null;
    private Context mContex;

    public static ImageLoader getInstance() {
        ImageLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (ImageLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ImageLoader(ApplicationLoader.applicationContext);
                }
            }
        }
        return localInstance;
    }

    public ImageLoader(Context context) {
        fileCache = new FileCache(context);
        executorService = Executors.newFixedThreadPool(3);
    }

    public final int stub_id = R.drawable.transparent;

    public void DisplayImage(String url, BackupImageView2 imageView) {
        imageViews.put(imageView, url);
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            queuePhoto(url, imageView);
            // imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url, BackupImageView2 imageView) {
        PhotoToLoad p = new PhotoToLoad(url, imageView);
        executorService.submit(new PhotosLoader(p));
    }

    private Bitmap getBitmap(String url) {
        File f = fileCache.getFile(url);
        FileLog.e("getBitmap", url + "");
        //from SD cache
        Bitmap b = decodeFile(f);
        if (b != null)
            return b;
        File imgFile = new File(url);
        if (imgFile.exists()) {

            Bitmap myBitmap =/* BitmapFactory.decodeFile(imgFile.getAbsolutePath());*/ decodeFile(imgFile);
            return myBitmap;
        }
        //from web
       /* try {
            Bitmap bitmap=null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream is=conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            Utils.CopyStream(is, os);
            os.close();
            conn.disconnect();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Throwable ex){
           ex.printStackTrace();
           if(ex instanceof OutOfMemoryError)
               memoryCache.clear();
           return null;
        }*/
        return null;
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1 = new FileInputStream(f);
            BitmapFactory.decodeStream(stream1, null, o);
            stream1.close();

            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE = OSUtilities.dp(150);
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;  //int scale=(int)ImageSizeUtils.computeImageScale(new ImageSize(width_tmp, height_tmp), new ImageSize(200, 200), ViewScaleType.CROP,false);

            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;

            FileInputStream stream2 = new FileInputStream(f);
            Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;// Utilities.scaleCenterCrop(bitmap,100,100) ;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Task for the queue
    private class PhotoToLoad {
        public String url;
        public BackupImageView2 imageView;

        public PhotoToLoad(String u, BackupImageView2 i) {
            url = u;
            imageView = i;
        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;

        PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
            try {
                if (imageViewReused(photoToLoad))
                    return;
                Bitmap bmp = getBitmap(photoToLoad.url);
                memoryCache.put(photoToLoad.url, bmp);
                if (imageViewReused(photoToLoad))
                    return;
                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
                handler.post(bd);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    boolean imageViewReused(PhotoToLoad photoToLoad) {
        String tag = imageViews.get(photoToLoad.imageView);
        if (tag == null || !tag.equals(photoToLoad.url))
            return true;
        return false;
    }

    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;

        public BitmapDisplayer(Bitmap b, PhotoToLoad p) {
            bitmap = b;
            photoToLoad = p;
        }

        public void run() {
            if (imageViewReused(photoToLoad))
                return;
            if (bitmap != null)
                photoToLoad.imageView.setImageBitmap(bitmap);
            // else
            // photoToLoad.imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

}
