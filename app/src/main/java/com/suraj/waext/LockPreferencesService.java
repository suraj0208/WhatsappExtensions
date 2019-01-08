package com.suraj.waext;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;

/**
 * Created by suraj on 30/8/16.
 */
public class LockPreferencesService extends IntentService {

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, new Notification());
    }

    public LockPreferencesService() {
        super("LockPreferencesService");
    }

    public LockPreferencesService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences.Editor editor = Utils.getEditor(this);
        int action = intent.getIntExtra("action", 0);
        String jid;

        //ToDo extract constants for this
        switch (action) {
            case 0:
                editor.putStringSet("lockedContacts", (Set) intent.getSerializableExtra("lockedContacts"));
                break;
            case 1:
                editor.putStringSet("hiddenGroups", (Set) intent.getSerializableExtra("hiddenGroups"));
                break;
            case 2:
                editor.putBoolean("showGroups", intent.getBooleanExtra("showGroups", true));
                break;
            case 3:
                editor.putStringSet("highlightedChats", (Set) intent.getSerializableExtra("highlightedChats"));
                break;
            case 4:
                jid = intent.getStringExtra("jid");
                showNotificationOfDeletedMessage(jid, false);
                break;
            case 5:
                jid = intent.getStringExtra("jid");
                showNotificationOfDeletedMessage(jid, true);
                break;
            case 6:
                showNotificationOfDeletedMessageFromUnknownContact();
                break;

        }
        editor.apply();

        try {
            (new Thread() {
                String cmd1[] = {"su", "-c",
                        "chmod 777 /data/data/com.suraj.waext"};

                Process p1 = Runtime.getRuntime().exec(cmd1);

                String cmd[] = {"su", "-c",
                        "chmod 664 /data/data/com.suraj.waext/shared_prefs/myprefs.xml"};
                Process p = Runtime.getRuntime().exec(cmd);

            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stopSelf();
    }

    @SuppressLint("StaticFieldLeak")
    private void showNotificationOfDeletedMessage(final String jid, final boolean isGroup) {
        (new AsyncTask<Void, Void, Object>() {

            @Override
            protected Object doInBackground(Void... voids) {
                try {
                    return Utils.getContactNameFromDatabase(jid);
                } catch (WhatsAppDBException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                String CHANNEL_ID = "waext_channel_02";// The id of the channel.

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(LockPreferencesService.this)
                        .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                        .setChannelId(CHANNEL_ID)
                        .setAutoCancel(true); // clear notification after click

                if (o instanceof WhatsAppDBException) {
                    mBuilder.setContentTitle(getString(R.string.message_deleted_unknown));
                } else if (isGroup) {
                    mBuilder.setContentTitle(getString(R.string.message_deleted_group, o));
                } else {
                    mBuilder.setContentTitle(getString(R.string.message_deleted, o));
                }

                Intent intent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                PendingIntent pi = PendingIntent.getActivity(LockPreferencesService.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                mBuilder.setContentIntent(pi);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = getString(R.string.app_name);// The user-visible name of the channel.
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                    mNotificationManager.createNotificationChannel(mChannel);
                }

                mNotificationManager.notify(7304, mBuilder.build());

            }
        }

        ).execute();
    }

    private void showNotificationOfDeletedMessageFromUnknownContact() {

        String CHANNEL_ID = "waext_channel_02";// The id of the channel.

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(LockPreferencesService.this)
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle(getString(R.string.message_deleted_unknown))  // title for notification
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(true); // clear notification after click
        Intent intent = new Intent(LockPreferencesService.this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(LockPreferencesService.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentIntent(pi);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        mNotificationManager.notify(7304, mBuilder.build());

    }

}