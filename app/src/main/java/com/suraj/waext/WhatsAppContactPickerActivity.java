package com.suraj.waext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WhatsAppContactPickerActivity extends Activity implements WhiteListContactRowManager {
    private HashMap<String, Object> nameToNumberHashMap;
    private ArrayList<String> allJids;
    private ArrayList<String> allContacts;
    private ArrayList<String> searchedContacts;
    private ArrayList<String> searchedJids;
    private AsyncTask<Void, Void, Void> contactsFinder;

    private ListView lstviewPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_app_contact_picker);

        displayAllContacts();

        final EditText etSearchContact = (EditText) findViewById(R.id.etSearchContact);
        lstviewPicker = (ListView) findViewById(R.id.lstviewPicker);

        etSearchContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                findAndDisplayContacts(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    private void findAndDisplayContacts(final CharSequence contactName) {
        if (contactsFinder != null && !contactsFinder.isCancelled())
            contactsFinder.cancel(true);

        contactsFinder = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                searchedContacts.clear();
                searchedJids.clear();

                if (contactName.length() == 0) {
                    searchedContacts.addAll(allContacts);
                    searchedJids.addAll(allJids);
                    return null;
                }

                for (int i = 0; i < allContacts.size(); i++) {
                    if (isCancelled())
                        break;

                    if (allContacts.get(i).toLowerCase().contains(contactName.toString().toLowerCase())) {
                        searchedContacts.add(allContacts.get(i));
                        searchedJids.add(allJids.get(i));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void val) {
                lstviewPicker.setAdapter(new WhiteListAdapter(WhatsAppContactPickerActivity.this, searchedContacts, WhatsAppContactPickerActivity.this));
            }
        };
        contactsFinder.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void displayAllContacts() {
        lstviewPicker = findViewById(R.id.lstviewPicker);

        (new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                HashMap<String, String> groupNumberToNameHashMap = null;
                try {
                    groupNumberToNameHashMap = WhatsAppDatabaseHelper.getGroupNumberToNameHashMap();
                    HashMap<String, String> groupNameToNumberHashMap = WhatsAppDatabaseHelper.getGroupNameToNumberHashMap();

                    ArrayList<HashMap.Entry<String, String>> groupEntryList = new ArrayList<>(groupNumberToNameHashMap.entrySet());

                    Collections.sort(groupEntryList, new Comparator<HashMap.Entry<String, String>>() {
                        @Override
                        public int compare(HashMap.Entry<String, String> lhs, HashMap.Entry<String, String> rhs) {
                            return lhs.getValue().compareTo(rhs.getValue());
                        }
                    });

                    allJids = new ArrayList<>();

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
                        allJids.add(groupEntryList.get(i).getKey());
                    }

                    displayList.add("");
                    allJids.add("");

                    for (int i = 0; i < contactEntryList.size(); i++) {
                        String name = contactEntryList.get(i).getKey().trim();

                        if (name.length() == 0)
                            continue;

                        Object val = nameToNumberHashMap.get(name);
                        if (val instanceof List) {
                            for (Object s : (List) val) {
                                displayList.add(name + " ( " + s.toString().split("-")[0] + " )");
                                allJids.add(s.toString());
                            }
                        } else {
                            displayList.add(name);
                            allJids.add(val.toString());
                        }
                    }

                    allContacts = new ArrayList<>(displayList);
                    searchedContacts = new ArrayList<>(allContacts);
                    searchedJids = new ArrayList<>(allJids);

                    return displayList;
                } catch (WhatsAppDBException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object object) {
                if (Utils.toastAndExitIfWaDbException(object, WhatsAppContactPickerActivity.this)) {
                    return;
                }

                final ArrayList<String> contacts = (ArrayList<String>) object;
                lstviewPicker.setAdapter(new WhiteListAdapter(WhatsAppContactPickerActivity.this, contacts, WhatsAppContactPickerActivity.this));

                lstviewPicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (searchedContacts.get(position).trim().length() != 0) {
                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("contact", searchedJids.get(position));
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
