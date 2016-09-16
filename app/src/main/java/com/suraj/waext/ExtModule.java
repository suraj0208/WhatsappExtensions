package com.suraj.waext;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * Created by suraj on 28/8/16.
 */

/**
 * This is where the magic happens
 */
public class ExtModule implements IXposedHookLoadPackage {

    public static final String LOCKED_CONTACTS_PREF_STRING = "lockedContacts";
    public static final String HIDDEN_GROUPS_PREF_STRING = "hiddenGroups";
    public static final String HIGHLIGHTED_CHATS_PREF_STRING = "highlightedChats";
    public static final String PACKAGE_NAME = "com.suraj.waext";
    public static final String UNLOCK_INTENT = ExtModule.PACKAGE_NAME + ".UNLOCK_INTENT";
    public static final String WALLPAPER_DIR = "/WhatsApp/Media/WallPaper/";

    private static HashSet<String> lockedContacts;
    private static HashSet<String> templockedContacts;
    private static HashSet<String> hiddenGroups;
    private static HashSet<String> highlightedChats;

    private static HashMap<View, View> processedViewsHashMap;
    private static HashMap<View, View> zerothChildrenHashMap;
    private static HashMap<View, View> firstChildrenHashMap;
    private static HashMap<Object, String> tagToContactHashMap;

    private static boolean showLockScreen = false;
    private static boolean firstTime = true;
    private static boolean enableHighlight = false;
    private static boolean enableHideSeen = false;
    private static boolean isGroup = false;
    private static boolean exceptionThrown = true;
    private static boolean enableHideCamera = false;


    private static int highlightColor = Color.GRAY;
    private static int individualHighlightColor = Color.GRAY;

    private String archiveBooleanFieldName;

    private int originalColor = -1;
    private int lockAfter;

    private Class<?> settingClass;
    private Class<?> preferenceClass;
    private Class<?> archiveClass;

    private String contactNumber;

    private Context context;

    private Thread thread;

    private XSharedPreferences sharedPreferences;

    private UnlockReceiver unlockReceiver;

    public ExtModule() {

    }

    private void hookMethodsForHighLight(XC_LoadPackage.LoadPackageParam loadPackageParam) {


        XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                updateHighlightColor();
                TypedValue a = new TypedValue();

                AndroidAppHelper.currentApplication().getApplicationContext().getTheme().resolveAttribute(android.R.attr.textColor, a, true);
                originalColor = a.data;
            }
        });

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setTag", Object.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        if (!enableHighlight && highlightedChats.size() == 0)
                            return;

                        String tag = param.args[0].toString();

                        boolean localIsGroup = tag.toString().contains("@g.us");

                        View parent = processedViewsHashMap.get(param.thisObject);

                        RelativeLayout rl = null;

                        String contact = null;

                        if (!localIsGroup) {

                            if (tagToContactHashMap.get(tag) == null) {
                                try {
                                    contact = tag.split(":")[1];
                                    contact = contact.split("_")[0];
                                    contact = contact.replace("+", "");
                                    tagToContactHashMap.put(tag, contact);
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    XposedBridge.log("ArrayIndexOutofBound");
                                    ex.printStackTrace();
                                }

                            } else
                                contact = tagToContactHashMap.get(tag);
                        } else {
                            contact = tag.split("@")[0];
                        }

                        if (parent == null) {
                            View contactPictureViewBackground = (View) ((View) param.thisObject).getParent();

                            if (contactPictureViewBackground == null)
                                return;

                            parent = (View) contactPictureViewBackground.getParent();

                            if (parent == null)
                                return;

                            processedViewsHashMap.put((View) param.thisObject, parent);

                            try {
                                rl = (RelativeLayout) parent;
                            }catch (ClassCastException e){
                                XposedBridge.log("ClassCastException");
                            }

                            zerothChildrenHashMap.put(parent, rl.getChildAt(0));
                            firstChildrenHashMap.put(parent, rl.getChildAt(1));

                        }

                        View zerothChild = zerothChildrenHashMap.get(parent);

                        View firstChild = firstChildrenHashMap.get(parent);

                        if (localIsGroup && enableHighlight) {
                            firstChild.setBackgroundColor(highlightColor);
                            zerothChild.setBackgroundColor(highlightColor);
                            firstChild.setPadding(0, 37, 30, 0);
                            ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -1;
                        } else {
                            if (contact != null && highlightedChats.contains(contact)) {
                                firstChild.setBackgroundColor(individualHighlightColor);
                                zerothChild.setBackgroundColor(individualHighlightColor);
                                firstChild.setPadding(0, 37, 30, 0);
                                ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -1;
                                return;
                            }

                            firstChild.setBackgroundColor(originalColor);
                            zerothChild.setBackgroundColor(originalColor);
                            firstChild.setPadding(0, 0, 30, 0);
                            ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -2;

                        }
                    }
                }
        );
    }

    private void updateHighlightColor() {
        sharedPreferences.reload();
        sharedPreferences.makeWorldReadable();

        highlightColor = sharedPreferences.getInt("highlightColor", Color.GRAY);
        individualHighlightColor = sharedPreferences.getInt("individualHighlightColor", Color.GRAY);

        enableHighlight = sharedPreferences.getBoolean("enableHighlight", false);
        enableHideCamera = sharedPreferences.getBoolean("hideCamera", false);
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

                            if (result.contains("@g.us")) {
                                isGroup = true;
                            } else
                                isGroup = false;

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

    public void hookConversationMethods(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", loadPackageParam.classLoader);

        XposedHelpers.findAndHookMethod(conversationClass, "onResume", new XC_MethodHook() {


            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                //XposedBridge.log("onResume");

                if (ExtModule.showLockScreen && !firstTime) {
                    ((Activity) param.thisObject).finish();
                } else
                    firstTime = false;

                if (!enableHideSeen)
                    return;


                try {
                    Thread.sleep(200);
                    setSeenOff("0");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }


            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                //XposedBridge.log("onCreateOptionMenu");

                //skip call button for group chats
                if (!contactNumber.contains("-")) {
                    MenuItem callMenuItem = ((Menu) param.args[0]).add("call");
                    callMenuItem.setIcon(android.R.drawable.ic_menu_search);
                    callMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }

                MenuItem menuItem;

                if (!hiddenGroups.contains(contactNumber))
                    menuItem = ((Menu) param.args[0]).add("Hide");
                else
                    menuItem = ((Menu) param.args[0]).add("Unhide");

                menuItem.setIcon(android.R.drawable.ic_menu_search);
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


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

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Menu menu = (Menu) param.args[0];

                File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPER_DIR + contactNumber + ".jpg");

                if (f.exists() && !f.isDirectory())
                    menu.getItem(menu.size() - 1).getSubMenu().add("Remove Wallpaper");
                else
                    menu.getItem(menu.size() - 1).getSubMenu().add("Custom Wallpaper");


                if (isGroup && enableHighlight)
                    return;

                if (highlightedChats.contains(contactNumber)) {
                    menu.getItem(menu.size() - 1).getSubMenu().add("Unhighlight");

                } else {
                    menu.getItem(menu.size() - 1).getSubMenu().add("Highlight");
                }

            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                ((Activity) param.thisObject).registerReceiver(unlockReceiver, new IntentFilter(ExtModule.UNLOCK_INTENT));
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
                String title = menuItem.getTitle().toString();
                //important: param.setResult(false) to prevent call to original method

                Intent intent;

                switch (title) {
                    case "Lock":
                        lockedContacts.add(contactNumber);
                        templockedContacts.add(contactNumber);

                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 0);
                        intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);
                        AndroidAppHelper.currentApplication().startService(intent);

                        ExtModule.this.showToast("Lock Enabled for this contact.");

                        ((Activity) param.thisObject).finish();
                        param.setResult(false);
                        break;

                    case "Unlock":
                        lockedContacts.remove(contactNumber);
                        templockedContacts.remove(contactNumber);

                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 0);
                        intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);

                        AndroidAppHelper.currentApplication().startService(intent);
                        ExtModule.this.showToast("Lock disabled for this contact.");

                        menuItem.setVisible(false);
                        param.setResult(false);
                        break;

                    case "call":
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        callIntent.setData(Uri.parse("tel:" + "+".concat(contactNumber.replaceAll(" ", ""))));

                        try {
                            AndroidAppHelper.currentApplication().startActivity(callIntent);
                        } catch (Exception ex) {
                            showToast("Couldn't place call");
                            ex.printStackTrace();
                        }

                        param.setResult(false);
                        break;

                    case "Add Reminder":
                        intent = new Intent();
                        intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.ReminderActivity"));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("contactNumber", contactNumber);
                        AndroidAppHelper.currentApplication().startActivity(intent);
                        param.setResult(false);
                        break;

                    case "Custom Wallpaper":
                        intent = new Intent();
                        intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.CropActivity"));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("contactNumber", contactNumber);
                        AndroidAppHelper.currentApplication().startActivity(intent);
                        param.setResult(false);
                        ExtModule.this.showToast("ReOpen chat to see effects");
                        break;

                    case "Remove Wallpaper":
                        File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPER_DIR + contactNumber + ".jpg");
                        if (f.exists())
                            f.delete();
                        param.setResult(false);
                        ExtModule.this.showToast("ReOpen chat to see effects");
                        break;

                    case "Hide":
                        hiddenGroups.add(contactNumber);

                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 1);
                        intent.putExtra(ExtModule.HIDDEN_GROUPS_PREF_STRING, hiddenGroups);
                        AndroidAppHelper.currentApplication().startService(intent);
                        menuItem.setTitle("Unhide");
                        ExtModule.this.showToast("Restart WhatsApp to take effect.");

                        param.setResult(false);

                        break;

                    case "Unhide":
                        hiddenGroups.remove(contactNumber);

                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 1);
                        intent.putExtra(ExtModule.HIDDEN_GROUPS_PREF_STRING, hiddenGroups);
                        AndroidAppHelper.currentApplication().startService(intent);
                        menuItem.setTitle("Hide");
                        ExtModule.this.showToast("Remove WhatApp from recent apps and Relaunch.");

                        param.setResult(false);
                        break;

                    case "Highlight":
                        highlightedChats.add(contactNumber);

                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 3);
                        intent.putExtra(ExtModule.HIGHLIGHTED_CHATS_PREF_STRING, highlightedChats);
                        AndroidAppHelper.currentApplication().startService(intent);
                        menuItem.setTitle("Unhighlight");
                        ExtModule.this.showToast("Restart WA to avoid unwanted effects.");
                        param.setResult(false);
                        break;

                    case "Unhighlight":
                        highlightedChats.remove(contactNumber);
                        intent = new Intent();
                        intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                        intent.putExtra("action", 3);
                        intent.putExtra(ExtModule.HIGHLIGHTED_CHATS_PREF_STRING, highlightedChats);
                        AndroidAppHelper.currentApplication().startService(intent);
                        menuItem.setTitle("Highlight");
                        ExtModule.this.showToast("Restart WA to avoid unwanted effects.");
                        param.setResult(false);
                        break;
                }
            }

        });

    }

    private void hookMethodsForLock(final XC_LoadPackage.LoadPackageParam loadPackageParam) {

        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                Activity activity = (Activity) param.thisObject;

                context = activity.getApplicationContext();


                //if (!activity.getClass().getName().equals("com.whatsapp.HomeActivity"))
                //    return;

                if (enableHideSeen)
                    setSeenOff("2");

                if (thread == null || !thread.isAlive())
                    startDaemon();

            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Activity activity = (Activity) param.thisObject;
                context = activity.getApplicationContext();

                //if (activity.getClass().getName().equals("com.whatsapp.HomeActivity"))
                //    setSeenOff("0");

                /*Class settingClass = XposedHelpers.findClass("com.whatsapp.SettingsPrivacy", loadPackageParam.classLoader);

                Constructor<?> settingsConstructor = settingClass.getDeclaredConstructor();
                settingsConstructor.setAccessible(true);
                Object settingsObject = settingsConstructor.newInstance();

                if (settingsObject != null) {
                    XposedBridge.log("we constructed.");
                }


                Class preferenceClass = XposedHelpers.findClass("com.whatsapp.preference.WaPrivacyPreference", loadPackageParam.classLoader);

                Constructor<?> preferenceConstructor = preferenceClass.getDeclaredConstructor(Context.class);
                preferenceConstructor.setAccessible(true);
                Object preferenceObject = preferenceConstructor.newInstance(activity.getApplicationContext());

                CharSequence[] entryCharSequences = new CharSequence[]{"Everyone", "My contacts", "Nobody"};
                CharSequence[] entryValuesSequences = new CharSequence[]{"0", "1", "2"};

                //ListPreference listPreference = new ListPreference(activity.getApplicationContext());
                //listPreference.setEntries(charSequences);
                //listPreference.setKey("privacy_last_seen");
                //listPreference.setValue("0");

                //preferenceObject=preferenceClass.cast(listpreferenceClass.cast(listPreference));

                XposedHelpers.callMethod(preferenceObject, "setKey", "privacy_last_seen");
                //XposedHelpers.callMethod(preferenceObject,"setEntries",charSequences);

                //preferenceObject.setKey("privacy_last_seen");
                XposedBridge.log(XposedHelpers.callMethod(preferenceObject, "getKey").toString());

                ListPreference listPreference = (ListPreference) preferenceObject;
                listPreference.setEntries(entryCharSequences);
                listPreference.setEntryValues(entryValuesSequences);

                XposedHelpers.callMethod(settingsObject, "a", preferenceObject, "0");

                Preference preference;

                /*Activity activity = (Activity) param.thisObject;
                HashMap hashMap = new HashMap();
                hashMap.put("profile", "all");
                Message message = new Message();
                message.arg1 = 69;
                message.what = 5;
                message.obj = hashMap;

                Class r = XposedHelpers.findClass("com.whatsapp.messaging.r", loadPackageParam.classLoader);
                Constructor<?> rrconstructor = r.getDeclaredConstructor(Context.class);
                rrconstructor.setAccessible(true);
                Object robject = rrconstructor.newInstance(activity.getApplicationContext());

                Class appclass = XposedHelpers.findClass("com.whatsapp.App", loadPackageParam.classLoader);
                Constructor<?> appconstructor = appclass.getConstructor(Application.class);
                appconstructor.setAccessible(true);

                Object appobject = appconstructor.newInstance(((Activity) param.thisObject).getApplication());

                Field fields[] = r.getDeclaredFields();
                BroadcastReceiver i, m, n, o;
                Field fi = r.getDeclaredField("I");
                fi.setAccessible(true);
                i = (BroadcastReceiver) fi.get(robject);

                Field fm = r.getDeclaredField("M");
                fm.setAccessible(true);
                m = (BroadcastReceiver) fm.get(robject);

                Field fn = r.getDeclaredField("N");
                fn.setAccessible(true);
                n = (BroadcastReceiver) fn.get(robject);

                Field fo = r.getDeclaredField("O");
                fo.setAccessible(true);
                o = (BroadcastReceiver) fo.get(robject);

                Field field = robject.getClass().getDeclaredField("c");
                field.setAccessible(true);
                field.set(robject, appobject);

                Class handlerclasss = XposedHelpers.findClass("com.whatsapp.messaging.aa", loadPackageParam.classLoader);
                Constructor<?> handlerconstructor = handlerclasss.getConstructor(r, Looper.class);
                handlerconstructor.setAccessible(true);

                Object object = handlerconstructor.newInstance(new Object[]{robject, activity.getMainLooper()});
                Handler handler = (Handler) object;

                handler.sendMessage(message);
*/

                //handler.sendMessage(message);

                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }
            }
        });
    }


    //daemon thread to lock contacts periodically
    private void startDaemon() {
        thread = new Thread() {
            @Override
            public void run() {

                try {
                    //XposedBridge.log("Thread started");
                    Thread.sleep(4000);

                    if (enableHideSeen)
                        setSeenOff("2");

                    Thread.sleep(lockAfter * 1000 * 60);
                    ExtModule.templockedContacts.addAll(lockedContacts);
                } catch (InterruptedException e) {
                    //XposedBridge.log("Thread Inturrupted");
                    //e.printStackTrace();
                }

            }
        };

        thread.setDaemon(true);
        thread.start();

    }

    private void setSeenOff(final String val) {
        if (context == null) {
            XposedBridge.log("WA context null");
            return;
        }

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                Constructor<?> settingsConstructor = null;
                try {
                    settingsConstructor = settingClass.getDeclaredConstructor();
                    settingsConstructor.setAccessible(true);
                    Object settingsObject = settingsConstructor.newInstance();


                    Constructor<?> preferenceConstructor = preferenceClass.getDeclaredConstructor(Context.class);
                    preferenceConstructor.setAccessible(true);
                    Object preferenceObject = preferenceConstructor.newInstance(context);

                    CharSequence[] entryCharSequences = new CharSequence[]{"Everyone", "My contacts", "Nobody"};
                    CharSequence[] entryValuesSequences = new CharSequence[]{"0", "1", "2"};


                    XposedHelpers.callMethod(preferenceObject, "setKey", "privacy_last_seen");

                    //XposedBridge.log(XposedHelpers.callMethod(preferenceObject, "getKey").toString());

                    ListPreference listPreference = (ListPreference) preferenceObject;
                    listPreference.setEntries(entryCharSequences);
                    listPreference.setEntryValues(entryValuesSequences);

                    XposedHelpers.callMethod(settingsObject, "a", preferenceObject, val);

                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
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

    public void initPrefs() {
        sharedPreferences.reload();
        sharedPreferences.makeWorldReadable();

        lockedContacts = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.LOCKED_CONTACTS_PREF_STRING, new HashSet<String>());
        highlightedChats = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.HIGHLIGHTED_CHATS_PREF_STRING, new HashSet<String>());
        hiddenGroups = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.HIDDEN_GROUPS_PREF_STRING, new HashSet<String>());

        highlightColor = sharedPreferences.getInt("highlightColor", Color.GRAY);

        enableHideSeen = sharedPreferences.getBoolean("hideSeen", false);
        enableHideCamera = sharedPreferences.getBoolean("hideCamera", false);

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
            templockedContacts.remove(contactNumber);

            //XposedBridge.log("Broadcast Received");
        }
    }

    private void hookMethodsForCameraAndZoom(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setVisibility", int.class, new de.robv.android.xposed.XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                if (!param.args[0].toString().equals("0"))
                    return;


                View view = (View) param.thisObject;

                if (view.getTransitionName() == null) {
                    if (enableHideCamera && param.thisObject.getClass().getName().equals("android.support.v7.widget.x")) {
                        View parent = (View) view.getParent();

                        if (parent instanceof LinearLayout) {
                            StackTraceElement[] stackTraceElements = new Exception().getStackTrace();

                            if (stackTraceElements[4].getMethodName().equals("afterTextChanged") || stackTraceElements[5].getMethodName().equals("onCreate"))
                                view.setVisibility(View.GONE);

                        }
                    }
                    return;

                }

                if (param.thisObject instanceof ImageView) {
                    new PhotoViewAttacher((ImageView) param.thisObject);
                }
            }
        });
    }

    private void hookMethodsForWallPaper(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.whatsapp.wallpaper.WallPaperView", loadPackageParam.classLoader, "setDrawable", Drawable.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Drawable drawable = Drawable.createFromPath(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPER_DIR + contactNumber + ".jpg");

                if (drawable != null)
                    param.args[0] = drawable;

            }
        });
    }

    private void hookMethodsForHideGroup(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("java.util.concurrent.ConcurrentHashMap", loadPackageParam.classLoader, "get", Object.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                if (param.args[0] == null) {
                    return;
                }

                if (param.getResult() == null) {
                    return;
                }

                if (exceptionThrown) {
                    if (param.args[0].toString().contains("@")) {
                        archiveClass = param.getResult().getClass();

                        for (Field field : archiveClass.getDeclaredFields()) {
                            if (field.getType().getName().equals("boolean")) {
                                archiveBooleanFieldName = field.getName();
                                XposedBridge.log("s name set");
                                exceptionThrown = false;
                            }
                        }
                    } else {
                        return;
                    }

                }

                if (!(archiveClass != null && archiveClass.isInstance(param.getResult()))) {
                    return;
                }


                if (!hiddenGroups.contains(param.args[0].toString().split("@")[0]))
                    return;

                Field f = param.getResult().getClass().getDeclaredField(archiveBooleanFieldName);
                f.setAccessible(true);
                f.set(param.getResult(), true);

                //XposedBridge.log(param.args[0] + " " + param.getResult().getClass().getName());

            }
        });
    }


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws
            Throwable {

        if (!loadPackageParam.packageName.equals("com.whatsapp"))
            return;

        XposedBridge.log("com.suraj.waext Loaded: " + loadPackageParam.packageName);

        sharedPreferences = new XSharedPreferences(ExtModule.PACKAGE_NAME, "myprefs");

        processedViewsHashMap = new HashMap<>();
        zerothChildrenHashMap = new HashMap<>();
        firstChildrenHashMap = new HashMap<>();
        tagToContactHashMap = new HashMap<>();

        hookConversationMethods(loadPackageParam);
        hookInitialStage(loadPackageParam);
        hookMethodsForLock(loadPackageParam);
        hookMethodsForHighLight(loadPackageParam);
        hookMethodsForWallPaper(loadPackageParam);
        hookMethodsForHideGroup(loadPackageParam);
        hookMethodsForCameraAndZoom(loadPackageParam);

        unlockReceiver = new UnlockReceiver();

        initPrefs();
        templockedContacts = new HashSet<>();
        templockedContacts.addAll(lockedContacts);

        //value of timer after which contact is to locked
        lockAfter = getLockAfter(sharedPreferences.getInt("lockAfter", 2));

        try {
            settingClass = XposedHelpers.findClass("com.whatsapp.SettingsPrivacy", loadPackageParam.classLoader);
            preferenceClass = XposedHelpers.findClass("com.whatsapp.preference.WaPrivacyPreference", loadPackageParam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError error) {
            error.printStackTrace();
        }

        /*XposedHelpers.findAndHookMethod("android.os.BaseBundle", loadPackageParam.classLoader, "getString", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                XposedBridge.log(param.args[0].toString()+" "+param.getResult());
                BaseBundle baseBundle = (BaseBundle)param.thisObject;
                Set set = baseBundle.keySet();

                if(set.size()==2){
                    if(set.contains("jid") && set.contains("msgid")){
                        baseBundle.putString("jid", "919657785171@s.whatsapp.net");
                        if(param.args[0].equals("jid"))
                            param.setResult("919657785171@s.whatsapp.net");
                    }
                }

                XposedBridge.log("------------------------start----------------------------");
                for(String s : baseBundle.keySet())
                    XposedBridge.log(s +" " + baseBundle.get(s));

                XposedBridge.log("------------------------end----------------------------");
                //for(StackTraceElement stackTraceElement : new Exception().getStackTrace())
                //    XposedBridge.log(stackTraceElement.getClassName() + " " + stackTraceElement.getMethodName());

            }
        });*/


    }

    public void printMethodOfClass(String className, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class cls = XposedHelpers.findClass(className, loadPackageParam.classLoader);

        Method[] methods = cls.getDeclaredMethods();


        for (Method method : methods) {
            String m = method.getName();

            for (Class p : method.getParameterTypes()) {
                m = m + " " + p.getName();
            }
            XposedBridge.log(m);

        }
    }

}
