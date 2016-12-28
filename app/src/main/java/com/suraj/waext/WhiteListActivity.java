package com.suraj.waext;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import java.util.Set;

public class WhiteListActivity extends AppCompatActivity implements WhiteListContactRowManager {
    private ArrayList<String> whitelist;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private WhiteListAdapter whiteListAdapter;
    private Set<String> whitelistSet;
    private Set<Integer> deleteItemsSet;
    private HashMap<String, Object> numberToNameHashmap;
    private HashMap<String, Object> nameToNumberHashmap;

    private ListView lstviewwhitelist;
    private CheckBox[] deleteCheckBoxes;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        sharedPreferences = getSharedPreferences("myprefs", 1);
        editor = sharedPreferences.edit();

        //tricky -- create new hashset -> getstringset returns a reference
        whitelistSet = new HashSet<>(sharedPreferences.getStringSet("rd_whitelist", new HashSet<String>()));

        final WhatsAppContactManager whatsAppContactManager = new WhatsAppContactManager();

        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                numberToNameHashmap = whatsAppContactManager.getNumberToNameHashMap();
                nameToNumberHashmap = whatsAppContactManager.getNameToNumberHashMap();

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                buildArrayList();

                whiteListAdapter = new WhiteListAdapter(getApplicationContext(), whitelist, WhiteListActivity.this);

                lstviewwhitelist = (ListView) findViewById(R.id.lstviewwhitelistcontacts);

                lstviewwhitelist.setAdapter(whiteListAdapter);

            }

        }).execute();

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fbaddtowhitelist);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.ContactPicker"));
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 1);
            }
        });

        (findViewById(R.id.imgbtnremovecontact)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (whitelist.size() == 0) {
                    Toast.makeText(getApplicationContext(), "List is empty.", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (fab.getVisibility() == View.VISIBLE) {
                    for (CheckBox checkBox : deleteCheckBoxes)
                        checkBox.setVisibility(View.VISIBLE);

                    fab.setVisibility(View.INVISIBLE);
                    v.setBackground(getDrawable(R.mipmap.ic_done_black_24dp));
                } else {
                    (new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            int i = 0;

                            for (Iterator<String> iterator = whitelist.iterator(); iterator.hasNext(); ) {
                                String name = iterator.next();
                                if (deleteItemsSet.contains(i)) {

                                    Object value = nameToNumberHashmap.get(name);

                                    if (value instanceof String)
                                        whitelistSet.remove(value.toString());
                                    else if (value instanceof List) {
                                        for (Object number : (List) value)
                                            whitelistSet.remove(number.toString());
                                    }
                                    iterator.remove();
                                }

                                i++;
                            }

                            deleteItemsSet.clear();

                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);

                            lstviewwhitelist.setAdapter(null);
                            lstviewwhitelist.setAdapter(new WhiteListAdapter(getApplicationContext(), whitelist, WhiteListActivity.this));
                            editor.putStringSet("rd_whitelist", whitelistSet);
                            editor.apply();


                        }
                    }).execute();

                    for (CheckBox checkBox : deleteCheckBoxes)
                        checkBox.setVisibility(View.INVISIBLE);

                    fab.setVisibility(View.VISIBLE);
                    v.setBackground(getDrawable(R.mipmap.ic_delete_black_24dp));

                }


            }
        });
    }

    private void buildArrayList() {
        if (whitelist == null)
            whitelist = new ArrayList<>();
        else
            whitelist.clear();

        if (numberToNameHashmap == null) {
            Log.e("com.suraj.waext", "may be su failed");
            WhiteListActivity.this.finish();
            return;
        }

        for (String number : whitelistSet) {
            if (numberToNameHashmap.get(number) != null)
                whitelist.add(numberToNameHashmap.get(number).toString());
            else{
                //Log.i("com.suraj","in whitelist act: .get null " + numberToNameHashmap.size());
                //Log.i("com.suraj","whitelist set size " + whitelistSet.size());
            }

        }
        Collections.sort(whitelist);

        deleteCheckBoxes = new CheckBox[whitelist.size()];
        deleteItemsSet = new HashSet<>();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            String number = data.getExtras().get("contact").toString().split("@")[0];

            whitelistSet.add(number);

            buildArrayList();

            lstviewwhitelist.setAdapter(null);
            lstviewwhitelist.setAdapter(new WhiteListAdapter(getApplicationContext(), whitelist, WhiteListActivity.this));

            editor.putStringSet("rd_whitelist", whitelistSet);

            editor.apply();

        }
    }

    @Override
    public void onInflateContactRow(View view, final List<String> whitelist, final int position) {
        if (nameToNumberHashmap == null || numberToNameHashmap == null) {
            Toast.makeText(getApplicationContext(), "Failed to get contact info. Make sure you have root", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView contactName = (TextView) view.findViewById(R.id.tvcontactname);
        contactName.setText(whitelist.get(position));

        final CheckBox deleteCheckBox = (CheckBox) view.findViewById(R.id.chkboxdeletewhitelistcontact);

        deleteCheckBoxes[position] = deleteCheckBox;

        deleteCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteCheckBox.isChecked())
                    deleteItemsSet.add(position);
                else
                    deleteItemsSet.remove(position);
            }
        });
    }
}