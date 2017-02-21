package com.suraj.waext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by suraj on 24/1/17.
 */
public class Utils {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private static void initPreferences(Context context){
        if(sharedPreferences==null){
            sharedPreferences=context.getSharedPreferences("myprefs", 1);
            editor=sharedPreferences.edit();
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

    public static void setUpCheckBox(final Context context,final CheckBox checkBox, final String prefname, final boolean onToast, final String onMessage, final boolean offToast, final String offMessage) {
        initPreferences(context);

        if (sharedPreferences.getBoolean(prefname, false))
            checkBox.setChecked(true);
        else
            checkBox.setChecked(false);

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long t = SystemClock.elapsedRealtime();

                /*if (t < 2 * 60 * 1000) {
                    checkBox.setChecked(!checkBox.isChecked());
                    Toast.makeText(getApplicationContext(), R.string.wait_message, Toast.LENGTH_SHORT).show();
                    return;
                }*/

                if (checkBox.isChecked()) {
                    editor.putBoolean(prefname, true);

                    if (onToast) {
                        Toast.makeText(context, onMessage, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    editor.putBoolean(prefname, false);

                    if (offToast) {
                        Toast.makeText(context, offMessage, Toast.LENGTH_SHORT).show();
                    }
                }
                editor.apply();
            }
        });
    }

    public static void setContactNameFromDataase(final TextView textView, final String jid){
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
}
