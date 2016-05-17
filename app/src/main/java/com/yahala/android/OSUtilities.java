/*
 * This is the source code of Telegram for Android v. 1.4.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package com.yahala.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;


import com.yahala.messenger.FileLog;
import com.yahala.ui.ApplicationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

public class OSUtilities {
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<String, Typeface>();
    private static int prevOrientation = -10;
    private static boolean waitingForSms = false;
    private static final Integer smsLock = 2;
    public static int leftBaseline;
    public static boolean usingHardwareInput;
    public static int statusBarHeight = 0;
    public static float density = 1;
    private static Boolean isTablet = false;
    public static Point displaySize = new Point();
    public static DisplayMetrics displayMetrics = new DisplayMetrics();

    static {
        density = ApplicationLoader.applicationContext.getResources().getDisplayMetrics().density;
        leftBaseline = isTablet ? 80 : 72;
        checkDisplaySize();
    }

    public static void lockOrientation(Activity activity) {
        if (prevOrientation != -10) {
            return;
        }
        try {
            prevOrientation = activity.getRequestedOrientation();
            WindowManager manager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            if (manager != null && manager.getDefaultDisplay() != null) {
                int rotation = manager.getDefaultDisplay().getRotation();
                int orientation = activity.getResources().getConfiguration().orientation;

                if (rotation == Surface.ROTATION_270) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        if (Build.VERSION.SDK_INT >= 9) {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        } else {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    }
                } else if (rotation == Surface.ROTATION_90) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        if (Build.VERSION.SDK_INT >= 9) {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        } else {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                } else if (rotation == Surface.ROTATION_0) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                } else {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        if (Build.VERSION.SDK_INT >= 9) {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        } else {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= 9) {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        } else {
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
    }

    public static void unlockOrientation(Activity activity) {
        try {
            if (prevOrientation != -10) {
                activity.setRequestedOrientation(prevOrientation);
                prevOrientation = -10;
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
    }

    public static void changeOrientation(Activity activity) {
        try {

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            prevOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
    }

    public static Typeface getTypeface(String assetPath) {
        synchronized (typefaceCache) {
            if (!typefaceCache.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), assetPath);
                    typefaceCache.put(assetPath, t);
                } catch (Exception e) {
                    FileLog.e("Typefaces", "Could not get typeface '" + assetPath + "' because " + e.getMessage());
                    return null;
                }
            }
            return typefaceCache.get(assetPath);
        }
    }

    public static boolean isWaitingForSms() {
        boolean value = false;
        synchronized (smsLock) {
            value = waitingForSms;
        }
        return value;
    }

    public static void setWaitingForSms(boolean value) {
        synchronized (smsLock) {
            waitingForSms = value;
        }
    }

    public static void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static boolean isKeyboardShowed(View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        return inputManager.isActive(view);
    }

    public static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!imm.isActive()) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static File getCacheDir() {
        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        if (state == null || state.startsWith(Environment.MEDIA_MOUNTED)) {
            try {
                File file = ApplicationLoader.applicationContext.getExternalCacheDir();
                if (file != null) {
                    return file;
                }
            } catch (Exception e) {
                FileLog.e("yahala", e);
            }
        }
        try {
            File file = ApplicationLoader.applicationContext.getCacheDir();
            if (file != null) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }
        return new File("");
    }

    public static int dp(float value) {
        return (int) Math.ceil(density * value);
    }

    public static float dpf2(float value) {
        return density * value;
    }

    public static int dpf(float value) {
        return (int) Math.ceil(density * value);
    }

    public static void checkDisplaySize() {
        try {
            Configuration configuration = ApplicationLoader.applicationContext.getResources().getConfiguration();
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    if (android.os.Build.VERSION.SDK_INT < 13) {
                        displaySize.set(display.getWidth(), display.getHeight());
                    } else {
                        display.getSize(displaySize);
                    }
                    FileLog.e("tmessages", "display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi);
                }
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = getConnectivityStatus(context);
        String status = null;
        if (conn == TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        //Canvas canvas = new Canvas(dest);
        //canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    public static Uri getMediaUri(String ringtoneTitle) {

        Uri parcialUri = Uri.parse("content://media/external/audio/media"); // also can be "content://media/internal/audio/media", depends on your needs
        Uri finalSuccessfulUri;

        RingtoneManager rm = new RingtoneManager(ApplicationLoader.applicationContext);
        rm.setType(RingtoneManager.TYPE_ALL);

        Cursor cursor = rm.getCursor();
        cursor = ApplicationLoader.applicationContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Media.TITLE);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            // FileLog.e("finalSuccessfulUri",cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
            if (ringtoneTitle.equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)))) {
                int ringtoneID = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));

                finalSuccessfulUri = Uri.withAppendedPath(parcialUri, "" + ringtoneID);
                // FileLog.e("finalSuccessfulUri",finalSuccessfulUri.getPath());
                return finalSuccessfulUri;
            }
            cursor.moveToNext();
        }
        return null;
    }

    public static boolean copyAssetFolder(AssetManager assetManager,
                                          String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return true; // ~ 10+ Mbps
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isHightBandwidthConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && isConnectionFast(info.getType(), info.getSubtype()));
    }
}
