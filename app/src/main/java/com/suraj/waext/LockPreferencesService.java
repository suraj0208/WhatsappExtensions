package com.suraj.waext;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putStringSet("lockedContacts", (Set<String>) intent.getSerializableExtra("lockedContacts"));
        //XposedBridge.log("saved preferences");
        editor.apply();


    }
}