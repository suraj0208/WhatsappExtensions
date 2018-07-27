package com.suraj.waext;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
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
    public static final String UPDATE_INTENT = ".UPDATE_INTENT";
    public static final String WHITE_LIST_PREFS_STRING = "rd_whitelist";
    public static final String BLOCKED_CONTACTS_PREFS_STRING = "blockedContactList";


    public static String MODULE_PATH;

    private static HashSet<String> lockedContacts;
    private static HashSet<String> templockedContacts;
    private static HashSet<String> hiddenGroups;
    private static HashSet<String> highlightedChats;
    private static HashSet<String> whitelistSet;
    private static HashSet<String> blockContactsSet;

    private static HashMap<View, View> processedViewsHashMap;
    private static HashMap<View, View> zerothChildrenHashMap;
    private static HashMap<View, View> firstChildrenHashMap;
    private static HashMap<Object, String> tagToContactHashMap;
    private static HashMap<String, Object> nameToNumberHashMap;

    private static boolean showLockScreen = false;
    private static boolean firstTime = true;
    private static boolean enableHighlight = false;
    private static boolean enableHideSeen = false;
    private static boolean isGroup = false;
    private static boolean exceptionThrown = true;
    private static boolean enableHideCamera = false;
    private static boolean hideReadReceipts = false;
    private static boolean replaceCallButton = false;
    private static boolean hideDeliveryReports = false;
    private static boolean alwaysOnline = false;
    private static boolean hideNotifs = false;
    private static boolean lockWAWeb = false;
    private static boolean blackOrWhite = true;
    private static boolean lockArchived = false;
    private static boolean sessionExpired = true;
    private static boolean chatSessionOngoing = false;
    private static boolean enableRRDuringSession = false;
    private static boolean isViewProfilePhotoActivityOpen = false;
    private static boolean blockContacts = false;
    private static boolean hideToast = false;

    private static int highlightColor = Color.GRAY;
    private static int individualHighlightColor = Color.GRAY;
    private static int oneClickAction = 3;
    private static int whatsappPrivacyLastSeen = 0;

    private String archiveBooleanFieldName;

    private int originalColor = -1;
    private int lockAfter;

    private Class<?> settingClass;
    private Class<?> preferenceClass;
    private Class<?> archiveClass;
    private Class<?> fragmentStatePagerAdapterClass;

    private String contactNumber;

    //private Context context;
    private View oneClickActionButton;

    private Thread thread;

    private XSharedPreferences sharedPreferences;
    private XSharedPreferences whatsappPreferences;

    private UnlockReceiver unlockReceiver;
    private XModuleResources modRes;

    public ExtModule() {

    }

    private void hookMethodsForHighLight(final XC_LoadPackage.LoadPackageParam loadPackageParam) {


        XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                initPrefs();

                TypedValue a = new TypedValue();

                AndroidAppHelper.currentApplication().getApplicationContext().getTheme().resolveAttribute(android.R.attr.textColor, a, true);
                originalColor = a.data;

                ((Activity) param.thisObject).registerReceiver(unlockReceiver, new IntentFilter(ExtModule.UNLOCK_INTENT));

            }
        });

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setTag", Object.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        if (!(param.args[0] instanceof String))
                            return;

                        String tag = param.args[0].toString();

                        boolean localIsGroup = tag.contains("@g.us");

                        View parent = processedViewsHashMap.get(param.thisObject);

                        RelativeLayout rl = null;

                        String contact = null;

                        //XposedBridge.log("number: " + tag.replaceAll("[0-9]+","x"));

                        if (!localIsGroup) {
                            //XposedBridge.log("not a group");
                            if (tagToContactHashMap.get(tag) == null) {
                                try {
                                    if (!tag.contains(":"))
                                        return;

                                    contact = tag.split(":")[1];
                                    contact = contact.split("_")[0];
                                    contact = contact.replace("+", "");
                                    tagToContactHashMap.put(tag, contact);
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    return;
                                }
                            } else
                                contact = tagToContactHashMap.get(tag);

                        } else {
                            contact = tag.split("@")[0];
                        }
                        //XposedBridge.log("contact: " + contact.replaceAll("[0-9]+","x"));

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
                                //XposedBridge.log("ClassCastException in hookMethodsForHighLight");
                            }

                            if (rl != null) {
                                zerothChildrenHashMap.put(parent, rl.getChildAt(0));
                                firstChildrenHashMap.put(parent, rl.getChildAt(1));
                            }
                        }

                        View zerothChild = zerothChildrenHashMap.get(parent);
                        View firstChild = firstChildrenHashMap.get(parent);


                        //hide previews for locked contacts
                        try {
                            if (lockedContacts.contains(contact)) {
                                View v = getPreviewView(firstChild);
                                if (v != null)
                                    v.setVisibility(View.GONE);
                            } else {
                                View v = getPreviewView(firstChild);
                                if (v != null)
                                    v.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception ex) {
                            //who cares
                        }

                        if (!enableHighlight && highlightedChats.size() == 0)
                            return;

                        if (localIsGroup && enableHighlight) {
                            firstChild.setBackgroundColor(highlightColor);
                            zerothChild.setBackgroundColor(highlightColor);
                            firstChild.setPadding(0, 37, 30, 0);
                            ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -1;

                        } else {
                            /*if(contact !=null){
                                for(String con : highlightedChats){
                                    if(con.contains(contact) || contact.contains(con)){
                                        XposedBridge.log("matched contact: " + con.replaceAll("[0-9]+","x"));
                                    }
                                }
                            }*/

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

    private View getPreviewView(View firstChild) {
        if (firstChild instanceof ViewGroup) {
            View firstChildOfFirstChild /*f1*/ = ((ViewGroup) firstChild).getChildAt(1);
            if (firstChildOfFirstChild != null && firstChildOfFirstChild instanceof ViewGroup) {
                View firstChildOfF1 /*f2*/ = ((ViewGroup) firstChildOfFirstChild).getChildAt(1);
                if (firstChildOfF1 != null && firstChildOfF1 instanceof ViewGroup) {
                    View v = ((ViewGroup) firstChildOfF1).getChildAt(2);
                    return v;
                }
            }
        }
        return null;
    }

    public void hookInitialStage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Intent.class, "getStringExtra", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                String result = (String) param.getResult();

                if (result != null) {
                    isGroup = result.contains("@g.us");
                    if (result.contains("@")) {
                        contactNumber = result.split("@")[0];
                    }
                }
            }
        });
    }

    public void hookConversationMethods(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        final Class conversationClass = XposedHelpers.findClass("com.whatsapp.Conversation", loadPackageParam.classLoader);

        XposedHelpers.findAndHookMethod(conversationClass, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                chatSessionOngoing = false;

                if (enableHideSeen)
                    setSeenOff("2", ((Activity) param.thisObject).getApplicationContext());
            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                if (!enableHideSeen)
                    return;

                try {
                    Thread.sleep(200);
                    setSeenOff("0", ((Activity) param.thisObject).getApplicationContext());
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
                if (!isGroup && !replaceCallButton) {
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

                menu.getItem(menu.size() - 1).getSubMenu().add(modRes.getString(R.string.stats));
            }
        });

        /*XposedHelpers.findAndHookMethod(conversationClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                ((Activity) param.thisObject).registerReceiver(unlockReceiver, new IntentFilter(ExtModule.UNLOCK_INTENT));

            }

        });*/

        XposedHelpers.findAndHookMethod(conversationClass, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity activity = ((Activity) param.thisObject);

                //activity.unregisterReceiver(unlockReceiver);

                /*if (enableHideSeen) {
                    setSeenOff("2", activity.getApplicationContext());
                }else if (alwaysOnline){
                    XposedBridge.log("always online");
                    setSeenOff("5", ((Activity) param.thisObject).getApplicationContext());
                }*/

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

                if (title.equals("Info")) {
                    Toast.makeText(AndroidAppHelper.currentApplication(), "info info info", Toast.LENGTH_SHORT);
                    param.setResult(false);
                }


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

                } else if (title.equals(modRes.getString(R.string.menuitem_call)) || (title.equals(modRes.getString(R.string.voice_call_string)) && replaceCallButton)) {
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

                } else if (title.equals((modRes.getString(R.string.backup) + "/" + modRes.getString(R.string.restore)))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.BackupManagerActivity"));
                    intent.putExtra("jid", contactNumber + (isGroup ? "@g.us" : "@s.whatsapp.net"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);
                } else if (title.equals((modRes.getString(R.string.stats)))) {
                    intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.StatsActivity"));
                    intent.putExtra("jid", contactNumber + (isGroup ? "@g.us" : "@s.whatsapp.net"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    AndroidAppHelper.currentApplication().startActivity(intent);
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

                if (thread == null || !thread.isAlive())
                    startDaemon();

            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                String className = param.thisObject.getClass().getName();
                switch (className) {
                    case "com.whatsapp.Conversation":

                        if (templockedContacts.contains(contactNumber)) {
                            startLockActivity(param.thisObject);
                        }
                        break;

                    case "com.whatsapp.ArchivedConversationsActivity":
                        if (!lockArchived)
                            return;

                        if (sessionExpired)
                            startLockActivity(param.thisObject);
                        break;
                }

                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }

            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.ArchivedConversationsActivity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!lockArchived)
                    return;

                firstTime = true;
                showLockScreen = true;
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.Conversation", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                firstTime = true;
                showLockScreen = true;
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loadPackageParam.classLoader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity activity = ((Activity) (param.thisObject));
                activity.unregisterReceiver(unlockReceiver);

                if (alwaysOnline) {
                    setSeenOff(Integer.toString(whatsappPrivacyLastSeen), activity.getApplicationContext());
                }
            }
        });

    }

    private void startLockActivity(Object thisObject) {
        if (firstTime && showLockScreen) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.LockActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            AndroidAppHelper.currentApplication().startActivity(intent);
            firstTime = false;
        } else if (!firstTime && showLockScreen) {
            ((Activity) thisObject).finish();
        }
    }


    //daemon thread to lock contacts periodically
    private void startDaemon() {
        thread = new Thread() {
            @Override
            public void run() {

                try {
                    //XposedBridge.log("Thread started");
                    Thread.sleep(4000);

                    //if (enableHideSeen)
                    //setSeenOff("2");

                    Thread.sleep(lockAfter * 1000 * 60);
                    ExtModule.templockedContacts.addAll(lockedContacts);
                    sessionExpired = true;
                } catch (InterruptedException e) {
                    //XposedBridge.log("Thread Inturrupted");
                    //e.printStackTrace();
                }

            }
        };

        thread.setDaemon(true);
        thread.start();

    }

    private void setSeenOff(final String val, final Context context) {
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

                    XposedHelpers.callMethod(settingsObject, "b", preferenceObject, val);

                } catch (Throwable e) {
                    XposedBridge.log("exception caught");
                    e.printStackTrace();
                }
            }
        });
    }

    Context newContext;

    private void hookMethodsForUpdatePrefs(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.whatsapp.BootReceiver", loadPackageParam.classLoader, "onReceive", Context.class, Intent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Context context = (Context) param.args[0];

                //Context appContext = AndroidAppHelper.currentApplication().getApplicationContext(); // context.getApplicationContext();

                newContext = context.createPackageContext(ExtModule.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);

                newContext.registerReceiver(new UpdateReceiver(), new IntentFilter(ExtModule.PACKAGE_NAME + UPDATE_INTENT));
                //XposedBridge.log("Registed receiver for update");

            }
        });
    }

    private void hookMethodsForCameraAndZoom(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.whatsapp.ViewProfilePhoto", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                isViewProfilePhotoActivityOpen = true;
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.ViewProfilePhoto", loadPackageParam.classLoader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                isViewProfilePhotoActivityOpen = false;
            }
        });


        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setVisibility", int.class, new de.robv.android.xposed.XC_MethodHook() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                if (!param.args[0].toString().equals("0"))
                    return;

                try {                                       // a pretty naive try catch -- until I get the logs from GBwhatsapp
                    View view = (View) param.thisObject;

                    if (view.getTransitionName() == null) {
                        if (enableHideCamera && param.thisObject instanceof ImageButton) {//param.thisObject.getClass().getName().equals("android.support.v7.widget.x")) {
                            View parent = (View) view.getParent();

                            if (parent instanceof LinearLayout) {
                                StackTraceElement[] stackTraceElements = new Exception().getStackTrace();

                                if (stackTraceElements[4].getMethodName().equals("afterTextChanged") || stackTraceElements[5].getMethodName().equals("onCreate")) {
                                    view.setVisibility(View.GONE);
                                    //((View) view.getParent()).setBackgroundColor(Color.BLACK);
                                }
                            }
                        }
                        return;
                    }

                    if (param.thisObject instanceof ImageView && isViewProfilePhotoActivityOpen) {
                        new PhotoViewAttacher((ImageView) param.thisObject);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
    }

    private void hookMethodsForPhotoViewAttacher(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(PhotoViewAttacher.class, "checkImageViewScaleType", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(null);
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

    private void hookMethodsForHideGroup(final XC_LoadPackage.LoadPackageParam loadPackageParam) {


        XposedHelpers.findAndHookMethod("java.util.concurrent.ConcurrentHashMap", loadPackageParam.classLoader, "get", Object.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
/*
                String number = param.args[0].toString().split("@")[0];

                if(blockContacts && blockContactsSet.contains(number))
                    param.setResult(null);
*/
            }

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
                    Pattern pattern = Pattern.compile("[0-9]+[\\-]*[0-9]*");
                    Matcher matcher = pattern.matcher(param.args[0].toString().split("@")[0]);

                    if (matcher.matches()) {
                        archiveClass = param.getResult().getClass();

                        for (Field field : archiveClass.getDeclaredFields()) {
                            if (field.getType().getName().equals("boolean")) {
                                archiveBooleanFieldName = field.getName();
                                exceptionThrown = false;
                                break;
                            }
                        }
                    } else {
                        return;
                    }
                }

                if (!(archiveClass != null && archiveClass.isInstance(param.getResult()))) {
                    return;
                }

                String number = param.args[0].toString().split("@")[0];

                if (!hiddenGroups.contains(number))
                    return;

                Field f = param.getResult().getClass().getDeclaredField(archiveBooleanFieldName);
                f.setAccessible(true);
                f.set(param.getResult(), true);

                //XposedBridge.log(param.args[0] + " " + param.getResult().getClass().getName());
            }
        });
    }

    public void hookMethodsForHideReadReceipts(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        final Class readReceiptsJobClass = XposedHelpers.findClass("com.whatsapp.jobqueue.job.SendReadReceiptJob", loadPackageParam.classLoader);

        XposedHelpers.findAndHookMethod("com.whatsapp.jobqueue.job.SendReadReceiptJob", loadPackageParam.classLoader, "c", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                if (chatSessionOngoing && enableRRDuringSession)
                    return;

                boolean shouldSend = false;

                Field jidField = readReceiptsJobClass.getDeclaredField("jid");
                Field participantField = readReceiptsJobClass.getDeclaredField("participant");

                jidField.setAccessible(true);
                participantField.setAccessible(true);

                Object jidString = jidField.get(param.thisObject);
                Object participantString = participantField.get(param.thisObject);


                if (jidString != null)
                    shouldSend = shouldSend || whitelistSet.contains(jidString.toString().split("@")[0]);

                if (participantString != null)
                    shouldSend = shouldSend || whitelistSet.contains(participantString.toString().split("@")[0]);

                //blackOrWhite = true -> whitelist else blacklist
                if (!blackOrWhite) {
                    shouldSend = !shouldSend;
                }

                if (hideReadReceipts && !shouldSend) {
                    param.setResult(null);
                }
            }
        });

        XposedHelpers.findAndHookMethod("com.whatsapp.jobqueue.job.SendE2EMessageJob", loadPackageParam.classLoader, "b", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                chatSessionOngoing = true;

            }
        });

    }

    public void hookMethodsForHideDeliveryReports(final XC_LoadPackage.LoadPackageParam loadPackageParam) {

        try {
            XposedHelpers.findAndHookMethod("com.whatsapp.messaging.h", loadPackageParam.classLoader, "a", Message.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    Message message = (Message) param.args[0];

                    if (hideDeliveryReports && message.arg1 == 9 && message.arg2 == 0)
                        param.setResult(null);

                    //XposedBridge.log("" + message.arg1 + " " + message.arg2 + " " + message.toString());

                    /*
                for(String k : bundle.keySet())
                    XposedBridge.log(k + " " + bundle.get(k));

                for(StackTraceElement stackTraceElement:new Exception().getStackTrace())
                   XposedBridge.log(stackTraceElement.getClassName() + " " + stackTraceElement.getMethodName());

                XposedBridge.log("_____________________________________");

                String className = new Exception().getStackTrace()[4].getClassName();

                if(className.equals("com.whatsapp.xi") || className.equals("com.whatsapp.aae$a") ) {
                    param.setResult(null);
                    XposedBridge.log("skip " +message.arg1 + " " + message.arg2 + " "+ message.toString());
                }

                */
                }
            });

        } catch (XposedHelpers.ClassNotFoundError ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodError ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } catch (Error ex) {
            ex.printStackTrace();
        }
    }

    private void hookMethodsForAlwaysOnline(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (settingClass == null)
            return;

        XposedHelpers.findAndHookMethod(settingClass, "a", Preference.class, Object.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (whatsappPreferences != null) {

                    if (!(param.args[0] instanceof ListPreference)) {
                        return;
                    }

                    ListPreference listPreference = (ListPreference) param.args[0];

                    if (listPreference.getKey().equals("privacy_last_seen")) {
                        whatsappPrivacyLastSeen = Integer.parseInt(param.args[1].toString());
                    }
                }
            }
        });

    }

    public void hookMethodsForClickToReply(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.widget.HeaderViewListAdapter", loadPackageParam.classLoader, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                if (param.args[2] != null && !param.args[2].getClass().getName().equals("com.whatsapp.ConversationListView"))
                    return;

                if (param.args[1] != null) {
                    try {
                        ((View) param.args[1]).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if (oneClickAction == 3)
                                    return;

                                oneClickActionButton = null;

                                v.performLongClick();

                                if (oneClickActionButton != null)
                                    oneClickActionButton.performClick();
                                else
                                    v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                            }
                        });

                    } catch (ArrayIndexOutOfBoundsException ex) {
                        XposedBridge.log(ex.toString());
                    }
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.support.v7.view.menu.ActionMenuItemView", loadPackageParam.classLoader, "setTitle", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                if (param.args[0] == null) {
                    XposedBridge.log("actionmenu:Title null");
                    return;
                }

                if (param.args[0].toString().equals(getOneClickActionString())) {
                    oneClickActionButton = (View) param.thisObject;
                }

            }
        });
    }

    public void hookMethodsForHidingNotifications(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.app.Notification.Builder", loadPackageParam.classLoader, "build", new XC_MethodHook() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                //for opening chat when unlocking from notification
                AndroidAppHelper.currentApplication().getApplicationContext().registerReceiver(unlockReceiver, new IntentFilter(ExtModule.UNLOCK_INTENT));

                if (!hideNotifs)
                    return;

                Notification notification = (Notification) (param.getResult());

                String currentContact = notification.extras.get(Notification.EXTRA_TITLE).toString();

                if (nameToNumberHashMap == null) {
                    nameToNumberHashMap = WhatsAppDatabaseHelper.getNameToNumberHashMap();
                }

                Object value = nameToNumberHashMap.get(currentContact);

                boolean isLocked = false;

                if (value instanceof String) {
                    isLocked = lockedContacts.contains(value.toString());
                } else if (value instanceof List) {
                    for (Object number : (List) value) {

                        if (lockedContacts.contains(number.toString())) {
                            isLocked = true;
                            break;
                        }

                    }
                }

                String xes = " ";

                if (isLocked) {
                    notification.extras.putString(Notification.EXTRA_TEXT, xes);
                    notification.extras.putString(Notification.EXTRA_BIG_TEXT, xes);
                    notification.extras.putStringArray(Notification.EXTRA_TEXT_LINES, new String[]{xes});
                }

                CharSequence[] notificationTexts = null;

                if (notification.extras.get(Notification.EXTRA_TEXT_LINES) instanceof CharSequence[])
                    notificationTexts = (CharSequence[]) notification.extras.get(Notification.EXTRA_TEXT_LINES);


                if (notificationTexts == null)
                    return;

                String[] newSequences = new String[notificationTexts.length];


                for (int i = 0; i < notificationTexts.length; i++) {
                    String[] currentSplits = notificationTexts[i].toString().split(":");
                    Object val = nameToNumberHashMap.get(currentSplits[0].trim());

                    isLocked = false;
                    if (val instanceof String)
                        isLocked = lockedContacts.contains(val);

                    else if (val instanceof List) {
                        for (String number : (List<String>) val) {
                            if (lockedContacts.contains(number)) {
                                isLocked = true;
                                break;
                            }
                        }
                    }

                    if (isLocked) {
                        newSequences[i] = currentSplits[0] + ": " + xes;
                    } else
                        newSequences[i] = notificationTexts[i].toString();
                }

                notification.extras.putStringArray(Notification.EXTRA_TEXT_LINES, newSequences);
            }
        });
    }

    private void hookMethodsForHideStatusTab(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            /*XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loadPackageParam.classLoader, "c", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if ((int) param.args[0] == 2)
                        param.setResult(3);
                }
            });
*/
            //correct
            XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity", loadPackageParam.classLoader, "j", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if ((int) param.args[0] == 2)
                        param.setResult(3);
                }
            });

            //correct
            XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity$b", loadPackageParam.classLoader, "a", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if ((int) param.args[0] == 2)
                        param.args[0] = 3;
                }
            });

            XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity$b", loadPackageParam.classLoader, "c", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if ((int) param.args[0] == 2)
                        param.args[0] = 3;
                }
            });

            printMethodOfClass("com.whatsapp.HomeActivity$b",loadPackageParam);
            //correct
            XposedHelpers.findAndHookMethod("com.whatsapp.HomeActivity$b", loadPackageParam.classLoader, "b", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.setResult(3);
                }
            });

        } catch (NoSuchMethodError error) {
            error.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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
        whitelistSet = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.WHITE_LIST_PREFS_STRING, new HashSet<String>());
        blockContactsSet = (HashSet<String>) sharedPreferences.getStringSet(ExtModule.BLOCKED_CONTACTS_PREFS_STRING, new HashSet<String>());

        highlightColor = sharedPreferences.getInt("highlightColor", Color.GRAY);
        individualHighlightColor = sharedPreferences.getInt("individualHighlightColor", Color.GRAY);
        oneClickAction = sharedPreferences.getInt("oneClickAction", 3);

        enableHighlight = sharedPreferences.getBoolean("enableHighlight", false);
        replaceCallButton = sharedPreferences.getBoolean("replaceCallButton", false);
        enableHideSeen = sharedPreferences.getBoolean("hideSeen", false);
        enableHideCamera = sharedPreferences.getBoolean("hideCamera", false);
        hideReadReceipts = sharedPreferences.getBoolean("hideReadReceipts", false);
        hideDeliveryReports = sharedPreferences.getBoolean("hideDeliveryReports", false);
        alwaysOnline = sharedPreferences.getBoolean("alwaysOnline", false);
        hideNotifs = sharedPreferences.getBoolean("hideNotifs", false);
        lockWAWeb = sharedPreferences.getBoolean("lockWAWeb", false);
        blackOrWhite = sharedPreferences.getBoolean("blackOrWhite", true);
        lockArchived = sharedPreferences.getBoolean("lockArchived", false);
        enableRRDuringSession = sharedPreferences.getBoolean("enableRRDuringSession", false);
        blockContacts = sharedPreferences.getBoolean("blockContacts", false);
        hideToast = sharedPreferences.getBoolean("hideToast", false);
    }

    private void initVars(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        templockedContacts = new HashSet<>();
        templockedContacts.addAll(lockedContacts);

        //value of timer after which contact is to locked
        lockAfter = getLockAfter(sharedPreferences.getInt("lockAfter", 2));

        /*if (nameToNumberHashMap == null) {
            (new Handler(AndroidAppHelper.currentApplication().getMainLooper())).post(
                    (new Runnable() {
                        @Override
                        public void run() {
                            nameToNumberHashMap = WhatsAppDatabaseHelper.getNameToNumberHashMap();
                        }
                    }));
        }*/

        whatsappPreferences = new XSharedPreferences("com.whatsapp", "com.whatsapp_preferences");


        if (whatsappPreferences != null) {
            whatsappPreferences.makeWorldReadable();
            whatsappPrivacyLastSeen = whatsappPreferences.getInt("privacy_last_seen", 0);
        } else {
            XposedBridge.log("whatsapp prefs null");
        }

        try {
            settingClass = XposedHelpers.findClass("com.whatsapp.SettingsPrivacy", loadPackageParam.classLoader);
            //preferenceClass = XposedHelpers.findClass("com.whatsapp.preference.WaCheckBoxPreference", loadPackageParam.classLoader);
            preferenceClass = XposedHelpers.findClass("com.whatsapp.preference.WaPrivacyPreference", loadPackageParam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError error) {
            error.printStackTrace();
        }

    }


    public String getOneClickActionString() {
        return modRes.getStringArray(R.array.oneclickactions)[oneClickAction];
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

            sessionExpired = false;

            //XposedBridge.log("Broadcast Received " + showLockScreen + " " + firstTime);
        }
    }

    class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            initPrefs();
            //XposedBridge.log("Recieved intent");
        }
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

        if (sharedPreferences != null && sharedPreferences.getBoolean("hideTabs", false))
            initPackageResourcesParam.res.setReplacement("com.whatsapp", "dimen", "tab_height", modRes.fwd(R.dimen.tab_height));

        if (sharedPreferences != null && sharedPreferences.getBoolean("showBlackTicks", false)) {
            initPackageResourcesParam.res.setReplacement("com.whatsapp", "drawable", "message_got_read_receipt_from_target", modRes.fwd(R.mipmap.ic_black_tick_conv));
            initPackageResourcesParam.res.setReplacement("com.whatsapp", "drawable", "message_got_read_receipt_from_target_onmedia", modRes.fwd(R.mipmap.ic_black_tick_conv));
            initPackageResourcesParam.res.setReplacement("com.whatsapp", "drawable", "msg_status_client_read", modRes.fwd(R.mipmap.ic_black_tick_main));
        }

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
        hookMethodsForHideReadReceipts(loadPackageParam);
        hookMethodsForClickToReply(loadPackageParam);
        hookMethodsForHideDeliveryReports(loadPackageParam);
        hookMethodsForHidingNotifications(loadPackageParam);
        hookMethodsForToastWorkAround(loadPackageParam);
        //prevent IllegalStateException while closing profile photo
        hookMethodsForPhotoViewAttacher(loadPackageParam);
        //hookMethodsForUpdatePrefs(loadPackageParam);

        unlockReceiver = new UnlockReceiver();

        initPrefs();

        initVars(loadPackageParam);
        //call it here
        hookMethodsForAlwaysOnline(loadPackageParam);

        if (sharedPreferences.getBoolean("hideStatusTab", false))
            hookMethodsForHideStatusTab(loadPackageParam);
    }

    private void hookMethodsForToastWorkAround(XC_LoadPackage.LoadPackageParam loadPackageParam){
        XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if(hideToast){
                    param.setResult(null);
                }
            }
        });

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

/*
changelog 5.0
custom contact picker - blacklist a group
lock archived chats
opening lock for critical preference changes
read receipts during chat session
hide message preview for locked contacts

fixed chat not opening from notification for locked contacts
fixed exception while opening profile photo

copiled using latest sdk
fixed margins
log error while database reading
minor performance improvements
 */
