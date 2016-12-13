package com.suraj.waext;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhiteListActivity extends AppCompatActivity implements DeleteButtonListener {
    private ArrayList<String> whitelist;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private WhiteListAdapter whiteListAdapter;
    private Set<String> whitelistSet;
    private HashMap<String, Object> numberToNameHashmap;
    private HashMap<String, Object> nameToNumberHashmap;

    private ListView lstviewwhitelist;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        sharedPreferences = getSharedPreferences("myprefs", 1);
        editor = sharedPreferences.edit();

        //tricky -- create new hashset -> getstringset returns a reference
        whitelistSet = new HashSet<>(sharedPreferences.getStringSet("rd_whitelist", new HashSet<String>()));

        final WhatsAppContactManager whatsAppContactManager = new WhatsAppContactManager();

        (new AsyncTask<Void,Void,Void>(){
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

        (findViewById(R.id.fbaddtowhitelist)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.ContactPicker"));
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 1);
            }
        });
    }

    private void buildArrayList() {
        if (whitelist == null)
            whitelist = new ArrayList<>();

        whitelist.clear();

        if (numberToNameHashmap == null) {
            Log.e("com.suraj.waext", "may be su failed");
            return;
        }

        for (String number : whitelistSet)
            whitelist.add(numberToNameHashmap.get(number).toString());

        Collections.sort(whitelist);

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
    public void deleteButtonPressed(int position) {
        if (nameToNumberHashmap == null || numberToNameHashmap == null) {
            Toast.makeText(getApplicationContext(), "Failed to get contact info. Make sure you have root", Toast.LENGTH_SHORT).show();
            return;
        }

        Object value = nameToNumberHashmap.get(whitelist.get(position));

        if (value instanceof String)
            whitelistSet.remove(value.toString());
        else if (value instanceof List) {
            for (Object number : (List) value)
                whitelistSet.remove(number.toString());
        }

        whitelist.remove(position);

        lstviewwhitelist.setAdapter(null);
        lstviewwhitelist.setAdapter(new WhiteListAdapter(getApplicationContext(), whitelist, WhiteListActivity.this));

        editor.putStringSet("rd_whitelist", whitelistSet);

        editor.apply();
    }
}