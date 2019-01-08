package com.suraj.waext;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

/**
 * Created by suraj on 1/9/16.
 */
public class ReminderService extends Service {

    public static Handler handler;
    public static int jobsRemaining = 0;
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getBooleanExtra("stopService", false)) {
            this.stopForeground(true);
            this.stopSelf();
            ReminderService.isRunning = false;
        }

        ReminderService.isRunning = true;

        (new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                    }
                };
                Looper.loop();
            }
        }).start();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        String CHANNEL_ID = "waext_channel_01";// The id of the channel.

        builder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("WhatsApp Reminder Service.")
                .setChannelId(CHANNEL_ID)
                .setContentText("Touch to configure.");

        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 965778, startIntent, 0);

        builder.setContentIntent(pendingIntent);

        startForeground(965778, builder.build());

        return START_REDELIVER_INTENT;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ReminderService.isRunning = false;


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
