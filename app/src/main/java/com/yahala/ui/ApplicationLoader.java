package com.yahala.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.ViewConfiguration;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.yahala.android.OSUtilities;
import com.yahala.android.NotificationsController;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NativeLoader;
import com.yahala.android.ScreenReceiver;
import com.yahala.messenger.UserConfig;
import com.yahala.android.emoji.EmojiManager;
import com.yahala.sip.SIPManager;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.xmpp.MessagesController;
import com.yahala.xmpp.NotificationsService;
import com.yahala.xmpp.XMPPManager;

import com.yahala.messenger.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.android.gms.gcm.GoogleCloudMessaging;
//import org.telegram.messenger.MessagesController;

public class ApplicationLoader extends Application {
    public static ArrayList<BaseFragment> fragmentsStack = new ArrayList<BaseFragment>();


    private AtomicInteger msgId = new AtomicInteger();
    private String regid;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static Drawable cachedWallpaper = null;

    public static volatile Context applicationContext = null;
    public static volatile Handler applicationHandler = null;
    private static volatile boolean applicationInited = false;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;


    public static void postInitApplication() {
        if (applicationInited) {
            if (UserConfig.getCurrentUser() != null) {
                // SIPManager.getInstance().initializeManager();
                XMPPManager.getInstance().maybeStartReconnect();
            }

            return;
        }

        NotificationsController.getInstance().setBadgeEnabled(true);
        addRingtoonsToMediaStore();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .resetViewBeforeLoading(true)
                        //.displayer(new FadeInBitmapDisplayer(300))
                .imageScaleType(ImageScaleType.EXACTLY)
                        //.bitmapConfig(Bitmap.Config.RGB_565)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(applicationContext)
                .defaultDisplayImageOptions(options)
                .threadPoolSize(3)
                .tasksProcessingOrder(QueueProcessingType.FIFO)
                .diskCacheExtraOptions(480, 480, null)
                        //.imageDownloader(new CustomImageDownloader(getApplicationContext()))
                .build();
        ImageLoader.getInstance().init(config);
        applicationInited = true;


        try {
            LocaleController.getInstance();
            EmojiManager.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("yahala", "screen state = " + isScreenOn);
        } catch (Exception e) {
            FileLog.e("yahala", e);
        }

        UserConfig.loadConfig();
        if (UserConfig.getCurrentUser() != null) {
            boolean changed = false;
            SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
            int v = preferences.getInt("v", 0);
            if (v != 1) {
                SharedPreferences preferences2 = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences2.edit();
                if (preferences.contains("view_animations")) {
                    editor.putBoolean("view_animations", preferences.getBoolean("view_animations", false));
                }
                if (preferences.contains("selectedBackground")) {
                    editor.putInt("selectedBackground", preferences.getInt("selectedBackground", 1000001));
                }
                if (preferences.contains("selectedColor")) {
                    editor.putInt("selectedColor", preferences.getInt("selectedColor", 0));
                }
                if (preferences.contains("fons_size")) {
                    editor.putInt("fons_size", preferences.getInt("fons_size", 16));
                }
                editor.commit();
                editor = preferences.edit();
                editor.putInt("v", 1);
                editor.remove("view_animations");
                editor.remove("selectedBackground");
                editor.remove("selectedColor");
                editor.remove("fons_size");
                editor.commit();
            }


            MessagesController.getInstance().users.put(UserConfig.currentUser.phone, UserConfig.getCurrentUser());

            // ConnectionsManager.getInstance().applyCountryPortNumber(UserConfig.getCurrentUser().phone);
            XMPPManager.getInstance().maybeStartReconnect();
        }
        if (!PreferenceManager.getDefaultSharedPreferences(
                applicationContext)
                .getBoolean("installed", false)) {
            PreferenceManager.getDefaultSharedPreferences(
                    applicationContext)
                    .edit().putBoolean("installed", true).commit();

            //  OSUtilities.copyAssetFolder(applicationContext.getAssets(), "onionClub",
            //         "/data/data/com.yahala.app/stickers/onionClub");
        }
        ApplicationLoader app = (ApplicationLoader) ApplicationLoader.applicationContext;
        app.initPlayServices();
        FileLog.e("yahala", "app initied");

    }

    public static void startPushService() {
        SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
        if (!com.yahala.xmpp.ContactsController.getInstance().contactsLoaded || com.yahala.xmpp.ContactsController.getInstance().first) {
            //FileLog.e("Test", "contacts not Loaded readContacts");
            com.yahala.xmpp.ContactsController.getInstance().readContacts(false);
            com.yahala.xmpp.ContactsController.getInstance().first = false;
        }
        if (preferences.getBoolean("pushService", true)) {

            applicationContext.startService(new Intent(applicationContext, NotificationsService.class));

            if (android.os.Build.VERSION.SDK_INT >= 19) {
//              Calendar cal = Calendar.getInstance();
//              PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
//              AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
//              alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 30000, pintent);

                PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
                AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
                alarm.cancel(pintent);
            }
            /*alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +
                    1000 * 30 , 1000 * 30 , pintent);*/

            /*AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(applicationContext, AlarmReceiver.class);
            PendingIntent pintent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarm.cancel(pintent);*/
            //alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, pintent);

        } else {
            stopPushService();
        }
    }

    public static void stopPushService() {
        applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));

        PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
        AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
    }

    private static void addRingtoonsToMediaStore() {
        File RingtonesDir = new File(Environment.getExternalStorageDirectory().toString() + "/Ringtones");
        if (!RingtonesDir.exists()) {
            RingtonesDir.mkdir();
        }

        File file = new File(Environment.getExternalStorageDirectory().toString() + "/Ringtones", "yahala_message.ogg");
        if (!file.exists()) {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                int read = 0;
                byte[] bytes = new byte[1024];
                InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("yahala_message.ogg");
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } catch (Exception e) {
                FileLog.e("Yahala", e);
            }
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());

            ApplicationLoader.applicationContext.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + file.getAbsolutePath() + "\"", null);

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            values.put(MediaStore.MediaColumns.TITLE, "Yahala Incoming Message");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
            values.put(MediaStore.Audio.Media.ARTIST, "Mamon ");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/ogg");
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
            values.put(MediaStore.Audio.Media.IS_ALARM, true);
            values.put(MediaStore.Audio.Media.IS_MUSIC, false);


            Uri newUri = ApplicationLoader.applicationContext.getContentResolver().insert(uri, values);

            RingtoneManager.setActualDefaultRingtoneUri(ApplicationLoader.applicationContext, RingtoneManager.TYPE_NOTIFICATION, newUri);

        }
    }

    public static int getAppVersion() {
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        NativeLoader.initNativeLibs(applicationContext);

        applicationHandler = new Handler(applicationContext.getMainLooper());

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        startPushService();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            OSUtilities.checkDisplaySize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayServices() {
       /* if (checkPlayServices()) {
            // gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId();

            if (regid.length() == 0) {
                registerInBackground();
            } else {
                sendRegistrationIdToBackend(false);
            }
        } else {
            FileLog.d("tmessages", "No valid Google Play Services APK found.");
        }*/
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
        /*if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("tmessages", "This device is not supported.");
            }
            return false;
        }
        return true;*/
    }

    private String getRegistrationId() {
        final SharedPreferences prefs = getGCMPreferences(applicationContext);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            FileLog.d("Yahala", "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            FileLog.d("Yahala", "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(ApplicationLoader.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private void registerInBackground() {
        AsyncTask<String, String, Boolean> task = new AsyncTask<String, String, Boolean>() {
            @Override
            protected Boolean doInBackground(String... objects) {
               /* if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(applicationContext);
                }*/
                int count = 0;
                while (count < 1000) {
                    try {
                        count++;
                        // regid = gcm.register(BuildVars.GCM_SENDER_ID);
                        sendRegistrationIdToBackend(true);
                        storeRegistrationId(applicationContext, regid);
                        return true;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    try {
                        if (count % 20 == 0) {
                            Thread.sleep(60000 * 30);
                        } else {
                            Thread.sleep(5000);
                        }
                    } catch (InterruptedException e) {
                        FileLog.e("tmessages", e);
                    }
                }
                return false;
            }
        }.execute(null, null, null);
    }

    private void sendRegistrationIdToBackend(final boolean isNew) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                UserConfig.pushString = regid;
                UserConfig.registeredForPush = !isNew;
                UserConfig.saveConfig(false);
                if (UserConfig.getClientUserId() != 0) {
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // MessagesController.getInstance().registerForPush(regid);
                        }
                    });
                }
            }
        });
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion();
        FileLog.e("tmessages", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
}

