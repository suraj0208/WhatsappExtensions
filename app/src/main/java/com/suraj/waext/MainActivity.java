package com.suraj.waext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;


public class MainActivity extends AppCompatActivity {
    private static final String PACKAGE_NAME = "com.suraj.waext";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences("myprefs", 1);
        editor = sharedPreferences.edit();

        setupLockUI();
        setupReminderUI();
        setupHighlightUI();

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

        final CheckBox checkBox = (CheckBox) (findViewById(R.id.chkboxhighlight));

        if (sharedPreferences.getBoolean("enableHighlight", false)) {
            checkBox.setChecked(true);
            btnchooser.setEnabled(true);
        } else {
            checkBox.setChecked(false);
            btnchooser.setEnabled(false);
        }
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    editor.putBoolean("enableHighlight", true);
                    btnchooser.setEnabled(true);
                } else {
                    editor.putBoolean("enableHighlight", false);
                    btnchooser.setEnabled(false);
                }
                editor.apply();
            }
        });


    }
}
