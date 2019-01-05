package com.suraj.waext;

import android.annotation.SuppressLint;
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
import android.widget.Switch;
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
    private ArrayList<String> whiteList;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private WhiteListAdapter whiteListAdapter;
    private Set<String> whiteListSet;
    private Set<Integer> deleteItemsSet;
    private HashMap<String, Object> numberToNameHashMap;
    private HashMap<String, Object> nameToNumberHashMap;
    private HashMap<String, String> groupNumberToNameHashMap;
    private HashMap<String, String> groupNameToNumberHashMap;

    private ListView lstviewWhiteList;
    private CheckBox[] deleteCheckBoxes;

    private FloatingActionButton fab;

    @SuppressLint("StaticFieldLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        sharedPreferences = Utils.getSharedPreferences(this);
        editor = Utils.getEditor(this);

        //tricky -- create new hashset -> getstringset returns a reference
        whiteListSet = new HashSet<>(sharedPreferences.getStringSet("rd_whitelist", new HashSet<String>()));

        final Switch switchBlackWhite = (Switch) findViewById(R.id.switchBlackWhite);
        final TextView tvwhitelistinfo = (TextView) findViewById(R.id.tvInforWhitelist);

        if (sharedPreferences.getBoolean("blackOrWhite", true)) {
            switchBlackWhite.setChecked(true);
            tvwhitelistinfo.setText(getString(R.string.whitelistinfo, getString(R.string.listtype_white)));
        } else {
            switchBlackWhite.setChecked(false);
            tvwhitelistinfo.setText(getString(R.string.whitelistinfo, getString(R.string.listtype_black)));
        }

        switchBlackWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchBlackWhite.isChecked()) {
                    editor.putBoolean("blackOrWhite", true);
                    tvwhitelistinfo.setText(getString(R.string.whitelistinfo, getString(R.string.listtype_white)));
                } else {
                    editor.putBoolean("blackOrWhite", false);
                    tvwhitelistinfo.setText(getString(R.string.whitelistinfo, getString(R.string.listtype_black)));
                }
                editor.apply();
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
                } catch (WhatsAppDBException e) {
                    return e;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object object) {
                super.onPostExecute(object);

                if (Utils.toastAndExitIfWaDbException(object, WhiteListActivity.this)) {
                    return;
                }

                buildArrayList();

                whiteListAdapter = new WhiteListAdapter(getApplicationContext(), whiteList, WhiteListActivity.this);

                lstviewWhiteList = findViewById(R.id.lstviewWhiteListContacts);

                lstviewWhiteList.setAdapter(whiteListAdapter);

            }

        }).execute();

        fab = findViewById(R.id.fbAddToWhiteList);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(WhiteListActivity.this, WhatsAppContactPickerActivity.class), 1);
            }
        });

        (findViewById(R.id.imgbtnRemoveReceiptsContact)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (whiteList.size() == 0) {
                    Toast.makeText(getApplicationContext(), "List is empty.", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (fab.getVisibility() == View.VISIBLE) {
                    for (CheckBox checkBox : deleteCheckBoxes)
                        if (checkBox != null)
                            checkBox.setVisibility(View.VISIBLE);

                    fab.setVisibility(View.INVISIBLE);
                    v.setBackground(getDrawable(R.mipmap.ic_done_black_24dp));
                } else {
                    (new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            int i = 0;

                            for (Iterator<String> iterator = whiteList.iterator(); iterator.hasNext(); ) {
                                String name = iterator.next();
                                if (deleteItemsSet.contains(i)) {

                                    Object value = nameToNumberHashMap.get(name);

                                    if (value == null)// value is null -- it may be a group
                                        value = groupNameToNumberHashMap.get(name);

                                    if (value instanceof String)
                                        whiteListSet.remove(value.toString());
                                    else if (value instanceof List) {
                                        for (Object number : (List) value)
                                            whiteListSet.remove(number.toString());
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

                            lstviewWhiteList.setAdapter(null);
                            lstviewWhiteList.setAdapter(new WhiteListAdapter(getApplicationContext(), whiteList, WhiteListActivity.this));
                            editor.putStringSet("rd_whitelist", whiteListSet);
                            editor.apply();


                        }
                    }).execute();

                    for (CheckBox checkBox : deleteCheckBoxes)
                        if (checkBox != null)
                            checkBox.setVisibility(View.INVISIBLE);

                    fab.setVisibility(View.VISIBLE);
                    v.setBackground(getDrawable(R.mipmap.ic_delete_black_24dp));

                }
            }
        });

        Utils.setUpCheckBox(this, (CheckBox) findViewById(R.id.chkboxEnableRRChatSession), "enableRRDuringSession", false, "", false, "");
    }

    private void buildArrayList() {
        if (whiteList == null)
            whiteList = new ArrayList<>();
        else
            whiteList.clear();

        if (numberToNameHashMap == null) {
            Log.e("com.suraj.waext", "numbers null may be su failed");
            WhiteListActivity.this.finish();
            return;
        }

        if (groupNameToNumberHashMap == null || groupNumberToNameHashMap == null) {
            Log.e("com.suraj.waext", "groups null may be su failed");
            WhiteListActivity.this.finish();
            return;
        }

        for (String number : whiteListSet) {
            if (numberToNameHashMap.get(number) != null) {
                whiteList.add(numberToNameHashMap.get(number).toString());
            } else if (groupNumberToNameHashMap.get(number) != null) {
                whiteList.add(groupNumberToNameHashMap.get(number));
            } else if (numberToNameHashMap.size() == 0 || groupNumberToNameHashMap.size() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.sqliteMissing), Toast.LENGTH_SHORT).show();
            }

        }
        Collections.sort(whiteList);

        deleteCheckBoxes = new CheckBox[whiteList.size()];
        deleteItemsSet = new HashSet<>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            String number = data.getExtras().get("contact").toString().split("@")[0];

            whiteListSet.add(number);

            buildArrayList();

            lstviewWhiteList.setAdapter(null);
            lstviewWhiteList.setAdapter(new WhiteListAdapter(getApplicationContext(), whiteList, WhiteListActivity.this));

            editor.putStringSet("rd_whitelist", whiteListSet);

            editor.apply();

        }
    }

    @Override
    public void onInflateContactRow(View view, final List<String> whitelist, final int position) {
        if (nameToNumberHashMap == null || numberToNameHashMap == null) {
            Toast.makeText(getApplicationContext(), "Failed to get contact info. Make sure you have root", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView contactName = (TextView) view.findViewById(R.id.tvcontactname);
        contactName.setText(whitelist.get(position));

        final CheckBox deleteCheckBox = (CheckBox) view.findViewById(R.id.chkboxdeletewhitelistcontact);

        deleteCheckBoxes[position] = deleteCheckBox;

        if (fab.getVisibility() != View.VISIBLE)
            deleteCheckBoxes[position].setVisibility(View.VISIBLE);

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

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setPreferencesRW(this);
    }

}