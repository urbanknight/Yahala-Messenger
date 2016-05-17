package com.yahala.xmpp;

import android.content.SharedPreferences;

/**
 * Created by user on 7/1/2014.
 */
public class Settings {
    private static Settings sSettings;
    private final String DEBUG_NETWORK;
    private final String LAST_ACTIVE_NETWORK;
    private SharedPreferences mSharedPreferences;

    private Settings() {
        DEBUG_NETWORK = "DEBUG_NETWORK";
        LAST_ACTIVE_NETWORK = "LAST_ACTIVE_NETWORK";
    }

    public static synchronized Settings getInstance() {
        if (sSettings == null) {
            sSettings = new Settings();
        }
        return sSettings;
    }

    public String getLastActiveNetwork() {
        return mSharedPreferences.getString(LAST_ACTIVE_NETWORK, "");
    }

    public void setLastActiveNetwork(String network) {
        mSharedPreferences.edit().putString(LAST_ACTIVE_NETWORK, network).commit();
    }

}
