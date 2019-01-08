package com.suraj.waext;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;

/**
 * Created by suraj on 24/1/17.
 */
public class Utils {
    public static final String MYPREFS = "myprefs";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private static void initPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(MYPREFS, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        initPreferences(context);
        return sharedPreferences;
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        initPreferences(context);
        return editor;
    }

    public static void setUpCheckBox(final Context context, final CheckBox checkBox, final String prefname, final boolean onToast, final String onMessage, final boolean offToast, final String offMessage) {
        initPreferences(context);

        checkBox.setChecked(sharedPreferences.getBoolean(prefname, false));

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long t = SystemClock.elapsedRealtime();

                /*if (t < 2 * 60 * 1000) {
                    checkBox.setChecked(!checkBox.isChecked());
                    Toast.makeText(getApplicationContext(), R.string.wait_message, Toast.LENGTH_SHORT).show();
                    return;
                }*/

                editor.putBoolean(prefname, checkBox.isChecked());

                if (checkBox.isChecked() && onToast) {
                    Toast.makeText(context, onMessage, Toast.LENGTH_SHORT).show();

                } else if (!checkBox.isChecked() && offToast) {
                    Toast.makeText(context, offMessage, Toast.LENGTH_SHORT).show();
                }
                editor.apply();
            }
        });
    }

    public static String getContactNameFromDatabase(final String jid) throws WhatsAppDBException {
        String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/wa.db", "select display_name from wa_contacts where jid like " + '"' + jid + '"');
        if (arr.length > 0) {
            return arr[0];
        } else {
            throw new WhatsAppDBException("No contact found for given number");
        }
    }

    public static void setPreferencesRW(Context context) {
        String datadir = context.getApplicationInfo().dataDir;
        System.out.println(datadir);

        // marshmallow+
        File p = new File(datadir + "/shared_prefs/" + Utils.MYPREFS + ".xml");
        if (p.exists()) {
            p.setReadable(true, false);
        }

        // nougat+ extra fix
        p = new File(datadir);
        if (p.exists() && p.isDirectory()) {
            p.setReadable(true, false);
            p.setExecutable(true, false);
        }
        /*final File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");

        final File prefsFile = new File(prefsDir, Utils.MYPREFS + ".xml");

        final File dataDir = new File(context.getApplicationInfo().dataDir);

        try {
            (new Thread() {
                String cmd1[] = {"su", "-c",
                        "chmod 777 /data/data/com.suraj.waext" };

                Process p1  = Runtime.getRuntime().exec(cmd1);

                String cmd[] = {"su", "-c",
                        "chmod -R 777 /data/data/com.suraj.waext"};
                Process p = Runtime.getRuntime().exec(cmd);

            }).start();
        } catch (Exception e) {

        }*/

    }

    public static boolean toastAndExitIfWaDbException(Object object, Activity activity) {
        if (object instanceof WhatsAppDBException) {
            Toast.makeText(activity, ((WhatsAppDBException) object).getMessage(), Toast.LENGTH_SHORT).show();
            activity.finish();
            return true;
        }
        return false;
    }
}
