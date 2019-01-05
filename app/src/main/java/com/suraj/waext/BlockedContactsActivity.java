package com.suraj.waext;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class BlockedContactsActivity extends AppCompatActivity implements WhiteListContactRowManager {

    private List<String> blockedContactsList;

    private HashSet<String> blockedContactsSet;
    private HashSet<Integer> deleteItemsSet;

    private HashMap<String, Object> numberToNameHashMap;
    private HashMap<String, Object> nameToNumberHashMap;
    private HashMap<String, String> groupNumberToNameHashMap;
    private HashMap<String, String> groupNameToNumberHashMap;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private WhiteListAdapter blockedContactListAdapter;
    private ListView listViewBlockedContacts;
    private CheckBox[] deleteCheckBoxes;

    private FloatingActionButton fab;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_contacts);

        sharedPreferences = Utils.getSharedPreferences(this);
        editor = Utils.getEditor(this);

        listViewBlockedContacts = (ListView) findViewById(R.id.lstviewBlockedContacts);


        //tricky -- create new hashset -> getstringset returns a reference
        blockedContactsSet = new HashSet<>(sharedPreferences.getStringSet("blockedContactList", new HashSet<String>()));

        if (blockedContactsSet.size() == 0)
            Toast.makeText(getApplicationContext(), "List is empty.", Toast.LENGTH_SHORT).show();

        fab = findViewById(R.id.fbAddToBlockedList);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BlockedContactsActivity.this, WhatsAppContactPickerActivity.class), 1);
            }
        });


        (new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    numberToNameHashMap = WhatsAppDatabaseHelper.getNumberToNameHashMap();
                    nameToNumberHashMap = WhatsAppDatabaseHelper.getNameToNumberHashMap();
                    groupNumberToNameHashMap = WhatsAppDatabaseHelper.getGroupNumberToNameHashMap();
                    groupNameToNumberHashMap = WhatsAppDatabaseHelper.getGroupNameToNumberHashMap();
                } catch (WhatsAppDBException ex) {
                    return ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object object) {
                super.onPostExecute(object);

                if (Utils.toastAndExitIfWaDbException(object, BlockedContactsActivity.this)) {
                    return;
                }

                buildArrayList();

                blockedContactListAdapter = new WhiteListAdapter(getApplicationContext(), blockedContactsList, BlockedContactsActivity.this);

                listViewBlockedContacts.setAdapter(blockedContactListAdapter);

            }

        }).execute();


    }

    private void buildArrayList() {
        if (blockedContactsList == null)
            blockedContactsList = new ArrayList<>();
        else
            blockedContactsList.clear();

        if (numberToNameHashMap == null) {
            Log.e("com.suraj.waext", "numbers null may be su failed");
            BlockedContactsActivity.this.finish();
            return;
        }

        if (groupNameToNumberHashMap == null || groupNumberToNameHashMap == null) {
            Log.e("com.suraj.waext", "groups null may be su failed");
            BlockedContactsActivity.this.finish();
            return;
        }

        for (String number : blockedContactsSet) {
            if (numberToNameHashMap.get(number) != null) {
                blockedContactsList.add(numberToNameHashMap.get(number).toString());
            } else if (groupNumberToNameHashMap.get(number) != null) {
                blockedContactsList.add(groupNumberToNameHashMap.get(number));
            }
        }

        Collections.sort(blockedContactsList);

        deleteCheckBoxes = new CheckBox[blockedContactsList.size()];
        deleteItemsSet = new HashSet<>();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            String number = data.getExtras().get("contact").toString().split("@")[0];
            blockedContactsSet.add(number);

            buildArrayList();

            listViewBlockedContacts.setAdapter(null);
            listViewBlockedContacts.setAdapter(new WhiteListAdapter(getApplicationContext(), blockedContactsList, BlockedContactsActivity.this));

            editor.putStringSet("blockedContactList", blockedContactsSet);

            editor.apply();

        }
    }

    @Override
    public void onInflateContactRow(View view, List<String> contactList, final int position) {

        if (nameToNumberHashMap == null || numberToNameHashMap == null) {
            Toast.makeText(getApplicationContext(), "Failed to get contact info. Make sure you have root", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView contactName = (TextView) view.findViewById(R.id.tvcontactname);
        contactName.setText(contactList.get(position));

        deleteCheckBoxes[position] = (CheckBox) view.findViewById(R.id.chkboxdeletewhitelistcontact);

        if (fab.getVisibility() != View.VISIBLE)
            deleteCheckBoxes[position].setVisibility(View.VISIBLE);

        deleteCheckBoxes[position].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;

                if (checkBox.isChecked())
                    deleteItemsSet.add(position);
                else
                    deleteItemsSet.remove(position);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_block_activity, menu);
        return true;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_remove) {

            if (blockedContactsList.size() == 0) {
                Toast.makeText(getApplicationContext(), "List is empty.", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
            }

            if (fab.getVisibility() == View.VISIBLE) {
                fab.setVisibility(View.INVISIBLE);

                for (CheckBox checkBox : deleteCheckBoxes) {
                    if (checkBox != null)
                        checkBox.setVisibility(View.VISIBLE);
                }

                item.setIcon(getDrawable(R.mipmap.ic_done_white_24dp));

            } else {
                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {

                        int i = 0;

                        for (Iterator<String> iterator = blockedContactsList.iterator(); iterator.hasNext(); ) {
                            String name = iterator.next();
                            if (deleteItemsSet.contains(i)) {

                                Object value = nameToNumberHashMap.get(name);

                                boolean localIsGroup = false;

                                if (value == null) {// value is null -- it may be a group
                                    value = groupNameToNumberHashMap.get(name);
                                    localIsGroup = true;
                                }
                                try {
                                    if (value instanceof String) {
                                        blockedContactsSet.remove(value.toString());
                                        WhatsAppDatabaseHelper.clearNullItemsFromMessages(localIsGroup ? value.toString() + "@g.us" : value + "@s.whatsapp.net");
                                    } else if (value instanceof List) {
                                        for (Object number : (List) value) {
                                            blockedContactsSet.remove(number.toString());
                                            WhatsAppDatabaseHelper.clearNullItemsFromMessages(localIsGroup ? number.toString() + "@g.us" : number + "@s.whatsapp.net");
                                        }
                                    }
                                    iterator.remove();
                                } catch (WhatsAppDBException ex) {
                                    Log.e(ExtModule.PACKAGE_NAME, ex.getMessage());
                                }

                            }

                            i++;
                        }
                        deleteItemsSet.clear();

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                        listViewBlockedContacts.setAdapter(null);
                        listViewBlockedContacts.setAdapter(new WhiteListAdapter(getApplicationContext(), blockedContactsList, BlockedContactsActivity.this));
                        editor.putStringSet("blockedContactList", blockedContactsSet);
                        editor.apply();

                    }
                }).execute();

                for (CheckBox checkBox : deleteCheckBoxes)
                    if (checkBox != null)
                        checkBox.setVisibility(View.INVISIBLE);

                fab.setVisibility(View.VISIBLE);
                item.setIcon(getDrawable(R.mipmap.ic_delete_white_24dp));

            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setPreferencesRW(this);
    }

}
