package com.suraj.waext;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChangePasswordActivity extends AppCompatActivity {

    private int count = 0;
    private String firstPassword;
    private TextView tvDefaultPassword;
    private EditText etPassword;
    private SharedPreferences sharedPreferences;
    private String decodedString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        etPassword = (EditText) (findViewById(R.id.etPassword));
        tvDefaultPassword = (TextView) (findViewById(R.id.tvDefaultPassword));
        tvDefaultPassword.setText("Enter Current Password. "+ tvDefaultPassword.getText());

        etPassword.setEnabled(false);

        setEditTextListeners();
        setButtonListeners();

        sharedPreferences = getSharedPreferences("myprefs", 1);

        byte[] defaultEncoded = Base64.encode("1234".getBytes(), 0);

        String encodedString = sharedPreferences.getString("password", new String(defaultEncoded));
        byte decodedBytes[] = Base64.decode(encodedString.getBytes(), 0);
        decodedString = new String(decodedBytes);

    }

    private void setEditTextListeners() {

        final Toast lengthGreaterToast = Toast.makeText(getApplicationContext(), "Length cannot be more than 4.", Toast.LENGTH_SHORT);

        //limit pin length to 4 digits
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 4) {
                    etPassword.setText(s.subSequence(0, 4));
                    lengthGreaterToast.show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setButtonListeners() {
        View.OnClickListener digitClickListner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPassword.setText(etPassword.getText().append(((Button) v).getText()));
            }
        };

        View digitButtons[] = {findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
                findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
                findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
                findViewById(R.id.btn9)};

        for (View v : digitButtons) {
            v.setOnClickListener(digitClickListner);
        }


        findViewById(R.id.btnenter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast doNotMatchToast = Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_SHORT);
                Toast confirmationToast = Toast.makeText(getApplicationContext(), "Incorrect Password", Toast.LENGTH_SHORT);

                //count 0 : current password, count 1: new password, count 2: confirm password
                if (count == 0) {
                    if(etPassword.getText().toString().equals(decodedString)){
                        tvDefaultPassword.setText("Enter New Password");
                        etPassword.setText("");
                        count++;
                    }else{
                        confirmationToast.show();
                        count=0;
                    }

                } else if(count==1){
                    firstPassword = etPassword.getText().toString();
                    etPassword.setText("");
                    tvDefaultPassword.setText("Confirm Password");
                    count++;

                }else if(count==2) {
                    if (etPassword.getText().toString().equals(firstPassword)) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        String encodedString = new String(Base64.encode(firstPassword.getBytes(), 0));
                        editor.putString("password", encodedString);
                        editor.apply();
                        Toast.makeText(getApplicationContext(),"Password Changed.",Toast.LENGTH_SHORT).show();
                        ChangePasswordActivity.this.finish();
                    } else {
                        count=2;
                        etPassword.setText("");
                        doNotMatchToast.show();
                    }

                }


            }
        });

        (findViewById(R.id.btnclear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPassword.setText("");
            }
        });

    }

}
