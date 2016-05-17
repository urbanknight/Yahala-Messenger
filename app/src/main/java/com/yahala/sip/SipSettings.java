package com.yahala.sip;

/**
 * Created by user on 11/4/2015.
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.yahala.messenger.R;

/**
 * Handles SIP authentication settings for the Walkie Talkie app.
 */
public class SipSettings extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Note that none of the preferences are actually defined here.
        // They're all in the XML file res/xml/preferences.xml.
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}