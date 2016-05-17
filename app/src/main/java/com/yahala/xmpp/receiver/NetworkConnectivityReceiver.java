package com.yahala.xmpp.receiver;

/**
 * Created by user on 7/1/2014.
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkConnectivityReceiver extends BroadcastReceiver {


    private boolean debug = false;

    private static void log(NetworkInfo networkInfo) {
        // @formatter:off
        System.out.print("networkName=" + networkInfo.getTypeName()
                + " available=" + networkInfo.isAvailable()
                + ", connected=" + networkInfo.isConnected()
                + ", connectedOrConnecting=" + networkInfo.isConnectedOrConnecting()
                + ", failover=" + networkInfo.isFailover()
                + ", roaming=" + networkInfo.isRoaming());
        // @formatter:on
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.print("onReceive; intent=" + intent.getAction());

        com.yahala.xmpp.Settings settings = com.yahala.xmpp.Settings.getInstance();
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (debug) {
            for (NetworkInfo networkInfo : cm.getAllNetworkInfo())
                log(networkInfo);
        }

        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            System.out.print("ActiveNetworkInfo follows:");
            log(activeNetworkInfo);
        }

        boolean connected;
        boolean networkTypeChanged;

        String lastActiveNetworkType = settings.getLastActiveNetwork();
        if (activeNetworkInfo != null) {
            // we have an active data connection
            String networkTypeName = activeNetworkInfo.getTypeName();
            connected = true;
            networkTypeChanged = false;
            if (!networkTypeName.equals(lastActiveNetworkType)) {
                System.out.print("networkTypeChanged current=" + networkTypeName + " last="
                        + lastActiveNetworkType);
                settings.setLastActiveNetwork(networkTypeName);
                networkTypeChanged = true;
            }
        } else {
            // we have *no* active data connection
            connected = false;
            if (lastActiveNetworkType.length() != 0) {
                networkTypeChanged = true;
            } else {
                networkTypeChanged = false;
            }
            settings.setLastActiveNetwork("");
        }


       /*LOG.d("Sending NETWORK_STATUS_CHANGED connected=" + connected + " changed="
                + networkTypeChanged);
        Intent i = new Intent(context, TransportService.class);
        i.setAction(Constants.ACTION_NETWORK_STATUS_CHANGED);
        i.putExtra(Constants.EXTRA_NETWORK_TYPE_CHANGED, networkTypeChanged);
        i.putExtra(Constants.EXTRA_NETWORK_CONNECTED, connected);
        context.startService(i);*/
    }
}
