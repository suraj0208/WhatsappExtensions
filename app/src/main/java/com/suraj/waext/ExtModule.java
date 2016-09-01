package com.suraj.waext;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/**
 * Created by suraj on 28/8/16.
 */
public class ExtModule implements IXposedHookLoadPackage {

    private static final String LOCKED_CONTACTS_PREF_STRING = "lockedContacts";
    private static final String PACKAGE_NAME = "com.suraj.waext";
    private static HashSet<String> lockedContacts;
    private static HashSet<String> templockedContacts;
    private static boolean showLockScreen = false;
    private static boolean firstTime = true;

    private String contactNumber;
    private XSharedPreferences sharedPreferences;
    private UnlockReceiver unlockReceiver;
    private Thread thread;

    private int lockAfter;

    public ExtModule() {

    }

    //broadcast receiver to unlock - broadcast is sent from LockActivity's unLock method
    class UnlockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //set boolean values so that onResume does not close activity. see beforeHook - onResume
            ExtModule.showLockScreen = intent.getBooleanExtra("showLockScreen", false);
            ExtModule.firstTime = intent.getBooleanExtra("firstTime", true);

            sharedPreferences.reload();
            sharedPreferences.makeWorldReadable();

            lockAfter = getLockAfter(sharedPreferences.getInt("lockAfter", 2));

            //if contact is not to be locked immediately remove it temporarily from lockedcontacts.
            if (lockAfter != 0) {
                templockedContacts.remove(contactNumber);
                ExtModule.this.showToast("Unlocked for " + lockAfter + " minutes.");

                //if thread is not running start it.
                if (thread != null && !thread.isAlive())
                    startDaemon();
            } else {
                //if contact is to be locked immediately, stop the thread, no use of it.
                if (thread != null && thread.isAlive())
                    thread.interrupt();
            }


            XposedBridge.log("Broadcast Received");
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        //XposedBridge.log("com.suraj.waext Loaded: " + loadPackageParam.packageName);

        if (!loadPackageParam.packageName.equals("com.whatsapp"))
            return;

        XposedBridge.log("com.suraj.waext Loaded: " + loadPackageParam.packageName);

        sharedPreferences = new XSharedPreferences(ExtModule.PACKAGE_NAME, "myprefs");

        hookConversationMethods(loadPackageParam);
        hookInitialStage(loadPackageParam);
        unlockReceiver = new UnlockReceiver();

        initPrefs();
        templockedContacts = new HashSet<>();
        templockedContacts.addAll(lockedContacts);

        //value of timer after which contact is to locked
        lockAfter = getLockAfter(sharedPreferences.getInt("lockAfter", 2));

        if (lockAfter != 0)
            startDaemon();
    }

    //daemon thread to lock contacts periodically
    private void startDaemon() {
        thread = new Thread() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Thread.sleep(lockAfter * 1000 * 60);
                        ExtModule.templockedContacts.addAll(lockedContacts);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        break;
                    }
                }

            }
        };

        thread.setDaemon(true);
        thread.start();

    }

    public void initPrefs() {
        sharedPreferences.reload();
        sharedPreferences.makeWorldReadable();
        lockedContacts = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.LOCKED_CONTACTS_PREF_STRING, new HashSet<String>());
    }

    public void hookInitialStage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Intent.class, "getStringExtra", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                ExtModule.showLockScreen = false;
                ExtModule.firstTime = true;


                (new Handler(Looper.getMainLooper())).post(new Runnable() {
                    @Override
                    public void run() {
                        String result = (String) param.getResult();

                        if (result != null) {
                            if (result.contains("@")) {
                                contactNumber = result.split("@")[0];
                                if (templockedContacts.contains(contactNumber)) {
                                    ExtModule.showLockScreen = true;
                                    ExtModule.firstTime = false;

                                    Intent intent = new Intent();
                                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.LockActivity"));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    AndroidAppHelper.currentApplication().startActivity(intent);
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    public void hookConversationMethods(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", loadPackageParam.classLoader);

        XposedHelpers.findAndHookMethod(conversationClass, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);


                XposedBridge.log("onResume");

                if (ExtModule.showLockScreen && !firstTime) {
                    XposedBridge.log("finished activity");
                    ((Activity) param.thisObject).finish();
                } else
                    firstTime = false;

            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                XposedBridge.log("onCreateOptionMenu");

                //skip call button for group chats
                if (!contactNumber.contains("-")) {
                    MenuItem callMenuItem = ((Menu) param.args[0]).add("Call");
                    callMenuItem.setIcon(android.R.drawable.ic_menu_search);
                    callMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }

                MenuItem lock;
                MenuItem unlock;

                lock = ((Menu) param.args[0]).add("Lock");
                lock.setIcon(android.R.drawable.ic_menu_search);
                lock.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                if (lockedContacts.contains(contactNumber)) {
                    unlock = ((Menu) param.args[0]).add("Unlock");
                    unlock.setIcon(android.R.drawable.ic_menu_search);
                    unlock.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }

                MenuItem reminderMenuItem = ((Menu) param.args[0]).add("Add Reminder");
                reminderMenuItem.setIcon(android.R.drawable.ic_menu_search);
                reminderMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


            }

        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("onCreate");

                ((Activity) param.thisObject).registerReceiver(unlockReceiver, new IntentFilter(ExtModule.PACKAGE_NAME + ".Unlock_Intent"));

            }

        });

        XposedHelpers.findAndHookMethod(conversationClass, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                ((Activity) param.thisObject).unregisterReceiver(unlockReceiver);
            }

        });

        XposedHelpers.findAndHookMethod(conversationClass, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                MenuItem menuItem = (MenuItem) param.args[0];

                //important: param.setResult(false) to prevent call to original method

                if (menuItem.getTitle() == "Lock") {

                    lockedContacts.add(contactNumber);
                    templockedContacts.add(contactNumber);

                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);
                    AndroidAppHelper.currentApplication().startService(intent);
                    XposedBridge.log("called the intent for adding lock");

                    ExtModule.this.showToast("Lock Enabled for this contact.");

                    ((Activity)param.thisObject).finish();
                    param.setResult(false);

                } else if (menuItem.getTitle() == "Unlock") {
                    lockedContacts.remove(contactNumber);
                    templockedContacts.remove(contactNumber);

                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));

                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);

                    AndroidAppHelper.currentApplication().startService(intent);
                    XposedBridge.log("called the intent for removing lock");
                    ExtModule.this.showToast("Lock disabled for this contact.");

                    menuItem.setVisible(false);
                    param.setResult(false);

                } else if (menuItem.getTitle() == "Call") {


                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    callIntent.setData(Uri.parse("tel:" + contactNumber.replaceAll(" ", "")));

                    try {
                        AndroidAppHelper.currentApplication().startActivity(callIntent);
                    } catch (Exception ex) {
                        showToast("Couldn't place call");
                        ex.printStackTrace();
                    }


                    param.setResult(false);

                } else if (menuItem.getTitle().toString().equals("Add Reminder")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.ReminderActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("contactNumber", contactNumber);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);
                }
            }

        });

    }


    public void showToast(final String text) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AndroidAppHelper.currentApplication(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getLockAfter(int position) {
        switch (position) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 5;
            case 4:
                return 10;
        }
        return 3;
    }

}
