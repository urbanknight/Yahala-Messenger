package com.yahala.xmpp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.yahala.messenger.FileLog;
import com.yahala.ui.ApplicationLoader;

/**
 * Created by Wael a on 21/03/14.
 */
public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        FileLog.e("yahala", "service started");
        ApplicationLoader.postInitApplication();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void onDestroy() {
        FileLog.e("yahala", "service destroyed");

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("com.yahala.start");
            sendBroadcast(intent);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

