package com.yahala.xmpp;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.yahala.SQLite.Messages;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.DispatchQueue;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.LruCache;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Views.BackupImageView2;
import com.yahala.ui.Views.ImageReceiver;

import com.yahala.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by user on 4/20/2014.
 */
public class FileLoader {
    public static final int FileDidUpload = 10000;
    public static final int FileDidFailUpload = 10001;
    public static final int FileUploadProgressChanged = 10002;
    public static final int FileLoadProgressChanged = 10003;
    public static final int FileDidLoaded = 10004;
    public static final int FileDidFailedLoad = 10005;


    private static volatile FileLoader Instance = null;


    public VMRuntimeHack runtimeHack = null;

    public LruCache memCache;

    public static volatile DispatchQueue cacheOutQueue = new DispatchQueue("cacheOutQueue");
    public static volatile DispatchQueue fileLoaderQueue = new DispatchQueue("fileUploadQueue");


    private String ignoreRemoval = null;
    private ConcurrentHashMap<String, CacheImage> imageLoading;
    private HashMap<Integer, CacheImage> imageLoadingByKeys;
    private Queue<FileLoadOperation> operationsQueue;
    private Queue<FileLoadOperation> runningOperation;
    private final int maxConcurentLoadingOpertaionsCount = 2;

    private int currentUploadOperationsCount = 0;
    private Queue<FileLoadOperation> loadOperationQueue;
    private Queue<FileLoadOperation> audioLoadOperationQueue;
    private Queue<FileLoadOperation> photoLoadOperationQueue;

    private Queue<FileOperation> uploadOperationQueue;
    private ConcurrentHashMap<String, FileOperation> uploadOperationPaths;


    private ConcurrentHashMap<String, FileLoadOperation> loadOperationPaths;

    private Queue<FileOperation> dwonloadOperationQueue;
    private ConcurrentHashMap<String, FileOperation> downloadOperationPaths;
    private int currentLoadOperationsCount = 0;
    private int currentAudioLoadOperationsCount = 0;
    private int currentPhotoLoadOperationsCount = 0;
    public static long lastCacheOutTime = 0;
    public ConcurrentHashMap<String, Float> fileProgresses = new ConcurrentHashMap<String, Float>();
    private long lastProgressUpdateTime = 0;
    private HashMap<String, Integer> BitmapUseCounts = new HashMap<String, Integer>();

    int lastImageNum;

    public FileLoader() {
        int cacheSize = Math.min(15, ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() / 7) * 1024 * 1024;

        if (Build.VERSION.SDK_INT < 11) {
            runtimeHack = new VMRuntimeHack();
            cacheSize = 1024 * 1024 * 3;
        }
        memCache = new LruCache(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT < 12) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                } else {
                    return bitmap.getByteCount();
                }
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
                if (ignoreRemoval != null && key != null && ignoreRemoval.equals(key)) {
                    return;
                }
                Integer count = BitmapUseCounts.get(key);
                if (count == null || count == 0) {
                    if (runtimeHack != null) {
                        runtimeHack.trackAlloc(oldBitmap.getRowBytes() * oldBitmap.getHeight());
                    }
                    if (Build.VERSION.SDK_INT < 11) {
                        if (!oldBitmap.isRecycled()) {
                            oldBitmap.recycle();
                        }
                    }
                }
            }
        };
        imageLoading = new ConcurrentHashMap<String, CacheImage>();
        imageLoadingByKeys = new HashMap<Integer, CacheImage>();
        operationsQueue = new LinkedList<FileLoadOperation>();
        runningOperation = new LinkedList<FileLoadOperation>();

        uploadOperationQueue = new LinkedList<FileOperation>();
        uploadOperationPaths = new ConcurrentHashMap<String, FileOperation>();

        dwonloadOperationQueue = new LinkedList<FileOperation>();
        downloadOperationPaths = new ConcurrentHashMap<String, FileOperation>();

        loadOperationPaths = new ConcurrentHashMap<String, FileLoadOperation>();
        loadOperationQueue = new LinkedList<FileLoadOperation>();
        audioLoadOperationQueue = new LinkedList<FileLoadOperation>();
        photoLoadOperationQueue = new LinkedList<FileLoadOperation>();
    }

    public static FileLoader getInstance() {
        FileLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLoader();
                }
            }
        }
        return localInstance;
    }

    public void removeImage(String key) {
        BitmapUseCounts.remove(key);
        memCache.remove(key);
    }

    public void incrementUseCount(String key) {
        Integer count = BitmapUseCounts.get(key);
        if (count == null) {
            BitmapUseCounts.put(key, 1);
        } else {
            BitmapUseCounts.put(key, count + 1);
        }
    }

    public boolean isInCache(String key) {
        return memCache.get(key) != null;
    }

    public boolean decrementUseCount(String key) {
        Integer count = BitmapUseCounts.get(key);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            BitmapUseCounts.remove(key);
            return true;
        } else {
            BitmapUseCounts.put(key, count - 1);
        }
        return false;
    }

    public static Bitmap loadBitmap(String path, Uri uri, float maxWidth, float maxHeight) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        FileDescriptor fileDescriptor = null;
        ParcelFileDescriptor parcelFD = null;
        FileLog.e("yahala loadBitmap", "path:" + path);
        if (path == null && uri != null && uri.getScheme() != null) {
            String imageFilePath = null;
            if (uri.getScheme().contains("file")) {
                path = uri.getPath();
            } else {
                try {
                    path = Utilities.getPath(uri);
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
            }
        }

        if (path != null) {
            BitmapFactory.decodeFile(path, bmOptions);
        } else if (uri != null) {
            boolean error = false;
            try {
                parcelFD = ApplicationLoader.applicationContext.getContentResolver().openFileDescriptor(uri, "r");
                fileDescriptor = parcelFD.getFileDescriptor();
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);
            } catch (Exception e) {
                FileLog.e("yahala", e);
                try {
                    if (parcelFD != null) {
                        parcelFD.close();
                    }
                } catch (Exception e2) {
                    FileLog.e("yahala", e2);
                }
                return null;
            }
        }
        float photoW = bmOptions.outWidth;
        float photoH = bmOptions.outHeight;
        float scaleFactor = Math.max(photoW / maxWidth, photoH / maxHeight);
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = (int) scaleFactor;

        String exifPath = null;
        if (path != null) {
            exifPath = path;
        } else if (uri != null) {
            exifPath = Utilities.getPath(uri);
        }

        Matrix matrix = null;

        if (exifPath != null) {
            ExifInterface exif;
            try {
                exif = new ExifInterface(exifPath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }
            } catch (Exception e) {
                FileLog.e("yahala", e);
            }
        }

        Bitmap b = null;
        if (path != null) {
            try {
                b = BitmapFactory.decodeFile(path, bmOptions);
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            } catch (Exception e) {
                FileLog.e("yahala", e);
                //FileLoader.getInstance().memCache.evictAll();
                if (b == null) {
                    b = BitmapFactory.decodeFile(path, bmOptions);
                }
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            }
        } else if (uri != null) {
            try {
                b = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, bmOptions);
                if (b != null) {
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
            } catch (Exception e) {
                FileLog.e("yahala", e);
            } finally {
                try {
                    if (parcelFD != null) {
                        parcelFD.close();
                    }
                } catch (Exception e) {
                    FileLog.e("yahala", e);
                }
            }
        }

        return b;
    }

    public static TLRPC.PhotoSize scaleAndSaveImage(Bitmap bitmap, float maxWidth, float maxHeight, int quality, boolean cache) {
        if (bitmap == null) {
            return null;
        }
        float photoW = bitmap.getWidth();
        float photoH = bitmap.getHeight();
        if (photoW == 0 || photoH == 0) {
            return null;
        }
        float scaleFactor = Math.max(photoW / maxWidth, photoH / maxHeight);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (photoW / scaleFactor), (int) (photoH / scaleFactor), true);

        TLRPC.TL_fileLocation location = new TLRPC.TL_fileLocation();
        location.volume_id = Integer.MIN_VALUE;
        location.dc_id = Integer.MIN_VALUE;
        location.local_id = UserConfig.lastLocalId;
        FileLog.e("scaleAndSaveImage", location.volume_id + "_" + location.local_id + ".jpg");
        UserConfig.lastLocalId--;
        TLRPC.PhotoSize size;
        if (!cache) {
            size = new TLRPC.TL_photoSize();
        } else {
            size = new TLRPC.TL_photoCachedSize();
        }
        size.location = location;
        size.w = (int) (photoW / scaleFactor);
        size.h = (int) (photoH / scaleFactor);
        try {
            if (!cache) {
                String fileName = location.volume_id + "_" + location.local_id + ".jpg";
                final File cacheFile = new File(OSUtilities.getCacheDir(), fileName);
                FileOutputStream stream = new FileOutputStream(cacheFile);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                size.size = (int) stream.getChannel().size();
            } else {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                size.bytes = stream.toByteArray();
                size.size = size.bytes.length;
            }
            if (Build.VERSION.SDK_INT < 11) {
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle();
                }
            }
            return size;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void cancelLoadingForImageView(final ImageReceiver imageView) {
        if (imageView == null) {
            return;
        }
    }

    Bitmap imageFromKey(String key) {
        if (key == null) {
            return null;
        }
        return memCache.get(key);
    }

    public Bitmap getImageFromMemory(TLRPC.FileLocation url, String httpUrl, ImageReceiver imageView, String filter, boolean cancel) {
        if (url == null && httpUrl == null) {
            return null;
        }
        String key;
        if (httpUrl != null) {
            key = Utilities.MD5(httpUrl);
        } else {
            key = url.volume_id + "_" + url.local_id;
        }
        if (filter != null) {
            key += "@" + filter;
        }

        Bitmap img = imageFromKey(key);
        if (imageView != null && img != null && cancel) {
            cancelLoadingForImageView(imageView);
        }
        return img;
    }

    public void loadFile(final TLRPC.Video video, final TLRPC.PhotoSize photo, final TLRPC.Document document, final TLRPC.Audio audio) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (video != null) {
                    fileName = MessageObject.getAttachFileName(video);
                } else if (photo != null) {
                    fileName = MessageObject.getAttachFileName(photo);
                } else if (document != null) {
                    fileName = MessageObject.getAttachFileName(document);
                } else if (audio != null) {
                    fileName = audio.path;//MessageObject.getAttachFileName(audio);
                }
                if (fileName == null || fileName.contains("" + Integer.MIN_VALUE)) {
                    return;
                }
                if (loadOperationPaths.containsKey(fileName)) {
                    return;
                }
                FileLoadOperation operation = null;
                if (video != null) {
                    operation = new FileLoadOperation(video);
                    operation.totalBytesCount = video.size;
                } else if (photo != null) {
                    operation = new FileLoadOperation(photo.location);
                    operation.totalBytesCount = photo.size;
                    operation.needBitmapCreate = false;
                } else if (document != null) {
                    operation = new FileLoadOperation(document);
                    operation.totalBytesCount = document.size;
                } else if (audio != null) {
                    operation = new FileLoadOperation(audio);
                    operation.totalBytesCount = audio.size;
                }

                final String arg1 = fileName;
                loadOperationPaths.put(fileName, operation);
                operation.delegate = new FileLoadOperation.FileLoadOperationDelegate() {
                    @Override
                    public void didFinishLoadingFile(FileLoadOperation operation) {
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, 1.0f);
                            }
                        });
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(FileDidLoaded, arg1);
                            }
                        });
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                loadOperationPaths.remove(arg1);
                                if (audio != null) {
                                    currentAudioLoadOperationsCount--;
                                    if (currentAudioLoadOperationsCount < 2) {
                                        FileLoadOperation operation = audioLoadOperationQueue.poll();
                                        if (operation != null) {
                                            currentAudioLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                } else {
                                    currentLoadOperationsCount--;
                                    if (currentLoadOperationsCount < 2) {
                                        FileLoadOperation operation = loadOperationQueue.poll();
                                        if (operation != null) {
                                            currentLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                }
                            }
                        });
                        fileProgresses.remove(arg1);
                    }

                    @Override
                    public void didFailedLoadingFile(FileLoadOperation operation) {
                        fileProgresses.remove(arg1);
                        if (operation.state != 2) {
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileDidFailedLoad, arg1);
                                }
                            });
                        }
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                loadOperationPaths.remove(arg1);
                                if (audio != null) {
                                    currentAudioLoadOperationsCount--;
                                    if (currentAudioLoadOperationsCount < 2) {
                                        FileLoadOperation operation = audioLoadOperationQueue.poll();
                                        if (operation != null) {
                                            currentAudioLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                } else {
                                    currentLoadOperationsCount--;
                                    if (currentLoadOperationsCount < 2) {
                                        FileLoadOperation operation = loadOperationQueue.poll();
                                        if (operation != null) {
                                            currentLoadOperationsCount++;
                                            operation.start();
                                        }
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didChangedLoadProgress(FileLoadOperation operation, final float progress) {
                        if (operation.state != 2) {
                            fileProgresses.put(arg1, progress);
                        }
                        long currentTime = System.currentTimeMillis();
                        if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                            lastProgressUpdateTime = currentTime;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, progress);
                                }
                            });
                        }
                    }
                };
                if (audio != null) {
                    if (currentAudioLoadOperationsCount < 2) {
                        currentAudioLoadOperationsCount++;
                        operation.start();
                    } else {
                        audioLoadOperationQueue.add(operation);
                    }
                } else {
                    if (currentLoadOperationsCount < 2) {
                        currentLoadOperationsCount++;
                        operation.start();
                    } else {
                        loadOperationQueue.add(operation);
                    }
                }
            }
        });
    }

    public void cancelLoadFile(final TLRPC.Video video, final TLRPC.PhotoSize photo, final TLRPC.Document document, final TLRPC.Audio audio) {
        if (video == null && photo == null && document == null && audio == null) {
            return;
        }
        //fileLoaderQueue.postRunnable(new Runnable() {
        //   @Override
        //   public void run() {
        String fileName = null;
        if (video != null) {
            fileName = MessageObject.getAttachFileName(video);
        } else if (photo != null) {
            fileName = MessageObject.getAttachFileName(photo);
        } else if (document != null) {
            fileName = MessageObject.getAttachFileName(document);
        } else if (audio != null) {
            fileName = MessageObject.getAttachFileName(audio);
        }
        if (fileName == null) {
            return;
        }
        FileLoadOperation operation = loadOperationPaths.get(fileName);
        if (operation != null) {
            if (audio != null) {
                audioLoadOperationQueue.remove(operation);
            } else {
                loadOperationQueue.remove(operation);
            }
            operation.cancel();
        }
        // }
        // });
    }

    public boolean isLoadingFile(String fileName) {
        return loadOperationPaths.containsKey(fileName);
    }

    private Integer getTag(Object obj) {
        if (obj instanceof BackupImageView2) {
            return (Integer) ((BackupImageView2) obj).getTag(R.string.CacheTag);
        } else if (obj instanceof ImageReceiver) {
            return ((ImageReceiver) obj).TAG;
        }
        return 0;
    }

    public void cancelLoadingForImageView(final Object imageView) {
        if (imageView == null) {
            return;
        }
        Utilities.imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public class VMRuntimeHack {
        private Object runtime = null;
        private Method trackAllocation = null;
        private Method trackFree = null;

        @SuppressWarnings("unchecked")
        public VMRuntimeHack() {
            boolean success = false;
            try {
                Class cl = Class.forName("dalvik.system.VMRuntime");
                Method getRt = cl.getMethod("getRuntime", new Class[0]);
                runtime = getRt.invoke(null, new Object[0]);
                trackAllocation = cl.getMethod("trackExternalAllocation", new Class[]{long.class});
                trackFree = cl.getMethod("trackExternalFree", new Class[]{long.class});
                success = true;
            } catch (ClassNotFoundException e) {
                FileLog.e("tmessages", e);
            } catch (SecurityException e) {
                FileLog.e("tmessages", e);
            } catch (NoSuchMethodException e) {
                FileLog.e("tmessages", e);
            } catch (IllegalArgumentException e) {
                FileLog.e("tmessages", e);
            } catch (IllegalAccessException e) {
                FileLog.e("tmessages", e);
            } catch (InvocationTargetException e) {
                FileLog.e("tmessages", e);
            }
            if (!success) {
                runtime = null;
                trackAllocation = null;
                trackFree = null;
            }
        }

        public boolean trackAlloc(long size) {
            if (runtime == null)
                return false;
            try {
                Object res = trackAllocation.invoke(runtime, size);
                return (res instanceof Boolean) ? (Boolean) res : true;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            } catch (InvocationTargetException e) {
                return false;
            }
        }

        public boolean trackFree(long size) {
            if (runtime == null)
                return false;
            try {
                Object res = trackFree.invoke(runtime, size);
                return (res instanceof Boolean) ? (Boolean) res : true;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            } catch (InvocationTargetException e) {
                return false;
            }
        }
    }

    private class CacheImage {
        public String key;
        final public ArrayList<ImageReceiver> imageViewArray = new ArrayList<ImageReceiver>();
        public FileLoadOperation loadOperation;

        public void addImageView(ImageReceiver imageView) {
            synchronized (imageViewArray) {
                boolean exist = false;
                for (Object v : imageViewArray) {
                    if (v == imageView) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    imageViewArray.add(imageView);
                }
            }
        }

        public void removeImageView(Object imageView) {
            synchronized (imageViewArray) {
                for (int a = 0; a < imageViewArray.size(); a++) {
                    Object obj = imageViewArray.get(a);
                    if (obj == null || obj == imageView) {
                        imageViewArray.remove(a);
                        a--;
                    }
                }
            }
        }

        public void callAndClear(Bitmap image) {
            synchronized (imageViewArray) {
                if (image != null) {
                    for (Object imgView : imageViewArray) {
                        if (imgView instanceof ImageReceiver) {
                            ((ImageReceiver) imgView).setImageBitmap(image, key);
                        }
                    }
                }
            }
            fileLoaderQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    synchronized (imageViewArray) {
                        imageViewArray.clear();
                    }
                    loadOperation = null;
                }
            });
        }

        public void cancelAndClear() {
            if (loadOperation != null) {
                loadOperation.cancel();
                loadOperation = null;
            }
            synchronized (imageViewArray) {
                imageViewArray.clear();
            }
        }
    }

    public void loadImage(final String url, final ImageReceiver imageView, final String filter, final boolean cancel) {
        loadImage(null, url, imageView, filter, cancel, 0);
    }

    public void loadImage(final TLRPC.FileLocation url, final ImageReceiver imageView, final String filter, final boolean cancel) {
        loadImage(url, null, imageView, filter, cancel, 0);
    }

    public void loadImage(final TLRPC.FileLocation url, final ImageReceiver imageView, final String filter, final boolean cancel, final int size) {
        loadImage(url, null, imageView, filter, cancel, size);
    }

    public void loadImage(final TLRPC.FileLocation url, final String httpUrl, final ImageReceiver imageView, final String filter, final boolean cancel, final int size) {
        if ((url == null && httpUrl == null) || imageView == null || (url != null && !(url instanceof TLRPC.TL_fileLocation) && !(url instanceof TLRPC.TL_fileEncryptedLocation))) {
            return;
        }
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String key;
                String fileName = null;
                if (httpUrl != null) {
                    key = Utilities.MD5(httpUrl);
                } else {
                    key = url.volume_id + "_" + url.local_id;
                    fileName = key + ".jpg";
                }
                if (filter != null) {
                    key += "@" + filter;
                }

                Integer TAG = imageView.TAG;
                if (TAG == null) {
                    TAG = imageView.TAG = lastImageNum;
                    lastImageNum++;
                    if (lastImageNum == Integer.MAX_VALUE)
                        lastImageNum = 0;
                }

                boolean added = false;
                boolean addToByKeys = true;
                CacheImage alreadyLoadingImage = imageLoading.get(key);
                if (cancel) {
                    CacheImage ei = imageLoadingByKeys.get(TAG);
                    if (ei != null) {
                        if (ei != alreadyLoadingImage) {
                            ei.removeImageView(imageView);
                            if (ei.imageViewArray.size() == 0) {
                                checkOperationsAndClear(ei.loadOperation);
                                ei.cancelAndClear();
                                imageLoading.remove(ei.key);
                            }
                        } else {
                            addToByKeys = false;
                            added = true;
                        }
                    }
                }

                if (alreadyLoadingImage != null && addToByKeys) {
                    alreadyLoadingImage.addImageView(imageView);
                    imageLoadingByKeys.put(TAG, alreadyLoadingImage);
                    added = true;
                }

                if (!added) {
                    final CacheImage img = new CacheImage();
                    img.key = key;
                    img.addImageView(imageView);
                    imageLoadingByKeys.put(TAG, img);
                    imageLoading.put(key, img);

                    final String arg2 = key;
                    final String arg3 = fileName;
                    FileLoadOperation loadOperation;
                    if (httpUrl != null) {
                        loadOperation = new FileLoadOperation(httpUrl);
                    } else {
                        loadOperation = new FileLoadOperation(url);
                    }
                    loadOperation.totalBytesCount = size;
                    loadOperation.filter = filter;
                    loadOperation.delegate = new FileLoadOperation.FileLoadOperationDelegate() {
                        @Override
                        public void didFinishLoadingFile(final FileLoadOperation operation) {
                            if (operation.totalBytesCount != 0) {
                                fileProgresses.remove(arg3);
                            }
                            fileLoaderQueue.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    if (arg3 != null) {
                                        loadOperationPaths.remove(arg3);
                                    }
                                    for (ImageReceiver v : img.imageViewArray) {
                                        imageLoadingByKeys.remove(v.TAG);
                                    }
                                    checkOperationsAndClear(img.loadOperation);
                                    imageLoading.remove(arg2);
                                }
                            });

                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    img.callAndClear(operation.image);
                                    if (operation.image != null && memCache.get(arg2) == null) {
                                        memCache.put(arg2, operation.image);
                                    }
                                    NotificationCenter.getInstance().postNotificationName(FileDidLoaded, arg3);
                                }
                            });
                        }

                        @Override
                        public void didFailedLoadingFile(final FileLoadOperation operation) {
                            fileLoaderQueue.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    if (arg3 != null) {
                                        loadOperationPaths.remove(arg3);
                                    }
                                    for (ImageReceiver view : img.imageViewArray) {
                                        imageLoadingByKeys.remove(view.TAG);
                                        imageLoading.remove(arg2);
                                        checkOperationsAndClear(operation);
                                    }
                                }
                            });
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    img.callAndClear(null);
                                }
                            });
                            if (operation.totalBytesCount != 0) {
                                final String arg1 = operation.location.volume_id + "_" + operation.location.local_id + ".jpg";
                                fileProgresses.remove(arg1);
                                if (operation.state != 2) {
                                    Utilities.RunOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NotificationCenter.getInstance().postNotificationName(FileDidFailedLoad, arg1);
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void didChangedLoadProgress(FileLoadOperation operation, final float progress) {
                            if (operation.totalBytesCount != 0) {
                                final String arg1 = operation.location.volume_id + "_" + operation.location.local_id + ".jpg";
                                if (operation.state != 2) {
                                    fileProgresses.put(arg1, progress);
                                }
                                long currentTime = System.currentTimeMillis();
                                if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 50) {
                                    lastProgressUpdateTime = currentTime;
                                    Utilities.RunOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NotificationCenter.getInstance().postNotificationName(FileLoadProgressChanged, arg1, progress);
                                        }
                                    });
                                }
                            }
                        }
                    };
                    img.loadOperation = loadOperation;
                    if (runningOperation.size() < maxConcurentLoadingOpertaionsCount) {
                        FileLog.e("loadOperation httpUrl", httpUrl + " ");
                        loadOperation.start();
                        runningOperation.add(loadOperation);
                    } else {
                        operationsQueue.add(loadOperation);
                    }
                    if (fileName != null) {
                        loadOperationPaths.put(fileName, loadOperation);
                    }
                }
            }
        });
    }

    private void checkOperationsAndClear(FileLoadOperation operation) {
        operationsQueue.remove(operation);
        runningOperation.remove(operation);
        while (runningOperation.size() < maxConcurentLoadingOpertaionsCount && operationsQueue.size() != 0) {
            FileLoadOperation loadOperation = operationsQueue.poll();
            runningOperation.add(loadOperation);
            loadOperation.start();
        }
    }


    public void cancelUploadFile(final String location, final boolean enc) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                FileLog.e("cancelUploadFile", location);
                FileOperation operation = uploadOperationPaths.get(location);
                if (operation != null) {
                    uploadOperationQueue.remove(operation);
                    operation.cancel();
                }

            }
        });
    }

    public void loadFile(final String sUrl, final Messages newMsg) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (loadOperationPaths.containsKey(sUrl)) {
                    return;
                }
                FileOperation operation = new FileOperation(sUrl, newMsg);
                downloadOperationPaths.put(sUrl, operation);
                operation.delegate = new FileOperation.FileUploadOperationDelegate() {
                    @Override
                    public void didFinishUploadingFile(FileOperation operation, final int mid) {
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileDidLoaded, sUrl, mid);
                                        fileProgresses.remove(sUrl);
                                    }
                                });
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileUploadProgressChanged, sUrl, 1.0f);
                                    }
                                });
                                downloadOperationPaths.remove(sUrl);

                                currentLoadOperationsCount--;
                                if (currentLoadOperationsCount < 2) {
                                    FileOperation operation = dwonloadOperationQueue.poll();
                                    if (operation != null) {
                                        currentLoadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }


                    @Override
                    public void didFailedUploadingFile(final FileOperation operation) {
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        fileProgresses.remove(sUrl);
                                        if (operation.state != 2) {
                                            NotificationCenter.getInstance().postNotificationName(FileDidFailedLoad, sUrl);
                                        }
                                    }
                                });

                                downloadOperationPaths.remove(sUrl);

                                currentUploadOperationsCount--;
                                if (currentLoadOperationsCount < 2) {
                                    FileOperation operation = dwonloadOperationQueue.poll();
                                    if (operation != null) {
                                        currentLoadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didChangedUploadProgress(FileOperation operation, final float progress, final int mid) {
                        if (operation.state != 2) {
                            fileProgresses.put(sUrl, progress);
                        }
                        long currentTime = System.currentTimeMillis();
                        if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                            lastProgressUpdateTime = currentTime;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileUploadProgressChanged, sUrl, progress);
                                }
                            });
                        }
                    }
                };
                if (currentUploadOperationsCount < 2) {
                    currentLoadOperationsCount++;
                    operation.start();
                } else {
                    dwonloadOperationQueue.add(operation);
                }
            }
        });

    }

    public void uploadFile(final String type, final String filePath, final Messages newMsg, final String toJid) {

        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (uploadOperationPaths.containsKey(filePath)) {
                    return;
                }
                FileOperation operation = new FileOperation(type, filePath, newMsg, toJid);
                uploadOperationPaths.put(filePath, operation);
                operation.delegate = new FileOperation.FileUploadOperationDelegate() {
                    @Override
                    public void didFinishUploadingFile(final FileOperation operation, final int mid) {
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileDidUpload, filePath);
                                        fileProgresses.remove(filePath);
                                    }
                                });
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(FileUploadProgressChanged, filePath, 1.0f);
                                    }
                                });
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (operation.state != 2) {
                                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageReceivedByServer, "0", (long) mid);
                                        }
                                    }
                                });
                                uploadOperationPaths.remove(filePath);

                                currentUploadOperationsCount--;
                                if (currentUploadOperationsCount < 2) {
                                    FileOperation operation = uploadOperationQueue.poll();
                                    if (operation != null) {
                                        currentUploadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }


                    @Override
                    public void didFailedUploadingFile(final FileOperation operation) {
                        fileLoaderQueue.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.stageQueue.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        fileProgresses.remove(filePath);
                                        if (operation.state != 2) {
                                            NotificationCenter.getInstance().postNotificationName(FileDidFailUpload, filePath);
                                        }
                                    }
                                });
                                uploadOperationPaths.remove(filePath);

                                currentUploadOperationsCount--;
                                if (currentUploadOperationsCount < 2) {
                                    FileOperation operation = uploadOperationQueue.poll();
                                    if (operation != null) {
                                        currentUploadOperationsCount++;
                                        operation.start();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void didChangedUploadProgress(FileOperation operation, final float progress, int mid) {
                        if (operation.state != 2) {
                            fileProgresses.put(filePath, progress);
                        }
                        long currentTime = System.currentTimeMillis();
                        if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                            lastProgressUpdateTime = currentTime;
                            Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(FileUploadProgressChanged, filePath, progress);
                                }
                            });
                        }
                    }
                };


                if (currentUploadOperationsCount < 2) {
                    currentUploadOperationsCount++;
                    operation.start();
                } else {
                    uploadOperationQueue.add(operation);
                }
            }
        });
    }
}
