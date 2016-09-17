package com.suraj.waext;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.rarepebble.colorpicker.ColorPickerView;

public class ColorChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_chooser);

        Bundle bundle  = getIntent().getExtras();

        if(bundle==null){
            Toast.makeText(getApplicationContext(),R.string.internal_error,Toast.LENGTH_SHORT).show();
            finish();
        }

        final String which = bundle.getString("groupOrIndividual");


        if(which==null) {
            Toast.makeText(getApplicationContext(),R.string.internal_error,Toast.LENGTH_SHORT).show();
             finish();
        }

        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);

        final ColorPickerView picker = (ColorPickerView) findViewById(R.id.colorPicker);
        picker.setColor(sharedPreferences.getInt(which, Color.GRAY));

        findViewById(R.id.btnColorChooserCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorChooserActivity.this.finish();
            }
        });

        findViewById(R.id.btnColorChooserOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHighlightColor(which,picker.getColor());
                ColorChooserActivity.this.finish();
            }

        });

    }

    public void updateHighlightColor(String which,int color) {
        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(which, color);
        editor.commit();
    }

    public void updateIndividualColor(int color) {
        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("highlightColor", color);
        editor.commit();
    }

}
