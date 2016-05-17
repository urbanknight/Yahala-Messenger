package com.yahala.ImageLoader;

import com.yahala.ImageLoader.cache.LruBitmapCache;
import com.yahala.ImageLoader.model.ImageTagFactory;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.ui.ApplicationLoader;

/**
 * Created by user on 7/20/2014.
 */
public class ImageLoaderInitializer {
    public static ImageTagFactory imageTagFactory;
    private static ImageManager imageManager;

    public static ImageManager getImageLoader() {
        return imageManager;
    }

    private static volatile ImageLoaderInitializer Instance = null;

    public static ImageLoaderInitializer getInstance() {
        ImageLoaderInitializer localInstance = Instance;
        if (localInstance == null) {
            synchronized (ImageLoaderInitializer.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ImageLoaderInitializer();
                }
            }
        }
        return localInstance;
    }

    ImageLoaderInitializer() {
        normalImageManagerSettings();
        //initImageLoader();
    }

    private void normalImageManagerSettings() {
        imageManager = new ImageManager(ApplicationLoader.applicationContext, new LoaderSettings.SettingsBuilder()
                .withCacheManager(new LruBitmapCache(ApplicationLoader.applicationContext))
                .build(ApplicationLoader.applicationContext));

    }

    public void initImageLoader(int defaultImageResId, int width, int height) {
        imageTagFactory = ImageTagFactory.newInstance(OSUtilities.dp(width), OSUtilities.dp(height), defaultImageResId);
        imageTagFactory.setErrorImageId(defaultImageResId);
        imageTagFactory.setAnimation(R.anim.fade_in);
        imageTagFactory.setErrorImageId(defaultImageResId);
        imageTagFactory.setSaveThumbnail(true);


    }

    /**
     * There are different settings that you can use to customize
     * the usage of the image loader for your application.
     */
    @SuppressWarnings("unused")
    private void verboseImageManagerSettings() {
        LoaderSettings.SettingsBuilder settingsBuilder = new LoaderSettings.SettingsBuilder();

        //You can force the urlConnection to disconnect after every call.
        settingsBuilder.withDisconnectOnEveryCall(true);

        //We have different types of cache, check cache package for more info
        settingsBuilder.withCacheManager(new LruBitmapCache(ApplicationLoader.applicationContext));

        //You can set a specific read timeout
        settingsBuilder.withReadTimeout(30000);

        //You can set a specific connection timeout
        settingsBuilder.withConnectionTimeout(30000);

        //You can disable the multi-threading ability to download image
        settingsBuilder.withAsyncTasks(false);

        //You can set a specific directory for caching files on the sdcard
        //settingsBuilder.withCacheDir(new File("/something"));

        //Setting this to false means that file cache will use the url without the query part
        //for the generation of the hashname
        settingsBuilder.withEnableQueryInHashGeneration(false);

        LoaderSettings loaderSettings = settingsBuilder.build(ApplicationLoader.applicationContext);
        imageManager = new ImageManager(ApplicationLoader.applicationContext, loaderSettings);
    }

}
