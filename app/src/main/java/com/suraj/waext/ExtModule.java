package com.suraj.waext;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
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

    public static final String LOCKED_CONTACTS_PREF_STRING = "lockedContacts";
    public static final String PACKAGE_NAME = "com.suraj.waext";
    public static final String UNLOCK_INTENT = ExtModule.PACKAGE_NAME + ".UNLOCK_INTENT";
    private static final String WALLPAPERDIR = "/WhatsApp/Media/WallPaper/";

    private static HashSet<String> lockedContacts;
    private static HashSet<String> templockedContacts;

    private static HashMap<View, View> processedViews;
    private static HashMap<View, View> zerothChildren;
    private static HashMap<View, View> firstChildren;

    private static boolean showLockScreen = false;
    private static boolean firstTime = true;
    private static boolean enableHighlight = false;

    private int originalColor = -1;
    private static int highlightColor = Color.GRAY;

    private String contactNumber;

    private XSharedPreferences sharedPreferences;

    private UnlockReceiver unlockReceiver;

    private Thread thread;

    private int lockAfter;

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

                        if (!enableHighlight)
                            return;

                        Object tag = param.args[0];

                        View parent = processedViews.get(param.thisObject);

                        RelativeLayout rl;

                        if (parent == null) {
                            View contactPictureViewBackground = (View) ((View) param.thisObject).getParent();

                            if (contactPictureViewBackground == null)
                                return;

                            parent = (View) contactPictureViewBackground.getParent();

                            if (parent == null)
                                return;

                            processedViews.put((View) param.thisObject, parent);

                            rl = (RelativeLayout) parent;

                            zerothChildren.put(parent, rl.getChildAt(0));
                            firstChildren.put(parent, rl.getChildAt(1));

                        }

                        View zerothChild = zerothChildren.get(parent);

                        View firstChild = firstChildren.get(parent);

                        if (tag.toString().contains("@g.us")) {

                            firstChild.setBackgroundColor(highlightColor);
                            zerothChild.setBackgroundColor(highlightColor);

                            if (firstChild instanceof LinearLayout) {
                                firstChild.setPadding(5, 5, 5, 5);
                                ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -1;
                            }

                        } else {

                            firstChild.setBackgroundColor(originalColor);
                            zerothChild.setBackgroundColor(originalColor);

                            if (firstChild instanceof LinearLayout) {
                                ((RelativeLayout.LayoutParams) firstChild.getLayoutParams()).height = -2;
                            }
                        }

                    }
                }
        );
    }

    private void updateHighlightColor() {
        sharedPreferences.reload();
        sharedPreferences.makeWorldReadable();
        highlightColor = sharedPreferences.getInt("highlightColor", Color.GRAY);
        enableHighlight = sharedPreferences.getBoolean("enableHighlight", false);
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
                //XposedBridge.log("onResume");

                if (ExtModule.showLockScreen && !firstTime) {
                    //XposedBridge.log("finished activity");
                    ((Activity) param.thisObject).finish();
                } else
                    firstTime = false;

            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                //XposedBridge.log("onCreateOptionMenu");

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

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Menu menu = (Menu) param.args[0];

                File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPERDIR + contactNumber + ".jpg");
                if (f.exists() && !f.isDirectory())
                    menu.getItem(menu.size() - 1).getSubMenu().add("Remove Wallpaper");
                else
                    menu.getItem(menu.size() - 1).getSubMenu().add("Custom Wallpaper");

            }
        });

        XposedHelpers.findAndHookMethod(conversationClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                //XposedBridge.log("onCreate");
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

                //important: param.setResult(false) to prevent call to original method

                if (menuItem.getTitle() == "Lock") {
                    lockedContacts.add(contactNumber);
                    templockedContacts.add(contactNumber);

                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));
                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);
                    AndroidAppHelper.currentApplication().startService(intent);
                    //XposedBridge.log("called the intent for adding lock");

                    ExtModule.this.showToast("Lock Enabled for this contact.");

                    ((Activity) param.thisObject).finish();
                    param.setResult(false);

                } else if (menuItem.getTitle() == "Unlock") {
                    lockedContacts.remove(contactNumber);
                    templockedContacts.remove(contactNumber);

                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(ExtModule.PACKAGE_NAME, "com.suraj.waext.LockPreferencesService"));

                    intent.putExtra(ExtModule.LOCKED_CONTACTS_PREF_STRING, lockedContacts);

                    AndroidAppHelper.currentApplication().startService(intent);
                    ExtModule.this.showToast("Lock disabled for this contact.");

                    menuItem.setVisible(false);
                    param.setResult(false);

                } else if (menuItem.getTitle() == "Call") {
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

                } else if (menuItem.getTitle().toString().equals("Add Reminder")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.ReminderActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("contactNumber", contactNumber);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);
                } else if (menuItem.getTitle().toString().equals("Custom Wallpaper")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.suraj.waext", "com.suraj.waext.CropActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("contactNumber", contactNumber);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                    param.setResult(false);
                    ExtModule.this.showToast("ReOpen chat to see effects");
                } else if (menuItem.getTitle().toString().equals("Remove Wallpaper")) {
                    File f = new File(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPERDIR + contactNumber + ".jpg");
                    if (f.exists())
                        f.delete();
                    param.setResult(false);
                    ExtModule.this.showToast("ReOpen chat to see effects");

                }
            }

        });

    }

    private void hookMethodsForLock(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                Activity activity = (Activity) param.thisObject;

                //XposedBridge.log(activity.getClass().getName());

                if (!activity.getClass().getName().equals("com.whatsapp.HomeActivity"))
                    return;

                if (thread == null || !thread.isAlive())
                    startDaemon();

            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

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
                    Thread.sleep(4000);
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
        highlightColor = sharedPreferences.getInt("highlightColor", Color.GRAY);
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

    private void hookMethodsForWallPaper(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.whatsapp.wallpaper.WallPaperView", loadPackageParam.classLoader, "setDrawable", Drawable.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                //XposedBridge.log(param.args[0].getClass().getName());

                Drawable drawable = Drawable.createFromPath(Environment.getExternalStorageDirectory() + ExtModule.WALLPAPERDIR + contactNumber + ".jpg");

                if (drawable != null)
                    param.args[0] = drawable;

            }
        });
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws
            Throwable {

        if (!loadPackageParam.packageName.equals("com.whatsapp"))
            return;

        XposedBridge.log("com.suraj.waext Loaded: " + loadPackageParam.packageName);

        sharedPreferences = new XSharedPreferences(ExtModule.PACKAGE_NAME, "myprefs");

        processedViews = new HashMap<>();
        zerothChildren = new HashMap<>();
        firstChildren = new HashMap<>();

        hookConversationMethods(loadPackageParam);
        hookInitialStage(loadPackageParam);
        hookMethodsForLock(loadPackageParam);
        hookMethodsForHighLight(loadPackageParam);
        hookMethodsForWallPaper(loadPackageParam);

        unlockReceiver = new UnlockReceiver();

        initPrefs();
        templockedContacts = new HashSet<>();
        templockedContacts.addAll(lockedContacts);

        //value of timer after which contact is to locked
        lockAfter = getLockAfter(sharedPreferences.getInt("lockAfter", 2));

        //if (lockAfter != 0)
        //    startDaemon();

    }


}
