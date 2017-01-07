package com.suraj.waext;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WhatsAppContactPickerActivity extends AppCompatActivity implements WhiteListContactRowManager {
    HashMap<String, Object> nameToNumberHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_app_contact_picker);

        final ListView lstviewPicker = (ListView) findViewById(R.id.lstviewPicker);


        (new AsyncTask<Void, Void, ArrayList<String>>() {

            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                ArrayList<String> groups = new ArrayList<>(WhatsAppDatabaseHelper.getGroupInfoHashMap().keySet());

                ArrayList<String> contacts = new ArrayList<>();

                nameToNumberHashMap = WhatsAppDatabaseHelper.getNameToNumberHashMap();


                for (String name : nameToNumberHashMap.keySet()) {
                    name = name.trim();

                    if (name.length() == 0)
                        continue;

                    Object val = nameToNumberHashMap.get(name);
                    if (val instanceof List) {
                        for (Object s : (List) val)
                            contacts.add(name + " ( " + s.toString().split("-")[0] + " )");
                    } else
                        contacts.add(name);

                }

                Collections.sort(groups);
                Collections.sort(contacts);

                groups.add("");
                groups.addAll(contacts);

                return groups;
            }

            @Override
            protected void onPostExecute(final ArrayList<String> strings) {
                lstviewPicker.setAdapter(new WhiteListAdapter(WhatsAppContactPickerActivity.this, strings, WhatsAppContactPickerActivity.this));

                if (nameToNumberHashMap == null) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.sqliteMissing), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                lstviewPicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (strings.get(position).trim().length() != 0) {
                            Intent intent = new Intent();

                            Bundle bundle = new Bundle();
                            bundle.putString("contact", nameToNumberHashMap.get(strings.get(position)).toString());
                            intent.putExtras(bundle);

                            intent.putExtras(bundle);
                            setResult(RESULT_OK, intent);
                            finish();
                        }


                    }
                });

            }
        }).execute();


    }

    @Override
    public void onInflateContactRow(View view, List<String> whitelist, int position) {
        ((TextView) view.findViewById(R.id.tvcontactname)).setText(whitelist.get(position));
    }
}
