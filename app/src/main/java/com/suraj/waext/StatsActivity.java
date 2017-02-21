package com.suraj.waext;

import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {
    private String jid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        jid = getIntent().getStringExtra("jid");

        if (jid == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        Utils.setContactNameFromDataase((TextView) findViewById(R.id.tvStatsContactName), jid);
        getMessagesCount((TextView) findViewById(R.id.tvMessagesSent), (TextView) findViewById(R.id.tvMessagesReceived), jid);
        getMessagesTimeSpan((TextView)findViewById(R.id.tvMessagesTimeSpan),jid);

    }

    private void getMessagesTimeSpan(final TextView textview, final String jid) {

        (new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select timestamp from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 order by timestamp limit 1;select receipt_device_timestamp from messages where key_remote_jid like " + '"' + jid + '"' + " and receipt_device_timestamp > 0 order by receipt_device_timestamp desc limit 1;");

                if (arr != null) {
                    return arr;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String[] s) {
                super.onPostExecute(s);
                if (s == null) {
                    return;
                }

                if (s.length<2) {
                    return;
                }

                try{
                    String first = getDateFromTimeStamp(Long.parseLong(s[0]));
                    String second = getDateFromTimeStamp(Long.parseLong(s[1]));

                    textview.setText(getResources().getString(R.string.messagesTimespan,first,second));


                }catch (NumberFormatException ex){
                    ex.printStackTrace();
                }



            }
        }).execute();

    }

    public void getMessagesCount(final TextView tvTo, TextView tvFrom, final String jid) {
        final TextView[] textViews = {tvTo, tvFrom};

        (new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select count(*),key_from_me from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 group by key_from_me;");

                if (arr != null) {
                    return arr;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String[] s) {
                super.onPostExecute(s);
                if (s == null) {
                    return;
                }
                for (String data : s) {
                    String rowData[] = data.split("\\|");

                    try {
                        int num = Integer.parseInt(rowData[1]);

                        textViews[num].setText(getResources().getStringArray(R.array.messagesTypeArray)[num] + rowData[0]);


                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException ne) {
                        ne.printStackTrace();
                    }

                }

            }
        }).execute();


    }

    public String getDateFromTimeStamp(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }



}
