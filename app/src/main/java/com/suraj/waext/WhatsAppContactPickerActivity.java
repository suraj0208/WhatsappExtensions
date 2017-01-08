package com.suraj.waext;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WhatsAppContactPickerActivity extends AppCompatActivity implements WhiteListContactRowManager {
    HashMap<String, Object> nameToNumberHashMap;
    ArrayList<String> jids;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_app_contact_picker);

        final ListView lstviewPicker = (ListView) findViewById(R.id.lstviewPicker);

        (new AsyncTask<Void, Void, ArrayList<String>>() {

            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                List<HashMap<String, String>> groupInfoHashMaps = WhatsAppDatabaseHelper.getGroupInfoHashMap();

                if (groupInfoHashMaps == null)
                    return null;

                ArrayList<HashMap.Entry<String, String>> groupEntryList = new ArrayList<>(groupInfoHashMaps.get(0).entrySet());

                Collections.sort(groupEntryList, new Comparator<HashMap.Entry<String, String>>() {
                    @Override
                    public int compare(HashMap.Entry<String, String> lhs, HashMap.Entry<String, String> rhs) {
                        return lhs.getValue().compareTo(rhs.getValue());
                    }
                });

                jids = new ArrayList<>();

                nameToNumberHashMap = WhatsAppDatabaseHelper.getNameToNumberHashMap();

                ArrayList<HashMap.Entry<String, Object>> contactEntryList = new ArrayList<>(nameToNumberHashMap.entrySet());

                Collections.sort(contactEntryList, new Comparator<HashMap.Entry<String, Object>>() {
                    @Override
                    public int compare(HashMap.Entry<String, Object> lhs, HashMap.Entry<String, Object> rhs) {
                        return lhs.getKey().compareTo(rhs.getKey());
                    }
                });


                ArrayList<String> displayList = new ArrayList<>();

                for (int i = 0; i < groupEntryList.size(); i++) {
                    String name = groupEntryList.get(i).getValue().trim();

                    if (name.length() == 0)
                        continue;

                    displayList.add(name);
                    jids.add(groupEntryList.get(i).getKey());
                }

                displayList.add("");
                jids.add("");

                for (int i = 0; i < contactEntryList.size(); i++) {
                    String name = contactEntryList.get(i).getKey().trim();

                    if (name.length() == 0)
                        continue;

                    Object val = nameToNumberHashMap.get(name);
                    if (val instanceof List) {
                        for (Object s : (List) val) {
                            displayList.add(name + " ( " + s.toString().split("-")[0] + " )");
                            jids.add(s.toString());
                        }
                    } else {
                        displayList.add(name);
                        jids.add(val.toString());
                    }
                }


                return displayList;
            }

            @Override
            protected void onPostExecute(final ArrayList<String> strings) {
                lstviewPicker.setAdapter(new WhiteListAdapter(WhatsAppContactPickerActivity.this, strings, WhatsAppContactPickerActivity.this));

                lstviewPicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (strings.get(position).trim().length() != 0) {
                            Intent intent = new Intent();

                            Bundle bundle = new Bundle();
                            bundle.putString("contact", jids.get(position));
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
