package com.suraj.waext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by suraj on 1/9/16.
 */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String contactName = intent.getStringExtra("contactName");
        Log.i("Reminder name", contactName);

        ReminderService.jobsRemaining--;

        if (ReminderService.jobsRemaining == 0) {
            Intent stopIntent = new Intent(context, ReminderService.class);
            stopIntent.putExtra("stopService", true);
            context.startService(stopIntent);
        }

        Intent newIntent = new Intent();
        newIntent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.ReminderActivity"));
        newIntent.putExtra("contactName", contactName);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);

    }
}
