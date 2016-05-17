package com.yahala.xmpp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yahala.messenger.FileLog;

/**
 * Created by Wael a on 21/03/14.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {


            //  WakeLocker.acquire(context);
            //  ApplicationLoader.postInitApplication();

            // XMPPManager.xmppQueue.postRunnable(new Runnable() {
            //     @Override
            //      public void run() {
            //        WakeLocker.release();
            //     }
            //   }, 10000);
          /*  if (XMPPManager.getInstance().isConnected() &&
                    (XMPPManager.getInstance().presenceType != XMPPManager.DoNotDisturb ||
                            XMPPManager.getInstance().presenceType != XMPPManager.Away)) {
                XMPPManager.getInstance().setPresence(XMPPManager.Away, false);
            }*/
            /*final Alarm alarm = (Alarm) bundle.getSerializable("alarm");

            Intent newIntent;
            if (alarm.type.equals("regular")) {
                newIntent = new Intent(context, RegularAlarmActivity.class);
            } else if (alarm.type.equals("password")) {
                newIntent = new Intent(context, PasswordAlarmActivity.class);
            } else if (alarm.type.equals("movement")) {
                newIntent = new Intent(context, MovementAlarmActivity.class);
            } else {
                throw new Exception("Unknown alarm type");
            }
            newIntent.putExtra("alarm", alarm);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);*/

        } catch (Exception e) {
            // Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show();
            FileLog.e("Alarm Receiver", e);
        }
    }

}