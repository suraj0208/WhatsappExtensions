package com.suraj.waext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.io.File;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity {
    private static final String PACKAGE_NAME = "com.suraj.waext";

    private static boolean isWaitingForLock = false;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private UnlockReceiver unlockReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = Utils.getSharedPreferences(this);
        editor = Utils.getEditor(this);

        unlockReceiver = new UnlockReceiver();

        setupLockUI();
        setupReminderUI();
        setupHighlightUI();
        setupPrivacyUI();
        setUpLayoutUI();


    }

    private void setUpLayoutUI() {
        CheckBox checkBoxHideCamera = (CheckBox) findViewById(R.id.chkboxhidecamera);
        CheckBox checkBoxHideTabs = (CheckBox) findViewById(R.id.chkboxhidetabs);
        CheckBox checkBoxReplaceCallButton = (CheckBox) findViewById(R.id.chkboxreplacecallbtn);
        CheckBox checkBoxBlackTicks = (CheckBox) findViewById(R.id.chkboxblackticks);
        CheckBox checkBoxHideStatusTab = (CheckBox) findViewById(R.id.chkboxHideStatusTab);
        CheckBox checkBoxHideToast = (CheckBox) findViewById(R.id.chkboxHideToast);

        Utils.setUpCheckBox(this, checkBoxHideCamera, "hideCamera", false, "", false, "");
        Utils.setUpCheckBox(this, checkBoxHideTabs, "hideTabs", true, getApplicationContext().getString(R.string.req_restart), true, getApplicationContext().getString(R.string.req_restart));
        Utils.setUpCheckBox(this, checkBoxReplaceCallButton, "replaceCallButton", false, "", false, "");
        Utils.setUpCheckBox(this, checkBoxBlackTicks, "showBlackTicks", true, getApplicationContext().getString(R.string.req_restart), true, getApplicationContext().getString(R.string.req_restart));
        Utils.setUpCheckBox(this, checkBoxHideStatusTab, "hideStatusTab", true, getApplicationContext().getString(R.string.req_restart), true, getApplicationContext().getString(R.string.req_restart));
        Utils.setUpCheckBox(this, checkBoxHideToast, "hideToast", false, "", false, "");

        Spinner spinSingleClickActions = (Spinner) findViewById(R.id.spinsingleclickactions);
        spinSingleClickActions.setSelection(sharedPreferences.getInt("oneClickAction", 3));

        spinSingleClickActions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("oneClickAction", position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }


    private void setupPrivacyUI() {
        final CheckBox checkBoxSeen = (CheckBox) findViewById(R.id.chkboxseen);
        final CheckBox checkBoxReadReports = (CheckBox) findViewById(R.id.chkboxreadreceipts);
        final CheckBox checkBoxDeliveryReports = (CheckBox) findViewById(R.id.chkboxdeliveryreports);

        Utils.setUpCheckBox(this, checkBoxSeen, "hideSeen", false, "", true, getApplicationContext().getString(R.string.restore_prefs));
        Utils.setUpCheckBox(this, checkBoxReadReports, "hideReadReceipts", false, "", false, "");
        Utils.setUpCheckBox(this, checkBoxDeliveryReports, "hideDeliveryReports", false, "", false, "");
        Utils.setUpCheckBox(this, (CheckBox) findViewById(R.id.chkboxalwaysonline), "alwaysOnline", true, getApplicationContext().getString(R.string.last_seen_hidden), false, "");
        //Utils.setUpCheckBox(this, (CheckBox) findViewById(R.id.chkboxBlockContacts), "blockContacts", false, "", false, "");
        Utils.setUpCheckBox(this, (CheckBox) findViewById(R.id.chkboxPreventDeletion), "preventRemoteDeletion", false, "", false, "");
        Utils.setUpCheckBox(this, (CheckBox) findViewById(R.id.chkboxPreventDeletionNotification), "remoteDeletionNotification", false, "", false, "");

        findViewById(R.id.imgbtnreceiptsetting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WhiteListActivity.class));
            }
        });

        findViewById(R.id.imgbtnblocksetting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BlockedContactsActivity.class));
            }
        });

        final CheckBox checkBoxBlockedContacts = findViewById(R.id.chkboxBlockContacts);
        checkBoxBlockedContacts.setChecked(sharedPreferences.getBoolean("blockContacts", false));

        checkBoxBlockedContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putBoolean("blockContacts", checkBoxBlockedContacts.isChecked());
                editor.apply();

                final HashSet<String> blockedContactsSet = new HashSet<>(sharedPreferences.getStringSet("blockedContactList", new HashSet<String>()));
                (new Thread() {
                    @Override
                    public void run() {
                        for (String value : blockedContactsSet) {
                            try {
                                WhatsAppDatabaseHelper.clearNullItemsFromMessages(value + "@g.us");
                                WhatsAppDatabaseHelper.clearNullItemsFromMessages(value + "@s.whatsapp.net");
                            } catch (WhatsAppDBException e) {
                                Log.e(ExtModule.PACKAGE_NAME, e.getMessage());
                            }
                        }
                    }
                }).start();
            }
        });


    }

    private void setupLockUI() {
        Spinner spinner = (Spinner) (findViewById(R.id.spinminutes));

        spinner.setSelection(sharedPreferences.getInt("lockAfter", 2));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("lockAfter", position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });


        (findViewById(R.id.btnchangepass)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, ChangePasswordActivity.class));
                    }
                }

        );

        setUpProtectedCheckBox((CheckBox) findViewById(R.id.chkboxHideNotifs), "hideNotifs", false, "", false, "");
        setUpProtectedCheckBox((CheckBox) findViewById(R.id.chkboxLockWAWeb), "lockWAWeb", false, "", false, "");
        setUpProtectedCheckBox((CheckBox) findViewById(R.id.chkboxLockArchived), "lockArchived", false, "", false, "");
    }


    private void setupReminderUI() {
        final CheckBox checkBox = (CheckBox) (findViewById(R.id.chkboxservicestate));

        if (ReminderService.isRunning) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    startService(new Intent(MainActivity.this, ReminderService.class));
                } else {
                    Intent stopIntent = new Intent(MainActivity.this, ReminderService.class);
                    stopIntent.putExtra("stopService", true);
                    startService(stopIntent);
                }
            }
        });
    }


    private void setupHighlightUI() {
        final Button btnchooser = (Button) findViewById(R.id.btncolorchooser);

        btnchooser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ColorChooserActivity.class));

            }
        });

        btnchooser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ColorChooserActivity.class);
                intent.putExtra("groupOrIndividual", "highlightColor");
                startActivity(intent);

            }
        });


        findViewById(R.id.btnindividualcolorchooser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ColorChooserActivity.class);
                intent.putExtra("groupOrIndividual", "individualHighlightColor");
                startActivity(intent);
            }
        });

        final CheckBox checkBox = (CheckBox) (findViewById(R.id.chkboxhighlight));

        Utils.setUpCheckBox(this, checkBox, "enableHighlight", false, "", false, "");

    }

    private void setUpProtectedCheckBox(final CheckBox checkBox, final String prefname, final boolean onToast, final String onMessage, final boolean offToast, final String offMessage) {
        if (sharedPreferences.getBoolean(prefname, false))
            checkBox.setChecked(true);
        else
            checkBox.setChecked(false);

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent lockIntent = new Intent(MainActivity.this, LockActivity.class);

                lockIntent.putExtra("hasPref", true);
                Bundle extras = new Bundle();
                extras.putBoolean(prefname, checkBox.isChecked());
                lockIntent.putExtras(extras);

                MainActivity.isWaitingForLock = true;

                checkBox.setChecked(!checkBox.isChecked());
                unlockReceiver.setCheckBox(v);
                startActivity(lockIntent);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.isWaitingForLock = false;
        this.registerReceiver(unlockReceiver, new IntentFilter(ExtModule.UNLOCK_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!MainActivity.isWaitingForLock) {
            this.unregisterReceiver(unlockReceiver);
        }

        Utils.setPreferencesRW(this);

    }

    class UnlockReceiver extends BroadcastReceiver {
        private CheckBox checkBox;

        public void setCheckBox(View view) {
            if (view instanceof CheckBox)
                this.checkBox = (CheckBox) view;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getBundleExtra("prefs");
            if (bundle != null) {
                for (String k : bundle.keySet()) {
                    if (bundle.get(k) instanceof Boolean) {
                        editor.putBoolean(k, bundle.getBoolean(k));
                        checkBox.setChecked(bundle.getBoolean(k));
                    }
                }
                editor.apply();
            }
        }
    }
}
