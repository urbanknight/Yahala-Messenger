package com.yahala.android;

/**
 * Created by user on 4/29/2014.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.yahala.messenger.FileLog;
import com.yahala.xmpp.NotificationsService;
import com.yahala.xmpp.PreferenceConstants;
import com.yahala.xmpp.XMPPManager;

public class Internet extends BroadcastReceiver {
    private static int networkType = -1;

    public static void initNetworkStatus(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        networkType = -1;
        if (networkInfo != null) {
            FileLog.e("Test Internet", "Init: ACTIVE NetworkInfo: " + networkInfo.toString());
            if (networkInfo.isConnected()) {
                networkType = networkInfo.getType();
            }
        }
        FileLog.e("Test Internet", "initNetworkStatus -> " + networkType);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        FileLog.e("Test Internet", "BroadcastReceiver");
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            FileLog.e("Test Internet", "System shutdown, stopping yahala.");
            Intent xmppServiceIntent = new Intent(context, NotificationsService.class);
            context.stopService(xmppServiceIntent);

        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            //ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //for (NetworkInfo network : cm.getAllNetworkInfo()) {
            //    FileLog.e("Internet ConnectivityManager","available=" + (network.isAvailable()?1:0)
            //            + ", connected=" + (network.isConnected()?1:0)
            //           + ", connectedOrConnecting=" + (network.isConnectedOrConnecting()?1:0)
            //            + ", failover=" + (network.isFailover()?1:0)
            //            + ", roaming=" + (network.isRoaming()?1:0)
            //           + ", networkName=" + network.getTypeName());
            //}
            FileLog.e("Test Internet action", intent.getAction());
            boolean connstartup = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PreferenceConstants.CONN_STARTUP, false);
            //if (!connstartup) // ignore event, we are not running
            //return;

            //refresh DNS servers from android prefs
            try {

                //  org.xbill.DNS.ResolverConfig.refresh();
                // org.xbill.DNS.Lookup.refreshDefault();
            } catch (Exception e) {
                // sometimes refreshDefault() will cause a NetworkOnMainThreadException;
                // ignore and hope for the best.
                FileLog.e("Test Internet", "DNS init failed: " + e);
            }
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            boolean isConnected = (networkInfo != null) && (networkInfo.isConnected() == true);


            boolean wasConnected = (networkType != -1);
            if (wasConnected && !isConnected) {
                FileLog.e("Test Internet", "we got disconnected");
                networkType = -1;
                XMPPManager.getInstance().xmppRequestStateChange(ConnectionState.DISCONNECTED);
                //XmppManager.getInstance().connectionState = ConnectionState.DISCONNECTED;
                // XmppManager.getInstance().destroy();
                //xmppServiceIntent.setAction("disconnect");
            } else if (isConnected && (networkInfo.getType() != networkType)) {
                FileLog.e("Test Internet", "we got (re)connected: " + networkInfo.toString());
                networkType = networkInfo.getType();

                XMPPManager.getInstance().mCurrentRetryCount = 0;
                XMPPManager.getInstance().connectionState = ConnectionState.DISCONNECTED;
                XMPPManager.getInstance().xmppRequestStateChange(ConnectionState.ONLINE);


                //xmppServiceIntent.setAction("reconnect");
            } else if (isConnected && (networkInfo.getType() == networkType)) {
                FileLog.e("Test Internet", "we stay connected, sending a ping");
                try {
                    XMPPManager.getInstance().mPingManager.pingMyServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // xmppServiceIntent.setAction("ping");
            } else {
                XMPPManager.getInstance().xmppRequestStateChange(ConnectionState.RECONNECT_NETWORK);
                return;
            }
            //context.startService(xmppServiceIntent);
        }


        if (intent.getAction().equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE")) {
            if (isInternet(context)) {
                //  Utilities.stageQueue.postRunnable(new Runnable() {
                //      @Override
                //     public void run() {
//
                //        }
                //     });*/
                // XmppManager.getInstance().AuthenticateUser();
            }
        }
    }

    public boolean isInternet(Context context) {
        ConnectivityManager IM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = IM.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
}
