/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Views;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.soundcloud.android.crop.Crop;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.LaunchActivity;
import com.yahala.xmpp.FileLoader;
import com.yahala.xmpp.XMPPManager;

import com.yahala.messenger.Utilities;

import java.io.File;
import java.io.IOException;

public class AvatarUpdater implements NotificationCenter.NotificationCenterDelegate, PhotoCropActivity.PhotoCropActivityDelegate {
    public String currentPicturePath;
    public String uploadingAvatar = null;
    public BaseFragment parentFragment = null;
    public AvatarUpdaterDelegate delegate;
    public boolean returnOnly = false;
    File picturePath = null;
    private TLRPC.PhotoSize smallPhoto;
    private TLRPC.PhotoSize bigPhoto;
    private boolean clearAfterUpdate = false;

    public void clear() {
        if (uploadingAvatar != null) {
            clearAfterUpdate = true;
        } else {
            parentFragment = null;
            delegate = null;
        }
    }

    public void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File image = Utilities.generatePicturePath();
            if (image != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                currentPicturePath = image.getAbsolutePath();
            }
            parentFragment.startActivityForResult(takePictureIntent, 0);
        } catch (Exception e) {
            FileLog.e("Yahala", e);
        }
    }

    public void openGallery() {
        try {

            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            Crop.pickImage(ApplicationLoader.applicationContext, parentFragment);
            photoPickerIntent.setType("image/*");
            //parentFragment.startActivityForResult(photoPickerIntent, 1);
        } catch (Exception e) {
            FileLog.e("Yahala", e);
        }
    }

    public Activity parentActivity;

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(parentActivity.getCacheDir(), "cropped"));
        Crop.of(source, destination).asSquare().start(ApplicationLoader.applicationContext, parentFragment);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(parentActivity.getContentResolver(), Crop.getOutput(result));
                processBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            FileLog.e("Settings", Crop.getError(result).getMessage());
        }
    }

   /* private void startCrop(String path, Uri uri) {
        try {
            LaunchActivity activity = (LaunchActivity) parentFragment.parentActivity;
            if (activity == null) {
                activity = (LaunchActivity) parentFragment.getActivity();
            }
            if (activity == null) {
                return;
            }
            Bundle params = new Bundle();
            if (path != null) {
                params.putString("photoPath", path);
            } else if (uri != null) {
                params.putParcelable("photoUri", uri);
            }
            Uri destination = Uri.fromFile(new File(activity.getCacheDir(), "cropped"));
            Crop.of(uri, destination).asSquare().start(activity);
           // PhotoCropActivity photoCropActivity = new PhotoCropActivity();
           // photoCropActivity.delegate = this;
           // photoCropActivity.setArguments(params);
           // photoCropActivity.onFragmentCreate();
          //  activity.presentFragment(photoCropActivity, "crop", false);
        } catch (Exception e) {
            FileLog.e("Yahala", e);
            Bitmap bitmap = FileLoader.loadBitmap(path, uri, 800, 800);
            processBitmap(bitmap);
        }
    }*/

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        FileLog.e("Avatarupdater", "onActivityResult");

        if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            beginCrop(data.getData());
            FileLog.e("Avatarupdater", "REQUEST_PICK and Activity.RESULT_OK");
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
            FileLog.e("Avatarupdater", "REQUEST_CROP");
        }

        /*if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
                Utilities.addMediaToGallery(currentPicturePath);
                beginCrop(currentPicturePath);
                //startCrop(currentPicturePath, null);

                currentPicturePath = null;
            } else if (requestCode == 1) {
                if (data == null || data.getData() == null) {
                    return;
                }
                beginCrop(data.getData());
                //startCrop(null, data.getData());
            }
        }*/
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        smallPhoto = FileLoader.scaleAndSaveImage(bitmap, 100, 100, 87, false);
        bigPhoto = FileLoader.scaleAndSaveImage(bitmap, 800, 800, 87, false);
        if (bigPhoto != null && smallPhoto != null) {
            if (returnOnly) {
                if (delegate != null) {
                    delegate.didUploadedPhoto(null, smallPhoto, bigPhoto);
                }
            } else {
                UserConfig.currentUser.photo = new TLRPC.TL_userProfilePhoto();
                UserConfig.currentUser.photo.photo_id = 0;


                uploadingAvatar = OSUtilities.getCacheDir() + "/" + bigPhoto.location.volume_id + "_" + bigPhoto.location.local_id + ".jpg";
                NotificationCenter.getInstance().addObserver(AvatarUpdater.this, FileLoader.FileDidUpload);
                NotificationCenter.getInstance().addObserver(AvatarUpdater.this, FileLoader.FileDidFailUpload);
                FileLog.e("Yahala", "processBitmap " + uploadingAvatar);

                XMPPManager.getInstance().changeImage(new File(uploadingAvatar));


                //FileLoader.getInstance().uploadFile(uploadingAvatar, null, null);
            }
        }
    }

    @Override
    public void didFinishCrop(Bitmap bitmap) {
        processBitmap(bitmap);
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == FileLoader.FileDidUpload) {
            String location = (String) args[0];
            if (uploadingAvatar != null && location.equals(uploadingAvatar)) {
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().removeObserver(AvatarUpdater.this, FileLoader.FileDidUpload);
                        NotificationCenter.getInstance().removeObserver(AvatarUpdater.this, FileLoader.FileDidFailUpload);
                        if (delegate != null) {
                            delegate.didUploadedPhoto((TLRPC.InputFile) args[1], smallPhoto, bigPhoto);
                        }
                        uploadingAvatar = null;
                        if (clearAfterUpdate) {
                            parentFragment = null;
                            delegate = null;
                        }
                    }
                });
            }
        } else if (id == FileLoader.FileDidFailUpload) {
            String location = (String) args[0];
            if (uploadingAvatar != null && location.equals(uploadingAvatar)) {
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().removeObserver(AvatarUpdater.this, FileLoader.FileDidUpload);
                        NotificationCenter.getInstance().removeObserver(AvatarUpdater.this, FileLoader.FileDidFailUpload);
                        uploadingAvatar = null;
                        if (clearAfterUpdate) {
                            parentFragment = null;
                            delegate = null;
                        }
                    }
                });
            }
        }
    }

    public static abstract interface AvatarUpdaterDelegate {
        public abstract void didUploadedPhoto(TLRPC.InputFile file, TLRPC.PhotoSize small, TLRPC.PhotoSize big);
    }
}
