package com.suraj.waext;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.rarepebble.colorpicker.ColorPickerView;

public class ColorChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_chooser);

        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);

        final ColorPickerView picker = (ColorPickerView) findViewById(R.id.colorPicker);
        picker.setColor(sharedPreferences.getInt("highlightColor", Color.GRAY));

        findViewById(R.id.btncolorchoosercancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorChooserActivity.this.finish();
            }
        });

        findViewById(R.id.btncolorchooserok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHighlightColor(picker.getColor());
                ColorChooserActivity.this.finish();
            }

        });

    }

    public void updateHighlightColor(int color) {
        SharedPreferences sharedPreferences = getSharedPreferences("myprefs", 1);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("highlightColor", color);
        editor.commit();
    }

}
