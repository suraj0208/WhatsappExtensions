package com.suraj.waext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
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

                } else if (!checkBox.isChecked() && !offToast) {
                    Toast.makeText(context, offMessage, Toast.LENGTH_SHORT).show();
                }
                editor.apply();
            }
        });
    }

    public static void setContactNameFromDataase(final TextView textView, final String jid) {
        (new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/wa.db", "select display_name from wa_contacts where jid like " + '"' + jid + '"');

                if (arr != null) {
                    return arr[0];
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s == null)
                    textView.setText("Cant retrieve contact name");
                else
                    textView.setText(s);

            }
        }).execute();
    }

    public static void setPreferencesRW(Context context) {
        File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        final File prefsFile = new File(prefsDir, Utils.MYPREFS + ".xml");
        prefsFile.setReadable(true, false);
        prefsFile.setWritable(true, false);
        try {
            (new Thread() {
                String cmd[] = {"su", "-c",
                        "chmod 666 " + prefsFile};
                Process p = Runtime.getRuntime().exec(cmd);
            }).start();
        } catch (Exception e) {

        }
    }
}
