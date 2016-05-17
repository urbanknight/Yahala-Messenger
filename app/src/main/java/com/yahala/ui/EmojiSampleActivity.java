package com.yahala.ui;


import java.io.IOException;
import java.util.Collection;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.gson.JsonSyntaxException;
import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;

import com.yahala.android.emoji.EmojiGroup;
import com.yahala.android.emoji.EmojiManager;

import com.yahala.ui.Views.BaseFragment;

public class EmojiSampleActivity extends BaseFragment {

    private final static String TAG = "EmojiSample";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            /*if(!XmppManager.getInstance().isConnected()){
                XmppManager.getInstance().xmppRequestStateChange(ConnectionState.ONLINE);
            }*/
            FileLog.e("CallMainActivity", "onCreateView");
            if (fragmentView == null) {
                super.onCreate(savedInstanceState);
                fragmentView = inflater.inflate(R.layout.activity_emoji_sample, container, false);

                EditText tv = (EditText) fragmentView.findViewById(R.id.textview_emoji_display);

                EmojiManager eManager = EmojiManager.getInstance();

                try {
                    eManager.addJsonDefinitions("phantomsmiles.json", "phantom", "png");
                    eManager.addJsonDefinitions("extrasmiles.json", "extra_emoji", "png");
                    Collection<EmojiGroup> emojiGroups = eManager.getEmojiGroups();

                    /*EmojiPagerAdapter emojiPagerAdapter = new EmojiPagerAdapter(getActivity(), tv, new ArrayList<EmojiGroup>(emojiGroups));
                    ViewPager vPager = (ViewPager)fragmentView.findViewById(R.id.emojiPager);
                    vPager.setAdapter(emojiPagerAdapter);*/
                } catch (JsonSyntaxException jse) {
                    Log.e(TAG, "could not parse json", jse);
                } catch (IOException fe) {
                    Log.e(TAG, "could not load emoji definition", fe);
                } catch (Exception fe) {
                    Log.e(TAG, "could not load emoji definition", fe);
                }
            } else {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fragmentView;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //  super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.emoji_sample, menu);


    }

}
