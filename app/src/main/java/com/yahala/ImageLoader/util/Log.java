package com.yahala.ImageLoader.util;

public abstract class Log {

    private static final String TAG = "ImageLoader";

    public static final void info(String message) {
        try {
            android.util.Log.i(TAG, message);
        } catch (Exception e) {
            System.out.println(TAG + " " + message);
        }
    }

    public static final void warning(String message) {
        try {
            android.util.Log.w(TAG, message);
        } catch (Exception e) {
            System.out.println(TAG + " " + message);
        }
    }

}
