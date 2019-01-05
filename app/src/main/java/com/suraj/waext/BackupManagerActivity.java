package com.suraj.waext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BackupManagerActivity extends AppCompatActivity {
    private String jid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_manager);

        jid = getIntent().getStringExtra("jid");

        if (jid == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }
        //setContactNameFromNumberAsync(jid);

        /*
        (findViewById(R.id.btnBackupChat)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        (new WhatsAppChatBackupManager()).backupChat(jid);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        Toast.makeText(getApplicationContext(), "Backup Successful", Toast.LENGTH_SHORT).show();
                    }
                }).execute();
            }
        });

        (findViewById(R.id.btnRestoreChat)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        (new WhatsAppChatBackupManager()).restoreChat(jid);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        Toast.makeText(getApplicationContext(), "Restored Successfully", Toast.LENGTH_SHORT).show();
                    }
                }).execute();
            }
        });

        if (ContextCompat.checkSelfPermission(BackupManagerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(BackupManagerActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }*/
    }
/*
    @SuppressLint("StaticFieldLeak")
    private void setContactNameFromNumberAsync(final String jid) {
        (new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String[] arr = new String[0];
                try {
                    arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/wa.db", "select display_name from wa_contacts where jid like " + '"' + jid + '"');
                } catch (WhatsAppDBException e) {
                    e.printStackTrace();
                }

                if (arr != null) {
                    return arr[0];
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                TextView textView = (TextView) findViewById(R.id.tvBackupContactName);

                if (s == null)
                    textView.setText("Cant retrieve contact name");
                else
                    textView.setText(s);

            }
        }).execute();
    }*/
}
