/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package com.yahala.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Rows.ConnectionsManager;
import com.yahala.xmpp.XMPPManager;

public class ScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            FileLog.e("yahala", "screen off");
            if (ConnectionsManager.lastPauseTime == 0) {
                ConnectionsManager.lastPauseTime = System.currentTimeMillis();
            }
            ApplicationLoader.isScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            if (UserConfig.getCurrentUser() != null) {
                if (!XMPPManager.getInstance().isConnected()) {
                    XMPPManager.getInstance().xmppRequestStateChange(ConnectionState.ONLINE);
                } else {
                    // XmppManager.getInstance().setPresence(XmppManager.Available);
                }
            }
            FileLog.e("yahala", "screen on");
            ConnectionsManager.resetLastPauseTime();
            ApplicationLoader.isScreenOn = true;
        }
    }
}
