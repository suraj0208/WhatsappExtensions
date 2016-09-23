package com.suraj.waext;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import uk.co.senab.photoview.PhotoViewAttacher;


/**
 * Created by suraj on 28/8/16.
 */

/**
 * This is where the magic happens
 */
public class ExtModule implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    public static final String LOCKED_CONTACTS_PREF_STRING = "lockedContacts";
    public static final String HIDDEN_GROUPS_PREF_STRING = "hiddenGroups";
    public static final String HIGHLIGHTED_CHATS_PREF_STRING = "highlightedChats";
    public static final String PACKAGE_NAME = "com.suraj.waext";
    public static final String UNLOCK_INTENT = ExtModule.PACKAGE_NAME + ".UNLOCK_INTENT";
    public static final String WALLPAPER_DIR = "/WhatsApp/Media/WallPaper/";
    public static String MODULE_PATH;

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
    private XModuleResources modRes;

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
                                    //XposedBridge.log("ArrayIndexOutofBound");
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
                            } catch (ClassCastException e) {
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
                    MenuItem callMenuItem = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_call));
                    callMenuItem.setIcon(android.R.drawable.ic_menu_search);
                    callMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }

                MenuItem menuItem;

                if (!hiddenGroups.contains(contactNumber))
                    menuItem = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_hide));
                else
                    menuItem = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_unhide));

                menuItem.setIcon(android.R.drawable.ic_menu_search);
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);


                MenuItem lock;
                MenuItem unlock;

                lock = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_lock));
                lock.setIcon(android.R.drawable.ic_menu_search);
                lock.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                if (lockedContacts.contains(contactNumber)) {
                    unlock = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_unlock));
                    unlock.setIcon(android.R.drawable.ic_menu_search);
                    unlock.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }

                MenuItem reminderMenuItem = ((Menu) param.args[0]).add(modRes.getString(R.string.menuitem_reminder));
                reminderMenuItem.setIcon(android.R.drawable.ic_menu_search);
                reminderMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Menu menu = (Menu) param.args[0];

                File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPER_DIR + contactNumber + ".jpg");

                if (f.exists() && !f.isDirectory())
                    menu.getItem(menu.size() - 1).getSubMenu().add(modRes.getString(R.string.menuitem_wallpaper_remove));
                else
                    menu.getItem(menu.size() - 1).getSubMenu().add(modRes.getString(R.string.menuitem_wallpaper_set));


                if (isGroup && enableHighlight)
                    return;

                if (highlightedChats.contains(contactNumber)) {
                    menu.getItem(menu.size() - 1).getSubMenu().add(modRes.getString(R.string.menuitem_unhighlight));

                } else {
                    menu.getItem(menu.size() - 1).getSubMenu().add(modRes.getString(R.string.menuitem_highlight));
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

                if (title.equals(modRes.getString(R.string.menuitem_lock))) {
                    lockedContacts.add(contactNumber);
                    templockedContacts.add(contactNumber);

                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 0);
                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);
                    AndroidAppHelper.currentApplication().startService(intent);

                    ExtModule.this.showToast(modRes.getString(R.string.lock_enable_message));

                    ((Activity) param.thisObject).finish();
                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_unlock))) {
                    lockedContacts.remove(contactNumber);
                    templockedContacts.remove(contactNumber);

                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 0);
                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);

                    AndroidAppHelper.currentApplication().startService(intent);
                    ExtModule.this.showToast(modRes.getString(R.string.lock_disable_message));

                    menuItem.setVisible(false);
                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_call))) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    callIntent.setData(Uri.parse("tel:" + "+".concat(contactNumber.replaceAll(" ", ""))));

                    try {
                        AndroidAppHelper.currentApplication().startActivity(callIntent);
                    } catch (Exception ex) {
                        showToast(modRes.getString(R.string.call_place_error));
                        ex.printStackTrace();
                    }

                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_reminder))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.ReminderActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("contactNumber", contactNumber);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_wallpaper_set))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.CropActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("contactNumber", contactNumber);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);
                    ExtModule.this.showToast(modRes.getString(R.string.chat_reopen_message));

                } else if (title.equals(modRes.getString(R.string.menuitem_wallpaper_remove))) {
                    File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPER_DIR + contactNumber + ".jpg");
                    if (f.exists())
                        f.delete();
                    param.setResult(false);
                    ExtModule.this.showToast(modRes.getString(R.string.chat_reopen_message));

                } else if (title.equals(modRes.getString(R.string.menuitem_hide))) {
                    hiddenGroups.add(contactNumber);

                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 1);
                    intent.putExtra(ExtModule.HIDDEN_GROUPS_PREF_STRING, hiddenGroups);
                    AndroidAppHelper.currentApplication().startService(intent);
                    menuItem.setTitle(modRes.getString(R.string.menuitem_unhide));
                    ExtModule.this.showToast(modRes.getString(R.string.whatsapp_restart_message));

                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_unhide))) {
                    hiddenGroups.remove(contactNumber);

                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 1);
                    intent.putExtra(ExtModule.HIDDEN_GROUPS_PREF_STRING, hiddenGroups);
                    AndroidAppHelper.currentApplication().startService(intent);
                    menuItem.setTitle(modRes.getString(R.string.menuitem_hide));
                    ExtModule.this.showToast(modRes.getString(R.string.whatsapp_restart_message_long));

                    param.setResult(false);
                } else if (title.equals(modRes.getString(R.string.menuitem_highlight))) {
                    highlightedChats.add(contactNumber);

                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 3);
                    intent.putExtra(ExtModule.HIGHLIGHTED_CHATS_PREF_STRING, highlightedChats);
                    AndroidAppHelper.currentApplication().startService(intent);
                    menuItem.setTitle(modRes.getString(R.string.menuitem_unhighlight));
                    ExtModule.this.showToast(modRes.getString(R.string.whatsapp_restart_message));
                    param.setResult(false);

                } else if (title.equals(modRes.getString(R.string.menuitem_unhighlight))) {
                    highlightedChats.remove(contactNumber);
                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra("action", 3);
                    intent.putExtra(ExtModule.HIGHLIGHTED_CHATS_PREF_STRING, highlightedChats);
                    AndroidAppHelper.currentApplication().startService(intent);
                    menuItem.setTitle(modRes.getString(R.string.menuitem_highlight));
                    ExtModule.this.showToast(modRes.getString(R.string.whatsapp_restart_message));
                    param.setResult(false);

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
                    if (enableHideCamera && param.thisObject instanceof ImageButton) {//param.thisObject.getClass().getName().equals("android.support.v7.widget.x")) {
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
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (!initPackageResourcesParam.packageName.equals("com.whatsapp"))
            return;

        modRes = XModuleResources.createInstance(MODULE_PATH, initPackageResourcesParam.res);

        if(sharedPreferences!=null && sharedPreferences.getBoolean("hideTabs",false))
            initPackageResourcesParam.res.setReplacement("com.whatsapp", "dimen", "tab_height", modRes.fwd(R.dimen.tab_height));
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
