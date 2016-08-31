package com.suraj.waext;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LockActivity extends AppCompatActivity {
    private static final String PACKAGE_NAME = "com.suraj.waext";
    private EditText etpassword;
    private byte[] decodedBytes;
    private String decodedString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_lock);

        etpassword = (EditText) (findViewById(R.id.etpassword));

        assert etpassword != null;
        etpassword.setEnabled(false);

        setEditTextListeners();

        setButtonListeners();

        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);

        byte[] defaultEncoded = Base64.encode("1234".getBytes(), 0);

        String encodedString = sharedPreferences.getString("password", new String(defaultEncoded));
        decodedBytes = Base64.decode(encodedString.getBytes(), 0);
        decodedString = new String(decodedBytes);

    }

    private void setButtonListeners() {
        View.OnClickListener digitClickListner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etpassword.setText(etpassword.getText().append(((Button) v).getText()));
            }
        };

        View digitButtons[] = {findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
                findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
                findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
                findViewById(R.id.btn9)};

        for (View v : digitButtons) {
            v.setOnClickListener(digitClickListner);
        }

        @SuppressLint("ShowToast")
        final Toast incorrectPasswordToast = Toast.makeText(getApplicationContext(), "Incorrect Password.", Toast.LENGTH_SHORT);

        findViewById(R.id.btnenter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (etpassword.getText().toString().equals(decodedString)) {
                    unLock();
                    LockActivity.this.finish();
                } else {
                    incorrectPasswordToast.show();
                }
            }
        });

        (findViewById(R.id.btnclear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etpassword.setText("");
            }
        });

    }

    private void setEditTextListeners() {


        final Toast lengthGreatertoast = Toast.makeText(getApplicationContext(), "Length cannot be more than 4.", Toast.LENGTH_SHORT);

        etpassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 4) {
                    etpassword.setText(s.subSequence(0, 4));
                    lengthGreatertoast.show();
                }

                if (etpassword.getText().toString().equals(decodedString)) {
                    unLock();
                    LockActivity.this.finish();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void unLock() {
        Intent intent = new Intent();
        intent.setAction(LockActivity.PACKAGE_NAME + ".Unlock_Intent");
        intent.putExtra("showLockScreen", false);
        intent.putExtra("firstTime", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendBroadcast(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
