package com.yahala.ui.Adapters;

/**
 * Created by user on 4/24/2014.
 */


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.yahala.ui.ChatsActivity;

public class TabsPagerAdapter extends FragmentPagerAdapter {

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                // Top Rated fragment activity


                return new ChatsActivity();
            case 1:
                // Games fragment activity

                return new ChatsActivity();
            case 2:
                // Movies fragment activity
                return new ChatsActivity();
        }

        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 3;
    }

}