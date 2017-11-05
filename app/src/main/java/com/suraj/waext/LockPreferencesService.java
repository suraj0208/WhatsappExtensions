package com.suraj.waext;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Set;


/**
 * Created by suraj on 30/8/16.
 */
public class LockPreferencesService extends IntentService {

    public LockPreferencesService() {
        super("LockPreferencesService");
    }

    public LockPreferencesService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences.Editor editor = Utils.getEditor(this);

        int action = intent.getIntExtra("action", 0);


        switch (action) {
            case 0:
                editor.putStringSet("lockedContacts", (Set) intent.getSerializableExtra("lockedContacts"));
                break;
            case 1:
                editor.putStringSet("hiddenGroups", (Set) intent.getSerializableExtra("hiddenGroups"));
                break;
            case 2:
                editor.putBoolean("showGroups", intent.getBooleanExtra("showGroups", true));
                break;
            case 3:
                editor.putStringSet("highlightedChats", (Set) intent.getSerializableExtra("highlightedChats"));
                break;

        }

        //XposedBridge.log("saved preferences");
        editor.apply();

        Utils.setPreferencesRW(this);
    }

}