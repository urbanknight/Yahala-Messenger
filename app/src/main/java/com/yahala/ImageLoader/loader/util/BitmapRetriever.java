package com.yahala.ImageLoader.loader.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.ImageView;


import com.yahala.ImageLoader.LoaderSettings;
import com.yahala.ImageLoader.exception.ImageNotFoundException;
import com.yahala.ImageLoader.model.ImageTag;

import java.io.File;
import java.io.InputStream;

public class BitmapRetriever {

    private final int width;
    private final int height;
    private final int notFoundResourceId;
    private final boolean saveScaledImage;
    private final Context context;
    private final ImageView imageView;
    private String url;
    private File imageFile;
    private boolean useCacheOnly;
    private LoaderSettings loaderSettings;

    public BitmapRetriever(String url, File imageFile, int width, int height, int notFoundResourceId,
                           boolean useCacheOnly, boolean saveScaledImage, ImageView imageView, LoaderSettings loaderSettings, Context context) {
        this.url = url;
        this.imageFile = imageFile;
        this.width = width;
        this.height = height;
        this.notFoundResourceId = notFoundResourceId;
        this.useCacheOnly = useCacheOnly;
        this.saveScaledImage = saveScaledImage;
        this.imageView = imageView;
        this.loaderSettings = loaderSettings;
        this.context = context;
    }

    public Bitmap getBitmap() {
        if (url == null || url.length() <= 0 || url.equals("_url_error")) {
            return getNotFoundImage();
        }

        if (!imageFile.exists()) {
            if (useCacheOnly) {
                return null;
            }
            Uri uri = Uri.parse(url);
            if (isContactPhoto(uri)) {
                return getContactPhoto(uri);
            } else if (isFromFileSystem(uri)) {
                return getLocalImage(uri);
            } else {
                return getNetworkImage(imageFile, uri);
            }
        }

        Bitmap bitmap = getImageFromFile(imageFile);
        if (bitmap == null) {
            onDecodeFailed();
        } else {
            if (bitmap.isRecycled()) {
                bitmap = null;
            }
        }
        return bitmap;
    }

    private void onDecodeFailed() {
        try {
            imageFile.delete();
        } catch (SecurityException e) {
            //
        }
    }

    private boolean isContactPhoto(Uri uri) {
        return uri.toString().startsWith("content://com.android.contacts/");
    }

    private Bitmap getContactPhoto(Uri uri) {
        if (context != null) {
            InputStream photoDataStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
            Bitmap photo = BitmapFactory.decodeStream(photoDataStream);
            return photo;
        } else {
            return null;
        }
    }

    private boolean isFromFileSystem(Uri uri) {
        return uri.getScheme() != null ? uri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_FILE) : true;
    }

    private Bitmap getLocalImage(Uri uri) {
        File image = new File(uri.getPath());
        if (image.exists()) {
            return getImageFromFile(image);
        } else {
            return getNotFoundImage();
        }
    }

    private Bitmap getNetworkImage(File imageFile, Uri uri) {
        try {
            loaderSettings.getNetworkManager().retrieveImage(uri.toString(), imageFile);
        } catch (ImageNotFoundException inf) {
            return getNotFoundImage();
        }
        if (hasImageViewUrlChanged()) {
            return null;
        }
        return getImageFromFile(imageFile);
    }

    private boolean hasImageViewUrlChanged() {
        if (url == null) {
            return false;
        } else {
            return !url.equals(((ImageTag) imageView.getTag()).getUrl());
        }
    }

    private Bitmap getImageFromFile(File imageFile) {
        Bitmap b;
        if (loaderSettings.isAlwaysUseOriginalSize()) {
            b = loaderSettings.getBitmapUtil().decodeFile(imageFile, width, height);
        } else {
            b = loaderSettings.getBitmapUtil().decodeFileAndScale(imageFile, width, height, loaderSettings.isAllowUpsampling());
        }

        if (b == null) {
            // decoding failed
            loaderSettings.getCacheManager().remove(url);
            return b;
        }

        if (saveScaledImage) {
            saveScaledImage(imageFile, b);
        }
        loaderSettings.getCacheManager().put(url, b);
        return b;
    }

    private void saveScaledImage(File imageFile, Bitmap b) {
        loaderSettings.getFileManager().saveBitmap(imageFile.getAbsolutePath(), b, width, height);
    }

    private Bitmap getNotFoundImage() {
        String key = "resource" + notFoundResourceId + width + height;
        Bitmap b = loaderSettings.getResCacheManager().get(key, width, height);
        if (b != null) {
            return b;
        }
        if (context != null) {
            if (loaderSettings.isAlwaysUseOriginalSize()) {
                b = loaderSettings.getBitmapUtil().decodeResourceBitmap(context, width, height, notFoundResourceId);
            } else {
                b = loaderSettings.getBitmapUtil().decodeResourceBitmapAndScale(context, width, height, notFoundResourceId, loaderSettings.isAllowUpsampling());
            }
            loaderSettings.getResCacheManager().put(key, b);
        }
        return b;
    }

}
